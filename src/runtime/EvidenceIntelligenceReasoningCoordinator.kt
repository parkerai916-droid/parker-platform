package parker.core.runtime

import parker.core.interfaces.EvidenceAnalysisRequest
import parker.core.interfaces.ReasoningContext
import parker.core.interfaces.ReasoningProvider
import parker.core.interfaces.ReasoningProviderRequest
import parker.core.interfaces.ReasoningProviderResponse
import parker.core.interfaces.ReasoningSubject

/**
 * Evidence Intelligence, Implementation Unit 3 ("Reasoning Provider
 * Orchestration"). Governed in full by
 * `docs/implementation/EVIDENCE_INTELLIGENCE_IMPLEMENTATION_PLAN.md`
 * ("the Implementation Plan"), §8 Unit 3 -- this class implements exactly
 * that Unit's own responsibilities, and only those.
 *
 * Establishes Evidence Intelligence as an orchestrating caller of an
 * existing [ReasoningProvider], never a Reasoning Provider itself
 * (CDR-007 §1). Invokes it by constructing a [ReasoningProviderRequest]
 * whose `subject` is [ReasoningSubject.OfEvidenceAnalysisRequest],
 * wrapping the supplied [EvidenceAnalysisRequest] unmodified
 * (`REASONING_PROVIDER_CONTRACT_DESIGN.md`, Amendment 1). Treats the
 * invocation as a pure, stateless callee with no obligations beyond what
 * the Reasoning Provider Contract Design already defines, and returns
 * its [ReasoningProviderResponse] unchanged.
 *
 * **The provider-facing `ReasoningContext` invariant.** [reasoningContext]
 * below becomes this request's own top-level
 * `ReasoningProviderRequest.reasoningContext` -- the sole context
 * `ReasoningProvider.reason` ever consults, per that document's own
 * frozen invariant. `EvidenceAnalysisRequest.reasoningContext` retains
 * its existing meaning and ownership within Evidence Intelligence's own
 * analysis (`EVIDENCE_INTELLIGENCE_CONTRACT_DESIGN.md` §4); it is not
 * read, merged, or otherwise consulted by this class.
 *
 * `internal`, not public -- this is Unit 3 implementation plumbing, not
 * one of the four public runtime types the Contract Design and Scope
 * Lock authorise. No interface is introduced; callers within this module
 * construct and call this class directly, mirroring
 * [EvidenceIntelligenceInputResolver]'s own identical precedent, and the
 * orchestration pattern [ConversationTurnReasoningCoordinator] already
 * establishes for the same [ReasoningProvider] interface.
 *
 * ## What this Unit deliberately does not implement
 *
 * - **No output shaping.** This class returns [ReasoningProviderResponse]
 *   exactly as [ReasoningProvider.reason] produced it -- translating it
 *   into an `EvidenceAnalysisResult` variant is Unit 4's and Unit 5's own
 *   responsibility, never this Unit's.
 * - **No acceptance, no permission composition, no runtime wiring.**
 *   None of Units 6, 7, or 8 exist in this file in any form.
 * - **No provider selection.** Exactly one, already-configured
 *   [ReasoningProvider] is invoked per call; this class holds no
 *   registry, router, or selection logic of any kind.
 *
 * @param reasoningProvider The already-configured [ReasoningProvider] to
 *   orchestrate. The absence of any other constructor parameter is
 *   itself the structural guarantee that this class cannot reach
 *   `PlannerRuntime`, `ExecutionPipeline`, `MemoryCore`'s write
 *   interface, or Knowledge Memory's submission interface.
 */
internal class EvidenceIntelligenceReasoningCoordinator(
    private val reasoningProvider: ReasoningProvider,
) {
    suspend fun reason(
        request: EvidenceAnalysisRequest,
        reasoningContext: ReasoningContext,
    ): ReasoningProviderResponse {
        val providerRequest = ReasoningProviderRequest(
            subject = ReasoningSubject.OfEvidenceAnalysisRequest(request),
            reasoningContext = reasoningContext,
        )
        return reasoningProvider.reason(providerRequest)
    }
}
