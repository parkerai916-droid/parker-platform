**Status:** Narrow governance clarification only. **Adopted.** Independent Constitutional Review is complete (`docs/reviews/PROGRAMME_3_EXPLICIT_OWNER_INSTRUCTION_PROMOTION_EXCEPTION_INDEPENDENT_CONSTITUTIONAL_REVIEW.md`): `ACCEPTED`, no required correction, no Defect Confirmation Review necessary. No Kotlin is implemented, proposed as a diff, or changed by this document. Neither `src/` nor `tests/` is touched. This document does not amend `docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_CONTRACT_DESIGN_V2.md` ("Contract Design V2"), `docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_SCOPE_LOCK.md` ("the Scope Lock"), or any prior Unit Scope Lock Clarification (Units 4–9), all of which remain frozen and unchanged. It does not reopen Unit 6, and it does not approve `DefaultKnowledgeCandidateEvaluator` or any other existing implementation as constitutionally complete (Contract Design V2 §16.12's own "No Post-Hoc Ratification" guarantee is unaffected and unrelaxed by this document). It performs exactly one act: it exercises the express-exception authority Contract Design V2 §5 itself already reserves ("absent an express, documented governing-rule exception stated and justified at Scope Lock, mirroring Article XI §1's own conditions") to create one new, narrowly-scoped single-factor promotion exception. Nothing is staged, committed, or pushed.

# Programme 3 — Explicit Owner Instruction Promotion Exception — Scope Lock Clarification

Programme: **Programme 3 — Knowledge Memory, Explicit Owner Instruction Promotion Exception (Scope Lock Clarification).**

## 1. Status and Precedent Basis for This Governance Vehicle

Contract Design V2 §5's own binding text: "Weighing must consider more than one factor — no single factor may, by itself, determine promotion or the resulting evidential-state classification, absent an express, documented governing-rule exception stated and justified at Scope Lock (mirroring Article XI §1's own conditions for such an exception)." Exactly one such exception exists today — Contradiction (Contract Design V2 §3, "Contradiction" paragraph; Scope Lock §89 restates the standing prohibition it operates against). This document creates a second, structured identically: a narrow, disclosed, Article-XI-conditioned carve-out from the standing multi-factor rule, for one specific, deterministically-recognisable circumstance — never a general loosening of the rule itself.

This document is a "Scope Lock Clarification" in exactly the sense Units 4–9 already established: it does not edit the frozen `PROGRAMME_3_KNOWLEDGE_MEMORY_SCOPE_LOCK.md` file itself, but constitutes Scope-Lock-tier governance in its own right, mirroring Unit 9 Clarification's own identical precedent for performing a first-instance classification at this tier without touching the original document.

**Why this vehicle, not a Contract Design amendment.** Contract Design V2 §5's own text names "Scope Lock" as the required situs for this specific kind of exception, distinct from where the Contradiction exception itself was recorded (Contract Design V2 §3). This document does not treat that as an inconsistency to resolve — it follows §5's own explicit instruction for *this* exception, exactly as written, without asserting anything about the correctness of any different choice made for Contradiction historically.

---

## 2. Repository Baseline

- **HEAD:** `1955c037dec4add558dd51e39c94d67fbe7e1458` (`1955c03`)
- **Branch:** `main`
- **Working tree:** clean, confirmed before drafting.

---

## 3. Authorities

Read fresh, in full or in every relevant section, for this document: `docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_CONTRACT_DESIGN_V2.md` (§2, §3, §5, §16 in full including the 16.9–16.13 extension); `docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_SCOPE_LOCK.md` (§89, §118, §120); `docs/architecture/epistemic-integrity.md` (Articles IV, XI, XIV, XV, in full); `docs/reviews/PROGRAMME_3_KNOWLEDGE_PROMOTION_FACTOR_PROVENANCE.md` (in full — read as historical research, superseded where it conflicts with Contract Design V2 §16, per Section 4 below); `src/interfaces/KnowledgeStore.kt` (`KnowledgeCandidate`, `KnowledgeCandidateEvaluation`, `EvidentialState`); `src/runtime/DefaultKnowledgeCandidateEvaluator.kt` (in full, as it exists today).

---

## 4. A Disclosed Correction to Prior Research — the Provenance Document Is Stale on One Point

`PROGRAMME_3_KNOWLEDGE_PROMOTION_FACTOR_PROVENANCE.md` §2.6 states "the Unit 5-authored `KnowledgeCandidate`... carries only one field" and classifies Explicit Request as **Planned**, not **Available**, on the basis that the field was "dropped" from the legacy `CandidateKnowledge` shape. Read against the current, actual `src/interfaces/KnowledgeStore.kt` (line 857 onward), this is no longer accurate: `KnowledgeCandidate` already carries `explicitlyRequested: Boolean? = null`, and `DefaultKnowledgeCandidateEvaluator` already reads it as one of its own two weighed factors. Contract Design V2 §16 (Phase 1 Amendment 5, adopted after the Provenance document was written) is what closed this gap — §16.3 Guarantee 1 authorises the field, and §16.10 confirms explicit request is now "Available" alongside confidence, not merely "Planned." This document treats Contract Design V2 §16 as authoritative and current; the Provenance document's own classification of Explicit Request is superseded on this one point and is not relied upon below.

This correction does not change this document's own central finding: §16.10 remains explicit that explicit request, now available, still counts as **exactly one factor**, and "Nothing in this Amendment relaxes" the prohibition on single-factor promotion. The Provenance document's staleness concerns only whether the field exists at all — not whether it is, by itself, sufficient. It is not, and §16.10 says so directly.

---

## 5. Question Presented

Whether an explicit, unambiguous owner instruction to remember a specific proposition — for example, "Remember that my favourite coffee mug is black" — may constitutionally serve as its own, sufficient promotion factor, distinct from and in addition to the "explicit request" factor Contract Design V2 §16 already authorises, given that §16.10 already forecloses treating explicit request *alone*, under the existing two-factor gate, as sufficient.

---

## 6. Why a New Exception, Rather Than Relying on §16 as Already Sufficient

Confirmed by direct reading of `DefaultKnowledgeCandidateEvaluator`, `Assertion.confidence` is the only other presently-reachable factor, and it is only ever genuinely present when independently recorded on the referenced `Assertion` at creation time from a real evidential source (Section 2.5 of the Provenance document, unaffected by Section 4's correction above: "must never be defaulted to zero," "never invented, inferred, or synthesised"). A proposition whose only basis is the owner's own conversational statement — with no independent document, external evidence, or corroborating record — has no honest, non-fabricated confidence value to record. Requiring the two-factor gate for this circumstance would mean **no explicit owner "remember" instruction could ever be promoted**, not merely a cautious default but a structural impossibility, since the second factor's only lawful source (genuine evidential confidence) does not exist for a bare, self-reported conversational statement by construction, not by any correctable implementation gap.

This is not a defect in the existing two-factor gate — it is functioning exactly as designed, for the evidential material it was built to weigh (Memory Core evidence resolved from Evidence Custodian/Evidence Intelligence, where a genuine confidence figure may exist). It simply was never designed to admit a fundamentally different circumstance: an owner directly exercising authority over their own personal memory store, asking Parker to remember something as a matter of instruction, not as a matter of evidential proof. This document's central determination is that this circumstance is constitutionally distinguishable from the two-factor gate's own subject matter, on the same reasoning Contract Design V2 §16.2 already used to distinguish explicit request itself from evidential content: "a factual claim about the circumstances of submission... architecturally distinct from an evidential judgment."

---

## 7. Constitutional Guarantees

1. **A new, narrowly-scoped express exception.** Where a `KnowledgeCandidate` carries a caller-reported indication that the evidence it references was created *specifically and only* to durably record an explicit, unambiguous owner instruction to remember a stated proposition, promotion may proceed on that basis alone, without requiring a second, independently-reachable factor under Contract Design V2 §5's ordinary gate.
2. **Recognition must be deterministic, never model-inferred importance.** The triggering fact is narrow and mechanical: did the owner directly and unambiguously instruct Parker to remember a specific, stated proposition? This is a speech-act classification (what kind of utterance occurred), not an evidential-weight or importance judgment (how significant, reliable, or noteworthy the proposition is). A Reasoning Provider may identify that such an instruction occurred — exactly as it already identifies whether a Turn is a `Goal` or a `Reply`, an intent classification already trusted to it — but it may never determine, characterise, or imply the proposition's own evidential weight, confidence, importance, or truth (Article XV; Contract Design V2 §16.9's own guardrails against fabricating a confidence figure apply identically here). Genuine ambiguity — an instruction that is not unmistakably a direct request to remember a specific, stated proposition — does not qualify; the constructing subsystem must fail closed, never guess.
3. **Mandatory weakest-honest classification.** A candidate promoted solely under this exception must be assigned `EvidentialState.UNKNOWN` — the same, weakest non-`INDETERMINATE` classification the existing two-factor gate already assigns when its own two factors are jointly, but not more than minimally, satisfied (`DefaultKnowledgeCandidateEvaluator`'s own current `promote(..., state = EvidentialState.UNKNOWN, ...)` call for its two-factor path). Nothing about this exception licenses a stronger classification than the existing gate itself ever produces; this exception is strictly no more generous in its evidential-state output than ordinary two-factor promotion, only more permissive about how few factors are required to reach it.
4. **Mandatory honest basis disclosure.** The Knowledge Promotion record's own basis (Contract Design V2 §3's existing disclosure requirement, unchanged) must state plainly that promotion rests solely on an explicit owner instruction, with no independent evidential weight — mirroring the existing Contradiction exception's own disclosure discipline (`DefaultKnowledgeCandidateEvaluator`'s own current Contradiction-branch basis text is the direct structural precedent). It must never be phrased in a way that implies confidence, corroboration, or evidential support this exception does not, and cannot, supply.
5. **No effect on the ordinary gate.** This exception applies only where the caller-reported indication above is present and true. Every other candidate — including any candidate whose `explicitlyRequested` flag is set for a reason other than this narrow circumstance — remains governed by Contract Design V2 §5's existing, unrelaxed two-factor requirement, exactly as before this document. This document creates no general relaxation of §5 and no wider reading of `explicitlyRequested` than §16 already authorised.
6. **No new evidential authority.** Exactly as §16.3 Guarantee 2 already establishes for the ordinary explicit-request factor, this exception creates no caller override of Knowledge Memory's own exclusive authority to compute confidence or evidential-state (Contract Design V2 §2, §4). The caller reports only the narrow, mechanical fact that an explicit remember-instruction occurred; Knowledge Memory alone decides the resulting classification, fixed at `UNKNOWN` by Guarantee 3 above, never a caller-supplied value.
7. **Article XI conditions, checked directly.** Article XI §1: "No single factor shall automatically determine truth or falsity unless an applicable governing rule expressly requires it." This exception does not determine truth or falsity — Contract Design V2 §3's own, unchanged reaffirmation applies without modification: "promotion never constitutes a truth determination." What this exception determines is narrower and lower-stakes: *durability-worthiness* of a record the owner directly asked to be kept, classified at the weakest honest evidential state available, with the fact of its own thin basis disclosed on every result (Contract Design V2 §3, "Staleness" subsection's own adjacent disclosure discipline is the structural sibling of this requirement, though staleness itself is unaffected by this document).
8. **Technology independence.** No specific representation (a boolean, an enumeration, or otherwise) is named, required, or precluded by this section for how the constructing subsystem reports the triggering fact; any representation satisfying the guarantees above is permitted.

---

## 8. Constitutional Boundaries

This exception does not:

- constitute evidence of the truth of the underlying proposition (Guarantee 7);
- authorise any evidential-state classification stronger than `EvidentialState.UNKNOWN` (Guarantee 3);
- authorise a Reasoning Provider, or any other module, to determine, characterise, or imply the proposition's own evidential weight, confidence, or importance (Guarantee 2; Article XV);
- authorise ordinary incidental conversational statements ("My mug is black," "I went to Wellington yesterday") to qualify — only a direct, unambiguous instruction to remember a specific, stated proposition qualifies, and genuine ambiguity fails closed (Guarantee 2);
- relax Contract Design V2 §5's ordinary two-factor requirement for any candidate outside this narrow circumstance (Guarantee 5);
- grant any caller override authority over Knowledge Memory's own exclusive confidence/evidential-state computation (Guarantee 6);
- authorise repetition of the same instruction to accumulate weight, corroborate itself, or otherwise be treated as more than one instance of this same, single exception (no mechanism for detecting or weighing repetition is introduced or implied by this document);
- create, require, or authorise any Memory Core write mechanism, permission proposal class, or runtime component — this document governs only the promotion-evaluation boundary; how the underlying Memory Core evidence is created, and under what permission gate, is Implementation-Plan-tier work this document does not perform and does not pre-empt;
- approve any existing implementation as compliant, or treat Knowledge Memory as complete (Contract Design V2 §16.12's own guarantee, unaffected).

---

## 9. Ownership and Consumers

Unchanged from §16.5's own established pattern: the Knowledge Candidate contract slot belongs to Knowledge Memory; the triggering fact is supplied by whichever subsystem constructs the candidate on the owner's behalf during a conversation turn. The only authorised consumer of the triggering fact is Knowledge Memory's own promotion evaluator. No new consumer is authorised by this document.

---

## 10. Failure Semantics

At the constitutional level only: where the constructing subsystem cannot establish, with confidence appropriate to a deterministic classification, that an owner's instruction unambiguously qualifies, it must not report the triggering fact as present. No exception, return type, status code, or API is defined by this document — that remains Implementation-Plan-tier work.

---

## 11. What This Document Does Not Decide

- The Kotlin representation of the triggering fact (a new field, a reused field with a documented convention, or otherwise) — Implementation-Plan-tier.
- How, mechanically, a conversation turn is classified as containing such an instruction (a new Reasoning Provider response tag, a deterministic text-pattern classifier, or another mechanism) — Implementation-Plan-tier, subject to Guarantee 2's own constraint that the classification itself must be deterministic and intent-only, never an evidential judgment.
- How, or under what permission proposal class, the underlying Memory Core evidence (a Provenance and an Assertion) is created before a Knowledge Candidate can reference it — Implementation-Plan-tier; Chapter 10's own general admission test already, unambiguously classifies any Memory Core write as a PermissionEngine proposal class, so no further Scope-Lock-tier self-certification is required for that determination the way one was required for Knowledge Retrieval (Unit 9 Clarification §6–7 exists precisely because that classification was genuinely contested; a write is not).
- Conversational retrieval of a promoted Knowledge Item — a separate, already-disclosed gap (`ParkerRuntime.kt`'s own `knowledgeRetrieval` field comment: "no production entry point consumes it yet... Programme 4's own, separately governed act"), entirely unaddressed by this document.

---

## 12. Constitutional Consistency Check

This document has been checked against Contract Design V2 in full (§1–§16.13), the Scope Lock (§89, §118, §120), `docs/architecture/epistemic-integrity.md` (Articles IV, XI, XIV, XV), and the Unit 6 (as amended), 7, 8, and 9 Scope Lock Clarifications, all confirmed frozen and unaffected. No inconsistency was found. This document narrows implementation freedom (a candidate may only rely on this exception where the narrow triggering fact genuinely holds) and forecloses post-hoc ratification of any existing implementation (Section 1); it relaxes no existing guarantee for any candidate outside its own narrow scope, and it authorises no field, mechanism, or consumer beyond the one narrow exception Sections 7–9 define.

```
EXPLICIT OWNER INSTRUCTION PROMOTION EXCEPTION — PROPOSED, PENDING INDEPENDENT CONSTITUTIONAL REVIEW
```

Confirmed: no production code modified; no tests modified; no other governance document modified; nothing staged; nothing committed; nothing pushed; implementation not started.
