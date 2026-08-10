**Status:** Unit 3-C Controlled Remedy Experiments — Timeout and Durability Scope Lock Amendment — **PROPOSED — PENDING INDEPENDENT CONSTITUTIONAL REVIEW.** This document amends specific, named sections of the frozen Unit 3-C Scope Lock (`fee2edd`) in place of editing that document directly, preserving its original text as the historical record of what was originally frozen and why. It contains no implementation, authorizes no live model call, selects no remedy, and does not reopen Unit 3-D or Unit 4 authority.

# Unit 3-C Controlled Remedy Experiments — Timeout and Durability Scope Lock Amendment

## 1. Baseline and authority

Drafted against committed baseline `f5c4dd7636e8f35f61c3320da8c3cb7f75ef8687`. Amends the frozen Unit 3-C Scope Lock (`fee2edd`), Sections 14, 16, 18, and 20 only — every other section of that document remains in full, unamended force. Controlling evidentiary authority: the Unit 3-C Timeout and Inference Latency Investigation Review and its Independent Constitutional Review (`ACCEPTED WITH QUALIFICATIONS`), both committed at `f5c4dd7`. Controlling cross-unit authority: the frozen Unit 3-B Remedy Experiment Scoping Scope Lock's own item 20 — **"any future experiment driver must implement the same durable ledger/checkpoint/intent-record discipline already implemented for Unit 2-D, not a weaker mechanism"** — independently re-confirmed present and unamended in this task. This amendment does not itself implement anything; per Unit 3-C's own established pattern (Scope Lock → Plan → implementation → review chain), a future implementation task must still occur, pass Completion/Readiness/Approval review, before any further live-execution attempt.

## 2. Amendment to Section 14 (Inference configuration, Phase M) — timeout value

**Original text (preserved, not deleted):** *"...the timeout value (`ModelReasoningProvider`'s current `30_000` ms default, unless a future, separately-governed act states an affirmative reason to vary it)..."*

**This amendment is that affirmative act.** Frozen, amended: **the Unit 3-C timeout value is 90,000 ms**, not `ModelReasoningProvider`'s `30_000` ms default.

**Evidentiary basis, preserved verbatim in substance, not restated as bare authority:**

- 90,000 ms is **not selected because it is a conventional round number**. It is the literal timeout value already used, and already empirically validated with zero timeouts, across two prior, separate, real Parker programme campaigns (Unit 2, Unit 2-D) on this exact hardware, with this exact model.
- 26 preserved qwen2.5-coder:7b observations exist across those two campaigns, all completed within 90,000 ms.
- Two historical cold-start observations: approximately **39.92 s** and **48.57 s**.
- Worst preserved warm observation: approximately **27.99 s** — already consuming 93.3% of the *previous* 30,000 ms ceiling while fully warm.
- Unit 3-C's own first genuine live-execution attempt (Attempt 3, `db9c612`) was terminated by the 30,000 ms ceiling at approximately 30.861 s, before producing any evidence, on what is **STRONGLY SUPPORTED, NOT CONFIRMED** to have been a cold-start load. **Cold-start loading is not stated here as a proven fact** — this amendment carries that exact confidence qualifier forward unchanged from the Investigation Review and its Independent Constitutional Review, and any future document citing this amendment must do the same, not silently upgrade it to "confirmed."

This amendment satisfies the frozen requirement already on record (Unit 3-A Reliability Contract Definition Scope Lock §4 row 36; Programme Planning Review §12): *"Production timeout value must derive from measured percentiles, explicit margin, and maximum."* The margin basis (worst cold observation × ~1.85, worst warm observation × ~3.2) is stated, not invented.

## 3. Amendment to Section 16 (Stop conditions, Phase O) — timeout classification

**Original text (preserved, not deleted):** *"Frozen distinction: measurement-invalidating failures — repository identity mismatch, model identity mismatch, configuration drift, harness defect, parser/classifier measurement defect, artifact-integrity failure, call-accounting ambiguity, unauthorized downstream action, campaign corruption — fail closed. Remedy-performance failures — wrong semantic action, false-positive REMEMBER, false-positive GOAL, representation failure, content-fidelity failure — are evidence and must be recorded, not concealed..."*

**This amendment extends, and explicitly does not silently resolve beyond, that frozen distinction:**

**Frozen, amended: a timeout occurring during any Unit 3-C governed warm-up trial is a measurement-invalidating failure.** Required consequence, frozen: a durable timeout outcome must be recorded; the campaign must halt (not merely the arm — a warm-up failure already, per the existing warm-up orchestration correction, prevents the affected arm's own scored trials from beginning at all, and Control's own warm-up gates the only arm order that runs before Family A/B/C); no progression into any measured arm may occur; no silent retry; no automatic recovery; no classification of the event as remedy-performance evidence under any circumstance; campaign state must be preserved exactly as produced; and any future continuation or rerun requires fresh, separate governance — not resumption under this amendment's own authority.

**Frozen, amended, and explicitly left unresolved: the classification of a timeout occurring during a scored (non-warm-up) trial remains UNRESOLVED.** This is a deliberate, binding non-decision, not an oversight: neither "remedy-performance evidence," "measurement-invalidating failure," "continue," "halt the arm," nor "halt the campaign" is selected by this amendment for that case. **This unresolved status blocks any future live Unit 3-C campaign attempt from proceeding past the point where a scored-trial timeout could first occur**, until a separate, future governance act resolves it — because without a resolved classification, a future implementation would have no frozen instruction for what to do when (not merely if) a scored-trial timeout occurs, and Unit 3-C's own Section 15 (Adaptive-experimentation prohibition) already forbids improvising experimental behavior mid-run. The next required gate is a dedicated governance determination for this specific question (Section 6 of this amendment's companion Independent Constitutional Review addresses whether this is correctly classified as blocking).

## 4. Amendment to Section 17 (Semantic/representation separation) — no change

Independently re-confirmed: nothing in this amendment touches semantic correctness, representation validity, or content-fidelity axes. Section 17 is unamended.

## 5. Amendment to Section 20 (Artifact/provenance requirements, Phase S) — intent-before-call durability

**Original text (preserved, not deleted):** *"...every observation must be attributable to campaign; family; arm; fixture; context profile; trial sequence; expected action; actual action; semantic result; representation result; content-fidelity result where applicable; model/configuration identity; prompt/protocol identity; candidate mechanism identity; the raw provider request and response where applicable; latency/transport result; and artifact hash/provenance. The future Implementation/Execution Plan must define the exact schema satisfying this requirement."*

**This amendment adds a frozen, constitutional-level requirement that Section 20's original text did not itself state explicitly, though it is not a new invention** — it is the Unit 3-C-specific instantiation of an already-binding, cross-unit requirement: Unit 3-B Scope Lock item 20 (Section 1 above) already requires "the same durable ledger/checkpoint/intent-record discipline already implemented for Unit 2-D, not a weaker mechanism," and Unit 3-C's own Implementation/Execution Plan Section 17 already states, independently confirmed by fresh re-reading for this task, that "a durable ledger entry recording the intended trial ID is written and fsynced before any HTTP call is issued" — a requirement the current committed implementation does not fully satisfy (`Unit3CArmLedger` writes an arm-level `identity.txt` once, but no per-trial intent record before each individual call; Attempt 3 is direct evidence of the resulting gap). This amendment makes that already-existing, multi-layer requirement unambiguous and field-complete:

**Frozen: before every live model call, a durable intent record must exist**, containing at minimum: campaign ID; arm; fixture; trial/repetition identity; expected action; model name; model digest; repository commit; prompt/request identity; timeout; inference/configuration identity; and call identity. **Only after this durable intent record exists may the model request be transmitted.** This is not implemented by this amendment, exactly as Section 17 of the Plan already states of itself — a future implementation task must satisfy it, following this programme's own established review chain, before any further live-execution attempt.

**Frozen, new: the minimum durable terminal timeout observation**, required whenever a call's intent record exists but no response was received within the governed timeout: intent/call ID; campaign/arm/fixture/trial identity; call-start timestamp; timeout ceiling; elapsed duration; terminal classification `TIMEOUT`; response-bytes-received yes/no; transport/error classification; model/configuration identity; parser result explicitly absent (never fabricated); semantic classification explicitly absent (never fabricated); and checkpoint/halt status. **Frozen, explicit prohibition: a request that returned no response must never be recorded as `GOAL`, `REPLY`, `REMEMBER`, or `NOACTION`** — those four classifications describe a parsed response; a timeout has none.

**Frozen, new: exact-once semantics are extended to distinguish four states**, not the previous two (never-transmitted / transmitted-and-completed):

- **(A) Never transmitted** — no intent record exists.
- **(B) Transmitted and completed** — intent record and a durable, checkpointed raw observation both exist. Complete; never re-issued.
- **(C) Transmitted and timed out** — intent record exists; no raw observation was durably persisted within the governed timeout. **Must not be automatically repeated.**
- **(D) Transmitted with ambiguous terminal transport state** — intent record exists; whether the model ever received, processed, or responded to the request cannot be determined from durable evidence alone. **Fails closed** — treated with the same caution as an artifact-integrity violation (Section 16), not silently retried and not silently treated as complete.

States (C) and (D) must remain durably distinguishable from state (A) at all times — a future reviewer inspecting only the campaign's own durable artifacts must be able to tell "this trial was never attempted" apart from "this trial was attempted but did not complete," which the current implementation (absent an intent record) cannot do. **No call that may have reached the model may ever be repeated merely because no response was obtained.**

## 6. Fairness statement (extends Section 19, Unit 3-D comparability)

**Frozen, new:** cold-start exposure is structurally concentrated at campaign start (Control's own first warm-up call), not spread evenly across Control and Family A/B/C, given the frozen arm order. A warm-up timeout (Section 3 above) is not directly comparable to, and must not be conflated with, a measured-arm (scored-trial) timeout, whose classification remains unresolved (Section 3 above). **Any future Unit 3-D comparative evaluation must not treat all timeout classes as equivalent absent further governance resolving the scored-trial question.** This amendment does not rank, compare, or express any preference among Control, Family A, Family B, or Family C.

## 7. Experimental invariance — independently verified unchanged

This amendment changes none of: campaign ID structure (`unit3c-remedy-experiments-<YYYYMMDD>`, unchanged); the 483-call schedule (3 warm-up + 145 Control + 220 Family A + 115 Family B + 0 Family C, unchanged); the warm-up count; the Control arm definition; the Family A decision/render mechanism; the Family B candidate prompt or its SHA-256; the Family C deterministic mechanism or its corrected trace (24/29, FP P03/P04/P05/P12, FN R03); the base or supplemental fixture corpus; any expected action; the repetition schedule; the model (`qwen2.5-coder:7b` only) or provider; any prompt candidate text; the artifact root (`/var/lib/parker/reasoning-protocol-live-model`); the 2 GiB disk-space threshold; the safety-checkpoint trigger condition; downstream isolation; the exploratory/qualification/production-selection evidence-tier distinction; the Unit 3-B remedy-family classification (INCLUDED/DEFERRED/EXCLUDED); Unit 3-D's own reserved authority (comparative evaluation, still not begun); or Unit 4's own reserved authority (production implementation, still not begun).

## 8. Prohibited interpretations

This amendment must not be read as: a live-execution authorization (none is granted here); a statement that cold-start loading is proven (confidence remains STRONGLY SUPPORTED, NOT CONFIRMED); a resolution of scored-trial timeout semantics (explicitly, bindingly left unresolved, Section 3); an implementation of intent-before-call durability (not performed here); a change to any fixture, mechanism, repetition count, model, or inference-configuration property (Section 7); a ranking or selection of any remedy family; or an amendment of Unit 3-B, Unit 3-D, or Unit 4 authority.

## 9. Status

```text
PROPOSED — PENDING INDEPENDENT CONSTITUTIONAL REVIEW
```
