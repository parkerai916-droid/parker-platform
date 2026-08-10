**Status:** Unit 3-C Warm-Up Orchestration Defect Confirmation Review — **DEFECT CONFIRMED AND CORRECTED (Classification A).** Governance/implementation correction only, against committed baseline `2dca602`. No live model call, no HTTP call, no campaign was created. Only the minimum implementation necessary to wire warm-up execution into the orchestration driver was changed.

# Unit 3-C Controlled Remedy Experiments — Warm-Up Orchestration Defect Confirmation Review

## 1. Defect classification

```text
A — IMPLEMENTATION DEFECT CONFIRMED: warm-ups counted but not executed
```

Independently re-traced, fresh, from the committed source (not from the failed Approval Review's own summary): `Unit3CCampaignDefinition.warmupTrials` (defined at line 441 of `ReasoningProtocolUnit3CControlledRemedyExperimentsTest.kt`) are included in `allTrials` (line 479) — the list used solely to derive the numeric total `liveModelCallCount == 483` — and in exactly one test asserting that count. An exhaustive, case-insensitive search of `ReasoningProtocolUnit3COrchestrationTest.kt` — the file containing the only runnable driver, `Unit3COrchestrationDriver` — for the substring `warmup` returned **zero matches** prior to this task's correction. `trialsFor(Unit3CFamily.CONTROL)` returned exactly `Unit3CCampaignDefinition.controlTrials` (145 trials), never the warm-ups. This exactly and precisely confirms the failed Explicit Execution Approval Review's own finding, independently re-derived rather than assumed.

## 2. Source-level cause

The schedule (`Unit3CCampaignDefinition`) and the orchestration driver (`Unit3COrchestrationDriver`) were implemented as two separately-verified components: the schedule's own `init` block proves 483 is the correct *count* by summing `makesModelCall == true` across `allTrials`; the driver's own tests proved each arm executes its own `trialsFor(family)` list correctly. Neither component's own test suite ever composed the two together end-to-end — nothing asserted that *running the driver* actually reaches 483 real executor invocations. The schedule was correct; the driver's own trial-selection function simply never consulted `warmupTrials` at all, because `Unit3CFamily` has no `WARMUP` case and `warmupTrials` was defined as a top-level property with no distinct arm identity ever wired to it.

## 3. Why arithmetic remained 483 while the executable path was 480

`liveModelCallCount` is computed once, statically, from the schedule *definition* — it does not execute anything and has no dependency on the driver at all. It will always correctly report 483 regardless of whether the driver ever runs, because it is answering a different question ("how many calls does the frozen schedule specify?") than the one that matters for execution readiness ("how many calls will actually be issued if the driver runs?"). The two numbers coincidentally shared the same *symbol* (`483`) in prior review documents without ever being *proven equal* by a test that exercised both simultaneously — exactly the gap the failed Explicit Execution Approval Review's own independent tracing exposed.

## 4. Exact correction

Added a private `Unit3COrchestrationDriver.runWarmups(executor: Unit3CTrialExecutor): Unit3CArmOutcome` method, and modified `runArm` to call it, unconditionally, as the first action whenever `family == Unit3CFamily.CONTROL`, before any of Control's own scored trials are attempted. `runWarmups`:

- executes exactly `Unit3CCampaignDefinition.warmupTrials` (3 trials, frozen order, iterated in list order);
- uses its own `Unit3CArmLedger`, rooted at `control/warmup/` — structurally separate from Control's own scored `control/` ledger, so warm-up observations can never be counted among, or confused with, the 145 scored Control observations;
- inherits the ledger's already-proven exact-once, duplicate-prevention, identity-drift-detection, and crash-recovery guarantees unchanged — no new durability logic was written, the existing, already-tested `Unit3CArmLedger` class was reused exactly as-is;
- on any `Unit3CArtifactIntegrityException` (identity drift, corrupted state), returns `Unit3CArmOutcome.HALTED` without propagating an exception past the arm boundary — consistent with every other measurement-invalidating-defect path in the driver;
- on success, returns `Unit3CArmOutcome.SEALED`, and only then does `runArm` proceed to Control's own scored 145-trial loop, unchanged from before this correction.

A warm-up failure halts the Control arm only — Family A, B, and C remain independently fault-isolated, exactly as the Scope Lock's own arm-isolation requirement already establishes for every other measurement-invalidating defect; no new, broader "halt the whole campaign" behavior was introduced, since none was explicitly required and introducing one would have exceeded "the minimum implementation necessary."

## 5. Code surface changed

Exactly one file: `tests/integration/ReasoningProtocolUnit3COrchestrationTest.kt`. One new private method (`runWarmups`) and a four-line modification to the start of `runArm`. Two pre-existing tests' stale expectations were updated to reflect the now-correct behavior (Section 7 below). Four new tests were added, proving the specific properties Phase 6 required. No other file was touched: not `ReasoningProtocolUnit3CControlledRemedyExperimentsTest.kt` (the schedule, fixtures, Family A/B/C mechanisms, artifact schema, and config loader are all byte-for-byte unchanged), not `build.gradle.kts`, not any frozen governance document.

## 6. Call-accounting proof

**483, now proven genuinely executable, not merely arithmetically derivable, by two structurally independent tests:**

1. `exact 483-call total is genuinely executed by the orchestration driver, including the 3 warm-ups` — runs the actual driver against counting fake executors and asserts the executor invocation count reaches exactly 483 (3 warm-up + 145 Control-scored + 220 Family A + 115 Family B + 0 Family C), with Control's own *reported* `completedTrialCount` remaining 145 (proving warm-ups do not contaminate the scored count).
2. `Unit3CCampaignDefinition`'s own pre-existing `init`-time structural check (`liveModelCallCount == 483`), independent of the driver, unchanged.

Both agree at 483. Additionally verified: no duplicate calls (`all three warm-ups are present, exactly once, in frozen order`; `crash recovery cannot duplicate already-completed warm-ups`); Family C remains exactly zero model calls (unchanged, re-verified); no hidden retry (the ledger's own duplicate-prevention logic, reused unchanged, already independently proven); artifact validation (the disk-space gate) makes zero executor calls before it passes (pre-existing test, re-confirmed still passing); safety-checkpoint handling makes zero additional calls (pre-existing test, re-confirmed still passing, unaffected by this change since it concerns scored trials, not warm-ups).

## 7. Governance invariance

Independently re-verified unchanged, item by item, per the task's own Phase 3 checklist: the three warm-up trial *definitions* (fixture, ID format, count) — unchanged, only their *execution wiring* changed; Control's own 145-trial schedule — unchanged; Family A's 220-trial schedule — unchanged; Family B's 115-trial schedule — unchanged; Family C's 29-trial, zero-call schedule — unchanged; all 29 fixtures — unchanged; repetition counts (n=5, n=1) — unchanged; Family A's decision/rendering mechanism — unchanged; Family B's candidate text and SHA-256 (`cfd5cb7f07d0d7da941b069e00b3f479fc5faf5e71662a83bda51f25bd629d60`) — unchanged, independently re-verified by re-running its own dedicated test; Family C's five-step mechanism and corrected 24/29 trace (four false positives `P03`/`P04`/`P05`/`P12`, one false negative `R03`) — unchanged, independently re-verified by re-running its own dedicated test; the artifact schema — unchanged; exact-once semantics — unchanged (the same `Unit3CArmLedger` class, reused, not modified); the safety checkpoint — unchanged; model strategy (`qwen2.5-coder:7b` only) — unchanged; inference configuration — unchanged; downstream isolation — unchanged, re-verified via a fresh import search; and the total governed schedule of 483 — unchanged as a number, now additionally proven executable.

Two pre-existing tests required updating, both because their own stated *expectations* were stale relative to the *newly correct* behavior, not because any governance or design changed: `driver prevents duplicate execution of an already-completed trial` expected Control's executor to be called 145 times; it is now correctly called 148 times (145 scored + 3 warm-up), and the test's assertion was updated to `148` accordingly, with its actual duplicate-prevention proof (a rejected append to a sealed ledger) unchanged. A newly-added test (`crash recovery cannot duplicate already-completed warm-ups`) was itself corrected during this task's own drafting: its first version incorrectly expected a full, idempotent second run over an already-sealed campaign to throw an exception — independently determined to be a wrong premise, since idempotent no-op recovery (skip every already-completed trial, re-verify, re-seal) is the *correct*, safe behavior, not an error condition; the test was rewritten to assert the executor is never re-invoked for any already-completed trial, which is the property that actually matters.

## 8. Zero live calls

Confirmed throughout: no `PARKER_REASONING_*` environment variable was set at any point during this task; every test uses a fake `Unit3CTrialExecutor`; no test invokes `Unit3CLiveEntryPoint.run` with real configuration; the two read-only Ollama identity calls made during the *prior* task's Approval Review (`/api/tags`, `/api/show`) were not repeated here, since this task performs no new identity verification of its own — that is reserved for the fresh Explicit Execution Approval Review.

## 9. Verdict

```text
DEFECT CONFIRMED AND CORRECTED
```

Ready for Independent Constitutional Defect Review.
