**Status: Adopted.** Independent Implementation Plan Review of this document returned VERDICT — REVISE BEFORE ACCEPTANCE, identifying two defects: (1) RKS.1 was not drafted as a strict compatibility gate and implicitly authorised RKS.1 itself to additively extend Unit 9.7.1's own shared contract; (2) Section 8/19's dependency ordering did not correctly require RKS.3 to wait on both RKS.2 and Unit 9.7.3 together, instead describing Unit 9.7.3 as RKS.3's only gate. That review found the plan's own architecture Accepted In Principle and identified no other blocking defect. A bounded defect-correction pass was applied: RKS.1 (Section 9) is now drafted as a strict compatibility gate that stops and returns any genuine incompatibility to Unit 9.7.1 itself, never resolving one under this plan's own authority (Section 8, Section 10, Section 17 updated to match); Section 8's "Earliest lawful parallelisation" paragraph and Section 19's own diagram now show RKS.3 as a join requiring both RKS.2 complete and Unit 9.7.3 complete, neither alone sufficient. Defect Confirmation Review then returned VERDICT — ACCEPT, confirming both defects resolved, no regression introduced, and no new blocking defect remaining. Adoption makes this plan the authoritative implementation sequence for the Reasoning Context Bounded Semantic Relevance capability. Adoption does not implement any RKS unit; does not begin Unit 9.7.1 or Unit 9.7.3; does not run or begin the Unit 9.7 semantic-mechanism selection spike; and does not, by itself, authorise any RKS unit to begin — each RKS unit named in Section 9 remains gated on its own stated dependencies (Section 8), in particular Unit 9.7.1 (all units) and Unit 9.7.3 (RKS.3 onward), neither of which exists in the repository as of this adoption. It is the dedicated implementation plan `REASONING_KNOWLEDGE_SOURCE_BOUNDED_SEMANTIC_RELEVANCE_CONTRACT_AND_PERMISSION_SUCCESSOR.md` Section 17(c) and Section 18 required before any unit applying Bounded Relevance Computation inside `ReasoningKnowledgeSource`/`DefaultReasoningKnowledgeSource` may begin. It translates the three adopted Reasoning Context sibling governance documents (commit `09dbab35f6f512bb2a011d36b0c48dfd9bf25f1a`, "docs(governance): adopt Reasoning Context semantic relevance package") into discrete, ordered, independently verifiable engineering units, mirroring `PROGRAMME_3_UNIT_9_7_BOUNDED_SEMANTIC_RELEVANCE_IMPLEMENTATION_PLAN.md`'s own already-adopted shape and discipline. It does not revisit, reweigh, or reopen anything any adopted document already settled. No Kotlin is implemented, proposed, or changed by this document. Neither `src/` nor `tests/` is touched. No existing governance document is modified — the three adopted Reasoning Context sibling documents and the adopted Unit 9.7 governance/implementation plan are each cited, never altered.

# Reasoning Context — Bounded Semantic Relevance Implementation Plan

Programme: **Reasoning Context, Bounded Semantic Relevance Implementation Plan for `ReasoningKnowledgeSource`/`DefaultReasoningKnowledgeSource` (adopted).**

This document is the detailed engineering breakdown of a single capability already authorised at three adopted governance tiers — the Reasoning Context Scope Lock's own Bounded Semantic Relevance Amendment, the Reasoning Context Contract Design's own Bounded Semantic Relevance Amendment, and the `ReasoningKnowledgeSource` Bounded Semantic Relevance Contract and Permission Successor — exactly as `PROGRAMME_3_UNIT_9_7_BOUNDED_SEMANTIC_RELEVANCE_IMPLEMENTATION_PLAN.md` already did for Unit 9.7 itself, one governance tier down. It exists specifically to close the Live-Reasoning Integration gate that adopted plan's own Section 8 named as a blocked, out-of-its-scope dependency. It translates the adopted contract into discrete, ordered, independently verifiable engineering units. It does not perform, plan in detail beyond the properties level, or authorise implementation of any kind — this document is planning only.

---

## 1. Status

Adopted, per the Status block above. Independent Implementation Plan Review returned REVISE BEFORE ACCEPTANCE; a bounded correction pass addressed the two identified defects (RKS.1 compatibility-gate authority, RKS.3 dependency/join ordering); Defect Confirmation Review returned ACCEPT. This document introduces no Kotlin and modifies no existing test. No unit named in Section 9, below, may begin implementation on the strength of this document's own adoption alone — each remains gated on its own stated dependencies in Section 8, and none may begin at all until Unit 9.7.1 (all units) and Unit 9.7.3 (RKS.3 onward) are themselves independently complete, neither of which exists in the repository as of this adoption.

---

## 2. Authoritative Governance Basis

Read fresh, in full, before drafting:

- `docs/governance/KNOWLEDGE_DISCOVERABILITY_REASONING_CONTEXT_BOUNDED_SEMANTIC_RELEVANCE_SCOPE_LOCK_AMENDMENT.md` (Adopted) — the single authorised capability (§1), the problem solved (§2), the Case 3 determination preserved (§3), what remains authoritative (§4), what semantic relevance may/may not do (§5–§6), permission boundaries (§7), fail-closed expectations (§8), provenance/identity preservation (§9), exclusions (§10), the shared-mechanism relationship to Unit 9.7 (§11), and the explicit prohibition on unintended architectural expansion (§12).
- `docs/governance/KNOWLEDGE_DISCOVERABILITY_REASONING_CONTEXT_CONTRACT_DESIGN_BOUNDED_SEMANTIC_RELEVANCE_AMENDMENT.md` (Adopted) — the Relevance Request/Relevance Result contract-tier concepts (§2–§4), the opaque per-request candidate identity and minimum relevance content determination (§3), canonical/authoritative re-resolution (§5), the Pre-Computation vs Pre-Disclosure distinction and the three-check rule stated at the contract tier (§9), failure semantics (§10), and implementation-neutrality (§14).
- `docs/governance/REASONING_KNOWLEDGE_SOURCE_BOUNDED_SEMANTIC_RELEVANCE_CONTRACT_AND_PERMISSION_SUCCESSOR.md` (Adopted) — the complete authorised processing sequence (§3), lifecycle and permission ordering (§5), lawful dereferenced-content handling and minimum-content reduction (§6), the opaque request-token boundary (§7), shared Unit 9.7 mechanism reuse (§8), canonical re-resolution (§9), the fresh Pre-disclosure three-check re-verification (§10), `SafeKnowledgeResultEntry` authority (§11), the split fail-closed table (§13), prohibited behaviours (§14), determinism/version/configuration requirements (§15), and required verification properties (§16).
- `docs/governance/PROGRAMME_3_UNIT_9_7_BOUNDED_SEMANTIC_RELEVANCE_CONTRACT_AND_PERMISSION_SUCCESSOR.md` (Adopted) — the fourteen-condition Model A-Strict production boundary (§3), the required contract surface (§4), and the fail-closed table (§6) this plan's own sibling capability reuses the identical shared mechanism against.
- `docs/governance/PROGRAMME_3_UNIT_9_7_BOUNDED_SEMANTIC_RELEVANCE_IMPLEMENTATION_PLAN.md` (Adopted) — in particular §4.2/§4.3 (the Case 3 finding this plan exists to act on), §8 (the Live-Reasoning Integration gate, named here as a blocked dependency this plan now closes the governance-and-planning side of), §13/§13.1 (the mechanism-selection spike and its ten mandatory semantic-fitness acceptance requirements — reused, not repeated, by this plan), and Unit Decomposition §8 (Units 9.7.1–9.7.6, in particular 9.7.1's contract types and 9.7.3's concrete mechanism, which this plan's own units depend on and reuse unchanged, never re-implement).
- `docs/decisions/CDR-008_MEMORY_CORE_DOWNSTREAM_RELEVANCE_BOUNDARY.md` (Accepted, Canonical, Frozen) — Decision A and its Mandatory Invariants, establishing that Memory Core Scope Lock's prohibition binds Memory Core's own interface, not a downstream, separately-governed component.
- `docs/governance/KNOWLEDGE_DISCOVERABILITY_REASONING_CONTEXT_SCOPE_LOCK.md` and `docs/governance/KNOWLEDGE_DISCOVERABILITY_REASONING_CONTEXT_CONTRACT_DESIGN.md` — the frozen base documents the two amendments above extend; neither is edited by this plan or by any unit it names.
- `docs/governance/KNOWLEDGE_DISCOVERABILITY_REASONING_CONTEXT_BOUNDARY_REVIEW.md` — §3 ("Formal Ownership"), establishing `ReasoningKnowledgeSource` as Knowledge Memory's own second, additive, content-bearing read surface, not a path Reasoning Context constructed for itself.

Current production implementation inspected fresh as authoritative, per this task's own instruction, in preference to prior summaries wherever they might differ:

- `src/runtime/DefaultReasoningKnowledgeSource.kt` (213 lines) — read in full this session. Confirms the frozen ten-step algorithm exactly as governed, and confirms two facts material to this plan that were not assumed from any prior summary: (a) its existing structural-match step (`resolveContent` then `content.contains(query.relevance, ignoreCase = true)`) is executed as a single combined loop over item-level-approved candidates, with no existing branch on "structural match found nothing," mirroring the exact same gap `PROGRAMME_3_UNIT_9_7_BOUNDED_SEMANTIC_RELEVANCE_IMPLEMENTATION_PLAN.md` §6 found in `DefaultKnowledgeRetrieval`; (b) unlike `DefaultKnowledgeRetrieval`, this class's structural match already requires a full Memory Core dereference of every item-level-approved candidate before matching can occur at all — there is no `basis`-only, dereference-free structural match here. Section 4.2 and Section 6, below, both depend on this second fact.
- `src/interfaces/KnowledgeStore.kt`, lines 1357–1610 — the `KnowledgeRetrievalQuery`, `StalenessDisclosure`, `KnowledgeRetrieval`, `ReasoningKnowledgeSource`, and `SafeKnowledgeResultEntry` declarations, confirmed unchanged from governance's own description: `ReasoningKnowledgeSource.recall(requestingPrincipalId: PrincipalId, query: KnowledgeRetrievalQuery): List<SafeKnowledgeResultEntry>`; `SafeKnowledgeResultEntry(content, evidentialState, status, staleness)`.
- `src/interfaces/MemoryCore.kt`, lines 114–120 and 1205–1217 — `MemoryCoreRecordStatus` (`ACTIVE, DISPUTED, SUPERSEDED, ARCHIVED, DELETED`) and `MemoryRetrieval` (`getEntity`, `getDocument`, `getAssertion`, `getRelationship`, plus six query-based methods), confirmed unchanged.
- `src/composition/PermissionFilteredMemoryRetrieval.kt` (full file read this session) — confirms `forAuthorizationPurpose` returns an immutable `PurposeBoundMemoryRetrieval` carrier with no engine, policy, registry, or raw delegate of its own, and confirms every direct-lookup method performs a fresh `permissionEngine.evaluate` call on every invocation — never cached, never reused across calls. This is the mechanism the fresh Pre-disclosure re-verification (Section 7.4, below) relies on for check C being genuinely fresh.
- `src/runtime/KnowledgeItemPersistence.kt` (full file read this session) — confirms `find(knowledgeId): KnowledgeItem?` (single-item lookup, distinct from `findAll()`) already exists and requires no extension for this plan's own check A.
- `src/interfaces/PermissionEngine.kt` and `src/runtime/DefaultPermissionEngine.kt` (both read in full this session) — confirm `evaluate(request: ExecutionRequest): PermissionDecision` is the sole authorisation entry point, identity-status-gated first, then delegated to `DefaultPermissionPolicy`; no change required or proposed.
- `src/runtime/DefaultReasoningContextAssembler.kt` (full file read this session) — confirms `knowledgeSource: ReasoningKnowledgeSource` is consumed through exactly one call, `knowledgeSource.recall(message.senderPrincipalId, knowledgeRetrievalQuery)`, with every returned `SafeKnowledgeResultEntry` rendered, in the exact order returned, via the fixed `renderKnowledgeEntry`/`escapeForPrompt` pair. Nothing in this class inspects, ranks, or reinterprets what `recall` returns — confirming Frozen Boundary #12 (ReasoningContext rendering unchanged) is satisfiable by construction, since this class's own contract with `recall` is already opaque to how `recall` produced its list.
- `src/runtime/ReasoningPromptBuilder.kt` (full file read this session) — confirms `DefaultReasoningPromptBuilder.buildPrompt(turn, reasoningContext)` joins `reasoningContext.entries` with `"\n"`, unmodified, ahead of the owner's own message text — the exact mechanism `ParkerRuntimeReasoningContextIntegrationTest.kt`'s own test "the assembled ReasoningContext's entries reach the real prompt sent to the model, unchanged" (line 106) already verifies for existing entries.
- `src/composition/ParkerRuntime.kt`, lines 891–990 (targeted read this session, not the full 102,450-byte file) — confirms the exact composition: `permissionFilteredMemoryRetrieval = PermissionFilteredMemoryRetrieval(durableMemoryCore, permissionEngine)` (line 905, one shared instance); `reasoningContextMemoryRetrieval = permissionFilteredMemoryRetrieval.forAuthorizationPurpose(REASONING_CONTEXT_RETRIEVAL_PURPOSE)` (line 969); `reasoningKnowledgeSource = DefaultReasoningKnowledgeSource(knowledgeItemPersistence, permissionEngine, reasoningContextMemoryRetrieval, REASONING_CONTEXT_RETRIEVAL_PURPOSE)` (lines 970–975, `clock` left at its production default); `reasoningContextAssembler = DefaultReasoningContextAssembler(identityService, toolRegistry, conversationHistorySource, reasoningKnowledgeSource, worldModelSource)` (line 989). `knowledgeRetrieval = DefaultKnowledgeRetrieval(knowledgeItemPersistence, permissionEngine)` (line 956, one line above) is Unit 9.7's own, entirely separate, wiring target — confirmed structurally distinct: `reasoningKnowledgeSource` and `knowledgeRetrieval` are two different `lateinit var` fields, constructed from the same shared `knowledgeItemPersistence`/`permissionEngine` instances but never from each other.
- Tests inspected for regression scope and existing coverage shape (test method names enumerated, not full bodies, per Section 16): `tests/runtime/DefaultReasoningKnowledgeSourceTest.kt` (37 test cases), `tests/composition/ParkerRuntimeReasoningKnowledgeSourceCompositionTest.kt` (17 test cases), `tests/runtime/DefaultReasoningContextAssemblerTest.kt` (13 knowledge/memory-relevant cases among its total), `tests/composition/ParkerRuntimeReasoningContextIntegrationTest.kt` (12 test cases, including the `aadd596`-lineage fixture), `tests/runtime/MemoryRetrievalPermissionPolicyOperationalisationTest.kt` (11 cases), `tests/runtime/DefaultPermissionEngineTest.kt` (10 cases), `tests/composition/PermissionFilteredMemoryRetrievalTest.kt` (25 cases), `tests/runtime/KnowledgeItemPersistenceTest.kt` (8 cases) and `tests/runtime/DurableKnowledgeItemPersistenceTest.kt` (10 cases).
- The experimental QMD spike material — `tests/composition/qmd-authorized-vector-bridge.mts`, `tests/composition/QmdCanonicalMemoryRetrievalExperimentTest.kt`, `tests/composition/QmdRealEmbeddingFixtures.kt` — inspected only far enough to confirm their own experimental, non-production nature (a Node.js subprocess bridge into a sibling `qmd`/`qmd-parker-experiment` directory, and an `ExperimentalQmdCanonicalMemoryAdapter`/`RealQmdAuthorizedVectorProcessBridge` composition-seam test, both explicitly commented "Experimental composition seam only"). This plan does not run, re-evaluate, or draw a mechanism-selection conclusion from this material — Unit 9.7's own Section 13 spike, reused unchanged (Section 8, below), remains the sole authority for that decision.
- Repository-wide search confirming a genuine, disclosed finding material to Section 8, below: **no production Kotlin file anywhere in `src/` declares a semantic/relevance mechanism interface, a `SemanticRelevance`/`BoundedRelevance`/`RelevanceMechanism` type, or any Unit 9.7.1/9.7.3-shaped contract or adapter.** Units 9.7.1 through 9.7.6 have not been implemented as of this plan's own drafting — confirmed by grep across `src/` and `tests/` for `Unit9_7`/`Unit_9_7` and for `SemanticRelevance`/`BoundedRelevance`/`RelevanceMechanism`/`QmdMechanism`/`SemanticMechanism`, all returning zero matches outside governance documents and this plan's own drafting.

---

## 3. Objective

Introduce exactly one capability into `ReasoningKnowledgeSource`/`DefaultReasoningKnowledgeSource`: **Bounded Relevance Computation**, precisely as the three adopted Reasoning Context sibling documents in Section 2 define it — a request-scoped, subordinate, replaceable ranking/subsetting operation over an already-eligible, already-permission-approved, already-lawfully-dereferenced, closed candidate set, running only as a fallback after structural retrieval has completed successfully and found exactly zero relevant candidates, and reusing the identical shared Unit 9.7 relevance mechanism (its contract, Unit 9.7.1, and its concrete implementation, Unit 9.7.3) unchanged. This plan authorises no broader capability, no second semantic mechanism, and no cutover of Reasoning Context to `KnowledgeRetrieval`. Section 17 restates, without elaboration, the explicit non-goal list this task's own instruction fixed; nothing in Sections 4–16 should be read as broadening it.

---

## 4. Current Live-Path Map

### 4.1 The governed production path, live reasoning included

```
InboundOwnerMessage
  → ParkerRuntime.submitOwnerMessage(...)
      → conversation continuity resolution (unchanged, out of scope)
      → DefaultReasoningContextAssembler.assemble(resolvedMessage)
          → KnowledgeRetrievalQuery(relevance = message.text, correlationId = message.correlationId.value,
                                     maximumResults = MEMORY_QUERY_MAXIMUM_RESULTS)
          → reasoningKnowledgeSource.recall(message.senderPrincipalId, knowledgeRetrievalQuery)
              [ = DefaultReasoningKnowledgeSource.recall, the frozen ten-step algorithm: ]
              1. query validation (KnowledgeRetrievalQuery's own construction-time checks)
              2-3. act-level ExecutionRequest → permissionEngine.evaluate → deny ⇒ emptyList()
              4-5. persistence.findAll() → isRetrievable (ACTIVE, or RETIRED iff includeRetired)
              6. per-candidate item-level permissionEngine.evaluate → itemApproved
              7-8. per item in itemApproved: resolveContent(item.evidenceReference)
                     [ evidenceMemoryRetrieval.getAssertion/getEntity, ACTIVE-status gated, normalized ]
                   → content.contains(query.relevance, ignoreCase = true) → relevant
              9. SafeKnowledgeResultEntry(content, evidentialState, status, staleness) per relevant item
              10. entries.take(query.maximumResults)
          → List<SafeKnowledgeResultEntry>
      → renderKnowledgeEntry(entry) per returned entry [escapeForPrompt(content), evidentialState, status, staleness]
      → entries += "Memory: ..." (fixed format, Reasoning Context Scope Lock Section 6/Contract Design Section 8)
      → ReasoningContext(entries.toList())
  → DefaultReasoningPromptBuilder.buildPrompt(turn, reasoningContext)
      → reasoningContext.entries.joinToString("\n") + turn.message.text + INSTRUCTION
  → ReasoningProvider / ModelReasoningProvider (out of scope, unmodified)
  → model inference
  → TaggedReasoningResponseParser (out of scope, unmodified)
```

Concrete classes/files participating: `KnowledgeRetrievalQuery`, `SafeKnowledgeResultEntry`, `ReasoningKnowledgeSource` (`src/interfaces/KnowledgeStore.kt`); `DefaultReasoningKnowledgeSource` (`src/runtime/DefaultReasoningKnowledgeSource.kt`) — the sole implementation; `KnowledgeItemPersistence` (read-only `findAll()`/`find()` source); `PermissionEngine`/`DefaultPermissionEngine`; `PermissionFilteredMemoryRetrieval` (purpose-bound as `reasoningContextMemoryRetrieval`, the `evidenceMemoryRetrieval` this class holds); `DefaultReasoningContextAssembler` (`src/runtime/DefaultReasoningContextAssembler.kt`) — the sole consumer of `ReasoningKnowledgeSource`; `DefaultReasoningPromptBuilder` (`src/runtime/ReasoningPromptBuilder.kt`) — the sole consumer of the resulting `ReasoningContext`; composition in `ParkerRuntime.kt` lines 969–975 and 989.

Today, structural matching (steps 7–8) is a single combined per-item loop — dereference, then test — with no existing concept of "structural match found zero candidates" as a distinguishable, branchable condition, exactly mirroring the identical gap `PROGRAMME_3_UNIT_9_7_BOUNDED_SEMANTIC_RELEVANCE_IMPLEMENTATION_PLAN.md` §4.1/§6 found in `DefaultKnowledgeRetrieval`.

### 4.2 Genuine finding: this surface's Pre-computation dereference already covers the full candidate set — a material difference from Unit 9.7's own architecture

Fresh inspection of `DefaultReasoningKnowledgeSource.kt` establishes a fact this plan is obligated to disclose, since it changes the shape of the gap analysis (Section 6) relative to a naive mirroring of Unit 9.7's own Section 6 table:

`DefaultKnowledgeRetrieval`'s structural match (Unit 9.7 plan §4.1) tests `query.relevance` against a `KnowledgeItem`'s own `basis` text — a field already present on the item, requiring no Memory Core call at all. Its Pre-computation phase therefore never dereferences anything; Unit 9.7.2's own "closed candidate set" (Unit 9.7 plan §7.2) has to be built by running the *existing* item-level permission gate a second time over the full lifecycle-eligible set, since the original single-pass loop never produced a reusable set at all.

`DefaultReasoningKnowledgeSource`'s structural match is different in kind, not merely in field name: this class matches against *dereferenced Memory Core content*, so its existing steps 7–8 already call `resolveContent` — and therefore already perform the full, lawful, `ACTIVE`-status-gated Memory Core dereference — for every item-level-approved candidate, before structural matching can occur at all. This means the dereferenced-content map Bounded Relevance Computation needs for its own Pre-computation-admitted closed candidate set (Frozen Boundary #4/#5) is not a new artefact this plan must invent; it is a byproduct already computed by the existing algorithm's own steps 7–8, provided those steps are restructured (Section 7.1, below) to *retain* that map instead of discarding it once the `contains` test for the current item is complete. Consequently, Section 6's gap analysis classifies "closed candidate set" and "minimum content" more favourably here than the analogous rows in Unit 9.7 plan §6 — as **requires restructuring of existing computation**, not **requires new dereference-boundary work**.

The converse asymmetry appears at Pre-disclosure. Unit 9.7's own Section 9 (Prohibited Behaviours) and Section 5 (Permission Integration) describe Pre-disclosure re-verification for `DefaultKnowledgeRetrieval` as a re-check of permission and lifecycle/status only — `DefaultKnowledgeRetrieval` never touches Memory Core at any point, so it has no dereferenced content to re-verify as current. `DefaultReasoningKnowledgeSource` does — its entire raison d'être (Contract Design Decision-Register item 3) is lawful dereferenced-content disclosure — so its own adopted Successor document's Section 10 correctly imposes a third check Unit 9.7's simpler two-check pattern never needed: a **second, fresh invocation of `evidenceMemoryRetrieval.getAssertion`/`getEntity`**, immediately before `SafeKnowledgeResultEntry` construction, for exactly the tokens the shared mechanism actually surfaces (never for the whole closed candidate set, since re-dereferencing candidates the mechanism did not select would be unnecessary Memory Core traffic). This asymmetry is the reason the two sibling contract-and-permission-successor documents are not textually identical, and this plan treats it as load-bearing, not a drafting inconsistency.

### 4.3 Case 3 status restated, not re-derived

The Case 3 determination (Unit 9.7 plan §4.3; Scope Lock Amendment §3) is settled, adopted governance, not reopened here: `ReasoningKnowledgeSource` is the correct, permanent, content-bearing live-reasoning surface; `DefaultKnowledgeRetrieval` is not, and never will be, wired into Reasoning Context directly. This plan exists because that determination is already true, not to re-argue it.

---

## 5. Frozen Boundaries

Every unit in Section 9 is bound by all seventeen of this task's own frozen architectural requirements, restated here once, each cross-referenced to where Section 4's current implementation map or Section 6's gap analysis already addresses it:

1. **Correct live surface.** Already satisfied structurally — `DefaultReasoningKnowledgeSource` is confirmed, by fresh composition inspection (Section 2), to be the actual, sole, live-path consumer feeding `DefaultReasoningContextAssembler`; no unit below changes this wiring target.
2. **Shared semantic mechanism.** Not yet implementable — depends on Unit 9.7.1 and Unit 9.7.3, neither of which exists in the repository today (Section 2's disclosed finding; Section 8, below).
3. **Structural retrieval remains primary.** Not yet implemented — Section 6, gap row 2; Section 7.1.
4. **Closed Pre-computation candidate set.** Partially already computed as a byproduct of the existing algorithm (Section 4.2's genuine finding) — requires restructuring, not new dereference — Section 6, gap row 3; Section 7.1.
5. **Minimum semantic content.** Already isolated by the existing `resolveContent`/`normalize` pair — the single normalized `String` per candidate this class already produces for structural matching is, unchanged, the minimum content boundary Contract Design Amendment §3 fixes (deliberately wider than Unit 9.7's `basis`-only boundary, per that section's own adopted text) — Section 6, gap row 5, requires verification only.
6. **Opaque request-scoped identity.** Not yet implementable — reuses Unit 9.7.1's token type unchanged, once it exists — Section 6, gap row 4; Section 8.
7. **Semantic result is not authority.** Governed by reusing Unit 9.7.1's mechanism interface unchanged; no new interface authorised here.
8. **Three-check Pre-disclosure verification.** Not yet implemented as a fallback-path re-verification — but all three underlying operations (`KnowledgeItemPersistence.find`, `PermissionEngine.evaluate`, `MemoryRetrieval.getAssertion`/`getEntity`) already exist and are already used once during Pre-computation; this is a second invocation, never a new capability (Task-b Case A precedent, restated by the adopted Successor document §10) — Section 6, gap row 7; Section 7.4.
9. **Final content from fresh Memory Core reread.** Same basis as #8 — `SafeKnowledgeResultEntry` content must come from Pre-disclosure check C's own fresh dereference, never from the mechanism's output or the Pre-computation snapshot — Section 7.4.
10. **Permission remains Parker's.** Already satisfied — `PermissionEngine`/`DefaultPermissionEngine` require, and receive, zero modification anywhere in this plan.
11. **`SafeKnowledgeResultEntry` authority remains Parker's.** Already satisfied — `DefaultReasoningKnowledgeSource` remains the sole constructor of this type; no unit below changes that.
12. **`ReasoningContext` rendering unchanged.** Already satisfied by construction — `DefaultReasoningContextAssembler` and `DefaultReasoningPromptBuilder` are both confirmed (Section 2) to be opaque to how `recall`'s list was produced; no unit in Section 9 touches either file.
13. **Fail-closed behaviour.** Mostly already satisfied by this codebase's own existing "no `try`/`catch`, faults propagate unchanged" discipline, already established in `DefaultReasoningKnowledgeSource` itself (confirmed by its own KDoc and by fresh reading); new integrity-fault validation for tokens is the genuinely new component — Section 6, gap row 8; Section 13.
14. **Disposable semantic state.** Achieved by construction if the token/content mapping introduced in Section 7 is method-scoped only, mirroring Unit 9.7.2/9.7.3's own identical discipline — Section 6, gap row 10.
15. **Local/operator-controlled.** Governed entirely by whichever mechanism Unit 9.7.3's own spike selects; this plan does not re-decide it and introduces no independent selection of its own — Section 8.
16. **Determinism/version/configuration.** Reuses Unit 9.7.3's frozen mechanism identity/version/configuration unchanged; no independent Reasoning-Context-specific semantic configuration is authorised — Section 14.
17. **Live end-to-end proof.** Not yet demonstrated, and cannot be demonstrated by Unit 9.7 alone (Unit 9.7 plan §17's own disclaimer) — this plan's own closing unit, RKS.6 (Section 9), exists specifically to produce it, extending the existing `aadd596`-lineage fixture in `ParkerRuntimeReasoningContextIntegrationTest.kt`.

---

## 6. Gap Analysis

| Adopted requirement | Classification | Basis |
|---|---|---|
| Correct live surface (Frozen #1) | Already satisfied | Confirmed by fresh `ParkerRuntime.kt` composition inspection, Section 2. |
| Zero-result-only fallback trigger (Frozen #3) | Requires extension | Steps 7–8 are currently one combined per-item loop with no branch on "nothing matched." |
| Closed candidate set (Frozen #4) | Requires restructuring of existing computation, not new dereference | Section 4.2's genuine finding: `resolveContent` already dereferences every item-level-approved candidate today; the map merely needs to be retained rather than discarded. |
| Opaque request-scoped identifiers (Frozen #6) | Requires shared-contract availability | No such type exists in this codebase yet; must be Unit 9.7.1's own type, reused unchanged, never a second, Reasoning-Context-specific token type. |
| Minimum content (Frozen #5) | Requires verification only | The existing `resolveContent`/`normalize` pair already isolates exactly the boundary Contract Design Amendment §3 fixes; a new test is needed to prove nothing else (identity, provenance, permission state) is ever passed to the mechanism. |
| No authority transfer (Frozen #7) | Requires shared-contract availability + new orchestration | The mechanism interface itself (Unit 9.7.1) must already be shaped this way; this plan's own orchestration must never pass it anything beyond query text and the token→content map. |
| Permission integration, Pre-computation/Pre-disclosure (Frozen #8–#9) | Requires new orchestration | Pre-computation (item-level gate) already exists and already runs before dereference; a fresh Pre-disclosure three-check re-verification specific to the fallback path does not exist yet. |
| Fail-closed behaviour (Frozen #13) | Mostly already satisfied, requires new component for integrity faults only | This class's existing "no `try`/`catch`" discipline already gives mechanism failure a distinguishable, honest outcome for free; unknown/stale/duplicate/cross-request token rejection is a genuinely new validation. |
| Disposable semantic state (Frozen #14) | Requires new implementation component, verified by design discipline | Satisfied automatically if the token/content mapping is method-scoped, never a class-level field. |
| Local/operator-controlled (Frozen #15) | Requires shared-contract availability only | No independent decision needed or authorised — reuses Unit 9.7.3's own selection. |
| Shared mechanism/determinism (Frozen #2, #16) | Requires shared-contract availability | Blocked entirely on Unit 9.7.1/Unit 9.7.3 existing (Section 8). |
| Live end-to-end proof (Frozen #17) | Requires new verification only | The `aadd596`-lineage fixture already exists and already demonstrates the negative case; RKS.6 extends it to the positive case once RKS.2–RKS.5 exist. |

No change is invented anywhere the current implementation already satisfies a requirement — in particular, the act-level gate, the item-level gate, the existing `resolveContent`/`normalize` pair, the ordering-by-construction guarantee, and the staleness-disclosure mechanism are all reused unchanged.

---

## 7. Proposed Implementation Architecture

Names below are conceptual, per this task's own instruction — no concrete Kotlin declaration is frozen here; the smallest production architecture is described only at the level Sections 6 and 9 need to plan units and estimate impact.

### 7.1 Fallback trigger, restructured from existing computation

`DefaultReasoningKnowledgeSource.recall` separates its current single combined loop (Section 4.1, steps 7–8) into two passes over `itemApproved`: an unmodified dereference pass producing an item→content map (exactly `resolveContent` as it exists today, called once per item, `null` results dropped exactly as today), and a structural-match pass applying the existing `content.contains(query.relevance, ignoreCase = true)` test against that map. If the structurally-matched subset is non-empty, today's exact, unmodified behaviour continues — one or more structural matches prevent fallback, regardless of how many ultimately would have survived any later step (permission denial and lifecycle exclusion are already resolved earlier, at steps 4–6, and are therefore never themselves a fallback trigger by construction). Only when the structurally-matched subset is empty, and the underlying item→content map itself is non-empty, does the new, additive branch below run. If the item→content map is itself empty (nothing item-level-approved successfully dereferenced), there is no candidate for the mechanism to rank; the query returns the existing empty result exactly as today, and the mechanism is never invoked.

### 7.2 Closed candidate set

The closed, Pre-computation-admitted candidate set for the fallback path is exactly the item→content map Section 7.1 already computed — no second dereference, no second permission evaluation, and no widening beyond what steps 6–8 already lawfully produced (Frozen Boundary #4). This is the material difference from Unit 9.7.2's own Section 7.2, noted in Section 4.2.

### 7.3 Opaque tokens and minimum content

A new, request-scoped identifier is minted per candidate in the map above, using Unit 9.7.1's own token type unchanged — never a second, Reasoning-Context-specific token type — held only in a local, method-scoped mapping from token to the item's own already-computed, already-normalized content string (never a class field, never persisted). This mapping and its tokens cease to exist when `recall` returns.

### 7.4 The mechanism boundary, canonical re-resolution, and fresh Pre-disclosure re-verification

The mechanism is invoked through Unit 9.7.1's interface, backed by Unit 9.7.3's concrete implementation, both reused unchanged — this plan declares no new mechanism interface and no second concrete mechanism. Every token the mechanism returns is resolved back to its own `KnowledgeItem` by direct lookup in the closed candidate set built in Section 7.2 — never trusted, never substituted; an absent or repeated token is an integrity fault (Section 13). Immediately before constructing a result entry for a resolved item, three fresh checks run, exactly as the adopted Successor document's §10 fixes and Task b's Case A analysis establishes as a second invocation of already-governed operations, never a new capability: **(A)** `persistence.find(item.knowledgeId)` — `KnowledgeItemPersistence.find`, confirmed to already exist (Section 2) — to confirm the item is still known to persistence; **(B)** a fresh `permissionEngine.evaluate` call using the identical item-level intent string `itemLevelIntent(item)` already computes today, to confirm permission currentness; **(C)** a second, fresh invocation of `evidenceMemoryRetrieval.getAssertion`/`getEntity` against the item's own `evidenceReference` — the genuinely new invocation type Section 4.2 identifies, not required by Unit 9.7's own simpler two-check pattern — to confirm the referenced Memory Core record is still `ACTIVE` and to obtain the current content. `SafeKnowledgeResultEntry` is constructed from check C's own freshly re-dereferenced content only, never from the Pre-computation snapshot (Section 7.1) and never from the mechanism's own output. An item failing any of the three checks is excluded, never disclosed, never substituted.

### 7.5 Fallback coordinator, or inline extension

Whether Sections 7.1–7.4 are expressed as new private methods inline within `DefaultReasoningKnowledgeSource` (mirroring how `resolveContent`, `normalize`, `disclosureFor`, and `isAuthorised` already live as private methods on that same class) or factored into a narrowly-scoped sibling collaborator is an implementation-tier decision, not fixed here — Section 9's own unit boundaries do not depend on which is chosen. Preferring extension of `DefaultReasoningKnowledgeSource`'s own existing, already-reviewed class over new parallel infrastructure is the default expectation, mirroring Unit 9.7 plan §7.6's own identical preference, unless the drafting unit finds the combined class becomes genuinely difficult to review as one file.

---

## 8. Dependency Relationship to Unit 9.7

**This plan depends on Unit 9.7.1 (Relevance Contract Types) and Unit 9.7.3 (Local Relevance Mechanism Adapter), both reused unchanged. It does not depend on Unit 9.7.2, Unit 9.7.4, or Unit 9.7.5.**

Determined from the adopted Unit 9.7 Implementation Plan's own text, not assumed:

- **Unit 9.7.1 (Relevance Contract Types) — a dependency, reused unchanged, never extended by this plan.** This plan's own Section 7.3/7.4 explicitly reuses Unit 9.7.1's opaque token type and mechanism interface unchanged, per the adopted Successor document §8's own "Shared Unit 9.7 Mechanism Reuse" requirement. No unit in Section 9 may begin until Unit 9.7.1 exists. RKS.1 (Section 9) is a strict compatibility gate against this dependency, not an extension point: if Unit 9.7.1's contract cannot be reused unchanged, that is a defect against Unit 9.7.1 itself, returned there for correction — this plan holds no authority to modify, widen, or additively extend it.
- **Unit 9.7.3 (Local Relevance Mechanism Adapter) — a dependency.** This plan's own mechanism invocation (Section 7.4) requires a concrete implementation of Unit 9.7.1's interface to exist; Unit 9.7.3 is that implementation, selected through Unit 9.7 plan §13's own spike and §13.1's own mandatory semantic-fitness gate, reused unchanged. RKS.3 (Section 9) cannot begin until Unit 9.7.3 is complete.
- **Unit 9.7.2 (Fallback Trigger and Closed Candidate Set) — not a dependency.** Unit 9.7.2's own "Expected repository impact" (Unit 9.7 plan §8) is `src/runtime/DefaultKnowledgeRetrieval.kt` exclusively — a structurally separate class this plan never touches. This plan's own RKS.2 (Section 9) implements the analogous fallback-trigger pattern independently, inside `DefaultReasoningKnowledgeSource`, per the deliberate, adopted, permanent duplication precedent Unit 9.7 plan §4.3 itself already documents between these two classes ("No duplicated semantic implementation" — the orchestration pattern is duplicated by design; only the mechanism itself, Unit 9.7.1/9.7.3, is shared).
- **Unit 9.7.4 (Integrity Validation, Canonical Re-resolution, and Pre-disclosure Re-verification) — not a dependency.** Unit 9.7.4's own repository impact is the same `DefaultKnowledgeRetrieval.kt` file, continuing Unit 9.7.2's own branch. This plan's RKS.4 implements the analogous, but not identical (Section 4.2), Pre-disclosure logic independently, inside `DefaultReasoningKnowledgeSource`.
- **Unit 9.7.5 (Runtime Composition Wiring) — not a dependency.** Unit 9.7.5 wires Unit 9.7.3's mechanism into `DefaultKnowledgeRetrieval`'s own construction at `ParkerRuntime.kt` line 956. This plan's own RKS.5 wires the same Unit 9.7.3 mechanism instance into `DefaultReasoningKnowledgeSource`'s own, entirely separate, construction at `ParkerRuntime.kt` lines 970–975 — a parallel wiring act, not a dependent one. Unit 9.7.5 completing first is not required, provided Unit 9.7.3 itself (the object Unit 9.7.5 also wires) is complete; a single Unit 9.7.3 mechanism instance, or two independently constructed instances of the same frozen configuration (an implementation-tier choice, not fixed here), may be injected into both `DefaultKnowledgeRetrieval` and `DefaultReasoningKnowledgeSource` without either wiring act depending on the other's completion.
- **Unit 9.7.6 (Full Verification Matrix and Regression Confirmation) — not a dependency**, for the same reason as 9.7.2/9.7.4/9.7.5: it verifies `DefaultKnowledgeRetrieval`'s own units complete, a disjoint concern from this plan's own RKS.6 (Section 9).

**Current state of the dependency.** Confirmed by the repository-wide search in Section 2: **neither Unit 9.7.1 nor Unit 9.7.3 exists in the repository as of this plan's own drafting.** This plan is therefore fully blocked today. Per the adopted Unit 9.7 Implementation Plan's own §18 ordering diagram, Unit 9.7.3 itself cannot begin in earnest until Unit 9.7.1 exists and §13's spike selects a mechanism — this plan's own dependency chain is therefore, transitively, gated on that same spike and its §13.1 semantic-fitness gate, exactly as Unit 9.7.5/9.7.6 are.

**Earliest lawful parallelisation.** This plan's own RKS.1 and RKS.2 (Section 9) require only Unit 9.7.1's interface to exist — they may proceed in parallel with Unit 9.7.2, 9.7.4, 9.7.5, and with Unit 9.7.3 itself, since none of those units' own repository impact ever touches a file this plan's units touch, and neither depends on the other's completion. **RKS.3 requires both RKS.2 complete and Unit 9.7.3 complete — a join, never an either-or gate.** Unit 9.7.3 may proceed concurrently with RKS.1/RKS.2 (their work is mutually independent), but that concurrency only shortens wall-clock time; it does not let RKS.3 begin before RKS.2 has finished, and it does not let RKS.3 begin before Unit 9.7.3 has finished, whichever of the two happens to complete later. RKS.4 depends on RKS.2 and RKS.3 (mirroring Unit 9.7.4's own dependency on 9.7.2/9.7.1). RKS.5 depends on RKS.2–RKS.4 complete. RKS.6 is strictly final for this plan. This plan does not assume all six Unit 9.7 units must finish first — only 9.7.1 and 9.7.3 are genuine prerequisites, and even 9.7.3 need not wait for 9.7.2, 9.7.4, 9.7.5, or 9.7.6, since none of those units modifies Unit 9.7.3's own mechanism-adapter artefact once it exists.

---

## 9. Implementation-Unit Decomposition

Units below are named **RKS.1–RKS.6** ("Reasoning Knowledge Source"), a numbering local to this plan, mirroring Unit 9.7's own "9.7.x" convention without colliding with it or with the existing Reasoning Context Implementation Plan's own "Implementation Unit 2"/"Implementation Unit 3" numbering (`src/runtime/DefaultReasoningKnowledgeSource.kt`'s own KDoc references). Whether these six sub-units are formally folded into that base Implementation Plan's own numbering as a future "Implementation Unit 4" is an editorial decision for a future revision of that document, not fixed here — exactly as Unit 9.7's own units received fresh "9.7.x" numbers rather than continuing Unit 9's own "9.x" sequence.

### RKS.1 — Shared Contract Compatibility Gate

**Objective.** A strict compatibility gate, not an extension point. Confirm, once Unit 9.7.1 exists, that its opaque token type and mechanism interface are consumable from `DefaultReasoningKnowledgeSource` (package `parker.core.runtime`) exactly as declared, with zero modification and zero extension — `DefaultReasoningKnowledgeSource` already imports extensively from `parker.core.interfaces` (Unit 9.7.1's own expected package, alongside `KnowledgeStore.kt`), so no cross-package barrier is anticipated, but this is confirmed, not assumed, since Unit 9.7.1 does not exist yet to inspect. **This unit does not have, and this plan does not grant it, authority to modify, widen, or additively extend Unit 9.7.1's own shared contract for this plan's own convenience.** If Unit 9.7.1's interface, as adopted and implemented, cannot be reused unchanged by `DefaultReasoningKnowledgeSource` — for any reason, including an incompatibility this plan did not anticipate — RKS.1 stops. The finding is returned to Unit 9.7.1 itself as a defect against that unit's own governance and implementation, to be corrected there through Unit 9.7's own review discipline, never worked around inside this plan by declaring a second, parallel, or locally-widened interface. No RKS unit after RKS.1 may proceed until Unit 9.7.1's own contract is confirmed reusable unchanged, whether that confirmation is reached on first inspection or only after Unit 9.7.1 itself has been revised and re-completed.

**Dependencies.** Unit 9.7.1, complete.

**Expected repository impact.** None, under any outcome. RKS.1 is a verification unit; it declares, modifies, or extends no file. A genuine incompatibility is reported as a defect and stops this plan's own progress — it is never resolved by an edit made under this plan's own authority.

**Tests required.** None beyond existing Unit 9.7.1 tests — RKS.1 performs an inspection, not an implementation, and adds no production or test code of its own.

**Governance properties satisfied.** Frozen Boundaries #2, #6, #7 (interface availability confirmed exactly as declared, never widened).

**Completion criteria.** Unit 9.7.1's interface is confirmed directly, exactly, and unchangeably consumable. If it is not, RKS.1 does not complete — it stops, and the incompatibility is reported as a defect against Unit 9.7.1, outside this plan's own authority to resolve.

### RKS.2 — Fallback Trigger and Closed Candidate Set

**Objective.** Implement Section 7.1–7.2: split the existing combined dereference/match loop into a retained item→content map and a structural-match pass over it; branch to the new fallback path only on a genuine, non-empty-map, empty-match outcome.

**Dependencies.** RKS.1.

**Expected repository impact.** `src/runtime/DefaultReasoningKnowledgeSource.kt`, modified additively; no other production file.

**Tests required.** A test proving one or more structural matches prevent fallback (mirroring `DefaultReasoningKnowledgeSourceTest.kt`'s own existing positive-match cases, e.g. "an ACTIVE Assertion whose statement contains the query relevance is returned," continuing to pass unmodified as the no-fallback-needed case). A test proving an empty item→content map (nothing item-level-approved successfully dereferenced) returns the existing empty result without invoking the mechanism. A test proving the retained map is exactly the item-level-approved, successfully-dereferenced set — never a superset including denied or unresolvable candidates.

**Governance properties satisfied.** Frozen Boundaries #3, #4.

**Completion criteria.** Every existing `DefaultReasoningKnowledgeSourceTest.kt` case continues to pass unmodified in substance; the new tests above pass; no existing structural-match behaviour changes for any query whose structural match is non-empty.

### RKS.3 — Mechanism Invocation and Token Minting

**Objective.** Implement Section 7.3–7.4's first half: mint opaque tokens (Unit 9.7.1's type) for the closed candidate set, invoke the shared mechanism (Unit 9.7.3's concrete implementation) with query text and the token→content map, and resolve returned tokens back against the closed candidate set by direct lookup.

**Dependencies.** RKS.1, RKS.2, Unit 9.7.3 complete.

**Expected repository impact.** `src/runtime/DefaultReasoningKnowledgeSource.kt`, the same file RKS.2 extended.

**Tests required.** A test proving the mechanism receives only query text and the token→content mapping — never `KnowledgeId`, `MemoryCoreRecordReference`, `evidentialState`, `status`, `StalenessDisclosure`, or principal identity (mirrors Unit 9.7.1's own structural test, applied at this call site). A test proving an unknown or repeated returned token is rejected as an integrity fault.

**Governance properties satisfied.** Frozen Boundaries #5, #6, #7.

**Completion criteria.** Compiles; both tests pass; no candidate outside the closed set from RKS.2 is ever visible to the mechanism.

### RKS.4 — Three-Check Pre-Disclosure Re-Verification and Fresh-Content Construction

**Objective.** Implement Section 7.4's three checks (A: `KnowledgeItemPersistence.find`; B: fresh `PermissionEngine.evaluate`; C: fresh `evidenceMemoryRetrieval.getAssertion`/`getEntity`) for every token RKS.3 resolves, and construct `SafeKnowledgeResultEntry` from check C's own fresh content only.

**Dependencies.** RKS.2, RKS.3.

**Expected repository impact.** `src/runtime/DefaultReasoningKnowledgeSource.kt`, continuing the same additive branch.

**Tests required.** One test per fail-closed table row (Section 13) this unit owns: KnowledgeItem retired/removed between Pre-computation and Pre-disclosure excludes the candidate; permission revoked between the two phases excludes the candidate; Memory Core record become non-`ACTIVE` or deleted between the two phases excludes the candidate. A test proving Pre-disclosure re-verification is genuinely a second, fresh evaluation for all three checks — not a reuse of the Pre-computation decision — by varying persistence/policy/Memory Core state between admission and disclosure. A test proving disclosed content always comes from check C, never from the mechanism's own output or the Pre-computation snapshot, by deliberately diverging the two.

**Governance properties satisfied.** Frozen Boundaries #8, #9, and the integrity-fault portion of #13.

**Completion criteria.** Every fail-closed table row this unit owns is independently tested and passes; RKS.2 and RKS.3's own tests remain passing unmodified.

### RKS.5 — Runtime Composition Wiring

**Objective, explicitly separate and penultimate, mirroring Unit 9.7.5's own precedent.** Construct or reuse Unit 9.7.3's mechanism instance and inject it into `DefaultReasoningKnowledgeSource`'s own constructor at `ParkerRuntime.kt` lines 970–975, additive wiring only. This unit performs no change to `knowledgeItemPersistence`, `permissionEngine`, `reasoningContextMemoryRetrieval`, or `REASONING_CONTEXT_RETRIEVAL_PURPOSE` wiring, and no change to `knowledgeRetrieval`'s own line 956 wiring (Unit 9.7.5's exclusive territory).

**Dependencies.** RKS.1–RKS.4, complete; Unit 9.7.3, complete (Unit 9.7.5 need not itself be complete — Section 8).

**Expected repository impact.** `src/composition/ParkerRuntime.kt`, additive constructor-argument change only, at the exact site identified in Section 2.

**Tests required.** A composition test confirming the composed `DefaultReasoningKnowledgeSource` instance genuinely holds the new mechanism, mirroring `ParkerRuntimeReasoningKnowledgeSourceCompositionTest.kt`'s own existing precedent (e.g. its "the constructed `DefaultReasoningContextAssembler` holds a genuine `DefaultReasoningKnowledgeSource` instance" test). Confirmation that every other existing composition line in `ParkerRuntime.kt` is unaltered, in particular line 956's own `DefaultKnowledgeRetrieval` construction.

**Governance properties satisfied.** No new property — this unit only makes RKS.1–RKS.4 reachable in the composed graph.

**Completion criteria.** The full composed runtime builds and starts; `ParkerRuntimeReasoningKnowledgeSourceCompositionTest.kt` and `ParkerRuntimeKnowledgeRetrievalCompositionTest.kt` both continue to pass unmodified — the latter proving, by its continued unmodified passage, that `DefaultKnowledgeRetrieval`'s own wiring remains genuinely untouched.

### RKS.6 — Live-Reasoning End-to-End Verification

**Objective.** Extend `ParkerRuntimeReasoningContextIntegrationTest.kt`'s existing `aadd596`-lineage fixture — specifically its "a genuinely related paraphrase does not recall a promoted proposition under the current literal substring retrieval" and "six genuine promoted memories remain canonically stored while the current retrieval still misses the emergency vet paraphrase" tests — with a new, positive-recall counterpart proving the identical paraphrase now **does** recall the target proposition through the real, fully composed `ParkerRuntime`, and that the existing "the assembled ReasoningContext's entries reach the real prompt sent to the model, unchanged" test's own pattern holds for the newly-recalled entry too. This is the Frozen Boundary #17 proof: promoted canonical proposition → structurally-dissimilar paraphrase → structural match = zero → shared mechanism surfaces the correct candidate → fresh three-check Pre-disclosure succeeds → `SafeKnowledgeResultEntry` built from current content → `ReasoningContext` contains the result → the real assembled model prompt contains it.

**Dependencies.** RKS.1–RKS.5, all complete.

**Expected repository impact.** `tests/composition/ParkerRuntimeReasoningContextIntegrationTest.kt`, extended additively; no production file.

**Tests required.** The positive-recall end-to-end test described above. A negative-discrimination companion, reusing the six existing distractor memories, proving they are not spuriously recalled by the paraphrase query. Full, unmodified passage of every existing test named in Section 16.

**Governance properties satisfied.** Frozen Boundary #17 in full; confirms Section 6's every "requires new orchestration" row is genuinely closed.

**Completion criteria.** Every row of Section 15's verification matrix owned by RKS.1–RKS.6 is independently verified; every test named in Section 16 passes unmodified in substance; a fresh clone of the repository builds and tests successfully.

---

## 10. Expected Affected Files

| File | Nature of change |
|---|---|
| `src/runtime/DefaultReasoningKnowledgeSource.kt` | Modified additively — RKS.2, RKS.3, RKS.4 |
| `src/composition/ParkerRuntime.kt` | Modified additively — RKS.5, at lines 970–975 only |
| `tests/runtime/DefaultReasoningKnowledgeSourceTest.kt` | Extended additively — RKS.2, RKS.3, RKS.4; no existing case modified |
| `tests/composition/ParkerRuntimeReasoningKnowledgeSourceCompositionTest.kt` | Extended additively — RKS.5 |
| `tests/composition/ParkerRuntimeReasoningContextIntegrationTest.kt` | Extended additively — RKS.6 |

No unit in this plan modifies or extends Unit 9.7.1's own contract-type file, under any outcome (Section 9, RKS.1). A genuine incompatibility there is a defect returned to Unit 9.7.1, resolved by that unit's own separate governance and review, never by an edit this plan authorises.

Explicitly **not** expected to change, and confirmed zero-diff at RKS.6's own completion: `src/runtime/DefaultReasoningContextAssembler.kt`; `src/runtime/ReasoningPromptBuilder.kt`; `src/interfaces/KnowledgeStore.kt` and Unit 9.7.1's own new contract-type file (both untouched by this plan — either already contains what Unit 9.7.1 itself adopted, or RKS.1 has stopped); `src/interfaces/MemoryCore.kt`; `src/composition/PermissionFilteredMemoryRetrieval.kt`; `src/runtime/KnowledgeItemPersistence.kt`; `src/interfaces/PermissionEngine.kt`; `src/runtime/DefaultPermissionEngine.kt`; `src/runtime/DefaultKnowledgeRetrieval.kt`; every Memory Core implementation file; every adopted governance document.

---

## 11. Permission Integration

Unchanged: `PermissionEngine` remains the sole permission authority; the existing act-level and item-level gates are neither reordered nor shortened. This plan adds exactly one new evaluation moment — the Pre-disclosure check B (Section 7.4, RKS.4) — which reuses the existing item-level gate mechanism and the existing `itemLevelIntent(item)` string unchanged; it is a second invocation of an existing gate, at a new point in the control flow, never a new gate, a new outcome type, or a new permission pathway. No unit in Section 9 constructs an `ExecutionRequest` naming a new resource or action beyond `REASONING_CONTEXT_RETRIEVAL_RESOURCE_ID`/`RETRIEVE_FOR_REASONING_CONTEXT_ACTION_NAME`/`ACT_LEVEL_INTENT`, all already registered.

---

## 12. Memory Core / Fresh-Content Handling

No unit in this plan introduces a new `MemoryRetrieval` dependency, a new Memory Core call kind, or any change to `src/interfaces/MemoryCore.kt` or any Memory Core implementation file. The Pre-disclosure check C (Section 7.4, RKS.4) reuses the identical, already purpose-bound `evidenceMemoryRetrieval` (`reasoningContextMemoryRetrieval`, scoped to `REASONING_CONTEXT_RETRIEVAL_PURPOSE`) this class already holds — a second invocation of `getAssertion`/`getEntity`, never a new collaborator, never a raw or differently-bound view. The content boundary remains exactly what `resolveContent`/`normalize` already produce today; this plan does not widen it, and does not authorise passing `MemoryCoreRecordReference`, `evidentialState`, `KnowledgeItemStatus`, or `StalenessDisclosure` to the mechanism at any point (Frozen Boundary #5).

---

## 13. Failure Handling

Mapped to the adopted Successor document's own §13 fail-closed table, with owning RKS unit:

| Failure | Constitutional requirement | Owning unit |
|---|---|---|
| Mechanism unavailable, timeout, or relevance computation failure | Propagates as a thrown exception, uncaught, mirroring `DefaultReasoningKnowledgeSource`'s own existing "no `try`/`catch`" discipline; the already-computed, zero-result structural outcome stands, never re-executed | RKS.3 |
| Successful semantic computation returning zero relevant candidates | Genuine, distinguishable Empty result — mechanism returns an empty token list, no `SafeKnowledgeResultEntry` is constructed, `recall` returns the existing empty behaviour | RKS.3 |
| Malformed ranking result | Fail loudly, reject the whole result | RKS.3 |
| Mechanism returns candidate content instead of identifiers | Rejected as an integrity fault | RKS.3 |
| Unknown or unauthorized returned identifier | Rejected as an integrity fault | RKS.3 |
| Duplicate identifier | De-duplicated before bounding; never consumes more than one `maximumResults` slot | RKS.3 |
| Too many results returned | The mechanism's own limit is a hint only; Parker's existing `take(query.maximumResults)` bounding remains authoritative | RKS.4 |
| Stale/cross-request token | Rejected as an integrity fault | RKS.3 |
| KnowledgeItem retired/removed between Pre-computation and Pre-disclosure | Excluded by Pre-disclosure check A, never disclosed, never substituted | RKS.4 |
| Permission revoked between Pre-computation and Pre-disclosure | Excluded by Pre-disclosure check B | RKS.4 |
| Memory Core record missing/non-`ACTIVE` at Pre-disclosure | Excluded by Pre-disclosure check C | RKS.4 |
| Mechanism attempts to enumerate or reach canonical persistence | Architecturally impossible by omission — inherited unchanged from Unit 9.7.1's own interface shape, never re-authorised here | RKS.1, RKS.3 |

Semantic failure never causes structural matching to run again — the fallback trigger (RKS.2) is evaluated exactly once, before the mechanism is ever invoked, so no code path exists in which mechanism failure could trigger a second structural pass.

---

## 14. Determinism/Shared Mechanism

The existing determinism guarantee (`DefaultReasoningKnowledgeSource`'s own ordering-by-construction — `List` operations only, no sorting or randomisation anywhere in the class) is extended to treat the mechanism's own identity, version, and configuration — Unit 9.7.3's own frozen, disclosed state — as retrieval-relevant here too: for unchanged persisted state, an unchanged permission policy, an unchanged mechanism identity/version/configuration, and a fixed instant, the same query yields an identical result, in identical order, on repeated invocation. This plan does not authorise a second, independently configured mechanism instance with different settings from the one Unit 9.7.5 composes for `DefaultKnowledgeRetrieval` — RKS.5 reuses Unit 9.7.3's own frozen configuration unchanged (Section 9), never a Reasoning-Context-specific variant.

---

## 15. Verification Matrix

| Property | Required test/inspection | Owning unit |
|---|---|---|
| Structural result prevents semantic fallback | One or more structural matches → no mechanism invocation | RKS.2 |
| Exact-zero structural result (non-empty candidate map) permits fallback | Zero structural matches, non-empty dereferenced map → mechanism invoked | RKS.2 |
| Permission denial is not a fallback trigger | Already resolved at step 6, before structural matching; a dedicated test confirms a denied candidate never appears in the candidate map | RKS.2 |
| Lifecycle exclusion is not a fallback trigger | Already resolved at step 5; a dedicated test confirms a `RETIRED`-excluded item never appears in the candidate map | RKS.2 |
| Memory dereference failure is not a fallback trigger | A candidate `resolveContent` returns `null` for is simply absent from the map, never causing fallback by itself if any other candidate matched | RKS.2 |
| Structural failure does not permit fallback | A thrown exception during dereference/matching never reaches the fallback branch | RKS.2 |
| Mechanism receives only minimum content | Reflection/inspection test at the RKS.3 call site | RKS.3 |
| `KnowledgeId`/`MemoryCoreRecordReference` never cross the mechanism boundary | Same test as above, extended | RKS.3 |
| Token cannot resolve cross-request | Token minted in one `recall` call rejected if presented to a resolution step from a different call | RKS.3 |
| Unknown/duplicate/stale token rejected | Fail-closed table tests | RKS.3 |
| Too-many-result handling | Existing `take(maximumResults)` bounding, confirmed still authoritative | RKS.4 |
| Malformed semantic result handling | Fail-closed table test | RKS.3 |
| Semantic timeout/unavailability | Fail-closed table test | RKS.3 |
| Semantic failure vs. semantic empty distinction | Two independent tests | RKS.3 |
| Fresh `KnowledgeItemPersistence.find` occurs at Pre-disclosure | RKS.4 test, varying persistence state between phases | RKS.4 |
| Fresh `PermissionEngine.evaluate` occurs at Pre-disclosure | RKS.4 test, varying policy state between phases | RKS.4 |
| Fresh `MemoryRetrieval` dereference occurs at Pre-disclosure | RKS.4 test, varying Memory Core state between phases | RKS.4 |
| Current Memory Core content used for final entry | RKS.4 test, diverging mechanism output from check C's own content | RKS.4 |
| Revoked permission excludes result | Fail-closed table test | RKS.4 |
| Retired/removed KnowledgeItem excludes result | Fail-closed table test | RKS.4 |
| Non-`ACTIVE`/missing Memory Core record excludes result | Fail-closed table test | RKS.4 |
| No persistent semantic state | Structural test — no new class-level field | RKS.2/RKS.3 |
| No new Memory Core call kind | Zero diff in `MemoryCore.kt` and every implementation | RKS.6 |
| No new `PermissionEngine` behaviour | Zero diff in `PermissionEngine.kt`/`DefaultPermissionEngine.kt` | RKS.6 |
| No new `ReasoningContext` rendering behaviour | Zero diff in `DefaultReasoningContextAssembler.kt`/`ReasoningPromptBuilder.kt` | RKS.6 |
| Shared mechanism identity/configuration with Unit 9.7 | Confirmed by construction — RKS.5 injects Unit 9.7.3's own instance/configuration unchanged | RKS.5 |
| Deterministic repeat behaviour | Determinism test, fixed configuration | RKS.5/RKS.6 |
| Accepted semantic positive fixture reproduced | `aadd596`-lineage positive-recall extension | RKS.6 |
| Negative/control discrimination | Six-distractor companion test | RKS.6 |
| Proposition fidelity | Inherited from Unit 9.7.3's own semantic-fitness gate; not re-tested here beyond the positive/negative fixtures above | RKS.6 |
| Ranking fidelity | Inherited from Unit 9.7.3; confirmed only at the level "the correct candidate is surfaced," not re-litigated | RKS.6 |
| Repetition stability | Determinism test, repeated invocation | RKS.6 |
| Live same-runtime `ReasoningContext` proof | RKS.6's own positive-recall test, extending "the assembled ReasoningContext's entries reach the real prompt sent to the model, unchanged" | RKS.6 |
| Model prompt/live reasoning path proof | Same test, asserting the recalled entry's rendered form appears in `DefaultReasoningPromptBuilder.buildPrompt`'s own output | RKS.6 |

---

## 16. Regression Requirements

Must continue to pass, entirely unmodified in substance: `tests/runtime/DefaultReasoningKnowledgeSourceTest.kt` (37 cases); `tests/composition/ParkerRuntimeReasoningKnowledgeSourceCompositionTest.kt` (17 cases); `tests/runtime/DefaultReasoningContextAssemblerTest.kt` (13 knowledge/memory-relevant cases, including "an empty recall result produces no Memory entries but calls recall exactly once" and "a single returned `SafeKnowledgeResultEntry` is rendered as one Memory entry in the exact frozen format"); `tests/composition/ParkerRuntimeReasoningContextIntegrationTest.kt` (12 cases, all pre-existing ones including the `aadd596`-lineage negative fixture, which must continue to demonstrate the *pre-fallback* baseline is unchanged for any query this plan's own fallback path does not alter); `tests/runtime/MemoryRetrievalPermissionPolicyOperationalisationTest.kt` (11 cases); `tests/runtime/DefaultPermissionEngineTest.kt` (10 cases); `tests/composition/PermissionFilteredMemoryRetrievalTest.kt` (25 cases); `tests/runtime/KnowledgeItemPersistenceTest.kt` (8 cases) and `tests/runtime/DurableKnowledgeItemPersistenceTest.kt` (10 cases); every Memory Core test; every Unit 9.7 shared-mechanism test (Unit 9.7.1's and Unit 9.7.3's own test files), once those exist. No existing test may be weakened, deleted, or have its assertions loosened to accommodate this plan's own capability — any test that would need to change is itself a signal that a unit above has exceeded its own scope. Full-repository build and test verification is required after RKS.6, not merely the tests each unit's own drafting names.

---

## 17. Out-of-Scope Items

This plan does not include, and no unit in Section 9 may be read as authorising: MUEP; Docling; document ingestion; evidence analysis; external AI reasoning; OpenAI/Anthropic integration; Output Composer; docx4j; UI work; server deployment; Memory Core redesign; Evidence Custodian changes; persistent vector storage; remote relevance; a second semantic mechanism; direct Reasoning Context → `KnowledgeRetrieval` cutover; re-running or re-evaluating Unit 9.7's own mechanism-selection spike (Section 13/13.1 of that plan) — this plan reuses that spike's outcome, it does not repeat it; any modification to `DefaultKnowledgeRetrieval.kt` or to Unit 9.7's own §8 units (9.7.2, 9.7.4, 9.7.5, 9.7.6); any modification, widening, or additive extension of Unit 9.7.1's own shared contract type or mechanism interface, for any reason, including a genuine incompatibility RKS.1 discovers — that finding is a defect returned to Unit 9.7.1's own governance and implementation, never a change this plan authorises itself to make; any modification to any adopted governance document, including the three Reasoning Context sibling documents this plan itself implements.

---

## 18. Completion Criteria

**A unit is complete when:** it compiles; every existing test affected by it still passes unmodified in substance; every new test named for it in Section 9 passes; no later unit in Section 19's own ordering was required to reach this state.

**A. This dedicated implementation unit is complete when:** all six RKS units in Section 9 are complete by the above definition; every row of Section 15's verification matrix is independently verified; every test named in Section 16 passes unmodified; a fresh clone of the repository builds and tests successfully with no partially-implemented unit's state present; Bounded Relevance Computation is reachable through `DefaultReasoningKnowledgeSource` exactly as the three adopted Reasoning Context sibling documents authorise, and through no other path.

**B. Programme-level practical live semantic recall is achieved only when**, in addition to (A): Unit 9.7 itself (its own six units) is separately complete; RKS.6's own live same-runtime proof (Section 9) passes against the real, fully composed `ParkerRuntime`; and no report, review, or adoption action describes any lesser state — code compiling, the mechanism being wired, or any individual RKS unit's own completion short of RKS.6 — as "fixing semantic recall." This mirrors, and does not weaken, the identical disclaimer the adopted Unit 9.7 Implementation Plan's own §17 already states for its own, narrower scope.

---

## 19. Implementation Order

```
                              [BLOCKED until Unit 9.7.1 exists]
                                            │
                                            ▼
                    RKS.1 (Shared Contract Compatibility Gate)
                    [STOPS here, defect returned to Unit 9.7.1,
                     if the contract cannot be reused unchanged]
                                            │
                    ┌───────────────────────┴───────────────────────┐
                    ▼                                                 ▼
       RKS.2 (Fallback Trigger +                        Unit 9.7.3 (Local Relevance
       Closed Candidate Set)                             Mechanism Adapter) — proceeds
                    │                                     independently, gated on Unit
                    │                                     9.7's own §13 spike + §13.1
                    │                                     semantic-fitness gate
                    │                                                 │
                    └───────────────────────┬───────────────────────┘
                                            ▼
                       RKS.3 (Mechanism Invocation + Token Minting)
                       [JOIN — requires BOTH RKS.2 complete AND
                        Unit 9.7.3 complete; neither alone suffices]
                                            │
                                            ▼
                       RKS.4 (Three-Check Pre-Disclosure +
                              Fresh-Content Construction)
                                            │
                                            ▼
                       RKS.5 (Runtime Composition Wiring)
                                            │
                                            ▼
                       RKS.6 (Live-Reasoning End-to-End Verification)
                                            │
                                            ▼
                          Programme-level objective achieved
                          (live semantic recall), Section 18(B)
```

RKS.1 may proceed as soon as Unit 9.7.1 exists; it either confirms the contract reusable unchanged or stops, per Section 9. RKS.2 requires only RKS.1 complete — it does not require Unit 9.7.3, Unit 9.7.2, Unit 9.7.4, or Unit 9.7.5. Unit 9.7.3 may proceed concurrently with RKS.1/RKS.2, since neither depends on the other. **RKS.3 is a join: it requires both RKS.2 complete and Unit 9.7.3 complete, and may not begin on the strength of either alone** — Unit 9.7.3 completing early does not let RKS.3 skip ahead of RKS.2, and RKS.2 completing early does not let RKS.3 skip ahead of Unit 9.7.3 (itself transitively gated on Unit 9.7's own §13 spike and §13.1 gate, per Section 8). RKS.4 depends on RKS.2 and RKS.3 both complete. RKS.5 is strictly last but one; RKS.6 is strictly final. This plan's own units may proceed in parallel with Unit 9.7.2, 9.7.4, 9.7.5, and 9.7.6 throughout, per Section 8's own determination — none of those units' repository impact ever intersects this plan's own.

---

## 20. Adoption Record

This plan underwent Independent Implementation Plan Review (VERDICT — REVISE BEFORE ACCEPTANCE, identifying the RKS.1 compatibility-gate defect and the RKS.3 dependency-ordering defect; architecture Accepted In Principle; no other blocking defect identified), a bounded defect-correction pass addressing exactly those two defects, and Defect Confirmation Review (VERDICT — ACCEPT, confirming both defects resolved, no regression introduced, and no new blocking defect remaining), mirroring the identical review discipline the adopted Unit 9.7 Implementation Plan itself underwent. This plan is now Adopted. No RKS unit may begin implementation on the strength of this adoption alone, and none may begin at all — regardless of this plan's own adopted status — until Unit 9.7.1 (all units) and Unit 9.7.3 (RKS.3 onward) are themselves independently complete, per Section 8. This plan itself modifies no adopted governance document, implements no Kotlin, and authorises no work beyond translating the three adopted Reasoning Context sibling documents into the ordered engineering units Section 9 fixes.
