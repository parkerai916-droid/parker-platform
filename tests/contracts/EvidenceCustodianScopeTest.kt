package parker.core.interfaces

import java.lang.reflect.Modifier
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.KVisibility
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.jvm.javaMethod
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import parker.core.runtime.DefaultEvidenceCustodian

/**
 * Evidence Custodian, Implementation Plan Phase 3/4, Units 2-3. Structural
 * scope-discipline tests, continuing the pattern
 * `EvidenceArtifactStorageScopeTest.kt` established for Unit 1. Confirms
 * this Unit introduced exactly the governed acceptance and retrieval
 * boundary described in `EvidenceCustodian.kt`'s own KDoc -- nothing this
 * Unit's own governing instruction explicitly excluded (Evidence
 * Intelligence, search, listing, deletion, provenance/ownership/classification/hashing
 * fields on any of the candidate/accepted/retrieval-result types) exists
 * anywhere in the compiled repository.
 *
 * ## Revision history (Unit 3)
 *
 * This file originally asserted that `EvidenceCustodian` declares "exactly
 * one domain operation" and that no `retrieve` operation exists -- both
 * true statements about Unit 2's own scope at the time they were written.
 * Implementation Plan Phase 4, Unit 3 has since introduced [EvidenceCustodian.retrieve]
 * deliberately and within its own explicitly authorised scope. The first
 * test below is revised to expect exactly *two* domain operations
 * (`accept`, `retrieve`) and to check `retrieve`'s own shape; the second
 * test's exclusion list no longer names `retrieve`, but continues to name
 * every operation still genuinely absent (deletion, search, listing,
 * intelligence-style operations) -- a scope guard that starts asserting
 * something false is not extra safety, it is a stale test that would block
 * correctly-scoped, correctly-authorised work, exactly as this file's own
 * prior revision (Unit 2) already reasoned when it removed its own two
 * now-false assertions inherited from `EvidenceArtifactStorageScopeTest.kt`.
 *
 * ## Why Kotlin reflection ([kotlin.reflect.full.declaredFunctions]), not
 * `java.lang.reflect.Class.declaredMethods`
 *
 * An earlier revision of the first test below used raw
 * `EvidenceCustodian::class.java.declaredMethods` and failed even after
 * excluding synthetic/bridge methods. Direct inspection of the actual
 * compiled class file (`build/classes/kotlin/main/.../EvidenceCustodian.class`,
 * parsed by hand -- no `javap` was available in this environment) showed
 * exactly one declared method, correctly `PUBLIC`/`ABSTRACT` and neither
 * synthetic nor a bridge -- but its JVM-level name was **`accept-fn53kRE`**,
 * not `accept`. This is Kotlin's own, well-documented inline/value-class
 * parameter name mangling: because `accept`'s first parameter
 * ([PrincipalId]) is a `@JvmInline value class`, the compiler appends a
 * hash suffix to the JVM method name to avoid an accidental signature clash
 * with an overload taking the value class's own underlying type directly.
 * The synthetic/bridge filtering added in that revision was therefore
 * chasing the wrong artefact -- there was never a second method, only a
 * correctly-mangled name for the one real method.
 *
 * `java.lang.reflect.Method.getName()` returns that mangled JVM name;
 * Kotlin reflection ([kotlin.reflect.full.declaredFunctions],
 * [kotlin.reflect.KFunction.name]) resolves the name Kotlin's own compiler
 * metadata records for the declaration, which is the un-mangled name --
 * exactly the level this architectural property is actually about. This
 * also matches this repository's own existing precedent for verifying
 * interface shape (`InterfaceContractShapeTest.kt`, which uses
 * `KClass::functions`/`KFunction::isSuspend`/`KParameter` throughout,
 * never raw `java.lang.reflect`). [retrieve]'s own shape assertion below
 * follows the identical, already-corrected discipline from the first line.
 */
class EvidenceCustodianScopeTest {

    @Test
    fun `EvidenceCustodian declares exactly two domain operations -- accept and retrieve`() {
        val declared = EvidenceCustodian::class.declaredFunctions

        assertEquals(
            setOf("accept", "retrieve"),
            declared.map { it.name }.toSet(),
            "EvidenceCustodian must declare exactly accept and retrieve -- found: ${declared.map { it.name }}",
        )
        assertEquals(2, declared.size, "no domain operation name may be declared more than once")
    }

    @Test
    fun `accept has the expected public, suspend, abstract shape`() {
        val accept = EvidenceCustodian::class.declaredFunctions.single { it.name == "accept" }

        assertEquals(KVisibility.PUBLIC, accept.visibility, "accept must be a public operation")
        assertTrue(accept.isSuspend, "accept must be a suspend function -- Permission Engine evaluation is async")
        assertAbstract(accept)

        val valueParameters = accept.parameters.filter { it.kind == KParameter.Kind.VALUE }
        assertEquals(2, valueParameters.size, "accept must take exactly two value parameters")
        assertEquals(
            PrincipalId::class,
            valueParameters[0].type.classifier,
            "accept's first parameter must be the requesting principal",
        )
        assertEquals(
            CandidateEvidenceArtifact::class,
            valueParameters[1].type.classifier,
            "accept's second parameter must be the candidate evidence artifact",
        )
        assertEquals(
            EvidenceAcceptanceResult::class,
            accept.returnType.classifier,
            "accept must return the sealed EvidenceAcceptanceResult",
        )
    }

    @Test
    fun `retrieve has the expected public, suspend, abstract shape`() {
        val retrieve = EvidenceCustodian::class.declaredFunctions.single { it.name == "retrieve" }

        assertEquals(KVisibility.PUBLIC, retrieve.visibility, "retrieve must be a public operation")
        assertTrue(retrieve.isSuspend, "retrieve must be a suspend function -- Permission Engine evaluation is async")
        assertAbstract(retrieve)

        val valueParameters = retrieve.parameters.filter { it.kind == KParameter.Kind.VALUE }
        assertEquals(2, valueParameters.size, "retrieve must take exactly two value parameters")
        assertEquals(
            PrincipalId::class,
            valueParameters[0].type.classifier,
            "retrieve's first parameter must be the requesting principal",
        )
        assertEquals(
            EvidenceArtifactId::class,
            valueParameters[1].type.classifier,
            "retrieve's second parameter must be the target artefact's identifier",
        )
        assertEquals(
            EvidenceRetrievalResult::class,
            retrieve.returnType.classifier,
            "retrieve must return the sealed EvidenceRetrievalResult",
        )
    }

    /** Shared by both shape tests above -- see this file's own "Why Kotlin reflection" KDoc. */
    private fun assertAbstract(function: KFunction<*>) {
        val javaMethod = function.javaMethod
        assertTrue(
            javaMethod == null || Modifier.isAbstract(javaMethod.modifiers),
            "${function.name} must remain an abstract interface operation with no default implementation",
        )
    }

    @Test
    fun `no deletion, search, listing, or intelligence operation exists on EvidenceCustodian`() {
        val declaredNames = EvidenceCustodian::class.declaredFunctions.map { it.name }
        listOf(
            "delete", "remove", "erase",
            "search", "list", "query", "find",
            "analyse", "analyze", "classify", "hash", "extract", "ocr", "summarise", "summarize",
        ).forEach { excludedName ->
            assertTrue(
                excludedName !in declaredNames,
                "EvidenceCustodian must not declare a '$excludedName' operation -- deletion (Phase 7) is a " +
                    "later, unimplemented phase, and search/listing/analysis are permanently out of this " +
                    "Unit's own scope",
            )
        }
    }

    @Test
    fun `DefaultEvidenceCustodian holds no dependency on EvidenceIntelligence`() {
        // Evidence Intelligence is now authorised and implemented (Unit 5 --
        // docs/reviews/EVIDENCE_INTELLIGENCE_UNIT_5_PLANNING_AND_BOUNDARY_REVIEW.md,
        // Verdict A), so asserting the type does not exist anywhere is now false --
        // exactly the "stale test that would block correctly-scoped, correctly-authorised
        // work" this file's own revision history (above) already warns against. The
        // real, still-true invariant this test guards is the dependency direction:
        // Evidence Custodian never depends on, references, or is constructed with
        // Evidence Intelligence -- checked structurally, not by the type's mere absence.
        val constructorParameterTypeNames = DefaultEvidenceCustodian::class.java.declaredConstructors
            .flatMap { it.parameterTypes.toList() }
            .map { it.simpleName }

        assertTrue(constructorParameterTypeNames.none { it == "EvidenceIntelligence" })
    }

    @Test
    fun `CandidateEvidenceArtifact carries no owner, classification, or provenance field`() {
        val fieldNames = CandidateEvidenceArtifact::class.java.declaredFields
            .filter { !it.isSynthetic }
            .map { it.name.lowercase() }

        listOf("owner", "artifacttype", "provenance", "hash", "checksum").forEach { excluded ->
            assertTrue(
                fieldNames.none { it.contains(excluded) },
                "CandidateEvidenceArtifact must not carry a field related to '$excluded'",
            )
        }
    }

    @Test
    fun `AcceptedEvidenceArtifact carries no owner, classification, or provenance field`() {
        val fieldNames = AcceptedEvidenceArtifact::class.java.declaredFields
            .filter { !it.isSynthetic }
            .map { it.name.lowercase() }

        listOf("owner", "artifacttype", "provenance", "hash", "checksum", "location", "path").forEach { excluded ->
            assertTrue(
                fieldNames.none { it.contains(excluded) },
                "AcceptedEvidenceArtifact must not carry a field related to '$excluded'",
            )
        }
    }

    @Test
    fun `EvidenceRetrievalResult's Found, NotFound, and Rejected carry no owner, classification, or provenance field`() {
        val excluded = listOf("owner", "artifacttype", "provenance", "hash", "checksum", "location", "path")
        val typesToCheck = listOf(
            EvidenceRetrievalResult.Found::class.java,
            EvidenceRetrievalResult.NotFound::class.java,
            EvidenceRetrievalResult.Rejected::class.java,
        )

        typesToCheck.forEach { type ->
            val fieldNames = type.declaredFields.filter { !it.isSynthetic }.map { it.name.lowercase() }
            excluded.forEach { excludedName ->
                assertTrue(
                    fieldNames.none { it.contains(excludedName) },
                    "${type.simpleName} must not carry a field related to '$excludedName'",
                )
            }
        }
    }

    // --- Implementation Plan Phase 7 ("Deletion workflow") -- Boundary Clarification Section 3 ---

    @Test
    fun `EvidenceCustodian still declares exactly accept and retrieve -- deletion was not added to it`() {
        val declared = EvidenceCustodian::class.declaredFunctions

        assertEquals(
            setOf("accept", "retrieve"),
            declared.map { it.name }.toSet(),
            "EvidenceCustodian must remain exactly accept and retrieve -- deletion (Phase 7) is a " +
                "structurally separate capability (OwnerEvidenceDeletionAuthority), never added to " +
                "this interface -- found: ${declared.map { it.name }}",
        )
    }

    @Test
    fun `OwnerEvidenceDeletionAuthority and EvidenceCustodian are unrelated types`() {
        assertFalse(
            EvidenceCustodian::class.java.isAssignableFrom(OwnerEvidenceDeletionAuthority::class.java),
            "OwnerEvidenceDeletionAuthority must not be a subtype of EvidenceCustodian",
        )
        assertFalse(
            OwnerEvidenceDeletionAuthority::class.java.isAssignableFrom(EvidenceCustodian::class.java),
            "EvidenceCustodian must not be a subtype of OwnerEvidenceDeletionAuthority",
        )
    }

    @Test
    fun `DefaultEvidenceCustodian does not implement OwnerEvidenceDeletionAuthority`() {
        assertFalse(
            OwnerEvidenceDeletionAuthority::class.java.isAssignableFrom(DefaultEvidenceCustodian::class.java),
            "DefaultEvidenceCustodian must never also implement OwnerEvidenceDeletionAuthority -- " +
                "deletion is a structurally separate capability, implemented by a separate class " +
                "(Boundary Clarification Section 3)",
        )
    }

    @Test
    fun `OwnerEvidenceDeletionAuthority declares exactly one operation -- deleteAsOwner`() {
        val declared = OwnerEvidenceDeletionAuthority::class.declaredFunctions

        assertEquals(setOf("deleteAsOwner"), declared.map { it.name }.toSet())
        assertEquals(1, declared.size, "no domain operation name may be declared more than once")
    }

    @Test
    fun `deleteAsOwner has the expected public, suspend, abstract shape with no reason parameter`() {
        val deleteAsOwner = OwnerEvidenceDeletionAuthority::class.declaredFunctions.single { it.name == "deleteAsOwner" }

        assertEquals(KVisibility.PUBLIC, deleteAsOwner.visibility, "deleteAsOwner must be a public operation")
        assertTrue(deleteAsOwner.isSuspend, "deleteAsOwner must be a suspend function -- Permission Engine evaluation is async")
        assertAbstract(deleteAsOwner)

        val valueParameters = deleteAsOwner.parameters.filter { it.kind == KParameter.Kind.VALUE }
        assertEquals(
            2,
            valueParameters.size,
            "deleteAsOwner must take exactly two value parameters -- no reason/justification " +
                "parameter (Boundary Clarification Section 4)",
        )
        assertEquals(
            PrincipalId::class,
            valueParameters[0].type.classifier,
            "deleteAsOwner's first parameter must be the requesting principal",
        )
        assertEquals(
            EvidenceArtifactId::class,
            valueParameters[1].type.classifier,
            "deleteAsOwner's second parameter must be the target artefact's identifier",
        )
        assertEquals(
            EvidenceDeletionResult::class,
            deleteAsOwner.returnType.classifier,
            "deleteAsOwner must return the sealed EvidenceDeletionResult",
        )
    }

    @Test
    fun `EvidenceDeletionAuditStage declares exactly AUTHORISED and COMPLETED -- no other value`() {
        assertEquals(
            setOf("AUTHORISED", "COMPLETED"),
            EvidenceDeletionAuditStage.values().map { it.name }.toSet(),
        )
        assertEquals(2, EvidenceDeletionAuditStage.values().size, "no FAILED, REQUESTED, or NOT_FOUND stage may exist")
    }
}
