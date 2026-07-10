package parker.core.runtime

import kotlinx.coroutines.withTimeout
import parker.core.interfaces.ReasoningProvider
import parker.core.interfaces.ReasoningProviderRequest
import parker.core.interfaces.ReasoningProviderResponse

/**
 * Model-Backed ReasoningProvider (Sprint 9), implementing exactly what
 * `docs/implementation/reviews/ModelReasoningProvider_Implementation_Review.md`
 * ("the Review") and
 * `docs/implementation/MODEL_REASONING_PROVIDER_IMPLEMENTATION_PLAN.md`
 * ("the Plan") authorise. Concrete [ReasoningProvider] implementation --
 * a pure orchestrator with exactly three constructor-injected
 * collaborators, each with one job (Review Section 4):
 *
 * ```
 * reason(request)
 *     1. prompt = promptBuilder.buildPrompt(request.turn, request.reasoningContext)
 *     2. raw    = withTimeout(timeoutMs) { modelInferenceClient.infer(prompt) }
 *     3. return responseParser.parse(raw)
 * ```
 *
 * Holds only its three collaborators and [timeoutMs] as fields -- no
 * cache of any prior Turn, prompt, or response (Review Section 5). No
 * `try`/`catch` anywhere in this class: if [modelInferenceClient]'s
 * `infer` throws, if the call times out, or if [responseParser]'s
 * `parse` throws, the fault propagates unchanged to the caller (Review
 * Section 8). `NoAction` is never used as a catch-all.
 *
 * No `IdentityService`, `PlannerRuntime`, `ExecutionPipeline`,
 * `PermissionEngine`, `ToolRegistry`, `ToolInvocationBinding`,
 * `MemoryStore`, `WorldModel`, `ModuleRegistry`, `ConversationEngine`,
 * `ResponseDelivery`, or `ModelManager` dependency exists anywhere in
 * this class (Review Section 4).
 *
 * @param promptBuilder Turns an already-in-memory [parker.core.interfaces.Turn]
 *   and [parker.core.interfaces.ReasoningContext] into a prompt string.
 * @param modelInferenceClient The entire model-call seam.
 * @param responseParser Classifies a raw model string into a
 *   [ReasoningProviderResponse], or throws.
 * @param timeoutMs Wraps only the [modelInferenceClient] call, default
 *   `30_000` ms (Review Section 8, Section 12 Decision 4).
 */
class ModelReasoningProvider(
    private val promptBuilder: ReasoningPromptBuilder,
    private val modelInferenceClient: ModelInferenceClient,
    private val responseParser: ReasoningResponseParser,
    private val timeoutMs: Long = 30_000L,
) : ReasoningProvider {

    override suspend fun reason(request: ReasoningProviderRequest): ReasoningProviderResponse {
        val prompt = promptBuilder.buildPrompt(request.turn, request.reasoningContext)
        val raw = withTimeout(timeoutMs) { modelInferenceClient.infer(prompt) }
        return responseParser.parse(raw)
    }
}
