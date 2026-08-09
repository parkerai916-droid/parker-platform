**Status:** Unit 2-D Diagnostic Characterisation Implementation Readiness Review — **PASS.** Implementation-only review against committed baseline `a859ba5`, the frozen Scope Lock, the frozen Implementation/Execution Plan, and the accepted Completion Review and its Independent Constitutional Review. No Unit 2-D campaign call has executed. Nothing is staged, committed, or pushed.

# Unit 2-D Diagnostic Characterisation — Implementation Readiness Review

## 1. Scope

Confirms the implementation is *capable of* correct, isolated, fail-closed live execution if and when separately authorized. This review does not authorize that execution.

## 2. Exact execution configuration contract

Reused, unmodified: `PARKER_REASONING_EVAL_ENDPOINT_URL`, `PARKER_REASONING_EVAL_MODEL_NAME` (must equal `qwen2.5-coder:7b`), `PARKER_REASONING_EVAL_TIMEOUT_MS` (must equal `90000`), `PARKER_REASONING_EVAL_OUTPUT_PATH`, `PARKER_REASONING_EVAL_REPOSITORY_COMMIT`, `PARKER_REASONING_EVAL_MODEL_DIGEST`, `PARKER_REASONING_EVAL_RUNTIME_IMAGE_ID` (Unit 1's own `LiveEvaluationConfigLoader`, reused directly).

New, Unit 2-D-specific: `PARKER_REASONING_DIAGNOSTIC_CAMPAIGN_ID`, `PARKER_REASONING_DIAGNOSTIC_ARTIFACT_ROOT`, `PARKER_REASONING_DIAGNOSTIC_UBUNTU_RUNTIME_ID`, `PARKER_REASONING_DIAGNOSTIC_CONTAINER_ID` (optional), `PARKER_REASONING_DIAGNOSTIC_QWEN_MODEL_SHOW_SHA256`, `PARKER_REASONING_DIAGNOSTIC_LLAMA_MODEL_NAME` (must equal `llama3.2:3b`), `PARKER_REASONING_DIAGNOSTIC_LLAMA_MODEL_DIGEST`, `PARKER_REASONING_DIAGNOSTIC_LLAMA_MODEL_SHOW_SHA256`.

Any missing or partial value fails redacted via `EvaluationConfigurationException`/`IllegalArgumentException` (`partial diagnostic configuration fails redacted`, `complete offline diagnostic configuration loads and pins both model identities` — both passing, offline, no live call).

## 3. Campaign identity

Must be machine-safe (`[a-z0-9][a-z0-9.-]*`), must contain the literal substring `diagnostic`, and must not equal `qwen25coder7b-baseline-20260809`. Enforced twice: once at `DiagnosticCampaignDefinition` construction (applies to every use, including offline tests), once again at `DiagnosticConfigLoader.campaignArtifactRoot` (applies specifically to the resolved live artifact path). Both are exercised by passing offline tests.

## 4. Model identity requirements

Exactly two models recognized, by name, nowhere else configurable: `qwen2.5-coder:7b` (production track, all of DQ1–DQ4 and both warm-ups except the Llama one) and `llama3.2:3b` (DQ4 and its own warm-up only). Both require an immutable digest matching `[0-9a-fA-F]{64}` plus a hashed `/api/show` evidence value; both fail closed on a missing, blank, or malformed digest. No third model name is accepted anywhere in the config loader or campaign definition — confirmed by the loader's `require(... == DIAGNOSTIC_QWEN_MODEL_NAME)` / `require(... == DIAGNOSTIC_LLAMA_MODEL_NAME)` checks, which reject any other configured name rather than accepting it. No code path downloads, installs, or substitutes a model.

## 5. Runtime identity requirements

Repository commit, Ubuntu runtime identity, and (optional) container identity are captured into `DiagnosticIdentity` and folded into its `fingerprint`; any later mismatch against a previously-written `campaign-identity.txt` fails closed (`writeDefinitionAndIdentity`, exercised by `campaign identity mismatch fails closed`, passing offline).

## 6. Artifact path

Must resolve to exactly one directory beneath `/var/lib/parker/reasoning-protocol-live-model/` (the same accepted parent Unit 2 uses) and must not equal or nest inside `qwen25coder7b-baseline-20260809`. Enforced at config-load time (`DiagnosticConfigLoader.campaignArtifactRoot`) and again, independently, at runner-start time (`DiagnosticCampaignRunner.run`'s first statement) — defense in depth for both the live path and any direct test/operator construction.

## 7. Free-space rule

`DIAGNOSTIC_MINIMUM_FREE_BYTES = 2 GiB`, checked via `Files.getFileStore(root).usableSpace` immediately after directory creation and before the lock is acquired, matching Unit 2's own threshold and convention exactly.

## 8. Live task selection

`./gradlew reasoningProtocolUnit2DDiagnostic` — filtered to `parker.integration.ReasoningProtocolDiagnosticCharacterisationTest` only, setting `parker.reasoning.diagnostic.enabled=true`, detached from `test`/`check`/`build`/`assemble` (Completion Review Sections 10; independently re-confirmed Section 9 of its Independent Constitutional Review). Selecting this task alone is not sufficient to reach a live call: the live entry point additionally requires the complete environment configuration in Section 2 above, absent by default.

## 9. Zero collision with Unit 2

Confirmed at three independent layers: (a) campaign-identity construction rejects the Unit 2 campaign ID and requires a `diagnostic` marker; (b) config-loader artifact-root resolution rejects any path equal to or nested inside Unit 2's directory; (c) the runner's own startup check repeats (b) independently of how it was constructed. Read-only host verification, repeated in both the Completion Review and its Independent Constitutional Review, confirms Unit 2's artifacts remain byte-identical and no `diagnostic`-named directory exists anywhere on the artifact filesystem.

## 10. 24-call schedule

Reconfirmed present as a compiled-in invariant (`check()` in `DiagnosticCampaignDefinition.init`), not merely a test assertion — enforced at every construction site, including the live entry point, which cannot proceed with any other trial count.

## 11. Execution stop behavior

Hard stops (identity/configuration/harness/artifact-integrity defects, or a `DiagnosticHardStopException`) halt immediately and write `campaign.halted` with a reason code; no automatic resumption is possible afterward without a fresh runner invocation succeeding past the same checks. Semantic failure, at any rate including 24/24, is not a stop condition and does not prevent `campaign.sealed`. Both directions are independently tested (Completion Review Section 8; its Independent Constitutional Review Section 7).

## 12. Interpretation boundaries

The remedy firewall (Plan Section 16) is unaffected by implementation — no structured output, prompt rewriting beyond the one firewalled DQ5 variant, retry, model replacement, or sampling change exists anywhere in the new code. The six named overclaiming traps (Plan Section 12) remain textual/procedural obligations on whoever populates `interpretation-worksheet.md` after a live run; the implementation does not and cannot enforce them mechanically, and does not attempt to auto-generate that document. DQ5's content-fidelity non-evidentiary status is the one interpretation boundary given mechanical, tested support (Completion Review Section 5) beyond documentation, since it follows directly and unavoidably from the fixed placeholder design rather than from operator discipline.

## 13. Readiness verdict

```text
PASS
```

## 14. Execution authorization status

```text
LIVE EXECUTION IS NOT AUTHORIZED BY THIS REVIEW.
```

Per the frozen Plan's own Section 17, readiness is one of four required gates (Independent Constitutional Review of the Plan — already accepted; Implementation Readiness Review — this document; its own Independent Constitutional Review — next; explicit execution approval — a separate, later governance act). A PASS readiness verdict, even after its own accepted Independent Constitutional Review, does not itself constitute that explicit execution approval. No live call is authorized by this document.
