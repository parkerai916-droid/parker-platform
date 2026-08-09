**Status:** Finalised Unit 2 Stage 0 Failure Review — **FAILURE CONFIRMED / CONTINUATION NOT AUTHORIZED**. Governance/evidence review against committed baseline `cbecaf3`, independently cross-model reviewed and supplemented by a read-only Ubuntu artifact-integrity capture. No live HTTP call or campaign execution occurred during this review.

# Reasoning Protocol Live-Model Conformance Unit 2 — Stage 0 Failure Review

## 1. Immutable campaign identity and supplied artifact state

```text
Campaign ID: qwen25coder7b-baseline-20260809
Repository commit: cbecaf32524b3372947c4bdd58661ef52d833239
Branch: main
Pre-execution repository state: HEAD == origin/main; clean
Model: qwen2.5-coder:7b
Ollama digest: dae161e27b0e90dd1856c8bb3209201fd6736d8eb66298e75ed87571486f4364
Evaluation timeout: 90000 ms
Scored approval: false
Artifact root: /var/lib/parker/reasoning-protocol-live-model/qwen25coder7b-baseline-20260809/
```

Supplied execution inventory:

- 3 warm-ups completed;
- PF01 completed;
- PF02–PF08 not executed;
- 4 intent records, 4 raw observations and 4 checkpoint entries;
- manifest status `PREFLIGHT_FAILED`;
- `stage-0.failed` contains `PF01:D`;
- `stage-0.sealed` absent;
- no Stage 1 or Stage 2 evidence.

## 2. PF01 evidence and classification

Frozen fixture:

```text
R01-direct / minimal-production-context
Remember that my synthetic test coffee mug is black.
Expected: REMEMBER
```

Captured live output:

```text
REPLY: I understand that your synthetic test coffee mug is black. Is there something specific you'd like me to do related to this information?
```

The output is one syntactically valid tagged result. `TaggedReasoningResponseParser` correctly maps the `REPLY:` prefix to `ReasoningProviderResponse.Reply`; actual action is therefore REPLY. Unit 1 classification selects `D` whenever parsing succeeds but actual action differs from the fixture's expected action. Representation validity is correctly `true`. There was no timeout, transport failure, parser failure or malformed representation.

Determination:

```text
A — genuine semantic action-selection failure by qwen2.5-coder:7b
```

It is not a representation failure, parser/classifier defect, campaign-driver defect, configuration/context defect, or insufficient-evidence result.

## 3. Harness trustworthiness

The committed path constructs and observes:

```text
DefaultReasoningPromptBuilder
→ ModelReasoningProvider
→ LocalHttpModelInferenceClient
→ configured qwen2.5-coder:7b endpoint
→ TaggedReasoningResponseParser
→ TrialObservation
```

The production builder's fixed guidance explicitly identifies direct “Remember that X” language as REMEMBER and requires only the fact after that tag. The minimal profile supplies no alternate context that changes the owner input. Transparent capture checks that the prompt sent equals the production-generated prompt. The supplied raw/extracted response, parsed `Reply`, classification `D`, endpoint metadata and durable record inventory are mutually consistent.

No retry or semantic repair exists. The evaluation harness terminates in `TrialObservation` and has no Memory, Goal, Knowledge Submission, planning, execution or runtime-composition dependency. No downstream consequence occurred.

Harness determination: **TRUSTWORTHY; NO MEASUREMENT DEFECT FOUND**.

## 4. Constitutional meaning of Stage 0

Stage 0 has combined but bounded purposes:

1. eleven unscored calls test identity, transport, prompt construction, parsing, raw capture and durable artifact operation;
2. the four expected-action preflight fixtures test gross semantic protocol flow under minimal/full contexts; and
3. Stage 0 is a mandatory fail-closed gate before scoring.

The Scope Lock says no scored call begins unless identity/artifact checks pass and Stage 0 is sealed. The Implementation Plan requires W01–W03 then PF01–PF08 and verification of exactly eleven records, while also requiring Stage 0 failure to prevent Stage 1. The accepted implementation/readiness reviews make the semantic gate concrete: a non-A/B preflight is durably preserved as `PREFLIGHT_FAILED`, continuation is blocked, and no Stage 0 seal is written.

Thus Stage 0 is both an instrument/environment sanity pass and an early semantic screening gate. It is not itself a scored statistical baseline.

## 5. Driver behavior

The driver forced PF01 raw evidence, checkpointed it, wrote `PF01:D`, sealed the batch manifest as `PREFLIGHT_FAILED`, returned before PF02, and withheld `stage-0.sealed`. On a later invocation it rejects the existing `stage-0.failed` marker before selecting another trial.

That behavior exactly matches the accepted implementation and its accepted readiness/constitutional reviews. No implementation defect is identified.

## 6. Continuation determinations

```text
PF02–PF08:
NOT AUTHORIZED UNDER EXISTING GOVERNANCE
```

The frozen plan originally enumerates all eight preflight calls, but the later accepted implementation boundary deliberately blocks continuation after a failed semantic preflight. No accepted text authorizes deleting, bypassing, clearing or reinterpreting `stage-0.failed`, and no current runner path can continue while preserving it. PF02–PF08 therefore cannot run under current governance. If continuation is proposed in future, it would first require new explicit governance authority defining whether and how an already-failed Stage 0 may collect remaining unscored observations without weakening or disguising the failure. That future requirement is not present authorization.

```text
SCORED CAMPAIGN:
NOT AUTHORIZED
```

Scored approval is false, `stage-0.sealed` is absent, and the manifest is `PREFLIGHT_FAILED`. Both governance and code require a valid identity-bound Stage 0 seal before Stage 1. Stage 1 and Stage 2 must not begin.

## 7. Adverse-baseline early closure

```text
UNIT 2 EARLY ADVERSE CLOSURE:
NOT CURRENTLY AVAILABLE
```

The Scope Lock permits only an independently accepted constitutional early-closure determination for a truncated adverse baseline. PF01 is one trustworthy adverse observation, but it does not statistically characterize Remember reliability, the other actions, repeatability, context effects, representation rates, false-positive rates, latency distributions or transport behavior. Three warm-ups plus one preflight cannot responsibly stand for the registered 3,900-trial baseline.

Additional live observations would be required for broader characterization, but none are presently authorized. Governance clarification must precede further live execution.

## 8. Exact supported conclusion

Fully supported:

> The evaluated `qwen2.5-coder:7b` configuration can produce syntactically valid Parker protocol output while selecting REPLY instead of REMEMBER for an explicit direct Remember instruction under the minimal production context.

Also supported: this occurred once at PF01 under the pinned commit/model/digest/timeout identity, with no timeout, transport or parser failure.

Not supported:

- all Remember requests fail;
- a failure probability or stochastic reliability rate;
- context caused the failure;
- prompt design is the sole cause;
- Qwen is generally unsuitable;
- structured output, retry, sampling or any other remedy would fix it;
- another model would perform better;
- false-positive Remember or Goal rates;
- performance for Reply, Goal, NOACTION, full context or the remaining fixtures.

No remedy is selected. Remedy families remain future Unit 3 questions only.

## 9. Artifact preservation and read-only integrity inventory

The existing campaign directory is immutable evidence. A read-only integrity inventory was captured on the authoritative Ubuntu Parker runtime after the failure:

| Artifact | SHA-256 | Bytes | Lines |
|---|---|---:|---:|
| `campaign-definition.txt` | `64ce538c39d135b9bcd7fee14bb5e49bcc9ef0ef1962d5afb318b02d9fe662a3` | 404211 | 3950 |
| `campaign-identity.txt` | `c2f7f56a7dabe552e5311051e2545d73a403e417137b56ccebb841b46ac12c94` | 202 | 3 |
| `stage-0.failed` | `8bb87b7a23b30de1bb8b890272f402bf91cbcbc845f8091e56e07ba6d9f11a1f` | 7 | 1 |
| `stage-0/STAGE-0/intent.jsonl` | `325ca44ec35f8c1594f77105fe0ca2f1a93eaa088fc4d6cd7324daccf5ca0f32` | 728 | 4 |
| `stage-0/STAGE-0/raw.jsonl` | `c635ebcd051a7eeb02e154e3b07a4ba9e101fcd71f019bebc5990961f8179d5f` | 24727 | 4 |
| `stage-0/STAGE-0/checkpoint.txt` | `1cdd4644df9c8d8f9437354228a090300c65285de1c70d2bfb7db2e72af2fd08` | 360 | 4 |
| `stage-0/STAGE-0/manifest.txt` | `4e63cb8a13edc7bfc146e4573cc4717acea00b5a86e4091416e4c985b2a40cce` | 275 | 10 |

This inventory was captured read-only on Ubuntu; it was not independently recomputed by the Windows workspace. It fingerprints the preserved failed state and is consistent with three warm-ups, PF01, four intent/raw/checkpoint records, `stage-0.failed`, failure before PF02 and no Stage 0 seal. It does not establish any broader semantic or statistical conclusion.

## 10. Next governance gate

The present lawful next step is to preserve the failed campaign and stop. PF01 may not be rerun; PF02–PF08 and the scored campaign are not authorized. Only if continuation is later proposed should a dedicated new governance determination decide whether a failed Stage 0 may collect PF02–PF08 solely as preserved unscored evidence. Any such future determination must not clear the failure, manufacture a Stage 0 seal, rerun PF01, change campaign identity/configuration, silently authorize scoring, or select a remedy.

Corrective action: **NONE within this review. Any future continuation requires new explicit governance authority before any live execution.**

## 11. Independent cross-model confirmation

An independent Claude cross-model review returned `ACCEPTED WITH QUALIFICATIONS` and independently confirmed the semantic-failure classification, valid representation, correct D classification, trusted production evaluation path, absence of harness/parser/driver defects, correct fail-closed stop, prohibition on Stage 1/2, absence of remedy authority and prohibition on broad reliability inference. Its precision refinement—PF02–PF08 are not authorized under existing governance—is incorporated above.
