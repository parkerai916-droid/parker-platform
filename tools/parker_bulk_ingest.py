#!/usr/bin/env python3
"""Deterministic Parker bulk-ingestion operator.

This tool is deliberately an untrusted client: it scans bytes, submits them to
the Agent Gateway, and records opaque Parker responses. It never reads or
interprets document contents and never invents an EvidenceArtifactId.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import mimetypes
import os
import sqlite3
import sys
import time
import urllib.error
import urllib.request
import uuid
from concurrent.futures import ThreadPoolExecutor, as_completed
from pathlib import Path

SUPPORTED = {
    "application/pdf", "image/jpeg", "image/png", "image/webp", "text/csv",
    "message/rfc822",
}
RETRYABLE_HTTP = {408, 425, 429, 500, 502, 503, 504}
STATES = ("DISCOVERED", "UNSUPPORTED", "READY_TO_SUBMIT", "SUBMITTING",
          "REGISTERED", "ALREADY_REGISTERED", "ACQUISITION_REQUESTED",
          "ACQUISITION_COMPLETE", "RETRYABLE_FAILURE", "PERMANENT_FAILURE")


def media_type(path: Path) -> str:
    value, _ = mimetypes.guess_type(path.name, strict=False)
    return (value or "application/octet-stream").lower()


class Ledger:
    def __init__(self, path: Path, batch: str, root: Path, case_id: str):
        self.db = sqlite3.connect(path, timeout=30, check_same_thread=False)
        self.db.execute("PRAGMA journal_mode=WAL")
        self.db.execute("CREATE TABLE IF NOT EXISTS job (batch TEXT PRIMARY KEY, root TEXT NOT NULL, case_id TEXT NOT NULL, created REAL NOT NULL, updated REAL NOT NULL)")
        self.db.execute("""CREATE TABLE IF NOT EXISTS file (
          batch TEXT NOT NULL, relpath TEXT NOT NULL, filename TEXT NOT NULL,
          bytes INTEGER NOT NULL, advisory_sha256 TEXT NOT NULL, media_type TEXT NOT NULL,
          state TEXT NOT NULL, artifact_id TEXT, acquisition_status TEXT,
          retries INTEGER NOT NULL DEFAULT 0, reason TEXT, discovered REAL NOT NULL,
          updated REAL NOT NULL, PRIMARY KEY(batch, relpath))""")
        now = time.time()
        self.db.execute("INSERT OR IGNORE INTO job VALUES (?,?,?,?,?)", (batch, str(root), case_id, now, now))
        self.db.commit()
        self.batch, self.root, self.case_id = batch, root, case_id

    def put(self, rel: str, name: str, size: int, digest: str, mt: str, state: str, reason=None):
        now = time.time()
        self.db.execute("""INSERT INTO file(batch,relpath,filename,bytes,advisory_sha256,media_type,state,reason,discovered,updated)
          VALUES(?,?,?,?,?,?,?,?,?,?) ON CONFLICT(batch,relpath) DO UPDATE SET
          filename=excluded.filename, bytes=excluded.bytes, advisory_sha256=excluded.advisory_sha256,
          media_type=excluded.media_type, state=CASE WHEN file.state IN ('REGISTERED','ALREADY_REGISTERED','ACQUISITION_COMPLETE') THEN file.state ELSE excluded.state END,
          reason=excluded.reason, updated=excluded.updated""", (self.batch, rel, name, size, digest, mt, state, reason, now, now))
        self.db.commit()

    def rows(self):
        return self.db.execute("SELECT relpath,filename,bytes,advisory_sha256,media_type,state,artifact_id,acquisition_status,retries,reason FROM file WHERE batch=? ORDER BY relpath", (self.batch,)).fetchall()

    def update(self, rel, **fields):
        fields["updated"] = time.time()
        values = list(fields.values()) + [self.batch, rel]
        self.db.execute("UPDATE file SET " + ",".join(k+"=?" for k in fields) + " WHERE batch=? AND relpath=?", values)
        self.db.commit()


def request(url, token, method, body=None, headers=None):
    req = urllib.request.Request(url, data=body, method=method, headers={"Authorization": "Bearer " + token, **(headers or {})})
    try:
        with urllib.request.urlopen(req, timeout=120) as response:
            return response.status, response.read()
    except urllib.error.HTTPError as e:
        return e.code, e.read()


def submit_one(args, ledger, row):
    rel, name, size, digest, mt, state, artifact, acq, retries, reason = row
    if state in ("UNSUPPORTED", "ACQUISITION_COMPLETE", "PERMANENT_FAILURE"):
        return
    path = ledger.root / Path(rel)
    try:
        if path.is_symlink():
            raise ValueError("symbolic links are not permitted")
        real = path.resolve(strict=True)
        if ledger.root not in real.parents:
            raise ValueError("canonical path escapes source root")
        if not real.is_file() or real.is_symlink():
            raise ValueError("source is not a regular file")
        data = real.read_bytes()
        current = hashlib.sha256(data).hexdigest()
        if current != digest or len(data) != size:
            ledger.update(rel, state="PERMANENT_FAILURE", reason="source changed after manifest")
            return
    except Exception as exc:
        ledger.update(rel, state="PERMANENT_FAILURE", reason=str(exc)[:240])
        return
    ledger.update(rel, state="SUBMITTING")
    headers = {"Content-Type": mt, "X-Parker-Original-Filename": name, "X-Parker-Advisory-Sha256": digest, "X-Parker-Ingestion-Batch-Id": ledger.batch}
    for attempt in range(args.retries + 1):
        code, payload = request(args.gateway.rstrip("/") + "/agent/evidence", args.token, "POST", data, headers)
        try: result = json.loads(payload.decode())
        except Exception: result = {"error": payload.decode(errors="replace")[:240]}
        if code in (200, 201) and result.get("evidenceArtifactId"):
            new_state = "ALREADY_REGISTERED" if result.get("status") == "ALREADY_REGISTERED" else "REGISTERED"
            ledger.update(rel, state=new_state, artifact_id=result["evidenceArtifactId"], retries=attempt, reason=None)
            if not args.assignment_endpoint:
                ledger.update(rel, state="PERMANENT_FAILURE", reason="Parker case-assignment endpoint is not configured")
                return
            assign_url = args.assignment_endpoint.rstrip("/").replace("{evidenceArtifactId}", result["evidenceArtifactId"])
            assign_code, assign_payload = request(assign_url, args.token, "POST", headers={"X-Parker-Ingestion-Batch-Id": ledger.batch})
            if assign_code not in (200, 201, 204):
                ledger.update(rel, state="RETRYABLE_FAILURE" if assign_code in RETRYABLE_HTTP else "PERMANENT_FAILURE", reason="case assignment failed: HTTP " + str(assign_code))
                return
            if args.acquire:
                acq_code, acq_payload = request(args.gateway.rstrip("/") + "/agent/evidence/" + result["evidenceArtifactId"] + "/acquire", args.token, "POST")
                try: acq_result = json.loads(acq_payload.decode())
                except Exception: acq_result = {"status": "FAILED", "reason": acq_payload.decode(errors="replace")[:240]}
                if acq_code == 200 and acq_result.get("status") == "COMPLETED":
                    ledger.update(rel, state="ACQUISITION_COMPLETE", acquisition_status="COMPLETED")
                else:
                    ledger.update(rel, state="RETRYABLE_FAILURE" if acq_code in RETRYABLE_HTTP else "PERMANENT_FAILURE", acquisition_status=acq_result.get("status"), reason=str(acq_result.get("reason", acq_result.get("status", "acquisition failed")))[:240])
            return
        retryable = code in RETRYABLE_HTTP
        if not retryable or attempt >= args.retries:
            ledger.update(rel, state="RETRYABLE_FAILURE" if retryable else "PERMANENT_FAILURE", retries=attempt + 1, reason=str(result.get("error", "submission failed"))[:240])
            return
        time.sleep(min(args.backoff * (2 ** attempt), args.max_backoff))


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("root", type=Path); p.add_argument("--case-id", required=True); p.add_argument("--gateway", required=True)
    p.add_argument("--assignment-endpoint", help="Parker assignment endpoint template, e.g. /agent/evidence/{evidenceArtifactId}/assign")
    p.add_argument("--token", default=os.environ.get("PARKER_AGENT_GATEWAY_TOKEN"), required=False)
    p.add_argument("--ledger", type=Path, default=Path("parker-bulk-ingest.sqlite3")); p.add_argument("--batch", default=None)
    p.add_argument("--workers", type=int, default=4); p.add_argument("--retries", type=int, default=3); p.add_argument("--backoff", type=float, default=1.0); p.add_argument("--max-backoff", type=float, default=30.0); p.add_argument("--acquire", action="store_true")
    args = p.parse_args()
    if not args.token: p.error("--token or PARKER_AGENT_GATEWAY_TOKEN is required")
    if not args.assignment_endpoint: p.error("--assignment-endpoint is required: Hermes must never claim success without Parker case assignment")
    root = args.root.resolve(strict=True)
    if not root.is_dir(): p.error("root must be a directory")
    batch = args.batch or "bulk-" + uuid.uuid4().hex
    ledger = Ledger(args.ledger, batch, root, args.case_id)
    for path in sorted((x for x in root.rglob("*") if x.is_file() and not x.is_symlink()), key=lambda x: x.relative_to(root).as_posix()):
        rel = path.relative_to(root).as_posix(); mt = media_type(path); digest = hashlib.sha256(path.read_bytes()).hexdigest()
        ledger.put(rel, path.name, path.stat().st_size, digest, mt, "READY_TO_SUBMIT" if mt in SUPPORTED else "UNSUPPORTED", None if mt in SUPPORTED else "unsupported media type: " + mt)
    with ThreadPoolExecutor(max_workers=max(1, args.workers)) as pool:
        futures = [pool.submit(submit_one, args, ledger, row) for row in ledger.rows()]
        for future in as_completed(futures): future.result()
    rows = ledger.rows(); counts = {}
    for row in rows: counts[row[5]] = counts.get(row[5], 0) + 1
    report = {"batchId": batch, "batchName": batch, "sourceRoot": str(root), "targetCaseId": args.case_id, "filesDiscovered": len(rows), "states": counts, "problematic": [{"relativePath": r[0], "reason": r[9]} for r in rows if r[9]]}
    print(json.dumps(report, ensure_ascii=False, indent=2))
    return 0 if not any(r[5] in ("RETRYABLE_FAILURE", "PERMANENT_FAILURE") for r in rows) else 2


if __name__ == "__main__": sys.exit(main())
