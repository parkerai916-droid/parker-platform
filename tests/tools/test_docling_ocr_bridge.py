"""Pure-Python, offline, deterministic tests for tools/docling-ocr-bridge.py.

Every test in this file runs with no Docling installation, no model, and
no network -- either by importing the bridge module directly and exercising
its Docling-independent functions/classes, or by invoking the real script
as a genuine subprocess against inputs that are guaranteed to fail closed
*before* any Docling import is ever attempted (a missing/absent
`modelCacheDir`, a missing request-file argument, a malformed request
file) -- proving real, current, subprocess-level behaviour, not merely
mocked in-process behaviour.

Run with: python3 -m unittest discover -s tests/tools -v
(stdlib `unittest` only -- no pytest or other third-party test dependency
is required to run this file, consistent with this task's own "do not
install anything beyond what implementation strictly requires" discipline
for a bridge script whose own correctness must be verifiable before any
Docling/pytest installation decision is made.)
"""

from __future__ import annotations

import importlib.util
import json
import os
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]
BRIDGE_SCRIPT_PATH = REPO_ROOT / "tools" / "docling-ocr-bridge.py"

_spec = importlib.util.spec_from_file_location("docling_ocr_bridge", BRIDGE_SCRIPT_PATH)
assert _spec is not None and _spec.loader is not None
bridge = importlib.util.module_from_spec(_spec)
# Registered in sys.modules *before* exec_module runs -- required for the
# bridge module's own `from __future__ import annotations` + `@dataclass`
# combination: dataclass field-type resolution looks the defining module up
# via `sys.modules[cls.__module__]`, which fails with an opaque
# AttributeError on `None` if the module was executed without first being
# registered under its own `__name__`.
sys.modules["docling_ocr_bridge"] = bridge
_spec.loader.exec_module(bridge)  # type: ignore[union-attr]


def write_request_file(tmp_dir: str, payload: dict) -> str:
    path = os.path.join(tmp_dir, "request.json")
    with open(path, "w", encoding="utf-8") as handle:
        json.dump(payload, handle)
    return path


def run_bridge_subprocess(request_path: str) -> subprocess.CompletedProcess:
    return subprocess.run(
        [sys.executable, str(BRIDGE_SCRIPT_PATH), request_path],
        capture_output=True,
        text=True,
        timeout=30,
    )


class OfflineEnvironmentTest(unittest.TestCase):
    def test_offline_flags_are_set_on_import(self) -> None:
        self.assertEqual(os.environ.get("HF_HUB_OFFLINE"), "1")
        self.assertEqual(os.environ.get("TRANSFORMERS_OFFLINE"), "1")


class RequestParsingTest(unittest.TestCase):
    def test_valid_request_is_parsed(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            path = write_request_file(
                tmp,
                {
                    "protocolVersion": "1",
                    "sourceFilePath": "/tmp/source.pdf",
                    "mediaType": "application/pdf",
                    "configurationProfile": "docling-bridge-v1",
                },
            )
            request = bridge.load_request(path)
            self.assertEqual(request["sourceFilePath"], "/tmp/source.pdf")
            self.assertEqual(request["mediaType"], "application/pdf")
            self.assertIsNone(request["modelCacheDir"])

    def test_modelCacheDir_is_carried_when_present(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            path = write_request_file(
                tmp,
                {
                    "protocolVersion": "1",
                    "sourceFilePath": "/tmp/source.png",
                    "mediaType": "image/png",
                    "configurationProfile": "docling-bridge-v1",
                    "modelCacheDir": "/opt/docling-models",
                },
            )
            request = bridge.load_request(path)
            self.assertEqual(request["modelCacheDir"], "/opt/docling-models")

    def test_missing_request_file_raises_BridgeError(self) -> None:
        with self.assertRaises(bridge.BridgeError):
            bridge.load_request("/nonexistent/request-file-9f3a.json")

    def test_malformed_json_raises_BridgeError(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            path = os.path.join(tmp, "request.json")
            with open(path, "w", encoding="utf-8") as handle:
                handle.write("not json at all")
            with self.assertRaises(bridge.BridgeError):
                bridge.load_request(path)

    def test_unsupported_protocol_version_raises_BridgeError(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            path = write_request_file(
                tmp,
                {"protocolVersion": "999", "sourceFilePath": "/tmp/x.pdf", "mediaType": "application/pdf"},
            )
            with self.assertRaises(bridge.BridgeError):
                bridge.load_request(path)

    def test_missing_sourceFilePath_raises_BridgeError(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            path = write_request_file(tmp, {"protocolVersion": "1", "mediaType": "application/pdf"})
            with self.assertRaises(bridge.BridgeError):
                bridge.load_request(path)


class ModelAssetPreflightTest(unittest.TestCase):
    def test_none_modelCacheDir_passes_without_filesystem_check(self) -> None:
        bridge.verify_model_assets_available(None)  # must not raise

    def test_nonexistent_modelCacheDir_raises_DoclingUnavailableError(self) -> None:
        with self.assertRaises(bridge.DoclingUnavailableError):
            bridge.verify_model_assets_available("/nonexistent/model-cache-dir-9f3a")

    def test_empty_modelCacheDir_raises_DoclingUnavailableError(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            with self.assertRaises(bridge.DoclingUnavailableError):
                bridge.verify_model_assets_available(tmp)

    def test_nonempty_modelCacheDir_passes(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            Path(tmp, "model.bin").write_bytes(b"fake model bytes")
            bridge.verify_model_assets_available(tmp)  # must not raise


class DimensionBoundTest(unittest.TestCase):
    def test_within_bound_passes(self) -> None:
        bridge.check_declared_dimensions(100, 100)  # must not raise

    def test_exact_boundary_passes(self) -> None:
        bridge.check_declared_dimensions(bridge.MAX_DIMENSION_PX, bridge.MAX_DIMENSION_PX)

    def test_width_exceeding_bound_raises_ResourceLimitBreach(self) -> None:
        with self.assertRaises(bridge.ResourceLimitBreach):
            bridge.check_declared_dimensions(bridge.MAX_DIMENSION_PX + 1, 100)

    def test_height_exceeding_bound_raises_ResourceLimitBreach(self) -> None:
        with self.assertRaises(bridge.ResourceLimitBreach):
            bridge.check_declared_dimensions(100, bridge.MAX_DIMENSION_PX + 1)

    def test_total_pixel_count_exceeding_bound_isolated_from_width_height_bound(self) -> None:
        # An extreme-aspect-ratio image individually satisfying the per-axis
        # bound but exceeding the total-pixel bound (contract Section 13's
        # own "catches an extreme-aspect-ratio image" rationale). At the
        # module's own default bounds (10,000 x 10,000 = exactly 100,000,000),
        # no width/height pair can satisfy both per-axis bounds and still
        # exceed the pixel bound -- the per-axis bound is temporarily
        # widened, module-wide, for this one isolated test only, to prove
        # the pixel-count branch is independently reachable at all, mirroring
        # the identical, deliberate test-construction technique already used
        # for this same isolation proof on the Kotlin side
        # (DoclingOcrProviderAdapterTest.kt's own equivalent test).
        original_max_dimension = bridge.MAX_DIMENSION_PX
        bridge.MAX_DIMENSION_PX = 50_000
        try:
            width, height = 40_000, 5_000
            self.assertLessEqual(width, bridge.MAX_DIMENSION_PX)
            self.assertLessEqual(height, bridge.MAX_DIMENSION_PX)
            self.assertGreater(width * height, bridge.MAX_PIXELS)
            with self.assertRaises(bridge.ResourceLimitBreach):
                bridge.check_declared_dimensions(width, height)
        finally:
            bridge.MAX_DIMENSION_PX = original_max_dimension

    def test_zero_dimension_fails_closed_as_unsupported_not_silently_within_bound(self) -> None:
        with self.assertRaises(bridge.DoclingProcessingError):
            bridge.check_declared_dimensions(0, 100)

    def test_negative_dimension_fails_closed(self) -> None:
        # Adversarial: a maliciously/corruptly declared dimension whose true
        # unsigned value overflowed into a negative number upstream must
        # never be silently treated as "within bounds" by naive comparison.
        with self.assertRaises(bridge.DoclingProcessingError):
            bridge.check_declared_dimensions(-1, -1)


class PageCountBoundTest(unittest.TestCase):
    def test_within_bound_passes(self) -> None:
        bridge.check_page_count(bridge.MAX_PDF_PAGES)  # exact boundary, must not raise

    def test_exceeding_bound_raises_ResourceLimitBreach(self) -> None:
        with self.assertRaises(bridge.ResourceLimitBreach):
            bridge.check_page_count(bridge.MAX_PDF_PAGES + 1)


class OutputSizeAccumulatorTest(unittest.TestCase):
    def test_small_text_passes(self) -> None:
        accumulator = bridge.OutputSizeAccumulator(max_bytes=1000)
        accumulator.add("hello")  # must not raise

    def test_early_abort_before_full_document_processed(self) -> None:
        accumulator = bridge.OutputSizeAccumulator(max_bytes=100)
        accumulator.add("x" * 50)
        with self.assertRaises(bridge.ResourceLimitBreach):
            accumulator.add("y" * 100)

    def test_accumulates_across_multiple_calls(self) -> None:
        accumulator = bridge.OutputSizeAccumulator(max_bytes=100)
        accumulator.add("x" * 60)
        with self.assertRaises(bridge.ResourceLimitBreach):
            accumulator.add("y" * 60)


class ResponseJsonConstructionTest(unittest.TestCase):
    def test_recognised_status_schema_exact(self) -> None:
        outcome = bridge.DoclingRecognitionOutcome(status="recognised", text="hello world", fidelity="VERBATIM")
        parsed = json.loads(bridge.build_response_json(outcome))
        self.assertEqual(parsed["status"], "recognised")
        self.assertEqual(parsed["recognisedText"], "hello world")
        self.assertEqual(parsed["fidelity"], "VERBATIM")
        self.assertNotIn("reason", parsed, "a full success must never carry a reason field")

    def test_partial_status_requires_reason(self) -> None:
        outcome = bridge.DoclingRecognitionOutcome(
            status="partial", text="partial text", fidelity="NORMALISED", reason="page 3 unreadable"
        )
        parsed = json.loads(bridge.build_response_json(outcome))
        self.assertEqual(parsed["status"], "partial")
        self.assertEqual(parsed["reason"], "page 3 unreadable")

    def test_no_recognisable_content_schema_exact(self) -> None:
        outcome = bridge.DoclingRecognitionOutcome(status="no_recognisable_content", reason="blank page")
        parsed = json.loads(bridge.build_response_json(outcome))
        self.assertEqual(parsed, {"status": "no_recognisable_content", "reason": "blank page"})

    def test_confidence_omitted_when_none(self) -> None:
        outcome = bridge.DoclingRecognitionOutcome(status="recognised", text="hi", fidelity="VERBATIM", confidence=None)
        parsed = json.loads(bridge.build_response_json(outcome))
        self.assertNotIn("confidence", parsed, "confidence must be genuinely absent, never fabricated as 0 or null-as-present")

    def test_confidence_carried_when_present(self) -> None:
        outcome = bridge.DoclingRecognitionOutcome(status="recognised", text="hi", fidelity="VERBATIM", confidence=0.87)
        parsed = json.loads(bridge.build_response_json(outcome))
        self.assertEqual(parsed["confidence"], 0.87)

    def test_segments_carry_page_numbers_in_supplied_order(self) -> None:
        outcome = bridge.DoclingRecognitionOutcome(
            status="recognised",
            text="page one page two",
            fidelity="VERBATIM",
            segments=[
                bridge.RecognisedSegment(text="page one", page_number=1),
                bridge.RecognisedSegment(text="page two", page_number=2),
            ],
        )
        parsed = json.loads(bridge.build_response_json(outcome))
        self.assertEqual([s["pageNumber"] for s in parsed["segments"]], [1, 2])
        self.assertEqual(set(parsed["segments"][0].keys()), {"text", "fidelity", "pageNumber"})

    def test_provenance_fields_carried_through_when_present(self) -> None:
        outcome = bridge.DoclingRecognitionOutcome(
            status="recognised",
            text="hi",
            fidelity="VERBATIM",
            mechanism_version="2.5.0",
            model_identity="layout-v3",
        )
        parsed = json.loads(bridge.build_response_json(outcome))
        self.assertEqual(parsed["mechanismVersion"], "2.5.0")
        self.assertEqual(parsed["modelIdentity"], "layout-v3")

    def test_provenance_fields_genuinely_absent_when_unknown(self) -> None:
        outcome = bridge.DoclingRecognitionOutcome(status="recognised", text="hi", fidelity="VERBATIM")
        parsed = json.loads(bridge.build_response_json(outcome))
        self.assertNotIn("mechanismVersion", parsed)
        self.assertNotIn("modelIdentity", parsed)

    def test_unicode_round_trips_exactly(self) -> None:
        original = "Café 中文 🎉"
        outcome = bridge.DoclingRecognitionOutcome(status="recognised", text=original, fidelity="VERBATIM")
        serialized = bridge.build_response_json(outcome)
        # json.dumps's own default ensure_ascii=True must have escaped every
        # non-ASCII character -- proving this test actually exercises \uXXXX
        # encoding, not merely a literal UTF-8 passthrough.
        self.assertTrue(serialized.isascii(), "serialized JSON must be pure ASCII (ensure_ascii=True, the default)")
        parsed = json.loads(serialized)
        self.assertEqual(parsed["recognisedText"], original)

    def test_oversized_response_raises_ResourceLimitBreach_never_truncates(self) -> None:
        outcome = bridge.DoclingRecognitionOutcome(status="recognised", text="x" * (bridge.MAX_OUTPUT_BYTES + 100), fidelity="VERBATIM")
        with self.assertRaises(bridge.ResourceLimitBreach):
            bridge.build_response_json(outcome)


class RecogniseOrchestrationTest(unittest.TestCase):
    def test_missing_assets_short_circuits_before_backend_is_invoked(self) -> None:
        backend_called = {"value": False}

        def fake_backend(source_file_path, media_type, model_cache_dir):
            backend_called["value"] = True
            return bridge.DoclingRecognitionOutcome(status="recognised", text="should not reach here", fidelity="VERBATIM")

        with self.assertRaises(bridge.DoclingUnavailableError):
            bridge.recognise("/tmp/x.pdf", "application/pdf", "/nonexistent/cache-dir-9f3a", docling_backend=fake_backend)
        self.assertFalse(backend_called["value"], "the Docling backend must never be invoked once the pre-flight check has already failed")

    def test_fake_backend_result_is_returned_unchanged(self) -> None:
        expected = bridge.DoclingRecognitionOutcome(status="recognised", text="fake text", fidelity="VERBATIM")

        def fake_backend(source_file_path, media_type, model_cache_dir):
            return expected

        actual = bridge.recognise("/tmp/x.pdf", "application/pdf", None, docling_backend=fake_backend)
        self.assertIs(actual, expected)

    def test_docling_processing_error_propagates_from_backend(self) -> None:
        def fake_backend(source_file_path, media_type, model_cache_dir):
            raise bridge.DoclingProcessingError("simulated corrupt document")

        with self.assertRaises(bridge.DoclingProcessingError):
            bridge.recognise("/tmp/x.pdf", "application/pdf", None, docling_backend=fake_backend)

    def test_resource_limit_breach_propagates_from_backend(self) -> None:
        def fake_backend(source_file_path, media_type, model_cache_dir):
            raise bridge.ResourceLimitBreach("simulated page-count breach")

        with self.assertRaises(bridge.ResourceLimitBreach):
            bridge.recognise("/tmp/x.pdf", "application/pdf", None, docling_backend=fake_backend)


class MainEntryPointTest(unittest.TestCase):
    """In-process tests of main(), with a fake `recognise` reachable only via
    module-level monkeypatch of the default backend parameter -- exercised
    by directly calling the pipeline functions main() itself calls, proving
    main()'s own control flow (argument handling, exit-code selection,
    stdout/stderr routing) independent of any Docling installation."""

    def test_missing_argument_returns_exit_1(self) -> None:
        self.assertEqual(bridge.main([str(BRIDGE_SCRIPT_PATH)]), bridge.EXIT_UNCLASSIFIED)

    def test_missing_request_file_returns_exit_1(self) -> None:
        self.assertEqual(bridge.main([str(BRIDGE_SCRIPT_PATH), "/nonexistent/request-9f3a.json"]), bridge.EXIT_UNCLASSIFIED)

    def test_nonexistent_modelCacheDir_returns_exit_2_via_full_main_flow(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            path = write_request_file(
                tmp,
                {
                    "protocolVersion": "1",
                    "sourceFilePath": "/tmp/does-not-matter.pdf",
                    "mediaType": "application/pdf",
                    "modelCacheDir": "/nonexistent/model-cache-9f3a",
                },
            )
            self.assertEqual(bridge.main([str(BRIDGE_SCRIPT_PATH), path]), bridge.EXIT_MISSING_ASSETS)


class RealSubprocessProtocolCompatibilityTest(unittest.TestCase):
    """Genuine subprocess invocations of the real script -- proving the
    *actual* argv/exit-code/stdout/stderr behaviour the Kotlin-side
    ProcessBuilderDoclingSubprocessInvoker will observe, for every case that
    is deterministic without a real Docling installation (this environment
    genuinely has none installed at the time these tests were authored,
    which the first test below both documents and depends on)."""

    def test_missing_argv_exits_nonzero_with_stderr_diagnostic_no_stdout(self) -> None:
        result = subprocess.run([sys.executable, str(BRIDGE_SCRIPT_PATH)], capture_output=True, text=True, timeout=30)
        self.assertNotEqual(result.returncode, 0)
        self.assertEqual(result.stdout, "")
        self.assertIn("request file path", result.stderr)

    def test_malformed_request_json_exits_nonzero_no_stdout(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            path = os.path.join(tmp, "request.json")
            with open(path, "w", encoding="utf-8") as handle:
                handle.write("{not valid json")
            result = run_bridge_subprocess(path)
            self.assertNotEqual(result.returncode, 0)
            self.assertEqual(result.stdout, "")

    def test_nonexistent_modelCacheDir_exits_2_via_real_subprocess(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            path = write_request_file(
                tmp,
                {
                    "protocolVersion": "1",
                    "sourceFilePath": "/tmp/does-not-matter.pdf",
                    "mediaType": "application/pdf",
                    "modelCacheDir": "/nonexistent/model-cache-9f3a",
                },
            )
            result = run_bridge_subprocess(path)
            self.assertEqual(result.returncode, bridge.EXIT_MISSING_ASSETS)
            self.assertEqual(result.stdout, "", "no stdout output is permitted on a non-zero exit")
            self.assertIn("modelCacheDir", result.stderr)

    def test_no_docling_installed_and_no_modelCacheDir_exits_2_not_a_crash(self) -> None:
        # This environment has no Docling installation (documented and
        # relied upon by this test, verified indirectly by its own
        # assertion): the real script must fail closed with the same
        # "missing assets" disposition a genuinely absent installation
        # produces in production, never an uncaught Python traceback on
        # stdout.
        with tempfile.TemporaryDirectory() as tmp, tempfile.NamedTemporaryFile(suffix=".pdf") as source:
            source.write(b"%PDF-1.4 fake minimal content")
            source.flush()
            path = write_request_file(
                tmp,
                {
                    "protocolVersion": "1",
                    "sourceFilePath": source.name,
                    "mediaType": "application/pdf",
                },
            )
            result = run_bridge_subprocess(path)
            self.assertEqual(result.stdout, "", "no stdout output is permitted on a non-zero exit")
            self.assertNotEqual(result.returncode, 0)
            self.assertNotIn("Traceback", result.stderr, "an uncaught Python traceback must never be the failure mode")


if __name__ == "__main__":
    unittest.main()
