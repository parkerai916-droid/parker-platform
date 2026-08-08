**Status:** Genuine Independent Constitutional Review, performed as if by another reviewer, against the governing documents re-read fresh and against the clarification's own actual text — not against its own summary of itself. This document does not amend the clarification, Contract Design V2, the Scope Lock, or any prior Unit Clarification. No Kotlin is touched. Nothing is staged, committed, or pushed.

# Programme 3 — Explicit Owner Instruction Promotion Exception — Independent Constitutional Review

## 1. Baseline Confirmation

`HEAD` at `1955c03`, matching the clarification's own claimed baseline. `git status --short` clean except the one new, untracked clarification document itself. Confirmed accurate.

---

## 2. Challenge — Is the Provenance-Document Correction (Section 4) Actually Necessary, or Does It Overstate a Non-Issue?

Independently re-read `src/interfaces/KnowledgeStore.kt` directly: `KnowledgeCandidate` (line 857) reads `data class KnowledgeCandidate(val evidenceReference: MemoryCoreRecordReference, val explicitlyRequested: Boolean? = null)` — two fields, not one. Independently re-read `PROGRAMME_3_KNOWLEDGE_PROMOTION_FACTOR_PROVENANCE.md` §2.6 directly: "the Unit 5-authored `KnowledgeCandidate`... carries only one field," and its own Deliverable Table classifies Explicit Request as **Planned**. This is a genuine, material discrepancy between a governance research document and the actual current source — not overstated. The clarification's own resolution (treat Contract Design V2 §16, adopted after the Provenance document, as superseding) is correct: §16.3 Guarantee 1 is unambiguous authorisation for exactly the field that exists today. **Necessary, and correctly resolved.**

---

## 3. Challenge — Does the Central Argument (Section 6) Actually Establish Structural Impossibility, or Merely Inconvenience?

This is the load-bearing claim: that no explicit owner "remember" instruction could *ever* satisfy the existing two-factor gate. Traced independently: `Assertion.confidence: Double?` is set once, at `CandidateAssertion` construction (confirmed directly in `src/interfaces/MemoryCore.kt`), and nothing recomputes it later. For a proposition whose only source is the current conversation turn, no independent evidential act (a document, an external record, a separately-authenticated fact) exists from which a non-fabricated confidence figure could be derived — inventing one would violate Article XIV directly ("Confidence shall be determined by evidential support... not... persuasiveness") and Contract Design V2 §16.9's own explicit prohibition on fabricating "a confidence figure." This is not a missing implementation that could be built later within the existing gate's own terms — it is a genuine absence of any lawful source, by the nature of the circumstance itself. **The structural-impossibility claim holds under independent re-derivation, not merely assertion.**

---

## 4. Challenge — Does Permitting the Reasoning Provider to "Recognise" the Instruction Create a Disguised Article XV Violation?

Pressed hard, since this is the clarification's own riskiest boundary. Article XV, read in full, independently: "Reasoning providers generate candidate explanations, inferences, and conclusions... They do not possess constitutional authority to determine the final evidential status of their own outputs." The question is whether "the owner just gave an explicit remember instruction" is an evidential-status determination or a speech-act classification. Traced against the *existing*, already-accepted precedent: `TaggedReasoningResponseParser` already lets the Reasoning Provider classify a Turn as `Goal` versus `Reply` versus `NoAction` — an intent classification, not an evidential one, and this has never been treated as an Article XV violation anywhere in this repository's own governance. Guarantee 2's own explicit boundary — the Reasoning Provider "may never determine, characterise, or imply the proposition's own evidential weight, confidence, importance, or truth" — is the correct line, and it is the *same* line Article XV itself draws (evidential status, not intent). **Sound, provided the boundary is enforced exactly as stated** — this is a genuine risk surface, not eliminated by the document but correctly bounded by it; Section 6 below presses whether the boundary is enforceable in practice, which is a distinct question from whether it is correctly stated in principle.

---

## 5. Challenge — Is Guarantee 2's Boundary Actually Enforceable, or Does It Leave a Loophole for Disguised Importance-Weighing?

This is the review's own hardest question. A Reasoning Provider recognising "unambiguous" instructions could, in principle, apply exactly the kind of judgment ("this seems important enough to be unambiguous") the document forbids, without that judgment ever surfacing as a labelled confidence or importance value — the risk is not that a forbidden *field* gets set, but that a forbidden *judgment* gets smuggled through a permitted one. Examined directly: this risk is not unique to this exception — it already exists, identically, for the *existing*, already-adopted `explicitlyRequested` factor itself (Contract Design V2 §16), and for `Goal`-versus-`Reply` classification generally. The clarification does not worsen this pre-existing surface; it inherits it, on the same terms the repository has already accepted for the general-purpose intent-classification pattern. **This is a real, disclosed limitation of the deterministic-classification model generally, not a defect specific to this document** — and the document does not overclaim on this point: Guarantee 2 states the requirement as a constraint on the constructing subsystem's own behaviour, not as a claim that the constraint is self-enforcing or automatically verifiable. A future implementation review (Completion Review / Independent Constitutional Review of the Kotlin itself) is the correct place to verify the actual classifier's own text genuinely tests only for speech-act structure ("did the owner say a remember-shaped sentence") and not for content-based importance — this document correctly defers that verification to implementation, rather than pretending to resolve it here.

---

## 6. Challenge — Does Guarantee 3 (`EvidentialState.UNKNOWN`) Actually Hold `EvidentialState.INDETERMINATE` at Bay, or Could a Future Implementation Legitimately Argue for the Weaker State?

Independently re-read `EvidentialState.kt`'s own KDoc: `UNKNOWN` "presumes the matter is knowable in principle... given further evidence," while `INDETERMINATE` is reserved for when "it cannot presently be established whether the matter is knowable at all." An owner's explicit "remember that X" statement is, by construction, a knowable-in-principle proposition (the owner could, in principle, be asked to clarify, or further evidence could later confirm or disconfirm it) — `INDETERMINATE` would be the wrong, and actually *less* honest, choice here, since it would imply a deeper epistemic void than genuinely exists. `UNKNOWN` is correct, and the clarification's own reasoning (mirroring `DefaultKnowledgeCandidateEvaluator`'s own existing rationale for never assigning `INDETERMINATE`) is independently sound. **Confirmed correct, not merely asserted.**

---

## 7. Challenge — Is the Vehicle Choice (Scope Lock Clarification, Not a CDR-005 Escalation) Correct?

Checked directly against CDR-005's own Decision Rules (`docs/decisions/CDR-005_CONSTITUTIONAL_ADMISSION_OF_PERMISSIONENGINE_PROPOSAL_CLASSES.md`): CDR-005 governs *PermissionEngine proposal-class* classification specifically — the question Unit 9's own Clarification had to resolve for Retrieval. This document's own subject matter is *promotion weighing* (Contract Design V2 §5), a Knowledge Memory-internal evidential question, not a PermissionEngine proposal-class question at all — no act described anywhere in this document is being classified as requiring, or not requiring, Permission Engine authorisation. CDR-005 is therefore inapplicable to this document's own question, and Section 11's own explicit deferral of the *separate* permission-classification question (for the underlying Memory Core write) to Implementation-Plan tier is correctly scoped, not an evasion — Chapter 10's own general test already resolves that separate question unambiguously (Section 8, below), unlike Retrieval's own genuinely contested classification. **The vehicle choice is correct.**

---

## 8. Challenge — Is the Claim That "Chapter 10 Already Unambiguously Classifies Any Memory Core Write" Accurate, or Does It Understate a Genuine Residual Question?

Independently re-read Chapter 10 §3 directly: "a real, external, or state-changing consequence beyond Parker's own internal reasoning — for example, writing, amending, or superseding a durable record." This is named as an explicit, direct example, not merely an illustrative analogy requiring domain-specific interpretation the way Knowledge Retrieval's own disclosure-based consequence did (Unit 9 Clarification §6–7's own extended reasoning was needed precisely because Retrieval's consequence was *indirect* — disclosure of an existing record's evidential standing — not because "is a write a proposal" was ever genuinely contested). **The claim is accurate; no residual ambiguity is understated.** This document correctly declines to perform a self-certification exercise a genuinely unambiguous case does not require.

---

## 9. Challenge — Full, Independent Quotation Audit

| Quoted fragment | Cited source | Verified |
| --- | --- | --- |
| "Weighing must consider more than one factor — no single factor may, by itself, determine promotion or the resulting evidential-state classification, absent an express, documented governing-rule exception stated and justified at Scope Lock (mirroring Article XI §1's own conditions for such an exception)." | Contract Design V2 §5 | Exact match, confirmed by direct re-read. |
| "No single factor shall automatically determine truth or falsity unless an applicable governing rule expressly requires it." | Article XI §1 | Exact match, confirmed by direct re-read. |
| "Reasoning providers generate candidate explanations, inferences, and conclusions... They do not possess constitutional authority to determine the final evidential status of their own outputs." | Article XV | Exact match (lightly elided, contiguous), confirmed by direct re-read. |
| "a real, external, or state-changing consequence beyond Parker's own internal reasoning — for example, writing, amending, or superseding a durable record" | Chapter 10 §3 | Exact match, confirmed by direct re-read. |
| "presumes the matter is knowable in principle" / "cannot presently be established whether the matter is knowable at all" | `EvidentialState.kt`'s own KDoc | Exact match, confirmed by direct re-read in Section 6, above. |

No further quoted fragment appears beyond ordinary type/field names. **No defect found in any citation.**

---

## 10. Challenge — Does This Document Silently Widen Scope Beyond What It Claims to Decide?

Checked directly against Section 11's own "What This Document Does Not Decide" list: the Kotlin representation, the classification mechanism, the underlying write's own permission wiring, and conversational retrieval are all correctly and explicitly left open. No section of this document's own body (Sections 1–10, 12) reaches into any of these four areas — confirmed by direct re-read of the full document text. **Sound.**

---

## Findings

No required correction was found. The central structural-impossibility argument (Section 3), the Article XV boundary (Sections 4–5), the `EvidentialState.UNKNOWN` determination (Section 6), and the governance-vehicle choice (Sections 7–8) are each independently re-derived from primary sources, not merely re-accepted from the clarification's own account. One genuine, disclosed limitation was pressed and found to be an inherited, pre-existing risk surface (Section 5) rather than a new defect — correctly bounded, not eliminated, and correctly left for implementation-time verification rather than falsely claimed as resolved here.

---

## Constitutional Verdict

```
ACCEPTED
```

No required correction. No Defect Confirmation Review is necessary.

---

## Recommended Next Step

Proceed to a lightweight, Implementation-Plan-tier design for the conversational admission adapter itself, per this clarification's own Section 11 deferrals — the underlying Memory Core write's own permission classification, the deterministic recognition mechanism, and the Kotlin representation of the triggering fact all remain to be fixed there, not here.
