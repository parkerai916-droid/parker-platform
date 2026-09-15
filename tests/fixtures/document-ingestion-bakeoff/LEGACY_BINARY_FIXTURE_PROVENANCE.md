# Stage 19C legacy binary fixture provenance

These are deterministic acceptance fixtures, not production evidence.

## `08-legacy-parker.doc`

- Base: Apache POI `REL_5_5_1` test-data/document/simple.doc
- Base URL: `https://raw.githubusercontent.com/apache/poi/REL_5_5_1/test-data/document/simple.doc`
- Transformation: Apache POI HWPF `HWPFDocument` opened the base document, replaced its main-range text, and wrote the result with POI 5.5.1.
- Expected paragraphs: `PARKER DOC TEST VALUE 43`; `SECOND DOC PARAGRAPH`
- SHA-256: `8ec610e3c37a8aacc4c54ac24ca30cba98d1a32c3dd62349d8f60e2242e7e839`

## `09-legacy-parker.msg`

- Generator: jotlmsg `2.0.1` (BSD-3-Clause), using the Apache POI 5.5.1 OLE implementation; generator used only to create this fixture, not by Parker at runtime.
- Expected subject: `Parker MSG Test`
- Expected From: `sender@example.test`
- Expected To: `recipient@example.test`
- Expected CC: `cc@example.test`
- Expected body: `PARKER MSG TEST VALUE 45`
- Expected attachment relationship: `attachment.txt`
- SHA-256: `57f8dcfab1b005a81de57826678782c717a685395bd794d4779be0ce029040bb`

Parker parses DOC with Apache POI HWPF and MSG with Apache POI HSMF. No credentials,
production stores, or production evidence are involved.

## `10-stage19d.docx`

- Base: the checked-in Apache POI/XWPF structured acceptance fixture `04-structured.docx`.
- Transformation: deterministic OOXML package rewrite of `word/document.xml`, replacing
  `PARKER-FIXTURE-2026-004` with `PARKER DOCX TEST VALUE 46`; all other package parts remain
  unchanged.
- Expected value: `PARKER DOCX TEST VALUE 46`
- SHA-256: `3e1abfe67d53f80fb7205c68f4e0d46e7eef3b2b8970ec18e4a2798a18a551a2`
