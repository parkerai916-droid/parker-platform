# AuditRecord Schema Specification

## Status
Version: 0.6-alpha

## Purpose
Defines the canonical data shape for an audit entry (ADR-014: Audit logs are protected resources; ADR-020: AuditRecord is a first-class canonical schema).

## Normative Source
`docs/schemas/AuditRecord.schema.json` is the normative, versioned source.
Out of Phase 1 scope -- summarised here for traceability, not yet
implemented in Kotlin.

## Required Fields
auditRecordId, timestamp, principalId, action, outcome, correlationId.

## Optional Fields
resourceId, redacted, details.

## Key Enumerations
- outcome: APPROVED, DENIED, EXECUTED, FAILED, CANCELLED, DEFERRED

## Kotlin Mapping
Not yet implemented -- out of Phase 1 scope.

## Versioning
Breaking changes require a schema version update and ADR (ADR-019).
