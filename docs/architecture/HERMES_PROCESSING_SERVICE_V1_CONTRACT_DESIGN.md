# Hermes Processing Service v1 — Contract Design Revision

## Status and scope

This is a design proposal only. It does not implement or authorize a Hermes
service, change Parker governance, change an existing ingestion route, or
authorize production deployment. It defines the boundary and wire contract
that a future implementation must satisfy.

The contract is a technical-processing contract. Hermes may inspect bytes and
produce candidate technical representations. It does not decide whether a
source is trusted, admissible, analysis-ready, associated with a case, or
accepted as evidence.

## 1. Trust boundary

Parker is authoritative for:

- cases and case assignment;
- ingestion batches and their authorization;
- evidence identity and governed source custody;
- admission, associations, and occurrences;
- human-review decisions;
- final processing state and downstream eligibility.

Hermes is authoritative only for facts about the processing attempt it
performed:

- source inspection and detected technical characteristics;
- extraction, OCR, transcription, and structured parsing;
- processor diagnostics and bounded resource observations;
- preliminary uncertainty or confidence when the underlying processor
  genuinely supplies a documented measure.

Hermes receives no `caseId`. `batchId` is an opaque Parker-issued correlation
value, not a permission grant that Hermes may interpret or resolve. Hermes
must not call Parker case, admission, association, or review operations as a
side effect of processing.

`PASS` means only that the requested technical processing completed according
to the method's technical conditions. It is not Parker admission, evidence
trust, legal/evidential truth, completeness, or analysis readiness.

## 2. Request schema

The logical request is versioned and has the following shape:

```json
{
  "protocolVersion": "1",
  "requestId": "req_01J...",
  "jobId": "job_01J...",
  "occurrenceId": "occ_01J...",
  "batchId": "batch_01J...",
  "source": {
    "reference": "parker-source:v1:src_01J...",
    "sha256": "64 lowercase hexadecimal characters",
    "sizeBytes": 12345,
    "originalFilename": "record.docx",
    "mediaType": "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
  },
  "requestedMethods": ["NATIVE_TEXT_EXTRACTION"],
  "limits": {
    "maxOutputBytes": 16777216,
    "deadlineSeconds": 120
  }
}
```

Required fields:

- `protocolVersion`: exactly `"1"` for this contract.
- `requestId`: Parker-generated idempotency key for one logical processing
  request.
- `jobId`: Parker job identity for operational correlation.
- `occurrenceId`: Parker occurrence identity. Hermes treats it as opaque.
- `batchId`: Parker-issued opaque batch identity. It is not a case identifier
  and must not be dereferenced by Hermes.
- `source.reference`: an opaque Parker source identity; its definition is in
  Section 8.
- `source.sha256`: the expected SHA-256 of the exact source byte stream.
- `source.sizeBytes`: expected exact byte length.
- `source.originalFilename`: metadata only; never a path or command fragment.
- `source.mediaType`: Parker's declared media type. Hermes may report a
  detected type separately and must disclose contradictions.

`requestedMethods` is optional only when the format router has one
unambiguous technical default. If supplied, Hermes runs only methods it is
configured and authorized to expose; it does not select a Parker admission
route. `limits` are upper bounds supplied by Parker, subject to Hermes's
stricter local limits.

### Source transport model

v1 chooses **authenticated byte upload**. Parker sends the exact source bytes
over the authenticated SSH/forced-command channel already used for the
Parker-to-Hermes boundary. The fixed remote forced command accepts one bounded
request envelope and one byte stream; Parker never supplies a shell command,
filesystem path, executable, or provider profile.

`source.reference` is an opaque, immutable Parker identifier such as
`parker-source:v1:src_01J...`. It is not a POSIX path, URI that Hermes can
dereference, mount location, or authorization token. It identifies the Parker
source for correlation and returned provenance only. The byte stream is the
source of processing truth for this invocation.

The concrete framing must be canonical and bounded before implementation is
approved. It must carry the JSON metadata and exact byte length, followed by
exactly `sizeBytes` bytes. Extra bytes, truncated input, malformed framing,
or a mismatched end marker are failures. JSON metadata must be parsed as data,
never interpolated into a command.

Hermes independently computes SHA-256 over the received bytes and compares it
with `source.sha256` before format processing. A mismatch is a non-retryable
source-integrity failure unless Parker deliberately creates a new request with
the corrected source identity.

No arbitrary shared path is part of v1. A future shared-storage design would
require a separate contract defining a confined immutable root, path grammar,
ownership, race prevention, and independent hashing; it is not implied by
this document.

## 3. Response schema

```json
{
  "protocolVersion": "1",
  "requestId": "req_01J...",
  "jobId": "job_01J...",
  "occurrenceId": "occ_01J...",
  "sourceSha256": "64 lowercase hexadecimal characters",
  "status": "PASS",
  "methods": [
    "DOCX_PACKAGE_INSPECTION",
    "EMBEDDED_IMAGE_EXTRACTION",
    "OCR"
  ],
  "representations": [
    {
      "representationId": "rep_01J...",
      "type": "OCR_TEXT",
      "method": "OCR",
      "content": "candidate recognized text",
      "provenanceRef": "prov_01J..."
    }
  ],
  "issues": [],
  "failure": null,
  "provenance": {
    "sourceSha256": "64 lowercase hexadecimal characters",
    "operations": []
  },
  "processor": {
    "name": "hermes-processing",
    "version": "1.0.0",
    "completedAt": "2026-01-01T00:00:00Z"
  }
}
```

`sourceSha256` must be the digest Hermes computed from received bytes, not
merely an echo of the request. `methods` is the ordered set of methods actually
run. A method requested but not run must appear as an issue or failure detail;
it must not silently disappear.

For large output, a representation may use an immutable candidate artifact
instead of inline `content`:

```json
{
  "representationId": "rep_01J...",
  "type": "TEXT",
  "method": "NATIVE_TEXT_EXTRACTION",
  "artifact": {
    "reference": "hermes-artifact:v1:art_01J...",
    "sha256": "64 lowercase hexadecimal characters",
    "sizeBytes": 5242880,
    "mediaType": "text/plain"
  },
  "provenanceRef": "prov_01J..."
}
```

The v1 response may inline representations up to a negotiated bounded limit.
Larger representations use an immutable artifact reference, digest, length,
and media type. Parker retrieves the bytes through the authenticated channel,
verifies the digest and length, and decides whether to admit them. A Hermes
artifact reference never by itself creates Parker custody or admission.

## 4. Status semantics

### `PASS`

Hermes received and integrity-verified the source, completed the requested
technical method(s), and produced the method's expected result shape. `PASS`
does not mean trusted, admissible, complete, associated, human-reviewed, or
analysis-ready.

### `REVIEW_REQUIRED`

Hermes completed enough processing to return useful candidate material, but an
explicit issue requires Parker or a human reviewer to assess it. Examples are
low or scoped OCR confidence, ambiguous structure, unsupported portions,
corruption with recoverable output, incomplete accounting, or a processor
qualification. The response must retain useful representations and include
structured issues. Hermes does not make the review decision final.

### `FAILED`

Hermes could not produce a usable result for the requested method. The
`failure` object is required. Partial diagnostic material may be returned only
if its meaning is explicit; it must not be presented as a successful
representation.

Transport authentication failure, framing failure, source hash mismatch,
resource-limit rejection, unsupported media type, timeout, malformed input,
and processor failure are all fail-closed conditions. Their retryability is
specified in `failure`, not inferred from the top-level status.

## 5. Representation model

Representations are typed, independently provenance-bound results. The
contract does not use one union object with unrelated nullable fields.

### Text

```json
{
  "type": "TEXT",
  "method": "NATIVE_TEXT_EXTRACTION",
  "content": "..."
}
```

### Structured document

```json
{
  "type": "STRUCTURED_DOCUMENT",
  "method": "XLSX_STRUCTURE_EXTRACTION",
  "content": {
    "sheets": []
  }
}
```

### OCR text

```json
{
  "type": "OCR_TEXT",
  "method": "OCR",
  "content": "...",
  "regions": [
    {
      "page": 3,
      "region": {"x": 10, "y": 20, "width": 300, "height": 40},
      "text": "...",
      "confidence": {
        "value": 0.81,
        "semantics": "processor-reported-region-confidence",
        "scope": "region"
      }
    }
  ]
}
```

### Transcript

```json
{
  "type": "TRANSCRIPT",
  "method": "AUDIO_TRANSCRIPTION",
  "content": "...",
  "language": "en",
  "segments": [
    {
      "startSeconds": 0.0,
      "endSeconds": 4.2,
      "text": "...",
      "confidence": {
        "value": 0.91,
        "semantics": "processor-reported-segment-confidence",
        "scope": "segment"
      }
    }
  ]
}
```

Confidence is optional and scoped. Hermes must omit it when the processor does
not supply a meaningful, documented measure. It must not invent a universal
document score or convert a status into a numeric confidence.

Structured issues use stable codes and explicit locations where available:

```json
{
  "code": "LOW_OCR_CONFIDENCE",
  "severity": "REVIEW",
  "explanation": "Processor confidence is below the configured threshold.",
  "location": {"page": 3, "region": "r17"}
}
```

The minimum issue severity vocabulary is `INFO`, `WARNING`, `REVIEW`, and
`ERROR`. Issue codes and location schemas are method-specific and must be
versioned with the method catalogue.

Failures are structured:

```json
{
  "kind": "UNSUPPORTED_MEDIA_TYPE",
  "detail": "No v1 processor route accepts the detected media type.",
  "retryable": false
}
```

## 6. Provenance model

Every representation and candidate artifact carries or references an
immutable provenance record containing, where applicable:

- original `sourceSha256`, source byte length, and opaque `source.reference`;
- request, job, occurrence, and representation identities;
- ordered method names and method versions;
- processor name/version and configuration identity or hash;
- tool/provider and model/version for OCR or transcription;
- processing start and completion timestamps;
- the exact transformation sequence, including decode, rasterization,
  normalization, segmentation, or format conversion;
- parent/child derived-object relationships;
- DOCX package part, relationship, embedded-image identity and source order;
- page, sheet, slide, message-part, region, or audio time-range mapping;
- warnings, unsupported portions, and completeness/accounting observations.

For image-only DOCX, each image derivative and OCR representation must point
to the original DOCX source digest and identify the embedded image relationship
or stable package-part/index identity. Where document ordering or placement is
not determinable, provenance says so instead of inventing coordinates.

For audio, each transcript segment records its source time range. A derived
audio segment must also retain the original source digest and the transformation
that produced the segment.

Parker-local filesystem paths are excluded from provenance. Only an explicit
governed shared-storage contract could make such a path meaningful; v1 uses
authenticated bytes and opaque references.

## 7. Idempotency and retry rules

`requestId` is the idempotency key for the complete logical request. Hermes
maintains a bounded durable or otherwise restart-safe idempotency record keyed
by authenticated Parker principal and `requestId`.

For the same principal and `requestId`:

1. The request metadata, source digest, source length, requested methods, and
   relevant limits must match byte-for-byte after canonical validation.
2. A completed response is returned exactly, including status, issues,
   representation identities, artifact references, and provenance.
3. Hermes does not run the processor again or create duplicate artifacts.
4. A request with the same `requestId` but conflicting input is rejected as
   `IDEMPOTENCY_CONFLICT`; it is never merged with the original.
5. A recorded in-progress request is resumed or returns a deterministic
   retryable-in-progress response; concurrent duplicate execution is not
   allowed.

`occurrenceId` and `sourceSha256` are correlation and identity constraints, not
substitutes for `requestId`. A new request with a new `requestId` but the same
occurrence and source may be allowed only as an explicit retry attempt. It must
produce a new attempt identity and must not mutate or overwrite the prior
response. Parker determines whether that attempt may replace, supplement, or
remain alongside the earlier technical result.

If the same source is sent under a different `occurrenceId`, Hermes treats it
as a distinct Parker request and does not infer association. If the same
`requestId` is paired with a different source digest, Hermes fails closed.

## 8. Security and transport

v1 uses the existing SSH/forced-command model with these requirements:

- Parker authenticates with a dedicated key whose authorized key is restricted
  to the fixed Hermes processing wrapper.
- `IdentitiesOnly=yes`, `BatchMode=yes`, strict host-key verification, and a
  pinned `UserKnownHostsFile` remain mandatory.
- The remote wrapper accepts only the v1 framed envelope and bytes on stdin;
  no caller-controlled command, path, shell fragment, or provider name is
  executed.
- The authenticated Parker principal must be authorized for the requested
  processing capability. Authentication alone is not Parker admission.
- Hermes rejects requests from unknown principals, unsupported protocol
  versions, malformed envelopes, mismatched lengths, and unknown methods.
- Source and output sizes, archive expansion, MIME nesting, page/pixel counts,
  audio duration, CPU, memory, child-process count, and wall-clock time are
  bounded. Local Hermes limits may be stricter than Parker's requested limits.
- `originalFilename` is metadata only. It is sanitized for display and never
  used to construct a path. No `..`, absolute path, shell syntax, or archive
  member name can escape a Hermes-controlled temporary root.
- Temporary files are created below a confined, service-owned directory with
  non-following link behavior, unique names, cleanup, and no execution of
  macros, embedded programs, links, or package relationships.
- Network access from processors is denied by default. A provider/model that
  requires network access needs a separately governed capability and explicit
  configuration.
- Every failure is fail-closed. No partial output is reported as `PASS`, and
  no processor result bypasses Parker validation or admission.

TLS is not required for the v1 SSH transport. If a future HTTPS transport is
chosen, it must provide equivalent mutual authentication, endpoint identity,
request authorization, bounded body handling, and forced operation semantics;
it is not wire-compatible by implication.

## 9. Format routing matrix

Parker selects the requested technical route from its governed format policy;
Hermes executes only the selected method. Extension is a hint, while bytes,
declared media type, and detected structure control safe routing.

| Input | Hermes method(s) | Representation type(s) | `PASS` | `REVIEW_REQUIRED` | `FAILED` |
|---|---|---|---|---|---|
| PDF with native text | `PDF_INSPECTION`, `NATIVE_TEXT_EXTRACTION` | `TEXT`, optional `STRUCTURED_DOCUMENT` | Pages and native text extracted within limits; accounting recorded | Some pages empty/ambiguous, damaged optional content, or incomplete page accounting with useful text | Hash/framing failure, unreadable PDF, limit breach, or no usable result |
| Scanned PDF | `PDF_INSPECTION`, `RASTERIZE`, `OCR` | `OCR_TEXT` | Pages rasterized and OCR returned with page/region provenance | Low scoped confidence, unreadable pages, partial OCR, or uncertain page accounting | Cannot open/rasterize, OCR unavailable, or no usable output |
| JPG/JPEG/PNG/TIFF | `IMAGE_INSPECTION`, `OCR` | `OCR_TEXT` | Image decodes and requested OCR completes | Low confidence, unsupported frame/region, or degraded decode with useful OCR | Decode failure, limit breach, OCR failure, or no usable result |
| DOCX native text | `DOCX_PACKAGE_INSPECTION`, `NATIVE_TEXT_EXTRACTION` | `TEXT`, `STRUCTURED_DOCUMENT` | Package and supported body structures extracted | Unsupported parts, ambiguous relationships, or incomplete accounting with useful output | Invalid package, unsafe expansion, or no usable result |
| DOCX image-only / embedded images | `DOCX_PACKAGE_INSPECTION`, `EMBEDDED_IMAGE_EXTRACTION`, `OCR` | `STRUCTURED_DOCUMENT`, child `OCR_TEXT` | Images extracted and OCR results retain package provenance | Missing/unsupported images, low confidence, or incomplete relationship accounting | Invalid package, extraction failure, or no usable representation |
| XLS/XLSX | `SPREADSHEET_INSPECTION`, `XLS_STRUCTURE_EXTRACTION` or `XLSX_STRUCTURE_EXTRACTION` | `STRUCTURED_DOCUMENT` | Sheets/cells and supported structure extracted | Unsupported features, formulas/objects not represented, or partial sheet accounting | Invalid workbook, unsafe expansion, or no usable structure |
| TXT/CSV/RTF | `TEXT_INSPECTION`, `NATIVE_TEXT_EXTRACTION` or `CSV_STRUCTURE_EXTRACTION` or `RTF_TEXT_EXTRACTION` | `TEXT`, `STRUCTURED_DOCUMENT` | Decoding and supported structure complete | Encoding ambiguity, malformed records, or incomplete accounting with useful output | Decode/parse failure, limit breach, or no usable result |
| EML/MSG | `MESSAGE_INSPECTION`, `MESSAGE_STRUCTURE_EXTRACTION` | `STRUCTURED_DOCUMENT`, `TEXT` | Headers/body/MIME structure accounted for within declared scope | Malformed parts, undecodable body, or attachments requiring separate governed ingestion | Invalid container, unsafe nesting, or no usable message result |
| WAV | `AUDIO_INSPECTION`, `AUDIO_TRANSCRIPTION` | `STRUCTURED_DOCUMENT` for audio facts, `TRANSCRIPT` | Audio decodes and transcript completes with segment timing | Low scoped confidence, uncertain language, silence, clipping, or partial segments | Invalid audio, unsupported codec, limit/timeout, or transcription failure |
| M4A | `AUDIO_INSPECTION`, `AUDIO_TRANSCRIPTION` | `STRUCTURED_DOCUMENT` for audio facts, `TRANSCRIPT` | Container/codec decodes and transcript completes with segment timing | Codec quirks, low scoped confidence, or partial/ambiguous transcript | Invalid container, unsupported codec, limit/timeout, or transcription failure |

The matrix describes technical outcomes only. A technical `PASS` remains a
candidate result for Parker's validation and admission handoff.

## 10. Audio transcription contract

Audio transcription is one Hermes processing method, not a second trust or
admission pipeline. It uses the same request identity, authenticated byte
transport, SHA-256 verification, idempotency rules, response envelope, issue
model, and provenance model as document processing.

The audio route must record:

- detected container, codec, sample rate, channels, duration, and decode facts;
- transcription method, provider/tool, model/version, and configuration;
- language selection or detection and its scope;
- ordered transcript segments with start/end seconds;
- processor-supplied segment confidence only where its semantics are known;
- silence, overlap, clipping, unsupported channel, and undecidable-language
  issues where reported.

An audio `PASS` means a usable technical transcript was produced. It does not
mean the transcript is verbatim, complete, truthful, or admitted. A transcript
with useful partial segments and explicit degradation is normally
`REVIEW_REQUIRED`; an audio decode, timeout, authentication, or processor
failure with no usable result is `FAILED`.

## 11. Image-only DOCX fallback contract

An image-only DOCX is processed as a source package and never flattened into
unrelated OCR text:

```text
DOCX source bytes
  → package inspection
  → embedded image candidate derivatives
  → per-image OCR representations
```

Each embedded image derivative records:

- original DOCX `sourceSha256` and opaque source reference;
- package-part or relationship identity, image index, and extraction order;
- image bytes digest, length, media type, and transformation applied;
- document location or ordering when determinable;
- the parent representation and child relationship.

Each OCR representation records the image derivative identity plus OCR method,
tool/model, regions, scoped confidence, and issues. If an image cannot be
extracted or its order/location is unknown, that fact is represented as a
structured issue; provenance is not guessed.

The extracted image is a candidate derivative, not automatically a new Parker
source or admitted evidence. Parker separately decides whether to custody it,
associate it, route it for review, or use its OCR representation.

## 12. Parker admission handoff

The handoff is one-way in authority:

```text
Parker governed source
  → authenticated Hermes request
  → Hermes technical response/candidate artifacts
  → Parker digest and schema verification
  → Parker-owned validation, accounting, and audit
  → Parker admission decision
  → Parker association/review/final processing state
```

Parker verifies at minimum:

- response protocol, request, job, occurrence, and source identities;
- returned source digest against its governed source identity;
- representation/artifact digest, length, media type, and provenance links;
- method declarations against the requested route;
- issue, failure, and status consistency;
- required format-specific accounting and resource disclosures.

Parker maps Hermes `PASS`, `REVIEW_REQUIRED`, and `FAILED` into its own
governed processing state. The mapping does not automatically admit a
representation. `REVIEW_REQUIRED` remains subject to Parker's human-review
workflow. A Hermes response cannot create a case association, trusted
evidence identity, analysis eligibility, or final state by itself.

## 13. Open questions / decisions requiring owner approval

1. Approve authenticated byte upload over the existing SSH/forced-command
   boundary as the v1 transport, including the exact canonical framing.
2. Set production source, output, archive-expansion, page/pixel, audio-duration,
   timeout, CPU, memory, and concurrency limits.
3. Approve the exact Hermes principal-to-capability authorization mapping and
   key rotation/revocation procedure.
4. Select the durable idempotency-record retention period and behavior after
   Hermes restart or storage loss.
5. Approve the complete v1 method catalogue, issue-code registry, and typed
   representation schemas for each format.
6. Decide which large-result artifact retrieval mechanism Parker will use and
   where candidate artifact bytes live before Parker admission.
7. Approve processor/provider/model choices and the required disclosure fields
   for OCR and transcription confidence.
8. Define Parker's exact mapping from Hermes statuses/issues to its existing
   processing, review, completeness, audit, and admission records.
9. Decide whether EML/MSG attachments are returned only as candidate child
   artifacts or may be submitted through a separate Parker-created request.
10. Confirm whether `jobId` and `occurrenceId` are both required for every
    route or whether one may be absent for a standalone technical inspection.
11. Approve the canonical timestamp precision, serialization rules, and
    representation/provenance digest model.
