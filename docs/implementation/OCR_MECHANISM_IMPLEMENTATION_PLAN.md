# OCR Mechanism — Implementation Plan

## Status

**Draft. Pending implementation. Not yet accepted. Implementation authority only after independent constitutional review of this document.** No Kotlin is implemented, proposed as a diff, or changed by this document — every unit below is described by purpose, obligation, and relationship, never by Kotlin interface, class, enum, method signature, or package name, mirroring the same "implementation independence" discipline `OCR_MECHANISM_CONTRACT_DESIGN.md`'s own Status section already established. No concrete OCR provider — OCRmyPDF, Tesseract, EasyOCR, PaddleOCR, or any other — is chosen, evaluated, or implied anywhere below. Neither `src/` nor `tests/` is touched by this document. Nothing is staged, committed, or pushed.

**Repository baseline confirmed before this document was drafted:** `main` at `172eb70de1943bc0aeeb270200bf265c71c83674`, working tree clean, matching the commit that carried `docs/architecture/OCR_MECHANISM_CONTRACT_DESIGN.md`, `docs/architecture/OCR_MECHANISM_SCOPE_LOCK.md`, and their two independent reviews.

**Implements `docs/architecture/OCR_MECHANISM_CONTRACT_DESIGN.md` ("the Contract Design") and `docs/architecture/OCR_MECHANISM_SCOPE_LOCK.md` ("the Scope Lock") without reopening either.** Every responsibility, boundary, deferral, and constitutional constraint those two documents already fixed is treated here as given, not re-derived. This document divides their already-authorised shape into implementable units and fixes unit boundaries, dependencies, and verification obligations — it adds no constitutional decision either document did not already make.

Also binding, unmodified, and not reopened by this document: `docs/decisions/CDR-006_CONSTITUTIONAL_CLASSIFICATION_OF_ORIGINAL_EVIDENCE_CUSTODY_AND_IMMUTABILITY.md` ("CDR-006"), `docs/decisions/CDR-007_CONSTITUTIONAL_CLASSIFICATION_OF_EVIDENCE_INTELLIGENCE.md` ("CDR-007"), `docs/architecture/EVIDENCE_INTELLIGENCE_CONTRACT_DESIGN.md` (as amended by Amendment 2), `docs/architecture/EVIDENCE_INTELLIGENCE_SCOPE_LOCK.md` (as amended, mirrored), and `docs/architecture/EVIDENCE_PROCESSING_SEARCHABLE_PDF_BOUNDARY_CLARIFICATION.md`/`EVIDENCE_PROCESSING_SEARCHABLE_PDF_SCOPE_LOCK.md`.

**This plan does not authorise everything the Scope Lock names.** The Scope Lock's own §18 ("Deferred to Future Governance and Implementation") lists eight items that remain open. Several of the twelve units below are fully buildable today; two are structurally blocked until specific §18 items are separately resolved. Section 7 identifies exactly which, and why — this is the one place this document adjusts the task's own suggested unit sequence, and it does so by narrowing scope to match already-accepted governance exactly, never by broadening it.

---

## 1. Purpose

Define the engineering sequence that implements the OCR mechanism's already-accepted contract and scope boundary: a provider-neutral, dependency-free capability Evidence Intelligence may invoke, producing recognised text and its required disclosures as candidate material only. This plan fixes unit boundaries, dependencies, verification obligations, and prohibited files for each unit — it chooses no concrete provider, no Kotlin name, and no runtime wiring beyond what the Scope Lock already authorises.

---

## 2. Authority

**Governing sources, by tier:**

- **The Contract Design** — controlling for the OCR mechanism's own responsibilities, non-responsibilities, shape, inputs, outputs, failure model, provenance obligations, and constitutional constraints (§1–§13 there).
- **The Scope Lock** — controlling for every implementation boundary this plan converts into units: capability boundary, invocation boundary, input/output boundary, original-evidence boundary, derivative boundary, provenance boundary, failure taxonomy, output-quality validation boundary, owner-control boundary, dependency boundary, provider neutrality, security minimums, explicit exclusions, and self-certification (§3–§17 there).
- **CDR-006** — controlling for original-evidence immutability and the Constitutional Optimisation Safeguard, naming OCR explicitly.
- **CDR-007** — controlling for OCR execution's classification as Evidence Intelligence's own analytical function.
- **Evidence Intelligence Contract Design (Amendment 2) / Scope Lock** — controlling for the OCR mechanism's own status as a capability-level Evidence Intelligence dependency, never a peer subsystem.
- **Evidence Processing Searchable PDF Boundary Clarification / Scope Lock / Implementation Plan** — cited as structural and procedural precedent only (the `EvidenceExtractor`/`TikaEvidenceExtractor` shape, the provider-confinement discipline, the "build and fully test via hand-written fakes before runtime composition" sequencing) — neither reopened nor extended by this document.

This plan does not reopen, re-argue, or narrow any decision in any of the above.

---

## 3. Implementation Objectives

The completed implementation shall provide, and shall be judged against, exactly the following — each already authorised by the Contract Design or Scope Lock, none invented here:

1. **Provider-neutral OCR capability** — one abstract capability, no concrete engine named or preferred (Scope Lock §14).
2. **Immutable original evidence** — no code path writes, overwrites, replaces, or obscures an original, under any condition, including failure (Scope Lock §7; CDR-006).
3. **Governed OCR output** — every disclosure remains candidate material until Evidence Intelligence's own judgement and the existing, unmodified acceptance path register it; nothing becomes canonical evidence merely because recognition succeeded (Scope Lock §8).
4. **Provenance preservation** — every fact a future provenance record needs is available from the disclosure, using only Memory Core's existing, unmodified contract (Scope Lock §9).
5. **Provider isolation** — no provider-specific type reaches any Parker-owned public contract, mirroring the `org.apache.tika.*`-confinement discipline already established (Scope Lock §14).
6. **Deterministic behaviour** — the same input, provider, and configuration reproduce the same disclosure, mirroring the reproducibility discipline Evidence Processing's own `ExtractionIdentity` already established.
7. **Complete testability** — every unit fully tested via hand-written fakes before any runtime composition exists, mirroring exactly how Evidence Processing's own Units 1–4B were built (OCR Ownership and Sequencing Review §13).

No unit below introduces any constitutional authority beyond these seven objectives.

---

## 4. Implementation Principles

- **No dependency is added anywhere the Scope Lock's §13 excludes it.** The OCR mechanism itself never depends on Evidence Custodian, Memory Core, Knowledge Submission, the Permission Engine, Evidence Intelligence's own result handling, any runtime conversation component, any reporting mechanism, or Docling — at any depth, in any unit.
- **Deferred governance stays deferred.** No unit resolves machine-triggered invocation, the owner-control model for it, the `RequiresOcr`-consuming coordinator, whether a dedicated Permission Engine proposal class is required, output-quality validation policy, or permission gating for rejected output. Where a unit's own boundary touches one of these questions, it stops at the boundary and states plainly that the question remains open (§7, §16, below).
- **Provider neutrality is structural, not conventional.** Provider isolation (Objective 5) is enforced the same way Evidence Processing already enforces Tika-confinement: by a dedicated adapter boundary and a structural test proving no provider type is reachable from any public contract — never merely by code review discipline.
- **Reuse before invention.** Every provenance fact, every fidelity category, every output category already exists in Memory Core's or the Evidence Intelligence Contract Design's own frozen contracts (Contract Design §13). No unit invents a parallel mechanism.

---

## 5. Existing Components Reused

Reused, unmodified, by this plan: `EvidenceArtifactId`, `CandidateEvidenceArtifact` (Evidence Custodian's own contract, referenced only by whichever future Evidence-Intelligence-side component eventually constructs a candidate — never referenced by the OCR mechanism itself); `Provenance`, `CandidateProvenance`, including `extractedFrom`/`derivedFrom` (Memory Core's own contract); the Evidence Intelligence Contract Design's own four-category `EvidenceAnalysisResult` taxonomy and three-category transcription-fidelity taxonomy (§5 there); the Evidence Intelligence Contract Design's own Confidence Model (§9 there). None of these five contracts gains a new field, a new variant, or a new category anywhere in this plan.

---

## 6. New Components

Described architecturally, by responsibility only — no Kotlin name, method signature, package, or source file is assigned to any of the six below:

1. **The OCR mechanism's own request/result/failure shapes** (Unit 1) — the provider-neutral capability contract itself.
2. **A provider-adapter boundary** (Unit 2) — the one place a future concrete provider's own types are permitted to exist, structurally confined exactly as `TikaEvidenceExtractor` already confines `org.apache.tika.*`.
3. **An internal execution sequencer** (Unit 3) — orchestrates the OCR mechanism's own single operation (receive input, invoke the adapter, construct disclosures); holds no dependency beyond the adapter boundary itself; **not** the composition-level coordinator Scope Lock §18 defers.
4. **A failure-disclosure mechanism** (Unit 7) — the concrete representation of the Scope Lock's seven non-collapsible distinctions, first designed at this tier exactly because Scope Lock §10 confines the concrete taxonomy to future implementation.
5. **A structural isolation test suite** (Unit 9) — proves, rather than merely asserts, the absence of every dependency Scope Lock §13 excludes.
6. **A verification suite** (Unit 11) — covers every category §9 of this document requires.

No seventh component is introduced. In particular, this plan does not define a runtime entry point, a `RequiresOcr` consumer, or a Permission Engine proposal class — each remains blocked (§7, §16, below).

---

## 7. Implementation Sequence

The task's own suggested twelve-unit sequence is retained in full, with four units reframed against the Scope Lock's own explicit boundaries, and two flagged as structurally blocked rather than removed — narrowing, never broadening, what the suggested sequence would otherwise imply:

- **Unit 4 ("Read-only evidence access")** is reframed from "the mechanism gains evidence access" to **"the mechanism's own input contract accepts only already-retrieved content."** Scope Lock §13 forbids any Evidence-Custodian dependency of the OCR mechanism's own; the "read-only access" Contract Design §4 describes is Evidence Intelligence's own, already-existing `EvidenceCustodian.retrieve` call, never a new access path this plan could add. Unit 4 builds nothing that reads evidence — it fixes the shape of what a caller must already have retrieved before calling in.
- **Unit 8 ("Provenance integration")** is reframed from "the mechanism writes provenance" to **"the mechanism's disclosure carries every fact a future provenance record needs."** The OCR mechanism holds no Memory Core dependency (Scope Lock §13); it never itself constructs a `Provenance` value.
- **Unit 9 ("Evidence Custodian integration")** is reframed from "the mechanism integrates with Evidence Custodian" — which Scope Lock §13 forbids outright — to **"structural proof that no such integration exists."** This unit's own deliverable is the test suite proving the absence of every excluded dependency, not a new dependency.
- **Unit 10 ("Evidence Intelligence integration")** is narrowed to **"confirming the OCR mechanism's own public shape is sufficient for a future Evidence-Intelligence-side caller"** — hand-written-fake-tested exactly as Evidence Processing's own Units 1–4B were, never wired into any real Evidence Intelligence code path. The actual caller — the composition-level coordinator that consumes Evidence Processing's own `RequiresOcr` disclosure and invokes Evidence Intelligence in response — is Scope Lock §18 item 2's own deferred item, not built here.
- **Unit 12 ("Runtime composition")** is **structurally blocked, not merely deferred in emphasis.** Real runtime composition requires the machine-triggered-invocation owner-control model, the `RequiresOcr` consumer, and a decision on whether a dedicated Permission Engine proposal class is required (Scope Lock §18 items 1–3) — none of which this plan, or the Scope Lock it implements, resolves. Unit 12 below states exactly what must happen first, and performs no wiring itself.

Units 1, 2, 3, 5, 6, 7, and 11 require no reframing — each is fully authorised and fully buildable under the Scope Lock as accepted.

---

## 8. Unit Breakdown

### Unit 1 — Provider-Neutral OCR Capability Contract

- **Purpose.** Establish the OCR mechanism's own request, result, and failure shapes as the sole new public surface this plan authorises, exactly as Contract Design §3–§6 and Scope Lock §5–§6, §10 already fix them.
- **Responsibilities.** Represent, in shape only: the permitted input categories (source evidence identity; immutable source bytes or read-only access; media type; page/document scope; processing context — Scope Lock §5); the permitted output categories (recognised text; fidelity disclosure, bound to the three already-frozen categories; structured identity disclosure; working confidence signal — Scope Lock §6); and the seven non-collapsible failure distinctions (Scope Lock §10) as a closed set, none a fourth or eighth invented category.
- **Dependencies.** None. Mirrors `ReasoningProvider`'s "pure callee, calls nothing" shape exactly (Contract Design §3, §12; Scope Lock §3, §13).
- **Inputs.** Not applicable — this unit defines the input shape; it does not consume one.
- **Outputs.** The defined request, result, and failure shapes themselves.
- **Constitutional constraints.** No dependency of any kind; no fifth output category; no fourth fidelity category; no eighth failure distinction; no requesting-principal field (Scope Lock §5 confirms this remains at Evidence Intelligence's own tier); no caller-declared confidence or evidential-state input field (Scope Lock §5).
- **Files expected to change.** One new file within the existing interface-shape location this repository already uses for narrow, dependency-free capability contracts — exact file and type names are Implementation-tier engineering choices this plan does not fix.
- **Files explicitly prohibited.** Any file importing a concrete provider library; any runtime or composition-root file; any file under Evidence Custodian's, Memory Core's, or Knowledge Memory's own existing locations.
- **Verification requirements.** A structural test confirming zero dependencies reachable from any of the three shapes, at any depth; a test confirming exactly four output categories, three fidelity categories, and seven failure distinctions are representable, no more, no fewer.
- **Completion criteria.** The three shapes exist, hold no dependency, and no code path anywhere can construct a fifth output category, a fourth fidelity category, or an eighth failure distinction.

### Unit 2 — Provider Adapter Abstraction

- **Purpose.** Establish the one boundary a future concrete provider's own types are permitted to exist behind, mirroring exactly how `TikaEvidenceExtractor` is the sole file permitted to import `org.apache.tika.*` (Evidence Processing Boundary Clarification §3).
- **Responsibilities.** Define an abstract adapter shape capable of being satisfied by any future concrete provider — OCRmyPDF, Tesseract, EasyOCR, PaddleOCR, or another — without naming, preferring, or evaluating any of them (Scope Lock §14). No concrete adapter is written in this unit; only the abstraction any future concrete adapter must satisfy.
- **Dependencies.** Unit 1's own shapes only.
- **Inputs.** Unit 1's own request shape.
- **Outputs.** Unit 1's own result/failure shapes.
- **Constitutional constraints.** No provider is named, selected, evaluated, ruled in, or ruled out (Scope Lock §14). No provider-specific type may appear in this abstraction's own public shape. Provider replacement must never require a constitutional change (Scope Lock §14, citing the Constitution's own "Replaceable reasoning providers" principle).
- **Files expected to change.** One new file defining the abstract adapter boundary, in the same location as Unit 1's shapes or immediately adjacent to it.
- **Files explicitly prohibited.** Any file that imports a concrete provider library at this unit — that remains a later, separate, provider-specific adapter implementation, explicitly deferred future work under this plan (§16, item 6, below).
- **Verification requirements.** A structural test confirming the abstraction's own public shape contains no provider-specific type, anywhere, including generic type parameters — mirroring exactly the reflection test Evidence Processing already uses for Tika-confinement.
- **Completion criteria.** The abstraction exists, is satisfiable in principle by more than one hypothetical provider, and no provider-specific type is reachable from it.

### Unit 3 — Internal Execution Sequencer

- **Purpose.** Sequence the OCR mechanism's own single operation — receive input, invoke the adapter boundary, construct the disclosure — as one capability, one act (Contract Design §3).
- **Responsibilities.** Orchestrate Units 1 and 2 only. Construct no governed record. Decide nothing about whether its own output is worth producing as a candidate (Scope Lock §11) — that determination is explicitly out of this unit's scope.
- **Dependencies.** Unit 1, Unit 2. No further dependency.
- **Inputs.** Unit 1's own request shape, already populated by a caller.
- **Outputs.** Unit 1's own result or failure shape.
- **Constitutional constraints.** **This is not the composition-level coordinator that consumes `RequiresOcr`.** That coordinator remains Scope Lock §18 item 2's own deferred item; this unit's sequencer holds no reference to Evidence Processing, Evidence Intelligence, or any composition root of any kind. No self-triggering, no background work, no independent read path of any kind (Scope Lock §4).
- **Files expected to change.** One new file containing the sequencing logic, depending only on Units 1 and 2's own shapes.
- **Files explicitly prohibited.** Any file referencing `EvidenceCustodian`, `MemoryCore`, Knowledge Memory's submission interface, the Permission Engine, `EvidenceIntelligence`, or any runtime conversation component.
- **Verification requirements.** A structural reachability test confirming the sequencer holds no reference, at any depth, to any of the six excluded dependencies above; hand-written-fake tests covering successful recognition, each of the seven failure distinctions, and the boundary between them.
- **Completion criteria.** The sequencer completes one full request-to-result cycle using only Units 1–2, fully tested via fakes, with zero excluded dependencies reachable.

### Unit 4 — Input Contract: Already-Retrieved Content Only

- **Purpose.** Fix, as a binding constraint rather than an aspiration, that the OCR mechanism never itself reads evidence — it receives only content a caller has already retrieved through that caller's own, pre-existing `EvidenceCustodian.retrieve` dependency.
- **Responsibilities.** Confirm and test that Unit 1's own request shape accepts only already-retrieved bytes or a controlled, read-only handle to them — never a live, writable reference, and never a mechanism by which the OCR mechanism could retrieve content on its own initiative.
- **Dependencies.** Unit 1 only.
- **Inputs.** None of its own — this unit verifies a property of Unit 1's existing shape.
- **Outputs.** None of its own.
- **Constitutional constraints.** No `EvidenceCustodian` dependency is added anywhere by this unit, directly or transitively (Scope Lock §5, §13). This unit builds no new access path; it proves one was never created.
- **Files expected to change.** None beyond Unit 1's own file, if Unit 1's own shape requires clarification to satisfy this unit's verification.
- **Files explicitly prohibited.** Any file that imports or references `EvidenceCustodian` in any form.
- **Verification requirements.** A structural test confirming no type reachable from Unit 1's own request shape holds a reference to `EvidenceCustodian`, at any depth.
- **Completion criteria.** The structural test passes; no code path anywhere in Units 1–3 can retrieve evidence independently.

### Unit 5 — OCR Execution Pipeline

- **Purpose.** Complete the end-to-end path from a populated request through Unit 3's own sequencer to a fully-constructed result or failure disclosure.
- **Responsibilities.** Wire Units 1–4 together into one coherent, fully-testable pipeline. Introduce no new dependency beyond what Units 1–4 already established.
- **Dependencies.** Units 1, 2, 3, 4. Unit 7 is deliberately not a dependency of this unit — see Verification requirements, below, for exactly how this unit's own failure-distinction testing relates to Unit 7's later, concrete work.
- **Inputs.** A populated request, exactly as Unit 1 shapes it.
- **Outputs.** A result or failure disclosure, exactly as Unit 1 shapes it.
- **Constitutional constraints.** Identical to Unit 3's — no dependency beyond the adapter boundary; no self-triggering; no acceptance, rejection, or persistence act of any kind.
- **Files expected to change.** None beyond Units 1–4's own files, unless the pipeline itself requires a thin, dependency-free composition point among them.
- **Files explicitly prohibited.** Identical to Unit 3's prohibited list.
- **Verification requirements.** End-to-end hand-written-fake tests covering: a successful recognition at each of the three fidelity categories; a request carrying page/document scope, confirming page-aligned output where the scope supports it (Scope Lock §6); and provisional exercise of each of the seven failure distinctions, at the shape-level representation Unit 1 already establishes — sufficient to prove the pipeline reaches a distinct, non-fabricated outcome for each, but **not** the complete, constitutionally-required non-collapse verification, which is Unit 7's own responsibility and completes only once Unit 7's concrete representation exists.
- **Completion criteria.** The pipeline completes deterministically for a fixed input, provider adapter fake, and configuration — the same three reproduce the same disclosure, every time (Objective 6, §3 above). This unit's own provisional exercise of the seven failure distinctions is not, by itself, sufficient to satisfy Unit 7's own completion criteria (§8, Unit 7, below) — the two are independent, sequential milestones, and this unit's own completion does not substitute for Unit 7's.

### Unit 6 — OCR Output Model

- **Purpose.** Finalise the shape of the disclosure Unit 5 produces, ensuring it carries everything a future Evidence-Intelligence-side caller needs to construct a governed candidate, without itself constructing one.
- **Responsibilities.** Confirm the result shape carries recognised text (optionally page-aligned), a fidelity disclosure distinguishing verbatim/normalised/inferred-reconstruction portions where more than one applies within a single recognition (Evidence Intelligence Contract Design §5), a structured identity disclosure naming no concrete provider, and a working, transient confidence signal.
- **Dependencies.** Units 1, 5.
- **Inputs.** Unit 5's own successful result.
- **Outputs.** The finalised disclosure shape.
- **Constitutional constraints.** The disclosure is never itself a `CandidateEvidenceArtifact`, an `EvidenceAnalysisResult`, or any other governed record (Scope Lock §6). No durable confidence field is populated (Evidence Intelligence Contract Design §9).
- **Files expected to change.** None beyond Unit 1's own file, unless the disclosure shape requires refinement discovered during Unit 5's own testing.
- **Files explicitly prohibited.** Any file referencing `CandidateAssertion.confidence` or any other durable confidence field.
- **Verification requirements.** A test confirming verbatim, normalised, and inferred-reconstruction portions remain discoverable, not merely asserted in prose comments; a test confirming no durable field is ever written by any code path in Units 1–6.
- **Completion criteria.** The disclosure shape is complete, discoverable at the fidelity granularity Evidence Intelligence Contract Design §5 requires, and carries no durable field.

### Unit 7 — Failure Handling

- **Purpose.** Design the concrete representation of the Scope Lock's seven non-collapsible failure distinctions — the first tier at which Scope Lock §10 permits this, since the Contract Design itself confines the concrete taxonomy to future implementation.
- **Responsibilities.** Represent, distinctly and without collapsing any pair: not authorised (orchestration-layer, never reached by Units 1–6 themselves — see constraint below); unsupported or inaccessible input; no recognisable content; partial or technically degraded output; validation rejection; processing or dependency failure; genuine implementation fault.
- **Dependencies.** Unit 1's own failure shape only.
- **Inputs.** Whatever internal condition Units 3 and 5 detect during execution.
- **Outputs.** One of the seven distinct failure disclosures.
- **Constitutional constraints.** "Not authorised" is represented for completeness of the taxonomy but is never itself produced by any code path in Units 1–7 — it is, by construction, an orchestration-layer outcome that stops before any of these units is ever reached (Scope Lock §10). No two of the seven distinctions may ever be collapsed into a shared representation. "No recognisable content" and "partial or technically degraded output" are disclosed honestly; neither this unit nor any unit before it decides whether that disclosure means the result is worth producing as a candidate — that remains Evidence Intelligence's own judgement (Scope Lock §11).
- **Files expected to change.** Unit 1's own failure shape, extended with whatever concrete representation this unit's own engineering requires — still no provider-specific type, still no fourth fidelity category, still no eighth distinction.
- **Files explicitly prohibited.** Any file that conflates two of the seven distinctions into one representation.
- **Verification requirements.** A test asserting each of the seven distinctions produces a distinguishable outcome; a test asserting no code path in Units 1–7 ever produces "not authorised" (since that outcome, by construction, never reaches this far); failure-injection tests for at least one representative condition per distinction (a corrupted input for "unsupported or inaccessible input," a provider fake returning nothing for "no recognisable content," and so on).
- **Completion criteria.** All seven distinctions are independently producible and independently distinguishable in every test that exercises them; none is ever silently substituted for another.

### Unit 8 — Provenance-Supporting Disclosure

- **Purpose.** Confirm the disclosure Unit 6 finalises carries every fact a future provenance record needs, without the OCR mechanism itself constructing, writing, or holding any provenance record.
- **Responsibilities.** Confirm the disclosure carries: source evidence identity (sufficient to populate a future `extractedFrom`/`derivedFrom` reference); processing mechanism identity, naming no concrete provider; processing version and time, where available; page ordering, where meaningful; warnings and partial-result status; and sufficient information for a future output-hash computation, where applicable (Scope Lock §9).
- **Dependencies.** Units 1, 6.
- **Inputs.** Unit 6's own finalised disclosure.
- **Outputs.** Confirmation (by test, not by new code) that the disclosure is provenance-sufficient.
- **Constitutional constraints.** **No `Provenance` or `CandidateProvenance` value is constructed, written, or held by any file in Units 1–8.** The OCR mechanism holds no Memory Core dependency, at any depth (Scope Lock §13). Provenance construction remains exclusively a future Evidence-Intelligence-side responsibility, using Memory Core's existing, unmodified contract.
- **Files expected to change.** None beyond Unit 6's own file, unless a field is found missing during this unit's own verification.
- **Files explicitly prohibited.** Any file referencing `Provenance`, `CandidateProvenance`, or `MemoryCore` directly.
- **Verification requirements.** A test constructing a hypothetical `Provenance` value entirely from Unit 6's own disclosure fields, external to Units 1–8's own code, confirming no field is missing; a structural test confirming no file in Units 1–8 references `Provenance`, `CandidateProvenance`, or `MemoryCore`.
- **Completion criteria.** The hypothetical-provenance test passes for every disclosure Unit 5's own test suite produces, and the structural absence test passes.

### Unit 9 — Structural Isolation Proof

- **Purpose.** Prove, rather than assert, that the OCR mechanism holds none of the dependencies Scope Lock §13 excludes.
- **Responsibilities.** Write the structural test suite proving zero reachability, at any depth, to `EvidenceCustodian`, `MemoryCore`'s write interface, Knowledge Memory's Knowledge Submission interface, the Permission Engine, `EvidenceIntelligence`'s own public result handling, any runtime conversation component, any reporting mechanism, and Docling.
- **Dependencies.** Units 1–8, as the subject of its own tests. No production dependency of its own.
- **Inputs.** The compiled shape of Units 1–8.
- **Outputs.** A passing (or failing, and therefore blocking) structural verification result.
- **Constitutional constraints.** This unit builds no new capability; it exists solely to prove Units 1–8 introduced none of the eight excluded dependencies (Scope Lock §13).
- **Files expected to change.** None in production code; new test files only.
- **Files explicitly prohibited.** None beyond what Units 1–8 already prohibit — this unit's own tests must themselves avoid depending on any of the eight excluded items even as test scaffolding, other than through hand-written fakes that are not the real thing.
- **Verification requirements.** Reflection-based or equivalent structural tests, preferred over source-text pattern matching wherever a structural mechanism can prove the same invariant (Contract Design's own Verification Requirements section), covering all eight exclusions independently.
- **Completion criteria.** All eight structural-absence tests pass against the actual compiled shape of Units 1–8.

### Unit 10 — Evidence-Intelligence-Side Contract Sufficiency

- **Purpose.** Confirm, via hand-written fakes only, that a future Evidence-Intelligence-side caller could invoke Units 1–9's own pipeline successfully — without building that caller, its permission gate, or its trigger logic.
- **Responsibilities.** Write a hand-written fake standing in for "a future Evidence Intelligence caller," exercising Units 1–9's own public shape exactly as Evidence Processing's own coordinator tests exercised `EvidenceExtractor` before any real coordinator existed. Confirm the shape is sufficient; build nothing that makes it reachable in production.
- **Dependencies.** Units 1–9. No dependency in the reverse direction — Units 1–9 gain no reference to this unit's own fake.
- **Inputs.** A fake, Evidence-Intelligence-shaped caller's own request.
- **Outputs.** Confirmation that Units 1–9's own pipeline satisfies that fake caller's needs.
- **Constitutional constraints.** **This unit does not build, wire, or authorise the composition-level coordinator that consumes `RequiresOcr`** (Scope Lock §4, §18 item 2) — that remains separate, future governance and implementation work, not begun here. This unit does not touch any real Evidence Intelligence source file.
- **Files expected to change.** New test files only, containing the hand-written fake and its exercising tests. No production file outside Units 1–9's own.
- **Files explicitly prohibited.** Any file under Evidence Intelligence's own existing runtime or coordinator locations; any file implementing the `RequiresOcr`-consuming coordinator.
- **Verification requirements.** The fake-caller test suite exercises every branch Unit 5's own pipeline test suite already covers, from the perspective of a caller rather than the pipeline's own internals.
- **Completion criteria.** The fake-caller tests pass; no production code outside Units 1–9 exists as a result of this unit.

### Unit 11 — Verification

- **Purpose.** Consolidate and confirm every verification obligation Units 1–10 individually established, plus the cross-cutting categories §9, below, requires.
- **Responsibilities.** Run, and confirm passing, the full suite: unit tests, boundary tests, failure tests, immutability tests, provenance tests, provider-isolation tests, permission tests, structural safeguard tests, runtime integration tests (to the extent any exist — see Unit 12), and regression tests against Units 1–10's own prior test suites.
- **Dependencies.** Units 1–10, as the subject of verification. No new production dependency.
- **Inputs.** The compiled, tested shape of Units 1–10.
- **Outputs.** A consolidated verification result.
- **Constitutional constraints.** No verification method may itself require any of the eight dependencies Scope Lock §13 excludes, other than as a hand-written fake.
- **Files expected to change.** None in production code; consolidated test documentation or a test-suite index only, if the repository's own convention calls for one.
- **Files explicitly prohibited.** Identical to Unit 9's list.
- **Verification requirements.** §9, below, in full.
- **Completion criteria.** Every category in §9, below, passes; every structural safeguard in §10, below, passes.

### Unit 12 — Runtime Composition (Blocked)

- **Purpose.** Record exactly what remains before real runtime composition may begin — this unit performs no wiring itself.
- **Responsibilities.** None performed by this plan. Real runtime composition requires, at minimum: (a) resolution of the owner-control model for machine-triggered invocation (Scope Lock §18 item 1); (b) design and acceptance of the composition-level coordinator that consumes `RequiresOcr` (Scope Lock §18 item 2); (c) a decision on whether a dedicated Permission Engine proposal class is required for OCR invocation specifically, applying CDR-005 Model C (Scope Lock §18 item 3); and (d) a decision on output-quality validation policy and permission gating for rejected output (Scope Lock §18 items 4–5), to the extent runtime composition would otherwise expose ungated rejected-output disposal.
- **Dependencies.** Units 1–11, plus the four unresolved governance items above — none of which this plan resolves.
- **Inputs.** Not applicable.
- **Outputs.** Not applicable.
- **Constitutional constraints.** This plan authorises no `ParkerRuntime` wiring, no Resource or `ActionVocabulary` registration, and no Permission Engine proposal-class definition. Attempting any of these before the four items above are separately resolved would exceed both this plan's own authority and the Scope Lock's own.
- **Files expected to change.** None.
- **Files explicitly prohibited.** The runtime composition layer, or any other composition-root file, in any form, for any purpose connected to the OCR mechanism.
- **Verification requirements.** Not applicable until unblocked.
- **Completion criteria.** Not applicable until the four listed governance items are separately resolved and a future Scope Lock revision or amendment authorises this unit to proceed.

---

## 9. Verification Strategy

Every category the task requires, mapped to the units that satisfy it:

- **Unit tests** — Units 1, 2, 3, 6, 7 (shape and sequencing correctness).
- **Boundary tests** — Unit 4 (input contract), Unit 9 (dependency exclusion).
- **Failure tests** — Unit 7 (all seven distinctions, independently producible and distinguishable).
- **Immutability tests** — Unit 4, Unit 9 (no write path to any original, structurally proven).
- **Provenance tests** — Unit 8 (hypothetical-provenance construction from disclosure fields alone).
- **Provider-isolation tests** — Unit 2 (reflection-based confinement, mirroring Tika's own).
- **Permission tests** — Unit 7 ("not authorised" never produced by Units 1–7 themselves), Unit 9 (no Permission Engine reachability).
- **Structural safeguard tests** — Unit 9, §10 below, in full.
- **Runtime integration tests** — none exist until Unit 12 is unblocked; this category is explicitly not satisfiable today, and this plan does not claim otherwise.
- **Regression tests** — Unit 11, run against every prior unit's own suite before this plan's own completion criteria (§11, below) may be declared met.

---

## 10. Structural Safeguards

Required, and proven by test rather than asserted:

1. **Provider types do not escape public contracts** — Unit 2's own reflection test.
2. **Original evidence cannot be modified** — Unit 4's own structural absence-of-`EvidenceCustodian`-write-path test, reinforced by Unit 9.
3. **OCR output cannot become evidence directly** — Unit 6's own test confirming the disclosure is never a `CandidateEvidenceArtifact` or other governed record; Unit 10's own fake-caller test confirming a real caller, not the OCR mechanism, performs any eventual candidate construction.
4. **Provider replacement requires no constitutional change** — Unit 2's own abstraction, satisfiable by more than one hypothetical provider without altering Units 1, 3–11.
5. **No runtime component bypasses the coordinator** — not yet testable in production, since no runtime component exists (Unit 12, blocked); Unit 10's own fake-caller test is the closest available proxy today, confirming the *pipeline's own* invocation path is the sole path into Units 1–9.

---

## 11. Completion Criteria

Implementation of Units 1–11 is complete only when:

- every one of Units 1–11 individually meets its own completion criteria (§8, above);
- the verification strategy (§9, above) passes in full, with the explicit, disclosed exception of runtime integration tests, which remain not-yet-applicable pending Unit 12;
- constitutional boundaries — the Contract Design and Scope Lock as accepted — remain textually unchanged by any unit;
- all five structural safeguards (§10, above) pass, with structural safeguard 5 understood as passing against the pipeline-only invocation path available today;
- no dependency excluded by Scope Lock §13 exists anywhere in Units 1–11's own compiled shape;
- provider neutrality remains intact — no concrete provider is named, selected, or preferred anywhere in Units 1–11.

**Unit 12 is never part of this completion determination.** Implementation of Units 1–11 may be declared complete while Unit 12 remains blocked; the two are independent milestones, exactly as Evidence Processing's own Units 1–4B were declared complete and committed well before that programme's own Unit 5 (Production Composition) began.

---

## 12. Files Likely to Change

Identified at the directory/category level only, consistent with this document's own "no package names, no interfaces" style constraint — exact file and type names remain an engineering choice for whoever implements each unit:

- One or more new files within the existing interface-shape location this repository already uses for narrow, dependency-free capability contracts (Units 1, 2, 6, 7).
- One or more new files within the existing runtime/coordination location this repository already uses for sequencing logic with no platform-subsystem dependency (Units 3, 5).
- New test files, within this repository's existing test-suite structure, for every unit's own verification requirements (Units 1–11).
- A build-configuration change, if and only if a hand-written provider fake requires no new production dependency to compile (Unit 2) — no build-configuration change naming a real, concrete provider library is authorised by this plan.

---

## 13. Files That Must Not Change

- `docs/architecture/parker-constitution.md`
- `docs/decisions/CDR-006_CONSTITUTIONAL_CLASSIFICATION_OF_ORIGINAL_EVIDENCE_CUSTODY_AND_IMMUTABILITY.md`
- `docs/decisions/CDR-007_CONSTITUTIONAL_CLASSIFICATION_OF_EVIDENCE_INTELLIGENCE.md`
- `docs/architecture/OCR_MECHANISM_CONTRACT_DESIGN.md`
- `docs/architecture/OCR_MECHANISM_SCOPE_LOCK.md`
- `docs/architecture/EVIDENCE_INTELLIGENCE_CONTRACT_DESIGN.md`
- `docs/architecture/EVIDENCE_INTELLIGENCE_SCOPE_LOCK.md`
- `docs/architecture/EVIDENCE_PROCESSING_SEARCHABLE_PDF_SCOPE_LOCK.md`
- `docs/architecture/EVIDENCE_PROCESSING_SEARCHABLE_PDF_BOUNDARY_CLARIFICATION.md`
- Every Evidence Custodian governance document (`EVIDENCE_ARTIFACT_CONTRACT_DESIGN.md`, `EVIDENCE_CUSTODIAN_SCOPE_LOCK.md`, `EVIDENCE_CUSTODIAN_IMPLEMENTATION_PLAN.md`, `EVIDENCE_CUSTODIAN_PHASE_7_BOUNDARY_CLARIFICATION.md`)
- Memory Core's own Contract Design and Scope Lock
- Knowledge Memory's own governance (`PROGRAMME_3_KNOWLEDGE_MEMORY_CONTRACT_DESIGN_V2.md` and its Scope Lock)

No unit in this plan touches any file in this list, for any reason. A future implementer who discovers a genuine need to change one of them halts and returns to governance review — never a silent edit.

---

## 14. Explicitly Prohibited

No unit in this plan may introduce, or may be read as authorising:

- Docling, or any structured-document-conversion capability;
- a structured document model of any kind;
- reporting of any kind;
- Knowledge Submission, or any dependency on Knowledge Memory's own submission interface;
- Memory writes, or any dependency on `MemoryCore`'s public write interface;
- evidence acceptance, or any dependency on `EvidenceCustodian.accept`;
- Permission Engine redesign, or any new `PermissionAction`/`ResourceType` pairing;
- conversation triggering, or any attachment to the ordinary owner-conversation submission path or any other communication-layer entry point;
- background queues, or any scheduled or asynchronous invocation;
- provider-specific APIs reachable from any public contract this plan defines.

---

## 15. Provider Neutrality Compliance

This plan remains compatible with OCRmyPDF, Tesseract, EasyOCR, PaddleOCR, and any future provider, without choosing one: Unit 2's own adapter abstraction is the sole point any of them would eventually implement, and Unit 2's own structural test (§10, safeguard 1) guarantees no provider-specific type could leak past it regardless of which provider a future, separate governance decision eventually selects. No unit in this plan evaluates, compares, or expresses a preference among the four named providers or any other.

---

## 16. Blocked Work — Requires Future Governance

Restated once, consolidating every place above that names a block, as the single list a future governance stage must resolve before Unit 12 may begin:

1. Owner control and authorisation for machine-triggered OCR invocation (Scope Lock §18 item 1).
2. Design and acceptance of the composition-level coordinator that consumes Evidence Processing's own `RequiresOcr` disclosure (Scope Lock §18 item 2).
3. Whether a dedicated Permission Engine proposal class is required for OCR invocation specifically (Scope Lock §18 item 3).
4. Output-quality validation policy and threshold (Scope Lock §18 item 4).
5. Permission gating and disposition for rejected output (Scope Lock §18 item 5).
6. Concrete provider identity and adapter implementation (Scope Lock §18 item 6) — Unit 2's own abstraction may proceed without this; a concrete adapter satisfying it may not be written under this plan alone.
7. Process/execution and deployment topology (Scope Lock §18 item 7).
8. Exact Kotlin names, method signatures, and file layout (Scope Lock §18 item 8) — unlike items 1–7, this is not a block on any unit's own start; it is ordinary engineering discretion each unit's own "Files expected to change" field already defers, exercised unit-by-unit as Units 1–11 are actually implemented, never fixed by this plan itself.

Units 1–11 of this plan do not require items 1–7 to be resolved first, and exercise item 8 as ordinary engineering discretion throughout their own implementation. Unit 12 requires items 1–3 at minimum, and should not begin before items 4–5 are at least addressed to the extent runtime exposure of rejected output would otherwise be ungated.

---

## Final Recommendation

This Implementation Plan divides the OCR Mechanism Contract Design's and Scope Lock's already-accepted boundary into eleven fully-authorised, fully-testable units and one explicitly blocked unit, without redesigning, reopening, or reinterpreting either governing document, CDR-006, CDR-007, or Amendment 2. It resolves no constitutional question those documents did not already resolve. Independent constitutional review of this document is the required next step before Units 1–11 may begin; this document does not authorise itself.

OCR MECHANISM IMPLEMENTATION PLAN — DRAFT — AWAITING INDEPENDENT CONSTITUTIONAL REVIEW

Confirmed: no Kotlin implemented; no interface, package, or provider API named; the Contract Design, the Scope Lock, CDR-006, CDR-007, Amendment 2, the Evidence Intelligence Contract Design/Scope Lock, and the Evidence Processing Boundary Clarification/Scope Lock not modified; nothing staged; nothing committed; nothing pushed.
