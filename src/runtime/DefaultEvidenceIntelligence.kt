package parker.core.runtime

import parker.core.interfaces.EvidenceAnalysisRequest
import parker.core.interfaces.EvidenceAnalysisResult
import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.EvidenceIntelligence
import parker.core.interfaces.EvidenceRetrievalResult
import parker.core.interfaces.ReasoningContext
import parker.core.interfaces.ReasoningProviderResponse
import parker.core.interfaces.RelationshipEndpoint

/**
 * Evidence Intelligence, Implementation Unit 5 ("The Evidence Intelligence
 * Operation"). Governed in full by
 * `docs/implementation/EVIDENCE_INTELLIGENCE_IMPLEMENTATION_PLAN.md`
 * ("the Implementation Plan"), §8 Unit 5, and by this Unit's own accepted
 * `docs/reviews/EVIDENCE_INTELLIGENCE_UNIT_5_PLANNING_AND_BOUNDARY_REVIEW.md`
 * ("the Planning Review", Verdict A) -- this class implements exactly
 * that Unit's own responsibilities, and only those: it assembles Units
 * 2-4, already implemented and frozen, behind the one public
 * [EvidenceIntelligence] operation. It introduces no new public type of
 * its own (the interface it implements is the fourth and final one
 * Contract Design §10 authorises), no new sealed variant, and no
 * dependency beyond what Units 2 and 3 already declare.
 *
 * **Whole reuse, not re-derivation** (mirroring
 * [CommunicationConversationCoordinator]'s own identical "Decision 3"
 * precedent): this class depends on an already-constructed
 * [EvidenceIntelligenceInputResolver] (Unit 2) and an optional,
 * already-constructed [EvidenceIntelligenceReasoningCoordinator] (Unit
 * 3) -- never on the lower-level [parker.core.interfaces.EvidenceCustodian],
 * [parker.core.interfaces.MemoryRetrieval], or
 * [parker.core.interfaces.ReasoningProvider] types those Units already
 * wrap, and never by re-implementing either Unit's own sequencing logic.
 * [reasoningCoordinator] is nullable because Reasoning Provider
 * orchestration is optional -- Contract Design §12's own "zero or more"
 * -- deciding, once, at composition time, whether this operation ever
 * invokes a Reasoning Provider at all, rather than by any
 * analysis-kind-specific, per-request algorithm this Unit is not
 * authorised to build (Scope Lock §3; Implementation Plan §11).
 *
 * ## The ReasoningContext limitation (Planning Review §5), preserved, not
 * worked around
 *
 * No governing document assigns responsibility for assembling a
 * non-empty [ReasoningContext] for an Evidence-Intelligence-invoked
 * Reasoning Provider. [parker.core.interfaces.EvidenceAnalysisRequest.reasoningContext]
 * is not a lawful source: the Reasoning Provider Contract Design's own
 * Amendment 1 invariant states that field, "for Evidence Intelligence's
 * own purposes, is not read, not merged, not compared, and not otherwise
 * given any effect on this invocation." The one existing production
 * assembler, [DefaultReasoningContextAssembler], is governed by entirely
 * separate documents and shaped for
 * [parker.core.interfaces.ResolvedInboundMessage], structurally
 * incompatible with [EvidenceAnalysisRequest]. Building a new assembler
 * would be an unauthorised seventh new component (Implementation Plan
 * §6: "No seventh new component is introduced anywhere in this plan").
 * This class therefore supplies [ReasoningContext.entries] as
 * [emptyList] directly, at the one invocation point below -- the
 * trivial, always-lawful value [ReasoningContext]'s own constructor
 * already permits -- and introduces no assembly, caching, enrichment, or
 * derivation mechanism of any kind. This is a disclosed, permanent
 * limitation of this Unit as governed today, not a defect silently
 * worked around.
 *
 * ## The response-to-result conversion is isolated, and is implementation
 * judgement, not contract logic
 *
 * No governing document defines a mapping from [ReasoningProviderResponse]
 * to [EvidenceAnalysisResult] -- by design, not omission: constructing a
 * genuine analytical result from a resolved input and an optional
 * reasoning response is exactly the "specific analysis kind's own
 * internal algorithm" every tier of this Programme's governance
 * permanently excludes (Contract Design, Out of Scope; Scope Lock §3;
 * Implementation Plan §11). [convertReasoningResponse] is the single,
 * isolated point where that ordinary engineering judgement is exercised;
 * no other function in this class inspects a [ReasoningProviderResponse]
 * or constructs an [EvidenceAnalysisResult].
 *
 * ## What this Unit deliberately does not implement
 *
 * - **No invocation permission gating (Unit 6).** This class holds no
 *   `PermissionEngine` reference of its own; whichever composing caller
 *   invokes [analyse] is responsible for evaluating the dedicated
 *   proposal class first (Scope Lock §6, step 0; §7).
 * - **No acceptance orchestration (Unit 7).** Nothing here calls
 *   `EvidenceCustodian.accept`, `MemoryCore`'s write interface, or
 *   Knowledge Memory's Knowledge Submission interface.
 * - **No runtime composition (Unit 8).** This class is not wired into
 *   `ParkerRuntime` by this Unit.
 * - **No analysis-kind-specific algorithm.** [convertReasoningResponse]
 *   performs the one generic, kind-independent classification Contract
 *   Design §5 itself already authorises (Reasoning-Provider-authored
 *   prose is, by definition, "model-generated explanatory language") --
 *   it does not perform comparison, OCR, transcription, translation, or
 *   any other named analysis kind's own algorithm.
 *
 * `internal`, not public -- exactly as [EvidenceIntelligenceInputResolver]
 * and [EvidenceIntelligenceReasoningCoordinator] themselves already are:
 * this class is Unit 5 implementation plumbing, not one of the four
 * public runtime types the Contract Design and Scope Lock authorise. A
 * public constructor could not expose either of those two `internal`
 * constructor parameter types regardless (Kotlin visibility rules);
 * callers depend on the public [EvidenceIntelligence] interface this
 * class implements, never on this concrete class by name.
 */
internal class DefaultEvidenceIntelligence(
    private val inputResolver: EvidenceIntelligenceInputResolver,
    private val reasoningCoordinator: EvidenceIntelligenceReasoningCoordinator?,
) : EvidenceIntelligence {

    /**
     * Implements the sequence Scope Lock §6 (steps 1-3) fixes for this
     * operation's own portion of it: resolve inputs (Unit 2); optionally
     * reason over the successfully resolved subset (Unit 3), supplying an
     * empty [ReasoningContext] directly at the invocation point; return a
     * list of [EvidenceAnalysisResult] values -- zero, one, or many. This
     * operation's own contract ends at that return; step 0 (permission
     * gating) and step 4 (acceptance) are never performed here.
     *
     * A request naming nothing to analyse is rejected by
     * [EvidenceIntelligenceInputResolver.resolve] itself, before any
     * further step -- this operation does not duplicate that check.
     *
     * **Partial completion.** Only the subset of referenced inputs that
     * actually resolved ([EvidenceRetrievalResult.Found] evidence, non-null
     * Memory Core records) is ever cited as a governed reference in any
     * produced [EvidenceAnalysisResult.TransientOutput]. An input that
     * failed to resolve is never fabricated into a result and never
     * silently cited as if it had succeeded; its own disposition, already
     * disclosed by [EvidenceIntelligenceInputResolver.resolve]'s own return
     * shape, is this operation's complete discharge of that disclosure
     * obligation (Contract Design §11; Implementation Plan §8 Unit 2) --
     * no further wrapper, logging, or side channel is introduced here.
     *
     * **Reasoning is only attempted once at least one input has resolved.**
     * With nothing successfully resolved, there is no lawful, non-empty
     * reference set any resulting [EvidenceAnalysisResult.TransientOutput]
     * could cite (its own constructor requires at least one), so this
     * operation returns an empty list rather than invoking a Reasoning
     * Provider it could not honestly attribute a response to -- a
     * confident "nothing to propose" outcome, not a failure (Contract
     * Design §5, §11).
     */
    override suspend fun analyse(request: EvidenceAnalysisRequest): List<EvidenceAnalysisResult> {
        val (evidenceResults, memoryCoreResults) = inputResolver.resolve(request)

        val resolvedEvidenceArtifactIds = evidenceResults
            .filterIsInstance<EvidenceRetrievalResult.Found>()
            .map { it.evidenceArtifactId }
        val resolvedMemoryCoreReferences = memoryCoreResults
            .filter { (_, record) -> record != null }
            .map { (endpoint, _) -> endpoint }

        if (resolvedEvidenceArtifactIds.isEmpty() && resolvedMemoryCoreReferences.isEmpty()) {
            return emptyList()
        }

        val coordinator = reasoningCoordinator ?: return emptyList()

        val response = coordinator.reason(request, ReasoningContext(emptyList()))

        return convertReasoningResponse(response, resolvedEvidenceArtifactIds, resolvedMemoryCoreReferences)
    }

    /**
     * The single, isolated point at which a [ReasoningProviderResponse] --
     * a Reasoning Provider's own, contract-defined output, carrying only
     * prose (Reasoning Provider Contract Design §3) -- becomes zero or
     * more [EvidenceAnalysisResult] values. This is ordinary implementation
     * judgement, not contract logic: no governing document defines this
     * mapping (see this class's own header KDoc).
     *
     * - [ReasoningProviderResponse.Reply] becomes exactly one
     *   [EvidenceAnalysisResult.TransientOutput], citing every
     *   successfully resolved reference from this invocation. Its own
     *   text is labelled [AnalyticalOutputDiscipline.MODEL_GENERATED] --
     *   not an analysis-kind-specific judgement, but the one
     *   classification Contract Design §5 already fixes for any prose "a
     *   Reasoning Provider Evidence Intelligence orchestrates... produces
     *   to explain, summarise, or narrate," true of this text regardless
     *   of `analysisKind`.
     * - [ReasoningProviderResponse.NoAction] becomes an empty list -- a
     *   confident semantic determination that nothing is warranted, never
     *   itself a failure (Reasoning Provider Contract Design §3; Contract
     *   Design §5).
     * - [ReasoningProviderResponse.Goal] has no lawful
     *   [EvidenceAnalysisResult] mapping -- no category corresponds to "a
     *   goal worth planning," and Evidence Intelligence holds no Planner
     *   Runtime dependency to route one to (Scope Lock §3, explicit
     *   exclusion). Nothing in this Programme's governance authorises a
     *   Reasoning Provider Evidence Intelligence orchestrates to return
     *   one, but nothing forbids it either (`ReasoningProvider.reason`'s
     *   own signature carries no such restriction) -- this is therefore
     *   treated as a genuine implementation-level anomaly and faulted,
     *   never silently coerced into one of the four categories (Planning
     *   Review §4).
     */
    private fun convertReasoningResponse(
        response: ReasoningProviderResponse,
        evidenceArtifactReferences: List<EvidenceArtifactId>,
        memoryCoreReferences: List<RelationshipEndpoint>,
    ): List<EvidenceAnalysisResult> = when (response) {
        is ReasoningProviderResponse.Reply -> listOf(
            EvidenceAnalysisResult.TransientOutput(
                text = AnalyticalOutputDiscipline.labelContent(AnalyticalOutputDiscipline.MODEL_GENERATED, response.text),
                evidenceArtifactReferences = evidenceArtifactReferences,
                memoryCoreReferences = memoryCoreReferences,
            ),
        )

        ReasoningProviderResponse.NoAction -> emptyList()

        is ReasoningProviderResponse.Goal -> throw UnsupportedOperationException(
            "ReasoningProviderResponse.Goal has no lawful EvidenceAnalysisResult mapping " +
                "(docs/reviews/EVIDENCE_INTELLIGENCE_UNIT_5_PLANNING_AND_BOUNDARY_REVIEW.md, Section 4)",
        )

        // Parker Conversational Memory Bridge, Admission Unit: Remember is a conversation-turn
        // classification (Conversation Reply Coordinator's own new, sole consumer) -- Evidence
        // Intelligence's own Reasoning Provider invocation is a wholly separate reasoning subject
        // (ReasoningSubject.OfEvidenceAnalysisRequest, never OfTurn), and no governance authorises a
        // lawful EvidenceAnalysisResult mapping for it, mirroring Goal's own identical treatment
        // immediately above -- a genuine implementation-level anomaly, faulted, never silently
        // coerced.
        is ReasoningProviderResponse.Remember -> throw UnsupportedOperationException(
            "ReasoningProviderResponse.Remember has no lawful EvidenceAnalysisResult mapping",
        )
    }
}
