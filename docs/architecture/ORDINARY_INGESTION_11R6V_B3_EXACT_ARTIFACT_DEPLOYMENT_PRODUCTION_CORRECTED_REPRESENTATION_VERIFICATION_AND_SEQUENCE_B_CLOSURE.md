# OI11R6V-B3 — Exact Artifact Deployment, Production Corrected Representation Verification and Sequence B Closure

## Status

FAILED CLOSED — ROLLED BACK — SEQUENCE B NOT CLOSED

## Verdict

**B — PRODUCTION CORRECTED REPRESENTATION FAILED CLOSED**

## Owner acceptance and starting gate

Steven Francis McTague explicitly accepted deployment of exact source `bb4e1602a550b82476999d0f8bb0c024de11ed70`, runtime JAR SHA-256 `dfb88aa601986b57c184aa5e8341226a6627be2fe4f482fa4f838dae99dc882b`, image/local manifest `sha256:e70aa02fcac91e7528d109c3b64e1cccc9bc2327a2b538cc2a6a0da6e1b3043b`, and OCI config `sha256:42c4eb65de35bb9b2aea5c4d696905ae322e8de98c8bbf987257fde335e9fc5e`.

The unit started on `main` at clean HEAD/upstream `6fc6de70d25d42b611205c68b11bd6c8914732be`. The local image and revision label matched. Production was running the previously accepted Sequence A image `sha256:26a503564698b4ac248cb3e9d94ceba7813b713523dd9b995e73cbf922267895` with readiness PASS and restart count zero.

## Governed storage-root preparation

The two B1-configured correction roots were absent. The first bounded root-creation invocation accidentally retained the Parker image entrypoint and was rejected for missing runtime configuration before mutation; both roots remained absent. The corrected network-disabled invocation explicitly selected `sh`, asserted both paths absent and non-symlinked, and created only:

- `/mnt/parker-data/parker/human-corrected-representations`;
- `/mnt/parker-data/parker/human-correction-audit`.

Both were established empty with UID 999, GID 1001, and mode 2775. No semantic record was created.

## Exact deployment

The accepted image was deployed through the established four-file Compose stack with a final exact-image override and:

`up -d --no-build --pull never --no-deps --force-recreate parker`

Only `parker-runtime` was recreated. The deployed result was:

- Container: `d65f5af759f60d46b62d21cb4479f2f6a0a36cee813218a3583921ed48e65c9f`.
- Image: `sha256:e70aa02fcac91e7528d109c3b64e1cccc9bc2327a2b538cc2a6a0da6e1b3043b`.
- Source label: `bb4e1602a550b82476999d0f8bb0c024de11ed70`.
- Runtime JAR SHA-256: `dfb88aa601986b57c184aa5e8341226a6627be2fe4f482fa4f838dae99dc882b`.
- Runtime status/readiness: running / PASS (`Runtime started`).
- Restart count: 0.
- Both correction roots: mounted read-write at the configured container paths.

There was no rebuild, pull, substitution, or unrelated service recreation.

## Governed correction invocation and fail-closed result

Because B1 intentionally added no public write endpoint, B3 used the established network-disabled one-shot invocation pattern. A temporary runner loaded all Parker service/domain/storage classes from the exact deployed image, registered only the configured owner `user.steve`, exact correction purpose, canonical review, and exact R6 correction target, and called `DefaultGovernedHumanCorrectionService`. It did not construct or edit store files directly.

The first invocation was rejected before authority or mutation because the existing derivative store constructors require writable roots even for retrieval and those two mounts were read-only. Inspection confirmed zero corrected-representation files and zero correction-audit files. The unchanged invocation was retried with the canonical derivative stores mounted read-write, while the service used them only for retrieval.

The governed service returned:

`Failed(reason=INVALID_CORRECTION)`

No corrected representation was prepared or published and no correction audit fact was appended.

## Exact implementation defect

Read-only diagnostic comparison of the canonical provider derivative and canonical human review identified the exact defect:

- The persisted V8 `transcriptionBlocks` entries are envelopes containing the derivative region identity, page ordinal, literal transcription, status, uncertainties, and warnings separated by unit separators.
- Canonical human-review discrepancy spans `[151,157)` on page 1 and `[89,95)` on page 5 are block-local offsets into each region's **literal transcription text**.
- B1 `DefaultGovernedHumanCorrectionService` resolves the correct derivative region but applies each canonical span directly to the full persisted V8 envelope string.
- At those offsets in the envelope, the exact reviewed provider substring is not `Kellee`, so B1's exact-substring validation correctly rejects the request as `INVALID_CORRECTION`.

The B1 offline fixture used plain representative literal blocks and therefore did not expose the envelope-to-literal offset translation defect. Fixing it requires implementation, tests, a new exact candidate artifact, and new owner acceptance. B3 explicitly prohibited implementation, rebuild, or artifact substitution; the defect could not be repaired in this unit.

## Canonical facts preserved

The diagnostic proved the immutable provider representation still contains:

- page 1: `MICHAEL GARY KELLEE`;
- page 5: `MICHAEL GARY KELLEE`.

The canonical human review remains exactly one record with the two location-bound `Kellee` → `Kellec` source resolutions. Corrected representation count is zero. Correction audit fact count is zero. Provider source-confirmed eligibility remains `DENIED — MATERIAL_DISCREPANCY`. No corrected-representation eligibility claim can be made because no corrected representation exists.

## Rollback

Because the accepted artifact could not perform its owner-authorized production purpose, production was restored without rebuild to the exact prior Sequence A artifact:

- Container: `755bc826d4e9564a204b18e97e95b7067cc40bced4d9fb4cfbf41bb2f59bfc3c`.
- Image: `sha256:26a503564698b4ac248cb3e9d94ceba7813b713523dd9b995e73cbf922267895`.
- Source label: `e2e824c062c94ffe5b8b75a387a753de7d2f72ce`.
- Runtime JAR SHA-256: `d78c99a389ba3a75e868d007ac13f0678f84ac25e46e7aeca41a513f510865e2`.
- Runtime status/readiness: running / PASS.
- Restart count: 0.
- Canonical R6 review records/audit facts: 1 / 3.
- Corrected representations/correction audit facts: 0 / 0.

The two empty non-semantic correction roots remain as deployment prerequisites; they contain no governed semantic facts.

## Provider and governance accounting

- OpenAI calls: 0.
- Claude calls: 0.
- Other provider calls: 0.
- Provider retries: 0.
- External evidence egress: 0.
- Provider budget: 1 authorized / 1 consumed / 0 retries, unchanged.
- Provider derivative: unchanged and still exactly one.
- Canonical human review: unchanged and still exactly one.
- Governed semantic-state delta: 0.

Sequence B is **not** accepted, canonical, or closed. A corrected implementation must explicitly map each canonical region-local discrepancy span to the literal-text field within the V8 persisted block envelope, retain exact substring validation, prove convergence against the actual canonical encoded R6 provider content, pass the necessary regression tests, produce a new candidate, and receive separate exact-artifact owner acceptance before another deployment attempt.
