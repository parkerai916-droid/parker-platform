package parker.core.runtime

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import parker.core.interfaces.DerivativeGenerationId
import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.TierADocumentFormat
import parker.core.interfaces.TierADocumentRoutingResult
import parker.core.interfaces.TierAOwnerInvocationOutcome
import parker.core.interfaces.TierAMediaFacts

class PostAdmissionProcessingCoordinatorTest {
    private val evidence = EvidenceArtifactId("evidence-post-admission-test")
    private val facts = TierAMediaFacts("application/pdf", "application/pdf", "test.pdf", false)

    @Test
    fun `native Tier A admission is analysis ready without OCR`() = runTest {
        var acquisitions = 0
        val coordinator = PostAdmissionProcessingCoordinator(
            invokeTierA = { TierAOwnerInvocationOutcome.Routed(TierADocumentRoutingResult.Admitted(
                TierADocumentFormat.PDF,
                testRecord(),
                parker.core.interfaces.TierADerivativePayload.Pdf(
                    parker.core.interfaces.PdfStructuralResult(
                        documentText = "native", pageCount = 1, pageTextAssociationAvailable = true,
                        metadata = emptyList(), embeddedResources = emptyList(),
                        producerIdentity = parker.core.interfaces.DerivativeProducerIdentity("t", "1", "test"),
                        transformationHistory = emptyList(), completenessState = parker.core.interfaces.DerivativeCompletenessState.ACCOUNTED_FOR,
                        warnings = emptyList(),
                    ),
                ), facts,
            )) },
            executeGovernedAcquisition = { acquisitions++; error("OCR/acquisition must not run for native Tier A") },
        )

        val result = coordinator.process(evidence)
        val ready = assertIs<PostAdmissionProcessingOutcome.AnalysisReady>(result)
        assertEquals("TIER_A_NATIVE_REPRESENTATION", ready.capabilityId)
        assertEquals(0, acquisitions)
    }

    @Test
    fun `RequiresTierB continues through governed acquisition and reports explicit unavailable state`() = runTest {
        val coordinator = PostAdmissionProcessingCoordinator(
            invokeTierA = { TierAOwnerInvocationOutcome.Routed(TierADocumentRoutingResult.RequiresTierB("OCR", facts)) },
            executeGovernedAcquisition = { AgentGatewayAcquisitionResult.ProviderNotReady(evidence) },
        )

        assertIs<PostAdmissionProcessingOutcome.CapabilityUnavailable>(coordinator.process(evidence))
    }

    @Test
    fun `external acquisition completion is analysis ready`() = runTest {
        val generation = DerivativeGenerationId("generation-post-admission-test")
        val coordinator = PostAdmissionProcessingCoordinator(
            invokeTierA = { TierAOwnerInvocationOutcome.Routed(TierADocumentRoutingResult.RequiresTierB("OCR", facts)) },
            executeGovernedAcquisition = { AgentGatewayAcquisitionResult.Completed(evidence, generation, "external", parker.core.interfaces.EvidenceAcquisitionMechanism.EXTERNAL_TRANSCRIPTION) },
        )

        assertIs<PostAdmissionProcessingOutcome.AnalysisReady>(coordinator.process(evidence))
    }

    @Test
    fun `existing governed derivative makes retry a no-op`() = runTest {
        var tierACalls = 0
        val generation = DerivativeGenerationId("generation-existing-test")
        val coordinator = PostAdmissionProcessingCoordinator(
            invokeTierA = { tierACalls++; error("retry must not rerun Tier A") },
            executeGovernedAcquisition = { error("retry must not reacquire") },
            findExistingAuthoritativeDerivative = { generation },
        )

        val ready = assertIs<PostAdmissionProcessingOutcome.AnalysisReady>(coordinator.process(evidence))
        assertEquals(generation, ready.derivativeGenerationId)
        assertEquals(0, tierACalls)
    }

    private fun testRecord() = parker.core.interfaces.DerivativeGenerationRecord(
        DerivativeGenerationId("generation-native-test"), evidence,
        listOf(parker.core.interfaces.DerivativeParentReference.RootEvidenceArtifact(evidence)),
        "PDF structure", parker.core.interfaces.DerivativeProducerIdentity("t", "1", "test"),
        listOf(parker.core.interfaces.DerivativeTransformation.STRUCTURAL_PARSING), java.time.Instant.EPOCH, parker.core.interfaces.DerivativeContentIdentity.NoCanonicalSerialization,
        parker.core.interfaces.DerivativeCompletenessState.ACCOUNTED_FOR, parker.core.interfaces.DerivativeOperationalOutcome.USABLE,
        emptyList(), null,
    )
}
