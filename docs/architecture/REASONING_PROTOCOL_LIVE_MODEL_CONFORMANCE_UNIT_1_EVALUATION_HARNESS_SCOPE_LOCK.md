**Status:** Unit 1 Scope Lock — **ACCEPTED**, subject to its accepted Independent Constitutional Review. Governance only against baseline `05d4c2a`; no implementation authority is implied before the required Unit 1 Boundary Review.

# Reasoning Protocol Live-Model Conformance and Structured-Output Reliability — Unit 1 Opt-in Evaluation Harness Scope Lock

## 1. Authority and purpose

This Scope Lock implements only immediate step A from the accepted programme Planning Review: create a trustworthy opt-in instrument that measures whether an explicitly configured live model conforms to Parker's current reasoning protocol.

Unit 1 succeeds when the existing production reasoning boundary can be exercised reproducibly and its successes and failures can be recorded. It does not succeed or fail according to whether Qwen, or any other model, passes.

The measured boundary is:

```text
synthetic Turn + synthetic ReasoningContext
    -> DefaultReasoningPromptBuilder
    -> ModelReasoningProvider
    -> LocalHttpModelInferenceClient
    -> explicitly configured live endpoint/model
    -> TaggedReasoningResponseParser
    -> recorded typed result or recorded failure
```

The harness terminates at that result. It never invokes conversation reply, Goal planning, Memory admission, Knowledge submission, persistence, or delivery.

## 2. Observation-only invariant

For production-conformance trials the harness must instantiate and call the production classes named above. It may use the existing `LocalHttpModelInferenceClient` request/response formatter injection points solely as transparent capture wrappers around `defaultOllamaRequestBody` and `defaultOllamaResponseBody`. The wrappers must pass the exact formatter input and output through unchanged.

No alternate reasoning provider, transport, prompt builder, response parser, semantic classifier, normalizer, repairer, constraint, retry, or downstream runtime is permitted. Expected-versus-actual evaluation occurs after production parsing and is evidence classification only; it cannot supply a runtime result.

## 3. Exact opt-in boundary

Unit 1 may add one Gradle source set rooted at `tests/integration` and one explicit task named `reasoningProtocolLiveModelEvaluation`. That source set must not be a dependency of `main`, `test`, `build`, `check`, or any ordinary lifecycle task. `./gradlew test` remains byte-for-byte independent of Ollama and network state.

Invoking the task is necessary but insufficient for a live call. The live portion runs only when all mandatory evaluation variables are present and valid:

- `PARKER_REASONING_EVAL_ENDPOINT_URL`
- `PARKER_REASONING_EVAL_MODEL_NAME`
- `PARKER_REASONING_EVAL_TIMEOUT_MS`
- `PARKER_REASONING_EVAL_REPOSITORY_COMMIT`
- `PARKER_REASONING_EVAL_OUTPUT_PATH`

Optional identity inputs are `PARKER_REASONING_EVAL_MODEL_DIGEST` and `PARKER_REASONING_EVAL_RUNTIME_IMAGE_ID`. Repetition defaults to one and may be set by `PARKER_REASONING_EVAL_REPETITIONS` to a positive integer.

If any mandatory live configuration is absent, the live test is reported as **SKIPPED / NOT EXECUTED** before constructing the HTTP client. Partial or invalid configuration fails the opt-in task as configuration error; it must never fall back to Parker production variables, localhost, a named model, a default endpoint, or mutable machine state.

The endpoint must be HTTP(S), contain no URI user-info, and be recorded only as a credential-free canonical identifier with query and fragment removed. Secrets in headers, query parameters, or artifacts are prohibited.

## 4. Synthetic-only fixtures

Every fixture is repository-defined synthetic data with a stable ID, synthetic owner message, expected action, content constraint, applicable context-profile IDs, and tags describing positive, negative, adversarial, or mixed-intent status. No fixture may be imported from production logs or contain real owner, case, message, Memory, Knowledge, world-belief, or tool facts.

The Unit 1 seed corpus must structurally cover the four actions and the positive, negative, ambiguous, quoted, injected, mixed-intent, whitespace, Unicode, and long-context families frozen by the programme review. Unit 1 proves that these fixture shapes can be expressed and measured; Unit 2 owns corpus sufficiency, repetitions, statistical execution, and qualification evidence.

Content assessment is deterministic where possible: exact, meaning-preserving paraphrase requiring later human review, material omission, material mutation, invention, not applicable, or indeterminate. Unit 1 must not introduce an LLM judge. An indeterminate result cannot pass content fidelity.

## 5. Production prompt and context profiles

All conformance prompts come from `DefaultReasoningPromptBuilder`. The harness constructs only valid synthetic `Turn`, `ReasoningSubject`, and `ReasoningContext` inputs.

It must support stable profile IDs for:

1. `minimal-production-context` — the smallest valid synthetic Turn and empty/minimal Reasoning Context passed through the production builder;
2. `production-selection-guidance` — a control asserting and hashing the production builder's own invariant selection guidance, not a copied prompt;
3. `identity-channel-time-conversation`;
4. `duplicated-current-request` — the current production-like duplication represented through synthetic context plus the builder's owner-message rendering;
5. `conversation-history`;
6. `knowledge-source-memory`;
7. `world-belief`;
8. `tool-context`; and
9. `mixed-full-production-like`.

“Minimal” in Unit 1 means minimum production-generated context. A manually simplified prompt is not a production-conformance profile and is forbidden. The selection guidance is invariant inside the current builder, so profiles 1 and 2 are controls over the same production-generated instruction layer rather than authority to inject a second template.

Profile data, order, timestamps, identifiers, and sizes are fixed and synthetic. The structure supports matched and repeated trials without modifying fixtures.

## 6. Parser and deterministic failure classification

Every extracted output is passed unchanged to `TaggedReasoningResponseParser`. Its typed result supplies the actual action. Its exception supplies representation failure evidence. The harness may inspect raw output after rejection only to assign a diagnostic subclass; this inspection never returns a `ReasoningProviderResponse`.

The frozen classification rules are:

- correct typed action, valid tag, exact faithful content -> A;
- correct typed action, valid tag, non-exact content -> B plus fidelity subclass;
- unknown/malformed leading tag -> C;
- parsed action differs from expected action -> D, even though representation is valid;
- untagged prose -> E;
- more than one tagged output or valid tag plus extra tagged/prose output -> F;
- blank, truncated, or partial output -> G;
- `TimeoutCancellationException` from the governed provider timeout -> H;
- HTTP, envelope, endpoint, model-load, or other inference failure -> I;
- changed action between a fixture's matched context profiles -> J, computed only after the individual trials remain classified;
- inconsistent results among identical fixture/profile/model/configuration repetitions -> K, computed only after individual classifications remain preserved.

Examples are immutable: `MEMBER:` is malformed; `NOACTION` or `REPLY:` for a Remember fixture is representation-valid but semantically wrong. None is reinterpreted as Remember.

## 7. Repetition and identity

Each trial has a stable fixture ID, context-profile ID, model/configuration identity, run ID, and one-based trial sequence. Repetition reconstructs equivalent inputs without modifying the fixture. Unit 1 provides this capability but performs no Unit 2 statistical campaign and declares no model qualification.

The identity is the exact repository commit supplied for the run, model name, optional digest, credential-free endpoint identifier, timeout, runtime image ID when available, and current production request-format identity. A model name without a digest is recorded as an identity limitation, not silently treated as immutable.

## 8. Dedicated evidence artifact

The output is deterministic UTF-8 JSON Lines: one run-header record followed by one trial record per attempt, written only to the explicitly configured path. Stable field order and escaping are required. The path must be outside tracked source/fixture directories; Unit 1 must add no generated artifact to Git.

Each trial records repository commit; run, fixture, context-profile and sequence IDs; model name and optional digest; sanitized endpoint identifier; timeout; optional runtime image identity; prompt SHA-256; the full synthetic prompt; raw HTTP/Ollama response envelope; extracted response field; parser variant or exception class; expected and actual action; representation-valid flag; fidelity result; latency; endpoint token/evaluation counts when supplied; primary A–I result; derived J/K flags when available; and pass/fail.

The response-envelope capture must not change `LocalHttpModelInferenceClient` behaviour. If token metadata cannot be extracted without altering production behaviour, it is recorded as unavailable. Artifacts are evaluation evidence, not ordinary Parker logs. Retention and access are operator-controlled; synthetic-content validation occurs before writing.

## 9. Consequential side-effect and fail-closed boundary

The harness may construct `ModelReasoningProvider` directly but may not construct or invoke `ParkerRuntime`, `ConversationReplyCoordinator`, Planner/Goal execution, `MemoryAdmissionCoordinator`, `KnowledgeSubmission`, Memory Core, or Knowledge persistence. A parsed `Remember` or `Goal` is recorded and discarded.

Malformed, multiple, blank, or unknown output remains rejected. A wrong valid action remains wrong. No fixture inspection, expected-action lookup, content matcher, or artifact serializer may create or replace a typed production result. Timeout is H and transport/model failure is I; neither is a semantic answer.

## 10. Explicit exclusions

Unit 1 must not implement or decide:

- prompt or selection-hierarchy changes;
- sampling/determinism options;
- JSON, JSON Schema, grammar, or structured output;
- invalid-output or semantic retry;
- semantic repair or heuristic tag recovery;
- classifier/renderer separation;
- production model selection, replacement, or qualification;
- production timeout changes;
- production logging changes;
- Parker configuration changes;
- downstream Remember/Goal acceptance tests;
- the Unit 2 live campaign or any statistical verdict; or
- any Gap #54, authorization, Memory, Knowledge, or OCR change.

## 11. Authorized file surface

Subject to an accepted Boundary Review and Implementation Plan, Unit 1 may change only:

- `build.gradle.kts` — isolated evaluation source set and explicit task;
- `tests/integration/ReasoningProtocolLiveModelEvaluationHarness.kt` — test-only fixture, profile, execution, classification, configuration, and artifact support;
- `tests/integration/ReasoningProtocolLiveModelConformanceTest.kt` — offline instrument tests and the assumption-gated live entry point; and
- Unit 1 Planning, Boundary, Completion, and Independent Review documents.

Production Kotlin, existing tests, existing governance, and dependency versions are forbidden. Discovery of a necessary production seam, dependency, existing-test modification, or additional file is a Boundary Review stop condition.

## 12. Completion and Unit 2 handoff

Unit 1 is complete only when deterministic evidence proves configuration absence skips before network access; fixture/profile construction is stable; the real builder, client, provider, and parser are used; raw and parsed evidence is captured; D/C/H/I are distinguished; JSONL is deterministic; repetition and all nine profiles are expressible; parsed consequential actions cause no downstream effect; the live task can target explicit configuration; and `test` plus the full suite remain offline and pass subject only to separately classified pre-existing environmental issues.

Completion also requires targeted verification, full-suite verification, Completion Review, and Independent Constitutional Review. A Defect Confirmation Review is required only for a genuine defect.

Unit 2 alone runs the frozen baseline characterisation campaign and may assess Qwen, prompt adequacy, structured-output necessity, model replacement, or qualification. Unit 1 makes none of those determinations.

## 13. Disposition

```text
ACCEPTED
```

Implementation remains blocked pending acceptance of the Unit 1 Implementation Plan and the separately required Unit 1 Boundary Review.
