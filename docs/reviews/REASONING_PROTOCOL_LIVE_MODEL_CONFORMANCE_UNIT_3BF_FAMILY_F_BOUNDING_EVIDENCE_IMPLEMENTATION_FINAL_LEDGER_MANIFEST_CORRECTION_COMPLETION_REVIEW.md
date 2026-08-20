# Family F Bounding Evidence Final Ledger/Manifest Correction Completion Review

**Status:** Fresh independent Implementation Completion Review after the final
ledger-contract and manifest-confinement correction — **ACCEPTED**.

## 1. Repository state and review boundary

```text
BRANCH=main
HEAD=a88e2e3d5f2022c340618783d5b6b9d97d7a21d6
ORIGIN_MAIN=a88e2e3d5f2022c340618783d5b6b9d97d7a21d6
```

The pre-review working tree matched the task's exact expected state.
No Codex scratch or staging file existed. This review created exactly this one
document and did not modify implementation, governance, or historical reviews.

## 2. Sources read fresh

Read fresh: the Family F Model Role and Research Question Scope Lock and accepted
ICR chain; Planning Review, accepted Model Role Amendment and ICR; Experimental
Reclassification Scope Lock, accepted Amendment and ICR; Capture-Proxy Bounding
Scope Lock, accepted Amendment and ICR; Bounding Evidence Acquisition and
Offline Estimator Plan; Plan Model Role Amendment and ICR; Bounding Evidence
Implementation Authorization Decision and ICR; FamilyFRole Source Correction
Amendment and ICR; and all five historical completion-review/ICR artifacts named
by this task.

Read completely: `build.gradle.kts`,
`tests/integration/ReasoningProtocolFamilyFDiagnosticTest.kt`, and
`tests/integration/ReasoningProtocolFamilyFBoundingEvidenceTest.kt`.
Referenced role, schedule, corpus, prompt, serialization, ledger, recovery,
manifest, copy, source-scan, and Gradle graph paths were traced independently.

## 3. Authorized surface, roles, and campaign

**Authorized surface: CONFORMING.** The Gradle addition is the authorized
detached task; the Diagnostic change is the separately authorized two-line role
correction; the Bounding Evidence test file is the authorized offline
implementation. No `src/`, runtime, Docker, persistence, QMD, UI, parser,
model configuration, or production source changed.

```text
DEPLOYED_BASELINE=qwen2.5-coder:7b
CONTROL_MODEL=qwen2.5-coder:7b
SUBJECT_MODEL=llama3.2:3b
```

`FamilyFRole` is authoritative and correct. The estimator consumes
`trial.role.name` and `trial.role.modelName` directly; no second handwritten
model-role map exists.

The Plan and frozen source independently establish 23 fixtures, two semantic
roles, two context profiles, and four repetitions:

```text
SCORED_CALLS=23 x 2 x 2 x 4 = 368
WARM_UP_CALLS=4 repetitions x 2 roles x 3 = 24
TOTAL_CALLS=392
```

## 4. Full prior-defect closure

| # | Defect | Current implementation | Test coverage | Independent finding |
|---|---|---|---|---|
| 1 | Real double-gated producer test | Real entry delegates only when both approvals are true | All four combinations; exact call count | **CURED** |
| 2 | WP-B/C/D validator substance | Category/provenance, target/kind, and documentation/observation rules enforced | Positive and adversarial validator cases | **CURED within authorized offline layer** |
| 3 | Recovery/resume | Strict parsed ledger drives verified skip/resume | Clean, partial, COMPLETE, FAILED, conflict, corrupt states | **CURED** |
| 4 | Source-inspection exclusion | Only the forbidden declaration is excluded | Self-detection and adjacent-token cases | **CURED** |
| 5 | FamilyFRole mismatch | Correct constants in authoritative enum | Role and campaign assertions | **CURED** |
| 6 | Stale documentation | Header records corrected role authority | Source inspection | **CURED** |
| 7 | Malformed JSONL syntax | Whole-record grammar, escapes, timestamp and final newline enforced | Malformed/truncated/escape/schema cases | **CURED** |
| 8 | Duplicate completion | Duplicate rejected before state construction | Duplicate-step cases | **CURED** |
| 9 | WP-B/C/D completed-artifact verification | Exact indexes and empty evidence directories verified before skip | Tamper/missing/type cases | **CURED** |
| 10 | Unknown campaign files | Recursive exact surface allowlist | Unknown file/directory/temp cases | **CURED** |
| 11 | Lexical manifest confinement | Exact members and confined lexical forms required | Absolute/rooted/backslash/traversal/member cases | **CURED** |
| 12 | Fresh-copy cleanup | `finally` cleanup preserves primary and suppresses cleanup failure | Success/false/exception/cleanup-failure cases | **CURED** |
| 13 | Ledger semantic metadata overreach | Governed status is exact; detail is structural and nonblank, not literal | Alternate and cross-step prose accepted; blank rejected; wrong status rejected | **CURED lawfully** |
| 14 | Symlink/real-path confinement | All links rejected; regular members resolve under real root | File/dir/nested/chained/broken/root-link cases | **CURED** |
| 15 | Fresh non-empty root | Only absent or empty lawful root accepted | Non-empty root preserved on rejection | **CURED** |
| 16 | Resumed WP-A identity/repository inputs | Current deterministic inputs and identity artifacts reverified | Missing, changed, malformed and byte-different cases | **CURED** |
| 17 | External/sibling manifest pre-read confinement | Exact lexical manifest, type, link, surface and real-path checks precede read | External/sibling/alternate/absolute/traversal/link/broken/invalid-UTF-8 cases | **CURED** |

## 5. Ledger governance contract

| Field/property | Governance status | Implementation validation | Controlling source | Finding |
|---|---|---|---|---|
| `step` | Exact six identifiers governed | Exact allowlist | Accepted Plan §22 | **CONFORMING** |
| `status` | Completion semantics governed per work package | Exact required status per step; arbitrary values reject | Accepted Plan work-package outcomes and §22 | **CONFORMING** |
| `detail` | Schema field; prose not frozen | Present, decoded, nonblank; wording otherwise unrestricted | Authorized ledger schema, with no primary prose freeze | **CONFORMING; no literal promotion** |
| `recordedAt` | Structural timestamp field | Required and parsed as an instant | Authorized ledger schema | **CONFORMING** |
| Record schema | Closed four-field record | Missing/extra/reordered fields reject | Authorized deterministic JSONL schema | **CONFORMING** |
| Field ordering | Deterministic serialized order | Exact `step,status,detail,recordedAt` order | Authorized deterministic schema | **CONFORMING** |
| Final newline | Required JSONL integrity boundary | Missing final newline rejects | Authorized append-only JSONL design | **CONFORMING** |
| Duplicate semantics | Exact-once | Duplicate step rejects before Set/state collapse | Accepted Plan §22 | **CONFORMING** |
| Step ordering | Exact | Strict governed sequence | Accepted Plan §22 | **CONFORMING** |
| Terminal semantics | COMPLETE/FAILED exclusive and final | Conflict, premature terminal and post-terminal content reject; append after finalization throws | Accepted Plan §22 | **CONFORMING** |

The final correction removes the unsupported exact prose table while retaining
the governed status contract. Tests now demonstrate that alternate nonblank
details, including prose resembling another step, remain informational. They
also independently reject blank detail and incorrect statuses. No test literal
has been elevated into governance.

## 6. Fail-closed state machine and recovery

The ledger rejects malformed JSON, truncation, missing or extra fields, invalid
escapes/timestamps, unknown steps, wrong governed statuses, duplicates,
impossible order, a missing final newline, post-finalization append, and
COMPLETE/FAILED conflict. Parsing is whole-ledger and fail-closed: it neither
skips a malformed line nor returns a partial clean state, mutates artifacts
before validation completes, or silently rewrites malformed state.

Completed WP-A/B/C/D artifacts are verified before skip. Checks cover existence,
regular-file/directory type, governed bytes/hashes, exact empty evidence state,
current identity and repository inputs, and deterministic WP-A records/summary.
Recovery does not recreate an artifact to manufacture success.

## 7. Root, symlink, and manifest integrity

Fresh production requires an absent or empty lawful root. A non-empty root
rejects without deleting its contents. The exact recursive top-level/member
allowlist rejects unknown files, directories and stale temporary artifacts.

The all-symbolic-links-rejected policy is lawful and fail-closed. Link identity
is checked with `NOFOLLOW_LINKS` before resolution or reads; file, directory,
nested, chained-escape and broken links reject. Accepted regular members must
resolve beneath the real campaign root.

`verifyManifest(root, manifestPath)` performs this order before any manifest
content access:

1. derive normalized `root/SHA256SUMS.txt`;
2. require the supplied absolute lexical path to equal it exactly;
3. validate the campaign surface;
4. reject manifest links;
5. require a regular file with `NOFOLLOW_LINKS`;
6. apply real-path confinement; and
7. require the resolved manifest beneath the resolved root.

Only then does it call `readAllLines`. External, sibling, alternate-directory,
absolute-external, traversal-equivalent, outside/inside/broken-link arguments
reject. Invalid UTF-8 external files also return false, demonstrating that
external content was not consumed before confinement.

After confinement, manifest validation requires the exact governed member set,
unique entries, valid hash syntax, no unknown/missing members, lexical and
real-path confinement, regular non-link member files, and matching hashes.

## 8. Fresh-copy verification

The source campaign is validated before copy, so no outside-root link is
followed. The copy is independently revalidated and is removed after success,
false verification, or exception. A cleanup failure is primary when verification
succeeded and is suppressed on the original failure otherwise. The original
campaign remains untouched.

## 9. Double gate and work-package validators

The actual producer entry behaves:

```text
false/false -> no producer
true/false  -> no producer
false/true  -> no producer
true/true   -> producer exactly once
```

Offline verification neither grants nor exercises evidence-production authority.

| WP | Governance requirement | Implementation and tests | Finding |
|---|---|---|---|
| A | Frozen deterministic campaign, production prompt/serializer, maxima only | Schedule, identity, inputs, records and summary verified byte-exact; no accepted bound | **CONFORMING** |
| B | Admissible offline primary-source category/provenance; no retrieval | Validator and exact unresolved no-source artifacts tested | **CONFORMING within authorized layer** |
| C | Distinct count/aggregate-byte bounding semantics | Validator and exact unresolved artifacts tested | **CONFORMING within authorized layer** |
| D | Official documentation only; reject observation-derived evidence | Validator and exact unresolved artifacts tested | **CONFORMING within authorized layer** |

## 10. Source, build, and external isolation

Source inspection uses the minimal declaration exclusion, avoids self-detection,
detects an adjacent prohibited symbol, and continues to scan substantive
implementation. The latest edits do not widen the blind spot.

The Bounding Evidence task has no dependency, finalizer, indirect lifecycle
coupling, or configuration side effect. Fresh combined dry-run output shows it
absent from `test`, `check`, `build`, and `assemble`.

Reachable paths are in-process and filesystem-only, ending at pure prompt and
request serialization. No socket, HTTP, provider, Ollama/model client,
`ProcessBuilder`, `Runtime.exec`, Docker, Parker runtime, or external evidence
producer is reachable.

## 11. Bound and provenance safety

```text
NUMERIC_BOUND_SELECTED=NONE
```

No observed/calculated value is promoted to an operational bound. No governed
evidence was produced; Attempts 1–2 and Unit 2/2-D were not pooled; the
lighthouse observation is absent; historical evidence is untouched.

## 12. Fresh adversarial search

No new blocking defect was found. In particular, no semantic over/underconstraint,
partial parser success, duplicate masking, symlink/path escape, pre-read manifest
access, stale-state acceptance, recovery rewrite, cleanup masking, hash bypass,
false validator pass, graph leak, reachable network/process path, nondeterministic
schedule, duplicated role source, manufactured pass/evidence, or unauthorized
scope expansion was identified.

## 13. Fresh offline verification

```text
./gradlew compileLiveModelEvaluationKotlin --rerun-tasks --console=plain
BUILD SUCCESSFUL in 22s; 3 actionable tasks executed

./gradlew reasoningProtocolFamilyFBoundingEvidence --rerun-tasks --console=plain
BUILD SUCCESSFUL in 26s; 4 actionable tasks executed
50 tests; 0 skipped; 0 failures; 0 errors

./gradlew reasoningProtocolFamilyFDiagnostic --rerun-tasks --console=plain
BUILD SUCCESSFUL in 31s; 4 actionable tasks executed
133 tests; 1 skipped; 0 failures; 0 errors

./gradlew test --rerun-tasks --console=plain
BUILD SUCCESSFUL in 54s; 8 actionable tasks executed
156 suites; 2253 tests; 5 skipped; 0 failures; 0 errors

./gradlew test check build assemble --dry-run --console=plain
BUILD SUCCESSFUL; BOUNDING_EVIDENCE_TASK=ABSENT_FROM_ALL_FOUR
```

These green results support, but do not substitute for, the source and
governance analysis above.

## 14. Verdict and exact next lawful action

```text
VERDICT=ACCEPTED
```

Every historical blocker is cured, ledger validation matches the governed
contract, manifest confinement precedes content access, and no new blocker
exists.

The exact next lawful action is an Independent Constitutional Review of this
accepted implementation and completion-review chain. No action here authorizes
Family F evidence production, model/provider access, or numeric-bound selection.

## 15. Discipline and STOP

This review created only this document. Implementation and every historical
review/ICR remain untouched. Nothing was staged, committed, or pushed. Parker,
Docker, Ollama/provider/model/network access, approval variables, and governed
evidence production were not used.

```text
STOP
```
