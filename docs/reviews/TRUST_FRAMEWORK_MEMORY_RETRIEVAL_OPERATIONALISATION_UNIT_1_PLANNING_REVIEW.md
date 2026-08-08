**Status:** Unit 1 Planning Review — PASS. Conducted against the explicitly accepted Operationalisation Scope Lock and Implementation Plan before Kotlin or test changes. No Boundary Review is required because the accepted mechanism fits the authorised production and test surfaces without touching a protected contract or later unit. Nothing is staged, committed, or pushed.

# Gap #54 Memory Retrieval Operationalisation — Unit 1 Policy Mechanism — Planning Review

## 1. Baseline and authority

Baseline: `main` at `334d709`, with the four explicitly accepted Operationalisation governance artifacts present and uncommitted.

Binding authority is the accepted Operationalisation Scope Lock §§3, 4.1, 4.2, 7 and 12 and Implementation Plan §§3.1, 3.2 and 5. Authorization Purpose Units 1–6 remain the accepted mechanism baseline.

## 2. Scope determination

Unit 1 may change only:

- `src/runtime/DefaultPermissionPolicy.kt`;
- `tests/runtime/DefaultPermissionPolicyTest.kt`; and
- new `tests/runtime/MemoryRetrievalPermissionPolicyOperationalisationTest.kt`.

It may implement only:

1. an empty-by-default, validated closed configuration for targetless derivation of the two governed Memory retrieval verbs; and
2. optional exact-verb rule discrimination, specificity precedence and ambiguity denial.

It may not register an action or real Purpose, propagate a Purpose, add a production rule, or change production composition.

## 3. Boundary review determination

No separate Boundary Review is required. Fresh inspection confirms:

- `ActionMapper` already exposes `ActionMappingResult.Resolved.proposedAction` and accepts supplied resource types;
- `PermissionPolicyRule` and `DefaultPermissionPolicy` are colocated in the sole authorised production file;
- the policy constructor can accept an additive empty-default configuration without changing callers;
- targetless actions can be mapped independently without changing `ActionMapper`;
- existing Authorization Purpose registry validation and request carrier are sufficient; and
- no public Permission Engine, Resource Registry, Memory Core or consumer contract must change.

If implementation contradicts any of these findings, Unit 1 must stop rather than expand.

## 4. Planned mechanism

The policy will validate any configured targetless entry against the frozen closed table:

- `memory.retrieve` → `MEMORY`;
- `memory.retrieve_document` → `DOCUMENT`.

Empty configuration preserves all existing behavior. For an empty-target request, each proposed action is mapped independently using only its exact configured types. Unknown, unconfigured or incorrectly configured verbs cannot derive a type. Requests with target resources use only Resource Registry resolution.

`PermissionPolicyRule` will gain optional `proposedAction: String? = null`. Applicable rules must match the resolved action/type plus every non-null optional dimension. Specificity is determined by the number of exact governed dimensions present: Authorization Purpose and proposed action. One unique maximally specific rule governs. Multiple maximal applicable rules are ambiguity and deny; list order cannot decide.

## 5. Fail-closed proof before implementation

- There is no production configuration in Unit 1, so the composed policy continues to derive no targetless types.
- No production action vocabulary entry is added, so the live Memory verbs remain unresolved independently of the new mechanism.
- No production rule is added, so even a test-configured derivation without a test-local rule denies.
- An absent, unregistered or retired Purpose cannot match a Purpose-specific rule.
- A coarse rule cannot defeat a unique more-specific rule.
- Multiple equally maximal rules deny.

## 6. Verification plan

Targeted verification must cover both governed derivations, unknown targetless denial, real-target regression, coarse-rule compatibility, exact verb matching, coarse-versus-verb precedence, Purpose/verb interaction, order independence, equal-specificity ambiguity, configuration validation, no Resource Registry fabrication and test-local-only vocabulary/rules.

After targeted tests pass, run the full repository suite without changing unrelated failures. Then produce the Unit 1 Completion Review and a genuine Independent Constitutional Review. A Defect Confirmation Review is required only if the independent review finds a correctable defect.

## 7. Planning verdict

```text
PASS
```

Implementation may proceed within Unit 1 only. Unit 2 remains prohibited.
