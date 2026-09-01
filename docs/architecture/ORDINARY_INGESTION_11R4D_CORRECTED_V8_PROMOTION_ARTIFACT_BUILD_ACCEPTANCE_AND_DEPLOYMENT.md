# OI11R4D — Corrected V8 Promotion Artifact Build, Acceptance and Deployment

## Verdict

`PASS — OI11R4D COMPLETE`

Steven explicitly accepted the exact candidate before deployment. No V8 capability-acceptance record was created and no provider call occurred.

## Source and tests

The artifact was built from the clean detached checkout at `/tmp/parker-oi11r4d-build`, commit `c304fdeff6bd89f96e8397ef4192e9f83b41cb93` (40 characters), which contains the OI11R4C endpoint/codec correction. Focused correction/routing tests and the full `./gradlew test` gate passed with zero failures and zero errors.

## Accepted artifact

- JAR SHA-256: `724a6adfaa11faedf3cf828f5c324c527ea14644b4f4f62fae21b33a46adc722`
- Embedded `Parker-Source-Commit`: `c304fdeff6bd89f96e8397ef4192e9f83b41cb93`
- Image ID/digest: `sha256:e161c65c98c0572cb0981be652adfa029406e0104f843b1359a70b0031398673`
- OCI manifest: `sha256:5a26f0fb2c875308e5ae76b194c0ba4a2827fa5816d5f0f6f2eed9fa2d389796`
- OCI config: `sha256:f24e4aa730b1cdabf27f777c0a517b98ab340b7bd61509f4883a0c2aab4f9048`
- Capability: `ordinary-external-request-region-transcription-v8`; digest `c0479979720455d2de3fc9861eeb5dee323a4770bdb15f807af611ad426f9ec0`
- Provider profile: `openai-fidelity-first-transcription-v1`; state `ACCEPTED`; SHA-256 `3038538d53b98595631c76325062688b40c449d512bb94cae17be2e7f0d6e956`

## Static correction proof

The candidate JAR contains `OwnerEvidenceHttpServer`, `ParkerRuntime`, `RuntimeReadinessDiagnostic`, and distinct `legacyPromotion`/`v8Promotion` composition classes. The corrected source routes the legacy and V8 capability families to their existing coordinators and retains fail-closed unknown/cross-family handling. Six legacy capability-acceptance records remained present; no V8 production acceptance record was created.

## Preservation and acceptance

Archive: `/mnt/parker-data/parker/replacement-candidates/oi11r4d-corrected-c304-20260901.tar`; SHA-256 `c22afd1e6b27ca5230c5f13edad1bd8f96b69cce3fbfadb3b35c4be37a36e1ca`; size 867,034,112 bytes; mode 600. The immutable acceptance record is `/mnt/parker-data/parker/replacement-candidates/oi11r4d-artifact-acceptance-e161c65c-v1.json`, SHA-256 `8f9f45e2dcdf03b15ebc2b8de271344f85c3538a2970988ea2a45d4831caa2e3`.

## Deployment

Rollback preimage of the deployment override was preserved at `/home/steve/.config/parker/docker-compose.fa-a1r.yml.oi11r4d-preimage` (SHA-256 `8dc3c540bfccd7ec739bf00fbb53843f58b409d9168f3b782a6699fe876817f2`). The final Compose render binds image `parker-oi11r4d-c304@sha256:e161c65c98c0572cb0981be652adfa029406e0104f843b1359a70b0031398673`, source/production/build commit `c304fdeff6bd89f96e8397ef4192e9f83b41cb93`, and has render SHA-256 `be95e2f50311e66d1996e5d3b06b12cab0ffb83bbcc48434ab023bc44b7d0ce8`.

Exact command:

```text
PARKER_BUILD_COMMIT=c304fdeff6bd89f96e8397ef4192e9f83b41cb93 docker compose -f /home/steve/parker-platform/docker-compose.yml -f /home/steve/.config/parker/docker-compose.openai-enablement.yml -f /home/steve/.config/parker/docker-compose.fa-a1r.yml up -d --no-build --pull never --no-deps --force-recreate parker
```

Previous container used the earlier accepted image. The new running container is `35549524b25256ec7d947cd814cd25d52b64a6f67268a607ef62aa175e0246e4`, running image `sha256:e161c65c98c0572cb0981be652adfa029406e0104f843b1359a70b0031398673`, status `running`, restart count `0`. Startup logs reported `Runtime starting` followed by `Runtime started`; no readiness or capability-parser rejection occurred.

## Integrity and boundaries

Post-deployment store counts were unchanged: `4 / 2 / 1 / 21 / 19 / 6 / 5`. The six legacy acceptance records were unchanged, no V8 production acceptance record exists, and the OI11R4A synthetic source remains preserved. Provider calls were OpenAI 0, Claude 0, external 0, retries 0. No provider execution, evidence ingestion, or external egress occurred. Production is stable on the exact accepted image. Historical compatibility and V8 composition are covered by the accepted artifact's existing tests and static inspection. A separate OI11R4E unit is required for V8 production capability acceptance/promotion; this unit did not perform it.
