# ToolRegistry Interface

## Status
Version: 0.7.1 (targeted refinement pass). Backfilled per
`IMPLEMENTATION_GAPS.md` #21: `src/interfaces/ToolRegistry.kt` was
implemented directly from `docs/architecture/tool-registry.md` during
Phase 2 Runtime Implementation, without a matching Volume 3 interface
document. This document closes that gap by summarizing the
already-approved architecture and the already-implemented interface
exactly as built -- it does not change, extend, or reinterpret either.

## Purpose

The ToolRegistry is the authoritative catalogue of Parker Tools: where
Tools are registered, discovered, and resolved for execution, per
`docs/architecture/tool-registry.md`.

## Responsibilities

- Register Tool descriptors against a corresponding Resource Registry entry
- Resolve a single invocable Tool for the Execution Pipeline's use only
- Provide a capability-filtered, descriptor-only discovery surface for
  planning components
- Enforce Tool lifecycle transitions
- Prevent any component other than the Execution Pipeline from obtaining
  something invocable

## Required Operations

```kotlin
interface ToolRegistry {
    suspend fun register(descriptor: ToolDescriptor, resourceId: ResourceId): ToolRegistrationOutcome
    suspend fun resolve(action: PermissionAction, resourceTypes: Set<ResourceType>): ToolResolution
    suspend fun findCandidates(actions: Set<PermissionAction>, resourceTypes: Set<ResourceType>): List<ToolDescriptor>
    suspend fun listAll(): List<ToolDescriptor>
    suspend fun setLifecycleState(toolId: String, version: String, newState: ToolLifecycleState): ToolDescriptor
}
```

Supporting types: `ToolDescriptor.md`, `ToolResult.md` (for the shape of
what a resolved Tool eventually returns), `ToolRegistrationOutcome`
(sealed: `Registered`, `Superseded`, `AlreadyRegistered`, `Rejected` --
`src/contracts/ToolResolution.kt`), `ToolResolution` (sealed: `Resolved`,
`Failed` with a `ToolResolutionFailureReason` -- same file), and
`ToolLifecycleState`/`ToolLifecycleTransitions`
(`src/contracts/ToolLifecycle.kt`,
`docs/diagrams/tool-lifecycle-state-machine.mmd`).

## Normative Requirements

- `register` MUST fail (`Rejected`) if `resourceId` does not resolve to a
  registered Resource.
- Re-registering an unchanged `toolId`/`version` pair MUST be idempotent
  (`AlreadyRegistered`, not an error).
- Registering a new `version` for an already-known `toolId` MUST be
  treated as version supersession (`Superseded`, not an error).
- A `toolId`/`version` pair that already exists with a *different*
  descriptor MUST be `Rejected` (inconsistent data -- not a no-op, not
  supersession).
- `resolve` is the only operation that may yield something invocable, and
  per tool-registry.md is intended to be called only by the Execution
  Pipeline.
- `resolve` MUST only return a Tool in `ENABLED` state; zero or multiple
  matching candidates MUST produce a typed `Failed` result
  (`TOOL_NOT_FOUND` / `TOOL_AMBIGUOUS`), never a thrown exception or a
  silently-picked candidate.
- `findCandidates` and `listAll` MUST NOT return anything invocable --
  descriptors only, per tool-registry.md's "models and planners never
  hold executable references" rule.
- `setLifecycleState` is the only sanctioned way to move a Tool through
  `ToolLifecycleState`; it MUST throw if `from -> to` is not a valid edge
  in `ToolLifecycleTransitions`.

## Runtime Lifecycle

See `docs/diagrams/tool-lifecycle-state-machine.mmd` and
`docs/architecture/tool-registry.md`'s "Runtime Lifecycle" section for
the full `Registered -> Enabled -> Disabled -> Deprecated -> Removed`
state machine this interface enforces via `setLifecycleState`.

## Known Scope Reductions (carried over from implementation, not
introduced here)

- No Principal-scoped visibility filtering on `listAll`/`findCandidates`
  yet -- blocked on IdentityService and a policy-bearing PermissionEngine
  (`IMPLEMENTATION_GAPS.md` #23).
- Registration and lifecycle changes are not yet gated by a live
  `PermissionEngine.evaluate` call (`IMPLEMENTATION_GAPS.md` #24).
- `InMemoryResourceRegistry` exists only as a supporting dependency this
  interface's registration invariant requires (`IMPLEMENTATION_GAPS.md`
  #22).

This document backfills a specification for already-built, already-tested
behaviour (`src/runtime/InMemoryToolRegistry.kt`,
`tests/runtime/InMemoryToolRegistryTest.kt`); it does not add new Tool
Registry behaviour, per the targeted refinement pass's explicit scope.

## Related

- Chapter 9 -- Tool Registry
- `docs/architecture/tool-registry.md` (architecture document this
  interface implements)
- ToolDescriptor.md
- ToolResult.md
- Tool.md
- ResourceRegistry.md
- ExecutionPipeline.md
- PermissionEngine.md
- `docs/diagrams/tool-lifecycle-state-machine.mmd`
