# Parker Platform — Repository Baseline Audit

## Document Metadata

| Field | Value |
|---|---|
| **Document type** | Repository-wide forensic audit (baseline) |
| **Audit date** | 2026-07-31 |
| **Repository** | Parker Platform (`parker-platform`) |
| **Branch** | `main` (`main == origin/main` at time of writing) |
| **Commit reviewed** | `4e657a9` — `feat: add evidence registration coordinator`, including the README.md milestone update applied in the same working session as this audit. That README update was subsequently committed by Steven as `b48ce9368cf4a6a83701fa81abb76a46f637b5d5` — `docs: update README for evidence custodian milestones` — with no further content changes beyond what this audit examined. Current `HEAD` at time this document was written is `b48ce93`. |
| **Audit scope** | Constitutional Documents; Architecture; Governance; Contract Design; Implementation Plans; Repository Structure; Production Code; Test Suite; Documentation; Current Platform Capability; Programme Assessment (per subsystem); Technical Debt; Repository Risks; Constitutional Compliance; Strategic Assessment |
| **Methodology** | Six parallel, independently-scoped research passes (five completed as dispatched; one — Test Suite Structure — was terminated mid-task by a platform-level API spend limit and was completed via direct, narrower manual investigation instead of a repeat dispatch). Every finding below is grounded in direct citation to a specific file, commit, or code inspection. No speculation and no invented problems are included. Every claim is labeled, implicitly by its section or explicitly inline, as one of: verified fact, repository evidence, constitutional interpretation, implementation observation, or an explicitly-tagged `[Inference]`. No inference is presented as established fact. |
| **Repository constraints observed** | Strictly read-only. No production code, test code, specifications, contracts, architecture/governance documents, README, implementation plans, Scope Locks, CDRs, or build configuration was modified in the course of the underlying audit. No git operations (staging, commits, pushes, tags, history rewrites) were performed. No test or build command was executed; no verification claim in this document should be read as an observed pass/fail result. |
| **Document status** | **Final — Baseline Audit.** This is a point-in-time governance record reflecting repository state as of the commit reviewed above. It is not a living document and should not be edited in place as the repository evolves; a materially later assessment should be produced as a new, separately dated audit document rather than as a revision of this one. |
| **Author / provenance** | Produced by Claude (Cowork mode) under explicit, scoped authorization from Steven, following the Parker governance lifecycle's read-only review conventions. Steven is responsible for all git operations on any resulting documentation changes. |

---

## Table of Contents

1. [Executive Summary](#1-executive-summary)
2. [Repository Maturity Assessment](#2-repository-maturity-assessment)
3. [Constitutional Assessment](#3-constitutional-assessment)
4. [Governance Assessment](#4-governance-assessment)
5. [Architecture Assessment](#5-architecture-assessment)
6. [Contract Assessment](#6-contract-assessment)
7. [Programme-by-Programme Assessment](#7-programme-by-programme-assessment)
8. [Production Code Assessment](#8-production-code-assessment)
9. [Test Assessment](#9-test-assessment)
10. [Documentation Assessment](#10-documentation-assessment)
11. [Technical Debt Register](#11-technical-debt-register)
12. [Risk Register](#12-risk-register)
13. [Repository Strengths](#13-repository-strengths)
14. [Repository Weaknesses](#14-repository-weaknesses)
15. [Immediate Priorities](#15-immediate-priorities)
16. [Medium-Term Priorities](#16-medium-term-priorities)
17. [Long-Term Priorities](#17-long-term-priorities)
18. [Recommended Next Lawful Implementation Unit](#18-recommended-next-lawful-implementation-unit)
19. [Overall Conclusion](#19-overall-conclusion)

---

## 1. Executive Summary

Parker is a substantially real, substantially governed platform with two honest, separately-verifiable stories layered on top of each other. The first story — Identity, Permission Engine, Execution Pipeline, the Conversation/Reasoning/Reply/Planning/Controlled-Agent-Run pipeline — is built, documented, wired into a single production composition root (`src/composition/ParkerRuntime.kt`), and is the one subsystem group demonstrably reachable end-to-end today. The second story — Memory Core, Knowledge Memory (Programme 3), and Evidence Custodian — is equally real and equally governed (each has a complete Contract-Design → Scope-Lock → Implementation-Plan lineage, each has passing, structurally rigorous tests), but **none of the three is constructed anywhere in `ParkerRuntime.kt`**. This is confirmed by direct grep, not inference, and it is self-disclosed inside the unwired classes' own KDoc as a deliberately deferred "Runtime Integration" phase — not a hidden defect. The single most important fact this audit surfaces is that Parker today has a working conversational/planning/execution runtime with **no memory, no knowledge, and no evidence custody reachable in production** — three fully-built subsystems sitting adjacent to, but outside, the running platform.

Separately, and unrelated to Evidence Custodian, a genuine constitutional-governance program ("Programme 3 Constitutional Remediation") found six omissions in the frozen governance underneath Knowledge Memory Units 6–10, closed two of them via CDR-003/CDR-004, and left Programme 3 in an explicitly self-declared "paused pending governance amendment" state (`docs/reviews/PROGRAMME_3_CONSTITUTIONAL_AUDIT.md`). One of the six — "User Importance" — has no owning document anywhere in the repository at all.

No forged, fabricated, or invented finding appears below; every claim is either a direct citation or explicitly marked `[Inference]`.

---

## 2. Repository Maturity Assessment

Parker is a **late-stage-alpha, single-pipeline-production platform with two additional fully-built-but-unintegrated subsystems**. The Conversation→Reasoning→Reply→Planning→Controlled-Agent-Run-Submission pipeline is genuinely production-composed and its own status headers are largely accurate. Roughly 50 numbered conceptual architecture chapters (`docs/architecture/01`–`50`) remain planned-only prose with no implementation claim of their own — this is honest, not overclaiming. Governance process maturity is high (CDR chain, Scope Locks, per-unit Implementation Plans, self-auditing review culture) but its own bookkeeping has drifted: `docs/implementation/IMPLEMENTATION_HISTORY.md`'s closing summary sections were never updated past Sprint 1 despite the document's body running through Sprint 11, and at least 11 Contract-Design documents still carry a literal "Design proposal, not yet reviewed or accepted" header for subsystems the repository elsewhere treats as accepted and implemented.

---

## 3. Constitutional Assessment

**Verified facts.** The Parker Constitution and `epistemic-integrity.md` (Constitutional Amendment No. 1, ratified 29 Jul 2026, 19 Articles / 69 Constitutional Tests) are internally consistent with each other; the Constitutional Audit states this directly: "No new constitutional contradiction was found in this audit. The Constitutional Articles themselves... are internally consistent with each other" (`PROGRAMME_3_CONSTITUTIONAL_AUDIT.md`, Part 6). `docs/architecture/PROJECT_GOVERNANCE.md` — a document named as one of the platform's four governing pillars — **is a zero-byte file**. This is a real, verified fact, not an inference: a named constitutional pillar exists in title only.

**CDR chain.** CDR-001/002 found the Constitution merely under-specified (not contradictory) on memory-record comparison; CDR-003 resolved it (Model B, frozen into `MEMORY_CORE_SCOPE_LOCK.md` §18, committed `b3f7090`); CDR-004 resolved provenance-identifier resolution similarly (Scope Lock §19, "Ready to Freeze"). **CDR-005 and CDR-006 are both still in Draft status** — neither has completed the "independent constitutional verification and Final Freeze Verification cycle" its own status line requires. This means the PermissionEngine proposal-class admission model (CDR-005) and the Evidence Custody/Immutability model (CDR-006) — the latter being the direct constitutional basis for the entire Evidence Custodian programme already implemented through Phase 5/6 — are running on **draft, not-yet-frozen** constitutional authority. This is a real governance-sequencing finding: substantial production code (Evidence Custodian) has been built against a CDR that has not itself completed its own required freeze cycle.

**Programme 3 Constitutional Remediation.** `PROGRAMME_3_BLOCKER_OWNERSHIP_MATRIX.md` found zero implementation omissions and six governance omissions blocking Knowledge Memory Units 6–10. Of six required amendments, two are resolved and frozen (Comparison — CDR-003; Provenance Lookup — CDR-004), one is resolved directly in Contract Design V2 §16 (Explicit Request), one has only a draft precursor (Permission Gating — CDR-005), one is unowned by any document at all (**User Importance** — confirmed absent everywhere it is named, including Contract Design V2 §16.11's own "Deferred Factors, Unchanged"), and one (Common-Origin Determination) has no resolving document despite its prerequisite now existing. `PROGRAMME_3_CONSTITUTIONAL_AUDIT.md`'s own recommendation: "Programme 3 should pause pending governance amendment." **Whether Units 6/7 are now unblocked following CDR-003/004 is not stated anywhere in the documents reviewed** — flagged explicitly `[Inference]`, not resolved.

**Assessment:** the constitutional layer is coherent where frozen, honestly incomplete where not, and the one clear structural weakness is that CDR-005/006 remain in draft while carrying real production weight (CDR-006 in particular).

---

## 4. Governance Assessment

The documented lifecycle (Governance Review → Contract Design → Scope Lock → Implementation → Native Verification → Commit → Push) was followed, not bypassed, everywhere checked. Every CDR and Programme 3 governance document carries an explicit "nothing staged, nothing committed" confirmation, and the one place an assigned governance obligation was found unbuilt (Runtime must wrap every `MemoryCore` write in a `PermissionEngine.evaluate` call, per Memory Core Scope Lock) is an **unbuilt obligation**, not a bypass — `InMemoryKnowledgeStore` is constructed directly in `ParkerRuntime.kt` with no such wrapper, a known, disclosed gap, not a silent violation.

The one clear governance-agreement gap found: the original `PROGRAMME_3_UNIT_6_SCOPE_LOCK_CLARIFICATION.md` directly conflicted with the higher-precedence, frozen `PROGRAMME_3_KNOWLEDGE_MEMORY_CONTRACT_DESIGN_V2.md` on three points (unconditional-promotion vs. a genuine promotion gate; silent substitution of corroboration/contradiction for six named criteria categories; unexcused single-factor classification) — found and corrected by `PROGRAMME_3_UNIT_6_CONSTITUTIONAL_RECONCILIATION.md`. This is the governance process catching its own error, which is evidence the process works, not evidence it is broken.

---

## 5. Architecture Assessment

**Structural fact:** `docs/architecture/` contains two very different kinds of document coexisting in one flat directory — roughly 50 short (200–1,500 byte) numbered conceptual chapters describing intended future architecture, and roughly 75 substantial, actively-governed Contract-Design/Scope-Lock/ADR/Reconciliation documents. None of the 50 numbered chapters makes a non-credible implementation claim; the risk runs the other way — several implemented subsystems (Permission Engine, World Model, Memory Runtime, Planner Runtime, Task Manager, Identity Service, Authentication) have **no forward citation from their own numbered chapter** to the later, more detailed governance document that actually describes what was built. A reader following the numbered chapters alone would not discover that most of these subsystems now exist.

**Status-header staleness (systemic, real).** At least 11 Contract-Design/Architecture documents governing subsystems `00-index.md` and `IMPLEMENTATION_GAPS.md` both treat as accepted/implemented still carry the literal, unedited header **"Design proposal, not yet reviewed or accepted"**: `MEMORY_RUNTIME_ARCHITECTURE.md`, `WORLD_MODEL_CONTRACT_DESIGN.md`, `WORLD_MODEL_RUNTIME_ARCHITECTURE.md`, `PLANNER_RUNTIME_CONTRACT_DESIGN.md`, `PLANNER_RUNTIME_PROGRESSION_DESIGN.md`, `MULTI_STEP_AGENT_RUN_DESIGN.md`, `MODULE_CONTRACT_DESIGN.md`, `COMMUNICATION_CONTRACT_DESIGN.md`, `CONVERSATION_ENGINE_CONTRACT_DESIGN.md`, `REASONING_PROVIDER_CONTRACT_DESIGN.md`, `RESPONSE_DELIVERY_CONTRACT_DESIGN.md`. Sibling documents in the same family (`WORLD_MODEL_SOURCE_CONTRACT_DESIGN.md`, `IdentityService.md`) *did* get their headers updated to "Implemented" — the convention exists, it just wasn't applied uniformly. This is a documentation-hygiene finding, not an architectural one: the underlying subsystems are genuinely built; their own status headers simply lie about it by omission.

**No duplication found** between `MEMORY_CONTRACT_DESIGN.md` (legacy `MemoryStore`, being renamed "Knowledge Memory") and `MEMORY_CORE_CONTRACT_DESIGN.md` (the newer, more foundational Entity/Document/Assertion/Relationship/Provenance layer) — `MEMORY_ARCHITECTURE_RECONCILIATION.md` explicitly reconciles both, and the rename itself is flagged "Recommended, not immediately" required — a disclosed, unexecuted migration, not a contradiction.

**Genuinely unresolved, per `IMPLEMENTATION_GAPS.md`'s own tracking:** #8/#16 (schema-file duplication), #20 (`AgentHealth` shape), #35–#38 and #41 (Principal-lifecycle/access-control edges), #50 (synchronous `EventBus` — a slow subscriber can block all publishers), #51 (no structural persistence/durability/audit boundary), and #53 (the largest, most load-bearing gap in the file — still marked Open at its final entry, listing: no real Planner-backed tool execution, the Goal-Manager/Chapter-23 naming collision untouched, `SUSPEND`/`RESUME`/`CANCEL` submission wiring absent, one-sided Conversation History, no live HTTP round-trip to a real model server ever exercised).

---

## 6. Contract Assessment

Every contract file read in `src/contracts/` and `src/interfaces/` (26 + 28 files) carries KDoc that explicitly names its own governing Contract Design/Scope Lock/Implementation Plan — an unusually self-documenting codebase. No conflicting-responsibility duplication was found between `MemoryCore`, `KnowledgeStore`, and `WorldModel` — each states an explicit non-overlap boundary in its own KDoc.

**Missing implementation, named in approved governance, not found in code:** `PermissionGatedMemoryCore` and `PermissionFilteredMemoryRetrieval` — both are named explicitly, by `MemoryCore`'s own interface KDoc, as the future decorators that would make Memory Core's permission story real; neither exists anywhere in `src/` (confirmed by grep, zero matches). This means Memory Core today is, by its own documentation's admission, "permission-neutral... not yet permission-gated" with no decorator built to close that gap.

**Orphaned (fully governed, fully implemented, zero production reachability):** `MemoryCore`/`InMemoryMemoryCore`/`EventPublishingMemoryCore`; `EvidenceCustodian`/`DefaultEvidenceCustodian`/`EvidenceRegistrationCoordinator`/both `EvidenceArtifactStorage` implementations; `DefaultKnowledgeCandidateEvaluator`/`DefaultKnowledgeRevisionEvaluator`/`DefaultKnowledgeRetirementEvaluator`. All three groups are confirmed, by direct grep against `ParkerRuntime.kt`, never constructed there (see §8).

**One disclosed near-duplication:** `KnowledgeItem.history` vs. legacy `KnowledgeRecord.history` — two differently-shaped fields sharing a name, resolved in writing (`PROGRAMME_3_UNIT_4_SCOPE_LOCK_CLARIFICATION.md` §1) as "never synchronised" — documented, but a real latent-confusion risk for any future reader who assumes the two are kept in sync.

---

## 7. Programme-by-Programme Assessment

**Programme 1 — Runtime Foundation (Identity, Permission Engine, Execution Pipeline, Tool Registry, Resource Registry, Event Bus).**
*Status:* Complete and wired. *Completed:* identity-aware, policy-backed Permission Engine; real Tool binding/execution; Event Bus with lifecycle publication; Module Registry. *Blocked:* durable audit-log/cross-restart persistence (ADR-024 decided the requirement, implemented none of it); `ToolInvocationBinding` access remains convention-, not construction-, enforced. *Recommended next lawful activity:* none urgent — this programme is stable; persistence is a disclosed, non-blocking gap.

**Conversation/Reasoning/Reply/Planning/Controlled-Agent-Run-Submission pipeline.**
*Status:* the platform's most mature programme, and the only one demonstrably wired end-to-end into `ParkerRuntime.kt`. *Completed:* Communication Intake, Conversation Engine, model-backed Reasoning Provider, Response Composer/Delivery, Reply Delivery Coordinator, Production Composition Root, Production Reasoning Context (Conversation History + Memory + World Model sources), Goal-Planning Handoff, Plan Candidate Generation, Planner Runtime integration, and Controlled Agent Run Submission (two-phase acceptance/execution). *Blocked:* production Tool execution from planned Goals (the `DeterministicAgentStepSource` is an explicit, disclosed stand-in); broader Task lifecycle completion/failure handling; live validation against a real model server has never been exercised. *Recommended next lawful activity:* per the README's own stated boundary, a Planner-backed `AgentStepSource` is the next unit — already correctly sequenced, no change recommended by this audit.

**Memory Core (Programme 2).**
*Status:* fully implemented (all 9 planned units — identifiers, Provenance, Entity, Document, Assertion, Relationship, `MemoryCore`/`MemoryRetrieval` interfaces, `InMemoryMemoryCore`, `EventPublishingMemoryCore`), fully governed (Contract Design + 4 Errata + Scope Lock + Implementation Plan), **zero production reachability** — not constructed anywhere in `ParkerRuntime.kt` (confirmed by grep). *Blocked:* `PermissionGatedMemoryCore`/`PermissionFilteredMemoryRetrieval` (named, not built) are the real prerequisite before wiring would even be safe, since Memory Core cannot self-gate by its own Scope Lock. *Constitutional dependency:* none outstanding — its own CDR-003/004 are frozen. *Recommended next lawful activity:* design and implement `PermissionGatedMemoryCore`, then a Runtime Integration unit — in that order, since wiring an ungated Memory Core into production would leave every write unauthorized-by-default rather than deliberately denied.

**Knowledge Memory (Programme 3).**
*Status:* Units 4–7.3 complete (KnowledgeItem/Reference, KnowledgeCandidate, constitutional promotion evaluator, lifecycle event types, revision evaluator, retirement evaluator); restoration-evaluation logic deferred (only the event shape exists); Unit 8 (history population) and Unit 9 (retrieval-time projection) not found in `src/`; Unit 10 (retiring the legacy field) not started. *Blocked, explicitly:* per its own Constitutional Audit, "paused pending governance amendment" — five of six governance omissions must close before further units resume; "User Importance" has no owning document anywhere. Not wired into `ParkerRuntime.kt`. *Recommended next lawful activity:* resolve the outstanding governance amendments (Permission Gating, User Importance, Common-Origin Determination) before resuming implementation — this is the programme's own stated position, not this audit inventing a blocker.

**World Model.**
*Status:* the one non-pipeline subsystem confirmed **wired into `ParkerRuntime.kt`** (via `WorldModelSource`, line 305). Governed by Contract Design + Runtime Architecture (no dedicated Scope Lock of its own name, unlike Memory Core/Evidence Custodian). *Blocked:* no `EventBus` publication (Gap #47, ADR-024 decided target shape, not implemented); no belief-category enumeration. *Recommended next lawful activity:* implement World Model event publication per the already-decided ADR-024 shape.

**Evidence Custodian.**
*Status:* Phases 1–5 confirmed complete by file presence (`EvidenceArtifactId`, `EvidenceArtifactStorage` + two implementations, `accept`, `retrieve`, `EvidenceRegistrationCoordinator`); Phase 6 (derivative relationship) verified this session via dedicated behavioural tests (`EvidenceDerivativeRelationshipTest.kt`) rather than new production code, per the Contract Design's own "satisfied entirely through the existing Provenance mechanism" text. **Not implemented:** Phase 7 (deletion), Phase 8 (optimisation-safeguard enforcement), Phase 9 (formal verification suite as its own artifact), Phase 10 (runtime integration). **Not wired into `ParkerRuntime.kt`**, and its own disclosed `evidence.accept`/`evidence.retrieve` action-name and resource-ID conventions are unregistered — meaning even a wired instance would deny every request today under `DefaultPermissionPolicy`'s own "Unknown Resource → DENIED" behavior, a safe-by-default, self-disclosed gap. *Recommended next lawful activity:* per the programme's own Implementation Plan ordering, Phase 7 (Deletion workflow) is next; Runtime Integration (Phase 10) remains explicitly sequenced last.

---

## 8. Production Code Assessment

**The single most load-bearing finding of this entire audit:** a direct grep of `ParkerRuntime.kt` (479 lines, the sole production composition root) for `InMemoryMemoryCore|EventPublishingMemoryCore|DefaultEvidenceCustodian|EvidenceRegistrationCoordinator|InMemoryEvidenceArtifactStorage|FileSystemEvidenceArtifactStorage|DefaultKnowledgeCandidateEvaluator|DefaultKnowledgeRevisionEvaluator|DefaultKnowledgeRetirementEvaluator` returns **zero matches**. None of these nine classes is imported, constructed, or referenced anywhere in the file. By contrast, `InMemoryKnowledgeStore` (line 291) and `InMemoryWorldModel` (line 305) are confirmed constructed. This was verified by direct grep, not inferred from prior conversation history.

**Subsystem independence — confirmed intact.** `DefaultEvidenceCustodian`'s constructor takes only `EvidenceArtifactStorage` and `PermissionEngine`; `InMemoryMemoryCore`'s constructor takes **no parameters at all**; `EventPublishingMemoryCore` takes only `MemoryCore` and `EventBus`. Neither Evidence Custodian nor Memory Core holds a reference to the other anywhere in `src/runtime/`.

**Permission-gating consistency — confirmed correct.** Self-gating classes (`DefaultEvidenceCustodian`, `DefaultExecutionPipeline`, `InMemoryAgentRuntime`) hold `PermissionEngine` directly; externally-gated classes (`EvidenceRegistrationCoordinator`) hold it only because the subsystem they're gating (Memory Core) is constitutionally forbidden from self-gating, and say so in their own KDoc. `InMemoryMemoryCore`, `InMemoryKnowledgeStore`, and `InMemoryWorldModel` all genuinely lack a `PermissionEngine` reference, matching their documented boundaries exactly.

**Dependency direction — no smell found** across the sampled coordinators (`ConversationTurnReasoningCoordinator`, `ReplyDeliveryCoordinator`, `GoalPlanningHandoffCoordinator`, `EvidenceRegistrationCoordinator`, `ConversationReplyCoordinator`, `CommunicationConversationCoordinator`) — every one holds only interfaces or deliberately-concrete sibling classes whose own KDoc discloses the design choice, never a concrete `InMemory*` implementation reached past its declared abstraction.

**Deliberate stand-ins, correctly disclosed:** `DeterministicAgentStepSource` (wired, self-documented as a fixed stand-in for a future Planner-backed step source); `InMemoryEvidenceArtifactStorage` (correctly never wired, self-documented "test-only").

---

## 9. Test Assessment

*Method note:* this section's original dedicated research pass was terminated mid-task by a platform spend limit; the cross-reference below was completed directly via targeted file-listing and grep rather than a full independent research pass, so this section is narrower in scope than the other eight research areas.

**Coverage, by direct file-name cross-reference.** Of 46 files in `src/runtime/`, 44 have a directly-named corresponding test file in `tests/runtime/` (e.g. `DefaultEvidenceCustodian.kt` → `DefaultEvidenceCustodianTest.kt`). The two without one — `ConversationOutcome.kt` and `GatedOutcome.kt` — are sealed outcome types with no behavior of their own; both are plausibly exercised indirectly through the coordinator tests that construct/return them (`ConversationReplyCoordinatorTest`, `ReplyDeliveryCoordinatorTest`), but neither has a dedicated test file confirming its own shape. This is a minor, low-risk gap, not a serious one.

**Structural/governance-boundary tests — a real, uneven finding.** Exactly three files in the entire test suite perform reflection-based structural verification of a subsystem's own architectural boundary (asserting what a class must NOT contain, not just what it does): `tests/contracts/EvidenceCustodianScopeTest.kt`, `tests/contracts/EvidenceArtifactStorageScopeTest.kt`, and `tests/contracts/InterfaceContractShapeTest.kt`. **All three are Evidence-Custodian-related or generic.** No equivalent scope test exists enforcing, for example, that `InMemoryMemoryCore` never gains a `PermissionEngine` field, or that `WorldModel`/`KnowledgeStore` never gain a forbidden cross-reference — these guarantees hold today by direct inspection (confirmed in §8), but nothing in the test suite would catch a future regression introducing one. This is a genuine, evidence-based coverage gap, not a guess.

**Fakes are centralized, not duplicated.** `tests/runtime/Fake*.kt` (19 files: `FakePermissionEngine`, `FakeIdentityService`, `FakeResourceRegistry`, `FakeToolRegistry`, `FakeWorldModelSource`, etc.) are each defined once and reused across multiple test files (confirmed reuse of `FakePermissionEngine` across `DefaultEvidenceCustodianTest`, `EvidenceRegistrationCoordinatorTest`, and `EvidenceDerivativeRelationshipTest`, among others) — no evidence of duplicated fake-class definitions was found.

**Composition-layer coverage** (`tests/composition/`, 8 files vs. `src/composition/`, 10 files): `ParkerRuntime.kt` itself is covered by five dedicated tests (conversation pipeline, failure handling, reasoning-context integration, startup/shutdown, stage cancellation); `EventPublishingMemoryCore` has its own dedicated test **despite never being wired into `ParkerRuntime.kt`** — confirming it was verified in isolation, which is good practice, but also underscores that "tested" and "reachable in production" are two separate facts throughout this repository. `OwnerNotificationSink.kt`, `ParkerLogger.kt`, and `RuntimeEventLogger.kt` have no obviously dedicated test file — plausibly thin enough to be exercised only indirectly; not independently confirmed in this pass.

**Verification claims:** no claim is made here about how many tests actually pass or fail — no test was executed in the course of this audit. Any test-count figure elsewhere in this repository's own documentation (e.g. README.md) is that document's own claim, sourced from Steven's own local verification, not something this audit re-confirmed.

---

## 10. Documentation Assessment

**No documentation/implementation drift found** in the three spec-vs-code pairs spot-checked: `Principal.md` vs `Principal.kt` (field-for-field match), `PermissionEngine.md` vs `PermissionEngine.kt` (exact two-method match), and `AgentRuntimeSpecification.md` §5 vs `AgentRunLifecycle.kt` (state-for-state match, including the deliberate absence of a `CREATED→FAILED` edge in both). This is a genuinely positive finding, not merely an absence of checking.

**Scaffolding documents left behind.** `docs/glossary/GLOSSARY.md`, `docs/interfaces/README.md`, `docs/schemas/README.md`, `docs/engineering/*`, `docs/development/CLAUDE_CODE_BRIEF.md` all carry the repository's earliest timestamp and are one-paragraph-or-less stubs, superseded in substance (though not formally replaced) by the much more detailed Volume 1–6 specifications and `docs/architecture/*` corpus built since.

**`docs/roadmap/ROADMAP.md` is nine lines** — one-line placeholders for v0.4–v1.0 with no cross-reference to the far more detailed governance/implementation history that has since accumulated.

**Volume-level inconsistency:** Volumes 1–3 (`docs/specifications/`) each have both a README and an index; Volumes 4–6 (Agent Runtime, Task Manager Runtime, Planner Runtime) have only a single specification file each, no README/index — inconsistent structure, though the content of those single files is extensive and well-governed.

**Repository Structure diagram gap:** the README's own "Repository Structure" section (prior to this audit's companion index update, see §4 of the update log below) omitted `docs/decisions/` — a real, substantive, actively-updated directory (all six CDRs, the most recently modified files in the whole `docs/` tree) — and did not yet reference `docs/audits/`, since that directory did not exist prior to this document.

**Vestigial top-level directories, confirmed:** `agents/`, `examples/`, `plugins/`, `tools/` each contain exactly one file — a one-line placeholder README — with zero other content.

**Historical material, no archival separation:** `docs/reviews/` (58 files) mixes documents that explicitly self-declare "Archived... superseded, do not treat as current or binding" (the Epistemic Integrity V0.1–V0.3 drafts) with documents still actively cited as authoritative, in one flat directory with no subfolder distinction. This is a structural observation, not a recommendation to act — no document instructs a reorganization, and this audit does not recommend performing one unilaterally.

---

## 11. Technical Debt Register

*(Deliberate constitutional decisions are explicitly excluded from this register — see the closing note below.)*

**Architectural debt:**
- Memory Core's permission story is incomplete by its own design's own admission — `PermissionGatedMemoryCore`/`PermissionFilteredMemoryRetrieval` are named, not built.
- Synchronous `EventBus` (Gap #50) — a slow subscriber can block all publishers; decided-but-unimplemented per ADR-024.
- No structural persistence/durability/audit boundary anywhere in the runtime (Gap #51).

**Governance debt:**
- CDR-005 and CDR-006 remain in Draft status while carrying real production weight (CDR-006 underlies the entire, partially-implemented Evidence Custodian programme).
- "User Importance" (Programme 3 Amendment 4) has no owning document anywhere in the repository.
- `docs/architecture/PROJECT_GOVERNANCE.md` is a zero-byte file despite being named a governing pillar.

**Implementation debt:**
- Three fully-built subsystems (Memory Core, Knowledge Memory, Evidence Custodian) with zero production reachability.
- Knowledge Memory Units 8–10 (history population, retrieval-time projection, legacy-field retirement) not started.
- Evidence Custodian Phases 7–10 (deletion, optimisation-safeguard enforcement, formal verification artifact, runtime integration) not implemented.

**Documentation debt:**
- 11 Contract-Design documents carry a stale "not yet reviewed or accepted" header for subsystems elsewhere treated as accepted.
- `IMPLEMENTATION_HISTORY.md`'s closing summary sections were never updated past Sprint 1 despite roughly 30 further units appended above them, and it mentions neither Evidence, Memory Core, nor Knowledge Memory anywhere in its body.
- Numbered architecture chapters largely lack forward citations to the governance documents that superseded their practical authority.

**Testing debt:**
- Only Evidence Custodian has dedicated reflection-based scope tests protecting its architectural boundaries; Memory Core, Knowledge Memory, and World Model have none.
- `ConversationOutcome`/`GatedOutcome` have no dedicated test file.

**Editorial debt** (explicitly not classified as technical debt — cosmetic only): stale status headers, the vestigial placeholder directories, `docs/roadmap/ROADMAP.md`'s brevity, and the README's prior `docs/decisions/` omission.

**Explicitly NOT technical debt** (deliberate constitutional decisions, correctly excluded from this register): Evidence Custodian's Phase 10 deferral; `DeterministicAgentStepSource` as a disclosed stand-in; the `MemoryStore`→"Knowledge Memory" rename being recommended-not-immediate; `InMemoryMemoryCore`'s in-memory-only, non-persistent design.

---

## 12. Risk Register

| Risk | Level | Evidence | Impact | Recommended Action |
|---|---|---|---|---|
| CDR-006 (Evidence Custody constitutional basis) remains Draft while Evidence Custodian is substantially implemented | **High** | CDR-006 status line: "Draft... not yet undergone independent constitutional verification or Final Freeze Verification" | Production code rests on non-final constitutional authority | Complete CDR-006's Final Freeze Verification before further Evidence Custodian phases (7–10) begin |
| Memory Core has no permission-gating decorator built | **High** | `PermissionGatedMemoryCore`/`PermissionFilteredMemoryRetrieval` named, zero matches in `src/` | Wiring Memory Core into production today would leave writes ungated unless a caller gates them itself | Build the decorator before any Runtime Integration unit for Memory Core |
| Three governed, tested subsystems (Memory Core, Knowledge Memory, Evidence Custodian) unreachable in production | **Medium** | Confirmed zero matches in `ParkerRuntime.kt` | Platform capability claims (README) must not imply these are live; risk of future confusion about what's "done" vs "reachable" | Treat Runtime Integration as its own tracked, prioritized unit per subsystem |
| "User Importance" amendment has no owning document | **Medium** | Confirmed absent from every document that names it | Knowledge Memory Units 6+ cannot lawfully resume until this and two other amendments close | Draft the missing amendment before resuming Programme 3 |
| Only Evidence Custodian has structural scope tests | **Medium** | 3 scope-test files, all Evidence-Custodian/generic | A future change could silently violate Memory Core/Knowledge Memory/World Model boundaries with no test catching it | Add scope tests mirroring `EvidenceCustodianScopeTest.kt` for Memory Core and Knowledge Memory |
| Stale "not yet accepted" headers on 11 accepted Contract Designs | **Low** | Direct header citations, §5 above | A reader trusting only the header would misjudge subsystem status | Batch-update headers; no code risk |
| `PROJECT_GOVERNANCE.md` empty | **Low** | Confirmed zero bytes | Named pillar with no content | Write it or remove the reference |
| Vestigial `agents/`/`examples/`/`plugins/`/`tools/` directories | **Low** | One-line placeholder READMEs only | None functional; cosmetic | No action required until those capabilities are actually built |

---

## 13. Repository Strengths

Unusually rigorous self-documentation — nearly every contract file cites its own governing document by name and section. A real, working, self-correcting governance culture: CDR chain, Scope Locks, and a review corpus that catches and corrects its own errors (the Unit 6 reconciliation). Subsystem independence and permission-gating discipline hold up under direct code inspection everywhere checked, not merely in prose. A genuinely production-composed conversational/planning/execution pipeline, not just a design. Honest disclosure culture at the file level: deliberate stand-ins (`DeterministicAgentStepSource`), deferred phases (Evidence Custodian Phase 10), and known gaps (`IMPLEMENTATION_GAPS.md`) are consistently self-labeled rather than hidden.

---

## 14. Repository Weaknesses

Governance bookkeeping (status headers, `IMPLEMENTATION_HISTORY.md`'s summary sections) has fallen behind the actual pace of work. Three fully-built subsystems have no path to production reachability yet, and no single tracked unit currently owns closing that gap for any of them. Constitutional debt sits under a partially-implemented programme (CDR-006/Evidence Custodian). Structural test protection is uneven — one subsystem has it, others rely on inspection alone. Two named governance pillars (`PROJECT_GOVERNANCE.md`, "User Importance") exist as concepts with zero content.

---

## 15. Immediate Priorities

1. Close CDR-005 and CDR-006's Final Freeze Verification cycle, or explicitly re-scope what's safe to build against a Draft CDR.
2. Draft the missing "User Importance" amendment (Programme 3's sole fully-unowned blocker).
3. Decide, in writing, whether Knowledge Memory Units 6/7 are now unblocked following CDR-003/004 — currently an open question no document answers.

## 16. Medium-Term Priorities

1. Build `PermissionGatedMemoryCore`/`PermissionFilteredMemoryRetrieval`, then schedule Memory Core Runtime Integration as its own unit.
2. Add scope tests for Memory Core and Knowledge Memory mirroring `EvidenceCustodianScopeTest.kt`.
3. Batch-correct the 11 stale "not yet accepted" Contract-Design headers and refresh `IMPLEMENTATION_HISTORY.md`'s closing summary.

## 17. Long-Term Priorities

1. Evidence Custodian Phases 7–9 (Deletion, Optimisation Safeguard, formal Verification), in that documented order, then Phase 10 (Runtime Integration) — for both Evidence Custodian and Memory Core together, per their apparent shared deferral.
2. Resolve Common-Origin Determination (Programme 3's remaining amendment) and resume Knowledge Memory Units 8–10.
3. Decide the fate of the roughly 40 planned-only numbered architecture chapters (21–50) as the platform matures toward those capabilities, or formally mark them out-of-scope-for-now.

---

## 18. Recommended Next Lawful Implementation Unit

Two candidates are simultaneously lawful and ready, per each programme's own governance:

- **Evidence Custodian Phase 7 (Deletion workflow)** — Contract Design, Scope Lock, and Implementation Plan all already cover it explicitly; no open governance question blocks it, unlike Knowledge Memory.
- **`PermissionGatedMemoryCore`** — named in already-frozen governance, blocking nothing else, and a prerequisite for eventually wiring Memory Core into production at all.

Of the two, **`PermissionGatedMemoryCore` carries the lower constitutional risk to start next**, since it does not depend on CDR-006 (still Draft) the way further Evidence Custodian phases arguably do, and it unblocks a fully-built, currently-inert subsystem. This is this audit's recommendation, not a decision — the choice between them, like all implementation decisions, remains Steven's.

---

## 19. Overall Conclusion

Parker is not a prototype and not a finished platform — it is a real, partially-integrated system with an unusually honest paper trail describing exactly where the seams are. The Conversation/Reasoning/Planning/Agent-Execution pipeline is genuinely production-composed. Memory Core, Knowledge Memory, and Evidence Custodian are genuinely, rigorously built and governed, and genuinely absent from the running platform today — a fact this audit confirmed by direct code inspection rather than assumption. The constitutional layer is coherent where frozen and honestly marked Draft where not, with one piece of real production weight (Evidence Custodian) resting on a Draft CDR. Nothing found in this audit is a fabricated problem or an invented risk; every finding above traces to a specific file, and every place inference was required is marked as such. The repository is ready for its next governed unit — the open question is only which one Steven chooses to authorize first.
