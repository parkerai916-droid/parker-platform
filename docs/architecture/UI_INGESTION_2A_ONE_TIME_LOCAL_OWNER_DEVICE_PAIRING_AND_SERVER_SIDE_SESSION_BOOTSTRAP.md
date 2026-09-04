# UI-INGESTION-2A — One-Time Local Owner Device Pairing and Server-Side Session Bootstrap

## Status

IMPLEMENTED — OFFLINE VERIFIED — NOT DEPLOYED

## Owner security decision and starting state

The selected bootstrap is an explicitly host-initiated, one-time, short-lived local pairing. Starting `main` HEAD/upstream was `bf02f627d66a8ff05f88eb3ae1f7a2fc36a6c6bc`; the worktree was clean. No legal owner name is required or recorded by this implementation.

## Threat boundary

LAN reachability does not authenticate a caller. An unregistered browser receives only a pairing page and cannot invoke owner evidence, acquisition, analysis, or administration APIs. Pairing requires a challenge created by an authenticated local/SSH host administrator. Wrong, absent, expired, or consumed challenges fail closed.

The current server is plain private-LAN HTTP, so cookies cannot truthfully carry `Secure` without becoming unusable. All authentication cookies are `HttpOnly`, `SameSite=Strict`, and path-bound. HTTPS termination remains required before any exposure beyond the protected LAN; when Parker gains an HTTPS listener, its cookie writer must add `Secure`.

## Pairing and registered-device model

`OwnerUiAuthentication` uses `SecureRandom` for a 24-byte URL-safe pairing code, 32-byte device credential, opaque device ID, and 32-byte session ID. A challenge expires after five minutes and is deleted before durable device issuance, enforcing single use. The challenge file contains only expiry and a SHA-256 verifier, not the usable code.

Successful pairing returns the device credential once through an HttpOnly cookie. The durable device record contains only opaque owner PrincipalId, credential verifier, creation instant, and ACTIVE/REVOKED state. It contains neither plaintext credential nor legal name. Device identity is not based on IP address or browser fingerprinting.

## Server-side session and revocation

A valid device cookie pair establishes a process-local eight-hour owner session. API authorization validates the opaque session and durable device ACTIVE state. Missing, invalid, expired, logged-out, or revoked sessions fail closed. `POST /owner/logout` invalidates the session.

The host command boundary is:

- `--owner-ui-pairing-admin pair`
- `--owner-ui-pairing-admin list`
- `--owner-ui-pairing-admin revoke <opaque-device-id>`
- `--owner-ui-pairing-admin revoke-all`

The pairing command is the sole place the short-lived code is displayed. List/revocation never display credential material. Runtime and host command share `PARKER_OWNER_UI_AUTHENTICATION_ROOT`; Compose maps its governed host root to `/data/owner-ui-authentication`.

## Legacy token retirement

The Owner token textbox, Remember token checkbox, `localStorage` persistence, bearer-header construction, and remembered-token warning were removed from the page. Browser requests now rely on same-origin HttpOnly cookies. `PARKER_OWNER_HTTP_TOKEN` remains only as a backward-compatible configuration field for older non-browser configuration parsing; production composition no longer consumes or publishes it, and Docker Compose no longer passes it.

## High-authority separation

The external high-authority verification credential, opaque correction principal, exact-purpose policy, and exact-target correction checks are unchanged. Pairing grants ordinary Owner UI access only. No correction service or high-authority secret is exposed through the page, JavaScript, cookies, session state, pairing records, or device records.

## Ingestion compatibility

UI-INGESTION-1 ingestion code and governance are unchanged: upload, durable registration, acquisition, explicit governed transcription, canonical readback, human-fidelity presentation, corrected-representation presentation, and existing Analyse gating retain their existing service boundaries. Only HTTP authentication transport changed.

## Focused verification

Command:

`./gradlew test --tests 'parker.composition.OwnerUiAuthenticationTest' --no-daemon -Dorg.gradle.jvmargs=-Xmx4g -Dkotlin.daemon.jvm.options=-Xmx4g`

Result: 1 suite, 3 tests, zero failures/errors/skips. Tests prove no authentication from reachability, active challenge requirement, wrong/expired denial, single use, opaque device identity, verifier-only persistence, valid/invalid device behavior, session expiry, device revocation, HttpOnly/SameSite cookie construction, legacy browser-token removal, and absence of the high-authority secret from browser source. Kotlin production and test compilation also completed successfully.

No full-suite run was performed, per the compute-conscious instruction; focused verification exposed no wider regression.

## Production and provider accounting

Production deployment, restart, device pairing, authentication-state creation, and governed-state mutation were not performed. Provider calls/retries/external egress were `0 / 0 / 0`.
