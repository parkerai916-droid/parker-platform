package parker.core.runtime

import kotlinx.coroutines.test.runTest
import parker.core.interfaces.CorrelationId
import parker.core.interfaces.InboundOwnerMessage
import parker.core.interfaces.ModuleId
import parker.core.interfaces.Principal
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.PrincipalStatus
import parker.core.interfaces.PrincipalType
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
 * covering exactly the nine items the Unit's own task instructions name:
 * successful assembly, immutable output, empty optional inputs, requesting
 * principal identity, available tool descriptions, current time handling,
 * dependency failures, assembler failure behaviour, and repeated calls
 * producing independent [parker.core.interfaces.ReasoningContext]
 * instances -- plus one structural test guarding the dependency discipline
 * `PRODUCTION_REASONING_CONTEXT_CONTRACT_DESIGN.md` Section 4 requires
 * (mirroring [ConversationTurnReasoningCoordinatorTest]'s own "constructor
 * accepts exactly N dependencies" precedent).
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

        val context = assembler.assemble(message)

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

        val first = assembler.assemble(message)
        val firstSnapshot = first.entries.toList()

        assembler.assemble(message(text = "a completely different request"))

        assertEquals(firstSnapshot, first.entries)
    }

    // --- 3. Empty optional inputs ---

    @Test
    fun `an empty tool catalogue produces no Available tool entries but a still-valid, non-empty ReasoningContext`() = runTest {
        val identityService = FakeIdentityService { principal(ownerPrincipalId) }
        val toolRegistry = FakeToolRegistry { emptyList() }
        val assembler = DefaultReasoningContextAssembler(identityService, toolRegistry)

        val context = assembler.assemble(message())

        assertFalse(context.entries.any { it.startsWith("Available tool:") })
        assertTrue(context.entries.isNotEmpty())
    }

    // --- 4. Requesting principal identity ---

    @Test
    fun `a resolved requesting principal is rendered with its display name and PrincipalId`() = runTest {
        val identityService = FakeIdentityService { principal(ownerPrincipalId, displayName = "Steven") }
        val toolRegistry = FakeToolRegistry { emptyList() }
        val assembler = DefaultReasoningContextAssembler(identityService, toolRegistry)

        val context = assembler.assemble(message())

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

        val context = assembler.assemble(message())

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

        val context = assembler.assemble(message())

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

        val context = assembler.assemble(message(timestamp = fixedTimestamp))

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

        val thrown = assertFailsWith<IllegalStateException> { assembler.assemble(message()) }
        assertSame(failure, thrown)
    }

    @Test
    fun `a ToolRegistry_listAll failure propagates unchanged, not caught or wrapped`() = runTest {
        val failure = IllegalStateException("simulated tool registry failure")
        val identityService = FakeIdentityService { principal(ownerPrincipalId) }
        val toolRegistry = FakeToolRegistry { throw failure }
        val assembler = DefaultReasoningContextAssembler(identityService, toolRegistry)

        val thrown = assertFailsWith<IllegalStateException> { assembler.assemble(message()) }
        assertSame(failure, thrown)
    }

    // --- 8. Assembler failure behaviour: no degraded-but-valid substitute is ever produced ---

    @Test
    fun `a dependency failure never produces a degraded ReasoningContext -- assemble either returns a complete one or throws`() = runTest {
        val identityService = FakeIdentityService { throw IllegalStateException("unreachable") }
        val toolRegistry = FakeToolRegistry { emptyList() }
        val assembler = DefaultReasoningContextAssembler(identityService, toolRegistry)

        assertFailsWith<IllegalStateException> { assembler.assemble(message()) }
        // No partial ReasoningContext is observable anywhere -- assemble threw before constructing one.
    }

    // --- 9. Repeated calls produce independent ReasoningContext instances ---

    @Test
    fun `two calls with equal-content messages produce equal but reference-distinct ReasoningContext instances`() = runTest {
        val identityService = FakeIdentityService { principal(ownerPrincipalId) }
        val toolRegistry = FakeToolRegistry { emptyList() }
        val assembler = DefaultReasoningContextAssembler(identityService, toolRegistry)
        val fixedTimestamp = Instant.parse("2026-01-01T00:00:00Z")
        val first = assembler.assemble(message(timestamp = fixedTimestamp, correlationId = "corr-fixed"))
        val second = assembler.assemble(message(timestamp = fixedTimestamp, correlationId = "corr-fixed"))

        assertEquals(first, second)
        assertNotSame(first, second)
        assertEquals(2, identityService.resolveCallCount)
        assertEquals(2, toolRegistry.listAllCallCount)
    }

    // --- structural: no prohibited dependency slot exists ---

    @Test
    fun `the assembler's constructor accepts exactly two dependencies -- IdentityService and ToolRegistry`() {
        val constructor = DefaultReasoningContextAssembler::class.java.declaredConstructors.single()
        val parameterTypes = constructor.parameterTypes.map { it.simpleName }.toSet()

        assertEquals(setOf("IdentityService", "ToolRegistry"), parameterTypes)
    }
}
