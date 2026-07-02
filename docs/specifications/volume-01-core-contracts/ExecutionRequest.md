# ExecutionRequest Contract

## Status
Version: 0.6-alpha  
Status: Draft Specification

## Purpose
An ExecutionRequest is the canonical request object used whenever Parker intends to perform work.

Everything that may lead to external action, state mutation, tool execution, plugin operation, scheduled task execution, or sensitive data access MUST become an ExecutionRequest.

## Origin Sources
- Voice
- Text
- Scheduled task
- Internal agent
- Plugin
- Home Assistant event
- Android system event
- Future remote interface

**Terminology note (added v0.7 Architecture Completion Phase, closes
consistency review §3.8):** the schema enum value for "Internal agent" is
`RequestOrigin.AGENT`, while the analogous Principal type
(`docs/specifications/volume-01-core-contracts/Principal.md`) is
`PrincipalType.INTERNAL_AGENT`. This is a deliberate, not accidental,
naming difference: `RequestOrigin` answers "what kind of channel did this
request arrive through" (a request can arrive *from* an internal agent
channel), while `PrincipalType` answers "what kind of actor is this" (the
Principal *is* an internal agent). The two concepts are related --  an
`AGENT`-origin request's `principalId` will typically resolve to an
`INTERNAL_AGENT` Principal -- but are not the same field describing the
same thing twice, so no renaming is proposed here. Confirmed not to be the
same class of drift already fixed in `Principal.schema.json`
(`AGENT`/`INTERNAL_AGENT` enum-value mismatch for the *same* field), which
was a genuine bug; this is two different fields that happen to share a
word.

## Required Fields
- requestId
- principalId
- sessionId
- origin
- intent
- targetResources
- proposedActions
- priority
- riskEstimate
- createdAt
- expiresAt
- correlationId
- metadata

## Lifecycle
Created → Validated → PermissionPending → Approved → Queued → Executing → Completed

Alternative terminal states: Denied, Deferred, Cancelled, Expired, Failed.

## Normative Requirements
- ExecutionRequests MUST be immutable after validation.
- ExecutionRequests MUST identify a Principal.
- ExecutionRequests MUST identify target Resources before Permission evaluation.
- ExecutionRequests MUST NOT bypass Permission evaluation.
- Expired ExecutionRequests MUST NOT execute.
- Every ExecutionRequest MUST produce an ExecutionResult or recorded terminal state.
