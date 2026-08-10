**Status:** Unit 3-C Evidence Completeness and Durability Determination Review — **DETERMINATION COMPLETE.** This is a read-only forensic and governance determination. No code, test, Gradle, or governance document was modified. No Attempt 5 artifact was altered. No live model call occurred. No campaign was rerun.

# Unit 3-C Evidence Completeness and Durability Determination Review

## 1. Baseline

`HEAD` = `origin/main` = `0a4823b8d458ebfba1fab9411fcf8ad8ad4304f3`, clean, independently re-confirmed at task start. Attempt 5's full artifact set (30 files under `unit3c-remedy-experiments-20260810-02/`) independently hashed as this task's own starting record (Section 4), to be re-verified byte-identical at Phase 12.

## 2. Authoritative governance examined

Read fresh for this task: Unit 3-C Scope Lock; Implementation/Execution Plan (specifically Section 16, Artifact schema, and Section 17, Exact-once and durability design); Timeout + Durability Scope Lock Amendment; Timeout + Durability Implementation Plan Amendment; Scored-Trial Timeout Semantics Determination; the fifth-refresh Completion Review/ICR and Readiness Review/ICR; Explicit Execution Approval Review 5; the Execution Evidence Review through Attempt 5; the Execution Evidence Independent Constitutional Review through Attempt 5. Also inspected: Unit 2-D's own `intent.jsonl` precedent (already cited in this programme's prior durability work) and Unit 3-A's Reliability Contract (semantic/representation independence, false-positive zero-tolerance framing).

## 3. Implementation path traced fresh (Phase 3)

Independently re-read `buildModelInvokingExecutor` in full. Exact trace:

1. **Before any call:** `runArm`/`runWarmups` call `ledger.recordIntent(buildIntentRecord(trial, campaignId, identity))` — durably persists campaign/family/fixture/trial identity, **`expectedAction`**, model/digest/commit/prompt-identity/timeout/config identity. This already exists durably, pre-call, for every trial with `makesModelCall == true`.
2. **The call:** `provider.reason(input.request)` returns a `ReasoningProviderResponse` (`Goal`/`Reply`/`Remember`, each carrying a `text: String` field — independently re-confirmed by reading `src/interfaces/ReasoningProvider.kt` directly — or the `NoAction` singleton).
3. **Immediately after:** `classifyResponse(response)` narrows the full response object down to only an `ExpectedAction` enum value, discarding `text` entirely. **The raw response text exists transiently, in memory, inside `response`, for the duration of one function call, and is never captured into any variable that survives past `classifyResponse`.**
4. **Observation construction:** `Unit3CObservation(...)` is constructed with `actualAction`, `semanticCorrect`, `representationValid`, `parserResult`, `parserFailure`, `latencyNanos`, `transportOutcome`, `prompt` (the full prompt **text**, correctly captured) all populated — independently re-confirmed by direct reading, field by field. `rawRequest` and `rawResponse` are **hardcoded `null`** in this constructor call, independent of what actually happened — not merely omitted from persistence, never captured into the object at all.
5. **`encodeObservation(observation)`** — independently re-read: `"campaignId=${observation.campaignId}|family=${observation.family}|fixtureId=${observation.fixtureId}"`. **Exactly three of the ~29 fields on the fully-populated `Unit3CObservation` object are persisted.** Every other field that the constructor step (4) correctly computed — `actualAction`, `semanticCorrect`, `representationValid`, `parserResult`, `parserFailure`, `latencyNanos`, `transportOutcome`, `prompt` — is discarded at this exact step, immediately before the only durable write (`ledger.appendObservation`).
6. **Safety-checkpoint evaluation:** `isAdversarialCategoryFalsePositive(observation.fixtureCategory, observation.expectedAction, observation.actualAction)` — independently re-confirmed this call has full access to the real, complete `observation` object, including `actualAction`, at evaluation time. `checkpoint.trigger(trial.id, "adversarial-category false positive")` — independently re-confirmed the reason string is a **hardcoded literal**, not derived from `observation` in any way. The checkpoint mechanism sees the true `actualAction` for one instant and then durably records only a generic reason string and the triggering trial ID.
7. **After process termination:** only `intent.jsonl` (pre-call identity + `expectedAction`), `raw.jsonl` (campaign/family/fixture only), `timeouts.jsonl` (full terminal-timeout schema, for timed-out trials only), `checkpoint.txt` (an ordered list of trial IDs, nothing else), and `SAFETY_CHECKPOINT` (triggering trial ID + generic reason) survive. Every field discarded at step 5 is gone, with no other durable artifact preserving it — independently re-confirmed by direct inspection of every file type Attempt 5 actually produced (Section 4).

## 4. Attempt 5 artifact inventory (independently re-hashed for this task)

30 files under `unit3c-remedy-experiments-20260810-02/`. Full inventory and hashes captured at Phase 1 (Section 1) for later byte-for-byte re-verification. Per-arm counts independently re-derived via a fresh Python script: warm-up 3 intent/3 raw/0 timeout (SEALED); Control 92 intent/89 raw/3 timeout (SAFETY_CHECKPOINT); Family A 52/52/0 (SAFETY_CHECKPOINT); Family B 29/29/0 (SAFETY_CHECKPOINT); Family C 0 intent (by design)/6 raw/0 timeout (SAFETY_CHECKPOINT).

## 5. Independent derivation of the "172 of 173" claim (Phase 4)

```text
B — directionally correct but numerically inaccurate
```

**Derivation, shown in full.** The Execution Evidence Review's own Section C8 table defines "173 completed observations" as the sum of warm-up (3) + Control (89) + Family A (52) + Family B (29) = 173 — this population **excludes Family C's own 6 completed classifications entirely**. The same review's Section C16 then describes Family C's own checkpoint-triggering observation as "the exceptional observation... interpretable," implicitly treating it as if it were one of the 173 — **it cannot be, since it was excluded from that sum by construction.** This is a genuine internal inconsistency, independently found by this task, not by the original review's own self-check.

**Corrected, self-consistent figures, independently re-derived:**

- Total completed observations across the entire campaign: **179** (173 LLM-based + 6 Family-C deterministic).
- Of the 173 LLM-based completions: **170 are fully uninterpretable** from durable evidence (no information beyond "this trial ID completed, for this campaign/family/fixture"). **3 are partially interpretable**, not from the durable payload itself, but by logical deduction from the safety checkpoint's own known trigger condition applied to the one durably-recorded fact that a checkpoint fired for that specific trial ID: Control's own trigger (`control/g03-later-action/main/02`, fixture category GOAL, expected action GOAL) is narrowed to exactly `REMEMBER` (the only value satisfying `actualAction ∈ {REMEMBER, GOAL} ∧ actualAction ≠ GOAL`); Family A's and Family B's own triggers (both on `p03-ambiguous-memory`, category ADVERSARIAL, expected REPLY) are narrowed only to `REMEMBER or GOAL`, not resolvable further.
- Of the 6 Family-C completions: **all 6 are fully, definitively re-derivable** — independently re-confirmed by this task via direct re-execution (Section 6) of the exact, frozen, deterministic classifier against the exact, frozen fixture text, not by inference.

**The original claim's core thesis — that the overwhelming majority of this campaign's evidence is semantically opaque from durable data alone, with very limited exceptions — is directionally sound.** The specific figure "172 of 173" is not; the accurate figures are **170 of 173 (LLM-based) fully opaque, 3 of 173 partially resolvable by deduction, and 6 of 6 (Family C) fully resolvable by exact re-derivation** — a materially different and more precise picture than the original two-number summary conveyed.

## 6. The exceptional/resolvable observations, explained precisely

**Family C (all 6, not just 1):** `Unit3CCandidateC1.classify` is independently re-confirmed, by direct re-reading, to be a pure function of `ownerMessage` text alone — no model call, no randomness, no external state. Independently re-executed the exact five-step algorithm (lowercase, trigger-word search, quote check, question-mark check, three-preceding-words negation check) against the six actual fixture texts Attempt 5 attempted, in a fresh, standalone script, for this review:

```text
r01-direct            -> REMEMBER_SIGNAL  (actualAction=REMEMBER, expected REMEMBER: correct)
r02-please             -> REMEMBER_SIGNAL  (actualAction=REMEMBER, expected REMEMBER: correct)
r03-dont-forget        -> NO_SIGNAL        (actualAction=null, expected REMEMBER: the known false negative, R03)
p01-ordinary-fact      -> NO_SIGNAL        (actualAction=null, expected REPLY: correct)
p02-quoted-remember    -> NO_SIGNAL        (actualAction=null, expected REPLY: correct)
p03-ambiguous-memory   -> REMEMBER_SIGNAL  (actualAction=REMEMBER, expected REPLY: the known false positive, P03 — the checkpoint trigger)
```

This is **re-derivation of an already-frozen, already-governed, deterministic function's exact output**, not inference and not an assumption — the same input, run through the same already-committed code, produces the same output every time, verifiably, right now. It independently, exactly reproduces the corrected Family C trace analysis's own prior prediction (24/29 correct; false positives `P03`/`P04`/`P05`/`P12`; false negative `R03`) for the specific 6 fixtures Attempt 5 happened to reach before its own checkpoint fired.

**Control/Family A/Family B (3 of 173, partial only):** These are LLM outputs. No deterministic function reproduces them. The 3 partial deductions rest entirely on the *already-verified correctness of the trigger condition's own code*, applied to the *fact that a checkpoint fired*, not on any reconstruction of what the model actually said.

## 7. Recoverability matrix (Phase 5)

Strict rule applied throughout: known input + missing output does not permit reconstruction of the output; expected action does not prove actual action; a checkpoint trigger proves only what it durably records.

| Missing evidence category | Classification | Basis |
|---|---|---|
| `actualAction` for Control/Family A/Family B's 170 non-triggering completions | **IRRECOVERABLE FROM EXISTING ARTIFACTS** | LLM output never captured in any durable form; no deterministic function to re-run |
| `actualAction` for Control/Family A/Family B's 3 triggering completions | **PARTIALLY RECOVERABLE** | Narrowed (not determined) by the checkpoint's own known, already-verified trigger logic |
| `actualAction` for Family C's 6 completions | **DERIVABLE WITHOUT INFERENCE** | Deterministic, frozen, pure function; exact re-execution reproduces the true value, independently confirmed |
| `semanticCorrect`, `parserResult` for the 170 | **IRRECOVERABLE FROM EXISTING ARTIFACTS** | Depends entirely on the unrecoverable `actualAction` |
| `representationValid` for all 173 LLM-based completions | **IRRECOVERABLE FROM EXISTING ARTIFACTS** | Never durably persisted; not derivable from `expectedAction` or any other durable field |
| `contentFidelity` for all 173 | **NOT REQUIRED FOR THE GOVERNED QUESTION, AS CURRENTLY SCOPED** | Independently re-confirmed hardcoded `null` in `buildModelInvokingExecutor`'s own construction regardless of whether a fixture defines `expectedContent` — never even computed in memory, a separate, pre-existing gap from the durability question this task examines, out of scope here |
| `latencyNanos` for the 173 completions | **IRRECOVERABLE FROM EXISTING ARTIFACTS** | Computed in memory, never persisted; the 3 timed-out trials' own elapsed time *is* durably recorded (in `timeouts.jsonl`), an inconsistency worth noting: the same numeric field is durably captured for failures but not for successes |
| `prompt`/`rawRequest`/`rawResponse` text for the 173 | **IRRECOVERABLE FROM EXISTING ARTIFACTS** (`prompt`) / **NEVER COMPUTED AT ALL** (`rawRequest`, `rawResponse`, both hardcoded `null` even in memory) | `prompt` existed transiently and was discarded; the other two never existed as populated values anywhere, even before persistence |
| `expectedAction` for every trial (all arms) | **DURABLY PRESENT** | In `intent.jsonl` for every trial with `makesModelCall == true`, independently re-confirmed via direct inspection |
| Campaign/family/fixture/trial identity for every trial | **DURABLY PRESENT** | In `raw.jsonl`/`intent.jsonl`/`timeouts.jsonl` uniformly |
| Model/digest/commit/timeout/endpoint identity | **DURABLY PRESENT** | In every arm's own `identity.txt`, independently re-confirmed identical across all five arm directories and matching the authorized configuration exactly |
| Timeout-specific detail (elapsed time, classification, transport detail) | **DURABLY PRESENT** | Full governed terminal-timeout schema, independently re-confirmed present and complete for all 3 Control timeouts |

## 8. Minimum sufficient future evidence schema (Phase 6)

Not implementation — a determination of what should be persisted, and why, for a future corrective task to build against.

| Field | Governed question it supports | Already computed in memory today? | Behavioral change if persisted? | Privacy/security | Raw text necessary? |
|---|---|---|---|---|---|
| `actualAction` | Semantic correctness, false-positive/negative tracking (Unit 3-A Reliability Contract; Plan §16, already required) | Yes | None — pure evidence preservation | None (synthetic fixtures only) | No — a small enum value suffices |
| `semanticCorrect` | Same | Yes (trivially derivable from `actualAction`) | None | None | No |
| `representationValid` | Representation/semantic independence (Unit 3-A §6; Plan §17) | Yes | None | None | No — boolean |
| `parserFailure` | Diagnosing representation failures | Yes | None | None | A short message string is sufficient; full stack trace not required |
| `transportOutcome` | Distinguishing transport-classified outcomes from clean completions | Yes | None | None | No |
| `latencyNanos` | Timeout-value calibration (directly useful for exactly the kind of analysis the Timeout and Inference Latency Investigation Review had to reconstruct from Unit 2/Unit 2-D's own evidence instead) | Yes | None | None | No — a number |
| `prompt` | Forensic/qualitative review of *why* a false positive occurred; not required by any currently-governed comparison question | Yes (transiently) | None if persisted; a real, if modest, storage-volume increase across 483 calls | None currently (synthetic fixtures) — general principle noted for any future non-synthetic use | Arguably useful but **not required** for the governed questions; could be persisted at reduced verbosity (hash + length) if storage is a concern |
| `rawResponse`/response `text` | Same forensic value as `prompt`; **currently not even captured in memory**, so persisting it requires a code change at the classification step, not only at the encoding step | **No — would need to be captured for the first time** | Requires changing `classifyResponse`'s own narrow return type or capturing `response.text` before narrowing | Same as prompt | Same reasoning as `prompt` — valuable, not strictly required |

**Recommendation, as a determination only:** the governed Unit 3-A/Unit 3-C/Unit 3-D questions (semantic correctness rate, representation validity rate, false-positive/negative rate, completion rate, timeout rate) are **fully supportable by structured fields alone** (`actualAction`, `semanticCorrect`, `representationValid`, `parserFailure`, `transportOutcome`, `latencyNanos`) — none of them require raw prompt/response text. Raw text has separate, real forensic value (understanding *why*, not just *what*) but is not itself a governed requirement today. **Minimum sufficient persistence = the structured fields `Unit3CObservation` already computes today, minus raw text** — i.e., fixing `encodeObservation` to serialize the fields the Plan's own Section 16 already requires, without necessarily adding prompt/response capture as well (a separate, larger, and currently non-required change).

## 9. Defect / governance classification (Phase 7)

```text
A — implementation defect against an already-frozen requirement
```

**Traced to its authoritative source, independently re-confirmed, not assumed.** The Unit 3-C Implementation/Execution Plan's own Section 16 (Artifact schema, Phase 17), frozen since this Plan's own original acceptance, explicitly and unambiguously specifies a full ~29-field schema including `actualAction`, `semanticCorrect`, `representationValid`, `prompt`, `rawResponse`, `parserResult`, `latency`, and `transportOutcome`, with explicit per-field nullability rules — independently re-read in full for this task (Section 2). `Unit3CObservation` (the in-memory type) **already, correctly implements this exact schema** — independently re-confirmed field-by-field in Section 3. The defect is confined entirely to `encodeObservation`, the one function responsible for turning an already-correct in-memory object into a durable record, which persists 3 of ~29 fields. **This is not a case of governance failing to specify a requirement that evidence then couldn't meet — governance specified the requirement correctly, in full, from the start, and one specific function never fulfilled it.**

This is independently judged **not** classification F (evidence limitation only, no defect) — the "limitation" is not an inherent property of what's knowable or preservable; the missing fields were already correctly computed and simply never written down. It is **not** classification C or D (Scope Lock/Plan defect) — neither document requires correction; the Plan's own schema is already correct. It is properly **A**, with **B** (incomplete implementation) as an accurate, compatible secondary description of the same fact.

**Why this was never caught across five prior review cycles, independently assessed:** every prior Completion Review, Completion ICR, Readiness Review, and Readiness ICR in this programme's history verified `raw.jsonl`'s own *existence*, *trial-ID correctness*, and *count* extensively — dozens of tests check line counts, trial-ID presence, absence of duplicates — but independently searched, and found, no test anywhere in this codebase that ever asserted the *payload content* of a `raw.jsonl` record against Plan Section 16's own schema table. This is the same category of gap this programme has now found five times (P12's trace, warm-up wiring, the live trigger, the disk-space gate target, and now this): a component correct in one respect (existence, structure, count) was never checked against a *different*, also-governed property (payload completeness) it was also required to have.

## 10. Attempt 5 evidential-value matrix (Phase 8)

| Question | Classification | Basis |
|---|---|---|
| Live reachability (Gradle → trigger → entry point → orchestration → HTTP call) | **SUPPORTED** | 176 real calls genuinely transmitted; the full path is now proven under real conditions, not only fakes |
| Model identity (`qwen2.5-coder:7b`, digest) | **SUPPORTED** | Every `identity.txt` durably records and matches the authorized digest |
| Timeout behavior (90,000 ms ceiling, classification, continuation) | **SUPPORTED** | 3 genuine `MODEL_TIMEOUT` events, durably recorded in full, with `ARM_CONTINUED` independently confirmed to have actually happened (later Control trials completed) |
| Transport reliability | **SUPPORTED (as "zero failures observed")** | 0 transport/provider failures and 0 ambiguous states occurred; the classification mechanism itself was never exercised on a genuine transport-failure case, so its own correctness for *that* case remains proven only by offline tests, not by this attempt |
| Completion rates | **SUPPORTED** | Durably, exactly countable per arm |
| Exact-once behavior | **SUPPORTED** | Zero duplicates, zero raw/timeout overlap, independently re-verified twice across two separate reviews |
| Durability behavior (intent-before-call, terminal timeout records) | **SUPPORTED** | Independently re-traced and confirmed working correctly under real conditions — ironically, the *timeout* durability mechanism this attempt was specifically built to validate performed exactly as governed; it is the unrelated, pre-existing *observation* durability mechanism that has the gap |
| Safety-checkpoint behavior | **SUPPORTED** | Four independent, genuine, first-occurrence halts, independently re-verified via last-record-in-sequence checks |
| Family C P03 false positive | **SUPPORTED** | Independently re-confirmed by direct re-execution (Section 6), not merely by the checkpoint firing |
| Existence of additional false-positive checkpoint events | **SUPPORTED (existence only)** | Three further genuine trigger events (Control, Family A, Family B) durably confirmed to have occurred; **their own precise nature is only PARTIALLY SUPPORTED** (narrowed to REMEMBER specifically for Control, REMEMBER-or-GOAL for Family A/B) |
| Semantic correctness rates | **NOT SUPPORTED** | 170 of 173 LLM-based completions carry no durable semantic information at all |
| Representation validity rates | **NOT SUPPORTED** | Never durably persisted for any LLM-based completion |
| Content-fidelity rates | **NOT APPLICABLE** | Never computed at all, for any trial, regardless of this durability gap (Section 7) |
| Relative remedy performance | **NOT SUPPORTED** | Requires semantic correctness data this attempt cannot provide for Control/Family A/Family B; would additionally require Unit 3-D's own comparative methodology, not exercised or authorized here |

## 11. Unit 3-D readiness determination (Phase 9)

```text
B (for a restricted subset of operational questions) combined with D (a fresh campaign will ultimately be required for semantic comparison, but only after the durability defect is corrected first)
```

**Not chosen in advance; derived from Sections 9–10 above.** Distinguishing the two questions Phase 9 requires distinguishing:

1. **Must the durability mechanism be corrected?** Yes — independently classified as a genuine implementation defect against already-frozen governance (Section 9), not a hypothetical nice-to-have.
2. **Is another expensive live campaign actually necessary?** **Not immediately, and not for every purpose.** Attempt 5's own evidence already fully supports the *operational* questions (Section 10: reachability, timeout behavior, exact-once, safety-checkpoint behavior, completion rates) — a rerun is not needed to establish any of those; they are already, durably, independently proven. A rerun **is** necessary specifically to obtain *semantic* comparison evidence for Control/Family A/Family B (the evidence Unit 3-D's own comparative purpose most likely needs most), but **running one now, before `encodeObservation` is corrected, would reproduce the identical gap and waste the campaign** — any future rerun must be sequenced strictly after a corrective implementation task, not before or in place of one.

Unit 3-D **may not** proceed on the assumption that Attempt 5 provides semantic comparison evidence — it does not. Unit 3-D **could**, in principle, be separately scoped to examine only the operational dimensions Attempt 5 does support, but whether that constitutes a meaningful or worthwhile Unit 3-D exercise on its own is a Unit-3-D-scoping question this determination does not decide.

## 12. Unresolved questions

Whether a corrective task should also capture raw prompt/response text (Section 8's own noted, non-required option) or confine itself to the already-governed structured fields; whether `contentFidelity`'s own separate, pre-existing non-computation (Section 7) warrants its own future determination; whether Unit 3-D should be scoped narrowly (operational-only) or deferred entirely pending a corrected rerun; the exact governance vehicle for authorizing the corrective implementation task (a Plan amendment is not needed, since the Plan is already correct — likely a direct implementation task citing this determination, analogous to how the timeout/durability implementation task cited its own governing amendments).

## 13. Exact next governance step

A narrowly-scoped implementation task, citing this determination as its authority, to correct `encodeObservation` (and, if separately authorized, the intent/timeout schemas' own already-correct pattern extended to raw.jsonl) so that `raw.jsonl` durably persists the full, already-governed Plan Section 16 schema — followed by this programme's own full Completion → Completion ICR → Readiness → Readiness ICR → Explicit Execution Approval Review chain, exactly as every prior Unit 3-C correction has required, before any further live-execution attempt. This task does not perform, authorize, or schedule that implementation.

## 14. Determination verdict

```text
DETERMINATION COMPLETE — CLASSIFICATION A (IMPLEMENTATION DEFECT AGAINST ALREADY-FROZEN GOVERNANCE) — CORRECTIVE IMPLEMENTATION REQUIRED BEFORE ANY FUTURE CAMPAIGN — NO IMMEDIATE RERUN REQUIRED FOR OPERATIONAL QUESTIONS ALREADY SUPPORTED BY ATTEMPT 5
```
