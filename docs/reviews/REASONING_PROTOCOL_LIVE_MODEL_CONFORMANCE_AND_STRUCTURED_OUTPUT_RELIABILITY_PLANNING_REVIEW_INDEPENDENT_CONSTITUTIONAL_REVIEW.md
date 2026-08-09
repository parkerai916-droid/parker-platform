**Status:** Independent Constitutional/Planning Review — **ACCEPTED**. The Planning Review was treated as evidence, not authority. Review performed against baseline `05d4c2a`, current contracts, implementation, composition, deterministic tests, and accepted consequential-action boundaries.

# Reasoning Protocol Live-Model Conformance and Structured-Output Reliability Planning Review — Independent Constitutional Review

## 1. Independent reconstruction

The confirmed defect is narrower than “Memory failed”: a live model did not reliably select and represent Parker's four-action protocol under production-like prompting. The accepted Memory admission and retrieval programme begins only after a typed `Remember` exists and remains closed. Current code validates exact tags, routes only typed Remember to admission, and has no heuristic repair. Existing stub-server tests establish deterministic orchestration but cannot qualify a real model.

The constitutional question is therefore whether the proposed programme measures this boundary without prematurely selecting a remedy or widening consequential authority.

## 2. Challenge findings

| Challenge | Independent finding |
|---|---|
| Blames the model without sufficient evidence | **Sound after qualification.** The plan calls this a conformance problem with unresolved cause and tests prompt, context, configuration, representation, and model capability independently. |
| Assumes prompt weakness | **Sound.** Prompt changes are only one evidence-dependent option; two bounded revisions are a stop rule, not a presumption that revisions will work. |
| Mistakes structured output for semantic correctness | **Sound.** Representation validity and action correctness are separate metrics; JSON/schema remains model-authored semantic output. |
| Retries create consequential authority | **Sound with explicit boundary.** Invalid-only retries are separately governed, all attempts remain evidence, exhaustion fails closed, and valid non-consequential outcomes cannot be escalated. Semantic retry is excluded initially. |
| Corpus is inadequate | **Sound for planning.** It covers all actions, positive/negative Remember, Goal discussion versus instruction, tightly bounded NOACTION, adversarial tags/injections, mixed intent, context factors, and held-out qualification. Mixed-intent precedence must be governed before scoring. |
| Metrics hide false positives | **Sound.** Per-action/stratum reporting and confusion matrices are mandatory; consequential false positives and material content mutation have zero-observed gates independent of aggregate scores. |
| Logging exposes owner content | **Sound.** Dedicated opt-in artifacts permit only synthetic prompts and prohibit real production content and ordinary verbose logging. |
| Live tests contaminate normal builds | **Sound.** Unit 1 must add an explicit source set/task; the current `test` task remains offline and external-state-free. Build changes require Boundary Review. |
| Qualification is measurable | **Sound.** The plan freezes action, representation, fidelity, timeout, repeatability, confidence, model digest, and context-stratum criteria before data collection. The 300-exposure critical gate has an explicit statistical rationale and is not represented as proof of zero risk. |
| Units are not minimal | **Sound after challenge.** Measurement precedes contract selection; implementation is conditional; closure is distinct from remedy coding. Combining Units 1 and 2 would risk changing the instrument while characterising the baseline. |
| Timeout is mixed with semantics | **Sound.** Timeout is H, not a semantic answer; it is measured on a separate axis and receives no setting change in this plan. |
| Existing fail-closed behaviour is weakened | **Sound.** Parser validation, typed action routing, admission, Goal handling, downstream authorization, and no-heuristic rules remain explicit invariants. |

## 3. Additional adversarial checks

### Schema-valid wrong action

A constrained object such as `{action: "NOACTION"}` can be perfectly valid and constitutionally wrong for “Remember that X.” The Planning Review counts it as class D and fails semantic qualification. This prevents format success from laundering a wrong decision.

### Retry-created Remember

If attempt one is malformed and attempt two returns Remember, the second result remains model-authored rather than parser-inferred, but it increases consequential exposure. The plan therefore requires separate retry authority, immutable attempt evidence, fixed limits, and qualification of retry behaviour. It does not authorize that policy now.

### Paraphrased proposition

A correct Remember discriminator does not establish faithful owner instruction. The plan separates paraphrase from exact content, prohibits material mutation/invention, and avoids an ungoverned model-as-judge. This is necessary because downstream governance faithfully persists the typed proposition it receives.

### Evaluation harness authority

The proposed harness measures production collaborators but must not be a production source set, default task dependency, production endpoint, or alternate parser. Its fixture classifier may calculate evidence only; it cannot become a runtime semantic decision path. Unit 1's Scope Lock and Boundary Review must enforce this.

### Model qualification identity

Qualification attaches to an exact digest and configuration, not a mutable model name. An unavailable digest is a recorded limitation requiring an equivalent immutable image/model identity before production closure; it is not silently ignored.

## 4. Issue classification

- Live wrong/malformed outputs: **CONFIRMED PROGRAMME INPUT**, not yet assigned to a single implementation defect.
- Existing lack of live-model conformance testing and raw opt-in evidence: **MEASUREMENT GAP** addressed first by Unit 1.
- Thirty-second timeout on hardware with observed 31–40 second successful inference: **SEPARATE OPERATIONAL QUALIFICATION ISSUE** within the measurement programme, not semantic evidence.
- Existing exact parser rejection: **CONSTITUTIONALLY CORRECT FAIL-CLOSED BEHAVIOUR**.
- Existing duplicated current-request representation: **OBSERVATION REQUIRING CONTROLLED EVALUATION**, not an authorized production defect finding.
- Gap #54 Memory and authorization components: **OUTSIDE SCOPE / NOT REOPENED**.

No constitutional defect exists in the Planning Review. No implementation correction is authorized or required at this stage.

## 5. Independent verdict

```text
ACCEPTED
```

Corrective action: **NONE**.

The exact next step remains **A. evaluation harness first**, beginning with a Unit 1 Scope Lock and Implementation Plan plus independent reviews—not implementation, prompt revision, structured output, retry, model replacement, or timeout alteration.
