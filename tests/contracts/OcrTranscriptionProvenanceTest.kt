package parker.core.interfaces

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull

class OcrTranscriptionProvenanceTest {
    private val digestA = OcrSha256Digest("a".repeat(64))
    private val digestB = OcrSha256Digest("b".repeat(64))

    @Test
    fun `page scope is one-based immutable and deterministically sorted`() {
        val input = mutableListOf(3, 1, 2)
        val scope = OcrPageScope(input)
        input.clear()

        assertEquals(listOf(1, 2, 3), scope.pageNumbers)
        assertFailsWith<UnsupportedOperationException> { (scope.pageNumbers as MutableList).add(4) }
    }

    @Test
    fun `page scope represents a known empty scope and rejects zero negative and duplicate pages`() {
        assertEquals(emptyList(), OcrPageScope(emptyList()).pageNumbers)
        assertFailsWith<IllegalArgumentException> { OcrPageScope(listOf(0)) }
        assertFailsWith<IllegalArgumentException> { OcrPageScope(listOf(-1)) }
        assertFailsWith<IllegalArgumentException> { OcrPageScope(listOf(1, 1)) }
    }

    @Test
    fun `all five governed page outcome values are representable`() {
        assertEquals(
            listOf(
                "TRANSCRIBED",
                "TRANSCRIBED_WITH_QUALIFICATIONS",
                "ILLEGIBLE_OR_NO_RECOGNISABLE_CONTENT",
                "FAILED",
                "NOT_RETURNED",
            ),
            OcrPageOutcomeKind.entries.map { it.name },
        )
    }

    @Test
    fun `page outcome retains bounded structured reason warnings and uncertainty`() {
        val span = OcrUncertaintySpan(2, 4, 9, OcrUncertaintyKind.UNCERTAIN, "Characters are uncertain")
        val outcome = OcrPageOutcome(
            pageNumber = 2,
            outcome = OcrPageOutcomeKind.TRANSCRIBED_WITH_QUALIFICATIONS,
            reason = OcrPageOutcomeReason("LOW_CONFIDENCE", "Provider marked a portion uncertain"),
            warnings = listOf("Review against the source"),
            uncertaintySpans = listOf(span),
        )

        assertEquals("LOW_CONFIDENCE", outcome.reason?.classification)
        assertEquals(listOf(span), outcome.uncertaintySpans)
        assertFailsWith<IllegalArgumentException> { OcrPageOutcome(0, OcrPageOutcomeKind.FAILED) }
        assertFailsWith<IllegalArgumentException> { OcrPageOutcomeReason("not bounded machine form") }
        assertFailsWith<IllegalArgumentException> { OcrPageOutcomeReason("UNKNOWN") }
        assertFailsWith<IllegalArgumentException> {
            OcrPageOutcome(1, OcrPageOutcomeKind.FAILED, warnings = listOf("x".repeat(4_097)))
        }
    }

    @Test
    fun `uncertain and illegible spans are valid character offsets over returned page text`() {
        assertEquals(OcrUncertaintyKind.UNCERTAIN, OcrUncertaintySpan(1, 0, 1, OcrUncertaintyKind.UNCERTAIN, "uncertain").kind)
        assertEquals(OcrUncertaintyKind.ILLEGIBLE, OcrUncertaintySpan(1, 1, 3, OcrUncertaintyKind.ILLEGIBLE, "illegible").kind)
    }

    @Test
    fun `uncertainty span rejects invalid page offsets and disclosure`() {
        assertFailsWith<IllegalArgumentException> { OcrUncertaintySpan(0, 0, 1, OcrUncertaintyKind.UNCERTAIN, "x") }
        assertFailsWith<IllegalArgumentException> { OcrUncertaintySpan(1, -1, 1, OcrUncertaintyKind.UNCERTAIN, "x") }
        assertFailsWith<IllegalArgumentException> { OcrUncertaintySpan(1, 2, 2, OcrUncertaintyKind.UNCERTAIN, "x") }
        assertFailsWith<IllegalArgumentException> { OcrUncertaintySpan(1, 3, 2, OcrUncertaintyKind.UNCERTAIN, "x") }
        assertFailsWith<IllegalArgumentException> { OcrUncertaintySpan(1, 0, 1, OcrUncertaintyKind.UNCERTAIN, " ") }
    }

    @Test
    fun `page accounting retains requested submitted and returned scopes independently`() {
        val accounting = OcrPageAccounting(
            requestedScope = OcrPageScope(listOf(1, 2, 3)),
            submittedScope = OcrPageScope(listOf(1, 2, 3)),
            returnedScope = OcrPageScope(listOf(1, 2)),
            pageOutcomes = listOf(OcrPageOutcome(1, OcrPageOutcomeKind.TRANSCRIBED)),
        )

        assertEquals(listOf(1, 2, 3), accounting.requestedScope.pageNumbers)
        assertEquals(listOf(1, 2), accounting.returnedScope.pageNumbers)
    }

    @Test
    fun `page accounting can truthfully retain a known empty returned scope`() {
        val accounting = OcrPageAccounting(
            requestedScope = OcrPageScope(listOf(1)),
            submittedScope = OcrPageScope(listOf(1)),
            returnedScope = OcrPageScope(emptyList()),
            pageOutcomes = listOf(OcrPageOutcome(1, OcrPageOutcomeKind.NOT_RETURNED)),
        )

        assertEquals(emptyList(), accounting.returnedScope.pageNumbers)
    }

    @Test
    fun `byte-exact processing provenance retains source identity and has no transformation`() {
        val provenance = byteExactProvenance()

        assertEquals(EvidenceArtifactId("evidence-1"), provenance.sourceEvidenceArtifactId)
        assertEquals("direct-pdf-v1", provenance.processingProfileIdentity)
        assertEquals(digestA, provenance.sourceManifestSha256)
        assertEquals(digestA, provenance.representationSha256)
        assertNull(provenance.materialTransformation)
    }

    @Test
    fun `transformed processing provenance retains material transformation parameters`() {
        val transformation = OcrMaterialTransformation(
            mechanismIdentity = "pdf-rasteriser",
            mechanismVersion = "1.2.3",
            sourcePageScope = OcrPageScope(listOf(2)),
            dpi = 300,
            dimensions = OcrPixelDimensions(2480, 3508),
            rotationDegrees = 90.0,
            colourMode = "GREYSCALE",
            scaleX = 1.0,
            scaleY = 1.0,
            crop = OcrCropParameters(10, 20, 2000, 3000),
            compression = "PNG_LOSSLESS",
        )
        val provenance = byteExactProvenance().copy(
            representationMediaType = "image/png",
            representationByteLength = 512,
            representationSha256 = digestB,
            byteExactCopy = false,
            processingProfileIdentity = "raster-300dpi-v1",
            materialTransformation = transformation,
        )

        assertFalse(provenance.byteExactCopy)
        assertEquals(300, provenance.materialTransformation?.dpi)
        assertEquals(OcrPageScope(listOf(2)), provenance.materialTransformation?.sourcePageScope)
    }

    @Test
    fun `processing provenance rejects invalid digests lengths and transformation masquerade`() {
        assertFailsWith<IllegalArgumentException> { OcrSha256Digest("A".repeat(64)) }
        assertFailsWith<IllegalArgumentException> { OcrSha256Digest("a".repeat(63)) }
        assertFailsWith<IllegalArgumentException> { byteExactProvenance().copy(sourceByteLength = 0) }
        assertFailsWith<IllegalArgumentException> { byteExactProvenance().copy(representationByteLength = -1) }
        assertFailsWith<IllegalArgumentException> {
            byteExactProvenance().copy(
                materialTransformation = OcrMaterialTransformation("x", "1", OcrPageScope(listOf(1))),
            )
        }
        assertFailsWith<IllegalArgumentException> { byteExactProvenance().copy(byteExactCopy = false) }
    }

    @Test
    fun `provider provenance represents a present snapshot and truthful not-exposed snapshot`() {
        val present = providerProvenance(OcrModelSnapshot.Present("snapshot-2026-08-26"))
        val notExposed = providerProvenance(OcrModelSnapshot.NotExposed)

        assertEquals("provider-model-id", present.providerReportedModelIdentifier)
        assertEquals("snapshot-2026-08-26", assertIs<OcrModelSnapshot.Present>(present.modelSnapshot).value)
        assertIs<OcrModelSnapshot.NotExposed>(notExposed.modelSnapshot)
    }

    @Test
    fun `provider provenance rejects missing fabricated or oversized mandatory identity`() {
        assertFailsWith<IllegalArgumentException> { providerProvenance(OcrModelSnapshot.NotExposed).copy(providerReportedModelIdentifier = "") }
        assertFailsWith<IllegalArgumentException> { providerProvenance(OcrModelSnapshot.NotExposed).copy(providerReportedModelIdentifier = "unknown") }
        assertFailsWith<IllegalArgumentException> { OcrModelSnapshot.Present("unknown") }
        assertFailsWith<IllegalArgumentException> { providerProvenance(OcrModelSnapshot.NotExposed).copy(providerCorrelationIdentifier = "unknown") }
        assertFailsWith<IllegalArgumentException> { providerProvenance(OcrModelSnapshot.NotExposed).copy(providerCorrelationIdentifier = "x".repeat(1_025)) }
    }

    @Test
    fun `digested configuration is exact and historical profiles never fabricate it`() {
        val configuration = OcrTranscriptionConfiguration.DigestedConfiguration(
            "openai-literal-page-transcription-v2", OcrSha256Digest("a".repeat(64)), OcrSha256Digest("b".repeat(64)),
        )
        val provider = OcrProviderProvenance(
            "provider", "adapter", "1.1.0", configuration.profileId, "model", OcrModelSnapshot.NotExposed,
            "response", configuration,
        )
        assertEquals(configuration, provider.transcriptionConfiguration)
        assertIs<OcrTranscriptionConfiguration.HistoricalProfileOnly>(providerProvenance(OcrModelSnapshot.NotExposed).transcriptionConfiguration)
        assertFailsWith<IllegalArgumentException> { provider.copy(transcriptionConfigurationProfile = "different-profile") }
    }

    @Test
    fun `request result and durable OCR payload have optional provider-neutral homes without changing legacy construction`() {
        val processing = byteExactProvenance()
        val provider = providerProvenance(OcrModelSnapshot.NotExposed)
        val accounting = OcrPageAccounting(
            OcrPageScope(listOf(1)),
            OcrPageScope(listOf(1)),
            OcrPageScope(listOf(1)),
            listOf(OcrPageOutcome(1, OcrPageOutcomeKind.TRANSCRIBED)),
        )
        val request = OcrRecognitionRequest(EvidenceArtifactId("evidence-1"), byteArrayOf(1), "application/pdf", requestedPageScope = OcrPageScope(listOf(1)), processingProvenance = processing)
        val result = OcrRecognitionResult(
            "text",
            TranscriptionFidelity.UNVERIFIED_LITERAL_TRANSCRIPTION,
            OcrRecognitionIdentity("mechanism", "profile"),
            recognisedAt = Instant.EPOCH,
            pageAccounting = accounting,
            processingProvenance = processing,
            providerProvenance = provider,
        )
        val legacyRequest = OcrRecognitionRequest(EvidenceArtifactId("legacy"), byteArrayOf(2), "image/png")

        assertEquals(processing, request.processingProvenance)
        assertEquals(provider, result.providerProvenance)
        assertNull(legacyRequest.requestedPageScope)
        assertNull(legacyRequest.processingProvenance)
    }

    @Test
    fun `provider-neutral contract source contains no provider-specific vocabulary`() {
        val source = java.io.File("src/interfaces/OcrTranscriptionProvenance.kt").readText()
        assertFalse(source.contains("OpenAI", ignoreCase = true))
        assertFalse(source.contains("Responses API", ignoreCase = true))
    }

    private fun byteExactProvenance() = OcrProcessingProvenance(
        sourceEvidenceArtifactId = EvidenceArtifactId("evidence-1"),
        sourceManifestSha256 = digestA,
        sourceMediaType = "application/pdf",
        sourceByteLength = 1_024,
        requestedPageScope = OcrPageScope(listOf(1, 2)),
        submittedPageScope = OcrPageScope(listOf(1, 2)),
        representationMediaType = "application/pdf",
        representationByteLength = 1_024,
        representationSha256 = digestA,
        byteExactCopy = true,
        processingProfileIdentity = "direct-pdf-v1",
        createdAt = Instant.parse("2026-08-26T00:00:00Z"),
    )

    private fun providerProvenance(snapshot: OcrModelSnapshot) = OcrProviderProvenance(
        providerIdentity = "provider",
        adapterIdentity = "adapter",
        adapterVersion = "1.0.0",
        transcriptionConfigurationProfile = "literal-transcription-v1",
        providerReportedModelIdentifier = "provider-model-id",
        modelSnapshot = snapshot,
        providerCorrelationIdentifier = "response-correlation-1",
    )
}
