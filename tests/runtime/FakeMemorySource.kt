package parker.core.runtime

import parker.core.interfaces.MemoryQuery
import parker.core.interfaces.MemoryRecord
import parker.core.interfaces.MemorySource

/**
 * Test-only fake, mirroring [FakeConversationHistorySource]'s lambda-based
 * fake precedent. Exists so [DefaultReasoningContextAssemblerTest] can
 * exercise [DefaultReasoningContextAssembler]'s Memory rendering (Sprint
 * 11 Unit 7) independently of any real [InMemoryMemoryStore]
 * implementation.
 *
 * Records every [MemoryQuery] it was called with, and how many times --
 * enough for tests to assert exactly one call, with the exact constructed
 * query, and no more.
 */
class FakeMemorySource(
    private val recordsFor: (MemoryQuery) -> List<MemoryRecord> = { emptyList() },
) : MemorySource {

    var recallCallCount: Int = 0
        private set

    val recallCallArguments: MutableList<MemoryQuery> = mutableListOf()

    override suspend fun recall(query: MemoryQuery): List<MemoryRecord> {
        recallCallCount++
        recallCallArguments += query
        return recordsFor(query)
    }
}
