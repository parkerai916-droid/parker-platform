# OI11R6V-B3R1 — Literal Region Offset Mapping Correction and Replacement Candidate

## Status

CORRECTED AND VERIFIED — REPLACEMENT CANDIDATE PENDING EXPLICIT OWNER ACCEPTANCE

## Scope and starting state

- Starting branch: `main`.
- Starting HEAD/upstream: `2b5ecb448d6ee0bbd1d505800a4ee3128632c8c9`, exact and equal.
- Starting worktree: clean.
- Corrected source commit: `6513435f846f0416a00d5f95137b02368c5a551c`.
- Scope: the B3 literal-region offset defect only.

Sequence A, the canonical human review, its discrepancy offsets, the provider derivative, authorization, eligibility, provenance, and provider-budget semantics were not changed.

## Defect and minimum correction

The persisted request-region V8 transcription block is a six-field envelope separated by Unicode unit separator (`U+001F`): region identity, page number, literal transcription text, status, uncertainties, and warnings. Canonical human-review code-point offsets address the literal transcription text, not that envelope.

`DefaultGovernedHumanCorrectionService` now:

1. decodes the established six-field envelope fail closed;
2. verifies the envelope region identity against the corresponding composite derivative-region binding;
3. resolves exactly one envelope by the reviewed derivative region identity and page number;
4. requires the reviewed transcription block index to be zero;
5. applies the stored half-open Unicode code-point range only to the literal-text field;
6. verifies the exact provider-before substring at that range;
7. substitutes only the accepted human source value; and
8. reconstructs the envelope with every non-literal field unchanged.

There is no fuzzy matching, global replacement, inferred rebasing, or provider-content mutation. Malformed envelopes, ambiguous/missing regions, page mismatches, incorrect substrings, invalid code-point ranges, and overlapping applications fail closed.

## Regression verification

Focused verification covered:

- literal-text-relative offsets in real V8 envelopes;
- composite region-binding resolution without treating the envelope as literal text;
- non-BMP prefixes and Unicode code-point rather than UTF-16 indexing;
- rejection of an incorrect literal provider-before substring with zero corrected-state publication;
- exact page 1 and page 5 `Kellee` to `Kellec` applications;
- unchanged envelope metadata and unchanged pages 2–4;
- unchanged provider representation;
- create-once duplicate behavior, restart durability, and corrected-representation source-confirmed eligibility;
- corrected-representation durability and production-composition regression coverage.

Focused tests passed with zero failures/errors. The dedicated service suite contained 7 tests.

The full suite was run once as the convergence gate and passed:

- suites: 262;
- tests: 3,407;
- skipped: 18;
- failures: 0;
- errors: 0.

## Exact R6 result

- Corrections applied: exactly 2 location-bound occurrences.
- Page 1: `Kellee` → `Kellec`.
- Page 5: `Kellee` → `Kellec`.
- All other literal content: unchanged.
- All non-literal envelope fields: unchanged.
- Provider representation: immutable and unchanged.
- Corrected representation: create-once, retrievable, and source-confirmed eligible.

The minimum post-build existing R6 corrected-representation smoke test passed. It verified create-once convergence, exact page 1/page 5 results, provider immutability, durable restart readback, and corrected-representation eligibility.

## Replacement artifact

The established bounded offline production distribution command was:

`PARKER_BUILD_COMMIT=6513435f846f0416a00d5f95137b02368c5a551c ./gradlew clean installDist --offline --no-daemon -Dorg.gradle.jvmargs=-Xmx4g -Dkotlin.daemon.jvm.options=-Xmx4g`

It completed successfully.

- Source commit: `6513435f846f0416a00d5f95137b02368c5a551c`.
- Runtime JAR: `build/libs/parker-platform-0.8.0-runtime-complete.jar`.
- Runtime JAR SHA-256: `e7b0a132dde08a40991a10e1d2027924a71c692f44adae53c135116d09f8bc0f`.
- Runtime JAR size: 5,064,566 bytes.
- Installed-distribution JAR: byte-identical.
- Embedded manifest source: `Parker-Source-Commit: 6513435f846f0416a00d5f95137b02368c5a551c`.

The established isolated-context image command was:

`docker buildx build --network=none --pull=false --platform linux/amd64 --provenance=false --file /tmp/oi11r6v-b3r1-context.bSiU0K/Dockerfile --load --tag parker-platform:oi11r6v-b3r1-6513435 /tmp/oi11r6v-b3r1-context.bSiU0K`

- Image ID / local image-manifest digest: `sha256:be54da37b6ccd5318174ea9a02237cdcfdfe5238615d8e261f5de091d05e1247`.
- OCI config digest: `sha256:57c11dfcb48d7ed6e62e1694c8dd726c38b91ddffeea7311097518a3a0ea6978`.
- Base image: existing local accepted `sha256:d33a5a47f8a540bf11375c2fd373d5bf3257f36da5f0f4afb444bbf3ce46f9cb`.
- Created: `2026-09-04T04:17:13.085345419Z`.
- Platform: `linux/amd64`.
- Runtime user: `parker`.
- Entry point: `/opt/parker/bin/parker`.
- Image size: 1,032,256,682 bytes.
- Revision label: `6513435f846f0416a00d5f95137b02368c5a551c`.

A stopped temporary container was used only to extract the final image JAR. Its SHA-256 exactly matched `e7b0a132dde08a40991a10e1d2027924a71c692f44adae53c135116d09f8bc0f`, its manifest retained the exact corrected source commit, and the temporary container was removed.

## Production and provider boundary

The candidate was not deployed. Production was not restarted or mutated. The established rolled-back production baseline remains image `sha256:26a503564698b4ac248cb3e9d94ceba7813b713523dd9b995e73cbf922267895`, container `755bc826d4e9564a204b18e97e95b7067cc40bced4d9fb4cfbf41bb2f59bfc3c`, readiness `PASS`, restart count 0. No production identity/readiness operation was needed for this offline correction/build unit.

- Production governed-state delta: 0.
- OpenAI calls: 0.
- Claude calls: 0.
- Other provider calls: 0.
- Retries: 0.
- External evidence egress: 0.

Deployment requires explicit owner acceptance of the exact source, JAR, image/manifest, and OCI config identities recorded above.
