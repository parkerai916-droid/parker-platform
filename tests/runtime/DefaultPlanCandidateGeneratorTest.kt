package parker.core.runtime

import kotlinx.coroutines.test.runTest
import parker.core.interfaces.PlanCandidateGenerator
import parker.core.interfaces.PlanCandidateId
import parker.core.interfaces.PlanningRequest
import parker.core.interfaces.PlanningSessionId
import parker.core.interfaces.PrincipalId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * `DefaultPlanCandidateGenerator` acceptance test, per
 * `docs/implementation/CANDIDATE_GENERATION_SCOPE_LOCK.md` Section 5.
 *
 * **Zero-candidate and multiple-candidate results are not tested here,
 * deliberately.** `DefaultPlanCandidateGenerator`'s own frozen policy
 * (Scope Lock Section 3.3, Section 3.6) always returns exactly one
 * candidate for every well-formed [PlanningRequest] -- it has no branch
 * that could ever produce zero or more than one. Testing those paths
 * against this concrete implementation would fabricate coverage for
 * behaviour this class does not, and is not authorised to, have. The
 * [PlanCandidateGenerator] contract itself permits zero/many for a future,
 * richer implementation (Contract Design Section 5) -- not this one.
 *
 * **No exception-propagation test exists here, deliberately.**
 * `DefaultPlanCandidateGenerator.generate` cannot throw for any
 * well-formed [PlanningRequest] -- see [DefaultPlanCandidateGenerator]'s
 * own KDoc, "Determinism; no fault surface." There is no dependency and
 * no policy branch to fault-inject against.
 */
class DefaultPlanCandidateGeneratorTest {

    private val generator = DefaultPlanCandidateGenerator()

    private fun request(
        planningSessionId: String = "session-1",
        initiatingPrincipalId: String = "user-1",
        goal: String = "book a flight to Denver",
        correlationId: String = "corr-1",
    ) = PlanningRequest(
        planningSessionId = PlanningSessionId(planningSessionId),
        initiatingPrincipalId = PrincipalId(initiatingPrincipalId),
        goal = goal,
        correlationId = correlationId,
    )

    // ================= 1. Implements the PlanCandidateGenerator contract =================

    @Test
    fun `implements the PlanCandidateGenerator contract`() {
        assertTrue(generator is PlanCandidateGenerator)
    }

    // ================= 2. Returns exactly one candidate =================

    @Test
    fun `returns exactly one candidate for a well-formed PlanningRequest`() = runTest {
        val result = generator.generate(request())

        assertEquals(1, result.size)
    }

    // ================= 3. Copies Goal text verbatim =================

    @Test
    fun `copies the Goal text verbatim, including internal whitespace, with no trimming or normalisation`() = runTest {
        val verbatimGoal = "  Feed the cat twice daily  -- morning AND evening  "

        val result = generator.generate(request(goal = verbatimGoal))

        assertEquals(verbatimGoal, result.single().goal)
    }

    // ================= 4. Produces the exact expected ID =================

    @Test
    fun `produces a candidate ID of the exact form planningSessionId-candidate-1`() = runTest {
        val result = generator.generate(request(planningSessionId = "session-77"))

        assertEquals(PlanCandidateId("session-77-candidate-1"), result.single().planCandidateId)
    }

    // ================= 5. Identical output for identical input =================

    @Test
    fun `produces identical output for identical input`() = runTest {
        val first = generator.generate(request())
        val second = generator.generate(request())

        assertEquals(first, second)
    }

    // ================= 6. Distinct IDs for distinct planning-session IDs =================

    @Test
    fun `produces distinct candidate IDs for distinct planningSessionId values`() = runTest {
        val first = generator.generate(request(planningSessionId = "session-A"))
        val second = generator.generate(request(planningSessionId = "session-B"))

        assertNotEquals(first.single().planCandidateId, second.single().planCandidateId)
    }

    // ================= 7. Output order is stable =================

    @Test
    fun `output list order is stable across repeated calls with the same input`() = runTest {
        val req = request(planningSessionId = "session-stable")

        val first = generator.generate(req)
        val second = generator.generate(req)

        // A single-element list has only one possible order; this asserts that
        // order (index 0) resolves to the same value on every call -- the list is
        // never assembled from a Map or Set whose iteration order could vary.
        assertEquals(first[0], second[0])
    }

    // ================= 8. Optional fields remain at their existing defaults =================

    @Test
    fun `leaves every optional PlanCandidate field at its existing default`() = runTest {
        val candidate = generator.generate(request()).single()

        assertEquals(null, candidate.riskEstimate)
        assertEquals(emptySet(), candidate.requiredCapabilities)
        assertEquals(emptySet(), candidate.anticipatedPermissionActions)
        assertEquals(emptyList(), candidate.constraints)
        assertEquals(emptyList(), candidate.dependencies)
        assertEquals(emptyList(), candidate.contextReferences)
        assertEquals(emptyList(), candidate.resourceReferences)
        assertEquals("", candidate.expectedOutputs)
    }

    // ================= 9. Rationale is fixed and truthfully discloses its own limits =================

    @Test
    fun `rationale is fixed and discloses direct Goal derivation, no decomposition, no alternatives, and no deliberation`() = runTest {
        val first = generator.generate(request(goal = "water the plants")).single()
        val second = generator.generate(request(goal = "an entirely different goal")).single()

        // Fixed: identical across two different Goals -- never derived from the Goal's own content.
        assertEquals(first.rationale, second.rationale)

        assertTrue("verbatim" in first.rationale)
        assertTrue("PlanningRequest.goal" in first.rationale)
        assertTrue("No decomposition" in first.rationale)
        assertTrue("no alternative generation" in first.rationale)
        assertTrue("no deliberation" in first.rationale)
    }

    // ================= 10. Structural: zero-argument constructor, no stored dependencies =================

    @Test
    fun `has a zero-argument constructor and declares no stored dependency`() {
        val constructor = DefaultPlanCandidateGenerator::class.java.declaredConstructors.single()

        assertEquals(0, constructor.parameterCount)
        assertTrue(DefaultPlanCandidateGenerator::class.java.declaredFields.isEmpty())
    }

    // ================= 11. Structural proof of no prohibited dependency =================

    @Test
    fun `structurally cannot reference PlannerRuntime, TaskManagerRuntime, the Permission Engine, the Tool Registry, ReasoningContext, Conversation, Memory, or the World Model`() {
        // Zero declared fields (proven above) is the strongest possible structural
        // guarantee available in this codebase's own convention (stronger than a
        // minimum-dependency list): a class with no fields at all cannot hold a
        // reference to PlannerRuntime, TaskManagerRuntime, PermissionEngine,
        // ToolRegistry, ReasoningContext, ConversationEngine/ConversationHistorySource,
        // MemoryStore/MemorySource, or WorldModel/WorldModelSource -- there is no
        // field for any such reference to occupy.
        val fieldNames = DefaultPlanCandidateGenerator::class.java.declaredFields.map { it.name }.toSet()

        assertEquals(emptySet(), fieldNames)
    }
}
