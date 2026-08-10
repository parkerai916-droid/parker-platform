**Status:** Independent Constitutional Review of the Unit 3-A Reliability Contract Definition Scope Lock — **ACCEPTED WITH QUALIFICATIONS.** The Scope Lock was treated as evidence, not authority. Every numerical threshold was independently re-verified against the primary source text (not the Scope Lock's own citation of it); every neutrality claim was independently checked by direct text search of the actual document, not accepted from the Scope Lock's own self-report. No live model call, no HTTP call, no campaign, and no repository mutation beyond this document occurred. The Scope Lock itself was not modified.

# Unit 3-A Reliability Contract Definition Scope Lock — Independent Constitutional Review

## 1. Method

Re-read the Scope Lock in full against: the programme Planning Review's own §6, §7, §9, §10, §11, §12 text, re-read directly; `src/interfaces/ReasoningProvider.kt`'s actual doc comments, re-read directly; and the Scope Lock's own text, searched directly (`grep`) for every claim it makes about itself (word absence, citation location, single-mention claims) rather than trusting those claims on their word. This is the same discipline this review chain has applied throughout — verify, do not accept.

## 2. Inherited requirements — independently re-traced

Re-checked every row of the Scope Lock's Section 4 table against the programme Planning Review's primary text, independently, a third time (following two independent re-readings already performed earlier in this same session): "at least 30 independent trials," "at least 300 exposures," "`1 - 0.05^(1/300)`," "zero observed false-positive `REMEMBER` and `GOAL`," "zero material mutation or invention," "at least 99% representation validity," "a one-sided 95% lower confidence bound of at least 97%" — every figure matches its source exactly, with no rounding, interpolation, strengthening, or weakening. **No numerical threshold anywhere in the Scope Lock is unsupported or invented** — independently confirmed, not merely repeated from the Scope Lock's own Section 4 disclaimer.

## 3. Whether any threshold was weakened or strengthened

Checked specifically for silent softening (for example, "approximately 99%" or "97% or better") and silent hardening (for example, "99.9%"): none found. Every threshold uses "≥" faithfully representing the source's "at least" language, with no substitution.

## 4. Whether any remedy was explicitly or implicitly selected

No remedy is selected. Checked Section 13's own neutrality claims by direct search rather than trusting them:

- **DQ5 mention count:** independently searched for `DQ5`, `0/18`, `2/5` — all three appear exactly once, in Section 13 itself, framed as diagnostic background. Confirmed accurate; DQ5 does not bias Sections 6–12.
- **Banned preference words** ("preferred," "best," "leading," "highest priority," "recommended architecture"): independently searched — the only occurrences are inside the single sentence of Section 13 that is *itself* asserting those words are absent elsewhere (i.e., they appear as quoted evidence of absence, not as description). No actual preference language exists in the document.

## 5. Whether structured output was confused with semantic correctness

Not confused anywhere. Sections 6 and 8 keep the two axes explicit and separate throughout; Section 13 correctly states structured output "is not... converted into a constitutional preference."

**Finding (non-blocking, citation accuracy):** Section 13 claims: *"Section 9's programme-level observation that a future structured transport 'need not change the domain contract' is cited in Section 4."* Independently searched the entire document for the exact phrase "later structured transport" and for "need not change the domain contract" — **neither appears anywhere in Section 4, or anywhere else in the document.** Section 4's actual citation of programme Planning Review §9 is a different sentence from the same paragraph ("Adding confidence, uncertainty, multiple acts, repair semantics, or a new action would require a separately governed contract change"), which is accurately quoted — but it is not the sentence Section 13 claims to be pointing back to. This is a false internal cross-reference: the underlying substantive point (that this specific structured-output-adjacent observation is used only as domain-contract-stability evidence, never as an endorsement) is not itself wrong in spirit and is not contradicted anywhere — but Section 13's claim about *where* that handling occurred is inaccurate. Recorded as a citation-integrity defect, not a substantive neutrality failure, since no preference actually leaks from the missing citation — there is simply nothing there to have leaked.

## 6. Whether deterministic classification was embedded into the contract

Not embedded. Appears only in Section 13's neutral enumeration. No dimension in Section 5, and no requirement in Sections 6–12, presupposes or requires rule-based classification.

## 7. Whether retry or semantic repair was smuggled into failure requirements

Semantic repair: not mentioned outside Section 13's neutral list. **Finding (non-blocking, structural):** "retry" is named specifically in Section 10 (the Failure Contract), not only in Section 13 (the Remedy-Neutrality Firewall): *"the closest is the already-existing, already-narrow 'invalid-representation-only retry' concept named in the programme Planning Review §8, which remains a permitted future candidate, not a requirement."* This is hedged carefully — it grants retry no threshold exemption, no fast-track, and is explicitly restricted to representation-class failures with semantic-level retry explicitly excluded in the same sentence — but it is nonetheless a mechanism named by name inside a section the Scope Lock's own Section 2 promises will stay "mechanism-free" except where a mechanism is "already constitutionally required" (none is, here — the text itself says "permitted," not "required"). The exception the sentence carves out for itself does not actually meet the bar its own surrounding sentence sets. This does not functionally privilege retry over any other family in Section 13's list — no other family's applicability to representation failures is foreclosed by this sentence — but the asymmetry of naming only this one family by name outside the firewall section is a genuine, findable inconsistency in the document's own stated discipline.

## 8. Whether model substitution was treated as equivalent to qualification

Independently re-checked Section 11: explicitly distinguishes "changing models is not, by itself, a remedy" from the qualification obligation both existing and any substitute model must meet. No conflation found.

## 9. Whether semantic correctness and representation correctness are independently enforceable

Confirmed, Section 6: both directions of non-conformance (valid-representation-wrong-action; correct-intent-invalid-representation) are stated explicitly and separately, with an explicit prohibition on collapsing them into one score.

## 10. Whether false-positive REMEMBER and GOAL protections are preserved

Confirmed unweakened, Section 7 — restates the zero-observed-event qualification requirement exactly, and independently adds a precise, previously-unstated distinction (contractual zero-tolerance vs. statistical proof of a true-zero rate) that strengthens the document's epistemic honesty without altering the requirement's substance. This addition was checked against the programme Planning Review's own §6 caveat ("This does not prove impossibility; it makes the residual statistical claim explicit") and found to be a faithful elaboration, not a new invented claim.

## 11. Whether content fidelity is defined sufficiently without prescribing implementation

Confirmed, Section 8: explicitly disclaims extraction algorithms, prompt formats, structured output, deterministic parsing, semantic repair, and rewriting mechanisms, defining only the externally observable property.

## 12. Whether ambiguity handling avoids pretending Parker can know unknowable intent

Confirmed, Section 9: explicitly declines to manufacture a universal clarification-seeking rule, correctly recording it as a deferred governance question (Section 5, item 11) rather than resolving it either way.

## 13. Whether failure behaviour is defined without selecting technical remedies

Substantially yes, subject to the Section 7 (of this review) finding above regarding retry's naming in Section 10.

## 14. Whether model/provider qualification is defined without selecting a model strategy

Confirmed, Section 11 — no candidate model is named, evaluated, or favored.

## 15. Whether measurement requirements are evidence-based and traceable

Confirmed, Section 12 — every requirement traces back to Section 4's table; every gap is recorded as unresolved rather than filled.

## 16. Whether the contract remains applicable to competing future remedy families

Confirmed by independent re-derivation: none of Sections 6–12's requirements presuppose or exclude any of the ten remedy families named in the Unit 3 Planning Review. A hypothetical future evaluation of any single family could be run against this contract's dimensions without modification.

## 17. Whether downstream authority has remained unchanged

Confirmed, Section 14 — explicit, unqualified, consistent with every prior unit's own treatment of this boundary.

## 18. Whether Unit 3-A is sufficiently narrow

Confirmed. No implementation, no experiment design, no comparative evaluation, and no remedy selection appears anywhere in the document.

## 19–23. Whether anything prematurely authorizes Unit 3-B, 3-C, 3-D, 3-E, or Unit 4

Independently searched every "authoriz-" occurrence in the document: all either explicitly deny authorization of later units or are unrelated (e.g., "authorization purpose" as a downstream-coordinator name in Section 14). No premature authorization of any kind found.

## 24. Whether anything authorizes live model execution

None found — no endpoint, no HTTP call, no campaign identity, no model-call instruction appears anywhere in the document.

## 25. Whether anything authorizes production/test implementation

None found.

## 26. Whether the Scope Lock closes honestly with unresolved questions rather than fabricating certainty

Confirmed as a genuine strength: Sections 5, 9, 10, and 12 each explicitly carry forward specific unresolved items (production timeout value; fail-vs-warn choice on qualification mismatch; whether clarification-seeking is ever required; provider-unavailable behavior) rather than inventing values to complete the tables. This is independently verified as consistent practice throughout, not merely asserted once and abandoned.

## 27. Blocking defects

None. No invented threshold, no weakened or strengthened requirement, no remedy selection, and no premature authorization was found anywhere in the document.

## 28. Non-blocking qualifications

1. **Section 4's "never may" attribution** overstates its source: `Remember`'s doc comment states "it carries no confidence, importance, or evidential-weight judgment of any kind, and never may," and separately analogizes Remember to Goal/Reply ("exactly like Goal versus Reply") — but the literal "never may" textual guarantee is only independently stated for `Remember`. The table row should attribute this quote to `Remember` specifically, noting `Goal`/`Reply` are analogized to it rather than independently the subject of identical wording.
2. **Section 10 names "retry" specifically**, outside the Section 13 firewall, in tension with the document's own stated discipline of keeping Sections 6–12 mechanism-free except where a mechanism is already *required* (this one is described as merely "permitted"). Recommended: move this observation into Section 13's neutral list, or rephrase Section 10 to describe the underlying already-governed constraint (no retry may escalate non-consequential results into consequential ones) without naming "retry" as a candidate mechanism by name.
3. **Section 13's self-referential citation claim is inaccurate**: the specific phrase it says is "cited in Section 4" does not appear there, or anywhere in the document. The underlying point survives via a different, accurately-quoted sentence from the same source paragraph, but the cross-reference itself should be corrected to point at what Section 4 actually says, or Section 4 should be extended to include the sentence Section 13 believes it already contains.

None of these three findings changes any requirement, threshold, or boundary this Scope Lock establishes — all three are precision and internal-consistency issues, independently verified as real but non-substantive.

## 29. Verdict

```text
ACCEPTED WITH QUALIFICATIONS
```

The Unit 3-A Reliability Contract Definition Scope Lock is independently confirmed to: trace every inherited requirement and threshold to authoritative primary governance without invention, weakening, or strengthening; maintain semantic/representation independence; preserve false-positive safety, content-fidelity, ambiguity, failure, and qualification requirements without prescribing mechanism (subject to qualification 2 above); remain neutral across competing remedy families (subject to qualification 3's citation-accuracy note); leave downstream authority untouched; and record unresolved questions honestly rather than fabricating closure. The three qualifications above are recommended corrections for the next natural revision of this document, not preconditions for treating it as ready to freeze.

## 30. Confirmation

No model or HTTP call occurred during this review. No campaign was created, resumed, or modified. No production, test, or Gradle file changed. No remedy was selected, prototyped, or endorsed. The Scope Lock document itself was not modified by this review.
