#!/usr/bin/env python3
"""Docling OCR bridge -- OCR Mechanism, Docling concrete adapter's own
Python-side counterpart.

Invoked by `ProcessBuilderDoclingSubprocessInvoker`
(`src/runtime/DoclingOcrProviderAdapter.kt`) as a fresh, disposable child
process for exactly one recognition call -- nothing this script creates
survives past its own single invocation, mirroring
`tools/qmd-relevance-bridge.mts`'s own identical "fresh, disposable child
process" shape.

Contract: `docs/architecture/OCR_MECHANISM_DOCLING_BRIDGE_SCRIPT_PYTHON_CONTRACT.md`
(Section 5-19 fix argv, request/response JSON, exit codes, resource bounds,
and offline behaviour precisely; this file implements exactly that
contract, not a superset or a subset of it).

argv[1]: absolute path to a request JSON file (never a shell, never other
arguments). stdout: exactly one JSON object, only on exit 0. stderr:
diagnostics only, any exit code. Exit codes: 0 success; 2 missing
runtime/model assets; 3 unsupported/inaccessible input; 4 resource-limit
breach; any other non-zero, an unclassified internal fault. Never -1
(reserved exclusively for the JVM-side "process failed to start" signal).

**Docling integration verified empirically (Unit 2 provisioning)** against a
real `docling==2.121.0` installation (CPU-only, Python 3.12, isolated venv)
-- `_build_converter`/`_real_docling_backend` use Docling's own actual,
confirmed public API (`DocumentConverter`, `PdfFormatOption`/`ImageFormatOption`,
`PdfPipelineOptions`, `RapidOcrOptions`, `ConversionResult.status`/`.pages`/
`.confidence`/`.errors`, `DoclingDocument.export_to_text`), not merely this
script's own earlier, speculative guess at that API's shape. Both real
fixture PDFs/images OCR successfully offline, including inside a genuinely
network-isolated (`docker run --network none`) container, using only
already-local model assets -- see
`docs/architecture/OCR_MECHANISM_DOCLING_BRIDGE_SCRIPT_PYTHON_CONTRACT.md`
for the full provisioning record. `_pdf_page_count`/`_check_image_dimensions_preflight`/
`_docling_version` are similarly verified. Every other function in this file
(request parsing, response-JSON construction, exit-code mapping,
resource-bound arithmetic, offline-flag handling) never depended on
Docling's own API and is exercised directly by this file's own pure-Python
test suite (`tests/tools/test_docling_ocr_bridge.py`) without Docling
installed.
"""

from __future__ import annotations

import contextlib
import json
import os
import sys
from dataclasses import dataclass, field

# Offline-mode flags, set before any Docling import anywhere in this
# process (contract Section 10: "before importing or invoking any Docling
# code capable of triggering model resolution"). `setdefault`, not
# overwrite -- the Kotlin side already sets these on this process's own
# environment before launch (`applyDoclingOfflineEnvironment`,
# `src/runtime/DoclingOcrProviderAdapter.kt`); this is a redundant,
# defence-in-depth second layer, not a replacement for that one, so this
# script remains correctly offline-configured even when invoked directly
# (for example, during a future live-acceptance test) without the
# Kotlin-side environment already in place.
os.environ.setdefault("HF_HUB_OFFLINE", "1")
os.environ.setdefault("TRANSFORMERS_OFFLINE", "1")

# ---------------------------------------------------------------------
# Exit codes -- verified character-for-character against
# DoclingOcrProviderAdapter.interpret's own `when (invocation.exitCode)`
# block (src/runtime/DoclingOcrProviderAdapter.kt). -1 is deliberately
# absent: it is exclusively the JVM-side "process failed to start" signal
# and can never be produced by code running inside this script.
# ---------------------------------------------------------------------

EXIT_SUCCESS = 0
EXIT_MISSING_ASSETS = 2
EXIT_UNSUPPORTED_INPUT = 3
EXIT_RESOURCE_LIMIT_BREACH = 4
EXIT_UNCLASSIFIED = 1

SUPPORTED_PROTOCOL_VERSION = "1"

# Resource bounds, reused verbatim from the Docling Authorization's own
# Section 6 / the Adapter Plan's own Section 13 -- never re-derived here.
MAX_PDF_PAGES = 200
MAX_DIMENSION_PX = 10_000
MAX_PIXELS = 100_000_000
MAX_OUTPUT_BYTES = 20 * 1024 * 1024


class BridgeError(Exception):
    """A disclosed, expected failure carrying its own exact exit code --
    request-parsing/malformed-protocol defects only (exit 1); Docling-side
    failure categories use the three dedicated exception types below
    instead, so their exit code is decided in exactly one place (`main`)."""

    def __init__(self, message: str, exit_code: int = EXIT_UNCLASSIFIED):
        super().__init__(message)
        self.exit_code = exit_code


class DoclingUnavailableError(Exception):
    """Required runtime/model/cache assets are absent, or the Docling
    package itself could not be imported -- maps to exit 2."""


class DoclingProcessingError(Exception):
    """Docling itself could not process the supplied content -- malformed,
    corrupt, or genuinely unsupported media -- maps to exit 3."""


class ResourceLimitBreach(Exception):
    """A frozen resource bound (page count, pixel count, output size) was
    exceeded -- maps to exit 4. Never raised after a truncated result has
    already been constructed; always raised instead of returning one."""


@dataclass
class RecognisedSegment:
    text: str
    page_number: int | None = None


@dataclass
class DoclingRecognitionOutcome:
    """The Docling-backend integration's own return shape -- entirely
    independent of the JSON wire format (`build_response_json` below owns
    that translation), so a test's fake backend can construct one of these
    without needing to know or reproduce any JSON detail."""

    status: str  # "recognised" | "partial" | "no_recognisable_content"
    text: str = ""
    fidelity: str = "UNVERIFIED_LITERAL_TRANSCRIPTION"
    segments: list[RecognisedSegment] = field(default_factory=list)
    confidence: float | None = None
    warnings: list[str] = field(default_factory=list)
    reason: str | None = None
    mechanism_version: str | None = None
    model_identity: str | None = None
    model_version: str | None = None

    def __post_init__(self) -> None:
        # Both-or-neither is an internal invariant, not merely a JSON-
        # serialisation convention -- a one-field-only outcome would mean
        # some earlier code path already violated the paired-presence rule
        # this file exists to guarantee, and `build_response_json` silently
        # treating that as "neither" would mask the defect rather than
        # surface it. Fails safely: a fixed, static message only, never the
        # actual field values (which could carry a real identity/digest, or
        # in a genuinely broken future caller, arbitrary content).
        if (self.model_identity is None) != (self.model_version is None):
            raise ValueError(
                "DoclingRecognitionOutcome: model_identity and model_version must be both present or both absent"
            )


# ---------------------------------------------------------------------
# Request parsing (contract Section 6 -- the request-file JSON shape is
# reused, not designed here; already fixed by
# DoclingOcrProviderAdapter.buildRequestJson).
# ---------------------------------------------------------------------


def load_request(request_path: str) -> dict:
    try:
        with open(request_path, "r", encoding="utf-8") as handle:
            raw = json.load(handle)
    except FileNotFoundError as error:
        raise BridgeError(f"request file not found: {request_path!r}: {error}") from error
    except json.JSONDecodeError as error:
        raise BridgeError(f"request file is not valid JSON: {request_path!r}: {error}") from error

    if not isinstance(raw, dict):
        raise BridgeError("request file does not contain a JSON object")

    protocol_version = raw.get("protocolVersion")
    if protocol_version != SUPPORTED_PROTOCOL_VERSION:
        raise BridgeError(f"unsupported protocolVersion: {protocol_version!r}")

    source_file_path = raw.get("sourceFilePath")
    if not isinstance(source_file_path, str) or not source_file_path:
        raise BridgeError("missing or empty sourceFilePath")

    media_type = raw.get("mediaType")
    if not isinstance(media_type, str) or not media_type:
        raise BridgeError("missing or empty mediaType")

    model_cache_dir = raw.get("modelCacheDir")
    if model_cache_dir is not None and (not isinstance(model_cache_dir, str) or not model_cache_dir):
        raise BridgeError("modelCacheDir must be a non-empty string when present")

    # configurationProfile is read but never required to be acted upon
    # (contract Section 6) -- deliberately not validated or used here.

    return {
        "sourceFilePath": source_file_path,
        "mediaType": media_type,
        "modelCacheDir": model_cache_dir,
    }


# ---------------------------------------------------------------------
# Offline / local-availability pre-flight (contract Section 10, Section 11)
# -- mirrors tools/qmd-relevance-bridge.mts's own verifyModelIsLocallyAvailable
# precedent: a local-filesystem-only check, performed before any Docling
# import capable of triggering model resolution, never itself capable of
# reaching a network.
# ---------------------------------------------------------------------


def verify_model_assets_available(model_cache_dir: str | None) -> None:
    if model_cache_dir is None:
        # No explicit cache directory configured -- this script relies on
        # whichever default location Docling's own current release
        # resolves internally, subject to the same offline-mode
        # enforcement above regardless (contract Section 11).
        return
    if not os.path.isdir(model_cache_dir):
        raise DoclingUnavailableError(
            f"configured modelCacheDir does not exist or is not a directory: {model_cache_dir!r}. "
            "This bridge never downloads or installs models itself -- pre-provision the model "
            "cache before invoking Docling recognition."
        )
    try:
        entries = os.listdir(model_cache_dir)
    except OSError as error:
        raise DoclingUnavailableError(f"could not read modelCacheDir {model_cache_dir!r}: {error}") from error
    if not entries:
        raise DoclingUnavailableError(
            f"configured modelCacheDir is empty: {model_cache_dir!r}. Pre-provision the required "
            "Docling model files before invoking recognition."
        )


# ---------------------------------------------------------------------
# Image dimension preflight (contract Section 13, Stage one -- declared
# header dimensions, before full decompression). Uses Pillow's own
# Image.open, which reads only the header for essentially every common
# format (JPEG/PNG/etc.) -- pixel data is not decoded until an explicit
# .load()/.getdata() call this function never makes.
# ---------------------------------------------------------------------


def check_declared_dimensions(width: int, height: int) -> None:
    """Pure arithmetic, independent of any image library -- directly
    testable, and reused for both the pre-decode (Stage one) and
    post-decode (Stage two) checks (contract Section 13)."""
    if width <= 0 or height <= 0:
        raise DoclingProcessingError(
            f"declared image dimensions {width}x{height} are not positive -- malformed or "
            "adversarial header, failing closed"
        )
    if width > MAX_DIMENSION_PX or height > MAX_DIMENSION_PX:
        raise ResourceLimitBreach(
            f"declared image dimensions {width}x{height} exceed the maximum of "
            f"{MAX_DIMENSION_PX}x{MAX_DIMENSION_PX}"
        )
    pixels = width * height
    if pixels > MAX_PIXELS:
        raise ResourceLimitBreach(f"declared total pixel count {pixels} exceeds the maximum of {MAX_PIXELS}")


def _check_image_dimensions_preflight(source_file_path: str) -> None:
    try:
        from PIL import Image  # type: ignore
    except ImportError:
        # Best-effort: Pillow is a well-known Docling transitive dependency,
        # but its actual presence has not been verified in this environment
        # (no live Docling installation exists yet). If genuinely absent,
        # this Stage-one check does not run; Docling's own conversion
        # attempt is still bounded by the Stage-two check performed after
        # conversion (_check_result_page_dimensions, below), which does not
        # depend on Pillow -- the bound is never silently skipped entirely,
        # only potentially deferred to a later point in the same call.
        return
    try:
        with Image.open(source_file_path) as image:
            width, height = image.size
    except Exception as error:  # noqa: BLE001 -- any Pillow failure here is an honest "cannot inspect", not a crash
        raise DoclingProcessingError(f"could not inspect declared image dimensions: {error}") from error
    check_declared_dimensions(width, height)


# ---------------------------------------------------------------------
# PDF page-count preflight (contract Section 13 -- "checked immediately
# after the PDF's own document structure is opened... before any per-page
# OCR work begins").
# ---------------------------------------------------------------------


def _pdf_page_count(source_file_path: str) -> int | None:
    """Best-effort, pre-conversion PDF page count, via pypdfium2 -- verified,
    empirically, against a real Docling installation (Unit 2 of this task's
    own governing instruction) to be an already-present transitive
    dependency of `docling[standard]` (it is Docling's own PDF backend
    library, not a package this script adds). Returns None when
    unavailable, in which case the caller must still enforce MAX_PDF_PAGES
    after Docling's own conversion completes (the post-decode re-check,
    below) -- the bound is deferred, never skipped."""
    try:
        import pypdfium2  # type: ignore
    except ImportError:
        return None
    try:
        document = pypdfium2.PdfDocument(source_file_path)
        try:
            return len(document)
        finally:
            document.close()
    except Exception:  # noqa: BLE001 -- a pre-flight probe failure is not itself a diagnosis; let Docling's own attempt produce the real error
        return None


def check_page_count(page_count: int) -> None:
    if page_count > MAX_PDF_PAGES:
        raise ResourceLimitBreach(f"PDF page count {page_count} exceeds the maximum of {MAX_PDF_PAGES} pages")


# ---------------------------------------------------------------------
# Output-size accumulation (contract Section 13 -- "accumulates
# recognised-text byte length as pages complete and aborts early on
# breach", never waiting until final serialisation to discover an
# oversized result).
# ---------------------------------------------------------------------


class OutputSizeAccumulator:
    def __init__(self, max_bytes: int = MAX_OUTPUT_BYTES) -> None:
        self._max_bytes = max_bytes
        self._bytes_so_far = 0

    def add(self, text: str) -> None:
        self._bytes_so_far += len(text.encode("utf-8"))
        if self._bytes_so_far > self._max_bytes:
            raise ResourceLimitBreach(
                f"accumulated recognised-text size of {self._bytes_so_far} bytes exceeds the "
                f"{self._max_bytes}-byte ceiling"
            )


# ---------------------------------------------------------------------
# Real Docling integration -- imported lazily (never at module import
# time) so this module, and every function above, can be imported and
# exercised by pure-Python tests without Docling installed at all.
# EXTERNAL KNOWLEDGE, NOT VERIFIED against a live installation -- see this
# file's own header comment.
# ---------------------------------------------------------------------


def _docling_version() -> str | None:
    # "docling" is this package's importable module name, but is not
    # guaranteed to be its own installed distribution name -- confirmed,
    # this pass, against a real container build: `pip install
    # docling-slim[standard]` registers exactly one distribution,
    # "docling-slim" (its own real PyPI name; "Modular version of the
    # Docling package" per its own summary), with no separate "docling"
    # dist-info at all, so `importlib.metadata.version("docling")` alone
    # genuinely, honestly finds nothing there -- not a bug in that
    # lookup, but an incomplete list of the one real distribution name
    # this environment actually uses. Tried in order; the first
    # distribution genuinely found wins. Never fabricated: an absent
    # version remains an honest `None` if neither name resolves.
    for distribution_name in ("docling", "docling-slim"):
        try:
            import importlib.metadata

            return importlib.metadata.version(distribution_name)
        except importlib.metadata.PackageNotFoundError:
            continue
        except Exception:  # noqa: BLE001 -- genuinely unavailable is an honest null, never fabricated
            return None
    return None


def _build_converter():
    """Constructs a DocumentConverter configured for local, offline, CPU-only
    execution -- verified empirically against a real installation (Unit 2).

    **Load-bearing discovery, not present in this file's own original,
    illustrative drafting:** Docling's own *default* OCR-engine auto-selection
    (`OcrAutoOptions`) chose RapidOCR's own `backend="torch"` variant, which
    downloads separate PyTorch-format model weights from `modelscope.cn`
    via RapidOCR's own bespoke downloader -- a network call this script's
    `HF_HUB_OFFLINE`/`TRANSFORMERS_OFFLINE` flags do **not** cover, since it
    never goes through `huggingface_hub` at all. Explicitly requesting
    `backend="onnxruntime"` instead uses `.onnx` model weights already
    bundled inside the installed `rapidocr` package itself (shipped as
    package data, present immediately after `pip install`, verified via a
    real, network-disabled Docker container run with zero download log
    lines) -- eliminating this network dependency entirely, not merely
    hiding it behind an offline flag. The document *layout* model
    (`docling-project/docling-layout-heron`, fetched via `huggingface_hub`)
    remains a genuine, one-time, `HF_HUB_OFFLINE`-respecting provisioning
    requirement -- pre-fetched once (with network available, outside any
    governed OCR invocation), cached locally, and reused offline thereafter.
    """
    from docling.datamodel.base_models import InputFormat  # type: ignore
    from docling.datamodel.pipeline_options import PdfPipelineOptions, RapidOcrOptions  # type: ignore
    from docling.document_converter import DocumentConverter, ImageFormatOption, PdfFormatOption  # type: ignore

    ocr_options = RapidOcrOptions(backend="onnxruntime")
    pipeline_options = PdfPipelineOptions(ocr_options=ocr_options)
    return DocumentConverter(
        format_options={
            InputFormat.PDF: PdfFormatOption(pipeline_options=pipeline_options),
            InputFormat.IMAGE: ImageFormatOption(pipeline_options=pipeline_options),
        }
    )


class _VerifiedRecognitionUnavailable(Exception):
    """Internal-only signal that verified OCR model provenance could not be
    prepared for this invocation -- never reaches the JSON response or maps
    to an exit code of its own. Every preparation failure (model file
    missing/unreadable/oversized, manifest entry missing, digest mismatch,
    InferenceSession construction failure) folds into this single class so
    callers have exactly one thing to catch (contract-equivalent plan
    Section 11: "folded into the single existing verification failed
    branch... it does not need its own, third, separately-reported
    state")."""


# Defensive ceiling on the recognition-model file read, independent of
# MAX_OUTPUT_BYTES (contract Section 13's own bound on the *response*). The
# real bundled artifact is a few MB; this only guards the verified-path
# preparation step against reading an implausibly large substituted file
# into memory before hashing it -- a preparation failure here folds into
# the same fallback branch as every other verification failure, never a
# resource-limit exit code, since fallback (Plan Section 13) is defined to
# never disrupt transient OCR.
MAX_MODEL_BYTES = 200 * 1024 * 1024


def _is_lowercase_sha256_hex(value: str) -> bool:
    """Exactly 64 lowercase hexadecimal characters -- rejects wrong length,
    uppercase, and any non-hex character explicitly, rather than relying on
    length alone or on eventual inequality with a computed digest to catch
    a malformed manifest value."""
    import re

    return re.fullmatch(r"[0-9a-f]{64}", value) is not None


def _resolve_and_verify_recognition_model(reader) -> tuple[bytes, str, str]:
    """Deterministically resolves RapidOCR's own bundled recognition-model
    artifact by reading the resolved engine/version/task/lang/model-type
    enums directly off the already-constructed `reader.text_rec.cfg` --
    never by re-deriving Docling's own language/version selection logic
    independently, which would risk silent drift from Docling's actual
    choice. Opens the artifact exactly once and reads at most
    `MAX_MODEL_BYTES + 1` bytes from that single open file object --
    `file.read(n)` never reads more than `n` bytes regardless of the
    underlying file's actual size, so this is a true bound enforced by the
    read call itself, never a `stat()`-then-`read_bytes()` pair (which
    leaves a window in which the file could grow between the size check
    and an unbounded full read). Verifies the read bytes against
    RapidOCR's own bundled `default_models.yaml` manifest entry for that
    exact (engine, version, task, lang, size) key -- including that the
    manifest's own expected digest is itself a syntactically valid,
    lowercase SHA-256 hex string, never merely "some string of length 64",
    before ever comparing against it.

    Returns `(model_bytes, model_url, verified_digest)` on success.
    Raises `_VerifiedRecognitionUnavailable` on any failure -- the bytes
    hashed here are always the exact bytes that get handed to
    `onnxruntime.InferenceSession` next; no second, independent file load
    of the recognition model occurs anywhere in this path (this does not
    describe RapidOCR's own separate, ordinary construction, which loads
    an unused default recognizer earlier and independently -- see
    `_prepare_verified_recognition`'s own docstring)."""
    from pathlib import Path

    from rapidocr.inference_engine.base import FileInfo, InferSession  # type: ignore

    try:
        cfg = reader.text_rec.cfg
        file_info = FileInfo(
            cfg.engine_type, cfg.ocr_version, cfg.task_type, cfg.lang_type, cfg.model_type
        )
        manifest_entry = InferSession.get_model_url(file_info)
        model_url = str(manifest_entry["model_dir"])
        # Validated against the manifest's own raw, unnormalised value --
        # never after a `.strip().lower()` pass, which would silently
        # accept an uppercase or whitespace-padded manifest entry by
        # normalising away the exact defect this check exists to catch
        # (Codex correction pass, item 6/7).
        expected_digest = str(manifest_entry["SHA256"])
        if not _is_lowercase_sha256_hex(expected_digest):
            raise _VerifiedRecognitionUnavailable("manifest digest is not a well-formed lowercase sha256 hex string")

        local_path = Path(cfg.model_root_dir) / Path(model_url).name
        if not local_path.is_file():
            raise _VerifiedRecognitionUnavailable("resolved recognition model artifact is not a regular file")

        with open(local_path, "rb") as handle:
            model_bytes = handle.read(MAX_MODEL_BYTES + 1)
        if len(model_bytes) == 0:
            raise _VerifiedRecognitionUnavailable("resolved recognition model artifact is empty")
        if len(model_bytes) > MAX_MODEL_BYTES:
            raise _VerifiedRecognitionUnavailable("resolved recognition model artifact exceeds the maximum bound")

        import hashlib

        actual_digest = hashlib.sha256(model_bytes).hexdigest()
        if actual_digest != expected_digest:
            raise _VerifiedRecognitionUnavailable("recognition model artifact digest did not match the bundled manifest")

        return model_bytes, model_url, actual_digest
    except _VerifiedRecognitionUnavailable:
        raise
    except Exception as error:  # noqa: BLE001 -- every preparation failure folds into the one fallback branch (plan Section 13)
        raise _VerifiedRecognitionUnavailable(type(error).__name__) from error


def _prepare_verified_recognition(pipeline) -> tuple[str, str] | None:
    """Attempts the corrected implementation plan's Section 5 verified
    binding: resolve and hash-verify the bundled Rec model, build an
    `onnxruntime.InferenceSession` from those exact verified bytes,
    construct a `TextRecognizer` directly around that session (via
    OmegaConf's own documented `allow_objects` flag, never the disproven
    high-level `RapidOCR(params={"Rec": {"session": ...}}})` call), and
    substitute it onto the pipeline's already-constructed RapidOCR engine
    in place of its own default recognizer.

    **Preliminary default load, distinguished from the model actually
    used.** By the time this function runs, Docling's own ordinary
    pipeline construction has already, unconditionally, built a complete
    default RapidOCR engine -- including a default `TextRecognizer`
    loaded from a separate, independent file read RapidOCR itself
    performs internally. This preliminary construction cannot be skipped
    without reimplementing Docling/RapidOCR's own initialization (rejected
    as out of scope; no supported hook to suppress it was found). It is
    never used for OCR after a successful call to this function: the
    substitution below (`reader.text_rec = verified_recognizer`) replaces
    the *only* attribute `RapidOCR.__call__` ever reads for recognition
    (confirmed: `self.text_rec(rec_input)`, one call site, no caching), so
    the preliminary recognizer becomes unreachable from `reader` the
    moment substitution succeeds. Provenance (`modelIdentity`/`modelVersion`)
    describes and binds *only* the verified recognizer this function
    builds and substitutes -- never the preliminary, unused one.

    **Configuration isolation.** `reader.text_rec.cfg` is the *live*
    configuration object the preliminary, already-installed default
    recognizer's own `OrtInferSession` was built from. This function never
    mutates that shared object -- it deep-copies it first, and only the
    copy is flagged `allow_objects`/given the verified session. If any
    step below fails after the copy is made, the original `reader.text_rec`
    and its own `.cfg` remain completely unmutated; the already-installed
    default recognizer's own behaviour (should the fallback branch end up
    using it) is unaffected by this function ever having run.

    Returns `(model_identity, model_version)` and performs the
    substitution as a side effect, ONLY on success. Returns `None` and
    performs no substitution at all on any failure -- RapidOCR's own
    default, already-loaded recognizer is left completely untouched in
    that case, so a provenance-preparation failure can never disrupt
    transient OCR (plan Section 13)."""
    ocr_model = getattr(pipeline, "ocr_model", None)
    reader = getattr(ocr_model, "reader", None)
    if reader is None or getattr(reader, "text_rec", None) is None:
        return None

    try:
        model_bytes, model_url, digest = _resolve_and_verify_recognition_model(reader)

        import copy

        import onnxruntime  # type: ignore
        from pathlib import Path

        from rapidocr.ch_ppocr_rec.main import TextRecognizer  # type: ignore

        session = onnxruntime.InferenceSession(model_bytes, providers=["CPUExecutionProvider"])

        # Deep-copied before mutation -- reader.text_rec.cfg itself (the
        # preliminary, already-installed default recognizer's own live
        # config) is never touched, per this function's own docstring.
        rec_cfg = copy.deepcopy(reader.text_rec.cfg)
        rec_cfg._set_flag("allow_objects", True)
        rec_cfg.session = session
        verified_recognizer = TextRecognizer(rec_cfg)

        # Defensive, runtime object-identity check -- mirrors the mandatory
        # test of the same name. If a future RapidOCR version silently
        # ignored the injected session and built its own instead, this
        # stops the verified digest from ever being paired with bytes that
        # were not actually bound into the recognizer in use.
        if verified_recognizer.session.session is not session:
            raise _VerifiedRecognitionUnavailable("constructed recognizer did not bind the verified session")

        reader.text_rec = verified_recognizer
    except Exception as error:  # noqa: BLE001 -- preparation failure only; never disrupts transient OCR (plan Section 13)
        sys.stderr.write(
            f"verified OCR model provenance unavailable for this invocation ({type(error).__name__}); "
            "falling back to RapidOCR's own default recognizer without provenance\n"
        )
        return None

    model_identity = f"rapidocr-onnxruntime:{Path(model_url).stem}"
    model_version = f"sha256:{digest}"
    return model_identity, model_version


def _real_docling_backend(source_file_path: str, media_type: str, model_cache_dir: str | None) -> DoclingRecognitionOutcome:
    # Resource-bound preflight checks run first, before even checking whether
    # Docling itself is importable (Unit 3 acceptance finding, corrected from
    # an earlier ordering that checked Docling's own importability first):
    # a resource-limit breach is a property of the *input*, independent of
    # runtime availability, and rejecting it cheaply should never be gated
    # on whether a heavier runtime happens to be present -- pypdfium2/Pillow
    # (both already-installed Docling transitive dependencies) are enough on
    # their own to answer these checks.
    if media_type == "application/pdf":
        page_count = _pdf_page_count(source_file_path)
        if page_count is not None:
            check_page_count(page_count)
    elif media_type.startswith("image/"):
        _check_image_dimensions_preflight(source_file_path)

    try:
        from docling.datamodel.base_models import ConversionStatus, InputFormat  # type: ignore
    except ImportError as error:
        raise DoclingUnavailableError(f"Docling package could not be imported: {error}") from error

    provenance: tuple[str, str] | None = None
    try:
        # Any stray print()/progress-bar output a transitive dependency emits
        # during model loading or conversion is redirected to stderr, never
        # left free to reach stdout and corrupt the one JSON value this
        # script's own contract (Section 7) requires stdout to carry exactly.
        # Scoped around construction too, not merely .convert(), since model
        # loading (the heaviest, most log-chatty step) happens at
        # DocumentConverter() construction time. Python-level only -- this
        # does not, and cannot, redirect writes a C extension makes directly
        # to file descriptor 1, a disclosed, not eliminated, limitation
        # (contract Section 24, adversarial item 8: "mitigated, not
        # eliminated by this document alone").
        with contextlib.redirect_stdout(sys.stderr):
            converter = _build_converter()
            # Force pipeline construction (and, with it, RapidOcrModel's own
            # normal, unmodified engine construction) before conversion, so
            # the verified-provenance preparation below has a real,
            # already-built RapidOCR engine to inspect and, on success,
            # substitute into -- `initialize_pipeline` reuses Docling's own
            # public pipeline cache (`DocumentConverter.initialized_pipelines`,
            # keyed by (pipeline class, options hash)), so `convert()` below
            # reuses this exact same pipeline instance rather than building a
            # second one (verified empirically: same object identity across
            # the two calls).
            docling_format = InputFormat.PDF if media_type == "application/pdf" else InputFormat.IMAGE
            converter.initialize_pipeline(docling_format)
            pipeline = next(iter(converter.initialized_pipelines.values()), None)
            if pipeline is not None:
                provenance = _prepare_verified_recognition(pipeline)
            result = converter.convert(source_file_path)
    except (DoclingUnavailableError, DoclingProcessingError, ResourceLimitBreach):
        raise
    except OSError as error:
        # A genuine "required model asset is missing, and offline mode
        # correctly prevented fetching it" condition -- discovered
        # empirically (Unit 2 provisioning): huggingface_hub's own
        # LocalEntryNotFoundError (a FileNotFoundError/OSError subclass) is
        # what Docling's own layout-model resolution raises when
        # HF_HUB_OFFLINE=1 and the model is not already cached. This is a
        # missing-assets condition (exit 2), never this specific input's own
        # "unsupported" disposition (exit 3) -- a genuine defect this
        # provisioning unit found and corrected: an earlier version of this
        # function mapped this exact case to exit 3, which would have told a
        # future implementer the *input* was the problem when the *runtime
        # provisioning* was. Deliberately checked before the generic
        # `Exception` handler below, and deliberately checking the exception
        # class hierarchy (OSError) rather than importing huggingface_hub
        # directly -- this file must remain importable in this module's own
        # pure-Python test suite without huggingface_hub installed.
        raise DoclingUnavailableError(
            f"required Docling model asset is not locally cached, and offline mode correctly prevented "
            f"fetching it: {error}"
        ) from error
    except Exception as error:  # noqa: BLE001 -- any other Docling conversion failure is this input's own "unsupported/inaccessible" disposition
        raise DoclingProcessingError(f"Docling could not process the supplied content: {error}") from error

    document = result.document

    # Stage-two, post-decode page-count re-check -- a backstop for the case
    # _pdf_page_count could not run or under-reported (contract Section 13's
    # own two-stage discipline, applied here to page count rather than pixel
    # dimensions). result.pages is Docling's own ConversionResult field
    # (verified fresh against a real installation), never the DoclingDocument
    # object's own, differently-shaped attribute.
    try:
        actual_page_count = len(result.pages)
    except TypeError:
        actual_page_count = None
    if actual_page_count is not None:
        check_page_count(actual_page_count)

    # Docling's own ConversionStatus is the authoritative success/partial/
    # failure signal (verified fresh against a real installation) -- never
    # inferred from "is the recognised text non-empty", which cannot
    # distinguish a genuine failure from a genuinely blank page.
    if result.status == ConversionStatus.FAILURE:
        error_messages = "; ".join(str(e) for e in (result.errors or [])) or "no further detail reported"
        raise DoclingProcessingError(f"Docling reported conversion failure: {error_messages}")
    if result.status == ConversionStatus.SKIPPED:
        raise DoclingProcessingError("Docling skipped the supplied content without attempting conversion")

    text = document.export_to_text()
    confidence = result.confidence.mean_score if result.confidence is not None else None
    model_identity, model_version = provenance if provenance is not None else (None, None)

    if not text or not text.strip():
        return DoclingRecognitionOutcome(
            status="no_recognisable_content",
            reason="Docling completed without error but recognised no usable content",
            mechanism_version=_docling_version(),
            model_identity=model_identity,
            model_version=model_version,
        )

    accumulator = OutputSizeAccumulator()
    accumulator.add(text)

    if result.status == ConversionStatus.PARTIAL_SUCCESS:
        error_messages = "; ".join(str(e) for e in (result.errors or [])) or "Docling reported partial success"
        return DoclingRecognitionOutcome(
            status="partial",
            text=text,
            fidelity="UNVERIFIED_LITERAL_TRANSCRIPTION",
            confidence=confidence,
            reason=error_messages,
            mechanism_version=_docling_version(),
            model_identity=model_identity,
            model_version=model_version,
        )

    return DoclingRecognitionOutcome(
        status="recognised",
        text=text,
        fidelity="UNVERIFIED_LITERAL_TRANSCRIPTION",
        confidence=confidence,
        mechanism_version=_docling_version(),
        model_identity=model_identity,
        model_version=model_version,
    )


# ---------------------------------------------------------------------
# Orchestration -- the only function that decides *which* backend runs;
# tests supply `docling_backend` directly, never monkeypatching module
# internals.
# ---------------------------------------------------------------------


def recognise(
    source_file_path: str,
    media_type: str,
    model_cache_dir: str | None,
    docling_backend=None,
) -> DoclingRecognitionOutcome:
    if docling_backend is None:
        docling_backend = _real_docling_backend
    verify_model_assets_available(model_cache_dir)
    return docling_backend(source_file_path, media_type, model_cache_dir)


# ---------------------------------------------------------------------
# Response-JSON construction (contract Section 7 -- verified field-for-
# field against DoclingOcrProviderAdapter.kt's own parseRecognitionResponse/
# parseBridgeResponse). Uses json.dumps's own default ensure_ascii=True
# (contract Section 7.5) -- the already-adopted Kotlin-side parser
# correctly decodes the resulting \uXXXX escapes (fixed and tested during
# that adapter's own owner-acceptance review).
# ---------------------------------------------------------------------


def build_response_json(outcome: DoclingRecognitionOutcome) -> str:
    if outcome.status == "no_recognisable_content":
        payload: dict = {"status": "no_recognisable_content", "reason": outcome.reason}
        return _serialize_within_bound(payload)

    payload = {
        "status": outcome.status,
        "recognisedText": outcome.text,
        "fidelity": outcome.fidelity,
    }
    if outcome.confidence is not None:
        payload["confidence"] = outcome.confidence
    if outcome.warnings:
        payload["warnings"] = outcome.warnings
    if outcome.segments:
        payload["segments"] = [
            {"text": segment.text, "fidelity": outcome.fidelity, "pageNumber": segment.page_number}
            for segment in outcome.segments
        ]
    if outcome.mechanism_version is not None:
        payload["mechanismVersion"] = outcome.mechanism_version
    # Both-or-neither, deliberately checked on both fields together (never
    # either field alone) -- mirrors the paired-presence invariant
    # `OcrRecognitionIdentity`'s own `init` enforces on the Kotlin side, so
    # a defect here would still be caught defensively at that boundary, not
    # only trusted from this end.
    if outcome.model_identity is not None and outcome.model_version is not None:
        payload["modelIdentity"] = outcome.model_identity
        payload["modelVersion"] = outcome.model_version
    if outcome.status == "partial":
        payload["reason"] = outcome.reason

    return _serialize_within_bound(payload)


def _serialize_within_bound(payload: dict) -> str:
    # Final, independent backstop -- never truncates; raises instead
    # (contract Section 13: "output must fail, never truncate
    # successfully"), mirroring DoclingOcrProviderAdapter.kt's own,
    # separate, already-adopted post-parse re-check.
    serialized = json.dumps(payload)
    serialized_bytes = len(serialized.encode("utf-8"))
    if serialized_bytes > MAX_OUTPUT_BYTES:
        raise ResourceLimitBreach(f"serialized response of {serialized_bytes} bytes exceeds the {MAX_OUTPUT_BYTES}-byte ceiling")
    return serialized


# ---------------------------------------------------------------------
# Entry point
# ---------------------------------------------------------------------


def main(argv: list[str]) -> int:
    if len(argv) < 2 or not argv[1]:
        sys.stderr.write("missing required argument: request file path\n")
        return EXIT_UNCLASSIFIED

    try:
        request = load_request(argv[1])
    except BridgeError as error:
        sys.stderr.write(f"{error}\n")
        return error.exit_code

    try:
        outcome = recognise(request["sourceFilePath"], request["mediaType"], request["modelCacheDir"])
        response = build_response_json(outcome)
    except DoclingUnavailableError as error:
        sys.stderr.write(f"{error}\n")
        return EXIT_MISSING_ASSETS
    except DoclingProcessingError as error:
        sys.stderr.write(f"{error}\n")
        return EXIT_UNSUPPORTED_INPUT
    except ResourceLimitBreach as error:
        sys.stderr.write(f"{error}\n")
        return EXIT_RESOURCE_LIMIT_BREACH

    sys.stdout.write(response)
    return EXIT_SUCCESS


if __name__ == "__main__":
    try:
        sys.exit(main(sys.argv))
    except Exception as error:  # noqa: BLE001 -- an unclassified, genuinely unanticipated fault; never a traceback on stdout
        sys.stderr.write(f"{error}\n")
        sys.exit(EXIT_UNCLASSIFIED)
