# Local Text Channel Deliver Tool — Implementation Plan

## Status

**Stage 3 Implementation Plan, PES-001. Not yet Scope Locked.** No Kotlin
is written by this document. This Plan implements only what has already
been authorised by:

1. `docs/architecture/LOCAL_TEXT_CHANNEL_CONTRACT_DESIGN.md` (Accepted,
   Stage 2A, as revised — `toolsExposed` now names one "deliver"
   `ToolDescriptor`, sourced from `RESPONSE_DELIVERY_CONTRACT_DESIGN.md`
   Section 4).
2. `docs/architecture/RESPONSE_DELIVERY_CONTRACT_DESIGN.md` (Accepted,
   Stage 2A) Section 4 — the Tool's exact shape: `supportedActions`
   including the `PermissionAction` its `proposedActions` string maps to,
   `supportedResourceTypes` including `ResourceType.TOOL`, and a
   `Tool.execute` implementation that reads response text from
   `request.metadata` and performs the channel-specific act of showing it
   to the owner.
3. `docs/architecture/ADR-025_RESPONSE_DELIVERY_CONTENT_CARRIER.md`
   (Accepted) — `ExecutionRequest.metadata` carries response text; the
   exact key is `RESPONSE_TEXT_METADATA_KEY = "response.text"`, already a
   public constant in `src/runtime/ResponseDelivery.kt`, reused unchanged
   here, not redefined.
4. `docs/architecture/ADR-026_MODULE_RESOURCE_OWNERSHIP_CONVENTION.md`
   (Accepted) — `Resource.ownerPrincipalId = PrincipalId(moduleId.value)`,
   already implemented by `InMemoryModuleRegistry`, unmodified by this
   Plan.
5. `docs/implementation/RESPONSE_DELIVERY_NOTIFY_VOCABULARY_DECISION.md`
   — the one `ActionVocabularyEntry` this Plan registers, exact shape
   already fixed: `ActionVocabularyEntry(verbPhrase = "notify owner",
   mappings = setOf(ActionResourceMapping(PermissionAction.NOTIFY,
   ResourceType.TOOL)))`.
6. `docs/implementation/RESPONSE_DELIVERY_IMPLEMENTATION_PLAN.md` — the
   already-implemented `ResponseDelivery` (Sprint 7, Unit C4) this Plan's
   own end-to-end test drives through to the real Tool, and the precedent
   this Plan follows for where an as-yet-unregistered vocabulary entry is
   registered (Required Analysis Item 8 of that Plan; Decision 9, below).

**This Plan redesigns no approved architecture, introduces no new public
interface, and drafts no ADR.** The one new type it schedules —
`LocalTextChannelDeliverTool` — is a concrete implementation of the
already-existing `Tool` interface (`src/interfaces/Tool.kt`), not a new
interface, mirroring `ResponseDelivery`'s own precedent of adding a
concrete class where Contract Design names no seam requiring one.

**One factual clarification, confirmed by direct reading of
`src/interfaces/Tool.kt`, stated here rather than silently assumed
either way:** `Tool.execute(request: ExecutionRequest)` returns
`ToolResult`, not `ExecutionResult`. `ExecutionResult` is constructed by
`DefaultExecutionPipeline.executeResolvedTool` from the `ToolResult` a
Tool returns — no Tool implementation in this codebase constructs an
`ExecutionResult` directly, and this Plan does not change that shape.
"Returning `ExecutionResult`," as this task's own Included Work names it,
is therefore satisfied indirectly: this Unit's `execute()` returns the
correct `ToolResult`, and the existing, unmodified pipeline turns it into
the `ExecutionResult` the caller (`ResponseDelivery`, and beyond it,
`ResponseDelivery`'s own caller) ultimately observes. This is not a
redesign of `Tool` or `ExecutionPipeline` — it is what already happens
today for every other Tool in this repository.

**Grounded exclusively in the as-built code and the documents that
authorise it:** `src/interfaces/Tool.kt`, `src/contracts/ToolDescriptor.kt`
(`ToolDescriptor`, `ValidationResult`), `src/contracts/ExecutionResult.kt`
(`ToolResult`, `ExecutionResult`), `src/contracts/Module.kt`
(`ModuleDescriptor`, `ModulePermissionRequirement`,
`ModuleConnectivityDeclaration`), `src/contracts/Permission.kt`
(`PermissionAction.NOTIFY`), `src/interfaces/ResourceRegistry.kt`,
`src/interfaces/ToolRegistry.kt`, `src/contracts/ToolInvocationBinding.kt`,
`src/runtime/InMemoryModuleRegistry.kt`, `src/runtime/InMemoryToolInvocationBinding.kt`,
`src/runtime/DefaultExecutionPipeline.kt`, `src/runtime/ActionMapper.kt`
(`ActionVocabulary`, `InMemoryActionVocabulary`), `src/runtime/ResponseDelivery.kt`
(`RESPONSE_TEXT_METADATA_KEY`, `deliver`), `src/interfaces/LocalTextChannel.kt`,
`src/runtime/DefaultLocalTextChannel.kt`, and the established test
convention `ModuleId("channel.local-text")` already used, unchanged, by
`tests/runtime/ResponseDeliveryTest.kt`, `tests/runtime/CommunicationConversationCoordinatorTest.kt`,
`tests/runtime/DefaultLocalTextChannelTest.kt`, and others.

---

## 1. Executive Summary

This Plan implements the concrete "deliver" Tool
`LOCAL_TEXT_CHANNEL_CONTRACT_DESIGN.md`'s revised `toolsExposed` names: a
small, concrete class, `LocalTextChannelDeliverTool`, implementing `Tool`.
Its `execute` reads response text out of `ExecutionRequest.metadata`
under the already-established `RESPONSE_TEXT_METADATA_KEY`, passes it
through unchanged (no formatting, trimming, or mutation) to an injected,
test-visible owner-notification callback — its one and only side effect
in this first implementation — and returns a `ToolResult` the existing,
unmodified `DefaultExecutionPipeline` turns into an `ExecutionResult`.
The Tool's backing `Resource` is registered with `ownerPrincipalId =
PrincipalId(channelModuleId.value)`, per `ADR-026`, automatically, by the
existing `ModuleRegistry.register`. The Tool becomes actually invocable
through the existing `ToolInvocationBinding.bind`. The already-fixed
`NOTIFY` vocabulary entry is registered at test level, since no
production composition root exists. No Android display, no real
human-visible rendering, and no production wiring are built or implied —
this first implementation is a test-visible, local-runtime deliver Tool
only, and this document does not claim otherwise anywhere. No existing
file is modified. No new interface, ADR, or architecture document is
introduced.

## 2. Scope

The field-level shape of the one Tool this Plan implements, and the
test-level wiring that proves `ResponseDelivery` can reach it through the
real `ExecutionPipeline`. Nothing else. This Plan does not schedule a
caller of `ResponseDelivery`, a production composition root, or any
component named in Section 4 (Excluded Work).

## 3. Included Work

- **The concrete `LocalTextChannelDeliverTool` class**, implementing
  `Tool` (`descriptor`, `validate`, `execute`) per
  `RESPONSE_DELIVERY_CONTRACT_DESIGN.md` Section 4.
- **Its `ToolDescriptor`** — `toolId`, `displayName`, `description`,
  `version`, `supportedActions`, `supportedResourceTypes` (Decisions 1–4,
  6, below).
- **`Tool.execute(request: ExecutionRequest)`** — reads response text
  from `request.metadata[RESPONSE_TEXT_METADATA_KEY]` (Decision 5),
  passes it through unchanged (Decision 7), invokes the injected
  owner-notification callback (Decision 8), and returns a `ToolResult`
  that the existing `DefaultExecutionPipeline` turns into the
  `ExecutionResult` the caller observes (Status, above).
- **Reading response text from `request.metadata[RESPONSE_TEXT_METADATA_KEY]`**
  — the existing constant in `src/runtime/ResponseDelivery.kt`, reused
  unchanged, not redefined.
- **Exposing the Tool through Local Text Channel registration** — one
  call to the existing, unmodified `ModuleRegistry.register(descriptor)`,
  where `descriptor.toolsExposed` contains this Tool's `ToolDescriptor`.
- **Ensuring the backing Resource is registered with `ownerPrincipalId =
  PrincipalId(channelModuleId.value)`** — automatic, by
  `InMemoryModuleRegistry.register`'s own existing, unmodified logic
  (`moduleToolResource`), per `ADR-026`. This Plan does not call
  `ResourceRegistry.register` directly; it verifies the convention holds
  by inspecting the registered Resource in its own tests (Section 8).
- **Registering/wiring the existing `NOTIFY` `ActionVocabularyEntry`
  where required for tests** — Decision 9, below.

## 4. Excluded Work

Restating this task's own Out of Scope list, each grounded in why it is
excluded, not merely named:

- **Android UI, actual display rendering.** No such dependency or API is
  introduced. The owner-notification callback (Decision 8) is a plain
  function type, not a UI or console binding.
- **Speech.** No text-to-speech, speech-to-text, or audio dependency
  exists anywhere this Unit's code can reach.
- **Notifications outside the returned `ExecutionResult`.** The only
  observable outputs of a call are the `ToolResult`/`ExecutionResult`
  this Unit's own `execute()` produces and the one owner-notification
  callback invocation (Decision 8) — no OS-level notification, push, or
  alert is created.
- **Persistence.** Nothing this Unit produces is written to disk, a
  database, or any durable store.
- **`EventBus` publication.** No new publication or subscription is
  introduced; `DefaultExecutionPipeline`'s own existing `execution.*`
  lifecycle events already cover observability for the `ExecutionRequest`
  this Tool is invoked against, unchanged by this Unit.
- **Planner, Conversation Engine, Reasoning Provider, model-backed LLMs,
  Memory, World Model.** No reference to `PlannerRuntime`,
  `ConversationEngine`, `ReasoningProvider`, any LLM/model dependency,
  `MemoryStore`, or `WorldModel` exists anywhere this Unit's code can
  reach.
- **Retry policy, queueing, streaming.** `execute()` is called at most
  once per `ExecutionRequest`; no retry, queue, or streaming behaviour is
  introduced.
- **Multiple recipients, multi-channel fan-out.** Exactly one Tool,
  backing exactly one channel's one Resource, is registered; no dispatch
  to more than one recipient or channel is introduced.
- **Production composition root.** Registration (Decision 9) happens
  inside this Unit's own tests only; wiring a real Parker instance to
  register this Tool at startup is a future, separately-scoped unit's
  responsibility, named but not performed here.

## 5. Dependencies

**Already satisfied, reused unchanged — no modification to any of the
following:**

- `Tool`, `ToolDescriptor`, `ValidationResult`, `ToolResult`,
  `ExecutionResult` (`src/interfaces/Tool.kt`, `src/contracts/ToolDescriptor.kt`,
  `src/contracts/ExecutionResult.kt`).
- `ModuleDescriptor`, `ModulePermissionRequirement`,
  `ModuleConnectivityDeclaration`, `ModuleId`, `ModuleLifecycleTransitions`
  (`src/contracts/Module.kt`).
- `ModuleRegistry` / `InMemoryModuleRegistry` (Sprint 6, Unit M1) — the
  sole registration path (Section 3).
- `ResourceRegistry` / `InMemoryResourceRegistry` — invoked automatically
  by `ModuleRegistry.register`, not called directly by this Unit's own
  code.
- `ToolRegistry` / `InMemoryToolRegistry` — likewise invoked automatically
  by `ModuleRegistry.register`.
- `ToolInvocationBinding` / `InMemoryToolInvocationBinding` — the one
  additional call this Unit's own test code makes directly (Decision 9).
- `ActionVocabulary` / `InMemoryActionVocabulary`, `ActionMapper`
  (`src/runtime/ActionMapper.kt`) — the `NOTIFY` entry registration and
  the mapping step `DefaultExecutionPipeline.submit` already performs.
- `ExecutionPipeline` / `DefaultExecutionPipeline` — the sole invocation
  path from a submitted `ExecutionRequest` to this Unit's own
  `Tool.execute` (via `ToolInvocationBinding.invocableFor`), unmodified.
- `ResponseDelivery`, `RESPONSE_TEXT_METADATA_KEY` (`src/runtime/ResponseDelivery.kt`,
  Sprint 7 Unit C4) — the metadata key this Unit's `execute()` reads, and
  the component this Unit's end-to-end test drives the real Tool through
  (Decision 10).
- `PermissionEngine` test configuration already established by
  `tests/runtime/ResponseDeliveryTest.kt`'s own end-to-end test —
  reused for this Unit's own end-to-end test, not reinvented.
  `DefaultExecutionPipelineTest.kt`'s own baseline configures
  `FakePermissionEngine` to approve a `READ`/`CALENDAR` request, a
  different action/resource pair; `ResponseDeliveryTest.kt`'s end-to-end
  test already configures `FakePermissionEngine` to approve the exact
  `NOTIFY`/`TOOL` pair this Unit also needs, confirmed by direct reading
  of both files.

**Not a dependency of this Unit:** any production composition root; a
real owner-visible display mechanism (Android, console, notification);
`ConversationEngine`, `ReasoningProvider`, `PlannerRuntime`, or any
coordinator that constructs an `OutboundParkerResponse` and calls
`ResponseDelivery` for real (`RESPONSE_DELIVERY_CONTRACT_DESIGN.md`
Section 8, Deferred Item 3 — still open, untouched by this Plan).

## 6. Required Implementation Decisions

Numbered to match this task's own ten required decisions, plus one
additional decision (11) this Plan's own scope makes unavoidable
(`moduleId`). Each is a genuine interpretive fork not resolved by any
authorising document, named here with a proposed, conservative default,
awaiting confirmation before Kotlin proceeds.

### Decision 1 — Exact `toolId` value

**`"deliver"`.** No authorising document proposes an alternative string.
`"deliver"` is adopted because it is the verb this entire document set
already uses for this exact concept (`COMMUNICATION_CONTRACT_DESIGN.md`
Section 7: "an ordinary 'deliver' `ToolDescriptor`";
`RESPONSE_DELIVERY_CONTRACT_DESIGN.md` Section 4's own heading,
"Interaction with the Local Text Channel Deliver Tool").

### Decision 2 — Exact Tool name/description

**`displayName = "Local Text Channel Deliver Tool"`, `description =
"Delivers an already-authorised OutboundParkerResponse's text to the
Local Text Channel's owner."`** Neither field is fixed by any authorising
document. `displayName` must be non-blank (`ToolDescriptor`'s own
constructor validation); `description` carries no validation but is a
required constructor parameter with no default — a value must be
supplied. Both proposed values state what this Tool does, in the same
plain, factual register `ResponseDelivery`'s own KDoc already uses,
inventing no capability beyond what Section 3 (Included Work) describes.

### Decision 3 — Exact supported actions

**`supportedActions = setOf(PermissionAction.NOTIFY)`.** Fixed, not
proposed: `RESPONSE_DELIVERY_CONTRACT_DESIGN.md` Section 4 names
`PermissionAction.NOTIFY` as the action a deliver Tool's descriptor must
support, matching `RESPONSE_DELIVERY_NOTIFY_VOCABULARY_DECISION.md`'s
own already-fixed vocabulary entry
(`ActionVocabularyEntry(verbPhrase = "notify owner", mappings =
setOf(ActionResourceMapping(PermissionAction.NOTIFY, ResourceType.TOOL)))`).
No alternative value is consistent with that entry — a different
declaration here would make the Tool unreachable through
`ActionMapper`/`ToolRegistry.resolve`, not merely non-conforming.

### Decision 4 — Exact supported resource types

**`supportedResourceTypes = setOf(ResourceType.TOOL)`.** Fixed, not
proposed: matches this Tool's own backing Resource's type under
`ADR-026`'s convention (`ResourceType.TOOL`, the type every
module-exposed Tool's backing Resource already carries, per
`InMemoryModuleRegistry.moduleToolResource`), and matches the same
`NOTIFY` vocabulary entry cited in Decision 3.

### Decision 5 — How missing response text in metadata is handled

**`Tool.validate(request)` returns `ValidationResult.Invalid(listOf(
"request.metadata[\"$RESPONSE_TEXT_METADATA_KEY\"] is missing"))` if the
key is absent from `request.metadata`.** No authorising document
specifies this rule. `DefaultExecutionPipeline.executeResolvedTool`
already calls `Tool.validate` before `Tool.execute`, and short-circuits
to a `FAILED` `ExecutionResult` (using `validation.reasons` as `errors`)
without ever calling `execute()` if validation fails — confirmed by
direct reading of `DefaultExecutionPipeline.kt`. `execute()` is therefore
never called with a missing key in practice; it is not additionally
defended inside `execute()` itself, since duplicating a check
`validate()` already performs would be redundant, unauthorised logic —
the same reasoning `LOCAL_TEXT_CHANNEL_CONTRACT_DESIGN.md`'s own
Minimalism Review already applied elsewhere ("a second, local copy would
be redundant... and a second place for that check to drift out of sync
with the first").

### Decision 6 — Whether blank response text is allowed or rejected

**Rejected.** `Tool.validate(request)` returns `ValidationResult.Invalid`
if `request.metadata[RESPONSE_TEXT_METADATA_KEY]` is present but blank,
using the identical reasoning and mechanism as Decision 5 (one combined
check: key absent, or present-but-blank, both produce `Invalid`). No
authorising document specifies this either way; rejecting blank text is
the conservative default, consistent with `OutboundParkerResponse.text`'s
own existing non-blank constructor requirement
(`COMMUNICATION_CONTRACT_DESIGN.md` Section 3) — a channel this deliver
Tool exists to serve never legitimately produces a response with no
content, so accepting one silently here would mask an upstream defect
rather than surface it.

### Decision 7 — Formatting, trimming, normalisation, or mutation

**None. The Tool passes `request.metadata[RESPONSE_TEXT_METADATA_KEY]`
through to the owner-notification callback exactly as read — no trim,
case-change, truncation, or reformatting of any kind.** No authorising
document proposes any transformation, and `RESPONSE_DELIVERY_CONTRACT_DESIGN.md`
Section 10's own "Response pass-through" invariant (governing
`ResponseDelivery` itself) establishes the precedent this Tool mirrors
one layer further downstream: the text a caller authorised is the exact
text the owner is shown, never a reinterpreted version of it.

### Decision 8 — Side effects beyond the returned result

**Exactly one: invoking an injected `suspend (text: String) -> Unit`
callback, once per successful `execute()`, with the exact text read
(Decision 7).** This is the Tool's entire side-effect surface in this
first implementation — no file write, network call, `EventBus`
publication, or other observable effect exists. No authorising document
specifies a real display mechanism —
`RESPONSE_DELIVERY_CONTRACT_DESIGN.md` Section 4 names "performing the
channel-specific act of showing it to the owner" as "a Local Text
Channel implementation detail entirely outside this document's own
scope," and this task's own instructions exclude Android UI, speech, and
actual display rendering. A constructor-injected callback is the
smallest mechanism consistent with that boundary: not a new interface (a
function type, mirroring the "decision seam" shape PES-001 Chapter 7.2
and `ARCHITECTURE_V2_FROZEN_BASELINE.md` Section 5 already establish for
injected, replaceable behaviour), and directly test-observable — this
Unit's own tests assert the callback received the exact text, exactly
once. **This Plan makes no claim that a response is shown to a human
being.** It is shown to whatever the test or future caller supplies as
the callback — in this Unit's own tests, a capturing lambda; a real,
human-visible display mechanism is a future, separately-scoped unit's
responsibility (Section 4, Excluded Work), not built or simulated here.

### Decision 9 — Binding to `ToolRegistry` / `ToolInvocationBinding`

**Two calls, both to already-existing, unmodified methods, both made
from this Unit's own test code, not from any `src/` production file:**
`ModuleRegistry.register(descriptor)` (registers the `ToolDescriptor`
with `ToolRegistry` and the backing `Resource` with `ResourceRegistry`,
automatically, per `InMemoryModuleRegistry`'s own existing logic), then
`ModuleRegistry.enable(moduleId, requestingPrincipalId)` (so the Tool's
`ToolLifecycleState` reaches `ENABLED` — `ToolRegistry.resolve` only
matches `ENABLED` Tools, confirmed by `tool-registry.md`'s "Lookup
Process" and `InMemoryToolRegistry`'s own implementation), then
`ToolInvocationBinding.bind(descriptor, toolInstance)` (the one step
`ModuleRegistry.register` does not itself perform — confirmed by direct
reading of `InMemoryModuleRegistry.kt`, which never references
`ToolInvocationBinding`). Mirrors `RESPONSE_DELIVERY_IMPLEMENTATION_PLAN.md`'s
own identical decision for the `NOTIFY` vocabulary entry (that Plan's
Required Analysis Item 8): no composition root exists in this repository
today (no `fun main(` under `src/`) to make any of these three calls at
production startup — a future, real composition root must perform the
identical calls once, named here so it is not silently forgotten when
one is eventually built.

**`ToolDescriptor` ownership rule (Stage 3 review correction).**
`LocalTextChannelDeliverTool` owns the single canonical `ToolDescriptor`
for this Unit, exposed through its own `override val descriptor`
(Decisions 1–4 fix its field values). `InMemoryToolInvocationBinding.bind`
requires `tool.descriptor == descriptor` exactly — confirmed by direct
reading of `src/runtime/InMemoryToolInvocationBinding.kt` — so every
subsequent use of this Tool's descriptor (the entry placed in
`ModuleDescriptor.toolsExposed`, the second argument to
`ToolInvocationBinding.bind`, and any test fixture that needs to refer to
it) must obtain it directly from `tool.descriptor`, never re-declare it
as a separate `ToolDescriptor(...)` literal. No duplicate `ToolDescriptor`
literal is to exist anywhere in this Unit's code or tests.

### Decision 10 — How tests prove `ResponseDelivery` can reach the real Tool through `ExecutionPipeline`

**One end-to-end test (Section 8), assembling real, non-fake
implementations of every collaborator** — `InMemoryResourceRegistry`,
`InMemoryToolRegistry`, `InMemoryToolInvocationBinding`,
`InMemoryModuleRegistry`, `InMemoryActionVocabulary` (with the `NOTIFY`
entry registered, Decision 9), `ActionMapper`, `DefaultExecutionPipeline`,
and the existing, unmodified `ResponseDelivery` — registering this Unit's
real `LocalTextChannelDeliverTool` through the calls named in Decision 9,
then calling `ResponseDelivery(resourceRegistry, executionPipeline).deliver(response)`
with a real `OutboundParkerResponse` and asserting `GatedOutcome.Produced`
with `ExecutionResultStatus.SUCCESS`, plus the injected callback (Decision
8) having received the response's exact text. This is the same pattern
`ResponseDeliveryTest.kt`'s own end-to-end test already used with a
throwaway placeholder Tool — this Unit's test replaces that placeholder
with the real one, proving the full, real stack coheres, not only that
each piece works in isolation.

### Decision 11 — `moduleId` reused, not invented

**`ModuleId("channel.local-text")`.** `LOCAL_TEXT_CHANNEL_CONTRACT_DESIGN.md`
Section 4 leaves this channel's exact `moduleId` string an
implementation-time choice, not an architectural constraint. This Plan
does not invent a new value: `ModuleId("channel.local-text")` is already
the established test-level identity for this exact channel, reused
unchanged across `tests/runtime/ResponseDeliveryTest.kt`,
`tests/runtime/DefaultLocalTextChannelTest.kt`,
`tests/runtime/CommunicationConversationCoordinatorTest.kt`,
`tests/runtime/ConversationTurnReasoningCoordinatorTest.kt`, and
`tests/runtime/InMemoryConversationEngineTest.kt`. Reusing it keeps this
Unit's own registration test continuous with every existing Sprint 7
test that already refers to "the local text channel" by this exact
`ModuleId`, rather than introducing a second, parallel identity for the
same conceptual channel.

## 7. Files Expected to Change

**All additions. No existing `src/` or `tests/` file requires
modification** — every dependency named in Section 5 is consumed exactly
as it exists today.

| File | New/Modified | Contents |
| --- | --- | --- |
| `src/runtime/LocalTextChannelDeliverTool.kt` | New | The `LocalTextChannelDeliverTool` class: implements `Tool`; constructor takes the injected owner-notification callback (Decision 8); `descriptor` fixed per Decisions 1–4; `validate` per Decisions 5–6; `execute` reads `RESPONSE_TEXT_METADATA_KEY`, passes it through unchanged (Decision 7), invokes the callback, returns a `ToolResult`. |
| `tests/runtime/LocalTextChannelDeliverToolTest.kt` | New | Isolated `validate`/`execute` tests (Section 8), plus the end-to-end test (Decision 10) proving real registration, binding, vocabulary resolution, and delivery through `ResponseDelivery` and `DefaultExecutionPipeline` together. |

## 8. Testing Strategy

**`LocalTextChannelDeliverToolTest.kt`, isolated:**

- **`descriptor` shape.** `toolId == "deliver"`, `displayName` and
  `description` non-blank, `supportedActions == setOf(PermissionAction.NOTIFY)`,
  `supportedResourceTypes == setOf(ResourceType.TOOL)` (Decisions 1–4).
- **`validate` — missing key.** `request.metadata` without
  `RESPONSE_TEXT_METADATA_KEY` produces `ValidationResult.Invalid` with a
  non-empty reason list (Decision 5).
- **`validate` — blank value.** `request.metadata[RESPONSE_TEXT_METADATA_KEY] = ""`
  produces `ValidationResult.Invalid` (Decision 6).
- **`validate` — present, non-blank value.** Produces `ValidationResult.Valid`.
- **`execute` — pass-through.** With a valid request, the injected
  callback receives the exact string from
  `request.metadata[RESPONSE_TEXT_METADATA_KEY]`, unchanged — no
  trimming or reformatting (Decision 7) — asserted by exact string
  equality, including deliberately-unusual input (leading/trailing
  whitespace, mixed case) to prove nothing is silently normalised.
- **`execute` — callback invoked exactly once.** Call-count assertion,
  mirroring `FakeExecutionPipeline`'s own call-recording precedent
  (Decision 8).
- **`execute` — return value.** Returns a `ToolResult` with
  `success == true` and `toolId == "deliver"` (Decision 8's own "no other
  side effect" scope — nothing else to assert).
- **Statelessness (structural).** `LocalTextChannelDeliverTool::class.java.declaredFields`
  contains exactly its one constructor-injected callback field and
  nothing else.

**End-to-end test (Decision 10), using real implementations — mirroring
`ResponseDeliveryTest.kt`'s own established end-to-end section:**

- Real `InMemoryResourceRegistry`, `InMemoryToolRegistry`,
  `InMemoryToolInvocationBinding`, `InMemoryModuleRegistry` (constructed
  from the first two), `InMemoryActionVocabulary` with the `NOTIFY` entry
  registered (Decision 9), `ActionMapper` wired to that vocabulary, and
  `DefaultExecutionPipeline` wired to all of the above plus a
  `FakePermissionEngine` configured to approve the `NOTIFY`/`TOOL` pair —
  the exact configuration `ResponseDeliveryTest.kt`'s own end-to-end test
  already uses, reused here, not reinvented (Section 5).
- Construct one `LocalTextChannelDeliverTool` with a capturing callback.
  Its `descriptor` property (Decision 9's ownership rule) is the single
  canonical `ToolDescriptor` for every step below — no separate
  `ToolDescriptor(...)` literal is constructed anywhere in this test.
- Construct this channel's `ModuleDescriptor` — `moduleId =
  ModuleId("channel.local-text")` (Decision 11), `toolsExposed =
  listOf(tool.descriptor)`, `requiredPermissions = emptyList()`
  (`LOCAL_TEXT_CHANNEL_CONTRACT_DESIGN.md` Section 4, as revised, leaves
  this value an open question this Plan does not resolve — see Section
  11, Completion Criteria), `connectivityDeclaration =
  ModuleConnectivityDeclaration.LOCAL_ONLY` (the Contract Design's own
  existing, unrevised value) — register, enable, and bind it (Decision
  9).
- Assert the registered backing `Resource`'s `ownerPrincipalId ==
  PrincipalId("channel.local-text")`, confirming `ADR-026`'s convention
  holds for this Tool specifically, not merely by inspection of
  `InMemoryModuleRegistry`'s own general-purpose test suite.
- Construct one `OutboundParkerResponse` with `channelId =
  ModuleId("channel.local-text")`.
- Call `ResponseDelivery(resourceRegistry, executionPipeline).deliver(response)`
  (the existing, unmodified `ResponseDelivery`) and assert:
  `GatedOutcome.Produced` with `ExecutionResultStatus.SUCCESS`; the
  capturing callback received the response's exact `text`, unchanged,
  exactly once.

**Full Gradle test suite.** Run the complete suite once implementation is
complete and report a real, Android-Studio-verified result. If the
sandbox used to prepare this repository cannot execute Gradle, report an
honest, arithmetic-projected total with an explicit "not verified"
disclosure and wait for external verification before any
`IMPLEMENTATION_HISTORY.md` update.

## 9. Acceptance Criteria

- `LocalTextChannelDeliverTool` exists, implements `Tool`, and its
  `descriptor` matches `RESPONSE_DELIVERY_CONTRACT_DESIGN.md` Section 4's
  shape exactly (Decisions 1–4).
- `validate()` rejects a request whose `metadata` lacks a non-blank
  `RESPONSE_TEXT_METADATA_KEY` entry, and accepts one that has it
  (Decisions 5–6) — verified structurally (Section 8).
- `execute()` passes the response text through unchanged and invokes the
  injected callback exactly once (Decisions 7–8) — verified structurally
  (Section 8).
- The Tool is reachable end-to-end: `ModuleRegistry.register` +
  `ModuleRegistry.enable` + `ToolInvocationBinding.bind` + the registered
  `NOTIFY` vocabulary entry together make `ExecutionPipeline.submit`
  resolve, validate, and execute this real Tool (Decisions 9–10).
- The backing `Resource`'s `ownerPrincipalId == PrincipalId(channelModuleId.value)`,
  verified directly, not merely assumed from `InMemoryModuleRegistry`'s
  own general behaviour.
- `ResponseDelivery.deliver` (unmodified) succeeds end-to-end against
  this real Tool, returning `GatedOutcome.Produced` with
  `ExecutionResultStatus.SUCCESS` (Decision 10).
- No existing `src/` or `tests/` file is modified (Section 7).
- No production composition root, real display mechanism, or caller of
  `ResponseDelivery` is introduced (Section 4).
- All tests listed in Section 8 pass, and the full Gradle suite passes
  (or a projected count is honestly reported, per Section 8's own
  disclosure discipline).

## 10. Architectural Traceability

| Implemented element | Authorised by |
| --- | --- |
| `LocalTextChannelDeliverTool` exists at all | `LOCAL_TEXT_CHANNEL_CONTRACT_DESIGN.md` Section 4 (revised, `toolsExposed`); `RESPONSE_DELIVERY_CONTRACT_DESIGN.md` Section 4 |
| `toolId = "deliver"` (Decision 1) | This document, Section 6, Decision 1 |
| `displayName`/`description` (Decision 2) | This document, Section 6, Decision 2 |
| `supportedActions = {NOTIFY}` (Decision 3) | `RESPONSE_DELIVERY_CONTRACT_DESIGN.md` Section 4; `RESPONSE_DELIVERY_NOTIFY_VOCABULARY_DECISION.md` |
| `supportedResourceTypes = {TOOL}` (Decision 4) | `RESPONSE_DELIVERY_CONTRACT_DESIGN.md` Section 4; `ADR-026_MODULE_RESOURCE_OWNERSHIP_CONVENTION.md` |
| Missing-metadata handling (Decision 5) | `src/interfaces/Tool.kt` ("Tools MUST validate before execution"); `src/runtime/DefaultExecutionPipeline.kt` (validate-before-execute short-circuit) |
| Blank-text rejection (Decision 6) | `COMMUNICATION_CONTRACT_DESIGN.md` Section 3 (`OutboundParkerResponse.text` non-blank precedent) |
| No formatting/mutation (Decision 7) | `RESPONSE_DELIVERY_CONTRACT_DESIGN.md` Section 10 ("Response pass-through" invariant) |
| Callback as sole side effect (Decision 8) | `RESPONSE_DELIVERY_CONTRACT_DESIGN.md` Section 4 ("a Local Text Channel implementation detail... outside this document's own scope"); this task's own Excluded Work |
| Registration/binding sequence (Decision 9) | `MODULE_CONTRACT_DESIGN.md` Section 5; `src/contracts/ToolInvocationBinding.kt`; `RESPONSE_DELIVERY_IMPLEMENTATION_PLAN.md` Required Analysis Item 8 (identical precedent) |
| End-to-end proof of reachability (Decision 10) | `RESPONSE_DELIVERY_IMPLEMENTATION_PLAN.md` Section 6 (identical end-to-end pattern) |
| `moduleId = ModuleId("channel.local-text")` (Decision 11) | `LOCAL_TEXT_CHANNEL_CONTRACT_DESIGN.md` Section 4 ("implementation-time choice"); established test convention |
| `RESPONSE_TEXT_METADATA_KEY` reused, not redefined | `ADR-025_RESPONSE_DELIVERY_CONTENT_CARRIER.md`; `src/runtime/ResponseDelivery.kt` |
| `ownerPrincipalId = PrincipalId(channelModuleId.value)` | `ADR-026_MODULE_RESOURCE_OWNERSHIP_CONVENTION.md`; `RESPONSE_DELIVERY_CONTRACT_DESIGN.md` Decision 2 |
| `NOTIFY` vocabulary entry, exact shape | `RESPONSE_DELIVERY_NOTIFY_VOCABULARY_DECISION.md` |
| No caller, no `OutboundParkerResponse` construction, no composition root | `RESPONSE_DELIVERY_CONTRACT_DESIGN.md` Section 8, Section 12; this document, Section 4 |

No file this Plan schedules introduces a concept none of the authorising
documents above already anticipated. No item in Section 4 (Excluded
Work) is scheduled, scoped, or partially implemented by this Unit.

## 11. Completion Criteria

- This Unit reaches its own Acceptance Criteria (Section 9).
- The full Android Studio test suite passes, with no existing test
  modified.
- `IMPLEMENTATION_HISTORY.md` records this Unit, only after verified
  tests — not performed by this Plan.
- `IMPLEMENTATION_GAPS.md` #53 may be further clarified (the outbound
  half now has a real, registered Tool, not only a tested mechanism with
  nowhere real to deliver to) but is not closed — the Reply-to-`OutboundParkerResponse`
  coordinator and a real model-backed `ReasoningProvider` remain open,
  unaffected by this Unit. Not performed by this Plan.
- `requiredPermissions` for this channel's `ModuleDescriptor` remains
  `emptyList()`, an explicitly open question `LOCAL_TEXT_CHANNEL_CONTRACT_DESIGN.md`
  Section 4 (as revised) already names and this Plan does not resolve —
  named again here so it is not silently forgotten at Completion.
- No architecture, Contract Design, or ADR document is modified at any
  point during this Unit's implementation.

## 12. Scope Lock

**Not yet locked. Do not begin implementation.** A separate, explicit
human instruction ("Scope Lock has been achieved" / "Scope Lock is
granted") is required before any Kotlin is written against this Plan,
per PES-001's Human-primary-authority model for Stage 3 through Stage 5.

**What becomes frozen once locked:** the file list in Section 7, the
dependency list in Section 5, the eleven Required Implementation
Decisions in Section 6 (as resolved — this document proposes
conservative defaults for each; Scope Lock should either confirm or
override them explicitly before Kotlin begins), the testing strategy in
Section 8, and the Excluded Work list in Section 4. Any change to any of
these after Scope Lock requires a new planning pass, not a silent
adjustment during implementation. If implementation reveals a
contradiction with this Plan or with any document it is grounded in,
implementation stops and the contradiction is reported — not resolved in
Kotlin.

## Related

- `docs/architecture/LOCAL_TEXT_CHANNEL_CONTRACT_DESIGN.md`
- `docs/architecture/RESPONSE_DELIVERY_CONTRACT_DESIGN.md`
- `docs/architecture/ADR-025_RESPONSE_DELIVERY_CONTENT_CARRIER.md`
- `docs/architecture/ADR-026_MODULE_RESOURCE_OWNERSHIP_CONVENTION.md`
- `docs/implementation/RESPONSE_DELIVERY_NOTIFY_VOCABULARY_DECISION.md`
- `docs/implementation/RESPONSE_DELIVERY_IMPLEMENTATION_PLAN.md`
- `docs/architecture/COMMUNICATION_CONTRACT_DESIGN.md`
- `docs/architecture/MODULE_CONTRACT_DESIGN.md`
- `src/interfaces/Tool.kt`, `src/contracts/ToolDescriptor.kt`
- `src/runtime/ResponseDelivery.kt`
- `src/runtime/InMemoryModuleRegistry.kt`, `src/runtime/InMemoryToolInvocationBinding.kt`
- `src/runtime/DefaultExecutionPipeline.kt`, `src/runtime/ActionMapper.kt`
- `tests/runtime/ResponseDeliveryTest.kt` (precedent for the end-to-end
  test in Section 8)
