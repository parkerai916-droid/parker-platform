**Status:** Unit 3 Planning Review — PASS. Conducted against baseline `061a006`, the accepted Scope Lock and current accepted Implementation Plan before Unit 3 Kotlin or test changes. No Boundary Review is required because the accepted private purpose-bound view fits the two authorized production files without changing consumers or the public `MemoryRetrieval` contract. Nothing is staged, committed, or pushed.

# Gap #54 Memory Retrieval Operationalisation — Unit 3 Consumer Purpose Propagation — Planning Review

## 1. Fresh boundary trace

`PermissionFilteredMemoryRetrieval` currently owns the raw retrieval delegate, one Permission Engine and all `ExecutionRequest` construction. Both consumers receive that same object through the existing `MemoryRetrieval` interface. The interface has ten methods and no Purpose field. Both consumer constructors already accept `MemoryRetrieval`; neither needs modification.

Unit 2 production contains the two active real Purpose values and exact verb-specific `DENIED` guards. No Purpose-specific rule exists.

## 2. Authorized implementation

Only:

- `src/composition/PermissionFilteredMemoryRetrieval.kt`; and
- `src/composition/ParkerRuntime.kt`

may change.

The parent decorator will expose a composition-internal factory producing a private immutable `MemoryRetrieval` view. The view holds only its parent and one `AuthorizationPurposeId`; it implements all ten interface methods by delegating to private parent operations with that fixed Purpose. It has no engine, policy, registry, raw Memory Core reference or decision logic.

The existing unbound methods delegate through the same private operations with `null`, preserving all existing callers and tests. The parent alone continues to retrieve, construct requests, evaluate permission and filter results.

## 3. Composition binding

`ParkerRuntime` will create exactly two views from its one parent decorator:

- candidate view → `knowledge-memory.candidate-evaluation`;
- Evidence Intelligence view → `evidence-intelligence.input-resolution`.

Each view is constructed once, held only by its designated consumer and never exposed through a public runtime API. Consumer constructors and public contracts remain unchanged. No caller identity or dynamic inference participates.

## 4. Fail-closed proof

Unit 3 changes request data only. Unit 2 guards remain the unique maximally specific applicable rules because Unit 4 Purpose-plus-verb approvals do not exist. Requests carrying either active real Purpose therefore remain `DENIED`. The unbound parent continues sending `null` Purpose and also remains denied.

No observable runtime state can approve: the fixed policy is built before either view, and both views enter the same parent/engine/policy path.

## 5. Verification boundary

Modify only the Unit 3 test surface named by the accepted Plan:

- `PermissionFilteredMemoryRetrievalTest.kt`;
- `ParkerRuntimeMemoryRetrievalOperationalisationCompositionTest.kt`;
- `ParkerRuntimeEvidenceIntelligenceCompositionTest.kt`;
- `ParkerRuntimeMemoryCoreDurabilityCompositionTest.kt` where direct-parent structural assumptions become obsolete;
- `ParkerRuntimeAuthorizationPurposeCompositionTest.kt` where non-adoption assertions become obsolete; and
- the unchanged conversational admission suite for real candidate denial.

Behavioral tests must capture exact `ExecutionRequest.authorizationPurpose` for direct and query methods, prove immutable/cross-binding properties, confirm the unbound parent remains absent-Purpose, trace both views to one parent, and verify real candidate and Evidence Intelligence denial.

## 6. Boundary Review determination

No separate Boundary Review is required. If implementation would require a public `MemoryRetrieval` change, consumer change, second decorator, policy change or registry/engine contract change, stop rather than expand.

## 7. Planning verdict

```text
PASS
```

Unit 3 may proceed. Unit 4 remains prohibited.
