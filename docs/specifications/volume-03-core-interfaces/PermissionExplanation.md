# PermissionExplanation Contract (Supporting Type)

## Status
Version: 0.6-alpha
Status: Provisional -- added to resolve IMPLEMENTATION_GAPS.md #3 (Phase 1, feature/phase-1-core-contracts)

## Purpose
The return type of `PermissionEngine.explain(decisionId): PermissionExplanation`
(see `PermissionEngine.md`). Gives a human- or agent-readable account of
why a given `PermissionDecision` was reached, supporting the Permission
Contract's normative requirement that "Permission decisions MUST be
auditable" and Chapter 43's audit/observability goals.

## Required Fields
- decisionId
- reason

## Normative Requirements
- `explain` MUST be answerable for any `decisionId` previously returned by `evaluate`.
- The explanation MUST reference the same `decisionId` it was requested for.

## Open Questions (not resolved by this entry)
This is a minimal shape, inferred only from how the interface uses it --
not an independently designed spec. In particular it does not yet address:
- Whether an explanation should reference the specific policy rule(s) that fired.
- Whether explanations differ in detail between an ADMINISTRATIVE-level requester and the original principal.

## Related
- PermissionEngine.md
- Permission Contract (Volume 1)
- IMPLEMENTATION_GAPS.md #3
