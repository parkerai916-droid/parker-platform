**Status:** Constitutional ownership audit only. No Kotlin is implemented, proposed, or changed by this document. Neither `src/` nor `tests/` is touched. No governance document is amended. Nothing is staged, committed, or pushed. This document classifies blockers already identified by `docs/reviews/PROGRAMME_3_CONSTITUTIONAL_AUDIT.md`; it does not resolve them.

# Programme 3 — Blocker Ownership Matrix

Programme: **Parker Constitutional Ownership Audit — Governance vs Implementation Responsibility.**

This document classifies each blocker `docs/reviews/PROGRAMME_3_CONSTITUTIONAL_AUDIT.md` identified, to determine whether governance already assigns responsibility correctly (in which case amending it would be wrong) or whether a genuine governance gap exists. It reuses the research already performed for that audit and its own two predecessor documents (`docs/reviews/PROGRAMME_3_UNIT_6_CONSTITUTIONAL_RECONCILIATION.md`, `docs/reviews/PROGRAMME_3_KNOWLEDGE_PROMOTION_FACTOR_PROVENANCE.md`), plus `docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_CONTRACT_DESIGN_V2.md`, `docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_SCOPE_LOCK.md`, `docs/implementation/PROGRAMME_3_KNOWLEDGE_MEMORY_IMPLEMENTATION_PLAN.md`, `docs/governance/PROGRAMME_3_UNIT_6_SCOPE_LOCK_CLARIFICATION.md` (amended), `docs/reviews/EPISTEMIC_INTEGRITY_CONSTITUTIONAL_REMEDIATION_PLAN.md`, `docs/architecture/IMPLEMENTATION_GAPS.md`, `docs/architecture/MEMORY_CONTRACT_DESIGN.md`, `docs/architecture/MEMORY_CORE_SCOPE_LOCK.md`, and the composition root (`src/composition/ParkerRuntime.kt`, `src/interfaces/PermissionEngine.kt`). No additional document review was required beyond these to determine ownership of any blocker below.

---

## Per-Blocker Analysis

### 1. Memory Record Comparison (repetition/frequency)

**Constitutional owner: Memory Core.** `docs/architecture/MEMORY_CONTRACT_DESIGN.md`: "repetition and frequency are evaluated by comparing a submission against Memory's own existing records — that comparison is `MemoryPromotionPolicy`'s job." The comparison *decision* belongs to Knowledge Memory's own promotion evaluator, but the underlying *capability* it would need — searching Memory Core for materially similar records — must be exposed by Memory Core itself, since Knowledge Memory holds no independent copy of Memory Core's records (`docs/implementation/PROGRAMME_3_KNOWLEDGE_MEMORY_IMPLEMENTATION_PLAN.md` §5, "avoid duplicate sources of truth").

**Governance status: Partially specified.** The *responsibility* is named directly (quoted above) and the *factors* are named (`docs/architecture/33-memory-consolidation.md`). The *mechanism* is not: `docs/architecture/IMPLEMENTATION_GAPS.md` Gap #46 states plainly, "Neither `docs/architecture/MEMORY_CONTRACT_DESIGN.md` (Unit A2) nor this Unit's own instructions shape a way to supply that population to `MemoryPromotionPolicy.evaluate`... Extending it to accept a queryable view of existing records would be a genuine interface change."

**Implementation status: Not implemented.** `MemoryRetrieval` (`src/interfaces/MemoryCore.kt`) exposes `findEntities`, `findDocuments`, `findByMetadata`, `findByTimeRange`, `findByProvenance`, and `traverseRelationships` — none searches record content for material similarity to a proposition.

**Root cause: Missing governance.** Gap #46 itself frames the blocker as an unauthorised, un-designed interface change, not an omitted-but-straightforward coding task — no approved shape exists to implement against.

**Correct next action: Create governance amendment** (to Memory Core's own frozen contract, since the capability belongs on `MemoryRetrieval`).

---

### 2. Provenance Lookup by Identifier

**Constitutional owner: Memory Core.** `Provenance` and `ContentNature` are exclusively Memory Core concepts (`docs/architecture/MEMORY_CORE_SCOPE_LOCK.md`); any lookup capability for them belongs on `MemoryRetrieval`.

**Governance status: Partially specified.** The concept (`Provenance`, `ContentNature`) and its ownership are fully specified. The specific accessor is not: `MemoryRetrieval` defines `findByProvenance` (search *by* provenance criteria) but no method resolves a bare `ProvenanceId` back to its own `Provenance` record. `docs/architecture/MEMORY_CORE_SCOPE_LOCK.md` §8 records exactly one "Open Implementation Question" (concurrent versus serialized revision evaluation) — this absent accessor is not recorded there or anywhere else as a deferred, open question; it is simply absent from the frozen method inventory, undiscussed.

**Implementation status: Not implemented.** Confirmed directly against `MemoryRetrieval`'s method list.

**Root cause: Missing governance.** No document defines this accessor's shape, and — unlike the Scope Lock §8 open question — its absence was never even flagged as deferred.

**Correct next action: Create governance amendment.**

---

### 3. Knowledge Submission Permission Gating

**Constitutional owner: Runtime.** `docs/architecture/MEMORY_CORE_SCOPE_LOCK.md`: "**Runtime owns** every `PermissionEngine.evaluate` call required before any `MemoryCore` write and before any sensitive `MemoryRetrieval` read reaches its requester." Contract Design V2 §7 additionally names Evaluation B (Knowledge Memory's own submission-boundary check) as sharing "one enforcement mechanism (the Permission Engine)" with Evaluation A, without assigning Evaluation B's wiring to a different owner.

**Governance status: Partially specified.** *That* Evaluation B must exist, must be a fresh evaluation of the submission act (never re-litigating Evaluation A), and must use the same Permission Engine is stated directly (Contract Design V2 §7). *How* a Knowledge Candidate submission act — which is not itself a Memory Core write — would be expressed to, or gated by, `PermissionEngine.evaluate(request: ExecutionRequest)` is not specified anywhere. `src/interfaces/PermissionEngine.kt` confirms the only request type this interface accepts is `ExecutionRequest`, an Execution Pipeline/Tool-invocation concept with no documented mapping to or from a Knowledge Candidate submission.

**Implementation status: Not implemented.** `src/composition/ParkerRuntime.kt` constructs `InMemoryKnowledgeStore()` with no `PermissionEngine` wrapper of any kind; `DefaultPermissionEngine` is wired elsewhere in that same file only for Execution Pipeline checks. No Evaluation-B-specific code path exists.

**Root cause: Missing governance.** Even had Runtime's wiring been built on the original schedule, the underlying mismatch — `PermissionEngine`'s sole request type not accommodating a Memory Core write or a Knowledge Candidate submission — would still exist; the blocking gap is the undesigned request shape, not merely un-executed wiring.

**Correct next action: Create governance amendment** (extending or supplementing the Trust Framework's `PermissionEngine` contract and/or Memory Core's own Scope Lock to define the mechanism).

---

### 4. User Importance

**Constitutional owner: No constitutional owner currently exists.** `docs/architecture/33-memory-consolidation.md` names "User importance" as a factor label only. `docs/architecture/IMPLEMENTATION_GAPS.md` Gap #46 confirms it is distinct from Explicit Request ("user importance beyond an explicit request"). No document — Chapter 33, `MEMORY_CONTRACT_DESIGN.md`, the Trust Framework stub, or any other document reviewed for this or the two predecessor documents — assigns this concept to any subsystem.

**Governance status: Not specified.** Beyond the two-word label, no document defines what this factor means operationally, where it would be recorded, or who would supply it.

**Implementation status: Not implemented.** Nothing exists to implement, since no operational definition exists.

**Root cause: Missing governance.**

**Correct next action: Create governance amendment** (to first define the concept and assign an owner before anything else can follow).

---

### 5. Explicit Request

**Constitutional owner: Knowledge Memory.** The field belongs on `KnowledgeCandidate`, Knowledge Memory's own submission contract; the *value* is supplied by whichever subsystem constructs the candidate on the user's behalf, but the *contract slot* is Knowledge Memory's own responsibility to define, exactly as its legacy predecessor (`CandidateKnowledge.explicitlyRequested: Boolean`) already did.

**Governance status: Partially specified.** The concept is clearly and unambiguously defined (`docs/architecture/MEMORY_CONTRACT_DESIGN.md`: "a user directly asking Parker to remember something is architecturally different evidence than Parker noticing a pattern on its own") and was previously implemented. What is not resolved is whether the current `KnowledgeCandidate` may carry it at all: Contract Design V2 §2 states a Knowledge Candidate "carries a reference to existing Memory Core evidence and nothing else evidential — explicitly, a Knowledge Candidate carries no caller-settable confidence value and no caller-settable evidential-state value." This is genuinely ambiguous between (a) excluding only confidence/evidential-state, leaving a non-evidential field like explicit-request unaddressed, and (b) excluding every field beyond the evidence reference categorically. Until this is resolved, it cannot be determined whether restoring the field is a straightforward implementation step or would itself contradict Contract Design V2.

**Implementation status: Not implemented.** `KnowledgeCandidate` (`src/interfaces/KnowledgeStore.kt`) carries only `evidenceReference: MemoryCoreRecordReference`.

**Root cause: Missing governance.** The blocking factor is not merely an omitted field — it is that no correct implementation action can be defined until Contract Design V2 §2's own ambiguity is resolved; adding the field back without resolving it risks contradicting frozen governance under reading (b).

**Correct next action: Create governance amendment** (a narrow Contract Design V2 §2 clarification, in the same pattern already used for the Unit 4, Unit 5, and Unit 6 Scope Lock Clarifications).

---

### 6. Independent Confidence Evaluation

**Constitutional owner: Knowledge Memory.** Contract Design V2 §3/§4 names Knowledge Memory as the party that would perform this, as an alternative to Memory Core's own recorded confidence.

**Governance status: Not specified.** Contract Design V2 names this as a *permitted source* ("or from Knowledge Memory's own independent evaluation performed at this moment") but defines no computation, method, or input set for it anywhere — this is narrower than "partially specified," since nothing beyond naming a hypothetical source exists.

**Implementation status: Not implemented.**

**Root cause: Intentional future dependency.** Contract Design V2's own phrasing ("or from... an alternative source") is permissive, not mandatory — Unit 6 can satisfy the confidence factor using only Memory Core's own recorded `Assertion.confidence` without ever exercising this second, undefined source. This mirrors the same rationale `docs/architecture/IMPLEMENTATION_GAPS.md` Gap #46 gives for `DefaultMemoryPromotionPolicy`'s own deliberately minimal two-factor baseline: "a future, more capable implementation could weigh all [factors]." Nothing in reviewed governance requires this alternative source to exist before Programme 3 can proceed.

**Correct next action: No action required.** This gap does not block any remaining Programme 3 unit; it is a dormant, permitted-but-unexercised option, not a prerequisite.

---

### 7. Common-Origin Determination

**Constitutional owner: Knowledge Memory.** `docs/reviews/EPISTEMIC_INTEGRITY_CONSTITUTIONAL_REMEDIATION_PLAN.md` explicitly assigns this: "Article XI independence/corroboration tracking | CT-EI-49 | Knowledge Memory (Programme 3) / World Model (Programme 5), split by subsystem." `docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_SCOPE_LOCK.md` §6 restates the requirement directly under Programme 3's own binding constitutional obligations: "Repetition and frequency must never be treated as independent corroboration without first determining whether repeated mentions share a common origin."

**Governance status: Partially specified.** *That* the check must occur, and *who* must perform it (Knowledge Memory, per the citations above), are both stated. *What data or algorithm* establishes "common origin" is not defined anywhere in any document reviewed for this or its two predecessor documents.

**Implementation status: Not implemented.** No code path in the (now-discarded) Unit 6 evaluator attempt, nor anywhere else, performs any common-origin check.

**Root cause: Missing governance.** As with Memory Record Comparison, the requirement and its owner are both settled; only the mechanism is absent, and no implementation can proceed without inventing one.

**Correct next action: Create governance amendment.**

---

### 8. Relevance (discovered during review; not itself a blocker)

Included for completeness, since it appears throughout the reviewed documents as one of Contract Design V2 §5's six named factors, but it does not function as a blocker.

**Constitutional owner: Reasoning Context / Knowledge Retrieval (Unit 9).** `docs/architecture/MEMORY_CONTRACT_DESIGN.md`: "No ranking or relevance score. That is a retrieval-time concept... not a submission-time one." `docs/architecture/reasoning-context.md` confirms relevance is assembled at Reasoning Context's own task-scoped stage, not at Memory promotion.

**Governance status: Fully specified** — governance affirmatively and deliberately excludes this factor from Knowledge Promotion, assigning it elsewhere.

**Implementation status: Not implemented at Unit 6** (correctly; it is not Unit 6's responsibility to implement).

**Root cause: Intentional future dependency** — deliberately deferred to Unit 9/Reasoning Context by existing, unambiguous governance.

**Correct next action: No action required.**

---

## Table

| Blocker | Constitutional Owner | Governance | Implementation | Root Cause | Correct Next Action |
| --- | --- | --- | --- | --- | --- |
| Memory record comparison | Memory Core | Partially specified | Not implemented | Missing governance | Create governance amendment |
| Provenance lookup by identifier | Memory Core | Partially specified | Not implemented | Missing governance | Create governance amendment |
| Knowledge submission permission gating | Runtime | Partially specified | Not implemented | Missing governance | Create governance amendment |
| User importance | No constitutional owner currently exists | Not specified | Not implemented | Missing governance | Create governance amendment |
| Explicit request | Knowledge Memory | Partially specified | Not implemented | Missing governance | Create governance amendment |
| Independent confidence evaluation | Knowledge Memory | Not specified | Not implemented | Intentional future dependency | No action required |
| Common-origin determination | Knowledge Memory | Partially specified | Not implemented | Missing governance | Create governance amendment |
| Relevance (non-blocking, discovered) | Reasoning Context / Knowledge Retrieval (Unit 9) | Fully specified | Not implemented at Unit 6 (correctly) | Intentional future dependency | No action required |

---

## Overall Assessment

- **Implementation omissions: 0.** No blocker was found where governance already fully or adequately specifies a mechanism and implementation simply failed to build it.
- **Governance omissions: 6.** Memory record comparison, Provenance lookup by identifier, Knowledge submission permission gating, User importance, Explicit request, and Common-origin determination each lack a defined mechanism, in every case confirmed against the specific governance text that would otherwise contain it.
- **Sequencing issues: 0.** No blocker was found where the Programme-level order (Memory Core → Knowledge Memory → Reasoning Context → World Model) was itself incorrect. `docs/reviews/PROGRAMME_3_CONSTITUTIONAL_AUDIT.md`'s own "sequencing" framing, on closer single-cause classification here, resolves in every instance to an underlying governance-completeness gap (Programme 2 and Runtime's own frozen governance never having specified a mechanism Programme 3 needs), not to the Programmes having been ordered incorrectly relative to one another.
- **Intentional future dependencies: 2** (Independent confidence evaluation; Relevance, the latter not a blocker at all), both correctly disclosed as deliberate, non-blocking, permissive design choices already present in frozen governance.

---

## Final Recommendation

```
Governance amendment is required before implementation resumes.
```

Six of the seven required blockers, and none of the two additionally-discovered items, resolve to a single root cause: a mechanism Programme 3's own frozen governance (or, in three cases, Memory Core's or Runtime's frozen governance) requires but never defines. None of these six is an implementation team's own failure to build something already fully specified, and none reflects Programmes running in the wrong order — Memory Core (Programme 2) correctly precedes Programme 3, and Programme 3 correctly precedes Programme 4. The evidence gathered here does not support amending governance where the Constitution already assigns responsibility correctly (the two "No action required" items, Independent confidence evaluation and Relevance, are left exactly as governance already, correctly, leaves them) — but it equally does not support resuming implementation, since six distinct, load-bearing mechanisms remain genuinely undefined, each confirmed against the specific governance text that would otherwise supply it.

---

## Final Report

**File created:** `docs/reviews/PROGRAMME_3_BLOCKER_OWNERSHIP_MATRIX.md` (only file created; no other file modified).

**Blockers analysed:** 7 required (memory record comparison, provenance lookup by identifier, knowledge submission permission gating, user importance, explicit request, independent confidence evaluation, common-origin determination) plus 1 additionally discovered (relevance, confirmed non-blocking).

**Implementation omissions:** 0.

**Governance omissions:** 6.

**Sequencing issues:** 0.

**Future dependencies:** 2 (independent confidence evaluation; relevance).

**Recommended next action:** governance amendment, before implementation resumes — targeted specifically at the six identified mechanism gaps, not at Programme order or at implementation effort.

PROGRAMME 3 BLOCKER OWNERSHIP AUDIT COMPLETE

Confirmed: no production code modified; no tests modified; no governance documents modified; nothing staged; nothing committed; nothing pushed; Unit 7 not started.
