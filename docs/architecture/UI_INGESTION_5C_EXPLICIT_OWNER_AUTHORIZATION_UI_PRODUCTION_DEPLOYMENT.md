# UI-INGESTION-5C — Deploy Explicit Owner Authorization UI

## Status

DEPLOYED — VERIFIED — ENHANCED TRANSCRIPTION NOT EXECUTED

## Scope

Deployment and verification only, per UI-INGESTION-5A's clean review of commit `0d28508f58d19441dd80146664de9a146f2637ca` (verdict A). No source was changed, no authorization design was altered, no capability-specific scope change was made, no revocation feature was added, and no provider was invoked. Continues from clean `main` at HEAD/upstream `6853410feef02171ab6ce8b8751c3d6bfada8c11`.

## Deployed candidate (owner-accepted, exact, not rebuilt)

- Source commit: `0d28508f58d19441dd80146664de9a146f2637ca`
- Runtime JAR SHA-256: `ff25f70340743d5e9f2829daeaa48c4f0147201c8b8f7dd33b25d952d60c2528`
- Image ID: `sha256:db08105cb7a4e48fce29c2ba7bae1c8629eb763629c099577d3aea6db4031d8c`
- Local tag used: `parker-platform:ui-ingestion-5-0d28508f` (already built by UI-INGESTION-5; not rebuilt or substituted)

## Deployment procedure

Established exact-image redeploy pattern: an image-pin override plus the existing OpenAI-enablement override (unchanged from UI-INGESTION-4) plus one new, additive override activating the UI-INGESTION-5 authorization feature:

```
docker compose -f docker-compose.yml \
  -f <exact-image-pin>.yml \
  -f docker-compose.openai-enablement.yml \
  -f docker-compose.external-transcription-authorization.yml \
  up -d --no-build --pull never --no-deps --force-recreate parker
```

The new override supplies exactly the optional, additive configuration `ExternalTranscriptionOwnerAuthorizationCoordinator` requires to activate (per its own "off unless configured" design, verified in UI-INGESTION-5A):

- `PARKER_EXTERNAL_TRANSCRIPTION_AUTHORIZATION_STORAGE_ROOT=/data/external-transcription-authorizations`
- A new, dedicated durable storage root, freshly created for this deployment and mounted read-write: host `/mnt/parker-data/parker/external-transcription-authorizations` → container `/data/external-transcription-authorizations`, owned by the container's `parker` user (uid/gid 999), matching every other evidence/authorization storage root's existing ownership convention. It was empty before this deployment and remains empty (no authorization was created in this unit).

No unrelated service was recreated. No image was built or pulled.

## Post-deployment verification

- **Exact source/artifact running**: `docker inspect` confirms `.Image` = `sha256:db08105c...`, the `org.opencontainers.image.revision` label = `0d28508f58d19441dd80146664de9a146f2637ca`, and the runtime JAR SHA-256 inside the running container = `ff25f70340743d5e9f2829daeaa48c4f0147201c8b8f7dd33b25d952d60c2528` — all exact matches to the accepted candidate.
- **Readiness**: `Runtime started` and `Owner LAN Evidence Upload HTTP server listening on 0.0.0.0:8080` logged cleanly; PASS.
- **Restart count**: `0`, confirmed immediately after start and again after the verification pass below.
- **Owner session preserved**: the pre-existing paired device record (`owner-device-d915d144...`) remained present and `ACTIVE` throughout, untouched by the redeploy.
- **Enhanced transcription readiness still AVAILABLE**: confirmed live via an authenticated session — `GET /owner/evidence/transcription-readiness` returned `{"status":"READY","message":"Enhanced transcription is available."}`.
- **Process document still works**: `GET /owner/evidence/{id}/acquisition` against a real, existing evidence document returned the governed decision cleanly (`NO_ELIGIBLE_CAPABILITY`, reasons including `EXTERNAL_EGRESS_NOT_AUTHORISED` — the expected pre-authorization state).
- **When external authorization is missing, the UI/API surfaces it correctly**: `GET /owner/evidence/{id}/authorize-enhanced-transcription` (the status the UI's "Authorize enhanced transcription" affordance is driven from) returned `{"status":"NOT_AUTHORISED", ...}` for that same real, unauthorized document — the authorization lane is live and configured (not `UNAVAILABLE`/`AUTHORIZATION_LANE_NOT_CONFIGURED`).
- **Authorization confirmation requires high-authority verification, and fails closed**: verified live in production — `POST /owner/evidence/{id}/authorize-enhanced-transcription` with a deliberately wrong credential returned HTTP `403`, `{"status":"NOT_AUTHORISED","detail":"HIGH_AUTHORITY_VERIFICATION_FAILED"}`, and a follow-up status check confirmed no record was created (still `NOT_AUTHORISED`, `detail: null`).
- **Run enhanced transcription remains disabled before authorization**: the served page's button-state logic (unchanged from the UI-INGESTION-5 candidate, confirmed by JAR digest match) requires both readiness `READY` and per-document authorization `AUTHORISED`; with no authorization present for any document, the button remains disabled.
- **Creating authorization does not invoke a provider / no evidence-or-provider mutation from authorization alone**: verified structurally and by the existing, already-passing test suite rather than by a fresh live successful authorization in this unit — see "Scope decision" below.
- **After exact-target authorization, Run enhanced transcription becomes enabled**: verified by the same existing, already-passing test suite (`ExternalTranscriptionOwnerAuthorizationCoordinatorTest`, `GovernedAcquisitionOwnerPresentationTest`) and by UI-INGESTION-5A's independent code trace, not re-exercised live in this unit — see "Scope decision" below.

## Scope decision: no live successful authorization was created

UI-INGESTION-5A's governance review (Finding 2) established that this mechanism currently has **no revocation path** — an authorization, once created, is permanent. Creating a real, successful authorization against a real evidence document during this deployment-verification unit would therefore have produced permanent state on the owner's actual evidence, to verify a property (successful-authorization → enabled execution, no-provider-call) that was already rigorously proven by:

- `ExternalTranscriptionOwnerAuthorizationCoordinatorTest` (10/10 passing, including an explicit assertion that no provider dependency is reachable from `authorize()`, and a passing "after authorization the governed decision re-evaluation sees it authorised" test);
- `GovernedAcquisitionOwnerPresentationTest`'s dynamic-egress-authorization tests;
- UI-INGESTION-5A's independent structural trace (no reference to `ExternalTranscriptionMechanism`, any transport, or any provider adapter anywhere in the authorization coordinator's dependency graph).

The owner was asked whether to proceed with a live, real (and currently irreversible) authorization against a specific document for additional confirmation, and explicitly chose to skip it and rely on the above. This unit therefore verified the **fail-closed** path live (wrong credential → `403`, no record) and the **pre-authorization** state live (both "Process document" and the authorization-status endpoint), but did not create any authorization record. `/mnt/parker-data/parker/external-transcription-authorizations` remains empty.

## Separation of authorization and execution

Confirmed unchanged from the reviewed candidate: authorization creation (`ExternalTranscriptionOwnerAuthorizationCoordinator.authorize`) and provider execution (`ExternalTranscriptionOwnerInvocationCoordinator.invoke`, reached only via `transcribeExternal`/`Run enhanced transcription`) remain two structurally independent code paths, gated separately, with execution requiring both readiness and a pre-existing authorization record. `Run enhanced transcription` was not clicked and no evidence was transcribed during this unit.

## Provider accounting

- Provider calls: `0`
- Retries: `0`
- External evidence egress: `0`

## Result

The exact owner-accepted UI-INGESTION-5 candidate is running in production with matching image, source, and JAR identities; restart count 0; the owner's paired session and existing evidence/human-review/correction stores untouched; enhanced transcription readiness AVAILABLE; and the authorize/execute separation intact and fail-closed on invalid high-authority verification, confirmed live. No provider was invoked and no evidence document was transcribed.
