# Parker Conversational Memory Bridge — Admission Unit — Completion Review

## Status

Completion Review for the Parker Conversational Memory Bridge, Admission Unit — the first of two units this bridge requires (conversational retrieval, confirmed unbuilt, is a separate, later unit). Implements a governed path from an explicit owner "remember" instruction to a durable Memory Core write and a genuine attempt at Knowledge promotion, with strict response integrity. A genuine governance gap was found and closed via a narrow, independently-reviewed clarification before any Kotlin was written. A genuine, significant, pre-existing architectural gap was also discovered during mandatory live verification — not introduced by this Unit, not fixed by this Unit (outside its authorised scope), and disclosed prominently below.

---

## 1. Baseline

Branch `main`, `HEAD` `1955c03`, working tree clean — confirmed accurate.

---

## 2. Phase 1 — Repository and Governance Audit Findings

- **Owner-message path** (traced by direct reading, not assumed): `InteractiveConsole.runInteractiveConsole` → `ParkerRuntime.submitOwnerMessage` → `reasoningContextAssembler.assemble` → `conversationReplyCoordinator.submitAndDeliver` → `communicationConversationCoordinator.submitAndReason` → `ConversationTurnReasoningCoordinator.submitTurnAndReason` → `ReasoningProvider.reason` → `TaggedReasoningResponseParser.parse`.
- **Reasoning response types**: exactly `GOAL:`, `REPLY:`, `NOACTION` existed before this Unit. `Goal` routes to the Planner/Agent pipeline (one production Tool exists, `LocalTextChannelDeliverTool`, unrelated to memory); no structured action for remember/submit-knowledge/create-memory existed.
- **`KnowledgeSubmission` confirmed the correct existing admission interface** (`KnowledgeCandidate(evidenceReference, explicitlyRequested)` → `KnowledgeSubmission.submit`), already self-gated (`KNOWLEDGE_SUBMISSION_RESOURCE_ID`/`SUBMIT_ACTION_NAME`).
- **Direct `MemoryCore.create*` from Conversation was confirmed unreachable** — the only two existing writers (`EvidenceRegistrationCoordinator`, `EvidenceIntelligenceAcceptanceCoordinator`) are both evidence-artifact-rooted.
- **The decisive governance finding**: Contract Design V2 §16.10/§16.12 (already adopted, found by reading the current text) explicitly forbid promoting a Knowledge Candidate on explicit-request alone — a bare conversational statement has no legitimate confidence source to pair with it. Confirmed `GOVERNANCE INSUFFICIENT`.
- **Conversational retrieval confirmed a separate, unbuilt bridge** — `ParkerRuntime.kt`'s own `knowledgeRetrieval` field carries its own comment: "no production entry point consumes it yet... Programme 4's own, separately governed act." This determination was made explicit before implementation, per the task's own instruction, and this Unit does not touch it.

## 3. Phase 2 — Boundary Review

Confirmed the lawful architecture is `Conversation → explicit-memory-intent adapter → KnowledgeSubmission` (reusing the existing, self-gated boundary), not `Conversation → direct MemoryCore mutation`. The adapter's own necessary Memory Core write (creating the underlying Assertion `KnowledgeCandidate` must reference) is a new, but ordinarily and independently permission-gated, write path — mirroring `EvidenceRegistrationCoordinator`'s own precedent exactly, not a bypass.

## 4. Phase 3 — Governance Determination

```
GOVERNANCE INSUFFICIENT – NEW CLARIFICATION REQUIRED
```

Resolved by drafting the narrowest possible artifact: `docs/governance/PROGRAMME_3_EXPLICIT_OWNER_INSTRUCTION_PROMOTION_EXCEPTION_SCOPE_LOCK_CLARIFICATION.md`, mirroring the one existing precedent (the Contradiction exception) exactly — an express, Article-XI-conditioned single-factor promotion exception, narrowly scoped to a deterministically-recognised explicit owner instruction, always classified `EvidentialState.UNKNOWN`, never fabricating confidence or importance. Subjected to a genuine, adversarial Independent Constitutional Review (`docs/reviews/PROGRAMME_3_EXPLICIT_OWNER_INSTRUCTION_PROMOTION_EXCEPTION_INDEPENDENT_CONSTITUTIONAL_REVIEW.md`) before any Kotlin was written: **`ACCEPTED`**, no required correction.

---

## 5. Admission Architecture Selected

```
Owner message
    v
ConversationTurnReasoningCoordinator (unchanged)
    v
ReasoningProvider.reason -> ReasoningProviderResponse.Remember(text)   [new variant/tag, mirrors Goal/Reply's own intent-classification precedent]
    v
ConversationReplyCoordinator (new branch, mirrors the existing Goal-interception shape)
    v
MemoryAdmissionCoordinator (new; self-gated, mirrors EvidenceRegistrationCoordinator)
    |-- permissionEngine.evaluate (own gate: CONVERSATIONAL_MEMORY_RESOURCE_ID / "create conversational memory record")
    |-- memoryCore.createProvenance / memoryCore.createAssertion(confidence = null)   [the existing, durable, composed DurableMemoryCore]
    |-- knowledgeSubmission.submit(candidate with soleBasisIsExplicitInstruction = true)   [the existing, self-gated KnowledgeSubmission]
    v
MemoryAdmissionOutcome (Stored / NotStored / NotAuthorised)
    v
buildAdmissionReply -- fixed, deterministic mapping, never the model's own text
    v
ReplyDeliveryCoordinator.composeAndDeliver (unchanged, existing path)
```

Implementation Plan: `docs/implementation/CONVERSATIONAL_MEMORY_ADMISSION_IMPLEMENTATION_PLAN.md` (Unit-tier, not a full Contract Design, since the architecture was already fully determined by the Planning/Boundary Review).

## 6. Conversational Retrieval — Confirmed Not Built

Confirmed not built, and not touched by this Unit. **This is one implementation unit of two.** A second, separately-governed unit is required to wire `knowledgeRetrieval` into Reasoning Context/Conversation before the task's own "ultimate acceptance test" can be attempted at all.

---

## 7. Files Created

- `docs/governance/PROGRAMME_3_EXPLICIT_OWNER_INSTRUCTION_PROMOTION_EXCEPTION_SCOPE_LOCK_CLARIFICATION.md`
- `docs/reviews/PROGRAMME_3_EXPLICIT_OWNER_INSTRUCTION_PROMOTION_EXCEPTION_INDEPENDENT_CONSTITUTIONAL_REVIEW.md`
- `docs/implementation/CONVERSATIONAL_MEMORY_ADMISSION_IMPLEMENTATION_PLAN.md`
- `src/runtime/MemoryAdmissionCoordinator.kt` (also declares `MemoryAdmissionOutcome`)
- `tests/runtime/MemoryAdmissionCoordinatorTest.kt`
- `tests/composition/ParkerRuntimeConversationalMemoryAdmissionCompositionTest.kt`

## 8. Files Modified

- `src/interfaces/ReasoningProvider.kt` — new `ReasoningProviderResponse.Remember` variant.
- `src/runtime/ReasoningResponseParser.kt` — new `REMEMBER:` tag.
- `src/interfaces/KnowledgeStore.kt` — new `KnowledgeCandidate.soleBasisIsExplicitInstruction` field (additive, deliberately distinct from `explicitlyRequested`).
- `src/runtime/DefaultKnowledgeCandidateEvaluator.kt` — one new, additive promotion branch (checked after Contradiction, before the two-factor gate).
- `src/runtime/ConversationReplyCoordinator.kt` — one new branch, one new constructor parameter (`memoryAdmissionCoordinator`), the new `buildAdmissionReply` mapping function.
- `src/runtime/ReasoningPromptBuilder.kt` — one new, conservative selection-guidance instruction for `REMEMBER:`.
- `src/composition/ParkerRuntime.kt` — construct `memoryAdmissionCoordinator` (reusing the existing `memoryCore`/`knowledgeSubmission`/`permissionEngine`); one reordering of existing construction (moved `conversationReplyCoordinator`/`runtimeEventLogger` after `knowledgeSubmission`'s own construction, since the new dependency requires it to already exist); new resource/action registration (`CONVERSATIONAL_MEMORY_RESOURCE_ID`, already-APPROVED WRITE/MEMORY rule, no new `PermissionPolicyRule` needed).
- `src/composition/LoggingReasoningProvider.kt`, `src/runtime/DefaultEvidenceIntelligence.kt`, `src/runtime/ResponseComposer.kt` — each gained one new, exhaustiveness-required `when` branch for `Remember` (logging; a faulted "no lawful mapping," mirroring `Goal`'s own identical treatment; a defensive, structurally-unreachable "not a Reply," mirroring `Goal`/`NoAction`'s own identical treatment — respectively).
- Five test files updated for the new constructor parameter, the new prompt text, and the new sealed-variant exhaustiveness (`tests/contracts/ReasoningProviderContractTest.kt`, `tests/runtime/ConversationReplyCoordinatorTest.kt`, `tests/runtime/DefaultKnowledgeCandidateEvaluatorTest.kt`, `tests/runtime/ReasoningPromptBuilderTest.kt`, `tests/runtime/ReasoningResponseParserTest.kt`).

No file under Memory Core Durability's own governed scope was touched. No `EvidenceRegistrationCoordinator`/`EvidenceIntelligenceAcceptanceCoordinator` internal gating logic was touched.

---

## 9. Behavioural Changes

An explicit, unambiguous owner "remember" instruction is now recognised, and Parker genuinely attempts durable admission through the existing governed path rather than only producing a conversational claim. Every other conversation behaviour (`Goal`, ordinary `Reply`, `NoAction`) is unchanged — confirmed by the full, unmodified pre-existing regression suite continuing to pass.

**Response integrity is structurally enforced, not merely asserted**: `buildAdmissionReply` is a fixed, deterministic function with exactly three branches (`Stored` → "I'll remember that."; `NotStored`/`NotAuthorised` → an honest, basis-disclosing message) — the Reasoning Provider's own claimed text is never used for this branch, proven directly by a dedicated test using a model response designed to say something else entirely.

---

## 10. The Significant Pre-Existing Gap Discovered During Live Verification

Mandatory live/behavioural verification (real `ParkerRuntime`, real `ModelReasoningProvider` against a stub model server, real `DurableMemoryCore`, real `KnowledgeSubmission`) surfaced that **`KnowledgeSubmission.submit` can never successfully promote any candidate in the live, composed runtime, for any caller** — not specific to this Unit's own candidates. `DefaultKnowledgeCandidateEvaluator`, as actually composed in `ParkerRuntime.kt`, resolves a candidate's evidence through `permissionFilteredMemoryRetrieval`, which is unconditionally fail-closed: `RETRIEVE_ACTION_NAME`/`RETRIEVE_DOCUMENT_ACTION_NAME` are never registered in the Action Vocabulary, and — traced directly through `DefaultPermissionPolicy.evaluate` → `ActionMapper.mapOne` — registering them would not help, since Memory Core records are never Resource Registry entries (Errata 004's own architecture), so `targetResourceTypes` is structurally always the empty set for every retrieval request, regardless of vocabulary registration. This is not specific to this Unit's own construction: `EvidenceIntelligenceAcceptanceCoordinator` calls the identical `knowledgeSubmission.submit`, wired to the identical evaluator and the identical `permissionFilteredMemoryRetrieval` instance, so any candidate reaching that call would hit the identical resolution failure — confirmed structurally, by direct code trace, not merely by analogy. (Separately, and not investigated further here since it is outside this Unit's own scope: `EvidenceAnalysisResult.CandidateRecordProduced`, the variant that would carry such a candidate, is declared but never actually constructed anywhere in `src/` today, meaning Evidence Intelligence's own live reasoning pipeline does not currently reach this call at all either — a distinct, likely already-known gap, not conflated with the retrieval-gating finding above.) `ParkerRuntimeEvidenceIntelligenceCompositionTest`'s own existing, already-accepted test (`analyseEvidence's Memory Core retrieval remains fail-closed even for a record that genuinely exists`) independently corroborates that the same decorator denies unconditionally, confirming this is not new and not a defect in anything built here.

**This was investigated for a narrow, in-scope fix, at the user's own direction, and none exists.** The only ways to resolve it require changing `DefaultPermissionPolicy` or `ActionMapper`'s own frozen matching logic, or Memory Core's own Resource-representation choice (Errata 004) — each a genuine architectural change to frozen, already-verified Trust Framework components, squarely outside "new permission architecture," which this Unit's own governing task explicitly excludes.

**This Unit's own responsibility, correctly discharged**: recognise the instruction, genuinely attempt admission through the real, governed path, and disclose the real outcome honestly. This Unit does so correctly, proven by dedicated tests using a real, unfiltered `MemoryCore` (isolating `MemoryAdmissionCoordinator`'s own correctness from the separate, pre-existing gap) and by a live composition test proving the real, composed runtime reaches the real failure and discloses it honestly, never fabricating success. **The underlying gap remains open, blocks the task's own "ultimate acceptance test" regardless of this Unit's own correctness, and requires its own, separately-governed resolution before any future retrieval unit's own work could ever be meaningfully exercised end-to-end.**

---

## 11. Targeted Tests

- `ReasoningResponseParserTest`: `REMEMBER:` parsing tests added, all passing.
- `ReasoningProviderContractTest`: `Remember` blank/non-blank validation and sealed-exclusivity tests added, all passing.
- `DefaultKnowledgeCandidateEvaluatorTest`: five new tests for the `soleBasisIsExplicitInstruction` exception (Assertion, Entity, false-has-no-effect, independence from `explicitlyRequested`, Contradiction priority), all passing.
- `MemoryAdmissionCoordinatorTest` (new, 9 tests): success, confidence-never-fabricated, correlation-preserved, statement-unchanged, both denial paths (own gate, `KnowledgeSubmission`'s own separate gate) with a spy proving exactly when the Memory Core write does/does not happen, no-double-gating (exactly two `evaluate` calls), exception propagation. All passing.
- `ConversationReplyCoordinatorTest`: seven new tests for the `Remember` branch (routing, three outcome-to-reply mappings, response-integrity proof against a deliberately misleading model response, no-Goal-routing, exception propagation) plus two structural tests updated for the new constructor parameter. All passing (22/22 total in this file).
- `ParkerRuntimeConversationalMemoryAdmissionCompositionTest` (new, 3 tests): the real, live round trip proving honest failure disclosure, no-regression for ordinary `Reply`, and structural confirmation this Unit's own admission gate is genuinely reached and approves. All passing.

## 12. Full Repository Verification

`./gradlew clean test`: **BUILD SUCCESSFUL**, **1938 tests** (up from 1911 before this Unit), 0 skipped beyond the pre-existing 5, **0 failures, 0 errors**.

---

## 13. Explicit Non-Responsibilities Honoured

No automatic memory extraction, model-decided importance, background harvesting, summarisation memory, semantic consolidation, forgetting, pruning, new durability mechanism, new Memory Core persistence, or new permission architecture was implemented. World Model and Evidence Custodian's own code were not touched. Conversational retrieval was not built. Unit 9's own no-double-gating and self-gating discipline for `EvidenceRegistrationCoordinator`/`EvidenceIntelligenceAcceptanceCoordinator` was not altered.

---

## Recommended Next Step

Proceed to a genuine Independent Constitutional Review of this Unit's own implementation (distinct from the governance clarification's own, already-complete ICR).
