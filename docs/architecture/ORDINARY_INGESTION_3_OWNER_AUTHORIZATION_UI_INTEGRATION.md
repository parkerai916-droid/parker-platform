# ORDINARY-INGESTION-3 Owner Authorization UI Integration

## Scope and safety

This unit exposes the existing governed ordinary region-v5 evidence-specific owner authorization boundary through the owner HTTP workflow. Authorization and execution remain distinct. All implementation verification used temporary stores and fake providers. No production evidence was authorized, no production provider call was made, and no production store was mutated during implementation or offline verification.

## Fresh trace and pre-edit gap inventory

The existing path was traced from `OrdinaryRegionOwnerAuthorization` and its create-once filesystem store, through `OrdinaryRegionIngestionWorkflow`, `ParkerRuntime`, `OwnerEvidenceOperations`, the HTTP server, and the browser UI. Seven gaps were enumerated before editing:

1. `AUTHORIZATION_UI_CONTROL_MISSING`: no explicit evidence-specific owner control.
2. `AUTHORIZATION_HTTP_ROUTE_NOT_EXPOSED`: no authenticated authorization route.
3. `EXISTING_ROUTE_NOT_WIRED_TO_UI`: the runtime boundary was absent from owner operations/composition.
4. `SAFE_AUTHORIZATION_COMMAND_MISSING`: the only runtime method accepted a caller-built grant instead of reconstructing governed fields.
5. `AUTHORIZATION_STATE_NOT_CANONICAL`: the proposal hardcoded `NOT_AUTHORISED`.
6. `AUTHORIZATION_CONFIRMATION_DISCLOSURE_MISSING`: no evidence/provider/transmission confirmation.
7. `AUTHORIZATION_STATE_NOT_REFRESHED`: the UI could not reload state following authorization.

No premature ordinary region execution control was present; `executeAvailable` was already false for proposals.

## Changes

- Added a narrow evidence-ID-only authorization command. It freshly requires exact-build capability acceptance, resolves and verifies the authoritative PDF, and reconstructs the source digest, capability digest, provider, purpose, transmitted scope, disclosure, owner principal, approval time, and expiry server-side.
- Added deterministic, guarded create-once identity and idempotent replay. Read-only status performs no creation. Missing, corrupt, revoked, expired, unsupported-source, and non-accepted states fail closed.
- Added fresh canonical authorization status to the proposal projection.
- Added `POST /owner/evidence/{evidenceId}/authorize-region-transcription`, protected by the existing owner bearer-token boundary. The response explicitly reports `executionStarted: false`.
- Added an `Authorize external transcription` UI action only for an accepted PDF in `NOT_AUTHORISED` state. Confirmation names the evidence filename and ID, provider `OpenAI`, exact transmission disclosure, and states that authorization does not transmit or execute.
- Refreshes the read-only acquisition decision after the action. Authorization and the existing execution entry point remain separate; the proposal continues to report `executeAvailable: false`.

## Offline verification

- `./gradlew testClasses --no-daemon`: PASS.
- Focused runtime, adapter, and HTTP server suites: PASS.
- `./gradlew test --no-daemon`: PASS — 3,249 tests, 0 failures, 0 errors, 9 skipped.
- `git diff --check`: PASS.
- Runtime test proves status is non-mutating, explicit authorization creates exactly one record, replay is idempotent, the binding is evidence/source/capability/build-specific, and provider calls remain zero before the separate fake-provider execution.
- Adapter test proves the distinct owner action projects canonical authorization without invoking execution.
- Existing full HTTP server suite passes with the authenticated route and UI source integrated.

## Production pre-change observation

Before deployment, the running container remained healthy with restart count zero. Its image was `sha256:f6e674e0f56f405de55ea73ba97850aaf8ec9c38727fbc924d5cdf08166f2da1` and embedded commit `363201a2233d29240571781ced0e78dfbc6680e1`. Read-only counts remained: capability acceptances 2, owner authorizations 0, attempts 4, provider-state records 2.

## Deployment and promotion

Implementation commit `3ae55f492e10f18b9dca0846114bd80458680fe6` was pushed to `origin/main`. The exact build succeeded and produced immutable image `sha256:6e811f2af485f8e65ade9a44e10cc1d49041bd41b7679eff2cc5b7b1bcbc4588`. Only Parker was recreated with `--no-deps --no-build --pull=never --force-recreate` using the governed three-file Compose stack.

The replacement container is `1a7e2120f6cb47335b0c34253386adab28bd97134d4429e19acb1179a6c0c653`, running with restart count zero. Its source commit, production commit, configured immutable image, and actual image all match the exact values above. The unauthenticated administration endpoint returns HTTP 401. The only startup log match for `error|exception` is Log4j's known missing-provider diagnostic, not a Parker startup failure.

Before promotion, authenticated fresh evaluation returned `CAPABILITY_NOT_ACCEPTED`, exact runtime commit, and no accepted commit. Exactly one promotion POST was issued with only capability ID and promoting commit. It returned HTTP 201 `CREATED`, record `1c08c6e5bce06505dc114970fb36a9e59f664326688f280b46403e5265bec01b`, capability digest `9b404e8dfc4f0ffa3067fcffb00c39e6bd739050f173418de740239a1dc94103`, and the exact runtime commit.

Fresh post-promotion evaluation returned `ACCEPTED` with runtime and accepted commits both equal to the implementation commit. Read-only acquisition requests for the intended fixture (`evidence-4c6f2ee8-2f62-47be-bd7a-946c744b2766`) and CV (`evidence-99b1c6fa-91e7-4a30-b212-b7a677718417`) both returned `PROPOSED`, `NOT_AUTHORISED`, `OWNER_AUTHORIZATION_REQUIRED`, `authorizationAvailable: true`, and `executeAvailable: false`, with the exact OpenAI transmission disclosure. The served UI contains the explicit authorization control and confirmation copy.

Final durable counts are capability acceptances 3, owner authorizations 0, attempts 4, and provider-state records 2. Therefore the sole intended production-store mutation was the one exact-build capability acceptance; no production evidence authorization, reservation, execution, provider call, retry, provider-state write, derivative admission, or other evidence mutation occurred.

## Verdict

UNIT ORDINARY-INGESTION-3 COMPLETE — EXISTING GOVERNED EVIDENCE-SPECIFIC AUTHORIZATION BOUNDARY IS EXPOSED THROUGH THE OWNER WORKFLOW WITH EXPLICIT TRANSMISSION DISCLOSURE; PRODUCTION PDF PROPOSALS NOW PRESENT A DISTINCT OWNER AUTHORIZATION ACTION WHILE EXECUTION REMAINS DISABLED; ZERO PRODUCTION AUTHORIZATION OR PROVIDER EGRESS OCCURRED
