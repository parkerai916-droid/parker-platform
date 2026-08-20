**Status:** Completion Review and Independent Constitutional Review of the Unit 3-BF Family F Diagnostic Implementation/Execution Plan — **ACCEPTED.** This review independently confirms the Plan authorizes only an exact three-file, offline, test-tier implementation; introduces no production-source change; is genuinely detached and double-gated against ordinary Gradle lifecycle tasks; faithfully transcribes the Scope Lock's frozen corpus, context profiles, and 392-call schedule with correct arithmetic; preserves production-path fidelity through a necessary and architecturally sound transparent capture proxy; specifies executable exact-once/durability/recovery semantics; defines resource and residency gates that touch only a dedicated diagnostic daemon; and authorizes no model acquisition, model run, campaign, production change, or Knowledge Discoverability Attempt 3.

**Model-identity premise correction.** Corrected in place against the accepted Model-Identity Premise Defect Confirmation Review and its Independent Constitutional Review (commit `4d8d5012243df955683fe929a6cf7a0dc6766ffc`): the reviewed document's own designation of `llama3.2:3b` as comparison control is not itself rewritten, but rests on an upstream premise — that `llama3.2:3b` was Parker's current, live, or production model — now corrected; `qwen2.5-coder:7b`, not `llama3.2:3b`, was Parker's committed deployed Docker baseline throughout this programme. CONTROL_MODEL/SUBJECT_MODEL roles and the Family F research question remain unresolved, pending separate governance. The remainder of this document's body is unmodified and remains the historical record of this review.

# Unit 3-BF Family F Diagnostic Implementation/Execution Plan — Completion and Independent Constitutional Review

## 1. Reviewed baseline and scope

```text
plan authored-from commit=0f9eb8b4d90b9f51a8afeda975ee5238464a6582
plan branch head=c102a9e8a2ae4dea2cabc0cc9342ece3a9e4b260
plan file=docs/implementation/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_DIAGNOSTIC_IMPLEMENTATION_EXECUTION_PLAN.md
```

Independently confirmed `0f9eb8b4` is an ancestor of `c102a9e8`, and that the branch differs from `origin/main` by exactly the one file named above (`git diff --stat origin/main HEAD`). This review performs both a completion check (does the Plan satisfy its own Section 29 exit criteria, is it internally complete and consistent) and an adversarial constitutional check (does it stay within the authority the Scope Lock actually granted). It authorizes nothing itself.

## 2. Independent sources and method

Challenged against primary text and primary source code fetched fresh, not accepted from the Plan's own restatement:

- `docs/architecture/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_EXPERIMENTAL_RECLASSIFICATION_AND_QUALIFICATION_BOUNDARY_SCOPE_LOCK.md` and its Independent Constitutional Review (this reviewer's own prior work, re-checked rather than assumed correct);
- the accepted Family F Alternative-Model Diagnostic Planning Review and its Independent Constitutional Review;
- the accepted Reopening Decision and its Independent Constitutional Review;
- `docs/architecture/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3B_REMEDY_EXPERIMENT_SCOPING_SCOPE_LOCK.md`, item 14 and Section 10;
- `docs/architecture/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_2_BASELINE_CHARACTERISATION_SCOPE_LOCK.md`, Sections 3–5 (fixture corpus, context profiles, warm-up input text);
- `src/runtime/ModelInferenceClient.kt` (`LocalHttpModelInferenceClient`, `defaultOllamaRequestBody`, `defaultOllamaResponseBody`), read fresh in full;
- `tests/integration/ReasoningProtocolLiveModelEvaluationHarness.kt`, read fresh in full;
- `tests/integration/ReasoningProtocolUnit3CControlledRemedyExperimentsTest.kt` and `tests/integration/ReasoningProtocolUnit3COrchestrationTest.kt`, grepped for the double-gate and resource-gate precedent;
- `build.gradle.kts`, read fresh, Sections defining `liveModelEvaluation` and the four existing detached `Test` task registrations.

The review specifically hunted for: a fourth implicitly-required file; any production-source edit; a live entry point reachable from `test`/`check`/`build`; corpus or context drift from Unit 2's canonical values; a schedule arithmetic error; a description of ordering that overstates a full Latin square; a warm-up input or context mismatch; any additional experimental variable beyond model identity; duplicated or reimplemented production protocol logic in the proxy; a retry path disguised as recovery; a resource or residency gate that could touch the production Parker process or production model daemon; an advancement gate satisfiable by a subset of cells or convertible into a ranking; qualification-credit leakage; any Knowledge Discoverability dependency; and any TBD, placeholder, or operator-discretion gap.

## 3. Three-file scope

Independently re-read Section 5, Section 4, and Section 26: exactly `build.gradle.kts` (task registration only), `tests/integration/ReasoningProtocolFamilyFDiagnosticTest.kt` (new), and `tests/integration/ReasoningProtocolFamilyFDiagnosticOrchestrationTest.kt` (new). Independently confirmed no other file is referenced as requiring a change anywhere in the document, and Section 26 explicitly stops implementation if a fourth file appears necessary. This mirrors, file-for-file in kind, the already-implemented Unit 3-C precedent (`ReasoningProtocolUnit3CControlledRemedyExperimentsTest.kt` + `ReasoningProtocolUnit3COrchestrationTest.kt` + one `build.gradle.kts` task registration), independently confirmed present and structured that way in this repository. **Not falsified.**

```text
THREE_FILE_SCOPE=CONFIRMED — exactly build.gradle.kts (task only) + 2 new tests/integration files; no fourth file authorized; matches proven Unit 3-C structural precedent
```

## 4. No production-source change

Independently re-read Section 4 ("No file under `src/` may change") and Section 3's prohibition list. Every production class the Plan cites — `DefaultReasoningPromptBuilder`, `ModelReasoningProvider`, `LocalHttpModelInferenceClient`, `TaggedReasoningResponseParser`, `defaultOllamaRequestBody`, `defaultOllamaResponseBody` — was independently re-verified present, unmodified, and exactly named in `src/runtime/ModelInferenceClient.kt` and against this reviewer's own prior direct reads of `ReasoningPromptBuilder.kt`, `ReasoningResponseParser.kt`, and `ModelReasoningProvider.kt` earlier in this session. No sentence anywhere proposes editing any of them. **Not falsified.**

## 5. Detachment and double gate

Independently re-read `build.gradle.kts`: the `liveModelEvaluation` source set (lines 101–105) is explicitly documented as "deliberately not attached to test/check/build/assemble," and all four existing custom `Test` tasks (`reasoningProtocolLiveModelEvaluation`, `reasoningProtocolBaselineCharacterisation`, `reasoningProtocolUnit2DDiagnostic`, `unit3cControlledRemedyExperiments`) use `tasks.register<Test>(...)` with `shouldRunAfter(tasks.test)`, never `dependsOn` from `check`/`build`/`assemble`. Independently searched the whole build file for `check.dependsOn`, `tasks.check`, or any wiring that would pull a custom `Test` task into the standard lifecycle — none found. Gradle's own default behavior attaches only the `test` task to `check`. The Plan's proposed `reasoningProtocolFamilyFDiagnostic` task (Section 7) follows the identical, already-proven registration pattern.

Independently re-read `ReasoningProtocolUnit3CControlledRemedyExperimentsTest.kt`: the real, already-implemented live entry point is gated by **exactly two** `assumeTrue` checks — one on `System.getProperty(UNIT_3C_PROPERTY) == "true"`, one on a non-blank campaign-ID environment variable — and a dedicated meta-test (`` `live Unit 3-C trigger source calls the real entry point with real environment exactly once, gated by exactly two assumeTrue checks` ``) independently enforces this count via source inspection. The Plan's Section 7 double gate (`parker.reasoning.familyf.enabled=true` system property, `PARKER_REASONING_FAMILY_F_EXECUTION_APPROVED=true` environment value, "Either one alone is insufficient") is the identical, already-proven-safe mechanism applied to a new task name. **Not falsified — genuinely implementable, not merely asserted.**

```text
SCHEDULE=368 scored + 24 warm-up = 392, independently recomputed exact (see Section 8 below); double-gate pattern independently confirmed already implemented and independently meta-tested in this exact repository
```

## 6. Corpus and context fidelity

Independently re-read Unit 2 Baseline Characterisation Scope Lock Section 3 (23 fixtures, distribution REMEMBER 3 / REPLY 13 / GOAL 5 / NOACTION 2) and Section 4 (profile 1 `minimal-production-context`, profile 9 `mixed-full-production-like`), and Section 5's exact warm-up text: "`Synthetic warm-up request: reply with a brief acknowledgement.`" All three independently match the Plan's Section 8 (distribution), Section 10 (both named profiles, both already verified real Unit 2 profiles, no invented profile), and Section 10 (identical warm-up text, `minimal-production-context`, three per block). Section 8's abbreviated fixture-order listing ("`R01, R02, R03, P01` through `P13`, `G01` through `G05`, `N01, N02`") is read as an order-reference shorthand, not a redefinition of fixture IDs — Section 8's own binding sentence ("must be equality-tested against the canonical Unit 2 values") and Section 1's supremacy clause ("The Scope Lock remains authoritative if any wording here can reasonably be read more broadly") together foreclose any implementer reading the shorthand as license to use shortened IDs in place of the canonical `R01-direct`-style identifiers. **Not falsified.**

## 7. Ordering description

Independently re-read Section 10: "the model order is AB/BA alternation, not a full Latin square: repetitions 1 and 3: subject block, then control block; repetitions 2 and 4: control block, then subject block." Independently checked this against the actual pattern it describes (subject-first, control-first, subject-first, control-first) — this is accurately self-labeled as AB/BA alternation, not overstated as a full Latin square design, and matches this reviewer's own independent characterization of the identical scheme in the Scope Lock review. **Not falsified.**

## 8. Schedule arithmetic re-derived independently

```text
23 fixtures x 2 profiles x 4 repetitions x 2 models = 368 scored calls
23 x 2 = 46 fixture/profile cells (matches Section 20's "every one of the 46 fixture/profile cells")
4 repetitions x 2 models = 8 residency blocks
8 blocks x 3 warm-ups = 24 warm-up calls
368 + 24 = 392 total calls
```

Every restatement of these figures throughout the document (Sections 10, 11, 20, 23, 24, 27) is independently consistent with this recomputation and with the frozen Scope Lock figures this reviewer independently verified in the prior task. **Not falsified.**

## 9. Model identity as sole variable and production-path fidelity

Independently re-read Section 12's explicit list of everything held identical across arms (commit, host, production chain, endpoint protocol, transparent proxy, timeout, classification procedure, fixtures, rubrics, profiles, order, resource policy) and its explicit statement that the production default formatter emits only `model`/`prompt`/`stream:false`, with model name the sole arm-specific value — independently confirmed against `defaultOllamaRequestBody`'s actual signature and body in `src/runtime/ModelInferenceClient.kt` (`"{\"model\":...,\"prompt\":...,\"stream\":false}"`, no other field). Section 12's stop-before-contact rule for any unavoidable inference-setting discrepancy is independently consistent with Unit 3-B Section 5 item 16's own single-variable-isolation requirement. **Not falsified.**

## 10. Transparent capture proxy — necessary and non-duplicative

Independently re-read `LocalHttpModelInferenceClient.infer()`: it calls `httpClient.sendAsync(...)` and returns only `responseBodyParser(response.body())` — the HTTP status code and headers are read by the `HttpResponse<String>` object but never surfaced to the caller or captured anywhere in the production class. This independently confirms the Plan's own Section 12 rationale ("`LocalHttpModelInferenceClient` does not expose the complete HTTP status/header exchange") is factually accurate, not an invented justification — a proxy sitting in front of the dedicated model daemon is the only way to capture status/headers without modifying `src/`.

Independently confirmed the proxy is additive, not a reimplementation: `LocalHttpModelInferenceClient`'s constructor already accepts an arbitrary `endpointUrl` with no default (verified in source), and `ReasoningProtocolLiveModelEvaluationHarness` already demonstrates this exact pattern (constructing the client against `config.endpointUrl`, with its own `requestBodyFormatter`/`responseBodyParser` hooks delegating to the identical production `defaultOllamaRequestBody`/`defaultOllamaResponseBody` functions rather than reimplementing them). The Plan's proxy adds a transport-layer capture point in front of this already-proven, already-reusable harness — the actual prompt formatting, HTTP dispatch, response extraction, and tag parsing all still run through the unmodified production functions and classes; the proxy only forwards bytes and separately records what it forwards. **Not falsified — sound and non-duplicative.**

```text
PRODUCTION_PATH_FIDELITY=CONFIRMED — DefaultReasoningPromptBuilder, ModelReasoningProvider, LocalHttpModelInferenceClient, TaggedReasoningResponseParser, defaultOllamaRequestBody, defaultOllamaResponseBody all reused unmodified; proxy is a necessary, additive, transport-layer-only capture point, not a protocol reimplementation
```

## 11. Exact-once dispatch, durability, and recovery

Independently re-read Section 18's eight-step per-call order (verify → append/force dispatch record → proxy persists raw request → dispatch once → proxy persists raw response → parse/classify → append/force terminal record → re-verify counts) and its five recovery rules (intent-without-dispatch resumable; dispatch-without-complete-response permanently ambiguous; complete-response-without-terminal classifiable offline without a new call; terminal-without-transport-evidence invalid and halting; no reissue of any completed or ambiguous trial). Independently checked this against Unit 2 Scope Lock Section 6's own resume/duplicate-rejection discipline (verified fresh in this task: "rejects duplicate IDs, reconstructs completion from verified records, and selects only the next pre-registered missing ID... A crash after raw append but before ledger update therefore preserves and adopts the one existing record; it never repeats the call") and Unit 3-B Section 12's fail-closed stop-condition precedent. The Plan's rules are a stricter, model-load-aware extension of the identical already-governed pattern, not a novel or untested mechanism. Section 17's required ledger files (`schedule.jsonl`, `intent.jsonl`, `dispatch.jsonl`, `transport.jsonl`, `terminal.jsonl`, `control-events.jsonl`, hash-chained records) are independently sufficient to reconstruct exact-once state from any crash point described in Section 18. **Not falsified — executable and internally consistent.**

```text
EXACT_ONCE_AND_DURABILITY=CONFIRMED — intent/dispatch/transport/terminal ordering with forced durability at each governance-critical transition; every recovery branch independently traced and found non-retrying; consistent with, and appropriately stricter than, the already-proven Unit 2 resume discipline
```

## 12. Resource and residency gates

Independently re-read Section 16 (pre-load: `MemAvailable >= artifact size + 2 GiB`; before and after every one of 392 calls: `MemAvailable >= 2 GiB`) against the Scope Lock's own Section 19, independently verified in the prior review to match the Planning Review's own RAM finding (`~4.1 GiB -> ~1.5 GiB` after loading `llama3.2:3b`, drawn from the durable Attempt 2 evidence report). The Plan's figures are byte-identical to the Scope Lock's frozen requirement — no drift. Independently confirmed the injectable-resource-reader testability pattern this gate requires ("using fake readings," Section 23) is already proven in this repository via `Unit3CDiskSpaceGate.check(path, usableSpaceSupplier)` in `ReasoningProtocolUnit3COrchestrationTest.kt`, which accepts an injectable space-reading lambda for offline testing — the Family F Plan's RAM gate is a reasoned, analogous extension of an already-implemented, already-tested gate pattern to a new resource type, not an unprecedented invention.

Independently re-read Section 15: residency verification and unload operations are explicitly scoped to "the dedicated provider's process/model-residency API" and "only the dedicated endpoint" — never the production Ollama server. Section 14 requires read-only, before/after PID and endpoint-identity checks against protected production processes, explicitly "never signals, stops, restarts, reconfigures, or routes traffic through them." **Not falsified.**

```text
RESOURCE_AND_RESIDENCY_GATES=CONFIRMED — artifact-size-aware pre-load gate and continuous 2 GiB per-call gate match the Scope Lock exactly; gate testability pattern already proven in this repository (Unit3CDiskSpaceGate precedent); residency/unload operations scoped exclusively to the dedicated diagnostic daemon; production Parker and production model daemon protected by read-only checks only
```

## 13. Advancement gate — investment-screening only, no ranking, no qualification credit

Independently re-read Section 20: the eight-condition gate spans all 46 fixture/profile cells (not a subset), requires zero false-positive `REMEMBER`/`GOAL`, zero material mutation/invention (correctly matching Unit 3-A's own tolerance for non-material paraphrase, independently re-confirmed against Unit 3-A Section 8's frozen content-fidelity contract read earlier in this session), full representation validity across all 184 subject calls, and full campaign/gate integrity — and is explicitly, repeatedly labeled "an investment-screening advancement gate," never a qualification, production, or selection threshold. Independently re-read Section 20's closing paragraph and Section 21: the control is reported under the identical matrix but "no relative comparison, win/loss statement, aggregate ranking, or substitution decision is authorized—even if both models satisfy their respective reported absolute measures," and no exposure counts toward Unit 3-A qualification (Section 21). **Not falsified.**

## 14. Execution authority and Knowledge Discoverability boundary

Independently re-read Section 3's explicit non-authorization list, Section 13's no-downstream-Knowledge-Discoverability-path list (no interactive runtime, no owner message, no Memory Core access, no `ReasoningKnowledgeSource` invocation, no reuse of either prior KD evidence directory, no Attempt 3), Section 22's linear seven-step governance sequence ("No later step may begin before both required reviews of the preceding step are accepted"), and Section 30's final authority statement (acceptance and merge authorize only the three-file implementation, zero model contact). Every occurrence is consistent; no sentence anywhere grants acquisition, load, campaign, production, or Knowledge Discoverability authority. **Not falsified.**

```text
EXECUTION_AUTHORITY=NO model run, campaign, acquisition, implementation beyond the three files, or production change authorized by acceptance of this Plan
KNOWLEDGE_DISCOVERABILITY_ATTEMPT_3=NOT AUTHORIZED — explicitly and repeatedly foreclosed (Sections 3, 13, 16, 28, 30)
```

## 15. Citation and source-claim accuracy

Every governance path in Section 1 independently resolves and matches content already read in full this session or fetched fresh for this task. Every production class/function name (Section 12) independently verified against `src/runtime/ModelInferenceClient.kt` and this reviewer's own prior direct reads of the remaining production reasoning files. The claimed reuse of `tests/integration/ReasoningProtocolLiveModelEvaluationHarness.kt`'s public types (Section 4) independently verified against that file's actual public surface (`ConformanceFixture`, `ContextProfileId`, `SyntheticContextProfiles`, `SyntheticTrialInput`, `TrialObservation`, `LiveEvaluationConfig`, `ReasoningProtocolLiveModelEvaluationHarness`, `EvaluationJsonLines`, `sha256`) — all present, all constructible externally without modification, none hardcoding a fixed fixture set that would prevent Family F from supplying its own 23-fixture corpus. The Gradle task/source-set/double-gate claims independently verified against `build.gradle.kts` and `ReasoningProtocolUnit3CControlledRemedyExperimentsTest.kt` directly. No citation drift found anywhere.

## 16. Completeness — no TBD, placeholder, or hidden discretion

Independently scanned the full document for hedge language ("TBD," "to be determined," "at the implementer's discretion," "may vary," "optional"). None found in any substantive requirement. Section 27's decision register explicitly states "No item remains TBD, optional, implementation-defined, or deferred to operator discretion." Values properly deferred to the future Explicit Execution Approval (exact digests, exact PIDs, exact host paths, exact campaign ID) are deferred because they cannot exist before execution is separately authorized — this mirrors exactly how the Scope Lock itself deferred the same values, and is correct governance layering, not vagueness in this Plan. **Not falsified.**

## 17. Adversarial findings

```text
P0=0
P1=0
P2=0
P3=0
```

No finding at any severity. In particular: no fourth file is implied anywhere; no production-source edit is proposed or implied; the live entry point is genuinely unreachable from ordinary Gradle tasks, using an already-proven, already-independently-tested double-gate mechanism; the corpus and context profiles are byte-identical to Unit 2's canonical definitions; the schedule arithmetic is exact; the AB/BA ordering description is accurate; the warm-up input, count, and profile are exact; model identity is the only isolated variable; the transparent proxy is both necessary (given `LocalHttpModelInferenceClient`'s actual, verified behavior) and non-duplicative of production protocol logic; exact-once dispatch and recovery are executable and internally consistent, extending an already-proven durability discipline; resource and residency gates are artifact-size-aware, scoped exclusively to the dedicated diagnostic daemon, and use an already-proven testability pattern; the advancement gate spans the complete 46-cell matrix and cannot be reduced to a partial or `R01`-only result; no ranking or qualification-credit authority is created even on a fully passing result; no model acquisition, run, campaign, production change, or Knowledge Discoverability Attempt 3 is authorized anywhere; every citation and source claim independently checks out; and no requirement is left as a TBD or operator-discretion gap.

## 18. Verdict

```text
VERDICT=ACCEPTED
PROGRAMME_STATUS=ACTIVE
FAMILY_F_STATUS=INCLUDED FOR PRE-QUALIFICATION DIAGNOSTIC SCOPING ONLY (unchanged by this review)
PLAN_AUTHORITY_UPON_ACCEPTANCE_AND_MERGE=EXACTLY THE THREE-FILE, OFFLINE, TEST-TIER IMPLEMENTATION IN SECTION 5 OF THE PLAN — NOTHING MORE
MODEL_ACQUISITION_AUTHORIZED=NO
MODEL_RUN_AUTHORIZED=NO
CAMPAIGN_AUTHORIZED=NO
PRODUCTION_CHANGE_AUTHORIZED=NO
KNOWLEDGE_DISCOVERABILITY_ATTEMPT_3_AUTHORIZED=NO
KNOWLEDGE_DISCOVERABILITY_CLOSURE=BLOCKED
NEXT_LAWFUL_ACTION=IMPLEMENTATION OF THE EXACT THREE FILES IN SECTION 5, WITH ZERO MODEL CONTACT, FOLLOWED BY ITS OWN COMPLETION REVIEW AND INDEPENDENT CONSTITUTIONAL REVIEW
```

This acceptance authorizes only the three-file offline implementation described in the Plan's own Section 5. It does not authorize model acquisition, model loading, execution, a campaign, production changes, or Knowledge Discoverability Attempt 3 — each of those remains gated behind the Plan's own Section 22 sequence (Completion Review, its Independent Constitutional Review, Readiness Review, its Independent Constitutional Review, and a separate Explicit Execution Approval), none of which this review performs or shortens.
