# Evidence Intelligence Contract Design — OCR Mechanism Dependency Amendment Proposal

## Status

**Proposal only. Not an amendment.** No canonical governance document is amended by this document. No Kotlin is implemented, proposed as a diff, or changed. Neither `src/` nor `tests/` is touched. Nothing is staged, committed, or pushed.

**Repository baseline confirmed before this analysis:** `main` at `eac43cb` ("feat: implement searchable PDF evidence processing"), working tree containing exactly two untracked files — `docs/reviews/EVIDENCE_PROCESSING_UNIT_5_OCR_PLANNING_REVIEW.md` and `docs/reviews/EVIDENCE_PROCESSING_OCR_OWNERSHIP_AND_SEQUENCING_REVIEW.md` — matching the expected state exactly. No other file is modified by this document's preparation.

This document is the direct output of `EVIDENCE_PROCESSING_OCR_OWNERSHIP_AND_SEQUENCING_REVIEW.md` §17 ("Recommended Next Step"), itself building on that review's Section 10 (Evidence Intelligence Ownership), Section 11 (Meaning of `RequiresOcr`), and Section 14 (Governance Vehicle Required). It identifies exactly what a future Evidence Intelligence Contract Design amendment — and, where the Scope Lock merely mirrors the Contract Design, a matching Scope Lock update — would need to change so that Evidence Intelligence can lawfully depend on a concrete OCR mechanism. It is a planning input to that future amendment, not the amendment itself, styled and statused exactly after `EVIDENCE_INTELLIGENCE_CONTRACT_DESIGN_AMENDMENT_1_PROPOSAL.md`'s own precedent (itself now accepted and folded into the Contract Design as "Amendment 1").

---

## 1. Amendment Objective

Authorise a concrete OCR mechanism as a fourth Evidence Intelligence dependency — structurally parallel to the existing `ReasoningProvider` row — so that `EvidenceIntelligence.analyse` may internally invoke it exactly as CDR-007 and this Contract Design's own §1 Responsibilities already say Evidence Intelligence is responsible for doing ("an OCR transcription" is already named there as an example of "Producing candidate derivative artefacts"). No new `EvidenceAnalysisResult` variant, no new public type, no new responsibility, and no change to acceptance orchestration is required — this is a narrow dependency-list correction, not a redesign, mirroring exactly the "downstream currency update, not a redesign" framing Amendment 1 itself used.

---

## 2. Exact Paragraphs Requiring Amendment

### 2.1 — Contract Design §12 Dependency Model, opening framing

**Current text:** "Every dependency below already exists in this repository's governed contracts. **No new platform subsystem is introduced by this document.**"

**Why stale:** This sentence is the load-bearing constraint any OCR-mechanism dependency must confront. A concrete OCR engine (OCRmyPDF, Tesseract, or any other mechanism named later) does not already exist as a governed Parker contract — unlike `EvidenceCustodian.retrieve`, `MemoryRetrieval`, and `ReasoningProvider`, each reused from an already-accepted Contract Design. Naming an OCR mechanism as a fourth dependency would, for the first time, introduce a genuinely new platform subsystem, directly contradicting this sentence as presently written.

**Smallest amendment:** Qualify the sentence rather than delete it: "Every dependency below already exists in this repository's governed contracts, or is authorised by name below as a new, narrowly-scoped platform dependency (Amendment 2, OCR mechanism)." Preserves the sentence's protective intent (no *undisclosed* new subsystem) while accommodating the one, explicitly-named exception.

### 2.2 — Contract Design §12 Dependency Model, the dependency table

**Current text:** the three-row table listing `EvidenceCustodian.retrieve`, `MemoryRetrieval`, and `ReasoningProvider` (zero or more) as the entire dependency set.

**Why stale:** Silent on OCR, not merely outdated — no row names any mechanism capable of interpreting a scanned image into text, yet §1 Responsibilities and §5 Outputs (§3, below) already describe Evidence Intelligence producing exactly that kind of artefact.

**Smallest amendment:** Add a fourth row, structurally mirroring the `ReasoningProvider` row's own *shape*, not its content: an OCR mechanism (zero or one) | Evidence Intelligence → OCR mechanism | Internal analytical mechanism, orchestrated, never itself, invoked only when an analysis's own input requires image-to-text interpretation | *not yet governed — authorised in principle by this amendment; its own concrete contract remains a future, separate Contract Design's responsibility (§4, below)*. No Kotlin name, method signature, interface, or concrete engine is specified anywhere in this row — mirroring the Contract Design's own "no language syntax is specified anywhere in this document" discipline (Status section) and the Scope Lock's own precedent for the "Candidate record produced" selector type ("No Kotlin name, package, method, or interface is assigned to this type by this document"). The fourth column deliberately does not read "already exists" — unlike the three existing rows, this dependency does not yet exist in any governed contract; the table must say so plainly rather than imply otherwise. **The row's own "Purpose" text must carry forward, explicitly, the rule already binding on every parser this repository governs:** no parser, no OCR engine, and no external library this dependency may eventually name ever possesses authority over truth (Ownership and Sequencing Review §12, Cross-Programme Boundary: "No parser — Tika today, any future OCR engine — ever holds authority over truth"; CDR-007 §2, "Not a truth authority"). This dependency remains, and can only ever be authorised as, a mechanical, orchestrated mechanism — exactly the same standing `ReasoningProvider` already has (CDR-007 §1: "a pure callee with no custody, provenance, or promotion obligations of its own") — never a truth authority, never a constitutional classifier, and never itself capable of assigning `EvidentialState` or determining what its own output means.

### 2.3 — Scope Lock §4 Dependency Freeze, the mirrored table

**Current text:** "Exactly which public contracts may be depended upon is frozen at precisely the Contract Design's own §12 table — no more, no less," followed by a verbatim duplicate of the same three-row table.

**Why stale:** The freeze *mechanism* itself needs no new prose — it already ties itself mechanically to whatever the Contract Design's own §12 table says. But the duplicated table text inside the Scope Lock would, left unedited, silently diverge from an amended Contract Design table, creating exactly the kind of two-copies-of-one-fact drift this governance discipline elsewhere forbids (`ExtractionIdentity`'s own "one structured record, not three independently-worded sentences" reasoning, Evidence Processing Boundary Clarification §4, is the same principle applied to prose here).

**Smallest amendment:** Add the identical fourth row to the Scope Lock's own duplicated table, with no other change — the freeze's own governing sentence already extends automatically.

### 2.4 — Contract Design §12, the "holds no dependency, at any depth, on" eight-item exclusion list

**Current text:** "`OwnerEvidenceDeletionAuthority`; `EvidenceArtifactStorage.delete`; `EvidenceDeletionAudit`; `EvidenceCustodian.accept`; `MemoryCore`'s public write interface; Knowledge Memory's own Knowledge Submission interface; Knowledge Memory's own promotion, revision, retirement, or restoration mechanisms; or `EvidentialState`."

**Why this one is *not* stale:** An OCR mechanism implicates none of these eight — it is a read/transform mechanism analogous to `ReasoningProvider`, never a custody, deletion, promotion, or evidential-classification authority. This paragraph remains fully accurate and requires **no change**. Listed here only to show it was checked, not skipped.

### 2.5 — Scope Lock §4, "No fifth new public type or interface is authorised"

**Current text:** "No fifth new public type or interface is authorised. These four are the entire new public surface this implementation may create."

**Why this one is *not* stale:** an OCR mechanism dependency contract would be authorised and owned by a *future OCR mechanism's own Contract Design* — a document this proposal does not draft and does not name a Kotlin shape for (mirroring exactly how `ReasoningProvider` is owned by the Reasoning Provider Contract Design, not by Evidence Intelligence) — Evidence Intelligence itself gains a dependency *edge*, not a new *owned* public type. This paragraph, which counts only Evidence-Intelligence-*owned* types, remains fully accurate and requires **no change**.

---

## 3. Exact Paragraphs Remaining Unchanged

Confirmed, by direct re-reading this turn, already sufficient without amendment:

- **Contract Design §1, "Producing candidate derivative artefacts"** — already names "an OCR transcription" by name as an example of a `CandidateEvidenceArtifact` Evidence Intelligence is responsible for producing. No amendment needed; this is the paragraph the new §12 dependency row exists to make *executable*, not a paragraph requiring its own change.
- **Contract Design §5, the `EvidenceAnalysisResult` variant table** — "Candidate artefact produced" already names "(an OCR transcription, an extraction, a translation)" by name. No new output category is required.
- **Contract Design §5, the verbatim/normalised/inferred-reconstruction transcription-fidelity paragraph** — already, explicitly, requires any transcription-produced `CandidateEvidenceArtifact` (OCR or otherwise) to disclose which of the three fidelity kinds applies. This already substantially answers OCR output-quality classification (§8, below); no amendment needed.
- **Contract Design §6, acceptance orchestration** — already fully generic; `EvidenceIntelligenceAcceptanceCoordinator` (Unit 7, already implemented) dispatches any `CandidateArtifactProduced` value to `EvidenceCustodian.accept` without regard to what produced it. No amendment needed.
- **Contract Design §9, Confidence Model** — already, explicitly, names "an extraction confidence" as ordinary, transient analytical machinery Evidence Intelligence may compute. No amendment needed for OCR to legitimately carry a working-confidence figure, provided it is never written into a durable field, exactly as already required for every other analytical figure.
- **Contract Design §12, the CDR-005 Model C self-certification paragraph** — already provides a generic path for identifying whether a *new* permission-relevant domain act (for example, "invoke OCR" as distinct from "invoke `EvidenceIntelligence`" itself, already frozen at Scope Lock §6 step 0) requires its own disclosed `PermissionAction`/`ResourceType` pairing. No amendment needed to enable that path — only its eventual use, a Scope Lock decision, not resolved here (§9, below).
- **Evidence Intelligence Scope Lock §3, Explicit Exclusions, the "OCR, transcription, translation, or any other analysis kind's own internal algorithm" row** — the Scope Lock's own copy of the same exclusion the Contract Design's "Out of Scope" section and the Implementation Plan §11 each separately state. **Confirmed explicitly unchanged.** It excludes Evidence Intelligence from *implementing* an OCR algorithm from scratch — the same relationship Evidence Intelligence already has with `ReasoningProvider` (orchestrated, never implemented) — and does not exclude depending on an externally-governed OCR mechanism, which is precisely what this proposal's new dependency row leaves open, mirroring `ReasoningProvider` exactly. Because this row is one of the two documents this proposal targets (§9, below), it is checked here directly, not only by way of its Implementation Plan restatement.
- **Implementation Plan §11, "Explicitly Out of Scope," the "OCR... own internal algorithm" exclusion** — confirmed, on inspection, not in tension with this proposal, for the identical reason given immediately above for the Scope Lock's own copy.
- **CDR-007 in full** — unaffected; already, and correctly, classifies OCR as Evidence Intelligence's own analytical function (Ownership and Sequencing Review §5, §7, §10). This proposal implements that classification; it does not revisit it.

---

## 4. Ownership Analysis

- **Evidence Intelligence gains ownership of nothing new.** It becomes a *consumer* of a new dependency edge, exactly as it already is for `ReasoningProvider` — it does not own, define, or implement the OCR mechanism itself.
- **A future OCR mechanism Contract Design would own the OCR mechanism's own contract** — no Kotlin name, interface, or type is assigned to it anywhere in this proposal — mirroring exactly how the Reasoning Provider Contract Design owns `ReasoningProvider`. This proposal does not draft that document, and does not pre-name it; identifying it as the eventual owner is sufficient for this amendment's own scope.
- **Evidence Custodian retains exclusive ownership of any accepted OCR-transcription artefact after acceptance**, unchanged — §5's ownership-transfer rule (Section 3, above) already governs this without modification.
- **No ownership change to `EvidenceArtifactId`, `CandidateEvidenceArtifact`, `Provenance`, or any Memory Core/Knowledge Memory type.** None is touched by this proposal.

---

## 5. Public Contract Analysis

**Does authorising an OCR-mechanism dependency require any new Evidence-Intelligence-owned public type? No.** Evidence Intelligence's own public model remains exactly the four types the Scope Lock's §4 already froze (`EvidenceAnalysisRequest`, `EvidenceAnalysisResult`, the "Candidate record produced" payload selector, `EvidenceIntelligence`). The new dependency is a *reference to an externally-owned contract*, the same shape `ReasoningProvider` already has — not a type Evidence Intelligence defines. Section 2.5, above, confirms the Scope Lock's own "no fifth new public type" language needs no edit.

---

## 6. Dependency Analysis

| Subsystem | Structural change? | Descriptive change? |
|---|---|---|
| Evidence Custodian | No | No — unrelated to this amendment |
| Memory Core | No | No — unrelated to this amendment |
| Reasoning Provider | No | No — unrelated to this amendment |
| OCR mechanism (new) | **Yes — new dependency row, §12** | Yes — the row's own "Already exists in" column states plainly that no governed contract exists yet, distinguishing this row from the three existing dependencies it sits beside (§2.2, above) |
| Conversation Engine | No | No |
| Permission Engine | No — Evidence Intelligence still holds no Permission Engine dependency of its own | No — an OCR-specific invocation gate, if warranted, is composed externally exactly as the existing invocation gate already is (Scope Lock §6 step 0) |
| Knowledge Memory | No | No — unrelated to this amendment |

**Conclusion: exactly one new dependency row is added; nothing else in the dependency table changes.** The amended table itself must distinguish three, not two, categories, and no row may blur them: (1) the three **existing governed dependencies** (`EvidenceCustodian.retrieve`, `MemoryRetrieval`, `ReasoningProvider`), each already existing in an already-accepted Contract Design; (2) the **newly authorised dependency** (the OCR mechanism), which this amendment authorises *in principle*, as a dependency *edge*, without yet governing what it connects to; and (3) the **future Contract Design still required** before that edge connects to anything concrete — not yet drafted, not begun, not named, and not authorised to begin by this proposal.

---

## 7. Who Consumes `RequiresOcr`

Settling Ownership and Sequencing Review §11's open question: **no Contract Design change is required to answer this.** `EvidenceAnalysisRequest` (§4) already accepts an arbitrary list of `EvidenceArtifactId` references and an open, non-enumerated `analysisKind` string — nothing about consuming Evidence Processing's own `RequiresOcr` disclosure requires a new input field. The lawful consumer is a **new, composition-level coordinator** — not yet built, no Kotlin name assigned here — mirroring exactly the already-established `EvidenceRegistrationCoordinator`/`EvidenceIntelligenceAcceptanceCoordinator` shape (Implementation Plan §5, "cited as structural precedent only, not a dependency of any kind"): it would observe `EvidenceExtractionCoordinator.extract` returning `RequiresOcr`, and in response construct an ordinary, already-legal `EvidenceAnalysisRequest` naming that same `EvidenceArtifactId` with `analysisKind = "ocr-transcription"` (illustrative, not prescribed), then invoke the existing, unmodified `EvidenceIntelligence.analyse` operation. This coordinator is **not part of Evidence Intelligence**, exactly as the acceptance coordinator is not (Scope Lock §6) — its own shape, invocation trigger, and permission gating are Scope Lock/Implementation Plan decisions, not resolved by this proposal, and not required for this proposal's own narrow objective (§1, above).

**Machine-triggered invocation is identified here as an open constitutional question, not resolved.** Every invocation of `EvidenceIntelligence.analyse` the existing governance already contemplates — including the one the already-frozen step-0 invocation gate (Scope Lock §6, §9, §11) is written to gate — is described as occurring "at a requester's own initiative" (Scope Lock §11's own self-certification language). A `RequiresOcr`-triggered coordinator, by contrast, would invoke `EvidenceIntelligence.analyse` in direct response to a prior pipeline outcome, with no human request necessarily in the loop at that moment — the first invocation path in this repository's governance shaped this way. This is squarely implicated by Constitutional Test 1 ("Does it preserve owner control? The owner must remain able to see, limit, and stop what this capability does") and by the Constitution's own "no intelligence within Parker is trusted with unchecked authority" principle, extended here to a purely mechanical trigger rather than a reasoning one. What principal, authorisation basis, or owner-visible control point attaches to such an invocation is **not decided by this proposal, and not decided by anything reviewed** — it is named here as a question a future Scope Lock decision must resolve, explicitly, before any such coordinator is implemented, exactly as the existing invocation gate itself required its own explicit Scope Lock decision (§7, §11 there) rather than being left to Implementation Plan discretion.

---

## 8. OCR Output-Quality Validation Ownership

Settling Ownership and Sequencing Review §9's open question: **substantially already answered by frozen text, not by any new paragraph.** Three already-frozen provisions, read together, place OCR output-quality classification inside Evidence Intelligence's own existing responsibility, with no amendment needed:

1. **§1's "Producing candidate derivative artefacts"** already makes Evidence Intelligence responsible for producing an OCR transcription as a `CandidateEvidenceArtifact` — the same act as judging it worth producing at all, exactly as ordinary analytical judgment already governs whether any analysis step yields a `CandidateArtifactProduced` or only a `TransientOutput` (§5).
2. **§5's verbatim/normalised/inferred-reconstruction fidelity paragraph** already, explicitly, requires any OCR-produced artefact to disclose which fidelity kind applies — the closest existing analogue to "quality validation" this Contract Design defines, and it already covers OCR by name.
3. **§9's Confidence Model** already permits a transient "extraction confidence" figure as ordinary analytical machinery, never durable — sufficient for any working-quality signal an OCR mechanism's own output might carry.

**The one genuinely open sliver:** whether *rejecting* OCR output as too poor to submit even as a `CandidateEvidenceArtifact` warrants its own disclosed `PermissionAction`/`ResourceType` pairing (distinct from ordinary analytical judgment) is a question the existing CDR-005 Model C self-certification path (§12, restated at Section 3 above) already has a mechanism to answer — this proposal does not perform that self-certification, and does not assert one is or is not required.

---

## 9. Scope Lock Assessment

**A narrow Scope Lock amendment is required — the mirrored dependency table (§2.3, above) — but no other Scope Lock section changes.** §5 (Ownership Freeze) and §6 (Sequencing Freeze) already state their rules generically enough to cover an OCR-produced `CandidateEvidenceArtifact` without modification (Section 3, above); §7 (Stop Conditions) and §8 (Implementation Invariants), not quoted in this proposal, were checked and found to reference no dependency-count-specific language that the new row would contradict.

---

## 10. Implementation Plan Assessment

**Exact passage requiring later change** (not edited here): `docs/implementation/EVIDENCE_INTELLIGENCE_IMPLEMENTATION_PLAN.md` §5, "Do not invent additional dependencies. This list is exhaustive." This sentence's own list would need the same fourth entry as Contract Design §12 (§2.2, above), once an actual Implementation Plan amendment cycle begins — not performed by this proposal.

**Not requiring change:** §11's "Explicitly Out of Scope" OCR exclusion (Section 3, above, confirmed not in tension). Units 1–8's own individual descriptions were not individually re-audited by this proposal — that level of detail belongs to the future Implementation Plan amendment itself, mirroring exactly how Amendment 1's own precedent deferred Unit 3's wording update to "a separate, later act" (Amendment 1 Proposal §11, step 5).

---

## 11. CDR and Constitutional Assessment

- **No operative CDR-007 decision changes.** CDR-007's classification of OCR as Evidence Intelligence's own analytical function is exactly what this proposal implements, not revisits.
- **No constitutional-tier document changes.** The Parker Constitution is unaffected.
- **This remains a Contract Design evolution only**, of the same tier and character as Amendment 1 — a dependency-list correction, not a new CDR, not a reopening of CDR-006 or CDR-007, and not an Evidence Processing document of any kind.
- **Why a Contract Design amendment suffices, and a new CDR does not:** the two prior CDRs in this lineage (CDR-006, CDR-007) each performed a *subsystem-tier* constitutional classification — deciding that Evidence Custodian, and then Evidence Intelligence, exist at all as first-class, peer subsystems, organisationally distinct from every other subsystem (CDR-007 §1: "a first-class, downstream, analytical subsystem... a peer of Evidence Custodian and Knowledge Memory"). An OCR mechanism is not proposed here as a peer subsystem of that kind — it is proposed as a *dependency*, structurally identical in constitutional weight to `ReasoningProvider`, which itself required no CDR of its own to be authorised as an Evidence Intelligence dependency (Contract Design §12). The Constitution's own governing distinction is exactly this one: "Parker owns authority. Modules provide capability" — reasoning providers, tool integrations, and "future extensions" are named together, in the Constitution's own "Parker owns authority. Modules provide capability" section (Core Principles), as capability-tier additions that supply what Parker can do without altering what Parker is allowed to do or which subsystems exist. An OCR mechanism, like a Reasoning Provider, supplies capability to an already-constitutionally-classified subsystem; it does not itself become a new subsystem, does not acquire custody, promotion, or truth authority (§2.2, above), and does not change Evidence Intelligence's own constitutional boundary, peer relationships, or exclusive-responsibility table (CDR-007 §3, §5) in any respect. CDR-007 has, moreover, already performed the one constitutional act this dependency actually depends on: it classified OCR itself — not merely "an OCR mechanism dependency," but the analytical function of performing OCR — as "Evidence Intelligence's own analytical function" (CDR-007 §1, Decision Rules), by name, without qualification, and without leaving that classification to a future document. What remains after CDR-007 is therefore not a constitutional question but a Contract-Design-tier mechanical one: naming, in the dependency table CDR-007's own successor document maintains, the dependency edge through which an already-classified analytical function may be exercised — the identical role `ReasoningProvider`'s own row already plays for reasoning. Escalating that naming step to a new CDR would treat a capability-tier addition as though it were a subsystem-tier one, which neither the Constitution's own module/authority distinction nor CDR-007's own classification requires.

---

## 12. Recommended Amendment Sequence

Mirroring the now-precedented sequence Amendment 1 itself followed (recorded in the Contract Design's own "Amendment 1" status section):

1. Draft an in-place "Amendment 2" section in `EVIDENCE_INTELLIGENCE_CONTRACT_DESIGN.md`, applying exactly the five paragraph-level changes identified in §2 (three substantive, two confirmed-unnecessary).
2. A matching, narrow Scope Lock edit (§2.3, §9 above) — the mirrored dependency table only.
3. Independent Constitutional Review of that draft, specifically re-examining Section 7 (who consumes `RequiresOcr`) and Section 8 (validation ownership) for internal consistency with this proposal's own reasoning.
4. Final Confirmation Review.
5. Commit and freeze.
6. Only then, as separate, later acts: (a) the Implementation Plan's §5 dependency list (§10, above) is updated to match; (b) a future OCR Mechanism Contract Design is drafted, owning the new dependency's concrete shape; (c) the composition-level `RequiresOcr` consumer (§7, above) is designed, as its own Scope Lock/Implementation Plan decision.

---

## 13. Confirmation No Canonical File Changed

No governance document was modified in the preparation of this proposal. `docs/architecture/EVIDENCE_INTELLIGENCE_CONTRACT_DESIGN.md`, `EVIDENCE_INTELLIGENCE_SCOPE_LOCK.md`, `docs/implementation/EVIDENCE_INTELLIGENCE_IMPLEMENTATION_PLAN.md`, both Evidence Processing OCR review documents, and every other file read remain exactly as they were at `eac43cb`. No Kotlin was implemented. No production code or test was touched.

## 14. No Git Actions

No git command beyond read-only inspection (`status`, `log`) was run. Nothing was staged, committed, or pushed.
