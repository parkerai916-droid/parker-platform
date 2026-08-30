# FA.9.4P-A1E-R6.10B4 — Collect-all implementation-closure audit

## 1. Authority and method

This diagnostic audit inspected the complete A–AD surface against repository HEAD
`a5ea871f1b0177d72d1700c1adc7cd488340ba43`. Governing document digests were:

- R6.10B: `dfa199633f651acd9f81e927ecf04fcb1a4a89efea94aecd31025ec41eca15ad`
- R6.10B1: `fd1ca7867c5103fa9a9519b3687e9f04b87b79b158bb741fd904d0d6766198a6`
- R6.10B2: `b72aefabeeaba6dd80f8c2ca46bb8cf495e336b0626e5a398fa7a18291fdb1c4`
- R6.10B3: `4b9fedd1fcc9d3454f4511d1daa3c7c6540bab4928460de8964437b7bcdf3d3f`

The method traced every desired transition to actual classes, persistence behavior and recovery
entry points; inspected owner HTTP/UI, renderer/geometry, transport, provider state, derivative
codec/admission, configuration and Compose mounts; and separated trust decisions from routine
engineering. Discovery of a defect did not terminate inspection.

Classification abbreviations: `DI` defined and implementable; `IC` defined but interface change
required; `UG` undefined governance decision; `MI` missing implementation surface; `CX`
contradictory; `NA` not applicable.

## 2. A–AD findings

### A. Capability acceptance — IC

The exact capability and R6.9 evidence chain are defined, as are create-once conflict semantics and
exact build equality. New canonical record/store/evaluator/reload code is required. Record identity,
digest and `ACCEPTED` evaluation are implementable. Future builds do not inherit acceptance.

### B. Build provenance — DI

`PARKER_BUILD_COMMIT` → Gradle manifest `Parker-Source-Commit` → `ParkerRuntime.buildIdentity()` →
configured production-commit equality is complete. Missing, malformed, multiple or unequal embedded
values fail closed. No ancestry service is needed.

### C. Owner proposal — IC

Current GET acquisition evaluation is non-executing, but its router receives
`NOT_AUTHORISED` and returns disabled/no-selection rather than a distinct external proposal. Extend
the evaluation/view/UI; current POST acquire is the only execution route.

### D. Owner egress authorization — MI

The grant/reservation/revocation contracts are defined, but record/store/API/audit views do not
exist. Fields and single-use semantics are implementable. Exact audit event representation is an
engineering choice using the create-once store and owner result surface.

### E. Authorization locking — IC

Per-ID Linux `FileChannel.lock()` is implementable using the attempt-ledger convention. Add a
dedicated root, hashed safe lock path, same-ID serialization and per-ID concurrency. Lock files need
not be durable evidence; grant/reservation/revocation records must be fsynced.

### F. Lock ordering — IC

B2 defines authorization → attempt ledger → later provider state. Current region coordinator owns
attempt-start internally, so it must expose prepare/guarded-start/transport phases. No current code
holds an attempt lock while requesting an authorization lock because authorization code is absent.

### G. Reservation — MI

AVAILABLE → RESERVED and same-execution idempotence are governed by B1/B2. Implement canonical
binding and exact conflict checks. Expiry is checked initially; later expiry does not strand the
same reservation. Revocation uses the same guard.

### H. Revocation/attempt-start — IC

The B2 winner protocol is implementable. Refactor `GovernedRegionTranscriptionExecutionCoordinator`
around its lines advancing `REQUEST_PREPARED`, `PROVIDER_ATTEMPT_STARTED`, and invoking
`mechanism.transcribe`. Acceptance compatibility wrapper must retain existing behavior.

### I. Attempt ledger — IC

Stages cover authorized, preflight, source, request, start, response, admitted and terminal states.
Timeout, provider error, parse, validation and admission failure have no typed durable reason; only
generic terminal failure metadata is possible. Extending bounded reason metadata is engineering,
but required for owner recovery visibility. No stage may make a started attempt retryable.

### J. Crash recovery — UG/IC

B1/B2 resolve reservation through raw-response recovery. Existing provider-state-first lookup and
idempotent pre-attempt advances support it. Four later transitions remain insufficiently governed:
zero derived regions, ambiguous order, crash during derivative admission, and crash after admission
publication before terminal ledger update. The latter two require deterministic/idempotent
derivative identity rules (G5 below), not merely exception handling.

### K. Provider transport — DI

`OpenAiRegionTranscriptionAdapter` binds `/v1/responses`, model, reasoning none, `store=false`,
original detail and adapter 4.0.0. `JdkOpenAiResponsesTransport` performs one JDK HTTP send and has
no application retry loop or SDK retry policy. Timeout/exception returns failure after the durable
start boundary. No hidden automatic retry was found.

### L. Provider state — DI/IC

`FileSystemRegionProviderStateStore` persists/fyncs raw response before parse, then a separate
immutable assessment with exact structured state. It binds request digest, response bytes, provider
order and record ID, enforces bounds and owner permissions, and recovers by request. The derivative
can reference `recordId`/digests. Ordinary root wiring is required; semantics are sufficient.

### M. Source custody — DI

Ordinary execution already re-resolves `AuthoritativeAcquisitionInput` through
`AuthoritativeAcquisitionSourceResolver` and checks evidence ID/SHA/length/media. The new executor
must accept only that resolved input; no bypass is needed.

### N. Page representation — DI

`DeterministicSourcePageRenderer` is the single reusable renderer. PDF uses PDFBox 3.0.7 at the
locked 300 DPI profile, with canonical pixels, page representation IDs and digests. PNG support
exists with DPI null, creating the media-scope governance issue G1; JPEG/WebP are not rendered.

### O. Region geometry — DI/UG

`DeterministicSourceRegionDeriver` deterministically creates IDs, crop digests, classes, graph and
ambiguity. It can recreate regions from custody bytes. It may produce zero regions, and its actual
implementation produces `UNAMBIGUOUS` or `HUMAN_ORDER_REQUIRED` but never
`NOT_YET_SUPPORTED`. Ordinary disposition for zero regions is undefined (G3).

### P. Request construction — UG/IC

Acceptance reconstruction already builds region images and a v5 request with `REGION_ONLY` crops.
It limits total regions to `MAX_REGIONS_PER_REQUEST`. The accepted ordinary media surface and
maximum page/region/encoded-request policy are not locked. No governed multi-call batching model
exists; splitting would conflict with one authorization/attempt semantics (G1/G2).

### Q. Wire v5/parsing — DI

Adapter 4.0.0, profile v2 and v5 validation implement point anchors, mixed-null/bounds rejection,
non-empty uncertainty spans, exact text and region accounting. v4 constants and validation remain
separately supported.

### R. Source-order reconstruction — UG/MI

Provider order is retained but must not order output. Graph regions are currently geometry-sorted;
there is no production `RegionSourceOrderReconstructor`. Exact treatment of
`HUMAN_ORDER_REQUIRED`/`NOT_YET_SUPPORTED` is not selected between no provider call, durable
non-usable result, or human order workflow. This is governance blocker G4. Unambiguous topological
ordering can then be implemented mechanically.

### S. Derivative model — IC

No current type represents region transcript provenance. Add the governed model and a new versioned
`TierADerivativePayload` variant/codec. All required fields can be represented, but codec bounds,
canonical content identity and deterministic generation identity must be explicit. Existing OCR
payload is insufficient and must not be overloaded.

### T. Admission — UG/IC

`DerivativeGenerationCoordinator` provides the correct content-first, generation prepare/audit/
publish boundary. Add typed region admission; no parallel store is needed. Current `idFactory`
defaults to random UUID. A crash after content publication or generation publication can mint a
second derivative on local restart. G5 requires a governed deterministic/idempotent admission key
and exact duplicate/conflict outcome.

### U. Owner result/review — UG/IC

Current owner views expose admitted ID, fidelity, completeness and human-review state. Exact states
for zero regions, ambiguous order, raw-only, validation failure, unknown provider outcome and
admission-reconciliation are not governed as a closed set (G3/G4/G5). API/JSON/UI wiring is
otherwise engineering.

### V. Runtime composition — IC

Add acceptance and authorization stores/evaluators, guard, phased coordinator, renderer/deriver,
ordinary executor, reconstructor, typed admission and owner operations. Catalogue must register a
distinct region-v5 capability/executor and retain the historical entry. No silent adapter selection
is necessary.

### W. Configuration — IC

Add feature enablement plus acceptance and authorization roots. Capability identity remains code/
record governed, not user-entered. Attempt/provider-state and derivative roots are reused. Credential
remains secret deployment-local. Exact container/host root constants are engineering but must be
fixed before R6.10D.

### X. Persistent roots — IC

Two new roots are required: capability-acceptance records and ordinary owner authorizations
(including per-ID locks/reservations/revocations). Recommended conventions are
`/data/region-transcription-capability-acceptances` and
`/data/external-region-owner-authorizations`, with host siblings under
`/mnt/parker-data/parker/`. R6.10D must create permissions and bind mounts. Existing attempt,
provider-state, derivative-content/generation and audit roots are reused.

### Y. Security — DI/IC

Credential/header persistence is absent; records need only non-secret identities. `store=false` is
hard bound. Owner disclosure must name OpenAI and crop-only scope. Fake transport injection is
available. No security blocker was found, subject to bounded safe error messages in new APIs.

### Z. Historical compatibility — IC

Use new record/file/codec versions and capability ID. Preserve adapter 2.0.0, adapter 3.0.0, v4,
acceptance coordinators, old ledgers/provider state and R6.9 assessment. New sealed payload/codec
must decode all existing representation versions unchanged. This is engineering with defined rules.

### AA. Testability — DI

All boundaries can be tested with temporary stores, synthetic PDF and fake transport. No real
provider or production mutation is required. Concurrency schedules can use latches around the guard
and ledger transition.

### AB. Deployment/R6.10D — UG/IC

B3 fixes exact build and safe high-level ordering. R6.10D needs two mounts and provider-free
evaluation. Whether acceptance records are dynamically reloaded or snapshot at startup is not
locked; either changes when `ACCEPTED` becomes observable and restart behavior (G6). Choose one
before implementation. Rollback to a different build fails exact acceptance as intended.

### AC. Complete state machine — four unresolved transitions

Defined: proposal → grant → reservation → preflight → source → render → regions → request → guarded
start winner → transport → raw state → assessment → validation → reconstruction → admission → owner
result. B1/B2 define expiry, revocation, crash and unknown-outcome safety. Undefined transitions are:

1. renderer/deriver returns no regions;
2. graph is `HUMAN_ORDER_REQUIRED` or `NOT_YET_SUPPORTED`;
3. crash/conflict during derivative content/generation/audit publication;
4. acceptance record appears or changes while runtime is already running.

### AD. Complete implementation trace

| Arrow | Current seam | Required addition/refactor | Status |
|---|---|---|---|
| owner request → proposal | HTTP acquisition GET, owner workflow | external proposal/disclosure view | IC |
| proposal → grant/reservation | none | authorization records/store/API/guard | MI |
| evaluation → accepted capability | catalogue/router | region capability + acceptance evaluator | IC |
| selection → executor | execution coordinator | region executor binding/registration | IC |
| executor → custody | source resolver | reuse exact input | DI |
| custody → pages/regions | renderer/deriver | reuse/extract preparation helper | DI |
| regions → request | acceptance reconstructor | ordinary request builder and governed bounds | UG/IC |
| prepared → guarded start | region coordinator/ledger | phased coordinator callback | IC |
| transport → provider state | adapter/store | reuse | DI |
| state → v5 result | parser/validator | reuse | DI |
| result → Parker order | graph only | reconstructor + ambiguity decision | UG/MI |
| reconstruction → derivative | no type | model + codec | IC |
| derivative → admission | derivative coordinator | typed, idempotent region admission | UG/IC |
| admission → owner result | owner views/HTTP/UI | closed review states | UG/IC |

## 3. Collect-all defect register

| ID | Surface | Class | Location | Problem | Kind | Blocks C? | Smallest correction | Consolidate |
|---|---|---|---|---|---|---|---|---|
| G1 | media scope | UG | B §10; catalogue; renderer | PDF-only accepted proof conflicts with existing image media projections; JPEG/WebP cannot use renderer | governance | yes | lock exact supported media, initially `application/pdf` | with G2 |
| G2 | request bounds | UG | request contract/reconstructor | no exact pages/regions/encoded-bytes limit or multi-attempt batching authority | governance | yes | lock one-call bounds and reject overflow; no batching | G1 |
| G3 | empty regions | UG | deriver/request/owner result | zero-region outcome has no terminal/review disposition | governance | yes | lock no-egress fail-closed owner-visible disposition | G4 |
| G4 | ambiguous order | UG | graph/B §8 | no exact pre/post-provider behavior for HUMAN_ORDER_REQUIRED/NOT_YET_SUPPORTED | governance | yes | lock pre-egress rejection or explicit review-only flow | G3 |
| G5 | admission idempotence | UG | `DerivativeGenerationCoordinator.idFactory` | random ID permits duplicate derivative after crash/restart | governance | yes | lock deterministic execution-bound generation ID and duplicate/conflict rules | owner states |
| G6 | acceptance reload | UG | B §11; B3 §8 | startup-required record versus post-deploy creation leaves reload/restart observability open | governance | yes | lock dynamic per-evaluation reload or startup snapshot + controlled restart | deployment |
| E1 | acceptance implementation | MI | new | record/store/evaluator absent | engineering | yes | implement canonical create-once components | — |
| E2 | proposal projection | IC | owner workflow/router | NOT_AUTHORISED yields no proposal | engineering | yes | add non-executing proposal evaluation | E13 |
| E3 | authorization persistence | MI | new | grant/reservation/revocation store absent | engineering | yes | implement B1/B2 store | E4 |
| E4 | shared guard | MI | new | authorization lock absent | engineering | yes | per-ID filesystem lock | E3 |
| E5 | phased coordinator | IC | governed region coordinator | attempt-start internal | engineering | yes | prepare/guarded-start/transport seam | E4 |
| E6 | typed failure metadata | IC | attempt ledger | failures not durably differentiated | engineering | no | bounded terminal reason metadata | E13 |
| E7 | ordinary request preparation | IC | acceptance reconstructor | logic tied to acceptance manifest/deployment | engineering | yes | extract provider-neutral source preparation | E8 |
| E8 | ordinary executor | MI | runtime | no bound executor | engineering | yes | implement coordinator/executor | E7 |
| E9 | reconstructor | MI | new | no production source-order component | engineering after G4 | yes | implement graph-checked ordering | G4 |
| E10 | derivative type | MI | payload/codec | provenance has no representation | engineering | yes | new versioned payload/model/codec | E11 |
| E11 | typed admission | IC | derivative coordinator | OCR validation shape is incompatible | engineering after G5 | yes | typed region admission | E10/G5 |
| E12 | catalogue/config identity | IC | acquisition types/catalogue | wire/renderer/geometry/acceptance digest not represented | engineering | yes | region-specific configuration value | E1 |
| E13 | owner API/UI/results | IC | HTTP/runtime/views/UI | proposal/auth/recovery states absent | engineering after G3–G6 | yes | add bounded endpoints/views | E2/E6 |
| E14 | runtime composition | IC | `ParkerRuntime` | stores/executor not wired | engineering | yes | compose dependencies fail closed | all |
| E15 | runtime config | IC | `ParkerRuntimeConfig` | roots/enablement absent | engineering | yes | add coupled validated keys | E14 |
| E16 | Compose mounts | MI | Compose/config | two persistent mounts absent | engineering (R6.10D) | no for C | add verified binds in D | E15 |
| E17 | codec migration | IC | derivative codec | no region format/version | engineering | yes | add version while preserving old decoders | E10 |
| E18 | admission restart tests | MI | tests | no region admission recovery fixture | engineering | yes | crash-point tests | G5 |
| E19 | concurrency tests | MI | tests | no authorization race fixture | engineering | yes | latch/fork tests | E4/E5 |
| E20 | high-level E2E | MI | tests | no ordinary region fixture | engineering | yes | synthetic PDF/fake transport/full stores | all |
| E21 | provider-free preflight | MI | owner/runtime | no accepted+authorized dry selection view | engineering | yes | expose evaluation without transport | E13 |
| E22 | documentation | MI | docs | implementation evidence absent | engineering | yes | R6.10C document | — |

## 4. Totals and closure result

- **TOTAL IMPLEMENTATION-BLOCKING GOVERNANCE DEFECTS: 6**
- **TOTAL ENGINEERING GAPS WITH ALREADY-DEFINED SEMANTICS: 22**
- **TOTAL CONTRADICTIONS: 0**
- **TOTAL UNRESOLVED STATE TRANSITIONS: 4**
- **TOTAL UNRESOLVED IMPLEMENTATION ARROWS: 3** (bounded request, source-order disposition,
  idempotent admission)

No hidden automatic retry or security blocker was found. Two new persistent mount roots are needed,
but their implementation is a deployment engineering task once G6 is settled. R6.10D is blocked by
G6 and by the five ordinary-execution decisions G1–G5.

**Overall classification: CONSOLIDATED_GOVERNANCE_CORRECTION_REQUIRED.**

One consolidated amendment should lock G1–G6 together. After that, all remaining gaps are bounded
engineering and R6.10C can proceed without another architecture-discovery loop.
