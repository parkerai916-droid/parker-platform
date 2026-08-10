**Status:** Unit 3-C Controlled Remedy Experiments — Timeout and Durability Implementation Plan Amendment — **PROPOSED — PENDING INDEPENDENT CONSTITUTIONAL REVIEW.** This document amends specific, named sections of the frozen Unit 3-C Implementation/Execution Plan in place of editing that document directly. It contains no implementation — every requirement below remains, exactly as the original Plan's own Section 17 already stated of itself, "not implemented by this Plan." No live model call, no campaign execution, no code change.

# Unit 3-C Controlled Remedy Experiments — Timeout and Durability Implementation Plan Amendment

## 1. Baseline and authority

Drafted against committed baseline `f5c4dd7636e8f35f61c3320da8c3cb7f75ef8687`. Amends the frozen Unit 3-C Implementation/Execution Plan, Sections 12, 16, 17, and 18 only — every other section remains in full, unamended force. Companion to, and must be read together with, the Timeout and Durability Scope Lock Amendment (`docs/architecture/...`), which this Plan Amendment implements at the design-specification level (never at the code level). Evidentiary authority: the Unit 3-C Timeout and Inference Latency Investigation Review and its Independent Constitutional Review, both committed at `f5c4dd7`.

## 2. Amendment to Section 12 (Model and inference identity, Phase 13) — timeout value

**Original text (preserved, not deleted):** *"...`ModelReasoningProvider`'s `timeoutMs` default of `30_000` ms, unchanged."*

**Amended: the Unit 3-C timeout is 90,000 ms.** Evidentiary basis identical to, and not restated separately from, the Scope Lock Amendment §2 — this Plan Amendment does not re-derive the figure independently; it adopts the Scope Lock Amendment's own frozen value and margin analysis by reference, to avoid two documents stating potentially-divergent justifications for the same number. **Cold-start loading remains STRONGLY SUPPORTED, NOT CONFIRMED** as the cause of Attempt 3's own timeout; this Plan Amendment does not restate it as proven fact.

All other content of Section 12 (model identity, request-body shape, execution-identity capture requirements) is unamended.

## 3. Amendment to Section 16 (Artifact schema, Phase 17) — intent-record and terminal-timeout-observation schema

**Original table (preserved, not deleted) defines the `Unit3CObservation`-equivalent schema for a *completed* trial.** This amendment adds two new, separate schema definitions the original Section 16 did not itself specify at field level, elaborating the requirement Section 17 of the same Plan already stated in prose ("a durable ledger entry recording the intended trial ID") and the requirement Unit 3-B Scope Lock item 20 already binds Unit 3-C to (intent-record discipline "not weaker" than Unit 2-D's).

**New schema: intent record, written before every live model call:**

| Field | Nullable |
|---|---|
| `campaignId` | never null |
| `family` | never null |
| `arm` | never null |
| `fixtureId` | never null |
| `trialSequence` | never null (repetition identity) |
| `expectedAction` | never null |
| `modelName` | never null (Family C is exempt — Family C issues no model call, so no intent record is written for it) |
| `modelDigest` | never null |
| `repositoryCommit` | never null |
| `promptIdentity` | never null |
| `timeoutMs` | never null |
| `inferenceConfigIdentity` | never null |
| `callId` | never null (a stable, unique identifier for this specific call attempt, distinct from `trialSequence`, so that a retried or resumed trial's own separate call attempts remain individually distinguishable in durable evidence even though — Section 5 below — a completed or ambiguous-state call is never automatically retried) |

Written and durably persisted (fsynced) strictly **before** the corresponding HTTP request is transmitted. This record's own presence or absence is what future recovery logic must consult to distinguish state (A) never-transmitted from states (B)/(C)/(D) (Section 5 below) — its absence is definitive proof no call was ever attempted for that trial; its presence is definitive proof one was.

**New schema: terminal timeout observation, written if and only if a call's own intent record exists and no raw observation (original Section 16 schema) was durably persisted within the governed timeout:**

| Field | Nullable |
|---|---|
| `callId` | never null (matches the intent record) |
| `campaignId`, `family`, `arm`, `fixtureId`, `trialSequence` | never null (matches the intent record) |
| `callStartTimestamp` | never null |
| `timeoutMs` | never null |
| `elapsedDurationMs` | never null |
| `terminalClassification` | never null; always exactly the literal value `TIMEOUT` for this schema |
| `responseBytesReceived` | never null; boolean |
| `transportErrorClassification` | never null (e.g., the exception type/class observed, such as `TimeoutCancellationException`) |
| `modelName`, `modelDigest`, `inferenceConfigIdentity` | never null (matches the intent record) |
| `parserResult` | **always null** — a request with no response has nothing to parse; never fabricated |
| `semanticClassification` | **always null** — no output exists to classify as `GOAL`, `REPLY`, `REMEMBER`, or `NOACTION`; never fabricated |
| `checkpointStatus` | never null (e.g., `NOT_CHECKPOINTED` — Section 5 below governs when, if ever, a timeout record may be checkpointed) |

**Frozen, explicit, restated from the Scope Lock Amendment for implementation-level clarity: no future implementation may populate `parserResult` or `semanticClassification` with any of `GOAL`, `REPLY`, `REMEMBER`, or `NOACTION` for a terminal timeout observation.** These four values describe a parsed response; a timeout record has none by definition.

## 4. Amendment to Section 17 (Exact-once and durability design, Phase 18)

**Original text (preserved, not deleted) already states:** *"Intent record before live call: a durable ledger entry recording the intended trial ID is written and fsynced before any HTTP call is issued."* **Independently re-confirmed: the current committed implementation (`Unit3CArmLedger`) does not satisfy this original requirement** — it writes an arm-level `identity.txt` once per arm, not a per-trial intent record before each individual call. This amendment does not weaken or restate the original requirement differently; it makes it field-complete (Section 3 above) and adds the states the original text did not enumerate.

**Amended, extending the original bullet list:**

- *(Original, unchanged)* Intent record before live call; raw result persistence; checkpoint timing; duplicate prevention; crash recovery; raw-without-checkpoint handling; checkpoint-without-raw handling (hard artifact-integrity violation); identity-mismatch handling; rerun prohibition; seal/halt discipline.
- **(New)** **Four-state trial classification, replacing the implicit two-state model:**
  - **(A) Never transmitted** — no intent record exists for the trial ID. Eligible for a first attempt.
  - **(B) Transmitted and completed** — intent record exists and a checkpointed raw observation exists. Complete; per the original rerun-prohibition bullet, never re-issued.
  - **(C) Transmitted and timed out** — intent record exists; a terminal timeout observation (Section 3 above) exists; no raw observation was ever durably persisted for that call ID. **Must not be automatically repeated** within the same campaign run.
  - **(D) Transmitted with ambiguous terminal transport state** — intent record exists; neither a raw observation nor a complete terminal timeout observation exists (e.g., the process crashed mid-request, before either could be durably written). **Fails closed**, treated with the same severity as the original checkpoint-without-raw case: never silently re-run, never silently treated as complete; halts the affected arm pending manual investigation.
- **(New)** States (C) and (D) must remain durably distinguishable from state (A) using only the campaign's own durable artifacts — a future recovery pass or reviewer must never need to consult ephemeral build logs (as Attempt 3 currently requires) to tell "never attempted" apart from "attempted, did not complete."

**Not implemented by this amendment** — exactly as the original Section 17 already states of itself.

## 5. Amendment to Section 18 (Stop conditions, Phase 19)

**Original text (preserved, not deleted)** names measurement-invalidating and remedy-performance failure categories without mentioning timeout at all.

**Amended: a timeout during any governed warm-up trial is added to the measurement-invalidating category**, with the consequence specified in the Scope Lock Amendment §3: durable record, campaign halt (not merely arm halt, given warm-up's own gating position ahead of every arm), no progression, no silent retry, no reclassification as remedy-performance evidence, state preservation, fresh governance required to continue.

**Amended: a timeout during any scored trial is explicitly, bindingly left unresolved** — not added to either the measurement-invalidating or remedy-performance list, and not given any of the "continue / halt arm / halt campaign" treatments a resolved category would receive. **This blocks any future live Unit 3-C campaign attempt from proceeding past the point a scored-trial timeout could first occur**, until a separate governance act resolves it (Scope Lock Amendment §3, restated here for the implementation-facing document).

The existing manual safety-review checkpoint definition (adversarial-category false-positive REMEMBER/GOAL, first occurrence) is unamended and unaffected.

## 6. Experimental invariance — independently verified unchanged

Identical scope to the Scope Lock Amendment §7: this Plan Amendment changes none of the 483-call schedule, warm-up count, Control/Family A/Family B/Family C mechanisms, fixtures, expected actions, repetitions, model, provider, prompt candidates, artifact root, 2 GiB disk-space threshold, safety-checkpoint trigger, downstream isolation, evidence tiers, remedy-family classifications, or Unit 3-D/Unit 4 authority. Independently re-verified: no field added by Section 3 above alters the *original* Section 16 schema's own fields or nullability rules; the two new schemas are additive, not replacements.

## 7. Prohibited interpretations

Identical scope to the Scope Lock Amendment §8, at the implementation-design level: no code is written or changed by this document; no live execution is authorized; cold-start remains an unconfirmed, strongly-supported explanation; scored-trial timeout semantics remain unresolved by design, not by omission; no remedy family is favored, ranked, or selected.

## 8. Status

```text
PROPOSED — PENDING INDEPENDENT CONSTITUTIONAL REVIEW
```
