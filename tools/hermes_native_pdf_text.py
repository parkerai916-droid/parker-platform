#!/usr/bin/env python3
"""Native PDF text-layer probe used by Hermes before its OCR fallback.

This probe uses PDFium's embedded-text API only.  It never renders pages and
never invokes OCR.  A non-empty page text result is the same searchable-text
signal Parker's native PDF route is designed to consume.
"""
from __future__ import annotations

import json
import sys


def main() -> int:
    if len(sys.argv) != 2:
        print("usage: hermes_native_pdf_text.py PDF_PATH", file=sys.stderr)
        return 2
    try:
        import pypdfium2  # type: ignore
    except ImportError as error:
        print(f"pypdfium2 unavailable: {error}", file=sys.stderr)
        return 2
    try:
        document = pypdfium2.PdfDocument(sys.argv[1])
        pages = []
        for page_number in range(len(document)):
            text = document[page_number].get_textpage().get_text_range()
            pages.append(text)
        print(json.dumps({"pageCount": len(pages), "pages": pages}, ensure_ascii=False))
        return 0
    except Exception as error:  # malformed/encrypted PDFs fail closed to Docling
        print(f"native PDF text probe failed: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
