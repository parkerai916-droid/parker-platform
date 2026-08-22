package parker.core.interfaces

import java.time.Instant

data class DocumentIngestionAuditRecord(
    val correlationValue: String,
    val sourceEvidenceArtifactId: EvidenceArtifactId,
    val requestingPrincipalId: PrincipalId,
    val operationalOutcome: String,
    val recordedAt: Instant,
    val derivativeGenerationId: DerivativeGenerationId? = null,
) {
    init {
        require(correlationValue.isNotBlank()) { "DocumentIngestionAuditRecord.correlationValue must not be blank" }
        require(operationalOutcome.isNotBlank()) { "DocumentIngestionAuditRecord.operationalOutcome must not be blank" }
    }
}

sealed class DocumentIngestionAuditException(message: String, cause: Throwable? = null) :
    RuntimeException(message, cause) {
    class PersistenceFailure(message: String, cause: Throwable) : DocumentIngestionAuditException(message, cause)
}

fun interface DocumentIngestionAudit {
    suspend fun record(record: DocumentIngestionAuditRecord)
}
