package parker.core.runtime

import kotlinx.coroutines.test.runTest
import parker.core.interfaces.AuthorizationPurposeId
import parker.core.interfaces.CandidateAssertion
import parker.core.interfaces.CandidateProvenance
import parker.core.interfaces.ContentNature
import parker.core.interfaces.ConversationId
import parker.core.interfaces.CorrelationId
import parker.core.interfaces.DecisionId
import parker.core.interfaces.EvidentialState
import parker.core.interfaces.ExecutionRequest
import parker.core.interfaces.InboundOwnerMessage
import parker.core.interfaces.KnowledgeCandidate
import parker.core.interfaces.KnowledgeItemStatus
import parker.core.interfaces.KnowledgeRetrievalQuery
import parker.core.interfaces.KnowledgeSubmissionDisposition
import parker.core.interfaces.MemoryCoreRecordReference
import parker.core.interfaces.ModuleId
import parker.core.interfaces.PermissionAction
import parker.core.interfaces.PermissionDecision
import parker.core.interfaces.PermissionDecisionOutcome
import parker.core.interfaces.PermissionLevel
import parker.core.interfaces.Principal
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.PrincipalStatus
import parker.core.interfaces.PrincipalType
import parker.core.interfaces.ReasoningKnowledgeSource
import parker.core.interfaces.ResolvedInboundMessage
import parker.core.interfaces.SafeKnowledgeResultEntry
import parker.core.interfaces.StalenessDisclosure
import parker.core.interfaces.ToolDescriptor
import parker.core.interfaces.Turn
import parker.core.interfaces.TurnId
import parker.core.interfaces.WorldBelief
import parker.core.interfaces.WorldModelSource
import parker.core.interfaces.WorldObservation
import parker.core.interfaces.WorldQuery
import java.time.Instant
import kotlin.reflect.full.functions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
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
 * Plus items originally added by Sprint 11 Unit 7 (Memory Source Integration) and revised by
 * Knowledge Discoverability and Governed Retrieval into Reasoning Context, Implementation Unit 3
 * (`docs/governance/KNOWLEDGE_DISCOVERABILITY_REASONING_CONTEXT_IMPLEMENTATION_PLAN.md` Section 8),
 * which replaced the assembler's former legacy memory-source dependency with
 * [ReasoningKnowledgeSource]: an empty `recall` result renders no entries but still calls `recall`
 * exactly once; a single returned [SafeKnowledgeResultEntry] renders one entry; multiple returned
 * entries render in the exact order `recall` returns them, never reordered; `evidentialState`,
 * `status`, and `staleness` are each rendered via their own `.name`, exactly, never fabricated, never
 * omitted; the frozen `escapeForPrompt` contract is exercised against every character it defines; the
 * constructed [KnowledgeRetrievalQuery] carries the request text as `relevance` and the message's own
 * `correlationId`, while `requestingPrincipalId` is passed as `recall`'s own separate first parameter;
 * `maximumResults` is always positive and caller-supplied, with no specific value architecturally
 * asserted; a `recall` failure propagates unchanged; [ReasoningKnowledgeSource] exposes no mutation
 * operation; and one genuine end-to-end test exercises a real [DefaultReasoningKnowledgeSource],
 * not a fake, wired to a real [InMemoryKnowledgeItemPersistence], a real [DefaultKnowledgeSubmission],
 * a real [DefaultKnowledgeCandidateEvaluator], and a real [InMemoryMemoryCore].
 *
 * [FakeIdentityService], [FakeToolRegistry], [FakeConversationHistorySource],
 * and [FakeReasoningKnowledgeSource] are used throughout (except where the real collaborators above
 * are deliberately substituted, Section 13, below), never a real
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
        val assembler = DefaultReasoningContextAssembler(identityService, toolRegistry, FakeConversationHistorySource(), FakeReasoningKnowledgeSource(), FakeWorldModelSource())

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
        val assembler = DefaultReasoningContextAssembler(identityService, toolRegistry, FakeConversationHistorySource(), FakeReasoningKnowledgeSource(), FakeWorldModelSource())
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
        val assembler = DefaultReasoningContextAssembler(identityService, toolRegistry, FakeConversationHistorySource(), FakeReasoningKnowledgeSource(), FakeWorldModelSource())

        val context = assembler.assemble(resolved(message()))

        assertFalse(context.entries.any { it.startsWith("Available tool:") })
        assertTrue(context.entries.isNotEmpty())
    }

    // --- 4. Requesting principal identity ---

    @Test
    fun `a resolved requesting principal is rendered with its display name and PrincipalId`() = runTest {
        val identityService = FakeIdentityService { principal(ownerPrincipalId, displayName = "Steven") }
        val toolRegistry = FakeToolRegistry { emptyList() }
        val assembler = DefaultReasoningContextAssembler(identityService, toolRegistry, FakeConversationHistorySource(), FakeReasoningKnowledgeSource(), FakeWorldModelSource())

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
        val assembler = DefaultReasoningContextAssembler(identityService, toolRegistry, FakeConversationHistorySource(), FakeReasoningKnowledgeSource(), FakeWorldModelSource())

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
        val assembler = DefaultReasoningContextAssembler(identityService, toolRegistry, FakeConversationHistorySource(), FakeReasoningKnowledgeSource(), FakeWorldModelSource())

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
        val assembler = DefaultReasoningContextAssembler(identityService, toolRegistry, FakeConversationHistorySource(), FakeReasoningKnowledgeSource(), FakeWorldModelSource())
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
        val assembler = DefaultReasoningContextAssembler(identityService, toolRegistry, FakeConversationHistorySource(), FakeReasoningKnowledgeSource(), FakeWorldModelSource())

        val thrown = assertFailsWith<IllegalStateException> { assembler.assemble(resolved(message())) }
        assertSame(failure, thrown)
    }

    @Test
    fun `a ToolRegistry_listAll failure propagates unchanged, not caught or wrapped`() = runTest {
        val failure = IllegalStateException("simulated tool registry failure")
        val identityService = FakeIdentityService { principal(ownerPrincipalId) }
        val toolRegistry = FakeToolRegistry { throw failure }
        val assembler = DefaultReasoningContextAssembler(identityService, toolRegistry, FakeConversationHistorySource(), FakeReasoningKnowledgeSource(), FakeWorldModelSource())

        val thrown = assertFailsWith<IllegalStateException> { assembler.assemble(resolved(message())) }
        assertSame(failure, thrown)
    }

    // --- 8. Assembler failure behaviour: no degraded-but-valid substitute is ever produced ---

    @Test
    fun `a dependency failure never produces a degraded ReasoningContext -- assemble either returns a complete one or throws`() = runTest {
        val identityService = FakeIdentityService { throw IllegalStateException("unreachable") }
        val toolRegistry = FakeToolRegistry { emptyList() }
        val assembler = DefaultReasoningContextAssembler(identityService, toolRegistry, FakeConversationHistorySource(), FakeReasoningKnowledgeSource(), FakeWorldModelSource())

        assertFailsWith<IllegalStateException> { assembler.assemble(resolved(message())) }
        // No partial ReasoningContext is observable anywhere -- assemble threw before constructing one.
    }

    // --- 9. Repeated calls produce independent ReasoningContext instances ---

    @Test
    fun `two calls with equal-content messages produce equal but reference-distinct ReasoningContext instances`() = runTest {
        val identityService = FakeIdentityService { principal(ownerPrincipalId) }
        val toolRegistry = FakeToolRegistry { emptyList() }
        val assembler = DefaultReasoningContextAssembler(identityService, toolRegistry, FakeConversationHistorySource(), FakeReasoningKnowledgeSource(), FakeWorldModelSource())
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
        val assembler = DefaultReasoningContextAssembler(identityService, toolRegistry, FakeConversationHistorySource(), FakeReasoningKnowledgeSource(), FakeWorldModelSource())
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
        val assembler = DefaultReasoningContextAssembler(identityService, toolRegistry, FakeConversationHistorySource(), FakeReasoningKnowledgeSource(), FakeWorldModelSource())
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
        val assembler = DefaultReasoningContextAssembler(identityService, toolRegistry, historySource, FakeReasoningKnowledgeSource(), FakeWorldModelSource())
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
        val assembler = DefaultReasoningContextAssembler(identityService, toolRegistry, historySource, FakeReasoningKnowledgeSource(), FakeWorldModelSource())

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
        val assembler = DefaultReasoningContextAssembler(identityService, toolRegistry, historySource, FakeReasoningKnowledgeSource(), FakeWorldModelSource())

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
        val assembler = DefaultReasoningContextAssembler(identityService, toolRegistry, historySource, FakeReasoningKnowledgeSource(), FakeWorldModelSource())

        val thrown = assertFailsWith<IllegalStateException> { assembler.assemble(resolved(message())) }
        assertSame(failure, thrown)
    }

    // --- structural: no prohibited dependency slot exists ---

    @Test
    fun `the assembler's constructor accepts exactly five dependencies -- IdentityService, ToolRegistry, ConversationHistorySource, ReasoningKnowledgeSource, and WorldModelSource`() {
        val constructor = DefaultReasoningContextAssembler::class.java.declaredConstructors.single()
        val parameterTypes = constructor.parameterTypes.map { it.simpleName }.toSet()

        assertEquals(
            setOf("IdentityService", "ToolRegistry", "ConversationHistorySource", "ReasoningKnowledgeSource", "WorldModelSource"),
            parameterTypes,
        )
    }

    // --- 12. Knowledge Discoverability and Governed Retrieval into Reasoning Context,
    // Implementation Unit 3: Memory rendering ---

    /**
     * Test-only fake, mirroring [FakeConversationHistorySource]'s lambda-based fake precedent, scoped
     * to this file since [ReasoningKnowledgeSource] is otherwise only exercised, real, by
     * `DefaultReasoningKnowledgeSourceTest`. Records every [KnowledgeRetrievalQuery] and requesting
     * [PrincipalId] `recall` was called with, and how many times -- enough for tests to assert exactly
     * one call, with the exact constructed query and principal, and no more.
     */
    private class FakeReasoningKnowledgeSource(
        private val entriesFor: (KnowledgeRetrievalQuery) -> List<SafeKnowledgeResultEntry> = { emptyList() },
    ) : ReasoningKnowledgeSource {

        var recallCallCount: Int = 0
            private set

        val recallCallArguments: MutableList<KnowledgeRetrievalQuery> = mutableListOf()
        val recallCallPrincipals: MutableList<PrincipalId> = mutableListOf()

        override suspend fun recall(requestingPrincipalId: PrincipalId, query: KnowledgeRetrievalQuery): List<SafeKnowledgeResultEntry> {
            recallCallCount++
            recallCallArguments += query
            recallCallPrincipals += requestingPrincipalId
            return entriesFor(query)
        }
    }

    private fun knowledgeEntry(
        content: String,
        evidentialState: EvidentialState = EvidentialState.VERIFIED_EVIDENCE,
        status: KnowledgeItemStatus = KnowledgeItemStatus.ACTIVE,
        staleness: StalenessDisclosure = StalenessDisclosure.INDETERMINATE,
    ) = SafeKnowledgeResultEntry(
        content = content,
        evidentialState = evidentialState,
        status = status,
        staleness = staleness,
    )

    @Test
    fun `an empty recall result produces no Memory entries but calls recall exactly once`() = runTest {
        val identityService = FakeIdentityService { principal(ownerPrincipalId) }
        val toolRegistry = FakeToolRegistry { emptyList() }
        val knowledgeSource = FakeReasoningKnowledgeSource { emptyList() }
        val assembler = DefaultReasoningContextAssembler(identityService, toolRegistry, FakeConversationHistorySource(), knowledgeSource, FakeWorldModelSource())

        val context = assembler.assemble(resolved(message()))

        assertFalse(context.entries.any { it.startsWith("Memory:") })
        assertEquals(1, knowledgeSource.recallCallCount)
    }

    @Test
    fun `a single returned SafeKnowledgeResultEntry is rendered as one Memory entry in the exact frozen format`() = runTest {
        val identityService = FakeIdentityService { principal(ownerPrincipalId) }
        val toolRegistry = FakeToolRegistry { emptyList() }
        val knowledgeSource = FakeReasoningKnowledgeSource {
            listOf(
                knowledgeEntry(
                    "the owner prefers window seats",
                    evidentialState = EvidentialState.VERIFIED_EVIDENCE,
                    status = KnowledgeItemStatus.ACTIVE,
                    staleness = StalenessDisclosure.INDETERMINATE,
                ),
            )
        }
        val assembler = DefaultReasoningContextAssembler(identityService, toolRegistry, FakeConversationHistorySource(), knowledgeSource, FakeWorldModelSource())

        val context = assembler.assemble(resolved(message()))

        val memoryEntries = context.entries.filter { it.startsWith("Memory:") }
        assertEquals(1, memoryEntries.size)
        assertEquals(
            "Memory: the owner prefers window seats (evidentialState=VERIFIED_EVIDENCE, status=ACTIVE, staleness=INDETERMINATE)",
            memoryEntries.single(),
        )
    }

    @Test
    fun `multiple returned entries are rendered in the exact order recall returns them, never reordered`() = runTest {
        val identityService = FakeIdentityService { principal(ownerPrincipalId) }
        val toolRegistry = FakeToolRegistry { emptyList() }
        val knowledgeSource = FakeReasoningKnowledgeSource {
            listOf(knowledgeEntry("memory C"), knowledgeEntry("memory A"), knowledgeEntry("memory B"))
        }
        val assembler = DefaultReasoningContextAssembler(identityService, toolRegistry, FakeConversationHistorySource(), knowledgeSource, FakeWorldModelSource())

        val context = assembler.assemble(resolved(message()))

        val memoryEntries = context.entries.filter { it.startsWith("Memory:") }
        assertEquals(3, memoryEntries.size)
        assertTrue("memory C" in memoryEntries[0])
        assertTrue("memory A" in memoryEntries[1])
        assertTrue("memory B" in memoryEntries[2])
    }

    @Test
    fun `evidentialState, status, and staleness are each rendered via their own name, exactly, never fabricated or omitted`() = runTest {
        val identityService = FakeIdentityService { principal(ownerPrincipalId) }
        val toolRegistry = FakeToolRegistry { emptyList() }
        val knowledgeSource = FakeReasoningKnowledgeSource {
            listOf(
                knowledgeEntry(
                    "the owner's dog is named Stellar",
                    evidentialState = EvidentialState.CORROBORATED_EVIDENCE,
                    status = KnowledgeItemStatus.RETIRED,
                    staleness = StalenessDisclosure.POSSIBLY_STALE,
                ),
            )
        }
        val assembler = DefaultReasoningContextAssembler(identityService, toolRegistry, FakeConversationHistorySource(), knowledgeSource, FakeWorldModelSource())

        val context = assembler.assemble(resolved(message()))

        val memoryEntry = context.entries.single { it.startsWith("Memory:") }
        assertEquals(
            "Memory: the owner's dog is named Stellar (evidentialState=CORROBORATED_EVIDENCE, status=RETIRED, staleness=POSSIBLY_STALE)",
            memoryEntry,
        )
    }

    @Test
    fun `backslash, LF, CR, and TAB each escape to their own exact two-character form`() = runTest {
        val identityService = FakeIdentityService { principal(ownerPrincipalId) }
        val toolRegistry = FakeToolRegistry { emptyList() }
        val content = "a\\b\nc\rd\te"
        val knowledgeSource = FakeReasoningKnowledgeSource { listOf(knowledgeEntry(content)) }
        val assembler = DefaultReasoningContextAssembler(identityService, toolRegistry, FakeConversationHistorySource(), knowledgeSource, FakeWorldModelSource())

        val context = assembler.assemble(resolved(message()))

        val memoryEntry = context.entries.single { it.startsWith("Memory:") }
        assertTrue("a\\\\b\\nc\\rd\\te" in memoryEntry)
    }

    @Test
    fun `every C0 control character other than LF, CR, and TAB, DEL, and every C1 control character escapes to its own four-hex-digit u form`() = runTest {
        val identityService = FakeIdentityService { principal(ownerPrincipalId) }
        val toolRegistry = FakeToolRegistry { emptyList() }
        val codePoints = ((0x00..0x1F) + 0x7F + (0x80..0x9F)).toSet() - setOf(0x0A, 0x0D, 0x09)

        for (codePoint in codePoints) {
            val knowledgeSource = FakeReasoningKnowledgeSource { listOf(knowledgeEntry("x${codePoint.toChar()}y")) }
            val assembler = DefaultReasoningContextAssembler(identityService, toolRegistry, FakeConversationHistorySource(), knowledgeSource, FakeWorldModelSource())

            val context = assembler.assemble(resolved(message()))

            val memoryEntry = context.entries.single { it.startsWith("Memory:") }
            val expectedEscape = "\\u" + codePoint.toString(16).uppercase().padStart(4, '0')
            assertTrue(
                "x$expectedEscape" + "y" in memoryEntry,
                "codepoint 0x${codePoint.toString(16)} did not escape to $expectedEscape in: $memoryEntry",
            )
        }
    }

    @Test
    fun `U+2028 and U+2029 each escape to their own deterministic four-hex-digit form`() = runTest {
        val identityService = FakeIdentityService { principal(ownerPrincipalId) }
        val toolRegistry = FakeToolRegistry { emptyList() }
        val content = "line-separator: paragraph-separator: end"
        val knowledgeSource = FakeReasoningKnowledgeSource { listOf(knowledgeEntry(content)) }
        val assembler = DefaultReasoningContextAssembler(identityService, toolRegistry, FakeConversationHistorySource(), knowledgeSource, FakeWorldModelSource())

        val context = assembler.assemble(resolved(message()))

        val memoryEntry = context.entries.single { it.startsWith("Memory:") }
        assertTrue("line-separator:\\u2028paragraph-separator:\\u2029end" in memoryEntry)
    }

    @Test
    fun `a content value containing a raw LF, CR, U+2028, or U+2029 still yields exactly one ReasoningContext entry, never an extra line or entry`() = runTest {
        val identityService = FakeIdentityService { principal(ownerPrincipalId) }
        val toolRegistry = FakeToolRegistry { emptyList() }
        val content = "first\nsecond\rthird fourth fifth"
        val knowledgeSource = FakeReasoningKnowledgeSource { listOf(knowledgeEntry(content)) }
        val assembler = DefaultReasoningContextAssembler(identityService, toolRegistry, FakeConversationHistorySource(), knowledgeSource, FakeWorldModelSource())

        val context = assembler.assemble(resolved(message()))

        val memoryEntries = context.entries.filter { it.startsWith("Memory:") }
        assertEquals(1, memoryEntries.size)
        val rendered = memoryEntries.single()
        assertFalse(rendered.contains('\n'))
        assertFalse(rendered.contains('\r'))
        assertFalse(rendered.contains(' '))
        assertFalse(rendered.contains(' '))
    }

    @Test
    fun `the constructed KnowledgeRetrievalQuery carries the request text as relevance and the message's own correlationId, with requestingPrincipalId passed as recall's own separate first parameter`() = runTest {
        val identityService = FakeIdentityService { principal(ownerPrincipalId) }
        val toolRegistry = FakeToolRegistry { emptyList() }
        val knowledgeSource = FakeReasoningKnowledgeSource { emptyList() }
        val assembler = DefaultReasoningContextAssembler(identityService, toolRegistry, FakeConversationHistorySource(), knowledgeSource, FakeWorldModelSource())
        val requestMessage = message(text = "what's the capital of France?", correlationId = "corr-memory-query-test")

        assembler.assemble(resolved(requestMessage))

        val query = knowledgeSource.recallCallArguments.single()
        val principalArgument = knowledgeSource.recallCallPrincipals.single()
        assertEquals("what's the capital of France?", query.relevance)
        assertEquals("corr-memory-query-test", query.correlationId)
        assertEquals(ownerPrincipalId, principalArgument)
    }

    @Test
    fun `the constructed KnowledgeRetrievalQuery always carries a positive, caller-supplied maximumResults -- no specific value is architecturally asserted`() = runTest {
        val identityService = FakeIdentityService { principal(ownerPrincipalId) }
        val toolRegistry = FakeToolRegistry { emptyList() }
        val knowledgeSource = FakeReasoningKnowledgeSource { emptyList() }
        val assembler = DefaultReasoningContextAssembler(identityService, toolRegistry, FakeConversationHistorySource(), knowledgeSource, FakeWorldModelSource())

        assembler.assemble(resolved(message()))

        val query = knowledgeSource.recallCallArguments.single()
        assertTrue(query.maximumResults >= 1, "KnowledgeRetrievalQuery.maximumResults must be a positive, caller-supplied bound")
    }

    @Test
    fun `a ReasoningKnowledgeSource_recall failure propagates unchanged, not caught or wrapped`() = runTest {
        val failure = IllegalStateException("simulated memory recall failure")
        val identityService = FakeIdentityService { principal(ownerPrincipalId) }
        val toolRegistry = FakeToolRegistry { emptyList() }
        val knowledgeSource = FakeReasoningKnowledgeSource { throw failure }
        val assembler = DefaultReasoningContextAssembler(identityService, toolRegistry, FakeConversationHistorySource(), knowledgeSource, FakeWorldModelSource())

        val thrown = assertFailsWith<IllegalStateException> { assembler.assemble(resolved(message())) }
        assertSame(failure, thrown)
    }

    @Test
    fun `ReasoningKnowledgeSource exposes no mutation operation -- no remember, no forget, only recall`() {
        val functionNames = ReasoningKnowledgeSource::class.functions.map { it.name }.toSet()

        assertTrue("recall" in functionNames, "ReasoningKnowledgeSource must expose recall")
        assertFalse("remember" in functionNames, "ReasoningKnowledgeSource must not expose remember")
        assertFalse("forget" in functionNames, "ReasoningKnowledgeSource must not expose forget")
    }

    // --- 13. Knowledge Discoverability and Governed Retrieval into Reasoning Context,
    // Implementation Unit 3: genuine end-to-end integration (real DefaultReasoningKnowledgeSource,
    // not a fake) ---

    /** Approves every evaluation unconditionally -- both DefaultKnowledgeSubmission's own act-level
     * write gate and DefaultReasoningKnowledgeSource's own act- and item-level read gates. */
    private fun approvingPermissionEngine() = FakePermissionEngine { request ->
        PermissionDecision(
            decisionId = DecisionId("decision-assembler-test"),
            principalId = request.principalId,
            resourceId = request.targetResources.first(),
            action = PermissionAction.READ,
            decision = PermissionDecisionOutcome.APPROVED,
            level = PermissionLevel.AUTOMATIC,
            timestamp = Instant.now(),
        )
    }

    @Test
    fun `a KnowledgeItem promoted through a real DefaultKnowledgeSubmission is retrieved through a real DefaultReasoningKnowledgeSource and rendered end-to-end`() = runTest {
        val identityService = FakeIdentityService { principal(ownerPrincipalId) }
        val toolRegistry = FakeToolRegistry { emptyList() }
        val core = InMemoryMemoryCore()
        val evaluator = DefaultKnowledgeCandidateEvaluator(core)
        val persistence = InMemoryKnowledgeItemPersistence()
        val permissionEngine = approvingPermissionEngine()
        val submission = DefaultKnowledgeSubmission(evaluator, persistence, permissionEngine)

        val provenance = core.createProvenance(
            ownerPrincipalId,
            CandidateProvenance(
                sourceIdentifier = "test-harness",
                sourceType = "test-harness",
                acquisitionTime = Instant.parse("2026-01-01T00:00:00Z"),
                contentNature = ContentNature.ORIGINAL,
            ),
        )
        val assertion = core.createAssertion(
            ownerPrincipalId,
            CandidateAssertion(
                statement = "the owner's favourite programming language is Kotlin",
                provenanceId = provenance.provenanceId,
                confidence = 0.9,
            ),
        )
        val candidate = KnowledgeCandidate(MemoryCoreRecordReference.ToAssertion(assertion.assertionId), explicitlyRequested = true)
        val disposition = submission.submit(ownerPrincipalId, candidate)
        assertIs<KnowledgeSubmissionDisposition.Promoted>(disposition)

        val purpose = AuthorizationPurposeId("knowledge-memory.reasoning-context-retrieval")
        val reasoningKnowledgeSource = DefaultReasoningKnowledgeSource(persistence, permissionEngine, core, purpose)
        val assembler = DefaultReasoningContextAssembler(identityService, toolRegistry, FakeConversationHistorySource(), reasoningKnowledgeSource, FakeWorldModelSource())

        val context = assembler.assemble(resolved(message(text = "Kotlin")))

        val memoryEntries = context.entries.filter { it.startsWith("Memory:") }
        assertEquals(1, memoryEntries.size)
        assertTrue("the owner's favourite programming language is Kotlin" in memoryEntries.single())
    }

    // --- 14. Sprint 11 Unit 8: World Model rendering ---

    private fun belief(
        subject: String,
        value: String = "some-value",
        confidence: Double = 0.9,
        source: String = "test-harness",
        timestamp: Instant = Instant.parse("2026-01-01T08:00:00Z"),
    ) = WorldBelief(
        subject = subject,
        value = value,
        confidence = confidence,
        timestamp = timestamp,
        source = source,
    )

    @Test
    fun `an empty WorldModelSource result produces no World belief entries but calls recall exactly once`() = runTest {
        val identityService = FakeIdentityService { principal(ownerPrincipalId) }
        val toolRegistry = FakeToolRegistry { emptyList() }
        val worldModelSource = FakeWorldModelSource { emptyList() }
        val assembler = DefaultReasoningContextAssembler(identityService, toolRegistry, FakeConversationHistorySource(), FakeReasoningKnowledgeSource(), worldModelSource)

        val context = assembler.assemble(resolved(message()))

        assertFalse(context.entries.any { it.startsWith("World belief:") })
        assertEquals(1, worldModelSource.recallCallCount)
    }

    @Test
    fun `a single returned belief is rendered as one World belief entry`() = runTest {
        val identityService = FakeIdentityService { principal(ownerPrincipalId) }
        val toolRegistry = FakeToolRegistry { emptyList() }
        val worldModelSource = FakeWorldModelSource { listOf(belief(subject = "device-front-door", value = "locked")) }
        val assembler = DefaultReasoningContextAssembler(identityService, toolRegistry, FakeConversationHistorySource(), FakeReasoningKnowledgeSource(), worldModelSource)

        val context = assembler.assemble(resolved(message()))

        val beliefEntries = context.entries.filter { it.startsWith("World belief:") }
        assertEquals(1, beliefEntries.size)
        assertTrue("device-front-door" in beliefEntries.single())
        assertTrue("locked" in beliefEntries.single())
    }

    @Test
    fun `multiple returned beliefs are rendered in the exact order recall returns them, never reordered`() = runTest {
        val identityService = FakeIdentityService { principal(ownerPrincipalId) }
        val toolRegistry = FakeToolRegistry { emptyList() }
        val worldModelSource = FakeWorldModelSource {
            listOf(
                belief(subject = "subject-C", value = "value-C"),
                belief(subject = "subject-A", value = "value-A"),
                belief(subject = "subject-B", value = "value-B"),
            )
        }
        val assembler = DefaultReasoningContextAssembler(identityService, toolRegistry, FakeConversationHistorySource(), FakeReasoningKnowledgeSource(), worldModelSource)

        val context = assembler.assemble(resolved(message()))

        val beliefEntries = context.entries.filter { it.startsWith("World belief:") }
        assertEquals(3, beliefEntries.size)
        assertTrue("subject-C" in beliefEntries[0])
        assertTrue("subject-A" in beliefEntries[1])
        assertTrue("subject-B" in beliefEntries[2])
    }

    @Test
    fun `each belief's own confidence is rendered exactly, never fabricated or shared between entries`() = runTest {
        val identityService = FakeIdentityService { principal(ownerPrincipalId) }
        val toolRegistry = FakeToolRegistry { emptyList() }
        val worldModelSource = FakeWorldModelSource {
            listOf(
                belief(subject = "device-high-confidence", confidence = 0.95),
                belief(subject = "device-low-confidence", confidence = 0.4),
            )
        }
        val assembler = DefaultReasoningContextAssembler(identityService, toolRegistry, FakeConversationHistorySource(), FakeReasoningKnowledgeSource(), worldModelSource)

        val context = assembler.assemble(resolved(message()))

        val beliefEntries = context.entries.filter { it.startsWith("World belief:") }
        val high = beliefEntries.single { "device-high-confidence" in it }
        val low = beliefEntries.single { "device-low-confidence" in it }
        assertTrue("0.95" in high)
        assertTrue("0.4" in low)
        // WorldBelief.confidence is a required field -- there is no "absent" case to omit;
        // this asserts each entry carries its own exact value, never the other's or a fabricated one.
        assertFalse("0.95" in low)
        assertFalse("0.4" in high)
    }

    @Test
    fun `the constructed WorldQuery carries subjectMatch = null -- no subject inference, classification, or parsing`() = runTest {
        val identityService = FakeIdentityService { principal(ownerPrincipalId) }
        val toolRegistry = FakeToolRegistry { emptyList() }
        val worldModelSource = FakeWorldModelSource { emptyList() }
        val assembler = DefaultReasoningContextAssembler(identityService, toolRegistry, FakeConversationHistorySource(), FakeReasoningKnowledgeSource(), worldModelSource)

        assembler.assemble(resolved(message(text = "is the front door locked?")))

        val query = worldModelSource.recallCallArguments.single()
        assertEquals(null, query.subjectMatch)
    }

    @Test
    fun `the constructed WorldQuery always carries a positive, caller-supplied maximumResults -- no specific value is architecturally asserted`() = runTest {
        val identityService = FakeIdentityService { principal(ownerPrincipalId) }
        val toolRegistry = FakeToolRegistry { emptyList() }
        val worldModelSource = FakeWorldModelSource { emptyList() }
        val assembler = DefaultReasoningContextAssembler(identityService, toolRegistry, FakeConversationHistorySource(), FakeReasoningKnowledgeSource(), worldModelSource)

        assembler.assemble(resolved(message()))

        val query = worldModelSource.recallCallArguments.single()
        assertTrue(query.maximumResults >= 1, "WorldQuery.maximumResults must be a positive, caller-supplied bound")
    }

    @Test
    fun `the constructed WorldQuery carries a null minimumConfidence, since no confidence floor is required by the approved design`() = runTest {
        val identityService = FakeIdentityService { principal(ownerPrincipalId) }
        val toolRegistry = FakeToolRegistry { emptyList() }
        val worldModelSource = FakeWorldModelSource { emptyList() }
        val assembler = DefaultReasoningContextAssembler(identityService, toolRegistry, FakeConversationHistorySource(), FakeReasoningKnowledgeSource(), worldModelSource)

        assembler.assemble(resolved(message()))

        val query = worldModelSource.recallCallArguments.single()
        assertEquals(null, query.minimumConfidence)
    }

    @Test
    fun `a WorldModelSource_recall failure propagates unchanged, not caught or wrapped`() = runTest {
        val failure = IllegalStateException("simulated world model recall failure")
        val identityService = FakeIdentityService { principal(ownerPrincipalId) }
        val toolRegistry = FakeToolRegistry { emptyList() }
        val worldModelSource = FakeWorldModelSource { throw failure }
        val assembler = DefaultReasoningContextAssembler(identityService, toolRegistry, FakeConversationHistorySource(), FakeReasoningKnowledgeSource(), worldModelSource)

        val thrown = assertFailsWith<IllegalStateException> { assembler.assemble(resolved(message())) }
        assertSame(failure, thrown)
    }

    // --- 15. Sprint 11 Unit 8: real-collaborator integration (InMemoryWorldModel, not a fake) ---

    @Test
    fun `a belief observed through a real InMemoryWorldModel is retrieved and rendered end-to-end`() = runTest {
        val identityService = FakeIdentityService { principal(ownerPrincipalId) }
        val toolRegistry = FakeToolRegistry { emptyList() }
        val realWorldModel = InMemoryWorldModel()
        realWorldModel.observe(
            WorldObservation(
                subject = "device-front-door",
                confidence = 0.95,
                source = "sensor-lock-1",
                value = "locked",
            ),
        )
        val assembler = DefaultReasoningContextAssembler(identityService, toolRegistry, FakeConversationHistorySource(), FakeReasoningKnowledgeSource(), realWorldModel)

        val context = assembler.assemble(resolved(message()))

        val beliefEntries = context.entries.filter { it.startsWith("World belief:") }
        assertEquals(1, beliefEntries.size)
        assertTrue("device-front-door" in beliefEntries.single())
        assertTrue("locked" in beliefEntries.single())
    }
}
