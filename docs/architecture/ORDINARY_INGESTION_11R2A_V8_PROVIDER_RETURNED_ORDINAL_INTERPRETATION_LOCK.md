# OI11R2A V8 `provider_returned_ordinal` interpretation lock

## Verdict and boundary

Accepted V8 `provider_returned_ordinal` is provider-reported forensic/audit metadata only. Governed
request-region identity associates returned content, and Parker's deterministic request-region order
controls reconstruction, page/document order, constituent order, and canonical evidentiary order.
Ordinal variation cannot reorder evidence or, by itself, invalidate an otherwise valid V8 response.
Strict identity validation remains fail-closed. This locks existing accepted semantics; it does not
change runtime behavior, schema, capability identity, acquisition semantics, literal semantics,
uncertainty semantics, request shaping, provider/model configuration, or production.

All work occurred directly on Ubuntu host `parker` in `/home/steve/parker-platform`. No Windows,
WSL, OneDrive, remote source staging, alternate checkout, external development environment, provider
call, retry, production promotion, deployment, authorization, or execution was used.

## R2 stop reason and historical review

OI11R2 stopped before source changes because its requirements could have been read to make equality
between provider ordinal and Parker request order a new V8 validity condition. That would have
promoted forensic metadata into governed ordering authority after fidelity acceptance.

The review examined:

- `ORDINARY_INGESTION_10R3_MULTI_REGION_SEMANTIC_ANCHOR_FORENSIC_ANALYSIS.md`;
- `ORDINARY_INGESTION_10R4_REQUEST_REGION_V8_TRUTHFUL_CONTRACT_HARDENING.md`;
- `ORDINARY_INGESTION_10R6_V8_FROZEN_REQUEST_DIGEST_RECONCILIATION.md`;
- `ORDINARY_INGESTION_10R7_REQUEST_REGION_V8_FIDELITY_ACCEPTANCE_CONTINUATION.md`;
- `ORDINARY_INGESTION_11R1P_UBUNTU_ONLY_PROVENANCE_REMEDIATION.md`;
- `OrdinaryRequestRegionV8TruthfulContract.kt`, including the codec, schema derivation, validator,
  parsed result, derivative binder, and capability digest;
- `OrdinaryRequestRegionV8TruthfulContractTest.kt`, the V8 acceptance harness, replay-isolation
  regression, and `scripts/verify-v8-replay-isolated.sh`.

OI10R4 explicitly classifies `provider_returned_ordinal` as forensic. The accepted V8 schema requires
the integer field and bounds it to 1..32, but establishes no equality-to-request-position,
monotonicity, or uniqueness constraint; repeated in-range values are schema-valid. The validator
looks up each returned block by governed `request_region_id`, checks its governed page, requires the
exact complete ID set, and rejects missing, duplicate, or unknown IDs. It preserves blocks in provider
presentation order and preserves each ordinal unchanged. The binder associates blocks by ID and then
maps over `request.regions`, producing `blocksInParkerOrder`. It neither reads nor rewrites the
ordinal. Derivative blocks and canonical derivative digest omit the forensic ordinal. OI10R7 fixture C
records that Parker reconstructed by frozen request order, not provider ordinal, with all five
source-order witness groups passing.

No reviewed evidence makes ordinal equality, monotonicity, uniqueness, or provider presentation order
a governed V8 validity or source-order requirement. The accepted evidence therefore supports the
lock and does not contradict it.

## Governed interpretation

`provider_returned_ordinal` belongs to **FORENSIC PROVIDER METADATA**. Governed request-region ID and
Parker deterministic request order belong to **GOVERNED IDENTITY / ORDERING AUTHORITY**.

The ordinal is preserved truthfully for audit and is never silently corrected. It cannot establish or
alter request-region identity, Parker request order, page order, constituent order, document source
order, literal reconstruction, or canonical evidentiary ordering. Provider block presentation order
also cannot override Parker order. Canonical, reversed, non-monotonic, and repeated schema-valid
ordinal values are valid when all other V8 requirements hold. Ordinal mismatch alone is not a
rejection. Missing expected identity, duplicate governed identity, unknown identity, page mismatch,
or an ordinal offered in place of valid identity remains a rejection.

There is no fuzzy matching, inferred identity, ordinal correction, ordinal-based reordering, or other
heuristic repair. Request-region identity validation remains strict and fail-closed.

## Semantic and capability decisions

- Accepted V8 acquisition semantics changed: **NO**. The lock preserves the accepted distinction
  between provider forensic reporting and Parker-authoritative deterministic identity/order.
- Capability digest changed or needs to change: **NO**. It remains
  `c0479979720455d2de3fc9861eeb5dee323a4770bdb15f807af611ad426f9ec0`.
- V8 schema, wire, profile, adapter/parser, processing identity, instruction, provider/model, request
  shaping, uncertainty model, and literal semantics changed: **NO**.
- Runtime behavior changed: **NO**. Only a focused regression test and this record were added.

## Focused regression

`OrdinaryRequestRegionV8TruthfulContractTest.provider ordinal is preserved forensic metadata while
governed identity and Parker order remain authoritative` uses an existing provider-free fixture and
passed. It proves:

- canonical, reversed, non-monotonic, and repeated schema-valid ordinals are accepted;
- parsed provider order and every ordinal are preserved unchanged;
- reversed provider presentation and ordinal variation cannot alter Parker request, page, constituent,
  canonical derivative order, derivative content, or canonical digest;
- missing, duplicate, and unknown request-region IDs reject, including when an ordinal appears usable
  as a substitute or repair.

Targeted result: PASS. Test commit: `d1415a6` (`test(ingestion): lock forensic v8 provider ordinal
semantics`). No runtime source changed.

## Ubuntu-native replay

The existing isolated foreground wrapper ran sequentially for A, B, and C under a fresh `/tmp` result
root. All three replay payloads passed and reproduced their accepted raw, structured, response,
assessment, and canonical derivative identities. No provider path was selected.

The immutable OI11R1P replay result hashes remain:

- A: PASS; `2aa58fdbc6e76bdd0c3c0b373c00098303eb64cf42d9eefefddf1f53f66cf2bb`.
- B: PASS; `9da548856d2c6d7808422010d2c8e8999b0785a7a52efb05c2e550e0f51539a9`.
- C: PASS; `6b4884b10fa6e260b9d55fca0e88f9d2c6efc615b6836911273d61d7fde48377`.

Fresh OI11R2A result envelopes have intentionally different whole-file hashes—`519e20ec...765b`,
`36afae56...7ab3`, and `f191080a...c01c`—because the isolation contract includes the fresh
invocation ID in the checksum-protected envelope. This is attribution metadata, not a replayed
evidence difference. The accepted typed evidence file was read directly and remains exactly
`34ec3c703aacb754c45fa58ddf941d7368e2b4cc2e373cb412eb99c4de30902b`.

Historical provider calls represented: 3. New OpenAI calls: 0. Retries: 0. Claude calls: 0. No
background watcher was created; after completion no replay wrapper or acceptance-harness Gradle
process remained.

## Complete verification and production preservation

The full suite passed: 246 suites, 3,287 tests, 0 failures, 0 errors, 17 skipped. All 246 generated
JUnit XML files recorded hostname `parker`. `git diff --check` passed.

Production remained unchanged:

- image: `sha256:fdb583d16d99a58d13983046b2ad8b936014ead6b6c22cdf0d670b895b071521`;
- container: `281bba01fa82ddd4a172a424688845ea180a6dfe28eb4ae2aebd9c064ecd68ca`;
- state: running; restart count: 0;
- governed stores: attempts 4, provider-state 2, authorities 1, generations 21, content 19,
  capability acceptances 6, owner authorizations 5 (`4 / 2 / 1 / 21 / 19 / 6 / 5`);
- production mutations, promotions, deployments, new authorizations, executions, and provider-state
  writes: 0.

## Exact next step

Resume OI11R2 V8 durable provider-state and guarded-execution path convergence, using the locked
forensic-only `provider_returned_ordinal` interpretation and remaining entirely on the authoritative
Ubuntu Parker environment.
