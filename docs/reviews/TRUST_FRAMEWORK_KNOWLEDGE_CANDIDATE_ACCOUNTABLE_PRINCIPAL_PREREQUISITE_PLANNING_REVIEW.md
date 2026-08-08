**Status:** Prerequisite Planning Review — PASS. Conducted against `03f0223`, the explicitly accepted Accountable Principal Architecture Decision and its accepted Independent Constitutional Review, accepted Gap #54 governance and Units 1–3, and the preserved paused Unit 4 working tree before prerequisite implementation. No Boundary Review is required. Nothing is staged, committed, or pushed.

# Knowledge Candidate Accountable Principal Prerequisite — Planning Review

## 1. Scope reconstruction

The prerequisite closes one carrier loss point only. `KnowledgeSubmission.submit` already receives and validates the real accountable `requestingPrincipalId`, but `DefaultKnowledgeSubmission` currently calls a candidate-only evaluator signature. `DefaultKnowledgeCandidateEvaluator` consequently substitutes fixed `system.knowledge-memory` for all evidence and relationship retrieval.

The accepted decision authorizes an explicit Principal parameter on evaluator invocation, unchanged forwarding by submission, and unchanged forwarding by the evaluator into its existing `MemoryRetrieval` dependency. Purpose remains fixed by Unit 3's composition-bound view.

## 2. Preserved Unit 4 state

Before prerequisite implementation, the four Unit 4 diff fingerprints were recorded:

- `c3252073ec114677876844f2c04e90f437d026ff` — `src/composition/ParkerRuntime.kt`;
- `25060a09fdebbaf2abf6397c6e757d017d719c24` — `tests/composition/ParkerRuntimeAuthorizationPurposeCompositionTest.kt`;
- `d4f7840705fc689e0c13aa823663624aff5f5088` — `tests/composition/ParkerRuntimeEvidenceIntelligenceCompositionTest.kt`;
- `1ebca9292b14afdeee6e2debfd38ae03ee60bdd2` — `tests/composition/ParkerRuntimeMemoryRetrievalOperationalisationCompositionTest.kt`.

These fingerprints are review evidence, not prerequisite scope. The prerequisite must not alter any of those diffs. Existing Unit 4 approval behavior may be observed but cannot be accepted by this unit.

## 3. Authorized production implementation

Only:

1. `src/interfaces/KnowledgeStore.kt` — require `PrincipalId` on `KnowledgeCandidateEvaluator.evaluate`;
2. `src/runtime/DefaultKnowledgeSubmission.kt` — forward its existing `requestingPrincipalId` unchanged after Evaluation B approval; and
3. `src/runtime/DefaultKnowledgeCandidateEvaluator.kt` — use the supplied Principal on every direct evidence lookup and relationship query and remove the live fixed constant.

No candidate field, submission signature, Memory Retrieval surface, policy, Purpose, engine, identity, Memory Core, Evidence Intelligence, composition or persistence change is required.

## 4. Test plan

Direct test changes are limited to `DefaultKnowledgeCandidateEvaluatorTest.kt` and `DefaultKnowledgeSubmissionTest.kt`, including signature adaptation and exact-value capture. Existing admission and Evidence Intelligence acceptance tests already prove their Principal reaches `KnowledgeSubmission`; they remain regression suites unless compilation or a missing explicit invariant requires a narrow change.

Targeted verification will cover:

- two distinct submitting Principals reaching the evaluator unchanged;
- direct and relationship retrieval receiving the exact value;
- no `system.knowledge-memory` constant/fallback in the live evaluator;
- non-null typed carrier and no ambient context;
- unchanged candidate Purpose while Principal varies, through preserved real composition;
- unregistered Principal denial through the real engine;
- unchanged Evidence Intelligence denial and admission propagation;
- dormant revision evaluator unchanged; and
- unchanged Unit 4 fingerprints.

## 5. Boundary Review determination

No Boundary Review is required. The authorized three-file surface contains the exact loss point and all existing downstream carrier capability. If compilation requires any protected production file, implementation must stop rather than expand.

## 6. Planning verdict

```text
PASS
```

The prerequisite may proceed. Unit 4 remains formally paused and Unit 5 remains prohibited.
