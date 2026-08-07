# Authorization Purpose — Scope Lock

## Status

**Freezes decisions already made. Does not redesign them.** Every freeze in this document is a direct restatement of a conclusion already reached by `docs/governance/TRUST_FRAMEWORK_AUTHORIZATION_CONTEXT_CONTRACT_DESIGN.md` (Adopted), `docs/governance/AUTHORIZATION_PURPOSE_CARRIER_CONTRACT_DESIGN.md` ("Unit 1," Adopted), `docs/governance/AUTHORIZATION_PURPOSE_VOCABULARY_GOVERNANCE_CONTRACT_DESIGN.md` ("Unit 2," Adopted), `docs/architecture/TRUST_FRAMEWORK_AUTHORIZATION_PURPOSE_PROGRAMME.md` (accepted, architecture tier), `docs/governance/TRUST_FRAMEWORK_MEMORY_RETRIEVAL_CONTRACT_DESIGN.md` (as consolidated), and `docs/architecture/TRUST_FRAMEWORK_IMPLEMENTATION_SEQUENCE.md` — cited by section, not re-derived. This document does not amend any of them. It does not write, propose, or imply Kotlin; it does not create an Implementation Plan; it does not modify tests. Wherever this document was uncertain whether a decision was already settled at Contract-Design tier, it froze less, not more, and recorded the question as deferred instead. Nothing is staged, committed, or pushed.

---

## 1. Executive Summary

Four governance documents and one architecture-tier Programme have independently, adversarially reviewed every constitutional question Authorization Purpose raises: what it is, how it distinguishes itself from Principal/Action/Resource, how it is carried, how its vocabulary is governed, and how it relates to Gap #54. This Scope Lock freezes those conclusions as a single, consolidated boundary between what is now settled governance and what remains implementation freedom — the line an Implementation Plan may not cross in one direction (redesigning a frozen decision) and must be free to cross in the other (choosing field names, registry classes, and algorithms without asking permission again).

---

## 2. Frozen Objectives

### 2.1 Constitutional Purpose

- Authorization Purpose is the **fourth constitutional authorization dimension**, alongside Principal (who is accountable), Action (what kind of act), and Resource (what is acted upon) — answering *for what governed reason, on behalf of which internal purpose, an act is proposed* (Authorization Context Contract Design §6; Carrier Contract Design §4).
- It **complements these three dimensions; it does not replace, absorb, or narrow any of them.** Principal continues to answer accountability alone (Carrier Contract Design §13: "additive, never substitutive"); Action continues to answer act-kind alone (Carrier Contract Design §15, Vocabulary Governance Contract Design §13: "parallel and orthogonal"); Resource continues to answer target-identity alone (Carrier Contract Design §14).

### 2.2 Carrier

- **Authorization Purpose is carried by `ExecutionRequest`.** It is part of the request itself (Carrier Contract Design §7: "the constitutional home is `ExecutionRequest` itself"; §10).
- **It is not a parallel authorization context.** Candidates constructing a second, separately-threaded object or wrapper (Carrier Contract Design's own Candidates B, C, D) were each investigated and rejected specifically because they risk exactly this (Carrier Contract Design §8) — consistent with ADR-017's own "No subsystem may invent a parallel execution request type."
- **`PermissionEngine` remains unchanged as the single authority.** Its own public interface (`evaluate(request: ExecutionRequest): PermissionDecision`) is not altered by anything this Scope Lock freezes (Carrier Contract Design §11; Chapter 10 §5).

**Not frozen:** Kotlin field names, constructor signatures, or any other API detail (Carrier Contract Design §7: "What this does not select: the field's own exact name, the exact shape of its own value type... These remain Scope-Lock/Implementation-Plan-tier questions"). **One behavioural constraint on that open shape is frozen, not the shape itself**: the eventual value type must be a distinct, closed value type, never a raw `String` directly on `ExecutionRequest` (Carrier Contract Design §7, §19 Risk 1) — a constraint on *what kind of thing* the field holds, not on its name or definition.

### 2.3 Vocabulary

- **A closed, governed vocabulary** — values are registered, never free text, never ambient, never inferred (Vocabulary Governance Contract Design §4).
- **Domain ownership** — each domain defines and registers its own Authorization Purpose values within its own governing Contract Design/Scope Lock; no central committee pre-approves every value (Vocabulary Governance Contract Design §5).
- **Composition-time registration** — values are registered once, at the same composition stage `ActionVocabulary`/`ResourceRegistry` entries already are (Vocabulary Governance Contract Design §6).
- **Namespacing** — a `<domain>.<purpose>` convention for core-platform values, `plugin:<pluginId>:<purpose>` for Plugin-supplied ones (Vocabulary Governance Contract Design §12).
- **Immutable identifiers** — once registered, a value's own meaning cannot change; a conflicting re-registration is rejected, never applied (Vocabulary Governance Contract Design §7).
- **Retirement without deletion** — a retired value is marked ineligible for new policy authorship, never deleted; every historical `PermissionDecision` that referenced it while active remains valid (Vocabulary Governance Contract Design §8, §17).
- **Plugin governance** — Plugins may register their own values, scoped to their own declared capabilities, namespaced by their own Plugin identity, never exceeding what their own Principal could ever be granted; installation does not imply permission (Vocabulary Governance Contract Design §10; Chapter 15).
- **Reject-on-conflict registration** — mirroring `InMemoryActionVocabulary.register`'s own established behaviour exactly: a registration naming an already-used identifier with a different meaning fails outright, never silently overwrites (Vocabulary Governance Contract Design §6, §11).

**Not frozen:** the registry's own implementation, storage mechanism, serialization format, or exact registration API (Vocabulary Governance Contract Design §6, §19 — explicitly Scope-Lock/Implementation-Plan-tier, and, per this document's own "freeze less" principle, not decided even by this Scope Lock beyond what Section 2.3 above already states).

### 2.4 Permission Model

- **Single Permission Engine, single Permission Policy** — no second evaluator, no second policy instance, anywhere in this Programme's own scope (Chapter 10 §5; Carrier Contract Design §11–12).
- **Fail-closed evaluation** — an absent or unregistered Authorization Purpose value denies by the same default every other unknown value already denies by (`PermissionPolicy.md` §4/§7; Carrier Contract Design §4).
- **Authorization Purpose participates in evaluation** — `DefaultPermissionPolicy`'s own resolution step gains it as an additional, optional matching dimension, the same "extend the single, existing resolution step" pattern already used for the Policy Rule Collision Clarification's own verb-phrase discriminator (Carrier Contract Design §12).
- **No caller-specific exceptions** — the mechanism is uniformly available to every caller, present or future; no "if caller == X" shape anywhere (Authorization Context Contract Design §7; Carrier Contract Design §4, §6).
- **No second authorization system** — every candidate that risked this (Carrier Contract Design's own Candidates B/C/D) was rejected specifically on that ground (Carrier Contract Design §8).
- **Precedence safety, frozen as a principle, not an algorithm.** Whatever precedence order is eventually chosen between the verb-phrase discriminator and the Authorization Purpose discriminator, it must guarantee that **a coarse `(action, resourceType)` rule may never resolve a request for which a more specific, Authorization-Purpose-aware rule was the one actually meant to govern it.** Where any ambiguity exists about which rule governs a given request, the fail-closed default above applies — never the coarser, Authorization-Purpose-blind rule by default. This closes the precise risk Carrier Contract Design §19's own Risk 2 named ("becomes difficult to reason about deterministically... a future Scope Lock must specify... explicitly") without freezing the algorithm itself: it constrains the *outcome space* any future precedence design must satisfy, the same way the fail-closed default above constrains outcomes without specifying implementation.

**Not frozen:** the precedence/matching order's own exact algorithm or internal data structure `DefaultPermissionPolicy`'s own eventual extension uses — only the outcome constraint stated immediately above (see Section 4, "Deferred Decisions," which names where the concrete mechanism is carried forward for resolution).

### 2.5 Memory Retrieval Relationship

- **Gap #54 depends on Authorization Purpose.** Memory Retrieval Contract Design §22: "Gap #54 cannot be fully resolved through Principal, Action, and Resource alone."
- **Knowledge Submission remains blocked until Authorization Purpose exists.** No `PermissionPolicyRule` approving `memory.retrieve`/`memory.retrieve_document` may be added before Authorization Purpose is an evaluable Trust Framework capability (Memory Retrieval Contract Design §22; Implementation Sequence Item 4).
- **Evidence Intelligence remains fail-closed unless separately governed.** Its own currently-correct, deliberately fail-closed retrieval survives Gap #54's resolution "unless a future, separate, explicitly-reasoned decision changes that" (Memory Retrieval Contract Design §14, §22; Implementation Sequence Item 8).
- **Conversational Retrieval uses the same constitutional mechanism.** No bespoke exception; it declares its own registered Authorization Purpose value the same way every other consumer does (Memory Retrieval Contract Design §22; Implementation Sequence Item 7).

### 2.6 Trust Framework Boundaries

Authorization Purpose is **not**:

- **Authentication** — a Chapter 42 concern, untouched; Authorization Purpose is consulted only after identity is already resolved and validated (`IdentityService.md`, "Integration with Permission Engine").
- **Identity** — Principal's own exclusive question; Authorization Purpose is additive, never substitutive (Carrier Contract Design §13).
- **Delegation** — Principal's own `owner` mechanism answers "who backs whose authority"; Authorization Purpose was tested against, and rejected as, a Delegation-based carrier for exactly this reason (Carrier Contract Design §8, Candidate E1; Authorization Context Contract Design §4d).
- **Provenance** — `ExecutionRequest.requestId`/`correlationId` already answer "where did this request come from"; Authorization Purpose inherits, never replaces, that mechanism (Carrier Contract Design §17).
- **Audit** — Authorization Purpose's own audit visibility is identical to every other `ExecutionRequest` content field's, not a new audit mechanism in itself (Carrier Contract Design §17).
- **Execution context** — a candidate carrier shape (extending `RequestOrigin`, or a broad "evaluation envelope") explicitly investigated and rejected (Carrier Contract Design §5/§8, Candidates D/5; Authorization Context Contract Design §8, Candidate 5).
- **Plugin capability** — `ModuleDescriptor`/`ModulePermissionRequirement` remain the sole, separate mechanism by which a Plugin declares what it might need; Authorization Purpose registration is a distinct act a Plugin may additionally perform, never a substitute for or extension of capability declaration (Vocabulary Governance Contract Design §10; Carrier Contract Design §4b).
- **Memory persistence** — no change to Memory Core's own record model, lifecycle, or storage (Memory Retrieval Contract Design §12; Carrier Contract Design §14).
- **Resource identity** — no new `ResourceType`, no new Resource, no `ResourceRegistry` method touched (Carrier Contract Design §14; Vocabulary Governance Contract Design, throughout).

**It is a constitutional authorization dimension only.**

---

## 3. Explicit Non-Responsibilities

This Scope Lock does not fix, and an Implementation Plan remains free to decide:

- Kotlin types, classes, or interfaces of any kind.
- Field names, parameter names, or constructor signatures.
- The registry's own concrete class, storage mechanism, or data structure.
- Serialization or schema representation (`ExecutionRequest.schema.json` amendment shape).
- Diagnostics, logging, or debug tooling.
- Testing strategy or test file organisation.
- Migration code or a migration mechanism (none is authorised or needed until a genuinely breaking change is proposed — Vocabulary Governance Contract Design §15).
- The sequencing of implementation work beyond what `docs/architecture/TRUST_FRAMEWORK_IMPLEMENTATION_SEQUENCE.md` already records as a planning document, not a binding schedule.
- Whether `PermissionDecision` is also extended to echo back the matched Authorization Purpose value (Carrier Contract Design §17 — an open question, not decided here).
- The exact precedence rule between the verb-phrase and Authorization Purpose discriminators (Section 4, below).

---

## 4. Deferred Implementation Decisions

Listed, not answered, per this Scope Lock's own governing instruction:

1. The exact Kotlin shape of the Authorization Purpose value type (a `@JvmInline value class`, or another shape).
2. The exact field name and position on `ExecutionRequest`, and the corresponding `ExecutionRequest.schema.json`/`docs/specifications/volume-01-core-contracts/ExecutionRequest.md` amendment.
3. The concrete registration mechanism and registry data structure.
4. The concrete precedence/matching *algorithm* between the verb-phrase discriminator (Policy Rule Collision Clarification) and the Authorization Purpose discriminator within `DefaultPermissionPolicy`'s own resolution step. **Disclosed explicitly**: the Carrier Contract Design's own Section 19 flagged the general risk here, and Section 2.4 above now freezes the one outcome constraint needed to keep that deferral safe (a coarse rule may never resolve a request a more specific rule was meant to govern). The *algorithm* satisfying that constraint remains open, consistent with this task's own explicit instruction not to freeze precedence implementation — carried forward to whichever narrower document (most likely the Programme's own Unit 3, Permission Policy Extension) takes up the concrete mechanism next; it must not be left to silent implementation-time discovery when that document is written.
5. Whether `PermissionDecision` is extended to echo back the matched Authorization Purpose value.
6. Whether the Authorization Purpose vocabulary ever becomes a formal, ADR-019-governed schema artifact under `docs/schemas/` (left open identically to Action Vocabulary's own unresolved version of the same question — Vocabulary Governance Contract Design §9).
7. The registration-time naming-structure validation mechanism recommended, but not designed, by Vocabulary Governance Contract Design §12.
8. Retrofit of specific Authorization Purpose values onto `DefaultKnowledgeCandidateEvaluator`, `EvidenceIntelligenceInputResolver`, and any other existing consumer (Implementation Sequence Item 4).
9. The policy-content decision of whether, and under what confirmation level, `memory.retrieve`/`memory.retrieve_document` should receive an `APPROVED` rule once distinguishable (Memory Retrieval Contract Design §17, §22 — explicitly not this Scope Lock's, or any prior document's, responsibility).

---

## 5. Risks

- **Risk: this Scope Lock is read as authorising an Implementation Plan to begin immediately.** **Mitigation:** this document authorises no Implementation Plan, no Kotlin, and no staging/commit/push (Status, above); it freezes decisions for whenever that later, separately-authorised work begins.
- **Risk: Section 4's own deferral of the precedence algorithm is mistaken for having silently dropped Carrier Contract Design §19's own risk mitigation.** **Mitigation:** Section 2.4 now freezes the outcome constraint that risk mitigation actually required, and Section 4, item 4 discloses the remaining algorithmic deferral explicitly, naming where the obligation is carried forward rather than allowing it to be forgotten.
- **Risk: a future Implementation Plan treats "not frozen" as "unconstrained."** **Mitigation:** Section 2.2 and 2.3 each state a behavioural constraint (closed value type; reject-on-conflict registration) that bounds implementation freedom even where exact shape is left open.
- **Risk: Plugin governance (Section 2.3) is read as granting Plugins any authority beyond registration.** **Mitigation:** Section 2.6 explicitly distinguishes Authorization Purpose registration from `ModulePermissionRequirement`-style capability grants, and Section 2.3 restates "installation does not imply permission" directly.

---

## 6. Acceptance Criteria

This Scope Lock is satisfied when a future Implementation Plan can be written that:

- Builds only what Sections 2.1–2.6 freeze, in the shape those sections freeze it, without needing to re-derive or reargue any constitutional question already answered by the four source documents.
- Answers every item in Section 4 explicitly, before or as part of that Implementation Plan, rather than leaving any of them to undocumented implementation-time discovery.
- Introduces no mechanism, object, or exception this document's own Section 2 does not already describe as frozen.

---

## 7. Recommendation

Proceed to whichever of the Authorization Purpose Programme's own remaining units (Permission Policy Extension, Existing Consumer Retrofit) is taken up next, informed by this Scope Lock's own boundary, before any Implementation Plan is drafted. This document does not authorise that Implementation Plan and does not begin it.

---

## 8. Independent Constitutional Review Self-Check

- **Accidental API freezing?** Checked against Section 2.2/3 — field names, constructors, and API details are explicitly excluded.
- **Accidental implementation freezing?** Checked against Section 3/5 — registry class, storage, serialization, and precedence algorithm are all explicitly deferred.
- **Second authorization model?** No — Section 2.2/2.4 restate the single-engine, single-policy freeze directly from source.
- **Weakened fail-closed behaviour?** No — Section 2.4 restates the existing default without modification, and additionally freezes a precedence-safety principle ensuring a coarse rule may never resolve a request a more specific, Authorization-Purpose-aware rule was meant to govern.
- **Caller-specific exceptions?** None — Section 2.4 restates uniform availability.
- **`ExecutionRequest` authority violated?** No — Section 2.2 restates ADR-017's own "no parallel request type" guarantee directly.
- **Vocabulary governance drift?** No — Section 2.3 restates Vocabulary Governance Contract Design's own eight freeze points without addition.
- **Plugin privilege escalation?** No — Section 2.6 and Section 2.3 both restate the existing capability ceiling.
- **Contradiction with Gap #54 governance?** No — Section 2.5 restates the Memory Retrieval Contract Design's own §22 conclusions verbatim in substance.
- **Contradiction with the Authorization Purpose Programme?** No — every freeze traces to a Programme-cited source document; nothing here contradicts Programme §7 (Non-Responsibilities) or §9 (carrier/API shape left open).

```
AUTHORIZATION PURPOSE SCOPE LOCK — DRAFT COMPLETE, PENDING INDEPENDENT CONSTITUTIONAL REVIEW
```
