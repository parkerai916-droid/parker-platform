package parker.core.runtime

import java.security.MessageDigest
import parker.core.interfaces.EvidenceArtifactId

/** Stable identity for one governed image-only DOCX OCR operation. */
internal object DocxExternalOcrOperationIdentity {
    const val VERSION = "docx-external-ocr-v1"
    const val WARNING_PREFIX = "DOCX_OCR_OPERATION_KEY="

    fun key(
        evidenceArtifactId: EvidenceArtifactId,
        sourceSha256: String,
        images: List<DocxEmbeddedImage>,
        providerProfileIdentity: String,
        instructionSha256: String?,
        schemaSha256: String?,
    ): String {
        val canonical = buildString {
            field("version", VERSION)
            field("evidenceArtifactId", evidenceArtifactId.value)
            field("sourceSha256", sourceSha256)
            field("providerProfileIdentity", providerProfileIdentity)
            field("instructionSha256", instructionSha256 ?: "<null>")
            field("schemaSha256", schemaSha256 ?: "<null>")
            field("processingProfileIdentity", OcrProcessingRepresentationFactory.DOCX_EMBEDDED_IMAGE_PROFILE_IDENTITY)
            field("imageCount", images.size.toString())
            images.forEach { image ->
                field("image[${image.ordinal}].ordinal", image.ordinal.toString())
                field("image[${image.ordinal}].partName", image.partName)
                field("image[${image.ordinal}].relationshipId", image.relationshipId ?: "<null>")
                field("image[${image.ordinal}].mediaType", image.mediaType)
                field("image[${image.ordinal}].sha256", image.sha256)
            }
        }
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(canonical.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        return "$VERSION:$digest"
    }

    fun warning(key: String) = "$WARNING_PREFIX$key"

    fun keyFromWarnings(warnings: List<String>): String? = warnings
        .firstOrNull { it.startsWith(WARNING_PREFIX) }
        ?.removePrefix(WARNING_PREFIX)
        ?.takeIf { it.startsWith("$VERSION:") && it.length == VERSION.length + 65 }

    private fun StringBuilder.field(name: String, value: String) {
        append(name).append('=').append(value.toByteArray(Charsets.UTF_8).size).append(':').append(value).append(';')
    }
}
