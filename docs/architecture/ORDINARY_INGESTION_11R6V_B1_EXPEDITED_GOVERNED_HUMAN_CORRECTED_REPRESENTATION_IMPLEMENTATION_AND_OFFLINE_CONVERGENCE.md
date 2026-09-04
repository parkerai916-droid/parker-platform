# Ordinary Ingestion 11R6V-B1 — Expedited Governed Human-Corrected Representation Implementation and Offline Convergence

## Status

IMPLEMENTED — OFFLINE CONVERGED — PENDING OWNER ARTIFACT/DEPLOYMENT GATE

## Authoritative governance

- Starting `main` HEAD/upstream: `c1a4ada7e95bb03cf13ecff52f1c39ce67466bc1`; worktree clean.
- Frozen R6T Scope Lock SHA-256: `f90c9fc654136ea5e92723a1704ad58108ab0f8a9e73a401e8039d3210e4cd2a`.
- Frozen R6V Implementation Plan SHA-256: `86f7b27095a6b80b2618556797a877a21b097f76c3c9ba2d91e235b90c395d1d`.
- Sequence A closure remains immutable and was not reopened.

The frozen documents supplied the required decisions: a separate correction proposal and acceptance act, the exact purpose `document-ingestion.human-transcription-correction`, exact-span application, an immutable `HUMAN_CORRECTED_REGION_TRANSCRIPTION`, independent provider history, and purpose-specific eligibility. No new architectural decision or owner gate was required.

## Repository inspection and reuse

B1 reused the existing `HumanFidelityReviewRecord`, exact six-part `HumanFidelityReviewTarget`, location-bound discrepancy/source-resolution contracts, `PrincipalId`, Authorization Purpose registry, action/resource permission policy, effective-review projector, derivative generation/content stores, Tier A region-transcription contract, create-once prepare/publish convention, narrow audit convention, runtime composition root, and immutable retrieval-result pattern.

The provider derivative, `HumanVerificationRecord` v1, `DerivativeReviewRegistry`, Sequence A human-review records, and existing provider authorization/execution/budget semantics were not changed.

## Implemented domain capability

The additive domain contains deterministic `CorrectionProposalId` and `CorrectionAcceptanceId` values, immutable proposal and acceptance facts, an immutable content-derived human-corrected representation identity, exact provider/review/acceptance/proposal references, canonical corrected content SHA-256, creation and authorization provenance, representation/schema identities, explicit correction results, and a separate corrected-representation presentation contract.

Proposal identities bind the exact review, discrepancy, six-part target, provider-before value, accepted source-after value, proposer, instant, and reason. Acceptance is a separate immutable act bound to the exact proposal set, owner, target, review, instant, and correction purpose. The corrected representation is straw-free: it contains no provider authority, retry authority, fuzzy selector, global replacement instruction, technical-cause inference, or mutable provider flag.

## Authorization purpose and policy

The exact active purpose is:

`document-ingestion.human-transcription-correction`

It is registered only when the correction roots and existing human-review capability are configured. The policy is owner-bound, exact seven-part correction-resource-bound (the six-part provider target plus canonical review identity), and action-bound. An unconditional deny rule precedes the purpose-specific high-assurance allow rule. Wrong owner, absent/wrong/inactive purpose, target mismatch, review mismatch, unknown resource, malformed input, or permission denial fails closed before provider resolution or governed correction/audit mutation. Human-fidelity-review recording and external-transcription purposes do not confer correction authority.

## Durability and audit

`HumanCorrectedRepresentationCodec` is an explicit deterministic version-1 codec. It reconstructs through validated domain constructors, bounds byte/string/item counts, rejects unsupported versions, truncation, trailing bytes, malformed identities, and inconsistent derived identities, and never serializes paths or runtime implementation details.

`FileSystemHumanCorrectedRepresentationStorage` has a separate namespace, safe opaque names, create-once preparation, atomic publication, exact-duplicate idempotence, conflicting-byte rejection, canonical readback, no update, and no delete. Restart reconstructs the same identity/content/provenance. Corrupt canonical state fails closed and is never repaired or overwritten automatically.

The dedicated append-only correction audit records only `CORRECTED_REPRESENTATION_PREPARED` and `CORRECTED_REPRESENTATION_PUBLISHED`. Event identity binds all factual event fields. PREPARED follows durable preparation; PUBLISHED follows publication and exact canonical readback. Success additionally requires both exact audit facts to read back. Audit entries are create-once, restart-readable, filename-bound to identity, and fail closed on corruption.

## Service boundary and correction application

`DefaultGovernedHumanCorrectionService` performs:

1. request/authority/target/review consistency checks;
2. correction permission evaluation before any correction or correction-audit mutation;
3. exact canonical review lookup and fail-closed effective-state check;
4. exact canonical provider-generation/content resolution and integrity verification;
5. verification that every material discrepancy is independently proposed and accepted;
6. verification of exact reviewed provider-before and human-resolved source-after values;
7. derivative-region resolution and block-local Unicode code-point span validation;
8. descending exact-span application with overlap rejection;
9. deterministic corrected content and identity construction;
10. create-once prepare, factual audit, publish, canonical readback, audit convergence, and result.

There is no global search-and-replace. The systematic pattern has no execution role. A discrepancy on page 1 and a discrepancy on page 5 are mapped through their distinct canonical derivative-region identities even though both have region-local block index zero.

## Retrieval, presentation, and eligibility

The internal read-only corrected-representation retrieval seam returns the immutable corrected representation and its eligibility separately. Eligibility is allowed only after canonical review lookup, exact target equality, full-generation coverage, deterministic non-conflicting projection, and incorporation of the complete material-discrepancy set. Missing, partial, mismatched, conflicting, or malformed state is denied fail closed.

Sequence A provider eligibility is unchanged: the raw R6 provider derivative remains `DENIED — MATERIAL_DISCREPANCY`. The independently stored corrected representation is eligible for `SOURCE_CONFIRMED_WHOLE_GENERATION` after both accepted source resolutions are incorporated. Retrieval keeps provider and human-corrected representations distinct and preserves explicit provider/review/discrepancy/acceptance provenance.

## Production composition

The existing `ParkerRuntime` composition now conditionally wires the correction purpose, action, permission rules, exact-target registrar, stored-provider resolver, create-once corrected store, narrow audit, governed correction service, eligibility evaluator, and internal retrieval seam. The configured persistent roots are:

- `PARKER_HUMAN_CORRECTED_REPRESENTATION_STORAGE_ROOT` → `/data/human-corrected-representations`;
- `PARKER_HUMAN_CORRECTION_AUDIT_STORAGE_ROOT` → `/data/human-correction-audit`.

Their Compose host paths follow Parker's governed hierarchy under `/mnt/parker-data/parker/`. Configuration is paired and fail closed, requires the existing human-review roots, and has no temporary/in-memory fallback. No public correction endpoint was added.

## Exact R6 offline convergence

The production-equivalent isolated fixture uses the exact R6 evidence, source, preparation, provider-generation/content integrity identities and canonical review semantics. Its five distinct representative provider blocks preserve the canonical location bindings and exact Kellee/Kellec spans without fabricating production state.

Result:

- provider representation: immutable and unchanged;
- canonical human review: immutable and unchanged;
- corrected representation: created exactly once and canonically read back;
- corrections applied: exactly two independently location-bound resolutions;
- page 1: `Kellee` → `Kellec`;
- page 5: `Kellee` → `Kellec`;
- pages/blocks 2, 3, and 4: byte-for-byte unchanged;
- systematic pattern: descriptive only;
- technical cause: remains `UNKNOWN`;
- duplicate creation: `AlreadyCreated`, without rewriting facts;
- restart: identical identity, content, provenance, and eligibility;
- provider derivative eligibility: `DENIED — MATERIAL_DISCREPANCY`;
- corrected representation eligibility: `ALLOWED` for source-confirmed whole-generation use.

## Negative and fail-closed verification

Focused tests cover authorization denial/exception before provider or storage access; wrong target/review; incomplete material-resolution sets; publication failure; missing canonical readback; exact duplicate; corrupt/tampered canonical representation; codec version/truncation/trailing-byte failure; create-once durability; audit idempotence/corruption; restart readback; complete audit convergence; exact region/span application; unchanged unaffected content; and production composition without a public write route.

The domain and policy also reject wrong principal/purpose/target, incorrect provider-before or human-after values, duplicate/overlapping applications, unreviewed locations, unresolved review conflict, unknown representations, and same-identity conflicting content. Every pre-authorization denial produces zero corrected-representation and correction-audit mutation.

## Tests

- Focused B1 gate: 4 suites, 15 tests, zero failures/errors.
- Full convergence gate (`./gradlew test`): 262 suites, 3,405 tests, 18 skipped, zero failures/errors.

## Historical compatibility and production boundary

Existing Sequence A tests, historical Tier A retrieval, unreviewed/fail-closed retrieval, human-review projection, provider retrieval, and permission behavior remain green. No existing durable codec was changed.

B1 did not deploy, restart production, create production roots, write a production correction, mutate the canonical R6 review, or mutate any provider/evidence/preparation state. Per the owner's compute-conscious direction, no redundant Docker production check was performed; the established Sequence A production baseline remains the referenced baseline. Production governed-state delta is zero.

Provider accounting during B1 is OpenAI 0, Claude 0, other reasoning providers 0, retries 0, and external evidence egress 0.

## Exact files changed

- additive human-corrected representation domain, codec, store, audit, policy, service, provider resolver, eligibility/retrieval seam, and exact-target registrar;
- existing runtime configuration/composition and Compose roots;
- focused correction/durability/composition tests;
- this single B1 report.

No Sequence A implementation, provider implementation, source evidence, governed production record, or unrelated ingestion feature changed.

## Remaining work

Subject to owner review, remaining Sequence B work is limited to the exact candidate artifact/build and deployment gate, separately authorized deployment/root preparation, production canonical correction recording if authorized, final production retrieval/eligibility/immutability verification, and Sequence B closure. No such action occurred in B1.
