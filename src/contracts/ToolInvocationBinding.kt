package parker.core.interfaces

/**
 * Sprint 1 contract closing Blocker 4 of
 * docs/implementation/SPRINT_1_VERTICAL_SLICE_PLAN.md
 * (`IMPLEMENTATION_GAPS.md` #32; see
 * docs/implementation/SPRINT_1_BLOCKER_CLOSURE.md for the full
 * investigation and closure record). This file is a **pure contract
 * addition**: one interface, two methods, no implementation, no changes
 * to any existing file.
 *
 * ## The gap this closes
 *
 * `docs/architecture/tool-registry.md` ("Lookup Process") and
 * `docs/specifications/volume-03-core-interfaces/ToolRegistry.md`
 * ("Normative Requirements") both already describe [ToolRegistry.resolve]
 * as "the Execution-Pipeline-only operation that yields an invocable
 * Tool." But [ToolResolution.Resolved] -- the actual return shape --
 * carries only a [ToolDescriptor], never a [Tool]. That type's own doc
 * comment says why: "Deliberately does not carry a live `Tool` instance
 * in this phase (no concrete `Tool` implementations exist yet to resolve
 * to)." `IMPLEMENTATION_GAPS.md` #32 confirms the practical consequence
 * directly: `DefaultExecutionPipeline` resolves a Tool descriptor
 * successfully but never calls [Tool.execute] -- "a `SUCCESS` result...
 * means every orchestration stage up to and including finding the right
 * Tool succeeded," not that a Tool actually ran.
 *
 * This is a Kotlin-implementation-completeness gap, not a specification
 * gap: the specifications already say what should happen; the contract
 * connecting a resolved descriptor to something invocable simply does not
 * exist yet.
 *
 * ## Why this is a new, additive interface rather than a change to
 * `ToolRegistry`/`ToolResolution`
 *
 * The task that produced this file was explicit: close this gap only if
 * the missing piece is a pure interface/contract type, and do not modify
 * production runtime logic. `ToolRegistry.resolve` and
 * `ToolResolution.Resolved` are both already implemented and exercised by
 * `tests/runtime/InMemoryToolRegistryTest.kt`; changing either's shape
 * (e.g. adding a `tool: Tool` field to `Resolved`, or changing
 * `ToolRegistry.register`'s signature to accept a [Tool] instead of a
 * [ToolDescriptor]) would require touching an already-tested Volume 3
 * interface and its one existing implementation
 * (`src/runtime/InMemoryToolRegistry.kt`) -- exactly the "runtime logic"
 * category this change is not authorised to touch. This interface is
 * therefore deliberately free-standing: no existing class is required to
 * implement it, and its addition breaks nothing.
 *
 * **This is recorded as the safe, non-breaking closure for this
 * contract-preparation task, not as a claim that it is the permanent,
 * best long-term shape.** The cleaner long-term fix -- folding an
 * invocable handle directly into `ToolResolution.Resolved`, so `resolve`
 * itself is the one and only lookup an Execution Pipeline ever needs --
 * remains open, recorded in
 * docs/implementation/SPRINT_1_BLOCKER_CLOSURE.md's Open Questions as a
 * human decision for whoever implements Sprint 1's Unit 3, not decided
 * here.
 *
 * ## What this interface does and does not do
 *
 * [ToolInvocationBinding] is a minimal, additional lookup a Sprint-1
 * implementation MAY wire up (in `src/runtime/`) so that
 * `DefaultExecutionPipeline` can go from an already-`Resolved`
 * [ToolDescriptor] to an actual, invocable [Tool] instance that was
 * registered against it, without giving any component other than the
 * Execution Pipeline a path to obtain one. It does not change how
 * [ToolRegistry.resolve] itself decides *which* descriptor matches a
 * request (capability matching, `ENABLED`-state filtering, ambiguity
 * handling) -- that lookup is unchanged. It only adds the one missing
 * step *after* a descriptor is already resolved: binding it to something
 * callable.
 *
 * No implementation of this interface exists in this repository as of
 * this change. Providing one, and wiring `DefaultExecutionPipeline` to
 * call it, is Sprint 1 coding work
 * (`docs/implementation/SPRINT_1_VERTICAL_SLICE_PLAN.md` Units 3 and 4),
 * not this contract-preparation change. Whichever implementation Sprint 1
 * builds MUST preserve `tool-registry.md`'s existing rule verbatim:
 * "nothing except the Execution Pipeline ever holds a live `Tool`
 * reference" -- [invocableFor] is named and documented for
 * Execution-Pipeline-only use, the same restriction [ToolRegistry.resolve]
 * already carries, not a new, more permissive surface.
 */
interface ToolInvocationBinding {

    /**
     * Registers an invocable [tool] instance against an already-registered
     * [descriptor] (i.e. a [ToolDescriptor] for which
     * [ToolRegistry.register] has already succeeded). Administrative,
     * matching [ToolRegistry.register]'s own treatment as an
     * administrative act, not a `Tool.execute` invocation
     * (`docs/architecture/tool-registry.md` "Registration Model").
     */
    suspend fun bind(descriptor: ToolDescriptor, tool: Tool)

    /**
     * Returns the invocable [Tool] bound to [descriptor], if any, for
     * Execution-Pipeline-only use. Returns `null` if [descriptor] has been
     * resolved (i.e. `ToolRegistry.resolve` succeeded) but no invocable
     * [Tool] has been [bind]-ed to it yet -- a distinct, typed absence,
     * not an exception, consistent with `resolve`'s own
     * null/typed-failure convention elsewhere in this repository.
     */
    suspend fun invocableFor(descriptor: ToolDescriptor): Tool?
}
