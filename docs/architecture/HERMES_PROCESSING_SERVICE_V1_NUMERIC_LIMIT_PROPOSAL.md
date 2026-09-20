# Hermes Processing Service v1 — Numeric Limit Proposal

## Status

The global initial v1 values below are **OWNER-FROZEN** for Unit 1 and the
initial implementation slice. They are not permission to enable excluded
capabilities or production routing.

The effective limit is always the lower of the Parker-supplied request limit
and the Hermes-owned ceiling. A format-specific limit may be lower still.

| Limit | Owner-frozen v1 value | Rationale | Security/resource consequence | Formats affected |
|---|---:|---|---|---|
| Maximum source bytes | 500 MiB (`524,288,000`) | Supports large ordinary evidence while bounding SSH transfer and parser memory | Rejects larger inputs before processing; format-specific expansion limits still apply | All initial formats |
| Metadata envelope bytes | 64 KiB (`65,536`) | More than enough for required identities, method lists, and limits without permitting payload smuggling | Bounds parser allocation before source receipt | All requests |
| Inline response bytes | 8 MiB (`8,388,608`) | Keeps the initial inline-only contract bounded while excluding large artifact transfer | Prevents unbounded response memory and SSH buffering; over-limit output is not truncated | All initial formats |
| Extracted text characters | 4,000,000 characters | Bounds text amplification from compact/binary inputs while supporting substantial documents | Rejects or qualifies excessive text without silent truncation | PDF, DOCX, TXT, RTF, EML/MSG |
| Structured representation bytes | 8 MiB (`8,388,608`) | Matches the inline response ceiling | Prevents cell/part expansion from exhausting memory; rejects or qualifies oversized structures | XLS/XLSX, DOCX, EML/MSG, CSV |
| Processing timeout | 300 seconds wall-clock per request | Allows bounded native parsing of large inputs while retaining a hard request limit | Kills or quarantines hung processors; prevents request-slot exhaustion | All initial formats |
| Request/result retention TTL | 7 days after terminal result | Supports bounded operational retries and Parker reconciliation | Requires bounded durable storage and explicit expiry; does not delete Parker records | Idempotency records and Hermes results |
| Temporary workspace TTL | 24 hours after creation; startup cleanup before service readiness | Allows crash recovery and delayed cleanup while bounding sensitive-byte exposure | Orphan cleanup limits disk disclosure and exhaustion | All formats |
| Maximum original filename length | 255 UTF-8 bytes | Compatible with common filesystem/display limits while filename remains metadata only | Bounds logs, envelopes, and UI exposure; rejects path-like or invalid metadata | All formats |
| Maximum issue count | 100 | Bounds issue amplification and is the v1 response contract | Prevents oversized diagnostic responses | All formats |
| Maximum representation count | 32 | Supports bounded native/structured output without enabling artifact fan-out | Limits response cardinality and provenance graph expansion | All initial formats |

## Proposed format-specific lower ceilings

These are implementation-level lower ceilings within the owner-frozen global
limits. They are not separate Unit 1 blockers; any value used in a production
format route must be recorded in that route's accepted configuration.

| Format/method | Proposed lower ceiling | Reason |
|---|---:|---|
| CSV records | 250,000 records | Bounds row/field expansion while supporting ordinary exports |
| XLS/XLSX cells | 100,000 cells per sheet; 1,000,000 cells per workbook | Prevents spreadsheet amplification and memory exhaustion |
| EML/MSG MIME parts | 10,000 parts | Bounds recursive MIME traversal |
| DOCX package entries | 10,000 entries | Bounds OOXML relationship/package expansion |
| ZIP/package uncompressed expansion | 256 MiB | Prevents decompression bombs while allowing ordinary office packages |

## Freeze effects

These values freeze the enforcement targets for Units 1, 2, 4, 5, 6, 7, 8,
and 9. Changing a value after implementation begins requires a compatibility
and security review. No value in this document authorizes OCR, audio, large
external artifacts, or production routing.
