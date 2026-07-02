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
