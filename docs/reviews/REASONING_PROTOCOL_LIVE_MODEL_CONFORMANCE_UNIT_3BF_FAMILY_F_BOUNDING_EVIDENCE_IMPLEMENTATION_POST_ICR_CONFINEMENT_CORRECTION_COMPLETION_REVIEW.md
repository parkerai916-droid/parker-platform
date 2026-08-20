# Family F Bounding Evidence Implementation Post-ICR Confinement Correction Completion Review

**Status:** Fresh independent Implementation Completion Review after the
ledger-metadata and filesystem-real-path correction — **REVISE BEFORE
ACCEPTANCE**.

## 1. Repository state and review boundary

```text
BRANCH=main
HEAD=a88e2e3d5f2022c340618783d5b6b9d97d7a21d6
ORIGIN_MAIN=a88e2e3d5f2022c340618783d5b6b9d97d7a21d6
```

The pre-review working tree matched the task's exact expected seven-item state.
Neither named Codex staging file nor any other Codex scratch file existed. This
review created only this document. It did not modify implementation, governance,
or any historical review/ICR.

## 2. Sources read fresh

Read fresh: Family F Model Role and Research Question Scope Lock and accepted ICR
chain; Alternative-Model Diagnostic Planning Review, accepted Model Role
Amendment and ICR; Experimental Reclassification Scope Lock, accepted Amendment
and ICR; Capture-Proxy Bounding Scope Lock, accepted Amendment and ICR; Bounding
Evidence Acquisition and Offline Estimator Plan; Plan Model Role Amendment and
ICR; Bounding Evidence Implementation Authorization Decision and ICR;
FamilyFRole Source Correction Amendment and ICR; and all four historical review
artifacts named by this task.

Read completely: `build.gradle.kts`,
`tests/integration/ReasoningProtocolFamilyFDiagnosticTest.kt`, and
`tests/integration/ReasoningProtocolFamilyFBoundingEvidenceTest.kt`. Referenced
schedule, corpus, prompt builder, request serializer, recovery, manifest,
terminal, source-scan, and Gradle graph paths were traced.

## 3. Authorized surface, model roles, and campaign

**Authorized surface: CONFORMING.** The Gradle change is the authorized detached
task; the Diagnostic change is the separately authorized two-line role
correction; the Bounding Evidence file remains within its authorized offline
surface. No production/runtime/configuration file is modified.

```text
DEPLOYED_BASELINE=qwen2.5-coder:7b
CONTROL_MODEL=qwen2.5-coder:7b
SUBJECT_MODEL=llama3.2:3b
```

`FamilyFRole` is correct and authoritative. Records directly consume
`trial.role.name` and `trial.role.modelName`; no second handwritten mapping
exists.

The accepted Plan and frozen source independently establish:

```text
FIXTURE_COUNT=23
ROLE_COUNT=2
CONTEXT_PROFILE_COUNT=2
REPETITIONS=4
SCORED_CALLS=23 x 2 profiles x 4 repetitions x 2 roles = 368
WARM_UP_STRUCTURE=4 repetitions x 2 roles x 3 warm-ups = 24
TOTAL_CALLS=392
```

The prior review's “2 profiles × 4 repetitions” wording is correct. Earlier
wording that labels the same arithmetic as four profiles and two repetitions is
semantically wrong.

## 4. Complete prior-defect closure

| # | Defect | Current implementation and verification | Finding |
|---|---|---|---|
| 1 | Real double-gated producer test | Four combinations exercise the real entry; true/true delegates once | **CURED** |
| 2 | WP-B/C/D validator substance | Category/provenance, target/kind, documentation/observation branches | **CURED within authorized offline layer** |
| 3 | Recovery/resume absent | Clean, partial, COMPLETE, FAILED, conflict, torn-state paths | **CURED** |
| 4 | Source exclusion broad | Only forbidden declaration excluded; adjacent tokens tested | **CURED** |
| 5 | FamilyFRole mismatch | Corrected authoritative constants directly consumed | **CURED** |
| 6 | Stale role documentation | Header states corrected mapping/amendment | **CURED** |
| 7 | Malformed JSONL accepted | Full-record regex, escapes, timestamp, newline and field checks | **CURED structurally** |
| 8 | Duplicate completion collapsed | Rejected before Set collapse | **CURED** |
| 9 | WP-B/C/D completed artifacts reconstructed | Exact index/empty-directory verification before skip | **CURED** |
| 10 | Unknown campaign paths | Recursive exact allowlist | **CURED** |
| 11 | Lexical manifest escape | Absolute, rooted, backslash, traversal, duplicate/member/hash checks | **CURED** |
| 12 | Fresh-copy leak | finally cleanup with primary/suppressed failure preservation | **CURED** |
| 13 | Step-specific ledger semantics | Exact status/detail table enforced | **NOT CURED lawfully: several exact prose details are not frozen by accepted governance** |
| 14 | Symlink/real-path escape | All campaign symlinks rejected; existing members real-path checked | **CURED for campaign members; manifest-path parameter gap remains** |
| 15 | Fresh non-empty root | Rejected without deletion | **CURED** |
| 16 | Resumed WP-A identity/repository input | Shape/timestamp and deterministic current-content verification | **CURED** |

## 5. Critical ledger semantic-contract finding

The accepted Plan Section 22 freezes six ordered step IDs, finalization order,
exact-once behavior, verified skip, and fail-closed conflict handling. Other Plan
sections govern work-package outcomes such as unresolved WP-B/C/D states. The
accepted Plan and Authorization Decision do **not** freeze an exact ledger
`status`/`detail` schema or these prose literals:

- `offline evidence production preflight`;
- `produced 392 records`;
- `no pre-supplied sources`;
- `validating artifacts and finalizing ledger`.

Those strings are deterministic outputs of the current writer, but an
implementation's current prose is not itself accepted governance. The correction
promotes them through `requiredMetadata` into supposedly governed exact
recovery requirements. The new tests duplicate the same table, proving
self-consistency rather than independent governance conformance.

The task expressly requires a blocking finding when a supposedly governed
literal was not actually frozen. Therefore the implementation cannot yet be
accepted. A lawful correction needs either accepted governance freezing the
ledger semantic schema, or validation expressed only in terms of already
governed semantic facts without inventing exact prose.

Within its invented table, the parser correctly rejects wrong status, arbitrary
and cross-step detail, missing/extra fields, invalid timestamp, unknown step,
invalid order, duplicate step, and impossible sequence before resume mutation.
It also enforces full JSONL syntax, valid escapes, a final newline, and no partial
successful parse.

## 6. Ledger state machine, root, and campaign surface

Append is append-only through finalization; post-finalization append throws.
Ordering is exact. Valid partial recovery is deterministic. COMPLETE, FAILED,
conflicting markers, WP-E without terminal, hash mismatch, missing/changed
completed artifacts, and ordinary unknown/stale paths reject without
normalization. Completed WP-A/B/C/D named artifacts are verified rather than
silently recreated.

Fresh production accepts only an absent or empty root and does not delete
pre-existing content. The campaign surface uses exact governed file/directory
sets and rejects unknown files, directories, nested paths, and stale temporary
artifacts.

## 7. Symlink policy and real-path confinement

Rejecting every symlink in the evidence surface is narrower than permitting
inside-root links, but is lawful and consistent with fail-closed governance.
`NOFOLLOW_LINKS` is used for existence/type checks. The root and every walked
member are checked for symbolic-link identity; existing regular members are
resolved with `toRealPath()` and compared using `Path.startsWith` against the
real root. Tests cover ordinary files/directories, outside file/directory links,
nested links, chains, broken links, unknown links, and a campaign-root link.
The normal production/copy path cannot follow such a link.

TOCTOU remains theoretically possible through concurrent hostile filesystem
mutation between check and read, but is not material to this single-process,
offline, test-owned architecture and is not classified as a defect here.

## 8. New manifest-path confinement defect

`verifyManifest(root, manifestPath)` validates the campaign surface and checks
only that the caller-supplied `manifestPath` is a regular non-symlink file.
It does not require:

- `manifestPath == normalizedRoot.resolve("SHA256SUMS.txt")`;
- lexical containment beneath `root`;
- `isRealPathConfined(root, manifestPath)`; or
- equality of its real path to the governed in-root manifest.

It then calls `Files.readAllLines(manifestPath, ...)`. Thus a caller can supply
an ordinary external/sibling manifest file; that external file is read before
confinement is established and can verify the campaign's internal members.
Production recovery currently constructs the correct in-root path, but the
manifest verifier itself is the fail-closed integrity boundary and accepts an
unconfined input. No test covers this route.

```text
MANIFEST_PATH_CONFINEMENT=BLOCKING
```

Manifest member lines themselves are lexically sound: absolute paths, Windows
drive/root forms, backslashes, `..`, normalized escapes, sibling paths,
duplicates, missing/unknown members, and malformed hashes reject. Member files
are checked with NOFOLLOW and real-path containment before hashing.

## 9. Copy verification

The normal copy path validates the source surface before creating the copy,
copies no accepted symlink, validates/hashes the copy through the same campaign
rules, and cleans the copy after success, false verification, or exception.
Cleanup failure is thrown when primary work succeeded and suppressed onto a
primary failure otherwise. Original campaign bytes remain untouched.

## 10. WP-A/B/C/D findings

| WP | Governance and implementation | Recovery/tests | Finding |
|---|---|---|---|
| A | Frozen schedule; production prompt/serializer; UTF-8, hashes, Base64, maxima; no selected bound | Identity, repository inputs, records and summary verified byte-exact | **CONFORMING except global ledger-contract blocker** |
| B | Offline primary-source category/provenance admissibility; no retrieval | Exact zero-source status/index and empty evidence directory verified | **CONFORMING within authorized layer** |
| C | Count/aggregate-byte semantics distinct | Exact unresolved index and empty directory verified | **CONFORMING within authorized layer** |
| D | Official documentation only; observation-derived evidence rejected | Exact unresolved index and empty directory verified | **CONFORMING within authorized layer** |

## 11. Gate, source scan, build, and external isolation

false/false, true/false, and false/true do not reach the producer; true/true with
the negative live-execution gate absent reaches the real offline producer exactly
once. Tests pass explicit values and do not set real approval variables.

The source-scan exclusion remains limited to the forbidden declaration; it does
not self-detect, catches adjacent forbidden text, and leaves implementation and
imports scanned.

The detached task has no lifecycle dependency/finalizer or configuration side
effect. Fresh dry runs show it absent from `test`, `check`, `build`, and
`assemble`.

Reachable Bounding Evidence paths are in-process and filesystem-only and stop at
the pure prompt builder/request serializer. No socket, HTTP/provider/Ollama/model
client, ProcessBuilder, Runtime.exec, Docker, Parker runtime, or external evidence
producer is reachable.

## 12. Numeric-bound and provenance safety

```text
NUMERIC_BOUND_SELECTED=NONE
```

The request maximum remains labeled only as a proposed evidence result, not an
accepted bound. No response, runtime, RAM, disk, header, E, or R bound is selected
or promoted.

No governed Family F evidence was produced. Attempts 1–2 and Unit 2/2-D were not
pooled, the lighthouse observation is absent, and no historical evidence was
relabelled or reclassified.

## 13. Fresh adversarial search

Blocking findings:

1. exact prose ledger details are enforced as governed despite not being frozen
   by accepted governance;
2. `verifyManifest` reads a caller-supplied external regular manifest path
   without first proving it is the exact confined campaign manifest.

No further material symlink bypass, timestamp normalization, duplicate-state,
member-path traversal, stale-artifact acceptance, recovery rewrite, exception
swallowing, source-scan bypass, graph leak, external execution, nondeterminism,
evidence manufacturing, or authority expansion was found.

## 14. Verification

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

test/check/build/assemble --dry-run
BOUNDING_EVIDENCE_TASK=ABSENT_FROM_ALL_FOUR
```

Passing tests do not prove the two uncovered integrity/governance paths.

## 15. Verdict and next lawful action

```text
VERDICT=REVISE BEFORE ACCEPTANCE
```

The exact next lawful action is a bounded governance/implementation
clarification:

1. obtain accepted governance for any exact ledger status/detail schema that is
   intended to be immutable, or revise validation to enforce only already
   governed semantic facts;
2. require the manifest argument to be the exact lexically and real-path
   confined `SHA256SUMS.txt` member before reading it; and
3. add negative offline tests for external, sibling, alternate, and symlinked
   manifest-path arguments.

After correction, run fresh offline verification and create a new Completion
Review and Independent Constitutional Review. No step authorizes evidence
production or bound selection.

## 16. Discipline and STOP

No implementation, governance, historical review, or ICR was edited. No Parker
runtime, Docker, Ollama/provider/model/network endpoint, governed evidence
production, approval variable, deployment, staging, commit, or push was used.

```text
STOP
```
