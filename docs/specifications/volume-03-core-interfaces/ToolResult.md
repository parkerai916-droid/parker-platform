# ToolResult Contract (Supporting Type)

## Status
Version: 0.6-alpha
Status: Provisional -- added to resolve IMPLEMENTATION_GAPS.md #3 (Phase 1, feature/phase-1-core-contracts)

## Purpose
The return type of `Tool.execute(request): ToolResult` (see `Tool.md`).
Tool.md requires "Tools MUST return structured results" without specifying
the structure; this entry gives the minimal shape needed to satisfy that
requirement until a fuller spec exists.

## Required Fields
- toolId
- success

## Optional Fields
- output (structured key/value data produced by the tool)
- errorMessage (populated when success is false)

## Normative Requirements
- A ToolResult MUST identify which tool produced it.
- A failed ToolResult SHOULD include a human-readable errorMessage.
- ToolResult MUST NOT be the mechanism by which a Tool asserts it was authorised to run -- that is ExecutionPipeline/PermissionEngine's responsibility, per Tool.md ("Tools MUST NOT decide whether work is authorised").

## Open Questions (not resolved by this entry)
- Whether ToolResult should carry structured (typed) output per tool rather than a generic key/value map.
- Whether ToolResult should reference affected ResourceIds directly (ExecutionResult already does, at the request level).

## Related
- Tool.md
- ExecutionResult Contract (Volume 1) -- `toolResults` field
- IMPLEMENTATION_GAPS.md #3
