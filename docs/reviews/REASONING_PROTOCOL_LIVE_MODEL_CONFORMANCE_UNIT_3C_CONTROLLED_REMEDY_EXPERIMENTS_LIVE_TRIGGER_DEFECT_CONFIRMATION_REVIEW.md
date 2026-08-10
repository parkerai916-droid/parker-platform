**Status:** Unit 3-C Controlled Remedy Experiments — Live Trigger Defect Confirmation Review — **DEFECT CONFIRMED, independently re-derived (not assumed) from the committed Execution Evidence Review's own report.** This document determines the defect and the narrowest constitutionally valid correction; it does not itself implement the correction. No live model call, no HTTP call, no campaign, and no code/test/Gradle modification occurred during this review.

# Unit 3-C Controlled Remedy Experiments — Live Trigger Defect Confirmation Review

## 1. Baseline

`HEAD` = `origin/main` = `c4b840f87e53d2854629ef795679f44c94fc9f76`, independently re-confirmed clean at the start of this task. No Unit 3-C campaign directory exists. No `PARKER_REASONING_*` environment variable is set. No uncommitted implementation exists.

## 2. Method

Independently re-read, fresh, from committed source rather than from the Execution Evidence Review's own prose: `tests/integration/ReasoningProtocolUnit3CControlledRemedyExperimentsTest.kt` in full for every reference to `Unit3CLiveEntryPoint`; `tests/integration/ReasoningProtocolUnit3COrchestrationTest.kt` in full for the orchestration driver and artifact-root/disk-space gates; `build.gradle.kts` lines 108–160 for both the `liveModelEvaluation` source-set definition and both detached live Gradle tasks; and `tests/integration/ReasoningProtocolDiagnosticCharacterisationTest.kt` lines 1430–1479 as the Unit 2-D precedent. The Execution Evidence Review's own Section 27 proposed correction was deliberately not taken as given; it was used only as one candidate among the possibilities considered in Section 5 below.

## 3. Question A — is there genuinely no executable path from the detached Gradle task to a real, env/config-driven `Unit3CLiveEntryPoint.run` invocation?

**Confirmed: yes, genuinely no such path exists.**

`grep -rn "Unit3CLiveEntryPoint" tests/ src/ build.gradle.kts` returns exactly two matches in the entire tracked repository:

1. The object's own declaration (`tests/integration/ReasoningProtocolUnit3CControlledRemedyExperimentsTest.kt:772`).
2. One existing test, `live entry point fails closed absent required environment configuration` (same file, line 1405), which calls `Unit3CLiveEntryPoint.run(emptyMap(), Path.of("."))` — proving only that the function itself fails closed on empty configuration.

No `@Test` function anywhere calls `System.getenv()` and passes it to `Unit3CConfigLoader.load` or to `Unit3CLiveEntryPoint.run`. The `unit3cControlledRemedyExperiments` Gradle task (`build.gradle.kts:149-160`) sets exactly one system property (`parker.reasoning.unit3c.enabled = "true"`) and filters to the two Unit 3-C test classes; nothing in either class reads that property. Consequently, running that Gradle task — with any combination of `PARKER_REASONING_*` environment variables exported — executes only the existing 69 offline tests against fake executors; it structurally cannot reach a live HTTP call. This independently reproduces, by direct re-derivation rather than by trusting the prior report, the exact finding already recorded in the committed Execution Evidence Review (`c4b840f`, Section 24).

## 4. Question B — why did prior tests/reviews fail to detect it?

Every review in the Unit 3-C chain (Completion Review and its ICR, both prior Implementation Readiness Reviews and their ICRs, both prior Explicit Execution Approval Reviews) independently verified that `Unit3CLiveEntryPoint.run` is **internally** correct: it calls `requireDownstreamIsolated()`, then `Unit3CConfigLoader.load`, then `Unit3CArtifactRootPolicy.resolve`, then constructs `Unit3COrchestrationDriver` with real, live-calling executors, then calls `driver.run(executors)` — gate ordering, fail-closed behavior, and executor wiring were all re-derived from source and found sound, repeatedly. None of those reviews asked the separate, structurally distinct question "does anything in this repository actually *call* `Unit3CLiveEntryPoint.run` under real conditions?" — every one of them implicitly treated "the function is correct" as equivalent to "the function is reachable," which are not the same claim. This is precisely the same category of gap the warm-up orchestration defect exhibited one task earlier (a value correctly defined in the schedule but never actually referenced by the code that was supposed to consume it) — here, a function correctly defined but never actually referenced by any caller under live conditions. The gap was only surfaced because the immediately prior task, rather than trusting the accumulated review history, attempted to trace the *literal, concrete* invocation path before exporting any environment variable, and found none.

## 5. Question C — is the appropriate correction analogous to Unit 2-D's live `@Test` trigger, or is another already-established repository mechanism more appropriate?

**Independently determined: directly analogous to Unit 2-D's own precedent, but structurally simpler, because Unit 3-C's own architecture already differs from Unit 2-D's in a load-bearing way.**

Unit 2-D's own trigger test (`ReasoningProtocolDiagnosticCharacterisationTest.kt:1450-1477`) inlines config-loading, identity verification, harness construction, and campaign execution directly inside one `@Test` function's body, because Unit 2-D never built a single function that encapsulates all of those steps — the test itself *is* the entry point. Unit 3-C's implementation already differs: `Unit3CLiveEntryPoint.run(environment, repositoryRoot)` already encapsulates config-loading, artifact-root resolution, driver construction, real-executor construction, and execution, as a single, already-reviewed, already-governed function. Re-deriving Unit 2-D's full inlined pattern for Unit 3-C would duplicate logic `Unit3CLiveEntryPoint.run` already owns, which is unnecessary and would itself be a wider change than the defect requires. No other already-established repository mechanism (there is no third live-trigger pattern anywhere else in the codebase; only Unit 2-D's is precedented) is more appropriate. The correct, narrowest analogy is: reuse Unit 2-D's *two-stage `assumeTrue` gate* (system property, then a real campaign-ID environment variable) verbatim as the pattern, but let the gated body be a single call to the already-existing `Unit3CLiveEntryPoint.run(System.getenv(), Path.of("."))`, rather than re-inlining logic that function already owns.

## 6. Question D — can the correction remain entirely within test/integration and Gradle test-tier execution surfaces?

**Independently confirmed: yes.** The new trigger is exactly one `@Test` function added to `tests/integration/ReasoningProtocolUnit3CControlledRemedyExperimentsTest.kt`, in the `parker.integration` package, within the `liveModelEvaluation` source set — the same file, same class, same source set as the existing fail-closed test it sits beside. It requires no new Gradle task: the existing `unit3cControlledRemedyExperiments` task's `includeTestsMatching("parker.integration.ReasoningProtocolUnit3CControlledRemedyExperimentsTest")` filter already matches by whole class name, so a new test method added to that class is automatically included without any `build.gradle.kts` change — independently confirmed by the fact that this exact pattern already causes all existing tests in that class (not just some) to run under the detached task today. No production `src/**` file is touched; `Unit3CLiveEntryPoint.run` itself is called, not modified.

## 7. Question E — does the correction require any production code / Scope Lock amendment / Implementation Plan amendment / fixture change / remedy mechanism change / call-count change / inference-configuration change?

**Independently derived, not assumed: no, on every count.**

- **Production code (`src/**`):** no. The defect is the absence of a caller, not a defect in any production class; nothing under `src/` is referenced by the fix.
- **Scope Lock / Implementation Plan amendment:** no. Both documents already specify that live execution occurs via the detached Gradle task gated by explicit environment configuration; neither document specifies (or omits) the internal mechanics of how the test tier wires that gate to `Unit3CLiveEntryPoint.run` — that is an implementation-tier concern the Plan delegated to the implementation task, exactly as Unit 2-D's own equivalent wiring was never separately specified in any Unit 2-D-governing document either.
- **Fixture change:** no. No fixture text, corpus, or expected-action value is touched.
- **Family A/B/C remedy mechanism change:** no. `FamilyADecisionPromptBuilder`, `FamilyARenderingPromptBuilder`, `FamilyBCandidatePromptBuilder`, `FAMILY_B_CANDIDATE_SELECTION_GUIDANCE`/its SHA-256, and `Unit3CCandidateC1` are not referenced by the correction.
- **Call-count / repetition-schedule change:** no. `Unit3CCampaignDefinition` (483 total, 3/145/220/115/0 breakdown) is not touched; the correction only adds a caller of code that already produces exactly that schedule.
- **Inference-configuration change:** no. Endpoint semantics, timeout, model identity, and request-body shape are all owned by `Unit3CLiveEntryPoint.run`/`Unit3CConfigLoader`/the shared `LiveEvaluationConfigLoader`, none of which the correction modifies.

## 8. Defect classification

**Structural omission (missing caller), not a logic defect.** Every gate, validation, and execution step the correction depends on already exists, is already implemented, and was already independently tested for its own internal correctness. The single missing element is a test-tier function that, under explicit opt-in conditions only, actually calls it with real arguments.

## 9. Confirmation

The defect reported in the committed Execution Evidence Review (`c4b840f`) is **independently confirmed accurate** by fresh re-derivation from source, not by trusting that review's own prose. The narrowest constitutionally valid correction is: one new `@Test` function in `ReasoningProtocolUnit3CControlledRemedyExperimentsTest.kt`, gated by `assumeTrue` on `System.getProperty(UNIT_3C_PROPERTY) == "true"` and then on a non-blank `System.getenv(Unit3CConfigLoader.CAMPAIGN_ID)`, whose body — once both gates pass — is exactly one call to `Unit3CLiveEntryPoint.run(System.getenv(), Path.of("."))`. This requires no `build.gradle.kts` change, no production code change, and no governance-document amendment.

## 10. Verdict

```text
DEFECT CONFIRMED — PROCEED TO PHASE 4 NARROW IMPLEMENTATION
```
