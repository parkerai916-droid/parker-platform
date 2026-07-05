package parker.core.runtime

import java.time.Instant
import parker.core.interfaces.DecisionId
import parker.core.interfaces.ExecutionRequest
import parker.core.interfaces.IdentityService
import parker.core.interfaces.PermissionAction
import parker.core.interfaces.PermissionDecision
import parker.core.interfaces.PermissionDecisionOutcome
import parker.core.interfaces.PermissionEngine
import parker.core.interfaces.PermissionExplanation
import parker.core.interfaces.PermissionLevel
import parker.core.interfaces.PrincipalStatus
import parker.core.interfaces.ResourceId

/**
 * Sprint 2, Unit A1 (`docs/implementation/SPRINT_2_IMPLEMENTATION_PLAN.md`):
 * the platform's first real, non-test-fixture [PermissionEngine]
 * implementation. Closes `IMPLEMENTATION_GAPS.md` #40 by resolving
 * [ExecutionRequest.principalId] via [identityService] as the first step
 * of [evaluate], per `docs/architecture/IdentityService.md`'s
 * "Integration with Permission Engine": `evaluate` "MUST resolve
 * `request.principalId` via the Identity Service as its first step,"
 * short-circuiting to `DENIED` before any other decision logic runs for
 * a Principal that is not in good standing.
 *
 * **Sprint 2, Unit A2 update:** the Unit A1 placeholder
 * (`decisionFor: (ExecutionRequest) -> PermissionDecision`) has been
 * replaced by [policy] -- a real [DefaultPermissionPolicy] implementing
 * `docs/specifications/volume-03-core-interfaces/PermissionPolicy.md`.
 * `IMPLEMENTATION_GAPS.md` #25 is closed by this change (see
 * `DefaultPermissionPolicy`'s own KDoc for the policy mechanism itself).
 * For any Principal this class does not itself deny, [evaluate] still
 * delegates unchanged to [policy] -- this class remains identity-status
 * enforcement only; it does not itself decide policy outcomes.
 * `tests/runtime/FakePermissionEngine.kt` remains untouched and remains
 * `DefaultExecutionPipelineTest`'s own fixture.
 *
 * ## Status coverage (an explicit interpretive decision, recorded here
 * for review, the same way `InMemoryIdentityService.register`'s
 * owner-validation interpretation was recorded against
 * `IMPLEMENTATION_GAPS.md` #38)
 *
 * `IdentityService.md`'s own normative text names only `Suspended` and
 * `Revoked` for the short-circuit. This implementation also denies
 * `Archived` -- a safe superset, since `PrincipalLifecycleTransitions`'s
 * strict linear chain (`Created -> Active -> Suspended -> Revoked ->
 * Archived`) means a Principal cannot reach `Archived` without having
 * already passed through `Revoked`. Neither the specification nor this
 * unit's own instructions address `Created` explicitly. This
 * implementation treats `Created` as `DENIED` as well -- only `Active`
 * reaches [policy]'s `evaluate` -- on the grounds that a Principal which
 * has been registered but never activated should not be authorised by
 * default. This is a deliberate, narrow interpretation, not a claim that
 * `IdentityService.md` settles the question either way.
 */
class DefaultPermissionEngine(
    private val identityService: IdentityService,
    private val policy: DefaultPermissionPolicy,
) : PermissionEngine {

    override suspend fun evaluate(request: ExecutionRequest): PermissionDecision {
        val principal = identityService.resolve(request.principalId)
            ?: return deniedDecision(request)

        return when (principal.status) {
            PrincipalStatus.SUSPENDED,
            PrincipalStatus.REVOKED,
            PrincipalStatus.ARCHIVED,
            PrincipalStatus.CREATED,
            -> deniedDecision(request)
            PrincipalStatus.ACTIVE -> policy.evaluate(request)
        }
    }

    /**
     * `PermissionExplanation`'s shape is PROVISIONAL (`src/contracts/Permission.kt`'s
     * own KDoc) -- no specification defines what an explanation actually
     * contains, and this unit does not invent one. This implementation
     * keeps no record of past decisions (that would be new, unspecified
     * state), so it returns the simplest contract-compatible explanation,
     * mirroring `FakePermissionEngine.explain`'s own minimal precedent.
     */
    override suspend fun explain(decisionId: DecisionId): PermissionExplanation =
        PermissionExplanation(decisionId, "DefaultPermissionEngine does not retain decision history")

    /**
     * Builds a self-contained `DENIED` [PermissionDecision] for the
     * identity-status short-circuit cases above. [PermissionDecision.resourceId]
     * and [PermissionDecision.action] are populated from [request] only to
     * satisfy the data class's shape -- `DefaultExecutionPipeline` never
     * reads either field on the `DENIED` path (only `APPROVED`/
     * `APPROVED_WITH_CONFIRMATION` ever reads `decision.action`, to
     * resolve a Tool), so no policy meaning should be inferred from
     * either value here. Carries no `reason` string of its own --
     * `PermissionDecision` has no such field (only [PermissionExplanation]
     * does), and this unit does not add a decision-reason lookup beyond
     * what [explain] already provides, per this unit's own "do not invent
     * policy explanations beyond what is needed" scope.
     */
    private fun deniedDecision(request: ExecutionRequest): PermissionDecision = PermissionDecision(
        decisionId = DecisionId("dec-identity-${request.requestId.value}"),
        principalId = request.principalId,
        resourceId = request.targetResources.firstOrNull() ?: ResourceId("no-target-resource"),
        action = PermissionAction.EXECUTE,
        decision = PermissionDecisionOutcome.DENIED,
        level = PermissionLevel.AUTOMATIC,
        timestamp = Instant.now(),
    )
}
