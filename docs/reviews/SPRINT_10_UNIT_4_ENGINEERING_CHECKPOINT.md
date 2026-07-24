# Sprint 10, Unit 4 — Production Composition Root — Engineering Checkpoint

## Status

**Engineering Checkpoint, PES-001 Stage 9.** Evaluates project direction
— whether architectural assumptions held, whether sprint sequencing
remains correct, whether new implementation gaps were discovered, and
whether future Implementation Plans should change as a result. Does not
review code quality (the Post-Implementation Review covers that).

---

## 1. Did the architectural assumptions hold?

Yes, with one confirmation worth stating plainly: every coordinator
Sprints 7–10 built (`CommunicationConversationCoordinator`,
`ConversationTurnReasoningCoordinator`, `ReplyDeliveryCoordinator`,
`ConversationReplyCoordinator`) was constructible with real,
production-shaped collaborators, with no missing seam, no undocumented
hidden dependency, and no interface shape that turned out to be wrong
once actually wired end-to-end for real. This is a meaningful, positive
signal about the Contract-Design-first discipline PES-001 Section 2A
introduced after Sprint 4: four coordinators, each built independently
across four different Units, composed on the first attempt.

The one place this Unit had to make a genuine decision the architecture
left open, rather than merely wire existing pieces, was Permission Policy
*content* (Plan Decision 2) — and `IMPLEMENTATION_GAPS.md` #25 had
already flagged, months earlier, that exactly this decision would fall
to "a caller." This Unit is that caller, and the gap's own prior text
predicted this moment accurately.

## 2. Is sprint sequencing still correct?

Yes. `SPRINT_9_HANDOVER.md` recommended closing gap #53 item 1 (the
Reply → `ResponseDelivery` wiring) before attempting the composition
root, specifically because "a composition root should wire a complete,
already-connected pipeline rather than a partially-wired one." Sprint 10
Units 1–3 did exactly that, in sequence, each verified before the next
began. This Unit, arriving fourth, found a fully-connected pipeline
waiting — `ConversationReplyCoordinator.submitAndDeliver` was already the
single call this composition root needed to invoke. Had this Unit been
attempted before Unit 3, it would have needed to either invoke three
separate coordinators itself (duplicating `ConversationReplyCoordinator`'s
own now-existing sequencing) or wait anyway. The recommended sequencing
was correct, and this Unit's own experience is direct evidence of that,
not merely a restatement of the prior recommendation.

## 3. Were new implementation gaps discovered?

One, narrow gap, not previously recorded: **`IMPLEMENTATION_GAPS.md` #25's
policy-content decision, once actually made concrete, is currently
recorded nowhere except this Unit's own Plan.** A future reader of
`DefaultPermissionPolicy.kt` or `PermissionPolicy.md` would not discover,
from either of those documents, that a real, running Parker instance
approves exactly one (action, resourceType) pair. This is recorded as
part of this Unit's own `IMPLEMENTATION_GAPS.md` update (Section 5,
"Update (Sprint 10, Unit 4)"), not left implicit.

No other new implementation gap was discovered. In particular: this Unit
did not discover any defect in `ConversationReplyCoordinator`,
`ReplyDeliveryCoordinator`, `ResponseComposer`, `ResponseDelivery`,
`DefaultExecutionPipeline`, `DefaultPermissionEngine`, or
`DefaultPermissionPolicy` requiring correction — every one of them
composed exactly as its own Scope Lock or class KDoc already documented.

## 4. Should future Implementation Plans change as a result?

Two observations worth carrying forward, neither urgent:

- **A real composition root now exists as a template.** The next Unit
  that needs to add a second production entry point (a real Android/UI
  channel, a scheduled task trigger, a second Communication Channel
  module) has a concrete, working pattern to extend
  (`ParkerRuntime.buildAndRegisterRuntimeGraph`) rather than needing to
  re-derive the full registration sequence (system Principals, action
  vocabulary, Module/Tool/Resource registration, permission policy
  content) from architecture documents alone.
- **The sandbox-verification limitation this Unit hit (Plan Section 10)
  is now more severe than the prior "Kotlin Gradle plugin cannot
  resolve" limitation Sprint 10 Units 1–3 each disclosed** — this Unit's
  sandbox could not even evaluate the Gradle project at all, for reasons
  that appear to be I/O-related rather than dependency-resolution-related
  (Plan Section 10's own diagnostic detail). This is worth Steven's
  awareness for planning future AI-authored Units' own verification
  expectations, independent of anything this Unit's own Kotlin does.

## 5. Recommendation for Sprint 11

With this Unit (pending Steven's verification), `IMPLEMENTATION_GAPS.md`
#53's "no production composition root exists" item closes. The gap's
remaining open items — `Goal`/Planner Runtime routing, `ReasoningContext`
assembly ownership, and the (now narrowed but not closed) live-HTTP-path
item — are each a larger, different kind of next step than this Unit was,
consistent with `SPRINT_9_HANDOVER.md`'s own original assessment of them.
This Checkpoint does not select one for Sprint 11; per PES-001, sequencing
authority remains Steven's.

## 6. Addendum — Post-Verification Correction

Steven's first Android Studio run reported 646 total, 641 passed, 5
failed, plus a compiler warning at `ParkerRuntime.kt` ~line 340. All 5
failures shared one root cause: `kotlinx.coroutines.test.runTest`'s
virtual-time scheduler racing ahead of real HTTP I/O in
`ParkerRuntimeConversationPipelineTest.kt` and
`ParkerRuntimeFailureHandlingTest.kt`, firing `ModelReasoningProvider`'s
real `withTimeout` before the real, fast `StubModelServer` result
arrived. This is a **test-harness defect, not an architectural one** —
consistent with Section 3's own finding above that no coordinator this
Unit wires required correction; it confirms, rather than revises, that
finding. Corrected by switching both files to `runBlocking`.

**Resolving the compiler warning surfaced a process gap worth recording
here directly: the first attempted fix did not compile.** Attempt 1
extracted `stage()` to a top-level `internal inline fun` and dropped its
outer `suspend`, reasoning that an `inline` function's only suspension
point (invoking its own `suspend` lambda parameter) doesn't require the
enclosing function to be `suspend` — reasoning this Checkpoint now
records as **incorrect**. Steven's next Android Studio run caught it at
compilation (before any test could run): `Suspend inline lambda
parameters of non-suspend function type are not supported`, and
`Suspended function 'invoke' should be called only from a coroutine or
another suspend function`. Attempt 2 restores `stage` as `internal
suspend inline fun` with a `crossinline` block parameter — Steven's own
specified correction. This is worth carrying forward alongside Section 2
and 3's own findings above: unlike every coordinator this Unit wires,
which composed correctly on the first attempt, this one small helper
function needed a second, corrective pass — a narrower failure than an
architectural one, but a real instance of "verification friction," not
zero-friction as this Checkpoint's Section 1 finding might otherwise
suggest read in isolation.

Independently of both `suspend`-modifier attempts, the same review
surfaced one further, genuine, independent defect in the same function —
a `CancellationException` caught too broadly, risking suppression of
real cancellation semantics during startup — fixed, with a new focused
regression test (`tests/composition/StageCancellationTest.kt`) added to
cover it.

No new implementation gap is recorded as a result of this correction
round beyond what Section 3 already recorded — this was a test-harness
and a cancellation-handling correction, not a discovery about any
coordinator's own contract. **Compilation after this second correction
is not yet independently verified** — this sandbox still cannot evaluate
the Gradle project, even with a locally available Gradle 8.10 install
that avoids the wrapper's network dependency. Updated static projection: **650** (646 + 4
new tests), still unverified in this sandbox. **This Unit remains not
accepted, pending Steven's own re-run.**
