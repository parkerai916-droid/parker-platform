# Sprint 10 Retrospective

## Status

**Sprint Retrospective, covering Sprint 10 in full: Units 1–4.** Written
alongside Unit 4's own Implementation Plan, Post-Implementation Review,
and Engineering Checkpoint. Unit 4's own test count is a static
projection, not yet Android-Studio-verified (see its own documents for
why) — this Retrospective is written on that basis and says so plainly
rather than overstating Sprint 10's own completion state.

---

## 1. What Sprint 10 Set Out to Do

`SPRINT_9_HANDOVER.md` identified, as its single recommended next unit,
closing `IMPLEMENTATION_GAPS.md` #53's item 1: "nothing in this
repository calls `ResponseComposer.compose` from a real conversation
turn... the wiring that would connect `CommunicationConversationCoordinator`'s
reasoning output to `ResponseComposer`, and `ResponseComposer`'s output
onward to `ResponseDelivery.deliver`, remains unimplemented" — while
explicitly recommending *against* attempting a production composition
root in the same pass, on the grounds that "a composition root should
wire a complete, already-connected pipeline rather than a partially-wired
one."

## 2. What Sprint 10 Actually Delivered

Four Units, each building directly on the last, each (Units 1–3)
Android-Studio-verified before the next began:

| Unit | Delivered | Verified |
| --- | --- | --- |
| 1 — `ResponseComposer` | `Reply` → `OutboundParkerResponse` construction | 589/589 |
| 2 — `ReplyDeliveryCoordinator` | Sequences `ResponseComposer` → `ResponseDelivery` | 599/599 |
| 3 — `ConversationReplyCoordinator` | Sequences `CommunicationConversationCoordinator` → `ReplyDeliveryCoordinator` | 612/612 |
| 4 — Production Composition Root (`ParkerRuntime`) | Constructs and starts a complete, real runtime graph; one production entry point running a real inbound message through the entire pipeline | **projected 646, not yet verified** |

By the end of Unit 3, one real, tested, production-shaped Kotlin call —
`ConversationReplyCoordinator.submitAndDeliver` — connected a real
inbound message all the way to a real `ResponseDelivery.deliver` call,
with nothing in the repository yet calling it outside a test. Unit 4
closes exactly that remaining gap: a real composition root now
constructs the complete graph and exposes that same call as a real
production entry point.

## 3. Did Sprint 10 Follow Its Own Recommended Sequencing?

Yes, precisely. `SPRINT_9_HANDOVER.md`'s "item 3 (composition root) is
better sequenced *after* item 1" recommendation was honoured exactly:
Units 1–3 closed item 1 in three small, individually-verified steps
(rather than one large one), and Unit 4 — initiated directly by Steven,
naming the composition root explicitly — followed only once that chain
was complete. See `SPRINT_10_UNIT_4_ENGINEERING_CHECKPOINT.md` Section 2
for the direct evidence this produced: Unit 4 needed to wire exactly one
already-complete coordinator, not reconstruct any part of the chain
itself.

## 4. Process Observations

- **Small units composed cleanly.** Four independently-scoped units,
  each with its own Scope Lock (Units 1–3) or Implementation Plan (Unit
  4), each adding exactly one new class with a narrow, named
  responsibility, produced a working end-to-end pipeline with zero
  interface-shape corrections needed along the way. This is the PES-001
  Stage 3 "small implementation units reduce architectural drift"
  principle, observed working in practice across a four-unit chain, not
  merely asserted.
- **`IMPLEMENTATION_GAPS.md` #53 functioned as intended.** A single,
  continuously-updated gap entry tracked exactly which part of "wire the
  conversation pipeline into production" was closed by which unit,
  without ever being prematurely marked closed. Unit 4's own update
  continues that discipline (Section 5 of this Sprint's
  `IMPLEMENTATION_GAPS.md` change).
- **Verification friction increased sharply for Unit 4.** Units 1–3 each
  ran and verified in Android Studio without incident. Unit 4's own
  sandbox could not evaluate the Gradle project at all (its own Plan
  Section 10 documents the diagnostic detail) — a more severe limitation
  than any earlier Sprint 10 unit hit. This is a sandbox/environment
  observation, not a finding about Unit 4's own Kotlin.
- **One process compression, disclosed, not hidden.** Units 1–3 each went
  through the full PES-001 workflow (Architecture Review → Implementation
  Plan → Implementation Decisions → Scope Lock → Implementation) as
  separate stages, separate documents. Unit 4, at Steven's own direct
  instruction naming the deliverables in a single message, compressed
  Plan/Decisions/Scope Lock into one document produced alongside the
  implementation. `PRODUCTION_COMPOSITION_ROOT_IMPLEMENTATION_PLAN.md`'s
  own Status section states this plainly. Whether this compression is
  the right default for a future Unit of similar shape is a process
  question for Steven, not resolved by this Retrospective.

## 5. What Remains Open After Sprint 10

Restated precisely from `IMPLEMENTATION_GAPS.md` #53's own updated text
(pending Unit 4's own verification):

- `Goal` / Planner Runtime routing — entirely unimplemented.
- `ReasoningContext` assembly ownership — entirely unassigned.
- `LocalHttpModelInferenceClient`'s live HTTP path — narrowed (a real
  loopback server is now exercised by Unit 4's own tests) but not
  closed — a real Ollama/model-server round-trip remains untested.
- The one new, narrow item Unit 4's own Engineering Checkpoint
  surfaced: `DefaultPermissionPolicy`'s real, concrete policy content
  (Sprint 10, Unit 4's own Decision 2) is recorded only in Unit 4's own
  Plan — not yet reflected in `PermissionPolicy.md` itself, a
  documentation-completeness follow-up, not a behavioural gap.

## 6. Recommendation

Sprint 10 is feature-complete against its own (revised, Steven-directed)
scope, pending Unit 4's Android Studio verification. No further sprint
sequencing decision is made here — per PES-001, that remains Steven's.

## 7. Addendum — Unit 4 Post-Verification Correction

Steven's first Android Studio run of Unit 4 — Sprint 10's first real,
authoritative verification result, referenced as pending throughout this
Retrospective above — reported 646 total, 641 passed, 5 failed, plus a
compiler warning. Diagnosis found a single shared root cause for all 5
failures: a test-harness timing defect (`kotlinx.coroutines.test.runTest`
racing its virtual-time scheduler against real HTTP I/O in two test
files), corrected by switching those tests to `runBlocking`.

**The compiler warning's own resolution took a second attempt.** The
first attempt (extracting `stage()` to a top-level function and dropping
its outer `suspend`) did not compile — Steven's next Android Studio run
caught it before any test could run, with two hard errors (`Suspend
inline lambda parameters of non-suspend function type are not
supported`; `Suspended function 'invoke' should be called only from a
coroutine or another suspend function`). The corrected version restores
`stage` as `suspend`, with a `crossinline` block parameter. Independently
of that, the same review surfaced one further, genuine, independent
defect — over-broad `CancellationException` handling in the same
function — also corrected, with a new focused regression test added.

This is worth recording as a further, second instance of the "Verification
friction increased sharply for Unit 4" observation in Section 4, above —
now doubly so: Unit 4 is Sprint 10's only Unit that needed a correction
round after its first real verification attempt, and its own first
correction attempt itself needed a second correction. Units 1–3 each
verified cleanly on the first attempt, with no correction round at all.
Updated static projection: **650** (646 + 4 new
tests), still unverified in this sandbox pending Steven's own re-run.
**Sprint 10 Unit 4 remains not accepted; Sprint 10 as a whole remains
pending on it.** Sprint 11 has not begun.
