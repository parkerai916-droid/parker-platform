package parker.core.runtime

import parker.core.interfaces.ReasoningProvider
import parker.core.interfaces.ReasoningProviderRequest
import parker.core.interfaces.ReasoningProviderResponse

/**
 * Test-only fake, mirroring [FakeCommunicationIntake]/[FakePermissionEngine]'s
 * lambda-based fake precedent. Exists so [ConversationTurnReasoningCoordinatorTest]
 * can prove the coordinator's own *orchestration* (does it call
 * [parker.core.interfaces.ConversationEngine.submitTurn] first, then
 * [ReasoningProvider.reason] with the correctly constructed request,
 * returning the response unchanged) independently of any real reasoning
 * provider implementation, of which none exists in this unit.
 */
class FakeReasoningProvider(
    private val responseFor: (ReasoningProviderRequest) -> ReasoningProviderResponse,
) : ReasoningProvider {

    var reasonCallCount: Int = 0
        private set

    var lastRequest: ReasoningProviderRequest? = null
        private set

    override suspend fun reason(request: ReasoningProviderRequest): ReasoningProviderResponse {
        reasonCallCount++
        lastRequest = request
        return responseFor(request)
    }
}
