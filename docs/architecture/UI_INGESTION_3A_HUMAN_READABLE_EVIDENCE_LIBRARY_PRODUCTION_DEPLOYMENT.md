# UI-INGESTION-3A — Human-Readable Evidence Library Production Deployment

## Status

DEPLOYED — ARTIFACT VERIFIED — AWAITING OWNER LIVE-BROWSER CONFIRMATION

## Scope

Deployment-only. No source was modified, no test suite was rerun, no historical evidence was reprocessed, and no provider was invoked. This continues from UI-INGESTION-3 (candidate built, focused-verified, not deployed) on clean `main` at HEAD/upstream `1bc9f1e1af1870a8c792bc206f57b085c525def7`.

## Deployed candidate (owner-accepted)

- Source commit: `296ff5802670387f44a7950ee6c57899ebf17e7d`
- Runtime JAR SHA-256: `ac5d9217a60d52ccaa2ef1dc4ecba6e75264960c19ae86280fd691bd30399d99`
- Image ID: `sha256:784aa3c69b8fcf1a52d3648f3fe7590fe458acf40ed0c11b3efda56e77665f24`
- OCI config digest: `sha256:0322c3947b4bd969af6c9a26400986bd984d6a57256450a686a3cc2b4aa064a7`

## Deployment procedure

Used the established exact-image redeploy pattern (matching UI-INGESTION-2B-R1): an override Compose file pinned `services.parker.image` to `parker-platform@sha256:784aa3c...`, with `PARKER_BUILD_COMMIT=296ff5802670387f44a7950ee6c57899ebf17e7d` supplied only to satisfy Compose's build-arg interpolation. Invocation:

`docker compose -f docker-compose.yml -f <override>.yml up -d --no-build --pull never --no-deps --force-recreate parker`

No image was built or pulled; the already-locally-present candidate image (tag `parker-platform:ui-ingestion-3-296ff580`) was used as-is. No unrelated service was recreated.

## Post-deployment verification

- **Production image exact**: `docker inspect` reports `.Image` = `sha256:784aa3c6...`, matching the accepted image ID exactly.
- **Embedded source exact**: `org.opencontainers.image.revision` label on the running container = `296ff5802670387f44a7950ee6c57899ebf17e7d`.
- **Runtime JAR exact**: `sha256sum` of `/opt/parker/lib/parker-platform-0.8.0-runtime-complete.jar` inside the running container = `ac5d9217a60d5...`, matching the accepted JAR digest exactly.
- **OCI config digest**: confirmed present as a same-named content-addressed blob (`0322c3947b...`) inside a `docker save` of the exact deployed image, alongside the manifest blob `784aa3c6...`.
- **Readiness**: startup log shows `Runtime started` followed by `Owner LAN Evidence Upload HTTP server listening on 0.0.0.0:8080`; unauthenticated `GET /` returns `200`.
- **Restart count**: `0`, both immediately after start and after a stability wait.
- **Owner-token field**: absent from the served unauthenticated page (device-pairing page only; no reusable-token input).
- **High-authority verification secret**: mounted read-only into the container from its existing host file via the unchanged Compose `secrets:` block; never read into shell output, logs, or this report.

## Owner session and authentication-store preservation

The `/mnt/parker-data/parker/owner-ui-authentication` volume was mounted unchanged (same host path, not recreated) into the redeployed container. The previously paired device's record (`owner-device-d915d1448a977b2ac6eb7bc3ce92bf63cd2b6cd47a4423e19b6fe81162e792e6.device`) is present and untouched by this deployment. Authenticated API routes (`/owner/evidence`, `/owner/evidence/list`, `/owner/admin/region-capability-acceptance`) correctly return `401` without a valid session, confirming access control is enforced on the redeployed runtime.

This deployment did not exercise a live authenticated browser session: no valid owner-session cookie is obtainable from this shell environment (session credentials are `HttpOnly` and live only in the owner's already-paired Windows browser), and no new device pairing was initiated, since that is an owner-authority action outside this task's deploy-and-verify scope. Session continuity is therefore confirmed by preservation of the untouched device/session store and unchanged server code, not by a fresh live request. Live confirmation from the owner's paired browser is the next step, per the "Real upload" boundary below.

## Human-readable evidence library

The deployed runtime JAR is byte-identical (matching SHA-256) to the UI-INGESTION-3 candidate whose focused suite (26 passed / 0 failed / 0 skipped) and isolated HTTP/runtime acceptance already exercised, through a real paired session, exactly the behavior this deployment now serves: human-readable filename as primary identity, Evidence ID as secondary/subordinate identity, `Unnamed evidence` fallback for records with no trustworthy filename, newest-first default ordering, filename/Evidence-ID search, sorting (newest, oldest, filename A–Z, status), deterministic status filters, the `Process document` action bound to the existing governed handler, a Details view retaining exact internal state, and the existing Analyse section unchanged. No historical evidence was reprocessed to re-check these controls in production.

## Real upload

No evidence document was uploaded or processed by this deployment task. The UI is ready for the owner to perform a real upload manually from the paired Windows browser.

## Provider accounting

- Provider calls: `0`
- Retries: `0`
- External evidence egress: `0`

## Result

The exact owner-accepted candidate is deployed, its image/source/JAR identities are cryptographically confirmed against the running container, readiness is PASS, restart count is 0, the authentication/session store and access-control boundary are intact and unmodified, and the high-authority verification secret remains separate, file-mounted, and undisclosed. Live in-browser confirmation of the human-readable evidence library and the paired session is pending the owner's own use of the deployed UI.
