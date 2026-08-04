# Programme 4 (Evidence Intelligence) — Unit 7 Dependency Record

## Status

**Permanent architectural dependency record. Historical and architectural only.** This document does not reopen governance, does not propose a new contract, does not propose a new interface, and does not redesign Programme 3. It records why Programme 4 Unit 7 ("The Evidence Intelligence Acceptance Coordinator") cannot yet be completed, and precisely what must exist before it can be. No governance document, implementation plan, contract design, or scope lock is modified by this record. No Kotlin is implemented or changed. No test is modified. Nothing is staged, committed, or pushed.

Repository baseline: `HEAD 7a62fac`, working tree clean of tracked-file changes at the time of writing.

---

## 1. Background

Unit 7 is the Evidence Intelligence Programme's acceptance-orchestration responsibility, frozen by `docs/architecture/EVIDENCE_INTELLIGENCE_SCOPE_LOCK.md` §6 step 4 and `docs/implementation/EVIDENCE_INTELLIGENCE_IMPLEMENTATION_PLAN.md` §6 item 5/§8 Unit 7. It is a single, concrete, non-interface-backed, stateless coordinator that:

- consumes the `List<EvidenceAnalysisResult>` Unit 5's `EvidenceIntelligence.analyse` operation returns for one invocation, unchanged;
- dispatches each candidate to the one existing acceptance interface its own kind names — `EvidenceCustodian.accept` for a candidate derivative artefact, `MemoryCore`'s public write interface for a candidate Assertion/Relationship, and Knowledge Memory's Knowledge Submission interface for a Knowledge Candidate;
- enforces that a proposed Memory Core record is accepted, and assigned a governed identifier, before any Knowledge Candidate referencing it is submitted;
- passes every acceptance call through the Permission Engine gate that interface's own contract already enforces;
- holds no custody, truth, promotion, deletion, or evidential-state authority of its own — it is a sequencing mechanism only, never a fourth authority alongside Evidence Custodian, Memory Core, and Knowledge Memory.

This responsibility, its exact boundaries, its dependency list, and its ownership were established in full by `docs/reviews/EVIDENCE_INTELLIGENCE_UNIT_7_PLANNING_AND_BOUNDARY_REVIEW.md`, which this record does not restate beyond what is necessary to state the dependency precisely.

---

## 2. Repository Verification

The following searches were performed against the repository at `HEAD 7a62fac`, across both `src/` and `docs/`:

| Search term | Result |
|---|---|
| `KnowledgeSubmission` | Zero matches in `src/`. Zero matches in `docs/`. |
| `submitKnowledge` | Zero matches in `src/`. Zero matches in `docs/`. |
| `submitCandidate` | Zero matches in `src/`. Zero matches in `docs/`. |
| `KnowledgeCandidate` | Present in three `src/` files (`src/interfaces/KnowledgeStore.kt`, `src/runtime/DefaultKnowledgeCandidateEvaluator.kt`, `src/runtime/DefaultKnowledgeRevisionEvaluator.kt`) and twelve governance/review documents. |
| `KnowledgeCandidateEvaluator` | Present in the same three `src/` files and three governance/review documents. |
| Any persistence interface for accepted knowledge | None found. `src/runtime/InMemoryKnowledgeStore.kt` — the sole concrete Knowledge Memory store in the repository — was read in full and contains no reference to `KnowledgeCandidateEvaluator`, `KnowledgeCandidate`, `KnowledgeItem`, or `PermissionEngine` anywhere in its text. |

**Conclusions drawn directly from the above, each independently confirmed by reading the named file in full:**

- **No `KnowledgeSubmission` interface exists**, under that name or any other. No interface anywhere in `src/interfaces/` offers a `submit`-shaped operation over `KnowledgeCandidate`.
- **No `submitKnowledge` operation exists.** No method of that name, or of equivalent effect, is declared on any interface in the repository.
- **No equivalent persistence interface exists.** The only interface that persists anything under the `Knowledge` namespace is the legacy `KnowledgeStore.remember(candidate: CandidateKnowledge): KnowledgePromotionDecision` — a different, pre-constitutional model (`CandidateKnowledge`/`KnowledgeRecord`), not `KnowledgeCandidate`/`KnowledgeItem`.
- **`KnowledgeCandidateEvaluator` performs evaluation only.** Its sole concrete implementation, `DefaultKnowledgeCandidateEvaluator`, states in its own governing comment: *"This class never calls `KnowledgeStore.remember`, never writes to `InMemoryKnowledgeStore`, and never performs a lifecycle transition. `KnowledgeItem` and `KnowledgePromotion` values returned by `evaluate` are proposed, constructed values only — durable storage remains a later unit's own, separately authorised responsibility."* Its own `evaluate` method is not `suspend`, holds no `PermissionEngine` dependency, and writes nothing to any store.
- **`InMemoryKnowledgeStore` remains the legacy seam.** It implements only `KnowledgeStore` and `KnowledgeSource` — both operating exclusively on `CandidateKnowledge`/`KnowledgeRecord`. It carries no field, method, constructor parameter, or import referencing `KnowledgeCandidate`, `KnowledgeItem`, `KnowledgeCandidateEvaluator`, or `PermissionEngine`.

`docs/implementation/PROGRAMME_3_KNOWLEDGE_MEMORY_IMPLEMENTATION_PLAN.md` §3 was also read in full and confirms this is not an oversight: it names ten engineering units, of which Units 1–7 are implemented in the current repository (rename; evidential-state; provenance-reference; Knowledge Item/Promotion/Reference; Knowledge Candidate; the promotion evaluator; knowledge lifecycle), and Units 8, 9, and 10 are not yet begun. Unit 8's own stated objective is: *"implement the Knowledge Memory-side submission evaluation (Evaluation B), gating only the act of submitting a Knowledge Candidate, and wire it so it never re-evaluates Memory Core's own already-settled evidence authorization (Evaluation A)."*

---

## 3. Dependency

**Programme 4 Unit 7 depends on Programme 3 Unit 8 introducing the constitutional Knowledge Submission interface.**

This is a cross-programme sequencing dependency, not a textual ambiguity in Programme 4's own governance. `docs/architecture/EVIDENCE_INTELLIGENCE_CONTRACT_DESIGN.md` and `docs/architecture/EVIDENCE_INTELLIGENCE_SCOPE_LOCK.md` both name "Knowledge Memory's Knowledge Submission interface" as an already-existing dependency the Unit 7 coordinator simply calls, on the same footing as `EvidenceCustodian.accept` and `MemoryCore`'s public write interface. Both of those two do exist and are implemented. The third does not yet exist in any form — not as an interface, not as a partial implementation, not under a different name — and its introduction is explicitly, by name, Programme 3 Unit 8's own responsibility, not Programme 4's.

---

## 4. Constitutional Reasoning

Programme 4 must not resolve this dependency itself, for four independent reasons, each traced to already-frozen governance rather than argued anew here:

- **Must not invent a temporary interface.** `docs/architecture/EVIDENCE_INTELLIGENCE_SCOPE_LOCK.md` §4 caps Evidence Intelligence's own new public surface at exactly four types, none of which is a Knowledge Memory submission contract, and Implementation Plan §8 Unit 7 states the coordinator introduces "not a new public type" of its own. A temporary submission interface authored by Programme 4 would be a fifth, unauthorised type, and would also constitute Programme 4 defining a contract that belongs, by CDR-007's own boundary (§3, "Constitutional interfaces"), to Knowledge Memory alone.
- **Must not reuse the legacy `KnowledgeStore` seam.** `docs/governance/PROGRAMME_3_UNIT_5_SCOPE_LOCK_CLARIFICATION.md` §1 explicitly declares the legacy `CandidateKnowledge`/`KnowledgeRecord` path and the constitutional `KnowledgeCandidate`/`KnowledgeItem` path to be separate, non-overlapping seams, never adapted into one another. Routing a Knowledge Candidate through `KnowledgeStore.remember` would silently misrepresent a constitutional submission as a legacy Memory promotion, violating that explicit separation and the Constitution's own transparency principle ("Parker does not obscure what it is doing or why").
- **Must not bypass Programme 3 ownership.** CDR-007 §3 and §5 place Knowledge Memory's own promotion evaluation and submission boundary exclusively with Knowledge Memory; Evidence Intelligence "has no independent epistemic-authorisation mechanism of its own and is not authorised to build one." Constructing any path — temporary or otherwise — by which Programme 4 itself accepts, gates, or persists a Knowledge Candidate would be Programme 4 quietly assuming an authority CDR-007 assigns permanently to a different, peer subsystem.
- **Must not fabricate acceptance outcomes.** `docs/implementation/EVIDENCE_INTELLIGENCE_IMPLEMENTATION_PLAN.md` §10 items 3 and 12, and §8 Unit 7's own "Rejection"/"Implementation failure" outcomes, require that a candidate's disposition always be the accepting subsystem's own, genuine, already-existing outcome — never a substituted, invented, or optimistic stand-in. Since no genuine Knowledge Memory submission outcome yet exists to report, any coded "success" for this leg today would necessarily be fabricated, which no governing document anywhere in this Programme permits.

---

## 5. Engineering Consequence

**Unit 7 is intentionally suspended pending completion of Programme 3 Unit 8.** Two of its three acceptance legs — `EvidenceCustodian.accept` and `MemoryCore`'s public write interface — are fully implementable today, following `EvidenceRegistrationCoordinator`'s own already-established precedent exactly. The third leg has no interface to route through, and Unit 7's own frozen completion criterion — that every candidate Unit 5 can produce must reach its accepting subsystem through the coordinator — cannot be satisfied while that interface does not exist.

**Unit 8 of Programme 4 (Runtime Composition and Full Verification) therefore also remains blocked.** Implementation Plan §7 fixes Unit 8 as depending on all seven prior Programme 4 units in full, including Unit 7. A Unit 7 that cannot reach completion cannot be composed into the running system as complete, and Unit 8's own completion criteria (Implementation Plan §10) cannot be verified for a coordinator that is itself only partially built.

---

## 6. Resumption Point

The exact condition required before Unit 7 implementation resumes:

> **A constitutional Knowledge Submission interface exists and is available for orchestration.**

---

## 7. Final Verdict

**A — Dependency correctly identified.**

No governance amendment required. Implementation deferred pending upstream programme completion.

---

## 8. Confirmation No Other File Changed

No governance document, implementation plan, contract design, scope lock, production source file, or test file was modified. This record is the only file created by this task.

## 9. No Git Actions

Nothing staged, committed, or pushed. Only read-only inspection (file reads, repository search) was performed.
