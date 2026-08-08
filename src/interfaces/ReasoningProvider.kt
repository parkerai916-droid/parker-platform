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
 *
 * **Amendment 1 (ReasoningSubject).** `ReasoningProviderRequest.turn: Turn`
 * is replaced by `subject: ReasoningSubject`, a closed, two-case sealed
 * selector (`OfTurn`, `OfEvidenceAnalysisRequest`), implementing exactly
 * `REASONING_PROVIDER_CONTRACT_DESIGN.md`'s own Amendment 1. `Turn`,
 * `ReasoningContext`, `ReasoningProviderResponse`, and the
 * [ReasoningProvider] interface itself are unmodified by this amendment.
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
 * Contract Design Section 2, **Amendment 1** (`REASONING_PROVIDER_CONTRACT_DESIGN.md`,
 * Amendment 1 -- ReasoningSubject): a closed, two-case sealed selector,
 * generalising [ReasoningProviderRequest]'s own subject to the
 * caller-agnostic reasoning subject `reasoning-context.md`'s own
 * constitutional-tier authority already defines ("a task"), while
 * leaving [Turn] itself, and Conversation Engine's exclusive
 * construction of it, completely unmodified.
 *
 * Frozen properties, none of which any future revision may weaken:
 * behaviour-free (no operation beyond ordinary structural operations);
 * closed to exactly these two cases -- [OfTurn], [OfEvidenceAnalysisRequest]
 * -- never a third without a further Contract Design amendment; owns no
 * data beyond the one selected subject value; introduces no new
 * responsibility to [ReasoningProvider] itself; grants no acceptance,
 * persistence, retrieval, reasoning, confidence, evidential-state,
 * ownership, or authorisation authority of its own; does not modify
 * [Turn] or [EvidenceAnalysisRequest] -- both remain reused, unmodified;
 * is not a generic, reusable union mechanism; creates no independent
 * dependency entitlement for any other subsystem. Owned by the
 * Reasoning Provider Contract Design -- never by Conversation Engine,
 * never by Evidence Intelligence, and never left ownerless.
 */
sealed class ReasoningSubject {

    /**
     * Wraps [Turn] unchanged (`CONVERSATION_ENGINE_CONTRACT_DESIGN.md`
     * Section 2). Conversation Engine remains the only component that
     * ever constructs a [Turn]; this case only references an
     * already-constructed one.
     */
    data class OfTurn(val turn: Turn) : ReasoningSubject()

    /**
     * Wraps [EvidenceAnalysisRequest] unchanged
     * (`EVIDENCE_INTELLIGENCE_CONTRACT_DESIGN.md` Section 4), reused
     * exactly as CDR-007 already authorises Evidence Intelligence to
     * orchestrate [ReasoningProvider] "as internal analytical
     * mechanisms." `EvidenceAnalysisRequest` remains owned by Evidence
     * Intelligence; this case only references it.
     */
    data class OfEvidenceAnalysisRequest(val request: EvidenceAnalysisRequest) : ReasoningSubject()
}

/**
 * Contract Design Section 2: the minimal request object passed to
 * [ReasoningProvider.reason] -- exactly two fields, deliberately excluding
 * any correlation or caller-Principal field (Contract Design Section 2:
 * the Reasoning Provider is a pure callee and has no need to know who is
 * calling it or why).
 *
 * @param subject **Amendment 1.** The reasoning subject, one of
 *   [ReasoningSubject]'s two closed cases -- replaces the original
 *   `turn: Turn` field.
 * @param reasoningContext The already-assembled context for this
 *   invocation. **Amendment 1 invariant:** this top-level field is the
 *   sole [ReasoningContext] value [ReasoningProvider.reason] ever
 *   consults, for every case of [subject], including
 *   [ReasoningSubject.OfEvidenceAnalysisRequest] -- whatever value
 *   `EvidenceAnalysisRequest.reasoningContext` carries internally, for
 *   Evidence Intelligence's own purposes, is not read, merged, or
 *   otherwise given any effect on this invocation.
 */
data class ReasoningProviderRequest(
    val subject: ReasoningSubject,
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
     * The Reasoning Provider has determined the owner's Turn is a direct,
     * unambiguous instruction to remember a specific, stated proposition
     * (Parker Conversational Memory Bridge, Admission Unit;
     * `docs/governance/PROGRAMME_3_EXPLICIT_OWNER_INSTRUCTION_PROMOTION_EXCEPTION_SCOPE_LOCK_CLARIFICATION.md`
     * Guarantee 2). This is a speech-act (intent) classification, exactly
     * like [Goal] versus [Reply] -- it carries no confidence, importance,
     * or evidential-weight judgment of any kind, and never may. Where any
     * doubt exists whether the owner intended such an instruction, [Reply]
     * or [NoAction] is the correct classification instead, never this one.
     *
     * @param text The proposition the owner asked to be remembered,
     *   expressed in plain prose, extracted unchanged from the owner's own
     *   words. Must be non-blank.
     */
    data class Remember(val text: String) : ReasoningProviderResponse() {
        init {
            require(text.isNotBlank()) { "Remember.text must not be blank" }
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
 * `ExecutionPipeline`, `PermissionEngine`, `KnowledgeStore`, or `WorldModel`,
 * and never remembers continuity internally -- any continuity across
 * chained invocations is supplied by the caller re-presenting context, not
 * retained by the provider (Architecture Sections 4 and 7).
 */
interface ReasoningProvider {
    suspend fun reason(request: ReasoningProviderRequest): ReasoningProviderResponse
}
