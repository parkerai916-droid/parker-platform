**Status:** Unit 1 Completion Review — PASS. Reviews only Gap #54 Memory Retrieval Operationalisation Unit 1 (Policy Mechanism) against the accepted Scope Lock, Implementation Plan and Planning Review. No Unit 2 work is included. Nothing is staged, committed, or pushed.

# Gap #54 Memory Retrieval Operationalisation — Unit 1 Completion Review

## 1. Reviewed implementation

Baseline: `main` at `334d709`, with accepted Operationalisation governance artifacts uncommitted.

Unit 1 implementation consists of:

- modified `src/runtime/DefaultPermissionPolicy.kt`;
- new `tests/runtime/MemoryRetrievalPermissionPolicyOperationalisationTest.kt`; and
- the Unit 1 Planning Review.

`tests/runtime/DefaultPermissionPolicyTest.kt` required no modification: its existing 17 tests pass unchanged and directly supply the required coarse-rule regression evidence.

No other production or test file changed for Unit 1.

## 2. Exact production diff

`DefaultPermissionPolicy.kt` adds:

1. optional `PermissionPolicyRule.proposedAction: String? = null`;
2. empty-by-default `targetlessResourceTypesByProposedAction` constructor configuration;
3. constructor validation limiting that configuration to exact subsets of:
   - `memory.retrieve` → `{ MEMORY }`;
   - `memory.retrieve_document` → `{ DOCUMENT }`;
4. independent per-proposed-action mapping for targetless requests;
5. retention of Resource Registry-derived types for requests carrying targets;
6. verb and Authorization Purpose applicability checks;
7. deterministic specificity calculation; and
8. denial when more than one applicable rule has maximal specificity.

No production rule, action registration, Purpose registration or configuration is added.

## 3. Exact test diff

The new 11-test Unit 1 suite proves:

- both frozen targetless derivations;
- zero Resource Registry resolution calls for targetless derivation;
- unknown and unconfigured targetless denial;
- rejection of ungoverned verbs and incorrect governed mappings;
- unchanged ordinary real-target resolution;
- derivation plus test-local vocabulary without a rule denies;
- exact verb matching;
- verb-over-coarse precedence in both rule orders;
- combined Purpose-and-verb precedence in both rule orders;
- equal-specificity conflict denial; and
- incomparable Purpose-only/verb-only ambiguity denial.

Existing policy tests add 17 unchanged regression checks. The existing composed Authorization Purpose suite adds 13 unchanged checks, including production `memory.retrieve` denial.

## 4. Required acceptance matrix

| Requirement | Result | Evidence |
|---|---|---|
| Closed-set derivation only | PASS | Constructor accepts only exact governed key/value pairs; unknown/wrong configuration throws. |
| `memory.retrieve` → `READ`/`MEMORY` | PASS | Real `ActionMapper` plus test-local vocabulary resolves and rule returns `READ`. |
| `memory.retrieve_document` → `READ`/`DOCUMENT` | PASS | Same mechanism independently verified for Document. |
| No fake resources | PASS | No register/create path exists; targetless test proves zero `ResourceRegistry.resolve` calls. |
| No global derivation | PASS | Empty default; unknown/unconfigured verbs receive empty types and deny. |
| Ordinary target behavior unchanged | PASS | Explicit real-target regression plus 17 unchanged existing tests. |
| Optional verb matching only | PASS | New field defaults to `null`; existing call sites compile and pass unchanged. |
| Exact verb match | PASS | Rule for `memory.retrieve` cannot match another verb. |
| Specificity precedence | PASS | Unique more-specific verb and Purpose-plus-verb rules govern over coarser rules. |
| Rule-order independence | PASS | Forward and reversed rule lists produce identical decisions. |
| Ambiguity denial | PASS | Equal-specificity conflicts and incomparable maxima return `DENIED`. |
| Authorization Purpose compatibility | PASS | Active Purpose participates; absent Purpose cannot match its rule; existing Unit 4 semantics remain. |
| No action registration | PASS | `ParkerRuntime.kt` unchanged; registrations exist only in test-local vocabularies. |
| No real Purpose registration | PASS | Production registry/composition unchanged; test Purpose is `test.*`. |
| No consumer propagation | PASS | Consumers and retrieval decorator unchanged. |
| No production policy approval | PASS | Production rule list and `ParkerRuntime.kt` unchanged. |
| No ParkerRuntime change | PASS | Git diff contains no composition modification. |
| Live Memory retrieval remains denied | PASS | `ParkerRuntimeAuthorizationPurposeCompositionTest`: 13/13 passed, including composed denial. |

## 5. Verification results

Targeted command:

```text
gradlew.bat test
  --tests parker.core.runtime.DefaultPermissionPolicyTest
  --tests parker.core.runtime.MemoryRetrievalPermissionPolicyOperationalisationTest
```

Result: `BUILD SUCCESSFUL`; 28 tests, 0 failures, 0 skipped.

Composed denial command:

```text
gradlew.bat test --tests parker.composition.ParkerRuntimeAuthorizationPurposeCompositionTest
```

Result: `BUILD SUCCESSFUL`; 13 tests, 0 failures.

Full suite:

```text
gradlew.bat test
```

Result: 2,002 tests completed, 1 failed, 8 skipped. The sole failure is `OcrStructuralIsolationTest > OcrExecutionSequencer is the sole class in src implementing OcrMechanism()` at `OcrStructuralIsolationTest.kt:338`, the already-recorded Windows path-separator comparison. It is unrelated to Unit 1 production and test files and was not modified.

## 6. Defects and qualifications

No Unit 1 defect was found. The Windows OCR failure is an unrelated environmental/portability issue and has no Unit 1 acceptance significance.

No Boundary Review or Defect Confirmation Review is required.

## 7. Completion verdict

```text
PASS
```

Unit 1 satisfies every frozen requirement and stops at its formal review boundary. Unit 2 has not begun.
