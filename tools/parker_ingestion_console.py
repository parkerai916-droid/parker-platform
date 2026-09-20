#!/usr/bin/env python3
"""Narrow same-origin Parker + Hermes operator console adapter."""
from __future__ import annotations

import json
import os
import re
import socket
import urllib.error
import urllib.request
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import urlsplit, parse_qs

try:
    from tools import parker_ingestion_handoff
except ModuleNotFoundError:
    import parker_ingestion_handoff

ROOT = Path(__file__).with_name("parker_ingestion_console")
HERMES = os.environ.get("HERMES_CONSOLE_URL", "http://192.168.178.45:8765").rstrip("/")
OWNER = os.environ.get("PARKER_OWNER_URL", "http://127.0.0.1:8080").rstrip("/")
MAX_BODY = 65 * 1024 * 1024
BATCH = re.compile(r"^bulk-[a-f0-9-]+$")
HASH = re.compile(r"^[a-f0-9]{64}$")
HEALTH_TIMEOUT_SECONDS = 5
BATCH_TIMEOUT_SECONDS = 15
INGEST_TIMEOUT_SECONDS = int(os.environ.get("PARKER_CONSOLE_INGEST_TIMEOUT_SECONDS", "180"))
PREP_ROOT = Path(os.environ.get("PARKER_INGESTION_PREP_ROOT", "/mnt/parker-data/ingestion-prep/jobs")).expanduser()
PREP_JOB = re.compile(r"^JOB-[A-Za-z0-9][A-Za-z0-9-]*$")
GATEWAY = os.environ.get("PARKER_GATEWAY_URL", "http://127.0.0.1:8090").rstrip("/")
GATEWAY_TOKEN_FILE = Path(os.environ.get(
    "PARKER_CONSOLE_GATEWAY_TOKEN_FILE",
    "/mnt/parker-secrets/parker/parker-agent-gateway-token",
)).expanduser()


class UpstreamConnectionUnavailable(RuntimeError):
    pass


class UpstreamTimeout(RuntimeError):
    pass


def call(url, method="GET", body=None, headers=None, timeout=HEALTH_TIMEOUT_SECONDS):
    try:
        request = urllib.request.Request(url, data=body, method=method, headers=headers or {})
        with urllib.request.urlopen(request, timeout=timeout) as response:
            return response.status, response.read()
    except urllib.error.HTTPError as error:
        return error.code, error.read()
    except (socket.timeout, TimeoutError):
        raise UpstreamTimeout("upstream request timed out") from None
    except urllib.error.URLError as error:
        if isinstance(error.reason, (socket.timeout, TimeoutError)):
            raise UpstreamTimeout("upstream request timed out") from None
        raise UpstreamConnectionUnavailable("upstream connection unavailable") from None


def value(raw):
    try: return json.loads(raw)
    except (TypeError, ValueError): return {"error": "upstream returned invalid JSON"}


def hermes_nonce():
    code, raw = call(HERMES + "/", timeout=HEALTH_TIMEOUT_SECONDS)
    if code != 200: raise RuntimeError("Hermes UI unavailable")
    match = re.search(rb"HERMES_UI_NONCE = '([^']+)'", raw)
    if not match: raise RuntimeError("Hermes UI session unavailable")
    return match.group(1).decode("ascii")


def ready_batches():
    code, raw = call(HERMES + "/api/ready-batches", headers={"X-Hermes-Ui-Nonce": hermes_nonce()}, timeout=BATCH_TIMEOUT_SECONDS)
    if code != 200: raise RuntimeError("Parker READY batch discovery failed")
    return value(raw)


def authoritative_batches(_cookie):
    """Return Parker's current READY-batch projection via the existing Hermes adapter."""
    ready = ready_batches()
    return 200, json.dumps(ready, separators=(",", ":")).encode()


def validate_authoritative_batch(_cookie, batch_id):
    """Revalidate one exact batch against Parker's current READY projection."""
    try:
        ready = ready_batches()
    except Exception:
        return False
    batches = ready.get("batches", []) if isinstance(ready, dict) else []
    return any(
        item.get("batchId") == batch_id
        and item.get("status") in ("READY", "USED")
        and bool(item.get("caseName"))
        for item in batches
    )


def owner(path, method, body, cookie, content_type=None):
    headers = {"Cookie": cookie} if cookie else {}
    if content_type: headers["Content-Type"] = content_type
    return call(OWNER + path, method, body, headers, timeout=BATCH_TIMEOUT_SECONDS)


def _prep_job_path(job_id: str) -> Path:
    if not PREP_JOB.fullmatch(job_id):
        raise parker_ingestion_handoff.HandoffError("valid JOB identifier is required")
    root = PREP_ROOT.resolve()
    job_root = (root / job_id).resolve()
    try:
        job_root.relative_to(root)
    except ValueError:
        raise parker_ingestion_handoff.HandoffError("prepared job is outside the configured root") from None
    return job_root


def _read_import_report(job_root: Path):
    report_path = job_root / "reports" / "handoff-import.json"
    if not report_path.is_file():
        return None
    try:
        report = json.loads(report_path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError):
        return {"status": "INVALID_REPORT"}
    return report if isinstance(report, dict) else {"status": "INVALID_REPORT"}


def _prepared_job_projection(job_id: str, cookie: str = ""):
    job_root = _prep_job_path(job_id)
    handoff_path = job_root / "reports" / "handoff.json"
    try:
        handoff = json.loads(handoff_path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as error:
        raise parker_ingestion_handoff.HandoffError("prepared handoff metadata is unavailable") from error
    if not isinstance(handoff, dict) or handoff.get("schema") != "parker-ingestion-prep-handoff-v1":
        raise parker_ingestion_handoff.HandoffError("unsupported or missing handoff schema")
    if handoff.get("jobId") != job_id:
        raise parker_ingestion_handoff.HandoffError("handoff job ID does not match its configured path")
    case_id = handoff.get("caseId")
    if not isinstance(case_id, str) or not case_id.strip() or case_id.strip().lower() in {"unassigned", "unknown", "null"}:
        raise parker_ingestion_handoff.HandoffError("production handoff requires a real caseId")
    if handoff.get("jobStatus") != "COMPLETE" or handoff.get("reconciliationStatus") != "COMPLETE":
        raise parker_ingestion_handoff.HandoffError("handoff job is not complete and reconciled")
    if handoff.get("handoffStatus") not in {"READY", "READY_WITH_EXCEPTIONS"}:
        raise parker_ingestion_handoff.HandoffError("handoff is NOT_READY")
    if handoff.get("unaccountedCount") != 0:
        raise parker_ingestion_handoff.HandoffError("handoff has unaccounted occurrences")
    for field in ("readyItemCount", "reviewRequiredCount", "failedCount", "unaccountedCount"):
        if not isinstance(handoff.get(field), int) or handoff[field] < 0:
            raise parker_ingestion_handoff.HandoffError("handoff count metadata is invalid")
    case_name = None
    try:
        code, raw = owner("/owner/cases", "GET", None, cookie)
        payload = value(raw)
        if code == 200 and isinstance(payload, dict):
            case_name = next((c.get("caseName") for c in payload.get("cases", [])
                              if isinstance(c, dict) and parker_ingestion_handoff.canonical_case_id(c.get("caseId")) == case_id), None)
    except Exception:
        pass
    imported = _read_import_report(job_root)
    return {
        "jobId": handoff["jobId"],
        "caseId": case_id,
        "caseName": case_name,
        "handoffStatus": handoff.get("handoffStatus"),
        "jobStatus": handoff.get("jobStatus"),
        "reconciliationStatus": handoff.get("reconciliationStatus"),
        "readyItemCount": handoff.get("readyItemCount"),
        "reviewRequiredCount": handoff.get("reviewRequiredCount"),
        "failedCount": handoff.get("failedCount"),
        "unaccountedCount": handoff.get("unaccountedCount"),
        "handoffImportExists": imported is not None,
        "importStatus": imported.get("status") if imported else "NOT_IMPORTED",
        "importedCount": imported.get("importedCount") if imported else None,
        "importReport": imported,
        "handoffPath": str(handoff_path),
    }


def _prepared_job(job_id: str, cookie: str = ""):
    """Return a metadata-only projection; import performs full validation."""
    return _prepared_job_projection(job_id, cookie)


def prepared_jobs(cookie: str = ""):
    root = PREP_ROOT.resolve()
    if not root.is_dir():
        return []
    jobs = []
    for handoff_path in sorted(root.glob("JOB-*/reports/handoff.json")):
        job_id = handoff_path.parent.parent.name
        if not PREP_JOB.fullmatch(job_id):
            continue
        try:
            jobs.append(_prepared_job(job_id, cookie))
        except (OSError, parker_ingestion_handoff.HandoffError, ValueError):
            continue
    return jobs


def _gateway_token() -> str:
    try:
        mode = GATEWAY_TOKEN_FILE.stat().st_mode & 0o777
        if mode & 0o177:
            raise parker_ingestion_handoff.HandoffError("gateway token file permissions must be 0600")
        token = GATEWAY_TOKEN_FILE.read_text(encoding="utf-8").strip()
    except OSError as error:
        raise parker_ingestion_handoff.HandoffError("gateway token file is unavailable") from error
    if not token:
        raise parker_ingestion_handoff.HandoffError("gateway token file is empty")
    return token


def import_prepared_job(job_id: str, cookie: str, confirmed: bool):
    if not confirmed:
        raise parker_ingestion_handoff.HandoffError("explicit import confirmation is required")
    handoff_path = _prep_job_path(job_id) / "reports" / "handoff.json"
    report = parker_ingestion_handoff.import_handoff(
        handoff_path, OWNER, cookie, GATEWAY, _gateway_token(),
        timeout=BATCH_TIMEOUT_SECONDS, processor_timeout=INGEST_TIMEOUT_SECONDS,
    )
    if report.get("jobId") != job_id:
        raise parker_ingestion_handoff.HandoffError("import result job ID mismatch")
    return report


class Handler(BaseHTTPRequestHandler):
    server_version = "ParkerHermesConsole/1"

    def output(self, code, raw, content_type="application/json"):
        self.send_response(code); self.send_header("Content-Type", content_type); self.send_header("Cache-Control", "no-store")
        self.send_header("Content-Length", str(len(raw))); self.end_headers(); self.wfile.write(raw)

    def json(self, code, data): self.output(code, json.dumps(data, separators=(",", ":")).encode())

    def do_GET(self):
        path = urlsplit(self.path).path
        if path == "/":
            page = (ROOT / "index.html").read_text()
            page = page.encode()
            self.output(200, page, "text/html; charset=utf-8"); return
        if path == "/api/health":
            try:
                parker, _ = call(OWNER + "/", timeout=HEALTH_TIMEOUT_SECONDS)
            except UpstreamTimeout:
                parker = 0
            except UpstreamConnectionUnavailable:
                parker = 0
            try: batches = ready_batches(); hermes = {"state": "HEALTHY", "readyBatches": len(batches.get("batches", []))}
            except UpstreamTimeout: hermes = {"state": "DEGRADED", "detail": "Hermes readiness timed out"}
            except UpstreamConnectionUnavailable: hermes = {"state": "OFFLINE", "detail": "Hermes connection unavailable"}
            except Exception as error: hermes = {"state": "OFFLINE", "detail": str(error)}
            health = {"parker": {"state": "HEALTHY" if parker == 200 else "DEGRADED", "ownerHttp": parker}, "hermes": hermes, "docling": {"state": "DEGRADED"}}
            try:
                code, raw = call(HERMES + "/api/health", headers={"X-Hermes-Ui-Nonce": hermes_nonce()}, timeout=HEALTH_TIMEOUT_SECONDS)
                if code == 200:
                    data = value(raw); health["hermes"].update(data); health["docling"] = data.get("docling", health["docling"])
            except Exception: health["docling"] = {"state": "OFFLINE"}
            self.json(200, health); return
        if path == "/api/batches":
            try:
                code, raw = authoritative_batches(self.headers.get("Cookie"))
                self.output(code, raw)
            except Exception as error: self.json(503, {"error": str(error)})
            return
        if path == "/api/prepared-jobs":
            self.json(200, {"jobs": prepared_jobs(self.headers.get("Cookie", ""))}); return
        match = re.fullmatch(r"/api/prepared-jobs/(JOB-[A-Za-z0-9][A-Za-z0-9-]*)", path)
        if match:
            try:
                self.json(200, _prepared_job(match.group(1), self.headers.get("Cookie", "")))
            except parker_ingestion_handoff.HandoffError:
                self.json(404, {"error": "prepared job is unavailable"})
            return
        if path == "/api/cases":
            code, raw = owner("/owner/cases", "GET", None, self.headers.get("Cookie")); self.output(code, raw); return
        if path == "/api/hermes-review":
            code, raw = owner("/owner/hermes-processing/review", "GET", None, self.headers.get("Cookie")); self.output(code, raw); return
        self.json(404, {"error": "not found"})

    def do_POST(self):
        path = urlsplit(self.path).path
        try: length = int(self.headers.get("Content-Length", "-1"))
        except ValueError: length = -1
        if length < 0 or length > MAX_BODY: self.json(413, {"error": "request too large"}); return
        body = self.rfile.read(length)
        cookie = self.headers.get("Cookie")
        if path == "/api/batches":
            code, raw = owner("/owner/ingestion-batches", "POST", body, cookie, self.headers.get("Content-Type")); self.output(code, raw); return
        match = re.fullmatch(r"/api/prepared-jobs/(JOB-[A-Za-z0-9][A-Za-z0-9-]*)/import", path)
        if match:
            try:
                request = value(body)
                if not isinstance(request, dict):
                    raise parker_ingestion_handoff.HandoffError("JSON object required")
                report = import_prepared_job(match.group(1), cookie or "", request.get("confirm") is True)
                self.json(200, report)
            except parker_ingestion_handoff.HandoffError as error:
                self.json(409, {"jobId": match.group(1), "status": "NOT_IMPORTED", "error": str(error)})
            except Exception:
                self.json(502, {"jobId": match.group(1), "status": "NOT_IMPORTED", "error": "prepared import failed"})
            return
        if path == "/api/ingest":
            batch = parse_qs(urlsplit(self.path).query).get("batchId", [""])[0]
            if not BATCH.fullmatch(batch): self.json(400, {"error": "valid batchId is required"}); return
            if not validate_authoritative_batch(cookie, batch):
                self.json(409, {"error": "CASE_BINDING_MISMATCH", "kind": "CASE_BINDING_MISMATCH", "retry": False}); return
            try:
                nonce = hermes_nonce()
            except UpstreamTimeout:
                self.json(504, {"error": "Hermes connection timed out before ingestion began", "kind": "connection_timeout", "retry": False}); return
            except UpstreamConnectionUnavailable:
                self.json(503, {"error": "Hermes connection unavailable", "kind": "connection_unavailable", "retry": False}); return
            try:
                code, raw = call(
                    HERMES + "/api/ingest?batchId=" + batch,
                    "POST",
                    body,
                    {"Content-Type": self.headers.get("Content-Type", ""), "X-Hermes-Ui-Nonce": nonce},
                    timeout=INGEST_TIMEOUT_SECONDS,
                )
                self.output(code, raw)
            except UpstreamTimeout:
                self.json(504, {"error": "Document processing exceeded the console timeout. Hermes may still be processing this file. Check the batch result before retrying.", "kind": "upstream_timeout", "retry": False})
            except UpstreamConnectionUnavailable:
                self.json(503, {"error": "Hermes connection unavailable", "kind": "connection_unavailable", "retry": False})
            return
        match = re.fullmatch(r"/api/hermes-review/(bulk-[a-f0-9-]+)/([a-f0-9]{64})/decision", path)
        if match:
            target = f"/owner/hermes-processing/review/{match.group(1)}/{match.group(2)}/decision"
            code, raw = owner(target, "POST", body, cookie, self.headers.get("Content-Type")); self.output(code, raw); return
        self.json(404, {"error": "not found"})


def main():
    bind = os.environ.get("PARKER_CONSOLE_BIND", "192.168.178.44")
    if bind in ("0.0.0.0", "::"): raise SystemExit("wildcard binding is not permitted")
    ThreadingHTTPServer((bind, int(os.environ.get("PARKER_CONSOLE_PORT", "8088"))), Handler).serve_forever()


if __name__ == "__main__": main()
