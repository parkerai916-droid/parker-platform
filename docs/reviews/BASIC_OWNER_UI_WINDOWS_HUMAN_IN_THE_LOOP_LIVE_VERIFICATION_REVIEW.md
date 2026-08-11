# Basic Owner UI Windows Human-in-the-Loop Live Verification Review

Date: 2026-08-11 (Pacific/Auckland)

## Classification

**C — PARTIALLY VERIFIED.** The human owner observed the real runtime-backed Parker window, truthful Parker Runtime/Ready presentation, and available message controls. Technical logs independently proved real `ParkerRuntime` startup and normal owner-driven shutdown. The one owner message was typed but deliberately not submitted because available RAM fell to 1.308 GiB before inference. Consequently the submission, governed runtime/model path, notification bridge, and reply rendering remain unexercised.

The stop protected the hard resource boundary. It is not evidence of a Parker implementation defect or model failure. The selected model remains a Windows-local **TEST FIXTURE ONLY — NOT PARKER PRODUCTION MODEL SELECTION**.

## Governing authority and baseline

This task authorized one human-operated Windows live verification using the real Basic Owner UI and real runtime composition. It prohibited shell/UI automation, direct HTTP or runtime substitutes, source/test/Gradle changes, Parker server access, Unit 3-C, paused-remedy work, model qualification, deployment, push, merge, and rebase.

- Repository: `C:\Projects\Parker\parker-platform\parker-platform`.
- Branch: `ui/basic-owner-interface-integration`.
- HEAD: `a481c7f518641dfd367a163f8ad9636aeeeecacb`.
- Fetched `origin/main`: `bfa618bece577408b247f76454836947f7257197`.
- Initial state: clean, 15 commits ahead of `origin/main`, nothing staged, no active merge/rebase/cherry-pick.
- Initial fixture state: no Ollama/Llama process and no port 11434 listener.
- Initial available RAM: 2,355,458,048 bytes (about 2.194 GiB), passing the 2.0 GiB gate.

Fresh inspection covered the prior live-execution reviews, fixture selection/installation reviews, blocker-resolution scope/completion reviews, diagnostic-boundary review, and the current real launcher/runtime/notification/model code. The prior non-interactive live attempt remains correctly recorded as not verified; it was not silently replaced or reinterpreted.

## Current implementation and configuration

The real launcher is `:ui-desktop:runOwnerUi` (`parker.ui.OwnerUiMainKt`). Its path remains:

`ParkerOwnerWindow` → `OwnerUiController` → `OwnerUiRuntimeAdapter` → `ParkerRuntime.submitOwnerMessage` → governed runtime → `LocalHttpModelInferenceClient` → loopback Ollama → tagged parser/outcome handling → `OwnerNotificationSink` → `OwnerUiNotificationBridge` → `OwnerReply` → UI transcript.

`PARKER_RUNTIME` presentation is supplied only after `OwnerUiRuntimeSession.start()` returns Ready. The adapter exposes only `runtime::submitOwnerMessage`, maps raw runtime failures/rejections to fixed owner-safe wording, and has no direct model, Memory, Goals, Planner, tool, Unit 3-C, or remedy-program capability.

The launch environment resolved exclusively to:

- `PARKER_MODEL_ENDPOINT_URL=http://127.0.0.1:11434/api/generate`;
- `PARKER_MODEL_NAME=qwen2.5:0.5b-instruct-q4_K_M`;
- `PARKER_MODEL_TIMEOUT_MS=120000`;
- owner principal `user.owner-ui-human-live`, display `Owner`;
- isolated evidence/audit/memory paths below `C:\Users\steve\AppData\Local\Parker\owner-ui-human-live`;
- `PARKER_LOG_LEVEL=INFO`.

No Parker server or remote endpoint was configured or accessed.

## Offline prerequisite

Microsoft OpenJDK 17.0.19 and Gradle 8.10 ran the established bounded suite: **47 tests across 7 classes; 47 passed, 0 failed, 0 errors, 0 skipped**.

| Suite | Tests |
|---|---:|
| `OwnerUiControllerTest` | 12 |
| `OwnerWindowPresentationTest` | 8 |
| `OwnerUiOutcomeCompletenessTest` | 5 |
| `OwnerUiRuntimeAdapterTest` | 10 |
| `OwnerUiRuntimeCompositionTest` | 6 |
| `OfflineOwnerUiLauncherIsolationTest` | 2 |
| `OfflineOwnerInteractionTest` | 4 |

No Unit 3-C, live-model diagnostic, or Reasoning Protocol campaign task ran.

## Fixture identity and RAM gates

Native Windows Ollama 0.32.8 started with exactly one listener at `127.0.0.1:11434`. Non-generative `/api/version` and `/api/tags` checks found exactly:

- model `qwen2.5:0.5b-instruct-q4_K_M`;
- size 397,821,319 bytes;
- digest `a8b0c51577010a279d933d14c2a8ab4b268079d44c5c8830c0a93900f1827c67`.

No model was pulled or generated during fixture verification. Available RAM immediately before Parker launch was 2,283,372,544 bytes (about 2.127 GiB), passing the launch gate.

## Launch evidence and human observation

Technical evidence:

- the real `:ui-desktop:runOwnerUi` task launched;
- `Runtime starting` at `2026-08-11T08:48:08.970683500Z`;
- `Runtime started` at `2026-08-11T08:48:09.438450100Z`;
- no immediate runtime startup failure.

Human-observed evidence, reported by Steve after being told not to submit:

- the Parker window was visible: **yes**;
- visible wording: faithfully normalized from Steve's report as **“Owner conversation · Parker Runtime”** and **“Ready”**;
- the owner message input and Send control were available: **yes**.

Steve reported no offline-preview wording, endpoint/model/server/production claim, raw exception, or arbitrary diagnostic text. That establishes the pre-submission presentation boundary only to the extent of his reported visual observation.

## Submission safety halt

Steve was instructed to type exactly `Hello Parker.` without submitting. He confirmed the text was entered. Immediately before authorizing submission, technical monitoring recorded:

- timestamp `2026-08-11T20:51:35.0608126+12:00`;
- available RAM 1,403,985,920 bytes (about **1.308 GiB**);
- prior `/api/generate` calls in this run: 0.

That memory state was plainly below the governed 2.0 GiB safety threshold and left insufficient margin for the already-observed model-load working set. Submission authorization was withheld. Steve did not press Enter or Send.

- Owner submissions: **0**.
- `/api/generate` calls: **0**.
- Model loading/inference in this run: **none**.
- `ParkerRuntime.submitOwnerMessage` traversal: **not exercised**.
- Governed outcome: **none**.
- `OwnerNotificationSink` / `OwnerUiNotificationBridge` / `OwnerReply`: **not exercised**.
- Parker reply or System outcome rendering: **not exercised**.
- Bypass: **none**.

No post-submission visual claim is made because no submission occurred.

## Shutdown and isolation

Steve was explicitly told to close the window using the normal visible close control and confirmed it closed. Technical logs then recorded at the same timestamp:

- `Runtime shutting down`;
- `Runtime stopped`.

The `:ui-desktop:runOwnerUi` Gradle task completed successfully, and every UI/Gradle Java process exited normally. No forced UI teardown was used. Ollama was then stopped; no `llama-server.exe` existed because inference never began.

Final checks found:

- no relevant Java process;
- no Ollama/Llama process;
- no listener on port 11434;
- final available RAM 2,264,817,664 bytes (about 2.109 GiB).

## Integrity, limitations, and merge readiness

- Parker server accessed: **NO**.
- Unit 3-C touched: **NO**.
- Paused remedy programme touched: **NO**.
- Production reasoning/runtime/UI/test/Gradle semantics changed: **NO**.
- Model qualified: **NO**.
- Staged/committed/pushed/merged/rebased: **NO**.

This attempt verifies more than the prior non-interactive run: the human-visible real runtime Ready window and normal owner-driven shutdown are now demonstrated. It does not verify the essential message-to-model-to-notification-to-rendering path.

**Merge readiness: NOT READY FOR FINAL MERGE REVIEW.** Exact next step: restore sufficient memory margin, then repeat the same human-in-the-loop procedure from a clean baseline. Require at least 2.0 GiB not only before Ollama/Parker launch but again immediately before authorizing the one owner submission. Use the same loopback fixture, exact model/digest, one-message/no-retry boundary, and no source changes.
