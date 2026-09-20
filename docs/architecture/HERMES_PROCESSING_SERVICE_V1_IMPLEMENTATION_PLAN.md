# Hermes Processing Service v1 — Implementation Plan

## Status and governing inputs

This is an implementation plan only. It does not implement, deploy, or
authorize production routing. It is governed by:

- `HERMES_PROCESSING_SERVICE_V1_CONTRACT_DESIGN.md`;
- `HERMES_PROCESSING_SERVICE_V1_SCOPE_LOCK.md`;
- `HERMES_PROCESSING_SERVICE_V1_NUMERIC_LIMIT_PROPOSAL.md` with its owner-frozen
  global ceilings;
- existing Parker evidence, ingestion, and Hermes governance contracts.

The initial slice excludes audio, OCR, image-only DOCX fallback, multipage TIFF
OCR, immutable large artifacts, and automatic EML/MSG attachment processing.

The seven Unit 1 owner decisions are frozen: SSH length-delimited UTF-8 JSON
framing; authenticated byte upload; required governed-ingestion fields without
`caseId`; closed registries; category/detail-code/retryability failure policy;
service-envelope wrapping of the existing `HermesProcessingResult`; and a
seven-day durable Hermes request/result ledger with startup and TTL cleanup.

## Existing primitives to reuse

The implementation must reconcile with, rather than recreate:

| Existing primitive | Plan treatment |
|---|---|
| `src/interfaces/HermesProcessingResult.kt` | Extend or adapt the existing pre-ingestion result vocabulary; do not create a parallel PASS/REVIEW_REQUIRED/FAILED taxonomy. Its existing bounded text, issue, failure, representation, and SHA-256 invariants are inputs to the v1 schema. |
| `src/runtime/DurableHermesProcessingRegistries.kt` and `HermesProcessingResultRegistry` | Reuse atomic filesystem and locking patterns. The current registry key `(batchId, sourceSha256)` is not sufficient for v1 `requestId` idempotency and must not be misrepresented as the new ledger. Add the smallest compatible request/result ledger contract. |
| `tools/hermes_processing_ingest.py` | Reuse existing native extraction functions, temporary-workspace discipline, SHA-256 helper, and Parker submission concepts where compatible. Its current HTTP batch orchestration is not the SSH framed service contract. |
| `tools/hermes_format_catalogue.py` | Reuse the existing supported-format catalogue and method routing; do not invent a second format registry. |
| `src/runtime/HermesSshSpeechTranscriber.kt` | Preserve as the existing STT SSH transport primitive during migration. It is not enabled by the initial slice and must not become a second logical processing contract. |
| `src/runtime/FileSystem*` durable stores | Reuse write-once, atomic temporary-file, lock, root-confinement, and startup orphan-cleanup patterns where their existing contracts fit. |
| Existing Python/Kotlin Hermes tests | Extend focused tests and end-to-end coverage; do not duplicate existing result invariant tests unnecessarily. |

## Unit 1 — Protocol value types and registries

**Objective.** Implement the v1 request/response value types, required
identifiers, closed method and representation registries, failure
categories/detail codes, optional-field rules, canonical serialization rules,
and UTC millisecond timestamp format, using the owner-frozen registries and
canonical serialization rules.

**Files/components likely affected:**

- `src/interfaces/HermesProcessingResult.kt`;
- new or extended protocol value types in the existing interfaces package;
- the existing Hermes binary/JSON codec location, if applicable;
- `tools/hermes_format_catalogue.py` only where identifiers must be reconciled;
- protocol/value tests under `tests/contracts` and `tests/runtime`.

**Tests:** required-field and identifier validation; no `caseId` acceptance;
opaque `batchId`; SHA-256, filename, media type, and size bounds; unknown
method/representation rejection; status/failure/issue invariants; canonical
serialization and timestamp round trips; compatibility fixtures for existing
`HermesProcessingResult` consumers.

**Success criteria:** one authoritative v1 model exists; all scope-locked
values are validated before transport or processing; every failure has a stable
category/detail code, retryable boolean, and bounded detail.

**Explicit non-goals:** SSH transport, processor invocation, OCR/audio types,
large-artifact references, and production endpoint wiring.

## Unit 2 — SSH forced-command framing/parser

**Objective.** Implement and test the fixed forced-command boundary that reads
a length-delimited UTF-8 JSON metadata envelope with explicit metadata and
source lengths, followed by exactly `sizeBytes` raw bytes.

**Files/components likely affected:** the established Hermes SSH
forced-command wrapper location; framing/codec component; candidate deployment
configuration; transport tests and bounded-stream fixtures.

**Tests:** valid envelope; truncation; extra bytes; invalid length; oversized
metadata/source; malformed JSON; invalid UTF-8; unexpected EOF; shell/path/
provider injection; duplicate/ambiguous fields; unsupported protocol version;
slow sender; timeout; disconnect; bounded parser memory.

**Success criteria:** only the canonical operation is accepted; framing
failures map to `MALFORMED` or `RESOURCE_LIMIT`; no arbitrary command or path is
interpreted.

**Explicit non-goals:** processor invocation, HTTP transport, shared mounts,
TLS endpoint, and source admission.

## Unit 3 — Authentication and capability enforcement

**Objective.** Bind the dedicated Parker SSH principal to the fixed wrapper and
enforce Hermes-side capability allowlisting independently of caller values.

**Files/components likely affected:** SSH authorized-key/forced-command
configuration; Hermes principal/capability policy; security configuration;
authentication and authorization tests.

**Tests:** valid principal and allowed initial method; unknown/revoked
principal; method/provider injection; disallowed capability; malformed or
missing authorization metadata; host-key and key-path checks.

**Success criteria:** authentication identifies the caller, authorization
permits only frozen initial methods, and caller data cannot create authority.

**Explicit non-goals:** OCR/audio capability, provider selection, case
authorization, and Parker admission permission.

## Unit 4 — Source byte receipt and SHA-256 verification

**Objective.** Enforce source size/length, stream bytes safely to a confined
workspace, compute SHA-256 independently, and reject mismatch before format
processing.

**Files/components likely affected:** framed request receiver; confined
workspace/temp-file helper; existing SHA-256 helpers; source integrity tests.

**Tests:** matching and mismatching digest; declared-size mismatch; zero-length
and oversized source policy; filename/path sanitization; cleanup on success,
failure, timeout, disconnect; startup orphan cleanup.

**Success criteria:** no processor sees bytes before integrity and bounds pass;
source/intermediate files are removed after successful handoff; orphaned
workspaces are removed at startup.

**Explicit non-goals:** shared mounts, source custody/admission, derivative
promotion, and large external artifact persistence.

## Unit 5 — Durable idempotency ledger

**Objective.** Implement Parker request identity integration and Hermes durable
records that return the exact prior result for a matching request and fail
closed on identity conflict.

**Files/components likely affected:** extension/companion to
`HermesProcessingResultRegistry`; `DurableHermesProcessingRegistries.kt`
atomic persistence patterns; retention/cleanup worker or startup maintenance;
ledger tests.

**Tests:** first request; same request/source returns identical result; same
request with different source, size, method, or limits fails; concurrent
duplicates do not process twice; crash/restart recovery; terminal/in-progress
retention for 7 days; TTL expiry; corruption fails closed.

**Success criteria:** retries are deterministic, duplicate results are not
created, and ledger corruption/write failure fails closed.

**Explicit non-goals:** automatic retry policy, human review, and large
artifact storage.

## Unit 6 — Native-text and structured processor adapter

**Objective.** Adapt existing supported native/structured extraction methods to
the v1 request/response contract with bounded inline representations only.

**Files/components likely affected:** `tools/hermes_processing_ingest.py`
native helpers; `tools/hermes_format_catalogue.py`; processor adapters and
format validators; existing fixtures and `tests/tools/test_hermes_processing_ingest.py`.

**Tests:** supported PDF native text, DOCX text, XLS/XLSX structure, TXT/CSV/
RTF, and already-supported EML/MSG native structure; declared/detected media
mismatch; corrupt, unsupported, empty, oversized inputs; representation and
inline ceilings; no automatic attachment submission; no method/provider
authority bypass.

**Success criteria:** existing native/structured capabilities produce typed
inline results with method, issue, failure, and provenance; over-limit output
fails closed as `RESOURCE_LIMIT` unless a separately frozen method contract
explicitly permits `REVIEW_REQUIRED`, without truncation.

**Explicit non-goals:** OCR, audio, image-only DOCX fallback, multipage TIFF
OCR, external artifact transfer, and new providers.

## Unit 7 — Response and provenance serialization

**Objective.** Serialize response, issue, failure, processor, method,
transformation, and source provenance deterministically with UTC millisecond
timestamps and canonical digests where required.

**Files/components likely affected:** Unit 1 codecs/value types; existing
provenance/reference interfaces and codecs; response validation;
serialization/provenance tests.

**Tests:** stable serialization; digest reproducibility; method and
representation order; processor/tool/version capture; no fabricated
confidence; no Parker-local path leakage; status/issue/failure consistency.

**Success criteria:** the same technical result has stable serialized identity
and enough provenance to reconstruct what Hermes actually did.

**Explicit non-goals:** OCR regions, audio segments, large-artifact provenance,
and Parker admission records.

## Unit 8 — Parker client/service adapter

**Objective.** Provide Parker-side request construction, SSH invocation,
response validation, and handoff into existing Parker processing state without
moving authority to Hermes.

**Files/components likely affected:** existing Parker/Hermes ingestion adapter;
compatible concepts from `tools/hermes_processing_ingest.py`; Parker
processing-result coordination; job/occurrence/batch identity integration;
adapter and contract tests.

**Tests:** required identity propagation without `caseId`; exact bytes/digest;
response identity mismatch; unknown methods/representations;
PASS/REVIEW_REQUIRED/FAILED mapping; transport/auth/integrity/resource/timeout
mapping; no automatic admission or association.

**Success criteria:** Parker submits one governed v1 request and validates/stores
the technical result while retaining Parker-owned admission, association,
review, and final state.

**Explicit non-goals:** production route enablement, UI change, case transfer,
audio, and OCR wiring.

## Unit 9 — End-to-end verification

**Objective.** Prove the initial slice across Parker, SSH forced command, Hermes
processing, durable records, cleanup, and response handoff.

**Files/components likely affected:** integration/composition harness; fixtures
and temporary roots; security/deployment verification scripts.

**Tests:** one positive case per supported initial format; malformed framing,
bad credentials, unauthorized method, hash mismatch, timeout, unsupported
format, processor failure, and over-limit cases; retry after success/restart;
workspace cleanup; provenance/digest replay; no duplicate work; existing STT
transport remains unchanged and unenabled.

**Success criteria:** all scope-lock production-gate criteria pass against an
identified build, with security failures and technical statuses distinguishable.

**Explicit non-goals:** production deployment, audio/OCR acceptance, and large
artifact acceptance.

## Unit 10 — Production deployment gate

**Objective.** Prepare an owner-reviewed candidate and verify deployment
configuration, principal/key policy, limits, retention, cleanup, monitoring,
and rollback preserve the frozen boundary.

**Files/components likely affected:** deployment/systemd/forced-command
configuration; key installation/rotation runbook; monitoring and operational
documentation; production-candidate verification artifacts.

**Tests/evidence:** exact build/source identity; clean candidate; key/host
verification; limit/timeout readback; no shared mount/path capability; rollback
rehearsal; approved native/structured smoke fixture; owner acceptance of
numeric limits and security runbook.

**Success criteria:** owner approval is recorded, the candidate passes the
deployment gate, and no route is enabled beyond the frozen initial slice.

**Explicit non-goals:** deployment is not authorized by this document; audio,
OCR, image-only DOCX, TIFF OCR, and large external artifacts remain disabled.

## Unit 1 decision status

Unit 1 is now unblocked by owner decision. The implementation must treat the
following as fixed constraints:

- source ceiling 500 MiB; metadata envelope 64 KiB; inline response 8 MiB;
  extracted text 4,000,000 characters; structured representation 8 MiB;
  timeout 300 seconds; request/result retention 7 days; temporary workspace
  TTL 24 hours; filename 255 UTF-8 bytes; at most 100 issues and 32
  representations;
- UTF-8 length-delimited JSON metadata with explicit metadata/source lengths,
  exact raw bytes, canonical digest-sensitive serialization, and fail-closed
  framing validation;
- required `protocolVersion`, `requestId`, `jobId`, `occurrenceId`, opaque
  `batchId`, `sourceSha256`, `sizeBytes`, `originalFilename`, and `mediaType`,
  with no `caseId`;
- closed versioned method, representation, issue, and failure registries;
- top-level failure categories with stable detail code, retryable boolean, and
  bounded detail;
- a service envelope wrapping, rather than redefining, the existing
  `HermesProcessingResult` semantics;
- durable request/result records across restart, exact same-request replay,
  fail-closed identity conflict, bounded expiry, and startup/TTL workspace
  cleanup.

No owner decision remains that blocks Unit 1. Implementation still must resolve
ordinary engineering details within these constraints, including concrete
registry member lists, storage paths, codec placement, and test fixture
construction. Those details cannot weaken or expand the frozen contract.
