# OI11R6V-B2 — Exact Human-Corrected Representation Production Candidate and Owner Gate

## Status

BUILT AND VERIFIED — PENDING EXPLICIT OWNER DEPLOYMENT ACCEPTANCE

## Exact source and precheck

- Branch: `main`.
- Source commit: `bb4e1602a550b82476999d0f8bb0c024de11ed70`.
- Starting HEAD/upstream: exact and equal.
- Starting worktree: clean.
- B1 report: present.
- Accepted B1 full-suite gate: 262 suites, 3,405 tests, 18 skipped, zero failures/errors.

The full suite was not repeated. No source, dependency, architecture, test, or production state was changed during candidate construction.

## Exact distribution build

The established bounded offline production distribution command was:

`PARKER_BUILD_COMMIT=bb4e1602a550b82476999d0f8bb0c024de11ed70 ./gradlew clean installDist --offline --no-daemon -Dorg.gradle.jvmargs=-Xmx4g -Dkotlin.daemon.jvm.options=-Xmx4g`

It completed successfully.

- Runtime JAR: `build/libs/parker-platform-0.8.0-runtime-complete.jar`.
- Runtime JAR SHA-256: `dfb88aa601986b57c184aa5e8341226a6627be2fe4f482fa4f838dae99dc882b`.
- Runtime JAR size: `5,057,534` bytes.
- Installed-distribution JAR: byte-identical.
- Manifest source identity: `Parker-Source-Commit: bb4e1602a550b82476999d0f8bb0c024de11ed70`.

## Exact image build and identity

The established isolated-context image procedure copied only the exact installed distribution and existing `tools` into a fresh temporary context and used the already-local accepted base `sha256:d33a5a47f8a540bf11375c2fd373d5bf3257f36da5f0f4afb444bbf3ce46f9cb`.

Build command:

`docker buildx build --network=none --pull=false --platform linux/amd64 --provenance=false --file /tmp/oi11r6v-b2-context.wSjfnS/Dockerfile --load --tag parker-platform:oi11r6v-b2-bb4e160 /tmp/oi11r6v-b2-context.wSjfnS`

- Image ID / local image-manifest digest: `sha256:e70aa02fcac91e7528d109c3b64e1cccc9bc2327a2b538cc2a6a0da6e1b3043b`.
- OCI config digest: `sha256:42c4eb65de35bb9b2aea5c4d696905ae322e8de98c8bbf987257fde335e9fc5e`.
- Created: `2026-09-04T02:59:12.621857867Z`.
- Platform: `linux/amd64`.
- Runtime user: `parker` (established runtime UID/GID supplied by the accepted local base).
- Entry point: `/opt/parker/bin/parker`.
- Image size: `1,032,250,574` bytes.
- Revision label: `bb4e1602a550b82476999d0f8bb0c024de11ed70`.
- Title: `Parker OI11R6V-B2 human-corrected representation candidate`.

A stopped temporary container was used solely to extract the final image JAR. Its SHA-256 was exactly `dfb88aa601986b57c184aa5e8341226a6627be2fe4f482fa4f838dae99dc882b`, and its manifest retained the exact B1 source commit. The temporary container was removed.

## Minimum candidate capability check

Final-image bytecode and metadata contain:

- immutable human-corrected representation/proposal/acceptance contracts;
- deterministic version-1 corrected-representation codec;
- create-once filesystem corrected-representation storage;
- narrow append-only correction audit;
- exact purpose `document-ingestion.human-transcription-correction` and its owner/exact-target policy;
- governed correction service and canonical readback path;
- corrected-representation retrieval and source-confirmed eligibility evaluator;
- exact-target registrar and production `ParkerRuntime` composition wiring;
- existing human-fidelity review contracts and immutable ordinary region provider representation support.

Correction creation resolves local canonical provider and human-review facts. It has no OpenAI, Claude, OCR, retranscription, retry, or external-reasoning provider dependency.

## Minimal R6 offline smoke

Only the existing focused test `DefaultGovernedHumanCorrectionServiceTest.exact R6 correction converges create-once and remains durable after restart` was selected. The first invocation encountered the known host Kotlin compiler heap ceiling after the clean build. The unchanged test was rerun with Parker's established bounded 4 GiB Gradle/Kotlin settings and passed.

Verified result:

- provider representation: `Kellee`, immutable and unchanged;
- human source resolutions: `Kellec` at the two exact reviewed locations;
- corrected representation: `Kellec` at those two locations;
- corrections: exactly two location-bound applications;
- unaffected content: unchanged;
- exact duplicate: idempotent;
- restart readback: durable and identical;
- provider derivative source-confirmed eligibility: `DENIED — MATERIAL_DISCREPANCY`;
- corrected representation source-confirmed eligibility: `ELIGIBLE`.

## Production and provider boundary

No deployment, production restart, production authorization, production corrected representation, or governed production-state mutation occurred. In accordance with the owner's direction, no repetitive Docker/readiness check of the already-established production baseline was performed. Production governed-state delta is zero.

Provider accounting for B2:

- OpenAI calls: 0;
- Claude calls: 0;
- other provider calls: 0;
- retries: 0;
- external evidence egress: 0.

## Owner gate

This report records the exact candidate identities only. Deployment is not authorized by B2. Steven Francis McTague must separately accept the exact source, JAR, image/manifest, and OCI config identities above before deployment.
