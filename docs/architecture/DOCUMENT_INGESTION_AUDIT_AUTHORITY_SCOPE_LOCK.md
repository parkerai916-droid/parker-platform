# Document Ingestion — Audit Authority Scope Lock

## 1. Purpose

Scope-lock Alignment Amendment 4 (Parker-owned ingestion audit
authority): determine and freeze the minimum governance required for
durable, auditable recording of Document Ingestion governance events —
who owns that authority, what it may and must never record or decide,
and how it relates to every subsystem Document Ingestion already
touches — without designing an implementation.

## 2. Status

**Draft for owner review. Not yet accepted, canonical, or
implementation-authorising.** Programme: **Document Ingestion —
Governance Alignment Unit 5**, scope-locking Alignment Amendment 4 only,
as identified by `DOCUMENT_INGESTION_CANONICAL_GOVERNANCE_ALIGNMENT.md`
§4 (adopted `84cc061`). No Kotlin is implemented, proposed as a diff, or
changed by this document. No dependency is added. No interface, port,
adapter, or persistence is created. No parser is installed. No evidence
is ingested. Alignment Amendment 3 and any implementation-plan unit
remain out of scope and are not begun here.

**This document reopens, redesigns, or reinterprets none of:** CDR-006,
CDR-007, `EVIDENCE_ARTIFACT_CONTRACT_DESIGN.md`, `EVIDENCE_CUSTODIAN_SCOPE_LOCK.md`,
`EVIDENCE_CUSTODIAN_IMPLEMENTATION_PLAN.md`,
`EVIDENCE_CUSTODIAN_PHASE_7_BOUNDARY_CLARIFICATION.md`,
`EVIDENCE_INTELLIGENCE_CONTRACT_DESIGN.md` and Amendments 1-2,
`EVIDENCE_INTELLIGENCE_SCOPE_LOCK.md`, `OCR_MECHANISM_CONTRACT_DESIGN.md`,
`OCR_MECHANISM_SCOPE_LOCK.md`, `OCR_MECHANISM_IMPLEMENTATION_PLAN.md`,
`EVIDENCE_PROCESSING_SEARCHABLE_PDF_SCOPE_LOCK.md`,
`EVIDENCE_PROCESSING_SEARCHABLE_PDF_IMPLEMENTATION_PLAN.md`,
`EVIDENCE_PROCESSING_SEARCHABLE_PDF_BOUNDARY_CLARIFICATION.md`,
`MEMORY_CORE_CONTRACT_DESIGN.md` and Errata 001-004, `MEMORY_CORE_SCOPE_LOCK.md`,
the RKS/QMD Contract Design/Scope Lock/Amendments, ADR-024, `src/interfaces/AuditService.kt`,
`src/interfaces/EvidenceDeletionAudit.kt`, `src/runtime/FileSystemEvidenceDeletionAudit.kt`,
and the eight documents already adopted at `84cc061`/`4faaeb8`/`1958730`/`ff589be`.
It does not amend any of those eight either.

## 3. Authorities inspected (fresh, this unit)

Read fresh, directly, for this scope lock: `docs/architecture/parker-constitution.md`
(the Auditability principle, line 74-76, quoted in full below);
`docs/architecture/epistemic-integrity.md`; `src/interfaces/EvidenceDeletionAudit.kt`
(full file, 103 lines); `src/runtime/FileSystemEvidenceDeletionAudit.kt`
(full file, 143 lines); `src/interfaces/AuditService.kt` (full file, 6
lines); `src/runtime/DefaultOwnerEvidenceDeletionAuthority.kt` (full
file, 180 lines); `tests/runtime/FileSystemEvidenceDeletionAuditTest.kt`
(test names, full); `tests/runtime/DefaultOwnerEvidenceDeletionAuthorityTest.kt`
(test names, full); `docs/architecture/EVIDENCE_CUSTODIAN_PHASE_7_BOUNDARY_CLARIFICATION.md`
§§2-7 (the governing document for the entire precedent); `docs/architecture/EVIDENCE_CUSTODIAN_SCOPE_LOCK.md`
(the "audited" obligation, §7); `docs/adr/ADR-024-module-event-audit-durability-boundary.md`
(lines 33, 129-134: no `AuditService` implementation exists; what must
eventually be durable). Confirmed no other file mentions `AuditService`
by name outside these three (direct repository grep). Cross-checked
against, and confirmed unmodified since adoption: `DOCUMENT_INGESTION_CANONICAL_GOVERNANCE_ALIGNMENT.md`
§4 (Amendment 4's own sketch); `DOCUMENT_INGESTION_GOVERNANCE_AMENDMENT_MAP.md`
Amendment 4's own row; `DOCUMENT_INGESTION_ROUTING_AND_COMPLETENESS_POLICY.md`
§5 ("Attempt audit") and §8 Owner Decision 4; `SOURCE_DERIVATIVE_PROVENANCE_MODEL.md`
§1 and §7; `DOCUMENT_INGESTION_DERIVATIVE_GENERATION_RECORD_SCOPE_LOCK.md`
§§10, 11, 13, 19, 21; `DOCUMENT_INGESTION_DERIVATIVE_REVIEW_TARGET_SCOPE_LOCK.md`
§§4.A, 12; `DOCUMENT_INGESTION_CDR007_OCR_EVIDENCE_INTELLIGENCE_CROSS_REFERENCE_SCOPE_LOCK.md`
§4.B (unmodified by this document).

## 4. Existing audit precedent

Established by direct inspection, not assumed from filenames:

- **`AuditService`** (`src/interfaces/AuditService.kt`): a two-method
  interface (`record` returning a minted `AuditRecordId`; `query`). **Zero
  implementations anywhere in this repository** (confirmed by direct
  grep: only `AuditService.kt` itself and two disclaiming comments in
  the deletion-audit files mention it). ADR-024, line 33: "no
  `AuditService` implementation exists." The Boundary Clarification §2:
  "Building its first implementation as a side effect of Phase 7 would
  mean designing general, platform-wide audit infrastructure — a
  decision disproportionate to, and not authorised by, anything
  reviewed for this clarification." This exact reasoning applies
  identically to Document Ingestion: nothing reviewed for this unit
  authorises writing `AuditService`'s first implementation either.
- **`EvidenceDeletionAudit`/`FileSystemEvidenceDeletionAudit`**: the
  governed precedent. Owned by Evidence Custodian's own Phase 7
  ("Deletion workflow"), built to discharge one frozen obligation —
  "Executing owner-authorised, audited deletion" (Scope Lock §7) — "for
  exactly one operation... and for nothing else" (`EvidenceDeletionAudit.kt`
  KDoc). Exactly one record type, `EvidenceDeletionAuditRecord`, five
  fields (`deletionRequestId: String`, `evidenceArtifactId`,
  `requestingPrincipalId`, `recordedAt`, `stage`); exactly one method,
  `record(record)`, returning `Unit`, throwing `EvidenceDeletionAuditException.PersistenceFailure`
  on genuine fault (never a result type — "a genuine, unexpected fault,"
  KDoc). `deletionRequestId` is explicitly "a correlation value only,
  never a domain identity type" (KDoc), minted by the *caller*
  (`DefaultOwnerEvidenceDeletionAuthority`), not by the port. Exactly
  two `EvidenceDeletionAuditStage` values, `AUTHORISED`/`COMPLETED` —
  "no `FAILED`, `REQUESTED`, `NOT_FOUND`... is introduced" (KDoc). No
  query capability — "Boundary Clarification Section 2 explicitly
  excludes one... a human reviewer inspects the durable record
  directly" (both the interface's own KDoc and the Boundary
  Clarification §2 verbatim). Append-only, durable via
  append-then-`FileChannel.force` — never truncates, rewrites, or
  removes a previously written line (`FileSystemEvidenceDeletionAudit.kt`
  KDoc), test-confirmed by `` `two records for one attempt are both
  appended, never overwritten` ``.
- **Ordering is the caller's responsibility, not the port's.** "This
  port itself enforces neither ordering rule — it is a pure write
  primitive" (`EvidenceDeletionAudit.kt` KDoc). `DefaultOwnerEvidenceDeletionAuthority.deleteAsOwner`
  (re-read directly, `src/runtime/DefaultOwnerEvidenceDeletionAuthority.kt`
  lines 100-142) sequences: Permission Engine evaluation → (approved)
  durably write `AUTHORISED` → physical deletion → (succeeded) durably
  write `COMPLETED`, returning `Deleted` only once that second write
  itself durably succeeds. Test-confirmed: `` `if the AUTHORISED audit
  write fails, physical deletion is never attempted` ``; `` `if the
  COMPLETED audit write fails after a successful physical deletion,
  Deleted is never returned` ``.
- **Explicitly narrow, explicitly not a precedent for general reuse as
  written.** Boundary Clarification §2: "Whether this narrow port is
  later subsumed into a future, properly governed, platform-wide
  `AuditService` is explicitly deferred and not decided here." The
  *pattern* (narrow, purpose-built, append-only, durable, no-query,
  exception-on-failure, caller-enforced ordering, correlation-only
  identity) is reusable; the *type itself*
  (`EvidenceDeletionAudit`/`EvidenceDeletionAuditRecord`) is not, by its
  own frozen terms.

## 5. Frozen objective

Document Ingestion requires a durable, auditable record of its own
governance events, discharging the same constitutional Auditability
obligation Evidence Custodian's deletion workflow already discharges for
its own domain ("Every authorized action leaves a record sufficient to
reconstruct what was proposed, what was authorized, by what authority,
and what was executed," `parker-constitution.md` line 76) — **via a new,
narrow, ingestion-specific port following the `EvidenceDeletionAudit`
pattern, never by extending that port, never by implementing
`AuditService`, and never by inventing new authority beyond recording
what already-governed callers supply.**

## 6. Central governance question — selected authority model

Of the four options the task frames:

- **(B) reuse/extension of `EvidenceDeletionAudit` — rejected.** That
  port is frozen, by its own KDoc, to "exactly one operation... and for
  nothing else." Extending it to a second, unrelated domain would
  violate its own already-adopted narrow-purpose determination
  (Boundary Clarification §2) without any governance review of that
  determination.
- **(B, alternative) reuse/extension of `AuditService` — rejected.**
  Zero implementations exist; ADR-024 and the Boundary Clarification
  both establish that writing its first implementation is "disproportionate
  to, and not authorised by," any review conducted so far — including
  this one. Nothing in the Document Ingestion programme's own adopted
  governance authorises it either.
- **(D) no new audit authority required — rejected.** `DOCUMENT_INGESTION_ROUTING_AND_COMPLETENESS_POLICY.md`
  §5 and §8 Owner Decision 4 (both already adopted at `84cc061`) already
  establish that ingestion attempts require a durable audit record and
  that "the current absence of a general ingestion-audit persistence
  port is a future implementation requirement" — i.e., adopted
  governance has already determined the capability does not yet exist.
- **(A) a wholly new, freestanding audit authority with no relationship
  to existing pattern — not selected**, because (C) is available and
  narrower.
- **(C) a narrow new ingestion-specific audit port following the
  already-adopted `EvidenceDeletionAudit` pattern — selected.** This is
  the narrowest option consistent with what adopted governance both
  requires (a durable ingestion audit exists — §8 Owner Decision 4) and
  permits (a new narrow port, not an extension of a frozen one, not an
  unauthorised `AuditService` implementation).

## 7. Audit authority owner

**Exclusively a new, Parker-owned ingestion audit port**, called only by
the Parker-owned ingestion coordinator already named throughout Units
1-4 (the same coordinator that mints `DerivativeGenerationId`s, per Unit
2 §18, and records `PENDING_REVIEW`, per Unit 3 §4.A). Mirroring the
deletion precedent exactly: the audit port itself is "a pure write
primitive" (`EvidenceDeletionAudit.kt` KDoc) — it does not decide
whether an ingestion attempt is authorised, does not decide whether a
Derivative Generation Record is admitted, and does not decide anything
about the event it records. The coordinator remains the sole authority
that both causes the governed event and causes it to be audited;
mirroring `DefaultOwnerEvidenceDeletionAuthority`'s own "not a
coordinator" structural isolation (Boundary Clarification §3), the audit
port is a separate, narrow dependency the coordinator calls — never a
second, independently-acting subsystem.

## 8. Audit/non-authority boundary

**Explicitly permitted** (mirroring `EvidenceDeletionAudit`'s own
minimal surface):

- observe an already-governed ingestion event the coordinator itself
  supplies — never one it independently detects;
- durably record that event, in whatever minimal shape Section 10 below
  fixes;
- correlate related records for one attempt via a caller-supplied
  correlation value (Section 11) — never mint a domain identity;
- record the recording act's own timestamp (Section 12) — never a
  timestamp belonging to any other event;
- report recording failure to its caller, by exception, mirroring
  `EvidenceDeletionAuditException.PersistenceFailure` — never by
  silently swallowing or downgrading a fault.

**Explicitly forbidden**, restated from the task's own list, all
grounded in the same reasoning — an audit port that could do any of
these would be exercising authority CDR-006, CDR-007, Evidence
Custodian, Unit 2, Unit 3, or Unit 4 already assign elsewhere, not
recording a fact:

- accepting, rejecting, creating, or mutating `EvidenceArtifact`
  authority of any kind (Evidence Custodian's exclusive domain, CDR-006,
  unaffected by this document);
- deleting evidence or authorising deletion (`OwnerEvidenceDeletionAuthority`'s
  exclusive, structurally isolated domain, unaffected);
- invoking OCR, choosing an OCR provider, or judging OCR quality (Unit 4
  §4.B, unaffected);
- generating derivative content, or admitting, approving, or rejecting a
  Derivative Generation Record (Unit 2 §18's authorization rule:
  admission is exclusively the ingestion coordinator's own act; the
  audit port records that act, never performs or ratifies it);
- performing Derivative Review, or determining `APPROVED`/`REJECTED`/`NEEDS_CORRECTION`
  (Unit 3 §4.A's identical authorization rule: only the ingestion
  coordinator, never a plugin or this audit port, may call
  `recordReviewState`);
- assigning evidential state, performing Evidence Intelligence analysis,
  or reasoning of any kind (CDR-007; Unit 4 §4.C-D, unaffected);
- creating Memory Core records or Knowledge Items (untouched; Section 22
  below);
- resolving parser disagreement, reconciling generations, or changing
  lineage (Unit 2 §16's multi-parser rule and §7's lineage rules —
  fixed at the generation's own creation, never touched by an audit
  write);
- manufacturing or repairing provenance, or inferring any fact not
  directly supplied by its caller — mirroring `DerivativeReviewRegistry`'s
  own identical prohibition (Unit 3 §12.4), extended here to the audit
  port by the same reasoning;
- converting its own act of recording into authority over the thing
  recorded — an audit record is evidence that an already-authorised
  event occurred, never itself the authorisation, exactly as
  `EvidenceDeletionAuditRecord.stage = AUTHORISED` is a *record of* a
  Permission Engine decision, never a substitute for one.

## 9. Ingestion coordinator relationship

Fresh-read against Unit 2's own atomicity rule (§19: "admission is
all-or-nothing at the governance boundary... or it does not exist as a
governed record at all") and Unit 3's own admission-before-review rule
(§12.1: existence must be "successfully and atomically established"
before review recording):

- **Only the Parker-owned ingestion coordinator causes an audit event to
  be recorded** — never a plugin (Plugin Contract §6, unaffected), never
  the audit port itself.
- **The audit mechanism verifies nothing; it records exactly what it is
  supplied**, mirroring `EvidenceDeletionAudit`'s own "pure write
  primitive" character. It does not re-derive, re-check, or infer any
  fact about the event.
- **Sequencing, by direct analogy to the deletion precedent's own
  two-point discipline** (never mandated as a specific stage count or
  name — that is implementation-plan work, Section 26):
  - a durable record establishing that an ingestion attempt was
    authorised/undertaken must exist *before* any irreversible
    consequence of that attempt (minting a `DerivativeGenerationId`,
    admitting a Derivative Generation Record) is treated as final —
    mirroring `AUTHORISED` preceding physical deletion;
  - a durable record of the attempt's outcome must exist, and must
    itself durably succeed, before the coordinator may report that
    outcome as final to any caller — mirroring `COMPLETED` gating
    `Deleted`.
- **Failure fail-closes the governed act, not merely the audit act.**
  `DOCUMENT_INGESTION_ROUTING_AND_COMPLETENESS_POLICY.md` §5 already
  states this: "Audit failure is fail-closed before declaring successful
  governed acceptance. An attempt record must not claim a derivative was
  custodied or registered until the corresponding durable operation
  actually succeeded." This document adds no new rule here — it
  confirms this already-adopted rule is satisfied by, and requires, the
  audit-authority model selected in Section 6.
- **Partially admitted but unaudited state is forbidden.** See Section
  14's explicit invariant.

## 10. Governed audit event/record semantics

`DOCUMENT_INGESTION_ROUTING_AND_COMPLETENESS_POLICY.md` §5 already lists
the facts a durable ingestion audit record must be able to capture. This
document does not add to, remove from, or contradict that list; it
disciplines *how* each fact is captured, per Section 13's
reference-versus-duplication rule, and classifies each fact below.

## 11. Identity semantics

**An audit record does not require its own free-standing identity.**
Mirroring the precedent exactly: `EvidenceDeletionAuditRecord` has no
`auditRecordId` field of its own — a record is addressed only by
scanning the durable log, never looked up by a minted ID. An ingestion
audit record needs only a **correlation value** — matching Routing
Policy §5's own "attempt/correlation ID" requirement — with the
identical semantic constraints the precedent already establishes for
`deletionRequestId`:

- a correlation value only, **never a domain identity type**;
- minted once, by the **caller** (the ingestion coordinator), never by
  the audit port;
- carries no meaning beyond linking the records one attempt produces to
  one another;
- must not encode chronology, authority, source identity, sequence, or
  any other semantic meaning — nothing in adopted governance requires
  otherwise, so nothing beyond opacity is fixed here.

No Kotlin type is chosen. Whether the correlation value is a bare
`String` (as `deletionRequestId` is) or a value-class wrapper is
implementation-plan work; this document fixes only the semantic
constraints above, which either representation could satisfy.

## 12. Time semantics

Five distinct time concepts, never conflated, extending the discipline
Memory Core Provenance (§7) and Unit 2 §12 already establish:

- **source-document/content date** — content's own claimed date; never
  owned by the audit record; captured, if at all, by the canonical
  record that already owns it (the Derivative Generation Record, or the
  source manifest);
- **evidence receipt/custody time** — `AcceptedEvidenceArtifact.acceptedAt`,
  unchanged, outside this document's scope;
- **derivative generation time** — Unit 2 §12's own required field on
  the Derivative Generation Record itself; the audit record references
  the generation, not this timestamp directly;
- **review time** — Unit 3's own `DerivativeReviewRecord.recordedAt`;
  likewise referenced, not duplicated;
- **audit-recording time** — required on every audit record: when *this
  audit fact* was durably recorded, mirroring `EvidenceDeletionAuditRecord.recordedAt`
  exactly. Must never be presented as, or mistaken for, any of the
  other four.

## 13. Reference versus duplication rule

**Audit is not a second canonical database.** Where a fact Routing
Policy §5 lists is already owned by an existing canonical record — the
source manifest (`SOURCE_DERIVATIVE_PROVENANCE_MODEL.md` §1: digest,
length, media type), the Derivative Generation Record (Unit 2 §10-13:
producer identity, transformation history, completeness state), or the
Derivative Review Record (Unit 3: review state) — the audit record
captures that fact **by reference to the owning record's own identity**
(`EvidenceArtifactId` or `DerivativeGenerationId`), never by copying the
owning record's own field values inline. This mirrors the precedent
exactly: `EvidenceDeletionAuditRecord` captures "which artifact" via a
bare `evidenceArtifactId` reference, never by duplicating the artifact's
own bytes or metadata into the audit log. A fact with no other canonical
owner (the attempt's own correlation value, its own operational outcome,
its own audit-recording time) is captured directly, because there is
nowhere else for it to be a reference *to*.

## 14. Immutability/append-only decision

**Immutable once recorded; append-only; never overwritten; never
silently corrected.** Directly adopted from the precedent, with no
narrowing or widening: `FileSystemEvidenceDeletionAudit`'s own KDoc
("never truncates, rewrites, or removes a previously written line") and
test-confirmed behaviour (`` `two records for one attempt are both
appended, never overwritten` ``) apply identically to ingestion audit
records. **If correction is ever required, the precedent's own pattern
already supplies the answer without inventing new semantics**: a new
record referring to the earlier one (via the same correlation value,
exactly as `AUTHORISED` and `COMPLETED` are linked, never merged, records
for one `deletionRequestId`) — never mutation of the earlier record.

**The load-bearing invariant, stated explicitly per the task's own
instruction:** Parker may never leave behind an admitted Derivative
Generation Record — one satisfying Unit 2 §19's required-semantics test
and therefore observable, referenceable, or reviewable by any consumer —
for which no durable audit record of its own admission exists. This is
not a new rule; it is Unit 2 §19's own atomicity rule ("admission is
all-or-nothing at the governance boundary") read together with the
already-adopted Routing Policy §5 fail-closed rule (Section 9, above),
made explicit for this unit exactly as the deletion precedent already
makes it explicit for deletion (`` `if the COMPLETED audit write fails
after a successful physical deletion, Deleted is never returned` ``): if
durable audit persistence for the completion fact fails, the
coordinator must not report, and no consumer may observe, that the
Derivative Generation Record was successfully admitted — regardless of
whether its content already exists in some underlying store.

## 15. Failure-atomicity decision

Per case, mirroring the precedent's own disclosed behaviour, never
inventing a mechanism:

- **Audit identity (correlation value) creation fails:** cannot occur
  under the semantics fixed here — the correlation value is a caller-minted,
  opaque value with no external dependency (mirroring
  `deletionRequestId = "evidence-deletion-${UUID.randomUUID()}"`'s own
  in-process construction); if minting it is ever impossible, that is a
  coordinator-internal fault, not an audit-port fault, and fails the
  attempt before the audit port is ever called.
- **Audit persistence fails:** the corresponding governed act must not
  be reported as, or treated as, successful. Mirrors both test-confirmed
  precedent behaviours exactly (Section 4, above) — before the
  irreversible act, the act must not proceed; after the irreversible act
  but before the completion audit succeeds, success must not be
  reported.
- **Required audit facts are unavailable:** the coordinator must not
  call the audit port with fabricated, defaulted, or inferred
  substitutes — mirroring Unit 3 §12.4's identical rule for review
  targets, extended here. An attempt lacking a required fact fails the
  attempt, not the audit call.
- **An audit write partially fails** (some bytes written, not durably
  confirmed): the write as a whole is a failure, not a partial success —
  mirroring `FileSystemEvidenceDeletionAudit.record`'s own "loop until
  every byte is actually written" plus `force`-before-return discipline,
  which this document does not re-specify as implementation but whose
  *outcome guarantee* (a call either durably completes or throws,
  never silently truncates) is the frozen semantic requirement.
- **The audit subsystem is unavailable:** the governed ingestion
  operation fails closed — no admission, no claimed success — exactly as
  Routing Policy §5 already requires.
- **An audit record cannot be durably confirmed:** treated identically
  to outright persistence failure; there is no intermediate "probably
  recorded" state a caller may rely on.

## 16. Retrieval/query boundary

**No retrieval/query capability is authorised by this scope lock.**
Mirroring the precedent exactly: "no query, no general record taxonomy...
durability is this port's entire purpose" (Boundary Clarification §2);
"a human reviewer inspects the durable record directly"
(`EvidenceDeletionAudit.kt` KDoc). This document does not create
analytics, search, reporting, Evidence Intelligence, or reasoning
authority under the label "audit retrieval." Whether ingestion audit
ever needs programmatic retrieval — plausible, given ingestion's likely
higher event volume than owner-initiated deletion — is **deferred**
(Section 26), not decided, exactly as the precedent's own §2 defers
whether it is ever "subsumed into a future, properly governed,
platform-wide `AuditService`."

## 17. Retention/deletion boundary

Fresh-read confirms the precedent is **silent** on its own log's
retention or deletion — no field, method, or governing document
inspected states whether `FileSystemEvidenceDeletionAudit`'s own log may
ever be pruned, archived, or deleted, or by what authority. This
document does not invent an answer where the precedent has none. Marked
**DEFERRED**:

- ingestion-audit-record retention policy;
- ingestion-audit-record deletion, and by what authority;
- whether audit-record deletion, if it is ever authorised at all, would
  require the same owner-only, structurally isolated shape
  `OwnerEvidenceDeletionAuthority` already requires for evidence.

**Not deferred — already settled by existing adopted invariants, applied
by direct analogy:**

- deleting a source `EvidenceArtifact` does not imply deleting its
  ingestion audit history — mirrors Unit 2 §17's "source deletion never
  authorizes silent derivative mutation," extended to audit records by
  identical reasoning: an audit record documents a historical fact about
  an attempt; deleting the artifact the attempt concerned does not erase
  the historical fact that the attempt occurred;
- deleting, or ceasing to retain, a Derivative Generation Record — if
  such deletion is ever separately authorised, which it is not by this
  document or by Unit 2 — likewise does not imply deleting its ingestion
  audit history, mirroring Unit 2 §17's identical "Source retention is
  independent of derivative retention" bullet, applied here to the
  derivative-to-audit direction by the same reasoning: the audit record
  documents that an admission attempt occurred, a historical fact
  independent of whatever later happens to the record it once documented;
  - conversely, deleting or ceasing to retain an ingestion audit record
    (if ever authorised) does not, and could not, retroactively affect
    the source or the Derivative Generation Record it once documented —
    mirroring Unit 2 §17's identical independence rule in the other
    direction.

## 18. Evidence Custodian boundary

**No interface modification.** Direct re-inspection of `EvidenceCustodian.kt`
(this session, prior units) and `EvidenceDeletionAudit.kt`/`FileSystemEvidenceDeletionAudit.kt`
(this unit) confirms: Evidence Custodian does not create ingestion audit
records — it has no relationship to this port at all, mirroring
`DefaultOwnerEvidenceDeletionAuthority`'s own "not a coordinator...
there is no `MemoryCore` reference anywhere in this class's constructor,
structurally, not merely by omission" (its own KDoc) — the analogous
statement holds for a future `DefaultDocumentIngestionAuditRecorder`
(illustrative name only, not fixed here) and `EvidenceCustodian`: no
structural dependency in either direction. Evidence Custodian **supplies
authoritative facts by reference** (`EvidenceArtifactId`) when the
coordinator constructs an audit record naming a source it custodies —
exactly as the deletion audit's own `evidenceArtifactId` field is a
reference Evidence Custodian's own identity supplies, never a fact
Evidence Custodian itself writes into the audit log.

## 19. Derivative Generation boundary

Unit 2 is unmodified. The audit port may record that a
`DerivativeGenerationId` was admitted, by reference, and may record the
attempt's own facts (Section 10, 13) — it never creates, admits,
approves, or alters a Derivative Generation Record, and Unit 2's own
authorization rule (§18: exclusively the Parker-owned ingestion
coordinator may admit) is unweakened: the audit port is not the
coordinator, has no admission authority of its own, and Section 14's
invariant strengthens, not weakens, Unit 2's atomicity guarantee.

## 20. Derivative Review boundary

Unit 3 is unmodified. The audit port has no relationship to
`DerivativeReviewRecord`/`DerivativeReviewRegistry` beyond, potentially,
recording that a review-target creation event occurred (by reference to
the target identity, Unit 3 §5) — it never performs review, never
records a `DerivativeReviewState` transition itself (that remains
exclusively `DerivativeReviewRegistry`'s own domain, Unit 3 §4.A), and
never influences what `APPROVED` means (Unit 3 §8, Unit 4 §4.I,
unaffected).

## 21. OCR/Evidence Intelligence boundary

Unit 4 is unmodified. The audit port may record, by reference, that a
Tier B transformation occurred (Unit 4 §4.B, the OCR boundary itself;
Unit 4 §4.G: "a **disclosure**, never an **endorsement**") — it never
invokes OCR, never selects a provider, never
judges recognition quality, never performs Evidence Intelligence
analysis, and never assigns evidential state. No authority laundering
occurs: an audit record's own existence confers no more evidential
weight on the fact it records than that fact already carried under its
own, unchanged governing authority (Unit 4 §4.G, extended identically
here).

## 22. Memory Core/Knowledge boundary

**Explicitly deferred to Alignment Amendment 3 — not decided here**,
per the task's own instruction. This document does not invent a Memory
Core field, reference, persistence responsibility, or review-record
field to make ingestion audit convenient. Where an ingestion audit
record might ever need to relate to a Memory Core `Document` or
`Provenance` identity, that relationship — if any — is Amendment 3's own
future decision; this document requires no such relationship to be
coherent, because every audit fact this document fixes (Section 10-13)
is expressible entirely through `EvidenceArtifactId`/`DerivativeGenerationId`
references already governed by Evidence Custodian and Unit 2. If
ingestion audit ever needs an opaque reference to an already-governed
Memory Core identity, that need is recorded here only as a possibility,
never as a requirement, and never with an assumed field shape.

## 23. Required / conditional / optional / forbidden classification

No Kotlin type is frozen. This classifies the audit facts
`DOCUMENT_INGESTION_ROUTING_AND_COMPLETENESS_POLICY.md` §5 already
names, disciplined by Section 13's reference-versus-duplication rule.

**A. Required** (every conforming audit record must capture, directly or
by reference):

- attempt/correlation value (Section 11);
- audit-recording time (Section 12);
- source `EvidenceArtifactId` reference (never duplicated content);
- requesting/submitting principal reference;
- operational outcome (the attempt's own typed result, e.g. admitted /
  failed / blocked — vocabulary already adopted, Routing Policy §3, not
  redefined here).

**B. Conditional** (required exactly when applicable to the specific
attempt; explicitly absent, never defaulted, otherwise):

- `DerivativeGenerationId` reference — present only for an attempt that
  reached admission;
- authorisation-decision reference — present only where a Permission
  Engine (or equivalent) decision was actually evaluated for this
  attempt;
- resource-limit/block event reference — present only when routing
  policy actually blocked or limited the attempt (Routing Policy §6).

**C. Optional/extensible** (useful, non-authority-defining):

- a human-readable summary label, provided it asserts no additional
  authority and is never treated as an identity.

**D. Forbidden** (would create authority leakage or duplication; no
implementation may include these):

- duplicated content from the source manifest, the Derivative Generation
  Record, or the Derivative Review Record where a reference already
  suffices (Section 13);
- any field encoding evidential-state classification, OCR judgment, or
  Evidence Intelligence conclusion (Section 21);
- any field allowing the audit record itself to be presented as, or
  substituted for, the authorisation it documents (Section 8's final
  bullet);
- a minted domain identity for the audit record itself (Section 11);
- any Memory Core field or reference presupposing Amendment 3's
  undecided shape (Section 22).

## 24. Failure/non-existence semantics

- **An audit record for a given correlation value does not exist:**
  means only that no audit fact has been recorded for that value —
  mirroring Unit 3 §12.3's "null means only 'no review recorded'"
  discipline, extended here: absence is never proof that the underlying
  ingestion attempt did or did not occur; the coordinator's own
  Section 14 obligation is what prevents an attempt from having
  observable governed consequences without a corresponding record, not
  an inference drawn from the record's absence.
- **A referenced `EvidenceArtifactId`/`DerivativeGenerationId` cannot be
  resolved:** outside the audit port's own concern, exactly as
  Unit 3 §12.4 already establishes for the review registry — the audit
  port does not manufacture, infer, repair, or resolve identities.
- **Malformed/blank correlation value:** prevented by construction,
  mirroring `EvidenceDeletionAuditRecord`'s own `require(deletionRequestId.isNotBlank())`.

## 25. Downstream consumers

| Consumer | Classification | Reasoning |
| --- | --- | --- |
| A future ingestion audit port/interface | requires later implementation-plan unit | not designed here, per this unit's own instruction |
| The Parker-owned ingestion coordinator's own audit call sites | requires later implementation-plan unit | sequencing frozen (Section 9), mechanism deferred |
| `AuditService` | untouched, not implemented by this document | Section 6; remains unimplemented, undecided whether ever subsumes this port |
| `EvidenceDeletionAudit`/`FileSystemEvidenceDeletionAudit` | untouched | Section 4; not extended, not modified |
| Evidence Custodian | unaffected | Section 18 |
| Derivative Generation Record (Unit 2) | unaffected, strengthened | Section 14, 19 |
| Derivative Review (Unit 3) | unaffected | Section 20 |
| OCR/Evidence Intelligence (Unit 4) | unaffected | Section 21 |
| Memory Core/Knowledge | deferred to Amendment 3 | Section 22 |
| A future human auditor/reviewer | narrow, direct log inspection only, mirroring precedent | Section 16 |

## 26. Explicit deferrals

- Retrieval/query capability for ingestion audit records (Section 16).
- Retention and deletion policy/authority for ingestion audit records
  (Section 17).
- Any relationship between ingestion audit and Memory Core/Knowledge
  identities (Section 22) — explicitly reserved for Alignment Amendment
  3, not pre-shaped here.
- Whether ingestion audit is ever subsumed into a future, platform-wide
  `AuditService` (Section 4, 16, mirroring the identical deferral the
  deletion-audit precedent already makes).
- The exact stage/event taxonomy, record field types, correlation-value
  representation, and persistence technology (Section 9, 11, 15) —
  implementation-plan work.

## 27. Non-goals

Explicitly out of scope for this document:

- no Kotlin implementation of any kind — no interface, no record type,
  no port, no exception hierarchy;
- no persistence technology selection;
- no dependency addition;
- no implementation of `AuditService`;
- no modification of `EvidenceDeletionAudit`, `FileSystemEvidenceDeletionAudit`,
  or `DefaultOwnerEvidenceDeletionAuthority`;
- no Memory Core, Knowledge, RKS/QMD, or Reasoning Context change;
- no Alignment Amendment 3 work of any kind;
- no `EvidenceArtifact`, `AcceptedEvidenceArtifact`, Derivative
  Generation Record, or Derivative Review Target redesign — Units 1-4
  remain unmodified;
- no retention/deletion policy invention (Section 17);
- no query/retrieval capability grant (Section 16);
- no source or derivative mutation of any kind;
- no real-evidence ingestion;
- any Runtime Integration or implementation-plan work of any kind.

## 28. Conflicts discovered

**None.** Every rule in this document is a direct, unmodified
restatement of the constitutional Auditability principle, ADR-024, the
Evidence Custodian Phase 7 Boundary Clarification and its implemented
`EvidenceDeletionAudit`/`FileSystemEvidenceDeletionAudit`/`DefaultOwnerEvidenceDeletionAuthority`
precedent (all freshly re-read and test-confirmed in Section 3-4 above),
or a narrow, reference-only extension of Unit 1's already-adopted
Routing Policy §5/§8 Owner Decision 4 and Units 2-4's own already-adopted
invariants. No genuine contradiction in already-adopted governance was
found; no missing rule prevented this unit from being coherently
scope-locked; no existing implementation shape conflicts with adopted
governance. The one substantive decision this document makes beyond
direct restatement — the reference-versus-duplication discipline
(Section 13) — narrows *how* Routing Policy §5's already-adopted fact
list is satisfied without contradicting, removing, or adding to that
list, and is required precisely because §5's own broad fact list, taken
without this discipline, would risk exactly the "second canonical
database" outcome this unit's own instructions warn against.

## 29. Constitutional self-certification / authority traceability

| Authority | Check | Result |
| --- | --- | --- |
| Parker Constitution | Auditability: every authorized action leaves a record | Section 5, quoted verbatim; discharged via a new narrow port, not a new constitutional principle |
| Epistemic Integrity | No fabrication; unknown stated as unknown | Section 15: no fabricated/defaulted audit facts; absence is explicit (Section 24) |
| CDR-006 | Original evidence custody/immutability frozen | Not reopened; Section 18 confirms no Evidence Custodian interface change |
| CDR-007 | OCR/EI assigned to Evidence Intelligence | Not touched; Section 21 restates Unit 4's own boundary verbatim |
| Evidence Custodian Scope Lock §7 | "Audited" deletion obligation | Precedent only (Section 4); not extended to ingestion, a new port is used instead |
| ADR-024 | No `AuditService` implementation exists; modules never write directly to platform state | Section 4, 6; this document does not implement `AuditService`; only the coordinator (never a plugin) calls the audit port (Section 7, 9) |
| Document Ingestion Units 1-4 (`84cc061`/`4faaeb8`/`1958730`/`ff589be`) | Governing authority for this programme | Every section cites and restates, never contradicts, already-adopted rules |
| Memory Core, Knowledge, RKS/QMD, Reasoning Context | Untouched | Section 22, 27 |
| Alignment Amendment 3 | Not begun, not pre-shaped | Section 22, 26 |

## Final Recommendation

**READY FOR OWNER REVIEW** (scope-lock stage).
