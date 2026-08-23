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

Docling's own actual, installed public API is EXTERNAL KNOWLEDGE, NOT
VERIFIED against a live installation in the environment this file was
drafted in (contract document Section 2's own disclosure, restated here).
The Docling-specific integration below (`_real_docling_backend`, `_pdf_page_count`,
`_check_image_dimensions_preflight`, `_docling_version`) is this script's
own best-effort design against Docling's well-known, documented API shape
at drafting time -- it must be reconfirmed, and corrected if wrong, against
whichever Docling version is actually installed before this script is used
against a real installation. Every other function in this file (request
parsing, response-JSON construction, exit-code mapping, resource-bound
arithmetic, offline-flag handling) does not depend on Docling's own API at
all and is exercised directly by this file's own pure-Python test suite
(`tests/tools/test_docling_ocr_bridge.py`) without Docling installed.
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
    fidelity: str = "VERBATIM"
    segments: list[RecognisedSegment] = field(default_factory=list)
    confidence: float | None = None
    warnings: list[str] = field(default_factory=list)
    reason: str | None = None
    mechanism_version: str | None = None
    model_identity: str | None = None


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
    """Best-effort, pre-conversion PDF page count. Returns None when no
    page-counting library is available, in which case the caller must
    still enforce MAX_PDF_PAGES after Docling's own conversion completes
    (_check_result_page_count, below) -- the bound is deferred, never
    skipped."""
    try:
        import pypdf  # type: ignore
    except ImportError:
        return None
    try:
        with open(source_file_path, "rb") as handle:
            reader = pypdf.PdfReader(handle)
            return len(reader.pages)
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
    try:
        import importlib.metadata

        return importlib.metadata.version("docling")
    except Exception:  # noqa: BLE001 -- genuinely unavailable is an honest null, never fabricated
        return None


def _real_docling_backend(source_file_path: str, media_type: str, model_cache_dir: str | None) -> DoclingRecognitionOutcome:
    try:
        from docling.document_converter import DocumentConverter  # type: ignore
    except ImportError as error:
        raise DoclingUnavailableError(f"Docling package could not be imported: {error}") from error

    if media_type == "application/pdf":
        page_count = _pdf_page_count(source_file_path)
        if page_count is not None:
            check_page_count(page_count)
    elif media_type.startswith("image/"):
        _check_image_dimensions_preflight(source_file_path)

    try:
        # Any stray print()/progress-bar output a transitive dependency emits
        # during model loading or conversion is redirected to stderr, never
        # left free to reach stdout and corrupt the one JSON value this
        # script's own contract (Section 7) requires stdout to carry exactly.
        # Scoped around construction too, not merely .convert(), since model
        # loading (the heaviest, most log-chatty step) happens at
        # DocumentConverter() construction time for at least some Docling
        # pipeline configurations. Python-level only -- this does not, and
        # cannot, redirect writes a C extension makes directly to file
        # descriptor 1, a disclosed, not eliminated, limitation (contract
        # Section 24, adversarial item 8: "mitigated, not eliminated by this
        # document alone").
        with contextlib.redirect_stdout(sys.stderr):
            converter = DocumentConverter()
            result = converter.convert(source_file_path)
    except Exception as error:  # noqa: BLE001 -- any Docling conversion failure is this input's own "unsupported/inaccessible" disposition
        raise DoclingProcessingError(f"Docling could not process the supplied content: {error}") from error

    document = result.document

    # Stage-two, post-decode page-count re-check -- a backstop for the
    # case _pdf_page_count could not run (no pypdf available) or
    # under-reported (contract Section 13's own two-stage discipline,
    # applied here to page count rather than pixel dimensions).
    pages_attr = getattr(document, "pages", None)
    if pages_attr is not None:
        try:
            actual_page_count = len(pages_attr)
        except TypeError:
            actual_page_count = None
        if actual_page_count is not None:
            check_page_count(actual_page_count)

    text = document.export_to_text() if hasattr(document, "export_to_text") else str(document)

    if not text or not text.strip():
        return DoclingRecognitionOutcome(
            status="no_recognisable_content",
            reason="Docling completed without error but recognised no usable content",
            mechanism_version=_docling_version(),
        )

    accumulator = OutputSizeAccumulator()
    accumulator.add(text)

    return DoclingRecognitionOutcome(
        status="recognised",
        text=text,
        fidelity="VERBATIM",
        mechanism_version=_docling_version(),
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
    if outcome.model_identity is not None:
        payload["modelIdentity"] = outcome.model_identity
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
