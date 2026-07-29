package parker.core.runtime

import parker.core.interfaces.CandidateKnowledge
import parker.core.interfaces.KnowledgeId
import parker.core.interfaces.KnowledgePromotionDecision
import parker.core.interfaces.KnowledgePromotionPolicy

/**
 * Sprint 4, Track A, Unit A3. The concrete, deterministic
 * [KnowledgePromotionPolicy] this Unit supplies, mirroring
 * `DefaultPlanDecision`'s own role for the Planner Runtime.
 *
 * Implements two of `docs/architecture/33-memory-consolidation.md`'s six
 * named promotion factors -- explicit request and confidence --
 * deterministically and without randomness:
 *
 * 1. If [CandidateKnowledge.explicitlyRequested] is `true`, the candidate is
 *    promoted unconditionally: a direct user request to remember
 *    something is the strongest, most direct promotion signal that
 *    document names, and this Unit treats it as sufficient on its own.
 * 2. Otherwise, if [CandidateKnowledge.confidence] is present and at least
 *    [confidenceThreshold], the candidate is promoted.
 * 3. Otherwise, the candidate is rejected, with a plain-language reason
 *    naming which of the two rules it failed to satisfy.
 *
 * Calling [evaluate] twice with the same arguments always produces an
 * identical [KnowledgePromotionDecision] -- no clock read, no random value,
 * and no external state is consulted.
 *
 * **Deliberately not implemented: repetition, (non-explicit) user
 * importance, goal relevance, and frequency.** Each of these requires
 * comparing a submission against a population of Memory's own existing
 * records, and neither `docs/architecture/MEMORY_CONTRACT_DESIGN.md` nor
 * this Unit's own instructions shape a way to supply that population to
 * [KnowledgePromotionPolicy.evaluate] -- inventing one now would be
 * inventing architecture mid-Kotlin, which this Unit's instructions
 * explicitly forbid. This is recorded as a genuine, disclosed
 * implementation gap, not silently dropped -- see
 * `docs/architecture/IMPLEMENTATION_GAPS.md`.
 */
class DefaultKnowledgePromotionPolicy(
    private val confidenceThreshold: Double = DEFAULT_CONFIDENCE_THRESHOLD,
) : KnowledgePromotionPolicy {

    override suspend fun evaluate(candidate: CandidateKnowledge, memoryId: KnowledgeId): KnowledgePromotionDecision =
        when {
            candidate.explicitlyRequested ->
                KnowledgePromotionDecision.Promote(memoryId, candidate.proposedCategory)

            candidate.confidence != null && candidate.confidence >= confidenceThreshold ->
                KnowledgePromotionDecision.Promote(memoryId, candidate.proposedCategory)

            else ->
                KnowledgePromotionDecision.Reject(
                    memoryId,
                    "not explicitly requested, and confidence " +
                        "(${candidate.confidence?.toString() ?: "absent"}) is below the promotion " +
                        "threshold ($confidenceThreshold)",
                )
        }

    companion object {
        const val DEFAULT_CONFIDENCE_THRESHOLD = 0.7
    }
}
