import hashlib
import json
import os
import tempfile
import unittest
import zipfile
from unittest import mock
from pathlib import Path

from tools.parker_ingestion_prep import HANDOFF_EXIT_CODES, PrepConfig, PrepError, PrepJob


class ParkerIngestionPrepTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.root = Path(self.temp.name)
        self.workspace = self.root / "prep"
        self.source = self.root / "source"
        self.source.mkdir()

    def tearDown(self):
        self.temp.cleanup()

    def job(self, source=None, job_id="JOB-TEST-1", case_id="case-a", config=None):
        return PrepJob(source or self.source, self.workspace, job_id, case_id, config)

    @staticmethod
    def digest(path):
        return hashlib.sha256(path.read_bytes()).hexdigest()

    def test_aggregate_exit_codes_distinguish_clean_exceptions_and_not_ready(self):
        self.assertEqual(HANDOFF_EXIT_CODES, {"READY": 0, "READY_WITH_EXCEPTIONS": 2, "NOT_READY": 3})

    def test_copy_verification_dedupes_content_preserves_occurrences_and_originals(self):
        (self.source / "nested").mkdir()
        (self.source / "nested" / "same name.txt").write_text("same bytes\n", encoding="utf-8")
        (self.source / "same name.txt").write_text("same bytes\n", encoding="utf-8")
        (self.source / "different").mkdir()
        (self.source / "different" / "same name.txt").write_text("different bytes", encoding="utf-8")
        (self.source / "é-empty.txt").write_bytes(b"")
        (self.source / "same-size.txt").write_text("different!", encoding="utf-8")
        before = {path: self.digest(path) for path in self.source.rglob("*") if path.is_file()}

        job = self.job()
        try:
            result = job.run()
        finally:
            job.close()

        occurrences = result["manifest"]["occurrences"]
        by_relative = {item["relative_path"]: item for item in occurrences}
        self.assertEqual(by_relative["same name.txt"]["preparation_status"], "READY")
        self.assertEqual(by_relative["nested/same name.txt"]["preparation_status"], "DUPLICATE_IN_JOB")
        self.assertEqual(by_relative["different/same name.txt"]["preparation_status"], "READY")
        self.assertEqual(by_relative["é-empty.txt"]["preparation_status"], "READY")
        self.assertEqual(by_relative["same-size.txt"]["preparation_status"], "READY")
        self.assertTrue((self.workspace / "jobs/JOB-TEST-1/source_copy/same name.txt").exists())
        self.assertFalse((self.workspace / "jobs/JOB-TEST-1/source_copy/nested/same name.txt").exists())
        self.assertEqual(result["reconciliation"]["unaccounted"], 0)
        self.assertEqual(result["handoff"]["handoffStatus"], "READY")
        self.assertEqual(result["handoff"]["reconciliationStatus"], "COMPLETE")
        self.assertEqual(result["handoff"]["readyItemCount"], len(result["handoff"]["readyContent"]))
        after = {path: self.digest(path) for path in self.source.rglob("*") if path.is_file()}
        self.assertEqual(before, after)

    def test_source_symlink_is_reviewed_without_following_or_modifying_target(self):
        target = self.source / "target.txt"
        target.write_text("do not alter", encoding="utf-8")
        link = self.source / "link.txt"
        try:
            link.symlink_to(target.name)
        except (OSError, NotImplementedError):
            self.skipTest("symlinks are unavailable on this platform")
        original = target.read_bytes()
        job = self.job(job_id="JOB-SYMLINK")
        try:
            result = job.run()
        finally:
            job.close()
        linked = next(item for item in result["manifest"]["occurrences"] if item["relative_path"] == "link.txt")
        self.assertEqual(linked["preparation_status"], "REVIEW_REQUIRED")
        self.assertEqual(linked["reason_code"], "UNSAFE_SOURCE_LINK")
        self.assertEqual(target.read_bytes(), original)

    def test_cross_job_duplicate_and_cross_case_context_are_retained(self):
        source_a = self.root / "case-a"
        source_b = self.root / "case-b"
        source_a.mkdir()
        source_b.mkdir()
        (source_a / "first.bin").write_bytes(b"identical content")
        (source_b / "second.bin").write_bytes(b"identical content")

        first = PrepJob(source_a, self.workspace, "JOB-A", "case-a")
        try:
            first.run()
        finally:
            first.close()
        second = PrepJob(source_b, self.workspace, "JOB-B", "case-b")
        try:
            result = second.run()
        finally:
            second.close()
        item = result["manifest"]["occurrences"][0]
        self.assertEqual(item["preparation_status"], "DUPLICATE_EXISTING_PREP")
        self.assertFalse((self.workspace / "jobs/JOB-B/source_copy/second.bin").exists())
        index = json.loads((self.workspace / "jobs/JOB-B/reports/handoff.json").read_text())
        self.assertEqual(index["handoffStatus"], "READY")
        import sqlite3
        with sqlite3.connect(self.workspace / "prep-content-index.db") as database:
            cases = database.execute("SELECT case_ids FROM content_index").fetchone()[0]
        self.assertEqual(json.loads(cases), ["case-a", "case-b"])

    def test_trusted_existing_evidence_hash_is_duplicate_without_new_copy(self):
        content = b"already governed bytes"
        path = self.source / "already.bin"
        path.write_bytes(content)
        digest = hashlib.sha256(content).hexdigest()
        job = self.job(job_id="JOB-EVIDENCE")
        try:
            job.register_existing_evidence(digest, "evidence-123", len(content), "case-a")
            result = job.run()
        finally:
            job.close()
        item = result["manifest"]["occurrences"][0]
        self.assertEqual(item["preparation_status"], "DUPLICATE_EXISTING_EVIDENCE")
        self.assertEqual(item["duplicate_target"], "evidence:evidence-123")
        self.assertFalse((self.workspace / "jobs/JOB-EVIDENCE/source_copy/already.bin").exists())

    def test_zip_extracts_from_copy_and_dedupes_children_with_archive_provenance(self):
        (self.source / "loose.txt").write_bytes(b"archive child")
        archive = self.source / "bundle.zip"
        with zipfile.ZipFile(archive, "w") as output:
            output.writestr("folder/inside.txt", b"archive child")
            output.writestr("unique.txt", b"unique child")
        source_archive_digest = self.digest(archive)
        job = self.job(job_id="JOB-ZIP")
        try:
            result = job.run()
        finally:
            job.close()
        items = result["manifest"]["occurrences"]
        children = [item for item in items if item["kind"] == "ARCHIVE_MEMBER" and not item["is_directory"]]
        self.assertEqual(len(children), 2)
        self.assertTrue(any(item["preparation_status"] == "DUPLICATE_IN_JOB" for item in items))
        unique = next(item for item in children if item["filename"] == "unique.txt")
        self.assertEqual(unique["preparation_status"], "READY")
        self.assertEqual(unique["parent_occurrence_id"], next(item["occurrence_id"] for item in items if item["filename"] == "bundle.zip"))
        copied_archive = self.workspace / "jobs/JOB-ZIP/source_copy/bundle.zip"
        self.assertEqual(self.digest(copied_archive), source_archive_digest)
        ready_paths = [Path(item["path"]) for item in result["handoff"]["readyContent"]]
        self.assertTrue(ready_paths)
        self.assertFalse(any(path.suffix.lower() == ".zip" for path in ready_paths))
        self.assertEqual(result["reconciliation"]["unaccounted"], 0)

    def test_zip_traversal_drive_path_link_and_corrupt_archives_fail_closed(self):
        archive = self.source / "unsafe.zip"
        with zipfile.ZipFile(archive, "w") as output:
            output.writestr("../escape.txt", b"escape")
            output.writestr("C:/drive.txt", b"drive")
            output.writestr("safe.txt", b"safe")
        corrupt = self.source / "corrupt.zip"
        corrupt.write_bytes(b"not a zip")
        outside = self.workspace / "escape.txt"
        job = self.job(job_id="JOB-UNSAFE")
        try:
            result = job.run()
        finally:
            job.close()
        items = result["manifest"]["occurrences"]
        self.assertFalse(outside.exists())
        self.assertTrue(any(item["reason_code"] == "UNSAFE_ARCHIVE_PATH" for item in items))
        corrupt_item = next(item for item in items if item["filename"] == "corrupt.zip")
        self.assertEqual(corrupt_item["preparation_status"], "REVIEW_REQUIRED")
        self.assertEqual(corrupt_item["reason_code"], "ARCHIVE_CORRUPT")
        self.assertEqual(result["reconciliation"]["unaccounted"], 0)
        self.assertEqual(result["handoff"]["handoffStatus"], "READY_WITH_EXCEPTIONS")

    def test_mixed_completed_job_hands_off_ready_items_with_exceptions(self):
        (self.source / "ready.txt").write_text("ready", encoding="utf-8")
        link = self.source / "review-link.txt"
        try:
            link.symlink_to("ready.txt")
        except (OSError, NotImplementedError):
            self.skipTest("symlinks are unavailable on this platform")
        job = self.job(job_id="JOB-MIXED")
        try:
            result = job.run()
        finally:
            job.close()
        handoff = result["handoff"]
        self.assertEqual(handoff["jobStatus"], "COMPLETE")
        self.assertEqual(handoff["reconciliationStatus"], "COMPLETE")
        self.assertEqual(handoff["handoffStatus"], "READY_WITH_EXCEPTIONS")
        self.assertEqual(handoff["readyItemCount"], 1)
        self.assertEqual(handoff["reviewRequiredCount"], 1)
        self.assertEqual(handoff["failedCount"], 0)
        self.assertEqual(handoff["unaccountedCount"], 0)
        self.assertEqual(len(handoff["readyContent"]), 1)
        self.assertEqual(handoff["readyItemCount"], len(handoff["readyContent"]))
        self.assertTrue(all(Path(item["path"]).is_file() for item in handoff["readyContent"]))
        self.assertEqual(len(list((self.workspace / "jobs/JOB-MIXED/ready").iterdir())), 1)

    def test_unreconciled_job_is_not_ready(self):
        (self.source / "item.txt").write_text("item", encoding="utf-8")
        job = self.job(job_id="JOB-UNRECONCILED")
        try:
            job.run()
            job.db.execute("UPDATE occurrence SET preparation_status='UNKNOWN'")
            job.db.commit()
            result = job.write_outputs()
        finally:
            job.close()
        self.assertEqual(result["handoff"]["jobStatus"], "COMPLETE")
        self.assertEqual(result["handoff"]["reconciliationStatus"], "INCOMPLETE")
        self.assertEqual(result["handoff"]["handoffStatus"], "NOT_READY")
        self.assertEqual(result["handoff"]["unaccountedCount"], 1)
        self.assertEqual(result["handoff"]["readyItemCount"], 0)
        self.assertEqual(result["handoff"]["readyContent"], [])
        self.assertEqual(list((self.workspace / "jobs/JOB-UNRECONCILED/ready").iterdir()), [])

    def test_missing_prepared_file_cannot_create_ready_count_list_divergence(self):
        path = self.source / "prepared.txt"
        path.write_text("prepared", encoding="utf-8")
        job = self.job(job_id="JOB-MISSING-READY")
        try:
            first = job.run()
            ready_source = Path(first["manifest"]["occurrences"][0]["working_copy_path"])
            ready_source.unlink()
            result = job.write_outputs()
        finally:
            job.close()
        self.assertEqual(result["handoff"]["handoffStatus"], "READY_WITH_EXCEPTIONS")
        self.assertEqual(result["handoff"]["readyItemCount"], len(result["handoff"]["readyContent"]))
        self.assertEqual(result["handoff"]["readyContent"], [])
        item = result["manifest"]["occurrences"][0]
        self.assertEqual(item["preparation_status"], "FAILED")
        self.assertEqual(item["reason_code"], "COPY_FAILED")

    def test_encrypted_archive_members_are_accounted_for_as_review(self):
        archive = self.source / "encrypted.zip"
        with zipfile.ZipFile(archive, "w") as output:
            info = zipfile.ZipInfo("secret.txt")
            output.writestr(info, b"secret")
        with zipfile.ZipFile(archive) as input_archive:
            encrypted_info = input_archive.infolist()[0]
        encrypted_info.flag_bits |= 0x1
        job = self.job(job_id="JOB-ENCRYPTED")
        try:
            with mock.patch.object(zipfile.ZipFile, "infolist", return_value=[encrypted_info]):
                result = job.run()
        finally:
            job.close()
        child = next(item for item in result["manifest"]["occurrences"] if item["kind"] == "ARCHIVE_MEMBER")
        self.assertEqual(child["reason_code"], "PASSWORD_PROTECTED_ARCHIVE")
        self.assertEqual(child["preparation_status"], "REVIEW_REQUIRED")
        self.assertEqual(result["reconciliation"]["unaccounted"], 0)

    def test_archive_entry_limit_reviews_every_member(self):
        archive = self.source / "many.zip"
        with zipfile.ZipFile(archive, "w") as output:
            output.writestr("one.txt", b"one")
            output.writestr("two.txt", b"two")
        job = self.job(job_id="JOB-ENTRY-LIMIT", config=PrepConfig(archive_max_entries=1))
        try:
            result = job.run()
        finally:
            job.close()
        members = [item for item in result["manifest"]["occurrences"] if item["kind"] == "ARCHIVE_MEMBER"]
        self.assertEqual(len(members), 2)
        self.assertTrue(all(item["reason_code"] == "ARCHIVE_ENTRY_LIMIT_EXCEEDED" for item in members))
        self.assertEqual(result["reconciliation"]["unaccounted"], 0)

    def test_nested_archive_depth_limit_and_restart_are_idempotent(self):
        nested = self.root / "level.zip"
        for number in range(5, 0, -1):
            current = self.root / f"level-{number}.zip"
            with zipfile.ZipFile(current, "w") as output:
                if number == 5:
                    output.writestr("leaf.txt", b"leaf")
                else:
                    output.write(nested, "nested.zip")
            nested = current
        (self.source / "nested.zip").write_bytes(nested.read_bytes())
        job = self.job(job_id="JOB-RESTART", config=PrepConfig(archive_max_depth=3))
        try:
            first = job.run()
        finally:
            job.close()
        second_job = self.job(job_id="JOB-RESTART", config=PrepConfig(archive_max_depth=3))
        try:
            second = second_job.run()
        finally:
            second_job.close()
        self.assertEqual(first["reconciliation"]["unaccounted"], 0)
        self.assertEqual(second["reconciliation"]["unaccounted"], 0)
        self.assertEqual(first["handoff"]["readyItemCount"], len(first["handoff"]["readyContent"]))
        self.assertEqual(second["handoff"]["readyItemCount"], len(second["handoff"]["readyContent"]))
        self.assertTrue(any(item["reason_code"] == "NESTING_LIMIT_EXCEEDED" for item in second["manifest"]["occurrences"]))
        self.assertEqual(len(first["manifest"]["occurrences"]), len(second["manifest"]["occurrences"]))
        import sqlite3
        with sqlite3.connect(self.workspace / "prep-content-index.db") as database:
            self.assertEqual(database.execute("SELECT COUNT(*) FROM content_index").fetchone()[0],
                             len({item["source_sha256"] for item in first["manifest"]["occurrences"] if item["source_sha256"]}))

    def test_copy_hash_mismatch_fails_closed(self):
        path = self.source / "mismatch.txt"
        path.write_text("original", encoding="utf-8")
        job = self.job(job_id="JOB-MISMATCH")
        occurrence_id = job._new_occurrence("source:mismatch.txt", "SOURCE_FILE", path, "mismatch.txt", path.name, ".txt")
        job._current_occurrence = occurrence_id
        try:
            with self.assertRaises(PrepError) as context:
                job._atomic_copy(path, job.source_copy / "mismatch.txt", "0" * 64)
            self.assertEqual(context.exception.reason_code, "HASH_MISMATCH")
        finally:
            job.close()

    def test_interrupted_copy_can_resume_without_duplicate_canonical_record(self):
        path = self.source / "resume.txt"
        path.write_text("resume me", encoding="utf-8")
        first = self.job(job_id="JOB-RESUME")
        try:
            with mock.patch.object(PrepJob, "_atomic_copy", side_effect=PrepError("COPY_FAILED", "simulated interruption")):
                result = first.run()
        finally:
            first.close()
        failed = next(item for item in result["manifest"]["occurrences"] if item["filename"] == "resume.txt")
        self.assertEqual(failed["preparation_status"], "FAILED")
        resumed = self.job(job_id="JOB-RESUME")
        try:
            result = resumed.run()
        finally:
            resumed.close()
        item = next(item for item in result["manifest"]["occurrences"] if item["filename"] == "resume.txt")
        self.assertEqual(item["preparation_status"], "READY")
        import sqlite3
        with sqlite3.connect(self.workspace / "prep-content-index.db") as database:
            self.assertEqual(database.execute("SELECT COUNT(*) FROM content_index").fetchone()[0], 1)


if __name__ == "__main__":
    unittest.main()
