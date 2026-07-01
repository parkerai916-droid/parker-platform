# Volume 1 – Core Contracts

## Purpose
Volume 1 defines the core contracts that Parker uses to describe identity, resources, permissions, execution requests, and execution outcomes.

## Contracts
- Principal
- Resource
- Permission
- ExecutionRequest
- ExecutionResult

## Contract Grammar
```text
Principal
    creates
ExecutionRequest
    targets
Resource
    evaluated by
Permission
    produces
ExecutionResult
```

## Implementation Requirement
Every Parker runtime implementation MUST conform to these contracts unless superseded by a later specification version and approved ADR.
