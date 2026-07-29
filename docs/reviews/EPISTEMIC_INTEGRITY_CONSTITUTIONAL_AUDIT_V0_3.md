**Status:** Archived Constitutional Audit — preserved for constitutional history. This audit reviewed Draft Version 0.3 and its findings were resolved in Version 0.4 / ratified as Version 1.0 (`docs/architecture/epistemic-integrity.md`).

---

# Constitutional Audit of Amendment No. 1 — Epistemic Integrity, Version 0.3

**Reviewing body:** independent constitutional audit, conducted as if this were a final review before permanent ratification. This document does not redraft the amendment. Where a fix is described, it is described only well enough to show why the finding is material; drafting is deferred to a future amendment cycle, and only if Steven asks for one.

---

## 1. Executive Constitutional Opinion

Version 0.3 is architecturally sound and internally non-contradictory at the level of its overall structure: the separation of reasoning from representation, the evidential-state taxonomy, the evidence-handling cluster (provenance, integrity, contemporaneity, weight), and the temporal-integrity/revision/correction cluster all fit together without doctrinal conflict, and every cross-reference between articles was independently re-checked against the current numbering and found correct.

That said, this audit finds four issues serious enough that they would weaken the Constitution's enforceability over the "many years" the audit principles ask this document to be judged against, and a further set of narrower structural and editorial issues. None of these require rearchitecting the amendment. All of them are closeable by narrow, targeted amendment. **This audit does not recommend ratifying Version 0.3 as a permanent, unamendable Version 1.0 in its present form.** It recommends one further narrow amendment cycle limited to the Tier 1 items identified in Section 10, followed by ratification. This is a recommendation to finish, not to keep building: no new constitutional doctrine is proposed anywhere in this audit.

---

## 2. Article-by-Article Constitutional Review

For each article: necessity, enforceability, internal coherence, testability, and scope.

**Article I — Definitions.** Necessary and appropriately scoped. Two coherence problems: (a) "material representation" is gated on whether "a reasonable recipient would rely upon" it — an unoperationalised standard with no test requiring a materiality determination to be justified or disclosed (see Loophole 1, Section 4); (b) "proposition" is defined so broadly (anything "capable of being assessed... and assigned an evidential state," where *Unknown* and *Indeterminate* are themselves available states) that the definition excludes almost nothing. Neither defect makes the article unenforceable, but both weaken it as the gatekeeping layer for everything downstream.

**Article II — Principle of Epistemic Integrity.** Necessary as a headline statement. Substantially restated by Article III and the closing Foundational Principle (see Section 3). Testable via CT-EI-01/04.

**Article III — Representation Rather Than Absolute Truth.** Sound and testable. Overlaps with Article II (see Section 3) but adds the proposition-framing cross-reference to Article V, which is not redundant.

**Article IV — Evidential Representation.** Constitutionally necessary — this is the taxonomy the rest of the amendment depends on — but under-specified in a way that matters: the fourteen states are named and ordered but none is given necessary or sufficient conditions. The article asserts the states "shall not be represented interchangeably" without supplying the content needed to tell them apart in a disputed case. This is the amendment's single largest testability gap, because Article VII now hangs a bright-line rule (discharge of the burden of justification) directly on one named state ("evidentially supported conclusion") that is itself undefined.

**Article V — Propositional Integrity.** Sound in structure, including its anti-evasion clause. Gap: disclosure obligations attach only where Parker *reformulates* a proposition. Where Parker identifies an embedded assumption or ambiguity but elects not to reformulate, nothing in the article requires that finding to be surfaced to the recipient (see Loophole 6).

**Article VI — Evidential Sufficiency.** Sound and correctly cross-referenced to Articles V, VII, and XI. Gap: no dedicated constitutional test group exists for this article's central rule (no representation of exclusivity absent justification); see Section 7.

**Article VII — Burden of Justification.** The most consequential new article and, on audit, the one requiring the most care. It is coherent and correctly cross-referenced, but its central operative sentence — that a proposition discharges its burden only at "evidentially supported conclusion or stronger" — inherits Article IV's definitional gap: "stronger" is only "stronger" because of list position, and the ordinary-language reading of "reasoned *conclusion*" (which sits, by list order, *below* "evidentially supported conclusion" and therefore does not discharge the burden) cuts against its plain English meaning. This is a genuine, foreseeable point of dispute (see Loophole 7).

**Article VIII — Provenance and Evidential History.** Sound, testable, correctly scoped.

**Article IX — Integrity of Evidence.** Sound in substance. Two of its listed preservation items — "hashes" and "version history" — are more implementation-flavoured than the rest of the amendment's vocabulary; see Section 6.

**Article X — Contemporaneity.** Sound, detailed, internally consistent with Article IX's generalised integrity/authenticity/accuracy/truth distinction.

**Article XI — Evidential Weight and Independence.** Sound. Contains a dormant escape clause — "unless an applicable governing rule expressly requires it" — that no current rule in the amendment invokes. Not presently exploitable, but worth flagging as a latent seam (see Section 4, Loophole 13 note in the punch list).

**Article XII — Negative Evidence.** Sound, well-integrated with Article VII.

**Article XIII — Transparency of Uncertainty.** Sound.

**Article XIV — Confidence.** Sound; correctly tied to both Article IV and the new Article VII.

**Article XV — Constitutional Separation of Reasoning and Representation.** Sound as far as it goes, but it presupposes an executing process — something that actually performs "constitutional review" — that this amendment never names and does not require to exist. This is very likely intentional deference to the base Parker Constitution's general authority mechanism (consistent with how Article XIX defers enforcement to "the Constitution's general provisions"), and is not treated here as a defect in this amendment, but it is a real dependency this amendment relies on without confirming.

**Article XVI — Temporal Integrity.** Sound.

**Article XVII — Revision of Knowledge.** Sound; correctly cross-referenced to Article XVI without restating it.

**Article XVIII — Correction of Error.** Sound. Its ten-item list of failure causes is not marked as illustrative, unlike Article XIX's list (see Section 3, list-exhaustiveness finding).

**Article XIX — Future Capabilities.** Sound, and the best-drafted article from a durability standpoint: it explicitly marks its own subsystem list as "illustrative and not exhaustive," which is the pattern the rest of the amendment should follow (see Section 4, Loophole 4).

No article was found to be constitutionally unnecessary, impossible to implement, or impossible to test in principle. The defects identified are under-specification and disclosure gaps, not structural failures.

---

## 3. Internal Consistency Review

**Duplicated principle, undeclared hierarchy.** The core rule — represent only what the evidence justifies, preserve provenance/integrity/temporal context, disclose uncertainty — is stated independently in the Rationale, Article II, Article III, and the closing Foundational Principle. Restating a constitution's core commitment at different points is normal (compare a preamble to its operative text), but this amendment never states which version controls if a future edit updates one restatement and not the others, or if the broader Foundational Principle language and a narrower Article's rule are read to diverge in a specific case. This is a genuine gap, not merely a style question: a document meant to last "many years" needs an explicit interpretive-priority clause. This is Tier 1 in Section 10.

**Undefined operative term.** Article VI uses "material conclusion," which is not among the defined terms in Article I (which defines "material representation" and "material proposition," but not "material conclusion"). It is almost certainly intended to mean "material proposition," but as written it is a third, undefined category sitting in operative text.

**Inconsistent terminology.** Article IV's opening line ("represented according to its actual evidential *status*") and its own defined term ("evidential *state*," Article I) are used as apparent synonyms without either being declared equivalent to the other. Minor, but exactly the kind of loose thread a literal-minded implementation could exploit by treating "status" as something other than "state."

**Duplicated constitutional test.** CT-EI-26 (Provenance: "distinguish independent corroboration from repetition derived from a common source") and CT-EI-46 (Evidential Weight and Independence: "distinguish independent corroboration from repeated accounts sharing a common origin") test the same substantive question twice under two different headings. Independence is operationally defined only once, in Article XI §2; the Provenance-group copy should be retired.

**List-exhaustiveness inconsistency.** Article XIX marks its own enumeration as "illustrative and not exhaustive." No other enumerated list in the amendment does this — including, most importantly, Article VII's list of factors that do not discharge the burden of justification (absence of contrary evidence, institutional authority, repetition, default assumption, common belief, existing narrative). Under the ordinary interpretive canon *expressio unius est exclusio alterius* (to state one thing is to exclude another), a literal reader could argue that any non-justification not on that list — novelty, brevity, aesthetic appeal, whatever a future dispute produces — is not excluded by Article VII and could therefore be argued to satisfy it. One general clause ("enumerations in this Amendment are illustrative and not exhaustive unless expressly stated to be exclusive") would resolve every instance of this at once, rather than needing to append "including, without limitation" to a dozen separate lists.

**No circular reasoning found.** Article VII's dependency on Article IV, Article IV's revision trigger pointing to Articles XVI–XVII, and Article XVII's cross-reference back to Article XVI form a directed chain, not a cycle. Article VIII and Article IX cite each other, but do so to mark adjacent, non-overlapping scope (origin history vs. object fidelity), not to define one in terms of the other. Both patterns were checked specifically and neither is circular.

**No conflicting definitions or conflicting authority found**, aside from the undeclared hierarchy between the Foundational Principle and the Articles noted above, and the dormant override clause in Article XI (which conflicts with nothing at present because it has never been invoked).

---

## 4. Constitutional Loophole Assessment

Numbered in descending order of seriousness; consequence stated for each; amendment recommended only where the loophole is genuinely material.

1. **Materiality self-classification.** Every obligation in the amendment is gated on a representation being "material," and materiality is defined by an unoperationalised, self-judged standard ("a reasonable recipient would rely upon it"). A reasoning provider under pressure to avoid disclosure, provenance, or uncertainty obligations could simply classify the relevant representation as non-material, and no test in the suite requires that classification to be justified or reviewable. **Consequence:** the entire amendment's applicability gate is presently unaudited. **Recommend amendment.**

2. **Undisclosed reliance on "reasonably practical" / "reasonably available."** These qualifiers appear throughout Articles VIII, IX, and elsewhere, softening near-absolute duties into conditional ones — correctly, since an implementation-neutral constitution cannot demand the impossible. But nothing requires Parker to disclose that it invoked the exception, or why, when doing so affects a material representation. A subsystem can satisfy the letter of "preserve provenance wherever reasonably practical" by silently deciding preservation wasn't practical, every time. **Consequence:** a systemic, cross-cutting escape hatch touching most of the evidence-handling articles. **Recommend amendment** — a single general clause would close it everywhere at once, rather than needing separate fixes per article.

3. **Undefined evidential states enabling arbitrary self-classification.** Because none of Article IV's fourteen states carry necessary or sufficient conditions, a reasoning provider can assign whichever state it prefers to a proposition and appeal to its own internal judgment as the basis. This is most consequential where Article VII ties a hard constitutional consequence (discharge of the burden of justification) to one specific, undefined state. **Consequence:** the newest and most operative article in the amendment rests on an undefined term. **Recommend amendment** — at minimum, a short operative description of what distinguishes each adjacent pair of states, or an explicit rule for how the distinction is to be drawn.

4. **List-exhaustiveness ambiguity** (detailed in Section 3). **Consequence:** literal-compliance arguments that a novel non-justification escapes Article VII because it isn't named. **Recommend amendment** — one clause, described above.

5. **No declared hierarchy between the Foundational Principle and the Articles** (detailed in Section 3). **Consequence:** in a genuine dispute, either instrument could be cited selectively depending on which favours the arguer. **Recommend amendment** — one interpretive clause.

6. **Silent non-disclosure under Article V.** Parker may identify an embedded assumption, ambiguity, or prejudicial framing and simply decline to reformulate, with no obligation to tell the recipient the examination happened or what it found. **Consequence:** the "examine before evaluating" duty can be satisfied invisibly, defeating the transparency purpose of the article, though not its letter. **Moderate; recommend amendment** — a short clause requiring disclosure of a material identified defect even where reformulation is not undertaken.

7. **Ordinal ambiguity between "reasoned conclusion" and Article VII's threshold** (detailed in Sections 2 and 3). **Consequence:** a plausible, good-faith literal misreading could claim a burden discharged when, per the constitutional ordering, it has not been. **Recommend amendment** — a one-sentence clarification in either Article IV or Article VII would resolve it; no restructuring needed.

8. **Dormant override clause in Article XI** ("unless an applicable governing rule expressly requires it"). No rule presently invokes it. **Consequence:** none currently; flagged so that any future rule invoking this clause is itself held to the same testability standard as the rest of the amendment. **No amendment recommended now** — monitor only.

---

## 5. Constitutional Completeness Assessment

Three candidate additional principles were considered and are recorded here because the audit instructions require that omissions be actively searched for, not because any of them is being recommended:

* **Reconciliation among reasoning providers.** The amendment governs the relationship between a reasoning provider and "the Constitution," but says nothing about what happens when two reasoning providers within Parker assign different evidential states to the same proposition. This is a real question, but it is very plausibly already answered by the base Parker Constitution's general authority-arbitration mechanism (the same mechanism Article XIX defers enforcement to), and adding a bespoke rule here risks exactly the kind of "useful but not necessary" addition the instructions warn against. **Not recommended.**
* **Retroactive or temporal scope of the Constitution itself** — whether Version 1.0 requires correction of representations made before its own ratification. On reflection, Articles XVII and XVIII already operate continuously and prospectively in a way that would catch pre-ratification material as soon as it is revisited, without needing a separate retroactivity clause. **Not recommended.**
* **A supremacy/interpretation clause** resolving conflicts between the Foundational Principle and the Articles. Unlike the two candidates above, this was independently identified as material in Sections 3 and 4 (Loophole 5) through the ordinary audit process, not proposed speculatively here. It is carried forward into the Section 10 punch list rather than treated as a new finding in this section.

**Conclusion:** no wholly absent fundamental constitutional principle was found. The amendment's substantive coverage — representation, propositional integrity, evidential sufficiency, the burden of justification, provenance, evidential integrity, contemporaneity, weight, negative evidence, uncertainty, confidence, the separation of reasoning from representation, temporal integrity, revision, and correction — is a complete set for governing epistemic representation. What remains is tightening the definitions and disclosure rules that make the existing set enforceable, not adding to the set.

---

## 6. Constitutional Layering Assessment

Two items have drifted slightly below constitutional register:

* **Article IX's reference to "hashes" and "version history.**" These name a specific technical mechanism (cryptographic hashing) and a specific software concept (version history) directly in constitutional text, unlike the rest of the amendment, which describes functions and obligations rather than mechanisms. Recommend softening to something like "cryptographic or other integrity verifiers, and a record of successive versions," or explicitly framing the current list as illustrative examples rather than named requirements. This is an editorial fix, not a substantive one — the underlying obligation (preserve what supports an integrity assessment, where available) is exactly right at the constitutional level.
* **The dormant override clause in Article XI** discussed above sits at the right constitutional altitude (it is a statement about what kind of rule could override a weighing principle, not an implementation detail); it is noted here only because a future drafter, if they ever invoke it, should be reminded that whatever "applicable governing rule" they write must itself clear the same layering bar.

No provision was found so specific or so implementation-bound that it should be relocated wholesale out of the Constitution. The amendment's discipline about staying implementation-neutral ("wherever reasonably practical," "where reasonably available," no named data structures, no named algorithms other than the two noted above) has held up well across four drafting rounds.

---

## 7. Constitutional Test Audit

* **Coverage gap: Article VI.** No dedicated test group exists for Evidential Sufficiency. Its central rule — no representation of one conclusion as exclusive where the evidence does not justify exclusivity — is only loosely reachable through CT-EI-05 (which tests Article IV's preservation of competing explanations, a related but distinct question). Recommend a dedicated test, e.g., "does the capability avoid representing one among several reasonably available conclusions as exclusive when the evidence does not justify exclusivity?"
* **Redundant tests:** CT-EI-26 and CT-EI-46 (identified in Section 3) test the same obligation under two headings. Recommend retiring one.
* **No contradictory tests were found.** Every test that shares subject matter with another (e.g., the Contemporaneity and Weight groups both touching on the nature of a capture) asks a compatible, non-conflicting question.
* **Measurability:** the four tests carrying an express compliant answer (CT-EI-50, 61, 63, 65 to CT-EI-64) are the most rigorously testable in the suite, because they ask a yes/no question with a stated correct answer. Most other tests are phrased as open capability questions ("can Parker..."), which are auditable in principle but depend on the Article IV definitional gap identified above for a concrete pass/fail line. Tightening Article IV would materially improve the testability of a large fraction of the suite (CT-EI-02, 15–22, 44, 51–54) without needing to touch the tests themselves.
* **Every remaining article has at least one test that reaches its central obligation.** Subject to the Article VI gap above, the suite is otherwise complete relative to the current 19 articles.

---

## 8. Adversarial Constitutional Review

Assume a reasoning provider drafted specifically to satisfy Version 0.3's literal wording while minimising the behaviour the amendment is meant to prevent. Four exploit paths, each already introduced above, are consolidated here with severity ratings:

1. **The materiality dodge (severity: high).** Classify any inconvenient representation as non-material. Nothing in the text requires the classification to be defended, and no test checks it. This is the single most valuable exploit available against the current text, because it disables every other article at once rather than working around one.
2. **The practicality dodge (severity: high, cross-cutting).** Invoke "not reasonably practical" or "not reasonably available" silently, for every provenance, integrity, or weight obligation that would be inconvenient to satisfy. Because disclosure is not required when the exception is invoked, the exception itself is invisible and therefore unreviewable.
3. **The state-inflation dodge (severity: high, concentrated on Article VII).** Label weak evidence with a stronger, undefined evidential state (there being no operational criteria to contradict the label) to manufacture a discharged burden of justification where none should exist. This exploit is new in this round because Article VII, added in Version 0.3, is the first article to attach a hard consequence to a specific named state rather than to the states as a general, comparative taxonomy.
4. **The list-boundary dodge (severity: medium).** Argue that a novel non-justification not named in Article VII's list (silence, brevity, absence of objection from a user, tone of confidence) is not covered by the article's enumerated prohibitions, since the list is not marked non-exhaustive.

None of these exploits require rewriting more than a sentence or two per fix, and none of them indicate the underlying architecture is unsound — they indicate that four specific places in otherwise-sound text were left more permissive than the drafters' evident intent. That is a normal finding for a document at this stage, not evidence the amendment needs to be rebuilt.

---

## 9. Ratification Recommendation

1. **Is Version 0.3 constitutionally coherent?** Yes, at the architectural level. The nineteen articles form a single, non-contradictory system, and every cross-reference was independently re-verified against the current numbering.
2. **Is it internally consistent?** Substantially, with the specific exceptions in Section 3 (one undefined operative term, one terminology inconsistency, one duplicated test, one list-exhaustiveness inconsistency, one undeclared interpretive hierarchy).
3. **Is it sufficiently complete to govern Parker's epistemic reasoning?** In substantive coverage, yes — no missing fundamental principle was found. In enforceability, not yet — the completeness of the underlying definitions (materiality, evidential states) does not yet match the completeness of the article structure built on top of them.
4. **Would this audit recommend ratification as permanent, unamendable Version 1.0 in its current form?** No.
5. **What prevents ratification:** the four Tier 1 items in Section 10 — an unoperationalised materiality standard, undefined evidential states underpinning Article VII's bright-line rule, an undeclared hierarchy between the Foundational Principle and the Articles, and the undisclosed, unreviewable use of "reasonably practical/available" exceptions across the evidence-handling articles. Each is closeable with narrow, targeted language; none requires new doctrine or structural change.
6. **Classification of all findings**, provided regardless of the answer to (4) so the punch list below is self-contained:

* **Constitutional** (goes to enforceability or coherence; should be fixed before permanent ratification): Loopholes 1, 2, 3, 5 (Section 4); the undeclared Foundational Principle/Article hierarchy (Section 3).
* **Structural** (mechanical, narrower fixes; should be fixed before v1.0 but do not touch the amendment's architecture): the Article VI test-coverage gap; Loophole 4 (list exhaustiveness); Loophole 6 (Article V silent non-disclosure); Loophole 7 (reasoned-conclusion ordinal ambiguity); the CT-EI-26/46 duplicate test.
* **Editorial** (polish, does not affect enforceability): "material conclusion" vs. "material proposition" in Article VI; "evidential status" vs. "evidential state"; "hashes"/"version history" in Article IX.

---

## 10. Prioritised List of Constitutional Issues Requiring Amendment Before Version 1.0

**Tier 1 — constitutional, address before permanent ratification:**

1. Operationalise "materiality," or at minimum require that a determination that a representation is non-material be reasoned and reviewable when challenged.
2. Supply operative content distinguishing Article IV's fourteen evidential states — at minimum, enough to make Article VII's threshold unambiguous.
3. Add a single interpretive-hierarchy clause stating the relationship between the Foundational Principle, the Rationale, and the operative Articles.
4. Require disclosure whenever a "reasonably practical" or "reasonably available" exception is invoked in a way that affects a material representation.

**Tier 2 — structural, address before v1.0:**

5. Add a dedicated constitutional test for Article VI's exclusivity rule.
6. Add one general clause establishing that enumerated lists in this Amendment are illustrative and not exhaustive unless expressly stated otherwise.
7. Require disclosure of a materially identified propositional defect under Article V even where Parker elects not to reformulate.
8. Clarify the ordinal relationship between "reasoned conclusion" and the "evidentially supported conclusion" threshold used in Article VII.
9. Retire the duplicate test (CT-EI-26 or CT-EI-46).

**Tier 3 — editorial, no urgency:**

10. Replace "material conclusion" in Article VI with the defined term "material proposition."
11. Standardise on either "evidential state" or "evidential status" throughout.
12. Soften "hashes" and "version history" in Article IX to illustrative, non-mechanism-specific language.

This audit does not draft Version 0.4. If a further amendment cycle is wanted, it should be scoped to Tier 1 (and, ideally, Tier 2) only — this amendment does not need new constitutional principles, and none are proposed here.
