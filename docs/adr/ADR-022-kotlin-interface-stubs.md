# ADR-022 – Kotlin Interface Stubs

## Status
Accepted

## Decision
Initial Kotlin files under `src/interfaces` are interface stubs only.

## Reason
They allow implementation planning without prematurely committing to runtime internals.

## Consequences
Concrete implementation must not be added until runtime architecture reaches the implementation phase.
