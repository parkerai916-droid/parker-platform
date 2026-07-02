# ExecutionStatus Contract (Supporting Type)

## Status
Version: 0.6-alpha
Status: Provisional -- added to resolve IMPLEMENTATION_GAPS.md #3 (Phase 1, feature/phase-1-core-contracts)

## Purpose
The return type of `ExecutionPipeline.status(requestId): ExecutionStatus`
(see `ExecutionPipeline.md`). Lets a caller poll the current lifecycle
state of a previously submitted request.

## Required Fields
- requestId
- state (an ExecutionLifecycleState value, per docs/diagrams/execution-state-machine.mmd)
- lastUpdatedAt

## Normative Requirements
- `state` MUST always be one of the states in the ExecutionRequest lifecycle diagram -- never a value outside that state machine.

## Open Questions (not resolved by this entry)
- How `status` is served under the coroutine model this Volume mandates (a polled store? a shared `StateFlow`? something else?) -- recorded as a required architecture decision in the reconciliation report, S8.

## Related
- ExecutionPipeline.md
- docs/diagrams/execution-state-machine.mmd
- IMPLEMENTATION_GAPS.md #3
