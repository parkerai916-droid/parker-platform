**Status:** Unit 2 Post-Stage-0-Failure Governance Determination — **A: STOP UNIT 2 AT THE EXISTING STAGE 0 FAILURE BOUNDARY. PF02–PF08 CONTINUATION AUTHORITY IS NOT GRANTED.** Governance/review only, against committed baseline `9fd3dbe`, cross-checked against a read-only Ubuntu artifact-integrity capture. No live HTTP call, no `/api/generate`, `/api/tags`, or `/api/show` call, no campaign execution, and no repository mutation beyond this document occurred.

# Reasoning Protocol Live-Model Conformance Unit 2 — Post-Stage-0-Failure Governance Determination

## 1. Status

This is a governance determination, not an implementation act. It decides whether Parker should create new explicit authority permitting PF02–PF08 to be collected as additional unscored, preserved-failure evidence following the accepted PF01:D Stage 0 failure, or whether Unit 2 should stop at the existing boundary. No such authority is created by this document. Nothing is implemented, staged, committed, or pushed as a result of this review.

## 2. Evidence and governance reviewed

Read fresh against committed baseline `9fd3dbe` (fast-forwarded from `cbecaf3`, which added only the two Stage 0 Failure Review documents; nothing else changed):

- `docs/architecture/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_2_BASELINE_CHARACTERISATION_SCOPE_LOCK.md`
- `docs/implementation/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_2_BASELINE_CHARACTERISATION_IMPLEMENTATION_EXECUTION_PLAN.md`
- `docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_2_BASELINE_CHARACTERISATION_IMPLEMENTATION_READINESS_REVIEW.md` and its Independent Constitutional Review
- the artifact-root and Ollama identity-extraction defect confirmation/final-pre-commit-integrity/independent-constitutional review sets (both classified `B — UNIT 2 IMPLEMENTATION DEFECT`, both `CONFIRMED / CORRECTED`, both narrow and already merged before the live campaign ran)
- `docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_2_STAGE_0_FAILURE_REVIEW.md`
- `docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_2_STAGE_0_FAILURE_INDEPENDENT_CONSTITUTIONAL_REVIEW.md`
- `tests/integration/ReasoningProtocolBaselineCharacterisationTest.kt` (Stage 0 sequencing and stop logic, lines ~303–360) and `tests/integration/ReasoningProtocolLiveModelEvaluationHarness.kt` (production-chain wiring, classification logic)
- git history at `b34f8d0`, `3a7c606`, `c08f141`, `cbecaf3`, `9fd3dbe` — confirms a clean, linear, additive history: harness → driver → artifact-root fix → identity-extraction fix → Stage 0 Failure Review/ICR, with no force-pushes, reverts, or rewritten commits

Independently, on the authoritative Ubuntu runtime, I recomputed SHA-256/byte/line counts for every campaign artifact (`campaign-definition.txt`, `campaign-identity.txt`, `stage-0.failed`, `stage-0/STAGE-0/{intent,raw,checkpoint,manifest}`). Every value matches the inventory recorded in the committed Stage 0 Failure Review exactly. The campaign directory is unmodified since the failure was recorded. No `stage-0.sealed`, `stage-1/`, or `stage-2/` path exists.

## 3. Existing constitutional state

```text
Campaign ID: qwen25coder7b-baseline-20260809
3 warm-ups completed
PF01 completed — expected REMEMBER, actual REPLY, representation valid, classification D
PF02–PF08 not executed
stage-0.failed: "PF01:D" (verified byte-identical)
manifest state: PREFLIGHT_FAILED
stage-0.sealed: absent
Stage 1: not executed
Stage 2: not executed
Scored approval: false
```

The Stage 0 Failure Review and its Independent Constitutional Review both concluded, and this determination independently re-confirms: genuine semantic action-selection failure (category A of the standard taxonomy), valid representation, correct `D` classification, trustworthy production evaluation path (real `DefaultReasoningPromptBuilder` → `ModelReasoningProvider` → `LocalHttpModelInferenceClient` → `TaggedReasoningResponseParser`, no retry, no repair, no downstream Memory/Goal dependency), no harness or driver defect, and a fail-closed stop that exactly matches the accepted implementation. PF02–PF08 are **not authorized under existing governance**; the scored campaign is **not authorized**; early adverse closure is **not currently available**. This determination does not disturb any of those findings — it answers the one question they deliberately left open: whether new authority for a bounded PF02–PF08 continuation should now be created.

## 4. Purpose of Unit 2

Scope Lock §1: Unit 2 exists to produce "a trustworthy favorable or adverse baseline" through 3,900 scored trials with confusion matrices, Wilson intervals, content-fidelity review, and repeatability/context-drift reporting. It is **statistical baseline characterization**, not gross screening for its own sake, not remedy evaluation (explicitly forbidden — "Unit 2 does not qualify a model and must not select, recommend, or implement a remedy"), and not mere instrument validation. Instrument/environment validation and gross semantic screening are what Stage 0 does, but Stage 0 is a precondition to Unit 2's purpose, not the purpose itself.

This framing matters directly to the decision below: neither stopping now nor collecting PF02–PF08 can complete Unit 2's actual deliverable. The dead campaign — with or without PF02–PF08 — can never become the 3,900-trial sealed statistical baseline the programme was built to produce. Any evidentiary value PF02–PF08 might have is therefore necessarily secondary and diagnostic, not completion-bearing.

## 5. Meaning of the Stage 0 fail-closed boundary

Determined precisely, not by default to the broadest reading: the gate's constitutional purpose is **protection against entering scored execution with an unproven configuration**, enforced by requiring deliberate new governance before any continuation of any kind — it is not a blanket, permanent prohibition on ever observing anything further about a failed campaign. The Scope Lock and Implementation/Execution Plan speak specifically in terms of blocking *scored* calls and Stage 1; the driver's `require(!stage-0.failed.exists())` guard blocks *automatic* continuation, not continuation under a new, explicit, narrowly-bounded governance act. Both the Stage 0 Failure Review and its Independent Constitutional Review explicitly left this door open ("if continuation is later proposed, a dedicated new governance determination should decide...") rather than closing it. That is the door this document was asked to open or close.

So the correct answer to Q2 is: principally (a), not (b) — protection of the scored gate, not termination of all further measurement — with the caveat that any further measurement requires exactly the kind of deliberate act being performed here, not silent resumption.

## 6. Evidentiary value of PF02–PF08

Assessed on evidential value alone, not assumed outcomes:

- **PF02** (`R01-direct` under `mixed-full-production-like`) is the one genuinely pointed observation available: it would show whether the REMEMBER miss is context-sensitive (richer context resolves it) or context-independent (fails again). This is the single most decision-relevant fixture in the set.
- **PF03–PF08** (greeting/GOAL/NOACTION under minimal and full context) would show whether the miss is narrow (REMEMBER-only) or broad (other action categories also mis-selected under single-shot conditions) — real information, but each is still a single, unscored attempt; none of it can distinguish "narrow failure" from "unlucky single sample" with any statistical confidence, and none of it can establish a false-positive REMEMBER/GOAL rate, a repeatability finding, or a context-drift finding, all of which require the 30-attempt scored design Stage 0 explicitly does not provide.

The honest bound: PF02–PF08 would upgrade the record from "one adverse observation" to "eight single-attempt observations across four action categories and two context depths" — a qualitative breadth signal, never a rate, never a statistical characterization, and never combinable with a future campaign's own Stage 0 (a fresh campaign must run and seal its own complete, uncontaminated Stage 0 regardless of what this dead campaign's PF02–PF08 show).

## 7. Governance cost/risk of continuation

Marginal resource cost is trivial (seven single-attempt calls under a harness already proven trustworthy, no consequential downstream path). The real cost is governance-structural: Parker's accepted documents repeatedly express a strong, consistent institutional preference for fail-closed states to be terminal absorbers of caution — every analogous mechanism in this codebase (the false-positive pause rule, the consequential-pause implementation, resume-rejection on any identity mismatch) stops completely and demands a fresh, narrowly-scoped act rather than resuming toward "a bit more data." Authorizing a "collect a little more unscored evidence from behind a fail-closed gate" exception — even a perfectly bounded one — sets a precedent that every future Stage-0-equivalent failure can request the same accommodation, gradually normalizing partial continuation as the default response to a fail-closed stop. That risk is real even when, as here, the specific proposed exception could in principle be drawn narrowly enough to leave the failure state provably intact.

## 8. Early adverse closure analysis

Scope Lock §13 permits Unit 2 to close on a truncated adverse baseline only via "an independently accepted constitutional early-closure determination." PF01 is one trustworthy, genuine, adverse, single-attempt observation on one fixture under one context profile — it cannot stand for Remember reliability, the other three action categories, repeatability, context sensitivity, representation rates, false-positive rates, or latency/transport behavior across the registered 3,900-trial design. The route is **not available on current evidence**, exactly as the Stage 0 Failure Review and its Independent Constitutional Review already concluded. Collecting PF02–PF08 would not change this: eight more single-attempt observations remain non-statistical and could not by themselves satisfy an early-closure determination either. This document does not invoke or grant early closure; that determination, if ever pursued, belongs to the Completion Review / Independent Constitutional Review track, not a Stage-0-scoped continuation decision.

## 9. New-campaign analysis

A fresh campaign, when and if Parker chooses to pursue Unit 2's actual statistical deliverable, is the constitutionally cleaner path relative to continuing the failed one:

- it leaves the existing failed campaign (identity, artifacts, `stage-0.failed`, `PREFLIGHT_FAILED`) completely untouched — zero risk of dilution or reinterpretation;
- it runs the full, already-authorized Stage 0 sequence (W01–W03, PF01–PF08) under existing governance with **no new exception required**;
- it does not "evade" the original adverse finding — that finding remains permanently on record regardless of what a later campaign shows; a second independent trial is a replication, not an erasure;
- it does not meaningfully "duplicate" PF02–PF08 evidence collected under Option B, because that evidence would be orphaned in a dead campaign that can never contribute to a sealed baseline, whereas a new campaign's Stage 0 produces evidence that *can* lead somewhere (a sealed Stage 0 and, ultimately, scored execution).

This document does not authorize a new campaign — that requires its own campaign-identity and planning governance and is outside this determination's charter — but the analysis materially weakens the case for Option B: the diagnostic value PF02–PF08 would offer is, in the ordinary course, reproduced or exceeded by any legitimate next campaign's own mandatory preflight, without requiring any exception to the fail-closed doctrine at all.

## 10. Unit 3 dependency analysis

Unit 3 receives "immutable evidence" only after Unit 2 acceptance (Scope Lock §13, Implementation/Execution Plan §14). Unit 2 has not been accepted: no scored trial has run, no early-closure determination has been accepted, and PF01 alone is explicitly insufficient for either. Whether or not PF02–PF08 are ever collected, Unit 3 remedy work has no valid Unit 2 output to receive yet. Authorizing PF02–PF08 collection now would not accelerate Unit 3 readiness and carries a soft risk of prejudicing Unit 3's eventual framing (accumulated anecdote about which action categories also failed could bias remedy-family thinking even under an explicit no-remedy-recommendation constraint) — a further, if secondary, argument for minimalism.

## 11. Provisional determination

Applying the decision standard — additional evidence must be *necessary* to Unit 2's constitutional purpose, not merely useful — the provisional determination is:

**A — STOP UNIT 2 AT THE EXISTING STAGE 0 FAILURE BOUNDARY.**

PF02–PF08 would be informative but not necessary: Unit 2's actual deliverable is unreachable from this dead campaign regardless; the single confirmed PF01:D observation already fully satisfies the purpose a Stage 0 gate serves (a trustworthy signal that this configuration has a genuine, confirmed protocol-conformance miss, sufficient to trigger caution before further investment); and the specific breadth-of-failure question PF02–PF08 would partially answer is naturally and more robustly re-answered, without any exception to fail-closed governance, by any future campaign's own mandatory Stage 0.

## 12. Adversarial constitutional challenge

**Strongest case for PF02–PF08 being necessary, not merely interesting:**

1. Marginal cost is genuinely trivial against a harness already proven trustworthy and free of downstream consequence.
2. This is a "now or never" opportunity: this exact frozen commit/model-digest/environment combination cannot be perfectly reconstructed later. Any future campaign runs under a new identity; even an apparently identical model tag could reflect a different digest by then. Something is irreversibly lost by stopping.
3. Breadth-of-failure (narrow Remember-miss vs. broad decision-hierarchy failure) is qualitatively decisive information for how much urgency a "is this configuration even worth a 3,900-trial investment" judgment deserves, and PF01 alone cannot supply it.

**Does stopping create an evidentiary blind spot that undermines Unit 2?**

No, not one that undermines Unit 2's constitutional purpose. Unit 2's purpose is a *sealed, scored, statistical* baseline; this campaign cannot produce one with or without PF02–PF08, so nothing about Unit 2's actual mission is lost by stopping. The breadth question the adversarial case relies on is real but bounded to triage/diagnostic value, and — critically — is not uniquely available now: any legitimate next campaign necessarily re-runs the full preflight set under current conditions, reproducing the same class of evidence (and doing so under a currently-live environment rather than a frozen one, which is arguably more, not less, relevant to any forward-looking decision). The "now or never" argument proves less than it appears to: what would be lost is a historical curiosity about one particular frozen moment, not a foreclosed path to the diagnostic information itself. The single, already-confirmed, unambiguous PF01:D finding is sufficient on its own to trigger exactly the caution a Stage 0 gate exists to trigger; corroborating it with seven more single-attempt, non-statistical observations sharpens the anecdote without changing the decision it supports.

**Conclusion: the provisional recommendation survives the adversarial challenge.**

## 13. Final determination

```text
A — STOP UNIT 2 AT THE EXISTING STAGE 0 FAILURE BOUNDARY
```

No new continuation authority is created. `stage-0.failed`, `PREFLIGHT_FAILED`, and the absence of `stage-0.sealed` remain fully authoritative and permanent for campaign `qwen25coder7b-baseline-20260809`. PF01 must never be rerun. PF02–PF08 remain unauthorized. The campaign is preserved unchanged as adverse, truncated, unscored Stage 0 evidence.

```text
EARLY ADVERSE CLOSURE ROUTE: NOT AVAILABLE
SCORED CAMPAIGN: NOT AUTHORIZED
UNIT 3 REMEDY WORK: NOT YET READY TO BEGIN
```

Unit 3 dependency remaining: a valid Unit 2 output does not yet exist in any form — neither a completed 3,900-trial scored baseline nor an accepted early-closure determination. Either would require substantially more governance and (for the former) an entirely new campaign; neither is authorized or initiated by this document.

## 14. Exact next authorized action

None is compelled. The lawful state is: preserve campaign `qwen25coder7b-baseline-20260809` exactly as it stands, and stop. If Parker later wishes to pursue Unit 2's actual statistical deliverable, the available, already-governed path is to plan and authorize a **new** campaign (new campaign identity, full Scope Lock-conformant Stage 0 through Stage 2) rather than to seek continuation authority for this one. Any such new campaign requires its own campaign-identity/planning governance and explicit execution approval; none of that is authorized, proposed in detail, or initiated by this document.

## 15. Explicit prohibited actions

The following remain prohibited without further, separate, explicit governance:

- rerunning PF01;
- running PF02–PF08 against campaign `qwen25coder7b-baseline-20260809`;
- running Stage 1 or Stage 2 against this campaign;
- creating `stage-0.sealed` for this campaign;
- deleting, clearing, renaming, normalizing, or reinterpreting `stage-0.failed`;
- any scored model execution;
- any HTTP call to `/api/generate`, `/api/tags`, or `/api/show`;
- changing this campaign's identity, configuration, or artifacts in any way;
- selecting, recommending, or implementing a remedy;
- beginning Unit 3 remedy work.

Corrective action: **NONE.** This document authorizes no implementation, no live execution, and no campaign mutation.
