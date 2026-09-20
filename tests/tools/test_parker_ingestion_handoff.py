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
