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

## Lifecycle

Diagram: `docs/diagrams/session-lifecycle-state-machine.mmd`
(added v0.7 Architecture Completion Phase, Priority 5).

### States
ACTIVE, EXPIRED, CANCELLED, AUTH_REQUIRED, CLOSED (from the schema's
`status` enum -- there is no CREATED state; a Session begins ACTIVE at
`startedAt`).

### Valid Transitions
- Active -> AuthRequired (confidence loss, authentication timeout, device
  lock -- Chapter 40)
- AuthRequired -> Active (re-authentication succeeds)
- AuthRequired -> Expired | Closed (re-authentication fails or times out)
- Active -> Expired (inactivity)
- Active -> Cancelled (user command, policy restriction)
- Active -> Closed (normal conversational end)

### Invalid Transitions
Any transition not listed above is invalid, in particular: no transition
out of Expired, Cancelled, or Closed (all terminal), and no direct
Active -> AuthRequired -> (skipping back to Active without re-auth).

### Failure States
There is no distinct "failure" status for Session -- Expired and
Cancelled are the closest equivalents (a Session does not "fail" the way
a Task or ExecutionRequest can; it simply ends).

### Archived States
**Finding:** unlike Principal and Resource, `Session.schema.json` defines
no `ARCHIVED` state. Its three terminal states (Expired, Cancelled,
Closed) are functionally its archival equivalent -- once reached, a
Session record is historical, not live. Whether a dedicated ARCHIVED
status should be added for consistency with Principal/Resource/Workflow is
recorded as an open question, not resolved by this document.
