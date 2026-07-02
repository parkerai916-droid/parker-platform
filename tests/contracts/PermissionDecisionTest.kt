package parker.core.interfaces

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class PermissionDecisionTest {

    private fun decision(outcome: PermissionDecisionOutcome = PermissionDecisionOutcome.APPROVED) = PermissionDecision(
        decisionId = DecisionId("dec-1"),
        principalId = PrincipalId("user-1"),
        resourceId = ResourceId("doc-1"),
        action = PermissionAction.READ,
        decision = outcome,
        level = PermissionLevel.AUTOMATIC,
        timestamp = Instant.parse("2026-01-01T00:00:00Z"),
    )

    @Test
    fun `answers the core question -- principal, action, resource, outcome are all present`() {
        val d = decision()

        assertEquals(PrincipalId("user-1"), d.principalId)
        assertEquals(ResourceId("doc-1"), d.resourceId)
        assertEquals(PermissionAction.READ, d.action)
        assertEquals(PermissionDecisionOutcome.APPROVED, d.decision)
    }

    @Test
    fun `all four decision outcomes from Permission md are representable`() {
        val outcomes = setOf(
            PermissionDecisionOutcome.APPROVED,
            PermissionDecisionOutcome.APPROVED_WITH_CONFIRMATION,
            PermissionDecisionOutcome.DEFERRED,
            PermissionDecisionOutcome.DENIED,
        )

        assertEquals(4, outcomes.size)
        outcomes.forEach { decision(outcome = it) }
    }

    @Test
    fun `decisions with identical fields are equal, different decisionIds are not`() {
        assertEquals(decision(), decision())
        assertNotEquals(decision().decisionId, DecisionId("dec-2"))
    }
}
