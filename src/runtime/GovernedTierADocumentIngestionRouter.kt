package parker.core.runtime

import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipInputStream
import parker.core.interfaces.*

class GovernedTierADocumentIngestionRouter internal constructor(
    private val coordinator: TierAFormatRoutes,
) : TierADocumentIngestionRouter {
    override suspend fun ingest(source: TierADocumentSourceContext): TierADocumentRoutingResult {
        val detected = detect(source.content)
        val received = source.receivedMediaType?.substringBefore(';')?.trim()?.lowercase()?.takeIf { it.isNotBlank() }
        val facts = TierAMediaFacts(source.receivedMediaType, detected, source.originalFileName,
            received != null && detected != null && received != detected)
        val selected = route(detected ?: received)
        if (selected == null) {
            return if (detected == PNG || received == PNG || received?.startsWith("image/") == true) {
                TierADocumentRoutingResult.RequiresTierB("Tier A textual ingestion cannot process image content; separately authorized Tier B OCR is required", facts)
            } else TierADocumentRoutingResult.Unsupported("No governed Tier A route exists for detected/received media type", facts)
        }
        return when (selected) {
            TierADocumentFormat.CSV -> mapCsv(coordinator.ingestCsv(CsvIngestionSource(source.evidenceArtifactId, source.content, source.expectedSha256), source.requestingPrincipalId, source.correlationValue), facts)
            TierADocumentFormat.EML -> mapEml(coordinator.ingestEml(EmlIngestionSource(source.evidenceArtifactId, source.content, source.expectedSha256), source.requestingPrincipalId, source.correlationValue), facts)
            TierADocumentFormat.DOCX -> mapDocx(coordinator.ingestDocx(DocxIngestionSource(source.evidenceArtifactId, source.content, source.expectedSha256), source.requestingPrincipalId, source.correlationValue), facts)
            TierADocumentFormat.TXT, TierADocumentFormat.DOC, TierADocumentFormat.XLS, TierADocumentFormat.XLSX, TierADocumentFormat.MSG, TierADocumentFormat.RTF -> mapStructured(selected, coordinator.ingestStructured(StructuredIngestionSource(source.evidenceArtifactId, source.content, source.expectedSha256, detected ?: received!!, source.originalFileName), source.requestingPrincipalId, source.correlationValue), facts)
            TierADocumentFormat.PDF -> mapPdf(coordinator.ingestPdf(PdfIngestionSource(source.evidenceArtifactId, source.content, source.expectedSha256), source.requestingPrincipalId, source.correlationValue), facts)
        }
    }

    private fun route(mediaType: String?) = when (mediaType) {
        CSV -> TierADocumentFormat.CSV
        EML -> TierADocumentFormat.EML
        DOCX -> TierADocumentFormat.DOCX
        TXT -> TierADocumentFormat.TXT
        DOC -> TierADocumentFormat.DOC
        XLS -> TierADocumentFormat.XLS
        XLSX -> TierADocumentFormat.XLSX
        MSG, MSG_OLE -> TierADocumentFormat.MSG
        RTF, RTF_TEXT, RTF_X -> TierADocumentFormat.RTF
        PDF -> TierADocumentFormat.PDF
        else -> null
    }

    private fun detect(bytes: ByteArray): String? {
        if (bytes.size >= 5 && String(bytes, 0, 5, StandardCharsets.ISO_8859_1) == "%PDF-") return PDF
        if (bytes.size >= 5 && String(bytes, 0, 5, StandardCharsets.ISO_8859_1) == "{\\rtf") return RTF
        if (bytes.size >= PNG_SIGNATURE.size && bytes.copyOfRange(0, PNG_SIGNATURE.size).contentEquals(PNG_SIGNATURE)) return PNG
        if (bytes.size >= 4 && bytes[0] == 'P'.code.toByte() && bytes[1] == 'K'.code.toByte()) {
            var entries = 0; var contentTypes = false; var document = false
            try {
                ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
                    while (entries++ < MAX_DETECTION_ZIP_ENTRIES) {
                        val entry = zip.nextEntry ?: break
                        if (entry.name == "[Content_Types].xml") contentTypes = true
                        if (entry.name == "word/document.xml") document = true
                        if (contentTypes && document) return DOCX
                    }
                }
            } catch (_: Exception) { return null }
        }
        val prefix = String(bytes, 0, minOf(bytes.size, MAX_HEADER_DETECTION_BYTES), StandardCharsets.ISO_8859_1)
        val emlHeaders = Regex("(?im)^(From|To|Date|Subject|Message-ID|MIME-Version|Content-Type):\\s*.+$")
            .findAll(prefix).map { it.groupValues[1].lowercase() }.toSet()
        if ("mime-version" in emlHeaders && emlHeaders.any { it in MESSAGE_IDENTITY_HEADERS } &&
            ("\r\n\r\n" in prefix || "\n\n" in prefix)) return EML
        return null
    }

    private fun mapStructured(format: TierADocumentFormat, outcome: StructuredDerivativeGenerationCoordinationOutcome, f: TierAMediaFacts) = when (outcome) {
        is StructuredDerivativeGenerationCoordinationOutcome.Admitted -> TierADocumentRoutingResult.Admitted(format, outcome.record, TierADerivativePayload.Structured(outcome.representation), f)
        is StructuredDerivativeGenerationCoordinationOutcome.ExtractionFailed -> TierADocumentRoutingResult.ExtractionFailed(format, outcome.reason, f)
        is StructuredDerivativeGenerationCoordinationOutcome.CapabilityUnavailable -> TierADocumentRoutingResult.Unsupported(outcome.reason, f)
        is StructuredDerivativeGenerationCoordinationOutcome.SourceIntegrityFailed -> TierADocumentRoutingResult.SourceIntegrityFailed(outcome.reason, f)
        is StructuredDerivativeGenerationCoordinationOutcome.PreparationFailed -> failed(format, "PREPARE", outcome.derivativeGenerationId, outcome.reason, f)
        is StructuredDerivativeGenerationCoordinationOutcome.AuthorisationAuditFailed -> failed(format, "ADMISSION_AUTHORISED", outcome.derivativeGenerationId, outcome.reason, f)
        is StructuredDerivativeGenerationCoordinationOutcome.PublicationFailed -> failed(format, "PUBLISH", outcome.derivativeGenerationId, outcome.reason, f)
    }

    private fun mapCsv(o: DerivativeGenerationCoordinationOutcome, f: TierAMediaFacts) = when (o) {
        is DerivativeGenerationCoordinationOutcome.Admitted -> TierADocumentRoutingResult.Admitted(TierADocumentFormat.CSV, o.record, TierADerivativePayload.Csv(o.csvStructure), f)
        is DerivativeGenerationCoordinationOutcome.ExtractionFailed -> TierADocumentRoutingResult.ExtractionFailed(TierADocumentFormat.CSV, o.reason, f)
        is DerivativeGenerationCoordinationOutcome.SourceIntegrityFailed -> TierADocumentRoutingResult.SourceIntegrityFailed(o.reason, f)
        is DerivativeGenerationCoordinationOutcome.PreparationFailed -> failed(TierADocumentFormat.CSV, "PREPARE", o.derivativeGenerationId, o.reason, f)
        is DerivativeGenerationCoordinationOutcome.AuthorisationAuditFailed -> failed(TierADocumentFormat.CSV, "ADMISSION_AUTHORISED", o.derivativeGenerationId, o.reason, f)
        is DerivativeGenerationCoordinationOutcome.PublicationFailed -> failed(TierADocumentFormat.CSV, "PUBLISH", o.derivativeGenerationId, o.reason, f)
        is DerivativeGenerationCoordinationOutcome.AdmittedAuditFailed -> TierADocumentRoutingResult.ReconciliationRequired(TierADocumentFormat.CSV, o.record, TierADerivativePayload.Csv(o.csvStructure), o.reason, f)
    }
    private fun mapEml(o: EmlDerivativeGenerationCoordinationOutcome, f: TierAMediaFacts) = when (o) {
        is EmlDerivativeGenerationCoordinationOutcome.Admitted -> TierADocumentRoutingResult.Admitted(TierADocumentFormat.EML, o.record, TierADerivativePayload.Eml(o.emlStructure, o.childSourceCandidates.size), f)
        is EmlDerivativeGenerationCoordinationOutcome.ExtractionFailed -> TierADocumentRoutingResult.ExtractionFailed(TierADocumentFormat.EML, o.reason, f)
        is EmlDerivativeGenerationCoordinationOutcome.SourceIntegrityFailed -> TierADocumentRoutingResult.SourceIntegrityFailed(o.reason, f)
        is EmlDerivativeGenerationCoordinationOutcome.PreparationFailed -> failed(TierADocumentFormat.EML, "PREPARE", o.derivativeGenerationId, o.reason, f)
        is EmlDerivativeGenerationCoordinationOutcome.AuthorisationAuditFailed -> failed(TierADocumentFormat.EML, "ADMISSION_AUTHORISED", o.derivativeGenerationId, o.reason, f)
        is EmlDerivativeGenerationCoordinationOutcome.PublicationFailed -> failed(TierADocumentFormat.EML, "PUBLISH", o.derivativeGenerationId, o.reason, f)
        is EmlDerivativeGenerationCoordinationOutcome.AdmittedAuditFailed -> TierADocumentRoutingResult.ReconciliationRequired(TierADocumentFormat.EML, o.record, TierADerivativePayload.Eml(o.emlStructure, o.childSourceCandidates.size), o.reason, f)
    }
    private fun mapDocx(o: DocxDerivativeGenerationCoordinationOutcome, f: TierAMediaFacts) = when (o) {
        is DocxDerivativeGenerationCoordinationOutcome.Admitted -> TierADocumentRoutingResult.Admitted(TierADocumentFormat.DOCX, o.record, TierADerivativePayload.Docx(o.docxStructure), f)
        is DocxDerivativeGenerationCoordinationOutcome.RequiresTierB -> TierADocumentRoutingResult.RequiresTierB(o.reason, f)
        is DocxDerivativeGenerationCoordinationOutcome.ReviewRequired -> TierADocumentRoutingResult.Unsupported("REVIEW_REQUIRED: ${o.reason}", f)
        is DocxDerivativeGenerationCoordinationOutcome.ExtractionFailed -> TierADocumentRoutingResult.ExtractionFailed(TierADocumentFormat.DOCX, o.reason, f)
        is DocxDerivativeGenerationCoordinationOutcome.SourceIntegrityFailed -> TierADocumentRoutingResult.SourceIntegrityFailed(o.reason, f)
        is DocxDerivativeGenerationCoordinationOutcome.PreparationFailed -> failed(TierADocumentFormat.DOCX, "PREPARE", o.derivativeGenerationId, o.reason, f)
        is DocxDerivativeGenerationCoordinationOutcome.AuthorisationAuditFailed -> failed(TierADocumentFormat.DOCX, "ADMISSION_AUTHORISED", o.derivativeGenerationId, o.reason, f)
        is DocxDerivativeGenerationCoordinationOutcome.PublicationFailed -> failed(TierADocumentFormat.DOCX, "PUBLISH", o.derivativeGenerationId, o.reason, f)
        is DocxDerivativeGenerationCoordinationOutcome.AdmittedAuditFailed -> TierADocumentRoutingResult.ReconciliationRequired(TierADocumentFormat.DOCX, o.record, TierADerivativePayload.Docx(o.docxStructure), o.reason, f)
    }
    private fun mapPdf(o: PdfDerivativeGenerationCoordinationOutcome, f: TierAMediaFacts) = when (o) {
        is PdfDerivativeGenerationCoordinationOutcome.Admitted -> TierADocumentRoutingResult.Admitted(TierADocumentFormat.PDF, o.record, TierADerivativePayload.Pdf(o.pdfStructure), f)
        is PdfDerivativeGenerationCoordinationOutcome.RequiresTierB -> TierADocumentRoutingResult.RequiresTierB(o.reason, f)
        is PdfDerivativeGenerationCoordinationOutcome.ExtractionFailed -> TierADocumentRoutingResult.ExtractionFailed(TierADocumentFormat.PDF, o.reason, f)
        is PdfDerivativeGenerationCoordinationOutcome.SourceIntegrityFailed -> TierADocumentRoutingResult.SourceIntegrityFailed(o.reason, f)
        is PdfDerivativeGenerationCoordinationOutcome.PreparationFailed -> failed(TierADocumentFormat.PDF, "PREPARE", o.derivativeGenerationId, o.reason, f)
        is PdfDerivativeGenerationCoordinationOutcome.AuthorisationAuditFailed -> failed(TierADocumentFormat.PDF, "ADMISSION_AUTHORISED", o.derivativeGenerationId, o.reason, f)
        is PdfDerivativeGenerationCoordinationOutcome.PublicationFailed -> failed(TierADocumentFormat.PDF, "PUBLISH", o.derivativeGenerationId, o.reason, f)
        is PdfDerivativeGenerationCoordinationOutcome.AdmittedAuditFailed -> TierADocumentRoutingResult.ReconciliationRequired(TierADocumentFormat.PDF, o.record, TierADerivativePayload.Pdf(o.pdfStructure), o.reason, f)
    }
    private fun failed(format: TierADocumentFormat, stage: String, id: DerivativeGenerationId, reason: String, facts: TierAMediaFacts) = TierADocumentRoutingResult.AdmissionFailed(format, stage, id, reason, facts)

    companion object {
        const val TXT = "text/plain"; const val CSV = "text/csv"; const val EML = "message/rfc822"; const val PDF = "application/pdf"; const val PNG = "image/png"
        const val DOC = "application/msword"; const val XLS = "application/vnd.ms-excel"; const val XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        const val MSG = "application/vnd.ms-outlook"; const val MSG_OLE = "application/x-ole-storage"; const val RTF = "application/rtf"; const val RTF_TEXT = "text/rtf"; const val RTF_X = "application/x-rtf"
        const val DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        const val MAX_DETECTION_ZIP_ENTRIES = 100
        const val MAX_HEADER_DETECTION_BYTES = 64 * 1024
        private val PNG_SIGNATURE = byteArrayOf(0x89.toByte(), 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a)
        private val MESSAGE_IDENTITY_HEADERS = setOf("from", "to", "date", "message-id")
    }
}

internal interface TierAFormatRoutes {
    suspend fun ingestStructured(source: StructuredIngestionSource, principal: PrincipalId, correlation: String): StructuredDerivativeGenerationCoordinationOutcome
    suspend fun ingestCsv(source: CsvIngestionSource, principal: PrincipalId, correlation: String): DerivativeGenerationCoordinationOutcome
    suspend fun ingestEml(source: EmlIngestionSource, principal: PrincipalId, correlation: String): EmlDerivativeGenerationCoordinationOutcome
    suspend fun ingestDocx(source: DocxIngestionSource, principal: PrincipalId, correlation: String): DocxDerivativeGenerationCoordinationOutcome
    suspend fun ingestPdf(source: PdfIngestionSource, principal: PrincipalId, correlation: String): PdfDerivativeGenerationCoordinationOutcome
}

internal class CoordinatorTierAFormatRoutes(private val coordinator: DerivativeGenerationCoordinator) : TierAFormatRoutes {
    override suspend fun ingestStructured(source: StructuredIngestionSource, principal: PrincipalId, correlation: String) = coordinator.ingestStructured(source, principal, correlation)
    override suspend fun ingestCsv(source: CsvIngestionSource, principal: PrincipalId, correlation: String) = coordinator.ingestCsv(source, principal, correlation)
    override suspend fun ingestEml(source: EmlIngestionSource, principal: PrincipalId, correlation: String) = coordinator.ingestEml(source, principal, correlation)
    override suspend fun ingestDocx(source: DocxIngestionSource, principal: PrincipalId, correlation: String) = coordinator.ingestDocx(source, principal, correlation)
    override suspend fun ingestPdf(source: PdfIngestionSource, principal: PrincipalId, correlation: String) = coordinator.ingestPdf(source, principal, correlation)
}
