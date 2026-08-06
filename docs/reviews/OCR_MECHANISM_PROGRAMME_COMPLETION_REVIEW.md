# OCR Mechanism Programme Completion Review

## Status

**Programme close-out review, not an implementation report.** This document consolidates and closes the OCR Mechanism programme (Implementation Plan Units 1–11), each already individually accepted through its own completion review. It reopens no governance document, redesigns no architecture, and proposes no new work. Nothing is staged, committed, or pushed by this document.

---

## 1. Programme Scope

**Purpose.** The OCR Mechanism programme exists to give Evidence Intelligence a provider-neutral capability for interpreting image content into recognised text — CDR-007's own classification of "document ingestion, OCR, transcription, and extraction" as Evidence Intelligence's own analytical function, made executable. The programme defines and implements an abstract, pluggable capability, structurally parallel to `ReasoningProvider`, that: recognises text from already-retrieved image content; discloses transcription fidelity (verbatim, normalised, or inferred reconstruction, including mixed-fidelity and page-aligned disclosure); discloses a structured, provider-neutral processing identity; discloses a working, transient confidence signal; represents its own non-collapsible failure distinctions distinctly; discloses sufficient technical fact for a future, separate caller to construct provenance; and holds no dependency of its own on any Parker platform subsystem. It acquires no authority over truth, custody, evidential classification, or knowledge promotion at any point.

**Units comprising the programme.** Eleven implementation units, each already independently accepted:

1. **Unit 1 — Provider-Neutral OCR Capability Contract.** The `OcrMechanism` interface and its own request/result/outcome shapes.
2. **Unit 2 — Provider Adapter Abstraction.** The `OcrProviderAdapter` interface, the sole boundary a future concrete provider's own types may exist behind.
3. **Unit 3 — Internal Execution Sequencer.** `OcrExecutionSequencer`, the sole production implementation of `OcrMechanism`.
4. **Unit 4 — Input Contract (Already-Retrieved Content Only).** Confirmation that the request shape accepts only already-retrieved content, with no independent retrieval path anywhere in Units 1–3.
5. **Unit 5 — OCR Execution Pipeline.** End-to-end verification of the already-built Units 1–4 pipeline.
6. **Unit 6 — OCR Output Model.** Additive refinement of the success disclosure: page-aligned, mixed-fidelity segments.
7. **Unit 7 — Failure Handling.** The concrete representation of the Scope Lock's seven non-collapsible failure distinctions.
8. **Unit 8 — Provenance-Supporting Disclosure.** Verification that the disclosure already carries every fact a future provenance record needs.
9. **Unit 9 — Structural Isolation Proof.** A dedicated, consolidated proof that the OCR mechanism holds none of the eight dependencies Scope Lock §13 excludes.
10. **Unit 10 — Evidence-Intelligence-Side Contract Sufficiency.** Confirmation, via a hand-written fake caller, that a future Evidence Intelligence caller could invoke Units 1–9's own pipeline successfully.
11. **Unit 11 — Verification.** Consolidated, collective confirmation that Units 1–10 satisfy every contractual requirement at once, not merely individually.

**Unit 12 is intentionally outside the completed programme.** Implementation Plan Unit 12 ("Runtime Composition") is explicitly marked **Blocked** by its own governing text, and Implementation Plan §16 ("Blocked Work — Requires Future Governance") lists the specific, still-unresolved future-governance items that must be settled before it may begin: owner control and authorisation for machine-triggered OCR invocation; design and acceptance of the composition-level coordinator that consumes Evidence Processing's own `RequiresOcr` disclosure; whether a dedicated Permission Engine proposal class is required for OCR invocation specifically; output-quality validation policy and threshold; and permission gating and disposition for rejected output. Implementation Plan §11 (Completion Criteria) states this explicitly: "Unit 12 is never part of this completion determination. Implementation of Units 1–11 may be declared complete while Unit 12 remains blocked." This document does not resolve, begin, or propose resolving any of those items.

---

## 2. Constitutional Objectives

Every objective below is drawn from the governing documents' own explicitly-labelled objective/constraint lists — Scope Lock §2 (Frozen Objectives), Contract Design §8 (Constitutional Constraints — Authority Never Possessed), and Scope Lock §17 (Constitutional Self-Certification) — rather than restated informally. (The full, exhaustive clause-by-clause matrix covering every numbered section of both documents was already produced by `docs/reviews/OCR_MECHANISM_UNIT_11_COMPLETION_REVIEW.md` §4; this section summarises at the objective level, not the full sub-clause level, and is not a duplicate of that matrix.)

### 2.1 Scope Lock §2 — Frozen Objectives

| # | Objective | Disposition | Governing clause |
| --- | --- | --- | --- |
| 1 | No dependency of its own on `EvidenceCustodian`, `MemoryCore`'s write interface, Knowledge Memory's Knowledge Submission interface, or the Permission Engine, at any depth | Fully satisfied | Scope Lock §2 item 1; Contract Design §2, §8, §12 |
| 2 | No code path ever writes, overwrites, replaces, or obscures original evidence content | Fully satisfied | Scope Lock §2 item 2; CDR-006's Constitutional Optimisation Safeguard |
| 3 | Never itself constructs a governed record (`CandidateEvidenceArtifact`, `Assertion`, `Relationship`, `KnowledgeCandidate`) | Fully satisfied | Scope Lock §2 item 3; Contract Design §5 |
| 4 | Every recognition carries a fidelity classification from the Evidence Intelligence Contract Design's own three frozen categories, never a fourth | Fully satisfied | Scope Lock §2 item 4 |
| 5 | No implementation holds a Permission Engine reference of its own, and no implementation may be invoked other than through an already-authorised Evidence Intelligence orchestration path | Fully satisfied (no Permission Engine reference); Deferred by governance (the orchestration path itself, since it requires Unit 12) | Scope Lock §2 item 5; §4, §12 |
| 6 | No implementation names, selects, or depends upon a concrete OCR engine, library, or service | Fully satisfied | Scope Lock §2 item 6; §14 |
| 7 | Every provenance fact uses Memory Core's existing, unmodified `Provenance`/`CandidateProvenance` contract — no new provenance type, no new field | Fully satisfied | Scope Lock §2 item 7; §9 |

### 2.2 Contract Design §8 — Constitutional Constraints (Authority Never Possessed)

| Constraint | Disposition | Governing clause |
| --- | --- | --- |
| No truth authority | Fully satisfied | Contract Design §8; Epistemic Integrity Articles III, VI, VII; CDR-007 §2 |
| No custody, modification, or deletion authority over any original | Fully satisfied | Contract Design §8; CDR-006's Constitutional Optimisation Safeguard |
| No acceptance authority — cannot cause anything to enter Evidence Custodian, Memory Core, or Knowledge Memory | Fully satisfied | Contract Design §8; Evidence Intelligence Contract Design §6, §12 |
| No evidential-classification authority — cannot assign `EvidentialState` | Fully satisfied | Contract Design §8; Article XV; Programme 3 Contract Design V2 §4 |
| No independent epistemic- or trust-authorisation mechanism | Fully satisfied | Contract Design §8; Constitution ("No intelligence within Parker is trusted with unchecked authority"; "No capability may bypass trust") |
| No self-granted authority | Fully satisfied | Contract Design §8; Constitution ("No module may grant itself authority") |

### 2.3 Scope Lock §17 — Constitutional Self-Certification

| Category | Disposition | Governing clause |
| --- | --- | --- |
| Owner control | Fully satisfied (preserved in full; the future trigger model is explicitly, not silently, deferred) | Scope Lock §17, §12 |
| Parser non-authority | Fully satisfied | Scope Lock §17, §3; Amendment 2 |
| Original-evidence immutability | Fully satisfied, without exception | Scope Lock §17, §7; CDR-006 |
| Evidence Custodian ownership | Fully satisfied | Scope Lock §17, §3, §13 |
| Evidence Processing ownership | Fully satisfied (never directly consumes `RequiresOcr`) | Scope Lock §17, §4 |
| Evidence Intelligence ownership | Fully satisfied and made executable (invoked only through an authorised orchestration path — the path itself remains Deferred by governance, pending Unit 12) | Scope Lock §17, §4; CDR-007 |
| Provenance | Fully satisfied | Scope Lock §17, §9 |
| No peer subsystem creation | Fully satisfied | Scope Lock §17, §3; Amendment 2 |
| No public-contract expansion | Fully satisfied | Scope Lock §17, §6 |
| No implementation pre-authorisation | Fully satisfied | Scope Lock §17, §14, §15, §16 |

**No objective resolves to "outside OCR responsibility" at this tier** — every item in Scope Lock §2, Contract Design §8, and Scope Lock §17 is, by its own text, an objective the OCR mechanism's own implementation must itself satisfy, not one belonging to another subsystem. (Objectives belonging to other subsystems — for example, Evidence Processing's own detection ownership, or Evidence Intelligence's own recognition-quality judgement — are addressed at the finer-grained clause level in the Unit 11 review's own §4 matrix, where several such items are correctly marked "outside OCR responsibility.")

---

## 3. Unit-by-Unit Completion Matrix

| Unit | Name | Production changes | Verification completed | Commit SHA | Status |
| --- | --- | --- | --- | --- | --- |
| 1 | Provider-Neutral OCR Capability Contract | `src/interfaces/OcrMechanism.kt` created | `OcrMechanismScopeTest.kt` | `38465aa` | Accepted |
| 2 | Provider Adapter Abstraction | `src/interfaces/OcrProviderAdapter.kt` created | `OcrProviderAdapterScopeTest.kt` | `d3f3428` | Accepted |
| 3 | Internal Execution Sequencer | `src/runtime/OcrExecutionSequencer.kt` created | `OcrExecutionSequencerTest.kt` | `4017b5e` | Accepted |
| 4 | Input Contract | None (verification only) | `OcrInputContractTest.kt` | `d18847c` | Accepted |
| 5 | OCR Execution Pipeline | None (verification only) | `OcrExecutionPipelineTest.kt` | `2d7cdf8` | Accepted |
| 6 | OCR Output Model | `OcrMechanism.kt` additively refined (`OcrRecognitionSegment`, `OcrRecognitionResult.segments`) | `OcrOutputModelTest.kt` | `e60237d` | Accepted |
| 7 | Failure Handling | `OcrMechanism.kt` additively refined (seven new `OcrRecognitionOutcome` sibling variants) | `OcrFailureHandlingTest.kt`; two pre-existing assertions in `OcrMechanismScopeTest.kt` lawfully updated | `7a6bb11` | Accepted |
| 8 | Provenance-Supporting Disclosure | None (verification only; no field found missing) | `OcrProvenanceDisclosureTest.kt` | `6e337e0` | Accepted |
| 9 | Structural Isolation Proof | None (verification only) | `OcrStructuralIsolationTest.kt` | `9cea986` | Accepted |
| 10 | Evidence-Intelligence-Side Contract Sufficiency | None (verification only; no omission found) | `OcrEvidenceIntelligenceContractSufficiencyTest.kt` | `7871c36` | Accepted |
| 11 | Verification | None (verification only) | `OcrProgrammeVerificationTest.kt`; full clause-by-clause matrix; full `./gradlew clean test` | `fbf6d08` | Accepted |

Every commit SHA above was confirmed directly from `git log` at the time this document was written, and matches the SHA recorded in each unit's own prior completion report where one was produced. Unit 12 does not appear in this matrix; it has not begun and is not part of this completion determination (Implementation Plan §11).

---

## 4. Boundary Verification

The completed programme's isolation from each of the following was verified structurally — by test, not by convention — at the point named, and re-confirmed collectively by Unit 11:

- **Evidence Custodian.** No type reachable from `OcrMechanism`, `OcrProviderAdapter`, `OcrExecutionSequencer`, or any request/result/outcome type references `EvidenceCustodian`, at any depth. Only `EvidenceArtifactId` — a reused identifier *value*, explicitly distinguished from a dependency on the `EvidenceCustodian` *interface* itself (Contract Design §13) — is referenced. Verified by Unit 4's own dedicated absence test, Unit 9's own closed, transitively-computed reachable-type-graph proof, and re-confirmed by Unit 11's own collective test.
- **Memory Core.** No reference to `MemoryCore`, `MemoryRetrieval`, `Provenance`, or `CandidateProvenance` exists in any production file. These types are referenced only inside test-scoped, hand-written fakes (Units 8 and 10), used purely to demonstrate that a *future* caller could construct provenance from OCR's own disclosure — never by OCR itself. Verified by Unit 8's own structural-absence test and Unit 9's own closed-graph proof.
- **Knowledge Submission.** No reference to `KnowledgeSubmission` or `CandidateKnowledge` exists anywhere in the programme's own production or test-fake code. Verified by Unit 9.
- **Permission Engine.** No reference to `PermissionEngine` exists anywhere in production code. The one outcome variant that would represent a permission denial (`OcrRecognitionOutcome.NotAuthorised`) is proven, by a dedicated construction-absence test, never to be constructed by any code path in Units 1–7 — it exists only "for completeness of the taxonomy" (Implementation Plan Unit 7). Verified by Unit 7's own construction-absence test and Unit 9's own closed-graph proof.
- **Evidence Intelligence implementation.** No `EvidenceAnalysisRequest`, `EvidenceAnalysisResult`, or `EvidenceIntelligence` type was ever touched, referenced, or created. Unit 10's own "future Evidence Intelligence caller" is a private, test-scoped fake, never reachable from `src/`. Verified by Unit 10's own final structural test and Unit 9's own closed-graph proof.
- **Runtime composition.** No reference to `ParkerRuntime`, a composition root, a runtime coordinator, or any dependency-injection or service-locator vocabulary exists in any production file — confirmed further by a repository-wide search finding no dependency-injection framework of any kind anywhere in this codebase. Verified by Unit 9's own dedicated composition-isolation tests.
- **Provider selection.** No concrete OCR provider (Tesseract, OCRmyPDF, PaddleOCR, EasyOCR, or any other) is named, selected, evaluated, ruled in, or ruled out anywhere in the programme. Zero concrete classes implement `OcrProviderAdapter` in `src/`. Verified by Unit 2's own provider-neutrality tests, Unit 9's own zero-implementers test, and Unit 11's own regression check that no fourth production file has appeared.
- **Registration.** No reference to `EvidenceRegistrationCoordinator` or `EvidenceRegistrationOutcome` exists in production code; Unit 10's own hypothetical `CandidateEvidenceArtifact` construction proves only that the *shape* required for a future registration call is available from OCR's own disclosure, without the programme itself ever invoking the real coordinator. Verified by Unit 10's own structural test.
- **Persistence.** No production file contains any persistence, database, storage, or repository vocabulary; no durable write occurs anywhere in Units 1–11's own code. Verified by Unit 9's own behaviour-isolation tests and the closed reachable-type-graph proof, which admits no storage-layer type of any kind.

---

## 5. Public OCR Contract

Described here in prose, as implemented, without reproducing source code:

- **Request.** A recognition request carries the original evidence's own existing, unmodified identifier; already-retrieved image content, held as a plain in-memory byte sequence never independently fetched by the mechanism itself; the detected media type; and the source's own page count, where known. No requesting principal, no caller-declared confidence, and no evidential-state value may be supplied.
- **Result.** A successful recognition's own disclosure carries: the complete, document-level recognised text; a single, whole-recognition fidelity classification; a structured, provider-neutral identity record (mechanism identity, configuration profile, and version, where available); a working, transient confidence signal, present only when genuinely available and bounded to the closed unit interval; the moment the recognition was performed; an ordered list of non-fatal warnings; and an optional, finer-grained breakdown of the same recognition into per-portion segments.
- **Outcome model.** Every recognition attempt produces exactly one of nine distinguishable outcomes: a successful recognition; a generic, undifferentiated failure disclosure (preserved from the programme's earliest shape, for source compatibility); and seven further, individually distinct outcomes added later in the programme, each corresponding to one of the Scope Lock's own seven non-collapsible constitutional failure distinctions — an orchestration-only denial and a Parker-owned validation rejection, neither of which the OCR mechanism's own code path ever produces itself; an unsupported-or-inaccessible-input disclosure; a no-recognisable-content disclosure; a partial-or-degraded-output disclosure, which uniquely carries the actual, still-usable partial recognition rather than discarding it; a processing-or-dependency-failure disclosure; and a genuine-implementation-fault disclosure. No two of the nine may ever be collapsed into a shared representation.
- **Segments.** Where a recognition's own text divides meaningfully into portions — by page, by differing fidelity within the same recognition, or both — each portion is disclosed with its own text, its own fidelity, and, where known, its own one-based page number. Page numbers, where present, must appear in non-decreasing order across the list; multiple portions may share one page. This finer-grained view never duplicates or contradicts the document-level recognised text; it supplements it.
- **Fidelity.** Every recognition, and every segment within it, is classified as exactly one of three categories already frozen by Evidence Intelligence's own governing contract: a verbatim transcription, reproducing the original's exact characters as read; a normalised transcription, standardised for readability at the cost of exact reproduction; or an inferred reconstruction, filling a gap the original does not clearly support with the mechanism's own best technical judgement. No fourth category exists.
- **Warnings.** Any non-fatal condition observed during recognition is disclosed as an ordered list of plain-text entries, preserved exactly in the order produced, with duplicates retained rather than deduplicated. An empty list means genuinely no warnings, never an omission.
- **Confidence.** A working technical confidence figure, present only when genuinely available, bounded to the closed interval between zero and one inclusive, and never written to any durable field anywhere — the confidence disclosed here is transient working material only, never a constitutional or evidential figure.
- **Timestamp.** The moment a given recognition was performed, disclosed unconditionally as part of every successful result.
- **Identity.** A single, structured record — never free-form prose — naming which mechanism configuration produced a given recognition and, where available, its version, without ever naming a concrete engine, library, or service anywhere in the public contract.
- **Failure model.** Beyond the nine-way outcome distinction above, the mechanism itself never decides whether a disclosed recognition is good enough to become governed evidence; it discloses honestly and lets a future caller's own judgement decide. A genuine, unexpected implementation fault may still propagate as an ordinary thrown exception rather than a disclosed outcome, exactly as the internal sequencing layer's own no-retry, no-catch discipline has required throughout the programme.

---

## 6. Verification Summary

- **Structural verification.** Reflection-based, compiled-shape proofs — preferred throughout the programme over source-text pattern matching wherever a structural mechanism could prove the same invariant — established: zero reachability to any of the eight Scope Lock §13 exclusions, at any depth, from any OCR-owned type (Unit 9); a closed, exact public-declaration surface per production file, with no undocumented API (Unit 9); exact import sets for every production file (Unit 9); subtype isolation from every governed candidate/record type (Unit 9); and a sole implementer of `OcrMechanism`, with zero implementers of `OcrProviderAdapter` anywhere in `src/` (Units 2, 9, 11).
- **Behavioural verification.** Hand-written-fake-driven tests confirmed: exactly one adapter invocation per sequencer call, with no retry, batching, or caching (Units 3, 7, 9); unchanged relay of both successful and failing outcomes through the sequencer (Units 3, 5); genuine, unexpected thrown faults propagating unaltered rather than being silently wrapped (Unit 7); and a provider adapter's own voluntary choice to disclose, rather than throw, a caught fault, coexisting with that same propagation discipline (Unit 7).
- **Contract verification.** Construction-time validation, immutability, and additive-compatibility were each demonstrated across the request, identity, segment, result, and every one of the nine outcome shapes (Units 1, 4, 6, 7); every fidelity category and every mixed-fidelity, page-aligned combination was shown discoverable, not merely asserted in prose (Unit 6); a hypothetical, downstream `CandidateProvenance` and a deterministic downstream integrity digest were each shown constructible from OCR's own disclosure alone, using only already-existing fields (Units 8, 10).
- **Programme verification.** Unit 11 consolidated all of the above into a single, collective demonstration that provider-neutral abstraction, execution sequencing, the input boundary, the output model, the failure model, provenance-supporting disclosure, structural isolation, and Evidence Intelligence contract sufficiency all hold *simultaneously*, on both a successful and a failing outcome, in one realistic end-to-end scenario — not merely as nine independently-passing test files. A full clause-by-clause verification matrix, covering every numbered section of the Contract Design, the Scope Lock, and the Implementation Plan, found no clause unsupported. The full repository test suite (1,661 tests at the time of Unit 11's own review) passed without failure, error, or new warning attributable to the programme.

---

## 7. Remaining Deferred Work

Only the work already, and explicitly, left outside the programme by its own governing documents — no new item is identified or proposed here:

- **Unit 12 — Runtime Composition.** Blocked. Not begun. Requires, at minimum, resolution of the future-governance items below before it may start.
- **Scope Lock §18 — Deferred to Future Governance and Implementation** (eight items, restated verbatim from the Scope Lock's own list): machine-triggered invocation and its owner-control model; the composition-level coordinator that consumes `RequiresOcr`; whether a dedicated Permission Engine proposal class is required for OCR invocation specifically; output-quality validation policy and threshold; permission gating and disposition for rejected output; concrete provider identity and adapter design; process/execution and deployment topology; and exact Kotlin names, method signatures, and file layout (this last item is not a block on any unit already completed — it was exercised as ordinary engineering discretion throughout Units 1–11).
- **Implementation Plan §16 — Blocked Work** mirrors the same eight items exactly, and additionally states: "Units 1–11 of this plan do not require items 1–7 to be resolved first... Unit 12 requires items 1–3 at minimum, and should not begin before items 4–5 are at least addressed to the extent runtime exposure of rejected output would otherwise be ungated."

No new work, redesign, or architectural change is proposed by this document.

---

## 8. Constitutional Assessment

An independent constitutional assessment of the completed programme, addressing each question in turn:

- **Is the programme constitutionally complete?** Yes, for Units 1–11 exactly as Implementation Plan §11 defines completeness — every unit meets its own completion criteria, the verification strategy passes in full with the single, explicitly-disclosed exception of runtime integration tests (correctly not-yet-applicable pending Unit 12), all five structural safeguards pass, no Scope Lock §13 dependency exists anywhere in the compiled shape, and provider neutrality remains intact throughout.
- **Is every implemented responsibility authorised?** Yes. Every production type, field, and outcome variant traces to an exact clause in the Contract Design or the Scope Lock, cited in each unit's own KDoc and confirmed by the exhaustive clause-by-clause matrix `OCR_MECHANISM_UNIT_11_COMPLETION_REVIEW.md` §4 produced. No field, type, or capability exists in the current shape that is not traceable to governing text.
- **Did any unit exceed its scope?** No, in the accepted, current shape. One historical near-exception is on record and was corrected before acceptance: Unit 1's first two drafts (seven bespoke failure subclasses, then a seven-value enum) each attempted to represent Scope Lock §10's own seven failure distinctions before Unit 7 was authorised to do so; both were caught by the established review discipline and corrected to the lawful two-variant shape before Unit 1 was ever accepted. This is documented, permanently, in `OcrMechanism.kt`'s own KDoc as a record of what was tried and why it was wrong, not as a live defect.
- **Were any prohibited dependencies introduced?** No. Unit 9's own closed, transitively-computed reachable-type-graph proof, and its five additional named-exclusion tests, establish this for all eight Scope Lock §13 exclusions at once; Unit 11 re-confirmed the same proof holds collectively, on both a successful and a failing outcome.
- **Does the public contract remain provider-neutral?** Yes. No concrete OCR provider is named, selected, evaluated, ruled in, or ruled out anywhere in the eleven completed units; zero concrete classes implement `OcrProviderAdapter` in `src/`; no provider-specific field, type, or vocabulary exists in any public shape (Units 2, 9).
- **Does the implementation remain runtime-neutral?** Yes. No reference to `ParkerRuntime`, a composition root, or any dependency-injection or service-locator concept exists anywhere in the programme's own production code; the sole invocation path proven is `OcrMechanism → OcrExecutionSequencer → OcrProviderAdapter`, with no alternate path (Units 3, 9).
- **Is the programme suitable for future integration?** Yes, within the boundary the governing documents themselves already draw. Unit 10's own hand-written fake caller demonstrates that a future Evidence Intelligence caller could invoke the completed pipeline successfully today, using only the public contract as it stands — but the composition-level work that would make that invocation reachable in production (the `RequiresOcr`-consuming coordinator, the owner-authorisation gate, and the output-quality validation policy) is unbuilt and unauthorised by design, correctly left to Unit 12 and the future governance stages named in Scope Lock §18.

---

## 9. Recommendation

**READY FOR ARCHITECTURAL ARCHIVE.**

Units 1–11 satisfy Implementation Plan §11's own completion criteria in full. Every constitutional objective named in Scope Lock §2, Contract Design §8, and Scope Lock §17 resolves to fully satisfied or, where the objective itself names a future orchestration path not yet built, deferred by governance exactly as those same documents already anticipate — never unsupported. No prohibited dependency, runtime composition, provider selection, registration, or persistence capability exists anywhere in the completed programme. The public contract remains provider-neutral and runtime-neutral throughout. This recommendation rests solely on the governing documents' own stated completion criteria (Implementation Plan §11) and their own self-certification categories (Scope Lock §17), both of which the completed programme satisfies without exception. Unit 12 and the future-governance items named in Scope Lock §18 remain, correctly, outside this recommendation's own scope — their resolution is a separate, future governance decision, not a precondition this document imposes.
