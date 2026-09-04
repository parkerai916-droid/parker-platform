package parker.composition

import java.time.Instant
import parker.core.interfaces.*
import parker.core.runtime.HumanCorrectionPermissionPolicy

internal class HumanCorrectionExactTargetRegistrar(
    private val resources: ResourceRegistry,
    private val owner: PrincipalId,
    private val clock: () -> Instant,
) {
    suspend fun register(target: HumanFidelityReviewTarget, reviewId: HumanFidelityReviewId): ResourceId {
        val id = HumanCorrectionPermissionPolicy.resourceIdFor(target, reviewId); val now = clock()
        val expected = Resource(id, ResourceType.DOCUMENT, "Human-corrected representation target", owner,
            ResourceSensitivity.LEGAL, ResourceLifecycleState.AVAILABLE, now, now,
            "composition-root:human-transcription-correction")
        val existing = resources.resolve(id)
        if (existing == null) resources.register(expected) else require(existing.resourceId == id &&
            existing.ownerPrincipalId == owner && existing.resourceType == ResourceType.DOCUMENT &&
            existing.sensitivity == ResourceSensitivity.LEGAL && existing.lifecycleState == ResourceLifecycleState.AVAILABLE &&
            existing.source == expected.source)
        return id
    }
}
