# OCR Mechanism Implementation Plan — Unit 10 (Evidence-Intelligence-Side Contract Sufficiency) — Completion Review

## Status

**Completion review of an implementation unit that is itself a constitutional review and verification unit.** No production file was created or modified in the course of Unit 10's own work. One new test file was created: `tests/contracts/OcrEvidenceIntelligenceContractSufficiencyTest.kt`. No governance document, implementation plan, or prior test file (Units 1–9) was modified. No real Evidence Intelligence source file was touched or created. Nothing staged, committed, or pushed at any point during this work — Steve alone performs local verification, staging, commits, and pushes.

---

## 1. Repository Baseline

Confirmed at the start of Unit 10's own work:

- **HEAD:** `9cea98648e14c0b0474cadba1584d0530b154e6` (short `9cea986`) — the commit containing Unit 9 ("test: prove OCR structural isolation").
- **Branch:** `main`.
- **Working tree:** clean.

No discrepancy against expectations.

---

## 2. Authorities Reviewed

Read fresh, in full or by targeted section, before any test was written:

- `docs/architecture/OCR_MECHANISM_CONTRACT_DESIGN.md` — the OCR mechanism's own responsibilities, non-responsibilities, and shape, as the fixed contract Evidence Intelligence must be able to consume without any addition.
- `docs/architecture/OCR_MECHANISM_SCOPE_LOCK.md` — Section 11 (Output-Quality Validation Boundary — confirming validation policy is never OCR's own to supply).
- `docs/implementation/OCR_MECHANISM_IMPLEMENTATION_PLAN.md` — Unit 10 ("Evidence-Intelligence-Side Contract Sufficiency"), read verbatim in full and established as controlling.
- `docs/architecture/EVIDENCE_INTELLIGENCE_CONTRACT_DESIGN.md` — Amendment 2 (the OCR mechanism dependency, its own explicit non-authorisations); Section 5 (Outputs — the transcription-fidelity / mixed-fidelity requirement, "must make which of the three it is — or which portions are which — apparent"); Section 8 (Provenance Model — Evidence Intelligence creates no independent provenance model of its own); Section 9 (Confidence Model — analytical confidence is transient, never durable, never an `EvidentialState`); Section 11 (Failure Model — the controlling principle that a fault from an internally invoked capability, named there for `ReasoningProvider`, "is Evidence Intelligence's own concrete implementation's concern to signal, by whatever mechanism it chooses, outside the `EvidenceAnalysisResult` sealed type"); Section 12 (Dependency Model — the OCR mechanism's own dependency row, added by Amendment 2, structurally parallel to the `ReasoningProvider` row).
- `src/interfaces/OcrMechanism.kt`, `src/interfaces/OcrProviderAdapter.kt`, `src/runtime/OcrExecutionSequencer.kt` — re-read fresh; confirmed unchanged since Unit 9's own work.
- All nine existing OCR test files (`OcrMechanismScopeTest.kt`, `OcrProviderAdapterScopeTest.kt`, `OcrInputContractTest.kt`, `OcrOutputModelTest.kt`, `OcrFailureHandlingTest.kt`, `OcrExecutionSequencerTest.kt`, `OcrExecutionPipelineTest.kt`, `OcrProvenanceDisclosureTest.kt`, `OcrStructuralIsolationTest.kt`) — reviewed to confirm no existing assertion would be rendered stale by this unit's own work, and to confirm no duplication of Unit 9's own exhaustive structural-isolation proof.

---

## 3. Constitutional Findings

Before any test was written, the controlling question was resolved: **can Evidence Intelligence perform every OCR-related decision required by its own contract using only the public OCR mechanism contract, as it exists after Units 1–9?**

Evidence Intelligence Contract Design Section 11 supplies the governing principle directly, restated from its own identical treatment of a faulting `ReasoningProvider` invocation: any fault or outcome from an internally invoked capability is "Evidence Intelligence's own concrete implementation's concern to signal, by whatever mechanism it chooses, outside the `EvidenceAnalysisResult` sealed type." Section 12's own Dependency Model places the OCR mechanism in exactly that same position, structurally parallel to `ReasoningProvider`. Evidence Intelligence therefore needs only to be able to *distinguish* each of the OCR mechanism's own outcomes — never a new field, type, or representation contributed by OCR itself.

Working through every required decision named by this unit's own governing instruction, against Units 1, 6, 7, and 8's already-frozen shape:

| Required decision | Required information | Source | Already exposed | Missing |
| --- | --- | --- | --- | --- |
| Transcription fidelity | Which of VERBATIM / NORMALISED / INFERRED_RECONSTRUCTION | `OcrRecognitionResult.fidelity` (Unit 1) | Yes | None |
| Mixed fidelity | Which portions are which, within one recognition | `OcrRecognitionResult.segments[].fidelity` (Unit 6) | Yes | None |
| Page-aligned output | Page-level breakdown, where meaningful | `OcrRecognitionSegment.pageNumber`, non-decreasing order (Unit 6) | Yes | None |
| Confidence | A transient technical confidence signal | `OcrRecognitionResult.confidence` (Unit 1) | Yes | None |
| Warnings | Non-fatal conditions, in order | `OcrRecognitionResult.warnings` (Unit 1) | Yes | None |
| Mechanism identity | Provider-neutral identity of the mechanism | `OcrRecognitionResult.identity.mechanismIdentity` (Unit 1) | Yes | None |
| Mechanism version | Reproducibility fact, where available | `OcrRecognitionResult.identity.mechanismVersion` (Unit 1) | Yes | None |
| Processing timestamp | When recognition occurred | `OcrRecognitionResult.recognisedAt` (Unit 1) | Yes | None |
| Provenance-supporting disclosure | Facts sufficient for `extractedFrom`/`derivedFrom` | `OcrRecognitionRequest.sourceEvidenceId` (held by the caller) plus `OcrRecognitionResult`'s own identity/timestamp/warnings/segments (Units 1, 6, 8) | Yes | None |
| Every failure outcome | Distinguishable, non-collapsible representation | `OcrRecognitionOutcome`'s own nine sealed variants (Unit 7) | Yes | None |
| Partial output | A distinguishable case preserving the actual partial text | `OcrRecognitionOutcome.PartialOrDegradedOutput(partialResult, reason)` (Unit 7) | Yes | None |
| Unsupported input | A distinguishable operational-failure case | `OcrRecognitionOutcome.UnsupportedOrInaccessibleInput` (Unit 7) | Yes | None |
| Dependency failure | A distinguishable, anticipated operational-limitation case | `OcrRecognitionOutcome.ProcessingOrDependencyFailure` (Unit 7) | Yes | None |
| Implementation fault | A distinguishable, unexpected-defect case, or ordinary exception propagation | `OcrRecognitionOutcome.GenuineImplementationFault`, or Unit 3's own unmodified fault-propagation discipline | Yes | None |
| Validation boundary | Raw material sufficient for Evidence Intelligence's own future policy, never OCR's own policy | `OcrRecognitionResult.confidence`/`.fidelity`/`.warnings`; `OcrRecognitionOutcome.ValidationRejection` reserved, never constructed by OCR (Units 1, 7) | Yes | None (a validation *policy* is correctly absent, and must remain absent, from OCR's own contract) |
| No recognisable content | A distinguishable, honest disclosure case | `OcrRecognitionOutcome.NoRecognisableContent` (Unit 7) | Yes | None |

**Conclusion: no genuine omission was found.** Every required Evidence Intelligence decision is already satisfiable using only the OCR mechanism's existing, frozen public contract. Per this unit's own governing constitutional rule, no production code was modified.

---

## 4. Structural Findings

- The OCR mechanism's own nine-variant `OcrRecognitionOutcome` sealed hierarchy remains exhaustively matchable by a caller-side `when` expression with no `else` branch — demonstrated directly in the new test file, not merely asserted.
- `OcrRecognitionRequest.sourceEvidenceId` remains sufficient, unduplicated, source-identity information: the caller already holds it from constructing the request itself, exactly as Unit 8's own review already established and as this unit's own tests independently re-confirm from the caller's side.
- Sufficient raw material exists for a caller to construct, entirely outside OCR's own contract: a hypothetical `CandidateProvenance` (mirroring `OcrProvenanceDisclosureTest.kt`'s own Unit 8 proof), a deterministic downstream SHA-256 integrity digest computed over `recognisedText` alone, a hypothetical downstream validation decision (explicitly disclosed as an illustrative, non-authoritative stand-in), and a hypothetical `CandidateEvidenceArtifact` sufficient in shape for `EvidenceRegistrationCoordinator.register`'s own eventual input — without this unit invoking the real coordinator or any of its own dependencies.
- No OCR-owned type (`OcrMechanism`, `OcrProviderAdapter`, `OcrRecognitionRequest`, `OcrRecognitionIdentity`, `OcrRecognitionSegment`, `OcrRecognitionResult`, `OcrRecognitionOutcome`, or any of its nine variants) references `CandidateProvenance`, `Provenance`, `CandidateEvidenceArtifact`, `MemoryCore`, or `PermissionEngine` — confirmed by a compact, Unit-10-scoped reflection check, deliberately non-duplicative of `OcrStructuralIsolationTest.kt`'s own exhaustive, transitively-computed reachable-type-graph proof (Unit 9), which this unit's own test cites directly rather than re-deriving.

---

## 5. Files Created

- `tests/contracts/OcrEvidenceIntelligenceContractSufficiencyTest.kt` — 13 tests, the sole deliverable of this unit.

## 6. Files Modified

None. No production file under `src/` was touched. No existing test file (Units 1–9) required modification. No real Evidence Intelligence source file was touched or created.

---

## 7. Tests Added

All thirteen tests exercise the OCR mechanism exclusively through a hand-written fake ("a future Evidence Intelligence caller"), private and test-file-scoped, never referenced from `src/`:

1. All three transcription-fidelity levels are distinguishable from a `Recognised` outcome.
2. Mixed fidelity within a single recognition is distinguishable at the segment level (Evidence Intelligence Contract Design Section 5's own requirement).
3. Page ordering is readable, in order, from a page-aligned recognition.
4. "No page ordering available" is distinguishable from a genuine, populated ordering — never fabricated as page one.
5. All nine `OcrRecognitionOutcome` variants are exhaustively distinguishable via a no-`else` `when` expression.
6. Partial output is identifiable as distinct from a clean `Recognised`, while the preserved partial text remains readable.
7. Whether recognised text exists at all is determinable without fabricating output for the seven outcome kinds that carry none.
8. A hypothetical downstream `CandidateProvenance` is constructible entirely from the request the caller itself holds and the result OCR discloses.
9. A deterministic downstream integrity hash is computable from `recognisedText` alone.
10. A hypothetical downstream validation decision is computable from OCR's own disclosed confidence and fidelity, explicitly disclosed as an illustrative, non-authoritative stand-in never referenced by production code.
11. A hypothetical downstream `CandidateEvidenceArtifact` is constructible from `recognisedText` alone — proving shape-sufficiency for `EvidenceRegistrationCoordinator.register`'s own eventual input, without invoking the real coordinator.
12. Warnings, confidence, timestamp, and mechanism identity/version all reach the caller unchanged.
13. No OCR-owned type references `CandidateProvenance`, `Provenance`, `CandidateEvidenceArtifact`, `MemoryCore`, or `PermissionEngine` — confirming every downstream responsibility demonstrated above belongs to the caller, never to OCR itself.

---

## 8. Independent Constitutional Review

Performed as an independent audit of this unit's own work, addressing each question in turn:

- **Did Unit 10 accidentally implement Evidence Intelligence?** No. The fake caller (`fakeEvidenceIntelligenceCaller`) and every helper function are private and scoped entirely to the new test file; no `EvidenceAnalysisRequest`, `EvidenceAnalysisResult`, or any other real Evidence Intelligence type was touched, referenced, or created.
- **Did any runtime behaviour creep into this verification unit?** No. Zero production files were touched; the fake adapter used throughout is a private test-scoped class, never a concrete provider, never wired into any runtime composition.
- **Did any production OCR interface change without constitutional necessity?** No. Nothing changed at all — the pre-implementation constitutional review (Section 3, above) found no omission, and the constitutional rule governing this unit ("if everything required already exists, do not modify production code") was therefore followed exactly.
- **Did any previous structural safeguard become stale?** No. Unit 10 introduced no new field, type, or variant to the OCR mechanism's own shape, so no prior assertion in any of the nine earlier OCR test files required updating, and none was updated.
- **Did any test accidentally manufacture governance rather than verify it?** One test deserves explicit acknowledgement: `hypotheticalDownstreamValidation`, a private test-scoped function, picks an illustrative confidence threshold and a fidelity exclusion as a stand-in for a *future*, Parker-owned validation policy. Its own KDoc explicitly discloses that this threshold is "an arbitrary illustration, not a governance decision, and is never referenced by any production file" — consistent with, and not a repeat of, the identical caveat Unit 8 already established for its own digest-computation demonstration. No test asserts that this specific threshold is, or should become, OCR's or Evidence Intelligence's own governed policy.
- **Does the OCR contract remain provider-neutral?** Yes. No concrete provider was named, selected, or implied anywhere in this work; the fake adapter is a hand-written test double, structurally indistinguishable in kind from every other fake used throughout Units 1–9.
- **Does the OCR contract remain runtime-neutral?** Yes. `OcrExecutionSequencer` was used exactly as Unit 3 already built it, unmodified; no composition-root or dependency-injection concept was introduced.
- **Does the OCR contract remain evidence-registration-neutral?** Yes. The hypothetical `CandidateEvidenceArtifact` construction occurs entirely within the test file; the final structural test in this unit's own suite confirms no OCR-owned type references it, `CandidateProvenance`, `Provenance`, `MemoryCore`, or `PermissionEngine`.

**Defect found:** one. A redundant, logically-vacuous assertion (`assertFalse(outcome is Recognised, ...)`) immediately followed a smart-casting `assertTrue(outcome is PartialOrDegradedOutput, ...)` — since the prior assertion, once it passes, already structurally guarantees the value cannot simultaneously be a different sealed variant, the following `assertFalse` could never meaningfully fail. This also produced a "No cast needed" compiler warning on an explicit cast made unnecessary by the same smart-cast.

**Defect corrected:** the redundant assertion and the now-unnecessary explicit cast were both removed. The test was re-run and confirmed to compile cleanly, with no warning, and to still pass, preserving its own meaningful assertion (that the preserved partial text is readable from the smart-cast value).

---

## 9. Targeted Verification

Command:

```
./gradlew test --tests "*OcrEvidenceIntelligenceContractSufficiencyTest"
```

Result: **BUILD SUCCESSFUL.** Run fresh both before and after the correction described in Section 8; the post-correction run produced no compiler warning of any kind.

## 10. Full Repository Verification

Command:

```
./gradlew clean test
```

Result: **BUILD SUCCESSFUL** in 42s, run fresh after the correction described in Section 8.

- No failure, no error attributable to this unit's own work.
- All compiler warnings present in this run occur in pre-existing, unrelated files (`KnowledgeLifecycleEventTest.kt`, `MemoryCoreInterfacesTest.kt`, `DefaultLocalTextChannelTest.kt`, `DefaultPlanCandidateGeneratorTest.kt`, `EvidenceExtractionCoordinatorTest.kt`, `InMemoryCommunicationIntakeTest.kt`, `InMemoryIdentityServiceTest.kt`, `InMemoryKnowledgeStoreTest.kt`, `InMemoryMemoryCoreTest.kt`, `InMemoryToolRegistryTest.kt`, `InMemoryWorldModelTest.kt`) — none in `OcrEvidenceIntelligenceContractSufficiencyTest.kt` or any other OCR file.

---

## 11. Programme Boundary Confirmations

- Unit 11 (Verification) has **not** begun.
- Unit 12 (Runtime Composition) has **not** begun.
- No composition-level coordinator consuming `RequiresOcr` was built, wired, or authorised.
- No real Evidence Intelligence source file was touched.
- No provider was selected. No runtime composition was added. No governance document was changed.

## 12. Git Confirmations

- Nothing was staged during this work.
- Nothing was committed during this work.
- Nothing was pushed during this work.

## 13. Final Git Status

```
$ git status --short
?? tests/contracts/OcrEvidenceIntelligenceContractSufficiencyTest.kt
```

Confirmed: this is the only uncommitted file in the repository at the conclusion of this review, and it is exactly the expected Unit 10 deliverable.

---

## 14. Constitutional Verdict

**READY FOR ACCEPTANCE.**

Unit 10's own Purpose, Responsibilities, Constitutional constraints, Files-expected-to-change, Files-explicitly-prohibited, Verification requirements, and Completion criteria are each satisfied exactly as written. The controlling constitutional question — whether Evidence Intelligence can perform every OCR-related decision its own contract requires using only the public OCR mechanism contract — was answered affirmatively for every decision reviewed, with no omission found and therefore no production code modified, exactly as this unit's own governing rule requires. The hand-written fake caller and its exercising tests remain entirely test-scoped, never reachable from production, and never touching any real Evidence Intelligence source file. The one defect found during independent review (a redundant, vacuous assertion) was corrected and re-verified before this review was written. The full repository test suite passes without failure, error, or new warning attributable to this unit.

## 15. Recommendation

Accept Unit 10 as complete. Proceed, when Steve is ready, with Steve's own normal local verification, staging, commit, and push of `tests/contracts/OcrEvidenceIntelligenceContractSufficiencyTest.kt`. No corrective action is required before proceeding to Unit 11.
