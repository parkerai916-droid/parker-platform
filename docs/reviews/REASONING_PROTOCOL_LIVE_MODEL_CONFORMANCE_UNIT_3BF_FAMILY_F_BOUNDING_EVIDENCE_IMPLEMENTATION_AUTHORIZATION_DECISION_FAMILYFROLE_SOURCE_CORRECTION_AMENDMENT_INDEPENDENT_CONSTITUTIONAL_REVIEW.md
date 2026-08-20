**Status:** Independent Constitutional Review of the `FamilyFRole` Source Correction Amendment — **ACCEPTED, WITH NON-BLOCKING QUALIFICATION.** Every material claim the amendment makes was independently re-derived from primary source rather than trusted: the frozen `FamilyFRole` mapping is confirmed genuinely reversed relative to the accepted Scope Lock; the frozen source is confirmed genuinely protected by the Decision's own Section 5 byte-unchanged requirement; no accepted governance instrument anywhere in the repository grants existing authority to edit it; the amendment's own claimed two-line correction is confirmed both necessary and sufficient, by an independent sweep broader than the amendment's own (repository-wide, not scoped to the three Family F files); the corrected values are confirmed to match the Scope Lock's own Section B exactly (`SUBJECT_MODEL=llama3.2:3b`, `CONTROL_MODEL=qwen2.5-coder:7b` — not the near-miss value the reviewing task's own prompt tested against); no fixture, schedule, corpus, context-profile, arithmetic, qualification, model-selection, evidence-production, live-call, numeric-bound, or historical-evidence authority is created; no duplicate model-role mapping or output-layer relabeling is authorized; and acceptance of this amendment alone does not itself authorize Gate 2 or the enum edit — a separate commit/merge, and then a separate implementation-correction task, are still required. One non-blocking qualification is recorded: the amendment's own Section 4 "exhaustive" sweep was scoped only to the three Family F test files, not the whole repository; this review's broader, repository-wide sweep independently confirms the amendment's conclusion is still correct, but the amendment's own text did not perform or disclose that wider check.

# `FamilyFRole` Source Correction Amendment — Independent Constitutional Review

## 1. Independent evidence reviewed

Read fresh for this task, not inherited from any prior task's summary:

```text
$ git rev-parse --abbrev-ref HEAD
main
$ git rev-parse HEAD
2c8842bd6f99e2dd1f9d125cd0c6a87e8facaabf
$ git status --short
 M build.gradle.kts
?? docs/decisions/..._FAMILYFROLE_SOURCE_CORRECTION_AMENDMENT.md
?? docs/reviews/..._BOUNDING_EVIDENCE_IMPLEMENTATION_COMPLETION_REVIEW.md
?? tests/integration/ReasoningProtocolFamilyFBoundingEvidenceTest.kt
```

- The target amendment, in full (167 lines).
- `docs/architecture/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_FAMILY_F_MODEL_ROLE_AND_RESEARCH_QUESTION_SCOPE_LOCK.md`, Section B, re-read fresh.
- The accepted Bounding Evidence Implementation Authorization Decision, Section 5, re-read fresh.
- The Family F Bounding Evidence Implementation Completion Review (`REVISE BEFORE ACCEPTANCE`), re-read for its own findings, not treated as authoritative for what the target amendment now claims.
- `tests/integration/ReasoningProtocolFamilyFDiagnosticTest.kt`, lines 55-80, re-read fresh from the actual working tree.
- An independent, repository-wide (not Family-F-scoped) `grep` for `FamilyFRole`, `FAMILY_F_SUBJECT_MODEL_NAME`, `FAMILY_F_CONTROL_MODEL_NAME`, and the two literal model-name strings — broader than the sweep the amendment's own Section 4 performed.

## 2. Finding A — is the mapping actually reversed?

```text
$ grep -n "FAMILY_F_SUBJECT_MODEL_NAME\|FAMILY_F_CONTROL_MODEL_NAME\|enum class FamilyFRole" \
    tests/integration/ReasoningProtocolFamilyFDiagnosticTest.kt
62:const val FAMILY_F_SUBJECT_MODEL_NAME = "qwen2.5-coder:7b"
63:const val FAMILY_F_CONTROL_MODEL_NAME = "llama3.2:3b"
74:enum class FamilyFRole(val modelName: String) {
75:    SUBJECT(FAMILY_F_SUBJECT_MODEL_NAME),
76:    CONTROL(FAMILY_F_CONTROL_MODEL_NAME),
```

**Confirmed independently, from the live working tree, not from the amendment's own restatement.** `SUBJECT -> qwen2.5-coder:7b`, `CONTROL -> llama3.2:3b`.

## 3. Finding — exact accepted mapping, independently re-derived

```text
$ sed -n '/## B\. Corrected model roles/,/## C\./p' \
    docs/architecture/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_FAMILY_F_MODEL_ROLE_AND_RESEARCH_QUESTION_SCOPE_LOCK.md
DEPLOYED_BASELINE = qwen2.5-coder:7b
CONTROL_MODEL     = qwen2.5-coder:7b
SUBJECT_MODEL     = llama3.2:3b
```

**Confirmed, exact strings, independently re-read from the controlling Scope Lock rather than assumed from the reviewing task's own prompt text.** The reviewing task's prompt itself tested whether the amendment might contain the near-miss value `llama3.2-coder:7b`; the amendment's own Section 2 and Section 3 both correctly use `llama3.2:3b`, matching this independently-confirmed value exactly — no discrepancy found.

## 4. Finding B — is the source genuinely byte-freeze protected?

```text
$ sed -n '/## 5\. Exact implementation boundary/,/## 6\./p' \
    docs/decisions/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_BOUNDING_EVIDENCE_IMPLEMENTATION_AUTHORIZATION_DECISION.md
...
- `tests/integration/ReasoningProtocolFamilyFDiagnosticTest.kt` must remain byte-unchanged;
...
```

**Confirmed.** This is the accepted, merged Decision's own text (commit `2c8842b`), independently re-read, not trusted from the amendment's quotation of it.

## 5. Finding C — does any existing accepted instrument already authorize the correction?

```text
$ grep -rln "FamilyFRole" docs/architecture/ docs/decisions/ docs/implementation/ docs/reviews/ 2>/dev/null
docs/decisions/..._FAMILYFROLE_SOURCE_CORRECTION_AMENDMENT.md
docs/reviews/..._BOUNDING_EVIDENCE_IMPLEMENTATION_COMPLETION_REVIEW.md
docs/reviews/..._UNIT_3BF_FAMILY_F_DIAGNOSTIC_IMPLEMENTATION_COMPLETION_REVIEW.md
docs/reviews/..._UNIT_3BF_FAMILY_F_DIAGNOSTIC_RAW_TRANSPORT_CAPTURE_CORRECTION_COMPLETION_REVIEW.md
```

Independently attributed: the target amendment itself (self-match, expected); the Bounding Evidence Completion Review (identifies the defect, grants no authority); the Family F Diagnostic *Implementation* Completion Review and its raw-transport-capture correction review (both review the *original, unrelated* Family F diagnostic implementation task that first wrote this enum — neither is part of the model-role correction chain, and neither purports to grant any later authority to re-edit it). **No hit among the root Scope Lock, any of the four accepted model-role amendments, either accepted ICR of the Bounding Evidence Decision, or the Decision itself.**

**Confirmed: no existing accepted governance instrument authorizes correction of this source.** Gate 1's finding, and this amendment's own premise, is independently verified accurate.

## 6. Finding D/E — is a new amendment actually necessary, and is this the narrowest lawful mechanism?

Given Findings A-C, a new governance act is necessary: the source is reversed, is genuinely protected, and no existing authority reaches it. Independently assessed whether the amendment's chosen *mechanism* (an `*_AMENDMENT` targeting the already-merged Decision's Section 5) is correct, rather than, e.g., a fresh, freestanding Scope Lock: the established, repeatedly-used precedent throughout this entire correction programme (the Unit 3-C Timeout and Durability Scope Lock Amendment; all four accepted Family F model-role amendments) is that a correction to an **already-accepted, merged** instrument uses the amendment pattern — preserving original text, adding a narrow "frozen, amended" carve-out — never a fresh, freestanding document. The target Decision is confirmed accepted and merged (`2c8842b`, independently re-verified via `git log`/`git status` in the prior task and re-confirmed here via `git rev-parse HEAD`). The amendment pattern is therefore the correct mechanism, and this document reproduces its established shape (Status line, "original text preserved / frozen, amended" pairs, prohibited interpretations, Status block) exactly.

**Confirmed: a new amendment is necessary, and this is the narrowest lawful mechanism** — it touches only Decision Section 5's byte-unchanged clause, and only for the two named lines, not the Decision's other content.

## 7. Finding — authorized correction surface (Question F)

```text
Amendment Section 2:
  const val FAMILY_F_SUBJECT_MODEL_NAME = "llama3.2:3b"
  const val FAMILY_F_CONTROL_MODEL_NAME = "qwen2.5-coder:7b"
```

Independently cross-checked against Finding 3 above: `SUBJECT_MODEL = llama3.2:3b` and `CONTROL_MODEL = qwen2.5-coder:7b` — **exact match, correct in both direction and exact string content.** No near-miss value (`llama3.2-coder:7b` or any other variant) appears anywhere in the amendment.

## 8. Finding G — are every other frozen campaign invariant protected?

Independently re-read the amendment's Section 6 claim list against the actual frozen file (`tests/integration/ReasoningProtocolFamilyFDiagnosticTest.kt`, lines 55-80 and the `FamilyFCorpus` object beginning at line 90, re-read fresh): the `FamilyFRole` enum's own declaration and constructor shape, every fixture, the warm-up fixture, the two context profiles and their order, and every other named constant are untouched by the amendment's Section 2 diff (exactly two lines, both already-existing constant *value* assignments, not new declarations). The amendment's own Section 7 carve-out is textually scoped to "the exact two-line correction... at lines 62-63" only. **Confirmed: every other frozen invariant remains fully protected**, including the two files (`...OrchestrationTest.kt`, `...Harness.kt`) and the production formatter/serializer the Decision's Section 5 also names, none of which this amendment touches or discusses changing.

## 9. Finding — duplicate source-of-truth / output-layer relabeling (Questions H, I)

Independently re-read Section 5 (explicit disclaimer list) and Section 9 (prohibited interpretations) against the H checklist: fixture/schedule/corpus/context-profile/arithmetic changes, qualification, model selection, remedy selection, model acquisition/load/unload/replacement/requantization, evidence production, live model/provider/network/Ollama contact, Explicit Execution Approval, and production deployment are all explicitly and individually disclaimed, consistent with the actual two-line diff scope (Section 7 above). "Modification of unrelated Parker systems" is not explicitly listed but is trivially excluded by the same narrow file/line scope.

**No second, handwritten model-role mapping is created.** The amendment corrects the *existing* two named constants' values; it does not introduce a third, parallel constant or table. Section 9 explicitly states the amendment "resolves the question by authorizing correction of the *source of truth* itself instead" of any output-layer relabeling, and explicitly declines to authorize the latter. **Confirmed sound on both H and I.**

## 10. Finding — estimator consumption (Question J)

Independently re-read `FamilyFBoundingEvidenceRequestEstimator.estimate()` in `tests/integration/ReasoningProtocolFamilyFBoundingEvidenceTest.kt` (lines 175, 188, re-read fresh for this review): `val modelName = trial.role.modelName` and `role = trial.role.name` — both a direct, unmediated read of the frozen enum, with no intermediate mapping, cache, or translation layer of the estimator's own. **Confirmed: correcting the two named constants at their source is sufficient for the estimator to consume the corrected roles naturally, with no second role translation and no edit to the estimator file required** — independently verified, not merely accepted from the amendment's Section 8 item 2 claim.

## 11. Finding — additional-source sweep (Question K)

The amendment's own Section 4 sweep was scoped to exactly three files (the Family F test surface). This review independently ran a **repository-wide** sweep, not scoped to Family F:

```text
$ grep -rln '"qwen2\.5-coder:7b"\|"llama3\.2:3b"' --include="*.kt" .
tests/composition/InteractiveConsoleTest.kt
tests/integration/ReasoningProtocolBaselineCharacterisationTest.kt
tests/integration/ReasoningProtocolUnit3CControlledRemedyExperimentsTest.kt
tests/integration/ReasoningProtocolDiagnosticCharacterisationTest.kt
tests/integration/ReasoningProtocolFamilyFDiagnosticTest.kt
tests/integration/ReasoningProtocolUnit3COrchestrationTest.kt
```

Five files beyond the amendment's own claimed scope. Each individually inspected for `SUBJECT_MODEL`/`CONTROL_MODEL`/role-pairing semantics:

- `InteractiveConsoleTest.kt` — asserts an interactive banner literally prints the currently-configured model name; no dual-role framework.
- `ReasoningProtocolBaselineCharacterisationTest.kt` (Unit 2) — uses `qwen2.5-coder:7b` as the single baseline-characterization model under test; no `SUBJECT`/`CONTROL` pairing.
- `ReasoningProtocolUnit3CControlledRemedyExperimentsTest.kt` (Unit 3-C) — a single `UNIT_3C_MODEL_NAME` constant and its own, separately-governed `Unit3CIdentity`; no Family F role framework.
- `ReasoningProtocolDiagnosticCharacterisationTest.kt` (Unit 2-D) — two independent constants (`DIAGNOSTIC_QWEN_MODEL_NAME`, `DIAGNOSTIC_LLAMA_MODEL_NAME`) used for its own, separately-governed diagnostic characterization; no `SUBJECT_MODEL`/`CONTROL_MODEL` terminology anywhere.
- `ReasoningProtocolFamilyFDiagnosticTest.kt` — already covered (Findings A-B).

**No additional frozen source requires correction for semantic consistency with the corrected Family F governance.** None of these other five programme units participates in Family F's `SUBJECT_MODEL`/`CONTROL_MODEL` role framework; each uses the two physical model-name strings independently, for its own, separately-governed purpose. The amendment's *conclusion* (Section 4: only two lines in one file) is confirmed correct by this broader, independent sweep — but the amendment's own text did not perform or disclose a repository-wide check, only a Family-F-scoped one. **This is the one non-blocking qualification this review records.**

## 12. Finding — historical evidence (Question L)

No Family F campaign has ever executed (`READINESS=NOT READY` throughout; `BOUNDING_EVIDENCE_PRODUCTION_STATUS=NOT AUTHORIZED`; independently re-confirmed no evidence root or campaign artifact exists anywhere in the repository or working tree). There is therefore no already-captured Family F evidence for a prospective enum correction to retroactively affect. The historical sources the amendment's Section 5 names as untouched (Knowledge Discoverability Attempts 1-2, Unit 2, Unit 2-D) are structurally separate captures against different, independently-governed test harnesses (confirmed by Finding 11 above — those units use their own, unrelated constants). **Confirmed: correcting the enum prospectively cannot rewrite, pool, relabel, or retrospectively reinterpret any historical evidence, because none exists from Family F to reinterpret, and the named historical sources are structurally unrelated.**

## 13. Finding — implementation-authority consequence (Questions M, N)

Independently re-read Section 8 and Section 10 (Status block): acceptance of this amendment's ICR alone is explicitly stated to be insufficient — the amendment must *also* be committed and merged (a separate act, not performed by an ICR) before **any** implementation step becomes lawful. Even after that merge, only two things become possible, each as its own separate task: (1) the exact two-line edit at lines 62-63; (2) re-verification (not expected to require an edit, per Finding 10) of the Bounding Evidence estimator. The three other Completion Review defects (non-functional entry test; WP-B/C/D stubs; missing recovery/resume) are independently confirmed, by this review, to be **unrelated to `FamilyFRole`** and were never gated by the absence of this amendment — they remain correctable at any time within the already-authorized two-file surface, with or without this amendment.

**Confirmed: acceptance of this ICR does not itself authorize Gate 2 or the enum edit.** A separate governance-acceptance commit/push task, and then a separate implementation-correction task, are both still required before any source is touched.

## 14. Constitutional risk sweep

| Risk | Finding |
|---|---|
| Governance laundering | Not present — scope is precisely as narrow as claimed, independently verified (Findings 7-9). |
| Authority expansion | Not present — Section 5/9 disclaimers independently confirmed consistent with the actual 2-line diff. |
| Disguised model selection | Not present — values copied verbatim from already-accepted Scope Lock (Finding 3), not independently chosen. |
| Duplicated source of truth | Not present (Finding 9). |
| Historical evidence relabelling | Not present (Finding 12). |
| Corpus/schedule drift | Not present — role-symmetric arithmetic unaffected; corpus/profiles untouched (Finding 8). |
| Qualification leakage | Not present — nothing here touches Unit 3-A qualification tier or the Reclassification Scope Lock's advancement gate. |
| Evidence-production leakage | Not present — `EVIDENCE_PRODUCTION_AUTHORIZED=NO`, independently consistent with the amendment's own text throughout. |
| Live-call/network leakage | Not present. |
| Numeric-bound selection | Not present — no numeric bound is discussed anywhere in this amendment. |
| Production coupling | Not present — only a test file, not `src/`, is ever named, and only for a future, separate task. |
| Implementation-before-acceptance | Not present — independently confirmed via `git diff --stat` that the frozen source is untouched by this amendment's own drafting. |
| Amendment mechanism misuse | Not present (Finding 6). |
| Downstream dependency ambiguity | Present in a narrow, disclosed, self-consistent form only — the amendment explicitly declines to resolve whether an *alternative* (output-layer) mechanism would separately have been lawful, but this does not create ambiguity in what the amendment *itself* authorizes, which is unambiguous. Non-blocking. |

## 15. Verification performed for this review

```text
$ git status --short
 M build.gradle.kts
?? docs/decisions/..._FAMILYFROLE_SOURCE_CORRECTION_AMENDMENT.md
?? docs/reviews/..._BOUNDING_EVIDENCE_IMPLEMENTATION_COMPLETION_REVIEW.md
?? tests/integration/ReasoningProtocolFamilyFBoundingEvidenceTest.kt

$ git diff --stat
 build.gradle.kts | 13 +++++++++++++
 1 file changed, 13 insertions(+)

$ git diff --stat -- tests/integration/ReasoningProtocolFamilyFDiagnosticTest.kt
(no output -- frozen source confirmed untouched)
```

(This review's own new file will appear as a fourth untracked entry once written.) No model process, campaign, experiment, or implementation action occurred before or during this review. No Gradle build was run. No Ollama server was contacted. No Parker server runtime was touched. The target amendment was not edited by this review. The frozen `FamilyFRole` source was not edited by this review. No other file was edited by this review.

## 16. Verdict

```text
ACCEPTED, WITH NON-BLOCKING QUALIFICATION
```

The qualification: the amendment's own Section 4 "exhaustive" sweep claim should, in a future revision of this document (should one ever be needed) or in the next instrument that cites it, be understood as scoped to the three Family F test files, not the whole repository — this review's own broader sweep (Finding 11) is what actually establishes repository-wide completeness. This does not change the amendment's substance, correction scope, or authorized values, and requires no revision to the amendment before it may be accepted.

## 17. Exact next lawful action

```text
NEXT_LAWFUL_ACTION = Commit this ICR together with the target amendment into
  repository history via the established governance-acceptance commit
  pattern, and merge to main -- not performed by this review. Only after
  that commit and merge does a separate, later implementation-correction
  task become lawful to (a) edit tests/integration/ReasoningProtocolFamilyFDiagnosticTest.kt
  lines 62-63 exactly as Section 2 of the amendment specifies, and (b)
  re-verify (not expected to require editing) the Bounding Evidence
  estimator's own output. The three defects unrelated to FamilyFRole
  (non-functional entry test; WP-B/C/D stubs; missing recovery/resume)
  remain separately correctable at any time and are not gated by this
  amendment.
```

Not performed by this review.

## 18. git status --short

```text
 M build.gradle.kts
?? docs/decisions/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_BOUNDING_EVIDENCE_IMPLEMENTATION_AUTHORIZATION_DECISION_FAMILYFROLE_SOURCE_CORRECTION_AMENDMENT.md
?? docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_BOUNDING_EVIDENCE_IMPLEMENTATION_COMPLETION_REVIEW.md
?? tests/integration/ReasoningProtocolFamilyFBoundingEvidenceTest.kt
```

## 19. git diff --stat

```text
 build.gradle.kts | 13 +++++++++++++
 1 file changed, 13 insertions(+)
```

## 20. Confirmation: target amendment untouched

Independently confirmed: `docs/decisions/..._FAMILYFROLE_SOURCE_CORRECTION_AMENDMENT.md` was not edited by this review — it remains untracked with the same content read at the start of this task (Section 1 above).

## 21. Confirmation: implementation files untouched

Independently confirmed: `build.gradle.kts`'s diff is byte-identical to its state before this review began; `tests/integration/ReasoningProtocolFamilyFBoundingEvidenceTest.kt` remains untracked and unedited; `tests/integration/ReasoningProtocolFamilyFDiagnosticTest.kt` (the frozen `FamilyFRole` source) shows no diff whatsoever. No source correction, no implementation correction, and no Gate 2 activity occurred during this review.

## STOP conditions confirmed

```text
NO source correction performed.
NO implementation correction performed.
NO model called, loaded, or contacted.
NO Ollama, provider, or network endpoint contacted.
NO evidence production performed.
NO Explicit Execution Approval issued.
NO numeric bound selected.
NO production deployment performed.
NO document staged, committed, or pushed.
Only read-only commands executed: git status/diff/rev-parse/log, grep, sed.
```
