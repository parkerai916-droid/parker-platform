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

ROOT = Path(__file__).with_name("parker_ingestion_console")
HERMES = os.environ.get("HERMES_CONSOLE_URL", "http://192.168.178.45:8765").rstrip("/")
_hermes_parts = urlsplit(HERMES)
_hermes_host = _hermes_parts.hostname or "127.0.0.1"
if ":" in _hermes_host and not _hermes_host.startswith("["):
    _hermes_host = f"[{_hermes_host}]"
HERMES_ANALYSIS = os.environ.get(
    "HERMES_ANALYSIS_URL",
    f"{_hermes_parts.scheme}://{_hermes_host}:9119",
).rstrip("/")
OWNER = os.environ.get("PARKER_OWNER_URL", "http://127.0.0.1:8080").rstrip("/")
MAX_BODY = 65 * 1024 * 1024
BATCH = re.compile(r"^bulk-[a-f0-9-]+$")
HASH = re.compile(r"^[a-f0-9]{64}$")
HEALTH_TIMEOUT_SECONDS = 5
BATCH_TIMEOUT_SECONDS = 15
INGEST_TIMEOUT_SECONDS = int(os.environ.get("PARKER_CONSOLE_INGEST_TIMEOUT_SECONDS", "180"))


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
        and item.get("status") == "READY"
        and bool(item.get("caseName"))
        for item in batches
    )


def owner(path, method, body, cookie, content_type=None):
    headers = {"Cookie": cookie} if cookie else {}
    if content_type: headers["Content-Type"] = content_type
    return call(OWNER + path, method, body, headers, timeout=BATCH_TIMEOUT_SECONDS)


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
            page = page.replace("__HERMES_CONSOLE_URL__", HERMES).replace("__HERMES_ANALYSIS_URL__", HERMES_ANALYSIS).encode()
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
