# Evidence Custodian — Scope Lock

## Status

Programme: **Evidence Custodian — Scope Lock, Phase 1.**
Phase: **Final governance document before implementation planning.** No
Kotlin is implemented, proposed as a diff, or changed by this document.
No API, database schema, hashing algorithm, or storage technology is
specified. Neither `src/` nor `tests/` is touched. Nothing is staged,
committed, or pushed.

**This document is binding.** It does not redefine any constitutional
decision already made. `docs/decisions/CDR-006_CONSTITUTIONAL_CLASSIFICATION_OF_ORIGINAL_EVIDENCE_CUSTODY_AND_IMMUTABILITY.md`,
`docs/architecture/EVIDENCE_ARTIFACT_CONTRACT_DESIGN.md`, and
`docs/architecture/epistemic-integrity.md` (Article IX, as amended) are
frozen, normative inputs — this document does not reopen the custody
classification, the Custodian/Evidence Intelligence separation, the
legal-ownership exclusion, or any constitutional question those three
already settled. Its own purpose is narrower and final: fix, without
ambiguity, exactly what the Evidence Custodian's first implementation
builds, and exactly what it does not. Every capability considered below
is marked `IN SCOPE` or `OUT OF SCOPE`. There is no third category.

**Scope Lock Principle.** The Evidence Custodian's first implementation
shall establish a truthful, governed system of technical custody for
preserved original evidence. It is not intended to become a document
management system, an analytical capability, or a second Memory Core.
Where a candidate capability is plausible, useful, or eventually
necessary but not required to satisfy that one sentence, it is `OUT OF
SCOPE` — the burden of proof favours exclusion, not inclusion, throughout
this document.

---

## 1. Executive Summary

CDR-006 decided that a future Evidence Custodian, not Memory Core, holds
technical custody of preserved original evidence artefacts. The Evidence
Artifact Contract Design then fixed the Custodian's constitutional
contract: a first-class infrastructure subsystem, a peer of Memory Core,
separate from Evidence Intelligence, responsible for custody and
immutability enforcement only. This Scope Lock is the next, final
governance stage before implementation planning: it freezes exactly which
of the Contract Design's already-authorised responsibilities the first
implementation builds, states explicitly what it must never become, and
fixes every subsystem boundary — without choosing a storage technology,
API, schema, or any other implementation detail.

---

## 2. Constitutional Purpose

The Evidence Custodian is a first-class Parker infrastructure subsystem
responsible only for the technical custody of preserved evidence
artefacts — a PDF, an email, a screenshot, a photograph, an audio
recording, a video recording, or metadata accompanying any of these. It
preserves, protects, and makes traceable. It does not reason, interpret,
analyse, or determine legal ownership (Contract Design §3). This purpose
is fixed and does not expand during implementation.

---

## 3. In-Scope Responsibilities

| In scope | Basis |
| --- | --- |
| Accepting evidence into custody after Permission Engine authorisation | Contract Design §4 ("Accepting evidence into custody"); §6.6 |
| Preserving immutable originals | Contract Design §4 ("Maintaining immutable preserved originals"); Article IX, as amended |
| Maintaining technical custody and availability | Contract Design §4 ("Maintaining technical custody") |
| Maintaining custody-side integrity facts consistent with Memory Core's Provenance record | Contract Design §4 ("Preserving provenance relationships"); §6.2 |
| Supporting stable references (the identifier a Memory Core `Document.locationReference` names) | Contract Design §6.1 |
| Supporting authorised read access, including access requested by Evidence Intelligence acting only as a consumer | Contract Design §4 ("Supporting derivative artefact generation"); §6.3, §6.6 |
| Supporting separately identified derivative artefacts, without generating them itself | Contract Design §4 ("Supporting derivative artefact generation"); §5 ("Perform OCR" is excluded) |
| Preserving traceability to originals via Memory Core's existing Provenance mechanism | Contract Design §4 ("Preserving traceability"); §6.2 |
| Structurally refusing unauthorised modification, as a default posture | Contract Design §4 ("Refusing unauthorised modification") |
| Executing owner-authorised, audited deletion | Contract Design §4 ("Supporting authorised deletion requests"); §6.6, §8 |
| Enforcing the Constitutional Optimisation Safeguard | Contract Design §4 ("Enforcing the Constitutional Optimisation Safeguard"); Article IX, as amended |

No responsibility beyond this table is in scope. Each entry is already
authorised by the Contract Design; none is introduced here for the first
time.

---

## 4. Explicit Exclusions

| Excluded capability | Reason |
| --- | --- |
| Reasoning | Contract Design §5; Constitution's "cognition proposes" |
| Interpreting | Contract Design §5 |
| Analysing | Contract Design §5; belongs to Evidence Intelligence (§6.3) |
| Summarising | Contract Design §5; produces Derived Work Product (§6.5) |
| Performing OCR | Contract Design §5; Memory Core Scope Lock §4's own Document Handling exclusion, extended here |
| Performing transcription | Contract Design §5, by the same reasoning as OCR |
| Determining truth | Contract Design §5; Epistemic Integrity Article III |
| Determining authenticity | Contract Design §5; an Evidence Intelligence question, not a custody function |
| Determining evidential weight | Contract Design §5; Article IV, Programme 3's `EvidentialState` |
| Determining legal ownership | CDR-006, Decision Rules; Contract Design §5 |
| Creating Knowledge Items | Contract Design §5; Programme 3 Unit 6's own, exclusive promotion path |
| Performing Knowledge promotion | Contract Design §5, §6.4 |
| Generating Derived Work Product | Contract Design §6.5; CDR-006 ("Derived Work Product is never evidence") |
| Becoming a general document-management system | Scope Lock Principle, above; no such capability is authorised by the Contract Design |
| Becoming Evidence Intelligence | Contract Design §6.3 — the Custodian "is not part of Evidence Intelligence and is not an intelligence capability" |
| Becoming Memory Core | Contract Design §6.1 — both are peer infrastructure; neither contains the other; Memory Core's Document/Provenance contracts are not reopened |
| Becoming a reasoning-provider storage layer | No reasoning provider has any custody-level relationship to the Custodian under any frozen document; introducing one would bypass the Permission Engine boundary (§7, below) |
| Bypassing the Permission Engine | Contract Design §6.6; Constitution's no-bypass principle |
| Modifying originals in place | Article IX, as amended; Contract Design §4, §8 |
| Replacing originals with derivatives | Constitutional Optimisation Safeguard (Article IX, as amended; Contract Design §4, §8) |
| Deleting evidence for efficiency or optimisation | Constitutional Optimisation Safeguard, explicitly |
| Silently repairing evidence | Article IX (unamended text, "shall not silently repair, clean, enhance, or normalise") |
| Creating hidden or inferred provenance | Contract Design §6.2 — Provenance remains exclusively Memory Core's own contract |
| Making legal or evidential conclusions | Contract Design §5 ("Determine truth," "Determine authenticity," "Determine legal ownership") |

No excluded capability above may be introduced by a future implementation
without a new or amended governance decision at the appropriate tier
(Section 10, Change Control).

---

## 5. Subsystem Boundaries

The Evidence Custodian's relationship to every adjacent subsystem is
fixed as follows. In every case, **access to a custodied artefact does
not confer custody authority.**

| Subsystem | Frozen boundary |
| --- | --- |
| Owner | The owner owns evidence; the Custodian never does (CDR-006). Owner-authorised requests (acceptance, deletion) are the only requests the Custodian executes without further internal question, and even these remain Permission-Engine-gated (Section 7). |
| Constitution | Governs the Custodian exactly as it governs every other subsystem — no capability, however operationally convenient, may create a path around "cognition proposes, trust authorises, runtime executes." |
| Permission Engine | Sole authority for authorising acceptance, deletion, and analytical access to a custodied artefact (Section 7). The Custodian never self-authorises. |
| Runtime | Executes only what the Permission Engine has authorised, exactly as it does for every other subsystem; carries no independent custody authority of its own. |
| Memory Core | Registration and provenance infrastructure only; decoupled from the Custodian by reference, not by call (Contract Design §6.1). Memory Core's Document and Provenance contracts are not amended, expanded, or reinterpreted by this document. |
| Provenance | Owned exclusively by Memory Core's Provenance contract; the Custodian supplies underlying custody-side facts but never writes or owns a Provenance record (Contract Design §6.2). |
| Evidence Intelligence | A separate, downstream, first-class-distinct subsystem under its own future governance. May request authorised read access to a custodied artefact; acquires no custody, modification, or deletion authority by doing so, and cannot bypass the Permission Engine to obtain it (Contract Design §6.3). |
| Knowledge Memory | Continues to read only Memory Core, never the Custodian directly, and never writes to Memory Core — entirely unaffected and invisible to this boundary (Contract Design §6.4). |
| World Model | No relationship. Nothing in the Contract Design or CDR-006 creates one, and this document does not create one either. |
| Reasoning providers | No custody-level relationship of any kind. A reasoning provider may propose that evidence be accepted, analysed, or deleted; it may never itself hold, modify, or delete a custodied artefact, and any such proposal remains subject to ordinary Permission Engine authorisation exactly like any other proposal. |
| Derived Work Product capabilities | Trace factual claims back to custodied originals exclusively through Memory Core's existing Provenance chain — never through a direct, bypassing request to the Custodian (Contract Design §6.5). |

---

## 6. Evidence Identity

Frozen without exception:

- Each accepted original has its own stable identity, assigned once at
  acceptance and never reassigned.
- Each derivative artefact (OCR output, transcription, extraction,
  thumbnail, normalised copy, preview, summary, embedding) has its own,
  separate identity, distinct from the original it was derived from.
- Derivatives never replace originals, regardless of accuracy,
  completeness, or operational convenience (Constitutional Optimisation
  Safeguard).
- Original and derivative identity can never be merged, aliased, or
  otherwise made indistinguishable.
- Every relationship between an original and a derivative remains
  traceable exclusively through the authorised provenance mechanism
  Memory Core's Provenance contract already provides
  (`derivedFromReferences`/`extractedFromReference`) — no parallel or
  custodian-owned traceability mechanism is authorised or required.

---

## 7. Permission Boundaries

Frozen without exception:

- Acceptance of an artefact into custody requires Permission Engine
  authorisation before the Custodian executes it.
- Deletion requires explicit owner authorisation and audit, through the
  same gated mechanism as Memory Core's own `DELETED`/`PermissionAction.DELETE`
  precedent.
- Analytical access — any request from Evidence Intelligence, a
  reasoning provider, or any other subsystem to read a custodied artefact
  for a purpose beyond the Custodian's own responsibilities — requires
  Permission Engine authorisation.
- **Authorised read access confers no write, mutation, or replacement
  capability.** Read access — whether granted to Evidence Intelligence, a
  reasoning provider, or any other subsystem under Section 5 — is
  observational only. It confers no write capability, no mutation
  capability, no replacement capability, and no indirect ability to
  modify a preserved original, beyond the specific authorised retrieval
  it was granted for.
- No subsystem, including the Custodian itself, may self-authorise a
  custody-changing action (acceptance, deletion) or grant itself
  analytical access.
- Refusal of a prohibited modification, replacement, or unauthorised
  request is the Custodian's structural default posture. Refusal does
  not itself require a Permission Engine decision on each occurrence —
  only the affirmative acts above (acceptance, deletion, analytical
  access) are gated proposals.

---

## 8. Immutability and Deletion

The narrow constitutional distinction this Scope Lock fixes:

- **Modification is prohibited**, absolutely, for as long as a custodied
  original remains retained — not subject to Article II's general
  "reasonably practical" qualifier once custody has attached (Article IX,
  as amended).
- **Deletion is not modification.** It is a distinct, terminal act ending
  custody entirely, never an in-place alteration of a retained artefact.
- **Deletion is permitted only through the explicit, owner-authorised,
  audited path** (Section 7) — mirroring Memory Core Scope Lock §8's own
  `DELETED`-is-terminal, owner-erasure-only precedent.
- **No convenience, storage pressure, analytical preference, derivative
  quality, or optimisation rationale may justify deletion.** This is the
  Constitutional Optimisation Safeguard's operative rule, restated here as
  a binding scope boundary: a derivative's existence, however good, is
  never grounds for discarding the original.

---

## 9. Implementation Independence

This document does not specify, and no future reader should treat it as
having specified: Kotlin types; interfaces; methods; file systems; object
stores; databases; schemas; hashing algorithms; encryption systems;
storage layouts; APIs; network protocols; deployment models; retention
schedules; or backup technologies. Every one of these belongs to a later
governance stage — an Implementation Plan, not yet begun and not
authorised to begin by this document.

---

## 10. Change Control

Once approved, any change to the constitutional scope this document locks
requires, in every case:

1. **Explicit governance review** — a change is never made silently as
   part of ordinary implementation work.
2. **Identification of the constitutional basis** for the change, citing
   the specific section of CDR-006, the Contract Design, Article IX, or
   this Scope Lock the change affects.
3. **A new or amended Constitutional Decision Record** where the change
   affects a constitutional boundary — the custody/legal-ownership
   distinction, the Custodian/Evidence Intelligence separation, the
   Memory Core boundary, or any Permission Engine gating requirement.
4. **Owner approval before implementation** of the change — consistent
   with the Constitution's "the owner remains in control" principle.

A change that merely selects among already-authorised implementation
options (Section 9) does not require this process; a change that
narrows, widens, or reinterprets any responsibility, exclusion, boundary,
or guarantee fixed above does.

---

## 11. Verification Questions

**1. Does this Scope Lock implement rather than expand the Contract
Design?** Yes. Every in-scope responsibility (Section 3) and every
exclusion (Section 4) traces to a specific Contract Design section or
CDR-006 rule; no new responsibility, capability, or authority is
introduced anywhere in this document.

**2. Does Memory Core remain registration-only?** Yes. Section 5 and
Section 4's "Becoming Memory Core" exclusion both confirm Memory Core's
Document and Provenance contracts are unamended and unexpanded.

**3. Does Evidence Intelligence receive no custody authority?** Yes.
Section 5 states this explicitly: authorised read access never confers
custody, modification, or deletion authority, and Evidence Intelligence
cannot bypass the Permission Engine to obtain any of them.

**4. Do originals remain immutable while retained?** Yes. Section 8
states this as an absolute obligation, not subject to any
practicability qualifier once custody attaches.

**5. Does Archived weaken preservation?** No. Not defined as a
separate section number in this document beyond what the Contract Design
already fixed; this Scope Lock does not reopen, narrow, or add exception
to the Contract Design §7 Archived definition — every obligation in
Sections 3, 6, and 8 above applies identically to an artefact regardless
of archival designation, and this document does not decide when archival
occurs, who may request it, or how it is implemented.

**6. Do derived artefacts remain separately identified?** Yes. Section 6
fixes this without exception.

**7. Does deletion remain owner-authorised and audited?** Yes. Sections 7
and 8 both fix this without exception, and Section 4 excludes any
optimisation-based justification for deletion.

**8. Has no implementation technology been selected?** Correct — none.
Section 9 excludes every implementation-level choice by name.

**9. Was no constitutional conflict found?** Correct — none. This
document is consistent with CDR-006, the Contract Design, Article IX (as
amended), the Parker Constitution, Memory Core's Contract Design and
Scope Lock, and Programme 3 Knowledge Memory governance.

**10. Is the document ready for constitutional review?** Yes.

---

## Final Recommendation

This Scope Lock is ready to be presented for constitutional review. If
approved, the next governance stage is an Evidence Custodian
Implementation Plan — not begun, and not authorised to begin, by this
document.

EVIDENCE CUSTODIAN SCOPE LOCK — WRITTEN TO REPOSITORY — PENDING
CONSTITUTIONAL REVIEW

Confirmed: no Kotlin implemented; no API, schema, or storage technology
defined; Memory Core Contract Design and Scope Lock unmodified; Knowledge
Memory governance unmodified; CDR-006, the Evidence Artifact Contract
Design, and Article IX unmodified; nothing staged; nothing committed;
nothing pushed; Implementation Plan not started.
