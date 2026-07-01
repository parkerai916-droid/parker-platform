# ADR-018 – ExecutionRequests Are Immutable After Validation

## Status
Accepted

## Decision
ExecutionRequests become immutable after validation.

## Reason
Mutable requests create audit ambiguity and security risk.

## Consequences
Changes require creation of a new ExecutionRequest linked by correlation ID.
