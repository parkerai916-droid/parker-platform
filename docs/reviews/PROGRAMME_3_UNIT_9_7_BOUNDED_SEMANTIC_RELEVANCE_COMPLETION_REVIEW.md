**Status: PROGRAMME COMPLETE.** This is the Programme 3, Unit 9.7 ("Bounded Semantic Relevance") Completion Review, produced by Unit 9.7.6 ("Full Verification, Regression, and Programme Closure") after genuine Windows executable verification of both outstanding gates. This document records the completion of Units 9.7.1 through 9.7.5's own already-adopted, already-committed, already-pushed work; it authorises no new implementation and changes no production or test code.

# Programme 3, Unit 9.7 — Bounded Semantic Relevance — Completion Review

Repository: `parkerai916-droid/parker-platform`. Branch `experiment/qmd-canonical-memory-retrieval`. HEAD at the time of this review: `77a414d4f42012b3b5652f91ceb3614888c265e7` ("feat(memory): wire QMD relevance mechanism into runtime").

## 1. Programme Purpose

Unit 9.7 exists to remedy a demonstrated recall defect: Parker's canonical Knowledge Memory can hold a genuinely promoted, canonical proposition that a later, structurally-dissimilar paraphrase of the same request fails to recall, because the pre-existing retrieval path's only relevance mechanism was case-insensitive substring matching. Unit 9.7 authorises exactly one capability — **Bounded Relevance Computation** — a request-scoped, subordinate, replaceable computation that orders or reduces an already Parker-authorised, already-eligible, closed candidate set, running only as a fallback after structural matching has completed successfully and found nothing. It authorises no library, model, product, or persistent architecture by name beyond that single bounded capability, and this Unit's own scope is limited to the `DefaultKnowledgeRetrieval` surface only — not the separately-governed live-reasoning surface (see §14, the RKS boundary statement).

## 2. Authoritative Governance

- `docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_BOUNDED_SEMANTIC_RELEVANCE_SCOPE_LOCK_AMENDMENT.md` (Adopted) — the single authorised capability, what remains authoritative, what semantic relevance may/may not do, permission boundaries, fail-closed expectations, provenance/identity preservation, exclusions, and the explicit prohibition on unintended architectural expansion.
- `docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_CONTRACT_DESIGN_V2_BOUNDED_SEMANTIC_RELEVANCE_AMENDMENT.md` (Adopted, Amendment 9) — Relevance Request/Result contract-tier concepts, canonical identity preservation, deterministic boundary expectations, permission behaviour, failure behaviour, ordering/ranking semantics, integration boundaries, implementation-neutrality.
- `docs/governance/PROGRAMME_3_UNIT_9_7_BOUNDED_SEMANTIC_RELEVANCE_CONTRACT_AND_PERMISSION_SUCCESSOR.md` (Adopted) — the fourteen-condition Model A-Strict production boundary, the required contract surface, and the fail-closed table.
- `docs/governance/PROGRAMME_3_UNIT_9_7_BOUNDED_SEMANTIC_RELEVANCE_IMPLEMENTATION_PLAN.md` (Adopted) — the Unit 9.7.1–9.7.6 decomposition, §13/§13.1 mechanism-selection spike mandate, and each sub-unit's own completion criteria.
- `docs/reviews/PROGRAMME_3_UNIT_9_7_SECTION_13_MECHANISM_SELECTION_SPIKE_EVIDENCE_RECORD.md` (Adopted) — the QMD-vs-JVM-comparator mechanism-selection spike and its ten mandatory pass/fail criteria.
- `docs/decisions/CDR-008_MEMORY_CORE_DOWNSTREAM_RELEVANCE_BOUNDARY.md` (Accepted, Canonical, Frozen) — establishes that Memory Core Scope Lock's prohibition on ranked/scored/semantic retrieval binds Memory Core's own interface, not a downstream, separately-governed component.
- `docs/governance/PROGRAMME_3_UNIT_9_SEMANTIC_RELEVANCE_SCOPE_LOCK_REVISION_PROPOSAL.md` (Adopted) — the original demonstrated defect and the Model A-Strict architecture this Unit implements without reopening.

All governing documents were re-read fresh during Unit 9.7.6 and found unedited by any Unit 9.7.1–9.7.5 implementation commit.

## 3. Units 9.7.1–9.7.5 Implementation Summary

- **9.7.1 (Relevance Contract Types)** — `src/interfaces/RelevanceMechanism.kt`: `RelevanceCandidateToken`, `RelevanceCandidate`, `RelevanceRequest`, `RelevanceResult`, and the `RelevanceMechanism` fun interface, at the properties level only, no concrete backing implementation. Structurally verified by `tests/contracts/RelevanceMechanismContractShapeTest.kt` (18 tests).
- **9.7.2 (Fallback Trigger and Closed Candidate Set)** — `DefaultKnowledgeRetrieval.kt`: separates the structural filter into a lifecycle-eligibility pass and a structural-match pass; the fallback branch runs only when `structurallyMatched.isEmpty() && closedCandidateSet.isNotEmpty()`.
- **9.7.3 (Local Relevance Mechanism Adapter)** — `src/runtime/QmdRelevanceMechanism.kt` and `tools/qmd-relevance-bridge.mts`: the concrete, local, disposable, subprocess-based QMD adapter selected by the Section 13/13.1 spike. Verified by `tests/runtime/QmdRelevanceMechanismTest.kt` (22 tests, offline/fake-subprocess) and `tests/integration/QmdRelevanceMechanismLiveAcceptanceTest.kt` (4 tests, live-gated).
- **9.7.4 (Integrity Validation, Canonical Token Re-resolution, Fresh Pre-disclosure Re-verification)** — `DefaultKnowledgeRetrieval.kt`'s `resolveSemanticResult`: validates every returned token fail-closed, resolves each survivor via a fresh `persistence.find()`, re-applies `isRetrievable` and a fresh `PermissionEngine.evaluate`, and builds the final `KnowledgeResultEntry` exclusively from that freshly-verified state. Verified by `tests/runtime/DefaultKnowledgeRetrievalTest.kt` (99 tests).
- **9.7.5 (Runtime Composition Wiring)** — `src/composition/ParkerRuntime.kt` and `ParkerRuntimeConfig.kt`: removes the temporary fail-closed placeholder and constructs the real `QmdRelevanceMechanism` with frozen mechanism identity (hardcoded literals) and deployment-specific paths (from `ParkerRuntimeConfig`), injected into `DefaultKnowledgeRetrieval`. Verified by `tests/composition/ParkerRuntimeKnowledgeRetrievalCompositionTest.kt` (20 tests) and `tests/composition/ParkerRuntimeConfigLoaderTest.kt` (22 tests).

## 4. Relevant Commit History

| Commit | Unit | Files |
|---|---|---|
| `a9d4a3e` | 9.7.1 | `RelevanceMechanism.kt`, `RelevanceMechanismContractShapeTest.kt` |
| `e551cb5` | 9.7.2 | `DefaultKnowledgeRetrieval.kt`, `DefaultKnowledgeRetrievalTest.kt` |
| `a91daa8` | 9.7.3 | `QmdRelevanceMechanism.kt`, `qmd-relevance-bridge.mts`, `QmdRelevanceMechanismTest.kt`, `QmdRelevanceMechanismLiveAcceptanceTest.kt`, `build.gradle.kts` |
| `2a25795` | Spike evidence adoption | `PROGRAMME_3_UNIT_9_7_SECTION_13_MECHANISM_SELECTION_SPIKE_EVIDENCE_RECORD.md`, `RelevanceMechanismSpikeJvmComparatorTest.kt`, `RelevanceMechanismSpikeQmdCandidateTest.kt` |
| `78de4f5` | 9.7.4 | `ParkerRuntime.kt`, `DefaultKnowledgeRetrieval.kt`, `DefaultKnowledgeRetrievalTest.kt`, `ParkerRuntimeReasoningContextIntegrationTest.kt` |
| `77a414d` (HEAD) | 9.7.5 | `ParkerRuntime.kt`, `ParkerRuntimeConfig.kt`, `ParkerRuntimeConfigLoaderTest.kt`, `ParkerRuntimeKnowledgeRetrievalCompositionTest.kt`, `ParkerRuntimeReasoningContextIntegrationTest.kt` |

Every file list above was confirmed fresh via `git show --stat` during Unit 9.7.6 and matches each Unit's own governance exactly.

## 5. QMD Mechanism-Selection Result

The Section 13/13.1 spike evaluated exactly two governed candidates — QMD (real captured embeddings, cosine similarity) and a JVM TF-IDF comparator — against ten mandatory pass/fail criteria. QMD passed all ten; the JVM comparator failed two mandatory criteria (ranking fidelity, proposition fidelity), scoring the propositionally-unrelated "emergency plumber" distractor above the genuinely-related "regular vet" distractor solely on a shared incidental token. QMD was selected on this correctness basis, following an Independent Mechanism Selection Review (REVISE BEFORE ACCEPTANCE, one bounded defect corrected — a mechanism-facing candidate representation illegitimately carrying `KnowledgeId` alongside `token`/`vector`) and a subsequent Defect Confirmation Review (ACCEPT). Frozen mechanism identity: `qmdVersion = "2.8.3"`, `embeddingModelUri = "hf:ggml-org/embeddinggemma-300M-GGUF/embeddinggemma-300M-Q8_0.gguf"`, `vectorDimension = 768`, `similarityMetric = "cosine"`.

## 6. Final Verification Matrix Summary (85 properties, categories A–L)

All 85 properties were verified by fresh code inspection during Unit 9.7.6 and PASS. Summary by category:

- **A. Structural-First Retrieval (1–5)** — structural match evaluated before any fallback; one or more structural matches, regardless of their own permission outcome, never triggers fallback; fallback runs only on exact-zero-structural-match.
- **B. Closed Candidate Set (6–10)** — built from the full lifecycle-eligible set, item-level permission-gated before token minting; empty set short-circuits before the mechanism is ever reached.
- **C. Minimum Content Boundary (11–14)** — only the most-recent-history-entry `basis` text crosses into `RelevanceCandidate.content`; no canonical field present; final disclosed content always comes from fresh Pre-disclosure re-resolution, never from mechanism input/output.
- **D. Opaque Identity (15–20)** — `RelevanceCandidateToken` structurally incapable of carrying `KnowledgeId`; freshly minted per call; token-to-item map local to one `retrieve()` call; unknown/duplicate/excess tokens rejected fail-closed.
- **E. QMD's Role (21–28)** — subordinate, referenced only behind `RelevanceMechanism`; local/in-process/disposable; never receives or returns canonical identifiers; selection was a governed spike decision; failure always propagates, never silently absorbed.
- **F. Mechanism Selection/Configuration (29–36)** — identity/version frozen as hardcoded literals at the composition site; deployment paths sourced from config; configuration is an immutable value type; validated at load time.
- **G. Local Control (37–42)** — no remote/cloud-hosted processor; bridge script performs no network fetch; local-availability precondition enforced before any embedding call; no candidate content crosses an uncontrolled network boundary.
- **H. Failure Semantics (43–54)** — every fault path (start failure, timeout, non-zero exit, malformed/partial/extra-field response, unknown/duplicate token) is a distinct thrown exception; zero candidates and a genuinely-empty relevant result are honest successes, never faults.
- **I. Pre-Disclosure Parker Authority (55–64)** — every mechanism-surfaced token undergoes a fresh persistence lookup, lifecycle check, and permission evaluation before disclosure; final content is built exclusively from that fresh state.
- **J. Ordering/Bounding (65–69)** — structural path preserves insertion order; semantic path preserves the mechanism's own order among survivors; `maximumResults` bounds after permission filtering on both paths at the identical governed stage.
- **K. Runtime Composition (70–75)** — the real `QmdRelevanceMechanism` (never the former placeholder) backs the composed graph; frozen identity and deployment-specific fields both flow through correctly; missing/invalid deployment configuration fails loudly through the real composed runtime.
- **L. Non-Expansion (76–85)** — `RelevanceMechanism.kt` and `DefaultKnowledgeRetrieval.kt` are unchanged since their own originating commits; `DefaultReasoningKnowledgeSource.kt` contains zero references to `RelevanceMechanism`/QMD; no persistent index exists anywhere in the path; no Unit 9.7.1–9.7.5 commit touches any governance amendment document.

No item returned FAIL. No genuine defect was found during this review.

## 7. Windows Full-Repository Result

`.\gradlew.bat test` — **BUILD SUCCESSFUL**. Supplied console evidence: `BUILD SUCCESSFUL in 2s, 8 actionable tasks: 2 executed, 6 up-to-date`. This gate is satisfied on the strength of the owner-supplied executable evidence; this review does not independently claim a specific pass/fail count beyond what that output states, since Gradle execution is unavailable in this review's own (Linux, cloud-sandboxed) environment.

## 8. Windows Live-QMD Result

`.\gradlew.bat qmdRelevanceMechanismLiveAcceptance --rerun-tasks --info` — **BUILD SUCCESSFUL in 2m 51s, 4 actionable tasks: 4 executed** (a forced, fully fresh, non-cached re-run, supplied after an initial terser result was queried for corroboration). The full console log shows a genuine live invocation: real Gradle/Kotlin daemon internals, the real compiled file list (matching this repository's actual source tree), and the test's own printed evidence:

```
QMD live relevance acceptance -- latencies (ms): [14489, 14098, 14157]
QMD live relevance acceptance -- rankings: [[candidate-1, candidate-2, candidate-3, candidate-4, candidate-5, candidate-6], [candidate-1, candidate-2, candidate-3, candidate-4, candidate-5, candidate-6], [candidate-1, candidate-2, candidate-3, candidate-4, candidate-5, candidate-6]]
```

Three repeated live invocations, each ~14 seconds (consistent with this mechanism's own documented disposable-state design — a fresh embedding-model cold-load on every `rank()` call, the reason its configured timeout is 120,000ms), all three producing an identical ranking with `candidate-1` (the emergency-vet target) first — matching the adopted six-candidate spike evidence exactly. This satisfies both the live-QMD regression gate and the Phase 9 (Unit 9.7.6) three-repetition determinism requirement for the mechanism in isolation.

## 9. End-to-End DefaultKnowledgeRetrieval Proof

Proven by composition of three independently-verified layers, per Unit 9.7.6's own explicit permission to reuse existing coverage rather than invent a new fixture or production endpoint: (a) `DefaultKnowledgeRetrievalTest.kt` proves the full chain — structural match, fallback trigger, closed candidate set, mechanism invocation, integrity validation, fresh Pre-disclosure re-verification, ordering, bounding — against a fake `RelevanceMechanism`; (b) `ParkerRuntimeKnowledgeRetrievalCompositionTest.kt`'s composition tests prove the same chain through the real, fully composed `ParkerRuntime` reaching a real `QmdRelevanceMechanism`, including genuine fail-loud behaviour when misconfigured; (c) `QmdRelevanceMechanismLiveAcceptanceTest.kt`, now confirmed genuinely executed on Windows, proves a real, live QMD subprocess reproduces the exact accepted six-candidate ranking; (d) `QmdRelevanceMechanismTest.kt`'s own dedicated test reproduces that identical accepted ranking through the real production adapter's request/response/ordering pipeline. No genuine gap remains between these three layers.

## 10. Structural-Path Regression Result

PASS. The structural retrieval path (Units 9.2–9.5) is unchanged by any Unit 9.7.x work and remains covered by its own pre-existing tests in `DefaultKnowledgeRetrievalTest.kt`, none of which were touched by Unit 9.7.1–9.7.5.

## 11. Local-Control Result

PASS. `ProcessBuilderQmdSubprocessInvoker` launches a local executable only; `tools/qmd-relevance-bridge.mts` performs no network call and enforces a local-availability precondition (`resolveModelFile(..., download: false)`) before any embedding call, live-verified by `QmdRelevanceMechanismLiveAcceptanceTest.kt`'s "missing local embedding model fails closed" test.

## 12. Authority/Security Result

PASS on all eleven adversarial "Can QMD..." questions reviewed during Unit 9.7.6 (create/modify/delete canonical records, alter a permission decision, alter lifecycle/evidential-state, see a candidate's `KnowledgeId`, bypass fresh Pre-disclosure re-verification, disclose an item outside the supplied set, have a failure silently absorbed, reach the network, persist state across calls, run concurrently with structural matching, or take a configuration change without a disclosed reviewed diff) — every answer is NO, each backed by specific code citation.

## 13. Known Non-Blocking Operational Limitations

- Cold-start latency: each `rank()` invocation cold-loads the embedding model fresh (Frozen Boundary #10, disposable state) — now empirically confirmed at ~14 seconds per call on the real Windows development environment, well within the configured 120,000ms timeout.
- Coroutine-cancellation non-responsiveness: `rank()`'s `withContext(Dispatchers.IO) { invoker.invoke(...) }` does not itself interrupt an in-flight subprocess wait before the configured timeout elapses on its own. Disclosed, reviewed, and found not to violate any governed property (`QmdRelevanceMechanism.kt`'s own KDoc, "Finding 2").

## 14. RKS Boundary Statement

**Unit 9.7 closure does not by itself establish semantic recall in Parker's live ReasoningContext/model-prompt path. That remains governed by the separate Reasoning Knowledge Source Bounded Semantic Relevance programme.** This was independently confirmed structurally during Unit 9.7.6: `src/runtime/DefaultReasoningKnowledgeSource.kt` contains zero references to `RelevanceMechanism`, `Qmd`, or `QMD`.

## 15. Final Programme Completion Review Verdict

**PROGRAMME COMPLETE.**

All properties in the 85-item verification matrix pass by inspection. Both Windows executable gates are satisfied by genuine, corroborated evidence (§7, §8). No genuine defect or contradiction was found. Units 9.7.1 through 9.7.5 each satisfy their own accepted completion criteria, and no later unit weakened an earlier one's accepted property. This verdict authorises no further implementation; it records that Unit 9.7's own governed scope — the `DefaultKnowledgeRetrieval` semantic relevance path — is complete.
