package parker.core.runtime

import parker.core.interfaces.WorldBelief
import parker.core.interfaces.WorldModelSource
import parker.core.interfaces.WorldQuery

/**
 * Test-only fake, mirroring [FakeMemorySource]/[FakeConversationHistorySource]'s
 * lambda-based fake precedent. Exists so [DefaultReasoningContextAssemblerTest]
 * can exercise [DefaultReasoningContextAssembler]'s World Model rendering
 * (Sprint 11 Unit 8) independently of any real [InMemoryWorldModel]
 * implementation.
 *
 * Records every [WorldQuery] it was called with, and how many times --
 * enough for tests to assert exactly one call, with the exact constructed
 * query, and no more. Returns whatever [beliefsFor] supplies, in the exact
 * order supplied -- this fake performs no reordering of its own, so a test
 * supplying a fixed-order list can assert the Assembler preserves that
 * exact order, independent of any real backing implementation's own
 * absence of an ordering guarantee.
 */
class FakeWorldModelSource(
    private val beliefsFor: (WorldQuery) -> List<WorldBelief> = { emptyList() },
) : WorldModelSource {

    var recallCallCount: Int = 0
        private set

    val recallCallArguments: MutableList<WorldQuery> = mutableListOf()

    override suspend fun recall(query: WorldQuery): List<WorldBelief> {
        recallCallCount++
        recallCallArguments += query
        return beliefsFor(query)
    }
}
