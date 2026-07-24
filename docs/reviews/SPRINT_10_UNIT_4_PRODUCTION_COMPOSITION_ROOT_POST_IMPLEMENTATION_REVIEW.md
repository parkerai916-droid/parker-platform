# Sprint 10, Unit 4 — Production Composition Root — Post-Implementation Review

## Status

**Post-Implementation Review, PES-001 Stage 9.** Evaluates the
implementation authorised by
`docs/implementation/PRODUCTION_COMPOSITION_ROOT_IMPLEMENTATION_PLAN.md`
("the Plan"). Includes the required Self-Traceability Review (PES-001
Chapter 4, Level 2) for every public type this Unit introduced. Written
before Android Studio verification, per the Plan's own Section 10
disclosure — this review evaluates implementation *correctness against
the Plan*, not a verified test result, which remains outstanding.

---

## 1. Scope Compliance

**Confirmed by direct review of every changed/added file:**

| Constraint | Status |
| --- | --- |
| No existing `src/contracts`, `src/interfaces`, or `src/runtime` file modified | Confirmed — `git`-visible diff is additions only, plus `build.gradle.kts` |
| No existing `tests/` file modified | Confirmed |
| `build.gradle.kts` changed only to add `src/composition`/`tests/composition` to `sourceSets` | Confirmed — no dependency, plugin, or compiler-option line touched |
| No new external dependency | Confirmed — `StubModelServer` uses `com.sun.net.httpserver`, JDK standard library |
| No coordinator responsibility moved or redefined | Confirmed — `ParkerRuntime` calls `ConversationReplyCoordinator.submitAndDeliver` exactly once per message, taking its result (`GatedOutcome`) and re-wrapping it, never inspecting or second-guessing what happened inside it |
| No service locator / singleton / global lookup | Confirmed — see Section 3 |
| Trust authorisation mandatory, never bypassed | Confirmed — see Section 4 |

## 2. Self-Traceability Review

Every public type `src/composition` introduces, and where its
authorisation comes from:

| Type | Authorised by | Required / Deferred / Excluded | Matches implementation? |
| --- | --- | --- | --- |
| `ParkerRuntime` | Task instruction ("expose a production-ready runtime object"); Plan Section 2 | Required | Yes — one class, constructs the graph, exposes `start`/`submitOwnerMessage`/`shutdown` |
| `RuntimeLifecycleState` | Task instruction ("lifecycle management"); Plan Section 2 | Required | Yes — six states, exactly the transitions `start()`/`shutdown()` drive |
| `ParkerRuntimeConfig` / `ParkerRuntimeConfigLoader` | Task instruction ("configuration loading"); Plan Section 2 | Required | Yes |
| `ParkerRuntimeException` (sealed, 6 subtypes) | Task instruction ("dependency construction failures... missing configuration... startup failure"); Plan Decision 4 | Required | Yes — one subtype per named failure category the task lists, plus `NotRunning` for caller misuse |
| `ParkerRuntimeOutcome` (sealed, 3 variants) / `PipelineStage` | Task instruction ("model unavailable... coordinator failure... tool failure"); Plan Decisions 4, 6, 7 | Required | Yes — see Plan Decision 4 for why `GatedOutcome` reuse was rejected |
| `ParkerLogger` / `LogLevel` / `ConsoleParkerLogger` | Task instruction ("Introduce production logging"); Plan Section 2 | Required | Yes — dependency-free, per Plan Decision 1's own "no new Gradle dependency" constraint |
| `LoggingCommunicationIntake` | Task's named log line "Conversation accepted"; Plan Decision 3 | Required | Yes — decorator, delegates every call, logs only structural fields |
| `LoggingReasoningProvider` | Task's named log line "Reasoning completed"; Plan Decision 3 | Required | Yes |
| `RuntimeEventLogger` | Task's named log lines "Execution authorised"/"Execution denied"; Plan Decision 3 | Required | Yes — `EventBus` subscription, not decoration, matching `InMemoryTaskManagerRuntime`'s own established precedent |
| `OwnerNotificationSink` / `LoggingOwnerNotificationSink` | Existing `LocalTextChannelDeliverTool.onOwnerNotified` constructor parameter, which this Unit's composition root is the first real caller obligated to supply | Required | Yes — named `fun interface` instead of a bare lambda solely so it is constructor-injectable/testable, per the task's own explicit constructor-injection instruction |

**No public type in this Unit is unauthorised.** "Reply composed" (one of
the task's own example log lines) is the one named line this Unit's own
Plan (Decision 3) explicitly declines to implement as an independently
observed event, and states why (`ResponseComposer` is non-interface,
non-`open`, frozen) rather than silently omitting it.

## 3. Constructor-Injection Discipline — Verified

Every `Default*`/`InMemory*`/`Model*`/`LocalHttp*`/`LocalTextChannelDeliverTool`
constructor call inside `ParkerRuntime.buildAndRegisterRuntimeGraph`
supplies its arguments from one of exactly two sources: a `val` declared
earlier in the same function, or `config`/`logger`/`ownerNotificationSink`/
`clock` (this class's own constructor parameters). No call site reads
`System.getenv()`, no call site references a companion-object-held
mutable instance of another collaborator, and no class in
`src/composition` has a companion object holding anything other than
`private` constant identifiers (`ParkerRuntime`'s own `SYSTEM_PARKER_PRINCIPAL_ID`
and two sibling `PrincipalId` constants; `RuntimeEventLogger`'s own seven
`EventType` constants) — none of which is itself a constructed
collaborator or a mutable, shared, globally-reachable instance.
`ParkerRuntimeConfigLoader.load` takes its environment map as a
parameter; it does not call `System.getenv()` itself, so even
configuration sourcing is caller-supplied, not a global lookup performed
by this Unit's own code.

## 4. Trust Framework Preservation — Verified

`ParkerRuntime` never holds a `Tool` reference and never calls
`Tool.execute` or `ExecutionPipeline.submit` directly — the only path
from a submitted message to a Tool invocation is
`ConversationReplyCoordinator.submitAndDeliver` → ... →
`ResponseDelivery.deliver` → `ExecutionPipeline.submit` →
`PermissionEngine.evaluate`, exactly as those already-frozen classes
define it. `ParkerRuntimeConversationPipelineTest`'s own successful-path
test asserts the logger genuinely recorded "Execution authorised" —
proof the Permission Engine was actually consulted and actually
approved, not merely assumed. The one policy rule this Unit supplies
(Plan Decision 2) is additive, narrow, and does not touch
`DefaultPermissionEngine`'s own identity-status gating (Sprint 2, Unit
A1) — a non-`ACTIVE` Principal is still denied before `DefaultPermissionPolicy`
is ever consulted, unchanged.

## 5. Deviations From the Plan

None. Implementation matches every Decision in the Plan's Section 5.

## 6. Known, Disclosed Limitations

Restated from the Plan (Sections 5 Decision 3, 9, 10), not repeated here
in full — see the Plan directly. In summary: "Reply composed" is not
independently logged; `StubModelServer` narrows but does not close
`IMPLEMENTATION_GAPS.md` #53's live-HTTP-path item; and this Review was
written without a completed local Gradle run (Section 7, below).

## 7. Verification

**Not yet performed by Steven.** This sandbox could not run
`./gradlew test` to completion (Plan Section 10 documents the exact
diagnostic steps taken and their results). A rigorous manual
signature-by-signature review was performed in its place (Section 3,
above, and throughout implementation). Static projection: **646**
(612 confirmed baseline + 34 new tests, net). This is a projection, not
a verified count, and this Unit is not to be treated as accepted until
Steven's own Android Studio run confirms it, per PES-001 Stage 7.

## 8. Recommendation

**Ready for Steven's Android Studio verification.** No further AI-side
implementation work is recommended before that verification occurs. If
verification surfaces a compile error or test failure, the correct next
step is a small, targeted implementation-decisions update against the
specific failure, not a re-plan of this Unit's own scope.

## 9. Addendum — Post-Verification Correction

Section 8's prediction was tested directly: Steven's first Android
Studio run surfaced exactly a targeted set of test failures (5, all
sharing one root cause) plus a compiler warning, not an architectural
problem — confirming Section 8's own recommended response was the right
one to reach for.

**Root cause, all 5 failures:** `ParkerRuntimeConversationPipelineTest.kt`
and `ParkerRuntimeFailureHandlingTest.kt` used
`kotlinx.coroutines.test.runTest`, whose virtual-time scheduler raced
ahead of the real, foreign-thread HTTP I/O these tests perform against
`StubModelServer`, firing `ModelReasoningProvider`'s real `withTimeout`
before the real (fast) loopback result arrived. A test-harness defect,
not a production one — no file under `src/composition` needed correction
for this failure. Both files were corrected to `runBlocking` (real
wall-clock time).

**Compiler warning -- two attempts, the first wrong.** Attempt 1
extracted the private `stage()` helper to a top-level `internal inline
fun` and dropped its outer `suspend`, reasoning (incorrectly) that an
`inline` function's only suspension point -- invoking its own `suspend`
lambda parameter -- doesn't require the enclosing function itself to be
`suspend`. **This did not compile.** Steven's next Android Studio run
caught it at compilation: `Suspend inline lambda parameters of
non-suspend function type are not supported`, and `Suspended function
'invoke' should be called only from a coroutine or another suspend
function`. Calling a `suspend`-typed parameter is itself a suspending
call requiring a `Continuation`; only a `suspend` function supplies one
to its own body, and inlining does not remove that requirement. Attempt
2 (current) restores `stage` as `internal suspend inline fun`, with
`block` marked `crossinline` -- Steven's own specified correction. This
Review does not have the literal original compiler warning text on file,
only a relayed paraphrase ("redundant `suspend` modifier"); given that
removing the modifier produced a hard error rather than a suppressible
warning, that paraphrase most likely described something other than
`stage`'s own enclosing keyword -- not re-diagnosed further without the
literal text. Independently of both attempts, the same review surfaced
one further, genuine defect: `stage()`'s `catch (e: Exception)` caught
`CancellationException` before any more specific handler could, which
would have misreported a genuine cancellation as
`DependencyConstructionFailed`. Fixed with an explicit
`CancellationException` catch ahead of the general one, unaffected by
either `suspend`-modifier attempt; covered directly by a new
`tests/composition/StageCancellationTest.kt` (4 tests).

**Updated static projection: 650** (646 + 4), still a projection, not a
verified result — this sandbox still cannot evaluate the Gradle project.
**This Unit remains not accepted, pending Steven's own Android Studio
re-run confirming a clean, passing result.**
