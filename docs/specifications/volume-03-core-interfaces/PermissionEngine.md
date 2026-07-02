# PermissionEngine Interface

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
