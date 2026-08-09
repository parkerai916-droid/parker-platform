**Status:** Unit 1 Completion Review — **PASS**. Reviewed against baseline `05d4c2a`, the accepted programme Planning Review, Unit 1 Scope Lock, Implementation Plan, their independent reviews, and the PASS Boundary Review. Nothing is staged, committed, or pushed.

# Reasoning Protocol Live-Model Conformance and Structured-Output Reliability — Unit 1 Evaluation Harness Completion Review

## 1. Exact implementation diff

The implementation surface is exactly:

- `build.gradle.kts` — detached source set and explicit task;
- `tests/integration/ReasoningProtocolLiveModelEvaluationHarness.kt` — test-only evaluation instrument; and
- `tests/integration/ReasoningProtocolLiveModelConformanceTest.kt` — deterministic instrument verification and assumption-gated live smoke entry.

No `src/**`, existing test, dependency, plugin, `.gitignore`, settings, production configuration, or accepted governance artifact changed. Build output remains under ignored `build/`.

## 2. Detached Gradle boundary

`liveModelEvaluation` is a new test-only source set rooted at `tests/integration`. `reasoningProtocolLiveModelEvaluation` is the only task that selects its output. It extends existing test dependency configurations but adds no dependency.

Gradle dry runs independently prove `test`, `check`, and `build` select only their existing ordinary task graphs; none includes compilation or execution of `liveModelEvaluation` or `reasoningProtocolLiveModelEvaluation`. `shouldRunAfter(test)` is ordering metadata only and creates no dependency in either direction.

## 3. Explicit configuration and skip behavior

The harness reads only `PARKER_REASONING_EVAL_*` values. Endpoint, model, positive timeout, artifact path, and repository commit are mandatory for a live call. Digest, runtime image identity, and positive repetitions are optional. There is no localhost, production-config, ParkerRuntimeConfigLoader, model, endpoint, or timeout fallback.

If no mandatory value is present, configuration is `Absent` and the live JUnit method skips before constructing the harness or HTTP client. Optional identity values alone do not activate it. Partial or invalid mandatory configuration fails with key names and redacted values. URI user-info, query, fragment, non-HTTP(S) schemes, and output beneath `src`, `tests`, or `docs` are rejected.

## 4. Real production observation path

Every trial constructs immutable synthetic `Turn` and `ReasoningContext` values and calls:

```text
DefaultReasoningPromptBuilder
    -> ModelReasoningProvider
    -> LocalHttpModelInferenceClient
    -> configured endpoint/model
    -> TaggedReasoningResponseParser
```

The request callback calls `defaultOllamaRequestBody` once and returns its exact body. The response callback captures the raw body, calls `defaultOllamaResponseBody` once, and returns its exact extracted string. The harness asserts its captured prompt equals a production-builder result. It performs no mutation, normalization, constraint, repair, or retry.

## 5. Fixture, context, and repetition support

The immutable seed corpus structurally covers Goal, Reply, Remember, NoAction, and an adversarial quoted-Remember negative. Every fact and identifier is explicitly synthetic.

All nine frozen profile IDs are implemented. The minimal and selection-guidance controls use an empty/minimal `ReasoningContext` through the production builder; no simplified prompt is copied. Other profiles construct synthetic identity/channel/time/conversation, duplicated request, history, memory, world belief, tool, and mixed/full entries. Stable hashes prove repetition does not mutate fixture/profile inputs.

## 6. Result semantics and fail-closed behavior

The typed production parser result alone determines actual action. Post-parse evidence classification cannot return a `ReasoningProviderResponse` or invoke another component.

- A: correct valid action and exact/not-applicable content;
- B: correct valid action with content deviation/paraphrase;
- C: malformed/unknown tag;
- D: wrong valid action;
- E: untagged prose;
- F: multiple tagged outputs;
- G: blank/partial output;
- H: provider timeout; and
- I: transport/model/envelope failure.

J context drift and K repeatability failure are derived without replacing primary outcomes. Tests prove `MEMBER:` remains C, and `NOACTION`/`REPLY:` for Remember remain representation-valid D. Multiple output is recorded as F even where the current prefix parser produces a typed first variant; that diagnostic classification neither repairs nor changes the parser result.

## 7. Evidence artifact

The deterministic UTF-8 JSONL writer emits one ordered run header and one ordered record per trial. It records run/fixture/profile/sequence and model/config identity; exact synthetic prompt and SHA-256; request body; raw Ollama envelope; extracted response; parsed variant or parser classification; expected/actual actions; representation and content results; latency; available token/timing metadata; A–I and J/K evidence; and pass/fail.

The configured path is explicit. `build/reports/reasoning-protocol-live-model/` is the recommended already-ignored location. The writer does not use Parker logging or a JSON dependency and does not serialize environment maps, endpoint credentials, or real content.

## 8. Consequential side-effect isolation

The instrument has no `ParkerRuntime`, reply coordinator, Goal planner, execution pipeline, Memory admission, Memory Core, Knowledge submission, or persistence dependency. Parsed Goal and Remember values become observations and are discarded. Reflection verification checks the harness object graph for these forbidden dependencies.

## 9. Verification evidence

Targeted command:

```text
gradlew reasoningProtocolLiveModelEvaluation --no-daemon
```

Result: **PASS** — 16 tests, 15 passed, 1 skipped, 0 failed. The single skip is the live smoke because none of the five mandatory evaluation variables was present. Therefore live smoke is **NOT EXECUTED**, as authorized, and this is not a Unit 1 failure.

Lifecycle dry runs:

- `gradlew test --dry-run` — PASS; no live source/task;
- `gradlew check --dry-run` — PASS; no live source/task; and
- `gradlew build --dry-run` — PASS; no live source/task.

Ordinary full suite:

```text
2,015 tests completed; 1 failed; 8 skipped
```

The sole failure is the known Windows `OcrStructuralIsolationTest.kt:338` path-separator portability issue. It is unrelated to Unit 1, predates this diff, and produced no repository modification. No Unit 1 test is part of the ordinary suite.

`git diff --check` passes.

## 10. Remedy and Unit 2 exclusions

No production prompt/parser/client/configuration changed. No sampling option, structured output, schema, grammar, retry, semantic repair, classifier/renderer split, model switch, model verdict, timeout-default change, production logging, or downstream acceptance path was implemented.

No live statistical campaign ran. Qwen, prompt adequacy, structured output, model replacement, and qualification remain undetermined. Unit 2 has not started.

## 11. Implementation iteration and defect determination

Before completion review, compilation identified and corrected two local harness syntax/API errors. A subsequent review corrected optional-only configuration activation and added an explicit parser-exception classification field. All corrections remained within the two authorized integration files and were verified before the completion boundary.

No defect exists in the completed implementation. A Defect Confirmation Review is therefore not required.

## 12. Completion verdict

```text
PASS
```

Unit 1 meets its success criterion: the configured live boundary can be measured reproducibly without altering production behavior, creating consequential side effects, or contaminating ordinary builds. Corrective action: **NONE**.
