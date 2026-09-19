#!/usr/bin/env python3
"""Fail-closed import of a verified Parker Ingestion Prep handoff.

The handoff, rather than a source directory, is the only import population.
Case validation and batch authorisation are performed through Parker's owner
boundary; content processing is delegated to the existing Hermes/Parker route.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import os
import re
import sqlite3
import sys
import urllib.error
import urllib.request
from datetime import datetime, timezone
from pathlib import Path

try:
    from hermes_processing_ingest import ParkerClient, process_one
except ModuleNotFoundError:  # direct import from repository-root tests
    import importlib.util
    _hermes_spec = importlib.util.spec_from_file_location("hermes_processing_ingest", Path(__file__).with_name("hermes_processing_ingest.py"))
    if _hermes_spec is None or _hermes_spec.loader is None:
        raise
    _hermes_module = importlib.util.module_from_spec(_hermes_spec)
    sys.modules["hermes_processing_ingest"] = _hermes_module
    _hermes_spec.loader.exec_module(_hermes_module)
    ParkerClient = _hermes_module.ParkerClient
    process_one = _hermes_module.process_one


SHA256 = re.compile(r"^[0-9a-f]{64}$")
SUCCESSFUL_GOVERNED_STATES = {"ANALYSIS_READY", "REGISTERED", "REQUIRES_OCR", "CAPABILITY_UNAVAILABLE", "REVIEW_REQUIRED", "FAILED"}


class HandoffError(RuntimeError):
    pass


def now() -> str:
    return datetime.now(timezone.utc).isoformat(timespec="microseconds")


def read_json(path: Path) -> dict:
    try:
        value = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as error:
        raise HandoffError(f"cannot read JSON {path}: {error}") from error
    if not isinstance(value, dict):
        raise HandoffError(f"JSON object required: {path}")
    return value


class OwnerParkerClient:
    """The narrow owner-side case/batch adapter; no agent authority is inferred."""

    def __init__(self, base_url: str, cookie: str, timeout: float):
        if not cookie.strip():
            raise HandoffError("owner authentication cookie is required")
        self.base_url = base_url.rstrip("/")
        self.cookie = cookie
        self.timeout = timeout

    def request(self, path: str, method: str = "GET", body: bytes | None = None) -> tuple[int, object]:
        headers = {"Cookie": self.cookie, "Accept": "application/json"}
        if body is not None:
            headers["Content-Type"] = "application/json"
        request = urllib.request.Request(self.base_url + path, data=body, method=method, headers=headers)
        try:
            with urllib.request.urlopen(request, timeout=self.timeout) as response:
                raw = response.read()
                return response.status, json.loads(raw) if raw else {}
        except urllib.error.HTTPError as error:
            raw = error.read()
            try:
                payload = json.loads(raw) if raw else {}
            except json.JSONDecodeError:
                payload = {"error": raw.decode("utf-8", "replace")}
            return error.code, payload

    def validate_case(self, case_id: str) -> dict:
        code, payload = self.request("/owner/cases")
        if code != 200 or not isinstance(payload, dict):
            raise HandoffError(f"case registry unavailable: HTTP_{code}")
        cases = payload.get("cases")
        if not isinstance(cases, list):
            raise HandoffError("case registry response is malformed")
        for case in cases:
            if isinstance(case, dict) and case.get("caseId") == case_id:
                return case
        raise HandoffError(f"unknown Parker case: {case_id}")

    def authorise_batch(self, case_id: str) -> str:
        code, payload = self.request("/owner/ingestion-batches", "POST", json.dumps({"caseId": case_id}).encode())
        if code != 201 or not isinstance(payload, dict) or not isinstance(payload.get("batchId"), str):
            detail = payload.get("error") if isinstance(payload, dict) else payload
            raise HandoffError(f"Parker batch authorisation failed: HTTP_{code} {detail}")
        return payload["batchId"]

    def register_occurrence(self, batch_id: str, evidence_artifact_id: str, occurrence: dict) -> tuple[int, object]:
        body = json.dumps(occurrence, separators=(",", ":")).encode("utf-8")
        return self.request(
            f"/owner/ingestion-batches/{batch_id}/evidence/{evidence_artifact_id}/occurrences",
            "POST",
            body,
        )


class RecordingParkerClient:
    """Delegates the existing Hermes route while retaining the returned evidence ID."""

    def __init__(self, client: ParkerClient):
        self.client = client
        self.evidence_by_hash: dict[str, str] = {}
        self.source_outcome_by_hash: dict[str, str] = {}

    def __getattr__(self, name):
        return getattr(self.client, name)

    def submit_source(self, batch_id, source_hash, data, filename, media):
        code, payload = self.client.submit_source(batch_id, source_hash, data, filename, media)
        if isinstance(payload, dict) and isinstance(payload.get("status"), str):
            self.source_outcome_by_hash[source_hash] = payload["status"]
        if isinstance(payload, dict) and isinstance(payload.get("evidenceArtifactId"), str):
            self.evidence_by_hash[source_hash] = payload["evidenceArtifactId"]
        return code, payload


def _under(path: Path, root: Path) -> bool:
    try:
        path.relative_to(root)
        return True
    except ValueError:
        return False


def validate_handoff(handoff_path: Path) -> tuple[dict, Path, dict[str, dict]]:
    handoff_path = handoff_path.expanduser().resolve(strict=True)
    if handoff_path.is_dir():
        handoff_path = (handoff_path / "reports" / "handoff.json").resolve(strict=True)
    handoff = read_json(handoff_path)
    if handoff.get("schema") != "parker-ingestion-prep-handoff-v1":
        raise HandoffError("unsupported or missing handoff schema")
    case_id = handoff.get("caseId")
    if not isinstance(case_id, str) or not case_id.strip() or case_id.strip().lower() in {"unassigned", "unknown", "null"}:
        raise HandoffError("production handoff requires a real caseId")
    if handoff.get("jobStatus") != "COMPLETE" or handoff.get("reconciliationStatus") != "COMPLETE":
        raise HandoffError("handoff job is not complete and reconciled")
    if handoff.get("handoffStatus") not in {"READY", "READY_WITH_EXCEPTIONS"}:
        raise HandoffError("handoff is NOT_READY")
    if handoff.get("unaccountedCount") != 0:
        raise HandoffError("handoff has unaccounted occurrences")
    ready = handoff.get("readyContent")
    if not isinstance(ready, list) or handoff.get("readyItemCount") != len(ready):
        raise HandoffError("readyItemCount does not match readyContent")

    job_root = Path(handoff.get("jobRoot", "")).expanduser().resolve()
    ready_root = Path(handoff.get("uniqueReadyContentRoot", "")).expanduser().resolve()
    manifest_path = Path(handoff.get("manifestPath", "")).expanduser().resolve()
    if not _under(manifest_path, job_root) or not _under(ready_root, job_root):
        raise HandoffError("handoff paths must remain inside jobRoot")
    manifest = read_json(manifest_path)
    occurrences = {item.get("occurrence_id"): item for item in manifest.get("occurrences", []) if isinstance(item, dict)}
    validated: dict[str, dict] = {}
    seen_hashes: set[str] = set()
    for item in ready:
        if not isinstance(item, dict):
            raise HandoffError("readyContent contains a non-object")
        occurrence_id = item.get("occurrenceId")
        digest = item.get("sha256")
        path = Path(item.get("path", "")).expanduser().resolve()
        if not isinstance(occurrence_id, str) or occurrence_id in validated:
            raise HandoffError("readyContent contains duplicate/invalid occurrenceId")
        if not isinstance(digest, str) or not SHA256.fullmatch(digest) or digest in seen_hashes:
            raise HandoffError("readyContent contains duplicate/invalid SHA-256")
        if not _under(path, ready_root) or not path.is_file():
            raise HandoffError(f"ready content is missing or outside ready/: {path}")
        actual, _ = _hash_file(path)
        if actual != digest:
            raise HandoffError(f"ready content hash mismatch: {occurrence_id}")
        occurrence = occurrences.get(occurrence_id)
        if not occurrence or occurrence.get("preparation_status") != "READY":
            raise HandoffError(f"readyContent occurrence is not READY: {occurrence_id}")
        if occurrence.get("source_sha256") != digest or occurrence.get("case_id") != case_id:
            raise HandoffError(f"readyContent provenance mismatch: {occurrence_id}")
        if item.get("caseId") != case_id:
            raise HandoffError(f"readyContent case mismatch: {occurrence_id}")
        relative_path = item.get("relativePath")
        if not isinstance(relative_path, str) or not relative_path.strip() or relative_path != occurrence.get("relative_path"):
            raise HandoffError(f"readyContent relative-path provenance mismatch: {occurrence_id}")
        source_suffix = Path(occurrence.get("filename", "")).suffix.lower()
        if not source_suffix or path.suffix.lower() != source_suffix:
            raise HandoffError(f"readyContent media-routing suffix mismatch: {occurrence_id}")
        seen_hashes.add(digest)
        validated[occurrence_id] = {"handoff": item, "occurrence": occurrence, "path": path}
    return handoff, handoff_path, validated


def _hash_file(path: Path) -> tuple[str, int]:
    digest = hashlib.sha256()
    size = 0
    with path.open("rb") as source:
        for chunk in iter(lambda: source.read(1024 * 1024), b""):
            digest.update(chunk)
            size += len(chunk)
    return digest.hexdigest(), size


def _open_ledger(job_root: Path) -> sqlite3.Connection:
    database = sqlite3.connect(job_root / "prep.db", timeout=30)
    database.execute("PRAGMA journal_mode=WAL")
    database.execute("""
        CREATE TABLE IF NOT EXISTS handoff_import (
          occurrence_id TEXT PRIMARY KEY, job_id TEXT NOT NULL, case_id TEXT NOT NULL,
          sha256 TEXT NOT NULL, batch_id TEXT NOT NULL, status TEXT NOT NULL,
          evidence_artifact_id TEXT, governed_ingestion TEXT, detail TEXT,
          association_id TEXT, registered_occurrence_id TEXT,
          updated_at TEXT NOT NULL
        )
    """)
    columns = {row[1] for row in database.execute("PRAGMA table_info(handoff_import)")}
    for name, definition in (("association_id", "TEXT"), ("registered_occurrence_id", "TEXT")):
        if name not in columns:
            database.execute(f"ALTER TABLE handoff_import ADD COLUMN {name} {definition}")
    database.commit()
    return database


def import_handoff(handoff_path: Path, owner_url: str, owner_cookie: str, gateway_url: str,
                   token: str, timeout: float = 120.0, processor_timeout: float = 120.0) -> dict:
    handoff, resolved_handoff, items = validate_handoff(handoff_path)
    case_id = handoff["caseId"]
    owner = OwnerParkerClient(owner_url, owner_cookie, timeout)
    case = owner.validate_case(case_id)
    job_root = Path(handoff["jobRoot"]).expanduser().resolve(strict=True)
    ledger = _open_ledger(job_root)
    report_items = []
    try:
        row = ledger.execute("SELECT batch_id, case_id FROM handoff_import WHERE job_id=? LIMIT 1", (handoff["jobId"],)).fetchone()
        if row and row[1] != case_id:
            raise HandoffError("existing import ledger is bound to a different case")
        batch_id = row[0] if row else owner.authorise_batch(case_id)
        client = RecordingParkerClient(ParkerClient(gateway_url, token, timeout))
        for occurrence_id, item in items.items():
            occurrence = item["occurrence"]
            existing = ledger.execute(
                "SELECT status, sha256, case_id, evidence_artifact_id, association_id, registered_occurrence_id, governed_ingestion FROM handoff_import WHERE occurrence_id=?",
                (occurrence_id,),
            ).fetchone()
            if existing and existing[0] == "IMPORTED" and existing[1] == item["handoff"]["sha256"] and existing[2] == case_id:
                report_items.append({"occurrenceId": occurrence_id, "status": "ALREADY_IMPORTED", "evidenceArtifactId": existing[3],
                                     "associationId": existing[4], "registeredOccurrenceId": existing[5],
                                     "associationStatus": "ALREADY_PRESENT", "occurrenceStatus": "ALREADY_PRESENT",
                                     "contentStatus": "ALREADY_REGISTERED",
                                     "governedIngestion": existing[6],
                                     "prepProvenance": {"jobId": handoff["jobId"], "relativePath": occurrence["relative_path"],
                                                        "sha256": item["handoff"]["sha256"], "caseId": case_id,
                                                        "parentOccurrenceId": occurrence.get("parent_occurrence_id"),
                                                        "archiveMemberPath": occurrence.get("archive_member_path")}})
                continue
            filename = occurrence["filename"]
            try:
                result = process_one(client, batch_id, item["path"], processor_timeout, original_filename=filename)
                evidence_id = client.evidence_by_hash.get(result.source_sha256)
                imported = result.result_submission in {"RECORDED", "ALREADY_RECORDED"} and result.governed_ingestion in SUCCESSFUL_GOVERNED_STATES
                association_id = None
                registered_occurrence_id = None
                association_status = None
                occurrence_status = None
                content_status = client.source_outcome_by_hash.get(result.source_sha256)
                detail = result.reason
                if imported and evidence_id:
                    provenance = {
                        "sourceSha256": result.source_sha256,
                        "prepJobId": handoff["jobId"],
                        "prepOccurrenceId": occurrence_id,
                        "relativePath": occurrence["relative_path"],
                    }
                    parent = occurrence.get("parent_occurrence_id")
                    member = occurrence.get("archive_member_path")
                    if parent is not None:
                        provenance["archiveParentOccurrenceId"] = parent
                    if member is not None:
                        provenance["archiveMemberPath"] = member
                    occurrence_code, occurrence_payload = owner.register_occurrence(batch_id, evidence_id, provenance)
                    if occurrence_code in (201, 200) and isinstance(occurrence_payload, dict) and occurrence_payload.get("status") in ("CREATED", "ALREADY_PRESENT"):
                        association_id = occurrence_payload.get("associationId")
                        registered_occurrence_id = occurrence_payload.get("occurrenceId")
                        occurrence_status = occurrence_payload["status"]
                        association_status = "CREATED" if occurrence_code == 201 else "ALREADY_PRESENT"
                    else:
                        imported = False
                        detail = f"occurrence registration failed: HTTP_{occurrence_code} {occurrence_payload}"
                if not imported:
                    status = "FAILED"
                else:
                    status = "IMPORTED"
                ledger.execute("""INSERT OR REPLACE INTO handoff_import
                    (occurrence_id, job_id, case_id, sha256, batch_id, status,
                     evidence_artifact_id, governed_ingestion, detail, association_id,
                     registered_occurrence_id, updated_at)
                    VALUES (?,?,?,?,?,?,?,?,?,?,?,?)""",
                               (occurrence_id, handoff["jobId"], case_id, result.source_sha256, batch_id, status,
                                evidence_id, result.governed_ingestion, detail, association_id,
                                registered_occurrence_id, now()))
                ledger.commit()
                report_items.append({"occurrenceId": occurrence_id, "status": status, "evidenceArtifactId": evidence_id,
                                     "associationId": association_id, "registeredOccurrenceId": registered_occurrence_id,
                                     "associationStatus": association_status, "occurrenceStatus": occurrence_status,
                                     "contentStatus": content_status,
                                     "governedIngestion": result.governed_ingestion, "result": result.json(),
                                     "prepProvenance": {"jobId": handoff["jobId"], "relativePath": occurrence["relative_path"],
                                                        "sha256": result.source_sha256, "caseId": case_id,
                                                        "parentOccurrenceId": occurrence.get("parent_occurrence_id"),
                                                        "archiveMemberPath": occurrence.get("archive_member_path")},
                                     "detail": detail})
            except Exception as error:
                ledger.execute("""INSERT OR REPLACE INTO handoff_import
                    (occurrence_id, job_id, case_id, sha256, batch_id, status,
                     evidence_artifact_id, governed_ingestion, detail, association_id,
                     registered_occurrence_id, updated_at)
                    VALUES (?,?,?,?,?,?,?,?,?,?,?,?)""",
                               (occurrence_id, handoff["jobId"], case_id, item["handoff"]["sha256"], batch_id, "FAILED",
                                None, None, str(error), None, None, now()))
                ledger.commit()
                report_items.append({"occurrenceId": occurrence_id, "status": "FAILED", "detail": str(error)})
        imported = sum(item["status"] in {"IMPORTED", "ALREADY_IMPORTED"} for item in report_items)
        failed = len(report_items) - imported
        newly_imported = sum(item.get("contentStatus") == "REGISTERED" for item in report_items)
        reused_existing = sum(item.get("contentStatus") == "ALREADY_REGISTERED" for item in report_items)
        associations_created = sum(item.get("associationStatus") == "CREATED" for item in report_items)
        associations_existing = sum(item.get("associationStatus") == "ALREADY_PRESENT" for item in report_items)
        occurrences_created = sum(item.get("occurrenceStatus") == "CREATED" for item in report_items)
        occurrences_existing = sum(item.get("occurrenceStatus") == "ALREADY_PRESENT" for item in report_items)
        report = {"schema": "parker-ingestion-handoff-import-v1", "jobId": handoff["jobId"], "caseId": case_id,
                  "caseName": case.get("caseName"), "batchId": batch_id, "handoffPath": str(resolved_handoff),
                  "handoffStatus": handoff["handoffStatus"],
                  "status": "COMPLETE" if failed == 0 else "COMPLETE_WITH_EXCEPTIONS", "readyCount": len(items),
                  "importedCount": imported, "importedNewCount": newly_imported,
                  "reusedExistingContentCount": reused_existing, "associationsCreatedCount": associations_created,
                  "associationsAlreadyPresentCount": associations_existing, "occurrencesCreatedCount": occurrences_created,
                  "occurrencesAlreadyPresentCount": occurrences_existing,
                  "withheldReviewCount": handoff.get("reviewRequiredCount", 0), "withheldFailedCount": handoff.get("failedCount", 0),
                  "failedCount": failed, "items": report_items, "completedAt": now()}
        output = job_root / "reports" / "handoff-import.json"
        temporary = output.with_suffix(".json.part")
        temporary.write_text(json.dumps(report, ensure_ascii=False, indent=2, sort_keys=True) + "\n", encoding="utf-8")
        os.replace(temporary, output)
        return report
    finally:
        ledger.close()


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("handoff", type=Path)
    parser.add_argument("--owner-url", default=os.environ.get("PARKER_OWNER_URL", "http://127.0.0.1:8080"))
    parser.add_argument("--owner-cookie", default=os.environ.get("PARKER_OWNER_COOKIE"))
    parser.add_argument("--gateway-url", default=os.environ.get("PARKER_GATEWAY_URL"))
    parser.add_argument("--token", default=os.environ.get("PARKER_AGENT_GATEWAY_TOKEN"), help=argparse.SUPPRESS)
    parser.add_argument("--token-file", type=Path)
    parser.add_argument("--timeout", type=float, default=120.0)
    parser.add_argument("--processor-timeout", type=float, default=120.0)
    args = parser.parse_args(argv)
    if not args.owner_cookie or not args.gateway_url:
        parser.error("PARKER_OWNER_COOKIE and PARKER_GATEWAY_URL (or their options) are required")
    token = args.token
    if args.token_file:
        token = args.token_file.read_text(encoding="utf-8").strip()
    if not token:
        parser.error("PARKER_AGENT_GATEWAY_TOKEN or --token-file is required")
    try:
        report = import_handoff(args.handoff, args.owner_url, args.owner_cookie, args.gateway_url, token, args.timeout, args.processor_timeout)
        print(json.dumps({key: report[key] for key in (
            "jobId", "caseName", "caseId", "batchId", "status", "handoffStatus", "readyCount",
            "importedCount", "importedNewCount", "reusedExistingContentCount", "associationsCreatedCount",
            "associationsAlreadyPresentCount", "occurrencesCreatedCount", "occurrencesAlreadyPresentCount",
            "withheldReviewCount", "withheldFailedCount", "failedCount",
        )}, indent=2, sort_keys=True))
        return 0 if report["failedCount"] == 0 else 2
    except HandoffError as error:
        print(json.dumps({"status": "NOT_IMPORTED", "error": str(error)}, indent=2), flush=True)
        return 3


if __name__ == "__main__":
    raise SystemExit(main())
