# ExecutionResult Schema Specification

## Status
Version: 0.6-alpha

## Purpose
Defines the canonical data shape for the outcome of an ExecutionRequest.

## Normative Source
`docs/schemas/ExecutionResult.schema.json` is the normative, versioned
source for this schema.

## Required Fields
resultId, requestId, status, startedAt.

## Optional Fields
completedAt, affectedResources, warnings, errors, auditRecordId, metadata.

Note: the prose ExecutionResult Contract (Volume 1) also lists
`toolResults` and `reflectionCandidate` as required fields; the JSON
Schema does not yet define either property. Not fixed in this pass (only
`ExecutionRequest.schema.json` was in scope -- see
docs/architecture/IMPLEMENTATION_GAPS.md, newly recorded finding).

## Key Enumerations
- status: SUCCESS, PARTIAL_SUCCESS, FAILED, CANCELLED, DENIED, EXPIRED, DEFERRED

## Validation Rules
- Required fields MUST be present.
- A FAILED result MUST include at least one machine-readable error.
- `completedAt`, when present, MUST NOT precede `startedAt`.

## Kotlin Mapping
Maps to `parker.core.interfaces.ExecutionResult` (`src/contracts/ExecutionResult.kt`).

## Versioning
Breaking changes require a schema version update and ADR (ADR-019).
