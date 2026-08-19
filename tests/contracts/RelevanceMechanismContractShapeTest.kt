package parker.core.interfaces

import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.functions
import kotlin.reflect.full.memberProperties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

/**
 * Programme 3, Unit 9.7.1 (Relevance Contract Types). Structural tests
 * over `src/interfaces/RelevanceMechanism.kt`, mirroring
 * `InterfaceContractShapeTest.kt`'s own established reflection-based
 * structural-guarantee convention (`kotlin.reflect.full`, declared
 * function/property inspection, `isSuspend`, parameter and return-type
 * classifiers). Proves, by reflection rather than by comment, exactly the
 * two structural guarantees
 * `docs/governance/PROGRAMME_3_UNIT_9_7_BOUNDED_SEMANTIC_RELEVANCE_IMPLEMENTATION_PLAN.md`
 * §8's own Unit 9.7.1 entry requires ("Tests required"), plus the
 * additional applicable value-type properties this task's own Phase 4
 * names.
 */
class RelevanceMechanismContractShapeTest {

    // ---- Required test 1: the mechanism interface's own declared
    // signature cannot express a write, a permission decision, or a
    // lifecycle/evidential-state assertion. ----

    @Test
    fun `RelevanceMechanism declares exactly one suspend rank function`() {
        val functions = RelevanceMechanism::class.functions.filter { it.name == "rank" }
        assertEquals(1, functions.size, "RelevanceMechanism must declare exactly one rank function")
        assertTrue(functions.single().isSuspend, "RelevanceMechanism.rank must be suspend")
    }

    @Test
    fun `RelevanceMechanism rank accepts only a RelevanceRequest and returns only a RelevanceResult`() {
        val rank = RelevanceMechanism::class.functions.single { it.name == "rank" }
        val valueParameters = rank.parameters.filter { it.kind == KParameter.Kind.VALUE }

        assertEquals(1, valueParameters.size, "RelevanceMechanism.rank must accept exactly one parameter")
        assertEquals(RelevanceRequest::class, valueParameters.single().type.classifier)
        assertEquals(RelevanceResult::class, rank.returnType.classifier)
    }

    @Test
    fun `RelevanceMechanism declares no member property of its own`() {
        // No PermissionEngine, KnowledgeItemPersistence, or MemoryRetrieval
        // handle is reachable through this interface: it declares exactly
        // one function (asserted above) and no properties at all -- there
        // is no parameter, return path, or property through which a write,
        // a permission decision, or a lifecycle/evidential-state assertion
        // could be expressed.
        assertEquals(0, RelevanceMechanism::class.memberProperties.size)
    }

    @Test
    fun `RelevanceResult exposes no field capable of a write, permission, lifecycle, or evidential-state assertion`() {
        val disallowedSimpleNames = setOf(
            "PermissionDecision",
            "PermissionExplanation",
            "KnowledgeItemStatus",
            "EvidentialState",
            "StalenessDisclosure",
            "KnowledgeId",
            "Boolean",
        )
        val propertyTypeNames = RelevanceResult::class.memberProperties.map {
            (it.returnType.classifier as? KClass<*>)?.simpleName
        }
        propertyTypeNames.forEach { typeName ->
            assertTrue(
                typeName !in disallowedSimpleNames,
                "RelevanceResult must not carry a $typeName-typed field",
            )
        }
    }

    // ---- Required test 2: the token type carries no field derivable
    // into a canonical KnowledgeId. ----

    @Test
    fun `RelevanceCandidateToken carries no field derivable into a canonical KnowledgeId`() {
        val properties = RelevanceCandidateToken::class.declaredMemberProperties

        properties.forEach { property ->
            assertTrue(
                property.returnType.classifier != KnowledgeId::class,
                "RelevanceCandidateToken must not carry a KnowledgeId-typed field",
            )
        }
        assertEquals(1, properties.size, "RelevanceCandidateToken must declare exactly one property")
        assertEquals(String::class, properties.single().returnType.classifier)
    }

    // ---- Additional applicable Phase 4 items ----

    @Test
    fun `RelevanceCandidateToken rejects a blank value`() {
        assertFailsWith<IllegalArgumentException> { RelevanceCandidateToken("") }
        assertFailsWith<IllegalArgumentException> { RelevanceCandidateToken("   ") }
    }

    @Test
    fun `RelevanceCandidateToken accepts a valid value`() {
        assertEquals("candidate-1", RelevanceCandidateToken("candidate-1").value)
    }

    @Test
    fun `RelevanceCandidate rejects blank content`() {
        assertFailsWith<IllegalArgumentException> {
            RelevanceCandidate(RelevanceCandidateToken("t1"), "")
        }
    }

    @Test
    fun `RelevanceCandidate carries no canonical Parker identifier alongside its opaque token`() {
        val properties = RelevanceCandidate::class.declaredMemberProperties.map { it.name }.toSet()
        assertEquals(setOf("token", "content"), properties)
    }

    @Test
    fun `RelevanceRequest exposes only query text and the candidate set`() {
        val properties = RelevanceRequest::class.declaredMemberProperties.map { it.name }.toSet()
        assertEquals(setOf("queryText", "candidates"), properties)
    }

    @Test
    fun `RelevanceRequest rejects blank query text`() {
        assertFailsWith<IllegalArgumentException> {
            RelevanceRequest("", listOf(RelevanceCandidate(RelevanceCandidateToken("t1"), "content")))
        }
    }

    @Test
    fun `RelevanceRequest rejects a repeated candidate token`() {
        val token = RelevanceCandidateToken("dup")
        assertFailsWith<IllegalArgumentException> {
            RelevanceRequest(
                "query",
                listOf(
                    RelevanceCandidate(token, "first"),
                    RelevanceCandidate(token, "second"),
                ),
            )
        }
    }

    @Test
    fun `RelevanceRequest accepts an empty candidate set`() {
        val request = RelevanceRequest("query", emptyList())
        assertTrue(request.candidates.isEmpty())
    }

    @Test
    fun `RelevanceResult exposes only an ordered list of tokens`() {
        val properties = RelevanceResult::class.declaredMemberProperties.map { it.name }.toSet()
        assertEquals(setOf("rankedTokens"), properties)
    }

    @Test
    fun `RelevanceResult represents a successful empty result, distinct from a thrown mechanism failure`() = runTest {
        val result = RelevanceResult(rankedTokens = emptyList())
        assertTrue(result.rankedTokens.isEmpty())

        val mechanism = RelevanceMechanism { throw IllegalStateException("mechanism failure") }
        assertFailsWith<IllegalStateException> {
            mechanism.rank(RelevanceRequest("query", emptyList()))
        }
    }

    @Test
    fun `RelevanceResult preserves the ordering it is constructed with`() {
        val t1 = RelevanceCandidateToken("t1")
        val t2 = RelevanceCandidateToken("t2")
        val t3 = RelevanceCandidateToken("t3")

        val result = RelevanceResult(rankedTokens = listOf(t3, t1, t2))

        assertEquals(listOf(t3, t1, t2), result.rankedTokens)
    }

    @Test
    fun `RelevanceResult may represent a strict subset of the tokens supplied`() {
        val t1 = RelevanceCandidateToken("t1")
        val t2 = RelevanceCandidateToken("t2")
        val request = RelevanceRequest(
            "query",
            listOf(
                RelevanceCandidate(t1, "first"),
                RelevanceCandidate(t2, "second"),
            ),
        )

        val result = RelevanceResult(rankedTokens = listOf(t1))

        assertTrue(result.rankedTokens.size < request.candidates.size)
        assertTrue(request.candidates.map { it.token }.containsAll(result.rankedTokens))
    }

    @Test
    fun `RelevanceMechanism is usable as a fun interface, unimplemented by any concrete provider in this Unit`() = runTest {
        // Confirms the interface is a plain, providerless SAM at this
        // Unit's own properties-level scope: constructing one inline from a
        // lambda is possible (proving the shape compiles and is callable),
        // but this Unit itself supplies no concrete, named implementation
        // anywhere in src/ -- mechanism selection remains Unit 9.7's own
        // later spike and Unit 9.7.3's own adapter.
        val mechanism = RelevanceMechanism { request -> RelevanceResult(request.candidates.map { it.token }) }
        val token = RelevanceCandidateToken("only")
        val request = RelevanceRequest("query", listOf(RelevanceCandidate(token, "content")))

        val result = mechanism.rank(request)

        assertEquals(listOf(token), result.rankedTokens)
    }
}
