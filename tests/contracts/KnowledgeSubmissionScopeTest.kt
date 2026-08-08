package parker.core.interfaces

import kotlin.reflect.KParameter
import kotlin.reflect.KVisibility
import kotlin.reflect.full.declaredFunctions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Programme 3, Knowledge Memory, Implementation Unit 8 (Knowledge
 * Submission). Structural scope-discipline tests for
 * [KnowledgeSubmission] and [KnowledgeSubmissionDisposition], continuing
 * the pattern `EvidenceCustodianScopeTest.kt` established. Confirms this
 * Unit introduced exactly the single, governed submission operation
 * `docs/governance/PROGRAMME_3_UNIT_8_SCOPE_LOCK_CLARIFICATION.md`
 * ("the Unit 8 Clarification") authorises -- no storage mechanic, no
 * lifecycle operation, no retrieval capability, and no evaluation policy
 * exists anywhere on the compiled public contract.
 *
 * Uses Kotlin reflection ([kotlin.reflect.full.declaredFunctions]), never
 * raw `java.lang.reflect.Class.declaredMethods`, for the same reason
 * `EvidenceCustodianScopeTest.kt`'s own KDoc already discloses in full:
 * [KnowledgeSubmission.submit]'s first value parameter is [PrincipalId],
 * a `@JvmInline value class`, whose JVM method name Kotlin mangles with a
 * hash suffix -- Kotlin reflection resolves the un-mangled declaration
 * name this architectural property is actually about.
 */
class KnowledgeSubmissionScopeTest {

    @Test
    fun `KnowledgeSubmission declares exactly one domain operation -- submit`() {
        val declared = KnowledgeSubmission::class.declaredFunctions

        assertEquals(
            setOf("submit"),
            declared.map { it.name }.toSet(),
            "KnowledgeSubmission must declare exactly submit -- found: ${declared.map { it.name }}",
        )
        assertEquals(1, declared.size, "no domain operation name may be declared more than once")
    }

    @Test
    fun `submit has the expected public, suspend shape`() {
        val submit = KnowledgeSubmission::class.declaredFunctions.single { it.name == "submit" }

        assertEquals(KVisibility.PUBLIC, submit.visibility, "submit must be a public operation")
        assertTrue(submit.isSuspend, "submit must be a suspend function -- Permission Engine evaluation is async")

        val valueParameters = submit.parameters.filter { it.kind == KParameter.Kind.VALUE }
        assertEquals(2, valueParameters.size, "submit must take exactly two value parameters")
        assertEquals(
            PrincipalId::class,
            valueParameters[0].type.classifier,
            "submit's first parameter must be the requesting principal, supplied explicitly",
        )
        assertEquals(
            KnowledgeCandidate::class,
            valueParameters[1].type.classifier,
            "submit's second parameter must be the constitutional KnowledgeCandidate, unchanged",
        )
        assertEquals(
            KnowledgeSubmissionDisposition::class,
            submit.returnType.classifier,
            "submit must return the sealed KnowledgeSubmissionDisposition",
        )
    }

    @Test
    fun `no storage, lifecycle, retrieval, or evaluation-policy operation exists on KnowledgeSubmission`() {
        val declaredNames = KnowledgeSubmission::class.declaredFunctions.map { it.name }
        listOf(
            // storage mechanics
            "store", "persist", "find", "read", "write",
            // lifecycle operations (Unit 7's own, closed responsibility)
            "revise", "retire", "restore",
            // retrieval (Unit 9, not begun)
            "retrieve", "query", "recall",
            // evaluation policy (Unit 6's own, closed responsibility)
            "evaluate", "promote", "reject",
            // legacy path
            "remember", "forget",
        ).forEach { excludedName ->
            assertTrue(
                excludedName !in declaredNames,
                "KnowledgeSubmission must not declare '$excludedName' -- found: $declaredNames",
            )
        }
    }

    // --- KnowledgeSubmissionDisposition ---

    @Test
    fun `a Declined with a blank basis is rejected at construction`() {
        assertFailsWith<IllegalArgumentException> { KnowledgeSubmissionDisposition.Declined("") }
        assertFailsWith<IllegalArgumentException> { KnowledgeSubmissionDisposition.Declined("   ") }
    }

    @Test
    fun `a NotAuthorised with a blank reason is rejected at construction`() {
        assertFailsWith<IllegalArgumentException> { KnowledgeSubmissionDisposition.NotAuthorised("") }
        assertFailsWith<IllegalArgumentException> { KnowledgeSubmissionDisposition.NotAuthorised("   ") }
    }

    @Test
    fun `Promoted carries exactly an item and a promotion, no additional field`() {
        val fieldNames = KnowledgeSubmissionDisposition.Promoted::class.java.declaredFields
            .filter { !it.isSynthetic }
            .map { it.name }
            .toSet()

        assertEquals(setOf("item", "promotion"), fieldNames)
    }

    @Test
    fun `KnowledgeCandidateEvaluator carries accountable Principal separately from candidate`() {
        // Guards against evaluator responsibility expansion: Evaluation B belongs exclusively
        // to KnowledgeSubmission (Unit 8 Clarification Section 5), never to the evaluator.
        val declared = KnowledgeCandidateEvaluator::class.declaredFunctions

        assertEquals(setOf("evaluate"), declared.map { it.name }.toSet())
        assertEquals(1, declared.size)

        val evaluate = declared.single()
        assertTrue(!evaluate.isSuspend, "KnowledgeCandidateEvaluator.evaluate must remain non-suspend")
        val valueParameters = evaluate.parameters.filter { it.kind == KParameter.Kind.VALUE }
        assertEquals(2, valueParameters.size)
        assertEquals(PrincipalId::class, valueParameters[0].type.classifier)
        assertEquals(KnowledgeCandidate::class, valueParameters[1].type.classifier)
        assertEquals(KnowledgeCandidateEvaluation::class, evaluate.returnType.classifier)
    }
}
