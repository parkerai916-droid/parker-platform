# Parker Platform Specification
# Volume 2 – Core Schemas

## Version
0.6-alpha2

## Purpose
Volume 2 defines Parker's canonical internal data schemas.

## Included Schemas
- Principal
- Resource
- PermissionDecision
- ExecutionRequest
- ExecutionResult
- Session
- Event
- Workflow
- Task
- AuditRecord

## Normative Source

Each schema's real, versioned field definitions live in
`docs/schemas/*.schema.json`. The markdown files in this directory are
human-readable summaries pointing at those JSON Schemas; previously they
were unfilled generic templates (see `docs/architecture/IMPLEMENTATION_GAPS.md`
#6). If a markdown summary and its `.schema.json` ever disagree, the JSON
Schema wins (ADR-019).

Two summaries (`Principal-Schema.md`, `Resource-Schema.md`) record real
discrepancies found between their JSON Schema and their Volume 1 prose
contract while writing these summaries. `PermissionDecision-Schema.md`
records a further discrepancy: two different schema files
(`Permission.schema.json` and `PermissionDecision.schema.json`) currently
describe the same concept differently.

## Normative Status
These schemas are normative for v0.6 engineering work.
