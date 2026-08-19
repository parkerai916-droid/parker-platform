**Status:** Adopted. This implementation plan underwent Independent Implementation Plan Review, which returned a verdict of REVISE BEFORE ACCEPTANCE identifying two blocking defects: (1) the governed Unit 9.7 capability does not reach Parker's live reasoning path; (2) the Section 13 mechanism-selection spike lacked a mandatory semantic-fitness gate. A bounded revision addressed both findings — Section 4.2/4.3 classify the reachability gap as Case 3 (a separate, future `ReasoningKnowledgeSource` governance dependency, not silently resolved by Unit 9.7), Section 8 gained an explicit Live-Reasoning Integration gate, and Section 13.1 added ten mandatory semantic-fitness acceptance requirements for the mechanism-selection spike. Defect Confirmation Review then returned a verdict of ACCEPT; no unresolved implementation-plan defect remains. Adoption makes this plan the authoritative implementation sequence for Unit 9.7. Adoption does not implement any unit; does not resolve Parker's live Reasoning Context semantic-recall gap; does not authorise modification of `DefaultReasoningKnowledgeSource`; and does not authorise, by implication, the future Reasoning Context sibling governance package Section 4.3 identifies. The units this plan defines (Section 8) may now proceed, but only through their own required unit-level workflow — this document's own adoption is not, by itself, authorisation to begin implementation. No Kotlin is implemented, proposed as a diff, or changed by this document. Neither `src/` nor `tests/` is touched. No existing governance document is modified — `docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_BOUNDED_SEMANTIC_RELEVANCE_SCOPE_LOCK_AMENDMENT.md` (Adopted), `docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_CONTRACT_DESIGN_V2_BOUNDED_SEMANTIC_RELEVANCE_AMENDMENT.md` (Adopted), `docs/governance/PROGRAMME_3_UNIT_9_7_BOUNDED_SEMANTIC_RELEVANCE_CONTRACT_AND_PERMISSION_SUCCESSOR.md` (Adopted), `docs/decisions/CDR-008_MEMORY_CORE_DOWNSTREAM_RELEVANCE_BOUNDARY.md` (Accepted, Canonical, Frozen), `docs/governance/PROGRAMME_3_UNIT_9_SEMANTIC_RELEVANCE_SCOPE_LOCK_REVISION_PROPOSAL.md` (Adopted), `docs/governance/PROGRAMME_3_UNIT_9_KNOWLEDGE_RETRIEVAL_CONTRACT_DESIGN.md` (Adopted), and `docs/governance/PROGRAMME_3_UNIT_9_PERMISSION_ENFORCEMENT_MECHANISM_CLARIFICATION.md` (Adopted) are each cited, never altered.

# Programme 3 — Unit 9.7: Bounded Semantic Relevance Implementation Plan

Programme: **Programme 3 — Knowledge Memory, Unit 9.7 Implementation Plan (Bounded Semantic Relevance).**

This document is the detailed engineering breakdown of a single capability already authorised at three adopted governance tiers — the Scope Lock Amendment, the Contract Design V2 Amendment 9, and the Unit 9.7 Contract and Permission Successor — exactly as `PROGRAMME_3_UNIT_9_KNOWLEDGE_RETRIEVAL_IMPLEMENTATION_PLAN.md` already did for Unit 9 itself. It translates the adopted contract into discrete, ordered, independently verifiable engineering units. It does not revisit, reweigh, or reopen anything any adopted document already settled, and it does not perform, plan, or authorise implementation of any kind — this document is planning only.

---

## 1. Status

Adopted, per the Status block above. Independent Implementation Plan Review returned REVISE BEFORE ACCEPTANCE; a bounded correction pass addressed both findings; Defect Confirmation Review returned ACCEPT. This document introduces no Kotlin and modifies no existing test. The units named in Section 8, below, may now proceed, but only through their own required unit-level workflow — adoption of this plan is not, by itself, authorisation for any unit to begin.

---

## 2. Authoritative Governance Basis

Read fresh, in full, before drafting:

- `docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_BOUNDED_SEMANTIC_RELEVANCE_SCOPE_LOCK_AMENDMENT.md` (Adopted) — the single authorised capability (§1), what remains authoritative (§4), what the mechanism may and may not do (§5–§6), permission boundaries (§7), fail-closed expectations (§8), provenance/identity preservation (§9), exclusions (§10), and the explicit prohibition on unintended architectural expansion (§12).
- `docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_CONTRACT_DESIGN_V2_BOUNDED_SEMANTIC_RELEVANCE_AMENDMENT.md` (Adopted, Amendment 9) — the Relevance Request/Relevance Result contract-tier concepts (§2–§3), canonical identity preservation (§4), permission behaviour with the Pre-computation/Pre-disclosure distinction (§7), failure behaviour (§8), ordering/ranking semantics and the exact fallback trigger (§9), integration boundaries (§10), and implementation-neutrality (§11).
- `docs/governance/PROGRAMME_3_UNIT_9_7_BOUNDED_SEMANTIC_RELEVANCE_CONTRACT_AND_PERMISSION_SUCCESSOR.md` (Adopted) — the fourteen-condition Model A-Strict production boundary (§3), the required contract surface (§4), permission integration (§5), the full fail-closed table (§6), canonical-memory authority (§7), semantic candidate status (§8), prohibited behaviours (§9), and required verification properties (§10).
- `docs/decisions/CDR-008_MEMORY_CORE_DOWNSTREAM_RELEVANCE_BOUNDARY.md` (Accepted, Canonical, Frozen) — Decision A and its Mandatory Invariants, establishing that Memory Core Scope Lock's prohibition binds Memory Core's own interface, not a downstream, separately-governed component.
- `docs/governance/PROGRAMME_3_UNIT_9_SEMANTIC_RELEVANCE_SCOPE_LOCK_REVISION_PROPOSAL.md` (Adopted) — §10 (Model A-Strict conditions, in full), §12–§13 (Pre-Authorization and Post-Computation Re-Verification Requirements), §15 (fail-closed table), §16 (the precise fallback trigger and its five bright-line rules).
- `docs/governance/PROGRAMME_3_UNIT_9_KNOWLEDGE_RETRIEVAL_CONTRACT_DESIGN.md` (Adopted) — §9's five-outcome Error Model, in particular that Empty result is a genuine success and Implementation failure must propagate honestly, never absorbed.
- `docs/governance/PROGRAMME_3_UNIT_9_PERMISSION_ENFORCEMENT_MECHANISM_CLARIFICATION.md` (Adopted) — the frozen, non-reorderable evaluation order and the two-tier act-level/item-level gate discipline.

Current production implementation inspected as authoritative, per this task's own instruction, in preference to README or prior summaries wherever they might differ: `src/runtime/DefaultKnowledgeRetrieval.kt` (576 lines); `src/interfaces/KnowledgeStore.kt` (1,610 lines — the `KnowledgeRetrievalQuery`, `KnowledgeResultEntry`, `StalenessDisclosure`, `KnowledgeRetrievalResult`, `KnowledgeRetrievalDisposition`, `KnowledgeRetrieval`, `ReasoningKnowledgeSource`, and `SafeKnowledgeResultEntry` declarations); `src/interfaces/PermissionEngine.kt`; `src/runtime/DefaultPermissionEngine.kt`; `src/composition/ParkerRuntime.kt` (Knowledge Retrieval composition, lines 239–956 and 1067–1099); `src/runtime/DefaultReasoningKnowledgeSource.kt` (213 lines, inspected for boundary purposes only — see Section 4's genuine finding, below); `tests/runtime/DefaultKnowledgeRetrievalTest.kt` (1,380 lines); `tests/contracts/KnowledgeRetrievalContractsTest.kt` (304 lines); `tests/composition/ParkerRuntimeKnowledgeRetrievalCompositionTest.kt` (427 lines). The experimental QMD proof was inspected only to the extent required by Section 13, below — its location (`C:\Projects\Parker\qmd-parker-experiment`, a sibling directory of this repository, not a subdirectory of it) and its implementation language (`raw-vector-probe.mjs` — Node.js/JavaScript, not Kotlin/JVM) were confirmed; its internal algorithm was not re-examined, since the adopted Proposal already settled feasibility and this task forbids reopening that determination.

---

## 3. Objective

Introduce exactly one capability into the production `KnowledgeRetrieval` boundary: **Bounded Relevance Computation**, precisely as the three adopted documents in Section 2 define it — a request-scoped, subordinate, replaceable ranking/subsetting operation over an already-eligible, already-permission-approved, closed candidate set, running only as a fallback after structural retrieval has completed successfully and found exactly zero relevant candidates. This plan authorises no broader capability. Section 16 restates, without elaboration, the explicit non-goal list this task's own instruction fixed; nothing in Sections 4–15 should be read as broadening it.

---

## 4. Current Implementation Map

### 4.1 The governed production path

```
KnowledgeRetrievalQuery
  → DefaultKnowledgeRetrieval.retrieve(requestingPrincipalId, query)
      → permissionEngine.evaluate(act-level ExecutionRequest)         [Clarification §6.2 item 1, §8 steps 2-3]
          → not authorised  → KnowledgeRetrievalDisposition.NotAuthorised
          → authorised      → persistence.findAll()                   [Clarification §8 step 4]
                                 .filter { matches(item, query.relevance) && isRetrievable(item, query) }
                                                                        [structural match + lifecycle shaping, one combined step]
                             → for each survivor: permissionEngine.evaluate(item-level ExecutionRequest)
                                                                        [Clarification §8 step 6, item-level gate]
                             → approved.take(query.maximumResults)     [Clarification §8 step 7]
                             → map { KnowledgeResultEntry(item, disclosureFor(item)) }
                                                                        [staleness disclosure, Clarification §8 step 8]
                             → KnowledgeRetrievalDisposition.Retrieved(KnowledgeRetrievalResult(entries))
                                                                        [Clarification §8 step 9]
```

Concrete classes/files participating: `KnowledgeRetrievalQuery`, `KnowledgeResultEntry`, `KnowledgeRetrievalResult`, `KnowledgeRetrievalDisposition`, `KnowledgeRetrieval` (all `src/interfaces/KnowledgeStore.kt`); `DefaultKnowledgeRetrieval` (`src/runtime/DefaultKnowledgeRetrieval.kt`) — the sole implementation; `KnowledgeItemPersistence` (read-only `findAll()` source); `PermissionEngine`/`DefaultPermissionEngine` (`src/interfaces/PermissionEngine.kt`, `src/runtime/DefaultPermissionEngine.kt`); composition in `ParkerRuntime.kt` line 956 (`knowledgeRetrieval = DefaultKnowledgeRetrieval(knowledgeItemPersistence, permissionEngine)`) and the Resource/ActionVocabulary registration at lines 1067–1099.

Today, structural matching is a single combined `.filter { matches(...) && isRetrievable(...) }` step — there is no existing concept of "structural match found zero candidates" as a distinguishable condition, because nothing downstream currently branches on it. `matches()` performs a case-insensitive substring match of `query.relevance` against the most recent history event's `basis` text — the only free-text field a `KnowledgeItem` carries. `SafeKnowledgeResultEntry` and `ReasoningContext` rendering are Reasoning Context's own, separate, unaffected responsibilities (Section 10, below).

### 4.2 Genuine finding: the governed insertion point is not the component the original defect was demonstrated against

`docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_BOUNDED_SEMANTIC_RELEVANCE_SCOPE_LOCK_AMENDMENT.md` §2 and the adopted Proposal §2 both state the problem this whole governance package exists to remedy in terms of `DefaultReasoningKnowledgeSource.recall()`'s substring matching, demonstrated in `ParkerRuntimeReasoningContextIntegrationTest.kt` (commit `aadd596`). Fresh inspection of the current repository establishes two facts together that this plan is obligated to disclose, per this task's own instruction, rather than silently work around:

1. **`DefaultReasoningKnowledgeSource`/`ReasoningKnowledgeSource` is governed by a wholly separate, differently-adopted document set** — `docs/governance/KNOWLEDGE_DISCOVERABILITY_REASONING_CONTEXT_CONTRACT_DESIGN.md` and `docs/governance/KNOWLEDGE_DISCOVERABILITY_REASONING_CONTEXT_SCOPE_LOCK.md` — neither cited, amended, nor in any way authorised for modification by any of the three Unit 9.7 documents in Section 2. Unit 9.7's own governance cites and elaborates only `PROGRAMME_3_UNIT_9_KNOWLEDGE_RETRIEVAL_CONTRACT_DESIGN.md` and `PROGRAMME_3_UNIT_9_PERMISSION_ENFORCEMENT_MECHANISM_CLARIFICATION.md` — the `KnowledgeRetrieval`/`DefaultKnowledgeRetrieval` boundary.
2. **`DefaultKnowledgeRetrieval` — the component Unit 9.7 does govern — is not currently consumed anywhere in the live conversation/reasoning path.** `knowledgeRetrieval` is constructed at `ParkerRuntime.kt` line 956, held as a `private lateinit var`, and never called anywhere in `src/` outside its own construction — confirmed by a repository-wide search for `.retrieve(` calls against a `KnowledgeRetrieval`-typed receiver. `DefaultReasoningContextAssembler` — the actual consumer of Reasoning Context — is wired to `reasoningKnowledgeSource` (a `DefaultReasoningKnowledgeSource` instance), a structurally separate object.

### 4.3 Investigation required by Independent Implementation Plan Review, and case classification

**A. Why two retrieval paths exist.** Fresh inspection of `docs/governance/KNOWLEDGE_DISCOVERABILITY_REASONING_CONTEXT_CONTRACT_DESIGN.md` §3 ("Interface and Adapter Shape," Gap #3, RESOLVED) establishes this is a **deliberate, adopted architectural decision, not a transitional or accidental duplication**. That document explicitly considered and rejected "widening `KnowledgeRetrieval` itself to carry [dereferenced Memory Core] content," on the ground that doing so "reopens the frozen Unit 9 Contract Design's own explicit, disclosed `KnowledgeItem`... [never-copies-Memory-Core-content] decision... for *every* current and future consumer of `KnowledgeRetrieval`," and adopted instead "a new, narrower, additive contract" — `ReasoningKnowledgeSource` — "reusing the existing, frozen `KnowledgeRetrievalQuery` request shape." The same document states, of `DefaultKnowledgeRetrieval`, `KnowledgeRetrieval`, `KnowledgeRetrievalResult`, and `KnowledgeResultEntry` together: "**Untouched by this design.** Remains available for any future structural-matching-only consumer; carries no content, never will under this design." `DefaultReasoningKnowledgeSource` therefore duplicates `DefaultKnowledgeRetrieval`'s own structural-match/lifecycle/two-tier-permission-gate logic **by explicit, adopted, disclosed design choice** (each duplicated method's own KDoc says so: "duplicated deliberately rather than shared, since `DefaultKnowledgeRetrieval` is not modified by this design"), not because a cutover was left incomplete.

**B. What the intended eventual architecture already says.** `docs/governance/KNOWLEDGE_DISCOVERABILITY_REASONING_CONTEXT_BOUNDARY_REVIEW.md` §3 ("Formal Ownership") states that Reasoning Context's only lawful knowledge input flows through "Knowledge Memory's own retrieval surface (`DefaultKnowledgeRetrieval` or its successor)," and forbids "a path Reasoning Context or any other consumer constructs for itself." Read together with finding A, the already-adopted resolution of that same question, one document later in the same governance chain, is that `ReasoningKnowledgeSource` **is** that "successor" surface for content-bearing Reasoning Context consumption specifically — not a rogue, self-constructed path, but Knowledge Memory's own second, additive, content-carrying read surface, built to the same query shape, the same permission discipline, and the same lifecycle rules as `KnowledgeRetrieval`, and explicitly adopted as such (Contract Design Gap #3, Gap #8). No adopted document anywhere states an intention to *later* cut Reasoning Context over from `ReasoningKnowledgeSource` to `KnowledgeRetrieval` itself — the "successor" language in the Boundary Review is satisfied by `ReasoningKnowledgeSource`'s own adoption, not left pointing at a future event. This determination is made from the adopted documents' own text (Contract Design Gap #3's resolution table row, and the Boundary Review's own "or its successor" phrase), not inferred from class names.

**C. Case classification: Case 3.** `DefaultReasoningKnowledgeSource` must itself receive an equivalent Bounded Relevance Computation capability; Reasoning Context is not, and architecturally cannot correctly be, cut over to consume `KnowledgeRetrieval` directly, because `KnowledgeRetrieval`'s own frozen, adopted contract structurally cannot carry the resolved textual content Reasoning Context requires without reopening Contract Design Gap #3's own settled resolution for every other current and future `KnowledgeRetrieval` consumer — exactly the kind of reopening this task's own scope discipline forbids absent a genuine, otherwise-unresolvable contradiction, and no such contradiction exists here: Gap #3's resolution is internally consistent and remains sound. This is not Case 1 (no adopted document authorises extending semantic relevance to `ReasoningKnowledgeSource` today — Unit 9.7's own three documents cite only the Unit 9/Knowledge Memory chain), not Case 2 in the pure sense (the *target* for the integration is settled by existing governance, per A and B above — what is missing is authorisation for a specific new capability on that target, not clarity about which target is correct), and not Case 4 (no third architecture is separately governed).

**Exact governance boundary preventing the integration.** `docs/governance/KNOWLEDGE_DISCOVERABILITY_REASONING_CONTEXT_CONTRACT_DESIGN.md` and `docs/governance/KNOWLEDGE_DISCOVERABILITY_REASONING_CONTEXT_SCOPE_LOCK.md` govern `ReasoningKnowledgeSource`/`DefaultReasoningKnowledgeSource` exclusively; neither document authorises, mentions, or contemplates Bounded Relevance Computation, Model A-Strict, or any semantic/ranked relevance capability of any kind — both currently require structural substring matching only, mirroring `KnowledgeRetrieval`'s own pre-Unit-9.7 boundary. CDR-008's own Decision A resolves the *Memory Core* boundary question only; it says nothing about the Knowledge-Memory-to-Reasoning-Context boundary Gap #3 separately settled, and does not, by itself, extend to authorise semantic relevance inside `ReasoningKnowledgeSource`.

**Smallest required governance change (identified, not drafted).** A sibling governance package, structurally mirroring Unit 9.7's own three-document shape exactly, but targeting the Reasoning-Context-facing surface: (i) a narrow amendment to `docs/governance/KNOWLEDGE_DISCOVERABILITY_REASONING_CONTEXT_SCOPE_LOCK.md` authorising exactly the same single capability Unit 9.7 §1 authorises, scoped to `ReasoningKnowledgeSource`; (ii) a corresponding amendment to `docs/governance/KNOWLEDGE_DISCOVERABILITY_REASONING_CONTEXT_CONTRACT_DESIGN.md`, mirroring Contract Design V2 Amendment 9; (iii) a Contract-and-Permission successor document for `DefaultReasoningKnowledgeSource`, mirroring `PROGRAMME_3_UNIT_9_7_BOUNDED_SEMANTIC_RELEVANCE_CONTRACT_AND_PERMISSION_SUCCESSOR.md` exactly, restating the same Model A-Strict fourteen conditions against `ReasoningKnowledgeSource`'s own permission and lifecycle discipline. This plan does not draft any of the three, per this task's explicit instruction — it identifies only their minimum required scope, so a future, separately authorised task can draft them without re-deriving this analysis.

**No duplicated semantic implementation.** Per this task's own instruction not to duplicate semantic retrieval implementations merely for convenience: the *mechanism* itself (Unit 9.7.1's contract types and Unit 9.7.3's concrete adapter, Section 8) is designed to be a single, shared, reusable collaborator — nothing about its interface ties it to `DefaultKnowledgeRetrieval` specifically. Only the *orchestration* pattern (fallback trigger, closed candidate set, integrity validation, Pre-disclosure re-verification) would need a second, analogous realisation inside `DefaultReasoningKnowledgeSource.recall()` once the governance in the paragraph above exists — exactly mirroring how this codebase already, deliberately duplicates `isRetrievable`, `disclosureFor`, and the two-tier permission-gate pattern between the two classes today, per finding A, rather than sharing a base class. This is precedented practice in this exact codebase, not a new anti-pattern this plan introduces.

**Consequence, stated plainly and without euphemism.** Implementing Unit 9.7 exactly as governed will produce a correctly-built, correctly-tested Bounded Relevance Computation capability inside `DefaultKnowledgeRetrieval` — but it will not, by itself, change what Parker's own reasoning ever sees or answers with, because the component consuming Reasoning Context's own knowledge feed is `DefaultReasoningKnowledgeSource`, a different, separately-governed, and — per B, above — deliberately, permanently separate class, not a temporary placeholder awaiting cutover. Section 8 represents the path to closing this gap as an explicit, named, blocked dependency, gated on the governance act identified above; Section 16 and Section 17 make clear that Unit 9.7's own completion does not, and must not be represented as, resolving Parker's live semantic-recall problem.

---

## 5. Frozen Boundaries

Every unit in Section 8 is bound by all twelve of this task's own frozen architectural requirements, restated here once rather than per unit, each cross-referenced to where the current implementation map (Section 4) or gap analysis (Section 6) already addresses it:

1. **Memory Core unchanged.** Already satisfied structurally — `DefaultKnowledgeRetrieval` holds no `MemoryRetrieval`/`MemoryCore` reference of any kind (confirmed by import inspection); no unit below introduces one.
2. **Structural retrieval remains primary.** Not yet implemented — see Section 6, gap analysis item 2, and Section 7.1.
3. **Closed candidate set.** Not yet implemented — Section 7.2.
4. **Opaque request-scoped identifiers.** Not yet implemented — Section 7.3.
5. **Minimum content.** Partially already satisfied — `matches()` already isolates the single free-text field (`basis`) as the only content a query is ever evaluated against; Section 7.3 extends this to the mechanism boundary.
6. **No authority transfer.** Not yet implemented — Section 7.4.
7. **Permission remains Parker's, Pre-computation → mechanism → Pre-disclosure.** Pre-computation is already satisfied by the existing item-level gate; Pre-disclosure re-verification for the fallback path specifically is new — Section 7.5, Section 10.
8. **Fail-closed behaviour.** Mostly already satisfied by this codebase's own existing "faults propagate as thrown exceptions, never absorbed" discipline (Unit 9 Contract Design §9) — Section 11.
9. **Architectural inability.** Not yet implemented — Section 7.4, Section 14.
10. **Request-scoped/disposable semantic state.** Achieved by construction once Section 7 is implemented as designed (no class-level or persisted state) — Section 7.3, Section 14.
11. **Local/operator-controlled.** Governs Section 13's dependency decision.
12. **Implementation-neutral governance.** Governs Section 13 in full.

---

## 6. Gap Analysis

| Adopted requirement | Classification | Basis |
|---|---|---|
| Memory Core unchanged (Frozen #1) | Already satisfied | No `MemoryRetrieval`/`MemoryCore` dependency exists or is added anywhere in this plan. |
| Zero-result-only fallback trigger (Frozen #2) | Requires extension | Structural match and lifecycle filtering are currently one combined `.filter{}` step with no branch on "structural match found nothing." |
| Closed candidate set (Frozen #3) | Requires new contract/type + extension | No concept of an eligible-but-unmatched pool exists; the fallback path needs a separately-computed, lifecycle-and-permission-filtered set independent of the structural-match outcome. |
| Opaque request-scoped identifiers (Frozen #4) | Requires new contract/type | No such type exists anywhere in this codebase; `KnowledgeId` is permanent and canonical, and must never cross the mechanism boundary. |
| Minimum content (Frozen #5) | Requires verification only | The existing `basis`-only substring-match design already isolates the minimum content boundary; a new test is needed to prove nothing else (identity, provenance, lifecycle, permission state) is ever passed to the mechanism. |
| No authority transfer (Frozen #6) | Requires new implementation component | The mechanism interface itself must be shaped so it can express only an ordering/subset of supplied tokens — no such interface exists yet. |
| Permission integration, Pre-computation/Pre-disclosure (Frozen #7) | Requires extension | Pre-computation (item-level gate) already exists; a fresh Pre-disclosure re-verification specific to the fallback path does not. |
| Fail-closed behaviour (Frozen #8) | Mostly already satisfied, requires new component for integrity faults only | This codebase's existing "no `try`/`catch`, faults propagate unchanged" discipline already gives mechanism failure a distinguishable, honest outcome for free; unknown/stale/duplicate/cross-request token rejection is a genuinely new validation this codebase has not needed before. |
| Architectural inability (Frozen #9) | Requires new implementation component + new verification | The mechanism interface's own signature must make write/enumeration access impossible by omission, mirroring `DefaultKnowledgeRetrieval`'s own existing "no dependency capable of reading Memory Core" precedent; a new structural (reflection) test is required, mirroring the existing convention already used for that guarantee. |
| Disposable semantic state (Frozen #10) | Requires new implementation component, verified by design discipline | Satisfied automatically if the token map is method-scoped, never a class-level field; a structural test should confirm no such field is ever added. |
| Local/operator-controlled (Frozen #11) | Requires new wiring, pending Section 13 | No mechanism exists yet to wire. |
| Implementation-neutral / QMD dependency question (Frozen #12) | Requires new wiring, pending Section 13's spike | See Section 13. |
| Mechanism identity/version/configuration as governed, frozen state (adopted Proposal §10.1) | Requires new implementation component + verification | No such concept exists yet; needed for the determinism guarantee in Section 12. |

No change is invented anywhere the current implementation already satisfies a requirement — in particular, the item-level permission gate, the act-level gate, the ordering-by-construction guarantee, and the staleness-disclosure mechanism are all reused unchanged.

---

## 7. Proposed Implementation Architecture

Names below are conceptual, per this task's own instruction — no concrete Kotlin declaration is frozen here; the smallest production architecture is described only at the level Sections 6 and 8 need to plan units and estimate impact.

### 7.1 Fallback trigger

`DefaultKnowledgeRetrieval.retrieve` separates its current single combined filter into two steps: a lifecycle-only eligibility filter (`isRetrievable`, unchanged), and a structural-match filter (`matches`, unchanged) applied only to the eligible set. If the structurally-matched set is non-empty, today's exact, unmodified behaviour continues — one or more structural matches prevent fallback, exactly as Frozen Boundary #2 and the adopted Proposal §16's own bright-line rules require, regardless of how many of those matches ultimately survive permission filtering (permission denial is never itself a fallback trigger). Only when the structurally-matched set is empty does a new, additive branch run.

### 7.2 Closed candidate set for the fallback path

On the empty-structural-match branch, the full lifecycle-eligible set (not the empty structural-match set) is item-level permission-gated, using the same existing gate, evaluated against the same fixed resource/action pair Unit 9.5 already established — no new permission pathway, no new outcome type (Frozen Boundary #3, #7). The result is the Pre-computation-admitted, closed candidate set the relevance mechanism may see. If this set is itself empty, no candidate exists for this principal at all; the query returns `Retrieved` with an empty result — a genuine, honest Empty result (Unit 9 Contract Design §9) — and the mechanism is never invoked, since there is nothing to rank.

### 7.3 Opaque tokens and minimum content

A new, request-scoped identifier type — valid only for the lifetime of one `retrieve` call, never derived from or convertible to `KnowledgeId` — is minted fresh for each member of the closed candidate set, held only in a local, method-scoped mapping (never a class field, never persisted) from token to the item's own minimum normalised content (the same `basis` text `matches()` already isolates — no widening of what content is ever exposed). This mapping, and the tokens themselves, cease to exist when `retrieve` returns; nothing survives as a secondary retrieval authority (Frozen Boundary #10).

### 7.4 The mechanism boundary — authority transfer and architectural inability, by omission

The relevance-mechanism interface is shaped to receive only the authorised query text and the token-to-content mapping from Section 7.3, and to return only an ordering or subset of the exact tokens it was given — never new tokens, never content, never a permission, lifecycle, or evidential-state assertion (Frozen Boundary #6). Critically, this interface is never given a `PermissionEngine` reference, a `KnowledgeItemPersistence` reference, or any handle capable of reaching canonical storage — mirroring exactly the precedent `DefaultKnowledgeRetrieval` itself already establishes for its own "structurally incapable of reading Memory Core" guarantee (Section 4.1: no `MemoryRetrieval`/`MemoryCore` field exists to call). Architectural inability is therefore achieved by the same, already-precedented technique this codebase already relies on elsewhere — omission of a capable dependency — not by a prompt, comment, or policy statement (Frozen Boundary #9).

### 7.5 Canonical re-resolution and Pre-disclosure re-verification

Every token the mechanism returns is resolved back to its own canonical `KnowledgeItem` by direct lookup in the closed candidate set built in Section 7.2 — never trusted, never substituted. A token absent from that set, or repeated, is an integrity fault (Section 11). Immediately before constructing a result entry for a resolved item, Parker performs a fresh permission and lifecycle/status re-verification — the Pre-disclosure step Frozen Boundary #7 and adopted Proposal §13 require, distinct from and never replacing Section 7.2's own Pre-computation gate. An item that fails this fresh check is excluded, never disclosed, and never substituted for.

### 7.6 Fallback coordinator, or inline extension

Whether the logic in Sections 7.1–7.5 is expressed as new private methods inline within `DefaultKnowledgeRetrieval` (mirroring how `matches`, `isRetrievable`, and `disclosureFor` already live as private methods on that same class) or factored into a narrowly-scoped sibling collaborator constructed alongside it is an implementation-tier decision, not fixed here — Section 8's own unit boundaries do not depend on which is chosen. Preferring extension of `DefaultKnowledgeRetrieval`'s own existing, already-reviewed class over new parallel infrastructure is the default expectation, consistent with this task's own "prefer extension of existing Parker patterns" instruction, unless the drafting unit finds the combined class becomes genuinely difficult to review as one file.

---

## 8. Unit Decomposition

### Unit 9.7.1 — Relevance Contract Types

**Objective.** Declare the opaque request-scoped token type, the minimal candidate/result shapes Section 7.3–7.4 describe, and the relevance-mechanism interface itself, at the properties level only — no concrete backing implementation.

**Expected repository impact.** A new file under `src/interfaces/`, alongside `KnowledgeStore.kt`, following that file's own established one-contract-per-concern pattern; additive only.

**Tests required.** A structural test proving the mechanism interface's own declared signature cannot express a write, a permission decision, or a lifecycle/evidential-state assertion (reflection over declared functions, mirroring this codebase's own existing structural-guarantee test convention). A test proving the token type carries no field derivable into a canonical `KnowledgeId`.

**Governance properties satisfied.** Frozen Boundaries #3, #4, #6, #9 (interface shape only).

**Completion criteria.** Compiles; the two tests above pass; no other file is touched.

### Unit 9.7.2 — Fallback Trigger and Closed Candidate Set

**Objective.** Implement Section 7.1–7.2 inside `DefaultKnowledgeRetrieval`: split the combined filter, compute the structurally-matched set, and — only when it is empty — compute and item-level-gate the full lifecycle-eligible set into the Pre-computation-admitted closed candidate set.

**Dependencies.** Unit 9.7.1 (token type, for the mapping this unit begins to build).

**Expected repository impact.** `src/runtime/DefaultKnowledgeRetrieval.kt`, modified additively; no other production file.

**Tests required.** A test proving one or more structural matches prevent fallback regardless of permission outcome on those matches (Frozen Boundary #2). A test proving structural failure (a thrown exception during matching/persistence read) never reaches the fallback branch — the existing "faults propagate unchanged" behaviour continues untouched. A test proving the closed candidate set is exactly the lifecycle-eligible, permission-approved set, never a superset including structurally-excluded-by-permission items from the original pass.

**Governance properties satisfied.** Frozen Boundaries #2, #3, #7 (Pre-computation half).

**Completion criteria.** Every existing `DefaultKnowledgeRetrievalTest.kt` case continues to pass unmodified in substance; the new tests above pass; no existing structural-match behaviour changes for any query whose structural match is non-empty.

### Unit 9.7.3 — Local Relevance Mechanism Adapter

**Objective.** Implement one concrete backing mechanism satisfying Unit 9.7.1's interface, per whichever answer Section 13's spike produces.

**Dependencies.** Unit 9.7.1; Section 13's own spike outcome (a governance-adjacent engineering decision, not a further governance act — see Section 13's own scope note).

**Expected repository impact.** A new file under `src/runtime/`, following the same one-class-per-responsibility convention as `DefaultKnowledgeCandidateEvaluator.kt` and `DefaultKnowledgeSubmission.kt`; additive only.

**Tests required.** A determinism test: identical query text, identical candidate content, and an identical frozen mechanism identity/version/configuration yield an identical ordering across repeated invocations. A test proving the mechanism's own declared dependencies contain nothing capable of reaching canonical persistence, `PermissionEngine`, or any network boundary the operator does not control (Frozen Boundary #9, #11).

**Governance properties satisfied.** Frozen Boundaries #9, #10, #11, #12; adopted Proposal §10.1 (mechanism identity/version/configuration as governed state).

**Completion criteria.** Compiles; both tests above pass; this unit introduces no dependency Unit 9.7.1's interface does not already permit.

### Unit 9.7.4 — Integrity Validation, Canonical Re-resolution, and Pre-disclosure Re-verification

**Objective.** Implement Section 7.5: token validation (unknown, stale, cross-request, duplicate — each an integrity fault, rejected outright), direct-lookup resolution against the closed candidate set, and the fresh Pre-disclosure permission/lifecycle re-check immediately before result-entry construction.

**Dependencies.** Unit 9.7.2 (the closed candidate set this unit resolves against); Unit 9.7.1 (token type).

**Expected repository impact.** `src/runtime/DefaultKnowledgeRetrieval.kt`, the same file Unit 9.7.2 extended — a continuation of that unit's own additive branch, not a separate file.

**Tests required.** One test per fail-closed table row in `PROGRAMME_3_UNIT_9_7_BOUNDED_SEMANTIC_RELEVANCE_CONTRACT_AND_PERMISSION_SUCCESSOR.md` §6 that this unit is responsible for (malformed/unknown/duplicate/stale-token rejection; permission or lifecycle change caught by re-verification; deleted canonical item treated as unresolvable). A test proving Pre-disclosure re-verification is genuinely a second, fresh evaluation — not a reuse of the Pre-computation decision — by varying policy state between the two.

**Governance properties satisfied.** Frozen Boundaries #6, #7 (Pre-disclosure half), #8 (the integrity-fault portion).

**Completion criteria.** Every fail-closed table row this unit owns is independently tested and passes; Unit 9.7.2's own tests remain passing unmodified.

### Unit 9.7.5 — Runtime Composition Wiring

**Objective, explicitly separate and final, mirroring Unit 9.6's own precedent.** Construct the chosen mechanism (Unit 9.7.3) and inject it into `DefaultKnowledgeRetrieval`'s construction at `ParkerRuntime.kt` line 956, additive wiring only. This unit performs no wiring of `DefaultKnowledgeRetrieval` into Reasoning Context or any new consumer — Section 4.2's genuine finding is explicitly not remedied by this unit, and no such remedy is in scope for Unit 9.7.

**Dependencies.** All of Units 9.7.1–9.7.4, complete.

**Expected repository impact.** `src/composition/ParkerRuntime.kt`, additive constructor-argument and (if Unit 9.7.3 requires its own Resource/ActionVocabulary entries beyond the ones Unit 9.5 already registered) additive registration lines only, mirroring the existing Knowledge Retrieval registration block at lines 1067–1099.

**Tests required.** A composition test confirming the composed `DefaultKnowledgeRetrieval` instance genuinely holds the new mechanism, mirroring `ParkerRuntimeKnowledgeRetrievalCompositionTest.kt`'s own existing precedent. Confirmation that every other existing composition line is unaltered.

**Governance properties satisfied.** No new property — this unit only makes Units 9.7.1–9.7.4 reachable in the composed graph.

**Completion criteria.** The full composed runtime builds and starts; `ParkerRuntimeKnowledgeRetrievalCompositionTest.kt` and `ParkerRuntimeReasoningKnowledgeSourceCompositionTest.kt` both continue to pass unmodified — the latter proving, by its continued unmodified passage, that `DefaultReasoningKnowledgeSource` remains genuinely untouched.

### Unit 9.7.6 — Full Verification Matrix and Regression Confirmation

**Objective.** Execute Section 14's complete verification matrix end to end, and Section 15's full regression boundary, as a single closing unit — mirroring Unit 9.6's own "does not re-test any guarantee already proven" discipline: this unit adds no new production code, only the small number of matrix rows not already covered by an earlier unit's own tests (in particular the full-repository build/test run itself).

**Dependencies.** All of Units 9.7.1–9.7.5, complete.

**Expected repository impact.** None beyond whatever remaining test files Section 14 identifies as not yet covered by an earlier unit.

**Completion criteria.** Every row of Section 14 is independently verified; every test named in Section 15 passes unmodified in substance.

### Live-Reasoning Integration — explicit gate, not a Unit 9.7 unit

Per Section 4.3's Case 3 determination, closing the gap between Unit 9.7's own completion and Parker's actual live semantic-recall behaviour requires a capability inside `DefaultReasoningKnowledgeSource`, under governance Unit 9.7's own three adopted documents do not provide. This is recorded here as a named, blocked dependency — not a seventh Unit 9.7 unit, and not implementable under this plan's own authority:

- **Gate.** A separate, dedicated governance package (Section 4.3's "smallest required governance change") amending or successor-extending `KNOWLEDGE_DISCOVERABILITY_REASONING_CONTEXT_SCOPE_LOCK.md` and `KNOWLEDGE_DISCOVERABILITY_REASONING_CONTEXT_CONTRACT_DESIGN.md`, itself independently reviewed and adopted through Programme 4's own established review discipline.
- **Only once that gate clears** may a future, separately planned and reviewed implementation unit apply Section 7's own orchestration pattern (fallback trigger, closed candidate set, integrity validation, Pre-disclosure re-verification) a second time inside `DefaultReasoningKnowledgeSource.recall()`, reusing Unit 9.7.1's mechanism interface and Unit 9.7.3's concrete adapter unchanged — never a second, independently-designed mechanism.
- **Unit 9.7 itself does not depend on this gate to be complete** by its own Section 17 definition — Units 9.7.1–9.7.6 are fully specifiable, implementable, and verifiable against `DefaultKnowledgeRetrieval` alone. The gate blocks only the *programme-level* practical objective (live semantic recall), never Unit 9.7's own narrower, governed completion.

---

## 9. Affected Files/Contracts

| File | Nature of change |
|---|---|
| A new file under `src/interfaces/` (Unit 9.7.1) | New — contract types and mechanism interface |
| `src/runtime/DefaultKnowledgeRetrieval.kt` (Units 9.7.2, 9.7.4) | Modified additively — fallback trigger, closed candidate set, integrity validation, re-resolution, Pre-disclosure re-verification |
| A new file under `src/runtime/` (Unit 9.7.3) | New — the concrete local mechanism adapter, per Section 13 |
| `src/composition/ParkerRuntime.kt` (Unit 9.7.5) | Modified additively — construction and wiring only |
| `tests/runtime/DefaultKnowledgeRetrievalTest.kt` | Extended additively — new cases per Units 9.7.2 and 9.7.4; no existing case is modified |
| `tests/composition/ParkerRuntimeKnowledgeRetrievalCompositionTest.kt` | Extended additively — Unit 9.7.5 |
| A new test file for Unit 9.7.1's contract types | New |
| A new test file for Unit 9.7.3's mechanism adapter | New |

No other file is expected to change. In particular, `src/runtime/DefaultReasoningKnowledgeSource.kt`, `tests/runtime/DefaultReasoningKnowledgeSourceTest.kt`, `tests/composition/ParkerRuntimeReasoningKnowledgeSourceCompositionTest.kt`, and every Memory Core file are expected to show zero diff at the end of implementation — Section 15 requires this be confirmed, not merely assumed.

---

## 10. Permission Integration

Unchanged: `PermissionEngine` remains the sole permission authority; the frozen nine-step evaluation order and the two-tier act-level/item-level gate are neither reordered nor shortened. Unit 9.7 adds exactly one new evaluation moment — the Pre-disclosure re-verification (Section 7.5, Unit 9.7.4) — which reuses the existing item-level gate mechanism and the existing fixed resource/action pair; it is a second invocation of an existing gate, at a new point in the control flow, never a new gate, a new outcome type, or a new permission pathway. No unit anywhere in Section 8 constructs an `ExecutionRequest` naming a new resource or action beyond what Unit 9.5 already registered, unless Unit 9.7.3's own concrete mechanism turns out to need its own disclosed-but-unregistered pair for some infrastructure-level concern (unlikely, since the mechanism itself never touches `PermissionEngine` directly, per Section 7.4) — if so, that registration follows the existing Knowledge Retrieval registration precedent exactly (Section 4.1) and is confirmed, not assumed, no later than Unit 9.7.5's own review.

---

## 11. Failure Handling

Implemented exactly as adopted governance fixes, using this codebase's own existing conventions wherever they already suffice, per the corrected Unit 9.7 fail-closed table:

- **Mechanism unavailable, timeout, or relevance computation failure.** Not caught anywhere in the new code — propagates as a thrown exception, exactly like every other genuine implementation fault this codebase already lets propagate unchanged (`DefaultKnowledgeSubmission`, `InMemoryMemoryCore`, and `DefaultKnowledgeRetrieval` itself all already establish this same "no `try`/`catch`" discipline). This is already a distinguishable, honestly-propagated Implementation-failure-class outcome under Unit 9 Contract Design §9 — no new exception type or sealed variant is required.
- **The already-computed, zero-result structural outcome is never re-executed.** Because the fallback trigger (Section 7.1) is evaluated exactly once, before the mechanism is ever invoked, there is no code path in which mechanism failure could cause a second structural-matching pass — the zero-result structural finding simply is the state the fallback branch was already in when the mechanism was called.
- **A successful semantic computation returning zero relevant candidates** is a genuine, distinguishable Empty result — the mechanism returns an empty token list, Section 7.5's resolution step produces zero entries, and `Retrieved(KnowledgeRetrievalResult(emptyList()))` is returned exactly as it already is today for any other zero-entry outcome. Nothing distinguishes this from the "no eligible candidates at all" case in Section 7.2 at the type level, mirroring Contract Design §4's own existing "empty result is a valid, successful outcome" discipline — both are `Retrieved` with an empty list, which is correct, since a caller does not need to distinguish "nothing existed" from "the mechanism found nothing relevant."
- **Malformed, unknown, stale, cross-request, or duplicate tokens** fail closed via the integrity validation Unit 9.7.4 implements (Section 7.5) — rejected outright, never silently substituted, never partially honoured.
- **Partial or stale output never masquerades as complete.** No unit in Section 8 introduces a partial-success representation; every outcome is either a complete, honestly-bounded `Retrieved` result or a propagated fault.

---

## 12. Determinism/Versioning

The existing determinism guarantee (Unit 9 Contract Design §8, already implemented by construction — ordering preserved by `List` operations alone, no sorting or randomisation anywhere) is extended, per adopted Proposal §10.1, to treat the relevance mechanism's own identity, version, and configuration as frozen, disclosed, retrieval-relevant state: for an unchanged persisted state, an unchanged permission policy, an unchanged mechanism identity/version/configuration, and a fixed instant (staleness disclosure's own existing time-relative exception, unchanged), the same query yields an identical result, in identical order, every time. A mechanism upgrade or reconfiguration is itself a disclosed, governed change — this plan does not authorise silent mechanism substitution, and Unit 9.7.5's own composition wiring makes the mechanism's identity a visible, reviewable construction-time argument, never a runtime-selectable or auto-updating one.

---

## 13. QMD/Dependency Decision

**Answer: undecided until a bounded implementation spike.**

This is an implementation-dependency choice only — it does not reopen the adopted semantic-relevance remedy, whose evidentiary basis (that a genuinely semantic mechanism can recall a structurally-dissimilar paraphrase) remains settled regardless of which concrete mechanism Unit 9.7 ultimately selects.

**Basis for this answer, against every factor this task requires it be justified from:**

- **Current repository architecture.** Direct inspection confirms QMD's own experimental proof (`raw-vector-probe.mjs`) is a Node.js/JavaScript program living in a sibling directory (`C:\Projects\Parker\qmd-parker-experiment`) entirely outside this Kotlin/JVM repository — not an internal library, not a JVM-native dependency already present in this build. Adopting it as the production mechanism would require either a cross-process/IPC boundary from the JVM runtime to a separate Node.js process, or a from-scratch reimplementation of an equivalent embedding approach natively in Kotlin/JVM. Neither is "wiring in an existing dependency."
- **Adopted governance.** Contract Design V2 Amendment 9 §11 and the Scope Lock Amendment §10 both state, without qualification, that QMD is experimental evidence only and never a constitutional or implementation dependency of this amendment — this plan is required to, and does, treat the choice as fully open.
- **Demonstrated experiment evidence.** The experiment demonstrated *feasibility* of semantic paraphrase recall using an embedding-based approach; it did not demonstrate, and this plan does not assume, that QMD specifically (as opposed to some other mechanism satisfying the same contract) is necessary to achieve it.
- **Dependency/runtime impact.** A cross-process adapter to the existing Node.js tool would add a new runtime dependency (a Node.js process, its own package manager, its own failure modes) to a codebase that is otherwise pure Kotlin/JVM — a materially different operational footprint than an in-process JVM mechanism.
- **Determinism.** An in-process JVM mechanism is straightforwardly testable for the frozen-configuration determinism Section 12 requires; a subprocess boundary adds serialization, process-lifecycle, and version-drift risk that a spike should measure before this plan assumes it away in either direction.
- **Local/operator-controlled requirement.** Both directions can satisfy Frozen Boundary #11 — a local subprocess is still "entirely under Parker's own operator/owner control" — but a subprocess is a materially different local-control shape than genuinely in-process code, worth confirming deliberately rather than assuming.
- **Testability and replaceability.** Unit 9.7.1's own interface (Section 7.4) is deliberately implementation-neutral for exactly this reason — whichever mechanism Unit 9.7.3 ultimately implements is fully swappable behind that interface without touching Units 9.7.2, 9.7.4, or 9.7.5's own governed contract, permission integration, or fail-closed behaviour.

**Recommended spike scope, bounded and narrow.** A single, time-boxed engineering spike — not a further governance act — evaluating exactly two candidates against Section 7.4's interface, Frozen Boundaries #9–#12, and the mandatory semantic-fitness gate below: (a) QMD as a local subprocess adapter; (b) a simpler, dependency-light, in-process JVM lexical or embedding mechanism. The spike selects Unit 9.7.3's own concrete implementation; it does not, and may not, alter Units 9.7.1, 9.7.2, 9.7.4, or 9.7.5, or any adopted governance document.

### 13.1 Semantic fitness is a mandatory acceptance gate, not a desirable characteristic

Architectural and operational fitness (Section 13's factors above) are necessary but not sufficient. A candidate that satisfies the mechanism interface, determinism, local-control, and dependency criteria while still failing to reproduce the semantic capability that justified Unit 9.7 in the first place must not be selected. This subsection makes semantic fitness an explicit, mandatory pass/fail gate.

**Accepted evidence surface, reused rather than re-run.** The adopted Proposal §2 and §3 already fix the controlled evidence this spike must benchmark against — this plan does not repeat or reopen the constitutional feasibility experiment, it reuses its accepted fixtures and outcomes:

- **Positive case.** The proposition "the owner's synthetic emergency vet is Harbour Animal Clinic," genuinely promoted via Parker's own Remember/promotion path, and the paraphrase query "Which animal clinic did I tell you to use in an emergency?" — containing no literal substring of the stored proposition — demonstrated in `ParkerRuntimeReasoningContextIntegrationTest.kt` (commit `aadd596`), test "a genuinely related paraphrase does not recall a promoted proposition under the current literal substring retrieval," and its six-memory extension.
- **Negative/control cases.** The six genuine distractor memories the same fixture already promotes alongside the target proposition (adopted Proposal §3: "ranks the intended canonical memory first among six genuine distractors"). If none of the six is already lexically or topically similar to the target proposition while remaining propositionally distinct, Unit 9.7.3's spike adds a small number of such fixtures in the same format — an extension of the existing accepted fixture set, not a new experiment.
- **Quantitative benchmark.** The independently recomputed cosine-similarity evidence already accepted into governance (adopted Proposal §3: 0.586 for the intended match vs. 0.414 for the strongest distractor, from raw captured vectors) is the reusable, deterministic expected-outcome fixture for repetition-stability and determinism testing — captured data, not live model inference, exactly as the adopted Proposal's own evidence was produced.
- **Honest limitation, disclosed rather than glossed over.** The `aadd596` fixture was captured against `DefaultReasoningKnowledgeSource`'s own dereferenced-content matching, not against `DefaultKnowledgeRetrieval`'s `basis`-field matching (Section 4.2). Reuse here is at the level of the query text, the target proposition text, the distractor proposition texts, and the expected relative ranking/similarity — content-and-outcome level, implementation-path-independent — not a literal re-execution of the original test's own object graph, since that graph belongs to a different, separately-governed class (Section 4.3). The spike must state this adaptation explicitly wherever it reports results, never presenting an adapted re-run as an unmodified repetition of the original.

**Mandatory candidate acceptance requirements.** A candidate may be selected for Unit 9.7.3 only if it demonstrates all ten of the following, evaluated against the evidence surface above:

1. **Positive semantic recall** — retrieves the accepted structurally-dissimilar, propositionally-equivalent positive case.
2. **Negative discrimination** — does not materially increase false-positive retrieval against the accepted negative/control cases.
3. **Ranking fidelity** — places genuinely relevant candidates appropriately within the bounded result ordering.
4. **Proposition fidelity** — does not treat merely lexically or topically related but propositionally different candidates as equivalent.
5. **Repetition stability** — stable outcomes across repeated runs under frozen state/configuration.
6. **Determinism** — satisfies the adopted deterministic-behaviour requirement (Section 12) under fixed mechanism identity/version/configuration.
7. **Contract compliance** — operates entirely through Unit 9.7.1's bounded mechanism interface.
8. **Architectural inability** — cannot enumerate canonical storage, write Memory Core, inspect permissions, or escape the supplied closed candidate set (Frozen Boundary #9).
9. **Local/operator-controlled execution** — satisfies Model A-Strict condition 13 (Frozen Boundary #11).
10. **Replaceability** — remains replaceable behind Unit 9.7.1's implementation-neutral interface.

**Comparison requirement.** Both candidates are evaluated against the identical evidence surface above and compared on: true positives; false positives; false negatives; ranking; proposition fidelity; repetition stability; determinism; latency; resource use; dependency/runtime burden; operational failure modes; testability; replaceability. Selection is never made solely because a candidate is simpler, and never made solely because QMD produced the original successful experiment — the selected mechanism is the smallest candidate that actually satisfies all ten mandatory requirements above. If only QMD satisfies them, QMD is recommended behind Unit 9.7.1's replaceable adapter boundary (Section 7.4) — this does not constitutionalise QMD, since the boundary makes it swappable without touching any governed contract. If the simpler JVM mechanism satisfies them equally or sufficiently, it is preferred on complexity grounds only after fitness is already established, never instead of establishing it.

**Stop condition.** If neither candidate satisfies all ten mandatory requirements, Unit 9.7.3 is blocked. The semantic acceptance criteria are never weakened to permit implementation to proceed; a blocked Unit 9.7.3 blocks only Units 9.7.5 and 9.7.6 (Section 18) — it does not license substituting a lesser mechanism, and it does not reopen QMD feasibility or the adopted remedy itself, both of which remain settled regardless of this spike's outcome.

---

## 14. Verification Matrix

| Property | Required test/inspection | Owning unit |
|---|---|---|
| Structural result prevents semantic fallback | One or more structural matches → no mechanism invocation, regardless of permission outcome | 9.7.2 |
| Exact-zero structural result permits fallback | Zero structural matches → mechanism invoked over the closed candidate set | 9.7.2 |
| Structural failure does not permit fallback | A thrown exception during structural matching/persistence read never reaches the fallback branch | 9.7.2 |
| Only authorised candidates reach the mechanism | Closed candidate set is exactly the lifecycle-eligible, item-level-permission-approved set | 9.7.2 |
| Permanent `KnowledgeId` never crosses the boundary | Reflection/structural test on the mechanism interface and the token type | 9.7.1 |
| Opaque token cannot resolve outside the current request | Token minted in one `retrieve` call is rejected if presented (in a test double) to a resolution step from a different call | 9.7.4 |
| Stale token rejected | Fail-closed table row test | 9.7.4 |
| Unknown token rejected | Fail-closed table row test | 9.7.4 |
| Duplicate result behaviour | De-duplicated before bounding; never consumes more than one `maximumResults` slot | 9.7.4 |
| Too-many-result behaviour | Mechanism's own limit is a hint only; Parker's own bounding remains authoritative | 9.7.4 |
| Malformed result failure | Fail loudly, reject the whole result | 9.7.4 |
| Relevance timeout/unavailability | Propagates as a thrown, distinguishable Implementation-failure-class fault | 9.7.3, 9.7.4 |
| Semantic failure vs. successful semantic-empty distinction | Two independent tests: one asserting a thrown fault on mechanism failure, one asserting `Retrieved(empty)` on a successful zero-relevant computation | 9.7.4 |
| Canonical re-resolution | Every returned token resolves, by direct lookup, to the exact `KnowledgeItem` originally supplied under it | 9.7.4 |
| Permission re-check before disclosure | Pre-disclosure re-verification genuinely re-evaluates, proven by varying policy state between admission and disclosure | 9.7.4 |
| Lifecycle change during relevance computation | Caught by Pre-disclosure re-verification; excluded, never disclosed | 9.7.4 |
| Deleted canonical item during computation | Treated as unresolvable, excluded, not substituted | 9.7.4 |
| No persistent relevance state | Structural test confirming no class-level field holds token or content state between calls | 9.7.2/9.7.3 |
| No candidate enumeration by mechanism | Structural test on the mechanism interface's own declared dependencies | 9.7.1 |
| No canonical writes by mechanism | Structural test on the mechanism interface's own declared dependencies | 9.7.1 |
| Deterministic repeated retrieval under frozen state/configuration | Determinism test, mechanism identity/version/configuration held fixed | 9.7.3 |
| Mechanism identity/version/configuration as governed state | Test proving a changed configuration is a disclosed, distinguishable change, never silent | 9.7.3 |
| Unchanged existing structural retrieval behaviour | Full, unmodified passage of `DefaultKnowledgeRetrievalTest.kt`'s existing cases | 9.7.6 |
| Unchanged Memory Core behaviour | Zero diff in every Memory Core file; full Memory Core test suite passes unmodified | 9.7.6 |
| Unchanged `PermissionEngine` behaviour | Zero diff in `PermissionEngine`/`DefaultPermissionEngine`; existing permission tests pass unmodified | 9.7.6 |
| Unchanged `SafeKnowledgeResultEntry` construction authority | Zero diff in `DefaultReasoningKnowledgeSource.kt`; its own test suite passes unmodified | 9.7.6 |
| Unchanged `ReasoningContext` rendering authority | Zero diff in `DefaultReasoningContextAssembler.kt`; its own test suite passes unmodified | 9.7.6 |
| Reproduction of accepted semantic positive cases | Selected mechanism retrieves the `aadd596`-derived positive case (Section 13.1), adapted to Unit 9.7's own content boundary | 9.7.3 spike |
| Accepted negative/control discrimination | Selected mechanism does not materially increase false positives against the six accepted distractors (plus any added lexically-similar/propositionally-distinct fixtures) | 9.7.3 spike |
| Proposition fidelity | Lexically/topically related but propositionally different candidates are not treated as equivalent | 9.7.3 spike |
| Ranking fidelity | Genuinely relevant candidates are appropriately placed within the bounded ordering | 9.7.3 spike |
| Repetition stability | Stable outcomes across repeated runs under frozen state/configuration | 9.7.3 spike |
| Mechanism-selection evidence | The Section 13.1 comparison (both candidates, all thirteen comparison metrics) is recorded and available for Defect Confirmation Review | 9.7.3 spike |
| Selected mechanism satisfies the same bounded contract used during the spike | The concrete Unit 9.7.3 implementation is re-verified against Unit 9.7.1's interface post-selection, not assumed unchanged from the spike prototype | 9.7.3 |
| Live-reasoning integration/cutover path proof, or explicit governance gate | Either a passing integration test through `DefaultReasoningKnowledgeSource` (only possible after Section 8's Live-Reasoning Integration gate clears) or, until then, explicit confirmation that Section 8's gate remains recorded, undischarged, and un-bypassed | 9.7.6 (gate check only, until the separate governance act exists) |

---

## 15. Regression Requirements

Must continue to pass, entirely unmodified in substance: `tests/runtime/DefaultKnowledgeRetrievalTest.kt` (1,380 lines); `tests/contracts/KnowledgeRetrievalContractsTest.kt` (304 lines); `tests/composition/ParkerRuntimeKnowledgeRetrievalCompositionTest.kt` (427 lines); `tests/runtime/DefaultReasoningKnowledgeSourceTest.kt`; `tests/composition/ParkerRuntimeReasoningKnowledgeSourceCompositionTest.kt`; every Memory Core test; every `PermissionEngine`/`DefaultPermissionEngine` test. No existing test may be weakened, deleted, or have its assertions loosened to accommodate the new capability — any test that would need to change is itself a signal that a unit above has exceeded its own scope. Full-repository build and test verification is required after Unit 9.7.6, not merely the tests each unit's own drafting names, mirroring `PROGRAMME_3_UNIT_9_KNOWLEDGE_RETRIEVAL_IMPLEMENTATION_PLAN.md` §7's own "a fresh clone of the repository builds and tests successfully" completion-gate discipline.

---

## 16. Out-of-Scope Items

This plan does not include, and no unit in Section 8 may be read as authorising: MUEP; Docling; document ingestion; evidence analysis; external AI reasoning; OpenAI/Anthropic integration; Output Composer; docx4j; Michael case work; Uber case work; UI work; server deployment; production rollout; Memory Core redesign; Evidence Custodian changes; persistent vector storage; remote semantic relevance; Models B or C.

Also explicitly out of scope, per Section 4.3's Case 3 determination: any modification to `DefaultReasoningKnowledgeSource`, `ReasoningKnowledgeSource`, `docs/governance/KNOWLEDGE_DISCOVERABILITY_REASONING_CONTEXT_CONTRACT_DESIGN.md`, or `docs/governance/KNOWLEDGE_DISCOVERABILITY_REASONING_CONTEXT_SCOPE_LOCK.md`; drafting the sibling governance package Section 4.3 identifies (its minimum scope is described there, not drafted); any wiring of `DefaultKnowledgeRetrieval` into Reasoning Context directly — Section 4.3 establishes this would be architecturally incorrect, not merely unauthorised, since `KnowledgeRetrieval`'s own frozen contract cannot carry the content Reasoning Context requires. Closing Section 8's Live-Reasoning Integration gate is a separate, future, Programme-4-owned governance and implementation act, not this plan's own concern.

---

## 17. Completion Criteria

**A unit is complete when:** it compiles; every existing test affected by it still passes unmodified in substance; every new test named for it in Section 8 passes; no later unit in Section 18's own ordering was required to reach this state.

**Unit 9.7 as a whole is complete when:** all six units in Section 8 are complete by the above definition; every row of Section 14's verification matrix owned by those six units is independently verified; every test named in Section 15 passes unmodified; a fresh clone of the repository builds and tests successfully with no partially-implemented unit's state present; Bounded Relevance Computation is reachable through `DefaultKnowledgeRetrieval` exactly as the three adopted documents authorise, and through no other path.

**Unit 9.7's own completion is not, and must never be represented as, resolution of Parker's practical semantic-recall objective.** Per Section 4.2 and Section 4.3, Reasoning Context consumes `DefaultReasoningKnowledgeSource`, not `DefaultKnowledgeRetrieval`, and remains exactly as disconnected from Bounded Relevance Computation after Unit 9.7 as before it. The programme-level objective — a live Parker conversation genuinely recalling a structurally-dissimilar paraphrase — is not achieved by Unit 9.7 alone and must not be reported, summarised, or adopted as achieved until: (a) Section 8's Live-Reasoning Integration gate's own separate governance act is independently reviewed and adopted; and (b) the resulting implementation unit, applying Unit 9.7's own mechanism (Units 9.7.1 and 9.7.3, reused unchanged) inside `DefaultReasoningKnowledgeSource`, is itself complete and verified. Any future report, review, or adoption action that describes Unit 9.7's completion as "fixing semantic recall" without both (a) and (b) having independently occurred is itself a defect, not a permissible summarisation.

---

## 18. Implementation Order

```
9.7.1 (Relevance Contract Types)
  │
  ├──> 9.7.2 (Fallback Trigger + Closed Candidate Set) ──> 9.7.4 (Integrity Validation, Re-resolution, Pre-disclosure)
  │
  └──> 9.7.3 (Local Mechanism Adapter) *requires prior spike, Section 13*

9.7.2 + 9.7.3 + 9.7.4, all complete ──> 9.7.5 (Runtime Composition Wiring) ──> 9.7.6 (Full Verification + Regression)
                                                                                        │
                                                                                        ╵  Unit 9.7 complete (Section 17,
                                                                                        ╵  narrow definition) — does NOT
                                                                                        ╵  reach live reasoning
                                                                                        ▼
                                              [BLOCKED — separate governance act, Section 8's
                                               Live-Reasoning Integration gate, Section 4.3]
                                                                │
                                                                ▼
                              Future implementation unit (out of this plan's scope): applies
                              Units 9.7.1 + 9.7.3's mechanism inside DefaultReasoningKnowledgeSource
                                                                │
                                                                ▼
                                    Programme-level objective achieved (live semantic recall)
```

9.7.2 and 9.7.3 may proceed in parallel once 9.7.1 is complete, since neither depends on the other's own output — 9.7.3 depends only on 9.7.1's interface. 9.7.4 depends on 9.7.2 (the candidate set it resolves against) and 9.7.1. 9.7.3 may not begin in earnest until Section 13's own spike selects a mechanism (and Section 13.1's semantic-fitness gate passes — if it does not, Section 13.1's stop condition blocks 9.7.3, and transitively 9.7.5 and 9.7.6); the spike itself may proceed as soon as 9.7.1's interface exists, since the spike's own purpose is to evaluate candidates against that interface. 9.7.5 is strictly last but one, depending on 9.7.2, 9.7.3, and 9.7.4 all being complete. 9.7.6 is strictly final **for Unit 9.7 itself**. The Live-Reasoning Integration gate and everything below it in the diagram above are explicitly outside Unit 9.7's own critical path and outside this plan's own authority to schedule, implement, or commit to a timeline for — they are recorded here only so no later reader mistakes 9.7.6's own completion for the programme's own completion.

---

## 19. Review/Adoption Requirements

This plan underwent Independent Implementation Plan Review (verdict: REVISE BEFORE ACCEPTANCE), a bounded correction pass addressing the two findings identified, and Defect Confirmation Review (verdict: ACCEPT), and is now Adopted. No unit may begin implementation on the strength of this document alone. Section 13's spike is an engineering step, not a governance act, and does not require Independent Constitutional Review in the sense the three Section 2 documents themselves underwent — but its outcome (Unit 9.7.3's own concrete mechanism choice) is subject to ordinary code review, to Section 13.1's own mandatory semantic-fitness gate, and to Unit 9.7.3's own completion criteria (Section 8) before Unit 9.7.5 may compose it. Section 8's Live-Reasoning Integration gate requires its own, separate, independently-reviewed governance package (Section 4.3) before any implementation unit may apply Unit 9.7's mechanism inside `DefaultReasoningKnowledgeSource` — that review is not performed, and that governance is not drafted, by this document. This plan itself modifies no adopted governance document, implements no Kotlin, and authorises no work beyond translating the three adopted Unit 9.7 documents into the ordered engineering units Section 8 fixes.

---

## 20. Independent Self-Review

- **Does this plan implement anything?** No — no Kotlin, class, method, or field is declared anywhere; every "expected repository impact" entry names a file location or a modification category, never a declaration.
- **Does it modify any existing governance document?** No — every document in Section 2 is cited, none altered.
- **Does it reopen CDR-008, Model A-Strict, the adopted remedy, the QMD experiment, or the amendment-map verdict?** No — Section 13's dependency question is treated strictly as an engineering choice within already-settled governance, never a re-litigation of feasibility or authorisation.
- **Does it preserve Memory Core separation?** Yes — Section 5 item 1 and Section 6's own gap-analysis row both confirm no `MemoryRetrieval`/`MemoryCore` dependency is introduced anywhere.
- **Does it preserve the frozen permission evaluation order?** Yes — Section 10 states explicitly that no step is reordered, shortened, or duplicated; Pre-disclosure re-verification is a new *invocation*, not a new *step kind*.
- **Does it introduce a new lifecycle state, ranking mechanism outside the governed boundary, or Permission Engine concept?** No — checked against Section 5's own twelve frozen boundaries; none appears.
- **Does it correctly isolate runtime composition as a separate, penultimate unit, and verification as the final one?** Yes — Section 8's own 9.7.5 and 9.7.6 mirror Unit 9.6's own precedent exactly.
- **Does it disclose the genuine architectural gap between the demonstrated problem and the governed remedy's production reach?** Yes — Section 4.2, Section 4.3, stated plainly, not minimised, and carried through into Section 8's explicit gate, Section 16, and Section 17's own strengthened completion-criteria disclaimer.
- **Does it claim to fix Reasoning Context's own recall behaviour?** No — Section 17 explicitly disclaims this and states what two further, separate events (a) and (b) would be required before that claim could ever be made.
- **Does it reach its Case 3 classification from the adopted documents' own text, or by inference from class names?** From the text — Section 4.3 cites Contract Design Gap #3's own resolution table row and the Boundary Review's own "`DefaultKnowledgeRetrieval` or its successor" language, not a guess based on naming.
- **Does it solve reachability by bypassing the twelve frozen protections?** No — Section 4.3's own "No duplicated semantic implementation" paragraph and Section 8's gate both require any future Reasoning-Context-facing unit to reuse Unit 9.7.1's and Unit 9.7.3's mechanism unchanged and to satisfy the same governance discipline, never a shortcut.
- **Does it draft the governance amendment Section 4.3 identifies as required?** No — its minimum scope is described, not drafted, per this task's own instruction.
- **Does it freeze QMD as a dependency merely because the experiment used it?** No — Section 13 treats the question as genuinely open; Section 13.1 adds a mandatory, ten-requirement semantic-fitness gate on top of the eight architectural factors, so a candidate cannot be selected on operational grounds alone, and QMD is recommended only if it, specifically, is the smallest candidate that passes every requirement.
- **Does the semantic-fitness gate reopen or repeat the constitutional feasibility experiment?** No — Section 13.1 reuses the adopted Proposal's own accepted fixtures (the `aadd596` positive case, its six distractors, and the captured 0.586/0.414 cosine-similarity evidence), adapted honestly and disclosedly to Unit 9.7's own content boundary, never re-run as a fresh experiment.
- **Does it weaken the semantic acceptance criteria to force a selection?** No — Section 13.1's stop condition blocks Unit 9.7.3 outright if neither candidate passes, rather than lowering the bar.

No genuine defect found in this plan itself requiring correction before it is offered for review, beyond the two findings this revision itself exists to address (Section 4.2/4.3's architectural gap, and Section 13.1's semantic-fitness gate) — both are now fully disclosed and bounded, not defects remaining in this plan's own reasoning.
