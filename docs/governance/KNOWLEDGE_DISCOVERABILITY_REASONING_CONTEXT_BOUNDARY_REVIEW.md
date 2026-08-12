**Status:** Boundary Review only. Governance and scoping document. No Kotlin is
implemented, proposed as a diff, or changed by this document. Neither `src/`
nor `tests/` is touched. This document does not amend any frozen governance
document (`docs/architecture/MEMORY_CORE_SCOPE_LOCK.md`,
`docs/architecture/MEMORY_CORE_CONTRACT_DESIGN_ERRATA_004.md`,
`docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_SCOPE_LOCK.md`,
`docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_CONTRACT_DESIGN_V2.md`,
`docs/governance/TRUST_FRAMEWORK_MEMORY_RETRIEVAL_CONTRACT_DESIGN.md`, or any
other frozen or adopted governance document) — it reads, cites, and reasons
from its own governing input,
`docs/governance/KNOWLEDGE_DISCOVERABILITY_REASONING_CONTEXT_PLANNING_REVIEW.md`.
It does not select a final content-representation design, a final
retrieval-surface mechanism, or a final Authorization Purpose rule. It does
not select final interfaces, request shapes, policy rules, or implementation
ordering. It does not rename Evidence Intelligence or modify any source
comment. It does not create or reserve a new numbered gap. It does not begin
a Contract Design. Gap #54 remains complete and is not reopened.

# Knowledge Discoverability and Governed Retrieval into Reasoning Context — Boundary Review

---

**Amendment Note (Timing Scope Correction).** Independent Contract Design review found that
Section 6's original, absolute "timing must not leak unauthorized knowledge" wording conflicted
with this same section's own "act-level authorization occurs before persistence inspection"
requirement: act denial and an authorized retrieval necessarily perform different amounts of work,
and therefore can honestly differ in elapsed wall-clock time, in any in-process architecture this
repository has ever built. This amendment narrows only the timing claim (Section 6, below); it does
not weaken the content, identity, count, match, denial-detail, authorization, Purpose, or Memory
Core non-disclosure guarantees Section 6 already fixes, all of which remain unchanged. Gap #54
remains complete and is not reopened by this amendment. No implementation is introduced by this
amendment.

---

## 1. Governing Input

This Boundary Review's sole governing input is
`docs/governance/KNOWLEDGE_DISCOVERABILITY_REASONING_CONTEXT_PLANNING_REVIEW.md`
("the Planning Review"), read fresh in full. Every citation below traces
either to the Planning Review directly or, through it, to a document the
Planning Review itself already cited (Planning Review Section 1). This
Boundary Review does not perform its own fresh read of production source or
of the documents underlying the Planning Review — it fixes boundaries from
what the Planning Review already established as the fixed factual baseline
(Planning Review Section 5) and already-governed direction (Planning Review
Section 7), and from the questions the Planning Review left open (Planning
Review Section 6, Section 8).

---

## 2. Purpose and Scope of This Boundary Review

This Boundary Review fixes the subsystem, authority, information-flow, and
scope boundaries that a future Contract Design must obey. It does not select
final interfaces, request shapes, policy rules, or implementation ordering —
those remain Contract Design's own responsibility, exercised inside the
boundaries fixed here.

This document follows the same Planning Review → Boundary Review → Contract
Design → Scope Lock → Implementation Plan → independently reviewed
implementation units → live verification → Closure Determination discipline
the Planning Review itself follows (Planning Review Section 2, Section 10),
and occupies exactly the second stage of that sequence.

This is not a reopening of Gap #54. Gap #54 is complete (Planning Review
Section 4) and remains complete and untouched by this document.

---

## 3. Formal Ownership

- **Programme 4 (Reasoning Context) owns the cutover.** The Reasoning Context
  cutover — wiring `DefaultReasoningContextAssembler` (or its successor) onto
  Knowledge Memory's own retrieval surface in place of the legacy
  `KnowledgeSource`/`KnowledgeStore` path — is formally and independently
  assigned to Programme 4 by three already-accepted governance documents
  (Planning Review Section 3). This Boundary Review does not reopen, weaken,
  or restate that allocation beyond acknowledging it as fixed.
- **Knowledge Memory owns promoted `KnowledgeItem` retrieval.** Every governed read
  of promoted, evaluated knowledge — including any future content-relevance
  narrowing — occurs through Knowledge Memory's own retrieval surface
  (`DefaultKnowledgeRetrieval` or its successor), not through a path Reasoning
  Context or any other consumer constructs for itself.
- **Memory Core remains the sole owner/source of raw remembered content,
  provenance, and relationships.** No other subsystem may hold, copy, or duplicate
  raw remembered proposition content, provenance, or relationship data. This
  restates, without weakening, the frozen Programme 3 guarantee that
  `KnowledgeItem` "never copies or duplicates Memory Core content" (Planning
  Review Section 6, citing
  `docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_SCOPE_LOCK.md` Section 3).
- **Reasoning Context consumes governed Knowledge results; it does not query raw
  Memory Core directly.** Reasoning Context's only lawful knowledge input is a
  Knowledge Retrieval result. No boundary fixed by this document permits Reasoning
  Context, its assembler, or any component acting on its behalf to hold, construct,
  or invoke a `MemoryRetrieval`-shaped dependency of its own.
- **The model receives only assembled Reasoning Context, never retrieval
  authority.** The model prompt receives rendered Reasoning Context entries — text,
  not capability. No boundary fixed by this document permits a retrieval
  capability, credential, or reusable query handle to reach the model or to be
  reachable from anything the model produces.

---

## 4. Information Flow

The permitted abstract flow for the governed read (query) path is:

```
owner query
  -> conversation/reasoning coordination
  -> Reasoning Context assembly
  -> governed Knowledge Retrieval
  -> authorized promoted KnowledgeItem selection
  -> governed resolution of referenced content, where Contract Design permits
  -> safe Knowledge result representation
  -> ReasoningContext entry
  -> model prompt
```

This diagram fixes which subsystem crossings are permitted and prohibits any bypass
of a crossing it shows — for example, Reasoning Context assembly calling Memory
Core directly instead of Knowledge Retrieval. Principal, correlation, query, and
authorization context may propagate through multiple stages of this flow, carried
forward rather than dropped and reconstructed, exactly as Contract Design defines
(Section 5, below).

The following partial-order invariants are fixed and may not be reordered by
Contract Design:

- act-level authorization occurs before persistence inspection;
- governed Memory Core resolution occurs before referenced-content disclosure;
- safe Knowledge-result construction occurs before Reasoning Context projection;
- Reasoning Context assembly occurs before model-prompt delivery.

Beyond these four fixed points, the internal ordering of candidate enumeration,
item-level authorization, evidence resolution, relevance evaluation, and result
bounding is left for Contract Design to sequence lawfully — it is not fixed by
this Boundary Review as a single total order.

**This is a distinct path from the existing Remember/promotion (write) path, which
remains unchanged.** The real owner Remember-to-promotion path — an explicit owner
Remember instruction reaching `MemoryAdmissionCoordinator`,
`DefaultKnowledgeSubmission.submit` gating on Evaluation B,
`DefaultKnowledgeCandidateEvaluator.evaluate` resolving evidence through the
candidate-purpose-bound `MemoryRetrieval` view, and promotion storing a
`KnowledgeItem` (Planning Review Section 5) — is a write path, governed by Gap
#54's own accepted mechanism, and is neither addressed nor modified by this
Boundary Review or by anything it authorizes Contract Design to decide. The
information-flow diagram above describes only the read (query) path this
programme adds; it does not describe, replace, or reorder the write path.

---

## 5. Principal and Purpose Boundaries

- The owner/requesting principal originates with the conversational request
  itself, not with any downstream stage of the read flow.
- Principal and correlation identity must propagate through the flow without
  substitution or fresh minting wherever continuity is required for a
  downstream authorization decision.
- The Parker runtime/system principal may be used only for an
  already-governed internal act; it must never replace the requesting owner
  for a visibility decision.
- Knowledge Retrieval consumer authority and Memory Core evidence-resolution
  authority are separate authorizations — satisfying one does not satisfy
  the other.
- A `KnowledgeItem` reference grants no authority of its own, restated and
  made more specific in Section 6, below.
- If Authorization Purpose is adopted by a future Contract Design, a Purpose
  must be registered, active, and recognized by policy before it can
  authorize anything.
- Absent, inactive, unregistered, wrong, or mismatched Purpose must not fall
  through to a broader approval.
- Contract Design must decide whether conversational Purpose is mandatory,
  and how it reaches every relevant authorization layer.

---

## 6. Authority Boundaries

- **Act-level authorization occurs before persistence inspection.** No stage of the
  read flow may inspect, enumerate, or reason about candidate `KnowledgeItem`s,
  referenced Memory Core content, or any other persisted state before the calling
  principal's act-level authorization for the governed retrieval verb is established.
- **Content, match existence, counts, identifiers, and denial reasons must not leak
  unauthorized knowledge; explicit timing metadata and deliberately encoded timing
  signals must not either.** No stage of the read flow, and no error, denial,
  empty-result, or partial-result path through it, may allow an unauthorized caller
  to infer the existence, content, count, identity, or denial reason of knowledge it
  is not authorized to see, or to have a deliberate delay, count, or signal encode
  that same protected state. This restates, without weakening, the "evaluate before
  disclose" discipline `PermissionFilteredMemoryRetrieval` already establishes
  (Planning Review Section 6). Ordinary elapsed wall-clock latency is addressed
  separately, immediately below, and is not absolutely prohibited from varying.
- **Timing scope, narrowed by amendment.** Elapsed wall-clock latency can honestly
  vary between act denial, authorized-empty retrieval, candidate filtering, and
  Memory Core evidence resolution, because each performs a genuinely different
  amount of work — no in-process architecture this repository has built provides
  constant-time execution or artificial padding, and this Boundary Review does not
  authorise adding one. The absence of explicit timing metadata in a result does
  not, by itself, eliminate an observer's ability to measure how long a call took;
  that limitation is accepted and explicitly disclosed, never concealed. This
  programme must not claim resistance to active timing analysis, and Contract
  Design must not claim wall-clock latency is structurally indistinguishable across
  these paths. Contract Design must instead: prevent explicit timing metadata and
  deliberately encoded timing signals from crossing this boundary; disclose
  naturally variable execution latency honestly; and confine verification to
  proving the absence of explicit timing fields and deliberate timing encoding,
  never to proving constant-time behaviour from an ordinary elapsed-time threshold.
  Adding constant-time padding, batching, or other timing-channel mitigation is a
  distinct, separately governed decision, not authorised here.
- **Item-level visibility remains independently governed.** Passing act-level
  authorization for the retrieval verb does not itself authorize visibility of any
  specific `KnowledgeItem` or its referenced content. Item-level visibility is a
  separate, independently evaluated gate, not a consequence of act-level
  authorization.
- **Memory Core authorization is not inherited or bypassed merely because a
  `KnowledgeItem` references a record.** A `KnowledgeItem` referencing a Memory Core
  record confers no standing authorization to read that record. Any resolution of
  referenced content, wherever Contract Design places it in the flow, must
  independently pass Memory Core's own governed authorization for that content —
  exactly as `DefaultKnowledgeCandidateEvaluator.resolve()` already does for
  candidate evidence during promotion (Planning Review Section 6).
- **Evidence Intelligence receives no new authority.** Nothing this Boundary Review
  fixes, and nothing a future Contract Design may authorize within it, may widen
  `EvidenceIntelligenceInputResolver`'s own retrieval path beyond exactly as
  fail-closed as Gap #54 left it (Planning Review Section 6).
- **Reasoning Context and the model receive no reusable `MemoryRetrieval`
  capability.** Consistent with Section 3, above, neither Reasoning Context nor the
  model may receive, hold, or be able to reconstruct a `MemoryRetrieval`-shaped
  capability of their own. Any governed resolution of referenced content that
  Contract Design authorizes occurs on the Knowledge Memory side of the boundary,
  before a result crosses into Reasoning Context — never as a capability handed
  across that boundary for Reasoning Context or the model to invoke itself.

---

## 7. Lifecycle and Failure Boundaries

- Denied act-level retrieval causes no persistence inspection of any kind —
  the flow terminates before any `KnowledgeItem` or referenced content is
  examined.
- Denied item-level visibility omits the item from the result without
  disclosing its existence to the caller.
- Denied referenced evidence exposes neither its content nor any fact about
  whether a match against it existed.
- Missing, deleted, or unavailable evidence produces deterministic,
  fail-closed handling; the exact partial-result semantics are deferred to
  Contract Design, not fixed here.
- Existing retired and superseded `KnowledgeItem` lifecycle semantics remain
  unchanged unless a future Contract Design explicitly redesigns them.
- Empty authorized results and authorization denial must remain
  distinguishable internally, for audit and correctness, without exposing
  that distinction, or any unauthorized fact it implies, to an unauthorized
  external caller.
- Contract Design must decide fail-whole versus authorized-partial behaviour
  for the case where only some candidate items resolve.
- Model-provider failure after Reasoning Context assembly must not mutate
  Knowledge Memory state, and must not turn a transient retrieval result
  into persistence.
- This Boundary Review selects no user-facing failure wording; that remains
  for Contract Design and its successors.

---

## 8. Data Ownership

- **`KnowledgeItem` remains the authoritative promoted/evaluated record.**
  No boundary fixed here authorizes a second authoritative record of what
  has been promoted or how it was evaluated.
- **Memory Core remains authoritative for remembered proposition content and
  provenance.** No boundary fixed here authorizes any other subsystem to become an
  authoritative, or co-authoritative, source for remembered proposition content or
  its provenance.
- **`ReasoningContext` entries are task-scoped projections, not persistence.** A
  `ReasoningContext` entry rendered from a Knowledge result is a transient,
  per-assembly projection for model-prompt delivery. It is not a store, and nothing
  in this Boundary Review authorizes treating it as one — a `ReasoningContext`
  entry has no standing beyond the assembly that produced it.
- **No second durable or indexed content source is authorized by this Boundary
  Review.** This restates, without weakening, Planning Review Section 6's own
  recorded risk for the "separate indexed projection" alternative. Contract Design
  may examine that alternative only to determine whether it can satisfy the
  no-duplication, single-source-of-truth boundary this bullet fixes — not as an
  equally pre-authorized implementation option alongside the other two candidates.
  If it cannot satisfy that boundary, Contract Design must reject it. Contract
  Design cannot waive this boundary itself: only an explicit upstream governance
  reopening, not this Boundary Review and not Contract Design acting alone, can
  authorize a second durable or indexed content source.
- **Query-time dereferencing remains a candidate, not a selected design.** Consistent with
  Planning Review Section 6, this Boundary Review does not select query-time dereferencing of
  `KnowledgeItem.evidenceReference`, content representation added to `KnowledgeItem`, or a
  separate indexed projection. Query-time dereferencing and adding content representation to
  `KnowledgeItem` remain candidate designs for Contract Design to evaluate, subject to the fixed
  boundaries and the required proofs the Planning Review already lists (Section 6: no duplicate
  source of truth; no raw Memory Core bypass; permission filtering before disclosure; no
  Evidence Intelligence authority widening; deterministic handling of missing, denied, deleted,
  or unavailable evidence). A separate indexed projection may be examined only for boundary
  compatibility: it is not an available design choice unless it satisfies the no-duplication and
  single-source-of-truth boundary (Section 8, above); if it cannot satisfy that boundary, it
  must be rejected or returned for explicit upstream governance reopening.

---

## 9. Retrieval Surfaces

- **The production Reasoning Context path must cut over from the legacy
  `KnowledgeSource`/`KnowledgeStore` feed.** This restates the already-governed
  direction confirmed by
  `docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_GOVERNANCE_REVIEW.md` Section 10
  and the Planning Review (Section 7): Reasoning Context must cut over to
  `KnowledgeRetrieval`, the surface Programme 3 built specifically for Programme 4
  to adopt.
- **Two active production knowledge sources are not permitted.** Contract Design
  and its successor Implementation Plan must retire the legacy `memorySource:
  KnowledgeSource` path as part of the cutover, not leave it running in parallel
  with `KnowledgeRetrieval`. This restates Planning Review Section 7's own
  explicit refusal to authorise preserving two active knowledge sources in
  production.
- **Contract Design must decide adapter/interface mechanics.** Whether
  `DefaultReasoningContextAssembler`'s own constructor signature changes directly,
  whether a translation layer is introduced, and how a `KnowledgeRetrievalResult`
  is rendered into `ReasoningContext` entries are not fixed by this Boundary
  Review and are left entirely to Contract Design, exactly as Planning Review
  Section 7 already states.
- **Conversation History and World Model inputs remain unchanged and out of
  scope.** `WorldModelSource` and `ConversationHistorySource` inputs to
  `DefaultReasoningContextAssembler` are not part of the boundary this Boundary
  Review fixes or that its successor Contract Design addresses, restating
  Planning Review Section 7's own final paragraph without alteration.

---

## 10. Explicit Scope

**In scope for the successor Contract Design:**

- the content-discoverability contract;
- the Knowledge Retrieval to Reasoning Context cutover;
- principal and correlation propagation;
- authorization non-widening;
- safe Knowledge-result-to-`ReasoningContext` projection;
- legacy knowledge-feed retirement;
- composition, regression, negative, and same-runtime verification seams.

**Explicitly excluded:**

- restart durability;
- embeddings and semantic search;
- databases, remote services, or new indexes, unless an upstream governance
  document explicitly reopens the no-duplication boundary (Section 8,
  above);
- Representation Engine changes;
- World Model changes;
- Conversation History changes;
- Evidence Intelligence capability expansion;
- Remember/promotion pipeline changes, unless Contract Design proves an
  unavoidable dependency and returns for explicit scope approval;
- user-facing explanation design;
- broader Programme 4 propositional-integrity and burden-of-justification
  work.

This restates and sharpens, without narrowing, the scope the Planning Review
itself already records (Planning Review Section 9).

---

## 11. Contract Design Decision Register

Contract Design must resolve every one of the following eleven decisions
before Scope Lock may begin. This register is finite and closed — it is not
a starting checklist Contract Design may add to or narrow; a decision
outside this list that later proves necessary is a stop condition requiring
a return to this Boundary Review (Section 12, below), not an assumed
twelfth item.

1. lawful content representation and discovery;
2. authorization and dereference sequence;
3. `KnowledgeRetrieval`-to-`ReasoningContext` interface or adapter shape;
4. owner and system-principal propagation;
5. conversational Authorization Purpose and rule specificity;
6. safe result representation and evidential metadata;
7. missing, denied, deleted, unavailable, and partial-result semantics;
8. lifecycle filtering and supersession treatment;
9. result ordering, bounds, and side-channel controls;
10. legacy `KnowledgeSource` retirement mechanics;
11. test and live-verification seams.

---

## 12. Explicit Stop Conditions

- No Kotlin implementation may begin before an accepted Contract Design,
  Scope Lock, and Implementation Plan each exist.
- Halt if remembered content would be duplicated outside Memory Core.
- Halt if Reasoning Context or the model would gain raw Memory Core access.
- Halt if Evidence Intelligence authority would widen.
- Halt if authorization would occur after persistence or content disclosure.
- Halt if a broad approval could override absent, inactive, unregistered,
  wrong, or mismatched Purpose.
- Halt if two production knowledge sources would remain active.
- Halt if a frozen Programme 3 or Memory Core guarantee must be reopened
  without explicit upstream authorization.
- Halt if live verification cannot inspect real `ReasoningContext` entries
  or the real assembled model prompt.
- Halt if implementation introduces an intentional timing signal, a deliberate
  delay, or explicit protected-state timing metadata — never merely because
  authorized and denied paths naturally perform different amounts of work and
  therefore take different amounts of time.
- Halt if any review claims constant-time execution or resistance to timing
  analysis without a separately governed mitigation mechanism and its own,
  matching verification.

---

## 13. Explicit Non-Claims

- This document implements nothing — no Kotlin, no test, no configuration.
- It does not select final interfaces, request shapes, policy rules, or
  implementation ordering.
- It does not select the final content-representation design among the
  three candidates the Planning Review records (Section 8, above).
- It does not select the final retrieval-surface adapter/interface
  mechanics (Section 9, above).
- It does not decide the Authorization Purpose question the Planning Review
  leaves open (Planning Review Section 8); Section 5 and Section 6, above,
  fix only the principal, purpose, and authority boundaries such a decision
  must remain inside.
- It does not claim restart durability, in any form, restating Planning
  Review Section 9 and Section 11's own exclusion without alteration.
- It does not create a new gap or a new programme identity.
- It does not reopen Gap #54, which remains complete.
- It does not rename Evidence Intelligence or modify any of its source
  comments.
- It modifies no existing file in this repository.

---

## 14. Position in the Required Governance Sequence

```
Planning Review (accepted; docs/governance/KNOWLEDGE_DISCOVERABILITY_REASONING_CONTEXT_PLANNING_REVIEW.md)
        |
Boundary Review (this document)
        |
Contract Design -- resolves content discoverability (Section 8, above) and retrieval-
        surface adapter mechanics (Section 9, above) as its own two prerequisite
        questions, within the ownership (Section 3), information-flow (Section 4),
        principal-and-purpose (Section 5), authority (Section 6), lifecycle-and-failure
        (Section 7), and data-ownership (Section 8) boundaries fixed here; resolves
        Authorization Purpose (Planning Review Section 8) as a dependent, third
        question, within the principal-and-purpose (Section 5) and authority (Section 6)
        boundaries fixed here; and resolves every item of the Contract Design Decision
        Register (Section 11, above) before Scope Lock may begin
        |
Scope Lock
        |
Implementation Plan
        |
Independently reviewed implementation units
        |
Live verification
        |
Closure Determination
```

The stop conditions this document fixes (Section 12, above) and the stop conditions the
Planning Review already fixes (Planning Review Section 10) both apply unchanged to this
document's own successor stage: no implementation may begin before content
representation, retrieval-surface mechanics, and authorization non-widening are each
settled by an accepted Contract Design, and any unit must halt and return to Contract
Design if a selected design would require reopening a frozen Programme 3 or Memory Core
guarantee not explicitly authorised by that Contract Design.

---

## 15. Recommended Next Step

A Contract Design pass scoped specifically to the Contract Design Decision
Register (Section 11, above) and the three prerequisite questions the
Planning Review names (Section 6, Section 7, Section 8), resolved inside the
boundaries this document fixes — beginning from the candidate directions and
required proofs the Planning Review already records, not as pre-made
decisions. Neither is authorised or begun by this document.

```
KNOWLEDGE DISCOVERABILITY AND GOVERNED RETRIEVAL INTO REASONING CONTEXT
BOUNDARY REVIEW -- COMPLETE, PENDING CONTRACT DESIGN
```
