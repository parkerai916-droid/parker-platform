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

Pending exact commit, image, deployment, pre-promotion fail-closed check, one-shot capability re-promotion, and final read-only production verification.
