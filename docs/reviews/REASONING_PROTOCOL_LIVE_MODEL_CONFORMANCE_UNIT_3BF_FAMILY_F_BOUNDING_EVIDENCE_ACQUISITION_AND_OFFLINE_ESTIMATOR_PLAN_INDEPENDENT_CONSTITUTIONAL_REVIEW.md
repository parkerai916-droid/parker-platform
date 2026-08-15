**Status:** Independent Constitutional Review of the Unit 3-BF Family F Bounding Evidence Acquisition and Offline Estimator Plan — **VERDICT=ACCEPTED**. This is a fresh, independent review pass performed against the reviewed file's exact content at SHA-256 `0fec51f957a6a9b458a0d3c6d980cc6982e02ef316e09218d01da999c5beea0e`. This is the corrected version of the Plan: an earlier version (SHA-256 `4ad12ec62e2c2b2648b1e7d581060b32199f4aa339b9d4c37673568142f50eb2`) was independently reviewed and found `NOT ACCEPTED` for a genuine internal contradiction in its Section 22 recovery/manifest design. No claim in the corrected Plan, including any prior reviewer's conclusion or any author-supplied account of the correction, was treated as verified without independent re-derivation against primary sources. `P0=0, P1=0, P2=0, P3=0`. `READINESS=NOT READY` (unchanged).

# Independent Constitutional Review — Unit 3-BF Family F Bounding Evidence Acquisition and Offline Estimator Plan

## 1. Preconditions

```text
WORKING_DIRECTORY=/home/steve/parker-platform
BRANCH=governance/reasoning-protocol-family-f-bounding-evidence-plan
HEAD=54b4ef33d2f1c6da07a38997d4f01537bd8ba630
REVIEWED_FILE_STATUS=untracked (git status --porcelain: "?? docs/implementation/...BOUNDING_EVIDENCE_ACQUISITION_AND_OFFLINE_ESTIMATOR_PLAN.md")
REVIEWED_FILE_SHA256_BEFORE=0fec51f957a6a9b458a0d3c6d980cc6982e02ef316e09218d01da999c5beea0e
WORKING_TREE_OTHER_CHANGES=NONE -- exactly one untracked file present, no modified tracked file
```

All preconditions confirmed exactly as specified. No precondition failure.

## 2. Prior finding and correction record

An earlier pass in this same review lineage independently found the following defect in the Plan at SHA-256 `4ad12ec62e2c2b2648b1e7d581060b32199f4aa339b9d4c37673568142f50eb2`:

> Section 22's append-only `progress-ledger.jsonl` listed `COPY_VERIFICATION` and `TERMINAL` as ledger step IDs, but Section 21 required `SHA256SUMS.txt` (item 7) — covering every mandatory artifact including the ledger — to be written before copy-verification (items 8–9) and the terminal marker (item 10). A literal implementation could only append the ledger's `COPY_VERIFICATION`/`TERMINAL` records after `SHA256SUMS.txt` had already hashed the ledger, guaranteeing a hash mismatch on every run and permanent failure under Section 22's own halt rule.

This review independently re-derived, from the corrected text alone (not from any summary of what changed), that Section 22 now reads:

- the ledger step-ID list ends at `WP_E_VALIDATION` (six entries: `PREFLIGHT, WP_A_ESTIMATOR, WP_B_RESPONSE_EVIDENCE, WP_C_HEADER_EVIDENCE, WP_D_RUNTIME_EVIDENCE, WP_E_VALIDATION`) — `COPY_VERIFICATION` and `TERMINAL` no longer appear;
- "`WP_E_VALIDATION` is the final progress-ledger step. After its durable completion record is appended, the implementation must finalize `progress-ledger.jsonl` and prohibit every later append before writing `SHA256SUMS.txt`. The manifest therefore covers the complete, immutable progress ledger exactly like every other mandatory artifact." (lines 511)
- "Copy verification is a read-only, safely repeatable integrity operation over that finalized evidence set; it must not append to the progress ledger or modify any manifest-covered artifact. If interrupted before `evidence.complete`, it must be rerun in full from a fresh copy. Terminal state is represented only by the mutually exclusive `evidence.complete` or `evidence.failed` marker, not by a later ledger record." (line 513)
- "an append attempted after ledger finalization" is added as an explicit permanent-halt trigger (line 515).

Tracing the full ordering against Section 21's own explicitly sequential ten-item list ("It must: 1. ... 10."): items 1–6 (schema verification, rehash, digest verification, cross-check, reject-undisclosed, report-write) complete, `WP_E_VALIDATION` is appended and the ledger is finalized, then item 7 (`SHA256SUMS.txt`, now covering a static, already-frozen ledger) is written, then items 8–10 (copy, verify-copy, terminal marker) run as read-only/idempotent operations that never touch the ledger. This is a fully determined, non-circular pipeline. Three resume/interruption scenarios were independently traced (interruption before `WP_E_VALIDATION`; interruption between ledger finalization and the manifest write; interruption during copy-verification) and each resolves without contradiction using the corrected text alone — the explicit "rerun in full from a fresh copy" rule closes the third case.

**Correction independently confirmed genuine and complete. The prior defect no longer exists in the reviewed text.**

One candidate residual issue was considered and dismissed: the Plan never states in so many words which of Section 21's ten WP-E sub-items the `WP_E_VALIDATION` ledger checkpoint corresponds to. On independent analysis this is not an ambiguity: Section 21's list is explicitly sequential and numbered 1–10, item 7 is the `SHA256SUMS.txt` write, and Section 22 anchors ledger finalization to "before writing `SHA256SUMS.txt`" — i.e., before item 7. Combining the two leaves exactly one possible reading, with no second interpretation available and no risk of a non-conforming implementation. This does not meet the bar of a defect and is not counted as a finding.

## 3. Sources independently read

- `docs/implementation/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_BOUNDING_EVIDENCE_ACQUISITION_AND_OFFLINE_ESTIMATOR_PLAN.md` — the Plan, read in full, fresh, in this session, at its current SHA-256;
- `docs/architecture/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_CAPTURE_PROXY_RESPONSE_REQUEST_SIZE_AND_DEDICATED_RUNTIME_GROWTH_BOUNDING_SCOPE_LOCK.md` and its accepted Independent Constitutional Review, read in full, including its own governance sequence (Section 22) mapped section-by-section against the Plan's Section 30;
- `docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_RESPONSE_RUNTIME_AND_PARKER_HOST_ISOLATION_PLANNING_REVIEW.md` and its accepted Independent Constitutional Review (confirmed as the corrected, second-round version, `VERDICT=ACCEPTED`, `P0=P1=P2=P3=0`);
- `docs/architecture/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_ALTERNATIVE_DIAGNOSTIC_HOST_REQUIREMENTS_SCOPE_LOCK.md` and its accepted Independent Constitutional Review, especially Sections 9.2–9.5;
- `docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_PARKER_CANDIDATE_HOST_ASSESSMENT_REVIEW.md` and its accepted Independent Constitutional Review;
- `docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_DIAGNOSTIC_READINESS_REVIEW.md` and its accepted Independent Constitutional Review;
- `docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_DIAGNOSTIC_READINESS_BLOCKER_RESOLUTION_PLANNING_REVIEW.md` and its accepted Independent Constitutional Review;
- `docs/implementation/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_DIAGNOSTIC_IMPLEMENTATION_EXECUTION_PLAN.md` and its accepted Independent Constitutional Review;
- `docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_DIAGNOSTIC_IMPLEMENTATION_COMPLETION_REVIEW.md` and `docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_DIAGNOSTIC_IMPLEMENTATION_INDEPENDENT_CONSTITUTIONAL_REVIEW.md`;
- `docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_DIAGNOSTIC_RAW_TRANSPORT_CAPTURE_DEFECT_CONFIRMATION_REVIEW.md` and its accepted Independent Constitutional Review;
- `docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_DIAGNOSTIC_RAW_TRANSPORT_CAPTURE_CORRECTION_COMPLETION_REVIEW.md` and `docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_DIAGNOSTIC_RAW_TRANSPORT_CAPTURE_CORRECTION_INDEPENDENT_CONSTITUTIONAL_REVIEW.md`;
- `docs/architecture/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_EXPERIMENTAL_RECLASSIFICATION_AND_QUALIFICATION_BOUNDARY_SCOPE_LOCK.md` and its accepted Independent Constitutional Review;
- `build.gradle.kts`, read in full;
- `tests/integration/ReasoningProtocolFamilyFDiagnosticTest.kt`, `tests/integration/ReasoningProtocolFamilyFDiagnosticOrchestrationTest.kt`, `tests/integration/ReasoningProtocolLiveModelEvaluationHarness.kt`;
- `src/runtime/ModelInferenceClient.kt`, `src/runtime/ReasoningPromptBuilder.kt`, `src/runtime/ReasoningResponseParser.kt`;
- `docs/architecture/parker-constitution.md`;
- `git log`, confirming the branch's commit lineage (PR #29 planning review merge, PR #30 Bounding Scope Lock merge) up to `HEAD=54b4ef33...`.

Every document above was confirmed to exist at its cited path. Every ICR cited as "accepted" was independently opened and confirmed to state `VERDICT=ACCEPTED` (or equivalent) with `P0=P1=P2=P3=0`: the Bounding Scope Lock ICR, the Response/Runtime and Parker Host Isolation Planning Review ICR (second-round, corrected), the Parker Candidate Host Assessment ICR, the Alternative Diagnostic Host Requirements Scope Lock ICR, the Diagnostic Readiness Review ICR, the Readiness Blocker Resolution Planning Review ICR, the Family F Diagnostic Implementation/Execution Plan ICR, the Diagnostic Implementation Completion ICR, the Raw Transport Capture Defect Confirmation ICR, the Raw Transport Capture Correction Completion ICR, and the Experimental Reclassification and Qualification Boundary Scope Lock ICR.

## 4. Governance-sequence and frozen-invariant cross-check

Bounding Scope Lock Section 22's own governance sequence was independently mapped step-by-step against the Plan's Section 30 fifteen-step sequence. Every Scope Lock step is present, safely ordered, and no step is skipped, merged in a way that hides a gate, or implied to auto-authorize its successor. The Plan's step 12 ("bounded-transport/runtime implementation Completion Review and Independent Constitutional Review") correctly precedes step 13 ("renewed candidate-host assessment and Readiness Review") — the previously-open "missing review gate" finding from the prior review cycle remains correctly fixed and was independently re-confirmed present in this pass. The Plan's subdivision of the Scope Lock's single "separately authorized evidence production" step into a distinct Implementation Authorization Decision / implementation / implementation Completion Review sub-track (Plan steps 4–8) adds an additional review gate rather than omitting one.

The Plan's Section 4 frozen-input table (`SUBJECT_MODEL=qwen2.5-coder:7b`, `CONTROL_MODEL=llama3.2:3b`, `FIXTURE_COUNT=23`, `CONTEXT_PROFILE_COUNT=2`, `SCORED_REPETITIONS_PER_CELL_PER_ROLE=4`, `SCORED_CALLS=368`, `WARMUP_CALLS=24`, `TOTAL_CALLS=392`, and the three `PROHIBITED` lines) was independently checked against the Bounding Scope Lock's own Section 4 and matches exactly. The Plan's Section 2 statement of the five values the Scope Lock leaves unresolved (`MAX_REQUEST_BOUND`, `MAX_RESPONSE_BOUND`, `MAX_HEADER_COUNT`, `MAX_AGGREGATE_HEADER_BYTES`, `R`) was independently checked against the Scope Lock's own Section 5 status table and matches exactly, including the identical phrase "UNCOMPUTABLE ON CURRENT GOVERNED EVIDENCE" for `MAX_RESPONSE_BOUND`.

## 5. Source-code and build-file citation audit

Every source-code and build-file citation in the Plan was independently checked against the current working tree, not accepted on the strength of the citation existing:

- **WP-A schedule source** (Plan Section 12): `FamilyFCampaignDefinition.allTrials` independently confirmed at `ReasoningProtocolFamilyFDiagnosticOrchestrationTest.kt:83-137`, with in-code `check()` assertions enforcing the schedule's shape.
- **Offline-recovery construction** (Plan Section 12, step 2): `ReasoningProtocolFamilyFDiagnosticOrchestrationTest.kt:1282-1286` independently confirmed to read, in order: `val profileId = trial.profileId ?: ContextProfileId.MINIMAL_PRODUCTION_CONTEXT`; `SyntheticContextProfiles.construct(trial.fixture, profileId)`; `val turn = (input.request.subject as ReasoningSubject.OfTurn).turn` (line 1284, exact citation match); feeding `DefaultReasoningPromptBuilder().buildPrompt(turn, input.request.reasoningContext)` (line 1285) and `defaultOllamaRequestBody(prompt, modelName)` (line 1286). A full-document grep for `OwnerTurn` in the Plan returned zero matches — the previously nonexistent symbol is fully absent.
- **buildPrompt / defaultOllamaRequestBody**: `DefaultReasoningPromptBuilder.buildPrompt` independently confirmed as a real, public method at `ReasoningPromptBuilder.kt:48`; `defaultOllamaRequestBody` independently confirmed as a real, public top-level function at `ModelInferenceClient.kt:102`. Both are reachable from an external test source set without modification.
- **Schedule arithmetic** (Plan Sections 4, 12): `23 × 2 × 4 × 2 = 368` scored calls, `8 × 3 = 24` warm-up calls, `TOTAL=392`, independently confirmed not only by the Plan's prose but by executable `check()` assertions inside `FamilyFCampaignDefinition`'s own `init` block enforcing `368`, `24`, `392`, and `8` block counts exactly.
- **`build.gradle.kts` precedent** (Plan Sections 5–6): the `liveModelEvaluation` source set independently confirmed to exist, explicitly commented as deliberately detached from `test`/`check`/`build`/`assemble`. Four existing detached Gradle tasks independently confirmed to follow the exact `register<Test>` + `filter { includeTestsMatching(...) }` + `systemProperty(...)` pattern the Plan proposes to replicate for `reasoningProtocolFamilyFBoundingEvidence`. The existing live-diagnostic approval gate `PARKER_REASONING_FAMILY_F_EXECUTION_APPROVED` independently confirmed real, defined at `ReasoningProtocolFamilyFDiagnosticTest.kt:59` — genuine precedent for the Plan's proposed double-gate design.
- **File-list sanity**: `src/runtime/ReasoningPromptBuilder.kt`, `tests/integration/ReasoningProtocolLiveModelEvaluationHarness.kt`, and `src/runtime/ReasoningResponseParser.kt` all independently confirmed to exist at their expected paths. The Plan's silence on `ReasoningResponseParser.kt` independently confirmed as correct, not a gap: WP-A's construction chain terminates at request-body serialization and never touches response parsing, and WP-B derives response-bound evidence from external provider documentation, not from this file.

No fabricated, non-existent, or materially mismatched citation found anywhere in this set.

## 6. Internal boundary, gate, and work-package consistency

- **Governance authority and boundaries** (Sections 1–3, 32–33): no sentence anywhere in the Plan is self-authorizing. Section 3's eleven-item "not authorized by acceptance and merge" list is never contradicted by a later section; every reference to implementation, evidence production, provider/model contact, host provisioning, or Explicit Execution Approval is explicitly framed as requiring a distinct, separately named future authorization.
- **Two-file implementation boundary** (Section 5): independently confirmed not strained by Section 9's fourteen runtime-written evidence artifacts (data, not source), by Section 21's report generation (happens inside the single new test file), or by Section 11's hashing of `src/runtime/ReasoningPromptBuilder.kt` for drift detection (read-only provenance tracking, unambiguously distinct in function from the Section 5 no-modification rule).
- **Detached task and execution gates** (Section 6): independently traced as genuinely fail-closed. The evidence-producing entry test lives in the shared `liveModelEvaluation` source set (so the broad `reasoningProtocolLiveModelEvaluation` task discovers the class), but evidence production requires both `parker.reasoning.familyf.boundingEvidence.enabled` (settable only by the dedicated, non-default-graph task) and `PARKER_REASONING_FAMILY_F_BOUNDING_EVIDENCE_APPROVED` (settable only by a future Evidence Production Authorization Decision); the broad task sets neither, so the entry test self-skips exactly as claimed. The existing live-diagnostic approval variable is explicitly refused as authority if present, closing the one cross-triggering direction that could otherwise exist; the reverse direction does not arise because Section 5 forbids touching the live diagnostic's own files or gate logic at all.
- **Work-package consistency** (Sections 7, 15–20, 28): WP-B/C/D are consistently bound to ingestion/validation-only — never browsing, querying, downloading, or contacting anything — across the umbrella rule in Section 7, the admissible-source lists in Sections 15 and 18, Section 17's WP-C evidence-collection language (bound identically by Section 7's umbrella "WP-B through WP-D" framing despite not repeating the "ingest only" phrase verbatim — a non-issue, not an authority leak), Section 20's collection-boundary defaults, and Section 28's stop conditions.
- **Negative-evidence, failure, and stop-condition consistency** (Sections 19, 23, 28): mutually reinforcing, with Section 23 explicitly stating an honestly-recorded `UNRESOLVED` result is not a task failure, and Section 28's stop conditions matching Section 19's "never convert not-found into does-not-exist" rule without conflict.
- **Decision register** (Section 29): spot-checked rows on schedule derivation, WP-B contact, host selection/provisioning, and `NO_MANUFACTURED_PASS` preservation each independently confirmed to accurately restate their source sections without overstatement.

## 7. Constitutional and governance compliance

Independently read `docs/architecture/parker-constitution.md` in full and assessed the Plan's Section 32 self-characterization against the constitution's seven Constitutional Tests (owner control; authority/capability separation; trust-bypass resistance; auditability; revocability; Parker User Rights; bounded blast radius). The Plan's every-step-separately-authorized design, extensive mandatory provenance/ledger/manifest artifacts, fail-closed double-gating with explicit refusal of the unrelated live-diagnostic approval value, and offline-only, network/process-code-path-free implementation boundary (enforced by a required source-inspection guard) map cleanly onto all seven tests. Section 32's self-assessment is supported by the document's own text, not merely asserted by it.

## 8. Authority boundaries preserved

Independently confirmed the Plan:

- selects no numeric request, response, header, or `R` bound anywhere (Sections 2, 4, 13, 16, 17, 28);
- authorizes no compilation or execution of the future estimator, no evidence directory or artifact creation, no network/provider/model contact, no dedicated provider launch or `R` observation campaign, no host/VM provisioning, no Explicit Execution Approval, and no Knowledge Discoverability Attempt 3 (Section 3's explicit list, independently checked against every later section for contradiction — none found);
- changes no file — this is a documentation-only Plan; no test, source, or Gradle file is created or modified by its acceptance;
- preserves every Section 4 frozen invariant (subject/control identity, 23-fixture corpus, 2 profiles, 4 repetitions, the 392-call schedule, and the ranking/model-substitution/quantization-change prohibitions) unchanged;
- preserves `READINESS=NOT READY` throughout, unchanged from the accepted chain it builds on.

## 9. Finding counts

```text
P0=0
P1=0
P2=0
P3=0
```

No finding of any severity survived independent re-derivation against primary sources, across three independent verification passes covering: the corrected Section 22 ledger/manifest design and its ripple effects on Sections 8, 9, 21, 23–25 (including the residual documentation-precision question considered and dismissed in Section 2 above, which does not rise to a defect); the governance sequence against the Bounding Scope Lock and the full governance-chain ICR-status audit; every source-code and build-file citation; and the Plan's internal authority-boundary, gate, and work-package consistency together with constitutional conformance.

## 10. Final validation

```text
$ sha256sum docs/implementation/...BOUNDING_EVIDENCE_ACQUISITION_AND_OFFLINE_ESTIMATOR_PLAN.md   (before this review's own actions)
  0fec51f957a6a9b458a0d3c6d980cc6982e02ef316e09218d01da999c5beea0e
$ sha256sum docs/implementation/...BOUNDING_EVIDENCE_ACQUISITION_AND_OFFLINE_ESTIMATOR_PLAN.md   (after this review's own actions)
  0fec51f957a6a9b458a0d3c6d980cc6982e02ef316e09218d01da999c5beea0e  -- UNCHANGED, file not modified
$ git diff --check
  (no output -- clean, exit 0)
$ git status --porcelain --branch
  ## governance/reasoning-protocol-family-f-bounding-evidence-plan
  ?? docs/implementation/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_BOUNDING_EVIDENCE_ACQUISITION_AND_OFFLINE_ESTIMATOR_PLAN.md
  ?? docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_BOUNDING_EVIDENCE_ACQUISITION_AND_OFFLINE_ESTIMATOR_PLAN_INDEPENDENT_CONSTITUTIONAL_REVIEW.md
$ git rev-parse HEAD
  54b4ef33d2f1c6da07a38997d4f01537bd8ba630
```

```text
READINESS=NOT READY (unchanged)
VERDICT=ACCEPTED
FILES_CREATED=1 (this document)
EXISTING_FILES_MODIFIED=NONE (the Plan's SHA-256 is identical before and after)
STAGED_COMMITTED_PUSHED=NONE
PULL_REQUEST_OPENED=NONE
BRANCH_SWITCHED=NONE
EVIDENCE_PRODUCTION_OR_ESTIMATOR_EXECUTION=NONE
MODEL_OR_PROVIDER_CONTACT=NONE
HOST_VM_CONTAINER_PROCESS_OR_INFRASTRUCTURE_MANIPULATED=NONE
EXPLICIT_EXECUTION_APPROVAL_ISSUED=NO
KNOWLEDGE_DISCOVERABILITY_ATTEMPT_3=NOT STARTED, NOT AUTHORIZED
```

No prohibited action occurred.
