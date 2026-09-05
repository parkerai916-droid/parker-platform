# UI-INGESTION-6 — Compose Existing Fidelity-First Execution Binding Into Owner UI Transcription Path

## Status

IMPLEMENTED — FOCUSED VERIFIED — CANDIDATE BUILT — NOT DEPLOYED

## Root cause

`ParkerRuntime` constructed `externalTranscriptionOwnerInvocationCoordinator` **once at startup** with `executionBinding = null` (the constructor's own default). `OpenAiResponsesExternalTranscriptionAdapter.buildRequestBody` requires a non-null binding whose `profileId` matches the accepted profile whenever the currently loaded profile is fidelity-first (`profile.transcriptionProfileId == FIDELITY_FIRST_TRANSCRIPTION_PROFILE_ID`) — true for this deployment's only configured profile, `openai-fidelity-first-transcription-v1`. Every owner-triggered "Run enhanced transcription" therefore hit `require(binding != null && ...)` and threw `IllegalArgumentException` before `transport.execute(...)` — confirmed: no provider call, no evidence egress, for any invocation.

## Governance finding surfaced during trace (owner-acknowledged, not resolved by this unit)

Tracing "how the previously accepted R6 fidelity-first path created and supplied profileId/requestId/attemptId" surfaced that this exact adapter/profile already underwent a real, governed acceptance-execution (`authority-fa-9.4p-a1-r2-2f5a4813-...`) that called OpenAI once and **failed human fidelity review** — wrong reading order on two pages of a real document. The owner-accepted analysis (`FA_9_4P_A1_STRUCTURAL_FIDELITY_FAILURE_ANALYSIS_AND_REMEDIATION_PLAN.md`) concluded the current profile/schema cannot prove source-order fidelity and that "the remedy therefore requires a new capability/profile and schema version, not an in-place mutation." That remediation (region-anchored transcription) is design-only (`FA_9_4P_A1_REGION_ANCHORED_FIDELITY_ACQUISITION_IMPLEMENTATION_DESIGN.md`) and has not been implemented. This composition fix makes real owner-triggered execution against that same unremediated mechanism possible again. **The owner was presented with this finding directly and explicitly chose to proceed, accepting the known risk**, rather than this being a determination that the finding is resolved or moot. No provider call was made in this unit either way.

## Existing binding mechanism reused

Traced `FidelityFirstAcceptanceCoordinator.invoke(authorityId)` (`src/runtime/FidelityFirstAcceptanceExecution.kt`) — the only place in the codebase that previously constructed an `ExternalTranscriptionExecutionBinding`. It:

- resolves the exact authoritative source once (sha256/byteLength/mediaType);
- builds one `FidelityFirstExecutionIdentity` (a bounded, opaque-identifier-validated value type) from that source plus the accepted profile's fields (`modelSelectionRule`, `transcriptionProfileId`, `instructionSha256`, `structuredSchemaSha256`, `processingProfileIdentity`) and the embedded repository commit;
- tracks the attempt through `FileSystemFidelityFirstAttemptLedger`/`FidelityFirstAttemptTracker` (one provider attempt per execution identity, enforced via a locked, checksummed, atomically-replaced ledger — "provider attempt already started; automatic second request prohibited");
- constructs one **fresh** `ExternalTranscriptionOwnerInvocationCoordinator` per invocation, bound to that identity's `ExternalTranscriptionExecutionBinding(safeRequestId, safeAttemptId, profileId)`.

This is the governed mechanism reused. What is specific to the one-time acceptance bootstrap — a pre-issued `FidelityFirstAcceptanceAuthority` and the `lifecycle() == ACCEPTANCE_PENDING` gate — is deliberately **not** reused: that authority/lifecycle gate exists to bound a one-time proof execution before global acceptance, not ordinary post-acceptance execution, and reusing it would have required either inventing a new per-click authority-issuance ceremony (redesigning the binding) or bypassing the authority requirement outright (weakening it). Neither was done.

## Minimum fix

`ParkerRuntime.invokeExternalTranscriptionAsOwner` now calls a new private method, `invokeExternalTranscriptionWithFreshBinding`, mirroring `FidelityFirstAcceptanceCoordinator.invoke`'s exact shape: resolve the evidence manifest once, build one `FidelityFirstExecutionIdentity` via the new `OrdinaryFidelityFirstExecutionIdentity.create(...)` (extracted to `FidelityFirstAcceptanceExecution.kt`, reusing the identical `FidelityFirstExecutionIdentity` type — no new identity semantics), track it through the **same** `FileSystemFidelityFirstAttemptLedger` instance the acceptance-bootstrap path already uses (promoted from a local `buildAndRegisterRuntimeGraph` value to a field so both call sites share it — not a second, parallel ledger), and construct one fresh `ExternalTranscriptionOwnerInvocationCoordinator` bound to `identity.toExecutionBinding()`.

`requestId`/`attemptId`/`executionId` are fresh `UUID.randomUUID()` values generated server-side on every call — never owner/browser-supplied, never reused across evidence artifacts or separate attempts. `profileId` is read directly from the currently loaded, accepted profile (`readyProfile.profile.transcriptionProfileId`), so it is always exactly consistent with what `buildRequestBody` checks against. If the ledger (`PARKER_FIDELITY_FIRST_ATTEMPT_STORAGE_ROOT`) is not configured — true in this deployment today — invocation fails closed (`AdmissionFailed("EXECUTION_BINDING_UNAVAILABLE")`) rather than proceeding without call-budget enforcement; this unit does not configure or deploy that storage root.

The pre-existing owner-authorization gate (`externalTranscriptionAuthorizationCoordinator?.isAuthorized(...)`, from UI-INGESTION-5) is unchanged and still runs first, unconditionally, before the binding is ever constructed.

## Files changed

- `src/composition/ParkerRuntime.kt` — promoted `openAiTransport` and the fidelity-first attempt ledger to fields; added `invokeExternalTranscriptionWithFreshBinding` and wired it in place of the old fixed-binding coordinator call.
- `src/runtime/FidelityFirstAcceptanceExecution.kt` — added `OrdinaryFidelityFirstExecutionIdentity.create(...)` and the `FidelityFirstExecutionIdentity.toExecutionBinding()` extension (pure, side-effect-free; no existing class modified).
- `tests/composition/FidelityFirstExternalTranscriptionTest.kt` — two new regression tests closing a real pre-existing gap: neither existing adapter test file previously exercised the fidelity-first profile with a missing or mismatched binding.
- `tests/runtime/OrdinaryFidelityFirstExecutionIdentityTest.kt` — new, covering the binding-construction properties directly.

## Focused tests

61/61 passed, 0 failed:

- `OrdinaryFidelityFirstExecutionIdentityTest` (4, new): non-null binding with exact profileId match; requestId/attemptId carried through unchanged and bounded-opaque-shaped; fresh UUIDs per call (never static/shared, never client-supplied); a wrong profileId is carried through exactly as given (not silently corrected — the adapter is what fails it closed).
- `FidelityFirstExternalTranscriptionTest` (6, incl. 2 new): reproduces the exact reported defect — missing binding and mismatched-profileId binding both throw `IllegalArgumentException` with **zero transport calls**, against the real adapter with the real fidelity-first profile.
- `OpenAiResponsesExternalTranscriptionAdapterTest` (18, unchanged): all pass — no adapter behavior changed.
- `ExternalTranscriptionOwnerInvocationCoordinatorTest` (7, unchanged): all pass — the coordinator class itself was not modified.
- `ExternalTranscriptionOwnerAuthorizationCoordinatorTest` (12, unchanged): all pass — confirms the separate owner-authorization gate (item 8) still runs, untouched.
- `FidelityFirstAcceptanceCoordinatorTest` (9, unchanged): all pass — the one-time acceptance-bootstrap path is undisturbed.
- `HumanCorrectionPermissionPolicyTest` (5, unchanged): all pass — human-review/correction high-authority semantics unaffected (item 10).

No test constructed a real `JdkOpenAiResponsesTransport` pointed at a live endpoint or reached `transport.execute(...)`; every test either used a `FakeTransport` or asserted failure strictly before transport.

## Provider accounting

- Provider calls: `0`
- Retries: `0`
- External evidence egress: `0`

## Candidate artifact

- Source commit: `1e52fac7c18fd914c2e7504050706e6f2a5a3a4b`
- Image ID: `sha256:96a54f465bf6e03e6ffa7233de8385d4b1224dc912784b2fc707f3cd534f6f86`
- Runtime JAR SHA-256: `1493674957b332ba4b31de2c0fe95aa9098c182bf514fd8199df8f11a0df01e0`
- Local tag: `parker-platform:ui-ingestion-6-1e52fac7`

Built with `docker buildx build --pull=false --provenance=false --platform linux/amd64 --build-arg PARKER_BUILD_COMMIT=<commit> --load .`

## Production unchanged

Verified before and after the candidate build: `parker-runtime` still runs `sha256:a31298ae4bb2b8c172850ca4615eb7af7f8b43654da192ade9c1a229854749da` (UI-INGESTION-5E), restart count `0`. Not deployed under this unit. Note that even once deployed, this fix alone does not make real execution live: `PARKER_FIDELITY_FIRST_ATTEMPT_STORAGE_ROOT` remains unconfigured in this deployment, so `invokeExternalTranscriptionAsOwner` will continue to fail closed (`EXECUTION_BINDING_UNAVAILABLE`) until a future deployment unit also wires that storage root — a deliberate, additional gate, not an oversight.
