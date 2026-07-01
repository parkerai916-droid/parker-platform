# ADR-017 – ExecutionRequest Is Canonical

## Status
Accepted

## Decision
Any proposed work that may cause execution, resource access, state mutation, or external side effects MUST become an ExecutionRequest.

## Reason
A canonical request model allows one permission model, one execution model and one audit model.

## Consequences
No subsystem may invent a parallel execution request type.
