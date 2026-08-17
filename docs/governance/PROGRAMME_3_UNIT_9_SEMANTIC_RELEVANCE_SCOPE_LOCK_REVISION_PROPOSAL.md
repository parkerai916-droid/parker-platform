**Status: Adopted.** This document has passed the review discipline Section 19 requires: an Independent Constitutional Review, conducted earlier in this governance engagement, found the blocking constitutional-boundary question of Section 5 unresolved by any existing mechanism (VERDICT C) and separately identified nine further, non-blocking corrections; `docs/decisions/CDR-008_MEMORY_CORE_DOWNSTREAM_RELEVANCE_BOUNDARY.md` (Accepted. Canonical. Frozen.) resolved the blocking question at the Memory Core interpretation layer; and this document's own Final Correction, Review and Adoption Gate pass — conducted as a single bounded turn within this same engagement — applied all nine corrections (see Sections 5.1, 6.1, 8, 9, 10, 10.1, 11, 15, 16, and 17, below), re-ran a seventeen-item hostile review with every case FORECLOSED, checked cross-document consistency with no blocking contradiction found, and determined REMEDY VERDICT — READY FOR ADOPTION. Unlike the Unit 9 Scope Lock Clarification and the Unit 9 Permission Enforcement Mechanism Clarification, this document's review history exists in this engagement's own conversation record rather than as separately committed `docs/reviews/` documents; a reader relying on this status line should treat that as this document's actual provenance rather than assume parallel review files exist on disk.

**Adoption of this document accepts it as Programme 3's own governing remedy design; it does not itself authorise semantic relevance.** Section 3's existing prohibition remains in full, unweakened, unqualified force until the further steps Section 20 names — a formally drafted Knowledge Memory Scope Lock revision and Unit 9 Contract Design amendment (or successor unit), themselves independently reviewed and adopted — also occur; and no implementation of Bounded Relevance Computation is authorised until a further, separately reviewed implementation unit exists (Section 17, Section 19). This document does not amend, weaken, reinterpret, or silently alter `docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_CONTRACT_DESIGN_V2.md`, `docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_SCOPE_LOCK.md`, `docs/governance/PROGRAMME_3_UNIT_9_KNOWLEDGE_RETRIEVAL_CONTRACT_DESIGN.md`, `docs/governance/PROGRAMME_3_UNIT_9_SCOPE_LOCK_CLARIFICATION.md`, `docs/governance/PROGRAMME_3_UNIT_9_PERMISSION_ENFORCEMENT_MECHANISM_CLARIFICATION.md`, `docs/architecture/MEMORY_CORE_SCOPE_LOCK.md`, or any other frozen governance document — all remain frozen, unchanged, and in full force unless and until the further, separate revision/amendment Section 20 requires is itself independently reviewed and formally adopted. No Kotlin is implemented, proposed, or changed. No production code, test, or experimental fixture is touched. Nothing is staged, committed, or pushed. This document does not authorise any implementation unit.

# Programme 3 — Unit 9 — Semantic Relevance Scope Lock Revision Proposal

## 1. Purpose

To propose a narrowly bounded, constitutionally scoped amendment permitting a subordinate, replaceable relevance-computation mechanism to operate over an already Parker-authorized, already-eligible candidate set — solely to remedy Parker's demonstrated inability to recall genuinely relevant, already-promoted canonical knowledge when an owner's request is a semantic paraphrase rather than a literal/structural match — without transferring any constitutional authority away from Parker's own Knowledge Retrieval boundary, and without adopting any particular retrieval library, embedding model, or persistent index architecture.

## 2. Trigger / Demonstrated Retrieval Defect

`ParkerRuntimeReasoningContextIntegrationTest.kt` (commit `aadd596`, test `a genuinely related paraphrase does not recall a promoted proposition under the current literal substring retrieval`, and its six-memory extension) demonstrates, against the real, running `ParkerRuntime`: a proposition — "the owner's synthetic emergency vet is Harbour Animal Clinic" — is genuinely promoted through Parker's existing, unmodified Remember/promotion path into canonical Knowledge Memory. A later, separate turn asking "Which animal clinic did I tell you to use in an emergency?" — a genuine paraphrase, not a rewording containing any literal substring of the stored proposition — fails to recall it, because `DefaultReasoningKnowledgeSource.recall()`'s only relevance mechanism (`src/runtime/DefaultReasoningKnowledgeSource.kt:120`) is `content.contains(query.relevance, ignoreCase = true)` — case-insensitive substring matching, and nothing else. This occurs even with six genuinely, separately promoted, canonically stored memories present. This is not a hypothetical defect; it is demonstrated against the real runtime, the real promotion path, and the real assembled prompt.

## 3. Existing Frozen Rule

Three independent, explicit statements exclude the class of mechanism needed to remedy Section 2's defect:

- `PROGRAMME_3_UNIT_9_KNOWLEDGE_RETRIEVAL_CONTRACT_DESIGN.md` §2: "Ranking, if any — bounded strictly to Section 8's own deterministic-ordering guarantee. No scoring, weighting, relevance-ranking, or similarity-ranking of any kind is a Knowledge Retrieval responsibility."
- Same document, §3: "Ranking algorithms beyond this contract's own deterministic-ordering guarantee — no embeddings, vector search, semantic similarity, or scored relevance ranking is designed, implied, or permitted anywhere in this document."
- `PROGRAMME_3_KNOWLEDGE_MEMORY_SCOPE_LOCK.md` §4 (Explicit Exclusions): "Embeddings, vector search, or any semantic/similarity-based retrieval | Never designed anywhere in the frozen contracts; explicitly excluded at both the Memory Core layer (`MEMORY_CORE_SCOPE_LOCK.md` §4, §10) and the Knowledge Memory layer (Contract Design Version 2 §13). Knowledge Retrieval performs structural matching against caller-supplied criteria only."
- `PROGRAMME_3_KNOWLEDGE_MEMORY_CONTRACT_DESIGN_V2.md` §13 (Out of Scope): vector storage and embeddings are named as out of scope.

A deeper, separate, and *explicitly out-of-scope-for-this-proposal* exclusion exists one architectural layer below, at `docs/architecture/MEMORY_CORE_SCOPE_LOCK.md` §4, §10, §15 and §19.6: Memory Core's own seven structural retrieval modes are constitutionally exclusive ("Memory Core **SHALL** support exactly the seven retrieval modes... and **SHALL NOT** implement any ranked, scored, or semantic retrieval mode"), and §10 explicitly warns this is "the one exclusion most likely to be quietly reintroduced under a different name, such as 'relevance-ranked retrieval' or 'smart lookup.'" **This proposal does not touch, does not require touching, and does not depend on any revision to Memory Core Scope Lock.** Section 5 explains why that boundary is architecturally unnecessary to cross.

## 4. Experimental Evidence

The `experiment/qmd-canonical-memory-retrieval` (commit `aadd596`) and `experiment/parker-authorized-vector-filter` (commit `fba2e37`) experiments, reviewed fresh for the governance determination this proposal follows, established, with strong or moderate-strong evidence: a genuine semantic paraphrase, embedded by a real (non-mocked) embedding model, ranks the intended canonical memory first among six genuine distractors (0.586 vs. 0.414 cosine similarity, independently recomputed from raw captured vectors); a subordinate ranking mechanism can be restricted, at the source level (`qmd/src/store.ts:4272-4308`, `exactVecScanByHashSeq`), to score only a caller-supplied allowed set, before scoring, not after; forty stronger unauthorized candidates cannot crowd out one weaker authorized candidate under a small result limit; ranked results resolve back to the exact canonical Parker object by identity (`assertSame`), never a competing representation; and automated test execution can be fully deterministic, using captured real embedding output, without live model inference at test time.

The same evidence did **not** establish, and this proposal does not rely on: authorization for a relevance mechanism to ingest content Parker has not yet authorized (the experimental bridge indexed all six candidates, including a denied one, before applying its query-time filter); any persistent index's compatibility with Parker's own lifecycle (the experimental index was fully disposable, rebuilt and destroyed per call); any production runtime composition; or that Parker's lifecycle/evidential/staleness semantics survive a pipeline that actually carries them (the experimental data model carried none of these fields).

QMD and its bundled embedding model (`embeddinggemma`) appear in this section, and throughout this proposal, solely as experimental evidence that a subordinate relevance mechanism satisfying Model A-Strict is technically achievable. Neither is, or becomes by this citation, a constitutional dependency of this proposal: every normative rule below is stated in implementation-neutral terms (subordinate relevance mechanism, relevance representation, relevance computation, request-scoped disposable state) and would be satisfied identically by any other mechanism meeting the same bounded requirements.

## 5. Constitutional Issue

Programme 3's own prior Constitutional Decision Record, `CDR-001_MEMORY_RECORD_COMPARISON_VS_SEMANTIC_RETRIEVAL.md`, examined a related question (a promotion-time comparison capability) and found Memory Core Scope Lock's use of "semantic retrieval" to be, without exception, "confined... to a differently-purposed, caller-facing retrieval capability" (CDR-001, Question 4, Table 1/2) — i.e., precisely a query-time, ranked/scored mechanism serving a caller's request. That is exactly, and only, what this proposal's capability is. Unlike CDR-001's own subject (which found genuine textual ambiguity, Option C), there is no comparable ambiguity here to resolve in this proposal's favor: the governing text squarely, repeatedly, and self-awarely names and excludes the exact mechanism this proposal requests. This proposal does not claim otherwise, and does not attempt the renaming Memory Core Scope Lock §10 explicitly warns against ("relevance-ranked retrieval," "smart lookup"). It is an honest, acknowledged request to reverse a deliberate exclusion, under narrow, load-bearing conditions — not a reinterpretation, and not exploitation of any drafting gap.

Because this proposal touches only Programme 3's own frozen scope decision (Knowledge Memory Contract Design V2 §13, the Knowledge Memory Scope Lock, and Unit 9's own Contract Design), and not Memory Core's separate architectural freeze, the required process is Programme 3's own self-described one — see Section 19 — not a reopening of Memory Core Scope Lock or a new cross-programme constitutional doctrine.

## 5.1. CDR-008 Cross-Reference (Adopted)

`docs/decisions/CDR-008_MEMORY_CORE_DOWNSTREAM_RELEVANCE_BOUNDARY.md` (Status: Accepted. Canonical. Frozen.) is the governing constitutional interpretation for the question Section 5 raises, and this proposal expressly cites and relies upon it rather than re-arguing the question it settles. CDR-008 decided that `MEMORY_CORE_SCOPE_LOCK.md`'s prohibition on ranked/scored/semantic retrieval is a prohibition on Memory Core's own retrieval interface, operations, implementations, and authority — not a platform-wide bar reaching a separately-governed downstream component computing relevance over a closed candidate set Memory Core has already, through its own unmodified interface, made available and Parker has independently determined eligible.

This proposal freezes, as governing fact rather than as its own argument, everything CDR-008 itself froze:

- Memory Core remains completely unchanged; its interface, seven retrieval modes, and SHALL/SHALL NOT obligations are untouched by CDR-008 and are not touched by this proposal either.
- Memory Core retains exactly seven structural retrieval modes, unchanged in count, name, or shape.
- Memory Core cannot directly or indirectly perform, delegate, or depend upon any ranked, scored, or semantic retrieval operation — whether internally or through any external service, index, dependency, library, process, or component (CDR-008 Mandatory Invariant 3) — a prohibition this proposal's own Bounded Relevance Computation mechanism never touches, tests, or relies upon any exception to.
- The capability this proposal describes, if ever adopted and implemented, exists solely downstream, in Knowledge Retrieval, under Programme 3's own governance — never inside Memory Core, and never by amendment to Memory Core Scope Lock.
- CDR-008 authorises no downstream capability by itself, does not authorise semantic relevance, and does not decide whether this proposal should be adopted; it resolves only the constitutional-boundary question Section 5 raises. CDR-008's own reasoning is confined to the Memory Core / Knowledge Retrieval boundary and creates no precedent-by-analogy for Evidence Custodian, World Model, or any other component (CDR-008, Constitutional Boundary) — Section 17, below, states this proposal's own, independent scope-creep denials in full.

CDR-008's adoption resolves, at the Memory Core interpretation layer, the blocking defect the prior Independent Constitutional Review of this proposal identified (VERDICT C — governing-mechanism insufficiency). It does not itself resolve any of the other, non-blocking corrections that review separately identified; Section 2 through Section 20, as corrected below, address those directly.

## 6. Authority-Preservation Principle

Parker retains, unconditionally and without exception, sole authority over: what canonical knowledge exists; `KnowledgeItem` eligibility (lifecycle/status, retirement, restoration); evidential-state assignment; the content and enforcement of every permission decision; staleness-disclosure computation and mandate; `KnowledgeRetrievalQuery` construction; maximum-result bounding as a final, Parker-owned step; `SafeKnowledgeResultEntry` construction; and reasoning-context rendering. A subordinate relevance mechanism may compute, at most, an ordering or score over a candidate set Parker has already fully and finally authorized for this one request. It may never expand that set, alter any item in it, substitute its own content for Parker's, or make any decision this section reserves to Parker.

## 6.1. Evidential State Is a Disclosure Field, Not an Eligibility Gate

Stated precisely, from existing, unmodified Programme 3 governance, and invented nowhere by this proposal: evidential-state classification (Article IV; Knowledge Memory Scope Lock §3, §6; Contract Design V2 §1, §4, §12) is a **disclosure field**. It is assigned exclusively by Knowledge Memory's own promotion or revision evaluation, is carried on the `KnowledgeItem`, and is surfaced as part of a Knowledge Result — it expresses how well available evidence supports a proposition, never whether the proposition is true, and never whether the item may be retrieved at all. No frozen Programme 3 or Memory Core document names, defines, or implies an evidential-state *eligibility gate* — that role is reserved, exclusively and separately, to permission (`PermissionEngine`, two-tier act-level/item-level gate) and to lifecycle/status filtering. Lifecycle status is itself expressly never a substitute for, or determinant of, a permission decision (Knowledge Memory Contract Design V2 §6); by the same textual logic, evidential state is never a substitute for, or determinant of, either lifecycle or permission eligibility.

This proposal does not invent an evidential-state gate, does not treat evidential state as a retrieval-eligibility criterion anywhere in this document, and does not authorise semantic relevance to alter evidential-state authority, computation, or semantics in any way (Section 9). Because no evidential-state eligibility gate exists in current governance, there is correspondingly no "evidential exclusion" capable of triggering or suppressing semantic fallback; Section 16's fallback trigger is defined without reference to one, and Section 15's failure table treats evidential state solely as an already-existing disclosure obligation Parker computes after eligibility, exactly as today.

## 7. Eligibility-Versus-Relevance Distinction

**Retrieval eligibility** — permission, lifecycle/status, evidential requirements, and query scope — is decided by Parker, using existing, unmodified mechanisms (the same `PermissionEngine` two-tier gate, the same `isRetrievable()`-style lifecycle filter, the same structural query-scope rules Unit 9 already fixes), before any candidate is visible to a relevance mechanism at all.

**Relevance computation** — given an already-eligible set, how well does each candidate answer this request — may be performed by a subordinate mechanism, strictly bounded as follows.

This proposal tested the eleven conditions given in the governing task against this distinction and finds all eleven necessary and, together, sufficient to preserve Unit 9's authority structure — provided every one is treated as a mandatory precondition, not an implementation preference:

1. Parker constructs the query — unchanged from today (`DefaultReasoningContextAssembler`).
2. Parker determines candidate eligibility — unchanged, using existing permission/lifecycle mechanisms, before Section 8's boundary.
3. Parker authorizes each candidate individually before semantic computation — extends, not replaces, the existing per-item permission gate.
4. The semantic mechanism receives no authority to add candidates — enforced by construction: it is given a fixed, closed, per-request set and nothing else.
5. It cannot expand the eligible set — same enforcement; its output is filtered against the input set at resolution (Section 14).
6. Results identify only Parker-supplied candidates — opaque per-request identifiers, never new identifiers or free text (Section 11).
7. Parker resolves returned identifiers against canonical Knowledge Memory — never trusts returned content (Section 14).
8. Parker re-verifies permission and lifecycle/status before disclosure — closes the race a single pre-computed snapshot cannot (Section 13).
9. Parker applies the final maximum-result bound — the mechanism's own limit parameter is a performance hint only, never the constitutional bounding decision (Section 10, Section 8 hardening note below).
10. Parker alone constructs `SafeKnowledgeResultEntry` — unchanged; the mechanism never sees or produces this type.
11. Parker alone renders reasoning context — unchanged; `DefaultReasoningContextAssembler`'s exclusive role is untouched.

**Finding: the separation is constitutionally sound**, conditioned on all eleven being mandatory. It is unsound, and this proposal does **not** endorse it, if any one of these becomes optional, "best effort," or an implementation detail left to engineering discretion.

## 8. Proposed Bounded Semantic-Relevance Capability

Defined abstractly, independent of any specific library, embedding model, or storage technology — deliberately not named "vector search" or bound to QMD:

> **Bounded Relevance Computation**: a request-scoped, subordinate, replaceable computation that accepts (a) a `KnowledgeRetrievalQuery`'s relevance text, and (b) a Parker-supplied, closed set of already-eligibility-determined candidates, each represented only by an opaque per-request identifier and the minimum normalized content necessary to assess relevance; and returns an ordering or subset of those same identifiers, and nothing else — never new identifiers, never free-text content, never any assertion about permission, lifecycle, evidential state, or eligibility.

QMD's `experiment/parker-authorized-vector-filter` demonstrates that this capability can be implemented safely under Model A-Strict boundaries (Section 10). It is evidence that the capability is achievable, not a request to adopt QMD, any embedding model, or any storage technology by name. This proposal creates no constitutional dependency on QMD, `embeddinggemma`, or any other named library, model, or product; a future implementation unit remains free to select any mechanism satisfying the bounded requirements this proposal fixes.

## 9. Explicitly Retained Prohibitions

A Bounded Relevance Computation mechanism may not, under any circumstance:

- determine retrieval eligibility, in whole or in part;
- determine or influence a permission decision;
- determine or influence lifecycle/status;
- determine or influence evidential-state classification;
- construct, in whole or in part, a `SafeKnowledgeResultEntry`;
- render, compose, or influence reasoning-context text;
- persist state beyond the single request that created it;
- receive any candidate Parker has not already, fully authorized for this request;
- receive query text without separate, explicit authorisation for that specific disclosure (Section 11);
- expand, substitute into, or otherwise alter the candidate set Parker supplied;
- write, persist, or otherwise mutate Parker canonical persistence, or enumerate, request, or obtain any candidate beyond the closed set Parker supplied — architecturally, not merely by instruction (Section 15, Architectural Inability Requirement).

## 10. Model A-Strict Initial Boundary

The first, and until separately re-governed, only authorized architecture:

1. Parker obtains canonical `KnowledgeItem`s from its own persistence, unchanged.
2. Parker performs all existing eligibility filtering (lifecycle/status), unchanged.
3. Parker performs all required permission checks (act-level and item-level), unchanged.
4. Only the resulting authorized candidates' minimally necessary content (Section 11) may cross into the relevance mechanism.
5. The mechanism ranks/scores only those candidates.
6. No unauthorized candidate is embedded, indexed, persisted, or disclosed to the mechanism, at any point, even transiently.
7. The mechanism returns only opaque identifiers or rankings for Parker-supplied candidates — never content, never new identifiers.
8. Parker resolves those identifiers against canonical Knowledge Memory by direct lookup — never trusting any content the mechanism might additionally return.
9. Parker re-checks permission and current lifecycle eligibility, freshly, immediately before constructing any `SafeKnowledgeResultEntry` — never relying solely on the pre-computation snapshot.
10. Semantic/index state is request-scoped and fully disposable; nothing survives past the single retrieval call that created it.
11. No persistent secondary Knowledge Memory index of any kind is authorised by this proposal.
12. Failure of the mechanism fails closed, or falls back only to an explicitly-authorised existing retrieval mechanism (today: structural substring matching) — never to a stale or partial result presented as complete.
13. The mechanism authorised under this initial architecture must be local, in-process, on-device, or otherwise entirely under Parker's own operator/owner control. No remote, cloud-hosted, or third-party-network-reachable relevance processor is authorised under Model A-Strict, and no candidate content or query text may cross any network boundary Parker's own operator does not control. Any future remote or cloud-based relevance processing requires separate, explicit governance and is not authorised by this proposal.
14. The mechanism's identity, version, and configuration are frozen, disclosed, retrieval-relevant state, not incidental implementation detail — see Section 10.1.

**This eliminates the ingestion-boundary problem (P8) and the index-lifecycle problem (P9) by construction** — condition 6 means unauthorized content structurally never reaches the mechanism at all (unlike the experiment's own bridge, which indexed a denied candidate transiently within a call); condition 10/11 means there is no persisted index whose lifecycle could ever diverge from Parker's own.

**This is deliberately not optimized for performance.** Rebuilding and discarding relevance-mechanism state on every retrieval call, and re-verifying permission/lifecycle after every ranking, is slower than a persistent index would be. If Model A-Strict proves too slow for production use, that is a reason to bring a *separate*, *new* governance proposal for a persistent architecture (Model B or C) — never a reason to relax condition 6, 9, 10, or 11 without one.

Models B and C remain prohibited pending separate, dedicated governance, for the reasons Section 5 of the prior Governance Determination gives in full (persistent unauthorized-content exposure, index-staleness risk, and, for Model C, direct conflict with the Knowledge-Memory-layer "no duplicate sources of truth" principle CDV2 §7 states for the Memory-Core boundary).

## 10.1. Determinism Requirement

Knowledge Memory Scope Lock §7 and Unit 9 Knowledge Retrieval Contract Design §8 already guarantee, for every retrieval: "a given Knowledge Query, issued twice against unchanged Knowledge Memory state, must return an identical Knowledge Result both times, including identical ordering." This proposal extends that guarantee, without weakening it, to cover the one new source of retrieval-relevant state Bounded Relevance Computation introduces:

- **The mechanism's identity, version, and configuration are part of retrieval state relevant to determinism**, exactly as Knowledge Memory's own state already is. The guarantee, as extended, reads: identical query, identical canonical state, and identical frozen relevance-mechanism identity/version/configuration together produce an identical retrieval result, including identical ordering, every time.
- **Candidate ordering returned by the mechanism must be deterministic** for a fixed query, fixed candidate set, and fixed mechanism identity/version/configuration — no hidden randomisation, time-based variation, or load-dependent reordering, mirroring Unit 9 §8's own existing "caller expectations" language for the structural case.
- **Tie-breaking must be deterministic** and disclosed, exactly one rule, applied consistently — mirroring Knowledge Memory Scope Lock §3's own discipline for concurrent-revision ordering (one consistent, disclosed rule; the specific rule is an implementation/Scope Lock decision, not fixed here).
- **A mechanism upgrade, replacement, or reconfiguration is itself an explicit governance/configuration change** — logged and attributable — never invisible behavioural drift discovered only by a change in retrieval results. This mirrors Memory Core Scope Lock §10's own warning against a capability being "quietly reintroduced under a different name": here, the analogous risk is a capability being quietly *changed* under an unchanged name, and this requirement forecloses it.

This does not require, and this proposal does not authorise anyone to require, bit-identical floating-point output across different hardware, operating systems, or numerical libraries — no existing governance document imposes that standard for any other Parker computation, and this proposal introduces no new one. What is required is that a fixed mechanism, fixed configuration, fixed query, and fixed canonical state deterministically produce the same candidate ordering on the same execution environment, and that any change capable of altering that ordering is a disclosed, governed change, never silent drift.

## 11. Information-Minimisation Boundary

The mechanism receives, at minimum and at maximum: the query's relevance text, and, per authorized candidate, an opaque per-request identifier and the minimum normalized proposition/content text necessary to compute relevance. It must **not** receive: the candidate's permanent `KnowledgeId` (a fresh, request-scoped, unlinkable token is sufficient and preferred); evidential state; provenance or evidence references; lifecycle history; permission information; or the requesting principal's identity.

Opaque per-request identifiers plus normalized content are sufficient — this is exactly the pattern `qmd-authorized-vector-bridge.mts` already implements (a per-request virtual path derived only from a candidate identifier, never Parker's permanent identity or any of the fields above).

**Disclosure of query text to the relevance mechanism requires explicit authorisation, mandatory and not advisory**, exactly as candidate-content disclosure does (Section 12): the relevance text may itself reveal what a specific, identifiable request is about, and this proposal does not treat that disclosure as automatic or incidental to authorising the mechanism itself. Only the authorised query/relevance text and minimal normalized candidate content necessary to compute relevance may reach the mechanism; it must not receive, unless separately required and separately governed: the candidate's permanent `KnowledgeId`; evidence references; provenance; lifecycle history; permission state; or the requesting principal's identity. Whatever query text is authorised for disclosure must be treated with the same sensitivity as candidate content — never logged or persisted by the relevance mechanism beyond the single request, and never associated with the requesting principal's identity in a way that would let it be reconstructed as a durable per-principal record outside Parker's own persistence.

## 12. Pre-Authorization Requirement

Mandatory, not advisory: eligibility (permission, lifecycle, evidential/query-scope rules) must be fully and finally determined **before** any candidate's content crosses into the relevance mechanism. No candidate may be supplied provisionally, "pending" a later check, or supplied on the expectation that a later step will filter it back out.

## 13. Post-Computation Re-Verification Requirement

Mandatory: immediately before constructing any `SafeKnowledgeResultEntry`, Parker must re-verify permission and current lifecycle/status for every identifier the relevance mechanism returned, using a fresh check against current state — never relying solely on the pre-computation snapshot. This is necessary because the round trip to a subordinate mechanism is real elapsed time during which permission or lifecycle state can genuinely change; a pre-computed authorization is a statement about the past, not a guarantee about the moment of disclosure.

## 14. Canonical-Object Resolution Requirement

Mandatory: the relevance mechanism's output identifies Parker-issued, per-request opaque identifiers only. Parker alone resolves each returned identifier back to its own canonical `KnowledgeItem`, by direct lookup against its own persistence — never accepting or rendering any content the mechanism itself returns. An identifier that fails to resolve, or that resolves outside the exact candidate set Parker originally supplied for this request, is an integrity fault: it must be rejected and, in production, logged as an anomaly — never silently substituted, never treated as equivalent to a low-relevance result.

## 15. Failure / Fail-Closed Requirements

Stated as constitutional requirements, not implementation detail:

| Failure | Constitutional requirement |
|---|---|
| Mechanism unavailable | Distinguishable, honestly-propagated outcome (Unit 9 §9's existing "Implementation failure" vocabulary); fall back to structural matching or an empty result — never silently omitted |
| Timeout | Treated identically to mechanism unavailable — a distinguishable, honestly-propagated outcome, never silently coerced into an empty result presented as a completed, zero-match computation |
| Embedding/relevance computation fails | Same as above; never presented as though the mechanism ran successfully and found nothing |
| Malformed ranking result | Must fail loudly (reject the whole result), never silently coerce to an empty or partial list |
| Mechanism returns candidate content instead of identifiers | Rejected as an integrity fault (Section 14); content returned by the mechanism is never trusted, rendered, or substituted for Parker's own canonical content under any circumstance |
| Returned content, where any is present, differs from the candidate content Parker supplied | Rejected as an integrity fault (Section 14) — an altered candidate is treated identically to an unauthorized one, never merged with or preferred over Parker's own canonical record |
| Unknown returned identifier | Rejected as an integrity fault (Section 14) |
| Unauthorized returned identifier (outside the supplied set) | Rejected as an integrity fault (Section 14) — never trusted merely because it resembles an authorized one |
| Duplicate identifier | De-duplicated by Parker before bounding; never permitted to consume more than one of `maximumResults`' slots |
| Too many results returned | The mechanism's own limit is a performance hint only (Section 10); Parker's own bounding step (Section 7, condition 9) is authoritative regardless of what the mechanism returns |
| Request-token collision (a returned opaque identifier matches a token issued for a different request) | Rejected as an integrity fault (Section 14); Parker resolves only against the exact candidate set issued for the current request, never against any other request's token space |
| Stale or cross-request token (a returned identifier was issued for an earlier, already-completed request) | Rejected as an integrity fault (Section 14), identically to an unauthorized identifier — a token's validity does not outlive the single request that issued it |
| Permission change during computation | Caught by the mandatory post-computation re-verification (Section 13); a candidate that became ineligible during the round trip is excluded, never disclosed |
| Lifecycle/status change during computation | Same mechanism, same requirement |
| Canonical item deleted during computation | Treated identically to an unresolvable identifier (Section 14) — excluded, not substituted |
| Candidate cannot be re-resolved against canonical persistence | Excluded (Section 14); never rendered from mechanism-supplied content as a fallback |
| Mechanism attempts to write, persist, or otherwise mutate Parker canonical persistence | Architecturally impossible, not merely prohibited by policy (see Architectural Inability Requirement, below); any such attempt indicates a Model A-Strict violation and the mechanism is out of this proposal's authorisation entirely |
| Mechanism attempts to enumerate, request, or otherwise obtain candidates beyond the closed set Parker supplied | Architecturally impossible, not merely prohibited by policy (see Architectural Inability Requirement, below); any such attempt indicates a Model A-Strict violation and the mechanism is out of this proposal's authorisation entirely |

**Pre-computation authorization and post-computation re-verification are both mandatory, not alternatives to each other.** Pre-authorization prevents unauthorized content from ever reaching the mechanism; post-verification prevents a race during the mechanism's own elapsed time from becoming a disclosure. Either alone is insufficient.

**Architectural Inability Requirement.** For the two structural-integrity rows above, and for Section 9's prohibitions on canonical-data modification and candidate-set expansion generally, this proposal requires architectural inability, not merely a behavioural promise or documented restriction: the interface, process boundary, or connection the mechanism is given must make writing Parker canonical persistence, enumerating a broader candidate set, or modifying canonical data structurally impossible for the mechanism to perform — for example, by supplying no credential, handle, or network path capable of reaching Parker's own persistence or any broader candidate source, rather than merely instructing the mechanism, or an implementer, not to use one if it existed. A mechanism that is merely told not to do these things, but retains the technical means to attempt them, does not satisfy this requirement.

## 16. Structural-Retrieval Relationship and the Precise Fallback Trigger

Structural (literal/substring) retrieval remains authoritative and unchanged. Bounded Relevance Computation may run **only** when all three of the following hold, in this order:

1. The same, already-eligible candidate set has been established — the identical set surviving Section 7's eligibility determination (permission, lifecycle/status, query-scope), with no separate or expanded set constructed for the relevance mechanism.
2. Structural relevance evaluation over that exact set has completed successfully — the existing substring-matching mechanism ran to completion without failure, exception, or error of any kind.
3. Structural relevance evaluation returned exactly zero relevant candidates from that set.

Frozen, explicitly, as bright-line rules admitting no implementation discretion:

- **Fewer than `maximumResults` structural matches is not zero.** Any structural match count of one or more forecloses fallback, regardless of how far short of `maximumResults` it falls.
- **One structural match prevents fallback.** A single relevant candidate found structurally is sufficient to make Bounded Relevance Computation inapplicable to that request, even where a semantic mechanism might have found other, arguably more relevant candidates the structural match did not surface.
- **Permission denial is not a fallback trigger.** If permission filtering reduces the eligible set (Section 7) such that zero candidates ever reach structural evaluation, or structural evaluation is never reached because the act-level or item-level gate denied first, this is a permission outcome (Unit 9 §9's own "Permission denial" category), not a structural-match outcome, and does not trigger fallback.
- **Lifecycle exclusion is not a fallback trigger.** The same applies where lifecycle/status filtering, not permission, removes candidates before structural evaluation runs.
- **Evidential exclusion is not a fallback trigger, because no evidential-state eligibility gate exists to exclude anything** (Section 6.1) — evidential state cannot suppress or trigger fallback because it plays no role in determining the eligible candidate set in the first place.
- **A malformed query is not an empty result.** Unit 9 §9's own "Invalid query" category is a distinct outcome from a well-formed query returning zero structural matches; a malformed query never reaches, and never triggers, Bounded Relevance Computation.
- **A structural retrieval failure or exception is not an empty result.** Unit 9 §9's own "Implementation failure" category is a distinct outcome from a well-formed query returning zero structural matches; a structural-matching failure never silently falls through to semantic fallback as though it were a legitimate zero-match outcome — it propagates honestly as a failure (Section 15).

**A weak structural literal match therefore suppresses a stronger semantic match, by design.** This is the deliberate Version-1 remedy boundary, recorded here explicitly rather than left as an implicit or accidental property of the ordering: this proposal remedies only the specific defect class Section 2 demonstrates (structural matching finds nothing at all), and does not attempt to make structural and semantic relevance compete or blend on every query. A future proposal seeking to change this precedence — for example, to let semantic relevance run alongside structural matching rather than only after it returns nothing — is a materially larger change requiring its own separate governance; it is not authorised by this document.

This is the smallest constitutional change available among the options considered. It preserves Unit 9's existing deterministic-ordering guarantee, unmodified, for every query that already works today — determinism and explainability for the common case never come to depend on the relevance mechanism at all, and Section 10.1's extended determinism guarantee applies only to the fallback path itself. It engages the new, more carefully governed capability only for the specific defect class Section 2 demonstrates: genuine paraphrases that structural matching, by design, cannot find. Running both mechanisms together on every query (a considered alternative) would change behavior and ordering for every retrieval, not just the demonstrated defect, and would make the "identical query, identical state, identical result" guarantee depend on the relevance mechanism's own determinism universally rather than only in the fallback case. Replacing structural matching entirely is not supported by any need identified in this review and would be a materially larger, unjustified change.

## 17. Explicit Non-Authorizations

This proposal does **not**, under any reading:

- adopt QMD, or any named retrieval library, by name;
- adopt any embedding model;
- authorise any persistent semantic index;
- authorise any secondary source of canonical Knowledge Memory;
- authorise supplying unauthorized `KnowledgeItem` content to a relevance mechanism, at ingestion or any other time;
- permit a relevance mechanism to decide permission;
- permit a relevance mechanism to decide lifecycle/status;
- permit a relevance mechanism to construct `SafeKnowledgeResultEntry`;
- permit a relevance mechanism to render reasoning context;
- authorise any production implementation by its own adoption — a separate implementation unit, separately scoped and separately reviewed, mirroring Unit 9.5's own precondition structure, remains required before any code is written;
- bring Models B or C into scope;
- weaken, reopen, or reinterpret Unit 9's existing permission-enforcement mechanism, its frozen evaluation order, or its outcome vocabulary, in any way;
- weaken, reopen, or reinterpret any existing evidential-state or provenance requirement;
- alter `DefaultReasoningContextAssembler`'s exclusive rendering role;
- revise, reopen, or depend upon any revision to `docs/architecture/MEMORY_CORE_SCOPE_LOCK.md` — that document's own, separate exclusion of semantic retrieval modes from `MemoryRetrieval` remains completely untouched, because Bounded Relevance Computation operates entirely downstream of Memory Core's existing structural read surface, over content Knowledge Memory has already obtained through ordinary point lookups;
- grant any relevance mechanism standing access to Parker's own model-serving infrastructure (including any Ollama/Qwen instance) — model selection and governance, if this proposal is ever adopted, is separate, deferred work (Section 18);
- authorise semantic, ranked, or scored retrieval for Evidence Custodian, in whole or in part, by this proposal's own force or by analogy to it;
- authorise semantic, ranked, or scored retrieval for World Model, in whole or in part, by this proposal's own force or by analogy to it;
- authorise semantic, ranked, or scored retrieval over conversations, by this proposal's own force or by analogy to it;
- authorise semantic, ranked, or scored retrieval over arbitrary documents, by this proposal's own force or by analogy to it;
- permit `PermissionEngine` to make, or be influenced by, any semantic or relevance-based decision — Chapter 10's sole authority over permission decisions is untouched, and no relevance mechanism this proposal describes has any input into it;
- authorise any ranked, scored, or semantic retrieval operation inside Memory Core, in whole or in part, by this proposal's own force or by analogy to it — see Section 5.1;
- authorise semantic, ranked, or scored retrieval for any other Parker subsystem not named in this document, by this proposal's own force or by analogy to it.

CDR-008's reasoning, and this proposal's own reasoning, are each confined to the specific boundary question and the specific capability they respectively address. Neither may be cited as implementation authority, or as persuasive precedent requiring a lighter review, for a semantic or relevance capability in any other Parker component — each such component, if it is ever proposed, requires its own independent, dedicated constitutional and governance review, exactly as this proposal itself required one.

## 18. Deferred Questions

Explicitly not decided by this proposal, and each requiring its own future governance before it can be answered: which specific relevance mechanism or library to select (QMD or otherwise); whether, and under what synchronization contract, a persistent index (Model B) could ever be justified; the concrete Kotlin type(s) expressing the new failure/fail-closed outcomes in Section 15; the concrete opaque-identifier scheme; what model-governance review any eventually-selected embedding or ranking model must pass before being trusted with even minimally-necessary Parker content; and whether Model A-Strict's per-request recomputation cost is acceptable in production, or whether it justifies bringing a separate persistent-architecture proposal later.

## 19. Required Review Path

Mirroring Programme 3's own established discipline for Unit 9's prior clarifications (the Unit 9 Scope Lock Clarification and the Unit 9 Permission Enforcement Mechanism Clarification, both of which followed this exact sequence before adoption): this document is a draft proposal; it requires an Independent Constitutional Review; if that review finds any required correction, a Defect Confirmation Review after correction; and only then is it eligible for adoption as a formal Programme 3 Knowledge Memory Scope Lock revision, together with a corresponding Unit 9 Contract Design amendment (or a new, narrowly-scoped successor unit, e.g. "Unit 9.7"). Adoption of this proposal, even if granted, does **not** itself authorise an implementation unit — a separate implementation plan, itself independently reviewed, is required afterward, exactly as Unit 9.5 could not begin until its own, separately adopted enforcement-mechanism Clarification existed.

This is Programme 3's own self-described mechanism (Knowledge Memory Scope Lock §10, "Out-of-Scope Change Policy": "requires formal governance before implementation — a Scope Lock revision, following the same review discipline this Programme itself just completed"). It is sufficient because this proposal touches only Programme 3's own frozen scope decision, not any constitutional Article, not Chapter 10/PermissionEngine's own sole-authority rules (CDR-005's higher CDR threshold governs changes to *those*, and nothing here proposes any), and not Memory Core's separate architectural freeze (Section 17). It is not under-governed, because it does propose reversing a deliberate, self-aware, adopted, independently-reviewed exclusion — which is exactly the class of change Programme 3's own review discipline (Independent Constitutional Review, then Defect Confirmation Review) exists to catch before adoption.

## 20. Adoption Status

This document has been adopted: (a) the Independent Constitutional Review and this document's own Final Correction, Review and Adoption Gate pass together satisfy the review-and-correction requirement — every defect either corrected in this pass or, for the blocking constitutional-boundary question, resolved by adopted CDR-008; (b) the Programme owner has explicitly accepted this document through that same pass, mirroring the process by which the prior Unit 9 Clarifications were accepted.

(c) remains outstanding, and is not a condition on *this document's own* adoption but a separate, further requirement this document's adoption does not satisfy or shortcut: a corresponding, formally drafted Knowledge Memory Scope Lock revision and Unit 9 Contract Design amendment (or successor unit) must themselves be independently reviewed and adopted before Section 3's existing prohibition is actually lifted. Until (c) occurs, Section 3's prohibition remains in full, unweakened, unqualified force, and any implementation of Bounded Relevance Computation — QMD-based or otherwise — remains exactly as prohibited as it was before this document was written. This document's own adoption accepts it as Programme 3's governing remedy design for the Section 2 defect and settles that no further Independent Constitutional Review or Defect Confirmation Review of this document itself is required before (c) is undertaken — it does not itself authorise semantic relevance, and does not narrow, shorten, or waive the independent review (c) requires for the Scope Lock revision and Contract Design amendment (or successor unit) themselves.
