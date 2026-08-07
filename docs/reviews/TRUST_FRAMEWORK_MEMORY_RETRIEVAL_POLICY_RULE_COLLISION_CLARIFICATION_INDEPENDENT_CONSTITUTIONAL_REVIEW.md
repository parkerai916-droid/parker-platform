**Status:** Genuine Independent Constitutional Review of `docs/governance/TRUST_FRAMEWORK_MEMORY_RETRIEVAL_POLICY_RULE_COLLISION_CLARIFICATION.md`, performed as if by another reviewer, against the governing documents and the actual, current file contents re-read fresh — not against that document's own Section 9 self-check alone. This document does not amend the Clarification, any frozen or draft governance document, or any Kotlin/test file. Nothing is staged, committed, or pushed.

# Trust Framework — Memory Retrieval Policy Rule Collision Clarification — Independent Constitutional Review

## 1. Baseline Confirmation

`git status --short` confirms the Clarification and this review are the only new files at review time, alongside the already-known, deliberately-uncommitted Parker Conversational Memory Bridge work and its own prior-task governance documents. The Clarification's own Section 1 citation list was independently re-checked against the actual repository — every cited file exists at the path given.

---

## 2. Challenge — Is the Collision (Section 2) Correctly and Currently Stated?

Re-traced fresh: `DefaultPermissionPolicy.evaluate`'s own `ruleOutcomeFor` (`rules.find { it.action == mapping.action && it.resourceType == mapping.resourceType }`), independently re-read, confirms no verb-phrase awareness exists. `ParkerRuntime.kt` independently re-read: `KNOWLEDGE_RETRIEVAL_RESOURCE_ID` registered with `resourceType = MEMORY` (line ~917), `knowledge.retrieve` registered in `ActionVocabulary` mapping to `(READ, MEMORY)` (line ~932), and a `PermissionPolicyRule(READ, MEMORY, APPROVED, AUTOMATIC)` already present in the composed policy (line ~576–581). `memory.retrieve`/`memory.retrieve_document` independently confirmed **not** registered in `ActionVocabulary` anywhere in `ParkerRuntime.kt` (grepped; the only two matches are the file's own comment disclosing the deliberate omission, and `DefaultKnowledgeRetrieval`'s unrelated constant). **Confirmed accurate**, exactly as stated.

---

## 3. Challenge — Section 3.1's Rejection of "Distinct `ResourceType`" — Is the Stated Reasoning Actually Sound? (Substantive Finding)

This is the Clarification's own central, load-bearing move — it reverses the prior Contract Design ICR's recommendation — so it receives the closest scrutiny.

Section 3.1 states the candidate is "not achievable without minting a new `ResourceType` enum value," citing the Unit 9 Permission Clarification §11's non-expansion of "any new `PermissionAction` or `ResourceType` value."

**This claim is not quite accurate, and the imprecision matters.** `ResourceType` already has fourteen values, including `WORLD_MODEL`, `DOCUMENT`, `TOOL`, `PLUGIN`, and others. Nothing in §11's literal text, or in `Resource.kt`, or in `ResourceRegistry.kt`, prevents `KNOWLEDGE_RETRIEVAL_RESOURCE_ID` from being registered with `resourceType = WORLD_MODEL` (or any other already-existing value distinct from `MEMORY`) instead of minting anything new — that is registering with an *existing* enum value, not a new one, and §11 forbids only the latter. **A materially simpler sub-variant of Candidate 3.1 therefore survives §11's literal text and was not addressed by the Clarification as drafted** — a real gap, not a restatement of what the Clarification already covers.

Having identified that gap, this review presses on whether that surviving sub-variant is actually lawful, since the Clarification's own conclusion (reject Section 3.1 entirely) could still be right for a different, undisclosed reason. Independently checked:

- `docs/specifications/volume-01-core-contracts/Resource.md` (read directly): "Resource Categories" enumerates `ResourceType`'s values as named, distinct kinds of protected object ("A Resource is anything Parker can read, create, modify, delete, execute, export, observe, or protect"); Chapter 8 (read directly): "The Resource Registry is the **authoritative catalogue** of every protected object within Parker" (emphasis reflects the word actually used). Neither document states a resourceType must accurately describe the resource's real nature in so many words, but both frame the Registry as an authoritative, truthful catalogue of what things actually are — not a namespace free to be gamed for policy-partitioning purposes.
- `WORLD_MODEL` is independently confirmed, from `DefaultKnowledgeRetrieval.kt`'s own KDoc (read directly, already quoted in the Contract Design's own citation list), to denote a genuinely different, already-governed subsystem: "`DefaultWorldModelUpdatePolicy.DEFAULT_STALE_AFTER`'s own identical naming convention -- though that World Model policy governs a genuinely different, legitimately age-based epistemic model (belief transience, per ADR-024), never this Unit's own evidence-status-change condition." Knowledge Retrieval's own boundary is Knowledge Memory's promoted-evidence surface (Contract Design V2), constitutionally distinct from the World Model (belief transience, ADR-024) — registering Knowledge Retrieval's Resource as `WORLD_MODEL` would misclassify it in the Registry's own "authoritative catalogue," not merely choose an inelegant label.
- This is, independently, exactly the pattern this task's own governing instructions forbid in substance even where not forbidden in literal enum-admission terms: **"Do not select a solution simply because it requires fewer code changes."** Reusing `WORLD_MODEL` (or any other existing, semantically wrong value) purely to dodge a policy-rule collision is a resource-classification decision driven by mechanism convenience, not by what the resource actually is — the same defect in substance the Clarification's own Section 3.1 already, correctly, rejects the "mint a new value" sub-variant for, but by a route the Clarification's own drafted reasoning does not cover.

**Conclusion: Section 3.1's ultimate verdict (reject) is correct and survives this challenge, but its stated reasoning is incomplete — it forecloses only the "mint a new value" sub-variant, not the "reuse an existing, semantically-mismatched value" sub-variant, which is the more tempting, more literally-permitted-by-§11 version of the same candidate a future reader might reach for.** This is a genuine, required correction, not a restylement.

---

## 4. Challenge — Is the Selected Mechanism (Section 3.2/4) Actually General, Non-Caller-Specific, and Confined to `DefaultPermissionPolicy`?

Independently re-read `ActionMapper.kt`: `ActionMappingResult.Resolved(proposedAction, applicable)` is confirmed to already carry the verb-phrase string (`mapOne`'s own `return ... ActionMappingResult.Resolved(proposedAction, applicable)`), and `DefaultPermissionPolicy.evaluate`'s own `resolvedMappings = ...flatMap { it.mappings }` is confirmed to discard it. The Clarification's claim that this data is "already computed, not absent" is independently verified true, not an assumption.

Checked whether every real caller ever supplies more than one `proposedAction` per `ExecutionRequest`, which would complicate the "verb phrase per mapping" association the mechanism depends on: independently re-read `PermissionFilteredMemoryRetrieval.buildExecutionRequest` (`proposedActions = listOf(actionName)`, always singleton) and `DefaultKnowledgeRetrieval.buildExecutionRequest` (`proposedActions = listOf(RETRIEVE_ACTION_NAME)`, always singleton). **Confirmed: every current production caller of the two colliding verb phrases supplies exactly one `proposedAction`, so no multi-action-per-request ambiguity exists for this mechanism to resolve incorrectly.** The Clarification does not make this claim explicitly — it is a supporting fact worth stating, not a defect, since the mechanism as described would still be well-defined even for a hypothetical multi-action request (each `ActionMappingResult.Resolved` already carries its own `proposedAction` independently), but confirming it holds for every actual caller today strengthens confidence beyond what the document currently discloses.

Checked for caller-specificity: the discriminator is the Action Vocabulary verb phrase, not `PrincipalId`, module identity, or any caller-supplied metadata. **Confirmed general**, no `if caller == X` shape.

---

## 5. Challenge — Does the Selected Mechanism Actually Require No Frozen-Governance Amendment?

Independently re-read `PermissionPolicy.md` §3 ("Requested Action. What the Principal is proposing to do") and §9 ("Policy Extensibility... without changing `PermissionEngine`'s public contract"). Both independently confirmed to say what the Clarification quotes. Independently re-read Errata 004 §7's own frozen table (via the Contract Design's own already-verified quotation, itself independently re-confirmed against Errata 004 directly in the prior task's own audit and not contradicted by anything found in this review) — the action-name-to-`(action, resourceType)` mapping is unchanged by this mechanism; only whether two action names sharing that pair may receive different rule outcomes is newly addressed, a question Errata 004 never spoke to. **Confirmed: no frozen document's own text is altered, contradicted, or reopened by the selected mechanism.**

---

## 6. Challenge — Item 1/Item 2 Independence (Section 6, Section 8)

Pressed directly, since the Clarification's own Section 8 conclusion ("not yet ready for Scope Lock... item 1 remains open") depends on these truly being independent, not entangled in a way that makes resolving item 2 already partially, silently resolve or foreclose item 1. Checked: `ActionMapper.map(proposedActions, targetResourceTypes)` accepts `targetResourceTypes` as an input regardless of how that set was derived (today: from resolved `Resource`s; under a future Candidate D fix: potentially also from a fallback ActionVocabulary-only derivation) — its own output shape (`List<ActionMappingResult>`, each `Resolved` carrying `proposedAction` and its `applicable` mappings) is unaffected by which derivation path produced `targetResourceTypes`. The selected mechanism (Section 3.2/4) operates entirely on `ActionMapper`'s own output, downstream of wherever `targetResourceTypes` came from. **Confirmed genuinely independent** — resolving item 2 does not presuppose, foreclose, or silently answer item 1's own separate question.

---

## 7. Challenge — Fail-Closed Semantics, Ambient Authority, Second Authorisation System, Non-Disclosure

Checked directly against Section 4's own selected mechanism: the "no rule matches → DENIED" default (`PermissionPolicy.md` §4, independently re-read) is untouched — adding an optional, more specific discriminator to a rule does not remove or weaken the default-deny path for any request that matches no rule at all, specific or coarse. No implicit identity signal is introduced — the verb phrase is already explicit on `ExecutionRequest.proposedActions`. No second `PermissionEngine`/`DefaultPermissionPolicy` instance is proposed — confirmed confined to one class's internal rule-matching logic. Errata 004 §8's own non-disclosure guarantee (`null` for both absence and denial) is untouched — this mechanism operates entirely upstream of `PermissionFilteredMemoryRetrieval`'s own return-shape, which nothing here proposes changing. **All four preserved.**

---

## 8. Challenge — CDR-005 Applicability (Section 6, item 5)

Independently re-read CDR-005's own "Decision Rules" (§"Who defines a domain act," "When a CDR is required"): a CDR is required "whenever a domain's self-certification against Chapter 10's criteria is genuinely contested, ambiguous, or would require choosing between two or more constitutionally plausible readings" for a *newly introduced act*. `memory.retrieve` and `knowledge.retrieve` are both already-classified, already-gated acts (both already reach `PermissionEngine.evaluate` in the live composed runtime or are structurally designed to). The selected mechanism introduces no new act and no new proposal class — only a finer-grained internal matching capability for two already-classified acts. **Confirmed: the Clarification's own "CDR-005 not required" conclusion is correct.**

---

## 9. Challenge — Does the Clarification Overreach Its Own Narrow Scope?

Checked against the task's own explicit instruction ("Resolve the single deferred prerequisite question... Do not select a solution simply because it requires fewer code changes... the chosen model must preserve semantic distinction... even if both are read-like operations"). Section 7 ("Non-Decisions") explicitly declines to select `memory.retrieve`'s own eventual outcome, declines to fix the Kotlin shape, and declines to resolve item 1. Section 8 explicitly declines to declare Gap #54 ready for Scope Lock. **No overreach found** — the Clarification is, if anything, conservative about what it claims to have settled.

---

## 10. Findings

**One required correction:** Section 3.1's rejection of "distinct `ResourceType`" is reasoned incompletely — it forecloses only the "mint a new `ResourceType` value" sub-variant (correctly, citing the Unit 9 Permission Clarification §11) but does not address the "reuse an existing, different `ResourceType` value" sub-variant, which survives §11's literal text and is the more tempting version of the same candidate a future reader might independently propose. The correct disqualifying reason for that sub-variant — resource-type misclassification against Chapter 8's own "authoritative catalogue" framing and `Resource.md`'s own named Resource Categories, evidenced concretely by `WORLD_MODEL` already denoting a distinct, separately-governed subsystem (belief transience, ADR-024) — exists and is sound, but is not currently stated in the Clarification. The document's ultimate verdict (reject Section 3.1 in its entirety, select Section 3.2/4 instead) is unaffected; only the completeness of the stated reasoning requires correction.

No other required correction was found. The collision statement, the selected mechanism's generality and confinement to `DefaultPermissionPolicy`, its non-amendment of frozen governance, the independence of items 1 and 2, fail-closed/ambient-authority/second-authorisation/non-disclosure preservation, CDR-005 non-applicability, and the document's own scope discipline were each independently re-derived from primary sources, not merely re-accepted from the Clarification's own self-check.

---

## Constitutional Verdict

```
REQUIRES REVISION
```

One narrow, required correction (Section 3, above): extend Section 3.1's own reasoning to explicitly address and foreclose the "reuse an existing, different `ResourceType` value" sub-variant, not only the "mint a new value" sub-variant. Proceeding to a Defect Confirmation Review after the correction is applied.

**Post-correction status:** the required correction was applied to `docs/governance/TRUST_FRAMEWORK_MEMORY_RETRIEVAL_POLICY_RULE_COLLISION_CLARIFICATION.md` Sections 3.1 and 5. See `docs/reviews/TRUST_FRAMEWORK_MEMORY_RETRIEVAL_POLICY_RULE_COLLISION_CLARIFICATION_DEFECT_CONFIRMATION_REVIEW.md` for the narrow Defect Confirmation Review, which found the correction complete and no further defect. The Clarification is accepted as of that review.
