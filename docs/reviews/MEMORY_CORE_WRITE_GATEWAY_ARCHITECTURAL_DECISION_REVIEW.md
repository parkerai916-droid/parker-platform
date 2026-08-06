# Memory Core Write-Gateway Architectural Decision Review

## Status

**Independent architectural decision review. No Kotlin implemented, proposed as a diff, or changed by this document. No governance document amended. Neither `src/` nor `tests/` is touched.** This is not an implementation task and not a Planning Review for one — it is a comparison of two architectural directions, tested against the actual repository and the governing documents, producing exactly one recommendation. Nothing is staged, committed, or pushed.

---

## 1. Repository Baseline

Confirmed at the start of this review:

- **HEAD:** `8d9f4a89d11e5ae085e56103d00371c2db1e750e` (short `8d9f4a8`) — "docs: archive OCR mechanism programme."
- **Branch:** `main`.
- **Working tree:** clean.
- **OCR Mechanism programme:** confirmed complete and archived (`docs/reviews/OCR_MECHANISM_PROGRAMME_COMPLETION_REVIEW.md`, recommendation READY FOR ARCHITECTURAL ARCHIVE).
- **Current Memory Core implementation state, confirmed directly from the repository, not from memory:**
  - `src/interfaces/MemoryCore.kt` — `MemoryCore` (6 write operations) and `MemoryRetrieval` (10 read operations), both amended by Errata 004 to carry `requestingPrincipalId` on every write and every direct-lookup read.
  - `src/runtime/InMemoryMemoryCore.kt` — the sole base implementation, permission-neutral by construction (no `PermissionEngine` parameter exists to accept).
  - `src/composition/PermissionGatedMemoryCore.kt` and `src/composition/PermissionFilteredMemoryRetrieval.kt` — both fully implemented, both already committed (commit `d6240b7`, an ancestor of current `HEAD`), both currently passing their own test suites (22 and 23 tests respectively, re-run fresh: BUILD SUCCESSFUL).
  - `src/composition/EventPublishingMemoryCore.kt` — a third decorator (event publication), also fully implemented, also **not** currently composed into `ParkerRuntime.kt`.
  - `src/composition/ParkerRuntime.kt` — constructs plain `InMemoryMemoryCore()` directly (line 604) and passes it, unwrapped by any decorator, to `EvidenceRegistrationCoordinator` and `EvidenceIntelligenceAcceptanceCoordinator`. `PermissionFilteredMemoryRetrieval` **is** composed live (line 735) and has two real consumers. `PermissionGatedMemoryCore` and `EventPublishingMemoryCore` are constructed nowhere in this file.

No discrepancy against expectations found.

---

## 2. Authorities Reviewed

Read fresh, in full or by targeted section:

- `docs/architecture/MEMORY_ARCHITECTURE_RECONCILIATION.md` — full (17 sections), previously read in full this session and re-confirmed; §12 (Trust Boundary) is the origin of the single-boundary decision this review re-examines.
- `docs/architecture/MEMORY_CORE_CONTRACT_DESIGN.md` — §15 (Runtime Responsibilities) read in full; remainder previously read in full this session.
- `docs/architecture/MEMORY_CORE_SCOPE_LOCK.md` — full (through §15 in depth this session; §5, §6, §13, §14 are the load-bearing sections for this review).
- `docs/implementation/MEMORY_CORE_IMPLEMENTATION_PLAN.md` — §7 (Runtime Integration), §13 (Completion Criteria), §14 (Deferred Work Register), §15 (Risks), §17 (Unit 10 Acceptance Tracking) read in full.
- `docs/architecture/MEMORY_CORE_CONTRACT_DESIGN_ERRATA_004.md` — read in full (all 11 sections) fresh this session.
- `docs/decisions/CDR-005_CONSTITUTIONAL_ADMISSION_OF_PERMISSIONENGINE_PROPOSAL_CLASSES.md` — Decision, Decision Rules, and Constitutional Visibility sections read; this is the only CDR governing how a domain act becomes (or stops being) a `PermissionEngine` proposal class, and is the correct authority for whether centralising Memory Core's permission checks requires new constitutional process.
- `docs/architecture/parker-constitution.md` — searched for "defense in depth," "layered," "single point," "redundant"; no explicit doctrine on double-checking is stated either way. The Constitution's own operative principle throughout is "no capability may bypass trust" and "cognition proposes, trust authorises, runtime executes" — a statement about *where* authority sits (never with the proposer), not about *how many times* a proposal may be checked.
- `src/composition/PermissionGatedMemoryCore.kt`, `src/composition/PermissionFilteredMemoryRetrieval.kt`, `src/composition/EventPublishingMemoryCore.kt`, `src/composition/ParkerRuntime.kt` (in full, 1319 lines), `src/runtime/EvidenceRegistrationCoordinator.kt`, `src/runtime/EvidenceIntelligenceAcceptanceCoordinator.kt` — all read in full.
- `src/runtime/InMemoryMemoryCore.kt` — re-confirmed structurally permission-neutral (no `PermissionEngine` constructor parameter).
- Every other production class referencing `MemoryCore`/`MemoryRetrieval` identified by repository-wide search: `src/runtime/EvidenceExtractionCoordinator.kt` (holds `MemoryRetrieval`, reads `getDocument`), `src/runtime/DefaultKnowledgeRevisionEvaluator.kt` (holds `MemoryRetrieval`), `src/runtime/DefaultKnowledgeCandidateEvaluator.kt` (holds `MemoryRetrieval`), `src/runtime/EvidenceIntelligenceInputResolver.kt` (holds `MemoryRetrieval`) — confirmed, by direct inspection of `ParkerRuntime.kt`, that only the latter two are actually constructed in the live composition graph.
- `tests/composition/PermissionGatedMemoryCoreTest.kt` (22 tests), `tests/composition/PermissionFilteredMemoryRetrievalTest.kt` (23 tests) — read/counted in full; both re-run fresh (BUILD SUCCESSFUL), together with `EvidenceRegistrationCoordinatorTest.kt` and `EvidenceIntelligenceAcceptanceCoordinatorTest.kt`.

---

## 3. Current Write-Path Inventory

Exactly two production classes invoke a `MemoryCore` write operation anywhere in the compiled, composed system. No third writer exists.

| Caller | Operation(s) invoked | Permission evaluation performed today | Resource/action evaluated | Principal source | Denial before Memory Core call? | Uses raw `MemoryCore`? | Would `PermissionGatedMemoryCore` beneath it double-gate? | Inline logic not reproducible by the decorator as it exists today? |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `EvidenceRegistrationCoordinator.register` | `createProvenance`, then `registerDocument` | Two **separate** `permissionEngine.evaluate` calls, one per operation, each with its own registered `ResourceId` (`MEMORY_CORE_PROVENANCE_RESOURCE_ID`, `MEMORY_CORE_DOCUMENT_REGISTRATION_RESOURCE_ID`) | `WRITE`/`MEMORY` for provenance; `WRITE`/`MEMORY` for document registration (both resolve against real, registered Resource Registry entries) | `requestingPrincipalId` parameter, passed unchanged from `ParkerRuntime.submitEvidence`'s own caller-supplied value | Yes — `createProvenance` is never called if the provenance decision denies; `registerDocument` is never called if the document decision denies | Yes — `memoryCore: MemoryCore` constructor parameter, the same instance held by `ParkerRuntime` | **Yes** — both operations are already individually gated before this coordinator calls them | **Yes.** Two independent, structurally-typed outcome variants (`ProvenanceNotAuthorised`, `DocumentRegistrationNotAuthorised`), each carrying the already-durable prior state (e.g. the already-accepted artifact) so a caller is never told less than the truth. `PermissionGatedMemoryCore` throws one exception type carrying only a `PermissionDecision` — it cannot express "provenance succeeded, document registration denied" as a distinguishable, non-exceptional caller-facing result the way `EvidenceRegistrationOutcome` does |
| `EvidenceIntelligenceAcceptanceCoordinator.dispatchRecord` | `createAssertion` or `createRelationship` (exactly one, per candidate) | One `permissionEngine.evaluate` call per candidate, using a registered `ResourceId` (`MEMORY_CORE_ACCEPTANCE_RESOURCE_ID`) | `WRITE`/`MEMORY` (resolves against a real, registered Resource Registry entry) | `requestingPrincipalId` parameter, passed unchanged from `ParkerRuntime.analyseEvidence`'s own caller | Yes — `memoryCore.createAssertion`/`createRelationship` is never called on denial | Yes — `memoryCore: MemoryCore` constructor parameter, the same shared instance | **Yes** — already gated before this coordinator calls it | **Yes.** The coordinator's own `internal` `EvidenceIntelligenceAcceptanceOutcome.RecordAcceptance` distinguishes `Written`/`NotAuthorised` as an ordinary, non-exceptional return value dispatched alongside three *other* candidate kinds (`TransientOutput`, `CandidateArtifactProduced`, `CandidateKnowledgeProduced`) in one uniform `List<EvidenceIntelligenceAcceptanceOutcome>` — the coordinator's own KDoc states plainly that collapsing this into a thrown exception "would misattribute the decision to a subsystem that never rendered it," since a thrown `MemoryCoreWriteDeniedException` would abort the whole `dispatch` call (per its own "no try/catch... a genuine exception propagates" discipline), terminating processing of every *other* candidate in the same batch rather than recording one candidate's own denial and continuing |

**Every other reference to `MemoryCore` found in `src/` is not a write-path caller:** `InMemoryMemoryCore` is the base implementation being called, not a caller; `PermissionGatedMemoryCore` and `EventPublishingMemoryCore` are decorators with no consumer in the composed graph; `DerivativeReview.kt`, `EvidenceExtractor.kt`, `EvidenceIntelligence.kt`, `KnowledgeStore.kt`, `OcrMechanism.kt`, `OcrProviderAdapter.kt`, `OcrExecutionSequencer.kt`, `ProvenanceReference.kt` reference Memory-Core-owned *types* (`Provenance`, `MemoryCoreRecordReference`, identifier value classes) without holding a `MemoryCore` dependency capable of writing.

No indirect or further-coordinator-mediated write path exists beyond these two. `submitEvidence` and `analyseEvidence` are `ParkerRuntime`'s own two production entry points that reach Memory Core's write surface, and each delegates to exactly one of the two coordinators above with no intermediate layer.

---

## 4. Current Read-Path Inventory

`PermissionFilteredMemoryRetrieval` is composed exactly once, in `ParkerRuntime.buildAndRegisterRuntimeGraph` (line 735), wrapping the single, shared `inMemoryMemoryCore` instance directly (Errata 004 §9: "the frozen stack has no `EventPublishingMemoryCore` equivalent on the read side"). It has two real, live production consumers:

- **`EvidenceIntelligenceInputResolver`** (constructor parameter `memoryRetrieval`), reading Memory Core records as part of resolving Evidence Intelligence's own analysis input.
- **`DefaultKnowledgeCandidateEvaluator`** (constructor parameter `memoryRetrieval`), reading Memory Core records to evaluate a Knowledge Candidate.

Neither consumer, nor `ParkerRuntime` itself, is ever handed the raw `inMemoryMemoryCore` under its `MemoryRetrieval` type — `ParkerRuntime.kt`'s own comment states this explicitly: "`inMemoryMemoryCore` is never exposed to either as a raw `MemoryRetrieval`."

Two further classes hold a `MemoryRetrieval` constructor parameter but are **not** constructed anywhere in `ParkerRuntime.kt` — `EvidenceExtractionCoordinator` (Evidence Processing's own coordinator, reads `getDocument`) and `DefaultKnowledgeRevisionEvaluator` (reads via `resolve`, evaluating whether a Knowledge Item's own cited evidence is still resolvable). Both are dormant in the same sense `PermissionGatedMemoryCore` is dormant on the write side — implemented, presumably tested in isolation, but with no live composition path today. This is noted for completeness; neither changes this review's own write-side analysis.

**Does the read side demonstrate a preferred architectural precedent that legitimately supports a mandatory write gateway? No — and the reason is a genuine, material difference in semantics, not an oversight to be smoothed over:**

- **Every current read-side consumer is generic.** Neither `EvidenceIntelligenceInputResolver` nor `DefaultKnowledgeCandidateEvaluator` has, or needs, its own bespoke permission logic for Memory Core reads — reading is reading, and "may this Principal see this record" is exactly one question, uniform across every reader, with no caller-specific outcome type to preserve. This is precisely why `PermissionFilteredMemoryRetrieval` could be composed as the *only* path to `MemoryRetrieval` without displacing anything: there was nothing to displace.
- **Both current write-side callers are not generic — each already owns a caller-specific compound outcome, denial semantics, and failure-continuation behaviour that a single, generic decorator does not (and, without further design, cannot) reproduce.** `EvidenceRegistrationCoordinator` must distinguish "provenance denied" from "document registration denied" as two *different*, non-exceptional outcomes; `EvidenceIntelligenceAcceptanceCoordinator` must record one candidate's denial as an ordinary list entry and continue processing the rest of the batch, which a decorator that throws cannot do without the coordinator adding a `try`/`catch` around a call it exists specifically to avoid needing.
- **Read denial and write denial are also observably different in kind.** Errata 004 §8 fixes read denial as *non-disclosing* (`null`, indistinguishable from "not found") specifically so a caller can never detect a protected record's existence. Write denial is the opposite: both existing coordinators *deliberately* disclose a denial as its own distinguishable outcome (`ProvenanceNotAuthorised`, `RecordAcceptance.NotAuthorised`), because a write denial has no non-disclosure obligation — nothing is being hidden by refusing to create a record. A single write gateway mirroring `PermissionFilteredMemoryRetrieval`'s own "swallow the distinction, return a uniform shape" pattern would need to actively *discard* information every current caller deliberately preserves.

Architecture is indeed less cooperative than the diagram suggests: the two sides are decorators of sibling interfaces, but that is where the symmetry ends.

---

## 5. Governing-Document Interpretation

| Clause | Requires runtime to permission-check each write? | Requires one central gateway? | Permits multiple correctly-ordered gates? | Forbids a `PermissionEngine` reference inside Memory Core itself? | Distinguishes the raw in-memory implementation from a composition decorator? | Requires the raw delegate never to escape? | Settles centralised vs. distributed enforcement? |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Scope Lock §6 ("Memory Core never evaluates permissions. Runtime performs all permission decisions before invoking Memory Core... No contract implementing `MemoryCore` or `MemoryRetrieval` may hold a `PermissionEngine` reference, construct a `PermissionDecision`, or perform any principal-based filtering of its own.") | **Yes**, unconditionally, for every operation | **Silent.** Names *Runtime* as the enforcing tier, never names a single class or a count of enforcement points | **Silent** — does not forbid more than one Runtime-tier check, and does not require exactly one | **Yes**, explicitly, for `InMemoryMemoryCore` (the type that implements the contract's own core storage semantics) | **Not explicitly** — see Contract Design §15 and the Constitutional Tier discussion below for why the accepted implementation (`PermissionGatedMemoryCore : MemoryCore`) reads this as governing the base implementation, not every class that happens to implement the interface for delegation | **Silent** | **No** — freezes *that* Runtime gates, not *how many* Runtime-tier classes may each independently gate |
| Scope Lock §14 ("Memory Core **SHALL NOT** hold, construct, evaluate, or reference a `PermissionEngine` decision anywhere within `MemoryCore` or `MemoryRetrieval`.") | Yes (restates §6 as a formal SHALL) | Silent | Silent | Yes, absolute, for the contract itself | Not addressed here either | Silent | No |
| Scope Lock §14 ("Every `MemoryCore` write operation **SHALL** be independently invocable and independently permission-gateable by Runtime — no two operations **SHALL** be bundled such that Runtime cannot evaluate a permission decision for one without also authorising the other.") | Yes | **No — actively cuts against a naive single gateway**, if that gateway would bundle two operations behind one decision. This is the clause most directly on point for Option B: it requires *independent* gateability per operation, which both existing coordinators already provide (two separate `ExecutionRequest`s for provenance/document; one per candidate for record acceptance) | Implicitly permits — independence is easiest to achieve when each caller can shape its own request per operation, which is what today's distributed model already does | N/A | N/A | N/A | **Partially** — this clause is evidence *against* collapsing distinct operations into one opaque gate, without settling how many classes may perform the gating |
| Contract Design §15 (Runtime Responsibilities: "Runtime (whatever composes Memory Core into the running system) owns... every `PermissionEngine.evaluate` call required... before any `MemoryCore` write and before any sensitive `MemoryRetrieval` read reaches its requester.") | Yes | Silent — "whatever composes Memory Core into the running system" is deliberately open about which class or classes perform this | Implicitly yes — the phrase does not say "exactly one class," and the repository's own accepted precedent (`EvidenceRegistrationCoordinator`, `EvidenceIntelligenceAcceptanceCoordinator`) already instantiates "Runtime" as more than one class | Restates the same boundary | No | No | No |
| Implementation Plan §7 (Runtime Integration: "`ParkerRuntime.kt` composes `InMemoryMemoryCore` once, and exposes it to the rest of the running system only through these two wrapper types... never the raw delegate directly.") | N/A (describes composition, not the check itself) | **This is the clause closest to requiring a single gateway** — but it describes a *composition intent* for a system with no other writer contemplated at drafting time (§7's own text: "No production caller is introduced for Memory Core's write path in this plan"). It says nothing about what happens once a caller *does* self-gate for its own documented, load-bearing reason, since no such caller existed when this was written | Not addressed — silent on multiple gates because no second caller existed to raise the question | N/A | N/A | **Yes, implicitly** — "never the raw delegate directly" is a statement about what `ParkerRuntime` itself hands out, not a statement reaching into a coordinator's own already-authorised, already-reviewed choice to hold `memoryCore: MemoryCore` directly for its own documented reason | No — this clause is silent on the exact question this review answers, and silence here is **not** read as authorising either option by default (see below) |
| Implementation Plan §17 ("`PermissionGatedMemoryCore`... but wrapping either's raw `memoryCore` dependency in `PermissionGatedMemoryCore` would double-gate an already-gated call; no genuine consumer for it exists in this graph.") | N/A | **No** — this is the composition root's own, already-accepted reasoning for *not* introducing a single gateway today | Implicitly yes — it treats the two coordinators' own inline gates as sufficient and correct as they stand | N/A | N/A | N/A | **This is the closest thing to a settled answer already on record**, and it settles in favour of the distributed model as *today's* correct state, not as a permanent architectural verdict |
| Errata 004 §7 (resource representation: `targetResources` always `emptyList()` for the decorators; `DefaultPermissionPolicy`'s resolution "would deny every Memory Core write unconditionally" if routed through the shared, already-wired policy instance) | N/A | **Actively works against adopting the gateway as currently built without further composition work** — a concrete, disclosed technical fact, not merely a governance silence | N/A | N/A | N/A | N/A | Confirms the current gap is partly *mechanical* (a resource-representation mismatch), not solely architectural preference |

**Silence is not read as authorisation for either option.** No clause states "one gateway is required" and no clause states "distributed gating is the permanent architecture." Where the documents are silent (how many Runtime-tier classes may gate, whether an already-accepted coordinator must eventually be migrated), this review treats that as an open question for the documents' own future revision — not as tacit endorsement of whichever option this review might otherwise have preferred.

---

## 6. Option A Assessment — Dormant Capability

**Constitutional validity.** Fully valid today, unconditionally. Scope Lock §6/§14's own absolute requirement — Memory Core itself never evaluates permissions — is satisfied regardless of whether `PermissionGatedMemoryCore` is ever wired, since the requirement binds `InMemoryMemoryCore`, not any particular composition choice above it. Nothing about leaving the decorator dormant violates any SHALL/SHALL NOT this review found.

**Security strength.** Equal to today's actual, running security posture — because Option A *is* today's actual, running security posture. Every Memory Core write in the composed system is already gated, once, by its own caller, before delegation. No write path exists that reaches `InMemoryMemoryCore` ungated.

**Duplicated permission policy.** None exists today under Option A, and none is created by choosing to remain here — the two coordinators' own policies are not duplicates of each other (different resources, different actions in `EvidenceIntelligenceAcceptanceCoordinator`'s case, different resource identifiers even where the action name is the same shape).

**Risk of future callers bypassing gating.** Real, but structural, not merely aspirational: nothing in the type system prevents a future third writer from being constructed with a raw `memoryCore: MemoryCore` reference and *not* adding its own gate, since Memory Core itself cannot enforce this (by design). This risk is identical under Option A and Option B unless Option B also removes raw `MemoryCore` from every constructor signature reachable outside the gateway itself (see §9, Migration Consequences).

**Discoverability for future engineers.** Good, not perfect. `PermissionGatedMemoryCore`'s own KDoc, `ParkerRuntime.kt`'s own inline comment at its construction site, and Implementation Plan §17 all explain *why* it is unwired, in detail, at the exact place a future engineer would look. The risk is that an engineer who does not read `ParkerRuntime.kt`'s comments before adding a third writer could reasonably assume the existing decorator is "the" way to gate a write and reach for it without noticing the double-gating consequence for two operations it does not, in fact, need to protect.

**Testability.** Fully tested in isolation (22 tests, `PermissionGatedMemoryCoreTest.kt`, covering delegation, denial-without-delegation, `APPROVED_WITH_CONFIRMATION`, exact `ExecutionRequest` shape, fault propagation from both the delegate and the engine, and structural safeguards on the class's own shape). What is *not*, and cannot be, tested today is its behaviour once composed with a real `DefaultPermissionPolicy` instance and a real caller — that integration path simply does not exist to test.

**Maintenance burden.** Low but non-zero: any future amendment to `MemoryCore`'s own signature (as Errata 004 already did once) requires updating a class with zero production callers, purely to keep it compiling — cost paid for a capability not currently exercised.

**Risk of dead code.** Present, and already disclosed as such by the repository's own governance (Implementation Plan §17: "not yet claimed as compiled or passing... the compiler itself" already flagged one earlier over-eager wiring attempt as unused). This is the honest cost of Option A, not something to minimise.

**Is dormant accepted code architecturally honest?** Yes, on the evidence here — it is not silently forgotten, unreviewed, or undocumented. It differs from ordinary "dead code" in three concrete ways: it has its own accepted Implementation Plan unit, its own passing test suite, and an explicit, reasoned KDoc/comment trail at its own non-use site explaining precisely why. Dormant-but-documented is not automatically acceptable merely by asserting it; it is acceptable *here* because all three of those conditions are independently verifiable in the repository today.

**Does the Implementation Plan already contemplate this state?** Yes, explicitly and by name (§7: "`InMemoryMemoryCore`, composed behind its two permission gates, will be in an identical, deliberately dormant-but-ready state at the end of this plan's own scope," mirroring today's Knowledge Memory write path, which the same document says has been fully implemented, fully tested, and fully wired with zero production callers for exactly the same disclosed reason).

**Lawful trigger for reconsideration.** The first approved Memory Core write consumer that does *not* already bring its own inline gate — exactly the gap Implementation Plan §14's Deferred Work Register already names ("a production caller for Memory Core's write path... not yet approved").

**Strongest case for Option A:** it is already true, already accepted, already tested, and changes nothing about a system whose write path is fully, correctly, and independently gated today; adopting it going forward costs nothing beyond continuing not to invent a consumer.

**Strongest case against Option A:** it leaves a second, structurally different enforcement idiom (registered-Resource-based inline gating vs. always-empty-`targetResources` decorator gating) permanently available side-by-side, which is exactly the kind of divergence that could let a *third*, future writer choose the wrong one, or choose neither, without any structural signal forcing a choice.

---

## 7. Option B Assessment — Mandatory Single Write Gateway

**Constitutional validity.** Not itself forbidden by any clause found — but not currently *achievable* as a drop-in replacement, for a concrete, disclosed, mechanical reason (below), which is a implementation-readiness finding, not a constitutional objection.

**Centralisation of authority.** Genuine and real if achieved: exactly one class becomes the sole place a Memory Core write permission decision is rendered, which is a legitimate security property in its own right (a single audit point, a single place to reason about correctness).

**Elimination of duplicated permission checks.** There are, today, no duplicated checks to eliminate (§6, above) — Option B's centralisation benefit here is prospective (preventing *future* duplication), not a fix for anything currently duplicated.

**Migration impact on existing writers — substantial, and the single most important finding of this review:** `PermissionGatedMemoryCore`, as it exists today, cannot correctly replace either coordinator's own inline gate without additional work, because of a genuine representational incompatibility, not a policy-content difference:
- `EvidenceRegistrationCoordinator` and `EvidenceIntelligenceAcceptanceCoordinator` each build an `ExecutionRequest` with `targetResources = listOf(<a real, registered ResourceId>)`, resolved successfully by `DefaultPermissionPolicy` against entries actually registered in `ParkerRuntime`'s `ResourceRegistry` — this is *why* their checks are approved today.
- `PermissionGatedMemoryCore`, per Errata 004 §7's own frozen, deliberate design, always builds `targetResources = emptyList()` — because Memory Core records are never Resource Registry entries at all.
- Under the *same, already-wired, shared* `DefaultPermissionPolicy` instance every other check in this runtime uses, an empty `targetResources` list resolves to an empty `resourceTypes` set, which — per Errata 004 §7's own disclosed consequence — **matches no rule and is denied unconditionally**, regardless of the `WRITE`/`MEMORY` rule already `APPROVED` in `ParkerRuntime.kt` for the coordinators' own, differently-represented checks.
- Consequence: naively substituting `PermissionGatedMemoryCore` for either coordinator's own gate today would not merely add redundancy — it would **break every currently-working Memory Core write**, unless a second, dedicated policy or rule set capable of deciding `(WRITE, MEMORY)` without a resolved target Resource is also introduced, exactly as Errata 004 §7's own final paragraph already anticipates as "a future, dedicated Runtime-composition wiring decision."

**Risk of losing caller-specific permission context.** Confirmed, concretely, not merely feared: `EvidenceRegistrationCoordinator`'s two-decision split and `EvidenceIntelligenceAcceptanceCoordinator`'s per-candidate, non-aborting denial handling are both caller-specific behaviours `PermissionGatedMemoryCore`'s own single, throwing `requireApproved` cannot reproduce without those coordinators independently catching `MemoryCoreWriteDeniedException` around each call — which reintroduces, at the coordinator layer, exactly the `try`/`catch` both classes' own KDoc currently states, as a documented design property, that they do not have.

**Principal propagation.** Not a blocker — Errata 004 already threads `requestingPrincipalId` through every relevant signature; both models could propagate it identically.

**Resource construction.** A blocker as currently built (above) — this is the crux of the migration difficulty, not principal handling.

**Denial semantics.** Materially different today: the decorator throws (`MemoryCoreWriteDeniedException`); both coordinators return a typed, non-exceptional outcome. Adopting Option B without changing the decorator's own denial semantics would force every existing caller to convert an exception into its own outcome type via `try`/`catch` it currently, deliberately, does not have.

**Exception behaviour.** Both models propagate a genuine fault (from the engine or the delegate) unchanged today; this property is preserved under either option and is not a point of difference.

**Audit/event behaviour.** A live gap exists independent of this decision: `EventPublishingMemoryCore` is *also* unwired today, so no Memory Core write currently publishes any of the five frozen events, regardless of which gating model is chosen. Adopting Option B does not, by itself, resolve this; it would need to be a deliberate, separate part of any migration that also wires the frozen `PermissionGatedMemoryCore → EventPublishingMemoryCore → InMemoryMemoryCore` stack Errata 004 §9 names.

**Transaction or sequencing effects.** None identified — Memory Core has no cross-record transactional model today under either option (each write is independently committed, per Scope Lock §11's own "immutable history" architectural expectation), so centralising the gate does not, by itself, introduce or resolve a sequencing concern beyond what already exists (e.g., `createProvenance` must still precede `registerDocument`, which is a data dependency, not a permission-ordering one).

**Can existing writers safely stop self-gating?** Not today, without the resource-representation fix above *and* a redesign of each coordinator's own outcome handling to tolerate (or convert) a thrown denial. "Safely" specifically requires both.

**Is the decorator contract currently rich enough to replace every inline gate?** No — confirmed structurally in §3 and §4 above: it has one denial shape (a thrown exception) against two callers that each need a different, richer, non-exceptional shape.

**Must raw `MemoryCore` become composition-private?** Yes, if Option B is ever adopted as *mandatory* — otherwise "mandatory" is aspirational language with no structural enforcement, exactly the same weakness Option A already carries (§6, above, "risk of future callers bypassing gating"). This would require narrowing every future coordinator's own constructor to accept only the gateway type, and, ideally, a structural test (mirroring OCR Unit 9's own precedent in this same repository) proving `InMemoryMemoryCore` is reachable from `ParkerRuntime.kt` only through the mandatory gateway — which Implementation Plan §15's own named risk ("a structural test that `InMemoryMemoryCore` itself is never reachable... except through the two wrappers") already anticipated wanting, for exactly this reason.

**Does this require Contract Design, Scope Lock, Errata, or CDR change?** Yes, at minimum an Errata (Errata 004's own successor), and very plausibly a CDR — see §10, below, for the reasoning.

**Strongest case for Option B:** if realised correctly, it removes the standing risk that a *third* future writer could pick the wrong idiom or none at all, and it gives Memory Core writes the same "one Runtime-tier enforcement point" property `ExecutionPipeline` already gives Tool execution generally.

**Strongest case against Option B:** it is not a composition-only change today. It requires resolving a genuine resource-representation incompatibility that would otherwise silently deny every Memory Core write, and it requires redesigning two already-accepted coordinators' own caller-specific outcome handling — real implementation and governance work, not a wiring flip.

---

## 8. Double-Gating Analysis

Compared operation by operation, not generalised from one method:

| Operation | Coordinator's own inline check | `PermissionGatedMemoryCore`'s own check, if placed beneath it | Observably different? | Classification |
| --- | --- | --- | --- | --- |
| `createProvenance` (via `EvidenceRegistrationCoordinator`) | `targetResources=[MEMORY_CORE_PROVENANCE_RESOURCE_ID]` (registered), action `memory.create-provenance` | `targetResources=[]`, action `memory.create_provenance` (note: distinct literal string, distinct casing/separator, from the coordinator's own `memory.create-provenance`) | **Yes** — under the shared production policy, the coordinator's check resolves and is approved; the decorator's check resolves to no rule and would be denied. Stacking them would not be "redundant but safe" — it would make the operation newly fail | **Semantically dangerous if stacked naively; operationally redundant only if a compatible second policy is separately supplied** |
| `registerDocument` (via `EvidenceRegistrationCoordinator`) | `targetResources=[MEMORY_CORE_DOCUMENT_REGISTRATION_RESOURCE_ID]` (registered), action `memory.register-document` | `targetResources=[]`, action `memory.register_document` | Same as above | Same as above |
| `createAssertion`/`createRelationship` (via `EvidenceIntelligenceAcceptanceCoordinator`) | `targetResources=[MEMORY_CORE_ACCEPTANCE_RESOURCE_ID]` (registered), action `evidence-intelligence.accept-memory-core-candidate` | `targetResources=[]`, action `memory.create_assertion`/`memory.create_relationship` | Same as above | Same as above |
| `transitionStatus` (no current inline caller) | N/A — no production caller transitions status today | `targetResources=[]`, action `memory.transition_status`/`memory.delete_record` | N/A | Not yet exercised in production either way |

**Answering the required question directly:** double-gating, as it would actually occur if `PermissionGatedMemoryCore` were composed beneath either existing coordinator *today, unmodified*, is:

- **Not constitutionally prohibited** — nothing in Scope Lock forbids more than one Runtime-tier check.
- **Not merely operationally redundant** — it is **semantically dangerous**, because the two layers use materially different resource-representation conventions that resolve differently under the same shared policy; the outer, second check would not silently repeat the inner one's own verdict, it would independently and differently deny.
- **Observably different because of resource construction specifically** (not timing, not exceptions, not principal handling — those three are identical or compatible across both layers; only the target-resource representation differs).
- **Acceptable during migration only** in the narrow sense that a *deliberately reconciled* pair of checks (same resource convention on both sides, or an explicit "coordinator gate is authoritative, decorator gate is a structural no-op/pass-through for this caller" design) could be built as a transitional state — but the two checks as they exist in the repository today are not that; they would need modification first.
- **Unsuitable as a permanent state** in its current, unreconciled form, precisely because "permanent double-gating" implies the second gate is meant to matter, and today it would matter in the wrong direction (denial where there should be approval), not in the redundant-safety-net direction the term "defence in depth" usually implies.

---

## 9. Enforcement Model Analysis

Distinguishing the five named concerns precisely, because duplicated calls are not automatically layered security:

- **Authority** — under both options, authority to write to Memory Core always originates from the same place: the owner, expressed through `PermissionEngine`/`DefaultPermissionPolicy`'s own policy content, which `ParkerRuntime` alone supplies (`IMPLEMENTATION_GAPS.md` #25, cited in `ParkerRuntime.kt`'s own KDoc: "policy content remains something a caller decides"). Neither option relocates authority; both merely choose *where* that authority is consulted.
- **Policy evaluation** — today, performed twice per Evidence Registration write (once for provenance, once for document) by design (Scope Lock §14's own "independently permission-gateable" requirement), and once per Evidence Intelligence acceptance write. This is not duplication of the *same* decision; it is two, or three, *distinct* decisions, each with its own resource/action pair.
- **Validation** — neither existing coordinator performs content validation as part of its permission check; validation (mandatory Provenance reference, lifecycle legality) is `InMemoryMemoryCore`'s own, separate, non-permission responsibility, unaffected by either option.
- **Orchestration** — both coordinators are orchestration in the ordinary sense (sequencing calls, producing a caller-facing outcome); a mandatory gateway, if adopted, would still need an orchestrator *above* it to sequence multi-step acts like Evidence Registration's own provenance-then-document dependency — the gateway itself cannot replace orchestration, only the permission-check step within it.
- **Defence in depth, properly understood** — a genuine layered-defence claim requires each layer to catch a *different* failure mode (e.g., a coordinator-level business-rule check plus an independent, structurally-separate authority check). Today's model already has this property in one direction Option B would not add: `EvidenceCustodian.accept`'s own internal gate is structurally independent of, and never duplicated by, either Memory Core coordinator. Applying a *second* Memory Core-specific gate directly beneath an already-gated Memory Core call is not defence in depth by this definition — it is the same authority question asked twice of the same policy source, which is why §8's mismatch matters: two askings of the same question should agree, and today they would not.

**Which model does Parker's constitutional material favour?** The documents read closest to: **one authoritative gate per governed act, exercised at whichever Runtime-tier point that act's own coordinator naturally sits** — not literally "exactly one class in the whole system," and not "caller-side gating plus a second, independent downstream enforcement of the identical question." Scope Lock §14's own "independently invocable and independently permission-gateable" requirement is satisfied today by each coordinator owning its own act-specific gate; a single shared gateway would need to preserve that same independence per act (not collapse Evidence Registration's own two acts into one decision), which is achievable but is exactly the redesign work identified above, not a wiring change.

---

## 10. Failure and Bypass Analysis

| Risk | Option A (dormant) | Option B (mandatory gateway) | Severity | Likelihood |
| --- | --- | --- | --- | --- |
| Accidental raw-delegate exposure to a new consumer | Present — nothing stops a future class from receiving raw `memoryCore: MemoryCore` and skipping its own gate | Present unless raw `MemoryCore` is made composition-private *and* structurally tested (not yet done under either option) | High | Medium — requires a future engineer to both add a new writer and omit gating |
| Future ungated consumer | Same as above | Same as above, unless the migration in §11 is completed in full, including the structural reachability test | High | Medium |
| Incomplete migration (Option B only) | N/A | High — the resource-representation and outcome-shape gaps identified in §7 mean a partial migration (wiring the gateway without fixing both) actively breaks production writes | Critical if attempted incompletely | Low if the sequence in §11 is followed; high if skipped |
| Stale inline policy (a coordinator's own hardcoded action name drifts from what a future ActionVocabulary registration expects) | Present today, low — both action names are literal constants, unregistered, disclosed as such | Present in either model unless centralised registration is added | Low | Low |
| Decorator omission from runtime composition | This *is* Option A, by definition — not a risk under A, since A is choosing this state deliberately | A real regression risk under B if a future refactor of `ParkerRuntime.kt` accidentally reverts to constructing a raw `InMemoryMemoryCore` for a new caller | Medium | Low, if the structural test named above exists; otherwise medium |
| Inconsistent resource/action mapping | Present, disclosed, and currently harmless (the coordinators' own two conventions never interact, since nothing stacks them) | The central finding of this review (§7, §8) — inconsistent mapping between coordinator and decorator conventions is precisely why naive migration is unsafe | High | Certain, if migration is attempted without first reconciling the two conventions |
| Inconsistent denial behaviour (thrown vs. returned) | N/A — only one shape exists per caller today, each internally consistent | Real, and named directly in §7 — requires resolution before adoption | High | Certain, without redesign |
| Testing gaps | The gap between isolated-unit-tested and integration-tested (§6) | Same gap, until an integration/composition test against the *actual*, shared `DefaultPermissionPolicy` is added — none exists today for either decorator | Medium | Medium |
| Audit gaps | `EventPublishingMemoryCore` is unwired regardless of this decision — no Memory Core write is currently event-audited in production | Unchanged by Option B alone, unless wiring the event decorator is bundled into the same migration | Medium | Certain today, independent of this decision |
| Silent bypass risk | Cannot occur for the two *existing* writers (each is gated); can occur only for a hypothetical future third writer | Same residual risk for a future writer, mitigated only if raw `MemoryCore` is structurally hidden — not merely discouraged by convention | High (if it occurs) | Low today; the risk is entirely prospective under both options |

**Ranking, most to least severe/likely today:** (1) incomplete Option-B migration breaking production writes — critical severity, but only if attempted without the reconciliation in §11; (2) accidental future bypass by a hypothetical third writer — high severity, currently zero live instances, likelihood rises only when such a writer is proposed; (3) the pre-existing, decision-independent event-audit gap — medium severity, already true today regardless of this review's outcome.

---

## 11. Migration Consequences (Option B, Sequence Only — Not Implemented Here)

The minimum lawful sequence, without performing any step:

1. **Governance amendment** (see §12, Constitutional Tier) — a successor to Errata 004 (or, if genuinely contested, a CDR) settling: (a) whether the decorator's own `targetResources=emptyList()` convention or the coordinators' own registered-`ResourceId` convention becomes canonical for all Memory Core checks; (b) the decorator's own denial semantics where a caller needs a non-exceptional, act-specific outcome (a new return-shape option, or an explicit statement that callers needing this must `try`/`catch` at their own boundary).
2. **Composition change** — `ParkerRuntime.kt` would construct `PermissionGatedMemoryCore` (and decide, separately, whether to also finally wire `EventPublishingMemoryCore` beneath it, closing the pre-existing audit gap) and supply it with a policy capable of deciding the amended resource convention.
3. **Migration of each existing writer** — `EvidenceRegistrationCoordinator` and `EvidenceIntelligenceAcceptanceCoordinator` would each need to either (a) receive the gateway type in place of raw `MemoryCore` and adapt their own outcome-handling to catch and re-express `MemoryCoreWriteDeniedException` as their own existing outcome variants, or (b) if the governance amendment instead makes the *coordinator's own* resource convention canonical, the gateway itself would need to accept per-call resource identity rather than always using `emptyList()` — a genuine interface change, not a call-site change.
4. **Preservation of principal and resource semantics** — `requestingPrincipalId` propagation is already uniform (Errata 004) and needs no change; resource identity is the one genuine open design choice (step 1).
5. **Removal or retention of inline checks** — a decision, not an assumption: full removal is only safe once the gateway can prove it reproduces the *exact* same approve/deny outcome for every existing registered resource/action pair; partial retention (coordinator keeps its own check as the authoritative one, gateway becomes a documented structural backstop) is the more conservative, lower-risk migration path given §8's findings.
6. **Structural safeguards preventing raw access** — a test proving `InMemoryMemoryCore` is unreachable from `ParkerRuntime.kt` except through the mandatory gateway, mirroring the OCR Mechanism programme's own Unit 9 precedent in this exact repository.
7. **Regression testing** — every existing `EvidenceRegistrationCoordinatorTest.kt`/`EvidenceIntelligenceAcceptanceCoordinatorTest.kt` scenario re-run against the migrated composition, plus new integration tests exercising the *actual*, shared `DefaultPermissionPolicy` (not `FakePermissionEngine`) to prove the resource-representation reconciliation genuinely works end to end — a test category that does not exist for either decorator today.
8. **Acceptance review** — an Independent Constitutional Review of the amendment plus a completion review of the migration unit, following this repository's own established governance-first workflow.

**Files likely to change, if this sequence were carried out** (identified only, not edited): `docs/architecture/MEMORY_CORE_CONTRACT_DESIGN_ERRATA_004.md` (a successor errata) or a new CDR document; `src/composition/ParkerRuntime.kt`; `src/composition/PermissionGatedMemoryCore.kt` (only if the resource/denial-shape reconciliation requires it); `src/runtime/EvidenceRegistrationCoordinator.kt`; `src/runtime/EvidenceIntelligenceAcceptanceCoordinator.kt`; `tests/composition/PermissionGatedMemoryCoreTest.kt`; `tests/runtime/EvidenceRegistrationCoordinatorTest.kt`; `tests/runtime/EvidenceIntelligenceAcceptanceCoordinatorTest.kt`; a new composition-level integration test file.

---

## 12. Constitutional Tier

**A domain-level governance amendment is required at minimum (an Errata 004 successor); a CDR is likely required, not merely possible, if this direction is pursued.**

Reasoning, applying CDR-005's own Decision Rules directly: adopting Option B would require the domain (Memory Core) to reclassify how an already-approved, already-governed act (Evidence Registration's provenance/document writes; Evidence Intelligence's Memory Core acceptance write) is gated — moving the authoritative decision from a coordinator-owned, registered-Resource-based check to a shared, differently-represented gateway. CDR-005's own rule is explicit: "When a CDR is required: whenever a domain's self-certification against Chapter 10's criteria is genuinely contested, ambiguous, or would require choosing between two or more constitutionally plausible readings." This review has just demonstrated exactly that condition: §5, above, shows the governing clauses are genuinely silent on whether one or several Runtime-tier gates are required, and §7/§8 show the two existing, independently-accepted conventions are not merely different in style but incompatible in outcome under the same shared policy. This is not the "ordinary case... not genuinely contested" CDR-005 reserves for a domain amendment alone — it is a live disagreement between two already-accepted implementation choices about how the same constitutional boundary (Scope Lock §6) should be operationalised, which is precisely the kind of ambiguity CDR-001 through CDR-004 were each written to resolve for their own domains.

No constitutional-tier (Parker Constitution) amendment is required — nothing about the Constitution's own "no capability may bypass trust" principle is disturbed by either option; both already satisfy it. No Contract Design change is required — Contract Design §15's own "Runtime owns every `PermissionEngine.evaluate` call" language already permits either model without modification. This is not drafted here, per this task's own explicit constraint.

---

## 13. Independent Constitutional Review

Audited as if written by another architect, against each named question:

- **Did the review favour an option before examining evidence?** No defensible basis to conclude so: the write-path inventory (§3), the resource-representation mismatch (§7, §8), and the governing-document silence (§5) were each established from direct repository inspection before either option's own assessment section was drafted, and the mismatch finding cuts *against* the option (B) that might otherwise have seemed architecturally cleaner in the abstract.
- **Did it mistake current implementation practice for constitutional law?** Checked directly: §5's own table separates what each clause *actually says* from what `ParkerRuntime.kt`'s own comment merely *currently does*, and §5 explicitly notes that Implementation Plan §17's own reasoning is "today's correct state, not... a permanent architectural verdict" — current practice is treated as evidence of a lawful, accepted state, not as itself a source of binding law.
- **Did it assume read/write symmetry?** Explicitly checked and rejected in §4, with a stated, evidence-based reason (non-disclosure semantics on reads vs. deliberate disclosure semantics on writes; generic readers vs. caller-specific writers) rather than an assertion.
- **Did it treat double-gating as automatically good or bad?** No — §8 classifies it precisely as "semantically dangerous if stacked naively" *and* "not constitutionally prohibited," refusing either blanket label, and grounds the classification in an operation-by-operation comparison, not a generalisation from one method.
- **Did it overlook caller-specific semantics?** No — §3's own table's rightmost column and §7's "risk of losing caller-specific permission context" section are built specifically around `EvidenceRegistrationCoordinator`'s two-decision split and `EvidenceIntelligenceAcceptanceCoordinator`'s non-aborting per-candidate handling, both named and reasoned through individually.
- **Did it propose implementation without authority?** No — §11 is explicitly headed "Sequence Only — Not Implemented Here," names files without editing them, and §12 explicitly declines to draft the amendment or CDR it identifies as likely necessary.
- **Did it invent a future consumer?** No — no hypothetical third writer is named or designed; §10's "future ungated consumer" risk is treated generically, as a structural category of risk, not as a concrete proposal.
- **Did it misstate dormant code as either inherently defective or inherently acceptable?** No — §6 states both the honest cost (dead-code risk, real and disclosed) and the honest justification (documented, tested, explicitly anticipated by Implementation Plan §7) without collapsing either into a blanket verdict.
- **Is the final recommendation actually supported by the matrix and risk analysis?** Yes — see §14: the recommendation follows §7's own concrete finding that Option B is not safely adoptable *as currently built*, and follows §10's own risk ranking that the highest-severity risk table entry is specifically "incomplete Option-B migration," which argues against adopting B now and for treating its precondition (a reconciled resource/denial model) as the trigger, rather than for freezing Option A as permanent or declaring Option B unconditionally correct in principle.

No genuine defect requiring correction was found in this self-audit.

---

## 14. Decision

```
RETAIN CURRENT STATE PENDING A DEFINED TRIGGER
```

Neither "retain dormant capability, permanently, as the final architecture" nor "adopt the mandatory gateway now" is supported by the evidence. Option A is correct *today* because it already is today's true, working, constitutionally valid state. Option B is not correct *now* because §7 and §8 demonstrate, concretely, that the existing decorator cannot safely absorb either existing writer's own gate without a resource-representation reconciliation and a denial-semantics redesign that do not exist yet — adopting it today, unmodified, would not add defence in depth; it would break production writes or force undocumented, unreviewed workarounds. This is a temporal answer, not a permanent endorsement of the distributed model: the moment the defined trigger below occurs, this review's own recommendation is that the mandatory-gateway question be reopened, not that it be foreclosed.

**The trigger:** the first of either of the following two events, whichever occurs first —

1. **A newly approved Memory Core write consumer that does not already bring its own inline permission gate.** This is Implementation Plan §14's own already-named gap ("a production caller for Memory Core's write path... not yet approved"). Such a consumer has no coordinator-owned convention to inherit, so it is the natural, lowest-cost point to require the shared gateway *and* to force the resource-representation question to be answered for the first time, without touching either already-accepted coordinator.
2. **Any proposal to modify either existing coordinator's own Memory Core dependency for an unrelated reason** (a refactor, a bug fix, a signature change forced by some other amendment) — at that point, the reconciliation work identified in §11 is already partially in motion, and folding the gateway migration into the same change avoids a second, separate migration later.

Until one of these occurs, Option A — dormant, documented, tested, unwired — remains the correct, lawful, and honest description of Memory Core's current write-path architecture.

---

## 15. Recommended Next Governance Step

Not drafted here, per this task's explicit constraint. If and when the trigger above occurs, the correct next step is a successor to `MEMORY_CORE_CONTRACT_DESIGN_ERRATA_004.md` proposing: (a) a single, canonical resource/action representation for every Memory Core permission check, reconciling the coordinators' own registered-`ResourceId` convention with the decorator's own `emptyList()` convention; and (b) an explicit denial-semantics decision for the gateway (retain the thrown exception and require every caller needing a non-exceptional outcome to catch it at its own boundary, or introduce a second, non-throwing gateway entry point). Per §12, above, this domain amendment should itself state, following CDR-005's own Decision Rules, whether the ambiguity §5/§7/§8 of this review already surfaced is contested enough to require escalation to a full CDR before the amendment may be treated as settled — this review's own view, stated plainly, is that it is.

---

## 16. Git Confirmations

- Nothing was staged during this review.
- Nothing was committed during this review.
- Nothing was pushed during this review.
- No Kotlin file was modified. No governance document was modified. No CDR or amendment was drafted.

## 17. Final Git Status

```
$ git status --short
?? docs/reviews/MEMORY_CORE_WRITE_GATEWAY_ARCHITECTURAL_DECISION_REVIEW.md
```

Only this review document is uncommitted. Nothing was staged, committed, or pushed at any point during this task.
