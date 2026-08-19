**Status: Adopted.** This is Amendment 9 to `docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_CONTRACT_DESIGN_V2.md` ("Contract Design V2"), continuing that document's own existing Amendment 1–8 numbering. This amendment, together with `PROGRAMME_3_KNOWLEDGE_MEMORY_BOUNDED_SEMANTIC_RELEVANCE_SCOPE_LOCK_AMENDMENT.md` and `PROGRAMME_3_UNIT_9_7_BOUNDED_SEMANTIC_RELEVANCE_CONTRACT_AND_PERMISSION_SUCCESSOR.md`, has passed the review discipline this governance package requires: an Independent Constitutional Review of all three documents together returned VERDICT — REVISE BEFORE ACCEPTANCE, identifying three defects (a semantic-mechanism failure/fallback inconsistency, a local/operator-controlled-restriction consistency question, and ambiguous "fully and finally" pre-computation wording); a bounded defect-correction pass applied the minimum corresponding corrections across the three documents; and a subsequent Defect Confirmation Review returned VERDICT — ACCEPT, with no unresolved constitutional defect remaining. As with `docs/governance/PROGRAMME_3_UNIT_9_SEMANTIC_RELEVANCE_SCOPE_LOCK_REVISION_PROPOSAL.md`, this document's review history exists in this engagement's own conversation record rather than as separately committed `docs/reviews/` documents; a reader relying on this status line should treat that as this document's actual provenance rather than assume parallel review files exist on disk. Contract Design V2 is not edited by this document — it remains frozen, unchanged, and in full force exactly as it exists today; this document is the governing instrument for the single §13 insertion below. This document cites and relies upon `docs/decisions/CDR-008_MEMORY_CORE_DOWNSTREAM_RELEVANCE_BOUNDARY.md` (Accepted. Canonical. Frozen.) and `docs/governance/PROGRAMME_3_UNIT_9_SEMANTIC_RELEVANCE_SCOPE_LOCK_REVISION_PROPOSAL.md` (Adopted), and is adopted together with, and only together with, `PROGRAMME_3_KNOWLEDGE_MEMORY_BOUNDED_SEMANTIC_RELEVANCE_SCOPE_LOCK_AMENDMENT.md` and `PROGRAMME_3_UNIT_9_7_BOUNDED_SEMANTIC_RELEVANCE_CONTRACT_AND_PERMISSION_SUCCESSOR.md`. This document establishes contractual architecture only; it invents no production API and implements nothing. No Kotlin is implemented, proposed, or changed by this document. **This document's adoption, together with its two sibling documents, makes it part of Programme 3's own governing architecture; it does not itself authorise implementation of Unit 9.7 — a separate, dedicated Unit 9.7 Implementation Plan, itself independently reviewed and adopted, is required before any code may be written.**

# Programme 3 — Knowledge Memory Contract Design V2 — Bounded Semantic Relevance Amendment (Amendment 9)

Programme: **Programme 3 — Knowledge Memory Contract Design, Version 2, Amendment 9.**

## 1. Purpose

This amendment establishes the minimum contractual concepts required to express Bounded Relevance Computation at Contract Design V2's own tier — request/input shape, candidate-result shape, canonical-identity handling, determinism, permission behaviour, and failure behaviour — without prematurely fixing any concrete Kotlin type, method signature, storage technology, or library. Every concept below is a contractual *requirement* a future implementation must satisfy, not an implementation itself.

## 2. Request/Input Concepts

A **Relevance Request** is the contractual concept covering what may cross into a subordinate relevance mechanism: the authorised query's relevance text, and a closed, Parker-supplied set of already-eligible candidates, each represented only by an opaque per-request identifier and the minimum normalised content necessary to assess relevance (adopted Proposal §8, §11). A Relevance Request is not a new public contract type with fixed fields; it is a contractual constraint on what any eventual type may contain — nothing more, and specifically not the candidate's permanent `KnowledgeId`, evidential state, provenance or evidence references, lifecycle history, permission information, or the requesting principal's identity, unless separately required and separately governed.

## 3. Candidate-Result Concepts

A **Relevance Result** is the contractual concept covering what a subordinate relevance mechanism may return: an ordering or subset of the exact per-request identifiers it was given, and nothing else — never new identifiers, never free-text content, never any assertion about permission, lifecycle, evidential state, or eligibility (adopted Proposal §8). A Relevance Result is not itself a `KnowledgeResult` or a `SafeKnowledgeResultEntry`; it is a strictly narrower, intermediate concept that Parker alone converts into either of those existing types, exactly as Section 4 requires.

## 4. Canonical Identity Preservation

Every identifier a Relevance Result contains must resolve, by Parker's own direct lookup, back to the exact canonical `KnowledgeItem` Parker originally supplied under that identifier — never to a competing or mechanism-supplied representation. An identifier that fails to resolve, or resolves outside the exact candidate set originally supplied for the current request, is an integrity fault, rejected and never silently substituted (adopted Proposal §14). This mirrors, and does not weaken, Contract Design V2's own existing "no duplicate sources of truth" Non-Functional Requirement (§7): a Relevance Result never becomes, and can never be treated as, an independent record of identity — it is exclusively a lens onto records Knowledge Memory already, separately holds.

## 5. Separation of Relevance Evidence from Canonical Authority

A relevance ordering or score is not evidential-state classification, is not a truth determination, and is not itself an act of canonical authority. Existing evidential-state classification (Article IV; Contract Design V2 §1, §4, §12) remains exclusively assigned by Knowledge Memory's own promotion or revision evaluation and is unaffected by, and never influenced by, any relevance computation this amendment authorises (adopted Proposal §6.1, §9). A Relevance Result carries no evidential, provenance, or lifecycle assertion of its own; every such property a caller ultimately sees continues to come exclusively from Parker's own canonical record, resolved fresh at disclosure time (Section 4, above).

## 6. Deterministic Boundary Expectations

Contract Design V2's own existing determinism guarantee (referenced at the Unit 9 contract tier, §8: "a given Knowledge Query, issued twice against unchanged Knowledge Memory state, must return an identical Knowledge Result both times, including identical ordering") is extended, without weakening, to cover the one new source of retrieval-relevant state Bounded Relevance Computation introduces: the relevance mechanism's own identity, version, and configuration. Identical query, identical canonical state, and identical frozen mechanism identity/version/configuration must together produce an identical result, including identical ordering, every time; a mechanism upgrade, replacement, or reconfiguration is itself a disclosed, governed change, never silent behavioural drift (adopted Proposal §10.1, incorporated here by reference in full). This amendment does not require bit-identical floating-point output across different hardware or numerical libraries; no existing Parker contract imposes that standard, and this amendment introduces no new one.

## 7. Permission Behaviour

Permission behaviour is entirely unchanged by this amendment. `PermissionEngine` remains the sole authority over every permission decision; the two-tier act-level/item-level gate and the frozen nine-step evaluation order (`PROGRAMME_3_UNIT_9_PERMISSION_ENFORCEMENT_MECHANISM_CLARIFICATION.md` §6.2, §8) are untouched, unreordered, and un-shortened. A Relevance Request may only be constructed from candidates that have completed Pre-computation — successfully passed both gate tiers' eligibility, lifecycle, and permission filtering, sufficient for admission to the closed candidate set (adopted Proposal §12) — and every Relevance Result must still undergo Pre-disclosure: a fresh re-verification of permission and current lifecycle/status immediately before any disclosure, never relying solely on the pre-computation snapshot (adopted Proposal §13). Nothing in this amendment creates, implies, or permits an alternate or parallel permission pathway.

## 8. Failure Behaviour

Failure behaviour follows Contract Design V2's own existing discipline: distinguishable outcome categories, never silently conflated, and genuine implementation faults propagate honestly rather than being absorbed into a successful-looking result. Every relevance-specific failure case — mechanism unavailable, timeout, malformed output, an unknown, unauthorised, altered, duplicate, or stale/cross-request identifier, a permission or lifecycle change mid-computation, or an attempt to exceed the closed candidate set or mutate canonical persistence — is a sub-case of the Unit 9 Contract Design's own existing "Implementation failure" outcome category (§9), never a sixth, newly-invented top-level category. The complete, binding table is fixed at `PROGRAMME_3_UNIT_9_SEMANTIC_RELEVANCE_SCOPE_LOCK_REVISION_PROPOSAL.md` §15 and restated as Unit 9-tier contract text in `PROGRAMME_3_UNIT_9_7_BOUNDED_SEMANTIC_RELEVANCE_CONTRACT_AND_PERMISSION_SUCCESSOR.md`; this amendment incorporates both by reference.

## 9. Ordering/Ranking Semantics

Ranking is authorised only to the exact extent the adopted Proposal already fixes it, and no further: Bounded Relevance Computation may run only as a fallback, strictly after structural (literal/substring) evaluation over the same already-eligible set has completed successfully and returned exactly zero relevant candidates (adopted Proposal §16, incorporated by reference in full — including its bright-line rules that fewer-than-`maximumResults` is not zero, one structural match prevents fallback, and permission denial, lifecycle exclusion, and structural failure are none of them fallback triggers). This amendment does not authorise ranking or ordering on any query where structural matching succeeds, and does not authorise running both mechanisms concurrently or blending their outputs.

## 10. Integration Boundaries

Bounded Relevance Computation integrates entirely downstream of Memory Core, inside Knowledge Retrieval, consuming only content Memory Core's own unmodified interface has already made available (CDR-008, incorporated by reference). It must be local, in-process, on-device, or otherwise entirely under Parker's own operator control; no remote or cloud-hosted processor is authorised, and no candidate content or query text may cross a network boundary the operator does not control (adopted Proposal §10, condition 13). It integrates with no component other than Knowledge Retrieval; Evidence Custodian, World Model, conversations, arbitrary documents, `PermissionEngine`, and Memory Core are all explicitly outside this amendment's integration boundary.

## 11. Implementation-Neutrality

Every concept in this amendment is stated without naming a specific library, embedding model, or storage technology. QMD and any embedding model appear only in the adopted Proposal's own evidence sections as proof that a mechanism satisfying these contractual concepts is achievable — never as a named, required, or implied dependency of this amendment (adopted Proposal §4, §8). A future implementation remains free to select any mechanism satisfying Sections 2 through 10 above.

## 12. Compatibility with Existing Knowledge Memory Contracts

This amendment is additive only. It does not alter Contract Design V2's own layering constraint, its Amendments 1 through 8, its Non-Functional Requirements (§7), or any other section beyond the single §13 insertion below. It is fully compatible with, and does not touch, the Unit 9 Knowledge Retrieval Contract Design's own §8 determinism guarantee and §9 error model (both extended by reference, never altered) or the Unit 9 Permission Enforcement Mechanism Clarification's frozen evaluation order.

## 13. Adopted Textual Amendment

*(Adopted text. Contract Design V2 itself is not edited — this document is the governing instrument for the single insertion below, exactly as CDR-008 already governs Memory Core Scope Lock's practical reach without editing its text.)*

Existing §13 text (superseded by the insertion below as of this amendment's adoption; Contract Design V2's own file is not edited):

> "Unchanged from Version 1: implementation, persistence, indexing, caching, optimisation, vector storage, embeddings, OCR/document extraction mechanics, Planner integration, Workflow Engine integration. ..."

Adopted insertion, in force immediately following that sentence as of this amendment's adoption:

> "**(Amendment 9.)** The single exception is Bounded Relevance Computation, authorised only as `PROGRAMME_3_KNOWLEDGE_MEMORY_BOUNDED_SEMANTIC_RELEVANCE_SCOPE_LOCK_AMENDMENT.md` (Scope Lock §11) and `PROGRAMME_3_UNIT_9_7_BOUNDED_SEMANTIC_RELEVANCE_CONTRACT_AND_PERMISSION_SUCCESSOR.md` together bound it: request-scoped, disposable, local-only, never a persistent index, never a parallel source of canonical Knowledge Memory. Persistence, indexing (beyond the single request), caching, optimisation, and any non-disposable vector storage remain fully out of scope, exactly as before this amendment."

This adopted sentence follows Amendments 1 through 8's own existing inline citation convention and requires no restructuring of the surrounding text beyond the single inserted sentence.

## 14. Explicit Non-Goals

This amendment does not define, imply, or foreclose any concrete Kotlin interface, class, or method signature; does not select or imply any storage, indexing, or embedding technology; does not authorise Models B or C; and does not create any contractual concept usable outside the exact Relevance Request / Relevance Result pairing Sections 2–3 define. Any broader contractual concept requires its own separate, dedicated amendment.

## 15. Adoption Record

This amendment is adopted, together with and only together with `PROGRAMME_3_KNOWLEDGE_MEMORY_BOUNDED_SEMANTIC_RELEVANCE_SCOPE_LOCK_AMENDMENT.md` and `PROGRAMME_3_UNIT_9_7_BOUNDED_SEMANTIC_RELEVANCE_CONTRACT_AND_PERMISSION_SUCCESSOR.md` — all three were independently reviewed (VERDICT — REVISE BEFORE ACCEPTANCE, three defects identified), corrected, and adopted together through Programme 3's own established review discipline, following a Defect Confirmation Review (VERDICT — ACCEPT, no unresolved defect remaining). This amendment alone does not authorise implementation of Unit 9.7 — a separate, dedicated Unit 9.7 Implementation Plan, itself independently reviewed and adopted, is required before any code may be written.
