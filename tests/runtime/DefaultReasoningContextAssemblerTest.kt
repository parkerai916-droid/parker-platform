package parker.core.runtime

import kotlinx.coroutines.test.runTest
import parker.core.interfaces.ConversationId
import parker.core.interfaces.CorrelationId
import parker.core.interfaces.InboundOwnerMessage
import parker.core.interfaces.ModuleId
import parker.core.interfaces.Principal
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.PrincipalStatus
import parker.core.interfaces.PrincipalType
import parker.core.interfaces.ResolvedInboundMessage
import parker.core.interfaces.ToolDescriptor
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Sprint 11, Unit 3 acceptance test for [DefaultReasoningContextAssembler],
 * revised Sprint 11 Unit 5 (Conversation Continuity Implementation) for
 * the [ResolvedInboundMessage] input-shape change
 * (`docs/architecture/CONVERSATION_CONTINUITY_CONTRACT_DESIGN.md`
 * Sections 6, 11). Covers the original nine items this Unit's own task
 * instructions named -- successful assembly, immutable output, empty
 * optional inputs, requesting principal identity, available tool
 * descriptions, current time handling, dependency failures, assembler
 * failure behaviour, repeated calls producing independent
 * [parker.core.interfaces.ReasoningContext] instances -- plus one
 * structural dependency test, plus one new item added by Unit 5: the
 * resolved `ConversationId` is read and rendered, with no lookup, no
 * resolution, and no mutation.
 *
 * [FakeIdentityService] and [FakeToolRegistry] are used throughout, never
 * a real [InMemoryIdentityService]/[InMemoryToolRegistry] -- this file
 * exercises [DefaultReasoningContextAssembler] in isolation, exactly as
 * [ConversationTurnReasoningCoordinatorTest] and [ResponseComposerTest]
 * already do for their own subjects.
 */
class DefaultReasoningContextAssemblerTest {

    private val ownerPrincipalId = PrincipalId("user.owner-assembler-test")

    private fun message(
        senderPrincipalId: PrincipalId = ownerPrincipalId,
        text: String = "good morning parker",
        channelId: String = "channel.local-text-assembler-test",
        timestamp: Instant = Instant.parse("2026-01-01T09:30:00Z"),
        correlationId: String = "corr-assembler-${System.nanoTime()}",
    ) = InboundOwnerMessage(
        channelId = ModuleId(channelId),
        senderPrincipalId = senderPrincipalId,
        text = text,
        timestamp = timestamp,
        correlationId = CorrelationId(correlationId),
    )

    /**
     * Wraps [message] into the [ResolvedInboundMessage] envelope this
     * Assembler now requires -- mirroring what `ParkerRuntime` constructs
     * in production after calling `ConversationEngine.resolveConversationId`.
     * [conversationId] defaults to an arbitrary, fixed value: this file's
     * own purpose is exercising the Assembler in isolation, not exercising
     * continuity resolution (see [InMemoryConversationEngineTest] for
     * that).
     */
    private fun resolved(message: InboundOwnerMessage, conversationId: ConversationId = ConversationId("conv-assembler-test")) =
        ResolvedInboundMessage(message, conversationId)

    private fun principal(principalId: PrincipalId, displayName: String = "Test Owner") = Principal(
        principalId = principalId,
        principalType = PrincipalType.USER,
        displayName = displayName,
        owner = null,
        status = PrincipalStatus.ACTIVE,
        createdAt = Instant.parse("2025-01-01T00:00:00Z"),
        lastSeenAt = Instant.parse("2025-01-01T00:00:00Z"),
    )

    private fun descriptor(toolId: String, displayName: String, description: String) = ToolDescriptor(
        toolId = toolId,
        displayName = displayName,
        description = description,
    )

    // --- 1. Successful assembly ---

    @Test
    fun `assemble produces a ReasoningContext carrying the requesting principal, channel, time, tools, and request`() = runTest {
        val message = message(text = "what's on my calendar today?")
        val identityService = FakeIdentityService { principalFor -> if (principalFor == ownerPrincipalId) principal(ownerPrincipalId) else null }
        val toolRegistry = FakeToolRegistry { listOf(descriptor("tool.notify", "Notify Owner", "Delivers a text reply to the owner")) }
        val assembler = DefaultReasoningContextAssembler(identityService, toolRegistry)

        val context = assembler.assemble(resolved(message))

        assertTrue(context.entries.any { "Test Owner" in it && ownerPrincipalId.value in it })
        assertTrue(context.entries.any { message.channelId.value in it })
        assertTrue(context.entries.any { message.timestamp.toString() in it })
        assertTrue(context.entries.any { "Notify Owner" in it && "Delivers a text reply to the owner" in it })
        assertTrue(context.entries.any { "what's on my calendar today?" in it })
        assertEquals(1, identityService.resolveCallCount)
        assertEquals(1, toolRegistry.listAllCallCount)
    }

    // --- 2. Immutable output ---

    @Test
    fun `the returned ReasoningContext's entries are not affected by a later, separate assemble call`() = runTest {
        val identityService = FakeIdentityService { principal(ownerPrincipalId) }
        val toolRegistry = FakeToolRegistry { emptyList() }
        val assembler = DefaultReasoningContextAssembler(identityService, toolRegistry)
        val message = message()

        val first = assembler.assemble(resolved(message))
        val firstSnapshot = first.entries.toList()

        assembler.assemble(resolved(message(text = "a completely different request")))

        assertEquals(firstSnapshot, first.entries)
    }

    // --- 3. Empty optional inputs ---

    @Test
    fun `an empty tool catalogue produces no Available tool entries but a still-valid, non-empty ReasoningContext`() = runTest {
        val identityService = FakeIdentityService { principal(ownerPrincipalId) }
        val toolRegistry = FakeToolRegistry { emptyList() }
        val assembler = DefaultReasoningContextAssembler(identityService, toolRegistry)

        val context = assembler.assemble(resolved(message()))

        assertFalse(context.entries.any { it.startsWith("Available tool:") })
        assertTrue(context.entries.isNotEmpty())
    }

    // --- 4. Requesting principal identity ---

    @Test
    fun `a resolved requesting principal is rendered with its display name and PrincipalId`() = runTest {
        val identityService = FakeIdentityService { principal(ownerPrincipalId, displayName = "Steven") }
        val toolRegistry = FakeToolRegistry { emptyList() }
        val assembler = DefaultReasoningContextAssembler(identityService, toolRegistry)

        val context = assembler.assemble(resolved(message()))

        val principalEntry = context.entries.single { it.startsWith("Requesting principal:") }
        assertTrue("Steven" in principalEntry)
        assertTrue(ownerPrincipalId.value in principalEntry)
        assertFalse("not resolved" in principalEntry)
    }

    @Test
    fun `an unresolvable requesting principal is rendered with an explicit not-resolved fallback, not an exception`() = runTest {
        val identityService = FakeIdentityService { null }
        val toolRegistry = FakeToolRegistry { emptyList() }
        val assembler = DefaultReasoningContextAssembler(identityService, toolRegistry)

        val context = assembler.assemble(resolved(message()))

        val principalEntry = context.entries.single { it.startsWith("Requesting principal:") }
        assertTrue(ownerPrincipalId.value in principalEntry)
        assertTrue("identity not resolved" in principalEntry)
    }

    // --- 5. Available tool descriptions ---

    @Test
    fun `every ToolDescriptor from ToolRegistry_listAll is rendered as its own entry, in order`() = runTest {
        val identityService = FakeIdentityService { principal(ownerPrincipalId) }
        val tools = listOf(
            descriptor("tool.notify", "Notify Owner", "Delivers a text reply to the owner"),
            descriptor("tool.calendar", "Calendar Lookup", "Reads the owner's calendar"),
        )
        val toolRegistry = FakeToolRegistry { tools }
        val assembler = DefaultReasoningContextAssembler(identityService, toolRegistry)

        val context = assembler.assemble(resolved(message()))

        val toolEntries = context.entries.filter { it.startsWith("Available tool:") }
        assertEquals(2, toolEntries.size)
        assertTrue(toolEntries[0].let { "Notify Owner" in it && "Delivers a text reply to the owner" in it })
        assertTrue(toolEntries[1].let { "Calendar Lookup" in it && "Reads the owner's calendar" in it })
    }

    // --- 6. Current time handling ---

    @Test
    fun `the message's own timestamp, not wall-clock time, is rendered`() = runTest {
        val identityService = FakeIdentityService { principal(ownerPrincipalId) }
        val toolRegistry = FakeToolRegistry { emptyList() }
        val assembler = DefaultReasoningContextAssembler(identityService, toolRegistry)
        val fixedTimestamp = Instant.parse("2026-07-24T14:00:00Z")

        val context = assembler.assemble(resolved(message(timestamp = fixedTimestamp)))

        val timeEntry = context.entries.single { it.startsWith("Current time:") }
        assertTrue(fixedTimestamp.toString() in timeEntry)
    }

    // --- 7. Dependency failures ---

    @Test
    fun `an IdentityService_resolve failure propagates unchanged, not caught or wrapped`() = runTest {
        val failure = IllegalStateException("simulated identity resolution failure")
        val identityService = FakeIdentityService { throw failure }
        val toolRegistry = FakeToolRegistry { emptyList() }
        val assembler = DefaultReasoningContextAssembler(identityService, toolRegistry)

        val thrown = assertFailsWith<IllegalStateException> { assembler.assemble(resolved(message())) }
        assertSame(failure, thrown)
    }

    @Test
    fun `a ToolRegistry_listAll failure propagates unchanged, not caught or wrapped`() = runTest {
        val failure = IllegalStateException("simulated tool registry failure")
        val identityService = FakeIdentityService { principal(ownerPrincipalId) }
        val toolRegistry = FakeToolRegistry { throw failure }
        val assembler = DefaultReasoningContextAssembler(identityService, toolRegistry)

        val thrown = assertFailsWith<IllegalStateException> { assembler.assemble(resolved(message())) }
        assertSame(failure, thrown)
    }

    // --- 8. Assembler failure behaviour: no degraded-but-valid substitute is ever produced ---

    @Test
    fun `a dependency failure never produces a degraded ReasoningContext -- assemble either returns a complete one or throws`() = runTest {
        val identityService = FakeIdentityService { throw IllegalStateException("unreachable") }
        val toolRegistry = FakeToolRegistry { emptyList() }
        val assembler = DefaultReasoningContextAssembler(identityService, toolRegistry)

        assertFailsWith<IllegalStateException> { assembler.assemble(resolved(message())) }
        // No partial ReasoningContext is observable anywhere -- assemble threw before constructing one.
    }

    // --- 9. Repeated calls produce independent ReasoningContext instances ---

    @Test
    fun `two calls with equal-content messages produce equal but reference-distinct ReasoningContext instances`() = runTest {
        val identityService = FakeIdentityService { principal(ownerPrincipalId) }
        val toolRegistry = FakeToolRegistry { emptyList() }
        val assembler = DefaultReasoningContextAssembler(identityService, toolRegistry)
        val fixedTimestamp = Instant.parse("2026-01-01T00:00:00Z")
        val first = assembler.assemble(resolved(message(timestamp = fixedTimestamp, correlationId = "corr-fixed")))
        val second = assembler.assemble(resolved(message(timestamp = fixedTimestamp, correlationId = "corr-fixed")))

        assertEquals(first, second)
        assertNotSame(first, second)
        assertEquals(2, identityService.resolveCallCount)
        assertEquals(2, toolRegistry.listAllCallCount)
    }

    // --- 10. Sprint 11 Unit 5: the resolved ConversationId is read and rendered ---

    @Test
    fun `the resolved ConversationId is rendered as its own entry, with no dependency call of any kind`() = runTest {
        val identityService = FakeIdentityService { principal(ownerPrincipalId) }
        val toolRegistry = FakeToolRegistry { emptyList() }
        val assembler = DefaultReasoningContextAssembler(identityService, toolRegistry)
        val conversationId = ConversationId("conv-rendered-test")

        val context = assembler.assemble(resolved(message(), conversationId))

        val conversationEntry = context.entries.single { it.startsWith("Current conversation:") }
        assertTrue(conversationId.value in conversationEntry)
        // Rendering it costs no additional dependency call -- confirmed by the exact same
        // call counts test 1 above already asserts for a call that also renders this entry.
        assertEquals(1, identityService.resolveCallCount)
        assertEquals(1, toolRegistry.listAllCallCount)
    }

    @Test
    fun `two different resolved ConversationIds for equal-content messages render different entries`() = runTest {
        val identityService = FakeIdentityService { principal(ownerPrincipalId) }
        val toolRegistry = FakeToolRegistry { emptyList() }
        val assembler = DefaultReasoningContextAssembler(identityService, toolRegistry)
        val message = message()

        val first = assembler.assemble(resolved(message, ConversationId("conv-1")))
        val second = assembler.assemble(resolved(message, ConversationId("conv-2")))

        assertNotSame(first, second)
        assertTrue(first.entries.any { "conv-1" in it })
        assertTrue(second.entries.any { "conv-2" in it })
    }

    // --- structural: no prohibited dependency slot exists ---

    @Test
    fun `the assembler's constructor accepts exactly two dependencies -- IdentityService and ToolRegistry`() {
        val constructor = DefaultReasoningContextAssembler::class.java.declaredConstructors.single()
        val parameterTypes = constructor.parameterTypes.map { it.simpleName }.toSet()

        assertEquals(setOf("IdentityService", "ToolRegistry"), parameterTypes)
    }
}
