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
completedAt, affectedResources, toolResults, warnings, errors,
auditRecordId, reflectionCandidate, metadata.

**Resolved in the v0.7 Architecture Completion Phase:** `toolResults`
(array, shape mirrors `parker.core.interfaces.ToolResult`) and
`reflectionCandidate` (boolean, default `false`) are now present in
`ExecutionResult.schema.json` and its example, matching
`src/contracts/ExecutionResult.kt`. Both are optional in the schema (not
in the `required` array) since the Kotlin type supplies defaults for
both, consistent with how `affectedResources`/`warnings`/`errors` are
already treated as optional despite prose listing them as "Required
Fields" generically.

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
