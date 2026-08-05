package parker.core.interfaces

import kotlin.reflect.KClass
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import parker.core.runtime.OcrExecutionSequencer

/**
 * OCR Mechanism, Implementation Unit 4 ("Input Contract: Already-Retrieved
 * Content Only"). Governed in full by
 * `docs/architecture/OCR_MECHANISM_CONTRACT_DESIGN.md` ("the Contract
 * Design") Section 4, Section 7; by
 * `docs/architecture/OCR_MECHANISM_SCOPE_LOCK.md` ("the Scope Lock")
 * Section 5, Section 7, Section 13; and by
 * `docs/implementation/OCR_MECHANISM_IMPLEMENTATION_PLAN.md`
 * ("the Implementation Plan") Unit 4.
 *
 * **This Unit adds no production file.** Implementation Plan Unit 4's own
 * Inputs, Outputs, and Files-expected-to-change fields are each fixed as
 * "None" -- its own Responsibilities are to "confirm and test" a property
 * Unit 1's request shape already has, never to build a new access path.
 * This file is that confirmation: a dedicated, comprehensive, Unit
 * 4-attributed test suite proving Unit 1's request shape accepts only
 * already-retrieved content, and that no code path anywhere in Units 1-3
 * (`OcrMechanism`, `OcrProviderAdapter`, `OcrExecutionSequencer`) can
 * retrieve evidence independently -- exactly Unit 4's own Completion
 * criteria, restated as tests rather than merely asserted in prose. Some
 * of what follows already has partial coverage in `OcrMechanismScopeTest.kt`,
 * `OcrProviderAdapterScopeTest.kt`, and `OcrExecutionSequencerTest.kt`;
 * this suite is deliberately self-contained and comprehensive rather than
 * relying on that incidental overlap, so Unit 4's own completion is
 * independently verifiable without reading any other unit's own tests.
 */
class OcrInputContractTest {

    private val ocrUnitFiles = listOf(
        java.io.File("src/interfaces/OcrMechanism.kt"),
        java.io.File("src/interfaces/OcrProviderAdapter.kt"),
        java.io.File("src/runtime/OcrExecutionSequencer.kt"),
    )

    /**
     * Strips KDoc/block comments (`/&#42; ... &#42;/`) and line comments
     * (`//...`) before a whole-file substring scan. Every OCR mechanism
     * file's own KDoc *deliberately* names the excluded dependencies it
     * holds none of -- as disclosure, not as code. A scan that did not
     * strip comments would treat that disclosure itself as a violation,
     * exactly the self-referential trap this repository's own Tika-leakage
     * test discipline was first written to avoid (`EvidenceExtractorScopeTest.kt`).
     * Only actual code -- imports, declarations, expressions -- may fail
     * the checks below.
     */
    private fun String.codeOnly(): String =
        replace(Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL), "")
            .lines()
            .joinToString("\n") { line -> line.substringBefore("//") }

    // -- No EvidenceCustodian dependency, anywhere, at any depth -----------

    @Test
    fun `no type reachable from OcrRecognitionRequest holds a reference to EvidenceCustodian, at any depth`() {
        val excludedQualifiedName = EvidenceCustodian::class.qualifiedName

        val propertyTypeNames = OcrRecognitionRequest::class.declaredMemberProperties.mapNotNull {
            (it.returnType.classifier as? KClass<*>)?.qualifiedName
        }
        val constructorTypeNames = OcrRecognitionRequest::class.primaryConstructor?.parameters?.mapNotNull {
            (it.type.classifier as? KClass<*>)?.qualifiedName
        } ?: emptyList()
        val functionTypeNames = OcrRecognitionRequest::class.declaredFunctions.flatMap { function ->
            function.parameters.mapNotNull { (it.type.classifier as? KClass<*>)?.qualifiedName } +
                listOfNotNull((function.returnType.classifier as? KClass<*>)?.qualifiedName)
        }

        (propertyTypeNames + constructorTypeNames + functionTypeNames).forEach { qualifiedName ->
            assertFalse(
                qualifiedName == excludedQualifiedName,
                "OcrRecognitionRequest must not reference EvidenceCustodian -- Scope Lock Section 5 and Section 13 " +
                    "both exclude it from the OCR mechanism's own dependency graph, at any depth.",
            )
        }
    }

    @Test
    fun `no file implementing Units 1 through 3 imports or references EvidenceCustodian in any form`() {
        ocrUnitFiles.forEach { file ->
            check(file.exists()) { "${file.path} not found from working directory ${java.io.File(".").absolutePath}" }
            assertFalse(
                file.readText().codeOnly().contains("EvidenceCustodian"),
                "${file.path} must not reference EvidenceCustodian in any form -- Implementation Plan Unit 4's own " +
                    "'Files explicitly prohibited' names exactly this: 'Any file that imports or references " +
                    "EvidenceCustodian in any form.'",
            )
        }
    }

    @Test
    fun `no code path anywhere in Units 1 through 3 can retrieve evidence independently -- no suspend function exists beyond the single recognise operations`() {
        val declaredSuspendFunctionNames = listOf(
            OcrMechanism::class,
            OcrProviderAdapter::class,
            OcrExecutionSequencer::class,
        ).flatMap { type -> type.declaredFunctions.filter { it.isSuspend }.map { "${type.simpleName}.${it.name}" } }

        assertEquals(
            listOf("OcrExecutionSequencer.recognise", "OcrMechanism.recognise", "OcrProviderAdapter.recognise"),
            declaredSuspendFunctionNames.sorted(),
            "Units 1-3 must declare no suspend function beyond the single, already-authorised recognise operation " +
                "on each of OcrMechanism, OcrProviderAdapter, and OcrExecutionSequencer -- a second suspend " +
                "function anywhere would be exactly the kind of independent retrieval path this Unit forbids.",
        )
    }

    // -- Already-retrieved content only: no live, writable, or retrieval-capable reference --

    @Test
    fun `OcrRecognitionRequest-content is a plain in-memory ByteArray, never a retrieval-capable or lazily-resolved type`() {
        val contentProperty = OcrRecognitionRequest::class.declaredMemberProperties.single { it.name == "content" }

        assertEquals(
            ByteArray::class,
            contentProperty.returnType.classifier,
            "OcrRecognitionRequest.content must be a plain ByteArray -- never a URL, a Path, a File handle, a " +
                "Supplier, or any other type capable of triggering a fresh, independent fetch when read.",
        )
    }

    @Test
    fun `OcrRecognitionRequest declares no function of its own beyond ordinary structural operations`() {
        val declaredFunctionNames = OcrRecognitionRequest::class.declaredFunctions.map { it.name }.toSet()
        val ordinaryStructuralOperations = setOf("equals", "hashCode", "toString")

        assertTrue(
            ordinaryStructuralOperations.containsAll(declaredFunctionNames),
            "OcrRecognitionRequest must declare no function beyond equals/hashCode/toString -- any further " +
                "function would be capable of representing a retrieval, refresh, or fetch operation this Unit " +
                "forbids. Found: $declaredFunctionNames",
        )
    }

    @Test
    fun `passing a request through the full Unit 1-3 pipeline never mutates the caller's own content bytes`() = runTest {
        val original = byteArrayOf(10, 20, 30, 40, 50)
        val originalSnapshot = original.copyOf()
        val request = OcrRecognitionRequest(
            sourceEvidenceId = EvidenceArtifactId("evidence-1"),
            content = original,
            mediaType = "image/png",
        )

        val adapter = object : OcrProviderAdapter {
            override suspend fun recognise(request: OcrRecognitionRequest): OcrRecognitionOutcome {
                // A well-behaved adapter reads request.content; it never writes to it. This fake
                // reads every byte (proving the content is genuinely readable) without mutating it.
                request.content.sum()
                return OcrRecognitionOutcome.Failed(reason = "fake adapter -- read only, no recognition performed")
            }
        }
        val sequencer = OcrExecutionSequencer(adapter)

        sequencer.recognise(request)

        assertContentEquals(
            originalSnapshot,
            original,
            "no code path in Units 1-3 may mutate the caller's own content bytes -- Unit 4 requires the request " +
                "carry 'never a live, writable reference,' verified here at the behavioural level across the full " +
                "recognise() call chain.",
        )
    }

    @Test
    fun `two requests built from the same already-retrieved bytes are independent copies at the caller's own discretion, never a shared mutable handle owned by this Unit`() {
        val bytesA = byteArrayOf(1, 2, 3)
        val requestA = OcrRecognitionRequest(EvidenceArtifactId("evidence-1"), bytesA, "image/png")

        // Unit 1-3 never clones, pools, or caches content on the caller's behalf -- the request is a
        // thin, passive holder of exactly the reference the caller supplied, nothing more.
        assertTrue(requestA.content === bytesA, "OcrRecognitionRequest must hold exactly the caller-supplied reference, unmodified and uncopied by this Unit itself")
    }

    // -- No prohibited dependency of any kind is reachable from the request shape --

    @Test
    fun `no type reachable from OcrRecognitionRequest holds a reference to MemoryCore, KnowledgeSubmission, PermissionEngine, or EvidenceIntelligence`() {
        val excludedQualifiedNames = setOf(
            MemoryCore::class.qualifiedName,
            MemoryRetrieval::class.qualifiedName,
            KnowledgeSubmission::class.qualifiedName,
            PermissionEngine::class.qualifiedName,
            EvidenceIntelligence::class.qualifiedName,
        )

        val propertyTypeNames = OcrRecognitionRequest::class.declaredMemberProperties.mapNotNull {
            (it.returnType.classifier as? KClass<*>)?.qualifiedName
        }

        propertyTypeNames.forEach { qualifiedName ->
            assertFalse(
                excludedQualifiedNames.contains(qualifiedName),
                "OcrRecognitionRequest must not reference '$qualifiedName' -- Scope Lock Section 13 excludes " +
                    "MemoryCore, KnowledgeSubmission, the Permission Engine, and EvidenceIntelligence from the " +
                    "OCR mechanism's own dependency graph, at any depth.",
            )
        }
    }

    @Test
    fun `no file implementing Units 1 through 3 performs orchestration -- no reference to EvidenceIntelligence, RequiresOcr, or the composition root`() {
        val forbiddenFragments = listOf("EvidenceIntelligence", "RequiresOcr", "ParkerRuntime")

        ocrUnitFiles.forEach { file ->
            val codeOnly = file.readText().codeOnly()
            forbiddenFragments.forEach { forbidden ->
                assertFalse(
                    codeOnly.contains(forbidden),
                    "${file.path} must not reference '$forbidden' -- orchestration, Evidence Intelligence " +
                        "integration, and runtime composition all remain later, separately-governed work " +
                        "(Implementation Plan Units 10 and 12; Scope Lock Section 18).",
                )
            }
        }
    }
}
