#!/usr/bin/env python3
"""Small local Hermes browser surface for Parker-authorised bulk ingestion.

The browser talks only to this local Hermes process. The Parker Agent Gateway bearer
token stays in the Hermes process and every Parker call uses the selected opaque batch.
"""
from __future__ import annotations

import argparse
import importlib.util
import json
import os
import re
import secrets
import sys
import tempfile
from pathlib import Path
from email import policy
from email.parser import BytesParser
import urllib.error
import urllib.request
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

try:
    from hermes_processing_ingest import ParkerClient, media_type_for, process_one
except ModuleNotFoundError:  # also supports direct spec-based test loading
    _processor_spec = importlib.util.spec_from_file_location("hermes_processing_ingest", Path(__file__).with_name("hermes_processing_ingest.py"))
    if _processor_spec is None or _processor_spec.loader is None:
        raise
    _processor_module = importlib.util.module_from_spec(_processor_spec)
    sys.modules["hermes_processing_ingest"] = _processor_module
    _processor_spec.loader.exec_module(_processor_module)
    ParkerClient = _processor_module.ParkerClient
    media_type_for = _processor_module.media_type_for
    process_one = _processor_module.process_one

SUPPORTED = {"application/pdf", "image/jpeg", "image/png", "image/webp", "text/plain", "text/csv", "application/pdf", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"}
MAX_FILE = 64 * 1024 * 1024
MULTIPART_ALLOWANCE = 1024 * 1024


class MultipartUploadError(ValueError):
    pass


def parse_multipart_upload(content_type: str, body: bytes):
    """Parse one bounded browser file part without creating a server-side path."""
    if content_type.split(";", 1)[0].strip().lower() != "multipart/form-data":
        raise MultipartUploadError("multipart/form-data is required")
    try:
        envelope = (
            b"Content-Type: " + content_type.encode("ascii") +
            b"\r\nMIME-Version: 1.0\r\n\r\n" + body
        )
        message = BytesParser(policy=policy.default).parsebytes(envelope)
    except (UnicodeError, ValueError) as error:
        raise MultipartUploadError("malformed multipart input") from error
    if not message.is_multipart() or message.defects:
        raise MultipartUploadError("malformed multipart input")
    file_parts = []
    for part in message.iter_parts():
        if part.get_param("name", header="content-disposition") == "file":
            file_parts.append(part)
    if len(file_parts) != 1 or file_parts[0].is_multipart():
        raise MultipartUploadError("file is missing or ambiguous")
    part = file_parts[0]
    if part.defects:
        raise MultipartUploadError("malformed multipart file part")
    raw_filename = part.get_filename()
    if not raw_filename:
        raise MultipartUploadError("file is missing")
    filename = os.path.basename(raw_filename.replace("\\", "/"))
    if not filename or filename in (".", ".."):
        raise MultipartUploadError("file is missing")
    data = part.get_payload(decode=True)
    if data is None:
        raise MultipartUploadError("malformed multipart file part")
    return part.get_content_type().lower(), filename, data


def parker_request(path: str, token: str, method: str = "GET", body: bytes | None = None, headers: dict[str, str] | None = None):
    request = urllib.request.Request(
        os.environ.get("PARKER_GATEWAY_URL", "http://127.0.0.1:8090").rstrip("/") + path,
        data=body, method=method,
        headers={"Authorization": "Bearer " + token, **(headers or {})},
    )
    try:
        with urllib.request.urlopen(request, timeout=120) as response:
            return response.status, response.read()
    except urllib.error.HTTPError as error:
        return error.code, error.read()


PAGE = r'''<!doctype html><meta charset="utf-8"><title>Hermes Bulk Ingestion</title>
<style>body{font:16px system-ui;max-width:850px;margin:2rem auto;padding:0 1rem;background:#111;color:#eee}button,select{padding:.45rem}#drop{border:2px dashed #888;padding:2rem;text-align:center;margin:1rem 0}.drag{border-color:#6f6}pre{white-space:pre-wrap}.note{color:#fd8}</style>
<h1>Hermes · Bulk Ingestion</h1><p class="note">Choose an existing Parker-authorised READY batch. Hermes cannot select its case.</p>
<label>Case/batch <select id="batch"><option>Loading…</option></select></label> <button id="refresh">Refresh READY batches</button>
<p id="selected">No folder selected.</p><input id="folder" type="file" webkitdirectory directory multiple hidden><button id="choose">Choose Folder</button>
<div id="drop">Drag a folder here</div><button id="start" disabled>Start Ingestion</button><pre id="progress"></pre><pre id="summary"></pre>
<script>
const HERMES_UI_NONCE = '__HERMES_UI_NONCE__';
const apiHeaders = {'X-Hermes-Ui-Nonce': HERMES_UI_NONCE};
let batches=[], files=[];
async function ready(){let r=await fetch('/api/ready-batches',{headers:apiHeaders});let d=await r.json();batches=d.batches||[];let s=document.querySelector('#batch');s.innerHTML='';batches.forEach((b,i)=>{let o=document.createElement('option');o.value=i;o.textContent=b.caseName+' · '+b.batchId;s.appendChild(o)}); update()}
function update(){document.querySelector('#start').disabled=!files.length||!batches.length;document.querySelector('#selected').textContent=files.length?'Folder: '+(files[0].webkitRelativePath?.split('/')[0]||'dropped folder')+' · Files: '+files.length:'No folder selected.'}
function setFiles(x){files=Array.from(x);update()}
async function walk(items){let es=Array.from(items).map(i=>i.webkitGetAsEntry&&i.webkitGetAsEntry()).filter(Boolean);if(!es.some(e=>e.isDirectory))throw Error('Dropped-folder recursion is unavailable in this browser. Use Choose Folder.');let out=[];async function w(e,p){if(e.isFile)return new Promise(ok=>e.file(f=>{Object.defineProperty(f,'webkitRelativePath',{value:p+f.name});out.push(f);ok()},ok));let r=e.createReader(),a;do{a=await new Promise(ok=>r.readEntries(ok,()=>ok([])));for(let c of a)await w(c,p+e.name+'/')}while(a.length)}for(let e of es)await w(e,'');if(!out.length)throw Error('No files found. Use Choose Folder.');return out}
document.querySelector('#choose').onclick=()=>document.querySelector('#folder').click();document.querySelector('#folder').onchange=e=>setFiles(e.target.files);let drop=document.querySelector('#drop');drop.ondragover=e=>{e.preventDefault();drop.classList.add('drag')};drop.ondragleave=()=>drop.classList.remove('drag');drop.ondrop=async e=>{e.preventDefault();drop.classList.remove('drag');try{setFiles(await walk(e.dataTransfer.items))}catch(x){document.querySelector('#selected').textContent=x.message}};
document.querySelector('#refresh').onclick=ready;
document.querySelector('#start').onclick=async()=>{let b=batches[document.querySelector('#batch').value],p=document.querySelector('#progress'),counts={PASS:0,REVIEW_REQUIRED:0,FAILED:0,INGESTED:0,ALREADY_INGESTED:0,BLOCKED:0},bad=[];if(!b||!confirm(`Process ${files.length} file(s) for Parker case “${b.caseName}”?`))return;for(let i=0;i<files.length;i++){let f=files[i];p.textContent=`File ${i+1} / ${files.length}: ${f.name}`;let form=new FormData();form.append('file',f,f.name);let r=await fetch('/api/ingest?batchId='+encodeURIComponent(b.batchId),{method:'POST',headers:apiHeaders,body:form});let d=await r.json();counts[d.status]=(counts[d.status]||0)+1;if(d.governed_ingestion)counts[d.governed_ingestion]=(counts[d.governed_ingestion]||0)+1;if(d.reason)bad.push(f.name+': '+d.reason)}document.querySelector('#summary').textContent=`Case name: ${b.caseName}\nBatch ID: ${b.batchId}\nFiles discovered: ${files.length}\nPASS: ${counts.PASS}\nREVIEW_REQUIRED: ${counts.REVIEW_REQUIRED}\nFAILED: ${counts.FAILED}\nIngested: ${counts.INGESTED+counts.ALREADY_INGESTED}\nBlocked: ${counts.BLOCKED}`+(bad.length?'\n\nProblems:\n'+bad.join('\n'):'')};
ready();
</script>'''


class Handler(BaseHTTPRequestHandler):
    server_version = "HermesBulkUI/1"

    @property
    def token(self):
        return self.server.hermes_token  # type: ignore[attr-defined]

    def send_json(self, status, value):
        data = json.dumps(value).encode()
        self.send_response(status); self.send_header("Content-Type", "application/json"); self.send_header("Content-Length", str(len(data))); self.end_headers(); self.wfile.write(data)

    def do_GET(self):
        if self.path == "/":
            data = PAGE.replace("__HERMES_UI_NONCE__", self.server.ui_nonce).encode(); self.send_response(200); self.send_header("Content-Type", "text/html; charset=utf-8"); self.send_header("Content-Length", str(len(data))); self.end_headers(); self.wfile.write(data); return
        if self.path == "/api/ready-batches":
            if self.headers.get("X-Hermes-Ui-Nonce") != self.server.ui_nonce: self.send_json(403, {"error":"invalid local UI session"}); return
            code, payload = parker_request("/agent/ingestion-batches", self.token)
            try: value = json.loads(payload)
            except ValueError: value = {"batches": [], "error": "Parker returned invalid data"}
            self.send_json(code, value); return
        self.send_json(404, {"error": "not found"})

    def do_POST(self):
        if self.headers.get("X-Hermes-Ui-Nonce") != self.server.ui_nonce:
            self.send_json(403, {"error":"invalid local UI session"}); return
        if not self.path.startswith("/api/ingest?"):
            self.send_json(404, {"error": "not found"}); return
        from urllib.parse import parse_qs, urlsplit
        batch = parse_qs(urlsplit(self.path).query).get("batchId", [""])[0]
        if not re.fullmatch(r"bulk-[a-f0-9-]+", batch):
            self.send_json(400, {"status": "FAILED", "reason": "invalid batch"}); return
        ready_code, ready_payload = parker_request("/agent/ingestion-batches", self.token)
        try: ready = json.loads(ready_payload)
        except ValueError: ready = {}
        if ready_code != 200:
            self.send_json(200, {"status": "FAILED", "reason": "Parker READY batch discovery failed"}); return
        if batch not in {item.get("batchId") for item in ready.get("batches", [])}:
            self.send_json(200, {"status": "FAILED", "reason": "batch is not currently Parker-authorised and READY"}); return
        try:
            length = int(self.headers.get("Content-Length", "-1"))
        except (TypeError, ValueError):
            self.send_json(400, {"status": "FAILED", "reason": "invalid Content-Length"}); return
        if length < 0 or length > MAX_FILE + MULTIPART_ALLOWANCE:
            self.send_json(413, {"status": "FAILED", "reason": "file is too large"}); return
        content_type = self.headers.get("Content-Type", "")
        try:
            request_body = self.rfile.read(length)
            media, filename, data = parse_multipart_upload(content_type, request_body)
        except MultipartUploadError as error:
            self.send_json(400, {"status": "FAILED", "reason": str(error)}); return
        if media_type_for(Path(filename)) != media or media not in SUPPORTED: self.send_json(200, {"status":"FAILED", "reason":"unsupported media type"}); return
        if len(data) > MAX_FILE: self.send_json(413, {"status":"FAILED", "reason":"file is too large"}); return
        try:
            with tempfile.NamedTemporaryFile(prefix="hermes-ui-", suffix=Path(filename).suffix, delete=True) as source:
                source.write(data); source.flush()
                item = process_one(ParkerClient(os.environ.get("PARKER_GATEWAY_URL", "http://127.0.0.1:8090"), self.token, 120), batch, Path(source.name), 120)
            payload = item.json(); payload["filename"] = filename
            self.send_json(200, payload)
        except Exception as error:
            self.send_json(200, {"status":"FAILED", "governed_ingestion":"BLOCKED", "reason":str(error)})


def main():
    parser = argparse.ArgumentParser(); parser.add_argument("--bind", default="127.0.0.1", help="default loopback; explicit LAN address permitted"); parser.add_argument("--port", type=int, default=8765); args = parser.parse_args()
    if args.bind in ("0.0.0.0", "::"): parser.error("wildcard binding is not permitted; provide an explicit trusted interface address")
    token = os.environ.get("PARKER_AGENT_GATEWAY_TOKEN")
    if not token: parser.error("PARKER_AGENT_GATEWAY_TOKEN is required")
    server = ThreadingHTTPServer((args.bind, args.port), Handler); server.hermes_token = token; server.ui_nonce = secrets.token_urlsafe(32)  # type: ignore[attr-defined]
    print(f"Hermes Bulk Ingestion UI: http://{args.bind}:{args.port}"); server.serve_forever()


if __name__ == "__main__": main()
