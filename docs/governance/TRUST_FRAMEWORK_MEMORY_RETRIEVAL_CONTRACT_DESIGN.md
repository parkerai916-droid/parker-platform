**Status:** Governance and design only. No Kotlin is implemented, proposed as a diff, or changed by this document. Neither `src/` nor `tests/` is touched. This document does not amend `docs/architecture/10-permission-engine.md` ("Chapter 10"), `docs/architecture/08-resource-registry.md` ("Chapter 8"), `docs/architecture/MEMORY_CORE_CONTRACT_DESIGN_ERRATA_004.md` ("Errata 004"), `docs/architecture/MEMORY_CORE_SCOPE_LOCK.md`, `docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_CONTRACT_DESIGN_V2.md`, or any other frozen or draft governance document — it reads them, cites them, and reasons from them. It does not modify the Parker Conversational Memory Bridge's own uncommitted Admission Unit implementation, does not use that implementation as authority for anything decided below, and does not weaken any existing Trust Framework guarantee to accommodate it. Nothing is staged, committed, or pushed.

# Trust Framework — Memory Retrieval Architecture — Contract Design (Gap #54)

Programme: **Trust Framework, Memory Retrieval Architecture — Contract Design.**

---

## 1. Governing Context

Authorities read fresh, in full or in every relevant section, for this document: `docs/architecture/parker-constitution.md`; `docs/architecture/09-trust-framework.md`; `docs/architecture/10-permission-engine.md` (Chapter 10, in full — noted below as draft, not yet frozen); `docs/architecture/08-resource-registry.md` (Chapter 8); `docs/architecture/MEMORY_CORE_SCOPE_LOCK.md`; `docs/architecture/MEMORY_CORE_CONTRACT_DESIGN_ERRATA_004.md` (Errata 004, in full); `docs/decisions/CDR-005_CONSTITUTIONAL_ADMISSION_OF_PERMISSIONENGINE_PROPOSAL_CLASSES.md`; `docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_CONTRACT_DESIGN_V2.md`; `docs/governance/PROGRAMME_3_UNIT_9_SCOPE_LOCK_CLARIFICATION.md`; `docs/architecture/IMPLEMENTATION_GAPS.md` (Gap #30, Gap #54); `docs/architecture/TRUST_FRAMEWORK_MEMORY_RETRIEVAL_GATING_BLOCKER.md` (the originating blocker document — treated as prior, disclosed evidence, independently re-verified below, not assumed correct); `docs/reviews/CONVERSATIONAL_MEMORY_ADMISSION_COMPLETION_REVIEW.md` and its own Independent Constitutional Review (evidentiary record of the discovery only — not authority for this design, per this document's own Status). Production code read fresh, in full: `src/composition/PermissionFilteredMemoryRetrieval.kt`, `src/composition/PermissionGatedMemoryCore.kt`, `src/runtime/DefaultPermissionPolicy.kt`, `src/runtime/ActionMapper.kt`, `src/interfaces/ResourceRegistry.kt`, `src/contracts/Resource.kt`, `src/contracts/Permission.kt`, `src/runtime/InMemoryResourceRegistry.kt`, `src/runtime/DefaultKnowledgeRetrieval.kt`, `src/runtime/DefaultKnowledgeCandidateEvaluator.kt`, `src/composition/ParkerRuntime.kt`.

---

## 2. Problem Statement

Gap #54: `KnowledgeSubmission.submit` cannot resolve, and therefore cannot promote, any candidate in the live, composed Parker runtime, for any caller. The proximate cause is that `DefaultKnowledgeCandidateEvaluator` resolves a candidate's evidence through `PermissionFilteredMemoryRetrieval`, which is unconditionally fail-closed by construction. This document begins a Contract Design for the underlying Trust Framework question this exposes: **how should Memory Core retrieval be authorised at all**, given that Memory Core records were deliberately never made Resource Registry entries.

---

## 3. Root-Cause Confirmation (Phase 1 — Fresh Architectural Audit)

Independently re-traced against the current repository, not assumed from the blocker document's own account:

1. `PermissionFilteredMemoryRetrieval.buildExecutionRequest` constructs every request with `targetResources = emptyList()` — confirmed by direct re-read, and by that class's own KDoc citing Errata 004 §7 as the reason: "Memory Core records are never Resource Registry entries."
2. `DefaultPermissionPolicy.evaluate` resolves `resourceTypes` from `request.targetResources` via `resourceRegistry.resolve` — an empty list resolves to an empty set, unconditionally, confirmed by direct re-read.
3. `ActionMapper.mapOne` requires `it.resourceType in targetResourceTypes` — an empty set can never contain any `ResourceType`, so every proposed action fails as `RESOURCE_TYPE_MISMATCH`, confirmed by direct re-read, regardless of whether the verb phrase is registered in the `ActionVocabulary` at all.
4. `resourceRegistry.resolve` (`InMemoryResourceRegistry`, confirmed by direct re-read) is a plain, volatile, in-process `mutableMapOf` — no Memory Core record has ever been, or is currently, an entry in it.
5. No alternative authorised resolver for Memory Core retrieval exists anywhere in the current repository — confirmed by search; the only two production consumers of Memory Core retrieval (`EvidenceIntelligenceInputResolver`, `DefaultKnowledgeCandidateEvaluator`) both go through the identical, single `PermissionFilteredMemoryRetrieval` instance `ParkerRuntime.kt` constructs once.

**Gap #54's own central technical claim is confirmed accurate.** No discrepancy was found requiring this Contract Design to stop.

### 3a. Two enrichments the original blocker document did not carry, found during this audit

**Chapter 8 (Resource Registry), in full: "The Resource Registry is the authoritative catalogue of every protected object within Parker... If something is not represented within the Resource Registry, Parker assumes it is inaccessible."** `Resource.kt`'s own KDoc quotes this directly (mis-citing it as "Chapter 4" — a stale cross-reference in that file, not material to this Contract Design's own reasoning, and not corrected here since this document does not amend `src/`). This means the currently-observed fail-closed behaviour is not an oversight — it is the literal, predictable consequence of Chapter 8's own stated invariant, applied to a subsystem (Memory Core) that was deliberately never wired to the Registry (Errata 004 §7's own explicit disclosure: "Memory Core has always been its own subsystem, never wired to `ResourceRegistry`"). Chapter 8 also states plainly that "Resources include Memory" among its named examples — meaning Memory Core's absence from the Registry is a genuine, disclosed tension with Chapter 8's own illustrative scope, not merely an implementation gap unrelated to constitutional intent.

**Errata 004 §7 already anticipated this exact failure mode and already prescribed the shape of a fix — which was never carried out.** Quoted directly: "Correctly evaluating the frozen mapping table therefore requires `ParkerRuntime` composition (Unit 10's own scope, 'production wiring') to supply `PermissionGatedMemoryCore`/`PermissionFilteredMemoryRetrieval` with a `PermissionEngine` (or `DefaultPermissionPolicy` `rules: List<PermissionPolicyRule>` set) capable of deciding `(WRITE, MEMORY)`, `(WRITE, DOCUMENT)`, `(READ, MEMORY)`, `(READ, DOCUMENT)`, `(DELETE, MEMORY)`, and `(DELETE, DOCUMENT)` without requiring a resolved target Resource... this is a Runtime-composition wiring choice... not a change to `PermissionEngine`'s interface... No new permission action or resource type is required to make this work — only a policy/engine composition decision, made once, at `ParkerRuntime` construction." Confirmed by direct re-read of `ParkerRuntime.kt`: exactly one `DefaultPermissionEngine`/`DefaultPermissionPolicy` is ever constructed, and it is the same shared instance passed to `PermissionFilteredMemoryRetrieval`, `DefaultExecutionPipeline`, and every other consumer — the resolution-without-a-Resource capability Errata 004 called for was never built. **Gap #54 is therefore not a newly-arising defect; it is the realisation of a compliance gap Errata 004 itself already, explicitly disclosed as a risk before Unit 10 was ever composed.**

### 3b. A third finding, newly surfaced by this audit specifically

`DefaultKnowledgeRetrieval` (Knowledge Memory's own, separate retrieval surface) already succeeds today, precisely because it does **not** rely on `PermissionFilteredMemoryRetrieval`'s own resolution path at all: it constructs its own `ExecutionRequest` with a real, registered `targetResources = listOf(KNOWLEDGE_RETRIEVAL_RESOURCE_ID)`, resolving to `resourceType = MEMORY`, and an already-registered `PermissionPolicyRule(READ, MEMORY, APPROVED, AUTOMATIC)` — the same rule already exists in the composed policy today (confirmed by direct re-read of `ParkerRuntime.kt`'s own policy construction), registered for Knowledge Retrieval's own purpose.

**This matters directly for any fix to Gap #54's own resolution mechanism**: `PermissionPolicyRule` is keyed only by `(PermissionAction, ResourceType)` — not by the underlying verb phrase (`"memory.retrieve"` versus `"knowledge.retrieve"`) that produced it. Both verb phrases already map to the identical `(READ, MEMORY)` pair. If `PermissionFilteredMemoryRetrieval`'s own resolution were fixed so that its requests could reach policy evaluation at all, they would hit the **same, already-`APPROVED` `(READ, MEMORY)` rule** Knowledge Retrieval's own self-gating already, legitimately relies on — silently converting `EvidenceIntelligenceInputResolver`'s own currently-correct, deliberately fail-closed retrieval (proven by `ParkerRuntimeEvidenceIntelligenceCompositionTest`'s own already-accepted test) into an accidentally-approved one, as an unintended side effect of fixing an unrelated caller's own resolution. **This is a genuine, load-bearing consequence this Contract Design must address directly (Section 17, Section 19) — it was not identified in the original blocker document and changes what "fixing Gap #54" responsibly requires.**

---

## 4. Constitutional Requirements

Distinguished explicitly by tier, per this task's own instruction not to promote implementation convenience into constitutional law.

### 4a. Frozen constitutional text (the Parker Constitution itself, quoted via Chapter 10's own citations, independently re-traceable to the Constitution)

- **Sole-authority.** "There is no path from proposal to execution that does not pass through the Permission Engine... Convenience is never a justification for a shortcut around trust."
- **Fail-closed / inaction-on-uncertainty.** "Where... the Permission Engine cannot establish that an action is authorized, Parker's only correct behavior is to decline to act. Uncertainty about trust never defaults to permissiveness. It defaults to inaction."
- **Owner-policy.** "Parker owns authority. Modules provide capability"; "The owner remains in control" — the Permission Engine applies only policy the owner holds, never inventing or expanding it on its own initiative.
- **Separation of trust and execution.** "Trust may not execute itself... Runtime may not reinterpret what was authorized."

### 4b. Already-accepted governance, Errata/Scope-Lock tier (not yet "the Constitution" itself, but binding on this design as already-adopted authority)

- **No ambient authority; explicit principal identity.** Errata 004 §§2–4, §11: every Memory Core operation carries an explicit `requestingPrincipalId`; no hidden, factory-scoped, or inferred identity is permitted.
- **Memory Core never evaluates permissions.** Memory Core Scope Lock §6 (already cited and relied upon by Programme 3 Unit 9's own Clarification): "Runtime performs all permission decisions before invoking Memory Core... without exception."
- **Retrieval non-disclosure.** Errata 004 §8: a denied caller must receive an identical signal to "no such record" — `null`, never a distinguishable denial type, exception, or sentinel.
- **Deterministic, table-driven policy.** `DefaultPermissionPolicy`'s own established design (no runtime editing, no randomness) — an implementation fact that itself encodes the Owner-policy guarantee's own requirement that policy be applied consistently.
- **Knowledge Memory's own exclusive authority over confidence/evidential-state.** Contract Design V2 §2/§5 — orthogonal to this document's own subject matter, but a boundary this design must not cross.
- **No double permission gating on an already-gated write path.** Established, repeated pattern across this repository (Memory Core Durability Scope Lock §18; every self-gating coordinator: `EvidenceRegistrationCoordinator`, `EvidenceIntelligenceAcceptanceCoordinator`, `DefaultKnowledgeSubmission`) — not literally "constitutional" text, but a load-bearing, repeatedly-reaffirmed architectural principle this design must not violate.

### 4c. Draft, not-yet-frozen governance (Chapter 10 itself) — cited for its own careful reasoning, not treated as equivalent to 4a/4b

Chapter 10's own Status line states plainly: "This chapter is a draft... pending independent constitutional review and a Final Freeze Verification before it is treated as frozen governance. It is not yet frozen." Its own guarantees (Section 5, quoted in 4a above where they restate the Constitution directly) are given full weight here because they trace to the Constitution itself; its own more elaborated, not-yet-frozen commentary (e.g., Section 9's discussion of "alternate permission engines") is treated as strongly persuasive, careful prior reasoning — not as an independently binding source this design cannot revisit if a future, genuine Final Freeze Verification reaches a different conclusion.

### 4d. Implementation facts (not requirements; the current, as-built shape of the mechanism)

- `PermissionAction` already has `READ`, `WRITE`, `DELETE` (among others); `ResourceType` already has `MEMORY`, `DOCUMENT`. No new enum value is required by anything this document considers.
- `ExecutionRequest` already carries `requestId`, `correlationId`, `principalId`, `createdAt` — an existing, adequate correlation/auditability shape, unchanged by anything this document considers.
- `ResourceRegistry`'s only implementation (`InMemoryResourceRegistry`) has no durability of any kind — a fact with direct bearing on Section 16's own analysis of Candidate A, not a requirement in itself.

### 4e. Unresolved design choices (what this document exists to narrow, not to invent)

Whether a Memory Core record is a Resource Registry entry, a differently-resolved protected object, or something else; how permission evaluation granularity should relate to policy-rule granularity now that the Section 3b finding is known; whether `PermissionPolicyRule`'s own `(PermissionAction, ResourceType)` keying remains adequate once more than one verb phrase legitimately needs different outcomes for the identical pair.

---

## 5. Non-Responsibilities

This document does not: write Kotlin; modify tests; modify any frozen or draft governance document; modify `ResourceRegistry`, `DefaultPermissionPolicy`, `ActionMapper`, `PermissionFilteredMemoryRetrieval`, or `PermissionGatedMemoryCore`; select a final policy-representation mechanism where Section 16/19 identifies a genuine prerequisite question instead; resolve Gap #30 (the separate, already-disclosed `PermissionEngine.evaluate` multi-action signature question); redesign Memory Core's own record model, lifecycle, or provenance shape; modify or complete the Parker Conversational Memory Bridge; or authorise a Scope Lock, Implementation Plan, or any implementation work.

---

## 6. Protected-Object / Resource Model — the Critical Design Question

**What is a Memory Core record, constitutionally, for permission evaluation purposes?**

**Answered, with reasonable confidence, on the evidence gathered: a Memory Core record is a protected object resolved through a distinct, already-anticipated governed resolution mode — not a Resource Registry entry, and not represented indirectly through a parent-resource relationship either.**

Reasoning:

1. **Chapter 8's own invariant is satisfied by "represented," not necessarily "individually registered."** Chapter 8's own text is six lines and does not specify *how* a protected object must be represented — only that something wholly unrepresented is inaccessible. Errata 004 §7's own frozen mapping table already represents every Memory Core operation's own `(PermissionAction, ResourceType)` pair, deterministically, from the operation's own name alone — this is a form of representation Chapter 8's own text does not exclude, and Errata 004 (already-accepted, more specific, more recent governance directly on this exact question) explicitly treats it as sufficient: "No new permission action or resource type is required to make this work — only a policy/engine composition decision."
2. **Per-record Resource Registry entries (Candidate A, Section 16) would satisfy Chapter 8's own literal text most directly, but at a cost Chapter 8 itself does not require and this repository's own, separately-governed Memory Core Durability Programme did not anticipate or authorise**: `ResourceRegistry` has no durability. Registering each record individually would make a record's own *authorisability* volatile even though `DurableMemoryCore` already makes the record's own *existence* durable — a genuine, newly-identified regression risk against already-completed, independently-verified work, not merely a migration inconvenience.
3. **Memory Core Scope Lock §6's own boundary ("Memory Core never evaluates permissions... without exception") is most cleanly preserved by keeping the resolution mechanism entirely within Trust Framework's own components** (`DefaultPermissionPolicy`/`ActionMapper`), never requiring Memory Core itself, or a decorator sitting close to it, to acquire a new `ResourceRegistry` dependency it does not have today.

This conclusion narrows the solution space (Section 16) but does not, by itself, select a final mechanism — see Section 17 for why.

---

## 7. Permission Evaluation Model

Unchanged in its outer shape: one `ExecutionRequest`, one `PermissionEngine.evaluate` call, one `PermissionDecision`, per Chapter 10 §5's sole-authority guarantee and per Gap #30's own already-disclosed, still-open "once per request" question (not reopened here). What changes, if anything, is *how* `resourceTypes` is derived when `targetResources` is genuinely, deliberately empty by a caller's own design (Memory Core retrieval's own permanent shape, per Errata 004 §7) — not a new evaluation *model*, an extension to the existing resolution *step* within the same model, for a caller shape Chapter 8/Errata 004 already anticipated but `DefaultPermissionPolicy` was never extended to handle. See Section 16, Candidate D.

---

## 8. Resource/Action Identity

**Action:** already fixed. Errata 004 §7's own frozen table: `memory.retrieve` (`READ`/`MEMORY`, every kind but Document) and `memory.retrieve_document` (`READ`/`DOCUMENT`) — both already implemented, unchanged, in `PermissionFilteredMemoryRetrieval`'s own companion object. This document invents no new action name, per the task's own explicit instruction not to silently invent one where governance already fixes it.

**Resource identity:** this is the one genuinely open question, per Section 6 — not a Resource Registry `ResourceId` naming an individual record. Section 16/19 discusses the candidate ways the `(action, resourceType)` pair may still be derived without one.

---

## 9. Evaluation Granularity

**Unchanged from Errata 004 §9's own already-frozen requirement: once per returned/candidate record, never once per whole query or whole retrieval act.** `PermissionFilteredMemoryRetrieval`'s own current structure already does this correctly (a `filter` call per query-based method, one `isApproved` call per direct lookup) — nothing this document considers proposes coarsening it. This is a **frozen constraint on any candidate direction**, not an open question: Candidate B/C's own coarser, act-level shape (Section 16) is evaluated against this constraint directly, and found wanting for general Memory Core retrieval specifically (though it remains correct, and already proven correct, for Knowledge Retrieval's own distinct, coarser-grained act).

---

## 10. Provenance and Auditability

Unaffected by anything this document considers. `ExecutionRequest.requestId`/`correlationId`/`createdAt`/`principalId` already exist and already flow through `PermissionFilteredMemoryRetrieval`'s own request construction; no candidate direction in Section 16 changes this shape.

---

## 11. Failure and Non-Disclosure Behaviour

Unaffected. Errata 004 §8's own already-frozen non-disclosure guarantee (a denied caller receives `null`, indistinguishable from "no such record") is preserved by every candidate direction Section 16 considers — none of them touches `PermissionFilteredMemoryRetrieval`'s own return-type shape or introduces a new denial signal.

---

## 12. Relationship to Memory Core

No change to Memory Core's own record model, candidate types, lifecycle states, or provenance shape. Memory Core Scope Lock §6's own "never evaluates permissions" boundary is preserved by every candidate this document considers — the resolution mechanism, whatever it becomes, lives in Trust Framework's own components, never inside `InMemoryMemoryCore`/`DurableMemoryCore` themselves.

## 13. Relationship to Knowledge Memory

`DefaultKnowledgeRetrieval`'s own, already-working, act-level self-gating (Section 3b) is not touched or weakened by anything this document proposes — it is cited as evidence, not as a component to be redesigned. `DefaultKnowledgeCandidateEvaluator`'s own resolution failure (Gap #54's own proximate symptom) would be fixed if and only if `PermissionFilteredMemoryRetrieval`'s own resolution is fixed **and** Section 3b's own newly-found policy-collision risk is separately, correctly addressed — fixing only the first would not safely fix Knowledge Submission's own promotion path without also silently changing Evidence Intelligence's own behaviour.

## 14. Relationship to Evidence Intelligence

`EvidenceIntelligenceInputResolver`'s own currently-correct, deliberately fail-closed retrieval (`ParkerRuntimeEvidenceIntelligenceCompositionTest`'s own already-accepted "even a record that genuinely exists" test) must remain fail-closed after this blocker is resolved, unless a future, separate, explicitly-reasoned decision changes that — this document takes no position on whether Evidence Intelligence's own retrieval *should* eventually become permissive, and treats its current fail-closed behaviour as a guarantee to preserve, not an accident to correct as a side effect of an unrelated fix.

## 15. Relationship to Existing Trust Framework Components

`PermissionEngine`/`DefaultPermissionEngine` remain the sole authority (Chapter 10 §5) — no candidate direction in Section 16 constructs a second one. `ActionMapper`/`ActionVocabulary` remain the sole action-resolution mechanism — no candidate introduces a second, competing lookup. `ResourceRegistry` remains untouched by the direction this document finds best-supported (Section 6) — Candidate A, which would touch it, is the one direction this document explicitly does not recommend (Section 18).

---

## 16. Candidate Directions Considered

Investigated per this document's own governing task, including the three directions the originating blocker document named, plus one additional direction this audit's own primary-source research surfaced directly from Errata 004's own already-written text.

### Candidate A — Register every Memory Core record as its own Resource Registry entry

- **Constitutional fit:** strongest *literal* fit to Chapter 8's own text ("Resources include Memory"); weakest fit to Memory Core Scope Lock §6 (would require new Memory-Core-adjacent coupling to `ResourceRegistry`) and to the newly-discovered durability regression below.
- **Ownership implications:** Memory Core, or a component sitting immediately beside it, would need a `ResourceRegistry` dependency it has never had.
- **Resource Registry implications:** volume growth from a small, fixed set of coarse resources (today, roughly a dozen, all Programme-level) to a per-record set with no natural upper bound over a household's lifetime.
- **Permission-evaluation semantics:** would work mechanically — `targetResources` would resolve normally, exactly like every other existing proposal class.
- **Action/resource vocabulary implications:** none beyond what already exists.
- **Lifecycle implications:** `ResourceLifecycleState` (`CREATED → REGISTERED → AVAILABLE → UPDATED → ARCHIVED → DELETED`) and `MemoryCoreRecordStatus` are two independent state machines that would need reconciling — a real design burden, not addressed by anything already governed.
- **Provenance implications:** none directly.
- **Runtime composition impact:** every `MemoryCore` write path gains a new failure mode (registration failure) it does not have today.
- **Effect on existing callers:** `EvidenceRegistrationCoordinator`/`EvidenceIntelligenceAcceptanceCoordinator`'s own coarse, act-level self-gating would become partially redundant with a simultaneously-existing fine-grained, per-record model.
- **Effect on Evidence Intelligence / Knowledge Submission / future retrieval:** would work, mechanically, for all three, same as any caller — but see the durability finding below, which affects all three identically.
- **Failure behaviour:** a new, currently-nonexistent failure mode (registration).
- **Migration cost:** high.
- **Risk of special-case architecture:** low (general, not caller-specific).
- **Second-authorization-model risk:** low (reuses the existing mechanism as-is).
- **Newly-identified, disqualifying risk (this audit, Section 6 item 2): `InMemoryResourceRegistry` has no durability.** Every record's own *authorisability* would be lost on every restart even though `DurableMemoryCore` already durably preserves the record's own *existence* — a direct, severe regression against Memory Core Durability's own already-completed, independently-verified guarantees (Units 1–10), not a hypothetical concern.

**Not recommended** — Section 18.

### Candidate B — A narrow, caller-scoped exception for a principal reading back a record it was itself just authorised to write

- As originally framed in the blocker document, this risks being indistinguishable from exactly the caller-specific special case this task's own governing instructions forbid ("if caller == KnowledgeSubmission then allow"). Reframed generally (principal-scoped, not caller-class-scoped: "a principal may resolve a record whose provenance shows that principal's own prior, already-authorised write"), it still introduces a wholly new authorization *concept* — self-ownership-implies-read-access — that no existing Trust Framework document currently states or governs. Whether "the creator of a record may always read it back" is even a sound *policy* is itself an unresolved policy question this document is not positioned to settle by mechanism alone; conflating the two would itself violate this task's own instruction not to promote implementation convenience into constitutional law.
- **Verdict:** not rejected outright, but confirmed to require its own, separate constitutional grounding before it could be considered a lawful mechanism — not merely a resolution-layer fix. Not recommended as this Gap's own resolution.

### Candidate C — A distinct, new evaluation path for Knowledge Submission's own resolution step specifically

- As originally framed, this is the clearest example of the forbidden pattern ("if caller == KnowledgeSubmission, use a different gate"). A generalised reframing — an internal, system-scoped resolution capability usable by any already-permission-gated Trust Framework component verifying its own just-written record, never owner-facing, never a substitute for `PermissionFilteredMemoryRetrieval` — is conceivable, but risks becoming a second, parallel resolution path (and therefore arguably a second authority, contrary to Chapter 10 §9's "never construct, implement, or substitute a second authority") unless very carefully scoped and justified on its own terms, which this document does not attempt.
- **Verdict:** weakest-supported of the four; not recommended.

### Candidate D — Extend the existing, single `DefaultPermissionPolicy`'s own resolution step to derive `(action, resourceType)` directly from the Action Vocabulary's own already-fixed mapping when `targetResources` is genuinely empty by a caller's own design

*(Not named in the original blocker document; surfaced directly from Errata 004 §7's own text during this audit, Section 3a.)*

- **Constitutional fit:** strongest of the four — this is what Errata 004, already-accepted governance, already explicitly called for at Unit 10's own composition time, and what was never actually built.
- **Ownership implications:** stays entirely within Trust Framework's own components; Memory Core Scope Lock §6 is not touched.
- **Resource Registry implications:** none — no growth, no durability exposure.
- **Permission-evaluation semantics:** the `(action, resourceType)` pair becomes derivable two ways — resolved from a targeted Resource (existing, unchanged, for every caller that has one) or declared directly on the vocabulary entry (new, additive, only for a caller that structurally has none) — both deterministic, neither caller-specific.
- **Action/resource vocabulary implications:** none new; makes the already-fixed table (Errata 004 §7) actually reachable.
- **Lifecycle implications:** none.
- **Runtime composition impact:** small in shape (a resolution-step enhancement to one already-shared class, or an equivalent composition-time choice), though this document does not commit to the exact mechanism (Section 19).
- **Effect on existing callers:** none for any caller that already has a resolvable target Resource (the new path only ever activates when none exists).
- **Effect on Evidence Intelligence:** **not neutral** — Section 3b's own finding applies directly. Fixing resolution alone would let `EvidenceIntelligenceInputResolver`'s own retrieval reach the already-`APPROVED` `(READ, MEMORY)` rule Knowledge Retrieval's own self-gating already relies on, silently converting a deliberately fail-closed path into an approved one. **This is the central, unresolved risk any adoption of Candidate D must first close** (Section 19).
- **Effect on Knowledge Submission:** would allow `DefaultKnowledgeCandidateEvaluator.resolve()` to genuinely succeed for the first time — the direct fix Gap #54 needs, *conditioned on* Section 3b's own risk being separately closed first.
- **Effect on future conversational retrieval:** would work, on the same general terms as any other caller, once built.
- **Failure behaviour:** unchanged — a genuinely unresolvable action/resource still denies, exactly as today.
- **Migration cost:** low, structurally — no data migration, no new dependency, no new record-level state.
- **Risk of special-case architecture:** low — the new resolution path is general-purpose, usable by any future caller shaped like Memory Core's own retrieval (structurally resourceless by design), not keyed to any specific caller identity.
- **Second-authorization-model risk:** low, provided the mechanism is built as an extension *within* the single, existing `DefaultPermissionPolicy`/`PermissionEngine`, not a parallel evaluator.

**Best-supported by existing governance — but not yet safely selectable as final, per Section 3b's own finding.** See Section 17, Section 19.

---

## 17. Selected Architecture

**Resolved at the mechanism level by two subsequent, dedicated Clarifications; policy content remains undecided.** This section originally deferred selection pending resolution of Section 19's own two items. Both are now resolved:

- **Item 2 (policy-rule collision)** — resolved by `docs/governance/TRUST_FRAMEWORK_MEMORY_RETRIEVAL_POLICY_RULE_COLLISION_CLARIFICATION.md` (Adopted): `DefaultPermissionPolicy`'s own rule model gains an optional, per-verb-phrase discriminator, using data (`ActionMappingResult.Resolved.proposedAction`) `ActionMapper` already produces, so `memory.retrieve` and `knowledge.retrieve` can lawfully receive independent policy outcomes despite sharing the identical `(READ, MEMORY)` pair.
- **Item 1 (resolution-derivation mechanism)** — resolved by `docs/governance/TRUST_FRAMEWORK_MEMORY_RETRIEVAL_RESOLUTION_DERIVATION_MECHANISM_CLARIFICATION.md` (Adopted): Candidate D's own resolution extension is narrowed to an explicit, closed, per-verb-phrase composition-time configuration on `DefaultPermissionPolicy` (the same tier Errata 004 §7 itself already named), authorised only for the closed set of Errata-004-§7-enumerated action names when `targetResources` is structurally empty — never a general "empty `targetResources`" fallback. Fully sound for the two action names this Contract Design's own Gap #54 problem statement concerns (`memory.retrieve`, `memory.retrieve_document`); `memory.transition_status`/`memory.delete_record` (dormant `PermissionGatedMemoryCore`, out of this Gap's own scope) remain an open, separately-deferred sub-question.

**What is decided:** a Memory Core record should not become an individually-registered Resource Registry entry (Candidate A, rejected — Section 18); Memory Core's own retrieval boundary should not be registered as a Resource Registry entry at all, even a fixed, boundary-level one mirroring Knowledge Retrieval's own precedent (foreclosed directly by Errata 004 §7's own repeated, explicit "`targetResources` is always `emptyList()`... there is never a Resource Registry entry to name" text — considered and rejected during the Resolution Derivation Mechanism Clarification's own audit); and the general shape of a lawful fix lies in extending the existing, single `DefaultPermissionPolicy` mechanism, at two of its own internal steps (rule-outcome matching and derivation eligibility), never in inventing a caller-specific exception (Candidates B/C, both rejected in their originally-framed forms).

**What remains not decided, and is not decided by either Clarification:** whether, and under what confirmation level, `memory.retrieve`/`memory.retrieve_document` should actually receive an `APPROVED` `PermissionPolicyRule` outcome. This is a separate, later, policy-content decision neither Clarification makes. **It may not yet safely be made**, because of a newly-disclosed prerequisite — see Section 19, item 5.

---

## 18. Explicitly Rejected Alternatives

- **Candidate A (per-record Resource Registry entries)** — rejected on two independent grounds: (1) it introduces a new coupling between Memory Core and `ResourceRegistry` that Memory Core Scope Lock §6's own boundary discipline does not require and this document's own reasoning (Section 6) finds unnecessary once Errata 004's own already-anticipated alternative is considered; (2) `ResourceRegistry`'s own total absence of durability would make every Memory Core record's own authorisability volatile across a restart even though its own existence is already durable — a genuine regression against already-completed, independently-verified work.
- **Candidate B, as originally framed (caller-scoped exception for the record's own creator)** — rejected as originally stated, because it requires inventing a new authorization concept (self-ownership-implies-read-access) with no existing constitutional or governance grounding, conflating a mechanism question with an unresolved policy question.
- **Candidate C, as originally framed (a dedicated Knowledge-Submission-specific resolution path)** — rejected as originally stated, as the clearest instance of the caller-specific special case this task's governing instructions explicitly forbid.
- **Any mechanism that would resolve Gap #54 by relaxing `PermissionFilteredMemoryRetrieval`'s own per-record evaluation granularity (Section 9) to an act-level check** — rejected, as it would silently weaken an already-frozen (Errata 004 §9) guarantee for every existing and future caller of general Memory Core retrieval, not only the caller motivating the fix.

---

## 19. Deferred Implementation Choices

1. **RESOLVED — the exact representation of Candidate D's own resolution extension.** Resolved by `docs/governance/TRUST_FRAMEWORK_MEMORY_RETRIEVAL_RESOLUTION_DERIVATION_MECHANISM_CLARIFICATION.md` (Adopted): an explicit, closed, per-verb-phrase composition-time configuration on `DefaultPermissionPolicy`, not a change to `ActionVocabulary`'s own general contract. Sound for `memory.retrieve`/`memory.retrieve_document`; `memory.transition_status`/`memory.delete_record` remain a separately-deferred sub-question (dormant `PermissionGatedMemoryCore`, out of this Gap's own scope).
2. **RESOLVED — the prerequisite policy-granularity question (Section 3b, Section 17).** Resolved by `docs/governance/TRUST_FRAMEWORK_MEMORY_RETRIEVAL_POLICY_RULE_COLLISION_CLARIFICATION.md` (Adopted): `DefaultPermissionPolicy` gains an optional, per-verb-phrase rule discriminator; a verb-phrase-specific rule takes precedence over a coarser `(action, resourceType)`-only rule addressing the same pair. The "distinct `ResourceType` for Knowledge Retrieval" candidate this section previously listed as cheapest was, on fresh audit during that Clarification, found unlawful in both its sub-variants (minting a new `ResourceType` value is forbidden by the frozen `PROGRAMME_3_UNIT_9_PERMISSION_ENFORCEMENT_MECHANISM_CLARIFICATION.md` §11; reusing an existing value such as `WORLD_MODEL` would misclassify the Resource Registry entry) and is no longer recommended — superseded by that Clarification's own Section 3.1/5.
3. **Whether Candidate B's own underlying policy question (creator-read-back) is ever worth pursuing as its own, separately-governed authorization concept** — deferred entirely; not investigated further by this document.
4. **CDR-005's own domain self-certification question** for whichever final mechanism is eventually selected — not performed here. Both Clarifications above independently confirmed CDR-005 is not required for the mechanism-level work itself, since neither introduces a new proposal class; `memory.retrieve` and `knowledge.retrieve` are both already-classified, already-gated acts.
5. **NEW — shared-decorator indistinguishability between Knowledge Submission's and Evidence Intelligence's own `memory.retrieve` calls.** Disclosed by the Resolution Derivation Mechanism Clarification's own Section 7, not previously identified by any document in this governance chain: `EvidenceIntelligenceInputResolver` and `DefaultKnowledgeCandidateEvaluator` both call `MemoryRetrieval`'s direct-lookup methods on the same, single, shared `PermissionFilteredMemoryRetrieval` instance, via the identical verb phrase, with no consumer-identity concept carried anywhere in the request. **A future policy-content decision to approve `memory.retrieve` (to unblock `KnowledgeSubmission.submit`) cannot, as the system is presently composed, avoid simultaneously approving `EvidenceIntelligenceInputResolver`'s own retrieval** — directly threatening Section 14's own guarantee. Resolving this requires a genuinely new architectural decision (for example, separately-composed `MemoryRetrieval` decorator instances per consumer, each with its own distinct verb phrase) that neither prior Clarification, nor this Contract Design, makes. **This is now the one remaining prerequisite standing between full mechanism-level resolution and any policy-content decision to approve `memory.retrieve`.**

---

## 20. Risks

- **Risk: a future pass resolves item 2 above in a way that reintroduces a caller-specific rule under a different name** (e.g., a policy dimension that is, in substance, "if verb phrase equals X"). **Mitigation:** any future resolution must be judged against this document's own Section 4b "no caller-specific special cases" requirement directly, not merely against whether it compiles.
- **Risk: this document's own two-step disclosure (fix resolution, then separately fix policy granularity) is read as license to ship the first step alone.** **Mitigation:** Section 17 states explicitly that Candidate D is not yet safely selectable in isolation; a future Scope Lock must not authorise the resolution-only half without the policy-granularity half.
- **Risk: Chapter 10's own not-yet-frozen status is used to justify a broader reinterpretation of Trust Framework guarantees than this specific gap requires.** **Mitigation:** Section 4a grounds every requirement this document treats as load-bearing in the Constitution's own text directly, not in Chapter 10's own draft elaboration alone.
- **Risk: the durability finding against Candidate A (Section 16) is itself relied upon without independent re-verification by a future reader.** **Mitigation:** the finding is traceable to a single, direct source (`InMemoryResourceRegistry`'s own implementation, quoted in Section 3), independently re-checkable in one file.

---

## 21. Recommendation

**Superseded in part.** The prerequisite design pass this section originally called for (Section 19, item 2, the policy-rule collision) is complete: `docs/governance/TRUST_FRAMEWORK_MEMORY_RETRIEVAL_POLICY_RULE_COLLISION_CLARIFICATION.md` (Adopted) resolved it via a per-verb-phrase rule discriminator, not the "distinct `ResourceType`" candidate this section previously recommended checking first (found unlawful during that Clarification's own audit — see Section 19, item 2). The second prerequisite (Section 19, item 1, the resolution-derivation mechanism) is also now resolved: `docs/governance/TRUST_FRAMEWORK_MEMORY_RETRIEVAL_RESOLUTION_DERIVATION_MECHANISM_CLARIFICATION.md` (Adopted).

**A new, narrower prerequisite pass is now recommended in their place** — scoped to Section 19's own new item 5, the shared-decorator indistinguishability between Knowledge Submission's and Evidence Intelligence's own `memory.retrieve` calls. This is the one remaining question standing between the now-resolved mechanism and any lawful policy-content decision to approve `memory.retrieve`/`memory.retrieve_document`. That pass should itself follow the same Planning Review → Boundary Review → Contract Design discipline this repository already applies throughout, and should not be conflated with, or used to justify reopening, either now-Adopted Clarification.

**A Scope Lock covering only the mechanism-level work both Clarifications together authorise** (verb-phrase-specific rule matching, plus closed-set, no-Resource-required derivation for `memory.retrieve`/`memory.retrieve_document`, with no `PermissionPolicyRule` content approving either) **could lawfully begin on the evidence gathered here.** A Scope Lock or Implementation Plan step that also adds a rule approving `memory.retrieve`/`memory.retrieve_document` — the step that would actually fix Gap #54's own live symptom — may not yet lawfully begin, pending resolution of item 5.

The Parker Conversational Memory Bridge's own second unit (conversational retrieval) should continue to wait, as already established, until Gap #54 is fully, safely resolved — which now visibly includes item 5, not the two originally-identified items alone.

---

## 22. Independent Constitutional Review Self-Check

Performed by the author before requesting external review, per this task's own required structure — a genuine external Independent Constitutional Review follows as a separate document, not replaced by this section.

- **Fail-closed semantics preserved?** Yes, on the evidence gathered — Candidate D changes only whether resolution is *attempted* via a second, additive path; every existing DENIED outcome for a genuinely unresolvable action/resource is unchanged, and Section 3b's own finding is treated as a blocking risk, not glossed over.
- **Ambient authority introduced?** No — every candidate retains explicit `requestingPrincipalId`; no candidate reads implicit context.
- **Memory Core improperly absorbed into Resource Registry semantics?** No — Candidate A (the direction that would do this) is explicitly rejected.
- **Second authorization system invented?** Not by the recommended direction (Candidate D, kept within the single existing `PermissionEngine`/`DefaultPermissionPolicy`); flagged as a live risk for Candidate C if pursued carelessly (Section 16).
- **Caller-specific exceptions present?** None in the recommended direction; Candidates B and C are both explicitly rejected in their original, caller-specific framings.
- **Evidence Intelligence constitutionally intact?** Explicitly treated as a hard constraint (Section 14) and as the reason this document does not force a final selection (Section 17).
- **Sufficiently specified resource/action identity?** Action identity: fully specified, already frozen (Section 8). Resource identity: deliberately, explicitly left open, with the reason stated (Section 6, Section 17) — not an oversight.
- **Implementation decisions frozen prematurely?** No — Section 17/19 explicitly declines to freeze the final mechanism, per this task's own explicit permission to do so.
- **Any existing frozen governance contradicted?** None found; every requirement cited in Section 4a/4b is traced to its own primary source, and no candidate direction this document recommends alters any of them.

```
TRUST FRAMEWORK MEMORY RETRIEVAL CONTRACT DESIGN — DRAFT COMPLETE, PENDING INDEPENDENT CONSTITUTIONAL REVIEW
```
