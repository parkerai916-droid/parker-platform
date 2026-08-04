# Reasoning Provider Contract Design — Amendment Proposal (Planning Review)

## Status

**Planning and constitutional design exercise only. Not an amendment.** No governance document is amended by this document. No Kotlin is implemented, proposed as a diff, or changed. Neither `src/` nor `tests/` is touched. Nothing is staged, committed, or pushed.

This document identifies exactly what a future Reasoning Provider Contract Design amendment would need to change in order to implement the constitutional interpretation recorded in `docs/reviews/EVIDENCE_INTELLIGENCE_UNIT_3_CONSTITUTIONAL_DECISION_MEMORANDUM.md` ("the Memorandum"). It is a planning input to that future amendment, not the amendment itself.

**Repository state confirmed before this analysis:** `docs/architecture/REASONING_PROVIDER_CONTRACT_DESIGN.md` and `docs/architecture/REASONING_PROVIDER_ARCHITECTURE.md` are unmodified in the working tree. No document named "Reasoning Provider Scope Lock" exists anywhere in this repository (confirmed by repository-wide search) — this is addressed directly in Section 6, not assumed.

---

## 1. Amendment Objective

Implement the constitutional interpretation recorded in the Memorandum: that `ReasoningContext` (`docs/architecture/reasoning-context.md`), not `Turn`, is the governing constitutional authority for what a reasoning provider reasons over, and that `Turn` is Conversation Engine's own first concrete instantiation of that authority rather than its universal shape. The Contract Design amendment generalises the subject `ReasoningProviderRequest` carries so that a second, already-authorised caller — Evidence Intelligence — can supply a reasoning subject of its own, without disturbing Conversation Engine's exclusive ownership of `Turn`/`Conversation`, without introducing a dedicated Evidence-Intelligence-specific reasoning contract, and without altering any operative decision CDR-007 records.

---

## 2. Exact Contract Design Paragraphs Requiring Amendment

Each entry below quotes the paragraph's current text from `docs/architecture/REASONING_PROVIDER_CONTRACT_DESIGN.md`, states why the constitutional interpretation requires it to change, and states the smallest amendment that would satisfy that requirement.

### 2.1 — Section 2, the `turn: Turn` bullet

**Current text:**
> "`turn: Turn` — reused unchanged (`CONVERSATION_ENGINE_CONTRACT_DESIGN.md` Section 2). Carries `turnId`, `conversationId`, and `message: InboundOwnerMessage`... as one already-defined bundle. This document does not duplicate any of `Turn`'s own fields as separate top-level fields on the request..."

**Why amendment is required:** This is the specific clause that ties `ReasoningProviderRequest` exclusively to `Turn`. It is the load-bearing text the entire Unit 3 investigation identified as incompatible with a non-conversational caller.

**Smallest possible amendment:** Replace the single bullet with a `subject: ReasoningSubject` bullet, where `ReasoningSubject` is a new, closed, sealed selector carrying exactly one case wrapping `Turn` unchanged (satisfying every existing Conversation Engine caller without modification to `Turn` itself) and one case Evidence Intelligence owns. No other field, and no other bullet in Section 2, changes.

### 2.2 — Section 4, the `Turn` row of "Existing Parker Types Reused Unchanged"

**Current text:**
> "`Turn` | `ReasoningProviderRequest.turn` (directly) | Same; embedding it whole avoids duplicating its own fields (Section 2)."

**Why amendment is required:** `Turn` is no longer reused "directly" as the request's own field; it is reused as the payload of one case of the sealed subject.

**Smallest possible amendment:** Change the "Reused via" column entry to name the specific sealed-subject case wrapping `Turn`, leaving the "Not redefined because" column's substance unchanged — `Turn` itself is still embedded whole, still not duplicated.

### 2.3 — Section 6, Lifecycle Boundaries, Step 1

**Current text:**
> "1. **Invocation.** A caller constructs a `ReasoningProviderRequest` (a `Turn` plus an already-assembled `ReasoningContext`) and calls `ReasoningProvider.reason`."

**Why amendment is required:** The parenthetical names `Turn` specifically as one of the request's two components; this is no longer accurate once the field is a sealed subject.

**Smallest possible amendment:** Replace "(a `Turn` plus an already-assembled `ReasoningContext`)" with "(a `ReasoningSubject` plus an already-assembled `ReasoningContext`)". No other step in Section 6 changes.

### 2.4 — Contract Minimalism Review — Summary table, and Section 10's fuller Minimalism Review table

**Current text (Summary table):**
> "`ReasoningProviderRequest` | **Include.** The minimal input a Reasoning Provider needs — see Section 2."

**Current text (Section 10 table):**
> "`ReasoningProviderRequest` (`turn` + `reasoningContext`) | **Yes** | Minimum input Architecture Section 2 names: a Turn's content and an assembled Reasoning Context. Two fields, both either reused or newly minimal."

**Why amendment is required:** Both tables presently describe `ReasoningProviderRequest` as a fixed `turn` + `reasoningContext` pair, and neither accounts for a new sealed type entering the document's own "new contracts" tally. Both need a new row for `ReasoningSubject`, and the existing `ReasoningProviderRequest` row's parenthetical needs updating from "(`turn` + `reasoningContext`)" to "(`subject` + `reasoningContext`)".

**Smallest possible amendment:** Add one row to each table for `ReasoningSubject` — "Include, as a closed selector spanning Conversation Engine's `Turn` and Evidence Intelligence's own analytical subject; mirrors `CandidateMemoryCoreRecord`'s existing precedent (`src/interfaces/EvidenceIntelligence.kt`) for a closed, behaviour-free selector wrapping existing, unmodified types under one owner." Update the existing `ReasoningProviderRequest` row's parenthetical only.

### 2.5 — Self-Traceability Review (Section 11), the `ReasoningProviderRequest` row

**Current text:**
> "`ReasoningProviderRequest` | Section 2 (Responsibilities: 'given a Turn's content... and an already-assembled Reasoning Context') | `Turn` (`CONVERSATION_ENGINE_CONTRACT_DESIGN.md` Section 2) | — | Stage 2A: 'minimum required set of public contracts'"

**Why amendment is required:** The traceability chain currently routes exclusively through Conversation Engine Architecture. A new row is needed tracing `ReasoningSubject` to its own authorising sources.

**Smallest possible amendment:** Add one new table row tracing `ReasoningSubject` to: `reasoning-context.md` ("task," the constitutional-tier generality — Memorandum §3.A), CDR-007 (authorising Evidence Intelligence's orchestration of `ReasoningProvider` without naming `Turn` — Memorandum §3.B, §6), and the Memorandum itself (the settled interpretation). The existing `ReasoningProviderRequest` row's own citation to `Turn`/Conversation Engine Contract Design remains accurate for the case that wraps `Turn` and is not altered.

### 2.6 — Conclusion, the contract-count sentence

**Current text:**
> "...four new field-level types (`ReasoningProviderRequest`, `ReasoningProviderResponse` with its three variants, and the deliberately minimal `ReasoningContext`), five existing contracts reused unchanged (three of them transitively via `Turn`), zero modified..."

**Why amendment is required:** The count and the "zero modified" claim are both superseded — one further new type (`ReasoningSubject`) is introduced, and `ReasoningProviderRequest` itself is modified, not merely reused.

**Smallest possible amendment:** Update the count to five new field-level types and state plainly that `ReasoningProviderRequest` is the one amended (not reused-unmodified) contract, tied to Section 2.1 above.

### 2.7 — "Related" section

**Current text:** lists `REASONING_PROVIDER_ARCHITECTURE.md`, `19-conversation-engine.md`, `CONVERSATION_ENGINE_CONTRACT_DESIGN.md`, `COMMUNICATION_CONTRACT_DESIGN.md`, `parker-constitution.md`, `PARKER_ENGINEERING_STANDARD.md`, `reasoning-context.md`, `ARCHITECTURE_V2_FROZEN_BASELINE.md`, two ADRs, `CommunicationIntake.kt`, `PlanDecision.kt`, `IMPLEMENTATION_GAPS.md`.

**Why amendment is required:** `CDR-007`, `EVIDENCE_INTELLIGENCE_CONTRACT_DESIGN.md`, `EVIDENCE_INTELLIGENCE_SCOPE_LOCK.md`, and this Memorandum are now directly relevant authorising sources and are absent from the list.

**Smallest possible amendment:** Append these four to the existing list. No existing entry is removed.

---

## 3. Exact Paragraphs Remaining Unchanged

Confirmed unaffected, quoted where useful to demonstrate why:

- **Status** section — the document's procedural header. Unaffected; still describes a Stage 2A Contract Design.
- **"Why this unit exists"** — historical narrative describing the document's own origin. Remains accurate as history; the amendment does not retroactively alter why the original document was written.
- **Constitutional Boundaries** — "A Reasoning Provider proposes. It authorises nothing, executes nothing, and accesses no Tool"; "No confidence score substitutes for Permission Engine evaluation"; "Replacement must never alter Parker's trust guarantees"; "Cognition proposes. Trust authorises. Runtime executes — with no shortcut for output that arrived via a Reasoning Provider." None of these depends on the request's subject shape; all remain true unchanged for a `Turn`-wrapping case, an Evidence-Intelligence-owned case, or any future case.
- **Section 1 (Public `ReasoningProvider` Interface)** — "One operation: given a `ReasoningProviderRequest`, return a `ReasoningProviderResponse`." The interface signature does not change; only the internal shape of one type flowing across it changes.
- **Section 2, the `reasoningContext: ReasoningContext` bullet** — unaffected; `ReasoningContext` remains the same opaque, deliberately minimal list of prose entries, with the same unassigned-assembly status.
- **Section 3 (Minimal Response Object — `ReasoningProviderResponse`)** in full — `Goal`, `Reply`, `NoAction`, and the "no fourth variant for failure" discipline are all independent of the request's subject shape.
- **Section 4**, every row except `Turn` — `TurnId`, `ConversationId`, `InboundOwnerMessage`, `PrincipalId`, `CorrelationId`, `PlanningRequest`, `OutboundParkerResponse` are all reused exactly as before, whether reached transitively via a `Turn`-wrapping subject case or not at all (for an Evidence-Intelligence-owned case).
- **Section 5**, in full, except the ownership addition Section 4 (below) identifies — "Nothing this document defines is owned by the Reasoning Provider... Neither type has an identifier of its own kind... The calling component owns the invocation itself" all remain true regardless of subject shape.
- **Section 6**, every step except Step 1's parenthetical — "Interpretation," "Return," and "No lifecycle beyond one invocation" are unaffected.
- **Section 7 (Runtime Boundaries)** in full — every listed non-dependency (`PlannerRuntime`, `AgentRuntime`, `TaskManagerRuntime`, `MemoryStore`, `WorldModel`, `ExecutionPipeline`, `PermissionEngine`, `ToolRegistry`, `ModuleRegistry`) remains a non-dependency for both subject cases; neither `Turn` nor an Evidence-Intelligence-owned case introduces a reference to any of them.
- **Section 8 (Contract Invariants)** in full — none of the eight invariants concerns the request's subject shape.
- **Section 9 (Deferred Items)** in full — prompt templates, token limits, streaming, provider selection/routing, retries, network protocols, model identifiers, provider implementations, the deferred `Failed` variant, and `ReasoningContext`'s own assembly mechanism are all independent of this amendment.

---

## 4. Ownership Analysis

**`ReasoningSubject` is owned by the Reasoning Provider Contract Design itself** — the same document that already owns `ReasoningProviderRequest`, `ReasoningProviderResponse`, and `ReasoningContext`. This is not a "shared constitutional abstraction" in the sense of belonging to no one, and not owned by Evidence Intelligence or by Conversation Engine individually.

**Justification from existing governance:** the direct, on-point precedent is `CandidateMemoryCoreRecord` (`src/interfaces/EvidenceIntelligence.kt`) — "a closed, two-case selector type... owned exclusively by Evidence Intelligence... does not modify `CandidateAssertion` or `CandidateRelationship` — both remain reused, unmodified; this type only references them." `ReasoningSubject` follows the identical shape one tier up: a closed selector, owned by the tier that already owns the interface it serves, wrapping — never modifying — types owned elsewhere (`Turn`, owned by Conversation Engine; whatever analytical-subject shape Evidence Intelligence contributes, owned by Evidence Intelligence). This satisfies the Evidence Intelligence Contract Design's own repeated principle, "every public object has exactly one constitutional owner," without disturbing `Turn`'s existing, exclusively-Conversation-Engine ownership — `CONVERSATION_CONTINUITY_CONTRACT_DESIGN.md` §12's statement that Conversation Engine "remains the only component that ever constructs a `Conversation` or `Turn`" is unaffected, since the sealed selector only ever wraps a `Turn` that Conversation Engine already constructed; it never constructs one itself.

---

## 5. Public Contract Analysis

Determined by direct application of repository precedent, not asserted:

- **Not solved by a new public type alone.** `ReasoningProviderRequest.turn: Turn` is a field on an existing public type; the field itself must change, which is a modification of an existing public type.
- **Not solved by modification alone.** Simply widening `turn`'s type or adding a second field reintroduces the nullable-field anti-pattern this same document's own Minimalism Review already rejected for the response side: "Why a sealed type, not three separate boolean/nullable fields... a sealed type makes that structurally impossible to violate, rather than merely conventionally expected of three independent nullable fields."
- **Requires a selector, structured as a sealed hierarchy.** The combination — modify `ReasoningProviderRequest` (replace `turn` with `subject`), introduce one new sealed type (`ReasoningSubject`) closed to a defined set of cases — is the minimum public-contract change consistent with this repository's own dominant idiom for "exactly one of several existing, unmodified types, closed and behaviour-free": `CandidateMemoryCoreRecord` (Evidence Intelligence), `ReasoningProviderResponse` itself (`Goal`/`Reply`/`NoAction`), and `MemoryCoreRecord` (`OfEntity`/`OfDocument`/`OfAssertion`/`OfRelationship`) all follow this exact shape.
- **No registry.** A `ReasoningProviderRegistry`-style discovery mechanism is not implicated by this change and remains excluded exactly as both the Architecture (§13) and Contract Design (§10) already exclude it — the sealed subject selects a *shape of input*, not a *choice of provider implementation*.

---

## 6. Dependency Analysis

| Subsystem | Gains a dependency? | Loses a dependency? | Basis |
|---|---|---|---|
| **Conversation Engine** | No. | No. | Continues to construct `Turn` exclusively and pass it, now wrapped in a `ReasoningSubject` case, into `ReasoningProviderRequest`. No new type or subsystem enters its dependency set. |
| **Evidence Intelligence** | No. | No. | Its existing, CDR-007-authorised dependency on `ReasoningProvider` is unchanged in identity — only the internal shape of one type flowing across that edge changes. It gains no dependency on `ConversationEngine`, confirming the central finding of the Remediation Analysis and Follow-up Review. |
| **Planning (Planner Runtime)** | No. | No. | Never a dependency of `ReasoningProvider`'s request/response shapes before this amendment; remains so. `Goal.text`'s eventual path into a `PlanningRequest` is unaffected. |
| **Memory Core** | No. | No. | Never a dependency of `ReasoningProvider`'s request/response shapes before this amendment; remains so. Evidence Intelligence's own, separately-authorised `MemoryRetrieval` dependency is untouched by this amendment. |

No subsystem gains or loses a dependency as a result of this amendment. Only the internal field shape of one already-shared type changes.

---

## 7. Compatibility

**Existing Conversation Engine behaviour remains valid in substance, subject to one mechanical call-site adaptation.** The invocation contract — construct a request, call `reason`, receive `Goal`/`Reply`/`NoAction` — is unchanged in every guarantee: statelessness, no retained reference, no authorisation implication, no dependency on any forbidden subsystem. The one concrete change is at the call site presently reading `ReasoningProviderRequest(turn = disposition.turn, reasoningContext = ...)` (`ConversationTurnReasoningCoordinator`), which would need to wrap `disposition.turn` in the new sealed subject's `Turn`-case constructor. This is a compiler-enforced, exhaustiveness-checked adaptation, not a behavioural change.

**Existing contracts guaranteed to remain unchanged by this amendment:**

- `ReasoningProvider` (the interface itself — one operation, same signature shape)
- `ReasoningProviderResponse` and all three variants (`Goal`, `Reply`, `NoAction`), including their validation rules
- `ReasoningContext` (opaque list of prose entries, unassigned assembly responsibility)
- `Turn` (`src/interfaces/ConversationEngine.kt`) — identical fields, identical construction exclusivity
- `Conversation`, `ConversationId`, `TurnId`, `ConversationDisposition`
- `InboundOwnerMessage`, `CorrelationId`, `PrincipalId`
- `PlanningRequest`, `OutboundParkerResponse`
- Every Constitutional Boundary, Contract Invariant, and Deferred Item this Contract Design already records (Sections 7, 8, 9)

---

## 8. Scope Lock Assessment

**No amendment is required, because no Reasoning Provider Scope Lock exists.** A repository-wide search confirms this: the only documents matching "Reasoning" and "Scope Lock" are `REASONING_TO_PLANNING_HANDOFF_SCOPE_LOCK.md` and `PRODUCTION_REASONING_CONTEXT_SCOPE_LOCK.md` — neither governs the `ReasoningProvider` interface itself. Unlike Evidence Intelligence's four-stage governance stack (CDR → Contract Design → Scope Lock → Implementation Plan), the Reasoning Provider tier's stack is Architecture → Contract Design → Implementation Plan, with no intervening Scope Lock stage. There is accordingly nothing at that tier to amend.

**A related, but distinct, document does require a small update:** `EVIDENCE_INTELLIGENCE_SCOPE_LOCK.md` §4 (Dependency Freeze) presently describes `ReasoningProvider` as reused "unmodified" in its dependency table. This is Evidence Intelligence's own Scope Lock, not a Reasoning Provider Scope Lock, and its required change is a narrative currency correction of the same kind identified for CDR-007's Repository Reuse Summary in the Memorandum — not a substantive reopening.

---

## 9. Implementation Plan Assessment

Units identified as requiring wording updates once an actual amendment is made. **None is edited here.**

- **`EVIDENCE_INTELLIGENCE_IMPLEMENTATION_PLAN.md`, Unit 3 ("Reasoning Provider Orchestration")** — its Responsibilities text ("Invoke an existing `ReasoningProvider` via its own, unmodified request/response contract") would need to record that the request is now constructed via the sealed subject's Evidence-Intelligence-owned case.
- **`EVIDENCE_INTELLIGENCE_IMPLEMENTATION_PLAN.md`, §5 ("Existing Components Reused")** — the row naming `ReasoningProviderRequest`, `ReasoningProviderResponse`, `ReasoningContext` as reused from the Reasoning Provider Contract Design would need a footnote identifying the new sealed subject.
- **`CONVERSATION_ENGINE_IMPLEMENTATION_PLAN.md`** — the unit implementing `ConversationTurnReasoningCoordinator` (`src/runtime/ConversationTurnReasoningCoordinator.kt`, cited in its own KDoc as a Sprint 7 Stage 3 Implementation Unit) constructs `ReasoningProviderRequest(turn = ..., reasoningContext = ...)` directly; its wording would need updating to reflect the new subject-wrapping call site.
- **`MODEL_REASONING_PROVIDER_IMPLEMENTATION_PLAN.md`** — this document's own three-collaborator design names `ReasoningPromptBuilder.buildPrompt(request.turn, request.reasoningContext)` explicitly; its wording would need updating wherever it names `request.turn` directly, since a concrete `ModelReasoningProvider` implementation must now handle whichever subject case it is prepared to interpret.

---

## 10. Recommended Amendment Sequence

Planning-level sequencing only; no step below is performed by this document.

1. **Format precedent.** This repository does not always edit an accepted Stage 2A document's own text in place for a substantive extension. `CONVERSATION_CONTINUITY_CONTRACT_DESIGN.md` — which additively extended `ConversationEngine` from one operation to two — is the closer precedent for this amendment's likely shape than the `MEMORY_CORE_CONTRACT_DESIGN_ERRATA_00N.md` series, since the errata documents are explicitly "documentation only... amends no architecture, no scope, no field, no event, and no behaviour," while this amendment is a genuine, substantive field-shape change. A future amendment would more likely take the shape of a new, separate, cross-referencing document (mirroring the Continuity Contract Design's own relationship to the Conversation Engine Contract Design) than an in-place rewrite of `REASONING_PROVIDER_CONTRACT_DESIGN.md`.
2. **Optional governance confirmation.** Per the Memorandum's own CDR Assessment, a disclosed, self-certifying confirmation (CDR-005 Model C pattern) may accompany the amendment; it is not a precondition for it.
3. **Contract Design amendment.** The paragraph-level changes in Section 2 of this document, applied via whichever document form step 1 selects.
4. **Evidence Intelligence Scope Lock currency note.** The narrative correction identified in Section 8.
5. **Implementation Plan wording updates.** The units identified in Section 9, updated for consistency once the Contract Design amendment exists.

---

## 11. Confirmation

No governance document was modified in the preparation of this planning review. `docs/architecture/REASONING_PROVIDER_CONTRACT_DESIGN.md`, `docs/architecture/REASONING_PROVIDER_ARCHITECTURE.md`, and every other document reviewed remain exactly as they were. No Kotlin was implemented. No production code or test was touched. Nothing was staged, committed, or pushed.
