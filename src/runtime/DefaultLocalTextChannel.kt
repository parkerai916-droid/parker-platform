package parker.core.runtime

import java.time.Instant
import java.util.UUID
import parker.core.interfaces.CommunicationIntake
import parker.core.interfaces.CommunicationIntakeDisposition
import parker.core.interfaces.CorrelationId
import parker.core.interfaces.InboundOwnerMessage
import parker.core.interfaces.LocalTextChannel
import parker.core.interfaces.ModuleId
import parker.core.interfaces.PrincipalId

/**
 * Default implementation of [LocalTextChannel], per
 * `docs/architecture/LOCAL_TEXT_CHANNEL_CONTRACT_DESIGN.md` and
 * `docs/implementation/LOCAL_TEXT_CHANNEL_IMPLEMENTATION_PLAN.md`
 * (Sprint 7, Unit C3).
 *
 * **Dependencies — exactly two, per the Contract Design and the
 * Implementation Plan's own Unit C3 acceptance criteria.** [moduleId] is
 * this channel's own fixed identity (Contract Design Section 1, "one
 * channel, one `ModuleId`"), supplied by the caller who already
 * registered it with `ModuleRegistry` — this class does not register
 * itself, does not call `ModuleRegistry`, and does not check its own
 * `ModuleStatus`. [communicationIntake] is this channel's sole
 * downstream collaborator. No dependency on `ExecutionPipeline`,
 * `ExecutionRequest`, `PlannerRuntime`, `AgentRuntime`, `ToolRegistry`,
 * `ResourceRegistry`, `MemoryStore`, `WorldModel`, `PermissionEngine`,
 * `IdentityService`, or `ModuleRegistry` is introduced.
 *
 * **`CorrelationId` minting (IDR-001).** Each call mints a fresh
 * [CorrelationId] from a randomly generated UUID
 * (`java.util.UUID.randomUUID()`), per the approved Implementation
 * Decision Record IDR-001: this channel has no stable parent identifier
 * to derive one from deterministically (unlike `AgentRunId`/`TaskProposalId`'s
 * parent-derived scheme), and a random value requires no shared,
 * mutable state.
 *
 * **No channel-status or sender-resolution check of its own.** Both
 * remain [CommunicationIntake.submitInboundMessage]'s alone (Contract
 * Design Section 6/9) — this class does not duplicate either check, and
 * does not translate a `Rejected` disposition into an exception; it is
 * returned unchanged.
 *
 * **Thread safety.** This class holds no mutable state of its own —
 * [moduleId] and [communicationIntake] are both immutable, and
 * `UUID.randomUUID()` requires no shared state to remain safe under
 * concurrent calls. No lock or other exclusion mechanism is needed or
 * introduced.
 */
class DefaultLocalTextChannel(
    private val moduleId: ModuleId,
    private val communicationIntake: CommunicationIntake,
) : LocalTextChannel {

    override suspend fun submitOwnerText(
        text: String,
        senderPrincipalId: PrincipalId,
        timestamp: Instant,
        metadata: Map<String, String>,
    ): CommunicationIntakeDisposition {
        val message = InboundOwnerMessage(
            channelId = moduleId,
            senderPrincipalId = senderPrincipalId,
            text = text,
            timestamp = timestamp,
            correlationId = CorrelationId(UUID.randomUUID().toString()),
            metadata = metadata,
        )
        return communicationIntake.submitInboundMessage(message)
    }
}
