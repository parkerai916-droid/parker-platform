"""The single Hermes/Parker practical source-format catalogue.

This module is intentionally dependency-free.  It is imported by the
authoritative Hermes processor, its local ingestion UI, and tooling tests.
The ``parker_native_route`` field records the current Parker downstream
capability; it is not inferred from Hermes' ability to inspect a file.
"""
from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True)
class FormatDefinition:
    extensions: tuple[str, ...]
    mime_types: tuple[str, ...]
    family: str
    parser: str
    text_extraction: str
    structure_provenance: str
    ocr_may_be_required: bool
    authoritative_ocr: str
    multiparts: str
    hermes_inspect: bool
    parker_native_route: str


CATALOGUE: tuple[FormatDefinition, ...] = (
    FormatDefinition((".txt",), ("text/plain",), "text", "UTF-8 decoder", "yes", "document offsets", False, "not applicable", "single", True, "native"),
    FormatDefinition((".csv",), ("text/csv",), "tabular", "CSV parser", "yes", "records/fields", False, "not applicable", "single", True, "governed external"),
    FormatDefinition((".pdf",), ("application/pdf",), "document", "PDF/Tika/PDFBox", "yes when text layer exists", "page mapping when established", True, "EXTERNAL_AUTHORITATIVE", "multi-page", True, "native"),
    FormatDefinition((".docx",), ("application/vnd.openxmlformats-officedocument.wordprocessingml.document",), "document", "OOXML reader", "yes", "paragraphs/tables/order; no invented pages", False, "not applicable", "multi-part package", True, "native"),
    FormatDefinition((".doc",), ("application/msword",), "document", "Apache POI HWPF", "yes", "paragraphs/order; no invented pages", False, "not applicable", "single document", True, "native"),
    FormatDefinition((".xlsx",), ("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",), "spreadsheet", "Apache POI Workbook", "yes", "workbook/sheet/cell/formula/displayed value", False, "not applicable", "multi-sheet package", True, "native"),
    FormatDefinition((".xls",), ("application/vnd.ms-excel",), "spreadsheet", "Apache POI HSSF", "yes", "workbook/sheet/cell/formula/displayed value", False, "not applicable", "multi-sheet workbook", True, "native"),
    FormatDefinition((".eml",), ("message/rfc822",), "email", "email/MIME parser", "yes", "headers/MIME parts/attachments", False, "not applicable", "multi-part MIME", True, "governed external"),
    FormatDefinition((".msg",), ("application/vnd.ms-outlook", "application/x-ole-storage"), "email", "Apache POI HSMF", "yes", "headers/body/attachment relationships", False, "not applicable", "attachments", True, "native"),
    FormatDefinition((".rtf",), ("application/rtf", "text/rtf", "application/x-rtf"), "document", "bounded RTF reader", "yes", "text and reliable control structure", False, "not applicable", "single document", True, "native"),
    FormatDefinition((".jpg", ".jpeg", ".png", ".webp"), ("image/jpeg", "image/png", "image/webp"), "image", "Docling/RapidOCR preliminary", "OCR-dependent", "page/frame only when established", True, "EXTERNAL_AUTHORITATIVE", "single frame", True, "Tier B/external OCR"),
    FormatDefinition((".tif", ".tiff"), ("image/tiff",), "image", "TIFF frame reader + OCR", "OCR-dependent", "frame identity; no fabricated coordinates", True, "EXTERNAL_AUTHORITATIVE", "single/multi-frame", True, "Tier B/external OCR"),
    FormatDefinition((".ppt", ".pptx"), ("application/vnd.ms-powerpoint", "application/vnd.openxmlformats-officedocument.presentationml.presentation"), "presentation", "not implemented", "no", "unavailable", False, "not applicable", "multi-slide", True, "planned"),
    FormatDefinition((".html", ".htm"), ("text/html",), "web", "not implemented", "no", "unavailable", False, "not applicable", "single document", True, "planned"),
    FormatDefinition((".mht", ".mhtml"), ("message/rfc822", "application/x-mimearchive"), "web archive", "not implemented", "no", "unavailable", False, "not applicable", "multi-part MIME", True, "planned"),
    FormatDefinition((".heic", ".heif"), ("image/heic", "image/heif"), "image", "not installed", "no", "unavailable", True, "EXTERNAL_AUTHORITATIVE", "single/multi-frame", True, "planned"),
    FormatDefinition((".odt",), ("application/vnd.oasis.opendocument.text",), "document", "not implemented", "no", "unavailable", False, "not applicable", "multi-part package", True, "planned"),
    FormatDefinition((".ods",), ("application/vnd.oasis.opendocument.spreadsheet",), "spreadsheet", "not implemented", "no", "unavailable", False, "not applicable", "multi-sheet package", True, "planned"),
    FormatDefinition((".zip",), ("application/zip",), "container", "safe container inspector", "container only", "member identity/path/limits", False, "not applicable", "multi-member", True, "container-only"),
)

BY_EXTENSION = {extension: definition for definition in CATALOGUE for extension in definition.extensions}
BY_MIME = {mime: definition for definition in CATALOGUE for mime in definition.mime_types}


def definition_for_extension(extension: str) -> FormatDefinition | None:
    return BY_EXTENSION.get(extension.lower() if extension.startswith(".") else "." + extension.lower())


def media_type_for_extension(extension: str) -> str | None:
    normalized = extension.lower() if extension.startswith(".") else "." + extension.lower()
    exact = {
        ".jpg": "image/jpeg", ".jpeg": "image/jpeg", ".png": "image/png", ".webp": "image/webp",
        ".tif": "image/tiff", ".tiff": "image/tiff", ".rtf": "application/rtf",
    }.get(normalized)
    if exact:
        return exact
    definition = definition_for_extension(normalized)
    return definition.mime_types[0] if definition else None


def supported_extensions() -> tuple[str, ...]:
    return tuple(
        extension
        for definition in CATALOGUE
        for extension in definition.extensions
        if definition.hermes_inspect
        and definition.text_extraction not in ("no", "container only")
        and definition.parker_native_route in ("native", "governed external", "Tier B/external OCR")
    )


def supported_mime_types() -> frozenset[str]:
    """Return MIME types belonging to at least one end-to-end supported format.

    A MIME type may legitimately serve more than one extension (for example,
    message/rfc822 is used by EML and MIME archives), so this must aggregate
    the catalogue rather than use the lossy one-to-one BY_MIME index.
    """
    return frozenset(
        mime
        for definition in CATALOGUE
        if definition.hermes_inspect
        and definition.text_extraction not in ("no", "container only")
        and definition.parker_native_route in ("native", "governed external", "Tier B/external OCR")
        for mime in definition.mime_types
    )
