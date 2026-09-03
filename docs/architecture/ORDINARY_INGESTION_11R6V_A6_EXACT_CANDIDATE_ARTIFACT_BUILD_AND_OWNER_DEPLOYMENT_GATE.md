# Ordinary Ingestion 11R6V-A6 — Exact Candidate Artifact Build and Owner Deployment Gate

## Status and starting gate

**COMPLETE — OWNER DEPLOYMENT DECISION REQUIRED**

Host `parker`, branch `main`, starting HEAD and upstream were exactly `01fd54237227daff7d0b83064825dd004c9fa1f6`; the worktree was clean. The frozen R6T Scope Lock SHA-256 was verified as `f90c9fc654136ea5e92723a1704ad58108ab0f8a9e73a401e8039d3210e4cd2a`, and the frozen R6V implementation-plan SHA-256 as `86f7b27095a6b80b2618556797a877a21b097f76c3c9ba2d91e235b90c395d1d`. The A5 completion report existed with SHA-256 `4386d2a39b5d0329d6235780eaccf2ddb7641f47279022d0e10f7b4428223168`.

The accepted implementation chain is A1 `58c03abcd96b62f17f10069825982454fd06e680`, A2 `c355ba8ee9254bd1925ada7245e018e568f4616b`, A3 `8bfb6847204735cb416f654357bd153d183b0640`, A4-MIN `d60530a7ec92af82222ead7857a5270e4285814c`, and A5 `01fd54237227daff7d0b83064825dd004c9fa1f6`.

## Tests

The mandatory pre-build `./gradlew test --no-daemon -Dorg.gradle.jvmargs=-Xmx4g -Dkotlin.daemon.jvm.options=-Xmx4g` gate remained green. Its established complete result is **258 suites, 3,383 tests, 18 skipped, 0 failures, 0 errors**.

After candidate construction, the exact A5 offline regression was run with `./gradlew test --tests parker.composition.ParkerRuntimeHumanFidelityReviewCompositionTest --offline --no-daemon -Dorg.gradle.jvmargs=-Xmx4g -Dkotlin.daemon.jvm.options=-Xmx4g`: **1 suite, 4 tests, 0 skipped, 0 failures, 0 errors**.

## Exact JAR build

The established bounded offline command was:

`PARKER_BUILD_COMMIT=01fd54237227daff7d0b83064825dd004c9fa1f6 ./gradlew clean installDist --offline --no-daemon -Dorg.gradle.jvmargs=-Xmx4g -Dkotlin.daemon.jvm.options=-Xmx4g`

It completed successfully. The JAR placed into the candidate was `build/libs/parker-platform-0.8.0-runtime-complete.jar`, size `4,890,787` bytes, SHA-256 `dc04f7c3498607f35b721348087389f7b1c15e9064ed8a98c5e11c765b2b981c`. The installed-distribution copy was byte-identical. Its manifest contains `Parker-Source-Commit: 01fd54237227daff7d0b83064825dd004c9fa1f6`, and extraction from the final candidate reproduced the same SHA-256.

## Exact image build and identity

Following the established R6P isolated-context pattern, the exact installed distribution and `tools` were copied over the already-local accepted runtime base `sha256:d33a5a47f8a540bf11375c2fd373d5bf3257f36da5f0f4afb444bbf3ce46f9cb`. The successful command used the temporary context only:

`docker buildx build --network=none --pull=false --platform linux/amd64 --provenance=false --file /tmp/oi11r6v-a6-context/Dockerfile --load --tag parker-platform:oi11r6v-a6-01fd542 /tmp/oi11r6v-a6-context`

No image was pushed. Build completion/creation was `2026-09-03T10:49:09.091499588Z`.

- Exact source: `01fd54237227daff7d0b83064825dd004c9fa1f6`.
- Runtime JAR SHA-256: `dc04f7c3498607f35b721348087389f7b1c15e9064ed8a98c5e11c765b2b981c`.
- Docker image ID: `sha256:51eff5a7060b478ec66b9ad6e42b56b4ec920d142a984b6e5e7e13dce56f89f5`.
- Local single-platform image/manifest digest: `sha256:51eff5a7060b478ec66b9ad6e42b56b4ec920d142a984b6e5e7e13dce56f89f5`.
- OCI config digest reported by BuildKit: `sha256:5dc4085de9105b471d9bd14e0295e62fa183b047cbb7ac4601281ffd0b83b6ea`.
- Platform: `linux/amd64`; runtime user `parker` (UID/GID `999:999`); entry point `/opt/parker/bin/parker`.
- Image size: `1,032,094,652` bytes.
- Labels bind revision `01fd54237227daff7d0b83064825dd004c9fa1f6`, title `Parker OI11R6V-A6 human fidelity review candidate`, version `22.04`, and created date `2026-09-03T00:00:00Z`.

An attempted OCI-tar export exhausted `/tmp` quota after BuildKit computed the same application manifest/config layers; the incomplete file was removed and is not identified as an artifact. A6 requires no preserved archive. The locally loaded, fully inspectable single-platform image above is the sole deployment candidate.

## Candidate configuration and composition verification

Candidate bytecode contains `FileSystemHumanFidelityReviewStorage`, `FileSystemHumanFidelityGovernanceAudit`, `HumanFidelityReviewRecordingPermissionPolicy`, `DefaultGovernedHumanFidelityReviewRecordingService`, and `HumanFidelityReviewExactTargetRegistrar`. The embedded config loader contains exactly:

- `PARKER_HUMAN_FIDELITY_REVIEW_STORAGE_ROOT`;
- `PARKER_HUMAN_FIDELITY_GOVERNANCE_AUDIT_STORAGE_ROOT`.

Rendered Compose configuration maps `/mnt/parker-data/parker/human-fidelity-reviews` to `/data/human-fidelity-reviews` and `/mnt/parker-data/parker/human-fidelity-review-audit` to `/data/human-fidelity-review-audit`. There is no public review-recording endpoint.

## Host-root assessment and A7 prerequisite

Both production host paths are **ABSENT**:

- `/mnt/parker-data/parker/human-fidelity-reviews`;
- `/mnt/parker-data/parker/human-fidelity-review-audit`.

The parent is ext2/ext3, owner/group `steve:steve` (`1000:1000`), mode `0775`. Existing comparable governed roots use owner UID `999`, group `parker-store-writers` (GID `1001`), mode `2775`; the candidate runtime is UID/GID `999:999`. Therefore A7 must separately authorize and create both roots before deployment, using the established governed-root protection (UID 999 ownership with the established writer group/setgid mode), and verify runtime read/write access. A6 did not create or modify them.

## Isolated candidate startup

The exact candidate was started with Docker network mode `none`, no port bindings, and tmpfs-only test stores. Logs recorded `Runtime starting` then `Runtime started`; both composed review/audit roots were present and writable. The container ran image `sha256:51eff5a7060b478ec66b9ad6e42b56b4ec920d142a984b6e5e7e13dce56f89f5`, restart count zero. It was stopped with SIGTERM and logged `Shutdown signal received`, `Runtime shutting down`, and `Runtime stopped`; the temporary container was removed. No production volume was mounted.

## Exact R6 offline regression

The production-equivalent A5 test preserved `HUMAN_REVIEWED_WITH_DISCREPANCY`, exactly two MATERIAL location-bound occurrences (page 1 and page 5, `Kellee` to human-resolved `Kellec`), one non-authoritative systematic pattern, UNKNOWN technical cause, exact duplicate `AlreadyRecorded`, and restart-durable canonical equality. No real R6S state was recorded and no provider dependency was exercised.

## Production and governed-state verification

Production remained container `7d51a0c2b3c499cee97818c04c8599351cc03c6b515e5ff0358eaa95dfef62fc`, image/index `sha256:55b4f29b4e8f30b80528fda075c7936a968ae98a0b6b54e55b536e9fb9d9ac9c`, source `d45efbee348b842340616a6a73831ef130086d90`, and runtime JAR `71f154b230a5ce318915f7fdc66b24ad11393c0112e5f76a1a9c289255c3815a`. It remained running, restart count zero, with established startup/readiness PASS.

Governed counts remained evidence 29, manifests 29, provider attempts 10, provider state 8, derivative generations 23, and derivative content 21; all established governed identities/hashes remained unchanged. The new production roots remained absent. Governed production-store delta was **0**.

OpenAI calls, Claude calls, other provider calls, retries, and external evidence egress were **0 / 0 / 0 / 0 / 0**. The isolated build/startup used no evidence and no provider path.

## Deployment boundary and verdict

Deployment has **NOT** occurred. Production was not restarted or replaced. No production authorization, real R6S review, effective-review projector, eligibility integration, retrieval integration, correction capability, Gap #54 work, or Sequence B capability was created.

A7 prerequisites are: explicit owner acceptance of every exact candidate identity above; explicit authorization to create/protect the two absent host roots; verification of their runtime UID access; then deployment of exactly the accepted image without rebuild or substitution.

**A — EXACT A5 CANDIDATE ARTIFACT BUILT AND READY FOR OWNER DEPLOYMENT DECISION**
