# Workflow Schema Specification

## Purpose
Defines the canonical data shape for multi-step structured processes.

## Structure
This schema includes required identifiers, lifecycle or status fields, metadata, and relationships to other Parker core contracts.

## Validation Rules
- Required fields MUST be present.
- Identifiers MUST be unique within their schema domain.
- Cross references SHOULD resolve to valid Parker objects.
- Sensitive fields SHOULD be redacted in logs.

## Kotlin Mapping
Each schema SHOULD map to an immutable Kotlin data class.

## Versioning
Breaking changes require a schema version update and ADR.
