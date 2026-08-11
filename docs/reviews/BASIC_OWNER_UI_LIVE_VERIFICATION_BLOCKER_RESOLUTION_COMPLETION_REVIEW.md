# Basic Owner UI Live Verification Blocker Resolution Completion Review

Date: 2026-08-11 (Pacific/Auckland)

## Status

**IMPLEMENTATION COMPLETE — READY FOR LIVE RETEST ONCE THE SEPARATELY AUTHORIZED WINDOWS-LOCAL ENDPOINT PREREQUISITE EXISTS.**

This unit implements only blocker A (truthful presentation mode) and blocker B (owner-safe status presentation). It did not install, start, configure, or call a model service and did not perform live verification.

## Baseline

- Repository: `C:\Projects\Parker\parker-platform\parker-platform`
- Branch: `ui/basic-owner-interface-integration`
- Starting HEAD: `9192e683dc0974a39670d3cf650d8a6ce67b076b`
- Starting subject: `governance: freeze owner UI live verification blocker resolution`
- `origin/main`: `bfa618bece577408b247f76454836947f7257197`
- Starting worktree: clean
- Uncached pre-edit gate: 45 tests, 45 passed, 0 failed, 0 errors, 0 skipped

## Exact files changed

Production, exactly the five files frozen by the Scope Lock:

- `src/ui/parker/ui/OwnerWindowPresentation.kt`
- `ui-desktop/src/main/kotlin/parker/ui/ParkerOwnerWindow.kt`
- `ui-desktop/src/main/kotlin/parker/ui/OfflineOwnerUiMain.kt`
- `ui-desktop/src/main/kotlin/parker/ui/OwnerUiMain.kt`
- `src/composition/OwnerUiRuntimeAdapter.kt`

Tests, exactly the six files frozen by the Scope Lock:

- `tests/ui/OwnerWindowPresentationTest.kt`
- `tests/ui/OfflineOwnerUiLauncherIsolationTest.kt`
- `tests/composition/OwnerUiRuntimeCompositionTest.kt`
- `tests/composition/OwnerUiRuntimeAdapterTest.kt`
- `tests/ui/OwnerUiControllerTest.kt`
- `tests/ui/OwnerUiOutcomeCompletenessTest.kt`

Review records:

- `docs/reviews/BASIC_OWNER_UI_LIVE_VERIFICATION_BLOCKER_RESOLUTION_COMPLETION_REVIEW.md`
- `docs/reviews/BASIC_OWNER_UI_LIVE_VERIFICATION_BLOCKER_RESOLUTION_COMPLETION_INDEPENDENT_REVIEW.md`

No runtime/core, Gradle, server, campaign, Unit 3-C, or provider file changed.

## Presentation-mode implementation

`OwnerWindowPresentationMode` is a closed, copy-only enum with exactly `OFFLINE_PREVIEW` and `PARKER_RUNTIME`. It contains no endpoint, provider, health, authority, or runtime capability.

The offline launcher supplies only `OFFLINE_PREVIEW`. Its header is `Owner conversation · deterministic offline preview`; its empty state says messages use Parker's deterministic offline interaction.

The real launcher supplies `PARKER_RUNTIME` only inside the existing `OwnerUiLaunchState.Ready` branch. Its header is `Owner conversation · Parker Runtime`; its empty state says messages are submitted through the local Parker Runtime. Starting remains `Starting Parker Runtime...`; failure remains `Parker could not start` plus the existing fixed startup-safe message. No copy claims model availability, inference success, production/server parity, provider identity, or Reasoning Protocol status.

## Owner-safe status implementation

`OwnerUiRuntimeAdapter.mapOutcome` now replaces every raw `NotAccepted.reason` and every `Failed.cause.message` with fixed text. It does not inspect, filter, truncate, or conditionally pass through diagnostic content.

| Condition | Fixed adapter output | Rendered owner System status |
|---|---|---|
| Starting | existing launch state | `Starting Parker Runtime...` |
| Ready | existing ready state | `Ready` plus mode copy |
| Unavailable / NotRunning | `Parker Runtime is not accepting messages` | `Stopped: Parker Runtime is not accepting messages` |
| Delivered with sink reply | no completion status | sink-delivered Parker reply only |
| Delivered without reply | no completion status | no fabricated reply/status |
| NotAccepted | `Parker did not accept this message.` | `Not accepted: Parker did not accept this message.` |
| Planned Completed | `Completed` | `Planned: Completed` |
| Planned Rejected | `Rejected` | `Planned: Rejected` |
| Planned Failed | `Failed` | `Planned: Failed` |
| Failed — REASONING | `Parker could not complete reasoning for this message.` | `Failed (REASONING): Parker could not complete reasoning for this message.` |
| Failed — any other stage / UNKNOWN | `Parker could not complete this message.` | `Failed (<stage>): Parker could not complete this message.` |
| Startup/configuration failure | existing fixed launch message | `Parker Runtime configuration is invalid` or `Parker Runtime could not start` |
| Unexpected adapter/controller exception | existing controller fallback | `Owner interaction failed unexpectedly` |

The `Not accepted:`, `Failed (<stage>):`, and `Stopped:` wrappers are pre-existing typed controller presentation and were deliberately retained because controller redesign was outside the frozen production surface. All variable content entering those wrappers is now allowlisted. This is the narrowest neutral implementation consistent with the lock.

Detailed causes/reasons remain owned by the unchanged runtime logging path. No logging subsystem, correlation UI, exception type, or runtime outcome changed.

## Verification

- Expanded targeted owner UI/runtime-boundary suite: **47 tests, 47 passed, 0 failed, 0 errors, 0 skipped** across 7 suites.
- Full ordinary repository suite: **2,062 tests, 2,054 passed, 0 failed, 0 errors, 8 skipped** across 147 suites.
- Root `build`: passed.
- `:ui-desktop:compileKotlin`: passed.
- `:ui-desktop:jar`: passed.
- `:ui-desktop:build`: passed.
- Ordinary lifecycle dry run: passed and contained only ordinary root and `ui-desktop` lifecycle tasks; no live-model, Unit 2-D diagnostic, or Unit 3-C task was attached.

Adversarial tests prove that Windows/Unix filesystem paths, endpoint/model detail, credentials, raw provider responses, arbitrary exception messages, and arbitrary `NotAccepted.reason` do not survive adapter mapping. Existing and updated tests prove System status remains typed System text and Parker speech remains exclusive to `OwnerNotificationSink -> OwnerUiNotificationBridge -> OwnerReply`.

## Boundary result

**BOUNDARY CLEAR.** `submitOwnerMessage = runtime::submitOwnerMessage` remains unchanged. `OwnerNotificationSink` and its bridge semantics remain unchanged. No direct model/provider access, Memory, Goals, Planner, Knowledge Submission, tool access, Unit 3-C dependency, remedy dependency, or production-reasoning change exists.

## Remaining live-verification prerequisite

No Windows-local compatible endpoint/model fixture was installed, started, configured, selected, or called. A later, separately authorized retest still requires an already-running loopback service compatible with the existing `LocalHttpModelInferenceClient` request/response and tagged-response contracts, plus isolated writable Windows-local paths. Ollama and `qwen2.5-coder:7b` are not required by this implementation.

This prerequisite is external and blocking for executing the live retest, but non-blocking for accepting these two code corrections.

## Readiness

**READY FOR LIVE RETEST: YES, conditionally on the external endpoint prerequisite and separate execution authorization.** This means architecture-path verification only. It is not model semantic qualification and does not reopen the paused Reasoning Protocol remedy programme.

No model/API calls, server calls, campaign changes, staging, commit, push, merge, or deployment occurred.
