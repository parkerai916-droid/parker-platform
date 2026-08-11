# Basic Owner UI Windows Human-in-the-Loop Live Verification Attempt 2 Review

Date: 2026-08-11 (Pacific/Auckland)

## Classification

**B — VERIFIED WITH QUALIFICATIONS.** The real production `parker.ui.OwnerUiMainKt` was launched directly with Microsoft OpenJDK 17 and the exact current Gradle-resolved `ui-desktop` runtime classpath. Steve used the visible Parker UI to make exactly one owner submission, `Hello Parker.`. Technical evidence establishes one governed runtime correlation, accepted conversation traversal, local-model inference, tagged `NoAction` handling, a `NotAccepted` runtime outcome, safe System-status rendering, and normal runtime/UI shutdown.

The qualification is evidence granularity, not an architectural failure: the Ollama listener was already owned by the native Ollama tray application's server process, whose persistent access log was unavailable. Exactly one `/api/generate` call is supported by the one human submission, one runtime correlation, one model load/inference, and the production client's single-send/no-retry implementation, but it is not independently counted in an Ollama access log. The model returned `NoAction`, so `OwnerNotificationSink` and `OwnerUiNotificationBridge` were correctly not invoked and no Parker reply was rendered. Semantic quality is not confused with architectural verification.

The selected model was a Windows-local **TEST FIXTURE ONLY — NOT PARKER PRODUCTION MODEL SELECTION**.

## Authority and baseline

- Repository: `C:\Projects\Parker\parker-platform\parker-platform`.
- Branch: `ui/basic-owner-interface-integration`.
- HEAD: `75c3f0357add4eae9d367b03d8f048d5c9fa77e0` (`governance: plan lower-overhead owner UI live verification`).
- `origin/main`: `bfa618bece577408b247f76454836947f7257197`.
- Initial state: clean, 17 commits ahead of `origin/main`, nothing staged, and no active merge/rebase/cherry-pick/revert/bisect operation.

Fresh inspection covered the prior human-in-the-loop reviews, resource-path planning review, fixture installation/preflight review, current `OwnerUiMain`, runtime composition, controller/adapter/notification boundary, model client, and UI presentation. No material conflict was found. The prior RAM-gated attempt remains preserved and unchanged.

## Resource preparation and classpath

The initial recorded available RAM was 2,772,172,800 bytes (2.582 GiB), above the preferred 2.5 GiB preparation target. No Java process was resident. A stale native Ollama process existed without a listener; it was replaced by the authorized loopback fixture.

An external temporary Gradle init script registered a read-only classpath-print task. Gradle 8.10 ran offline with the existing local cache and emitted the exact current `ui-desktop` `runtimeClasspath`: 60 entries and the established 9,504-character Windows classpath. Required entries existed locally; the absent Java-source and resource output directories were the two already-established harmless entries. No dependency was downloaded and no source or Gradle build file changed. Gradle was then stopped and no Gradle client or daemon JVM remained before UI launch. The temporary init script was removed.

The existing compiled outputs remained valid: the only change since the artifact-reviewed `c8d011c` baseline was the resource-path review itself; no source or build input had changed.

## Fixture

Native Windows Ollama 0.32.8 served only `127.0.0.1:11434`. Metadata-only `/api/version` and `/api/tags` checks established exactly one installed model:

- name: `qwen2.5:0.5b-instruct-q4_K_M`;
- size: 397,821,319 bytes;
- digest: `a8b0c51577010a279d933d14c2a8ab4b268079d44c5c8830c0a93900f1827c67`;
- family/parameters/quantization: Qwen2, 494.03M, Q4_K_M.

No `/api/generate` preflight occurred. No other model was selected.

## Direct-Java equivalence and environment

The UI was launched from the repository root with `C:\Users\steve\.jdks\ms-17.0.19\bin\java.exe`, the exact resolved classpath, and main class `parker.ui.OwnerUiMainKt`. This invoked the same `OwnerUiMain` bytecode and `createOwnerUiRuntimeSession(System.getenv())` composition as the governed Gradle launcher, without retaining Gradle launcher JVMs.

The isolated environment was:

- `PARKER_MODEL_ENDPOINT_URL=http://127.0.0.1:11434/api/generate`;
- `PARKER_MODEL_NAME=qwen2.5:0.5b-instruct-q4_K_M`;
- `PARKER_MODEL_TIMEOUT_MS=120000`;
- `PARKER_OWNER_PRINCIPAL_ID=user.owner-ui-human-live`;
- `PARKER_OWNER_DISPLAY_NAME=Owner`;
- `PARKER_LOCAL_TEXT_CHANNEL_MODULE_ID=channel.local-text-owner-ui-human-live`;
- evidence, audit, and durability paths below `C:\Users\steve\AppData\Local\Parker\owner-ui-human-live`;
- `PARKER_LOG_LEVEL=INFO`.

No Parker server, cloud inference, or Unit 3-C machinery was configured or accessed.

## Ready state and pre-submission human evidence

Technical logs recorded `Runtime starting` at `2026-08-11T09:33:36.070962600Z` and `Runtime started` at `2026-08-11T09:33:36.548066100Z`. The Ready process set contained one Java UI/runtime JVM and the base Ollama supervisor, with no Gradle JVM. Available RAM at the Ready snapshot was 2,246,451,200 bytes (2.092 GiB).

Steve reported, as human-observed evidence:

- Parker window visible: yes;
- `Owner conversation · Parker Runtime` shown: yes;
- `Ready` shown: yes;
- message input and Send control available: yes;
- raw diagnostic/internal text visible: no.

## Immediate RAM gate and single submission

Immediately before submission authorization, at `2026-08-11T21:38:58.2153188+12:00`, available RAM was 2,159,464,448 bytes (2.011 GiB). This passed the hard 2.0 GiB gate. Steve was told to type exactly `Hello Parker.`, confirmed it was entered without submission, and was then authorized to use the normal Send control exactly once. No second submission was authorized or made.

## Technical trace

The one submission produced one correlation ID, `da4d77a5-6220-4953-9a36-b8f8c3439dd0`, and the following technical sequence:

1. `ParkerOwnerWindow` invoked `OwnerUiController.submit` from the visible Send control.
2. The controller added the owner transcript entry and invoked the production `OwnerInteraction`.
3. `OwnerUiRuntimeAdapter` constructed the owner message and invoked `ParkerRuntime.submitOwnerMessage`.
4. Logs recorded conversation continuity resolution, Reasoning Context assembly, and `Conversation accepted` on `channel.local-text-owner-ui-human-live`.
5. The configured `LocalHttpModelInferenceClient` used the loopback endpoint and selected model. Ollama created one `llama-server.exe` worker; `/api/ps` identified the exact expected model/digest and a loaded 494.03M Q4_K_M model.
6. At `2026-08-11T09:40:11.967136400Z`, runtime logs recorded `Reasoning completed ... outcome=NoAction`.
7. At `2026-08-11T09:40:11.969835800Z`, runtime logs recorded `Conversation not accepted for delivery ... reason=not a Reply; reasoningResponse was NoAction`.
8. `OwnerUiRuntimeAdapter` replaced that internal `NotAccepted.reason` with its fixed owner-safe wording. `OwnerUiController` rendered it as a System entry and returned to Ready.

Counts and outcomes:

- human owner submissions: exactly 1;
- distinct runtime submission correlations: exactly 1;
- `/api/generate` calls: 1 supported, qualified because no independent Ollama access-log count was available;
- model: exact expected fixture and digest;
- tagged parser/reasoning result: `NoAction`;
- runtime outcome: `NotAccepted` because the reasoning response was not a Reply;
- `OwnerNotificationSink` calls: 0, correct for `NoAction`/`NotAccepted`;
- `OwnerUiNotificationBridge` Parker replies: 0;
- visible System outcome: fixed safe rejection wording.

No instrumentation, direct HTTP generation substitute, retry, fallback, or second owner submission occurred.

## Post-submission human evidence and presentation safety

Steve reported, and supplied a screenshot showing:

- submitted `Hello Parker.` owner message visible: yes;
- Parker reply visible: no;
- System status visible: yes, `Not accepted: Parker did not accept this message.`;
- UI responsive and showing Ready: yes;
- no raw exception, path, endpoint, model, provider envelope, or internal reason was visible.

Steve included the safe System status when answering the raw-diagnostic question. The screenshot and technical comparison distinguish it from the hidden internal reason `not a Reply; reasoningResponse was NoAction`. The UI therefore did not expose `Throwable.message`, raw `NotAccepted.reason`, a provider envelope, filesystem path, endpoint, or model detail. Parker speech remained reserved for sink-originated owner notification; because no Reply was produced, no text was falsely presented as Parker speech. Runtime wording remained truthful and System status remained visually distinct.

## Normal shutdown and final isolation

Steve closed the Parker window normally. Logs recorded `Runtime shutting down` and `Runtime stopped` together at `2026-08-11T09:45:12.040594300Z`; the UI Java process exited and no unintended Java child remained.

The Ollama model worker had already exited by the first post-close process check. Stopping `ollama.exe` alone initially caused the installed `ollama app.exe` tray process to restart the server and worker. Teardown then explicitly stopped only the installed Ollama application/server/worker processes. Final verification found:

- no Java process;
- no Ollama application or server process;
- no `llama-server.exe` process;
- no port 11434 listener or connection;
- final RAM at `2026-08-11T21:46:52.2606312+12:00`: 2,582,638,592 bytes (2.405 GiB).

## Result and merge readiness

This attempt verifies the real visible UI-to-controller-to-adapter-to-`ParkerRuntime.submitOwnerMessage`-to-governed-runtime-to-local-model-to-tagged-outcome-to-safe-visible-System-status path, plus normal shutdown and isolation. It does not demonstrate a Reply outcome or notification delivery because the one permitted model result was `NoAction`; no retry is allowed. That is a result-semantic qualification, not evidence that composition was bypassed or failed.

**Merge readiness: supported for final merge review, with the documented qualifications.** The essential real-runtime submission and model path has now been exercised safely. Reviewers must retain the lack of an independent Ollama access-log count and the unexercised Reply/notification branch as explicit limitations; this review does not itself authorize merge, push, or any further attempt.

Repository scope at handoff is limited to this review and its independent companion. Nothing is staged, committed, pushed, merged, or rebased.
