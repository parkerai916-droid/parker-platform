package parker.core.runtime

import kotlin.reflect.KClass
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import parker.core.interfaces.EvidenceCustodian
import parker.core.interfaces.EvidenceIntelligence
import parker.core.interfaces.KnowledgeSubmission
import parker.core.interfaces.MemoryCore
import parker.core.interfaces.MemoryRetrieval
import parker.core.interfaces.OcrMechanism
import parker.core.interfaces.OcrProviderAdapter
import parker.core.interfaces.OcrRecognitionIdentity
import parker.core.interfaces.OcrRecognitionOutcome
import parker.core.interfaces.OcrRecognitionRequest
import parker.core.interfaces.OcrRecognitionResult
import parker.core.interfaces.OwnerEvidenceDeletionAuthority
import parker.core.interfaces.PermissionEngine
import parker.core.interfaces.TranscriptionFidelity

/**
 * OCR Mechanism, Implementation Unit 3 ("Internal Execution Sequencer").
 * Structural, plus hand-written-fake behavioural, tests -- mirroring
 * `EvidenceExtractionCoordinatorTest.kt`'s own hand-written-fake style
 * (`kotlinx.coroutines.test.runTest`) and `OcrMechanismScopeTest.kt`'s /
 * `OcrProviderAdapterScopeTest.kt`'s own reflection discipline.
 *
 * **On "each of the seven failure distinctions."** Implementation Plan
 * Unit 3's own Verification requirements were drafted before Unit 1's own
 * corrected shape settled `OcrRecognitionOutcome` at exactly two variants
 * ([OcrRecognitionOutcome.Recognised], [OcrRecognitionOutcome.Failed]) --
 * Scope Lock Section 10 forbids Unit 1 from representing the seven
 * distinctions as any named, coded, or enum-like list, and no such list
 * exists anywhere in this repository. This suite therefore tests success
 * and failure -- the only two outcomes Unit 1 actually defines -- and the
 * boundary between them, which is the literal requirement's own intent
 * applied to the shape as it now, lawfully, exists.
 */
class OcrExecutionSequencerTest {

    private class FakeOcrProviderAdapter(
        private val outcome: OcrRecognitionOutcome? = null,
        private val throwable: Throwable? = null,
    ) : OcrProviderAdapter {
        var invocationCount: Int = 0
            private set
        var lastRequest: OcrRecognitionRequest? = null
            private set

        override suspend fun recognise(request: OcrRecognitionRequest): OcrRecognitionOutcome {
            invocationCount += 1
            lastRequest = request
            throwable?.let { throw it }
            return outcome ?: error("FakeOcrProviderAdapter constructed with neither an outcome nor a throwable")
        }
    }

    private fun sampleRequest() = OcrRecognitionRequest(
        sourceEvidenceId = parker.core.interfaces.EvidenceArtifactId("evidence-1"),
        content = byteArrayOf(1, 2, 3),
        mediaType = "image/png",
    )

    private fun sampleResult() = OcrRecognitionResult(
        recognisedText = "sample text",
        fidelity = TranscriptionFidelity.VERBATIM,
        identity = OcrRecognitionIdentity(mechanismIdentity = "fake", configurationProfile = "default"),
        recognisedAt = java.time.Instant.EPOCH,
    )

    // -- Behaviour: successful recognition passes through unchanged --------

    @Test
    fun `a successful recognition from the adapter is returned unchanged`() = runTest {
        val expected = OcrRecognitionOutcome.Recognised(sampleResult())
        val adapter = FakeOcrProviderAdapter(outcome = expected)
        val sequencer = OcrExecutionSequencer(adapter)

        val actual = sequencer.recognise(sampleRequest())

        assertSame(expected, actual, "a successful outcome must be returned exactly as the adapter produced it")
    }

    // -- Behaviour: failed recognition passes through unchanged -- the boundary between success and failure --

    @Test
    fun `a failed recognition from the adapter is returned unchanged, with its own reason preserved`() = runTest {
        val expected = OcrRecognitionOutcome.Failed(reason = "the supplied content could not be decoded")
        val adapter = FakeOcrProviderAdapter(outcome = expected)
        val sequencer = OcrExecutionSequencer(adapter)

        val actual = sequencer.recognise(sampleRequest())

        assertSame(expected, actual, "a failed outcome must be returned exactly as the adapter produced it")
        assertEquals("the supplied content could not be decoded", (actual as OcrRecognitionOutcome.Failed).reason)
    }

    // -- No execution beyond calling the supplied adapter once --------------

    @Test
    fun `recognise invokes the supplied adapter exactly once, with the exact request supplied`() = runTest {
        val request = sampleRequest()
        val adapter = FakeOcrProviderAdapter(outcome = OcrRecognitionOutcome.Recognised(sampleResult()))
        val sequencer = OcrExecutionSequencer(adapter)

        sequencer.recognise(request)

        assertEquals(1, adapter.invocationCount, "the sequencer must invoke the adapter exactly once per call -- no retry")
        assertSame(request, adapter.lastRequest, "the sequencer must pass the exact request through unchanged")
    }

    @Test
    fun `two sequential calls invoke the adapter exactly twice, never fewer, never more, never concurrently`() = runTest {
        val adapter = FakeOcrProviderAdapter(outcome = OcrRecognitionOutcome.Recognised(sampleResult()))
        val sequencer = OcrExecutionSequencer(adapter)

        sequencer.recognise(sampleRequest())
        sequencer.recognise(sampleRequest())

        assertEquals(
            2,
            adapter.invocationCount,
            "each call to recognise must invoke the adapter exactly once of its own -- no caching, no batching, " +
                "no queueing of a prior call's own result",
        )
    }

    @Test
    fun `a fault the adapter throws propagates unchanged -- no try-catch, no retry, no conversion to Failed`() = runTest {
        val fault = IllegalStateException("adapter crashed")
        val adapter = FakeOcrProviderAdapter(throwable = fault)
        val sequencer = OcrExecutionSequencer(adapter)

        val thrown = assertFailsWith<IllegalStateException> {
            sequencer.recognise(sampleRequest())
        }
        assertSame(fault, thrown, "a genuine adapter fault must propagate unchanged, mirroring EvidenceExtractionCoordinator's own identical discipline")
        assertEquals(1, adapter.invocationCount, "a single invocation attempt must occur -- no retry after a fault")
    }

    // -- Contract shape: exactly one adapter accepted, never a selection among several --

    @Test
    fun `OcrExecutionSequencer implements OcrMechanism`() {
        assertTrue(OcrMechanism::class.java.isAssignableFrom(OcrExecutionSequencer::class.java))
    }

    @Test
    fun `OcrExecutionSequencer declares exactly one constructor parameter, of type OcrProviderAdapter -- never a collection`() {
        val constructor = OcrExecutionSequencer::class.primaryConstructor
            ?: error("OcrExecutionSequencer must declare a primary constructor")

        assertEquals(1, constructor.parameters.size, "exactly one constructor parameter -- never a list, map, or registry of adapters")
        val parameterType = constructor.parameters.single().type.classifier
        assertEquals(OcrProviderAdapter::class, parameterType, "the sole parameter must be a single OcrProviderAdapter, not a collection of them")
    }

    @Test
    fun `OcrExecutionSequencer declares exactly one property -- its own single adapter`() {
        val properties = OcrExecutionSequencer::class.declaredMemberProperties

        assertEquals(1, properties.size, "no property beyond the single adapter dependency may exist")
        assertEquals(OcrProviderAdapter::class, properties.single().returnType.classifier)
    }

    @Test
    fun `OcrExecutionSequencer declares exactly one function -- recognise -- performing no further orchestration`() {
        val declared = OcrExecutionSequencer::class.declaredFunctions.filter { it.name == "recognise" }
        assertEquals(1, declared.size, "exactly one recognise implementation -- no additional public operation")
    }

    // -- Prohibited dependencies (Scope Lock Section 13; Implementation Plan Unit 3) --

    @Test
    fun `no property or constructor parameter reachable from OcrExecutionSequencer references EvidenceCustodian, MemoryCore, KnowledgeSubmission, PermissionEngine, or EvidenceIntelligence`() {
        val excludedQualifiedNames = setOf(
            EvidenceCustodian::class.qualifiedName,
            MemoryCore::class.qualifiedName,
            MemoryRetrieval::class.qualifiedName,
            KnowledgeSubmission::class.qualifiedName,
            PermissionEngine::class.qualifiedName,
            EvidenceIntelligence::class.qualifiedName,
            OwnerEvidenceDeletionAuthority::class.qualifiedName,
        )

        val typesToCheck: List<KClass<*>> = listOf(OcrExecutionSequencer::class, OcrProviderAdapter::class)

        typesToCheck.forEach { type ->
            val propertyTypeNames = type.declaredMemberProperties.mapNotNull { (it.returnType.classifier as? KClass<*>)?.qualifiedName }
            val constructorTypeNames = type.primaryConstructor?.parameters?.mapNotNull {
                (it.type.classifier as? KClass<*>)?.qualifiedName
            } ?: emptyList()
            val functionTypeNames = type.declaredFunctions.flatMap { function ->
                function.parameters.mapNotNull { (it.type.classifier as? KClass<*>)?.qualifiedName } +
                    listOfNotNull((function.returnType.classifier as? KClass<*>)?.qualifiedName)
            }

            (propertyTypeNames + constructorTypeNames + functionTypeNames).forEach { qualifiedName ->
                assertFalse(
                    excludedQualifiedNames.contains(qualifiedName),
                    "${type.simpleName} must not reference '$qualifiedName' -- Scope Lock Section 13 and " +
                        "Implementation Plan Unit 3 exclude EvidenceCustodian, MemoryCore, KnowledgeSubmission, " +
                        "the Permission Engine, EvidenceIntelligence, and OwnerEvidenceDeletionAuthority from the " +
                        "sequencer's own dependency graph, at any depth.",
                )
            }
        }
    }

    @Test
    fun `no file implementing or testing OcrExecutionSequencer references ParkerRuntime or any composition-root type`() {
        val srcRoot = java.io.File("src")
        check(srcRoot.exists()) { "src/ directory not found from working directory ${java.io.File(".").absolutePath}" }

        val offendingFiles = srcRoot.walkTopDown()
            .filter { it.isFile && it.extension == "kt" && it.name == "OcrExecutionSequencer.kt" }
            .filter { file -> file.readText().contains("ParkerRuntime") }
            .map { it.path }
            .toList()

        assertTrue(
            offendingFiles.isEmpty(),
            "OcrExecutionSequencer.kt must not reference ParkerRuntime or any composition-root type -- runtime " +
                "composition remains Implementation Plan Unit 12, structurally blocked -- found in: $offendingFiles",
        )
    }

    @Test
    fun `no provider-selection, discovery, or registration vocabulary appears in OcrExecutionSequencer's own file`() {
        val file = java.io.File("src/runtime/OcrExecutionSequencer.kt")
        check(file.exists()) { "src/runtime/OcrExecutionSequencer.kt not found" }
        val forbiddenFragments = listOf("registry", "register(", "discover", "select(", "chooseProvider", "List<OcrProviderAdapter>")

        val text = file.readText()
        forbiddenFragments.forEach { forbidden ->
            assertFalse(
                text.contains(forbidden, ignoreCase = false),
                "OcrExecutionSequencer.kt must not contain '$forbidden' -- provider selection, discovery, and " +
                    "registration are all explicitly prohibited in this Unit.",
            )
        }
    }
}
