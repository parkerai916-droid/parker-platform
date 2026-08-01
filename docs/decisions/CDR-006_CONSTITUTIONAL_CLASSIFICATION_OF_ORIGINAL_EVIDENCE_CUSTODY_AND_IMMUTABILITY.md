# CDR-006: Constitutional Classification of Original Evidence Custody and Immutability

## Status

**Frozen.** This record has completed independent constitutional
verification and Final Freeze Verification
(`docs/reviews/EVIDENCE_CUSTODIAN_CDR-006_FINAL_FREEZE_REVIEW.md`,
1 August 2026) and is Accepted and Canonical. No Kotlin is implemented,
proposed, or changed by this record. Neither `src/` nor `tests/` is
touched by this record. `docs/architecture/epistemic-integrity.md`,
`docs/architecture/MEMORY_CORE_CONTRACT_DESIGN.md`, and
`docs/architecture/MEMORY_CORE_SCOPE_LOCK.md` remain unmodified by this
record. Nothing is staged, committed, or pushed by this record.

---

## Context

The Governance Impact Assessment ("Evidence Immutability and Derived
Work Product") found that Epistemic Integrity Amendment No. 1's Article
VIII (Provenance and Evidential History), Article IX (Integrity of
Evidence), and Article XIX (Future Capabilities) already establish, in
substance, that original evidence must be distinguished from derivative
evidence, that the relationship between them must be preserved, and
that these obligations already bind "Evidence Intelligence" and
"Document Intelligence" by name as anticipated future subsystems. The
assessment also found two gaps: first, Article IX's preservation duty is
qualified by Article II's general "wherever reasonably practical"
standard and its disclosed-departure mechanism, rather than being
absolute; second, and the subject of this record, no governance document
assigns any subsystem responsibility for the technical custody of an
original evidence artefact's own stored content.
`docs/architecture/MEMORY_CORE_CONTRACT_DESIGN.md` §5 (Document
Contract) is explicit that Memory Core's role is "registration
only" — it "never fetches, opens, or validates" the location reference a
Document names, and its `integrityHash` field is nullable precisely
because a registration "reference to an external, mutable location"
cannot always supply one. Fetching, storing, or interpreting a
document's own contents is repeatedly and explicitly assigned to a
future "Document Handling" Programme that does not yet exist as a
governed subsystem (Contract Design §5, §17; Governance Review §13;
Scope Lock §4).

**A terminological note, adopted deliberately and applied consistently
throughout this record — revised at this stage to remove a legal
ownership ambiguity identified on review.** This record speaks of
**custody**, never **ownership**, when describing any subsystem's
relationship to original evidence. It also deliberately does not
conflate two distinct senses of "owner" that appear elsewhere in this
Programme's governance:

1. **"The owner"** as the Parker Constitution already defines the term —
   the person who owns and controls a given Parker instance and remains
   its final authority ("the owner remains in control"; Parker User
   Rights: "own their data," "own their memories"). This is a relationship
   between a person and their Parker instance, never a legal
   determination about any specific artefact's content.
2. **The legal or rights-holding owner** of a specific evidence
   artefact — its copyright holder, proprietor, or other legally
   recognised rights-holder — which is an entirely separate question
   Parker does not, and cannot always, resolve. A Parker instance owner
   may lawfully possess, submit, or control an artefact — a court
   exhibit, a government record, an employer's file, a medical record, a
   contract, a corporate document, multi-party correspondence, or
   material in which another party holds an interest — without being its
   legal owner in the second sense.

**Nothing in this record transfers, alters, determines, or asserts
ownership, copyright, proprietary interest, legal custody, or any other
legal right relating to an evidence artefact.** Parker does not, and no
subsystem this record designates is authorised to, adjudicate or assert
legal ownership of anything it holds. What this record establishes is
narrower and entirely technical: **Parker's Evidence Custodian provides
technical custody only.** Parker preserves evidence. Parker protects
evidence from modification, overwriting, replacement, and obscuring.
Parker maintains provenance. Parker enforces immutability while
retained. Parker does not determine legal ownership — and this record
does not require, and must not be read to require, any future
implementation to attempt that determination as a precondition of
custody.

This decision is constitutionally material because it determines, for
the forthcoming Evidence Intelligence Programme and for the targeted
Article IX amendment this same governance stage produces, which
subsystem must be built to actually enforce the "must never modify,
overwrite, replace, or obscure" duty — a question bearing directly on
the "if a safeguard cannot be pointed to in the architecture, it does
not count as a guarantee" principle (Constitution), and on Memory Core's
own already-frozen scope boundary (an incorrect answer risks either
silently re-expanding Memory Core past a boundary this Programme has
independently reaffirmed multiple times, or leaving original evidence
custody with no assigned custodian at all).

This record reviewed `docs/architecture/parker-constitution.md`,
`docs/architecture/epistemic-integrity.md` (Articles I, II, VIII, IX,
XVI, XVII, XVIII, XIX), `docs/architecture/user-authorship-and-evidence.md`,
`docs/architecture/MEMORY_CORE_CONTRACT_DESIGN.md` (§5, §7, §11, §12),
`docs/architecture/MEMORY_CORE_SCOPE_LOCK.md` (§4, §6, §8),
`docs/architecture/MEMORY_CORE_GOVERNANCE_REVIEW.md` §13,
`docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_CONTRACT_DESIGN_V2.md`,
`docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_SCOPE_LOCK.md`,
`docs/governance/PROGRAMME_3_UNIT_7_SCOPE_LOCK_CLARIFICATION.md`, the
accepted Governance Impact Assessment, and CDR-001 through CDR-005 in
full.

---

## Decision Question

> Which governed Parker subsystem holds technical custody of, and
> enforces immutability for, preserved original evidence artefacts —
> without determining, asserting, or requiring a determination of legal
> ownership of them?

---

## Constitutional Constraints

Any answer must remain consistent with:

- **Legal ownership is never asserted or determined by this record.**
  Only technical custody is assigned. Neither this record, nor any
  subsystem it designates, decides who legally owns, holds copyright in,
  or has a proprietary interest in an evidence artefact. This record
  decides only which subsystem may hold technical custody and enforce
  immutability as a delegated, revocable, purely technical
  responsibility — never a legal or proprietary claim.
- **Structural, not merely behavioural, guarantee.** "If a safeguard
  cannot be pointed to in the architecture, it does not count as a
  guarantee" (Constitution). The chosen model must be capable of a
  runtime/write-path enforcement mechanism, not only a cognition-layer
  norm.
- **Article XIX's forward binding.** Epistemic Integrity Article XIX
  already extends Articles VIII, IX, XVI, and XVIII to "Evidence
  Intelligence" and "Document Intelligence" by name, before either
  exists. Whichever subsystem this record designates as custodian
  inherits those obligations automatically; this record does not need
  to, and does not, re-grant them.
- **Memory Core's already-frozen exclusion.** Memory Core Scope Lock §4
  excludes "Document Intelligence, OCR, PDF parsing, file import, image
  analysis" from Memory Core's scope, reserving them to "a future
  Document Handling Programme." This exclusion has already been
  independently reaffirmed across the Governance Review, the Contract
  Design, and every Programme 3 document that cites it. Any model that
  requires reopening or contradicting this exclusion carries a high
  burden of justification this record does not find satisfied (see
  Model A, below).
- **Owner data rights and the existing erasure path.** The Constitution
  grants the owner the right to "own their data" and "own their
  memories"; Memory Core Scope Lock §8 already reserves `DELETED`
  exclusively to the owner-requested erasure path, gated by
  `PermissionAction.DELETE` (Contract Design §11 table). Any model must
  preserve, not narrow, this existing instance-control right and its
  existing gating.
- **No bypass / fail-closed.** No model may leave open a path by which a
  reasoning provider or ordinary evidence-analysis capability reaches
  deletion or modification authority over an original without passing
  through Permission Engine authorisation.
- **No functional justification for erasure.** No subsystem's own
  operational convenience, storage efficiency, or technical design may
  ever justify destroying, altering, replacing, or losing a preserved
  original (developed fully as a Decision Rule below).
- **Minimal-reopening discipline.** Consistent with Amendments 1, 2, and
  5 and CDR-005's own reasoning, the model that does not force reopening
  an already-frozen document without a repository-grounded reason is
  preferred over one that does.

---

## Options Considered

### Model A — Memory Core Custody

Extend Memory Core's Document contract so Memory Core holds custody of
and technically protects the underlying artefact bytes.

**Strengths:** no new subsystem; Document already carries
`locationReference`/`integrityHash`; reuses Section 12's existing,
already-frozen immutable-field pattern.

**Constitutional risks:** directly contradicts Memory Core Scope Lock
§4's own explicit, frozen exclusion of Document Handling, OCR, PDF
parsing, file import, and image analysis from Memory Core's scope.
Memory Core Contract Design §5 states in its own words that fetching,
storing, or interpreting a document's contents is "Document Handling's
own, later, separate concern," and that "nothing about this contract
anticipates or reserves space for it" beyond a single processing-status
field. Adopting Model A would require reopening and materially
contradicting a boundary this Programme has independently reaffirmed at
least three times (Scope Lock, Governance Review, Contract Design), with
no new repository evidence justifying the reversal. It would also
conflate two structurally distinct responsibilities this architecture
has deliberately kept separate from Memory Core's first design:
recording facts about a document (registration/provenance
infrastructure) versus actually holding and technically protecting its
bytes (custody infrastructure) — and, independently of that conflation,
still would not require or enable Memory Core to determine legal
ownership of anything, which remains out of scope under every model.

**Consequences:** Memory Core's Document contract, Scope Lock §4, the
Governance Review, and every Programme 3 document citing this boundary
would all require reopening; Memory Core would acquire a custody
responsibility orthogonal to its stated purpose as "the authoritative
system of record" for facts, not bytes.

**Scalability:** poor. Every future evidence format (audio, video, new
container types) would require Memory Core's own frozen contract to be
extended again, permanently coupling a general-purpose fact registry to
arbitrary artefact-custody concerns it was designed to exclude.

### Model B — Evidence Intelligence Custody

Retain Memory Core as registration and provenance infrastructure while
assigning artefact custody and technical immutability enforcement to a
future Evidence Intelligence or Document Handling subsystem — an Evidence
Custodian, in the technical-custody-only sense defined above, never a
legal owner, adjudicator of ownership, or rights-holder of any kind.

**Strengths:** consistent with Memory Core's own, already-frozen
exclusion — Document Handling/Evidence Intelligence is already the
named, anticipated custodian in three independently frozen documents
(Contract Design §5/§17; Governance Review §13; Scope Lock §4), and
Epistemic Integrity Article XIX already names "Evidence Intelligence"
and "Document Intelligence" as future subsystems inheriting Article
VIII/IX obligations. This model requires no new constitutional
invention — only formal recognition of a boundary the architecture
already anticipated. It keeps custody (a distinct, delegated, revocable,
technical responsibility) separate from fact-registration (Memory
Core's actual purpose) and, more fundamentally, separate from legal
ownership, copyright, and proprietary interest (questions this model
never asks the custodian to resolve), consistent with the Constitution's
own capability/authority separation principle applied here to
custody/legal-rights. Memory Core's
`Document.locationReference`/`integrityHash` continue to function
exactly as designed — the pointer and tamper-detection hook into
whatever the Evidence Custodian actually holds — with no change required
to either field.

**Constitutional risks:** requires a new subsystem's own governance
(Contract Design, then Scope Lock) to eventually be drafted before
Evidence Intelligence may be implemented — explicitly deferred by the
originating instruction and not performed by this record. Until that
governance exists, the custody guarantee is a constitutional commitment
without a built enforcement mechanism — a gap this record discloses
candidly rather than treats as closed.

**Consequences:** no amendment required to Memory Core's Document or
Provenance contracts. A future Evidence Artifact Contract Design
(already identified as the next required document by the accepted
Governance Impact Assessment, and explicitly not drafted here) becomes
the vehicle that operationalises the Evidence Custodian's structural
immutability enforcement for whatever storage mechanism is eventually
chosen — and that future document, too, must not be drafted to require
the custodian to determine legal ownership as a condition of accepting
or retaining custody.

**Scalability:** good. New evidence formats remain the Evidence
Custodian's own, separately scoped concern; Memory Core's contract
remains untouched regardless of how many artefact types the custodian
eventually supports.

### Model C — Provenance-Only Sufficiency

Treat current Knowledge Memory and provenance capabilities as sufficient
without establishing a dedicated artefact-custody subsystem.

**Strengths:** no new governance or subsystem required; Provenance's
`contentNature`/`derivedFromReferences`/`extractedFromReference` chain
already gives strong traceability between derivative and original
evidence; fastest path to declaring the invariant addressed.

**Constitutional risks:** conflates traceability (a Knowledge
Memory/Provenance concern, already well served) with custody (a
storage/technical-write-path concern the Provenance chain does not, and
structurally cannot, provide). Provenance records the *relationship*
between derivative and original evidence; it does not prevent
modification of the original artefact at its external location — Memory
Core Contract Design §5 explicitly anticipates that location may be "an
external, mutable location," a fact no Provenance field changes. Model
C does not, in substance, answer this record's own Decision Question:
it identifies a subsystem responsible for describing custody's lineage
after the fact, not one responsible for the "must never modify,
overwrite, replace, or obscure" duty itself. Adopting it would leave
Article IX's core preservation obligation with no structural enforcement
anywhere in the architecture — directly contrary to "if a safeguard
cannot be pointed to in the architecture, it does not count as a
guarantee" (Constitution).

**Consequences:** no new governance drafted, but the permanent invariant
the originating instruction requires would remain aspirational rather
than structural, contrary to that instruction's own framing of it as "a
permanent design requirement," not a description of already-sufficient
behaviour.

**Scalability:** does not scale to any evidence type or custody threat
Parker does not already, coincidentally, avoid — it relies entirely on
discipline the Governance Impact Assessment's own Gap 2 finding already
identified as insufficient.

---

## Decision

```
Model A — Rejected
Model B — Evidence Intelligence Custody — Adopted
Model C — Rejected
```

**Independent confirmation, not an assumed outcome.** The accepted
Governance Impact Assessment's expected recommendation (Model B) is
confirmed here on independent grounds, not adopted merely because it was
expected: Model A is rejected because it directly contradicts an
already-frozen, multiply-reaffirmed Memory Core boundary with no new
repository evidence justifying the reversal; Model C is rejected because
it does not, in substance, answer the Decision Question — it names a
traceability mechanism, not a custodian. Model B is the only option that
both answers the question asked and remains consistent with the
existing, already-frozen document set without contradiction. None of the
three models was assessed on, or requires, any capacity to determine
legal ownership — that question is outside this record's scope under
every option considered.

---

## Decision Rules

- **This record does not decide legal ownership.** Nothing in this CDR
  transfers, alters, determines, or asserts ownership, copyright,
  proprietary interest, legal custody, or other legal rights relating to
  an evidence artefact, and no future implementation of the Evidence
  Custodian may be built to require such a determination as a
  precondition of accepting, preserving, or protecting an artefact.
  Parker's Evidence Custodian provides **technical custody only**:
  Parker preserves evidence; Parker protects evidence from modification,
  overwriting, replacement, and obscuring; Parker maintains provenance;
  Parker enforces immutability while retained. Parker does not determine
  legal ownership.
- **Who holds custody.** A future Evidence Intelligence / Document
  Handling subsystem — the Evidence Custodian, not yet governed, and not
  created by this record — is the sole intended custodian of preserved
  original evidence artefacts and the sole intended enforcer of their
  technical immutability. "Evidence Intelligence" and "Document
  Handling" are used here as provisional labels for whichever subsystem
  is eventually built to hold custody; this record does not decide, and
  leaves to the Evidence Artifact Contract Design, whether that
  subsystem is organised as part of a broader analytical Evidence
  Intelligence programme or as a separate, first-class infrastructure
  subsystem that such a programme consumes. This custodianship is held
  on behalf of whoever lawfully submitted the artefact or authorised
  Parker's custody of it, regardless of whether that person is also the
  artefact's legal owner, copyright holder, or other rights-holder — a
  question this record does not require the custodian to resolve.
- **What Memory Core continues to own.** Registration (Document
  contract) and provenance/derivation chain (Provenance contract) —
  unchanged, unexpanded, exactly as already frozen. This record amends
  neither.
- **Separate-identity requirement.** Original evidence and any
  derivative artefact — including OCR output, transcription, extraction,
  thumbnails, normalised copies, previews, and summaries — must never
  share record identity. A derivative is always a new, separately
  identified object, linked to its original through Provenance's
  existing `derivedFromReferences`/`extractedFromReference` mechanism,
  never a revision in place of the original.
- **Immutability while retained.** Once accepted into preserved custody
  by the Evidence Custodian, an original evidence artefact's content
  must never be modified, overwritten, replaced, or obscured for as long
  as it remains retained. This is a structural, unconditional obligation
  once custody attaches — not one subject to a disclosed-departure
  exception, and not one conditioned on any determination of legal
  ownership.
- **Constitutional Optimisation Safeguard.** No Parker subsystem —
  including the Evidence Custodian itself — may require, justify, or
  perform the destruction, alteration, replacement, or loss of a
  preserved original evidence artefact in order to perform its own
  function, achieve storage efficiency, or simplify its own processing.
  A derivative artefact's existence, however accurate, complete, or
  operationally convenient, never becomes a justification for retiring
  the original it was derived from. This includes, without limitation
  and by way of illustration only: deleting an original after optical
  character recognition; replacing a PDF with its extracted text;
  replacing a screenshot with recognised text; replacing an audio or
  video recording with its transcript; replacing evidence with a vector
  embedding generated from it; and replacing an original artefact with
  a summary of it. Derivative artefacts of every kind remain permitted,
  encouraged, and useful; replacement of a preserved original is not,
  regardless of which subsystem proposes it or how technically
  well-justified the proposal appears.
- **Derived Knowledge is not evidence.** Reaffirmed, not reopened: a
  `KnowledgeItem` (Programme 3) remains a governed representation
  derived from evidence, never evidence itself.
- **Derived Work Product is never evidence.** Extending the same,
  already-established principle as an explicit constitutional rule, not
  merely a description: Derived Work Product — any report, chronology,
  summary, witness-statement draft, briefing paper, or other governed
  representation Evidence Intelligence or a downstream capability
  produces — is never evidence. It is a governed representation derived
  from evidence, disposable and regenerable, and it must never be
  presented, treated, or relied upon as a substitute for the preserved
  original evidence it describes.
- **Mandatory traceability.** Every material factual representation in
  Derived Work Product must retain a resolvable provenance chain back to
  the preserved original evidence relied upon, using the
  `derivedFromReferences`/`extractedFromReference` mechanism Memory
  Core's Provenance contract already provides. This is a requirement,
  not merely an available capability, for any future Evidence
  Intelligence or Derived-Work-Product-generating capability.
- **Inference labelling.** An analytical inference must be marked as an
  inference, distinct from evidence, and must identify the evidence it
  was reasoned from — consistent with, and not reopening, `user-authorship-and-evidence.md`'s
  existing evidence/inference/opinion/allegation/lived-experience
  distinction.
- **Conflict preservation.** Conflicting evidence must be preserved and
  represented as conflict, never resolved by overwriting one side —
  consistent with Epistemic Integrity Article XVI/XVII and Programme 3's
  own `COMPETING_EXPLANATIONS` precedent.
- **Deletion remains available and remains gated — an instance-control
  right, not a legal-ownership determination.** Owner-requested deletion
  continues through an explicit, authorised, and audited erasure
  process, mirroring Memory Core's own `DELETED`/`PermissionAction.DELETE`
  precedent (Scope Lock §8; Contract Design §11). "Owner" here means the
  person who controls the Parker instance and is constitutionally
  authorised to make this request, exactly as the Constitution already
  defines that role — this right does not depend on, and Parker does not
  verify, that person's legal ownership of the artefact's content. It is
  never available to a reasoning provider or ordinary evidence-analysis
  capability directly, and this record does not create any new deletion
  path — it relies on the gating mechanism already governed elsewhere.
  The Constitutional Optimisation Safeguard above does not narrow this
  right: an owner's own, explicit, authorised, audited deletion request
  is never itself a subsystem "requiring, justifying, or performing"
  destruction for its own functional convenience — it is the instance
  owner exercising a distinct, constitutionally protected right, and the
  two must never be conflated.
- **When Evidence Intelligence's own governance is required.** Before
  any implementation of custody or storage begins, following this
  Programme's established Contract Design → Scope Lock → Implementation
  Plan sequence. This record does not constitute that governance and
  does not authorise implementation to begin.
- **When a further CDR is required.** If the Evidence Custodian's own
  future Contract Design finds that this record's boundary (Memory Core
  = registration/provenance; Evidence Custodian = custody/enforcement)
  cannot cleanly accommodate a specific evidence type, storage mechanism,
  or genuinely contested classification — including any future question
  about how Parker should behave if a legal-ownership dispute over a
  custodied artefact becomes known to it, which this record does not
  anticipate or resolve.

This record remains technology-independent throughout: no storage
mechanism, file format, hashing scheme, interface, or Kotlin type is
prescribed or implied by any of these rules.

---

## Constitutional Visibility

This record does not create a central registry of custodied artefacts.
Visibility is preserved distributively, consistent with how this
Programme has already made every other capability independently
discoverable: Memory Core's own Document contract continues to state its
own registration-only scope; the future Evidence Artifact Contract
Design, once drafted, becomes the single place custody rules live,
discoverable independently by any reviewer without a separate registry.
Any classification a reviewer finds doubtful remains open to challenge
through the same CDR escalation path this record itself uses.

---

## Consequences

- **Epistemic Integrity Amendment:** Article IX requires the narrow,
  surgical amendment (drafted separately, Deliverable 2) establishing an
  absolute custody-preservation sub-rule and the Constitutional
  Optimisation Safeguard; no other Article is affected.
- **Memory Core:** no amendment. Document and Provenance contracts are
  unchanged. Scope Lock §4's exclusion is reaffirmed, not reopened.
- **Programme 3 Knowledge Memory:** unaffected. "Derived knowledge is not
  evidence" is reaffirmed, not reopened. No conflict with Unit 7
  retirement or restoration: both operate exclusively on
  `KnowledgeItem.status` and never touch original evidence or Memory
  Core's write path (Unit 7 Scope Lock Clarification §8, §9; both
  already independently verified never to write to Memory Core).
- **Future Evidence Artifact Contract Design:** becomes the vehicle for
  the Evidence Custodian's actual enforcement mechanism, including the
  Constitutional Optimisation Safeguard's technical realisation. That
  document must likewise not require legal-ownership determination as a
  precondition of custody. Explicitly deferred; not begun by this
  record.
- **Legal ownership, copyright, and proprietary interest:** unaffected,
  unaddressed, and unresolved by this record in every respect — this
  record establishes only technical custody, never a legal or rights
  determination, and no future implementation of the Evidence Custodian
  may be built to assert or require one.
- **Owner (instance-control) rights:** reinforced. Deletion remains
  authorised and audited, and is available regardless of the instance
  owner's legal-ownership status over the artefact's content; immutability
  while retained is explicitly distinguished from indefinite retention
  (see the accompanying Article IX amendment).

---

## Rejected Alternatives

**Model A** is rejected because it requires reopening and materially
contradicting Memory Core Scope Lock §4's own explicit, already-frozen
exclusion of Document Handling/artefact-content concerns from Memory
Core's scope, with no new repository evidence justifying that reversal,
and because it conflates registration/provenance infrastructure with
custody infrastructure — two responsibilities this architecture has
deliberately kept separate since Memory Core's first design.

**Model C** is rejected because it does not, in substance, answer this
record's own Decision Question: it identifies a mechanism for describing
the relationship between derivative and original evidence, not a
custodian responsible for the underlying artefact's technical
immutability, and adopting it would leave that obligation with no
structural enforcement anywhere in the architecture.

---

## Non-Decisions

CDR-006 does not decide, and no future reader should treat it as having
decided:

- any implementation interface, Kotlin type, storage mechanism, file
  format, or hashing scheme for Evidence Intelligence or artefact
  custody;
- the content of a future Evidence Artifact Contract Design, Evidence
  Intelligence Scope Lock, Derived Work Product schema, implementation
  plan, storage design, API, or test — all explicitly deferred;
- any amendment to Memory Core's Document or Provenance contract fields;
- retention duration or any data-lifecycle/expiry policy beyond
  confirming that immutability-while-retained is not itself a mandatory
  indefinite-retention rule;
- **any question of legal ownership, copyright, proprietary interest, or
  lawful possession of an evidence artefact** — this record establishes
  technical custody only and takes no position, express or implied, on
  who legally owns, or has rights in, any artefact Parker custodies;
- what Parker should do if it becomes aware of a legal-ownership dispute
  over a custodied artefact — not anticipated or addressed by this
  record;
- whether the Evidence Custodian is organised as part of a broader
  analytical Evidence Intelligence programme or as a separate,
  first-class infrastructure subsystem — left to the Evidence Artifact
  Contract Design;
- the exact wording of the Article IX amendment beyond what is drafted
  alongside this record (Deliverable 2) — that text stands on its own
  and is subject to its own review.

---

## Verification Criteria

A future reviewer may confirm this decision has been followed by
checking that:

- any Evidence Intelligence or Document Handling governance document
  defines artefact custody and enforcement, never Memory Core's own
  Document or Provenance contract, and never describes that subsystem as
  a legal "owner" of evidence or requires it to determine legal
  ownership, copyright, or proprietary interest as a condition of
  custody;
- Memory Core Scope Lock §4's exclusion remains unamended;
- the Epistemic Integrity Article IX amendment described in this
  governance stage has been adopted before any Evidence Intelligence
  implementation proceeds;
- deletion of original evidence remains gated by
  `PermissionAction.DELETE`/instance-owner authorisation only, never
  reachable by a reasoning provider or ordinary evidence-analysis
  capability, and is never made conditional on proof of the instance
  owner's legal ownership of the artefact;
- no derivative artefact (OCR output, transcript, extract, thumbnail,
  normalised copy, preview, summary, embedding) shares record identity
  with its original, or is ever proposed, justified, or implemented as a
  replacement for it;
- Derived Work Product generated by any future capability retains a
  resolvable provenance chain to original evidence for every material
  factual representation it contains, and is never presented as a
  substitute for the original;
- no governance document or implementation asserts, adjudicates, or
  requires a determination of legal ownership of any evidence artefact.

---

## Final Report

**Document created:** `docs/decisions/CDR-006_CONSTITUTIONAL_CLASSIFICATION_OF_ORIGINAL_EVIDENCE_CUSTODY_AND_IMMUTABILITY.md`
(only file created by this task; see accompanying report for the other
files this governance stage also wrote).

**Decision:** Model B — Evidence Intelligence Custody — Adopted; Models
A and C rejected. Legal ownership, copyright, and proprietary interest
are outside this record's scope in every respect. This record does not
decide whether the Evidence Custodian is organised within a broader
Evidence Intelligence programme or as a separate, first-class
infrastructure subsystem — that question is resolved by the Evidence
Artifact Contract Design.

**Status:** Frozen, incorporating the ownership clarification and the
provisional-labels clarification approved on review. Independent
constitutional verification and Final Freeze Verification completed
1 August 2026 (`docs/reviews/EVIDENCE_CUSTODIAN_CDR-006_FINAL_FREEZE_REVIEW.md`),
consistent with this Programme's established lifecycle.

CDR-006 — RATIFIED — INDEPENDENT CONSTITUTIONAL VERIFICATION AND FINAL
FREEZE VERIFICATION COMPLETE

Confirmed: no production code modified by this record; no tests
modified by this record; `docs/architecture/MEMORY_CORE_CONTRACT_DESIGN.md`
and `docs/architecture/MEMORY_CORE_SCOPE_LOCK.md` not modified; CDR-001
through CDR-005 not modified; nothing staged; nothing committed; nothing
pushed by this record. The Evidence Artifact Contract Design, Evidence
Custodian Scope Lock, and Evidence Custodian Implementation Plan are
each separately ratified per the same Final Freeze Review; Evidence
Custodian implementation Phases 1–6 are complete and Phases 7–10 remain
pending; Evidence Intelligence implementation has not started.
