package parker.composition

/**
 * Sprint 10, Unit 4 (Production Composition Root). The composition root's
 * own typed failure model for everything that can go wrong *before* the
 * runtime is accepting inbound work: missing/invalid configuration,
 * dependency construction failure, and startup/shutdown failure -- the
 * task's own named categories. Deliberately **not** used for a failure
 * that occurs *while processing* an already-started runtime's inbound
 * message (model unavailable mid-conversation, a Tool failure, a
 * coordinator exception) -- those are ordinary, expected-to-happen
 * operational outcomes, reported as [ParkerRuntimeOutcome.Failed] to the
 * caller of [ParkerRuntime.submitOwnerMessage], not thrown. This mirrors
 * the distinction this repository's own coordinators already draw between
 * "a structural admission gate" (`GatedOutcome`) and "a genuine fault"
 * (an uncaught exception) -- see each coordinator's own Scope Lock,
 * Section 12 ("Exception Behaviour").
 *
 * Every subtype is a `RuntimeException`, per this codebase's own
 * established convention (`IdentityService`, `ModuleRegistry`,
 * `ToolRegistry` all throw for caller-misuse/precondition failures rather
 * than returning a sealed result) -- this Unit does not invent a new
 * error-handling convention, it applies the one already in use one layer
 * further out, at the composition root's own boundary.
 */
sealed class ParkerRuntimeException(message: String, cause: Throwable? = null) : RuntimeException(message, cause) {

    /** A required configuration key (see `ParkerRuntimeConfigLoader`) was absent or blank. */
    class MissingConfiguration(val key: String) :
        ParkerRuntimeException("Missing required Parker Runtime configuration key '$key'")

    /** A configuration key was present but its value could not be interpreted. */
    class InvalidConfiguration(val key: String, val reason: String) :
        ParkerRuntimeException("Invalid Parker Runtime configuration for key '$key': $reason")

    /**
     * Constructing or registering one named runtime component failed
     * during [ParkerRuntime.start]. [component] names which one, in plain
     * language (e.g. `"Local Text Channel module registration"`), so a
     * startup failure is diagnosable without reading a stack trace first.
     */
    class DependencyConstructionFailed(val component: String, cause: Throwable) :
        ParkerRuntimeException("Failed to construct or register '$component'", cause)

    /**
     * [ParkerRuntime.start] failed for a reason not already captured by
     * [MissingConfiguration], [InvalidConfiguration], or
     * [DependencyConstructionFailed] -- a catch-all for a genuinely
     * unexpected startup fault, not a silently-swallowed one: [cause] is
     * always the real, original exception, never discarded.
     */
    class StartupFailed(cause: Throwable) :
        ParkerRuntimeException("Parker Runtime failed to start", cause)

    /**
     * [ParkerRuntime.shutdown] could not complete every shutdown step
     * cleanly. [cause] carries the first failure encountered;
     * [ParkerRuntime.shutdown]'s own KDoc documents that shutdown still
     * attempts every remaining step on a best-effort basis rather than
     * aborting at the first failure.
     */
    class ShutdownFailed(cause: Throwable) :
        ParkerRuntimeException("Parker Runtime did not shut down cleanly", cause)

    /**
     * A caller invoked [ParkerRuntime.submitOwnerMessage] while the
     * runtime was not in [RuntimeLifecycleState.RUNNING] -- a caller-usage
     * error (the runtime was never started, is still starting, is
     * shutting down, has already stopped, or failed to start), not an
     * operational failure of the conversation pipeline itself.
     */
    class NotRunning(val state: RuntimeLifecycleState) :
        ParkerRuntimeException("Parker Runtime is not accepting messages (state=$state)")
}
