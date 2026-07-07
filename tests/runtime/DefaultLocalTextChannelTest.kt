package parker.core.runtime

import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import parker.core.interfaces.CommunicationIntakeDisposition
import parker.core.interfaces.CorrelationId
import parker.core.interfaces.LocalTextChannel
import parker.core.interfaces.ModuleConnectivityDeclaration
import parker.core.interfaces.ModuleDescriptor
import parker.core.interfaces.ModuleId
import parker.core.interfaces.Principal
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.PrincipalStatus
import parker.core.interfaces.PrincipalType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Sprint 7, Unit C3. Proves the behaviour
 * `docs/architecture/LOCAL_TEXT_CHANNEL_CONTRACT_DESIGN.md` and
 * `docs/implementation/LOCAL_TEXT_CHANNEL_IMPLEMENTATION_PLAN.md` Section
 * 6 (Testing Strategy) require of [DefaultLocalTextChannel]: delegation
 * to [CommunicationIntake] exactly once per call, disposition
 * passthrough unchanged, construction-time validation ahead of that
 * call, distinct [CorrelationId] minting per IDR-001, timestamp/metadata
 * handling, thread safety, and the scope-discipline constructor proof.
 *
 * Every `channel` fixture below is declared as [LocalTextChannel] (the
 * interface), not [DefaultLocalTextChannel] (the concrete class) --
 * `submitOwnerText`'s default parameter values are declared on the
 * interface (Kotlin does not allow an override to redeclare them), so
 * calling with omitted `timestamp`/`metadata` arguments requires the
 * interface-typed reference. This also exercises the class exactly as
 * any real caller, holding only a [LocalTextChannel] reference, would.
 */
class DefaultLocalTextChannelTest {

    private val fixedInstant: Instant = Instant.parse("2026-07-07T12:00:00Z")
    private val channelModuleId = ModuleId("channel.local-text")
    private val owner = PrincipalId("user.owner")

    // --- Fake-isolated behaviour ---

    @Test
    fun `submitOwnerText constructs an InboundOwnerMessage using this channel's own ModuleId and submits it exactly once`() = runTest {
        val fake = FakeCommunicationIntake { message -> CommunicationIntakeDisposition.Accepted(message.correlationId, message) }
        val channel: LocalTextChannel = DefaultLocalTextChannel(channelModuleId, fake)

        channel.submitOwnerText("turn on the kitchen light", owner, fixedInstant, mapOf("k" to "v"))

        assertEquals(1, fake.submitInboundMessageCallCount)
        val submitted = fake.lastSubmittedMessage!!
        assertEquals(channelModuleId, submitted.channelId)
        assertEquals(owner, submitted.senderPrincipalId)
        assertEquals("turn on the kitchen light", submitted.text)
        assertEquals(fixedInstant, submitted.timestamp)
        assertEquals(mapOf("k" to "v"), submitted.metadata)
    }

    @Test
    fun `submitOwnerText returns an Accepted disposition from CommunicationIntake unchanged`() = runTest {
        val fake = FakeCommunicationIntake { message -> CommunicationIntakeDisposition.Accepted(message.correlationId, message) }
        val channel: LocalTextChannel = DefaultLocalTextChannel(channelModuleId, fake)

        val disposition = channel.submitOwnerText("hello", owner, fixedInstant)

        val accepted = assertIs<CommunicationIntakeDisposition.Accepted>(disposition)
        assertEquals(channelModuleId, accepted.message.channelId)
    }

    @Test
    fun `submitOwnerText returns a Rejected disposition from CommunicationIntake unchanged -- no exception, no reinterpretation`() = runTest {
        val fake = FakeCommunicationIntake { message -> CommunicationIntakeDisposition.Rejected(message.correlationId, "channel not enabled") }
        val channel: LocalTextChannel = DefaultLocalTextChannel(channelModuleId, fake)

        val disposition = channel.submitOwnerText("hello", owner, fixedInstant)

        val rejected = assertIs<CommunicationIntakeDisposition.Rejected>(disposition)
        assertEquals("channel not enabled", rejected.reason)
    }

    @Test
    fun `blank text fails at construction before CommunicationIntake is ever called`() = runTest {
        val fake = FakeCommunicationIntake { message -> CommunicationIntakeDisposition.Accepted(message.correlationId, message) }
        val channel: LocalTextChannel = DefaultLocalTextChannel(channelModuleId, fake)

        assertFailsWith<IllegalArgumentException> {
            channel.submitOwnerText("   ", owner, fixedInstant)
        }
        assertEquals(0, fake.submitInboundMessageCallCount)
    }

    @Test
    fun `each call mints a distinct CorrelationId`() = runTest {
        val fake = FakeCommunicationIntake { message -> CommunicationIntakeDisposition.Accepted(message.correlationId, message) }
        val channel: LocalTextChannel = DefaultLocalTextChannel(channelModuleId, fake)

        val first = assertIs<CommunicationIntakeDisposition.Accepted>(channel.submitOwnerText("first", owner, fixedInstant))
        val second = assertIs<CommunicationIntakeDisposition.Accepted>(channel.submitOwnerText("second", owner, fixedInstant))

        assertNotEquals(first.correlationId, second.correlationId)
    }

    @Test
    fun `a caller-supplied timestamp is threaded through unchanged`() = runTest {
        val fake = FakeCommunicationIntake { message -> CommunicationIntakeDisposition.Accepted(message.correlationId, message) }
        val channel: LocalTextChannel = DefaultLocalTextChannel(channelModuleId, fake)
        val suppliedTimestamp = Instant.parse("2020-01-01T00:00:00Z")

        channel.submitOwnerText("hello", owner, suppliedTimestamp)

        assertEquals(suppliedTimestamp, fake.lastSubmittedMessage!!.timestamp)
    }

    @Test
    fun `an omitted timestamp defaults to the current time`() = runTest {
        val fake = FakeCommunicationIntake { message -> CommunicationIntakeDisposition.Accepted(message.correlationId, message) }
        val channel: LocalTextChannel = DefaultLocalTextChannel(channelModuleId, fake)

        val before = Instant.now()
        channel.submitOwnerText(text = "hello", senderPrincipalId = owner)
        val after = Instant.now()

        val stamped = fake.lastSubmittedMessage!!.timestamp
        assertTrue(!stamped.isBefore(before) && !stamped.isAfter(after), "timestamp $stamped should fall between $before and $after")
    }

    @Test
    fun `supplied metadata reaches the constructed message unchanged`() = runTest {
        val fake = FakeCommunicationIntake { message -> CommunicationIntakeDisposition.Accepted(message.correlationId, message) }
        val channel: LocalTextChannel = DefaultLocalTextChannel(channelModuleId, fake)

        channel.submitOwnerText("hello", owner, fixedInstant, mapOf("confidence" to "0.9"))

        assertEquals(mapOf("confidence" to "0.9"), fake.lastSubmittedMessage!!.metadata)
    }

    @Test
    fun `omitted metadata defaults to empty`() = runTest {
        val fake = FakeCommunicationIntake { message -> CommunicationIntakeDisposition.Accepted(message.correlationId, message) }
        val channel: LocalTextChannel = DefaultLocalTextChannel(channelModuleId, fake)

        channel.submitOwnerText(text = "hello", senderPrincipalId = owner, timestamp = fixedInstant)

        assertTrue(fake.lastSubmittedMessage!!.metadata.isEmpty())
    }

    // --- Real, end-to-end integration (InMemoryCommunicationIntake + InMemoryModuleRegistry + InMemoryIdentityService) ---

    private fun channelDescriptor(moduleId: ModuleId) = ModuleDescriptor(
        moduleId = moduleId,
        name = "Local Text Channel",
        version = "1.0.0",
        toolsExposed = emptyList(),
        requiredPermissions = emptyList(),
        connectivityDeclaration = ModuleConnectivityDeclaration.LOCAL_ONLY,
    )

    private suspend fun enabledChannelIntake(moduleId: ModuleId): InMemoryCommunicationIntake {
        val resources = InMemoryResourceRegistry()
        val tools = InMemoryToolRegistry(resources)
        val moduleRegistry = InMemoryModuleRegistry(tools, resources)
        val id = moduleRegistry.register(channelDescriptor(moduleId))
        moduleRegistry.enable(id, PrincipalId("user.admin"))

        val identity = InMemoryIdentityService()
        identity.register(
            Principal(
                principalId = owner,
                principalType = PrincipalType.USER,
                displayName = "Owner",
                owner = null,
                status = PrincipalStatus.CREATED,
                createdAt = fixedInstant,
                lastSeenAt = fixedInstant,
            ),
        )
        identity.updateStatus(owner, PrincipalStatus.ACTIVE)

        return InMemoryCommunicationIntake(moduleRegistry, identity)
    }

    @Test
    fun `a valid submission through a real, enabled channel and a resolvable sender is accepted and inspectable`() = runTest {
        val intake = enabledChannelIntake(channelModuleId)
        val channel: LocalTextChannel = DefaultLocalTextChannel(channelModuleId, intake)

        val disposition = channel.submitOwnerText("turn on the kitchen light", owner, fixedInstant)

        val accepted = assertIs<CommunicationIntakeDisposition.Accepted>(disposition)
        assertEquals(listOf(accepted.message), intake.acceptedMessages())
        assertEquals(accepted.message, intake.acceptedMessageFor(accepted.correlationId))
    }

    @Test
    fun `a submission through a real but not-yet-ENABLED channel is rejected, not thrown`() = runTest {
        val resources = InMemoryResourceRegistry()
        val tools = InMemoryToolRegistry(resources)
        val moduleRegistry = InMemoryModuleRegistry(tools, resources)
        moduleRegistry.register(channelDescriptor(channelModuleId)) // registered, never enabled
        val identity = InMemoryIdentityService()
        val intake = InMemoryCommunicationIntake(moduleRegistry, identity)
        val channel: LocalTextChannel = DefaultLocalTextChannel(channelModuleId, intake)

        val disposition = channel.submitOwnerText("hello", owner, fixedInstant)

        assertIs<CommunicationIntakeDisposition.Rejected>(disposition)
    }

    // --- Thread safety ---

    @Test
    fun `concurrent valid submissions are all accepted, each with a distinct CorrelationId, with no lost updates`() = runBlocking {
        val intake = enabledChannelIntake(channelModuleId)
        val channel: LocalTextChannel = DefaultLocalTextChannel(channelModuleId, intake)
        val submissionCount = 50

        val results = List(submissionCount) { i ->
            async(Dispatchers.Default) {
                channel.submitOwnerText("message-$i", owner, fixedInstant)
            }
        }.awaitAll()

        assertTrue(results.all { it is CommunicationIntakeDisposition.Accepted }, "every concurrent valid submission should be accepted")
        val correlationIds = results.filterIsInstance<CommunicationIntakeDisposition.Accepted>().map { it.correlationId.value }
        assertEquals(submissionCount, correlationIds.toSet().size, "every CorrelationId minted concurrently must be distinct")
        assertEquals(submissionCount, intake.acceptedMessages().size)
    }

    // --- Scope discipline ---

    @Test
    fun `DefaultLocalTextChannel depends on only ModuleId and CommunicationIntake`() = runTest {
        // Structural proof, not a runtime assertion, mirroring InMemoryCommunicationIntakeTest's
        // own identical pattern: DefaultLocalTextChannel's constructor takes only a ModuleId and
        // a CommunicationIntake. If this class ever gained an ExecutionPipeline, ToolRegistry,
        // PermissionEngine, PlannerRuntime, AgentRuntime, MemoryStore, WorldModel, ModuleRegistry,
        // or IdentityService dependency, this two-argument construction would no longer compile --
        // the constructor signature itself is the guarantee, not this assertion.
        val fake = FakeCommunicationIntake { message -> CommunicationIntakeDisposition.Accepted(message.correlationId, message) }
        val channel: LocalTextChannel = DefaultLocalTextChannel(channelModuleId, fake)
        assertTrue(channel is LocalTextChannel)
    }
}
