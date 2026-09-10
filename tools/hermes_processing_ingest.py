#!/usr/bin/env python3
"""Hermes R0 processing and Parker contract orchestration.

This is deliberately a small command-line boundary around Parker's existing
Agent Gateway contract.  It never creates or transmits a CaseId: the selected
opaque Parker batch is the only Parker identity carried by this process.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import os
import subprocess
import sys
import tempfile
import urllib.error
import urllib.request
import zipfile
from dataclasses import dataclass
from pathlib import Path
from xml.etree import ElementTree


BRIDGE = Path(__file__).with_name("docling-ocr-bridge.py")
SUPPORTED_MEDIA = {
    ".txt": "text/plain",
    ".csv": "text/csv",
    ".pdf": "application/pdf",
    ".docx": "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    ".jpg": "image/jpeg",
    ".jpeg": "image/jpeg",
    ".png": "image/png",
    ".webp": "image/webp",
}
REVIEW_CONFIDENCE_THRESHOLD = 0.80


class ParkerRequestError(RuntimeError):
    def __init__(self, status: int, payload: object):
        super().__init__(f"Parker returned HTTP {status}: {payload}")
        self.status = status
        self.payload = payload


@dataclass
class ProcessedFile:
    filename: str
    source_sha256: str
    status: str
    methods: list[str]
    result_submission: str
    governed_ingestion: str
    reason: str | None = None

    def json(self) -> dict:
        return self.__dict__.copy()


def media_type_for(path: Path) -> str | None:
    return SUPPORTED_MEDIA.get(path.suffix.lower())


def sha256_bytes(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def direct_text(data: bytes) -> tuple[str, str | None]:
    try:
        text = data.decode("utf-8-sig")
    except UnicodeDecodeError as error:
        return "", f"source is not valid UTF-8: {error}"
    if not text.strip():
        return "", "source contains no readable text"
    return text, None


def docx_text(data: bytes) -> tuple[str, str | None]:
    """Extract paragraphs and table cells without adding a DOCX dependency."""
    try:
        with zipfile.ZipFile(__import__("io").BytesIO(data)) as archive:
            xml = archive.read("word/document.xml")
        root = ElementTree.fromstring(xml)
    except (KeyError, OSError, ElementTree.ParseError, zipfile.BadZipFile) as error:
        return "", f"DOCX package is corrupt: {error}"
    texts = []
    for node in root.iter():
        if node.tag.rsplit("}", 1)[-1] == "t" and node.text:
            texts.append(node.text)
    text = " ".join(texts).strip()
    return (text, None) if text else ("", "DOCX contains no readable text")


def run_docling(path: Path, media_type: str, timeout: float) -> dict:
    request = {
        "protocolVersion": "1",
        "sourceFilePath": str(path.absolute()),
        "mediaType": media_type,
    }
    with tempfile.TemporaryDirectory(prefix="hermes-processing-") as temp:
        request_path = Path(temp) / "request.json"
        request_path.write_text(json.dumps(request), encoding="utf-8")
        python = os.environ.get("HERMES_DOCLING_PYTHON", "/home/steve/docling-venv/bin/python")
        try:
            completed = subprocess.run(
                [python, str(BRIDGE), str(request_path)],
                capture_output=True,
                text=True,
                timeout=timeout,
                check=False,
            )
        except FileNotFoundError as error:
            raise RuntimeError(f"required Docling processor unavailable: {error}") from error
        except subprocess.TimeoutExpired as error:
            raise TimeoutError(f"Docling processing exceeded {timeout:g}s") from error
    if completed.returncode != 0:
        detail = completed.stderr.strip() or f"Docling bridge exited {completed.returncode}"
        if completed.returncode == 2:
            raise RuntimeError(f"required Docling processor unavailable: {detail}")
        raise ValueError(detail)
    try:
        value = json.loads(completed.stdout)
    except json.JSONDecodeError as error:
        raise RuntimeError(f"Docling returned malformed response: {error}") from error
    if not isinstance(value, dict):
        raise RuntimeError("Docling returned a non-object response")
    return value


def make_result(batch_id: str, source_hash: str, path: Path, data: bytes, timeout: float) -> dict:
    media = media_type_for(path)
    if media is None:
        return {
            "sourceSha256": source_hash, "status": "FAILED", "methods": ["DIRECT_TEXT_EXTRACTION"],
            "issues": [], "failure": {"kind": "UNSUPPORTED_FILE_FORMAT", "detail": path.suffix.lower() or "no extension"},
        }
    if media in ("text/plain", "text/csv"):
        text, error = direct_text(data)
        method = "STRUCTURED_DOCUMENT_EXTRACTION" if media == "text/csv" else "DIRECT_TEXT_EXTRACTION"
        if error:
            return {"sourceSha256": source_hash, "status": "FAILED", "methods": [method], "issues": [], "failure": {"kind": "NO_READABLE_CONTENT", "detail": error}}
        return {"sourceSha256": source_hash, "status": "PASS", "methods": [method], "issues": [], "failure": None}
    if media == "application/vnd.openxmlformats-officedocument.wordprocessingml.document":
        text, error = docx_text(data)
        if error:
            kind = "CORRUPT_SOURCE" if "corrupt" in error else "NO_READABLE_CONTENT"
            return {"sourceSha256": source_hash, "status": "FAILED", "methods": ["STRUCTURED_DOCUMENT_EXTRACTION"], "issues": [], "failure": {"kind": kind, "detail": error}}
        return {"sourceSha256": source_hash, "status": "PASS", "methods": ["STRUCTURED_DOCUMENT_EXTRACTION"], "issues": [], "failure": None}
    try:
        outcome = run_docling(path, media, timeout)
    except TimeoutError as error:
        return {"sourceSha256": source_hash, "status": "FAILED", "methods": ["OCR"], "issues": [], "failure": {"kind": "PROCESSING_TIMEOUT", "detail": str(error)}}
    except RuntimeError as error:
        kind = "REQUIRED_PROCESSOR_UNAVAILABLE" if "unavailable" in str(error) else "PROCESSOR_FAILURE"
        return {"sourceSha256": source_hash, "status": "FAILED", "methods": ["OCR"], "issues": [], "failure": {"kind": kind, "detail": str(error)}}
    except ValueError as error:
        return {"sourceSha256": source_hash, "status": "FAILED", "methods": ["OCR"], "issues": [], "failure": {"kind": "CORRUPT_SOURCE", "detail": str(error)}}
    status = outcome.get("status")
    if status == "no_recognisable_content":
        return {"sourceSha256": source_hash, "status": "FAILED", "methods": ["OCR"], "issues": [], "failure": {"kind": "NO_READABLE_CONTENT", "detail": outcome.get("reason")}}
    if status not in ("recognised", "partial"):
        return {"sourceSha256": source_hash, "status": "FAILED", "methods": ["OCR"], "issues": [], "failure": {"kind": "PROCESSOR_FAILURE", "detail": "unknown Docling status"}}
    confidence = outcome.get("confidence")
    uncertain = status == "partial" or (isinstance(confidence, (int, float)) and confidence < REVIEW_CONFIDENCE_THRESHOLD)
    if uncertain:
        explanation = outcome.get("reason") or "OCR confidence is below the Hermes processing-quality threshold"
        return {"sourceSha256": source_hash, "status": "REVIEW_REQUIRED", "methods": ["OCR"], "issues": [{"kind": "OCR_UNCERTAINTY", "explanation": explanation}], "failure": None}
    return {"sourceSha256": source_hash, "status": "PASS", "methods": ["OCR"], "issues": [], "failure": None}


class ParkerClient:
    def __init__(self, base_url: str, token: str, timeout: float):
        self.base_url = base_url.rstrip("/")
        self.token = token
        self.timeout = timeout

    def request(self, path: str, method: str = "GET", body: bytes | None = None, headers: dict[str, str] | None = None) -> tuple[int, object]:
        request = urllib.request.Request(self.base_url + path, data=body, method=method, headers={"Authorization": "Bearer " + self.token, **(headers or {})})
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

    def ready_batch(self, batch_id: str) -> dict:
        status, payload = self.request("/agent/ingestion-batches")
        if status != 200 or not isinstance(payload, dict):
            raise ParkerRequestError(status, payload)
        for batch in payload.get("batches", []):
            if batch.get("batchId") == batch_id:
                return batch
        raise ValueError(f"batch is not currently Parker-authorised and READY: {batch_id}")

    def submit_result(self, batch_id: str, result: dict) -> tuple[int, object]:
        body = json.dumps(result, separators=(",", ":")).encode()
        return self.request(f"/agent/ingestion-batches/{batch_id}/processing-results", "POST", body, {"Content-Type": "application/json"})

    def submit_source(self, batch_id: str, source_hash: str, data: bytes, filename: str, media: str | None) -> tuple[int, object]:
        headers = {"Content-Type": media or "application/octet-stream", "X-Parker-Original-Filename": filename}
        return self.request(f"/agent/ingestion-batches/{batch_id}/sources/{source_hash}", "POST", data, headers)


def process_one(client: ParkerClient, batch_id: str, path: Path, timeout: float) -> ProcessedFile:
    data = path.read_bytes()
    digest = sha256_bytes(data)
    result = make_result(batch_id, digest, path, data, timeout)
    code, payload = client.submit_result(batch_id, result)
    if code == 201:
        submission = "RECORDED"
    elif code == 200 and isinstance(payload, dict) and payload.get("status") == "ALREADY_RECORDED":
        submission = "ALREADY_RECORDED"
    elif code == 409:
        return ProcessedFile(path.name, digest, result["status"], result["methods"], "CONFLICT", "NOT_ATTEMPTED", str(payload))
    else:
        return ProcessedFile(path.name, digest, result["status"], result["methods"], f"HTTP_{code}", "NOT_ATTEMPTED", str(payload))
    if result["status"] != "PASS":
        return ProcessedFile(path.name, digest, result["status"], result["methods"], submission, "BLOCKED", result.get("failure", {}).get("detail") if result.get("failure") else (result.get("issues") or [{}])[0].get("explanation"))
    code, payload = client.submit_source(batch_id, digest, data, path.name, media_type_for(path))
    if code in (201, 200) and isinstance(payload, dict) and payload.get("status") in ("INGESTED", "ALREADY_INGESTED"):
        return ProcessedFile(path.name, digest, "PASS", result["methods"], submission, payload["status"])
    return ProcessedFile(path.name, digest, "PASS", result["methods"], submission, f"HTTP_{code}", str(payload))


def iter_input(path: Path):
    if path.is_file():
        yield path
    elif path.is_dir():
        yield from sorted((item for item in path.rglob("*") if item.is_file()), key=lambda item: str(item))
    else:
        raise ValueError(f"input path does not exist: {path}")


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--batch", required=True, help="Parker-minted opaque ingestion batch ID")
    parser.add_argument("--input", required=True, type=Path, help="one source file or a folder")
    parser.add_argument("--gateway-url", default=os.environ.get("PARKER_GATEWAY_URL"), help="Parker Agent Gateway base URL")
    parser.add_argument("--token", default=os.environ.get("PARKER_AGENT_GATEWAY_TOKEN"), help=argparse.SUPPRESS)
    parser.add_argument("--token-file", type=Path, default=None)
    parser.add_argument("--timeout", type=float, default=120.0)
    parser.add_argument("--processor-timeout", type=float, default=120.0)
    parser.add_argument("--yes", action="store_true", help="confirm the displayed Parker case/batch")
    args = parser.parse_args(argv)
    if not args.gateway_url:
        parser.error("--gateway-url or PARKER_GATEWAY_URL is required")
    token = args.token
    if args.token_file:
        token = args.token_file.read_text(encoding="utf-8").strip()
    if not token:
        parser.error("PARKER_AGENT_GATEWAY_TOKEN or --token-file is required")
    client = ParkerClient(args.gateway_url, token, args.timeout)
    try:
        batch = client.ready_batch(args.batch)
        print(f"Parker case: {batch.get('caseName', '(case name unavailable)')}\nParker batch: {args.batch}")
        if not args.yes:
            if input("Confirm this batch for processing [y/N]: ").strip().lower() != "y":
                print("Processing cancelled.")
                return 2
        results = []
        for path in iter_input(args.input):
            try:
                results.append(process_one(client, args.batch, path, args.processor_timeout))
            except Exception as error:  # one source must not terminate the bulk set
                digest = sha256_bytes(path.read_bytes())
                results.append(ProcessedFile(path.name, digest, "FAILED", [], "NOT_SUBMITTED", "BLOCKED", str(error)))
        counts = {status: sum(item.status == status for item in results) for status in ("PASS", "REVIEW_REQUIRED", "FAILED")}
        summary = {"total": len(results), **counts, "ingested": sum(item.governed_ingestion in ("INGESTED", "ALREADY_INGESTED") for item in results), "blocked": sum(item.governed_ingestion == "BLOCKED" for item in results), "submission_errors": sum(item.result_submission not in ("RECORDED", "ALREADY_RECORDED") for item in results), "files": [item.json() for item in results]}
        print(json.dumps(summary, indent=2, sort_keys=True))
        return 0
    finally:
        # Keep the bearer token out of all output and discard the local reference promptly.
        token = None


if __name__ == "__main__":
    sys.exit(main())
