# ToolInvocationBinding Interface

## Status
Version: 1.1 (Architecture v1.1 backfill). This document is created as
part of the Architecture v1.1 documentation pass, per
`docs/architecture/PARKER_ARCHITECTURE_V1_1_CONSOLIDATION_PLAN.md`
("Change 11"). It backfills a Volume 3 specification for an interface
and implementation that already exist and are already exercised by
Sprint 1 (`src/contracts/ToolInvocationBinding.kt`,
`src/runtime/InMemoryToolInvocationBinding.kt`, wired into
`DefaultExecutionPipeline` by Unit 11A, commit `13c9322`). It does not
change, extend, or reinterpret any of that already-built behaviour --
the same backfill posture `ToolRegistry.md` itself already establishes
for `IMPLEMENTATION_GAPS.md` #21.

## Purpose

`ToolInvocationBinding` supplies the one step `ToolRegistry.resolve`
deliberately does not: turning an already-resolved `ToolDescriptor`
into the actual, invocable `Tool` instance registered against it, for
use by the Execution Pipeline only. `ToolResolution.Resolved` carries a
descriptor, never a live `Tool` reference; `ToolInvocationBinding`
closes that gap as a small, additive lookup rather than by changing
`ToolRegistry`'s or `ToolResolution`'s already-implemented shape. It
was introduced to close `IMPLEMENTATION_GAPS.md` #32 (Blocker 4 of
`docs/implementation/SPRINT_1_VERTICAL_SLICE_PLAN.md`) and wired into
`DefaultExecutionPipeline` by Sprint 1 Unit 11A.

## Responsibilities

- Register an invocable `Tool` instance against an already-registered
  `ToolDescriptor` (`bind`)
- Return the invocable `Tool` bound to a resolved `ToolDescriptor`, for
  Execution-Pipeline-only use as an architectural boundary (`invocableFor`)
- Preserve the architectural rule established by `ToolRegistry.md` that
  nothing except the Execution Pipeline ever holds a live `Tool`
  reference.
- Leave descriptor resolution (capability matching, `ENABLED`-state
  filtering, ambiguity handling) entirely to `ToolRegistry.resolve`,
  unchanged

## Required Operations

```kotlin
interface ToolInvocationBinding {
    suspend fun bind(descriptor: ToolDescriptor, tool: Tool)
    suspend fun invocableFor(descriptor: ToolDescriptor): Tool?
}
```

Supporting types: `ToolDescriptor.md`, `Tool.md` (the invocable type
this interface hands back), `ToolRegistry.md` (the interface this one
supplements, not replaces).

## Normative Requirements

- `bind` requires that the bound `Tool`'s own descriptor equal the
  `ToolDescriptor` it is bound to -- a Tool must not be discoverable
  under a descriptor that does not describe it, mirroring
  `ToolRegistry.register`'s own descriptor-consistency check
  (`src/runtime/InMemoryToolInvocationBinding.kt`).
- `invocableFor` returns `null`, not an exception, when a descriptor has
  been resolved but no `Tool` has been bound to it yet -- a distinct,
  explicit absence, consistent with `resolve`'s own null/typed-failure
  convention.
- `invocableFor` is named and documented for Execution-Pipeline-only
  use as an architectural boundary, the same restriction
  `ToolRegistry.resolve` already carries, not a new, more permissive
  surface.
- `bind` is administrative, matching `ToolRegistry.register`'s own
  treatment as an administrative act, not a `Tool.execute` invocation.

## Known Scope Reductions (carried over from implementation, not
introduced here)

- The Execution-Pipeline-only restriction on `invocableFor` is enforced
  by documentation and convention only, not by construction -- there is
  no caller-identity or visibility mechanism anywhere in this
  repository, including on `ToolRegistry.resolve` itself, which this
  interface is deliberately built to match
  (`src/runtime/InMemoryToolInvocationBinding.kt`'s own doc comment).
  Tracked as `IMPLEMENTATION_GAPS.md` #41, deliberately distinct from
  the now-closed #32.
- No implementation of this interface exists other than
  `InMemoryToolInvocationBinding`; it is not yet backed by anything
  beyond an in-memory, mutex-guarded map keyed by `(toolId, version)`.

This document records the architecture for already-built,
already-tested behaviour (`src/runtime/InMemoryToolInvocationBinding.kt`,
exercised via `DefaultExecutionPipeline`'s Unit 11A tests in
`tests/runtime/DefaultExecutionPipelineTest.kt`). It introduces no new
behaviour or requirements.

## Related

- Chapter 9 -- Tool Registry
- ToolRegistry.md
- ToolDescriptor.md
- Tool.md
- ExecutionPipeline.md
- `IMPLEMENTATION_GAPS.md` #32 (closed), #41 (open)
