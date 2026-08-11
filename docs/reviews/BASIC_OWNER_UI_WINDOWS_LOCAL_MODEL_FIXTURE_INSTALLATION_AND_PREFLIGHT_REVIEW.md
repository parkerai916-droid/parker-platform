# Basic Owner UI Windows-Local Model Fixture Installation and Bounded Preflight Review

Date: 2026-08-11 (Pacific/Auckland)

## Status

**FINAL REQUEST-CONSTRUCTION-CORRECTED PREFLIGHT PASSED — A: FIXTURE STRUCTURALLY COMPATIBLE. READY ONLY FOR SEPARATELY AUTHORIZED BASIC OWNER UI LIVE VERIFICATION.**

Native Ollama was installed and its loopback-only metadata endpoint was verified. After two RAM-gated continuations stopped below the hard threshold, a third continuation passed both required 2.0 GiB gates, acquired only the selected model, and spent the single authorized `/api/generate` call. The PowerShell HTTP client failed locally with a `NullReferenceException` before returning a response object, so no response body or tagged-response result was captured. No retry occurred.

A separately authorized diagnostic classified that original observed failure as **A — CLIENT-SIDE FAILURE CONFIRMED**: Windows PowerShell 5.1 `Invoke-WebRequest` threw locally without returning an HTTP response object. No original-request Ollama log survived, so whether the server completed that first generation remains indeterminate. One authorized diagnostic retry used `curl.exe`; PowerShell-to-native argument handling removed required JSON key quoting, and Ollama returned HTTP 400 in 1.2132 ms with `{"error":"invalid character 'm' looking for beginning of object key string"}`. That retry did not begin model inference and did not resolve tagged-response compatibility. No second retry occurred.

A final, separately authorized request-construction-corrected preflight preserved both earlier failures and corrected only Attempt 2's demonstrated JSON transport defect. A validated BOM-free UTF-8 request file was sent with `curl.exe --data-binary @file`. Ollama returned HTTP 200 and generated exactly `REPLY:Hello Parker.`. The compact `"response":"` field, `done:true`, and nonblank `REPLY:` text satisfy the current production formatter/parser seam and `TaggedReasoningResponseParser`. Classification: **A — FIXTURE STRUCTURALLY COMPATIBLE**, with the narrow interpretation stated below.

Continuation checkpoint `F-3-4-2-5-F-6` was attempted on 2026-08-11 and stopped at the baseline RAM gate before any deliberate Ollama startup. The fresh Windows `GlobalMemoryStatusEx` reading was 1,470,357,504 bytes (about 1.369 GiB), below the required 2.0 GiB. No endpoint check, model pull, or generation call was made during the continuation. A final teardown check found the installed Ollama background processes present without a port 11434 listener; both processes were stopped.

A later resumption of the same checkpoint on 2026-08-11 also stopped at the baseline gate. Although the operator reported 2.42 GB free, the execution-time `GlobalMemoryStatusEx` reading was 1,966,542,848 bytes (about 1.831 GiB). No Ollama process or port 11434 listener was present, and the post-teardown-confirmation reading was 1,998,032,896 bytes (about 1.861 GiB), still below 2.0 GiB. Ollama was not started; no endpoint request, model pull, or generation call occurred.

A third resumption of checkpoint `F-3-4-2-5-F-6` passed its baseline gate at 2,534,162,432 bytes (about 2.360 GiB). Ollama 0.32.8 was started with the fixture environment, exactly one listener owned by `ollama.exe` was verified at `127.0.0.1:11434`, and only `qwen2.5:0.5b-instruct-q4_K_M` was pulled. The post-pull gate passed at 2,517,778,432 bytes (about 2.345 GiB), permitting the one bounded generation attempt.

The fixture remains **TEST FIXTURE ONLY — NOT PARKER PRODUCTION MODEL SELECTION**.

## Baseline

- Repository: `C:\Projects\Parker\parker-platform\parker-platform`
- Branch: `ui/basic-owner-interface-integration`
- HEAD: `d302b62c3f0037dcfc226b55cef6076ae6a67d2a`
- HEAD subject: `governance: select Windows local UI verification fixture`
- `origin/main`: `bfa618bece577408b247f76454836947f7257197`
- Starting worktree: clean
- Initial Ollama state: command absent, no Ollama process, no listener on port 11434

The expected baseline and clean-tree gates passed. No branch change, fetch mutation, rebase, merge, stage, commit, or push occurred.

## Machine-resource checks

| Checkpoint | Available physical RAM |
|---|---:|
| Before installation | 2,228,031,488 bytes (about 2.075 GiB) |
| After installation, before model acquisition | 1,329,664,000 bytes (about 1.238 GiB) |
| After stopping Ollama processes | 1,656,791,040 bytes (about 1.543 GiB) |
| Continuation checkpoint `F-3-4-2-5-F-6`, before Ollama startup | 1,470,357,504 bytes (about 1.369 GiB) |
| Continuation checkpoint, after stopping discovered background processes | 1,431,072,768 bytes (about 1.333 GiB) |
| Later resumption, execution-time baseline gate | 1,966,542,848 bytes (about 1.831 GiB) |
| Later resumption, after teardown confirmation | 1,998,032,896 bytes (about 1.861 GiB) |
| Third resumption, execution-time baseline gate | 2,534,162,432 bytes (about 2.360 GiB) |
| Third resumption, after model pull | 2,517,778,432 bytes (about 2.345 GiB) |
| Third resumption, immediately before teardown | 1,914,617,856 bytes (about 1.783 GiB) |
| Third resumption, after teardown | 1,977,556,992 bytes (about 1.842 GiB) |
| Third resumption, after stopping detached `llama-server.exe` worker | 2,368,196,608 bytes (about 2.206 GiB) |
| Diagnostic baseline | 2,385,772,544 bytes (about 2.222 GiB) |
| Diagnostic retry gate | 2,514,522,112 bytes (about 2.342 GiB) |
| Immediately before diagnostic retry | 2,459,013,120 bytes (about 2.290 GiB) |
| Immediately after diagnostic retry | 2,460,729,344 bytes (about 2.292 GiB) |
| After diagnostic teardown | 2,514,526,208 bytes (about 2.342 GiB) |
| Attempt 3 initial baseline gate | 2,562,658,304 bytes (about 2.387 GiB) |
| Attempt 3 immediate pre-inference gate | 2,600,927,232 bytes (about 2.422 GiB) |
| Attempt 3 immediately before request | 2,617,159,680 bytes (about 2.437 GiB) |
| Attempt 3 immediately after request | 2,083,733,504 bytes (about 1.941 GiB) |
| Attempt 3 after complete teardown | 2,636,779,520 bytes (about 2.456 GiB) |

The pre-install reading exceeded the authorized minimum. The post-install reading did not. The instruction says to stop if available RAM is below 2.0 GB before model execution; therefore model acquisition and all inference were stopped. The post-stop reading remained below 2.0 GB.

The continuation instruction retained the same hard gate. Because its fresh pre-start reading was also below 2.0 GiB, execution halted before deliberately starting the existing Ollama installation. The one-call `/api/generate` allowance remains unused. The post-stop reading also remained below 2.0 GiB.

The later resumption produced the same determination: its measured baseline and post-teardown-confirmation readings both failed the gate, so the operator-reported estimate was not used in place of execution-time evidence.

Disk observations:

- C: initially 41,245,876,224 bytes free; 36,691,050,496 bytes free while the installer file and installed runtime were present; 38,257,655,808 bytes free after deleting only the temporary installer.
- D: initially 66,559,803,392 bytes free; 66,559,541,248 bytes free after creating the empty fixture model-storage structure.
- Installed Ollama tree measured 2,942,724,891 bytes.
- At the initial installation checkpoint, selected-model bytes were zero and the local API reported zero models; the later successful acquisition superseded that initial observation.

## Installation method and version

The native Windows installer was downloaded directly from `https://ollama.com/download/OllamaSetup.exe`, the official Ollama distribution URL identified by the selection review and official Windows documentation.

Before execution:

- installer size: 1,564,836,592 bytes;
- SHA-256: `CBA870E7111BD141623531FAA694A77AF2C66A414832DE712F8BC1D67A7B163C`;
- Authenticode status: Valid;
- signer: `Ollama Inc.` (Toronto, Ontario, Canada).

It was installed silently for the current user. Installed CLI path:

`C:\Users\steve\AppData\Local\Programs\Ollama\ollama.exe`

Installed version: **0.32.8**.

The installer launched `ollama app.exe` and `ollama.exe`. The silent installer wrapper did not return after installation had completed, so only its stale waiting shell wrapper was terminated after the installed files, processes, version, and listener had independently been confirmed. The installed application was not damaged. The temporary installer was later removed to recover C: space; Ollama itself remains installed.

Fixture user variables were set as selected:

- `OLLAMA_HOST=127.0.0.1:11434`
- `OLLAMA_MODELS=D:\ParkerVerificationFixture\ollama-models`

No Parker production configuration, source, Gradle configuration, or persistent `PARKER_*` environment variable was changed.

## Model acquisition

Authorized model: `qwen2.5:0.5b-instruct-q4_K_M`.

**Pulled successfully during the third continuation.** Exact installed identity reported by `GET /api/tags`:

- model identifier: `qwen2.5:0.5b-instruct-q4_K_M`;
- reported size: 397,821,319 bytes;
- digest: `a8b0c51577010a279d933d14c2a8ab4b268079d44c5c8830c0a93900f1827c67`;
- fallback model: not pulled or substituted.

## Endpoint and listener result

After installation, Ollama listened only on:

`127.0.0.1:11434`

The owner process was the newly installed local `ollama.exe`. No `0.0.0.0`, LAN, tunnel, proxy, cloud, or Parker-server endpoint was used.

Permitted non-generative metadata checks:

- `GET http://127.0.0.1:11434/api/version` returned `0.32.8`;
- the initial installation-time `GET http://127.0.0.1:11434/api/tags` returned zero installed models.

During the third continuation, the listener was independently rechecked as exactly `127.0.0.1:11434`, owned by the started `ollama.exe` process. The selected model then appeared as the sole model in `GET /api/tags`.

Attempt 1 sent one transport `POST` to `http://127.0.0.1:11434/api/generate`. The client waited about 35,015 ms and then PowerShell `Invoke-WebRequest` threw a local `NullReferenceException`; it yielded no HTTP response object or raw body. That attempt cannot establish whether Ollama completed generation server-side.

Attempt 2 reached the same loopback endpoint but received HTTP 400 because PowerShell-native argument handling corrupted its JSON. Attempt 3 used a validated request file and received HTTP 200 with a complete Ollama response.

## Bounded protocol preflight

- Attempt 1 authorization: one `/api/generate` call; one made.
- Attempt 2 diagnostic authorization: one retry; one malformed request made, rejected before inference.
- Attempt 3 final authorization: one request; one made. **No retry made.**
- Request shape: `model`, `prompt`, and `stream:false` only, sent as compact JSON.
- Exact request purpose: instruct the selected fixture to answer the benign greeting `Hello Parker. Please reply briefly.` with exactly one `REPLY:` line.
- Client outcome: local `Invoke-WebRequest` `NullReferenceException` after about 35,015 ms, before a response object was returned.
- Attempt 1 model response: unavailable; no raw body captured.
- Attempt 3 model response: `REPLY:Hello Parker.`.
- Attempt 3 tagged-response classification: valid nonblank `REPLY:`.
- Compatibility with Parker's existing `TaggedReasoningResponseParser`: **empirically established for this single narrow fixture preflight**.

No retry occurred under the original preflight authorization. The later diagnostic authorization permitted exactly one request; it was made, rejected before inference because its transmitted JSON was malformed, and not repeated. The final corrected authorization permitted exactly one further request; it succeeded and was not repeated. No fallback, model substitution, prompt tuning, Unit 3-C fixture, campaign prompt, quality evaluation, ParkerRuntime launch, or graphical UI launch occurred.

## Teardown state

After the RAM failure, both installed fixture processes (`ollama app.exe` and `ollama.exe`) were stopped. Final listener count on port 11434: zero. No model was loaded or required unloading.

At continuation checkpoint `F-3-4-2-5-F-6`, the final teardown again stopped both installed background processes after they were discovered running without a port 11434 listener. The final continuation listener count was zero and the final available physical RAM reading was 1,431,072,768 bytes (about 1.333 GiB).

After the third continuation's single preflight attempt, the `ollama.exe` supervisor was stopped. A final process audit found its detached `llama-server.exe` worker still resident with a working set of 526,897,152 bytes; that worker was also stopped. No Ollama- or Llama-named process and no port 11434 listener remained. Available RAM rose from 1,914,617,856 bytes (about 1.783 GiB) immediately before teardown to 2,368,196,608 bytes (about 2.206 GiB) after complete teardown. No retry or fallback call was made.

Ollama remains installed, as the task prohibited uninstalling a successfully installed fixture unless safety required it. The empty/disposable model root and the two user variables remain configured. The downloaded installer has been deleted from the Windows temporary directory.

To restart for a future separately authorized continuation after memory is freed:

1. Recheck available physical RAM and require at least 2.0 GB before starting Ollama; preferably provide the 3 GB margin recommended by the selection review.
2. Start the installed native Ollama application from the Windows Start menu, inheriting `OLLAMA_HOST` and `OLLAMA_MODELS`.
3. Confirm only `127.0.0.1:11434` is listening.
4. Do not pull another model. Any future `/api/generate` call requires new explicit authority because both the original call allowance and the separately authorized diagnostic retry are exhausted.

The actual Parker UI live verification requires another separate authorization after the fixture installation/preflight review reaches Ready.

## Single-preflight failure diagnostic

Diagnostic baseline: branch `ui/basic-owner-interface-integration`; HEAD `f3425f6e4abdd43022c0e74676fc04daedba41bc`; `origin/main` `bfa618bece577408b247f76454836947f7257197`. The pre-existing review modification was preserved. No Ollama/Llama process or port 11434 listener existed at diagnostic start.

### Original failure determination

**Classification: A — CLIENT-SIDE FAILURE CONFIRMED.**

- The recovered command used Windows PowerShell 5.1 Desktop `Invoke-WebRequest` without `-UseBasicParsing`.
- After about 35,015 ms it threw `System.NullReferenceException` inside `Invoke-WebRequest` and returned no response object; subsequent response-content parsing therefore had null input.
- The exact constructed logical request fields were valid for the current `defaultOllamaRequestBody` contract: `model`, `prompt`, and `stream:false`.
- The Ollama supervisor and a 526,897,152-byte-working-set `llama-server.exe` worker remained alive after the client exception. That is inconsistent with a demonstrated server crash or confirmed resource-exhaustion termination, though it does not prove generation completed.
- No redirected Ollama log, persistent Ollama log file, or relevant Windows application event survived for the original request. Therefore server receipt, model-load completion, generation completion, and any original raw response remain unprovable.

Accordingly, the observed failure mechanism is confirmed at the client boundary. B (Ollama/model failure), C (resource failure), D (request/contract failure), and E (another cause) are not confirmed for the original attempt. The server-side fate of that request remains F-indeterminate within the narrower question of whether generation completed.

### Authorized diagnostic retry

One retry was justified because a raw-byte-capable client could have resolved tagged-response compatibility without changing the fixture. Preconditions passed: 2.342 GiB at the retry gate, loopback-only listener `127.0.0.1:11434`, Ollama 0.32.8, and exactly the already-downloaded model/digest recorded above.

The retry used Windows `curl.exe` 8.21.0 and attempted one POST. Result:

- curl exit: 0;
- HTTP status: 400;
- elapsed time: 66 ms client-side; 1.2132 ms in Ollama's access log;
- raw body: `{"error":"invalid character 'm' looking for beginning of object key string"}`;
- complete generation response: no;
- `"response":"` wire field: absent;
- tagged-protocol result: indeterminate, because inference did not begin;
- RAM: 2.290 GiB immediately before, 2.292 GiB immediately after.

This retry failure is **D — REQUEST/CONTRACT FAILURE CONFIRMED for the retry only**. PowerShell-to-native argument handling stripped the JSON key quotes from the curl argument before transmission. Ollama demonstrably received the POST and rejected malformed JSON immediately; no `llama-server.exe` worker was created. This does not reclassify the original attempt, whose body was supplied directly to `Invoke-WebRequest` and whose observed failure remains client-side.

Afterward, `ollama.exe` was stopped. No Ollama/Llama process and no port 11434 listener remained; final RAM was 2.342 GiB. The temporary diagnostic logs remain outside the repository at `C:\Users\steve\AppData\Local\Temp\parker-ollama-preflight-retry.stdout.log` and `.stderr.log`.

## Attempt 3 — final request-construction-corrected preflight

The historical record of Attempts 1 and 2 above remains unchanged in substance. Attempt 3 corrected only the proven request-byte construction problem.

### Baseline and contract

- Repository: `C:\Projects\Parker\parker-platform\parker-platform`.
- Branch: `ui/basic-owner-interface-integration`.
- HEAD: `f3425f6e4abdd43022c0e74676fc04daedba41bc`.
- `origin/main`: `bfa618bece577408b247f76454836947f7257197`.
- Starting git state: only this existing review was modified; its prior changes were preserved.
- Starting fixture state: no Ollama/Llama process and no port 11434 listener.
- Initial RAM: 2,562,658,304 bytes (about 2.387 GiB), passing the 2.0 GiB gate.

Fresh source inspection re-established that `LocalHttpModelInferenceClient` POSTs JSON to its complete configured endpoint, with `Content-Type: application/json`; the default request contains exactly `model`, `prompt`, and `stream:false`. `defaultOllamaResponseBody` requires the compact sequence `"response":"` and extracts that string. `TaggedReasoningResponseParser` trims once and accepts nonblank `GOAL:`, `REPLY:`, or `REMEMBER:` content, or exact `NOACTION`.

### Exact request construction

A PowerShell ordered object containing exactly `model`, `prompt`, and `stream = false` was serialized with `ConvertTo-Json -Compress`, written as BOM-free UTF-8, read back, parsed again, and checked for exact property names and values before transmission.

- Request file: `C:\Users\steve\AppData\Local\Temp\parker-final-preflight-attempt3.json`.
- Byte length: 241.
- SHA-256: `FEA87C2C7DBAED90A49D0FBD3EA34265590E2C5566B49789072BE17F6418FE37`.
- Validation: passed; properties exactly `model,prompt,stream`; no UTF-8 BOM.
- Transmission: Windows `curl.exe` 8.21.0 with `--data-binary @<request-file>`; no hand-escaped command-line JSON.
- Model: only `qwen2.5:0.5b-instruct-q4_K_M`, size 397,821,319 bytes, digest `a8b0c51577010a279d933d14c2a8ab4b268079d44c5c8830c0a93900f1827c67`.

### Result and evidence

- `/api/generate` calls in Attempt 3: exactly 1.
- curl exit: 0.
- HTTP: `200 OK`; `Content-Type: application/json; charset=utf-8`; content length 663 bytes.
- curl time: 11.266288 s; independently measured wall clock: 11,307 ms.
- Complete response: yes; `done:true`, `done_reason:"stop"`.
- Generated text: exactly `REPLY:Hello Parker.`.
- Ollama wire compatibility: yes; raw body contains compact `"response":"`.
- Parker tagged-parser structural compatibility: yes; nonblank `REPLY:` branch.
- RAM immediately before: 2,617,159,680 bytes (about 2.437 GiB).
- RAM immediately after: 2,083,733,504 bytes (about 1.941 GiB).

Exact response body (663 UTF-8 bytes):

```json
{"model":"qwen2.5:0.5b-instruct-q4_K_M","created_at":"2026-08-11T08:14:44.3002593Z","response":"REPLY:Hello Parker.","done":true,"done_reason":"stop","context":[151644,8948,198,2610,525,1207,16948,11,3465,553,54364,14817,13,1446,525,264,10950,17847,13,151645,198,151644,872,198,785,1196,1053,25,21927,28206,13,5209,9851,26753,13,3411,6896,825,1555,304,419,3561,25,3596,24834,31252,6280,16278,1180,1467,14276,3155,537,2550,4113,1573,476,1283,429,1555,13,151645,198,151644,77091,198,787,24834,25,9707,28206,13],"total_duration":11260565700,"load_duration":10296534100,"prompt_eval_count":66,"prompt_eval_duration":700323000,"eval_count":7,"eval_duration":247055000}
```

Ollama diagnostics prove the model loaded and inference occurred: `Qwen2.5 0.5B Instruct`, 494.03M parameters, 373.73 MiB CUDA model buffer plus 137.94 MiB host buffer; `llama_server: model loaded`; 66 prompt tokens evaluated in 700.32 ms; 7 generated tokens evaluated in 247.06 ms; request total duration 11.2605657 s including 10.2965341 s load duration; and a completed HTTP 200 response. These values establish load, prompt evaluation, generation, and completion without implying broader quality.

### Classification and narrow meaning

**A — FIXTURE STRUCTURALLY COMPATIBLE.** The selected Windows-local test fixture completed inference and returned output structurally consumable by Parker's current tagged-response parser.

This proves only that the selected Windows-local **TEST FIXTURE** satisfies the narrow transport/tagged-output prerequisite for a separately authorized Basic Owner UI live verification. It does not prove Parker production-model suitability, Reasoning Protocol conformance, semantic reliability, remedy efficacy, model qualification, production parity, Memory, Goals, Planner, tools, or general Parker correctness. The model remains **TEST FIXTURE ONLY — NOT PARKER PRODUCTION MODEL SELECTION**.

### Attempt 3 teardown

The `ollama.exe` supervisor and detached `llama-server.exe` worker (507,748,352-byte working set at teardown) were stopped. No related process and no port 11434 listener remained. Final RAM was 2,636,779,520 bytes (about 2.456 GiB). Ollama and the downloaded model remain installed. Temporary request, response, header, and diagnostic log files remain outside the repository under `C:\Users\steve\AppData\Local\Temp`.

## Repository changes

Exactly one repository file was created:

- `docs/reviews/BASIC_OWNER_UI_WINDOWS_LOCAL_MODEL_FIXTURE_INSTALLATION_AND_PREFLIGHT_REVIEW.md`

No UI, runtime, reasoning, Memory, Goals, Planner, Knowledge Submission, tool, test, Gradle, Unit 3-C, remedy, campaign, or production configuration file changed.

## Server and governance isolation

No Parker server address, filesystem, service, credential, Ollama instance, Qwen instance, or API was accessed. No cloud model/API was used. No deployment or production-model selection occurred. The paused Reasoning Protocol programme remained untouched.

## Readiness determination

**READY FOR SEPARATELY AUTHORIZED UI LIVE VERIFICATION: YES, FOR THE NARROW FIXTURE PREREQUISITE ONLY.**

Attempt 3 closes the fixture's narrow transport/tag prerequisite. Attempts 1 and 2 remain part of the evidence history and are not concealed or reclassified. Attempt 1's server-side fate remains unprovable, but that no longer blocks the narrow fixture determination because Attempt 3 independently used valid request bytes and captured a complete compatible response.

Exact next step: hard stop here. Review this result, then seek separate explicit authorization for the Basic Owner UI live verification. Do not launch ParkerRuntime or the UI under this task.
