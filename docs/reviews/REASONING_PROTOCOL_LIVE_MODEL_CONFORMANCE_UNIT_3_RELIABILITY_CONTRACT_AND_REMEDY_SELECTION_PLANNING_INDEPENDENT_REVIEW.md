**Status:** Independent Planning Review of the Unit 3 Reliability Contract and Remedy Selection Planning Review — **ACCEPTED WITH QUALIFICATIONS.** The Planning Review was treated as evidence, not authority — every cited number and every remedy-family status was independently re-derived from the original programme Planning Review's own text or from the raw Unit 2-D evidence, not accepted from the Planning Review's own tables. No live model call, no HTTP call, no campaign, and no repository mutation beyond this document occurred.

# Unit 3 Planning Review — Independent Planning Review

## 1. Method

Re-read the Planning Review in full against: the original, top-level programme Planning Review (`05d4c2a`) and its Independent Constitutional Review, re-read directly rather than trusted from citation; the raw Unit 2-D campaign data already independently verified in the Interpretation and Closure Review chain; and the current production files' documented invariants. Each of the nine specific challenges the task requires is addressed as its own section below.

## 2. Whether DQ5 has biased remedy selection

No remedy is selected anywhere in the Planning Review — independently confirmed by re-reading Sections 7, 14, 15, and 16 in full; Section 7 explicitly tests and separates propositions A–E and stops at E ("warrants controlled comparison"), never reaching D ("should become Parker's architecture").

**Finding (non-blocking):** Section 11's internal-structure proposal names "the highest-priority candidates (Section 6 families 1, 3, and 9 as currently best-evidenced)" for the first wave of Unit 3-B experiment scoping. This is evidence-traceable — family 1 has DQ5's direct support, family 3 has both historical precedent and a clear evidentiary gap to close, family 9 has DQ1's consistency pattern as its motivating observation — but "highest-priority" is itself a soft ranking, and ranking three specific families ahead of the other seven is one step closer to informal selection than a flat, unranked enumeration would be. This is exactly the category of drift the prior Independent Constitutional Review (of the Unit 2-D Closure Review) already warned against for "best-evidenced candidate" language. It does not cross into remedy selection — no mechanism is chosen, no architecture is endorsed — but the language should be softened in any future citation (for example: "candidates with a specific, named evidentiary basis for near-term Unit 3-B scoping, in no mandated order" rather than "highest-priority").

## 3. Whether structured output has been confused with semantic correctness

Independently re-checked Section 6 (family 2) and Section 8 against the raw campaign data: PF01 plus 17 of Unit 2-D's 24 observations were representation-valid and semantically wrong, and the Planning Review cites this exact fact as the reason structured output is "not responsive to the dominant observed failure mode." The distinction between syntactic/representation enforcement and semantic action-selection correctness is stated explicitly and repeated in two separate sections. No conflation found — this is one of the most carefully guarded claims in the document.

## 4. Whether deterministic classification has been treated as obviously safe

Independently re-checked Section 6 (family 9) and Section 9: both explicitly name it "the single highest-constitutional-stakes family" and require investigation to "begin with the adversarial/false-positive risk surface, not the easy explicit-case win." Not treated as safe in either section.

**Finding (non-blocking):** Section 11 lists family 9 alongside families 1 and 3 under one undifferentiated "highest-priority" grouping without repeating the risk asymmetry Section 6 itself establishes. A reader encountering only Section 11 would not learn that family 9 carries materially higher constitutional stakes than families 1 or 3 — that context exists two sections earlier but is not carried forward into the structural recommendation where it matters most (which experiment gets scoped, and how cautiously). Recommended: Section 11's listing should explicitly flag family 9's elevated risk requirement inline, not only in Section 6.

## 5. Whether model substitution has been treated as a cure without evidence

Independently re-checked Section 6 (family 7): explicitly states current evidence "gives no reason to expect a different model performs better" and requires the original Planning Review's own full blind-corpus qualification process (§11 there) before any substitution decision, "not ruled out, not prioritized." Not treated as a cure. No finding.

## 6. Whether retry has been allowed to conceal semantic unreliability

Independently re-checked Section 6 (families 4 and 5): semantic retry is classified "NOT RESPONSIVE TO OBSERVED FAILURE," citing DQ1's own consistency as evidence against it, and explicitly ties the concealment/authority-escalation risk to existing governance rather than dismissing it. The one narrower door left open — retry restricted to representation-class failures (C/E/F/G) only — was independently checked for a concealment loophole: accepting whichever attempt first achieves valid representation still records that attempt's semantic content unmodified, with no semantic repair or preference for a "better" answer across attempts; this preserves the "no semantic repair" principle rather than reintroducing it by another route. No defect found, though this distinction is subtle enough that any future Unit 3-B scope drafting this door should restate it explicitly rather than relying on this Planning Review's brief mention.

## 7. Whether numerical acceptance criteria were invented

Independently cross-checked every number in Sections 5 and 12 against the original programme Planning Review's own §6 text, not against the Unit 3 Planning Review's citation of it: "at least 99% representation validity" → cited as "≥99%," exact. "a one-sided 95% lower confidence bound of at least 97%" → cited exactly, same structure. "at least 300 exposures" → cited as "300-exposure," exact. "zero observed false-positive REMEMBER and GOAL" and "zero material mutation or invention" → both cited verbatim in substance. **No number appears anywhere in the Planning Review that is not traceable to this already-frozen source.** Every threshold explicitly outside that set (timeout values, any mechanism-specific metric) is correctly marked UNRESOLVED rather than assigned a value. This is the strongest-verified section of the review.

## 8. Whether Unit 3 has become too broad

The document's breadth (ten remedy families, a full contract table, a five-part internal structure, nine architectural boundaries) matches the task's own explicit, itemized requirements rather than representing scope creep beyond them. The *authorized* scope (Section 14) is narrow: drafting a Unit 3-A Scope Lock only. Independently confirmed no section anywhere authorizes 3-B/3-C/3-D/3-E content, only their existence as required future gates. No finding.

## 9. Whether experimental work and remedy selection are sufficiently separated

Section 11's A–E internal structure independently re-checked: 3-B (experiment Scope Locks) and 3-C (experiments) are evidence-gathering only; 3-D (comparative evaluation) is explicitly modeled on Unit 2-D's own Interpretation/Closure pattern, itself already proven not to leak into selection; 3-E (Remedy Selection) is explicitly described as "a document, not code," separated from Unit 4 (implementation) by the *original* programme's own governance, not a boundary this Planning Review invented. This mirrors, gate-for-gate, the Scope Lock → Plan → Readiness → Approval → Execution → Evidence → Closure sequence Unit 2-D itself was actually held to. Adequately separated, subject to the Section 2/4 ranking-language qualifications already noted.

## 10. Whether production implementation has been prematurely authorized

Checked explicitly: Sections 15 and 16 both restate, in different words, that nothing beyond planning is authorized, and Section 16's prohibited-conclusions list explicitly names "production implementation of any kind" as not authorized. No finding.

## 11. Additional check: relationship to prior units

Independently verified the claimed relationship between the original programme Planning Review's Unit 3/Unit 4 split and this Planning Review's Section 11 structure: the original table (§13 there) names Unit 3 as governance-document-only and Unit 4 as the separate implementation unit — re-read directly, confirmed accurate, not a convenient reinterpretation. The claim that Unit 2-D's insertion "qualifies" rather than restructures this table is also independently sound: nothing in the Post-Unit-2 Diagnostic Planning Review or the Unit 2-D Scope Lock renames, removes, or reassigns Unit 3's or Unit 4's original purpose.

## 12. Blocking defects

None.

## 13. Non-blocking qualifications

1. Section 11's "highest-priority" language for families 1, 3, and 9 is evidence-traceable but should be softened to avoid reading as informal remedy-family selection (Section 2 above).
2. Section 11's grouping of family 9 alongside families 1 and 3 does not carry forward Section 6's own explicit risk-severity distinction; family 9's elevated constitutional stakes should be restated at the point where experiment scoping actually happens (Section 4 above).

## 14. Verdict

```text
ACCEPTED WITH QUALIFICATIONS
```

The Planning Review's principal determination — proceed to Unit 3-A Scope Lock drafting now, with the five-part internal structure frozen as a binding constraint on everything that follows — is independently confirmed sound, evidence-traceable, and free of invented numbers, premature remedy selection, or unauthorized implementation. The two qualifications above are wording-level, not substantive, and do not require redrafting before Unit 3-A Scope Lock work begins.

## 15. Confirmation

No model or HTTP call occurred during this review. No campaign was created, resumed, or modified. No production, test, or Gradle file changed. No remedy was selected, prototyped, or endorsed.
