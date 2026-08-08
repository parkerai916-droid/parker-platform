**Status:** Unit 4 Boundary Review — BLOCKED. A pre-existing production identity-composition dependency prevents the mandatory real-candidate verification. This review grants no authority to expand Unit 4. Completion Review and Independent Constitutional Review must not proceed until governance resolves the boundary. Nothing is staged, committed, or pushed.

# Gap #54 Memory Retrieval Operationalisation — Unit 4 Candidate-Only Policy Authority — Boundary Review

## 1. Trigger

The Unit 4 Planning Review found that the two frozen rules fit `ParkerRuntime.kt` without contract changes. Implementation added exactly those rules and the direct production authorization matrix passed. Mandatory real-consumer verification then exercised a genuine Memory Core assertion through the real composed `DefaultKnowledgeCandidateEvaluator`.

The evaluator returned `KnowledgeCandidateEvaluation.Reject` because its initial evidence lookup was denied. Direct source trace identifies the hidden dependency: `DefaultKnowledgeCandidateEvaluator` always requests retrieval as `PrincipalId("system.knowledge-memory")`, while `ParkerRuntime` never registers that Principal with the production Identity Service. `DefaultPermissionEngine` therefore denies before the candidate-specific policy rule can authorize the request.

## 2. Evidence

- `src/runtime/DefaultKnowledgeCandidateEvaluator.kt` defines `SYSTEM_PRINCIPAL_ID = PrincipalId("system.knowledge-memory")` and supplies it to every direct and query-shaped `MemoryRetrieval` call.
- `src/composition/ParkerRuntime.kt` contains no `system.knowledge-memory` Principal registration.
- The direct Unit 4 production-policy/full-engine matrix using the already-registered owner Principal approves both exact candidate-purpose verbs and denies every other tested purpose state.
- The targeted run compiled and executed 98 tests: 97 passed and the sole failure was the new genuine candidate-evaluator composition test, which received `Reject` rather than `Promote` because the referenced record could not be resolved.

This is not repaired by changing rule specificity or adding another approval. Identity eligibility precedes policy outcome in the one authoritative `DefaultPermissionEngine` path.

## 3. Boundary classification

The frozen Unit 4 Implementation Plan §8 defines the implementation boundary as adding exactly the two Scope Lock §4.6 rules. Its authorized production file is `ParkerRuntime.kt`, but authorization to edit that file is responsibility-specific; it does not silently authorize unrelated Principal-vocabulary adoption.

Registering `system.knowledge-memory` would be new production identity-composition content. The accepted Unit 4 governance does not name that Principal, define its immutable meaning/lifecycle, specify registration, or include verification of its effect on other acts. Treating the file boundary alone as authority would bypass the frozen policy-content boundary.

The issue is therefore classified as a **PRE-EXISTING ARCHITECTURAL/COMPOSITION BLOCKER REQUIRING GOVERNANCE CLARIFICATION**, not as a defect in the two Unit 4 policy rules and not as an environmental failure.

## 4. Required governance decision

Before Unit 4 can complete, authoritative governance must determine one of the following without redesigning the accepted single path:

1. explicitly authorize production registration of the existing `system.knowledge-memory` Principal, with its exact type/status and bounded composition/test surface; or
2. identify an already-governed registered Principal that the evaluator is constitutionally required to use and separately authorize the necessary consumer change (which is outside current Unit 4 and protected by its instruction).

No direct Memory Core bypass, identity shortcut, fake engine, consumer-local approval, broader rule, or test substitution can satisfy the mandatory real-consumer proof.

## 5. Boundary verdict

```text
BLOCKED — GOVERNANCE CLARIFICATION REQUIRED
```

Unit 4 Completion Review and Independent Constitutional Review cannot honestly be produced. Unit 5 has not started. The exact policy implementation and tests remain uncommitted for inspection; no additional production change has been made.
