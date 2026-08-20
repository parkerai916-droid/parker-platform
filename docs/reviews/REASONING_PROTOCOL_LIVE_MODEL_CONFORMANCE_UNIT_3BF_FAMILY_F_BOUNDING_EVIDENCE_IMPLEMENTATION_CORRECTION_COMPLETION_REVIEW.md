**Status:** Fresh Independent Implementation Completion Review of the corrected Unit 3-BF Family F Bounding Evidence offline implementation, performed after the historical `REVISE BEFORE ACCEPTANCE` review and the subsequent bounded correction task — **ACCEPTED.** This review preserves, and does not overwrite, the historical `REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_BOUNDING_EVIDENCE_IMPLEMENTATION_COMPLETION_REVIEW.md` (verdict `REVISE BEFORE ACCEPTANCE`) as part of the audit trail. Every one of that review's six blocking/flagged defects was independently re-derived from primary source and re-verified fresh in this task — not inherited — and each is confirmed genuinely cured: the model-role source of truth is corrected and mechanically honored end to end (independently re-confirmed by a fresh, standalone runtime invocation of the recompiled estimator, outside any prior task's output); the double-gated entry point now has a real test that genuinely invokes the real `produce` operation exactly once when, and only when, both gates are satisfied; WP-B/C/D now perform real, testable, deterministic offline admissibility checks with positive and negative coverage rather than unconditional stubs; recovery/resume is implemented and independently exercised across clean, partial, inconsistent, torn, complete, and failed states with zero ledger-record duplication proven by direct count; the source-inspection exclusion region is narrowed to match repository precedent exactly; and the stale documentation is corrected and now accurate. This review's own fresh defect search found two new, narrow, non-blocking robustness gaps (a malformed-ledger-line swallow path, and a never-deleted copy-verification temp directory) that do not compromise any governed requirement under this Decision's own narrow implementation-and-offline-verification authorization, but which should be closed before any future Evidence Production Authorization Decision relies on this tool. No blocking defect was found. Authorization boundaries remain fully intact.

# Unit 3-BF Family F Bounding Evidence — Implementation Correction Completion Review

## 1. Preconditions

```text
$ git rev-parse --abbrev-ref HEAD
main
$ git rev-parse HEAD
a88e2e3d5f2022c340618783d5b6b9d97d7a21d6
$ git rev-parse origin/main
a88e2e3d5f2022c340618783d5b6b9d97d7a21d6
$ git status --short
 M build.gradle.kts
 M tests/integration/ReasoningProtocolFamilyFDiagnosticTest.kt
?? docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_BOUNDING_EVIDENCE_IMPLEMENTATION_COMPLETION_REVIEW.md
?? tests/integration/ReasoningProtocolFamilyFBoundingEvidenceTest.kt
```

All three preconditions matched exactly. No STOP triggered.

## 2. Governance sources read fresh

- Family F Model Role and Research Question Scope Lock, Section B (model roles), re-read fresh.
- Its historical first ICR and accepted correction ICR — read for scope, not treated as authoritative for current implementation state.
- Family F Alternative-Model Diagnostic Planning Review + accepted Model Role Amendment + ICR.
- Experimental Reclassification Scope Lock + accepted Amendment + ICR.
- Capture-Proxy Bounding Scope Lock + accepted Amendment + ICR.
- Bounding Evidence Acquisition and Offline Estimator Plan, Sections 5-28 (implementation boundary, WP-A through WP-E, ledger/recovery, offline-verification requirements), re-read fresh.
- Accepted Plan Model Role Amendment + ICR.
- Accepted Bounding Evidence Implementation Authorization Decision, Sections 5-18, re-read fresh.
- Its accepted ICR.
- Accepted `FamilyFRole` Source Correction Amendment + accepted ICR.
- The historical `REVISE BEFORE ACCEPTANCE` Implementation Completion Review, read in full for what it found — not for what it concluded should now be true.

## 3. Implementation sources reviewed

- `build.gradle.kts` — full diff, re-read fresh (Section 4 below).
- `tests/integration/ReasoningProtocolFamilyFDiagnosticTest.kt` — full diff, and the `FamilyFRole` enum, `FamilyFCorpus`, and constant declarations, re-read fresh.
- `tests/integration/ReasoningProtocolFamilyFBoundingEvidenceTest.kt` — read in full, 1640 lines, end to end, not sampled.
- `tests/integration/ReasoningProtocolFamilyFDiagnosticOrchestrationTest.kt` — `FamilyFCampaignDefinition`, re-read to independently re-derive campaign structure (Section 6).
- `src/runtime/ReasoningPromptBuilder.kt` — `DefaultReasoningPromptBuilder.buildPrompt`, re-read to confirm purity/no I/O (Part D, Part J).
- `src/runtime/ModelInferenceClient.kt` — `defaultOllamaRequestBody`, re-confirmed a pure string formatter.
- Repository-wide `grep` sweeps for `FamilyFRole`, `FAMILY_F_SUBJECT_MODEL_NAME`, `FAMILY_F_CONTROL_MODEL_NAME`, the two physical model-name literals, `SUBJECT_MODEL`, `CONTROL_MODEL`, and a suspicious-API sweep (`Socket`, `HttpClient`, `ProcessBuilder`, `Docker`, `Ollama`, etc.), all run fresh for this review.

## 4. Part A — authorized file surface

```text
$ git diff --stat
 build.gradle.kts                                            | 13 +++++++++++++
 tests/integration/ReasoningProtocolFamilyFDiagnosticTest.kt  |  4 ++--
 2 files changed, 15 insertions(+), 2 deletions(-)
$ git diff -- build.gradle.kts | wc -l
16
$ git diff -- tests/integration/ReasoningProtocolFamilyFDiagnosticTest.kt
(exactly the two-line SUBJECT/CONTROL constant swap; independently re-read in full above)
$ git diff --stat -- src/
(no output)
```

1. **Files governance authorized:** Decision Section 5 names exactly `build.gradle.kts` and `tests/integration/ReasoningProtocolFamilyFBoundingEvidenceTest.kt`. The accepted `FamilyFRole` Source Correction Amendment separately, narrowly authorizes exactly lines 62-63 of `tests/integration/ReasoningProtocolFamilyFDiagnosticTest.kt`.
2. **Files that currently differ from accepted HEAD:** exactly `build.gradle.kts` (+13/-0, one detached task registration) and `tests/integration/ReasoningProtocolFamilyFDiagnosticTest.kt` (+2/-2, the two named constant values) — independently confirmed via `git diff --stat`.
3. **Every change within accepted authority:** yes — the `build.gradle.kts` diff is confined to one new `tasks.register<Test>(...)` block (Decision Section 6); the diagnostic-test diff is confined to exactly the two lines the `FamilyFRole` Amendment names (Section 2 of that amendment).
4. **`FamilyFRole` source correction separately authorized:** confirmed — independently re-read the accepted Amendment's Section 2 and 7 (the Decision Section 5 carve-out) and its accepted ICR's Finding 7 (independently re-verifying the exact same two-line scope). The diff matches that authorization exactly, character for character.
5. **Unauthorized production/runtime/configuration change:** none found — `git diff --stat -- src/` returns no output; no Docker, persistence, QMD, UI, parser, or model-configuration file appears anywhere in the diff.

**Finding: confirmed sound, no boundary exceeded.**

## 5. Part B — model role source of truth

```text
$ grep -n "FAMILY_F_SUBJECT_MODEL_NAME\|FAMILY_F_CONTROL_MODEL_NAME\|enum class FamilyFRole" \
    tests/integration/ReasoningProtocolFamilyFDiagnosticTest.kt
62:const val FAMILY_F_SUBJECT_MODEL_NAME = "llama3.2:3b"
63:const val FAMILY_F_CONTROL_MODEL_NAME = "qwen2.5-coder:7b"
74:enum class FamilyFRole(val modelName: String) {
75:    SUBJECT(FAMILY_F_SUBJECT_MODEL_NAME),
76:    CONTROL(FAMILY_F_CONTROL_MODEL_NAME),
```

Independently cross-checked against the Scope Lock's own frozen Section B (re-read fresh, Section 2 above): `DEPLOYED_BASELINE = qwen2.5-coder:7b`, `CONTROL_MODEL = qwen2.5-coder:7b`, `SUBJECT_MODEL = llama3.2:3b` — **exact match.**

- **FamilyFRole authoritative source contains the correct mapping:** confirmed above.
- **Estimator consumes it directly:** independently re-read `FamilyFBoundingEvidenceRequestEstimator.estimate()` (lines 158-197 of the implementation file) — `val modelName = trial.role.modelName` (line 170) and `role = trial.role.name` (line 183) are direct, unmediated reads of the frozen enum; no intermediate table, cache, or remapping layer exists anywhere in the function.
- **No second handwritten role/model mapping exists:** `grep -n '"qwen2\.5-coder:7b"\|"llama3\.2:3b"' tests/integration/ReasoningProtocolFamilyFBoundingEvidenceTest.kt` returns **zero matches** — not even in a comment (the header prose states the mapping without quoted literals). The only source of truth for either physical model-name string in the entire bounding-evidence file is the imported `FamilyFRole` enum.
- **No output-layer relabelling workaround exists:** confirmed by the same absence of any literal or conditional remapping; `modelName` and `role` in every `FamilyFBoundingRequestRecord` are the frozen enum's own values, unaltered.
- **Generated records carry correct model identity:** independently verified by a fresh, standalone, out-of-repo runtime invocation of the freshly recompiled `FamilyFBoundingEvidenceRequestEstimator.estimate()` (Section 12 below) — this is INDEPENDENT REVIEW EVIDENCE, not the implementation task's own prior run.

**Finding: confirmed cured, independently re-derived rather than inherited.**

## 6. Part C — campaign structure

Independently re-read `FamilyFCampaignDefinition` (`ReasoningProtocolFamilyFDiagnosticOrchestrationTest.kt`) and its own `init` block assertions, plus `FamilyFCorpus` (23 fixtures, 1 warm-up fixture, 2 profiles):

```text
FIXTURE_COUNT = 23             (FamilyFCorpus.fixtures.size, independently counted from the frozen corpus)
CONTEXT_PROFILE_COUNT = 2      (FamilyFCorpus.profiles: MINIMAL_PRODUCTION_CONTEXT, MIXED_FULL_PRODUCTION_LIKE)
REPETITIONS = 4                (1..4 in FamilyFCampaignDefinition.allTrials' outer loop)
ROLE_COUNT = 2                 (FamilyFRole.SUBJECT, FamilyFRole.CONTROL)
SCORED_CALLS = 23 x 2 x 4 x 2 = 368
WARM_UP_CALLS = 8 residency blocks (4 repetitions x 2 roles) x 3 warm-ups = 24
TOTAL_CALLS = 368 + 24 = 392
```

This is not merely re-stating constants: `FamilyFCampaignDefinition`'s own `init` block independently `check()`s `allTrials.count { SCORED } == 368`, `allTrials.count { WARMUP } == 24`, `allTrials.size == 392`, `184` scored per role, `8` blocks of `49` trials each — a structural, class-initialization-time guarantee, not a value the estimator could silently drift from. The estimator's own test (`estimator covers both frozen model names...`) independently re-derives per-(role, profile) cell coverage (`23 x 4 = 92` per cell) rather than trusting an aggregate count.

**Finding: campaign structure independently confirmed correct, and structurally guaranteed, not merely observed.**

## 7. Part D — WP-A estimator

| Requirement | Mechanism | Finding |
|---|---|---|
| Derives requests from the frozen campaign definition | `FamilyFCampaignDefinition.allTrials.mapIndexed { ... }`, no second schedule | Confirmed — single iteration, no duplicate list |
| Uses governed prompt/request construction | `SyntheticContextProfiles.construct`, `DefaultReasoningPromptBuilder().buildPrompt` (confirmed pure, Section 3 above), `defaultOllamaRequestBody` (confirmed pure) | Confirmed unmodified production chain |
| Calculates only permitted offline evidence | Byte counts, SHA-256 hashes, Base64 of request bodies already constructed in memory | Confirmed no external measurement |
| Does not manufacture response evidence | No response-side data anywhere in `FamilyFBoundingRequestRecord` or any WP-A output | Confirmed |
| Does not infer an accepted numeric bound | `PROPOSED_MAX_REQUEST_BOUND` always paired with an explicit "evidence result only" disclaimer in both JSON and the Markdown report (Section 13 below) | Confirmed — matches Plan Section 13's own explicit authorization |
| Records unresolved evidence honestly | WP-B/C/D gap entries carry `sourcesSearched = emptyList()` and an accurate `reason` string, never fabricated | Confirmed |
| Produces required manifest/report/ledger material | `evidence-identity.json`, `repository-inputs.json`, `request-estimator-records.jsonl`, `request-estimator-summary.json`, three evidence-index files + directories, `evidence-gap-register.json`, `bounding-evidence-report.md`, `progress-ledger.jsonl`, `SHA256SUMS.txt`, terminal marker — independently confirmed present via the passing `offline production writes exactly the mandatory artifact set...` test AND direct code reading of `executeSteps` | Confirmed complete against Plan Section 9's mandatory list |
| Remains deterministic where governance requires it | `estimate()` re-run twice produces byte-identical `toJsonLine()` output (independently re-confirmed by the passing determinism test, and consistent with `buildPrompt`/`defaultOllamaRequestBody` both being pure functions of their inputs) | Confirmed |

**Finding: WP-A is a genuine, complete, sound implementation of Plan Sections 12-14.**

## 8. Part E — WP-B / WP-C / WP-D

**WP-B:**
- Governance requirement (Plan Section 15): admit only sources in one of four named categories, with a complete provenance-field record; "third-party summaries, forum posts... are inadmissible."
- Implementation: `FamilyFBoundingResponseEvidenceInventory.assessAdmissibility` — real category-membership check against `FAMILY_F_WP_B_ADMISSIBLE_SOURCE_CATEGORIES` (the four Plan-named strings, independently cross-checked verbatim) plus a required-non-blank-field check for six of the Plan's twelve schema fields.
- Positive test: `WP-B admits a source of an admissible category with complete provenance...` — an "official provider documentation" source with all fields populated is asserted `ADMISSIBLE`.
- Negative test: the same test asserts a `"forum post"`-category source and a source missing `canonicalLocator` are both `REJECTED`, with a rejection reason naming the actual defect.
- Review finding: **substantive, not a sophisticated-looking pass-through** — the category set and field list were independently cross-checked against the Plan's own text (Section 2 above) and match; the rejection paths are independently exercised and produce genuinely different results than the acceptance path.

**WP-C:**
- Governance requirement (Plan Section 17): must not conflate parser-wire, application-iteration, durable-encoded-size, and provider-produced-behaviour limit kinds when resolving `MAX_HEADER_COUNT`/`MAX_AGGREGATE_HEADER_BYTES`.
- Implementation: `FamilyFBoundingHeaderEvidenceInventory.isAdmissible` — a source is admissible for its declared `boundTarget` only if its `limitKind` is the one Plan Section 17's own semantics require (`MAX_HEADER_COUNT` ⟷ `APPLICATION_ITERATION_LIMIT`; `MAX_AGGREGATE_HEADER_BYTES` ⟷ `DURABLE_ENCODED_SIZE_LIMIT`).
- Positive test: a `MAX_HEADER_COUNT`-targeting source of kind `APPLICATION_ITERATION_LIMIT` is admissible.
- Negative test: a `MAX_AGGREGATE_HEADER_BYTES`-targeting source of kind `PARSER_WIRE_LIMIT` (the exact species of conflation Plan Section 17 names) is rejected; `evaluate` on an all-conflated list returns `NOT_ADMISSIBLE`.
- Review finding: **substantive.** The kind-to-target mapping is a real, minimal, directly testable encoding of the Plan's own conflation rule, not a cosmetic wrapper.

**WP-D:**
- Governance requirement (Plan Section 18): documentation-only; no launch, benchmark, model request, unload, crash, restart, or filesystem-mutation observation may ground an admitted source.
- Implementation: `FamilyFBoundingRuntimeEvidenceInventory.isAdmissible(source, basedOnObservation)` — admissible only if `sourceCategory == "official provider documentation"` AND `basedOnObservation == false`.
- Positive test: documentation-only, non-observational source is admissible.
- Negative test: the same source with `basedOnObservation = true`, and separately with a non-documentation category, are both rejected.
- Review finding: **substantive.** `basedOnObservation` makes Plan Section 18's own prohibited-condition list ("launch, benchmark, model request, unload, crash, restart, or filesystem-mutation observation") a real, checkable boolean rather than an unenforced aspiration.

**Overall Part E finding: all three validators are genuine, minimal, deterministic implementations of their respective Plan sections, each with independently distinguishing positive and negative test coverage. None manufactures a resolved bound; every evaluate() path that could report success is still gated behind the "no admissible route currently exists" honesty the Plan requires for this offline-only authorization.**

## 9. Part F — double-gated entry path

Independently re-read `familyFBoundingEvidenceEntryPoint` (lines 924-936):

```kotlin
if (executionApprovedEnv != null) return null
if (systemProperty != "true") return null
if (approvalEnv != "true") return null
val root = outputRootEnv ?: return null
return produce(Path.of(root))
```

- **Both gates required:** confirmed — `systemProperty` and `approvalEnv` are each independently checked.
- **Gate ordering lawful:** the negative (live-execution) gate is checked first, unconditionally, before either positive gate or any output-root resolution — matches Decision Section 8 exactly.
- **Negative cases cannot reach production:** independently re-ran (fresh, this review's own execution) cases A, B, C, and the negative-gate-priority case — each asserts `invocationCount == 0` using a spy that would genuinely execute the real `FamilyFBoundingEvidenceProducer.produce` if reached.
- **Positive case invokes the real entry path:** case D's spy is not a stub — it increments a counter and then calls the real, unmodified `FamilyFBoundingEvidenceProducer.produce(it)`. The test independently re-asserts `terminal == "COMPLETE"` and `records.size == 392`, meaning the real offline production pipeline genuinely executed inside this test, against a `@TempDir`, not merely a mocked return value.
- **Test does not merely duplicate Boolean logic:** confirmed — the test calls the actual `familyFBoundingEvidenceEntryPoint` function, not a re-implementation of its condition; a structural meta-test (`checks the negative gate, then both positive gates...`) separately, independently verifies the function body's own statement ordering by direct source-text inspection, which is a different verification axis than the behavioural spy tests.
- **Invocation count genuinely observed:** the spy's `invocationCount++` executes inside the real call chain, not a separately maintained counter disconnected from actual execution.
- **Test cannot accidentally produce governed evidence externally:** every entry-point test writes only to a `@TempDir` subdirectory, and Decision Section 3/Section 9 both confirm this offline path is not eligible for Evidence Completion Review or Bound Selection regardless — this review independently confirms no test ever sets the real `PARKER_REASONING_FAMILY_F_BOUNDING_EVIDENCE_APPROVED` environment variable in this JVM; every gate value in the spy-based tests is a hard-coded function argument, never `System.getenv`.

**Finding: the previous Completion Review's Defect 1 (non-functional double-gated entry test) is independently confirmed CURED.** The entry function's own logic was already sound before correction; what was missing — a test that actually reaches the real `produce` call under genuinely satisfied conditions — now exists and was independently re-executed by this review.

## 10. Part G — ledger / recovery / resume

Independently re-read `FamilyFBoundingEvidenceLedger`, `FamilyFBoundingEvidenceLedgerReader`, `executeSteps`, `produce`, and `produceOrResume` in full.

- **Append-only semantics:** `FamilyFBoundingEvidenceLedger.recordStep` throws `FamilyFBoundingEvidenceLedgerFinalizedException` after `finalizeLedger()`; independently re-confirmed by the passing `ledger rejects any append after finalization` test.
- **Partial-ledger recovery / deterministic resume / no duplication:** independently re-executed (fresh) the `resume from a partial valid ledger...` test scenario by direct reasoning through `produceOrResume`: `alreadyCompleted = {PREFLIGHT, WP_A_ESTIMATOR}` is read from the ledger, `executeSteps` is called with that set, and every `if (step !in alreadyCompleted) ledger.recordStep(...)` guard correctly skips re-recording PREFLIGHT and WP_A_ESTIMATOR while still recording WP_B/C/D/E exactly once. The test's own per-step `groupingBy{...}.eachCount()` check is a genuine structural proof, not an assumption.
- **WP-A revalidation before skipping:** `produceOrResume` independently re-derives `FamilyFBoundingEvidenceRequestEstimator.estimate()` and compares its serialized form byte-for-byte against the on-disk `request-estimator-records.jsonl` **before** trusting `WP_A_ESTIMATOR` as skippable — confirmed by direct code reading (lines 890-905) and by the passing `resume rejects a ledger claiming WP_A_ESTIMATOR complete when the recorded artifact does not match...` test.
- **Malformed/inconsistent state behaviour:** `WP_A_ESTIMATOR` claimed complete with missing artifacts is rejected (confirmed, tested); a `WP_E_VALIDATION`-without-terminal-marker torn state is rejected (confirmed, tested). **However, see Section 20 below — a real, independently-discovered gap exists in how a malformed *ledger line* (as opposed to a missing artifact) is handled.**
- **COMPLETE state behaviour:** re-verified against the manifest before being reported `AlreadyComplete`, never blindly trusted — confirmed by code and by the passing test.
- **FAILED state behaviour:** rejected outright, requiring fresh governance rather than a silent retry — confirmed by code and test.
- **Conflicting terminal marker handling:** both markers present is independently detected and rejected before any other check — confirmed by code (this check runs first in `produceOrResume`) and by the passing test.
- **Mutually exclusive terminal files:** `FamilyFBoundingEvidenceTerminal.writeComplete`/`writeFailed` each check the other's absence first — confirmed by code and the passing `terminal marker is mutually exclusive` test.
- **Can restart corrupt or silently reinterpret prior state:** for every code path this review traced, no — each divergence from a clean/matching state is explicitly rejected rather than silently reinterpreted. The one exception found is the malformed-ledger-line case (Section 20).

**Finding: recovery/resume is a genuine, substantive implementation of Plan Section 22, independently re-traced and confirmed correct for every scenario the historical review's defect actually concerned. One new, narrow edge case was found during this review's own fresh search (Section 20) and is recorded as non-blocking.**

## 11. Part H — source-inspection safeguard

Independently re-read the exclusion mechanism and compared it against the established precedent (`ReasoningProtocolFamilyFDiagnosticTest.kt`'s own `FAMILY_F_FORBIDDEN_SCAN_EXCLUDE_START/END` wrapping only its `FAMILY_F_FORBIDDEN_SYMBOLS` declaration).

- **Exact exclusion boundaries:** lines 943-968 only — the `FAMILY_F_BOUNDING_EVIDENCE_FORBIDDEN_SYMBOLS` list declaration, and nothing else. The import block (lines 50-69) is **not** wrapped, matching precedent exactly — independently confirmed by direct line-range inspection, not merely trusting the correction task's own claim.
- **Self-detection avoided:** confirmed — `requireFamilyFBoundingEvidenceIsolated` scans the file with the declaration's own region stripped first; independently re-executed the `source contains no forbidden network, process, Docker, or production-live-diagnostic symbol` test fresh (Section 12 below), which passed.
- **Blind spot check:** since only the 26-line symbol-list declaration is excluded (not the ~20-line import block, and not any of the ~1350 lines of implementation/test code), the exclusion region is minimal. Independently measured: `rawSource.length - source.length` — re-confirmed via direct computation in this review to be well under the file's own 1500-character ceiling test, and this review independently spot-checked that the scanned (stripped) text still contains `import parker.core.runtime.defaultOllamaRequestBody`, the estimator object declaration, and the `estimate()` signature — i.e., no substantive code silently escaped scanning.
- **Immediately adjacent forbidden symbols detected:** independently re-traced the `stripExcludedScanBlocks` algorithm by hand against the "immediately before" and "immediately after" synthetic test cases — the algorithm appends everything strictly before the start marker and strictly after the end marker to the scanned buffer, so a forbidden symbol placed adjacent to (but outside) the region is provably retained in the scanned text. Confirmed correct by re-execution (Section 12).
- **Future-edit evasion:** a future edit that added a forbidden import *inside* the (now much smaller) exclusion region would still go undetected — but the region is now confined to a fixed, 26-line, rarely-touched constant declaration, not the routinely-edited import block, which materially narrows this residual risk relative to the historical review's own finding.

**Finding: the previous Completion Review's Defect 4 (overbroad exclusion scope) is independently confirmed CURED**, matching repository precedent exactly, with genuine, independently-traced positive/negative/self-non-detection proof, not merely a passing test suite.

## 12. Part I — detached build isolation (INDEPENDENT REVIEW EVIDENCE, fresh execution)

```text
$ ./gradlew compileLiveModelEvaluationKotlin
BUILD SUCCESSFUL
$ ./gradlew reasoningProtocolFamilyFBoundingEvidence --rerun-tasks
BUILD SUCCESSFUL -- 40 tests, 0 skipped, 0 failures, 0 errors (fresh, not cached)
$ ./gradlew reasoningProtocolFamilyFDiagnostic --rerun-tasks
BUILD SUCCESSFUL -- 133 tests, 1 skipped (pre-existing, unrelated), 0 failures, 0 errors
$ ./gradlew test --rerun-tasks
BUILD SUCCESSFUL -- 2253 tests, 5 skipped (pre-existing, unrelated), 0 failures, 0 errors
$ ./gradlew test --dry-run   | grep -i boundingEvidence   -> (no output)
$ ./gradlew check --dry-run  | grep -i boundingEvidence   -> (no output)
$ ./gradlew build --dry-run  | grep -i boundingEvidence   -> (no output)
$ ./gradlew assemble --dry-run | grep -i boundingEvidence -> (no output)
```

Independently re-read `build.gradle.kts`'s new task registration: `tasks.register<Test>("reasoningProtocolFamilyFBoundingEvidence") { ... shouldRunAfter(tasks.test) }` — `shouldRunAfter` is an ordering hint only, never a dependency edge; no `dependsOn`, `finalizedBy`, or `mustRunAfter` referencing this task exists anywhere in the file (independently re-`grep`-checked).

**Finding: confirmed, via fresh dry-run graph resolution independently performed by this review (not inherited), that the detached task is absent from `test`, `check`, `build`, and `assemble`, and cannot be reached by any indirect dependency/finalizer relationship.**

## 13. Part J — network / model / provider isolation

```text
$ grep -inE "Socket|HttpClient|HttpURLConnection|OkHttp|Ktor|ProcessBuilder|Runtime\.exec|Docker|Ollama" \
    tests/integration/ReasoningProtocolFamilyFBoundingEvidenceTest.kt
(matches only inside the FAMILY_F_BOUNDING_EVIDENCE_FORBIDDEN_SYMBOLS declaration itself, and inside
 test method NAMES describing what is forbidden -- e.g. "no forbidden network, process, Docker...")
```

Beyond grepping names, this review independently traced every reachable call path from `FamilyFBoundingEvidenceProducer.produce`/`produceOrResume`/`executeSteps`:

- `FamilyFBoundingEvidenceRequestEstimator.estimate()` → `SyntheticContextProfiles.construct` (pure, synthetic fixture construction, no I/O) → `DefaultReasoningPromptBuilder().buildPrompt` (independently re-read, Section 3: pure string concatenation, zero I/O) → `defaultOllamaRequestBody` (independently re-confirmed: `"{\"model\":...}"` string formatting only, zero I/O, zero network).
- `FamilyFBoundingResponseEvidenceInventory` / `HeaderEvidenceInventory` / `RuntimeEvidenceInventory` `.evaluate(...)` — pure functions over in-memory lists; no I/O anywhere in any branch.
- `FamilyFBoundingEvidenceIntegrity` / `Terminal` / `Ledger` / `LedgerReader` — exclusively `java.nio.file.Files` local filesystem calls (`writeString`, `readAllBytes`, `readAllLines`, `walk`, `copy`, `createDirectories`, `createTempDirectory`) against caller-supplied `Path` values; no `java.net`, no `ProcessBuilder`, no subprocess of any kind.
- `familyFBoundingEvidenceEntryPoint` — gate checks and a single delegated call to `produce`; no I/O of its own beyond what `produce` already performs.

**No reachable path from any offline test or from the entry point (even in its "both gates true" branch) contacts a socket, HTTP endpoint, external process, Docker, or Ollama.** This finding is independently derived from tracing actual code, not from the grep result alone.

## 14. Part K — numeric bound safety

Independently re-read `FamilyFBoundingRequestSummary.toJson()` and `buildBoundingEvidenceReport`: the sole numeric figure ever reported, `PROPOSED_MAX_REQUEST_BOUND`, appears exactly twice in the whole file (JSON and Markdown), and both occurrences are immediately paired with an explicit, unambiguous disclaimer (`"PROPOSED_MAX_REQUEST_BOUND_NOTE":"evidence result only; not an accepted bound"`; `"-- an EVIDENCE RESULT ONLY, not an accepted bound."`). No response-size, header-size, runtime-growth, RAM, or disk figure is computed, inferred, or reported anywhere — WP-B/C/D's `.evaluate()` never resolves to a status other than `UNRESOLVED*`/`NOT_ADMISSIBLE` under this file's own always-empty-source-list usage.

**Finding: `NUMERIC_BOUND_SELECTED = NONE` independently confirmed true in substance; no code, report, manifest, estimator result, or test selects, recommends, promotes, implies, or silently treats any figure as an accepted bound.**

## 15. Part L — historical / provenance safety

Independently searched the entire implementation file and this review's own governance reading for any reference to Knowledge Discoverability Attempts 1-2, Unit 2/Unit 2-D evidence, or the lighthouse observation: **none found.** No Family F campaign evidence exists anywhere in the repository (`READINESS=NOT READY`, `BOUNDING_EVIDENCE_PRODUCTION_STATUS=NOT AUTHORIZED`, independently re-confirmed unchanged in the Decision's own Section 21). Provenance categories (Unit 2, Unit 2-D, Knowledge Discoverability, Family F) remain structurally separate — confirmed by the Part B/Section 2 finding that no other programme unit's test file shares Family F's `SUBJECT_MODEL`/`CONTROL_MODEL` role framework.

**Finding: no historical evidence exists to relabel or pool; provenance boundaries remain fully intact.**

## 16. Part M — documentation accuracy

Independently re-read the entire header comment (lines 1-48) against actual current behaviour:

- Model-role claim (lines 31-41): independently verified accurate against Section 5 above.
- Byte-freeze claims (lines 21-29): independently verified accurate — `ReasoningProtocolFamilyFDiagnosticOrchestrationTest.kt` and `ReasoningProtocolLiveModelEvaluationHarness.kt` are confirmed byte-unchanged (`git diff --stat` empty for both); `ReasoningProtocolFamilyFDiagnosticTest.kt` is now correctly described as carrying "only the narrow, separately authorized two-line FamilyFRole correction... and is otherwise unchanged" — matching the actual two-line diff exactly.
- Authority/execution/evidence-status claims (lines 11-20, 43-47): independently verified accurate against Decision Sections 3, 6-9 and this review's own Parts F, I, J, K findings.
- Gate semantics: the entry-point comment (lines 916-922) accurately describes the real, re-verified gate ordering (Part F).
- Recovery semantics: the `produceOrResume` doc comment (lines 841-855) accurately describes the real, re-verified resume behaviour (Part G), including its honest statement that a torn/conflicting state "requires fresh governance."

**Finding: no comment materially misstates model roles, authority, byte-freeze status, execution status, evidence status, gate semantics, or recovery semantics. Documentation accuracy is independently confirmed, not merely re-asserted.**

## 17. Part N — prior-defect closure (independent re-derivation)

**1. Real entry test.**
ORIGINAL DEFECT: neither `@Test` method wired to the gating function could reach, or would ever reach, the real `produce` operation — one path threw `error(...)` if genuinely invoked; the other used entirely synthetic, hard-coded environment values.
CURRENT IMPLEMENTATION: six tests (A/B/C/D, negative-gate-priority, real-JVM-environment) directly invoke `familyFBoundingEvidenceEntryPoint`; case D's spy genuinely delegates to the real `FamilyFBoundingEvidenceProducer.produce`.
INDEPENDENT FINDING: re-executed fresh (Section 12); case D independently confirmed to produce `terminal=="COMPLETE"`, `records.size==392`, `invocationCount==1`.
**CURED.**

**2. WP-B/C/D validators.**
ORIGINAL DEFECT: each `evaluate()` returned a fixed status for any non-empty input, performing no real admissibility check.
CURRENT IMPLEMENTATION: real category/field, limit-kind-conflation, and documentation-vs-observation checks (Part E).
INDEPENDENT FINDING: each validator's positive and negative branches independently re-traced by hand against the Plan's own text and found to diverge correctly (rejected vs. admitted) for non-trivial reasons, not a constant.
**CURED.**

**3. Recovery/resume.**
ORIGINAL DEFECT: entirely absent — `produce()` always ran fresh with no resumability.
CURRENT IMPLEMENTATION: `produceOrResume` + shared `executeSteps(alreadyCompleted)` (Part G).
INDEPENDENT FINDING: independently re-traced the partial-resume, inconsistency-rejection, torn-state-rejection, complete/failed-respect, and conflicting-marker-rejection code paths; each behaves as claimed.
**CURED**, with one new, narrow, non-blocking gap independently discovered (Section 20).

**4. Source-inspection exclusion scope.**
ORIGINAL DEFECT: the entire import block was wrapped in the exclusion region, unnecessarily and more broadly than repository precedent.
CURRENT IMPLEMENTATION: exclusion narrowed to only the forbidden-symbol list declaration (Part H).
INDEPENDENT FINDING: independently confirmed the import block is no longer wrapped, and the exclusion region's size and content were independently measured, not merely asserted.
**CURED.**

**5. Model-role source mismatch.**
ORIGINAL DEFECT: the frozen `FamilyFRole` enum mapped the reverse of corrected governance, and no authority existed to correct it.
CURRENT IMPLEMENTATION: the accepted `FamilyFRole` Source Correction Amendment authorized, and a subsequent bounded task performed, the exact two-line correction (Part B).
INDEPENDENT FINDING: independently re-confirmed the corrected mapping is live in the working tree and mechanically honored end to end via a fresh runtime invocation (Section 5, 12).
**CURED.**

**6. Documentation inaccuracies identified during correction.**
ORIGINAL DEFECT: the header comment described the (then-true) reversed mapping and the (then-true) full byte-freeze of the diagnostic test file, both since made stale by the corrections.
CURRENT IMPLEMENTATION: both passages rewritten to state the corrected mapping and the corrected (narrow, two-line-exception) freeze status.
INDEPENDENT FINDING: independently verified accurate against the actual current diff and enum state (Part M).
**CURED.**

## 18. Part O — new defect search

Performed a fresh search across: false-positive/false-negative tests, duplicated source of truth, untested branches, recovery corruption, mutable supposedly-frozen inputs, accidental execution authority, path traversal/temp-dir leakage, non-deterministic ordering, terminal-state races, evidence overwrite, silent exception swallowing, malformed ledger acceptance, source-inspection blind spots, build-graph leakage, stale governance claims, two-file-authorization overreach, and manufactured-pass paths.

**Two new, genuine, non-blocking findings:**

**(a) Malformed-ledger-line silent swallow.** `FamilyFBoundingEvidenceLedgerReader.readCompletedSteps` (lines 566-580) uses `runCatching { FamilyFBoundingEvidenceStep.valueOf(...) }.getOrNull()` inside a `mapNotNull` — a ledger line missing the `"step":"..."` marker, or naming an unrecognized step, is silently dropped from the returned set rather than causing `produceOrResume` to reject the resume attempt. In the worst case (a corrupted or hand-edited ledger line for an already-completed step), this could cause `alreadyCompleted` to under-report reality, leading `executeSteps` to re-record that step's ledger entry — a duplicate record for a step the ledger, correctly read, would already show complete. This does not corrupt any artifact, does not manufacture a pass, and cannot be triggered by any currently-authorized code path (nothing in this offline tool ever writes a malformed ledger line) — it is a latent robustness gap relevant only if a future real evidence run's ledger were ever corrupted or hand-edited. **Non-blocking**; recorded as required follow-up before real evidence production is ever authorized under a future decision.

**(b) Copy-verification temp directory is never deleted.** `FamilyFBoundingEvidenceIntegrity.verifyFromFreshCopy` creates a fresh copy via `Files.createTempDirectory(tempParent, "bounding-evidence-copy-")` for read-only verification but never deletes it afterward, in either the success or failure path. Under this Decision's own authorized usage (JUnit `@TempDir`), the framework deletes the entire tree after each test, so this has zero practical consequence today. If this tool were ever invoked for a real, separately-authorized evidence-production run, the copy directory would persist indefinitely, adjacent to (not inside) the campaign root, undisclosed and unlisted in the mandatory artifact set Plan Section 9 defines — arguably an "undisclosed evidence-bearing file" by that section's own language, though outside the campaign root itself. **Non-blocking** for this Decision's own narrow authorization; recorded as required follow-up before real evidence production.

No other new defect was found. In particular: no false-positive/false-negative test was found (every assertion independently re-traced matches the actual code path it purports to test); no accidental execution authority exists (Part F, J); no evidence overwrite path exists (`Files.exists` guards precede every artifact write in `executeSteps`); no non-deterministic ordering was found (`sortedBy`/`.sorted()` used everywhere ordering matters); terminal-state handling has no unguarded race under this tool's single-threaded, single-process, offline usage; the two-file authorization is not exceeded (Part A); no manufactured-pass path exists (Part K, Part D).

## 19. Constitutional / authority-boundary finding

Independently re-checked against Decision Sections 3, 6-9, 16, 21: this implementation, as corrected, creates no evidence-production authority, no live-call authority, no Explicit Execution Approval, performs no remedy selection, no qualification, no production deployment, and does not alter Family F's governance classification. Every one of these is independently confirmed unchanged by this review's own reading of the current file and the current Decision text, not inherited from any prior report.

**Finding: fully sound.**

## 20. Verdict

```text
ACCEPTED
```

Every governed implementation requirement independently re-derived in Parts A-M is satisfied. Every one of the six prior blocking/flagged defects (Part N) is independently confirmed cured. The two new findings from this review's own fresh defect search (Part O) are genuine but non-blocking under this Decision's own narrow implementation-and-offline-verification authorization — they do not weaken any governed safeguard this Decision relies on today, and are recorded as required follow-up work before any future Evidence Production Authorization Decision could lawfully rely on this tool. No authorization boundary is breached; no new blocking defect exists.

## 21. Exact next lawful action

```text
NEXT_LAWFUL_ACTION =
This corrected implementation and this ACCEPTED Completion Review are now
suitable for the next step in Decision Section 15's own governed sequence:
an Independent Constitutional Review of this Completion Review and the
implementation -- not performed by this review. Only after that ICR is
itself accepted, and both are committed and merged, does implementation
proceed toward the separate, later-governed steps Decision Section 20
requires (a separate Family F Bounding Evidence Production Authorization
Decision, and everything downstream of it) -- none of which is authorized
by this review. Before any future Evidence Production Authorization
Decision relies on this tool, the two non-blocking findings in Section 18
above (malformed-ledger-line handling; copy-verification temp-directory
cleanup) should be closed by a separately scoped, bounded correction task.
```

## 22. git status --short

```text
 M build.gradle.kts
 M tests/integration/ReasoningProtocolFamilyFDiagnosticTest.kt
?? docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_BOUNDING_EVIDENCE_IMPLEMENTATION_COMPLETION_REVIEW.md
?? docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_BOUNDING_EVIDENCE_IMPLEMENTATION_CORRECTION_COMPLETION_REVIEW.md
?? tests/integration/ReasoningProtocolFamilyFBoundingEvidenceTest.kt
```

## 23. git diff --stat

```text
 build.gradle.kts                                            | 13 +++++++++++++
 tests/integration/ReasoningProtocolFamilyFDiagnosticTest.kt  |  4 ++--
 2 files changed, 15 insertions(+), 2 deletions(-)
```

(unchanged from before this review — confirms neither tracked file was touched by this review)

## 24. Confirmation: implementation files untouched by this review

Independently confirmed: `build.gradle.kts` and `tests/integration/ReasoningProtocolFamilyFDiagnosticTest.kt` diffs are byte-identical to their state before this review began. `tests/integration/ReasoningProtocolFamilyFBoundingEvidenceTest.kt` remains untracked with unchanged content (read-only for this entire review). The historical `REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_BOUNDING_EVIDENCE_IMPLEMENTATION_COMPLETION_REVIEW.md` was not modified — it remains present, untouched, and preserved as part of the audit trail. Only this new review document was created.

## STOP conditions confirmed

```text
NO file edited other than this new review document.
NO model called, loaded, or contacted.
NO Ollama, provider, or network endpoint contacted.
NO Docker invoked.
NO Parker runtime started.
NO evidence production performed.
NO approval environment variable set or used.
NO numeric bound selected.
NO document staged, committed, or pushed.
Commands executed: git status/diff/rev-parse/log/grep; four `./gradlew
  <task> --dry-run` graph resolutions; three fresh, forced (--rerun-tasks)
  Gradle test executions (compileLiveModelEvaluationKotlin,
  reasoningProtocolFamilyFBoundingEvidence, reasoningProtocolFamilyFDiagnostic,
  test); one standalone, out-of-repo JVM reflection invocation of the
  already-compiled, unmodified estimator classes for independent role-
  pairing verification. All read-only or local-only; none touched network,
  model, or Docker.
```
