package parker.core.interfaces

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
 * OCR Mechanism, Implementation Unit 2 ("Provider Adapter Abstraction").
 * Structural scope-discipline tests only, mirroring
 * `OcrMechanismScopeTest.kt`'s own Kotlin-reflection discipline
 * ([KClass.declaredFunctions], never raw `java.lang.reflect`). Covers
 * contract shape, provider neutrality, and prohibited dependencies only
 * -- no execution-behaviour, orchestration, or runtime-composition test
 * belongs in this Unit (Implementation Plan Unit 2's own scope; no
 * concrete adapter exists yet for any such test to exercise).
 */
class OcrProviderAdapterScopeTest {

    // -- Contract shape ----------------------------------------------------

    @Test
    fun `OcrProviderAdapter declares exactly one operation, public, suspend, abstract, taking an OcrRecognitionRequest and returning an OcrRecognitionOutcome`() {
        val declared = OcrProviderAdapter::class.declaredFunctions

        assertEquals(1, declared.size, "OcrProviderAdapter must declare exactly one operation -- found: ${declared.map { it.name }}")
        val recognise = declared.single()
        assertEquals("recognise", recognise.name)
        assertEquals(KVisibility.PUBLIC, recognise.visibility, "recognise must be a public operation")
        assertTrue(recognise.isSuspend, "recognise must be a suspend function")
        assertTrue(recognise.isAbstract, "recognise must be abstract -- no default implementation on the interface itself")

        val valueParameters = recognise.parameters.filter { it.kind == kotlin.reflect.KParameter.Kind.VALUE }
        assertEquals(1, valueParameters.size, "recognise must take exactly one value parameter")
        assertEquals(OcrRecognitionRequest::class, valueParameters.single().type.classifier)
        assertEquals(OcrRecognitionOutcome::class, recognise.returnType.classifier)
    }

    @Test
    fun `OcrProviderAdapter declares no constructor, property, or dependency of its own`() {
        assertEquals(0, OcrProviderAdapter::class.declaredMemberProperties.size)
        assertEquals(null, OcrProviderAdapter::class.primaryConstructor?.parameters?.takeIf { it.isNotEmpty() })
    }

    @Test
    fun `OcrProviderAdapter introduces no new public type -- it reuses exactly OcrRecognitionRequest and OcrRecognitionOutcome`() {
        val declared = OcrProviderAdapter::class.declaredFunctions.single()

        val referencedTypeNames = declared.parameters
            .filter { it.kind == kotlin.reflect.KParameter.Kind.VALUE }
            .mapNotNull { (it.type.classifier as? KClass<*>)?.qualifiedName } +
            listOfNotNull((declared.returnType.classifier as? KClass<*>)?.qualifiedName)

        assertEquals(
            setOf(OcrRecognitionRequest::class.qualifiedName, OcrRecognitionOutcome::class.qualifiedName),
            referencedTypeNames.toSet(),
            "OcrProviderAdapter must reference exactly OcrRecognitionRequest and OcrRecognitionOutcome -- " +
                "Implementation Plan Unit 2 fixes its own dependencies, inputs, and outputs as 'Unit 1's own " +
                "shapes only,' never a new or provider-adjacent type -- found: $referencedTypeNames",
        )
    }

    @Test
    fun `OcrProviderAdapter has no reference back to OcrMechanism itself`() {
        val declared = OcrProviderAdapter::class.declaredFunctions.single()
        val allClassifiers = declared.parameters.mapNotNull { it.type.classifier as? KClass<*> } +
            listOfNotNull(declared.returnType.classifier as? KClass<*>)

        assertFalse(
            allClassifiers.any { it.qualifiedName == OcrMechanism::class.qualifiedName },
            "OcrProviderAdapter must hold no reference back to OcrMechanism -- the two are separate abstractions " +
                "(the public capability contract and the provider plug point), and neither depends on the other.",
        )
    }

    // -- Provider neutrality (Scope Lock Section 14) ------------------------

    @Test
    fun `no field, function, or type name on OcrProviderAdapter names a concrete OCR provider`() {
        val forbiddenProviderFragments = listOf("tesseract", "ocrmypdf", "paddleocr", "easyocr", "docling")

        val namesToCheck = listOf(OcrProviderAdapter::class.simpleName.orEmpty().lowercase()) +
            OcrProviderAdapter::class.declaredFunctions.map { it.name.lowercase() } +
            OcrProviderAdapter::class.declaredMemberProperties.map { it.name.lowercase() }

        namesToCheck.forEach { name ->
            forbiddenProviderFragments.forEach { forbidden ->
                assertFalse(
                    name.contains(forbidden),
                    "No name on OcrProviderAdapter may reference a concrete provider -- found '$name' containing " +
                        "'$forbidden' (Scope Lock Section 14: provider neutrality).",
                )
            }
        }
    }

    @Test
    fun `no file in src imports a concrete OCR provider package -- a prose non-example in a KDoc comment is not a leak`() {
        val srcRoot = java.io.File("src")
        check(srcRoot.exists()) { "src/ directory not found from working directory ${java.io.File(".").absolutePath}" }
        val forbiddenProviderNames = listOf("Tesseract", "OCRmyPDF", "PaddleOCR", "EasyOCR")

        val offendingFiles = srcRoot.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filter { file ->
                file.readLines().any { line ->
                    line.trimStart().startsWith("import ") && forbiddenProviderNames.any { line.contains(it) }
                }
            }
            .map { it.path }
            .toList()

        assertTrue(
            offendingFiles.isEmpty(),
            "No file in src/ may import a concrete OCR provider package -- concrete adapter implementation remains " +
                "explicitly deferred future work (Scope Lock Section 18, item 6) -- found a provider-package " +
                "import in: $offendingFiles",
        )
    }

    @Test
    fun `no concrete class implementing OcrProviderAdapter exists anywhere in src`() {
        val srcRoot = java.io.File("src")
        check(srcRoot.exists()) { "src/ directory not found from working directory ${java.io.File(".").absolutePath}" }

        val offendingFiles = srcRoot.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filter { file -> file.readText().contains(": OcrProviderAdapter") || file.readText().contains(", OcrProviderAdapter") }
            .map { it.path }
            .toList()

        assertTrue(
            offendingFiles.isEmpty(),
            "No file in src/ may implement OcrProviderAdapter -- a concrete adapter is explicitly deferred future " +
                "work (Scope Lock Section 18, item 6; Implementation Plan Section 16, item 6), never this Unit's " +
                "own responsibility -- found a candidate implementation in: $offendingFiles",
        )
    }

    // -- Prohibited dependencies (Scope Lock Section 13) --------------------

    @Test
    fun `no type reachable from OcrProviderAdapter references EvidenceCustodian, MemoryCore, KnowledgeSubmission, PermissionEngine, or EvidenceIntelligence`() {
        val excludedQualifiedNames = setOf(
            EvidenceCustodian::class.qualifiedName,
            MemoryCore::class.qualifiedName,
            MemoryRetrieval::class.qualifiedName,
            KnowledgeSubmission::class.qualifiedName,
            PermissionEngine::class.qualifiedName,
            EvidenceIntelligence::class.qualifiedName,
            OwnerEvidenceDeletionAuthority::class.qualifiedName,
        )

        val declared = OcrProviderAdapter::class.declaredFunctions.single()
        val referencedTypeNames = declared.parameters.mapNotNull { (it.type.classifier as? KClass<*>)?.qualifiedName } +
            listOfNotNull((declared.returnType.classifier as? KClass<*>)?.qualifiedName)

        referencedTypeNames.forEach { qualifiedName ->
            assertFalse(
                excludedQualifiedNames.contains(qualifiedName),
                "OcrProviderAdapter must not reference '$qualifiedName' -- Scope Lock Section 13 excludes " +
                    "EvidenceCustodian, MemoryCore, KnowledgeSubmission, the Permission Engine, EvidenceIntelligence, " +
                    "and OwnerEvidenceDeletionAuthority from the OCR mechanism's own dependency graph, at any depth.",
            )
        }
    }

    @Test
    fun `no file in src outside existing subsystems references ParkerRuntime or any composition-root type from an OCR-mechanism file`() {
        val srcRoot = java.io.File("src")
        check(srcRoot.exists()) { "src/ directory not found from working directory ${java.io.File(".").absolutePath}" }

        val ocrFiles = listOf("OcrMechanism.kt", "OcrProviderAdapter.kt")
        val offendingFiles = srcRoot.walkTopDown()
            .filter { it.isFile && it.extension == "kt" && it.name in ocrFiles }
            .filter { file -> file.readText().contains("ParkerRuntime") }
            .map { it.path }
            .toList()

        assertTrue(
            offendingFiles.isEmpty(),
            "No OCR mechanism file may reference ParkerRuntime or any composition-root type -- runtime composition " +
                "remains Implementation Plan Unit 12, structurally blocked -- found a reference in: $offendingFiles",
        )
    }
}
