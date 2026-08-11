# Basic Owner UI Windows-Local Model Fixture Selection Review

Date: 2026-08-11 (Pacific/Auckland)

## Status and baseline

**FIXTURE SELECTED — PLANNING ONLY. NO INSTALLATION OR LIVE CALL AUTHORIZED OR PERFORMED.**

- Repository: `C:\Projects\Parker\parker-platform\parker-platform`
- Environment: Windows development machine only
- Branch: `ui/basic-owner-interface-integration`
- HEAD: `1de1297edcc70ae9f9748264e1ef0ee097f7bb55`
- HEAD subject: `fix(ui): resolve live verification presentation blockers`
- `origin/main`: `bfa618bece577408b247f76454836947f7257197`
- Starting worktree: clean

No live-model Gradle task, endpoint request, model process, Parker-server access, installation, download, staging, commit, or push occurred.

## Exact provider contract

Fresh source tracing establishes the production path:

```text
ParkerRuntime
  -> ModelReasoningProvider (30,000 ms configured default)
  -> LocalHttpModelInferenceClient
  -> TaggedReasoningResponseParser
```

`LocalHttpModelInferenceClient` sends HTTP `POST` to the complete URL supplied as `PARKER_MODEL_ENDPOINT_URL`; the class does not append a path. Current production composition supplies its default Ollama-shaped formatter/parser, so the configured URL must normally end in `/api/generate`.

The request has `Content-Type: application/json` and exact logical fields:

```json
{"model":"<PARKER_MODEL_NAME>","prompt":"<assembled Parker prompt>","stream":false}
```

`model` and `prompt` are JSON strings escaped for backslash, quote, newline, carriage return, and tab. `model` is required even if a compatible server could otherwise infer a default. `stream` must be false because Parker expects one response body.

The response parser requires a compact top-level JSON sequence exactly matching `"response":"..."`; whitespace between the key, colon, and opening quote is not accepted. Other response fields are ignored. The extracted string is unescaped for the same five sequences. The downstream parser trims it once and accepts only:

- `REPLY:<non-blank text>`;
- `GOAL:<non-blank text>`;
- `REMEMBER:<non-blank text>`; or
- exactly `NOACTION`.

Matching is case-sensitive. Untagged output, prose before the tag, malformed/unterminated response JSON, or trailing text after `NOACTION` fails.

`ModelReasoningProvider` wraps only `infer` in `withTimeout`. `PARKER_MODEL_TIMEOUT_MS` is optional and defaults to 30,000 ms; it must be a positive integer. The Java `HttpClient` has no separate explicit connect/request timeout in this class. Transport errors, cancellation, timeout, response-parser errors, and tagged-parser errors propagate; `LocalHttpModelInferenceClient` does not inspect HTTP status before parsing the body. The governed runtime classifies/logs the resulting failure, while the corrected UI displays fixed owner-safe status.

**Compatibility determination: B, with a narrower qualification.** Ollama itself is not architecturally required. Current production composition requires a service that accepts the Ollama-shaped non-streaming `/api/generate` exchange and returns Parker's specifically parseable compact `response` field. A different runtime is valid only if that exact wire behavior is confirmed. Ollama's official API documents `model`, `prompt`, `stream:false`, and the single-object `response` field, matching Parker's defaults: [Ollama generate API](https://docs.ollama.com/api/generate).

## Fixture purpose

The fixture exists only to support a later proof of:

```text
visible owner UI
  -> ParkerRuntime.submitOwnerMessage
  -> ordinary governed runtime
  -> real local model-provider boundary
  -> parsable Parker reply/outcome
  -> OwnerNotificationSink
  -> visible owner UI
```

A deterministic HTTP stub would prove network formatting, response extraction, and downstream plumbing, but it would not prove that the real model-provider inference seam can consume Parker's prompt and emit the required tag. The governed objective therefore requires one genuine local inference call. The model's answer need only be benign and parsable.

## Non-goals

This fixture does not prove reasoning quality, semantic reliability, Reasoning Protocol conformance, remedy efficacy, production-model suitability, Memory behavior, Planner behavior, tool use, performance readiness, provider approval, or production/server parity. It does not select a Parker production model and creates no evidence for reopening the paused remedy programme.

## Minimum model requirements

The minimum is a local, CPU-capable, instruction-tuned text model small enough for this 8 GB Windows machine, able to follow the existing one-tag output instruction for one benign conversational prompt. It needs no code specialization, tool calling, vision, long-context qualification, cloud service, or production-grade semantic performance.

A very small model is adequate for this architectural proof if a preflight invocation (separately authorized later) demonstrates one exact tagged response. `qwen2.5-coder:7b` is neither technically nor constitutionally required. Using it would add resource cost and improperly blur this fixture with the paused Reasoning Protocol work.

## Runtime candidates

| Runtime approach | Contract fit | Windows/hardware fit | Determination |
|---|---|---|---|
| Native Ollama for Windows | Exact native `/api/generate` behavior; default bind is `127.0.0.1:11434` | Lowest setup uncertainty and no Docker/WSL overhead | **Preferred** |
| LocalAI with its Ollama-compatible API | Potential direct fit, but exact release/config behavior must be verified | Broader platform and backend setup add disproportionate complexity on 8 GB RAM | Valid fallback runtime class, not preferred |
| `llama.cpp` server plus a deliberately narrow Ollama-shape adapter | Genuine local inference, but the adapter must translate Parker's request and return exact compact JSON | More moving pieces and custom verification code | Technically valid but disproportionate |
| Deterministic local HTTP stub | Exact wire shape can be manufactured | Very light | Insufficient for the real inference-path objective; suitable only for transport testing already covered offline |

Ollama's Windows documentation confirms local API use at port 11434 and configurable model storage: [Ollama on Windows](https://docs.ollama.com/windows). Its FAQ states the default bind is `127.0.0.1:11434`: [Ollama FAQ](https://docs.ollama.com/faq). LocalAI describes drop-in Ollama API support, but its wider backend/runtime surface is unnecessary here: [LocalAI repository](https://github.com/mudler/LocalAI).

## Model candidates

RAM figures below are conservative planning estimates, not benchmarks or vendor guarantees. Actual memory varies with runtime version, context allocation, and prompt length.

| Model | Parameters / download | Likely working RAM | CPU responsiveness here | Tagged-instruction fit | Disadvantages |
|---|---:|---:|---|---|---|
| `qwen2.5:0.5b-instruct-q4_K_M` | 0.5B / 398 MB | roughly 1–2 GB total runtime working set | Best prospect for a short call on i7-8550U | Explicit instruction-tuned tag; smallest practical balance | Very small model may still occasionally add prose or select the wrong tag |
| `gemma3:1b-it-q4_K_M` (`gemma3:1b`) | 1B / 815 MB | roughly 2–3 GB | Slower but still proportionate after memory is freed | Instruction-tuned and designed for resource-limited devices | About twice the weights and memory pressure |
| `smollm2:360m` | 360M / 726 MB | roughly 1.5–2.5 GB | Likely responsive | Compact instruction model | Larger package than Qwen 0.5B despite fewer parameters; weaker confidence in strict tag adherence |

Published Ollama registry sizes are 398 MB for Qwen2.5 0.5B instruct, 815 MB for Gemma 3 1B, and 726 MB for SmolLM2 360M: [Qwen2.5 tags](https://ollama.com/library/qwen2.5/tags), [Gemma 3](https://ollama.com/library/gemma3), [SmolLM2](https://ollama.com/library/smollm2).

## Windows hardware fit

Read-only, non-benchmark inspection reported:

- CPU: Intel Core i7-8550U at 1.80 GHz;
- logical processors: 8;
- installed RAM: 8,442,306,560 bytes (about 7.86 GiB);
- available RAM at inspection: 413,368,320 bytes (about 394 MiB);
- GPU: integrated Intel UHD Graphics 620; no dedicated VRAM was reported;
- C: 510.8 GB total, 34.1 GB free (about 31.7 GiB);
- D: 67.1 GB total, 66.6 GB free (about 62.0 GiB), FAT32.

Disk is sufficient for every candidate. CPU-only inference is the appropriate assumption. Current available RAM is **not sufficient to authorize immediate startup**. Before any installation/execution authorization, close memory-heavy applications or reboot and recheck. Require at least 2 GiB available for the preferred fixture; 3 GiB is the safer gate. Do not compensate with an invasive benchmark.

## Preferred fixture

**Native Ollama for Windows + `qwen2.5:0.5b-instruct-q4_K_M`.**

**TEST FIXTURE ONLY — NOT PARKER PRODUCTION MODEL SELECTION.**

This is preferred because Ollama supplies the exact wire contract without a translation layer, the explicit instruction-tuned Q4 model is only 398 MB, and it is the lowest-proportion genuine inference option with a reasonable prospect of following the one-tag instruction. It is not `qwen2.5-coder:7b`, is not selected because of Unit 3-C, and yields no Qwen-family production endorsement.

## Fallback fixture

**Native Ollama for Windows + `gemma3:1b`.** This retains the known exact runtime contract while changing model family and increasing instruction capacity modestly. Use it only if the preferred 0.5B fixture cannot produce one parsable `REPLY:` response within the separately authorized minimal preflight. It remains a test fixture, not production selection. It requires more free RAM and an additional download, so fallback use needs explicit approval rather than automatic escalation.

## Later installation and configuration plan (do not execute in this task)

1. Recheck available RAM; proceed only with at least 2 GiB free, preferably 3 GiB.
2. Obtain the signed native Windows installer from the official [Ollama Windows page](https://docs.ollama.com/windows). Do not use Docker, WSL, a package mirror, or the Parker server.
3. Before first model acquisition, set the user variable `OLLAMA_HOST=127.0.0.1:11434`. Optionally set `OLLAMA_MODELS=D:\ParkerVerificationFixture\ollama-models` to keep disposable weights on the roomier local drive. Restart Ollama after environment changes.
4. Start the native Ollama Windows application from the Start menu (or, if the approved installation is CLI-only, run `ollama serve` in a dedicated local terminal). Confirm it listens only on loopback.
5. Acquire exactly one model with `ollama pull qwen2.5:0.5b-instruct-q4_K_M`. Do not pull the fallback unless separately justified.
6. Create new local paths outside the repository and every historical experiment/campaign location:
   - `C:\Users\steve\AppData\Local\Parker\owner-ui-live-retest\evidence`
   - `C:\Users\steve\AppData\Local\Parker\owner-ui-live-retest\audit\evidence-deletion.jsonl`
   - `C:\Users\steve\AppData\Local\Parker\owner-ui-live-retest\memory\durability.jsonl`
7. In the dedicated launch terminal only, set:
   - `PARKER_MODEL_ENDPOINT_URL=http://127.0.0.1:11434/api/generate`
   - `PARKER_MODEL_NAME=qwen2.5:0.5b-instruct-q4_K_M`
   - `PARKER_MODEL_TIMEOUT_MS=120000` (allows CPU cold load without changing production defaults)
   - `PARKER_OWNER_PRINCIPAL_ID=user.owner-ui-live-retest`
   - `PARKER_OWNER_DISPLAY_NAME=Owner`
   - `PARKER_LOCAL_TEXT_CHANNEL_MODULE_ID=channel.local-text-owner-ui-live-retest`
   - `PARKER_EVIDENCE_STORAGE_ROOT=<evidence directory above>`
   - `PARKER_EVIDENCE_DELETION_AUDIT_LOG_PATH=<audit file above>`
   - `PARKER_MEMORY_CORE_DURABILITY_LOG_PATH=<memory file above>`
   - `PARKER_LOG_LEVEL=INFO`
8. Health-check without inference using `GET http://127.0.0.1:11434/api/version`, then `GET /api/tags` to confirm the exact model is local. Do not use `/api/generate` merely as a health check.
9. Under separate live-retest authorization, launch only `:ui-desktop:runOwnerUi`, wait for Ready, submit the single benign message, observe one sink-delivered Parker reply, and close the window cleanly.

The later run should record runtime/model versions, bind address, exact environment (excluding any secrets), timestamps, one correlation/log trail, UI state transitions, reply authorship, and shutdown evidence.

## Security and isolation requirements

- Bind only to `127.0.0.1`; do not use `0.0.0.0`, LAN exposure, tunnels, port forwarding, or firewall openings.
- Use only local model weights after acquisition; disable/avoid Ollama cloud models and all cloud inference/API credentials.
- Do not configure or contact any Parker server address, storage path, credential, Ollama instance, or Qwen instance.
- Keep Parker durability artifacts in the new Windows-local disposable root above, outside the repository and all campaign paths.
- Use a dedicated non-production owner principal/module identifier and no production credentials.
- Keep model storage and runtime disposable. Do not add endpoint/model defaults to source, Gradle, shell profiles, or repository configuration.

## Live-call budget

The authorized budget should be **one successful Parker UI owner-message submission**, producing one genuine model inference and one owner-visible sink-delivered reply. A short greeting such as `Hello Parker. Please reply briefly.` is preferred because the existing prompt explicitly directs greetings to `REPLY:`.

Permit at most **one additional model preflight/retry call** only if separately included in the live authorization and needed to establish exact tag adherence before the visible proof. If the first response is unclassifiable, stop and report it; do not tune prompts, cycle models, or create an evaluation campaign. All non-success/status cases remain deterministic-test evidence and require no live calls.

## Teardown plan

After evidence capture and clean Parker shutdown:

1. Quit the Ollama Windows application or stop the dedicated `ollama serve` process.
2. Confirm port 11434 is no longer listening.
3. Remove the fixture model with `ollama rm qwen2.5:0.5b-instruct-q4_K_M` before uninstalling the runtime.
4. Remove the dedicated user variables `OLLAMA_HOST` and `OLLAMA_MODELS` if created; clear the terminal-scoped `PARKER_*` variables by closing that terminal.
5. Uninstall Ollama through Windows Installed Apps if it has no other explicitly approved use.
6. After confirming review evidence has been preserved elsewhere as authorized, delete only the resolved disposable directories `D:\ParkerVerificationFixture` and `C:\Users\steve\AppData\Local\Parker\owner-ui-live-retest`.
7. Recheck that no Ollama process/listener, fixture model, or disposable Parker durability path remains.

Deletion must be performed only under the later teardown authorization after resolving and verifying each exact target; it is not authorized by this review.

## Blocking issues

1. Available RAM was only about 394 MiB during inspection. Installation/start authorization should be withheld until a fresh read-only check shows at least 2 GiB free, preferably 3 GiB.
2. Runtime installation, model download, endpoint start, and live execution each remain unauthorized external-state changes.
3. The preferred tiny model's tag adherence is plausible, not proven. That uncertainty is why the later call budget is tightly bounded and why an unclassifiable first response must stop rather than expand into tuning.

These are execution prerequisites, not reasons to select a larger production-like model.

## Exact next step

**NOT YET READY TO AUTHORIZE INSTALLATION.** First free system memory and perform a fresh non-invasive RAM/disk recheck. If at least 2 GiB (preferably 3 GiB) is available, seek a separate, explicit authorization limited to official native Ollama installation, loopback configuration, acquisition of exactly `qwen2.5:0.5b-instruct-q4_K_M`, creation of the isolated local paths, non-inference health checks, and teardown preparation. Live `/api/generate` use must remain a further separately authorized step.
