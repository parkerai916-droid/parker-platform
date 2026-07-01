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
