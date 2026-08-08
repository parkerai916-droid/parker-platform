**Status:** Genuine Independent Constitutional Review, performed as if by another reviewer, against the governing documents and the actual, current file contents re-read fresh — not against the Completion Review's own summary of them. This document does not amend any production file, test file, the Completion Review, the governance clarification, or its own already-complete ICR. Nothing is staged, committed, or pushed.

# Parker Conversational Memory Bridge — Admission Unit — Independent Constitutional Review

## 1. Baseline and Diff-Shape Re-Verification

Independently re-confirmed: `git status --short` shows fifteen modified files and six new files, matching the Completion Review's own account exactly. `git diff --stat` shows no file under Memory Core Durability's own governed scope (`DurableMemoryCore.kt`, `FileSystemMemoryCoreDurabilityLog.kt`, `MemoryCoreRecovery.kt`, `InMemoryMemoryCore.kt`) and no change to `EvidenceRegistrationCoordinator.kt` or `EvidenceIntelligenceAcceptanceCoordinator.kt` — both independently confirmed empty. `KnowledgeCandidate(` is constructed at exactly one production call site in the entire repository (`MemoryAdmissionCoordinator.kt`), confirmed by direct `grep`, not assumed.

---

## 2. Challenge — Was the Governance Clarification Genuinely Necessary, or Could the Existing Two-Factor Gate Have Been Satisfied Some Other Way?

Re-derived independently, not re-accepted: `Assertion.confidence: Double?` is the only reachable second factor, set once at `CandidateAssertion` construction. For a proposition whose sole basis is the current conversation turn, no independent evidential act exists to derive a non-fabricated value from — inventing one would violate Article XIV directly. This is a structural absence, not an implementation gap fixable within the existing gate's own terms. The Clarification's own central argument (Section 6) is sound, and was already independently re-verified by that document's own dedicated ICR (`ACCEPTED`) before this Unit's own Kotlin was written. **Confirmed necessary.**

---

## 3. Challenge — Does `soleBasisIsExplicitInstruction` Genuinely Leave the Ordinary Gate Unaffected for Every Other Candidate?

Read `DefaultKnowledgeCandidateEvaluator.kt` directly, in full, fresh. The new branch:

```kotlin
if (candidate.soleBasisIsExplicitInstruction == true) {
    return@runBlocking promote(..., state = EvidentialState.UNKNOWN, basis = "...")
}
```

is inserted after the Contradiction check and before the two-factor gate, checked only against `soleBasisIsExplicitInstruction` — a field distinct from `explicitlyRequested`, confirmed by direct re-read of `KnowledgeStore.kt`'s own current field list (`evidenceReference`, `explicitlyRequested`, `soleBasisIsExplicitInstruction`, in that order). Every other line of the pre-existing two-factor gate is byte-for-byte unchanged (confirmed via `git diff` showing only the one inserted block, no surrounding line altered). A candidate with `explicitlyRequested = true` and `soleBasisIsExplicitInstruction` left at its default `null` — the shape every pre-existing construction site (`EvidenceIntelligenceInputResolver`, wherever a candidate is built for Evidence Intelligence's own pipeline) already produces — takes the identical code path it always did. **Confirmed: the ordinary gate is genuinely, structurally unaffected for every candidate this Unit did not itself construct.**

---

## 4. Challenge — Is `EvidentialState.UNKNOWN` Genuinely the Correct, Weakest-Honest Classification, or Could `INDETERMINATE` Apply?

Independently re-derived, not re-accepted from the Clarification's own ICR: `EvidentialState.kt`'s own KDoc distinguishes `UNKNOWN` ("presumes the matter is knowable in principle... given further evidence") from `INDETERMINATE` ("cannot presently be established whether the matter is knowable at all"). An owner's explicit statement is knowable-in-principle by construction — `INDETERMINATE` would overstate the epistemic void. **Sound**, and consistent with `DefaultKnowledgeCandidateEvaluator`'s own pre-existing, unmodified reasoning for never assigning `INDETERMINATE` anywhere else in the class.

---

## 5. Challenge — Is Response Integrity Genuinely Structural, or Could a Future Change Silently Reintroduce a False Success Claim?

Read `ConversationReplyCoordinator.kt` directly. `buildAdmissionReply` is a `private` function with an exhaustive `when` over the three-variant sealed `MemoryAdmissionOutcome`, each branch producing a fixed string template — no branch reads `response.text` (the model's own claimed text) or any other Reasoning-Provider-sourced value. The `Remember` branch in `submitAndDeliver`'s own dispatch calls `buildAdmissionReply(memoryAdmissionCoordinator.admit(...))` — the argument to `buildAdmissionReply` is the coordinator's own *return value*, not the original `response`, so there is no code path by which the model's own text could reach the owner for this branch even accidentally. This is verified further by a dedicated test (`the success reply is never composed until MemoryAdmissionCoordinator actually returns Stored`) using a model response engineered to say something else entirely, confirmed to never leak through. **Structurally enforced, not merely asserted** — a future change would have to actively rewire `buildAdmissionReply`'s own signature to reintroduce the risk, not merely add a line.

---

## 6. Challenge — Independently Re-Verify the Central, Newly-Discovered Gap (`PermissionFilteredMemoryRetrieval` Fail-Closed) From Primary Sources, Not the Completion Review's Own Account

Traced fresh, independent of the Completion Review's own narrative:

1. `PermissionFilteredMemoryRetrieval.kt`'s own KDoc, read directly: "Memory Core records are never Resource Registry entries, so `ExecutionRequest.targetResources` is always `emptyList()`."
2. `DefaultPermissionPolicy.evaluate`, read directly: `resourceTypes` is computed by resolving `request.targetResources` against `resourceRegistry` — an empty list resolves to an empty set, unconditionally.
3. `ActionMapper.mapOne`, read directly: `entry.mappings.filter { it.resourceType in targetResourceTypes }` — an empty `targetResourceTypes` set can never contain any `ResourceType`, so `applicable` is always empty, regardless of whether `entry` (the vocabulary lookup) itself succeeded or failed.
4. Independently confirmed via `grep`: `RETRIEVE_ACTION_NAME`/`RETRIEVE_DOCUMENT_ACTION_NAME` (`PermissionFilteredMemoryRetrieval`'s own companion constants) are never registered as `ActionVocabularyEntry` verb phrases anywhere in `ParkerRuntime.kt` — but per point 3, registering them would not change the outcome, since the failure occurs one step earlier, at the resource-type-resolution stage, not at vocabulary lookup.

**This four-step trace is independently reproducible from primary source alone and requires no assumption from the Completion Review's own account.** The conclusion — no in-scope fix exists without altering `DefaultPermissionPolicy`, `ActionMapper`, or Memory Core's own Resource-representation choice — is confirmed sound.

**Further independently checked**: `EvidenceIntelligenceAcceptanceCoordinator.kt`'s own `dispatch` method, read directly, confirms line 278 calls `knowledgeSubmission.submit(requestingPrincipalId, result.knowledgeCandidate)` — the identical `KnowledgeSubmission` instance this Unit's own `MemoryAdmissionCoordinator` receives (both constructed from the same `ParkerRuntime.kt` local `val knowledgeSubmission`, confirmed by direct re-read of the composition root). The claim that this Unit's own newly-discovered gap equally affects Evidence Intelligence's own call path is independently confirmed, not merely asserted by analogy. **Also independently confirmed**: `grep -rn "CandidateRecordProduced(" src` returns only the type's own declaration, never a construction site — the Completion Review's own disclosed caveat (that Evidence Intelligence's own live pipeline may not reach this call today for an entirely separate reason) is accurate and appropriately hedged, not overclaimed.

---

## 7. Challenge — Is `MemoryAdmissionCoordinator`'s Own "No Double Gating" Claim Correct?

Read `MemoryAdmissionCoordinator.kt` directly, in full. Exactly one `permissionEngine.evaluate` call exists in `admit`, targeting `CONVERSATIONAL_MEMORY_RESOURCE_ID`/`CREATE_CONVERSATIONAL_MEMORY_ACTION_NAME` — a resource and action distinct from `KnowledgeSubmission`'s own `KNOWLEDGE_SUBMISSION_RESOURCE_ID`/`SUBMIT_ACTION_NAME`, confirmed by direct comparison of both companion objects. `knowledgeSubmission.submit` is called once, unmodified, performing its own separate, pre-existing gate internally — this class never re-evaluates that decision or wraps the shared `PermissionEngine` a second time around the same proposal. Independently confirmed by `MemoryAdmissionCoordinatorTest`'s own `no double gating` test, which counts exactly two `evaluate` calls per successful admission and asserts no more, no fewer. **Sound.**

---

## 8. Challenge — Is Confidence Genuinely Never Fabricated?

Read `MemoryAdmissionCoordinator.kt`'s own `admit` method directly: `CandidateAssertion(statement = instructionText, provenanceId = provenance.provenanceId, confidence = null)` — a literal `null`, not a computed or defaulted expression. No other line in this class constructs a `CandidateAssertion`. Independently confirmed by a dedicated test reading the resulting `Assertion.confidence` back from `MemoryCore` directly. **Sound.**

---

## 9. Challenge — Does the `ParkerRuntime.kt` Construction Reordering Introduce Any Risk?

Read the current `buildAndRegisterRuntimeGraph` directly: `conversationReplyCoordinator`'s own construction (and `runtimeEventLogger`'s, moved alongside it) now occurs after `knowledgeSubmission`'s own construction rather than before. Checked every statement between the old and new position for a dependency on either moved value being already-assigned: none exists — the intervening code (`permissionFilteredMemoryRetrieval`, `knowledgeItemPersistence`, `knowledgeCandidateEvaluator`, `knowledgeSubmission`) references neither. Checked forward from the new position to the end of the method: `evidenceIntelligenceAcceptanceCoordinator`, `knowledgeRetrieval`, and the two new resource-registration stages all reference values already in scope by then. **No ordering risk found.**

---

## 10. Full, Independent Quotation Audit

| Quoted fragment | Cited source | Verified |
| --- | --- | --- |
| "no single factor may... determine promotion... absent an express, documented governing-rule exception stated and justified at Scope Lock" | Contract Design V2 §5 | Exact match, re-verified independently in Section 2, above (via the Clarification's own already-complete ICR, itself independently re-checked against this same source at that time). |
| "Memory Core records are never Resource Registry entries, so `ExecutionRequest.targetResources` is always `emptyList()`" | `PermissionFilteredMemoryRetrieval.kt`'s own KDoc | Exact match, confirmed by direct re-read in Section 6, above. |
| "the referenced Memory Core record could not be resolved -- either it does not exist, or access to it was not authorised" | `DefaultKnowledgeCandidateEvaluator.kt`'s own rejection basis text | Exact match, confirmed by direct re-read; also independently reproduced verbatim in this Unit's own live composition test output. |

No further quoted fragment appears in the Completion Review beyond ordinary identifiers and file paths. **No defect found.**

---

## 11. Challenge — Was Anything Beyond This Unit's Own Authorised Scope Implemented?

Checked directly against the task's own explicit exclusion list: no automatic memory extraction, model-decided importance, background harvesting, summarisation, forgetting, pruning, new durability mechanism, new Memory Core persistence, new permission architecture (the one new resource/action pair mirrors five already-established precedents exactly, using the already-existing `WRITE`/`MEMORY` `APPROVED` rule — no new `PermissionPolicyRule` was added), World Model change, or Evidence change appears anywhere in the diff. Conversational retrieval was not built — `knowledgeRetrieval`'s own field and its own "no production entry point consumes it yet" comment are both untouched, confirmed by `git diff` showing zero change to that region of `ParkerRuntime.kt`. **Sound.**

---

## Findings

No required correction was found. The governance clarification's own necessity (Section 2), the ordinary gate's own structural insulation from the new exception (Section 3), the `EvidentialState.UNKNOWN` determination (Section 4), response integrity's own structural (not merely asserted) enforcement (Section 5), the central newly-discovered `PermissionFilteredMemoryRetrieval` gap (Section 6), no-double-gating (Section 7), and confidence non-fabrication (Section 8) are each independently re-derived from primary sources — code, governance text, and dedicated tests — not merely re-accepted from the Completion Review's own account. One claim (Evidence Intelligence's own candidates being "identically wired") was found accurately, if narrowly, stated in the Completion Review after independent re-verification; its own caveat about `CandidateRecordProduced` being currently unconstructed was independently confirmed accurate and appropriately hedged, not a defect.

---

## Constitutional Verdict

```
ACCEPTED
```

No required correction. No Defect Confirmation Review is necessary.

---

## Recommended Next Step

Per this Unit's own governing task: stop before staging, committing, or pushing. Report the significant, pre-existing `PermissionFilteredMemoryRetrieval`/`KnowledgeSubmission` gap prominently as the primary open finding — it blocks the task's own "ultimate acceptance test" regardless of this Unit's own correctness, and requires its own, separately-governed resolution (Trust Framework/Memory Core tier) before a future conversational-retrieval unit's own work could be meaningfully exercised end-to-end.

---

## Final Git Status at Time of This Review

```
$ git status --short
 M src/composition/LoggingReasoningProvider.kt
 M src/composition/ParkerRuntime.kt
 M src/interfaces/KnowledgeStore.kt
 M src/interfaces/ReasoningProvider.kt
 M src/runtime/ConversationReplyCoordinator.kt
 M src/runtime/DefaultEvidenceIntelligence.kt
 M src/runtime/DefaultKnowledgeCandidateEvaluator.kt
 M src/runtime/ReasoningPromptBuilder.kt
 M src/runtime/ReasoningResponseParser.kt
 M src/runtime/ResponseComposer.kt
 M tests/contracts/ReasoningProviderContractTest.kt
 M tests/runtime/ConversationReplyCoordinatorTest.kt
 M tests/runtime/DefaultKnowledgeCandidateEvaluatorTest.kt
 M tests/runtime/ReasoningPromptBuilderTest.kt
 M tests/runtime/ReasoningResponseParserTest.kt
?? docs/governance/PROGRAMME_3_EXPLICIT_OWNER_INSTRUCTION_PROMOTION_EXCEPTION_SCOPE_LOCK_CLARIFICATION.md
?? docs/implementation/CONVERSATIONAL_MEMORY_ADMISSION_IMPLEMENTATION_PLAN.md
?? docs/reviews/CONVERSATIONAL_MEMORY_ADMISSION_COMPLETION_REVIEW.md
?? docs/reviews/CONVERSATIONAL_MEMORY_ADMISSION_INDEPENDENT_CONSTITUTIONAL_REVIEW.md
?? docs/reviews/PROGRAMME_3_EXPLICIT_OWNER_INSTRUCTION_PROMOTION_EXCEPTION_INDEPENDENT_CONSTITUTIONAL_REVIEW.md
?? src/runtime/MemoryAdmissionCoordinator.kt
?? tests/composition/ParkerRuntimeConversationalMemoryAdmissionCompositionTest.kt
?? tests/runtime/MemoryAdmissionCoordinatorTest.kt
```

Nothing staged, committed, or pushed.
