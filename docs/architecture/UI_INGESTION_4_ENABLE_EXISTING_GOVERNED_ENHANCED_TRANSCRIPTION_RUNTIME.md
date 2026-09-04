# UI-INGESTION-4 — Enable Existing Governed Enhanced Transcription Capability in Production Runtime

## Status

ENABLED — DEPLOYED — VERIFIED (readiness only; no execution)

## Scope

Deployment-configuration-only. No source, test, or Compose file in this repository was changed. Continues on clean `main` at HEAD/upstream `d3517b4e17c0e8e31515eb54d14c8755c073b8df`, unchanged throughout this unit.

## Root cause (category B — capability composed, deployment configuration absent)

`ParkerRuntime.buildAndRegisterRuntimeGraph` (`src/composition/ParkerRuntime.kt:551-561`) evaluates enhanced-transcription readiness from three inputs alone: `config.openAiExternalTranscriptionEnabled`, `config.openAiExternalTranscriptionProviderProfilePath`, and `config.openAiApiCredential`. These are loaded from `PARKER_OPENAI_EXTERNAL_TRANSCRIPTION_ENABLED`, `PARKER_OPENAI_EXTERNAL_TRANSCRIPTION_PROVIDER_PROFILE_PATH`, and `PARKER_OPENAI_API_KEY` (`ParkerRuntimeConfig.kt:353-355, 484-489, 577-580`). `OpenAiExternalTranscriptionProviderReadinessEvaluator.evaluate` (`OpenAiExternalTranscriptionProviderProfile.kt:74-75`) returns `Disabled` immediately whenever `enabled` is false — the exact `EnhancedTranscriptionReadiness.Disabled` → `"Enhanced transcription is not enabled in this runtime."` the UI showed.

The repository's `docker-compose.yml` (the file backing the production `parker-runtime` container) never referenced any of these three keys — confirmed by grep across the file and by the running container's actual environment (`docker inspect` showed zero `PARKER_OPENAI_*` variables before this fix). The composition code itself (`ParkerRuntime.kt:1606-1632`, `OwnerUiEvidenceRuntimeAdapter.kt`, `OwnerEvidenceHttpServer.kt`, `Main.kt:145-146`) is fully wired end-to-end with no stub in the production path — this is exactly the capability already proven under Ordinary Ingestion 11R6.

Both prerequisites already existed on the host, already governed, and were never touched by this unit:

- Provider profile `/home/steve/.config/parker/openai-external-transcription.properties` — SHA-256 `3038538d53b98595631c76325062688b40c449d512bb94cae17be2e7f0d6e956`, `acceptanceState=ACCEPTED`, `nextReviewDate=2026-09-26` (not stale) — the exact artifact explicitly accepted by the owner in OI11R3F.
- `PARKER_OPENAI_API_KEY` already present in the deployment-local `.env` (git-ignored), unused only because it was never passed through Compose to the container.
- An established override file, `/home/steve/.config/parker/docker-compose.openai-enablement.yml`, already existed on the host wiring exactly these three variables plus the read-only profile mount — prepared previously but never applied to this deployment lineage.

No new profile, credential, or acceptance was created or altered. No governance gate was bypassed or weakened.

## Fix applied

Deployment configuration only — no rebuild. The already-running exact UI-INGESTION-3A image (`sha256:784aa3c69b8fcf1a52d3648f3fe7590fe458acf40ed0c11b3efda56e77665f24`) was redeployed with one additional pre-existing Compose override layered on top of the existing exact-image pin:

`docker compose -f docker-compose.yml -f <exact-image-pin>.yml -f /home/steve/.config/parker/docker-compose.openai-enablement.yml up -d --no-build --pull never --no-deps --force-recreate parker`

This added exactly:

- `PARKER_OPENAI_EXTERNAL_TRANSCRIPTION_ENABLED=true`
- `PARKER_OPENAI_EXTERNAL_TRANSCRIPTION_PROVIDER_PROFILE_PATH=/run/parker-config/openai-external-transcription.properties`
- `PARKER_OPENAI_API_KEY` (from the deployment-local `.env`, passed through, never printed or committed)
- a read-only bind mount of the already-accepted profile file to that in-container path

No unrelated service was recreated.

## Focused verification

- `OpenAiExternalTranscriptionProviderProfileTest`: 10/10 passed.
- `OpenAiApiCredentialTest`: 5/5 passed.
- `OwnerUiEvidenceRuntimeAdapterTest`: 23/23 passed — this is the layer at which `enhancedTranscriptionReadiness()`/`transcribeExternal()` are exposed to the UI adapter, fully exercising fail-closed and enabled composition behavior.
- `ParkerRuntimeConfigLoaderTest`: 45/49 passed; 4 failures are pre-existing and unrelated to `PARKER_OPENAI_*` config loading (all concern `PARKER_OWNER_HTTP_BIND_ADDRESS`/`PARKER_OWNER_HTTP_PORT`/`PARKER_OWNER_HTTP_TOKEN` interaction, not touched by this unit).
- `OwnerEvidenceHttpServerTest`: 16/100 passed. Root-caused precisely: this large pre-existing file predates the device-pairing session model (commit `25652bb`, "feat(auth): add local owner device pairing"), and the vast majority of its tests still send a stale `Authorization: Bearer $token` header — a scheme the current server no longer checks (`isAuthorised` in `OwnerEvidenceHttpServer.kt` reads only session cookies). Only the small subset of tests already migrated to the current `pairedCookie(harness)` cookie pattern pass. This is pre-existing test debt spanning far beyond enhanced transcription, was not introduced by this unit (no source was changed), and fixing it — a large, unrelated rewrite of dozens of test cases — is explicitly out of this unit's minimum-fix scope. It was not attempted.
- Because the HTTP-layer readiness tests specifically are among the affected stale-auth tests, the actual HTTP endpoint's enabled/fail-closed behavior was instead verified directly against the live redeployed production container (see below), consistent with how prior units already relied on direct HTTP verification of this server.

No source, test, or Compose file was modified; no full suite run was performed (out of scope, per the pre-existing/unrelated finding above).

## Post-deployment verification (production)

- Image: `sha256:784aa3c69b8fcf1a52d3648f3fe7590fe458acf40ed0c11b3efda56e77665f24` (unchanged, exact).
- Embedded source label: `296ff5802670387f44a7950ee6c57899ebf17e7d` (unchanged, exact).
- Runtime JAR SHA-256: `ac5d9217a60d52ccaa2ef1dc4ecba6e75264960c19ae86280fd691bd30399d99` (unchanged, exact).
- Readiness: `Runtime started` and `Owner LAN Evidence Upload HTTP server listening on 0.0.0.0:8080` logged cleanly.
- Restart count: `0`, before and after verification.
- Owner session preservation: the original paired device record (`owner-device-d915d144...`) remained present and `ACTIVE` throughout, untouched by this redeploy.
- Unauthenticated requests still fail closed: root page serves only the pairing page (`200`, no evidence UI), and `GET /owner/evidence/transcription-readiness` returns `401` without a session.
- **Enhanced transcription readiness, checked with a real authenticated session**: a host-admin pairing challenge was issued via the existing `--owner-ui-pairing-admin pair` CLI (the same established, non-logged, single-use mechanism prior units used), completed over `POST /owner/pair`, then `GET /owner/evidence/transcription-readiness` returned:

  `{"status":"READY","message":"Enhanced transcription is available."}`

  Both throwaway verification devices created for this check were revoked immediately afterward via `--owner-ui-pairing-admin revoke`; only the original owner device remains active. No evidence was listed, selected, processed, or transcribed during this check.

## Explicit non-execution statement

No enhanced transcription action was invoked. No evidence document was selected, processed, or transcribed. The verification above queried only the readiness endpoint (`GET /owner/evidence/transcription-readiness`), which performs no provider request.

## Provider accounting

- Provider calls: `0`
- Retries: `0`
- External evidence egress: `0`

## Result

Enhanced transcription is now enabled and reports `READY` in the production runtime, using only pre-existing, already-governed configuration (accepted provider profile, existing credential, established override file). No governance gate, capability acceptance, or authorization boundary was created, weakened, or bypassed. The UI's "Run enhanced transcription" action is ready for owner-triggered use against evidence meeting the existing governed prerequisites; it was not exercised by this unit.
