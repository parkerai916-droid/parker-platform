# ToolDescriptor Contract (Supporting Type)

## Status
Version: 0.6-alpha
Status: Provisional -- added to resolve IMPLEMENTATION_GAPS.md #3 (Phase 1, feature/phase-1-core-contracts)

## Purpose
The type of `Tool.descriptor` (see `Tool.md`). Describes a tool's identity
independent of any single invocation -- what it is, not what it's doing
right now.

## Required Fields
- toolId
- displayName
- description

## Optional Fields
- version (defaults to an initial development version if unspecified)

## Normative Requirements
- toolId MUST be stable across the tool's lifetime (it is how the Tool Registry, Chapter 12, resolves tools).
- displayName and description exist for user-facing and audit-facing explanation, not for programmatic dispatch.

## Open Questions (not resolved by this entry)
- Whether ToolDescriptor should declare requiredPermissions/expected Resource categories directly (the v0.1 prototype's `ParkerTool.requiredPermissions` did this; the authoritative model evaluates Permission per-ExecutionRequest instead, so this may not be needed -- see the reconciliation report).

## Related
- Tool.md
- Chapter 12 -- Tool Framework
- IMPLEMENTATION_GAPS.md #3
