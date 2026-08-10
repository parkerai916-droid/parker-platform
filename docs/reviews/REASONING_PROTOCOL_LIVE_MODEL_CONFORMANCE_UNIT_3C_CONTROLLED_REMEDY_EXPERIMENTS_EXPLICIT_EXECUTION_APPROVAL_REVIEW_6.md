**Status:** Unit 3-C Controlled Remedy Experiments — Explicit Execution Approval Review (6) — **AUTHORIZED.** This document authorizes exactly one future campaign execution and does not itself execute it. It is a wholly fresh review, independently re-verifying the repository, the execution path, and — specifically — that the observation-durability correction committed at `deba366` is genuinely present on the live call path, not merely that a corrected function exists somewhere in source. It does not treat any prior Completion, Readiness, or Approval verdict as proof of the current state; every claim below is independently re-derived. Only read-only `/api/tags` and `/api/version` calls occurred during this review; no `/api/generate` call, no campaign directory creation or mutation, and no code/test/Gradle modification occurred.

# Unit 3-C Controlled Remedy Experiments — Explicit Execution Approval Review (6)

## 1. Baseline verification (Section 1)

`git rev-parse HEAD` = `deba3666839c2ad88f54e6582fc13cf1358ba088`. `git rev-parse origin/main` = `deba3666839c2ad88f54e6582fc13cf1358ba088` (after `git fetch origin`). `HEAD == origin/main`: confirmed. `git status`: clean, nothing to commit. Matches the expected baseline exactly, independently re-confirmed, not assumed from the task prompt. Last commit: `deba366 fix: correct Unit 3-C observation durability`, touching exactly `tests/integration/ReasoningProtocolUnit3COrchestrationTest.kt` and six governance documents (two new: the Observation Durability Defect Confirmation Review and its Independent Constitutional Review; four refreshed in place: Completion Review, Completion ICR, Readiness Review, Readiness ICR). Nothing else.

## 2. Governance read fresh

Read in full for this task, not from summary: Unit 3-C Scope Lock (`fee2edd`); Implementation/Execution Plan, specifically Section 16's artifact schema (`a188284`/`08f3692`); Timeout + Durability Scope Lock Amendment and Implementation Plan Amendment (`0072c01`); Scored-Trial Timeout Semantics Determination (`144cf51`); Evidence Completeness and Durability Determination and its ICR (`4430e8b`); the Observation Durability Defect Confirmation Review and its ICR (`deba366`); the current (sixth-refresh) Completion Review and Completion ICR (`PASS`/`ACCEPTED`); the current (sixth-refresh) Implementation Readiness Review and Readiness ICR (`READY`/`ACCEPTED`); Explicit Execution Approval Review 5 (`AUTHORIZED`, examined `e06f85c`); the full Execution Evidence Review and its Independent Constitutional Review through Attempt 5. Independently verified via `git log --oneline` on each file that none has changed since last read — confirmed unmodified since their own governing commits, none touched by `deba366` except the six listed above.

## 3. Attempt 5 preservation (Section 2)

Campaign `unit3c-remedy-experiments-20260810-02`: read-only inspection only, confirmed untouched. 26 files, independently re-hashed: `control/warmup/raw.jsonl` → `f61fcd867ead44d392b5bbfb9391ce0af8ae3cd06891373e507d3d111e8b60f0`, matching `control/warmup/manifest.txt`'s own recorded value and every prior review's citation, byte-for-byte. Every file's mtime (earliest `1786351242`, latest `1786353928`) independently re-confirmed unchanged before and after this review's own offline test run — a second, independent capture taken after the full regression suite ran shows identical timestamps to the first capture, confirming no write occurred at any point during this task. Attempt 3's own preserved directory (`unit3c-remedy-experiments-20260810`) also re-verified: `control/warmup/identity.txt` → `56af7ca3fa84b1e3c6aca3d4fdd2a23f5884cc5a0ff5a7b574fe2d663d62c9c8`, unchanged. Unit 2 (`c635ebcd...`) and Unit 2-D (all three: `c12de361...`, `568f08f2...`, `d542f0ed...`) independently re-hashed and unchanged. **No new campaign may, and none does, use `unit3c-remedy-experiments-20260810-02` or `unit3c-remedy-experiments-20260810`.**

## 4. Observation-durability correction — live-path proof (Section 3)

Independently re-traced the full chain from source, not merely confirmed the corrected function's existence:

1. **`Unit3CLiveEntryPoint.run`** (`ReasoningProtocolUnit3CControlledRemedyExperimentsTest.kt:949`) constructs `Unit3COrchestrationDriver(config.campaignId, artifactRoot, config.identity)` and calls `driver.run(executors)`.
2. **`Unit3COrchestrationDriver.run`** dispatches to `runWarmups`/`runArm` for each arm in frozen order.
3. **`runWarmups`** (line 557) and **`runArm`** (line 599), both independently re-read in full: each calls `ledger.appendObservation(trial.id, encodeObservation(observation))` — lines 589 and 665 respectively.
4. **`encodeObservation`** (line 299): independently re-read character by character. Serializes `campaignId`, `family`, `arm`, `fixtureId`, `fixtureCategory`, `contextProfileId`, `trialSequence`, `expectedAction`, `actualAction`, `semanticCorrect`, `representationValid`, `contentFidelity`, `modelName`, `modelDigest`, `runtimeIdentity`, `endpointIdentifier`, `timeoutMs`, `inferenceConfigIdentity`, `promptIdentity`, `parserResult`, `parserFailure`, `latencyNanos`, `transportOutcome`, `candidateMechanismIdentity`, `stableInputHash`, `repositoryCommit` — every field the Plan's Section 16 schema requires that the experiment actually computes, excluding only `prompt`/`rawRequest`/`rawResponse` per the Determination's own accepted minimum-evidence analysis (Section 6 below).
5. **No shadowing member exists.** Independently re-read the full body of `class Unit3COrchestrationDriver` (lines 484–685): the only two references to `encodeObservation` inside the class are the two call sites themselves; no private member of that name exists anywhere in the class. Both call sites therefore resolve to the single, corrected, top-level function at line 299 — verified structurally, not merely by absence of a compiler warning.
6. **`Unit3CArmLedger.appendObservation`** writes the corrected payload to `raw.jsonl`, unmodified in its own outer format, checkpoint timing, or duplicate-prevention logic (independently re-confirmed via `git diff e06f85c HEAD -- tests/integration/ReasoningProtocolUnit3CControlledRemedyExperimentsTest.kt`, which is **empty** — the file containing `Unit3CArmLedger`, `Unit3CObservation`, `buildModelInvokingExecutor`, `buildFamilyCExecutor`, and `Unit3CLiveEntryPoint` itself has not changed at all since Approval Review 5 examined it).

**This is not merely "a corrected function exists" — it is the same, unmodified entry-point wiring Approval Review 5 already verified, with the one function it calls now corrected, and independently re-confirmed to be the only function of that name reachable from either call site.**

## 5. Section 16 schema preservation result (Section 3)

Independently confirmed via direct source comparison against the Plan's own Section 16 table (re-read fresh, Section 2 above): every field the table specifies that `Unit3CObservation`'s constructor actually populates today is present in `encodeObservation`'s output. Fields never computed by the experiment (`rawRequest`, `rawResponse` — hardcoded `null` in `buildModelInvokingExecutor`/`buildFamilyCExecutor`, independently re-confirmed unchanged) are not fabricated; they remain absent from the durable record exactly as they are absent from the in-memory object, consistent with the governed prohibition on fabricating values. `prompt` is computed transiently but deliberately excluded from persistence — this is the Determination's own accepted minimum, not an omission this review discovered.

## 6. `contentFidelity` status (Section 3, explicit)

**Non-blocking.** `contentFidelity` is hardcoded `null` in both `buildModelInvokingExecutor` and `buildFamilyCExecutor`, unconditionally, independently re-confirmed via direct source read — this is a separate, pre-existing non-computation gap, not newly introduced and not touched by `deba366` (that file is entirely absent from `deba366`'s diff, Section 4 above). No frozen governance document — independently re-checked the Scope Lock, the Plan, both Amendments, and the Determination and its ICR — requires `contentFidelity` to be computed as a precondition for further live execution; the Determination's own Section 7 explicitly classifies this as "NOT REQUIRED FOR THE GOVERNED QUESTION, AS CURRENTLY SCOPED," and its ICR (qualification 1) flags it as open without making it blocking. This review does not implement it, does not treat its absence as a defect requiring correction before authorization, and records it here so it is not silently forgotten a third time.

## 7. Timeout and exact-once verification (Section 4)

Independently re-read `UNIT_3C_TIMEOUT_MS = 90_000L` (line 57) and its sole consumer, `Unit3CConfigLoader.load`'s `require(live.timeoutMs == UNIT_3C_TIMEOUT_MS)` (line 539) — unchanged, active. Independently re-read `Unit3CArmLedger.recordIntent`, `recordTimeout`, `recover()`, `appendObservation`, and `Unit3CSafetyCheckpoint` (all in `ReasoningProtocolUnit3COrchestrationTest.kt`) and independently confirmed, via `git diff deba366~1 deba366 -- tests/integration/ReasoningProtocolUnit3COrchestrationTest.kt`, that none of these functions appears in the diff at all — only `encodeObservation` (rewritten, relocated to top level) and net-new test code changed. Four-state exact-once semantics (never-transmitted / transmitted-and-completed / transmitted-and-timed-out / ambiguous-terminal-state), no-automatic-retry, warm-up campaign-halt semantics, scored-trial arm-continuation semantics, and the `MODEL_TIMEOUT`/`TRANSPORT_OR_PROVIDER_FAILURE`/`AMBIGUOUS` transport distinction are all independently re-confirmed byte-identical to what Approval Review 5 examined. **The observation-durability correction did not touch, and could not have weakened, any of these mechanisms — they live in different functions the diff never reaches.**

## 8. Experimental invariance (Section 5)

`git diff --stat e06f85c HEAD -- tests/integration/ReasoningProtocolUnit3CControlledRemedyExperimentsTest.kt` is **empty**. This is the file containing `Unit3CBaseCorpus`, `Unit3CSupplementalCorpus`, `Unit3CCampaignDefinition` (schedule), `Unit3CCandidateC1` (Family C), the Family A/B prompt builders, `DefaultReasoningPromptBuilder` usage (Control), `Unit3CArtifactRootPolicy`, `UNIT_3C_MINIMUM_FREE_BYTES`, `isAdversarialCategoryFalsePositive`'s call site context, and `Unit3CLiveEntryPoint` itself. **Byte-for-byte unchanged since Approval Review 5.** `git diff --stat e06f85c HEAD -- build.gradle.kts` and `-- src/` are both independently confirmed empty.

Freshly re-derived schedule, from source, not from any prior document's arithmetic: `WARMUP_ATTEMPTS = 3`; Control = `(23 base + 6 supplemental) × 5 = 145`; Family A = `23 × 5 (decision) + 21 × 5 (render, excluding the 2 NOACTION fixtures) = 115 + 105 = 220`; Family B = `23 × 5 = 115`; Family C = `0` model calls (29 fixtures × 1 rep, deterministic, offline). **Total = 3 + 145 + 220 + 115 + 0 = 483**, matching the compile-time-checked `init { check(liveModelCallCount == 483) }` invariant, independently re-confirmed still present and still passing on this review's own fresh test run.

Control/Family A/Family B/Family C mechanisms, fixtures, expected actions, repetition counts, ordering, the safety checkpoint (`isAdversarialCategoryFalsePositive`, non-numeric first-occurrence trigger), model (`qwen2.5-coder:7b` only), `Unit3CArtifactRootPolicy`, the 2 GiB disk-space threshold, and downstream isolation — all independently re-confirmed unchanged, all in the untouched file. **The correction affected evidence preservation only, never experimental treatment.**

## 9. Test and repository verification (Section 6)

Run fresh, no live environment variable set before, during, or after (`env | grep -iE "unit3c|parker_reasoning"` returned nothing before the run):

```
./gradlew clean test reasoningProtocolBaselineCharacterisation reasoningProtocolUnit2DDiagnostic reasoningProtocolLiveModelEvaluation unit3cControlledRemedyExperiments
```

**BUILD SUCCESSFUL.** Independently parsed XML directly (not Gradle's own console summary):

| Task | Tests | Skipped | Failures | Errors |
|---|---|---|---|---|
| `test` (full repository) | 2015 | 5 | 0 | 0 |
| `reasoningProtocolBaselineCharacterisation` (Unit 2) | 19 | 1 | 0 | 0 |
| `reasoningProtocolUnit2DDiagnostic` (Unit 2-D) | 27 | 1 | 0 | 0 |
| `reasoningProtocolLiveModelEvaluation` (general offline harness, includes Unit 1) | 163 | 4 | 0 | 0 |
| `unit3cControlledRemedyExperiments` (targeted Unit 3-C) | 100 | 1 | 0 | 0 |

No test unexpectedly reached `/api/generate` — independently confirmed via `ps aux`/network activity absent during the run and via the durable campaign artifacts' own unchanged mtimes (Section 3). `git diff --check`: clean, no output. `git status` (post-test): clean, nothing to commit. No campaign artifact changed (Section 3, re-verified after this run).

## 10. Live environment readiness (Section 7)

Read-only checks only, no `/api/generate` call issued:

- `/api/tags`: `qwen2.5-coder:7b` present, digest `dae161e27b0e90dd1856c8bb3209201fd6736d8eb66298e75ed87571486f4364` — matches every prior review's own capture across this entire programme, including Approval Review 5's.
- `/api/version`: `0.32.5` — unchanged.
- Ollama daemon reachable and responsive (`/bin/ollama serve`, running).
- Artifact root `/var/lib/parker/reasoning-protocol-live-model`: owned `steve:steve`, mode `0700` — unchanged.
- Free space: `4,260,544,512` bytes available on the artifact root's filesystem — above the governed `2,147,483,648`-byte (2 GiB) minimum, with `2,113,060,864` bytes (≈1.97 GiB) of margin.
- Proposed fresh campaign directory (`unit3c-remedy-experiments-20260810-03`, Section 11 below): independently confirmed absent from `/var/lib/parker/reasoning-protocol-live-model/`.

No live campaign environment variable was exported at any point during this review.

## 11. Fresh campaign ID (Section 8)

```text
unit3c-remedy-experiments-20260810-03
```

Distinct from every prior Unit 3-C campaign ID (`unit3c-remedy-experiments-20260810`, `unit3c-remedy-experiments-20260810-02`) and from both preserved Unit 2/Unit 2-D IDs. Independently re-confirmed absent on disk. Independently re-confirmed to satisfy `Unit3CArtifactRootPolicy`'s own machine-safe regex and `unit3c-remedy-experiments-` marker-prefix requirement, and to not appear in `UNIT_3C_PRESERVED_CAMPAIGN_IDS`. Bound to commit `deba3666839c2ad88f54e6582fc13cf1358ba088`. The directory is **not** created by this review.

## 12. Independent Constitutional Check — adversarial review of the proposed authorization (Section 10)

**1. Is the corrected observation encoder genuinely reached by the live campaign?** Yes — Section 4 traces the unbroken chain from `Unit3CLiveEntryPoint.run` to `encodeObservation`'s corrected body, with no shadowing member and no alternate code path.

**2. Could any completed LLM observation still be durably reduced to the old three-field representation?** No. The old private-member `encodeObservation` (the one that produced `"campaignId=...|family=...|fixtureId=..."`) no longer exists anywhere in the file — independently re-confirmed via `grep -c "campaignId=\${observation.campaignId}|family="` returning zero matches in the current source.

**3. Could a timeout, crash, or restart produce a duplicate call?** No. `Unit3CArmLedger.recover()`'s four-state logic, `checkIdentity`'s identity-drift fail-closed behavior, and the intent-before-call/no-automatic-retry discipline are all in functions `deba366`'s diff never touches (Section 7), and are independently re-confirmed unchanged by direct diff inspection, not by re-trusting the tests that exercise them.

**4. Could Attempt 5 accidentally be resumed?** No, by two independent layers: (a) this review's own authorization boundary (Section 13) names only `unit3c-remedy-experiments-20260810-03`; (b) even a mistaken reuse of `unit3c-remedy-experiments-20260810-02` would mechanically fail closed — that campaign's own recorded identity (`6931fabcd2588bb1cce6279c39bde18eb30028bb|...`) does not match the current commit (`deba3666...`), so `Unit3CArmLedger.checkIdentity` would throw `Unit3CArtifactIntegrityException` ("identity drift detected") before any call, independently re-verified by direct string comparison in Section 3.

**5. Has any experimental mechanism changed while fixing durability?** No — Section 8's empty diff on `ReasoningProtocolUnit3CControlledRemedyExperimentsTest.kt` (the file holding every experimental mechanism) is independent, structural proof, not an inference from test passage.

**6. Is the proposed campaign tied to the exact reviewed commit?** Yes — `deba3666839c2ad88f54e6582fc13cf1358ba088`, the same commit this entire review examined, independently re-confirmed as current `HEAD` immediately before drafting this section.

**7. Could ordinary `./gradlew test` reach the live campaign?** No — independently re-confirmed `build/test-results/test/` contains zero Unit 3-C result files after this review's own fresh `clean test` run, and `build.gradle.kts` (which defines the structural source-set/task isolation) is unchanged since Approval Review 5.

**8. Is any live-task-incompatible test accidentally selected?** No — `unit3cControlledRemedyExperiments`'s own `excludeTags("unit3cLiveTaskIncompatible")` is unchanged in `build.gradle.kts`; the one test carrying that tag (line 1606 of the schedule/mechanism file) is itself unchanged, in the untouched file.

**9. Is the campaign artifact path valid before campaign creation?** Yes — `Unit3CArtifactRootPolicy.resolve` (unchanged) independently re-verified via its own regex/prefix/preserved-ID logic against the proposed ID (Section 11); the resulting path does not yet exist (Section 10).

**10. Does the approval accidentally authorize Unit 3-D or remedy selection?** No — Section 13 below states this exclusion explicitly and this review performs no comparison, ranking, or selection anywhere in its own text.

**No adversarial challenge exposed a blocking defect.**

## 13. Authorization boundary

This review authorizes **exactly one** future Unit 3-C live campaign, under **all** of the following, simultaneously:

```text
Campaign ID:            unit3c-remedy-experiments-20260810-03
Repository commit:      deba3666839c2ad88f54e6582fc13cf1358ba088
Model:                  qwen2.5-coder:7b
Model digest:           dae161e27b0e90dd1856c8bb3209201fd6736d8eb66298e75ed87571486f4364
Runtime identity:       host parker, Ollama 0.32.5
Endpoint:               http://127.0.0.1:11434/api/generate
Timeout:                90000 ms
Artifact root:          /var/lib/parker/reasoning-protocol-live-model
Frozen maximum schedule: 483 live model calls (3 warm-up + 145 Control + 220 Family A + 115 Family B + 0 Family C)
```

Also bound, unmodified, exactly as committed at `deba3666839c2ad88f54e6582fc13cf1358ba088`: current intent-before-call durability; current observation durability (the corrected `encodeObservation`, Section 4); current terminal-timeout durability; current four-state exact-once semantics; current scored-trial timeout semantics (model-side timeout continues the arm; transport/provider failure or ambiguous state halts the arm; warm-up failure of any classification halts the whole campaign); current non-numeric, first-occurrence adversarial-category safety checkpoint; current Control/Family A/Family B/Family C mechanisms exactly as committed, with **zero post-hoc modification during execution**; no automatic retry under any circumstance; invocation exclusively via the gated live trigger under the detached `unit3cControlledRemedyExperiments` Gradle task.

**Explicitly not authorized by this review:** a second campaign under any ID; reuse, resumption, or repair of `unit3c-remedy-experiments-20260810-02` or `unit3c-remedy-experiments-20260810`; any Unit 3-D comparative-evaluation work; any remedy selection or ranking; any Unit 4 work; any correction of `contentFidelity`'s own non-computation or of any other newly-discovered defect — a defect found during a future execution attempt must be documented and referred to a separate, narrowly-scoped task, exactly as every prior attempt in this programme has done, not patched in place. This authorization lapses if the repository commit executed against differs from `deba3666839c2ad88f54e6582fc13cf1358ba088`, or if any mechanism examined in Sections 4–9 above is modified before execution.

## 14. Prohibited actions confirmed not taken

No `/api/generate` call — only `/api/tags` and `/api/version`. No live Gradle task invocation. No campaign directory created. No environment variable exported in any way that would activate execution. No code, test, or Gradle modification during this review. No governance amendment. No remedy selected. No Unit 3-D work performed. Nothing staged, committed, or pushed.

## 15. Final verdict

```text
AUTHORIZED
```

**This document authorizes exactly one future campaign execution, under the exact boundary stated in Section 13, and does not itself execute it.** The observation-durability correction committed at `deba366` is independently re-confirmed genuinely present on the real, unchanged live-execution path — not merely as an isolated function, but as the sole resolution target of both real call sites, with the file containing every other experimental and durability mechanism (`ReasoningProtocolUnit3CControlledRemedyExperimentsTest.kt`) independently confirmed byte-for-byte unchanged since Approval Review 5. Every governed mechanism (timeout, exact-once, warm-up/scored-trial semantics, transport distinction, safety checkpoint, the 483-call schedule) is independently re-traced from committed source, not from prior review prose. Attempt 5 and Attempt 3 remain preserved, untouched, and unreused. `contentFidelity`'s own separate non-computation is explicitly non-blocking and unresolved by this review, exactly as governance requires. A fresh, distinct, unused campaign identity has been determined. The next step, outside this review's own scope, is the actual execution under this exact boundary — not performed here.

## 16. Confirmation

Nothing was staged, committed, or pushed during this review. No `/api/generate` call was made. No campaign was created, resumed, modified, or deleted. No production `src/**` file, test file, or `build.gradle.kts` was modified. No frozen governance document was amended. No remedy was selected or ranked. No Unit 3-D or Unit 4 work was performed.
