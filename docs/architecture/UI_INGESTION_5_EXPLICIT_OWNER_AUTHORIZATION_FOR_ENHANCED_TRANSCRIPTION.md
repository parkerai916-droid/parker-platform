# UI-INGESTION-5 — Explicit Owner Authorization for Enhanced Transcription

## Status

IMPLEMENTED — FOCUSED VERIFIED — CANDIDATE BUILT — NOT DEPLOYED

## Starting state

Clean `main` at HEAD/upstream `a7afbf47ec72d255075f921c905e2d19bb2330dc` (UI-INGESTION-4). Production `parker-runtime` healthy, restart count `0`, enhanced transcription readiness `READY`.

## Current production blocker

`GovernedAcquisitionOwnerWorkflow.evaluate`/`execute` (`src/runtime/GovernedAcquisitionOwnerWorkflow.kt`) hardcoded `ExternalEgressAuthorisation.NOT_AUTHORISED` for every evaluation, unconditionally — there was no per-evidence-target owner authorization record anywhere. Separately, the standalone "Run enhanced transcription" HTTP action (`ExternalTranscriptionOwnerInvocationCoordinator.invoke`) is gated only by a **coarse, static** `PermissionPolicyRule` keyed to `(EXECUTE, DOCUMENT, evidence-intelligence.external-transcription)` — active for every evidence artifact alike the moment the Authorization Purpose registers at startup, with no per-target distinction. The button itself was hardcoded `disabled = true` in the shipped UI (`OwnerEvidenceHttpServer.kt`), so this gap was never live-reachable from the browser, but a direct HTTP POST to `/owner/evidence/{id}/transcribe-external` would previously have succeeded for any evidence artifact once readiness was `READY`, with no exact-target owner confirmation at all.

## Existing authorization mechanism reused

- **Authorization Purpose**: `ExternalTranscriptionInvocationGate.AUTHORIZATION_PURPOSE` (`evidence-intelligence.external-transcription`) — already registered at composition startup; unchanged.
- **Permission-policy evaluation**: the same `permissionEngine.evaluate(ExternalTranscriptionInvocationGate.buildExecutionRequest(...))` call the existing invocation path already performs; the new authorize step performs the identical check before persisting a grant, so creation fails closed exactly like execution would.
- **Owner high-authority verification**: `ExternalFileOwnerHighAuthorityVerification` (`src/runtime/HumanCorrectionPermissionPolicy.kt`) — the same secret file, same constant-time comparison, same loading discipline already used for human correction. Generalized **additively**: `load(path, allowedPurposes = setOf(HUMAN_TRANSCRIPTION_CORRECTION_PURPOSE))` — the default preserves every existing caller's behavior unchanged; this unit's new composition call site explicitly passes `allowedPurposes = setOf(ExternalTranscriptionInvocationGate.AUTHORIZATION_PURPOSE)`. No new credential mechanism was invented.
- **Owner principal**: `PrincipalId(config.ownerPrincipalId)` — the same principal already used at actual invocation time.
- **Execution path**: `ExternalTranscriptionOwnerInvocationCoordinator` is **unmodified**; a new gate was added only at the `ParkerRuntime.invokeExternalTranscriptionAsOwner` call site, one layer above it.

## What was added (new, minimal)

`src/runtime/ExternalTranscriptionOwnerAuthorization.kt` — a durable, per-evidence-target grant record neither of the reused mechanisms above already persisted:

- `ExternalTranscriptionOwnerAuthorization`: exact-target grant (evidence artifact ID, source SHA-256, principal, purpose, timestamp).
- `FileSystemExternalTranscriptionAuthorizationStore`: one tamper-evident file per target; `createOrGet` is idempotent for a matching grant and fails closed (`Conflict`) on any mismatch.
- `ExternalTranscriptionOwnerAuthorizationCoordinator`: `status`/`authorize`/`isAuthorized`. `authorize` requires an active Purpose, a resolvable authoritative manifest (exact-target binding), a passing high-authority verification, and an approved permission decision — in that order, failing closed at the first unmet gate. It has no dependency on any provider adapter or transport.

Optional/additive composition: gated by a new `PARKER_EXTERNAL_TRANSCRIPTION_AUTHORIZATION_STORAGE_ROOT` config key (`ParkerRuntimeConfig.kt`). Absent (as in every current deployment) it is `null` and every new behavior below falls back to its pre-existing default — no regression for any deployment that has not opted in.

`GovernedAcquisitionOwnerWorkflow` gained one new, defaulted constructor parameter, `externalEgressAuthorised: suspend (EvidenceArtifactId) -> Boolean = { false }` — the default preserves the exact prior always-`NOT_AUTHORISED` behavior for every existing test call site; production composition wires it to `externalTranscriptionAuthorizationCoordinator?.isAuthorized(id) == true`.

`ParkerRuntime.invokeExternalTranscriptionAsOwner` gained one new pre-check: when the authorization coordinator is configured, the exact evidence target must already carry a durable grant, or the call returns `NotAuthorised` before `ExternalTranscriptionOwnerInvocationCoordinator.invoke` — and therefore the provider — is ever reached.

## UI flow added

`OwnerEvidenceUpload.kt` / `OwnerEvidenceHttpServer.kt`:

- `GET`/`POST /owner/evidence/{id}/authorize-enhanced-transcription` — status and explicit-confirmation endpoints. The `POST` body is the raw presented high-authority verification credential only (mirroring the existing `/owner/pair` raw-body convention) — never a JSON envelope, never the owner's legal name.
- When "Process document" returns `NO_ELIGIBLE_CAPABILITY` with reason `EXTERNAL_EGRESS_NOT_AUTHORISED`, the panel now shows: *"This document needs enhanced transcription. External transcription has not yet been authorised."* Raw internal reason codes remain visible under the existing "Why execution is unavailable" field — nothing was removed.
- "Authorize enhanced transcription" reveals document name, Evidence ID, media type, provider/purpose, and a disclosure statement, plus a password-style credential field. Confirming posts to the new endpoint; it never triggers execution.
- "Run enhanced transcription" — previously permanently `disabled = true` in the shipped page — now enables only when readiness is `READY` **and** the exact-target authorization status is `AUTHORISED`; otherwise it stays disabled with an explanatory title. Authorization and execution remain two separate owner-visible clicks.

## Focused tests

- `ExternalTranscriptionOwnerAuthorizationCoordinatorTest` (new, 10 tests): missing-authorization status, exact confirmation creates a grant with no provider dependency reachable, post-authorization status/idempotent re-authorization, wrong/unresolvable evidence target fails closed, inactive purpose fails closed, missing/wrong high-authority credential fails closed even with an approved permission decision (and the permission engine is never even reached first), permission-policy denial fails closed with a correct credential, one target's grant never authorizes a different target, store-level idempotent-vs-conflicting behavior.
- `GovernedAcquisitionOwnerPresentationTest` (+2 tests): the dynamic `externalEgressAuthorised` check is bound to the exact evidence artifact (authorizing one ID does not select the capability for another), and omitting the parameter preserves the prior always-`NOT_AUTHORISED` default.
- `ExternalTranscriptionOwnerInvocationCoordinatorTest` (existing, unmodified, re-run): 7/7 pass — proves the existing enhanced-transcription execution path is unchanged.
- `HumanCorrectionPermissionPolicyTest` (existing, unmodified, re-run): 5/5 pass — proves the high-authority verification generalization did not weaken or alter the existing human-correction path.
- `OpenAiExternalTranscriptionProviderProfileTest`, `OpenAiApiCredentialTest`: 10/10 and 5/5 pass, unaffected.

Total: 49/49 passed, 0 failed. `ParkerRuntimeConfigLoaderTest` was also re-run in isolation: the same 4 pre-existing, unrelated failures already root-caused and reported in UI-INGESTION-4 (`PARKER_OWNER_HTTP_BIND_ADDRESS`/`PORT`/`TOKEN` interaction) remain, with zero new failures — the new `PARKER_EXTERNAL_TRANSCRIPTION_AUTHORIZATION_STORAGE_ROOT` key introduced no regression. The full suite was not run (out of scope; no signal indicated a wider regression).

## Provider accounting

- Provider calls: `0`
- Retries: `0`
- External evidence egress: `0`

No provider adapter, transport, or mechanism was invoked at any point during implementation, testing, or candidate build. `ExternalTranscriptionOwnerAuthorizationCoordinator` has no dependency capable of reaching a provider.

## Exact candidate artifact

- Candidate source commit: `0d28508f58d19441dd80146664de9a146f2637ca`
- Runtime JAR SHA-256: `ff25f70340743d5e9f2829daeaa48c4f0147201c8b8f7dd33b25d952d60c2528`
- Image ID / manifest digest: `sha256:db08105cb7a4e48fce29c2ba7bae1c8629eb763629c099577d3aea6db4031d8c`
- OCI config digest: `sha256:10beef5ce15b1e90edd5223f364a2439e882426d8c688885b9457e131547ffb6`
- Image size: `868,370,679` bytes
- Platform: `linux/amd64`
- Revision label: `0d28508f58d19441dd80146664de9a146f2637ca`
- Local tag: `parker-platform:ui-ingestion-5-0d28508f`

Built with `docker buildx build --pull=false --provenance=false --platform linux/amd64 --build-arg PARKER_BUILD_COMMIT=<commit> --load .` Both the OCI config blob and the manifest blob were confirmed present as self-verifying content-addressed blobs inside a `docker save` of the exact built image. `--network=none` (the deployment convention used for prior candidates) failed at the Gradle-wrapper-distribution-download build step in this environment (no locally cached distribution was available in the buildx cache this time); ordinary network access was used only for that build-tooling step, not for anything evidence- or provider-related. The candidate was **not deployed**; no production authorization was created; no provider was invoked.

## Production unchanged

Verified after the candidate build completed: `parker-runtime` still reports image `sha256:784aa3c69b8fcf1a52d3648f3fe7590fe458acf40ed0c11b3efda56e77665f24` (the UI-INGESTION-4 candidate), restart count `0`, status `running` — untouched by this unit.
