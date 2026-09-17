from __future__ import annotations

import importlib.util
import json
import io
import sys
import tempfile
import unittest
import zipfile
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

    def test_ocr_recognised_text_is_preserved_in_representation(self):
        outcome = {
            "status": "recognised",
            "recognisedText": "Jane Doe\nProduct Manager\nAcme Corporation",
            "confidence": 0.99,
            "warnings": [],
            "mechanismVersion": "docling-test",
            "modelIdentity": "rapidocr-test",
            "modelVersion": "sha256:" + "a" * 64,
        }
        with patch.object(hermes, "run_docling", return_value=outcome):
            result, representation = hermes.make_result_and_representation(
                "bulk-test", "a" * 64, Path("images.jpg"), b"jpeg", 1, "images.jpg"
            )
        self.assertEqual(result["status"], "PASS")
        self.assertIsNotNone(representation)
        self.assertIn("Jane Doe", representation["recognisedText"])
        self.assertEqual(representation["originalFilename"], "images.jpg")
        self.assertEqual(representation["derivativeContentSha256"], hermes.sha256_bytes(representation["recognisedText"].encode()))

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

    def test_catalogue_routes_required_extensions_without_duplicate_lists(self):
        expected = {".txt", ".csv", ".pdf", ".docx", ".doc", ".xlsx", ".xls", ".eml", ".msg", ".rtf", ".jpg", ".jpeg", ".png", ".webp", ".tif", ".tiff"}
        self.assertTrue(all(hermes.definition_for_extension(extension) is not None for extension in expected))
        self.assertEqual(hermes.media_type_for(Path("mail.eml")), "message/rfc822")
        self.assertEqual(hermes.media_type_for(Path("scan.tiff")), "image/tiff")

    def test_all_emitted_processing_methods_use_the_typed_parker_vocabulary(self):
        emitted = {"DIRECT_TEXT_EXTRACTION", "STRUCTURED_DOCUMENT_EXTRACTION",
                   "STRUCTURED_SPREADSHEET_EXTRACTION", "STRUCTURED_EMAIL_EXTRACTION",
                   "OCR", "TIFF_FRAME_INSPECTION"}
        source = Path(hermes.__file__).read_text()
        for method in emitted:
            self.assertIn(f'"{method}"', source)

    def test_eml_metadata_survives_authoritative_processing_result(self):
        source = (b"From: sender@example.test\nTo: recipient@example.test\n"
                  b"Subject: Training Agreement\nDate: Tue, 1 Jan 2030 10:00:00 +0000\n"
                  b"MIME-Version: 1.0\nContent-Type: text/plain; charset=utf-8\n\nBody\n")
        result = hermes.make_result("bulk-test", "a" * 64, Path("message.eml"), source, 1)
        self.assertEqual(result["status"], "PASS")
        self.assertEqual(result["structuredRepresentation"]["from"], "sender@example.test")
        self.assertEqual(result["structuredRepresentation"]["to"], "recipient@example.test")
        self.assertEqual(result["structuredRepresentation"]["subject"], "Training Agreement")

    def test_xlsx_preserves_sheet_and_cell_coordinate(self):
        ns = "http://schemas.openxmlformats.org/spreadsheetml/2006/main"
        relns = "http://schemas.openxmlformats.org/officeDocument/2006/relationships"
        workbook = f'<workbook xmlns="{ns}" xmlns:r="{relns}"><sheets><sheet name="Payments" sheetId="1" r:id="rId1"/></sheets></workbook>'
        rels = '<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Target="worksheets/sheet1.xml"/></Relationships>'
        sheet = f'<worksheet xmlns="{ns}"><sheetData><row r="4"><c r="B4"><v>1250.00</v></c></row></sheetData></worksheet>'
        buffer = io.BytesIO()
        with zipfile.ZipFile(buffer, "w") as archive:
            archive.writestr("xl/workbook.xml", workbook)
            archive.writestr("xl/_rels/workbook.xml.rels", rels)
            archive.writestr("xl/worksheets/sheet1.xml", sheet)
        result = hermes.make_result("bulk-test", "a" * 64, Path("payments.xlsx"), buffer.getvalue(), 1)
        self.assertEqual(result["status"], "PASS")
        cell = result["structuredRepresentation"]["sheets"][0]["cells"][0]
        self.assertEqual(result["structuredRepresentation"]["sheets"][0]["name"], "Payments")
        self.assertEqual((cell["cell"], cell["value"]), ("B4", "1250.00"))

    def test_rtf_extracts_text_and_corrupt_rtf_fails_closed(self):
        result = hermes.make_result("bulk-test", "a" * 64, Path("note.rtf"), b"{\\rtf1\\ansi Training Agreement}", 1)
        self.assertEqual(result["status"], "PASS")
        self.assertIn("Training Agreement", result["structuredRepresentation"]["text"])
        bad = hermes.make_result("bulk-test", "a" * 64, Path("note.rtf"), b"not rtf", 1)
        self.assertEqual(bad["status"], "FAILED")

    def test_tiff_frames_are_counted_but_local_ocr_is_not_authoritative(self):
        # Little-endian TIFF with two empty IFDs linked together.
        data = bytearray(b"II" + (42).to_bytes(2, "little") + (8).to_bytes(4, "little"))
        data += (0).to_bytes(2, "little") + (26).to_bytes(4, "little")
        data += b"\x00" * (26 - len(data))
        data += (0).to_bytes(2, "little") + (0).to_bytes(4, "little")
        result = hermes.make_result("bulk-test", "a" * 64, Path("scan.tiff"), bytes(data), 1)
        self.assertEqual(result["status"], "REQUIRES_OCR")
        self.assertEqual(result["frameCount"], 2)
        self.assertIsNone(hermes.make_result_and_representation("bulk-test", "a" * 64, Path("scan.tiff"), bytes(data), 1)[1])

    def test_legacy_binary_core_formats_fail_closed_when_parser_unavailable(self):
        for extension in (".doc", ".xls", ".msg"):
            result = hermes.make_result("bulk-test", "a" * 64, Path("source" + extension), b"not a parser fixture", 1)
            self.assertEqual(result["status"], "FAILED")
            self.assertEqual(result["failure"]["kind"], "CORRUPT_SOURCE")

    def test_legacy_ole_container_is_admitted_to_parker_governed_parser(self):
        for extension in (".doc", ".xls", ".msg"):
            result = hermes.make_result("bulk-test", "a" * 64, Path("source" + extension), bytes.fromhex("D0CF11E0A1B11AE1") + b"container", 1)
            self.assertEqual(result["status"], "PASS")


class FakeParker:
    def __init__(self):
        self.results = []
        self.sources = []
        self.pending = []
        self.representations = []
        self.acquisitions = []
        self.source_response = (201, {"status": "INGESTED", "evidenceArtifactId": "evidence-test"})
        self.acquire_response = (200, {"status": "COMPLETED", "evidenceArtifactId": "evidence-test", "derivativeGenerationId": "external-generation"})

    def submit_result(self, batch_id, result):
        self.results.append((batch_id, result))
        return 201, {"status": "RECORDED"}

    def submit_source(self, batch_id, source_hash, data, filename, media):
        self.sources.append((batch_id, source_hash, data, filename, media))
        return self.source_response

    def acquire(self, evidence_artifact_id):
        self.acquisitions.append(evidence_artifact_id)
        return self.acquire_response

    def submit_ocr_representation(self, batch_id, source_hash, representation):
        self.representations.append((batch_id, source_hash, representation))
        return 201, {"status": "ADMITTED", "derivativeGenerationId": "generation-test"}

    def submit_pending_review_source(self, batch_id, source_hash, data, filename, media):
        self.pending.append((batch_id, source_hash, data, filename, media))
        return 201, {"status": "STORED"}


class RejectingParker(FakeParker):
    def submit_result(self, batch_id, result):
        self.results.append((batch_id, result))
        return 400, {"error": "malformed processing result", "detail": "controlled rejection"}


class SubmissionBoundaryTest(unittest.TestCase):
    def test_failed_processing_result_submission_preserves_safe_downstream_diagnostic(self):
        fake = RejectingParker()
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "message.eml"
            path.write_bytes(b"From: sender@example.test\nSubject: Test\n\nBody\n")
            item = hermes.process_one(fake, "bulk-test", path, 1)
        self.assertEqual(item.status, "PASS")
        self.assertEqual(item.governed_ingestion, "NOT_ATTEMPTED")
        self.assertEqual(item.result_submission, "HTTP_400")
        self.assertEqual(item.submission_error["httpStatus"], 400)
        self.assertEqual(item.submission_error["endpoint"], "/agent/ingestion-batches/bulk-test/processing-results")
        self.assertEqual(item.submission_error["batchId"], "bulk-test")
        self.assertEqual(item.submission_error["sourceSha256"], item.source_sha256)
        self.assertNotIn("Authorization", json.dumps(item.json()))

    def test_only_pass_submits_exact_source_bytes(self):
        fake = FakeParker()
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "clean.txt"
            path.write_bytes(b"exact bytes\n")
            item = hermes.process_one(fake, "bulk-test", path, 1)
        self.assertEqual(item.status, "PASS")
        self.assertEqual(item.governed_ingestion, "REGISTERED")
        self.assertEqual(fake.sources[0][2], b"exact bytes\n")
        self.assertEqual(fake.sources[0][1], hermes.sha256_bytes(b"exact bytes\n"))
        self.assertFalse(fake.acquisitions)

    def test_authorized_ocr_required_admission_continues_through_governed_acquire(self):
        fake = FakeParker()
        fake.source_response = (201, {"status": "REQUIRES_OCR", "processingState": "REQUIRES_OCR", "evidenceArtifactId": "evidence-scan"})
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "scan.pdf"
            path.write_bytes(b"scanned bytes")
            with patch.object(hermes, "run_docling", return_value={"status": "recognised", "recognisedText": "external candidate"}):
                item = hermes.process_one(fake, "bulk-test", path, 1)
        self.assertEqual(item.governed_ingestion, "ANALYSIS_READY")
        self.assertTrue(item.acquisition_attempted)
        self.assertEqual(item.acquisition_status, "COMPLETED")
        self.assertEqual(fake.acquisitions, ["evidence-scan"])

    def test_authorized_jpg_uses_the_same_governed_acquire_continuation(self):
        fake = FakeParker()
        fake.source_response = (201, {"status": "REQUIRES_OCR", "evidenceArtifactId": "evidence-jpg"})
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "scan.jpg"
            path.write_bytes(b"jpeg bytes")
            with patch.object(hermes, "run_docling", return_value={"status": "recognised", "recognisedText": "image text"}):
                item = hermes.process_one(fake, "bulk-test", path, 1)
        self.assertEqual(item.governed_ingestion, "ANALYSIS_READY")
        self.assertEqual(fake.acquisitions, ["evidence-jpg"])

    def test_review_required_png_and_webp_never_call_acquire(self):
        for extension in (".png", ".webp"):
            fake = FakeParker()
            with tempfile.TemporaryDirectory() as directory:
                path = Path(directory) / ("uncertain" + extension)
                path.write_bytes(b"image bytes")
                with patch.object(hermes, "run_docling", return_value={"status": "partial", "confidence": 0.4}):
                    item = hermes.process_one(fake, "bulk-test", path, 1)
            self.assertEqual(item.status, "REVIEW_REQUIRED")
            self.assertEqual(item.governed_ingestion, "BLOCKED")
            self.assertFalse(fake.acquisitions)

    def test_unauthorized_ocr_required_admission_stays_requires_ocr(self):
        fake = FakeParker()
        fake.source_response = (201, {"status": "REQUIRES_OCR", "evidenceArtifactId": "evidence-scan"})
        fake.acquire_response = (409, {"status": "AUTHORIZATION_REQUIRED", "evidenceArtifactId": "evidence-scan"})
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "scan.pdf"
            path.write_bytes(b"scanned bytes")
            with patch.object(hermes, "run_docling", return_value={"status": "recognised"}):
                item = hermes.process_one(fake, "bulk-test", path, 1)
        self.assertEqual(item.governed_ingestion, "REQUIRES_OCR")
        self.assertTrue(item.acquisition_attempted)
        self.assertEqual(item.acquisition_status, "AUTHORIZATION_REQUIRED")
        self.assertEqual(fake.acquisitions, ["evidence-scan"])

    def test_provider_unavailable_is_preserved_as_capability_unavailable(self):
        fake = FakeParker()
        fake.source_response = (201, {"status": "REQUIRES_OCR", "evidenceArtifactId": "evidence-scan"})
        fake.acquire_response = (409, {"status": "PROVIDER_NOT_READY", "evidenceArtifactId": "evidence-scan"})
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "scan.pdf"
            path.write_bytes(b"scanned bytes")
            with patch.object(hermes, "run_docling", return_value={"status": "recognised"}):
                item = hermes.process_one(fake, "bulk-test", path, 1)
        self.assertEqual(item.governed_ingestion, "CAPABILITY_UNAVAILABLE")
        self.assertEqual(item.acquisition_status, "PROVIDER_NOT_READY")
        self.assertEqual(item.acquisition_error["endpoint"], "/agent/evidence/evidence-scan/acquire")
        self.assertEqual(item.acquisition_error["httpStatus"], 409)

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
        self.assertFalse(fake.acquisitions)

    def test_pass_image_submits_source_then_ocr_representation(self):
        fake = FakeParker()
        outcome = {
            "status": "recognised",
            "recognisedText": "Jane Doe\nProduct Manager\nAcme Corporation",
            "confidence": 0.99,
            "warnings": [],
            "mechanismVersion": "docling-test",
            "modelIdentity": "rapidocr-test",
            "modelVersion": "sha256:" + "a" * 64,
        }
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "upload.jpg"
            path.write_bytes(b"jpeg bytes")
            with patch.object(hermes, "run_docling", return_value=outcome):
                item = hermes.process_one(fake, "bulk-test", path, 1, "images.jpg")
        self.assertEqual(item.governed_ingestion, "REGISTERED")
        self.assertEqual(fake.sources[0][3], "images.jpg")
        self.assertEqual(fake.representations[0][2]["evidenceArtifactId"], "evidence-test")
        self.assertEqual(fake.representations[0][2]["recognisedText"], outcome["recognisedText"])
        self.assertFalse(fake.acquisitions)


if __name__ == "__main__":
    unittest.main()
