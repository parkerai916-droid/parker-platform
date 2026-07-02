# PermissionDecision Schema Specification

## Status
Version: 0.6-alpha

## Purpose
Defines the canonical data shape for permission evaluation outcomes.

## Normative Source
**Two schema files exist for this concept and disagree:**
`docs/schemas/PermissionDecision.schema.json` and
`docs/schemas/Permission.schema.json` (both titled "PermissionDecision").
The former's `action` enum includes `SEND_EXTERNAL`; the latter's does
not, and the latter additionally defines `reason`/`requiresConfirmation`
properties the former lacks, and omits a top-level `$schema` key entry the
former has in the standard position. This document treats
`PermissionDecision.schema.json` as normative (it matches the name used
throughout Volume 3's `PermissionEngine.md` and this implementation's
`src/contracts/Permission.kt`) and flags `Permission.schema.json` as
requiring reconciliation or removal. Newly discovered during the Phase 1
follow-up cleanup pass -- see `docs/architecture/IMPLEMENTATION_GAPS.md`.

## Required Fields
decisionId, principalId, resourceId, action, decision, level, timestamp.

## Key Enumerations
- action: READ, WRITE, DELETE, EXECUTE, EXPORT, SHARE, CONTROL, NOTIFY, SCHEDULE, SEND_EXTERNAL (per PermissionDecision.schema.json; Permission.schema.json omits SEND_EXTERNAL)
- decision: APPROVED, APPROVED_WITH_CONFIRMATION, DEFERRED, DENIED
- level: AUTOMATIC, USER_AWARE, CONFIRMATION_REQUIRED, HIGH_ASSURANCE, ADMINISTRATIVE

## Validation Rules
- Required fields MUST be present.
- Identifiers MUST be unique within their schema domain.
- Sensitive fields SHOULD be redacted in logs.

## Kotlin Mapping
Maps to `parker.core.interfaces.PermissionDecision` (`src/contracts/Permission.kt`).

## Versioning
Breaking changes require a schema version update and ADR (ADR-019). The
Permission/PermissionDecision schema duplication above should be resolved
with an ADR before this schema's next revision.
