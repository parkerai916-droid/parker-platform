# Principal Schema Specification

## Status
Version: 0.6-alpha (schema reconciled this pass -- see IMPLEMENTATION_GAPS.md #9)

## Purpose
Defines the canonical data shape for any actor that can request access,
initiate work, publish events, or appear in audit records.

## Normative Source
`docs/schemas/Principal.schema.json` is the normative, versioned source.
It now agrees with the prose `Principal Contract` (Volume 1) and with
`src/contracts/Principal.kt`: same `principalType` enum values, and
`owner`/`lastSeenAt` are both present and required.

## Required Fields
principalId, principalType, displayName, owner, status, createdAt, lastSeenAt.

`owner` is required as a key but its value may be `null` (type
`["string", "null"]`) -- a root User or System principal may genuinely
have no owner. This matches `src/contracts/Principal.kt`'s `owner: PrincipalId?`.

## Optional Fields
metadata.

## Key Enumerations
- principalType: USER, SYSTEM, INTERNAL_AGENT, PLUGIN, TOOL, SCHEDULED_TASK, DEVELOPER_SESSION, FUTURE_REMOTE_DEVICE
- status: CREATED, ACTIVE, SUSPENDED, REVOKED, ARCHIVED

## Validation Rules
- Required fields MUST be present (owner's value MAY be null; the others MUST NOT be).
- Sensitive fields SHOULD be redacted in logs.

## Kotlin Mapping
Maps to `parker.core.interfaces.Principal` (`src/contracts/Principal.kt`).
Schema, prose, and Kotlin are now consistent.

## Versioning
Breaking changes require a schema version update and ADR (ADR-019).
