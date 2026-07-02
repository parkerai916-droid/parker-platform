# Parker Platform Specification
# Volume 1 – Core Contracts

## Version
0.6-alpha

## Status
Initial engineering specification release.

## Purpose
Volume 1 establishes Parker's core runtime contracts.

These contracts define how identity, resources, permissions, execution requests, and execution results are represented across the platform.

## Included Specifications
- Principal
- Resource
- Permission
- ExecutionRequest
- ExecutionResult

## Included Schemas
- Principal.schema.json
- Resource.schema.json
- PermissionDecision.schema.json (corrected in the v0.7 Architecture
  Completion Phase -- this index previously listed the deprecated
  `Permission.schema.json` duplicate; see
  docs/specifications/volume-02-core-schemas/PermissionDecision-Schema.md
  and docs/architecture/IMPLEMENTATION_GAPS.md #8)
- ExecutionRequest.schema.json
- ExecutionResult.schema.json

## Included Diagrams
- principal-resource-model.mmd
- permission-flow.mmd
- execution-lifecycle.mmd
- execution-state-machine.mmd

## Revision History
| Version | Date | Description |
|---|---|---|
| 0.6-alpha | 2026-07-01 | Initial core contracts specification |
| 0.6-alpha (correction) | 2026-07-02 | Corrected "Included Schemas" to reference PermissionDecision.schema.json instead of the deprecated Permission.schema.json duplicate (v0.7 Architecture Completion Phase) |

## Normative Status
This volume is normative for v0.6 engineering work.

Future changes require ADR review.
