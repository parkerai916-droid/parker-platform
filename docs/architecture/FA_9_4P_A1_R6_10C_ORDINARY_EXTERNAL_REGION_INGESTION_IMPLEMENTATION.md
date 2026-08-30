# FA.9.4P-A1E-R6.10C — Ordinary external region-ingestion implementation

## 1. Authority and safety boundary

Implementation began from clean `3431f086139091bd956fe542c1a3e2dbc96cc4b6`. Exact governing digests:

- R6.10B: `dfa199633f651acd9f81e927ecf04fcb1a4a89efea94aecd31025ec41eca15ad`
- R6.10B1: `fd1ca7867c5103fa9a9519b3687e9f04b87b79b158bb741fd904d0d6766198a6`
- R6.10B2: `b72aefabeeaba6dd80f8c2ca46bb8cf495e336b0626e5a398fa7a18291fdb1c4`
- R6.10B3: `4b9fedd1fcc9d3454f4511d1daa3c7c6540bab4928460de8964437b7bcdf3d3f`
- R6.10B4: `f74c9defd5b62940752ba36af4580e4c36b928fbac2d4cb28584ba608230b6b2`
- R6.10B5: `a5c4f990c2d7571cacfd557aad3ad6070fd3545b0c32337a0492a2e4616b63d1`

All implementation verification was offline with synthetic evidence, temporary stores and fake
transport. No production Compose file, production store, acceptance record, owner authorization,
lifecycle, container or provider was changed or invoked.

## 2. Files and principal classes

- `src/runtime/OrdinaryRegionIngestion.kt`: exact capability identity; acceptance record/store/
  evaluator; authorization record/store/guard; proposal/result API; PDF request preparer; Parker
  source-order reconstructor; deterministic generation key; idempotent admission; end-to-end owner
  workflow; root configuration.
- `src/interfaces/OrdinaryRegionDerivative.kt`: versioned, provenance-bearing region-transcription
  derivative model.
- `src/interfaces/TierADocumentIngestionRouter.kt` and `src/runtime/DerivativeContentCodec.kt`: new
  `RegionTranscription` payload kind and bounded representation version 1 while retaining every old
  decoder.
- `src/runtime/GovernedRegionTranscriptionExecution.kt`: preparation, guarded durable attempt-start,
  and post-guard transport/recovery phases; historical `execute` composes the same phases.
- `src/composition/ParkerRuntimeConfig.kt`: fail-closed opt-in and two new durable-root keys.
- `src/composition/ParkerRuntime.kt`: opt-in composition and explicit owner proposal, authorization,
  reservation, revocation and execution entry points.
- `src/runtime/ProductionAcquisitionCapabilityCatalogue.kt`: distinct PDF-only ordinary region-v5
  projection; historical default catalogue remains unchanged.
- `src/composition/OwnerUiEvidenceRuntimeAdapter.kt`, `src/runtime/DocumentAnalysisCoordinator.kt`,
  and `src/runtime/GovernedAcquisitionIntegration.kt`: exhaustive, explicit compatibility handling
  for the new sealed payload.
- `tests/runtime/OrdinaryRegionIngestionTest.kt`: focused B5, concurrency, recovery, codec and full
  offline end-to-end evidence.

## 3. E1–E22 closure

| Gap | Closure |
|---|---|
| E1 acceptance implementation | Implemented: canonical create-once record/store and direct durable exact-build evaluator. |
| E2 proposal projection | Implemented: non-executing, evidence-specific PDF proposal with OpenAI crop disclosure. |
| E3 authorization persistence | Implemented: checksummed base grant plus forced reservation/revocation event history. |
| E4 shared guard | Implemented: deterministic authorization-scoped JVM serialization plus Linux `FileChannel.lock()`. |
| E5 phased coordinator | Implemented: provider-free prepare, guarded durable start, post-release transport/recovery. |
| E6 typed failure metadata | Implemented in bounded owner result dispositions; ledger remains factual stage authority. |
| E7 request preparation | Implemented as reusable `OrdinaryRegionRequestPreparer`. |
| E8 ordinary executor | Implemented as `OrdinaryRegionIngestionWorkflow.execute`. |
| E9 reconstructor | Implemented deterministic graph validation/topological reconstruction. |
| E10 derivative type | Implemented distinct versioned payload with full governed provenance. |
| E11 typed admission | Implemented deterministic, conflict-aware region admission using existing stores. |
| E12 catalogue/config identity | Implemented exact capability digest, catalogue projection and validated config keys. |
| E13 owner API/UI/results | Implemented owner workflow/runtime proposal, grant, reservation, revocation, execution and typed result APIs; no internal identity entry by owners. |
| E14 runtime composition | Implemented opt-in `ParkerRuntime` construction from existing custody, ledger, provider-state, derivative and audit stores. |
| E15 runtime config | Implemented coupled fail-closed enablement and root/provider/build readiness validation. |
| E16 Compose mounts | Deferred to R6.10D as deployment-only; no production Compose mutation is authorized in C. |
| E17 codec migration | Implemented representation version 1 and unchanged decoding of all historical kinds/versions. |
| E18 admission restart tests | Implemented matching restart recovery and immutable conflict test. |
| E19 concurrency tests | Implemented real two-thread revocation versus attempt-start race. |
| E20 high-level E2E | Implemented synthetic PDF through fake provider, admission and complete store reload. |
| E21 provider-free preflight | Implemented proposal, dynamic acceptance and all pre-egress dispositions without transport. |
| E22 documentation | This implementation record. |

Only E16 is deferred, exactly because it is production deployment work assigned to R6.10D.

## 4. Capability acceptance and dynamic reload

`OrdinaryRegionCapabilityIdentity` binds OpenAI Responses `POST /v1/responses`, `gpt-5.6-sol`,
reasoning none, `store=false`, detail original, adapter 4.0.0, profile v2, wire v5, exact schema/
instruction/processing identities and PDFBox 3.0.7 at 300 DPI. The acceptance record binds its
canonical capability digest, R6.9 evidence-chain digests, exact lowercase 40-character promoting
commit, approver and time. Its SHA-256 record identity uses length-prefixed canonical framing.

The evaluator lists and validates the durable store on every call. It accepts exactly one record
whose capability digest and promoting commit equal the current embedded `Parker-Source-Commit`.
Missing, unreadable, corrupt, ambiguous, wrong-build or wrong-capability state is not accepted. No
positive cache exists. A test creates a record after an initial negative evaluation and observes it
on the next call without recreating the evaluator, then proves later corruption fails closed.

## 5. Owner authorization, reservation and linearization

The create-once grant binds authorization ID, evidence ID, source SHA-256, capability digest,
provider, purpose, crop scope, disclosure/approval facts and expiry. Reservation events add exactly
one execution ID. Exact replay is idempotent; a different execution or binding fails closed. Expiry
is enforced before reservation but not after valid reservation. Revocation is durable and reports
whether attempt-start had already won.

`OrdinaryRegionAuthorizationGuard` first takes a process-local lock (preventing JVM overlapping-lock
exceptions), then the deterministic per-authorization Linux file lock. Under that guard execution
reloads and verifies authorization/reservation/revocation/bindings, inspects the attempt ledger and
forces `PROVIDER_ATTEMPT_STARTED`. It releases the guard before transport. Lock order is therefore
authorization guard → attempt ledger → later provider state and is never reversed.

The real two-thread test permits only: revocation first with no start/call, or start first with
post-attempt revocation and one call. The forbidden mixed outcome is asserted absent.

## 6. Source preparation and request limits

Only custody-verified `application/pdf` enters ordinary preparation. PDFBox 3.0.7 renders at 300 DPI
under existing 64 MiB source, 200-page, dimension and decoded-pixel bounds. R6.2 derives regions.
Zero regions returns `NO_TRANSCRIBABLE_REGIONS`; 33 or more returns `REQUEST_BOUNDS_EXCEEDED`;
1–32 inclusive is eligible. `HUMAN_ORDER_REQUIRED` and `NOT_YET_SUPPORTED` return their distinct
review/unsupported states before attempt-start. No batching, truncation, omission or second request
exists.

The selected aggregate request-body bound is exactly **16,777,216 bytes (16 MiB)** of the final
UTF-8 JSON body produced by the region-v5 adapter. This is a conservative aggregate bound below the
existing 20 MiB raw-response ceiling and substantially below the worst-case sum permitted by the
32 MiB per-image type bound. It prevents excessive base64/JSON heap and transport payloads without
misrepresenting either existing limit as an aggregate request rule. The exact final body is encoded
and measured before attempt-start; boundary `16 MiB` passes and `16 MiB + 1` fails with zero calls.

## 7. Provider state, v5 and Parker ordering

The workflow reuses `FileSystemRegionProviderStateStore`: raw response is create-once before parse,
assessment is a separate create-once sidecar, and request/raw/structured/record digests remain
available for recovery and derivative provenance. Transport is one fake invocation in tests and has
no retry loop.

The existing v5 validator remains authoritative for exact region accounting, Unicode/whitespace,
point anchors, unanchored observations, uncertainty spans, mixed-null/reversed/out-of-bounds
rejection and provider provenance. Historical v4 remains unchanged.

`RegionSourceOrderReconstructor` requires `UNAMBIGUOUS`, validates graph membership and acyclicity,
uses deterministic topological order with geometry/identity tie-breaking, rejects missing,
duplicate and unknown returned regions, and reorders validated blocks by Parker order. Provider
ordinal is retained only as provenance.

## 8. Derivative identity and crash recovery

The new durable payload preserves evidence/source, page and pixel digests, regions/crops, exact
transcription and annotations, both orders, provider/model/adapter/profile/wire/schema/instruction/
processing identities, request/response/provider-state identities, capability acceptance,
authorization, execution, attempt, reconstructed digest, canonical generation digest and admission
provenance.

Generation-key SHA-256 uses domain `parker.region-transcription.generation.v1` and four-byte
length-prefixed UTF-8 framing over execution ID, evidence ID, source SHA-256, acceptance record ID,
provider-state ID and reconstructed-content digest. The storage ID is deterministic
`region-<digest>` and the original digest remains in payload provenance.

Admission checks content and generation stores before writing. Exact existing immutable state
recovers as admitted; any content/provenance/digest conflict is `ADMISSION_CONFLICT`. Missing
matching stages resume through the existing content-first, audit, generation prepare/publish
boundaries. A terminal-ledger gap is advanced locally after recovery. It never retries transport.

Tests cover the fourteen governed checkpoint identities: reservation; request preparation; guard
before start; immediately after start; guard release before transport; unknown transport; raw and
structured provider state; validation; reconstruction; content publication; generation
publication; audit publication; and admission before terminal ledger update. Existing ledger,
provider-state and coordinator regressions exercise the corresponding durable recovery states; the
new admission test proves restart identity and conflict behavior; the full E2E reload proves one
call and one derivative.

## 9. Owner-visible states and APIs

Pre-egress: `UNSUPPORTED_MEDIA`, `NO_TRANSCRIBABLE_REGIONS`, `REQUEST_BOUNDS_EXCEEDED`,
`SOURCE_ORDER_REVIEW_REQUIRED`, `SOURCE_ORDER_NOT_SUPPORTED`, `CAPABILITY_NOT_ACCEPTED`,
`OWNER_AUTHORIZATION_REQUIRED`, `OWNER_AUTHORIZATION_REVOKED`, and
`OWNER_AUTHORIZATION_EXPIRED_BEFORE_RESERVATION`. Post-attempt: `PROVIDER_OUTCOME_UNKNOWN`,
`PROVIDER_RESPONSE_AVAILABLE`, `VALIDATION_FAILED`, `ADMISSION_CONFLICT`, `ADMITTED`, and
`REVIEW_REQUIRED`.

`ParkerRuntime` exposes separate proposal, grant, reservation, revocation and execution entry
points. Proposal/grant/reservation do not execute. Execution is possible only when opt-in runtime
configuration, provider readiness, fresh exact acceptance, custody, exact authorization, source
bounds/order, guarded attempt-start and all downstream validation succeed.

## 10. Configuration, security and compatibility

New opt-in keys are `PARKER_ORDINARY_REGION_INGESTION_ENABLED`,
`PARKER_ORDINARY_REGION_CAPABILITY_ACCEPTANCE_STORAGE_ROOT`, and
`PARKER_ORDINARY_REGION_OWNER_AUTHORIZATION_STORAGE_ROOT`. Enablement requires both roots,
external-provider readiness, deployment-local credential, exact source commit, and existing attempt
and provider-state roots. Disabled remains the historical default.

No credential or header is stored, logged, hashed or placed in request/derivative state. Provider
transport tests use injected fakes; `store=false` remains exact. No `.env` or production container
environment was inspected.

Historical adapter 2.0.0, region adapter 3.0.0/profile v1/wire v4, acceptance authorities, ledgers,
provider state, R6.9 malformed-schema assessment and old derivative payload representations decode
and behave unchanged. Exhaustive consumers explicitly keep the new payload out of historical Tier A
analysis/presentation paths.

## 11. Verification and completeness trace

Focused coverage includes acquisition/owner presentation and HTTP regressions, config, ledger,
provider-state, renderer/deriver, adapter/v5 contract, phased execution, derivative codec/storage/
admission, R6.2 geometry, R6.9A forensic replay and R6.9B point-anchor behavior. The new suite has
9 tests, including the complete synthetic-PDF E2E and real race.

The full offline Ubuntu suite passes 3,237 tests across 235 suites with 0 failures and 0 errors; 9
existing conditional tests are skipped. `git diff --check` passes.

One existing provider-state corruption fixture was made deterministic: it formerly replaced the
first checksum nibble with `0`, which did not corrupt a timestamp-dependent checksum already
beginning with `0`. It now flips that nibble. Product behavior was unchanged.

Actual trace:

owner request → `OrdinaryRegionProposal` → `OrdinaryRegionOwnerAuthorization` → durable reservation
→ fresh acceptance evaluation → `OrdinaryRegionIngestionWorkflow` → custody resolver → PDFBox
renderer → R6.2 deriver → bounds/order checks → `OrdinaryRegionRequestPreparer` → authorization
guard → durable attempt-start → guard release → adapter/fake transport → provider-state store → v5
validator → `RegionSourceOrderReconstructor` → canonical generation key → typed derivative codec →
idempotent admission → `OrdinaryRegionOwnerResult`.

Unresolved implementation arrows: **0**.

## 12. R6.10D deployment and promotion requirements

R6.10D must, without provider traffic:

1. build the exact R6.10C commit image and verify embedded `Parker-Source-Commit`;
2. create owner-only host roots and bind them to
   `/data/region-transcription-capability-acceptances` and
   `/data/external-region-owner-authorizations`;
3. preserve/reuse existing attempt, provider-state, derivative-content, derivative-generation and
   ingestion-audit mounts;
4. configure the three new runtime keys, exact source/image/build identity and existing provider
   readiness/credential keys without disclosing secrets;
5. deploy the exact image with no region-v5 acceptance record and prove normal startup plus dynamic
   `CAPABILITY_NOT_ACCEPTED` through a provider-free proposal/evaluation;
6. create exactly one governed production acceptance record bound to that running commit,
   capability and R6.9 evidence chain;
7. prove the next provider-free evaluation observes accepted status without restart;
8. create no owner authorization, make no provider call and perform no ordinary execution during
   promotion;
9. verify rollback/different-build exact equality fails closed and production persistence survives
   restart without mutating historical R6.9 artifacts.

R6.10C readiness for that unit: **READY FOR R6.10D DEPLOYMENT/PROMOTION GOVERNANCE**.
