package parker.core.runtime

import java.security.MessageDigest
import java.time.Instant
import java.util.UUID
import parker.core.interfaces.CsvStructuralExtractionOutcome
import parker.core.interfaces.CsvStructuralExtractor
import parker.core.interfaces.CsvStructuralResult
import parker.core.interfaces.DerivativeContentIdentity
import parker.core.interfaces.DerivativeGenerationId
import parker.core.interfaces.DerivativeGenerationRecord
import parker.core.interfaces.DerivativeGenerationStorage
import parker.core.interfaces.DerivativeGenerationStorageException
import parker.core.interfaces.DerivativeOperationalOutcome
import parker.core.interfaces.DerivativeParentReference
import parker.core.interfaces.DocumentIngestionAudit
import parker.core.interfaces.DocumentIngestionAuditRecord
import parker.core.interfaces.DocumentIngestionAuditException
import parker.core.interfaces.DocumentIngestionAuditStage
import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.PrincipalId

data class CsvIngestionSource(
    val evidenceArtifactId: EvidenceArtifactId,
    val content: ByteArray,
    val expectedSha256: String,
) {
    init {
        require(expectedSha256.matches(Regex("^[0-9a-f]{64}$"))) { "CsvIngestionSource.expectedSha256 must be a lowercase SHA-256 digest" }
    }
}

sealed class DerivativeGenerationCoordinationOutcome {
    data class Admitted(
        val record: DerivativeGenerationRecord,
        val csvStructure: CsvStructuralResult,
    ) : DerivativeGenerationCoordinationOutcome()
    data class ExtractionFailed(val reason: String) : DerivativeGenerationCoordinationOutcome()
    data class SourceIntegrityFailed(val reason: String) : DerivativeGenerationCoordinationOutcome()
    data class PreparationFailed(val derivativeGenerationId: DerivativeGenerationId, val reason: String) : DerivativeGenerationCoordinationOutcome()
    data class AuthorisationAuditFailed(val derivativeGenerationId: DerivativeGenerationId, val reason: String) : DerivativeGenerationCoordinationOutcome()
    data class PublicationFailed(val derivativeGenerationId: DerivativeGenerationId, val reason: String) : DerivativeGenerationCoordinationOutcome()
    data class AdmittedAuditFailed(
        val record: DerivativeGenerationRecord,
        val csvStructure: CsvStructuralResult,
        val reason: String,
    ) : DerivativeGenerationCoordinationOutcome()
}

class DerivativeGenerationCoordinator(
    private val csvExtractor: CsvStructuralExtractor,
    private val storage: DerivativeGenerationStorage,
    private val audit: DocumentIngestionAudit,
    private val idFactory: () -> DerivativeGenerationId = { DerivativeGenerationId(UUID.randomUUID().toString()) },
    private val now: () -> Instant = Instant::now,
) {
    suspend fun ingestCsv(
        source: CsvIngestionSource,
        requestingPrincipalId: PrincipalId,
        correlationValue: String,
    ): DerivativeGenerationCoordinationOutcome {
        require(correlationValue.isNotBlank()) { "correlationValue must not be blank" }
        if (sha256(source.content) != source.expectedSha256) {
            return DerivativeGenerationCoordinationOutcome.SourceIntegrityFailed("Source SHA-256 does not match the governed source context")
        }
        val extracted = when (val outcome = csvExtractor.extract(source.content.copyOf())) {
            is CsvStructuralExtractionOutcome.Malformed -> return DerivativeGenerationCoordinationOutcome.ExtractionFailed(outcome.reason)
            is CsvStructuralExtractionOutcome.Extracted -> outcome.result
        }
        if (sha256(source.content) != source.expectedSha256) {
            return DerivativeGenerationCoordinationOutcome.SourceIntegrityFailed("Source SHA-256 changed during CSV extraction")
        }
        val id = idFactory()
        val record = DerivativeGenerationRecord(
            derivativeGenerationId = id,
            rootSourceEvidenceArtifactId = source.evidenceArtifactId,
            parents = listOf(DerivativeParentReference.RootEvidenceArtifact(source.evidenceArtifactId)),
            derivativeKind = "CSV structure",
            producerIdentity = extracted.producerIdentity,
            transformationHistory = extracted.transformationHistory,
            generatedAt = now(),
            contentIdentity = DerivativeContentIdentity.NoCanonicalSerialization,
            completenessState = extracted.completenessState,
            operationalOutcome = DerivativeOperationalOutcome.USABLE,
            warnings = extracted.warnings,
        )
        try {
            storage.prepare(record)
        } catch (e: DerivativeGenerationStorageException) {
            return DerivativeGenerationCoordinationOutcome.PreparationFailed(id, e.message ?: e::class.simpleName.orEmpty())
        }
        try {
            audit.record(auditRecord(correlationValue, source.evidenceArtifactId, requestingPrincipalId, id, DocumentIngestionAuditStage.ADMISSION_AUTHORISED))
        } catch (e: DocumentIngestionAuditException) {
            return DerivativeGenerationCoordinationOutcome.AuthorisationAuditFailed(id, e.message ?: e::class.simpleName.orEmpty())
        }
        try {
            storage.publishPrepared(id)
        } catch (e: DerivativeGenerationStorageException) {
            return DerivativeGenerationCoordinationOutcome.PublicationFailed(id, e.message ?: e::class.simpleName.orEmpty())
        }
        try {
            audit.record(auditRecord(correlationValue, source.evidenceArtifactId, requestingPrincipalId, id, DocumentIngestionAuditStage.ADMITTED))
        } catch (e: DocumentIngestionAuditException) {
            return DerivativeGenerationCoordinationOutcome.AdmittedAuditFailed(record, extracted, e.message ?: e::class.simpleName.orEmpty())
        }
        return DerivativeGenerationCoordinationOutcome.Admitted(record, extracted)
    }

    private fun auditRecord(
        correlationValue: String,
        sourceId: EvidenceArtifactId,
        principalId: PrincipalId,
        generationId: DerivativeGenerationId,
        stage: DocumentIngestionAuditStage,
    ) = DocumentIngestionAuditRecord(
        correlationValue = correlationValue,
        sourceEvidenceArtifactId = sourceId,
        requestingPrincipalId = principalId,
        operationalOutcome = stage.name,
        recordedAt = now(),
        derivativeGenerationId = generationId,
        stage = stage,
    )

    private fun sha256(content: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(content).joinToString("") { "%02x".format(it) }
}
