**Status:** Implementation Plan. No Kotlin is implemented, proposed as a diff, or changed by this document. This document does not amend `docs/governance/PROGRAMME_3_EXPLICIT_OWNER_INSTRUCTION_PROMOTION_EXCEPTION_SCOPE_LOCK_CLARIFICATION.md` ("the Clarification") or any other governance document. It exercises the deferrals that Clarification's own Section 11 explicitly reserved for this tier — the Kotlin representation, the recognition mechanism, and the underlying write's own permission classification — and fixes each one before implementation begins. Nothing is staged, committed, or pushed.

# Conversational Memory Admission — Implementation Plan

Programme: **Parker Conversational Memory Bridge — Admission (Unit 1 of 2; Retrieval is a separate, later Unit, not built by this Plan).**

---

## 1. Scope

**In scope:** one new conversational path from an explicit owner "remember" instruction to a durably persisted, promoted Knowledge Item, using the existing `KnowledgeSubmission` boundary and the new promotion exception the Clarification authorises.

**Out of scope, explicitly:** conversational retrieval (a separate, already-disclosed gap — `ParkerRuntime.kt`'s own `knowledgeRetrieval` field comment); automatic memory extraction from ordinary statements; model-decided importance or confidence; background memory harvesting; summarisation; forgetting/pruning; any new durability mechanism (this Plan writes through the existing, already-verified `DurableMemoryCore`/`KnowledgeSubmission` paths only); any change to `MemoryCore`/`MemoryRetrieval`/`DurableMemoryCore`/`FileSystemMemoryCoreDurabilityLog` (Memory Core Durability Programme, frozen); any change to `EvidenceRegistrationCoordinator`, `EvidenceIntelligenceAcceptanceCoordinator`, or their own permission gating.

---

## 2. Architecture (fixed by the Planning/Boundary Review, restated here for implementation)

```
InteractiveConsole
        v
ParkerRuntime.submitOwnerMessage
        v
ConversationTurnReasoningCoordinator (unchanged)
        v
ReasoningProvider.reason -> ReasoningProviderResponse.Remember(text)   [NEW variant]
        v
ConversationReplyCoordinator (new branch, mirroring the existing Goal branch exactly)
        v
MemoryAdmissionCoordinator (NEW)
        |-- permissionEngine.evaluate(create conversational memory record)   [self-gated, no PermissionGatedMemoryCore]
        |-- memoryCore.createProvenance / memoryCore.createAssertion(confidence = null)
        |-- knowledgeSubmission.submit(candidate with soleBasisIsExplicitInstruction = true)
        v
KnowledgeSubmissionDisposition (Promoted / Declined / NotAuthorised)
        v
deterministic, disposition-driven Reply text (never model-generated for this path)
        v
ReplyDeliveryCoordinator.composeAndDeliver (unchanged, existing path)
        v
ConversationOutcome.ReplyDelivered (unchanged shape)
```

This reuses `KnowledgeSubmission` (self-gated, no double gating), `MemoryCore.createProvenance`/`createAssertion` (a new, but ordinarily-gated, write path — not a bypass), `DurableMemoryCore` (unmodified), and the entire existing reply-composition/delivery chain (unmodified). No direct, ungoverned Memory Core mutation from Conversation is introduced.

---

## 3. New Types and Fields

### 3.1 `ReasoningProviderResponse.Remember`

```
data class Remember(val text: String) : ReasoningProviderResponse()
```
Same non-blank-text validation shape as `Goal`/`Reply`. `text` is the proposition the owner asked to be remembered — the model's own extraction of it, never the model's own judgment about its confidence, importance, or truth (Clarification Guarantee 2). Mirrors `Goal`'s own existing precedent exactly: the Reasoning Provider classifies intent; it never performs or authorises the resulting act.

### 3.2 `TaggedReasoningResponseParser` — one new tag

`REMEMBER:<text>` — parsed identically to `GOAL:`/`REPLY:` (trim, extract, construct). System-prompt instruction (Section 6, below) is what keeps this narrow in practice; the parser itself performs no semantic judgment, exactly as it performs none for the three existing tags.

### 3.3 `KnowledgeCandidate` — one new, additive field

```
data class KnowledgeCandidate(
    val evidenceReference: MemoryCoreRecordReference,
    val explicitlyRequested: Boolean? = null,
    val soleBasisIsExplicitInstruction: Boolean? = null,   // NEW
)
```

**Deliberately distinct from `explicitlyRequested`, not a reuse of it.** `explicitlyRequested` continues to serve exactly its existing role in the ordinary two-factor gate (Contract Design V2 §16), unaffected. `soleBasisIsExplicitInstruction` is the Clarification's own Guarantee 1 trigger: true only where the referenced evidence exists *specifically and only* to record an explicit remember instruction — a distinction the existing single boolean cannot carry, since a candidate built from Evidence Intelligence's own pipeline could legitimately set `explicitlyRequested = true` without qualifying for this narrow exception. Keeping them separate is what makes Clarification Guarantee 5 ("no effect on the ordinary gate" for any other candidate) hold structurally, not merely by convention. `MemoryAdmissionCoordinator` (Section 4) is the only production caller that ever sets it `true`; every other existing construction site (`EvidenceIntelligenceInputResolver`, wherever a `KnowledgeCandidate` is built today) leaves it at its default `null`, unchanged.

### 3.4 `DefaultKnowledgeCandidateEvaluator` — one new, additive branch

Inserted before the existing two-factor gate, after the existing Contradiction check (so an unresolved contradiction is still disclosed honestly even for an explicit-instruction candidate — Clarification Section 8 does not exempt this exception from Contradiction's own, separate, mandatory disclosure):

```
if (candidate.soleBasisIsExplicitInstruction == true) {
    return promote(..., state = EvidentialState.UNKNOWN,
        basis = "promoted solely because of an explicit, deterministic owner instruction to remember this; " +
            "no independent evidential weight was established")
}
```

No other line of the existing evaluator changes. The existing two-factor gate remains exactly as it is today for every candidate this new branch does not intercept.

### 3.5 `MemoryAdmissionCoordinator` (new class, mirrors `EvidenceRegistrationCoordinator`'s own shape)

```
class MemoryAdmissionCoordinator(
    private val memoryCore: MemoryCore,
    private val knowledgeSubmission: KnowledgeSubmission,
    private val permissionEngine: PermissionEngine,
)
```

One method, `admit(requestingPrincipalId, correlationId, instructionText): MemoryAdmissionOutcome`. Sequence:
1. Self-gated permission check (`permissionEngine.evaluate`, own `ExecutionRequest`, resource/action from Section 5) — mirrors `EvidenceRegistrationCoordinator.register`'s own identical two-stage-check shape exactly. Denied -> `MemoryAdmissionOutcome.NotAuthorised`, no Memory Core write attempted.
2. `memoryCore.createProvenance(requestingPrincipalId, CandidateProvenance(sourceIdentifier = correlationId, sourceType = "conversation", acquisitionTime = Instant.now(), contentNature = ContentNature.ORIGINAL))`.
3. `memoryCore.createAssertion(requestingPrincipalId, CandidateAssertion(statement = instructionText, provenanceId = provenance.provenanceId, confidence = null))` — `confidence` is always `null`; never fabricated (Clarification Guarantee 6; Article XIV).
4. `knowledgeSubmission.submit(requestingPrincipalId, KnowledgeCandidate(evidenceReference = MemoryCoreRecordReference.ToAssertion(assertion.assertionId), soleBasisIsExplicitInstruction = true))`.
5. Map `KnowledgeSubmissionDisposition` to `MemoryAdmissionOutcome`: `Promoted` -> `MemoryAdmissionOutcome.Stored`; `Declined` -> `MemoryAdmissionOutcome.NotStored(basis)`; `NotAuthorised` -> `MemoryAdmissionOutcome.NotAuthorised(reason)`.

A thrown exception at step 2 or 3 (e.g. a genuine Memory Core fault) propagates unchanged — this class performs no `try`/`catch`, exactly mirroring `EvidenceRegistrationCoordinator`'s own established discipline; `ConversationReplyCoordinator`'s own caller (`ParkerRuntime.submitOwnerMessage`) already has a general fault-handling path (`ParkerRuntimeOutcome.Failed`) for exactly this case.

**No double gating.** `memoryCore`/`knowledgeSubmission` are the existing, raw, already-composed `durableMemoryCore` instance and the existing `knowledgeSubmission` instance `ParkerRuntime.kt` already constructs — the same instances `EvidenceRegistrationCoordinator`/`EvidenceIntelligenceAcceptanceCoordinator` already hold, reused, never duplicated. `knowledgeSubmission.submit` already self-gates its own call (existing `KNOWLEDGE_SUBMISSION_RESOURCE_ID`/`SUBMIT_ACTION_NAME` check) — this class's own permission check (step 1) gates only the *write* it performs itself (the Provenance/Assertion creation), never re-evaluates or duplicates the submission's own, separate, already-existing gate.

### 3.6 Permission proposal class for the new write

New `ResourceId`/action pair, registered in `ParkerRuntime.kt`'s own existing vocabulary-registration block, exactly like every prior coordinator's own resource/action:
- `CONVERSATIONAL_MEMORY_RESOURCE_ID = ResourceId("resource-conversational-memory")`
- `CREATE_CONVERSATIONAL_MEMORY_ACTION_NAME = "create conversational memory record"`

**Classification:** a PermissionEngine proposal class, per Chapter 10 §3's own unambiguous, direct example ("writing... a durable record") — no CDR-005 self-certification document is required for this determination, exactly as the Clarification's own Section 11/§8 of its own Independent Constitutional Review already establishes; this is routine, not contested.

### 3.7 `ConversationReplyCoordinator` — one new branch, one new constructor parameter

```
class ConversationReplyCoordinator(
    private val communicationConversationCoordinator: CommunicationConversationCoordinator,
    private val replyDeliveryCoordinator: ReplyDeliveryCoordinator,
    private val goalPlanningHandoffCoordinator: GoalPlanningHandoffCoordinator,
    private val memoryAdmissionCoordinator: MemoryAdmissionCoordinator,   // NEW
)
```

`submitAndDeliver`'s own `when (response)` gains one new branch, mirroring the existing `Goal` branch's own interception shape exactly:

```
is ReasoningProviderResponse.Remember ->
    deliverReply(message, buildAdmissionReply(memoryAdmissionCoordinator.admit(message.senderPrincipalId, message.correlationId.value, response.text)))
```

`buildAdmissionReply` is a small, deterministic, private function mapping `MemoryAdmissionOutcome` to a fixed `ReasoningProviderResponse.Reply` — **never the model's own text for this path** (Section 6, below, "Response Integrity"). The mapped `Reply` is then passed through `deliverReply`/`replyDeliveryCoordinator.composeAndDeliver` completely unchanged from the existing `Reply`/`NoAction` path — `ResponseComposer`, `ResponseDelivery`, and `LocalTextChannelDeliverTool` are none of them modified by this Plan.

---

## 4. `MemoryAdmissionOutcome` (new sealed type)

```
sealed class MemoryAdmissionOutcome {
    data class Stored(val item: KnowledgeItem) : MemoryAdmissionOutcome()
    data class NotStored(val basis: String) : MemoryAdmissionOutcome()
    data class NotAuthorised(val reason: String) : MemoryAdmissionOutcome()
}
```

---

## 5. Response Integrity (mandatory, restated from the task's own instruction)

`buildAdmissionReply` is a pure, deterministic function — no branch of it ever invokes a Reasoning Provider or otherwise produces text the governed outcome does not itself justify:

- `Stored` -> `Reply("I'll remember that.")`
- `NotStored(basis)` -> `Reply("I wasn't able to store that: $basis")`
- `NotAuthorised(reason)` -> `Reply("I'm not able to store that right now: $reason")`

No other branch, phrasing, or fallback exists. This is the structural mechanism that satisfies the task's own mandatory requirement: "Parker must only say... 'I've stored that'... after the governed persistence path reports actual successful acceptance." The reply is never composed until `MemoryAdmissionCoordinator.admit` has already returned a real, governed outcome.

---

## 6. System Prompt (`DefaultReasoningPromptBuilder`) — narrow, disclosed instruction

One new instruction added alongside the existing `GOAL:`/`REPLY:`/`NOACTION` instructions: emit `REMEMBER:<proposition>` **only** where the owner has given a direct, unambiguous instruction to remember a specific, stated proposition (for example, "Remember that X," "Please remember X," "Don't forget that X") — never for an ordinary statement of fact, an incidental mention, or a question. Where any doubt exists whether the owner intended an instruction, use `REPLY:` (optionally asking a clarifying question) or `NOACTION`, never `REMEMBER:`. This is the deterministic-recognition boundary Clarification Guarantee 2 requires; it is a narrowing instruction on top of the same intent-classification mechanism already trusted for `GOAL:`/`REPLY:`, not a new kind of authority.

---

## 7. Composition (`ParkerRuntime.kt`)

`memoryAdmissionCoordinator` constructed once, alongside `evidenceRegistrationCoordinator`, reusing the existing `memoryCore` (the durable instance) and `knowledgeSubmission` (the existing local `val`, now also passed here in addition to `evidenceIntelligenceAcceptanceCoordinator`) — no second `KnowledgeSubmission` instance, no second `DurableMemoryCore`. `conversationReplyCoordinator`'s own construction call gains the one new argument. New resource/action registered alongside the existing five Evidence Custodian entries, same block.

---

## 8. Testing Requirements

Per the task's own list, at minimum: explicit "remember" instruction recognised and routes to `Remember`; ordinary statement does not route to `Remember` (prompt-level, verified by `TaggedReasoningResponseParser`'s own unit tests continuing to reject anything not exactly matching a known tag, and by `MemoryAdmissionCoordinator` never being reachable except via that branch); question does not persist; malformed/ambiguous request (blank `text`) rejected by `Remember`'s own constructor validation, mirroring `Goal`/`Reply`; successful request reaches `KnowledgeSubmission.submit` with `soleBasisIsExplicitInstruction = true`; permission denial prevents persistence and produces the `NotAuthorised` reply; submission rejection (a contrived case where the evaluator still declines) prevents a false "stored" reply; a genuine Memory Core fault propagates rather than being swallowed into a false success reply; success reply text occurs only after a real `Promoted` disposition; no duplicate permission gating (only one `permissionEngine.evaluate` call per admission attempt beyond `KnowledgeSubmission`'s own existing, separate self-gate); no direct Memory Core bypass (`MemoryAdmissionCoordinator` is the only new caller of `memoryCore.createProvenance`/`createAssertion` from the conversation path); correlation ID from the inbound message is preserved into the `CandidateProvenance.sourceIdentifier`; a duplicate identical explicit request behaves according to `DefaultKnowledgeCandidateEvaluator`'s own existing idempotence semantics (unaffected by this Plan); unrelated conversation behaviour (`Goal`/`Reply`/`NoAction`) does not regress — existing `ConversationReplyCoordinatorTest`-equivalent suites continue to pass unchanged for those three branches.

Full round-trip retrieval (teach, restart, retrieve) is explicitly **not** tested by this Plan — conversational retrieval does not exist yet (Section 1).

---

## 9. Files Expected to Change

- `src/interfaces/ReasoningProvider.kt` — new `Remember` variant.
- `src/runtime/ReasoningResponseParser.kt` — new `REMEMBER:` tag.
- `src/interfaces/KnowledgeStore.kt` — new `KnowledgeCandidate.soleBasisIsExplicitInstruction` field.
- `src/runtime/DefaultKnowledgeCandidateEvaluator.kt` — one new, additive promotion branch.
- `src/runtime/MemoryAdmissionCoordinator.kt` — new file.
- `src/interfaces/` or `src/runtime/` — new `MemoryAdmissionOutcome` sealed type (co-located with `MemoryAdmissionCoordinator`, mirroring `EvidenceRegistrationOutcome`'s own co-location precedent).
- `src/runtime/ConversationReplyCoordinator.kt` — one new branch, one new constructor parameter.
- `src/runtime/DefaultReasoningPromptBuilder.kt` — one new instruction.
- `src/composition/ParkerRuntime.kt` — construct `memoryAdmissionCoordinator`; register new resource/action/policy rule; wire the new constructor argument.
- Corresponding test files for each.

No file under Memory Core Durability's own governed scope (`src/runtime/DurableMemoryCore.kt`, `FileSystemMemoryCoreDurabilityLog.kt`, `MemoryCoreRecovery.kt`, `InMemoryMemoryCore.kt`) is touched.

---

## 10. Stop Condition

If implementation reveals that `KnowledgeCandidate`'s existing constructor, `KnowledgeSubmissionDisposition`, or any Memory Core Durability file requires modification beyond what Section 9 lists, stop and report — this Plan does not authorise touching the durability boundary.
