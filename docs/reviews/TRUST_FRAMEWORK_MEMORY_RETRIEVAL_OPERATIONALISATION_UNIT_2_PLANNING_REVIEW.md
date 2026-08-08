**Status:** Corrected Unit 2 Planning Review — PASS. Conducted after explicit acceptance of the Fail-Closed Guard Rules governance correction and before Unit 2 Kotlin or test changes. No Boundary Review is required: the corrected work fits `ParkerRuntime.kt` and the accepted Unit 2 composition-test surface. Nothing is staged, committed, or pushed.

# Gap #54 Memory Retrieval Operationalisation — Unit 2 Inert Production Configuration — Planning Review

## 1. Baseline and authority

Baseline: `main` at `700f422` (`feat: implement memory retrieval operationalisation unit 1`). Unit 1 is complete and accepted. The corrected Implementation Plan and its accepted correction review authorize exactly two action registrations, two closed derivations, two real Purpose registrations and two exact verb-only `DENIED` guards.

## 2. Fresh live-policy finding

Production contains coarse `READ`/`MEMORY` and `READ`/`DOCUMENT` approvals. Unit 1 correctly preserves coarse fallback. Therefore the guards are mandatory in the same runtime-composition change as registration/derivation; no intermediate or final Unit 2 state may expose a derived verb without its exact denial.

## 3. Authorized production boundary

Only `src/composition/ParkerRuntime.kt` may change. It will:

1. register `memory.retrieve` → `READ`/`MEMORY`;
2. register `memory.retrieve_document` → `READ`/`DOCUMENT`;
3. configure the corresponding exact targetless resource-type map;
4. register `knowledge-memory.candidate-evaluation`;
5. register `evidence-intelligence.input-resolution`;
6. add exact verb-specific `DENIED`/`AUTOMATIC` guards with null Purpose; and
7. update only directly inaccurate composition comments.

No other production file or contract is required. No Boundary Review is needed. Any discovered need to change policy mechanism, decorator, consumer or contract is a stop condition.

## 4. Test boundary

Unit 2 will:

- modify `ParkerRuntimeAuthorizationPurposeCompositionTest.kt` only where its old empty-registry/unregistered-verb assertions become obsolete;
- add `ParkerRuntimeMemoryRetrievalOperationalisationCompositionTest.kt` for exact production configuration, guard precedence, registry contents, single authority and denial; and
- rely on unchanged candidate-admission, Evidence Intelligence, durability and Unit 1 suites for behavioral regression evidence.

## 5. Fail-closed proof

Both guards match one more dimension than the existing coarse approvals and are therefore uniquely maximal for their exact verbs. They are Purpose-null, so absent, active, unregistered and retired Purpose states cannot evade them unless a still-more-specific Purpose-plus-verb rule exists. Unit 2 adds no such rule. Neither consumer is modified and both continue producing Purpose-absent requests.

Action, derivation and Purpose registration are eligibility/configuration only. The only new policy content is denial. Candidate and Evidence Intelligence retrieval must remain denied through the shared decorator.

## 6. Planned verification

Targeted tests must prove exact configuration and no extras, both real Purposes active, no Purpose-specific rule, no consumer adoption, both guards present, guard-over-coarse precedence in both list orders, absent/active/synthetic/retired denial, real decorator denial, single engine/policy/registry/decorator graph and unchanged unrelated coarse approval.

Then run the full suite unchanged, classify only the known Windows OCR issue separately if it recurs, and complete formal Completion and Independent Constitutional Reviews.

## 7. Planning verdict

```text
PASS
```

Corrected Unit 2 may proceed. Units 3 and 4 remain prohibited.
