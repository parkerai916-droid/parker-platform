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

## Normative Status

These interfaces are normative for v0.6 engineering work.

Implementations MAY vary internally, but public behaviour MUST conform to these contracts.
