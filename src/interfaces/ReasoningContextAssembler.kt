package parker.core.interfaces

/**
 * The Resolved Inbound Envelope
 * (`docs/architecture/CONVERSATION_CONTINUITY_CONTRACT_DESIGN.md`, "the
 * Continuity Contract Design", Sections 6 and 8; Sprint 11 Unit 5).
 * Pairs an [InboundOwnerMessage] with the [ConversationId]
 * [ConversationEngine.resolveConversationId] has already, authoritatively,
 * decided for it -- before any [Turn] exists. Constructed exactly once per
 * inbound message, by the Production Composition Root
 * ([parker.composition.ParkerRuntime]), immediately after resolution and
 * before [ReasoningContextAssembler.assemble] is invoked. No other
 * component constructs or holds one.
 *
 * **Deliberately does not carry `isNewConversation` or any other
 * disposition-shaped field** (Continuity Contract Design Section 6): this
 * envelope commits to nothing about whether [conversationId] already had
 * a Conversation recorded against it -- that remains
 * [ConversationEngine.submitTurn]'s own, sole, later determination, from
 * its own owned state. This envelope's only role is making the resolved
 * identifier's *value* available before a Turn exists.
 *
 * @param message The original, unmutated inbound message.
 * @param conversationId The identifier [ConversationEngine.resolveConversationId]
 *   already produced for [message]'s own continuity key.
 */
data class ResolvedInboundMessage(
    val message: InboundOwnerMessage,
    val conversationId: ConversationId,
)

/**
 * Reasoning Context Assembler contract, originally Sprint 11 Unit 3,
 * revised by the Continuity Contract Design (Sprint 11 Unit 5) Sections
 * 4.4, 5, 6, and 11 for the input-shape change described below. Built on
 * `docs/architecture/PRODUCTION_REASONING_CONTEXT_CONTRACT_DESIGN.md`
 * ("the Reasoning Context Contract Design").
 *
 * One operation, mirroring this codebase's own established convention --
 * [ConversationEngine], [ReasoningProvider], and [CommunicationIntake]
 * are each exactly one or two methods, never more (Reasoning Context
 * Contract Design Section 3).
 *
 * **One input, revised: [ResolvedInboundMessage], not a bare
 * [InboundOwnerMessage].** This is an additive change to this interface's
 * input shape only -- not a redesign of its responsibilities, ownership,
 * determinism, statelessness, or side-effect-freedom, all of which are
 * unchanged below. It exists because Question 2 of the Continuity
 * Contract Design (making a `ConversationId` available before a Turn
 * exists) requires this Assembler to be able to read the already-resolved
 * identifier -- it still runs strictly before
 * [ConversationEngine.submitTurn] constructs a `Turn`; no `Turn` exists
 * yet at the point an implementation of this interface runs, exactly as
 * before this revision.
 *
 * **This interface never calls [ConversationEngine], directly or
 * indirectly, in any implementation it authorises** (Continuity Contract
 * Design Section 4.4, Frozen Fact 4): the [ConversationId] an
 * implementation reads off [ResolvedInboundMessage] was resolved entirely
 * by the Production Composition Root, before this interface's [assemble]
 * was ever invoked. An implementation must perform no lookup, no
 * resolution, and no mutation of that value -- it only reads it, exactly
 * as it already reads other already-resolved inputs (an `IdentityService`
 * resolution result, a `ToolRegistry` catalogue).
 *
 * **One output:** a [ReasoningContext] -- unchanged.
 *
 * **Ownership (Reasoning Context Scope Lock Section 4, restated here as
 * the contract's own binding term).** An implementation of this interface
 * is the sole constructor of [ReasoningContext] in production. The
 * Production Composition Root ([parker.composition.ParkerRuntime]) is the
 * sole production invoker of [assemble].
 *
 * **Contract principles (Reasoning Context Contract Design Section 5),
 * binding on every implementation, unchanged by this revision:**
 * deterministic; stateless (no mutable field retained between calls);
 * side-effect free (never writes to Memory, the World Model,
 * `IdentityService`, `ToolRegistry`, `ConversationEngine`, or anywhere
 * else); assembles a projection only -- it does not cache, persist, plan,
 * invoke a Tool, invoke the Permission Engine, or invoke the Execution
 * Pipeline.
 *
 * **Failure behaviour (Reasoning Context Contract Design Section 6),
 * unchanged.** A genuine failure during [assemble] is not caught here --
 * it propagates unchanged to the Production Composition Root's own
 * existing outer `try`/`catch` (`ParkerRuntime.submitOwnerMessage`).
 */
fun interface ReasoningContextAssembler {
    suspend fun assemble(resolvedMessage: ResolvedInboundMessage): ReasoningContext
}
