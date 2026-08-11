# Basic Owner UI Live Verification Blocker Resolution Scope Lock

**Status: PROPOSED — PENDING BOUNDARY / INDEPENDENT REVIEW**

Date: 2026-08-11 (Pacific/Auckland)

## 1. Baseline

- Repository: `C:\Projects\Parker\parker-platform\parker-platform`
- Branch: `ui/basic-owner-interface-integration`
- Baseline: `3f485fd070024b18203d408f6ac53d6daf3bd89b`
- Baseline subject: `governance: plan owner UI live verification blocker resolution`
- `origin/main`: `bfa618bece577408b247f76454836947f7257197`
- Starting worktree: clean

This lock governs only the prerequisites identified by the committed Windows live-verification reviews and the blocker-resolution Planning Review.

## 2. Frozen problem statement

Exactly three prerequisites exist and must remain distinct:

### A. Implementation blocker — truthful presentation mode

The shared window currently labels both offline and real-runtime modes as a deterministic offline preview. The real launcher must truthfully identify the Parker Runtime path without claiming model health, production parity, or server connectivity.

### B. Implementation blocker — owner-safe status/error presentation

The adapter currently passes arbitrary `ParkerRuntimeOutcome.Failed.cause.message` and untyped `ParkerRuntimeOutcome.NotAccepted.reason` into owner-visible System transcript entries. Arbitrary runtime diagnostic text must not be displayed directly to the owner.

### C. Environment prerequisite — Windows-local compatible endpoint

Later live verification requires a separately authorized and configured Windows-local model service compatible with Parker’s current endpoint and tagged-response contracts. This is not a code correction and must not be collapsed into A or B.

## 3. Presentation-mode scope

### 3.1 Closed model

Future implementation shall add one closed presentation-only mode with exactly two cases:

- `OFFLINE_PREVIEW`
- `PARKER_RUNTIME`

The mode carries no runtime capability, provider identity, endpoint, health signal, authority, or configuration. It selects truthful copy only.

### 3.2 Binding wording

| Path/state | Owner-visible wording | Frozen truth claim |
|---|---|---|
| Offline Ready | Header: `Owner conversation · deterministic offline preview` | Uses the scripted offline interaction only |
| Offline empty state | `Messages in this preview use Parker’s deterministic offline interaction.` | No real runtime/model path |
| Real Starting | `Starting Parker Runtime...` | Startup attempt is in progress; no readiness/model claim |
| Real startup/config failure | `Parker could not start` plus the existing fixed safe startup status | Runtime did not reach Ready |
| Real Ready | Header: `Owner conversation · Parker Runtime` | `ParkerRuntime.start()` succeeded and `OwnerInteraction` is available |
| Real Ready empty state | `Messages are submitted through the local Parker Runtime.` | Messages use the real runtime boundary; no model-health claim |
| Runtime unavailable/stopped | Existing Stopped state plus fixed safe status | Runtime is not accepting submissions |

Forbidden wording includes `model connected`, `model online`, `production`, `server`, provider/model names, Unit 3-C terms, remedy terms, and any statement that inference has succeeded before a reply/outcome proves it.

### 3.3 Presentation files

Expected production files:

- `src/ui/parker/ui/OwnerWindowPresentation.kt`
- `ui-desktop/src/main/kotlin/parker/ui/ParkerOwnerWindow.kt`
- `ui-desktop/src/main/kotlin/parker/ui/OfflineOwnerUiMain.kt`
- `ui-desktop/src/main/kotlin/parker/ui/OwnerUiMain.kt`

No controller, runtime coordinator, model client, Gradle, or packaging change is authorized for mode labelling.

## 4. Diagnostic/owner-safe boundary

### 4.1 Binding rule

**ARBITRARY RUNTIME DIAGNOSTIC TEXT MUST NOT BE DISPLAYED DIRECTLY TO THE OWNER.**

Forbidden owner-visible material includes:

- `Throwable.message` or `cause.message`;
- stack traces and exception class names;
- raw provider response/model output used as diagnostic evidence;
- endpoint, model, credential, filesystem, component, resource, principal, or channel details;
- raw `NotAccepted.reason` unless a future separately governed type explicitly guarantees owner safety; and
- any developer-oriented coordinator/parser/HTTP failure message.

### 4.2 Permitted output model

Permitted owner-visible System text is allowlisted and derived only from existing typed outcomes/stages. Detailed causes remain in existing runtime logs with their existing correlation IDs. The UI shall not add a new logging system, error registry, diagnostic store, correlation lookup, or exception hierarchy.

The current `OwnerInteraction` contract does not expose a governed correlation/reference ID. No correlation ID shall be shown in this correction. The runtime continues to preserve it in logs. Adding an owner-visible reference requires separate design because it would expand the presentation contract.

### 4.3 Binding safe-status matrix

| Runtime/UI condition | Owner-visible category | Binding owner-visible text | Diagnostic handling | Parker speech? | Correlation shown? | Retry guidance |
|---|---|---|---|---|---|---|
| Connecting/Starting | STARTING | `Starting Parker Runtime...` | Startup detail remains in logs | No | No | No |
| Ready | READY | `Ready` plus mode wording in Section 3 | None | No | No | No |
| Unavailable/NotRunning | STOPPED | `Parker Runtime is not accepting messages.` | Lifecycle state remains internal/logged | No | No | No fixed advice |
| Delivered with reply | READY | No System completion text | Execution detail logged | **Yes, only sink-delivered reply** | No | No |
| Delivered without reply | READY | No fabricated reply or System completion text | Execution status remains disposition/log evidence | No | No | No |
| NotAccepted | NOT_ACCEPTED | `Parker did not accept this message.` | Raw reason retained only in runtime logs | No | No | No generic retry claim |
| Planned Completed | PLANNED | `Planned: Completed` | Planning identifiers/details remain logged | No | No | No |
| Planned Rejected | PLANNED | `Planned: Rejected` | Planning reason/details remain outside UI | No | No | No |
| Planned Failed | PLANNED | `Planned: Failed` | Planning reason/details remain outside UI | No | No | No |
| Failed — REASONING, including timeout/provider unavailable/parser fault | ERROR | `Parker could not complete reasoning for this message.` | Real cause/raw response/endpoint detail remains in logs | No | No | No fixed advice |
| Failed — UNKNOWN, including unexpected runtime exception | ERROR | `Parker could not complete this message.` | Real cause/internal/path detail remains in logs | No | No | No fixed advice |
| Startup/configuration failure | STARTUP_FAILED | Existing fixed `Parker Runtime configuration is invalid` or `Parker Runtime could not start` | Actual exception remains outside UI | No | No | No |
| Unexpected adapter/controller exception | ERROR | Existing fixed `Owner interaction failed unexpectedly` | Exception not rendered; developer diagnosis remains external | No | No | No |

Parker-authored reply text is not a status or diagnostic. It may enter a Parker transcript entry only through `OwnerNotificationSink -> OwnerUiNotificationBridge -> OwnerReply`. System/error/status text must never become Parker speech.

### 4.4 Diagnostic correction files

Expected production file:

- `src/composition/OwnerUiRuntimeAdapter.kt`

It shall map Failed stages and NotAccepted to the fixed allowlist above before producing `OwnerSubmissionDisposition`. It shall not modify `ParkerRuntimeOutcome`, `ParkerRuntime`, exception types, conversation outcomes, runtime logging, or `OwnerNotificationSink`.

## 5. Windows-local endpoint verification dependency

The later verification dependency is an already-running Windows-local/loopback-only service implementing the existing production client contract:

- HTTP `POST` to configured `PARKER_MODEL_ENDPOINT_URL`;
- `Content-Type: application/json`;
- request equivalent to `{"model":"<name>","prompt":"<prompt>","stream":false}`;
- response containing the exact compact sequence `"response":"<text>"` understood by `defaultOllamaResponseBody`; and
- extracted text accepted by `TaggedReasoningResponseParser`: `REPLY:<text>`, `GOAL:<text>`, `REMEMBER:<text>`, or exactly `NOACTION`.

Configuration requires `PARKER_MODEL_ENDPOINT_URL`, `PARKER_MODEL_NAME`, owner identity, and new writable Windows-local evidence/audit/memory paths. Timeout is configured by `PARKER_MODEL_TIMEOUT_MS` or its existing 30,000 ms default.

This endpoint/model is a **TEST FIXTURE / VERIFICATION DEPENDENCY**, not a selected Parker production model. Ollama is not required as a product; current production composition requires its compatible wire shape. `qwen2.5-coder:7b` is not required and must not be selected merely to imitate the paused programme. A smaller/different instruction-capable local model is valid if it emits a parsable reply for the one architectural proof.

The future proof shall not claim semantic model qualification, production parity, Reasoning Protocol conformance, remedy efficacy, provider approval, performance readiness, or quality of planning/Memory/Goal/tool behaviour.

No installation, product selection, model download, service start, firewall change, or live call is authorized by this scope lock.

## 6. Frozen implementation surface

Production files: exactly five expected:

| File | Classification |
|---|---|
| `src/ui/parker/ui/OwnerWindowPresentation.kt` | Mode labelling/pure copy mapping |
| `ui-desktop/src/main/kotlin/parker/ui/ParkerOwnerWindow.kt` | Mode-aware presentation |
| `ui-desktop/src/main/kotlin/parker/ui/OfflineOwnerUiMain.kt` | Offline launcher supplies offline mode |
| `ui-desktop/src/main/kotlin/parker/ui/OwnerUiMain.kt` | Real launcher supplies runtime mode only after Ready |
| `src/composition/OwnerUiRuntimeAdapter.kt` | Owner-safe status mapping |

Expected tests: exactly six:

- `tests/ui/OwnerWindowPresentationTest.kt`
- `tests/ui/OfflineOwnerUiLauncherIsolationTest.kt`
- `tests/composition/OwnerUiRuntimeCompositionTest.kt`
- `tests/composition/OwnerUiRuntimeAdapterTest.kt`
- `tests/ui/OwnerUiControllerTest.kt`
- `tests/ui/OwnerUiOutcomeCompletenessTest.kt`

Governance/completion review documents may be added separately. No production runtime file outside UI/composition needs modification.

## 7. Frozen offline test requirements

Mode tests shall prove:

- exact offline/deterministic copy;
- exact Parker Runtime copy;
- Starting copy makes no readiness/model claim;
- startup/unavailable copy is fixed and truthful;
- real runtime mode is supplied only to the existing Ready window;
- no production/server/provider/model/Unit 3-C/remedy wording; and
- offline launcher remains structurally isolated.

Diagnostic tests shall prove:

- arbitrary `Throwable.message` never enters a disposition/transcript;
- Windows and Unix filesystem paths never render;
- endpoint/model/credential details never render;
- raw provider responses and unclassifiable model output never render;
- arbitrary `NotAccepted.reason` never renders;
- the exact allowlisted status renders as a System entry;
- no correlation ID is fabricated or exposed; existing injected correlation ID still enters `InboundOwnerMessage` and remains available to runtime logs;
- Parker transcript contains only sink-delivered Parker speech; and
- System/error text never becomes Parker speech.

Regression requirements:

- all existing 45 UI/runtime-boundary tests plus added/updated tests;
- relevant runtime failure-handling/conversation tests;
- full ordinary suite with exact counts;
- root build;
- `ui-desktop` compile/JAR/build; and
- proof ordinary lifecycle remains detached from live-model, Unit 2-D diagnostic, and Unit 3-C tasks.

## 8. Live-retest prerequisites

Live verification may not be retried until all are true:

1. mode correction implemented under separate authorization and verified;
2. diagnostic boundary correction implemented under separate authorization and verified;
3. all offline tests/builds green;
4. Windows-local compatible endpoint/model separately authorized, available, and disclosed as a verification fixture;
5. Windows-local durability paths isolated from repository campaign and server storage;
6. clean integration branch freshly reconciled with current `origin/main`;
7. no Parker-server dependency;
8. no Unit 3-C/remedy dependency; and
9. a separate live-verification execution authorization.

## 9. Exclusions and stop boundary

This lock does not authorize implementation, endpoint provisioning, model choice, live verification, runtime/error/logging redesign, new UI features, production reasoning changes, Memory/Goal/Planner/Knowledge/tool changes, Unit 3-C work, remedy work, merge, deployment, commit, or push.

Stop after the Scope Lock, focused Boundary Review, and independent review.
