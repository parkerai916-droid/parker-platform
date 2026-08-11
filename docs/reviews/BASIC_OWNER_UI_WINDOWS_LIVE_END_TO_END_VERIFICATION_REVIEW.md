# Basic Owner UI Windows Live End-to-End Verification Review

Date: 2026-08-11 (Pacific/Auckland)

## 1. Status

**LIVE OWNER UI VERIFIED: NO.**

Verification stopped at the mandatory Windows-local model-endpoint prerequisite. No graphical UI, real `ParkerRuntime`, or model inference was launched. This is a prerequisite failure, not evidence of a UI implementation failure.

## 2. Baseline

- Repository: `C:\Projects\Parker\parker-platform\parker-platform`
- Branch: `ui/basic-owner-interface-integration`
- HEAD: `17a5ed94c777eb070924b717b7c25dff88d89283`
- Fresh `origin/main`: `bfa618bece577408b247f76454836947f7257197`
- Divergence before this review: integration 7 commits ahead, main 0 commits ahead
- Starting worktree: clean

A read-only fetch confirmed `origin/main` had not moved since reconciliation.

## 3. Windows environment

- OS: Windows 11, amd64
- Checkout: Windows development checkout only
- No Parker-server path, IP, credential, filesystem, service, or endpoint was used.

## 4. JDK/build prerequisite

- `JAVA_HOME`: `C:\Users\steve\.codex\jdks\temurin-17\jdk-17.0.20+8`
- Java: Eclipse Temurin OpenJDK 17.0.20+8
- `javac`: 17.0.20
- Gradle: 8.10
- Kotlin: 1.9.24

The required fresh targeted gate passed: **45 tests, 45 passed, 0 failures, 0 errors, 0 skipped**. No live-model, Unit 2-D diagnostic, or Unit 3-C task ran.

## 5. Local model-endpoint determination

Classification: **B — no suitable Windows-local compatible model endpoint exists.**

Source requires an already-running endpoint compatible with `LocalHttpModelInferenceClient`. Its production defaults use the Ollama `/api/generate` request/response shape, while endpoint URL and model name have no defaults.

Inspection found:

- no `PARKER_*` or `OLLAMA_*` process environment variables;
- no recognized Ollama, LM Studio, llama, Jan, Kobold, or LocalAI process;
- no Docker or Podman process;
- no TCP listener on the typical local compatible ports checked, including 11434, 1234, 8080, and 8000; and
- no configured local model name.

No HTTP request, `/api/generate` call, endpoint health check, or inference request was made. Installing or configuring a model service was explicitly outside this task.

## 6. Exact runtime configuration

No live runtime configuration was applied because the endpoint prerequisite failed. The source requires:

- `PARKER_MODEL_ENDPOINT_URL`
- `PARKER_MODEL_NAME`
- `PARKER_OWNER_PRINCIPAL_ID`
- `PARKER_EVIDENCE_STORAGE_ROOT`
- `PARKER_EVIDENCE_DELETION_AUDIT_LOG_PATH`
- `PARKER_MEMORY_CORE_DURABILITY_LOG_PATH`

Optional keys include model timeout, owner display name, local-text channel module ID, and log level. No local durability paths were created because live execution was prohibited after the prerequisite failure.

## 7. Live source path

Fresh source inspection confirms one production path:

```text
visible Compose input
  -> OwnerUiController.submit
  -> OwnerInteraction.submit
  -> OwnerUiRuntimeAdapter
  -> InboundOwnerMessage
  -> injected ParkerRuntime::submitOwnerMessage
  -> normal governed runtime pipeline
  -> runtime outcome/reply delivery
  -> OwnerNotificationSink
  -> OwnerUiNotificationBridge
  -> OwnerReply callback
  -> typed transcript/status
  -> Compose rendering
```

`createOwnerUiRuntimeSession` constructs the ordinary `ParkerRuntime`, supplies one bridge as its `OwnerNotificationSink`, and gives the adapter only `runtime::submitOwnerMessage`. No alternate live path was found. UI code cannot directly invoke Memory, Goals, Knowledge Submission, Planner, tools, model/provider, HTTP, Ollama, or Qwen.

## 8. Graphical startup

Not attempted. The governed command would have been `\.\gradlew.bat :ui-desktop:runOwnerUi`, but Phase 3 required a stop before execution. Consequently there is no runtime state transition or visible Ready-state evidence.

## 9. Owner-message submission evidence

No live owner message was submitted. There is no live correlation ID, timestamp, principal, channel, or message trace. Deterministic tests continue to prove message construction structurally, but they were not misrepresented as graphical live proof.

## 10. Governed runtime trace

Not obtained because `ParkerRuntime` was not launched. Source and deterministic tests demonstrate the governed route remains structurally intact, but identity, intake, continuity, reasoning, permission/governance, execution/planning, conversation outcome, and delivery were not observed live in this task.

## 11. Response/outcome path

No live response or outcome occurred. `OwnerNotificationSink` was therefore not exercised live, and no UI rendering claim is made.

## 12. Deterministic non-success verification

The fresh 45-test gate passed and covers Delivered without reply, NotAccepted, Planned, Failed, Unavailable/startup failure, blank-input rejection, concurrent-submit prevention, reply ordering, and typed Owner/Parker/System authorship. No extra model requests were generated.

## 13. Shutdown result

No live process existed to close. Deterministic tests passed for controller cancellation, idempotent runtime shutdown, startup-failure cleanup, and controller-before-runtime close ordering. Clean live process exit remains unproven.

## 14. Architecture/governance boundary

Source audit: **PASS**. The adapter-only architecture remains intact and production reasoning is unchanged. This does not substitute for live trace evidence.

## 15. Unit 3-C/remedy isolation

**PASS.** No Unit 2-D diagnostic, Unit 3-C task, experimental property, campaign mechanism, or remedy mechanism ran. The NO REMEDY disposition remained unchanged.

## 16. Server-isolation result

**PASS.** Parker server used: **NO**. Server model endpoint used: **NO**. Server calls: **0**.

## 17. Mode-labelling finding

Classification: **A — blocking truthfulness defect** and **D — requires separate UI-copy correction**.

The real launcher renders the shared `ParkerOwnerWindow`, whose header says `Owner conversation · local offline preview` and whose empty state says messages use Parker's deterministic offline interaction. In real mode those statements are false and could reasonably cause an owner to mistake live model-backed operation for the offline harness. This task did not edit the copy.

## 18. safeMessage finding

Classification: **NOT GUARANTEED SAFE; BLOCKING; REQUIRES SEPARATE POLICY/IMPLEMENTATION REVIEW**.

Startup failures use a fixed safe string, but `OwnerUiRuntimeAdapter` maps arbitrary `ParkerRuntimeOutcome.Failed.cause.message` directly into `OwnerSubmissionDisposition.Failed.safeMessage`, and the controller renders it. No inspected runtime contract guarantees every dependency exception message is presentation-safe. No sensitive failure was provoked.

## 19. Live-verification verdict

**LIVE OWNER UI VERIFIED: NO.** Essential links 1–6 and 8 were not demonstrated because no suitable local endpoint existed and the mandated stop was obeyed.

## 20. Merge-readiness verdict

**NOT READY FOR FINAL MERGE REVIEW.**

Blocking before merge:

1. obtain separately authorized Windows-local compatible endpoint configuration and complete real graphical live proof;
2. correct materially false real-mode labelling under separate UI authorization; and
3. establish and implement a presentation-safe runtime error-message policy.

Native installer/distribution work and upstream publication remain non-blocking future enhancements unless separately made release requirements.

## 21. Blocking issues

- No suitable Windows-local compatible model endpoint or model name.
- No live graphical startup/message/reply/shutdown evidence.
- Materially false offline-preview wording in real mode.
- Runtime exception messages are not guaranteed presentation-safe.

## 22. Non-blocking issues

- No native installer/distribution declaration; existing compile/JAR tasks remain reproducible.
- Branch is local and unpublished by instruction.

## 23. Exact next step

Request separate authorization to install or configure a Windows-local compatible model service and model, and separate authorization to correct real-mode copy and define safe error presentation. After those prerequisites are resolved and offline gates remain green, repeat this live-verification plan from the same fresh-main baseline. Do not use the Parker server or its Ollama/Qwen endpoint.

