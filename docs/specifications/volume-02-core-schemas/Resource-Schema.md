# Resource Schema Specification

## Status
Version: 0.6-alpha

## Purpose
Defines the canonical data shape for anything Parker can read, create,
modify, delete, execute, export, observe, or protect.

## Normative Source
`docs/schemas/Resource.schema.json` is the normative, versioned source.
**Resolved by the targeted refinement pass** (`IMPLEMENTATION_GAPS.md`
#10/#29): `src/contracts/Resource.kt` now defines `ResourceSensitivity`
with exactly the nine values below and types `Resource.sensitivity` as
that enum, rather than a free-form `String`. No values were invented --
the enum is a direct transcription of the schema's existing `sensitivity`
property.

**Resolved in the v0.7 Architecture Completion Phase:** `createdAt`,
`updatedAt`, and `source` are now present in `Resource.schema.json` and
its example, matching the prose Contract and `src/contracts/Resource.kt`.

**New finding, requires human decision:** the schema's `resourceType`
enum has 18 values, but the prose Contract's "Resource Categories" list
and `src/contracts/Resource.kt`'s `ResourceType` enum both agree on only
14 (the schema additionally has `SESSION`, `EVENT`, `TASK`, `WORKFLOW`).
Unlike the earlier `Principal.schema.json` mismatch -- where prose and
Kotlin agreed and the schema was simply wrong -- this one is ambiguous:
it's plausible the schema is *ahead* of prose/Kotlin (Session, Event,
Task, and Workflow all have their own canonical schemas and plausibly
should be catalogued as Resources too, per Chapter 8's "authoritative
catalogue of every protected object"), not behind it. This document does
not silently add or remove categories; see
`docs/architecture/IMPLEMENTATION_GAPS.md` for the recorded decision
point.

## Required Fields (per JSON Schema)
resourceId, resourceType, displayName, ownerPrincipalId, sensitivity,
lifecycleState, createdAt, updatedAt, source.

## Key Enumerations
- resourceType: MEMORY, WORLD_MODEL, DOCUMENT, EMAIL, CALENDAR, CONTACT, HOME_ASSISTANT_ENTITY, ANDROID_CAPABILITY, TOOL, PLUGIN, AGENT, SECRET, CONFIGURATION, AUDIT_LOG, SESSION, EVENT, TASK, WORKFLOW (see "new finding" above)
- sensitivity: PUBLIC, PERSONAL, HOUSEHOLD, FINANCIAL, MEDICAL, LEGAL, SECURITY_SENSITIVE, CREDENTIALS_SECRETS, THIRD_PARTY_PERSONAL_DATA
- lifecycleState: CREATED, REGISTERED, AVAILABLE, UPDATED, ARCHIVED, DELETED

## Validation Rules
- Required fields MUST be present.
- Every Resource MUST have an owner and a sensitivity classification.
- Sensitive fields SHOULD be redacted in logs.

## Kotlin Mapping
Maps to `parker.core.interfaces.Resource` (`src/contracts/Resource.kt`).

## Versioning
Breaking changes require a schema version update and ADR (ADR-019).
