"""Live, Docling/rapidocr/onnxruntime-dependent tests for the verified OCR
model provenance mechanism added to tools/docling-ocr-bridge.py (the
corrected implementation plan's Section 5 "verified" binding and Section
13 "fallback" branch):
`docs/architecture/TIER_B_OCR_TRUTHFUL_MANDATORY_PROVENANCE_IMPLEMENTATION_PLAN.md`.

Requires a provisioned Docling/rapidocr/onnxruntime environment -- the same
`~/docling-venv` `tests/integration/DoclingOcrProviderAdapterLiveAcceptanceTest.kt`
already depends on. Every test class here is skipped entirely (never
failed) when that environment is not importable, mirroring that Kotlin
live-acceptance file's own `assumeTrue`/skip discipline, and
`tests/tools/test_docling_ocr_bridge.py`'s own "runs with no Docling
installed" convention for the rest of the bridge script.

Deliberately never mutates the real, shared `~/docling-venv` installation
-- every corrupted/missing/truncated/unreadable-model scenario is built by
temporarily redirecting a live `TextRecognizer.cfg.model_root_dir`
(a real, ordinary OmegaConf config attribute, restored in a `finally`
block) at a disposable `tempfile.TemporaryDirectory()`, never by touching
`rapidocr`'s own installed package files. `_resolve_and_verify_recognition_model`
reads bytes directly (`Path.read_bytes()`), never through RapidOCR's own
`download_file.py` self-heal-on-mismatch machinery, so redirecting
`model_root_dir` this way exercises this file's own verification logic in
full isolation from RapidOCR's own, separate, pre-existing "redownload on
hash mismatch" behaviour (discovered, disclosed, during this task's own
fresh empirical testing -- see the human-review report for the full
finding).

Run explicitly with the docling venv's own interpreter, for example:
    ~/docling-venv/bin/python3 -m unittest tests.tools.test_docling_ocr_bridge_provenance -v
(run from the repository root so the module import above resolves).
"""

from __future__ import annotations

import hashlib
import importlib.util
import os
import shutil
import sys
import tempfile
import unittest
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]
BRIDGE_SCRIPT_PATH = REPO_ROOT / "tools" / "docling-ocr-bridge.py"
FIXTURE_PNG = REPO_ROOT / "tests" / "fixtures" / "document-ingestion-bakeoff" / "fixtures" / "07-text-image.png"

_DOCLING_AVAILABLE = all(
    importlib.util.find_spec(name) is not None for name in ("docling", "rapidocr", "onnxruntime")
)

_spec = importlib.util.spec_from_file_location("docling_ocr_bridge_provenance_target", BRIDGE_SCRIPT_PATH)
assert _spec is not None and _spec.loader is not None
bridge = importlib.util.module_from_spec(_spec)
sys.modules["docling_ocr_bridge_provenance_target"] = bridge
_spec.loader.exec_module(bridge)  # type: ignore[union-attr]


def _build_real_pipeline():
    """Constructs a real `DocumentConverter` and forces real pipeline
    construction exactly as `_real_docling_backend` does (`_build_converter`
    then `initialize_pipeline`), returning `(converter, pipeline)` so a
    test can inspect/mutate `pipeline.ocr_model.reader` directly. Loads a
    real, unmodified RapidOCR engine every time -- never faked, never
    mocked."""
    from docling.datamodel.base_models import InputFormat  # type: ignore

    converter = bridge._build_converter()
    converter.initialize_pipeline(InputFormat.IMAGE)
    pipeline = next(iter(converter.initialized_pipelines.values()))
    return converter, pipeline


def _real_manifest_entry(reader):
    from rapidocr.inference_engine.base import FileInfo, InferSession  # type: ignore

    cfg = reader.text_rec.cfg
    file_info = FileInfo(cfg.engine_type, cfg.ocr_version, cfg.task_type, cfg.lang_type, cfg.model_type)
    return InferSession.get_model_url(file_info)


@unittest.skipUnless(_DOCLING_AVAILABLE, "requires a provisioned docling/rapidocr/onnxruntime environment")
class VerifiedRecognitionModelResolutionTest(unittest.TestCase):
    """`_resolve_and_verify_recognition_model` -- the hash-verification step
    in isolation, real manifest lookup, real hashing, controlled scratch
    artifacts only."""

    def test_resolves_and_verifies_the_real_bundled_model_against_an_independent_digest(self) -> None:
        _, pipeline = _build_real_pipeline()
        reader = pipeline.ocr_model.reader
        model_bytes, model_url, digest = bridge._resolve_and_verify_recognition_model(reader)

        real_root = Path(reader.text_rec.cfg.model_root_dir)
        real_path = real_root / Path(model_url).name
        independent_digest = hashlib.sha256(real_path.read_bytes()).hexdigest()

        self.assertEqual(digest, independent_digest, "reported digest must equal an independent sha256 of the real bundled artifact")
        self.assertEqual(len(model_bytes), real_path.stat().st_size, "hashed byte count must equal the real file's own size")

    def test_a_genuinely_different_valid_onnx_file_yields_a_different_correctly_rejected_digest(self) -> None:
        """Actual-bytes binding proof, failure side: substituting a
        genuinely different, real, valid ONNX file (the detection model,
        never the recognition model) at the resolved path must be rejected
        -- its digest does not match the recognition manifest entry -- not
        silently accepted as if it were the real Rec model."""
        _, pipeline = _build_real_pipeline()
        reader = pipeline.ocr_model.reader
        cfg = reader.text_rec.cfg
        real_root = Path(cfg.model_root_dir)
        manifest = _real_manifest_entry(reader)
        rec_name = Path(manifest["model_dir"]).name
        det_candidates = [p for p in real_root.glob("*det*.onnx")]
        self.assertTrue(det_candidates, "expected a bundled detection-model .onnx file to exist for this substitution proof")
        det_path = det_candidates[0]
        self.assertNotEqual(
            hashlib.sha256(det_path.read_bytes()).hexdigest(),
            str(manifest["SHA256"]).lower(),
            "precondition: the detection model's digest must genuinely differ from the recognition model's manifest digest",
        )

        with tempfile.TemporaryDirectory() as scratch:
            scratch_path = Path(scratch)
            shutil.copyfile(det_path, scratch_path / rec_name)
            original_root_dir = cfg.model_root_dir
            cfg.model_root_dir = str(scratch_path)
            try:
                with self.assertRaises(bridge._VerifiedRecognitionUnavailable):
                    bridge._resolve_and_verify_recognition_model(reader)
            finally:
                cfg.model_root_dir = original_root_dir

    def test_a_single_corrupted_byte_is_rejected(self) -> None:
        _, pipeline = _build_real_pipeline()
        reader = pipeline.ocr_model.reader
        cfg = reader.text_rec.cfg
        real_root = Path(cfg.model_root_dir)
        manifest = _real_manifest_entry(reader)
        real_name = Path(manifest["model_dir"]).name
        real_bytes = (real_root / real_name).read_bytes()

        with tempfile.TemporaryDirectory() as scratch:
            scratch_path = Path(scratch)
            corrupted_path = scratch_path / real_name
            corrupted_path.write_bytes(real_bytes[:-1] + bytes([real_bytes[-1] ^ 0xFF]))
            original_root_dir = cfg.model_root_dir
            cfg.model_root_dir = str(scratch_path)
            try:
                with self.assertRaises(bridge._VerifiedRecognitionUnavailable):
                    bridge._resolve_and_verify_recognition_model(reader)
            finally:
                cfg.model_root_dir = original_root_dir

    def test_missing_model_file_is_rejected(self) -> None:
        _, pipeline = _build_real_pipeline()
        reader = pipeline.ocr_model.reader
        cfg = reader.text_rec.cfg
        with tempfile.TemporaryDirectory() as scratch:
            original_root_dir = cfg.model_root_dir
            cfg.model_root_dir = scratch  # empty directory -- the resolved file does not exist here
            try:
                with self.assertRaises(bridge._VerifiedRecognitionUnavailable):
                    bridge._resolve_and_verify_recognition_model(reader)
            finally:
                cfg.model_root_dir = original_root_dir

    def test_unreadable_model_file_is_rejected(self) -> None:
        if os.geteuid() == 0:
            self.skipTest("running as root -- file permission bits do not block reads")
        _, pipeline = _build_real_pipeline()
        reader = pipeline.ocr_model.reader
        cfg = reader.text_rec.cfg
        real_root = Path(cfg.model_root_dir)
        manifest = _real_manifest_entry(reader)
        real_name = Path(manifest["model_dir"]).name

        with tempfile.TemporaryDirectory() as scratch:
            scratch_path = Path(scratch)
            unreadable = scratch_path / real_name
            shutil.copyfile(real_root / real_name, unreadable)
            os.chmod(unreadable, 0o000)
            original_root_dir = cfg.model_root_dir
            cfg.model_root_dir = str(scratch_path)
            try:
                with self.assertRaises(bridge._VerifiedRecognitionUnavailable):
                    bridge._resolve_and_verify_recognition_model(reader)
            finally:
                os.chmod(unreadable, 0o644)
                cfg.model_root_dir = original_root_dir

    def test_manifest_entry_missing_is_rejected(self) -> None:
        """No real (engine, version, task, lang, size) combination the
        bundled `default_models.yaml` lacks an entry for is reachable
        through Docling's own default configuration -- constructing one
        directly (an unregistered synthetic lang/type combination) proves
        `InferSession.get_model_url`'s own real `KeyError`/lookup failure
        folds into the same `_VerifiedRecognitionUnavailable` branch as
        every other preparation failure, never a crash of its own."""
        from rapidocr.utils.typings import LangRec, ModelType  # type: ignore

        class _FakeCfg:
            engine_type = None
            ocr_version = None
            task_type = None
            lang_type = LangRec.CH
            model_type = ModelType.SMALL
            model_root_dir = "/nonexistent"

        class _FakeTextRec:
            cfg = _FakeCfg()

        class _FakeReader:
            text_rec = _FakeTextRec()

        with self.assertRaises(bridge._VerifiedRecognitionUnavailable):
            bridge._resolve_and_verify_recognition_model(_FakeReader())

    def test_model_exactly_at_the_bound_is_accepted(self) -> None:
        """Codex correction pass, item 4: the bound is enforced by the
        `file.read(n)` call itself, never a `stat()`-then-`read_bytes()`
        pair -- exercised here against the real, unmodified bundled
        artifact with `MAX_MODEL_BYTES` temporarily set to exactly its own
        real size, restored in `finally`."""
        _, pipeline = _build_real_pipeline()
        reader = pipeline.ocr_model.reader
        manifest = _real_manifest_entry(reader)
        real_path = Path(reader.text_rec.cfg.model_root_dir) / Path(manifest["model_dir"]).name
        real_size = real_path.stat().st_size

        original_bound = bridge.MAX_MODEL_BYTES
        bridge.MAX_MODEL_BYTES = real_size
        try:
            model_bytes, _model_url, digest = bridge._resolve_and_verify_recognition_model(reader)
            self.assertEqual(len(model_bytes), real_size)
            self.assertEqual(digest, hashlib.sha256(real_path.read_bytes()).hexdigest())
        finally:
            bridge.MAX_MODEL_BYTES = original_bound

    def test_model_one_byte_over_the_bound_is_rejected(self) -> None:
        _, pipeline = _build_real_pipeline()
        reader = pipeline.ocr_model.reader
        manifest = _real_manifest_entry(reader)
        real_path = Path(reader.text_rec.cfg.model_root_dir) / Path(manifest["model_dir"]).name
        real_size = real_path.stat().st_size

        original_bound = bridge.MAX_MODEL_BYTES
        bridge.MAX_MODEL_BYTES = real_size - 1
        try:
            with self.assertRaises(bridge._VerifiedRecognitionUnavailable):
                bridge._resolve_and_verify_recognition_model(reader)
        finally:
            bridge.MAX_MODEL_BYTES = original_bound

    def test_oversized_artifact_is_rejected_without_an_unbounded_read(self) -> None:
        """Decisive proof there is no unbounded allocation: a genuinely
        huge (2 GiB, sparse -- near-zero real disk/memory cost to create)
        substituted file at the resolved path, with a small bound, must be
        rejected almost instantly. An implementation that read the whole
        file before checking its length would either take many seconds or
        exhaust memory attempting to materialise/hash 2 GiB; this test's
        tight wall-clock ceiling makes that failure mode observable."""
        import time

        _, pipeline = _build_real_pipeline()
        reader = pipeline.ocr_model.reader
        cfg = reader.text_rec.cfg
        manifest = _real_manifest_entry(reader)
        real_name = Path(manifest["model_dir"]).name

        with tempfile.TemporaryDirectory() as scratch:
            scratch_path = Path(scratch)
            huge_path = scratch_path / real_name
            with open(huge_path, "wb") as handle:
                handle.seek((2 * 1024 * 1024 * 1024) - 1)  # 2 GiB sparse file
                handle.write(b"\0")

            original_bound = bridge.MAX_MODEL_BYTES
            original_root_dir = cfg.model_root_dir
            bridge.MAX_MODEL_BYTES = 1024
            cfg.model_root_dir = str(scratch_path)
            try:
                started = time.monotonic()
                with self.assertRaises(bridge._VerifiedRecognitionUnavailable):
                    bridge._resolve_and_verify_recognition_model(reader)
                elapsed = time.monotonic() - started
                self.assertLess(
                    elapsed, 5.0,
                    f"rejecting an oversized artifact took {elapsed:.2f}s -- too slow for a bounded read, "
                    "suggesting the full 2 GiB file was read before the bound was checked",
                )
            finally:
                bridge.MAX_MODEL_BYTES = original_bound
                cfg.model_root_dir = original_root_dir

    def test_manifest_digest_wrong_length_is_rejected(self) -> None:
        from unittest import mock

        _, pipeline = _build_real_pipeline()
        reader = pipeline.ocr_model.reader
        real_manifest = _real_manifest_entry(reader)
        malformed = dict(real_manifest)
        malformed["SHA256"] = "a" * 63  # one short

        with mock.patch("rapidocr.inference_engine.base.InferSession.get_model_url", return_value=malformed):
            with self.assertRaises(bridge._VerifiedRecognitionUnavailable):
                bridge._resolve_and_verify_recognition_model(reader)

    def test_manifest_digest_uppercase_is_rejected(self) -> None:
        from unittest import mock

        _, pipeline = _build_real_pipeline()
        reader = pipeline.ocr_model.reader
        real_manifest = _real_manifest_entry(reader)
        malformed = dict(real_manifest)
        malformed["SHA256"] = str(real_manifest["SHA256"]).upper()

        with mock.patch("rapidocr.inference_engine.base.InferSession.get_model_url", return_value=malformed):
            with self.assertRaises(bridge._VerifiedRecognitionUnavailable):
                bridge._resolve_and_verify_recognition_model(reader)

    def test_manifest_digest_non_hex_is_rejected(self) -> None:
        from unittest import mock

        _, pipeline = _build_real_pipeline()
        reader = pipeline.ocr_model.reader
        real_manifest = _real_manifest_entry(reader)
        malformed = dict(real_manifest)
        malformed["SHA256"] = ("g" * 64)

        with mock.patch("rapidocr.inference_engine.base.InferSession.get_model_url", return_value=malformed):
            with self.assertRaises(bridge._VerifiedRecognitionUnavailable):
                bridge._resolve_and_verify_recognition_model(reader)


@unittest.skipUnless(_DOCLING_AVAILABLE, "requires a provisioned docling/rapidocr/onnxruntime environment")
class VerifiedRecognitionSubstitutionTest(unittest.TestCase):
    """`_prepare_verified_recognition` -- the full verified/fallback
    binding, against a real pipeline's real, already-constructed RapidOCR
    engine."""

    def test_object_identity_the_recognizer_binds_the_exact_supplied_session(self) -> None:
        """Mandatory object-identity test: proves the real inference path
        uses the exact pre-built InferenceSession Parker supplied -- not
        merely a configuration string comparison. Deep-copies `cfg` first,
        mirroring the production implementation's own config-isolation
        discipline (Codex correction pass, item 5) -- and confirms the
        original, shared config was never touched."""
        import copy

        import onnxruntime  # type: ignore
        from rapidocr.ch_ppocr_rec.main import TextRecognizer  # type: ignore

        _, pipeline = _build_real_pipeline()
        reader = pipeline.ocr_model.reader
        original_cfg = reader.text_rec.cfg
        model_bytes, _model_url, _digest = bridge._resolve_and_verify_recognition_model(reader)

        session = onnxruntime.InferenceSession(model_bytes, providers=["CPUExecutionProvider"])
        rec_cfg = copy.deepcopy(original_cfg)
        rec_cfg._set_flag("allow_objects", True)
        rec_cfg.session = session
        substituted = TextRecognizer(rec_cfg)

        self.assertIs(substituted.session.session, session, "the recognizer's own underlying InferenceSession must be the exact object supplied, not merely an equal-looking one")
        self.assertIsNone(original_cfg.get("session", None), "the original, shared cfg must never be mutated by constructing the verified recognizer's own isolated copy")
        self.assertIsNone(original_cfg._get_flag("allow_objects"), "the original, shared cfg's allow_objects flag must never be set")

    def test_verified_provenance_matches_independent_digest_and_transient_output_is_unchanged(self) -> None:
        converter, pipeline = _build_real_pipeline()

        baseline_converter = bridge._build_converter()
        baseline_text = baseline_converter.convert(str(FIXTURE_PNG)).document.export_to_text()

        result = bridge._prepare_verified_recognition(pipeline)
        self.assertIsNotNone(result, "expected the verified path to succeed against the real, unmodified bundled model")
        model_identity, model_version = result
        self.assertTrue(model_identity.startswith("rapidocr-onnxruntime:"))
        self.assertNotIn("/", model_identity, "modelIdentity must never contain a filesystem path")
        self.assertNotIn("unknown", model_identity.lower())
        self.assertRegex(model_version, r"^sha256:[0-9a-f]{64}$")

        reader = pipeline.ocr_model.reader
        manifest = _real_manifest_entry(reader)
        real_root = Path(reader.text_rec.cfg.model_root_dir)
        independent_digest = hashlib.sha256((real_root / Path(manifest["model_dir"]).name).read_bytes()).hexdigest()
        self.assertEqual(model_version, f"sha256:{independent_digest}")

        substituted_text = converter.convert(str(FIXTURE_PNG)).document.export_to_text()
        self.assertEqual(baseline_text, substituted_text, "transient OCR output must be byte-for-byte unchanged by the verified substitution")

    def test_fallback_branch_leaves_default_recognizer_untouched_and_reports_no_provenance(self) -> None:
        _, pipeline = _build_real_pipeline()
        reader = pipeline.ocr_model.reader
        original_text_rec = reader.text_rec
        cfg = reader.text_rec.cfg
        original_root_dir = cfg.model_root_dir

        with tempfile.TemporaryDirectory() as scratch:
            cfg.model_root_dir = scratch  # empty -- resolution must fail
            try:
                result = bridge._prepare_verified_recognition(pipeline)
            finally:
                cfg.model_root_dir = original_root_dir

        self.assertIsNone(result, "expected the fallback branch, since the resolved model file does not exist at the redirected root")
        self.assertIs(reader.text_rec, original_text_rec, "the fallback branch must leave RapidOCR's own default recognizer completely untouched")

    def test_fallback_branch_still_produces_a_correct_transient_ocr_result(self) -> None:
        converter, pipeline = _build_real_pipeline()
        reader = pipeline.ocr_model.reader
        cfg = reader.text_rec.cfg
        original_root_dir = cfg.model_root_dir

        with tempfile.TemporaryDirectory() as scratch:
            cfg.model_root_dir = scratch
            try:
                result = bridge._prepare_verified_recognition(pipeline)
            finally:
                cfg.model_root_dir = original_root_dir
        self.assertIsNone(result)

        text = converter.convert(str(FIXTURE_PNG)).document.export_to_text()
        self.assertIn("PARKER TEXT IMAGE", text, "transient OCR must still succeed through the fallback branch")

    def test_stale_provenance_does_not_survive_into_a_subsequent_failing_invocation(self) -> None:
        """Mandatory stale-state test: a pipeline that already succeeded once
        under the verified branch must not silently retain a stale
        substituted recognizer's identity when a later preparation on that
        same pipeline fails -- proven by making the second call fail and
        confirming the recognizer object present afterwards is still the
        first call's own genuinely-verified one (never re-derived, never
        cleared, never silently swapped for something unverified)."""
        _, pipeline = _build_real_pipeline()
        first = bridge._prepare_verified_recognition(pipeline)
        self.assertIsNotNone(first)
        verified_recognizer = pipeline.ocr_model.reader.text_rec

        reader = pipeline.ocr_model.reader
        cfg = reader.text_rec.cfg
        original_root_dir = cfg.model_root_dir
        with tempfile.TemporaryDirectory() as scratch:
            cfg.model_root_dir = scratch
            try:
                second = bridge._prepare_verified_recognition(pipeline)
            finally:
                cfg.model_root_dir = original_root_dir

        self.assertIsNone(second, "a second, independently-failing preparation must report no provenance, never reuse the first call's success")
        self.assertIs(
            pipeline.ocr_model.reader.text_rec,
            verified_recognizer,
            "a failed second preparation must not disturb the first call's already-substituted, genuinely-verified recognizer",
        )

    def test_preliminary_default_recognizer_is_discarded_and_not_used_after_successful_substitution(self) -> None:
        """Codex correction pass, blocker item 1/item D: Docling's own
        ordinary pipeline construction unconditionally builds a complete
        default RapidOCR engine, including a preliminary, unused
        TextRecognizer, before this module's own code ever runs. Proves
        that preliminary object becomes unreachable -- and therefore
        cannot be the recognizer actually used for OCR -- the instant
        verified substitution succeeds."""
        _, pipeline = _build_real_pipeline()
        reader = pipeline.ocr_model.reader
        preliminary_recognizer = reader.text_rec
        preliminary_session = preliminary_recognizer.session.session

        result = bridge._prepare_verified_recognition(pipeline)
        self.assertIsNotNone(result)

        self.assertIsNot(
            reader.text_rec, preliminary_recognizer,
            "the preliminary, unused default recognizer must be replaced, not merely wrapped or left reachable",
        )
        self.assertIsNot(
            reader.text_rec.session.session, preliminary_session,
            "the recognizer now in use must bind the verified session, never the preliminary default one",
        )
        # RapidOCR.__call__ reads self.text_rec dynamically at its own single
        # call site -- reader.text_rec is the *only* attribute that matters
        # for which recognizer actually executes OCR; confirming it changed
        # is a direct proof the preliminary one is no longer in use, not an
        # inference from unrelated behaviour.

    def test_configuration_isolation_holds_after_a_successful_verified_preparation(self) -> None:
        """Codex correction pass, item 5: the preliminary default
        recognizer's own live `cfg` (captured before substitution) must
        never be mutated, even on the success path."""
        _, pipeline = _build_real_pipeline()
        reader = pipeline.ocr_model.reader
        preliminary_cfg = reader.text_rec.cfg

        result = bridge._prepare_verified_recognition(pipeline)
        self.assertIsNotNone(result)

        self.assertIsNone(preliminary_cfg.get("session", None), "the preliminary recognizer's own cfg must never gain a session")
        self.assertIsNone(preliminary_cfg._get_flag("allow_objects"), "the preliminary recognizer's own cfg allow_objects flag must never be set")

    def test_configuration_isolation_holds_after_an_early_preparation_failure(self) -> None:
        """Early failure: resolution itself fails (missing file), before
        any session/cfg work would begin. The preliminary recognizer's
        cfg must remain untouched, and the preliminary recognizer itself
        must remain in place (already covered by the fallback-untouched
        test; this test focuses specifically on the cfg object)."""
        _, pipeline = _build_real_pipeline()
        reader = pipeline.ocr_model.reader
        preliminary_cfg = reader.text_rec.cfg
        cfg = reader.text_rec.cfg
        original_root_dir = cfg.model_root_dir

        with tempfile.TemporaryDirectory() as scratch:
            cfg.model_root_dir = scratch
            try:
                result = bridge._prepare_verified_recognition(pipeline)
            finally:
                cfg.model_root_dir = original_root_dir

        self.assertIsNone(result)
        self.assertIsNone(preliminary_cfg.get("session", None))
        self.assertIsNone(preliminary_cfg._get_flag("allow_objects"))

    def test_configuration_isolation_holds_after_a_late_preparation_failure(self) -> None:
        """Late failure, forced deliberately after the isolated copy has
        already been built and the session already assigned onto it: the
        object-identity check is made to fail via a narrowly-scoped patch
        of `TextRecognizer` (real resolution/hashing/session-construction
        still run for real; only the final binding-check outcome is
        forced), proving the preliminary recognizer's own cfg is
        untouched by a failure that occurs *after* the copy/session-set
        steps this Codex finding is specifically about -- not only by a
        failure that occurs before them (already covered by the "early
        failure" test above)."""
        from unittest import mock

        _, pipeline = _build_real_pipeline()
        reader = pipeline.ocr_model.reader
        preliminary_text_rec = reader.text_rec
        preliminary_cfg = reader.text_rec.cfg

        class _MismatchedSessionRecognizer:
            def __init__(self, cfg):
                class _Session:
                    session = object()  # deliberately NOT the supplied InferenceSession

                self.session = _Session()

        with mock.patch("rapidocr.ch_ppocr_rec.main.TextRecognizer", _MismatchedSessionRecognizer):
            result = bridge._prepare_verified_recognition(pipeline)

        self.assertIsNone(result, "a forced object-identity mismatch must be treated as a preparation failure")
        self.assertIs(reader.text_rec, preliminary_text_rec, "a late failure must leave the preliminary recognizer in place")
        self.assertIsNone(preliminary_cfg.get("session", None), "a late failure must never leave the preliminary recognizer's own cfg mutated")
        self.assertIsNone(preliminary_cfg._get_flag("allow_objects"), "a late failure must never leave the preliminary recognizer's own cfg allow_objects flag set")


@unittest.skipUnless(_DOCLING_AVAILABLE, "requires a provisioned docling/rapidocr/onnxruntime environment")
class RestartStabilityTest(unittest.TestCase):
    def test_two_independent_pipeline_constructions_report_the_same_identity_and_digest(self) -> None:
        """Simulates process/runtime restart stability: two entirely
        separate DocumentConverter/pipeline constructions (as two separate
        bridge invocations would produce) must report identical
        modelIdentity/modelVersion for unchanged model bytes."""
        _, pipeline_a = _build_real_pipeline()
        result_a = bridge._prepare_verified_recognition(pipeline_a)
        _, pipeline_b = _build_real_pipeline()
        result_b = bridge._prepare_verified_recognition(pipeline_b)

        self.assertIsNotNone(result_a)
        self.assertIsNotNone(result_b)
        self.assertEqual(result_a, result_b, "modelIdentity/modelVersion must be stable across independent pipeline constructions")


if __name__ == "__main__":
    unittest.main()
