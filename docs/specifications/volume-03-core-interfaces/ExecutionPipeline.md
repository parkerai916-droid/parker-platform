# ExecutionPipeline Interface

## Status
Version: 0.6-alpha3 (content unchanged since the v0.6 Volume 3 release;
stamped during the v0.7 Architecture Completion Phase for consistency
with other Volume 3 documents -- see
docs/reviews/PARKER_V0_6_CONSISTENCY_REVIEW.md §3.6).

## Purpose

The ExecutionPipeline receives approved ExecutionRequests and coordinates controlled execution.

## Responsibilities

- Accept validated ExecutionRequests
- Enforce execution lifecycle
- Dispatch tools
- Produce ExecutionResults
- Emit audit events
- Report failures

## Required Operations

```kotlin
interface ExecutionPipeline {
    suspend fun submit(request: ExecutionRequest): ExecutionResult
    suspend fun cancel(requestId: RequestId): CancellationResult
    suspend fun status(requestId: RequestId): ExecutionStatus
}
```

## Normative Requirements

- The pipeline MUST NOT accept unvalidated requests.
- The pipeline MUST NOT bypass PermissionEngine.
- Every submitted request MUST produce an ExecutionResult or terminal status.
- Execution failures MUST be explicit.

## Related

- Chapter 11 – Execution Pipeline
- ExecutionRequest Contract
- ExecutionResult Contract
