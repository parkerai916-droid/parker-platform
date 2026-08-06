# OCR Mechanism Implementation Plan — Unit 9 (Structural Isolation Proof) — Completion Review

## Status

**Completion review of an implementation unit that is itself a verification-only unit.** No production file was created or modified in the course of Unit 9's own work. One new test file was created: `tests/contracts/OcrStructuralIsolationTest.kt`. No governance document, implementation plan, or prior test file (Units 1–8) was modified. Nothing staged, committed, or pushed at any point during this work — Steve alone performs local verification, staging, commits, and pushes.

---

## 1. Repository Baseline

Confirmed at the start of Unit 9's own work:

- **HEAD:** `6e337e0351b687504bb209684c0585071de98e49` (short `6e337e0`) — the commit containing Unit 8 ("test: verify OCR provenance disclosure").
- **Branch:** `main`.
- **Working tree:** clean.

No discrepancy against expectations, with one minor path correction: the governance-document paths named in the originating instruction (`docs/contracts/OCR_MECHANISM_CONTRACT_DESIGN.md`, `docs/governance/OCR_MECHANISM_IMPLEMENTATION_PLAN.md`) do not exist at those locations. The correct, actual paths — confirmed present and used throughout this review — are `docs/architecture/OCR_MECHANISM_CONTRACT_DESIGN.md` and `docs/implementation/OCR_MECHANISM_IMPLEMENTATION_PLAN.md`.

---

## 2. Authorities Reviewed

Read fresh, in full or by targeted section, before any test was written:

- `docs/architecture/OCR_MECHANISM_CONTRACT_DESIGN.md` — Section 12 (dependency shape: "holds no dependency of its own"), Section 13 (Repository Reuse — confirming `EvidenceArtifactId` is a reused identifier value, not a dependency on the `EvidenceCustodian` interface).
- `docs/architecture/OCR_MECHANISM_SCOPE_LOCK.md` — Section 13 (Dependency Boundary — the eight named exclusions), Section 14 (Provider Neutrality), Section 15 (Security and Resource Limits), Section 16 (Explicit Exclusions table).
- `docs/implementation/OCR_MECHANISM_IMPLEMENTATION_PLAN.md` — Unit 9 ("Structural Isolation Proof"), read verbatim in full, established as controlling for this unit.
- `src/interfaces/OcrMechanism.kt` — re-read in full; confirmed unchanged since the immediately preceding unit's own work (Unit 8).
- `src/interfaces/OcrProviderAdapter.kt` — re-read in full; confirmed unchanged.
- `src/runtime/OcrExecutionSequencer.kt` — confirmed unchanged since last read.
- All eight existing OCR test files (`OcrMechanismScopeTest.kt`, `OcrProviderAdapterScopeTest.kt`, `OcrInputContractTest.kt`, `OcrOutputModelTest.kt`, `OcrFailureHandlingTest.kt`, `OcrExecutionSequencerTest.kt`, `OcrExecutionPipelineTest.kt`, `OcrProvenanceDisclosureTest.kt`) — reviewed to confirm no existing structural assertion would be rendered stale by Unit 9's own additions.
- Also inspected, without modification, for exact type names and locations: `src/interfaces/EvidenceCustodian.kt` (`EvidenceArtifactId`, `OwnerEvidenceDeletionAuthority`), `src/interfaces/MemoryCore.kt` (`MemoryCore`, `MemoryRetrieval`, `Provenance`, `CandidateProvenance`, `CandidateDocument`), `src/interfaces/KnowledgeStore.kt` (`CandidateKnowledge`, `KnowledgeSubmission`), `src/interfaces/PermissionEngine.kt`, `src/interfaces/EvidenceIntelligence.kt`, `src/runtime/EvidenceRegistrationCoordinator.kt` (`EvidenceRegistrationOutcome`), `src/composition/ParkerRuntime.kt` and its sibling composition files, and a repository-wide search confirming the complete absence of any dependency-injection framework, service-locator pattern, or repository/database abstraction anywhere in the codebase.

---

## 3. Structural Findings (Pre-Implementation)

Before any test was written, the following was established:

1. **Complete reachable-type closure** from all eight OCR entry types (`OcrMechanism`, `OcrProviderAdapter`, `OcrExecutionSequencer`, `OcrRecognitionRequest`, `OcrRecognitionIdentity`, `OcrRecognitionSegment`, `OcrRecognitionResult`, `OcrRecognitionOutcome` and its nine sealed variants) consists of exactly: Kotlin/Java standard-library leaves (`String`, `Int`, `Double`, `ByteArray`, `List<String>`, `List<OcrRecognitionSegment>`, `java.time.Instant`); `EvidenceArtifactId` — a `@JvmInline value class` wrapping a single `String value` with only non-blank validation, declared in `EvidenceCustodian.kt` but carrying zero dependency of its own on the `EvidenceCustodian` interface it happens to share a file with; `TranscriptionFidelity` — a closed, three-value enum with no dependency; and the OCR mechanism's own 17 types.
2. **No path exists, at any depth**, to any of the ten explicitly named exclusion categories: `EvidenceCustodian`, `EvidenceRegistration`/`EvidenceRegistrationOutcome`, `MemoryCore`, `MemoryRetrieval`, `CandidateKnowledge`, `CandidateEvidenceArtifact`, `CandidateProvenance`, `EvidenceIntelligence`, `KnowledgeSubmission`, `PermissionEngine`, `OwnerEvidenceDeletionAuthority`, `ParkerRuntime`, runtime composition, a service locator, a dependency-injection framework, a repository implementation, a database/storage layer, networking, the filesystem (beyond ordinary test-scoped source-file reads used by the test suite itself), logging, or configuration.
3. **No OCR type performs** persistence, orchestration, evidence registration, memory submission, authorisation, provenance construction, provider discovery, provider selection, provider registration, retry, batching, or caching. Confirmed both by source inspection (zero relevant vocabulary in any of the three production files) and by `OcrExecutionSequencer.recognise`'s own single-expression body (`adapter.recognise(request)`), which contains no conditional branch, no `when`, and no `try`/`catch`.
4. **No genuine ambiguity was found**, and no governance conflict was found. Unit 9's own controlling Implementation Plan text is unambiguous on scope: "Outputs. A passing (or failing, and therefore blocking) structural verification result." / "Files expected to change. None in production code; new test files only." This matches exactly what was produced.

---

## 4. Files Created

- `tests/contracts/OcrStructuralIsolationTest.kt` — 22 tests, the sole deliverable of this unit.

## 5. Files Modified

None. No production file under `src/` was touched. No existing test file (Units 1–8) required modification, since Unit 9 introduced no new type, field, or variant to the OCR mechanism's own shape that would render any prior assertion stale.

---

## 6. Isolation Proofs Added

### 6.1 Dependency isolation

A single, transitively-computed reachable-type-graph proof (`collectReachableParkerTypes`): a recursive walker over every declared property, primary-constructor parameter, and function signature (parameters and return type, including generic type arguments) reachable from each OCR entry type, expanding only `parker.*`-qualified types and treating every standard-library type as a safe, non-expanded leaf. The resulting closure is asserted to be a subset of an explicit allow-list containing only the OCR mechanism's own 17 types plus two named external leaves (`EvidenceArtifactId`, `TranscriptionFidelity`). This single test structurally guarantees the absence of every excluded category at once, named or unnamed by Scope Lock Section 13, rather than checking only the specific types that document happens to enumerate.

Supplemented, for direct citation and readability, by five named per-exclusion tests covering `EvidenceCustodian`/`EvidenceIntelligence`/`OwnerEvidenceDeletionAuthority`, `MemoryCore`/`MemoryRetrieval`, `KnowledgeSubmission`, `PermissionEngine`, and the evidence-registration/provenance/candidate-record cluster (`Provenance`, `CandidateProvenance`, `CandidateEvidenceArtifact`, `CandidateKnowledge`, `CandidateDocument`, `EvidenceRegistrationOutcome`).

### 6.2 Import isolation

Exact, closed-set assertions of each of the three production files' own import lists: `OcrMechanism.kt` imports exactly `{java.time.Instant}`; `OcrProviderAdapter.kt` imports nothing; `OcrExecutionSequencer.kt` imports exactly its own four sibling OCR types. Supplemented by a defensive blocklist scan across all three files for database, networking, filesystem, logging, configuration, and dependency-injection/service-locator import fragments.

### 6.3 Type isolation

`OcrRecognitionOutcome` and its nine variants, together with every other OCR-owned type, are confirmed to be subclasses of neither `CandidateEvidenceArtifact`, `CandidateKnowledge`, `CandidateDocument`, nor `CandidateProvenance`. A further test walks each OCR type's own declared `supertypes` and confirms every entry is either `Any` or another OCR-owned type — nothing from any Parker runtime or composition type.

### 6.4 Behaviour isolation

Source-text absence (using the established comment-stripping `codeOnly()` discipline, never a naive whole-file scan) of retry, batching, caching, authorisation, registration, provenance-construction, persistence, and orchestration vocabulary across all three production files. Supplemented by a behavioural test using a counting fake adapter, confirming `OcrExecutionSequencer` performs exactly one adapter invocation per `recognise` call, with no retry, batching, or caching of a prior call's own result.

### 6.5 Composition isolation

Absence, across all three production files, of `ParkerRuntime`, `CompositionRoot`, `RuntimeCoordinator`, `ServiceLocator`, and dependency-injection annotation/vocabulary fragments (`@Inject`, `@Component`, `DependencyInjection`).

### 6.6 Provider isolation

`OcrExecutionSequencer` is confirmed, via a precise supertype-declaration regex (distinguishing a genuine `: OcrMechanism` implementation from an incidental property or parameter of that type), to be the sole class in `src/` implementing `OcrMechanism`. Zero classes in `src/` implement `OcrProviderAdapter` — confirmed still true, consistent with a concrete adapter remaining explicitly deferred future work. `OcrExecutionSequencer.recognise`'s own function body is confirmed to contain no conditional, no `when`, and no `try`, proving the sole execution path is `OcrMechanism → OcrExecutionSequencer → OcrProviderAdapter` with no alternate route.

### 6.7 Interface isolation

Exact top-level-declaration-name assertions per production file (`OcrMechanism.kt` declares exactly its own seven top-level public declarations; `OcrProviderAdapter.kt` and `OcrExecutionSequencer.kt` each declare exactly one), plus an exact nine-name assertion against `OcrRecognitionOutcome::class.sealedSubclasses`, proving no undocumented public API has appeared anywhere in the OCR mechanism's own governed surface.

---

## 7. Independent Constitutional Review

Performed as an independent audit of this unit's own work, addressing each question in turn:

- **Did any production implementation accidentally creep into this verification unit?** No. `git status --short` shows exactly one untracked file, the new test file itself. No file under `src/` was created or modified. Every addition in the new file is a `@Test` method or a private test-scoped helper function; none is reachable from, or alters, any production code path.
- **Was any governance boundary unintentionally weakened?** No. No governance document, implementation plan, or prior test file (Units 1–8) was edited. Nothing already established was loosened, relaxed, narrowed, or removed anywhere in the repository.
- **Did any previous structural safeguard become stale?** No. Unit 9 introduced no new field, type, or variant to the OCR mechanism's own shape, so no earlier assertion in any of the eight prior OCR test files required updating — and none was updated. (This is unlike Units 7 and 8, where a genuine, lawful expansion of the public shape required two prior assertions in `OcrMechanismScopeTest.kt` to be revised; no analogous revision was needed or made here.)
- **Does any test manufacture governance rather than verify governance?** One category of test deserves explicit acknowledgement here: the exact, closed-set import and top-level-declaration assertions (for example, that `OcrMechanism.kt` imports exactly `{java.time.Instant}`) assert a stronger, more specific fact than governance's own literal wording, which requires only the absence of a prohibited dependency, not a fixed, closed inventory. This was a deliberate choice to maximise precision against the compiled shape as it exists today, consistent with Unit 9's own "prove, rather than assert" framing and its own "Inputs: the compiled shape of Units 1–8." It is not an invented constitutional rule and does not bind or constrain any future unit's own governance — it verifies an observed, currently-true fact rather than asserting that fact is itself a permanent requirement.
- **Did any prohibited dependency escape the structural proofs?** No. The primary proof (Section 6.1) is a transitive closure over every declared property, constructor parameter, and function signature reachable from all eight OCR entry types, asserted to be a subset of an explicit, minimal allow-list. Nothing escapes it by construction: any future addition, whether named by Scope Lock Section 13 or not, would surface as a disallowed entry in that same test, not merely in one of the five supplementary named tests.
- **Was any defect found?** No.

**On the current absence of infrastructure elsewhere in Parker.** An earlier, informal statement of this review — that no dependency-injection framework, service locator, or repository/database layer exists anywhere in this repository — is, and should be understood strictly as, an observation about the codebase's present state, never as the basis of the isolation guarantee itself. The actual guarantee (Section 6.1) reasons only about the OCR mechanism's own declared dependencies: it asserts that everything reachable from `OcrMechanism`, `OcrProviderAdapter`, `OcrExecutionSequencer`, and the OCR data/outcome types is confined to the OCR mechanism's own types plus two named external leaves. That assertion is authority-agnostic about what infrastructure exists, or comes to exist, anywhere else in Parker. Should Parker later adopt a dependency-injection framework, a service locator, or any other infrastructure elsewhere in the platform, this proof continues to hold unchanged — it would fail only if the OCR mechanism itself began depending on such a thing, which is precisely the isolation property this unit exists to guarantee. The supplementary import-fragment blocklist, which does name concrete package fragments (`dagger`, `koin`, `guice`, and similar), is comparatively weaker and more convention-based; the reachable-type closure proof is the test that carries the actual, durable architectural guarantee.

---

## 8. Defects Found

None.

## 9. Defects Corrected

None — no defect was found in either the implementation phase or the independent review phase, so no correction was required or made.

---

## 10. Targeted Verification

Command:

```
./gradlew test \
  --tests "*OcrMechanism*" \
  --tests "*OcrStructuralIsolationTest"
```

Result: **BUILD SUCCESSFUL.** Re-run fresh at the time of this review (not recalled from an earlier turn), with no failure and no error.

## 11. Full Repository Verification

Command:

```
./gradlew clean test
```

Result: **BUILD SUCCESSFUL** in 42s.

- **Total tests:** 1643
- **Failures:** 0
- **Errors:** 0
- **Skipped:** 5 — all five within `TikaEvidenceExtractorTest` (Evidence Processing's own, pre-existing test class; unrelated to the OCR mechanism and unrelated to this unit's own work).
- **New compiler warnings introduced by Unit 9:** none. The 19 warnings emitted by this run (`Check for instance is always 'true'`; `No cast needed`; one unused-parameter warning; one unnecessary non-null-assertion warning) all occur in pre-existing, unrelated files — `KnowledgeLifecycleEventTest.kt`, `MemoryCoreInterfacesTest.kt`, `DefaultLocalTextChannelTest.kt`, `DefaultPlanCandidateGeneratorTest.kt`, `EvidenceExtractionCoordinatorTest.kt`, `InMemoryCommunicationIntakeTest.kt`, `InMemoryIdentityServiceTest.kt`, `InMemoryKnowledgeStoreTest.kt`, `InMemoryMemoryCoreTest.kt`, `InMemoryToolRegistryTest.kt`, `InMemoryWorldModelTest.kt` — none in `OcrStructuralIsolationTest.kt` or any other OCR file.

---

## 12. Programme Boundary Confirmations

- Unit 10 (Evidence-Intelligence-Side Contract Sufficiency) has **not** begun.
- Unit 11 (Verification) has **not** begun.
- Unit 12 (Runtime Composition) has **not** begun.
- No provider was selected. No runtime composition was added. No governance document was changed.

## 13. Git Confirmations

- Nothing was staged during this work.
- Nothing was committed during this work.
- Nothing was pushed during this work.

## 14. Final Git Status

```
$ git status --short
?? tests/contracts/OcrStructuralIsolationTest.kt
```

Confirmed: this is the only uncommitted file in the repository at the conclusion of this review, and it is exactly the expected Unit 9 deliverable.

---

## 15. Constitutional Verdict

**READY FOR ACCEPTANCE.**

Unit 9's own Purpose, Responsibilities, Constitutional constraints, Files-expected-to-change, Files-explicitly-prohibited, Verification requirements, and Completion criteria are each satisfied exactly as written. No production file was created or modified. No prior unit's own governed shape or test coverage was altered or weakened. The structural isolation of the OCR mechanism from Evidence Custodian, Memory Core, Knowledge Submission, the Permission Engine, Evidence Intelligence's own public result handling, any runtime conversation component, any reporting mechanism, and Docling — Scope Lock Section 13's own eight exclusions — is proven, not merely asserted, by a transitive reachable-type closure that is authority-agnostic about Parker's current or future infrastructure elsewhere. The full repository test suite passes without failure, error, or new warning attributable to this unit.

## 16. Recommendation

Accept Unit 9 as complete. Proceed, when Steve is ready, with Steve's own normal local verification, staging, commit, and push of `tests/contracts/OcrStructuralIsolationTest.kt`. No corrective action is required before proceeding to Unit 10.
