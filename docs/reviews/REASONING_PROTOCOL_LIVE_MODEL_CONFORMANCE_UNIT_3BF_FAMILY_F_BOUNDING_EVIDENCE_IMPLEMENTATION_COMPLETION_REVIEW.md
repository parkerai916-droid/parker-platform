**Status:** Completion Review of the Unit 3-BF Family F Bounding Evidence Offline Implementation — **REVISE BEFORE ACCEPTANCE.** Independent, read-only, source-level review (not a rerun of the implementation's own reported test results) finds the implementation genuinely confined to the authorized two-file surface, genuinely detached from every ordinary Gradle lifecycle task (confirmed by dry-run task-graph resolution, not mere precedent inference), free of any reachable network/process/Docker/model-contact path, and free of any numeric-bound promotion beyond what Plan Section 13 explicitly authorizes. It nonetheless finds two independently confirmed, material defects: (1) the corrected `SUBJECT_MODEL`/`CONTROL_MODEL` roles the controlling Decision and Scope Lock freeze are **not mechanically honored** — every generated request record's `role`/`modelName` pairing is a direct, unmodified pass-through of the pre-existing, byte-frozen `FamilyFRole` enum, which still maps the reverse of the corrected table — and (2) the "frozen double-gated evidence-producing entry test" Decision Section 7 item 1 requires does not, in fact, exist as a functioning entry test: neither `@Test` method wired to the gating function ever reaches, or could ever reach, the real `FamilyFBoundingEvidenceProducer.produce` under genuine approved-execution conditions. Two further, non-blocking-on-their-own gaps are recorded: WP-B/C/D's "offline validators" are unconditional stubs for any non-empty supplied-source list, and no ledger/artifact recovery-on-resume logic exists despite Plan Section 22's explicit requirement. This document does not fix any of these; it records findings only.

# Unit 3-BF Family F Bounding Evidence Offline Implementation — Completion Review

## 1. Preconditions

```text
$ git rev-parse --abbrev-ref HEAD
main
$ git rev-parse HEAD
2c8842bd6f99e2dd1f9d125cd0c6a87e8facaabf
$ git rev-parse origin/main
2c8842bd6f99e2dd1f9d125cd0c6a87e8facaabf
$ git status --short
 M build.gradle.kts
?? tests/integration/ReasoningProtocolFamilyFBoundingEvidenceTest.kt
```

All three preconditions matched exactly. No STOP triggered.

## 2. Governance documents read fresh from HEAD

- `docs/decisions/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_BOUNDING_EVIDENCE_IMPLEMENTATION_AUTHORIZATION_DECISION.md` (full, 462 lines).
- `docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_BOUNDING_EVIDENCE_IMPLEMENTATION_AUTHORIZATION_DECISION_INDEPENDENT_CONSTITUTIONAL_REVIEW.md` (full).
- `docs/implementation/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_BOUNDING_EVIDENCE_ACQUISITION_AND_OFFLINE_ESTIMATOR_PLAN.md` and its accepted Model Role Amendment + both ICRs.
- `docs/architecture/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_FAMILY_F_MODEL_ROLE_AND_RESEARCH_QUESTION_SCOPE_LOCK.md` and its accepted correction ICR.
- The complete new implementation file, `tests/integration/ReasoningProtocolFamilyFBoundingEvidenceTest.kt` (1049 lines, read in full, not sampled).
- The full `build.gradle.kts` diff.
- Independently re-read, fresh, the pre-existing frozen source this new file depends on: `tests/integration/ReasoningProtocolFamilyFDiagnosticTest.kt` (constants, `FamilyFRole`, `FamilyFCorpus`), `FamilyFCampaignDefinition` in `ReasoningProtocolFamilyFDiagnosticOrchestrationTest.kt`, `src/runtime/ReasoningPromptBuilder.kt`, `src/runtime/ModelInferenceClient.kt`.

None of this was taken from the implementation task's own self-review; every finding below was independently re-derived from the cited primary source.

## 3. Review A — authorized file surface

```text
$ git diff --stat
 build.gradle.kts | 13 +++++++++++++
 1 file changed, 13 insertions(+)
$ git status --short
 M build.gradle.kts
?? tests/integration/ReasoningProtocolFamilyFBoundingEvidenceTest.kt
$ git diff --stat -- src/
(no output)
$ git diff --stat -- tests/integration/ReasoningProtocolFamilyFDiagnosticTest.kt tests/integration/ReasoningProtocolFamilyFDiagnosticOrchestrationTest.kt tests/integration/ReasoningProtocolLiveModelEvaluationHarness.kt
(no output)
```

**Finding: confirmed.** Changes are confined to exactly the two authorized files. Zero bytes changed under `src/`. The three named frozen test files are byte-identical to baseline. No production source, parser, reasoning, persistence, Memory Core, Knowledge Item, QMD, UI, Docker, runtime, or model-configuration file changed.

## 4. Review B — Gradle task isolation

Independently re-read the `build.gradle.kts` diff: the new `reasoningProtocolFamilyFBoundingEvidence` task is registered with `tasks.register<Test>(...)`, `testClassesDirs`/`classpath` drawn from the pre-existing `liveModelEvaluation` source set, `useJUnitPlatform()`, a `filter { includeTestsMatching(...) }` restricted to exactly `parker.integration.ReasoningProtocolFamilyFBoundingEvidenceTest`, one `systemProperty(...)` call, and `shouldRunAfter(tasks.test)` — an ordering hint, not a dependency edge. No `dependsOn` appears anywhere in the diff or in a `grep` of the whole file.

Rather than inferring isolation merely from a prior passing `./gradlew test`, this review independently resolved the actual Gradle task graph, read-only, via `--dry-run` (graph resolution only; no compilation, no test execution):

```text
$ ./gradlew check --dry-run --console=plain | grep -i boundingEvidence
(no output)
$ ./gradlew build --dry-run --console=plain | grep -i boundingEvidence
(no output)
$ ./gradlew assemble --dry-run --console=plain | grep -i boundingEvidence
(no output)
$ ./gradlew test --dry-run --console=plain | grep -i boundingEvidence
(no output)
```

**Finding: confirmed.** The new task is not attached to `test`, `check`, `build`, or `assemble`, cannot execute through any ordinary default path, uses only the pre-existing, already-authorized `liveModelEvaluation` classpath, and introduces no new dependency, plugin, repository, or unrelated Gradle change.

## 5. Review C — implementation conformance against Decision Sections 5-18

Confirmed present and sound:

- **Exact two-file surface** (Section 5) — confirmed, Section 3 above.
- **Offline request-body estimator** (Section 7 item 2; Plan WP-A) — `FamilyFBoundingEvidenceRequestEstimator.estimate()` iterates `FamilyFCampaignDefinition.allTrials` exactly once, in order, using the unmodified `SyntheticContextProfiles.construct`, `DefaultReasoningPromptBuilder().buildPrompt`, and `defaultOllamaRequestBody` — independently re-read line by line; no second fixture list, no duplicated formatter or serializer.
- **Evidence schemas/serializers** (Section 7 item 3) — hand-written, minimal JSON/JSONL writers scoped to this file only (`toJsonLine`, `toJson` extensions); does not touch or fork the production Ollama serializer.
- **Evidence-gap handling** (Section 7 item 5) — `FamilyFEvidenceGapEntry`/`FamilyFBoundingEvidenceGapRegister` honestly record `sourcesSearched = emptyList()` and a genuine, non-fabricated reason whenever no source is supplied; independently confirmed by direct reading, not merely by the implementation's own claim.
- **Manifest/report generation** (Section 7 item 6, part) — `FamilyFBoundingEvidenceIntegrity.writeManifest`/`verifyManifest` and `buildBoundingEvidenceReport` are genuine, non-stub, and independently confirmed to compute the report's numeric content from the actual `summary`/`gaps` objects, not hand-typed values.
- **Append-only progress-ledger behaviour** (Section 7 item 6, part) — `FamilyFBoundingEvidenceLedger.recordStep` throws `FamilyFBoundingEvidenceLedgerFinalizedException` after `finalizeLedger()`; independently confirmed by reading the implementation (not merely the test).
- **Mutually exclusive terminal markers** (Section 7 item 6, part) — `FamilyFBoundingEvidenceTerminal.writeComplete`/`writeFailed` each `check()` the other's absence first; confirmed sound.
- **Source-inspection isolation guard** (Section 7 item 7, part) — present; see Review H below for a quality finding.
- **Fake-driven/temporary-directory tests** (Section 7 item 8) — present, all against `@TempDir`, never the repository tree.
- **No numeric-bound selection** — see Review F below.

**Two material gaps confirmed missing or defective, independent of the model-role finding (Review D):**

1. **The "frozen double-gated evidence-producing entry test" (Section 7 item 1) does not function as such.** See Review G for the full mechanical trace. In summary: the only two `@Test` methods that exercise `familyFBoundingEvidenceEntryPoint` both override its `produce` parameter with `{ error(...) }`, so neither can, even in principle, invoke the real `FamilyFBoundingEvidenceProducer.produce` under genuinely satisfied gate conditions. One of the two (`... self-skips before any filesystem access when gates are absent`) passes the real `System.getProperty`/`System.getenv` values through to the gating function itself, meaning that if this task were ever actually run with both real positive gates genuinely set and the negative gate absent, this specific test would **throw an unhandled exception from the `error(...)` stub**, not perform real evidence production and not gracefully skip. There is no `@Test` anywhere in the file matching the established precedent shape (`... live Family F campaign is skipped ... `, gated by exactly two `assumeTrue` checks, calling the real entry point with the real environment) that this Decision's own Section 7 item 1 and the repository's own established convention require.

2. **WP-B/C/D "offline validators for pre-supplied local response, header, and runtime primary evidence" (Section 7 item 4) are unconditional stubs for the non-empty case.** Independently re-read `FamilyFBoundingResponseEvidenceInventory.evaluate`, `FamilyFBoundingHeaderEvidenceInventory.evaluate`, `FamilyFBoundingRuntimeEvidenceInventory.evaluate`: each correctly and honestly handles the empty-list case (the only case any test exercises), but each returns a bare, fixed status (`UNRESOLVED_INCOMPLETE_SERIALIZATION_BOUND` / `"UNRESOLVED"` / `UNRESOLVED_PROVIDER_DOCUMENTATION_INCOMPLETE`) for **any** non-empty `preSuppliedSources` list, performing no actual admissibility check (source category, digest, applicability) described by Plan Sections 15-18. This code path is also completely untested. Non-blocking on its own only because no evidence-collection mechanism is authorized under this Decision, so the non-empty path is currently unreachable in practice — but it means Section 7 item 4's literal content requirement ("validators") is not yet actually implemented, only stubbed.

3. **No ledger/artifact recovery-on-resume logic exists**, despite Plan Section 22's explicit requirement ("Completed ledger-governed steps may be verified and skipped on resume. An interrupted ledger-governed step may restart only if..."). `FamilyFBoundingEvidenceProducer.produce` always runs WP-A through WP-E fresh against its output root with no check for, or handling of, pre-existing partial state. Non-blocking on its own for this narrow offline-verification authorization (no real evidence run has ever occurred to resume from), but a genuine content gap relative to the Plan's explicit recovery requirement.

## 6. Review D — model role semantics (critical)

Independently inspected the actual source, not the implementation task's description of it.

```text
$ grep -n "FAMILY_F_SUBJECT_MODEL_NAME\|FAMILY_F_CONTROL_MODEL_NAME\|enum class FamilyFRole" tests/integration/ReasoningProtocolFamilyFDiagnosticTest.kt
62:const val FAMILY_F_SUBJECT_MODEL_NAME = "qwen2.5-coder:7b"
63:const val FAMILY_F_CONTROL_MODEL_NAME = "llama3.2:3b"
74:enum class FamilyFRole(val modelName: String) {
75:    SUBJECT(FAMILY_F_SUBJECT_MODEL_NAME),
76:    CONTROL(FAMILY_F_CONTROL_MODEL_NAME),
```

**1. The discrepancy is confirmed real**, independently, from the byte-frozen source itself (`git diff --stat` for this file is empty — it is genuinely unedited): `FamilyFRole.SUBJECT.modelName = "qwen2.5-coder:7b"`, `FamilyFRole.CONTROL.modelName = "llama3.2:3b"` — the exact reverse of the accepted Decision Section 10 / Scope Lock Section B table (`SUBJECT_MODEL = llama3.2:3b`, `CONTROL_MODEL = qwen2.5-coder:7b`).

**2. The new estimator consumes `FamilyFRole` directly**, not indirectly: `FamilyFBoundingEvidenceRequestEstimator.estimate()` line 175, `val modelName = trial.role.modelName`, and line 188, `role = trial.role.name` — both read straight off the frozen enum with no intermediate layer or remapping.

**3. Generated output does associate the "wrong" physical model with each role label, relative to the corrected governance's own definitions.** Every `FamilyFBoundingRequestRecord` written to `request-estimator-records.jsonl` carries `role="SUBJECT"` paired with `modelName="qwen2.5-coder:7b"` and `role="CONTROL"` paired with `modelName="llama3.2:3b"`. Per the Scope Lock's own Section B, `CONTROL_MODEL` is defined as "the actual, continuously deployed reasoning-protocol model Family F's comparison is measured against" (`qwen2.5-coder:7b`) and `SUBJECT_MODEL` as "the already-identified candidate under bounded, pre-qualification diagnostic evaluation" (`llama3.2:3b`) — a reader applying those corrected definitions to this file's own `role`/`modelName` pairing would draw exactly the wrong conclusion about which physical model is the deployed reference and which is the diagnostic candidate.

**4. The implementation does merely document the corrected roles while mechanically retaining the old mapping.** The file's own header comment (lines 29-46) states this explicitly and accurately — independently verified against the code, this self-report is honest and correct, not merely asserted. The correction exists only in prose; the executable role-to-model wiring is untouched.

**5. The estimator's 392-record schedule is structurally/arithmetically correct but label-semantically incorrect.** Coverage, counts, and per-cell completeness are independently confirmed sound (Review E below); what is wrong is only which role name (`"SUBJECT"`/`"CONTROL"`) is attached to which physical model in the metadata, not the completeness or correctness of the underlying measurement set itself. Both physical models still receive exactly 184 correctly-constructed scored request bodies each, covering every fixture/profile/repetition combination once.

**6. Correcting this at its root would require modification of a third file outside the two-file boundary.** The actual role-to-model wiring lives in `FamilyFRole` inside `tests/integration/ReasoningProtocolFamilyFDiagnosticTest.kt` — a file Decision Section 5 explicitly requires to "remain byte-unchanged." No governance instrument reviewed for this task authorizes editing it.

**7. A lawful correction entirely within the authorized new file is not clearly available, and is genuinely ambiguous rather than straightforwardly safe.** One could imagine the new file overriding the model-name string it passes to `defaultOllamaRequestBody` (and records in its own `modelName` field) independently of `trial.role.modelName`, while still iterating the real, unmodified `FamilyFCampaignDefinition.allTrials` schedule for everything else (fixture, profile, repetition, warm-up/scored status). This would not create "a second handwritten fixture or schedule copy" in the sense of duplicating the corpus or trial count. But it would mean the estimator's actual `defaultOllamaRequestBody(prompt, modelName)` call, for at least one role, would use a **different** string than the accepted schedule's own `trial.role.modelName` value — and Decision Section 10's own explicit prohibition list bars, verbatim, a "**substituted model name**" alongside "no handwritten fixture list, second schedule, duplicated formatter, duplicated serializer, shortened run, ... or reduced corpus." Whether a deliberate, disclosed, governance-motivated substitution of the model-name string is the kind of "substituted model name" that sentence means to forbid, or a narrower kind (e.g., substituting an altogether different, non-frozen model), is not resolved by any document read for this review. This review does not resolve that ambiguity and does not attempt the correction.

**8. Classification.** This is **not harmless/documentary** — it has a real, confirmed mechanical consequence on the `role`/`modelName` pairing recorded in the tool's own generated evidence artifacts. It is **not cleanly implementation-local and correctable within current authority** either — the only clear-cut fix (correcting the enum itself) requires a third file this Decision explicitly freezes, and the only in-boundary alternative (output-layer relabeling) runs into an unresolved, genuinely ambiguous reading of Decision Section 10's own "no substituted model name" prohibition. This is most accurately classified as **a blocking upstream production/test-source and governance-interpretation dependency defect**: the current two-file authority cannot lawfully resolve it without either (a) new governance narrowly authorizing a correction to `FamilyFRole` as its own separately reviewed change (a test file, not `src/` production code, so such authorization would not itself conflict with any "no production code" boundary elsewhere in this programme — only with this specific Decision's own Section 5 byte-unchanged requirement, which a later, express amendment could lawfully revise), or (b) new governance explicitly resolving whether an output-layer relabeling is, or is not, a prohibited "substituted model name."

Per the reviewing task's own explicit blocking-defect criterion — "corrected model roles are not mechanically honored" — this finding, on its own, is sufficient to prevent an ACCEPTED verdict.

## 7. Review E — campaign arithmetic

Independently re-derived, not merely checked against `.size == 392`:

```text
23 fixtures x 2 context profiles x 4 repetitions x 2 roles = 368 scored calls
8 residency blocks (4 repetitions x 2 roles) x 3 warm-up calls = 24 warm-up calls
368 + 24 = 392 total scheduled calls
```

Independently re-read the implementation's own test `estimator covers both frozen model names, both profiles, every fixture for both roles, and all 24 warm-up records`: for each of the four (role, profile) cells it asserts exactly `23 × 4 = 92` scored records and that the fixture-ID set exactly equals the full 23-fixture corpus — a per-cell coverage check, not merely an aggregate count. Independently re-verified this claim by re-reading `FamilyFCampaignDefinition`'s own `init` block (unedited, in the frozen orchestration file), whose own `check()` assertions independently enforce the identical 368/24/392/184-per-role invariants at class-initialization time, structurally guaranteeing the estimator (which iterates this same `allTrials` list) cannot diverge from them.

**Finding: the generated schedule's structure and counts are confirmed correct.** The corrected model-role *semantics* are not (Review D) — the counts and coverage are right, but two of the four (role, profile) cells' `role` label is paired with the wrong physical model per the corrected governance's own definitions.

## 8. Review F — numeric bound

Independently re-read `FamilyFBoundingEvidenceRequestSummary.toJson()` and `buildBoundingEvidenceReport`: both places the estimator's observed global-maximum byte count is written, it is labeled `PROPOSED_MAX_REQUEST_BOUND` with an adjacent, structurally-attached disclaimer (`"PROPOSED_MAX_REQUEST_BOUND_NOTE":"evidence result only; not an accepted bound"` in the JSON; `"-- an EVIDENCE RESULT ONLY, not an accepted bound."` in the Markdown report, on the same line). This matches Plan Section 13's own explicit language authorizing exactly this: "The summary may state `PROPOSED_MAX_REQUEST_BOUND=<exact global maximum>` only as an evidence result. It is not an accepted bound..."

No response-size, header-size, runtime-growth, RAM, or disk value is computed, inferred, or reported anywhere in this file — `FamilyFBoundingResponseEvidenceInventory`/`HeaderEvidenceInventory`/`RuntimeEvidenceInventory` never resolve to a numeric value under any code path this file can reach (no non-empty source list is ever supplied).

**Finding: confirmed sound.** `NUMERIC_BOUND_SELECTED=NONE` is honored in substance; the one reported value is explicitly and prominently disclaimed exactly as governance requires, not silently promoted.

## 9. Review G — network / model / execution isolation

Independently inspected source (not runtime behaviour) for every listed capability:

- **Socket / HTTP / process / Docker / Ollama-CLI / provider contact:** none found anywhere in the file; independently confirmed by direct reading of every import and every function body, not only by the source-inspection test's own claim (see Review H for a quality assessment of that test itself).
- **Live model call, Parker start, model acquire/load/unload/replace/requantize:** none found; no such API is imported or referenced.

**Double-gated entry point, inspected carefully:**

```kotlin
fun familyFBoundingEvidenceEntryPoint(
    systemProperty: String?, approvalEnv: String?, executionApprovedEnv: String?,
    outputRootEnv: String?, produce: (Path) -> ... = { FamilyFBoundingEvidenceProducer.produce(it) },
): FamilyFBoundingEvidenceProductionResult? {
    if (executionApprovedEnv != null) return null
    if (systemProperty != "true") return null
    if (approvalEnv != "true") return null
    val root = outputRootEnv ?: return null
    return produce(Path.of(root))
}
```

This function's own internal ordering **does** conform exactly to Decision Section 8: the negative (live-execution) gate is checked first and causes refusal regardless of value; both positive gates are required before output-root resolution; the real `produce` default is invoked exactly once, only after every gate. This part is sound.

**However — as found under Review C — no `@Test` in the file ever calls this function in a way that could reach the real default `produce` under genuinely satisfied conditions.** Both tests that exercise it override `produce` with `{ error(...) }`. This means: (a) the entry point *function* is correctly gated and, in isolation, conforms to Decision Section 8's structural requirements; but (b) the file, as a whole, contains no actual "entry test" wired to real execution — so the double gate's practical purpose (allowing a future, separately authorized real run to reach real evidence production while remaining inert otherwise) is not actually demonstrated or exercised anywhere, and would in fact crash rather than function correctly if a real run were ever attempted through the one JUnit method (`... self-skips before any filesystem access when gates are absent`) that passes real environment values through.

**Finding: the double gate's own internal logic conforms to governance; the surrounding real-invocation entry test required by Decision Section 7 item 1 is missing/broken. This is treated as a distinct, confirmed defect (Review C, item 1), independent of the model-role finding.**

## 10. Review H — source-inspection test quality

Independently re-read `requireFamilyFBoundingEvidenceIsolated`, `familyFBoundingEvidenceScanSafeSource`, `familyFBoundingEvidenceStripExcludedScanBlocks`, and the `FAMILY_F_BOUNDING_EVIDENCE_FORBIDDEN_SYMBOLS` list.

- **Genuinely detects prohibited symbols outside the exclusion region:** confirmed — independently re-derived that the forbidden-symbol list is itself now wrapped in `FAMILY_F_BOUNDING_EVIDENCE_FORBIDDEN_SCAN_EXCLUDE_START/END` markers (lines 704, 729), correctly preventing the self-detection failure the implementation task reported and fixed. No occurrence of any listed forbidden symbol was found anywhere else in the file by independent `grep`.
- **Exclusion markers are broader than the established precedent, and broader than necessary.** The established precedent (`ReasoningProtocolFamilyFDiagnosticTest.kt`'s own `FAMILY_F_FORBIDDEN_SCAN_EXCLUDE_START/END`) wraps *only* its forbidden-symbol list declaration. This file's exclusion additionally wraps the entire **import block** (lines 55-74). Independently checked: none of the twelve imports in that block contains any forbidden substring, so this exclusion was unnecessary for the current file to pass. Its cost is a genuine, if narrow, blind spot: a future edit that added a forbidden import (e.g. `import java.net.Socket`) inside that block would never be caught by this safeguard, whereas the established precedent's narrower exclusion scope would have caught an equivalent addition elsewhere in its own file. This is a real, material weakness relative to precedent, though it does not affect the correctness of the *current* file (which contains no forbidden import).
- **Cannot exclude substantive implementation code accidentally beyond the above:** confirmed — every production object/function in the file (estimator, ledger, integrity, terminal, entry point, evidence-gap handling) sits outside both exclusion blocks and is fully subject to the scan.
- **Obvious formatting/string-construction evasion (e.g. string concatenation to build a forbidden literal at runtime) is possible in principle** but is an inherited limitation of the whole repository's source-inspection technique, not something this implementation introduces or worsens; consistent with the task's own instruction not to require impossible static-analysis perfection.

**Finding: the mechanism functions correctly for the file as it exists today, but its exclusion scope is broader than the established precedent's own model and creates an avoidable blind spot for future edits.** Non-blocking on its own; recorded as a quality weakness.

## 11. Review I — the three implementation-time fixes

1. **`MutableList<Path> +=` compile failure, fixed via `.add(...)`.** Independently re-read every call site (10 occurrences): behaviourally identical to a correctly-resolving `+=`; no governance safeguard depends on this operator choice. **Sound, no weakening.**
2. **Gate-ordering meta-test body-extraction bug, fixed by matching the exact `): FamilyFBoundingEvidenceProductionResult? {` signature-end string.** Independently re-verified: the original bug (using `indexOf(")", start)`, which matched the inner lambda-type parenthesis in the `produce: (Path) -> ...` default parameter before the real signature end) would have caused the meta-test to scan the wrong, unrelated fragment of source, most likely producing spurious `indexOf` results of `-1` and failing the `assertTrue(... >= 0)` guard — a **false failure of a correctly-gated implementation**, not a false pass of an incorrectly-gated one. The fix targets an unambiguous, fail-closed marker (if the return-type name is ever renamed, the marker silently stops matching and the test fails loudly via the `assertTrue(signatureEndIndex >= 0, ...)` guard, rather than silently mis-scoping). **Sound; does not weaken the underlying gate-ordering safeguard, which this review independently re-verified by direct reading of `familyFBoundingEvidenceEntryPoint`'s own body (Review G) rather than trusting the meta-test alone.**
3. **Forbidden-symbol list self-detection bug, fixed via exclusion markers around the list declaration.** The specific reported bug is genuinely fixed (Review H, first bullet). However, the fix as applied also wrapped the import block in the same exclusion region — broader than strictly required to fix the reported bug, and broader than the established precedent. This is a **partial weakening of the safeguard's future robustness**, not of its current correctness: recorded under Review H as a non-blocking quality gap, but explicitly not "sound with no weakening" the way fixes 1 and 2 are.

## 12. Review J — ordinary-suite / detached-task evidence treatment

Per this task's own instruction, this review did **not** rerun the offline test suite or the detached task; the accepted Decision does not require the Completion Review itself to re-execute tests, and this review instead performed independent source-level verification throughout (Reviews C-I). The implementation's own reported results (20/20 detached-task tests passed; 2253/2253 ordinary-suite tests passed, 5 unrelated pre-existing skips) are treated strictly as implementation-run evidence, not as proof of governance conformance — and indeed, this review's own independent source reading found two material conformance defects (Review C items 1-2; Review D) that a green test run did not, and structurally could not, surface, because the missing "real entry test" was never exercised by any passing test, and the model-role mismatch was never asserted against by any test (every test that checks `role`/`modelName` pairing uses a `setOf(...)` membership check, which cannot detect a swapped pairing).

The only commands this review executed were read-only Gradle task-graph resolutions (`--dry-run`, four invocations, Review B) — no compilation, no test execution, no evidence production, no model/network/Docker contact.

## 13. Review K — constitutional / authority boundary

| Property | Finding |
|---|---|
| Remains offline | Yes (Review G) |
| Remains detached | Yes (Review B, empirically confirmed via dry-run) |
| Remains reversible | Yes — a plain two-file diff, trivially revertible |
| Remains within two files | Yes (Review A) |
| Creates no evidence-production authority | Yes — `EVIDENCE_PRODUCTION_AUTHORIZED`-equivalent paths are unreachable by any existing test, and the real entry point cannot currently be reached even if attempted (Review C/G) |
| Creates no live-call authority | Yes |
| Creates no Explicit Execution Approval | Yes — nothing in the diff touches any approval mechanism |
| Performs no remedy selection | Yes |
| Performs no qualification | Yes |
| Performs no production deployment | Yes |
| Does not alter Family F's governance classification | Yes — no governance document touched by this implementation |

**Finding: the constitutional/authority boundary is fully sound.** Every defect found by this review (Reviews C, D, G, H) is a *conformance/correctness* defect internal to the two-file offline tool, not a boundary violation.

## 14. Omitted or weakened requirements (summary)

1. Decision Section 7 item 1 ("the frozen double-gated evidence-producing entry test") — effectively missing; no test reaches or could reach the real entry point under genuine conditions (Reviews C, G).
2. Decision Section 10 / Scope Lock Section B corrected model roles — not mechanically honored in the estimator's output (Review D).
3. Decision Section 7 item 4 ("offline validators for pre-supplied ... primary evidence") — stubbed for the non-empty case, untested (Review C).
4. Plan Section 22 recovery-on-resume behaviour — entirely absent (Review C).
5. Source-inspection exclusion scope broader than established precedent, creating an avoidable future blind spot (Review H, Review I item 3).

## 15. New defects discovered by this review (not previously reported)

- The broken/non-existent real entry test (Section 5/9/14, item 1) was **not** disclosed in the implementation task's own self-review or report; it is a new finding of this review.
- The WP-B/C/D stub-validator gap and the absent recovery/resume logic were likewise not disclosed by the implementation task; both are new findings of this review.
- The source-inspection exclusion-scope breadth (import block) was not disclosed by the implementation task; new finding of this review.

The only defect the implementation task itself disclosed — the model-role mismatch — is independently confirmed accurate by this review (Review D), with its consequences traced further (mechanical propagation into generated JSONL evidence fields; the "substituted model name" ambiguity in any within-boundary fix) than the implementation task's own report did.

## 16. Verdict

```text
REVISE BEFORE ACCEPTANCE
```

Per the governing task's own explicit blocking-defect criteria, this verdict is required by at least two independent grounds: (1) "corrected model roles are not mechanically honored" (Review D), and (2) a required governance safeguard — the real, functioning evidence-producing entry test (Decision Section 7 item 1) — is missing (Reviews C, G). Everything else reviewed (file surface, Gradle isolation, campaign arithmetic/structure, numeric-bound discipline, network/process isolation of the code that does exist, and the constitutional/authority boundary) is independently confirmed sound.

## 17. Exact next lawful action

```text
NEXT_LAWFUL_ACTION =
Do not implement or fix anything as a continuation of this review.
The narrowest lawful next step is a new, separately scoped implementation-
correction task, confined to the same already-authorized two-file surface
(build.gradle.kts; tests/integration/ReasoningProtocolFamilyFBoundingEvidenceTest.kt),
that:
  (a) replaces the two non-functional gate tests with a genuine entry test
      matching the established precedent shape -- gated by assumeTrue on the
      real System.getProperty/System.getenv values, invoking the real,
      non-overridden FamilyFBoundingEvidenceProducer.produce default -- so
      that the tool actually functions if a future, separately authorized
      run ever sets the real approval environment;
  (b) either obtains a narrow, separate governance act resolving whether an
      output-layer model-name relabeling is a prohibited "substituted model
      name" under Decision Section 10, or obtains a narrow, separate
      governance act authorizing a corrected FamilyFRole mapping as its own
      reviewed change to the test file that currently defines it -- neither
      of which this review performs or recommends between;
  (c) is followed by its own fresh Independent Constitutional Review before
      any Completion Review may return ACCEPTED.
Items (c) and (d) from Section 5 above (stub validators; missing recovery
logic) may be addressed in the same corrective task or deferred with an
explicit governance statement narrowing Decision Section 7's scope; this
review takes no position on which.
```

## 18. Review artifact

**Authorization determination:** Decision Section 15 names "a Family F Bounding Evidence Implementation Completion Review" as the first step of the required post-implementation sequence, to occur after implementation and before any evidence production. Implementation occurred in the immediately preceding task. This task is that review. Creating this artifact — regardless of its verdict — is therefore authorized by, and is the direct fulfilment of, Decision Section 15 step 1; it mirrors the established, repeatedly-used precedent throughout this program of writing a review document even when its verdict is `REVISE BEFORE ACCEPTANCE` (e.g. the Family F Model Role and Research Question Scope Lock's own first Independent Constitutional Review). Created:

```text
docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_BOUNDING_EVIDENCE_IMPLEMENTATION_COMPLETION_REVIEW.md
```

(This document.) Neither implementation file (`build.gradle.kts`, `tests/integration/ReasoningProtocolFamilyFBoundingEvidenceTest.kt`) was modified by this review.

## STOP conditions confirmed

```text
NO file edited other than this new review document.
NO model called, loaded, or contacted.
NO Ollama, provider, or network endpoint contacted.
NO Docker invoked.
NO Parker runtime started.
NO evidence production performed.
NO live Family F diagnostic performed.
NO numeric bound selected.
NO model acquired or replaced.
NO production deployment performed.
NO document staged, committed, or pushed.
Only read-only commands executed: git status/diff/rev-parse/log/grep, and
  four `./gradlew <task> --dry-run` task-graph resolutions (no compilation,
  no test execution).
```
