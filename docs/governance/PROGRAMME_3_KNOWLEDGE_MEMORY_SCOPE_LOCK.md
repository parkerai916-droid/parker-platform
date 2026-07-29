**Status:** Formal governance — Scope Lock. Final governance document before implementation planning. No Kotlin is implemented, proposed as a diff, or changed by this document. Neither `src/` nor `tests/` is touched. Nothing is staged, committed, or pushed.

# Programme 3 — Knowledge Memory Scope Lock

Programme: **Programme 3 — Knowledge Memory Scope Lock.**

**This document is binding.** It does not redefine any decision already made. `docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_GOVERNANCE_REVIEW.md`, `docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_CONTRACT_DESIGN_V2.md`, `docs/reviews/PROGRAMME_3_KNOWLEDGE_MEMORY_ADVERSARIAL_CONTRACT_REVIEW.md`, `docs/reviews/PROGRAMME_3_KNOWLEDGE_MEMORY_FINAL_ADVERSARIAL_CONFIRMATION.md`, `docs/reviews/EPISTEMIC_INTEGRITY_CONSTITUTIONAL_REMEDIATION_PLAN.md`, `docs/architecture/epistemic-integrity.md`, `docs/architecture/MEMORY_CORE_GOVERNANCE_REVIEW.md`, and `docs/architecture/MEMORY_CORE_SCOPE_LOCK.md` are frozen, normative inputs — this document does not reopen the layering, the terminology, the public model, the eight resolved amendments, or any constitutional question those documents already settled. Its own purpose is narrower and final, mirroring `MEMORY_CORE_SCOPE_LOCK.md`'s own precedent exactly: fix, without ambiguity, what Programme 3 builds, and what it does not. Every capability considered below is marked `IN SCOPE` or `OUT OF SCOPE`. There is no third category. Where a candidate capability is plausible, useful, or eventually necessary but not required to satisfy Programme 3's own objective (Section 2), it is `OUT OF SCOPE` — the burden of proof favours exclusion, not inclusion, throughout this document.

---

## 1. Executive Summary

Governance for Programme 3 is complete. The sequence — Governance Review, Contract Design Version 1, Adversarial Contract Review, Contract Design Version 2, Final Adversarial Confirmation — concluded with an independent, final decision of `READY FOR SCOPE LOCK`, itself confirming that all eight findings from the Adversarial Review were resolved in Version 2, that no constitutional guarantee was weakened, that no responsibility leaked across any external boundary, and that the sole remaining implementation-level question (Section 8, below) does not constitute a constitutional blocker. The contract is confirmed. Implementation is authorised, strictly within the boundary this document now fixes.

---

## 2. Programme Objective

**Programme 3 exists to replace Parker's legacy, provenance-free `MemoryStore` with Knowledge Memory — the evaluation-and-promotion layer built atop Memory Core — so that the durable knowledge Parker's reasoning eventually operates upon carries provenance, an honest evidential-state classification, and a non-erasing historical record, without weakening any existing guarantee and without duplicating any responsibility Memory Core already owns.**

This objective is fixed and shall not change during implementation. It does not include making Knowledge Memory the subsystem Reasoning Context actually reads from at runtime — that cutover is a Programme 4 act (Section 4). Programme 3's objective is to deliver a complete, tested, constitutionally compliant Knowledge Memory that Programme 4 can then adopt.

---

## 3. Scope

Each item below is `IN SCOPE`, with the specific governance basis that puts it there — nothing is included merely because it is adjacent or convenient.

| In scope | Basis |
| --- | --- |
| The full Knowledge Memory contract set — Knowledge Item, Knowledge Reference, Knowledge Candidate, Knowledge Promotion, Knowledge Query, Knowledge Result, the Knowledge Submission interface, the Knowledge Retrieval interface | Designed in full by Contract Design Version 2 §12, confirmed by the Final Adversarial Confirmation §1, §4, §5 |
| The `MemoryStore` → Knowledge Memory rename, at both the documentation/vocabulary level and, in this Programme, the Kotlin identifier level | Reconciliation §13's canonical terminology, scheduled explicitly as Programme 3's own step by Reconciliation §16 step (4) and Governance Review §10 |
| Adaptation of the promotion/submission path to reference real Memory Core Provenance, Entity, Document, and Evidence records instead of the legacy flat, duplicate fields | Reconciliation §8, §14; Governance Review §2, §4 |
| The promotion pipeline, implemented to satisfy Contract Design Version 2's binding multi-factor, independence-aware weighing requirement and its confidence-sourcing restriction | Contract Design Version 2 §5 (Amendments 1 and 2), confirmed non-weakened by the Final Adversarial Confirmation §5 |
| Housing and expressing the Article IV evidential-state classification at the Knowledge Memory layer, including the mandatory capability to express an insufficiently-supported/unresolved outcome | Governance Review §3; Contract Design Version 2 §4 (Amendment 3) |
| The knowledge lifecycle — promotion, revision, supersession, contradiction, retirement, and restoration — implemented to satisfy Version 2's ordering, non-forking, per-revision-disclosure, and non-deletive-retirement requirements | Contract Design Version 2 §3 (Amendments 4 and 5) |
| The provenance-reference mechanism, implemented as a minimal, immutable, non-content-bearing pointer | Contract Design Version 2 §6 (Amendment 6) |
| The staleness-disclosure mechanism, mandatory on every Knowledge Result | Contract Design Version 2 §3, §6 (Amendment 7) |
| The Knowledge Memory-side permission evaluation (Evaluation B), wired to inherit, and never re-litigate, Memory Core's own Evaluation A | Contract Design Version 2 §7 (Amendment 8) |
| Verification that the existing `MemoryStore`/`InMemoryMemoryStoreTest.kt` test suite remains valid, unmodified in substance, as Knowledge Memory's own test suite once the rename is carried out | Reconciliation §14 |
| Delivery (not consumption) of the Knowledge Retrieval surface, built so that Programme 4 can adopt it without further Knowledge-Memory-side redesign | Governance Review §10: "Programme 3 does not implement Reasoning Context's own consumption of that surface — it delivers a surface Programme 4 can consume" |

---

## 4. Explicit Exclusions

Every item below is `OUT OF SCOPE` for Programme 3, with the reason stated directly, not merely asserted:

| Excluded capability | Reason |
| --- | --- |
| World Model implementation or modification of any kind | Permanently and structurally independent of the whole Memory family, in both directions (Reconciliation §10; Governance Review §7; Contract Design Version 2 §9). Not deferred — excluded. |
| Document Intelligence, OCR, PDF parsing, file import, image analysis | Belongs to a future Document Handling/Document Intelligence Programme (`MEMORY_CORE_SCOPE_LOCK.md` §4; Governance Review §8; Contract Design Version 2 §10). Knowledge Memory accepts whatever Memory Core presents, regardless of origin, and implements no document-specific path. |
| Embeddings, vector search, or any semantic/similarity-based retrieval | Never designed anywhere in the frozen contracts; explicitly excluded at both the Memory Core layer (`MEMORY_CORE_SCOPE_LOCK.md` §4, §10) and the Knowledge Memory layer (Contract Design Version 2 §13). Knowledge Retrieval performs structural matching against caller-supplied criteria only. |
| Planner integration | Not named as a Knowledge Memory responsibility in any frozen document; Contract Design Version 2 §13 excludes it explicitly. The Planner's own existing, unchanged relationship to today's `MemoryStore` (direct consultation) is not re-examined or altered by this Programme. |
| Workflow Engine integration | Not named as a Knowledge Memory responsibility anywhere; Contract Design Version 2 §13 excludes it explicitly. |
| Representation Engine work — provenance disclosure, uncertainty disclosure, or any user-facing composition of Knowledge Memory content | Governance Review §9; Contract Design Version 2 §1. Knowledge Memory makes state retrievable; it never composes explanatory or user-facing text. Reserved for a future Representation Engine Programme (Remediation Plan §12, Programme 7). |
| Reasoning Context's own consumption of Knowledge Memory — wiring `DefaultReasoningContextAssembler` (or its successor) onto the new retrieval surface, and any resulting change to what Reasoning Context actually reads at runtime | Explicitly allocated to Programme 4 by Governance Review §10 and the Remediation Plan §9 Stage 3. Programme 3 delivers the surface; it does not perform the cutover. |
| Any agent-reasoning, planning, or execution-pipeline change beyond the Knowledge Memory contracts and the adapter work named in Section 3 | No such change is authorised by any frozen document; introducing one here would be exactly the kind of unjustified scope expansion this Programme's own precedent (`MEMORY_CORE_SCOPE_LOCK.md`) exists to prevent. |
| Any redesign of Memory Core's own contracts, permission boundary, lifecycle, retrieval modes, or event scope | Frozen by `MEMORY_CORE_SCOPE_LOCK.md` and not reopened by Programme 3 under any circumstance; Knowledge Memory references Memory Core, and referencing something is not grounds to revise it. |
| Any new constitutional doctrine, article, or Constitutional Test | Epistemic Integrity is ratified and constitutionally frozen. Programme 3 satisfies existing Articles; it does not propose new ones. |
| Any capability reserved for a later Programme not otherwise named above — including persistence technology beyond an in-memory implementation, network synchronisation, cloud storage, and external database integration | No concrete requirement for any of these has been identified in any frozen document; introducing one now would commit this Programme to unjustified technology or infrastructure decisions, mirroring `MEMORY_CORE_SCOPE_LOCK.md` §4's own identical exclusions for Memory Core. |

---

## 5. Deliverables

Every deliverable below is already authorised by one or more of the frozen governance documents; none is introduced here for the first time.

1. **Knowledge Memory contract set** — the eight contracts inventoried in Contract Design Version 2 §12, implemented in full.
2. **`MemoryStore` → Knowledge Memory rename and adapter** — Kotlin identifiers renamed per Reconciliation §13's canonical terminology; the submission path adapted to reference real Memory Core records (Section 3).
3. **Promotion pipeline** — implementing Version 2's binding multi-factor, independence-aware weighing constraint and confidence-sourcing restriction (Amendments 1, 2).
4. **Evidential-state representation** — capable, as a structural requirement, of expressing an insufficiently-supported/unresolved outcome, not only graduated confidence (Amendment 3).
5. **Knowledge lifecycle implementation** — promotion, ordered non-forking revision with a per-revision disclosure record, multi-hop supersession, contradiction preservation, non-deletive and selectively reversible retirement (Amendments 4, 5).
6. **Provenance-reference mechanism** — minimal, immutable, identifier-only, with Memory Core's own retrieval surface remaining directly reachable (Amendment 6).
7. **Staleness-disclosure mechanism** — mandatory on every Knowledge Result (Amendment 7).
8. **Permission-boundary wiring** — Evaluation B implemented at Knowledge Memory's own submission boundary, inheriting and never re-litigating Memory Core's own Evaluation A (Amendment 8).
9. **Retrieval pipeline** — the Knowledge Query/Knowledge Result surface, delivered complete and ready for Programme 4's own future adoption, but not itself wired into Reasoning Context by this Programme (Section 4).
10. **Regression verification** — confirmation that the existing `MemoryStore` test suite remains valid, unmodified in substance, as Knowledge Memory's own suite (Reconciliation §14).

No deliverable outside this list is authorised. A Programme 3 that ships a subset of the above and defers the remainder is a different, smaller scope requiring a Scope Lock revision before proceeding, mirroring `MEMORY_CORE_SCOPE_LOCK.md` §3's own "all mandatory deliverables, no optional or partial deliverable" discipline.

---

## 6. Constitutional Requirements

Every obligation below is binding on implementation, each traced to the Article it satisfies:

- **Article IV (Evidential Representation).** Knowledge Memory's evidential-state classification must be structurally capable of expressing an insufficiently-supported/unresolved outcome, never only graduated confidence.
- **Article VI (Evidential Sufficiency) / Article VII (Burden of Justification).** Promotion is never a truth determination; a Knowledge Item's classification expresses how well Memory Core evidence supports a proposition, never whether it is true. Weighing must consider more than one factor; no single factor may, by itself, determine promotion or classification absent an express, documented, Article-XI-conditioned exception.
- **Article VIII (Provenance and Evidential History) / Article IX (Integrity of Evidence).** Knowledge Memory never owns, copies, or duplicates provenance. Every provenance reference is minimal (identifier-only) and immutable once issued; Memory Core's own retrieval surface remains directly reachable for full provenance detail.
- **Article XI (Evidential Weight and Independence).** Repetition and frequency must never be treated as independent corroboration without first determining whether repeated mentions share a common origin.
- **Article XIII (Transparency of Uncertainty).** Uncertainty must never be concealed: the evidential-state representation must be able to express genuine non-resolution, and every Knowledge Result must carry a mandatory staleness disclosure when the evidence behind a returned item has changed status since its classification was last computed.
- **Article XV (Constitutional Separation of Reasoning and Representation).** No subsystem determines its own evidential status: a Knowledge Candidate must never carry a caller-supplied confidence or evidential-state value; both are computed exclusively by Knowledge Memory, from Memory Core's own recorded evidence or Knowledge Memory's own independent evaluation.
- **Article XVI (Temporal Integrity) / Article XVII (Revision of Knowledge).** A Knowledge Item's classification history is a single, chronologically ordered, non-forking sequence; every revision, not only the initial promotion, produces its own disclosed basis record; nothing is ever overwritten in place.
- **Article XVIII (Correction of Error), applied to retirement.** Retirement never implies deletion. Reversal is expressed as a new, visible restoration event appended to history, never as an erasure of the fact that retirement occurred — except where the underlying Memory Core evidence was itself permanently erased through the owner-requested deletion path, in which case only a new promotion, never a restoration, is possible.
- **Parker Constitution's trust-authorises discipline.** Two permission evaluations exist with non-overlapping responsibilities (Evaluation A over evidence, Evaluation B over the submission act); Evaluation B never re-litigates Evaluation A's already-settled outcome.

---

## 7. Non-Functional Requirements

- **Determinism.** A given Knowledge Query, issued twice against unchanged Knowledge Memory state, must return identical results both times — mirroring `MEMORY_CORE_SCOPE_LOCK.md` §11's own repeatable-retrieval guarantee, applied here for the first time to Knowledge Memory explicitly (identified as a completeness gap by the Adversarial Review §8 and closed here as a binding requirement).
- **Provenance preservation.** Every Knowledge Memory record references Memory Core provenance; none copies, summarises, or embeds provenance content.
- **Historical preservation.** Every lifecycle event (promotion, revision, supersession, retirement, restoration) is a new, appended act; no operation mutates an existing classification or disclosure record in place.
- **Permission integrity.** Evaluation A and Evaluation B remain structurally distinct and non-overlapping for the lifetime of the implementation; no future change may collapse them into one check or allow either to be silently bypassed.
- **No duplicate sources of truth.** Knowledge Memory holds no independent copy of identity, provenance, or relationships; every such reference is by identifier only, resolved against Memory Core at the point of need.
- **No hidden ownership transfer.** Provenance ownership remains with Memory Core regardless of how frequently a Knowledge Memory-issued reference is the practical path by which a caller reaches it; Memory Core's own retrieval surface must remain directly reachable and must not be deprecated, hidden, or discouraged as a side effect of this Programme.
- **Backward compatibility.** The existing `MemoryStore` test suite must remain valid, unmodified in substance, and passing throughout the rename and adaptation; today's `MemorySource.recall` read path, which is genuinely production-wired, must not be broken by any Programme 3 deliverable, even though its cutover to Knowledge Memory is a Programme 4 act performed after, and separately from, this Programme's own work.

---

## 8. Open Implementation Questions

**Exactly one, carried forward from the Final Adversarial Confirmation, and no other.** No additional open question is introduced by this Scope Lock.

**Concurrent re-evaluation versus serialized ordering.** Contract Design Version 2 §3 requires that where more than one piece of new Memory Core evidence bearing on the same Knowledge Item arrives concurrently, the result must be a single, consistently ordered classification, not a fork — but leaves open whether this is achieved by evaluating the combined evidence jointly, in one atomic act, or by applying a defined sequential ordering rule. The Final Adversarial Confirmation identified this as a genuine wording tension, not previously flagged, and determined:

- **It is not a constitutional blocker.** Whichever mechanism is chosen, Amendment 1's own standing, generally-worded prohibition — no single factor may, by itself, determine promotion or the resulting evidential-state classification — already governs it. A sequential ordering rule that functioned as a disguised single-factor determinant would violate that standing prohibition regardless of this open question's resolution.
- **It must be resolved consistently during implementation.** Exactly one mechanism (joint evaluation, or a specific, disclosed sequential ordering rule) must be chosen and applied uniformly; an implementation may not vary its approach by code path, record type, or circumstance.
- **Whichever implementation is chosen must not violate the constitutional prohibition against implicit weighting.** If a sequential ordering rule is chosen, it must be justified and disclosed on the same terms Article XI §1 requires of any express governing-rule exception — it may never be adopted silently, as a mere implementation convenience.

---

## 9. Success Criteria

Programme 3 is complete only when all of the following are objectively true:

- **Implementation is complete.** Every deliverable named in Section 5 exists, compiles, and satisfies every requirement named in Sections 6 and 7.
- **Tests pass.** The verified, unmodified `MemoryStore` test suite continues to pass as Knowledge Memory's own suite, and new tests exist proving each of the eight constitutional requirements in Section 6 individually, not merely proving the code compiles or runs without error.
- **Constitutional behaviour is demonstrated, not merely claimed.** A re-run of the Executable Test Compliance Audit's own methodology against the relevant Constitutional Tests (principally the Memory Core PARTIALLY ENFORCED group and any newly applicable Knowledge Memory-specific tests) shows measurable, evidenced closure of the findings the Constitutional Remediation Plan scheduled to this Programme — never inferred from naming, architecture documents, or successful compilation alone.
- **Migration from `MemoryStore` is complete.** The rename and adapter work (Section 5, item 2) has landed in full; no code path continues to construct or depend on the legacy flat, duplicate-field submission shape.
- **Knowledge Memory is proven ready to become the active reasoning source.** Consistent with Section 4's own exclusion, Programme 3's own completion criterion is that Knowledge Memory is a fully built, tested, and adapter-complete replacement for `MemoryStore`'s current production role — verifiably equivalent in behaviour and ready for adoption. The act of actually making Knowledge Memory the subsystem Reasoning Context reads from at runtime is executed under Programme 4's own, separately governed scope; Programme 3's completion is a precondition for that cutover, not the cutover itself.

---

## 10. Out-of-Scope Change Policy

Any implementation proposal that:

- expands this Programme's scope beyond Section 3,
- weakens any constitutional guarantee named in Section 6,
- introduces a new subsystem responsibility not named in Section 3 or Section 5, or
- changes any public contract Contract Design Version 2 froze,

**requires formal governance before implementation** — a Scope Lock revision, following the same review discipline this Programme itself just completed, never an implementation-level decision made under the belief that it is a small, load-bearing exception. This mirrors `MEMORY_CORE_SCOPE_LOCK.md` §6's own identical discipline, applied here without modification.

---

## 11. Implementation Authority

Implementation teams are authorised to begin engineering work strictly within the boundary this Scope Lock fixes. Implementation must not reinterpret governance: where this document, or any document it treats as frozen, is genuinely ambiguous about a question that matters to compliance, that is grounds to pause and request a Scope Lock revision — never grounds to resolve the ambiguity unilaterally under implementation discretion, and never grounds to treat silence as permission. The one open implementation question this document records (Section 8) is the sole exception, and even it is bounded: a mechanism must be chosen and applied consistently, but the choice itself is authorised, not merely tolerated, by this document.

---

## Final Recommendation

**Programme 3 is authorised to enter implementation planning.**
