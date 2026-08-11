# Basic Owner UI — Unit 1 Completion Review

**Status:** Complete

## Delivered capability

UI Unit 1 introduces a plain-Kotlin owner-conversation presentation boundary and deterministic offline harness:

- `OwnerInteraction` accepts only owner text, a separate reply receiver, and exposes basic availability.
- `OwnerSubmissionDisposition` represents Delivered, NotAccepted, Failed, Planned, and Unavailable without importing runtime types.
- typed transcript entries structurally distinguish Owner, Parker, and System authorship;
- `OwnerUiController` owns Ready/Processing/Stopped/Error state, blank rejection, single-flight enforcement, outcome presentation, and cancellation-safe shutdown; and
- `OfflineOwnerInteraction` records submissions and executes deterministic scripted reply, delivery, rejection, failure, planning, delay, and unavailable scenarios.

## Files added

- `docs/governance/BASIC_OWNER_UI_UNIT_1_SCOPE_LOCK.md`
- `docs/reviews/BASIC_OWNER_UI_UNIT_1_PLANNING_REVIEW.md`
- `docs/reviews/BASIC_OWNER_UI_UNIT_1_SCOPE_LOCK_REVIEW.md`
- `docs/reviews/BASIC_OWNER_UI_UNIT_1_COMPLETION_REVIEW.md`
- `docs/reviews/BASIC_OWNER_UI_UNIT_1_INDEPENDENT_CONSTITUTIONAL_REVIEW.md`
- `src/ui/parker/ui/OwnerInteraction.kt`
- `src/ui/parker/ui/OwnerUiState.kt`
- `src/ui/parker/ui/OwnerUiController.kt`
- `src/ui/parker/ui/OfflineOwnerInteraction.kt`
- `tests/ui/OwnerUiControllerTest.kt`
- `tests/ui/OfflineOwnerInteractionTest.kt`

## Files modified

- `build.gradle.kts` adds only `src/ui` and `tests/ui` to the existing Kotlin source sets.
- `tests/contracts/OcrStructuralIsolationTest.kt` normalises discovered Windows path separators before comparing the unchanged expected OCR implementer path. This test-only correction was required when full Windows verification exposed its prior platform-specific literal comparison; no OCR production code or asserted boundary changed.

## Tests added

Sixteen deterministic UI tests prove submission routing, blank rejection, controller and fake single-flight enforcement, reply authorship, Delivered-without-reply behaviour, NotAccepted/Failed/Planned/Unavailable mapping, processing visibility, completion recovery, shutdown cancellation, safe unexpected-failure presentation, scripted disposition preservation, stopped fake behaviour, and forbidden-dependency absence.

## Verification

- Targeted: `gradlew.bat test --tests "parker.ui.*" --no-daemon --offline` — passed.
- Full ordinary lifecycle: `gradlew.bat test check build --no-daemon --offline` — passed.
- Final test result: 2,031 tests, 0 failures, 0 errors, 8 skipped, 142 suites.
- JDK: local Windows Microsoft JDK 17.0.19.
- No live-model, baseline-characterisation, diagnostic, or Unit 3-C task was invoked. The ordinary task graph remains detached from `liveModelEvaluation`.

## Exclusion proof

- No Compose plugin, dependency, import, state type, window, rendering, packaging, or installer was added.
- Production `src/ui` imports only Kotlin/JDK concurrency utilities and existing `kotlinx.coroutines` APIs.
- `src/ui` contains no reference to `ParkerRuntime`, `ParkerRuntimeConfig`, runtime coordinators, Permission Engine, Execution Pipeline, model clients, HTTP/network APIs, environment configuration, Memory Core, Evidence Custodian, or Evidence Intelligence.
- No existing runtime, interface, contract, or composition production file was changed.
- `parker.runtime`, contracts, interfaces, and composition contain no dependency on `parker.ui`.
- Reply text can enter a Parker transcript entry only through the `OwnerReply` receiver supplied to `OwnerInteraction.submit`; Delivered contains execution status only.
- Work remains local on `ui/basic-owner-interface`; it has not been committed, pushed, merged, or deployed.

## Explicitly not delivered

No graphical UI, Compose Desktop integration, real `ParkerRuntime` adapter, model/runtime configuration, network access, privileged capability, generic command facade, deployment, packaging, push, or merge exists in this Unit.

**Decision:** UI UNIT 1 ENGINEERING COMPLETE.
