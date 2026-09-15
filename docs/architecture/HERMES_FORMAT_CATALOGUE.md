# Hermes Practical Format Catalogue

This is the operator-facing support matrix for the authoritative
`tools/hermes_processing_ingest.py` path. The executable catalogue is
`tools/hermes_format_catalogue.py`; this document describes its current
verified behaviour and Parker's downstream native route.

| Extension | MIME | Hermes inspect/extract | Parker governed representation | OCR/authority | Status |
|---|---|---|---|---|---|
| `.txt` | `text/plain` | yes / UTF-8 text | governed structured text | none | supported |
| `.csv` | `text/csv` | yes / records | governed CSV representation + external verification policy | none | supported with governed external step |
| `.pdf` | `application/pdf` | yes / native or OCR path | native PDF or governed Tier B | external authoritative when OCR required | supported with limitations |
| `.docx` | OOXML Word | yes / paragraphs and tables | governed native structure | none | supported with limitations |
| `.doc` | `application/msword` | OLE validation; Parker HWPF extraction | governed structured word | none | supported with limitations |
| `.xlsx` | OOXML Workbook | yes / sheet and cell coordinates | governed structured spreadsheet | none | supported |
| `.xls` | `application/vnd.ms-excel` | OLE validation; Parker HSSF extraction | governed structured spreadsheet | none | supported with limitations |
| `.eml` | `message/rfc822` | yes / headers, body, MIME parts | governed EML representation + external verification policy | none | supported with governed external step |
| `.msg` | Outlook/OLE | OLE validation; Parker HSMF extraction | governed structured email | none | supported with limitations |
| `.rtf` | `application/rtf`, `text/rtf` | yes / bounded text extraction | governed structured text | none | supported with limitations |
| `.jpg`, `.jpeg` | `image/jpeg` | yes / OCR processor path | governed Tier B | external authoritative | supported with OCR limitation |
| `.png` | `image/png` | yes / OCR processor path | governed Tier B | external authoritative | supported with OCR limitation |
| `.webp` | `image/webp` | yes / OCR processor path | governed Tier B | external authoritative | supported with OCR limitation |
| `.tif`, `.tiff` | `image/tiff` | yes / frame inspection | governed Tier B after external OCR | external authoritative | supported with OCR limitation |
| `.ppt`, `.pptx` | PowerPoint MIME | recognized only | none | not applicable | planned |
| `.html`, `.htm` | `text/html` | recognized only | none | not applicable | planned |
| `.mht`, `.mhtml` | MIME archive | recognized only | none | not applicable | planned |
| `.heic`, `.heif` | HEIF MIME | recognized only | none | external if later supported | planned |
| `.odt` | OpenDocument text | recognized only | none | not applicable | planned |
| `.ods` | OpenDocument spreadsheet | recognized only | none | not applicable | planned |
| `.zip` | `application/zip` | container classification only | no implicit flattening | not applicable | container-only / planned |

Hermes does not silently treat a local extraction result as an authoritative
Parker derivative. `REQUIRES_OCR`, `CAPABILITY_UNAVAILABLE`, `REVIEW_REQUIRED`,
and `FAILED` remain explicit outcomes. Attachments in EML are candidates for
separate governed source ingestion and are not merged into the parent message.

The superseded `tools/parker_bulk_ingest.py` is not an authoritative processor
and is excluded from this catalogue's operator path.
