# Evidence Intelligence — Scope Lock

## Status

Programme: **Evidence Intelligence — Scope Lock.**
Phase: **Final governance document before Implementation Plan.** No
Kotlin is implemented, proposed as a diff, or changed by this document.
No API, database schema, hashing algorithm, or storage technology is
specified, invented, or implied. No new interface, repository, service,
or public type is introduced anywhere below. Neither `src/` nor
`tests/` is touched. Nothing is staged, committed, or pushed.

**Ratification status: Accepted. Canonical. Implementation Frozen.**
`docs/architecture/EVIDENCE_INTELLIGENCE_CONTRACT_DESIGN.md`
("the Contract Design") has already completed that review and is
Accepted, Canonical, and — per its own closing status — Ready for Scope
Lock. This document is the next, separate governance stage that
authorisation permitted, and its own acceptance — a distinct event from
the Contract Design's own — has now occurred. See §12 (Scope Lock
Status) for the confirmed terminal status this document now holds.

**Correction applied.** The two questions the Contract Design (§6, §12)
explicitly assigned to Scope Lock — which mechanism performs acceptance
orchestration, and whether invoking Evidence Intelligence is itself a
distinct permission-relevant domain act — are decided in this version
(§6, §7, §9, §11), not deferred to the Implementation Plan. An earlier
draft of this document deferred both; the Contract Design does not
permit that, since it assigned both to this governance stage by name.

### Amendment 2 — OCR Mechanism Dependency (Mirrored)

This document is amended by Amendment 2, mirroring the Contract Design's
own Amendment 2 exactly, as this document's own §4 requires ("Exactly
which public contracts may be depended upon is frozen at precisely the
Contract Design's own §12 table — no more, no less"): the identical
fourth dependency row the Contract Design's own §12 now carries is added
below (§4). No other paragraph in this document is altered by it.
Amendment 2's own scope was fixed in advance by
`docs/reviews/EVIDENCE_INTELLIGENCE_CONTRACT_DESIGN_OCR_MECHANISM_AMENDMENT_PROPOSAL.md`.

**This document is binding, now that it has been accepted.** It does not redesign, and
does not revisit, any constitutional decision already made.
`docs/decisions/CDR-007_CONSTITUTIONAL_CLASSIFICATION_OF_EVIDENCE_INTELLIGENCE.md`
("CDR-007") and the Contract Design are frozen, normative inputs — this
document does not reopen Evidence Intelligence's classification, its
constitutional boundary, its four permitted output categories, its
ownership-transfer rule, its dependency model, or any other decision
those two documents already settled. Its own purpose is narrower and
final: fix, without ambiguity, exactly what Evidence Intelligence's
first implementation builds, and exactly what it does not. Every
capability considered below is either within the frozen scope or
explicitly excluded from it. There is no third category.

**Scope Lock Principle.** Evidence Intelligence's first implementation
shall perform analytical processing of already-governed evidence and
Memory Core records, and produce candidate propositions, comparisons,
and derivative artefacts for acceptance elsewhere — nothing more. It is
not intended to become a second Evidence Custodian, a second Memory
Core, a second Knowledge Memory, a truth authority, or an
acceptance-orchestration mechanism in its own right. Where a candidate
capability is plausible, useful, or eventually necessary but not
required to satisfy that one sentence, it is **OUT OF SCOPE** — the
burden of proof favours exclusion, not inclusion, throughout this
document.

---

## 1. Purpose

**Implementation objective.** Freeze the implementation boundaries for
Evidence Intelligence, exactly as authorised by the Contract Design, so
that subsequent implementation work cannot expand, reinterpret, or
weaken what the Contract Design already fixed. This Scope Lock is
**implementation-governing, not implementation-describing**: it chooses
no storage technology, no Kotlin type, no API, no database schema, and
no repository layout. It fixes exactly which of the Contract Design's
already-authorised responsibilities the first implementation builds
(§2), exactly which it must never build (§3), exactly which existing
public contracts it may depend upon (§4), exactly who owns what (§5),
exactly what order governed steps occur in (§6), exactly where Evidence
Intelligence's own implementation stops (§7), and exactly which
behavioural invariants (§8) and verification requirements (§9) a future
Implementation Plan must satisfy.

This document governs Evidence Intelligence's first implementation
only. It is not itself a Scope Lock, contract change, or implementation
plan for Evidence Custodian, Memory Core, Knowledge Memory, or the
Reasoning Provider — each remains governed exclusively by its own,
separate, already-frozen documents, unmodified by anything below.

---

## 2. Scope

Exactly what this implementation includes — each entry already
authorised by the Contract Design; none introduced here for the first
time:

| In scope | Basis |
| --- | --- |
| Analytical processing of one or more `EvidenceArtifactId`-referenced evidence artefacts and/or Memory-Core-addressable records: comparison, extraction, transcription, translation, summarisation, contradiction detection, chronology/timeline construction, issue/entity mapping | Contract Design §1 ("Analytical processing of governed evidence") |
| Orchestrating zero or more existing `ReasoningProvider` implementations as an internal analytical mechanism | Contract Design §1 ("Orchestrating existing Reasoning Providers"); §12 |
| Producing transient working output not submitted for acceptance | Contract Design §1 ("Producing transient working output"); §5 |
| Producing candidate derivative artefacts (`CandidateEvidenceArtifact`) — never accepting, holding, storing, or protecting them | Contract Design §1 ("Producing candidate derivative artefacts"); §5 |
| Producing candidate Memory Core records (`CandidateAssertion`, `CandidateRelationship`, including `SUPPORTS`/`CONTRADICTS`) — never registering them | Contract Design §1 ("Producing candidate Memory Core records"); §5 |
| Producing Knowledge Candidates referencing already-accepted Memory Core evidence — never submitting them | Contract Design §1 ("Producing Knowledge Candidates"); §5 |
| Disclosing, never suppressing, contradictory evidence encountered during analysis | Contract Design §1 ("Preserving contradictory evidence") |
| Maintaining traceability at claim level, not merely output level, for every material analytical claim in a `TransientOutput` | Contract Design §1 ("Maintaining traceability"); §5, §8 |
| Distinguishing extracted content, observed content, inferred analytical conclusions, and model-generated explanatory language within any analytical output | Contract Design §5 |
| Recognising the distinct evidential meanings of verbatim transcription, normalised transcription, and inferred reconstruction, where a `CandidateEvidenceArtifact` results from transcription | Contract Design §5 |
| Computing intermediate analytical confidence figures, held only in transient output, never in a durable field | Contract Design §9 |
| Returning zero, one, or many `EvidenceAnalysisResult` values per invocation, including legitimate partial completion where some referenced inputs could be analysed and others could not | Contract Design §5, §11 |

No responsibility beyond this table is in scope.

---

## 3. Explicit Exclusions

This section is deliberately unusually explicit. No excluded capability
below may be introduced by a future implementation without a new or
amended governance decision at the appropriate tier — a Contract Design
amendment, or a new or amended Constitutional Decision Record, as the
change warrants (mirroring §4's identical discipline for a dependency
discovered to be genuinely needed).

| Excluded capability | Reason |
| --- | --- |
| Evidence Custodian redesign, of any kind | Contract Design §2 ("Store evidence," "Accept evidence," "Delete evidence"); §12, §13 — every Custodian contract is reused unmodified |
| Memory Core redesign, of any kind | Contract Design §2 ("Register Memory Core records," "Modify Memory Core"); §8, §12, §13 — every Memory Core contract is reused unmodified |
| Knowledge Memory redesign, of any kind | Contract Design §2 ("Promote Knowledge," "Submit Knowledge Candidates," "Modify Knowledge Memory"); §13 |
| Provenance redesign, a new provenance type, or mirrored/duplicate provenance | Contract Design §2 ("Own provenance"); §8 — Evidence Intelligence "creates no independent provenance model," at claim level or otherwise |
| OCR, transcription, translation, or any other analysis kind's own internal algorithm | Contract Design, Out of Scope ("any specific analysis kind's own internal algorithm") |
| A document ingestion pipeline, or any acceptance of raw, uncustodied evidence bytes | Contract Design §4 ("Never accepted as input, by design") |
| A user interface of any kind | No interface, output, or responsibility anywhere in the Contract Design is presentation-shaped; §10 defines exactly one request-in/results-out operation |
| Android or any client/mobile-layer work | Not named, authorised, or implied anywhere in the Contract Design; entirely outside CDR-007's own subsystem classification |
| Planning, or any dependency on Planner Runtime | Not named in Contract Design §12's dependency table; that table (four rows, following Amendment 2) remains exhaustive |
| Execution, or any dependency on Execution Pipeline | Not named in Contract Design §12's dependency table; "Orchestrate downstream acceptance" is an explicit non-responsibility (§2) |
| Autonomous or self-initiated analysis | Contract Design §10: the one operation Evidence Intelligence exposes takes an explicit, caller-supplied `EvidenceAnalysisRequest` (§4) and returns; nothing in the Contract Design authorises Evidence Intelligence to decide, on its own initiative, what or when to analyse |
| Agent or Agent Runtime changes | Not named in Contract Design §12's dependency table; no relationship of any kind is described anywhere |
| Repository restructuring, a new source directory, or a new module boundary | Contract Design §13 (Repository Reuse) enumerates exactly what is reused, extended, and not reused — all within existing contract locations |
| Acceptance orchestration, performed by Evidence Intelligence itself | Contract Design §1, §2, §6, §12, §14 — "Evidence Intelligence performs only the first" (producing); acceptance is "a separate responsibility this document does not assign to Evidence Intelligence" |
| A new evidence store, memory system, comparison engine, or promotion system of Evidence Intelligence's own | Contract Design §13 ("Not adopted, and explicitly rejected as unnecessary"), restating CDR-007's own rejection |
| A Reasoning Provider abstraction specific to Evidence Intelligence | Contract Design §13; Evidence Intelligence orchestrates the existing `ReasoningProvider` contract unmodified |
| An `EvidenceIntelligenceRegistry` or similar discovery mechanism | Contract Design §13, mirroring `ReasoningProviderRegistry`'s own already-excluded precedent |
| A caller-declared confidence or evidential-state value, accepted as input | Contract Design §4 ("Never accepted as input, by design") |
| Any durable confidence field population (`CandidateAssertion.confidence` or any other) | Contract Design §2 ("Populate durable confidence"); §9 |
| Any `EvidentialState` assignment, or dependency reaching one | Contract Design §2 ("Assign evidential state"); §12 (`EvidentialState` is one of the eight named zero-dependency items) |
| Any Knowledge promotion, revision, retirement, or restoration act | Contract Design §2 ("Promote Knowledge"); §12 |
| Any dependency, at any depth, on `OwnerEvidenceDeletionAuthority`, `EvidenceArtifactStorage.delete`, or `EvidenceDeletionAudit` | Contract Design §2 ("Delete evidence"); §12, §15 |
| Any dependency on `EvidenceCustodian.accept`, `MemoryCore`'s public write interface, or Knowledge Memory's Knowledge Submission interface | Contract Design §12 ("Evidence Intelligence holds no dependency on any acceptance interface") |
| A Permission Engine dependency of Evidence Intelligence's own | Contract Design §12 ("Evidence Intelligence holds no dependency on the Permission Engine of its own"); §2 ("Authorise its own actions") |
| A bespoke comparison-result type, or a comparison mechanism not reconciled with Model B | Contract Design §7 |
| A fifth `EvidenceAnalysisResult` category, a new failure-result taxonomy, a partial-result wrapper, an alternate output taxonomy, or any new sealed or closed shape that competes with, expands, or stands beside the frozen four-category `EvidenceAnalysisResult` taxonomy | Contract Design §11 — restated here explicitly: partial completion (§2 above) is represented through the existing non-empty result list and existing failure-signalling mechanism, never a new type |

**Clarification: this exclusion does not prohibit the single selector
type the Contract Design now authorises (§3 of the Contract Design, as
amended; §4 below).** That type is not an output category — it competes
with, expands, or stands beside nothing `EvidenceAnalysisResult` itself
classifies; it is not a failure taxonomy — it signals no implementation-
level fault of any kind; it is not a partial-result mechanism — it
carries no notion of partial or incomplete analysis. It exists solely as
the payload selector inside the already-authorised "Candidate record
produced" category, choosing between the two existing candidate kinds
that category's own content requirement already names. The exclusion
above remains binding, without exception, against every alternate
output taxonomy, failure taxonomy, and partial-result mechanism it
already named; this clarification narrows nothing about what it
excludes, only confirms what it was never intended to reach.

---

## 4. Dependency Freeze

**Exactly which public contracts may be depended upon is frozen at
precisely the Contract Design's own §12 table — no more, no less.**

**Reused, unmodified (Evidence Intelligence → the named subsystem):**

| Dependency | Direction | Purpose | Already exists in |
| --- | --- | --- | --- |
| `EvidenceCustodian.retrieve` | Evidence Intelligence → Evidence Custodian | Read-only access to custodied originals and derivatives | Evidence Artifact Contract Design |
| `MemoryRetrieval` | Evidence Intelligence → Memory Core | Read-only access to registered Memory Core records | Memory Core Contract Design |
| `ReasoningProvider` (zero or more) | Evidence Intelligence → Reasoning Provider(s) | Internal analytical mechanism, orchestrated, never itself | Reasoning Provider Contract Design |
| An OCR mechanism (zero or one) | Evidence Intelligence → OCR mechanism | Internal analytical mechanism, orchestrated, never itself, invoked only when an analysis's own input requires image-to-text interpretation (**Amendment 2**); never a truth authority, never a constitutional classifier, and never itself capable of assigning `EvidentialState` or determining what its own output means | Not yet governed — authorised in principle by Amendment 2; its own concrete contract remains a future, separate Contract Design's responsibility |

**New contracts already authorised by the Contract Design (owned by
Evidence Intelligence, §3):**

- **`EvidenceAnalysisRequest`** — the single input shape (§4 of the
  Contract Design).
- **`EvidenceAnalysisResult`** — the sealed output shape, exactly four
  variants (§5 of the Contract Design).
- **A closed, two-case selector type** — carried solely as the payload
  of `EvidenceAnalysisResult`'s own "Candidate record produced"
  category (§3 of the Contract Design, as amended). Frozen, without
  exception:
  - owned exclusively by Evidence Intelligence;
  - behaviour-free — no operation beyond the ordinary structural
    operations any plain value already has;
  - closed to exactly two cases — an existing, unmodified
    `CandidateAssertion`, or an existing, unmodified
    `CandidateRelationship` — never a third case without a further
    Contract Design amendment;
  - owns no data beyond the one selected candidate value;
  - introduces no fifth `EvidenceAnalysisResult` category —
    `EvidenceAnalysisResult` (immediately above) remains exactly four
    variants;
  - creates no acceptance, persistence, retrieval, reasoning,
    confidence, evidential-state, provenance, or ownership authority
    of its own;
  - does not modify `CandidateAssertion` or `CandidateRelationship` —
    both remain reused, unmodified, exactly as this section's own
    reused-dependency table above already fixes;
  - is not a generic union abstraction, reusable for any other pair of
    types;
  - may be used only as the payload of the already-authorised
    "Candidate record produced" result: no other output category may
    use it, and no subsystem may adopt, produce, extend, or depend
    upon it for any purpose unrelated to that one category. Legitimate
    `EvidenceAnalysisResult` consumers — including the separate
    acceptance coordinator (§6 below), which already dispatches each
    "Candidate record produced" value to Memory Core's own write
    interface — may inspect this selector solely to determine which
    existing candidate it carries, in order to dispatch that candidate
    through the frozen acceptance sequence; that consumption grants no
    ownership, no authority, no extension right, and no independent
    dependency entitlement over the selector itself;
  - creates no independent dependency entitlement for any other
    subsystem — its public visibility confers no reusable role, no
    ownership interest, and no authority beyond this one, narrow
    purpose.

  No Kotlin name, package, method, or interface is assigned to this
  type by this document, exactly as none is assigned to the two types
  above.
- **`EvidenceIntelligence`** — the single public interface, one
  operation, request in, result list out (§10 of the Contract Design).

No fifth new public type or interface is authorised. These four are
the entire new public surface this implementation may create.

**Frozen, without exception: no additional public contract of any kind
may be introduced.** In particular, and restating Contract Design §12
verbatim in effect:

- No dependency on `EvidenceCustodian.accept`.
- No dependency on `MemoryCore`'s public write interface.
- No dependency on Knowledge Memory's Knowledge Submission interface.
- No dependency, at any depth, on `OwnerEvidenceDeletionAuthority`,
  `EvidenceArtifactStorage.delete`, or `EvidenceDeletionAudit`.
- No dependency, at any depth, on Knowledge Memory's own promotion,
  revision, retirement, or restoration mechanisms.
- No dependency, at any depth, on `EvidentialState`.
- **No dependency on the Permission Engine of Evidence Intelligence's
  own — unchanged by the invocation-gating decision this Scope Lock now
  freezes (§6, §7 below).** Three permission-relevant boundaries now
  surround Evidence Intelligence, and all three are enforced by
  whatever composes Evidence Intelligence into the running system —
  never by a Permission Engine reference Evidence Intelligence itself
  holds: invocation of `EvidenceIntelligence`'s own operation (newly
  frozen, §6 below); `EvidenceCustodian.retrieve`, already
  permission-gated at its own boundary; and `MemoryRetrieval`, gated by
  the composing caller exactly as Memory Core Contract Design §10
  already requires of every caller. Freezing the invocation gate does
  not alter, reopen, or contradict Contract Design §12's own statement
  — it applies CDR-005 Model C to recognise one further domain act the
  composing caller must gate, exactly as that caller already gates
  `MemoryRetrieval`; Evidence Intelligence's own code path gains no new
  reference of any kind by this decision.
- No dependency on the Evidence Intelligence acceptance coordinator
  (§6 below), and no dependency in the reverse direction either: the
  coordinator consumes `EvidenceAnalysisResult` values Evidence
  Intelligence has already returned; Evidence Intelligence never calls,
  references, or knows of the coordinator's existence.
- No new evidence-artefact, assertion, relationship, provenance,
  knowledge, evidential-classification, reasoning-invocation, or
  comparison-specific type of any kind (Contract Design §3's own closed
  list, restated here as frozen).

**The Evidence Intelligence acceptance coordinator's own dependency
shape is frozen separately, precisely because it is not part of
Evidence Intelligence (§6 below defines it in full):** it may depend
only on `EvidenceCustodian.accept`, `MemoryCore`'s public write
interface, Knowledge Memory's Knowledge Submission interface, and the
Permission Engine gate each of those three already enforces. It holds
no dependency, at any depth, on `OwnerEvidenceDeletionAuthority`,
`EvidenceArtifactStorage.delete`, `EvidenceDeletionAudit`, or any
Knowledge Memory promotion/revision/retirement/restoration mechanism —
the identical eight-item exclusion this section already fixes for
Evidence Intelligence itself applies to the coordinator without
exception.

A future implementation that discovers a genuine need beyond this list
does not resolve that need by adding a dependency here; it returns to
governance (a Contract Design amendment or a new Constitutional
Decision Record, as the need warrants), never by silent implementation
choice.

---

## 5. Ownership Freeze

**Ownership is frozen exactly as the Contract Design's §3 table and §5
ownership-transfer rule already fix it. No ownership change, no
duplicate authority, and no mirrored provenance is authorised anywhere
in this implementation.**

| Type | Owner | Frozen relationship |
| --- | --- | --- |
| `EvidenceAnalysisRequest` | Evidence Intelligence | Defines; owns permanently |
| `EvidenceAnalysisResult` | Evidence Intelligence | Defines; owns permanently |
| The "Candidate record produced" payload selector (§4, above) | Evidence Intelligence | Defines; owns permanently; behaviour-free; used only as the payload selector for the "Candidate record produced" category; no independent authority or reusable utility role |
| `EvidenceArtifactId`, `CandidateEvidenceArtifact` | Evidence Custodian | Reused, unmodified; ownership of a produced `CandidateEvidenceArtifact` transfers to Evidence Custodian on acceptance, never before, never retained after |
| `Assertion`, `CandidateAssertion`, `Relationship`, `CandidateRelationship` | Memory Core | Reused, unmodified; ownership of a produced candidate transfers to Memory Core on acceptance, never before, never retained after |
| `Provenance`, `CandidateProvenance` | Memory Core | Reused, unmodified; Evidence Intelligence never constructs a parallel or mirrored provenance mechanism, at claim level or otherwise (§8 of the Contract Design) |
| `KnowledgeCandidate`, `KnowledgeItem` | Knowledge Memory | `KnowledgeCandidate` reused (produced, never submitted); `KnowledgeItem` never touched by Evidence Intelligence's own model at all |
| `EvidentialState` | Knowledge Memory | Never referenced by Evidence Intelligence's own model at all |
| `ReasoningProvider`, `ReasoningProviderRequest`, `ReasoningProviderResponse`, `ReasoningContext` | Reasoning Provider Contract Design | Reused, unmodified, as an orchestrated dependency only |
| `PrincipalId` | Existing platform identifier | Reused, unmodified, for audit purposes only |

**Ownership transfers on acceptance, and only on acceptance** (Contract
Design §5, restated as a frozen invariant): Evidence Intelligence owns
the production of a candidate output up to the point of its acceptance
by the subsystem that governs it. Once accepted, constitutional
ownership belongs exclusively to that subsystem — never to Evidence
Intelligence, in any form, for any duration. No implementation may
grant Evidence Intelligence a residual claim, a reference-holding
privilege, a modification right, or a second, mirrored copy of an
artefact, record, or its provenance after acceptance.

**No duplicate authority is created by this implementation.** Every
authority Evidence Intelligence's output eventually becomes subject to
— custody, registration, promotion, evidential-state assignment,
deletion — remains exactly where the Contract Design, CDR-006, and
Programme 3 already placed it. This implementation creates no second
holder of any of these authorities, partial or otherwise.

---

## 6. Sequencing Freeze

**The orchestration order is frozen, including the two mechanisms this
correction completes. No implementation may reorder, parallelise past,
or collapse the steps below.**

0. **Permission Engine evaluates and, only on approval, authorises the
   "invoke Evidence Intelligence" domain act** — a distinct,
   permission-relevant act this Scope Lock now freezes as required
   (§7, §11 below; CDR-005 Model C, applied, not deferred). This
   evaluation is performed by whatever composes Evidence Intelligence
   into the running system, before `EvidenceIntelligence`'s own
   operation is called — never by a Permission Engine dependency
   Evidence Intelligence holds itself (§4 above). **Denial stops here:**
   no evidence is retrieved, no Memory Core record is read, and no
   `ReasoningProvider` is invoked.
1. Evidence Intelligence resolves its inputs — custodied evidence via
   `EvidenceCustodian.retrieve`, Memory Core records via
   `MemoryRetrieval` — both read-only, both already permission-gated
   where applicable (Contract Design §4, §12), and both independently
   of, and in addition to, step 0's own invocation gate. Step 0's
   evaluation neither replaces nor is replaced by either of these.
2. Evidence Intelligence performs analysis, optionally orchestrating
   one or more `ReasoningProvider` invocations internally (§1, §12).
3. Evidence Intelligence returns a list of `EvidenceAnalysisResult`
   values — zero, one, or many — including, where genuinely applicable,
   a legitimate partial-completion result (§5, §11). **Evidence
   Intelligence's own contract ends here** (§6, §14 of the Contract
   Design).
4. **The Evidence Intelligence acceptance coordinator** — a separate,
   composition-level mechanism this Scope Lock now freezes, completing
   the decision the Contract Design (§6) assigned here, not deferred to
   the Implementation Plan — consumes that list and invokes the
   appropriate existing acceptance interface for each candidate:
   `EvidenceCustodian.accept` for a `CandidateEvidenceArtifact`,
   `MemoryCore`'s write interface for a `CandidateAssertion`/
   `CandidateRelationship`, Knowledge Memory's Knowledge Submission
   interface for a `KnowledgeCandidate`. The coordinator's own frozen
   properties, binding on any implementation:
   - **it is not part of Evidence Intelligence** — a structurally
     separate mechanism, never a class implementing
     `EvidenceIntelligence`, never invoked by Evidence Intelligence,
     and never invoking Evidence Intelligence in return (§4 above);
   - **it owns no custody, truth, promotion, deletion, or
     evidential-state authority** — every one of those five remains
     exactly where the Contract Design, CDR-006, and Programme 3
     already placed it (§5 above); the coordinator holds none of them,
     partially or conditionally;
   - **it cannot bypass Permission Engine evaluation** — every
     acceptance call it makes passes through the same Permission
     Engine gate that interface's own existing contract already
     enforces for every other caller (Contract Design §6);
   - **it invokes only existing acceptance interfaces** —
     `EvidenceCustodian.accept`, `MemoryCore`'s public write interface,
     and Knowledge Memory's Knowledge Submission interface, each
     unmodified; it invokes no other interface, and no new acceptance
     interface is introduced for it to invoke instead;
   - **it must accept a proposed Memory Core record before
     constructing or submitting any `KnowledgeCandidate` that
     references it** — restated and enforced by step 5 below, without
     exception;
   - **it is a sequencing mechanism only** — it holds no capability
     beyond invoking the three already-governed interfaces above, in
     the order this section requires; it decides nothing about
     evidential weight, truth, or promotion, and it is not a new
     authority (Contract Design §6's own "not a new authority... holds
     no capability beyond sequencing calls" standard, applied here by
     name rather than left open).

   This freezes *which* mechanism performs acceptance orchestration —
   a separate, composition-level coordinator, mirroring the shape
   `EvidenceRegistrationCoordinator` already establishes elsewhere in
   this repository for sequencing two subsystems without either
   calling the other (Contract Design §6's own cited precedent). It
   does **not** define a Kotlin name, signature, constructor, or
   method for it — that remains Implementation Plan work (§10 below).
   **"Evidence Intelligence acceptance coordinator" is a descriptive
   architectural label only** — it prescribes no Kotlin class name, no
   public interface name, no method signature, no source-file name, and
   no other concrete implementation identifier; only the six frozen
   properties above and this sequencing responsibility are binding.
5. **A `KnowledgeCandidate` referencing a newly proposed Memory Core
   record may be constructed only after that record has been accepted
   and assigned a governed identifier** — never within the same result
   list that first proposes the record (Contract Design §5, §6,
   restated as a frozen, non-negotiable ordering rule). No
   implementation may collapse steps 4 and 5 into one simultaneous
   step referencing an identifier not yet assigned. This is the
   coordinator's own binding obligation, restated from step 4 above:
   it accepts the Memory Core record first, in full, before
   constructing or submitting any Knowledge Candidate naming it.

This sequence, including the coordinator's identity and the invocation
gate's placement at step 0, is frozen. A future Implementation Plan
chooses the coordinator's concrete Kotlin shape and the invocation
gate's exact `PermissionAction`/`ResourceType` names (§10 below) — it
does not choose whether either exists, and it does not reorder,
parallelise past, or collapse any step above.

---

## 7. Stop Conditions

**Evidence Intelligence's own implementation stops at the operation
defined in Contract Design §10: given an `EvidenceAnalysisRequest`,
return a list of `EvidenceAnalysisResult` values. Nothing beyond that
point is built by this implementation.**

What later, separate programmes perform instead:

- **Acceptance orchestration** — invoking `EvidenceCustodian.accept`,
  `MemoryCore`'s write interface, or Knowledge Memory's Knowledge
  Submission interface for a candidate Evidence Intelligence produced —
  is performed by a later, separate responsibility: **the Evidence
  Intelligence acceptance coordinator, frozen by name and by property
  in §6 above.** This Scope Lock, completing the decision the Contract
  Design (§6) assigned to it, does select the mechanism — a
  composition-level coordinator, never Evidence Intelligence itself.
  What remains for the Implementation Plan is narrower: the
  coordinator's own concrete Kotlin shape (§10 below), not whether it
  exists or what it may and may not do.
- **A dedicated Permission Engine proposal class for invoking
  `EvidenceIntelligence`'s own operation is required** — decided here,
  applying CDR-005's Model C self-certification, completing the
  decision the Contract Design (§12) assigned to this Scope Lock, not
  deferred to the Implementation Plan (§11 below records the
  self-certification itself). Every invocation of
  `EvidenceIntelligence`'s own operation requires Permission Engine
  evaluation of that dedicated proposal class before analysis begins
  (§6 above, step 0) — separate from, and never a replacement for, the
  existing gates already protecting `EvidenceCustodian.retrieve`,
  `MemoryRetrieval`, or any of the three downstream acceptance
  interfaces the coordinator calls. Denial stops the analysis before
  any evidence is retrieved or any `ReasoningProvider` is invoked. What
  remains for the Implementation Plan is narrower: the exact
  `PermissionAction` and `ResourceType` names (§10 below), not whether
  a dedicated proposal class is required.
- **Comparison reconciliation against Model B's nine guarantees**
  (Contract Design §7) — the actual check that any comparison
  capability Evidence Intelligence's implementation builds does not
  provide materially weaker guarantees than Memory Core's own adopted
  model — is performed at Implementation Plan / implementation-review
  time, not by this document.
- **Registration, promotion, evidential-state assignment, custody, and
  deletion** remain exactly where CDR-006 and Programme 3 already
  placed them, performed exclusively by Evidence Custodian, Memory
  Core, and Knowledge Memory, respectively — never by Evidence
  Intelligence, and never by whatever mechanism performs acceptance
  orchestration on Evidence Intelligence's behalf.

---

## 8. Implementation Invariants

The following must remain true of every implementation of this Scope
Lock, regardless of concrete design, and regardless of which
`analysisKind` value is in use (Contract Design §4's own "every
`analysisKind` value... remains fully bound" clarification):

- **Statelessness where required.** Evidence Intelligence holds no
  persistence responsibility of any kind, for transient or
  acceptance-destined output alike (Contract Design §2 "Persist
  outputs"; §14 "never stores").
- **No ownership mutation.** No implementation ever mutates, reassigns,
  or retains ownership of an artefact or record after acceptance (§5
  above; Contract Design §5).
- **No evidence promotion.** No implementation performs any part of
  Knowledge promotion, revision, retirement, or restoration (Contract
  Design §2 "Promote Knowledge"; §14 "never classifies
  constitutionally").
- **No provenance creation.** No implementation constructs a new,
  parallel, or mirrored provenance mechanism, at claim level or
  otherwise; every provenance reference uses Memory Core's existing,
  unmodified fields (Contract Design §8).
- **No knowledge creation.** No implementation constructs a
  `KnowledgeItem`, or performs any act that itself promotes a
  `KnowledgeCandidate` (Contract Design §2, §14).
- **No execution.** No implementation holds an Execution Pipeline
  dependency, and no implementation performs acceptance orchestration
  itself (Contract Design §1, §2, §6, §12).
- **No planning.** No implementation holds a Planner Runtime
  dependency, or any dependency capable of constructing or submitting a
  Task Proposal.
- **No Memory writes.** No implementation holds a dependency on
  `MemoryCore`'s public write interface, and no implementation ever
  invokes it (Contract Design §2 "Register Memory Core records"; §12).
- **No constitutional authority.** No implementation assigns an
  `EvidentialState`, determines truth, determines legal ownership, or
  holds a Permission Engine dependency of its own (Contract Design §2,
  §9, §12, §14).
- **No self-authorisation.** No implementation grants itself access to
  evidence it is not authorised to read, or bypasses any Permission
  Engine gate at any boundary it calls (Contract Design §2 "Authorise
  its own actions").
- **No caller-declared confidence or evidential-state acceptance.** No
  field on any type this implementation constructs accepts a
  caller-supplied confidence or evidential-state value (Contract Design
  §4 "Never accepted as input, by design").
- **No durable confidence population.** No implementation populates
  `CandidateAssertion.confidence`, or any other durable confidence
  field, from its own analytical confidence computation (Contract
  Design §2, §9).
- **No Reasoning Provider identity.** No implementation of
  `EvidenceIntelligence` itself implements `ReasoningProvider`, and no
  `ReasoningProvider` implementation retains a reference back to
  `EvidenceIntelligence` (Contract Design §15).
- **No self-initiated analysis.** Every analysis is triggered only by
  an explicit, caller-supplied `EvidenceAnalysisRequest` (§3 above;
  Contract Design §10).
- **No silent content blurring.** Extracted content, observed content,
  inferred analytical conclusions, and model-generated explanatory
  language remain distinguishable in any analytical output; verbatim
  transcription, normalised transcription, and inferred reconstruction
  are never silently conflated where a `CandidateEvidenceArtifact`
  results from transcription (Contract Design §5).
- **No contradiction suppression.** Contradictory evidence encountered
  during analysis is always disclosed, never silently resolved in
  favour of one account (Contract Design §1).
- **No fabricated failure signalling.** An empty `EvidenceAnalysisResult`
  list is never used to signal a genuine analytical failure, and
  partial completion is never silently collapsed into either total
  success or complete failure (Contract Design §11).

---

## 9. Verification Requirements

Preferring structural or architectural verification over
behavioural-assertion or source-text pattern matching wherever a
structural mechanism can prove the same invariant, restating and
freezing Contract Design §15 as binding on the Implementation Plan:

- **No dependency reachability.** No class implementing
  `EvidenceIntelligence`, and no type reachable from
  `EvidenceAnalysisRequest` or `EvidenceAnalysisResult`, holds a
  reference — directly, or through any constructor parameter, function
  parameter, or property, at any depth — to
  `OwnerEvidenceDeletionAuthority`, `EvidenceArtifactStorage`,
  `EvidenceDeletionAudit`, `EvidenceCustodian.accept`, `MemoryCore`'s
  public write interface, Knowledge Memory's Knowledge Submission
  interface, or any Knowledge Memory promotion/revision/retirement/
  restoration mechanism.
- **No Permission Engine reachability of Evidence Intelligence's own.**
  No class implementing `EvidenceIntelligence` holds a Permission
  Engine reference of its own, beyond what the two existing,
  already-gated read boundaries it calls already provide — unaffected
  by the invocation gate below, which is held and evaluated by the
  composing caller, never by Evidence Intelligence itself (§4, §6
  above).
- **Invocation gating.** Every call to `EvidenceIntelligence`'s own
  operation is preceded by a Permission Engine evaluation of the
  dedicated "invoke Evidence Intelligence" proposal class (§6 above,
  step 0; §7, §11 above); a denied evaluation results in no
  `EvidenceCustodian.retrieve` call, no `MemoryRetrieval` call, no
  `ReasoningProvider` invocation, and no `EvidenceAnalysisResult`
  production of any kind.
- **Acceptance coordinator boundary.** No class implementing
  `EvidenceIntelligence` is the same class as, or holds a reference to,
  the Evidence Intelligence acceptance coordinator, and no coordinator
  implementation holds a reference back to any class implementing
  `EvidenceIntelligence` (§4, §6 above). No coordinator implementation
  holds a reference — directly, or through any constructor parameter,
  function parameter, or property, at any depth — to
  `OwnerEvidenceDeletionAuthority`, `EvidenceArtifactStorage`,
  `EvidenceDeletionAudit`, or any Knowledge Memory
  promotion/revision/retirement/restoration mechanism; every acceptance
  call it makes is independently evaluated by the Permission Engine
  gate that acceptance interface already enforces for every other
  caller.
- **No durable confidence or evidential-state authority.** No type this
  implementation defines carries an `EvidentialState` field, and no
  code path by which Evidence Intelligence produces a
  `CandidateAssertion` populates that Assertion's `confidence` field.
- **Acceptance-path exclusivity.** Every `CandidateEvidenceArtifact`,
  `CandidateAssertion`, `CandidateRelationship`, and
  `KnowledgeCandidate` Evidence Intelligence produces reaches its
  accepting subsystem only through that subsystem's own existing,
  unmodified acceptance interface, invoked only by the Evidence
  Intelligence acceptance coordinator (§6 above) — never invoked by
  Evidence Intelligence itself, and never through any other alternate
  path.
- **Sequencing correctness.** No `KnowledgeCandidate` is ever
  constructed referencing a Memory Core record that has not yet been
  accepted and assigned a governed identifier; an implementation that
  constructs both within a single, pre-acceptance step is
  non-compliant.
- **Claim-level traceability.** Every `TransientOutput` carries, for
  each material analytical claim it states, at least one governed
  reference sufficient to resolve that specific claim back to the
  evidence or Memory Core record it draws upon — not merely one
  reference list for the output as a whole.
- **Content-type distinguishability.** Extracted content, observed
  content, inferred analytical conclusions, and model-generated
  explanatory language are structurally or textually distinguishable
  within any analytical output a concrete implementation produces.
- **Transcription-fidelity distinguishability.** Where a
  `CandidateEvidenceArtifact` results from transcription, whether it
  (or a portion of it) is verbatim, normalised, or an inferred
  reconstruction is discoverable, not merely asserted in prose
  documentation.
- **Ownership-transfer correctness.** No test, and no production code
  path, treats Evidence Intelligence as retaining any reference,
  modification right, or residual claim over an artefact or record
  after it has been accepted by Evidence Custodian, Memory Core, or
  Knowledge Memory.
- **Reasoning Provider orchestration, not identity.** No class
  implementing `EvidenceIntelligence` itself implements
  `ReasoningProvider`, and no `ReasoningProvider` implementation holds
  a reference back to `EvidenceIntelligence`.
- **Comparison reconciliation.** Any comparison capability implemented
  under Contract Design §7 is checked, at implementation time, against
  Model B's nine constitutional guarantees
  (`MEMORY_CORE_SCOPE_LOCK.md` §18), and any material divergence is
  disclosed and justified, not silently introduced.
- **Contradiction preservation.** Given two Memory Core Assertions
  connected, or connectable, by a genuine contradiction, an analysis of
  them never silently produces only a `SUPPORTS`-typed output for one
  side, and never omits the contradiction from any transient output
  describing them.
- **Failure signalling.** An implementation that cannot reach a
  confident analytical determination never returns an empty
  `EvidenceAnalysisResult` list as its signal for that failure; it
  signals failure by some other, disclosed means.
- **Partial-completion honesty.** An implementation that analyses some
  referenced inputs successfully while others cannot be retrieved or
  processed represents the successful portion as genuine
  `EvidenceAnalysisResult` values and signals the unsuccessful portion
  by the same disclosed failure-signalling means above — never by
  silently omitting the unanalysed inputs from any disclosure, and
  never by fabricating a result for them.

---

## 10. Out of Scope

This document does not authorise, and no future reader should treat it
as having authorised:

- any storage technology, file system, database, or object store;
- any hashing algorithm, integrity-verification scheme, or
  cryptographic method;
- any API, RPC contract, or wire format;
- any Kotlin interface, class, enum, or method signature;
- any database schema or persistence model;
- an Implementation Plan for Evidence Intelligence — the next, separate
  governance stage, not begun and not authorised to begin by this
  document itself;
- the specific mechanism by which `EvidenceAnalysisRequest`'s Memory
  Core references or Reasoning Provider invocations are assembled;
- **the Evidence Intelligence acceptance coordinator's own concrete
  Kotlin shape** — its class name, constructor, method signatures, and
  internal structure. **Not out of scope: *that* the coordinator
  exists, that it is separate from Evidence Intelligence, and every one
  of its six frozen properties (§6 above)** — all decided here, not
  deferred;
- **the exact `PermissionAction` and `ResourceType` names** for
  invoking `EvidenceIntelligence`'s own operation. **Not out of scope:
  *whether* a dedicated Permission Engine proposal class is required**
  — decided here, applying CDR-005 Model C (§7, §11 above), not
  deferred;
- any specific analysis kind's own internal algorithm (how comparison,
  OCR, translation, or summarisation is actually performed);
- any amendment to Memory Core's, Evidence Custodian's, or Knowledge
  Memory's own contract, schema, or interface — none is proposed, and
  none is required by anything in this document;
- any question of legal ownership, copyright, proprietary interest, or
  lawful possession of any artefact — permanently out of scope per
  CDR-006, unaffected here;
- a user interface, an Android client, autonomous or scheduled
  invocation, planning, execution, or agent behaviour of any kind (§3
  above);
- any repository restructuring, new source directory, new service, or
  new module boundary.

---

## 11. Design Rules Compliance

A direct self-check against every named governing authority, performed
once here rather than asserted without demonstration:

**Parker Constitution.** No capability this document freezes creates a
path around "cognition proposes, trust authorises, runtime executes."
Evidence Intelligence proposes candidate output only (§2 above); it
never authorises its own action and never executes acceptance itself
(§3, §8 above).

**Epistemic Integrity (Amendment No. 1).** Every invariant in §8 above
that touches truth, evidential state, confidence, or contradiction
traces directly to an Article the Contract Design already cites
(Article III — truth determination; Article IV and Article XVI —
contradiction disclosure; Article VIII/IX — provenance and
traceability; Article XV — evidential-state assignment). None is
reinterpreted, narrowed, or extended here.

**CDR-001 through CDR-007.** CDR-001–CDR-003's canonical comparison
model (Model B) is reused by reconciliation requirement only, never
re-derived (§7 above; Contract Design §7). CDR-004 is cited as
structural precedent only, not reopened. CDR-005's Model C is applied,
not deferred (self-certification below). CDR-006 is unmodified; the
Custodian boundary it establishes is fully preserved (§3, §4 above).
CDR-007 is the controlling classification for this entire document and
is not reopened anywhere above.

**CDR-005 Model C self-certification (performed here, completing the
decision the Contract Design (§12) assigned to this Scope Lock, not
deferred to the Implementation Plan).** Invoking `EvidenceIntelligence`'s
own operation is a genuine, disclosed, narrow domain act: at a
requester's own initiative, it triggers analytical processing of
governed evidence and Memory Core records — including, internally,
zero or more `ReasoningProvider` invocations — for a purpose distinct
from the two existing read boundaries (`EvidenceCustodian.retrieve`,
`MemoryRetrieval`) it calls internally. This mirrors the same
minimum-required, narrowly-granted `PermissionAction`/`ResourceType`
discipline this Programme's own existing composition root already
applies elsewhere (for example, gating Tool delivery and Agent Run
initiation as their own, separately named domain acts, each narrow in
what it grants and reachable by no other path). Leaving invocation
ungated would mean any caller able to construct an
`EvidenceAnalysisRequest` could trigger analysis — and every
`ReasoningProvider` invocation and evidence read that follows from it —
without any authorisation decision naming that act specifically,
relying only on the two narrower internal read gates to catch what an
invocation-level gate exists precisely to catch earlier. This
self-certification is not contested by the Contract Design, by
CDR-001 through CDR-007, or by any other document this Scope Lock's
own governing sources name; per CDR-005's own escalation rule, an
uncontested self-certification requires no further Constitutional
Decision Record. The exact `PermissionAction`/`ResourceType` names
remain Implementation Plan work (§6, §7, §10 above) — the
self-certification itself, that a dedicated proposal class is required
at all, is complete.

**Memory Core.** No dependency on `MemoryCore`'s write interface (§4,
§8 above); every Memory Core type this implementation may touch is
reused unmodified (§4, §5 above); Memory Core's own Scope Lock
(including its Model B guarantees, §18) is treated as controlling, not
revisited.

**Evidence Custodian.** No dependency on `EvidenceCustodian.accept`,
`OwnerEvidenceDeletionAuthority`, `EvidenceArtifactStorage.delete`, or
`EvidenceDeletionAudit`, at any depth (§3, §4 above); the Evidence
Custodian Scope Lock's own custody, immutability, and deletion
guarantees are unaffected and unaddressed by anything in this document.

**Knowledge Memory.** No dependency on Knowledge Memory's Knowledge
Submission interface or on any promotion/revision/retirement/
restoration mechanism (§4, §8 above); `KnowledgeCandidate` is produced,
never submitted; `KnowledgeItem` and `EvidentialState` are never
touched by Evidence Intelligence's own model (§5 above).

**Reasoning Provider.** Orchestrated as a pure, stateless, non-retaining
callee only (§4, §8, §9 above); the `ReasoningProvider` interface is
not broadened, extended, or reinterpreted; Evidence Intelligence is
never itself classified as one.

---

## 12. Scope Lock Status

This Scope Lock was presented, in full, for Independent Constitutional
Review, exactly as the Contract Design itself was, and has been
accepted through that review. It redesigns nothing, revisits no
constitutional decision, introduces no new capability, no new
interface, no new repository, no new service, no new storage design,
and no implementation detail. Every boundary it freezes traces to a
specific, named section of the accepted Contract Design. Both Scope
Lock decisions the Contract Design assigned to this document by name —
the acceptance orchestration mechanism (§6) and the Permission Engine
invocation question (§7, §11) — are complete, exercising discretion the
Contract Design itself delegated here, not adding to what it
authorised.

Following that review, this document's status is:

**Accepted.**
**Canonical.**
**Implementation Frozen.**

The next governance stage — an Evidence Intelligence Implementation
Plan — is now authorised, following this Scope Lock's own acceptance;
it is not begun by this document itself.

EVIDENCE INTELLIGENCE SCOPE LOCK — ACCEPTED — CANONICAL — IMPLEMENTATION
FROZEN

Confirmed: no Kotlin implemented; no API, schema, or storage technology
defined; no new interface, repository, service, or public type
introduced beyond the four the Contract Design already authorised
(§4 above); CDR-001 through CDR-007 not modified; the Contract Design,
Memory Core Contract Design and Scope Lock, Evidence Artifact Contract
Design and Scope Lock, Programme 3 Knowledge Memory Contract Design V2,
and Reasoning Provider Contract Design not modified; nothing staged;
nothing committed; nothing pushed; Evidence Intelligence Implementation
Plan not started.
