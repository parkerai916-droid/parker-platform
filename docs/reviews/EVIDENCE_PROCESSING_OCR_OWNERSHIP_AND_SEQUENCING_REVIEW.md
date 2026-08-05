# Evidence Processing / Evidence Intelligence — OCR Ownership and Sequencing Reconciliation Review

## Status

**Governance analysis only.** No Kotlin is implemented, proposed as a
diff, or changed. No existing governance document is modified. Neither
`src/` nor `tests/` is touched. Nothing is staged, committed, or pushed.
Repository baseline: `HEAD eac43cb`, branch `main`, working tree
containing only the untracked `docs/reviews/EVIDENCE_PROCESSING_UNIT_5_OCR_PLANNING_REVIEW.md`
("the OCR Planning Review"), confirmed before this review began and
unchanged throughout it.

Purpose: resolve, by careful reading of already-frozen governance —
never by inventing new governance — the two open items the OCR Planning
Review identified but explicitly declined to resolve: the Unit 5
numbering collision, and constitutional ownership of OCR. This document
does not reopen, narrow, or re-argue CDR-006, CDR-007, the Evidence
Artifact Contract Design, the Evidence Custodian governance stack, the
Evidence Processing (Searchable PDF) Boundary Clarification/Scope
Lock/Implementation Plan, or the Evidence Intelligence Contract
Design/Scope Lock/Implementation Plan. Where it finds that existing text
already answers a question, it cites that text; it does not restate it as
a new decision.

---

## 1. Repository Baseline

HEAD `eac43cbbb9d4e02731608769107fc44b89f367af` (matches expected
`eac43cb`), branch `main`. Working tree confirmed to contain exactly one
untracked file, `docs/reviews/EVIDENCE_PROCESSING_UNIT_5_OCR_PLANNING_REVIEW.md`
— no discrepancy from the expected state.

## 2. Documents Reviewed

**Read in full, this pass:** `docs/decisions/CDR-007_CONSTITUTIONAL_CLASSIFICATION_OF_EVIDENCE_INTELLIGENCE.md`
("CDR-007") — in full, including sections not previously quoted in this
conversation's own earlier reviews (Section 4 "Existing Parker Subsystems
Reused," Section 6 "Permitted Analytical Artefacts," the Decision Rules,
and the Non-Decisions). `docs/reviews/EVIDENCE_INTELLIGENCE_CONTRACT_DESIGN_AMENDMENT_1_PROPOSAL.md`
(consulted for its own established structure as "a planning input to a
future amendment, not the amendment itself" — the precedent this review's
own recommended vehicle, Section 14 below, follows).

**Read in full in this same conversation's immediately preceding turns,
and relied upon here without re-reading, since `git log` confirms none
has changed since:** `docs/reviews/EVIDENCE_PROCESSING_UNIT_5_OCR_PLANNING_REVIEW.md`;
`docs/architecture/EVIDENCE_PROCESSING_SEARCHABLE_PDF_{BOUNDARY_CLARIFICATION,SCOPE_LOCK,IMPLEMENTATION_PLAN}.md`
(the last including its own Acceptance Tracking addition);
`docs/architecture/EVIDENCE_INTELLIGENCE_CONTRACT_DESIGN.md`;
`docs/architecture/EVIDENCE_INTELLIGENCE_SCOPE_LOCK.md`;
`docs/implementation/EVIDENCE_INTELLIGENCE_IMPLEMENTATION_PLAN.md` (including its
own Unit 8 Acceptance Tracking addition); `src/interfaces/{EvidenceExtractor,DerivativeReview}.kt`;
`src/runtime/EvidenceExtractionCoordinator.kt`; CDR-006.

**Repository-wide search performed this pass** for: OCR, optical
character recognition, RequiresOcr, scanned PDF, searchable PDF,
image-only PDF, document processing, analytical function, derivative
creation, Unit 5, Production Composition. Findings folded into Sections 3
and 5 below; no additional governing document beyond those already listed
was found to reference OCR by name.

---

## 3. Existing Unit Sequence

**Evidence Processing (Searchable PDF) Implementation Plan, Section 4**
fixes, and does not rename here: Unit 1 (Extraction Contracts, complete),
Unit 2 (Apache Tika Adapter, complete), Unit 3 (Human Review Registry,
complete), Units 4A/4B (Coordinator, complete), **Unit 5 — Production
Composition and Runtime Entry Points** (not begun), Unit 6 — Verification
(not begun), Unit 7 — Real-World Operational Proof (not begun). Confirmed
by direct inspection: `src/composition/ParkerRuntime.kt` contains no
reference to `EvidenceExtractionCoordinator`, `TikaEvidenceExtractor`, or
`DerivativeReviewRegistry` anywhere.

**Evidence Intelligence Implementation Plan, Section 7/8** fixes Units
1–8, all complete, verified, committed, and pushed (Units 1–7 are the
analytical operation itself and its acceptance coordination; Unit 8,
"Runtime Composition," wired `ParkerRuntime.analyseEvidence` and is this
same conversation's own most recent prior implementation work, confirmed
by that document's own Acceptance Tracking section).

## 4. Unit-Numbering Finding

**The collision is real, and is resolved here not by renumbering either
document, but by recognising that OCR was never Evidence Processing's own
"Unit 5" to name in the first place** (Section 6, below, establishes why).
Evidence Processing (Searchable PDF)'s own "Unit 5" **remains, unmodified,
"Production Composition and Runtime Entry Points"** — the `ParkerRuntime`
wiring step for the already-complete Tika/coordinator pipeline. It is not
renamed, reassigned, or reused for OCR by this document. OCR-related
governance work, once a vehicle is chosen (Section 14), takes its own
identity within Evidence Intelligence's own Implementation Plan sequence
(next available number: Unit 9, that Plan's Section 7/8 currently ending
at Unit 8), never inside Evidence Processing's own numbering at all.

## 5. Existing OCR Clauses

Every clause any reviewed document contains that names OCR directly:

- **Boundary Clarification, Determination 2 & Section 2:** "Searchable
  PDF only... A PDF whose text layer cannot be read directly is out of
  scope for this capability entirely. It is classified `RequiresOcr` and
  reported, never attempted, never silently degraded." Section 2's
  exclusion list: "OCR — interpreting an image into text. A PDF requiring
  it is reported (`RequiresOcr`) and stops there," explicitly grouped with
  transcription and translation as requiring "interpreting an image, a
  sound, or a language into a claim about its content" — the same
  reasoning Determination 1 uses to explain *why* deterministic text-layer
  reading, alone, qualifies for this document's own narrow exception.
- **Boundary Clarification, Section 2, closing line:** "Every one of the
  excluded items above, should it ever be built, remains governed by
  CDR-007 and the (currently paused) Evidence Intelligence Contract Design
  — this document creates no alternate path around either." (Evidence
  Intelligence is, in fact, no longer paused — Section 12, below.)
- **CDR-007, Section 1:** "document ingestion, OCR, transcription, and
  extraction are Evidence Intelligence's own analytical functions."
- **CDR-007, Section 6, category 2 ("Durable evidence artefacts"):** "A
  derivative (**an OCR transcription**, an extraction, a translation) that
  Evidence Intelligence causes to be accepted into Evidence Custodian
  custody via the existing acceptance path, and registered in Memory Core
  with `extractedFrom`/`derivedFrom` set, becomes a first-class,
  separately-identified custodied artefact... *Representative examples,
  once accepted:* **an OCR transcription**; extracted text; a
  translation."
- **CDR-007, Section 4 ("Existing Parker Subsystems Reused"):**
  "`EvidenceCustodian.accept` (or a coordinator mirroring
  `EvidenceRegistrationCoordinator`'s already-frozen shape) — the sole
  path by which any Evidence-Intelligence-produced derivative (**an OCR
  transcript**, a translation) enters custody as its own, separately
  identified artefact."
- **CDR-007, Decision Rules:** "The capabilities previously discussed
  under 'Document Intelligence' are classified as Evidence Intelligence's
  own analytical functions, including document ingestion, OCR,
  transcription, and extraction."

No reviewed document contains any clause classifying OCR as Evidence
*Processing*'s own function. Every clause that names OCR by name assigns
it to Evidence Intelligence.

## 6. Detection Ownership

**Evidence Processing (Searchable PDF), already built, unmodified by
this review.** `TikaEvidenceExtractor`'s twenty-character text-density
threshold is the sole existing signal; `EvidenceExtractionCoordinator`
wraps it unchanged as the terminal `EvidenceExtractionOutcome.RequiresOcr`
(`src/runtime/EvidenceExtractionCoordinator.kt`, step 5). This is
correctly Evidence Processing's own responsibility because it is
mechanical classification (Determination 1: "no interpretation, no
pattern recognition, and no judgment of any kind"), the identical
reasoning that placed deterministic text-layer reading inside Evidence
Processing's own boundary in the first place. Nothing in Section 5's
clauses disturbs this — CDR-007 never claims detection; it claims
OCR itself.

## 7. OCR Execution Ownership

**Evidence Intelligence**, per CDR-007 Section 1 and the Decision Rules,
quoted in full in Section 5 above — OCR is explicitly named as
interpretive ("interpreting an image into text," Boundary Clarification
Section 2), the exact criterion Determination 1 uses to distinguish what
Evidence Processing may do (mechanical parsing) from what it may not
(interpretation). Determination 1's own narrow exception was never
extended to OCR — the Boundary Clarification says so directly (Section 2,
quoted in Section 5). Running the OCR tool itself is therefore not a
question left open by any document reviewed; it is already decided,
against Evidence Processing and for Evidence Intelligence.

## 8. Derivative Custody Ownership

**Shared, already-frozen infrastructure — owned exclusively by neither
programme.** `EvidenceCustodian` (accept/retrieve) and
`EvidenceRegistrationCoordinator` (unchanged) remain the sole path any
derivative, from either programme, enters custody through — CDR-007
Section 4 names this explicitly for Evidence Intelligence's own future
OCR derivative ("a coordinator mirroring `EvidenceRegistrationCoordinator`'s
already-frozen shape"), and Evidence Processing's own `EvidenceExtractionCoordinator`
already calls the identical, unmodified coordinator today. Neither
programme owns custody or registration; both are callers of Evidence
Custodian and Memory Core, exactly as CDR-007 Section 2 ("Not an evidence
store... Not a memory system") and the Boundary Clarification's own
"`EvidenceCustodian`, `MemoryCore`... unchanged" (Section 8) already
establish independently for each programme.

## 9. Validation Ownership

Not decided by anything reviewed, and this review does not decide it.
Structural validation (output exists, page count/hash recorded) mirrors
Evidence Processing's own Unit 4A discipline regardless of which
programme performs it. Whether *quality* validation (is the recognised
text plausible, sufficient, or garbage) is itself an interpretive act
that would, by Determination 1's own reasoning, belong wherever OCR
execution belongs (Evidence Intelligence, Section 7) is a genuine open
question this review surfaces but does not resolve — carried forward
unchanged from the OCR Planning Review's own Section 3.7/3.9.

## 10. Evidence Intelligence Ownership

Confirmed, not merely inferred: CDR-007 Section 6 places "an OCR
transcription" inside category 2 ("Durable evidence artefacts") of
Evidence Intelligence's own permitted analytical artefacts — structurally
identical in shape to `EvidenceAnalysisResult.CandidateArtifactProduced`,
the existing, unmodified output category `EvidenceIntelligenceAcceptanceCoordinator`
(Evidence Intelligence Unit 7, already implemented) already dispatches to
`EvidenceCustodian.accept` today (`src/runtime/EvidenceIntelligenceAcceptanceCoordinator.kt`).
**No new `EvidenceAnalysisResult` category is implied by this finding** —
an OCR transcription fits the existing, frozen four-category taxonomy
exactly as CDR-007 itself already anticipated. What Evidence
Intelligence's own governance does *not* yet name is OCR as a concrete
*internal mechanism* `EvidenceIntelligence.analyse` (or a new coordinator
alongside it) may invoke — the existing Contract Design's own dependency
list (`EvidenceCustodian.retrieve`, `MemoryRetrieval`, `ReasoningProvider`
zero-or-more) is stated as exhaustive ("Do not invent additional
dependencies... This list is exhaustive," Implementation Plan Section 5)
and names no OCR-shaped dependency. `ReasoningProvider` itself is not a
structural fit for an OCR engine (`ReasoningProvider.reason` is
LLM/reasoning-shaped, not an image-to-text operation) — this is the one
genuine gap Evidence Intelligence's own governance has, addressed in
Section 14.

## 11. Meaning of `RequiresOcr`

Answering each sub-question directly:

- **Merely a terminal classification result, or intended to trigger a
  later stage?** Both, and the Boundary Clarification is explicit about
  which: "A terminal, explicit, non-silent outcome — never attempted
  further by *this capability*" (Section 3, emphasis added). It is
  terminal *for Evidence Processing*. It is simultaneously, by the same
  document's own closing line (Section 2, quoted in Section 5), a
  disclosure that a further, differently-owned capability may act on it.
- **Which component may consume it?** Not decided by anything frozen
  today — no consumer exists yet. Given Section 7's finding, the lawful
  consumer is a future Evidence-Intelligence-owned component, not a
  further Evidence Processing unit.
- **Does consuming it require runtime composition or new orchestration?**
  Yes, necessarily — `RequiresOcr` is returned from `EvidenceExtractionCoordinator.extract`,
  a suspend function call; nothing today calls it in production (Evidence
  Processing's own Unit 5 is unbegun), so no live signal exists yet to
  consume. A future consumer requires both Evidence Processing's own
  eventual Unit 5 (to reach `extract` at all in production) and its own
  new composition.
- **Does it authorise OCR execution, or only disclose that extraction
  cannot proceed without OCR?** Only the latter. `RequiresOcr` is a
  disclosure, not an authorisation — no Permission Engine evaluation
  occurs anywhere in its production (`EvidenceExtractionCoordinator`
  holds no `PermissionEngine` reference, Scope Lock Section 5). Whatever
  future component invokes OCR must independently evaluate its own
  governed proposal, exactly as Evidence Intelligence's own existing
  invocation gate (`EvidenceIntelligenceInvocationGate`, Evidence
  Intelligence Unit 6) already does for ordinary analysis — exact
  precedent for what an "OCR invocation gate" would need to be, not yet
  built.

## 12. Cross-Programme Boundary

Preserved, confirmed by direct inspection, none altered by this review:
Evidence Custodian retains exclusive custody/immutability/deletion
authority (CDR-006, unmodified; `EvidenceCustodian` interface declares no
mutation operation). Evidence Processing retains ownership of mechanical
detection only (Section 6). Evidence Intelligence retains ownership of
analysis and candidate production, now including OCR execution and
registration-causation specifically (Section 7, Section 10). No parser —
Tika today, any future OCR engine — ever holds authority over truth;
CDR-007 Section 2 ("Not a truth authority") and Article XV bind any
future OCR mechanism identically to how they already bind
`EvidenceIntelligence.analyse`. No alteration of original evidence is
possible under either programme's existing contracts (`EvidenceCustodian`
declares no write-after-accept path at all). One correction to the record
this review must disclose, not resolve: the Boundary Clarification's own
closing line (Section 5, above) describes Evidence Intelligence as
"currently paused" — it is not; Evidence Intelligence Units 1–8 are
complete, verified, committed, and pushed, confirmed by that
Implementation Plan's own Acceptance Tracking section and by
`src/composition/ParkerRuntime.kt`'s live `analyseEvidence` entry point.
This is a factual staleness in a frozen document's own prose, not a
constitutional conflict — the Boundary Clarification's substantive
determinations are unaffected by whether Evidence Intelligence happens to
be paused or resumed at the moment of reading.

## 13. Sequencing Decision

Evidence Processing's own Units 5–7 and OCR governance are **independent
tracks with no ordering dependency between them** — Section 4's own
finding (OCR was never Evidence Processing's own unit) means OCR
governance does not wait on Evidence Processing's own runtime composition
to proceed. OCR *governance* (Section 14's recommended vehicle) may begin
immediately, in parallel with Evidence Processing Units 5–7. OCR
*implementation* (contracts, a new coordinator, its own tests) may
likewise proceed largely in parallel, mirroring exactly how Evidence
Processing's own Units 1–4B were built and fully tested via hand-written
fakes before any runtime composition existed. The one genuine ordering
constraint: OCR's own eventual runtime composition needs a live,
composed `EvidenceExtractionCoordinator` to consume `RequiresOcr` from in
production — meaning **OCR runtime composition should follow, or at
minimum land alongside, Evidence Processing's own Unit 5**, not before
it. Docling, the structured document model, and reporting all remain
strictly later, each depending on OCR's own output shape being settled
first (OCR Planning Review, Section 3.10) — this review does not disturb
that ordering.

## 14. Governance Vehicle Required

**Narrow Evidence Intelligence clarification** — not a Contract Design
rewrite, not a new CDR, not a reopening of CDR-007 (which already,
correctly, classifies OCR — Section 5), and not an Evidence Processing
document of any kind (which owns none of this). The smallest lawful
vehicle mirrors two already-established precedents exactly: (a) the
Searchable-PDF Boundary Clarification's own method — narrowly resolving
one specific technique's classification without reopening CDR-006/CDR-007
— applied here to authorise OCR *execution* as a concrete Evidence
Intelligence mechanism; and (b) `EVIDENCE_INTELLIGENCE_CONTRACT_DESIGN_AMENDMENT_1_PROPOSAL.md`'s
own established structure and status framing ("a planning input to a
future amendment, not the amendment itself") for how such a narrow
addition should be drafted and reviewed before being adopted. The one
genuine gap such a clarification must close (Section 10): naming a
concrete "OCR mechanism" as an authorised Evidence Intelligence
dependency, since the existing Contract Design's own dependency list is
stated as exhaustive and currently names none. This clarification would
also be the natural place to settle Section 9's own open validation-
ownership question and Section 11's "who consumes `RequiresOcr`" finding,
rather than leaving them for separate documents.

## 15. Risks

1. **If this reconciliation is not adopted before implementation begins,**
   a future implementer could plausibly build an OCR coordinator under
   Evidence Processing's own numbering (exactly the collision the OCR
   Planning Review first flagged), producing code that must later be
   re-attributed or re-homed under Evidence Intelligence's own governance
   track.
2. **The Boundary Clarification's "currently paused" language (Section
   12) is now factually stale.** Left uncorrected, a future reader could
   be misled about Evidence Intelligence's own present status, though the
   underlying determinations are unaffected.
3. **No concrete OCR-engine dependency exists in any governance document
   yet.** Without Section 14's clarification, any implementation attempt
   would exceed Evidence Intelligence's own exhaustive dependency list
   (Implementation Plan Section 5) the moment it introduces one.
4. **Validation ownership (Section 9) remains genuinely open.** Proceeding
   to implementation without settling it risks quality-validation logic
   being scattered between a mechanical, Evidence-Processing-shaped
   coordinator and an interpretive, Evidence-Intelligence-shaped one, with
   no principled line between them yet drawn.

## 16. Final Finding

The unit-numbering collision is resolved by recognising its premise was
mistaken, not by renumbering either document: Evidence Processing's own
Unit 5 remains Production Composition, unrenamed; OCR was never Evidence
Processing's own work to number, because CDR-007 already, explicitly,
by name, classifies OCR execution and OCR-derivative custody-causation as
Evidence Intelligence's own analytical functions (Section 5, Section 7,
Section 10) — a decision this review discovers already made, not one it
makes. What remains genuinely open is narrow and mechanical: Evidence
Intelligence's own governance has not yet named a concrete OCR mechanism
as an authorised dependency, and has not yet settled who consumes
Evidence Processing's own `RequiresOcr` disclosure or owns quality
validation. None of this requires reopening CDR-006, CDR-007, or either
programme's own frozen Boundary Clarification/Scope Lock/Contract Design.

## 17. Recommended Next Step

Produce a document in the same style and status as
`EVIDENCE_INTELLIGENCE_CONTRACT_DESIGN_AMENDMENT_1_PROPOSAL.md` — a
planning input identifying exactly which paragraphs of the Evidence
Intelligence Contract Design (its dependency list, Section 12/13) and
Scope Lock would need a narrow amendment to authorise a concrete OCR
mechanism as an Evidence Intelligence dependency, and to settle Section 9
(validation ownership) and Section 11 (the `RequiresOcr` consumer). That
proposal, once independently reviewed and accepted, becomes the
authorising vehicle; only after its acceptance should any OCR coordinator
shape, dependency signature, or file list be designed — mirroring exactly
the discipline Evidence Processing's own Boundary Clarification → Scope
Lock → Implementation Plan sequence already modelled for this programme's
own first capability.

---

## Decision

**Narrow Evidence Intelligence clarification required.**

---

## Confirmation No Other File Changed

No governance document, production source file, or test file was
modified. The OCR Planning Review (`docs/reviews/EVIDENCE_PROCESSING_UNIT_5_OCR_PLANNING_REVIEW.md`)
was read only, never edited. This review document is the only file
created by this task.

## Confirmation No Git Actions

Nothing staged, committed, or pushed. Only read-only `git`
commands were run, to confirm the baseline and confirm no governing
document had changed since it was last reviewed in full.
