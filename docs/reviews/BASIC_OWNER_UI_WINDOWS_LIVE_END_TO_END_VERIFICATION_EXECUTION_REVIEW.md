# Basic Owner UI Windows Live End-to-End Verification Execution Review

Date: 2026-08-11 (Pacific/Auckland)

## Verdict

**D — NOT VERIFIED.** The real `:ui-desktop:runOwnerUi` launcher constructed and started the real `ParkerRuntime`, but this execution environment had no interactive Windows desktop handle. The Compose window could not be observed or controlled, its Ready presentation could not be verified, and no owner message was submitted. No direct runtime or HTTP substitute was used. The required end-to-end path therefore remains unproven.

This does not reverse the separate fixture result: `qwen2.5:0.5b-instruct-q4_K_M` remains a structurally compatible Windows-local **TEST FIXTURE ONLY — NOT PARKER PRODUCTION MODEL SELECTION**.

## Authority and scope

This attempt was authorized only to launch the real Basic Owner UI on Windows, submit one benign visible owner message if Ready was observed, trace the real governed runtime path, inspect owner-visible safety, and shut down. It did not authorize code repair, a direct-runtime substitute, multiple submissions, Unit 3-C, Parker server access, model qualification, deployment, staging, commit, push, merge, or rebase.

The earlier blocked live-verification history and the fixture installation/preflight history remain intact. This document records only the newly authorized live attempt.

## Baseline

- Repository: `C:\Projects\Parker\parker-platform\parker-platform`.
- Branch: `ui/basic-owner-interface-integration`.
- HEAD: `33700dc4cba7547b7c2189bef0086377d2af5c1c`.
- Fetched `origin/main`: `bfa618bece577408b247f76454836947f7257197`.
- Initial status: clean, 14 commits ahead of `origin/main`; nothing staged.
- No merge, rebase, or cherry-pick operation was active.
- No Ollama/Llama process and no port 11434 listener existed initially.
- Initial available RAM: 2,270,892,032 bytes (about 2.115 GiB), passing the 2.0 GiB gate.
- JDK: Microsoft OpenJDK 17.0.19; Gradle 8.10.

## Fresh implementation inspection

The real launcher is Gradle task `:ui-desktop:runOwnerUi`, main class `parker.ui.OwnerUiMainKt`. It calls `createOwnerUiRuntimeSession(System.getenv())`, which loads `ParkerRuntimeConfig`, constructs one `OwnerUiNotificationBridge`, one real `ParkerRuntime`, and one `OwnerUiRuntimeAdapter` holding only `runtime::submitOwnerMessage`. `OwnerUiRuntimeSession.start()` invokes `runtime.start()` and exposes the interaction only on Ready.

The current path is:

`ParkerOwnerWindow` → `OwnerUiController.submit` → `OwnerUiRuntimeAdapter.submit` → `ParkerRuntime.submitOwnerMessage` → governed conversation/reasoning path → `LocalHttpModelInferenceClient` → configured endpoint → tagged response parser/outcome handling → `OwnerNotificationSink` (`OwnerUiNotificationBridge`) → `OwnerReply` → controller transcript → visible UI.

The UI has no direct provider/model, Memory, Goals, Planner, tool, Unit 3-C, or paused-remedy-programme capability. `PARKER_RUNTIME` presentation is constructed only in `OwnerUiLaunchState.Ready`. In the real adapter, raw runtime rejection reasons and failures are replaced with fixed safe wording before reaching `OwnerUiController`; unexpected exceptions are likewise presented generically. The close path calls `shutdownOwnerUiSession`, which stops controller work before `ParkerRuntime.shutdown()`.

Required configuration was resolved exclusively to:

- endpoint `http://127.0.0.1:11434/api/generate`;
- model `qwen2.5:0.5b-instruct-q4_K_M`;
- timeout `120000` ms;
- owner principal `user.owner-ui-live-retest`, display `Owner`;
- isolated evidence, deletion-audit, and memory paths under `C:\Users\steve\AppData\Local\Parker\owner-ui-live-retest`;
- log level `INFO`.

No Parker server or remote endpoint was configured.

## Offline prerequisite

The targeted command selected six required test classes. Result: **43 tests, 43 passed, 0 failures, 0 errors, 0 skipped**.

| Class | Tests |
|---|---:|
| `OwnerUiControllerTest` | 12 |
| `OwnerWindowPresentationTest` | 8 |
| `OwnerUiOutcomeCompletenessTest` | 5 |
| `OwnerUiRuntimeAdapterTest` | 10 |
| `OwnerUiRuntimeCompositionTest` | 6 |
| `OfflineOwnerUiLauncherIsolationTest` | 2 |

No Unit 2-D, Unit 3-C, campaign, remedy, or unrelated model test ran.

## Fixture and RAM gates

Native Windows Ollama 0.32.8 started with exactly one listener at `127.0.0.1:11434`. Non-generative `/api/version` and `/api/tags` checks confirmed only:

- `qwen2.5:0.5b-instruct-q4_K_M`;
- size 397,821,319 bytes;
- digest `a8b0c51577010a279d933d14c2a8ab4b268079d44c5c8830c0a93900f1827c67`.

No model was pulled or updated. Immediately before the live launcher, available RAM was 2,247,344,128 bytes (about 2.093 GiB), passing the hard gate.

## Live execution

The intended real graphical task `:ui-desktop:runOwnerUi` ran. Its logs recorded:

- `Runtime starting` at `2026-08-11T08:35:06.482195800Z`;
- `Runtime started` at `2026-08-11T08:35:07.074410Z`.

This proves real runtime construction/startup under the real launcher. It does **not** prove that a usable visible Compose window opened: every launched Java process exposed an empty `MainWindowTitle`, and Windows screen capture failed with `The handle is invalid`, demonstrating that the execution shell had no interactive desktop handle. No available tool could observe or operate the native window.

Under the instruction to stop rather than substitute a fake UI, direct HTTP call, or direct `ParkerRuntime` invocation:

- visible Ready transition: unproven;
- `PARKER_RUNTIME` owner-visible wording: unobserved;
- owner message: none;
- owner submissions: **0**;
- live-interaction `/api/generate` calls: **0**;
- `submitOwnerMessage` traversal: not exercised;
- governed conversation/model/outcome traversal: not exercised;
- actual model output: none;
- OwnerNotificationSink/bridge/OwnerReply traversal: not exercised;
- visible owner-facing result and diagnostic-boundary inspection: not performed.

No retry occurred.

## Shutdown

Because no controllable window existed, the intended window-close path could not be invoked or verified. The exact launch processes (`cmd.exe` and three Java processes created by this launch) were forcibly stopped. This is a shutdown qualification and a reason the end-to-end verification cannot pass. Ollama was then stopped; no detached `llama-server.exe` existed because no model inference occurred.

Final checks found no relevant Java/Ollama/Llama process and no port 11434 listener. Final available RAM was 2,214,920,192 bytes (about 2.063 GiB).

## Repository and isolation integrity

No source, test, Gradle, campaign, Unit 3-C, or paused-remedy artifact was changed by live execution. The only intended repository changes are this new execution review and its independent review. Local runtime data and logs were confined outside the repository.

- Parker server accessed: **NO**.
- Unit 3-C touched: **NO**.
- Paused remedy programme touched: **NO**.
- Production reasoning/runtime/tagged-parser semantics changed: **NO**.
- Model calls: **0**.
- Staged/committed/pushed/merged/rebased: **NO**.

## Limitations and merge readiness

The real runtime startup is demonstrated, but the required visible UI, one owner submission, notification path, owner-visible safety, and clean window-driven shutdown are all unproven. The live verification is therefore not partially promoted to success merely because runtime startup logged successfully.

**Merge readiness: NOT ESTABLISHED by live verification.** Exact next step: rerun this same one-message live verification from an execution context with a controllable interactive Windows desktop. Preserve the same fixture/model/digest, loopback endpoint, hard RAM gate, single-submission limit, and no-repair/no-retry discipline. Do not change production code to work around the execution-environment limitation.
