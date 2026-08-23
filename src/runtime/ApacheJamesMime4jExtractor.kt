package parker.core.runtime

import java.io.ByteArrayInputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction
import java.security.MessageDigest
import java.time.Instant
import org.apache.james.mime4j.dom.BinaryBody
import org.apache.james.mime4j.dom.Entity
import org.apache.james.mime4j.dom.Message
import org.apache.james.mime4j.dom.Multipart
import org.apache.james.mime4j.dom.TextBody
import org.apache.james.mime4j.dom.field.DateTimeField
import org.apache.james.mime4j.dom.field.ContentTypeField
import org.apache.james.mime4j.dom.field.UnstructuredField
import org.apache.james.mime4j.message.DefaultMessageBuilder
import parker.core.interfaces.*

class ApacheJamesMime4jExtractor : EmlStructuralExtractor {
    override suspend fun extract(sourceBytes: ByteArray): EmlStructuralExtractionOutcome {
        if (sourceBytes.size > MAX_MESSAGE_BYTES) {
            return EmlStructuralExtractionOutcome.Malformed("EML source exceeds the $MAX_MESSAGE_BYTES-byte adapter limit")
        }
        return try {
            extractMessage(DefaultMessageBuilder().parseMessage(ByteArrayInputStream(sourceBytes)), sourceBytes)
        } catch (e: Exception) {
            EmlStructuralExtractionOutcome.Malformed("EML/MIME parsing failed: ${e.message ?: e::class.simpleName}")
        }
    }

    private fun extractMessage(message: Message, sourceBytes: ByteArray): EmlStructuralExtractionOutcome {
        if (message.header.fields.size > MAX_HEADER_COUNT) {
            return EmlStructuralExtractionOutcome.Malformed("EML header count exceeds the $MAX_HEADER_COUNT-header adapter limit")
        }
        if (message.header.fields.any { it.raw.length() > MAX_HEADER_BYTES }) {
            return EmlStructuralExtractionOutcome.Malformed("EML header exceeds the $MAX_HEADER_BYTES-byte adapter limit")
        }
        val topContentType = message.header.getField("Content-Type") as? ContentTypeField
        val boundary = topContentType?.boundary
        if (topContentType?.isMultipart == true && boundary != null &&
            !String(sourceBytes, Charsets.ISO_8859_1).contains("--$boundary--")) {
            return EmlStructuralExtractionOutcome.Malformed("Multipart MIME closing boundary is absent")
        }
        val warnings = mutableListOf<String>()
        val entities = mutableListOf<EmlMimeEntity>()
        val bodies = mutableListOf<EmlBodyAlternative>()
        val attachments = mutableListOf<EmlAttachmentCandidate>()
        visit(message, "0", null, 0, 0, entities, bodies, attachments, warnings)
        if (entities.isEmpty() || entities.any { it.mediaType.isBlank() }) {
            return EmlStructuralExtractionOutcome.Malformed("MIME structure could not be accounted for")
        }
        val headers = message.header.fields.map { field ->
            val rawBytes = field.raw.toByteArray()
            EmlHeader(field.name, field.body, rawBytes, String(rawBytes, Charsets.ISO_8859_1))
        }
        val rawDate = message.header.getField("Date")?.body
        val parsedDate = (message.header.getField("Date") as? DateTimeField)?.date?.toInstant()
        if (bodies.isEmpty()) warnings += "No readable body alternative was discovered"
        val completeness = if (warnings.isEmpty()) DerivativeCompletenessState.ACCOUNTED_FOR
            else DerivativeCompletenessState.ACCOUNTED_FOR_WITH_QUALIFICATIONS
        return EmlStructuralExtractionOutcome.Extracted(
            EmlStructuralResult(
                headers = headers,
                from = value(message, "From"), to = value(message, "To"), cc = value(message, "Cc"),
                rawDate = rawDate, parsedDate = parsedDate,
                subject = (message.header.getField("Subject") as? UnstructuredField)?.value ?: value(message, "Subject"),
                messageId = value(message, "Message-ID"), mimeVersion = value(message, "MIME-Version"),
                contentType = value(message, "Content-Type"), mimeEntities = entities,
                bodyAlternatives = bodies, attachmentCandidates = attachments,
                producerIdentity = PRODUCER_IDENTITY, transformationHistory = TRANSFORMATIONS,
                completenessState = completeness, warnings = warnings.distinct(),
            )
        )
    }

    private fun visit(
        entity: Entity, id: String, parent: String?, order: Int, depth: Int,
        entities: MutableList<EmlMimeEntity>, bodies: MutableList<EmlBodyAlternative>,
        attachments: MutableList<EmlAttachmentCandidate>, warnings: MutableList<String>,
    ) {
        if (depth > MAX_MIME_DEPTH) throw BoundedMimeException("MIME nesting exceeds the $MAX_MIME_DEPTH-level adapter limit")
        if (entities.size >= MAX_ENTITY_COUNT) throw BoundedMimeException("MIME entity count exceeds the $MAX_ENTITY_COUNT-entity adapter limit")
        val body = entity.body
        val children = if (body is Multipart) body.bodyParts.mapIndexed { index, _ -> "$id.$index" } else emptyList()
        val mediaType = entity.mimeType.lowercase()
        val disposition = entity.dispositionType
        val filename = entity.filename
        val encoding = entity.contentTransferEncoding
        val charset = (entity.header.getField("Content-Type") as? ContentTypeField)
            ?.getParameter(ContentTypeField.PARAM_CHARSET)
        entities += EmlMimeEntity(id, parent, order, mediaType, disposition, encoding, filename, charset, children)
        when (body) {
            is Multipart -> body.bodyParts.forEachIndexed { index, child ->
                visit(child, "$id.$index", id, index, depth + 1, entities, bodies, attachments, warnings)
            }
            is TextBody -> {
                val bytes = body.inputStream.use { readBounded(it, MAX_DECODED_PART_BYTES, id) }
                if (disposition.equals("attachment", ignoreCase = true) || filename != null) {
                    attachments += attachment(id, parent, filename, mediaType, disposition, encoding, charset, bytes)
                    if (charset == null) warnings += "Attachment $id has no declared charset; exact decoded bytes remain authoritative"
                } else {
                    val decoded = decodeText(bytes, charset, id, warnings)
                    if (decoded != null) bodies += EmlBodyAlternative(id, mediaType, charset, bytes, decoded)
                }
            }
            is BinaryBody -> {
                val bytes = body.inputStream.use { readBounded(it, MAX_DECODED_PART_BYTES, id) }
                attachments += attachment(id, parent, filename, mediaType, disposition, encoding, charset, bytes)
            }
            else -> warnings += "MIME entity $id has an unreadable or unsupported body representation"
        }
    }

    private fun decodeText(bytes: ByteArray, charsetName: String?, id: String, warnings: MutableList<String>): String? {
        if (charsetName == null) {
            warnings += "Body entity $id has no declared charset and was not text-decoded"
            return null
        }
        return try {
            Charset.forName(charsetName).newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString()
        } catch (e: IllegalArgumentException) {
            warnings += "Body entity $id declares unsupported charset '$charsetName'"
            null
        } catch (e: CharacterCodingException) {
            warnings += "Body entity $id cannot be decoded strictly as '$charsetName'"
            null
        }
    }

    private fun attachment(id: String, parent: String?, filename: String?, mediaType: String,
        disposition: String?, encoding: String?, charset: String?, bytes: ByteArray) = EmlAttachmentCandidate(
        id, parent, filename, mediaType, disposition, encoding, charset, bytes, bytes.size.toLong(), sha256(bytes),
        listOf(DerivativeTransformation.MIME_TRANSFER_DECODING),
    )

    private fun value(message: Message, name: String) = message.header.getField(name)?.body
    private fun readBounded(input: InputStream, maximum: Int, entityId: String): ByteArray {
        val output = java.io.ByteArrayOutputStream(minOf(8192, maximum))
        val buffer = ByteArray(8192)
        var total = 0
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            total += count
            if (total > maximum) throw BoundedMimeException("Decoded MIME entity $entityId exceeds the $maximum-byte adapter limit")
            output.write(buffer, 0, count)
        }
        return output.toByteArray()
    }
    private fun sha256(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    private class BoundedMimeException(message: String) : IllegalArgumentException(message)

    companion object {
        const val PRODUCT_IDENTITY = "Apache James Mime4j"
        const val PRODUCT_VERSION = "0.8.14"
        const val ADAPTER_IDENTITY = "parker.apache-james-mime4j"
        const val ADAPTER_VERSION = "1"
        const val CONFIGURATION_IDENTITY = "mime4j-dom-exact-transfer-bytes-ordered-tree-bounded-v2"
        const val MAX_MESSAGE_BYTES = 16 * 1024 * 1024
        const val MAX_DECODED_PART_BYTES = 8 * 1024 * 1024
        const val MAX_HEADER_COUNT = 500
        const val MAX_HEADER_BYTES = 64 * 1024
        const val MAX_ENTITY_COUNT = 1000
        const val MAX_MIME_DEPTH = 32
        val PRODUCER_IDENTITY = DerivativeProducerIdentity(PRODUCT_IDENTITY, PRODUCT_VERSION, CONFIGURATION_IDENTITY, ADAPTER_IDENTITY, ADAPTER_VERSION)
        val TRANSFORMATIONS = listOf(
            DerivativeTransformation.MIME_TRANSFER_DECODING,
            DerivativeTransformation.CHARACTER_DECODING,
            DerivativeTransformation.DATE_PARSING,
            DerivativeTransformation.STRUCTURAL_PARSING,
        )
    }
}
