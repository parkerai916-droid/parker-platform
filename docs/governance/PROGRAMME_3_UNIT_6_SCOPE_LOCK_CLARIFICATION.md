**Status:** Narrow governance clarification only, **amended** per `docs/reviews/PROGRAMME_3_UNIT_6_CONSTITUTIONAL_RECONCILIATION.md`'s finding that the original version of this document conflicted with higher-precedence frozen governance. This amendment brings this document into conformity with `docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_CONTRACT_DESIGN_V2.md` and `docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_SCOPE_LOCK.md`, neither of which this amendment alters. Does not reopen Programme 3's architecture, layering, public model, or any of the eight amendments Contract Design Version 2 resolved. Does not amend Contract Design Version 2, the Scope Lock, the Implementation Plan, the Constitutional Reconciliation, or the Unit 4/Unit 5 Clarifications, all of which remain frozen and unchanged. No Kotlin is implemented, proposed, or changed by this document.

# Programme 3 — Unit 6 Scope Lock Clarification (Amended)

Programme: **Programme 3 — Knowledge Memory, Unit 6 Scope Lock Clarification, Amended.**

This amendment corrects three conflicts the Constitutional Reconciliation identified in the original version of this document: (1) it had adopted an unconditional-promotion model where Contract Design V2 requires a genuine promotion gate; (2) it had silently substituted corroboration/contradiction for Contract Design V2 §5's own six named promotion-criteria categories instead of supplementing them; (3) it had let a single factor (corroboration) determine an evidential-state classification with no express, documented exception authorising that. All three are corrected below. Nothing else in the original document is altered beyond what correcting these three requires.

---

## 1. Legacy Promotion Path Remains Frozen

*(Unchanged from the original version of this document.)*

`CandidateKnowledge`, `KnowledgePromotionPolicy`, `DefaultKnowledgePromotionPolicy`, and `KnowledgePromotionDecision` remain unchanged throughout Units 6–9. They are legacy compatibility contracts only, preserved per `docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_SCOPE_LOCK.md` §7 (Backward Compatibility) and `docs/implementation/PROGRAMME_3_KNOWLEDGE_MEMORY_IMPLEMENTATION_PLAN.md` §5 ("none is asked to change what it already proves").

Unit 6 must not: rewrite their behaviour; weaken their tests; route any `KnowledgeCandidate` object through them; or treat them as the constitutional promotion path. Their final migration or retirement remains **Unit 10**'s own, separately governed responsibility.

---

## 2. New Constitutional Evaluator, Authorised

One new public evaluator contract, `KnowledgeCandidateEvaluator`, is authorised for the Programme 3 model. Its responsibility is now stated as **two separate decisions, not one merged decision** (correcting the original version, which described only "evaluate a `KnowledgeCandidate`" without distinguishing them):

1. **The promotion decision** — whether the candidate satisfies the promotion boundary (Section 6, below) and therefore qualifies to become a proposed `KnowledgeItem` at all.
2. **The evidential-state classification decision** — which `EvidentialState` a candidate that has already, independently, satisfied decision (1) should carry (Section 7, below).

The evaluator must not persist, store, revise, retire, or restore knowledge; must not modify Memory Core; and must not perform any Unit 7 lifecycle behaviour.

---

## 3. New Result Type, Authorised

One new closed result type, `KnowledgeCandidateEvaluation`, is authorised, with exactly two outcomes: `Promote(item: KnowledgeItem, promotion: KnowledgePromotion)` and `Reject(basis: String)`. No second result hierarchy is introduced by this amendment.

*(Amended.)* `Reject` now covers two distinguishable categories of non-promotion, and the disclosed `basis` must make clear which applies:

- **Structural or constitutional prerequisite failure** — the referenced Memory Core record cannot be resolved, access to it was not authorised, or its provenance reference is unavailable (Section 5). This is unchanged from the original version.
- **Substantive promotion-boundary failure** — the record resolves, its provenance reference is available, but the candidate does not satisfy the promotion criteria Section 6 names (for example: no reachable factor establishes sufficient support, and no constitutionally recognised exception applies). This category did not exist in the original version of this document and is restored by this amendment, per the Constitutional Reconciliation's Issue 1 finding that Contract Design V2 §3 and §5 require it ("A candidate that is not promoted produces no Knowledge Item and no Knowledge Memory record at all").

In both categories, the basis must disclose *why* the candidate was not promoted without declaring the underlying Memory Core evidence itself false, mistaken, or invalid — rejection here is a durability-worthiness judgment, never a verdict on the evidence (Contract Design V2 §1, "Truth").

---

## 4. Evaluation Versus Persistence

*(Unchanged from the original version.)*

Unit 6 evaluates the candidate, constructs the proposed immutable `KnowledgeItem`, constructs the associated immutable `KnowledgePromotion`, and returns both through `KnowledgeCandidateEvaluation.Promote`, only when the promotion decision (Section 6) is independently satisfied. Unit 6 does not persist either object — no call to `KnowledgeStore.remember`, no write to `InMemoryKnowledgeStore`, no lifecycle transition. Durable storage and lifecycle handling remain assigned to later units.

---

## 5. Memory Core Access

*(Unchanged in substance from the original version; provenance language tightened per Amendment Item 8.)*

The evaluator requires an authorised read dependency capable of resolving `MemoryCoreRecordReference` against Memory Core. `MemoryRetrieval` (`src/interfaces/MemoryCore.kt`) already exposes this without redesign: `MemoryCoreRecordReference`'s four cases (`ToEntity`, `ToDocument`, `ToAssertion`, `ToRelationship`) each dispatch directly to `MemoryRetrieval.getEntity`/`getDocument`/`getAssertion`/`getRelationship`. No duplicate Memory Core repository or provenance owner is introduced; `MemoryRetrieval` is reused unchanged.

`MemoryRetrieval`'s four `getX` methods require a `requestingPrincipalId: PrincipalId`, carried for auditability only. `KnowledgeCandidate` and `evaluate(candidate: KnowledgeCandidate)` carry no `PrincipalId`. This is resolved the same way `EventPublishingMemoryCore.publisherPrincipalId` already resolves an identical need: the default evaluator carries its own fixed, named system-identity `PrincipalId` constant, used for every audit-only read it performs.

**Provenance — availability versus validity, precisely distinguished (amended).** A resolved Entity/Document/Assertion/Relationship's own mandatory, non-nullable `provenanceId` field proves only that a **provenance reference is available** — sufficient, and only sufficient, for `ProvenanceReference` construction. It does **not** prove the referenced `Provenance` record's own **semantic validity** (for example, whether its `contentNature` or `confidence` are coherent, or whether the record itself has since been invalidated) — `MemoryRetrieval` exposes no method resolving a bare `ProvenanceId` back to its own `Provenance` record (no `getProvenance`; `findByProvenance` searches *by* provenance criteria, it does not return one *by identifier*). This Unit does not, and structurally cannot, perform semantic provenance validation, and this document does not require it to. Full provenance validation is recorded as **out of scope for Unit 6**, not as a passed check — structural non-nullability of `provenanceId` must never be represented, in code, tests, or reports, as equivalent to semantic validation of the `Provenance` record it identifies. Should a future unit add a provenance-lookup capability, this boundary should be revisited; it is not invented here.

---

## 6. Promotion Boundary — Model A, Restored

*(Substantially amended. This section replaces the original version's "Evidential Factors, Authorised Categories" and corrects the Constitutional Reconciliation's Issue 1 and Issue 2 findings.)*

**The promotion decision procedure is:**

```
Resolve referenced Memory Core record
        |
        v
Evaluate promotion criteria
        |
        v
   Reject
     or
   Promote
        |
        v
Assign EvidentialState (Section 7 — a separate, later decision)
```

A resolved record is **not** automatically promoted. Resolution (Section 5) is a structural prerequisite for evaluating promotion criteria at all — it is not itself sufficient to satisfy them.

**Authorised promotion-criteria categories.** Contract Design V2 §5 names six: repetition, importance, relevance, frequency, confidence, explicit request. This amendment expressly incorporates all six as the categories governing the promotion decision. They are not replaced, and corroboration/contradiction are not treated as a substitute list. Availability of each, under `KnowledgeCandidate`'s current shape (`evidenceReference: MemoryCoreRecordReference` only, per Unit 5) and Memory Core's current retrieval surface, is stated honestly below rather than assumed or invented:

- **Confidence** — available, partially. `Assertion.confidence: Double?` is a real, existing, directly reachable field for Assertion-kind evidence only; `Entity`, `Document`, and `Relationship` carry no confidence field of any kind. Where present, it may contribute to the promotion decision as one factor among several; it may never, by itself, determine promotion (Section 8).
- **Repetition** — no authorised source currently exists. Establishing whether a proposition has been independently recorded more than once would require a capability to search Memory Core for other records materially restating the same proposition (for example, a full-text or statement-similarity search over `Assertion.statement`). No such capability is exposed by `MemoryRetrieval` (`findByMetadata` searches structured metadata key/value pairs, not statement content). This Unit does not invent one.
- **Importance** — no authorised source currently exists. No Memory Core record type (`Entity`, `Document`, `Assertion`, `Relationship`) carries an importance field, and `KnowledgeCandidate` carries none either.
- **Relevance** — no authorised source currently exists, and there is a documented reason it should not be treated as a submission-time factor: the legacy `CandidateKnowledge`'s own KDoc (`src/interfaces/KnowledgeStore.kt`) explicitly excludes "any ranking or relevance score (a retrieval-time concept, not a submission-time one)." This Unit accepts that precedent rather than reversing it — relevance remains a Unit 9 (Knowledge Retrieval) concern, not a Unit 6 (promotion) input.
- **Frequency** — no authorised source currently exists, for the same reason as repetition: no capability exists to count how often a proposition has recurred across Memory Core records reachable from a single `MemoryCoreRecordReference`.
- **Explicit request** — no authorised source currently exists under the current `KnowledgeCandidate` shape. The legacy `CandidateKnowledge` carried an `explicitlyRequested: Boolean` field; the Unit 5-authored `KnowledgeCandidate` carries only `evidenceReference` and does not carry this field. This Unit cannot infer explicit-request status from unrelated metadata, and does not.

**No numeric weights, scores, thresholds, or fixed quotas are introduced for any of the above** — consistent with Contract Design V2 §5's own "weighing... not... a single structural pass/fail rule" language and with the original version of this document.

**Corroboration and contradiction — supplementary, not substitutive (amended).** Relationships of type `SUPPORTS` (corroboration) and `CONTRADICTS`/`DISPUTES` (contradiction), reachable via `MemoryRetrieval.traverseRelationships`, are constitutionally relevant and may inform the promotion decision and the evidential-state classification decision. They do **not** replace the six named categories above, and this document does not treat them as a self-sufficient factor set. Where corroboration or contradiction is the only reachable signal beyond a bare resolved record, this document does not treat that as automatically sufficient to promote (see Section 10's viability determination, which addresses whether this is enough in practice).

**Genuine rejection for insufficient promotion basis is restored.** A resolved, provenance-available candidate for which no combination of reachable, authorised factors establishes sufficient, multi-factor support may be, and must be capable of being, rejected under `KnowledgeCandidateEvaluation.Reject` — this is not limited to missing records, inaccessible records, or structural invalidity (Section 3). The rejection basis must disclose which factors were considered and why they did not establish sufficient support, without asserting that the underlying Memory Core evidence is false or invalid.

---

## 7. Evidential-State Assignment — Only After Independently Justified Promotion

*(Amended. This corrects the Constitutional Reconciliation's Issue 5 and Issue 6 findings.)*

`EvidentialState` assignment (including `UNKNOWN` and `INDETERMINATE`) is a **separate, later decision from the promotion decision (Section 6)**, made only for a candidate that has already, independently, satisfied the promotion boundary. `UNKNOWN` and `INDETERMINATE` are representational classifications that preserve uncertainty *within* durable, already-promoted knowledge — they are not substitutes for meeting the promotion criteria, and they do not themselves justify promotion. **A candidate that does not meet the promotion criteria must be rejected, never promoted merely with `UNKNOWN`.** This directly reverses the original version's Section 7, which promoted every resolved, non-contradicted, non-corroborated candidate as `UNKNOWN` by default — the Constitutional Reconciliation's Issue 5 found no support for that reading in Contract Design V2, whose §4 "Mandatory expressiveness" language concerns the representation's *capability* to express uncertainty once promoted, not a licence to promote in the absence of promotion-worthy evidence.

For a candidate that *has* independently satisfied the promotion boundary:

- A contradicted record (an unresolved `CONTRADICTS`/`DISPUTES` relationship exists) is classified `COMPETING_EXPLANATIONS` — the honest, unresolved disclosure Contract Design V2 §3 requires, never a promotion determinant by itself (Section 8).
- A corroborated, uncontradicted record may be classified `CORROBORATED_EVIDENCE` **only where corroboration is not the sole reachable factor** — see Section 8; corroboration alone does not license this classification.
- Where the reachable, authorised factors leave genuine uncertainty about the strength of an already-promoted item's support, `UNKNOWN` or `INDETERMINATE` is assigned, consistent with Article IV's own definitions, as an honest representation of that already-promoted item's uncertainty — never as a way to promote in the first place.

---

## 8. Single-Factor Prohibition — No Exception for Corroboration

*(Amended. This corrects the Constitutional Reconciliation's Issue 4 and Issue 6 findings.)*

Per Contract Design V2 §5 and Scope Lock §6 (both quoted verbatim in the Constitutional Reconciliation, Section 5): "no single factor may, by itself, determine promotion **or the resulting evidential-state classification**, absent an express, documented governing-rule exception." This prohibition applies to **both** the promotion decision (Section 6) and the evidential-state classification decision (Section 7) — it is not confined to one or the other.

**Exactly one express, documented exception exists in higher-precedence governance, and this document identifies it precisely rather than inventing a second one:** Contract Design V2 §3 ("Contradiction") permits contradiction-detection alone to trigger the `COMPETING_EXPLANATIONS` disclosure. Per the Constitutional Reconciliation's Issue 6, this exception is narrower than a general licence for single-factor decisiveness — it authorises only the *honest disclosure that the matter is unresolved*; it does not authorise contradiction to resolve anything in favour of one side, and it does not extend to any other classification.

**No comparable exception exists for corroboration.** This amendment does not create one. `CORROBORATED_EVIDENCE` must never be assigned from the presence of a single `SUPPORTS` relationship alone. It may be assigned only where a second, genuinely independent factor is also evaluated (for example, `Assertion.confidence`, where reachable, considered jointly with corroboration — never corroboration considered alone and confidence merely disclosed afterward as decorative text), **or** where a future amendment to this document, or to higher-precedence governance, expressly authorises single-factor corroboration on the same terms Contract Design V2 §3 already grants contradiction. Neither condition currently exists for corroboration; this document does not manufacture one.

---

## 9. Permission Boundary — Unit 8 Allocation Preserved

*(Unchanged in substance from the original version; restated for completeness per this amendment's own required checklist.)*

Unit 6 does not introduce independent permission wiring of any kind. It operates only on records supplied through the existing, authorised `MemoryRetrieval` boundary, which performs no principal-based filtering itself. A denial and a missing record are, at this Unit, indistinguishable, both surfacing as a resolution failure (Section 3's structural-prerequisite `Reject` category) — this is not a permission check Unit 6 performs; it is the absence of one, correctly deferred. Permission-boundary Evaluation B, per `docs/implementation/PROGRAMME_3_KNOWLEDGE_MEMORY_IMPLEMENTATION_PLAN.md` (Unit 8, "Permission-boundary wiring"), remains Unit 8's own, separately governed responsibility. This document does not claim, and no Unit 6 test may claim, that denied-permission behaviour has been fully implemented or tested — only that a resolution failure of any origin is rejected.

---

## 10. Implementation Viability Determination

*(New section, required by this amendment's own governing task.)*

Per Section 6, of the six Contract Design V2-named promotion-criteria categories, only **one** — confidence, and only for Assertion-kind evidence — has an authorised source under `KnowledgeCandidate`'s current shape and Memory Core's current retrieval surface. The other five (repetition, importance, relevance, frequency, explicit request) have no authorised source and cannot be supplied without inventing a field, a query capability, or an inference from unrelated metadata, all of which this document declines to do.

Corroboration and contradiction (Section 6) are constitutionally relevant supplementary signals, not named-category substitutes, and are themselves subject to the single-factor prohibition (Section 8), with only one narrow, disclosure-only exception (contradiction).

**Consequence:** a genuine, multi-factor promotion decision, drawing on more than one of Contract Design V2's own named categories, cannot presently be constructed for the common case (any evidence kind other than a confidence-bearing Assertion) without either (a) treating corroboration/contradiction as bearing more weight than Section 6 and Section 8 permit, or (b) inventing data Contract Design V2 §5 does not authorise. For an Assertion carrying a recorded confidence value, only one named factor (confidence) is reachable — insufficient, alone, to satisfy "weighing must consider more than one factor" (Contract Design V2 §5) even before corroboration/contradiction are added as supplementary signals.

**This document does not resolve this gap by inventing a workaround.** Consistent with this amendment's own governing task, the conclusion is: **Unit 6 cannot presently be implemented to a standard that genuinely satisfies Contract Design V2 §5's multi-factor promotion requirement using only currently authorised contracts.** Closing this gap requires either an extension to `KnowledgeCandidate` or to Memory Core's retrieval surface (a contract-level change, outside this clarification's own authority per Scope Lock §10), or a further, higher-precedence governance decision expressly authorising a narrower factor set than Contract Design V2 §5's own six categories for Unit 6 specifically. Neither currently exists.

---

## Scope of This Clarification

This document resolves the eight points the original version addressed, as amended above, plus the viability determination in Section 10. It does not authorise any change to `CandidateKnowledge`, `KnowledgePromotionPolicy`, `DefaultKnowledgePromotionPolicy`, `KnowledgePromotionDecision`, Memory Core, `KnowledgeCandidate`'s own shape, or any work belonging to Unit 7 or later. It creates no constitutional doctrine and reopens no prior Programme 3 decision beyond correcting the three conflicts the Constitutional Reconciliation identified in this document's own, prior text.

---

## Disposition

*(Amended.)* Unit 6 implementation is **not** authorised to proceed on the basis of this clarification alone. Sections 6–8 correct this document's own internal conformity with Contract Design V2 and the Scope Lock, but Section 10's viability determination finds that the currently authorised contracts (`KnowledgeCandidate`'s shape, Memory Core's retrieval surface) cannot supply enough independent, named promotion factors to implement a genuinely compliant multi-factor promotion decision. Implementation may resume only once that gap is closed by a contract-level change or a further, higher-precedence governance decision — neither of which this document performs.
