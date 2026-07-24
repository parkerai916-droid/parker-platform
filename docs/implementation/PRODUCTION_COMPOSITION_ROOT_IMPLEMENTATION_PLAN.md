# Production Composition Root — Implementation Plan and Implementation Decisions

## Status

**Sprint 10, Unit 4.** Combined Implementation Plan and Implementation
Decisions document, per PES-001 Stage 3 / Stage 4. This Unit was
initiated directly by Steven, in a single instruction naming its own
scope, its own architectural constraints, and its own deliverables
(Implementation Plan, Implementation Review, Engineering Checkpoint,
Sprint Retrospective) explicitly — the normal separate Architecture
Review / Contract Design / Scope Lock stages that Sprint 10 Units 1–3
each went through individually are, for this Unit, compressed into this
one document plus the implementation it authorises, at Steven's own
direction. This is a disclosed process variation, not a silent skipping
of PES-001: Chapter 2's Human/AI authorship split is preserved (this
document, the Kotlin, and the tests are AI-authored; final Android Studio
verification and acceptance remain Steven's), and every substantive
question a Contract Design or Scope Lock would normally settle is
answered explicitly below, with its reasoning, before the Kotlin it
governs was written.

No architecture, ADR, or Constitution document is modified by this
document or by this Unit's implementation.

---

## 1. Governing Documents

In order of authority:

1. `docs/architecture/PARKER_ENGINEERING_STANDARD.md` (PES-001).
2. `docs/architecture/parker-constitution.md` — Trust Framework
   invariants, preserved without exception (Section 6, below).
3. `docs/architecture/COMMUNICATION_CONTRACT_DESIGN.md`,
   `docs/architecture/REASONING_PROVIDER_CONTRACT_DESIGN.md`,
   `docs/architecture/RESPONSE_DELIVERY_CONTRACT_DESIGN.md` — the three
   Contract Designs governing the components this Unit wires, all
   unmodified.
4. `docs/implementation/CONVERSATION_REPLY_COORDINATOR_SCOPE_LOCK.md` and
   `docs/implementation/REPLY_DELIVERY_COORDINATOR_SCOPE_LOCK.md` — each
   explicitly names "a production composition root" as work it does
   *not* authorise and defers to "a future, separately-scoped unit." This
   is that unit.
5. `docs/architecture/IMPLEMENTATION_GAPS.md` #53 — the gap this Unit
   narrows (Section 13, below).
6. `docs/implementation/SPRINT_9_HANDOVER.md` — identified the
   composition root as gap #53 item 3, recommending it be sequenced
   *after* the coordinator-wiring chain (items 1) was complete. That
   chain (Sprint 10, Units 1–3) is now complete and verified (612/612,
   confirmed by Steven) — this Unit follows it exactly as recommended,
   not out of sequence.
7. This document itself, as the direct source governing this Unit's
   implementation.

---

## 2. Included Work

- One new production source directory, `src/composition` (package
  `parker.composition`), and matching `tests/composition`, added to
  `build.gradle.kts`'s existing `sourceSets` block (Section 5, Decision 1).
- `ParkerRuntime`: the composition root itself — constructs, owns, and
  starts/stops a complete, real runtime graph, and exposes one production
  entry point, `submitOwnerMessage`, running an inbound message through
  the existing, unmodified conversation pipeline.
- `ParkerRuntimeConfig` / `ParkerRuntimeConfigLoader`: configuration
  loading from a caller-supplied environment map, with named, typed
  failures for missing/invalid values.
- `ParkerRuntimeException` (sealed): the composition root's own typed
  failure model for configuration, construction, startup, and shutdown
  faults.
- `ParkerRuntimeOutcome` / `PipelineStage`: the composition root's own
  per-message result type, distinct from `GatedOutcome`, adding exactly
  one thing `GatedOutcome` cannot represent — a genuine caught fault.
- `ParkerLogger` / `ConsoleParkerLogger`: a minimal, dependency-free,
  constructor-injected logging seam.
- `LoggingCommunicationIntake`, `LoggingReasoningProvider`,
  `RuntimeEventLogger`: three small, additive observability seams, each
  wrapping or subscribing to an existing interface, none modifying a
  frozen production file.
- `OwnerNotificationSink` / `LoggingOwnerNotificationSink`: the
  composition root's own supplied implementation of
  `LocalTextChannelDeliverTool`'s existing `onOwnerNotified` constructor
  parameter.
- Test suite: `tests/composition/*.kt` (Section 8, below).
- This document; the Implementation Review, Engineering Checkpoint, and
  Sprint Retrospective it precedes; `IMPLEMENTATION_GAPS.md` and
  `IMPLEMENTATION_HISTORY.md` updates, all performed only after
  implementation.

## 3. Excluded Work

Restated explicitly, matching the task's own architectural constraints:

- No redesign of any existing coordinator, contract, or interface.
- No new Trust/Permission architecture — `DefaultPermissionEngine` and
  `DefaultPermissionPolicy` are used exactly as they already exist; this
  Unit supplies policy *content* only (Section 7, Decision 1), which
  `IMPLEMENTATION_GAPS.md` #25 already names as a caller's decision to
  make, not a new mechanism.
- No `Goal` / Planner Runtime routing. A `Goal` response is returned as
  `ParkerRuntimeOutcome.NotAccepted`, exactly as
  `ReplyDeliveryCoordinator`/`ResponseComposer` already produce for it —
  untouched, unrouted, unchanged.
- No `ReasoningContext` assembly. `submitOwnerMessage` accepts one as a
  parameter (defaulting to `ReasoningContext(emptyList())`); this Unit
  does not decide or implement assembly ownership, which
  `REASONING_PROVIDER_CONTRACT_DESIGN.md` Section 9 leaves genuinely
  open.
- No service locator, no singleton, no static/global lookup anywhere in
  `src/composition` (Section 7, Decision 6 — the task's own explicit,
  additional instruction).
- No modification of any existing `src/` or `tests/` file, with one
  necessary, minimal exception: `build.gradle.kts`'s `sourceSets` block
  (Section 5, Decision 1).
- No real model server, no real Android/UI channel, no persistence layer.

## 4. Dependencies

Every dependency `ParkerRuntime` constructs is a real, already-implemented,
already-tested, unmodified production class:
`InMemoryResourceRegistry`, `InMemoryActionVocabulary`, `ActionMapper`,
`InMemoryToolRegistry`, `InMemoryModuleRegistry`, `InMemoryToolInvocationBinding`,
`InMemoryEventBus`, `InMemoryIdentityService`, `DefaultPermissionPolicy`,
`DefaultPermissionEngine`, `DefaultExecutionPipeline`,
`LocalTextChannelDeliverTool`, `InMemoryCommunicationIntake`,
`InMemoryConversationEngine`, `DefaultReasoningPromptBuilder`,
`LocalHttpModelInferenceClient`, `TaggedReasoningResponseParser`,
`ModelReasoningProvider`, `ConversationTurnReasoningCoordinator`,
`CommunicationConversationCoordinator`, `ResponseComposer`,
`ResponseDelivery`, `ReplyDeliveryCoordinator`, `ConversationReplyCoordinator`.

No dependency of `ParkerRuntime` is itself modified by this Unit.

---

## 5. Implementation Decisions

### Decision 1 — A new `src/composition` source directory, not `src/runtime`

The composition root is added under a new `src/composition` directory
(package `parker.composition`), not alongside the coordinators in
`src/runtime` (package `parker.core.runtime`). **Reasoning:** the task's
own framing draws a sharp line between "runtime components" (which the
composition root only wires) and the composition root itself, which
"holds no conversation-domain responsibility." Every existing Scope Lock
this Unit builds on (`CONVERSATION_REPLY_COORDINATOR_SCOPE_LOCK.md`
Section 15, `REPLY_DELIVERY_COORDINATOR_SCOPE_LOCK.md` Section 9) already
lists "production composition root" as something none of them are and
none of them do — mirroring that same separation at the package level
makes it structurally, not just documentarily, true that this new class
is not one more coordinator. This required one, minimal,
non-domain-behavioural change to `build.gradle.kts`'s `sourceSets` block
— the same kind of change already made twice before for identical
reasons (`EventBus.kt` returning to the build, `src/runtime` itself being
introduced in Phase 2), each recorded in `IMPLEMENTATION_GAPS.md`'s own
`build.gradle.kts` comment, which this Unit extends rather than
replaces.

### Decision 2 — The one real Permission Policy content decision

`DefaultPermissionPolicy` requires a caller-supplied
`List<PermissionPolicyRule>`. `IMPLEMENTATION_GAPS.md` #25 states plainly
that policy *content* "remains something a caller decides" — this
composition root is the first real caller, and therefore the first
component in this repository required to supply one. **Decision:**
supply exactly one rule — `NOTIFY` on `TOOL` → `APPROVED`/`AUTOMATIC` —
mirroring the identical decision already made, as a test-only stand-in,
by `ResponseDeliveryTest.kt`'s and `LocalTextChannelDeliverToolTest.kt`'s
own end-to-end tests (`FakePermissionEngine` approving exactly this pair).
This is the minimum required for the one Tool this runtime registers to
be reachable at all. No other action/resource-type pair is approved;
every other request `DefaultPermissionPolicy` evaluates is `DENIED` by
its own existing, unmodified conservative default. This is a narrow,
disclosed policy-content decision, not a new policy *mechanism* — the
mechanism (`DefaultPermissionPolicy` itself) is unmodified, frozen,
already-approved architecture.

### Decision 3 — Logging seams: decoration and `EventBus` subscription only, never modification

Three of the eight log-line examples the task names (`Conversation
accepted`, `Reasoning completed`, `Execution authorised`/`Execution
denied`) are logged by wrapping the relevant *interface* dependency
(`CommunicationIntake`, `ReasoningProvider`) via delegation, or by
subscribing to `EventBus` events `DefaultExecutionPipeline` already
publishes — never by modifying `InMemoryCommunicationIntake`,
`ModelReasoningProvider`, or `DefaultExecutionPipeline` themselves.
**One log line the task names is deliberately not independently
observed: "Reply composed."** `ResponseComposer` is, by its own Contract
Design (Decision 1, restated in its own class KDoc), deliberately **not**
interface-backed — "no swappable seam exists for this component" — and
its Kotlin `class` declaration is not `open`, so it cannot be subclassed
either. Instrumenting it would require modifying a frozen, Scope-Locked,
already-implemented file, which this Unit's own constraints forbid absent
a genuine defect (none exists here). **Consequence, stated plainly, not
silently worked around:** "Reply composed" is not logged as its own,
independently-observed event anywhere in this Unit. What *is* logged
instead — "Reasoning completed" (before composition) and "Reply
delivered"/"Execution denied" (after composition, inferred from the
`execution.completed`/`permission.denied` events composition's own
downstream `ResponseDelivery` call causes) — brackets composition on both
sides without ever touching it directly. This is recorded here as a
disclosed scope boundary of this Unit's logging coverage, not an
oversight to be discovered later.

### Decision 4 — `ParkerRuntimeOutcome`, not a reuse of `GatedOutcome`

`ConversationReplyCoordinator.submitAndDeliver` returns
`GatedOutcome<ExecutionResult>`. `submitOwnerMessage` returns a distinct
type, `ParkerRuntimeOutcome`, with three variants: `Delivered`,
`NotAccepted` (mirroring `GatedOutcome`'s two), and `Failed` — a third
variant no frozen coordinator's own return type has any equivalent for,
because every one of those coordinators' own Scope Locks states plainly
that an exception "propagates unchanged" to the caller rather than being
represented as a value. Reusing `GatedOutcome<ExecutionResult>` directly
would have meant either fabricating a `NotAccepted` reason string to
represent a genuine fault (dishonest — conflating a structural rejection
with a caught exception) or leaving `submitOwnerMessage` to still throw
on a genuine fault (contradicting the task's own explicit requirement
that "model unavailable," "tool failure," and "coordinator failure" be
handled, not merely re-thrown). A new, small, composition-root-owned type
was the only option that kept both distinctions honest.

### Decision 5 — Exactly one caught-exception boundary, and why it is the correct one

No coordinator between `ConversationReplyCoordinator` and the real fault
origins (`LocalHttpModelInferenceClient`'s HTTP call,
`LocalTextChannelDeliverTool.execute`'s callback) catches anything itself
— confirmed by direct reading of `CommunicationConversationCoordinator`,
`ConversationTurnReasoningCoordinator`, `ReplyDeliveryCoordinator`,
`ResponseComposer`, `ResponseDelivery`'s own Scope Locks and source
(each states "no `try`/`catch` exists anywhere in this class"), and of
`DefaultExecutionPipeline.executeResolvedTool`, which calls
`tool.execute(request)` with no surrounding `try`/`catch`.
`ParkerRuntime.submitOwnerMessage` is therefore the *first* point in the
entire call chain, from a real inbound message to a real Tool
invocation, at which any component catches a genuine fault at all. This
is not a gap this Unit works around — it is exactly where the task's own
"production error handling" requirement belongs, and exactly why a
composition root, not a coordinator, is the correct place to add it.

### Decision 6 — `TimeoutCancellationException` is caught and reported, every other `CancellationException` is rethrown

`ModelReasoningProvider.reason`'s own `withTimeout` call throws
`kotlinx.coroutines.TimeoutCancellationException` — a `CancellationException`
subtype — on a real model timeout. Rethrowing every `CancellationException`
unconditionally (the naively "safe" coroutine-cancellation-hygiene rule)
would misreport a genuine, expected "the model was unavailable" outcome
as a real coroutine cancellation, propagating it to
`submitOwnerMessage`'s own caller as an uncaught exception rather than
the `Failed` outcome the task explicitly requires for "model
unavailable." **Decision:** catch `TimeoutCancellationException`
specifically, before the general `CancellationException` rethrow-guard,
and report it as `ParkerRuntimeOutcome.Failed(PipelineStage.REASONING, e)`.
Every other `CancellationException` (a real coroutine cancellation, e.g.
structured-concurrency scope shutdown) is still rethrown unchanged,
never swallowed, never misreported as a `Failed` outcome.

### Decision 7 — `PipelineStage` distinguishes only `REASONING` from `UNKNOWN`

A caught fault is tagged with a best-effort `PipelineStage`. Only
`REASONING` is ever distinguished from `UNKNOWN`, because it is the only
stage capable of throwing a structurally-distinguishable exception type
from this runtime's own vantage point (the model call is this pipeline's
only network call). Every other exception — from `InMemoryConversationEngine`,
`ResponseComposer`, `LocalTextChannelDeliverTool`, or anywhere else — is
classified `UNKNOWN` deliberately, not guessed at: no tagged,
stage-labelled exception type exists on any of those components (adding
one would mean modifying frozen production code this Unit is not
authorised to touch), so reporting anything more specific would be
fabricating a fact this runtime cannot actually observe.

### Decision 8 — System Principal registration and activation

`ResponseComposer`'s own operating identity (`system.response-composer`)
becomes `ExecutionRequest.principalId` on the delivery path — the
identity `DefaultPermissionEngine` actually evaluates. For that
evaluation to ever reach `DefaultPermissionPolicy` (rather than being
short-circuited to `DENIED` for a non-`ACTIVE` Principal),
`system.response-composer` must be registered and `ACTIVE`, not merely
`CREATED`. **Decision:** `ParkerRuntime.start()` registers and activates
(`CREATED` → `ACTIVE`, via `IdentityService.updateStatus`, the only
sanctioned transition mechanism) four Principals: `system.parker` (this
runtime's own operating/audit identity, used for `ModuleRegistry.enable`
and `EventBus.subscribe` calls), `system.conversation-engine` and
`system.response-composer` (each already required, by `InMemoryConversationEngine`/
`ResponseComposer`'s own existing code, to resolve to *some* registered
Principal, but not required by either class to be `ACTIVE` — activated
anyway here, since a real production identity graph should not leave a
system Principal sitting in `CREATED` indefinitely), and the configured
owner (`ParkerRuntimeConfig.ownerPrincipalId`, `PrincipalType.USER`).
This is a disclosed interpretive choice mirroring the same one already
made and recorded for the Identity Service's own `USER`/`SYSTEM` owner
rules (`IMPLEMENTATION_GAPS.md` #38) — no new lifecycle rule is invented;
`PrincipalLifecycleTransitions`' existing, unmodified `CREATED → ACTIVE`
edge is used exactly as it already exists.

---

## 6. Trust Framework Preservation (Constitutional Constraint)

Every execution this runtime performs still passes through
`DefaultExecutionPipeline` → `DefaultPermissionEngine` →
`DefaultPermissionPolicy`, unmodified. There is no code path in
`ParkerRuntime`, or in any class it constructs, that submits a Tool
invocation, or delivers a response, other than through
`ResponseDelivery.deliver` → `ExecutionPipeline.submit`. No bypass, no
shortcut, no "trusted internal path" exists. This is verified both by
direct code review (nothing in `src/composition` holds a `Tool` reference
or calls `Tool.execute` directly) and by test
(`ParkerRuntimeConversationPipelineTest`'s own assertion that "Execution
authorised" is genuinely logged, proving the Permission Engine was
actually consulted and actually approved, not skipped).

---

## 7. Acceptance Criteria

- `ParkerRuntime` exists, is constructible from `ParkerRuntimeConfig` +
  `ParkerLogger` alone (two other parameters optional, defaulted), and
  `start()` constructs a complete, real runtime graph.
- A real `InboundOwnerMessage`, submitted via `submitOwnerMessage` after
  `start()`, traverses `CommunicationIntake → ConversationEngine →
  ReasoningProvider → ResponseComposer → ResponseDelivery →
  ExecutionPipeline → Tool execution`, with Trust authorisation
  genuinely exercised.
- Every existing coordinator's own constructor, method signature, and
  Scope-Locked behaviour is unchanged — confirmed by this Unit not
  editing a single file under `src/runtime`, `src/interfaces`, or
  `src/contracts`.
- Missing configuration, invalid configuration, dependency construction
  failure, startup failure, model unavailability, and Tool failure each
  produce a named, typed, logged outcome — never a silently swallowed
  exception.
- `shutdown()` is graceful: cancels this runtime's own `EventBus`
  subscriptions and transitions to `STOPPED`, attempting every step even
  if an earlier one fails.
- Every object `ParkerRuntime` constructs receives 100% of its own
  constructor parameters explicitly, from this function's own local
  variables or `config` — no global, static, or singleton lookup anywhere
  in `src/composition` (verified by direct code review, Section 3 of the
  Implementation Review).
- The new test suite (Section 8, below) passes; the existing 612-test
  suite is unmodified and, per this Unit's own manual review (Section 4
  of the Implementation Review), unaffected.

## 8. Test Strategy

`tests/composition/`:

- `ParkerRuntimeConfigLoaderTest.kt` (9 tests) — every required/optional
  key, every named configuration failure.
- `LoggingDecoratorsTest.kt` (9 tests) — each of the three logging seams,
  in isolation, including "never logs message/reply text."
- `ParkerRuntimeStartupAndShutdownTest.kt` (9 tests) — the full lifecycle
  state machine, including a real dependency-construction failure
  (a blank configured Module ID) and shutdown after a failed start.
- `ParkerRuntimeConversationPipelineTest.kt` (4 tests) — the complete,
  real, inbound-to-reply path (using `StubModelServer`, Section 9, below,
  in place of a real model server only), plus the three structural
  rejection paths (unregistered sender, `Goal`, `NoAction`).
- `ParkerRuntimeFailureHandlingTest.kt` (3 tests) — model unavailable
  (connection refused), model timeout, and Tool-level failure (a throwing
  `OwnerNotificationSink`), each proven caught and reported, never thrown
  past `submitOwnerMessage`, never silently dropped.
- `CompositionTestFixtures.kt` — fixtures only (`RecordingParkerLogger`,
  `RecordingOwnerNotificationSink`, `StubModelServer`), no tests of their
  own, mirroring this repository's own established fixture-file
  convention.

**34 new test methods**, net.

## 9. `StubModelServer` — Scope and Honest Limits

`StubModelServer` is a local loopback HTTP server (JDK's own
`com.sun.net.httpserver.HttpServer`, no new Gradle dependency), speaking
the same Ollama-shaped JSON `LocalHttpModelInferenceClient`'s existing,
unmodified default formatters already produce/consume. Using it exercises
`LocalHttpModelInferenceClient`'s real, live HTTP path for the first time
in this repository's test suite — **narrowing, not closing**,
`IMPLEMENTATION_GAPS.md` #53's "live HTTP path is not exercised" item: a
real loopback HTTP round-trip is now exercised; a *real Ollama server* is
still not, and this Unit does not claim otherwise.

## 10. Verification Requirements and Disclosed Sandbox Limitation

This implementation was written and manually reviewed in a sandboxed
environment that could not run `./gradlew test` to completion:
`./gradlew` project-evaluating tasks (`tasks`, `compileKotlin`, even
plain `help`) each hung past every available invocation's own time
budget, across every combination attempted (online/offline,
daemon/no-daemon, fresh `GRADLE_USER_HOME` on a native filesystem,
toolchain auto-download disabled, file-system watching disabled); `gradle
--version` (which skips project evaluation) returned normally in 6
seconds, and a plain `cp -r` of the project directory to a native
filesystem did not finish copying top-level files within the same time
budget — pointing to slow per-file I/O on the mounted project directory
as the underlying cause, not a Gradle- or Kotlin-specific defect. No
standalone `kotlinc` was available either. This mirrors this
repository's own established, repeatedly-used precedent (`docs/implementation/CONVERSATION_REPLY_COORDINATOR_SCOPE_LOCK.md`
Section 16: "a static, honestly-disclosed projected count if the sandbox
cannot resolve the Kotlin Gradle plugin, pending Steven's Android Studio
verification").

**What was done instead:** every new production class's constructor call
was checked, argument-by-argument, against the real, current source of
every dependency it constructs (not against memory or assumption); every
import was verified to resolve to a real, existing, correctly-packaged
type; every test was written against the same real signatures. This is a
rigorous manual review, not a compiler run, and cannot substitute for
one.

**Static projection: 612 (Sprint 10, Unit 3 baseline, Android-Studio-verified,
confirmed by Steven) + 34 new = 646.** This number is a projection, not a
verified result. Per PES-001 Stage 7, Android Studio remains the
authoritative verification environment for implementation acceptance —
this Unit is not to be treated as complete until Steven runs the full
suite there and records the real pass/fail count, exactly as every prior
Sprint 10 Unit's own Scope Lock already required of itself.

## 11. Stop Conditions

- No existing `src/` file under `contracts`, `interfaces`, or `runtime`
  may be modified. (None was.)
- No existing `tests/` file may be modified. (None was.)
- `build.gradle.kts` may only be changed to add the two new source
  directories (Section 5, Decision 1) — no dependency, plugin, or
  compiler-option change. (Confirmed: only the `sourceSets` block
  changed.)
- No new external Gradle dependency. (None added — `StubModelServer` uses
  only the JDK standard library.)
- No service locator, singleton, or global/static lookup in
  `src/composition`. (Verified in the Implementation Review, Section 3.)
- No architecture, ADR, Constitution, or existing Scope Lock document may
  be modified. (None was.)

---

## Conclusion

This Plan authorises Parker's first production composition root:
`ParkerRuntime` (`src/composition/`), constructing and owning a complete,
real runtime graph from the already-existing, already-tested, frozen
components Sprints 1–10 built, adding no new architecture, no new Trust
mechanism, and no bypass of any existing one. It closes the "no
production composition root exists" item of `IMPLEMENTATION_GAPS.md` #53
specifically (Section 13 of the Implementation Review states this
precisely), narrows (not closes) the "live HTTP path untested" item, and
leaves `Goal`/Planner routing and `ReasoningContext` assembly ownership
exactly as open as they were before this Unit, as required. Verification
in this sandbox was structurally blocked; a rigorous manual review and an
honest, clearly-labelled static test-count projection (646) stand in its
place, pending Steven's own Android Studio run.

---

## 12. Addendum — Post-Verification Correction (Sprint 10, Unit 4)

Steven's first Android Studio run of this Unit reported the real,
authoritative result this Plan's Section 10 anticipated might diverge
from projection: 646 total, 641 passed, 5 failed, compilation
successful, plus a compiler warning at `ParkerRuntime.kt` ~line 340
(redundant `suspend` modifier).

Diagnosis found one shared root cause for all 5 failures:
`kotlinx.coroutines.test.runTest`, used by
`ParkerRuntimeConversationPipelineTest.kt` and
`ParkerRuntimeFailureHandlingTest.kt`, races its own virtual-time
scheduler against the real, foreign-thread HTTP I/O these tests perform
against `StubModelServer` — the scheduler fires `ModelReasoningProvider`'s
real `withTimeout` before the real, fast loopback result arrives. This is
a test-harness defect, not a production defect: no file under
`src/composition` required correction for it. Both files were corrected
to use `runBlocking` (real wall-clock time) instead.

**Resolving the compiler warning took two attempts; the first was wrong
and broke compilation.** Attempt 1 extracted the private `stage()`
helper to a top-level `internal inline fun` and dropped its outer
`suspend`, on the (incorrect) reasoning that an `inline` function's only
suspension point — invoking its own `suspend`-typed `block` parameter —
doesn't require the enclosing function itself to be `suspend`. Steven's
next Android Studio run caught this at compilation, before any test
could run: `Suspend inline lambda parameters of non-suspend function
type are not supported`, and `Suspended function 'invoke' should be
called only from a coroutine or another suspend function`. Calling a
`suspend`-typed parameter is itself a suspending call requiring a
`Continuation`, which only a `suspend` function provides to its own
body — inlining does not remove that requirement. Attempt 2 (current)
restores `stage` as `internal suspend inline fun`, with `block` marked
`crossinline` — Steven's own specified correction, applied exactly. This
Plan does not have the literal original Android Studio warning text on
file, only a relayed paraphrase ("redundant `suspend` modifier"); since
removing the modifier produced a hard compile error rather than a
suppressible warning, that paraphrase most likely described something
other than `stage`'s own enclosing keyword — this is not re-diagnosed
further without the literal compiler text.

Independently of both attempts, the same review surfaced one further,
genuine defect in the same function: its `catch (e: Exception)` clause
caught `CancellationException` before any more specific handler could,
misreporting a genuine cancellation as
`ParkerRuntimeException.DependencyConstructionFailed`. Corrected by
adding an explicit `CancellationException` catch ahead of the general
one — unaffected by either `suspend`-modifier attempt — with a new
focused regression test file (`tests/composition/StageCancellationTest.kt`,
4 tests) covering it directly.

This Addendum's Section 11 stop conditions all still hold: no
`contracts`/`interfaces`/`runtime` file under `src/` was touched; the two
test files corrected are the only test files changed; `build.gradle.kts`
was not touched further; no new dependency was added.

**Compilation itself is not yet independently verified after this second
correction.** This sandbox still cannot evaluate the Gradle project
(re-confirmed again during this round, including with a locally
available Gradle 8.10 install that avoids the wrapper's network
dependency — project evaluation still did not complete within any
available time budget). **Updated static projection: 650** (646 + 4 new
`StageCancellationTest.kt` methods) — a projection from manual review,
not a verified result. **This Unit remains not accepted, pending
Steven's own Android Studio re-run.**
