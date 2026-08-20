# Family F Bounding Evidence Implementation Post-ICR Correction Completion Review

**Status:** Fresh independent Implementation Completion Review of the bounded
post-ICR recovery/evidence-integrity correction — **REVISE BEFORE ACCEPTANCE**.

## 1. Repository state and review boundary

```text
BRANCH=main
HEAD=a88e2e3d5f2022c340618783d5b6b9d97d7a21d6
ORIGIN_MAIN=a88e2e3d5f2022c340618783d5b6b9d97d7a21d6
```

Pre-review status was exactly:

```text
 M build.gradle.kts
 M tests/integration/ReasoningProtocolFamilyFDiagnosticTest.kt
?? docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_BOUNDING_EVIDENCE_IMPLEMENTATION_COMPLETION_REVIEW.md
?? docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_BOUNDING_EVIDENCE_IMPLEMENTATION_CORRECTION_COMPLETION_REVIEW.md
?? docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_BOUNDING_EVIDENCE_IMPLEMENTATION_CORRECTION_COMPLETION_REVIEW_INDEPENDENT_CONSTITUTIONAL_REVIEW.md
?? tests/integration/ReasoningProtocolFamilyFBoundingEvidenceTest.kt
```

Neither `.codex-bounding-staging.kt` nor `.codex-icr-staging.md` existed.
This review created only this document. It did not edit implementation,
governance, either historical Completion Review, or the existing ICR.

## 2. Governance read fresh

The review read the Family F Model Role and Research Question Scope Lock and
accepted ICR chain; Alternative-Model Diagnostic Planning Review, Model Role
Amendment and ICR; Experimental Reclassification Scope Lock, Amendment and ICR;
Capture-Proxy Bounding Scope Lock, Amendment and ICR; Bounding Evidence
Acquisition and Offline Estimator Plan and ICR; Plan Model Role Amendment and
ICR; Bounding Evidence Implementation Authorization Decision and ICR;
FamilyFRole Source Correction Amendment and ICR; historical Completion Review
#1; historical correction Completion Review #2; and the existing
`REVISE BEFORE ACCEPTANCE` ICR. The correction task's self-review was not
treated as implementation evidence.

## 3. Implementation read fresh

Read completely: `build.gradle.kts`,
`tests/integration/ReasoningProtocolFamilyFDiagnosticTest.kt`, and
`tests/integration/ReasoningProtocolFamilyFBoundingEvidenceTest.kt`.
Referenced schedule, corpus, context construction, prompt builder, request-body
serializer, ledger, manifest, recovery, terminal, source-scan, and Gradle task
paths were traced.

## 4. Authorized file surface

**CONFORMING.** The Gradle change is the authorized detached Test task. The
Diagnostic change is exactly the separately authorized two-line FamilyFRole
correction. The new Bounding Evidence file is the authorized offline
implementation. No production/runtime/configuration file is changed.

## 5. Model-role source of truth

```text
DEPLOYED_BASELINE=qwen2.5-coder:7b
CONTROL_MODEL=qwen2.5-coder:7b
SUBJECT_MODEL=llama3.2:3b
```

**CONFORMING.** `FamilyFRole` is authoritative and corrected. The estimator
uses `trial.role.name` and `trial.role.modelName` directly. No second
handwritten role mapping was found; schedule/output identities inherit the
authoritative values.

## 6. Prior-defect closure

| # | Original defect | Current implementation and test coverage | Independent finding |
|---|---|---|---|
| 1 | No real double-gated producer-entry test | Four combinations call the real entry path; true/true delegates once to the offline producer | **CURED** |
| 2 | WP-B/C/D validators were pass-through | Category/provenance, target/kind, and documentation/observation branches are tested | **CURED for the authorized offline admissibility layer** |
| 3 | No basic recovery/resume | Clean, partial, COMPLETE, FAILED, conflicting and torn-state paths exist | **CURED at basic level; strict metadata defect below blocks full conformance** |
| 4 | Source-inspection exclusion too broad | Only the declaration is excluded; adjacent-token tests exist | **CURED** |
| 5 | FamilyFRole mismatch | SUBJECT/CONTROL constants corrected and directly consumed | **CURED** |
| 6 | Stale role documentation | Header describes the corrected mapping and narrow amendment | **CURED** |
| 7 | Malformed ledger failed open | Full-line regex, newline, field, timestamp, enum, duplicate and order checks added | **NOT CURED fully: arbitrary nonblank status/detail metadata remains accepted** |
| 8 | Duplicate completion collapsed into Set | Duplicate is rejected before insertion; adjacent and separated cases covered | **CURED** |
| 9 | Completed WP-B/C/D artifacts reconstructed | Exact index and empty-directory checks precede execution | **CURED for the named package artifacts** |
| 10 | Unknown campaign files accepted | Recursive exact allowlist rejects unknown top-level/nested/temp paths | **CURED for ordinary paths; symlink bypass below remains** |
| 11 | Manifest paths unconstrained | Lexical normalization, exact members, duplicate and hash checks added | **NOT CURED fully: governed-name symlinks escape the root** |
| 12 | Fresh-copy leak | try/finally deletes after success, false result, and exception; cleanup exceptions are preserved/suppressed correctly | **CURED** |

## 7. Malformed ledger and duplicate completion

The reader rejects invalid syntax, truncation, unterminated JSONL, missing
fields, invalid escapes, invalid timestamps, unknown steps, blank status/detail,
duplicates, impossible order, blank records, and missing final newline before
mutation. It neither skips bad lines nor returns partial state.

**BLOCKING DEFECT:** accepted ICR Section 17 expressly requires
`invalid-status` records to halt. The reader checks only that status and detail
are nonblank. It never validates step-specific metadata. For example,
`PREFLIGHT` with status `BANANA`, or `WP_A_ESTIMATOR` with an unrelated
status/detail, is accepted as a completion and influences resume. The test named
`invalid-metadata` covers only an invalid timestamp, so the passing suite does
not exercise the required invalid-status case.

Duplicate handling itself is conforming: one completion is accepted; adjacent
and separated duplicates are rejected before Set collapse; a duplicate cannot
move or hide the resume position.

## 8. WP-A/B/C/D recovery

WP-A verifies identity shape/timestamp, current repository-input content,
records, and summary byte-for-byte; missing/changed state rejects. WP-B/C/D
verify their exact zero-source status/index and required empty evidence
directory. Missing, changed, malformed, non-directory, and non-empty states
reject before execution; completed named artifacts are not rewritten to make
recovery pass. Finding: **CONFORMING subject to the malformed-ledger defect.**

## 9. Fresh root and unknown-file surface

`produce` requires an absent or empty root and does not delete pre-existing
content. `produceOrResume` rejects ordinary unknown top-level, nested, and
stale temporary paths. Legitimate exact files/directories are admitted on the
case-sensitive Linux target.

**BLOCKING NEW DEFECT:** the allowlist does not reject symbolic links. A symlink
whose campaign-relative name equals a governed file is classified by name as an
allowed file. Java's default `Files.readAllBytes` then follows that link.
Therefore an outside/sibling file can enter manifest hashing and verification
through an allowed-name symlink. A symlink using a governed directory name also
creates ambiguous outside-root behavior because `Files.isDirectory` follows
links. This is a relevant Linux campaign-root escape, not a speculative platform
requirement.

## 10. Manifest confinement

Lexical controls reject empty names, backslashes, Windows drive-root forms,
absolute paths, `..` normalization/root escape, sibling paths, duplicates,
missing governed members, unknown members, and malformed lowercase SHA-256.
`Path.startsWith` is used rather than string-prefix comparison.

Finding: **NOT CONFORMING** because lexical confinement is not real-filesystem
confinement. Existing governed-name symlinks are followed during hashing and
verification, so a target outside normalized campaign root can pass when its
bytes match the manifest. Exact member-set checking does not cure this.

## 11. Fresh-copy cleanup

The copy is removed after verification success, verification rejection, and
thrown exception. The original is only read/copied. Cleanup failure is thrown
when primary verification succeeded and attached as suppressed when a primary
failure exists. **CONFORMING.**

## 12. Terminal and state integrity

Append-after-finalization through the same ledger object throws. COMPLETE and
FAILED writers reject the opposing marker. Conflicting terminal markers,
FAILED resume, WP-E-without-terminal, manifest mismatch, premature final
artifacts, and ordinary unknown paths reject. Resume order is deterministic.

Finding: **NOT CONFORMING overall.** Invalid step-status metadata is normalized
into a usable completed-step Set, and symlink paths can escape the governed
surface. These are fail-open routes at recovery/integrity boundaries.

## 13. Double gate

false/false, true/false, and false/true do not invoke the producer. true/true
with the negative live-execution gate absent invokes it exactly once. Presence
of the live-execution approval variable refuses execution. Offline temporary
tests pass explicit arguments and do not set real approval variables or
authorize external evidence production. **CONFORMING.**

## 14. WP-A/B/C/D substantive validators

| WP | Requirement | Implementation | Finding |
|---|---|---|---|
| A | Frozen schedule plus production formatter/serializer; hashes/bytes; no selected bound | Direct schedule iteration and production prompt/request serialization; 392 records; UTF-8/hash/Base64/max tests | **CONFORMING** |
| B | Validate pre-supplied primary provenance without retrieval or manufactured resolution | Admissible category plus required-field checks; remains unresolved pending worksheet | **CONFORMING within authorized layer** |
| C | Keep count and aggregate-byte semantics distinct | Target-to-limit-kind check rejects conflation | **CONFORMING within authorized layer** |
| D | Official documentation only; reject observed/benchmark evidence | Category and observation-origin checks; remains unresolved | **CONFORMING within authorized layer** |

No sophisticated pass-through that manufactures a resolved value was found.

## 15. Source inspection, build isolation, and reachability

The source exclusion is minimal, does not self-detect, catches immediately
adjacent forbidden tokens, and leaves imports/implementation scanned.

The Bounding Evidence task has no `dependsOn`/`finalizedBy` lifecycle
attachment. Dry runs for `test`, `check`, `build`, and `assemble` did
not include it.

Reachable producer/validator code performs in-process construction and
filesystem operations only. It reaches the pure production prompt builder and
request serializer, not the Diagnostic file's capture proxy. No socket, URL/HTTP
client, OkHttp/Ktor, provider/Ollama/model client, ProcessBuilder,
Runtime.exec, Docker, Parker runtime, or external evidence producer is reachable.

## 16. Campaign structure

Actual construction is:

```text
23 fixtures x 2 context profiles x 4 repetitions x 2 roles = 368 scored
4 repetitions x 2 roles x 3 warm-ups                       = 24 warm-up
TOTAL                                                       = 392
```

Thus the requested arithmetic's factors produce the same total, but its labels
`4 context profiles x 2 repetitions` are reversed relative to the actual
frozen schedule. IDs are schedule-derived, ordered, and unique. Corrected
role/model pairs flow directly into records.

## 17. Numeric-bound and provenance safety

```text
NUMERIC_BOUND_SELECTED=NONE
```

The request maximum is explicitly labeled a proposed evidence result and not an
accepted bound. No response/runtime/RAM/disk/header bound is selected,
recommended, or promoted.

No historical Family F evidence is imported or rewritten; Attempts 1-2 and Unit
2/2-D are not pooled; the lighthouse observation is absent. Verification used
