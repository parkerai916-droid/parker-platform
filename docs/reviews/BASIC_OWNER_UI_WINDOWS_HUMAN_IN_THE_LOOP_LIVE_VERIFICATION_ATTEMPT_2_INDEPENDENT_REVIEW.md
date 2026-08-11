# Basic Owner UI Windows Human-in-the-Loop Live Verification Attempt 2 Independent Review

Date: 2026-08-11 (Pacific/Auckland)

## Independent verdict

**ACCEPTED WITH QUALIFICATIONS as B — VERIFIED WITH QUALIFICATIONS.** The evidence supports a real one-message production UI/runtime/model traversal and safe visible handling of the model's `NoAction` result. It does not support claiming a delivered Parker Reply, direct notification-bridge exercise, or an independently server-logged call count.

## Adversarial challenges

1. **Direct-Java equivalence:** Accepted with the explicit-classpath qualification. The command used Microsoft OpenJDK 17, repository-root working directory, the fresh offline Gradle-resolved `ui-desktop` runtime classpath, and `parker.ui.OwnerUiMainKt`. It did not substitute a different main, omit production dependencies, or retain Gradle launcher JVMs. No source/build input changed after the reviewed artifacts were produced.
2. **Real `OwnerUiMain`:** Proven by the launched main class, UI process, real Runtime/Ready presentation, and production startup/shutdown logs. `OfflineOwnerUiMain`, a test harness, and `:ui-desktop:runOwnerUi` were not used as the live launcher.
3. **Real `ParkerRuntime`:** Proven by `Runtime starting/started`, production correlation logs, governed acceptance/context/reasoning logs, and `Runtime shutting down/stopped`. The adapter was wired to `runtime::submitOwnerMessage` by the inspected production composition.
4. **Loopback endpoint and model:** Metadata proved Ollama 0.32.8 listening only at `127.0.0.1:11434`, with exactly the expected installed model and digest. `/api/ps` during the submission proved that exact model was loaded. No remote/cloud model or Parker server endpoint was configured.
5. **Exactly one owner submission:** Human procedure authorized one Send action. The UI showed one owner entry, and logs contain one correlation and one reasoning completion. No second message, retry, automation, direct runtime call, or direct generation preflight occurred.
6. **`submitOwnerMessage` traversal:** The one correlation's continuity, context, acceptance, reasoning, and delivery-rejection logs are specific to the production `ParkerRuntime.submitOwnerMessage` pipeline. Static inspection closes the UI/controller/adapter call edges; runtime evidence closes the governed pipeline edge.
7. **Model-call provenance:** One call is strongly supported by one submission/correlation, one newly loaded worker/model, one completed `NoAction`, and a single-send/no-retry client. However, the listener belonged to the native tray application's server, and no persistent access log was available. Therefore “exactly one `/api/generate` call” is not independently counted at the server boundary and must remain qualified.
8. **Tagged parser and outcome:** Runtime logged `NoAction`, which is a valid tagged reasoning outcome, followed by `NotAccepted` because it was not a Reply. No model response body was safely captured, so the claim is limited to the parsed runtime outcome rather than verbatim provider output.
9. **Notification provenance:** No Reply existed, so `OwnerNotificationSink` and `OwnerUiNotificationBridge` were not applicable and correctly emitted no Parker speech. This attempt cannot verify the positive Reply-notification branch. It does verify that a non-Reply was not mislabeled as Parker speech and instead became a distinct System status.
10. **Human versus technical evidence:** Window visibility, exact visible wording, controls, responsiveness, and absence of visible raw diagnostics are Steve's observations, supported by his screenshot. Runtime lifecycle, correlation, parser/outcome, process/model identity, RAM, and shutdown are technical evidence. Neither category is silently upgraded into the other.
11. **Presentation safety:** The visible `Not accepted: Parker did not accept this message.` is fixed owner-safe wording. The internal log reason `not a Reply; reasoningResponse was NoAction` did not appear in the UI. No raw throwable, provider envelope, filesystem path, endpoint, model, or provider detail was visible. System status remained distinct and real-runtime wording truthful.
12. **RAM gate:** The immediate gate measured 2.011 GiB and passed the frozen 2.0 GiB requirement before Steve was authorized to submit. RAM later fell to 1.493 GiB after model load, which does not invalidate the correctly timed gate and caused no retry or unsafe escalation.
13. **Shutdown:** Steve used the normal window close. Runtime logs proved normal shutdown and the Java process exited. Ollama's tray application restarted the server after the first stop; the final bounded teardown stopped the tray/server/worker and proved no related process or port 11434 listener remained.
14. **Server isolation:** No Parker server was started or accessed. The only network endpoint in scope was Windows-local loopback Ollama. No cloud inference occurred.
15. **Unit 3-C isolation:** No Unit 3-C machinery, diagnostic campaign, paused remedy programme, source, test, or production composition was changed or invoked.

## Classification calibration

Classification A would overclaim because a positive Reply/notification path was not exercised and the server lacked an independent access-log count. Classification C would understate the evidence: unlike the prior RAM-gated attempt, this run executed the essential human Send action through the real governed runtime and exact local model, obtained a valid parsed outcome, rendered a safe result, and shut down normally. **B — VERIFIED WITH QUALIFICATIONS** is therefore calibrated.

The model's `NoAction` result is not treated as an architecture failure. It proves the parser/outcome branch selected by this response; it says nothing broad about model quality or Parker production-model suitability.

## Merge-readiness challenge

The evidence supports advancing to final merge review because the previously missing real submission/model/outcome/UI traversal is now demonstrated without bypass, retry, server access, or presentation leakage. It does not make merge automatic. Final reviewers must explicitly accept these residual qualifications:

- no independently countable Ollama access-log entry;
- no positive `REPLY:` result in the one authorized attempt;
- no live `OwnerNotificationSink`/`OwnerUiNotificationBridge` delivery;
- fixture model remains test-only and unqualified for production semantics.

These qualifications do not justify another attempt under this authorization. They are review inputs, not permission to retry or fix. Nothing should be staged, committed, pushed, merged, or rebased by this task.
