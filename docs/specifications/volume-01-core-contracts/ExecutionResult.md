# ExecutionResult Contract

## Status
Version: 0.6-alpha  
Status: Draft Specification

## Purpose
An ExecutionResult records the outcome of an ExecutionRequest.

It provides structured feedback to the Runtime, Audit Framework, Reflection Engine, Conversation Engine, and user-facing response layer.

## Required Fields
- resultId
- requestId
- status
- startedAt
- completedAt
- affectedResources
- toolResults
- warnings
- errors
- auditRecordId
- reflectionCandidate
- metadata

## Result Status
- Success
- PartialSuccess
- Failed
- Cancelled
- Denied
- Expired
- Deferred

## Normative Requirements
- Every executed request MUST produce an ExecutionResult.
- ExecutionResults MUST be structured.
- Failed results MUST include a machine-readable reason.
- Partial success MUST identify completed and incomplete steps.
- ExecutionResults MUST reference affected Resources.
- ExecutionResults MUST link to audit records where applicable.
