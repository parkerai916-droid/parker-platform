# Resource Schema Specification

## Status
Version: 0.6-alpha

## Purpose
Defines the canonical data shape for anything Parker can read, create,
modify, delete, execute, export, observe, or protect.

## Normative Source
`docs/schemas/Resource.schema.json` is the normative, versioned source.
Note: the schema defines a concrete `sensitivity` enum (see below); this
implementation's `src/contracts/Resource.kt` currently types `sensitivity`
as a free-form `String` because that Kotlin was written before this
schema was read closely (see `docs/architecture/IMPLEMENTATION_GAPS.md` --
this corrects an earlier, mistaken claim that no sensitivity enum existed
anywhere). Recommended follow-up: change the Kotlin type to the enum below.

## Required Fields (per JSON Schema)
resourceId, resourceType, displayName, ownerPrincipalId, sensitivity, lifecycleState.

## Required Fields (per prose Contract, not yet in the JSON Schema)
createdAt, updatedAt, source, metadata.

## Key Enumerations
- resourceType: MEMORY, WORLD_MODEL, DOCUMENT, EMAIL, CALENDAR, CONTACT, HOME_ASSISTANT_ENTITY, ANDROID_CAPABILITY, TOOL, PLUGIN, AGENT, SECRET, CONFIGURATION, AUDIT_LOG, SESSION, EVENT, TASK, WORKFLOW
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
