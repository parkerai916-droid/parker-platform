**Status:** Governance and planning only. No Kotlin is implemented, proposed as a diff, or changed by this document. Neither `src/` nor `tests/` is touched. No existing governance document is modified — `docs/governance/PROGRAMME_3_UNIT_9_KNOWLEDGE_RETRIEVAL_CONTRACT_DESIGN.md` (Adopted), `docs/governance/PROGRAMME_3_UNIT_9_SCOPE_LOCK_CLARIFICATION.md` (Adopted), `docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_CONTRACT_DESIGN_V2.md`, `docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_SCOPE_LOCK.md`, and `docs/implementation/PROGRAMME_3_KNOWLEDGE_MEMORY_IMPLEMENTATION_PLAN.md` are each cited, never altered. Nothing is staged, committed, or pushed.

# Programme 3 — Unit 9: Knowledge Retrieval Implementation Plan

Programme: **Programme 3 — Knowledge Memory, Unit 9 Implementation Plan (Knowledge Retrieval).**

This document is the detailed engineering breakdown of a single entry already authorised at two higher tiers: `docs/implementation/PROGRAMME_3_KNOWLEDGE_MEMORY_IMPLEMENTATION_PLAN.md` §3's own "Unit 9 — Knowledge Query, Knowledge Result, Knowledge Retrieval," and Scope Lock §5's own Deliverable 9. Neither document specified implementation-planning-level detail; the adopted Contract Design (`PROGRAMME_3_UNIT_9_KNOWLEDGE_RETRIEVAL_CONTRACT_DESIGN.md`) fixed that detail at the properties-and-guarantees level; this document translates that Contract Design into discrete, ordered, independently verifiable engineering units, exactly as `docs/implementation/MEMORY_CORE_IMPLEMENTATION_PLAN.md` and the parent Programme 3 Implementation Plan already did for their own units. It does not revisit, reweigh, or reopen anything either adopted document already settled.

---

## 1. Repository Baseline

- **HEAD:** `67fcb45afe21bd8d18955e11c7ad760d11748ca7`
- **Branch:** `main`
- **Working tree:** clean at the start of this task.

---

## 2. Governing Authorities

Read fresh, in full, before drafting: `docs/governance/PROGRAMME_3_UNIT_9_KNOWLEDGE_RETRIEVAL_CONTRACT_DESIGN.md` (Adopted, full, all 12 numbered sections plus Context and Final Recommendation); `docs/governance/PROGRAMME_3_UNIT_9_SCOPE_LOCK_CLARIFICATION.md` (Adopted, full, all 17 sections); `docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_CONTRACT_DESIGN_V2.md` (full); `docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_SCOPE_LOCK.md` (full); `docs/implementation/PROGRAMME_3_KNOWLEDGE_MEMORY_IMPLEMENTATION_PLAN.md` (full, §3 Unit 9 entry and Tracking Note read exactly). Source inspected for current implementation boundaries, informational only, not redefining anything: `src/interfaces/KnowledgeStore.kt`; `src/runtime/KnowledgeItemPersistence.kt`; `src/runtime/DefaultKnowledgeSubmission.kt`; `src/runtime/DefaultKnowledgeCandidateEvaluator.kt`; `src/composition/PermissionFilteredMemoryRetrieval.kt`; `src/composition/ParkerRuntime.kt` (Knowledge-related composition lines).

---

## 3. Cross-Cutting Constitutional Boundaries

Every unit below is bound by all five of the following, restated once here rather than repeated per unit:

- **Memory Core separation, preserved.** No unit constructs, holds, or introduces a `MemoryRetrieval`/`MemoryCore` dependency for Knowledge Retrieval. The one lawful contact with Memory Core — forwarding an already-issued provenance reference — requires no such dependency, since the reference already exists on the `KnowledgeItem` being retrieved (Contract Design §7, §9).
- **Provider neutrality, preserved.** No unit introduces a dependency on any specific reasoning provider, model, or prompt format. Knowledge Retrieval answers a Knowledge Query with a Knowledge Result; what any downstream reasoning provider does with that result is Reasoning Context's own concern (Contract Design §1, §3), never Knowledge Retrieval's, mirroring `ReasoningProvider`'s own existing abstraction boundary elsewhere in this codebase.
- **The adopted retrieval permission classification, preserved.** Every unit treats Knowledge Retrieval as an already-settled PermissionEngine proposal class (the Unit 9 Clarification, Adopted). No unit re-argues, re-derives, or weakens that classification; the one open question it left — enforcement mechanism — is treated below (Unit 9.5) as a governance precondition, not an implementation detail to resolve unilaterally.
- **No ranking, no semantic matching, anywhere.** Structural, criteria-based matching only (Contract Design §2, §3, §8), in every unit that touches query execution.
- **No new lifecycle state or event kind.** The existing two-value `KnowledgeItemStatus` (`ACTIVE`, `RETIRED`) and the existing four event kinds (promotion, revision, retirement, restoration) are the only vocabulary any unit may use (Contract Design §6).

---

## 4. Engineering Units

Each unit states its objective, its dependencies, the Contract Design section(s) it discharges, its expected repository impact at file-location granularity (never at Kotlin-declaration granularity), and the verification required to consider it complete.

### Unit 9.1 — Knowledge Query and Knowledge Result Contracts

**Objective.** Declare the public request and response shapes Contract Design §4 fixes at the properties level: a Knowledge Query capable of expressing structural matching criteria and an explicit correlation identifier; a Knowledge Result capable of bundling Knowledge Items/References with evidential-state, provenance, and staleness disclosures, and of expressing the five Error Model outcomes (Contract Design §9) as distinguishable from one another.

**Dependencies.** None beyond already-complete work: `KnowledgeItem`, `KnowledgeReference`, and the evidential-state/provenance-reference types (Programme 3 Units 2–5, already implemented).

**Expected repository impact.** `src/interfaces/KnowledgeStore.kt`, extended additively, mirroring this file's own established pattern of adding V2-tier types alongside the legacy model without altering it.

**Verification required.** Structural tests proving: a Knowledge Query cannot be constructed without a correlation identifier; a Knowledge Query carries no ranking instruction, semantic hint, or permission assertion field; a Knowledge Result's five possible outcomes (invalid query, unavailable data, permission denial, empty result, implementation failure — Contract Design §9) are each independently representable and never collapse into one another at the type level.

### Unit 9.2 — Deterministic Retrieval Engine

**Objective.** Implement structural query execution against Knowledge Memory's own held state: matching, filtering, and exactly one disclosed, consistently-applied ordering rule (Contract Design §8); forward each retrieved item's own already-existing provenance reference without independently querying Memory Core (Contract Design §7).

**Dependencies.** Unit 9.1 (the Query/Result types this engine consumes and produces); the existing `KnowledgeItemPersistence` read surface (Programme 3 Unit 8, already implemented) as this engine's own read source.

**Expected repository impact.** A new file under `src/runtime/`, alongside `DefaultKnowledgeCandidateEvaluator.kt` and `DefaultKnowledgeSubmission.kt`, following this directory's own established one-class-per-responsibility pattern.

**Verification required.** A determinism test proving the same query against unchanged state returns an identical result, in identical order, across repeated calls (Contract Design §8; Scope Lock §7). A structural test proving this engine holds no `MemoryRetrieval`/`MemoryCore` dependency of any kind, mirroring the precedent test already established for `DefaultKnowledgeSubmission`'s own "no dependency capable of reading Memory Core" guarantee. A test proving no scoring, weighting, or semantic comparison occurs anywhere in the matching path.

### Unit 9.3 — Staleness Disclosure Mechanism

**Objective.** Implement the specific staleness-detection mechanism Contract Design V2 §3/§6 and this Unit's own Contract Design §2 require to exist, without this Implementation Plan itself choosing between the two directions those documents leave open (continuous monitoring versus checked-at-query-time) until this unit's own drafting.

**Dependencies.** Unit 9.1 (the Knowledge Result's own staleness-disclosure component).

**Expected repository impact.** Contained within the same file as Unit 9.2, or a narrowly-scoped sibling file, depending on which detection direction this unit's own drafting selects — not fixed here.

**Verification required.** A test proving every Knowledge Result entry that includes a Knowledge Item or Knowledge Reference carries a staleness disclosure, with no code path capable of omitting it.

### Unit 9.4 — Retirement and Supersession Retrieval-Shape Decision

**Objective.** Resolve, once and consistently, the two retrieval-shape questions Contract Design §6 explicitly left open: whether a retired item is included in an ordinary Knowledge Query's own result set by default; how a superseded classification is surfaced relative to a Knowledge Item's own current classification. This is an implementation-tier decision, not a reopening of lifecycle ownership — Unit 7's own revision/retirement/restoration evaluators are untouched by it.

**Dependencies.** Unit 9.2 (the engine that will apply whichever default this unit selects).

**Expected repository impact.** Within Unit 9.2's own file — a policy decision embedded in the retrieval engine's own filtering step, not a separate class.

**Verification required.** A test proving the chosen default is applied uniformly, never varying by query shape or code path (mirroring the same uniformity discipline Scope Lock §8 already requires of concurrent-revision ordering). A test proving a retired or superseded item, if returned at all, discloses its own status honestly and is never presented as though it were current (Contract Design §6).

### Unit 9.5 — Permission Enforcement Wiring

**Objective.** Implement whichever enforcement mechanism — Knowledge Retrieval self-gating (mirroring `DefaultKnowledgeSubmission`'s own precedent) or external gating by Runtime (mirroring `PermissionFilteredMemoryRetrieval`'s own precedent) — a prior, narrower, implementation-facing Unit 9 Scope Lock Clarification determines, exactly as the adopted Contract Design §5 and the adopted Unit 9 Clarification §8 both require.

**Governance precondition, not merely a dependency.** This unit may not begin until that narrower Clarification exists and is adopted. This Implementation Plan does not perform, draft, or pre-empt that Clarification — doing so would be a governance act this document's own Status block forbids. Unit 9.5 is named and sequenced here so the dependency is visible, not to authorise skipping it.

**Dependencies.** Unit 9.2 (the engine being gated); the not-yet-drafted enforcement-mechanism Clarification (governance precondition, external to this Implementation Plan).

**Expected repository impact.** Either `src/runtime/PermissionFilteredKnowledgeRetrieval.kt`-shaped (a `src/composition/`-tier decorator, if externally gated) or a `PermissionEngine` field on Unit 9.2's own class (if self-gating) — the choice itself is fixed by the governance precondition above, not by this Implementation Plan.

**Verification required.** Whichever mechanism is chosen, a structural test mirroring the corresponding existing precedent exactly (`DefaultKnowledgeSubmission`'s own permission-gate test, or `PermissionFilteredMemoryRetrieval`'s own per-record-gating test). A test proving permission denial remains distinguishable from an empty result at every layer (Contract Design §9), not merely at the type level Unit 9.1 already fixed.

### Unit 9.6 — Runtime Composition

**Objective, and explicitly separate and final, per this task's own instruction.** Construct and compose the concrete Knowledge Retrieval implementation into `ParkerRuntime.kt`: instantiate it with whatever dependencies Units 9.1–9.5 fixed; resolve caller identity before a requesting principal is ever passed in, mirroring Memory Core Scope Lock §5's identical treatment. This unit stops at making Knowledge Retrieval reachable within the composed runtime — it does not wire Knowledge Retrieval to Reasoning Context, which remains Programme 4's own, separately governed act (Scope Lock §4).

**Dependencies.** All of Units 9.1–9.5, complete.

**Expected repository impact.** `src/composition/ParkerRuntime.kt`, additive wiring lines only, alongside the existing Knowledge Submission composition block (lines 742–748 and neighbouring registration lines), mirroring that block's own established pattern exactly.

**Verification required.** A composition test confirming exactly one Knowledge Retrieval instance is reachable from the composed graph, mirroring `ParkerRuntimeEvidenceIntelligenceCompositionTest.kt`'s own existing precedent for `InMemoryKnowledgeItemPersistence`. Confirmation that no other existing composition line is altered — additive only.

---

## 5. Implementation Ordering

```
9.1 (Query/Result Contracts)
  │
  ├──> 9.2 (Deterministic Retrieval Engine) ──> 9.4 (Retirement/Supersession Shape)
  │         │
  │         └──> 9.5 (Permission Enforcement Wiring) *requires prior governance*
  │
  └──> 9.3 (Staleness Disclosure)

9.2 + 9.3 + 9.4 + 9.5, all complete ──> 9.6 (Runtime Composition)
```

9.2 and 9.3 may proceed in parallel once 9.1 is complete, since neither depends on the other. 9.4 depends only on 9.2. 9.5 depends on 9.2 and, separately, on a governance step this plan does not perform. 9.6 is strictly last, depending on every other unit's own completion, exactly as this task requires.

---

## 6. Verification Strategy

No unit is considered verified by compilation or naming alone (mirroring the parent Programme 3 Implementation Plan's own §8 discipline). Each unit's own test proves the specific guarantee its Contract Design section fixes, not merely that code runs without error. Determinism (Unit 9.2), staleness presence (Unit 9.3), lifecycle-status honesty (Unit 9.4), and permission-denial distinguishability (Unit 9.5) are each independently, individually tested — no single end-to-end test is treated as a substitute for any of the others. Unit 9.6's own composition test is the only unit whose verification spans more than its own file, and it verifies reachability and non-interference only, never re-testing any guarantee already proven by an earlier unit's own test.

---

## 7. Completion Gates

**A unit is complete, objectively, when:** it compiles; every existing test affected by it still passes unmodified in substance; every new test named for it in Section 4 passes; no later unit in Section 5's own ordering was required to reach this state.

**Unit 9 as a whole is complete, objectively, when:** all six sub-units are complete by the above definition; a fresh clone of the repository builds and tests successfully with no partially-implemented sub-unit's state present; Knowledge Retrieval is reachable within the composed runtime (Unit 9.6) but not yet consumed by Reasoning Context, exactly as Scope Lock §4 requires of this Programme's own boundary.

**This is not, and does not claim to be, Programme 3's own overall completion.** Scope Lock §9's own five success criteria require every one of the ten Deliverables in Scope Lock §5, including Deliverable 2 (the `MemoryStore` migration) and Deliverable 10 (regression verification) — neither is touched by this plan. Completing Unit 9 satisfies Deliverable 9 alone.

---

## 8. Deferred Work Register

| Deferred item | Reason | Owner |
| --- | --- | --- |
| The enforcement-mechanism Clarification itself (self-gating vs. externally-gated) | A governance act, not an implementation step; explicitly reserved by the adopted Contract Design §5 and the adopted Unit 9 Clarification §8 | A future, narrow Unit 9 Scope Lock Clarification |
| Reasoning Context's own consumption and composition of Knowledge Retrieval | Explicitly allocated to Programme 4 (Scope Lock §4) | Programme 4 |
| Reconciliation of the legacy `KnowledgeSource`/`InMemoryKnowledgeStore` path with the V2 `KnowledgeItem` store | Identified, not resolved, by the preceding Unit 9 planning reviews; orthogonal to this plan's own scope | A separate, future planning task |
| Migration completion from the legacy `MemoryStore` shape (Scope Lock Deliverable 2) | A separate Programme 3 deliverable, not part of Unit 9 | Programme 3, outside this plan |
| Regression verification of the full `MemoryStore` test suite (Scope Lock Deliverable 10) | A separate Programme 3 deliverable | Programme 3, outside this plan |
| Storage technology, indexing, caching, optimisation | Explicitly out of scope per Contract Design §3 and §11, and per Scope Lock §4 | Implementation detail, never governance |
| Recording the retrieval permission classification within Contract Design V2 itself | Named by the adopted Contract Design's own Status block as a lawful, independent future step it neither performs nor forecloses | A future Contract Design V2 amendment, if and when drafted |

---

## 9. Risks

- **Sequencing risk (Unit 9.5).** Implementing permission enforcement before the required governance precondition exists would silently manufacture a resource/action representation or an enforcement decision no adopted document has authorised — exactly the risk the Unit 9 Clarification's own CDR-005 discipline exists to prevent. Mitigation: Unit 9.5 is explicitly blocked, not merely sequenced, in Section 4 above.
- **False-completion risk.** Because Unit 9.6 makes Knowledge Retrieval reachable in the composed runtime, a reader could mistake that reachability for "knowledge now flows to reasoning." It does not — Reasoning Context's own cutover is a separate, Programme 4 act (Section 7, above). Mitigation: Section 7's own completion gate states this explicitly.
- **Staleness-mechanism tradeoff (Unit 9.3).** Continuous monitoring and checked-at-query-time detection carry different consistency and performance properties neither the Contract Design nor this plan resolves. Mitigation: deferred deliberately to Unit 9.3's own drafting, consistent with Contract Design §3's own "must exist, not how" discipline; not a defect in this plan.
- **Novel design surface (Unit 9.4).** Unlike most other units, the retirement/supersession default has no directly analogous precedent elsewhere in this repository to mirror. Mitigation: Contract Design §6 already fixes the honesty constraint (never presented as current); only the inclusion default itself is genuinely new design work, narrowly scoped to one boolean-shaped decision.

---

## 10. Recommendation

Unit 9.1 may begin immediately — its only dependencies are already complete. Units 9.2, 9.3, and 9.4 may proceed once 9.1 lands, 9.2/9.3 in parallel. Unit 9.5 must not begin until the enforcement-mechanism Clarification Section 8 (Deferred Work Register) names is drafted and adopted; attempting it earlier would be a governance violation, not merely a sequencing inconvenience. Unit 9.6 is correctly the final, separate unit this task required it to be, gated on every other unit's own completion. This plan authorises no work beyond what the adopted Contract Design and the adopted Unit 9 Clarification already fix.

---

## 11. Independent Self-Review

- **Does this plan implement anything?** No — no Kotlin, class, method, or field is declared anywhere; every "expected repository impact" entry names a file location, never a declaration.
- **Does it modify any existing governance document?** No — Contract Design V2, the Scope Lock, the parent Implementation Plan, the Unit 9 Contract Design, and the Unit 9 Clarification are each cited, none altered.
- **Does it reopen the adopted permission classification?** No — Section 3 and Unit 9.5 both treat it as fixed, and Unit 9.5 is explicitly gated behind a future governance act rather than resolving the question itself.
- **Does it preserve Memory Core separation?** Yes — Section 3 and Unit 9.2's own verification requirement both fix that no unit holds a `MemoryRetrieval`/`MemoryCore` dependency.
- **Does it preserve provider neutrality?** Yes — Section 3 states this explicitly; no unit names or depends on a reasoning provider.
- **Does it invent a new lifecycle state, ranking mechanism, or Permission Engine concept?** No — checked against Section 3's own cross-cutting boundaries and each unit's own objective text; none appears.
- **Does it correctly isolate runtime composition as a separate, final unit?** Yes — Unit 9.6 depends on all five other units and performs no work any of them owns.
- **Does it claim Programme 3's own overall completion?** No — Section 7 explicitly disclaims this, naming the specific Scope Lock deliverables this plan does not touch.

No genuine defect found requiring correction before this document is offered for review.
