package parker.core.interfaces

import java.lang.reflect.Modifier
import kotlin.reflect.KParameter
import kotlin.reflect.KVisibility
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.jvm.javaMethod
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Evidence Custodian, Implementation Plan Phase 3, Unit 2. Structural
 * scope-discipline tests, continuing the pattern
 * `EvidenceArtifactStorageScopeTest.kt` established for Unit 1. Confirms
 * this Unit introduced exactly the governed acceptance boundary described
 * in `EvidenceCustodian.kt`'s own KDoc -- nothing this Unit's own governing
 * instruction explicitly excluded (Evidence Intelligence, retrieval,
 * deletion, provenance/ownership/classification fields on either
 * candidate or accepted types) exists anywhere in the compiled repository.
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
 * The synthetic/bridge filtering added in the previous revision was
 * therefore chasing the wrong artefact -- there was never a second method,
 * only a correctly-mangled name for the one real method.
 *
 * `java.lang.reflect.Method.getName()` returns that mangled JVM name;
 * Kotlin reflection ([kotlin.reflect.full.declaredFunctions],
 * [kotlin.reflect.KFunction.name]) resolves the name Kotlin's own compiler
 * metadata records for the declaration, which is the un-mangled `accept` --
 * exactly the level this architectural property is actually about. This
 * also matches this repository's own existing precedent for verifying
 * interface shape (`InterfaceContractShapeTest.kt`, which uses
 * `KClass::functions`/`KFunction::isSuspend`/`KParameter` throughout,
 * never raw `java.lang.reflect`).
 */
class EvidenceCustodianScopeTest {

    @Test
    fun `EvidenceCustodian declares exactly one domain operation -- accept`() {
        val declared = EvidenceCustodian::class.declaredFunctions

        assertEquals(
            1,
            declared.size,
            "EvidenceCustodian must declare exactly one domain operation -- found: ${declared.map { it.name }}",
        )

        val accept = declared.single()
        assertEquals("accept", accept.name)
        assertEquals(KVisibility.PUBLIC, accept.visibility, "accept must be a public operation")
        assertTrue(accept.isSuspend, "accept must be a suspend function -- Permission Engine evaluation is async")

        val abstractCheck = accept.javaMethod
        assertTrue(
            abstractCheck == null || Modifier.isAbstract(abstractCheck.modifiers),
            "accept must remain an abstract interface operation with no default implementation",
        )

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
    fun `no retrieval or deletion operation exists on EvidenceCustodian`() {
        val declaredNames = EvidenceCustodian::class.declaredFunctions.map { it.name }
        listOf("retrieve", "read", "get", "delete", "remove").forEach { excludedName ->
            assertTrue(
                excludedName !in declaredNames,
                "EvidenceCustodian must not declare a '$excludedName' operation -- retrieval (Phase 4) and " +
                    "deletion (Phase 7) are later, unimplemented phases",
            )
        }
    }

    @Test
    fun `no EvidenceIntelligence type exists anywhere in the repository`() {
        assertFailsWith<ClassNotFoundException> { Class.forName("parker.core.interfaces.EvidenceIntelligence") }
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
}
