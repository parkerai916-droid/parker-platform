package parker.core.runtime

import kotlinx.coroutines.test.runTest
import parker.core.interfaces.CandidateMemory
import parker.core.interfaces.ConversationId
import parker.core.interfaces.CorrelationId
import parker.core.interfaces.InboundOwnerMessage
import parker.core.interfaces.MemoryCategory
import parker.core.interfaces.MemoryId
import parker.core.interfaces.MemoryRecord
import parker.core.interfaces.MemorySource
import parker.core.interfaces.ModuleId
import parker.core.interfaces.Principal
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.PrincipalStatus
import parker.core.interfaces.PrincipalType
import parker.core.interfaces.ResolvedInboundMessage
import parker.core.interfaces.ToolDescriptor
import parker.core.interfaces.Turn
import parker.core.interfaces.TurnId
import java.time.Instant
import kotlin.reflect.full.functions
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
 * structural dependency test, plus one item added by Unit 5: the
 * resolved `ConversationId` is read and rendered, with no lookup, no
 * resolution, and no mutation -- plus items added by Sprint 11 Unit 6
 * (Conversation History Source): empty history renders no entries but
 * still calls `history` exactly once with the resolved `ConversationId`;
 * a single prior Turn renders one entry; multiple prior Turns render in
 * the exact order returned; a `history` failure propagates unchanged.
 *
 * Plus items added by Sprint 11 Unit 7 (Memory Source Integration): an
 * empty memory result renders no entries but still calls `recall` exactly
 * once; a single returned memory renders one entry; multiple returned
 * memories render in the exact order `recall` returns them, never
 * reordered; confidence is rendered when present and omitted, not
 * fabricated, when absent; the constructed `MemoryQuery` carries the
 * sender `PrincipalId`, the request text as `relevance`, and the
 * message's own `correlationId`, with `category` always `null`;
 * `maximumResults` is always positive and caller-supplied, with no
 * specific value architecturally asserted; a `recall` failure propagates
 * unchanged; `MemorySource` exposes no mutation operation; and one
 * real-collaborator test exercises a real [InMemoryMemoryStore], not a
 * fake, end-to-end.
 *
 * [FakeIdentityService], [FakeToolRegistry], [FakeConversationHistorySource],
 * and [FakeMemorySource] are used throughout (except where a real
 * [InMemoryMemoryStore] is deliberately substituted, Section 13, below),
 * never a real
 * [InMemoryIdentityService]/[InMemoryToolRegistry]/[InMemoryConversationEngine]
 * -- this file exercises [DefaultReasoningContextAssembler] in isolation,
 * exactly as [ConversationTurnReasoningCoordinatorTest] and
 * [ResponseComposerTest] already do for their own subjects.
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
        val assembler = DefaultReasoningContextAssembler(identityService, toolRegistry, FakeConversationHistorySource(), FakeMemorySource())

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
        val assembler = DefaultReasoningContextAssembler(identityService, toolRegistry, FakeConversationHistorySource(), FakeMemorySource())
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
        val assembler = DefaultReasoningContextAssembler(identityService, toolRegistry, FakeConversationHistorySource(), FakeMemorySource())

        val context = assembler.assemble(resolved(message()))

        assertFalse(context.entries.any { it.startsWith("Available tool:") })
        assertTrue(context.entries.isNotEmpty())
    }

    // --- 4. Requesting principal identity ---

    @Test
    fun `a resolved requesting principal is rendered with its display name and PrincipalId`() = runTest {
        val identityService = FakeIdentityService { principal(ownerPrincipalId, displayName = "Steven") }
        val toolRegistry = FakeToolRegistry { emptyList() }
        val assembler = DefaultReasoningContextAssembler(identityService, toolRegistry, FakeConversationHistorySource(), FakeMemorySource())

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
        val assembler = DefaultReasoningContextAssembler(identityService, toolRegistry, FakeConversationHistorySource(), FakeMemorySource())

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
        val assembler = DefaultReasoningContextAssembler(identityService, toolRegistry, FakeConversationHistorySource(), FakeMemorySource())

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
        val assembler = DefaultReasoningContextAssembler(identityService, toolRegistry, FakeConversationHistorySource(), FakeMemorySource())
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
        val assembler = DefaultReasoningContextAssembler(identityService, toolRegistry, FakeConversationHistorySource(), FakeMemorySource())

        val thrown = assertFailsWith<IllegalStateException> { assembler.assemble(resolved(message())) }
        assertSame(failure, thrown)
    }

    @Test
    fun `a ToolRegistry_listAll failure propagates unchanged, not caught or wrapped`() = runTest {
        val failure = IllegalStateException("simulated tool registry failure")
        val identityService = FakeIdentityService { principal(ownerPrincipalId) }
        val toolRegistry = FakeToolRegistry { throw failure }
        val assembler = DefaultReasoningContextAssembler(identityService, toolRegistry, FakeConversationHistorySource(), FakeMemorySource())

        val thrown = assertFailsWith<IllegalStateException> { assembler.assemble(resolved(message())) }
        assertSame(failure, thrown)
    }

    // --- 8. Assembler failure behaviour: no degraded-but-valid substitute is ever produced ---

    @Test
    fun `a dependency failure never produces a degraded ReasoningContext -- assemble either returns a complete one or throws`() = runTest {
        val identityService = FakeIdentityService { throw IllegalStateException("unreachable") }
        val toolRegistry = FakeToolRegistry { emptyList() }
        val assembler = DefaultReasoningContextAssembler(identityService, toolRegistry, FakeConversationHistorySource(), FakeMemorySource())

        assertFailsWith<IllegalStateException> { assembler.assemble(resolved(message())) }
        // No partial ReasoningContext is observable anywhere -- assemble threw before constructing one.
    }

    // --- 9. Repeated calls produce independent ReasoningContext instances ---

    @Test
    fun `two calls with equal-content messages produce equal but reference-distinct ReasoningContext instances`() = runTest {
        val identityService = FakeIdentityService { principal(ownerPrincipalId) }
        val toolRegistry = FakeToolRegistry { emptyList() }
        val assembler = DefaultReasoningContextAssembler(identityService, toolRegistry, FakeConversationHistorySource(), FakeMemorySource())
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
        val assembler = DefaultReasoningContextAssembler(identityService, toolRegistry, FakeConversationHistorySource(), FakeMemorySource())
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
        val assembler = DefaultReasoningContextAssembler(identityService, toolRegistry, FakeConversationHistorySource(), FakeMemorySource())
        val message = message()

        val first = assembler.assemble(resolved(message, ConversationId("conv-1")))
        val second = assembler.assemble(resolved(message, ConversationId("conv-2")))

        assertNotSame(first, second)
        assertTrue(first.entries.any { "conv-1" in it })
        assertTrue(second.entries.any { "conv-2" in it })
    }

    // --- 11. Sprint 11 Unit 6: Conversation History rendering ---

    private fun turn(text: String, senderPrincipalId: PrincipalId = ownerPrincipalId, receivedAt: Instant = Instant.parse("2026-01-01T09:00:00Z"), conversationId: ConversationId = ConversationId("conv-history-test")) =
        Turn(
            turnId = TurnId("turn-${System.nanoTime()}"),
            conversationId = conversationId,
            message = message(senderPrincipalId = senderPrincipalId, text = text),
            receivedAt = receivedAt,
        )

    @Test
    fun `an empty history produces no Prior message entries but calls history exactly once with the resolved ConversationId`() = runTest {
        val identityService = FakeIdentityService { principal(ownerPrincipalId) }
        val toolRegistry = FakeToolRegistry { emptyList() }
        val historySource = FakeConversationHistorySource { emptyList() }
        val assembler = DefaultReasoningContextAssembler(identityService, toolRegistry, historySource, FakeMemorySource())
        val conversationId = ConversationId("conv-empty-history")

        val context = assembler.assemble(resolved(message(), conversationId))

        assertFalse(context.entries.any { it.startsWith("Prior message:") })
        assertEquals(1, historySource.historyCallCount)
        assertEquals(listOf(conversationId), historySource.historyCallArguments)
    }

    @Test
    fun `a single prior Turn is rendered as one Prior message entry`() = runTest {
        val identityService = FakeIdentityService { principal(ownerPrincipalId) }
        val toolRegistry = FakeToolRegistry { emptyList() }
        val historySource = FakeConversationHistorySource { listOf(turn("what's the weather like?")) }
        val assembler = DefaultReasoningContextAssembler(identityService, toolRegistry, historySource, FakeMemorySource())

        val context = assembler.assemble(resolved(message()))

        val priorEntries = context.entries.filter { it.startsWith("Prior message:") }
        assertEquals(1, priorEntries.size)
        assertTrue("what's the weather like?" in priorEntries.single())
    }

    @Test
    fun `multiple prior Turns are rendered oldest first, in the exact order history returns them`() = runTest {
        val identityService = FakeIdentityService { principal(ownerPrincipalId) }
        val toolRegistry = FakeToolRegistry { emptyList() }
        val historySource = FakeConversationHistorySource {
            listOf(turn("first message"), turn("second message"), turn("third message"))
        }
        val assembler = DefaultReasoningContextAssembler(identityService, toolRegistry, historySource, FakeMemorySource())

        val context = assembler.assemble(resolved(message()))

        val priorEntries = context.entries.filter { it.startsWith("Prior message:") }
        assertEquals(3, priorEntries.size)
        assertTrue("first message" in priorEntries[0])
        assertTrue("second message" in priorEntries[1])
        assertTrue("third message" in priorEntries[2])
    }

    @Test
    fun `a ConversationHistorySource_history failure propagates unchanged, not caught or wrapped`() = runTest {
        val failure = IllegalStateException("simulated history retrieval failure")
        val identityService = FakeIdentityService { principal(ownerPrincipalId) }
        val toolRegistry = FakeToolRegistry { emptyList() }
        val historySource = FakeConversationHistorySource { throw failure }
        val assembler = DefaultReasoningContextAssembler(identityService, toolRegistry, historySource, FakeMemorySource())

        val thrown = assertFailsWith<IllegalStateException> { assembler.assemble(resolved(message())) }
        assertSame(failure, thrown)
    }

    // --- structural: no prohibited dependency slot exists ---

    @Test
    fun `the assembler's constructor accepts exactly four dependencies -- IdentityService, ToolRegistry, ConversationHistorySource, and MemorySource`() {
        val constructor = DefaultReasoningContextAssembler::class.java.declaredConstructors.single()
        val parameterTypes = constructor.parameterTypes.map { it.simpleName }.toSet()

        assertEquals(setOf("IdentityService", "ToolRegistry", "ConversationHistorySource", "MemorySource"), parameterTypes)
    }

    // --- 12. Sprint 11 Unit 7: Memory rendering ---

    private fun memoryRecord(
        payload: String,
        sourceSubsystem: String = "test-harness",
        confidence: Double? = null,
        memoryId: MemoryId = MemoryId("memory-${System.nanoTime()}"),
        promotedAt: Instant = Instant.parse("2026-01-01T08:00:00Z"),
    ) = MemoryRecord(
        memoryId = memoryId,
        category = MemoryCategory.SEMANTIC,
        sourceSubsystem = sourceSubsystem,
        correlationId = "corr-memory-test",
        promotedAt = promotedAt,
        knowledgePayload = payload,
        confidence = confidence,
    )

    @Test
    fun `an empty memory result produces no Memory entries but calls recall exactly once`() = runTest {
        val identityService = FakeIdentityService { principal(ownerPrincipalId) }
        val toolRegistry = FakeToolRegistry { emptyList() }
        val memorySource = FakeMemorySource { emptyList() }
        val assembler = DefaultReasoningContextAssembler(identityService, toolRegistry, FakeConversationHistorySource(), memorySource)

        val context = assembler.assemble(resolved(message()))

        assertFalse(context.entries.any { it.startsWith("Memory:") })
        assertEquals(1, memorySource.recallCallCount)
    }

    @Test
    fun `a single returned memory is rendered as one Memory entry`() = runTest {
        val identityService = FakeIdentityService { principal(ownerPrincipalId) }
        val toolRegistry = FakeToolRegistry { emptyList() }
        val memorySource = FakeMemorySource { listOf(memoryRecord("the owner prefers window seats")) }
        val assembler = DefaultReasoningContextAssembler(identityService, toolRegistry, FakeConversationHistorySource(), memorySource)

        val context = assembler.assemble(resolved(message()))

        val memoryEntries = context.entries.filter { it.startsWith("Memory:") }
        assertEquals(1, memoryEntries.size)
        assertTrue("the owner prefers window seats" in memoryEntries.single())
    }

    @Test
    fun `multiple returned memories are rendered in the exact order recall returns them, never reordered`() = runTest {
        val identityService = FakeIdentityService { principal(ownerPrincipalId) }
        val toolRegistry = FakeToolRegistry { emptyList() }
        val memorySource = FakeMemorySource {
            listOf(memoryRecord("memory C"), memoryRecord("memory A"), memoryRecord("memory B"))
        }
        val assembler = DefaultReasoningContextAssembler(identityService, toolRegistry, FakeConversationHistorySource(), memorySource)

        val context = assembler.assemble(resolved(message()))

        val memoryEntries = context.entries.filter { it.startsWith("Memory:") }
        assertEquals(3, memoryEntries.size)
        assertTrue("memory C" in memoryEntries[0])
        assertTrue("memory A" in memoryEntries[1])
        assertTrue("memory B" in memoryEntries[2])
    }

    @Test
    fun `a memory's confidence is rendered when present and omitted, not fabricated, when absent`() = runTest {
        val identityService = FakeIdentityService { principal(ownerPrincipalId) }
        val toolRegistry = FakeToolRegistry { emptyList() }
        val memorySource = FakeMemorySource {
            listOf(
                memoryRecord("memory with confidence", confidence = 0.9),
                memoryRecord("the owner dislikes cilantro", confidence = null),
            )
        }
        val assembler = DefaultReasoningContextAssembler(identityService, toolRegistry, FakeConversationHistorySource(), memorySource)

        val context = assembler.assemble(resolved(message()))

        val memoryEntries = context.entries.filter { it.startsWith("Memory:") }
        val withConfidence = memoryEntries.single { "memory with confidence" in it }
        val withoutConfidence = memoryEntries.single { "the owner dislikes cilantro" in it }
        assertTrue("0.9" in withConfidence)
        assertFalse(withoutConfidence.contains(", confidence:"))
    }

    @Test
    fun `the constructed MemoryQuery carries the sender PrincipalId, the request text as relevance, and the message's own correlationId, with a null category`() = runTest {
        val identityService = FakeIdentityService { principal(ownerPrincipalId) }
        val toolRegistry = FakeToolRegistry { emptyList() }
        val memorySource = FakeMemorySource { emptyList() }
        val assembler = DefaultReasoningContextAssembler(identityService, toolRegistry, FakeConversationHistorySource(), memorySource)
        val requestMessage = message(text = "what's the capital of France?", correlationId = "corr-memory-query-test")

        assembler.assemble(resolved(requestMessage))

        val query = memorySource.recallCallArguments.single()
        assertEquals(ownerPrincipalId, query.requestingPrincipalId)
        assertEquals("what's the capital of France?", query.relevance)
        assertEquals("corr-memory-query-test", query.correlationId)
        assertEquals(null, query.category)
    }

    @Test
    fun `the constructed MemoryQuery always carries a positive, caller-supplied maximumResults -- no specific value is architecturally asserted`() = runTest {
        val identityService = FakeIdentityService { principal(ownerPrincipalId) }
        val toolRegistry = FakeToolRegistry { emptyList() }
        val memorySource = FakeMemorySource { emptyList() }
        val assembler = DefaultReasoningContextAssembler(identityService, toolRegistry, FakeConversationHistorySource(), memorySource)

        assembler.assemble(resolved(message()))

        val query = memorySource.recallCallArguments.single()
        assertTrue(query.maximumResults >= 1, "MemoryQuery.maximumResults must be a positive, caller-supplied bound")
    }

    @Test
    fun `a MemorySource_recall failure propagates unchanged, not caught or wrapped`() = runTest {
        val failure = IllegalStateException("simulated memory recall failure")
        val identityService = FakeIdentityService { principal(ownerPrincipalId) }
        val toolRegistry = FakeToolRegistry { emptyList() }
        val memorySource = FakeMemorySource { throw failure }
        val assembler = DefaultReasoningContextAssembler(identityService, toolRegistry, FakeConversationHistorySource(), memorySource)

        val thrown = assertFailsWith<IllegalStateException> { assembler.assemble(resolved(message())) }
        assertSame(failure, thrown)
    }

    @Test
    fun `MemorySource exposes no mutation operation -- no remember, no forget, only recall`() {
        val functionNames = MemorySource::class.functions.map { it.name }.toSet()

        assertTrue("recall" in functionNames, "MemorySource must expose recall")
        assertFalse("remember" in functionNames, "MemorySource must not expose remember")
        assertFalse("forget" in functionNames, "MemorySource must not expose forget")
    }

    // --- 13. Sprint 11 Unit 7: real-collaborator integration (InMemoryMemoryStore, not a fake) ---

    @Test
    fun `a memory promoted through a real InMemoryMemoryStore is retrieved and rendered end-to-end`() = runTest {
        val identityService = FakeIdentityService { principal(ownerPrincipalId) }
        val toolRegistry = FakeToolRegistry { emptyList() }
        val realMemoryStore = InMemoryMemoryStore()
        realMemoryStore.remember(
            CandidateMemory(
                knowledgePayload = "the owner's favourite programming language is Kotlin",
                proposedCategory = MemoryCategory.SEMANTIC,
                sourceSubsystem = "test-harness",
                correlationId = "corr-seed",
                originatingPrincipalId = ownerPrincipalId,
                explicitlyRequested = true,
            ),
        )
        val assembler = DefaultReasoningContextAssembler(identityService, toolRegistry, FakeConversationHistorySource(), realMemoryStore)
        // The request text becomes MemoryQuery.relevance (Contract Design Section 5), and
        // InMemoryMemoryStore.retrieve's own existing, already-tested behaviour requires the
        // memory's knowledgePayload to *contain* relevance as a substring -- so a short,
        // substring-matching request text is used here deliberately, not a full sentence, to
        // exercise the real, unmodified matching behaviour rather than inventing a semantic one.
        val context = assembler.assemble(resolved(message(text = "Kotlin")))

        val memoryEntries = context.entries.filter { it.startsWith("Memory:") }
        assertEquals(1, memoryEntries.size)
        assertTrue("the owner's favourite programming language is Kotlin" in memoryEntries.single())
    }
}
