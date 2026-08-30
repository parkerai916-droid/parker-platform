# FA.9.4P-A1E-R6.10B5 — Consolidated implementation-governance closure amendment

## 1. Authority, scope, and precedence

This amendment supplements the R6.10B ordinary external region-ingestion architecture and its
R6.10B1, R6.10B2, and R6.10B3 amendments. It closes all six implementation-blocking governance
findings G1–G6 in the R6.10B4 collect-all audit. It supersedes only conflicting or previously
undefined portions of those documents; all unaffected governance remains intact.

The governing inputs, verified byte-for-byte at repository starting commit
`5cb46084b958e76986b0bddd2cdaf0d40c1d775f`, are:

- R6.10B: `dfa199633f651acd9f81e927ecf04fcb1a4a89efea94aecd31025ec41eca15ad`
- R6.10B1: `fd1ca7867c5103fa9a9519b3687e9f04b87b79b158bb741fd904d0d6766198a6`
- R6.10B2: `b72aefabeeaba6dd80f8c2ca46bb8cf495e336b0626e5a398fa7a18291fdb1c4`
- R6.10B3: `4b9fedd1fcc9d3454f4511d1daa3c7c6540bab4928460de8964437b7bcdf3d3f`
- R6.10B4: `f74c9defd5b62940752ba36af4580e4c36b928fbac2d4cb28584ba608230b6b2`

This is a governance document only. It does not implement, deploy, restart, call a provider, create
an acceptance record or owner authorization, or mutate production.

## 2. G1 — initial ordinary accepted media surface

The initial ordinary region-v5 accepted media surface is exactly `application/pdf`.

The accepted path is authoritative PDF evidence → deterministic PDFBox page rendering →
deterministic Parker region geometry → region-v5 external transcription. Existing renderer support
for `image/png` does not confer ordinary region-v5 acceptance. `image/png`, `image/jpeg`,
`image/webp`, DOC/DOCX, email, spreadsheets, and every other media type are outside this initial
surface. Future media require separate governance and acceptance.

For any governed media identity other than exactly `application/pdf`, ordinary region-v5 execution
returns `UNSUPPORTED_MEDIA` before transmission-capacity reservation, request construction,
attempt-start, or provider transport. It performs no silent fallback inside the region-v5 executor.
The acquisition router may separately select a native/local mechanism under that mechanism's own
governance.

## 3. G2 — one request and bounded request surface

One owner authorization plus one ordinary execution plus one attempt identity permits at most one
provider transport invocation. Multi-request batching, splitting, automatic truncation, region
omission, and attempt chaining are not authorized.

An ordinary request must contain the complete deterministically derived region set, with a region
count in `1..32` inclusive. The upper bound is the existing
`RegionTranscriptionRequest.MAX_REGIONS_PER_REQUEST = 32`. More than 32 regions yields
`REQUEST_BOUNDS_EXCEEDED` before `PROVIDER_ATTEMPT_STARTED`, with no request and zero provider
calls.

The current deterministic `SourcePageRendererLimits` already imposes these stricter preparation
bounds, which ordinary execution preserves:

- maximum authoritative source bytes: `64 * 1024 * 1024` (67,108,864 bytes);
- maximum PDF pages: `200`;
- maximum rendered dimension: 20,000 pixels;
- maximum decoded pixels per page representation: 50,000,000.

Thus no new arbitrary page-count limit is introduced. A PDF outside the existing renderer bounds
fails closed during source preparation. The one-request complete-region-set rule remains the
controlling egress bound.

### 3.1 Encoded request/body bound discovery and rule

No finite aggregate encoded request/body size limit exists at the current region-v5 request-builder
or transport boundary. `OpenAiRegionTranscriptionAdapter.buildRequestBody` constructs an aggregate
JSON string and passes it directly to `OpenAiResponsesTransportRequest`; it does not bound the
UTF-8 body before transport. The adapter's 20 MiB `maximumResponseBytes` is response-only.
`RegionTranscriptionImage.MAX_IMAGE_BYTES = 32 MiB` bounds each encoded image input independently,
not the aggregate request. The separate non-region external-transcription adapter's 96 MiB encoded
input approximation does not govern the region-v5 adapter.

R6.10C must therefore implement a finite deterministic aggregate encoded-request bound at or before
the region-v5 transport boundary. It must calculate the exact UTF-8 request-body size after
deterministic construction, reject oversize bodies before `PROVIDER_ATTEMPT_STARTED`, perform zero
provider calls, and document and test the selected engineering safety value. That value must not
exceed any hard/provider transport limit represented by the implementation at that time. No body
may be partially transmitted to discover oversize status.

## 4. G3 — zero derived regions

Zero regions is the pre-egress terminal/review disposition `NO_TRANSCRIBABLE_REGIONS`. Parker does
not construct a region-v5 request, record `PROVIDER_ATTEMPT_STARTED`, call the provider, or admit a
transcription derivative. It reports a source-grounded explanation to the owner. It is neither
provider success nor `NO_VISIBLE_TEXT`, and Parker must not manufacture an empty successful
derivative.

If the authorization was validly reserved, it remains `RESERVED_FOR_EXECUTION`, unconsumed and
bound to that execution. It cannot be reassigned; the owner may revoke it. A changed evidence or
mechanism requires a separately governed execution and new authorization.

## 5. G4 — source-order ambiguity

Ordinary region-v5 egress requires a deterministic R6.2 source-order graph classified
`UNAMBIGUOUS`. For such a graph, Parker's deterministic source order is authoritative and
provider-returned order remains provenance only.

`HUMAN_ORDER_REQUIRED` yields `SOURCE_ORDER_REVIEW_REQUIRED` before attempt-start.
`NOT_YET_SUPPORTED` yields `SOURCE_ORDER_NOT_SUPPORTED` before attempt-start. Both perform zero
provider calls, send no request, accept no provider-derived or guessed geometric order, and admit no
reconstructed transcription derivative. The owner receives the explicit review/unsupported result.

A reserved authorization remains `RESERVED_FOR_EXECUTION`, unconsumed, non-transferable, and
revocable. R6.10C need not implement human order editing; it implements these fail-closed pre-egress
dispositions.

## 6. G5 — deterministic derivative-generation identity

Ordinary region-transcription admission uses an execution-bound deterministic canonical generation
key. The canonical input fields are, in this order:

1. ordinary external region execution ID;
2. authoritative evidence ID;
3. authoritative source SHA-256;
4. accepted capability-record identity;
5. provider-state record identity;
6. reconstructed region-transcription content digest.

All are immutable before admission. The key digest is:

```
SHA-256(
  UTF8("parker.region-transcription.generation.v1")
  + canonical field framing
  + execution ID
  + evidence ID
  + source SHA-256
  + capability acceptance identity
  + provider-state record identity
  + reconstructed content digest
)
```

Every field is framed with Parker's existing unambiguous canonical length framing (or an exactly
equivalent documented length-prefixed encoding); unframed string concatenation is forbidden. If
the existing derivative API requires a UUID or string ID, R6.10C may deterministically map the
digest into that type only when the mapping is domain-separated, deterministic,
collision-resistant, documented and tested, and the original digest is retained in provenance.

### 6.1 Duplicate, conflict, and crash reconciliation

Recomputing the key after restart identifies the same admission operation. Matching immutable
content, generation, provenance, and audit state is idempotently completed or recovered; Parker
does not mint a second derivative. Any mismatch under that identity is `ADMISSION_CONFLICT` and
fails closed without overwrite or duplicate publication.

Recovery explicitly covers crash after content publication, after generation preparation or
publication, after audit publication, and after admission completion but before the attempt ledger's
later terminal update. Parker reconciles existing immutable state through the
`DerivativeGenerationCoordinator` create-once boundaries. Fully admitted state recovers the same
derivative identity and continues local terminal/result publication. Matching partial state resumes
or reconciles; conflicting partial state fails closed.

Admission already completed but not yet reflected in the attempt ledger is not provider-retryable.
The admitted state is recovered locally, and the ledger advances idempotently to the appropriate
local completion stage. There is no provider retry and no second derivative.

## 7. G6 — dynamic capability-acceptance lookup

The ordinary region-v5 capability evaluator performs a durable immutable-store lookup on every
eligibility/execution evaluation. It must not rely on a startup-only snapshot or require a restart
to observe a newly created valid record.

At startup the acceptance root must exist, be configured and be usable. Absence of a matching
record is not startup failure; the exact capability evaluates `NOT ACCEPTED`. A subsequently
created record bound to the exact region-v5 capability, R6.9 evidence chain, and embedded
`Parker-Source-Commit` becomes observable on the next evaluation without restart.

Records remain immutable and create-once. Conflict, ambiguity, corruption, disappearance, or store
unavailability fails closed for new ordinary executions. Parker must not retain stale in-memory
`ACCEPTED` state. Caching is permitted only if it never delays a new valid record, preserves
acceptance after unreadability/corruption, or accepts another build/capability; direct lookup per
evaluation is the simplest compliant design.

The R6.10D promotion sequence is consequently: build the exact R6.10C commit image; verify embedded
build identity; create/configure persistent roots and mounts; deploy with no region-v5 acceptance
record; start normally; observe `NOT ACCEPTED`; create one governed production acceptance record
for that exact running build/capability/evidence chain; observe `ACCEPTED` at the next provider-free
evaluation without restart. Promotion makes no provider call. Rollback to a different build fails
the B3 exact-build comparison.

## 8. Closed owner-visible disposition semantics

The initial implementation distinguishes at least these pre-egress dispositions, none of which
implies provider transmission:

- `UNSUPPORTED_MEDIA`
- `NO_TRANSCRIBABLE_REGIONS`
- `REQUEST_BOUNDS_EXCEEDED`
- `SOURCE_ORDER_REVIEW_REQUIRED`
- `SOURCE_ORDER_NOT_SUPPORTED`
- `CAPABILITY_NOT_ACCEPTED`
- `OWNER_AUTHORIZATION_REQUIRED`
- `OWNER_AUTHORIZATION_REVOKED`
- `OWNER_AUTHORIZATION_EXPIRED_BEFORE_RESERVATION`

It separately represents at least these post-attempt states:

- `PROVIDER_OUTCOME_UNKNOWN`
- `PROVIDER_RESPONSE_AVAILABLE`
- `VALIDATION_FAILED`
- `ADMISSION_CONFLICT`
- `ADMITTED`
- `REVIEW_REQUIRED`

Exact program enum and class names may follow Parker conventions, but these semantic distinctions
must not collapse.

## 9. Complete ordinary state-machine update

The ordinary state machine is proposal → grant → reservation → capability/media/source preflight →
custody retrieval → PDF rendering → deterministic region derivation/order classification → complete
bounded one-request construction → authorization/attempt-start linearization → one transport → raw
provider state → assessment/validation → Parker-order reconstruction → deterministic admission →
owner result.

The four formerly unresolved transitions are closed:

1. zero regions → `NO_TRANSCRIBABLE_REGIONS` → no attempt-start → zero provider calls;
2. `HUMAN_ORDER_REQUIRED`/`NOT_YET_SUPPORTED` → explicit review/unsupported disposition → no
   attempt-start → zero provider calls;
3. admission crash → deterministic generation-key reconciliation → matching state resumes or
   completes, conflicting state fails closed → no duplicate derivative or provider retry;
4. acceptance record appears while running → next durable evaluation observes exact valid record →
   `ACCEPTED` without restart.

After this amendment, unresolved governance state transitions are **0**.

## 10. Implementation-arrow recheck

| Arrow | Governing closure | Status after B5 |
|---|---|---|
| regions → request | PDF-only; complete set of 1–32 regions; exactly one request; renderer bounds retained; finite aggregate body bound required pre-egress | governed |
| result → Parker order | ordinary egress requires `UNAMBIGUOUS`; Parker graph controls reconstruction; ambiguous values terminate pre-egress | governed |
| derivative → admission | deterministic execution-bound generation key plus immutable duplicate/conflict reconciliation | governed |
| admission → owner result | distinct `ADMITTED`, `ADMISSION_CONFLICT`, `REVIEW_REQUIRED`, and recovery semantics | governed |
| acceptance → runtime eligibility | direct durable exact-record evaluation each time; absence/corruption/unavailability fail closed | governed |

Remaining governance-related unresolved implementation arrows: **0**.

## 11. Static implementability check

Repository inspection confirms all six decisions are implementable without a higher-order conflict:

- media identity already flows through authoritative acquisition input and renderer requests;
- `RegionTranscriptionRequest.MAX_REGIONS_PER_REQUEST` is exactly 32;
- renderer limits already provide the 64 MiB source and 200-page PDF bounds;
- `SourceRegionAmbiguityState` already contains `UNAMBIGUOUS`, `HUMAN_ORDER_REQUIRED`, and
  `NOT_YET_SUPPORTED`;
- `DerivativeGenerationCoordinator` already exposes content-first, prepare, audit, and publish
  create-once boundaries and accepts an ID factory, allowing a typed deterministic region path;
- immutable derivative-content/generation/audit stores provide reconciliation seams;
- the planned R6.10B acceptance store can perform create-once direct lookup;
- `ParkerRuntime.buildIdentity()` reads the embedded `Parker-Source-Commit` needed by B3;
- ordinary catalogue/evaluator/runtime composition can add the governed capability without changing
  historical acceptance paths.

The missing aggregate encoded-body guard is bounded implementation work, not a contradiction.

## 12. Engineering versus governance classification

All 22 R6.10B4 engineering gaps remain R6.10C work with now-defined semantics: acceptance
record/store/evaluator; proposal projection; authorization persistence; shared guard; phased
coordinator; bounded failure metadata; source preparation extraction; ordinary executor;
source-order reconstructor; region derivative type and codec; typed deterministic admission;
catalogue/config identity; owner API/UI/results; runtime composition and configuration; codec
migration; admission-restart, concurrency, end-to-end and provider-free preflight tests; and
implementation evidence documentation. Persistent Compose mounts remain R6.10D engineering.

Class names, DTOs, helper placement, packages and endpoint plumbing require no further governance.

## 13. Closure totals and R6.10C readiness

- R6.10B4 governance blockers remaining: **0**
- contradictions: **0**
- unresolved governance state transitions: **0**
- unresolved governance-related implementation arrows: **0**
- engineering gaps remaining for R6.10C: **22**

**Exact readiness verdict: READY FOR R6.10C IMPLEMENTATION.** R6.10C must implement the 22
engineering gaps under R6.10B/B1/B2/B3 as amended here, including the deterministic aggregate
encoded-request pre-egress bound. No further governance amendment is required unless implementation
discovers a direct higher-order contradiction rather than an ordinary code/refactoring task.
