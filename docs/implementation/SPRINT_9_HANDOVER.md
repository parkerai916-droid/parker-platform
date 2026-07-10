# Sprint 9 Handover — Model-Backed ReasoningProvider

## Status

**Sprint 9 complete. Verified.** Android Studio verified: 578/578
passing (Human authority, PES-001), confirmed by Steven. BUILD
SUCCESSFUL. This document summarises exactly what Sprint 9 delivered,
against the verified repository state, and identifies (does not plan)
the single highest-priority remaining implementation unit for Sprint 10.
No architecture, ADR, or Constitution document is touched by this
document. No Kotlin is written here.

---

## 1. What Sprint 9 Delivered

Sprint 9 implemented `docs/implementation/MODEL_REASONING_PROVIDER_IMPLEMENTATION_PLAN.md`
in full, itself built on the accepted
`docs/implementation/reviews/ModelReasoningProvider_Implementation_Review.md`.
No architectural redesign occurred at any point in this Sprint.

**Four new production files, all additions, no existing file modified:**

| File | Contents |
| --- | --- |
| `src/runtime/ModelReasoningProvider.kt` | Concrete `ReasoningProvider` implementation. A pure orchestrator over three constructor-injected collaborators, with no `try`/`catch` anywhere in the class: `reason()` builds a prompt, calls the model inference seam under `withTimeout`, and parses the raw result, returning it unchanged. |
| `src/runtime/ReasoningPromptBuilder.kt` | `ReasoningPromptBuilder` (`fun interface`) and `DefaultReasoningPromptBuilder` — a deterministic template: already-assembled context entries (one per line), then the owner's message, then a fixed instruction naming the `GOAL:`/`REPLY:`/`NOACTION` convention. |
| `src/runtime/ModelInferenceClient.kt` | `ModelInferenceClient` (`fun interface`) and `LocalHttpModelInferenceClient` — a JDK `java.net.http.HttpClient`-based implementation; `endpointUrl`/`modelName` required, no default; cancellation wired through `suspendCancellableCoroutine`. Plus two named, overridable default formatting functions, `defaultOllamaRequestBody`/`defaultOllamaResponseBody`, using minimal, hand-rolled JSON string construction — no JSON library or other new Gradle dependency was added. |
| `src/runtime/ReasoningResponseParser.kt` | `ReasoningResponseParser` (`fun interface`) — the frozen architectural component — and `TaggedReasoningResponseParser`, its first, explicitly-replaceable default implementation, plus `UnclassifiableModelResponseException`. |

**Test additions (37 new test methods, net):** `tests/runtime/ModelReasoningProviderTest.kt`
(7), `ReasoningPromptBuilderTest.kt` (6), `ModelInferenceClientTest.kt`
(12), `ReasoningResponseParserTest.kt` (12), plus three lambda-based test
fakes with no tests of their own (`FakeModelInferenceClient.kt`,
`FakeReasoningPromptBuilder.kt`, `FakeReasoningResponseParser.kt`),
mirroring `FakeReasoningProvider`'s established precedent.

**Verified total:** 541 (Sprint 8, Local Text Channel Deliver Tool) + 37
= **578/578**, BUILD SUCCESSFUL, confirmed by Steven in Android Studio.
One test-only defect was found and corrected during verification: a
reflective constructor-shape test assumed exactly one JVM constructor,
which does not hold for a constructor with a default parameter value
(`timeoutMs`) — Kotlin emits an additional, synthetic
`DefaultConstructorMarker` constructor for such cases. The fix was
confined to the test (`declaredConstructors.single { !it.isSynthetic }`);
no production file was touched. Full detail in `IMPLEMENTATION_HISTORY.md`'s
own Sprint 9 entry.

**What this Sprint did not do**, restated from the Plan's own Excluded
Work, unchanged:

- No production caller was wired. `ConversationTurnReasoningCoordinator`
  and `CommunicationConversationCoordinator` are both unmodified —
  nothing in this repository yet constructs a real `ModelReasoningProvider`
  or hands it to either coordinator.
- No `IdentityService`, `PlannerRuntime`, `ExecutionPipeline`,
  `PermissionEngine`, `ToolRegistry`, `ToolInvocationBinding`,
  `MemoryStore`, `WorldModel`, `ModuleRegistry`, `ConversationEngine`,
  `ResponseDelivery`, or `ModelManager` dependency exists anywhere in
  this Sprint's code.
- `LocalHttpModelInferenceClient`'s own live HTTP path is not exercised
  by the automated test suite — no real model server exists in the
  sandbox this Sprint was implemented in (a disclosed limitation, not a
  defect).
- No architecture, Contract Design, or ADR document was modified.

**Gap closed:** `docs/architecture/IMPLEMENTATION_GAPS.md` #53's "no
concrete, model-backed `ReasoningProvider` implementation exists" item is
now resolved — the implementation exists, is tested, and is verified.
Gap #53 itself **remains Open** — see Section 2.

---

## 2. Current State of `IMPLEMENTATION_GAPS.md` #53

Restated verbatim from the gap's own current text, the items still
preventing closure:

1. Constructing an `OutboundParkerResponse` from a
   `ReasoningProviderResponse.Reply` — the wiring that would actually
   call `ResponseDelivery` in response to a real conversation turn —
   remains unimplemented.
2. The `Goal` / Planner Runtime routing path remains entirely
   unimplemented.
3. No production composition root exists to register the Local Text
   Channel module, its deliver Tool, the `NOTIFY` vocabulary entry, or
   now a real `ModelReasoningProvider` (with a real
   `LocalHttpModelInferenceClient` endpoint) at real startup.
4. `ReasoningContext` assembly ownership remains unassigned
   (`REASONING_PROVIDER_CONTRACT_DESIGN.md` Section 9's own disclosed
   open item).
5. `LocalHttpModelInferenceClient`'s own live HTTP path is not exercised
   by the automated test suite — no real model server exists in this
   sandbox.

---

## 3. Sprint 10 Candidate Analysis

**This section identifies a candidate. It does not plan, scope, or
authorise one.** No Implementation Plan, Contract Design, or Scope Lock
is produced here; that remains a separate, future step.

### Other open gaps surveyed

`IMPLEMENTATION_GAPS.md` carries several open items besides #53. Briefly,
for comparison: #44 (`ExecutionPipeline.cancel` cannot interrupt an
in-flight `Tool.execute()`), #45 (no `planner.session_rejected` event for
a reachable `SUBMITTED -> REJECTED` transition), #46
(`DefaultMemoryPromotionPolicy` implements 2 of 6 named promotion
factors), #47 (`InMemoryWorldModel` does not publish state change
events), #48 (deterministic parent-derived IDs cap Agent Run/Task
Proposal multiplicity — formally constrained, not a defect), #50
(`EventBus` publish is synchronous — logged, deferred scale issue), #51
(persistence/durability/audit boundary undefined — logged, strategic
architecture gap), and #52 (Module Registry's Tool/Resource wiring rests
on disclosed interpretive choices; Permission Engine gating of its own
lifecycle operations is deferred). Each is real and open, but each also
sits in a different subsystem than the one Sprints 7–9 have been
building continuity in, and several (#50, #51) are explicitly logged as
strategic/deferred rather than immediately actionable implementation
units.

### Recommended single unit: close gap #53 item 1 — Reply → `OutboundParkerResponse` → `ResponseDelivery` wiring

**Why this one, over the alternatives above:**

- **Every dependency it needs already exists, is implemented, and is
  tested.** `ConversationTurnReasoningCoordinator`/`CommunicationConversationCoordinator`
  already produce a `ReasoningProviderResponse` from a real inbound
  message; `ResponseDelivery` already delivers an `OutboundParkerResponse`
  end-to-end through a real, registered `LocalTextChannelDeliverTool`
  (Sprint 8) and now has a real `ReasoningProvider` behind it (Sprint 9).
  This unit is the one remaining piece that connects two already-verified
  ends of the same pipeline — no other open gap is this close to
  immediately implementable.
- **It is the narrowest of the four remaining #53 items.** Unlike item 2
  (`Goal`/Planner Runtime routing, which has no Planner-facing contract
  for this purpose yet), item 3 (a production composition root, which is
  cross-cutting across every subsystem registered so far, not one
  focused unit), or item 4 (`ReasoningContext` assembly ownership, which
  `REASONING_PROVIDER_CONTRACT_DESIGN.md` Section 9 leaves genuinely
  undecided and would likely need a Contract Design pass before an
  Implementation Plan, not only an implementation unit), item 1 has a
  single, already-typed input (`ReasoningProviderResponse.Reply`) and a
  single, already-implemented consumer (`ResponseDelivery.deliver`,
  taking `OutboundParkerResponse`) — the work is constructing one value
  from another and calling one existing method, not deciding new
  architecture.
- **It directly continues the Sprint 7–9 thread** (Unit C2 wired
  Communication → Conversation; Unit C4 built Response Delivery; Sprint 8
  gave it a real Tool; Sprint 9 gave it a real Reasoning Provider) rather
  than opening a new subsystem. Closing it is the most direct way to make
  the existing, already-verified pipeline actually reachable end-to-end
  from a real inbound message to a real delivered reply — every
  individual piece already proven to work in isolation, still not called
  in sequence by anything in production.
- **It stays inside this Sprint's own restricted boundaries.** It touches
  no Trust, Planner, Memory, World Model, or Execution Pipeline
  architecture — `ResponseDelivery` and `ConversationTurnReasoningCoordinator`
  are both already-approved, unmodified contracts this unit would only
  call, not redesign.

**Not recommended for Sprint 10, and why, briefly:** item 3 (composition
root) is better sequenced *after* item 1, since a composition root should
wire a complete, already-connected pipeline rather than a partially-wired
one; item 2 (`Goal`/Planner routing) and item 4 (`ReasoningContext`
assembly) each plausibly require a Contract Design or architecture
decision before an implementation unit can be scoped, which is a larger,
different kind of next step than "the single highest-priority remaining
*implementation* unit" asks for.

**This is a recommendation for Steven's own sequencing decision, not a
Scope Lock, not an Implementation Plan, and not an authorisation to
begin.** Per `PARKER_ENGINEERING_STANDARD.md`, Sprint 10 begins only when
Steven says so.
