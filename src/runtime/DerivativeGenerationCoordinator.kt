package parker.core.runtime

import parker.core.interfaces.OcrStructuredValidationOutcome
import parker.core.interfaces.OcrRecognitionOutcome
import parker.core.interfaces.OcrModelSnapshot
import parker.core.interfaces.OcrAuthorityClassification
import parker.core.interfaces.OcrAuthorityPolicy

import java.security.MessageDigest
import java.time.Instant
import java.util.UUID
import parker.core.interfaces.CsvStructuralExtractionOutcome
import parker.core.interfaces.CsvStructuralExtractor
import parker.core.interfaces.CsvStructuralResult
import parker.core.interfaces.DerivativeContentEntry
import parker.core.interfaces.DerivativeContentIdentity
import parker.core.interfaces.DerivativeContentStorage
import parker.core.interfaces.DerivativeContentStorageException
import parker.core.interfaces.DerivativeGenerationId
import parker.core.interfaces.DerivativeGenerationRecord
import parker.core.interfaces.DerivativeGenerationStorage
import parker.core.interfaces.DerivativeGenerationStorageException
import parker.core.interfaces.DerivativeGenerationDiscovery
import parker.core.interfaces.DerivativeCompletenessState
import parker.core.interfaces.DerivativeOperationalOutcome
import parker.core.interfaces.DerivativeParentReference
import parker.core.interfaces.DerivativeProducerIdentity
import parker.core.interfaces.DerivativeTransformation
import parker.core.interfaces.DocumentIngestionAudit
import parker.core.interfaces.DocumentIngestionAuditRecord
import parker.core.interfaces.DocumentIngestionAuditException
import parker.core.interfaces.DocumentIngestionAuditStage
import parker.core.interfaces.DocxStructuralExtractionOutcome
import parker.core.interfaces.DocxStructuralExtractor
import parker.core.interfaces.DocxStructuralResult
import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.EmlAttachmentCandidate
import parker.core.interfaces.EmlExternalVerificationAdmissionOutcome
import parker.core.interfaces.EmlExternalVerificationReceipt
import parker.core.interfaces.EmlValidatedExternalVerificationAdmission
import parker.core.interfaces.EmlStructuralExtractionOutcome
import parker.core.interfaces.EmlStructuralExtractor
import parker.core.interfaces.EmlStructuralResult
import parker.core.interfaces.OcrDerivativeExtractedResult
import parker.core.interfaces.OcrDerivativeOutcomeKind
import parker.core.interfaces.OcrRecognitionResult
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.PdfStructuralExtractionOutcome
import parker.core.interfaces.PdfStructuralExtractor
import parker.core.interfaces.PdfStructuralResult
import parker.core.interfaces.TierADerivativePayload
import parker.core.interfaces.StructuredDocumentExtractor
import parker.core.interfaces.StructuredDocumentExtractionOutcome
import parker.core.interfaces.StructuredDocumentRepresentation

data class CsvIngestionSource(
    val evidenceArtifactId: EvidenceArtifactId,
    val content: ByteArray,
    val expectedSha256: String,
) {
    init {
        require(expectedSha256.matches(Regex("^[0-9a-f]{64}$"))) { "CsvIngestionSource.expectedSha256 must be a lowercase SHA-256 digest" }
    }
}

data class EmlIngestionSource(
    val evidenceArtifactId: EvidenceArtifactId,
    val content: ByteArray,
    val expectedSha256: String,
) {
    init { require(expectedSha256.matches(Regex("^[0-9a-f]{64}$"))) }
}

data class DocxIngestionSource(val evidenceArtifactId: EvidenceArtifactId, val content: ByteArray, val expectedSha256: String) {
    init { require(expectedSha256.matches(Regex("^[0-9a-f]{64}$"))) }
}

data class PdfIngestionSource(val evidenceArtifactId: EvidenceArtifactId, val content: ByteArray, val expectedSha256: String) {
    init { require(expectedSha256.matches(Regex("^[0-9a-f]{64}$"))) }
}

data class StructuredIngestionSource(val evidenceArtifactId: EvidenceArtifactId, val content: ByteArray, val expectedSha256: String, val mediaType: String, val fileName: String?) {
    init { require(expectedSha256.matches(Regex("^[0-9a-f]{64}$"))) }
}

sealed class StructuredDerivativeGenerationCoordinationOutcome {
    data class Admitted(val record: DerivativeGenerationRecord, val representation: StructuredDocumentRepresentation) : StructuredDerivativeGenerationCoordinationOutcome()
    data class ExtractionFailed(val reason: String) : StructuredDerivativeGenerationCoordinationOutcome()
    data class CapabilityUnavailable(val reason: String) : StructuredDerivativeGenerationCoordinationOutcome()
    data class SourceIntegrityFailed(val reason: String) : StructuredDerivativeGenerationCoordinationOutcome()
    data class PreparationFailed(val derivativeGenerationId: DerivativeGenerationId, val reason: String) : StructuredDerivativeGenerationCoordinationOutcome()
    data class AuthorisationAuditFailed(val derivativeGenerationId: DerivativeGenerationId, val reason: String) : StructuredDerivativeGenerationCoordinationOutcome()
    data class PublicationFailed(val derivativeGenerationId: DerivativeGenerationId, val reason: String) : StructuredDerivativeGenerationCoordinationOutcome()
}

sealed class PdfDerivativeGenerationCoordinationOutcome {
    data class Admitted(val record: DerivativeGenerationRecord, val pdfStructure: PdfStructuralResult) : PdfDerivativeGenerationCoordinationOutcome()
    data class RequiresTierB(val pageCount: Int?, val reason: String) : PdfDerivativeGenerationCoordinationOutcome()
    data class ExtractionFailed(val reason: String) : PdfDerivativeGenerationCoordinationOutcome()
    data class SourceIntegrityFailed(val reason: String) : PdfDerivativeGenerationCoordinationOutcome()
    data class PreparationFailed(val derivativeGenerationId: DerivativeGenerationId, val reason: String) : PdfDerivativeGenerationCoordinationOutcome()
    data class AuthorisationAuditFailed(val derivativeGenerationId: DerivativeGenerationId, val reason: String) : PdfDerivativeGenerationCoordinationOutcome()
    data class PublicationFailed(val derivativeGenerationId: DerivativeGenerationId, val reason: String) : PdfDerivativeGenerationCoordinationOutcome()
    data class AdmittedAuditFailed(val record: DerivativeGenerationRecord, val pdfStructure: PdfStructuralResult, val reason: String) : PdfDerivativeGenerationCoordinationOutcome()
}

sealed class DocxDerivativeGenerationCoordinationOutcome {
    data class Admitted(val record: DerivativeGenerationRecord, val docxStructure: DocxStructuralResult) : DocxDerivativeGenerationCoordinationOutcome()
    data class ExtractionFailed(val reason: String) : DocxDerivativeGenerationCoordinationOutcome()
    data class SourceIntegrityFailed(val reason: String) : DocxDerivativeGenerationCoordinationOutcome()
    data class PreparationFailed(val derivativeGenerationId: DerivativeGenerationId, val reason: String) : DocxDerivativeGenerationCoordinationOutcome()
    data class AuthorisationAuditFailed(val derivativeGenerationId: DerivativeGenerationId, val reason: String) : DocxDerivativeGenerationCoordinationOutcome()
    data class PublicationFailed(val derivativeGenerationId: DerivativeGenerationId, val reason: String) : DocxDerivativeGenerationCoordinationOutcome()
    data class AdmittedAuditFailed(val record: DerivativeGenerationRecord, val docxStructure: DocxStructuralResult, val reason: String) : DocxDerivativeGenerationCoordinationOutcome()
}

data class CandidateChildSource(
    val rootSourceEvidenceArtifactId: EvidenceArtifactId,
    val originatingMimeEntityId: String,
    val parentMimeEntityId: String?,
    val filename: String?,
    val declaredMimeType: String,
    val disposition: String?,
    val transferEncoding: String?,
    val charset: String?,
    val decodedBytes: ByteArray,
    val byteLength: Long,
    val sha256: String,
    val transformations: List<parker.core.interfaces.DerivativeTransformation>,
)

sealed class EmlDerivativeGenerationCoordinationOutcome {
    data class Admitted(val record: DerivativeGenerationRecord, val emlStructure: EmlStructuralResult,
        val childSourceCandidates: List<CandidateChildSource>) : EmlDerivativeGenerationCoordinationOutcome()
    data class ExtractionFailed(val reason: String) : EmlDerivativeGenerationCoordinationOutcome()
    data class SourceIntegrityFailed(val reason: String) : EmlDerivativeGenerationCoordinationOutcome()
    data class PreparationFailed(val derivativeGenerationId: DerivativeGenerationId, val reason: String) : EmlDerivativeGenerationCoordinationOutcome()
    data class AuthorisationAuditFailed(val derivativeGenerationId: DerivativeGenerationId, val reason: String) : EmlDerivativeGenerationCoordinationOutcome()
    data class PublicationFailed(val derivativeGenerationId: DerivativeGenerationId, val reason: String) : EmlDerivativeGenerationCoordinationOutcome()
    data class AdmittedAuditFailed(val record: DerivativeGenerationRecord, val emlStructure: EmlStructuralResult,
        val childSourceCandidates: List<CandidateChildSource>, val reason: String) : EmlDerivativeGenerationCoordinationOutcome()
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

/**
 * Document Ingestion — Tier B Durable OCR Derivative Content. The
 * generic-admission-side outcome [DerivativeGenerationCoordinator.ingestOcr]
 * returns -- mirrors [PdfDerivativeGenerationCoordinationOutcome]'s own
 * established shape, plus [MandatoryProvenanceUnavailable] for the Tier B
 * scope lock's own §11/§19 fail-closed gate, checked before any
 * [DerivativeGenerationId] is minted.
 */
sealed class OcrDerivativeGenerationCoordinationOutcome {
    data class Admitted(val record: DerivativeGenerationRecord, val extracted: OcrDerivativeExtractedResult) : OcrDerivativeGenerationCoordinationOutcome()
    data class MandatoryProvenanceUnavailable(val reason: String) : OcrDerivativeGenerationCoordinationOutcome()
    data class PreparationFailed(val derivativeGenerationId: DerivativeGenerationId, val reason: String) : OcrDerivativeGenerationCoordinationOutcome()
    data class AuthorisationAuditFailed(val derivativeGenerationId: DerivativeGenerationId, val reason: String) : OcrDerivativeGenerationCoordinationOutcome()
    data class PublicationFailed(val derivativeGenerationId: DerivativeGenerationId, val reason: String) : OcrDerivativeGenerationCoordinationOutcome()
    data class AdmittedAuditFailed(val record: DerivativeGenerationRecord, val extracted: OcrDerivativeExtractedResult, val reason: String) : OcrDerivativeGenerationCoordinationOutcome()
}

fun interface ValidatedExternalTranscriptionAdmission {
    suspend fun admit(
        evidenceArtifactId: EvidenceArtifactId,
        validation: OcrStructuredValidationOutcome.Validated,
        requestingPrincipalId: PrincipalId,
        correlationValue: String,
    ): OcrDerivativeGenerationCoordinationOutcome
}

class DerivativeGenerationCoordinator(
    private val csvExtractor: CsvStructuralExtractor,
    private val storage: DerivativeGenerationStorage,
    private val audit: DocumentIngestionAudit,
    private val idFactory: () -> DerivativeGenerationId = { DerivativeGenerationId(UUID.randomUUID().toString()) },
    private val now: () -> Instant = Instant::now,
    private val emlExtractor: EmlStructuralExtractor? = null,
    private val docxExtractor: DocxStructuralExtractor? = null,
    private val pdfExtractor: PdfStructuralExtractor? = null,
    private val structuredExtractor: StructuredDocumentExtractor? = null,
    // Document Ingestion — Derivative Content Persistence and Retrieval
    // (DOCUMENT_INGESTION_DERIVATIVE_CONTENT_PERSISTENCE_RETRIEVAL_SCOPE_LOCK.md §9): content is
    // published to durable storage BEFORE the DerivativeGenerationRecord is ever prepared, so a
    // generation is never reported admitted while its required content is absent. Nullable,
    // defaulted null, purely so every existing test constructing this coordinator without content
    // persistence in view keeps compiling unchanged; the real production composition always
    // supplies a real instance (TierADocumentIngestionComposition.create).
    private val contentStorage: DerivativeContentStorage? = null,
) : ValidatedExternalTranscriptionAdmission, EmlValidatedExternalVerificationAdmission {

    suspend fun ingestStructured(source: StructuredIngestionSource, requestingPrincipalId: PrincipalId, correlationValue: String): StructuredDerivativeGenerationCoordinationOutcome {
        require(correlationValue.isNotBlank())
        val extractor = requireNotNull(structuredExtractor) { "structured extractor is not configured" }
        if (sha256(source.content) != source.expectedSha256) return StructuredDerivativeGenerationCoordinationOutcome.SourceIntegrityFailed("Source SHA-256 does not match the governed source context")
        val representation = when (val outcome = extractor.extract(source.content.copyOf(), source.expectedSha256, source.mediaType, source.fileName)) {
            is StructuredDocumentExtractionOutcome.CapabilityUnavailable -> return StructuredDerivativeGenerationCoordinationOutcome.CapabilityUnavailable(outcome.reason)
            is StructuredDocumentExtractionOutcome.Malformed -> return StructuredDerivativeGenerationCoordinationOutcome.ExtractionFailed(outcome.reason)
            is StructuredDocumentExtractionOutcome.Extracted -> outcome.result
        }
        if (sha256(source.content) != source.expectedSha256) return StructuredDerivativeGenerationCoordinationOutcome.SourceIntegrityFailed("Source SHA-256 changed during structured extraction")
        val id = idFactory()
        val record = DerivativeGenerationRecord(id, source.evidenceArtifactId, listOf(DerivativeParentReference.RootEvidenceArtifact(source.evidenceArtifactId)),
            "${representation.kind.name} structured representation", DerivativeProducerIdentity(representation.parserIdentity, representation.parserVersion, "structured-representation-v1"),
            representation.transformations, now(), DerivativeContentIdentity.NoCanonicalSerialization, representation.completenessState, DerivativeOperationalOutcome.USABLE, representation.warnings)
        publishContentFirst(id, source.evidenceArtifactId, TierADerivativePayload.Structured(representation))?.let { return StructuredDerivativeGenerationCoordinationOutcome.PreparationFailed(id, it) }
        try { storage.prepare(record) } catch (e: DerivativeGenerationStorageException) { return StructuredDerivativeGenerationCoordinationOutcome.PreparationFailed(id, e.message ?: e::class.simpleName.orEmpty()) }
        try { audit.record(auditRecord(correlationValue, source.evidenceArtifactId, requestingPrincipalId, id, DocumentIngestionAuditStage.ADMISSION_AUTHORISED)) }
        catch (e: DocumentIngestionAuditException) { return StructuredDerivativeGenerationCoordinationOutcome.AuthorisationAuditFailed(id, e.message ?: e::class.simpleName.orEmpty()) }
        try { storage.publishPrepared(id) } catch (e: DerivativeGenerationStorageException) { return StructuredDerivativeGenerationCoordinationOutcome.PublicationFailed(id, e.message ?: e::class.simpleName.orEmpty()) }
        return try { audit.record(auditRecord(correlationValue, source.evidenceArtifactId, requestingPrincipalId, id, DocumentIngestionAuditStage.ADMITTED)); StructuredDerivativeGenerationCoordinationOutcome.Admitted(record, representation) }
        catch (e: DocumentIngestionAuditException) { StructuredDerivativeGenerationCoordinationOutcome.AuthorisationAuditFailed(id, e.message ?: e::class.simpleName.orEmpty()) }
    }
    /**
     * Publishes [payload]'s own durable content representation for [id],
     * strictly before [id]'s [DerivativeGenerationRecord] is ever prepared
     * (Scope Lock §9). Returns `null` on success (or when [contentStorage]
     * is absent, the test-only default above); a non-null String is the
     * honest failure reason, which each caller below wraps in its own
     * sealed outcome type's existing `PreparationFailed(id, reason)`
     * variant -- reused, not a new variant, since either failure means the
     * same truthful fact from the caller's own vantage point: nothing was
     * ever admitted.
     */
    private suspend fun publishContentFirst(
        id: DerivativeGenerationId,
        sourceEvidenceArtifactId: EvidenceArtifactId,
        payload: TierADerivativePayload,
    ): String? {
        val store = contentStorage ?: return null
        try {
            store.prepare(DerivativeContentEntry(id, sourceEvidenceArtifactId, payload))
        } catch (e: DerivativeContentStorageException) {
            return "content store prepare failed: ${e.message ?: e::class.simpleName.orEmpty()}"
        }
        try {
            store.publishPrepared(id)
        } catch (e: DerivativeContentStorageException) {
            return "content store publish failed: ${e.message ?: e::class.simpleName.orEmpty()}"
        }
        return null
    }
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
        publishContentFirst(id, source.evidenceArtifactId, TierADerivativePayload.Csv(extracted))?.let {
            return DerivativeGenerationCoordinationOutcome.PreparationFailed(id, it)
        }
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

    suspend fun ingestEml(
        source: EmlIngestionSource,
        requestingPrincipalId: PrincipalId,
        correlationValue: String,
    ): EmlDerivativeGenerationCoordinationOutcome {
        require(correlationValue.isNotBlank())
        val extractor = requireNotNull(emlExtractor) { "EML extractor is not configured" }
        if (sha256(source.content) != source.expectedSha256) {
            return EmlDerivativeGenerationCoordinationOutcome.SourceIntegrityFailed("Source SHA-256 does not match the governed source context")
        }
        val extracted = when (val outcome = extractor.extract(source.content.copyOf())) {
            is EmlStructuralExtractionOutcome.Malformed -> return EmlDerivativeGenerationCoordinationOutcome.ExtractionFailed(outcome.reason)
            is EmlStructuralExtractionOutcome.Extracted -> outcome.result
        }
        if (sha256(source.content) != source.expectedSha256) {
            return EmlDerivativeGenerationCoordinationOutcome.SourceIntegrityFailed("Source SHA-256 changed during EML extraction")
        }
        val id = idFactory()
        val record = DerivativeGenerationRecord(
            derivativeGenerationId = id,
            rootSourceEvidenceArtifactId = source.evidenceArtifactId,
            parents = listOf(DerivativeParentReference.RootEvidenceArtifact(source.evidenceArtifactId)),
            derivativeKind = "EML MIME structure",
            producerIdentity = extracted.producerIdentity,
            transformationHistory = extracted.transformationHistory,
            generatedAt = now(),
            contentIdentity = DerivativeContentIdentity.NoCanonicalSerialization,
            completenessState = extracted.completenessState,
            operationalOutcome = DerivativeOperationalOutcome.USABLE,
            warnings = extracted.warnings,
        )
        val candidates = extracted.attachmentCandidates.map { it.linkTo(source.evidenceArtifactId) }
        publishContentFirst(id, source.evidenceArtifactId, TierADerivativePayload.Eml(extracted, candidates.size))?.let {
            return EmlDerivativeGenerationCoordinationOutcome.PreparationFailed(id, it)
        }
        try { storage.prepare(record) } catch (e: DerivativeGenerationStorageException) {
            return EmlDerivativeGenerationCoordinationOutcome.PreparationFailed(id, e.message ?: e::class.simpleName.orEmpty())
        }
        try { audit.record(auditRecord(correlationValue, source.evidenceArtifactId, requestingPrincipalId, id, DocumentIngestionAuditStage.ADMISSION_AUTHORISED)) }
        catch (e: DocumentIngestionAuditException) {
            return EmlDerivativeGenerationCoordinationOutcome.AuthorisationAuditFailed(id, e.message ?: e::class.simpleName.orEmpty())
        }
        try { storage.publishPrepared(id) } catch (e: DerivativeGenerationStorageException) {
            return EmlDerivativeGenerationCoordinationOutcome.PublicationFailed(id, e.message ?: e::class.simpleName.orEmpty())
        }
        try { audit.record(auditRecord(correlationValue, source.evidenceArtifactId, requestingPrincipalId, id, DocumentIngestionAuditStage.ADMITTED)) }
        catch (e: DocumentIngestionAuditException) {
            return EmlDerivativeGenerationCoordinationOutcome.AdmittedAuditFailed(record, extracted, candidates, e.message ?: e::class.simpleName.orEmpty())
        }
        return EmlDerivativeGenerationCoordinationOutcome.Admitted(record, extracted, candidates)
    }

    suspend fun ingestDocx(source: DocxIngestionSource, requestingPrincipalId: PrincipalId, correlationValue: String): DocxDerivativeGenerationCoordinationOutcome {
        require(correlationValue.isNotBlank())
        val extractor = requireNotNull(docxExtractor) { "DOCX extractor is not configured" }
        if (sha256(source.content) != source.expectedSha256) return DocxDerivativeGenerationCoordinationOutcome.SourceIntegrityFailed("Source SHA-256 does not match the governed source context")
        val extracted = when (val outcome = extractor.extract(source.content.copyOf())) {
            is DocxStructuralExtractionOutcome.Malformed -> return DocxDerivativeGenerationCoordinationOutcome.ExtractionFailed(outcome.reason)
            is DocxStructuralExtractionOutcome.Extracted -> outcome.result
        }
        if (sha256(source.content) != source.expectedSha256) return DocxDerivativeGenerationCoordinationOutcome.SourceIntegrityFailed("Source SHA-256 changed during DOCX extraction")
        val id = idFactory()
        val record = DerivativeGenerationRecord(
            id, source.evidenceArtifactId, listOf(DerivativeParentReference.RootEvidenceArtifact(source.evidenceArtifactId)),
            "DOCX OOXML structure", extracted.producerIdentity, extracted.transformationHistory, now(),
            DerivativeContentIdentity.NoCanonicalSerialization, extracted.completenessState,
            DerivativeOperationalOutcome.USABLE, extracted.warnings,
        )
        publishContentFirst(id, source.evidenceArtifactId, TierADerivativePayload.Docx(extracted))?.let {
            return DocxDerivativeGenerationCoordinationOutcome.PreparationFailed(id, it)
        }
        try { storage.prepare(record) } catch (e: DerivativeGenerationStorageException) {
            return DocxDerivativeGenerationCoordinationOutcome.PreparationFailed(id, e.message ?: e::class.simpleName.orEmpty())
        }
        try { audit.record(auditRecord(correlationValue, source.evidenceArtifactId, requestingPrincipalId, id, DocumentIngestionAuditStage.ADMISSION_AUTHORISED)) }
        catch (e: DocumentIngestionAuditException) { return DocxDerivativeGenerationCoordinationOutcome.AuthorisationAuditFailed(id, e.message ?: e::class.simpleName.orEmpty()) }
        try { storage.publishPrepared(id) } catch (e: DerivativeGenerationStorageException) {
            return DocxDerivativeGenerationCoordinationOutcome.PublicationFailed(id, e.message ?: e::class.simpleName.orEmpty())
        }
        try { audit.record(auditRecord(correlationValue, source.evidenceArtifactId, requestingPrincipalId, id, DocumentIngestionAuditStage.ADMITTED)) }
        catch (e: DocumentIngestionAuditException) { return DocxDerivativeGenerationCoordinationOutcome.AdmittedAuditFailed(record, extracted, e.message ?: e::class.simpleName.orEmpty()) }
        return DocxDerivativeGenerationCoordinationOutcome.Admitted(record, extracted)
    }

    suspend fun ingestPdf(source: PdfIngestionSource, requestingPrincipalId: PrincipalId, correlationValue: String): PdfDerivativeGenerationCoordinationOutcome {
        require(correlationValue.isNotBlank())
        val extractor = requireNotNull(pdfExtractor) { "PDF extractor is not configured" }
        if (sha256(source.content) != source.expectedSha256) return PdfDerivativeGenerationCoordinationOutcome.SourceIntegrityFailed("Source SHA-256 does not match the governed source context")
        val extracted = when (val outcome = extractor.extract(source.content.copyOf())) {
            is PdfStructuralExtractionOutcome.RequiresTierB -> return PdfDerivativeGenerationCoordinationOutcome.RequiresTierB(outcome.pageCount, outcome.reason)
            is PdfStructuralExtractionOutcome.Malformed -> return PdfDerivativeGenerationCoordinationOutcome.ExtractionFailed(outcome.reason)
            is PdfStructuralExtractionOutcome.Unsupported -> return PdfDerivativeGenerationCoordinationOutcome.ExtractionFailed(outcome.reason)
            is PdfStructuralExtractionOutcome.Extracted -> outcome.result
        }
        if (sha256(source.content) != source.expectedSha256) return PdfDerivativeGenerationCoordinationOutcome.SourceIntegrityFailed("Source SHA-256 changed during PDF extraction")
        findEquivalentPdf(source, extracted)?.let { existing ->
            return PdfDerivativeGenerationCoordinationOutcome.Admitted(existing.first, existing.second)
        }
        val idempotentPdfPersistence = storage is DerivativeGenerationDiscovery && contentStorage != null
        val id = if (idempotentPdfPersistence) stablePdfGenerationId(source, extracted) else idFactory()
        val boundExtracted = extracted.copy(pageTextSegments = extracted.pageTextSegments.map { segment ->
            require(segment.sourceSha256 == source.expectedSha256) { "PDF page segment source digest does not match the governed source" }
            segment.copy(derivativeGenerationId = id)
        })
        val record = DerivativeGenerationRecord(
            id, source.evidenceArtifactId, listOf(DerivativeParentReference.RootEvidenceArtifact(source.evidenceArtifactId)),
            "Searchable PDF literal text", boundExtracted.producerIdentity, boundExtracted.transformationHistory, now(),
            DerivativeContentIdentity.NoCanonicalSerialization, boundExtracted.completenessState,
            DerivativeOperationalOutcome.USABLE, boundExtracted.warnings,
        )
        val payload = TierADerivativePayload.Pdf(boundExtracted)
        publishContentFirst(id, source.evidenceArtifactId, payload)?.let { failure ->
            // A deterministic id makes the content-first sequence restart-safe.  If a
            // previous process published content and stopped before the record, verify the
            // exact payload and continue with the record; never accept a mismatched payload.
            if (idempotentPdfPersistence) {
                existingPdfById(id, source.evidenceArtifactId, payload)?.let { existing ->
                    return PdfDerivativeGenerationCoordinationOutcome.Admitted(existing.first, existing.second)
                }
            }
            if (!idempotentPdfPersistence || !contentMatches(id, source.evidenceArtifactId, payload)) {
                findEquivalentPdf(source, extracted)?.let { existing ->
                    return PdfDerivativeGenerationCoordinationOutcome.Admitted(existing.first, existing.second)
                }
                return PdfDerivativeGenerationCoordinationOutcome.PreparationFailed(id, failure)
            }
        }
        try { storage.prepare(record) } catch (e: DerivativeGenerationStorageException) { return PdfDerivativeGenerationCoordinationOutcome.PreparationFailed(id, e.message ?: e::class.simpleName.orEmpty()) }
        try { audit.record(auditRecord(correlationValue, source.evidenceArtifactId, requestingPrincipalId, id, DocumentIngestionAuditStage.ADMISSION_AUTHORISED)) }
        catch (e: DocumentIngestionAuditException) { return PdfDerivativeGenerationCoordinationOutcome.AuthorisationAuditFailed(id, e.message ?: e::class.simpleName.orEmpty()) }
        try { storage.publishPrepared(id) } catch (e: DerivativeGenerationStorageException) { return PdfDerivativeGenerationCoordinationOutcome.PublicationFailed(id, e.message ?: e::class.simpleName.orEmpty()) }
        try { audit.record(auditRecord(correlationValue, source.evidenceArtifactId, requestingPrincipalId, id, DocumentIngestionAuditStage.ADMITTED)) }
        catch (e: DocumentIngestionAuditException) { return PdfDerivativeGenerationCoordinationOutcome.AdmittedAuditFailed(record, boundExtracted, e.message ?: e::class.simpleName.orEmpty()) }
        return PdfDerivativeGenerationCoordinationOutcome.Admitted(record, boundExtracted)
    }

    private suspend fun findEquivalentPdf(
        source: PdfIngestionSource,
        extracted: PdfStructuralResult,
    ): Pair<DerivativeGenerationRecord, PdfStructuralResult>? {
        val discovery = storage as? DerivativeGenerationDiscovery ?: return null
        val contents = contentStorage ?: return null
        val records = try {
            discovery.findGenerationsForEvidence(source.evidenceArtifactId)
        } catch (_: Exception) {
            return null
        }
        for (record in records) {
            if (record.derivativeKind != "Searchable PDF literal text" ||
                record.operationalOutcome != DerivativeOperationalOutcome.USABLE ||
                record.producerIdentity != extracted.producerIdentity ||
                record.transformationHistory != extracted.transformationHistory ||
                record.completenessState != extracted.completenessState ||
                record.warnings != extracted.warnings
            ) continue
            val entry = try { contents.retrieve(record.derivativeGenerationId) } catch (_: Exception) { null } ?: continue
            if (entry.rootSourceEvidenceArtifactId != source.evidenceArtifactId) continue
            val pdf = (entry.payload as? TierADerivativePayload.Pdf)?.value ?: continue
            // Historical whole-document PDFs did not persist a source digest in a page
            // segment.  Do not collapse those records against a new source merely because
            // their extracted text happens to compare equal: source identity must remain
            // explicit for idempotent reuse.
            if (pdf.pageTextSegments.isEmpty() || pdf.pageTextSegments.any { it.sourceSha256 != source.expectedSha256 }) continue
            if (normalisePdf(pdf) == normalisePdf(extracted)) return record to pdf
        }
        return null
    }

    private suspend fun contentMatches(
        id: DerivativeGenerationId,
        evidenceArtifactId: EvidenceArtifactId,
        payload: TierADerivativePayload,
    ): Boolean {
        val entry = try { contentStorage?.retrieve(id) } catch (_: Exception) { null } ?: return false
        return entry.rootSourceEvidenceArtifactId == evidenceArtifactId && entry.payload == payload
    }

    private suspend fun existingPdfById(
        id: DerivativeGenerationId,
        evidenceArtifactId: EvidenceArtifactId,
        expectedPayload: TierADerivativePayload,
    ): Pair<DerivativeGenerationRecord, PdfStructuralResult>? {
        val record = try { storage.retrieve(id) } catch (_: Exception) { null } ?: return null
        if (record.rootSourceEvidenceArtifactId != evidenceArtifactId || record.derivativeKind != "Searchable PDF literal text") return null
        val entry = try { contentStorage?.retrieve(id) } catch (_: Exception) { null } ?: return null
        if (entry.rootSourceEvidenceArtifactId != evidenceArtifactId || entry.payload != expectedPayload) return null
        val pdf = (entry.payload as? TierADerivativePayload.Pdf)?.value ?: return null
        return record to pdf
    }

    private fun stablePdfGenerationId(source: PdfIngestionSource, extracted: PdfStructuralResult): DerivativeGenerationId {
        val fingerprint = canonicalPdfFingerprint(source, extracted)
        val digest = MessageDigest.getInstance("SHA-256").digest(fingerprint.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        return DerivativeGenerationId("pdf-native-$digest")
    }

    /** Versioned, field-ordered identity input; never rely on data-class toString(). */
    private fun canonicalPdfFingerprint(source: PdfIngestionSource, result: PdfStructuralResult): String = buildString {
        appendField("version", "pdf-native-fingerprint-v1")
        appendField("evidence", source.evidenceArtifactId.value)
        appendField("sourceSha256", source.expectedSha256)
        appendField("documentText", result.documentText)
        appendField("pageCount", result.pageCount?.toString())
        appendField("pageTextAssociationAvailable", result.pageTextAssociationAvailable.toString())
        appendField("metadata.count", result.metadata.size.toString())
        result.metadata.forEachIndexed { index, value ->
            appendField("metadata[$index].name", value.name)
            appendField("metadata[$index].value", value.value)
            appendField("metadata[$index].representation", value.representation)
        }
        appendField("embeddedResources.count", result.embeddedResources.size.toString())
        result.embeddedResources.forEachIndexed { index, value ->
            appendField("embeddedResources[$index].fileName", value.declaredFileName)
            appendField("embeddedResources[$index].mediaType", value.declaredMediaType)
        }
        appendField("producer.pluginIdentity", result.producerIdentity.pluginIdentity)
        appendField("producer.pluginVersion", result.producerIdentity.pluginVersion)
        appendField("producer.configurationIdentity", result.producerIdentity.configurationIdentity)
        appendField("producer.adapterIdentity", result.producerIdentity.adapterIdentity)
        appendField("producer.adapterVersion", result.producerIdentity.adapterVersion)
        appendField("producer.modelIdentity", result.producerIdentity.modelIdentity)
        appendField("producer.modelVersion", result.producerIdentity.modelVersion)
        appendField("transformations.count", result.transformationHistory.size.toString())
        result.transformationHistory.forEachIndexed { index, value -> appendField("transformations[$index]", value.name) }
        appendField("completeness", result.completenessState.name)
        appendField("warnings.count", result.warnings.size.toString())
        result.warnings.forEachIndexed { index, value -> appendField("warnings[$index]", value) }
        appendField("pageSegments.count", result.pageTextSegments.size.toString())
        result.pageTextSegments.forEachIndexed { index, segment ->
            appendField("pageSegments[$index].pageNumber", segment.pageNumber.toString())
            appendField("pageSegments[$index].text", segment.text)
            appendField("pageSegments[$index].startOffset", segment.startOffset.toString())
            appendField("pageSegments[$index].endOffset", segment.endOffset.toString())
            appendField("pageSegments[$index].sectionHeading", segment.sectionHeading)
            appendField("pageSegments[$index].region.left", segment.region?.left?.toString())
            appendField("pageSegments[$index].region.top", segment.region?.top?.toString())
            appendField("pageSegments[$index].region.right", segment.region?.right?.toString())
            appendField("pageSegments[$index].region.bottom", segment.region?.bottom?.toString())
            appendField("pageSegments[$index].extractionMethod", segment.extractionMethod)
            appendField("pageSegments[$index].confidence", segment.confidence?.toString())
            appendField("pageSegments[$index].sourceSha256", segment.sourceSha256)
        }
    }

    private fun StringBuilder.appendField(name: String, value: String?) {
        val encoded = value ?: "<null>"
        append(name).append(':').append(encoded.toByteArray(Charsets.UTF_8).size).append(':').append(encoded).append(';')
    }

    private fun normalisePdf(result: PdfStructuralResult): PdfStructuralResult = result.copy(
        pageTextSegments = result.pageTextSegments.map { it.copy(derivativeGenerationId = null) },
    )

    /**
     * Document Ingestion — Tier B Durable OCR Derivative Content Scope
     * Lock §9/§11/§19. Called only after: (a) the caller has already
     * evaluated Permission Engine authorisation for this invocation, (b)
     * [result] is an already-completed OCR execution's own truthful
     * output ([outcomeKind]/[degradationReason] classify which of the two
     * admissible outcomes it represents, §13) -- this method never
     * invokes OCR itself. Performs the mandatory-provenance check (§11)
     * *before* minting any [DerivativeGenerationId] -- fabricating a
     * placeholder value to satisfy a missing field is never authorised;
     * absence fails closed instead.
     */
    suspend fun ingestOcr(
        evidenceArtifactId: EvidenceArtifactId,
        result: OcrRecognitionResult,
        outcomeKind: OcrDerivativeOutcomeKind,
        degradationReason: String?,
        requestingPrincipalId: PrincipalId,
        correlationValue: String,
        operationalOutcome: DerivativeOperationalOutcome = DerivativeOperationalOutcome.USABLE,
        persistProcessingProvenance: Boolean = false,
    ): OcrDerivativeGenerationCoordinationOutcome {
        require(correlationValue.isNotBlank()) { "correlationValue must not be blank" }

        val identity = result.identity
        val mechanismVersion = identity.mechanismVersion
        val modelIdentity = identity.modelIdentity
        val modelVersion = identity.modelVersion
        if (mechanismVersion == null || modelIdentity == null || modelVersion == null) {
            return OcrDerivativeGenerationCoordinationOutcome.MandatoryProvenanceUnavailable(
                "OCR recognition identity does not truthfully carry every field the Derivative Generation Record " +
                    "requires as mandatory for Tier B (mechanismVersion/modelIdentity/modelVersion) -- durable " +
                    "admission fails closed rather than fabricating a placeholder value",
            )
        }

        val producerIdentity = DerivativeProducerIdentity(
            pluginIdentity = identity.mechanismIdentity,
            pluginVersion = mechanismVersion,
            configurationIdentity = identity.configurationProfile,
            modelIdentity = modelIdentity,
            modelVersion = modelVersion,
        )
        val transformationHistory = listOf(DerivativeTransformation.OCR, DerivativeTransformation.MODEL_INFERENCE)
        // Tier B scope lock §14: RECOGNISED and PARTIAL_OR_DEGRADED both map to
        // AccountedForWithQualifications today -- AccountedFor requires governed coverage
        // evidence no current OCR result field provides; never assigned by this method.
        val completenessState = DerivativeCompletenessState.ACCOUNTED_FOR_WITH_QUALIFICATIONS
        val extracted = OcrDerivativeExtractedResult(
            recognisedText = result.recognisedText,
            fidelity = result.fidelity,
            outcomeKind = outcomeKind,
            degradationReason = degradationReason,
            warnings = result.warnings,
            segments = result.segments,
            producerIdentity = producerIdentity,
            transformationHistory = transformationHistory,
            completenessState = completenessState,
            pageAccounting = result.pageAccounting.takeIf { persistProcessingProvenance },
            processingProvenance = result.processingProvenance.takeIf { persistProcessingProvenance },
            providerProvenance = result.providerProvenance.takeIf { persistProcessingProvenance },
            recognisedAt = result.recognisedAt.takeIf { persistProcessingProvenance },
            authority = OcrAuthorityPolicy.classify(result),
        )

        val id = idFactory()
        // The degradation reason is carried alongside the generation's own warnings on the
        // Record (Tier B scope lock §14) -- the Record shape itself has no separate field;
        // OcrDerivativeExtractedResult.degradationReason (above) is the distinct, structured
        // fact the durable content payload itself preserves.
        val recordWarnings = if (degradationReason != null) result.warnings + degradationReason else result.warnings
        val record = DerivativeGenerationRecord(
            derivativeGenerationId = id,
            rootSourceEvidenceArtifactId = evidenceArtifactId,
            parents = listOf(DerivativeParentReference.RootEvidenceArtifact(evidenceArtifactId)),
            derivativeKind = "OCR recognised text",
            producerIdentity = producerIdentity,
            transformationHistory = transformationHistory,
            generatedAt = result.recognisedAt,
            contentIdentity = DerivativeContentIdentity.NoCanonicalSerialization,
            completenessState = completenessState,
            operationalOutcome = operationalOutcome,
            warnings = recordWarnings,
            confidence = result.confidence,
        )
        publishContentFirst(id, evidenceArtifactId, TierADerivativePayload.Ocr(extracted))?.let {
            return OcrDerivativeGenerationCoordinationOutcome.PreparationFailed(id, it)
        }
        try {
            storage.prepare(record)
        } catch (e: DerivativeGenerationStorageException) {
            return OcrDerivativeGenerationCoordinationOutcome.PreparationFailed(id, e.message ?: e::class.simpleName.orEmpty())
        }
        try {
            audit.record(auditRecord(correlationValue, evidenceArtifactId, requestingPrincipalId, id, DocumentIngestionAuditStage.ADMISSION_AUTHORISED))
        } catch (e: DocumentIngestionAuditException) {
            return OcrDerivativeGenerationCoordinationOutcome.AuthorisationAuditFailed(id, e.message ?: e::class.simpleName.orEmpty())
        }
        try {
            storage.publishPrepared(id)
        } catch (e: DerivativeGenerationStorageException) {
            return OcrDerivativeGenerationCoordinationOutcome.PublicationFailed(id, e.message ?: e::class.simpleName.orEmpty())
        }
        try {
            audit.record(auditRecord(correlationValue, evidenceArtifactId, requestingPrincipalId, id, DocumentIngestionAuditStage.ADMITTED))
        } catch (e: DocumentIngestionAuditException) {
            return OcrDerivativeGenerationCoordinationOutcome.AdmittedAuditFailed(record, extracted, e.message ?: e::class.simpleName.orEmpty())
        }
        return OcrDerivativeGenerationCoordinationOutcome.Admitted(record, extracted)
    }

    /** Admits only a Unit C validated external result carrying complete Unit B/I provenance. */
    override suspend fun admit(
        evidenceArtifactId: EvidenceArtifactId,
        validation: OcrStructuredValidationOutcome.Validated,
        requestingPrincipalId: PrincipalId,
        correlationValue: String,
    ): OcrDerivativeGenerationCoordinationOutcome {
        require(correlationValue.isNotBlank())
        val resultAndKind = when (val outcome = validation.outcome) {
            is OcrRecognitionOutcome.Recognised -> Triple(outcome.result, OcrDerivativeOutcomeKind.RECOGNISED, null as String?)
            is OcrRecognitionOutcome.PartialOrDegradedOutput -> Triple(outcome.partialResult, OcrDerivativeOutcomeKind.PARTIAL_OR_DEGRADED, outcome.reason)
            else -> return OcrDerivativeGenerationCoordinationOutcome.MandatoryProvenanceUnavailable("Validated external result is not durably admissible")
        }
        val (result, outcomeKind, degradationReason) = resultAndKind
        val processing = result.processingProvenance
            ?: return OcrDerivativeGenerationCoordinationOutcome.MandatoryProvenanceUnavailable("Processing provenance is mandatory")
        val provider = result.providerProvenance
            ?: return OcrDerivativeGenerationCoordinationOutcome.MandatoryProvenanceUnavailable("Provider provenance is mandatory")
        val accounting = result.pageAccounting
            ?: return OcrDerivativeGenerationCoordinationOutcome.MandatoryProvenanceUnavailable("Page accounting is mandatory")
        val requested = accounting.requestedScope.pageNumbers.toSet()
        val submitted = accounting.submittedScope.pageNumbers.toSet()
        val returned = accounting.returnedScope.pageNumbers.toSet()
        val outcomePages = accounting.pageOutcomes.map { it.pageNumber }
        if (processing.sourceEvidenceArtifactId != evidenceArtifactId || accounting != validation.pageAccounting ||
            requested.isEmpty() || !requested.containsAll(submitted) || !submitted.containsAll(returned) ||
            outcomePages.size != outcomePages.toSet().size || outcomePages.toSet() != requested ||
            accounting.pageOutcomes.any { page -> page.uncertaintySpans.any { it.pageNumber != page.pageNumber } } ||
            processing.requestedPageScope?.let { it != accounting.requestedScope } == true ||
            processing.submittedPageScope?.let { it != accounting.submittedScope } == true ||
            processing.byteExactCopy && (processing.materialTransformation != null ||
                processing.sourceManifestSha256 != processing.representationSha256 ||
                processing.sourceByteLength != processing.representationByteLength ||
                processing.sourceMediaType != processing.representationMediaType) ||
            provider.providerReportedModelIdentifier.isBlank() || provider.providerCorrelationIdentifier.isBlank()
        ) return OcrDerivativeGenerationCoordinationOutcome.MandatoryProvenanceUnavailable("Validated external provenance contradicts mandatory admission facts")

        val producer = DerivativeProducerIdentity(
            pluginIdentity = result.identity.mechanismIdentity,
            pluginVersion = requireNotNull(result.identity.mechanismVersion),
            configurationIdentity = result.identity.configurationProfile,
            adapterIdentity = provider.adapterIdentity,
            adapterVersion = provider.adapterVersion,
            modelIdentity = provider.providerReportedModelIdentifier,
            modelVersion = (provider.modelSnapshot as? OcrModelSnapshot.Present)?.value,
        )
        val transformations = listOf(DerivativeTransformation.OCR, DerivativeTransformation.MODEL_INFERENCE)
        val extracted = OcrDerivativeExtractedResult(
            result.recognisedText, result.fidelity, outcomeKind, degradationReason, result.warnings, result.segments,
            producer, transformations, validation.completenessState, accounting, processing, provider, result.recognisedAt,
            OcrAuthorityClassification.EXTERNAL_AUTHORITATIVE,
        )
        val id = idFactory()
        val recordWarnings = if (degradationReason == null) result.warnings else result.warnings + degradationReason
        val record = DerivativeGenerationRecord(
            id, evidenceArtifactId, listOf(DerivativeParentReference.RootEvidenceArtifact(evidenceArtifactId)),
            "External transcription recognised text", producer, transformations, result.recognisedAt,
            DerivativeContentIdentity.NoCanonicalSerialization, validation.completenessState,
            DerivativeOperationalOutcome.USABLE, recordWarnings,
        )
        publishContentFirst(id, evidenceArtifactId, TierADerivativePayload.Ocr(extracted))?.let {
            return OcrDerivativeGenerationCoordinationOutcome.PreparationFailed(id, it)
        }
        try { storage.prepare(record) } catch (e: DerivativeGenerationStorageException) {
            return OcrDerivativeGenerationCoordinationOutcome.PreparationFailed(id, e.message ?: e::class.simpleName.orEmpty())
        }
        try { audit.record(auditRecord(correlationValue, evidenceArtifactId, requestingPrincipalId, id, DocumentIngestionAuditStage.ADMISSION_AUTHORISED)) }
        catch (e: DocumentIngestionAuditException) { return OcrDerivativeGenerationCoordinationOutcome.AuthorisationAuditFailed(id, e.message ?: e::class.simpleName.orEmpty()) }
        try { storage.publishPrepared(id) } catch (e: DerivativeGenerationStorageException) {
            return OcrDerivativeGenerationCoordinationOutcome.PublicationFailed(id, e.message ?: e::class.simpleName.orEmpty())
        }
        try { audit.record(auditRecord(correlationValue, evidenceArtifactId, requestingPrincipalId, id, DocumentIngestionAuditStage.ADMITTED)) }
        catch (e: DocumentIngestionAuditException) { return OcrDerivativeGenerationCoordinationOutcome.AdmittedAuditFailed(record, extracted, e.message ?: e::class.simpleName.orEmpty()) }
        return OcrDerivativeGenerationCoordinationOutcome.Admitted(record, extracted)
    }

    /**
     * Admits a validated, non-paginated EML external-verification receipt
     * (STEP 4G, FIDELITY_PRESERVING_EVIDENCE_ACQUISITION_SCOPE_LOCK.md §6.1). Reuses exactly the
     * same [storage]/[audit]/[publishContentFirst] primitives [admit] and [ingestEml] already use
     * -- no parallel persistence mechanism. The durable content payload reuses the existing
     * `TierADerivativePayload.Eml` shape: the retrievable content (the deterministic
     * [parker.core.interfaces.EmlStructuralResult]) is identical regardless of which mechanism
     * (native Tier A extraction or governed external verification) produced this generation;
     * [DerivativeGenerationRecord.derivativeKind]/`producerIdentity`/`transformationHistory`
     * remain the actual mechanism discriminator, exactly as this file's own existing convention
     * already establishes for OCR (`TierADerivativePayload.Ocr` reused for both local and
     * external OCR).
     */
    override suspend fun admitEmlExternalVerification(
        evidenceArtifactId: EvidenceArtifactId,
        structuralResult: EmlStructuralResult,
        receipt: EmlExternalVerificationReceipt,
        requestingPrincipalId: PrincipalId,
        correlationValue: String,
    ): EmlExternalVerificationAdmissionOutcome {
        require(correlationValue.isNotBlank())
        if (receipt.sourceEvidenceArtifactId != evidenceArtifactId) {
            return EmlExternalVerificationAdmissionOutcome.MandatoryProvenanceUnavailable("Validated receipt does not match the requested evidence identity")
        }
        val id = idFactory()
        val record = DerivativeGenerationRecord(
            derivativeGenerationId = id,
            rootSourceEvidenceArtifactId = evidenceArtifactId,
            parents = listOf(DerivativeParentReference.RootEvidenceArtifact(evidenceArtifactId)),
            derivativeKind = "EML external verification receipt",
            producerIdentity = receipt.producerIdentity,
            transformationHistory = listOf(DerivativeTransformation.MODEL_INFERENCE, DerivativeTransformation.STRUCTURAL_PARSING),
            generatedAt = receipt.recognisedAt,
            contentIdentity = DerivativeContentIdentity.NoCanonicalSerialization,
            completenessState = receipt.completenessState,
            operationalOutcome = DerivativeOperationalOutcome.USABLE,
            warnings = receipt.warnings,
        )
        publishContentFirst(id, evidenceArtifactId, TierADerivativePayload.Eml(structuralResult, 0))?.let {
            return EmlExternalVerificationAdmissionOutcome.PreparationFailed(id, it)
        }
        try { storage.prepare(record) } catch (e: DerivativeGenerationStorageException) {
            return EmlExternalVerificationAdmissionOutcome.PreparationFailed(id, e.message ?: e::class.simpleName.orEmpty())
        }
        try { audit.record(auditRecord(correlationValue, evidenceArtifactId, requestingPrincipalId, id, DocumentIngestionAuditStage.ADMISSION_AUTHORISED)) }
        catch (e: DocumentIngestionAuditException) { return EmlExternalVerificationAdmissionOutcome.AuthorisationAuditFailed(id, e.message ?: e::class.simpleName.orEmpty()) }
        try { storage.publishPrepared(id) } catch (e: DerivativeGenerationStorageException) {
            return EmlExternalVerificationAdmissionOutcome.PublicationFailed(id, e.message ?: e::class.simpleName.orEmpty())
        }
        try { audit.record(auditRecord(correlationValue, evidenceArtifactId, requestingPrincipalId, id, DocumentIngestionAuditStage.ADMITTED)) }
        catch (e: DocumentIngestionAuditException) { return EmlExternalVerificationAdmissionOutcome.AdmittedAuditFailed(record, receipt, e.message ?: e::class.simpleName.orEmpty()) }
        return EmlExternalVerificationAdmissionOutcome.Admitted(record, receipt)
    }

    private fun EmlAttachmentCandidate.linkTo(root: EvidenceArtifactId) = CandidateChildSource(
        root, mimeEntityId, parentMimeEntityId, filename, declaredMimeType, disposition, transferEncoding,
        charset, decodedBytes.copyOf(), byteLength, sha256, transformations,
    )

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
