**Status:** Governance and architecture only. No Kotlin is implemented, proposed as a diff, or changed by this document. Neither `src/` nor `tests/` is touched. This document does not amend `docs/architecture/parker-constitution.md`, `docs/architecture/09-trust-framework.md`, `docs/architecture/10-permission-engine.md`, `docs/architecture/08-resource-registry.md`, `docs/architecture/action-mapping.md`, `docs/architecture/IdentityService.md`, `docs/architecture/MEMORY_CORE_CONTRACT_DESIGN_ERRATA_004.md`, `docs/specifications/volume-03-core-interfaces/PermissionPolicy.md`, or any other frozen or draft governance document — it reads them, cites them, and reasons from them. It does not authorise a Scope Lock, an Implementation Plan, or any implementation work. Nothing is staged, committed, or pushed.

# Trust Framework — Authorization Context (Consumer Identity) — Contract Design

Programme: **Trust Framework Development Programme, Authorization Context (Consumer Identity).**

---

## 1. Governing Context

Read fresh for this document, in full or in every relevant section, independent of any prior task's own conclusions: `docs/architecture/parker-constitution.md`; `docs/architecture/09-trust-framework.md` (Chapter 9, in full — 8 lines); `docs/architecture/10-permission-engine.md` (Chapter 10, §5, §9); `docs/architecture/08-resource-registry.md` (Chapter 8, in full); `docs/architecture/action-mapping.md` (in full); `docs/specifications/volume-03-core-interfaces/PermissionPolicy.md` (in full); `docs/architecture/IdentityService.md` (in full — including "Trust Relationships," "Authentication Flow," "Integration with Permission Engine," read for the first time in this specific governance chain); `docs/architecture/MEMORY_CORE_CONTRACT_DESIGN_ERRATA_004.md` §2–4, §7, §8, §11; `docs/governance/TRUST_FRAMEWORK_MEMORY_RETRIEVAL_CONTRACT_DESIGN.md` and its two subsequent Clarifications (`TRUST_FRAMEWORK_MEMORY_RETRIEVAL_POLICY_RULE_COLLISION_CLARIFICATION.md`, `TRUST_FRAMEWORK_MEMORY_RETRIEVAL_RESOLUTION_DERIVATION_MECHANISM_CLARIFICATION.md`) — treated as prior evidence and the originating trigger for this Programme, not as authority for anything concluded below, per this task's own explicit "ignore previous conclusions, re-derive everything independently" instruction. Production code read fresh, in full: `src/contracts/ExecutionRequest.kt`, `src/contracts/Principal.kt`, `src/contracts/Permission.kt`, `src/contracts/Resource.kt`, `src/contracts/ActionMapping.kt`, `src/contracts/Module.kt`, `src/interfaces/PermissionEngine.kt`, `src/interfaces/ResourceRegistry.kt`, `src/runtime/DefaultPermissionEngine.kt`, `src/runtime/DefaultPermissionPolicy.kt`, `src/runtime/ActionMapper.kt`, `src/runtime/InMemoryIdentityService.kt`, `src/runtime/DefaultKnowledgeCandidateEvaluator.kt`, `src/runtime/DefaultKnowledgeRevisionEvaluator.kt`, `src/runtime/EvidenceIntelligenceInputResolver.kt`, `src/composition/PermissionFilteredMemoryRetrieval.kt`, `src/composition/EventPublishingMemoryCore.kt`, `src/runtime/DurableMemoryCore.kt`, `src/runtime/MemoryCoreRecovery.kt`, `src/runtime/InMemoryConversationEngine.kt`, `src/runtime/ResponseComposer.kt`, `src/composition/ParkerRuntime.kt` — plus an exhaustive grep across all of `src/` and `docs/architecture/`, `docs/specifications/` for any existing "consumer identity," "subsystem identity," "capability identity," "purpose identity," "execution context," or "authorization context" concept.

---

## 2. Purpose

Determine whether Parker's Trust Framework requires a new, governed **Authorization Context (Consumer Identity)** concept — additional, governed information accompanying a permission request that lets the Trust Framework lawfully distinguish constitutionally different internal consumers performing the identical action on the identical resource, without introducing caller-specific exceptions. This document performs the independent architectural audit Phase 1 requires, defines the constitutional problem if no existing concept suffices, surveys candidate models, and — if the evidence supports it — selects one at the design level only.

---

## 3. Existing Architecture — The Fresh Trace

Re-derived directly from primary source, not assumed from any prior document:

1. **Caller** constructs an `ExecutionRequest` (`src/contracts/ExecutionRequest.kt`): `requestId`, `principalId`, `origin: RequestOrigin`, `intent: String`, `targetResources: List<ResourceId>`, `proposedActions: List<String>`, `priority`, `createdAt`, `correlationId`, plus optional `sessionId`, `riskEstimate`, `expiresAt`, `metadata: Map<String, String>`.
2. **`PermissionEngine.evaluate(request)`** (`DefaultPermissionEngine`) resolves `request.principalId` via `IdentityService.resolve` as its first step (`IdentityService.md`, "Integration with Permission Engine": "MUST resolve `request.principalId`... before any action-mapping or Resource-sensitivity logic runs"). A `Suspended`/`Revoked`/`Archived`/`Created` Principal short-circuits to `DENIED` before policy is ever consulted.
3. **`DefaultPermissionPolicy.evaluate`** derives `resourceTypes` from `request.targetResources` via `ResourceRegistry.resolve`, then calls `ActionMapper.map(request.proposedActions, resourceTypes)`.
4. **`ActionMapper.mapOne`** looks up each proposed-action string in the `ActionVocabulary` (a Planner-owned, deterministic table, per `action-mapping.md`), and intersects the entry's declared `ActionResourceMapping`s against the resolved `resourceTypes`, producing `ActionMappingResult.Resolved(proposedAction, mappings)` or `Failed`.
5. **`DefaultPermissionPolicy`** matches the resolved `(PermissionAction, ResourceType)` pair(s) — now also, per the Adopted Policy Rule Collision Clarification, optionally the specific verb phrase — against its own fixed `PermissionPolicyRule` table, and produces a `PermissionDecision`.

**Information that actually participates in authorization today:** `principalId` (identity/status gate, and — per `PermissionPolicy.md` §3/§9 — a legitimate, anticipated policy-matching input, though no rule currently uses it); `proposedActions` (verb phrase, now a legitimate rule-matching discriminator per the Adopted Collision Clarification); `targetResources`/derived `resourceTypes`. **Information that does not, and by design must not:** `intent` (`action-mapping.md`: "the Permission Engine never interprets free text itself... natural-language-to-vocabulary matching [is] Planner/Chapter 20... territory"); `metadata` (unstructured, ungoverned, never read by `DefaultPermissionPolicy`).

---

## 4. Phase 1 — Independent Audit for an Existing Consumer-Identity Concept

Ignoring every prior document's own conclusions, re-derived from primary source:

### 4a. Exhaustive literal search — no hit

Grepped across all of `src/`, `docs/architecture/`, `docs/specifications/` for `ConsumerId`, `SubsystemId`, `CapabilityId`, `ExecutionContext`, `AuthorizationContext`, `PurposeId`, `InvokerId`, `CallerId` (exact identifiers) — **zero matches anywhere in the repository.**

### 4b. Two structurally-present but semantically-distinct existing fields — both checked, both ruled out

- **`RequestOrigin`** (`VOICE, TEXT, SCHEDULED_TASK, AGENT, PLUGIN, HOME_ASSISTANT_EVENT, ANDROID_EVENT, REMOTE_INTERFACE`) does reach `ExecutionRequest` and does participate structurally, but answers a different question: which *external input channel* triggered the overall interaction, never which *internal, already-trusted* subsystem is making a specific permission check. Confirmed empirically: `PermissionFilteredMemoryRetrieval.buildExecutionRequest` hardcodes `origin = RequestOrigin.AGENT` for every call it makes, regardless of whether the caller is `DefaultKnowledgeCandidateEvaluator` or `EvidenceIntelligenceInputResolver` — `RequestOrigin` cannot, and structurally does not, distinguish them.
- **`ModuleId`/`ModuleDescriptor`/`ModulePermissionRequirement`** (`src/contracts/Module.kt`) governs *external, installable Plugin* capability declarations, checked only at owner-enable time. Its own KDoc: "every actual invocation is still independently evaluated by `PermissionEngine.evaluate`, exactly as any other `ExecutionRequest` is." `ExecutionRequest` carries no `moduleId` field at all — Module identity never reaches the per-request permission decision this Programme concerns itself with.

### 4c. The closest candidate, taken seriously — the ad hoc `system.*` Principal convention

An already-widespread, KDoc-disclosed (not Trust-Framework-governed) naming convention: `system.parker`, `system.conversation-engine`, `system.response-composer`, `system.planner-runtime`, `system.task-manager-runtime`, `system.memory-core`, `system.memory-core-recovery`, `system.durable-memory-core`, `system.knowledge-memory` — a distinct, fixed `PrincipalId` per internal component, referenced by `MemoryCoreRecovery.kt` as "`ParkerRuntime`'s own established `system.*` Principal-naming convention." At first inspection, this looks exactly like an already-existing "subsystem identity" mechanism. Pressed further, it does not qualify:

- **Ungoverned.** No Trust Framework document (`IdentityService.md`, `Principal.md`, Chapter 9, Chapter 10) establishes "one distinct system Principal per internal subsystem, for authorization-differentiation purposes" as a deliberate mechanism. It emerged as an organic, per-class implementation convenience.
- **Inconsistently applied, and demonstrably insufficient even where used.** `DefaultKnowledgeCandidateEvaluator` *and* `DefaultKnowledgeRevisionEvaluator` — two genuinely distinct Knowledge Memory-internal consumers (candidate evaluation and revision evaluation are different acts) — share the *identical* `SYSTEM_PRINCIPAL_ID = PrincipalId("system.knowledge-memory")`. If a fixed system Principal per consumer were the operative mechanism, these two would already need distinct identities and do not have them. **Evidentiary weight, stated precisely:** `DefaultKnowledgeCandidateEvaluator` is confirmed live — wired into `ParkerRuntime.kt` and sharing the same `permissionFilteredMemoryRetrieval` instance `EvidenceIntelligenceInputResolver` also receives (Section 15). `DefaultKnowledgeRevisionEvaluator` is confirmed **dormant** — grepped directly, it is never constructed or wired anywhere in `ParkerRuntime.kt`'s own composition root, the same dormancy status the Resolution Derivation Mechanism Clarification already found and disclosed for `PermissionGatedMemoryCore`. This pairing is therefore latent evidence of the ad hoc pattern's own organic reuse for a genuinely different act — not a second, currently-live collision on par with the confirmed-live Evidence-Intelligence/Knowledge-Submission pair (Section 15) — and is cited here as exactly that: evidence the pattern recurs by construction, not evidence of a second live incident. Meanwhile `EvidenceIntelligenceInputResolver` uses no `system.*` identity at all — it propagates `request.requestingPrincipalId`, the real, caller-supplied principal from its own `EvidenceAnalysisRequest`.
- **Independently confirmed broken on its own terms.** Grepped `ParkerRuntime.kt`'s own identity-registration stage directly: only five Principals are ever registered `ACTIVE` — `system.parker`, `system.conversation-engine`, `system.response-composer`, `system.planner-runtime`, `system.task-manager-runtime`. `system.knowledge-memory` (used by both `DefaultKnowledgeCandidateEvaluator` and `DefaultKnowledgeRevisionEvaluator`) is **never registered**. Since `InMemoryIdentityService` is a strict, explicit-registration-only store with no fallback, and `DefaultPermissionEngine.evaluate`'s own first step is `identityService.resolve(request.principalId) ?: return deniedDecision(request)`, every Memory Core permission check either evaluator makes is *already*, today, independently denied at the **identity** layer — a live defect, additional to and independent of Gap #54's own already-documented causes, not created or investigated further by this document, but disclosed here as direct, concrete evidence that ad hoc subsystem-identity substitution is not merely theoretically insufficient but is actively broken in the live system.

**Not an existing, governed concept.** The pattern is real, but informal, inconsistent, and currently non-functional on its own terms.

### 4d. The most serious candidate — Principal Delegation (`owner`)

`Principal.owner: PrincipalId?` and `IdentityService.md`'s own "Trust Relationships" section govern exactly this shape of question: "the Identity Service's mechanism for **delegation** — the Principal on whose behalf another Principal acts," explicitly naming `INTERNAL_AGENT` as a Principal type whose `owner` is "the `USER` (or `SYSTEM`) Principal it was created to act on behalf of." This is the one candidate this audit takes most seriously as a possible "already exists — stop" answer, since it is genuinely governed, not ad hoc.

**Rigorously tested, and found insufficient — for a precise, structural reason, not a stylistic one:**

- Delegation answers "who backs whose authority" — an accountability chain (`owner` is, per `IdentityService.md`, "metadata for accountability and cascading lifecycle decisions... not a substitution mechanism"; "delegation is single-level... an owned Principal's own actions are attributed to itself"). It does not answer, and was never designed to answer, "on behalf of which internal, already-trusted *purpose* is this specific act being proposed, while the genuinely accountable principal is separately, correctly, still propagated."
- **`ExecutionRequest.principalId` is a single field.** Errata 004 §§2–4, §11 already require every Memory Core operation to carry an explicit, real, non-ambient `requestingPrincipalId` — `EvidenceIntelligenceInputResolver` already, correctly, does this by propagating the real analysis-request principal. Registering each internal consumer as its own `INTERNAL_AGENT` Principal and using *that* identity as `ExecutionRequest.principalId` (to gain Principal-keyed rule differentiation, mirroring the ad hoc `system.*` pattern this section's predecessor already found wanting) would necessarily **displace** the real principal on that same field — regressing exactly the accountability guarantee Errata 004 already, deliberately established, for whichever consumer adopts it. There is no way to populate one field with both the real, accountable principal *and* a distinct internal-consumer marker at once.
- This tension is not hypothetical: it is the precise, observed reason `DefaultKnowledgeCandidateEvaluator` (fixed system identity) and `EvidenceIntelligenceInputResolver` (real principal propagation) already diverge in practice — each has, independently, chosen to satisfy one of the two needs (subsystem distinction; accountable-principal propagation) at the expense of the other, because `Principal`, including via Delegation, has no field capable of satisfying both simultaneously.

**Conclusion: Delegation is real, governed, and answers a genuinely different question. It does not supply the missing distinction.**

### 4e. Phase 1 conclusion

**No existing, governed concept represents execution context, consumer identity, subsystem identity, capability identity, purpose identity, or authorization context.** The two closest candidates (the ad hoc `system.*` convention and Principal Delegation) were each taken seriously, traced to primary source, and found insufficient for structural, not stylistic, reasons. This document proceeds to Phase 2/3, and does not invent a second concept alongside an existing one, since none exists.

---

## 5. Architectural Deficiency (Phase 2)

**Why principal identity alone is insufficient.** Principal answers "who is accountable for this act" — a real actor with its own lifecycle (`Created → Active → Suspended → Revoked → Archived`), subject to genuine consequences (suspension blocks every future request; revocation cascades). `ExecutionRequest.principalId` is a single field. When the real, accountable principal is — correctly, per Errata 004 — propagated unchanged through every internal consumer that acts on a user's behalf, that field is exhausted; it cannot *additionally* carry which internal, already-trusted purpose is making the request without either inventing a second field (which is exactly this Programme's own subject) or displacing the real principal (which regresses accountability). Section 4c/4d's own evidence shows this is not a theoretical tension: it is the reason two existing consumers of the identical boundary already disagree on how to use the one field they have.

**Why action identity alone is insufficient.** The Action Vocabulary is deliberately caller-agnostic and reusable by design (`action-mapping.md`: a "Planner-owned lookup table," deterministic, "closed at evaluation time"). Even the finer-grained verb phrase (the Adopted Collision Clarification's own mechanism) is a property of the *act's shape*, not of who or why it is being requested. Using it to also encode consumer identity would require either inventing new action names purely to distinguish callers (forbidden: "do not silently invent action names if governance already fixes them," and a direct violation of the Vocabulary's own closed, deterministic, act-shaped design) or accepting that two consumers proposing the *same* act — as `memory.retrieve` genuinely is, for both Evidence Intelligence and Knowledge Submission — can never be told apart this way. Confirmed directly: neither consumer has, or should invent, a different verb phrase for what is structurally the identical Memory Core read.

**Why resource identity alone is insufficient.** `ResourceType`/`ResourceId` answers "what is being acted upon." Every existing fixed boundary resource (`KNOWLEDGE_RETRIEVAL_RESOURCE_ID`, `KNOWLEDGE_SUBMISSION_RESOURCE_ID`, etc.) is, by design, one resource shared by every legitimate caller of that boundary — it identifies the *target class*, not the *requester*. Minting a distinct Resource per consumer would repeat the already-rejected per-caller Resource Registry proliferation pattern (Memory Retrieval Contract Design, Candidate A) in a new guise, and would misuse Chapter 8's own "authoritative catalogue of every protected object" for a purpose — identifying who is asking — it was never designed for.

**Why authorization cannot distinguish legitimate subsystem intent.** None of the three existing, governed dimensions — Principal (who is accountable), Action (what kind of act), Resource (what is acted upon) — is designed to answer a fourth, genuinely distinct question: on behalf of which internal, already-trusted *purpose* is this act being proposed. All three are deliberately caller-agnostic, by design, for good reason (to prevent exactly the proliferation and gaming risk noted above). Their caller-agnosticism is not a defect to route around; it means a fourth, dedicated, honestly-named axis — one this Trust Framework has never named — is genuinely missing, not merely under-used.

**Why this is not merely a Memory Core issue.** The same shape of problem recurs wherever two or more internal, independently-governed subsystems (a) each faithfully propagate whatever principal is genuinely accountable for a given request, per Errata 004's own general no-ambient-identity discipline, and (b) share the same underlying resource/action-vocabulary pair because they perform structurally the same kind of act against the same kind of protected object. The confirmed-live instance of this is Evidence Intelligence and Knowledge Submission (Section 15) — two live, independently-governed Programmes sharing one decorator instance and one verb phrase today. `DefaultKnowledgeCandidateEvaluator` and `DefaultKnowledgeRevisionEvaluator` — two distinct consumers *within Knowledge Memory itself*, unrelated to Evidence Intelligence — additionally show the identical `system.knowledge-memory` identity already being reused for a second, genuinely different act (Section 4c), though `DefaultKnowledgeRevisionEvaluator` is presently dormant, not yet composed into the live runtime; this is latent evidence the ad hoc pattern recurs by construction, not a second confirmed live incident. Together, the confirmed-live pairing and the dormant-but-already-drifting pairing support the same conclusion: this is a structural property of the architecture, not an artifact of one decorator's own specific shape. The Programme's own named future consumers (Conversational Retrieval, Memory Maintenance, World Model retrieval, and any future Memory Core consumer) would each, if built to the same already-established principal-propagation discipline, recreate the identical collision.

---

## 6. Critical Constitutional Question

**"What additional governed information, if any, must accompany a permission request so the Trust Framework can distinguish constitutionally different consumers performing the same action on the same resource?"**

**Answer:** an explicit, disclosed, closed-vocabulary dimension — orthogonal to Principal, Action, and Resource — naming the governed *purpose* under which an act is proposed, distinct from who is accountable for it, what kind of act it is, and what it targets. It must never substitute for Principal (accountable-principal propagation remains mandatory, unweakened), must be drawn from a registered, Trust-Framework-recognised vocabulary (never free text, never an ambient or inferred signal), and must be evaluated by the same, single `PermissionEngine`/`DefaultPermissionPolicy` — never a second authority. Sections 8–9 develop what this should be called and shaped like; this document does not yet claim to have settled the exact representation mechanism (Section 12, Section 15).

---

## 7. Constitutional Requirements

Any lawful model must preserve, without exception:

- **Sole authority.** Evaluated by the one existing `PermissionEngine`/`DefaultPermissionPolicy` — never a second evaluator (Chapter 10 §5, §9).
- **Fail-closed.** Absence of a required, governed value denies; an unrecognised value denies (`PermissionPolicy.md` §4, §7's own "unknown → DENIED" defaults, extended by analogy).
- **No ambient authority.** Explicit only — never inferred from call stack, thread-local, or implicit context.
- **Principal semantics unweakened.** The real, accountable principal continues to be propagated exactly as Errata 004 §§2–4, §11 already require; this new dimension supplements, never substitutes for, Principal.
- **No caller-specific exceptions.** The new dimension must be a governed, closed, purpose-shaped classification — never a raw "which Kotlin class called this" identity, and never keyed to check "if X then allow."
- **Deterministic, auditable.** Fixed for a given caller shape, carried through to the decision trail exactly as `principalId`/`proposedActions`/`targetResources` already are.
- **General, not Memory-Core-specific.** Must be expressible for any future Trust-Framework-gated consumer, not a special mechanism wired only to `PermissionFilteredMemoryRetrieval`.

---

## 8. Candidate Models

### Candidate 1 — Consumer/Subsystem Identity (raw)

A new field naming *which internal component* (e.g. `"knowledge-memory.candidate-evaluator"`, `"evidence-intelligence.input-resolver"`) is making the request, closely mirroring class or module identity.

- **Constitutional fit:** solves the immediate problem mechanically, but risks the precise failure this Contract Design's own Independent Constitutional Review is later asked to test for: naming an *implementation* identity (a class, a component) inside a *constitutional* artifact. Governance should describe durable, purpose-level distinctions, not code structure that may be refactored freely.
- **Fail-closed:** achievable (unrecognised consumer → deny), but a raw component-name vocabulary would need constant maintenance as code is refactored, risking either staleness or a temptation to keep it loosely typed (a free string), which would reopen the "no caller-specific exceptions" risk in a different guise.
- **Interaction with Principal:** additive, not substitutive — compatible if implemented correctly, but easy to implement *incorrectly* as principal-substitution (Section 4c/4d's own already-observed failure mode).
- **Ambient-authority risk:** low, if explicit; but a component-identity vocabulary invites derivation from call-site convenience (e.g. auto-populated from a class name) rather than deliberate declaration — a real implementation-discipline risk, not a design-level flaw, but one worth naming.
- **Caller-specific-exception risk:** highest of the candidates surveyed — "if consumer == KnowledgeSubmission" is a hair's breadth from "if caller == KnowledgeSubmission," the exact pattern this whole governance effort forbids, differing only in *where* the identity is carried, not in *what kind of decision* it enables.
- **Migration:** requires naming, once, every current and future Trust-Framework-facing internal consumer — open-ended and never fully enumerable in advance.

### Candidate 2 — Authorization Context (broad)

A richer context object carrying multiple related facts (which subsystem, under what constraint, for what session) rather than a single identifier.

- **Constitutional fit:** the breadth is itself the risk — a general-purpose "context bag" invites exactly the kind of ungoverned, unverifiable, catch-all field `ExecutionRequest.metadata` already is today (deliberately unread by `DefaultPermissionPolicy` for this reason). Anything broad enough to be called "context" is broad enough to smuggle unverifiable claims into a decision the Trust Framework cannot honestly check.
- **Fail-closed / ambient authority:** the primary danger — a multi-field, loosely-scoped context object is difficult to fail closed on uniformly, and each additional field is a fresh opportunity for something ambient to enter.
- **Verdict:** not recommended as a broad object; a narrow, single-purpose value (Candidate 4) captures the legitimate need without the risk surface.

### Candidate 3 — Capability Identity

Frame the missing dimension as "which governed capability is being exercised," reusing the Tool Registry's own existing "capability" vocabulary (`ToolDescriptor.supportedActions`/`supportedResourceTypes`).

- **Constitutional fit:** poor — "capability," in this codebase, is already a specific, governed concept belonging to the Tool Registry/Module Framework, concerned with *external, Plugin-provided* invocable actions (Chapter 15 territory). Reusing the name for an *internal*, core-system consumer-distinction purpose would conflate two genuinely different Trust Framework concepts sharing only a word, inviting exactly the confusion a careful reader of `ModulePermissionRequirement`'s own KDoc (Section 4b, above) would immediately notice.
- **Verdict:** rejected — not because the underlying need differs, but because this name is already spoken for by a different, incompatible concept.

### Candidate 4 — Purpose-Bound Authorization ("Authorization Purpose")

A new, closed, Trust-Framework-governed vocabulary of *purposes* (analogous in kind, though orthogonal in axis, to the Action Vocabulary) — e.g. `"knowledge-memory.candidate-evaluation"`, `"evidence-intelligence.input-resolution"` — declared once per legitimate internal act-shape, registered the same disciplined way `ActionVocabularyEntry`s already are, and consulted by `DefaultPermissionPolicy` as an additional, optional rule-matching dimension alongside `(PermissionAction, ResourceType)` and the already-Adopted verb-phrase discriminator.

- **Constitutional fit:** strongest of the candidates surveyed. It asks the constitutionally correct question — *for what governed purpose* is this act being proposed — rather than *which code* is proposing it, keeping the abstraction at the same altitude Action Vocabulary and Resource Registry already occupy (a closed, registered, deterministic classification), not at the altitude of implementation structure.
- **Fail-closed:** an unregistered or absent purpose value denies, mirroring `PermissionPolicy.md` §7's own existing "unknown → DENIED" family of defaults exactly.
- **Interaction with Principal:** strictly additive — never substitutes for or reads from `principalId`; a request still carries its own real, accountable principal, unchanged.
- **Interaction with Action Vocabulary:** parallel, not overlapping — Action Vocabulary continues to answer "what kind of act"; Authorization Purpose answers "for what governed reason," a genuinely different axis, resolved independently.
- **Interaction with Resource Registry:** untouched — no new Resource, no new `ResourceType`.
- **Interaction with Permission Policy:** a natural extension of the same mechanism the Adopted Collision Clarification already introduced (an optional, more specific matching key alongside the coarse `(action, resourceType)` pair) — not a second policy model.
- **Interaction with audit:** additive — carried through to the decision trail exactly as every other `ExecutionRequest` field already is.
- **Interaction with provenance:** none — orthogonal; provenance concerns the record's own history, not the request that reads or writes it.
- **Interaction with future subsystems:** general by construction — any future Trust-Framework-facing consumer registers its own purpose value, the same disciplined way any new action or resource is registered today.
- **Ambient-authority risk:** low, provided the value is always explicit and drawn from a closed, registered vocabulary — never inferred, never free text.
- **Caller-specific-exception risk:** low — the discriminator is a governed *purpose classification*, evaluated identically regardless of which specific caller happens to declare it, exactly mirroring how the Action Vocabulary's own verb phrases are evaluated identically regardless of caller.
- **Migration implications:** the most significant open question (Section 13) — whether this is representable without amending `ExecutionRequest`'s own frozen schema (ADR-017, ADR-018) is not resolved by this document.

### Candidate 5 — Execution Context (extend `RequestOrigin`)

Add new `RequestOrigin` values, or a parallel enum, to distinguish internal Trust-Framework-facing subsystems the way `RequestOrigin` already distinguishes external input channels.

- **Constitutional fit:** poor, for the same category-conflation reason as Candidate 3. `RequestOrigin` is already a governed, working concept answering a specific, different question (external trigger channel). Repurposing or extending it to also answer "which internal subsystem" would blur a currently-clean distinction and risk the same enum value needing to mean two unrelated things depending on context.
- **Verdict:** rejected — the right axis, wrong host.

---

## 9. Selected Model

**The general shape is selected; the exact representation mechanism is not.** Candidate 4, **Authorization Purpose** — a new, closed, Trust-Framework-governed vocabulary naming the governed purpose under which an act is proposed, additive to and never substitutive for Principal, evaluated by the same single `DefaultPermissionPolicy` as an extension of the mechanism the Adopted Policy Rule Collision Clarification already introduced — is the best-supported candidate on the evidence gathered here, for the reasons in Section 8.

**What is not decided:** whether this is represented as a new `ExecutionRequest` field (requiring an ADR-017/ADR-018-tier schema amendment, `ExecutionRequest.schema.json`, and `docs/specifications/volume-01-core-contracts/ExecutionRequest.md`, touching every existing caller in the system), as a parameter carried alongside but outside `ExecutionRequest` (a `PermissionEngine.evaluate` signature change, equally significant), or as some other, narrower mechanism this document has not anticipated. This is deliberately not resolved here (Section 13), consistent with this task's own explicit permission not to force a decision where the evidence does not yet support one, and mirroring the same "select the shape, defer the exact representation" pattern already used twice in the Memory Retrieval governance chain this Programme grew out of.

---

## 10. Rejected Alternatives

- **Candidate 1 (raw Consumer/Subsystem Identity)** — rejected as the *primary* mechanism: highest caller-specific-exception risk of any candidate surveyed, and the closest in shape to naming implementation structure inside governance.
- **Candidate 2 (broad Authorization Context object)** — rejected: breadth itself is the risk; a narrow, single-purpose value satisfies the actual need without a general-purpose, hard-to-fail-closed-on context bag.
- **Candidate 3 (Capability Identity)** — rejected: the name is already governed, and means something different and incompatible (Tool Registry/Module Framework's own external-capability concept).
- **Candidate 5 (extend `RequestOrigin`)** — rejected: conflates two currently-clean, differently-scoped concepts (external trigger channel vs. internal purpose).
- **Registering each internal consumer as its own `INTERNAL_AGENT` Principal (a Delegation-based alternative)** — considered at length in Section 4d and rejected there: `ExecutionRequest.principalId` is a single field, and this approach would necessarily displace real, accountable-principal propagation for whichever consumer adopted it, regressing Errata 004's own already-accepted no-ambient-identity guarantee.
- **Continuing, or formalising, the ad hoc `system.*` Principal convention** — rejected (Section 4c): already shown inconsistent, insufficiently granular (two distinct Knowledge Memory consumers already share one identity), and independently confirmed non-functional for at least one consumer today.

---

## 11. Relationship to Principal

Strictly additive. Authorization Purpose never reads, substitutes for, or is derived from `principalId`. The real, accountable principal continues to be resolved via `IdentityService` exactly as today (Section 3, steps 2); Suspended/Revoked/Archived/Created status continues to short-circuit before any policy — including any Purpose-aware rule — is ever consulted. A request lacking a required Purpose value, where one is required, fails independently of, and in addition to, principal-status evaluation, never in place of it.

## 12. Relationship to Action Vocabulary

Parallel, not overlapping. Action Vocabulary continues to answer "what kind of act, resolving to which `(PermissionAction, ResourceType)` pair(s)," unchanged, per `action-mapping.md`'s own Transformation Rules. Authorization Purpose answers an orthogonal question — "for what governed reason" — resolved independently and consulted, if present, as an additional, more specific matching dimension, mirroring exactly how the Adopted Collision Clarification's own verb-phrase discriminator already extends `DefaultPermissionPolicy`'s matching without altering `ActionMapper`'s own resolution step.

## 13. Relationship to Resource Registry

Untouched. No new `ResourceType` value, no new Resource, no change to `ResourceRegistry.resolve`/`register`/`update`. Chapter 8's own "authoritative catalogue of every protected object" continues to describe *what is acted upon*; Authorization Purpose never describes a protected object and is never registered there.

## 14. Relationship to Permission Engine

`PermissionEngine`'s own public interface (`evaluate(request): PermissionDecision`, `explain(decisionId): PermissionExplanation`) is not required to change for the *conceptual* model selected here — the open question (Section 9, Section 15) is whether the *carrier* of the Purpose value can live inside the existing `ExecutionRequest` shape or requires a companion parameter. Either way, `PermissionEngine` remains the sole authority (Chapter 10 §5); nothing here constructs a second evaluator, a second policy instance, or a second decision type.

## 15. Relationship to Evidence Intelligence

Directly motivating. `EvidenceIntelligenceInputResolver`'s own currently-correct principal-propagation discipline (Section 4d) would, under this model, remain entirely unchanged — it would additionally declare its own governed Authorization Purpose (e.g. `"evidence-intelligence.input-resolution"`) on the same request that already, correctly, carries the real accountable principal, resolving the Resolution Derivation Mechanism Clarification's own Section 7 finding (Knowledge Submission and Evidence Intelligence sharing an indistinguishable request shape) without requiring Evidence Intelligence to give up real-principal propagation, and without requiring Knowledge Submission to adopt a fixed system identity in its place.

## 16. Relationship to Knowledge Memory

Motivating, independent of Evidence Intelligence, though at a different evidentiary weight than Section 15: `DefaultKnowledgeCandidateEvaluator` (live) and `DefaultKnowledgeRevisionEvaluator` (confirmed dormant — not composed into `ParkerRuntime.kt`) already share one `system.knowledge-memory` identity constant despite being genuinely distinct acts (Section 4c). This is latent evidence of the same ad hoc pattern's own organic reuse, not a second currently-live collision — but it shows the risk is not confined to the Evidence-Intelligence/Knowledge-Submission pairing, and would become a live collision the moment `DefaultKnowledgeRevisionEvaluator` is composed into the runtime under the identity convention it already, presently declares. Under this model, each would declare its own Authorization Purpose, resolving both the confirmed-live and the latent collision alike, without needing either evaluator to adopt, or continue relying on, an ungoverned `system.*` identity substitution.

## 17. Relationship to Memory Core

None directly — Memory Core Scope Lock §6's own "Memory Core never evaluates permissions" boundary is untouched; Authorization Purpose, like every other permission-evaluation concept, lives entirely within Trust Framework's own components, evaluated before Memory Core is ever invoked, exactly as today.

## 18. Relationship to Future Subsystems

General by construction. Future Conversational Retrieval, Memory Maintenance, World Model retrieval, or any other future Memory Core (or other protected-object) consumer would each register their own Authorization Purpose value, the same disciplined way a new action name or Resource is registered today — no bespoke mechanism per subsystem, no caller-specific rule anywhere in the design.

---

## 19. Migration Considerations

Not resolved by this document, and disclosed as the central open question a future, narrower design pass must address before any Scope Lock:

- **Carrier mechanism.** Whether Authorization Purpose becomes a new `ExecutionRequest` field (an ADR-017/ADR-018-tier schema change touching `ExecutionRequest.schema.json`, `docs/specifications/volume-01-core-contracts/ExecutionRequest.md`, and — in principle — every existing caller in the system, whether or not each caller ever populates a non-default value) or a mechanism that avoids touching `ExecutionRequest`'s own frozen shape at all. This is a materially large decision this narrow Contract Design does not make.
- **Vocabulary governance.** Who registers a new Authorization Purpose value, under what review discipline, and whether this mirrors `ActionVocabulary`'s own registration model exactly or requires its own, dedicated registry.
- **Backward compatibility.** Whether an absent Purpose value must always deny (strict fail-closed) or may default to today's caller-agnostic behaviour for existing, unmigrated callers during a transition — a policy-content question, not decided here.
- **Retrofitting existing consumers.** `DefaultKnowledgeCandidateEvaluator`, `DefaultKnowledgeRevisionEvaluator`, `EvidenceIntelligenceInputResolver`, and every other existing Trust-Framework-facing consumer would each need a deliberate, reviewed Purpose assignment — not a mechanical, automated migration.

---

## 20. Risks

- **Risk: Authorization Purpose is implemented as, or drifts into, raw component/class identity** (Candidate 1's own rejected shape), reintroducing the caller-specific-exception risk this document exists to avoid. **Mitigation:** any future Scope Lock must judge candidate Purpose values against "does this name a governed reason, or does it name a piece of code" directly, not merely against whether it compiles.
- **Risk: the carrier mechanism (Section 19) is resolved by quietly weakening `ExecutionRequest`'s own immutability/canonical-schema guarantees** (ADR-017, ADR-018) rather than through a deliberate, reviewed amendment. **Mitigation:** any future pass touching `ExecutionRequest`'s own shape must be its own, explicit Contract Design tier decision, not an incidental side effect of adding this field.
- **Risk: the vocabulary becomes a second, informally-governed classification system, drifting away from the same registration discipline `ActionVocabulary` already enforces** (uniqueness, deterministic content, no silent overwrite). **Mitigation:** whichever registration mechanism is eventually selected should be held to the identical discipline `InMemoryActionVocabulary.register` already demonstrates, not a looser standard.
- **Risk: this document's own conclusion is read as authorising immediate retrofitting of `system.knowledge-memory` or any other existing ad hoc identity, without the Scope Lock this document does not authorise.** **Mitigation:** Section 19's own migration considerations are explicitly unresolved; nothing here licenses touching `src/` in response to this document alone.

---

## 21. Recommendation

A narrower, dedicated design pass — scoped specifically to Section 19's own carrier-mechanism question — should be undertaken before any Scope Lock for Authorization Purpose is considered, mirroring the same Planning Review → Boundary Review → Contract Design discipline already applied throughout this governance chain. That pass should resolve whether Authorization Purpose can be represented without amending `ExecutionRequest`'s own frozen schema, and if not, what the narrowest lawful amendment looks like, before any Kotlin is written. Once that question is resolved, this document's own conceptual selection (Section 9) can be carried into a Scope Lock that also, separately, retrofits `DefaultKnowledgeCandidateEvaluator`, `DefaultKnowledgeRevisionEvaluator`, and `EvidenceIntelligenceInputResolver` with real Purpose values — the concrete step that would let Gap #54's own remaining prerequisite (the Resolution Derivation Mechanism Clarification's own Section 7 finding) finally be closed without weakening Evidence Intelligence's own fail-closed guarantee or Knowledge Submission's own accountable-principal propagation.

The Parker Conversational Memory Bridge's own second unit (conversational retrieval) should continue to wait, as already established, until Gap #54 is fully, safely resolved — which, on this document's own evidence, now includes Authorization Purpose's own eventual adoption, not only the mechanism-level work the two prior Clarifications already completed.

---

## 22. Independent Constitutional Review Self-Check

Performed by the author before requesting external review — a genuine external Independent Constitutional Review follows as a separate document.

- **Second permission model accidentally invented?** No — Authorization Purpose is consulted by the same, single `DefaultPermissionPolicy`; no second `PermissionEngine` or evaluator is proposed.
- **Ambient authority appeared?** No — explicit only, never inferred; the model requires the value be declared, not derived.
- **Fail-closed behaviour preserved?** Yes — an absent or unregistered Purpose value denies, mirroring `PermissionPolicy.md` §7's own existing defaults.
- **Subsystem identity leaks implementation concepts?** Addressed directly by rejecting Candidate 1 in favour of Candidate 4 — a governed purpose classification, not a class or component name — though Section 20's own first risk discloses this remains a live implementation-discipline risk for any future pass to guard against.
- **Principal semantics weakened?** No — Section 11 traces this explicitly; Section 4d's own rejected Delegation-based alternative was rejected precisely to avoid this.
- **Audit and provenance sufficient?** Audit: additive, unchanged mechanism (Section 11's own carry-through). Provenance: orthogonal, untouched (Section 8, Candidate 4).
- **Scales beyond Memory Core?** Yes, by construction (Section 18) — no mechanism here is wired specifically to `PermissionFilteredMemoryRetrieval` or any single decorator.
- **Overly specific to Gap #54?** The motivating evidence is Memory-Core-adjacent (Sections 15–16), but Section 5's own "why this is not merely a Memory Core issue" argument, and the Knowledge-Memory-internal `system.knowledge-memory` collision (independent of Evidence Intelligence entirely), support a general conclusion, not a narrow one.

```
TRUST FRAMEWORK AUTHORIZATION CONTEXT (CONSUMER IDENTITY) CONTRACT DESIGN — DRAFT COMPLETE, PENDING INDEPENDENT CONSTITUTIONAL REVIEW
```
