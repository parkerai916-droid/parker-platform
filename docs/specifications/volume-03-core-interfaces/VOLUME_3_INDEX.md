# Parker Platform Specification
# Volume 3 – Core Interfaces

## Version
0.6-alpha3

## Purpose

Volume 3 defines Parker's stable core service interfaces.

These interfaces are implementation contracts. They describe what each service must provide without prescribing how the service is implemented.

## Included Interfaces

- ExecutionPipeline
- PermissionEngine
- ResourceRegistry
- EventBus
- Tool
- Agent
- Plugin
- MemoryStore
- WorldModel
- ModelManager
- NotificationService
- AuditService

## Supporting Types

These types are referenced by the interfaces above but were not
independently specified until this entry. Each is marked Provisional --
inferred from how its owning interface uses it, not independently
designed. See `docs/architecture/IMPLEMENTATION_GAPS.md` #3.

- PermissionExplanation (used by PermissionEngine)
- ToolResult (used by Tool)
- ToolDescriptor (used by Tool)
- CancellationResult (used by ExecutionPipeline)
- ExecutionStatus (used by ExecutionPipeline)

## Normative Status

These interfaces are normative for v0.6 engineering work.

Implementations MAY vary internally, but public behaviour MUST conform to these contracts. Provisional supporting types are normative only insofar as *some* shape is needed to compile against them -- treat their field lists as subject to change without a full ADR until they graduate out of Provisional status.
