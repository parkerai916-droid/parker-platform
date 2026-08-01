# CDR-006 — Independent Constitutional Final Freeze Review

## Status

**Final.** This document performs the independent constitutional
verification and Final Freeze Verification that
`docs/decisions/CDR-006_CONSTITUTIONAL_CLASSIFICATION_OF_ORIGINAL_EVIDENCE_CUSTODY_AND_IMMUTABILITY.md`
("CDR-006") itself stated it had not yet undergone. It does not amend,
redefine, or reopen CDR-006, the Evidence Artifact Contract Design, the
Evidence Custodian Scope Lock, the Evidence Custodian Implementation
Plan, the Parker Constitution, or Epistemic Integrity Amendment No. 1
("Article IX, as amended"). It identifies conflict, or its absence,
only, and states a recommendation. No Kotlin is implemented, proposed,
or changed. No `src/` or `tests/` file is modified. Nothing is staged,
committed, or pushed by this document.

Programme: **Evidence Custodian — Final Freeze Review.**

---

## 1. Scope and Method

This review reads, in full: the Parker Constitution
(`docs/architecture/parker-constitution.md`); Epistemic Integrity
Amendment No. 1 (`docs/architecture/epistemic-integrity.md`), with
particular attention to Articles II, VIII, IX, and XIX; CDR-006; the
Evidence Artifact Contract Design; the Evidence Custodian Scope Lock;
the Evidence Custodian Implementation Plan; and the six already-shipped
implementation phases and their tests (`src/interfaces/EvidenceCustodian.kt`,
`src/interfaces/EvidenceArtifactStorage.kt`,
`src/runtime/DefaultEvidenceCustodian.kt`,
`src/runtime/FileSystemEvidenceArtifactStorage.kt`,
`src/runtime/InMemoryEvidenceArtifactStorage.kt`,
`src/runtime/EvidenceRegistrationCoordinator.kt`, and the corresponding
files under `tests/contracts` and `tests/runtime`).

Every CDR-006 Decision Rule, every Contract Design Required
Responsibility and Non-Responsibility, and every Scope Lock in-scope/
excluded-capability entry was checked against (a) the Constitution's own
text, (b) Article IX's own text, and (c) the compiled interface and
implementation code — not merely against other governance documents'
citations of one another. Claims checkable directly against code (for
example, "Memory Core's Document contract is unamended," or "no delete
operation exists") were verified against the current repository state,
not accepted on a citing document's word.

---

## 2. Finding 1 — Procedural defect: CDR-006 was never taken through Final Freeze Verification

This is the only defect this review identifies.

CDR-006's own Status section stated, unchanged since it was written:
*"**Draft.** This record is not Accepted, not Canonical, and not
Frozen. It has not yet undergone independent constitutional
verification or Final Freeze Verification."* The Evidence Artifact
Contract Design, the Evidence Custodian Scope Lock, and the Evidence
Custodian Implementation Plan each closed with an identical
self-declaration — *"WRITTEN TO REPOSITORY — PENDING CONSTITUTIONAL
REVIEW"* — and each stated that the next document in the sequence was
"not begun, and not authorised to begin, by this document."

No commit in this repository's history closed that loop for any of the
four documents, despite an established, repeated pattern for doing so
elsewhere: `2d5de9a Ratify Epistemic Integrity Amendment v1.0` (after
which `epistemic-integrity.md`'s own header was rewritten to `Version:
1.0 — Ratified` with a Ratification Date); `b3f7090 docs(governance):
freeze constitutional comparison model`; `7bcb0e0 Programme 3: Freeze
Unit 7 Scope Lock Clarification after constitutional verification`; and
Memory Core's own Contract Design and Implementation Plan, which close
with unambiguous terminal markers (`READY FOR SCOPE LOCK`, `READY FOR
IMPLEMENTATION`) rather than a "pending" qualifier.

This is not stale boilerplate. `CDR-005` carries the identical
Draft/not-yet-frozen self-declaration, and, consistent with that status,
zero production code anywhere in `src/` or `tests/` references it — this
project's demonstrated practice is not to build on an unfrozen CDR.
Evidence Custodian was the one place that practice was not followed: six
implementation phases and a full behavioural test suite were built
directly on a CDR that, by its own words, had never been verified or
frozen.

---

## 3. Finding 2 — Consistent with the Parker Constitution

- **"Parker owns authority. Modules provide capability."** CDR-006
  assigns the Evidence Custodian technical custody only, never
  authority — every custody-changing act (acceptance, deletion) remains
  gated by Permission Engine authorisation under CDR-006's own Decision
  Rules and Contract Design §6.6/Scope Lock §7.
- **"If a safeguard cannot be pointed to in the architecture, it does
  not count as a guarantee."** CDR-006 discloses candidly that, on its
  own, Model B leaves "a constitutional commitment without a built
  enforcement mechanism." The Contract Design, Scope Lock, and
  Implementation Plan exist to close exactly that gap, and Phases 1–6
  close it for acceptance, retrieval, storage immutability, and
  provenance/derivative traceability.
- **No-bypass.** Verified directly in code: `DefaultEvidenceCustodian.accept`
  and `.retrieve` each call `PermissionEngine.evaluate` before any
  storage access and return `Rejected` without minting an identifier or
  touching storage on denial; `EvidenceRegistrationCoordinator.register`
  evaluates two further, independent gates before
  `MemoryCore.createProvenance` and `MemoryCore.registerDocument`. No
  self-authorisation path exists anywhere in the reviewed code.
- **Owner control / "own their data."** CDR-006's deletion carve-out —
  an owner's own authorised, audited deletion is never itself a
  subsystem "requiring, justifying, or performing" destruction for its
  own convenience — restates the Constitution's owner-control principle
  without narrowing it.

No conflict found.

---

## 4. Finding 3 — Consistent with Article IX, as amended

CDR-006's Decision Rules were checked clause-by-clause against Article
IX's ratified text:

| CDR-006 Decision Rule | Article IX text | Consistent? |
| --- | --- | --- |
| Immutability while retained is a structural, unconditional obligation once custody attaches, not subject to a disclosed-departure exception | "This custody-preservation obligation is absolute for as long as the original evidence remains retained, and is not displaced, narrowed, or excused by Article II's general 'reasonably practical' qualification or by a disclosure that preservation was impractical." | Yes |
| Constitutional Optimisation Safeguard, including the illustrative list (OCR-then-delete, PDF-replaced-by-extracted-text, embedding-replaces-original) | "No subsystem may require, justify, or perform the destruction, alteration, replacement, or loss of a preserved original in order to perform its own function... including... on the grounds that a derivative artefact... is more efficient to store, process, or retrieve." | Yes — near-verbatim |
| Deletion remains available, gated, and is not itself a violation of the preservation duty | "...an explicit, authorised, and audited deletion, requested by the owner, remains available exactly as elsewhere provided, and is not itself a violation of this paragraph." | Yes |
| This record does not decide legal ownership | "This paragraph does not determine, and does not require Parker to determine, legal ownership, copyright, or proprietary interest in the evidence." | Yes |
| Separate-identity requirement for derivatives | "Parker shall not represent derivative evidence as though it were original evidence." | Yes — CDR-006 extends an already-present Article IX principle |

No wording in CDR-006 weakens, narrows, or contradicts Article IX. 
Article XIX independently names "Evidence Intelligence" and "Document
Intelligence" as subsystems inheriting Article VIII/IX obligations
before either exists — consistent with, and presupposed by, CDR-006's
framing of "Evidence Intelligence" as a provisional label whose
organisational position the Contract Design, not CDR-006, resolves.

---

## 5. Finding 4 — Consistent with Memory Core's frozen governance

CDR-006's rejection of Model A (Memory Core custody) rests on Memory
Core Scope Lock §4's already-frozen exclusion of Document
Handling/OCR/artefact-content concerns. This review confirms, directly
in code rather than by citation, that this boundary has not been
reopened:

- `src/interfaces/MemoryCore.kt`'s `Document`, `Provenance`,
  `CandidateDocument`, and `CandidateProvenance` data classes carry no
  Evidence-Custodian-specific field. `Document.locationReference` and
  `CandidateDocument.locationReference` remain the same generic
  `String` field the Contract Design §6.1 describes reusing — the
  Evidence Custodian populates it with an `EvidenceArtifactId.value`,
  exactly as designed, without Memory Core's own contract changing.
- No file under `KnowledgeStore.kt`, `DefaultKnowledgeCandidateEvaluator.kt`,
  `DefaultKnowledgeRevisionEvaluator.kt`, or
  `DefaultKnowledgeRetirementEvaluator.kt` references `EvidenceCustodian`,
  `EvidenceArtifactId`, or any Evidence Custodian type. Every "evidence"
  occurrence in these files is Knowledge Memory's own pre-existing
  `evidenceReference`/`MemoryCoreRecordReference` concept, confirming
  Contract Design §6.4's "invisible to Knowledge Memory" claim holds in
  the compiled system, not only in prose.
- `EvidenceRegistrationCoordinator` holds exactly three dependencies
  (`EvidenceCustodian`, `MemoryCore`, `PermissionEngine`); neither
  `EvidenceCustodian` nor `MemoryCore` references the other or this
  coordinator, matching Contract Design §6.1's "decoupled by reference,
  not by call" requirement exactly.

No conflict found.

**Minor, non-blocking terminology note:** CDR-006 and the Contract
Design refer to Memory Core's derivative-traceability mechanism as
`derivedFromReferences`/`extractedFromReference`; the actual, already-frozen
field names in `MemoryCore.kt` are `derivedFrom: List<ProvenanceId>` and
`extractedFrom: DocumentId?`. This naming predates CDR-006 and describes
a Memory Core contract CDR-006 did not touch — the mechanism relied upon
exists and behaves exactly as described; only the literal field names
quoted in prose differ. No correction is constitutionally required.

---

## 6. Finding 5 — Implemented code (Phases 1–6) does not exceed what is authorised

Checked directly against Scope Lock §3 (in-scope) and §4 (excluded):

- `EvidenceCustodianScopeTest.kt` confirms `EvidenceCustodian` declares
  exactly `accept` and `retrieve` — no `delete`, `search`, `list`,
  `query`, `find`, `analyse`, `classify`, `hash`, `extract`, `ocr`, or
  `summarise` operation exists anywhere in the compiled interface. The
  same test asserts no `EvidenceIntelligence` type exists anywhere in
  the repository, and that `CandidateEvidenceArtifact`,
  `AcceptedEvidenceArtifact`, and `EvidenceRetrievalResult`'s variants
  carry no owner, classification, provenance, hash, checksum, or
  location field.
- `EvidenceArtifactStorage` exposes only `write` (no-overwrite, via
  `DuplicateIdentifier`) and `read`; no code path in either storage
  implementation can modify previously written content. The one
  `Files.deleteIfExists` call in `FileSystemEvidenceArtifactStorage`
  deletes only its own temporary staging file during an atomic
  write-then-rename — it never touches an already-accepted artefact.
- No legal-ownership, retention/expiry, or Evidence-Intelligence-organisational
  decision is made anywhere in the reviewed code, consistent with
  CDR-006's own Non-Decisions list.

No excluded capability from Scope Lock §4 was found, directly or
indirectly, anywhere in the implemented code.

---

## 7. Determination and Recommendation

**Determination.** No constitutional, governance, or scope change is
required to CDR-006, the Evidence Artifact Contract Design, the
Evidence Custodian Scope Lock, or the Evidence Custodian Implementation
Plan before ratification. All four remain consistent with the Parker
Constitution and Epistemic Integrity Amendment No. 1 as ratified, and
the code and tests implementing Phases 1–6 are faithful to what these
documents authorise and exclude everything they exclude. The single
defect identified — Finding 1 — is procedural, not substantive, and is
the gap this review exists to close.

**Recommendation.**

1. CDR-006 is fit for ratification as drafted — Model B (Evidence
   Intelligence Custody) adopted, Models A and C rejected — without
   amendment.
2. The Evidence Artifact Contract Design, the Evidence Custodian Scope
   Lock, and the Evidence Custodian Implementation Plan are each fit to
   be marked accepted/frozen, without amendment, on the strength of
   this review.
3. The recording mechanism should mirror the precedent already
   established for Memory Core, the Permission Engine, and Epistemic
   Integrity: each document's own status header updated to an
   unambiguous ratified/frozen marker, with this review as the
   disclosed basis.
4. This review does not alter the sequencing conclusion of the prior
   governance review of this Programme: once the freeze above is
   recorded, Phase 7 (Deletion Workflow) remains the correct next
   implementation unit, followed by Phase 8 (Optimisation Safeguard
   Enforcement), a genuine Phase 9 verification pass covering all
   Section 3 objectives including 7 and 8, and Phase 10 (Runtime
   Integration) last.

---

## Final Report

**Decision reviewed:** CDR-006 — Model B (Evidence Intelligence
Custody) — Adopted; Models A and C rejected.

**Outcome:** No constitutional, governance, or scope change required.
One procedural gap identified (Section 2) and one non-blocking
terminology note (Section 5). Recommendation: ratify.

CDR-006 INDEPENDENT CONSTITUTIONAL FINAL FREEZE REVIEW — COMPLETE

Confirmed: no Kotlin implemented, proposed, or changed; no test
modified; CDR-006, the Evidence Artifact Contract Design, the Evidence
Custodian Scope Lock, the Evidence Custodian Implementation Plan, the
Parker Constitution, and Epistemic Integrity Amendment No. 1 all
unmodified by this review; nothing staged; nothing committed; nothing
pushed by this review.
