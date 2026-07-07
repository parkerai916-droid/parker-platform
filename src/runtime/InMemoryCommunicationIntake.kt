package parker.core.runtime

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import parker.core.interfaces.CommunicationIntake
import parker.core.interfaces.CommunicationIntakeDisposition
import parker.core.interfaces.CorrelationId
import parker.core.interfaces.IdentityService
import parker.core.interfaces.InboundOwnerMessage
import parker.core.interfaces.ModuleRegistry
import parker.core.interfaces.ModuleStatus

/**
 * In-memory implementation of [CommunicationIntake], per
 * `docs/architecture/COMMUNICATION_CONTRACT_DESIGN.md`. Sprint 7, Unit C1.
 * Implements exactly what Contract Design specifies for Parker Runtime
 * intake:
 *
 * - **Channel-status check** (Section 6, check 1): `message.channelId` must
 *   resolve, via the injected [ModuleRegistry], to a currently `ENABLED`
 *   module. An unregistered channel (`getModuleStatus` returns `null`), or
 *   one that is `REGISTERED`-but-not-yet-`ENABLED`, `DISABLED`, or
 *   `REMOVED`, is rejected.
 * - **Sender-resolution check** (Section 6, check 2): `message.senderPrincipalId`
 *   must resolve to a registered `Principal` via the injected
 *   [IdentityService]. An unresolvable sender is rejected. Per Section 5,
 *   this implementation does not additionally require the resolved
 *   Principal's `principalType` to be `USER`, or its `status` to be
 *   `ACTIVE` -- Contract Design explicitly leaves that a caller decision it
 *   does not change (mirroring `IdentityService.resolve`'s own existing
 *   gap #37 precedent of not itself suppressing non-Active Principals).
 * - **Order.** The channel-status check runs before the sender-resolution
 *   check, matching Section 6's own numbered order exactly.
 * - **Acceptance is inspectable, not silently dropped.** Every accepted
 *   [InboundOwnerMessage] is retained in an internal, in-memory list,
 *   exposed via [acceptedMessages]/[acceptedMessageFor] -- observability
 *   methods outside the formal [CommunicationIntake] interface, mirroring
 *   `InMemoryMemoryStore.wasForgotten`'s identical "observability method
 *   outside the formal interface" precedent. This is exactly the shape
 *   Contract Design's own Conclusion names as sufficient for a first
 *   implementation unit ("making accepted ones inspectable").
 *
 * **NOT implemented, per Contract Design's own Conclusion (explicitly
 * deferred, not silently dropped):**
 * - No `ExecutionRequest` is constructed, and no `ExecutionPipeline`,
 *   `PermissionEngine`, or `ToolRegistry` call is made, by this class or by
 *   [CommunicationIntake] itself. Response Delivery (Contract Design
 *   Section 7) and everything upstream of it (Cognition, Section 9) are
 *   both explicitly out of scope for a first Communication Runtime unit.
 * - No `PlannerRuntime`, `AgentRuntime`, `MemoryStore`, or `WorldModel` call
 *   is made. Section 9 is explicit that engaging any of the four frozen
 *   subsystems is Cognition's own decision, downstream of an accepted
 *   message -- not `CommunicationIntake`'s.
 * - No mechanism exists yet, in this class or anywhere else in this
 *   repository, for Cognition to actually consume an accepted message --
 *   Section 6's own disclosed open item, unresolved by design.
 * - This class does not itself register a Communication Channel module, or
 *   implement any actual channel (a local text channel or otherwise) --
 *   both remain out of this Unit's scope (Contract Design Section 11,
 *   Conclusion).
 */
class InMemoryCommunicationIntake(
    private val moduleRegistry: ModuleRegistry,
    private val identityService: IdentityService,
) : CommunicationIntake {

    private val mutex = Mutex()
    private val accepted = mutableListOf<InboundOwnerMessage>()

    override suspend fun submitInboundMessage(message: InboundOwnerMessage): CommunicationIntakeDisposition {
        val channelStatus = moduleRegistry.getModuleStatus(message.channelId)
        if (channelStatus != ModuleStatus.ENABLED) {
            return CommunicationIntakeDisposition.Rejected(
                correlationId = message.correlationId,
                reason = "channel '${message.channelId.value}' is not an ENABLED module (status=$channelStatus)",
            )
        }

        val sender = identityService.resolve(message.senderPrincipalId)
        if (sender == null) {
            return CommunicationIntakeDisposition.Rejected(
                correlationId = message.correlationId,
                reason = "senderPrincipalId '${message.senderPrincipalId.value}' does not resolve to a registered Principal",
            )
        }

        mutex.withLock { accepted.add(message) }
        return CommunicationIntakeDisposition.Accepted(
            correlationId = message.correlationId,
            message = message,
        )
    }

    /**
     * Every [InboundOwnerMessage] accepted so far, in acceptance order.
     * Observability method outside the formal [CommunicationIntake]
     * interface, mirroring `InMemoryMemoryStore.wasForgotten`'s identical
     * precedent. Not a substitute for a real Cognition consumption
     * mechanism (Section 6's own disclosed open item) -- this exists only
     * so an accepted message's fate can be inspected by a test or a future
     * caller, not as a queue-consumption API.
     */
    suspend fun acceptedMessages(): List<InboundOwnerMessage> = mutex.withLock { accepted.toList() }

    /** The accepted [InboundOwnerMessage] carrying [correlationId], or `null` if none was accepted under it. */
    suspend fun acceptedMessageFor(correlationId: CorrelationId): InboundOwnerMessage? =
        mutex.withLock { accepted.firstOrNull { it.correlationId == correlationId } }
}
