**Status:** Narrow, implementation-facing governance clarification only. **Adopted.** Independent constitutional review is complete (`docs/reviews/PROGRAMME_3_UNIT_9_PERMISSION_ENFORCEMENT_MECHANISM_CLARIFICATION_INDEPENDENT_CONSTITUTIONAL_REVIEW.md`): two required corrections were identified — both citation-location defects, not substantive ones — applied, and confirmed by a Defect Confirmation Review (`docs/reviews/PROGRAMME_3_UNIT_9_PERMISSION_ENFORCEMENT_MECHANISM_CLARIFICATION_DEFECT_CONFIRMATION_REVIEW.md`), with no regression found. This document performs exactly the act the Unit 9 Knowledge Retrieval Implementation Plan's own Unit 9.5 entry names as its governance precondition — resolving the concrete permission-enforcement *mechanism* for an already-classified proposal class — and nothing else. It does not reopen the adopted `docs/governance/PROGRAMME_3_UNIT_9_SCOPE_LOCK_CLARIFICATION.md` ("the Unit 9 Clarification"), which classified Knowledge Retrieval as a PermissionEngine proposal class and is treated here as settled, frozen authority. It does not reopen the adopted `docs/governance/PROGRAMME_3_UNIT_9_KNOWLEDGE_RETRIEVAL_CONTRACT_DESIGN.md` ("the Unit 9 Contract Design"). It does not amend Chapter 10, CDR-005, Contract Design V2, the Scope Lock, or any prior Unit Clarification, all of which remain frozen and unchanged. No Kotlin is implemented, proposed as a diff, or changed by this document. Neither `src/` nor `tests/` is touched. Nothing is staged, committed, or pushed.

# Programme 3 — Unit 9 Permission Enforcement Mechanism Clarification

Programme: **Programme 3 — Knowledge Memory, Unit 9 Permission Enforcement Mechanism Clarification (the governance precondition named by the Unit 9 Implementation Plan's own Unit 9.5 entry).**

## 1. Status and Authority

This document is the "prior, narrower, implementation-facing Unit 9 Scope Lock Clarification" the Unit 9 Knowledge Retrieval Implementation Plan's own Unit 9.5 entry names by that exact description and requires to exist and be adopted before Unit 9.5 may begin: *"Implement whichever enforcement mechanism — Knowledge Retrieval self-gating (mirroring `DefaultKnowledgeSubmission`'s own precedent) or external gating by Runtime (mirroring `PermissionFilteredMemoryRetrieval`'s own precedent) — a prior, narrower, implementation-facing Unit 9 Scope Lock Clarification determines... This unit may not begin until that narrower Clarification exists and is adopted."* Its closest and most directly analogous precedent is `docs/governance/PROGRAMME_3_UNIT_8_SCOPE_LOCK_CLARIFICATION.md` ("the Unit 8 Clarification"), which resolved the identical category of question (enforcement location, principal, resource/action disclosure, evaluation order, denial disposition) for Knowledge Submission's own Evaluation B before Unit 8 implementation began. This document follows that Clarification's own structure and discipline throughout, adapted for retrieval rather than submission.

## 2. Repository Baseline

- **HEAD:** `8b929c223ba11af5df66da061373239002b12269` (`8b929c2`)
- **Branch:** `main`
- **Working tree, confirmed before drafting:** clean. Units 9.1 through 9.4 accepted, committed, and pushed.
- **Staged changes:** none.

## 3. Purpose

To resolve, once and consistently, the concrete mechanics the adopted Unit 9 Clarification and the adopted Unit 9 Contract Design each deliberately left open: where the permission gate for Knowledge Retrieval lives; at what granularity it is evaluated; what resource and action it names; how a denial is represented; and in what order these steps occur relative to matching, lifecycle shaping, and staleness disclosure. This document decides the *mechanism* for an act already, positively classified as requiring Permission Engine authorisation — it does not perform, reopen, or revisit that classification.

## 4. Constitutional Basis

This document is bound by, and does not relitigate:

- **Chapter 10** (`docs/architecture/10-permission-engine.md`), Sections 1–9 — the Permission Engine's sole authority, the four constitutional outcomes, the fail-closed guarantee, and the "separation from domain evaluation" boundary.
- **CDR-005** (`docs/decisions/CDR-005_CONSTITUTIONAL_ADMISSION_OF_PERMISSIONENGINE_PROPOSAL_CLASSES.md`), Model C (Governed Admission) — already applied to Knowledge Retrieval by the adopted Unit 9 Clarification (Section 13, below, addresses this record's own disclosed Draft status).
- **The adopted Unit 9 Clarification** — Knowledge Retrieval's own positive proposal-class classification, treated here as fixed. Section 8 of that document identifies self-gating-versus-external-gating as open and explicitly reserves it to "a future, narrower Unit 9 implementation-facing Clarification, mirroring the Unit 8 precedent" — precisely the document this is.
- **The adopted Unit 9 Contract Design**, Section 5 (Permission Boundary) and Section 9 (Error Model) — both quoted and applied throughout this document, neither altered.
- **The Unit 8 Clarification** — read in full as this document's own closest, structurally analogous precedent (Section 1, above).

## 5. The Governed Act

Exactly as the adopted Unit 9 Clarification already, fully defines it, not redefined here: a caller issues a `KnowledgeRetrievalQuery`; Knowledge Retrieval answers with a `KnowledgeRetrievalResult` — zero or more `KnowledgeResultEntry` values, each carrying a `KnowledgeItem` and a mandatory `StalenessDisclosure`. This document adds no new field, criterion, or capability to that act; it fixes only how the already-classified act is gated.

It does **not** govern, and this document does not authorise it to be read as governing:

- Knowledge Submission's own Evaluation B — the Unit 8 Clarification's own, closed, already-implemented responsibility, not reopened here;
- revision, retirement, restoration, or supersession evaluation — Unit 7's own closed responsibility, explicitly classified by Unit 7 Clarification §13 as not requiring Permission Engine evaluation at all, unaffected by anything below;
- matching, lifecycle shaping (Unit 9.4), or staleness disclosure (Unit 9.3) — each remains exactly as those Units already, separately fixed; this document adds a gate around their existing behaviour, never a change to it;
- runtime composition — wiring the eventual gated implementation into `src/composition/ParkerRuntime.kt` is Unit 9.6's own, later, separately authorised responsibility, not begun, addressed, or authorised here;
- Reasoning Context's own consumption of Knowledge Retrieval — Programme 4's own, separately governed act (Scope Lock §4), untouched by this document.

## 6. Enforcement Location and Granularity

**Freeze: Knowledge Retrieval self-gates, at two distinct granularities — once per query (an act-level gate) and, where the act-level gate approves, once per structurally-matched-and-lifecycle-eligible candidate item (a per-item gate) — never externally gated by a Runtime-composed decorator.**

### 6.1 Self-gating, not external gating

The Unit 8 Clarification §5 already establishes the governing test from this repository's own two existing precedents: `MemoryCore`/`MemoryRetrieval` are constitutionally forbidden from holding a `PermissionEngine` reference and are therefore gated externally, because that interface serves many operations reachable by many independent callers; `EvidenceCustodian` and `DefaultKnowledgeSubmission` each hold `PermissionEngine` directly and self-gate, because each is "a single, self-contained, accept-shaped boundary with one accepting responsibility." Applying that same test to Knowledge Retrieval: the Unit 9 Contract Design §4 fixes "the retrieval interface" as "exactly one operation" — no second, batching, or streaming operation is authorised. This is structurally the `EvidenceCustodian`/`DefaultKnowledgeSubmission` shape, not the `MemoryCore`/`MemoryRetrieval` shape. No text anywhere in Contract Design V2, the Scope Lock, the Unit 9 Contract Design, or the Unit 9 Clarification imposes a Memory-Core-style prohibition on Knowledge Retrieval holding `PermissionEngine`. The adopted Unit 9 Clarification's own Section 8 already observes this directly, without deciding it: Contract Design V2 §12's own description of Knowledge Retrieval — "the single public path through which a Knowledge Query is answered" — "reads closer to the first shape," i.e., the self-gating shape.

**Freeze:** the default `KnowledgeRetrieval` implementation holds and invokes `PermissionEngine` directly. Enforcement is not performed by a dedicated permission-gating decorator surrounding a permission-unaware implementation. `PermissionEngine`'s own existing interface (`src/interfaces/PermissionEngine.kt`) is reused entirely unchanged — no new method, overload, or parameter is introduced, required, or contemplated by this document.

### 6.2 Two granularities, not one — both constitutionally required

This is the one genuinely new design question this document resolves that the Unit 8 Clarification never had to: Knowledge Submission gates exactly one candidate per call, so "self-gating" and "gated once per call" were the same fact. `retrieve` can return many items in one call, so "where the gate lives" and "how many times it evaluates" are two separate questions, and both must be answered.

**A pure per-query gate is insufficient.** If Knowledge Retrieval evaluated permission only once per query — "may this principal call `retrieve` at all" — an approved principal would see every structurally matching, lifecycle-eligible item regardless of that item's own sensitivity. The adopted Unit 9 Clarification's own reasoning for classifying Knowledge Retrieval as a proposal class in the first place is explicitly anchored to *per-item* disclosure risk, not merely act-level access: *"A caller unable to see certain Memory Core evidence directly, through `PermissionFilteredMemoryRetrieval`'s own per-record gate, would gain an indirect view of that evidence's existence and evidential standing through an ungated Knowledge Result referencing it — precisely the kind of disclosure Chapter 10 §3's criterion exists to catch."* A per-query-only gate would leave exactly this disclosure ungated for every item once the query itself is approved, contradicting the reasoning that justified gating Knowledge Retrieval at all.

**A pure per-item gate is also insufficient.** The adopted Unit 9 Contract Design §9 fixes, as a binding requirement: *"Permission denial. A requesting principal not authorised to perform the retrieval act itself (Section 5) is a distinct outcome from a well-formed, authorised query that matches nothing... A caller receiving an empty Knowledge Result must be able to trust that the query was valid, the data was available, and permission was granted — not merely infer this from the absence of an error."* `KnowledgeRetrievalDisposition` (Unit 9.1, frozen) already carries exactly the two variants this requires: `Retrieved(result)` and `NotAuthorised(reason)`. If permission were evaluated only per item — silently filtering non-approved items out of `entries`, exactly as `PermissionFilteredMemoryRetrieval` already does for Memory Core records — a principal denied every single matching item would receive an ordinary `Retrieved` with an empty result, indistinguishable from a query that genuinely matched nothing. `NotAuthorised` would never be reachable in practice, and the caller could no longer "trust that... permission was granted" from an empty result, directly violating Contract Design §9's own binding text.

**Freeze — the two-tier mechanism, synthesising two already-established repository patterns rather than inventing a new one:**

1. **Act-level gate**, mirroring `DefaultKnowledgeSubmission`'s own single-fixed-resource self-gating pattern exactly: evaluated **exactly once per `retrieve` call**, before any read of `KnowledgeItemPersistence` occurs, using the fixed resource/action pair Section 7 names. If the decision is not `APPROVED` or `APPROVED_WITH_CONFIRMATION`, `retrieve` returns `KnowledgeRetrievalDisposition.NotAuthorised` immediately — no persistence read, no matching, no lifecycle shaping, and no staleness computation occurs on this path, mirroring the Unit 8 Clarification §8's own "stop immediately on denial... no read... occurs on this path" discipline exactly.
2. **Item-level gate**, mirroring `PermissionFilteredMemoryRetrieval`'s own per-record filtering pattern: where the act-level gate approves, each candidate `KnowledgeItem` that already survives Unit 9.2's structural matching and Unit 9.4's lifecycle shaping receives its own, separate evaluation, using the *same* fixed resource/action pair (Section 7 explains why a second, item-varying identifier is not required). An item whose evaluation is not `APPROVED` or `APPROVED_WITH_CONFIRMATION` is silently excluded from `KnowledgeRetrievalResult.entries` — never surfaced as a distinguishable per-item denial, exactly as `PermissionFilteredMemoryRetrieval`'s own "filtering out anything not approved" precedent already establishes for Memory Core records, and consistent with Chapter 10 §8's own "their own substantive evaluation remains separate and downstream" principle applied at the disclosure layer.

These are two evaluations of two genuinely different questions — "may this principal use Knowledge Retrieval at all" and "may this principal see this specific item" — never a redundant, duplicate evaluation of the same proposal. A single query therefore evaluates `PermissionEngine.evaluate` **exactly** `1` time if the act-level gate denies, or **exactly** `1 + N` times if it approves, where `N` is the number of items surviving matching and lifecycle shaping before bounding (Section 8 explains the ordering this formula depends on). No implementation may evaluate any item more than once, and no implementation may skip the act-level gate under any circumstance, including when the matched set is empty.

## 7. Resource and Action Disclosure

- **Resource identity:** a single, fixed resource identity representing the Knowledge Retrieval boundary itself, mirroring `DefaultKnowledgeSubmission.KNOWLEDGE_SUBMISSION_RESOURCE_ID`'s own exact naming and treatment — not a per-item resource. A `KnowledgeItem` has no Resource Registry identity of its own, exactly as a `KnowledgeCandidate` does not (Unit 8 Clarification §7).
- **Action name:** a single, fixed action name representing "retrieve knowledge," following this repository's own established dotted-namespace convention (`evidence.accept`, `evidence.retrieve`, `knowledge.submit`, `memory.retrieve`). This document names the convention it must follow, not a literal string constant, consistent with the hard constraint against drafting Kotlin identifiers.
- **One pair, evaluated at two granularities, not two pairs.** Section 6.2's act-level and item-level gates use the *same* resource/action pair. This mirrors, rather than departs from, `PermissionFilteredMemoryRetrieval`'s own established precedent: that class evaluates the *same* fixed action name (`RETRIEVE_ACTION_NAME`) once for every one of potentially many Memory Core records of the same kind, never minting a new action identifier per record. A `KnowledgeItem`'s own content is no more relevant to *which* action name names the request than a Memory Core record's own content is to `PermissionFilteredMemoryRetrieval`'s choice of action name — only *how many times*, and *against which principal*, the same fixed pair is evaluated varies. Inventing a second, distinct action name for the item-level tier would create a Knowledge Memory permission vocabulary broader than this document's own minimal, disclosed authority (Section 11).
- **Fixed, not query-specific or item-specific.** One resource identity, one action name, evaluated identically for every query and every candidate item, regardless of query criteria or item content.
- **Registration:** these identifiers follow the repository's own, already-established, repeatedly-used disclosed-but-unregistered precedent (`EvidenceCustodian`'s own identifiers; `DefaultKnowledgeSubmission`'s own identifiers). They do not require registration in any `ActionVocabulary`/`ResourceRegistry` before Unit 9.5 may be implemented; their omission is not itself a defect. Registration remains a future Runtime-integration-phase responsibility.

**This document authorises no broader Knowledge Memory permission framework than this one resource/action pair, for this one act, evaluated at the two granularities Section 6.2 fixes.** No future Knowledge Memory act gains any permission-gating authority, expectation, or precedent from this document.

## 8. Evaluation Order and Count

The following order is frozen, and no implementation may reorder, parallelise past, or collapse any step:

1. **Receive the retrieval request** — the `KnowledgeRetrievalQuery` and the `requestingPrincipalId` (Unit 9.1, unchanged).
2. **Evaluate the act-level gate** (Section 6.2's own first item), using the fixed resource/action pair (Section 7) and the `ExecutionRequest.correlationId` propagation Section 9 requires. This step must never read `KnowledgeItemPersistence` before it completes.
3. **Stop immediately on any outcome other than `APPROVED` or `APPROVED_WITH_CONFIRMATION`** (mirroring Chapter 10 §4's four constitutional outcomes and §9's fail-closed treatment of a `Deferred` or unresolved decision as never authorising): return `KnowledgeRetrievalDisposition.NotAuthorised` immediately. No persistence read, no matching, no lifecycle shaping, and no staleness computation of any kind occurs on this path.
4. **Read `KnowledgeItemPersistence.findAll()`**, exactly as Unit 9.2 already does, unchanged.
5. **Apply structural matching (Unit 9.2) and lifecycle shaping (Unit 9.4)**, exactly as those Units already, separately fixed, unchanged by this document.
6. **Evaluate the item-level gate (Section 6.2's own second item) for each item surviving step 5, in the same order Unit 9.2's own ordering guarantee already fixes, before bounding.** This ordering — permission filtering before `maximumResults` bounding, not after — is a disclosed design choice, not compelled by any single existing precedent, and its own reasoning is stated here rather than left implicit: bounding after permission filtering ensures a caller who receives fewer than `maximumResults` entries can trust this reflects genuinely fewer visible items, not an artefact of a bound applied before visibility was known — the same "a caller must be able to trust what a result honestly represents" principle Contract Design §9 already applies to the empty-result case, extended here to the bounded case. The disclosed cost of this ordering, stated plainly rather than concealed: an implementation may evaluate permission for more items than are ultimately returned, whenever more than `maximumResults` items survive step 5 for an approved principal. This is a bounded, disclosed cost, not a caching, indexing, or optimisation question of the kind Contract Design §11 excludes from scope.
7. **Bound the surviving, permission-approved set to `maximumResults`**, exactly as Unit 9.2 already does, unchanged.
8. **Compute staleness disclosure (Unit 9.3) only for the final, bounded, permission-approved set.**
9. **Return `KnowledgeRetrievalDisposition.Retrieved`** with the resulting `KnowledgeRetrievalResult`.

**Verification consequence, stated explicitly for the future implementer:** for a query where the act-level gate denies, `PermissionEngine.evaluate` is called exactly once and `KnowledgeItemPersistence.findAll()` is never called. For a query where the act-level gate approves and exactly `N` items survive structural matching and lifecycle shaping, `PermissionEngine.evaluate` is called exactly `1 + N` times, regardless of `maximumResults`. No item is ever evaluated more than once; no evaluation is ever skipped for an item that survives step 5.

## 9. Principal and Correlation Identifier Propagation

**Principal:** `requestingPrincipalId: PrincipalId` is already Unit 9.1's own, frozen, explicit, non-ambient parameter to `KnowledgeRetrieval.retrieve`. This document introduces no second source of identity and requires none — every `ExecutionRequest` this Unit constructs, at either granularity, carries the same caller-supplied `requestingPrincipalId` unchanged, mirroring `DefaultKnowledgeSubmission`'s and `DefaultEvidenceCustodian`'s own identical treatment.

**Correlation identifier — a genuine departure from the Unit 8 precedent, disclosed here rather than silently copied.** `DefaultKnowledgeSubmission` mints its own, freshly generated correlation identifier when constructing an `ExecutionRequest`, because `KnowledgeCandidate`'s own frozen field list (Unit 5) carries no correlation identifier of its own to propagate. `KnowledgeRetrievalQuery` is different: the adopted Unit 9 Contract Design §4 explicitly added `correlationId` to it for exactly this purpose — *"a Knowledge Query must additionally be capable of carrying an explicit correlation identifier, sufficient to correlate the retrieval request with its own permission evaluation... This identifier must be explicit, never ambient or inferred."* **Freeze:** every `ExecutionRequest` this Unit constructs, at either the act-level or the item-level granularity, must set `ExecutionRequest.correlationId` to the caller-supplied `KnowledgeRetrievalQuery.correlationId`, never a freshly minted value. Where more than one `ExecutionRequest` is constructed for the same query (the item-level gate, evaluated once per surviving item), every one of them carries the *same* `query.correlationId` — the correlation identifier correlates the entire retrieval request, not one particular permission evaluation within it, mirroring the "sufficient to correlate the retrieval request with its own permission evaluation" language's own singular framing of one request to (potentially) several evaluations.

## 10. Permission Denial Disposition

**No new type is introduced.** `KnowledgeRetrievalDisposition` (Unit 9.1, frozen) already carries exactly the two-variant shape this mechanism requires: `Retrieved(result: KnowledgeRetrievalResult)` and `NotAuthorised(reason: String)`. This document authorises no widening of either variant and no third variant.

- **Act-level denial** is expressed as `KnowledgeRetrievalDisposition.NotAuthorised`, with a non-blank `reason` disclosing that the retrieval act itself was not authorised (mirroring `DefaultEvidenceCustodian`'s and `DefaultKnowledgeSubmission`'s own existing reason-string conventions) — never the Permission Engine's own internal decision detail, consistent with `PermissionEngine.explain`'s own separate, dedicated purpose.
- **Item-level denial** is expressed as filtering — the denied item is simply absent from `KnowledgeRetrievalResult.entries`, indistinguishable, at the type level, from an item that never structurally matched or was excluded by lifecycle shaping. This is not a concealment defect: Contract Design §9's own distinguishability requirement concerns the *query-level* outcome categories (invalid query, unavailable data, permission denial, empty result, implementation failure) — it does not require every possible *reason* a specific item is absent from a non-empty, `Retrieved` result to be separately disclosed, and no governing document requires this. This mirrors `PermissionFilteredMemoryRetrieval`'s own established, already-accepted precedent exactly (Errata 004 §8: "This class introduces no new denial type, sentinel, or thrown exception on this path -- doing so would itself leak which case occurred").

**No exception-based denial mechanism is authorised for either granularity.** A thrown exception on denial would be inconsistent with `KnowledgeRetrievalDisposition`'s own already-fixed, two-variant, non-throwing shape, and inconsistent with `DefaultKnowledgeSubmission`'s and `DefaultEvidenceCustodian`'s own identical, already-accepted "return a disposition, do not throw" discipline for an ordinary permission denial. This document does not disturb the existing, unrelated "faults propagate, never silently swallowed" discipline for genuine implementation failures (Contract Design §9's own fifth outcome) — that concerns a different failure category entirely and is untouched here.

## 11. Explicit Non-Expansions

For the avoidance of doubt, this document does **not** authorise:

- any new `PermissionAction` or `ResourceType` value, or any second resource/action pair beyond the single one Section 7 names;
- any change to `PermissionEngine`'s own interface, method signature, or outcome vocabulary;
- any change to `KnowledgeRetrievalQuery`, `KnowledgeResultEntry`, `KnowledgeRetrievalResult`, or `KnowledgeRetrievalDisposition`'s own already-frozen field shapes (Units 9.1, 9.3, 9.4) — this document relies on all four exactly as they already exist;
- any change to matching (Unit 9.2), lifecycle shaping (Unit 9.4), or staleness disclosure (Unit 9.3) — this document wraps their existing, unchanged behaviour with a gate, never alters it;
- any Memory Core or Memory Retrieval dependency of any kind;
- any ranking, semantic matching, or "best item" selection of any kind;
- any runtime composition of any kind — wiring the eventual implementation into `ParkerRuntime.kt` remains Unit 9.6's own, later, separately authorised responsibility;
- any Reasoning Context integration of any kind;
- any Knowledge Memory permission framework broader than the single resource/action pair this document names, evaluated at the two granularities Section 6.2 fixes, for this single act.

## 12. Programme Boundary

This document reaffirms, without redesigning anything Programme 4 or a later Unit owns:

- **Programme 3 owns Knowledge Retrieval's own permission mechanism exclusively** — its enforcement location, its granularity, its resource/action disclosure, and its denial disposition, all fixed above.
- **Runtime composition — wiring a gated Knowledge Retrieval implementation into the running system** — is Unit 9.6's own, later, separately authorised responsibility. Neither begun, addressed, nor authorised by this document.
- **Reasoning Context's own consumption of Knowledge Retrieval** remains Programme 4's own, separately governed act (Scope Lock §4), untouched by this document.

## 13. CDR-005 Model C — Not Reopened, Only Cited

This document does not perform a CDR-005 Model C self-certification of its own, because none is required: Knowledge Retrieval's own proposal-class classification was already, positively performed by the adopted Unit 9 Clarification, itself already checked against Chapter 10's admission criteria and found not genuinely contested (Unit 9 Clarification §13). This document resolves *mechanism* for an already-admitted proposal class — a question CDR-005 itself expressly places outside its own scope (CDR-005's own "Non-Decisions": *"any implementation interface, Kotlin type, or method signature for PermissionEngine or any request representation... Runtime's own wiring or composition-root sequencing... the content of any permission policy"*). Resolving mechanism for an already-classified act is squarely the kind of question the Unit 8 Clarification already resolved, by the same method, for Knowledge Submission, without itself requiring a fresh CDR-005 self-certification beyond the one Contract Design V2 §7 later performed for that act. No genuine contest or ambiguity exists here requiring CDR escalation: this document's own reasoning throughout (Sections 6–10) is grounded in direct, checked analogy to two already-accepted repository precedents, never in a novel or contested reading of Chapter 10's own criteria.

## 14. Disclosure: Chapter 10 and CDR-005's Own Freeze Status

**Stated candidly, not concealed, mirroring the Unit 8 Clarification's own identical disclosure exactly.** Both Chapter 10 and CDR-005 remain, in their own self-declared status headers, **Draft** — Chapter 10: *"a draft, prepared to complete Chapter 10's own position in Parker's numbered architecture sequence, pending independent constitutional review and a Final Freeze Verification... It is not yet frozen."* CDR-005: *"Draft. This record is not Accepted, not Canonical, and not Frozen... has not yet undergone the independent constitutional verification and Final Freeze Verification cycle this Programme has applied to every other governance artefact."*

This document relies on both, exactly as the Unit 9 Clarification and the Unit 8 Clarification before it already did, as the best-available, and only existing, governance mechanism for the admission question — though this document itself performs no fresh admission act (Section 13). This is now (at minimum) the fourth such reliance in this repository's own disclosed lineage — Evidence Intelligence, Unit 8 (Knowledge Submission), the Unit 9 Clarification (Knowledge Retrieval's own admission), and now this document (Knowledge Retrieval's own mechanism) — each disclosed on the same terms rather than concealed. **Completing CDR-005's and Chapter 10's own Final Freeze Verification is a distinct, separately-scoped governance task this document does not perform**, and this document's own validity does not depend on that broader task completing first, exactly as each prior reliance was not blocked by it either.

## 15. Scope of This Clarification

This document resolves only the points Sections 6–10 address, plus the CDR-005 disclosure (Section 13) and its own disclosed freeze-status limitation (Section 14). It does not authorise any change to `KnowledgeRetrievalQuery`'s, `KnowledgeResultEntry`'s, `KnowledgeRetrievalResult`'s, or `KnowledgeRetrievalDisposition`'s own already-frozen shapes, Unit 9.2's own matching or ordering behaviour, Unit 9.3's own staleness mechanism, Unit 9.4's own lifecycle-shaping default, or any work belonging to Unit 9.6 or Programme 4. It creates no new constitutional doctrine and reopens no prior Programme 3 decision.

## 16. Verification Required of the Future Unit 9.5 Implementation

Named here so the dependency is visible, not performed by this document: a structural test proving the default `KnowledgeRetrieval` implementation holds a `PermissionEngine` dependency directly (mirroring `DefaultKnowledgeSubmissionTest`'s own precedent); a test proving a denied act-level decision returns `NotAuthorised` and never reaches `KnowledgeItemPersistence` (mirroring `DefaultKnowledgeSubmissionTest`'s own "the evaluator is never invoked" pattern, adapted to persistence); a test proving the exact evaluation-count formula Section 8 fixes (`1` on act-level denial; `1 + N` on act-level approval, using a fake `PermissionEngine` exposing an `evaluateCallCount`, mirroring `DefaultKnowledgeSubmissionTest`'s own existing fake); a test proving `requestingPrincipalId` and `query.correlationId` are propagated unchanged into every constructed `ExecutionRequest`; a test proving permission denial remains distinguishable from an empty, authorised result at the disposition level (Contract Design §9); a test proving matching, lifecycle shaping, ordering, and staleness disclosure are byte-identical to their pre-Unit-9.5 behaviour for every item that survives both gates; a test proving no `MemoryCore`/`MemoryRetrieval` dependency exists.

---

## Disposition

```
UNIT 9 PERMISSION ENFORCEMENT MECHANISM CLARIFICATION -- ADOPTED
READY FOR UNIT 9.5 IMPLEMENTATION, ON THE ENFORCEMENT-MECHANISM DIMENSION
```

Independent constitutional review is complete and found no substantive conflict (`docs/reviews/PROGRAMME_3_UNIT_9_PERMISSION_ENFORCEMENT_MECHANISM_CLARIFICATION_INDEPENDENT_CONSTITUTIONAL_REVIEW.md`); two citation-location corrections were applied and confirmed (`docs/reviews/PROGRAMME_3_UNIT_9_PERMISSION_ENFORCEMENT_MECHANISM_CLARIFICATION_DEFECT_CONFIRMATION_REVIEW.md`). Section 14's disclosed limitation stands: Chapter 10 and CDR-005 themselves remain Draft, pending their own, separately-scoped Final Freeze Verification, which this document does not perform and does not require in order to be relied upon. Unit 9.5 implementation may now proceed on the enforcement-mechanism dimension this document settles; groundwork not dependent on it may proceed in parallel, consistent with the preceding Unit 8 Clarification's own identical disposition.

## 17. Final Git Status

```
$ git status --short
?? docs/governance/PROGRAMME_3_UNIT_9_PERMISSION_ENFORCEMENT_MECHANISM_CLARIFICATION.md
?? docs/reviews/PROGRAMME_3_UNIT_9_PERMISSION_ENFORCEMENT_MECHANISM_CLARIFICATION_DEFECT_CONFIRMATION_REVIEW.md
?? docs/reviews/PROGRAMME_3_UNIT_9_PERMISSION_ENFORCEMENT_MECHANISM_CLARIFICATION_INDEPENDENT_CONSTITUTIONAL_REVIEW.md
```

Nothing staged, committed, or pushed.
