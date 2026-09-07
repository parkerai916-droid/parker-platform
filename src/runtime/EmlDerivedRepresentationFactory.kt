package parker.core.runtime

import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.time.Instant
import parker.core.interfaces.*

/**
 * Pure, offline construction boundary for a governed EML canonical text projection
 * (FIDELITY_PRESERVING_EVIDENCE_ACQUISITION_SCOPE_LOCK.md §6.1). Consumes an already-verified
 * authoritative source and an already-produced [EmlStructuralResult] -- it does not itself parse
 * raw bytes, resolve custody, or invoke any provider. Stage 1 (MIME parsing/transfer-decoding/
 * charset-decoding) is [ApacheJamesMime4jExtractor]'s own responsibility, performed before this
 * factory is ever called; this factory is stage 2 only.
 */
class EmlDerivedRepresentationFactory(
    private val maximumRepresentationBytes: Long = ExternalTranscriptionRequest.MAX_SOURCE_BYTES,
    private val now: () -> Instant = Instant::now,
) {
    internal fun create(
        authoritativeSource: AuthoritativeAcquisitionInput,
        structuralResult: EmlStructuralResult,
    ): EmlDerivedRepresentationOutcome {
        val sourceMediaType = authoritativeSource.mediaType
        if (sourceMediaType != "message/rfc822") return EmlDerivedRepresentationOutcome.UnsupportedMedia
        if (structuralResult.mimeEntities.isEmpty()) return EmlDerivedRepresentationOutcome.MalformedStructuralResult

        val attachmentIds = structuralResult.attachmentCandidates.map { it.mimeEntityId }.toSet()
        val bodyAlternativesById = structuralResult.bodyAlternatives.associateBy { it.mimeEntityId }
        // A body-bearing leaf: no children (not a container), and not classified as an
        // attachment. Every such entity must have a corresponding successfully-decoded body
        // alternative, or the whole representation fails closed -- no partial derivative.
        val bodyLeafEntities = structuralResult.mimeEntities.filter {
            it.childEntityIds.isEmpty() && it.entityId !in attachmentIds
        }
        if (bodyLeafEntities.isEmpty()) return EmlDerivedRepresentationOutcome.NoBodyContent
        val failedBodyEntities = bodyLeafEntities.filter { it.entityId !in bodyAlternativesById }
        if (failedBodyEntities.isNotEmpty()) {
            return EmlDerivedRepresentationOutcome.IncompleteBodyContent(failedBodyEntities.map { it.entityId })
        }

        return try {
            val structuralDigest = OcrSha256Digest(sha256Hex(canonicalStructuralBytes(structuralResult)))
            val textBytes = canonicalTextProjection(structuralResult).toByteArray(Charsets.UTF_8)
            if (textBytes.size.toLong() > minOf(maximumRepresentationBytes, ExternalTranscriptionRequest.MAX_SOURCE_BYTES)) {
                return EmlDerivedRepresentationOutcome.BoundsExceeded
            }
            val representationDigest = OcrSha256Digest(sha256Hex(textBytes))
            val attachmentManifestDigest = OcrSha256Digest(sha256Hex(canonicalAttachmentManifestBytes(structuralResult.attachmentCandidates)))

            val provenance = EmlDerivedRepresentationProvenance(
                sourceEvidenceArtifactId = authoritativeSource.evidenceArtifactId,
                sourceMediaType = sourceMediaType,
                sourceSha256 = OcrSha256Digest(authoritativeSource.sha256),
                sourceByteLength = authoritativeSource.byteLength,
                structuralParserIdentity = structuralResult.producerIdentity,
                structuralTransformationHistory = structuralResult.transformationHistory,
                structuralCompletenessState = structuralResult.completenessState,
                structuralResultSha256 = structuralDigest,
                representationGenerationProfileIdentity = EML_CANONICAL_PROJECTION_PROFILE_IDENTITY,
                representationMediaType = EML_DERIVED_TEXT_REPRESENTATION_MEDIA_TYPE,
                representationByteLength = textBytes.size.toLong(),
                representationSha256 = representationDigest,
                includedMimeEntityCount = structuralResult.mimeEntities.size,
                mimeEntityOrder = structuralResult.mimeEntities.map { it.entityId },
                perPartDecoding = bodyLeafEntities.map { entity ->
                    EmlPartDecodingRecord(entity.entityId, entity.charset, entity.transferEncoding, decodeSucceeded = true)
                },
                bodyAlternativesIncluded = bodyLeafEntities.map { it.entityId },
                excludedParts = emptyList(),
                nestedMessageEntityIds = structuralResult.mimeEntities.filter { it.nestedMessageHeaders != null }.map { it.entityId },
                attachmentManifestEntryCount = structuralResult.attachmentCandidates.size,
                attachmentManifestSha256 = attachmentManifestDigest,
                createdAt = now(),
            )
            EmlDerivedRepresentationOutcome.Created(EmlDerivedRepresentation(textBytes, provenance))
        } catch (_: Exception) {
            EmlDerivedRepresentationOutcome.ImplementationFailure
        }
    }

    private fun sha256Hex(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    companion object {
        /** Explicit control-picture substitutes for embedded CR/LF in single-line canonical-text fields, so an injected value can never fabricate a line boundary. */
        private const val CR_SUBSTITUTE = '␍'
        private const val LF_SUBSTITUTE = '␊'

        private fun escapeLine(value: String) = value.replace('\r', CR_SUBSTITUTE).replace('\n', LF_SUBSTITUTE)
        private fun fieldOrNone(value: String?) = value?.let(::escapeLine) ?: "(none)"

        /**
         * Fixed top-level section order -- deliberately not to be casually re-ordered after
         * implementation: (1) header block, (2) MIME structure, (3) body alternatives,
         * (4) nested messages, (5) attachment manifest.
         */
        internal fun canonicalTextProjection(result: EmlStructuralResult): String = buildString {
            append("=== PARKER EML CANONICAL TEXT PROJECTION v1 ===\n\n")

            append("--- HEADER BLOCK ---\n")
            result.headers.forEach { append(escapeLine(it.name)).append(": ").append(escapeLine(it.value)).append('\n') }
            append('\n')

            append("--- MIME STRUCTURE ---\n")
            result.mimeEntities.forEach { e ->
                append('[').append(e.entityId).append("] parent=").append(e.parentEntityId ?: "(root)")
                    .append(" children=[").append(e.childEntityIds.joinToString(", ")).append(']')
                    .append(" mediaType=").append(e.mediaType)
                    .append(" disposition=").append(fieldOrNone(e.disposition))
                    .append(" filename=").append(fieldOrNone(e.filename))
                    .append(" charset=").append(fieldOrNone(e.charset))
                    .append(" transferEncoding=").append(fieldOrNone(e.transferEncoding))
                    .append(" contentId=").append(fieldOrNone(e.contentId))
                    .append(" nestedMessage=").append(e.nestedMessageHeaders != null).append('\n')
            }
            append('\n')

            append("--- BODY ALTERNATIVES ---\n")
            val transferEncodingByEntity = result.mimeEntities.associate { it.entityId to it.transferEncoding }
            result.bodyAlternatives.forEach { b ->
                append("[BODY ").append(b.mimeEntityId).append("] mediaType=").append(b.mediaType)
                    .append(" charset=").append(fieldOrNone(b.charset))
                    .append(" transferEncoding=").append(fieldOrNone(transferEncodingByEntity[b.mimeEntityId])).append('\n')
                append(b.decodedText)
                if (b.decodedText.isEmpty() || !b.decodedText.endsWith('\n')) append('\n')
                append("[END BODY ").append(b.mimeEntityId).append("]\n")
            }
            append('\n')

            append("--- NESTED MESSAGES ---\n")
            val nested = result.mimeEntities.filter { it.nestedMessageHeaders != null }
            if (nested.isEmpty()) append("(none)\n")
            nested.forEach { e ->
                append("[NESTED MESSAGE ").append(e.entityId).append("] parent=").append(e.parentEntityId ?: "(root)").append('\n')
                e.nestedMessageHeaders!!.forEach { append("  ").append(escapeLine(it.name)).append(": ").append(escapeLine(it.value)).append('\n') }
            }
            append('\n')

            append("--- ATTACHMENT MANIFEST ---\n")
            if (result.attachmentCandidates.isEmpty()) append("(none)\n")
            result.attachmentCandidates.forEach { a ->
                append("[ATTACHMENT ").append(a.mimeEntityId).append("] parent=").append(a.parentMimeEntityId ?: "(root)")
                    .append(" filename=").append(fieldOrNone(a.filename))
                    .append(" mediaType=").append(a.declaredMimeType)
                    .append(" disposition=").append(fieldOrNone(a.disposition))
                    .append(" transferEncoding=").append(fieldOrNone(a.transferEncoding))
                    .append(" charset=").append(fieldOrNone(a.charset))
                    .append(" contentId=").append(fieldOrNone(a.contentId))
                    .append(" byteLength=").append(a.byteLength)
                    .append(" sha256=").append(a.sha256).append('\n')
            }
        }

        /**
         * Deterministic, collision-resistant canonical serialization of an entire
         * [EmlStructuralResult], used only to compute [EmlDerivedRepresentationProvenance.structuralResultSha256].
         * Every variable-length field is length-prefixed (never delimiter/escape-based), so no
         * field value -- however hostile -- can be confused with a structural boundary. Never
         * serialized or persisted verbatim; write-only, for hashing.
         */
        internal fun canonicalStructuralBytes(result: EmlStructuralResult): ByteArray {
            val w = CanonicalWriter()
            w.list(result.headers.size); result.headers.forEach { w.str(it.name).str(it.value) }
            w.str(result.from).str(result.to).str(result.cc)
            w.str(result.rawDate).str(result.parsedDate?.toString())
            w.str(result.subject).str(result.messageId).str(result.mimeVersion).str(result.contentType)
            w.list(result.mimeEntities.size)
            result.mimeEntities.forEach { e ->
                w.str(e.entityId).str(e.parentEntityId).int(e.order).str(e.mediaType).str(e.disposition)
                    .str(e.transferEncoding).str(e.filename).str(e.charset)
                w.list(e.childEntityIds.size); e.childEntityIds.forEach { w.str(it) }
                w.str(e.contentId)
                if (e.nestedMessageHeaders == null) {
                    w.bool(false)
                } else {
                    w.bool(true)
                    w.list(e.nestedMessageHeaders.size)
                    e.nestedMessageHeaders.forEach { w.str(it.name).str(it.value) }
                }
            }
            w.list(result.bodyAlternatives.size)
            result.bodyAlternatives.forEach { b -> w.str(b.mimeEntityId).str(b.mediaType).str(b.charset).bytesField(b.decodedBytes) }
            w.list(result.attachmentCandidates.size)
            result.attachmentCandidates.forEach { a ->
                w.str(a.mimeEntityId).str(a.parentMimeEntityId).str(a.filename).str(a.declaredMimeType)
                    .str(a.disposition).str(a.transferEncoding).str(a.charset).long(a.byteLength).str(a.sha256)
                w.list(a.transformations.size); a.transformations.forEach { w.str(it.name) }
                w.str(a.contentId)
            }
            w.str(result.producerIdentity.pluginIdentity).str(result.producerIdentity.pluginVersion)
                .str(result.producerIdentity.configurationIdentity).str(result.producerIdentity.adapterIdentity)
                .str(result.producerIdentity.adapterVersion).str(result.producerIdentity.modelIdentity)
                .str(result.producerIdentity.modelVersion)
            w.list(result.transformationHistory.size); result.transformationHistory.forEach { w.str(it.name) }
            w.str(result.completenessState.name)
            w.list(result.warnings.size); result.warnings.forEach { w.str(it) }
            return w.toByteArray()
        }

        /** Same length-prefixed discipline as [canonicalStructuralBytes], scoped to attachment metadata only -- never raw attachment bytes. */
        internal fun canonicalAttachmentManifestBytes(attachments: List<EmlAttachmentCandidate>): ByteArray {
            val w = CanonicalWriter()
            w.list(attachments.size)
            attachments.forEach { a ->
                w.str(a.mimeEntityId).str(a.parentMimeEntityId).str(a.filename).str(a.declaredMimeType)
                    .str(a.disposition).str(a.transferEncoding).str(a.charset).long(a.byteLength).str(a.sha256).str(a.contentId)
            }
            return w.toByteArray()
        }
    }
}

/**
 * Length-prefixed (Netstring-style) canonical byte writer: every variable-length field is
 * `<tag><ascii-length>:<utf8-bytes>` (or `~` for an explicit null), so no field value can ever be
 * mistaken for a delimiter -- there is no escaping to get wrong.
 */
private class CanonicalWriter {
    private val out = ByteArrayOutputStream()

    fun str(value: String?): CanonicalWriter {
        if (value == null) {
            out.write('~'.code)
        } else {
            val bytes = value.toByteArray(Charsets.UTF_8)
            out.write('S'.code)
            out.write(bytes.size.toString().toByteArray(Charsets.US_ASCII))
            out.write(':'.code)
            out.write(bytes)
        }
        return this
    }

    fun bytesField(value: ByteArray): CanonicalWriter {
        out.write('B'.code)
        out.write(value.size.toString().toByteArray(Charsets.US_ASCII))
        out.write(':'.code)
        out.write(value)
        return this
    }

    fun int(value: Int): CanonicalWriter {
        out.write('I'.code); out.write(value.toString().toByteArray(Charsets.US_ASCII)); out.write(';'.code)
        return this
    }

    fun long(value: Long): CanonicalWriter {
        out.write('I'.code); out.write(value.toString().toByteArray(Charsets.US_ASCII)); out.write(';'.code)
        return this
    }

    fun bool(value: Boolean): CanonicalWriter {
        out.write(if (value) 'T'.code else 'F'.code)
        return this
    }

    fun list(size: Int): CanonicalWriter {
        out.write('L'.code); out.write(size.toString().toByteArray(Charsets.US_ASCII)); out.write(';'.code)
        return this
    }

    fun toByteArray(): ByteArray = out.toByteArray()
}
