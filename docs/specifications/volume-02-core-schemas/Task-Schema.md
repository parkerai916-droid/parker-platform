# Task Schema Specification

## Status
Version: 0.6-alpha

## Purpose
Defines the canonical data shape for a unit of tracked work (ADR-012: Tasks track work, Workflows define structured multi-step behaviour).

## Normative Source
`docs/schemas/Task.schema.json` is the normative, versioned source. Out of
Phase 1 scope -- summarised here for traceability, not yet implemented in Kotlin.

## Required Fields
taskId, ownerPrincipalId, status, createdAt, updatedAt.

## Key Enumerations
- status: CREATED, QUEUED, RUNNING, PAUSED, COMPLETED, FAILED, CANCELLED, EXPIRED, SUPERSEDED

## Kotlin Mapping
Not yet implemented -- out of Phase 1 scope.

## Versioning
Breaking changes require a schema version update and ADR (ADR-019).

## Lifecycle

Diagram: `docs/diagrams/task-lifecycle-state-machine.mmd`
(added v0.7 Architecture Completion Phase, Priority 5).

### States
CREATED, QUEUED, RUNNING, PAUSED, COMPLETED, FAILED, CANCELLED, EXPIRED,
SUPERSEDED (from the schema's `status` enum; matches Chapter 37's stated
chain plus its listed alternative terminal states).

### Valid Transitions
- Created -> Queued | Cancelled | Superseded
- Queued -> Running | Cancelled | Expired | Superseded
- Running -> Paused | Completed | Failed | Cancelled
- Paused -> Running | Cancelled | Expired | Superseded

### Invalid Transitions
Superseded is deliberately **not** reachable from Running -- a task
actively executing must be Cancelled to stop it, not silently replaced
out from under itself. This is a documented design choice (avoids an
ambiguous "was it stopped or overtaken" audit record for in-flight work),
not an omission. No transition out of any terminal state (Completed,
Failed, Cancelled, Expired, Superseded).

### Failure States
FAILED is the sole failure terminal state, reachable only from Running
(a Task cannot fail before it has started executing; a Task that cannot
even be queued is a validation problem upstream, not a Task failure).

### Archived States
Task has no distinct ARCHIVED state -- its five terminal states
(Completed, Failed, Cancelled, Expired, Superseded) together serve that
role, consistent with Chapter 37's framing of the Task Manager as giving
"operational memory for work in progress" rather than long-term archival
storage (that role belongs to Audit, Chapter 43).
