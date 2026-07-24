package parker.composition

import parker.core.interfaces.ExecutionResult

/**
 * Sprint 10, Unit 4 (Production Composition Root). The result of one
 * [ParkerRuntime.submitOwnerMessage] call -- a composition-root-level
 * outcome, deliberately **not** a reuse of `GatedOutcome<ExecutionResult>`
 * (`ConversationReplyCoordinator.submitAndDeliver`'s own return type),
 * because this type additionally has to represent a genuine fault
 * ([Failed]) that no frozen coordinator's own `GatedOutcome` shape has any
 * variant for -- every one of those coordinators' own Scope Locks states
 * plainly that such an exception "propagates unchanged to the caller"
 * rather than being represented as a value. This composition root is that
 * caller; representing the fault as a value here, rather than letting it
 * propagate further, is exactly the production error-handling behaviour
 * the task requires ("model unavailable," "coordinator failure," "tool
 * failure") -- see [ParkerRuntime.submitOwnerMessage]'s own KDoc for
 * exactly which exceptions this maps to [Failed] versus lets propagate
 * (`kotlinx.coroutines.CancellationException`, deliberately, always).
 */
sealed class ParkerRuntimeOutcome {

    /** The full pipeline ran to completion and delivery was attempted; [executionResult] is Execution Pipeline's own, unchanged. */
    data class Delivered(val executionResult: ExecutionResult) : ParkerRuntimeOutcome()

    /** A structural admission gate rejected the message before any fault occurred -- [reason] is the rejecting component's own, unchanged. */
    data class NotAccepted(val reason: String) : ParkerRuntimeOutcome()

    /**
     * A genuine fault was caught at this runtime's own outer boundary --
     * the composition root's job, since no frozen coordinator between
     * here and the fault's origin catches anything itself (see this
     * class's own KDoc). [stage] is a best-effort, structurally-derived
     * hint, not a guess dressed up as certainty: see [PipelineStage]'s own
     * KDoc for exactly what this runtime can and cannot honestly know
     * about where a given exception originated.
     */
    data class Failed(val stage: PipelineStage, val cause: Throwable) : ParkerRuntimeOutcome()
}

/**
 * A best-effort classification of which stage of the conversation pipeline
 * an exception caught by [ParkerRuntime.submitOwnerMessage] most likely
 * originated in. **Only [REASONING] is ever distinguished from
 * [UNKNOWN]**, because it is the only stage capable of throwing a
 * structurally-distinguishable exception type from this runtime's own
 * vantage point: `kotlinx.coroutines.TimeoutCancellationException` (the
 * model call exceeded `ModelReasoningProvider`'s own `timeoutMs`) or a
 * `java.io.IOException`/`java.net.http.HttpConnectTimeoutException`
 * (`LocalHttpModelInferenceClient`'s own real HTTP failure modes) are both
 * unambiguous, and both can only originate from the one network call this
 * entire pipeline makes. Every other exception -- from
 * `InMemoryConversationEngine`, `ResponseComposer`,
 * `LocalTextChannelDeliverTool`, or anywhere else -- is classified
 * [UNKNOWN] deliberately, not guessed at: this runtime has no tagged,
 * stage-labelled exception type to read from any of those components (none
 * exists, and inventing one would mean modifying frozen production code
 * this Unit is not authorised to touch), so reporting anything more
 * specific than [UNKNOWN] for them would be fabricating a fact this
 * runtime cannot actually observe.
 */
enum class PipelineStage {
    REASONING,
    UNKNOWN,
}
