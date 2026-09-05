# UI-INGESTION-7 — Deploy Fidelity-First Execution Binding and Configure Existing Attempt Ledger

## Status

DEPLOYED — VERIFIED — NO PROVIDER INVOKED

## Owner acceptance recorded

The owner explicitly accepted deployment of the UI-INGESTION-6 candidate and explicitly accepted the known, unresolved structural-fidelity limitation of `openai-fidelity-first-transcription-v1` (per `FA_9_4P_A1_STRUCTURAL_FIDELITY_FAILURE_ANALYSIS_AND_REMEDIATION_PLAN.md`) for operational use. This acceptance does not declare provider output source-faithful, does not supersede that accepted failure finding, and does not remove human fidelity review, correction governance, source-confirmed eligibility rules, or the possibility of a future region-anchored remediation. Nothing in this unit touched any of those controls.

## Deployed candidate (owner-accepted, exact, not rebuilt)

- Source commit: `1e52fac7c18fd914c2e7504050706e6f2a5a3a4b`
- Image ID: `sha256:96a54f465bf6e03e6ffa7233de8385d4b1224dc912784b2fc707f3cd534f6f86`
- Runtime JAR SHA-256: `1493674957b332ba4b31de2c0fe95aa9098c182bf514fd8199df8f11a0df01e0`
- Local tag used: `parker-platform:ui-ingestion-6-1e52fac7` (already built by UI-INGESTION-6; not rebuilt or substituted)

## Attempt-ledger root: reused, not created

`ParkerRuntimeConfigLoader` requires the acceptance-authority root, attempt root, and production commit to be configured together (all three or none) — an existing, established co-requirement, not something new (documented in `FA_9_4P_A1R_PENDING_ACCEPTANCE_EXECUTION_DEPLOYMENT.md`). A first deployment attempt supplying only `PARKER_FIDELITY_FIRST_ATTEMPT_STORAGE_ROOT` was correctly rejected by this exact check and the container crash-looped (restart count reached 9 before being caught); **production was restored to the prior UI-INGESTION-5E image within seconds** and remained down for less than a minute total, with zero provider exposure.

Both required host directories already existed from the historical `FA.9.4P-A1` governed acceptance-execution, correctly owned (`999:parker-store-writers`, matching the container's own uid/gid) and already in active use — the attempt-ledger directory already contained real, unrelated ledger records from the ordinary-region pipeline's own prior attempts. Both were reused exactly as-is; neither was created or altered:

- `/mnt/parker-data/parker/external-transcription-acceptance-authorities` → `/data/external-transcription-acceptance-authorities`
- `/mnt/parker-data/parker/external-transcription-attempts` → `/data/external-transcription-attempts`

`PARKER_PRODUCTION_COMMIT` was set to the exact deployed commit (`1e52fac7...`), placed in the per-deployment exact-image-pin override rather than the reusable ledger-config override, since its value is deployment-specific.

**No new authority model was created.** Configuring the co-required acceptance-authority root activates `FidelityFirstAcceptanceCoordinator` as a side effect, but it remains fail-closed for every authority ID — new or the historical, already-consumed one (`authority-fa-9.4p-a1-r2-2f5a4813-...`, confirmed still present, untouched) — because the accepted profile's lifecycle is `ACCEPTED`, not `ACCEPTANCE_PENDING`, and `FidelityFirstAcceptanceCoordinator.invoke` rejects with `LIFECYCLE_NOT_ACCEPTANCE_PENDING` before any authority, ledger, or provider logic runs. The historical consumed authority cannot be retried through this path.

## Deployment procedure

```
docker compose -f docker-compose.yml \
  -f <exact-image-pin: 1e52fac7 + PARKER_PRODUCTION_COMMIT>.yml \
  -f docker-compose.openai-enablement.yml \
  -f docker-compose.external-transcription-authorization.yml \
  -f docker-compose.fidelity-first-attempt-ledger.yml \
  up -d --no-build --pull never --no-deps --force-recreate parker
```

No unrelated service was recreated. No image was built or pulled.

## Production configuration preserved

Every environment variable and mount present before this deployment remains present, confirmed by direct inspection: owner UI/session authentication (`PARKER_OWNER_UI_AUTHENTICATION_ROOT`, paired-device store), high-authority secret mount (`/run/secrets/parker_owner_high_authority_verification`), opaque owner principal, OpenAI provider profile mount and API key, enhanced-transcription enablement, external-transcription authorization storage root, evidence stores, human fidelity review stores, and corrected-representation stores. Two new variables were added (`PARKER_FIDELITY_FIRST_ACCEPTANCE_AUTHORITY_STORAGE_ROOT`, `PARKER_FIDELITY_FIRST_ATTEMPT_STORAGE_ROOT`) plus `PARKER_PRODUCTION_COMMIT`; nothing existing was dropped or altered.

## Post-deployment verification

- **Exact source/image/JAR running**: `docker inspect` `.Image` = `sha256:96a54f465b...`, `org.opencontainers.image.revision` = `1e52fac7c18fd914c2e7504050706e6f2a5a3a4b`, in-container runtime JAR SHA-256 = `1493674957b332ba4b31de2c0fe95aa9098c182bf514fd8199df8f11a0df01e0` — exact matches.
- **Readiness**: PASS — `Runtime started` / `Owner LAN Evidence Upload HTTP server listening on 0.0.0.0:8080` logged cleanly on the corrected deployment.
- **Restart count**: `0` on the corrected deployment, confirmed after a stability wait.
- **Owner session preserved**: the original paired device (`owner-device-d915d144...`) remained `ACTIVE` throughout; all throwaway verification devices created during this unit were revoked immediately after use.
- **Enhanced transcription readiness AVAILABLE**: `{"status":"READY","message":"Enhanced transcription is available."}`.
- **External-transcription authorization lane CONFIGURED**: status endpoint returns real disposition values (not `AUTHORIZATION_LANE_NOT_CONFIGURED`).
- **Existing exact-target authorization present for the test document**: the owner had independently authorized `evidence-44d61bfe-e46f-4d39-85e7-9f68f122369d` after UI-INGESTION-5E (durable record dated `2026-09-05T03:55:06Z`, principal `user.steve`) — confirmed live: `GET /owner/evidence/{id}/authorize-enhanced-transcription` returns `{"status":"AUTHORISED", ...}`. "Process document" for that same evidence no longer lists `EXTERNAL_EGRESS_NOT_AUTHORISED` among its reasons (present before authorization, absent now) — direct, live confirmation that the router's dynamic authorization check is working end-to-end.
- **High-authority verification remains configured**: secret file mount unchanged, present, correct size; the runtime's own startup-time `require()` over it would have crashed the container otherwise (it did not).
- **Fidelity-first attempt ledger root present and writable**: `FileSystemFidelityFirstAttemptLedger`'s own constructor requires `Files.isDirectory(root) && Files.isWritable(root)` — the runtime booting cleanly (not crash-looping) is direct, structural proof this holds from the container's own perspective.
- **Execution binding can now be created**: all preconditions `invokeExternalTranscriptionWithFreshBinding` requires are met — readiness `Ready`, ledger non-null, OpenAI credential present, and (checked live above) the evidence manifest resolves. The binding-construction logic itself was exhaustively covered offline by UI-INGESTION-6's 61 passing focused tests (`OrdinaryFidelityFirstExecutionIdentityTest`, `FidelityFirstExternalTranscriptionTest`). This unit did not invoke the real endpoint that would exercise it, to guarantee zero provider exposure.
- **Run enhanced transcription remains enabled, not executed**: the served page's button logic (unchanged since UI-INGESTION-5D/5E) requires both readiness `READY` and per-document authorization `AUTHORISED` — both true for the authorized test document. `POST /owner/evidence/{id}/transcribe-external` was **not called** during this unit.

## Provider accounting

- Provider calls: `0`
- Retries: `0`
- External evidence egress: `0`

## Known structural-fidelity limitation — unresolved, separately governed

Unchanged by this unit: `openai-fidelity-first-transcription-v1` has an open, owner-accepted finding that its real-document output previously failed human fidelity review (wrong block reading order), with an accepted remediation (region-anchored transcription, `FA_9_4P_A1_REGION_ANCHORED_FIDELITY_ACQUISITION_IMPLEMENTATION_DESIGN.md`) that remains design-only. Human fidelity review, correction governance, and source-confirmed eligibility rules are all untouched and remain the operative safeguards for whatever this profile now produces once the owner triggers real execution.

## Result

The exact owner-accepted UI-INGESTION-6 candidate is running in production with matching image, source, and JAR identities; restart count 0; every prior configuration item preserved; the existing fidelity-first attempt ledger reused (not duplicated); the owner's own independently-created exact-target authorization confirmed live and effective; and "Run enhanced transcription" enabled for that document without being executed.
