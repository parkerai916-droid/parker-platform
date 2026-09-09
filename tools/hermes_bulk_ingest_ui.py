#!/usr/bin/env python3
"""Small local Hermes browser surface for Parker-authorised bulk ingestion.

The browser talks only to this local Hermes process. The Parker Agent Gateway bearer
token stays in the Hermes process and every Parker call uses the selected opaque batch.
"""
from __future__ import annotations

import argparse
import cgi
import json
import os
import re
import secrets
import urllib.error
import urllib.request
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

SUPPORTED = {"application/pdf", "image/jpeg", "image/png", "image/webp", "text/csv", "message/rfc822"}
MAX_FILE = 64 * 1024 * 1024


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
document.querySelector('#start').onclick=async()=>{let b=batches[document.querySelector('#batch').value],p=document.querySelector('#progress'),counts={REGISTERED:0,ALREADY_REGISTERED:0,ASSIGNED:0,UNSUPPORTED:0,FAILED:0},bad=[];if(!b)return;for(let i=0;i<files.length;i++){let f=files[i];p.textContent=`File ${i+1} / ${files.length}: ${f.name}`;let form=new FormData();form.append('file',f,f.name);let r=await fetch('/api/ingest?batchId='+encodeURIComponent(b.batchId),{method:'POST',headers:apiHeaders,body:form});let d=await r.json();if(d.status==='ASSIGNED'){counts.ASSIGNED++;counts[d.registration==='ALREADY_REGISTERED'?'ALREADY_REGISTERED':'REGISTERED']++}else counts[d.status]=(counts[d.status]||0)+1;if(d.status==='FAILED'||d.status==='UNSUPPORTED')bad.push(f.name+': '+d.reason)}document.querySelector('#summary').textContent=`Case name: ${b.caseName}\nBatch ID: ${b.batchId}\nFiles discovered: ${files.length}\nRegistered: ${counts.REGISTERED}\nAlready registered: ${counts.ALREADY_REGISTERED}\nAssigned: ${counts.ASSIGNED}\nUnsupported: ${counts.UNSUPPORTED}\nFailed: ${counts.FAILED}`+(bad.length?'\n\nProblems:\n'+bad.join('\n'):'')};
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
        length = int(self.headers.get("Content-Length", "-1"))
        if length < 0 or length > MAX_FILE + 1024 * 1024:
            self.send_json(413, {"status": "FAILED", "reason": "file is too large"}); return
        content_type = self.headers.get("Content-Type", "")
        form = cgi.FieldStorage(fp=self.rfile, headers=self.headers, environ={"REQUEST_METHOD":"POST", "CONTENT_TYPE":content_type, "CONTENT_LENGTH":str(length)})
        field = form["file"] if "file" in form else None
        if field is None or not getattr(field, "filename", None): self.send_json(400, {"status":"FAILED", "reason":"file is missing"}); return
        media = (field.type or "").lower(); filename = os.path.basename(field.filename); data = field.file.read(MAX_FILE + 1)
        if media not in SUPPORTED: self.send_json(200, {"status":"UNSUPPORTED", "reason":"unsupported media type"}); return
        if len(data) > MAX_FILE: self.send_json(413, {"status":"FAILED", "reason":"file is too large"}); return
        headers = {"Content-Type": media, "X-Parker-Original-Filename": filename, "X-Parker-Ingestion-Batch-Id": batch}
        code, payload = parker_request("/agent/evidence", self.token, "POST", data, headers)
        try: result = json.loads(payload)
        except ValueError: result = {}
        if code not in (200, 201) or not result.get("evidenceArtifactId"):
            self.send_json(200, {"status":"FAILED", "reason":result.get("error", "registration failed")}); return
        aid = result["evidenceArtifactId"]
        acode, apayload = parker_request(f"/agent/evidence/{aid}/assign", self.token, "POST", b"", {"X-Parker-Ingestion-Batch-Id": batch})
        if acode not in (200, 201, 204):
            self.send_json(200, {"status":"FAILED", "reason":"assignment failed"}); return
        self.send_json(200, {"status":"ASSIGNED", "registration":result.get("status"), "evidenceArtifactId":aid})


def main():
    parser = argparse.ArgumentParser(); parser.add_argument("--bind", default="127.0.0.1", help="default loopback; explicit LAN address permitted"); parser.add_argument("--port", type=int, default=8765); args = parser.parse_args()
    if args.bind in ("0.0.0.0", "::"): parser.error("wildcard binding is not permitted; provide an explicit trusted interface address")
    token = os.environ.get("PARKER_AGENT_GATEWAY_TOKEN")
    if not token: parser.error("PARKER_AGENT_GATEWAY_TOKEN is required")
    server = ThreadingHTTPServer((args.bind, args.port), Handler); server.hermes_token = token; server.ui_nonce = secrets.token_urlsafe(32)  # type: ignore[attr-defined]
    print(f"Hermes Bulk Ingestion UI: http://{args.bind}:{args.port}"); server.serve_forever()


if __name__ == "__main__": main()
