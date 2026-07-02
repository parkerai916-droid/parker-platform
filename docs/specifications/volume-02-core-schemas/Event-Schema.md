# Event Schema Specification

## Status
Version: 0.6-alpha

## Purpose
Defines the canonical data shape for structured facts published inside Parker (as `ParkerEvent`).

## Normative Source
`docs/schemas/Event.schema.json` is the normative, versioned source.
Out of Phase 1 scope (EventBus is a later phase per `IMPLEMENTATION_ORDER.md`)
-- summarised here for traceability, not yet implemented in Kotlin.

## Required Fields
eventId, publisherPrincipalId, eventType, timestamp, correlationId, payload.

## Optional Fields
signature, metadata.

## Validation Rules
- Required fields MUST be present.
- Events MUST identify their publisher Principal (EventBus.md).
- Unauthenticated events MUST NOT influence Memory, World Model, Execution or Trust.

## Kotlin Mapping
Not yet implemented -- deferred to the phase that implements EventBus.

## Versioning
Breaking changes require a schema version update and ADR (ADR-019).
