# Programme 4 — Unit 7 (Acceptance Coordination) — Planning Review

## Status

**Governance sufficiency review only. Planning and constitutional verification, not implementation.** No Kotlin is implemented, proposed, or changed by this document. No production source file is modified. No existing governance document is modified. Neither `src/` nor `tests/` is touched beyond read-only inspection and running the existing, already-committed test suite for verification purposes. Nothing is staged, committed, or pushed.

**Central finding, stated at the outset rather than buried: this review's own premise requires correction before it can be usefully answered. Programme 4 Unit 7 is not merely unblocked and ready to begin — it is already implemented, tested, reviewed through its own governance-first workflow, composed into the running system, and passing today, in the repository exactly as it stands at this review's own baseline.** The remainder of this document proves that conclusion from primary evidence, point by point, exactly as the governing task requires, rather than asserting it.

---

## Repository Baseline

- **HEAD:** `93ace44f2672fd9890e6cbec68b9b94bbc203ad2`
- **Branch:** `main`
- **Working tree:** clean at the start of this review.
- Confirmed present and accepted, per the governing task's own stated baseline: Programme 3 Unit 8 (Knowledge Submission) and Programme 3 Unit 9 (Knowledge Retrieval, sub-units 9.1 through 9.6) — both independently re-confirmed present in this repository during this review, not merely taken on the task's own word (Section 3, below).

---

## Documents and Code Read for This Review

**Governance, read in full or in the relevant sections:** `docs/architecture/parker-constitution.md` (cumulative context from this session's own prior work, re-applied here); `docs/architecture/10-permission-engine.md` ("Chapter 10"); `docs/decisions/CDR-005_CONSTITUTIONAL_ADMISSION_OF_PERMISSIONENGINE_PROPOSAL_CLASSES.md`; `docs/decisions/CDR-006` (via CDR-007's own citation and Repository Reuse Summary); `docs/decisions/CDR-007_CONSTITUTIONAL_CLASSIFICATION_OF_EVIDENCE_INTELLIGENCE.md` (in full); `docs/architecture/EVIDENCE_INTELLIGENCE_CONTRACT_DESIGN.md`, `docs/architecture/EVIDENCE_INTELLIGENCE_SCOPE_LOCK.md` (via direct citation in the documents below, and repository search); `docs/implementation/EVIDENCE_INTELLIGENCE_IMPLEMENTATION_PLAN.md` (§7, §8 Unit 7/Unit 8 entries, §14 in full); `docs/reviews/EVIDENCE_INTELLIGENCE_UNIT_7_PLANNING_AND_BOUNDARY_REVIEW.md` (in full); `docs/reviews/PROGRAMME_4_UNIT_7_DEPENDENCY_RECORD.md` (in full); Programme 3's own adopted Unit 8 and Unit 9 governance chain (`PROGRAMME_3_UNIT_8_SCOPE_LOCK_CLARIFICATION.md`, `PROGRAMME_3_UNIT_9_KNOWLEDGE_RETRIEVAL_CONTRACT_DESIGN.md`, `PROGRAMME_3_UNIT_9_SCOPE_LOCK_CLARIFICATION.md`, `PROGRAMME_3_UNIT_9_KNOWLEDGE_RETRIEVAL_IMPLEMENTATION_PLAN.md`, `PROGRAMME_3_UNIT_9_PERMISSION_ENFORCEMENT_MECHANISM_CLARIFICATION.md`) — all already read in full during this session's own immediately preceding work and re-applied here, not re-read from scratch.

**Source, read in full:** `src/runtime/EvidenceIntelligenceAcceptanceCoordinator.kt`; `src/composition/ParkerRuntime.kt` (the Evidence Intelligence and Knowledge Memory composition sections, grepped and read directly); `src/runtime/DefaultKnowledgeSubmission.kt`; `src/runtime/DefaultKnowledgeRetrieval.kt` (to confirm, directly, that Unit 7 has no dependency on it — Section 5, below).

**Verification performed, not merely asserted:** `git log` traced against the actual commit graph to establish the true chronological sequence of Programme 3 Unit 8, Programme 4 Unit 7, and Programme 4 Unit 8; `./gradlew clean test` run against the current repository state.

---

## 1. Why "Programme 4 Unit 7" Means What This Review Treats It As Meaning

This repository does not use a `PROGRAMME_4_...`-prefixed governance-document naming convention the way Programme 3 does. A repository-wide search for `PROGRAMME_4_*` governance documents returns exactly one file: `docs/reviews/PROGRAMME_4_UNIT_7_DEPENDENCY_RECORD.md`. Programme 4's own substantive governance is filed under its domain name, **Evidence Intelligence** — `EVIDENCE_INTELLIGENCE_CONTRACT_DESIGN.md`, `EVIDENCE_INTELLIGENCE_SCOPE_LOCK.md`, `EVIDENCE_INTELLIGENCE_IMPLEMENTATION_PLAN.md` — and that Implementation Plan's own §8 names "Unit 7 — The Evidence Intelligence Acceptance Coordinator" as the unit whose responsibility is exactly "Acceptance Coordination." The `PROGRAMME_4_UNIT_7_DEPENDENCY_RECORD.md` document itself titles its own subject "Programme 4 (Evidence Intelligence) — Unit 7," confirming the identification directly. This review therefore treats "Programme 4 Unit 7: Acceptance Coordination," as named by the governing task, and "Evidence Intelligence Implementation Unit 7 (the Evidence Intelligence Acceptance Coordinator)," as named throughout the repository's own governance and source, as the same unit — not by inference, but because the repository's own documents use both descriptions for the identical subject.

---

## 2. What Blocked Unit 7, and When

`docs/reviews/EVIDENCE_INTELLIGENCE_UNIT_7_PLANNING_AND_BOUNDARY_REVIEW.md` (repository baseline at the time: `HEAD 7a62fac`) performed the original planning-and-boundary analysis this task's own instructions describe, and reached verdict **"B — Governance return required"**: Unit 7's own three acceptance legs required `EvidenceCustodian.accept` (existed, self-gating), `MemoryCore`'s write interface (existed, requires the coordinator's own `PermissionEngine`, precedented by `EvidenceRegistrationCoordinator`), and "Knowledge Memory's Knowledge Submission interface" — which, at that baseline, **did not exist anywhere in the repository, under any name**. `docs/reviews/PROGRAMME_4_UNIT_7_DEPENDENCY_RECORD.md`, written immediately after, restated this as a formal dependency record with an explicit **Resumption Point**: *"A constitutional Knowledge Submission interface exists and is available for orchestration."*

---

## 3. The Dependency Is Resolved — Verified Independently, Not Assumed

The governing task's own "Repository baseline" states Programme 3 Unit 8 (Knowledge Submission) and Unit 9 (Knowledge Retrieval) are complete. This review does not accept that on the task's own word; it is independently re-confirmed here, directly against the repository:

- **`src/interfaces/KnowledgeStore.kt`** declares `interface KnowledgeSubmission { suspend fun submit(requestingPrincipalId: PrincipalId, candidate: KnowledgeCandidate): KnowledgeSubmissionDisposition }`, confirmed present.
- **`src/runtime/DefaultKnowledgeSubmission.kt`** exists, is the sole implementation, self-gates via its own held `PermissionEngine`, and is composed into `ParkerRuntime.kt` as `knowledgeSubmission`.
- **`src/runtime/DefaultKnowledgeRetrieval.kt`** exists, implements the full Unit 9.1–9.5 chain (deterministic matching, staleness disclosure, lifecycle shaping, two-tier permission enforcement), and is composed into `ParkerRuntime.kt` as `knowledgeRetrieval`.

**The exact condition the Dependency Record's own Resumption Point named — "a constitutional Knowledge Submission interface exists and is available for orchestration" — is satisfied**, confirmed directly, not by inference from the task's own framing.

---

## 4. The Central Finding: Unit 7 Was Already Built, Against This Exact Resolved Dependency

**This is the finding the rest of this review exists to establish beyond doubt.** Traced through `git log`, in chronological order:

| Commit | Date | Content |
|---|---|---|
| `7a62fac` | (baseline) | "feat: implement evidence intelligence unit 6" — the Planning and Boundary Review's own baseline |
| `badc0f5` | — | "docs: record Programme 4 Unit 7 dependency on Programme 3" — the Dependency Record |
| `3c77bbd` | — | "docs: add Operational Assurance Programme roadmap" |
| **`ca7aa8d`** | **2026-08-05, earlier** | **"feat: implement constitutional knowledge submission" — Programme 3 Unit 8** |
| **`abdd513`** | **2026-08-05, 04:00** | **"feat: implement evidence intelligence acceptance coordination" — Programme 4 Unit 7 itself** |
| `d6240b7` | — | "feat: add permission-gated memory core composition" |
| **`7f3ba03`** | **2026-08-05, 05:55** | **"feat: compose evidence intelligence runtime" — Programme 4 Unit 8 (Runtime Composition)** |
| (OCR-programme commits, unrelated) | 2026-08-05 later, 2026-08-06 early | — |
| `d9725f4` through `93ace44` | 2026-08-06 | Programme 3 Unit 9, sub-units 9.1 through 9.6 (this session's own immediately preceding work) |

**Commit `abdd513` is Unit 7, built directly against the real `KnowledgeSubmission` interface `ca7aa8d` had just introduced** — confirmed by reading the diff directly: it adds `src/runtime/EvidenceIntelligenceAcceptanceCoordinator.kt` (404 lines) and `tests/runtime/EvidenceIntelligenceAcceptanceCoordinatorTest.kt` (793 lines), and appends a 26-line "Acceptance-tracking note" to the Implementation Plan itself confirming, in the same commit, that Finding 1 of the Planning and Boundary Review is resolved. **Commit `7f3ba03` is Unit 8**, composing Units 1–7 into `ParkerRuntime.kt`, adding the `analyseEvidence` production entry point, and adding `tests/composition/ParkerRuntimeEvidenceIntelligenceCompositionTest.kt`.

**Both commits predate this session's own Programme 3 Unit 9 work by a full day.** The governing task's framing — determine whether Unit 7 "now" has sufficient authority to "proceed directly to implementation" — describes a state of affairs the repository moved past before the present session began.

---

## 5. Point-by-Point Verification, Against the Task's Own Nine Checks

| # | Check | Finding |
|---|---|---|
| 1 | Constitutional authority | **Present, and exercised.** CDR-007 (Accepted, Canonical, Frozen) classifies Evidence Intelligence as a first-class analytical subsystem and names "a Knowledge Candidate submission to Knowledge Memory's existing pipeline" as its sole path to durable knowledge — exactly the act Unit 7 performs. Chapter 10 and CDR-005 govern the `MemoryCore`-leg permission gate Unit 7 itself holds, applied correctly (self-gating for two legs, coordinator-gated for the one leg `MemoryCore` constitutionally cannot self-gate — Memory Core Scope Lock §6). |
| 2 | Complete contracts | **Complete, and already consumed.** `KnowledgeSubmission`/`KnowledgeSubmissionDisposition`, `EvidenceCustodian`/`EvidenceAcceptanceResult`, and `MemoryCore`'s write interface are each frozen, implemented, and are exactly the three types `EvidenceIntelligenceAcceptanceCoordinator.dispatch` calls, unchanged. |
| 3 | Complete scope definition | **Complete.** `EVIDENCE_INTELLIGENCE_UNIT_7_PLANNING_AND_BOUNDARY_REVIEW.md` §2–§9 fixed exactly seven responsibilities and a full ownership-boundary table before implementation began; the built class's own KDoc traces every one of its own design choices back to that same table, confirmed by direct comparison (Section 6, below). |
| 4 | Complete ownership boundaries | **Complete.** CDR-007 §5's ownership table and the Planning Review's §8 ownership-verification table both fix that Unit 7 owns sequencing only — never custody, promotion, or evidential-state authority — and the built class holds exactly four dependencies, no interface of its own, and no reference to `EvidenceIntelligence`, `ReasoningProvider`, `KnowledgeCandidateEvaluator`, or any Knowledge Memory lifecycle evaluator, confirmed directly by reading its own import list and constructor. |
| 5 | Complete runtime dependencies | **Complete.** `EvidenceCustodian`, `MemoryCore`, `KnowledgeSubmission`, `PermissionEngine` — all four exist, are each independently implemented and composed, and are the coordinator's own, sole four constructor parameters. |
| 6 | Permission authority | **Present and correctly applied.** Two of three legs (`EvidenceCustodian.accept`, `KnowledgeSubmission.submit`) already self-gate; the coordinator holds its own `PermissionEngine` for the one leg (`MemoryCore`'s write interface) that constitutionally never self-gates, mirroring `EvidenceRegistrationCoordinator`'s own already-accepted precedent exactly, exactly as Finding 2 of the Planning Review required. Its own disclosed resource/action pair (`MEMORY_CORE_ACCEPTANCE_RESOURCE_ID`/`ACCEPT_MEMORY_CORE_CANDIDATE_ACTION_NAME`) is confirmed, by direct inspection of the current `ParkerRuntime.kt`, both registered and mapped to an already-`APPROVED` `WRITE`/`MEMORY` policy rule — genuinely reachable today, not merely constructed. |
| 7 | Knowledge Submission availability | **Available, and is Unit 7's own third acceptance leg.** Confirmed present, composed, and directly invoked by `dispatchKnowledge`. |
| 8 | Knowledge Retrieval availability | **Available — but not, and never was, a Unit 7 dependency at all.** This check's own premise requires correction: neither the Planning and Boundary Review, the Dependency Record, the Implementation Plan's own Unit 7 entry, nor the built class's own four-dependency list names `KnowledgeRetrieval` anywhere. Unit 7 dispatches candidates to accepting subsystems; it never reads anything back. Confirmed directly by reading `EvidenceIntelligenceAcceptanceCoordinator.kt`'s own imports and constructor: no reference to `KnowledgeRetrieval` or `DefaultKnowledgeRetrieval` exists anywhere in the file. Knowledge Retrieval's own completion (Programme 3 Unit 9) is welcome, correct, and unrelated background progress — not a Unit 7 precondition that was ever open. |
| 9 | No remaining upstream blockers | **None found.** The one blocker Findings 1/2 of the Planning Review and the Dependency Record identified is resolved (Section 3, above); no other blocker was ever named by any governance document read for this review. |

---

## 6. Scope, Boundary, and Design-Rule Fidelity — Built Against Reviewed, Not Re-Litigated

Direct comparison of the built `EvidenceIntelligenceAcceptanceCoordinator.kt` against the Planning and Boundary Review's own pre-implementation determinations, each independently re-checked against the actual code rather than accepted from the class's own KDoc:

- **Stateless** — confirmed: no `var`, no mutable collection, exactly four `val` constructor fields, no field written after construction.
- **No `try`/`catch`** — confirmed: none exists anywhere in the file; a genuine dependency exception propagates unchanged, exactly as the Planning Review's own "Faults are never swallowed" requirement and this task's own "faults propagate" discipline both require.
- **Concrete, non-interface-backed** — confirmed: `internal class`, no interface declared for it anywhere in `src/interfaces/`.
- **Reuses existing outcome types, adding only the one genuinely missing case** — confirmed: `ArtifactAcceptance` wraps `EvidenceAcceptanceResult` unchanged; `KnowledgeAcceptance` wraps `KnowledgeSubmissionDisposition` unchanged; `RecordAcceptance` is the one new, `internal`-only case, precisely because `MemoryCore`'s own contract has no vocabulary for a denial only the coordinator's own Permission Engine call can produce — exactly the reasoning Finding 2 anticipated.
- **The defensive "governed identifier already exists" check** the Implementation Plan's own §8 Unit 7 required is present (`requireGovernedIdentifier`), reads only the candidate's own already-carried reference (never a `MemoryRetrieval` call this class holds no dependency capable of performing), and fails as "a genuine implementation fault" (a thrown `IllegalStateException` via `check`), never a silent proceed — matching the Implementation Plan's own text exactly.
- **Accept-before-submit ordering** — confirmed structurally: `dispatchRecord` (the `MemoryCore` leg) and `dispatchKnowledge` (the `KnowledgeSubmission` leg) are two independent branches of the same `when` in `dispatch`; the coordinator's own defensive check inside `dispatchKnowledge` is the enforcement mechanism the Planning Review named, not a scheduling guarantee across separate invocations (which the Planning Review itself correctly scoped to Unit 8, not Unit 7).

No divergence from the pre-implementation Planning and Boundary Review was found anywhere in the built class.

---

## 7. Runtime Composition — Also Already Complete, Verified Directly

Although the governing task names Unit 7 specifically, the Dependency Record itself ties Unit 7's own completion directly to Unit 8's ("Unit 8 of Programme 4... therefore also remains blocked... pending Unit 7"), so this review confirms Unit 8's own state too, for completeness and honesty rather than silently stopping at the exact unit named:

- `src/composition/ParkerRuntime.kt` composes `evidenceIntelligenceAcceptanceCoordinator = EvidenceIntelligenceAcceptanceCoordinator(defaultEvidenceCustodian, memoryCore, knowledgeSubmission, permissionEngine)`, confirmed present at the current `HEAD`, using the real, shared `knowledgeSubmission` and `permissionEngine` instances, never parallel ones.
- A production entry point, `analyseEvidence(requestingPrincipalId, request)`, exists, gates through Unit 6's own invocation gate first, then dispatches Unit 5's own result list to Unit 7's `dispatch` unchanged.
- `tests/composition/ParkerRuntimeEvidenceIntelligenceCompositionTest.kt` exists (16 tests, confirmed present and passing) and independently verifies shared-instance identity, fail-closed Memory Core retrieval behaviour, and full invocation-to-acceptance wiring end to end.

**Programme 4 (Evidence Intelligence), Units 1 through 8, is complete in full**, not merely Unit 7 in isolation.

---

## 8. Test Verification — Run, Not Assumed

```
$ ./gradlew clean test
BUILD SUCCESSFUL in 45s
5 actionable tasks: 5 executed
```

Full repository suite: **1763 tests, 0 failures, 0 errors, 5 pre-existing skips**, confirmed at this review's own `HEAD`. This includes `EvidenceIntelligenceAcceptanceCoordinatorTest` (25 tests) and `ParkerRuntimeEvidenceIntelligenceCompositionTest` (16 tests), both passing today, not merely at the time of their own original commit. Running the existing suite is read-only verification of already-committed code; no file was modified to obtain this result.

---

## 9. Implementation Gaps Versus Governance Gaps

**Governance gaps found: none.** Every governing document Unit 7 depends on — CDR-006, CDR-007, the Evidence Intelligence Contract Design, Scope Lock, and Implementation Plan, and Programme 3's own Unit 8 Clarification — is Adopted/Accepted/Frozen and cited correctly by the built implementation's own KDoc.

**Implementation gaps found: none.** Every responsibility the Planning and Boundary Review named is present in the built class; every dependency is real, shared, and correctly gated; every test category the original review anticipated (statelessness, dependency-reachability, the defensive identifier check, fault propagation) has a corresponding, passing test.

**Do not invent governance simply because previous units required it, per this task's own instruction — and none is invented here.** Unit 9's own multi-document Completion Review / Independent Constitutional Review / Defect Confirmation Review cadence, used repeatedly this session, was a discipline Programme 3 Unit 9 itself required because its own work was newly authored during this session. Unit 7's own governance-first workflow — a Planning and Boundary Review, a Dependency Record, then direct implementation once the dependency resolved, with an in-place "Acceptance-tracking note" appended to the Implementation Plan rather than a separate Completion Review document — is a different, already-completed, already-accepted cadence belonging to a prior session's own work. This review does not require Unit 7 to be retroactively re-reviewed in Programme 3's own document shape merely because that is this session's own recent habit; doing so would be exactly the kind of invented-governance this task's own instruction warns against.

---

## 10. Constitutional Conclusion

**Programme 4 Unit 7 (Acceptance Coordination) already possesses complete constitutional authority, and has already exercised it.** No further governance action is required, and no further implementation action is required. The question the governing task poses — whether Unit 7 "now" has sufficient authority to "proceed directly to implementation" — is answered most accurately not with "yes, proceed," but with the more precise finding this review's own evidence supports: **there is no remaining implementation to proceed to.** Unit 7 was built, tested, and composed against the exact dependency this repository's own governance record identified as the sole blocker, once and only once that dependency was genuinely resolved, and it remains correct, tested, and composed today.

---

## Implementation Readiness

```
PROGRAMME 4 UNIT 7 (ACCEPTANCE COORDINATION) -- ALREADY COMPLETE.
NO IMPLEMENTATION ACTION REQUIRED. NO GOVERNANCE ACTION REQUIRED.
```

If the intent behind this task was to resume genuinely suspended work, that work has already been resumed and finished, in a prior session, and is present, correct, and passing at this review's own baseline. If further Evidence Intelligence work is intended, it belongs to a unit later than Unit 8 — none is named or authorised by any document this review found, and none is proposed here.

---

## Final Report

**Files created:** `docs/reviews/PROGRAMME_4_UNIT_7_ACCEPTANCE_COORDINATION_PLANNING_REVIEW.md` (this document) — the only file created.

**Files modified:** none.

**Constitutional conclusion:** no genuine governance gap exists; Unit 7 (and Unit 8) were already correctly authorised, built, and composed, verified here against primary repository evidence rather than assumed.

**Implementation readiness:** not applicable in the sense the task anticipated — implementation is not pending; it is complete, confirmed passing at this review's own `HEAD`.

**Final Git Status:**

```
$ git status --short
?? docs/reviews/PROGRAMME_4_UNIT_7_ACCEPTANCE_COORDINATION_PLANNING_REVIEW.md
```

Nothing staged, committed, or pushed.
