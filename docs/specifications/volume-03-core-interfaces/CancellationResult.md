# CancellationResult Contract (Supporting Type)

## Status
Version: 0.6-alpha
Status: Provisional -- added to resolve IMPLEMENTATION_GAPS.md #3 (Phase 1, feature/phase-1-core-contracts)

## Purpose
The return type of `ExecutionPipeline.cancel(requestId): CancellationResult`
(see `ExecutionPipeline.md`).

## Required Fields
- requestId
- cancelled (whether the cancellation actually took effect)

## Optional Fields
- reason (e.g. why cancellation failed, if `cancelled` is false -- a request already in a terminal state cannot be cancelled)

## Normative Requirements
- Cancelling a request already in a terminal ExecutionLifecycleState (Completed, Failed, Denied, Deferred, Cancelled, Expired) MUST report `cancelled: false`, not throw.

## Related
- ExecutionPipeline.md
- ExecutionRequest lifecycle state machine (docs/diagrams/execution-state-machine.mmd)
- IMPLEMENTATION_GAPS.md #3
