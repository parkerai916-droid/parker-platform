package parker.core.interfaces

import kotlin.reflect.KVisibility
import kotlin.reflect.full.declaredFunctions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Evidence Processing (Searchable PDF), Implementation Unit 3 ("Human
 * Review Registry"). Structural scope-discipline tests, mirroring
 * `EvidenceCustodianScopeTest.kt`'s own Kotlin-reflection discipline.
 */
class DerivativeReviewScopeTest {

    @Test
    fun `DerivativeReviewState has exactly four values`() {
        assertEquals(
            setOf("PENDING_REVIEW", "APPROVED", "REJECTED", "NEEDS_CORRECTION"),
            DerivativeReviewState.entries.map { it.name }.toSet(),
        )
        assertEquals(4, DerivativeReviewState.entries.size, "no fifth value, and no caller-defined open classification")
    }

    @Test
    fun `DerivativeReviewRegistry declares exactly two operations, both suspend, both public, both abstract`() {
        val declared = DerivativeReviewRegistry::class.declaredFunctions

        assertEquals(
            setOf("recordReviewState", "currentReviewState"),
            declared.map { it.name }.toSet(),
            "DerivativeReviewRegistry must declare exactly recordReviewState and currentReviewState -- found: ${declared.map { it.name }}",
        )
        assertEquals(2, declared.size, "no operation name may be declared more than once")

        declared.forEach { function ->
            assertEquals(KVisibility.PUBLIC, function.visibility, "${function.name} must be a public operation")
            assertTrue(function.isSuspend, "${function.name} must be a suspend function")
            assertTrue(function.isAbstract, "${function.name} must be abstract -- no default implementation on the interface itself")
        }
    }

    @Test
    fun `recordReviewState takes exactly one DerivativeReviewRecord value parameter and returns Unit`() {
        val function = DerivativeReviewRegistry::class.declaredFunctions.single { it.name == "recordReviewState" }
        val valueParameters = function.parameters.filter { it.kind == kotlin.reflect.KParameter.Kind.VALUE }

        assertEquals(1, valueParameters.size)
        assertEquals(DerivativeReviewRecord::class, valueParameters.single().type.classifier)
        assertEquals(Unit::class, function.returnType.classifier, "recordReviewState returns nothing on success -- invalid transitions throw, never a result type")
    }

    @Test
    fun `currentReviewState takes exactly one EvidenceArtifactId value parameter and returns a nullable DerivativeReviewState`() {
        val function = DerivativeReviewRegistry::class.declaredFunctions.single { it.name == "currentReviewState" }
        val valueParameters = function.parameters.filter { it.kind == kotlin.reflect.KParameter.Kind.VALUE }

        assertEquals(1, valueParameters.size)
        assertEquals(EvidenceArtifactId::class, valueParameters.single().type.classifier)
        assertEquals(DerivativeReviewState::class, function.returnType.classifier)
        assertTrue(function.returnType.isMarkedNullable, "an identifier with no review record at all must be distinguishable from PENDING_REVIEW")
    }
}
