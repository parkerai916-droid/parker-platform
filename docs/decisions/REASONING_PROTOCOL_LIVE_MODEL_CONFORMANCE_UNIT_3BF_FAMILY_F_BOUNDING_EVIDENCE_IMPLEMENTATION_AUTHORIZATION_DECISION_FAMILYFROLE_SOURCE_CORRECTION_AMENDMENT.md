**Status:** Unit 3-BF Family F Bounding Evidence Implementation Authorization Decision — `FamilyFRole` Source Correction Amendment — **PROPOSED — PENDING INDEPENDENT CONSTITUTIONAL REVIEW.** This document amends one specific, named clause of the accepted, merged Unit 3-BF Family F Bounding Evidence Implementation Authorization Decision (`docs/decisions/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_BOUNDING_EVIDENCE_IMPLEMENTATION_AUTHORIZATION_DECISION.md`, commit `2c8842bd6f99e2dd1f9d125cd0c6a87e8facaabf`) in place of editing that Decision directly, preserving its original text as the historical record — following the identical precedent already established by the Unit 3-C Timeout and Durability Scope Lock Amendment and the four accepted Family F model-role amendments earlier in this same correction programme. It derives its entire authority from the already-accepted Family F Model Role and Research Question Scope Lock (`docs/architecture/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_FAMILY_F_MODEL_ROLE_AND_RESEARCH_QUESTION_SCOPE_LOCK.md`) and its accepted Correction Independent Constitutional Review. It selects no remedy, adopts no candidate, authorizes no live model call, no evidence production, no Explicit Execution Approval, and no production deployment, and it does not itself edit any source file, implement anything, or authorize any implementation task. It requires its own accepted Independent Constitutional Review before it becomes effective, and — separately, and only after that — a further, independent implementation-correction task before the frozen source it names may actually be edited.

# Bounding Evidence Implementation Authorization Decision — `FamilyFRole` Source Correction Amendment

## 1. Why this artifact exists

The independent Family F Bounding Evidence Implementation Completion Review (`docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_BOUNDING_EVIDENCE_IMPLEMENTATION_COMPLETION_REVIEW.md`, verdict `REVISE BEFORE ACCEPTANCE`) independently confirmed, from primary source, that the byte-frozen `FamilyFRole` enum in `tests/integration/ReasoningProtocolFamilyFDiagnosticTest.kt` still maps:

```text
SUBJECT -> qwen2.5-coder:7b
CONTROL -> llama3.2:3b
```

— the exact reverse of the role assignment the accepted Family F Model Role and Research Question Scope Lock freezes:

```text
DEPLOYED_BASELINE = qwen2.5-coder:7b
CONTROL_MODEL     = qwen2.5-coder:7b
SUBJECT_MODEL     = llama3.2:3b
```

That Completion Review found this "not mechanically honored" — a defect its own governing task classified as blocking. A subsequent bounded-correction task, executing this artifact's own Gate 1, independently re-confirmed:

1. the exact authoritative source is `tests/integration/ReasoningProtocolFamilyFDiagnosticTest.kt`, lines 62–63;
2. that file is expressly protected: the accepted Bounding Evidence Implementation Authorization Decision's own Section 5 states "`tests/integration/ReasoningProtocolFamilyFDiagnosticTest.kt` must remain byte-unchanged";
3. no accepted instrument in the four-instrument model-role correction chain (the Planning Review Amendment, the Experimental Reclassification Scope Lock Amendment, the Capture-Proxy Bounding Scope Lock Amendment, or the Bounding Evidence Acquisition Plan Amendment), nor the root Model Role and Research Question Scope Lock itself, nor either accepted ICR of the Bounding Evidence Decision, contains any reference to `FamilyFRole` or to this file — an independent `grep` across all five documents returned no match;
4. therefore correction of this source is **not currently authorized** by any accepted governance.

Per that bounded-correction task's own governing instructions, implementation correction stopped at that boundary, and this narrow governance artifact was prepared instead. No source file has been edited. No implementation correction has been performed.

## 2. Exact frozen source and exact values requiring correction

```text
FILE = tests/integration/ReasoningProtocolFamilyFDiagnosticTest.kt
LINES = 62-63
```

**Original text (preserved, not deleted; quoted verbatim from the current, still-unamended file):**

```kotlin
const val FAMILY_F_SUBJECT_MODEL_NAME = "qwen2.5-coder:7b"
const val FAMILY_F_CONTROL_MODEL_NAME = "llama3.2:3b"
```

**Frozen, amended — the only future-permitted correction, once this artifact and its own ICR are accepted and merged, and once a further, independently reviewed implementation-correction task actually performs the edit:**

```kotlin
const val FAMILY_F_SUBJECT_MODEL_NAME = "llama3.2:3b"
const val FAMILY_F_CONTROL_MODEL_NAME = "qwen2.5-coder:7b"
```

No other line of this file, and no other file, is authorized to change under this amendment.

## 3. Corrected values derived solely from the accepted Model Role and Research Question Scope Lock

The two corrected string literals above are not independently proposed by this amendment. They are copied verbatim from the already-accepted Scope Lock's own frozen Section B:

```text
DEPLOYED_BASELINE = qwen2.5-coder:7b
CONTROL_MODEL     = qwen2.5-coder:7b
SUBJECT_MODEL     = llama3.2:3b
```

`FAMILY_F_SUBJECT_MODEL_NAME` becomes `"llama3.2:3b"` because the Scope Lock's own Section B defines `SUBJECT_MODEL` as "the already-identified candidate under bounded, pre-qualification diagnostic evaluation," which the Scope Lock identifies as `llama3.2:3b`. `FAMILY_F_CONTROL_MODEL_NAME` becomes `"qwen2.5-coder:7b"` because the Scope Lock's own Section B defines `CONTROL_MODEL` as "the actual, continuously deployed reasoning-protocol model Family F's comparison is measured against," which the Scope Lock identifies as `qwen2.5-coder:7b`. This amendment introduces no new fact, no new evidence, and no independent role determination of its own.

## 4. Exhaustive confirmation: only these two lines require correction

Independently re-swept, for this amendment, every occurrence of the two physical model-name literals across the entire frozen Family F test surface:

```text
$ grep -n '"qwen2\.5-coder:7b"\|"llama3\.2:3b"' \
    tests/integration/ReasoningProtocolFamilyFDiagnosticTest.kt \
    tests/integration/ReasoningProtocolFamilyFDiagnosticOrchestrationTest.kt \
    tests/integration/ReasoningProtocolLiveModelEvaluationHarness.kt

tests/integration/ReasoningProtocolFamilyFDiagnosticTest.kt:62:const val FAMILY_F_SUBJECT_MODEL_NAME = "qwen2.5-coder:7b"
tests/integration/ReasoningProtocolFamilyFDiagnosticTest.kt:63:const val FAMILY_F_CONTROL_MODEL_NAME = "llama3.2:3b"
```

No other literal occurrence exists anywhere in the three frozen files. Every other reference to either physical model anywhere in this Family F test surface — the `FamilyFRole` enum's own constructor arguments (`SUBJECT(FAMILY_F_SUBJECT_MODEL_NAME)`, `CONTROL(FAMILY_F_CONTROL_MODEL_NAME)`), every call site that reads `.modelName` or the named constants symbolically, and the frozen `FamilyFCampaignDefinition`'s own role-symmetric arithmetic — resolves the physical model identity only *through* these two named constants, never by an independent literal. Correcting exactly these two lines is therefore both necessary and sufficient to correct the entire frozen source's model-role mapping; no third line, and no other file, requires any change.

## 5. What this is, and is explicitly not

This is a **factual role-identity correction** only: it corrects which of two already-frozen, already-governed physical model-name strings a pre-existing named constant equals, so that a pre-existing, unmodified `FamilyFRole` enum construction resolves to the identity the already-accepted Scope Lock separately established.

This is explicitly, and is not to be read as:

- model selection;
- remedy selection;
- qualification of either model;
- approval of either model for deployment;
- model acquisition, download, load, or replacement;
- a redesign of the frozen 392-call schedule, corpus, context profiles, or advancement gate;
- a change to the frozen campaign arithmetic (`23 × 2 × 4 × 2 = 368`; `+ 24 = 392`) — unaffected, because that arithmetic is role-symmetric and independent of which physical model each role name resolves to;
- a reclassification of any historical evidence (Knowledge Discoverability Attempts 1–2, Unit 2, Unit 2-D remain untouched and unreferenced by this amendment);
- authorization of implementation, evidence production, or execution of any kind.

## 6. Preserved, byte-for-byte, wherever possible

Everything in `tests/integration/ReasoningProtocolFamilyFDiagnosticTest.kt` other than the two named literal values at lines 62–63 is unaffected by this amendment, including but not limited to: the `FamilyFRole` enum's own declaration and constructor shape; every fixture in `FamilyFCorpus`; the warm-up fixture and input text; the two frozen `ContextProfileId` values and their order; every constant other than the two named above (`FAMILY_F_PROPERTY`, `FAMILY_F_EXECUTION_APPROVED_ENV`, `FAMILY_F_LIVE_TASK_INCOMPATIBLE_TAG`, `FAMILY_F_TIMEOUT_MS`, `FAMILY_F_CAMPAIGN_ID_MARKER`, `FAMILY_F_MINIMUM_FREE_MEMORY_BYTES`, `FAMILY_F_WARMUP_INPUT`); and every other Family F governance invariant this file participates in. `tests/integration/ReasoningProtocolFamilyFDiagnosticOrchestrationTest.kt` and `tests/integration/ReasoningProtocolLiveModelEvaluationHarness.kt` remain wholly untouched and are not named by this amendment at all. The Decision's Section 5 requirement that those two files, the production prompt builder, request serializer, provider client, parser, and all existing Family F gates "must remain byte-unchanged" is unamended and remains in full force.

## 7. Amendment to Decision Section 5

**Original text (preserved, not deleted):**

> - no file under `src/` may change;
> - `tests/integration/ReasoningProtocolFamilyFDiagnosticTest.kt` must remain byte-unchanged;
> - `tests/integration/ReasoningProtocolFamilyFDiagnosticOrchestrationTest.kt` must remain byte-unchanged;
> - `tests/integration/ReasoningProtocolLiveModelEvaluationHarness.kt` must remain byte-unchanged;
> - the production prompt builder, request serializer, provider client, parser, and all existing Family F gates must remain byte-unchanged; and
> - needing any third file is a stop condition requiring new governance.

**Frozen, amended — a single, narrow carve-out, nothing else in Section 5 is touched:**

> `tests/integration/ReasoningProtocolFamilyFDiagnosticTest.kt` must remain byte-unchanged **except for the exact two-line correction this amendment authorizes at lines 62–63 (Section 2 above)**, which may be made only by a further, separately reviewed implementation-correction task, after this amendment and its own Independent Constitutional Review are themselves accepted and merged. No other line of that file, and no other file named in Section 5's byte-unchanged list, is affected by this carve-out.

This amendment does not itself perform that edit. It does not authorize a third implementation file: the correction remains confined to the two files the Decision's own Section 5 already names (`build.gradle.kts`; `tests/integration/ReasoningProtocolFamilyFBoundingEvidenceTest.kt`), now joined — once this amendment is accepted — by the exact two-line carve-out in a third, previously-frozen file this amendment names precisely.

## 8. What may change downstream, if and only if this amendment is accepted

Only if this amendment and its own Independent Constitutional Review are both accepted and merged does the following become possible, and only as separate, independently reviewed, later tasks:

1. a bounded implementation-correction task may edit exactly `tests/integration/ReasoningProtocolFamilyFDiagnosticTest.kt` lines 62–63 as specified in Section 2 above;
2. that same or a further bounded implementation-correction task may then verify and, if necessary, correct `tests/integration/ReasoningProtocolFamilyFBoundingEvidenceTest.kt` so that its own estimator output (`role`/`modelName` pairing) is confirmed consistent with the corrected enum — since the estimator already consumes `FamilyFRole` by direct, unmodified pass-through (Completion Review, Section 6, finding 2), no separate "second handwritten mapping" is expected to be required there; the estimator should require no edit of its own, only re-verification;
3. the other three defects the Completion Review identified (the non-functional double-gated entry test; the WP-B/C/D stub validators; the missing recovery/resume logic; the overbroad source-inspection exclusion scope) remain independently correctable within the existing two-file authorized surface and are not gated by this amendment at all.

This amendment does not itself authorize step 1 or step 2 to be performed in the same task as its own acceptance. A fresh Independent Constitutional Review of this amendment must intervene first.

## 9. Prohibited interpretations

This amendment must not be read as: authorization for any live model, provider, network, or Ollama contact; authorization for evidence production of any kind; issuance or implication of Explicit Execution Approval; authorization for production deployment of any kind; authorization to edit any file other than the exact two lines named in Section 2, and only after this amendment's own acceptance and a further, separate implementation-correction task; a change to `READINESS=NOT READY`; a change to the pending Decision's own `IMPLEMENTATION_AUTHORIZED` status beyond the narrow Section 5 carve-out stated above; a resolution of the "substituted model name" question the Completion Review raised for any *output-layer* relabeling approach — this amendment resolves the question by authorizing correction of the *source of truth* itself instead, and takes no position on whether an output-layer relabeling would separately have been lawful; or a re-opening, reclassification, or reinterpretation of any historical evidence.

## 10. Status

```text
PROPOSED — PENDING INDEPENDENT CONSTITUTIONAL REVIEW
SOURCE_EDIT_PERFORMED = NO
IMPLEMENTATION_PERFORMED = NO
LIVE_MODEL_AUTHORITY = NONE
EVIDENCE_PRODUCTION_AUTHORIZED = NO
EXPLICIT_EXECUTION_APPROVAL_AUTHORIZED = NO
PRODUCTION_DEPLOYMENT_AUTHORIZED = NO
REMEDY_SELECTED = NO
CANDIDATE_ADOPTED = NO
NEXT_LAWFUL_ACTION = Independent Constitutional Review of this amendment
```

Not effective until that review is accepted and this amendment is merged. Until then, `tests/integration/ReasoningProtocolFamilyFDiagnosticTest.kt` remains byte-unchanged in full, exactly as the Decision's own unamended Section 5 already requires, and the Bounding Evidence Implementation Completion Review's `REVISE BEFORE ACCEPTANCE` verdict stands unaltered.

## STOP conditions confirmed

```text
NO source file edited by this amendment (verified: git status shows this
  file as the only new addition; tests/integration/ReasoningProtocolFamilyFDiagnosticTest.kt
  remains untouched).
NO model called, loaded, or contacted.
NO live diagnostic performed.
NO evidence production performed.
NO Explicit Execution Approval issued.
NO production code, test, Docker, persistence, QMD, UI, parser, or model
  configuration file touched.
NO implementation correction performed (Gate 2 not entered).
NO Independent Constitutional Review performed (this document's own, or
  any other).
NO document staged, committed, or pushed.
```
