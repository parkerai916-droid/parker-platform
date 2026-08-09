**Status:** Unit 1 Implementation Plan — **ACCEPTED**, subject to its accepted Independent Constitutional Review. This plan authorizes no implementation until the required Unit 1 Boundary Review is accepted and explicit approval to implement is given.

# Reasoning Protocol Live-Model Conformance and Structured-Output Reliability — Unit 1 Opt-in Evaluation Harness Implementation Plan

## 1. Implementation objective

Implement the smallest test-only instrument that reproducibly observes the current production prompt/provider/client/parser path against an explicitly configured live model. It must not alter that path, make live access part of ordinary builds, or exercise downstream consequential effects.

Production Kotlin changes: **NONE**.

Existing test changes: **NONE**.

External dependency changes: **NONE**.

## 2. Exact file plan

1. **`build.gradle.kts`**
   - add a `liveModelEvaluation` Kotlin/JVM test source set rooted only at `tests/integration`;
   - extend its compile/runtime classpaths from main output and existing test dependencies without adding a library;
   - register `reasoningProtocolLiveModelEvaluation` as an explicit JUnit Platform `Test` task;
   - do not attach it to `test`, `check`, `build`, or another lifecycle task; and
   - leave the existing `test` source set and task unchanged.

2. **`tests/integration/ReasoningProtocolLiveModelEvaluationHarness.kt`**
   - test-only immutable fixture, context profile, configuration, trial result, taxonomy, runner, endpoint sanitizer, hashing, content assessment, and JSONL writer;
   - transparent capture callbacks around the production Ollama formatter/parser functions;
   - no second provider, client, prompt builder, parser, or semantic inference path.

3. **`tests/integration/ReasoningProtocolLiveModelConformanceTest.kt`**
   - deterministic offline tests of the instrument;
   - one assumption-gated live entry point using explicit configuration;
   - no `ParkerRuntime` construction and no downstream effect assertion.

4. Unit 1 Boundary, Completion, and Independent Review documents as the workflow advances.

Any need to modify `src/**`, existing `tests/runtime/**`, existing `tests/composition/**`, dependencies, or any other file stops implementation for Boundary Review reassessment.

## 3. Instrument data shapes

Keep all shapes test-only and immutable:

- `ConformanceFixture`: stable ID, synthetic owner text, expected action enum, content rule, allowed profiles, fixture-family flags, and a hard-coded `synthetic=true` marker validated against prohibited-source metadata.
- `ContextProfile`: stable ID and a pure constructor of fixed synthetic `ReasoningContext` entries plus the valid synthetic `Turn`/subject data required by the production builder.
- `LiveEvaluationConfig`: endpoint URI, model name, positive timeout, supplied repository commit, output path, optional model digest/image ID, and positive repetitions.
- `TrialIdentity`: run ID, fixture ID, profile ID, model/config identity, and sequence.
- `TrialObservation`: hashes, captured envelope/extracted text, parser result/error, expected/actual action, representation and fidelity classifications, latency/token metadata, primary taxonomy class, J/K derived flags, and pass/fail.

No shape becomes a production contract. Use existing `ReasoningProviderResponse` types for actual parsed variants.

## 4. Configuration and skip design

The test reads only the `PARKER_REASONING_EVAL_*` variables frozen by the Scope Lock. It must not call `ParkerRuntimeConfigLoader` or fall back to `PARKER_MODEL_*` production configuration.

The live JUnit method first loads configuration without constructing a client. If every mandatory variable is absent, JUnit `assumeTrue(false, ...)` records skip. If only some are present or a value is invalid, fail with a redacted configuration message. The endpoint parser rejects user-info and non-HTTP(S) schemes. The output path is required for a live run and must not resolve under `src`, `tests`, or `docs`.

The explicit Gradle task plus mandatory evaluation namespace forms the opt-in boundary. Ordinary `./gradlew test` neither compiles nor executes the integration source set.

## 5. Production-chain construction

For each trial:

1. construct a fresh synthetic request from the immutable fixture/profile;
2. instantiate `DefaultReasoningPromptBuilder`;
3. instantiate `LocalHttpModelInferenceClient` with the configured endpoint/model;
4. provide a request formatter callback that records the exact generated prompt/body and delegates unchanged to `defaultOllamaRequestBody`;
5. provide a response parser callback that records the raw HTTP body, delegates unchanged to `defaultOllamaResponseBody`, and records the extracted response;
6. instantiate `TaggedReasoningResponseParser`;
7. construct `ModelReasoningProvider` with these real collaborators and the evaluation timeout;
8. call `reason` once; and
9. record the typed response or caught failure without invoking another Parker component.

The runner performs no retry. Each requested repetition is a distinct one-attempt trial.

## 6. Fixtures and profiles

Seed one or more stable synthetic fixtures for every required action/family, sufficient to prove the instrument can represent the programme corpus. Do not run Unit 2's full matrix automatically.

Implement the nine frozen profile IDs with pure deterministic constructors. The minimal and selection-guidance controls both use the production builder; no copied minimal template is introduced. Context-bearing profiles add one frozen synthetic factor at a time, and the mixed profile combines them in production order. A profile hash detects unintended fixture/context mutation between repetitions.

## 7. Classification algorithm

The production parser alone decides representation validity and actual action. After it returns or throws:

- compare typed action to expected action for A/B/D;
- apply deterministic content rules for exact/not-applicable/material mismatch, leaving semantic paraphrase indeterminate for later human review;
- classify parser rejection by raw-text predicates into C/E/F/G while preserving the original exception;
- catch `TimeoutCancellationException` as H;
- classify inference/HTTP/envelope failures as I without treating them as parser failures;
- after trials, compare matched profiles for J and identical repetitions for K without replacing their primary result.

Wrong valid action is always D. An indeterminate fidelity result cannot pass. Classification code cannot return or alter `ReasoningProviderResponse`.

## 8. Artifact implementation

Write UTF-8 JSONL with a small test-only deterministic encoder using fixed field order; no dependency is added. Write a run header before trials and one record per attempted trial. Escape control characters correctly and flush after each trial so partial campaigns retain evidence.

Calculate SHA-256 over the exact UTF-8 prompt. Capture endpoint fields from the raw Ollama envelope using test-only evidence extraction while production extraction remains `defaultOllamaResponseBody`. Unknown token fields are null/unavailable. Store the full prompt only after synthetic validation. Sanitize endpoint identity before serialization. Never serialize environment maps, headers, credentials, stack traces containing URLs, or output-path machine details.

Artifact serialization failure fails the evaluation task; evidence must not be silently lost while a trial is reported as completed.

## 9. Deterministic verification plan

The isolated evaluation task must run the following offline tests using loopback stub HTTP only where transport is required:

1. all configuration absent -> live entry skipped before network/client construction;
2. partial/invalid configuration -> redacted configuration failure;
3. fixture and every context-profile construction/hash is stable;
4. prompt capture equals direct `DefaultReasoningPromptBuilder` output, proving the production builder is used;
5. raw body and extracted field traverse `LocalHttpModelInferenceClient` unchanged through its default functions;
6. typed results come from `TaggedReasoningResponseParser`;
7. `REPLY`/`NOACTION` for a Remember fixture -> D, not representation failure or Remember;
8. `MEMBER:`, prose, multiple tags, and blank/partial output -> their C/E/F/G classes while production parsing rejects;
9. provider timeout -> H and not D/I;
10. refused connection/malformed envelope -> I and not semantic failure;
11. repetition preserves fixture/profile hashes and sequence identity;
12. matched-profile drift and repeatability flags derive without erasing trial outcomes;
13. JSONL bytes and field order are deterministic, escaped, credential-free, and contain required fields;
14. parsed Remember/Goal ends at observation with no runtime/downstream dependency in the harness object graph; and
15. `./gradlew test` remains unchanged, offline, and does not select the live source set/task.

Then run targeted evaluation-instrument tests and the full normal suite. Do not run the configured live campaign merely to complete Unit 1 unless explicitly authorized and synthetic configuration is supplied; a one-fixture smoke call may demonstrate targetability only under separate execution approval and is not Unit 2 evidence.

## 10. Timeout and failure handling

The evaluation timeout is required, positive, and passed only to the constructed `ModelReasoningProvider`. It does not alter `ParkerRuntimeConfig` or its 30-second default. Record requested timeout and elapsed duration. Preserve cancellation; do not retry. H remains separate from I and all semantic classes.

## 11. Privacy and side-effect proof

Fixture constructors accept only repository constants, not file imports, production logs, or arbitrary runtime owner content. The live test does not load Parker production configuration. Reflection/object-graph assertions and source inspection must prove no `ParkerRuntime`, reply, planner, admission, Memory, Knowledge, or persistence dependency exists.

The endpoint receives synthetic prompts, which is still an external disclosure; invocation is therefore explicit and its configured operator is responsible for endpoint trust. No ordinary log receives raw prompt/output evidence.

## 12. Workflow and stop conditions

Before implementation:

1. Scope Lock — accepted;
2. Scope Lock Independent Constitutional Review — accepted;
3. Implementation Plan — accepted;
4. Implementation Plan Independent Constitutional Review — accepted;
5. **Unit 1 Boundary Review — required and accepted**; and
6. explicit approval to implement.

Stop and return to Boundary Review if production modification, a new dependency, a copied prompt/parser, real data, normal-task network dependency, a downstream side effect, a remedy, or an additional file surface appears necessary.

After implementation: targeted verification, full normal suite, Completion Review, and Independent Constitutional Review. Defect Confirmation Review is conditional on a genuine defect.

## 13. Completion and handoff

Unit 1 completes only when every Scope Lock criterion and deterministic verification passes, the explicit task can target configured live infrastructure, missing configuration skips, raw/parsed evidence is reproducible, and no production behaviour or ordinary test boundary changed.

Unit 2 receives the accepted harness, frozen corpus/profile versions, artifact schema, and execution instructions. Unit 2—not Unit 1—runs repeated baseline characterisation or decides model, prompt, representation, retry, timeout, or qualification matters.

## 14. Plan disposition

```text
ACCEPTED
```

No implementation begins through this document alone.
