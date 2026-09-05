package parker.core.runtime

import java.time.Instant
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import parker.core.interfaces.DerivativeGenerationId
import parker.core.interfaces.DerivativeGenerationRecord
import parker.core.interfaces.DerivativeGenerationTest
import parker.core.interfaces.DerivativeParentReference
import parker.core.interfaces.DerivativeProducerIdentity
import parker.core.interfaces.DerivativeTransformation
import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.OcrDerivativeGenerationDiscovery

/**
 * UI-INGESTION-8B: [TierBOcrDerivativeGenerationDiscoveryCoordinator]'s own paired-identity
 * re-validation discipline -- exercised here against a deliberately misbehaving fake
 * [OcrDerivativeGenerationDiscovery] that returns records it should not, proving the coordinator
 * itself (not merely the real storage implementation) is the thing that guarantees no cross-evidence
 * or non-OCR leakage, per `DOCUMENT_INGESTION_TIER_B_OCR_EXACT_EVIDENCE_DERIVATIVE_GENERATION_DISCOVERY_SCOPE_LOCK_AMENDMENT.md` §9.
 */
class TierBOcrDerivativeGenerationDiscoveryCoordinatorTest {
    private fun ocrRecord(id: String, evidenceId: String = "source-1", generatedAt: Instant = Instant.EPOCH) =
        DerivativeGenerationTest.record(id).copy(
            rootSourceEvidenceArtifactId = EvidenceArtifactId(evidenceId),
            parents = listOf(DerivativeParentReference.RootEvidenceArtifact(EvidenceArtifactId(evidenceId))),
            producerIdentity = DerivativeProducerIdentity(
                pluginIdentity = "test-parser", pluginVersion = "1.0", configurationIdentity = "test-config-v1",
                modelIdentity = "test-model", modelVersion = "1.0",
            ),
            transformationHistory = listOf(DerivativeTransformation.OCR, DerivativeTransformation.MODEL_INFERENCE),
            generatedAt = generatedAt,
        )

    private class FakeDiscovery(private val all: List<DerivativeGenerationRecord>) : OcrDerivativeGenerationDiscovery {
        var calls = 0
        var lastRequested: EvidenceArtifactId? = null
        override suspend fun findOcrGenerationsForEvidence(evidenceArtifactId: EvidenceArtifactId): List<DerivativeGenerationRecord> {
            calls++
            lastRequested = evidenceArtifactId
            return all
        }
    }

    @Test
    fun `the coordinator strips out a candidate rooted at a different evidence artifact even if the underlying discovery source wrongly returns it`() = runTest {
        val mine = ocrRecord("generation-mine", evidenceId = "source-1")
        val theirs = ocrRecord("generation-theirs", evidenceId = "source-2")
        val coordinator = TierBOcrDerivativeGenerationDiscoveryCoordinator(FakeDiscovery(listOf(mine, theirs)))
        val discovered = coordinator.discover(EvidenceArtifactId("source-1"))
        assertEquals(listOf(mine), discovered)
    }

    @Test
    fun `the coordinator strips out a non-OCR candidate even if the underlying discovery source wrongly returns it`() = runTest {
        val ocr = ocrRecord("generation-ocr")
        val nonOcr = DerivativeGenerationTest.record("generation-csv")
        val coordinator = TierBOcrDerivativeGenerationDiscoveryCoordinator(FakeDiscovery(listOf(ocr, nonOcr)))
        val discovered = coordinator.discover(EvidenceArtifactId("source-1"))
        assertEquals(listOf(ocr), discovered)
    }

    @Test
    fun `the coordinator returns every legitimate admitted generation -- 0, 1, or many -- with no single authoritative slot`() = runTest {
        val empty = TierBOcrDerivativeGenerationDiscoveryCoordinator(FakeDiscovery(emptyList()))
        assertEquals(emptyList(), empty.discover(EvidenceArtifactId("source-1")))

        val first = ocrRecord("6d8d9307-8281-4574-a050-f9fec1c916f1", generatedAt = Instant.parse("2026-09-05T03:48:00Z"))
        val second = ocrRecord("4c8ed1e2-7524-467c-b4b3-32e8293c7854", generatedAt = Instant.parse("2026-09-05T03:50:00Z"))
        val many = TierBOcrDerivativeGenerationDiscoveryCoordinator(FakeDiscovery(listOf(first, second)))
        val discovered = many.discover(EvidenceArtifactId("source-1"))
        assertEquals(listOf(second.derivativeGenerationId, first.derivativeGenerationId), discovered.map { it.derivativeGenerationId })
    }

    @Test
    fun `the coordinator queries the discovery source with exactly the requested evidence artifact, once`() = runTest {
        val discovery = FakeDiscovery(emptyList())
        val coordinator = TierBOcrDerivativeGenerationDiscoveryCoordinator(discovery)
        coordinator.discover(EvidenceArtifactId("source-1"))
        assertEquals(1, discovery.calls)
        assertEquals(EvidenceArtifactId("source-1"), discovery.lastRequested)
    }

    @Test
    fun `a discovered generation still requires paired retrieval -- wrong evidence artifact fails closed at TierBOcrContentRetrievalCoordinator, not at discovery`() = runTest {
        val id = "generation-real"
        val record = ocrRecord(id, evidenceId = "source-1")
        val discoveryCoordinator = TierBOcrDerivativeGenerationDiscoveryCoordinator(FakeDiscovery(listOf(record)))
        val discovered = discoveryCoordinator.discover(EvidenceArtifactId("source-1"))
        val discoveredId = discovered.single().derivativeGenerationId

        val generationStorage = object : parker.core.interfaces.DerivativeGenerationStorage {
            override suspend fun prepare(record: DerivativeGenerationRecord) = error("not used")
            override suspend fun publishPrepared(derivativeGenerationId: DerivativeGenerationId) = error("not used")
            override suspend fun retrieve(derivativeGenerationId: DerivativeGenerationId): DerivativeGenerationRecord? =
                if (derivativeGenerationId == discoveredId) record else null
        }
        val contentStorage = object : parker.core.interfaces.DerivativeContentStorage {
            override suspend fun prepare(entry: parker.core.interfaces.DerivativeContentEntry) = error("not used")
            override suspend fun publishPrepared(derivativeGenerationId: DerivativeGenerationId) = error("not used")
            override suspend fun retrieve(derivativeGenerationId: DerivativeGenerationId): parker.core.interfaces.DerivativeContentEntry? = error("must not be reached -- retrieval must fail closed before ever touching content")
        }
        val retrievalCoordinator = TierBOcrContentRetrievalCoordinator(generationStorage, contentStorage)

        val wrongEvidenceResult = retrievalCoordinator.retrieve(EvidenceArtifactId("source-2"), discoveredId)
        assertEquals(parker.core.interfaces.TierBOcrContentRetrievalOutcome.SourceMismatch(EvidenceArtifactId("source-2"), discoveredId), wrongEvidenceResult)
    }
}
