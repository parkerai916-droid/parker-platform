**Status:** Governance-only research document. No Kotlin is implemented, proposed, or changed by this document. Neither `src/` nor `tests/` is touched. Nothing is staged, committed, or pushed. This document does not amend any governance document it reviews; it traces provenance and documents gaps only.

# Programme 3 — Knowledge Promotion Factor Provenance

Programme: **Programme 3 — Knowledge Memory, Knowledge Promotion Factor Provenance.**

This document answers one question for each of the six promotion factors Contract Design V2 §5 names (repetition, importance, relevance, frequency, confidence, explicit request): where does it constitutionally originate, and is it currently reachable through Parker's existing public contracts? It draws on `docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_CONTRACT_DESIGN_V2.md`, `docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_SCOPE_LOCK.md`, `docs/implementation/PROGRAMME_3_KNOWLEDGE_MEMORY_IMPLEMENTATION_PLAN.md`, `docs/governance/PROGRAMME_3_UNIT_6_SCOPE_LOCK_CLARIFICATION.md` (amended), and `docs/reviews/PROGRAMME_3_UNIT_6_CONSTITUTIONAL_RECONCILIATION.md`, plus the documents actually found to materially describe one or more factors: `docs/architecture/33-memory-consolidation.md` (the origin of the six-factor list itself), `docs/architecture/MEMORY_CONTRACT_DESIGN.md`, `docs/architecture/MEMORY_RUNTIME_ARCHITECTURE.md`, `docs/architecture/MEMORY_ARCHITECTURE_RECONCILIATION.md`, `docs/architecture/MEMORY_CORE_GOVERNANCE_REVIEW.md`, `docs/architecture/IMPLEMENTATION_GAPS.md`, `docs/architecture/epistemic-integrity.md` (Articles XI and XIV), and `docs/architecture/reasoning-context.md`. Goal Manager (`docs/architecture/23-goal-manager.md`) and Trust Framework (`docs/architecture/09-trust-framework.md`) were reviewed and found not to materially describe any of the six factors — both are two-line stubs with no relevant content — and are not discussed further.

---

## 1. The Six Factors' Origin

All six factors trace to one place: `docs/architecture/33-memory-consolidation.md`, in full:

```
## Promotion Factors
- Repetition
- User importance
- Goal relevance
- Frequency
- Confidence
- Explicit request

## Rule
Memory promotion is conservative and auditable.
```

This is a short architecture stub, not a detailed specification — it names the six factors and states one governing rule ("conservative and auditable"); it does not define how any factor is computed, sourced, or owned. Every later document (`MEMORY_CONTRACT_DESIGN.md`, `MEMORY_RUNTIME_ARCHITECTURE.md`, `MEMORY_ARCHITECTURE_RECONCILIATION.md`, Contract Design V2 §5) quotes this same six-item list verbatim and in the same order, without adding a seventh factor or removing one of the six. Contract Design V2 §5 is therefore not itself the origin of these factors — it inherits them from Chapter 33 and adds only the multi-factor weighing constraint (Amendment 1) on top of an already-existing list.

**This gap is not new.** `docs/architecture/IMPLEMENTATION_GAPS.md` Gap #46 records that the very first `MemoryPromotionPolicy` implementation (Sprint 4, Track A, Unit A3, predating Programme 3 entirely) implemented only two of the six factors (confidence, explicit request) and explicitly, disclosedly left the other four (repetition, user importance beyond explicit request, goal relevance, frequency) unimplemented, "not... an oversight quietly worked around." Gap #46 is recorded as **"Open, not yet closed"** as of that document. This reconciliation's own findings below arrive at the same conclusion this earlier, independent gap record already reached, using different reasoning.

---

## 2. Per-Factor Analysis

### 2.1 Repetition

**Category B — already intended elsewhere, not yet exposed.** `MEMORY_CONTRACT_DESIGN.md` states directly: "No self-reported repetition or frequency figure... repetition and frequency are evaluated by comparing a submission against Memory's own existing records — that comparison is `MemoryPromotionPolicy`'s job, performed during Evaluation, not something the submitter can assert about itself." The intent is unambiguous: repetition is computed by Knowledge Memory's own evaluator, by comparing the candidate against Memory Core's existing record population.

- **Owning subsystem:** the comparison itself is Knowledge Memory's own job (per `MEMORY_ARCHITECTURE_RECONCILIATION.md`'s layering decision, Section 3 below); the *data* being compared against lives in Memory Core.
- **Relevant governance:** `MEMORY_CONTRACT_DESIGN.md` (quoted above); `docs/architecture/IMPLEMENTATION_GAPS.md` Gap #46 ("repetition... require[s] comparing a submission against Memory's own existing records, which `MemoryPromotionPolicy.evaluate`'s current signature has no way to supply").
- **Why Unit 6 cannot yet access it:** `MemoryRetrieval` (`src/interfaces/MemoryCore.kt`) exposes only structured lookups — `findEntities` (label/alias/type), `findDocuments` (type/location), `findByMetadata` (structured key/value), `findByTimeRange`, `findByProvenance`, and `traverseRelationships`. None of these searches `Assertion.statement` (or any other record's substantive content) for material similarity to a given proposition. No capability exists to ask "has something like this been recorded before."

**Objective or contextual:** objective in principle (a countable fact) but requires a similarity judgment (what counts as "the same" proposition) that no document defines.
**Static or dynamic:** dynamic — changes as new records are added to Memory Core.
**Belongs in:** the underlying data lives in Memory Core; the comparison/weighing is Knowledge Memory's own responsibility.

**Available / Planned / Missing:** **Planned.**

---

### 2.2 Importance ("User Importance")

**Category C — no constitutional source currently exists.** Chapter 33 names "User importance" as a factor distinct from "Explicit request" — `docs/architecture/IMPLEMENTATION_GAPS.md` Gap #46 makes this distinction explicit by naming the unimplemented remainder as "user importance **beyond** an explicit request," confirming these are two different concepts, not one. No document — not Chapter 33, not `MEMORY_CONTRACT_DESIGN.md`, not the Trust Framework stub, not any other reviewed document — defines what "user importance beyond explicit request" means operationally, where it would be recorded, or which subsystem would supply it. There is no user-preference field, priority marker, pinning mechanism, or equivalent concept anywhere in `Entity`, `Document`, `Assertion`, `Relationship`, `KnowledgeCandidate`, or any Trust Framework document reviewed.

- **Owning subsystem:** none identified. This is not a case of "intended elsewhere but not yet exposed" — no document assigns this concept to any subsystem at all.
- **Governance gap, precisely:** Chapter 33 names the factor in two words and no other frozen document operationalises it further.

**Objective or contextual:** contextual — inherently user-relative and subjective.
**Static or dynamic:** unstated; plausibly dynamic (a user's sense of importance could change), but no document says.
**Belongs in:** unassigned by any document reviewed.

**Available / Planned / Missing:** **Missing.**

---

### 2.3 Relevance ("Goal Relevance")

**Category B — already intended elsewhere, and deliberately excluded from promotion time.** `MEMORY_CONTRACT_DESIGN.md` states this with unusual directness: "No ranking or relevance score. That is a retrieval-time concept (`MemoryRetrievalPolicy`, deferred below), not a submission-time one." This is corroborated by `docs/architecture/reasoning-context.md`: Reasoning Context is described as "What matters for the **current task**," and "Memory is responsible for durable storage and retrieval of long-term knowledge, and for **exposing only the portions relevant to a given task when Reasoning Context is assembled**." Relevance is therefore a task-scoped, assembly-time concept, computed when Reasoning Context is built from Memory's retrieval surface — not a property evaluated when a candidate is promoted into Memory in the first place.

- **Owning subsystem:** Knowledge Retrieval (Unit 9, per `docs/implementation/PROGRAMME_3_KNOWLEDGE_MEMORY_IMPLEMENTATION_PLAN.md` §3) and, beyond Programme 3's own delivery of that surface, Reasoning Context (Programme 4), which actually consumes it against a specific task or goal.
- **Relevant governance:** `MEMORY_CONTRACT_DESIGN.md` (`MemoryRetrievalPolicy`, "the seam by which relevance and ordering are computed when `MemoryStore.retrieve` is called"); `docs/architecture/reasoning-context.md`.
- **Why Unit 6 cannot access it:** not merely inaccessible — architecturally out of place at this stage. Relevance depends on a specific task or goal that does not exist at promotion time; Unit 6 evaluates a candidate against Memory Core evidence alone, with no task or goal in scope.

`docs/architecture/23-goal-manager.md` ("Goal Manager") was reviewed for whether "goal relevance" names that subsystem specifically; it is a two-line stub ("The Goal Manager maintains long-term objectives... Goals may also be paused, cancelled or superseded") with no description of relevance computation, and does not materially describe this factor.

**Objective or contextual:** contextual — relative to a task or goal, not an intrinsic property of the evidence.
**Static or dynamic:** dynamic — can change per task independently of the underlying record.
**Belongs in:** Reasoning Context / Knowledge Retrieval, never Knowledge Memory's promotion boundary.

**Available / Planned / Missing:** **Planned** (planned elsewhere, by design — not a Unit 6 gap to close).

---

### 2.4 Frequency

**Category B — already intended elsewhere, not yet exposed**, for the identical reason as Repetition. `MEMORY_CONTRACT_DESIGN.md` groups repetition and frequency together in the same sentence ("repetition and frequency are evaluated by comparing a submission against Memory's own existing records"), and `IMPLEMENTATION_GAPS.md` Gap #46 groups them identically ("repetition/user-importance/goal-relevance/frequency require comparing a submission against Memory's own existing records").

**What "frequency" means, precisely, per governance:** the required analysis asks whether frequency means repeated observations, repeated retrieval, repeated reasoning, repeated user interaction, repeated external occurrence, or another meaning. `MEMORY_CONTRACT_DESIGN.md`'s own text ties frequency to comparing a submission against Memory's **existing records** — meaning repeated *external occurrence as recorded in Memory Core* (how often a materially similar proposition has been independently recorded as evidence), not repeated retrieval, not repeated reasoning, and not repeated user interaction. No document ties "frequency" to usage/retrieval counting anywhere.

- **Owning subsystem:** same as Repetition — comparison is Knowledge Memory's job; the record population being compared against is Memory Core's.
- **Why Unit 6 cannot yet access it:** identical reason as Repetition — no `MemoryRetrieval` method searches record content for material similarity or counts recurrence.

**Objective or contextual:** objective in principle, same similarity-judgment caveat as Repetition.
**Static or dynamic:** dynamic.
**Belongs in:** Memory Core (data) plus Knowledge Memory (weighing).

**Available / Planned / Missing:** **Planned.**

---

### 2.5 Confidence

**Category A, partially — available for one Memory Core record kind only.** `Assertion.confidence: Double?` (`src/interfaces/MemoryCore.kt`) is a real, directly reachable field.

- **Interface:** `MemoryRetrieval.getAssertion(requestingPrincipalId, assertionId): Assertion?`
- **Field:** `Assertion.confidence: Double?`
- **Ownership:** Memory Core, specifically the `Assertion` record type.

`Entity`, `Document`, and `Relationship` carry **no** confidence field of any kind — for these record kinds, confidence is **Category C**: no constitutional source exists.

**Constitutional origin:** Article XIV ("Confidence"), `docs/architecture/epistemic-integrity.md`: "Confidence shall be determined by evidential support rather than reasoning capability, fluency, or persuasiveness. Parker shall not express confidence exceeding that justified by the available evidence." This is a representation/expression-level principle, not itself a data-storage rule. Contract Design V2 §3/§4 supplies the operational sourcing rule built on top of it: a classification's confidence component may be sourced "only from Memory Core's own recorded evidence... or from Knowledge Memory's own independent evaluation performed at this moment."

**The second source ("Knowledge Memory's own independent evaluation") is itself Category C.** No document — not Contract Design V2, not the Scope Lock, not the Implementation Plan, not any architecture document reviewed — defines what this independent evaluation would compute or how. This is not a case of "owned by another subsystem Unit 6 cannot yet reach" — it is a genuinely undefined mechanism that no document assigns to anyone.

**Every Memory Core record may possess confidence? No.** Only `Assertion` carries the field; `Entity`, `Document`, and `Relationship` do not.

**Mandatory or optional? Optional.** `Double?` is nullable even on `Assertion`; Contract Design V2 §3/§4 requires the resulting classification to express that absence honestly rather than manufacturing a figure or defaulting to zero.

**Historical or recomputed? Historical**, as currently exposed. `Assertion.confidence` is set once, at `CandidateAssertion` construction, and stored; nothing in Memory Core recomputes it later. The "independent evaluation" alternative Contract Design V2 permits would be a recomputed value, but no mechanism for it exists anywhere (see above) — this document does not invent one.

**Available / Planned / Missing:** **Available** (for Assertion-kind evidence, historical figure only; Missing for every other record kind and for the "independent evaluation" alternative).

---

### 2.6 Explicit Request

**Category B — already intended and previously implemented, on a contract Unit 6 no longer has access to.** `MEMORY_CONTRACT_DESIGN.md` states directly: "An explicit-request flag, since `33-memory-consolidation.md` names 'explicit request' as its own distinct promotion factor, separate from confidence or repetition — a user directly asking Parker to remember something is architecturally different evidence than Parker noticing a pattern on its own." This is unambiguous on what the concept means.

**What "explicit request" means, precisely, per governance:** the required analysis asks whether it means user request, owner instruction, runtime instruction, constitutional instruction, agent instruction, or another concept. Per the quoted text, it means **user request specifically** — "a user directly asking Parker to remember something." No document ties it to an owner-level, runtime-level, constitutional, or agent-originated instruction.

- **Owning subsystem:** whichever subsystem constructs the candidate on the user's behalf during a conversation turn (for example, a conversation/reasoning coordinator that observed the user say "remember that..."), by setting a flag at submission time. This is a caller-supplied fact about the submission context, not something Memory Core or Knowledge Memory computes internally.
- **Relevant governance:** `MEMORY_CONTRACT_DESIGN.md` (quoted above); the legacy, still-frozen `CandidateKnowledge.explicitlyRequested: Boolean` field (`src/interfaces/KnowledgeStore.kt`), which already carries this exact concept and is still used by the legacy `DefaultKnowledgePromotionPolicy`/`DefaultMemoryPromotionPolicy` (`docs/architecture/IMPLEMENTATION_GAPS.md` Gap #46: "explicit request (unconditional promotion)").
- **Why Unit 6 cannot yet access it:** the Unit 5-authored `KnowledgeCandidate` (`src/interfaces/KnowledgeStore.kt`: `data class KnowledgeCandidate(val evidenceReference: MemoryCoreRecordReference)`) carries only one field. The legacy `CandidateKnowledge` shape had this field; it was not carried forward when `KnowledgeCandidate` was defined.

**A genuine textual ambiguity, disclosed rather than resolved here:** Contract Design V2 §2 states a Knowledge Candidate "carries a reference to existing Memory Core evidence and nothing else evidential — explicitly, a Knowledge Candidate carries no caller-settable confidence value and no caller-settable evidential-state value." This could be read either (a) narrowly — "nothing else evidential" refers specifically to confidence and evidential-state, and a non-evidential submission-context fact like explicit-request is not addressed by this sentence at all — or (b) broadly — "and nothing else" is read as a categorical statement that the evidence reference is the *only* field of any kind the type may carry. This document does not resolve this ambiguity; it is recorded here as a genuine open question that the Constitutional Reconciliation's own document set does not settle, rather than assumed one way in order to reach a tidier conclusion.

**Objective or contextual:** objective as a fact ("did the user ask, or not"), reported honestly by the constructing subsystem — the same trust category the legacy field already required.
**Static or dynamic:** static per submission — fixed at candidate-construction time.
**Belongs in:** `KnowledgeCandidate` itself (Knowledge Memory's own submission-time contract), mirroring the legacy shape, subject to the ambiguity above.

**Available / Planned / Missing:** **Planned.**

---

## 3. Promotion Boundary — What Knowledge Promotion Is Intended to Evaluate

Per the six factors' actual sourcing, established above, and `docs/architecture/MEMORY_ARCHITECTURE_RECONCILIATION.md`'s own layering decision ("Knowledge Memory's evaluation and promotion decisions operate over records... that Memory Core already knows how to identify and store... Rejected [alternative]: Knowledge Memory's own promotion factors... are exactly the kind of judgment that should be evaluated against real Entity/Document/Evidence/Provenance context"), Knowledge Promotion is intended to evaluate a **combination**, but a narrower one than "everything" — specifically:

- **Intrinsic properties of the evidence and its recorded history in Memory Core** — confidence (Section 2.5) and, where the evidence has been independently recorded before, repetition/frequency (Sections 2.1, 2.4). These are evaluated against Memory Core's own record population, per `MEMORY_CONTRACT_DESIGN.md`.
- **User intent, narrowly** — explicit request (Section 2.6): a specific, reportable fact about how the submission arose. "User importance" (Section 2.2) is also user-relative, but no document defines an operational mechanism for it, unlike explicit request.

Governance evidence does **not** support including:

- **Contextual/task relevance** — explicitly excluded from promotion time by `MEMORY_CONTRACT_DESIGN.md`'s own text (Section 2.3); it is a retrieval-time, task-scoped concept belonging to a different layer entirely.
- **Runtime usage** (how often something is retrieved or acted upon after promotion) — no document ties any of the six named factors to post-promotion usage statistics. "Frequency" (Section 2.4), on the textual evidence available, means recorded external occurrence, not retrieval frequency.

---

## 4. Deliverable Table

| Factor | Constitutional Source | Owning Subsystem | Current Contract Available | Governance Gap | Notes |
| --- | --- | --- | --- | --- | --- |
| Repetition | `33-memory-consolidation.md`; `MEMORY_CONTRACT_DESIGN.md` | Knowledge Memory (weighing) over Memory Core (data) | No | No `MemoryRetrieval` method searches record content for material similarity to a proposition | Same root gap as Frequency; disclosed pre-Programme-3 in `IMPLEMENTATION_GAPS.md` Gap #46 |
| Importance | `33-memory-consolidation.md` ("User importance") | Unassigned | No | No document defines "user importance beyond explicit request" operationally, anywhere | Distinct from Explicit Request per `IMPLEMENTATION_GAPS.md`'s own wording |
| Relevance | `33-memory-consolidation.md` ("Goal relevance") | Knowledge Retrieval (Unit 9) / Reasoning Context (Programme 4) | No (by design, not by oversight) | None — governance already allocates this elsewhere and explicitly excludes it from submission time | `MEMORY_CONTRACT_DESIGN.md`: "a retrieval-time concept... not a submission-time one" |
| Frequency | `33-memory-consolidation.md`; `MEMORY_CONTRACT_DESIGN.md` | Knowledge Memory (weighing) over Memory Core (data) | No | Same as Repetition | Means recorded external occurrence, not retrieval/usage frequency |
| Confidence | Article XIV; Contract Design V2 §3/§4 | Memory Core (`Assertion.confidence`) | Yes, for `Assertion` only | No source for `Entity`/`Document`/`Relationship`; no defined mechanism for "Knowledge Memory's own independent evaluation" | Optional, nullable, historical; must never be defaulted to zero |
| Explicit Request | `33-memory-consolidation.md`; `MEMORY_CONTRACT_DESIGN.md` | Constructing subsystem, via `KnowledgeCandidate` | No | Field dropped when `KnowledgeCandidate` (Unit 5) replaced legacy `CandidateKnowledge` | Restoring it may also require resolving a genuine Contract Design V2 §2 textual ambiguity (Section 2.6) |

Each row above resolves to exactly one of the required classifications: Repetition — **Planned**; Importance — **Missing**; Relevance — **Planned**; Frequency — **Planned**; Confidence — **Available** (partial); Explicit Request — **Planned**.

---

## 5. Recommendation

```
Knowledge Promotion requires a new constitutional contract before implementation.
```

Only one of six named factors (Confidence) is presently available, and only for one Memory Core record kind (`Assertion`); a genuine multi-factor promotion decision cannot be constructed from one partially-available factor alone, consistent with `docs/governance/PROGRAMME_3_UNIT_6_SCOPE_LOCK_CLARIFICATION.md`'s own §10 finding.

This document does not design any new contract. The gaps have three distinct owners, not one, and should not be collapsed into a single fix:

1. **Repetition and Frequency** — require a new Memory Core retrieval capability able to compare a candidate against Memory Core's existing record population for material similarity. Owning subsystem: **Memory Core** (Programme 2 — already scope-locked and implementation-planned per `docs/reviews/EPISTEMIC_INTEGRITY_CONSTITUTIONAL_REMEDIATION_PLAN.md`). `docs/architecture/IMPLEMENTATION_GAPS.md` Gap #46 already records two candidate closure paths: extend the seam with a way to consult existing records, or explicitly document a narrower factor baseline as an intentional, non-placeholder minimum. Which future Programme should introduce it: a Programme 2 (Memory Core) amendment, since the capability belongs on `MemoryRetrieval` itself, not on Knowledge Memory's own contract.
2. **Explicit Request** — requires restoring a field `KnowledgeCandidate` (Unit 5) dropped from its legacy predecessor, and resolving the Contract Design V2 §2 ambiguity over whether a non-evidential field may be added to it at all. Owning subsystem: **Knowledge Memory** (Programme 3) itself. Which future Programme should introduce it: Programme 3, via a Scope Lock revision to Contract Design V2 §2 and a corresponding `KnowledgeCandidate` amendment — this is Programme 3's own unfinished business, not a dependency on any other Programme.
3. **User Importance (beyond Explicit Request)** — has no defined mechanism or owner in any document reviewed. Owning subsystem: **unassigned**. Which future Programme should introduce it: cannot be determined from governance as it stands; this is a genuine, undesigned gap, not merely an unexposed one, and no document points to where it should be resolved.

Relevance requires no new contract — governance already, correctly, excludes it from Knowledge Promotion entirely.

---

## Final Report

**File created:** `docs/reviews/PROGRAMME_3_KNOWLEDGE_PROMOTION_FACTOR_PROVENANCE.md` (only file created; no other file modified).

**Factors analysed:** repetition, importance, relevance, frequency, confidence, explicit request (six).

**Available factors:** Confidence (partial — `Assertion` only).

**Planned factors:** Repetition, Relevance, Frequency, Explicit Request.

**Missing factors:** Importance.

**Constitutional gaps identified:** (1) no Memory Core capability to compare a candidate against existing records for repetition/frequency; (2) `KnowledgeCandidate` (Unit 5) does not carry an explicit-request field the legacy type had, compounded by a genuine Contract Design V2 §2 textual ambiguity over whether it may; (3) "user importance beyond explicit request" has no defined mechanism or owner anywhere in reviewed governance; (4) confidence has no source for non-Assertion record kinds, and "Knowledge Memory's own independent evaluation" (Contract Design V2's second permitted confidence source) has no defined mechanism.

**Whether Unit 6 remains blocked:** yes — consistent with, and independently corroborating, `docs/governance/PROGRAMME_3_UNIT_6_SCOPE_LOCK_CLARIFICATION.md` §10's own viability determination.

KNOWLEDGE PROMOTION FACTOR PROVENANCE COMPLETE

Confirmed: no production code modified; no tests modified; no governance documents modified except this newly created review; nothing staged; nothing committed; nothing pushed; Unit 7 not started.
