# Evidence Processing (Searchable PDF) — Boundary Clarification

## Status

**Accepted. Canonical. Frozen.** Independent Constitutional Verification
and Final Freeze Verification have both been completed. This Boundary
Clarification is adopted as normative Parker governance for the
searchable-PDF evidence-processing boundary. Produced following a
read-only Planning and Boundary Review, accepted in principle subject to
three refinements (human review as a real governed state; reproducible
extraction identity; validated source identity pairing), all
incorporated below.
Does not reopen `docs/decisions/CDR-006_CONSTITUTIONAL_CLASSIFICATION_OF_ORIGINAL_EVIDENCE_CUSTODY_AND_IMMUTABILITY.md`
("CDR-006"), `docs/decisions/CDR-007_CONSTITUTIONAL_CLASSIFICATION_OF_EVIDENCE_INTELLIGENCE.md`
("CDR-007"), `docs/architecture/EVIDENCE_ARTIFACT_CONTRACT_DESIGN.md`
("the Contract Design"), `docs/architecture/EVIDENCE_CUSTODIAN_SCOPE_LOCK.md`,
`docs/architecture/EVIDENCE_CUSTODIAN_IMPLEMENTATION_PLAN.md`,
`docs/architecture/MEMORY_CORE_CONTRACT_DESIGN.md`, or
`docs/architecture/EVIDENCE_INTELLIGENCE_CONTRACT_DESIGN.md` (currently
paused) — all remain frozen, unmodified, and unreopened by this
document. No Kotlin is implemented, proposed as a diff, or changed. No
Scope Lock and no Implementation Plan is produced. Nothing is staged,
committed, or pushed.

Programme: **Evidence Processing — Searchable PDF, Boundary
Clarification.**

---

## Context

CDR-006 and the Contract Design already anticipate a derivative-generating
capability distinct from custody itself: the Contract Design's own §4
("Supporting derivative artefact generation") names "OCR, transcription,
extraction, thumbnailing, summarisation" as processes a custodied
original may be made available to, without the Evidence Custodian ever
performing the transformation itself. CDR-007, separately, classifies
"document ingestion, OCR, transcription, and extraction" as "Evidence
Intelligence's own analytical functions." Read together without further
distinction, these two statements collide for exactly the capability
this document governs: extracting the text layer already embedded in a
searchable PDF.

This document resolves that collision narrowly, for one specific
technique only, and fixes the three refinements required before any
Kotlin is written. It does not decide anything about OCR, transcription,
translation, summarisation, comparison, inference, or knowledge
promotion — each remains exactly where CDR-006, CDR-007, and the paused
Evidence Intelligence Contract Design already place it.

---

## 1. Determinations

1. **Deterministic extraction of an already-encoded PDF text layer is
   not an exercise of Evidence Intelligence's analytical functions.**
   CDR-007's "extraction" sits in a list with OCR, transcription, and
   translation — every other member of that list requires interpreting
   an image, a sound, or a language into a claim about its content.
   Reading text objects a searchable PDF already stores natively
   involves no interpretation, no pattern recognition, and no judgment
   of any kind; it is mechanical file-format parsing, structurally
   closer to "Memory Core records where a Document can be found without
   fetching it" than to analysis. This determination is narrow and does
   not extend to OCR, transcription, translation, or any technique that
   requires interpreting a representation of content rather than reading
   content already encoded as text (Section 2).
2. **Searchable PDF only.** A PDF whose text layer cannot be read
   directly is out of scope for this capability entirely. It is
   classified `RequiresOcr` and reported, never attempted, never
   silently degraded to a shorter or emptier result (Section 3).
3. **Human review is a governed lifecycle state, not an inference from
   registration.** A derivative existing in Memory Core, or being
   retrievable through Evidence Custodian, proves nothing about whether
   a human has looked at it. This document creates a narrow, separate
   contract for that fact (Section 6), and no future consumer may treat
   extracted text as human-verified without querying it.
4. **Source identity pairing must be verified before provenance is
   created, not assumed from caller input.** A caller-supplied
   `EvidenceArtifactId` and `DocumentId` are two independent inputs; the
   coordinator confirms they actually name the same original before any
   `extractedFrom`/`derivedFrom` fact is recorded (Section 5).
5. **Extraction metadata records reproducibility conditions, not an
   unconditional determinism claim.** "Deterministic" throughout this
   document means: deterministic under a fixed input, a fixed parser
   version, a fixed dependency set, a fixed configuration profile, and a
   fixed normalisation profile — never a claim that re-running extraction
   after a library upgrade must reproduce byte-identical output (Section
   4).

---

## 2. Classification Boundary

**In scope, classified by this document:**

- deterministic retrieval of an already-encoded PDF text layer;
- media detection (which format a candidate artefact actually is);
- integrity verification (a computed digest checked against what was
  recorded at custody, yielding `Verified`, `Mismatch`, or `Unverifiable`
  — Section 7 — with `Mismatch` and `Unverifiable` both failing closed,
  and a produced derivative's own digest recorded regardless of which
  outcome resulted);
- derivative creation (a new, separately identified custodied artefact);
- provenance registration (linking that derivative back to its original
  through Memory Core's existing, unmodified mechanism);
- extraction metadata (the reproducibility facts Section 4 fixes);
- human review state (Section 6).

**Explicitly excluded, and not decided by this document in any respect:**

- OCR — interpreting an image into text. A PDF requiring it is reported
  (`RequiresOcr`) and stops there.
- image interpretation of any other kind;
- transcription — interpreting audio into text;
- translation — interpreting one language as another;
- summarisation — producing a shorter representation of meaning;
- comparison — evaluating two pieces of content against each other;
- inference — proposing a conclusion from evidence;
- knowledge promotion — anything touching Knowledge Memory's own
  promotion boundary;
- Evidence Intelligence reasoning of any kind. This document does not
  reopen, narrow, or reinterpret CDR-007, and does not authorise the
  paused Evidence Intelligence Contract Design to resume.

Every one of the excluded items above, should it ever be built, remains
governed by CDR-007 and the (currently paused) Evidence Intelligence
Contract Design — this document creates no alternate path around either.

---

## 3. Extraction Port and Failure Classification

One new, narrow, Parker-owned interface, `EvidenceExtractor` (one
operation: given content bytes, return an extraction outcome), living in
`src/interfaces/`. No Apache Tika type, or any other third-party type,
appears anywhere in this interface's own shape — every field is a JDK
type, a Parker-owned type, or a primitive. `EvidenceExtractor` holds no
dependency of its own — no `PermissionEngine`, no `EvidenceCustodian`, no
`MemoryCore` — mirroring `ReasoningProvider`'s own "pure callee, calls
nothing" shape: it receives bytes already obtained through an already-
gated read, and returns an outcome.

The outcome is a sealed type with exactly four variants:

- **Extracted** — carries the extraction result (Section 4's fields).
- **RequiresOcr** — carries the detected media type and, where known,
  the page count. A terminal, explicit, non-silent outcome — never
  attempted further by this capability.
- **Unsupported** — the candidate is not a searchable PDF at all (a
  different, undetected, or encrypted format). Carries the detected
  media type and a reason.
- **Malformed** — a genuine parse failure (corrupt or truncated bytes).
  Carries a reason.

No fifth variant, and no default/empty result, may ever stand in for a
genuine failure to classify a document — mirroring the same discipline
already established for `ReasoningProviderResponse.NoAction` and the
paused Evidence Intelligence Contract Design's own empty-result rule.

The one production implementation, `TikaEvidenceExtractor`
(`src/runtime/`), is the **only** file in this repository permitted to
import `org.apache.tika.*`. It converts Apache Tika's own metadata and
exception types into the Parker-owned shapes above internally; nothing
Tika-typed ever crosses `EvidenceExtractor`'s own boundary. A structural
reflection test enforces this directly (Section 12).

**Searchable-versus-scanned detection.** After extraction, if the
extracted text, trimmed, falls below a small configured threshold while
the document reports at least one page, the outcome is `RequiresOcr`,
never a false, emptily "successful" `Extracted`. Apache Tika does not
invoke OCR of its own accord unless a Tesseract installation is present
and an OCR strategy is explicitly configured; neither is configured by
this capability (Determination 2; Section 2).

**Embedded resources.** `TikaEvidenceExtractor` configures Tika's own
embedded-document handling to record the presence, declared file name,
and declared media type of any embedded file or attachment, without
parsing its content and without merging it into the extracted text.
These observations are carried as their own, distinctly labelled field
(Section 4) — never folded into the general `warnings` list, and never
silently accepted as though they were part of the primary document.

---

## 4. Extraction Metadata — Reproducibility Contract

Every field below is retained. None requires a new Memory Core field —
each maps onto an existing, unmodified `Provenance`/`Document` field, per
the table that follows.

| Field | Meaning | Recorded in |
| --- | --- | --- |
| Original evidence digest | SHA-256 of the bytes actually retrieved for the original, computed before extraction. Digest calculation is a fact about the bytes in hand and always succeeds; integrity verification is the separate act of checking that fact against what custody recorded (Section 7), and may itself be `Verified`, `Mismatch`, or `Unverifiable` | `CandidateProvenance.processingHistory` entry, alongside the `IntegrityVerificationOutcome` (Section 7, Section 8) that comparing it against the original `Document.integrityHash` produced |
| Extracted derivative digest | SHA-256 of the produced derivative's own content | `CandidateDocument.integrityHash` on the derivative's own registration — the field already exists exactly for this |
| Detected media type | What `EvidenceExtractor` determined the original to be (for example, `application/pdf`) | `CandidateProvenance.processingHistory` entry; never the derivative's own `documentType`, which describes what the derivative *is* (Section 7), not what the original was |
| Extractor name and version | The overall extraction framework and its version (for example, "Apache Tika", "3.3.2") | `CandidateProvenance.creator` — an exact fit for "free-text description of who or what produced the underlying content" |
| Actual parser identity | The specific parser implementation that handled this document (Apache Tika delegates to one of several concrete parsers; recording which one is a distinct, finer-grained fact than the framework name/version above, and matters for genuine reproducibility) | `CandidateProvenance.processingHistory` entry — one field of the structured `ExtractionIdentity` record described below |
| Parser/configuration profile | A named tag identifying the specific parser configuration in force (embedded-resource handling, OCR strategy explicitly disabled, and any other configuration this capability sets) — a different configuration could produce different output from the same extractor version | `CandidateProvenance.processingHistory` entry — one field of the structured `ExtractionIdentity` record described below |
| Normalisation profile or version | Whether, and which, post-extraction text normalisation was applied. This first unit applies none — the record states that explicitly (`normalisationProfile=none`), never omits the field, so a future unit that begins normalising text changes a recorded fact rather than silently altering behaviour no one can see happened | `CandidateProvenance.processingHistory` entry — one field of the structured `ExtractionIdentity` record described below |
| Extraction timestamp | When this extraction was performed | `CandidateProvenance.processingHistory` entry, and the source of `CandidateProvenance.acquisitionTime` for the derivative |
| Warnings | Any non-fatal condition `EvidenceExtractor` observed | `CandidateProvenance.processingHistory` entry (or an explicit "warnings=none") |
| OCR-used flag | Always `false` for this capability (Determination 2; Section 2) | `CandidateProvenance.processingHistory` entry |
| Embedded-resource observations | Presence, declared name, and declared media type of any embedded file or attachment (Section 3) | `CandidateProvenance.processingHistory` entry, distinctly labelled from warnings |

**Extraction identity is one structured record, not three independently-worded
sentences.** Actual parser identity, parser/configuration profile, and
normalisation profile together answer a single question — exactly what
produced this derivative's text — and must be read back as a unit to
judge reproducibility; recording each as its own free-standing
`processingHistory` sentence would let them drift apart, or be read in
isolation, or be reworded inconsistently between extractions. `ExtractionIdentity`
(Section 8) is a small, flat, structured type — `parserIdentity`,
`configurationProfile`, `normalisationProfile`, each a fixed field, none
free narrative text — and is what the coordinator actually writes into
the single `CandidateProvenance.processingHistory` entry these three
table rows share, serialised in a fixed, defined form (for example,
`parserIdentity=...; configurationProfile=...; normalisationProfile=...`),
never composed as prose a future reader must interpret. `processingHistory`
itself remains the same free-text-typed field Memory Core already
defines — no new Memory Core field is added — but the content of this
particular entry is a defined structure, not free text relying on
narrative description.

`CandidateProvenance.contentNature` is `EXTRACTED` (already exists,
exactly for this). `CandidateProvenance.extractedFrom` is the original's
own `DocumentId`, set only after the verification in Section 5 succeeds.
`CandidateProvenance.confidence` is left `null`: this extraction is
deterministic under the conditions above, not probabilistic, and
inventing a numeric confidence figure for it would be fabricated
precision this repository's own governing principle ("an unknown value
is never fabricated, defaulted, or inferred") already forbids.
`CandidateDocument.metadata` carries only the document metadata Tika
itself reports about the original (page count, declared producer,
declared title, and similar) — never a duplicate of any fact already
recorded in `Provenance`, consistent with Memory Core's own "no scattered
provenance fields" discipline.

No new field is added to `Provenance`, `Document`, `CandidateProvenance`,
or `CandidateDocument`. Every fact above is expressed through a field
that already exists, used exactly as its own contract already documents
it.

---

## 5. Source Identity Verification

The coordinator (Section 7) accepts both an `EvidenceArtifactId` (naming
the original in Evidence Custodian) and a `DocumentId` (naming the
original's own Memory Core registration) as separate caller inputs — they
are not derivable from one another, and nothing compels a caller to
supply a genuinely matching pair.

**Verification, before any retrieval of content and before any
provenance is constructed:**

1. Look up the `Document` by the supplied `DocumentId`, via
   `MemoryRetrieval.getDocument` (already exists, read-only, unmodified).
   If nothing is found, the outcome is `SourceDocumentNotFound` — the
   coordinator never calls `EvidenceCustodian.retrieve` on this path.
2. Compare the found `Document.locationReference` against the supplied
   `EvidenceArtifactId`'s own value. `EvidenceRegistrationCoordinator`'s
   own existing registration path already sets
   `locationReference = evidenceArtifactId.value` for every Document it
   registers (`src/runtime/EvidenceRegistrationCoordinator.kt`) — this
   comparison is a direct, already-supported check, not a new field or a
   new inference.
3. If they do not match, the outcome is `SourceIdentityMismatch`. No
   retrieval, no extraction, no candidate construction, and no
   registration occurs. This is a hard failure, never a warning, and
   never silently proceeds using either supplied identifier alone.
4. Only once the pair is confirmed does the coordinator call
   `EvidenceCustodian.retrieve` for the actual bytes.

This ordering means an invalid pairing is rejected before any content
is read at all, minimising both unnecessary access and the chance of
extracting from, or attaching provenance to, the wrong original.

---

## 6. Human Review Lifecycle

**A new, narrow, standalone contract — not a Memory Core record kind,
not an Evidence Custodian operation, not a general workflow engine.**
Mirroring `EvidenceDeletionAudit`'s own precedent (Evidence Custodian
Phase 7 Boundary Clarification, Section 2): one small interface, one
record type, one enumeration, addressing existing identifiers by value
only, holding no structural dependency on `EvidenceCustodian` or
`MemoryCore`, and held by neither in return.

- **`DerivativeReviewState`** — exactly four values: `PENDING_REVIEW`,
  `APPROVED`, `REJECTED`, `NEEDS_CORRECTION`. No fifth value, and no
  caller-defined open classification — this is a closed, exhaustive
  enumeration, deliberately unlike Memory Core's own open
  `entityType`/`documentType` strings, because a downstream gate
  (Section 6, "Gating rule," below) must be able to test for exactly
  `APPROVED` without needing to know every string a future caller might
  invent.
- **`DerivativeReviewRecord`** — one flat type: the derivative's own
  `EvidenceArtifactId`, its own `DocumentId`, the `DerivativeReviewState`
  being recorded, a timestamp, an optional reviewing `PrincipalId` (`null`
  for the initial, system-recorded `PENDING_REVIEW`), and an optional
  free-text note (a reviewer's reason for `REJECTED` or
  `NEEDS_CORRECTION`).
- **`DerivativeReviewRegistry`** — two operations: recording a new review
  state, and reading the current review state for a given
  `EvidenceArtifactId`. Recording is **append-only** — every call adds a
  new record; nothing already recorded is ever overwritten or removed,
  mirroring this repository's own "history is never silently rewritten"
  discipline (Memory Core Section 12; the deletion audit's own two-stage
  append). The current state for an identifier is whichever record was
  most recently appended for it.

**Initial state.** The coordinator (Section 7) records `PENDING_REVIEW`
for every newly registered derivative, immediately, as part of the same
successful pipeline run — never left unset, and never inferred from the
absence of a record (an identifier with no review record at all is a
distinct, and itself reportable, condition from one genuinely at
`PENDING_REVIEW`).

**Valid transitions**, enforced by whatever implements
`DerivativeReviewRegistry`, never by the enumeration itself (mirroring
`MemoryCoreRecordStatus`'s own identical discipline): `PENDING_REVIEW` →
`APPROVED`, `REJECTED`, or `NEEDS_CORRECTION`. `APPROVED`, `REJECTED`,
and `NEEDS_CORRECTION` are all terminal for that identifier — mirroring
`DELETED`'s own terminality in Memory Core, and for the same underlying
reason: Memory Core's own records are immutable once created, so
"correcting" a derivative never means reopening or editing its existing
review record in place, and a derivative recorded `NEEDS_CORRECTION` is
never itself transitioned back to `PENDING_REVIEW`. Instead, "correcting"
means a new extraction run — the same Section 7 sequence, invoked again
against the same original — produces a new, separately identified
derivative (its own `EvidenceArtifactId`, `DocumentId`, and provenance,
Section 4), which the coordinator registers exactly as it registers any
first extraction, recording `PENDING_REVIEW` for that new identifier
independently (Section 7, step 8). The superseded derivative's own
`NEEDS_CORRECTION` record remains exactly as recorded — history is never
rewritten to make room for a retry. An attempted transition outside this
graph — including any attempt to move an existing identifier from
`NEEDS_CORRECTION`, `APPROVED`, or `REJECTED` back to `PENDING_REVIEW` —
is rejected, not silently accepted.

**Gating rule.** No future consumer — Evidence Intelligence once
resumed, or any other downstream capability — may treat a derivative's
extracted text as human-verified unless `DerivativeReviewRegistry.currentReviewState`
for that derivative's own `EvidenceArtifactId` returns `APPROVED`.
Absent that check, or for any state other than `APPROVED`, the text must
be treated as unverified machine-extracted output only. This document
fixes the rule now, before any consumer exists to apply it, so it is not
invented ad hoc when Evidence Intelligence resumes; it implements no
enforcement mechanism of its own, since no consumer exists yet to
enforce it against.

**Permission Engine expectation, disclosed not decided.** Recording a
transition to `APPROVED` changes what a future subsystem may rely on —
a genuinely consequential act, not ordinary internal computation. Per
CDR-005's own Governed Admission Model, a future Scope Lock must gate at
least the `APPROVED` transition through Permission Engine, following the
same disclosed-but-unregistered convention Evidence Custodian already
uses (a new, disclosed resource/action pairing, self-certified against
Chapter 10's published criteria, not registered by this document). This
document does not wire that gate — Runtime Integration remains deferred
(Section 7) — but records the expectation so it is not overlooked.

**Durability, explicitly scoped down for this first unit.** An in-memory
`DerivativeReviewRegistry` implementation is sufficient here. A lost
`PENDING_REVIEW`/`NEEDS_CORRECTION` record on process restart means, at
worst, a derivative needs re-review — a materially smaller failure mode
than the deletion audit's own irreversible-action correctness
requirement, which is why that capability required a durable, file-backed
implementation from its first unit and this one does not. Durability may
be added later without changing this contract.

**Reviewer-facing interaction is out of scope.** This document fixes the
contract only. Whatever interface a human actually uses to inspect a
`PENDING_REVIEW` derivative and record a decision — a command-line tool,
an administrative endpoint, or something else — is not designed here.

---

## 7. Coordinator Sequencing

One new coordinator, `EvidenceExtractionCoordinator` (`src/runtime/`),
with exactly five dependencies: `EvidenceCustodian`, `MemoryRetrieval`,
`EvidenceExtractor`, `EvidenceRegistrationCoordinator`, and
`DerivativeReviewRegistry`. No `PermissionEngine` reference of its own —
every step that changes or discloses governed state is already gated by
one of these five dependencies' own existing contracts; the steps this
coordinator adds (identity verification, digest computation, integrity
verification, extraction) are pure computation over data a caller is
already authorised to read.

**Sequence:**

1. `MemoryRetrieval.getDocument(documentId)` — not found → `SourceDocumentNotFound`.
2. Compare `document.locationReference` to the supplied `evidenceArtifactId.value` —
   mismatch → `SourceIdentityMismatch` (Section 5).
3. `EvidenceCustodian.retrieve(principal, evidenceArtifactId)` — rejected
   or not found → the corresponding outcome, unchanged from
   `EvidenceCustodian`'s own result.
4. Compute the retrieved bytes' own SHA-256 — digest calculation, a pure
   fact about the bytes in hand, independent of whether anything exists
   to check it against. Then, separately, verify: compare the computed
   digest against `document.integrityHash`, producing one of exactly
   three `IntegrityVerificationOutcome` values (Section 8) — `Verified`
   (matches the recorded `integrityHash`), `Mismatch` (a recorded
   `integrityHash` exists and does not match), or `Unverifiable` (no
   `integrityHash` is recorded to compare against, or the comparison
   otherwise cannot be performed). Only `Verified` allows the sequence to
   continue to step 5. `Mismatch` → `IntegrityMismatch`; `Unverifiable` →
   `IntegrityUnverifiable`. Both fail closed identically: extraction
   never proceeds, no candidate is constructed, no derivative is created.
   An absent `integrityHash` is not evidence of integrity, and this
   capability draws no distinction, in what it does next, between a
   digest proven wrong and a digest that could not be checked at all —
   only the disclosed reason recorded for each differs (Section 4,
   Section 9).
5. `EvidenceExtractor.extract(content)` — branch on `ExtractionOutcome`
   (Section 3): `RequiresOcr`/`Unsupported`/`Malformed` each stop the
   pipeline and are surfaced unchanged; only `Extracted` continues.
6. Build `CandidateEvidenceArtifact` (the extracted text, as bytes),
   `CandidateProvenance` (Section 4), and `CandidateDocument`
   (`documentType = "extracted-text"`; `integrityHash` = the derivative's
   own digest; `metadata` = Tika's own document metadata only).
7. `EvidenceRegistrationCoordinator.register(...)` — unchanged, reused
   exactly as it already exists.
8. On `EvidenceRegistrationOutcome.Registered`, record `PENDING_REVIEW`
   for the new derivative via `DerivativeReviewRegistry.recordReviewState`
   (Section 6).
9. Return `EvidenceExtractionOutcome.Completed`, carrying the
   registration outcome and the extraction result.

No `try`/`catch` anywhere in this sequence beyond what already exists in
the dependencies it calls — a genuine fault propagates unchanged,
mirroring `EvidenceRegistrationCoordinator`'s own identical discipline.

---

## 8. Contract Surfaces

- **`EvidenceCustodian`, `MemoryCore`, `MemoryRetrieval`, `Provenance`,
  `Document`, `EvidenceRegistrationCoordinator` — unchanged.** No new
  field, no new operation, no new record kind.
- **New interface, `EvidenceExtractor`** — one operation, `extract`
  (Section 3).
- **New sealed type, `ExtractionOutcome`** — `Extracted`, `RequiresOcr`,
  `Unsupported`, `Malformed` (Section 3).
- **New type, `ExtractionResult`** — the fields in Section 4's table,
  carried as one flat value.
- **New class, `TikaEvidenceExtractor`** — the sole implementation of
  `EvidenceExtractor`; the only file permitted to import
  `org.apache.tika.*`.
- **New enum, `DerivativeReviewState`** — exactly four values (Section
  6).
- **New type, `DerivativeReviewRecord`** — the fields in Section 6.
- **New interface, `DerivativeReviewRegistry`** — two operations,
  `recordReviewState` and `currentReviewState` (Section 6).
- **New class, `EvidenceExtractionCoordinator`** — the sole
  implementation of the sequence in Section 7. Exactly five
  dependencies; no `PermissionEngine` reference.
- **New sealed type, `IntegrityVerificationOutcome`** — `Verified`,
  `Mismatch` (carries both the computed and recorded digests), and
  `Unverifiable` (carries the reason no comparison could be made — most
  commonly, no `integrityHash` recorded). Produced by step 4 of Section
  7's sequence; a distinct concern from digest calculation itself, which
  always succeeds once the bytes are in hand (Section 4).
- **New type, `ExtractionIdentity`** — one flat, structured value:
  `parserIdentity`, `configurationProfile`, `normalisationProfile`
  (Section 4). Recorded as a single, fixed-form `processingHistory`
  entry, never as independently-worded prose per field.
- **New sealed type, `EvidenceExtractionOutcome`** — `Completed`,
  `SourceDocumentNotFound`, `SourceIdentityMismatch`,
  `SourceRetrievalRejected` (wrapping `EvidenceRetrievalResult.Rejected`
  unchanged), `SourceNotFound` (wrapping `EvidenceRetrievalResult.NotFound`
  unchanged), `IntegrityMismatch`, `IntegrityUnverifiable`, `RequiresOcr`,
  `Unsupported`, `Malformed`.

---

## 9. Failure Semantics

- **Source Document not found:** `SourceDocumentNotFound`. No retrieval,
  no extraction, no registration.
- **Source identity mismatch:** `SourceIdentityMismatch`. No retrieval
  beyond the Document lookup already performed, no extraction, no
  registration, no `extractedFrom`/`derivedFrom` fact created.
- **Retrieval rejected or not found:** the corresponding
  `EvidenceExtractionOutcome` variant, wrapping `EvidenceCustodian`'s own
  result unchanged.
- **Integrity verification yields `Mismatch`** (a recorded
  `integrityHash` exists and does not match the computed digest):
  `IntegrityMismatch`. Extraction never runs; no derivative is created.
- **Integrity verification yields `Unverifiable`** (no `integrityHash`
  recorded to compare against, or the comparison otherwise cannot be
  performed): `IntegrityUnverifiable`. Extraction never runs; no
  derivative is created — the same fail-closed treatment as `Mismatch`,
  because an unprovable digest is not proof of integrity. The computed
  original-evidence digest and the disclosed reason verification could
  not complete are still recorded (Section 4); only the derivative
  itself is never produced.
- **`RequiresOcr`/`Unsupported`/`Malformed`:** the pipeline stops; no
  candidate is constructed; no registration is attempted; nothing is
  silently retried with a different configuration.
- **`EvidenceRegistrationCoordinator.register` returns anything other
  than `Registered`:** the coordinator returns that outcome unchanged,
  wrapped in `EvidenceExtractionOutcome.Completed`'s own sibling variant
  or forwarded directly (Scope Lock decision; this document fixes only
  that the underlying `EvidenceRegistrationOutcome` is never discarded or
  reinterpreted) — no review state is recorded for a derivative that was
  never actually registered.
- **`DerivativeReviewRegistry.recordReviewState` fails after successful
  registration:** the derivative exists, custodied and registered, with
  no recorded review state — a disclosed residual case, mirroring the
  deletion audit's own disclosed residual case (Evidence Custodian Phase
  7 Boundary Clarification, Section 7): the fault propagates, and no
  outcome falsely claims `PENDING_REVIEW` was recorded when it was not.

---

## 10. How This Clarification Prevents Each Named Failure Mode

| Must prevent | Mechanism |
| --- | --- |
| Treating registration/retrieval alone as human review | A separate, closed-enum review state exists and starts at `PENDING_REVIEW`; nothing is `APPROVED` merely by existing |
| A downstream consumer treating unreviewed text as verified | The gating rule (Section 6) is fixed now, binding on any future consumer, before one exists |
| An open-ended, general workflow engine | `DerivativeReviewState` is a closed, four-value enum with a fixed transition graph; `DerivativeReviewRegistry` has exactly two operations and no custom-state, branching, or notification capability |
| Overclaiming determinism | Extraction metadata is defined as reproducible under a fixed input/parser version/dependency set/configuration/normalisation profile, never unconditionally (Determination 5) |
| Silent Tika leakage into Parker's own contracts | `EvidenceExtractor`'s own shape contains no third-party type; enforced by a structural reflection test (Section 12) |
| A mismatched artefact/Document pair producing false provenance | Verified before retrieval and before any provenance is built; a mismatch is a hard, explicit failure (Section 5) |
| OCR, interpretation, or any Evidence Intelligence function being performed under this capability's name | Section 2's exclusion list; `RequiresOcr` is terminal, never attempted further |
| Embedded attachments silently entering the primary extracted text | Recorded as a distinct, labelled observation; never parsed, never merged (Section 3) |
| Correcting a derivative by reopening or editing it in place | `NEEDS_CORRECTION` is terminal for that identifier, exactly like `APPROVED`/`REJECTED`; a correction is always a new extraction producing a new, separately identified derivative starting at its own `PENDING_REVIEW` (Section 6) |
| An absent or unmatched integrity digest being treated as a pass | Digest calculation and integrity verification are distinct steps; `Verified`, `Mismatch`, and `Unverifiable` are the only outcomes, and `Mismatch`/`Unverifiable` both fail closed identically — extraction never proceeds for either (Section 7, Section 9) |

---

## 11. Boundary Between This Capability and Future Runtime Integration

This document authorises the port, the coordinator, and the review
registry to be built and fully tested (via fakes, mirroring
`EvidenceRegistrationCoordinatorTest.kt`'s own established style) without
wiring any of it into `ParkerRuntime`. Runtime Integration — a new
`ParkerRuntime` entry point, Resource/ActionVocabulary registration for
the review-approval gate (Section 6), and configuration for constructing
a real `TikaEvidenceExtractor` — is a separate, later phase, mirroring
Evidence Custodian's own phase ordering (storage → accept → retrieve →
registration → deletion → runtime integration last). It is not designed,
and not authorised to begin, here.

---

## 12. Proposed Implementation File List

**Production:**
- New file: `src/interfaces/EvidenceExtractor.kt` — `EvidenceExtractor`,
  `ExtractionResult` (carrying `ExtractionIdentity`), `ExtractionIdentity`,
  `ExtractionOutcome`.
- New file: `src/interfaces/DerivativeReview.kt` — `DerivativeReviewState`,
  `DerivativeReviewRecord`, `DerivativeReviewRegistry`.
- New file: `src/runtime/TikaEvidenceExtractor.kt`.
- New file: `src/runtime/InMemoryDerivativeReviewRegistry.kt`.
- New file: `src/runtime/EvidenceExtractionCoordinator.kt` —
  `EvidenceExtractionOutcome`, `IntegrityVerificationOutcome`,
  `EvidenceExtractionCoordinator`.
- `build.gradle.kts` — add `org.apache.tika:tika-core:3.3.2` and the
  narrowest available Tika 3.3.2 PDF-parser module (exact artefact
  coordinate to confirm against Maven Central at implementation time,
  favouring a scoped PDF-only module over the full parsers bundle).
- **Not touched:** `EvidenceCustodian.kt`, `EvidenceArtifactStorage.kt`,
  `MemoryCore.kt`, `EvidenceRegistrationCoordinator.kt`,
  `ParkerRuntime.kt`, `ParkerRuntimeConfig.kt`.

**Tests:**
- New file: `tests/contracts/EvidenceExtractorScopeTest.kt` — structural
  shape checks, plus a reflection-based test confirming no
  `org.apache.tika` type is reachable from any public
  `EvidenceExtractor`/`ExtractionResult`/`ExtractionOutcome` signature.
- New file: `tests/contracts/DerivativeReviewScopeTest.kt` — confirms
  `DerivativeReviewState` has exactly four values and
  `DerivativeReviewRegistry` declares exactly two operations.
- New file: `tests/runtime/TikaEvidenceExtractorTest.kt` — the real
  extractor against the test corpus (searchable Uber PDF, searchable
  WelTec/ERA PDF, one malformed/unsupported file, one scanned PDF
  requiring OCR), reading from a gitignored fixtures directory that
  tests skip gracefully if absent, plus one small synthetic PDF checked
  in for fast, deterministic CI.
- New file: `tests/runtime/InMemoryDerivativeReviewRegistryTest.kt` —
  initial state, valid/invalid transitions, append-only history, current
  state resolves to the most recent record.
- New file: `tests/runtime/EvidenceExtractionCoordinatorTest.kt` —
  hand-written fakes for all five dependencies (mirroring
  `EvidenceRegistrationCoordinatorTest.kt`'s own style), covering every
  branch in Section 9.
- **Not touched:** `EvidenceCustodianScopeTest.kt`,
  `EvidenceRegistrationCoordinatorTest.kt`, any Memory Core test.

---

## Final Recommendation

This Boundary Clarification is Accepted, Canonical, and Frozen.
Independent Constitutional Verification is complete. Final Freeze
Verification is complete. It is adopted as normative Parker governance
for the searchable-PDF evidence-processing boundary. It authorises no
implementation on its own. It fixes the contract shape, sequencing, and
file list in Sections 3–9 and 12 as the basis for the first Evidence
Processing implementation unit, without reopening CDR-006, CDR-007, the
Contract Design, Memory Core's own contracts, or the paused Evidence
Intelligence Contract Design, and without authorising Runtime
Integration (Section 11) to begin.

EVIDENCE PROCESSING (SEARCHABLE PDF) BOUNDARY CLARIFICATION — ACCEPTED —
CANONICAL — FROZEN

Confirmed: no Kotlin implemented; no test written; CDR-006, CDR-007, the
Evidence Artifact Contract Design, the Evidence Custodian Scope Lock, the
Evidence Custodian Implementation Plan, Memory Core's own contracts, and
the paused Evidence Intelligence Contract Design all unmodified by this
document; no Scope Lock or Implementation Plan produced; nothing staged;
nothing committed; nothing pushed.
