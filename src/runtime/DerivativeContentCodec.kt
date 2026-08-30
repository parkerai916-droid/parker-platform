package parker.core.runtime

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import parker.core.interfaces.CsvStructuralResult
import parker.core.interfaces.DerivativeContentEntry
import parker.core.interfaces.DerivativeGenerationId
import parker.core.interfaces.DerivativeCompletenessState
import parker.core.interfaces.DerivativeProducerIdentity
import parker.core.interfaces.DerivativeTransformation
import parker.core.interfaces.DocxHeaderFooter
import parker.core.interfaces.DocxMetadata
import parker.core.interfaces.DocxParagraph
import parker.core.interfaces.DocxRun
import parker.core.interfaces.DocxStructuralResult
import parker.core.interfaces.DocxTable
import parker.core.interfaces.DocxTableCell
import parker.core.interfaces.DocxTableRow
import parker.core.interfaces.EmbeddedResourceObservation
import parker.core.interfaces.EmlAttachmentCandidate
import parker.core.interfaces.EmlBodyAlternative
import parker.core.interfaces.EmlHeader
import parker.core.interfaces.EmlMimeEntity
import parker.core.interfaces.EmlStructuralResult
import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.OcrDerivativeExtractedResult
import parker.core.interfaces.OcrDerivativeOutcomeKind
import parker.core.interfaces.OcrRecognitionSegment
import parker.core.interfaces.OoxmlPartInventoryEntry
import parker.core.interfaces.PdfMetadataValue
import parker.core.interfaces.PdfStructuralResult
import parker.core.interfaces.TierADerivativePayload
import parker.core.interfaces.TranscriptionFidelity
import java.time.Instant
import parker.core.interfaces.*

/**
 * Document Ingestion — Derivative Content Persistence and Retrieval.
 * Governed by `docs/architecture/DOCUMENT_INGESTION_DERIVATIVE_CONTENT_PERSISTENCE_RETRIEVAL_SCOPE_LOCK.md`
 * §6/§7/§8. Encodes/decodes a [DerivativeContentEntry]'s own
 * [TierADerivativePayload] using the identical hand-rolled,
 * length-prefixed, bounded [DataOutputStream]/[DataInputStream] framing
 * [DerivativeGenerationRecordCodec] already established — never Java/Kotlin
 * native object serialization. **This is a storage encoding only, never
 * declared canonical** (Scope Lock §6): it exists to durably reconstruct
 * the specialist's own already-produced field values, not to assert one
 * true byte form of the semantic content.
 *
 * EML attachment-candidate bytes and body-alternative raw bytes are
 * deliberately never encoded (Scope Lock §7) — only the metadata/decoded-text
 * fields the owner-facing presentation layer already exposes.
 */
internal object DerivativeContentCodec {
    private const val MAGIC = 0x50444143 // "PDAC" -- Parker Derivative content
    private const val ENVELOPE_VERSION = 1

    // Per-format-kind schema versions (Scope Lock §8) -- independent of ENVELOPE_VERSION,
    // which governs only the outer envelope (id/root/formatKind/version framing itself).
    const val CSV_REPRESENTATION_VERSION = 1
    const val EML_REPRESENTATION_VERSION = 1
    const val DOCX_REPRESENTATION_VERSION = 1
    const val PDF_REPRESENTATION_VERSION = 1
    const val OCR_REPRESENTATION_VERSION = 3
    const val OCR_V2_REPRESENTATION_VERSION = 2
    const val OCR_LEGACY_REPRESENTATION_VERSION = 1
    const val REGION_TRANSCRIPTION_REPRESENTATION_VERSION = 1

    private const val FORMAT_CSV: Byte = 1
    private const val FORMAT_EML: Byte = 2
    private const val FORMAT_DOCX: Byte = 3
    private const val FORMAT_PDF: Byte = 4
    private const val FORMAT_OCR: Byte = 5
    private const val FORMAT_REGION_TRANSCRIPTION: Byte = 6

    private const val MAX_SHORT_STRING_BYTES = 1024 * 1024 // 1 MiB -- identifiers/names
    private const val MAX_LARGE_TEXT_BYTES = 32 * 1024 * 1024 // 32 MiB -- documentText/body/paragraph blobs
    private const val MAX_COLLECTION_SIZE = 1_000_000
    const val MAX_ENTRY_BYTES = 64L * 1024 * 1024 // matches the existing 64 MiB source-ingress bound

    // Tier B OCR-specific bounds -- TIER_B_OCR_DURABLE_REPRESENTATION_BOUNDS_DECISION.md §2.
    // Every value reuses an already-governed number (MAX_PDF_PAGES=200, the 20 MiB OCR
    // recognisedText ceiling, MAX_SHORT_STRING_BYTES); the outer MAX_ENTRY_BYTES check above
    // remains the final backstop regardless of these per-field bounds.
    private const val MAX_OCR_COLLECTION_SIZE = 200 // segment count, warning count -- reuses MAX_PDF_PAGES (page-aligned facts)
    private const val MAX_OCR_TEXT_BYTES = 20 * 1024 * 1024 // recognisedText / individual segment text -- reuses the OCR mechanism's own MAX_OUTPUT_BYTES

    class MalformedRepresentationException(message: String) : Exception(message)
    class UnsupportedRepresentationVersionException(val version: Int) : Exception("unsupported representation version $version")

    /** Returns encoded bytes with a trailing SHA-256 digest over everything preceding it (Scope Lock §6 storage-integrity digest). */
    fun encode(entry: DerivativeContentEntry): ByteArray {
        val body = ByteArrayOutputStream().use { bytes ->
            DataOutputStream(bytes).use { output ->
                output.writeInt(MAGIC)
                output.writeInt(ENVELOPE_VERSION)
                output.writeString(entry.derivativeGenerationId.value, MAX_SHORT_STRING_BYTES)
                output.writeString(entry.rootSourceEvidenceArtifactId.value, MAX_SHORT_STRING_BYTES)
                when (val payload = entry.payload) {
                    is TierADerivativePayload.Csv -> { output.writeByte(FORMAT_CSV.toInt()); output.writeInt(CSV_REPRESENTATION_VERSION); output.writeCsv(payload.value) }
                    is TierADerivativePayload.Eml -> { output.writeByte(FORMAT_EML.toInt()); output.writeInt(EML_REPRESENTATION_VERSION); output.writeEml(payload.value, payload.childSourceCandidateCount) }
                    is TierADerivativePayload.Docx -> { output.writeByte(FORMAT_DOCX.toInt()); output.writeInt(DOCX_REPRESENTATION_VERSION); output.writeDocx(payload.value) }
                    is TierADerivativePayload.Pdf -> { output.writeByte(FORMAT_PDF.toInt()); output.writeInt(PDF_REPRESENTATION_VERSION); output.writePdf(payload.value) }
                    is TierADerivativePayload.Ocr -> {
                        output.writeByte(FORMAT_OCR.toInt())
                        val v2 = payload.value.pageAccounting != null && payload.value.processingProvenance != null &&
                            payload.value.providerProvenance != null && payload.value.recognisedAt != null
                        val v3 = v2 && payload.value.providerProvenance?.transcriptionConfiguration is
                            OcrTranscriptionConfiguration.DigestedConfiguration
                        output.writeInt(when { v3 -> OCR_REPRESENTATION_VERSION; v2 -> OCR_V2_REPRESENTATION_VERSION; else -> OCR_LEGACY_REPRESENTATION_VERSION })
                        when { v3 -> output.writeOcrV3(payload.value); v2 -> output.writeOcrV2(payload.value); else -> output.writeOcr(payload.value) }
                    }
                    is TierADerivativePayload.RegionTranscription -> {
                        output.writeByte(FORMAT_REGION_TRANSCRIPTION.toInt())
                        output.writeInt(REGION_TRANSCRIPTION_REPRESENTATION_VERSION)
                        output.writeRegionTranscription(payload.value)
                    }
                }
            }
            bytes.toByteArray()
        }
        val digest = MessageDigest.getInstance("SHA-256").digest(body)
        val whole = body + digest
        require(whole.size <= MAX_ENTRY_BYTES) { "encoded derivative content exceeds the $MAX_ENTRY_BYTES-byte bound" }
        return whole
    }

    /** Verifies the trailing digest before decoding anything else -- corrupt storage bytes are detected before any field is trusted. */
    fun decode(bytes: ByteArray): DerivativeContentEntry {
        require(bytes.size <= MAX_ENTRY_BYTES) { "stored derivative content exceeds the $MAX_ENTRY_BYTES-byte bound" }
        if (bytes.size < 32) throw MalformedRepresentationException("stored content shorter than its own digest trailer")
        val body = bytes.copyOfRange(0, bytes.size - 32)
        val storedDigest = bytes.copyOfRange(bytes.size - 32, bytes.size)
        val actualDigest = MessageDigest.getInstance("SHA-256").digest(body)
        if (!MessageDigest.isEqual(storedDigest, actualDigest)) {
            throw MalformedRepresentationException("storage-representation digest mismatch")
        }
        return DataInputStream(ByteArrayInputStream(body)).use { input ->
            if (input.readInt() != MAGIC) throw MalformedRepresentationException("invalid content magic")
            if (input.readInt() != ENVELOPE_VERSION) throw MalformedRepresentationException("unsupported content envelope version")
            val id = DerivativeGenerationId(input.readString(MAX_SHORT_STRING_BYTES))
            val root = EvidenceArtifactId(input.readString(MAX_SHORT_STRING_BYTES))
            val formatByte = input.readByte()
            val representationVersion = input.readInt()
            val payload = when (formatByte) {
                FORMAT_CSV -> {
                    if (representationVersion != CSV_REPRESENTATION_VERSION) throw UnsupportedRepresentationVersionException(representationVersion)
                    TierADerivativePayload.Csv(input.readCsv())
                }
                FORMAT_EML -> {
                    if (representationVersion != EML_REPRESENTATION_VERSION) throw UnsupportedRepresentationVersionException(representationVersion)
                    val (eml, childCount) = input.readEml()
                    TierADerivativePayload.Eml(eml, childCount)
                }
                FORMAT_DOCX -> {
                    if (representationVersion != DOCX_REPRESENTATION_VERSION) throw UnsupportedRepresentationVersionException(representationVersion)
                    TierADerivativePayload.Docx(input.readDocx())
                }
                FORMAT_PDF -> {
                    if (representationVersion != PDF_REPRESENTATION_VERSION) throw UnsupportedRepresentationVersionException(representationVersion)
                    TierADerivativePayload.Pdf(input.readPdf())
                }
                FORMAT_OCR -> {
                    val ocr = when (representationVersion) {
                        OCR_LEGACY_REPRESENTATION_VERSION -> input.readOcr()
                        OCR_V2_REPRESENTATION_VERSION -> input.readOcrV2()
                        OCR_REPRESENTATION_VERSION -> input.readOcrV3()
                        else -> throw UnsupportedRepresentationVersionException(representationVersion)
                    }
                    TierADerivativePayload.Ocr(ocr)
                }
                FORMAT_REGION_TRANSCRIPTION -> {
                    if (representationVersion != REGION_TRANSCRIPTION_REPRESENTATION_VERSION) throw UnsupportedRepresentationVersionException(representationVersion)
                    TierADerivativePayload.RegionTranscription(input.readRegionTranscription())
                }
                else -> throw MalformedRepresentationException("unknown format kind byte $formatByte")
            }
            if (input.available() != 0) throw MalformedRepresentationException("trailing bytes after derivative payload")
            DerivativeContentEntry(id, root, payload)
        }
    }

    // ---- Ordinary external region-v5 transcription ------------------------------------------

    private fun DataOutputStream.writeRegionTranscription(r: OrdinaryRegionTranscriptionDerivative) {
        writeInt(r.representationVersion)
        writeString(r.evidenceArtifactId, MAX_SHORT_STRING_BYTES)
        writeString(r.sourceSha256, MAX_SHORT_STRING_BYTES)
        writeStrings(r.pageBindings); writeStrings(r.regionBindings); writeStrings(r.transcriptionBlocks)
        writeStrings(r.providerReturnedOrder); writeStrings(r.parkerSourceOrder)
        listOf(r.provider, r.model, r.adapterId, r.adapterVersion, r.providerProfile).forEach { writeString(it, MAX_SHORT_STRING_BYTES) }
        writeInt(r.wireVersion)
        listOf(r.schemaSha256, r.instructionSha256, r.processingProfile, r.requestIdentity,
            r.requestDigest, r.responseIdentity, r.providerStateRecordIdentity,
            r.capabilityAcceptanceRecordIdentity, r.ownerAuthorizationIdentity, r.executionIdentity,
            r.attemptIdentity, r.reconstructedContentDigest, r.canonicalGenerationKeyDigest,
            r.admissionProvenance).forEach { writeString(it, MAX_SHORT_STRING_BYTES) }
    }

    private fun DataInputStream.readRegionTranscription(): OrdinaryRegionTranscriptionDerivative {
        val version = readInt(); val evidence = readString(MAX_SHORT_STRING_BYTES); val source = readString(MAX_SHORT_STRING_BYTES)
        val pages = readStrings(); val regions = readStrings(); val blocks = readStrings(); val providerOrder = readStrings(); val parkerOrder = readStrings()
        val provider = readString(MAX_SHORT_STRING_BYTES); val model = readString(MAX_SHORT_STRING_BYTES)
        val adapterId = readString(MAX_SHORT_STRING_BYTES); val adapterVersion = readString(MAX_SHORT_STRING_BYTES)
        val profile = readString(MAX_SHORT_STRING_BYTES); val wire = readInt()
        val values = List(14) { readString(MAX_SHORT_STRING_BYTES) }
        return OrdinaryRegionTranscriptionDerivative(version, evidence, source, pages, regions, blocks,
            providerOrder, parkerOrder, provider, model, adapterId, adapterVersion, profile, wire,
            values[0], values[1], values[2], values[3], values[4], values[5], values[6], values[7],
            values[8], values[9], values[10], values[11], values[12], values[13])
    }

    // ---- PDF ----------------------------------------------------------------------------------

    private fun DataOutputStream.writePdf(r: PdfStructuralResult) {
        writeString(r.documentText, MAX_LARGE_TEXT_BYTES)
        writeBoolean(r.pageCount != null)
        r.pageCount?.let(::writeInt)
        writeBoolean(r.pageTextAssociationAvailable)
        writeCollectionSize(r.metadata.size)
        r.metadata.forEach { writeString(it.name, MAX_SHORT_STRING_BYTES); writeString(it.value, MAX_SHORT_STRING_BYTES); writeString(it.representation, MAX_SHORT_STRING_BYTES) }
        writeCollectionSize(r.embeddedResources.size)
        r.embeddedResources.forEach { writeNullableString(it.declaredFileName); writeNullableString(it.declaredMediaType) }
        writeProducer(r.producerIdentity)
        writeTransformations(r.transformationHistory)
        writeString(r.completenessState.name, MAX_SHORT_STRING_BYTES)
        writeStrings(r.warnings)
    }

    private fun DataInputStream.readPdf(): PdfStructuralResult {
        val documentText = readString(MAX_LARGE_TEXT_BYTES)
        val pageCount = if (readBoolean()) readInt() else null
        val pageTextAssociationAvailable = readBoolean()
        val metadata = List(readCollectionSize()) { PdfMetadataValue(readString(MAX_SHORT_STRING_BYTES), readString(MAX_SHORT_STRING_BYTES), readString(MAX_SHORT_STRING_BYTES)) }
        val embeddedResources = List(readCollectionSize()) { EmbeddedResourceObservation(readNullableString(), readNullableString()) }
        val producer = readProducer()
        val transformations = readTransformations()
        val completeness = enumValueOf<DerivativeCompletenessState>(readString(MAX_SHORT_STRING_BYTES))
        val warnings = readStrings()
        return PdfStructuralResult(documentText, pageCount, pageTextAssociationAvailable, metadata, embeddedResources, producer, transformations, completeness, warnings)
    }

    // ---- OCR (Tier B durable, TIER_B_OCR_DURABLE_REPRESENTATION_BOUNDS_DECISION.md) -----------

    private fun DataOutputStream.writeOcr(r: OcrDerivativeExtractedResult) {
        writeString(r.recognisedText, MAX_OCR_TEXT_BYTES)
        writeString(r.fidelity.name, MAX_SHORT_STRING_BYTES)
        writeString(r.outcomeKind.name, MAX_SHORT_STRING_BYTES)
        writeNullableString(r.degradationReason)
        require(r.warnings.size <= MAX_OCR_COLLECTION_SIZE) { "OCR warnings exceed the $MAX_OCR_COLLECTION_SIZE-entry codec limit" }
        writeCollectionSize(r.warnings.size)
        r.warnings.forEach { writeString(it, MAX_SHORT_STRING_BYTES) }
        require(r.segments.size <= MAX_OCR_COLLECTION_SIZE) { "OCR segments exceed the $MAX_OCR_COLLECTION_SIZE-entry codec limit" }
        writeCollectionSize(r.segments.size)
        r.segments.forEach { segment ->
            writeString(segment.text, MAX_OCR_TEXT_BYTES)
            writeString(segment.fidelity.name, MAX_SHORT_STRING_BYTES)
            writeBoolean(segment.pageNumber != null)
            segment.pageNumber?.let(::writeInt)
        }
        writeProducer(r.producerIdentity)
        writeTransformations(r.transformationHistory)
        writeString(r.completenessState.name, MAX_SHORT_STRING_BYTES)
    }

    private fun DataInputStream.readOcr(): OcrDerivativeExtractedResult {
        val recognisedText = readString(MAX_OCR_TEXT_BYTES)
        val fidelity = enumValueOf<TranscriptionFidelity>(readString(MAX_SHORT_STRING_BYTES))
        val outcomeKind = enumValueOf<OcrDerivativeOutcomeKind>(readString(MAX_SHORT_STRING_BYTES))
        val degradationReason = readNullableString()
        val warningCount = readCollectionSize()
        if (warningCount > MAX_OCR_COLLECTION_SIZE) throw MalformedRepresentationException("OCR warning count $warningCount exceeds the $MAX_OCR_COLLECTION_SIZE-entry codec limit")
        val warnings = List(warningCount) { readString(MAX_SHORT_STRING_BYTES) }
        val segmentCount = readCollectionSize()
        if (segmentCount > MAX_OCR_COLLECTION_SIZE) throw MalformedRepresentationException("OCR segment count $segmentCount exceeds the $MAX_OCR_COLLECTION_SIZE-entry codec limit")
        val segments = List(segmentCount) {
            val text = readString(MAX_OCR_TEXT_BYTES)
            val segmentFidelity = enumValueOf<TranscriptionFidelity>(readString(MAX_SHORT_STRING_BYTES))
            val pageNumber = if (readBoolean()) readInt() else null
            OcrRecognitionSegment(text, segmentFidelity, pageNumber)
        }
        val producer = readProducer()
        val transformations = readTransformations()
        val completeness = enumValueOf<DerivativeCompletenessState>(readString(MAX_SHORT_STRING_BYTES))
        return OcrDerivativeExtractedResult(
            recognisedText, fidelity, outcomeKind, degradationReason, warnings, segments, producer, transformations, completeness,
        )
    }

    private fun DataOutputStream.writeOcrV2(r: OcrDerivativeExtractedResult) {
        writeOcr(r)
        val accounting = requireNotNull(r.pageAccounting)
        require(accounting.pageOutcomes.map { it.pageNumber }.distinct().size == accounting.pageOutcomes.size) { "duplicate OCR page outcomes" }
        writeScope(accounting.requestedScope); writeScope(accounting.submittedScope); writeScope(accounting.returnedScope)
        require(accounting.pageOutcomes.size <= MAX_OCR_COLLECTION_SIZE)
        writeCollectionSize(accounting.pageOutcomes.size)
        accounting.pageOutcomes.forEach { page ->
            writeInt(page.pageNumber); writeString(page.outcome.name, MAX_SHORT_STRING_BYTES)
            writeBoolean(page.reason != null); page.reason?.let { writeString(it.classification, MAX_SHORT_STRING_BYTES); writeNullableString(it.detail) }
            writeStrings(page.warnings)
            writeCollectionSize(page.uncertaintySpans.size)
            page.uncertaintySpans.forEach { span ->
                writeInt(span.pageNumber); writeInt(span.startOffsetInclusive); writeInt(span.endOffsetExclusive)
                writeString(span.kind.name, MAX_SHORT_STRING_BYTES); writeString(span.disclosure, MAX_SHORT_STRING_BYTES)
            }
        }
        val processing = requireNotNull(r.processingProvenance)
        writeString(processing.sourceEvidenceArtifactId.value, MAX_SHORT_STRING_BYTES)
        writeString(processing.sourceManifestSha256.value, MAX_SHORT_STRING_BYTES)
        writeString(processing.sourceMediaType, MAX_SHORT_STRING_BYTES); writeLong(processing.sourceByteLength)
        writeNullableScope(processing.requestedPageScope); writeNullableScope(processing.submittedPageScope)
        writeString(processing.representationMediaType, MAX_SHORT_STRING_BYTES); writeLong(processing.representationByteLength)
        writeString(processing.representationSha256.value, MAX_SHORT_STRING_BYTES); writeBoolean(processing.byteExactCopy)
        writeString(processing.processingProfileIdentity, MAX_SHORT_STRING_BYTES); writeString(processing.createdAt.toString(), MAX_SHORT_STRING_BYTES)
        writeBoolean(processing.materialTransformation != null)
        processing.materialTransformation?.let { t ->
            writeString(t.mechanismIdentity, MAX_SHORT_STRING_BYTES); writeString(t.mechanismVersion, MAX_SHORT_STRING_BYTES); writeScope(t.sourcePageScope)
            writeNullableInt(t.dpi); writeBoolean(t.dimensions != null); t.dimensions?.let { writeInt(it.width); writeInt(it.height) }
            writeNullableDouble(t.rotationDegrees); writeNullableString(t.colourMode); writeNullableDouble(t.scaleX); writeNullableDouble(t.scaleY)
            writeBoolean(t.crop != null); t.crop?.let { writeInt(it.leftPx); writeInt(it.topPx); writeInt(it.widthPx); writeInt(it.heightPx) }
            writeNullableString(t.compression)
        }
        val provider = requireNotNull(r.providerProvenance)
        writeString(provider.providerIdentity, MAX_SHORT_STRING_BYTES); writeString(provider.adapterIdentity, MAX_SHORT_STRING_BYTES)
        writeString(provider.adapterVersion, MAX_SHORT_STRING_BYTES); writeString(provider.transcriptionConfigurationProfile, MAX_SHORT_STRING_BYTES)
        writeString(provider.providerReportedModelIdentifier, MAX_SHORT_STRING_BYTES)
        when (val snapshot = provider.modelSnapshot) {
            OcrModelSnapshot.NotExposed -> writeByte(0)
            is OcrModelSnapshot.Present -> { writeByte(1); writeString(snapshot.value, MAX_SHORT_STRING_BYTES) }
        }
        writeString(provider.providerCorrelationIdentifier, MAX_SHORT_STRING_BYTES)
        writeString(requireNotNull(r.recognisedAt).toString(), MAX_SHORT_STRING_BYTES)
    }

    private fun DataInputStream.readOcrV2(): OcrDerivativeExtractedResult {
        val legacy = readOcr()
        val accounting = OcrPageAccounting(readScope(), readScope(), readScope(), List(readOcrCount("page outcome")) {
            val pageNumber = readInt(); val kind = enumValueOf<OcrPageOutcomeKind>(readString(MAX_SHORT_STRING_BYTES))
            val reason = if (readBoolean()) OcrPageOutcomeReason(readString(MAX_SHORT_STRING_BYTES), readNullableString()) else null
            val warnings = readStrings()
            val spans = List(readOcrCount("uncertainty span")) {
                OcrUncertaintySpan(readInt(), readInt(), readInt(), enumValueOf(readString(MAX_SHORT_STRING_BYTES)), readString(MAX_SHORT_STRING_BYTES))
            }
            OcrPageOutcome(pageNumber, kind, reason, warnings, spans)
        })
        requireUnique(accounting.pageOutcomes.map { it.pageNumber }, "page outcomes")
        val processing = OcrProcessingProvenance(
            EvidenceArtifactId(readString(MAX_SHORT_STRING_BYTES)), OcrSha256Digest(readString(MAX_SHORT_STRING_BYTES)),
            readString(MAX_SHORT_STRING_BYTES), readLong(), readNullableScope(), readNullableScope(),
            readString(MAX_SHORT_STRING_BYTES), readLong(), OcrSha256Digest(readString(MAX_SHORT_STRING_BYTES)), readBoolean(),
            readString(MAX_SHORT_STRING_BYTES), Instant.parse(readString(MAX_SHORT_STRING_BYTES)),
            if (readBoolean()) OcrMaterialTransformation(
                readString(MAX_SHORT_STRING_BYTES), readString(MAX_SHORT_STRING_BYTES), readScope(), readNullableInt(),
                if (readBoolean()) OcrPixelDimensions(readInt(), readInt()) else null, readNullableDouble(), readNullableString(),
                readNullableDouble(), readNullableDouble(), if (readBoolean()) OcrCropParameters(readInt(), readInt(), readInt(), readInt()) else null,
                readNullableString(),
            ) else null,
        )
        val provider = OcrProviderProvenance(
            readString(MAX_SHORT_STRING_BYTES), readString(MAX_SHORT_STRING_BYTES), readString(MAX_SHORT_STRING_BYTES),
            readString(MAX_SHORT_STRING_BYTES), readString(MAX_SHORT_STRING_BYTES),
            when (val marker = readByte().toInt()) { 0 -> OcrModelSnapshot.NotExposed; 1 -> OcrModelSnapshot.Present(readString(MAX_SHORT_STRING_BYTES)); else -> throw MalformedRepresentationException("invalid model snapshot marker $marker") },
            readString(MAX_SHORT_STRING_BYTES),
        )
        return legacy.copy(pageAccounting = accounting, processingProvenance = processing, providerProvenance = provider,
            recognisedAt = Instant.parse(readString(MAX_SHORT_STRING_BYTES)))
    }

    private fun DataOutputStream.writeOcrV3(r: OcrDerivativeExtractedResult) {
        writeOcrV2(r)
        val configuration = requireNotNull(r.providerProvenance).transcriptionConfiguration
        require(configuration is OcrTranscriptionConfiguration.DigestedConfiguration) {
            "OCR v3 requires digested transcription configuration"
        }
        writeString(configuration.instructionSha256.value, MAX_SHORT_STRING_BYTES)
        writeString(configuration.structuredSchemaSha256.value, MAX_SHORT_STRING_BYTES)
    }

    private fun DataInputStream.readOcrV3(): OcrDerivativeExtractedResult {
        val v2 = readOcrV2()
        val provider = requireNotNull(v2.providerProvenance)
        val configuration = OcrTranscriptionConfiguration.DigestedConfiguration(
            provider.transcriptionConfigurationProfile,
            OcrSha256Digest(readString(MAX_SHORT_STRING_BYTES)),
            OcrSha256Digest(readString(MAX_SHORT_STRING_BYTES)),
        )
        return v2.copy(providerProvenance = provider.copy(transcriptionConfiguration = configuration))
    }

    private fun DataOutputStream.writeScope(scope: OcrPageScope) { writeCollectionSize(scope.pageNumbers.size); scope.pageNumbers.forEach(::writeInt) }
    private fun DataInputStream.readScope(): OcrPageScope = OcrPageScope(List(readOcrCount("page scope")) { readInt() })
    private fun DataOutputStream.writeNullableScope(scope: OcrPageScope?) { writeBoolean(scope != null); scope?.let { writeScope(it) } }
    private fun DataInputStream.readNullableScope(): OcrPageScope? = if (readBoolean()) readScope() else null
    private fun DataOutputStream.writeNullableInt(value: Int?) { writeBoolean(value != null); value?.let(::writeInt) }
    private fun DataInputStream.readNullableInt(): Int? = if (readBoolean()) readInt() else null
    private fun DataOutputStream.writeNullableDouble(value: Double?) { writeBoolean(value != null); value?.let(::writeDouble) }
    private fun DataInputStream.readNullableDouble(): Double? = if (readBoolean()) readDouble().also { if (!it.isFinite()) throw MalformedRepresentationException("non-finite double") } else null
    private fun DataInputStream.readOcrCount(label: String): Int = readCollectionSize().also { if (it > MAX_OCR_COLLECTION_SIZE) throw MalformedRepresentationException("OCR $label count exceeds limit") }
    private fun requireUnique(values: List<Int>, label: String) { if (values.size != values.toSet().size) throw MalformedRepresentationException("duplicate $label") }

    // ---- CSV ----------------------------------------------------------------------------------

    private fun DataOutputStream.writeCsv(r: CsvStructuralResult) {
        writeStrings(r.headers)
        writeCollectionSize(r.rows.size)
        r.rows.forEach { writeStrings(it) }
        writeChar(r.delimiter.code)
        writeChar(r.quoteCharacter.code)
        writeString(r.lineEnding, MAX_SHORT_STRING_BYTES)
        writeProducer(r.producerIdentity)
        writeTransformations(r.transformationHistory)
        writeString(r.completenessState.name, MAX_SHORT_STRING_BYTES)
        writeStrings(r.warnings)
    }

    private fun DataInputStream.readCsv(): CsvStructuralResult {
        val headers = readStrings()
        val rows = List(readCollectionSize()) { readStrings() }
        val delimiter = readChar()
        val quoteCharacter = readChar()
        val lineEnding = readString(MAX_SHORT_STRING_BYTES)
        val producer = readProducer()
        val transformations = readTransformations()
        val completeness = enumValueOf<DerivativeCompletenessState>(readString(MAX_SHORT_STRING_BYTES))
        val warnings = readStrings()
        return CsvStructuralResult(headers, rows, delimiter, quoteCharacter, lineEnding, producer, transformations, completeness, warnings)
    }

    // ---- EML (attachment/body raw bytes deliberately never encoded, Scope Lock §7) ------------

    private fun DataOutputStream.writeEml(r: EmlStructuralResult, childSourceCandidateCount: Int) {
        writeCollectionSize(r.headers.size)
        r.headers.forEach { writeString(it.name, MAX_SHORT_STRING_BYTES); writeString(it.value, MAX_SHORT_STRING_BYTES); writeString(it.rawRepresentation, MAX_SHORT_STRING_BYTES) }
        writeNullableString(r.from); writeNullableString(r.to); writeNullableString(r.cc)
        writeNullableString(r.rawDate)
        writeBoolean(r.parsedDate != null); r.parsedDate?.let { writeString(it.toString(), MAX_SHORT_STRING_BYTES) }
        writeNullableString(r.subject); writeNullableString(r.messageId); writeNullableString(r.mimeVersion); writeNullableString(r.contentType)
        writeCollectionSize(r.mimeEntities.size)
        r.mimeEntities.forEach {
            writeString(it.entityId, MAX_SHORT_STRING_BYTES); writeNullableString(it.parentEntityId); writeInt(it.order)
            writeString(it.mediaType, MAX_SHORT_STRING_BYTES); writeNullableString(it.disposition); writeNullableString(it.transferEncoding)
            writeNullableString(it.filename); writeNullableString(it.charset); writeStrings(it.childEntityIds)
        }
        writeCollectionSize(r.bodyAlternatives.size)
        r.bodyAlternatives.forEach {
            writeString(it.mimeEntityId, MAX_SHORT_STRING_BYTES); writeString(it.mediaType, MAX_SHORT_STRING_BYTES)
            writeNullableString(it.charset); writeString(it.decodedText, MAX_LARGE_TEXT_BYTES)
        }
        writeCollectionSize(r.attachmentCandidates.size)
        r.attachmentCandidates.forEach {
            writeString(it.mimeEntityId, MAX_SHORT_STRING_BYTES); writeNullableString(it.parentMimeEntityId); writeNullableString(it.filename)
            writeString(it.declaredMimeType, MAX_SHORT_STRING_BYTES); writeNullableString(it.disposition); writeNullableString(it.transferEncoding)
            writeNullableString(it.charset); writeLong(it.byteLength); writeString(it.sha256, MAX_SHORT_STRING_BYTES)
            writeTransformations(it.transformations)
        }
        writeProducer(r.producerIdentity)
        writeTransformations(r.transformationHistory)
        writeString(r.completenessState.name, MAX_SHORT_STRING_BYTES)
        writeStrings(r.warnings)
        writeInt(childSourceCandidateCount)
    }

    private fun DataInputStream.readEml(): Pair<EmlStructuralResult, Int> {
        val headers = List(readCollectionSize()) { EmlHeader(readString(MAX_SHORT_STRING_BYTES), readString(MAX_SHORT_STRING_BYTES), ByteArray(0), readString(MAX_SHORT_STRING_BYTES)) }
        val from = readNullableString(); val to = readNullableString(); val cc = readNullableString()
        val rawDate = readNullableString()
        val parsedDate = if (readBoolean()) Instant.parse(readString(MAX_SHORT_STRING_BYTES)) else null
        val subject = readNullableString(); val messageId = readNullableString(); val mimeVersion = readNullableString(); val contentType = readNullableString()
        val mimeEntities = List(readCollectionSize()) {
            EmlMimeEntity(
                readString(MAX_SHORT_STRING_BYTES), readNullableString(), readInt(),
                readString(MAX_SHORT_STRING_BYTES), readNullableString(), readNullableString(),
                readNullableString(), readNullableString(), readStrings(),
            )
        }
        val bodyAlternatives = List(readCollectionSize()) {
            EmlBodyAlternative(readString(MAX_SHORT_STRING_BYTES), readString(MAX_SHORT_STRING_BYTES), readNullableString(), ByteArray(0), readString(MAX_LARGE_TEXT_BYTES))
        }
        val attachmentCandidates = List(readCollectionSize()) {
            EmlAttachmentCandidate(
                readString(MAX_SHORT_STRING_BYTES), readNullableString(), readNullableString(), readString(MAX_SHORT_STRING_BYTES),
                readNullableString(), readNullableString(), readNullableString(), ByteArray(0), readLong(), readString(MAX_SHORT_STRING_BYTES),
                readTransformations(),
            )
        }
        val producer = readProducer()
        val transformations = readTransformations()
        val completeness = enumValueOf<DerivativeCompletenessState>(readString(MAX_SHORT_STRING_BYTES))
        val warnings = readStrings()
        val childSourceCandidateCount = readInt()
        return EmlStructuralResult(
            headers, from, to, cc, rawDate, parsedDate, subject, messageId, mimeVersion, contentType,
            mimeEntities, bodyAlternatives, attachmentCandidates, producer, transformations, completeness, warnings,
        ) to childSourceCandidateCount
    }

    // ---- DOCX -----------------------------------------------------------------------------------

    private fun DataOutputStream.writeDocx(r: DocxStructuralResult) {
        writeCollectionSize(r.paragraphs.size)
        r.paragraphs.forEach { writeParagraph(it) }
        writeCollectionSize(r.tables.size)
        r.tables.forEach { table ->
            writeInt(table.order); writeNullableString(table.styleId)
            writeCollectionSize(table.rows.size)
            table.rows.forEach { row ->
                writeInt(row.order)
                writeCollectionSize(row.cells.size)
                row.cells.forEach { writeInt(it.order); writeString(it.text, MAX_LARGE_TEXT_BYTES) }
            }
        }
        writeCollectionSize(r.headers.size); r.headers.forEach { writeHeaderFooter(it) }
        writeCollectionSize(r.footers.size); r.footers.forEach { writeHeaderFooter(it) }
        writeNullableString(r.metadata.title); writeNullableString(r.metadata.author); writeNullableString(r.metadata.subject)
        writeBoolean(r.metadata.parsedCreated != null); r.metadata.parsedCreated?.let { writeString(it.toString(), MAX_SHORT_STRING_BYTES) }
        writeNullableString(r.metadata.application); writeNullableString(r.metadata.applicationVersion)
        writeCollectionSize(r.parts.size)
        r.parts.forEach { writeString(it.name, MAX_SHORT_STRING_BYTES); writeNullableString(it.contentType); writeLong(it.uncompressedBytes) }
        writeInt(r.relationshipCount)
        writeStrings(r.relationshipTypes)
        writeStrings(r.mediaPartNames)
        writeProducer(r.producerIdentity)
        writeTransformations(r.transformationHistory)
        writeString(r.completenessState.name, MAX_SHORT_STRING_BYTES)
        writeStrings(r.warnings)
    }

    private fun DataOutputStream.writeParagraph(p: DocxParagraph) {
        writeInt(p.order); writeString(p.text, MAX_LARGE_TEXT_BYTES); writeNullableString(p.styleId)
        writeNullableString(p.numberingId); writeBoolean(p.numberingLevel != null); p.numberingLevel?.let(::writeInt)
        writeCollectionSize(p.runs.size)
        p.runs.forEach { writeInt(it.order); writeString(it.text, MAX_LARGE_TEXT_BYTES); writeBoolean(it.bold); writeBoolean(it.italic) }
        writeInt(p.hardPageBreakCount)
    }

    private fun DataInputStream.readParagraph(): DocxParagraph {
        val order = readInt(); val text = readString(MAX_LARGE_TEXT_BYTES); val styleId = readNullableString()
        val numberingId = readNullableString(); val numberingLevel = if (readBoolean()) readInt() else null
        val runs = List(readCollectionSize()) { DocxRun(readInt(), readString(MAX_LARGE_TEXT_BYTES), readBoolean(), readBoolean()) }
        val hardPageBreakCount = readInt()
        return DocxParagraph(order, text, styleId, numberingId, numberingLevel, runs, hardPageBreakCount)
    }

    private fun DataOutputStream.writeHeaderFooter(h: DocxHeaderFooter) {
        writeString(h.kind, MAX_SHORT_STRING_BYTES); writeInt(h.order); writeNullableString(h.relationshipId)
        writeCollectionSize(h.paragraphs.size); h.paragraphs.forEach { writeParagraph(it) }
    }

    private fun DataInputStream.readHeaderFooter(): DocxHeaderFooter {
        val kind = readString(MAX_SHORT_STRING_BYTES); val order = readInt(); val relationshipId = readNullableString()
        val paragraphs = List(readCollectionSize()) { readParagraph() }
        return DocxHeaderFooter(kind, order, relationshipId, paragraphs)
    }

    private fun DataInputStream.readDocx(): DocxStructuralResult {
        val paragraphs = List(readCollectionSize()) { readParagraph() }
        val tables = List(readCollectionSize()) {
            val order = readInt(); val styleId = readNullableString()
            val rows = List(readCollectionSize()) {
                val rowOrder = readInt()
                val cells = List(readCollectionSize()) { DocxTableCell(readInt(), readString(MAX_LARGE_TEXT_BYTES)) }
                DocxTableRow(rowOrder, cells)
            }
            DocxTable(order, styleId, rows)
        }
        val headers = List(readCollectionSize()) { readHeaderFooter() }
        val footers = List(readCollectionSize()) { readHeaderFooter() }
        val title = readNullableString(); val author = readNullableString(); val subject = readNullableString()
        val parsedCreated = if (readBoolean()) Instant.parse(readString(MAX_SHORT_STRING_BYTES)) else null
        val application = readNullableString(); val applicationVersion = readNullableString()
        val metadata = DocxMetadata(title, author, subject, parsedCreated, application, applicationVersion)
        val parts = List(readCollectionSize()) { OoxmlPartInventoryEntry(readString(MAX_SHORT_STRING_BYTES), readNullableString(), readLong()) }
        val relationshipCount = readInt()
        val relationshipTypes = readStrings()
        val mediaPartNames = readStrings()
        val producer = readProducer()
        val transformations = readTransformations()
        val completeness = enumValueOf<DerivativeCompletenessState>(readString(MAX_SHORT_STRING_BYTES))
        val warnings = readStrings()
        return DocxStructuralResult(paragraphs, tables, headers, footers, metadata, parts, relationshipCount, relationshipTypes, mediaPartNames, producer, transformations, completeness, warnings)
    }

    // ---- shared helpers -------------------------------------------------------------------------

    private fun DataOutputStream.writeProducer(value: DerivativeProducerIdentity) {
        writeString(value.pluginIdentity, MAX_SHORT_STRING_BYTES)
        writeString(value.pluginVersion, MAX_SHORT_STRING_BYTES)
        writeString(value.configurationIdentity, MAX_SHORT_STRING_BYTES)
        writeNullableString(value.adapterIdentity)
        writeNullableString(value.adapterVersion)
        writeNullableString(value.modelIdentity)
        writeNullableString(value.modelVersion)
    }

    private fun DataInputStream.readProducer() = DerivativeProducerIdentity(
        pluginIdentity = readString(MAX_SHORT_STRING_BYTES),
        pluginVersion = readString(MAX_SHORT_STRING_BYTES),
        configurationIdentity = readString(MAX_SHORT_STRING_BYTES),
        adapterIdentity = readNullableString(),
        adapterVersion = readNullableString(),
        modelIdentity = readNullableString(),
        modelVersion = readNullableString(),
    )

    private fun DataOutputStream.writeTransformations(value: List<DerivativeTransformation>) {
        writeCollectionSize(value.size)
        value.forEach { writeString(it.name, MAX_SHORT_STRING_BYTES) }
    }

    private fun DataInputStream.readTransformations(): List<DerivativeTransformation> =
        List(readCollectionSize()) { enumValueOf<DerivativeTransformation>(readString(MAX_SHORT_STRING_BYTES)) }

    private fun DataOutputStream.writeStrings(value: List<String>) {
        writeCollectionSize(value.size)
        value.forEach { writeString(it, MAX_LARGE_TEXT_BYTES) }
    }

    private fun DataInputStream.readStrings(): List<String> = List(readCollectionSize()) { readString(MAX_LARGE_TEXT_BYTES) }

    private fun DataOutputStream.writeString(value: String, maxBytes: Int) {
        val encoded = value.toByteArray(StandardCharsets.UTF_8)
        require(encoded.size <= maxBytes) { "string exceeds codec limit ($maxBytes bytes)" }
        writeInt(encoded.size)
        write(encoded)
    }

    private fun DataInputStream.readString(maxBytes: Int): String {
        val size = readInt()
        if (size !in 0..maxBytes) throw MalformedRepresentationException("invalid string length $size")
        val encoded = ByteArray(size).also(::readFully)
        return try {
            StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(java.nio.ByteBuffer.wrap(encoded))
                .toString()
        } catch (e: Exception) {
            throw MalformedRepresentationException("invalid UTF-8 in stored string")
        }
    }

    private fun DataOutputStream.writeNullableString(value: String?) {
        writeBoolean(value != null)
        value?.let { writeString(it, MAX_SHORT_STRING_BYTES) }
    }

    private fun DataInputStream.readNullableString(): String? = if (readBoolean()) readString(MAX_SHORT_STRING_BYTES) else null

    private fun DataInputStream.readCollectionSize(): Int {
        val size = readInt()
        if (size !in 0..MAX_COLLECTION_SIZE) throw MalformedRepresentationException("invalid collection size $size")
        return size
    }

    private fun DataOutputStream.writeCollectionSize(size: Int) {
        require(size in 0..MAX_COLLECTION_SIZE) { "collection exceeds codec limit" }
        writeInt(size)
    }
}
