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
import math
import os
import re
import subprocess
import sys
import tempfile
import urllib.error
import urllib.request
import zipfile
from dataclasses import dataclass
from email import policy
from email.parser import BytesParser
from pathlib import Path
from xml.etree import ElementTree

try:
    from hermes_format_catalogue import definition_for_extension, media_type_for_extension
except ModuleNotFoundError:  # direct spec loading from the repository tests
    import importlib.util
    _catalogue_spec = importlib.util.spec_from_file_location("hermes_format_catalogue", Path(__file__).with_name("hermes_format_catalogue.py"))
    if _catalogue_spec is None or _catalogue_spec.loader is None:
        raise
    _catalogue_module = importlib.util.module_from_spec(_catalogue_spec)
    sys.modules["hermes_format_catalogue"] = _catalogue_module
    _catalogue_spec.loader.exec_module(_catalogue_module)
    definition_for_extension = _catalogue_module.definition_for_extension
    media_type_for_extension = _catalogue_module.media_type_for_extension


BRIDGE = Path(__file__).with_name("docling-ocr-bridge.py")
NATIVE_PDF_TEXT_PROBE = Path(__file__).with_name("hermes_native_pdf_text.py")
REVIEW_CONFIDENCE_THRESHOLD = 0.80
# Parker's authoritative extractor accepts one non-whitespace character
# (TikaEvidenceExtractor.SEARCHABLE_TEXT_THRESHOLD == 1). These deliberately
# separate constants are only Hermes' preliminary routing heuristic: Hermes
# requires materially usable text before it suppresses its own Docling
# diagnostic run, but it never changes Parker's authoritative decision.
HERMES_PRELIMINARY_NATIVE_TEXT_MIN_CHARS = 32
HERMES_PRELIMINARY_NATIVE_TEXT_MIN_TOKENS = 5


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
    pending_source_retained: bool = False
    submission_error: dict | None = None
    acquisition_attempted: bool = False
    acquisition_status: str | None = None
    acquisition_error: dict | None = None

    def json(self) -> dict:
        return self.__dict__.copy()


def media_type_for(path: Path) -> str | None:
    return media_type_for_extension(path.suffix)


def processing_result_endpoint(batch_id: str) -> str:
    return f"/agent/ingestion-batches/{batch_id}/processing-results"


def processing_result_submission_error(batch_id: str, source_sha256: str, status: int, payload: object) -> dict:
    """Return a browser-safe diagnostic; authentication headers are never included."""
    return {"endpoint": processing_result_endpoint(batch_id), "batchId": batch_id,
            "sourceSha256": source_sha256, "httpStatus": status, "response": payload}


def acquisition_endpoint(evidence_artifact_id: str) -> str:
    return f"/agent/evidence/{evidence_artifact_id}/acquire"


def acquisition_error(evidence_artifact_id: str, status: int, payload: object) -> dict:
    """Return a browser-safe governed-acquisition diagnostic."""
    return {"endpoint": acquisition_endpoint(evidence_artifact_id),
            "evidenceArtifactId": evidence_artifact_id, "httpStatus": status,
            "response": payload}


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


def xlsx_structure(data: bytes) -> tuple[dict | None, str | None]:
    """Read OOXML workbook identity and cell coordinates without flattening it."""
    try:
        with zipfile.ZipFile(__import__("io").BytesIO(data)) as archive:
            workbook = ElementTree.fromstring(archive.read("xl/workbook.xml"))
            rels = ElementTree.fromstring(archive.read("xl/_rels/workbook.xml.rels"))
            shared = []
            if "xl/sharedStrings.xml" in archive.namelist():
                root = ElementTree.fromstring(archive.read("xl/sharedStrings.xml"))
                for item in root:
                    shared.append("".join(node.text or "" for node in item.iter() if node.tag.rsplit("}", 1)[-1] == "t"))
            relationships = {node.attrib.get("Id"): node.attrib.get("Target") for node in rels}
            sheets = []
            ns = {"m": "http://schemas.openxmlformats.org/spreadsheetml/2006/main", "r": "http://schemas.openxmlformats.org/officeDocument/2006/relationships"}
            for sheet in workbook.findall("m:sheets/m:sheet", ns):
                target = relationships.get(sheet.attrib.get("{" + ns["r"] + "}id"))
                if not target:
                    return None, "XLSX sheet relationship is missing"
                target = target.lstrip("/") if target.startswith("/") else "xl/" + target
                root = ElementTree.fromstring(archive.read(target))
                cells = []
                for cell in root.findall(".//m:c", ns):
                    value = cell.find("m:v", ns)
                    formula = cell.find("m:f", ns)
                    raw = value.text if value is not None else None
                    if raw is not None and cell.attrib.get("t") == "s":
                        raw = shared[int(raw)]
                    cells.append({"cell": cell.attrib.get("r"), "value": raw, "formula": formula.text if formula is not None else None, "displayedValue": raw})
                sheets.append({"name": sheet.attrib.get("name"), "cells": cells})
            return {"workbook": "OOXML", "sheets": sheets}, None
    except (KeyError, OSError, ValueError, IndexError, ElementTree.ParseError, zipfile.BadZipFile) as error:
        return None, f"XLSX package is corrupt: {error}"


def email_structure(data: bytes) -> tuple[dict | None, str | None]:
    try:
        message = BytesParser(policy=policy.default).parsebytes(data)
    except Exception as error:
        return None, f"EML message is malformed: {error}"
    if not message.get("From") and not message.get("To") and not message.get("Subject"):
        return None, "EML has no recognisable message headers"
    body = message.get_body(preferencelist=("plain", "html"))
    attachments = [{"filename": part.get_filename(), "contentType": part.get_content_type()} for part in message.iter_attachments()]
    return {"from": message.get("From"), "to": message.get("To"), "cc": message.get("Cc"), "bcc": message.get("Bcc"), "subject": message.get("Subject"), "date": message.get("Date"), "body": body.get_content() if body else "", "messageFormat": body.get_content_type() if body else None, "attachments": attachments}, None


def rtf_text(data: bytes) -> tuple[str, str | None]:
    try:
        source = data.decode("ascii", errors="strict")
    except UnicodeDecodeError as error:
        return "", f"RTF is not valid ASCII control text: {error}"
    if not source.startswith("{\\rtf"):
        return "", "RTF header is missing"
    text = re.sub(r"\\'[0-9a-fA-F]{2}", "", source)
    text = re.sub(r"\\[a-zA-Z]+-?\d*\s?", "", text)
    text = text.replace("{", "").replace("}", "").replace("\\", "").strip()
    return (text, None) if text else ("", "RTF contains no readable text")


def tiff_frame_count(data: bytes) -> tuple[int | None, str | None]:
    if len(data) < 8 or data[:2] not in (b"II", b"MM"):
        return None, "TIFF header is missing"
    endian = "little" if data[:2] == b"II" else "big"
    if int.from_bytes(data[2:4], endian) != 42:
        return None, "TIFF magic is invalid"
    offset = int.from_bytes(data[4:8], endian)
    count = 0
    seen = set()
    try:
        while offset and offset not in seen:
            seen.add(offset)
            if offset + 2 > len(data): return None, "TIFF directory is truncated"
            entries = int.from_bytes(data[offset:offset + 2], endian)
            next_offset = offset + 2 + entries * 12
            if next_offset + 4 > len(data): return None, "TIFF directory is truncated"
            count += 1
            offset = int.from_bytes(data[next_offset:next_offset + 4], endian)
        return (count, None) if count else (None, "TIFF contains no frames")
    except (OverflowError, ValueError):
        return None, "TIFF directory is invalid"


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


def native_pdf_text_available(path: Path, timeout: float) -> bool:
    """Return true only when an embedded/native PDF text layer is readable.

    Probe failure is deliberately treated as unknown, so Hermes falls through
    to the existing Docling path rather than claiming native text it could not
    verify.  The probe is PDFium text extraction only; it does not render or
    OCR pages.
    """
    python = os.environ.get("HERMES_DOCLING_PYTHON", "/home/steve/docling-venv/bin/python")
    try:
        completed = subprocess.run(
            [python, str(NATIVE_PDF_TEXT_PROBE), str(path.absolute())],
            capture_output=True,
            text=True,
            timeout=timeout,
            check=False,
        )
    except (FileNotFoundError, subprocess.TimeoutExpired):
        return False
    if completed.returncode != 0:
        return False
    try:
        value = json.loads(completed.stdout)
    except json.JSONDecodeError:
        return False
    pages = value.get("pages") if isinstance(value, dict) else None
    return native_pdf_text_usable(pages)


def native_pdf_text_usable(pages: object) -> bool:
    """Apply Hermes' conservative preliminary Docling-routing heuristic.

    Parker alone decides authoritative native-PDF usability. The normalized
    character count is comparable to Parker's trim-and-count signal, while the
    higher Hermes-only minimum prevents page numbers, watermarks, or tiny
    hidden text fragments from suppressing Docling. Short legitimate PDFs may
    therefore run through Docling even though Parker will still use their
    native NO_OCR derivative if Parker's own criterion is satisfied.
    """
    if not isinstance(pages, list) or not all(isinstance(page, str) for page in pages):
        return False
    normalized = " ".join(" ".join(page.split()) for page in pages)
    tokens = re.findall(r"[\w]+", normalized, flags=re.UNICODE)
    return len(normalized) >= HERMES_PRELIMINARY_NATIVE_TEXT_MIN_CHARS and len(tokens) >= HERMES_PRELIMINARY_NATIVE_TEXT_MIN_TOKENS


def make_result_and_representation(batch_id: str, source_hash: str, path: Path, data: bytes, timeout: float, original_filename: str | None = None) -> tuple[dict, dict | None]:
    media = media_type_for(path)
    definition = definition_for_extension(path.suffix)
    if media is None:
        return ({
            "sourceSha256": source_hash, "status": "FAILED", "methods": ["DIRECT_TEXT_EXTRACTION"],
            "issues": [], "failure": {"kind": "UNSUPPORTED_FILE_FORMAT", "detail": path.suffix.lower() or "no extension"},
        }, None)
    if media in ("text/plain", "text/csv"):
        text, error = direct_text(data)
        method = "STRUCTURED_DOCUMENT_EXTRACTION" if media == "text/csv" else "DIRECT_TEXT_EXTRACTION"
        if error:
            return ({"sourceSha256": source_hash, "status": "FAILED", "methods": [method], "issues": [], "failure": {"kind": "NO_READABLE_CONTENT", "detail": error}}, None)
        return ({"sourceSha256": source_hash, "status": "PASS", "methods": [method], "issues": []}, None)
    if media == "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet":
        structure, error = xlsx_structure(data)
        if error:
            return ({"sourceSha256": source_hash, "status": "FAILED", "methods": ["STRUCTURED_SPREADSHEET_EXTRACTION"], "issues": [], "failure": {"kind": "CORRUPT_SOURCE", "detail": error}}, None)
        return ({"sourceSha256": source_hash, "status": "PASS", "methods": ["STRUCTURED_SPREADSHEET_EXTRACTION"], "issues": [], "structuredRepresentation": structure}, None)
    if media == "message/rfc822":
        structure, error = email_structure(data)
        if error:
            return ({"sourceSha256": source_hash, "status": "FAILED", "methods": ["STRUCTURED_EMAIL_EXTRACTION"], "issues": [], "failure": {"kind": "CORRUPT_SOURCE", "detail": error}}, None)
        return ({"sourceSha256": source_hash, "status": "PASS", "methods": ["STRUCTURED_EMAIL_EXTRACTION"], "issues": [], "structuredRepresentation": structure}, None)
    if media in ("application/rtf", "text/rtf"):
        text, error = rtf_text(data)
        if error:
            return ({"sourceSha256": source_hash, "status": "FAILED", "methods": ["STRUCTURED_DOCUMENT_EXTRACTION"], "issues": [], "failure": {"kind": "CORRUPT_SOURCE", "detail": error}}, None)
        return ({"sourceSha256": source_hash, "status": "PASS", "methods": ["STRUCTURED_DOCUMENT_EXTRACTION"], "issues": [], "structuredRepresentation": {"text": text}}, None)
    if media == "image/tiff":
        frames, error = tiff_frame_count(data)
        if error:
            return ({"sourceSha256": source_hash, "status": "FAILED", "methods": ["TIFF_FRAME_INSPECTION"], "issues": [], "failure": {"kind": "CORRUPT_SOURCE", "detail": error}}, None)
        # Local OCR is diagnostic only.  Do not submit its text as an authoritative
        # representation; Parker must take the governed external OCR route.
        return ({"sourceSha256": source_hash, "status": "REQUIRES_OCR", "methods": ["TIFF_FRAME_INSPECTION"], "issues": [{"kind": "AUTHORITATIVE_OCR_REQUIRED", "explanation": "TIFF contains %d frame(s); local OCR is preliminary only" % frames}], "frameCount": frames}, None)
    if media in ("application/msword", "application/vnd.ms-excel", "application/vnd.ms-outlook", "application/x-ole-storage"):
        if len(data) < 8 or data[:8] != bytes.fromhex("D0CF11E0A1B11AE1"):
            return ({"sourceSha256": source_hash, "status": "FAILED", "methods": ["STRUCTURED_DOCUMENT_EXTRACTION"], "issues": [], "failure": {"kind": "CORRUPT_SOURCE", "detail": f"{path.suffix.lower()} is not a valid OLE compound document"}}, None)
        # Parker performs the governed Apache POI HWPF/HSSF/HSMF extraction
        # after source admission. Hermes has validated the container signature;
        # it never invents a text derivative or submits one as authoritative.
        return ({"sourceSha256": source_hash, "status": "PASS", "methods": ["STRUCTURED_DOCUMENT_EXTRACTION"], "issues": [], "processingCompleteness": "COMPLETE"}, None)
    if media == "application/vnd.openxmlformats-officedocument.wordprocessingml.document":
        text, error = docx_text(data)
        if error:
            kind = "CORRUPT_SOURCE" if "corrupt" in error else "NO_READABLE_CONTENT"
            return ({"sourceSha256": source_hash, "status": "FAILED", "methods": ["STRUCTURED_DOCUMENT_EXTRACTION"], "issues": [], "failure": {"kind": kind, "detail": error}}, None)
        return ({"sourceSha256": source_hash, "status": "PASS", "methods": ["STRUCTURED_DOCUMENT_EXTRACTION"], "issues": []}, None)
    if media == "application/pdf" and native_pdf_text_available(path, timeout):
        return ({
            "sourceSha256": source_hash,
            "status": "PASS",
            "methods": ["DIRECT_TEXT_EXTRACTION"],
            "issues": [],
            "processingCompleteness": "COMPLETE",
        }, None)
    try:
        outcome = run_docling(path, media, timeout)
    except TimeoutError as error:
        return ({"sourceSha256": source_hash, "status": "FAILED", "methods": ["OCR"], "issues": [], "failure": {"kind": "PROCESSING_TIMEOUT", "detail": str(error)}}, None)
    except RuntimeError as error:
        kind = "REQUIRED_PROCESSOR_UNAVAILABLE" if "unavailable" in str(error) else "PROCESSOR_FAILURE"
        return ({"sourceSha256": source_hash, "status": "FAILED", "methods": ["OCR"], "issues": [], "failure": {"kind": kind, "detail": str(error)}}, None)
    except ValueError as error:
        return ({"sourceSha256": source_hash, "status": "FAILED", "methods": ["OCR"], "issues": [], "failure": {"kind": "CORRUPT_SOURCE", "detail": str(error)}}, None)
    status = outcome.get("status")
    if status == "no_recognisable_content":
        return ({"sourceSha256": source_hash, "status": "FAILED", "methods": ["OCR"], "issues": [], "failure": {"kind": "NO_READABLE_CONTENT", "detail": outcome.get("reason")}}, None)
    if status not in ("recognised", "partial"):
        return ({"sourceSha256": source_hash, "status": "FAILED", "methods": ["OCR"], "issues": [], "failure": {"kind": "PROCESSOR_FAILURE", "detail": "unknown Docling status"}}, None)
    confidence = outcome.get("confidence")
    observed_confidence = confidence if isinstance(confidence, (int, float)) and not isinstance(confidence, bool) and math.isfinite(confidence) and 0.0 <= confidence <= 1.0 else None
    warnings = outcome.get("warnings") if isinstance(outcome.get("warnings"), list) else []
    warnings = [warning for warning in warnings if isinstance(warning, str) and warning.strip()]
    completeness = "PARTIAL" if status == "partial" else "COMPLETE"
    common = {
        "sourceSha256": source_hash,
        "methods": ["OCR"],
        "reviewConfidenceThreshold": REVIEW_CONFIDENCE_THRESHOLD,
        "processingCompleteness": completeness,
        "processingWarnings": warnings + ([
            "Hermes Docling output is preliminary PDF routing diagnostics only; Parker remains authoritative for native PDF usability and derivative selection."
        ] if media == "application/pdf" else []),
    }
    uncertain = status == "partial" or (observed_confidence is not None and observed_confidence < REVIEW_CONFIDENCE_THRESHOLD)
    if uncertain:
        explanation = outcome.get("reason") or "OCR confidence is below the Hermes processing-quality threshold"
        issue = {"kind": "OCR_UNCERTAINTY", "explanation": explanation}
        if observed_confidence is not None:
            issue["observedConfidence"] = observed_confidence
        if media == "application/pdf":
            # PDF Docling output is preliminary routing material.  A structurally
            # valid scanned PDF must reach Parker even when this diagnostic OCR is
            # partial or below Hermes' local confidence threshold; Parker owns the
            # authoritative REQUIRES_OCR/admission decision.
            warning = "Preliminary PDF OCR is uncertain; Parker must make the authoritative REQUIRES_OCR decision"
            result = {**common, "status": "PASS", "issues": [issue], "processingWarnings": common["processingWarnings"] + [warning]}
        else:
            result = {**common, "status": "REVIEW_REQUIRED", "issues": [issue]}
    else:
        result = {**common, "status": "PASS", "issues": []}
    recognised_text = outcome.get("recognisedText")
    representation = {
        "originalFilename": original_filename or path.name,
        "originalMediaType": media,
        "processingMethod": "OCR",
        "recognisedText": recognised_text,
        "confidence": observed_confidence,
        "completeness": completeness,
        "status": result["status"],
        "warnings": result.get("processingWarnings", warnings),
        "issues": result["issues"],
        "mechanismVersion": outcome.get("mechanismVersion"),
        "modelIdentity": outcome.get("modelIdentity"),
        "modelVersion": outcome.get("modelVersion"),
        "derivativeContentSha256": sha256_bytes((recognised_text or "").encode("utf-8")),
    }
    return result, representation


def make_result(batch_id: str, source_hash: str, path: Path, data: bytes, timeout: float) -> dict:
    result, _ = make_result_and_representation(batch_id, source_hash, path, data, timeout)
    return result


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

    def submit_pending_review_source(self, batch_id: str, source_hash: str, data: bytes, filename: str, media: str | None) -> tuple[int, object]:
        headers = {"Content-Type": media or "application/octet-stream", "X-Parker-Original-Filename": filename}
        return self.request(f"/agent/ingestion-batches/{batch_id}/pending-review-sources/{source_hash}", "POST", data, headers)

    def submit_source(self, batch_id: str, source_hash: str, data: bytes, filename: str, media: str | None) -> tuple[int, object]:
        headers = {"Content-Type": media or "application/octet-stream", "X-Parker-Original-Filename": filename}
        return self.request(f"/agent/ingestion-batches/{batch_id}/sources/{source_hash}", "POST", data, headers)

    def submit_ocr_representation(self, batch_id: str, source_hash: str, representation: dict) -> tuple[int, object]:
        body = json.dumps({"sourceSha256": source_hash, **representation}, separators=(",", ":")).encode()
        return self.request(f"/agent/ingestion-batches/{batch_id}/ocr-representations/{source_hash}", "POST", body, {"Content-Type": "application/json"})

    def acquire(self, evidence_artifact_id: str) -> tuple[int, object]:
        """Ask Parker to continue governed acquisition; Parker owns authorization and OCR."""
        return self.request(acquisition_endpoint(evidence_artifact_id), "POST")


def process_one(client: ParkerClient, batch_id: str, path: Path, timeout: float, original_filename: str | None = None) -> ProcessedFile:
    data = path.read_bytes()
    digest = sha256_bytes(data)
    display_name = original_filename or path.name
    result, representation = make_result_and_representation(batch_id, digest, path, data, timeout, display_name)
    code, payload = client.submit_result(batch_id, result)
    if code == 201:
        submission = "RECORDED"
    elif code == 200 and isinstance(payload, dict) and payload.get("status") == "ALREADY_RECORDED":
        submission = "ALREADY_RECORDED"
    elif code == 409:
        return ProcessedFile(display_name, digest, result["status"], result["methods"], "CONFLICT", "NOT_ATTEMPTED", str(payload), False, processing_result_submission_error(batch_id, digest, code, payload))
    else:
        return ProcessedFile(display_name, digest, result["status"], result["methods"], f"HTTP_{code}", "NOT_ATTEMPTED", str(payload), False, processing_result_submission_error(batch_id, digest, code, payload))
    if result["status"] != "PASS":
        if result["status"] == "REVIEW_REQUIRED":
            custody_code, custody_payload = client.submit_pending_review_source(batch_id, digest, data, display_name, media_type_for(path))
            if custody_code not in (200, 201) or not isinstance(custody_payload, dict) or custody_payload.get("status") not in ("STORED", "ALREADY_STORED"):
                return ProcessedFile(display_name, digest, result["status"], result["methods"], submission, "BLOCKED", f"pending review custody failed: HTTP_{custody_code} {custody_payload}", True)
        return ProcessedFile(display_name, digest, result["status"], result["methods"], submission, "BLOCKED", result.get("failure", {}).get("detail") if result.get("failure") else (result.get("issues") or [{}])[0].get("explanation"))
    code, payload = client.submit_source(batch_id, digest, data, display_name, media_type_for(path))
    authoritative_statuses = {"ANALYSIS_READY", "REQUIRES_OCR", "CAPABILITY_UNAVAILABLE", "REVIEW_REQUIRED", "FAILED", "INGESTED", "ALREADY_INGESTED"}
    if code in (201, 202, 200) and isinstance(payload, dict) and payload.get("status") in authoritative_statuses:
        # Parker's post-admission result is authoritative for completion. Hermes' local
        # Docling result may still be retained as explicitly preliminary diagnostic material,
        # but it can never turn a source-only admission into COMPLETE_INGESTION.
        if representation is not None and isinstance(representation.get("recognisedText"), str) and representation["recognisedText"].strip():
            evidence_id = payload.get("evidenceArtifactId")
            if isinstance(evidence_id, str) and evidence_id:
                representation["evidenceArtifactId"] = evidence_id
                client.submit_ocr_representation(batch_id, digest, representation)
        # Parker's persisted evidence processing projection is the shared state source for
        # Dual and Owner presentations. The legacy status remains a transport fallback only for
        # older Parker gateways; current gateways return processingState explicitly.
        status = payload.get("processingState", payload["status"])
        if status in ("INGESTED", "ALREADY_INGESTED"):
            status = "REGISTERED"
        detail = payload.get("detail")
        if status != "REQUIRES_OCR":
            return ProcessedFile(display_name, digest, "PASS", result["methods"], submission, status, str(detail) if detail else None)

        # Source admission has succeeded and supplied the opaque evidence identity.  Continue
        # only through Parker's existing governed acquisition endpoint.  This call does not
        # authorize anything, select a provider, or promote local OCR: Parker decides whether
        # the existing per-evidence authorization and capability permits continuation.
        evidence_id = payload.get("evidenceArtifactId")
        if not isinstance(evidence_id, str) or not evidence_id:
            return ProcessedFile(display_name, digest, "PASS", result["methods"], submission, "REQUIRES_OCR",
                                 "Parker returned REQUIRES_OCR without an evidenceArtifactId")
        acquire_code, acquire_payload = client.acquire(evidence_id)
        acquire_status = acquire_payload.get("status") if isinstance(acquire_payload, dict) else None
        if acquire_code == 200 and acquire_status == "COMPLETED":
            return ProcessedFile(display_name, digest, "PASS", result["methods"], submission, "ANALYSIS_READY",
                                 None, False, None, True, "COMPLETED", None)
        if acquire_status == "AUTHORIZATION_REQUIRED":
            final_status = "REQUIRES_OCR"
        elif acquire_status == "PROVIDER_NOT_READY":
            final_status = "CAPABILITY_UNAVAILABLE"
        elif acquire_status == "FAILED":
            final_status = "FAILED"
        else:
            final_status = f"HTTP_{acquire_code}"
        detail = acquire_payload.get("reason") if isinstance(acquire_payload, dict) else None
        return ProcessedFile(display_name, digest, "PASS", result["methods"], submission, final_status,
                             str(detail) if detail else str(acquire_payload), False, None, True,
                             acquire_status or f"HTTP_{acquire_code}", acquisition_error(evidence_id, acquire_code, acquire_payload))
    return ProcessedFile(display_name, digest, "PASS", result["methods"], submission, f"HTTP_{code}", str(payload))


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
        summary = {"total": len(results), **counts, "ingested": sum(item.governed_ingestion == "ANALYSIS_READY" for item in results), "blocked": sum(item.governed_ingestion in ("BLOCKED", "REQUIRES_OCR", "CAPABILITY_UNAVAILABLE", "REVIEW_REQUIRED") for item in results), "submission_errors": sum(item.result_submission not in ("RECORDED", "ALREADY_RECORDED") for item in results), "files": [item.json() for item in results]}
        print(json.dumps(summary, indent=2, sort_keys=True))
        return 0
    finally:
        # Keep the bearer token out of all output and discard the local reference promptly.
        token = None


if __name__ == "__main__":
    sys.exit(main())
