package parker.core.interfaces

import java.time.Instant

/**
 * Communication Runtime contracts (Sprint 7, Unit C1), implementing exactly
 * the shapes approved by `docs/architecture/COMMUNICATION_CONTRACT_DESIGN.md`
 * ("Communication Contract Design"), itself built on
 * `docs/architecture/COMMUNICATION_CHANNEL_ARCHITECTURE.md`. No concept here
 * is new: every type below is cited, section by section, in Contract
 * Design's own Section 12 (Public Contract Inventory) / Section 13
 * (Self-Traceability Review). This file does not redesign, extend, or
 * reinterpret either document -- it is their literal Kotlin realisation.
 *
 * **Scope, restated from Contract Design's own Conclusion.** A first
 * implementation unit "may reasonably stop at: `CommunicationIntake`
 * accepting or rejecting inbound messages and making accepted ones
 * inspectable... with actual Cognition consumption and actual Tool-based
 * response delivery following once their own respective open items are
 * resolved by a future, separately-scoped unit." Concretely, this means:
 *
 * - [CommunicationIntake] performs exactly the two structural checks
 *   Section 6 names -- nothing more. It does not interpret
 *   [InboundOwnerMessage.text], does not decide whether a message implies an
 *   action, and does not itself construct a `PlanningRequest` or an
 *   `ExecutionRequest`.
 * - **This file deliberately does NOT introduce any dependency on, or call
 *   into, `ExecutionPipeline`, `PermissionEngine`, `ToolRegistry`,
 *   `PlannerRuntime`, `AgentRuntime`, `MemoryStore`, or `WorldModel`.**
 *   Constructing an `ExecutionRequest` and submitting it through
 *   `ExecutionPipeline` is Response Delivery's job (Contract Design Section
 *   7) and, further upstream of that, Cognition's job (Section 9) -- both
 *   explicitly named by Contract Design's Conclusion as open items a first
 *   unit must NOT resolve. `ExecutionRequest` itself has no field shaped for
 *   arbitrary message content (Section 7's own disclosed, unresolved
 *   tension), so constructing one here would mean inventing a resolution to
 *   an open question this document reserves for a future, separately
 *   authorised unit -- not this one.
 */

/**
 * Contract Design Section 4: a single, non-blank string value, matching
 * [PrincipalId]/[ModuleId]/[RequestId]'s identical established shape. Minted
 * by the Communication Channel itself, at the moment it constructs an
 * [InboundOwnerMessage] -- never minted or overwritten by
 * [CommunicationIntake] (Section 4, Section 6).
 */
@JvmInline
value class CorrelationId(val value: String) {
    init {
        require(value.isNotBlank()) { "CorrelationId must not be blank" }
    }
}

/**
 * Contract Design Section 2: the inbound message, field-shaped. Constructed
 * once, by the Communication Channel, from whatever the owner said;
 * submitted whole to [CommunicationIntake.submitInboundMessage]; not
 * retained, mutated, or re-submitted by the channel once submitted.
 *
 * @param channelId The Communication Channel this message travelled
 *   through. A plain [ModuleId] -- Section 1 deliberately introduces no
 *   separate `ChannelId` type; a Communication Channel's identity is its
 *   `ModuleId`.
 * @param senderPrincipalId The owner's own, already-registered [Principal]
 *   -- never the channel's own identity (Section 5). Resolved and checked
 *   by [CommunicationIntake] before acceptance.
 * @param text The message content, non-blank. This contract does not
 *   require every channel to originate text directly -- a future speech
 *   channel's audio must already have become text by the time it reaches
 *   this boundary.
 * @param timestamp When the owner sent the message, not when it happened to
 *   be processed.
 * @param correlationId Minted by the channel before submission (Section 4)
 *   -- present on the message itself, not assigned by intake.
 * @param metadata A channel-specific, non-authoritative extension point,
 *   mirroring `ParkerEvent.metadata`/`ToolDescriptor`'s identical existing
 *   precedent. Nothing this contract, [CommunicationIntake], or any
 *   downstream consumer relies on for a trust or planning decision may live
 *   only here. Defaults to empty.
 */
data class InboundOwnerMessage(
    val channelId: ModuleId,
    val senderPrincipalId: PrincipalId,
    val text: String,
    val timestamp: Instant,
    val correlationId: CorrelationId,
    val metadata: Map<String, String> = emptyMap(),
) {
    init {
        require(text.isNotBlank()) { "InboundOwnerMessage.text must not be blank" }
    }
}

/**
 * Contract Design Section 3: the outbound response, field-shaped, kept as a
 * **distinct type** from [InboundOwnerMessage] rather than a single
 * direction-flagged type -- mirroring [CandidateMemory]/[MemoryRecord] and
 * [WorldObservation]/[WorldBelief]'s identical precedent of keeping
 * structurally-similar but semantically-distinct lifecycle stages
 * structurally distinct, precisely so a future implementation cannot
 * accidentally deliver an inbound message back out unchanged, or treat an
 * outbound response as if it still carried the owner's own authority.
 *
 * @param channelId Which channel to deliver through; normally copied from
 *   the [InboundOwnerMessage] this response answers.
 * @param senderPrincipalId **Not** the channel's own identity -- the
 *   [Principal] responsible for the response's content, whatever Runtime
 *   component produced it (Section 9). This contract does not decide which
 *   specific Principal that is; it requires only that it be a real,
 *   resolved [PrincipalId], threaded through explicitly, never a hardcoded
 *   constant -- mirroring the discipline gap #49's Planner Runtime fix
 *   already established.
 * @param text The response content, non-blank.
 * @param timestamp When the response was produced.
 * @param correlationId Copied unchanged from the [InboundOwnerMessage] that
 *   prompted this response (Section 4), so the channel can pair a reply
 *   with the message that elicited it.
 * @param metadata Same non-authoritative extension-point discipline as
 *   [InboundOwnerMessage.metadata]. Defaults to empty.
 */
data class OutboundParkerResponse(
    val channelId: ModuleId,
    val senderPrincipalId: PrincipalId,
    val text: String,
    val timestamp: Instant,
    val correlationId: CorrelationId,
    val metadata: Map<String, String> = emptyMap(),
) {
    init {
        require(text.isNotBlank()) { "OutboundParkerResponse.text must not be blank" }
    }
}

/**
 * Contract Design Section 6: the two-variant, **structural-only** outcome
 * of submitting an [InboundOwnerMessage] to [CommunicationIntake] -- mirrors
 * [TaskProposalDisposition]'s shape, not its five-way semantic richness.
 *
 * **Why a sealed disposition, not a throw.** [TaskProposalIntake] itself
 * returns a sealed [TaskProposalDisposition], and [CommunicationIntake]
 * mirrors that exact choice (rather than the throw-based pattern
 * [ModuleRegistry]/[IdentityService] use for their own operations) because
 * rejection here is an expected, routine outcome of ordinary channel
 * lifecycle timing (a channel briefly disabled during maintenance, a
 * just-removed Principal), not a caller-misuse condition.
 */
sealed class CommunicationIntakeDisposition {
    abstract val correlationId: CorrelationId

    /**
     * Both of [CommunicationIntake]'s structural checks (Section 6) passed.
     * [message] is the accepted [InboundOwnerMessage], unchanged. Acceptance
     * is a precondition, never a permission decision (Section 8) -- it must
     * never be read as approval of any action the message might eventually
     * be interpreted to request.
     */
    data class Accepted(
        override val correlationId: CorrelationId,
        val message: InboundOwnerMessage,
    ) : CommunicationIntakeDisposition()

    /**
     * At least one of [CommunicationIntake]'s two structural checks failed.
     * [reason] names which one, in plain language -- not a caller-misuse
     * exception, since a channel being briefly `DISABLED` or a Principal
     * having just been revoked are ordinary, expected timing conditions.
     */
    data class Rejected(
        override val correlationId: CorrelationId,
        val reason: String,
    ) : CommunicationIntakeDisposition() {
        init {
            require(reason.isNotBlank()) { "CommunicationIntakeDisposition.Rejected.reason must not be blank" }
        }
    }
}

/**
 * Contract Design Section 6: the single public interface for inbound
 * reception, mirroring [TaskProposalIntake]'s exact shape and minimalism
 * (one method, one payload type, one outcome type).
 *
 * Given an [InboundOwnerMessage], an implementation performs exactly two
 * structural checks -- nothing more, nothing interpretive:
 *
 * 1. **Is `message.channelId` a currently `ENABLED` module?** -- checked
 *    against `ModuleRegistry.getModuleStatus`. A message from an
 *    unregistered, `DISABLED`, or `REMOVED` channel is rejected.
 * 2. **Does `message.senderPrincipalId` resolve to a registered
 *    `Principal`?** -- checked against `IdentityService.resolve`, mirroring
 *    `InMemoryPlannerRuntime`/`InMemoryAgentRuntime`'s own established
 *    "resolve before acting" precedent. An unresolvable sender is rejected.
 *
 * If both checks pass, the message is accepted.
 *
 * **What an implementation must not do (Section 6):** interpret
 * `message.text`; decide whether the message implies an action; construct a
 * `PlanningRequest` or an `ExecutionRequest`; or itself call
 * `PlannerRuntime`, `AgentRuntime`, `MemoryStore`, or `WorldModel`.
 *
 * **What happens to an accepted message next is not decided by this
 * interface.** `CommunicationIntake` makes an accepted [InboundOwnerMessage]
 * available; the mechanism by which Cognition subsequently consumes it is a
 * genuine, disclosed open item for a future Contract Design pass (Section
 * 6, Section 14) -- not solved here, and does not block this interface's
 * own defined surface.
 *
 * **Dependencies.** An implementation's only two collaborators are
 * `ModuleRegistry` (channel-status check) and `IdentityService`
 * (sender-resolution check) -- no dependency on `ToolRegistry`,
 * `ExecutionPipeline`, `PlannerRuntime`, `AgentRuntime`, `MemoryStore`, or
 * `WorldModel` is introduced (Section 9).
 */
interface CommunicationIntake {
    suspend fun submitInboundMessage(message: InboundOwnerMessage): CommunicationIntakeDisposition
}
