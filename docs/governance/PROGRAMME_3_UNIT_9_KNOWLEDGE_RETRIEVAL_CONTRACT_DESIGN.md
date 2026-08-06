**Status:** Contract design only. **Adopted.** Independent constitutional review is complete (`docs/reviews/PROGRAMME_3_UNIT_9_KNOWLEDGE_RETRIEVAL_CONTRACT_DESIGN_INDEPENDENT_CONSTITUTIONAL_REVIEW.md`): four required corrections were identified, applied, and confirmed by a Defect Confirmation Review, with no regression found. Not an amendment, not a Scope Lock, not an Implementation Plan, not an implementation. No Kotlin is implemented, proposed as a diff, or changed by this document. No schema, file format, serializer, resource identifier, action string, or Kotlin type is fixed by this document except where explicitly stated that governance already requires one. Neither `src/` nor `tests/` is touched. Nothing is staged, committed, or pushed.

**Governing vehicle.** This document is a new, dedicated Contract Design — sibling to, not an amendment of, `docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_CONTRACT_DESIGN_V2.md` ("Contract Design V2"). It elaborates, in the detail Contract Design V2 §13 itself reserved for a later pass ("the specific mechanism for staleness detection... [is a] Scope Lock or implementation decision — this document fixes that each must exist and be applied consistently, not how"), the Knowledge Retrieval interface Contract Design V2 §6 and §12 already named but did not specify to implementation-planning depth. It does not redesign, reopen, or reinterpret Contract Design V2, the Scope Lock, `docs/governance/PROGRAMME_3_UNIT_9_SCOPE_LOCK_CLARIFICATION.md` ("the Unit 9 Clarification," Adopted), or any prior Unit Clarification. Every determination below either restates an already-frozen requirement at greater precision or fixes a genuinely new, narrow requirement traceable to those frozen documents — nothing here is asserted from preference.

The Unit 9 Clarification's own Section 15 (Recommendation) named a future Contract Design V2 amendment — recording the Clarification's own permission classification within Contract Design V2 itself, mirroring how Amendment 8 recorded Evaluation B — as its own expected next step. This document proceeds differently: as a separate, sibling Contract Design, with Contract Design V2's own text left entirely unamended. The distinction is deliberate, not an oversight. Amendment of Contract Design V2 would mean altering that document's own frozen text — its Contract Inventory, its Public Model, or its Amendment Validation record — to newly state the permission classification there; that path remains available but is not taken here. Elaboration by a dedicated sibling contract, the path this document takes instead, adds contract-tier detail (Sections 1–12, below) alongside Contract Design V2 without touching a single word of it, exactly as `docs/architecture/MEMORY_CORE_DURABILITY_CONTRACT_DESIGN.md` already elaborated Memory Core durability requirements without amending `MEMORY_CORE_CONTRACT_DESIGN.md`. This preserves Contract Design V2's own existing frozen text completely — every section, every amendment record, and every guarantee it already states remains exactly as adopted, with nothing in this document capable of being mistaken for a change to it. It also does not reopen the Unit 9 Clarification: the Clarification's own classification is treated as settled authority this document cites and builds upon (Section 5, below), never re-examined, re-argued, or re-derived, regardless of which document eventually records that classification inside Contract Design V2's own text. Recording the classification within Contract Design V2 itself, if and when that amendment is made, remains a lawful, independent future step this document neither performs nor forecloses.

**Repository baseline treated as authoritative for this document:** `main` at commit `b04710c` ("docs: clarify Unit 9 knowledge retrieval governance"), at which all Unit 9 governance work — the planning reviews, the Clarification, its Independent Constitutional Review, its Defect Confirmation Review, and its adoption — is accepted and binding.

---

## Context and Constitutional Basis

Contract Design V2 §12's own Contract Inventory already fixes Knowledge Retrieval's outline: "the single public path through which a Knowledge Query is answered... Performs no write of any kind; answers strictly from already-promoted Knowledge Items, always with staleness disclosure... Dependency: Memory Core (for forwarded, minimal, immutable provenance references only)." The Unit 9 Clarification settles, separately and already-adopted, that Knowledge Retrieval is a PermissionEngine proposal class — a positive classification this document treats as fixed, not open. What remains unfixed, and what this document fixes, is the contract-level detail an Implementation Plan and Scope Lock will need: the precise shape and behaviour of Knowledge Query and Knowledge Result, the ordering and error-model guarantees a caller may rely on, exactly what lifecycle and provenance disclosure retrieval guarantees, and the boundary between what Knowledge Retrieval itself owns and what Runtime owns around it.

**The constitutional principle governing every determination below**, restated from Contract Design V2's own governing principle and applied one layer deeper: *Knowledge Memory exposes knowledge; it never exposes storage mechanics, never duplicates Memory Core's responsibilities, and never owns provenance, identity, or relationships.* Retrieval is the one layer of Knowledge Memory where this principle is tested against an actual, external caller — every requirement below exists to keep that exposure honest, bounded, and non-leaking.

---

## 1. Purpose

Knowledge Retrieval exists to answer a caller's task-scoped request for relevant, already-promoted knowledge — issuing a Knowledge Query and receiving, in return, a Knowledge Result disclosing zero or more Knowledge Items or Knowledge References, each with its evidential-state classification, its provenance reference, and a mandatory staleness disclosure. It performs no write of any kind. It is the sole public path through which anything outside Knowledge Memory may observe promoted knowledge.

**Distinguished from adjacent acts, precisely:**

- **Knowledge Submission** (`docs/governance/PROGRAMME_3_UNIT_8_SCOPE_LOCK_CLARIFICATION.md`) is the write boundary — it accepts a Knowledge Candidate and, on approval, causes a new Knowledge Item to exist. Retrieval never causes anything to exist; it only discloses what already does.
- **Promotion** (Contract Design V2 §3, §5) is the internal evaluation act that decides whether a submitted candidate becomes durable knowledge. Retrieval never evaluates, decides, or promotes; it answers queries about the outcome of promotion, never participates in it.
- **Lifecycle** (revision, supersession, retirement, restoration — Contract Design V2 §3; `docs/governance/PROGRAMME_3_UNIT_7_SCOPE_LOCK_CLARIFICATION.md`) is Knowledge Memory's own internal computation over an already-promoted Knowledge Item's history. Retrieval discloses the current and historical state lifecycle produces; it never triggers, computes, or influences a lifecycle transition.
- **Reasoning (Context Assembly)** (Contract Design V2 §8) is a *consumer* of Knowledge Retrieval, not Knowledge Retrieval itself. Reasoning Context issues a Knowledge Query and renders whatever Knowledge Result it receives; composing that rendering, choosing what else to include alongside it, and any interpretation of retrieved content belong entirely to Reasoning Context, never to Knowledge Retrieval.
- **Memory Retrieval** (`MemoryRetrieval`, Memory Core's own interface) operates one layer below, over Memory Core's own raw record kinds (`Entity`, `Document`, `Assertion`, `Relationship`). Knowledge Retrieval never performs a Memory Core query of its own — its only lawful contact with Memory Core is forwarding an already-issued, minimal, immutable provenance reference (Contract Design V2 §6, §12).
- **Evidence retrieval** — read, for the purposes of this document, as the same capability named "Memory Retrieval" above, under Memory Core's own vocabulary. The distinction is identical: evidence retrieval answers queries about Memory Core's own records; Knowledge Retrieval answers queries about Knowledge Memory's own promoted, evaluated records. Knowledge Retrieval never substitutes for, wraps, or re-exposes evidence retrieval's own query surface.

---

## 2. Responsibilities

Knowledge Retrieval is responsible for:

- **Retrieving Knowledge Item records** already promoted and held by Knowledge Memory's own store, in response to a Knowledge Query.
- **Query execution** — matching a Knowledge Query's own caller-supplied criteria against Knowledge Memory's own held state, structurally (Contract Design V2 §13; Scope Lock §4: "structural matching against caller-supplied criteria only," never semantic or similarity-based).
- **Filtering** — returning only Knowledge Items or Knowledge References genuinely matching the Knowledge Query's own criteria; filtering is a function of query criteria and permission outcome only, never of any ranking heuristic (Section 3, below).
- **Ordering** — see Section 8.
- **Ranking, if any** — bounded strictly to Section 8's own deterministic-ordering guarantee. No scoring, weighting, relevance-ranking, or similarity-ranking of any kind is a Knowledge Retrieval responsibility (Contract Design V2 §13; Section 3, below).
- **Provenance disclosure** — see Section 7.
- **Lifecycle visibility, revision visibility, restoration behaviour** — see Section 6.
- **Staleness disclosure** — a mandatory, always-present component of every Knowledge Result entry that includes a Knowledge Item or Knowledge Reference (Contract Design V2 §3, §6, Amendment 7). Never optional, never inferred from its own absence.

---

## 3. Non-Responsibilities

Knowledge Retrieval is explicitly not responsible for, and must not be implemented to perform:

- **Memory Core's own responsibilities** — identity, provenance content, relationships, or any Memory Core record-level query. Knowledge Retrieval forwards a provenance *reference* only (Section 7); it never queries, reads, or reasons over Memory Core content directly.
- **Evidence Intelligence** — Knowledge Retrieval has no dependency on, and no awareness of, Evidence Intelligence's own analysis pipeline. Nothing about how a Knowledge Item's underlying evidence was produced is Knowledge Retrieval's concern.
- **Reasoning Context composition** — assembling, rendering, or interpreting a Knowledge Result into user-facing or reasoning-provider-facing text belongs entirely to Reasoning Context (Contract Design V2 §8; Scope Lock §4, explicitly allocating this cutover and composition work to Programme 4).
- **Ranking algorithms beyond this contract's own deterministic-ordering guarantee** — no embeddings, vector search, semantic similarity, or scored relevance ranking is designed, implied, or permitted anywhere in this document (Contract Design V2 §13; Scope Lock §4).
- **Semantic reasoning** — Knowledge Retrieval performs structural matching only; it does not interpret a Knowledge Query's own criteria beyond matching them against held state.
- **Permission policy** — the *content* of any permission decision (what is approved, denied, or deferred, and under what owner-defined policy) is exclusively the Permission Engine's own responsibility (`docs/architecture/10-permission-engine.md` §5, §6, §8). Knowledge Retrieval's own classification as a proposal class (the Unit 9 Clarification) fixes only that a decision is required, never what that decision is or how policy is authored.
- **Runtime orchestration** — composing, wiring, or invoking Knowledge Retrieval within `ParkerRuntime.kt` is Implementation-Plan/runtime-composition-tier work, not fixed here (Section 11).
- **Persistence implementation** — no storage technology, schema, index, cache, or query-execution mechanism is named, implied, or foreclosed by this document (Section 11).
- **Knowledge Submission's own responsibilities** — accepting a candidate, evaluating it, and persisting a promotion are entirely outside Knowledge Retrieval's own scope (Section 1).

---

## 4. Public Contract

**Fixed here: properties and guarantees. Not fixed here: any Kotlin type, method signature, field name, or interface declaration.**

- **Knowledge Query** — a request contract expressing a task-scoped request for relevant, already-promoted knowledge. Per Contract Design V2 §12, it "describes what [the caller] is asking for; it carries no retrieval logic itself." This document adds no new field, criterion, or capability to Knowledge Query beyond what Contract Design V2 §2, §12 already authorise; it fixes only that whatever concrete shape Knowledge Query eventually takes must express caller-supplied structural matching criteria and nothing else — no ranking instruction, no semantic hint, no permission assertion. A Knowledge Query must additionally be capable of carrying an explicit correlation identifier, sufficient to correlate the retrieval request with its own permission evaluation (Section 5), its own result (this Section), and whatever audit or diagnostic record this repository's own established auditability discipline requires of any PermissionEngine proposal (`docs/architecture/10-permission-engine.md` §5's "Traceability and auditability" guarantee). This identifier must be explicit, never ambient or inferred, mirroring the same treatment this repository already gives every other permission-evaluated act's own identity (Errata 004; the Unit 8 Clarification §6). No field name, identifier format, or generation mechanism is fixed here — only that the capability to carry one, explicitly, is a contract-level requirement, not an implementation option.
- **Knowledge Result** — a response contract expressing what Knowledge Memory returns for a given Knowledge Query. Per Contract Design V2 §12, it "bundles Knowledge References/Items, evidential-state disclosures, unresolved-competition disclosures, provenance references, and a mandatory staleness disclosure for every included item." This document fixes, in addition: a Knowledge Result must be capable of representing zero matching items without that representation being confusable with a permission denial or an implementation failure (Section 9); a Knowledge Result must preserve whatever order Knowledge Retrieval's own implementation produced (Section 8) without the response contract itself re-ordering, deduplicating beyond identity, or truncating silently.
- **The retrieval interface** — the single public path through which a Knowledge Query is answered (Contract Design V2 §12). This document fixes: exactly one operation, accepting a Knowledge Query and a requesting principal (Section 5), and returning a Knowledge Result or a distinguishable non-result outcome (Section 9). No second operation, no batching operation, and no streaming operation is authorised by this document.
- **Request/response behaviour** — a call answers strictly from already-promoted Knowledge Items existing in Knowledge Memory's own store at the time the call is answered (Contract Design V2 §12); it never blocks pending a future promotion, and never triggers one. A call that finds nothing matching returns an empty, valid Knowledge Result — never an error, never `null` in place of a result contract, and never a fabricated entry.

---

## 5. Permission Boundary

**Settled, not reopened.** The Unit 9 Clarification (Adopted) already classifies Knowledge Retrieval as a PermissionEngine proposal class, reasoned from Chapter 10 §3's admission criteria and CDR-005's Model C self-certification. This document treats that classification as fixed and does not re-examine, re-argue, or re-derive it.

What this document adds, at Contract Design tier, consistent with and non-contradictory to the Clarification:

- **A requesting principal is a required input to every retrieval call**, explicit, never ambient or inferred — mirroring Errata 004's and the Unit 8 Clarification's own identical treatment for Memory Core and Knowledge Submission respectively, and restating the Unit 9 Clarification's own Section 11 consequence.
- **A permission denial is a distinguishable outcome**, never conflated with an empty result or any other outcome (Section 9).
- **The enforcement mechanism — whether Knowledge Retrieval self-gates or is externally gated by Runtime — is not decided by this document.** The Unit 9 Clarification's own Section 8 leaves this open, dependent on Knowledge Retrieval's eventual concrete shape; this Contract Design does not resolve it either. Resolving it belongs to a future, narrower, implementation-facing Clarification, mirroring exactly how the Unit 8 Clarification (not Contract Design V2's own text) resolved the equivalent question for Knowledge Submission.
- **No new Permission Engine concept is introduced.** No new outcome beyond the four Chapter 10 §4 already names (Approved, Denied, Deferred, Approved With Confirmation) is contemplated; no new resource type, action category, or evaluation tier is named. "Evaluation C" is not used anywhere in this document as canonical terminology, consistent with the Unit 9 Clarification's own discipline.

---

## 6. Lifecycle Behaviour

**Using exactly the lifecycle model Contract Design V2 and the current implementation already fix — no new status value or event kind is introduced.** A Knowledge Item's own `status` holds exactly one of two values at any time: active or retired (Contract Design V2 §3; the current implementation's own two-value status model, confirmed by direct inspection). "Revised" and "restored" are not status values — they are two of the four lifecycle *event kinds* (promotion, revision, retirement, restoration) Contract Design V2 §3 and §12 already fix as entries in a Knowledge Item's own append-only history.

- **Active.** An active Knowledge Item is retrievable through ordinary Knowledge Query matching, exactly as any other. No special disclosure beyond the ordinary evidential-state and staleness disclosures (Contract Design V2 §3) is required for an active item by virtue of its status alone.
- **Revised.** A revision does not change a Knowledge Item's status; it appends a new classification and a new disclosure record to the item's own history (Contract Design V2 §3, Amendment 4). Retrieval must be capable of disclosing that a revision has occurred — the item's own current classification already reflects the most recent revision — and must never present a revised item as though its classification had always been what it currently is. Whether and how prior classifications in the history are surfaced on an ordinary query, as opposed to a history-specific request, is not fixed by this document (Section 11) — only that retrieval must not conceal that history exists.
- **Superseded.** Supersession is not a separate status or a separate lifecycle event kind — Contract Design V2 §3 fixes it as a specific trigger for an ordinary revision ("the dependent Knowledge Item is re-evaluated against the superseding evidence, exactly as an ordinary revision"), and this document introduces no new state or event kind to represent it (consistent with this Section's own opening rule). A superseded classification is never erased: Contract Design V2 §3 requires that "supersession chains of arbitrary length are supported: each hop links to the Memory Core relationship that justified it, and the full chain — not merely the most recent hop — remains transitively retrievable as part of the Knowledge Item's evidential history," and this document fixes that Knowledge Retrieval must preserve that transitive retrievability — a superseded classification, and every earlier classification in the same chain, must remain reachable, never silently dropped. At contract level, a Knowledge Result distinguishes a Knowledge Item's current classification from a superseded one by the same mechanism Section 6's own "Revised" treatment already fixes: the item's current classification is the most recent entry in its own single, non-forking history, and any earlier, superseded entry remains part of that same history rather than being presented as though it were current. Which specific superseded entries an ordinary query surfaces by default, how a caller requests the full supersession chain, and any caller-facing presentation of that chain are retrieval-shape and revision-selection questions this document does not decide — the same open question Section 6's "Revised" treatment already discloses, not a new one. Nothing here selects a "latest only" retrieval policy; Contract Design V2 §3's own multi-hop retrievability requirement forecloses that policy as the sole behaviour, without this document itself prescribing what alternative behaviour an implementation must provide.
- **Retired.** Retirement never implies deletion (Contract Design V2 §3, Amendment 5); a retired Knowledge Item remains a genuine record. Whether a retired item is included in an ordinary Knowledge Query's own result set by default, excluded by default, or included only under an explicit caller criterion, is not fixed by this document — it is a retrieval-shape question left to the Implementation Plan (Section 11). What is fixed here: whichever default is chosen, a retired item, if returned at all, discloses its retired status honestly and is never presented as though it were active.
- **Restored.** Restoration appends a new event to a retired item's history and returns its status to active (Contract Design V2 §3; `docs/governance/PROGRAMME_3_UNIT_7_SCOPE_LOCK_CLARIFICATION.md` §9). A restored item is retrievable exactly as any other active item once restored; retrieval must never silently collapse the promoted→retired→restored sequence into an undifferentiated "active" presentation with no trace that retirement and restoration both occurred, if the caller is examining the item's own history rather than only its current classification.

**Fixed independently of default-inclusion policy:** lifecycle status is never a substitute for, or determinant of, a permission decision (Section 5; the Unit 9 Clarification's own Section 10). A retired item's visibility to a given requesting principal is governed by the same permission classification as any other item, never a different one by virtue of its own lifecycle status.

---

## 7. Provenance

**Guaranteed:** every Knowledge Item and Knowledge Reference disclosed by a Knowledge Result carries a provenance reference sufficient to reach its originating Memory Core provenance record (Contract Design V2 §6, Amendment 6, §11). That reference is minimal — it contains only the identifier necessary to address the underlying record, never any provenance field content (source type, acquisition time, creator, or any other attribute) — and immutable once issued: it addresses the same target for as long as it exists.

**Not guaranteed, and not this document's to invent:** Knowledge Retrieval does not generate, mint, or construct a provenance reference — that act belongs entirely to whichever process already produced the Knowledge Item being retrieved (Promotion, Contract Design V2 §3, §5). Retrieval only forwards an already-issued reference; it performs no provenance lookup, resolution, or dereferencing of its own. A caller needing full provenance detail follows the forwarded reference to Memory Core's own retrieval surface directly (Contract Design V2 §6) — Knowledge Retrieval never proxies, summarises, or embeds that detail on the caller's behalf.

---

## 8. Ordering

- **Deterministic behaviour, guaranteed.** A given Knowledge Query, issued twice against unchanged Knowledge Memory state, must return an identical Knowledge Result both times, including identical ordering (Scope Lock §7, Non-Functional Requirements: "Determinism... mirroring `MEMORY_CORE_SCOPE_LOCK.md` §11's own repeatable-retrieval guarantee, applied here... explicitly, as a binding requirement").
- **Undefined behaviour, disclosed as such.** The specific ordering rule itself — insertion order, a fixed structural tiebreak, or another deterministic scheme — is not fixed by this document. What is fixed is only that exactly one such rule must exist and be applied consistently; an implementation may not vary its ordering approach by code path, query shape, or circumstance (mirroring Scope Lock §8's identical discipline for concurrent revision ordering).
- **Ranking is not ordering.** Nothing in this section authorises, implies, or permits a relevance-scored or similarity-scored ordering; "deterministic ordering" here means only that the same structural match set is arranged the same way every time, never that matches are weighed against one another by any heuristic (Section 3).
- **Caller expectations.** A caller may rely on: the same query against unchanged state always returning the same result in the same order; the absence of any hidden randomisation, time-based variation, or load-dependent reordering. A caller may not rely on: any specific ordering rule persisting across implementation changes, or on ordering reflecting relevance, importance, or any other substantive judgment.

---

## 9. Error Model

**Fixed here: distinguishable outcome categories. Not fixed here: any exception type, sealed result type, field, or status code.**

Five outcomes must remain distinguishable from one another, never conflated:

- **Invalid query.** A Knowledge Query that is structurally malformed (violates whatever construction-time invariants its own eventual type enforces) is a distinct outcome from a well-formed query that happens to match nothing.
- **Unavailable data.** Knowledge Memory's own store being unreachable or in a failed state is a distinct outcome from a well-formed query that legitimately matches nothing.
- **Permission denial.** A requesting principal not authorised to perform the retrieval act itself (Section 5) is a distinct outcome from a well-formed, authorised query that matches nothing — mirroring the Unit 9 Clarification's own Section 11 requirement and the identical, already-established pattern for Knowledge Submission's own three-way disposition (`docs/governance/PROGRAMME_3_UNIT_8_SCOPE_LOCK_CLARIFICATION.md` §9).
- **Empty result.** A well-formed, authorised query that genuinely matches no Knowledge Item is a valid, successful outcome — an empty Knowledge Result, never an error of any kind.
- **Implementation failure.** A genuine fault in the retrieval mechanism itself (unrelated to query validity, data availability, or permission) is a distinct outcome from all four above, and must propagate honestly rather than being silently absorbed into any of them — mirroring `InMemoryMemoryCore`'s and `DefaultKnowledgeSubmission`'s own shared "faults propagate, never silently swallowed" discipline.

**Binding requirement:** whatever concrete mechanism eventually expresses these five outcomes, no two of them may ever be represented identically to a caller. A caller receiving an empty Knowledge Result must be able to trust that the query was valid, the data was available, and permission was granted — not merely infer this from the absence of an error.

---

## 10. Runtime Responsibilities

**Retrieval owns:** query execution against Knowledge Memory's own held state; structural matching and deterministic ordering (Sections 4, 8); provenance-reference forwarding (Section 7); staleness computation or disclosure (Contract Design V2 §3, §6); producing a Knowledge Result or a distinguishable non-result outcome (Section 9).

**Runtime owns:** constructing and composing whatever concrete class implements Knowledge Retrieval into `ParkerRuntime.kt`, exactly as it already does for every other domain interface; supplying Knowledge Retrieval with whatever dependencies its own eventual implementation requires; identity *resolution* (who is asking) prior to a requesting principal ever being passed into Knowledge Retrieval, mirroring Memory Core Scope Lock §5's identical treatment. Whether Runtime additionally owns the `PermissionEngine.evaluate` call for this act, or whether Knowledge Retrieval self-gates, is the one mechanism question Section 5 leaves open — Runtime's own role here is not narrowed or expanded by this document beyond what that later resolution determines.

**Existing governance boundaries maintained, not altered:** the Permission Engine remains the sole authority for any permission decision (`docs/architecture/10-permission-engine.md` §5); Memory Core's own boundaries are untouched (Section 3, above); Reasoning Context's own consumption and composition role is untouched (Scope Lock §4); no dependency on Knowledge Submission, Knowledge Candidate Evaluation, or the lifecycle evaluators is introduced (Section 3).

---

## 11. Explicit Exclusions

This document deliberately leaves the following to later, separately governed work:

| Excluded | Reserved for |
| --- | --- |
| Concrete Kotlin types, method signatures, interface declarations for Knowledge Query, Knowledge Result, or the retrieval interface itself | Implementation Plan |
| The specific ordering rule (Section 8) | Implementation Plan |
| The specific staleness-detection mechanism (continuous monitoring, checked at query time, or otherwise) | Implementation Plan, per Contract Design V2 §3's own existing deferral |
| Whether retired items are included by default (Section 6) | Implementation Plan / Scope Lock, as a retrieval-shape decision |
| Whether Knowledge Retrieval self-gates or is externally gated by Runtime (Section 5) | A future, narrower Unit 9 implementation-facing Clarification, mirroring the Unit 8 precedent |
| Runtime composition — wiring Knowledge Retrieval into `ParkerRuntime.kt` | Implementation Plan / runtime composition, Programme 3's own Unit 10 and/or a future Programme 4 act (Scope Lock §4) |
| Optimisation, indexing, caching | Implementation, explicitly out of scope per Contract Design V2 §13 and Scope Lock §4 |
| Storage technology, persistence mechanism | Implementation, unaffected by and outside this document's own scope |
| Reconciliation of the legacy `KnowledgeSource`/`InMemoryKnowledgeStore` retrieval path with the V2 `KnowledgeItem` store | A separate, already-identified planning question (`docs/reviews/PROGRAMME_3_UNIT_9_KNOWLEDGE_RETRIEVAL_PLANNING_REVIEW.md`), not resolved here |

---

## 12. Constitutional Constraints

This document has been checked against, and does not violate, each of the following:

- **Does not reopen the Unit 9 Clarification.** Section 5 explicitly treats its classification as fixed and adds only Contract-Design-tier consequences already implied by that classification, never a re-derivation of it.
- **Does not invent new Permission Engine concepts.** No new outcome, resource type, action category, or evaluation tier is introduced anywhere in this document (Section 5).
- **Does not modify the Scope Lock.** Every Scope Lock citation above is quotation or restatement, never alteration; nothing in Scope Lock's own Deliverables (§5), Exclusions (§4), or Non-Functional Requirements (§7) is changed.
- **Does not modify Contract Design V2.** Every Contract Design V2 citation above is quotation or elaboration at greater precision, never a change to its own text, public model, or contract inventory.
- **Does not redefine Memory Core.** Section 3 and Section 7 fix only how Knowledge Retrieval may touch Memory Core (provenance-reference forwarding only); nothing about Memory Core's own contracts, permission boundary, or retrieval surface is altered.
- **Does not redefine Evidence Intelligence.** Section 3 states only that no dependency exists; nothing about Evidence Intelligence's own responsibilities is touched.
- **Does not redefine Knowledge Submission.** Section 1 and Section 3 distinguish Retrieval from Submission without altering anything the Unit 8 Clarification or Contract Design V2 §7 already fixed for Submission.

---

## Final Recommendation

This document fixes the contract-level detail Contract Design V2 §6/§12 named but did not specify, and the retrieval-shape questions the Unit 9 Clarification's own Section 12 explicitly left open, without reopening any constitutional question already settled. It selects no storage technology, no ordering algorithm, no Kotlin shape, and no enforcement mechanism, leaving each to a future Implementation Plan or narrower Clarification, exactly as this repository's own established discipline requires. It resolves no question belonging to Memory Core, Evidence Intelligence, Reasoning Context, or Knowledge Submission, each named explicitly rather than silently assumed.

The next governance stage — a Unit 9 Implementation Plan passage, and, separately, the enforcement-mechanism Clarification Section 5 identifies as still open — is authorised to begin only after this document is itself reviewed.

PROGRAMME 3 UNIT 9 KNOWLEDGE RETRIEVAL CONTRACT DESIGN — ADOPTED. Independent Constitutional Review completed (`docs/reviews/PROGRAMME_3_UNIT_9_KNOWLEDGE_RETRIEVAL_CONTRACT_DESIGN_INDEPENDENT_CONSTITUTIONAL_REVIEW.md`); four required corrections applied; Defect Confirmation Review (`docs/reviews/PROGRAMME_3_UNIT_9_KNOWLEDGE_RETRIEVAL_CONTRACT_DESIGN_DEFECT_CONFIRMATION_REVIEW.md`) found no regression.

---

## Independent Constitutional Review (Performed Before Completion)

Audited as if written by another reviewer, against the governing documents re-read for this task:

- **Does this document implement anything?** No — no Kotlin, interface, type, resource identifier, or action string is fixed anywhere; every mechanism-level question is explicitly deferred (Section 11).
- **Does it reopen the Unit 9 Clarification's own classification?** No — Section 5 states plainly that the classification is treated as fixed, and adds only consequences already implied by it.
- **Does it invent a lifecycle state or event kind beyond what Contract Design V2 and the current implementation already fix?** No — Section 6 checked directly against the two-value status model and the four named event kinds; nothing beyond them is introduced.
- **Does it invent provenance generation?** No — Section 7 explicitly states Retrieval never generates or mints a provenance reference, only forwards one already issued elsewhere.
- **Does it smuggle in a ranking algorithm?** No — Sections 2, 3, 8 each independently exclude scored or semantic ranking, consistent with Contract Design V2 §13 and Scope Lock §4.
- **Does it modify Contract Design V2 or the Scope Lock?** No — every citation is quotation or elaboration; no cited section's own text is altered.
- **Does it name a Kotlin type, method, resource identifier, or action string anywhere?** No — checked directly; none appears outside the disclaimers noting their deliberate absence.
- **Does it resolve any question outside its own named scope?** No — checked against Section 11's own exclusion table; Memory Core, Evidence Intelligence, Reasoning Context composition, storage technology, and the enforcement-mechanism question are each named and left open, not silently folded into a recommendation.

No genuine defect found requiring correction before completion.
