#!/usr/bin/env python3
"""Fail-closed Parker-side preparation for evidence before Hermes ingestion.

This tool only discovers, hashes, copies, safely extracts, inventories, and
deduplicates bytes.  It does not submit evidence, assign cases, OCR, parse
documents, acquire evidence, or make a governance decision.  The output
handoff contains only verified unique preparation items for the existing
Parker/Hermes operator path.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import os
import secrets
import sqlite3
import stat
import time
import uuid
import zipfile
from dataclasses import dataclass
from datetime import datetime, timezone
from pathlib import Path, PurePosixPath


PREP_STATES = frozenset({
    "DISCOVERED", "HASHED", "DUPLICATE_IN_JOB", "DUPLICATE_EXISTING_PREP",
    "DUPLICATE_EXISTING_EVIDENCE", "COPYING", "COPIED", "HASH_VERIFIED",
    "ARCHIVE_EXTRACTING", "ARCHIVE_EXTRACTED", "READY", "REVIEW_REQUIRED",
    "FAILED", "ARCHIVE_MEMBER_DIRECTORY",
})
ACCOUNTED_STATES = PREP_STATES
ARCHIVE_SUFFIXES = frozenset({".zip"})
HANDOFF_EXIT_CODES = {
    "READY": 0,
    "READY_WITH_EXCEPTIONS": 2,
    "NOT_READY": 3,
}


def aggregate_handoff_status(unaccounted: int, review_required: int, failed: int) -> str:
    """Return the job-level handoff state without changing item dispositions."""
    if unaccounted:
        return "NOT_READY"
    if review_required or failed:
        return "READY_WITH_EXCEPTIONS"
    return "READY"


@dataclass(frozen=True)
class PrepConfig:
    archive_max_depth: int = 3
    archive_max_entries: int = 10_000
    archive_max_uncompressed_bytes: int = 1_073_741_824
    copy_worker_count: int = 1
    hash_worker_count: int = 1
    dedupe_enabled: bool = True

    def __post_init__(self):
        if self.archive_max_depth < 0:
            raise ValueError("archive_max_depth must not be negative")
        if self.archive_max_entries < 1:
            raise ValueError("archive_max_entries must be positive")
        if self.archive_max_uncompressed_bytes < 1:
            raise ValueError("archive_max_uncompressed_bytes must be positive")
        if self.copy_worker_count < 1 or self.hash_worker_count < 1:
            raise ValueError("worker counts must be positive")


def utc_now() -> str:
    return datetime.now(timezone.utc).isoformat(timespec="microseconds")


def sha256_file(path: Path, chunk_size: int = 1024 * 1024) -> tuple[str, int]:
    digest = hashlib.sha256()
    size = 0
    with path.open("rb") as source:
        while True:
            chunk = source.read(chunk_size)
            if not chunk:
                break
            digest.update(chunk)
            size += len(chunk)
    return digest.hexdigest(), size


def safe_json(value) -> str:
    return json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(",", ":"))


class PrepError(RuntimeError):
    def __init__(self, reason_code: str, detail: str):
        super().__init__(detail)
        self.reason_code = reason_code
        self.detail = detail


class PrepJob:
    """One restartable preparation job and its durable content index."""

    def __init__(
        self,
        source_root: Path,
        workspace: Path,
        job_id: str,
        case_id: str | None = None,
        config: PrepConfig | None = None,
    ):
        self.source_root = Path(source_root).expanduser().resolve(strict=True)
        if not self.source_root.is_dir():
            raise ValueError("source_root must be a directory")
        self.workspace = Path(workspace).expanduser().absolute().resolve()
        self.job_id = job_id
        self.case_id = case_id
        self.config = config or PrepConfig()
        self.job_root = self.workspace / "jobs" / job_id
        self.source_copy = self.job_root / "source_copy"
        self.extracted = self.job_root / "extracted"
        self.ready = self.job_root / "ready"
        self.manifests = self.job_root / "manifests"
        self.reports = self.job_root / "reports"
        self.logs = self.job_root / "logs"
        self.job_db_path = self.job_root / "prep.db"
        self.index_db_path = self.workspace / "prep-content-index.db"
        if self.source_root == self.workspace or self.workspace in self.source_root.parents:
            raise ValueError("source_root must not be the Parker preparation workspace")
        for directory in (self.source_copy, self.extracted, self.ready, self.manifests, self.reports, self.logs):
            directory.mkdir(parents=True, exist_ok=True)
        self.db = sqlite3.connect(self.job_db_path, timeout=30)
        self.index = sqlite3.connect(self.index_db_path, timeout=30)
        self.db.execute("PRAGMA journal_mode=WAL")
        self.index.execute("PRAGMA journal_mode=WAL")
        self._create_schema()

    def close(self):
        self.db.close()
        self.index.close()

    def _create_schema(self):
        self.db.executescript(
            """
            CREATE TABLE IF NOT EXISTS job (
              job_id TEXT PRIMARY KEY, source_root TEXT NOT NULL, case_id TEXT,
              created_at TEXT NOT NULL, updated_at TEXT NOT NULL, status TEXT NOT NULL
            );
            CREATE TABLE IF NOT EXISTS occurrence (
              occurrence_id TEXT PRIMARY KEY,
              occurrence_key TEXT NOT NULL UNIQUE,
              kind TEXT NOT NULL,
              job_id TEXT NOT NULL,
              case_id TEXT,
              source_path TEXT NOT NULL,
              relative_path TEXT NOT NULL,
              filename TEXT NOT NULL,
              extension TEXT NOT NULL,
              byte_size INTEGER,
              source_sha256 TEXT,
              discovery_timestamp TEXT NOT NULL,
              duplicate_status TEXT,
              duplicate_target TEXT,
              canonical_content_id TEXT,
              copy_status TEXT,
              working_copy_path TEXT,
              working_copy_sha256 TEXT,
              preparation_status TEXT NOT NULL,
              reason_code TEXT,
              detail TEXT,
              parent_occurrence_id TEXT,
              archive_member_path TEXT,
              extracted_path TEXT,
              member_index INTEGER,
              is_directory INTEGER NOT NULL DEFAULT 0,
              archive_depth INTEGER NOT NULL DEFAULT 0
            );
            CREATE INDEX IF NOT EXISTS occurrence_status ON occurrence(preparation_status);
            CREATE INDEX IF NOT EXISTS occurrence_hash ON occurrence(source_sha256);
            """
        )
        self.index.executescript(
            """
            CREATE TABLE IF NOT EXISTS content_index (
              sha256 TEXT PRIMARY KEY,
              canonical_content_id TEXT NOT NULL,
              canonical_job_id TEXT NOT NULL,
              canonical_occurrence_id TEXT NOT NULL,
              canonical_path TEXT,
              canonical_evidence_id TEXT,
              first_seen_job_id TEXT NOT NULL,
              case_ids TEXT NOT NULL,
              byte_size INTEGER NOT NULL,
              status TEXT NOT NULL,
              first_seen_at TEXT NOT NULL
            );
            CREATE INDEX IF NOT EXISTS content_index_job ON content_index(canonical_job_id);
            """
        )
        now = utc_now()
        self.db.execute(
            "INSERT OR IGNORE INTO job VALUES (?, ?, ?, ?, ?, ?)",
            (self.job_id, str(self.source_root), self.case_id, now, now, "DISCOVERED"),
        )
        self.db.commit()
        self.index.commit()

    def _touch_job(self, status: str):
        self.db.execute("UPDATE job SET status=?, updated_at=? WHERE job_id=?", (status, utc_now(), self.job_id))
        self.db.commit()

    def _new_occurrence(self, key: str, kind: str, source_path: Path, relative_path: str,
                        filename: str, extension: str, parent_id: str | None = None,
                        archive_member_path: str | None = None, member_index: int | None = None,
                        is_directory: bool = False, archive_depth: int = 0) -> str:
        row = self.db.execute("SELECT occurrence_id FROM occurrence WHERE occurrence_key=?", (key,)).fetchone()
        if row:
            return row[0]
        occurrence_id = "prep-occ-" + uuid.uuid4().hex
        self.db.execute(
            """INSERT INTO occurrence(
              occurrence_id,occurrence_key,kind,job_id,case_id,source_path,relative_path,filename,
              extension,discovery_timestamp,preparation_status,parent_occurrence_id,archive_member_path,
              member_index,is_directory,archive_depth
            ) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)""",
            (occurrence_id, key, kind, self.job_id, self.case_id, str(source_path), relative_path,
             filename, extension, utc_now(), "DISCOVERED", parent_id, archive_member_path,
             member_index, int(is_directory), archive_depth),
        )
        self.db.commit()
        return occurrence_id

    def _update(self, occurrence_id: str, **fields):
        allowed = {
            "byte_size", "source_sha256", "duplicate_status", "duplicate_target", "canonical_content_id",
            "copy_status", "working_copy_path", "working_copy_sha256", "preparation_status",
            "reason_code", "detail", "extracted_path", "archive_depth", "is_directory",
        }
        if not fields or not set(fields).issubset(allowed):
            raise ValueError("invalid occurrence update field")
        assignments = ", ".join(f"{field}=?" for field in fields)
        self.db.execute(f"UPDATE occurrence SET {assignments} WHERE occurrence_id=?", (*fields.values(), occurrence_id))
        self.db.commit()

    def _occurrence(self, occurrence_id: str) -> dict:
        cursor = self.db.execute("SELECT * FROM occurrence WHERE occurrence_id=?", (occurrence_id,))
        row = cursor.fetchone()
        if row is None:
            raise KeyError(occurrence_id)
        return dict(zip((column[0] for column in cursor.description), row))

    def _index_row(self, digest: str):
        cursor = self.index.execute("SELECT * FROM content_index WHERE sha256=?", (digest,))
        row = cursor.fetchone()
        return dict(zip((column[0] for column in cursor.description), row)) if row else None

    def register_existing_evidence(self, digest: str, evidence_id: str, byte_size: int,
                                   case_id: str | None = None):
        """Load a trusted Parker hash export when Parker provides one.

        This is intentionally explicit: the prep tool does not guess or query
        production evidence storage.  The caller must supply the trusted
        Parker-owned hash/evidence export.
        """
        if len(digest) != 64 or any(character not in "0123456789abcdef" for character in digest):
            raise ValueError("digest must be lowercase SHA-256")
        now = utc_now()
        self.index.execute(
            """INSERT OR IGNORE INTO content_index
              (sha256,canonical_content_id,canonical_job_id,canonical_occurrence_id,canonical_path,
               canonical_evidence_id,first_seen_job_id,case_ids,byte_size,status,first_seen_at)
              VALUES(?,?,?,?,?,?,?,?,?,?,?)""",
            (digest, "evidence:" + evidence_id, "EXISTING_EVIDENCE", "EXISTING_EVIDENCE",
             None, evidence_id, "EXISTING_EVIDENCE", safe_json([case_id] if case_id else []),
             byte_size, "EXISTING_EVIDENCE", now),
        )
        self.index.commit()

    def _claim_or_duplicate(self, occurrence_id: str, digest: str, size: int) -> bool:
        if not self.config.dedupe_enabled:
            return True
        existing = self._index_row(digest)
        if existing is not None and existing["canonical_occurrence_id"] != occurrence_id:
            if existing["status"] == "EXISTING_EVIDENCE":
                state = "DUPLICATE_EXISTING_EVIDENCE"
            elif existing["canonical_job_id"] == self.job_id:
                state = "DUPLICATE_IN_JOB"
            else:
                state = "DUPLICATE_EXISTING_PREP"
            self._update(occurrence_id, source_sha256=digest, byte_size=size,
                         duplicate_status=state, duplicate_target=existing["canonical_content_id"],
                         canonical_content_id=existing["canonical_content_id"], preparation_status=state)
            if self.case_id and self.case_id not in json.loads(existing["case_ids"]):
                cases = json.loads(existing["case_ids"])
                cases.append(self.case_id)
                self.index.execute("UPDATE content_index SET case_ids=? WHERE sha256=?", (safe_json(cases), digest))
                self.index.commit()
            return False
        return True

    def _register_canonical(self, occurrence_id: str, digest: str, size: int, path: Path, status: str = "READY"):
        content_id = "prep-content-" + digest
        self.index.execute(
            """INSERT OR IGNORE INTO content_index
              (sha256,canonical_content_id,canonical_job_id,canonical_occurrence_id,canonical_path,
               canonical_evidence_id,first_seen_job_id,case_ids,byte_size,status,first_seen_at)
              VALUES(?,?,?,?,?,?,?,?,?,?,?)""",
            (digest, content_id, self.job_id, occurrence_id, str(path), None, self.job_id,
             safe_json([self.case_id] if self.case_id else []), size, status, utc_now()),
        )
        self.index.commit()
        existing = self._index_row(digest)
        if existing is None or existing["canonical_occurrence_id"] != occurrence_id:
            raise PrepError("DUPLICATE_RACE", "another preparation claimed this SHA-256 during registration")
        self._update(occurrence_id, canonical_content_id=content_id)

    def _safe_destination(self, root: Path, relative: str) -> Path:
        normalized = relative.replace("\\", "/")
        path = PurePosixPath(normalized)
        drive_qualified = bool(path.parts) and ":" in path.parts[0]
        if not normalized or path.is_absolute() or drive_qualified:
            raise PrepError("UNSAFE_ARCHIVE_PATH", f"unsafe archive path: {relative!r}")
        if any(part in ("", ".", "..") for part in path.parts):
            raise PrepError("UNSAFE_ARCHIVE_PATH", f"unsafe archive path: {relative!r}")
        destination = (root / Path(*path.parts)).resolve()
        if destination != root.resolve() and root.resolve() not in destination.parents:
            raise PrepError("UNSAFE_ARCHIVE_PATH", f"archive path escapes extraction root: {relative!r}")
        return destination

    def _atomic_copy(self, source: Path, destination: Path, expected_digest: str) -> str:
        destination.parent.mkdir(parents=True, exist_ok=True)
        temporary = destination.with_name(destination.name + ".part-" + secrets.token_hex(8))
        try:
            self._update(self._current_occurrence, copy_status="COPYING", preparation_status="COPYING")
            digest = hashlib.sha256()
            with source.open("rb") as input_file, temporary.open("wb") as output_file:
                while True:
                    chunk = input_file.read(1024 * 1024)
                    if not chunk:
                        break
                    digest.update(chunk)
                    output_file.write(chunk)
                output_file.flush()
                os.fsync(output_file.fileno())
            copied_digest = digest.hexdigest()
            if copied_digest != expected_digest:
                raise PrepError("HASH_MISMATCH", f"working copy hash {copied_digest} != source hash {expected_digest}")
            os.replace(temporary, destination)
            return copied_digest
        except PrepError:
            temporary.unlink(missing_ok=True)
            raise
        except (OSError, ValueError) as error:
            temporary.unlink(missing_ok=True)
            raise PrepError("COPY_FAILED", str(error)) from error

    def _process_source(self, occurrence_id: str, path: Path, relative: str):
        self._current_occurrence = occurrence_id
        try:
            if path.is_symlink():
                raise PrepError("UNSAFE_SOURCE_LINK", "symbolic-link source is not processed")
            if not path.is_file():
                raise PrepError("SOURCE_UNREADABLE", "source is not a regular file")
            digest, size = sha256_file(path)
            self._update(occurrence_id, source_sha256=digest, byte_size=size, preparation_status="HASHED")
            if not self._claim_or_duplicate(occurrence_id, digest, size):
                return
            current = self._occurrence(occurrence_id)
            if current["preparation_status"] in ("READY", "ARCHIVE_EXTRACTED") and current["working_copy_path"]:
                working_copy = Path(current["working_copy_path"])
                if working_copy.is_file() and current["working_copy_sha256"] == digest:
                    return
            destination = self.source_copy / Path(relative)
            copied_digest = self._atomic_copy(path, destination, digest)
            self._update(occurrence_id, copy_status="COPIED", working_copy_path=str(destination),
                         working_copy_sha256=copied_digest, preparation_status="HASH_VERIFIED")
            self._register_canonical(occurrence_id, digest, size, destination, "HASH_VERIFIED")
            if path.suffix.lower() in ARCHIVE_SUFFIXES:
                self._process_archive(occurrence_id, destination, relative, 1)
            else:
                self._update(occurrence_id, preparation_status="READY")
        except PrepError as error:
            self._update(occurrence_id, preparation_status="REVIEW_REQUIRED" if error.reason_code.startswith(("UNSAFE_", "ARCHIVE_", "NESTING_", "PASSWORD_")) else "FAILED",
                         reason_code=error.reason_code, detail=error.detail)
        except (OSError, ValueError) as error:
            self._update(occurrence_id, preparation_status="FAILED", reason_code="SOURCE_UNREADABLE", detail=str(error))

    def _member_is_link(self, info: zipfile.ZipInfo) -> bool:
        mode = (info.external_attr >> 16) & 0xFFFF
        return stat.S_ISLNK(mode)

    def _process_archive(self, parent_id: str, archive_path: Path, archive_relative: str, depth: int):
        self._update(parent_id, preparation_status="ARCHIVE_EXTRACTING")
        if depth > self.config.archive_max_depth:
            raise PrepError("NESTING_LIMIT_EXCEEDED", f"archive nesting depth {depth} exceeds configured maximum")
        try:
            with zipfile.ZipFile(archive_path, "r") as archive:
                infos = archive.infolist()
                extraction_root = self.extracted / ("occ-" + parent_id)
                extraction_root.mkdir(parents=True, exist_ok=True)
                members = []
                for index, info in enumerate(infos):
                    member_name = info.filename
                    child_id = self._new_occurrence(
                        f"archive:{parent_id}:{index}", "ARCHIVE_MEMBER", Path(self._occurrence(parent_id)["source_path"]),
                        archive_relative + "::" + member_name, Path(member_name).name or member_name,
                        Path(member_name).suffix.lower(), parent_id, member_name, index,
                        info.is_dir(), depth,
                    )
                    members.append((index, info, member_name, child_id))

                def reject_members(error: PrepError):
                    for _, _, _, child_id in members:
                        self._update(child_id, preparation_status="REVIEW_REQUIRED",
                                     reason_code=error.reason_code, detail=error.detail)
                    raise error

                if len(infos) > self.config.archive_max_entries:
                    reject_members(PrepError("ARCHIVE_ENTRY_LIMIT_EXCEEDED", "archive contains too many entries"))
                declared_size = sum(max(0, info.file_size) for info in infos)
                if declared_size > self.config.archive_max_uncompressed_bytes:
                    reject_members(PrepError("ARCHIVE_SIZE_LIMIT_EXCEEDED", "declared archive expansion exceeds limit"))
                if any(info.flag_bits & 0x1 for info in infos):
                    reject_members(PrepError("PASSWORD_PROTECTED_ARCHIVE", "encrypted/password-protected archive member"))
                member_names: set[str] = set()
                archive_had_review = False
                for index, info, member_name, child_id in members:
                    try:
                        if member_name in member_names:
                            raise PrepError("ARCHIVE_DUPLICATE_MEMBER_NAME", f"duplicate archive member name: {member_name!r}")
                        member_names.add(member_name)
                        if info.is_dir():
                            self._update(child_id, preparation_status="ARCHIVE_MEMBER_DIRECTORY", byte_size=0)
                            continue
                        if self._member_is_link(info):
                            raise PrepError("UNSAFE_ARCHIVE_LINK", f"symbolic/link archive member: {member_name!r}")
                        destination = self._safe_destination(extraction_root, member_name)
                        destination.parent.mkdir(parents=True, exist_ok=True)
                        temporary = destination.with_name(destination.name + ".part-" + secrets.token_hex(8))
                        digest = hashlib.sha256()
                        size = 0
                        with archive.open(info, "r") as input_file, temporary.open("wb") as output_file:
                            while True:
                                chunk = input_file.read(1024 * 1024)
                                if not chunk:
                                    break
                                size += len(chunk)
                                if size > self.config.archive_max_uncompressed_bytes:
                                    raise PrepError("ARCHIVE_SIZE_LIMIT_EXCEEDED", "archive member expansion exceeds limit")
                                digest.update(chunk)
                                output_file.write(chunk)
                            output_file.flush()
                            os.fsync(output_file.fileno())
                        digest_hex = digest.hexdigest()
                        self._update(child_id, source_sha256=digest_hex, byte_size=size,
                                     extracted_path=str(destination), working_copy_sha256=digest_hex,
                                     copy_status="COPIED", preparation_status="HASHED")
                        if not self._claim_or_duplicate(child_id, digest_hex, size):
                            temporary.unlink(missing_ok=True)
                            destination.unlink(missing_ok=True)
                            continue
                        os.replace(temporary, destination)
                        self._register_canonical(child_id, digest_hex, size, destination, "HASH_VERIFIED")
                        self._update(child_id, preparation_status="HASH_VERIFIED")
                        if destination.suffix.lower() in ARCHIVE_SUFFIXES:
                            self._process_archive(child_id, destination, archive_relative + "::" + member_name, depth + 1)
                        else:
                            self._update(child_id, preparation_status="READY")
                    except PrepError as error:
                        archive_had_review = True
                        self._update(child_id, preparation_status="REVIEW_REQUIRED", reason_code=error.reason_code, detail=error.detail)
                    except (OSError, zipfile.BadZipFile, RuntimeError) as error:
                        archive_had_review = True
                        self._update(child_id, preparation_status="FAILED", reason_code="ARCHIVE_EXTRACTION_FAILED", detail=str(error))
                self._update(parent_id, preparation_status="REVIEW_REQUIRED" if archive_had_review else "ARCHIVE_EXTRACTED",
                             reason_code="ARCHIVE_MEMBER_REVIEW" if archive_had_review else None,
                             detail="one or more archive members require review" if archive_had_review else None)
        except zipfile.BadZipFile as error:
            raise PrepError("ARCHIVE_CORRUPT", str(error)) from error
        except RuntimeError as error:
            raise PrepError("ARCHIVE_EXTRACTION_FAILED", str(error)) from error

    def _discover_paths(self):
        for root, directories, filenames in os.walk(self.source_root, topdown=True, followlinks=False):
            directories.sort()
            filenames.sort()
            root_path = Path(root)
            unsafe_directories = [name for name in directories if (root_path / name).is_symlink()]
            for name in unsafe_directories:
                directories.remove(name)
                yield root_path / name, (root_path / name).relative_to(self.source_root).as_posix(), True
            for name in filenames:
                yield root_path / name, (root_path / name).relative_to(self.source_root).as_posix(), False

    def run(self) -> dict:
        self._touch_job("DISCOVERED")
        for path, relative, is_link in self._discover_paths():
            occurrence_id = self._new_occurrence(
                "source:" + relative, "SOURCE_FILE", path, relative, path.name, path.suffix.lower(),
            )
            if is_link:
                self._update(occurrence_id, preparation_status="REVIEW_REQUIRED",
                             reason_code="UNSAFE_SOURCE_LINK", detail="symbolic-link source is not processed")
            else:
                self._process_source(occurrence_id, path, relative)
        self._touch_job("COMPLETE")
        return self.write_outputs()

    def _occurrences(self) -> list[dict]:
        cursor = self.db.execute("SELECT * FROM occurrence ORDER BY occurrence_key")
        columns = [column[0] for column in cursor.description]
        return [dict(zip(columns, row)) for row in cursor.fetchall()]

    def write_outputs(self) -> dict:
        occurrences = self._occurrences()
        status_counts: dict[str, int] = {}
        for occurrence in occurrences:
            status_counts[occurrence["preparation_status"]] = status_counts.get(occurrence["preparation_status"], 0) + 1
        unaccounted = sum(1 for occurrence in occurrences if occurrence["preparation_status"] not in ACCOUNTED_STATES)
        # An unreconciled job must not expose even previously materialised READY
        # links. For a reconciled job, materialisation can convert a stale READY
        # record into FAILED; reload the ledger below before building counts.
        ready_candidates = [] if unaccounted else [
            item for item in occurrences if item["preparation_status"] == "READY"
        ]
        ready_content = self._materialize_ready(ready_candidates)
        occurrences = self._occurrences()
        status_counts = {}
        for occurrence in occurrences:
            status_counts[occurrence["preparation_status"]] = status_counts.get(occurrence["preparation_status"], 0) + 1
        unaccounted = sum(1 for occurrence in occurrences if occurrence["preparation_status"] not in ACCOUNTED_STATES)
        source_files = [item for item in occurrences if item["kind"] == "SOURCE_FILE"]
        archives = [item for item in source_files if item["extension"] == ".zip"]
        unique_ready = [item for item in occurrences if item["preparation_status"] == "READY"]
        duplicates = [item for item in occurrences if item["preparation_status"].startswith("DUPLICATE_")]
        review_required = status_counts.get("REVIEW_REQUIRED", 0)
        failed = status_counts.get("FAILED", 0)
        reconciliation_status = "COMPLETE" if unaccounted == 0 else "INCOMPLETE"
        handoff_status = aggregate_handoff_status(unaccounted, review_required, failed)
        job_status = self.db.execute("SELECT status FROM job WHERE job_id=?", (self.job_id,)).fetchone()[0]
        manifest = {
            "schema": "parker-ingestion-prep-manifest-v1",
            "jobId": self.job_id,
            "caseId": self.case_id,
            "sourceRoot": str(self.source_root),
            "jobRoot": str(self.job_root),
            "config": self.config.__dict__,
            "occurrences": occurrences,
        }
        reconciliation = {
            "schema": "parker-ingestion-prep-reconciliation-v1",
            "jobId": self.job_id,
            "sourceFilesDiscovered": len(source_files),
            "uniqueSourceContents": len({item["source_sha256"] for item in source_files if item["source_sha256"] and not item["preparation_status"].startswith("DUPLICATE_")}),
            "duplicateOccurrences": len(duplicates),
            "copiesHashVerified": sum(1 for item in occurrences if item["working_copy_sha256"] and item["working_copy_sha256"] == item["source_sha256"]),
            "zipArchivesDiscovered": len(archives),
            "zipArchivesExtracted": sum(1 for item in archives if item["preparation_status"] == "ARCHIVE_EXTRACTED"),
            "archiveMembers": sum(1 for item in occurrences if item["kind"] == "ARCHIVE_MEMBER"),
            "readyUniqueContent": len(ready_content),
            "states": status_counts,
            "reviewRequired": review_required,
            "failed": failed,
            "unaccounted": unaccounted,
            "reconciliationStatus": reconciliation_status,
        }
        handoff = {
            "schema": "parker-ingestion-prep-handoff-v1",
            "jobId": self.job_id,
            "caseId": self.case_id,
            "jobRoot": str(self.job_root),
            "uniqueReadyContentRoot": str(self.ready),
            "sourceCopyRoot": str(self.source_copy),
            "extractedRoot": str(self.extracted),
            "manifestPath": str(self.manifests / "manifest.json"),
            "reconciliationPath": str(self.reports / "reconciliation.json"),
            "duplicateRelationshipData": str(self.manifests / "manifest.json"),
            "jobStatus": job_status,
            "reconciliationStatus": reconciliation_status,
            "handoffStatus": handoff_status,
            # Compatibility alias for older consumers; new consumers should use handoffStatus.
            "preparationStatus": handoff_status,
            "readyItemCount": len(ready_content),
            "duplicateCount": len(duplicates),
            "reviewRequiredCount": review_required,
            "failedCount": failed,
            "unaccountedCount": unaccounted,
            "readyContent": ready_content,
        }
        self._atomic_json(self.manifests / "manifest.json", manifest)
        self._atomic_json(self.reports / "reconciliation.json", reconciliation)
        self._atomic_json(self.reports / "handoff.json", handoff)
        return {"jobId": self.job_id, "jobRoot": str(self.job_root), "manifest": manifest,
                "reconciliation": reconciliation, "handoff": handoff}

    def _materialize_ready(self, items: list[dict]) -> list[dict]:
        """Expose only unique ready files; retained ZIPs never enter this view."""
        def ready_name(item: dict) -> str:
            # The governed Hermes route uses the suffix to select its parser.  Keep
            # the opaque occurrence identity while retaining the source/member
            # suffix; the occurrence record remains the authoritative provenance.
            suffix = Path(item["filename"]).suffix
            return item["occurrence_id"] + suffix

        valid_items = []
        for item in items:
            source = Path(item["working_copy_path"] or item["extracted_path"])
            if not source.is_file():
                self._update(
                    item["occurrence_id"], preparation_status="FAILED",
                    reason_code="COPY_FAILED",
                    detail="verified READY content is missing from the Parker preparation workspace",
                )
                continue
            valid_items.append((item, source))
        expected = {ready_name(item) for item, _ in valid_items}
        for child in self.ready.iterdir():
            if child.is_file() or child.is_symlink():
                if child.name not in expected:
                    child.unlink()
        result = []
        for item, source in valid_items:
            destination = self.ready / ready_name(item)
            if destination.exists() or destination.is_symlink():
                destination.unlink()
            try:
                os.link(source, destination)
            except OSError as error:
                raise PrepError("COPY_FAILED", f"could not create ready handoff link: {error}") from error
            copied_digest, copied_size = sha256_file(destination)
            if copied_digest != item["source_sha256"] or copied_size != item["byte_size"]:
                destination.unlink(missing_ok=True)
                raise PrepError("HASH_MISMATCH", f"ready handoff hash mismatch for {item['occurrence_id']}")
            result.append({"occurrenceId": item["occurrence_id"], "path": str(destination),
                           "sha256": item["source_sha256"], "caseId": item["case_id"],
                           "relativePath": item["relative_path"]})
        return result

    @staticmethod
    def _atomic_json(path: Path, value: dict):
        temporary = path.with_name(path.name + ".part-" + secrets.token_hex(8))
        with temporary.open("w", encoding="utf-8", newline="\n") as output:
            json.dump(value, output, ensure_ascii=False, indent=2, sort_keys=True)
            output.write("\n")
            output.flush()
            os.fsync(output.fileno())
        os.replace(temporary, path)


def new_job_id() -> str:
    return "JOB-" + datetime.now(timezone.utc).strftime("%Y%m%d-%H%M%S") + "-" + secrets.token_hex(4)


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("source", type=Path)
    parser.add_argument("--workspace", type=Path, default=Path("/mnt/parker-data/ingestion-prep"))
    parser.add_argument("--job-id", default=None)
    parser.add_argument("--case-id", default=None)
    parser.add_argument("--archive-max-depth", type=int, default=3)
    parser.add_argument("--archive-max-entries", type=int, default=10_000)
    parser.add_argument("--archive-max-uncompressed-bytes", type=int, default=1_073_741_824)
    args = parser.parse_args(argv)
    job_id = args.job_id or new_job_id()
    job = PrepJob(
        args.source, args.workspace, job_id, args.case_id,
        PrepConfig(args.archive_max_depth, args.archive_max_entries, args.archive_max_uncompressed_bytes),
    )
    try:
        result = job.run()
        print(json.dumps({"jobId": result["jobId"], "jobRoot": result["jobRoot"],
                          "handoff": result["handoff"], "reconciliation": result["reconciliation"]},
                         ensure_ascii=False, indent=2, sort_keys=True))
        return HANDOFF_EXIT_CODES[result["handoff"]["handoffStatus"]]
    finally:
        job.close()


if __name__ == "__main__":
    raise SystemExit(main())
