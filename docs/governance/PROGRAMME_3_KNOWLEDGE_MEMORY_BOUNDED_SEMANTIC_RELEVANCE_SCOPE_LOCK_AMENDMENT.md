**Status: Adopted.** This amendment, together with `PROGRAMME_3_KNOWLEDGE_MEMORY_CONTRACT_DESIGN_V2_BOUNDED_SEMANTIC_RELEVANCE_AMENDMENT.md` and `PROGRAMME_3_UNIT_9_7_BOUNDED_SEMANTIC_RELEVANCE_CONTRACT_AND_PERMISSION_SUCCESSOR.md`, has passed the review discipline this governance package requires: an Independent Constitutional Review of all three documents together returned VERDICT — REVISE BEFORE ACCEPTANCE, identifying three defects (a semantic-mechanism failure/fallback inconsistency, a local/operator-controlled-restriction consistency question, and ambiguous "fully and finally" pre-computation wording); a bounded defect-correction pass applied the minimum corresponding corrections across the three documents; and a subsequent Defect Confirmation Review returned VERDICT — ACCEPT, with no unresolved constitutional defect remaining. As with `docs/governance/PROGRAMME_3_UNIT_9_SEMANTIC_RELEVANCE_SCOPE_LOCK_REVISION_PROPOSAL.md`, this document's review history exists in this engagement's own conversation record rather than as separately committed `docs/reviews/` documents; a reader relying on this status line should treat that as this document's actual provenance rather than assume parallel review files exist on disk. This is a revision to `docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_SCOPE_LOCK.md` ("the Scope Lock"). The Scope Lock is not edited by this document — it remains frozen, unchanged, and in full force exactly as it exists today; this document is the governing instrument for the single, narrow qualification Section 13 states, exactly as CDR-008 already governs Memory Core Scope Lock's practical reach without editing its text. This document cites and relies upon `docs/decisions/CDR-008_MEMORY_CORE_DOWNSTREAM_RELEVANCE_BOUNDARY.md` (Status: Accepted. Canonical. Frozen.) and `docs/governance/PROGRAMME_3_UNIT_9_SEMANTIC_RELEVANCE_SCOPE_LOCK_REVISION_PROPOSAL.md` (Status: Adopted) as its exclusive design basis, without reopening or re-litigating either. No Kotlin is implemented, proposed, or changed by this document. **This document's adoption, together with its two sibling documents, makes it part of Programme 3's own governing architecture; it does not itself authorise implementation of Unit 9.7 — a separate, dedicated Unit 9.7 Implementation Plan, itself independently reviewed and adopted, is required before any code may be written.**

# Programme 3 — Knowledge Memory — Bounded Semantic Relevance Scope Lock Amendment

Programme: **Programme 3 — Knowledge Memory Scope Lock, Bounded Semantic Relevance Amendment.**

## 1. Capability Authorised

This amendment authorises exactly one capability: **Bounded Relevance Computation**, exactly as `PROGRAMME_3_UNIT_9_SEMANTIC_RELEVANCE_SCOPE_LOCK_REVISION_PROPOSAL.md` §8 defines it — a request-scoped, subordinate, replaceable computation that orders or reduces an already Parker-authorised, already-eligible, closed candidate set, and does nothing else. It authorises no library, model, product, or persistent architecture by name, and no capability broader than the one sentence above.

## 2. Problem Solved

Parker's canonical Knowledge Memory can hold a genuinely promoted, canonical proposition that a later, structurally-dissimilar paraphrase of the same request fails to recall, because `DefaultReasoningKnowledgeSource.recall()`'s only relevance mechanism today is case-insensitive substring matching (adopted Proposal §2, demonstrated against the real, running `ParkerRuntime` in commit `aadd596`). This amendment exists solely to remedy that demonstrated recall defect for genuine semantic paraphrases, and for no broader purpose.

## 3. Position Relative to Memory Core

This capability sits entirely downstream of Memory Core, inside Knowledge Retrieval, and operates only over content already obtained through Memory Core's own unmodified, authorised interface. `docs/decisions/CDR-008_MEMORY_CORE_DOWNSTREAM_RELEVANCE_BOUNDARY.md` (Accepted. Canonical. Frozen.) is the governing constitutional interpretation establishing that Memory Core Scope Lock's prohibition on ranked/scored/semantic retrieval binds Memory Core's own retrieval interface, operations, implementations, and authority — not a downstream, separately-governed component computing relevance over a candidate set Memory Core has already, through its own unmodified interface, made available. This amendment does not reopen that question, does not touch `docs/architecture/MEMORY_CORE_SCOPE_LOCK.md`, and creates no precedent-by-analogy for any other component (CDR-008, Constitutional Boundary).

## 4. What Remains Authoritative

Parker retains, unconditionally and without exception: canonical persistence and identity of every `KnowledgeItem`; candidate enumeration; lifecycle/status determination; evidential-state assignment (an existing disclosure field, never an eligibility gate — see adopted Proposal §6.1); every permission decision, in content and enforcement, exclusively through `PermissionEngine`; query construction; final `maximumResults` bounding; `SafeKnowledgeResultEntry` construction; and exclusive `ReasoningContext` rendering. Memory Core remains Parker's sole authoritative system of record; nothing this amendment authorises creates, or can become, a parallel or secondary source of canonical truth (CDR-008 Mandatory Invariant 12, carried forward).

## 5. What Semantic Relevance May Do

The mechanism this amendment authorises may, and may only: accept an authorised query's relevance text and a closed, Parker-supplied set of already-eligible candidates, each reduced to an opaque per-request identifier and the minimum normalised content necessary to assess relevance; and return an ordering or subset of those same identifiers. Nothing else is a permitted output (adopted Proposal §8).

## 6. What Semantic Relevance May Not Do

Candidate discovery under this amendment does not, and may never: establish truth; create, modify, or delete canonical knowledge; alter provenance; determine or influence a permission decision; determine or influence lifecycle/status or evidential-state classification; construct, in whole or in part, a `SafeKnowledgeResultEntry`; render, compose, or influence `ReasoningContext` text; persist beyond the single request that created it; expand, substitute into, or otherwise alter the candidate set Parker supplied; or redefine canonical retrieval semantics in any way. Every one of these is architecturally foreclosed, not merely discouraged (adopted Proposal §9, §15 Architectural Inability Requirement).

## 7. Permission Boundaries

`PermissionEngine`'s sole authority over every permission decision is untouched. The mechanism this amendment authorises runs strictly after `docs/governance/PROGRAMME_3_UNIT_9_PERMISSION_ENFORCEMENT_MECHANISM_CLARIFICATION.md` §8's frozen, nine-step evaluation order has produced a closed, already permission-approved candidate set, and Parker re-verifies permission and lifecycle/status, freshly, immediately before any disclosure — never relying solely on the pre-computation snapshot (adopted Proposal §13). Permission enforcement is fail-closed: any non-`APPROVED`/`APPROVED_WITH_CONFIRMATION` outcome, at either gate tier, forecloses the candidate before it is ever visible to the relevance mechanism.

## 8. Fail-Closed Expectations

Every failure mode — mechanism unavailable, timeout, malformed output, an unknown, unauthorised, altered, duplicate, or stale/cross-request identifier, a permission or lifecycle change during computation, or an attempt to write canonical persistence or enumerate beyond the supplied set — must propagate as a distinguishable, honestly-disclosed outcome, never silently coerced into an empty or partial result presented as complete. The full, binding fail-closed table is fixed at `PROGRAMME_3_UNIT_9_SEMANTIC_RELEVANCE_SCOPE_LOCK_REVISION_PROPOSAL.md` §15 and restated as Unit-9-tier contract text in `PROGRAMME_3_UNIT_9_7_BOUNDED_SEMANTIC_RELEVANCE_CONTRACT_AND_PERMISSION_SUCCESSOR.md`; this Scope Lock amendment incorporates both by reference rather than duplicating them a third time in independently-worded text.

## 9. Provenance and Identity Preservation

The relevance mechanism never sees, assigns, or discloses a candidate's permanent `KnowledgeId`, provenance reference, evidence reference, lifecycle history, or permission state (adopted Proposal §11). It identifies candidates solely by opaque, request-scoped, unlinkable tokens. Parker alone resolves every returned token back to its own canonical `KnowledgeItem` by direct lookup; a token that fails to resolve, or resolves outside the exact set Parker supplied, is an integrity fault, rejected outright — never silently substituted (adopted Proposal §14). No provenance is ever created, inferred, or attributed by the relevance mechanism itself.

## 10. Exclusions and Non-Goals

This amendment does not authorise: Models B or C (any persistent or shared-index architecture); remote or cloud-hosted relevance processing; a persistent semantic index of any kind; a parallel or secondary canonical store; semantic, ranked, or scored retrieval for Evidence Custodian, World Model, conversations, arbitrary documents, `PermissionEngine`, Memory Core, or any other Parker subsystem; or any production implementation by its own adoption. QMD and any embedding model referenced anywhere in the adopted Proposal's evidence sections remain experimental evidence only and are not, and do not become, a constitutional dependency of this amendment (adopted Proposal §4, §8). Consistent with Model A-Strict condition 13 (adopted Proposal §10), the mechanism this amendment authorises must be local, in-process, on-device, or otherwise entirely under Parker's own operator/owner control; no remote, cloud-hosted, or third-party-network-reachable relevance processor is authorised, and no candidate content or query text may cross any network boundary the operator does not control.

## 11. Relationship to CDR-008 and Model A-Strict

This amendment implements, without reopening or re-litigating, CDR-008's Decision A and the adopted Proposal's Model A-Strict architecture in full. It does not revisit the Memory Core / Knowledge Retrieval boundary question CDR-008 already settled, does not alter CDR-008's Decision, Rationale, or any of its twelve Mandatory Invariants, and does not weaken, broaden, or reinterpret any of Model A-Strict's fourteen conditions (adopted Proposal §10, §10.1). Where this amendment's own text and either source document could be read to diverge, the source document controls, and that divergence is itself a defect to be corrected before adoption, not a silent amendment of CDR-008 or the Proposal.

## 12. Explicit Prohibition on Unintended Architectural Expansion

No sentence in this amendment may be cited as authorising anything beyond Section 1's single capability. In particular: this amendment creates no general "relevance" or "ranking" responsibility for Knowledge Retrieval beyond the precise, zero-result-only fallback the adopted Proposal §16 fixes; no precedent-by-analogy for any other component's own Scope Lock; and no implicit authorisation of any future broadening (persistent indexing, remote processing, additional subsystems) without that broadening's own, separate, dedicated governance review. A future implementer or reviewer who reads this amendment as authorising more than Section 1 states has misread it.

## 13. Proposed Textual Amendment

*(Adopted text. The Scope Lock is not edited by this document — this document is the governing instrument, exactly as CDR-008 already governs Memory Core Scope Lock's practical reach without editing its text.)*

### 13.1 Proposed §4 row replacement

Existing row (superseded by the replacement below as of this amendment's adoption; the Scope Lock document itself is not edited):

> "Embeddings, vector search, or any semantic/similarity-based retrieval | Never designed anywhere in the frozen contracts; explicitly excluded at both the Memory Core layer (`MEMORY_CORE_SCOPE_LOCK.md` §4, §10) and the Knowledge Memory layer (Contract Design Version 2 §13). Knowledge Retrieval performs structural matching against caller-supplied criteria only."

Adopted replacement row, in force as of this amendment's adoption:

> "Embeddings, vector search, or any semantic/similarity-based retrieval, **except the single, narrowly bounded Bounded Relevance Computation capability this Scope Lock's own Section 11 authorises** | Explicitly excluded at both the Memory Core layer (`MEMORY_CORE_SCOPE_LOCK.md` §4, §10, unaffected by, and not reached by, this exception — see `CDR-008_MEMORY_CORE_DOWNSTREAM_RELEVANCE_BOUNDARY.md`) and, as a general matter, the Knowledge Memory layer (Contract Design Version 2 §13, similarly qualified only by that document's own Bounded Semantic Relevance Amendment). Knowledge Retrieval performs structural matching against caller-supplied criteria only, **except for the single, request-scoped, fallback-only relevance mechanism Section 11 bounds.**"

### 13.2 New §11 — Bounded Semantic Relevance

**Status.** This section is an adopted governance amendment, in force under this Scope Lock's own §10 "Out-of-Scope Change Policy," following adopted CDR-008 and the adopted Proposal. It is additive and narrowly qualifying only. It does not modify, reopen, or reinterpret Sections 1–10 except the single §4 row Section 13.1 replaces, and it does not touch, reach, or depend upon any revision to `MEMORY_CORE_SCOPE_LOCK.md`.

**11.1 Constitutional Purpose.** As stated in Section 2 of this amendment document, incorporated here in full.

**11.2 Authorised Capability.** Bounded Relevance Computation, under Model A-Strict only, as elaborated at the Unit 9 contract tier by `docs/governance/PROGRAMME_3_UNIT_9_7_BOUNDED_SEMANTIC_RELEVANCE_CONTRACT_AND_PERMISSION_SUCCESSOR.md`, once that document is itself independently reviewed and adopted. This section authorises no implementation by its own force.

**11.3 Preserved Boundaries.** Sections 4 through 9 of this amendment document, incorporated here in full: what remains authoritative, what semantic relevance may and may not do, permission boundaries, fail-closed expectations, and provenance/identity preservation.

**11.4 Non-Goals.** Section 10 of this amendment document, incorporated here in full.

**11.5 Constitutional Consistency Check.** This section is confined to Programme 3's own frozen scope decision (this Scope Lock and Contract Design V2's own corresponding amendment). It does not reach, and creates no precedent for, `MEMORY_CORE_SCOPE_LOCK.md`, Chapter 10/PermissionEngine's sole-authority rules (CDR-005's own higher threshold governs those), or any other component's own Scope Lock.

## 14. Adoption Record

This amendment is adopted. All three conditions this section previously fixed as preconditions are satisfied: (a) it was independently reviewed (VERDICT — REVISE BEFORE ACCEPTANCE, three defects identified), the identified defects were corrected, and a Defect Confirmation Review confirmed all identified defects were corrected (VERDICT — ACCEPT, no unresolved defect remaining); (b) the Programme owner has explicitly accepted it; and (c) `PROGRAMME_3_KNOWLEDGE_MEMORY_CONTRACT_DESIGN_V2_BOUNDED_SEMANTIC_RELEVANCE_AMENDMENT.md` and `PROGRAMME_3_UNIT_9_7_BOUNDED_SEMANTIC_RELEVANCE_CONTRACT_AND_PERMISSION_SUCCESSOR.md` are themselves independently reviewed and adopted, together with this document, as one governance set. This amendment alone does not authorise Bounded Relevance Computation to be implemented; all three documents together authorise the capability Section 1 states, and none of them, singly or together, authorises implementation — a separate, dedicated Unit 9.7 Implementation Plan, itself independently reviewed and adopted, is required before any code may be written.
