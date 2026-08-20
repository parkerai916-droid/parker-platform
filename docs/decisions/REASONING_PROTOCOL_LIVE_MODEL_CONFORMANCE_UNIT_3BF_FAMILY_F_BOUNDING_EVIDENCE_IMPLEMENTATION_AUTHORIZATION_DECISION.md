**Status:** Unit 3-BF Family F Bounding Evidence Implementation Authorization Decision — **PROPOSED — PENDING INDEPENDENT CONSTITUTIONAL REVIEW.** This decision was originally drafted against merged baseline `86736dcaf1840d8c1003a6d56e8b0924300d865c` (PR #31) and, never having been accepted, committed, or effective, is corrected in place against current baseline `eec049238cfb5806a8b1d7d3951804713215d101` to align its own Section 10 model-role table and Section 1 controlling-authority citations with the now-accepted Family F Model Role and Research Question Scope Lock and the four accepted downstream model-role amendments it authorized. This is a bounded factual correction, not a new authorization design: it changes no implementation file scope, no numeric bound (none existed to change), no readiness status, and no authorization boundary. If, and only if, this decision and its Independent Constitutional Review are accepted and merged, it authorizes the exact two-file offline implementation defined by the accepted Family F Bounding Evidence Acquisition and Offline Estimator Plan. It does not authorize evidence production, the evidence-production approval environment value, a governed evidence root, network/provider/model contact, numeric-bound selection, host qualification, Explicit Execution Approval, a live Family F diagnostic run, or Knowledge Discoverability Attempt 3. `READINESS=NOT READY` remains unchanged.

# Reasoning Protocol Live-Model Conformance — Unit 3-BF Family F Bounding Evidence Implementation Authorization Decision

## 1. Decision record

```text
DECISION_TYPE=IMPLEMENTATION AUTHORIZATION
BASELINE=86736dcaf1840d8c1003a6d56e8b0924300d865c
BASELINE_DESCRIPTION=Merge pull request #31 from governance/reasoning-protocol-family-f-bounding-evidence-plan
PLAN_COMMIT=5bcbad0ce50e0c1debb91b5baf77f45eff164b2b
PLAN_SHA256=0fec51f957a6a9b458a0d3c6d980cc6982e02ef316e09218d01da999c5beea0e
PLAN_ICR_SHA256=61948c5b8b968a45b70938c9f5a4f46f73cf9337c24a7d273b4405d21f0b7d11
PLAN_VERDICT=ACCEPTED
PLAN_FINDINGS=P0=0,P1=0,P2=0,P3=0
READINESS=NOT READY
```

The Plan and its accepted Independent Constitutional Review are present in the baseline at:

```text
docs/implementation/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_BOUNDING_EVIDENCE_ACQUISITION_AND_OFFLINE_ESTIMATOR_PLAN.md
docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_BOUNDING_EVIDENCE_ACQUISITION_AND_OFFLINE_ESTIMATOR_PLAN_INDEPENDENT_CONSTITUTIONAL_REVIEW.md
```

This decision must be read with the controlling Plan and its accepted Model Role Amendment and Independent Constitutional Review, the accepted Capture-Proxy Response/Request-Size and Dedicated-Runtime-Growth Bounding Scope Lock and its accepted Model Role Amendment and Independent Constitutional Review, the accepted Family F Model Role and Research Question Scope Lock and its accepted Correction Independent Constitutional Review, and `docs/architecture/parker-constitution.md`. Where this decision is silent, the narrower or more protective controlling requirement prevails.

## 2. Question decided

The question is:

> May implementation begin for the exact detached, offline Family F bounding-evidence tooling defined by the accepted Plan, without authorizing evidence production or any live/provider/model activity?

## 3. Decision

```text
DECISION=AUTHORIZE THE EXACT TWO-FILE OFFLINE IMPLEMENTATION AFTER THIS DECISION AND ITS ICR ARE ACCEPTED AND MERGED
EVIDENCE_PRODUCTION_AUTHORIZED=NO
```

The authorization is intentionally narrower than the complete evidence programme. It permits code construction and offline verification only. It does not permit the evidence-producing entry point to pass its approval gate, does not create a governed evidence campaign, and does not establish or select any bound.

## 4. Effective condition

This decision is not effective merely because it has been drafted.

Before acceptance and merge:

```text
IMPLEMENTATION_AUTHORIZED=NO
NEXT_LAWFUL_ACTION=Independent Constitutional Review of this decision
```

Only after all of the following are true does the authorization become effective:

1. this decision receives an accepted Independent Constitutional Review with no P0–P3 finding;
2. this decision and that review are merged into `main`;
3. implementation starts from that merged `main` lineage with a clean working tree; and
4. the implementation remains entirely inside Sections 5–13 of this decision.

No condition may be waived by operator intent, environment configuration, available capacity, or the presence of already-downloaded model artifacts.

## 5. Exact implementation boundary

The authorized source-file boundary is exactly:

```text
build.gradle.kts
tests/integration/ReasoningProtocolFamilyFBoundingEvidenceTest.kt (new)
```

No third source, test, configuration, documentation, script, generated source, fixture, or production file is authorized as part of implementation.

In particular:

- no file under `src/` may change;
- `tests/integration/ReasoningProtocolFamilyFDiagnosticTest.kt` must remain byte-unchanged;
- `tests/integration/ReasoningProtocolFamilyFDiagnosticOrchestrationTest.kt` must remain byte-unchanged;
- `tests/integration/ReasoningProtocolLiveModelEvaluationHarness.kt` must remain byte-unchanged;
- the production prompt builder, request serializer, provider client, parser, and all existing Family F gates must remain byte-unchanged; and
- needing any third file is a stop condition requiring new governance.

Runtime evidence artifacts created only inside test-owned temporary directories are test outputs, not additional implementation files. They may not be committed or represented as governed evidence.

## 6. Authorized `build.gradle.kts` change

`build.gradle.kts` may add exactly one detached `Test` task:

```text
TASK=reasoningProtocolFamilyFBoundingEvidence
SOURCE_SET=liveModelEvaluation
TEST_FILTER=parker.integration.ReasoningProtocolFamilyFBoundingEvidenceTest
SYSTEM_PROPERTY=parker.reasoning.familyf.boundingEvidence.enabled=true
```

The task must:

- follow the accepted detached-task precedent already present in `build.gradle.kts`;
- remain absent from `test`, `check`, `build`, and `assemble` task graphs;
- set only the frozen system property above;
- not set, synthesize, default, or inherit an approval environment value;
- not set a live Family F execution property or approval;
- not declare an ordinary lifecycle dependency that can reach it; and
- contain no provider, model, network, Docker, Proxmox, process, filesystem-cleanup, or infrastructure operation.

No plugin, dependency, repository, source set, production task, or existing detached task may be added or changed under this authorization.

## 7. Authorized new test-file content

`tests/integration/ReasoningProtocolFamilyFBoundingEvidenceTest.kt` may contain only:

1. the frozen double-gated evidence-producing entry test;
2. the offline request estimator defined by Plan WP-A;
3. the evidence schemas and deterministic serializers defined by the Plan;
4. offline validators for pre-supplied local response, header, and runtime primary evidence;
5. negative-evidence and evidence-gap handling;
6. the progress ledger, manifest, copied-directory verification, terminal-marker, recovery, and failure behavior defined by the Plan;
7. source-inspection and task-isolation meta-tests;
8. fake-driven and temporary-directory tests; and
9. minimal helpers used only by those items.

The file may invoke the accepted schedule, prompt builder, request serializer, and immutable repository inputs. It may not duplicate or fork their logic.

It may not contain:

- a socket, HTTP, URL-fetch, browser, provider, model, Docker, Proxmox, shell, subprocess, or Ollama client;
- a daemon launch, stop, restart, signal, unload, or residency operation;
- model acquisition, extraction, copying, loading, generation, or deletion;
- production-process, production-endpoint, production-volume, or model-store mutation;
- a hard-coded numeric request, response, header, `E`, or `R` bound represented as accepted;
- ranking, winner, recommendation, model-selection, or qualification logic;
- a Knowledge Discoverability entry point; or
- any path that treats `PARKER_REASONING_FAMILY_F_EXECUTION_APPROVED` as relevant authority.

## 8. Frozen execution gates

The evidence-producing entry test must check, in this order and before resolving configuration or touching the filesystem:

```text
1. System.getProperty("parker.reasoning.familyf.boundingEvidence.enabled") == "true"
2. System.getenv("PARKER_REASONING_FAMILY_F_BOUNDING_EVIDENCE_APPROVED") == "true"
```

If either condition is absent or false, the entry test must self-skip before output-root resolution, writer construction, schedule iteration, or artifact creation.

The entry point must also refuse to run when:

```text
PARKER_REASONING_FAMILY_F_EXECUTION_APPROVED
```

is present, regardless of its value. That variable belongs to a different live-execution authority and can never strengthen, supplement, or substitute for bounding-evidence authority.

A source-inspection meta-test must prove:

- exactly the two positive gates above are required;
- the live-execution approval variable causes refusal;
- the real evidence entry point is invoked exactly once after the gates;
- output configuration is not resolved before the gates; and
- no forbidden network/process API is reachable from the new file.

## 9. What offline verification may execute

This authorization permits compilation and offline verification of the two-file implementation.

Offline tests may:

- invoke estimator and validator functions directly using repository inputs, fakes, immutable test data already present in the repository, and test-owned temporary directories;
- exercise the complete 392-entry schedule in memory or in test-owned temporary output;
- serialize, decode, hash, copy, corrupt, resume, fail, and verify synthetic/test evidence packages;
- invoke the detached task with the evidence-production approval environment value absent solely to confirm that the entry test skips before configuration or filesystem access;
- inspect Gradle task graphs; and
- run the full ordinary Gradle suite.

Any output from these tests is non-authoritative test output. It must be isolated to test-owned temporary directories, automatically disposable by the test framework, and never copied into a proposed governed evidence root or cited as programme evidence.

Offline verification may not:

- set `PARKER_REASONING_FAMILY_F_BOUNDING_EVIDENCE_APPROVED=true`;
- supply a real `PARKER_REASONING_FAMILY_F_BOUNDING_EVIDENCE_ROOT` for evidence production;
- run a successful evidence-producing entry-point path;
- retrieve or ingest newly collected external evidence;
- contact a provider, model, network service, container API, or hypervisor;
- use the Ollama CLI or API;
- create a governed evidence campaign directory; or
- create a result eligible for Evidence Completion Review or Bound Selection.

## 10. Frozen estimator fidelity

The implementation must derive work only from the accepted `FamilyFCampaignDefinition.allTrials` schedule and must preserve:

```text
SUBJECT_MODEL=llama3.2:3b
CONTROL_MODEL=qwen2.5-coder:7b
FIXTURE_COUNT=23
CONTEXT_PROFILE_COUNT=2
SCORED_REPETITIONS_PER_CELL_PER_ROLE=4
SCORED_CALLS=368
WARMUP_CALLS=24
TOTAL_CALLS=392
FROZEN_SCHEDULE=accepted deterministic AB/BA schedule
PRODUCTION_FORMATTER=DefaultReasoningPromptBuilder
PRODUCTION_REQUEST_SERIALIZER=defaultOllamaRequestBody
```

`SUBJECT_MODEL` denotes the model under diagnostic evaluation; `CONTROL_MODEL` denotes `qwen2.5-coder:7b`, Parker's actual, continuously deployed reasoning-protocol model. Neither role implies preference, approval, selection, or qualification of either model, and this table confers no authority to acquire, load, unload, replace, or requantize any model.

For each scheduled call, the estimator must use the accepted construction:

1. resolve a null warm-up profile to `ContextProfileId.MINIMAL_PRODUCTION_CONTEXT`;
2. call `SyntheticContextProfiles.construct(trial.fixture, profileId)`;
3. obtain the turn with `(input.request.subject as ReasoningSubject.OfTurn).turn`;
4. call `DefaultReasoningPromptBuilder().buildPrompt(turn, input.request.reasoningContext)`;
5. call `defaultOllamaRequestBody(prompt, modelName)`; and
6. encode the returned body as UTF-8.

No handwritten fixture list, second schedule, duplicated formatter, duplicated serializer, shortened run, substituted model name, or reduced corpus is authorized.

## 11. Frozen artifacts, integrity, and recovery

Implementation must support exactly the mandatory artifact set in Plan Section 9, including the mandatory in-directory `progress-ledger.jsonl`.

The implementation must preserve the corrected ordering accepted by the Plan and its ICR:

1. `WP_E_VALIDATION` is the final progress-ledger step;
2. its durable completion record is appended;
3. the progress ledger is finalized and every later append is prohibited;
4. `SHA256SUMS.txt` is then written and covers the complete immutable ledger with every other manifest-covered mandatory artifact;
5. copy verification is read-only and safely repeatable;
6. copy verification never modifies the ledger or another manifest-covered artifact; and
7. terminal state is represented only by the mutually exclusive `evidence.complete` or `evidence.failed` marker.

An attempted post-finalization append, malformed record, duplicate completion, unknown file, hash mismatch, final/temporary conflict, or copied-directory verification failure must fail closed.

No implementation shortcut may exclude the ledger from manifest coverage, add an undisclosed evidence-bearing file, or represent a partial directory as complete.

## 12. Required offline tests

Before the implementation can be submitted for Completion Review, offline tests must prove at minimum:

- the authorized two-file boundary and absence of changes under `src/`;
- task detachment from `test`, `check`, `build`, and `assemble`;
- the exact task filter and system property;
- self-skip before configuration/filesystem access when either positive gate is absent;
- refusal when the live execution-approval variable is present;
- exactly-once entry-point invocation after gates;
- absence of network/process/provider/model code paths;
- exact 368 scored + 24 warm-up = 392 schedule coverage;
- exact trial-ID equality, no missing/duplicate ID, and both roles/profiles/models represented;
- accepted prompt and request-serialization fidelity;
- deterministic byte-identical output across two clean test runs;
- Base64, UTF-8 byte-count, SHA-256, JSON/JSONL, Unicode, and maximum-tie correctness;
- overflow rejection;
- primary-evidence schema and applicability validation using local test fixtures only;
- honest unresolved and negative-evidence handling;
- progress-ledger exact-once and recovery behavior;
- ledger finalization before manifest creation;
- rejection of every post-finalization append;
- manifest completeness and tamper detection;
- fresh copied-directory verification;
- terminal-marker exclusivity; and
- no accepted-bound, readiness, selection, ranking, or execution-authority output.

The full ordinary Gradle suite must pass. No live model task and no successful governed evidence-production path is part of implementation verification.

## 13. Stop conditions

Implementation work must stop without expanding scope if any of the following becomes necessary or occurs:

- a third implementation file is needed;
- any existing accepted Family F, harness, production, or `src/` file must change;
- the accepted schedule, formatter, or request serializer cannot be invoked directly;
- an external dependency, plugin, network source, subprocess, provider tool, or model is needed;
- implementation requires setting the evidence-production approval environment value;
- implementation requires a real governed evidence root;
- a numeric bound must be chosen to make a test pass;
- primary evidence must be retrieved, generated, or refreshed;
- a production process, endpoint, volume, model store, VM, or container must be touched;
- ordinary lifecycle isolation cannot be proven;
- ledger/manifest/copy/terminal ordering cannot be implemented without contradiction;
- test output would be represented as governed evidence;
- any frozen invariant must be weakened; or
- Knowledge Discoverability Attempt 3 would become reachable.

When a stop condition occurs, the authorized response is to preserve the working state, report the blocker, and seek new governance. It is not to improvise a workaround.

## 14. Implementation completion package

The future implementation submission must contain exactly:

```text
MODIFIED=build.gradle.kts
NEW=tests/integration/ReasoningProtocolFamilyFBoundingEvidenceTest.kt
```

Its handoff must report:

```text
BASE_COMMIT
BRANCH
FILES_CHANGED
THIRD_FILE_OR_SRC_DRIFT=NONE
TASK_NAME
TASK_DETACHMENT
GATE_TESTS
SCHEDULE_TESTS
SERIALIZATION_FIDELITY_TESTS
DETERMINISM_TESTS
EVIDENCE_SCHEMA_TESTS
NEGATIVE_EVIDENCE_TESTS
LEDGER_RECOVERY_TESTS
MANIFEST_AND_COPY_TESTS
FOCUSED_OFFLINE_TEST_RESULT
FULL_GRADLE_TEST_RESULT
EVIDENCE_PRODUCTION=NOT PERFORMED
MODEL_OR_PROVIDER_CONTACT=NONE
READINESS=NOT READY
DIFF_CHECK
GIT_STATUS
```

No implementation claim is accepted merely because tests pass. The exact diff and every claimed property remain subject to independent review.

## 15. Required post-implementation gates

After implementation, and before any evidence production, the following must occur in order:

1. a Family F Bounding Evidence Implementation Completion Review;
2. an Independent Constitutional Review of that completion review and implementation;
3. acceptance and merge of the reviewed implementation and both reviews; and
4. a separate Family F Bounding Evidence Production Authorization Decision.

The Production Authorization Decision must separately fix every permitted collection mechanism, evidence campaign identity, output root, reviewed implementation commit, protected input hashes, allowed pre-supplied sources, and any allowed network boundary. This implementation decision supplies none of those values and grants none of that authority.

## 16. Authority matrix

| Action | Authorized after this decision and its ICR are accepted and merged? |
|---|---|
| Modify `build.gradle.kts` exactly as Section 6 permits | YES |
| Create the single new test file in Section 5 | YES |
| Compile the implementation | YES |
| Run fake-driven/temp-directory offline tests | YES |
| Run ordinary Gradle tests | YES |
| Invoke the detached task with approval absent to prove self-skip | YES |
| Set `PARKER_REASONING_FAMILY_F_BOUNDING_EVIDENCE_APPROVED=true` | NO |
| Produce a governed 392-record evidence package | NO |
| Create or select a real bounding-evidence root | NO |
| Retrieve official web documentation | NO |
| Contact a provider or model endpoint | NO |
| Use Ollama CLI/API | NO |
| Launch, stop, restart, or observe a dedicated provider | NO |
| Acquire, load, unload, replace, or requantize a model | NO |
| Select a request, response, header, `E`, or `R` bound | NO |
| Provision or qualify a host/VM | NO |
| Manipulate production Parker/Ollama | NO |
| Issue Explicit Execution Approval | NO |
| Run the live Family F diagnostic | NO |
| Begin Knowledge Discoverability Attempt 3 | NO |

## 17. `NO_MANUFACTURED_PASS`

Implementation and verification remain subject to the accepted Alternative Diagnostic Host Requirements Scope Lock Section 9.5, Bounding Scope Lock Section 11.1, and Plan Section 27.

No pass may be manufactured by:

- deleting or cleaning unrelated data;
- pointing a check or test at a filesystem the real future campaign would not use and representing that as campaign evidence;
- truncating artifacts, dropping ledgers, weakening hashes, or reducing the 392-call schedule;
- substituting a smaller, different, or requantized model/workload identity;
- hard-coding a convenient numeric bound;
- treating test output as primary evidence; or
- treating available disk space as a substitute for computed `E+R` and governed reserves.

## 18. Explicit non-claims

This decision does not claim that:

- implementation exists or has begun;
- the estimator compiles or passes tests;
- the detached task is registered;
- any estimator or evidence-producing entry point has run;
- any evidence root or evidence package exists;
- 392 governed request records have been produced;
- primary response, header, or runtime evidence has been acquired;
- a request, response, header, `E`, or `R` bound is resolved or selected;
- Parker VM 102 or another host is ready;
- the production-workload coexistence gap is resolved;
- evidence production is authorized;
- provider/model contact is authorized;
- readiness has improved; or
- live Family F execution or Knowledge Discoverability Attempt 3 is lawful.

## 19. Constitutional conformance

This decision preserves the separation of proposal, authorization, and execution. It gives implementation only the minimum capability required to construct and test an offline tool, with no authority to produce governed evidence or contact external capability. The exact file boundary limits blast radius; the double gate preserves trust control; mandatory provenance, recovery, and review preserve auditability; stop conditions preserve revocability; and explicit post-implementation gates prevent implementation from self-authorizing collection, selection, readiness, or execution.

## 20. Governance sequence and next lawful action

The governing sequence is:

1. this Implementation Authorization Decision;
2. Independent Constitutional Review;
3. acceptance and merge;
4. exact two-file implementation and offline verification;
5. Implementation Completion Review;
6. Independent Constitutional Review;
7. acceptance and merge;
8. separate Evidence Production Authorization Decision;
9. separately authorized evidence production;
10. Evidence Completion Review and Independent Constitutional Review;
11. Bound Selection Decision;
12. bounded-transport/runtime implementation plan;
13. bounded-transport/runtime implementation and offline verification;
14. bounded-transport/runtime implementation Completion Review and Independent Constitutional Review;
15. renewed candidate-host assessment and Readiness Review;
16. Explicit Execution Approval; and only then
17. live Family F diagnostic execution.

No step authorizes its successor.

Before this decision and its ICR are accepted and merged:

```text
NEXT_LAWFUL_ACTION=Independent Constitutional Review of this decision
```

After acceptance and merge:

```text
NEXT_LAWFUL_ACTION=Exact two-file Family F Bounding Evidence implementation and offline verification
```

## 21. Final authority statement

```text
PROGRAMME_STATUS=ACTIVE
FAMILY_F_STATUS=INCLUDED FOR PRE-QUALIFICATION DIAGNOSTIC SCOPING ONLY (unchanged)
EXISTING_DIAGNOSTIC_IMPLEMENTATION_STATUS=ACCEPTED (unchanged)
BOUNDING_EVIDENCE_PLAN_STATUS=ACCEPTED
BOUNDING_EVIDENCE_IMPLEMENTATION_AUTHORIZATION=PROPOSED; NOT EFFECTIVE BEFORE ACCEPTED ICR AND MERGE
AUTHORIZED_IMPLEMENTATION_FILES=build.gradle.kts; tests/integration/ReasoningProtocolFamilyFBoundingEvidenceTest.kt
BOUNDING_EVIDENCE_IMPLEMENTATION_STATUS=NOT STARTED
BOUNDING_EVIDENCE_PRODUCTION_STATUS=NOT AUTHORIZED
BOUNDING_EVIDENCE_APPROVAL_ENV_AUTHORIZED=NO
GOVERNED_EVIDENCE_ROOT_AUTHORIZED=NO
READINESS=NOT READY
MAX_REQUEST_BOUND_STATUS=NOT COMPUTED OR SELECTED
MAX_RESPONSE_BOUND_STATUS=UNCOMPUTABLE
HEADER_BOUNDS_STATUS=UNRESOLVED
E_STATUS=UNCOMPUTABLE
R_STATUS=UNCOMPUTABLE
NUMERIC_BOUND_SELECTED=NONE
PROVIDER_OR_MODEL_CONTACT_AUTHORIZED=NO
DEDICATED_PROVIDER_LAUNCH_AUTHORIZED=NO
R_OBSERVATION_AUTHORIZED=NO
HOST_OR_VM_PROVISIONING_AUTHORIZED=NO
MODEL_ACQUISITION_AUTHORIZED=NO
CAMPAIGN_AUTHORIZED=NO
EXPLICIT_EXECUTION_APPROVAL_AUTHORIZED=NO
KNOWLEDGE_DISCOVERABILITY_ATTEMPT_3_AUTHORIZED=NO
NEXT_LAWFUL_ACTION=Independent Constitutional Review; after acceptance and merge, exact two-file implementation and offline verification
```
