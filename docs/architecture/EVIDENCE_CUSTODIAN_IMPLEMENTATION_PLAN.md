# Evidence Custodian — Implementation Plan

## Status

Programme: **Evidence Custodian — Implementation Plan, Phase 1.**
Phase: **Final design document before production coding begins.** No
Kotlin is implemented, proposed as a diff, or changed by this document.
No API, database schema, hashing algorithm, encryption choice, or
storage technology is specified. Neither `src/` nor `tests/` is touched.
Nothing is staged, committed, or pushed.

**Normative inputs, frozen, not redefined:** `docs/architecture/parker-constitution.md`;
`docs/architecture/epistemic-integrity.md` (Article IX, as amended);
`docs/decisions/CDR-006_CONSTITUTIONAL_CLASSIFICATION_OF_ORIGINAL_EVIDENCE_CUSTODY_AND_IMMUTABILITY.md`;
`docs/architecture/EVIDENCE_ARTIFACT_CONTRACT_DESIGN.md`; and
`docs/architecture/EVIDENCE_CUSTODIAN_SCOPE_LOCK.md`. This document
describes **how** the already-approved Evidence Custodian will
eventually be built — sequencing, dependencies, verification, and risk.
It introduces no responsibility, no exclusion, no subsystem boundary, no
permission rule, and no lifecycle concept beyond what those five
documents already froze. It selects no storage technology, no API
shape, no schema, no hashing algorithm, and no Kotlin design. Where this
plan states a phase name or ordering choice, that choice is disclosed as
a planning-level convenience, never as a new architectural decision.

---

## 1. Purpose

This plan exists to translate the frozen Evidence Custodian Scope Lock
into a staged engineering roadmap, so that a future implementer has a
disclosed build order, a disclosed dependency map, and a disclosed
verification strategy before any code is written — without that roadmap
itself making a single new constitutional decision. Every phase below is
scoped so that, at its completion, the Custodian's behaviour up to that
point remains fully explainable by reference to the Scope Lock's own
Sections 3–8: no phase may be completed by relying on a capability the
Scope Lock excludes, and no phase may leave a constitutional guarantee
partially enforced without disclosing that fact as an open risk (Section
9, below). The constitutional boundaries already approved — custody
without ownership, immutability while retained, the Custodian/Evidence
Intelligence separation, mandatory traceability, Permission Engine
gating, and owner-authorised deletion — are treated as fixed points this
plan sequences work around, never as questions this plan reopens.

---

## 2. Implementation Governance Rules

Implementation shall remain continuously traceable to the approved
constitutional governance. Every implementation phase (Section 4, below)
must remain fully consistent with `docs/architecture/parker-constitution.md`;
`docs/architecture/epistemic-integrity.md` (Article IX, as amended);
`docs/decisions/CDR-006_CONSTITUTIONAL_CLASSIFICATION_OF_ORIGINAL_EVIDENCE_CUSTODY_AND_IMMUTABILITY.md`;
`docs/architecture/EVIDENCE_ARTIFACT_CONTRACT_DESIGN.md`; and
`docs/architecture/EVIDENCE_CUSTODIAN_SCOPE_LOCK.md` — at every stage of
implementation, not only at this plan's own drafting.

Implementation may select among already-authorised implementation
options. It may not:

- expand subsystem responsibilities beyond what the Scope Lock already
  fixes;
- reinterpret any constitutional boundary already settled by the five
  documents above;
- introduce new authority for the Custodian, for Evidence Intelligence,
  or for any other subsystem;
- weaken Permission Engine controls;
- weaken immutability guarantees;
- weaken provenance requirements;
- weaken the Constitutional Optimisation Safeguard.

If implementation reveals an ambiguity, a missing constitutional rule,
conflicting governance, or a requirement that cannot be satisfied without
expanding authority, engineering work must stop and governance must
resume — through the same escalation this plan's own Section 6 (Out of
Scope) and the Scope Lock's Section 10 (Change Control) already
establish — before implementation continues. Implementation convenience
is never sufficient reason to bypass constitutional governance.

Every implementation phase must complete its own constitutional
verification, per Section 8's own Verification Strategy, before the next
phase begins. A phase is not complete merely because its behaviour
appears to work; it is complete only once verified against the specific
governance obligations Section 9's Traceability table assigns to it.

---

## 3. Implementation Objectives

- **Immutable custody.** A preserved original's content, once accepted,
  is never modifiable, overwritable, replaceable, or obscurable while
  retained (Scope Lock §8; Article IX, as amended).
- **Governed acceptance.** An artefact enters custody only following
  Permission Engine authorisation, never implicitly or as a side effect
  (Scope Lock §3, §7).
- **Authorised retrieval.** Read access to a custodied artefact is
  itself a gated proposal, observational only, and confers no write,
  mutation, or replacement capability (Scope Lock §7).
- **Provenance integration.** Custody-side facts remain consistent with
  whatever Memory Core's Provenance contract was told at registration,
  without the Custodian ever writing a Provenance record itself (Scope
  Lock §3, §5).
- **Derivative separation.** Every derivative artefact carries its own,
  separate identity, linked to its original only through Memory Core's
  existing Provenance mechanism, never merged or substituted (Scope Lock
  §6).
- **Owner-authorised deletion.** The sole path out of custody is an
  explicit, authorised, audited deletion request, gated exactly as
  Memory Core's own `DELETED` precedent (Scope Lock §7, §8).
- **Optimisation safeguard enforcement.** No efficiency, storage, or
  processing rationale — the Custodian's own or any other subsystem's —
  may ever justify destroying, replacing, or losing a preserved original
  (Scope Lock §4, §8; Article IX, as amended).

---

## 4. Major Implementation Phases

These are planning stages only — sequencing and dependency information,
not a Kotlin design. No phase below names a class, interface, file, or
storage mechanism.

1. **Foundational custody model.** Establish the conceptual shape of
   "an artefact in custody" and its stable identity (Scope Lock §6),
   sufficient to support every later phase, without yet choosing how
   that shape is persisted.
2. **Immutable storage behaviour.** Establish the behavioural guarantee
   that, once an artefact is accepted, its content cannot be altered
   through any code path this phase introduces — verified behaviourally
   (Section 8), not by a chosen storage engine's own claims.
3. **Governed acceptance path.** Wire artefact acceptance so that it
   only proceeds following Permission Engine authorisation, producing
   the artefact's stable reference and its initial custody-side
   integrity facts.
4. **Retrieval interface behaviour.** Establish authorised, observational
   read access — gated the same way acceptance is — confirming no read
   path can be used to write, mutate, or replace a preserved original.
5. **Provenance integration.** Establish the mechanism by which
   custody-side facts remain available and consistent for whatever
   Memory Core's Provenance contract records at registration, without
   the Custodian acquiring any write path into Memory Core.
6. **Derivative relationship support.** Establish support for a
   derivative artefact's own, separate identity and its traceable link
   back to its original, entirely through the existing Provenance
   mechanism — introducing no parallel traceability structure.
7. **Deletion workflow.** Establish the explicit, authorised, audited
   path by which a custodied original's custody ends, and confirm no
   other path exists.
8. **Optimisation safeguard enforcement.** Establish the structural
   refusal behaviour that rejects any request — from any subsystem,
   including the Custodian's own future maintenance tooling — to
   destroy, replace, or discard a preserved original in favour of a
   derivative.
9. **Verification.** Confirm every objective in Section 3 behaviourally,
   against the Scope Lock's own Sections 3–8, before proceeding to
   runtime integration (Section 4, item 10, below).
10. **Runtime integration.** Wire the Custodian into Parker's runtime
    exactly as an already-authorised, already-gated subsystem — adding
    no new authority, no new bypass, and no new relationship beyond
    those the Scope Lock's Section 5 already fixes.

Phases are sequenced so that no later phase's verification depends on an
earlier phase's guarantee being merely assumed; each phase's completion
is itself a precondition the next phase's own verification checks for
(Section 8).

---

## 5. Dependencies

- **Memory Core.** The Custodian depends on Memory Core's existing
  Document and Provenance contracts continuing to function exactly as
  already frozen — a stable location-reference field to populate, and a
  stable Provenance record to keep consistent with. This plan does not
  redefine either contract; it only consumes them, exactly as Scope Lock
  §5 fixes.
- **Provenance.** The Custodian depends on the existing
  `derivedFromReferences`/`extractedFromReference` mechanism as the sole
  authorised path for expressing a derivative's link to its original. No
  new provenance mechanism is introduced.
- **Permission Engine.** The Custodian depends on the Permission Engine
  as the sole authority for acceptance, deletion, and analytical-access
  proposals. This plan does not redefine `PermissionAction`, policy
  evaluation, or any other Permission Engine contract — it only issues
  proposals to it, exactly as every other gated subsystem already does.
- **Runtime.** The Custodian depends on Runtime to execute only what the
  Permission Engine has authorised, and to carry no independent custody
  authority of its own — an existing guarantee this plan relies upon,
  not one it re-establishes.

No dependency on Evidence Intelligence, Knowledge Memory, or the World
Model exists or is introduced (Scope Lock §5).

---

## 6. Out of Scope

Excluded from this Implementation Plan and from the implementation it
sequences, exactly as the Scope Lock already fixes:

OCR; transcription; reasoning; Evidence Intelligence (as a capability
the Custodian performs or absorbs); document analysis; legal-ownership
determination; authenticity assessment; truth determination; Knowledge
promotion; World Model integration; any user interface; storage
technology selection; database selection; API design; encryption-scheme
selection; hashing-algorithm selection.

No phase in Section 4 may be read as authorising any of the above merely
because it would be convenient during implementation. Any perceived need
for one of these capabilities during actual engineering work is a signal
to stop and escalate to governance (Section 2, Section 10, Scope Lock),
not to proceed.

---

## 7. Verification Strategy

Verification throughout this Programme is **behavioural, not
code-structural** — it asks what the Custodian does when exercised, not
what its internal design looks like:

- **Immutability verification.** Confirm that no exercised code path,
  under any input, can alter a previously accepted artefact's content —
  checked by attempting every modification, overwrite, replacement, and
  obscuring action the Scope Lock names (§4, §8) and confirming each is
  refused.
- **Acceptance-gating verification.** Confirm that acceptance never
  occurs without a prior, successful Permission Engine authorisation,
  and that no code path reaches acceptance by any other route.
- **Read-access verification.** Confirm that every read path is itself
  gated, and that no read path can be used, directly or indirectly, to
  write, mutate, or replace a preserved original (Scope Lock §7).
- **Provenance-consistency verification.** Confirm that custody-side
  facts available for Provenance never drift from what was recorded at
  registration, and that the Custodian never writes a Provenance record
  itself.
- **Derivative-identity verification.** Confirm that a derivative
  artefact's identity is never observably indistinguishable from, or
  substitutable for, its original's identity.
- **Deletion-path verification.** Confirm deletion occurs only through
  the explicit, authorised, audited path, and that no other operation —
  including one framed as "cleanup," "optimisation," or "maintenance" —
  produces the same effect.
- **Optimisation-safeguard verification.** Confirm that no request citing
  storage efficiency, derivative quality, or processing convenience is
  ever honoured as grounds for discarding a preserved original.
- **Boundary verification.** Confirm that Evidence Intelligence, a
  reasoning provider, or any other consumer never acquires custody,
  modification, or deletion authority merely by being granted
  authorised read access.

No verification step above depends on, or is satisfied by, a specific
storage technology, API, or Kotlin design — each is stated as a
behaviour a future test suite must demonstrate, regardless of how the
Custodian is eventually implemented.

---

## 8. Traceability

| Implementation phase (§4) | Constitution | Article IX | CDR-006 | Contract Design | Scope Lock |
| --- | --- | --- | --- | --- | --- |
| 1. Foundational custody model | Owner control; capability/authority separation | — | Custody vs. ownership | §4 (custody responsibilities); §7 (lifecycle) | §2, §6 |
| 2. Immutable storage behaviour | "If a safeguard cannot be pointed to in the architecture, it does not count as a guarantee" | Absolute custody-preservation paragraph | Immutability while retained | §4, §8 | §3, §8 |
| 3. Governed acceptance path | No-bypass; cognition/trust/runtime separation | — | Permission Engine gating (implicit via Constitution) | §4, §6.6 | §3, §7 |
| 4. Retrieval interface behaviour | No-bypass | — | — | §6.6 | §7 |
| 5. Provenance integration | — | Article VIII (by reference) | Custody ≠ Provenance ownership | §4, §6.2 | §3, §5 |
| 6. Derivative relationship support | — | "shall not represent derivative evidence as though it were original" | Separate-identity requirement | §4, §6.2 | §6 |
| 7. Deletion workflow | Owner control; "own their data" | Owner-authorised-deletion carve-out | Deletion as instance-control right, not legal-ownership determination | §4, §6.6, §8 | §3, §7, §8 |
| 8. Optimisation safeguard enforcement | — | Constitutional Optimisation Safeguard paragraph | Constitutional Optimisation Safeguard rule | §4, §8 | §4, §8 |
| 9. Verification | Auditability; Constitutional Tests precedent | — | Verification Criteria | §11 | §11 |
| 10. Runtime integration | "Cognition proposes, trust authorises, runtime executes" | — | — | §6.6 | §5, §7 |

---

## 9. Risks

- **Silent immutability erosion.** A future implementer, under
  performance or storage pressure, introduces a "fast path" that
  bypasses the immutability check for convenience — directly violating
  Article IX and Scope Lock §8. Mitigation: Section 7's immutability
  verification must be exercised against every code path, not a sampled
  subset.
- **Custody/Evidence-Intelligence boundary drift.** A future
  implementation, seeking efficiency, grants Evidence Intelligence a
  direct storage-level access path "just this once" — reintroducing
  exactly the conflation Contract Design §6.3 and Scope Lock §5 were
  written to prevent. Mitigation: Section 7's boundary verification, and
  Section 6's explicit escalation instruction.
- **Deletion path proliferation.** A future implementer adds a second,
  "administrative" deletion mechanism (for example, a maintenance script)
  outside the owner-authorised, audited path — violating Scope Lock §7,
  §8. Mitigation: Section 7's deletion-path verification must confirm
  exactly one path exists.
- **Optimisation-safeguard rationalisation.** A future implementer treats
  "the derivative is good enough" as a legitimate reason to discard an
  original under storage pressure — precisely the failure mode the
  Constitutional Optimisation Safeguard exists to foreclose. Mitigation:
  Section 7's optimisation-safeguard verification, framed as a required,
  not optional, test.
- **Provenance drift.** Custody-side facts (existence, integrity,
  availability) silently diverge from what Memory Core's Provenance
  record states, undermining Article VIII without any single visible
  failure. Mitigation: Section 7's provenance-consistency verification.
- **Scope creep via "reasonable convenience."** Any phase in Section 4
  is implemented with an added capability from Section 6's exclusion
  list, justified as incidental. Mitigation: Section 2's and Section 6's
  explicit instruction to escalate rather than proceed.

---

## 10. Success Criteria

Implementation of the Evidence Custodian may be considered complete only
when all of the following are objectively true:

1. Every objective in Section 3 is demonstrated by a passing behavioural
   verification from Section 7 — not asserted, demonstrated.
2. No code path exists capable of modifying, overwriting, replacing, or
   obscuring a preserved original while retained, under any input.
3. No code path exists by which acceptance, deletion, or analytical
   access occurs without prior Permission Engine authorisation.
4. No code path exists by which Evidence Intelligence, a reasoning
   provider, or any other consumer acquires custody, modification, or
   deletion authority through read access alone.
5. Exactly one deletion path exists, and it is owner-authorised and
   audited.
6. No code path exists capable of destroying, replacing, or discarding a
   preserved original on efficiency, storage, or convenience grounds.
7. Every derivative artefact is demonstrably, observably distinct in
   identity from its original, and traceable to it only through Memory
   Core's existing Provenance mechanism.
8. No excluded capability from Section 6 has been introduced, directly
   or indirectly.
9. No new constitutional decision has been made in the course of
   implementation without a corresponding governance record (Section 2;
   Scope Lock §10, Change Control).

---

## Final Recommendation

This Implementation Plan is ready to be presented for review. If
approved, it authorises engineering work to begin sequencing Phase 1
(Foundational custody model) — it does not itself constitute that work,
select any technology, or write any Kotlin.

EVIDENCE CUSTODIAN IMPLEMENTATION PLAN — WRITTEN TO REPOSITORY — PENDING
CONSTITUTIONAL REVIEW

Confirmed: no Kotlin implemented; no API, schema, storage technology,
hashing algorithm, or encryption choice defined; Constitution, Article
IX, CDR-006, the Evidence Artifact Contract Design, and the Evidence
Custodian Scope Lock all unmodified; nothing staged; nothing committed;
nothing pushed; no production code touched.
