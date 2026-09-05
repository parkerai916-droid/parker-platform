# UI-INGESTION-5E — Deploy Authorization UI State Fix

## Status

DEPLOYED — VERIFIED — NO PROVIDER INVOKED — NO PERMANENT AUTHORIZATION CREATED

## Scope

Deployment and verification only, of the UI-INGESTION-5D candidate that fixed the client-side JS defect where a failed "Confirm authorization" attempt was written into the same `row.acquisitionError` field that blanks the entire acquisition panel. No source was changed in this unit.

## Deployed candidate (owner-accepted, exact, not rebuilt)

- Source commit: `656e76c727aa664139431416e5cc4a4d821eec08`
- Image ID: `sha256:a31298ae4bb2b8c172850ca4615eb7af7f8b43654da192ade9c1a229854749da`
- Runtime JAR SHA-256: `28cf09a42499762bf10d1e10812f2b7a1f0f9db725792b01f2e4c89027fcc680`
- Local tag used: `parker-platform:ui-ingestion-5d-656e76c7` (already built by UI-INGESTION-5D; not rebuilt or substituted)

Prior production baseline (pre-deployment): `sha256:db08105cb7a4e48fce29c2ba7bae1c8629eb763629c099577d3aea6db4031d8c` (UI-INGESTION-5C).

## Deployment procedure

Established exact-image redeploy pattern, unchanged from UI-INGESTION-5C except the image pin:

```
docker compose -f docker-compose.yml \
  -f <exact-image-pin: 656e76c>.yml \
  -f docker-compose.openai-enablement.yml \
  -f docker-compose.external-transcription-authorization.yml \
  up -d --no-build --pull never --no-deps --force-recreate parker
```

No new configuration was introduced. No unrelated service was recreated. No image was built or pulled.

## Post-deployment verification

- **Exact artifact running**: `docker inspect` `.Image` = `sha256:a31298ae...`, `org.opencontainers.image.revision` = `656e76c727aa664139431416e5cc4a4d821eec08`, in-container runtime JAR SHA-256 = `28cf09a42499762bf10d1e10812f2b7a1f0f9db725792b01f2e4c89027fcc680` — all exact matches.
- **Readiness**: `Runtime started` / `Owner LAN Evidence Upload HTTP server listening on 0.0.0.0:8080` logged cleanly. PASS.
- **Restart count**: `0`, confirmed immediately after start and again after the full verification pass.
- **Owner session preserved**: the pre-existing paired device (`owner-device-d915d144...`) remained `ACTIVE` throughout; every throwaway verification device created during this unit was revoked immediately after use.
- **Enhanced transcription readiness AVAILABLE**: `GET /owner/evidence/transcription-readiness` → `{"status":"READY","message":"Enhanced transcription is available."}`.
- **Fix is live in the served page**: confirmed the deployed page's JavaScript contains `row.authorizationError = resp.ok ? null : (result.detail || 'Authorization was not created.')` and the corresponding `if (row.authorizationError) {` rendering branch — the dedicated, non-panel-blocking field from the UI-INGESTION-5D fix.

## Live check against a real, existing, unauthorized document

Performed against `evidence-0275472f-535a-4cf1-b30d-f45ac7684743` (an existing PDF, previously unauthorized):

1. **Process document renders authorization-required state**: `GET /acquisition` returned `NO_ELIGIBLE_CAPABILITY` with `EXTERNAL_EGRESS_NOT_AUTHORISED` among the reasons; `GET /authorize-enhanced-transcription` returned `NOT_AUTHORISED`, `detail: null`.
2. **Intentionally wrong high-authority credential submitted once**: `POST /authorize-enhanced-transcription` with body `intentionally-wrong-credential`.
3. Confirmed:
   - **Fails closed**: HTTP `403`, `{"status":"NOT_AUTHORISED","detail":"HIGH_AUTHORITY_VERIFICATION_FAILED"}`.
   - **No authorization record created**: the durable store directory (`/mnt/parker-data/parker/external-transcription-authorizations`) remained empty; an immediate status re-check returned `NOT_AUTHORISED`, `detail: null` (not a lingering failure state).
   - **Panel is not replaced by `HIGH_AUTHORITY_VERIFICATION_FAILED`**: server-side state was never polluted (the whole point of the 5D fix is that this failure lives only in transient client JS state, `row.authorizationError`, never server state) — confirmed by the clean re-check above and by the served page's JS containing the fixed rendering path.
   - **Authorize control remains visible / failure shown only as non-blocking text**: verified structurally via the served JS (`appendEnhancedTranscriptionAuthorizationSection` renders `row.authorizationError` as a `'Last authorization attempt: Failed: ...'` line only when set, and always continues on to render the Authorize button/form — it never returns early the way the panel-blocking `acquisitionError` path does).
4. **Refreshed existing evidence**: `GET /owner/evidence` returned the full list (30 items) including the target document, unaffected.
5. **Process document again**: `GET /acquisition` for the same document returned the identical clean `NO_ELIGIBLE_CAPABILITY`/`EXTERNAL_EGRESS_NOT_AUTHORISED` state as step 1.
6. **Authorization UI still renders normally**: `GET /authorize-enhanced-transcription` again returned clean `NOT_AUTHORISED`, `detail: null` — no residual failure state, confirming the fix holds across a repeated "Process document" cycle following a failed attempt.

No successful/permanent authorization was created at any point. No evidence was mutated. No provider was invoked.

## Provider accounting

- Provider calls: `0`
- Retries: `0`
- External evidence egress: `0`

## Result

The exact UI-INGESTION-5D candidate is running in production with matching image, source, and JAR identities; restart count 0; owner session and all evidence/authorization stores untouched; enhanced transcription readiness AVAILABLE. A live, intentionally-wrong high-authority credential attempt against a real document confirmed the authorization flow fails closed, creates no record, and — critically — no longer poisons the acquisition panel: "Process document" and the "Authorize enhanced transcription" control both continue to render normally afterward, including after a full evidence-list refresh.
