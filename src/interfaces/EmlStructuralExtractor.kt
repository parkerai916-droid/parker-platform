package parker.core.interfaces

import java.time.Instant

data class EmlHeader(
    val name: String,
    val value: String,
    val rawBytes: ByteArray,
    val rawRepresentation: String,
)

data class EmlMimeEntity(
    val entityId: String,
    val parentEntityId: String?,
    val order: Int,
    val mediaType: String,
    val disposition: String?,
    val transferEncoding: String?,
    val filename: String?,
    val charset: String?,
    val childEntityIds: List<String>,
)

data class EmlBodyAlternative(
    val mimeEntityId: String,
    val mediaType: String,
    val charset: String?,
    val decodedBytes: ByteArray,
    val decodedText: String,
)

data class EmlAttachmentCandidate(
    val mimeEntityId: String,
    val parentMimeEntityId: String?,
    val filename: String?,
    val declaredMimeType: String,
    val disposition: String?,
    val transferEncoding: String?,
    val charset: String?,
    val decodedBytes: ByteArray,
    val byteLength: Long,
    val sha256: String,
    val transformations: List<DerivativeTransformation>,
)

data class EmlStructuralResult(
    val headers: List<EmlHeader>,
    val from: String?,
    val to: String?,
    val cc: String?,
    val rawDate: String?,
    val parsedDate: Instant?,
    val subject: String?,
    val messageId: String?,
    val mimeVersion: String?,
    val contentType: String?,
    val mimeEntities: List<EmlMimeEntity>,
    val bodyAlternatives: List<EmlBodyAlternative>,
    val attachmentCandidates: List<EmlAttachmentCandidate>,
    val producerIdentity: DerivativeProducerIdentity,
    val transformationHistory: List<DerivativeTransformation>,
    val completenessState: DerivativeCompletenessState,
    val warnings: List<String>,
)

sealed class EmlStructuralExtractionOutcome {
    data class Extracted(val result: EmlStructuralResult) : EmlStructuralExtractionOutcome()
    data class Malformed(val reason: String) : EmlStructuralExtractionOutcome() {
        init { require(reason.isNotBlank()) }
    }
}

fun interface EmlStructuralExtractor {
    suspend fun extract(sourceBytes: ByteArray): EmlStructuralExtractionOutcome
}
