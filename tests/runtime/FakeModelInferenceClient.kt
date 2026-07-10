package parker.core.runtime

/**
 * Test-only fake, mirroring [FakeReasoningProvider]'s lambda-based fake
 * precedent (Plan Decision G). Exists so [ModelReasoningProviderTest] can
 * exercise [ModelReasoningProvider]'s own orchestration independently of
 * any real model or HTTP call.
 */
class FakeModelInferenceClient(
    private val responseFor: suspend (String) -> String,
) : ModelInferenceClient {

    var inferCallCount: Int = 0
        private set

    var lastPrompt: String? = null
        private set

    override suspend fun infer(prompt: String): String {
        inferCallCount++
        lastPrompt = prompt
        return responseFor(prompt)
    }
}
