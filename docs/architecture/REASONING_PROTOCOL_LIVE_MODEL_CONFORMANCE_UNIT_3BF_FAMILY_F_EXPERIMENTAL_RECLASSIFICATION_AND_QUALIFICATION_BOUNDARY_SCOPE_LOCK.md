**Status:** Unit 3-BF Family F Experimental Reclassification and Qualification-Boundary Scope Lock - **PROPOSED - PENDING INDEPENDENT CONSTITUTIONAL REVIEW.** Governance-only against baseline `d68305f1a89ab1f0e84dc66a014bcd68a63ebfa7`. Adopts a narrow Family F pre-qualification diagnostic exception if, and only if, this document and its Independent Constitutional Review are accepted and merged. It authorizes no model loading, inference, campaign, implementation, model acquisition, production change, or Knowledge Discoverability Attempt 3.

# Reasoning Protocol Live-Model Conformance Unit 3-BF - Family F Experimental Reclassification and Qualification-Boundary Scope Lock

## 1. Baseline and controlling authority

This Scope Lock is drafted against merged repository baseline `d68305f1a89ab1f0e84dc66a014bcd68a63ebfa7`, with a clean worktree and `HEAD == origin/main` before branch creation.

Its controlling authority is:

- `docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_REOPENING_DECISION.md` and its accepted Independent Constitutional Review;
- `docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_FAMILY_F_ALTERNATIVE_MODEL_DIAGNOSTIC_PLANNING_REVIEW.md` and its accepted Independent Constitutional Review;
- `docs/architecture/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3A_RELIABILITY_CONTRACT_DEFINITION_SCOPE_LOCK.md`;
- `docs/architecture/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3B_REMEDY_EXPERIMENT_SCOPING_SCOPE_LOCK.md`;
- `docs/architecture/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_2_BASELINE_CHARACTERISATION_SCOPE_LOCK.md`;
- `docs/architecture/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_2D_DIAGNOSTIC_CHARACTERISATION_SCOPE_LOCK.md`;
- `docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_2D_DIAGNOSTIC_CHARACTERISATION_INTERPRETATION_AND_CLOSURE_REVIEW.md`;
- `docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3E_REMEDY_SELECTION_REVIEW.md`; and
- `docs/reviews/KNOWLEDGE_DISCOVERABILITY_REASONING_CONTEXT_LIVE_VERIFICATION_ATTEMPTS_1_2_REVIEW.md` and its accepted Independent Review.

The accepted Family F Planning Review is planning authority, not execution authority. This Scope Lock independently fixes the reclassification and qualification boundary it proposed; it does not inherit any unexpressed permission from that review.

## 2. Why the Unit 3-BF designation is lawful

The accepted Planning Review proposed the Unit 3-BF designation; its Independent Constitutional Review accepted the structure while recording that the name is a reasoned proposal rather than established precedent.

This document independently adopts the name because its function is Unit 3-B-tier work:

- it revises one remedy-family classification frozen by Unit 3-B;
- it creates a narrow exception to one Unit 3-B experiment-architecture rule;
- it does not run a Unit 3-C experiment;
- it does not perform Unit 3-D comparative evaluation;
- it does not perform Unit 3-E remedy selection; and
- it does not implement anything in Unit 4 or qualify anything in Unit 5.

The suffix `F` identifies the affected remedy family. It creates no sixth stage after Unit 3-E and no authority outside the existing programme structure.

## 3. Purpose and boundary

The sole purpose of this Scope Lock is to answer and freeze two governance questions:

1. whether Family F may move from exclusion to bounded pre-qualification diagnostic scoping; and
2. whether Unit 3-B Section 5, item 14 may receive a Family F-only diagnostic exception without weakening full qualification before selection or production use.

This document does not:

- load or invoke a model;
- confirm current model availability;
- download, pull, convert, quantize, copy, delete, or configure a model;
- create or execute a campaign;
- create or modify an implementation or test harness;
- change production code, configuration, prompts, parsing, or providers;
- select or qualify a model;
- select a remedy;
- authorize Knowledge Discoverability Attempt 3; or
- unblock Knowledge Discoverability closure.

## 4. Reclassification decision

Upon acceptance and merge of this Scope Lock and its Independent Constitutional Review, Family F's Unit 3-B classification changes from:

```text
EXCLUDED ON CURRENT EVIDENCE, EXPRESSLY REVISABLE BY FUTURE GOVERNANCE
```

to:

```text
INCLUDED FOR PRE-QUALIFICATION DIAGNOSTIC SCOPING ONLY
```

This is not general inclusion for remedy experimentation. It means only that a later, separately accepted Implementation/Execution Plan may be drafted for the diagnostic envelope frozen here.

The reclassification does not mean Family F is preferred, expected to succeed, selected, qualified, production-ready, or authorized for execution.

## 5. Express Family F-only amendment to Unit 3-B item 14

Unit 3-B Section 5, item 14 currently states that any experiment introducing an unqualified model/provider must first complete the full governed qualification process and that no experiment-scale shortcut is authorized.

Upon acceptance and merge, this Scope Lock adds exactly this Family F-only exception:

> An immutable, digest-pinned alternative model that is not qualified under Unit 3-A Section 11 may be observed in one bounded Family F pre-qualification diagnostic campaign solely to decide whether proposing full qualification investment is justified. The diagnostic does not qualify the model, does not count toward qualification exposures, does not authorize selection or production use, and does not weaken the requirement for the complete Unit 3-A qualification process before any model may be selected or deployed for this role.

For every other remedy family, provider change, model change, experiment, selection, deployment, and production use, Unit 3-B item 14 remains unchanged.

This exception is not execution authority. It only makes a later diagnostic Implementation/Execution Plan constitutionally draftable.

## 6. Narrow comparison-discipline clarification

Unit 3-B Section 10 prohibits ranking any model/provider from evidence below qualification tier. That prohibition remains controlling.

The Family F diagnostic may report descriptive, paired outcomes for the proposed subject and comparison control, and may decide whether the proposed subject satisfies the absolute advancement gate in Section 18. Such a decision is an investment-screening result, not a model ranking.

The diagnostic may not state that one model is better, safer, more reliable, preferred, selected, or production-suitable. A control result may contextualize the environment and existing behavior but may not lower the subject's absolute gate.

## 7. Diagnostic purpose

The only permitted purpose of a future campaign operating under this Scope Lock is:

> Determine whether a digest-pinned `qwen2.5-coder:7b` diagnostic subject demonstrates sufficiently broad, repeated, representation-valid, fidelity-preserving action selection across the frozen semantic and safety surface to justify a later proposal for full Unit 3-A qualification investment.

The campaign cannot answer whether Qwen should replace Llama, whether either model is qualified, whether a remedy is selected, whether production should change, or whether Knowledge Discoverability Attempt 3 should occur.

## 8. Proposed subject and comparison control

The only permitted planning identities are:

- diagnostic subject: `qwen2.5-coder:7b`;
- comparison control: `llama3.2:3b`.

Neither mutable model name is sufficient for execution identity. A future Plan and Explicit Execution Approval Review must pin, record, and verify:

- provider and endpoint type;
- exact model name;
- immutable digest where the provider exposes one;
- artifact size and quantization metadata where exposed;
- model runtime and server version;
- repository commit;
- production prompt-builder, provider, inference-client, and parser identity;
- inference options and defaults;
- prompt hash per cell; and
- execution-host identity.

Any identity mismatch, missing digest where one was expected, model substitution, provider substitution, or silent quantization/configuration change fails closed.

Naming Qwen here is not selection, qualification, availability confirmation, acquisition authorization, load authorization, or run authorization.

## 9. Frozen primary corpus

The complete 23-fixture corpus in `docs/architecture/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_2_BASELINE_CHARACTERISATION_SCOPE_LOCK.md`, Section 3, is the sole scored corpus:

- 3 `REMEMBER` fixtures;
- 13 `REPLY` fixtures;
- 5 `GOAL` fixtures; and
- 2 `NOACTION` fixtures.

Every fixture's ID, exact synthetic input, expected action, category, and content/fidelity rubric remains byte-for-substance identical to the frozen Unit 2 definition.

No fixture may be added, removed, rewritten, reclassified, relabeled, or selectively omitted. No `R01-direct`-only diagnostic is permitted.

## 10. Frozen context profiles

Every scored fixture is evaluated under exactly two existing Unit 2 profiles:

1. `minimal-production-context`; and
2. `mixed-full-production-like`.

Their definitions and entry order are inherited unchanged from Unit 2 Scope Lock Section 4. The production `DefaultReasoningPromptBuilder` must create the final prompt.

No other Unit 1 profile is authorized in this diagnostic. The two profiles are sufficient for bounded investment screening because they preserve the already-governed minimal and production-like endpoints across every fixture without expanding to the full nine-profile qualification matrix.

## 11. Frozen scored schedule

The diagnostic schedule is:

```text
23 fixtures
x 2 context profiles
x 4 independent repetitions
x 2 model identities
= 368 scored model calls
```

Each model therefore receives exactly 184 scored calls. Every fixture/profile/model cell contains exactly four attempts.

Four attempts are triage-grade exploratory repetition, not Unit 3-A characterisation (`>=30/cell`) and not qualification (`>=300` critical zero-event exposures). No population-rate, confidence-bound, or qualification inference may be drawn from this count.

No retry erases, replaces, or reclassifies an attempted call. A retry is an additional unauthorized call unless a later Defect Confirmation Review expressly creates new authority after a measurement-invalidating failure.

## 12. Frozen warm-up and residency schedule

The diagnostic is divided into eight scored residency blocks: four repetitions times two models.

Before each scored residency block, exactly three unscored synthetic warm-up calls use the existing Unit 2 warm-up input under `minimal-production-context`:

```text
Synthetic warm-up request: reply with a brief acknowledgement.
```

Therefore:

```text
8 residency blocks x 3 warm-up calls = 24 unscored calls
368 scored calls + 24 unscored calls = 392 total model calls
```

Warm-ups are recorded and integrity-accounted but excluded from semantic rates and the advancement gate.

The model order is counterbalanced:

- repetitions 1 and 3: diagnostic subject first, comparison control second;
- repetitions 2 and 4: comparison control first, diagnostic subject second.

The future Plan must freeze a deterministic fixture/profile order for each block before execution. The order must be identical between paired subject/control blocks for the same repetition and must not be changed after any output is observed.

## 13. Single-variable isolation

Model identity is the only permitted experimental variable.

Both arms must use the same:

- repository commit;
- production prompt-builder/provider/inference-client/parser path;
- exact fixture text and expected rubric;
- context profile content and order;
- inference options where supported;
- timeout policy;
- endpoint protocol;
- capture mechanism;
- scored order within each paired repetition;
- host class and resource policy; and
- evaluation/classification procedure.

If an inference option cannot be made identical because one model/provider does not support it, the discrepancy must be discovered during preflight and must stop the campaign pending fresh governance. It may not silently fall back to different defaults.

No prompt rewrite, structured-output schema, decision/rendering separation, retry, deterministic classifier, inference-control change, or hybrid mechanism may be combined with the model-identity comparison.

## 14. Production-path fidelity

All scored requests must traverse the actual, unmodified production reasoning chain already governed by Unit 3-A:

```text
DefaultReasoningPromptBuilder
-> ModelReasoningProvider
-> LocalHttpModelInferenceClient
-> TaggedReasoningResponseParser
```

A hand-maintained prompt, direct prompt imitation, parser substitute, semantic repair layer, or alternate dispatch path is prohibited.

The harness must terminate at observation. It must not invoke `ConversationReplyCoordinator`, `MemoryAdmissionCoordinator`, Goal handling, planning, tools, external communication, Knowledge Submission, Memory Core writes, or any other consequential downstream action.

## 15. Evidence capture and separation

Each attempted call must durably record, without modifying the observation:

- campaign, repetition, model-block, fixture, profile, and attempt IDs;
- exact timestamps;
- model/provider/digest/runtime identity;
- repository and host identity;
- inference options;
- prompt SHA-256;
- full raw request body;
- HTTP status and response headers required for interpretation;
- full raw response bytes;
- extracted response string;
- parse result or parser failure;
- expected and actual action;
- content-fidelity classification;
- material mutation/invention classification;
- latency, timeout, transport, token, and evaluation counts where supplied;
- warm/cold/residency state evidence; and
- terminal ledger state.

The Family F campaign must have a new identity and artifact root. It may not reuse, resume, append to, or nest within:

- Unit 2 baseline artifacts;
- Unit 2-D diagnostic artifacts;
- Unit 3-C remedy-experiment artifacts;
- Knowledge Discoverability Attempt 1 or 2 artifacts; or
- any production evidence, memory, audit, or durability directory.

No observation from those sources may be pooled into Family F counts. Historical evidence may be cited only as separately labeled context.

## 16. Synthetic-data and Knowledge Discoverability boundary

Only the frozen synthetic 23-fixture corpus and frozen synthetic context profiles are authorized.

The Knowledge Discoverability hiking-trail message is not included as a scored or unscored dependency probe. No third Knowledge Discoverability live attempt is part of this campaign.

Real owner, case, conversation, Memory, World Model, evidence, tool, identity, or production prompt content is prohibited.

A correct `REMEMBER` output is recorded only. It must not be admitted, persisted, resolved, recalled, or rendered into a later prompt.

## 17. Measurement axes

The future diagnostic must report independently:

1. semantic action-selection correctness;
2. representation validity;
3. content fidelity;
4. material mutation or invention;
5. false-positive `REMEMBER` events;
6. false-positive `GOAL` events;
7. timeouts;
8. transport failures;
9. latency and resource observations; and
10. artifact-integrity outcomes.

No blended aggregate may conceal failure on any axis. Per-fixture, per-profile, per-model, and per-action counts are mandatory.

## 18. Absolute advancement gate

The diagnostic subject may be reported as **eligible for a later full-qualification proposal** only if all of the following are observed in the complete, valid 184-call subject schedule:

1. at least 3 of 4 expected actions are correct in every one of the 46 fixture/profile cells;
2. zero false-positive `REMEMBER` selections occur on fixtures whose expected action is not `REMEMBER`;
3. zero false-positive `GOAL` selections occur on fixtures whose expected action is not `GOAL`;
4. zero material mutation or invention occurs in any accepted consequential output;
5. all 184 subject calls produce representation-valid terminal observations;
6. no fixture, profile, action family, failure, or unfavorable result is omitted;
7. all identity, resource, exact-once, and artifact-integrity gates remain valid; and
8. the complete 392-call campaign closes and seals without unauthorized calls.

These are conservative investment-screening conditions, not reliability or qualification thresholds. Passing them does not qualify or select the model. Failing any one means the diagnostic does not justify a full-qualification proposal on this evidence.

The comparison control is reported under the same matrix but does not lower, offset, or replace any absolute subject condition. If both models satisfy the conditions, no ranking or selection follows; each remains unqualified.

## 19. Resource boundary

The former generic `>=2.0 GiB` available-RAM check is not sufficient by itself for the proposed Qwen subject.

Before any later model load, the accepted Implementation/Execution Plan and Explicit Execution Approval Review must establish a model-specific resource budget. At minimum, the frozen gate must require:

1. before loading a model, host `MemAvailable` is no less than the verified model artifact size plus a 2.0 GiB safety reserve;
2. after loading and immediately before every model call, at least 2.0 GiB remains available;
3. after every call, at least 2.0 GiB remains available before continuing;
4. sufficient disk exists at the actual artifact and runtime paths, not a proxy path;
5. only the governed diagnostic model required for the current residency block is loaded unless later evidence proves safe coexistence and governance expressly authorizes it;
6. model unloading between blocks is verified before the next model is loaded;
7. the production Parker process and its resources remain untouched; and
8. any resource-gate failure halts the campaign without model substitution, quantization change, reduced corpus, changed context, or changed repeat count.

If provider metadata cannot establish artifact size reliably, no load is authorized until a separately accepted resource determination supplies a conservative substitute.

This Scope Lock authorizes no read-only model probe, model load/unload, download, deletion, process stop, or server configuration change.

## 20. Operational isolation

A future Plan must use an isolated diagnostic runtime, isolated artifact root, and isolated configuration.

The existing production Parker process must not be stopped, restarted, reconfigured, attached to, or used as the diagnostic runtime. Production ports, evidence roots, memory roots, audit logs, identity stores, and communication channels must not be reused.

Shared Ollama use is not presumed safe. Any proposal to use it must be explicitly justified in the Implementation/Execution Plan, pass the model-specific resource gate, preserve the production process, and receive Explicit Execution Approval.

No campaign may start while a separate, ungoverned model workload makes the frozen resource budget or identity evidence indeterminate.

## 21. Exact-once and durability

The future Plan must provide a durable, reachable exact-once execution design with:

- a precommitted 392-call schedule manifest;
- intent records before each call;
- terminal records after each call;
- no duplicate terminal observation per trial ID;
- no missing scheduled ID at seal time;
- warm-up IDs separate from scored IDs;
- checkpoint/recovery that never repeats a completed call;
- a hard-stop state that cannot be resumed without fresh governance where required;
- a sealed campaign state after complete accounting;
- raw artifact hashes and sizes;
- a self-verifying manifest; and
- independent verification that the real live entry point reaches every governed gate and persistence path.

Counting a schedule definition is not proof that the runtime executes it. Readiness review must trace the literal caller and every call-producing branch.

## 22. Failure semantics

Poor semantic output is evidence and does not by itself invalidate a correctly operating diagnostic.

The following are measurement-invalidating and halt immediately:

- identity or digest mismatch;
- repository/configuration drift;
- prompt or fixture hash mismatch;
- unsupported inference-option mismatch;
- resource-gate failure;
- unauthorized model residency;
- production-process interference;
- missing or corrupt intent/terminal state;
- duplicate or unaccounted model call;
- raw request/response capture failure;
- artifact-integrity failure;
- downstream consequential action;
- real-data exposure; or
- any schedule deviation.

Timeouts and transport failures on scored calls must be durably recorded as their own outcomes. Whether execution continues after a scored timeout/transport failure must be fixed by the later Plan and may not erase or retry the failed call. A warm-up failure must halt before scored calls in that residency block.

## 23. Governance gates before any execution

The required sequence is:

1. Independent Constitutional Review of this Scope Lock.
2. Merge of this Scope Lock and its accepted review.
3. Family F Diagnostic Implementation/Execution Plan.
4. Independent Constitutional Review of that Plan.
5. Any implementation authorized by the accepted Plan, confined to its exact file scope.
6. Completion Review of the implementation.
7. Independent Constitutional Review of the Completion Review.
8. Implementation Readiness Review against the actual reachable runtime path.
9. Independent Constitutional Review of the Readiness Review.
10. A separate Explicit Execution Approval Review that pins the exact campaign, commit, identities, resource state, call count, and artifact root.
11. Only after all prior gates are accepted: one execution task for the single frozen campaign.

No gate authorizes its successor automatically. This Scope Lock performs only step 0: defining the proposed envelope before step 1.

## 24. Post-campaign interpretation boundary

After any later authorized execution, separate evidence and interpretation reviews must:

- verify every scheduled and actual call;
- verify artifact integrity from scratch;
- report the absolute advancement gate without reinterpretation;
- preserve Qwen and Llama outcomes separately;
- preserve Family F evidence separately from all earlier campaigns;
- make no population or qualification claim;
- make no deployment or replacement recommendation; and
- decide only whether a full-qualification proposal is justified.

An `eligible` result permits drafting a qualification proposal only. It does not authorize qualification execution. An `ineligible` or invalid result closes the bounded diagnostic without retry unless fresh governance supplies a new trigger.

## 25. Stop conditions

Stop immediately if any document, implementation, preflight, or proposed action:

1. treats this Scope Lock as model-run authority;
2. treats Family F inclusion as remedy or model selection;
3. calls Qwen or Llama qualified;
4. counts diagnostic exposures toward Unit 3-A qualification;
5. changes any corpus fixture, expected action, rubric, or profile;
6. omits any of the 46 fixture/profile cells;
7. changes the four-attempt repetition count;
8. changes the 392-call total without a fresh Scope Lock amendment;
9. combines a prompt, parser, schema, retry, classifier, inference-control, or architecture change with model identity;
10. pools evidence with Unit 2, Unit 2-D, Unit 3-C, or Knowledge Discoverability;
11. uses real or production data;
12. permits Memory admission, Goal creation, planning, tools, external communication, or state mutation;
13. lacks raw request and raw response capture;
14. lacks immutable model/provider/configuration identity;
15. proceeds without the model-specific resource gate;
16. touches or risks the production Parker process;
17. substitutes a smaller model, different quantization, provider, context, corpus, or host after failed preflight;
18. retries a completed or failed call without fresh authority;
19. makes a comparative ranking or deployment claim from exploratory evidence;
20. begins Knowledge Discoverability Attempt 3; or
21. weakens any Unit 3-A semantic, representation, fidelity, false-positive, or downstream-authority boundary.

## 26. Amendment ledger

This Scope Lock changes exactly these Unit 3-B effects upon acceptance and merge:

| Unit 3-B location | Prior effect | Unit 3-BF effect |
|---|---|---|
| Section 3, Family F | Excluded on current evidence | Included for pre-qualification diagnostic scoping only |
| Section 5, item 14 | Full qualification before any experiment introducing an unqualified model; no shortcut | One bounded Family F diagnostic may precede qualification solely as an investment screen; no qualification credit or downstream authority |
| Section 10, final bullet | No model/provider ranking below qualification tier | Still no ranking; descriptive paired reporting and an absolute qualification-investment eligibility result are permitted |

Every other Unit 3-B requirement remains unchanged and controlling, including fixture reuse, new-fixture governance, semantic/representation separation, provenance, exact-once integrity, downstream isolation, and prohibition on experiment execution by a Scope Lock.

## 27. Decision register

| Question | Decision | Status |
|---|---|---|
| Is Path 2 adopted? | Yes, as a narrow Family F-only pre-qualification diagnostic exception effective only after acceptance and merge. | RESOLVED |
| Is Family F generally included for remedy experimentation? | No; included only for diagnostic scoping under this envelope. | RESOLVED |
| Is Qwen selected or qualified? | No. | RESOLVED |
| Is Llama selected or qualified? | No. | RESOLVED |
| Is the corpus fixed? | Yes, the complete frozen 23-fixture Unit 2 corpus. | RESOLVED |
| Are contexts fixed? | Yes, minimal and mixed-full production-like only. | RESOLVED |
| Is repetition fixed? | Yes, four attempts per fixture/profile/model cell. | RESOLVED |
| Is the total call count fixed? | Yes, 368 scored plus 24 warm-up calls, 392 total. | RESOLVED |
| Is the advancement rule fixed? | Yes, Section 18's absolute, all-conditions gate. | RESOLVED |
| Can evidence count toward qualification? | No. | RESOLVED |
| Can models be ranked or selected? | No. | RESOLVED |
| Is a Knowledge Discoverability dependency probe included? | No. | RESOLVED |
| Is model acquisition authorized? | No. | RESOLVED |
| Is implementation authorized? | No. | RESOLVED |
| Is a model run or campaign authorized? | No. | RESOLVED |
| Is Knowledge Discoverability Attempt 3 authorized? | No. | RESOLVED |
| What may follow acceptance? | A separately reviewed Family F Diagnostic Implementation/Execution Plan only. | RESOLVED |

## 28. Explicit non-claims

This Scope Lock does not claim that:

- Qwen is available now;
- Qwen can fit safely on the current server;
- Qwen is better than Llama;
- Llama is an adequate or inadequate production model;
- either model will pass the diagnostic gate;
- four attempts estimate a population rate;
- zero observed false positives prove a zero failure rate;
- diagnostic eligibility is qualification;
- model substitution repairs PF01;
- the prompt/protocol is faultless;
- any prior remedy family is eliminated;
- implementation or execution must proceed after acceptance;
- a correct tag proves promotion or recall;
- Knowledge Discoverability implementation is defective;
- Knowledge Discoverability live verification is satisfied; or
- Knowledge Discoverability closure is unblocked.

## 29. Exit criteria

This Scope Lock is ready to freeze only when:

1. every cited repository path resolves;
2. the amendment ledger is independently verified against Unit 3-B;
3. the 392-call schedule arithmetic is independently re-derived;
4. the corpus and profile definitions are verified against Unit 2;
5. the qualification boundary is verified against Unit 3-A;
6. every non-authorization statement is internally consistent;
7. `git diff --check` passes;
8. exactly this one document is changed; and
9. an Independent Constitutional Review returns `ACCEPTED` or `ACCEPTED WITH NON-BLOCKING QUALIFICATIONS`.

A blocking finding means the exception does not take effect and Family F remains excluded under the prior Unit 3-B classification.

## 30. Final authority statement

Pending acceptance and merge:

```text
PROGRAMME_STATUS=ACTIVE
FAMILY_F_STATUS=EXCLUDED_ON_CURRENT_EVIDENCE
UNIT_3B_ITEM_14_EXCEPTION_EFFECTIVE=NO
REMEDY_SELECTED=NO
MODEL_SELECTED=NO
MODEL_QUALIFIED=NO
QWEN_RUN_AUTHORIZED=NO
LLAMA_RUN_AUTHORIZED=NO
MODEL_RUN_AUTHORIZED=NO
CAMPAIGN_AUTHORIZED=NO
MODEL_ACQUISITION_AUTHORIZED=NO
IMPLEMENTATION_AUTHORIZED=NO
KNOWLEDGE_DISCOVERABILITY_ATTEMPT_3_AUTHORIZED=NO
KNOWLEDGE_DISCOVERABILITY_CLOSURE=BLOCKED
NEXT_LAWFUL_ACTION=INDEPENDENT_CONSTITUTIONAL_REVIEW_OF_THIS_SCOPE_LOCK
```

If this Scope Lock and its Independent Constitutional Review are accepted and merged, only these fields change:

```text
FAMILY_F_STATUS=INCLUDED_FOR_PRE_QUALIFICATION_DIAGNOSTIC_SCOPING_ONLY
UNIT_3B_ITEM_14_EXCEPTION_EFFECTIVE=YES_FOR_THE_SINGLE_BOUNDARY_DEFINED_HERE
NEXT_LAWFUL_ACTION=FAMILY_F_DIAGNOSTIC_IMPLEMENTATION_EXECUTION_PLAN
```

Every other `NO` remains `NO` until its own separately accepted governance act.
