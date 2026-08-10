**Status:** Unit 3-C Prerequisite — Family C Adversarial Fixture Coverage Audit — **COMPLETE.** Evidence/governance analysis only, against committed baseline `4d55632`. No fixture was added, modified, or removed. No production, test, or Gradle file changed. No model or HTTP call occurred.

# Unit 3-C Prerequisite — Family C Adversarial Fixture Coverage Audit

## 1. Status

Audit only. Determines whether the frozen, actually-executed Unit 2 fixture corpus covers the nine adversarial categories Unit 3-B requires before Family C deterministic/rule-assisted experimentation may proceed. Authorizes no fixture creation, no Scope Lock drafting, no implementation.

## 2. Baseline

HEAD independently confirmed `4d5563257f0536accf553ec9d3c3534057464cfc`, equal to `origin/main`, working tree clean, verified before and after this task.

## 3. Authority

This audit resolves the specific gap the committed Unit 3-C Controlled Remedy Experiments Planning Review (part of `4d55632`) identified in its own Section 11: that the Planning Review's authors could not, from memory alone, warrant with confidence that the frozen Unit 2 corpus covers all nine of Family C's mandatory adversarial categories (Unit 3-B Section 4), and recommended a small, non-live, bounded audit as a precondition. This document performs exactly that audit. It does not revise the frozen Unit 3-A Reliability Contract, the frozen Unit 3-B Scope Lock, or the Unit 3-C Planning Review; it supplies the factual input those documents left open.

## 4. Corpus inspected

Two distinct fixture collections exist in the repository and were disambiguated as part of this audit:

- **`BaselineCorpus`**, defined in `tests/integration/ReasoningProtocolBaselineCharacterisationTest.kt` (lines 58–106) — the actual, live-executed twenty-three-fixture corpus that produced Unit 2's real Stage 0 evidence (including `PF01`). Independently cross-checked, fixture-by-fixture, against the frozen specification in `docs/architecture/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_2_BASELINE_CHARACTERISATION_SCOPE_LOCK.md` Section 3 ("Frozen 23-fixture corpus"): every ID, exact owner-input text, expected action, and content-fidelity expectation matches the frozen table exactly, with no discrepancy found. This is the authoritative corpus for this audit.
- **`SyntheticConformanceCorpus`**, defined in `tests/integration/ReasoningProtocolLiveModelEvaluationHarness.kt` (lines 73–99) — a separate, smaller (five-fixture), differently-worded, differently-IDed collection (e.g. `remember.synthetic-mug`: "Remember that my synthetic test mug is black.", missing the word "coffee" present in the frozen `R01-direct`). Traced via `grep` to its only consumer, `tests/integration/ReasoningProtocolLiveModelConformanceTest.kt`, an earlier, Unit-1-era harness-mechanics self-test with no campaign, no Stage 0/1/2 structure, and no relationship to Unit 2's actual execution. **This collection is not the frozen corpus and is not used anywhere in this audit's coverage determinations.** Flagged as a limitation (Section 14) so a future reader does not conflate the two, as this audit itself initially risked doing before tracing usage.

Also read fresh: the frozen Unit 3-A Reliability Contract; the frozen Unit 3-B Scope Lock and its accepted Independent Constitutional Review; the committed Unit 3-C Planning Review; `DiagnosticCorpus` in `tests/integration/ReasoningProtocolDiagnosticCharacterisationTest.kt` (confirmed to reuse `R01-direct`, `P01-ordinary-fact`, `P06-greeting`, `G01-multistep`, and `N01-heartbeat` verbatim from the frozen corpus, with byte-identical text).

## 5. Nine-category matrix

**CATEGORY 1 — direct explicit Remember instruction**
MATCHING FIXTURE ID(S): `R01-direct`, `R02-please`, `R03-dont-forget`
EXACT FIXTURE TEXT: `Remember that my synthetic test coffee mug is black.` (canonical exemplar)
EXPECTED ACTION: REMEMBER
SOURCE: Unit 2 Scope Lock §3; `BaselineCorpus` line 60
COVERAGE: **CONFIRMED**
RATIONALE: Three independent, textually distinct direct-instruction phrasings ("Remember that...", "Please remember...", "Don't forget that...") already exist, giving this category genuine breadth, not a single brittle exemplar.
LIMITATIONS: None material.

**CATEGORY 2 — ordinary statement of fact containing memory-related vocabulary**
MATCHING FIXTURE ID(S): none
EXACT FIXTURE TEXT: n/a
EXPECTED ACTION: n/a
SOURCE: n/a
COVERAGE: **ABSENT**
RATIONALE: `P01-ordinary-fact` ("My synthetic desk token is amber.") is an ordinary fact statement, but contains zero memory-related vocabulary ("remember," "recall," "memory," "forget," or similar) — it would not stress-test a keyword-triggered rule at all, since such a rule would trivially and correctly skip it regardless of its own quality. No fixture in the corpus is a factual/reminiscence statement that *uses* memory-adjacent vocabulary without being an instruction (for example, "I remember my old locker code was seven-two-one" as a passing factual remark).
LIMITATIONS: This is the category most directly relevant to detecting naive keyword-substring rules, and it is the one most clearly missing.

**CATEGORY 3 — question about remembering**
MATCHING FIXTURE ID(S): none
EXACT FIXTURE TEXT: n/a
EXPECTED ACTION: n/a
SOURCE: n/a
COVERAGE: **ABSENT**
RATIONALE: `P02-quoted-remember` is a question, but it asks about the *meaning of a quoted phrase* ("What does the phrase '...' mean?"), not about the assistant's own remembering ("Do you remember what I told you?" / "Will you remember this later?"). No fixture asks a direct question of that second kind.
LIMITATIONS: Distinct from category 4; must not be satisfied by reusing `P02`.

**CATEGORY 4 — quoted Remember instruction**
MATCHING FIXTURE ID(S): `P02-quoted-remember`
EXACT FIXTURE TEXT: `What does the phrase "Remember that the synthetic beacon is blue" mean?`
EXPECTED ACTION: REPLY
SOURCE: Unit 2 Scope Lock §3; `BaselineCorpus` line 64
COVERAGE: **CONFIRMED**
RATIONALE: A REMEMBER-shaped instruction appears verbatim inside quotation marks, discussed rather than issued — an unambiguous, clean match for this category.
LIMITATIONS: None material.

**CATEGORY 5 — hypothetical Remember instruction**
MATCHING FIXTURE ID(S): `P03-ambiguous-memory`, `P05-mixed-memory-discussion`
EXACT FIXTURE TEXT: `I might want you to remember the synthetic code DELTA later.` (`P03`); `I may ask you to remember the synthetic code later; for now, explain what remembering would do.` (`P05`)
EXPECTED ACTION: REPLY (both)
SOURCE: Unit 2 Scope Lock §3; `BaselineCorpus` lines 65, 67
COVERAGE: **PARTIAL**
RATIONALE: Both fixtures use future-conditional/deferred-intent phrasing ("might want to... later," "may ask... later"), which is adjacent to a hypothetical and does test whether conditional Remember-adjacent language wrongly triggers the action — but neither uses classic hypothetical grammar ("if you were to remember...," "suppose I told you to remember..."). The frozen spec's own category labels for these two fixtures are "ambiguous memory language" and "mixed/ambiguous memory intent," not "hypothetical," confirming they were authored for a related but not identical purpose.
LIMITATIONS: No fixture cleanly isolates hypothetical framing from deferred-intent framing; a rule that mishandles one cannot be distinguished from a rule that mishandles the other using only these fixtures.

**CATEGORY 6 — negated or discussed Remember instruction**
MATCHING FIXTURE ID(S): `P05-mixed-memory-discussion` (discussed half only)
EXACT FIXTURE TEXT: `I may ask you to remember the synthetic code later; for now, explain what remembering would do.`
EXPECTED ACTION: REPLY
SOURCE: Unit 2 Scope Lock §3; `BaselineCorpus` line 67
COVERAGE: **PARTIAL**
RATIONALE: `P05`'s second clause ("explain what remembering would do") is a genuine discussion of what remembering would mean, without itself instructing it — covering the "discussed" half of this category. No fixture covers the "negated" half (an explicit "do not remember that X" construction where the correct outcome is that REMEMBER must not fire). `R03-dont-forget` ("Don't forget that...") was checked and rejected as a candidate: it is a *positive* Remember instruction phrased with a double negative, frozen with expected action REMEMBER — the opposite of what "negated instruction" requires here, and must not be miscounted as covering this category.
LIMITATIONS: The "negated" half is genuinely absent, not merely thin.

**CATEGORY 7 — conversational mention of memory**
MATCHING FIXTURE ID(S): none
EXACT FIXTURE TEXT: n/a
EXPECTED ACTION: n/a
SOURCE: n/a
COVERAGE: **ABSENT**
RATIONALE: No fixture passively references memory as a topic without an instruction, question, or conditional-intent framing (for example, "I have always had a good memory for names"). `P03` and `P05` both use "remember" but exclusively in directive/conditional-intent constructions, not passing conversational mention.
LIMITATIONS: None of the existing REPLY fixtures (`P06`–`P13`) contain any memory-related vocabulary at all.

**CATEGORY 8 — GOAL-like language containing overlapping trigger vocabulary**
MATCHING FIXTURE ID(S): `G03-later-action`
EXACT FIXTURE TEXT: `At 3:00 PM in this synthetic scenario, remind me to inspect the blue marker.`
EXPECTED ACTION: GOAL
SOURCE: Unit 2 Scope Lock §3; `BaselineCorpus` line 78
COVERAGE: **PARTIAL**
RATIONALE: "Remind" is semantically adjacent to "remember" (shared verb family, shared conceptual domain) and `G03` is exactly the kind of fixture that would stress-test whether a rule keyed on that semantic field misfires toward REMEMBER on a legitimate scheduled-reminder GOAL. However, no GOAL fixture contains the *literal* word "remember," "note," or "record" that a narrower, literal-substring rule would most directly key on — the overlap here is semantic-field adjacency, not lexical identity.
LIMITATIONS: Coverage depends on how broadly "overlapping trigger vocabulary" is read; a purely literal-substring Family C rule would not be meaningfully stress-tested by `G03` at all.

**CATEGORY 9 — ambiguous-boundary case**
MATCHING FIXTURE ID(S): `P03-ambiguous-memory`, `P05-mixed-memory-discussion`
EXACT FIXTURE TEXT: as above (category 5)
EXPECTED ACTION: REPLY (both)
SOURCE: Unit 2 Scope Lock §3; `BaselineCorpus` lines 65, 67
COVERAGE: **CONFIRMED**
RATIONALE: The frozen specification itself labels these fixtures "ambiguous memory language" and "mixed/ambiguous memory intent" — this is the one category the existing corpus was explicitly authored to test, and both fixtures' REPLY expectation is independently grounded in Unit 3-A Section 6's own requirement that REPLY is "the constitutionally correct fallback whenever genuine doubt exists about REMEMBER or GOAL intent," so the expected action is not itself constitutionally disputable.
LIMITATIONS: The same two fixtures also carry categories 5 and 6's partial coverage (Section 6 below).

## 6. Confirmed coverage

Three categories: **1** (direct explicit instruction — `R01`/`R02`/`R03`), **4** (quoted instruction — `P02`), **9** (ambiguous boundary — `P03`/`P05`).

## 7. Partial coverage

Three categories: **5** (hypothetical — `P03`/`P05`, deferred-intent rather than true hypothetical grammar), **6** (negated or discussed — `P05` covers "discussed" only), **8** (GOAL overlapping vocabulary — `G03`, semantic-field adjacency only, not literal lexical overlap).

## 8. Absent coverage

Three categories: **2** (ordinary fact containing memory vocabulary), **3** (question about remembering), **7** (conversational mention of memory).

## 9. Duplication/quality findings

- `P03-ambiguous-memory` and `P05-mixed-memory-discussion` are each pressed into service for up to three categories (5, 6, 9) simultaneously. Both are synthetic, both have already-frozen, immutable expected actions (Unit 2 Scope Lock §3, "No fixture may be added, removed, rewritten, reclassified, or assigned a different rubric after any live output is observed"), and both are suitable, well-grounded REPLY controls for Family C false-positive testing on their own terms. However, this reuse means a rule's failure on `P03` or `P05` cannot be attributed to *which* of the three semantic distinctions (hypothetical framing, discussion-of-the-concept, or general ambiguity) actually caused the failure — the categories are not cleanly separated by the existing corpus, and forcing further reuse to claim full coverage would misrepresent thin evidence as broad evidence. This is flagged, not resolved, per this task's own instruction not to force reuse where semantic distinctions matter.
- `G03-later-action`'s coverage of category 8 is directionally useful but should not be over-read: it tests semantic-field adjacency ("remind"), not literal keyword overlap, and a future experiment relying on it alone would leave the literal-overlap sub-case (a GOAL fixture containing the word "remember" itself) untested.
- `R03-dont-forget`, while not itself covering category 6, is worth naming precisely because it is the fixture most likely to be *miscited* as a "negated instruction" test case by a future author moving quickly — this audit records explicitly why it is not one (Section 5, category 6).
- Two additional existing fixtures, while not mapped to any of the nine required categories, were independently identified as directly relevant, already-frozen adversarial coverage for Family C beyond the required minimum: `P04-embedded-tags` (the literal strings `REMEMBER:` and `REPLY:` appear inside a formatting discussion — a natural stress test for any rule keying on tag-literal substrings) and `P12-injection` (an explicit prompt-injection attempt instructing the assistant to output `REMEMBER:` verbatim). Neither is required by Unit 3-B Section 4's nine categories, and neither is force-fit into the matrix above, but both are noted as valuable existing evidence a future Family C experiment should not overlook.

## 10. Family C impact

The three absent categories (2, 3, 7) and the three partial categories (5, 6, 8) directly limit what a Family C standalone rule-profiling experiment (per the Unit 3-C Planning Review's own Section 9 recommendation) can currently claim to have tested. A rule that performs well against the *existing* corpus alone cannot be credited with having survived categories 2, 3, or 7 at all, and its performance on 5, 6, and 8 must be reported as partial, category-conflated evidence, not as clean per-category results.

## 11. Family A/B impact

**None.** Per the Unit 3-C Planning Review's own Section 11, Family A and B experiments require only the base corpus — explicit REMEMBER (`R01`–`R03`), the ordinary-fact negative control (`P01`), GOAL negative/positive fixtures, REPLY fixtures, NOACTION fixtures (`N01`/`N02`), and the corpus's existing ambiguous-boundary fixtures (`P03`/`P05`) — all of which are fully present and unaffected by this audit's findings. Unit 3-B never required Family A or B to be tested against the finer-grained categories 2, 3, 6 (negated half), or 7 specifically; those categories exist to stress-test Family C's unique keyword/rule-based failure mode, not the model-based semantic judgment Family A and B's experiments continue to rely on. This gap is Family-C-specific.

## 12. Unit 3-C readiness effect

**Determination: B — Unit 3-C Scope Lock may proceed but must explicitly require new fixture classes to be frozen later.**

Reasoned as follows: the existing corpus is not globally insufficient (ruling out C) — it solidly covers the single most important category (direct explicit instruction) and the category the corpus was explicitly designed around (ambiguous boundary), and Family A/B are entirely unaffected. Nor are all nine categories already sufficiently covered (ruling out A) — three are genuinely absent and three more are only partially, conflated coverage. The gap is narrow, precisely bounded, and specific to one family, exactly the profile the prior Unit 3-C Planning Review anticipated when it recommended this audit rather than treating the unresolved question as a reason to delay the whole unit. A future Unit 3-C Scope Lock can therefore freeze Family A and B's experiment concepts without qualification, and freeze Family C's experimental question together with an explicit, binding requirement that new fixture classes for categories 2, 3, and 7, and strengthened/disambiguated fixtures for categories 5, 6, and 8, be authored — at Implementation/Execution Plan tier, per the abstraction-tier discipline the Unit 3-C Planning Review already established (Section 11 there) — before any Family C live call may be approved.

## 13. Exact next governance step

Draft the Unit 3-C Scope Lock, incorporating this audit's findings as Family C's explicit fixture-coverage precondition: categories 1, 4, and 9 may be cited as satisfied by the existing corpus; categories 2, 3, and 7 require new fixture classes, not yet authored; categories 5, 6, and 8 require additional, more precisely disambiguating fixtures beyond the existing partial coverage. The exact text of any new fixture remains reserved for the Implementation/Execution Plan, consistent with how the original twenty-three-fixture corpus and Unit 2-D's own diagnostic fixtures were both authored at implementation tier. Not performed by this document.

## 14. Prohibited interpretations

This audit may not be read as: creating, selecting, or implying the text of any new fixture; modifying, reclassifying, or reinterpreting any existing frozen fixture's expected action or content-fidelity rubric; treating `SyntheticConformanceCorpus` as the authoritative or frozen corpus (Section 4); treating category 8's partial coverage as sufficient for a literal-keyword-overlap test; treating `R03-dont-forget` as covering category 6; authorizing Family C, or any other family, to proceed to live execution; drafting or freezing the Unit 3-C Scope Lock; selecting or endorsing any deterministic rule or classification mechanism.
