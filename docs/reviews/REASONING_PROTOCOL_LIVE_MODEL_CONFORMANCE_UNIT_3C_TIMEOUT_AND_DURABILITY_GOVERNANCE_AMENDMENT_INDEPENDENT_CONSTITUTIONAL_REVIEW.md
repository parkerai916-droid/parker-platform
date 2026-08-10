**Status:** Independent Constitutional Review of the Unit 3-C Timeout and Durability Governance Amendment — **ACCEPTED WITH QUALIFICATIONS.** Independently reviews both the Scope Lock Amendment and the Implementation Plan Amendment together, since they are companion documents addressing the same evidentiary basis. No live model call, no HTTP call, no campaign, and no repository mutation beyond this document occurred.

# Unit 3-C Timeout and Durability Governance Amendment — Independent Constitutional Review

## 1. Method

Independently re-read both amendment documents in full against the frozen originals they amend (Unit 3-C Scope Lock Sections 14/16/17/18/20; Unit 3-C Implementation/Execution Plan Sections 12/16/17/18), confirming every "original text preserved" quotation is verbatim, not paraphrased. Independently re-read the Unit 3-B Scope Lock's own item 20 and the Unit 3-C Plan's own Section 17 opening bullet directly from source, not from the amendment's own quotation of them. Independently re-derived the 90,000 ms margin arithmetic a third time (across this task and the two preceding it) rather than accepting it as settled.

## 2. Is 90,000 ms evidence-derived?

Yes, independently re-confirmed by re-computing the margin ratios directly: 90,000 / 48,568 ≈ 1.85×; 90,000 / 27,990 ≈ 3.22×. Both amendments correctly attribute this derivation to the already-`ACCEPTED WITH QUALIFICATIONS` Investigation Review rather than re-deriving it independently within the amendment itself — this is judged the correct choice (Plan Amendment §2's own explicit statement that it "does not re-derive the figure independently... to avoid two documents stating potentially-divergent justifications for the same number") rather than a shortcut, since re-deriving it a second time from the same underlying data would risk producing a subtly different number through transcription drift.

## 3. Is historical latency evidence represented accurately?

Yes. Independently spot-checked three figures against the Investigation Review's own committed table: cold observations (39.92 s, 48.57 s), worst warm observation (27.99 s), and total observation count (26) — all reproduced correctly, with no rounding or restatement drift found.

## 4. Is cold-start confidence overstated anywhere?

**No overstatement found — specifically checked for, not merely trusted.** Independently `grep`-searched both amendment documents for "confirmed," "proven fact," and "proven that": every match is either a negation ("not confirmed," "not stated... as a proven fact") or an unrelated methodological use ("independently re-confirmed" as a review-process phrase, not a claim about cold-start). The exact phrase "STRONGLY SUPPORTED, NOT CONFIRMED" appears intact, unmodified, in both documents, including once in each document's own "prohibited interpretations" section, which is the correct place to guard against future silent upgrading.

## 5. Is warm-up-timeout classification constitutionally justified?

Yes, independently re-derived from the Scope Lock's own existing text rather than accepting the amendment's own reasoning: warm-up's governed purpose (already frozen by the prior warm-up-orchestration correction, unaffected by this amendment) is to establish a stable, measurable baseline *before* any scored trial — a warm-up that cannot complete has not established that baseline, which is a harness/environment-readiness condition, matching the existing measurement-invalidating category's own listed members (e.g., "harness defect," "configuration drift") far more closely than it matches any remedy-performance member (all of which require a parsed semantic output to exist). Independently confirmed no member of the remedy-performance list could apply to an event that produced no output at all.

## 6. Is intent-before-call durability necessary, or is this amendment inventing a requirement?

**Necessary, and independently re-confirmed to already be required by two separate, higher-priority sources the amendment did not invent.** Independently re-read Unit 3-B Scope Lock item 20 directly: "any future experiment driver must implement the same durable ledger/checkpoint/intent-record discipline already implemented for Unit 2-D, not a weaker mechanism" — unconditional, unambiguous, already binding on Unit 3-C before this amendment existed. Independently re-read Unit 3-C's own Plan Section 17 directly: "a durable ledger entry recording the intended trial ID is written and fsynced before any HTTP call is issued" — also already present, also already binding. Independently re-read `Unit3CArmLedger` (`checkIdentity`, `appendObservation`) and confirmed neither writes any per-trial record before a call; only a once-per-arm `identity.txt`. **This is the review's single most significant independent finding**, beyond what either amendment document's own prose already states: the current committed Unit 3-C implementation is not merely "missing a nice-to-have Unit 2-D pattern" (the framing the Investigation Review used) — it has never satisfied its own already-frozen Plan Section 17, nor the cross-unit Unit 3-B item 20 requirement, at any point since the original implementation was accepted through this programme's own Completion/Readiness/Approval chain. Both amendments should be read as *making explicit and field-complete* an obligation that already existed, not creating a new one — and this review recommends that framing be made even more prominent in any future implementation task's own scope description, so it is understood as closing a pre-existing non-conformance, not merely adopting a nice-to-have.

## 7. Is the terminal timeout observation complete without inventing semantics?

Yes, independently checked specifically for the prohibited-fabrication requirement: the Plan Amendment's own schema table marks `parserResult` and `semanticClassification` **"always null"** for a terminal timeout observation, and both amendment documents separately, explicitly prohibit populating either with `GOAL`/`REPLY`/`REMEMBER`/`NOACTION`. Independently confirmed this is stated twice (once per document) rather than only in one, which this review judges appropriately redundant given how easy it would be for a future implementation to default an enum field to some non-null placeholder value under time pressure.

## 8. Is exact-once strengthened, not weakened?

Yes, independently re-traced: the original two-state model (never-transmitted / transmitted-and-completed, implicit in the original Plan's own bullet list) is extended to four explicit states (A/B/C/D), with states (C) and (D) both independently confirmed to receive strictly *more* caution than the original text specified for anything, not less — state (D) is explicitly bound to "the same severity as the original checkpoint-without-raw case," which the original Plan already treats as "a hard, fail-closed artifact-integrity violation." No existing rule (duplicate prevention, rerun prohibition, checkpoint-before-raw ordering) is loosened, relaxed, or removed by either amendment — independently confirmed via a direct diff-style comparison between the original Section 17 bullet list and the amended one.

## 9. Can a transmitted, timed-out call be silently rerun?

**No, independently and specifically checked, since this is exactly the risk this whole amendment exists to close.** State (C)'s own rule is stated in both documents in the same unambiguous terms: "must not be automatically repeated." State (D) is stricter still: "fails closed... never silently re-run." Neither amendment anywhere describes a code path, condition, or exception under which either state would be eligible for automatic re-issue.

## 10. Does scored-trial timeout semantics remain genuinely unresolved, or has a default been smuggled in?

**Genuinely unresolved, independently verified by absence, not by trusting the amendment's own claim of neutrality.** Independently searched both documents for any sentence that could function as a de facto default for a scored-trial timeout (e.g., "unless stated otherwise, treat as X"): none found. Both documents state the same five non-choices explicitly (not remedy failure; not infrastructure failure; not continue; not halt-arm; not halt-campaign) rather than picking one and calling it "unresolved" as a hedge. This is judged a genuine non-decision, not a disguised one.

## 11. Is the unresolved scored-trial question correctly classified as blocking?

**Yes, and this review independently re-derives the same conclusion from first principles rather than accepting the amendment's own reasoning as given.** Unit 3-C's own Section 15 (Adaptive-experimentation prohibition, unamended, independently re-read) already forbids any "run → inspect → tweak → rerun" improvisation and requires stop rules to be pre-registered and frozen *before* live execution. A campaign that reaches a scored-trial timeout with no frozen instruction for what happens next would necessarily require exactly the kind of live, ad hoc, mid-run decision Section 15 already prohibits. Independently confirmed this reasoning is sound and sufficient on its own, independent of the amendment's own restatement of it — this review would have reached "blocking" even without the amendment's own Section 3 argument to that effect.

## 12. Did any experimental mechanism change?

No, independently re-verified via a full re-read of both amendments' own "experimental invariance" sections against the frozen originals: the 483-call schedule, warm-up count, all four arm mechanisms, the fixture corpus, expected actions, repetitions, model, provider, prompt candidates, artifact root, 2 GiB threshold, safety-checkpoint trigger, and downstream isolation are independently confirmed untouched — none of these terms appears anywhere in either amendment's own substantive (non-invariance-statement) sections.

## 13. Did any remedy preference leak in?

No. Independently searched both documents for any comparative or evaluative language about Control, Family A, Family B, or Family C individually (e.g., "performs better," "more reliable," "preferred"): none found. Family B is mentioned exactly once (Investigation Review's own carried-forward caveat that its prompt shape has never been latency-measured) — a stated evidentiary gap, not a preference.

## 14. Did Unit 3-D or Unit 4 authority leak backward?

No. Both amendments' own "prohibited interpretations" sections explicitly disclaim any Unit 3-D or Unit 4 authorization, and independently confirmed neither document contains any comparative-evaluation language, any remedy-selection criterion, or any production-implementation instruction.

## 15. Naming-convention check

Independently searched `docs/architecture/` and `docs/implementation/` for any pre-existing "AMENDMENT"-named document before accepting the chosen filenames: none exists as precedent either way (prior Unit 3-C corrections were reviewed via ICR documents in `docs/reviews/`, with no confirmed prior instance of the underlying correction being applied via a separate amendment file versus a direct in-place edit — the Unit 3-C Scope Lock's own single-commit git history shows no prior in-place edit either). Given no contrary convention was found, and given the task's own explicit preference for "explicit amendment documents rather than silently rewriting frozen historical governance," the chosen names and locations are judged reasonable and consistent with the one relevant repository norm this review could independently verify: original frozen documents are preserved unedited, and corrections/amendments are recorded as separate, dated artifacts.

## 16. Blocking defects

None.

## 17. Non-blocking qualifications

1. Both amendment documents should be read as *closing a pre-existing non-conformance* with Unit 3-C's own already-frozen Plan Section 17 and Unit 3-B's own item 20 (Section 6 above), not merely as adopting a best-practice recommendation — this review recommends any future implementation task's own scope description state this explicitly, so the correction is not under-prioritized relative to other Unit 3-C work.
2. The STRONGLY SUPPORTED / NOT CONFIRMED confidence qualifier on cold-start causation must continue to travel verbatim into any further document (a future implementation task's own Completion Review, for instance) — this review repeats the same caution the Investigation Review's own Independent Constitutional Review already raised, because it is a recurring risk across every stage of this programme's own governance chain, not a one-time check.
3. Family B's own prompt shape remains latency-unmeasured; any future implementation or execution task should not assume the 90,000 ms ceiling's margin analysis (derived from Control-equivalent and Family-A/candidate-track-equivalent prompts) applies with equal confidence to Family B specifically.

## 18. Verdict

```text
ACCEPTED WITH QUALIFICATIONS
```

Both amendment documents are independently confirmed to be evidence-derived, accurately sourced, honestly hedged on cold-start confidence, constitutionally grounded (not merely asserted) on warm-up classification, correctly non-resolving on scored-trial classification, correctly blocking rather than permissive about that non-resolution, strengthening rather than weakening exact-once semantics, free of fabricated response semantics, free of experimental-mechanism drift, and free of any remedy preference or Unit 3-D/Unit 4 authority leakage. The qualifications in Section 17 must travel forward into any future task that acts on this amendment.

## 19. Confirmation

No model or HTTP call occurred during this review. No campaign was created, resumed, or modified. No production, test, or Gradle file changed. No fixture or Family A/B/C definition was altered. No remedy was selected. Neither amendment document was modified by this review.
