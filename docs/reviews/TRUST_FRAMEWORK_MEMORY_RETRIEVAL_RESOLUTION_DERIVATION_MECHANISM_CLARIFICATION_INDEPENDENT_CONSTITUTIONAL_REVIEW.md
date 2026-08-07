**Status:** Genuine Independent Constitutional Review of `docs/governance/TRUST_FRAMEWORK_MEMORY_RETRIEVAL_RESOLUTION_DERIVATION_MECHANISM_CLARIFICATION.md`, performed as if by another reviewer, against the governing documents and the actual, current file contents re-read fresh — not against that document's own Section 10 self-check alone. This document does not amend the Clarification, any frozen or draft governance document, or any Kotlin/test file. Nothing is staged, committed, or pushed.

# Trust Framework — Memory Retrieval Resolution Derivation Mechanism Clarification — Independent Constitutional Review

## 1. Baseline Confirmation

`git status --short` confirms the Clarification and this review are the only new files at review time, alongside the already-known, deliberately-uncommitted Parker Conversational Memory Bridge work and its own prior-task governance documents. The Clarification's own Section 1 citation list was independently re-checked against the actual repository — every cited file exists at the path given.

---

## 2. Challenge — Is the Governance Tension (Section 4) Real, and Correctly Resolved?

Independently re-read `docs/architecture/action-mapping.md`'s own "Validation" section in full: "A vocabulary entry's `applicableResourceTypes` MUST be checked against the actual `ResourceType`(s) of `ExecutionRequest.targetResources` at validation time. A mapped action whose declared resource types don't match any target Resource is also a validation failure." Confirmed the Clarification's own quotation is exact. Independently confirmed the "more specific, already-accepted governance controls" resolution mirrors the Contract Design's own Section 6 precedent (Chapter 8 vs. Errata 004) exactly, not a newly-invented principle. **Confirmed sound.**

---

## 3. Challenge — Resource Existence vs. Resource Classification (Section 5) — Does the Separation Actually Hold?

Independently re-verified each of the four supporting points against primary source: `DefaultPermissionPolicy.evaluate`'s own existing fallback (`request.targetResources.firstOrNull() ?: ResourceId("no-target-resource")`), re-read directly, confirmed unchanged and honest. `PermissionFilteredMemoryRetrieval`'s own direct-lookup methods, re-read directly, confirmed existence is established by `delegate.getEntity(...)` etc. *before* `isApproved` is ever called — independent of classification. `ResourceRegistry.register`/`.update` calls, grepped across `src/`, confirmed never invoked by either Memory Core decorator. **Confirmed sound** — the separation the Clarification claims is real, not asserted.

---

## 4. Challenge — Is the "Closed Set" (Section 3's own table, Section 4) Actually Closed and Correctly Enumerated? (Substantive Finding)

Independently re-ran the grep the Clarification itself reports (`targetResources\s*=` across all of `src/`), broadening beyond the two literal patterns (`emptyList()`/`listOf(...)`) the Clarification's own Section 3 table cites. **This surfaces one additional case the Clarification does not disclose**: `src/runtime/InMemoryAgentRuntime.kt:508` constructs an `ExecutionRequest` with `targetResources = decision.targetResources` — a value forwarded from an `AgentStepDecision.Propose`, which is caller/Planner-supplied and could, in principle, be empty for some future Agent-proposed action.

Pressed on whether this undermines the Clarification's own mechanism: it does not, but the Clarification's own Section 3 table phrasing ("the only two production classes... constructing `targetResources = emptyList()`") is more absolute than the evidence supports, since it describes only *literal* empty-list construction, not every path that could *produce* an empty list at runtime. The reason this does not actually weaken the mechanism: the Clarification's own Section 6 condition 2 (the verb phrase must be one of the nine already-named, closed set) already, independently, guards against this — an Agent proposing a *different* string would never match; an Agent somehow proposing the literal string `"memory.retrieve"` would receive the same treatment any other caller proposing that same, already-governed verb phrase already receives today, by the pre-existing, system-wide design that verb phrases (not caller identity) are the trust boundary (`action-mapping.md`'s own "Plugin Supplied Actions" section already accepts this same shape of exposure for every other registered verb phrase, not something introduced here). **This is not a required correction to the mechanism itself, but the Clarification's own Section 3 table should be corrected to state the closed-set claim precisely** (literal construction, not every path that could theoretically produce an empty list), since a document holding itself to primary-source rigor throughout should not leave its own strongest empirical claim slightly overstated.

---

## 5. Challenge — Does the Selected Mechanism (Section 6) Actually Work Uniformly for All Nine Named Action Names? (Substantive Finding, Required Correction)

This is the most significant finding of this review. The Clarification's own Section 6 mechanism derives "the `(PermissionAction, ResourceType)` pair Errata 004 §7's own frozen table already, specifically assigns to that verb phrase" — implicitly treating each of the nine action names as mapping to exactly **one** pair. Independently re-read Errata 004 §7's own frozen table directly: `transitionStatus`, target ≠ `DELETED` → `WRITE` / **"`MEMORY`/`DOCUMENT` (per the transitioned record's own kind)"**; `transitionStatus`, target = `DELETED` → `DELETE` / **"`MEMORY`/`DOCUMENT` (per the transitioned record's own kind)"**. Independently re-read `PermissionGatedMemoryCore.kt` directly (`transitionStatus`, lines 159–171): the action-name selection (`DELETE_RECORD_ACTION_NAME` vs. `TRANSITION_STATUS_ACTION_NAME`) distinguishes **only** delete-vs-not; it does **not** distinguish `MEMORY`-kind from `DOCUMENT`-kind records, and `buildExecutionRequest` is called with `actionName` alone — no record-kind parameter reaches the `ExecutionRequest` at all for these two operations.

**Confirmed: `memory.transition_status` and `memory.delete_record` cannot be reduced to a single `(action, resourceType)` pair from the verb phrase alone** — Errata 004 §7's own frozen table makes their `resourceType` depend on the specific record's own kind, information the verb phrase does not carry and the Clarification's own Section 6 mechanism (as drafted) does not account for. The remaining seven action names (`memory.create_provenance`, `memory.create_entity`, `memory.create_assertion`, `memory.create_relationship`, `memory.register_document`, `memory.retrieve`, `memory.retrieve_document`) are each genuinely single-typed by their own name and are unaffected by this finding — in particular, **the two action names this task's own audit trace is actually scoped to (`memory.retrieve`, `memory.retrieve_document`) are both single-typed and correctly handled** by the mechanism as drafted.

This is a genuine gap, not a restatement: as drafted, applying Section 6's mechanism uniformly to `memory.transition_status`/`memory.delete_record` would require the derivation to produce a *set* of two candidate pairs (mirroring `ActionMapper`'s own already-supported "composite action" shape) and rely on `DefaultPermissionPolicy`'s own existing "most restrictive wins" tie-break (`evaluated.minByOrNull { restrictiveness(...) }`) to select an outcome — which is safe in the sense that it can never *over*-approve (a `DENIED` on either candidate pair wins), but could *under*-approve a transition that should be permitted for its own actual kind, because the *other* kind's own rule happens to be stricter. The Clarification does not disclose this at all, and `PermissionGatedMemoryCore` (the class this would affect) is, however, dormant and unused in the live composed runtime today (`ParkerRuntime.kt`, independently re-confirmed by grep: no construction of `PermissionGatedMemoryCore` appears in the composition root), and squarely outside this task's own named audit trace (`PermissionFilteredMemoryRetrieval → ExecutionRequest → DefaultPermissionPolicy.evaluate → ActionMapper → ActionMappingResult.Resolved`, the **read**-side path only).

**Required correction:** Section 6 must not present the mechanism as working uniformly across all nine action names. It should state plainly that it is fully, unambiguously sound for the seven single-typed action names — and, centrally, for the two this task's own scope actually concerns (`memory.retrieve`, `memory.retrieve_document`) — and that `memory.transition_status`/`memory.delete_record` require either the disclosed, weaker composite/tie-break treatment (with its own honest under-approval caveat) or a separate, later design pass, deferred because `PermissionGatedMemoryCore` is not live and not this task's own subject.

---

## 6. Challenge — Is the Shared-Decorator Finding (Section 7) Genuine, or an Overreach Beyond This Task's Own Scope?

Independently re-read `EvidenceIntelligenceInputResolver.kt` and `DefaultKnowledgeCandidateEvaluator.kt` in full: confirmed both call `MemoryRetrieval`'s direct-lookup methods, confirmed `DefaultKnowledgeCandidateEvaluator`'s own fixed `SYSTEM_PRINCIPAL_ID = PrincipalId("system.knowledge-memory")`, confirmed `EvidenceIntelligenceInputResolver` instead forwards `request.requestingPrincipalId` (the analysis request's own caller-supplied principal) — the two callers' own principal identities do genuinely differ in practice, exactly as the Clarification states. Confirmed, via `ParkerRuntime.kt`, both are wired to the same `permissionFilteredMemoryRetrieval` instance. Checked whether this finding oversteps the task's own "resolve only this question" instruction: the task's own "Contract Design consolidation" section explicitly requires "Confirm no other unresolved constitutional prerequisite remains" — this finding is exactly responsive to that instruction, not a digression from it, and the Clarification is careful to disclose rather than attempt to solve it. **Confirmed genuine and appropriately scoped**, not overreach.

---

## 7. Challenge — Fail-Closed Semantics, Ambient Authority, Per-Record Evaluation, Non-Disclosure

Independently re-checked each against the mechanism as drafted (accounting for Section 5's own finding, which narrows but does not invalidate the mechanism for its own primary, in-scope subject): the "no rule matches → DENIED" default is untouched — no `PermissionPolicyRule` outcome is added by this document. `requestingPrincipalId` remains explicit throughout every code path examined. `PermissionFilteredMemoryRetrieval`'s own per-record call pattern (Errata 004 §9) and null-collapsing return shape (Errata 004 §8) are untouched by anything inside `DefaultPermissionPolicy.evaluate`. **All confirmed preserved**, for the mechanism's own actual, in-scope subject.

---

## 8. Challenge — Interaction With the Collision Clarification's Own Verb-Phrase Mechanism

Independently re-read the Collision Clarification's own Section 4 and its own ICR's Section 6 (item-1/item-2 independence finding). Confirmed the two mechanisms operate on genuinely separate steps (derivation eligibility vs. rule-outcome matching) and share a common, disclosed rationale (both live at the tier Errata 004 §7 itself already names). **No conflict found.**

---

## 9. Challenge — Does "Readiness for Scope Lock" (Section 9) Require Adjustment Given Section 5's Own Finding?

Checked directly: Section 9's own scoping of the lawfully-beginnable Scope Lock work ("the mechanism enabling verb-phrase-specific rule matching and closed-set, no-Resource-required derivation, with no rule content that approves `memory.retrieve`/`memory.retrieve_document`") does not itself claim uniform coverage of all nine action names, and remains accurate once Section 6 is corrected to disclose the `transition_status`/`delete_record` caveat — a future Scope Lock would simply need to inherit that same disclosed caveat rather than silently build a uniform mechanism. **No separate correction to Section 9 required**, provided Section 6's own correction (Section 5, above) is applied first.

---

## 10. Findings

**Two required corrections:**

1. **Section 6** presents the resolution-derivation mechanism as working uniformly across all nine Errata-004-named action names, but `memory.transition_status` and `memory.delete_record` cannot be reduced to a single `(action, resourceType)` pair from the verb phrase alone — their own resourceType depends on the transitioned record's own kind, information not carried by the action name or the `ExecutionRequest`. The mechanism is fully sound for the seven single-typed action names, and in particular for the two this task's own scope actually concerns (`memory.retrieve`, `memory.retrieve_document`); the two-action exception must be disclosed, not silently generalised over.
2. **Section 3's own table** claims "the only two production classes... constructing `targetResources = emptyList()`" — accurate for literal construction, but a broader grep independently confirms `InMemoryAgentRuntime.kt` also constructs an `ExecutionRequest` with a caller/Planner-supplied `targetResources` that could, in principle, be empty. This does not undermine the mechanism (Section 6's own verb-phrase-closed-set condition already guards against it, consistent with the system's own pre-existing, verb-phrase-is-the-trust-boundary design), but the claim should be stated precisely.

No other required correction was found. The governance-tension resolution, the resource-existence/classification separation, the shared-decorator finding's own genuineness and appropriate scope, fail-closed/ambient-authority/per-record/non-disclosure preservation, and the interaction with the Collision Clarification's own mechanism were each independently re-derived from primary sources, not merely re-accepted from the Clarification's own self-check.

---

## Constitutional Verdict

```
REQUIRES REVISION
```

Two narrow, required corrections (Sections 4 and 5, above): disclose the `memory.transition_status`/`memory.delete_record` exception in Section 6 rather than implying uniform coverage, and state Section 3's own "closed set" claim precisely. Proceeding to a Defect Confirmation Review after both corrections are applied.

**Post-correction status:** both required corrections were applied to `docs/governance/TRUST_FRAMEWORK_MEMORY_RETRIEVAL_RESOLUTION_DERIVATION_MECHANISM_CLARIFICATION.md` Sections 3, 4, and 6. See `docs/reviews/TRUST_FRAMEWORK_MEMORY_RETRIEVAL_RESOLUTION_DERIVATION_MECHANISM_CLARIFICATION_DEFECT_CONFIRMATION_REVIEW.md` for the narrow Defect Confirmation Review, which found both corrections complete and no further defect. The Clarification is accepted as of that review.
