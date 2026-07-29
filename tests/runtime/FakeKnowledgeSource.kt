package parker.core.runtime

import parker.core.interfaces.KnowledgeQuery
import parker.core.interfaces.KnowledgeRecord
import parker.core.interfaces.KnowledgeSource

/**
 * Test-only fake, mirroring [FakeConversationHistorySource]'s lambda-based
 * fake precedent. Exists so [DefaultReasoningContextAssemblerTest] can
 * exercise [DefaultReasoningContextAssembler]'s Memory rendering (Sprint
 * 11 Unit 7) independently of any real [InMemoryKnowledgeStore]
 * implementation.
 *
 * Records every [KnowledgeQuery] it was called with, and how many times --
 * enough for tests to assert exactly one call, with the exact constructed
 * query, and no more.
 */
class FakeMemorySource(
    private val recordsFor: (KnowledgeQuery) -> List<KnowledgeRecord> = { emptyList() },
) : KnowledgeSource {

    var recallCallCount: Int = 0
        private set

    val recallCallArguments: MutableList<KnowledgeQuery> = mutableListOf()

    override suspend fun recall(query: KnowledgeQuery): List<KnowledgeRecord> {
        recallCallCount++
        recallCallArguments += query
        return recordsFor(query)
    }
}
