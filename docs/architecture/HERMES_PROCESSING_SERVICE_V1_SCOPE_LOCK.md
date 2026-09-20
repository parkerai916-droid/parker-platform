# Hermes Processing Service v1 — Scope Lock

## Status

Owner decisions are frozen for the initial implementation slice. This is a
design and implementation boundary only. It does not implement the service,
authorize production routing, or replace any existing Parker governance
contract.

No production route may invoke this service until the implementation and
acceptance criteria in the companion implementation plan have passed.

## 1. Initial implementation scope

V1 initial implementation includes only:

- the SSH forced-command transport;
- canonical length-delimited metadata framing followed by the exact source
  byte stream;
- strict protocol parsing and closed v1 value registries;
- dedicated Parker authentication and Hermes capability authorization;
- exact source byte receipt, size enforcement, and independent SHA-256
  verification;
- Parker request identity plus Hermes durable idempotency/result records;
- the frozen top-level failure categories and stable method-specific detail
  codes;
- canonical response, configuration, and provenance serialization;
- bounded inline representations only;
- native-text and structured extraction methods already supported by the
  existing Hermes catalogue and processor path.

The implementation may reuse and adapt existing Parker/Hermes primitives. It
must not create a competing result, status, provenance, format catalogue, or
evidence-admission model.

## 2. Explicit exclusions from the initial slice

The following are not enabled, implemented, or production-approved by this
scope lock:

- audio transcription;
- OCR;
- image-only DOCX extraction and OCR fallback;
- multipage TIFF OCR;
- immutable large external representation artifacts;
- EML/MSG automatic attachment processing;
- arbitrary filesystem paths or shared source mounts;
- caller-selected provider, model, or method authority;
- Parker case identifiers sent to Hermes;
- Hermes evidence admission, case association, trust, review, or analysis
  readiness decisions;
- any production routing before the acceptance gate passes.

EML/MSG technical extraction may return candidate attachment metadata or child
artifact descriptions where the already-supported native processor does so, but
Parker must explicitly create any later governed child request.

## 3. Frozen trust boundary

Parker owns cases, batches, authorization, source/evidence identity, governed
admission, associations, occurrences, human review, and final processing state.

Hermes owns only technical source inspection, extraction, structured parsing,
processor diagnostics, and processor-supplied scoped uncertainty. Hermes does
not receive `caseId`, does not resolve `batchId` to a case, and does not create
or mutate Parker evidence or associations.

`PASS`, `REVIEW_REQUIRED`, and `FAILED` describe Hermes technical processing
only. None is Parker admission or trust.

## 4. Frozen transport and source identity

V1 uses authenticated byte upload over the existing SSH/forced-command
boundary. The wire shape is:

```text
[4-byte unsigned big-endian metadata length]
[exact metadataLength bytes of UTF-8 JSON metadata]
[exact source.sizeBytes raw source bytes]
[EOF]
```

The metadata length field is exactly four bytes in unsigned network byte order
and is limited to 64 KiB. Zero, invalid, short, or over-limit metadata lengths
are rejected. Metadata is decoded only after the exact declared byte count has
been received. The source byte count is defined by `source.sizeBytes`; short
reads and trailing bytes are rejected. There are no delimiter characters or
EOF-based source sizing, and no silent truncation.

The forced command is fixed. No caller-controlled shell command, path,
provider, or executable is accepted. No shared source mount is used.

`source.reference` is an opaque Parker correlation identity only. It is not a
path, URI to dereference, mount location, or authorization token. Hermes hashes
the received bytes independently and rejects any SHA-256 or byte-length
mismatch before processing.

## 5. Frozen request responsibilities

Governed v1 requests require:

- `protocolVersion`;
- `requestId`;
- `jobId`;
- `occurrenceId`;
- opaque Parker `batchId`;
- source SHA-256;
- original filename metadata;
- declared media type;
- exact source byte size.

Parker supplies identity, authorization context, requested method, and limits.
Hermes validates the envelope, verifies the source bytes, applies Hermes-owned
ceilings, and performs only the requested authorized technical method.

Parker-supplied limits may never exceed Hermes-owned ceilings. The owner-frozen
initial ceilings are recorded in `HERMES_PROCESSING_SERVICE_V1_NUMERIC_LIMIT_PROPOSAL.md`.

## 6. Frozen protocol and response responsibilities

The metadata envelope is UTF-8 JSON with the wire framing above, an explicit
metadata length, and explicit source byte length. Canonical serialization is
used wherever identity or a digest depends on serialized data. Hermes rejects
malformed framing, excess bytes, short reads, invalid lengths, duplicate or
ambiguous fields, unsupported protocol versions, and non-UTF-8 input.

Response framing uses the same convention:

```text
[4-byte unsigned big-endian response body length]
[exact responseLength bytes of canonical UTF-8 JSON response]
[EOF]
```

The response body is limited to 8 MiB and is never silently truncated.

The service envelope wraps the existing `HermesProcessingResult` model. The
existing model's established semantics are preserved; the envelope owns
protocol identity, request correlation, source identity, framing/integrity,
idempotency, typed representations, provenance, and issue/failure metadata.

Hermes returns the validated request correlation identities, the independently
computed source digest, methods actually run, bounded typed inline
representations, structured issues, structured failure where applicable,
processor identity/version, and canonical provenance.

Hermes must not fabricate confidence. It may report only processor-supplied
confidence with documented scope and semantics.

Initial v1 supports inline results only. Outputs exceeding the approved inline
ceilings fail closed as `FAILED` with `RESOURCE_LIMIT` unless a separately
frozen method contract explicitly permits `REVIEW_REQUIRED`; they must not be
truncated silently.
Immutable large-artifact transfer is a later unit.

## 7. Idempotency and retention boundary

Parker owns the durable attempt/request identity. Hermes maintains durable
request/result records across restart for 7 days after the terminal result.
Expired records may be removed by bounded cleanup. Orphaned temporary
workspaces are cleaned at startup and by the 24-hour TTL policy.

For the same authenticated Parker principal and `requestId`:

- the source identity, request metadata, methods, and relevant limits must
  match;
- a completed result is returned exactly;
- duplicate processing and duplicate result creation are forbidden;
- reuse with a different source identity fails closed;
- in-progress duplicate handling is deterministic and does not permit
  concurrent duplicate execution.

Retention and recovery behavior are implementation obligations, not permission
to discard Parker's attempt history or change a prior result.

## 8. Authorization and failure boundary

The service uses a dedicated Parker processing SSH principal and Hermes-side
capability allowlisting. Caller-supplied method/provider values cannot create
authority. Authentication, authorization, source integrity, protocol, and
processor outcomes remain distinguishable.

Frozen top-level failure categories are:

```text
TRANSPORT
AUTHORIZATION
INTEGRITY
UNSUPPORTED
MALFORMED
RESOURCE_LIMIT
TIMEOUT
PROCESSOR
INTERNAL
```

Each category may contain a stable method-specific detail code. Retryability is
policy-derived from category/detail, never inferred from free text. Every
failure carries the stable category, stable detail code, retryable boolean, and
bounded human-readable detail.

## 9. Production gate

No Parker production route may be switched to this service until all of the
following are demonstrated:

1. strict framing rejects truncation, extra bytes, malformed lengths, and
   oversized envelopes;
2. authentication and capability allowlisting fail closed;
3. source size and SHA-256 are independently enforced;
4. protocol registries reject unknown v1 methods and representations;
5. idempotent retries return the same durable result without duplicate work;
6. temporary source/intermediate data is removed after successful handoff and
   orphaned workspaces are cleaned at startup;
7. inline result ceilings reject or qualify over-limit output without
   truncation;
8. provenance and canonical serialization are stable and verifiable;
9. existing native-text/structured formats pass positive, malformed,
   unsupported, oversized, and timeout tests;
10. Parker-side response validation preserves the existing trust boundary;
11. the full targeted test suite, security tests, and end-to-end verification
    pass at an identified build;
12. the owner-approved numeric limits and deployment/security runbook are
    recorded.

Audio, OCR, image-only DOCX, TIFF OCR, and external large artifacts require
separate implementation units and acceptance gates.
