**Status: Adopted.** This is a sibling contract document, elaborating `docs/governance/PROGRAMME_3_UNIT_9_KNOWLEDGE_RETRIEVAL_CONTRACT_DESIGN.md` ("the Unit 9 Contract Design") and `docs/governance/PROGRAMME_3_UNIT_9_PERMISSION_ENFORCEMENT_MECHANISM_CLARIFICATION.md` ("the Permission Enforcement Mechanism Clarification") without amending either — exactly as the Unit 9 Contract Design itself elaborates Contract Design V2 "without touching a single word of it." Neither predecessor document is edited by this document. This document, together with `PROGRAMME_3_KNOWLEDGE_MEMORY_BOUNDED_SEMANTIC_RELEVANCE_SCOPE_LOCK_AMENDMENT.md` and `PROGRAMME_3_KNOWLEDGE_MEMORY_CONTRACT_DESIGN_V2_BOUNDED_SEMANTIC_RELEVANCE_AMENDMENT.md`, has passed the review discipline this governance package requires: an Independent Constitutional Review of all three documents together returned VERDICT — REVISE BEFORE ACCEPTANCE, identifying three defects (a semantic-mechanism failure/fallback inconsistency, a local/operator-controlled-restriction consistency question, and ambiguous "fully and finally" pre-computation wording); a bounded defect-correction pass applied the minimum corresponding corrections across the three documents; and a subsequent Defect Confirmation Review returned VERDICT — ACCEPT, with no unresolved constitutional defect remaining. As with `docs/governance/PROGRAMME_3_UNIT_9_SEMANTIC_RELEVANCE_SCOPE_LOCK_REVISION_PROPOSAL.md`, this document's review history exists in this engagement's own conversation record rather than as separately committed `docs/reviews/` documents; a reader relying on this status line should treat that as this document's actual provenance rather than assume parallel review files exist on disk. It cites and relies upon `docs/decisions/CDR-008_MEMORY_CORE_DOWNSTREAM_RELEVANCE_BOUNDARY.md` (Accepted. Canonical. Frozen.) and `docs/governance/PROGRAMME_3_UNIT_9_SEMANTIC_RELEVANCE_SCOPE_LOCK_REVISION_PROPOSAL.md` (Adopted), and is adopted together with, and only together with, `PROGRAMME_3_KNOWLEDGE_MEMORY_BOUNDED_SEMANTIC_RELEVANCE_SCOPE_LOCK_AMENDMENT.md` and `PROGRAMME_3_KNOWLEDGE_MEMORY_CONTRACT_DESIGN_V2_BOUNDED_SEMANTIC_RELEVANCE_AMENDMENT.md`. This document is governance, not implementation: it defines the bounded production boundary a later, separately reviewed implementation unit and implementation plan must satisfy — it does not itself implement Unit 9.7, write Kotlin, or authorise any code to be written. No Kotlin is implemented, proposed, or changed by this document. **This document's adoption, together with its two sibling documents, makes it part of Programme 3's own governing architecture; it does not itself authorise implementation of Unit 9.7 — a separate, dedicated Unit 9.7 Implementation Plan, itself independently reviewed and adopted, is required before any code may be written.**

# Programme 3 — Unit 9.7 — Bounded Semantic Relevance Contract and Permission Successor

Programme: **Programme 3 — Knowledge Memory, Unit 9.7 (Bounded Semantic Relevance Contract and Permission Successor).**

## 1. Unit Purpose

Unit 9.7 exists to define the bounded contract and permission-integration surface required to implement, in a later and separately reviewed implementation unit, the single capability `PROGRAMME_3_UNIT_9_SEMANTIC_RELEVANCE_SCOPE_LOCK_REVISION_PROPOSAL.md` describes: Bounded Relevance Computation, running only as a fallback when structural retrieval finds nothing, over a candidate set Parker has already made eligible. Unit 9.7 exists because Unit 9's own Contract Design (§2, §3) and the Permission Enforcement Mechanism Clarification's own frozen evaluation order do not, and are not meant to, silently expand to cover a capability outside their own original scope — a new, narrowly-scoped successor unit is the mechanism `PROGRAMME_3_UNIT_9_SEMANTIC_RELEVANCE_SCOPE_LOCK_REVISION_PROPOSAL.md` §19 itself anticipated ("a new, narrowly-scoped successor unit, e.g. 'Unit 9.7'").

## 2. Predecessor/Successor Relationship

Unit 9.7 is a **sibling, not a replacement**, to two predecessor documents, each remaining entirely unedited and in full, unweakened force:

- **The Unit 9 Contract Design** — Unit 9.7 elaborates one narrow, additional responsibility (Section 4, below) alongside it, exactly as the Unit 9 Contract Design itself elaborates Contract Design V2 without amending it. Unit 9's own §2 ("No scoring, weighting, relevance-ranking, or similarity-ranking of any kind is a Knowledge Retrieval responsibility") and §3 ("no embeddings, vector search, semantic similarity, or scored relevance ranking is designed, implied, or permitted anywhere in this document") both remain literally true of the Unit 9 Contract Design's own text; Unit 9.7's own later-in-time, expressly-cited adoption is what narrowly qualifies their practical reach, exactly as CDR-008 already qualifies Memory Core Scope Lock's practical reach without altering its text.
- **The Permission Enforcement Mechanism Clarification** — Unit 9.7 is a permission *successor* in the sense that it fixes exactly where Bounded Relevance Computation sits relative to that document's own frozen, nine-step evaluation order (Section 5, below); it does not amend, reorder, shorten, or introduce any new step, gate tier, or outcome variant into that order.

## 3. Authorised Production Boundary (Model A-Strict, Restated as Binding Contract Text)

The following is the complete authorised architecture for any future implementation of Unit 9.7 — nothing broader is authorised by this contract, regardless of engineering convenience:

1. Parker obtains canonical `KnowledgeItem`s from its own persistence, unchanged.
2. Parker performs all existing eligibility filtering (lifecycle/status), unchanged.
3. Parker performs all required permission checks (act-level and item-level), unchanged, in the exact frozen order the Permission Enforcement Mechanism Clarification §8 fixes.
4. Only the resulting authorised candidates' minimally necessary content may cross into the relevance mechanism (adopted Proposal §11).
5. The mechanism ranks/scores only those candidates.
6. No unauthorised candidate is embedded, indexed, persisted, or disclosed to the mechanism, at any point, even transiently.
7. The mechanism returns only opaque identifiers or rankings for Parker-supplied candidates — never content, never new identifiers.
8. Parker resolves those identifiers against canonical Knowledge Memory by direct lookup — never trusting any content the mechanism might additionally return.
9. Parker re-checks permission and current lifecycle eligibility, freshly, immediately before constructing any `SafeKnowledgeResultEntry`.
10. Semantic/index state is request-scoped and fully disposable; nothing survives past the single retrieval call that created it.
11. No persistent secondary Knowledge Memory index of any kind is authorised.
12. Failure of the mechanism fails closed: the failure is a distinguishable, honestly-propagated outcome (Unit 9 §9's "Implementation failure"), never silently converted into a successful-looking empty result. Because Bounded Relevance Computation may only run after structural matching has already completed successfully and returned exactly zero relevant candidates (adopted Proposal §16), a mechanism failure does not trigger a second execution of structural matching — the already-computed, successful, zero-result structural outcome that made the mechanism eligible to run simply stands as the final result. This is distinct from the mechanism itself running successfully and returning zero relevant candidates, which is a genuine Empty result (Unit 9 §9), not a failure. Never a stale or partial result presented as complete.
13. The mechanism must be local, in-process, on-device, or otherwise entirely under Parker's own operator/owner control. No remote, cloud-hosted, or third-party-network-reachable relevance processor is authorised; no candidate content or query text may cross any network boundary the operator does not control.
14. The mechanism's identity, version, and configuration are frozen, disclosed, retrieval-relevant state; upgrades are governed changes, never silent drift (adopted Proposal §10.1).

## 4. Required Contract Surface

### 4.1 Responsibilities

Bounded Semantic Relevance is responsible for exactly one capability, narrower than a general Knowledge Retrieval responsibility and available only under the conditions Section 3 and Section 5 fix:

- **Fallback relevance computation** — computing an ordering over an already-eligible, Parker-supplied, closed candidate set, strictly when the Unit 9 Contract Design's own structural matching (its §2, §8) has completed successfully and returned exactly zero relevant candidates (adopted Proposal §16).

This is the Unit 9 Contract Design's §2 "Ranking, if any" clause's own single, narrowly-defined exception: Unit 9.7 does not create a general ranking responsibility; it authorises one bounded, fallback-only computation, under Model A-Strict exclusively.

### 4.2 Non-Responsibilities

Bounded Semantic Relevance is explicitly not responsible for, and must never be implemented to perform, anything the Unit 9 Contract Design's own §3 already excludes, plus the following, specific to this contract (adopted Proposal §9, §17):

- Eligibility, permission, or lifecycle/status determination (Section 5, below).
- `SafeKnowledgeResultEntry` construction or `ReasoningContext` rendering — both remain the Unit 9 Contract Design's own, entirely unaltered, exclusive Parker responsibilities.
- Persistence, indexing beyond the single request, or any parallel canonical store.
- Remote or cloud-hosted processing (Section 3, condition 13).
- Any capability for Evidence Custodian, World Model, conversations, arbitrary documents, or any Parker subsystem other than Knowledge Retrieval.

## 5. Permission Integration

Bounded Semantic Relevance may run only after the Permission Enforcement Mechanism Clarification's frozen nine-step order has produced a closed, permission-approved, lifecycle-shaped candidate set and structural matching (that order's own step 5) has returned zero matches from it. This contract does not reorder, parallelise, or collapse any step of that frozen order; it adds no new step to it and instead consults it only after it — through its permission and structural-matching steps — has already run to completion for the given request. Immediately before any `SafeKnowledgeResultEntry` is constructed from a relevance-mechanism-returned identifier, Parker re-verifies permission and current lifecycle/status for that identifier, freshly, against current state — never relying solely on the pre-computation snapshot (adopted Proposal §13). This is the successor relationship named in Section 2: Unit 9.7 is where the permission-enforcement order's own frozen steps and Bounded Relevance Computation's own fallback trigger are jointly, precisely fixed relative to one another, so that no future implementer has to guess where one ends and the other begins.

## 6. Fail-Closed Behaviour

Stated as binding contract requirements, restated here in full so that Unit 9.7 alone is sufficient to implement against, without requiring simultaneous cross-referencing of a separate document mid-implementation:

| Failure | Constitutional requirement |
|---|---|
| Mechanism unavailable | Distinguishable, honestly-propagated outcome (Unit 9 §9's "Implementation failure"), reported as such rather than converted into a successful-looking empty result; the already-computed, successful, zero-result structural outcome that triggered fallback (adopted Proposal §16) stands as the final result, without re-executing structural matching — never silently omitted |
| Timeout | Treated identically to mechanism unavailable |
| Relevance computation fails | Same as above; never presented as though the mechanism ran successfully and found nothing |
| Malformed ranking result | Fail loudly (reject the whole result), never silently coerce to an empty or partial list |
| Mechanism returns candidate content instead of identifiers | Rejected as an integrity fault; content returned by the mechanism is never trusted, rendered, or substituted for Parker's own canonical content |
| Returned content differs from the candidate content Parker supplied | Rejected as an integrity fault, treated identically to an unauthorized candidate |
| Unknown or unauthorized returned identifier | Rejected as an integrity fault — never trusted merely because it resembles an authorized one |
| Duplicate identifier | De-duplicated by Parker before bounding; never consumes more than one of `maximumResults`' slots |
| Too many results returned | The mechanism's own limit is a performance hint only; Parker's own bounding step is authoritative regardless |
| Request-token collision or stale/cross-request token | Rejected as an integrity fault; a token's validity never outlives the single request that issued it |
| Permission or lifecycle/status change during computation | Caught by the mandatory post-computation re-verification (Section 5); an ineligible candidate is excluded, never disclosed |
| Canonical item deleted, or candidate cannot be re-resolved | Treated as an unresolvable identifier — excluded, not substituted |
| Mechanism attempts to write/mutate canonical persistence, or enumerate beyond the supplied set | Architecturally impossible, not merely prohibited by policy — the interface or process boundary given to the mechanism must make this structurally impossible, not merely instructed against |

Pre-computation authorisation (Section 3, condition 4) and post-computation re-verification (Section 5) are both mandatory and not alternatives to each other.

## 7. Canonical-Memory Authority

Memory Core, and Knowledge Memory's own canonical persistence downstream of it, remain Parker's sole authoritative system of record. Unit 9.7 does not create, and no implementation of it may become, a persistent parallel or secondary source of canonical truth (CDR-008 Mandatory Invariant 12). Every fact a caller ultimately receives is Parker's own canonical record, resolved fresh at disclosure time; the relevance mechanism's own output is never itself disclosed, rendered, or treated as a record of anything.

## 8. Semantic Candidate Status

A candidate surfaced by Bounded Relevance Computation is **discovery, not authority**. Specifically, and without exception, candidate discovery under this contract does not: establish truth; create knowledge; modify canonical memory; delete canonical memory; alter provenance; bypass permissions; determine final authority over any retrieval decision; or redefine canonical retrieval semantics. A "relevant" candidate is exactly as authoritative, and no more authoritative, than the exact same candidate would be if it had instead been found by structural substring matching — the relevance mechanism changes only *which* eligible candidates are surfaced when structural matching finds none, never *what standing* a surfaced candidate has once found. Authoritative records are, in every case, obtained fresh from Parker's own canonical store and subjected to the whole of Parker's own governance (permission, lifecycle, evidential-state disclosure) before any disclosure — never from the relevance mechanism's own content, and never on the relevance mechanism's own say-so.

## 9. Prohibited Behaviours

Consolidated from Sections 3–8, above, and from the adopted Proposal's own Explicit Non-Authorizations (§17) and this drafting package's cross-document consistency review: Unit 9.7 may never become or contribute to canonical memory, an alternative memory store, or a secondary source of truth; determine eligibility, permission, lifecycle/status, or evidential-state classification; construct `SafeKnowledgeResultEntry` or render `ReasoningContext`; persist beyond a single request; run on a candidate before that candidate has completed Pre-computation — successful admission to the closed, already-eligible candidate set under Section 3, condition 4, and Section 5, above — regardless of any later Pre-disclosure re-verification of that same candidate; run when structural matching found one or more results or itself failed; enumerate or request candidates beyond the closed set supplied; write or mutate canonical persistence; use a remote or cloud-hosted mechanism; or extend, by its own force or by analogy, to Evidence Custodian, World Model, conversations, arbitrary documents, `PermissionEngine`, or Memory Core.

## 10. Required Verification Properties

A later implementation plan and implementation unit for Unit 9.7 must be able to demonstrate, before completion is claimed, that the implementation satisfies each of the following — stated here as governance-level verification criteria, not as test code:

1. **Determinism.** Identical query, identical canonical state, and identical frozen mechanism identity/version/configuration produce an identical result, including identical ordering, on repeated invocation.
2. **Fallback precision.** The mechanism never runs when structural matching returns one or more results, and never runs when structural matching fails; it runs only on a genuine, successful, zero-result structural outcome.
3. **Fail-closed completeness.** Every row of Section 6's table produces its stated outcome under deliberate fault injection, with no silent degradation to a partial or stale result presented as complete.
4. **No authority transfer.** Every row of the Authority Matrix (Section 4 of the cross-document consistency review accompanying this package) is independently verifiable as unchanged from today's behaviour for every responsibility other than fallback relevance ordering itself.
5. **Architectural inability, not policy promise.** The mechanism's own interface or process boundary is inspectable and demonstrates it structurally cannot reach canonical persistence or enumerate beyond its supplied candidate set — not merely that it is instructed not to.
6. **Hostile-scenario foreclosure.** Each of the seventeen hostile-review scenarios this drafting package's own consistency review lists remains foreclosed against the concrete implementation, re-tested against real code rather than against text alone.
7. **Memory Core non-involvement.** The implementation demonstrably makes no new call, dependency, or behavioural change to any Memory Core file or interface.

## 11. Completion Criteria

Mirroring Unit 9.5's own precondition structure, Unit 9.7 is complete as a *governance* matter — eligible for a later implementation plan to begin — as of this adoption: (a) this document, together with `PROGRAMME_3_KNOWLEDGE_MEMORY_BOUNDED_SEMANTIC_RELEVANCE_SCOPE_LOCK_AMENDMENT.md` and `PROGRAMME_3_KNOWLEDGE_MEMORY_CONTRACT_DESIGN_V2_BOUNDED_SEMANTIC_RELEVANCE_AMENDMENT.md`, passed its own Independent Constitutional Review (VERDICT — REVISE BEFORE ACCEPTANCE, three defects identified), the identified defects were corrected, and a Defect Confirmation Review confirmed all identified defects were corrected (VERDICT — ACCEPT, no unresolved defect remaining); and (b) all three documents are now formally adopted together through Programme 3's own established review discipline. Unit 9.7 remains incomplete as an *implementation* matter until, separately and afterward: (c) a dedicated implementation plan, itself independently reviewed, is adopted; and (d) an implementation satisfies every Required Verification Property in Section 10. No stage may be skipped or merged into an earlier one; in particular, this governance completion (a)–(b) does not itself constitute, imply, or shorten implementation completion (c)–(d) — this adoption authorises no implementation.

## 12. Extensibility and Non-Goals

None. This contract authorises exactly the capability Section 4.1 names, under exactly the boundary Section 3 fixes. Any broader capability — Models B or C, a persistent index, remote processing, application to any other Parker subsystem — requires its own separate, dedicated governance and is not extensible from this contract by implementation discretion. This document does not write an implementation plan, and its own adoption does not authorise one to begin without the separate review Section 11(c) requires.

## 13. Constitutional Consistency Check

Consistent with: `CDR-008_MEMORY_CORE_DOWNSTREAM_RELEVANCE_BOUNDARY.md` (Memory Core unaffected, Section 7); the adopted Proposal (this contract restates, and does not diverge from, its own boundaries throughout); the Permission Enforcement Mechanism Clarification (frozen order untouched, Section 5); the Unit 9 Contract Design's own §8 determinism guarantee (extended, not weakened) and §9 five-outcome error model (this contract's own failure cases are sub-cases of "Implementation failure," never a sixth category). No blocking contradiction identified against any of the four.
