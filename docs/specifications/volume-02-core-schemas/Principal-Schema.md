# Principal Schema Specification

## Status
Version: 0.6-alpha

## Purpose
Defines the canonical data shape for any actor that can request access,
initiate work, publish events, or appear in audit records.

## Normative Source
`docs/schemas/Principal.schema.json` is the normative, versioned source.
**It currently disagrees with the prose `Principal Contract` (Volume 1)
in two ways**, newly discovered during this cleanup pass (not one of the
originally scoped fixes -- recorded, not corrected, in
`docs/architecture/IMPLEMENTATION_GAPS.md`):
- The schema's `principalType` enum uses `AGENT`/`REMOTE_DEVICE`; the
  prose and this implementation's Kotlin (`src/contracts/Principal.kt`)
  use `INTERNAL_AGENT`/`FUTURE_REMOTE_DEVICE`.
- The schema does not define `owner` or `lastSeenAt` at all, though the
  prose lists both as required fields.

## Required Fields (per JSON Schema)
principalId, principalType, displayName, status, createdAt.

## Required Fields (per prose Contract, not yet in the JSON Schema)
owner, lastSeenAt.

## Key Enumerations
- principalType (schema): USER, SYSTEM, AGENT, PLUGIN, TOOL, SCHEDULED_TASK, DEVELOPER_SESSION, REMOTE_DEVICE
- principalType (prose / this implementation): User, System, Internal Agent, Plugin, Tool, Scheduled Task, Developer Session, Future Remote Device
- status: CREATED, ACTIVE, SUSPENDED, REVOKED, ARCHIVED

## Validation Rules
- Required fields MUST be present.
- Sensitive fields SHOULD be redacted in logs.

## Kotlin Mapping
Maps to `parker.core.interfaces.Principal` (`src/contracts/Principal.kt`),
which currently follows the **prose** naming/field set, not the JSON
Schema. Reconciling these is recommended before the schema's next
revision.

## Versioning
Breaking changes require a schema version update and ADR (ADR-019).
