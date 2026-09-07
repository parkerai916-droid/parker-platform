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
    /** The raw, un-normalised Content-ID header value, when present. Never fetched, resolved, rendered, or interpreted as a location -- inert metadata only, preserved exactly as declared (including hostile/odd values), matching this extractor's existing unstripped Message-ID precedent. */
    val contentId: String? = null,
    /** Populated only for an entity that is itself a nested message/rfc822 (never for the root message). The nested message's own top-level headers, captured the same way the root message's headers are. */
    val nestedMessageHeaders: List<EmlHeader>? = null,
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
    /** The raw, un-normalised Content-ID header value, when present. See [EmlMimeEntity.contentId]. */
    val contentId: String? = null,
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
