from __future__ import annotations

import importlib.util
import json
import sys
import tempfile
import unittest
from unittest.mock import patch
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SCRIPT = ROOT / "tools" / "hermes_processing_ingest.py"
spec = importlib.util.spec_from_file_location("hermes_processing_ingest", SCRIPT)
assert spec and spec.loader
hermes = importlib.util.module_from_spec(spec)
sys.modules["hermes_processing_ingest"] = hermes
spec.loader.exec_module(hermes)


class ProcessingDecisionTest(unittest.TestCase):
    def test_text_is_pass_and_uses_direct_extraction(self):
        result = hermes.make_result("bulk-test", "a" * 64, Path("source.txt"), b"hello Parker\n", 1)
        self.assertEqual(result["status"], "PASS")
        self.assertEqual(result["methods"], ["DIRECT_TEXT_EXTRACTION"])
        self.assertNotIn("failure", result)
        self.assertNotIn("batchId", result)
        self.assertNotIn("caseId", result)

    def test_pass_serialization_omits_absent_optional_failure(self):
        result = hermes.make_result("bulk-test", "a" * 64, Path("source.txt"), b"hello Parker\n", 1)
        payload = json.dumps(result, separators=(",", ":"))
        self.assertNotIn('"failure":null', payload)
        self.assertNotIn("failure", result)

    def test_empty_text_fails_closed(self):
        result = hermes.make_result("bulk-test", "a" * 64, Path("source.txt"), b"\n", 1)
        self.assertEqual(result["status"], "FAILED")
        self.assertEqual(result["failure"]["kind"], "NO_READABLE_CONTENT")

    def test_docx_text_is_structured_extraction(self):
        # A malformed package must be classified as a source failure without
        # attempting to submit bytes for governed ingestion.
        result = hermes.make_result("bulk-test", "a" * 64, Path("source.docx"), b"not-a-docx", 1)
        self.assertEqual(result["status"], "FAILED")
        self.assertEqual(result["methods"], ["STRUCTURED_DOCUMENT_EXTRACTION"])
        self.assertEqual(result["failure"]["kind"], "CORRUPT_SOURCE")

    def test_unknown_extension_is_unsupported(self):
        result = hermes.make_result("bulk-test", "a" * 64, Path("source.bin"), b"bytes", 1)
        self.assertEqual(result["status"], "FAILED")
        self.assertEqual(result["failure"]["kind"], "UNSUPPORTED_FILE_FORMAT")

    def test_ocr_confidence_threshold_and_warnings_are_preserved(self):
        outcome = {
            "status": "partial",
            "confidence": 0.74,
            "reason": "page 3 was incomplete",
            "warnings": ["page 3 was incomplete"],
        }
        with patch.object(hermes, "run_docling", return_value=outcome):
            result = hermes.make_result("bulk-test", "a" * 64, Path("scan.pdf"), b"pdf", 1)
        self.assertEqual(result["status"], "REVIEW_REQUIRED")
        self.assertEqual(result["issues"][0]["observedConfidence"], 0.74)
        self.assertEqual(result["reviewConfidenceThreshold"], 0.80)
        self.assertEqual(result["processingCompleteness"], "PARTIAL")
        self.assertEqual(result["processingWarnings"], ["page 3 was incomplete"])
        self.assertNotIn("failure", result)

    def test_missing_ocr_confidence_stays_absent(self):
        outcome = {"status": "recognised", "warnings": []}
        with patch.object(hermes, "run_docling", return_value=outcome):
            result = hermes.make_result("bulk-test", "a" * 64, Path("scan.pdf"), b"pdf", 1)
        self.assertEqual(result["status"], "PASS")
        self.assertEqual(result["issues"], [])
        self.assertEqual(result["reviewConfidenceThreshold"], 0.80)
        self.assertNotIn("failure", result)

    def test_review_serialization_preserves_fractional_confidence_and_omits_absent_optionals(self):
        outcome = {"status": "recognised", "confidence": 0.8, "warnings": []}
        with patch.object(hermes, "run_docling", return_value=outcome):
            result = hermes.make_result("bulk-test", "a" * 64, Path("scan.pdf"), b"pdf", 1)
        payload = json.dumps(result, separators=(",", ":"))
        self.assertEqual(result["status"], "PASS")
        self.assertEqual(result["reviewConfidenceThreshold"], 0.80)
        self.assertNotIn("failure", result)
        self.assertNotIn(":null", payload)

    def test_failed_serialization_preserves_populated_failure(self):
        result = hermes.make_result("bulk-test", "a" * 64, Path("source.bin"), b"bytes", 1)
        payload = json.dumps(result, separators=(",", ":"))
        self.assertEqual(result["status"], "FAILED")
        self.assertEqual(result["failure"]["kind"], "UNSUPPORTED_FILE_FORMAT")
        self.assertIn('"failure":{"kind":"UNSUPPORTED_FILE_FORMAT"', payload)


class FakeParker:
    def __init__(self):
        self.results = []
        self.sources = []
        self.pending = []

    def submit_result(self, batch_id, result):
        self.results.append((batch_id, result))
        return 201, {"status": "RECORDED"}

    def submit_source(self, batch_id, source_hash, data, filename, media):
        self.sources.append((batch_id, source_hash, data, filename, media))
        return 201, {"status": "INGESTED"}

    def submit_pending_review_source(self, batch_id, source_hash, data, filename, media):
        self.pending.append((batch_id, source_hash, data, filename, media))
        return 201, {"status": "STORED"}


class SubmissionBoundaryTest(unittest.TestCase):
    def test_only_pass_submits_exact_source_bytes(self):
        fake = FakeParker()
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "clean.txt"
            path.write_bytes(b"exact bytes\n")
            item = hermes.process_one(fake, "bulk-test", path, 1)
        self.assertEqual(item.status, "PASS")
        self.assertEqual(item.governed_ingestion, "INGESTED")
        self.assertEqual(fake.sources[0][2], b"exact bytes\n")
        self.assertEqual(fake.sources[0][1], hermes.sha256_bytes(b"exact bytes\n"))

    def test_failed_result_is_submitted_but_source_is_not(self):
        fake = FakeParker()
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "bad.bin"
            path.write_bytes(b"unsupported")
            item = hermes.process_one(fake, "bulk-test", path, 1)
        self.assertEqual(item.status, "FAILED")
        self.assertEqual(item.governed_ingestion, "BLOCKED")
        self.assertEqual(len(fake.results), 1)
        self.assertEqual(fake.results[0][1]["failure"]["kind"], "UNSUPPORTED_FILE_FORMAT")
        self.assertEqual(fake.sources, [])

    def test_review_required_custodies_source_before_temp_scope_exits(self):
        fake = FakeParker()
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "uncertain.pdf"
            path.write_bytes(b"pending bytes")
            with patch.object(hermes, "run_docling", return_value={"status": "recognised", "confidence": 0.4}):
                item = hermes.process_one(fake, "bulk-test", path, 1)
        self.assertEqual(item.status, "REVIEW_REQUIRED")
        self.assertEqual(item.governed_ingestion, "BLOCKED")
        self.assertEqual(len(fake.pending), 1)
        self.assertEqual(fake.pending[0][2], b"pending bytes")
        self.assertEqual(fake.sources, [])


if __name__ == "__main__":
    unittest.main()
