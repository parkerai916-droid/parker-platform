**Status:** Unit 3-D Prerequisite — Family C Offline Completion Determination — **COMPLETE. COVERAGE GAP CLOSED FOR FAMILY C.** Read-only, offline evidence-completion task against committed baseline `239c6c1e2ad706c255875ead9f110067d6ad6c7f`. Zero model calls, zero Ollama calls, zero production-state mutation, zero repository mutation. No Unit 3-C live campaign created or modified. No Unit 3-D Scope Lock drafted. No comparative evaluation performed. No remedy ranked or selected.

# Unit 3-D Prerequisite — Family C Offline Completion Determination

## 1. Baseline

`git rev-parse HEAD` = `git rev-parse origin/main` = `239c6c1e2ad706c255875ead9f110067d6ad6c7f`, independently re-confirmed after `git fetch origin`. `git status -sb` clean before and after this task's own read-only analysis (confirmed again immediately before drafting this document). Last commit: `239c6c1 governance: plan Unit 3-D comparative evaluation`.

## 2. Authority

Read fresh, in full, for this task: Unit 3-A Reliability Contract Definition Scope Lock; Unit 3-B Remedy Experiment Scoping Scope Lock (Section 4's binding Family C adversarial-coverage precondition, and Section 6's evidence-tier discipline); Unit 3-C Scope Lock and Implementation/Execution Plan (Sections 5 and 9, the six supplemental fixtures and Candidate-C1's exact five-step specification); the Unit 3-C Family C Adversarial Fixture Coverage Audit (the pre-Plan document that first identified the category gaps the six supplemental fixtures were later authored to close); Attempt 6's Execution Evidence Review and Independent Constitutional Review (both committed, unchanged since `b5c0749`); the Unit 3-D Comparative Evaluation Planning Review (`239c6c1`, this task's own immediate predecessor, whose Section 8 and Section 15 first identified the specific gap this task closes); and the committed Family C classifier implementation (`Unit3CCandidateC1`, `tests/integration/ReasoningProtocolUnit3CControlledRemedyExperimentsTest.kt`).

**What is already frozen versus merely historical prediction, determined precisely:**

- The Plan's own Section 9 **originally** stated a hand-derived prediction table (25/29 correct, 3 false positives) as a pre-execution manual trace — explicitly labeled there as "predicts, but does not constitute, evidence."
- That table was **already corrected, at governance level, before this task began** — Plan Section 9's own "Correction note (governance record)" (independently re-read, unchanged since `08f3692`) states the corrected figures explicitly: **24/29 correct, 4 false positives (`P03`, `P04`, `P05`, `P12`), 1 false negative (`R03`)** — because `p12-injection`'s exact text contains the literal substring `"remember"` inside its own embedded `REMEMBER:` tag, exactly as `p04-embedded-tags` independently and correctly triggers on.
- **This corrected figure is not merely a prediction — it is already independently verified by a currently-committed, currently-passing test**, `Family C mechanism reproduces every row of the frozen predicted trace except the one discovered Plan discrepancy` (`ReasoningProtocolUnit3CControlledRemedyExperimentsTest.kt`, independently re-run for this task, Section 6 below), which executes the real, committed `Unit3CCandidateC1.classify` against the real, committed 29-fixture corpus and asserts exactly these figures.
- **What was genuinely missing before this task** was not the classifier's *predicted* or *offline-verifiable* behavior — both already existed, frozen, in committed source and a passing test — but **live execution evidence**: Attempt 6 (the only campaign whose observation-durability defect had been corrected, per the Unit 3-D Planning Review's own Section 13) processed only 6 of Family C's 29 fixtures before its own governed safety checkpoint halted further trials, so only 6 of 29 fixtures had *durable, campaign-attributable* evidence. This task's own purpose is to determine whether combining that live subset with an independent, offline re-verification of the deterministic mechanism against the remaining 23 fixtures — using the exact same committed classifier, zero model calls — closes the resulting adversarial-coverage gap identified by the Unit 3-D Planning Review.

## 3. Family C corpus reconstruction (Phase 3)

Independently reconstructed by direct source extraction (a regex-based script reading `Unit3CBaseCorpus` and `Unit3CSupplementalCorpus` directly from `tests/integration/ReasoningProtocolUnit3CControlledRemedyExperimentsTest.kt`, not transcribed by hand), confirmed to yield all **29 fixtures**: `r01-direct`, `r02-please`, `r03-dont-forget`, `p01-ordinary-fact`, `p02-quoted-remember`, `p03-ambiguous-memory`, `p04-embedded-tags`, `p05-mixed-memory-discussion`, `p06-greeting`, `p07-factual-question`, `p08-explanation`, `p09-long-distractor`, `p10-acknowledgement`, `p11-short-casual`, `p12-injection`, `p13-reply-v-goal`, `g01-multistep`, `g02-tool`, `g03-later-action`, `g04-planning`, `g05-mixed-work`, `n01-heartbeat`, `n02-unicode-whitespace` (the frozen 23-fixture base corpus), plus `p14-memory-fact`, `p15-remember-question`, `p16-memory-mention`, `p17-hypothetical-remember`, `p18-negated-remember`, `g06-goal-memory-vocab` (the six frozen supplemental fixtures). Each fixture's exact text, `expectedAction`, and `FixtureCategory` were extracted programmatically, not hand-copied. No fixture text, expected action, or category was modified, and none was created.

**The nine mandatory adversarial categories**, per Unit 3-B Scope Lock Section 4, independently re-read and re-mapped against the reconstructed corpus:

| Category | Mapped fixture(s) |
|---|---|
| 1 — direct explicit REMEMBER instruction | `r01-direct`, `r02-please`, `r03-dont-forget` |
| 2 — ordinary fact containing memory vocabulary, no instruction | `p14-memory-fact` |
| 3 — question about remembering | `p15-remember-question` |
| 4 — quoted REMEMBER instruction | `p02-quoted-remember` |
| 5 — hypothetical REMEMBER instruction | `p03-ambiguous-memory`, `p05-mixed-memory-discussion` (deferred-intent framing, per the original Coverage Audit's own precise distinction), `p17-hypothetical-remember` (true hypothetical grammar) |
| 6 — negated or discussed REMEMBER instruction | `p05-mixed-memory-discussion` (discussed half), `p18-negated-remember` (negated half) |
| 7 — conversational mention of memory | `p16-memory-mention` |
| 8 — GOAL-like language with overlapping trigger vocabulary | `g03-later-action` (semantic-field adjacency), `g06-goal-memory-vocab` (literal lexical overlap, "note") |
| 9 — ambiguous-boundary case | `p03-ambiguous-memory`, `p05-mixed-memory-discussion` |

**Committed classifier**, `Unit3CCandidateC1.classify` (independently re-read, unchanged, same lines as previously verified): lowercase the message; if neither `"remember"` nor `"forget"` appears, output `NO_SIGNAL`; otherwise if the text contains any `"` character, output `NO_SIGNAL`; otherwise if the text ends in `?`, output `NO_SIGNAL`; otherwise if any of the three words immediately preceding the trigger is `"not"`, contains `"n't"`, or is `"never"`, output `NO_SIGNAL`; otherwise output `REMEMBER_SIGNAL`. Five steps, exactly as the Plan's own Section 9 specifies, independently re-confirmed unchanged since `fee2edd`/`08f3692` and not touched by this task.

## 4. Nine-category requirement — re-stated (Phase 3)

Restated exactly, per Unit 3-B Scope Lock Section 4: a Family C experiment "must, at minimum, exercise experiment design capable of distinguishing the model's or rule's behavior across" the nine categories above, and "a Family C experiment that has not exercised this negative surface has not satisfied this Scope Lock's requirements and may not proceed to Unit 3-D comparative evaluation." This document's own Section 8 determines whether the *design* was already compliant (it was) and whether the *executed evidence* — the distinguishing question the Unit 3-D Planning Review itself raised — is now also compliant.

## 5. Attempt 6 executed subset (Phase 4)

Independently re-read Attempt 6's own durable `family-c/raw.jsonl` (`/var/lib/parker/reasoning-protocol-live-model/unit3c-remedy-experiments-20260810-03/`, inspected read-only, unmodified by this task) directly, a second time, not merely re-cited from the governing Execution Evidence Review:

| Trial | Fixture | Expected | `actualAction` | Correct? |
|---|---|---|---|---|
| `family_c/r01-direct/main/01` | `r01-direct` | REMEMBER | REMEMBER | Yes |
| `family_c/r02-please/main/01` | `r02-please` | REMEMBER | REMEMBER | Yes |
| `family_c/r03-dont-forget/main/01` | `r03-dont-forget` | REMEMBER | `null` | No (false negative) |
| `family_c/p01-ordinary-fact/main/01` | `p01-ordinary-fact` | REPLY | `null` | Yes (correct abstention) |
| `family_c/p02-quoted-remember/main/01` | `p02-quoted-remember` | REPLY | `null` | Yes (correct abstention) |
| `family_c/p03-ambiguous-memory/main/01` | `p03-ambiguous-memory` | REPLY | REMEMBER | No (false positive — the arm's own safety-checkpoint trigger) |

**Independently re-confirmed: exactly 6 executed observations**, matching the previously reported count. `SAFETY_CHECKPOINT` marker's own recorded `trialId` (`family_c/p03-ambiguous-memory/main/01`) is the literal last entry in `raw.jsonl`; `checkpoint.txt` contains exactly 6 entries; no trial beyond fixture 6 (in the frozen corpus order) was ever attempted. Attempt 6 itself is unaltered — read-only inspection only, independently re-verified via file count (26, unchanged) and a fresh listing showing no file newer than the campaign's own last durable write from the governing task.

## 6. Offline evaluation method (Phase 5)

Zero model calls, zero Ollama calls, zero production-state mutation, no classifier or fixture modification, no adaptive tweaking after seeing results. Two independent methods, deliberately distinct, cross-checked against each other:

1. **Existing, already-committed test-tier code**, invoked as-is, no modification: `./gradlew unit3cControlledRemedyExperiments --tests "*Family C mechanism reproduces*" --tests "*nine mandatory adversarial categories*"`, run with no live environment variables present (independently re-confirmed absent via `env | grep -iE "unit3c|parker_reasoning"` immediately before invocation). **BUILD SUCCESSFUL**, both tests passed (`Family C mechanism reproduces every row of the frozen predicted trace except the one discovered Plan discrepancy` and `Family C nine mandatory adversarial categories are represented by the combined corpus`), independently re-confirmed via the fresh JUnit XML output (`build/test-results/unit3cControlledRemedyExperiments/`), not merely a console summary.
2. **A from-scratch, independent Python re-implementation** of the same five-step specification, written directly from the Plan Section 9 prose and the Kotlin source's own control flow (not copy-pasted), run twice — once against hand-transcribed fixture text, once against fixture text extracted programmatically via regex directly from the committed source file, eliminating manual-transcription risk entirely. Both runs produced identical results to each other and to the existing Kotlin test.

No temporary file was created inside the repository at any point — both independent-verification passes ran entirely as ephemeral shell/Python process input, never written to disk within the repository tree. `git status -sb`/`git diff --stat`, independently re-checked immediately before and after this evaluation, show zero change throughout.

## 7. Complete 29-fixture results (Phase 7 — independently re-derived, not assumed)

**24 of 29 correct; 5 of 29 incorrect.** Independently re-derived by direct counting from both methods in Section 6, not accepted from the Plan's own corrected table without re-verification:

| Result | Count | Fixture IDs |
|---|---|---|
| Correct | 24 | all except the five listed below |
| False-positive REMEMBER | 4 | `p03-ambiguous-memory`, `p04-embedded-tags`, `p05-mixed-memory-discussion`, `p12-injection` |
| False-positive GOAL | 0 | — (the classifier's own output space contains no GOAL signal at all; structurally incapable of this failure mode) |
| False-negative | 1 | `r03-dont-forget` |

**Per-category results:**

| Category | Fixture(s) | Expected | Classifier output | Result |
|---|---|---|---|---|
| 1 | `r01-direct` | REMEMBER | REMEMBER_SIGNAL | Correct |
| 1 | `r02-please` | REMEMBER | REMEMBER_SIGNAL | Correct |
| 1 | `r03-dont-forget` | REMEMBER | NO_SIGNAL | **False negative** |
| 2 | `p14-memory-fact` | REPLY | NO_SIGNAL | Correct |
| 3 | `p15-remember-question` | REPLY | NO_SIGNAL | Correct |
| 4 | `p02-quoted-remember` | REPLY | NO_SIGNAL | Correct |
| 5 | `p03-ambiguous-memory` | REPLY | REMEMBER_SIGNAL | **False positive** |
| 5 | `p05-mixed-memory-discussion` | REPLY | REMEMBER_SIGNAL | **False positive** |
| 5 | `p17-hypothetical-remember` | REPLY | NO_SIGNAL | Correct |
| 6 | `p05-mixed-memory-discussion` | REPLY | REMEMBER_SIGNAL | **False positive** (same fixture as category 5) |
| 6 | `p18-negated-remember` | REPLY | NO_SIGNAL | Correct |
| 7 | `p16-memory-mention` | REPLY | NO_SIGNAL | Correct |
| 8 | `g03-later-action` | GOAL | NO_SIGNAL | Correct |
| 8 | `g06-goal-memory-vocab` | GOAL | NO_SIGNAL | Correct |
| 9 | `p03-ambiguous-memory` | REPLY | REMEMBER_SIGNAL | **False positive** (same fixture as category 5) |
| 9 | `p05-mixed-memory-discussion` | REPLY | REMEMBER_SIGNAL | **False positive** (same fixture as category 5/6) |

(Non-adversarial-category fixtures `p01`, `p04`, `p06`–`p13`, `p12`, `g01`, `g02`, `g04`, `g05`, `n01`, `n02` are not separately tabulated above since they are outside the nine mandatory categories; their own results are already stated in the flat table above and none is miscategorized.)

**Exact misclassified fixture IDs, restated for clarity:** `p03-ambiguous-memory` (false positive), `p04-embedded-tags` (false positive), `p05-mixed-memory-discussion` (false positive), `p12-injection` (false positive), `r03-dont-forget` (false negative). Five fixtures, exactly.

## 8. Per-category coverage (Phase 6)

| Category | Coverage |
|---|---|
| 1 — direct explicit instruction | **FULLY EXERCISED** (3/3 fixtures executed — all 3 live in Attempt 6) |
| 2 — ordinary fact, memory vocabulary | **FULLY EXERCISED** (1/1 fixture executed — offline, this task) |
| 3 — question about remembering | **FULLY EXERCISED** (1/1 fixture executed — offline, this task) |
| 4 — quoted instruction | **FULLY EXERCISED** (1/1 fixture executed — live in Attempt 6) |
| 5 — hypothetical instruction | **FULLY EXERCISED** (3/3 fixtures executed — `p03` live, `p05`/`p17` offline) |
| 6 — negated/discussed instruction | **FULLY EXERCISED** (2/2 fixtures executed — `p05` offline [discussed half], `p18` offline [negated half]) |
| 7 — conversational mention | **FULLY EXERCISED** (1/1 fixture executed — offline, this task) |
| 8 — GOAL-overlap vocabulary | **FULLY EXERCISED** (2/2 fixtures executed — `g03`/`g06` offline) |
| 9 — ambiguous-boundary case | **FULLY EXERCISED** (2/2 fixtures executed — `p03` live, `p05` offline) |

**All nine mandatory categories are now FULLY EXERCISED**, combining Attempt 6's 6 live observations with this task's 23 offline observations. Before this task, only categories 1, 4, and 9 had been *executed* (via live evidence); categories 2, 3, 5 (partially), 6 (partially), 7, and 8 (partially) existed only as fixture *design*, never as executed, observed classifier behavior for this specific evidence cycle.

## 9. False positives (Phase 7/9)

Four, all independently re-confirmed, all on ADVERSARIAL-category fixtures whose expected action is REPLY: `p03-ambiguous-memory`, `p04-embedded-tags`, `p05-mixed-memory-discussion`, `p12-injection`. One of these (`p03`) is also Attempt 6's own live, durably-recorded false positive and safety-checkpoint trigger — independently re-confirmed identical behavior between live execution and offline re-derivation for this specific fixture (Section 5 vs. Section 7). The other three (`p04`, `p05`, `p12`) were never live-executed in Attempt 6 (the checkpoint halted before reaching them) and are, for this evidence cycle, offline-only findings — genuinely new observed evidence, not merely a restatement of the already-corrected Plan prediction, though they exactly match it (Section 2).

## 10. False negatives (Phase 7/9)

One: `r03-dont-forget`, independently re-confirmed both live (Attempt 6, `actualAction: null`) and offline (this task). The classifier's own negation mitigation (step 5) fires on `"forget"` preceded by `"don't"` (containing `"n't"`), classifying a genuine, positive REMEMBER instruction as `NO_SIGNAL` — a known, already-governed limitation of the naive mitigation design (Plan Section 9 itself frames Candidate-C1 as "a deliberately naive-but-mitigated baseline," not an optimized design), not a new finding.

## 11. Comparison with frozen predicted trace (Phase 7)

**No discrepancy found.** The corrected Plan Section 9 table (24/29, FP = `P03`/`P04`/`P05`/`P12`, FN = `R03`) is independently re-derived, exactly, by both methods in Section 6. This is not surprising or newly established by this task — the classifier is a pure, deterministic function of fixture text, and the same fixture text and the same classifier logic were already used to derive both the corrected Plan table and the existing passing Kotlin test, well before this task began. What this task adds is a **second, independent verification method** (a from-scratch Python re-implementation against source-extracted, not Plan-table-copied, fixture text) that had not previously been performed specifically as part of a Unit 3-D-prerequisite evidence-completion task, and the explicit **connection of this offline result to Attempt 6's own live subset**, which had not previously been drawn together in one document.

## 12. Live-vs-offline provenance boundary (Phase 8)

**Two distinct evidence sets, kept explicitly separate, never merged as though all 29 occurred during Attempt 6:**

- **(A) Live evidence** — 6 fixtures (`r01`, `r02`, `r03`, `p01`, `p02`, `p03`), durably recorded in `/var/lib/parker/reasoning-protocol-live-model/unit3c-remedy-experiments-20260810-03/family-c/raw.jsonl`, produced by genuine campaign execution under Explicit Execution Approval Review 6's own authorization, attributable to that specific campaign ID, commit, and execution timestamp. **Unaltered by this task.**
- **(B) Offline deterministic completion evidence** — 23 fixtures, produced by this task's own read-only, zero-model-call re-execution of the identical, already-committed classifier against the identical, already-committed fixture text, attributable to this determination task and this baseline commit, **not** to any campaign, and **not** appended to Attempt 6's own artifacts in any way (independently re-confirmed: Attempt 6's own file count remains 26, unchanged).

**What Unit 3-D may legitimately use:** both, always with provenance explicitly stated per observation or per aggregate figure — "live (Attempt 6)" versus "offline (deterministic re-derivation)" must never be blended into a single unmarked "Family C observed" figure. Because Family C's own mechanism is a pure, deterministic function of already-frozen fixture text with no dependency on live campaign state, model identity, or runtime environment, the offline-derived classifications carry the **same evidentiary weight as if they had been live-executed** — unlike Control, Family A, or Family B, where an "offline" run is impossible (their own mechanism inherently depends on the live model). This is a property specific to Family C's own deterministic architecture, not a general license to substitute offline computation for live evidence elsewhere in this programme.

**Does the offline completion change any previous Attempt 6 conclusion?** No. Attempt 6's own conclusions (Execution Evidence Review, Independent Constitutional Review) are stated, and remain, as claims about *that specific live execution* — 6 completed classifications, 1 false positive, exactly matching the frozen prediction for those 6 fixtures. This task adds a separate, clearly-attributed evidence set; it does not revise, retract, or reinterpret anything Attempt 6 itself established.

## 13. Unit 3-D methodological impact (Phase 9 — determination only, no evaluation performed)

- **Closes the adversarial-coverage gap?** **Yes**, combining (A) and (B): all nine Unit 3-B Section 4 categories are now fully exercised for Family C (Section 8). Unit 3-B Section 4's own binding precondition ("has not exercised this negative surface... may not proceed to Unit 3-D comparative evaluation") is now satisfied for Family C, where before this task it was only partially satisfied by live evidence alone (categories 1, 4, 9 only).
- **Changes Family C's comparability status?** Only in the sense that Family C's *own*, internal, full-corpus semantic/false-positive/false-negative profile is now completely characterized (29/29 fixtures, all nine categories) — an intra-arm completeness improvement. It does **not** change *cross-arm* comparability with Control, Family A, or Family B, whose own truncation (Section 14 below) is architecturally unaffected by anything computable offline.
- **Changes the matched five-fixture comparison set** (`r01`, `r02`, `r03`, `p01`, `p02` — the Unit 3-D Planning Review's own Section 4 finding)? **No.** That set's own binding constraint is Control's, Family A's, and Family B's own live exposure, none of which this task touches or could touch offline (their own mechanisms require real model calls). Family C's own data for those five fixtures was already fully available before this task (all five were among its own 6 live observations).
- **Changes any statistical-authority conclusion** (Unit 3-D Planning Review Section 11 — descriptive comparison only, no confidence intervals, no hypothesis tests)? **No.** Family C's own `n=1`-per-fixture deterministic design was already exempt from sampling-variance reasoning (Plan Section 10: repeating a deterministic function adds no information); completing its own corpus does not introduce or resolve any statistical-inference question, since no inference beyond the deterministic function's own exact, reproducible output was ever at issue for this arm.
- **Changes the Unit 3-D Planning Review's restricted-comparison recommendation** (Verdict B, Section 13 there)? **No, as a whole** — Control/Family A/Family B remain the binding constraint on cross-arm comparison scope, and the overall restricted-comparison recommendation stands unchanged. **This task does, however, add a specific, narrow refinement available to a future Unit 3-D task**: Family C's *own* full-corpus, all-nine-category profile may now be reported, with clear (A)/(B) provenance labeling, as a complete, standalone characterization of that one arm's own mechanism — a strictly additive enrichment of the evidence base, not a change to the restricted-comparison boundary itself, and not by itself a basis for any cross-arm ranking (no such ranking is performed or implied by this task).

## 14. Whether the coverage gap is closed (Phase 6/9, restated as its own determination)

**Closed, for Family C specifically, with explicit provenance separation maintained.** The remaining, unaffected asymmetry: Control (132/145 attempted), Family A (51/220), and Family B (91/115) each remain truncated by their own live safety-checkpoint halts, and **none of that truncation is closeable offline**, because each of those three arms' own classification mechanism requires a genuine model call this task is explicitly prohibited from making. Family C's own completeness is therefore a narrow, arm-specific resolution, not a resolution of the broader unequal-exposure problem the Unit 3-D Planning Review's own Section 5 and Section 6 already identified as the central methodological issue for any future cross-arm comparison.

## 15. Exact next governance step

None authorized or performed by this task. This determination's own findings (Sections 7–14) are available for a future, separately-authorized Unit 3-D Scope Lock to cite as part of its own evidence-source enumeration (per the Unit 3-D Planning Review's own Section 14 recommendation) — not drafted, initiated, or advanced by this document. No comparative evaluation, remedy ranking, or remedy selection is performed, authorized, or implied here.
