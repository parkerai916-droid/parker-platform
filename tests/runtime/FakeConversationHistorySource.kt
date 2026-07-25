package parker.core.runtime

import parker.core.interfaces.ConversationHistorySource
import parker.core.interfaces.ConversationId
import parker.core.interfaces.Turn

/**
 * Test-only fake, mirroring [FakeIdentityService]/[FakeToolRegistry]'s
 * lambda-based fake precedent. Exists so
 * [DefaultReasoningContextAssemblerTest] can exercise
 * [DefaultReasoningContextAssembler]'s Conversation History rendering
 * (Sprint 11 Unit 6) independently of any real
 * [InMemoryConversationEngine] implementation.
 *
 * Records every [ConversationId] it was called with, and how many times
 * -- enough for tests to assert exactly one call, with the exact resolved
 * identifier, and no more.
 */
class FakeConversationHistorySource(
    private val turnsFor: (ConversationId) -> List<Turn> = { emptyList() },
) : ConversationHistorySource {

    var historyCallCount: Int = 0
        private set

    val historyCallArguments: MutableList<ConversationId> = mutableListOf()

    override suspend fun history(conversationId: ConversationId): List<Turn> {
        historyCallCount++
        historyCallArguments += conversationId
        return turnsFor(conversationId)
    }
}
