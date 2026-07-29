package parker.core.runtime

import parker.core.interfaces.CandidateKnowledge
import parker.core.interfaces.KnowledgeId
import parker.core.interfaces.KnowledgePromotionDecision
import parker.core.interfaces.KnowledgePromotionPolicy

/**
 * Test-only fake, mirroring [FakePermissionEngine]/`FakeTaskProposalIntake`'s
 * lambda-based fake precedent. Exists so [InMemoryMemoryStoreTest] can
 * prove [InMemoryKnowledgeStore]'s own *orchestration* (does [InMemoryKnowledgeStore.remember]
 * consult [KnowledgePromotionPolicy] internally, and correctly branch on
 * `Promote`/`Reject`) independently of [DefaultKnowledgePromotionPolicy]'s
 * own promotion-factor logic, which [DefaultMemoryPromotionPolicyTest]
 * covers on its own.
 */
class FakeMemoryPromotionPolicy(
    private val decisionFor: (CandidateKnowledge, KnowledgeId) -> KnowledgePromotionDecision,
) : KnowledgePromotionPolicy {

    var evaluateCallCount: Int = 0
        private set

    override suspend fun evaluate(candidate: CandidateKnowledge, memoryId: KnowledgeId): KnowledgePromotionDecision {
        evaluateCallCount++
        return decisionFor(candidate, memoryId)
    }
}
