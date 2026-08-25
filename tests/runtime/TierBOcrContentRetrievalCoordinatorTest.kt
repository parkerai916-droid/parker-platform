package parker.core.runtime

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.test.runTest
import parker.core.interfaces.DerivativeContentEntry
import parker.core.interfaces.DerivativeContentIdentity
import parker.core.interfaces.DerivativeContentStorage
import parker.core.interfaces.DerivativeGenerationId
import parker.core.interfaces.DerivativeGenerationRecord
import parker.core.interfaces.DerivativeOperationalOutcome
import parker.core.interfaces.DerivativeParentReference
import parker.core.interfaces.DerivativeTransformation
import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.TierADerivativePayload
import parker.core.interfaces.TierADerivativePayloadFixtures
import parker.core.interfaces.TierBOcrContentRetrievalOutcome
import java.time.Instant

/**
 * Document Ingestion — Tier B Durable OCR Derivative Content Retrieval
 * Boundary. Behavioural tests for [TierBOcrContentRetrievalCoordinator] --
 * unknown generation, source mismatch, and the §28 kind-discrimination
 * requirement specifically (a Tier A generation retrieved through the
 * Tier B-specific path yields [TierBOcrContentRetrievalOutcome.WrongDerivativeKind],
 * never a mis-decoded result). Real, filesystem-backed storage (temp
 * roots) -- never mocked, mirroring [TierAContentRetrievalCoordinatorTest]'s
 * own established style.
 */
class TierBOcrContentRetrievalCoordinatorTest {

    private fun coordinator(): Pair<TierBOcrContentRetrievalCoordinator, FileSystemDerivativeGenerationStorage> {
        val generationStorage = FileSystemDerivativeGenerationStorage(Files.createTempDirectory("tierb-retrieval-generation"))
        val contentStorage = FileSystemDerivativeContentStorage(Files.createTempDirectory("tierb-retrieval-content"))
        return TierBOcrContentRetrievalCoordinator(generationStorage, contentStorage) to generationStorage
    }

    private suspend fun admitOcrGeneration(
        coordinator: TierBOcrContentRetrievalCoordinator,
        generationStorage: FileSystemDerivativeGenerationStorage,
        contentStorage: DerivativeContentStorage,
        id: DerivativeGenerationId,
        evidenceArtifactId: EvidenceArtifactId,
    ): DerivativeGenerationRecord {
        val extracted = TierADerivativePayloadFixtures.ocr()
        val record = DerivativeGenerationRecord(
            derivativeGenerationId = id,
            rootSourceEvidenceArtifactId = evidenceArtifactId,
            parents = listOf(DerivativeParentReference.RootEvidenceArtifact(evidenceArtifactId)),
            derivativeKind = "OCR recognised text",
            producerIdentity = extracted.producerIdentity,
            transformationHistory = extracted.transformationHistory,
            generatedAt = Instant.EPOCH,
            contentIdentity = DerivativeContentIdentity.NoCanonicalSerialization,
            completenessState = extracted.completenessState,
            operationalOutcome = DerivativeOperationalOutcome.USABLE,
            warnings = extracted.warnings,
        )
        contentStorage.prepare(DerivativeContentEntry(id, evidenceArtifactId, TierADerivativePayload.Ocr(extracted)))
        contentStorage.publishPrepared(id)
        generationStorage.prepare(record)
        generationStorage.publishPrepared(id)
        return record
    }

    private suspend fun admitTierAGeneration(
        generationStorage: FileSystemDerivativeGenerationStorage,
        contentStorage: DerivativeContentStorage,
        id: DerivativeGenerationId,
        evidenceArtifactId: EvidenceArtifactId,
    ): DerivativeGenerationRecord {
        val csv = TierADerivativePayloadFixtures.csv()
        val record = DerivativeGenerationRecord(
            derivativeGenerationId = id,
            rootSourceEvidenceArtifactId = evidenceArtifactId,
            parents = listOf(DerivativeParentReference.RootEvidenceArtifact(evidenceArtifactId)),
            derivativeKind = "CSV structure",
            producerIdentity = csv.producerIdentity,
            transformationHistory = csv.transformationHistory,
            generatedAt = Instant.EPOCH,
            contentIdentity = DerivativeContentIdentity.NoCanonicalSerialization,
            completenessState = csv.completenessState,
            operationalOutcome = DerivativeOperationalOutcome.USABLE,
            warnings = csv.warnings,
        )
        contentStorage.prepare(DerivativeContentEntry(id, evidenceArtifactId, TierADerivativePayload.Csv(csv)))
        contentStorage.publishPrepared(id)
        generationStorage.prepare(record)
        generationStorage.publishPrepared(id)
        return record
    }

    @Test
    fun `unknown generation id fails closed with UnknownGeneration`() = runTest {
        val (coordinator, _) = coordinator()
        val outcome = coordinator.retrieve(EvidenceArtifactId("evidence-1"), DerivativeGenerationId("never-registered"))
        assertEquals(TierBOcrContentRetrievalOutcome.UnknownGeneration(DerivativeGenerationId("never-registered")), outcome)
    }

    @Test
    fun `a generation belonging to a different evidence artefact fails closed with SourceMismatch`() = runTest {
        val generationStorage = FileSystemDerivativeGenerationStorage(Files.createTempDirectory("tierb-retrieval-generation"))
        val contentStorage = FileSystemDerivativeContentStorage(Files.createTempDirectory("tierb-retrieval-content"))
        val coordinator = TierBOcrContentRetrievalCoordinator(generationStorage, contentStorage)
        val id = DerivativeGenerationId("generation-1")
        admitOcrGeneration(coordinator, generationStorage, contentStorage, id, EvidenceArtifactId("evidence-real"))

        val outcome = coordinator.retrieve(EvidenceArtifactId("evidence-other"), id)

        assertEquals(TierBOcrContentRetrievalOutcome.SourceMismatch(EvidenceArtifactId("evidence-other"), id), outcome)
    }

    @Test
    fun `a genuine Tier B OCR generation retrieves correctly with its own extracted content`() = runTest {
        val generationStorage = FileSystemDerivativeGenerationStorage(Files.createTempDirectory("tierb-retrieval-generation"))
        val contentStorage = FileSystemDerivativeContentStorage(Files.createTempDirectory("tierb-retrieval-content"))
        val coordinator = TierBOcrContentRetrievalCoordinator(generationStorage, contentStorage)
        val id = DerivativeGenerationId("generation-ocr-real")
        val evidenceArtifactId = EvidenceArtifactId("evidence-real")
        val record = admitOcrGeneration(coordinator, generationStorage, contentStorage, id, evidenceArtifactId)

        val outcome = assertIs<TierBOcrContentRetrievalOutcome.Retrieved>(coordinator.retrieve(evidenceArtifactId, id))

        assertEquals(record, outcome.record)
        assertEquals(TierADerivativePayloadFixtures.ocr(), outcome.extracted)
    }

    @Test
    fun `a Tier A generation retrieved through the Tier B-specific path yields WrongDerivativeKind, never a mis-decoded result -- scope lock §28`() = runTest {
        val generationStorage = FileSystemDerivativeGenerationStorage(Files.createTempDirectory("tierb-retrieval-generation"))
        val contentStorage = FileSystemDerivativeContentStorage(Files.createTempDirectory("tierb-retrieval-content"))
        val coordinator = TierBOcrContentRetrievalCoordinator(generationStorage, contentStorage)
        val id = DerivativeGenerationId("generation-tier-a")
        val evidenceArtifactId = EvidenceArtifactId("evidence-tier-a")
        admitTierAGeneration(generationStorage, contentStorage, id, evidenceArtifactId)

        val outcome = coordinator.retrieve(evidenceArtifactId, id)

        assertEquals(TierBOcrContentRetrievalOutcome.WrongDerivativeKind(id), outcome)
    }

    @Test
    fun `a generation record present with no corresponding content entry fails closed with ContentMissing`() = runTest {
        val generationStorage = FileSystemDerivativeGenerationStorage(Files.createTempDirectory("tierb-retrieval-generation"))
        val contentStorage = FileSystemDerivativeContentStorage(Files.createTempDirectory("tierb-retrieval-content"))
        val coordinator = TierBOcrContentRetrievalCoordinator(generationStorage, contentStorage)
        val id = DerivativeGenerationId("generation-orphan")
        val evidenceArtifactId = EvidenceArtifactId("evidence-orphan")
        val extracted = TierADerivativePayloadFixtures.ocr()
        val record = DerivativeGenerationRecord(
            derivativeGenerationId = id,
            rootSourceEvidenceArtifactId = evidenceArtifactId,
            parents = listOf(DerivativeParentReference.RootEvidenceArtifact(evidenceArtifactId)),
            derivativeKind = "OCR recognised text",
            producerIdentity = extracted.producerIdentity,
            transformationHistory = extracted.transformationHistory,
            generatedAt = Instant.EPOCH,
            contentIdentity = DerivativeContentIdentity.NoCanonicalSerialization,
            completenessState = extracted.completenessState,
            operationalOutcome = DerivativeOperationalOutcome.USABLE,
            warnings = extracted.warnings,
        )
        generationStorage.prepare(record)
        generationStorage.publishPrepared(id)
        // Deliberately never prepare/publish content -- the reachable orphan-generation
        // reconciliation state (Tier B scope lock §19/§22).

        val outcome = coordinator.retrieve(evidenceArtifactId, id)

        assertEquals(TierBOcrContentRetrievalOutcome.ContentMissing(id), outcome)
    }
}
