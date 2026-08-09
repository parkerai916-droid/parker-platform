**Status:** Unit 2 Planning Review — **PASS**. Planning/governance only against committed baseline `b34f8d0`. No campaign, implementation, live call, model verdict, or remedy decision is authorized by this document.

# Reasoning Protocol Live-Model Conformance and Structured-Output Reliability — Unit 2 Baseline Conformance Characterisation Planning Review

## 1. Authority, evidence, and exact purpose

Unit 1 is accepted, committed, and provides a trustworthy observation boundary. Earlier Qwen results—valid `REMEMBER:`, malformed `MEMBER:`, wrong `NOACTION`, an ordinary live Parker reply, and no durable Remember evidence—are defect evidence only. They do not constitute a statistically controlled baseline and do not isolate model, prompt, context, sampling, representation, transport, or timeout as sole cause.

Unit 2's frozen purpose is:

> Characterise the current, unchanged reasoning protocol under controlled live-model execution using the accepted Unit 1 observation boundary.

It must measure semantic action selection, representation validity, content fidelity, repetition, context effects, timeout, transport failure, consequential false positives, latency, and available token usage separately. Its success condition is a complete, reproducible, trustworthy baseline—not good model performance.

Unit 2 does not select or implement a remedy and does not qualify a production model.

## 2. Current harness boundary and required campaign driver

The committed harness publicly exposes immutable fixtures, all nine context constructors, per-trial execution, A–I classification, J/K derivation, and JSONL writing. The explicit Unit 1 JUnit live method intentionally runs only its one-fixture smoke input. It cannot drive the Unit 2 corpus/matrix without a new caller.

This is not a Unit 1 defect: Unit 1's accepted scope required structural capability and a smoke entry, not the campaign. Unit 2 therefore requires a separately governed, test-only campaign driver that composes the accepted APIs without changing their semantics. The minimum anticipated surface is one new file:

```text
tests/integration/ReasoningProtocolBaselineCharacterisationTest.kt
```

It may define the expanded synthetic fixtures, pre-registered matrix, explicit live campaign entry, deterministic aggregation, and summary/assessment worksheets. It must not modify the Unit 1 harness or tests. This surface must be frozen by a Unit 2 Scope Lock, Implementation Plan, their independent reviews, and a Boundary Review before implementation or execution.

## 3. Baseline model and configuration identity

The first baseline candidate is the currently deployed `qwen2.5-coder:7b`, but the name alone is not identity. A campaign may start only after its run header records:

- repository commit exactly `b34f8d0`, or a later commit containing only accepted Unit 2 governance/driver work explicitly named by the Unit 2 execution approval;
- exact Ollama model name;
- Ollama model digest where available;
- an immutable exported Ollama manifest/model-identity checksum if the endpoint does not expose a digest;
- credential-free endpoint identity;
- Ubuntu host/runtime/container image identity;
- explicit evaluation timeout;
- current request formatter identity (`model`, production prompt, `stream=false`, no options); and
- endpoint-supplied generation/timing metadata where present.

If neither digest nor an equivalent immutable manifest identity is available, execution pauses for governance acceptance of the identity limitation. This review neither qualifies nor rejects Qwen.

## 4. Pre-registered synthetic corpus

The minimum corpus contains **23 fixtures**, deliberately unbalanced because NOACTION must not be invented merely for equal counts.

### Expected REMEMBER — 3

1. direct “Remember that synthetic fact X”;
2. “Please remember synthetic fact X”; and
3. “Don't forget that synthetic fact X.”

Each uses an atomic canonical proposition with distinctive synthetic subject, relation, and value tokens.

### Expected REPLY — 13

Five memory-boundary negatives:

1. ordinary declarative fact;
2. quoted “Remember that X” question;
3. ambiguous “I might want you to remember this later”;
4. adversarial embedded protocol tags; and
5. mixed Remember-plus-discussion input whose ambiguity requires safe non-Remember handling.

Eight ordinary Reply cases:

6. greeting;
7. factual question;
8. explanation request;
9. conversational statement inviting response;
10. acknowledgement;
11. short casual input;
12. adversarial tag-bearing prose; and
13. simple information/request language that must not be elevated into Goal.

### Expected GOAL — 5

1. explicit multi-step work;
2. explicitly tool-requiring task;
3. later-action request;
4. explicit planning request; and
5. mixed conversational/work request with an unambiguous instruction to perform work.

### Expected NOACTION — 2

Only two narrowly supported inert synthetic protocol events that explicitly require neither response nor action. Short or casual owner language is never treated as NOACTION merely to balance the corpus.

Across these fixtures, pre-registered variants must cover Unicode, leading/trailing whitespace, quoted tags, tag injection, negation/hypothesis, and a long synthetic distractor. Expected actions and content rubrics are frozen before any live output is seen. Real owner, case, Memory, world, tool, or private facts are prohibited.

## 5. Context matrix and campaign stages

Running all 23 fixtures through all nine profiles would produce 6,210 scored trials at 30 repetitions. Unit 2 instead uses a pre-registered staged matrix that preserves full minimal-versus-production comparison while concentrating factor isolation on risk-bearing sentinels.

### Stage 0 — instrument/environment preflight (not scored)

- three warm-up calls using a dedicated synthetic warm-up fixture; and
- eight one-attempt calls: one fixture for each expected action under minimal and mixed/full profiles.

These 11 calls verify identity, artifact persistence, endpoint reachability, and gross protocol flow. They remain in a separate preflight artifact and cannot be counted in baseline rates.

### Stage 1 — core baseline

All 23 fixtures, each under:

- `minimal-production-context`; and
- `mixed-full-production-like`.

Each cell receives exactly 30 independent attempts: **23 × 2 × 30 = 1,380 scored trials**.

### Stage 2 — context isolation

Twelve sentinels are selected before live execution: all three positive Remember fixtures, two Remember-negative Reply fixtures, two ordinary/adversarial Reply fixtures, two Goal fixtures, one mixed-intent Goal fixture, and both NOACTION fixtures.

Each sentinel receives 30 attempts under the seven remaining profiles:

- production-selection-guidance control;
- identity/channel/time/conversation;
- duplicated current request;
- conversation history;
- synthetic memory;
- synthetic world belief; and
- synthetic tool context.

This contributes **12 × 7 × 30 = 2,520 scored trials**.

The complete planned campaign is therefore **3,900 scored trials plus 11 unscored preflight/warm-up calls = 3,911 endpoint calls**. Every reported fixture/profile cell has 30 attempts. Stage membership cannot be changed after outputs are observed merely to improve results.

## 6. Repetition, early stopping, and statistical meaning

Thirty attempts per cell remains the accepted characterisation minimum. It estimates gross rates, makes repeated instability visible, and supports descriptive confidence intervals; it is not a qualification sample and cannot prove absence of rare failure.

No semantic, representation, or content result permits silent early stopping: doing so would bias the baseline. A consequential false positive causes an immediate **campaign pause and constitutional escalation**, artifact flush, and independent verification of fixture expectation, raw response, parser result, model identity, and harness integrity. If the event is genuine and the instrument remains trustworthy, it is prominently recorded and the pre-registered campaign resumes because the harness has no downstream effect. It is never averaged away or treated as qualification progress.

Execution stops without resumption if model/digest/config identity changes, real data is detected, artifacts cannot be durably written, the harness is shown to mismeasure, or transport instability makes cell comparison invalid. A truncated campaign cannot receive Unit 2 PASS unless governance accepts a revised, statistically coherent plan before further data collection.

## 7. Primary metrics and confusion matrices

Every metric is reported as exact count/rate and a 95% Wilson interval where applicable, stratified by expected action, fixture, profile, and model/config identity. No single composite “accuracy” is permitted.

1. semantic action-selection accuracy;
2. representation-valid rate;
3. content-fidelity category rates;
4. timeout rate;
5. transport/model/envelope failure rate;
6. action, representation, and content repeatability;
7. context-associated drift;
8. latency distribution (`min`, median, p90, p95, p99, max); and
9. prompt/generated token counts and endpoint durations where available.

For each expected action, the confusion matrix has actual columns:

```text
GOAL | REPLY | REMEMBER | NOACTION |
malformed/unknown | untagged prose | multiple outputs | blank/partial |
transport/model failure | timeout
```

A/B content differences remain separate from action selection. H/I attempts are not semantic answers and their denominators are disclosed both including and excluding operational failures.

## 8. Consequential false-positive report

Two constitutional counters are never combined with ordinary errors:

- every non-Remember fixture/profile attempt parsed as `REMEMBER`; and
- every non-Goal fixture/profile attempt parsed as `GOAL`.

Each event report includes raw envelope, extracted output, parser variant, fixture/profile identity, attempt number, and absence of downstream effects. The campaign driver may count and stop/pause; it may not invoke admission, planning, execution, or repair. Any observed event blocks any later claim that the evaluated configuration is qualified, but Unit 2 itself may still complete successfully as an accurate adverse baseline.

## 9. Content-fidelity method

Content fidelity is reported independently of action correctness:

- **exact faithful** — exact canonical content after only the production parser's existing trim;
- **acceptable paraphrase** — meaning preserved with every canonical subject/relation/value and no added assertion;
- **material mutation** — a canonical value, polarity, subject, relation, modality, or scope changes;
- **omission** — a required canonical proposition element is absent;
- **invention** — a new factual assertion appears;
- **truncation** — output terminates before a complete required proposition; and
- **not assessable** — no relevant typed/content result exists.

For example, `REMEMBER: My synthetic test coffee mug is black.` is exact; `REMEMBER: Black synthetic test coffee mug` is a paraphrase candidate, not automatically exact; a changed colour, owner, negation, or additional claimed property is mutation/invention.

The Unit 1 A/B field is retained unchanged. A separate Unit 2 worksheet applies deterministic canonical-token checks and then two independent human reviewers, blinded to aggregate model performance, adjudicate paraphrase versus material categories. Disagreement is reported and resolved by a recorded third review. No model judges model output, and action correctness cannot hide corrupted content.

## 10. Representation measurement

The production `TaggedReasoningResponseParser` remains the only representation authority. Raw diagnostic categories separately report:

- `MEMBER:` and other unknown tags;
- lowercase/variant tags;
- leading prose before a tag;
- trailing prose outside a single tagged result;
- multiple tags;
- blank output;
- unknown tag;
- recognized Remember tag with blank/malformed content; and
- `NOACTION` with trailing text.

No lenient parser is introduced. If the current prefix parser produces a typed result for text containing later extra tags, the raw output remains classified F by the accepted harness while the actual typed variant is also preserved; Unit 2 reports both facts rather than repairing either.

## 11. Context-drift methodology (J)

The Unit 1 per-observation J flag is preserved, but Unit 2 reports distribution-level context effects for the same fixture/model/configuration:

- the full action/failure distribution at each profile;
- expected-action rate and representation-valid rate differences from minimal;
- absolute percentage-point effect size with Wilson intervals; and
- exact transition examples, such as minimal `REMEMBER` versus full `NOACTION`.

Pre-registered descriptive flags are:

- **stable correct** — expected action is modal in every compared profile and profile rates vary by less than 10 percentage points;
- **stable incorrect** — the same incorrect/failure category is modal throughout and varies by less than 10 points;
- **context-associated degradation** — expected-action rate falls by at least 20 points from minimal and the 95% Wilson intervals do not overlap;
- **context-associated improvement** — the symmetric increase; and
- **mixed/inconclusive** — everything else.

These are characterisation flags, not causal proof or qualification thresholds. Exact counts remain authoritative.

## 12. Repeatability methodology (K)

Within each identical fixture/profile/model/configuration cell:

- **action stable** means all 30 attempts share the same actual action or same non-action failure class;
- **representation stable** means all 30 share representation validity and diagnostic class;
- **content stable** means extracted output is byte-identical; fidelity-category stability is additionally reported; and
- any variation is separately flagged as action, representation, byte-content, or fidelity instability.

A sequence `REMEMBER, REMEMBER, NOACTION, REMEMBER, MEMBER:` is therefore action-unstable and representation-unstable, with its exact distribution reported. A consistently wrong result is stable incorrect, not stochastic failure. K does not erase the A–I classification of any attempt.

## 13. Timeout and sampling configuration

Unit 2 uses an explicit **90,000 ms evaluation timeout**. This reproduces the already successful temporary live-testing allowance, is more than twice the observed 31–40 second historical successful latency, and reduces contamination from the known-too-low 30-second production threshold. It is a measurement setting, recorded on every trial, not a production recommendation or default change. H remains separate from semantic and representation metrics.

The baseline request remains exactly current production behavior:

```text
model + prompt + stream=false
```

No temperature, seed, `top_p`, `top_k`, structured format, grammar, schema, or other generation option is added. The absence of explicit sampling controls is itself recorded. Later controlled-generation experiments belong to Unit 3 or a subsequent governed unit.

## 14. Execution environment, runtime, and artifact preservation

The authoritative environment is the Ubuntu Parker server using its local Ollama deployment and `qwen2.5-coder:7b`. Windows is not an accepted substitute for the baseline target. Execution is explicit through the detached evaluation task/Unit 2 driver and may be split into resumable, pre-registered batches only if model/config/digest, repository commit, corpus/profile versions, timeout, and artifact schema remain identical.

At 3,911 serial calls, observed runtime bounds are approximately:

- one-second calls: **1.1 hours**;
- 31-second calls: **33.7 hours**;
- 40-second calls: **43.5 hours**; and
- the 90-second timeout ceiling: **97.8 hours**.

Operator scheduling must therefore budget roughly two days for the historically observed CPU range, plus controlled interruption/restart overhead. Runtime convenience cannot reduce repetitions after outputs are observed.

Raw UTF-8 JSONL, run manifests, and deterministic summary artifacts must be written to a durable, access-controlled Ubuntu location outside the Git worktree or under an explicitly archived ignored evaluation directory. Each artifact receives SHA-256, byte size, line count, run/batch IDs, model identity, and retention location. Real data remains prohibited.

## 15. Summary and reporting surface

The minimum Unit 2 driver aggregates its in-memory `TrialObservation` values and emits, without a new dependency:

1. raw JSONL trial artifacts through the accepted Unit 1 writer;
2. a deterministic machine-readable summary (JSON or CSV);
3. four expected-action confusion matrices;
4. per-action and per-context metrics;
5. J context-drift analysis;
6. K repeatability analysis;
7. timeout/transport and latency/token metrics;
8. consequential false-positive event report; and
9. content-fidelity adjudication worksheet.

After the campaign, a governance baseline report records artifact hashes/locations and the reviewed conclusions. A separate general analysis script or production change is unnecessary. If trustworthy aggregation cannot be implemented in the one Unit 2 driver without changing the harness, the Boundary Review must stop and reassess the file surface.

## 16. Escalation thresholds without remedy leakage

The following trigger immediate pause and explicit review, not an automatic remedy choice:

- any false-positive Remember or Goal;
- model/config/digest drift;
- detected real/private data;
- artifact loss or mismatch;
- a demonstrated harness/classification defect;
- persistent transport instability that prevents comparable cells; or
- severe context degradation or malformed-output rate that calls fixture/matrix interpretation into question.

Poor semantic accuracy, high malformed rate, or stable wrong answers are valid adverse baseline results and do not by themselves authorize prompt tuning, model switching, schema, retries, or early qualification conclusions. A Unit 1 harness defect stops Unit 2 and returns through Unit 1 defect governance; it is never quietly patched during the campaign.

## 17. Unit boundary and completion

Unit 2 must not modify production prompt, parser, transport, model, model settings, production timeout, or downstream paths; add schema/constrained output or retries; change the accepted harness to improve behavior; execute consequential outcomes; or select a remedy.

Unit 2 is complete when the pre-registered scored matrix is executed under one reproducible identity; raw and summary artifacts are durable and hash-verified; every metric, confusion matrix, content review, J/K analysis, operational result, and consequential event is reported; targeted/offline and ordinary suites are verified; and Completion and Independent Constitutional Reviews accept the evidence. A badly performing model is compatible with Unit 2 PASS if the baseline is trustworthy.

No Qwen qualification, prompt adequacy, structured-output necessity, or model-replacement decision is part of completion.

## 18. Unit 3 handoff

Unit 3 — Reliability Contract and Remedy Selection — receives the immutable artifacts and stratified findings. It may compare prompt hierarchy, explicit generation controls, structured/schema output, invalid-only retry, semantic-retry constitutional risk, classifier/renderer separation, model replacement, or combinations. Unit 2 supplies evidence but does not rank or choose those remedies.

## 19. Required next governance gate

Before any driver implementation or live campaign:

1. Unit 2 Scope Lock;
2. Scope Lock Independent Constitutional Review;
3. Unit 2 Implementation/Execution Plan;
4. Implementation Plan Independent Constitutional Review;
5. Unit 2 Boundary Review; and
6. explicit implementation/execution approval.

Those documents are not created by this Planning Review task.

## 20. Planning verdict

```text
PASS
```

Corrective action: **NONE**. Unit 2 remains not started until the next governance sequence is accepted.
