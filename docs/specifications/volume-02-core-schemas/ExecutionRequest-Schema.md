# ExecutionRequest Schema Specification

## Status
Version: 0.6-alpha (schema updated this pass -- see IMPLEMENTATION_GAPS.md #2)

## Purpose
Defines the canonical data shape for proposed work before execution.

## Normative Source
`docs/schemas/ExecutionRequest.schema.json` is the normative, versioned
source for this schema. This document is a human-readable summary; if the
two ever disagree, the JSON Schema wins (ADR-019).

## Required Fields
requestId, principalId, origin, intent, targetResources, proposedActions,
priority, createdAt, correlationId.

## Optional Fields
sessionId, riskEstimate, expiresAt, metadata.

## Key Enumerations
- origin: VOICE, TEXT, SCHEDULED_TASK, AGENT, PLUGIN, HOME_ASSISTANT_EVENT, ANDROID_EVENT, REMOTE_INTERFACE
- priority: IMMEDIATE, HIGH, NORMAL, BACKGROUND, DEFERRED, MAINTENANCE
- riskEstimate: LOW, MEDIUM, HIGH, CRITICAL

## Validation Rules
- Required fields MUST be present.
- Identifiers MUST be unique within their schema domain.
- Cross references SHOULD resolve to valid Parker objects.
- Sensitive fields SHOULD be redacted in logs.
- `expiresAt`, when present, MUST be after `createdAt`.

## Kotlin Mapping
Maps to `parker.core.interfaces.ExecutionRequest`
(`src/contracts/ExecutionRequest.kt`), an immutable data class.

## Versioning
Breaking changes require a schema version update and ADR (ADR-019).
