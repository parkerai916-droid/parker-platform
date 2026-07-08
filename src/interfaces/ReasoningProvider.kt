package parker.core.interfaces

/**
 * Reasoning Provider contracts (Sprint 7, Stage 3 Implementation Unit),
 * implementing exactly the shapes approved by
 * `docs/architecture/REASONING_PROVIDER_CONTRACT_DESIGN.md` ("Reasoning
 * Provider Contract Design"), itself built on
 * `docs/architecture/REASONING_PROVIDER_ARCHITECTURE.md` ("Reasoning
 * Provider Architecture"). No concept here is new: every type below is
 * cited, section by section, in Contract Design Sections 2 and 3. This
 * file does not redesign, extend, or reinterpret either document -- it is
 * their literal Kotlin realisation.
 *
 * **Scope.** This unit implements the contract only: the [ReasoningProvider]
 * interface and its request/response shapes. It contains no concrete
 * provider implementation -- Contract Design Section 9 (Deferred Items)
 * explicitly defers provider implementations, prompt construction, model
 * selection, and routing to later, separate work.
 */

/**
 * Contract Design Section 2: a new type, not a reuse of anything defined
 * in `reasoning-context.md` despite the shared name -- an ordered list of
 * already-assembled context entries, each a plain prose string, with no
 * further internal structure imposed. Reasoning Context *assembly* remains
 * an unassigned responsibility (Contract Design Section 9); this type only
 * carries the already-assembled result.
 *
 * @param entries The ordered context entries. Each entry must be
 *   non-blank; an entirely empty list is permitted (Contract Design
 *   Section 2 -- a Turn may carry no prior context, e.g. the first Turn of
 *   a new Conversation).
 */
data class ReasoningContext(val entries: List<String>) {
    init {
        require(entries.all { it.isNotBlank() }) {
            "ReasoningContext entries must not be blank"
        }
    }
}

/**
 * Contract Design Section 2: the minimal request object passed to
 * [ReasoningProvider.reason] -- exactly two fields, deliberately excluding
 * any correlation or caller-Principal field (Contract Design Section 2:
 * the Reasoning Provider is a pure callee and has no need to know who is
 * calling it or why).
 *
 * @param turn The Turn to reason about, as produced by [ConversationEngine.submitTurn].
 * @param reasoningContext The already-assembled context for this Turn.
 */
data class ReasoningProviderRequest(
    val turn: Turn,
    val reasoningContext: ReasoningContext,
)

/**
 * Contract Design Section 3: the sealed response shape, exactly three
 * variants -- no more, no fewer.
 *
 * **No `Failed` variant.** `docs/architecture/19-conversation-engine.md`
 * Section 11 (Failure Model) reinforces this exclusion: it assigns
 * failure-surfacing to the calling component (the Conversation Engine),
 * not to this response shape. Correspondingly, [ReasoningProvider.reason]
 * may still fault -- throw -- for genuine implementation-level failures
 * (timeout, crash, malformed model output, inability to reason) that fall
 * outside this sealed type. [NoAction] means a confident semantic
 * determination that no action is warranted; it must never be used as a
 * catch-all for such failures.
 */
sealed class ReasoningProviderResponse {

    /**
     * The Reasoning Provider has determined the owner's Turn expresses an
     * intent that should be pursued as a goal. Maps, at the caller's
     * discretion and outside this unit's scope, toward a future
     * `PlanningRequest.goal`.
     *
     * @param text The goal, expressed in plain prose. Must be non-blank.
     */
    data class Goal(val text: String) : ReasoningProviderResponse() {
        init {
            require(text.isNotBlank()) { "Goal.text must not be blank" }
        }
    }

    /**
     * The Reasoning Provider has determined the owner's Turn warrants a
     * direct conversational reply, with no goal to pursue. Maps, at the
     * caller's discretion and outside this unit's scope, toward a future
     * `OutboundParkerResponse.text`.
     *
     * @param text The reply, expressed in plain prose. Must be non-blank.
     */
    data class Reply(val text: String) : ReasoningProviderResponse() {
        init {
            require(text.isNotBlank()) { "Reply.text must not be blank" }
        }
    }

    /**
     * The Reasoning Provider has confidently determined that the owner's
     * Turn warrants neither a goal nor a reply. Not a failure signal --
     * see the class-level documentation above.
     */
    object NoAction : ReasoningProviderResponse()
}

/**
 * Contract Design Section 1: the single public interface for invoking a
 * Reasoning Provider, mirroring [ConversationEngine]'s minimalism (one
 * method, one request type, one response type).
 *
 * An implementation interprets the supplied [Turn] in light of the
 * supplied [ReasoningContext] and returns a [ReasoningProviderResponse].
 * It is a pure callee: it never executes or authorises actions, never
 * accesses tools directly, never invokes `PlannerRuntime`,
 * `ExecutionPipeline`, `PermissionEngine`, `MemoryStore`, or `WorldModel`,
 * and never remembers continuity internally -- any continuity across
 * chained invocations is supplied by the caller re-presenting context, not
 * retained by the provider (Architecture Sections 4 and 7).
 */
interface ReasoningProvider {
    suspend fun reason(request: ReasoningProviderRequest): ReasoningProviderResponse
}
