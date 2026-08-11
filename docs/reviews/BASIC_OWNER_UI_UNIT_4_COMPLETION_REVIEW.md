# Basic Owner UI Unit 4 Completion Review

## Outcome

UI Unit 4 implements the real composition-layer adapter without constructing or executing a real ParkerRuntime graph. The frozen Units 1-3 UI capability and authorship model remain unchanged.

## Adapter architecture

`OwnerUiRuntimeAdapter` implements `OwnerInteraction` under `parker.composition`. Its only runtime-facing dependency is the explicitly named function capability:

`suspend (InboundOwnerMessage) -> ParkerRuntimeOutcome`

Production composition may supply `runtime::submitOwnerMessage`; the adapter never receives ParkerRuntime and therefore cannot reach any other public runtime method. `OwnerUiNotificationBridge` implements `OwnerNotificationSink` and binds the single active UI reply receiver for the duration of one submission.

## Files added in Unit 4

- `docs/governance/BASIC_OWNER_UI_UNIT_4_SCOPE_LOCK.md`
- `docs/reviews/BASIC_OWNER_UI_UNIT_4_PLANNING_REVIEW.md`
- `docs/reviews/BASIC_OWNER_UI_UNIT_4_BOUNDARY_AND_CAPABILITY_REVIEW.md`
- `docs/reviews/BASIC_OWNER_UI_UNIT_4_SCOPE_LOCK_REVIEW.md`
- `src/composition/OwnerUiRuntimeAdapter.kt`
- `tests/composition/OwnerUiRuntimeAdapterTest.kt`
- `docs/reviews/BASIC_OWNER_UI_UNIT_4_COMPLETION_REVIEW.md`
- `docs/reviews/BASIC_OWNER_UI_UNIT_4_INDEPENDENT_CONSTITUTIONAL_REVIEW.md`

No pre-existing file was modified for Unit 4.

## Message construction

Each accepted call constructs exactly one `InboundOwnerMessage` with:

- exact, untrimmed owner text;
- the composition-configured owner `PrincipalId`;
- the composition-configured local-text `ModuleId`;
- the injected clock value;
- one newly supplied `CorrelationId`; and
- default empty metadata.

The narrow submit capability is invoked exactly once. Identity cannot be supplied by the UI.

## Outcome and reply mapping

- Delivered -> Delivered with `ExecutionResult.status.name`; no reply text is created.
- NotAccepted -> NotAccepted with unchanged reason.
- Failed -> Failed with `PipelineStage.name` and the available permitted cause message, or a fixed fallback when absent.
- Planned -> Planned with an exhaustive Completed/Rejected/Failed planning-session category.
- NotRunning -> Unavailable with safe stopped text; adapter availability remains Stopped and later calls do not reach submission.
- `OwnerNotificationSink.notify(text)` -> exactly one `OwnerReply(text)` delivered through the active UI callback.

Direct concurrent adapter use fails before a second submit call. No correlation-based concurrency was added.

## Capability and lifecycle proof

- Neither production type declares a ParkerRuntime field.
- The source contains no evidence method, lifecycle call, HTTP/network import, model client, or ParkerRuntime construction.
- The adapter calls no PermissionEngine, ReasoningProvider, ConversationEngine, ResponseComposer, ResponseDelivery, ExecutionPipeline, Tool, Memory, Knowledge, or Evidence API.
- The adapter does not call `start()` or `shutdown()`.
- `Main.kt` and the existing CLI/headless responsibility are unchanged.
- A real graphical launcher was deliberately deferred to Unit 5; `OfflineOwnerUiMain` remains the only UI launcher and safe development default.

## Verification

- Focused adapter suite: 10 deterministic tests passed.
- Full offline lifecycle: root `test`, `check`, `build` and isolated `ui-desktop:check`, `ui-desktop:build` passed.
- Inventory: 2,054 tests, 146 suites, 0 failures, 0 errors, 8 skipped.
- `git diff --check`: passed; informational line-ending warnings apply only to prior files.
- Branch: `ui/basic-owner-interface`.

No real runtime/model execution occurred. No HTTP endpoint, Ollama, Qwen, Parker server, Ubuntu environment, live-model task, or Unit 3-C task/artefact was accessed. No deployment, commit, push, or merge occurred.

## Verdict

Unit 4 is complete. The adapter exposes exactly one governed conversation submission capability and preserves notification-only reply authorship, offline verification, lifecycle separation, CLI behavior, and Compose isolation.

