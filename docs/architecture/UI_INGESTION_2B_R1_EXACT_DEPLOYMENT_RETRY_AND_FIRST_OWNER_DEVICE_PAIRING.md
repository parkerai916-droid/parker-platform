# UI-INGESTION-2B-R1 — Exact Deployment Retry and First Owner Device Pairing

## Status

ACCEPTED — DEPLOYED — PAIRED

## Deployment retry basis

The earlier UI-INGESTION-2B deployment attempt stopped before Parker runtime startup because Docker Compose evaluates the required `PARKER_BUILD_COMMIT` build-argument interpolation even for an exact prebuilt-image deployment using `--no-build`. This was an invocation/configuration failure, not a candidate, pairing, authentication-runtime, or ingestion defect. No implementation or Compose source was changed for this retry.

The retry began on clean `main` with HEAD and upstream both at `25652bb83fac85cb5a24b8fd68b94816a3f46a44`. It explicitly supplied `PARKER_BUILD_COMMIT=25652bb83fac85cb5a24b8fd68b94816a3f46a44`, matching the accepted candidate source, while retaining `--no-build`, `--pull never`, and `--no-deps parker`.

## Exact accepted and deployed candidate

- Source: `25652bb83fac85cb5a24b8fd68b94816a3f46a44`
- Runtime JAR SHA-256: `eb18bfc12ba762e04b1b2d817e454047b97881c2b4c23e31b85714cab5e00d1d`
- Image ID/digest: `sha256:6784c77b08e745e9beb1695eaca2e80acf7735c566160a460889e08d83a1ab0a`
- OCI configuration digest: `sha256:b19159d7c7ec8de7eb990c9b9e0910f4f075344f300fc663ef2cf70953a179ef`
- Production container: `98572970b01c97e7f013d1c5d18613bdaecef710e846bceb6ab5cd2fbb45cc1a`
- Runtime state: running; Parker runtime started successfully and the Owner LAN Evidence Upload server listens on port 8080
- Restart count: `0`

Docker inspection proved that the running container uses the exact accepted image. The image revision label is the exact source commit, and the runtime JAR read from the running container has the accepted SHA-256.

## Authentication storage and unpaired boundary

The existing `/mnt/parker-data/parker/owner-ui-authentication` root was preserved rather than recreated. It remains mounted read-write at `/data/owner-ui-authentication`; runtime read/write access was verified.

Before pairing, an unauthenticated request received only the device-pairing page. The reusable Owner token field, Remember token control, evidence upload controls, and owner capabilities were absent. LAN reachability therefore did not establish owner identity.

## First owner-device pairing

The host-administration command initiated exactly one cryptographically random, single-use challenge with a five-minute validity period. The challenge was delivered interactively and was not written to Git, this report, or ordinary runtime logs.

Pairing completed successfully. Server-side verification established:

- the pairing challenge was consumed and no longer exists;
- exactly one registered device exists;
- opaque device ID: `owner-device-d915d1448a977b2ac6eb7bc3ce92bf63cd2b6cd47a4423e19b6fe81162e792e6`;
- the durable device record contains a verifier, not the usable plaintext device credential;
- the browser device credential and owner-session identifier are issued as `HttpOnly` cookies with `SameSite=Strict`;
- owner-session state is maintained server-side and bound to the opaque owner principal;
- no reusable owner credential is exposed to browser JavaScript;
- the separate high-authority verification secret remains server-side and is neither a device credential nor an ordinary UI session credential.

The host device-list operation returned only the opaque device ID and no credential material. The newly paired device was not revoked.

## UI ingestion smoke result

The paired owner session opens the existing Owner Evidence Upload UI without Owner token entry. Its existing evidence list, upload controls, machine-readable-representation action, enhanced-transcription readiness control, and human-fidelity/corrected-representation presentation remain available under the server-side owner session. No document was uploaded and no external transcription action was invoked during this deployment acceptance.

## Provider accounting

- Provider calls: `0`
- Retries: `0`
- External egress: `0`

No provider operation was required for deployment, pairing, or the UI smoke verification.

## Result

The exact accepted candidate is deployed and healthy. The legacy reusable browser token path is absent, an unpaired browser fails closed, the first owner device is paired under an opaque identity, and routine governed-ingestion UI access now uses server-side session state. High-authority verification remains separate and undisclosed.
