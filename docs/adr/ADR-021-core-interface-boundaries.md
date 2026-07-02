# ADR-021 – Core Interface Boundaries

## Status
Accepted

## Decision
Parker core services expose small, stable interfaces rather than implementation classes.

## Reason
Interfaces preserve replaceability and prevent implementation leakage.

## Consequences
Services depend on contracts rather than concrete implementations.
