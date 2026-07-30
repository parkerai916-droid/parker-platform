**Status:** Governance-only, Programme-level audit. No Kotlin is implemented, proposed, or changed by this document. Neither `src/` nor `tests/` is touched. No governance document is amended. Nothing is staged, committed, or pushed. This document identifies problems; it does not solve them.

# Programme 3 — Constitutional Audit

Programme: **Parker Constitutional Programme Audit — Programme 3 Viability and Sequencing Review.**

This audit reviewed, in full: `docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_CONTRACT_DESIGN_V2.md`, `docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_SCOPE_LOCK.md`, `docs/implementation/PROGRAMME_3_KNOWLEDGE_MEMORY_IMPLEMENTATION_PLAN.md`, `docs/governance/PROGRAMME_3_UNIT_6_SCOPE_LOCK_CLARIFICATION.md` (amended), `docs/reviews/PROGRAMME_3_UNIT_6_CONSTITUTIONAL_RECONCILIATION.md`, `docs/reviews/PROGRAMME_3_KNOWLEDGE_PROMOTION_FACTOR_PROVENANCE.md`, and `docs/reviews/EPISTEMIC_INTEGRITY_CONSTITUTIONAL_REMEDIATION_PLAN.md`. It additionally reviewed, because each was found to materially bear on Programme 3 sequencing: `docs/architecture/MEMORY_CORE_SCOPE_LOCK.md` and `docs/architecture/MEMORY_CORE_GOVERNANCE_REVIEW.md` (Memory Core); `docs/architecture/reasoning-context.md` (Reasoning Context); `docs/architecture/IMPLEMENTATION_GAPS.md`, `docs/architecture/MEMORY_CONTRACT_DESIGN.md`, `docs/architecture/MEMORY_ARCHITECTURE_RECONCILIATION.md` (Memory promotion-factor history); and the actual composition root, `src/composition/ParkerRuntime.kt`, together with `src/interfaces/PermissionEngine.kt` and `src/composition/EventPublishingMemoryCore.kt` (Runtime/Trust Framework permission wiring). `docs/architecture/23-goal-manager.md` (Goal Engine) and `docs/architecture/09-trust-framework.md` (Trust Framework's own architecture stub) were checked and found to be two-line stubs with no material content bearing on Programme 3; they are not discussed further. World Model was reviewed only to confirm the Scope Lock's own exclusion (Section 2, below); Task Engine was not found to bear materially on any remaining unit.

---

## Part 1 — Programme 3 Readiness, Unit by Unit

**Units 1–5: complete.** Implemented, locally verified, committed, and pushed per the project's own standing engineering workflow; not re-audited here beyond confirming the Implementation Plan records no outstanding obligation against them.

**Unit 6 (Promotion pipeline) — blocked by missing contracts.** `docs/governance/PROGRAMME_3_UNIT_6_SCOPE_LOCK_CLARIFICATION.md` §10 (amended) and `docs/reviews/PROGRAMME_3_KNOWLEDGE_PROMOTION_FACTOR_PROVENANCE.md` both independently conclude that only one of Contract Design V2 §5's six named promotion factors (confidence, and only for `Assertion`-kind evidence) is reachable through current contracts. Two further factors (repetition, frequency) are blocked by a missing Memory Core retrieval capability (Part 2, below); one factor (explicit request) is blocked by a Programme-3-owned contract gap (`KnowledgeCandidate` dropped the field its legacy predecessor had); one factor (relevance) is correctly excluded by design, not a gap; and one factor ("user importance" beyond explicit request) has **no constitutional owner at all** — the most severe finding of the three, since it is not merely unexposed, it is undesigned.

**Unit 7 (Knowledge lifecycle) — blocked by another unit.** `docs/implementation/PROGRAMME_3_KNOWLEDGE_MEMORY_IMPLEMENTATION_PLAN.md` §3 states Unit 7's own dependencies as "Units 4, 6." This is not merely a scheduling wait: Unit 7's re-evaluation logic re-applies "promotion criteria" (Contract Design V2 §3, "Revision") whenever new Memory Core evidence arrives — the identical criteria Unit 6 cannot presently evaluate. Unit 7 therefore inherits Unit 6's own factor-availability gap directly, not only a sequencing dependency.

**Unit 8 (Permission-boundary wiring) — blocked by architecture, independently of Unit 6.** The Implementation Plan's own stated dependencies for Unit 8 are "Unit 5; Memory Core's existing, unmodified permission boundary" — notably, **not** Unit 6. On paper, both are satisfied (Unit 5 is complete; Memory Core's permission boundary is frozen). In practice, this audit found two concrete gaps:

1. `src/composition/ParkerRuntime.kt` constructs `InMemoryKnowledgeStore()` with **no permission gating of any kind** (line ~291: `val inMemoryMemoryStore = InMemoryKnowledgeStore()`, no `PermissionEngine` wrapper). `docs/architecture/MEMORY_CORE_SCOPE_LOCK.md` states "**Runtime owns** every `PermissionEngine.evaluate` call required before any `MemoryCore` write... reaches its requester" — this is Memory Core's own Evaluation A, and it is not, in fact, wired into the running composition root for Memory/Knowledge operations today.
2. The existing `PermissionEngine` (`src/interfaces/PermissionEngine.kt`) is shaped as `suspend fun evaluate(request: ExecutionRequest): PermissionDecision` — `ExecutionRequest` is an Execution Pipeline/Tool-invocation concept. No document defines how "submitting a Knowledge Candidate" would be expressed as, or converted into, an `ExecutionRequest`, and inventing such a conversion, or a new `PermissionEngine` overload, is an architecture decision squarely outside Unit 8's own authority.

Unit 8 is therefore not implementable as currently scoped, for reasons independent of Unit 6's own blockage.

**Unit 9 (Knowledge Query, Result, Retrieval) — blocked by another unit.** Dependencies per the Implementation Plan: "Units 4, 6, 7." Transitively blocked by Units 6 and 7. No additional independent gap was found beyond this dependency — Unit 9's own subject matter (relevance, staleness, determinism) does not itself require the six promotion factors; relevance is explicitly, correctly assigned here rather than to Unit 6 (`docs/reviews/PROGRAMME_3_KNOWLEDGE_PROMOTION_FACTOR_PROVENANCE.md`, Section 2.3).

**Unit 10 (Composition wiring and regression) — blocked by another unit.** Dependencies: "all prior units," per the Implementation Plan. Transitively blocked by Units 6, 7, 8, 9.

---

## Part 2 — Dependency Audit

Programme 3 governance assumes the following dependencies exist. Each is assessed against what this audit actually found in the codebase and governance documents, not against what governance narrates.

- **Memory Core (read/write contracts: `MemoryCore`, `MemoryRetrieval`, `Entity`, `Document`, `Assertion`, `Relationship`, `Provenance`).** Exists, and is frozen/complete for everything Programme 3 has used through Unit 5. Two specific capabilities Programme 3's own Unit 6 needs are **absent**: a way to search Memory Core for records materially similar to a given proposition (needed for repetition/frequency), and a way to resolve a bare `ProvenanceId` back to its own `Provenance` record (needed for `ContentNature`). Neither is a Programme 3 concept — both belong to Memory Core (Programme 2).
- **Knowledge Memory (Programme 3's own prior units).** Units 1–5 exist and are complete. Units 6–10 do not yet exist.
- **Runtime / composition-layer permission wiring.** Governance exists (`docs/architecture/MEMORY_CORE_SCOPE_LOCK.md`: "Runtime owns every `PermissionEngine.evaluate` call required before any `MemoryCore` write"); **implementation is absent** — `ParkerRuntime.kt` does not wrap `InMemoryKnowledgeStore` or `InMemoryMemoryCore` construction or operation with any `PermissionEngine` check. `PermissionEngine`/`DefaultPermissionEngine` exist and are wired, but only for Execution Pipeline (`ExecutionRequest`-shaped) checks.
- **Reasoning Context.** Exists, production-wired (`DefaultReasoningContextAssembler`, consuming the renamed `MemorySource`/`KnowledgeSource`). Not a dependency Programme 3's *remaining* units need satisfied — Reasoning Context's own adoption of the new Knowledge Query/Result surface is Programme 4's forward act, not a Programme 3 prerequisite.
- **World Model.** Not a dependency at all. `docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_SCOPE_LOCK.md` §4 excludes it explicitly and permanently ("Permanently and structurally independent... Not deferred — excluded").
- **Retrieval (Knowledge Query/Result surface).** Governance exists (Contract Design V2 §6); implementation does not yet exist — this is Unit 9's own deliverable, not a pre-existing dependency Programme 3 assumes.
- **Provenance.** Partially exists: `ProvenanceReference`/`provenanceId` (identifier only) is fully available on every resolved Memory Core record; a lookup from that identifier back to the full `Provenance` record (and its `ContentNature`) does not exist anywhere in `MemoryRetrieval`.
- **Evidence (Entity/Document/Assertion/Relationship).** Fully exists; no gap identified.
- **Trust Framework (`PermissionEngine`).** Partially exists — the interface and `DefaultPermissionEngine` exist and are genuinely wired for Tool/Execution Pipeline permission checks; no equivalent path exists for a Knowledge Candidate submission act (see Part 1, Unit 8).

---

## Part 3 — Missing Constitutional Concepts

Beyond the five the governing task named as examples, this audit located two further concepts referenced by Programme 3 governance with no defined owner or mechanism:

- **User importance** (beyond explicit request) — Chapter 33 names it; no document anywhere defines what it means operationally or which subsystem would supply it. **No constitutional owner currently exists.**
- **Explicit request** (as a field) — the *concept* has a clear, well-documented owner (the subsystem constructing the candidate on the user's behalf, per `docs/architecture/MEMORY_CONTRACT_DESIGN.md`), and was previously implemented on the legacy `CandidateKnowledge`. The *field* is simply absent from the current `KnowledgeCandidate` (Unit 5), and Contract Design V2 §2's own text ("carries a reference to existing Memory Core evidence and nothing else evidential") is genuinely ambiguous about whether restoring it is even permitted without a further amendment.
- **Repetition / frequency comparison mechanism** — conceptually owned (Knowledge Memory weighs; Memory Core holds the data), but no defined search or comparison capability exists on `MemoryRetrieval` to perform it.
- **Common-origin determination** (Article XI, Independence; restated in `docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_SCOPE_LOCK.md` §6) — governance requires that before repetition, frequency, or corroboration may contribute weight, Knowledge Memory must first determine whether the repeated mentions or corroborating records "share a common origin." **No document, anywhere reviewed, defines what data or algorithm would establish common origin** (shared `Provenance.sourceIdentifier`? shared upstream `Document`? something else). This is a distinct gap from the repetition/frequency search capability itself — even if that search existed, nothing defines how its results would be checked for common origin.
- **`ContentNature`-by-reference access** — `ContentNature` itself is a defined Memory Core concept (`Provenance.contentNature`), but no `MemoryRetrieval` method resolves a `ProvenanceId` to its owning `Provenance` record, so this concept, while owned, is unreachable.
- **"Knowledge Memory's own independent [confidence] evaluation"** — Contract Design V2 §3/§4 names this as a second permitted confidence source, alongside Memory Core's own recorded figure. No document defines any computation for it. **No constitutional owner currently exists** for this specific mechanism, even though Knowledge Memory is named as the party that would perform it — naming a party is not the same as defining a mechanism.
- **A permission-request shape for Knowledge Candidate submission** — `PermissionEngine` exists, but its sole request type (`ExecutionRequest`) has no defined mapping to or from a Knowledge Candidate submission act. This is required for Unit 8 and has no current owner or design.

---

## Part 4 — Programme Sequencing

Programme-level order (Memory Core → Knowledge Memory → Reasoning Context → World Model, per `docs/reviews/EPISTEMIC_INTEGRITY_CONSTITUTIONAL_REMEDIATION_PLAN.md`'s own roadmap) is not, itself, found to be wrong — Programme 3 correctly follows Programme 2, and Programme 4 correctly follows Programme 3. The defect this audit found is narrower and more specific: **Programme 2 (Memory Core) was declared complete and frozen without ever delivering two capabilities Programme 3's own frozen governance (Chapter 33; Contract Design V2 §5) requires** — the repetition/frequency comparison capability and the provenance-by-identifier lookup. Likewise, **Runtime's permission-gating responsibility for Memory/Knowledge operations, assigned by Memory Core's own Scope Lock, was never actually built**, which Unit 8 now needs. Neither of these is a case of the Programmes running in the wrong order; both are cases of an earlier Programme's own "complete" declaration understating what a later, dependent Programme would actually require of it.

**Unit-level finding, within Programme 3 itself:** Unit 8's own stated dependencies ("Unit 5; Memory Core's existing, unmodified permission boundary," Implementation Plan §3) do not include Unit 6 or Unit 7. Strictly by the Implementation Plan's own dependency graph, Unit 8 could be attempted independently of Units 6–7's blockage. This audit does not recommend doing so (Unit 8 carries its own, separate architectural blocker, Part 1 above) — it records this only as a sequencing fact: Programme 3's remaining units are not a single, undifferentiated blocked mass; Unit 8 fails for a different, unrelated reason than Units 6/7/9/10 do.

---

## Part 5 — Implementation Viability

| Unit | Classification |
| --- | --- |
| 6 | Implementable after governance amendment (Memory Core capability extension and/or Programme 3's own `KnowledgeCandidate`/Contract Design V2 §2 amendment); the "user importance" sub-factor cannot be classified even this favourably — it requires a concept to be defined and owned before any amendment can name it |
| 7 | Implementable after governance amendment (inherits Unit 6's blocker in full) |
| 8 | Implementable after architectural dependency (Runtime permission-gating wiring for Memory/Knowledge operations; a defined permission-request shape for Knowledge Candidate submission, or a `PermissionEngine` contract extension) |
| 9 | Implementable after architectural dependency (Units 6 and 7 must exist first; no independent gap of its own) |
| 10 | Implementable after architectural dependency (all prior units) |

No remaining unit is classified "implementable now." None is recommended to "move to a later Programme" outright — each remaining obligation is still correctly assigned to Programme 3 or, where a capability gap points elsewhere (Memory Core, Runtime), to the Programme that already, constitutionally, owns that capability (Programme 2, Runtime/Trust Framework respectively) rather than to Programme 3 adopting it itself.

---

## Part 6 — Architectural Health

The defects this audit and its two predecessor reviews surfaced are not uniform in kind:

- **Isolated drafting defects (now corrected):** the original `PROGRAMME_3_UNIT_6_SCOPE_LOCK_CLARIFICATION.md`'s adoption of an unconditional-promotion model and an unexcused single-factor corroboration rule were genuine drafting defects, confined to that one document, and already corrected by amendment following the Constitutional Reconciliation. These do not indicate a systemic problem — they indicate the review process functioned as intended.
- **Architectural omissions (not corrected, and not confined to one document):** Memory Core's absent repetition/frequency-comparison capability and absent provenance-by-identifier lookup are not drafting defects — they are capabilities Programme 2 never built, disclosed as far back as `docs/architecture/IMPLEMENTATION_GAPS.md` Gap #46, predating Programme 3 entirely and still "Open, not yet closed" as of that document. Runtime's absent permission-gating wiring for Memory/Knowledge operations is the same kind of omission, one layer further out.
- **A genuine sequencing/completeness defect at the inter-Programme level:** Programme 2 was marked complete before its own deliverables were checked against what Programme 3 would actually need from it; the same pattern recurs between Runtime/Trust Framework and Programme 3's Unit 8.
- **No new constitutional contradiction was found in this audit.** The Constitutional Articles themselves (IV, XI, XIV, and others reviewed) are internally consistent with each other and with Contract Design V2's own text; the problems found are omissions and incomplete delivery, not contradictions between binding rules.
- **This is not normal design evolution.** Normal evolution would mean requirements changing or being refined over time. Here, the requirements (Chapter 33's six factors; Memory Core's frozen contract) have not changed since before Programme 3 began — what changed is that Programme 3's Unit 6 was the first attempt to actually operationalize them, and that attempt revealed capabilities that were always required but never delivered.

**Overall assessment:** Programme 3's own governance-drafting discipline is healthy — each defect found in its own documents was identified, traced to its source, and correctable through the same channel that found it. Programme 3's *foundation*, however, is not yet complete relative to Programme 3's own needs: two Memory Core capabilities and one Runtime capability, all assigned elsewhere by existing governance, were never built.

---

## Table 1 — Unit Readiness

| Unit | Status | Blocking Reason | Recommended Action |
| --- | --- | --- | --- |
| 1–5 | Complete | N/A | None |
| 6 | Blocked by missing contracts | Only 1 of 6 named promotion factors reachable (confidence, `Assertion`-only); repetition/frequency need an absent Memory Core capability; explicit request needs a dropped `KnowledgeCandidate` field; user importance has no owner at all | Not proposed here — governance-level resolution required before any implementation attempt |
| 7 | Blocked by another unit | Depends on Unit 6 (Implementation Plan §3); re-applies the same unavailable promotion criteria | Cannot begin until Unit 6 resolved |
| 8 | Blocked by architecture | No Runtime permission-gating wiring exists for Memory/Knowledge operations; `PermissionEngine`'s `ExecutionRequest` shape does not fit a Knowledge Candidate submission act | Cannot begin until Runtime/Trust Framework wiring and request-shape questions are resolved, independent of Unit 6 |
| 9 | Blocked by another unit | Depends on Units 6, 7 (Implementation Plan §3) | Cannot begin until 6 and 7 resolved |
| 10 | Blocked by another unit | Depends on all prior units | Cannot begin until all prior units resolved |

## Table 2 — Dependency Audit

| Dependency | Owner | Available | Missing | Required Before |
| --- | --- | --- | --- | --- |
| Memory Core (`MemoryCore`/`MemoryRetrieval`) | Programme 2 | Yes, for everything through Unit 5 | Record-similarity search (repetition/frequency); `ProvenanceId` → `Provenance` lookup | Unit 6 |
| Knowledge Memory (Programme 3's own units) | Programme 3 | Units 1–5 | Units 6–10 | Units 6–10 |
| Runtime permission-gating wiring | Runtime (per Memory Core Scope Lock) | No — not wired in `ParkerRuntime.kt` for Memory/Knowledge operations | Composition-level `PermissionEngine` gating for `InMemoryKnowledgeStore`/`InMemoryMemoryCore` | Unit 8 |
| Reasoning Context | Programme 4 | Yes (legacy-wired) | Adoption of new Knowledge Query/Result surface (Programme 4's own future act) | Not required before Programme 3's remaining units |
| World Model | Programme 5 | N/A | N/A | Not a dependency — explicitly excluded |
| Retrieval (Knowledge Query/Result) | Programme 3, Unit 9 | No | Entire surface | Unit 9 (self) |
| Provenance | Memory Core | Partial — identifier only | Full-record lookup by identifier | Unit 6 (`ContentNature` factor) |
| Evidence (Entity/Document/Assertion/Relationship) | Memory Core | Yes | None identified | N/A |
| Trust Framework (`PermissionEngine`) | Runtime/Trust Framework | Partial — exists for `ExecutionRequest` only | A Knowledge-Candidate-submission-shaped request path | Unit 8 |

## Table 3 — Missing Constitutional Concepts

| Constitutional Concept | Owner | Exists | Missing | Recommendation |
| --- | --- | --- | --- | --- |
| User importance (beyond explicit request) | Unassigned | No | Entire mechanism and definition | No constitutional owner currently exists |
| Explicit request (field) | Constructing subsystem / Knowledge Memory | Partial (legacy precedent only) | Field on current `KnowledgeCandidate`; resolution of Contract Design V2 §2 ambiguity | Programme 3's own contract-level fix |
| Repetition/frequency comparison mechanism | Memory Core (data) / Knowledge Memory (weighing) | No | Record-similarity/recurrence search capability | Programme 2 contract extension |
| Common-origin determination (Article XI) | Unassigned | No | Any defined data source or algorithm | No constitutional owner currently exists |
| `ContentNature`-by-reference access | Memory Core | Partial (field exists; lookup does not) | `ProvenanceId` → `Provenance` retrieval | Programme 2 contract extension |
| Knowledge Memory's own independent confidence evaluation | Unassigned (named but undefined) | No | Any defined computation | No constitutional owner currently exists |
| Permission-request shape for Knowledge Candidate submission | Unassigned | No | Submission-shaped request type or `PermissionEngine` extension | Requires an architecture decision before Unit 8 |

## Table 4 — Programme Sequencing

| Programme | Depends On | Correct Order | Notes |
| --- | --- | --- | --- |
| Programme 2 (Memory Core) | None | 1st | Declared complete, but two capabilities Programme 3 needs were never delivered |
| Programme 3 (Knowledge Memory) | Programme 2 | 2nd | Units 1–5 complete; Units 6–10 blocked, partly on Programme 2's own undelivered capabilities, not solely on Programme 3's own governance |
| Programme 4 (Reasoning Context) | Programme 3 (Unit 9 delivery) | 3rd | Correctly ordered after; not currently blocking Programme 3 |
| Programme 5 (World Model) | Independent | Unordered relative to Programme 3 | Explicitly, permanently excluded from any Programme 3 dependency |
| Runtime / Trust Framework (permission wiring) | Cross-cutting, predates Programme numbering | N/A | Assigned Memory/Knowledge permission-gating responsibility by Memory Core's own Scope Lock; never delivered |

---

## Final Recommendation

```
Programme 3 should pause pending governance amendment.
```

Every remaining unit (6–10) is blocked, and the blockers are governance-completeness problems, not ordering problems: Programme 2 (Memory Core) was declared complete without delivering two capabilities its own frozen Chapter 33/Contract Design V2 §5 obligations require of a downstream consumer; Programme 3's own `KnowledgeCandidate` contract (Unit 5) dropped a field its legacy predecessor had, compounded by a genuine textual ambiguity in Contract Design V2 §2; one named factor ("user importance") has never been assigned an owner by any document; and Runtime's own Memory Core Scope Lock-assigned permission-gating responsibility for Memory/Knowledge operations was never built. Reordering Programme 3's remaining units does not resolve any of this — Unit 8's nominal independence from Unit 6 (Part 4) does not help, since Unit 8 carries its own, separate, equally real blocker. Each of these requires a governance-level decision (a Memory Core Scope Lock amendment; a Contract Design V2 §2 clarification/amendment; a Trust Framework/Runtime wiring authorization) before any further Programme 3 implementation can honestly proceed without inventing architecture this audit was instructed not to invent.

---

## Final Report

**File created:** `docs/reviews/PROGRAMME_3_CONSTITUTIONAL_AUDIT.md` (only file created; no other file modified).

**Documents reviewed:** the seven named in the governing task, plus `MEMORY_CORE_SCOPE_LOCK.md`, `MEMORY_CORE_GOVERNANCE_REVIEW.md`, `reasoning-context.md`, `IMPLEMENTATION_GAPS.md`, `MEMORY_CONTRACT_DESIGN.md`, `MEMORY_ARCHITECTURE_RECONCILIATION.md`, and the composition root (`ParkerRuntime.kt`, `PermissionEngine.kt`, `EventPublishingMemoryCore.kt`); Goal Manager and Trust Framework stubs checked and found immaterial.

**Implementation units audited:** 6, 7, 8, 9, 10 (Units 1–5 confirmed complete, not re-audited).

**Dependencies audited:** Memory Core, Knowledge Memory, Runtime/permission wiring, Reasoning Context, World Model, Retrieval, Provenance, Evidence, Trust Framework (nine total).

**Constitutional gaps identified:** seven distinct missing or partially-owned concepts (Table 3), two of which ("user importance," "common-origin determination," "Knowledge Memory's own independent confidence evaluation" — three, not two) have no constitutional owner at all.

**Sequencing issues identified:** Programme 2's "complete" declaration understates what Programme 3 actually requires of it; Runtime's Memory Core Scope Lock-assigned permission-gating responsibility was never built; within Programme 3 itself, Unit 8 is not dependent on Unit 6 despite both being currently blocked, for unrelated reasons.

**Overall Programme 3 health:** Programme 3's own governance-drafting discipline is sound — every defect found in its own documents was correctly identified and traced to its source through proper channels. Its foundation is incomplete: two Memory Core capabilities and one Runtime capability, all assigned by existing governance to subsystems other than Programme 3 itself, were never delivered.

**Recommended next step:** pause Programme 3 implementation pending governance amendment to Memory Core (repetition/frequency search; provenance-by-identifier lookup), Programme 3's own Contract Design V2 §2 (explicit-request field), and Runtime/Trust Framework (permission-gating wiring for Memory/Knowledge operations) — none of which this document performs.

PROGRAMME 3 CONSTITUTIONAL AUDIT COMPLETE

Confirmed: no production code modified; no tests modified; no governance documents modified; nothing staged; nothing committed; nothing pushed; Unit 7 not started.
