# Evidence Processing (Searchable PDF) — "Unit 5" (OCR Decision Boundary) — Planning Review

## Status

**Governance analysis only.** No Kotlin is implemented, proposed as a
diff, or changed. No existing governance document is modified. Neither
`src/` nor `tests/` is touched. Nothing is staged, committed, or pushed.
Repository baseline: `HEAD eac43cb`, branch `main`, working tree clean,
confirmed before this review began and unchanged throughout it.

Produced following a read-only planning review, per this task's own
instruction. Does not reopen
`docs/architecture/EVIDENCE_PROCESSING_SEARCHABLE_PDF_BOUNDARY_CLARIFICATION.md`
("the Boundary Clarification"),
`docs/architecture/EVIDENCE_PROCESSING_SEARCHABLE_PDF_SCOPE_LOCK.md`
("the Scope Lock"),
`docs/architecture/EVIDENCE_PROCESSING_SEARCHABLE_PDF_IMPLEMENTATION_PLAN.md`
("the Implementation Plan"), CDR-006, or CDR-007 — all remain frozen,
unmodified, and unreopened by this document.

Purpose: determine the constitutional boundary for introducing OCR into
the Evidence Processing pipeline, given Units 1–4B (extraction contracts,
the Apache Tika adapter, the human review registry, and the coordinator)
are independently verified, committed, and pushed. This document
identifies risks and open questions; **it resolves none of them.**

---

## 1. Exact Files Reviewed

**Governance, read in full:**
`docs/architecture/EVIDENCE_PROCESSING_SEARCHABLE_PDF_BOUNDARY_CLARIFICATION.md`;
`docs/architecture/EVIDENCE_PROCESSING_SEARCHABLE_PDF_SCOPE_LOCK.md`;
`docs/architecture/EVIDENCE_PROCESSING_SEARCHABLE_PDF_IMPLEMENTATION_PLAN.md`
(including its own recently-added Acceptance Tracking section). Also
consulted, from this same governance-first workflow's own prior review in
this conversation: `docs/decisions/CDR-006_CONSTITUTIONAL_CLASSIFICATION_OF_ORIGINAL_EVIDENCE_CUSTODY_AND_IMMUTABILITY.md`
("CDR-006"), `docs/decisions/CDR-007_CONSTITUTIONAL_CLASSIFICATION_OF_EVIDENCE_INTELLIGENCE.md`
("CDR-007"), and `docs/reviews/EPISTEMIC_INTEGRITY_CONSTITUTIONAL_REMEDIATION_PLAN.md`
§12 — cited below where directly relevant; not re-read line-by-line in
this pass since their governing text has not changed since last reviewed
in full and `git log` confirms no commit has touched them.

**Implementation, read in full (the current endpoint):**
`src/interfaces/EvidenceExtractor.kt` (`EvidenceExtractor`,
`ExtractionOutcome`, `ExtractionResult`, `ExtractionIdentity`,
`EmbeddedResourceObservation`); `src/interfaces/DerivativeReview.kt`
(`DerivativeReviewState`, `DerivativeReviewRecord`,
`DerivativeReviewRegistry`); `src/runtime/EvidenceExtractionCoordinator.kt`
(`EvidenceExtractionCoordinator`, `EvidenceExtractionOutcome`,
`IntegrityVerificationOutcome`). `src/runtime/TikaEvidenceExtractor.kt`
and `src/runtime/InMemoryDerivativeReviewRegistry.kt` were verified via
`git show --stat eac43cb` to be committed byte-for-byte as implemented
(identical line counts, no post-implementation edit) and were reviewed in
full in the immediately preceding implementation pass of this same
conversation.

---

## 2. Current Endpoint — What Actually Exists Today

`EvidenceExtractionCoordinator.extract` runs a nine-step sequence ending,
on a scanned/image-only PDF, at step 5: `EvidenceExtractor.extract`
returns `ExtractionOutcome.RequiresOcr(detectedMediaType, pageCount)`,
which the coordinator wraps unchanged in
`EvidenceExtractionOutcome.RequiresOcr` and **returns immediately.** No
candidate is constructed, no registration is attempted, nothing is
retried (Boundary Clarification Section 2, Determination 2: "never
attempted further by this capability"). `TikaEvidenceExtractor` classifies
this outcome using a fixed twenty-character text-density threshold; no
OCR module is on the classpath (`tika-parser-ocr-module` is not, and per
the Scope Lock's own Section 4 must not become, a dependency), so OCR is
today **structurally unreachable**, not merely unconfigured.

This is the exact terminal state "Unit 5" must begin from: a closed,
frozen, four-variant `ExtractionOutcome` taxonomy in which `RequiresOcr`
is already the correctly-classified, correctly-terminal outcome, and a
coordinator that is itself frozen (Scope Lock Section 9: "No unit in
this programme may reopen the Boundary Clarification... Any change to
this Scope Lock or its companion Implementation Plan after acceptance
follows... a new, dated revision, never a silent edit").

---

## 3. Planning Objectives — Findings, Not Decisions

### 3.1 Exactly when OCR is invoked

The only structurally coherent point of invocation is **after**
`EvidenceExtractionCoordinator.extract` returns
`EvidenceExtractionOutcome.RequiresOcr`, as a **separate, subsequent
step** — never inline inside the existing coordinator, which the Scope
Lock's own change-control section (Section 9) forbids reopening. Any OCR
mechanism is therefore necessarily a **new caller**, composed around the
existing coordinator's own output, not a modification to it. Whether that
new caller re-invokes the existing coordinator a second time (feeding an
OCR-produced searchable PDF back through the same `extract` operation),
or calls a wholly separate pipeline, is not determined by anything
frozen today (Section 5, below).

### 3.2 Which component decides OCR is required

Already decided, and already shipped: `TikaEvidenceExtractor`'s own
twenty-character threshold heuristic (`SEARCHABLE_TEXT_THRESHOLD`) is the
**only** existing signal. A new OCR-invoking component does not, and per
Determination 1/2 must not, re-decide this question independently — it
only reacts to the classification `EvidenceExtractor` already produced.
Whether a new component may perform *additional* validation before
invoking OCR (for example, re-confirming page-level image-only status)
is not decided by anything frozen today.

### 3.3 Whether OCR modifies the original evidence

**Must not, and nothing in the existing architecture provides a code path
by which it could.** `EvidenceCustodian` declares exactly `accept` and
`retrieve` — no update, overwrite, or mutation operation exists on the
interface at all (`src/interfaces/EvidenceCustodian.kt`). CDR-006's
Constitutional Optimisation Safeguard already names this exact scenario
as forbidden by illustration: "deleting an original after optical
character recognition." Any OCR mechanism must read the original only
through `EvidenceCustodian.retrieve`, exactly as `EvidenceExtractionCoordinator`
already does — this is not a new constraint OCR introduces; it is the
existing constraint restated for a new caller.

### 3.4 Whether OCR produces replacement, derivative, temporary, or multiple derivatives

**Not decided by anything frozen today — the single largest open
architectural question this review identifies.** OCR tooling of the kind
this task's own prior planning material named (OCRmyPDF) characteristically
produces a *new searchable PDF* (the original's images with an invisible
text layer added), not raw text directly. This creates a genuine fork:

- **Option A — two chained derivatives.** OCR's own searchable-PDF output
  is itself registered as a new, separately identified derivative
  (original → OCR'd-PDF → extracted-text, a three-node provenance chain),
  with the OCR'd PDF then fed back through the existing, unmodified
  `EvidenceExtractionCoordinator`/`TikaEvidenceExtractor` pipeline a
  second time to obtain the final extracted text.
- **Option B — one derivative, transient intermediate.** OCR's own
  searchable-PDF output is treated as a purely transient, non-custodied
  intermediate artefact (existing only long enough to run a second,
  in-process Tika pass), and only the final extracted text is ever
  registered (original → extracted-text, the same two-node shape Units
  1–4B already produce).

Both are constitutionally defensible; neither is fixed by any existing
document. This choice has direct consequences for Section 3.10 (Docling
compatibility), below.

### 3.5 Provenance requirements

The existing mechanism (`Provenance.extractedFrom`/`derivedFrom`/`processingHistory`,
reused unchanged, no new Memory Core field) extends structurally to
however many derivative hops Section 3.4 settles on. **One substantive,
previously-undiscussed tension surfaces here:** Boundary Clarification
Determination 1 draws a hard, deliberate line — deterministic text-layer
reading "involves no interpretation, no pattern recognition, and no
judgment of any kind," distinguishing it explicitly from OCR, named in
the very same document's own exclusion list as "interpreting an image
into text" (Section 2). `Provenance.contentNature = EXTRACTED` was
reserved, by that same reasoning, for the non-interpretive case. Whether
OCR output should therefore carry `ContentNature.EXTRACTED` (reusing
Tika's own category) or `ContentNature.INFERRED` (reflecting genuine
interpretation) — or whether the existing five-value taxonomy has no
correct answer at all for OCR — is not decided anywhere. Separately:
`Provenance.confidence` is left `null` throughout Units 1–4B specifically
because deterministic extraction has no honest confidence value to
report; OCR engines characteristically *do* produce genuine per-page or
per-word confidence figures, meaning OCR may be the first case in this
programme where a genuine, non-fabricated confidence value could
legitimately be recorded — a design opportunity, not a settled decision.

### 3.6 Evidence identity requirements

Consistent with CDR-006's separate-identity rule, restated, not altered:
every OCR-produced artefact, however many Section 3.4 settles on, is a
new, separately identified `EvidenceArtifactId`/`DocumentId` pair, never
sharing identity with the original or with any other derivative in the
chain.

### 3.7 Failure behaviour

OCR introduces failure modes the existing, closed four-variant
`ExtractionOutcome` taxonomy has no vocabulary for: OCR engine crash or
timeout, and genuine "OCR ran, but produced insufficient or low-quality
output" — a materially different case from `Malformed` (corrupt bytes)
or `Unsupported` (wrong format), since the input bytes are neither
corrupt nor the wrong format; they are validly-formed image content the
OCR step itself could not adequately interpret. Silently reusing
`Malformed`/`Unsupported` for this case would misattribute the failure's
own cause. Partial success — some pages OCR successfully, others do not,
within one document — is a distinct further case with no existing
representation. None of this is decided; it is identified as a gap in
the existing closed taxonomy that any OCR mechanism will need its own
answer for.

### 3.8 Security boundaries

**Not addressed by anything frozen today, and materially different in
kind from Unit 2's own boundary.** Scope Lock Section 4 fixes "No Tika
Server — in-process JVM integration only," a discipline Unit 2 satisfies
because Apache Tika is a pure-JVM library. OCR tooling of the kind named
in this task's own prior planning material (OCRmyPDF, wrapping Tesseract
and Ghostscript) is native, non-JVM, externally-invoked software —
introducing a materially different risk surface (process/subprocess
invocation, command-injection surface, decompression-bomb and
resource-exhaustion exposure, temporary-file handling, and whether
network access must be structurally prevented rather than merely
unconfigured) that no existing Evidence Processing governance has ever
had to address, because Determination 2 excluded OCR specifically to
avoid addressing it. Whether OCR runs in-process, as a supervised
subprocess, or in a fully isolated container is not decided anywhere.

### 3.9 Performance expectations

OCR is characteristically orders of magnitude slower than deterministic
text-layer reading (seconds to minutes per page, against Unit 2's own
sub-second path). Whether this remains compatible with this programme's
own established "explicit invocation only, no background processing
unless required and governed" discipline, or whether OCR specifically
requires asynchronous or background invocation, is not decided by
anything frozen today and is not merely an implementation detail — it
bears on whether a new runtime-lifecycle concept is needed at all.

### 3.10 Future compatibility with Docling

Docling (structured document conversion — hierarchy, headings, tables,
page coordinates) is named in this workstream's own prior planning
material as the capability immediately following OCR in the eventual
pipeline. Docling's own structural analysis characteristically requires
the visual/spatial PDF itself, not flattened extracted text. This means
**Section 3.4's own choice is not cost-free**: if OCR is designed to
produce only extracted text (Option B), a future Docling adapter would
have no PDF-shaped artefact to operate on for a scanned document, and
Section 3.4 would need to be revisited under a different name later. If
OCR is designed to register its own searchable-PDF output as a genuine
derivative (Option A), that door remains open at the cost of a deeper
provenance chain and an additional governed artefact today. This review
surfaces the dependency; it does not choose between the two.

---

## 4. Constitutional Risks

1. **CDR-007 may already assign OCR elsewhere, unresolved by this
   programme's own governance.** CDR-007's own Context section classifies
   "document ingestion, OCR, transcription, and extraction" as "Evidence
   Intelligence's own analytical functions." The Boundary Clarification
   resolved the collision between that classification and CDR-006's own
   anticipation of derivative-generating processes **narrowly, for
   deterministic text-layer extraction only** (Determination 1) — and
   explicitly, repeatedly declined to decide anything about OCR itself
   (Section 2: "OCR... explicitly excluded, and not decided by this
   document in any respect"). No narrow determination analogous to
   Determination 1 exists for OCR. Treating OCR as a further "Evidence
   Processing" unit, rather than as Evidence Intelligence's own
   analytical function, may directly conflict with CDR-007's own binding
   text unless a dedicated boundary determination is produced for OCR
   specifically, mirroring how Determination 1 was produced for
   deterministic extraction.
2. **`ContentNature` classification tension** (Section 3.5) — reusing
   `EXTRACTED` for genuinely interpretive OCR output would blur the exact
   distinction Determination 1 was written to draw.
3. **No governance has yet authorised OCR at all.** This planning review
   does not, and cannot, authorise implementation. A full
   Boundary-Clarification → Scope Lock → Implementation Plan cycle,
   mirroring Units 1–4B's own governance history in full, remains
   required before any OCR code is written.

## 5. Architectural Risks

1. Whether OCR produces one or two new derivatives (Section 3.4) is
   undecided and changes the shape of the provenance chain, the review
   registry's own scope, and the eventual Docling handoff.
2. The existing four-variant `ExtractionOutcome` taxonomy has no
   representation for OCR-specific failure or partial-success modes
   (Section 3.7); a new, closed taxonomy is required and does not yet
   exist even in draft.
3. The security/process-isolation boundary for native OCR tooling is
   entirely unaddressed by existing governance (Section 3.8), which has
   only ever governed a pure-JVM integration.

## 6. Sequencing Risks

1. **Direct unit-numbering collision.** The already-frozen Implementation
   Plan's own Section 4 names its fifth unit "Unit 5 — Production
   Composition and Runtime Entry Points" — the `ParkerRuntime` wiring
   step, not yet begun (confirmed: no reference to `EvidenceExtractionCoordinator`,
   `TikaEvidenceExtractor`, or `DerivativeReviewRegistry` exists anywhere
   in `src/composition/ParkerRuntime.kt` today). This task's own title
   assigns the name "Unit 5" to OCR instead. These are two different
   pieces of work sharing one identifier in two different documents. This
   review does not resolve the collision or propose a replacement number;
   it records that the collision exists.
2. **A prior, related sequencing gate was already not honoured.** The
   Implementation Plan's own "Programme Completion Criteria" states that
   Evidence Intelligence's own (paused) Contract Design becomes "eligible
   to resume" only once Evidence Processing (Searchable PDF) is complete
   in full, including Units 5–7. Evidence Intelligence has, in fact,
   already been fully implemented and composed into `ParkerRuntime`
   (confirmed by direct inspection in this same conversation's own prior
   review), before Evidence Processing's own Units 5–7 were ever begun.
   Adding a new unit to this programme's own sequence now does not
   retroactively reconcile that gap, and this review does not attempt to.
3. Units 5 (Production Composition), 6 (Verification), and 7 (Real-World
   Operational Proof) of the *existing* Implementation Plan remain
   entirely unbegun. Whether OCR planning should precede or follow their
   completion is not decided by anything frozen today.

## 7. Dependency Risks

1. OCR tooling of the kind previously discussed (OCRmyPDF, wrapping
   Tesseract and Ghostscript) would be this repository's first
   non-JVM/native dependency — a materially different category from
   Apache Tika (Unit 2's own "first third-party dependency," but still
   pure-JVM), with licensing implications (Ghostscript's licensing varies
   by distribution) this repository has no existing tracking convention
   for.
2. If OCR output is designed to re-enter the existing Tika/coordinator
   pipeline (Section 3.4, Option A), a dependency relationship from a new
   OCR mechanism onto `TikaEvidenceExtractor`/`EvidenceExtractionCoordinator`
   is created that does not exist today, and those two classes' own
   frozen shape (Scope Lock Section 9: no unit may reopen them) was never
   designed with a second, internally-originating caller in mind.
3. `EvidenceExtractionCoordinator` is frozen with exactly five
   dependencies and no route for a caller to inject OCR behaviour into
   it; any OCR mechanism is necessarily new, external composition, never
   an addition to that class's own constructor — a constraint worth
   stating explicitly given how easy it would be to reach for "just add
   a sixth dependency."

## 8. Unresolved Questions

1. Does OCR produce one derivative (text only) or two (a registered
   searchable-PDF derivative, then extracted text)?
2. Is an OCR-produced searchable PDF durably registered and custodied, or
   a purely transient intermediate?
3. What `ContentNature` value applies to OCR output?
4. Should `Provenance.confidence` be genuinely populated for OCR, unlike
   the `null`-always discipline Units 1–4B established for deterministic
   extraction?
5. Does OCR require process isolation (subprocess, container) or could a
   JVM-native OCR mechanism satisfy the existing "in-process only"
   discipline?
6. Does OCR's own performance profile require asynchronous or background
   invocation, and if so, under what governance?
7. Is "Unit 5" the correct label for this work at all, given the direct
   collision identified in Section 6.1 — and separately, does CDR-007
   require a dedicated boundary determination for OCR (mirroring
   Determination 1) before any further planning proceeds?
8. How is partial-page OCR (a document mixing scanned and digital pages)
   represented — one derivative per document, or per-page granularity?
9. If two chained derivatives exist, does each receive its own
   independent `DerivativeReviewRegistry` lifecycle, or only the final
   text derivative?

None of the above is resolved by this document.

---

## 9. Confirmation No Other File Changed

No governance document, production source file, or test file was
modified. This review document is the only file created by this task.

## 10. Confirmation No Git Actions

Nothing staged, committed, or pushed. Only read-only `git log`/`git
status`/`git show` commands were run, to confirm the baseline and confirm
no governing document had changed since it was last reviewed in full.
