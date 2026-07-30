**Status:** Governance remediation planning only. No amendment is drafted by this document. No Kotlin is implemented, proposed, or changed. Neither `src/` nor `tests/` is touched. No other governance document is modified. Nothing is staged, committed, or pushed. This document sequences the six governance omissions `docs/reviews/PROGRAMME_3_BLOCKER_OWNERSHIP_MATRIX.md` confirmed; it does not resolve them.

# Programme 3 — Constitutional Remediation Roadmap

Programme: **Parker Constitutional Remediation Programme — Governance Amendment Planning.**

This roadmap reviewed `docs/reviews/PROGRAMME_3_CONSTITUTIONAL_AUDIT.md`, `docs/reviews/PROGRAMME_3_BLOCKER_OWNERSHIP_MATRIX.md`, `docs/reviews/PROGRAMME_3_KNOWLEDGE_PROMOTION_FACTOR_PROVENANCE.md`, `docs/governance/PROGRAMME_3_UNIT_6_SCOPE_LOCK_CLARIFICATION.md` (amended), and `docs/reviews/PROGRAMME_3_UNIT_6_CONSTITUTIONAL_RECONCILIATION.md` in full. It additionally consulted, only to confirm amendment ownership: `docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_CONTRACT_DESIGN_V2.md`, `docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_SCOPE_LOCK.md`, `docs/architecture/MEMORY_CORE_SCOPE_LOCK.md`, `docs/architecture/33-memory-consolidation.md`, and `docs/architecture/10-permission-engine.md`. The last of these is a two-line architecture stub; no dedicated, detailed Trust Framework contract-design document defining `PermissionEngine`'s own shape was located among documents already in this audit trail, and this roadmap does not broaden the review further to find one — that gap is itself disclosed under Amendment 3, below, rather than resolved by assumption.

---

## Per-Omission Ownership and Rationale

### Amendment 1 — Memory Record Comparison

- **Constitutional owner:** Memory Core.
- **Governance document requiring amendment:** `docs/architecture/MEMORY_CORE_SCOPE_LOCK.md` (and, consequentially, `docs/architecture/MEMORY_CONTRACT_DESIGN.md`, which currently defines `MemoryRetrieval`'s full method inventory without this capability).
- **Why required:** `docs/architecture/IMPLEMENTATION_GAPS.md` Gap #46 and `docs/architecture/MEMORY_CONTRACT_DESIGN.md` both assign the *comparison* responsibility to Knowledge Memory's promotion evaluator but supply no *mechanism* by which Memory Core's existing record population can be searched for material similarity. Without this amendment, repetition and frequency (Contract Design V2 §5) remain permanently unusable as promotion factors.
- **Prerequisite amendments:** none.
- **Downstream documents affected:** `docs/governance/PROGRAMME_3_UNIT_6_SCOPE_LOCK_CLARIFICATION.md` §6 (would need updating once the capability exists, to authorise its use); `docs/implementation/PROGRAMME_3_KNOWLEDGE_MEMORY_IMPLEMENTATION_PLAN.md` §6 (Unit 6/7 verification strategy references this capability).

### Amendment 2 — Provenance Lookup by Identifier

- **Constitutional owner:** Memory Core.
- **Governance document requiring amendment:** `docs/architecture/MEMORY_CORE_SCOPE_LOCK.md` / `docs/architecture/MEMORY_CONTRACT_DESIGN.md`.
- **Why required:** `MemoryRetrieval` exposes `findByProvenance` (search *by* provenance criteria) but no method resolving a bare `ProvenanceId` back to its own `Provenance` record. `docs/governance/PROGRAMME_3_UNIT_6_SCOPE_LOCK_CLARIFICATION.md` §5 (amended) already documents this as a disclosed, non-blocking gap for confidence/`ContentNature` access; it remains a genuine absence in frozen Memory Core governance, never recorded even as a deferred open question in `docs/architecture/MEMORY_CORE_SCOPE_LOCK.md` §8.
- **Prerequisite amendments:** none.
- **Downstream documents affected:** `docs/governance/PROGRAMME_3_UNIT_6_SCOPE_LOCK_CLARIFICATION.md` §5–§6 (the disclosed gap language would need updating once resolved).

### Amendment 3 — Knowledge Submission Permission Gating

- **Constitutional owner:** Runtime (per `docs/architecture/MEMORY_CORE_SCOPE_LOCK.md`: "Runtime owns every `PermissionEngine.evaluate` call required before any `MemoryCore` write..."), jointly with Programme 3's own Contract Design V2 §7 (Evaluation B).
- **Governance document requiring amendment:** `docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_CONTRACT_DESIGN_V2.md` §7, to specify Evaluation B's actual mechanism; and whichever document constitutes the Trust Framework's own frozen `PermissionEngine` contract. This roadmap could not conclusively identify that second document (`docs/architecture/10-permission-engine.md` is a stub; `docs/architecture/AUTHENTICATION_AND_TRUST_GOVERNANCE.md` does not discuss `PermissionEngine` or `ExecutionRequest` directly) — this absence is itself disclosed as a finding, not resolved by assuming which document governs it.
- **Why required:** `src/interfaces/PermissionEngine.kt`'s sole method accepts only `ExecutionRequest`, an Execution Pipeline/Tool-invocation concept with no defined mapping to or from a Knowledge Candidate submission act; `src/composition/ParkerRuntime.kt` wires `PermissionEngine` for Execution Pipeline checks only and constructs `InMemoryKnowledgeStore()` with no permission gating at all.
- **Prerequisite amendments:** none.
- **Downstream documents affected:** `docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_CONTRACT_DESIGN_V2.md` §7; `docs/implementation/PROGRAMME_3_KNOWLEDGE_MEMORY_IMPLEMENTATION_PLAN.md` Unit 8's own objective text.

### Amendment 4 — User Importance

- **Constitutional owner:** No constitutional owner currently exists.
- **Governance document requiring amendment:** `docs/architecture/33-memory-consolidation.md` (the origin of the two-word label) and, consequentially, `docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_CONTRACT_DESIGN_V2.md` §5 (which inherits the label without adding definition).
- **Why required:** no document reviewed anywhere in this audit trail defines what "user importance beyond an explicit request" means operationally, or assigns it to any subsystem. This amendment must define the concept and assign an owner before any further step is possible — it is the only omission of the six where a concept, not merely a mechanism, is missing.
- **Prerequisite amendments:** none.
- **Downstream documents affected:** `docs/governance/PROGRAMME_3_UNIT_6_SCOPE_LOCK_CLARIFICATION.md` §6 (would gain a newly usable factor once defined).

### Amendment 5 — Explicit Request

- **Constitutional owner:** Knowledge Memory.
- **Governance document requiring amendment:** `docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_CONTRACT_DESIGN_V2.md` §2.
- **Why required:** §2's own text ("carries a reference to existing Memory Core evidence and nothing else evidential") is genuinely ambiguous between excluding only confidence/evidential-state and excluding every field beyond the evidence reference categorically. Until resolved, whether `KnowledgeCandidate` may lawfully carry an explicit-request field — which its legacy predecessor, `CandidateKnowledge`, already did — cannot be determined.
- **Prerequisite amendments:** none.
- **Downstream documents affected:** `docs/governance/PROGRAMME_3_UNIT_6_SCOPE_LOCK_CLARIFICATION.md` §6 (currently lists this factor as unavailable under the current `KnowledgeCandidate` shape; would need updating).

### Amendment 6 — Common-Origin Determination

- **Constitutional owner:** Knowledge Memory (`docs/reviews/EPISTEMIC_INTEGRITY_CONSTITUTIONAL_REMEDIATION_PLAN.md`: "Article XI independence/corroboration tracking | CT-EI-49 | Knowledge Memory (Programme 3) / World Model (Programme 5), split by subsystem").
- **Governance document requiring amendment:** `docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_SCOPE_LOCK.md` §6, which states the requirement ("Repetition and frequency must never be treated as independent corroboration without first determining whether repeated mentions share a common origin") without defining a mechanism.
- **Why required:** no document defines what data or algorithm establishes "common origin" between two records or relationships. Using repetition, frequency, or relationship-based corroboration as a promotion factor without this mechanism would violate Article XI as directly as the single-factor defect the amended Unit 6 Clarification already corrected.
- **Prerequisite amendments:** **Amendment 1 (Memory record comparison).** The common-origin check is applied to whichever population of "repeated mentions" Amendment 1's own mechanism identifies (`docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_SCOPE_LOCK.md` §6's own wording: mentions must be checked for common origin once they have been identified as repeated at all). Defining the check before the mechanism that produces its input exists would risk designing against an unknown shape.
- **Downstream documents affected:** `docs/governance/PROGRAMME_3_UNIT_6_SCOPE_LOCK_CLARIFICATION.md` §6, §8 (both currently disclose repetition/frequency/corroboration weighing as constrained by this exact gap).

---

## Amendment Sequencing

| Phase | Amendment | Owner | Depends On | Unlocks |
| --- | --- | --- | --- | --- |
| 1 | Memory record comparison | Memory Core | None | Legitimate repetition/frequency data; is itself the prerequisite input Amendment 6 needs |
| 1 | Provenance lookup by identifier | Memory Core | None | `ContentNature` as a usable Unit 6 factor |
| 1 | Knowledge submission permission gating | Runtime / Trust Framework | None | Unit 8 (Evaluation B) |
| 1 | User importance | No constitutional owner currently exists (to be assigned by this amendment) | None | Use of this named Contract Design V2 §5 factor in Unit 6 |
| 1 | Explicit request | Knowledge Memory | None | Use of this named Contract Design V2 §5 factor in Unit 6, restoring legacy-equivalent submission fidelity |
| 2 | Common-origin determination | Knowledge Memory | Memory record comparison (Phase 1) | Constitutionally sound use of repetition/frequency (and relationship-based corroboration) weighing in Units 6 and 7 |

Every amendment above appears exactly once. The five Phase 1 amendments have no dependency on one another and may proceed in any order or in parallel; Phase 2's single amendment cannot be soundly designed before Phase 1's Memory record comparison amendment exists, for the reason stated under Amendment 6 above.

---

## Programme Restart Point

```
after Phase 1
```

**Support from reviewed documents:** `docs/architecture/IMPLEMENTATION_GAPS.md` Gap #46 already establishes, as accepted project precedent, that a promotion evaluator implementing fewer than all six Chapter 33 factors is legitimate provided the narrower baseline is "an intentional, non-placeholder minimal baseline," explicitly disclosed rather than silently worked around ("`DefaultMemoryPromotionPolicy` implements only 2 of `33-memory-consolidation.md`'s six named promotion factors... This is not a defect... It is a real, disclosed limitation... not of the `MemoryPromotionPolicy` interface itself"). `docs/governance/PROGRAMME_3_UNIT_6_SCOPE_LOCK_CLARIFICATION.md` (amended) already applies this identical pattern to `ContentNature`. Once Phase 1 completes, Unit 6 would have four legitimately usable factors — confidence (already available), `ContentNature` (Amendment 2), explicit request (Amendment 5), and user importance (Amendment 4) — sufficient for genuine multi-factor weighing (Contract Design V2 §5) without touching repetition, frequency, or relationship-based corroboration at all. Repetition/frequency weighing must remain explicitly, disclosedly deferred until Phase 2 (Amendment 6) also completes, exactly as `docs/governance/PROGRAMME_3_SCOPE_LOCK.md` §6's own common-origin requirement demands before that specific weighing may occur. Unit 8 additionally requires its own Phase 1 item (Amendment 3) before it, specifically, may resume — this does not change the Programme-level restart point, since Unit 8 is a distinct unit from Unit 6/7 and was already found, in `docs/reviews/PROGRAMME_3_CONSTITUTIONAL_AUDIT.md` Part 4, not to depend on Unit 6's own resolution.

---

## Risk Assessment

| Amendment | Constitutional risk if omitted | Implementation risk if deferred | Likelihood of affecting completed Units 1–5 |
| --- | --- | --- | --- |
| Memory record comparison | Moderate — repetition/frequency (two of six named factors) remain permanently unusable, and Unit 7's re-evaluation logic inherits the same gap | Low for Unit 6's own resumption (can proceed on other factors, disclosed); grows for Unit 7 if never resolved | None — no completed unit implements or tests this capability |
| Provenance lookup by identifier | Moderate — `ContentNature`, a factor Contract Design V2 §5 names directly, remains unreachable indefinitely | Low — already treated as a disclosed, non-blocking gap by the amended Unit 6 Clarification | None |
| Knowledge submission permission gating | High if permanently omitted — Evaluation B, a named Contract Design V2 §7 guarantee, would never exist, leaving Knowledge Candidate submission permanently ungated by any check | Moderate-to-high, but isolated to Unit 8 specifically; does not block Units 6/7/9/10 | None — no completed unit touches submission-time permission gating |
| User importance | Moderate — a named Contract Design V2 §5 factor remains permanently unusable until defined | Low for immediate resumption (other factors suffice, disclosed); persists indefinitely without a plan | None |
| Explicit request | Moderate — a factor the legacy path already implemented remains unavailable on the new path, and the Contract Design V2 §2 ambiguity remains live for any future work touching `KnowledgeCandidate` | Low-moderate — usable as one of several factors once resolved, but the ambiguity itself is a standing interpretive risk until closed | **Low but nonzero — the only amendment among the six whose target type (`KnowledgeCandidate`) is a Unit 5 deliverable already covered by committed, verified tests; any change to it must be additive and must not touch Unit 5's own frozen test assertions** |
| Common-origin determination | Moderate-to-high if the gap is exercised without resolution — using repetition, frequency, or relationship-based corroboration without this check would reproduce exactly the "undeclared single-factor/corroboration" defect Article XI and Amendment 1 exist to prevent | Low now (repetition/frequency weighing can be deferred whole, per the restart-point analysis above); rises sharply if Unit 7's revision logic is ever built to consume repetition/frequency before this amendment lands | None |

---

## Final Recommendation

```
Begin Governance Amendment Phase 1
```

All six omissions have a determined (or, for one, explicitly "no owner currently exists, to be assigned") constitutional owner, a named governance document to amend, and a documented reason grounded in the existing audit trail. Five of the six have no interdependency and can proceed in any order or in parallel; the sixth (common-origin determination) has exactly one prerequisite, itself inside Phase 1. No further constitutional investigation is required to begin — the open question this roadmap could not resolve (which document governs `PermissionEngine`'s own detailed contract) is narrow, confined to Amendment 3 alone, and does not block beginning the other four Phase 1 amendments or Phase 1 work generally.

---

## Final Report

**File created:** `docs/governance/PROGRAMME_3_CONSTITUTIONAL_REMEDIATION_ROADMAP.md` (only file created; no other file modified).

**Governance omissions planned:** 6 (Memory record comparison; Provenance lookup by identifier; Knowledge submission permission gating; User importance; Explicit request; Common-origin determination).

**Remediation phases:** 2 — Phase 1 (five independent amendments); Phase 2 (one amendment, dependent on Phase 1's Memory record comparison).

**Implementation restart point:** after Phase 1, for Units 6, 7, 9, and 10 (using the four factors Phase 1 unlocks, with repetition/frequency/relationship-based corroboration explicitly and disclosedly deferred to Phase 2, mirroring accepted project precedent); Unit 8 also requires its own Phase 1 item specifically before it may resume, independent of Units 6/7's own status.

**Risk summary:** no amendment carries more than a low, nonzero risk to completed Units 1–5; the one exception (Explicit Request) touches a Unit 5 deliverable directly and must be handled additively. Knowledge Submission Permission Gating carries the highest standalone constitutional risk if permanently omitted (Evaluation B would never exist); Common-Origin Determination carries the highest risk if its prerequisite is resolved but it itself is skipped and repetition/frequency weighing is attempted anyway.

PROGRAMME 3 CONSTITUTIONAL REMEDIATION ROADMAP COMPLETE

Confirmed: no production code modified; no tests modified; no governance documents modified except this newly created roadmap; nothing staged; nothing committed; nothing pushed; Unit 7 not started.
