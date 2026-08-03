# Evidence Intelligence Unit 2 — Retrieval Result Governance Remediation Plan

## Status

**Programme:** Evidence Intelligence, Implementation Unit 2 ("Governed
Input Resolution") — pre-implementation. **Phase:** Governance
remediation proposal, prepared in response to the mandatory
`EvidenceRetrievalResult` verification gate's failure. **This document
is itself a proposal, not an amendment.** It recommends what Evidence
Custodian's own Contract Design and Scope Lock, and Evidence
Intelligence's own Implementation Plan, should say once amended or
annotated; it does not itself change what any of them currently says.
No Kotlin is implemented, proposed as a diff, or changed by this
document. Neither `src/` nor `tests/` is touched.
`docs/architecture/EVIDENCE_ARTIFACT_CONTRACT_DESIGN.md`,
`docs/architecture/EVIDENCE_CUSTODIAN_SCOPE_LOCK.md`,
`docs/architecture/EVIDENCE_CUSTODIAN_IMPLEMENTATION_PLAN.md`,
`docs/architecture/EVIDENCE_INTELLIGENCE_CONTRACT_DESIGN.md`,
`docs/architecture/EVIDENCE_INTELLIGENCE_SCOPE_LOCK.md`,
`docs/implementation/EVIDENCE_INTELLIGENCE_IMPLEMENTATION_PLAN.md`, and
`docs/decisions/CDR-006_CONSTITUTIONAL_CLASSIFICATION_OF_ORIGINAL_EVIDENCE_CUSTODY_AND_IMMUTABILITY.md`
are all unmodified by this document. Nothing is staged, committed, or
pushed.

**Ratification status: Drafted. Presented for Independent Constitutional
Review.** Not yet reviewed or accepted. This document authorises nothing
by its own drafting.

**Normative inputs, frozen, not redefined:** `docs/architecture/parker-constitution.md`;
`docs/architecture/epistemic-integrity.md`; `docs/decisions/CDR-006_CONSTITUTIONAL_CLASSIFICATION_OF_ORIGINAL_EVIDENCE_CUSTODY_AND_IMMUTABILITY.md`
("CDR-006"); `docs/architecture/EVIDENCE_ARTIFACT_CONTRACT_DESIGN.md`
("the Custodian Contract Design" — Accepted and frozen); `docs/architecture/EVIDENCE_CUSTODIAN_SCOPE_LOCK.md`
("the Custodian Scope Lock" — Accepted and frozen); `docs/architecture/EVIDENCE_CUSTODIAN_IMPLEMENTATION_PLAN.md`
("the Custodian Implementation Plan"); `docs/architecture/EVIDENCE_INTELLIGENCE_CONTRACT_DESIGN.md`,
`docs/architecture/EVIDENCE_INTELLIGENCE_SCOPE_LOCK.md`, and
`docs/implementation/EVIDENCE_INTELLIGENCE_IMPLEMENTATION_PLAN.md`
(the Evidence Intelligence governance chain, as amended by GCR-EI-UNIT1-001's
own remediation); and `docs/decisions/CDR-005_CONSTITUTIONAL_ADMISSION_OF_PERMISSIONENGINE_PROPOSAL_CLASSES.md`
("CDR-005"), consulted here only for its general precedent on when a new
Constitutional Decision Record is required.

---

## 1. Blocker Identifier

**GCR-EI-UNIT2-001 — Rejected Retrieval Identity Defect.**

Discovered by the mandatory `EvidenceRetrievalResult` verification gate
the accepted Evidence Intelligence Implementation Plan (§8, Unit 2)
requires before Unit 2 may be treated as complete. The gate has failed;
this document is the resulting governance return.

---

## 2. The Precise Defect

`EvidenceRetrievalResult` (`src/interfaces/EvidenceCustodian.kt`), read
directly from its declaration, has exactly three cases:

- `Found(evidenceArtifactId, content)`
- `NotFound(evidenceArtifactId)`
- `Rejected(reason)`

**`Rejected` carries no `EvidenceArtifactId`.** Its sole field is a
plain-language `reason: String` (confirmed against `DefaultEvidenceCustodian.retrieve`:
the constructed reason names the requesting principal and the Permission
Engine's decision outcome — it does not name the artefact identifier
being retrieved).

Evidence Intelligence Unit 2 requires every unsuccessful input, within a
multi-input analysis, to remain individually identifiable and externally
disclosable in structured data, alongside whatever inputs succeeded
(Evidence Intelligence Contract Design §11; Implementation Plan §8, Unit
2). `Rejected`, as currently shaped, cannot supply that identity — a
`Rejected` value carries no field a caller, or anything the caller later
hands that value to, can read to determine which of several
concurrently-retrieved artefacts was denied.

---

## 3. Distinctions

**`Found` and `NotFound` already preserve identity.** Both carry
`evidenceArtifactId` as a declared field. A caller (or anything
downstream of the caller) holding either value alone can determine
exactly which artefact it concerns, with no external bookkeeping of any
kind.

**`Rejected` preserves only a reason string.** That string is
human-readable prose describing *why* a request was denied (principal,
decision outcome) — it is not, and was never intended as, a structured
identifier field, and it does not name the artefact in question at all
in its current construction.

**Structured contract data is distinct from logs, list position, caller
memory, and message parsing — and only the former satisfies the gate.**
A `Rejected` value's identity can, today, be recovered only by one of
four means, all of which the accepted Implementation Plan's own
verification gate explicitly disqualifies:

- the calling code's own local variable, held from the loop or call
  site that supplied the identifier ("caller memory");
- the position of the `Rejected` value within a list of results,
  correlated back to the position of the corresponding request ("list
  position");
- parsing the free-text `reason` string for a substring that happens to
  resemble an identifier ("message parsing" — moot in any case, since
  the current `reason` string does not contain the identifier at all);
- a log line emitted at the point of the original call, correlated
  after the fact by a human or a log-aggregation query ("logging").

None of these make the identity part of the `EvidenceRetrievalResult`
value itself. A structural fix must instead be a field on `Rejected`,
exactly as `Found` and `NotFound` already have.

---

## 4. Implementation Stop Condition

- **Unit 2 production implementation has not begun.** No Kotlin file
  under `src/` or `tests/` was created or modified in the course of
  running this gate or preparing this remediation plan.
- **Unit 2 must not begin until this governance gap is repaired.** The
  Implementation Plan's own Unit 2 completion criteria (§10, item 13)
  make the verification gate's passing a precondition of Unit 2's
  completion; it has not passed.
- **Unit 3 and every later unit remain blocked**, exactly as the
  Implementation Plan's own sequencing freeze (§7) already requires —
  no unit may begin before every unit it depends on has met its own
  completion criteria, and every unit from Unit 3 onward depends,
  directly or transitively, on Unit 2.
- **No ad hoc wrapper, side channel, fifth `EvidenceAnalysisResult`
  category, or logging-only solution is authorised** by this document or
  by anything discovered in the course of this gate. Nothing here
  proposes one; §5, below, analyses only a direct amendment to the
  defective type itself.

---

## 5. Analysis of the Smallest Governance Repair

**The narrowest repair under consideration:** amend
`EvidenceRetrievalResult.Rejected` so it carries both the rejected
`EvidenceArtifactId` and the existing `reason` string — no other change
to `EvidenceRetrievalResult`, `Found`, `NotFound`, `EvidenceCustodian`,
or any other Custodian type. This is analysed below on each of the axes
requested; adoption is not assumed.

**Ownership of `EvidenceRetrievalResult`.** Defined in, and owned
exclusively by, Evidence Custodian (`src/interfaces/EvidenceCustodian.kt`;
cited as such in Evidence Intelligence's own Contract Design §12
dependency table: "`EvidenceCustodian.retrieve` — Evidence Intelligence →
Evidence Custodian — Read-only access... reused, unmodified"). Evidence
Intelligence's own governance chain — Contract Design, Scope Lock, or
Implementation Plan — holds no authority to define, redefine, or amend
this type under any circumstance; every one of those three documents
already commits to reusing it "unmodified." Any change to its shape is
exclusively Evidence Custodian's own governance decision.

**Is this an Evidence Custodian contract amendment?** Yes. Whether or
not any existing Custodian document specifies this exact field (see
below), `EvidenceRetrievalResult` is an already-implemented, already-
tested, already-integrated Evidence Custodian production contract
(`DefaultEvidenceCustodian.retrieve`, `DefaultEvidenceCustodianTest.kt`,
and any other existing consumer). Changing its shape, for any reason, is
squarely a Custodian-tier contract change, never something a downstream
consumer's own governance (Evidence Intelligence's) can authorise on its
own account.

**Do the Custodian Contract Design and Scope Lock require amendment?**
Direct inspection finds **neither document names `EvidenceRetrievalResult`,
`Rejected`, or any field-level retrieval-result shape at all.** The
Custodian Contract Design's own Phase 4 responsibility ("Supporting
authorised read access," §6.3) and the Custodian Scope Lock's own Phase
4 scope entry ("Retrieval interface behaviour... authorised,
observational read access... confirming no read path can be used to
write, mutate, or replace a preserved original") are both stated at the
level of behavioural guarantee, not Kotlin field enumeration; the actual
shape of `EvidenceRetrievalResult` — including that `Rejected` carries
only `reason` — was a Unit 3 implementation-time judgment call, never
elevated to a named clause in either frozen document. **No existing
clause in either document is contradicted by adding a field to
`Rejected`** — this is a gap, not a conflict, unlike GCR-EI-UNIT1-001's
own genuine contradiction between named clauses. Nonetheless, both
documents are marked "Accepted and frozen following independent
constitutional verification and Final Freeze Verification" — under this
Programme's own consistent discipline (applied throughout the Evidence
Intelligence governance chain, including where a Contract Design was
found merely silent rather than contradicted), a change to an
already-frozen, already-implemented production contract's shape still
requires a disclosed governance record at the tier that owns it, not a
silent implementation-time fix. The recommended treatment is therefore:
- **Custodian Contract Design:** a minimal, additive clause recording
  that `EvidenceRetrievalResult.Rejected` also names the specific
  `EvidenceArtifactId` denied, alongside the existing `reason`, with the
  justification in §6, below. This is an addition filling a
  previously-unstated gap, not a correction of a false prior statement.
- **Custodian Scope Lock:** assessed as **likely not requiring
  amendment** — its own Phase 4 entry is already satisfied by any
  Kotlin realisation of "authorised, observational read access," and
  naming a field is exactly the kind of implementation-level detail the
  Scope Lock's own "no Kotlin type... is specified" Status-section
  disclaimer already places outside its remit. This is a judgment call,
  not a certainty; an independent reviewer may reasonably require a
  short confirming cross-reference in the Scope Lock rather than no
  entry at all, and §7, below, allows for that possibility explicitly.

**Do Evidence Intelligence's own governance documents require
amendment, cross-reference correction, or nothing?** **Nothing
substantive.** The Evidence Intelligence Implementation Plan's own Unit
2 text already describes reliance on "the already-authorised existing
mechanism" (`EvidenceRetrievalResult`) in general terms, without itself
asserting `Rejected`'s current field shape as sufficient in a way that
would become false once the Custodian-tier field is added — no sentence
in the Evidence Intelligence Contract Design, Scope Lock, or
Implementation Plan need change in substance. A short **status note**
recording that the verification gate ran, found `GCR-EI-UNIT2-001`, and
that Unit 2 is blocked pending the Custodian-tier repair above is
appropriate for institutional memory, but is a status annotation, not a
governance amendment, and is not required to make any existing sentence
in those three documents accurate again.

**Is a Constitutional Decision Record required?** Analysed in §4, below.

---

## 6. Preserved Semantics

The repair analysed in §5 changes nothing about:

- `Found` — unchanged in every field and every guarantee.
- `NotFound` — unchanged in every field and every guarantee.
- Permission evaluation — `retrieve`'s own two-step sequence (evaluate,
  then read or deny) is unchanged; a denied decision still produces
  `Rejected` before any storage access, exactly as today.
- The meaning of rejection — `Rejected` still means, and only means,
  "this request was not authorised"; adding an identifier field
  discloses no new authorisation outcome and creates no new disposition.
- No batch retrieval API is introduced — `retrieve` remains one
  identifier in, one `EvidenceRetrievalResult` out; multiple artefacts
  are still resolved by multiple calls, exactly as Evidence Intelligence
  Implementation Plan Unit 2 already assumes.
- No new wrapper, and no new retrieval taxonomy — the repair adds one
  field to one existing case; it does not add a fourth case, a wrapper
  around the sealed type, or a parallel result shape.
- No change to `EvidenceArtifactId` itself — the identifier type is
  reused, unmodified; the repair only lets `Rejected` carry a reference
  to a value of that type, exactly as `Found`/`NotFound` already do.
- No Evidence Intelligence production implementation — this document
  proposes no Unit 2 code, and none has been written.

The identifier a repaired `Rejected` would carry is not new information
disclosed to the caller: it is the same `EvidenceArtifactId` the caller
already supplied as `retrieve`'s own second parameter to produce that
very `Rejected` value. Echoing it back discloses nothing the caller did
not already possess — the identical pattern `Found` and `NotFound`
already establish.

---

## 7. Proposed Governance Sequence

1. **Evidence Custodian Contract Design amendment** — add the minimal,
   additive clause described in §5, above, recording `Rejected`'s
   corrected content and its justification. Re-run the review this
   Programme applies to any change to an already-frozen Contract
   Design (mirroring the Contract Design amendment already performed
   for `GCR-EI-UNIT1-001`).
2. **Evidence Custodian Scope Lock assessment** — confirm, as part of
   that same review, whether §5's "likely not required" judgment holds
   or whether a short confirming cross-reference is warranted; amend
   only if the review finds it necessary.
3. **Evidence Intelligence Implementation Plan status note** — record
   that the Unit 2 verification gate ran, identified `GCR-EI-UNIT2-001`,
   and that Unit 2 remains blocked pending steps 1–2 above; no
   substantive rewrite of Unit 2's own text.
4. **Independent Constitutional Review of the amended Custodian
   documents** — the same Final Freeze Verification discipline already
   applied when the Custodian Contract Design and Scope Lock were first
   frozen (`docs/reviews/EVIDENCE_CUSTODIAN_CDR-006_FINAL_FREEZE_REVIEW.md`),
   since reopening a document marked "frozen" warrants the same rigor
   as freezing it the first time, not a lighter pass.
5. **Only then** does the production contract amendment proceed:
   `EvidenceRetrievalResult.Rejected` gains the field, `DefaultEvidenceCustodian`
   and its own existing tests are updated for the new shape, and the
   Custodian's own existing test suite is re-run in full before any
   Evidence Intelligence work resumes.
6. **Only then** does Evidence Intelligence Unit 2 implementation begin.
   Units 3 and later remain blocked throughout steps 1–5.

---

## 8. Constitutional Decision Record Requirement Assessment

Applying the same precedent already used for `GCR-EI-UNIT1-001` — CDR-005's
Model C escalation test, the only general "when is a new CDR required"
rule this repository states — by direct analogy, with CDR-006 standing
in the role Chapter 10 plays in CDR-005's own domain:

- **Does the repair materially alter anything CDR-006 itself states?**
  No. A direct search of CDR-006's text for any mention of retrieval-
  result shape, rejection disposition fields, or Kotlin realisation
  mechanics returns nothing — CDR-006 governs custody and immutability
  classification (acceptance, storage, deletion), not the field-level
  shape of an authorised read operation's own denial outcome. The
  repair changes no custody rule, no immutability guarantee, and no
  ownership boundary CDR-006 fixes.
- **Is this "genuinely contested, ambiguous, or requiring a choice
  between constitutionally plausible readings" of CDR-006 or the
  Constitution?** No identified ambiguity. The repair echoes back to
  the caller an identifier the caller already supplied — it creates no
  new disclosure, no new authority, and no competing constitutional
  reading was found in the course of this review.

**Conclusion: applying the repository's current escalation criteria,
this remediation recommends that no new Constitutional Decision Record
is required.** A Custodian Contract Design amendment (and, if the
independent review so finds, a short Scope Lock cross-reference) is
sufficient. Consistent with CDR-005's own posture and with the identical
conclusion reached for `GCR-EI-UNIT1-001`, this is a recommendation, not
a foreclosure — the escalation valve to a new CDR remains available if
independent review disagrees.

---

## 9. Scope Discipline

This document proposes no Kotlin source code, method signature,
interface, pseudocode, or diff of any kind. It does not itself amend the
Custodian Contract Design, the Custodian Scope Lock, the Custodian
Implementation Plan, any Evidence Intelligence governance document, or
any CDR. It does not decide, only recommends, whether a Scope Lock
cross-reference is ultimately required (§5, §7). It does not begin Unit
2, Unit 3, or any later unit.

---

**Status: Drafted. Presented for Independent Constitutional Review.**
Not marked Accepted. No Constitutional Decision Record is created by
this document. No other document — the Custodian Contract Design, the
Custodian Scope Lock, the Custodian Implementation Plan, any Evidence
Intelligence governance document, any CDR, or the Parker Constitution —
is modified by this document.

EVIDENCE INTELLIGENCE UNIT 2 RETRIEVAL RESULT GOVERNANCE REMEDIATION
PLAN — DRAFTED — PRESENTED FOR INDEPENDENT CONSTITUTIONAL REVIEW

Confirmed: no Kotlin implemented; no interface, method signature, API,
schema, or storage technology defined; no pseudocode, diagram, or
implementation example included; the Custodian Contract Design, the
Custodian Scope Lock, the Custodian Implementation Plan, the Evidence
Intelligence Contract Design, Scope Lock, and Implementation Plan,
CDR-005, CDR-006, and the Parker Constitution all unmodified; nothing
staged; nothing committed; nothing pushed; Unit 2 production
implementation not begun; Unit 3 and all later units remain blocked.
