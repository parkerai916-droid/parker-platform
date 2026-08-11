# Basic Owner UI Windows-Local Model Fixture Installation and Bounded Preflight Review

Date: 2026-08-11 (Pacific/Auckland)

## Status

**STOPPED AT MANDATORY RESOURCE GATE AFTER RUNTIME INSTALLATION. NOT READY FOR SEPARATELY AUTHORIZED UI LIVE VERIFICATION.**

Native Ollama was installed and its loopback-only metadata endpoint was verified. The selected model was not acquired or executed because available RAM fell below the task's hard 2.0 GB threshold after installation. Exactly zero `/api/generate` calls occurred.

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

The pre-install reading exceeded the authorized minimum. The post-install reading did not. The instruction says to stop if available RAM is below 2.0 GB before model execution; therefore model acquisition and all inference were stopped. The post-stop reading remained below 2.0 GB.

Disk observations:

- C: initially 41,245,876,224 bytes free; 36,691,050,496 bytes free while the installer file and installed runtime were present; 38,257,655,808 bytes free after deleting only the temporary installer.
- D: initially 66,559,803,392 bytes free; 66,559,541,248 bytes free after creating the empty fixture model-storage structure.
- Installed Ollama tree measured 2,942,724,891 bytes.
- Selected-model bytes: zero; the local API reported zero models.

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

**Not pulled.** The hard RAM gate failed before acquisition. Consequently:

- exact installed model identifier: none;
- reported model size: none;
- digest: none;
- model-storage payload: zero bytes;
- fallback model: not pulled or substituted.

## Endpoint and listener result

After installation, Ollama listened only on:

`127.0.0.1:11434`

The owner process was the newly installed local `ollama.exe`. No `0.0.0.0`, LAN, tunnel, proxy, cloud, or Parker-server endpoint was used.

Permitted non-generative metadata checks:

- `GET http://127.0.0.1:11434/api/version` returned `0.32.8`;
- `GET http://127.0.0.1:11434/api/tags` returned zero installed models.

No transport `POST` was sent because the selected model was unavailable and the RAM stop boundary had already been reached. Source-level compatibility remains established by the committed selection review, but installed-fixture request handling was not exercised in this stopped run.

## Bounded protocol preflight

- Authorized maximum `/api/generate` inference calls: 1.
- Actual `/api/generate` calls: **0**.
- Exact request purpose: not exercised.
- Model response: none.
- Tagged-response classification: not determined.
- Compatibility with Parker's existing `TaggedReasoningResponseParser`: not empirically determined.

No retry, fallback, prompt tuning, Unit 3-C fixture, campaign prompt, quality evaluation, ParkerRuntime launch, or graphical UI launch occurred.

## Teardown state

After the RAM failure, both installed fixture processes (`ollama app.exe` and `ollama.exe`) were stopped. Final listener count on port 11434: zero. No model was loaded or required unloading.

Ollama remains installed, as the task prohibited uninstalling a successfully installed fixture unless safety required it. The empty/disposable model root and the two user variables remain configured. The downloaded installer has been deleted from the Windows temporary directory.

To restart for a future separately authorized continuation after memory is freed:

1. Recheck available physical RAM and require at least 2.0 GB before starting Ollama; preferably provide the 3 GB margin recommended by the selection review.
2. Start the installed native Ollama application from the Windows Start menu, inheriting `OLLAMA_HOST` and `OLLAMA_MODELS`.
3. Confirm only `127.0.0.1:11434` is listening.
4. Under explicit continuation authority, pull only `qwen2.5:0.5b-instruct-q4_K_M`, recheck RAM/disk, and perform at most the one still-unused `/api/generate` preflight call.

The actual Parker UI live verification requires another separate authorization after the fixture installation/preflight review reaches Ready.

## Repository changes

Exactly one repository file was created:

- `docs/reviews/BASIC_OWNER_UI_WINDOWS_LOCAL_MODEL_FIXTURE_INSTALLATION_AND_PREFLIGHT_REVIEW.md`

No UI, runtime, reasoning, Memory, Goals, Planner, Knowledge Submission, tool, test, Gradle, Unit 3-C, remedy, campaign, or production configuration file changed.

## Server and governance isolation

No Parker server address, filesystem, service, credential, Ollama instance, Qwen instance, or API was accessed. No cloud model/API was used. No deployment or production-model selection occurred. The paused Reasoning Protocol programme remained untouched.

## Readiness determination

**READY FOR SEPARATELY AUTHORIZED UI LIVE VERIFICATION: NO.**

Blocking issues:

1. Available physical RAM is below the mandatory 2.0 GB threshold even after stopping Ollama.
2. The selected model is not acquired and has no recorded size/digest in this installation.
3. The installed endpoint has not handled the Parker-shaped POST.
4. The single authorized protocol preflight remains unused, so tagged-parser compatibility is not empirically established.

Exact next step: free memory (close nonessential applications or reboot), then perform a fresh read-only RAM check. Only when at least 2.0 GB is available should a separately authorized continuation start Ollama, pull the already-selected model, verify the listener/request shape, use the one-call protocol preflight budget, stop the fixture, and update readiness. Do not launch ParkerRuntime or the UI in that continuation.
