**Status:** Proposed Architecture Decision. Governance only. This document does not modify or authorize immediate modification of Kotlin or tests. It is subject to the accompanying Independent Constitutional Review and explicit acceptance. Gap #54 Operationalisation Unit 4 remains paused. Nothing is staged, committed, or pushed.

# Trust Framework — Knowledge Candidate Accountable Principal Architecture Decision

## 1. Authority and question

This decision resolves the identity prerequisite exposed by Gap #54 Memory Retrieval Operationalisation Unit 4. It is subordinate to the Parker Constitution; Chapters 9, 10, 41 and 42; `IdentityService.md`; Memory Core Errata 004; the adopted Authorization Context Contract Design; the accepted Authorization Purpose Programme, Scope Lock and Units 1–6; Programme 3 Knowledge Candidate/Submission governance; and the accepted Gap #54 Operationalisation Scope Lock and Implementation Plan.

The question is which Principal is accountable when `DefaultKnowledgeCandidateEvaluator` resolves the Memory Core evidence referenced by a candidate, and how that Principal reaches the retrieval request without being replaced by a subsystem identity or conflated with Authorization Purpose.

## 2. Frozen constitutional model

The four authorization dimensions remain independent:

- Principal answers **who is accountable**;
- Authorization Purpose answers **why this act is requested**;
- Action answers **what act is proposed**; and
- Resource answers **what is acted upon**.

Authorization Purpose is additive to Principal and never substitutes for it. Every Memory operation carries an explicit requesting Principal; no ambient identity, caller inference or fixed component identity may erase the real accountable actor. Identity recognition is necessary but does not itself grant permission.

The candidate retrieval Purpose remains exactly `knowledge-memory.candidate-evaluation`.

## 3. Fresh accountability trace

| Boundary | Current input | Principal information | Authority and survival |
|---|---|---|---|
| Conversation owner message | `InboundOwnerMessage.senderPrincipalId` | Real registered owner | Authoritative inbound identity; supplied into conversation/admission. |
| `MemoryAdmissionCoordinator.admit` | `requestingPrincipalId`, correlation, instruction | Same owner | KDoc and implementation require unchanged propagation to its permission request, Memory Core writes and `KnowledgeSubmission.submit`. |
| Evidence Intelligence acceptance | `dispatch(requestingPrincipalId, results)` | Real invocation Principal | `dispatchKnowledge` passes it unchanged to `KnowledgeSubmission.submit`. |
| Other lawful producer | `KnowledgeSubmission.submit(requestingPrincipalId, candidate)` | Caller must explicitly supply accountable Principal | Public contract already requires it and forbids ambient derivation. |
| `KnowledgeCandidate` | Evidence reference and two submission-context facts | Deliberately no Principal | Programme 3 Unit 8 explicitly freezes Principal as a separate submission parameter, never a candidate field. |
| `DefaultKnowledgeSubmission.submit` | `requestingPrincipalId`, candidate | Principal remains present and is used for Evaluation B | Authoritative after the submission gate; survives until evaluator invocation. |
| `KnowledgeCandidateEvaluator.evaluate` | Candidate only | Principal absent | **Loss point.** The interface cannot receive the Principal that `DefaultKnowledgeSubmission` still holds. |
| `DefaultKnowledgeCandidateEvaluator` | Candidate plus fixed constant | Substitutes `system.knowledge-memory` | Ungoverned and constitutionally incorrect; the fixed value is not the originating accountable actor and is not registered. |
| Purpose-bound `PermissionFilteredMemoryRetrieval` | Retrieval method Principal plus fixed candidate Purpose | Whatever Principal evaluator supplies | Correct dual carrier: method argument supplies accountable Principal; immutable view supplies Purpose. |
| `ExecutionRequest` | Principal, Purpose, exact verb and other fields | Both dimensions explicit | One engine validates identity, then one policy evaluates Purpose/action/resource. |

Accountability is therefore not missing at candidate creation or submission. It is discarded at one invocation boundary despite remaining available to the submitting implementation.

## 4. Accountable Principal decision

```text
A — the real Principal supplied to KnowledgeSubmission.submit
```

Candidate evaluation is an internal processing step in the already-attributed Knowledge Submission act. It is not an autonomous root actor. Its evidence lookup is accountable to the same real Principal whose submission passed Evaluation B.

`DefaultKnowledgeSubmission` must pass its `requestingPrincipalId` unchanged into candidate evaluation. The evaluator must pass that value unchanged into every direct lookup and every relationship query it performs. It must not resolve, translate, replace, register or mutate the Principal.

This remains true for conversational admission, Evidence Intelligence-produced candidates, and every other lawful producer: each producer already supplies its own real accountable Principal to the single Knowledge Submission boundary.

## 5. Minimum lawful carrier

The `KnowledgeCandidateEvaluator` invocation contract is narrowly amended from:

```kotlin
fun evaluate(candidate: KnowledgeCandidate): KnowledgeCandidateEvaluation
```

to the conceptual shape:

```kotlin
fun evaluate(
    requestingPrincipalId: PrincipalId,
    candidate: KnowledgeCandidate,
): KnowledgeCandidateEvaluation
```

Exact formatting and parameter order may follow repository conventions, but both strongly typed parameters are mandatory.

`DefaultKnowledgeSubmission.submit` must call the evaluator exactly once, after Evaluation B approval, with the identical `requestingPrincipalId` it received. No other sequence or disposition changes.

`DefaultKnowledgeCandidateEvaluator` must use only that supplied value for all Memory Retrieval calls and query objects. Its fixed `SYSTEM_PRINCIPAL_ID` must be removed from the live candidate evaluator.

This is the smallest lawful carrier because:

- adding Principal to `KnowledgeCandidate` contradicts the Unit 8 contract's explicit separate-parameter decision and would mix evidence payload with invocation accountability;
- constructor binding would incorrectly freeze one Principal into a reusable evaluator and would fail for multiple callers;
- changing `MemoryRetrieval` is unnecessary because it already requires Principal on every method/query;
- changing the purpose-bound view is unnecessary because it already supplies the separate immutable Purpose;
- global, thread-local, metadata, singleton or inferred context is unconstitutional; and
- `DefaultKnowledgeSubmission` already owns the exact value at the exact loss point.

The public evaluator signature is a frozen contract and therefore may change only in the separately governed prerequisite implementation unit authorized by this decision after acceptance. No other `KnowledgeCandidate` or Knowledge Submission contract field changes.

## 6. `system.knowledge-memory` disposition

`system.knowledge-memory` is retired entirely from **live candidate evaluation**. It must not be registered merely to cross the identity gate, and it must not be retained as candidate evaluator fallback.

No lawful autonomous root-system role for it is established by current governance. This decision therefore does not assign it a PrincipalType, lifecycle, owner, credentials or policy authority.

`DefaultKnowledgeRevisionEvaluator` is dormant and excluded from the prerequisite implementation unit. Its identical constant may remain temporarily in unreachable source only as a disclosed unresolved future-governance issue; it gains no registration or authority. Before revision evaluation is ever composed, separate governance must determine and implement accountable-Principal propagation and remove any fixed-identity substitution there. Dormancy is not acceptance of the pattern.

No other `system.*` root identity is changed. Existing registered system Principals that genuinely identify autonomous accountable actors remain outside scope.

## 7. Security and non-widening invariants

The prerequisite implementation must prove:

1. the evaluator receives exactly the Principal supplied to `KnowledgeSubmission.submit`;
2. the same value appears on every candidate direct lookup and relationship query;
3. no fixed `system.knowledge-memory` value remains in live candidate evaluation;
4. candidate Purpose remains exactly `knowledge-memory.candidate-evaluation` and cannot be selected by the Principal;
5. an unregistered, `CREATED`, suspended, revoked or archived originating Principal still fails closed through the unchanged engine;
6. changing Principal cannot create authority beyond the exact candidate Purpose-and-verb rules;
7. Evidence Intelligence continues to propagate its real request Principal with `evidence-intelligence.input-resolution` and remains denied;
8. no root Principal is registered and no existing coarse rule becomes reachable for a newly activated identity;
9. no alternate engine, policy, registry, retrieval decorator or direct Memory Core route is added; and
10. omission or loss of the Principal cannot fall back to a system identity.

The evaluator does not validate identity itself. It carries the value to the existing `PermissionFilteredMemoryRetrieval` → `DefaultPermissionEngine` path, which remains the sole identity and authorization authority.

## 8. Prohibited mechanisms

Implementation must not use:

- global mutable current-Principal state;
- thread-local/coroutine-local ambient identity;
- stack inspection or reflection;
- caller-class, method or runtime-type discrimination;
- hard-coded Principal substitution or fallback;
- Principal derivation from Authorization Purpose;
- Purpose derivation from Principal;
- free-text intent or metadata as identity;
- a new Principal, identity registry, engine, policy or retrieval route; or
- direct/unfiltered Memory Core access.

## 9. Prerequisite implementation authority

### 9.1 Exact production surface expected

Only:

- `src/interfaces/KnowledgeStore.kt` — amend `KnowledgeCandidateEvaluator.evaluate` to require `PrincipalId` separately from `KnowledgeCandidate` and update directly inaccurate KDoc;
- `src/runtime/DefaultKnowledgeSubmission.kt` — pass the existing `requestingPrincipalId` unchanged into the evaluator invocation and update directly inaccurate KDoc; and
- `src/runtime/DefaultKnowledgeCandidateEvaluator.kt` — accept/use the supplied Principal for every retrieval and remove the live fixed constant/KDoc.

No `ParkerRuntime`, `MemoryAdmissionCoordinator`, `KnowledgeCandidate`, `KnowledgeSubmission.submit`, `MemoryRetrieval`, `PermissionFilteredMemoryRetrieval`, Permission Engine, Identity Service, registry, Purpose, Memory Core, persistence, promotion criterion, Evidence Intelligence or Knowledge Retrieval production change is authorized.

### 9.2 Exact test surface expected

Required direct changes:

- `tests/runtime/DefaultKnowledgeCandidateEvaluatorTest.kt` — adapt invocation and prove exact Principal propagation across every supported lookup/query path, with no fixed fallback;
- `tests/runtime/DefaultKnowledgeSubmissionTest.kt` — adapt evaluator fixtures and prove the submitted Principal is forwarded exactly once only after Evaluation B approval.

Required unchanged or narrowly extended verification where necessary:

- `tests/runtime/MemoryAdmissionCoordinatorTest.kt` — prove the owner Principal remains unchanged through submission;
- `tests/runtime/EvidenceIntelligenceAcceptanceCoordinatorTest.kt` — prove its real requesting Principal remains unchanged through submission;
- `tests/composition/ParkerRuntimeMemoryRetrievalOperationalisationCompositionTest.kt` — after Unit 4 resumes, prove genuine candidate evidence resolves through the real composed engine using the registered originating Principal and candidate Purpose;
- `tests/composition/ParkerRuntimeEvidenceIntelligenceCompositionTest.kt` — retain real-principal plus distinct-Purpose denial;
- `tests/composition/ParkerRuntimeConversationalMemoryAdmissionCompositionTest.kt` — regression only; promotion closure remains Unit 5 and must not be implemented in the prerequisite unit.

Only tests genuinely requiring adaptation to the evaluator signature may change. No unrelated test correction is authorized.

## 10. Unit 4 impact and sequencing

```text
B — a new prerequisite implementation unit must be completed before Unit 4 can resume
```

The carrier change crosses Unit 4's frozen one-production-file policy-content boundary and changes a protected public evaluator contract. It must therefore be implemented, targeted/adversarially verified, Completion Reviewed and independently constitutionally accepted as its own prerequisite unit.

After that prerequisite is accepted, Unit 4 may resume from its preserved working tree without redesigning its two candidate policy rules. The policy rules themselves remain constitutionally correct. Unit 4's real-consumer test must then use a genuine registered originating Principal rather than registering or relying on `system.knowledge-memory`.

Unit 5 remains prohibited until Unit 4 subsequently completes and is accepted.

## 11. Acceptance criteria

This decision may be accepted only if independent review confirms that:

- it selects the originating submission Principal rather than a subsystem identity;
- the carrier closes the one observed loss point without contaminating `KnowledgeCandidate`;
- Authorization Purpose remains independent and immutable at composition;
- no new or activated Principal becomes eligible for coarse approvals;
- dormant revision evaluation is honestly excluded and blocked from future composition without governance;
- Evidence Intelligence identity and Purpose boundaries remain unchanged;
- the single authority path and fail-closed identity validation remain intact; and
- Unit 4 remains paused pending a separately accepted prerequisite implementation unit.

## 12. Decision

The real `KnowledgeSubmission.submit` Principal is the accountable Principal for candidate-evidence retrieval. The evaluator invocation contract is the sole lawful carrier gap. `system.knowledge-memory` has no live candidate-evaluation role and is not registered. A separate prerequisite carrier unit must complete before Unit 4 resumes.
