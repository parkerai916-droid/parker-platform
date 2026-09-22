import json
import tempfile
import unittest
from pathlib import Path
from unittest import mock

from tools import parker_ingestion_handoff as handoff
from tools.hermes_processing_ingest import ProcessedFile, sha256_bytes
from tools.parker_ingestion_prep import PrepJob


class ParkerIngestionHandoffTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.root = Path(self.temp.name)
        self.source = self.root / "source"
        self.workspace = self.root / "workspace"
        self.source.mkdir()

    def tearDown(self):
        self.temp.cleanup()

    def test_case_id_normalizer_accepts_raw_and_exact_owner_wrapper(self):
        self.assertEqual(handoff.canonical_case_id("case-alpha-1"), "case-alpha-1")
        self.assertEqual(handoff.canonical_case_id("CaseId(value=case-alpha-1)"), "case-alpha-1")

    def test_case_id_normalizer_rejects_different_or_malformed_values(self):
        self.assertNotEqual(handoff.canonical_case_id("CaseId(value=case-alpha-1)"), "case-beta-1")
        for value in ("CaseId(value=case-alpha-1", "CaseId(value=other-1)", " case-alpha-1", "case-"):
            self.assertIsNone(handoff.canonical_case_id(value))

    def test_validate_case_accepts_serialized_owner_case_id_and_returns_raw_id(self):
        client = handoff.OwnerParkerClient("http://owner", "cookie", 1)
        with mock.patch.object(client, "request", return_value=(200, {
            "cases": [{"caseId": "CaseId(value=case-alpha-1)", "caseName": "Case Alpha"}]
        })):
            case = client.validate_case("case-alpha-1")
        self.assertEqual(case["caseId"], "case-alpha-1")
        self.assertEqual(case["caseName"], "Case Alpha")

    def test_import_accepts_wrapped_owner_case_id_representation(self):
        self.make_job()
        handoff_path = self.workspace / "jobs/JOB-HANDOFF/reports/handoff.json"

        def owner_request(path, method="GET", body=None):
            if path == "/owner/cases":
                return 200, {"cases": [{"caseId": "CaseId(value=case-a)", "caseName": "Case A"}]}
            if path == "/owner/ingestion-batches":
                return 201, {"batchId": "batch-wrapped"}
            return 201, {"status": "CREATED", "associationId": "association-a", "occurrenceId": "occurrence-a"}

        def fake_process(client, batch_id, path, timeout, original_filename=None):
            digest = sha256_bytes(path.read_bytes())
            client.evidence_by_hash[digest] = "evidence-a"
            client.source_outcome_by_hash[digest] = "ANALYSIS_READY"
            client.source_admission_by_hash[digest] = "CREATED"
            return ProcessedFile(original_filename, digest, "PASS", ["DIRECT_TEXT_EXTRACTION"], "RECORDED", "ANALYSIS_READY")

        with mock.patch.object(handoff.OwnerParkerClient, "request", side_effect=owner_request), \
             mock.patch.object(handoff, "ParkerClient"), \
             mock.patch.object(handoff, "process_one", side_effect=fake_process):
            report = handoff.import_handoff(handoff_path, "http://owner", "cookie", "http://agent", "token")
        self.assertEqual(report["status"], "COMPLETE")
        self.assertEqual(report["caseId"], "case-a")
        self.assertEqual(report["batchId"], "batch-wrapped")
        self.assertEqual(report["sourceAdmissionsCreatedCount"], 1)
        self.assertNotIn("importedNewCount", report)
        self.assertNotIn("reusedExistingContentCount", report)

    def make_job(self):
        (self.source / "a-ready.txt").write_text("ready content", encoding="utf-8")
        (self.source / "z-duplicate.txt").write_text("ready content", encoding="utf-8")
        job = PrepJob(self.source, self.workspace, "JOB-HANDOFF", "case-a")
        result = job.run()
        job.close()
        return result

    def test_ready_with_exceptions_imports_only_ready_content_and_is_idempotent(self):
        result = self.make_job()
        handoff_path = self.workspace / "jobs/JOB-HANDOFF/reports/handoff.json"
        value = json.loads(handoff_path.read_text())
        # A duplicate is represented in the manifest but cannot enter readyContent.
        self.assertEqual(value["readyItemCount"], 1)
        self.assertEqual(len(value["readyContent"]), 1)

        class Owner:
            def __init__(self, *args): self.authorisations = 0; self.occurrences = []
            def validate_case(self, case_id): return {"caseId": case_id, "caseName": "Case A"}
            def authorise_batch(self, case_id): self.authorisations += 1; return "batch-a"
            def register_occurrence(self, batch_id, evidence_artifact_id, occurrence):
                self.occurrences.append((batch_id, evidence_artifact_id, occurrence))
                return 201, {"status": "CREATED", "associationId": "association-a", "occurrenceId": "occurrence-a"}

        calls = []

        def fake_process(client, batch_id, path, timeout, original_filename=None):
            digest = sha256_bytes(path.read_bytes())
            client.evidence_by_hash[digest] = "evidence-a"
            calls.append((batch_id, path, original_filename))
            return ProcessedFile(original_filename, digest, "PASS", ["DIRECT_TEXT_EXTRACTION"], "RECORDED", "ANALYSIS_READY")

        with mock.patch.object(handoff, "OwnerParkerClient", Owner), \
             mock.patch.object(handoff, "ParkerClient"), \
             mock.patch.object(handoff, "process_one", side_effect=fake_process):
            first = handoff.import_handoff(handoff_path, "http://owner", "cookie", "http://agent", "token")
            second = handoff.import_handoff(handoff_path, "http://owner", "cookie", "http://agent", "token")

        self.assertEqual(first["status"], "COMPLETE")
        self.assertEqual(first["importedCount"], 1)
        self.assertEqual(second["importedCount"], 1)
        self.assertEqual(len(calls), 1)
        self.assertEqual(calls[0][2], "a-ready.txt")
        self.assertTrue(calls[0][1].suffix == ".txt")
        imported = json.loads((self.workspace / "jobs/JOB-HANDOFF/reports/handoff-import.json").read_text())
        self.assertEqual(imported["items"][0]["prepProvenance"]["relativePath"], "a-ready.txt")
        self.assertEqual(imported["items"][0]["prepProvenance"]["caseId"], "case-a")
        self.assertEqual(imported["occurrencesCreatedCount"] + imported["occurrencesAlreadyPresentCount"], 1)

    def test_source_admission_reporting_uses_http_outcomes_not_content_status(self):
        class SourceClient:
            def submit_source(self, batch_id, source_hash, data, filename, media):
                return self.code, {"status": self.status, "evidenceArtifactId": "evidence"}

        client = handoff.RecordingParkerClient(SourceClient())
        for code, status, expected in ((201, "ANALYSIS_READY", "CREATED"),
                                       (200, "ANALYSIS_READY", "ALREADY_PRESENT"),
                                       (202, "REQUIRES_OCR", "REQUIRES_OCR")):
            client.client.code = code
            client.client.status = status
            client.submit_source("batch", str(code), b"content", "file.txt", "text/plain")
            self.assertEqual(client.source_admission_by_hash[str(code)], expected)
            self.assertEqual(client.source_outcome_by_hash[str(code)], status)

    def test_ocr_required_admission_records_evidence_identity_for_occurrence_registration(self):
        class SourceClient:
            def submit_ocr_required_source(self, batch_id, source_hash, data, filename, media):
                return 202, {"status": "REQUIRES_OCR", "evidenceArtifactId": "evidence-docx"}

        client = handoff.RecordingParkerClient(SourceClient())
        client.submit_ocr_required_source("batch", "d" * 64, b"docx", "image-only.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
        self.assertEqual(client.evidence_by_hash["d" * 64], "evidence-docx")
        self.assertEqual(client.source_outcome_by_hash["d" * 64], "REQUIRES_OCR")
        self.assertEqual(client.source_admission_by_hash["d" * 64], "REQUIRES_OCR")

    def test_import_status_progress_is_durable_and_content_free(self):
        self.make_job()
        handoff_path = self.workspace / "jobs/JOB-HANDOFF/reports/handoff.json"
        status = handoff.claim_import(handoff_path)
        self.assertEqual(status["state"], "IMPORTING")
        self.assertEqual(status["readyCount"], 1)
        with mock.patch.object(handoff, "_hash_file", side_effect=AssertionError("status must not hash")):
            status = handoff.import_status(handoff_path)
        self.assertEqual(status["processedCount"], 0)
        self.assertIsNone(status["latestOccurrenceId"])

    def test_import_status_reports_partial_and_complete_with_exceptions(self):
        self.make_job()
        handoff_path = self.workspace / "jobs/JOB-HANDOFF/reports/handoff.json"
        job_root = self.workspace / "jobs/JOB-HANDOFF"
        ledger = handoff._open_ledger(job_root)
        try:
            ledger.execute("""INSERT INTO handoff_import
                (occurrence_id, job_id, case_id, sha256, batch_id, status, updated_at)
                VALUES (?,?,?,?,?,?,?)""",
                           ("occ-1", "JOB-HANDOFF", "case-a", "a" * 64, "batch-a", "FAILED", handoff.now()))
            ledger.commit()
            status = handoff._update_import_state(ledger, "JOB-HANDOFF", 2, "IMPORTING")
            self.assertEqual(status["state"], "IMPORTING")
            self.assertEqual(status["processedCount"], 1)
            self.assertEqual(status["failedCount"], 1)
            self.assertEqual(status["latestOccurrenceId"], "occ-1")
            status = handoff._update_import_state(ledger, "JOB-HANDOFF", 2, "COMPLETE_WITH_EXCEPTIONS", handoff.now())
            self.assertEqual(status["state"], "COMPLETE_WITH_EXCEPTIONS")
            self.assertIsNotNone(status["completedAt"])
        finally:
            ledger.close()

    def test_not_ready_is_rejected_before_owner_or_agent_boundary(self):
        result = self.make_job()
        handoff_path = self.workspace / "jobs/JOB-HANDOFF/reports/handoff.json"
        value = json.loads(handoff_path.read_text())
        value["handoffStatus"] = "NOT_READY"
        handoff_path.write_text(json.dumps(value), encoding="utf-8")
        with self.assertRaises(handoff.HandoffError), mock.patch.object(handoff, "OwnerParkerClient") as owner:
            handoff.import_handoff(handoff_path, "http://owner", "cookie", "http://agent", "token")
        owner.assert_not_called()

    def test_missing_or_unassigned_case_is_rejected(self):
        result = self.make_job()
        handoff_path = self.workspace / "jobs/JOB-HANDOFF/reports/handoff.json"
        value = json.loads(handoff_path.read_text())
        value["caseId"] = "unassigned"
        handoff_path.write_text(json.dumps(value), encoding="utf-8")
        with self.assertRaises(handoff.HandoffError):
            handoff.validate_handoff(handoff_path)

    def test_ready_with_exceptions_is_accepted_but_unaccounted_and_count_mismatch_fail_closed(self):
        self.make_job()
        handoff_path = self.workspace / "jobs/JOB-HANDOFF/reports/handoff.json"
        value = json.loads(handoff_path.read_text())
        value["handoffStatus"] = "READY_WITH_EXCEPTIONS"
        value["reviewRequiredCount"] = 1
        value["failedCount"] = 1
        handoff_path.write_text(json.dumps(value), encoding="utf-8")
        handoff.validate_handoff(handoff_path)

        value["readyItemCount"] += 1
        handoff_path.write_text(json.dumps(value), encoding="utf-8")
        with self.assertRaises(handoff.HandoffError):
            handoff.validate_handoff(handoff_path)

        value["readyItemCount"] -= 1
        value["unaccountedCount"] = 1
        handoff_path.write_text(json.dumps(value), encoding="utf-8")
        with self.assertRaises(handoff.HandoffError):
            handoff.validate_handoff(handoff_path)

    def test_modified_ready_file_is_rejected(self):
        self.make_job()
        handoff_path = self.workspace / "jobs/JOB-HANDOFF/reports/handoff.json"
        value = json.loads(handoff_path.read_text())
        Path(value["readyContent"][0]["path"]).write_text("tampered", encoding="utf-8")
        with self.assertRaises(handoff.HandoffError):
            handoff.validate_handoff(handoff_path)

    def test_incomplete_reconciliation_is_rejected(self):
        self.make_job()
        handoff_path = self.workspace / "jobs/JOB-HANDOFF/reports/handoff.json"
        value = json.loads(handoff_path.read_text())
        value["reconciliationStatus"] = "INCOMPLETE"
        handoff_path.write_text(json.dumps(value), encoding="utf-8")
        with self.assertRaises(handoff.HandoffError):
            handoff.validate_handoff(handoff_path)

    def test_case_mismatch_in_occurrence_provenance_is_rejected(self):
        self.make_job()
        handoff_path = self.workspace / "jobs/JOB-HANDOFF/reports/handoff.json"
        value = json.loads(handoff_path.read_text())
        manifest_path = Path(value["manifestPath"])
        manifest = json.loads(manifest_path.read_text())
        manifest["occurrences"][0]["case_id"] = "different-case"
        manifest_path.write_text(json.dumps(manifest), encoding="utf-8")
        with self.assertRaises(handoff.HandoffError):
            handoff.validate_handoff(handoff_path)


if __name__ == "__main__":
    unittest.main()
