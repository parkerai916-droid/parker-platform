# PermissionEngine Interface

## Status
Version: 0.6-alpha3 (content unchanged since the v0.6 Volume 3 release;
stamped during the v0.7 Architecture Completion Phase for consistency
with other Volume 3 documents -- see
docs/reviews/PARKER_V0_6_CONSISTENCY_REVIEW.md §3.6).

## Purpose

The PermissionEngine evaluates whether a Principal may perform requested actions against Resources.

## Responsibilities

- Evaluate PermissionDecision
- Enforce permission levels
- Check resource sensitivity
- Apply policy state
- Identify confirmation requirements

## Required Operations

```kotlin
interface PermissionEngine {
    suspend fun evaluate(request: ExecutionRequest): PermissionDecision
    suspend fun explain(decisionId: DecisionId): PermissionExplanation
}
```

## Normative Requirements

- Every ExecutionRequest MUST be evaluated before execution.
- Denied decisions MUST include a reason.
- Confirmation-required decisions MUST be explicit.
- Permission evaluation MUST be auditable.

## Related

- Chapter 10 – Permission Engine
- Permission Contract
