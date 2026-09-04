# OI11R6V-A8D — Owner-Accepted Final Sequence A Artifact Deployment

## Verdict

**A — EXACT FINAL SEQUENCE A ARTIFACT DEPLOYED AND VERIFIED**

## Starting state, governance, and owner acceptance

The unit started on host `parker`, branch `main`, at clean HEAD/upstream `d05230f967094f1efe8624f1b3a1466cebfd654a`. The frozen R6T Scope Lock SHA-256 was `f90c9fc654136ea5e92723a1704ad58108ab0f8a9e73a401e8039d3210e4cd2a`; the frozen R6V Implementation Plan SHA-256 was `86f7b27095a6b80b2618556797a877a21b097f76c3c9ba2d91e235b90c395d1d`.

Steven Francis McTague explicitly accepted deployment of exactly:

- Source: `e2e824c062c94ffe5b8b75a387a753de7d2f72ce`
- Runtime JAR SHA-256: `d78c99a389ba3a75e868d007ac13f0678f84ac25e46e7aeca41a513f510865e2`
- Image ID/local manifest digest: `sha256:26a503564698b4ac248cb3e9d94ceba7813b713523dd9b995e73cbf922267895`
- OCI config digest: `sha256:7427e35399b1a9d62b7768c70c9612727cde4f5de2239569012eceae836a5768`

The local image, revision label, stopped-container extracted JAR, and candidate report all matched before production mutation. No rebuild, pull, substitution, or source change occurred.

## Previous production and rollback identity

The fixed rollback target was:

- Container: `ccf93adcaf7b37e12eb5d8f93c7419d588d713c03881420b49021e5dd8e1b707`
- Image: `sha256:51eff5a7060b478ec66b9ad6e42b56b4ec920d142a984b6e5e7e13dce56f89f5`
- Source: `01fd54237227daff7d0b83064825dd004c9fa1f6`
- Runtime JAR SHA-256: `dc04f7c3498607f35b721348087389f7b1c15e9064ed8a98c5e11c765b2b981c`
- Readiness/restarts: PASS / `0`

Rollback was prepared as the established four-file Compose invocation with a final temporary override pinning this exact prior image and identity environment, followed by `up -d --no-build --pull never --no-deps --force-recreate parker`. Rollback was not required.

## Exact deployment procedure

A temporary final override pinned only service `parker` to `parker-platform@sha256:26a503564698b4ac248cb3e9d94ceba7813b713523dd9b995e73cbf922267895` and set the deployed/source/production identity values to the accepted image and source. `docker compose ... config --images` resolved exactly that digest.

Deployment used the established stack:

```text
PARKER_BUILD_COMMIT=e2e824c062c94ffe5b8b75a387a753de7d2f72ce docker compose \
  -f docker-compose.yml \
  -f /home/steve/.config/parker/docker-compose.fa-a1r.yml \
  -f /home/steve/.config/parker/docker-compose.openai-enablement.yml \
  -f /tmp/oi11r6v-a8d-exact-image.yml \
  up -d --no-build --pull never --no-deps --force-recreate parker
```

Only `parker-runtime` was recreated.

## Deployed production identity and startup

- Container: `e4d2c427776a4a8c103bfb8847ed0923acbb215c55b68803f94df89a0e8ae751`
- Image: `sha256:26a503564698b4ac248cb3e9d94ceba7813b713523dd9b995e73cbf922267895`
- Source label: `e2e824c062c94ffe5b8b75a387a753de7d2f72ce`
- Runtime JAR SHA-256: `d78c99a389ba3a75e868d007ac13f0678f84ac25e46e7aeca41a513f510865e2`
- Status: running
- Readiness: PASS (`Runtime starting`, `Runtime started`, and owner HTTP listener startup recorded)
- Restart count: `0`
- Started: `2026-09-04T01:34:45.249681475Z`

The accepted candidate contains the A5 recording composition, A8A projection/eligibility capability, and A8B retrieval/status presentation integration. No A9 governed retrieval was invoked in this deployment unit.

## Persistent mounts and canonical review preservation

The existing roots were not recreated or altered. They remain mounted read-write:

- `/mnt/parker-data/parker/human-fidelity-reviews` -> `/data/human-fidelity-reviews`
- `/mnt/parker-data/parker/human-fidelity-review-audit` -> `/data/human-fidelity-review-audit`

Canonical review state remained exact:

- Review ID: `review-3cf3186ca166acb0f4b6331ca574926dc874225247b296fb972666504992ea6e`
- Stored-record SHA-256: `13e6f5e285d95e19c0926821b63422486e005d22ee484feb70a6b54635046106`
- Review records: `1`
- Audit facts: `3`
- Review state: `HUMAN_REVIEWED_WITH_DISCREPANCY`

Startup created no review or audit fact.

## Historical R6 preservation

The exact source evidence remained SHA-256 `5d73e6e55d3491e94aa9d6c02a0735572f9840fe8185a71546dba9f2258e237e`. Preparation `85054cc742813d9b05339d07bce77d8665210b7c6e851fe9470b68a33c9bed8f`, execution `ordinary-exec-3c2bf685-d6c2-44e0-acf8-0224d92fd976`, provider state `2b1fbe06ebee0b7a3fdb618159c6987fa713976d7bfd2732b9048b50f11df3a7`, raw response SHA `4706c24b8b0b83675a8ded1165f316229fa61a92bff4d8fe0a16c1d7d50cfb4a`, response ID `resp_04aa0adc3e021174016a980c0c891487d09764395f58adef7b`, and model `gpt-5.6-sol` remain the established historical facts.

The single R6 provider derivative remained exact:

- Generation: `region-f0df253d73500fef1dd5bbca186632c6be7f0a94faf10310e07cccb8fb673bc6`
- Generation SHA-256: `9fb18b02db5ac55e5d446cd48ebc619de929c4596f94d2a11fba1a07da71af14`
- Content SHA-256: `18a6ed08a4729350027d3140dc0f07dd49d32c04aa45f9e3e9558df5d007c4eb`

Pre/post counts were unchanged: derivative generations `23`, derivative content `21`, provider attempts `10`, and provider-state facts `8`. The historical real-document provider budget remains maximum `1`, consumed `1`, retries `0`; no second R6 derivative or provider attempt was created.

## Governance boundary

OpenAI calls, Claude calls, other provider calls, retries, and external evidence egress were `0 / 0 / 0 / 0 / 0`. No review mutation, correction, corrected representation, implementation, Gap #54, or Sequence B work occurred.

A9 production semantic verification was deliberately not performed. The next separate unit must verify governed production retrieval, human-fidelity status, source-confirmed eligibility, and immutability through the deployed path.
