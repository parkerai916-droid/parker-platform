package parker.core.runtime

import parker.core.interfaces.ObservationResult
import parker.core.interfaces.WorldBelief
import parker.core.interfaces.WorldModelUpdatePolicy
import parker.core.interfaces.WorldObservation

/**
 * Test-only fake, mirroring [FakeMemoryPromotionPolicy]/[FakePermissionEngine]'s
 * lambda-based fake precedent. Exists so [InMemoryWorldModelTest] can
 * prove [InMemoryWorldModel]'s own *orchestration* (does [InMemoryWorldModel.observe]
 * consult [WorldModelUpdatePolicy] internally exactly once, and correctly
 * branch on `Accepted`/`Invalidated`/`Rejected`) independently of
 * [DefaultWorldModelUpdatePolicy]'s own evaluation logic, which
 * [DefaultWorldModelUpdatePolicyTest] covers on its own.
 */
class FakeWorldModelUpdatePolicy(
    private val resultFor: (WorldObservation, WorldBelief?) -> ObservationResult,
    private val currentFor: (WorldBelief) -> Boolean = { true },
) : WorldModelUpdatePolicy {

    var evaluateCallCount: Int = 0
        private set

    var isStillCurrentCallCount: Int = 0
        private set

    override suspend fun evaluate(observation: WorldObservation, existing: WorldBelief?): ObservationResult {
        evaluateCallCount++
        return resultFor(observation, existing)
    }

    override suspend fun isStillCurrent(belief: WorldBelief): Boolean {
        isStillCurrentCallCount++
        return currentFor(belief)
    }
}
