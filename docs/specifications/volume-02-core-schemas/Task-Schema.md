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
