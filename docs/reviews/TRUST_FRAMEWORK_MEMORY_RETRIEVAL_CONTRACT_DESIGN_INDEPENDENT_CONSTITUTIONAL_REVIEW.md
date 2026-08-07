**Status:** Genuine Independent Constitutional Review, performed as if by another reviewer, against the governing documents and the actual, current file contents re-read fresh — not against the Contract Design's own self-check (Section 22) alone. This document does not amend the Contract Design, any frozen or draft governance document, or any Kotlin/test file. Nothing is staged, committed, or pushed.

# Trust Framework — Memory Retrieval Architecture — Contract Design (Gap #54) — Independent Constitutional Review

## 1. Baseline Confirmation

`git status --short` confirms the Contract Design and this review are the only new files at review time, alongside the Parker Conversational Memory Bridge's own already-known, deliberately-uncommitted work. The Contract Design's own Section 1 (Governing Context) citation list was independently re-checked against the actual repository — every cited file exists at the path given.

---

## 2. Challenge — Is Gap #54's Own Root-Cause Trace (Section 3) Actually Correct, Re-Derived Independently?

Re-traced fresh, not accepted from the Contract Design's own account: `PermissionFilteredMemoryRetrieval.buildExecutionRequest` (read directly) constructs `targetResources = emptyList()` unconditionally. `DefaultPermissionPolicy.evaluate` (read directly) computes `resourceTypes` via `request.targetResources.mapNotNull { resourceRegistry.resolve(it) }.map { it.resourceType }.toSet()` — an empty list produces an empty set by construction, independent of `ResourceRegistry`'s own content. `ActionMapper.mapOne` (read directly) requires `it.resourceType in targetResourceTypes` — an empty set membership test is always `false`. **Independently confirmed correct**, exactly as traced.

---

## 3. Challenge — Are the Chapter 8 and Errata 004 Citations (Section 3a) Accurate, or Overstated?

Independently re-read `docs/architecture/08-resource-registry.md` in full (it is six lines): "The Resource Registry is the authoritative catalogue of every protected object within Parker. Resources include Memory, World Model, Documents, Calendar, Email, Home Assistant, Plugins, Tools, Agents, Configuration and Logs. If something is not represented within the Resource Registry, Parker assumes it is inaccessible." Exact match to the Contract Design's own quotation. The "Chapter 4" mis-citation in `Resource.kt`'s own KDoc was independently re-confirmed (the phrase appears in Chapter 8, not in `docs/architecture/04-guiding-principles-and-invariants.md`, which was independently searched and does not contain it). Independently re-read Errata 004 §7 in full: the quoted passage ("Correctly evaluating the frozen mapping table therefore requires `ParkerRuntime` composition...") is an exact, contiguous quotation, not assembled from disjoint fragments. **Both citations are accurate, not overstated.**

---

## 4. Challenge — Is the Section 3b Finding (Policy-Rule Collision) Genuine, or an Artefact of Misreading `DefaultKnowledgeRetrieval`?

Independently re-read `DefaultKnowledgeRetrieval.kt` directly: its own `ExecutionRequest` construction (lines confirmed) sets `targetResources = listOf(KNOWLEDGE_RETRIEVAL_RESOURCE_ID)` and `proposedActions = listOf(RETRIEVE_ACTION_NAME)`, where `RETRIEVE_ACTION_NAME = "knowledge.retrieve"`. Independently re-read `ParkerRuntime.kt`'s own resource registration: `KNOWLEDGE_RETRIEVAL_RESOURCE_ID` is registered with `resourceType = ResourceType.MEMORY`. Independently re-read the composed policy rules: `PermissionPolicyRule(action = READ, resourceType = MEMORY, outcome = APPROVED, level = AUTOMATIC)` exists, unconditioned on verb phrase. Independently re-read `DefaultPermissionPolicy.ruleOutcomeFor`: `rules.find { it.action == mapping.action && it.resourceType == mapping.resourceType }` — keyed only by the pair, never by the originating verb phrase. **The collision is genuine and independently reproducible from primary source alone.** This is the Contract Design's own strongest, most consequential finding, and it holds.

---

## 5. Challenge — Was a Simpler Alternative Overlooked? (Substantive Finding)

Pressed directly, since Section 19's own candidate list for the deferred prerequisite question should be as complete as the evidence allows before a future pass begins from it. The Contract Design's own Section 19, item 2 lists: policy keyed by verb phrase directly; a second, more specific matching tier; or accepting that Evidence Intelligence's own guarantee must move to a different layer. **A fourth, materially simpler candidate was not listed**: giving `DefaultKnowledgeRetrieval`'s own registered Resource a `resourceType` distinct from `MEMORY` (a new, or differently-chosen existing, `ResourceType` value), rather than reusing `MEMORY`. This would make `(READ, MEMORY)` (Memory Core's own eventual, Candidate-D-enabled resolution) and Knowledge Retrieval's own `(READ, <distinct type>)` structurally non-colliding pairs, each governed by its own independent policy rule — with **no change whatsoever to `DefaultPermissionPolicy`'s own matching algorithm, no new policy dimension, and no redesign of the deterministic table-driven model**.

Independently checked whether this would reopen frozen or already-adopted governance: `docs/governance/PROGRAMME_3_UNIT_9_SCOPE_LOCK_CLARIFICATION.md`, read directly and searched, contains no mention of `ResourceType.MEMORY` or any commitment to a specific `ResourceType` value for Knowledge Retrieval — it classifies Knowledge Retrieval as a proposal class requiring gating (a Clarification-tier decision) but leaves the specific `ResourceType` choice unaddressed, meaning it was fixed later, at `ParkerRuntime.kt`'s own composition time — the same tier Errata 004 itself already authorises composition-time fixes to be made at. **This candidate would not reopen any frozen or adopted governance tier**, and appears, on this evidence, to be cheaper than every option Section 19 currently lists — a genuine gap in the Contract Design's own candidate completeness, not a defect in its reasoning about the candidates it did consider.

**This finding requires a correction** — Section 19's own candidate list for the deferred prerequisite question should include this option, and Section 21's own Recommendation should note it as worth checking first, precisely because it may resolve the prerequisite question without requiring any change to the shared permission-resolution mechanism at all. See Findings and Verdict, below.

---

## 6. Challenge — Does the Design Preserve Fail-Closed Semantics?

Checked directly against every candidate the Contract Design recommends or leaves open: Candidate D only ever activates a *second resolution path* when `targetResources` is empty by a caller's own structural design (never as a fallback for a resolution that merely failed) — a genuinely unresolvable action or resource, on any candidate, still denies, exactly as today. The newly-surfaced Section 5 alternative (distinct `ResourceType`) does not touch fail-closed semantics at all — it only changes which rule two different callers' requests are matched against. **Fail-closed semantics are preserved by every direction this design leaves open.**

---

## 7. Challenge — Does the Design Accidentally Create Ambient Authority?

Checked directly: every candidate retains `requestingPrincipalId` as an explicit parameter on every call; none reads implicit context, a thread-local, or a factory-closed identity. Candidate B (creator-read-back), the one candidate that comes closest to an ambient-authority risk, is explicitly rejected in the Contract Design's own Section 18, and correctly so — resolving a record by provenance-matching its own creator is not "ambient" in the strict sense (the principal is still explicit), but it would be a new, ungoverned *policy* concept, correctly identified and correctly not adopted. **No ambient authority is introduced by anything this design recommends.**

---

## 8. Challenge — Has Memory Core Been Improperly Absorbed Into Resource Registry Semantics?

Checked directly: Candidate A (the direction that would do this) is explicitly rejected, on two independently-verified grounds — Memory Core Scope Lock §6's own boundary, and the durability finding. Independently re-verified the durability claim: `InMemoryResourceRegistry` (read directly, in full) is a bare `mutableMapOf` behind a `Mutex`, with no file-backing, no `DurableMemoryCore`-equivalent, and `ParkerRuntime.kt` constructs it fresh (`InMemoryResourceRegistry()`) on every `start()`. **Confirmed independently: Candidate A would genuinely regress Memory Core Durability's own already-completed guarantees if adopted**, and the Contract Design is correct to treat this as disqualifying rather than merely costly.

---

## 9. Challenge — Has a Second Authorisation System Been Invented?

Checked directly against Candidate D (the recommended direction's own foundation): it proposes extending `DefaultPermissionPolicy`'s own single resolution step, not constructing a second `PermissionEngine` or a second `DefaultPermissionPolicy` instance. Chapter 10 §9's own text, independently re-read: "may never construct, implement, or substitute a second authority in the Permission Engine's place" — Candidate D does not do this; it adds a second *resolution path within* the one authority, analogous to how the existing Resource-based path and a hypothetical vocabulary-declared path would both terminate in the identical, single `PermissionEngine.evaluate` call and the identical, single policy-rule table. **No second authorisation system is created by the recommended direction.** Candidate C, as originally framed, was the one direction genuinely at risk of this — correctly rejected in Section 18.

---

## 10. Challenge — Do Caller-Specific Exceptions Exist Anywhere in the Recommended Direction?

Checked directly: Candidate D's own activation condition ("`targetResources` is empty by the caller's own structural design") is a *shape* test, not an *identity* test — any future caller structurally shaped like Memory Core's own retrieval (permanently resourceless, per its own frozen design) would activate the identical path, without naming any specific caller. Candidates B and C, both explicitly rejected, were the two directions genuinely at risk of this. **No caller-specific exception survives in what the Contract Design actually recommends.**

---

## 11. Challenge — Does Evidence Intelligence Remain Constitutionally Intact?

This is the Contract Design's own central, disclosed reason for not forcing a final selection (Section 17), and it is independently confirmed sound: adopting Candidate D in isolation, today, would silently convert `EvidenceIntelligenceInputResolver`'s own currently-correct, already-verified fail-closed retrieval into an approved one, via the shared `(READ, MEMORY)` rule (Section 4/9, above). The Contract Design's own refusal to select a final mechanism until this is separately closed is the correct, constitutionally-careful outcome — not indecision for its own sake.

---

## 12. Challenge — Would Future Conversational Retrieval Use the Same General Architecture?

Checked directly: nothing in Candidate D, or in the Section 5 alternative newly surfaced by this review, is scoped to Memory Core specifically in a way that would exclude a future, similarly-shaped caller (a conversational retrieval unit, once built) from using the identical resolution path and the identical policy-rule model. **Confirmed general**, not a one-off accommodation.

---

## 13. Challenge — Is Resource/Action Identity Sufficiently Specified?

Action identity (Section 8): fully specified, unchanged from Errata 004 §7's own frozen table — independently re-confirmed by direct re-read of `PermissionFilteredMemoryRetrieval`'s own companion object (`RETRIEVE_ACTION_NAME = "memory.retrieve"`, `RETRIEVE_DOCUMENT_ACTION_NAME = "memory.retrieve_document"`). Resource identity: correctly, explicitly left open (Section 6, Section 17) — this is a disclosed deferral, not an unspecified gap the document fails to acknowledge.

---

## 14. Challenge — Were Implementation Decisions Frozen Prematurely?

Checked directly: Section 17 explicitly declines to select a final mechanism; Section 19 lists deferred shapes rather than a single chosen one (subject to the Section 5 correction above). **No implementation decision is frozen prematurely** — if anything, the document under-specifies rather than over-specifies, consistent with its own governing task's explicit permission to do so.

---

## 15. Challenge — Does Any Existing Frozen Governance Get Contradicted?

Checked every requirement the Contract Design treats as load-bearing (Section 4a/4b) against its own cited primary source, independently: Constitution-tier quotations (via Chapter 10's own citations) match; Errata 004 §§2–4, 8, 11 quotations match; Memory Core Scope Lock §6 quotation matches (already independently verified once this session, for a different task, and re-confirmed here). **No contradiction found.**

---

## 16. Full, Independent Quotation Audit

| Quoted fragment | Cited source | Verified |
| --- | --- | --- |
| "The Resource Registry is the authoritative catalogue of every protected object within Parker... If something is not represented within the Resource Registry, Parker assumes it is inaccessible." | Chapter 8, in full | Exact match, independently re-read in Section 3, above. |
| "Correctly evaluating the frozen mapping table therefore requires `ParkerRuntime` composition... No new permission action or resource type is required to make this work — only a policy/engine composition decision, made once, at `ParkerRuntime` construction." | Errata 004 §7 | Exact, contiguous quotation, independently re-read in Section 3, above. |
| "may never construct, implement, or substitute a second authority in the Permission Engine's place" | Chapter 10 §9 (§10 in the document's own section numbering as currently drafted) | Exact match, independently re-read in Section 9, above. |
| "Runtime performs all permission decisions before invoking Memory Core... without exception." | Memory Core Scope Lock §6 | Exact match, already independently verified this session for a separate task, re-confirmed here by direct re-read. |

No further quoted fragment appears beyond ordinary identifiers and file paths. **No misquotation found.**

---

## Findings

One required correction: Section 19's own candidate list for the deferred prerequisite question omits a materially simpler alternative (a distinct `ResourceType` for Knowledge Retrieval's own registered Resource) that this review independently confirmed would not reopen any frozen or already-adopted governance tier, and that appears cheaper than every option currently listed. This does not undermine the Contract Design's own central reasoning (Sections 3–17 are independently confirmed sound throughout) — it narrows, rather than contradicts, the document's own already-correct refusal to force a final selection.

No other required correction was found. Fail-closed semantics, absence of ambient authority, Memory Core's own boundary, the single-authority guarantee, absence of caller-specific exceptions, Evidence Intelligence's own continued integrity, resource/action identity's own disclosed specification state, and consistency with every cited frozen source were each independently re-derived from primary sources, not merely re-accepted from the Contract Design's own self-check.

---

## Constitutional Verdict

```
REQUIRES REVISION
```

One narrow, required correction (Section 5, above). Proceeding to a Defect Confirmation Review after the correction is applied.

**Post-correction status:** the required correction was applied to `docs/governance/TRUST_FRAMEWORK_MEMORY_RETRIEVAL_CONTRACT_DESIGN.md` Sections 19 and 21. See `docs/reviews/TRUST_FRAMEWORK_MEMORY_RETRIEVAL_CONTRACT_DESIGN_DEFECT_CONFIRMATION_REVIEW.md` for the narrow Defect Confirmation Review, which found the correction complete and no further defect. The Contract Design is accepted as of that review.
