package parker.core.interfaces

import java.time.Instant

/**
 * Document Ingestion — Derivative Content Persistence and Retrieval. Shared,
 * realistic fixture builders for each of the four governed
 * [TierADerivativePayload] structural result shapes, reused across
 * [parker.core.runtime.DerivativeContentCodec], [parker.core.runtime.FileSystemDerivativeContentStorage],
 * [parker.core.runtime.TierAContentRetrievalCoordinator], and
 * [parker.composition.OwnerUiEvidenceRuntimeAdapter] tests -- never a
 * re-implementation of any specialist itself, just literal, governed field
 * values every field in the codec must faithfully round trip.
 */
object TierADerivativePayloadFixtures {
    val PRODUCER = DerivativeProducerIdentity(
        pluginIdentity = "fixture-parser",
        pluginVersion = "1.0",
        configurationIdentity = "fixture-config-v1",
        adapterIdentity = "fixture-adapter",
        adapterVersion = "2.0",
        modelIdentity = null,
        modelVersion = null,
    )

    fun pdf(documentText: String = "Sample PDF document text with a Māori macron ā for UTF-8 fidelity.") = PdfStructuralResult(
        documentText = documentText,
        pageCount = 3,
        pageTextAssociationAvailable = false,
        metadata = listOf(PdfMetadataValue("Title", "Fixture PDF", "STRING"), PdfMetadataValue("Author", "Fixture Author", "STRING")),
        embeddedResources = listOf(EmbeddedResourceObservation("image1.png", "image/png"), EmbeddedResourceObservation(null, null)),
        producerIdentity = PRODUCER,
        transformationHistory = listOf(DerivativeTransformation.STRUCTURAL_PARSING),
        completenessState = DerivativeCompletenessState.ACCOUNTED_FOR,
        warnings = listOf("page 2 contains a rotated image"),
    )

    fun csv() = CsvStructuralResult(
        headers = listOf("name", "amount", "notes"),
        rows = listOf(listOf("Alice", "10", "first \"quoted\" note"), listOf("Bob", "20", "line\nbreak")),
        delimiter = ',',
        quoteCharacter = '"',
        lineEnding = "\r\n",
        producerIdentity = PRODUCER,
        transformationHistory = listOf(DerivativeTransformation.STRUCTURAL_PARSING),
        completenessState = DerivativeCompletenessState.ACCOUNTED_FOR,
        warnings = listOf("row 2 contains an embedded newline"),
    )

    fun eml() = EmlStructuralResult(
        headers = listOf(EmlHeader("Subject", "Fixture subject", ByteArray(0), "Fixture subject"), EmlHeader("From", "alice@example.com", ByteArray(0), "alice@example.com")),
        from = "alice@example.com",
        to = "bob@example.com",
        cc = null,
        rawDate = "Mon, 1 Jan 2026 00:00:00 +0000",
        parsedDate = Instant.parse("2026-01-01T00:00:00Z"),
        subject = "Fixture subject <script>alert(1)</script>",
        messageId = "<fixture@example.com>",
        mimeVersion = "1.0",
        contentType = "multipart/mixed",
        mimeEntities = listOf(
            EmlMimeEntity("entity-1", null, 0, "text/plain", null, "7bit", null, "utf-8", listOf("entity-2")),
            EmlMimeEntity("entity-2", "entity-1", 1, "application/pdf", "attachment", "base64", "report.pdf", null, emptyList()),
        ),
        bodyAlternatives = listOf(EmlBodyAlternative("entity-1", "text/plain", "utf-8", ByteArray(0), "Hello world, with a Māori macron ā.")),
        attachmentCandidates = listOf(
            EmlAttachmentCandidate(
                "entity-2", "entity-1", "report.pdf", "application/pdf", "attachment", "base64", null,
                ByteArray(0), 1024L, "abc123", listOf(DerivativeTransformation.STRUCTURAL_PARSING),
            ),
        ),
        producerIdentity = PRODUCER,
        transformationHistory = listOf(DerivativeTransformation.STRUCTURAL_PARSING),
        completenessState = DerivativeCompletenessState.ACCOUNTED_FOR,
        warnings = emptyList(),
    )

    fun docx() = DocxStructuralResult(
        paragraphs = listOf(
            DocxParagraph(0, "First paragraph", "Heading1", null, null, listOf(DocxRun(0, "First paragraph", true, false)), 0),
            DocxParagraph(1, "Second paragraph", null, "numId1", 0, listOf(DocxRun(0, "Second ", false, true), DocxRun(1, "paragraph", false, false)), 1),
        ),
        tables = listOf(DocxTable(0, "TableGrid", listOf(DocxTableRow(0, listOf(DocxTableCell(0, "cell-a"), DocxTableCell(1, "cell-b")))))),
        headers = listOf(DocxHeaderFooter("default", 0, "rId1", listOf(DocxParagraph(0, "Header text", null, null, null, emptyList(), 0)))),
        footers = listOf(DocxHeaderFooter("default", 0, "rId2", listOf(DocxParagraph(0, "Footer text", null, null, null, emptyList(), 0)))),
        metadata = DocxMetadata("Title", "Author", "Subject", Instant.parse("2026-01-01T00:00:00Z"), "Microsoft Word", "16.0"),
        parts = listOf(OoxmlPartInventoryEntry("word/document.xml", "application/xml", 4096L), OoxmlPartInventoryEntry("word/media/image1.png", null, 2048L)),
        relationshipCount = 3,
        relationshipTypes = listOf("styles", "numbering"),
        mediaPartNames = listOf("word/media/image1.png"),
        producerIdentity = PRODUCER,
        transformationHistory = listOf(DerivativeTransformation.STRUCTURAL_PARSING),
        completenessState = DerivativeCompletenessState.ACCOUNTED_FOR,
        warnings = emptyList(),
    )

    /** Tier B OCR producer identity -- unlike [PRODUCER], carries the mandatory modelIdentity/modelVersion pair (Tier B scope lock §11). */
    val OCR_PRODUCER = DerivativeProducerIdentity(
        pluginIdentity = "docling",
        pluginVersion = "2.121.0",
        configurationIdentity = "docling-bridge-v1",
        modelIdentity = "rapidocr-onnxruntime:PP-OCRv6_rec_small",
        modelVersion = "sha256:" + "a".repeat(64),
    )

    fun ocr(
        recognisedText: String = "Recognised text with a Māori macron ā for UTF-8 fidelity.",
        fidelity: TranscriptionFidelity = TranscriptionFidelity.VERBATIM,
        outcomeKind: OcrDerivativeOutcomeKind = OcrDerivativeOutcomeKind.RECOGNISED,
        degradationReason: String? = null,
        segments: List<OcrRecognitionSegment> = listOf(OcrRecognitionSegment("segment one", TranscriptionFidelity.VERBATIM, 1)),
    ) = OcrDerivativeExtractedResult(
        recognisedText = recognisedText,
        fidelity = fidelity,
        outcomeKind = outcomeKind,
        degradationReason = degradationReason,
        warnings = listOf("page 2 low scan quality"),
        segments = segments,
        producerIdentity = OCR_PRODUCER,
        transformationHistory = listOf(DerivativeTransformation.OCR, DerivativeTransformation.MODEL_INFERENCE),
        completenessState = DerivativeCompletenessState.ACCOUNTED_FOR_WITH_QUALIFICATIONS,
    )
}
