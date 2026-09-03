package parker.composition

import java.time.Instant
import parker.core.interfaces.HumanFidelityReviewTarget
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.Resource
import parker.core.interfaces.ResourceId
import parker.core.interfaces.ResourceLifecycleState
import parker.core.interfaces.ResourceRegistry
import parker.core.interfaces.ResourceSensitivity
import parker.core.interfaces.ResourceType
import parker.core.runtime.HumanFidelityReviewRecordingPermissionPolicy

/** Production-composed, exact-target-only registration seam for a later separately governed act. */
internal class HumanFidelityReviewExactTargetRegistrar(
    private val resourceRegistry: ResourceRegistry,
    private val ownerPrincipalId: PrincipalId,
    private val clock: () -> Instant,
) {
    suspend fun register(target: HumanFidelityReviewTarget): ResourceId {
        val resourceId = HumanFidelityReviewRecordingPermissionPolicy.resourceIdFor(target)
        val now = clock()
        val expected = Resource(
            resourceId = resourceId,
            resourceType = ResourceType.DOCUMENT,
            displayName = "Human fidelity review recording target",
            ownerPrincipalId = ownerPrincipalId,
            sensitivity = ResourceSensitivity.LEGAL,
            lifecycleState = ResourceLifecycleState.AVAILABLE,
            createdAt = now,
            updatedAt = now,
            source = SOURCE,
        )
        val existing = resourceRegistry.resolve(resourceId)
        if (existing == null) {
            resourceRegistry.register(expected)
        } else {
            require(
                existing.resourceId == expected.resourceId &&
                    existing.resourceType == expected.resourceType &&
                    existing.ownerPrincipalId == expected.ownerPrincipalId &&
                    existing.sensitivity == expected.sensitivity &&
                    existing.lifecycleState == expected.lifecycleState &&
                    existing.source == expected.source,
            ) { "Exact human fidelity review target resource conflicts with existing registration" }
        }
        return resourceId
    }

    private companion object {
        const val SOURCE = "composition-root:human-fidelity-review-recording"
    }
}
