# OCR Mechanism — Contract Design

## Status

**Contract design only. Not an amendment, not a Scope Lock, not an Implementation Plan, not an implementation.** No Kotlin is implemented, proposed as a diff, or changed by this document — every shape below is described in prose, by purpose, obligation, and relationship, never as a Kotlin interface, class, enum, or method signature, mirroring exactly the "implementation independence" discipline `EVIDENCE_INTELLIGENCE_CONTRACT_DESIGN.md`'s own Status section and `REASONING_PROVIDER_CONTRACT_DESIGN.md`'s own Status section already established. No storage technology, database schema, hashing algorithm, or wire format is specified. No concrete OCR engine, library, or service — OCRmyPDF, Tesseract, PaddleOCR, EasyOCR, or any other — is chosen, named, evaluated, or implied anywhere in this document. Neither `src/` nor `tests/` is touched. No Scope Lock and no Implementation Plan is produced by this document. Nothing is staged, committed, or pushed.

**Repository baseline confirmed before this document was drafted:** `main` at `89bd6ad754017ae02d8de90d95d71ee52d5fdd1a`, working tree clean, matching `docs/reviews/EVIDENCE_INTELLIGENCE_CONTRACT_DESIGN_AMENDMENT_2_FINAL_ACCEPTANCE_CONFIRMATION.md`'s own recorded acceptance of Amendment 2.

This document is the "future, separate OCR mechanism Contract Design" that `EVIDENCE_INTELLIGENCE_CONTRACT_DESIGN.md`'s own Amendment 2 (§12; Status section) identified as still required — the document that owns the OCR mechanism dependency's own concrete contract, mirroring exactly how `REASONING_PROVIDER_CONTRACT_DESIGN.md` owns `ReasoningProvider`'s own contract as a dependency of the subsystems that orchestrate it. This document does not redesign, reopen, or reinterpret Amendment 2, the Evidence Intelligence Contract Design, the Evidence Intelligence Scope Lock, CDR-006, or CDR-007 — each is treated as controlling and unmodified throughout.

---

## Context and Constitutional Basis

CDR-007 classifies "document ingestion, OCR, transcription, and extraction" as "Evidence Intelligence's own analytical functions" (§1, Decision Rules) and names "an OCR transcription" by example under both its "Producing candidate derivative artefacts" responsibility (Evidence Intelligence Contract Design §1) and its permitted "Candidate artefact produced" output category (§5, §6). Amendment 2 completed the mechanical step CDR-007's own classification implied but had not yet made executable: authorising a concrete-in-principle, abstract-in-shape OCR mechanism as a fourth Evidence Intelligence dependency, structurally parallel to the existing `ReasoningProvider` dependency, explicitly as a capability-level addition *within* the existing Evidence Intelligence subsystem — never a new peer subsystem, never itself owning custody, promotion, or truth authority.

This document performs the next, and only the next, required step: defining what that dependency's own contract *is* — its responsibilities, its input and output shape, its failure taxonomy, its provenance obligations, and its constitutional constraints — with no concrete engine, Kotlin type, or runtime composition attached to any of it. Exactly as `REASONING_PROVIDER_CONTRACT_DESIGN.md` once defined `ReasoningProvider` as an abstract, pluggable capability before any concrete reasoning provider existed, this document defines the OCR mechanism the same way.

**The constitutional principle governing every responsibility below, restated verbatim from the documents this one is built on and applied literally:** *the OCR mechanism transforms image content Evidence Intelligence already holds a governed reference to into recognised text, and nothing more — it acquires no authority over truth, custody, evidential classification, or knowledge promotion, exactly as CDR-007 already requires of Evidence Intelligence itself, and exactly as the Reasoning Provider Contract Design already requires of `ReasoningProvider`.*

---

## 1. Responsibilities

The OCR mechanism is responsible for, and only for, one narrow act: **interpreting image content into recognised text**, performed only when Evidence Intelligence's own analysis requires it (Amendment 2, Contract Design §12 row: "invoked only when an analysis's own input requires image-to-text interpretation"). This is the interpretive act the Evidence Processing Boundary Clarification's own exclusion list names as "OCR — interpreting an image into text" (§2) and that the OCR Ownership and Sequencing Review's own Section 7 finding places, by CDR-007's own classification, with Evidence Intelligence, never with Evidence Processing.

Concretely, and only:

- **Recognising text from image content.** Given image content Evidence Intelligence already holds a governed reference to, produce the text a human reader would recognise in that image — nothing evaluative, nothing corroborative, nothing beyond the recognition act itself.
- **Disclosing recognition fidelity.** Where recognition is machine-produced but not independently verified, uncertain, partial, normalised, or requires reconstructing an illegible or ambiguous passage, disclose which of the Evidence Intelligence Contract Design's own fidelity categories (§5, as amended by `OCR_TRANSCRIPTION_FIDELITY_VERIFICATION_AMENDMENT.md`: verbatim transcription, unverified literal transcription, normalised transcription, inferred reconstruction) the output — or which portion of it — represents. The OCR mechanism binds its output to those four governed categories and invents no fifth category.
- **Disclosing recognition identity.** Report, as a structured fact rather than free narrative prose (mirroring `ExtractionIdentity`'s own already-established "one structured record, not independently-worded sentences" discipline, Evidence Processing Boundary Clarification §4), what mechanism configuration produced a given recognition — without naming a concrete engine, which remains unspecified by this document.
- **Disclosing an honest, working confidence signal, where genuinely available.** Consistent with the Evidence Intelligence Contract Design's own Confidence Model (§9), which already permits "an extraction confidence" as ordinary, transient analytical machinery — never a durable, constitutional confidence value.

The OCR mechanism does not decide whether its own output is good enough to become a governed artefact; that judgement belongs to Evidence Intelligence's own analysis (§6, below).

---

## 2. Explicit Non-Responsibilities

The OCR mechanism does **not**, under any circumstance, and this document authorises no future implementation to change any item below without a further governance decision at the appropriate tier:

- **Decide truth.** Truth-of-content determination belongs to no subsystem (Epistemic Integrity Article III, VI, VII); the OCR mechanism's recognised text is, at most, a candidate representation of what an image shows, never an assertion of what is true.
- **Accept evidence.** The OCR mechanism holds no dependency on `EvidenceCustodian.accept`, and never itself causes anything to enter custody. Acceptance remains the separate responsibility the Evidence Intelligence Contract Design already assigns elsewhere (§6), exercised only by whatever already-governed acceptance orchestration consumes Evidence Intelligence's own output.
- **Reject evidence.** The OCR mechanism has no authority to determine that its own output is unacceptable and thereby prevent Evidence Intelligence from producing it as a candidate; it discloses fidelity and confidence honestly (§1, above) and lets Evidence Intelligence's own analytical judgement decide what to do with that disclosure. Whether a disclosed-poor recognition warrants its own gating mechanism is future governance (§10, below), not decided here.
- **Modify original evidence.** The OCR mechanism holds no write, update, or delete operation of any kind over any original. CDR-006's own Constitutional Optimisation Safeguard names this exact scenario by illustration — "deleting an original after optical character recognition" — as permanently forbidden, for any subsystem, without exception; this document restates that prohibition as binding on the OCR mechanism specifically, not as a new rule.
- **Write Memory.** The OCR mechanism holds no dependency on `MemoryCore`'s public write interface, and never itself registers, creates, or modifies a Memory Core record.
- **Perform Knowledge Submission.** The OCR mechanism holds no dependency on Knowledge Memory's Knowledge Submission interface, and never itself submits, promotes, or evaluates a Knowledge Candidate.
- **Hold custody, at any depth.** No dependency, at any depth, on `OwnerEvidenceDeletionAuthority`, `EvidenceArtifactStorage.delete`, or `EvidenceDeletionAudit` — mirroring the identical exclusion the Evidence Intelligence Contract Design (§12) already fixes for Evidence Intelligence itself.
- **Assign evidential state.** No type this document describes carries, or could carry, an `EvidentialState` value. Final evidential-state assignment remains exclusively Knowledge Memory's (Article XV; Programme 3 Contract Design V2 §4).
- **Constitute a truth authority or a constitutional classifier.** Restated from Amendment 2's own dependency-table row (Evidence Intelligence Contract Design §12): the OCR mechanism "never [possesses] authority over truth, is a constitutional classifier, or is capable of assigning `EvidentialState` or determining what its own output means."
- **Authorise its own invocation.** The OCR mechanism holds no Permission Engine dependency of its own — mirroring exactly `ReasoningProvider`'s and `EvidenceIntelligence`'s own identical "gated by what calls it or by what composes it, never by a mechanism of its own" shape. Who or what may lawfully trigger a given invocation is owner authority (§9) and future governance (§10), never this mechanism's own decision.
- **Select or implement a concrete engine.** This document names no provider, library, or service, and authorises none by implication.
- **Perform orchestration, runtime composition, or dependency injection.** How the OCR mechanism is wired into `ParkerRuntime`, invoked, or composed with any coordinator is explicitly out of scope (§10, §11, below).

---

## 3. Shape — Described in Prose Only

Consistent with the "no language syntax is specified anywhere in this document" discipline the Evidence Intelligence Contract Design and Reasoning Provider Contract Design both already establish, the OCR mechanism's own shape is described here by relationship, not by declared type:

- **One capability, one act.** The OCR mechanism performs one thing: given image content Evidence Intelligence already holds a governed reference to, and the fixed set of already-established context facts Evidence Intelligence supplies alongside it, produce recognised text together with the fidelity, identity, and confidence disclosures §1 requires. It holds no session, run, or multi-step lifecycle of its own — the same reason `ReasoningProvider` and `EvidenceIntelligence` each need none (Reasoning Provider Contract Design §1; Evidence Intelligence Contract Design §10).
- **No new evidence-artefact type.** Any text the OCR mechanism recognises is carried, once Evidence Intelligence chooses to produce it as a candidate, by the existing, unmodified `CandidateEvidenceArtifact` type Evidence Custodian already owns (Evidence Intelligence Contract Design §5, "Candidate artefact produced," already naming "an OCR transcription" by example) — never a new, OCR-specific artefact shape.
- **No new provenance type.** Every provenance fact the OCR mechanism's own output requires is carried by Memory Core's existing, unmodified `Provenance`/`CandidateProvenance` contract (§7, below) — never a parallel or mirrored mechanism.
- **No new public interface, type, or authority is introduced by this document.** This document defines a capability's *contract* — its responsibilities and shape — never a Kotlin abstraction realising it.

---

## 4. Inputs

Every input the OCR mechanism may consume is already-retrieved, already-governed content or fact Evidence Intelligence itself supplies — never something the mechanism independently fetches:

- **Already-retrieved image content**, obtained by Evidence Intelligence through its own existing `EvidenceCustodian.retrieve` dependency before the OCR mechanism is ever invoked. The OCR mechanism holds no `EvidenceCustodian` dependency of its own — mirroring `ReasoningProvider`'s own "pure callee, calls nothing" shape exactly (Reasoning Provider Contract Design; Evidence Processing Boundary Clarification §3, describing the identical shape for `EvidenceExtractor`).
- **Detected media type and page context**, passed through unchanged from whatever upstream detection produced it (for example, Evidence Processing's own `RequiresOcr` disclosure, where that is the originating signal) — the OCR mechanism does not re-derive or second-guess this classification; it is a fact supplied to it, not one it independently determines (§6, §7, below).
- **No caller-declared confidence or evidential-state value.** Mirroring the Evidence Intelligence Contract Design's own §4 discipline exactly, no input to the OCR mechanism may carry a confidence figure or evidential classification a caller supplies — any confidence this mechanism reports is its own, working, transient output only (§1, above).

This document does not decide, and leaves entirely to future implementation, the concrete mechanism by which these inputs are assembled or passed (§11, below).

---

## 5. Outputs

Every output the OCR mechanism may produce, and only these:

- **Recognised text** — a candidate representation of what the supplied image content shows, never yet accepted anywhere, never yet a governed artefact.
- **A fidelity disclosure**, binding the output to one of the Evidence Intelligence Contract Design's own four governed categories (§5, as amended by `OCR_TRANSCRIPTION_FIDELITY_VERIFICATION_AMENDMENT.md`): verbatim transcription, unverified literal transcription, normalised transcription, or inferred reconstruction — or, where more than one applies within a single recognition, which portions are which. This document introduces no fifth category. Ordinary machine recognition success is not, by itself, sufficient to classify output as verbatim.
- **A structured identity disclosure** — what configuration produced this recognition, in the same "one structured record, not independently-worded sentences" shape Evidence Processing's own `ExtractionIdentity` already established — without naming a concrete engine.
- **A working confidence signal, where genuinely available** — transient, never durable, never written to `CandidateAssertion.confidence` or any other durable field, exactly as the Evidence Intelligence Contract Design's own Confidence Model (§9) already requires of every analytical figure Evidence Intelligence computes.

**What the OCR mechanism's output is not:** it is not itself a `CandidateEvidenceArtifact`, an `EvidenceAnalysisResult`, or any other governed record. It is the raw material Evidence Intelligence's own analysis uses to decide whether, and how, to produce a governed `CandidateEvidenceArtifact` under its own existing, unmodified §5 output taxonomy. The OCR mechanism itself constructs no governed record of any kind.

---

## 6. Failure Model

The OCR mechanism's failures fall into three kinds, each belonging to a different responsible party:

**Detection failure — belongs to Evidence Processing, and never reaches the OCR mechanism at all.** Whether a document requires OCR in the first place is Evidence Processing's own, already-governed, terminal determination (Boundary Clarification Determination 1, 2; OCR Ownership and Sequencing Review §6, "Detection Ownership"). The OCR mechanism is never invoked to make or revisit that determination, and this document creates no path by which it could be.

**Recognition-quality failure — belongs to Evidence Intelligence's own analytical judgement, informed by the OCR mechanism's own honest disclosure.** Where the OCR mechanism's own recognition is genuinely insufficient — too degraded, too ambiguous, or too incomplete to be worth producing as a candidate — the mechanism's own responsibility is limited to disclosing that honestly (a low or absent confidence signal, an inferred-reconstruction fidelity marking, or an explicit "no usable recognition" outcome); it never silently upgrades a poor recognition into a plausible-looking one, and it never itself decides that the recognition should therefore be discarded, rejected, or withheld — that determination is Evidence Intelligence's own, exactly as Evidence Intelligence already decides, for any other analytical step, whether a result is worth producing as a `CandidateArtifactProduced` or only as `TransientOutput` (Evidence Intelligence Contract Design §5). Whether this determination eventually warrants its own disclosed permission gate is future governance (§10, below), not decided here.

**Operational/mechanical failure — belongs to a future, concrete OCR mechanism implementation, never to this document, to Evidence Processing, or to Evidence Intelligence's own contract.** Engine faults, timeouts, resource exhaustion, unsupported input, and similar mechanical failures are the OCR mechanism's own implementation-level concern to signal, by whatever mechanism a future implementation chooses, outside any type this document describes — mirroring exactly how the Evidence Intelligence Contract Design's own Failure Model (§11) treats a faulting `ReasoningProvider` invocation as "Evidence Intelligence's own concrete implementation's concern to signal," not a constitutional question.

**Which failures are constitutional.** Only one class of failure is constitutional, rather than operational: any attempted act this document's §2 forbids — modifying an original, writing Memory, submitting Knowledge, assigning `EvidentialState`, or authorising its own invocation. These are prevented structurally, by the OCR mechanism holding no dependency capable of performing any of them (§2, §8, below), not merely policed as a runtime failure mode. No such attempt is a "failure" in the operational sense above; it is a structural impossibility this document's own dependency shape guarantees.

---

## 7. Provenance Preservation

The OCR mechanism creates no independent provenance model of its own, exactly as the Evidence Intelligence Contract Design already requires of Evidence Intelligence itself (§8):

- Any `CandidateEvidenceArtifact` Evidence Intelligence constructs from the OCR mechanism's own recognised text carries a `CandidateProvenance` whose `extractedFrom`/`derivedFrom` field names the original evidence artefact the image content came from — the same, existing, unmodified mechanism CDR-006's own mandatory traceability rule already requires of every derivative.
- The fidelity disclosure §5 requires (verbatim, normalised, or inferred reconstruction) must be preserved into whatever record eventually carries it, exactly as the Evidence Intelligence Contract Design's own §5 transcription-fidelity paragraph already requires — this document adds no new field, no new provenance type, and no relaxation of Article VIII or Article IX's obligations.
- No new field, and no new provenance-carrying type, is introduced anywhere in this document. Where the OCR mechanism's own recognition cannot establish a fact with confidence, that uncertainty is represented honestly through Memory Core's existing, already-nullable provenance fields — never fabricated, never silently omitted.

---

## 8. Constitutional Constraints — Authority Never Possessed

Restated once more, in full, as the single load-bearing list a future reviewer may check this document and any future implementation against — every item below traces to CDR-006, CDR-007, Epistemic Integrity Amendment No. 1, or the Parker Constitution directly, and none is a new rule this document invents:

- **No truth authority** (Epistemic Integrity Article III, VI, VII; CDR-007 §2 — "Not a truth authority").
- **No custody, modification, or deletion authority over any original** (CDR-006's Constitutional Optimisation Safeguard, naming OCR explicitly).
- **No acceptance authority** — cannot cause anything to enter Evidence Custodian, Memory Core, or Knowledge Memory (Evidence Intelligence Contract Design §6, §12).
- **No evidential-classification authority** — cannot assign `EvidentialState` (Article XV; Programme 3 Contract Design V2 §4).
- **No independent epistemic- or trust-authorisation mechanism** — cannot authorise its own invocation, cannot bypass any Permission Engine gate at any boundary it or its caller touches (Constitution: "No intelligence within Parker is trusted with unchecked authority"; "No capability may bypass trust").
- **No self-granted authority** — cannot expand its own scope or acquire a capability this document does not name (Constitution: "No module may grant itself authority").

---

## 9. Owner Authority

Consistent with the Constitution's own "the owner remains in control" principle and Parker User Rights ("revoke permissions"), the following remain, unconditionally, decisions for the Parker instance owner, exercised through the Permission Engine — never assumed, defaulted, or exercised by the OCR mechanism, Evidence Intelligence, or any coordinator on the owner's behalf:

- **Whether OCR capability is available at all** for a given Parker instance — an ordinary module-enablement decision, no different in kind from enabling any other capability (Constitution: "Modules... supply what Parker can do. They never supply what Parker is allowed to do").
- **Whether a specific invocation is authorised** — mirroring the Evidence Intelligence Scope Lock's own already-frozen "invoke Evidence Intelligence" gate (§6 step 0, §9, §11): whatever composes the OCR mechanism into the running system remains responsible for Permission Engine evaluation before invocation, exactly as it already is for Evidence Intelligence's own operation. This document does not design that gate's exact shape (§10, below); it confirms only that the owner's authority over it is preserved, never bypassed, by anything this document describes.
- **Revocation** — the owner may withdraw authorisation for OCR capability at any time, without needing to justify the withdrawal (Parker User Rights).

---

## 10. Deferred to Future Scope Lock Work

This document identifies, and deliberately does not resolve, the following — each requires its own future Scope Lock (or, where a permission-relevant domain act is discovered, CDR-005 Model C self-certification) before implementation may proceed:

1. **Owner control and authorisation for machine-triggered OCR invocation.** Amendment 2 itself named this an open question against Constitutional Test 1, not decided by anything reviewed; this document does not decide it either. What principal, authorisation basis, or owner-visible control point attaches to an invocation triggered by a prior pipeline outcome rather than a human request remains open.
2. **Permission gating and disposition of OCR output rejected during output-quality validation.** Whether a disclosed-poor recognition (§6, above) warrants its own dedicated `PermissionAction`/`ResourceType` pairing, distinct from ordinary analytical judgement, is not decided here — the existing CDR-005 Model C self-certification path remains available to answer it when a future Scope Lock reaches the question.
3. **Which composition-level mechanism consumes Evidence Processing's own `RequiresOcr` disclosure and triggers an Evidence Intelligence analysis in response** — already identified (OCR Mechanism Amendment Proposal §7) as requiring no Contract Design change, but requiring its own future Scope Lock/Implementation Plan design.
4. **Whether a dedicated Permission Engine proposal class is required for OCR invocation specifically**, as distinct from the already-frozen general "invoke Evidence Intelligence" gate — a CDR-005 Model C question, not exercised by this document.
5. **Output-quality validation policy** — what threshold, if any, separates a recognition worth producing as a candidate from one that is not. This document identifies the category of judgement (§6, above); it does not supply the policy.

---

## 11. Deferred to Future Implementation

Not decided, chosen, or implied by this document, and not authorised to begin by it:

- **Concrete engine, library, or service identity** — no provider is selected, evaluated, or ruled in or out.
- **Any Kotlin interface, class, enum, or method signature.**
- **Runtime composition or dependency injection** — how the OCR mechanism is wired into `ParkerRuntime` or any composition root.
- **Process or execution boundary** — whether the mechanism runs in-process, as a subprocess, or in an isolated container; the OCR Planning Review (§3.8) already identified this as a materially new risk category this document does not resolve.
- **Any concrete failure-taxonomy type** — §6's three-way classification is a constitutional/organisational one, not a Kotlin sealed type.
- **Reporting of any kind.**

---

## 12. Dependency Model

The OCR mechanism holds no dependency of its own — mirroring exactly `ReasoningProvider`'s "pure callee, calls nothing" shape. It is Evidence Intelligence that depends on the OCR mechanism (Evidence Intelligence Contract Design §12, as amended by Amendment 2), never the reverse; the OCR mechanism holds no reference back to `EvidenceIntelligence`, `EvidenceCustodian`, `MemoryCore`, Knowledge Memory, or the Permission Engine, at any depth. This is the same "holds no dependency, at any depth" discipline the Evidence Intelligence Contract Design's own §12 already fixes for Evidence Intelligence itself, applied here one tier further out.

---

## 13. Repository Reuse

**Reused, unmodified:**

- `EvidenceArtifactId`, `CandidateEvidenceArtifact` — Evidence Custodian's own contract; the OCR mechanism never constructs these directly, but the candidate Evidence Intelligence eventually produces from its output reuses them unchanged.
- `Provenance`, `CandidateProvenance`, including `extractedFrom`/`derivedFrom` — Memory Core's own contract, reused exactly as §7 describes.
- The Evidence Intelligence Contract Design's own four-category `EvidenceAnalysisResult` taxonomy (§5) — already sufficient for OCR output; no new category is introduced.
- The Evidence Intelligence Contract Design's own verbatim/normalised/inferred-reconstruction fidelity taxonomy (§5) — reused exactly, not extended.
- The Evidence Intelligence Contract Design's own Confidence Model (§9) — reused exactly for the OCR mechanism's own working confidence signal.
- `EvidentialState` — never touched, never referenced by anything this document defines.

**Not adopted, and explicitly rejected as unnecessary:**

- A new evidence-artefact type, a new provenance type, a new fidelity taxonomy, or a new confidence mechanism of the OCR mechanism's own — each already exists and already suffices.
- A concrete engine or provider abstraction of any kind — deferred in full (§11).

---

## Out of Scope

This document does not define, and no future reader should treat it as having defined:

- provider selection, including OCRmyPDF, Tesseract, PaddleOCR, EasyOCR, or any other concrete engine;
- Docling, or any structured-document-conversion capability;
- runtime composition, dependency injection, or any `ParkerRuntime` wiring;
- any Kotlin interface, class, enum, or method signature;
- any Permission Engine proposal class, `PermissionAction`, or `ResourceType` naming;
- owner-trigger or machine-trigger invocation rules;
- reporting of any kind;
- output-quality validation policy or threshold;
- any amendment to Memory Core's, Evidence Custodian's, Knowledge Memory's, or Evidence Processing's own contract, schema, or interface — none is proposed, and none is required by anything in this document;
- a Scope Lock or Implementation Plan for the OCR mechanism — both remain the next, separate governance stages.

Where any of the above proved necessary to state precisely, it is identified above (§10, §11) as future governance, never solved here.

---

## Verification Requirements

Properties a future Scope Lock and Implementation Plan must be capable of demonstrating, by whatever concrete mechanism they choose, preferring structural verification over source-text pattern matching wherever possible:

- **No dependency reachability.** No type reachable from the OCR mechanism's own input or output holds a reference, at any depth, to `EvidenceCustodian.accept`, `MemoryCore`'s public write interface, Knowledge Memory's Knowledge Submission interface, `OwnerEvidenceDeletionAuthority`, `EvidenceArtifactStorage.delete`, `EvidenceDeletionAudit`, or the Permission Engine.
- **No durable confidence or evidential-state authority.** No type this document describes carries an `EvidentialState` field or writes to any durable confidence field.
- **No independent content-fetch path.** The OCR mechanism never itself calls `EvidenceCustodian.retrieve`; it only ever receives content already retrieved by its caller.
- **Fidelity distinguishability.** Verbatim, normalised, and inferred-reconstruction portions of any recognition remain discoverable, not merely asserted in prose.
- **No self-invocation authority.** No implementation of the OCR mechanism holds a Permission Engine reference of its own.

---

## Final Recommendation

This document defines the OCR mechanism's contract exactly as Amendment 2 authorised it to be defined: a capability-level dependency of Evidence Intelligence, never a peer subsystem, holding no dependency of its own, introducing no new public type, no new provenance mechanism, and no new output category — and never possessing authority over truth, custody, evidential classification, or knowledge promotion. It resolves no constitutional question CDR-006, CDR-007, or Amendment 2 did not already resolve, and reopens none of them. It chooses no concrete engine, no Kotlin shape, and no runtime composition, leaving each to the future governance and implementation stages this document names explicitly (§10, §11).

The next governance stage — an OCR Mechanism Scope Lock — is authorised to begin only after independent constitutional review of this document; neither it nor an Implementation Plan is begun here.

OCR MECHANISM CONTRACT DESIGN — DRAFT — AWAITING INDEPENDENT CONSTITUTIONAL REVIEW

Confirmed: no Kotlin implemented; no API, schema, or storage technology defined; no concrete OCR engine named; CDR-006, CDR-007, the Evidence Intelligence Contract Design, the Evidence Intelligence Scope Lock, and Amendment 2 not modified; the Evidence Processing Boundary Clarification and Scope Lock not modified; nothing staged; nothing committed; nothing pushed; OCR Mechanism Scope Lock not started.
