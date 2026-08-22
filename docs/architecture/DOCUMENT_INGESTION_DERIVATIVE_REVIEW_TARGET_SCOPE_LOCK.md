# Document Ingestion — Derivative Review Target Scope Lock

## Status

**Draft for owner review. Not yet accepted, canonical, or
implementation-authorising.** Programme: **Document Ingestion — Governance
Alignment Unit 3**, scope-locking Alignment Amendment 2 only (Derivative
Review Target broadening), as identified by
`DOCUMENT_INGESTION_CANONICAL_GOVERNANCE_ALIGNMENT.md` §2 (adopted
`84cc061`) and by `DOCUMENT_INGESTION_DERIVATIVE_GENERATION_RECORD_SCOPE_LOCK.md`
§23 (adopted `4faaeb8`). No Kotlin is implemented, proposed as a diff, or
changed by this document. No dependency is added. No interface is
implemented. No persistence technology is chosen. No parser is installed.
No evidence is ingested. Alignment Amendments 3, 4, and 5 remain out of
scope and are not begun here.

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
the RKS/QMD Contract Design/Scope Lock/Amendments, ADR-024, and the six
documents already adopted at `84cc061`/`4faaeb8`. It amends none of those
six either — it narrowly widens what `DerivativeReviewRecord`/
`DerivativeReviewRegistry` may name as a target, exactly as anticipated
and deferred by the Derivative Generation Record Scope Lock's own §23
("Downstream-reference boundary").

## 1. Executive Summary

The existing `DerivativeReviewRecord`/`DerivativeReviewRegistry` human
review mechanism (Evidence Processing Searchable-PDF, Implementation Unit
3) is hard-keyed to `EvidenceArtifactId`. Document Ingestion's own
Derivative Generation Record (Unit 2, adopted `4faaeb8`) introduced a
second, non-byte governed identity, `DerivativeGenerationId`, with no
path into the same review mechanism. This scope lock freezes the minimum
governed abstraction — a closed, two-case **review target** — that lets a
review record name either kind of identity, and freezes every semantic
consequence of doing so, while changing nothing about how review already
works for the existing `EvidenceArtifactId` case.

## 2. Frozen Objectives

1. A review target names exactly one of `EvidenceArtifactId` or
   `DerivativeGenerationId` — never both, never neither, never an open
   string, never a new third identity.
2. Existing `EvidenceArtifactId`-targeted review behaviour is unchanged
   in every particular: the same four-value `DerivativeReviewState`, the
   same transition graph, the same append-only history, the same
   `APPROVED`-only gating rule, the same absence of Evidence Custodian or
   Memory Core dependency.
3. Human review of any target never confers evidential authority, never
   mutates or replaces a source, never converts a derivative into an
   `EvidenceArtifact`, and never grants Evidence Custodian, Memory Core,
   OCR, Evidence Intelligence, or RKS/QMD authority.
4. Reviewing one target never implicitly reviews, approves, or affects
   any other target — including a reconciliation generation and the
   parent generations it names under Unit 2's multi-parent same-root
   rule.
5. No new authority is created. Only the Parker-owned ingestion
   coordinator may record the initial `PENDING_REVIEW` transition for any
   target, exactly as only it does today for the byte-backed case.

## 3. Canonical authorities and current implementation inspected (fresh, this unit)

Read fresh for this scope lock, not from prior-session summary:
`src/interfaces/DerivativeReview.kt` (full file); `tests/runtime/InMemoryDerivativeReviewRegistryTest.kt`
(full file — the only source of ground truth for the registry's actual
transition-graph and history behaviour); `EVIDENCE_PROCESSING_SEARCHABLE_PDF_BOUNDARY_CLARIFICATION.md`
§6 ("Human Review Lifecycle") and §7 (coordinator sequencing, step 8) in
full. Held in context, already fresh within this session and re-verified
for consistency against the above: the six documents adopted at
`84cc061`/`4faaeb8`; `EvidenceCustodian.kt`; CDR-006; CDR-007; ADR-024;
`MEMORY_CORE_CONTRACT_DESIGN.md` §7; OCR Mechanism governance; RKS/QMD
governance.

## 4. Existing `DerivativeReview` semantics (Step 3 findings)

Established by direct inspection of the interface, its test suite, and
the Boundary Clarification:

**A. Who may create a record.** The interface itself enforces no caller
identity — `recordReviewState` takes no `PrincipalId` parameter of its
own; `reviewingPrincipalId` is a field *of the record being recorded*,
not an enforced credential. Authorization is structural, not
type-enforced: only the component holding a reference to the registry
implementation can call it. Per the Boundary Clarification's own
coordinator sequencing (§7, step 8), only the Parker-owned extraction/
ingestion coordinator currently calls `recordReviewState`, and only to
record the initial `PENDING_REVIEW` — "the coordinator... records
`PENDING_REVIEW` for every newly registered derivative, immediately, as
part of the same successful pipeline run." No human-reviewer-driven
caller exists yet; canon explicitly defers it ("Runtime Integration
remains deferred") and discloses, without wiring, an expectation that a
future Scope Lock gates at least the `APPROVED` transition through
Permission Engine (Boundary Clarification §6, "Permission Engine
expectation, disclosed not decided").

**B. What a review record means.** One flat, timestamped fact about the
human-review lifecycle *state* of one target's own extracted/derived
content — never a fact about the source, never a fact about custody.

**C. What `APPROVED` means.** Exactly and only: "no future consumer...
may treat a derivative's extracted text as human-verified unless
`DerivativeReviewRegistry.currentReviewState`... returns `APPROVED`.
Absent that check, or for any state other than `APPROVED`, the text must
be treated as unverified machine-extracted output only" (Boundary
Clarification §6, verbatim). A narrow, binary human-verification signal
for downstream consumers — nothing more.

**D. Whether approval confers evidential authority.** No. Nowhere in the
interface, its KDoc, or the Boundary Clarification does `APPROVED` alter
what a derivative *is* — only what a downstream consumer may *treat it
as* for its own purposes (human-verified vs. unverified machine output).

**E. Whether approval mutates an `EvidenceArtifact`.** No. The registry
"holds no structural dependency on `EvidenceCustodian`," and
`recordReviewState` returns nothing on success and touches only the
registry's own storage.

**F. Review identity representation.** No separate "review record
identity" type exists; a record is not itself independently addressable
by ID — records are read back only as "the current state for target X,"
resolved as the most recently appended record for that target.

**G. Review target representation (current).** A bare `EvidenceArtifactId`
— the sole parameter to `currentReviewState` and the sole target field on
`DerivativeReviewRecord`.

**H. Registry uniqueness/key semantics.** Keyed by target identity.
Recording is append-only — "every call adds a new record; nothing
already recorded is ever overwritten or removed." "The current state for
an identifier is whichever record was most recently appended for it."
Confirmed directly by the test suite: `` `distinct identifiers maintain
fully independent review histories` ``; `` `current state always resolves
to the most recently appended record` ``.

**I. Immutability.** Every individual record, once appended, is
permanent — never edited, never removed. History for a given target
grows; it is never rewritten.

**J. Multiple reviews of one target.** Yes, constrained by a fixed
transition graph, confirmed directly by the test suite: `PENDING_REVIEW`
is the mandatory first transition (skipping it throws); from
`PENDING_REVIEW`, exactly `APPROVED`, `REJECTED`, or `NEEDS_CORRECTION`
are valid; `APPROVED`, `REJECTED`, and `NEEDS_CORRECTION` are **all**
terminal for that identifier — "every transition away from either
throws" (tested for `APPROVED`/`REJECTED` against all four states;
`NEEDS_CORRECTION` → `PENDING_REVIEW` on the same identifier is
separately tested and throws). "Correcting" a derivative is never a
transition on the existing identifier — it is always a new extraction
producing a new, separately identified derivative, registered
independently with its own fresh `PENDING_REVIEW` (Boundary
Clarification §6).

**K. Whether review is required for every derivative or only particular
classes.** The coordinator records `PENDING_REVIEW` for "every newly
registered derivative" in the one implemented pipeline (searchable-PDF
literal extraction) — universal for that class, by that coordinator's own
existing sequencing. This scope lock does not decide whether every future
derivative class must enter review; that remains a routing/policy
question outside this document's scope (`DOCUMENT_INGESTION_ROUTING_AND_COMPLETENESS_POLICY.md`
is silent on it and this document does not amend that silence).

**L. Downstream consumers.** Named in canon: "Evidence Intelligence once
resumed, or any other downstream capability" — as of this reading, no
consumer is actually wired to enforce the gating rule; the rule is fixed
now, "before any consumer exists to apply it... it implements no
enforcement mechanism of its own." See Section 12 for the full downstream
classification this unit requires.

## 5. Review-target abstraction (Step 4)

**Semantic requirement, not a Kotlin declaration.** The review target
must be a **closed, two-case union**: exactly `EvidenceArtifactId` or
`DerivativeGenerationId`, never an open string, never a third case,
never a bare identity value compared across kinds. This follows the same
closed-enumeration discipline this codebase already applies without
exception to `DerivativeReviewState` itself, and to every sealed outcome
type touching evidence (`EvidenceAcceptanceResult`, `EvidenceRetrievalResult`,
`EvidenceDeletionResult`) — a closed sum type is the established Parker
convention for exactly this shape of problem, making it, per this
document's own instruction, an unavoidable semantic requirement even
though the literal Kotlin declaration (sealed class, sealed interface, or
otherwise) remains implementation-plan work, not fixed here.

The target:

- **preserves the concrete target kind** — which case is present is
  itself part of the target's identity, not merely a tag on a shared
  string;
- **preserves the opaque target identity** — each case wraps the
  existing, unmodified `EvidenceArtifactId` or `DerivativeGenerationId`
  value unchanged; neither identity type is altered, extended, or
  reinterpreted by being reviewable;
- **prevents ambiguity between the two identity kinds structurally, not
  by convention** — a target naming `EvidenceArtifactId("x")` and a
  target naming `DerivativeGenerationId("x")` are never equal and never
  collide in the registry's own keying (Section 8), even though the two
  underlying string values happen to be identical text, because kind is
  part of identity for this purpose;
- **parses no semantic meaning** from either wrapped identity — exactly
  as today;
- **creates no new evidence identity** — the union only names one of the
  two identities that already exist; it mints nothing;
- **does not duplicate** the underlying `EvidenceArtifact` or Derivative
  Generation Record — the target is a reference, never a copy;
- **confers no authority merely by being reviewable** — constructing a
  target value is not itself an authorized act, exactly as constructing
  an `EvidenceArtifactId` value today confers no retrieval or acceptance
  authority by itself.

## 6. `EvidenceArtifactId` review semantics (Step 3/Frozen Objective 2 restated)

Unchanged in every particular from Section 4 above. This is the load-
bearing constraint of this entire scope lock: nothing about the existing
byte-backed review path may differ once the target type is widened. The
same `DerivativeReviewState` four values, the same transition graph, the
same append-only history, the same terminal-state set, the same
`APPROVED`-only gating rule, the same absence of a structural dependency
on `EvidenceCustodian` or `MemoryCore`, and the same currently-disclosed-
but-unwired Permission Engine expectation for the `APPROVED` transition,
apply identically and without modification.

## 7. `DerivativeGenerationId` review semantics

A `DerivativeGenerationId` target reuses, unmodified, the identical
`DerivativeReviewState` enumeration and transition graph already
governing the `EvidenceArtifactId` case (Section 4.J) — this document
does not define a second, parallel state machine for the new target
kind. Specifically:

- `PENDING_REVIEW` is the mandatory first transition for a
  `DerivativeGenerationId` target, exactly as for an `EvidenceArtifactId`
  target;
- `APPROVED`, `REJECTED`, and `NEEDS_CORRECTION` are equally terminal for
  a `DerivativeGenerationId` target;
- "correcting" a `DerivativeGenerationId` target is never a transition on
  the existing target — it is always a **new** Derivative Generation
  Record (Unit 2, Section 6: reprocessing always mints a distinct new
  `DerivativeGenerationId`), registered for review independently with its
  own fresh `PENDING_REVIEW`. This is not a new rule invented here — it is
  Unit 2's own generation-immutability rule and Section 4.J's own
  existing `NEEDS_CORRECTION` precedent, applied identically to the new
  target kind.

**`documentId` — deferred whole, not conditioned on Amendment 3's future
shape.** `DerivativeReviewRecord` today carries a mandatory
`documentId: DocumentId` (a Memory Core `Document` identifier) alongside
the target's own `evidenceArtifactId`. An earlier draft of this document
stated that `documentId` becomes populated for a `DerivativeGenerationId`
target "once a Memory Core cross-reference exists (Amendment 3)." On
re-review, that framing is corrected: it presupposed that Amendment 3 —
not yet begun, not yet governed — will take the specific form of
producing something assignable to this record's own `documentId` field.
Nothing adopted requires or implies that shape. `DOCUMENT_INGESTION_CANONICAL_GOVERNANCE_ALIGNMENT.md`
§3 (the only adopted description of Amendment 3's intended relationship)
describes Memory Core's *own* fields being populated by a future
ingestion coordinator (`sourceIdentifier`, `extractedFromReference`,
`derivedFromReferences`, one `processingHistory` entry) — it says nothing
about `DerivativeReviewRecord.documentId`, and nothing about a review
record gaining any Memory-Core-linking field at all. Presupposing that
connection here would be Unit 3 inventing a Memory Core relationship
Amendment 3 has not governed, contrary to this program's own intended
dependency direction (Unit 3 defines review-target semantics; Amendment 3
later, independently, defines Memory Core cross-reference semantics).

This document therefore fixes `documentId` as:

- **mandatory, unchanged**, for an `EvidenceArtifactId` target (Section
  6 — no weakening);
- **not part of the required, conditional, or optional shape** of a
  `DerivativeGenerationId` target's review record, full stop, under this
  document. Whether Amendment 3, once separately scope-locked, ever
  introduces some Memory-Core-linking field to a `DerivativeGenerationId`
  target's review record — its name, its shape, whether it is even called
  `documentId`, or whether Memory Core linkage for a reviewed generation
  is instead expressed some other way entirely (for example, purely
  through the Derivative Generation Record's own root-source reference,
  Unit 2 §21, with no review-record-level field at all) — is Amendment
  3's own decision to make, when made, not decided, presupposed, named,
  or reserved a slot for here. This is a whole deferral, not a
  conditional field.

## 8. Exact meaning of `APPROVED` for a `DerivativeGenerationId` target (Step 5 — critical)

`APPROVED` for a `DerivativeGenerationId` target means exactly: a human
reviewer has verified that **this specific generation's own derived
content** is human-verified, for downstream consumers' own purposes —
restating Section 4.C's existing rule, target-kind-neutral. `APPROVED`
does **not**, under any circumstance, for either target kind:

- convert the target into an `EvidenceArtifact`;
- make the derivative's bytes or text authoritative source evidence;
- replace, modify, or reinterpret the authoritative source artifact;
- alter source custody in any way;
- silently reconcile, average, or resolve disagreement between two
  Derivative Generation Records — reconciliation is a distinct, new
  generation (Unit 2 §16), never a side effect of reviewing an existing
  one;
- grant Evidence Custodian authority — the registry has, and retains, no
  structural dependency on `EvidenceCustodian`;
- grant Memory Core authority — reviewing has no write path into Memory
  Core; where `documentId` is present at all (the unchanged
  `EvidenceArtifactId` case, Section 7), it is, at most, a reference,
  never a write;
- grant Evidence Intelligence authority — `APPROVED` is a consumable
  signal *for* a future Evidence Intelligence caller, never an act *of*
  Evidence Intelligence, and grants it no new capability;
- grant OCR authority — reviewing an OCR-derived generation's output does
  not invoke, gate, or authorize OCR itself; OCR's own authorization
  boundary (CDR-007, OCR Mechanism governance) is untouched;
- grant RKS/QMD authority — the registry has no relationship to RKS/QMD
  of any kind, before or after this widening;
- grant reasoning authority — review is a lifecycle fact, never an act of
  evidential, legal, or credibility reasoning (Tier C, Plugin Contract
  §9.1, remains untouched and unentered).

## 9. Reconciliation / multi-parser case (Step 6)

Directly examined against Unit 2's same-root multi-parent rule (Unit 2
§7):

- **One parser generation, another parser generation from the same
  source, and a reconciliation generation naming both as parents** are
  **three separate targets**, each with its own independent review
  history, exactly as "distinct identifiers maintain fully independent
  review histories" already establishes for the existing byte-backed
  case (Section 4.H, test-confirmed). Nothing about a target's lineage
  (which parents it has, Unit 2 §7) is visible to, or alterable by, the
  review mechanism — `DerivativeReviewRecord` carries no lineage field.
- **Reviewing one generation never implicitly reviews another.**
  Approving the reconciliation generation does not, and cannot, mark
  either parent generation `APPROVED` — no code path exists, or is
  proposed, that would propagate a review-state transition from one
  target to another. This is Frozen Objective 4, restated concretely.
- **Approving a reconciliation generation does not erase or override its
  parent generations' existence.** Unit 2's own multi-parser-coexistence
  and generation-immutability rules (§16, §6) already guarantee both
  parents remain independently retrievable and immutable regardless of
  what happens to the reconciliation generation or its own review state;
  this document adds nothing that could weaken that guarantee, because
  review touches only the reviewed target's own state, never its
  parents'.
- **Cross-source synthesis is not permitted through the review
  mechanism.** Review cannot create, mint, or alter lineage of any kind
  — it operates entirely on an already-existing target's already-fixed
  identity. Since Unit 2 §7 already forbids a Derivative Generation
  Record from naming parents whose roots differ, and review cannot
  create or modify a generation's parent links, review supplies no path
  by which cross-source combination could occur, whether or not the
  reviewer intends it.

## 10. Review-target immutability (Step 7)

- **Target identity is immutable after record creation.** A `DerivativeReviewRecord`'s
  target field, once set at that record's own creation, is never mutated
  — inherited directly from the existing append-only/immutable-record
  discipline (Section 4.I), extended without modification to the widened
  target type.
- **Target kind is immutable.** The same reasoning applies identically:
  a record naming an `EvidenceArtifactId` target never becomes a record
  naming a `DerivativeGenerationId` target, or vice versa.
- **An existing review record may never be retargeted.** No mechanism,
  proposed or existing, permits changing which target a recorded review
  fact describes.
- **Reprocessing creates a new review target.** Confirmed for both kinds:
  the existing `EvidenceArtifactId` case already establishes this
  (Section 4.J); Unit 2's own generation-immutability rule establishes it
  identically for `DerivativeGenerationId` (a new generation always gets
  a new, distinct identity — Section 7, above).
- **Review history remains traceable when later generations exist.**
  Each target's own review history is permanently, independently
  retrievable via its own identity, regardless of whether a later
  generation superseding it exists. Lineage (which generation superseded
  which, Unit 2 §7) and review history (this document) are separate
  facts, cross-referenceable only because both name the same identity —
  this document does not build a joint traversal between them; a future
  consumer wanting "the full correction history" of a source must walk
  both structures independently.

## 11. Registry keying (Step 8)

Preserved exactly, extended uniformly to the widened target type — no
new semantics introduced:

- keyed by target identity (now the closed two-case union, Section 5),
  not by bare string, so an `EvidenceArtifactId` and a
  `DerivativeGenerationId` sharing the same underlying string value are
  never treated as the same key;
- append-only recording, unchanged;
- "current" state = the most recently appended record for that target,
  unchanged;
- distinct targets maintain fully independent histories, unchanged and
  now also true across kinds (an `EvidenceArtifactId` target's history
  and a `DerivativeGenerationId` target's history never interact, even if
  they happen to share an underlying string).

Explicitly **not** introduced by this widening: one-review-per-target
semantics (already false today — multiple records per target already
occur, constrained by the transition graph); latest-review-wins
semantics presented as new (this is simply the existing, preserved
"current = most recent" rule, not a new concept); mutable target indexes
(never — append-only throughout); approval-replacement semantics (never
— `APPROVED` remains terminal; a "replacement" is always a new target via
reprocessing, never an edit).

## 12. Failure and non-existence semantics (Step 9)

**Target-existence validation is not, and never becomes, the registry's
own responsibility.** `DerivativeReviewRegistry` today validates only
review-*state* transitions (Section 4.J); it has never validated, and
this document does not newly require it to validate, whether a target
identity actually names something that exists. That is, and remains, a
different Parker-owned boundary's responsibility, frozen precisely below
— without inventing a new port, per this document's own instruction.

**12.1 Who confirms existence before the initial `PENDING_REVIEW` is
admitted.** For an `EvidenceArtifactId` target, this is already
established, adopted governance, not new: the Boundary Clarification's
own coordinator sequencing (§7) places `EvidenceCustodian.retrieve` (step
3) and `EvidenceRegistrationCoordinator.register` (step 7) — both of
which fail closed on a non-existent or unverifiable artifact — strictly
*before* `recordReviewState(PENDING_REVIEW)` (step 8), in one fixed
pipeline run by one coordinator. Existence is therefore never assumed by
the review step; it is a precondition already satisfied by the two steps
immediately preceding it, in the same caller. For a `DerivativeGenerationId`
target, the identical structural discipline applies by direct analogy,
not by inventing a new validating port: Unit 2's own Parker-owned
ingestion coordinator is the sole authority permitted to admit a
Derivative Generation Record at all, and admission is itself atomic and
all-or-nothing (Unit 2 §19 — a record either satisfies every required
field and is admitted whole, or does not exist as a governed record).
Section 4.A of this document already establishes that only that same
coordinator may ever call `recordReviewState`. The existence-confirming
boundary for a `DerivativeGenerationId` target is therefore: **the same
Parker-owned ingestion coordinator's own admission act (Unit 2 §19),
which must complete, successfully and atomically, before that identical
coordinator's own subsequent call to record `PENDING_REVIEW` for the
identity it just admitted.** No new port, service, or interface is
introduced to answer this; the existing (byte-backed) and Unit-2-adopted
(generation) coordinator-sequencing disciplines are the answer, extended
by structural analogy. The mechanical composition of that sequencing for
the generation case is deferred, implementation-plan work (Section 16);
the semantic requirement — admission before review, same caller, fixed
order, no exception — is frozen now.

**12.2 A review record may never be admitted for an unresolved target.**
Frozen explicitly: no `PENDING_REVIEW` record, and no subsequent
transition, may ever be recorded for a target identity that the calling
coordinator has not itself already, successfully, and atomically
established to exist (Section 12.1). This is not a new restriction on
the registry — the registry still performs no existence check of its own
— it is a restriction on the *caller*, already true today for the
byte-backed case (no code path in the existing coordinator sequence calls
step 8 without steps 3 and 7 having already succeeded) and extended
identically to the generation case.

**12.3 The exact, bounded meaning of `currentReviewState(target)`
returning `null`.** `null` is, and must remain, a statement about the
review registry's own history *only*: "no review record has ever been
appended for this target identity." It is not, and must never be
interpreted as, evidence either that the target exists (an unreviewed but
real, already-admitted target correctly returns `null` until its first
`PENDING_REVIEW` is recorded — the two events are not simultaneous by
type, only by the coordinator's own fixed-order convention, Section 12.1)
or that the target does not exist (the registry has no way to know
either way, and does not claim to). Any future consumer treating `null`
as proof of non-existence, or as proof of existence, exceeds what this
value means and is a misuse this document does not authorize.

**12.4 The registry must not manufacture, infer, repair, or resolve
target identities.** Restated as a first-class, standalone rule, not
merely a description of current behaviour: no future implementation of
`DerivativeReviewRegistry` may construct, guess, default, look up
elsewhere, or otherwise produce a target identity it was not directly
given by its caller; it must not attempt to resolve a target against
`EvidenceCustodian`, a future Derivative Generation Record store, or any
other subsystem; and it must not silently correct, normalise, or repair
a malformed or unresolvable target rather than surfacing the caller's own
error. This grants the registry no new Evidence Custodian, Derivative
Generation Record persistence, or ingestion authority — it is a
prohibition, not a capability, and it forecloses exactly the shortcut a
future implementer might otherwise be tempted to take once the target
type is widened to two kinds.

- **Target kind and ID "disagree" (a caller constructs a target claiming
  the wrong kind for a given string):** not detectable, or intended to be
  detected, by the registry itself (Section 12.4) — the registry, as
  today, holds "no structural dependency" on `EvidenceCustodian` or any
  future Derivative Generation Record store, and therefore cannot and
  does not verify that a named identity actually exists in either. This
  division of responsibility (the calling coordinator supplies only real,
  already-established identities, per Section 12.1; the registry trusts
  its caller for existence) is unchanged from today's
  `EvidenceArtifactId`-only behaviour and is not altered by this
  widening.
- **A referenced `DerivativeGenerationId` or `EvidenceArtifactId` cannot
  be resolved** (against Evidence Custodian or a future generation
  store): outside this registry's own concern, exactly as today (Section
  12.4); this document does not add a resolution/verification
  responsibility to the registry that it does not already have.
- **A target existed but a downstream derivative was later generated:**
  the earlier target's review history remains exactly as recorded,
  permanently (Section 10); the later generation is a wholly new,
  independent target with its own history — no automatic linkage,
  invalidation, or cross-reference is created between the two by this
  document.
- **Malformed/blank target identity:** both `EvidenceArtifactId` and
  `DerivativeGenerationId` already enforce non-blank at construction
  (`require(value.isNotBlank())`, confirmed directly in
  `EvidenceCustodian.kt` and required by Unit 2 §4-5 for the new
  identity); a target wrapping either can never be blank by construction.
  No new validation is invented here.
- **Duplicate review submission where existing governance forbids it:**
  the existing transition-graph enforcement (Section 4.J) already rejects
  an invalid repeated/duplicate transition — for example, a second
  `APPROVED` submission for an already-`APPROVED` target throws, because
  `APPROVED` is terminal. This enforcement is validated purely on
  `DerivativeReviewState` values and the target's own prior history,
  independent of target kind, so it applies identically, unmodified, to
  both kinds.

## 13. Downstream boundary (Step 10)

| Consumer | Classification | Reasoning |
| --- | --- | --- |
| `DerivativeReviewState`, `DerivativeReviewRecord`, `DerivativeReviewRegistry` (the interface itself) | requires later implementation amendment | must widen the target field/parameter type per Section 5; not done by this scope lock |
| `InMemoryDerivativeReviewRegistry` (the one production implementation) | requires later implementation amendment | must widen its own key type; not done by this scope lock |
| `EvidenceExtractionCoordinator`'s existing `recordReviewState` call site (step 8) | requires later implementation amendment; unaffected in substance | mechanical wrapping of its existing `EvidenceArtifactId` into the new closed-union case; zero behavioural change for the byte-backed flow (Section 6) |
| A future Document Ingestion coordinator's own `recordReviewState` call site for `DerivativeGenerationId` targets | outside this unit | does not yet exist; its creation is implementation-plan work for a later unit, not this scope lock |
| Evidence Intelligence (future gating consumer of `currentReviewState`) | outside this unit | not yet wired to enforce the gating rule for either target kind (Section 4.L); this document changes nothing about when or how it will be |
| A future human-review UI / Runtime Integration consumer | outside this unit | explicitly deferred by canon already ("Runtime Integration remains deferred"); this document does not begin it |
| Memory Core, Evidence Custodian, OCR Mechanism, RKS/QMD | unaffected | this registry holds, and retains, no structural dependency on any of them (Section 4.A, Section 8) |
| Alignment Amendment 3 (Memory Core cross-reference) | outside this unit | Section 7 whole-defers `documentId`/any Memory-Core-linking field for a `DerivativeGenerationId` target to Amendment 3's own future decision; no shape, name, or coupling is presupposed in either direction |
| Alignment Amendment 4 (ingestion audit authority) | outside this unit | untouched; a review record may in the future be referenced by an audit fact, but this document does not establish that reference |
| Alignment Amendment 5 (CDR-007/OCR/EI cross-reference) | outside this unit | untouched |

No consumer listed above is modified by this document; every listed
implementation amendment is deferred to a later, separately governed
unit.

## 14. Required / conditional / optional / forbidden semantic classification (Step 11)

No Kotlin data class or sealed type is frozen by this document.

**A. Required governance semantics** (every conforming implementation
must preserve; a record missing any of these is not a valid review
record for either target kind):

- a review target value naming exactly one of `EvidenceArtifactId` or
  `DerivativeGenerationId` (Section 5);
- the `DerivativeReviewState` value being recorded, drawn from the
  existing closed four-value enumeration, unmodified;
- a timestamp for the recording;
- enforcement of the existing transition graph (Section 4.J), applied
  identically regardless of target kind;
- append-only recording — no update or removal of any prior record, for
  either target kind (Section 11).

**B. Conditionally required semantics** (required exactly when
applicable; explicitly absent, never defaulted, otherwise):

- `documentId`: mandatory for an `EvidenceArtifactId` target (unchanged);
  not part of this document's shape at all for a `DerivativeGenerationId`
  target — this is a whole deferral to Amendment 3's own future decision,
  not a condition this document evaluates (Section 7);
- `reviewingPrincipalId`: absent for the system-recorded initial
  `PENDING_REVIEW`; present for a human-recorded transition — unchanged
  from today, target-kind-neutral.

**C. Optional/extensible semantics** (useful, non-authority-defining):

- `note`: a reviewer's free-text reason for `REJECTED` or
  `NEEDS_CORRECTION`, unchanged from today;
- a future, purely-derived, non-authoritative helper for presenting a
  target's kind/identity together for display purposes, provided it
  asserts no additional authority and is never treated as identity
  itself.

**D. Forbidden semantics** (would create authority leakage or ambiguity;
no implementation may include these):

- any target representation that compares an `EvidenceArtifactId`'s and a
  `DerivativeGenerationId`'s underlying string values as equal, or that
  otherwise permits kind-crossing collision in registry keying (Section
  5, Section 11);
- any field or code path permitting `APPROVED` (or any other state) to
  alter, mutate, or replace the reviewed target's own underlying content
  or the source it descends from (Section 8);
- any field or code path permitting a review-state transition on one
  target to propagate, implicitly or explicitly, to any other target,
  including a reconciliation generation's parents (Section 9);
- any field or code path granting the registry a structural dependency
  on `EvidenceCustodian`, `MemoryCore`, `OcrMechanism`, Evidence
  Intelligence, or RKS/QMD (Section 8, Section 13);
- a fabricated, defaulted, or presupposed `documentId` (or any other
  Memory-Core-linking field) on a `DerivativeGenerationId` target's
  review record — that decision belongs entirely to Amendment 3, not to
  this document (Section 7);
- any retargeting of an already-recorded review record (Section 10);
- any caller-supplied credential or permission bypass embedded in the
  target or record shape itself, in place of the existing structural
  (reference-holding) authorization model (Section 4.A) — this document
  neither builds nor assumes a Permission Engine gate beyond what canon
  already discloses as expected and unwired;
- admission of a `PENDING_REVIEW` (or any) record for a target the
  calling coordinator has not itself already, successfully, and
  atomically established to exist (Section 12.1, Section 12.2);
- any registry implementation that manufactures, infers, repairs, or
  resolves a target identity rather than using exactly the identity its
  caller supplied, or that treats `currentReviewState` returning `null`
  as proof of a target's existence or non-existence rather than solely as
  "no review record has ever been appended" (Section 12.3, Section
  12.4).

## 15. Non-goals

Explicitly out of scope for this document:

- no parser implementation, plugin integration, OCR invocation, or model
  invocation;
- no Kotlin implementation of any kind — no sealed type, no interface
  change, no registry implementation change;
- no Memory Core change (Amendment 3 not begun);
- no Knowledge change;
- no RKS/QMD change;
- no Evidence Intelligence change;
- no Reasoning Context change;
- no `EvidenceArtifact` or `AcceptedEvidenceArtifact` redesign;
- no Derivative Generation Record redesign (Unit 2 unmodified);
- no source mutation of any kind;
- no real-evidence ingestion;
- no persistence technology selection;
- no dependency addition;
- no wiring of the Permission Engine expectation already disclosed
  (Section 4.A) — remains disclosed-but-unwired exactly as before;
- Alignment Amendments 3, 4, and 5 (not begun by this scope lock);
- any Runtime Integration work of any kind.

## 16. Implementation-plan obligations (deferred, not performed here)

Recorded for a future implementation-plan unit, not undertaken now:

1. Widen `DerivativeReviewRecord`'s target field and
   `DerivativeReviewRegistry.currentReviewState`'s parameter from bare
   `EvidenceArtifactId` to the closed two-case union fixed in Section 5.
2. Leave `documentId` mandatory, unchanged, for the `EvidenceArtifactId`
   case; omit it entirely from the `DerivativeGenerationId` case's shape,
   per Section 7's whole deferral — do not add a conditional or
   Amendment-3-shaped field speculatively.
3. Update `InMemoryDerivativeReviewRegistry`'s own key type accordingly,
   preserving every existing test's observable behaviour for the
   `EvidenceArtifactId` case unchanged.
4. Update `EvidenceExtractionCoordinator`'s existing call site to wrap
   its `EvidenceArtifactId` in the new union's corresponding case, with
   no other behavioural change.
5. Compose the `DerivativeGenerationId` case's admission-before-review
   ordering (Section 12.1) within the same Parker-owned ingestion
   coordinator that Unit 2 already assigns admission authority to —
   mechanical wiring only; the ordering itself is frozen by this
   document, not invented by the implementation unit.
6. Do not wire Evidence Intelligence, a human-review UI, Runtime
   Integration, or the disclosed Permission Engine gate — all remain
   separately governed, later work.

## 17. Conflicts discovered (Step 12)

**None with adopted governance.** One internal defect was found and
corrected during final-review re-checking, before owner acceptance: an
earlier draft of Section 7 conditioned `documentId`'s presence on "a
Memory Core cross-reference... exists (Amendment 3)," which presupposed a
specific future shape for Amendment 3 that no adopted document states or
implies (`DOCUMENT_INGESTION_CANONICAL_GOVERNANCE_ALIGNMENT.md` §3, the
only adopted description of Amendment 3's intent, says nothing about
`DerivativeReviewRecord.documentId`). This was corrected to a whole
deferral (Section 7) that decides nothing about Amendment 3's eventual
shape. A second gap — target-existence validation was described but not
frozen as an explicit rule — was closed by Section 12.1-12.4, which
identify the existing (byte-backed) and Unit-2-adopted (generation)
coordinator-sequencing disciplines as the already-sufficient answer,
inventing no new port.

Every rule in this document, as corrected, is either a direct,
unmodified restatement of the existing `DerivativeReview.kt`/test-suite/
Boundary Clarification §6 semantics (Sections 4, 6, 10, 11, 12), or a
narrow extension of Unit 2's own already-adopted rules applied to the
review domain (Sections 7, 9, 10, 12). The corrected `documentId`
deferral (Section 7) was tested against Evidence Custodian authority
(untouched), `AcceptedEvidenceArtifact` semantics (untouched), Derivative
Generation Record semantics (Unit 2 unmodified; nothing about it is
decided by this document), Memory Core (no field added, no write
introduced, no shape presupposed), OCR (untouched), Evidence Intelligence
(untouched, still unwired as a consumer), RKS/QMD (untouched), reasoning
authority (untouched; Tier C never entered), deletion authority
(untouched; this document creates none), and existing `DerivativeReview`
behaviour (Section 6 confirms zero change). No contradiction was found
with any adopted governance document.

## 18. Constitutional self-certification

| Authority | Check | Result |
| --- | --- | --- |
| Parker Constitution | Parker owns authority; modules provide capability | Section 4.A restates: only the coordinator records; no plugin ever calls `recordReviewState` |
| Epistemic Integrity | No fabrication; unknown stated as unknown | Section 7 (`documentId` deferred whole, never presupposed or fabricated), Section 12.3 (`null` never overstated as proof of existence or non-existence) |
| Evidence Artifact governance | Frozen; not reopened | Not touched; Section 6 preserves the byte-backed path unmodified |
| Evidence Custodian | Sole custodian; no dependency from this registry | Section 4.A, Section 8, Section 13 all confirm "no structural dependency," unchanged |
| CDR-006 | Original evidence custody/immutability frozen | Not reopened; Section 8 confirms review never mutates a source |
| CDR-007 / Evidence Intelligence | OCR/extraction assigned to EI; EI not a truth authority | Section 8, Section 13: EI remains an unwired future consumer, granted no new authority |
| Document Ingestion Unit 1/Unit 2 (`84cc061`/`4faaeb8`) | Governing authority for this programme | Every section cites and restates, never contradicts, already-adopted rules |
| OCR governance | Untouched | Section 8; reviewing OCR-derived output never invokes or authorizes OCR |
| Memory Core | Provenance/Document unchanged; not spoken for by this document | Section 7's whole deferral of `documentId`/any Memory-Core-linking field to Amendment 3; no schema, write-path, or future shape presupposed |
| RKS/QMD | Untouched, retrieval-only | Section 8, Section 13 |
| Reasoning Context | Untouched | Section 13 |
| Deletion governance | No new deletion authority created | Not addressed by this document; review records' own retention is unaffected and undecided here, exactly as Unit 2 deferred derivative retention |
| ADR-024 | Modules never write directly to platform state | Section 4.A: only the coordinator writes; unchanged |

## Final Recommendation

**READY FOR OWNER REVIEW** (scope-lock stage).
