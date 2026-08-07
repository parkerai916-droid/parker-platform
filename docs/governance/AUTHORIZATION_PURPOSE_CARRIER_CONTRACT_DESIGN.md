**Status:** Governance and architecture only. No Kotlin is implemented, proposed as a diff, or changed by this document. Neither `src/` nor `tests/` is touched. This document does not amend `docs/architecture/parker-constitution.md`, `docs/architecture/10-permission-engine.md` ("Chapter 10"), `docs/architecture/08-resource-registry.md` ("Chapter 8"), `docs/architecture/IdentityService.md`, `docs/adr/ADR-017-execution-request-is-canonical.md`, `docs/adr/ADR-018-immutable-execution-requests.md`, `docs/governance/TRUST_FRAMEWORK_AUTHORIZATION_CONTEXT_CONTRACT_DESIGN.md`, `docs/architecture/TRUST_FRAMEWORK_AUTHORIZATION_PURPOSE_PROGRAMME.md`, `docs/governance/TRUST_FRAMEWORK_MEMORY_RETRIEVAL_CONTRACT_DESIGN.md`, either Adopted Memory Retrieval Clarification, or any other frozen or draft governance document — it reads them, cites them, and reasons from them. It does not authorise a Scope Lock, an Implementation Plan, or any implementation work. Nothing is staged, committed, or pushed.

# Trust Framework — Authorization Purpose — Unit 1 — Carrier / Representation Contract Design

Programme: **Trust Framework Authorization Purpose Programme, Unit 1 (Carrier / Representation Design).**

---

## 1. Governing Context

Read fresh for this document, in full or in every relevant section: `docs/governance/TRUST_FRAMEWORK_AUTHORIZATION_CONTEXT_CONTRACT_DESIGN.md` (Adopted — the sole conceptual authority for *what* Authorization Purpose is; not reopened or re-derived here); `docs/architecture/TRUST_FRAMEWORK_AUTHORIZATION_PURPOSE_PROGRAMME.md` (accepted, architecture tier — names this Unit as its own first step); `docs/architecture/10-permission-engine.md` (Chapter 10, §2–5, §9, §10 read exactly); `docs/adr/ADR-017-execution-request-is-canonical.md`; `docs/adr/ADR-018-immutable-execution-requests.md`; `docs/specifications/volume-03-core-interfaces/PermissionPolicy.md` §3, §9; `docs/architecture/action-mapping.md`; `docs/architecture/IdentityService.md` ("Trust Relationships," "Integration with Permission Engine"); `docs/architecture/08-resource-registry.md`; `docs/governance/TRUST_FRAMEWORK_MEMORY_RETRIEVAL_CONTRACT_DESIGN.md` (as consolidated) and both Adopted Clarifications. Production code and schemas read fresh, in full or in every relevant part: `src/contracts/ExecutionRequest.kt`, `src/contracts/Principal.kt`, `src/contracts/Permission.kt`, `src/contracts/ActionMapping.kt`, `src/interfaces/PermissionEngine.kt`, `src/runtime/DefaultPermissionEngine.kt`, `src/runtime/DefaultPermissionPolicy.kt`, `src/runtime/ActionMapper.kt`, `src/runtime/DefaultExecutionPipeline.kt`, `src/runtime/InMemoryAgentRuntime.kt`, `src/composition/InteractiveConsole.kt`, `docs/schemas/ExecutionRequest.schema.json`; grepped across `src/` for every genuinely concurrent (`coroutineScope`/`async`/`launch`) or nested `PermissionEngine.evaluate`/`ExecutionPipeline.submit` call site.

---

## 2. Purpose

Determine the constitutional carrier of Authorization Purpose: not how it is implemented, but what kind of governed object or concept lawfully carries it through permission evaluation, from a caller's own declaration through to `DefaultPermissionPolicy`'s own matching step. This is Unit 1 of the Authorization Purpose Programme; it does not re-derive why Authorization Purpose is needed (settled, Adopted) and does not authorise building it.

---

## 3. Existing Architecture — The Permission Path, Traced Fresh

```
Caller
    |
    v
ExecutionRequest            (src/contracts/ExecutionRequest.kt — data class, val-only, no mutator)
    |
    v
PermissionEngine.evaluate   (src/interfaces/PermissionEngine.kt — suspend fun evaluate(request: ExecutionRequest): PermissionDecision)
    |
    v
DefaultPermissionEngine     (resolves principalId via IdentityService first; short-circuits DENIED for non-Active status)
    |
    v
DefaultPermissionPolicy.evaluate  (resolves resourceTypes from targetResources; calls ActionMapper)
    |
    v
ActionMapper.mapOne         (matches proposedActions against ActionVocabulary + resolved resourceTypes)
    |
    v
PermissionDecision          (src/contracts/Permission.kt — data class: decisionId, principalId, resourceId, action, decision, level, timestamp)
```

**Object-by-object classification, re-derived directly from source, not assumed:**

| Object | Mutability | Lifecycle | Governance tier |
|---|---|---|---|
| `ExecutionRequest` | Immutable by construction (`val`-only data class); ADR-018: "become immutable after validation... changes require creation of a new `ExecutionRequest`" | One per proposed act; freshly constructed by the caller for every request, never reused or mutated | Canonical (ADR-017: "Any proposed work... MUST become an `ExecutionRequest`"; "No subsystem may invent a parallel execution request type") |
| `PermissionEngine`/`DefaultPermissionEngine` | Stateless per call (constructor-injected dependencies only) | One shared instance for the runtime's lifetime (`ParkerRuntime.kt` constructs exactly one) | Sole authority (Chapter 10 §5) |
| `DefaultPermissionPolicy` | Stateless per call; `rules: List<PermissionPolicyRule>` fixed at construction | Same lifetime as `PermissionEngine` | Implementation of `PermissionPolicy.md`; not itself frozen governance, but the sole implementation in production |
| `ActionMapper`/`ActionVocabulary` | `InMemoryActionVocabulary` is runtime-mutable only via `register` (append-only, reject-on-conflict); `ActionMapper` itself is stateless | Vocabulary entries registered once, at composition time, in `ParkerRuntime.kt` | Governed (`action-mapping.md`'s own "Planner-owned lookup table," deterministic, closed at evaluation time) |
| `Principal`/`IdentityService` | `Principal` values are stored, status-mutable only via `updateStatus`; identity itself is not re-mutated after registration | Registered once per Principal; resolved fresh on every `evaluate` call | Governed (`IdentityService.md`, Chapter 41) |
| `ResourceRegistry`/`Resource` | `InMemoryResourceRegistry` has no durability (confirmed in the Memory Retrieval governance chain); resources are registered at composition time | Registered once per Resource, resolved fresh per request | Governed (Chapter 8) |
| `PermissionDecision` | Immutable data class, freshly constructed per `evaluate` call | One per evaluation, never reused | Not itself independently governed; a return value shape |

**ADR-017/ADR-018, read exactly, not paraphrased:** ADR-017's own Decision: "Any proposed work that may cause execution, resource access, state mutation, or external side effects MUST become an `ExecutionRequest`." Its own Consequence: "No subsystem may invent a parallel execution request type." ADR-018's own Decision: "ExecutionRequests become immutable after validation." Its own Consequence: "Changes require creation of a new `ExecutionRequest` linked by correlation ID." **These are two distinct constraints**: ADR-017 governs whether a *second, competing* top-level request type may exist (it may not); ADR-018 governs *instance-level* mutation after validation (already satisfied structurally by the `val`-only data class). Neither, on its own text, forecloses `ExecutionRequest`'s own *schema* gaining an additional field over successive, deliberate amendments — the class already carries four optional fields (`sessionId`, `riskEstimate`, `expiresAt`, `metadata`) added incrementally to the same canonical type, not as competing request types.

**Chapter 10 §10 ("Extensibility"), read exactly:** "`ExecutionRequest` remains today's only implemented request type, per Section 3, but this chapter's own guarantees do not depend on that remaining permanently true." This is a direct, already-accepted disclosure that Chapter 10's own sole-authority guarantee is independent of `ExecutionRequest`'s own exact shape — it is bound to there being **one** `PermissionEngine`, not to any specific carrier shape remaining frozen forever.

**Nested and asynchronous evaluation, checked directly, not assumed:** grepped every `coroutineScope`/`async {`/`launch {` in `src/` — the only match (`InteractiveConsole.kt`) is a UI spinner, unrelated to permission evaluation. No genuinely concurrent or recursive `PermissionEngine.evaluate` call exists anywhere in the codebase. What does exist is **sequential chaining**: `InMemoryAgentRuntime` evaluates a run-initiation `ExecutionRequest`, and — later, as a separate, subsequent step, never within the same call stack — evaluates a second, distinct `ExecutionRequest` for a proposed action, via `ExecutionPipeline.submit`. Each is its own, independently-constructed, freshly-evaluated request; neither inherits any value from the other implicitly. **This confirms the operative propagation discipline for Authorization Purpose is the same one `principalId` already uses**: explicit, fresh, per-request declaration — never ambient inheritance across a chain of related but distinct requests.

---

## 4. Constitutional Requirements

Restated from this task's own list, each tied to the primary source that already governs it — none invented here:

- **Single Permission Engine, single Permission Policy.** Chapter 10 §5, §9: "may never construct, implement, or substitute a second authority." Any carrier must be evaluated by the one, existing `PermissionEngine`/`DefaultPermissionPolicy`.
- **Fail-closed behaviour.** `PermissionPolicy.md` §4, §7: "DENIED is the default when no rule matches"; unknown action/resource → DENIED. A carrier lacking, or carrying an unregistered, Authorization Purpose value must deny by the same default, never by a new, separately-invented failure mode.
- **No ambient authority.** Errata 004 §§2–4, §11 (already binding on Memory Core callers; treated here as the general Trust Framework norm it already exemplifies): explicit only, never inferred from call stack, thread-local, or implicit context.
- **No caller-specific exceptions.** Authorization Context Contract Design §7: "never keyed to check 'if X then allow.'" The carrier must be equally available, in the same shape, to every caller — never a mechanism only some consumers can reach.
- **No forged Authorization Purpose.** Addressed directly in Section 13, below — this is not a requirement any carrier shape can satisfy through a runtime verification mechanism; it is satisfied the same way `principalId`'s own integrity already is (Section 13).
- **Explicit provenance; auditability; deterministic evaluation.** `ExecutionRequest`'s own existing `requestId`/`correlationId`/`createdAt`/`principalId` shape already satisfies this for every other field; whichever carrier is selected must inherit, not reinvent, this guarantee (Section 17).
- **Compatibility with Principal, Action Vocabulary, Resource Registry.** Authorization Context Contract Design §11–13: additive to Principal, parallel to Action Vocabulary, untouched Resource Registry. Restated, not re-derived, in Sections 10–15 below for this specific carrier question.

---

## 5. Candidate Carrier Models

### Candidate A — Extend `ExecutionRequest` with a new, optional field

A new field, e.g. shaped `authorizationPurpose: AuthorizationPurposeId?`, added to the existing canonical data class. `PermissionEngine.evaluate`'s own signature (`evaluate(request: ExecutionRequest): PermissionDecision`) is **unchanged**. The field's own value is a small, separately-defined, closed value type (mirroring `PrincipalId`/`ResourceId`'s own established `@JvmInline value class` shape) — never a raw `String`.

### Candidate B — Separate immutable carrier object, passed alongside `ExecutionRequest`

A new object (e.g. `AuthorizationContext(purpose: AuthorizationPurposeId)`), submitted as a **second parameter** to `PermissionEngine.evaluate` — `evaluate(request: ExecutionRequest, authorization: AuthorizationContext): PermissionDecision`. Requires changing `PermissionEngine`'s own public interface.

### Candidate C — Contextual wrapper around `ExecutionRequest`

A wrapping type (e.g. `AuthorizedExecutionRequest(request: ExecutionRequest, purpose: AuthorizationPurposeId)`) that every caller constructs instead of a bare `ExecutionRequest`, submitted to a `PermissionEngine.evaluate` overload or replacement accepting the wrapper.

### Candidate D — Evaluation envelope

A broader, intentionally extensible bundle of evaluation-relevant facts (of which Authorization Purpose would be only one member today), designed as a general future extension point for additional authorization dimensions not yet named.

### Candidate E — Attach Authorization Purpose to an existing governed object

Three sub-variants, each mirroring a concept already examined and rejected by the Authorization Context Contract Design for the underlying *concept* question, re-examined here specifically for the narrower *carrier* question:
- **E1 — Principal**, via `owner`/Delegation or a new `Principal` field.
- **E2 — Action Vocabulary**, by making a verb phrase's own registration imply a fixed Authorization Purpose.
- **E3 — Resource Registry**, by attaching Authorization Purpose to a Resource's own record.

---

## 6. Comparative Analysis

| Dimension | A — Extend `ExecutionRequest` | B — Separate parameter | C — Wrapper | D — Envelope | E — Existing object |
|---|---|---|---|---|---|
| **Constitutional fit** | Strong — additive to the one already-canonical type (ADR-017); no competing request type created | Moderate — does not create a competing *request* type, but does create a second, co-equal input `PermissionEngine.evaluate` must trust, outside `ExecutionRequest`'s own governed shape | Weak — every caller must change *how* it calls `evaluate`, not merely *what* it populates; closer to inventing a parallel request-construction path ADR-017 warns against | Weak — inherits Authorization Context Contract Design's own already-rejected "broad context bag" finding (Candidate 2 of that document) unless artificially narrowed to just Authorization Purpose, at which point it collapses into B | Poor — each sub-variant already, independently disqualified by the Authorization Context Contract Design's own Sections 4d/5/8, for reasons restated in Section 9 below |
| **Authority boundaries** | Unchanged — one `PermissionEngine`, one call signature | Unchanged in principle, but every existing `PermissionEngine` implementer (including test fakes) must accept a new parameter | Unchanged in principle, same implementer-touching cost as B, plus every *caller* also changes | Same as B, with the added risk that a loosely-scoped envelope invites future authors to smuggle additional, unreviewed authority-adjacent fields into it over time | Unchanged, but blurs which object owns which question (Section 9) |
| **Lifecycle** | Identical to every other `ExecutionRequest` field — one per request, freshly constructed | A new object with its own lifecycle question: created once per request (straightforward) or reused across requests (a new ambient-authority risk if reused) | Same as B | Same as B, worse if scope grows | Piggybacks on the host object's own lifecycle — mismatched for E1 (Principal's own lifecycle is per-actor, not per-request) and E3 (Resource's own lifecycle is per-target, not per-request) |
| **Ownership** | `ExecutionRequest`'s own existing owner (ADR-017/018, Volume 1 core contracts) | A new, undetermined owner — Trust Framework, but which document? | Same open question as B | Same open question as B | Whichever object's own existing governance already covers it — but that governance answers a different question (Section 9) |
| **Creation** | By the same caller already constructing the `ExecutionRequest`, at the same moment, no new call site | A second value the caller must separately construct and correctly pair with the right `ExecutionRequest` — a new correlation risk (Section 8's own "no forged Authorization Purpose" concern deepens here: two objects, not one, must agree) | Same pairing risk as B, structurally forced together (arguably safer than B on this one dimension, since the wrapper cannot exist without both) | Same as B | Depends on host object's own existing creation path, often at a different time than the request itself (e.g., Resource registration happens at composition time, long before any specific request) |
| **Propagation** | Automatic — travels with the request through every layer already reading `ExecutionRequest` | Requires every intermediate layer between caller and `PermissionEngine.evaluate` to also thread the second parameter through — more surface for accidental loss | Requires every intermediate layer to thread the wrapper instead of the bare request — the largest propagation surface of any candidate | Same as B/C | No propagation problem for E2/E3 (resolved via lookup, not carried) — but this is exactly why they are unable to express a *per-request*, caller-declared distinction (Section 9) |
| **Immutability** | Inherits `ExecutionRequest`'s own `val`-only, ADR-018 guarantee automatically | Must independently declare and enforce its own immutability — a new, separate guarantee to get right | Same as B | Same as B | Inherits the host's own immutability posture, which is a different, non-per-request guarantee (Section 9) |
| **Audit implications** | Automatic — already flows into whatever `ExecutionRequest`'s own fields already reach (Section 17) | Requires a new, explicit decision about whether/how the separate object also reaches the audit trail | Same as B | Same as B, and harder to bound as the envelope grows | Audit trail would need to separately resolve the host object's own state at the time of the request — a weaker, less direct trail than a value carried on the request itself |
| **Provenance implications** | None beyond `ExecutionRequest`'s own existing `requestId`/`correlationId` | A second object needs its own provenance story, or must borrow the request's — an open question this candidate does not answer by construction | Same as B | Same as B | Provenance already belongs to the host object's own governance (Principal identity, Resource registration) — but that provenance answers "who/what is this," not "why is this specific act being proposed," the actual question at hand (Section 9) |
| **Interaction with nested calls** | None — Section 3's own finding (no genuine nesting exists) applies identically regardless of carrier; each freshly-constructed `ExecutionRequest` states its own field explicitly | Same finding applies, but a second, separately-threaded parameter is more likely to be accidentally, silently reused across a chained call than a field embedded in a freshly-constructed value object | Same as B | Same as B | Not applicable in the same way — E2/E3 are resolved by lookup each time, so nesting is moot for them, but this is again because they cannot express the per-request distinction at all |
| **Interaction with asynchronous execution** | None — Section 3's own finding (`suspend fun`, no genuine concurrency around `evaluate`) is unaffected by a plain data-class field addition | None additionally, beyond the ordinary cost of adding a parameter to a `suspend` function signature | None additionally | None additionally | Not applicable, for the same reason as nesting |
| **Interaction with future Tools** | Any future Tool/Plugin invocation path that constructs its own `ExecutionRequest` already must populate every other required field; Authorization Purpose is one more, using the identical mechanism (Chapter 10 §10's own "does not depend on `ExecutionRequest` remaining permanently the only... type" leaves room for this to evolve further later, without requiring it to evolve now) | A future Tool must additionally learn and correctly use a second, separate parameter/type — a materially larger integration surface for every future caller, including Plugin-authored ones (`action-mapping.md`'s own "Plugin Supplied Actions" section) | Same as B, plus the wrapper itself becomes something every future Tool must construct correctly | Same as B, worsening as the envelope's own scope grows over time | Not applicable for the same structural reason as nesting/async |
| **Migration cost** | Low, structurally — additive, optional field; every existing caller that never sets it behaves exactly as today (mirrors how `sessionId`/`riskEstimate`/`expiresAt` were each added without breaking existing callers); still an ADR-017/018-tier schema decision, requiring `ExecutionRequest.schema.json` and `docs/specifications/volume-01-core-contracts/ExecutionRequest.md` to be updated in step | Moderate-to-high — every existing `PermissionEngine` implementer (including `FakePermissionEngine` and any other test fixture) must be updated to accept the new parameter, even those that will never use it | High — every existing caller must change *how* it invokes evaluation, not only *what* it populates | High, and open-ended — cost grows with whatever else the envelope eventually accretes | Low to construct, but does not actually solve the problem (Section 9) — its "low cost" is irrelevant if it does not satisfy the requirement |

---

## 7. Selected Carrier Model

**Candidate A — extend `ExecutionRequest` with one new, optional field, whose own value is a small, separate, closed, immutable value type.** This is the best-supported candidate on every dimension in Section 6 except raw novelty of concept, and it is explicitly the shape ADR-017/018 and Chapter 10 §10 already anticipate room for: `ExecutionRequest` remains the single, canonical carrier of every fact relevant to a proposed act (ADR-017), gains this fact the same way it has already, incrementally gained four other optional fields, and `PermissionEngine`'s own public interface — the actual locus of "sole authority" (Chapter 10 §5) — does not change at all.

**What this selects, precisely:** the *constitutional home* is `ExecutionRequest` itself — Authorization Purpose is a property of the proposed act, declared once, by the same caller, at the same moment, as every other fact about that act — not a co-equal, separately-threaded input requiring its own propagation discipline. **What this does not select:** the field's own exact name, the exact shape of its own value type, whether it is nullable or has a default, how the closed vocabulary is registered or validated, and whether `PermissionDecision` should also be extended to echo back the matched value (Section 17). These remain Scope-Lock/Implementation-Plan-tier questions, not decided here.

---

## 8. Rejected Alternatives

- **Candidate B (separate parameter)** — rejected: changes `PermissionEngine`'s own public interface, the one thing every candidate should avoid touching if possible (Chapter 10 §5's own sole-authority guarantee is about the *engine*, not the *request shape*, but a signature change still touches every implementer, including test fixtures, for a fact that belongs on the request itself); introduces a new object-pairing/correlation risk Candidate A does not have.
- **Candidate C (wrapper)** — rejected: the highest propagation and migration cost of any candidate; every existing and future caller must change its own call shape, not merely populate one more field.
- **Candidate D (envelope)** — rejected: the Authorization Context Contract Design's own Candidate 2 ("Authorization Context, broad") already rejected this exact shape of risk for the underlying concept; nothing about the narrower carrier question changes that reasoning, and an intentionally open-ended future extension point invites exactly the "context bag" drift `ExecutionRequest.metadata` already, deliberately, is never read by `DefaultPermissionPolicy` to avoid.
- **Candidate E1 (Principal/Delegation)** — rejected, restating the Authorization Context Contract Design's own Section 4d finding for the carrier question specifically: `Principal`'s own lifecycle (per-actor, spanning many requests) does not match Authorization Purpose's own required lifecycle (per-request, Section 3); attaching it to Principal would either require a new Principal per purpose (recreating the already-rejected ad hoc `system.*` substitution pattern) or silently conflate accountability with purpose on the one field that must stay singular.
- **Candidate E2 (Action Vocabulary)** — rejected: an action's own registration is caller-agnostic and reusable by design (`action-mapping.md`); baking a fixed Authorization Purpose into a verb phrase's own registration would require a distinct verb phrase per (action, purpose) pair — exactly the "invent new action names to distinguish callers" pattern the Authorization Context Contract Design's own Section 5 already forecloses.
- **Candidate E3 (Resource Registry)** — rejected: a Resource's own registration happens once, at composition time, describing a target class, not a per-request requester's own reason for acting — Chapter 8's own "authoritative catalogue of every protected object" describes what is acted upon, never why or on whose purpose's behalf.

---

## 9. Why the "Existing Object" Candidates Fail on the Carrier Question Specifically

Distinguished from the Authorization Context Contract Design's own, broader "does an existing *concept* already solve this" question (Adopted, not reopened): here, each existing object is tested narrowly as a *physical carrier*, and each fails for the same underlying, structural reason — **none of Principal, Action Vocabulary, or Resource Registry has a per-request lifecycle.** Principal persists across many requests; a registered action persists across every caller that ever proposes it; a registered Resource persists across every request that ever targets it. Authorization Purpose, by contrast, must be declared **freshly, per request** (Section 3's own sequential-chaining finding) — the same lifecycle `ExecutionRequest` itself already has, and no other existing object shares.

---

## 10. Relationship to `ExecutionRequest`

Direct and structural. Authorization Purpose becomes one more fact `ExecutionRequest` already exists to carry (ADR-017: "any proposed work... MUST become an `ExecutionRequest`" — the fact of *why*, constitutionally, this proposal is being made is squarely within that same canonical scope). No competing request type is created; ADR-018's own immutability guarantee is inherited automatically, not re-implemented.

## 11. Relationship to Permission Engine

None to its own public interface. `PermissionEngine.evaluate(request: ExecutionRequest): PermissionDecision` is unchanged in shape; it simply now may read one more field on the request it already receives. Chapter 10 §5's sole-authority guarantee is preserved exactly — no second evaluator, no second call path.

## 12. Relationship to Permission Policy

Direct. `DefaultPermissionPolicy`'s own resolution step — already extended once by the Adopted Policy Rule Collision Clarification to optionally match on the specific verb phrase — gains a second, optional, additional matching dimension: the Authorization Purpose value now present on the `ExecutionRequest` it already receives. This is the same "extend the single, existing resolution step" pattern already used for the verb-phrase discriminator, applied to one more field, not a new policy model.

## 13. Relationship to Principal — Including "No Forged Authorization Purpose"

**Additive, never substitutive** (restated from the Authorization Context Contract Design, unchanged). **On forgery, addressed directly, not assumed away:** no carrier candidate considered — including the selected one — introduces a cryptographic or runtime non-repudiation mechanism preventing an already-trusted, already-reviewed internal caller from mis-declaring its own Authorization Purpose, any more than any existing mechanism today prevents such a caller from mis-declaring `principalId`. This is not a gap unique to Authorization Purpose; it is the same trust boundary every `ExecutionRequest` field already relies on: the caller constructing the request is itself part of the reviewed, governed Trust-Framework-facing codebase, not untrusted external input. **The mitigation is, and can only be, the same one already in force**: only already-reviewed, Scope-Locked code may construct a request declaring a given Authorization Purpose value, exactly as only already-reviewed code may set a given `principalId` today — enforced by code review, Scope Lock discipline, and Independent Constitutional Review, not by a new runtime check this document does not, and could not honestly, invent.

**The analogy to `principalId` is not complete, and the asymmetry is disclosed rather than smoothed over**: `principalId` carries an indirect, *structural* enforcement backstop `IdentityService` already provides — a Principal that misbehaves can be moved to `Suspended`/`Revoked`, and `DefaultPermissionEngine.evaluate`'s own first step then denies every subsequent request naming it, regardless of anything else a caller declares (Section 3). **Authorization Purpose, as specified by this document, has no analogous backstop** — nothing in Sections 5–9 proposes a way to "suspend" a Purpose value or otherwise structurally block a caller that mis-declares one; a mis-declaration's only check is code review, before or after the fact, with no runtime-enforced consequence once a request is constructed. This is a genuinely weaker enforcement position than `principalId`'s own, not merely a restatement of it — disclosed here as an honest limit of every candidate this document considered, not a defect specific to the one selected. Whether a future, separate mechanism should close this gap (for example, a registration-time integrity check on which components may declare which Purpose values) is a vocabulary-governance question, not a carrier question, and is not addressed here.

## 14. Relationship to Resource Registry

Untouched, restated for the carrier question specifically: the selected model adds a field to `ExecutionRequest`; it touches no `Resource`, no `ResourceType`, and no `ResourceRegistry` method.

## 15. Relationship to Action Vocabulary

Parallel, restated: `ActionMapper`/`ActionVocabulary` continue to resolve `proposedActions` exactly as today; Authorization Purpose is read directly from `ExecutionRequest` by `DefaultPermissionPolicy`, never by `ActionMapper`, and never folded into a verb phrase's own registered meaning (Section 8, Candidate E2).

---

## 16. Lifecycle

One Authorization Purpose value per `ExecutionRequest`, declared at construction, immutable thereafter (inherited from ADR-018), never reused across a subsequent, separately-constructed request even where that later request is causally related to an earlier, approved one (Section 3's own sequential-chaining finding). No independent lifecycle of its own beyond the request that carries it — it is created and destroyed with the request, exactly like `intent` or `proposedActions`.

---

## 17. Audit and Provenance

**Stated precisely, against the actual mechanism, not assumed.** `DefaultExecutionPipeline.publishLifecycleEvent` — the only event-publishing mechanism anywhere near the permission path (`DefaultPermissionEngine`/`DefaultPermissionPolicy` publish no events at all) — constructs each `ParkerEvent` with `publisherPrincipalId = request.principalId` and `correlationId = request.correlationId` as distinguished, top-level fields, but its own `payload` carries only `mapOf("requestId" to request.requestId.value)`. **No request content field — not `proposedActions`, not `targetResources`, not `intent`, and not, were it added, Authorization Purpose — reaches the `EventBus` via this mechanism today.** This is not a gap specific to Authorization Purpose; every existing content field on `ExecutionRequest` is in the identical position.

Authorization Purpose's own audit visibility, under the selected carrier, would therefore be **identical to every other `ExecutionRequest` content field's**: reachable by direct inspection of the retained request object wherever it is passed or logged (for example, by whatever component ultimately constructs an audit record from a completed `ExecutionRequest`/`PermissionDecision` pair), never automatically published to the event trail as a side effect of adding the field. This is a correct, honest inheritance of the *existing* provenance posture — not a weaker one invented for this document — but it is a materially different claim from "reaches wherever those fields already reach" read as implying event-trail visibility.

**One question is deliberately left open, not decided here**: whether `PermissionDecision` (`src/contracts/Permission.kt`) should also be extended to echo back the matched Authorization Purpose, for fuller audit fidelity on the *decision* side, not only the *request* side. This mirrors `PermissionDecision.action`'s own existing precedent (it already echoes back the resolved `PermissionAction`) and is worth a future Scope Lock's own consideration, but is not settled by this Contract Design.

---

## 18. Migration Considerations

- **Schema.** `ExecutionRequest.schema.json` and `docs/specifications/volume-01-core-contracts/ExecutionRequest.md` would need a corresponding, deliberate amendment — an ADR-017/018-tier decision, not a casual one, but a narrower one than Candidates B/C/D require (no interface signature change).
- **Existing callers.** Every current constructor of an `ExecutionRequest` (`PermissionFilteredMemoryRetrieval`, `DefaultKnowledgeRetrieval`, `DefaultKnowledgeSubmission`, `EvidenceRegistrationCoordinator`, and every other already-identified site) continues to compile and behave unchanged if the new field is optional/defaulted — exactly as adding `sessionId`/`riskEstimate`/`expiresAt` did not break any existing caller.
- **Retrofit.** Assigning real Authorization Purpose values to existing consumers (`DefaultKnowledgeCandidateEvaluator`, `EvidenceIntelligenceInputResolver`, and others named in the Authorization Purpose Programme's own Section 5/10) remains separate, later, Scope-Lock-and-Implementation-Plan-tier work — not authorised or begun here.
- **Vocabulary governance and registration mechanism** remain open, per the Authorization Purpose Programme's own Section 10, Unit 2 — not resolved by this document, since it concerns the vocabulary's own content and review discipline, not the carrier question this Unit answers.

---

## 19. Risks

- **Risk: the selected field is implemented as, or drifts into, a raw `String`,** reopening the "no forged/free-text Authorization Purpose" risk the Authorization Context Contract Design already rejected. **Mitigation:** any future Scope Lock must specify a closed, non-`String` value type (mirroring `PrincipalId`/`ResourceId`), not merely add a field of any shape.
- **Risk: `DefaultPermissionPolicy`'s own resolution step, once extended a second time (verb phrase, then Authorization Purpose), becomes difficult to reason about deterministically.** **Mitigation:** a future Scope Lock must specify the exact precedence/matching order between the two discriminators explicitly, not leave it to implementation-time discovery.
- **Risk: this document's own selection is read as authorising the schema amendment to begin immediately.** **Mitigation:** Section 2 and this document's own Status line state explicitly that no Scope Lock, Implementation Plan, or Kotlin is authorised by this Contract Design alone.
- **Risk: a future author, encountering the "no forged Authorization Purpose" boundary (Section 13), interprets its honest limits as an unaddressed defect rather than an already-accepted, general Trust Framework trust boundary.** **Mitigation:** Section 13 states this explicitly and ties it to the identical, already-accepted `principalId` precedent, so it is not mistaken for an oversight.

---

## 20. Recommendation

Proceed to Unit 2 of the Authorization Purpose Programme (Vocabulary Governance) informed by this Unit's own selection — Authorization Purpose values will be declared on `ExecutionRequest` itself — before any Scope Lock is drafted for either Unit. A Scope Lock for the carrier itself (the schema amendment, the value type's own exact shape, and `DefaultPermissionPolicy`'s own precedence rule between the verb-phrase and Authorization Purpose discriminators) may reasonably follow once vocabulary governance is far enough along to know what the value type must actually hold. This document does not authorise that Scope Lock and does not begin it.

---

## 21. Independent Constitutional Review Self-Check

Performed by the author before requesting external review — a genuine external Independent Constitutional Review follows as a separate document.

- **Forged Authorization Purpose?** Addressed directly (Section 13) as an inherited, already-accepted trust boundary, not concealed.
- **Ambient authority?** No — the field is explicit, caller-declared, never inferred; Section 3's own sequential-chaining finding confirms no implicit inheritance across related requests.
- **No caller-specific exceptions?** Confirmed — the field is available identically to every caller of `ExecutionRequest`, present or future (Section 6's own "future Tools" row).
- **Single Permission Engine/Policy preserved?** Yes — no interface change; `DefaultPermissionPolicy` extended, not duplicated.
- **Nested/asynchronous evaluation?** Directly investigated (Section 3), found not to exist in the current codebase; propagation discipline stated explicitly regardless.
- **Compatibility with future Trust Framework evolution?** Chapter 10 §10's own disclosure is cited directly, not assumed, as the basis for this not foreclosing future carrier evolution.
- **Implementation details frozen prematurely?** No — Section 7 states explicitly what is and is not decided; field name, value type shape, registration mechanism, and `PermissionDecision`'s own possible extension are all left open.

```
AUTHORIZATION PURPOSE CARRIER CONTRACT DESIGN — DRAFT COMPLETE, PENDING INDEPENDENT CONSTITUTIONAL REVIEW
```
