# Ordinary Ingestion 11R6V-A5 — Production Composition Wiring and Exact R6 Offline Convergence

## Status and starting state

**COMPLETE — COMPOSITION AND ISOLATED CONVERGENCE ONLY**

Host `parker`, branch `main`, starting HEAD/upstream were exactly `d60530a7ec92af82222ead7857a5270e4285814c`; the worktree was clean. The frozen R6T Scope Lock SHA-256 was `f90c9fc654136ea5e92723a1704ad58108ab0f8a9e73a401e8039d3210e4cd2a`, and the frozen R6V implementation-plan SHA-256 was `86f7b27095a6b80b2618556797a877a21b097f76c3c9ba2d91e235b90c395d1d`. Completed identities were A1 `58c03abcd96b62f17f10069825982454fd06e680`, A2 `c355ba8ee9254bd1925ada7245e018e568f4616b`, A3 `8bfb6847204735cb416f654357bd153d183b0640`, and A4-MIN `d60530a7ec92af82222ead7857a5270e4285814c`.

## Composition root investigation

`ParkerRuntime` is the single production composition root. It already constructs one shared `InMemoryAuthorizationPurposeRegistry`, `InMemoryResourceRegistry`, `InMemoryActionVocabulary`, `DefaultPermissionPolicy`, `DefaultPermissionEngine`, and configured active owner `PrincipalId`. Persistent filesystem dependencies are constructed through named fail-closed startup stages from explicit `ParkerRuntimeConfig` paths. `docker-compose.yml` maps fixed `/data/...` paths to the governed `/mnt/parker-data/parker/...` hierarchy. A2 storage constructors already require an existing writable root and never create or fall back to an arbitrary root.

The composition therefore required no parallel architecture and no A1–A4-MIN redesign.

## Exact changes and storage-root design

Changed production files are `ParkerRuntime.kt`, `ParkerRuntimeConfig.kt`, `HumanFidelityReviewExactTargetRegistrar.kt`, and `docker-compose.yml`. Focused tests are in `ParkerRuntimeHumanFidelityReviewCompositionTest.kt`; the existing configuration-loader test gained only the new required-root cases.

Two explicit production paths were added:

- `PARKER_HUMAN_FIDELITY_REVIEW_STORAGE_ROOT=/data/human-fidelity-reviews`, mapped from `/mnt/parker-data/parker/human-fidelity-reviews`;
- `PARKER_HUMAN_FIDELITY_GOVERNANCE_AUDIT_STORAGE_ROOT=/data/human-fidelity-review-audit`, mapped from `/mnt/parker-data/parker/human-fidelity-review-audit`.

The environment loader requires both. Direct programmatic configurations retain a null/null compatibility state for pre-existing isolated tests; a partial pair fails startup. When configured, missing, non-directory, or unwritable roots fail through the named storage construction stage. There is no `/tmp`, working-directory, memory, or unrelated-store fallback. This unit did not execute Compose or create either host production directory.

## Purpose, authority, and service wiring

For a configured runtime, composition registers and activates exactly `document-ingestion.human-fidelity-review-recording`, registers `human-fidelity-review.record` as `WRITE/DOCUMENT`, and installs a verb-specific denial plus the exact active-purpose `HIGH_ASSURANCE` approval. Existing unconfigured test runtimes retain the previous vocabulary/rule set.

`HumanFidelityReviewExactTargetRegistrar` derives the A3 resource ID from all six target fields: evidence ID, source SHA-256, preparation identity, generation ID, generation SHA-256, and content SHA-256. It registers only that exact `LEGAL`, owner-bound document resource and rejects a conflicting pre-existing registration. It contains no wildcard, evidence-wide, or globally pre-authorized R6 target.

The runtime internally composes `FileSystemHumanFidelityGovernanceAudit`, `FileSystemHumanFidelityReviewStorage`, `HumanFidelityReviewRecordingPermissionPolicy`, and `DefaultGovernedHumanFidelityReviewRecordingService` using the one shared registry, vocabulary, resource registry, permission engine, and configured owner. No public HTTP, UI, or convenience recording entry point was added.

## Exact R6 isolated convergence

The production `ParkerRuntime` composition was started against temporary persistent roots using owner `owner.steven-francis-mctague`. The exact target bound evidence `evidence-a51887d1-1a40-4b68-b340-c60e02e9a8d9`, source SHA `5d73e6e55d3491e94aa9d6c02a0735572f9840fe8185a71546dba9f2258e237e`, preparation `85054cc742813d9b05339d07bce77d8665210b7c6e851fe9470b68a33c9bed8f`, generation `region-f0df253d73500fef1dd5bbca186632c6be7f0a94faf10310e07cccb8fb673bc6`, generation SHA `9fb18b02db5ac55e5d446cd48ebc619de929c4596f94d2a11fba1a07da71af14`, and content SHA `18a6ed08a4729350027d3140dc0f07dd49d32c04aa45f9e3e9558df5d007c4eb`.

The purpose was ACTIVE, the configured owner identity was active, the exact target resource resolved, and A3 approved only that target before A2 mutation. A4-MIN returned `Recorded`; A2 audit converged; canonical encoding of readback exactly equalled the request. Two MATERIAL `Kellee` occurrences on pages 1 and 5, one descriptive systematic pattern, two human source resolutions to `Kellec`, and UNKNOWN technical cause survived unchanged. Exact repetition returned `AlreadyRecorded`. Recomposition against the same temporary roots retrieved the identical immutable record and again returned `AlreadyRecorded`, proving restart durability independent of in-memory state.

No production R6S review or target authorization was recorded.

## Fail-closed matrix

- Missing required root: loader rejection.
- Unusable/nonexistent root: named runtime dependency-construction failure; no fallback or directory creation.
- Inactive or missing review purpose: denied with zero review/audit facts.
- Wrong principal or wrong purpose: denied with zero review/audit facts.
- Wrong evidence ID, source SHA, preparation identity, generation ID, generation SHA, or content SHA: exact target mismatch denial with zero review/audit facts.
- Restart: canonical review recovered from A2 filesystem state, not process memory.
- Provider dependency: absent from the composed review service and its storage/authority chain.

## Tests

Final focused composition run: **3 suites, 70 tests, 0 skipped, 0 failures, 0 errors** (`ParkerRuntimeHumanFidelityReviewCompositionTest`, `ParkerRuntimeConfigLoaderTest`, and the pre-existing `ParkerRuntimeAuthorizationPurposeCompositionTest`). A broader focused run also exercised A2, A3, and A4-MIN suites. The initial full run exposed one legacy fixed-count characterization; narrowing the new rules to explicitly configured runtimes preserved the historical composition and the focused regression passed.

Final `./gradlew test` result: **258 suites, 3,383 tests, 18 skipped, 0 failures, 0 errors**.

## Production and governed-state verification

Read-only verification found production unchanged: container `7d51a0c2b3c499cee97818c04c8599351cc03c6b515e5ff0358eaa95dfef62fc`, image/index `sha256:55b4f29b4e8f30b80528fda075c7936a968ae98a0b6b54e55b536e9fb9d9ac9c`, source `d45efbee348b842340616a6a73831ef130086d90`, and runtime JAR SHA-256 `71f154b230a5ce318915f7fdc66b24ad11393c0112e5f76a1a9c289255c3815a`. It remained running with restart count zero; startup/runtime logs remained healthy, so readiness was PASS.

Evidence, manifests, corrected preparations, capability acceptances, authorizations, attempts, provider state, derivative generations, and derivative content retained their established counts/hashes. Governed production-store delta was **0**. Provider calls, retries, and external evidence egress were **0 / 0 / 0**.

## Outstanding frozen Sequence A requirements

The following frozen requirements remain expressly outstanding after A5: effective-review projection beyond the single canonical record; conflict/supersession/adjudication projection; purpose-specific source-confirmed eligibility; retrieval/presentation integration exposing review, discrepancy, and eligibility facts; canonical production R6S review recording; and final R6 closure verification. They are deferred, not cancelled, and this unit makes no R6-closure claim.

Correction purpose, correction proposal/acceptance, corrected representation, and all other correction work remain untouched Sequence B scope.

## Verdict

**A — GOVERNED HUMAN FIDELITY REVIEW RECORDING COMPOSITION AND R6 OFFLINE CONVERGENCE VERIFIED**
