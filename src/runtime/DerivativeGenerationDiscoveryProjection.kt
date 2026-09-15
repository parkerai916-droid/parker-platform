package parker.core.runtime

import java.security.MessageDigest
import parker.core.interfaces.*

data class DerivativeCandidateSummary(
    val derivativeGenerationId: DerivativeGenerationId,
    val rootSourceEvidenceArtifactId: EvidenceArtifactId,
    val derivativeKind: String,
    val producerIdentity: DerivativeProducerIdentity,
    val generatedAt: java.time.Instant,
    val operationalOutcome: DerivativeOperationalOutcome,
    val completenessState: DerivativeCompletenessState,
    val warnings: List<String>,
    val transformationHistory: List<DerivativeTransformation>,
    val contentAvailable: Boolean,
    val authority: OcrAuthorityClassification = OcrAuthorityClassification.LOCAL_PRELIMINARY,
    /** Null means equivalence could not be established; such a candidate is never collapsed. */
    val equivalenceKey: String? = null,
)

class DerivativeGenerationDiscoveryProjection(
    private val generations: DerivativeGenerationDiscovery,
    private val contents: DerivativeContentStorage,
) {
    suspend fun discover(evidenceArtifactId: EvidenceArtifactId): List<DerivativeCandidateSummary> =
        generations.findGenerationsForEvidence(evidenceArtifactId)
            .filter { it.rootSourceEvidenceArtifactId == evidenceArtifactId }
            .map { record ->
                val content = try { contents.retrieve(record.derivativeGenerationId) }
                catch (_: DerivativeContentStorageException) { null }
                val available = content != null
                val ocr = (content?.payload as? TierADerivativePayload.Ocr)?.value
                val authority = ocr?.authority ?: if (ocr?.providerProvenance != null) {
                    OcrAuthorityClassification.EXTERNAL_AUTHORITATIVE
                } else OcrAuthorityClassification.LOCAL_PRELIMINARY
                val equivalenceKey = content?.let { derivativeEquivalenceKey(record, it.payload, authority) }
                DerivativeCandidateSummary(record.derivativeGenerationId, record.rootSourceEvidenceArtifactId,
                    record.derivativeKind, record.producerIdentity, record.generatedAt, record.operationalOutcome,
                    record.completenessState, record.warnings, record.transformationHistory, available, authority, equivalenceKey)
            }

    private fun derivativeEquivalenceKey(
        record: DerivativeGenerationRecord,
        payload: TierADerivativePayload,
        authority: OcrAuthorityClassification,
    ): String {
        // The generation id and timestamps are deliberately excluded: retries with identical
        // analytical structure must compare equal.  Do not use a generic object serializer here:
        // JVM array identity strings and request timestamps would make equivalent retries differ.
        val canonical = buildString {
            append("evidence=").append(record.rootSourceEvidenceArtifactId.value)
            append("|kind=").append(record.derivativeKind)
            append("|producer=").append(record.producerIdentity)
            append("|transformations=").append(record.transformationHistory)
            append("|contentIdentity=").append(record.contentIdentity)
            append("|completeness=").append(record.completenessState)
            append("|outcome=").append(record.operationalOutcome)
            append("|authority=").append(authority)
            append("|payload=").append(canonicalPayload(payload))
        }
        return MessageDigest.getInstance("SHA-256")
            .digest(canonical.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }

    private fun canonicalPayload(payload: TierADerivativePayload): String = when (payload) {
        is TierADerivativePayload.Csv -> payload.value.let { "CSV|headers=${it.headers}|rows=${it.rows}|delimiter=${it.delimiter}|quote=${it.quoteCharacter}|lineEnding=${it.lineEnding}" }
        is TierADerivativePayload.Structured -> payload.value.toString()
        is TierADerivativePayload.Docx -> payload.value.toString()
        is TierADerivativePayload.Pdf -> payload.value.copy(
            pageTextSegments = payload.value.pageTextSegments.map { it.copy(derivativeGenerationId = null) },
        ).toString()
        is TierADerivativePayload.Eml -> payload.value.let { value ->
            "EML|headers=${value.headers.map { listOf(it.name, it.value, sha256(it.rawBytes), it.rawRepresentation) }}" +
                "|from=${value.from}|to=${value.to}|cc=${value.cc}|date=${value.rawDate}|parsedDate=${value.parsedDate}" +
                "|subject=${value.subject}|messageId=${value.messageId}|mimeVersion=${value.mimeVersion}|contentType=${value.contentType}" +
                "|entities=${value.mimeEntities}|bodies=${value.bodyAlternatives.map { listOf(it.mimeEntityId, it.mediaType, it.charset, sha256(it.decodedBytes), it.decodedText) }}" +
                "|attachments=${value.attachmentCandidates.map { listOf(it.mimeEntityId, it.parentMimeEntityId, it.filename, it.declaredMimeType, it.disposition, it.transferEncoding, it.charset, it.sha256, it.byteLength, it.contentId) }}"
        }
        is TierADerivativePayload.Ocr -> payload.value.let { value ->
            // recognisedAt, processing createdAt, and provider correlation IDs identify an
            // attempt, not the analytical representation. Provider/model/configuration and
            // authority remain in the key and therefore cannot be collapsed across authorities.
            val processing = value.processingProvenance?.let {
                listOf(it.sourceEvidenceArtifactId, it.sourceManifestSha256, it.sourceMediaType, it.sourceByteLength,
                    it.requestedPageScope, it.submittedPageScope, it.representationMediaType,
                    it.representationByteLength, it.representationSha256, it.byteExactCopy,
                    it.processingProfileIdentity, it.materialTransformation)
            }
            val provider = value.providerProvenance?.let {
                listOf(it.providerIdentity, it.adapterIdentity, it.adapterVersion,
                    it.transcriptionConfigurationProfile, it.providerReportedModelIdentifier,
                    it.modelSnapshot, it.transcriptionConfiguration)
            }
            "OCR|text=${value.recognisedText}|fidelity=${value.fidelity}|outcome=${value.outcomeKind}|degradation=${value.degradationReason}" +
                "|warnings=${value.warnings}|segments=${value.segments}|producer=${value.producerIdentity}" +
                "|transformations=${value.transformationHistory}|completeness=${value.completenessState}" +
                "|pages=${value.pageAccounting}|processing=$processing|provider=$provider"
        }
        is TierADerivativePayload.RegionTranscription -> payload.value.toString()
    }

    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes)
        .joinToString("") { "%02x".format(it) }
}
