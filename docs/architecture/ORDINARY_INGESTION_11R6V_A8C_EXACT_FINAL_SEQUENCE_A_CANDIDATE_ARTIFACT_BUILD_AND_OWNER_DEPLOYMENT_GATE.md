# OI11R6V-A8C — Exact Final Sequence A Candidate Artifact Build and Owner Deployment Gate

## Verdict

**A — FINAL SEQUENCE A CANDIDATE BUILT AND READY FOR OWNER DEPLOYMENT DECISION**

## Starting repository state and governance

The unit started on host `parker`, branch `main`, at clean HEAD/upstream `e2e824c062c94ffe5b8b75a387a753de7d2f72ce`. The frozen R6T Scope Lock SHA-256 was `f90c9fc654136ea5e92723a1704ad58108ab0f8a9e73a401e8039d3210e4cd2a`; the frozen R6V Implementation Plan SHA-256 was `86f7b27095a6b80b2618556797a877a21b097f76c3c9ba2d91e235b90c395d1d`. The A8, A8A, and A8B reports were present.

The mandatory pre-build suite initially encountered host `/tmp` quota exhaustion across unrelated temporary-storage tests. Inspection found hundreds of thousands of accumulated owner-created Parker test directories. Only explicitly enumerated Parker test temporary-directory prefixes were removed; repository and production governed data were untouched. The unchanged suite was rerun and passed completely.

## Full test gate

`./gradlew test` passed with 259 suites, 3,394 tests, 18 skipped, zero failures, and zero errors.

## Exact distribution build

The established bounded offline command was:

`PARKER_BUILD_COMMIT=e2e824c062c94ffe5b8b75a387a753de7d2f72ce ./gradlew clean installDist --offline --no-daemon -Dorg.gradle.jvmargs=-Xmx4g -Dkotlin.daemon.jvm.options=-Xmx4g`

It completed successfully without a source amendment or dependency upgrade.

- Source commit: `e2e824c062c94ffe5b8b75a387a753de7d2f72ce`
- Runtime JAR: `build/libs/parker-platform-0.8.0-runtime-complete.jar`
- Runtime JAR size: `4,917,260` bytes
- Runtime JAR SHA-256: `d78c99a389ba3a75e868d007ac13f0678f84ac25e46e7aeca41a513f510865e2`
- Installed-distribution JAR: byte-identical
- Manifest: `Parker-Source-Commit: e2e824c062c94ffe5b8b75a387a753de7d2f72ce`

## Exact image build and candidate identity

Following the established A6 isolated-context procedure, the exact installed distribution and existing `tools` were copied into a fresh `/tmp/oi11r6v-a8c-context` and layered over the already-local accepted base `sha256:d33a5a47f8a540bf11375c2fd373d5bf3257f36da5f0f4afb444bbf3ce46f9cb`.

The exact no-network command was:

`docker buildx build --network=none --pull=false --platform linux/amd64 --provenance=false --file /tmp/oi11r6v-a8c-context/Dockerfile --load --tag parker-platform:oi11r6v-a8c-e2e824c /tmp/oi11r6v-a8c-context`

- Docker image ID: `sha256:26a503564698b4ac248cb3e9d94ceba7813b713523dd9b995e73cbf922267895`
- Local image/manifest digest: `sha256:26a503564698b4ac248cb3e9d94ceba7813b713523dd9b995e73cbf922267895`
- OCI config digest: `sha256:7427e35399b1a9d62b7768c70c9612727cde4f5de2239569012eceae836a5768`
- Created: `2026-09-03T12:21:53.498201681Z`
- Platform: `linux/amd64`
- Runtime user: `parker`, established UID/GID `999:999`
- Entry point: `/opt/parker/bin/parker`
- Image size: `1,032,120,519` bytes
- Revision label: `e2e824c062c94ffe5b8b75a387a753de7d2f72ce`
- Title: `Parker OI11R6V-A8C final Sequence A candidate`

A stopped temporary container was used only to extract the final JAR. Its SHA-256 was exactly `d78c99a389ba3a75e868d007ac13f0678f84ac25e46e7aeca41a513f510865e2`, matching the built accepted JAR, and its manifest retained the exact source commit. The temporary container was removed. No image was pushed or deployed.

## Candidate capability verification

Candidate bytecode/metadata contains:

1. `DefaultGovernedHumanFidelityReviewRecordingService` and the A5 recording composition;
2. `DefaultEffectiveHumanFidelityReviewProjector`;
3. `HumanFidelityEligibilityUse.SOURCE_CONFIRMED_WHOLE_GENERATION`;
4. the A8B `TierAContentRetrievalCoordinator` integration;
5. `OwnerHumanFidelityStatus`;
6. additive `humanFidelityStatus` owner JSON presentation;
7. `PARKER_HUMAN_FIDELITY_REVIEW_STORAGE_ROOT` and `PARKER_HUMAN_FIDELITY_GOVERNANCE_AUDIT_STORAGE_ROOT`;
8. `document-ingestion.human-fidelity-review-recording`;
9. no `document-ingestion.human-transcription-correction` purpose;
10. no Sequence B corrected-representation capability.

An isolated candidate startup was not repeated. A6 already established startup of the same accepted local base and distribution layout with network none and temporary stores; A8C instead used the existing production-equivalent composition and retrieval tests, avoiding a new test architecture or any production mounts.

## Exact R6 offline regression

The established offline regression command selected the A8A projector, canonical Tier A retrieval, HTTP presentation, and production-equivalent human-fidelity composition suites. Result: 4 suites, 119 tests, zero skipped/failures/errors.

The preserved R6 result is:

- provider representation: `RETRIEVED / REGION_TRANSCRIPTION / UNCHANGED`;
- review: `HUMAN_REVIEWED_WITH_DISCREPANCY`;
- coverage: `FULL_GENERATION`;
- material discrepancies: `2`;
- systematic patterns: `1`;
- unresolved conflict: `false`;
- source-confirmed eligibility: `DENIED / MATERIAL_DISCREPANCY`;
- provider values: `Kellee` at both immutable provider locations;
- human source resolution: `Kellec` at pages 1 and 5.

No canonical review was created or changed during testing.

## Production pre/post baseline and historical preservation

Production remained unchanged before and after candidate construction:

- Container: `ccf93adcaf7b37e12eb5d8f93c7419d588d713c03881420b49021e5dd8e1b707`
- Image: `sha256:51eff5a7060b478ec66b9ad6e42b56b4ec920d142a984b6e5e7e13dce56f89f5`
- Source: `01fd54237227daff7d0b83064825dd004c9fa1f6`
- Runtime JAR SHA-256: `dc04f7c3498607f35b721348087389f7b1c15e9064ed8a98c5e11c765b2b981c`
- Status/readiness: running / PASS
- Restart count: `0`

Canonical R6S review preservation:

- review count: `1`
- review ID: `review-3cf3186ca166acb0f4b6331ca574926dc874225247b296fb972666504992ea6e`
- stored-record SHA-256: `13e6f5e285d95e19c0926821b63422486e005d22ee484feb70a6b54635046106`
- audit fact count: `3`

Historical derivative preservation:

- generation SHA-256: `9fb18b02db5ac55e5d446cd48ebc619de929c4596f94d2a11fba1a07da71af14`
- content SHA-256: `18a6ed08a4729350027d3140dc0f07dd49d32c04aa45f9e3e9558df5d007c4eb`
- derivative generation/content counts: `23 / 21`, unchanged
- provider attempt/provider-state counts: `10 / 8`, unchanged
- historical provider budget: maximum `1`, consumed `1`, retries `0`

OpenAI calls, Claude calls, other provider calls, retries, and external evidence egress were all zero.

## Deployment and correction boundary

Deployment has **not** occurred. Production was not restarted and no production store was mutated. No review was recorded or replaced. No correction purpose, proposal, acceptance, corrected representation, provider call, retranscription, external reasoning, Gap #54, or Sequence B capability was introduced.

Deployment requires a separate owner decision accepting the exact source, JAR, image/manifest, and OCI config identities recorded above.
