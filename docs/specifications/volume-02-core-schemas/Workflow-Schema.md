# Workflow Schema Specification

## Status
Version: 0.6-alpha

## Purpose
Defines the canonical data shape for structured, multi-step behaviour (ADR-012: Tasks track work, Workflows define structured multi-step behaviour).

## Normative Source
`docs/schemas/Workflow.schema.json` is the normative, versioned source.
Out of Phase 1 scope -- summarised here for traceability, not yet
implemented in Kotlin.

## Required Fields
workflowId, ownerPrincipalId, status, steps, createdAt.

## Key Enumerations
- status: CREATED, ACTIVE, RUNNING, PAUSED, COMPLETED, FAILED, CANCELLED, ARCHIVED

## Kotlin Mapping
Not yet implemented -- out of Phase 1 scope.

## Versioning
Breaking changes require a schema version update and ADR (ADR-019).

## Lifecycle

Diagram: `docs/diagrams/workflow-lifecycle-state-machine.mmd`
(added v0.7 Architecture Completion Phase, Priority 5).

### Terminology Note (important)
Chapter 38 describes a *different* sequence -- `Trigger -> Preconditions
-> Steps -> Validation -> Completion -> Reflection` -- which is the
execution-phase flow of a single Workflow **run**, not this schema's
`status` field. The two are complementary, not contradictory: a Workflow
definition's `status` (this section) tracks whether the definition itself
is active/running/archived, while Chapter 38's phases describe what
happens *inside* a single Running episode. Conflating them was flagged as
a risk during the v0.6 consistency review's terminology-drift check; this
note exists so future readers don't assume the schema enum is a
mis-transcription of the chapter's phases (or vice versa).

### States
CREATED, ACTIVE, RUNNING, PAUSED, COMPLETED, FAILED, CANCELLED, ARCHIVED
(from the schema's `status` enum).

### Valid Transitions
- Created -> Active | Cancelled
- Active -> Running | Cancelled
- Running -> Paused | Completed | Failed | Cancelled
- Paused -> Running | Cancelled
- Completed | Failed | Cancelled -> Archived

### Invalid Transitions
No transition directly from Created or Active to Completed/Failed
(a Workflow must actually be Running to complete or fail). No transition
out of Archived (terminal). No transition back from Archived to any
earlier state -- archival is one-way, consistent with Resource's Archived
semantics.

### Failure States
FAILED, reachable only from Running, mirroring Task's rule for the same
reason (a Workflow cannot fail before any step has executed).

### Archived States
ARCHIVED is Workflow's genuine terminal state, reached only from
Completed, Failed, or Cancelled -- never directly from Created, Active,
Running, or Paused. This matches the Resource lifecycle's pattern
(Archived as a deliberate final step, not a synonym for "stopped").
