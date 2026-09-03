# Ordinary Ingestion 11R6V-A1 — Governed Human Fidelity Review Domain Contracts and Exhaustive Validation

## Status and starting state

**COMPLETE — CONTRACT IMPLEMENTATION AND STATIC/OFFLINE VERIFICATION ONLY**

Host `parker`, branch `main`, starting HEAD and upstream were exactly `cd4b5a34cc8dc9c1991368c4e697aa6e7788ab64`; the worktree was clean.

The authoritative frozen Scope Lock SHA-256 was verified as `f90c9fc654136ea5e92723a1704ad58108ab0f8a9e73a401e8039d3210e4cd2a`. The authoritative frozen implementation-plan SHA-256 was verified as `86f7b27095a6b80b2618556797a877a21b097f76c3c9ba2d91e235b90c395d1d`. Both documents reported `ACCEPTED — CANONICAL — FROZEN` before implementation.

## Codebase reuse findings

The implementation reuses existing `EvidenceArtifactId`, `DerivativeGenerationId`, `DerivativeContentIdentity` digest semantics through `OcrSha256Digest`, `PrincipalId`, `PageRepresentationId`, `SourceRegionId`, and the established one-based page and bounded immutable-value conventions.

Historical `HumanVerificationRecord`, `HumanVerificationOutcome`, their version-1 codec/storage meaning, `DerivativeReviewRegistry`, `DerivativeReviewState`, provider derivative records, and R6 execution/attempt/provider-state records were not modified. The new model is additive in [HumanFidelityReview.kt](../../src/interfaces/HumanFidelityReview.kt).

## New domain contracts

The A1 implementation adds:

- deterministic opaque `HumanFidelityReviewId`, `FidelityDiscrepancyId`, and `SystematicDiscrepancyPatternId` values;
- the closed four-value `HumanFidelityReviewState` projection vocabulary;
- immutable full/partial `HumanFidelityReviewCoverage` and exact character scopes;
- the frozen discrepancy classifications and `MINOR`, `MATERIAL`, and `NON_ERROR_OBSERVATION` severity;
- independent `ESTABLISHED`, `HYPOTHESISED`, and `UNKNOWN` cause assessment;
- independent unresolved or attributable source-resolved `HumanSourceResolution`;
- exact evidence/source/preparation/generation/content/page/region/block/code-point/sub-string location binding;
- immutable location-bound discrepancy occurrences and explicit systematic-pattern associations;
- immutable target, artifact, predecessor, and adjudication-reference value contracts;
- one immutable completed `HumanFidelityReviewRecord` with formal state and descriptive fidelity kept separate; and
- value-only effective-review and source-confirmed eligibility projections with explicit fail-closed denial reasons.

The record contains neither execution authority, correction acceptance, mutable effective/source-confirmed flags, provider authority, nor corrected content.

## Validation invariants

Validation rejects malformed identities/digests, empty or excessive bounded values, invalid/duplicate page coverage, ambiguous overlapping partial scopes, whole-generation PASS under partial coverage, completed `UNREVIEWED` or conflict records, PASS with discrepancies, discrepancy verdict without facts, cross-review/cross-target/cross-reviewer bindings, and non-reciprocal pattern membership.

`OTHER_EXPLICITLY_CLASSIFIED` requires explicit detail. `APPROPRIATE_UNCERTAINTY` and `NON_ERROR_OBSERVATION` are constrained to occur together. Only missing-source-text observations may use an empty insertion span. Cause UNKNOWN forbids a fabricated mechanism/basis; HYPOTHESISED requires a hypothesis; ESTABLISHED requires both mechanism and supporting basis. Source resolution validates the exact asserted value digest and remains valid with cause UNKNOWN.

Patterns require at least two unique explicit members, exact occurrence count, review/reviewer binding, and reciprocal occurrence association. They contain no replacement or correction authority. Local supersession rejects self-reference and target contradiction; adjudication references require at least two unique conflicts and an explicitly selected member. Global graph traversal/cycle resolution remains assigned to A4/A2 and was not falsely claimed by A1.

Projection values have no permissive default: denial requires an explicit reason; allowed results cannot carry one; UNREVIEWED and unresolved conflict have exact fail-closed shapes.

## Unicode span implementation

`FidelityDiscrepancyLocation.fromProviderBlock` treats offsets as half-open Unicode code-point offsets over the exact unnormalised provider block. It bounds the block, validates the code-point range, converts with `offsetByCodePoints`, verifies the exact substring, and verifies its SHA-256. UTF-16 character offsets are not the semantic contract.

The focused suite proves this with a non-BMP emoji before `Kellee`: the UTF-16 and code-point offsets differ, while exact substring resolution remains correct. Negative, reversed, out-of-range, page-zero, mismatched-substring, and digest-mismatch cases fail closed.

## Exact R6 fixture representation

The contract test binds exact evidence `evidence-a51887d1-1a40-4b68-b340-c60e02e9a8d9`, source SHA `5d73e6e55d3491e94aa9d6c02a0735572f9840fe8185a71546dba9f2258e237e`, preparation `85054cc742813d9b05339d07bce77d8665210b7c6e851fe9470b68a33c9bed8f`, provider generation `region-f0df253d73500fef1dd5bbca186632c6be7f0a94faf10310e07cccb8fb673bc6`, generation SHA `9fb18b02db5ac55e5d446cd48ebc619de929c4596f94d2a11fba1a07da71af14`, content SHA `18a6ed08a4729350027d3140dc0f07dd49d32c04aa45f9e3e9558df5d007c4eb`, worksheet SHA `8e7928c671cd36c7a4517dc5d9429706c46efb65c565e948684d6c3e7c8773a4`, and owner-record SHA `2d47f50e0f2915bd0e18e914eac4bd5abc879cf5419969d482b2b7f6ff6b1293`.

Deterministic representative provider blocks exercise the truthful `Kellee`/`Kellec` semantics without fabricating production offsets: two independently page-bound MATERIAL discrepancies on pages 1 and 5, one explicit systematic pattern, cause UNKNOWN, source resolved to `Kellec` by the reviewer, formal `HUMAN_REVIEWED_WITH_DISCREPANCY`, and the independent description “high fidelity overall with one systematic material proper-name discrepancy pattern.” Provider execution SUCCESS remains an independent historical fact and is not represented as review status.

## Tests

Focused command:

`./gradlew test --tests parker.core.interfaces.HumanFidelityReviewTest --no-daemon -Dorg.gradle.jvmargs=-Xmx4g -Dkotlin.daemon.jvm.options=-Xmx4g`

Result: **1 suite, 12 tests, 0 skipped, 0 failures, 0 errors**.

The first default-heap attempt compiled production contracts but exhausted the Kotlin test compiler heap before test execution. The established bounded four-gigabyte heap command above resolved only that infrastructure constraint. No source accommodation was made.

Full command:

`./gradlew test --no-daemon -Dorg.gradle.jvmargs=-Xmx4g -Dkotlin.daemon.jvm.options=-Xmx4g`

Result: **252 suites, 3,341 tests, 18 skipped, 0 failures, 0 errors**. The live-model/external evaluation source set is not attached to the ordinary test lifecycle.

## Production, governed-store, and provider verification

Before and after A1, production remained container `7d51a0c2b3c499cee97818c04c8599351cc03c6b515e5ff0358eaa95dfef62fc`, image/index `sha256:55b4f29b4e8f30b80528fda075c7936a968ae98a0b6b54e55b536e9fb9d9ac9c`, source `d45efbee348b842340616a6a73831ef130086d90`, runtime JAR SHA-256 `71f154b230a5ce318915f7fdc66b24ad11393c0112e5f76a1a9c289255c3815a`, running with restart count zero and authenticated readiness `READY`/PASS.

Before/after governed-store counts and aggregate hashes were identical: evidence 29 (`e5d29f86...`), manifests 29 (`ec2a7dc1...`), corrected preparations 6 (`07018599...`), capability acceptances 14 (`14eeeb9c...`), authorizations 14 (`b9b30cce...`), attempts 10 (`798ed0fd...`), provider state 8 (`be1b3310...`), derivative generations 23 (`801bdfd3...`), derivative content 21 (`80eee2bb...`), evidence audit 1 (`1d8b0f78...`), and document-ingestion audit 1 (`3ff333aa...`). Governed-store delta: **0**.

OpenAI calls, Claude calls, other provider calls, retries, and external evidence egress were respectively **0 / 0 / 0 / 0 / 0**.

## A1 boundary confirmation and verdict

No durable or filesystem review storage, durable persistence codec, audit storage, Authorization Purpose, permission policy, authorized recording service, effective-state calculation service, production integration, `DocumentAnalysisCoordinator` change, Tier A presentation change, composition, deployment, canonical R6S recording, correction contract, corrected representation, or other Sequence B work was implemented.

**A — GOVERNED HUMAN FIDELITY REVIEW DOMAIN CONTRACTS IMPLEMENTED AND VERIFIED**
