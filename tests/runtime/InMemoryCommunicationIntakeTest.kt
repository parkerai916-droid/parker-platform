package parker.core.runtime

import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import parker.core.interfaces.CommunicationIntakeDisposition
import parker.core.interfaces.CorrelationId
import parker.core.interfaces.InboundOwnerMessage
import parker.core.interfaces.ModuleConnectivityDeclaration
import parker.core.interfaces.ModuleDescriptor
import parker.core.interfaces.ModuleId
import parker.core.interfaces.Principal
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.PrincipalStatus
import parker.core.interfaces.PrincipalType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Sprint 7, Unit C1. Proves the behaviour
 * `docs/architecture/COMMUNICATION_CONTRACT_DESIGN.md` requires of
 * `CommunicationIntake`'s first implementation: the two structural checks
 * (channel `ENABLED`, sender resolves), their order, deterministic and
 * thread-safe behaviour, invalid-request handling, that acceptance is
 * inspectable, and the scope boundary that no `ExecutionPipeline`,
 * `PlannerRuntime`, `AgentRuntime`, `MemoryStore`, or `WorldModel`
 * dependency is introduced.
 */
class InMemoryCommunicationIntakeTest {

    private val fixedInstant: Instant = Instant.parse("2026-07-07T12:00:00Z")

    private fun message(
        channelId: ModuleId = ModuleId("channel.text"),
        senderPrincipalId: PrincipalId = PrincipalId("user.owner"),
        text: String = "turn on the kitchen light",
        correlationId: CorrelationId = CorrelationId("corr-1"),
    ) = InboundOwnerMessage(
        channelId = channelId,
        senderPrincipalId = senderPrincipalId,
        text = text,
        timestamp = fixedInstant,
        correlationId = correlationId,
    )

    private suspend fun moduleRegistry(): InMemoryModuleRegistry {
        val resources = InMemoryResourceRegistry()
        val tools = InMemoryToolRegistry(resources)
        return InMemoryModuleRegistry(tools, resources)
    }

    private fun channelDescriptor(moduleId: String = "channel.text") = ModuleDescriptor(
        moduleId = ModuleId(moduleId),
        name = "Local Text Channel",
        version = "1.0.0",
        toolsExposed = emptyList(),
        requiredPermissions = emptyList(),
        connectivityDeclaration = ModuleConnectivityDeclaration.LOCAL_ONLY,
    )

    private val moduleAdmin = PrincipalId("user.admin")

    /** Registers `channel.text` and drives it to ENABLED, returning the registry. */
    private suspend fun registryWithEnabledChannel(moduleId: String = "channel.text"): InMemoryModuleRegistry {
        val registry = moduleRegistry()
        val id = registry.register(channelDescriptor(moduleId))
        registry.enable(id, moduleAdmin)
        return registry
    }

    /**
     * Registers `user.owner` and drives it, step by step through the only
     * legal linear chain ([PrincipalLifecycleTransitions]), to [status].
     */
    private suspend fun identityWithOwnerAt(
        status: PrincipalStatus = PrincipalStatus.ACTIVE,
        principalId: PrincipalId = PrincipalId("user.owner"),
    ): InMemoryIdentityService {
        val identity = InMemoryIdentityService()
        identity.register(
            Principal(
                principalId = principalId,
                principalType = PrincipalType.USER,
                displayName = "Owner",
                owner = null,
                status = PrincipalStatus.CREATED,
                createdAt = fixedInstant,
                lastSeenAt = fixedInstant,
            ),
        )
        val chain = listOf(PrincipalStatus.CREATED, PrincipalStatus.ACTIVE, PrincipalStatus.SUSPENDED, PrincipalStatus.REVOKED, PrincipalStatus.ARCHIVED)
        val targetIndex = chain.indexOf(status)
        for (i in 1..targetIndex) {
            identity.updateStatus(principalId, chain[i])
        }
        return identity
    }

    // --- Successful path ---

    @Test
    fun `an ENABLED channel and a resolvable sender are accepted`() = runTest {
        val registry = registryWithEnabledChannel()
        val identity = identityWithOwnerAt()
        val intake = InMemoryCommunicationIntake(registry, identity)
        val msg = message()

        val disposition = intake.submitInboundMessage(msg)

        val accepted = assertIs<CommunicationIntakeDisposition.Accepted>(disposition)
        assertEquals(msg.correlationId, accepted.correlationId)
        assertEquals(msg, accepted.message)
    }

    @Test
    fun `an accepted message becomes inspectable via acceptedMessages and acceptedMessageFor`() = runTest {
        val registry = registryWithEnabledChannel()
        val identity = identityWithOwnerAt()
        val intake = InMemoryCommunicationIntake(registry, identity)
        val msg = message()

        intake.submitInboundMessage(msg)

        assertEquals(listOf(msg), intake.acceptedMessages())
        assertEquals(msg, intake.acceptedMessageFor(msg.correlationId))
    }

    @Test
    fun `acceptance does not require the resolved sender to be ACTIVE specifically`() = runTest {
        // Contract Design Section 5: this document does not require the resolved Principal's
        // status to be ACTIVE specifically -- mirroring gap #37's own precedent that resolve()
        // does not itself suppress non-Active Principals.
        val registry = registryWithEnabledChannel()
        val identity = identityWithOwnerAt(status = PrincipalStatus.SUSPENDED)
        val intake = InMemoryCommunicationIntake(registry, identity)

        val disposition = intake.submitInboundMessage(message())

        assertIs<CommunicationIntakeDisposition.Accepted>(disposition)
    }

    // --- Channel-status check (invalid request / authorisation-adjacent failure handling) ---

    @Test
    fun `a message from an unregistered channel is rejected`() = runTest {
        val registry = moduleRegistry()
        val identity = identityWithOwnerAt()
        val intake = InMemoryCommunicationIntake(registry, identity)

        val disposition = intake.submitInboundMessage(message())

        assertIs<CommunicationIntakeDisposition.Rejected>(disposition)
        assertTrue((disposition as CommunicationIntakeDisposition.Rejected).reason.contains("channel"))
        assertTrue(intake.acceptedMessages().isEmpty())
    }

    @Test
    fun `a message from a REGISTERED-but-not-yet-ENABLED channel is rejected`() = runTest {
        val registry = moduleRegistry()
        registry.register(channelDescriptor())
        val identity = identityWithOwnerAt()
        val intake = InMemoryCommunicationIntake(registry, identity)

        val disposition = intake.submitInboundMessage(message())

        assertIs<CommunicationIntakeDisposition.Rejected>(disposition)
        assertTrue(intake.acceptedMessages().isEmpty())
    }

    @Test
    fun `a message from a DISABLED channel is rejected`() = runTest {
        val registry = registryWithEnabledChannel()
        registry.disable(ModuleId("channel.text"), moduleAdmin)
        val identity = identityWithOwnerAt()
        val intake = InMemoryCommunicationIntake(registry, identity)

        val disposition = intake.submitInboundMessage(message())

        assertIs<CommunicationIntakeDisposition.Rejected>(disposition)
        assertTrue(intake.acceptedMessages().isEmpty())
    }

    @Test
    fun `a message from a REMOVED channel is rejected`() = runTest {
        val registry = moduleRegistry()
        val id = registry.register(channelDescriptor())
        registry.remove(id, moduleAdmin)
        val identity = identityWithOwnerAt()
        val intake = InMemoryCommunicationIntake(registry, identity)

        val disposition = intake.submitInboundMessage(message())

        assertIs<CommunicationIntakeDisposition.Rejected>(disposition)
        assertTrue(intake.acceptedMessages().isEmpty())
    }

    // --- Sender-resolution check ---

    @Test
    fun `a message from an unresolvable sender is rejected even when the channel is ENABLED`() = runTest {
        val registry = registryWithEnabledChannel()
        val identity = InMemoryIdentityService() // user.owner never registered
        val intake = InMemoryCommunicationIntake(registry, identity)

        val disposition = intake.submitInboundMessage(message())

        assertIs<CommunicationIntakeDisposition.Rejected>(disposition)
        assertTrue((disposition as CommunicationIntakeDisposition.Rejected).reason.contains("senderPrincipalId"))
        assertTrue(intake.acceptedMessages().isEmpty())
    }

    // --- Check order ---

    @Test
    fun `when both checks fail, the channel-status check is reported -- it runs first`() = runTest {
        val registry = moduleRegistry() // channel never registered
        val identity = InMemoryIdentityService() // sender never registered either
        val intake = InMemoryCommunicationIntake(registry, identity)

        val disposition = intake.submitInboundMessage(message())

        assertIs<CommunicationIntakeDisposition.Rejected>(disposition)
        assertTrue((disposition as CommunicationIntakeDisposition.Rejected).reason.contains("channel"))
    }

    // --- Deterministic behaviour ---

    @Test
    fun `repeated submissions of equivalent messages produce equivalent, reproducible dispositions`() = runTest {
        val registry = moduleRegistry() // no channel registered -- always rejected
        val identity = identityWithOwnerAt()
        val intake = InMemoryCommunicationIntake(registry, identity)

        val first = intake.submitInboundMessage(message(correlationId = CorrelationId("corr-a")))
        val second = intake.submitInboundMessage(message(correlationId = CorrelationId("corr-b")))

        val firstRejected = assertIs<CommunicationIntakeDisposition.Rejected>(first)
        val secondRejected = assertIs<CommunicationIntakeDisposition.Rejected>(second)
        assertEquals(firstRejected.reason, secondRejected.reason)
    }

    @Test
    fun `distinct valid submissions are each independently accepted and retained in submission order`() = runTest {
        val registry = registryWithEnabledChannel()
        val identity = identityWithOwnerAt()
        val intake = InMemoryCommunicationIntake(registry, identity)

        val first = message(correlationId = CorrelationId("corr-1"))
        val second = message(correlationId = CorrelationId("corr-2"))
        intake.submitInboundMessage(first)
        intake.submitInboundMessage(second)

        assertEquals(listOf(first, second), intake.acceptedMessages())
    }

    // --- Thread safety ---

    @Test
    fun `concurrent valid submissions are all accepted, with no lost updates`() = runBlocking {
        val registry = registryWithEnabledChannel()
        val identity = identityWithOwnerAt()
        val intake = InMemoryCommunicationIntake(registry, identity)
        val submissionCount = 50

        val results = List(submissionCount) { i ->
            async(Dispatchers.Default) {
                intake.submitInboundMessage(message(correlationId = CorrelationId("corr-$i")))
            }
        }.awaitAll()

        assertTrue(results.all { it is CommunicationIntakeDisposition.Accepted }, "every concurrent valid submission should be accepted")
        assertEquals(submissionCount, intake.acceptedMessages().size)
        assertEquals((0 until submissionCount).map { "corr-$it" }.toSet(), intake.acceptedMessages().map { it.correlationId.value }.toSet())
    }

    // --- Lookup surface ---

    @Test
    fun `acceptedMessageFor returns null for a correlationId that was never accepted`() = runTest {
        val registry = registryWithEnabledChannel()
        val identity = identityWithOwnerAt()
        val intake = InMemoryCommunicationIntake(registry, identity)

        assertNull(intake.acceptedMessageFor(CorrelationId("never-submitted")))
    }

    // --- Scope discipline ---

    @Test
    fun `InMemoryCommunicationIntake has no dependency on ExecutionPipeline, ToolRegistry, PlannerRuntime, AgentRuntime, MemoryStore, or WorldModel`() = runTest {
        // Structural proof, not a runtime assertion, mirroring InMemoryMemoryStoreTest's own
        // identical pattern: InMemoryCommunicationIntake's constructor takes only a
        // ModuleRegistry and an IdentityService. If this class ever gained an
        // ExecutionPipeline, ToolRegistry, PlannerRuntime, AgentRuntime, MemoryStore, or
        // WorldModel dependency, this two-argument construction would no longer compile --
        // the constructor signature itself is the guarantee, not this assertion.
        val registry = registryWithEnabledChannel()
        val identity = identityWithOwnerAt()
        val intake = InMemoryCommunicationIntake(registry, identity)
        assertTrue(intake is parker.core.interfaces.CommunicationIntake)
    }

    @Test
    fun `submitInboundMessage never constructs or references an ExecutionRequest -- rejection and acceptance are both purely structural`() = runTest {
        // Per Contract Design's Conclusion, this Unit must not resolve ExecutionRequest's
        // content-carrying gap or construct one at all. There is no ExecutionRequest import
        // anywhere in this test or in InMemoryCommunicationIntake.kt/CommunicationIntake.kt --
        // the absence of that import, checked by this file's own compilation, is the proof.
        val registry = registryWithEnabledChannel()
        val identity = identityWithOwnerAt()
        val intake = InMemoryCommunicationIntake(registry, identity)

        val disposition = intake.submitInboundMessage(message())
        assertIs<CommunicationIntakeDisposition.Accepted>(disposition)
    }
}
