# ADR-020 – Session, Event and Audit Schemas

## Status
Accepted

## Decision
Session, Event and AuditRecord are first-class canonical schemas.

## Reason
Execution, observability and accountability require consistent data structures across the platform.

## Consequences
All services producing sessions, events or audit entries must conform to these schemas or approved successors.
