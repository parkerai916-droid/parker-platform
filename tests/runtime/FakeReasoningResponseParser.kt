package parker.core.runtime

import parker.core.interfaces.ReasoningProviderResponse

/**
 * Test-only fake, mirroring [FakeReasoningProvider]'s lambda-based fake
 * precedent (Plan Decision G). Exists so [ModelReasoningProviderTest] can
 * exercise [ModelReasoningProvider]'s own orchestration independently of
 * [TaggedReasoningResponseParser]'s real classification convention.
 */
class FakeReasoningResponseParser(
    private val responseFor: (String) -> ReasoningProviderResponse,
) : ReasoningResponseParser {

    var parseCallCount: Int = 0
        private set

    var lastRaw: String? = null
        private set

    override fun parse(raw: String): ReasoningProviderResponse {
        parseCallCount++
        lastRaw = raw
        return responseFor(raw)
    }
}
