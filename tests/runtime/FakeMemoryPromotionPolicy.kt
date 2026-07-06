package parker.core.runtime

import parker.core.interfaces.CandidateMemory
import parker.core.interfaces.MemoryId
import parker.core.interfaces.MemoryPromotionDecision
import parker.core.interfaces.MemoryPromotionPolicy

/**
 * Test-only fake, mirroring [FakePermissionEngine]/`FakeTaskProposalIntake`'s
 * lambda-based fake precedent. Exists so [InMemoryMemoryStoreTest] can
 * prove [InMemoryMemoryStore]'s own *orchestration* (does [InMemoryMemoryStore.remember]
 * consult [MemoryPromotionPolicy] internally, and correctly branch on
 * `Promote`/`Reject`) independently of [DefaultMemoryPromotionPolicy]'s
 * own promotion-factor logic, which [DefaultMemoryPromotionPolicyTest]
 * covers on its own.
 */
class FakeMemoryPromotionPolicy(
    private val decisionFor: (CandidateMemory, MemoryId) -> MemoryPromotionDecision,
) : MemoryPromotionPolicy {

    var evaluateCallCount: Int = 0
        private set

    override suspend fun evaluate(candidate: CandidateMemory, memoryId: MemoryId): MemoryPromotionDecision {
        evaluateCallCount++
        return decisionFor(candidate, memoryId)
    }
}
