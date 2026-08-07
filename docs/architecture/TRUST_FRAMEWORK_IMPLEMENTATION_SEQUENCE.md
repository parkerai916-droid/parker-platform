# Trust Framework Implementation Sequence

**Status:** Planning only. This is not governance, not a Contract Design, not a Scope Lock, and not an Implementation Plan — it introduces no new design decision and settles nothing that accepted governance has not already settled. It records the implementation order the accepted governance package already implies, so future work has a single "you are here" map instead of re-deriving sequencing from scratch. It does not modify any governance document, any Kotlin, or any test. Nothing is staged, committed, or pushed.

---

## 1. Current Status

**Completed governance:** Memory Core Durability (Units 1–10, each with its own Completion Review and Independent Constitutional Review); the Trust Framework Memory Retrieval Contract Design (Gap #54), including the Policy Rule Collision Clarification and the Resolution Derivation Mechanism Clarification (both Adopted); the Trust Framework Authorization Context Contract Design (Adopted); the Trust Framework Authorization Purpose Programme (accepted, architecture tier); the Gap #54 dependency consolidation binding the two together.

**Completed implementation:** Memory Core Durability (Units 1–10, live and composed). Parker Conversational Memory Bridge, Admission Unit (Unit 1 of 2) — implemented, tested (1938 tests passing), Completion Review and Independent Constitutional Review both complete (`ACCEPTED`), left deliberately uncommitted pending the blocker it discovered.

**Intentionally blocked work:** Gap #54's own policy-content resolution (approving `memory.retrieve`/`memory.retrieve_document`); the Conversational Memory Admission merge; Conversational Retrieval (Unit 2 of 2) — all blocked on Authorization Purpose existing as a built, evaluable Trust Framework capability, per the Gap #54 dependency consolidation.

---

## 2. Implementation Sequence

### 1. Authorization Purpose carrier / representation design
- **Purpose:** Decide how Authorization Purpose is carried on a permission request (a new `ExecutionRequest` field, a companion parameter, or another mechanism) and how its vocabulary is governed.
- **Dependency:** Authorization Context Contract Design (Adopted); Authorization Purpose Programme §9.
- **Expected outcome:** A dedicated Contract Design selecting a carrier mechanism. No Kotlin.

### 2. Authorization Purpose Scope Lock
- **Purpose:** Freeze the carrier design's own selected mechanism into an implementation-ready boundary.
- **Dependency:** Item 1.
- **Expected outcome:** A Scope Lock authorising Authorization Purpose's own implementation to begin.

### 3. Authorization Purpose implementation
- **Purpose:** Build the carrier mechanism, register the closed vocabulary, extend `DefaultPermissionPolicy`'s own matching to consult it — the same single Permission Policy, no second authority.
- **Dependency:** Item 2.
- **Expected outcome:** Authorization Purpose exists as an evaluable Trust Framework capability.

### 4. Gap #54 implementation
- **Purpose:** Retrofit `DefaultKnowledgeCandidateEvaluator` and `EvidenceIntelligenceInputResolver` with distinct, governed Authorization Purpose values; decide and add the still-open policy-content rule enabling Knowledge Submission's own outcome specifically. **Scoped to Knowledge Submission only** — Evidence Intelligence's own retrieval remains fail-closed by default, unchanged, through this item; becoming permissive for Evidence Intelligence is Item 8's own separate, later decision, never a side effect of this one.
- **Dependency:** Item 3; the mechanism-level work the two Adopted Clarifications already completed.
- **Expected outcome:** Memory Core retrieval permission evaluation can lawfully distinguish Knowledge Submission from Evidence Intelligence, and Knowledge Submission's own path becomes approvable without altering Evidence Intelligence's own default.

### 5. Knowledge Submission becomes live
- **Purpose:** `KnowledgeSubmission.submit` successfully resolves and promotes candidates in the live, composed runtime.
- **Dependency:** Item 4.
- **Expected outcome:** Gap #54's own live symptom is closed.

### 6. Conversational Memory Admission merge
- **Purpose:** Stage, commit, and integrate the already-complete, already-reviewed Admission Unit.
- **Dependency:** Item 5 — `IMPLEMENTATION_GAPS.md` Gap #54 records the Admission Unit as itself, directly, confirmed blocked by this gap.
- **Expected outcome:** The Bridge's own first unit reaches the live runtime.

### 7. Conversational Retrieval
- **Purpose:** Build the Bridge's own second, previously out-of-scope unit, wiring `knowledgeRetrieval` into Reasoning Context/Conversation.
- **Dependency:** Item 6; must use the same general Authorization Purpose mechanism every other consumer uses — no bespoke exception.
- **Expected outcome:** Owner "remember" instructions become retrievable in conversation.

### 8. Evidence Intelligence retrieval
- **Purpose:** A separate, explicitly-reasoned decision on whether Evidence Intelligence's own retrieval should ever become permissive, now that it can be evaluated distinctly from other consumers via its own Authorization Purpose.
- **Dependency:** Item 4.
- **Expected outcome:** Evidence Intelligence's own knowledge-dispatch path decided on its own terms, never as a side effect of another consumer's fix.

### 9. Remaining Trust Framework adoption
- **Purpose:** Any future Memory Core, or other Trust-Framework-gated, consumer (Memory Maintenance, World Model retrieval, and others) adopts the same general Authorization Purpose mechanism.
- **Dependency:** Item 3.
- **Expected outcome:** No future consumer requires its own bespoke authorization mechanism.

---

## 3. Dependency Chain

```
Authorization Purpose carrier / representation design
        |
        v
Authorization Purpose Scope Lock
        |
        v
Authorization Purpose implementation  ---------------------+
        |                                                   |
        v                                                   v
Gap #54 implementation                          Remaining Trust Framework adoption
        |
        +----------------------------------+
        v                                   v
Knowledge Submission becomes live      Evidence Intelligence retrieval
        |
        v
Conversational Memory Admission merge
        |
        v
Conversational Retrieval
```

---

## 4. Current Stop Point

- **Conversational Memory Admission Unit 1 is complete** — implemented, tested, reviewed, `ACCEPTED`, uncommitted.
- **Conversational Retrieval remains intentionally blocked**, transitively, on every item between it and Authorization Purpose.
- **The current implementation frontier is Authorization Purpose** — specifically, Item 1 (carrier/representation design), which has not yet begun.

---

## 5. Architectural Principles Governing Implementation

- **Governance before implementation.** No Scope Lock precedes its own Contract Design; no Kotlin precedes its own Scope Lock.
- **Fail-closed evolution.** Every step above preserves, and never relaxes, an existing fail-closed guarantee as a side effect of unblocking something else.
- **No caller-specific exceptions.** Every mechanism in this sequence is general — evaluated identically regardless of which consumer invokes it.
- **Single Trust Framework authority.** One `PermissionEngine`, one `DefaultPermissionPolicy`, throughout this entire sequence — nothing here introduces a second.
- **Implementation follows accepted constitutional dependencies.** This sequence records an order already implied by adopted governance; it does not invent one.

```
TRUST FRAMEWORK IMPLEMENTATION SEQUENCE — PLANNING DOCUMENT COMPLETE, PENDING INDEPENDENT ARCHITECTURAL REVIEW
```
