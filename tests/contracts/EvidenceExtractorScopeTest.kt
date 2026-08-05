package parker.core.interfaces

import java.io.File
import kotlin.reflect.KClass
import kotlin.reflect.KVisibility
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Evidence Processing (Searchable PDF), Implementation Unit 1 ("Extraction
 * Contracts"). Structural scope-discipline tests, mirroring
 * `EvidenceCustodianScopeTest.kt`'s own Kotlin-reflection discipline
 * ([KClass.declaredFunctions], never raw `java.lang.reflect` -- that
 * file's own KDoc documents why: Kotlin's value-class parameter name
 * mangling makes JVM-level method names unreliable for this exact kind of
 * check).
 *
 * The Tika-leakage assertion below is written before Unit 2 adds the Tika
 * dependency and passes vacuously until then (no such type could appear,
 * since Tika is not yet on the classpath at all) -- Unit 2 re-runs it as
 * its own first verification step, and it must still pass now that Tika
 * genuinely is on the classpath.
 */
class EvidenceExtractorScopeTest {

    @Test
    fun `EvidenceExtractor declares exactly one operation, public, suspend, abstract, taking a ByteArray`() {
        val declared = EvidenceExtractor::class.declaredFunctions

        assertEquals(1, declared.size, "EvidenceExtractor must declare exactly one operation -- found: ${declared.map { it.name }}")
        val extract = declared.single()
        assertEquals("extract", extract.name)
        assertEquals(KVisibility.PUBLIC, extract.visibility, "extract must be a public operation")
        assertTrue(extract.isSuspend, "extract must be a suspend function")
        assertTrue(extract.isAbstract, "extract must be abstract -- no default implementation on the interface itself")

        val valueParameters = extract.parameters.filter { it.kind == kotlin.reflect.KParameter.Kind.VALUE }
        assertEquals(1, valueParameters.size, "extract must take exactly one value parameter")
        assertEquals(ByteArray::class, valueParameters.single().type.classifier)
    }

    @Test
    fun `EvidenceExtractor declares no constructor, property, or dependency of its own`() {
        assertEquals(0, EvidenceExtractor::class.declaredMemberProperties.size)
    }

    @Test
    fun `ExtractionOutcome has exactly four subclasses`() {
        val subclasses = ExtractionOutcome::class.sealedSubclasses

        assertEquals(
            setOf("Extracted", "RequiresOcr", "Unsupported", "Malformed"),
            subclasses.map { it.simpleName }.toSet(),
            "ExtractionOutcome must have exactly Extracted, RequiresOcr, Unsupported, and Malformed -- found: ${subclasses.map { it.simpleName }}",
        )
        assertEquals(4, subclasses.size, "no ExtractionOutcome variant name may be declared more than once")
    }

    @Test
    fun `ExtractionResult and ExtractionIdentity carry no digest-named or confidence-named field`() {
        val forbiddenNameFragments = listOf("digest", "hash", "confidence")

        val resultFieldNames = ExtractionResult::class.declaredMemberProperties.map { it.name.lowercase() }
        val identityFieldNames = ExtractionIdentity::class.declaredMemberProperties.map { it.name.lowercase() }

        (resultFieldNames + identityFieldNames).forEach { fieldName ->
            forbiddenNameFragments.forEach { forbidden ->
                assertFalse(
                    fieldName.contains(forbidden),
                    "ExtractionResult/ExtractionIdentity must carry no digest or confidence field -- " +
                        "found field name '$fieldName' containing '$forbidden'. Digests are the coordinator's own " +
                        "responsibility (Unit 4A); confidence is deferred to Provenance.confidence, left null.",
                )
            }
        }
    }

    @Test
    fun `ExtractionIdentity carries exactly parserIdentity, configurationProfile, and normalisationProfile`() {
        val fieldNames = ExtractionIdentity::class.declaredMemberProperties.map { it.name }.toSet()

        assertEquals(setOf("parserIdentity", "configurationProfile", "normalisationProfile"), fieldNames)
    }

    @Test
    fun `no org-apache-tika type is reachable from any public EvidenceExtractor, ExtractionResult, or ExtractionOutcome signature`() {
        val typesToCheck: List<KClass<*>> = listOf(
            EvidenceExtractor::class,
            ExtractionResult::class,
            ExtractionIdentity::class,
            EmbeddedResourceObservation::class,
            ExtractionOutcome::class,
        ) + ExtractionOutcome::class.sealedSubclasses

        typesToCheck.forEach { type ->
            val propertyTypeNames = type.declaredMemberProperties.flatMap { property ->
                listOf(property.returnType.classifier?.let { (it as? KClass<*>)?.qualifiedName }) +
                    property.returnType.arguments.mapNotNull { arg -> (arg.type?.classifier as? KClass<*>)?.qualifiedName }
            }
            val functionTypeNames = type.declaredFunctions.flatMap { function ->
                function.parameters.mapNotNull { (it.type.classifier as? KClass<*>)?.qualifiedName } +
                    listOfNotNull((function.returnType.classifier as? KClass<*>)?.qualifiedName)
            }
            val constructorTypeNames = type.primaryConstructor?.parameters?.mapNotNull {
                (it.type.classifier as? KClass<*>)?.qualifiedName
            } ?: emptyList()

            (propertyTypeNames + functionTypeNames + constructorTypeNames)
                .filterNotNull()
                .forEach { qualifiedName ->
                    assertFalse(
                        qualifiedName.startsWith("org.apache.tika"),
                        "${type.simpleName} must not expose any org.apache.tika type -- found '$qualifiedName'. " +
                            "Only TikaEvidenceExtractor.kt is permitted to reference Apache Tika types.",
                    )
                }
        }
    }

    @Test
    fun `no file in src outside TikaEvidenceExtractor-kt contains the string org-apache-tika`() {
        val srcRoot = File("src")
        check(srcRoot.exists()) { "src/ directory not found from working directory ${File(".").absolutePath}" }

        val offendingFiles = srcRoot.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filter { it.name != "TikaEvidenceExtractor.kt" }
            .filter { it.readText().contains("org.apache.tika") }
            .map { it.path }
            .toList()

        assertTrue(
            offendingFiles.isEmpty(),
            "Only TikaEvidenceExtractor.kt may reference org.apache.tika -- found the string in: $offendingFiles",
        )
    }
}
