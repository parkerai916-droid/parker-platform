# Programme 3 — Unit 9: Knowledge Query / Retrieval — Planning Review

## Status

**Governance-first Planning Review only.** No production code, test, governance document, Contract Design, Scope Lock, Implementation Plan, or runtime composition file was modified in the course of this review. Nothing is staged, committed, or pushed. This review does not assume the Implementation Plan's "not begun" label for Unit 9 is correct on its face — it independently traces repository reality (source, tests, and every governance document that constrains or describes retrieval) and reports where reality confirms, refines, or complicates that label.

---

## 1. Repository Baseline

- **HEAD:** `fabb2124a94eed449095b207efada804dc072ea8` (`fabb212`)
- **Branch:** `main`
- **Working tree:** clean
- **Staged changes:** none

---

## 2. Governing Documents Reviewed

Read in full or in every retrieval-relevant section: `docs/architecture/MEMORY_CONTRACT_DESIGN.md` (legacy, §7 `MemoryQuery`, §8 `MemoryRetrievalPolicy` (deferred), §9 `MemoryStore`); `docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_CONTRACT_DESIGN_V2.md` (§3 Staleness, §6 Retrieval Contracts, §11, §12 Contract Inventory, §13 Non-Responsibilities, §14 Amendment Validation, §15); `docs/architecture/MEMORY_CORE_SCOPE_LOCK.md` (every Knowledge-Memory-referencing clause); `docs/implementation/MEMORY_CORE_IMPLEMENTATION_PLAN.md` (every Knowledge-Memory-referencing clause); `docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_SCOPE_LOCK.md` (§3 Scope, §4 Explicit Exclusions, §5 Deliverable 9, §9 Success Criteria); `docs/implementation/PROGRAMME_3_KNOWLEDGE_MEMORY_IMPLEMENTATION_PLAN.md` (§3 Unit 9, §4 Repository Impact, §5 Migration Strategy, Tracking Note); `docs/architecture/MEMORY_SOURCE_CONTRACT_DESIGN.md` (via `KnowledgeSource.kt`'s own citations); `docs/architecture/PRODUCTION_REASONING_CONTEXT_CONTRACT_DESIGN.md` (via `DefaultReasoningContextAssembler.kt`'s own citations). Source read directly and in full: `src/interfaces/KnowledgeStore.kt`, `src/interfaces/KnowledgeSource.kt`, `src/runtime/InMemoryKnowledgeStore.kt`, `src/runtime/KnowledgeItemPersistence.kt`, `src/runtime/DefaultReasoningContextAssembler.kt`, `src/runtime/DefaultKnowledgeCandidateEvaluator.kt`, `src/runtime/DefaultKnowledgeSubmission.kt`, `src/composition/ParkerRuntime.kt` (Knowledge-related wiring). Tests located and inspected: `tests/runtime/InMemoryKnowledgeStoreTest.kt`, `tests/runtime/DefaultReasoningContextAssemblerTest.kt`, `tests/runtime/FakeKnowledgeSource.kt`, `tests/contracts/KnowledgeContractsTest.kt`, `tests/composition/ParkerRuntimeReasoningContextIntegrationTest.kt`.

---

## 3. What Knowledge Retrieval Is (Question 1)

**Definition, per Contract Design V2 §6 and §12's own Contract Inventory:** Knowledge Retrieval is the single, read-only public path through which a **Knowledge Query** (a task-scoped request for relevant, already-promoted knowledge) is answered with a **Knowledge Result** (a bundle of `KnowledgeItem`/`KnowledgeReference` entries, each carrying its evidential-state classification, provenance reference, and a **mandatory staleness disclosure** — present on every result, never optional, never inferred from absence).

**Precisely separated from adjacent concepts, per direct textual authority:**

| Concept | What it is | How it differs from Knowledge Retrieval |
| --- | --- | --- |
| **Memory Retrieval** (`MemoryRetrieval`, `src/interfaces/MemoryCore.kt`) | Memory Core's own four direct-lookup/traversal methods over raw `Entity`/`Document`/`Assertion`/`Relationship` records | Operates one layer below Knowledge Memory entirely, on Memory Core's own record kinds, never on `KnowledgeItem`. Consumed today by `DefaultKnowledgeCandidateEvaluator` (Unit 6) and `EvidenceIntelligenceInputResolver`, permission-gated via `PermissionFilteredMemoryRetrieval`. Knowledge Retrieval, once it exists, is authorised to forward Memory Core provenance references only (Contract Design V2 §12: "Memory Core (for forwarded, minimal, immutable provenance references only)") — it is never authorised to perform Memory Core's own record-level queries directly. |
| **Knowledge Submission** (`DefaultKnowledgeSubmission`, Unit 8) | The write boundary — submits a `KnowledgeCandidate` for promotion evaluation | Write, not read. Unit 8 Clarification §4 explicitly excludes retrieval from Evaluation B's own governed act. |
| **Knowledge (Candidate) Evaluation** (`DefaultKnowledgeCandidateEvaluator`, Unit 6) | Decides whether a submitted candidate is promoted, and constructs the resulting `KnowledgeItem` | A one-shot evaluation invoked once per submission, not a repeatable query surface; it never answers a caller-issued `KnowledgeQuery` and holds no retrieval-ranking responsibility. |
| **Reasoning Context Assembly** (`DefaultReasoningContextAssembler`) | The consumer that renders retrieved knowledge (among other things) into a flat text context for reasoning | A caller of a retrieval capability, never the retrieval capability itself. Scope Lock §4 explicitly excludes "Reasoning Context's own consumption of Knowledge Memory... wiring `DefaultReasoningContextAssembler`... onto the new retrieval surface" from Programme 3's own scope, allocating it to Programme 4 instead. |

**Inputs:** a Knowledge Query — caller-supplied criteria (V1-inherited `KnowledgeQuery` shape: `requestingPrincipalId`, `relevance`, `correlationId`, `maximumResults`, `category`), expressing what is wanted, never an algorithm for finding it.

**Outputs:** a Knowledge Result — zero or more `KnowledgeItem`/`KnowledgeReference` entries plus a mandatory staleness disclosure per entry, in an order Knowledge Memory determines but does not rank or score (Contract Design V2 §13: no embeddings, vector search, or semantic/similarity ranking is designed anywhere).

**Responsibilities:** answer strictly from already-promoted `KnowledgeItem`s; disclose staleness on every result unconditionally; forward Memory Core provenance references without duplicating provenance content; return deterministic, repeatable results for an unchanged store (Implementation Plan §3 Unit 9's own verification requirement).

**Explicit non-responsibilities:** no write of any kind; no ranking, scoring, or semantic matching algorithm (deferred, per the legacy `MemoryRetrievalPolicy` precedent in `MEMORY_CONTRACT_DESIGN.md` §8, and per Contract Design V2 §13); no wiring into Reasoning Context (Programme 4's act, not Programme 3's); no Memory Core record-level query of its own.

---

## 4. Existing Implementation (Question 2)

**No component satisfying Contract Design V2's own "Knowledge Retrieval interface" definition exists anywhere in `src/`.** Direct search confirms: no `KnowledgeResult` type, no V2-tier `KnowledgeRetrieval` interface, and no implementation of one, exist in this repository. This part of the Implementation Plan's "not begun" label is confirmed accurate by direct repository inspection, not merely trusted.

**However, a different, pre-existing, already-wired retrieval mechanism occupies the functional slot "how does Reasoning Context get knowledge" today, and it must not be mistaken for Unit 9:**

- `KnowledgeSource.recall(query: KnowledgeQuery): List<KnowledgeRecord>` (`src/interfaces/KnowledgeSource.kt`) — a **Sprint 11 Unit 7 ("Memory Source Integration")** contract, predating Programme 3's own numbered units entirely, governed by `docs/architecture/MEMORY_SOURCE_CONTRACT_DESIGN.md`, not by Contract Design V2. It answers from the **legacy** `KnowledgeRecord` shape (flat: `knowledgePayload`, `sourceSubsystem`, optional `confidence` — no provenance reference, no evidential-state classification, no staleness field), not from V2's `KnowledgeItem`.
- Implemented by `InMemoryKnowledgeStore` (`src/runtime/InMemoryKnowledgeStore.kt:132,151` — `retrieve`/`recall`, case-insensitive substring match on `relevance`, no ranking).
- **Wired, live, in production:** `ParkerRuntime.kt:358-359` constructs `InMemoryKnowledgeStore`, narrows it to `KnowledgeSource`, and passes it to `DefaultReasoningContextAssembler` (`ParkerRuntime.kt:376`), which calls `memorySource.recall(memoryQuery)` on every `assemble()` invocation (`DefaultReasoningContextAssembler.kt:302-312`).
- **Never populated in production:** no caller of `InMemoryKnowledgeStore.remember()` exists anywhere in `ParkerRuntime.kt` (confirmed by direct grep). The store is permanently empty in the running system, so this live, wired retrieval call always returns an empty list in practice today.
- **This is disclosed, intended sequencing, not an oversight:** Implementation Plan §5 (Migration Strategy) states explicitly: *"`DefaultReasoningContextAssembler` continues to depend on exactly the same (renamed) interface, receiving exactly the same information it does today, for the entire duration of Programme 3. Its adoption of the new Knowledge Query/Result surface (Unit 9) is explicitly Programme 4's own act... not performed here."* Scope Lock §4 states the same allocation independently.

**Components inventoried, with status:**

| Component | Status |
| --- | --- |
| `KnowledgeSource`/`KnowledgeStore.retrieve`/`recall` (legacy) | **Implemented, tested, wired, live** — but a different, earlier, differently-governed capability, answering the legacy `KnowledgeRecord` shape, never populated in production |
| V2 `KnowledgeItem`-shaped retrieval interface/`KnowledgeResult` type | **Does not exist** — no file, type, or interface anywhere |
| `KnowledgeItemPersistence.find(knowledgeId)` | **Exists**, but is a direct identifier lookup for the persistence layer's own internal use and its own tests only — not a `KnowledgeQuery`-driven retrieval capability, and not called from any production path outside its own test |
| Duplicated/abandoned/dead implementations | None found specific to retrieval — the legacy and V2 paths are parallel and disconnected (Section 6, below), not duplicates of the same responsibility |

---

## 5. Runtime Flow (Question 3)

**Traced directly from source, in full:**

Parker currently obtains "knowledge" for reasoning exclusively via `DefaultReasoningContextAssembler.assemble()` → `memorySource.recall(memoryQuery)` → `InMemoryKnowledgeStore.recall()` (legacy path). This call is real, live, and executed on every reasoning-context assembly. Because nothing in production ever writes to `InMemoryKnowledgeStore`, it deterministically returns an empty list every time — the call is genuine but its result is always vacuous today.

**Does any promoted `KnowledgeItem` ever return to Reasoning Context? No.** Traced explicitly: `DefaultKnowledgeSubmission.submit()` persists a promoted `KnowledgeItem` into `InMemoryKnowledgeItemPersistence` (`ParkerRuntime.kt:742`, `DefaultKnowledgeSubmission.kt:122`). No production code anywhere calls `InMemoryKnowledgeItemPersistence.find()` outside that class's own test. `DefaultReasoningContextAssembler` holds no reference to `KnowledgeItemPersistence` at all — its `memorySource` field is typed `KnowledgeSource`, a completely different interface, backed by a completely different store instance.

**Where the architectural break is:** it is not merely "Unit 9 doesn't exist." It is that **two independent, non-communicating Knowledge stores coexist in the composed runtime** — one wired for read but never written (`InMemoryKnowledgeStore`, legacy), and one wired for write but never read (`InMemoryKnowledgeItemPersistence`, V2). The break sits precisely at this store boundary: even if a minimal V2 retrieval interface were built today, it would have nothing to wire *to* on the Reasoning Context side without Programme 4's own, separately-scoped cutover — and even without any new interface, the two existing stores already fail to communicate with each other by design, not by omission.

---

## 6. Ownership (Question 4)

| Responsibility | Owner | Basis |
| --- | --- | --- |
| Retrieval permission decisions | **Undetermined — no governance yet exists for Knowledge Retrieval's own permission treatment.** Contract Design V2 never names a Knowledge-Retrieval-side "Evaluation C"; the Unit 8 Clarification §14 explicitly authorises no Knowledge Memory permission framework broader than Evaluation B's own single resource/action pair. Whether Knowledge Retrieval is gated at all — and if so, by whom, in what shape — is not settled by any document read for this review. |
| Filtering (structural, by caller-supplied criteria) | Knowledge Memory's own retrieval implementation, once built | Contract Design V2 §6: "How `relevance` is matched... is owned entirely by the Memory implementation" (legacy precedent; V2 does not relocate this ownership) |
| Ordering | Knowledge Memory's own retrieval implementation | Same as above — `DefaultReasoningContextAssembler.kt:159` (KDoc) confirms the consumer "does not rank, score, reorder... what it receives," placing whatever ordering exists entirely inside the retrieval implementation |
| Ranking | **Explicitly nobody's responsibility today — deferred.** `MEMORY_CONTRACT_DESIGN.md` §8 names `MemoryRetrievalPolicy` "(deferred)"; Contract Design V2 §13 excludes embeddings/vector/semantic ranking from anything designed so far |
| Lifecycle visibility (which status values a result may include) | Not settled — no document specifies whether Knowledge Retrieval may return `RETIRED` items, or only `ACTIVE`/current ones |
| Revision visibility (whether/how history is exposed) | Not settled — Contract Design V2 defines the history model (Unit 7) but does not specify whether Knowledge Result exposes full history or only current classification |
| Retirement visibility | Not settled, same gap as above |
| Restoration visibility | Not settled, same gap as above |

---

## 7. Dependency Analysis (Question 5)

Retrieval **may** depend on:

- **Knowledge Store** (the V2 `KnowledgeItem` persistence layer) — necessarily; this is what it reads.
- **Memory Core, narrowly** — Contract Design V2 §12's own Contract Inventory: "Memory Core (for forwarded, minimal, immutable provenance references only)." Retrieval may forward a provenance reference; it may not perform its own independent Memory Core record-level query.
- **Permission Engine** — plausible, by analogy to Evaluation A/B, but **not yet authorised** for this specific act by any document (see Section 6, above — this is the one dependency question this review cannot resolve from existing authority).

Retrieval **must not** depend on:

- **Memory Retrieval directly, for anything beyond provenance-reference forwarding.** Performing Memory Core's own structural queries (`findEntities`, `traverseRelationships`, etc.) as part of answering a Knowledge Query would collapse Knowledge Memory's own promotion boundary — Memory Core Scope Lock's one-directional dependency rule (`Knowledge Memory SHALL depend on Memory Core, never the reverse`) is about direction, not license for Knowledge Retrieval to re-implement Memory Retrieval's own responsibilities.
- **Reasoning Context**, in either direction — Reasoning Context is a *caller* of Knowledge Retrieval; Knowledge Retrieval must never depend on `ReasoningContextAssembler`, `ReasoningContext`, or any Reasoning-layer type.
- **Knowledge Candidate Evaluation or Knowledge Submission** — retrieval is a pure read path; nothing in Contract Design V2 or the Scope Lock authorises it to invoke evaluation or submission logic.

No prohibited dependency was found already present in any code, since no retrieval implementation exists to violate this yet.

---

## 8. Runtime Composition (Question 6)

- **Current composition:** the legacy `KnowledgeSource`/`InMemoryKnowledgeStore` path is fully composed (`ParkerRuntime.kt:358-359, 376`) and live, but structurally disconnected from anything Programme 3 has built since. The V2 write path (`DefaultKnowledgeSubmission`/`InMemoryKnowledgeItemPersistence`) is fully composed (`ParkerRuntime.kt:742-748`) but has no composed reader.
- **Missing composition:** no V2 retrieval interface exists to compose in the first place — this is an implementation gap, not a composition gap, at present.
- **Dormant composition:** none specific to retrieval (the dormant Unit 7.2/7.3 lifecycle evaluators are a Unit 7 matter, not Unit 9's).
- **Unnecessary composition:** none identified — every currently-composed Knowledge-related line serves a disclosed, intended purpose (including the legacy path's continued composition, which is explicitly required to persist "for the entire duration of Programme 3" per the Implementation Plan).

---

## 9. Migration (Question 7)

**Coexistence confirmed, dependency identified, not resolved (per instruction):**

- **Legacy `InMemoryKnowledgeStore`** — wired to `DefaultReasoningContextAssembler`; holds `KnowledgeRecord` (flat, no Memory Core reference); currently the only store Reasoning Context can reach; currently always empty in production.
- **V2 `InMemoryKnowledgeItemPersistence`** — wired only to `DefaultKnowledgeSubmission`; holds `KnowledgeItem` (Memory Core-referencing, evidential-state-classified); currently the only store anything writes to; currently unreadable by anything outside its own test.
- **Current system of record, if the question is "which store holds real, promoted knowledge today":** the V2 store — it is the only one anything actually populates.
- **Current retrieval source of truth, if the question is "which store Reasoning Context actually reads":** the legacy store — and it is, by design, disconnected from the V2 store, so it never reflects what the V2 store holds.
- **Migration implication (identified, not resolved):** Unit 9, once built, creates a retrieval capability over the V2 store. Whichever store `DefaultReasoningContextAssembler` (or its Programme 4 successor) is eventually pointed at, the other store's own content becomes either orphaned (if the V2 store is adopted, the legacy store's — currently always-empty — content is simply abandoned, a low-cost migration) or invisible (if nothing changes, Unit 9's own output remains as unreachable from Reasoning Context as `InMemoryKnowledgeItemPersistence` is today, until Programme 4's own separately-governed cutover occurs). This review does not determine which outcome is intended — only that the dependency exists and that Scope Lock §4 already allocates the cutover decision to Programme 4, not Programme 3.

---

## 10. Constitutional Gaps, Separated by Kind (Question 8)

| Kind | Gap | Status |
| --- | --- | --- |
| **Constitutional** | None identified. Article XIII (staleness never concealed) is already fixed by Contract Design V2 §3/§11; no article is silent on retrieval in a way that blocks design. |
| **Contract** | None identified — Contract Design V2 §6/§12 already defines Knowledge Query, Knowledge Result, and the Knowledge Retrieval interface's own shape and boundaries in full. |
| **Scope Lock** | None identified — Deliverable 9 and §4's exclusions already fix Unit 9's own scope and its explicit non-responsibility for the Reasoning Context cutover. |
| **Implementation** | **Genuine gap.** No Kotlin implementation of the V2 Knowledge Retrieval interface, `KnowledgeResult`, or a staleness-disclosure mechanism exists. This is the one gap this review confirms as real, not assumed. |
| **Runtime Composition** | Not yet applicable — there is nothing built to compose. Once built, composing it into Reasoning Context is explicitly out of Programme 3's own scope regardless. |
| **Testing** | No test exists proving V2 retrieval behaviour (staleness presence, determinism) — necessarily absent, since no implementation exists to test. |
| **Documentation** | **A narrow, genuine governance gap, not invented by this review:** no document read for this review settles retrieval-side permission ownership (Section 6, above — "Evaluation C," if any, is unaddressed by name anywhere), or lifecycle/revision/retirement/restoration visibility in a Knowledge Result. This is reported as a real absence of authority, not resolved or filled in here. |

---

## 11. Files Expected to Change (Question 9)

**Unit 9 does not already exist; it genuinely requires new implementation.** No file needs to change to complete it, because there is nothing to complete — new files/types must be created. Consistent with this review's own prohibition on proposing implementation, only the files the Implementation Plan's own Repository Impact section (§4) already names are reported, not designed further here:

- `src/interfaces/KnowledgeStore.kt` — new `KnowledgeResult` type and the Knowledge Retrieval interface itself belong here, alongside the existing V2 model, per this file's own established additive-extension pattern.
- A new runtime implementation file under `src/runtime/` (unnamed by any governance document read for this review — naming it further would be implementation, not planning).
- `src/composition/ParkerRuntime.kt` — only to the extent Programme 3 itself ever composes the new interface for its own tests; wiring it to `DefaultReasoningContextAssembler` is explicitly excluded from Programme 3's scope (Section 8, above).
- Corresponding new test files under `tests/runtime/` and/or `tests/contracts/`.

No existing file's current behaviour is expected to change — this is additive, mirroring every other unit in this Programme's own established discipline.

---

## Independent Constitutional Review

- **Did the review invent governance?** No. Every requirement and boundary stated (staleness mandatory, no ranking, provenance-forwarding-only Memory Core dependency, Programme 4 owns the cutover) is a direct citation of Contract Design V2, the Scope Lock, or the Implementation Plan — never an assertion introduced here. Section 6's and Section 10's "not settled" findings are reported as absences, not filled with invented rules.
- **Did it assume implementation gaps?** No — the central implementation gap (no V2 retrieval interface exists) was confirmed by direct, repeated source search (`grep` across `src/` for `KnowledgeResult`/`KnowledgeRetrieval`/any file named for Knowledge retrieval), not inferred from the Implementation Plan's own label alone, per the task's explicit instruction.
- **Did it mistake runtime composition for implementation?** No — Section 4 explicitly distinguishes the legacy path's *composition* (real, live) from Unit 9's own *implementation* (absent), and Section 8 separately confirms no composition gap exists yet because nothing exists to compose.
- **Did it confuse Memory Retrieval with Knowledge Retrieval?** No — Section 3's comparison table treats them as structurally distinct from the outset, and Section 7 states explicitly that Knowledge Retrieval must not depend on Memory Retrieval beyond narrow provenance-reference forwarding.
- **Did it propose implementation outside existing authority?** No — Section 11 deliberately stops at the files the Implementation Plan itself already names, declining to name a new class, interface shape, or permission model for the undecided retrieval-permission question in Section 6.

---

## Recommendation

Unit 9 (Knowledge Query/Result/Retrieval) genuinely requires new implementation — the Implementation Plan's "not begun" label is confirmed accurate for Unit 9's own V2-shaped deliverable by direct repository inspection, not merely trusted. The one material refinement repository reality adds beyond the existing planning documents is that a **different, earlier, already-wired retrieval mechanism** (the Sprint-11 `KnowledgeSource`/`InMemoryKnowledgeStore` path) already occupies the "how does Reasoning Context get knowledge" functional slot today, disclosed and intended to remain so throughout Programme 3, and must not be mistaken for Unit 9 or treated as satisfying it. Before implementation begins, one narrow governance gap should be closed: whether, and how, a retrieval-side permission evaluation applies (Section 6/10) — everything else Unit 9 needs is already fixed by existing, frozen authority.

---

## Final Git Status

```
$ git status --short
?? docs/reviews/PROGRAMME_3_UNIT_9_KNOWLEDGE_RETRIEVAL_PLANNING_REVIEW.md
```
