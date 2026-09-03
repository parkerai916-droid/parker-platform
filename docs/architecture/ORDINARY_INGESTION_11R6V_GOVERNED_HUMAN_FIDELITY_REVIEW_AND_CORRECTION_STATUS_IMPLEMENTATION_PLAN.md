# Ordinary Ingestion 11R6V — Governed Human Fidelity Review and Correction Status Implementation Plan

## 1. Status

**ACCEPTED — CANONICAL — FROZEN**

Owner: Steven Francis McTague
Accepted: 3 September 2026

This document is an implementation plan only. It creates no production contract, authority, policy, record, corrected representation, or deployment approval. Owner acceptance of this plan cannot itself authorize implementation or governed-data mutation.

## 2. Purpose

This plan maps the owner-accepted and frozen OI11R6T architecture onto the current Parker codebase. It defines the smallest safe path to make the completed R6S human review canonical, expose its material discrepancies, and prevent the uncorrected provider text from being treated as source-confirmed. It separately plans the optional follow-on capability for a human-corrected representation.

Every planned operation is local and zero-egress. No unit requires or may authorize retranscription, provider retry, external OCR, external verification, or external reasoning.

## 3. Authoritative Scope Lock

The sole architectural authority is:

`docs/architecture/ORDINARY_INGESTION_11R6T_GOVERNED_HUMAN_FIDELITY_REVIEW_AND_CORRECTION_STATUS_SCOPE_LOCK.md`

Its verified SHA-256 at the R6V starting state is:

`f90c9fc654136ea5e92723a1704ad58108ab0f8a9e73a401e8039d3210e4cd2a`

Its status is `ACCEPTED — CANONICAL — FROZEN`. This plan implements rather than reinterprets its decisions, including the R6T-A distinctions among observed discrepancy, severity, cause, pattern, source resolution, and overall descriptive fidelity.

## 4. Current Repository Baseline

R6V began on `main` with `HEAD` and upstream both at:

`d2b29f603a45beb44a7758369e389d3fcb1ac253`

The worktree was clean. Current production was verified read-only as:

- container: `7d51a0c2b3c499cee97818c04c8599351cc03c6b515e5ff0358eaa95dfef62fc`;
- image/index: `sha256:55b4f29b4e8f30b80528fda075c7936a968ae98a0b6b54e55b536e9fb9d9ac9c`;
- source: `d45efbee348b842340616a6a73831ef130086d90`;
- runtime JAR SHA-256: `71f154b230a5ce318915f7fdc66b24ad11393c0112e5f76a1a9c289255c3815a`;
- restart count: `0`;
- readiness: `PASS`.

No implementation or test file is changed by R6V.

## 5. Existing Architecture Mapping

### Evidence, derivative, and content identity

`EvidenceArtifactId`, `DerivativeGenerationId`, derivative parent references, and derivative content identity already provide the canonical immutable identifiers. `DerivativeGenerationStore` and `DerivativeContentStorage` implement prepare/publish, create-once persistence, canonical codec validation, restart durability, duplicate rejection, and corrupt/unsupported-record failure. Ordinary-region transcription already retains page, preparation-region, derivative-region, block, Parker-order, provider-state, authorization, execution, attempt, and capability provenance. These contracts are reused; review state is never added as a mutable field on the provider generation.

### Human verification

`HumanVerificationRecord` already binds an exact evidence artifact and generation to page and half-open character scopes, a `PrincipalId`, review time, outcome, and review-artifact digest. `HumanVerificationStorage`, `HumanVerificationRecordCodec`, and `FileSystemHumanVerificationStorage` supply strong write-once and versioned-storage precedents. `HumanVerificationOutcome` and `AnalysisHumanReviewState` also establish conservative `UNREVIEWED` semantics.

The current record is insufficient for the frozen capability: it has no structured discrepancies, content digest binding, source-location identity, severity/cause/source-resolution separation, pattern identity, coverage completeness, supersession, or conflict semantics. Its version-1 codec meaning must not be changed. Existing records remain readable and contribute only their historical verification facts.

### Existing derivative review

`DerivativeReviewRegistry` and `DerivativeReviewState` (`PENDING_REVIEW`, `APPROVED`, `REJECTED`, `NEEDS_CORRECTION`) are evidence-level and do not bind exact generation/content or structured discrepancy facts. The in-memory registry is non-durable and has a one-way terminal transition graph. It must not be stretched into the new model and must not become an effective-review authority. It remains a historical workflow signal until separately retired by an authorized programme.

### Identity and permissions

`PrincipalId` is the existing reviewer identity primitive and is reused. Owner-only routes already resolve the configured owner principal after authentication. `AuthorizationPurposeId`, the purpose registry, permission evaluation, and request authorization mechanisms provide the canonical authority pattern. Two narrow purposes are required; neither is registered in R6V.

### Supersession

`Relationship.SUPERSEDES` demonstrates immutable graph semantics, but its Memory-domain relationship graph is not authoritative for review/correction records and cannot safely supply domain-specific validation. The new records use explicit typed predecessor relationships and a domain projector that validates acyclicity, exact bindings, and conflicts. This reuses the semantic precedent, not the Memory store.

### Audit

`DocumentIngestionAudit` and its filesystem implementation demonstrate narrow, append-only, fact-based audit records and durable sequencing. Its two ingestion-admission stages do not describe review/correction events and must not be broadened. The general `AuditService` is only a contract and must not be incidentally implemented. A narrow human-fidelity governance audit port/store is required.

### Retrieval and downstream use

`DocumentAnalysisCoordinator` already selects exact generations, projects absence as `UNREVIEWED`, and requires explicit acknowledgement for unverified external transcription in applicable flows. Canonical owner/Tier A presentation already exposes immutable derivative content and human-review assurance, including ordinary-region transcription. These are integration points, not authority to equate acknowledgement with source confirmation.

Source-confirmed eligibility needs a new purpose-sensitive projection. It must be consulted by any path that promotes or consumes transcription as source-confirmed evidence, including analysis and any later Memory/Knowledge promotion boundary. Raw provider retrieval remains available and explicitly labelled.

### Storage conventions

The filesystem stores establish the conventions to retain: safe opaque IDs, bounded canonical binary encoding, explicit schema version, digest verification, `.prepared` staging, atomic create-new publication, deterministic duplicate handling, restart readback, trailing-byte rejection, and fail-closed corrupt/unknown-version handling.

## 6. Gap Matrix

| Frozen Requirement | Existing Parker Mechanism | Reuse As-Is | Extend Narrowly | New Mechanism Required | Reason | Implementation Unit |
|---|---|---:|---:|---:|---|---|
| Four provenance-bearing layers | Evidence, generation/content, human verification | Yes | Yes | Yes | Review and corrected layers need their own immutable contracts | A1, B1 |
| Provider transcription immutable | Generation/content create-once stores | Yes | No | No | Review never mutates the parent | A1–A8 |
| Execution success independent of fidelity | Execution outcome plus analysis review state | Yes | Yes | Yes | A combined status must be impossible in the new projection | A1, A4 |
| Actor-neutral discrepancy semantics | Provider-independent derivative/evidence IDs | Yes | No | Yes | Structured discrepancy must not encode producer type | A1 |
| Materiality independent of quality | None sufficient | No | No | Yes | Separate severity and descriptive-quality fields/projection | A1 |
| Established/hypothesised/unknown cause | None | No | No | Yes | Cause assessment is optional and independent | A1 |
| Source resolution independent of cause | Character scopes are precedent | Yes | Yes | Yes | Resolution needs explicit state and asserted source value | A1 |
| Exact location/span binding | Page and character scopes; ordinary-region identities | Yes | Yes | Yes | Must add source/content/preparation/block/original-substring binding | A1 |
| Optional systematic pattern | No authoritative mechanism | No | No | Yes | Immutable association without replacement authority | A1 |
| Reviewer attribution | `PrincipalId`, owner principal resolution | Yes | Yes | No | Existing identity system is adequate | A1, A3 |
| Immutable additive reviews | Human verification prepare/publish | Yes | Yes | Yes | New semantic record and store required; v1 remains unchanged | A2 |
| Separate corrected representation | Derivative parent reference and stores | Yes | Yes | Yes | New corrected payload/kind and correction governance are follow-on | B1–B4 |
| Exact-span correction | Existing half-open character scope | Yes | Yes | Yes | Must validate original substring and location against parent | B1, B2 |
| Proposal and acceptance separate | Authorization/publish patterns | Yes | Yes | Yes | Distinct actors/authority and records | B1–B3 |
| Explicit non-temporal supersession | `Relationship.SUPERSEDES` precedent | No | Yes | Yes | Dedicated typed DAG; timestamps never select winner | A1, A4, B1 |
| Conflict/adjudication | No sufficient review mechanism | No | No | Yes | Unresolved material conflict blocks effective state | A4, B1–B3 |
| Purpose-specific eligibility | Analysis acknowledgement precedent | Yes | Yes | Yes | Source-confirmed is a distinct fail-closed decision | A4 |
| Retrieval exposes all layers | Owner/Tier A presentation | Yes | Yes | Yes | Add review/discrepancy/effective-state views without substitution | A4 |
| Historical derivatives default unreviewed | `AnalysisHumanReviewState.UNREVIEWED` | Yes | Yes | No | Absence remains conservative | A4 |
| Unknown/malformed fail closed | Versioned codecs and corrupt-record errors | Yes | Yes | No | Apply established handling to new stores/projector | A1, A2, A4 |
| Narrow append-only audit | `DocumentIngestionAudit` precedent | No | Yes | Yes | Different facts and stages require a separate port/store | A2, A4 |
| Two Authorization Purposes | Purpose registry/policy | Yes | Yes | No | Add only the frozen narrow vocabulary/policies | A3, B3 |
| External reasoning boundary | Existing authorization separation | Yes | Yes | No | Eligibility response must retain assurance/provenance | A4, B2 |
| No retranscription/retry | Persisted derivative and provider budget | Yes | No | No | All fixture verification is local/read-only | Every unit |
| R6 closure before correction materialisation | Existing fixture and canonical readback | Yes | Yes | Yes | Canonical review recording and eligibility enforcement are missing | A1–A10 |

No proposed mechanism lacks a frozen requirement, and every frozen decision has an implementation destination.

## 7. Minimum R6 Closure Capability

Sequence A implements only what R6 closure requires:

1. an immutable exact-generation human-fidelity review record containing complete coverage and descriptive fidelity independently from its formal verdict;
2. structured, location-bound discrepancy occurrences with classification, severity, cause assessment, and human source-resolution state;
3. an optional immutable systematic-pattern association that never carries replacement authority;
4. exact evidence/source/preparation/generation/content/reviewer/review-artifact binding;
5. create-once durable storage and narrow append-only audit;
6. explicit supersession/conflict projection with no timestamp precedence;
7. canonical retrieval that shows provider text, formal review, discrepancies, patterns, and eligibility as distinct facts;
8. fail-closed purpose-specific source-confirmed eligibility;
9. the review-recording Authorization Purpose and owner policy;
10. production composition, deployment, and read-only verification;
11. a separately owner-authorized canonical import of the completed R6S record; and
12. verification that the provider derivative and historical provider budget remain unchanged.

Correction proposal, acceptance, corrected content, and corrected-generation admission are not required for R6 closure. The R6 fixture will close in a truthful state: provider execution `SUCCESS`, provider transcription retrievable, review `HUMAN_REVIEWED_WITH_DISCREPANCY`, and source-confirmed eligibility blocked.

## 8. Correction Follow-On Boundary

Sequence B is a separate, immediately available follow-on. It adds exact correction proposals, independent owner acceptance/adjudication, deterministic corrected representation construction, parent/provider preservation, unchanged-block equality, correction provenance, correction supersession, corrected retrieval, and source-confirmed eligibility after acceptance.

Sequence B must not mutate the provider transcription or reuse the review-recording authority to accept corrections. The two `Kellee` occurrences remain separately bound and corrected; their shared pattern is descriptive only. No whole-document retranscription or provider retry is inherent.

## 9. Authorization Purpose Plan

### `document-ingestion.human-fidelity-review-recording`

Semantic meaning: authorize an attributable human to record a completed exact-source/exact-generation fidelity assessment, structured discrepancies, pattern associations, source resolutions, and review supersession facts.

It authorizes validation and create-once publication of those facts and the corresponding narrow audit event. It does not authorize derivative mutation, correction acceptance, corrected representation creation, source-confirmed promotion, provider access, execution, or external reasoning.

The owner principal is the initial policy subject. Permission evaluation must occur before prepare, with the exact purpose and target bindings included in the request. This purpose is required before R6 closure.

### `document-ingestion.human-transcription-correction`

Semantic meaning: authorize exact correction proposal acceptance, conflict adjudication, corrected-representation admission, and explicit correction supersession for a bound source/review/provider generation.

It does not authorize editing the provider derivative, fuzzy/global replacement, provider access, retranscription, unrelated evidence operations, or external reasoning. Proposal authorship alone does not imply acceptance authority. Owner acceptance/adjudication is the minimum initial policy. This purpose is required only for Sequence B unless a material review conflict must be adjudicated before Sequence A can close.

Both values follow the existing namespaced purpose convention. Registration is additive, conflicts fail closed, retirement cannot be bypassed, and no Gap #54 work is included.

## 10. Domain Contract Plan

### Sequence A contracts

- **Existing identifiers:** `EvidenceArtifactId`, `DerivativeGenerationId`, derivative content identity/digest, preparation/region/block identifiers, and `PrincipalId` are reused.
- **`HumanFidelityReviewId` (new):** content-derived or canonical-record-derived immutable identity over the complete review payload, excluding storage path and publication time.
- **`HumanFidelityReviewRecord` (new):** one completed review act binding source, preparation, generation, content, reviewer, review instant, worksheet/owner-record digests, complete/partial coverage, formal verdict, and optional bounded descriptive fidelity. It owns the discrepancy and pattern facts recorded by that same act so publication is atomic; it does not own authorization or correction acceptance.
- **Review state (new projection vocabulary):** exactly `UNREVIEWED`, `HUMAN_REVIEWED_PASS`, `HUMAN_REVIEWED_WITH_DISCREPANCY`, and `HUMAN_REVIEW_CONFLICT`. Coverage is separate.
- **`FidelityDiscrepancyId` and occurrence (new):** immutable identity and actor-neutral fact with exact source/derivative location, original provider value/hash, classification, severity, reason, source resolution, and cause assessment.
- **Classification/severity/cause (new):** exhaustive frozen values; unknown values fail closed. Severity does not determine descriptive quality. Cause is `ESTABLISHED`, `HYPOTHESISED`, or `UNKNOWN` with bounded supporting text/evidence only when applicable.
- **Source resolution (new):** unresolved or human-resolved-against-source, with exact asserted source value/hash and reviewer attribution. It never claims technical-cause resolution.
- **`SystematicDiscrepancyPatternId` and association (new):** immutable association over explicit occurrence IDs. It is descriptive, cannot contain a replacement instruction, and cannot relax location validation.
- **Location (new composite using existing identities):** evidence ID/source SHA, page ordinal, preparation region, derivative region/block, and half-open Unicode code-point span in exact unnormalised provider block, plus original substring SHA and value. Geometry is optional only where already canonical; page/block/span bindings are mandatory for the R6 text substitutions.
- **Review supersession/adjudication fact (new):** optional single immediate predecessor and explicit owner adjudication where conflicts exist. The projector validates a DAG and exact target bindings.
- **Effective review/eligibility projection (new derived service):** read-only result assembled from immutable records. It is never persisted as a mutable status flag.

All values are bounded, canonical, and reject missing identities, invalid digests, empty required text, invalid spans, unknown values, and mismatched source/generation/content.

### Sequence B contracts

- **`CorrectionProposalId` and record (new):** exact occurrence/span target, original value/hash, proposed source value/hash, proposing principal, reason, parent review, and source/generation/content bindings.
- **`CorrectionAcceptanceId` and record (new):** separate accepting principal/authority, exact proposal digest, acceptance instant, disposition, and optional superseded acceptance.
- **Correction adjudication (new):** explicit resolution of conflicting proposals/acceptances; no latest-time winner.
- **Corrected representation identity (new semantic kind using existing generation identity):** a distinct `HUMAN_CORRECTED_REGION_TRANSCRIPTION`-equivalent payload and generation parented to the immutable provider generation, with accepted correction set and review/acceptance provenance.
- **Corrected content construction (new deterministic coordinator):** applies accepted exact-span substitutions in descending offset order per block, verifies original substrings, proves unaffected block/substring equality, and performs no inference.

Every contract receives an explicit schema version. Unknown versions fail closed. No giant aggregate combines review authority with correction authority: the atomic review aggregate contains only one review act and its observations; correction proposals and acceptances are separate records.

## 11. Storage Plan

### `HumanFidelityReviewStorage` (Sequence A)

A new store is required rather than altering `HumanVerificationRecordCodec` version 1. It follows the existing prepare/publish convention:

- create once by `HumanFidelityReviewId` using `CREATE_NEW` and atomic publication;
- canonical versioned encoding with size/count bounds and a record digest;
- exact duplicate replay returns the existing identity as an explicitly idempotent result; same identity/different bytes is a conflict;
- indexes by exact generation, evidence, reviewer, and predecessor are derived/rebuildable from canonical records, never independent authority;
- restart reload validates every record and index projection;
- corruption, trailing bytes, digest mismatch, unsafe IDs, unknown enum values, and unknown versions fail closed;
- no update or delete operation;
- supersession remains an immutable edge in a new record; cycles, missing predecessors, and cross-source/generation edges fail closed;
- unresolved incompatible effective reviews project conflict rather than choosing by time.

The existing `HumanVerificationStorage` remains readable and unchanged. Its legacy records may be displayed but cannot manufacture structured source-confirmed status.

### Human-fidelity governance audit storage (Sequence A)

A narrow append-only store records prepare authorization, publish result, supersession, conflict/adjudication, and eligibility decisions where the architecture audits access decisions. The mutation coordinator orders authorization, validation, durable prepare, durable fact audit, then publish according to the established crash-safe convention. Recovery must expose no accepted record without a corresponding required audit fact; ambiguous prepared state fails closed and is reconciled deterministically.

### Correction governance storage (Sequence B)

Correction proposals, acceptances/adjudications, and their audit facts use a separate create-once store/record family. Corrected content and generation use the existing generation/content prepare/publish stores after codec/payload support is explicitly added. Exact duplicate proposals may be idempotent; conflicting bytes, overlapping incompatible spans, invalid supersession, or ambiguous effective acceptance fail closed. No update/delete endpoint exists.

## 12. Supersession and Conflict Plan

Each superseding review or correction record names at most one immediate predecessor. The domain projector builds and validates an acyclic graph within the same evidence/source/generation/content lineage. Missing, cross-lineage, self, multi-parent, and cyclic edges fail closed. Timestamps are display facts only.

Compatible additive reviews may coexist. Conflicting material source resolutions, correction values, coverage claims, or supersession branches project `HUMAN_REVIEW_CONFLICT` until an explicitly authorized owner adjudication record selects or supersedes exact record IDs. While unresolved, no source-confirmed representation is selected. All branches and adjudications remain retrievable.

## 13. Audit Plan

Future audit records are durable facts, not narrative:

- review authorization evaluated;
- review prepared and published;
- discrepancy/pattern/source-resolution facts included by ID/digest;
- review supersession created;
- conflict detected and adjudicated;
- correction proposed;
- correction acceptance/rejection recorded;
- corrected generation/content prepared and published;
- correction supersession created; and
- effective-representation eligibility selected or denied for a named purpose.

Each binds correlation ID, principal, purpose, target source/generation/content, record IDs/digests, outcome, and time. Provider payload text is not copied into audit. Audit sequencing and crash recovery are tested; neither the existing ingestion audit nor the incomplete general `AuditService` is broadened incidentally.

## 14. Retrieval and Projection Plan

Canonical retrieval exposes separate named projections:

1. immutable provider transcription and complete provider provenance;
2. applicable human reviews and coverage;
3. discrepancy occurrences and optional patterns;
4. correction proposals/acceptances, when Sequence B exists;
5. effective representation identity for a requested purpose; and
6. an eligibility decision with reasons and assurance provenance.

Requesting the historical provider generation always returns its historical text (`Kellee`) and never silently substitutes corrected text. The R6 material review adds visible page-1/page-5 discrepancy facts and blocks source-confirmed use. Owner/Tier A presentation gains explicit review/discrepancy sections without hiding provider provenance. Existing ordinary-region rendering remains the content adapter.

## 15. Downstream Eligibility Plan

A new read-only eligibility evaluator accepts exact evidence/source/generation/content and a consumption purpose. It fails closed when review state is missing, malformed, conflicted, partially covers the requested region, or contains unresolved material discrepancy.

- raw provider transcription may remain retrievable and may enter only workflows explicitly permitting unreviewed/provider text with its assurance and acknowledgement;
- `UNREVIEWED` is never source-confirmed;
- known material discrepancy is never source-confirmed;
- minor discrepancy may qualify only where the frozen purpose rule expressly permits it and the discrepancy remains exposed;
- `HUMAN_REVIEWED_PASS` may qualify for source-confirmed use for an authorized purpose;
- after Sequence B, a fully bound accepted corrected representation may qualify independently while the provider parent remains `HUMAN_REVIEWED_WITH_DISCREPANCY`;
- unresolved conflict always denies source-confirmed eligibility.

`DocumentAnalysisCoordinator` and any downstream Memory/Knowledge promotion boundary must call this evaluator rather than infer assurance from execution success, admission, acknowledgement, or latest timestamp.

## 16. Historical Compatibility Plan

No existing derivative, human-verification record, codec, or store is rewritten. Existing provider derivatives remain canonically readable. Absence of a new exact-generation review record projects `UNREVIEWED`, never pass, verified, or failure. Legacy `HumanVerificationRecord` facts remain displayable and may inform coverage only under an explicit compatible adapter; they cannot create structured discrepancies or source-confirmed eligibility absent the new canonical record.

New codecs use new magic/version namespaces. Unknown versions and corrupt records fail closed without changing historical codec meaning. Restart tests use mixed legacy/new stores.

## 17. Fail-Closed Plan

Reject or deny effective state for:

- unknown review state, classification, severity, cause, resolution, or schema version;
- missing reviewer, source, preparation, generation, or content binding;
- source hash, generation digest, or content digest mismatch;
- invalid page/region/block/span or mismatched original substring;
- occurrence associated with the wrong pattern/review;
- pattern used as replacement authority;
- malformed, missing, cross-lineage, or cyclic supersession;
- overlapping incompatible corrections;
- proposal treated as acceptance;
- unauthorized reviewer/acceptor/adjudicator;
- unresolved material conflict;
- audit/publish ambiguity; or
- any attempt to infer source confirmation from provider execution success.

Failure never falls back to verified, corrected, latest, or raw-as-source-confirmed.

## 18. R6 Fixture

The required implementation and acceptance fixture is:

- evidence: `evidence-a51887d1-1a40-4b68-b340-c60e02e9a8d9`;
- source SHA-256: `5d73e6e55d3491e94aa9d6c02a0735572f9840fe8185a71546dba9f2258e237e`;
- provider generation: `region-f0df253d73500fef1dd5bbca186632c6be7f0a94faf10310e07cccb8fb673bc6`;
- generation SHA-256: `9fb18b02db5ac55e5d446cd48ebc619de929c4596f94d2a11fba1a07da71af14`;
- content SHA-256: `18a6ed08a4729350027d3140dc0f07dd49d32c04aa45f9e3e9558df5d007c4eb`;
- R6S worksheet SHA-256: `8e7928c671cd36c7a4517dc5d9429706c46efb65c565e948684d6c3e7c8773a4`;
- R6S owner-review record SHA-256: `2d47f50e0f2915bd0e18e914eac4bd5abc879cf5419969d482b2b7f6ff6b1293`;
- provider execution: `SUCCESS`;
- formal review: `FAIL`;
- descriptive fidelity: high fidelity overall with one systematic material proper-name discrepancy pattern;
- two independently bound occurrences: page 1 and page 5, `Kellee` to human-resolved source value `Kellec`;
- severity: `MATERIAL`, reason: identity-bearing proper name;
- cause: `UNKNOWN`;
- pattern count: one, with no replacement authority;
- provider derivative: immutable and retrievable;
- provider budget: one authorized, one consumed, zero retries.

Sequence A imports the signed/hash-bound R6S result, yields `HUMAN_REVIEWED_WITH_DISCREPANCY`, exposes both occurrences and the pattern, preserves execution `SUCCESS`, and denies source-confirmed eligibility. No corrected representation is created. Sequence B may later propose and accept the two exact corrections without another provider call.

## 19. Test Strategy

No tests are written in R6V. Future focused suites must cover:

- canonical review encoding/decoding and deterministic identity;
- exact source/generation/content/preparation/reviewer binding;
- page/region/block/code-point span and original-substring validation;
- complete versus partial coverage;
- classification and severity independent from descriptive quality;
- established, hypothesised, and unknown cause;
- source resolution independent from cause;
- pattern grouping without replacement authority;
- the two R6 occurrences remaining independently bound;
- immutable prepare/publish, exact duplicate idempotence, and conflict rejection;
- corrupt/malformed/trailing-byte/unknown-version failure;
- restart durability and index rebuilding;
- supersession cycle/cross-lineage rejection;
- incompatible reviews projecting conflict, with no latest-time winner;
- historical derivative defaulting to `UNREVIEWED`;
- material discrepancy blocking source-confirmed eligibility;
- provider execution remaining `SUCCESS` and provider text remaining retrievable;
- review authorization, correction authorization, and audit-ordering failures;
- proposal never implying correction acceptance;
- later corrected representation exact-span application and unaffected-content equality;
- exact R6 fixture offline acceptance and canonical presentation;
- no new attempt, provider state, provider call, retry, or external egress.

Focused suites should align with `HumanFidelityReviewCodecTest`, filesystem storage/restart tests, effective projection/eligibility tests, authorization/policy tests, owner presentation tests, and R6 fixture acceptance tests. Each implementation unit runs focused tests and `./gradlew test` with bounded heap where required, requiring zero failures/errors and recording totals/skips.

## 20. Sequence A — Minimum R6 Closure

1. **A1 Contracts:** implement frozen review/discrepancy/pattern/location/projection contracts and exhaustive validation.
2. **A2 Durability:** implement versioned create-once review and narrow audit stores with recovery tests.
3. **A3 Authority:** register and policy-bind only the review-recording purpose.
4. **A4 Service boundary:** implement authorized recording, effective projection, conflict rules, eligibility, and retrieval/presentation exposure.
5. **A5 Composition and fixture convergence:** compose locally and prove the exact R6S fixture offline without production mutation.
6. **A6 Artifact gate:** build, inspect, preserve, and present an exact production candidate for owner artifact acceptance.
7. **A7 Deployment gate:** after explicit artifact acceptance, deploy exactly that artifact and verify identity/readiness with the new store still unchanged.
8. **A8 Governed review-recording gate:** under a separate explicit owner authorization, canonically record exactly one R6S review; create no correction.
9. **A9 Production verification:** verify retrieval, conflict-safe projection, source-confirmed denial, immutable provider derivative, audit, idempotence, and zero provider activity.
10. **A10 R6 closure:** record an evidence-based closure decision only after every preceding gate passes.

## 21. Sequence B — Human-Corrected Representation

1. **B1 Correction contracts/storage:** implement proposal, acceptance, adjudication, supersession, and exact-span correction contracts and stores.
2. **B2 Deterministic representation:** add a distinct corrected payload/generation path, unchanged-content proof, retrieval, and eligibility projection.
3. **B3 Correction authority:** register and policy-bind `document-ingestion.human-transcription-correction`; proposal and acceptance remain distinct operations.
4. **B4 Composition/offline fixture:** prove two exact R6 corrections can be constructed locally while the parent remains unchanged.
5. **B5 Artifact acceptance/deployment:** use separate build, preservation, owner acceptance, deployment, and production verification gates.
6. **B6 Governed correction decision:** separately authorize exact proposals and owner acceptance/adjudication.
7. **B7 Corrected admission/readback:** create once, verify canonical corrected retrieval and source-confirmed eligibility, and retain provider/review history.

Sequence B cannot be smuggled into Sequence A or an artifact-deployment decision.

## 22. Detailed Implementation Units

| Unit | Purpose / permitted implementation surface | Prohibited surface | Dependencies | Tests and acceptance criteria | Repo mutation | Production mutation | Governed-state mutation | Owner gate / mandatory stop |
|---|---|---|---|---|---|---:|---:|---|
| R6V-A1 | Domain value types, immutable review aggregate, discrepancies, patterns, source resolution, location, projection contracts | Stores, APIs, policy, composition, correction | Accepted R6T/R6V | Exhaustive invariants/code-point fixtures; full suite pass | Domain + tests | No | No | Owner-accepted plan; stop on semantic conflict |
| R6V-A2 | Canonical codecs, review store, typed supersession validation, narrow audit port/store | Existing codec reinterpretation; UI; data import | A1 | Corruption/version/restart/atomicity/audit tests; full suite | Runtime/storage + tests | No | No | Stop on non-atomic/unrecoverable design |
| R6V-A3 | Register review-recording purpose and minimal owner policy | Correction purpose; broad admin purpose; Gap #54 | A1 | Registry/retirement/permission-denial tests | Purpose/policy + tests | No | No | Explicit policy-owner acceptance before activation |
| R6V-A4 | Recording coordinator, effective projector, eligibility evaluator, canonical retrieval/Tier A exposure | Corrected text; provider path; Memory redesign | A1–A3 | Conflict, historical, presentation, eligibility, provider-prohibition tests | Services/adapters + tests | No | No | Stop if source-confirmed cannot fail closed |
| R6V-A5 | Production composition wiring and exact R6 fixture offline convergence | Live recording/deployment | A1–A4 | Exact R6S fixture; 2 occurrences/1 pattern; zero egress; full suite | Composition + tests/report | No | No | Implementation review acceptance before build |
| R6V-A6 | Exact JAR/OCI build, artifact-local checks, archive preservation | Deploy, policy/data mutation | A5 | Identity/recoverability/offline fixture PASS | Report only after executable commit | No | No | Explicit owner artifact acceptance required |
| R6V-A7 | Deploy exact accepted artifact; readiness/identity/store-zero verification | Review import, correction, provider | A6 acceptance | Image/source/JAR exact; review store unchanged | Deployment report | Yes, deployment only | No | Stop after deployment verification |
| R6V-A8 | Canonically record exact R6S review under review purpose | Correction, derivative change, provider | A7 and explicit owner data authorization | CREATED/exact ID/digest; one review, 2 discrepancies, 1 pattern | Recording report | No | Yes, exact review/audit records only | Separate owner authorization; stop on any unexpected delta |
| R6V-A9 | Production readback, eligibility, immutability, idempotence verification | Further mutation; correction | A8 | Provider text retrievable; discrepancy visible; source-confirmed denied; hashes unchanged | Verification report | No | No | Stop on integrity/governance failure |
| R6V-A10 | R6 closure evidence/report | New capability/data | A9 | All closure criteria below | Closure report | No | No | Explicit owner closure decision |
| R6V-B1 | Correction proposal/acceptance/adjudication contracts, codecs, stores | Corrected admission, provider mutation | Sequence A | Exact-span/conflict/supersession/restart tests | Domain/storage + tests | No | No | Separate implementation acceptance |
| R6V-B2 | Corrected payload/kind, deterministic builder, generation/content integration, retrieval/eligibility | Global/fuzzy replacement; provider calls | B1 | Original-substring/unaffected-content/provenance/idempotence tests | Services/codecs + tests | No | No | Stop on semantic or historical mutation risk |
| R6V-B3 | Register correction purpose and owner acceptance/adjudication policy | Review-purpose broadening | B1 | Permission/purpose/audit tests | Purpose/policy + tests | No | No | Explicit policy-owner acceptance |
| R6V-B4 | Composition and exact R6 offline correction convergence | Production correction | B1–B3 | Two independent corrections; parent equality; full suite | Composition + tests/report | No | No | Implementation review before build |
| R6V-B5 | Candidate build, owner artifact acceptance, exact deployment, verification | Correction decision/admission | B4 | Artifact/recoverability/readiness/store-zero | Reports/config identity only | Deployment only at deployment sub-gate | No | Artifact acceptance and deployment kept separate |
| R6V-B6 | Record exact correction proposals and owner decisions | Admission before acceptance; provider | B5 + explicit owner authorizations | Exact accepted values, conflict state, audit | Decision report | No | Exact proposal/acceptance/audit only | Stop before admission unless separately authorized |
| R6V-B7 | Create-once corrected generation/content, canonical readback, eligibility verification | Parent mutation, retry, external reasoning | B6 + explicit admission authorization | Correct `Kellec` at exact spans, unchanged remainder, source-confirmed PASS, history intact | Admission report | No | Exact corrected generation/content/audit only | Stop on any unexpected mutation |

Provider activity allowed for every unit: **MUST ALWAYS BE NO**. Every unit has a mandatory stop on identity, authority, integrity, governance, or provider-accounting failure.

## 23. Build/Artifact/Deployment Gates

Executable source is committed and fully tested before a candidate build. The build unit captures JAR SHA, image/index, manifest, config, embedded source/JAR, artifact-local fixture results, and a recoverable protected archive. It cannot accept its own artifact.

Owner artifact acceptance is a separate exact-identity record. Deployment consumes that exact archive/image without rebuild or substitution. Deployment verification checks running identity, readiness, restart count, historical readback, and pre/post store hashes. Artifact deployment creates neither review authority nor governed review data.

Because a changed executable source may make existing implementation-bound V8 execution authority fail closed, deployment records the V8 evaluator state. R6 closure review/readback requires no V8 provider-execution authority and must not create one. Any future V8 execution reacceptance is a separate owner gate and is outside this zero-provider plan. No new capability identity is invented merely for document review; authority is supplied by the two frozen purposes and policy unless a later constitutionally mandated capability gate is separately scope-locked.

## 24. Governed-State Mutation Gates

Implementation, tests, build, preservation, artifact acceptance, and deployment permit zero governed-data mutation. The first production mutation is A8, after exact deployment and explicit owner authorization, and is limited to one canonical R6S review aggregate plus its required audit facts. Exact duplicate submission must be non-mutating/idempotent; contradictory submission fails closed.

Sequence B similarly separates proposal, acceptance/adjudication, and corrected admission. Each owner decision names exact record identities/digests. Store snapshots and historical hashes bracket every mutation. No gate authorizes updates/deletes, derivative replacement, or provider-state changes.

## 25. Provider and External Reasoning Boundary

No unit may call OpenAI, Claude, another provider, external OCR, or external reasoning. Review recording, projection, correction construction, and fixture verification consume only canonical local state and owner-produced records. There is no provider client dependency in a read/review/correction coordinator.

The historical real-document budget remains maximum one, consumed one, retries zero. No provider attempt or provider-state record is created. Any provider activity is a governance failure and mandatory stop.

Any later external-reasoning request must receive explicit representation type, review status, discrepancies/conflict, correction provenance, and purpose-specific eligibility. This plan neither authorizes nor implements that request.

## 26. R6 Closure Criteria

R6 may close only when:

- exact canonical review/discrepancy capability is deployed and authorized;
- the immutable R6S record is canonically recorded once with its exact worksheet/record digests;
- two page-bound material discrepancies and one non-replacing pattern are retrievable;
- formal verdict `FAIL`, high-overall-fidelity description, cause `UNKNOWN`, and human source resolution `Kellec` coexist without contradiction;
- provider execution remains `SUCCESS` and provider generation/content hashes are unchanged;
- canonical retrieval exposes provider text and the known discrepancy;
- source-confirmed eligibility for the uncorrected generation is denied;
- historical and malformed-state compatibility is verified fail closed;
- authority, audit, restart durability, and idempotence pass;
- production is stable and all unexplained store deltas are zero; and
- provider calls/retries/egress are `0 / 0 / 0` for the programme work and the historical budget remains `1 / 1 / 0`.

A corrected representation is explicitly not an R6 closure criterion.

## 27. Scope Exclusions

Excluded are production implementation in R6V itself; provider calls or retry; retranscription; external OCR/reasoning/verification; automated or LLM correction; spelling correction; identity resolution; fuzzy/global replacement; legal interpretation; admissibility or evidential-weight decisions; autonomous legal/materiality decisions; broad/collaborative editing UI; notifications; unrelated Memory/Knowledge work; Gap #54; and unrelated ingestion redesign.

## 28. Risks and Architectural Constraints

- Reusing `HumanVerificationRecord` directly would create ambiguous version semantics; use a new canonical record family and preserve v1.
- Reusing `DerivativeReviewRegistry` would lose restart durability and exact-generation binding; keep it non-authoritative.
- Review aggregate atomicity must not accidentally combine correction acceptance authority.
- Text spans must be Unicode code-point offsets against exact unnormalised block content; renderer offsets or normalised strings are unsafe.
- A material discrepancy can coexist with high overall fidelity; UI and APIs must not infer quality from severity.
- Pattern grouping can tempt global replacement; contracts must make replacement instructions unrepresentable.
- Multiple reviews require graph validation; timestamps cannot resolve conflict.
- Audit/publish crash windows require explicit recovery semantics before production mutation.
- Deployment source changes can affect implementation-bound V8 authority; review capability must neither depend on nor silently restore provider-execution authority.
- Downstream callers may currently treat admission or acknowledgement as sufficient. Every source-confirmed boundary must be inventoried and routed through the new evaluator before R6 closure.

## 29. Acceptance Criteria

This plan is ready for owner acceptance only if it maps every frozen decision, reuses existing contracts only where semantics match, introduces no unsupported mechanism, separates review from correction, supplies exact authority/storage/audit/retrieval/eligibility boundaries, preserves historical compatibility, defines explicit independently verifiable units and governance gates, uses the exact R6 fixture, and requires no provider activity.

Implementation acceptance later requires all focused and full tests to pass, exact offline fixture convergence, no historical rewrite, deterministic failure of malformed/conflicted state, and zero provider calls. Production acceptance later requires exact artifact identity, readiness, restart and store accounting, canonical readback, and owner-controlled mutation gates.

## 30. Frozen Implementation Boundaries

If this draft is accepted, implementation is bounded as follows:

1. Sequence A is the minimum R6 closure path; Sequence B is not included in it.
2. Existing provider generations/content and human-verification v1 records remain immutable.
3. New review records are exact-generation, content-bound, attributable, immutable, versioned, and create once.
4. Discrepancy, severity, quality, cause, pattern, and source resolution remain separate dimensions.
5. Exact location occurrences, not patterns or global strings, are the unit of correction.
6. Effective state is a validated projection over immutable facts, never a mutable flag or latest timestamp.
7. Unresolved material discrepancy/conflict denies source-confirmed eligibility.
8. Provider retrieval never silently returns corrected text.
9. Correction proposal, owner acceptance/adjudication, and corrected admission are separate Sequence B gates.
10. The two narrow Authorization Purposes cannot substitute for each other.
11. Artifact acceptance, deployment, policy activation, governed review recording, and closure remain separate owner-governed gates.
12. No unit requires or permits a provider call, retry, external egress, or external reasoning.

The plan is **ACCEPTED — CANONICAL — FROZEN**.
