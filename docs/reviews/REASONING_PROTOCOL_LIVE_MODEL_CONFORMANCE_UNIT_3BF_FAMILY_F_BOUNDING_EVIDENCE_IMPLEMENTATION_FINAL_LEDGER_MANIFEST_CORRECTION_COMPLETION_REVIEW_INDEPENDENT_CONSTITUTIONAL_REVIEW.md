**Status:** Independent Constitutional Review of the Final Ledger/Manifest Correction Completion Review and the implementation it accepts — **ACCEPTED.** Every prior review artifact in this six-document chain (one `REVISE BEFORE ACCEPTANCE` Completion Review, one `ACCEPTED` Completion Review, one `REVISE BEFORE ACCEPTANCE` ICR, two further `REVISE BEFORE ACCEPTANCE` Completion Reviews, and this ICR's target `ACCEPTED` Completion Review) is preserved untouched and was read fresh, not inherited. Every material claim the target review makes — model-role correctness, campaign structure, all seventeen defect closures, the ledger governance contract (including the "status is governed, detail is not" distinction the prior ICR itself demanded), fail-closed ledger parsing, WP-A/B/C/D artifact re-verification before skip, unknown-file/symlink rejection, the seven-step manifest pre-read confinement ordering, fresh-copy cleanup, the double gate, source-inspection isolation, detached-build isolation, network/model isolation, and numeric-bound safety — was independently re-derived from primary governance and from direct, line-level reading of the actual current source (2384 lines) and its 50 tests, not from the review's own narrative. Fresh, independently-executed offline verification (compilation, the detached task, the original diagnostic task, the ordinary suite, dry-run isolation checks, and a standalone out-of-repo runtime invocation of the recompiled estimator) reproduces every claimed result. No new blocking defect was found. The target review's `ACCEPTED` verdict is constitutionally sustainable.

# Final Ledger/Manifest Correction Completion Review — Independent Constitutional Review

## 1. Preconditions

```text
$ git branch --show-current
main
$ git rev-parse HEAD
a88e2e3d5f2022c340618783d5b6b9d97d7a21d6
$ git rev-parse origin/main
a88e2e3d5f2022c340618783d5b6b9d97d7a21d6
$ git status --short
 M build.gradle.kts
 M tests/integration/ReasoningProtocolFamilyFDiagnosticTest.kt
?? docs/reviews/..._IMPLEMENTATION_COMPLETION_REVIEW.md                                   (REVISE BEFORE ACCEPTANCE)
?? docs/reviews/..._IMPLEMENTATION_CORRECTION_COMPLETION_REVIEW.md                        (ACCEPTED)
?? docs/reviews/..._IMPLEMENTATION_CORRECTION_COMPLETION_REVIEW_INDEPENDENT_CONSTITUTIONAL_REVIEW.md   (REVISE BEFORE ACCEPTANCE)
?? docs/reviews/..._IMPLEMENTATION_POST_ICR_CORRECTION_COMPLETION_REVIEW.md               (REVISE BEFORE ACCEPTANCE)
?? docs/reviews/..._IMPLEMENTATION_POST_ICR_CONFINEMENT_CORRECTION_COMPLETION_REVIEW.md   (REVISE BEFORE ACCEPTANCE)
?? docs/reviews/..._IMPLEMENTATION_FINAL_LEDGER_MANIFEST_CORRECTION_COMPLETION_REVIEW.md  (ACCEPTED -- this review's target)
?? tests/integration/ReasoningProtocolFamilyFBoundingEvidenceTest.kt
```

All expected. No unrelated or scratch file exists — independently `find`-checked for any `.codex*` file at the repository root and under `docs/`; none found. Preconditions matched exactly.

## 2. Historical review chain — read fresh

Read in full, end to end, not summarized: the historical Completion Review (`REVISE BEFORE ACCEPTANCE`), the correction Completion Review (`ACCEPTED`), the ICR of that correction review (`REVISE BEFORE ACCEPTANCE` — the malformed-ledger-fail-open, duplicate-completion, WP-B/C/D-reconstruction, unknown-file, and manifest-confinement blocking findings), the Post-ICR Correction Completion Review (`REVISE BEFORE ACCEPTANCE` — found the "status/detail" over-freeze and a governed-name symlink-follow escape), and the Post-ICR Confinement Correction Completion Review (`REVISE BEFORE ACCEPTANCE` — found the ledger detail literal-table over-freeze and the manifest external-path pre-read gap). None was overwritten; none was reinterpreted; each remains exactly as its own author left it. This ICR treats none of their conclusions as authoritative — every finding below was independently re-derived from the target review, the governance chain, and the current source.

## 3. Governance sources read fresh

Root Model Role and Research Question Scope Lock (Section B) and its first/correction ICRs; Planning Review, accepted Model Role Amendment, and amendment ICR; Experimental Reclassification Scope Lock, accepted Amendment, and amendment ICR; Capture-Proxy Bounding Scope Lock, accepted Amendment, and amendment ICR; Bounding Evidence Acquisition and Offline Estimator Plan, Sections 5–28; Plan Model Role Amendment and ICR; Bounding Evidence Implementation Authorization Decision, Sections 5–18 and Section 11; its accepted ICR; `FamilyFRole` Source Correction Amendment and its accepted ICR.

## 4. Implementation sources reviewed

`build.gradle.kts` (full diff); `tests/integration/ReasoningProtocolFamilyFDiagnosticTest.kt` (full diff); `tests/integration/ReasoningProtocolFamilyFBoundingEvidenceTest.kt` — read completely, 2384 lines, in three passes covering (a) WP-A/B/C/D and constants, (b) the ledger reader, manifest integrity, recovery/resume, and the entry point, and (c) the full offline test suite (50 `@Test` methods, enumerated and cross-checked by name). `FamilyFCampaignDefinition` and `FamilyFRole` were independently re-confirmed unedited except the authorized two-line correction, consistent with earlier independent readings in this same review chain.

## 5. Part A — review chain legitimacy

Every review file remains present, unedited, and distinct — independently confirmed via `git status --short` (six untracked review files, none showing as modified or deleted) and by having read each one's full, undisturbed content in Section 2. Decision Section 15 requires, in strict order: Implementation Completion Review; Independent Constitutional Review of that review and the implementation; acceptance and merge; a separate Evidence Production Authorization Decision. This ICR is exactly the required gate before the final `ACCEPTED` Completion Review and the implementation it accepts may be merged. No step in the six-document chain skipped a required gate: each `REVISE BEFORE ACCEPTANCE` verdict was followed by a bounded correction and a fresh review, never by silently re-asserting the prior conclusion. The final review does not erase the failed intermediate states — it is a fresh, independent judgment of a materially different (corrected) implementation state, which is the lawful way for a Completion Review chain to advance after `REVISE BEFORE ACCEPTANCE`.

**Finding: CONFORMING.**

## 6. Part B — authorized file surface

```text
$ git diff --stat -- build.gradle.kts tests/integration/ReasoningProtocolFamilyFDiagnosticTest.kt
 build.gradle.kts                                            | 13 +++++++++++++
 tests/integration/ReasoningProtocolFamilyFDiagnosticTest.kt  |  4 ++--
$ git diff --stat -- src/
(no output)
```

Independently re-confirmed unchanged from every prior review in this chain: the Gradle addition is exactly Decision Section 6's detached-task registration; the Diagnostic change is exactly the two-line `FamilyFRole` constant swap the accepted Amendment authorizes; the Bounding Evidence file is the Decision's own authorized offline implementation. No `src/`, runtime, Docker, persistence, QMD, UI, parser, or model-configuration file is touched anywhere in the diff.

**Finding: CONFORMING. No unauthorized implementation expansion.**

## 7. Part C — model-role correctness

```text
$ grep -n "FAMILY_F_SUBJECT_MODEL_NAME\|FAMILY_F_CONTROL_MODEL_NAME" tests/integration/ReasoningProtocolFamilyFDiagnosticTest.kt
62:const val FAMILY_F_SUBJECT_MODEL_NAME = "llama3.2:3b"
63:const val FAMILY_F_CONTROL_MODEL_NAME = "qwen2.5-coder:7b"
```

Matches the Scope Lock's Section B exactly (`DEPLOYED_BASELINE=qwen2.5-coder:7b`, `CONTROL_MODEL=qwen2.5-coder:7b`, `SUBJECT_MODEL=llama3.2:3b`). The estimator (`FamilyFBoundingEvidenceRequestEstimator.estimate()`, lines 172/185) reads `trial.role.modelName`/`trial.role.name` directly; no second literal or table exists anywhere in the 2384-line file (`grep -n '"qwen2\.5-coder:7b"\|"llama3\.2:3b"' tests/integration/ReasoningProtocolFamilyFBoundingEvidenceTest.kt` returns zero matches, including in comments — the header states the mapping in unquoted prose only).

Independently re-executed, fresh, a standalone out-of-repo JVM invocation of the freshly recompiled estimator classes:

```text
TOTAL_RECORDS=392
ROLE=CONTROL MODELS=[qwen2.5-coder:7b]
ROLE=SUBJECT MODELS=[llama3.2:3b]
```

**Finding: CONFORMING, independently re-derived at the record level, not merely at the source level.**

## 8. Part D — campaign structure

Independently re-traced `FamilyFCampaignDefinition.allTrials`'s own construction (unedited, in the frozen orchestration file): for each of 4 repetitions, `blockOrder(repetition)` yields exactly one `SUBJECT` block and one `CONTROL` block (alternating AB/BA order), each block containing 3 warm-ups plus the full 23-fixture × 2-profile cross product.

```text
FIXTURE_COUNT = 23
ROLE_COUNT = 2 (SUBJECT, CONTROL)
CONTEXT_PROFILE_COUNT = 2
REPETITIONS = 4
SCORED_CALLS = 23 x 2 x 4 x 2 = 368
WARM_UP_CALLS = 4 repetitions x 2 role blocks x 3 = 24
TOTAL_CALLS = 392
```

This is independently confirmed to be exactly what the target review's Section 3 states, and matches the Post-ICR Correction review's own correction of the intermediate "4 profiles × 2 repetitions" mislabeling found earlier in this chain (that mislabeling was corrected by the third document in the chain and does not appear in the final target review). `FamilyFCampaignDefinition`'s own `init` block independently `check()`s the 368/24/392/184-per-role invariants at class-load time — a structural guarantee, not an assertion that could silently drift.

**Finding: CONFORMING, semantically re-derived, not merely arithmetically re-checked.**

## 9. Part E — full defect-closure audit (independently re-derived, not inherited from the target review's own table)

| # | Historical defect | Controlling governance | Current implementation | Current test coverage | Independent finding |
|---|---|---|---|---|---|
| 1 | Real double-gated producer test | Decision §§7–9 | `familyFBoundingEvidenceEntryPoint` checks negative gate first, then both positive gates, before output resolution; default `produce` delegates to the real producer | Cases A/B/C/D + refusal + real-JVM-environment + structural ordering meta-test (7 tests) | **CURED** — case D independently re-executed, confirmed `COMPLETE`/392/invocation-count-1 |
| 2 | WP-B/C/D validator substance | Plan §§15–18 | Category+field, target/kind-conflation, documentation/observation checks | 3 dedicated tests with positive+negative branches | **CURED within authorized offline admissibility layer** — never resolves a value |
| 3 | Recovery/resume | Plan §22, Decision §11 | `produceOrResume` + shared `executeSteps(alreadyCompleted)` | 8+ dedicated tests (clean, partial, inconsistent, torn, complete, failed, conflicting, non-mutating reader) | **CURED** |
| 4 | Source-inspection exclusion scope | Repository precedent | Exclusion confined to the 26-line symbol-declaration only; import block unwrapped | 5 tests incl. self-non-detection, adjacent-before/after, allowed-code, region-size measurement | **CURED** |
| 5 | `FamilyFRole` mismatch | Scope Lock §B, Amendment | Corrected two-line constants | Role/campaign assertions + fresh runtime invocation (Section 7 above) | **CURED** |
| 6 | Stale documentation | — | Header states corrected mapping and the narrow amendment exception | Source-inspection/documentation re-read | **CURED** |
| 7 | Malformed ledger accepted (silent `mapNotNull`/`getOrNull`) | Decision §11, Plan §22 | Full whole-record regex, required trailing newline, blank-line/blank-field rejection, `Instant.parse` timestamp validation, unknown-step rejection, all via **thrown** `FamilyFBoundingEvidenceLedgerMalformedException` | `malformed, truncated, missing-field, unknown-step, invalid-metadata, and impossible-order ledger records all fail closed without mutation` (7 sub-cases + unterminated-JSONL case) | **CURED** — independently re-read the regex and control flow; every case throws, none is silently dropped |
| 8 | Duplicate completion collapsed into a `Set` | Decision §11, Plan §22 | `if (!completed.add(step)) throw ...` — duplicate detected before the value is used, not after | `one completion is valid while duplicate completions in any position fail closed...` (immediate + later-position cases) | **CURED** |
| 9 | WP-B/C/D completed artifacts silently reconstructed | Plan §22 | `validateResumeArtifacts` verifies exact index content byte-for-byte and requires the evidence directory genuinely empty **before** `executeSteps` is ever called; on any mismatch, `produceOrResume` returns `Rejected` without invoking `executeSteps` at all | `completed WP-B WP-C and WP-D artifacts are verified and safely skipped when valid` + `...each reject missing changed and malformed final artifacts without rewriting` | **CURED** — independently confirmed no reconstruction path exists once verification fails |
| 10 | Unknown campaign files accepted | Plan §§9, 21–22 | `validateCampaignSurface` walks the whole tree and rejects any path not in the exact `governedFiles`/`governedDirectories` allowlist | `unknown top-level nested and stale temporary campaign files fail closed while the exact governed surface passes` | **CURED** |
| 11 | Lexical-only manifest confinement | Decision §11 | `verifyManifest` requires exact lexical identity, `NOFOLLOW_LINKS` regular-file check, and `toRealPath()`-based confinement for both the manifest argument and every member | `manifest verification confines paths to the exact governed campaign-root member set` (external/sibling/alternate/absolute/traversal/symlink-outside/symlink-inside/broken-link cases) | **CURED** |
| 12 | Fresh-copy directory never deleted | Plan §21 | `try { ... } catch (failure) { primaryFailure = failure; throw failure } finally { deleteTree(copyRoot); suppress-on-cleanup-failure-if-primary-exists }` | 3 dedicated tests: success, false-verification, exception, each asserting the copy count is unchanged before/after | **CURED** |
| 13 | Ledger metadata: exact `detail` prose invented as governance | (was never governed — the defect was the invention itself) | `detail` validated only for non-blank; no exact-string table anywhere in the reader | `ledger enforces governed status while accepting nonblank informational detail` — explicitly accepts alternate and cross-step prose, rejects only blank | **CURED lawfully** — independently confirmed no `detail` literal is compared anywhere in `readCompletedSteps` |
| 14 | Symlink/real-path escape | Decision §11 | Every walked member checked via `Files.isSymbolicLink` before any read; regular members confined via `toRealPath()`/`startsWith` | `campaign and manifest reject every symbolic link including outside nested chained and broken escapes` (file/dir/nested/chained/broken/unknown-name/root-link — 7 sub-cases) | **CURED** |
| 15 | Fresh non-empty root silently accepted | Plan §22 | `produce()` explicitly checks `Files.list(outputRoot)` is empty before proceeding | `...fresh-producer-non-empty` sub-case of the unknown-files test; asserts the pre-existing file is untouched | **CURED** |
| 16 | Resumed WP-A identity/repository-input not verified | Plan §22 | `validateResumeArtifacts` re-derives fresh identity/repository-input/records/summary and requires byte-exact match | `resume rejects a ledger claiming WP_A_ESTIMATOR complete when the recorded artifact does not match a fresh recomputation` | **CURED** |
| 17 | External/sibling manifest read before confinement | Decision §11 | `verifyManifest`'s seven-step ordering (Section 10 below) — independently re-read and re-traced in the actual code | Same manifest test as #11, explicit external/sibling/alternate-directory/absolute-external/traversal cases, each asserted `false` before any content could matter | **CURED** |

**All seventeen historical defects independently confirmed CURED**, each verified against the actual current source and its own dedicated test, not against the target review's own restatement.

## 10. Part F — ledger governance contract (independently classified)

| Field/property | Governance status | Implementation validation | Controlling source | Independent finding |
|---|---|---|---|---|
| `step` | Exact six-identifier vocabulary | `FamilyFBoundingEvidenceStep.valueOf(...)`, else throws | Plan §22 | **CONFORMING** |
| `status` | For WP-B/C/D, Plan §§16–18 define a closed enum vocabulary; for PREFLIGHT/WP_A/WP_E, "OK" is this implementation's own internal completion sentinel, not itself Plan vocabulary | `requiredStatus` map checked exactly; wrong value throws | Plan §§16–18 (WP-B/C/D); internal consistency (others) | **CONFORMING, and independently distinguished from `detail`**: unlike `detail`, `status` for WP-B/C/D is drawn from a real governed enum, and for PREFLIGHT/WP_A/WP_E this deterministic, always-empty-source-list implementation can only ever produce exactly one value per step — checking it is corruption/tampering detection consistent with Decision §11's "malformed record" requirement, not new invented governance. This is a materially different situation from the historical `detail`-literal-table defect, which had no such deterministic single-value backing and no governed vocabulary at all. |
| `detail` | Not frozen by any governed vocabulary | Required non-blank; wording otherwise unconstrained | Local schema field only | **CONFORMING; independently re-confirmed no literal comparison exists in `readCompletedSteps`** |
| `recordedAt` | Structural timestamp | `Instant.parse`, else throws | Local schema field | **CONFORMING** |
| Record schema | Closed four-field record | Whole-record regex; extra/missing field fails the match | Deterministic writer's own schema | **CONFORMING** — independently confirmed via the `extra-metadata` test, which adds a fifth JSON key and is rejected |
| Field ordering | Deterministic `step,status,detail,recordedAt` | Regex anchors on exactly this order | Deterministic writer's own schema | **CONFORMING** |
| Final newline | Required JSONL boundary | `!rawLedger.endsWith("\n")` throws | Append-only JSONL design | **CONFORMING** |
| Duplicate semantics | Exact-once | `completed.add(step)` returning `false` throws | Plan §22 | **CONFORMING** |
| Step ordering | Exact governed sequence | `FamilyFBoundingEvidenceStep.entries[index]` compared per line | Plan §22 | **CONFORMING** |
| Terminal semantics | `COMPLETE`/`FAILED` mutually exclusive and final | Conflict rejected first in `produceOrResume`; writers reject the opposing marker; WP-E-without-terminal rejected | Plan §22 | **CONFORMING** |

**Finding: the contract is neither broader nor narrower than governance requires.** The one place this review scrutinized most carefully — whether "status" enforcement is itself an invented-governance over-freeze in the same way "detail" once was — was independently reasoned through rather than accepted from the target review's bare assertion, and found materially distinguishable (Plan-grounded enum vocabulary for WP-B/C/D; single-deterministic-value corruption detection for the rest).

## 11. Part G — ledger fail-closed integrity

Independently re-traced `readCompletedSteps` line by line against the required rejection list: malformed JSON (regex fails to match) — rejects; truncated record — rejects (regex requires the closing brace); missing field — rejects (regex requires all four groups); extra field — rejects (regex is anchored `^...$`, no trailing content permitted); invalid escape — rejects (the regex's escape-sequence alternation excludes `\q`, confirmed by the dedicated `invalid-json-escape` test case); invalid timestamp — rejects (`Instant.parse` throws, caught and converted); unknown step — rejects (`valueOf` throws, caught and converted); wrong governed status — rejects (`requiredStatus` mismatch); duplicate completion — rejects; invalid order — rejects; missing final newline — rejects (explicit pre-check before any line-splitting). Every one of the eleven-plus rejection conditions independently re-verified present, each with its own dedicated test, each asserting the campaign directory is byte-unchanged after rejection (`assertRejectedWithoutMutation`).

**No malformed record is skipped** — the reader processes the whole ledger as one string and throws on the first violation, never returning a partial `Set`. **No mutation occurs before validation** — `produceOrResume` calls `readCompletedSteps` and converts any thrown exception into `Rejected` before ever calling `executeSteps`. **No invalid state is normalized into valid** — confirmed by the exhaustive per-case tests, each independently re-read.

**Finding: CONFORMING.**

## 12. Part H — recovery artifact integrity

`validateResumeArtifacts` is called by `produceOrResume` **before** `executeSteps`; a returned non-null message causes immediate rejection, and `executeSteps` — the only code path capable of writing artifacts — is never reached. For WP-A: identity (regex + `Instant.parse`), repository-inputs (byte-exact against a freshly recomputed hash map), records and summary (byte-exact against a freshly recomputed estimator run) are each independently re-derived and compared. For WP-B/C/D: the exact expected index string (a compile-time constant matching the deterministic empty-source-list writer output) and directory emptiness are both checked. No artifact is recreated or rewritten anywhere in this path — `executeSteps` only ever writes an artifact under `if (!Files.exists(path))`, and by the time it runs (post-validation), a verified-complete step's artifacts already exist and are left untouched.

**Finding: CONFORMING — no manufactured recovery state.**

## 13. Part I — fresh-root / campaign-surface safety

`produce()` requires the output root absent or empty (`Files.list(outputRoot)` must find nothing), independently re-confirmed via the `fresh-producer-non-empty` test, which also asserts the pre-existing file is untouched after rejection. `produceOrResume` calls `validateCampaignSurface` first, rejecting any unknown top-level/nested/stale-temporary path via a closed allowlist (`governedFiles`/`governedDirectories`), independently re-confirmed via three dedicated sub-cases. Rejected content is never deleted in any code path this review traced.

**Finding: CONFORMING.**

## 14. Part J — symbolic-link policy

Independently determined the all-symlinks-rejected policy is lawful: Decision §11 and Plan §22 require fail-closed behavior at every integrity boundary, and rejecting every link (rather than attempting to distinguish safe from unsafe links) is the narrowest interpretation that cannot under-reject. `validateCampaignSurface` checks `Files.isSymbolicLink` on the root and on every walked member via `NOFOLLOW_LINKS`-safe calls **before** any `Files.isDirectory`/`isRegularFile`/read call that could otherwise silently follow a link. Independently re-traced each of the seven symlink test sub-cases (governed-name file link, governed-name directory link, nested link, chained link, broken link, unknown-name link, campaign-root link) against the actual `hasSymbolicLink`/`isRealPathConfined` code and confirmed each is detected before any follow. TOCTOU (a filesystem mutation between check and read) is theoretically possible but immaterial to this single-process, offline, `@TempDir`-scoped architecture — independently agreed with the target review's own reasoning here, not merely accepted.

**Finding: CONFORMING; no unrealistic requirement was manufactured, and none was needed.**

## 15. Part K — manifest pre-read confinement (critical)

Independently re-read `verifyManifest` character by character against the required ordering:

```text
1. suppliedManifest != expectedManifest (root/SHA256SUMS.txt)   -> reject
2. validateCampaignSurface(normalizedRoot) != null               -> reject
3. Files.isSymbolicLink(suppliedManifest)                        -> reject
4. !Files.isRegularFile(suppliedManifest, NOFOLLOW_LINKS)        -> reject
5. !isRealPathConfined(normalizedRoot, suppliedManifest)         -> reject
6. Files.isSymbolicLink(normalizedRoot)                          -> reject
7. toRealPath() on root and manifest; !startsWith / equality     -> reject
   ---- only then ----
   Files.readAllLines(suppliedManifest, ...)
```

This is confirmed, by direct reading (not by trusting the target review's own numbered list), to precede the only line in the function that reads external content. Independently re-executed test coverage confirms external, sibling, alternate-directory, absolute-external, traversal-equivalent, symlink-to-outside, symlink-to-inside-target, and broken-symlink manifest arguments all return `false` — and the invalid-UTF-8 external-file case specifically demonstrates rejection occurs *before* any attempted decode (a `false` result, not a decoding exception).

**No external, sibling, alternate-directory, absolute-external, traversal, or symlinked manifest can be opened or consumed before rejection.**

**Finding: CONFORMING. Not blocking.**

## 16. Part L — manifest content integrity

After confinement, `verifyManifest` independently re-confirmed to require: exact governed member set (`seen == manifestCoveredFiles`), unique entries (`seen.add(...)` guards duplicates), valid lowercase-hex 64-character hash syntax, rejection of absolute/backslash/Windows-drive/`..`-traversal relative paths, per-member `NOFOLLOW_LINKS` regular-file + real-path-confinement checks, and exact hash match.

**Finding: CONFORMING.**

## 17. Part M — fresh-copy verification

Independently re-read `verifyFromFreshCopy`: `validateCampaignSurface(root)` is checked before any copy begins (no outside-root link is ever followed into the copy); the copy is independently re-verified via the same `verifyManifest` path; cleanup occurs in a `finally` block on every exit path (success, `false` result, and thrown exception), with correct primary/suppressed exception semantics when both a primary failure and a cleanup failure occur. Three dedicated tests independently re-confirm the copy count is unchanged across all three exit paths, and that the original campaign's bytes are untouched.

**Finding: CONFORMING.**

## 18. Part N — double gate / execution authority

Independently re-executed (fresh, this review's own run) and re-traced: `false/false`, `true/false`, `false/true` never invoke the producer (`invocationCount == 0` in each case); `true/true` with the negative gate absent invokes the real, unmocked `FamilyFBoundingEvidenceProducer.produce` exactly once, producing a genuine offline `COMPLETE`/392-record result inside a `@TempDir`. The offline test exercises only in-process, filesystem-local behavior; no evidence-production authority is granted or exercised — Decision §3/§9 both confirm this path is not eligible for Evidence Completion Review or Bound Selection regardless of outcome, and no test ever sets the real `PARKER_REASONING_FAMILY_F_BOUNDING_EVIDENCE_APPROVED` value in this JVM.

**Finding: CONFORMING. No evidence-production authority arises from implementation acceptance.**

## 19. Part O — WP-A/B/C/D validators

| WP | Governance requirement | Implementation | Tests | Independent finding |
|---|---|---|---|---|
| A | Frozen schedule; production prompt/serializer; exact counts/hashes/Base64/maxima; no selected bound | Direct `FamilyFCampaignDefinition.allTrials` iteration; unmodified `SyntheticContextProfiles`, `DefaultReasoningPromptBuilder`, `defaultOllamaRequestBody` | IDs/coverage/determinism/Unicode/hash/tie/overflow (7 tests) | **CONFORMING** |
| B | Admissible category + provenance; no retrieval; no manufactured resolution | Real category-membership + six-field completeness check | Admissible/wrong-category/missing-field positive+negative | **CONFORMING; no false-positive pass found** |
| C | Distinct count/aggregate-byte semantics; reject conflation | Target-to-limit-kind mapping | Correct-kind/conflated-kind positive+negative | **CONFORMING; no false-positive pass found** |
| D | Documentation-only; reject observation-derived evidence | Category + `basedOnObservation` check | Documentation-only/observation-derived/wrong-category | **CONFORMING; no false-positive pass found** |

No validator was found to be a sophisticated-looking pass-through — every rejection path independently re-traced diverges from the acceptance path for a real, Plan-grounded reason.

## 20. Part P — source-inspection safeguard

Independently re-measured: the exclusion region (lines 1234–1259) wraps only the `FAMILY_F_BOUNDING_EVIDENCE_FORBIDDEN_SYMBOLS` declaration; the import block is unwrapped. `familyFBoundingEvidenceForbiddenSymbolsFound` correctly does not self-detect (independently re-executed the fragment-built synthetic tests), correctly detects a forbidden token immediately before and after the region, and the scanned text independently confirmed to still contain the import statements and the estimator's own implementation. The later ledger/manifest/recovery corrections added substantial new code but did not touch or widen the exclusion region — independently confirmed by its unchanged line span relative to the version this review chain's earlier documents already examined.

**Finding: CONFORMING; no new blind spot.**

## 21. Part Q — detached build boundary

```text
$ ./gradlew test check build assemble --dry-run --console=plain | grep -i boundingEvidence
(no output for any of the four)
```

Independently re-read `build.gradle.kts`'s task registration: only `shouldRunAfter(tasks.test)` (an ordering hint, never a dependency edge); no `dependsOn`, `finalizedBy`, or other lifecycle attachment anywhere in the file referencing this task.

**Finding: CONFORMING.**

## 22. Part R — network / model / provider isolation

Independently traced every reachable call from `produce`/`produceOrResume`/`executeSteps`/the entry point: `SyntheticContextProfiles.construct` → `DefaultReasoningPromptBuilder.buildPrompt` (re-confirmed pure string concatenation) → `defaultOllamaRequestBody` (re-confirmed pure string formatting) for construction; exclusively `java.nio.file.Files` local calls for everything else. No `java.net.*`, no `ProcessBuilder`, no `Runtime.exec`, no Docker, no Ollama, no `FamilyFLiveEntryPoint` or any other capture-proxy/live-diagnostic symbol is imported or referenced anywhere in the file.

**Finding: CONFORMING; no reachable path to a socket, HTTP endpoint, external process, Docker, or Ollama.**

## 23. Part S — numeric bound

`PROPOSED_MAX_REQUEST_BOUND` remains the only figure ever emitted, in both JSON and Markdown, always paired with an explicit "evidence result only, not an accepted bound" disclaimer. No response/header/runtime/RAM/disk/`E`/`R` value is computed, inferred, or reported anywhere; WP-B/C/D never resolve to anything but `UNRESOLVED*`/`NOT_ADMISSIBLE` under this file's own always-empty-source-list usage.

**Finding: `NUMERIC_BOUND_SELECTED=NONE` independently confirmed true in substance.**

## 24. Part T — historical / provenance safety

No governed Family F evidence exists anywhere in the repository (no campaign artifact, no evidence root). No reference to Knowledge Discoverability Attempts 1–2, Unit 2/2-D, or the lighthouse observation appears anywhere in the implementation file. No historical evidence is touched, pooled, or reclassified by anything this review examined.

**Finding: CONFORMING.**

## 25. Part U — fresh adversarial search

Independently swept for: ledger over/under-constraint (Section 10 above — found sound, with the status/detail distinction independently re-derived rather than assumed); parser partial success (none — whole-ledger, all-or-nothing); duplicate masking (none — rejected before use); path escape (none — real-path confinement throughout); symlink escape (none — Section 14); manifest pre-read access (none — Section 15); stale-artifact acceptance (none — `rejectUnexpectedFinalStateForIncompleteStep` catches artifacts present without a completing ledger record); recovery rewriting (none — Section 12); cleanup failure (handled — Section 17); hash bypass (none — exact SHA-256 comparison throughout); source-inspection blind spot (none new — Section 20); task-graph leakage (none — Section 21); external execution reachability (none — Section 22); nondeterministic schedule (none — `FamilyFCampaignDefinition`'s own structural guarantees, Section 8); duplicated source of truth (none — Section 7); false validator pass (none — Section 19); manufactured success/evidence (none found anywhere in this pass); unauthorized scope expansion (none — Section 6).

**One residual, non-blocking observation, independently discovered by this review and not mentioned in the target review:** `verifyExactArtifact`'s error messages for WP-B/C/D (`"...but ${path.fileName} is malformed or changed"`) do not distinguish a byte-for-byte content mismatch from a wrong-type (e.g., directory instead of file) condition as precisely as `verifyIdentityArtifact`'s regex-based message does; this affects only the human-readable rejection string, never the pass/fail outcome, and every test that exercises these paths already asserts the correct binary result. **Non-blocking; cosmetic.**

No genuine new blocking defect was found. This review did not manufacture one to appear independent.

## 26. Part V — Completion Review accuracy

The target review accurately describes the implementation in every respect this ICR independently checked: the authorized-surface claim, the model-role claim, the campaign-dimension claim, all seventeen defect-closure claims, the ledger-governance-contract claim (including its own correct distinction that `detail` was over-frozen historically but `status` legitimately is not), the manifest seven-step ordering claim, the fresh-copy claim, the double-gate claim, the WP-A/B/C/D claims, the isolation claims, and the numeric-bound claim. Its stated test counts (50 for the detached task, 133/1-skip for the diagnostic task, 2253/5-skip for the ordinary suite) were independently reproduced by this review's own fresh execution, matching exactly. It correctly applies the acceptance standard Decision §15 implies (every governed requirement satisfied; every historical blocker cured; no new blocker), and its `ACCEPTED` verdict is independently justified, not merely asserted.

**Finding: the Completion Review is accurate and its verdict is sound.**

## 27. Part W — constitutional risk sweep

| Risk | Finding |
|---|---|
| Authority expansion | Not present — Section 6 |
| Implementation laundering | Not present — every defect fix is independently traceable to real code and a real test, not a renamed symptom |
| Model-selection leakage | Not present — Section 7; `SUBJECT_MODEL` carries no preference claim anywhere |
| Evidence-production leakage | Not present — Section 18, 23 |
| Numeric-bound laundering | Not present — Section 23 |
| Historical rewriting | Not present — Section 24 |
| Provenance pooling | Not present — Section 24 |
| Fail-open recovery | Not present — Sections 9–17 (this was the central historical risk; now closed) |
| Path-confinement failure | Not present — Sections 14–16 |
| Duplicated source of truth | Not present — Section 7 |
| Production-runtime coupling | Not present — Section 22 |
| Premature execution | Not present — Section 18; every gate independently re-verified |
| Qualification leakage | Not present — nothing here touches Unit 3-A qualification tier or any advancement gate |
| Acceptance-before-merge ambiguity | Not present — this ICR and the target review both explicitly distinguish acceptance from merge, and merge from every later-governed step (Section 28 below) |

## 28. Verdict

```text
ACCEPTED
```

Every prior blocking defect (seventeen, across the full six-document chain) is independently confirmed cured against the actual current source and its tests, not against any review's own narrative. The ledger governance contract, manifest pre-read confinement, symlink policy, recovery-artifact integrity, and fresh-copy cleanup were each independently re-traced at the code level and found sound. No new blocking defect was found; one cosmetic, non-blocking observation is recorded (Section 25). The implementation remains exactly within its accepted two-file (plus the separately authorized two-line `FamilyFRole` correction) authority.

## 29. Governance consequence if ACCEPTED

1. **May be committed/merged:** yes — this ICR, the target Completion Review it reviews, and the underlying implementation diffs are now suitable for a governance-acceptance commit and merge, per Decision §15's sequence (Completion Review → ICR → acceptance and merge).
2. **Exact files belonging in that acceptance commit:**
   ```text
   build.gradle.kts
   tests/integration/ReasoningProtocolFamilyFDiagnosticTest.kt
   tests/integration/ReasoningProtocolFamilyFBoundingEvidenceTest.kt
   docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_BOUNDING_EVIDENCE_IMPLEMENTATION_FINAL_LEDGER_MANIFEST_CORRECTION_COMPLETION_REVIEW.md
   docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_BOUNDING_EVIDENCE_IMPLEMENTATION_FINAL_LEDGER_MANIFEST_CORRECTION_COMPLETION_REVIEW_INDEPENDENT_CONSTITUTIONAL_REVIEW.md
   ```
3. **Historical failed reviews/ICRs as audit history:** yes, they should also be committed — Decision §11/Plan §22's own fail-closed, auditable-history ethos, and this whole governance programme's established practice throughout this session (every `REVISE BEFORE ACCEPTANCE` artifact has always been preserved and committed alongside its eventual correction), both favor committing:
   ```text
   docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_BOUNDING_EVIDENCE_IMPLEMENTATION_COMPLETION_REVIEW.md
   docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_BOUNDING_EVIDENCE_IMPLEMENTATION_CORRECTION_COMPLETION_REVIEW.md
   docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_BOUNDING_EVIDENCE_IMPLEMENTATION_CORRECTION_COMPLETION_REVIEW_INDEPENDENT_CONSTITUTIONAL_REVIEW.md
   docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_BOUNDING_EVIDENCE_IMPLEMENTATION_POST_ICR_CORRECTION_COMPLETION_REVIEW.md
   docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_BOUNDING_EVIDENCE_IMPLEMENTATION_POST_ICR_CONFINEMENT_CORRECTION_COMPLETION_REVIEW.md
   ```
   alongside the final accepted pair, as the complete audit trail — not merely the final `ACCEPTED` state in isolation.
4. **Implementation authority effective after merge:** exactly the bounded offline construction and offline verification Decision §§5–18 already define — compiling the two authorized files, running the detached `reasoningProtocolFamilyFBoundingEvidence` task offline, and nothing else.
5. **Remains explicitly NOT AUTHORIZED:** everything Section 30 below lists.

This determination does **not** itself perform the commit or merge.

## 30. Explicit authority distinctions

| Authority | Status after this ICR's acceptance |
|---|---|
| Offline implementation acceptance (compile + offline test) | Becomes eligible for commit/merge, not performed here |
| Estimator execution (offline, in-process) | Already exercised only inside `@TempDir`-scoped tests; remains non-governed test output |
| Evidence production (real campaign, real output root) | **NOT AUTHORIZED** — requires a separate Evidence Production Authorization Decision |
| Explicit Execution Approval | **NOT AUTHORIZED** |
| Live Family F diagnostic | **NOT AUTHORIZED** |
| Model acquisition | **NOT AUTHORIZED** |
| Qualification | **NOT AUTHORIZED** |
| Remedy selection | **NOT AUTHORIZED** |
| Production deployment | **NOT AUTHORIZED** |

## 31. Exact next lawful action

```text
NEXT_LAWFUL_ACTION =
A governance-acceptance commit/push task, confined to staging exactly the
files named in Section 29 item 2 (and, per item 3, the five preserved
historical review/ICR artifacts as audit history), with the exact commit
message such a task specifies -- not performed by this review. After that
merge, the exact two-file offline implementation and offline verification
already defined by Decision Sections 5-18 becomes the only newly effective
authority; every item in Section 30 marked NOT AUTHORIZED remains so until
its own, separately governed decision.
```

## 32. git status --short

```text
 M build.gradle.kts
 M tests/integration/ReasoningProtocolFamilyFDiagnosticTest.kt
?? docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_BOUNDING_EVIDENCE_IMPLEMENTATION_COMPLETION_REVIEW.md
?? docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_BOUNDING_EVIDENCE_IMPLEMENTATION_CORRECTION_COMPLETION_REVIEW.md
?? docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_BOUNDING_EVIDENCE_IMPLEMENTATION_CORRECTION_COMPLETION_REVIEW_INDEPENDENT_CONSTITUTIONAL_REVIEW.md
?? docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_BOUNDING_EVIDENCE_IMPLEMENTATION_FINAL_LEDGER_MANIFEST_CORRECTION_COMPLETION_REVIEW.md
?? docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_BOUNDING_EVIDENCE_IMPLEMENTATION_FINAL_LEDGER_MANIFEST_CORRECTION_COMPLETION_REVIEW_INDEPENDENT_CONSTITUTIONAL_REVIEW.md
?? docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_BOUNDING_EVIDENCE_IMPLEMENTATION_POST_ICR_CONFINEMENT_CORRECTION_COMPLETION_REVIEW.md
?? docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_BOUNDING_EVIDENCE_IMPLEMENTATION_POST_ICR_CORRECTION_COMPLETION_REVIEW.md
?? tests/integration/ReasoningProtocolFamilyFBoundingEvidenceTest.kt
```

## 33. git diff --stat

```text
 build.gradle.kts                                            | 13 +++++++++++++
 tests/integration/ReasoningProtocolFamilyFDiagnosticTest.kt  |  4 ++--
 2 files changed, 15 insertions(+), 2 deletions(-)
```

(unchanged from before this review — confirms neither tracked file was touched)

## 34. Confirmation: implementation untouched

Independently confirmed: `build.gradle.kts` and `tests/integration/ReasoningProtocolFamilyFDiagnosticTest.kt` diffs are byte-identical to their state before this review began. `tests/integration/ReasoningProtocolFamilyFBoundingEvidenceTest.kt` remains untracked with unchanged content — read-only throughout this entire review.

## 35. Confirmation: all Completion Reviews / previous ICRs untouched

Independently confirmed via `git status --short`: all six pre-existing review/ICR documents remain present, untracked, and unmodified (none shows as `M`; none was deleted or overwritten). Only this document was created.

## STOP conditions confirmed

```text
NO implementation file edited.
NO Completion Review edited or overwritten.
NO historical ICR edited.
NO model called, loaded, or contacted.
NO Ollama/provider/network endpoint contacted.
NO Docker or Parker runtime invoked.
NO evidence producer invoked; NO evidence-production approval variable set or used.
NO governed evidence produced.
NO numeric bound selected.
NO deployment performed.
NO file staged, committed, or pushed.
Commands executed: git status/diff/rev-parse/branch/log/grep/find; four
  `./gradlew <task> --dry-run` graph resolutions; four fresh, forced
  (--rerun-tasks) Gradle executions (compileLiveModelEvaluationKotlin,
  reasoningProtocolFamilyFBoundingEvidence, reasoningProtocolFamilyFDiagnostic,
  test); one standalone, out-of-repo JVM reflection invocation of the
  already-compiled, unmodified estimator classes. All read-only, local-only,
  or offline test execution; none touched network, model, or Docker.
```
