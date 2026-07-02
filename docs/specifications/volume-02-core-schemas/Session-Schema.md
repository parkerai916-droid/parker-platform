# Session Schema Specification

## Status
Version: 0.6-alpha

## Purpose
Defines the canonical data shape for a bounded interaction session (ADR-020: Session is a first-class canonical schema).

## Normative Source
`docs/schemas/Session.schema.json` is the normative, versioned source.
Out of Phase 1 scope -- summarised here for traceability, not yet
implemented in Kotlin.

## Required Fields
sessionId, principalId, sessionType, status, startedAt.

## Optional Fields
expiresAt, metadata.

## Key Enumerations
- sessionType: VOICE, TEXT, BACKGROUND_TASK, DEVELOPER, PLUGIN, REMOTE
- status: ACTIVE, EXPIRED, CANCELLED, AUTH_REQUIRED, CLOSED

## Kotlin Mapping
Not yet implemented -- out of Phase 1 scope.

## Versioning
Breaking changes require a schema version update and ADR (ADR-019).
