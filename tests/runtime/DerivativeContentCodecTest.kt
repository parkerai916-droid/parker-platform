package parker.core.runtime

import java.nio.ByteBuffer
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import parker.core.interfaces.DerivativeContentEntry
import parker.core.interfaces.DerivativeGenerationId
import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.TierADerivativePayload
import parker.core.interfaces.TierADerivativePayloadFixtures

/**
 * Document Ingestion — Derivative Content Persistence and Retrieval.
 * Governed by `docs/architecture/DOCUMENT_INGESTION_DERIVATIVE_CONTENT_PERSISTENCE_RETRIEVAL_SCOPE_LOCK.md`
 * §6/§7/§8. Proves [DerivativeContentCodec] round-trips every governed
 * field of all four [TierADerivativePayload] shapes exactly, detects
 * corruption/truncation/tampering before trusting any field, and rejects an
 * unsupported representation version -- never silently substituting a
 * default or partially-decoded value.
 */
class DerivativeContentCodecTest {

    @Test
    fun `PDF payload round trips every governed field exactly`() {
        val entry = DerivativeContentEntry(
            DerivativeGenerationId("generation-pdf"),
            EvidenceArtifactId("source-pdf"),
            TierADerivativePayload.Pdf(TierADerivativePayloadFixtures.pdf()),
        )
        val decoded = DerivativeContentCodec.decode(DerivativeContentCodec.encode(entry))
        assertEquals(entry, decoded)
    }

    @Test
    fun `CSV payload round trips headers full rows delimiter quote and line ending exactly`() {
        val entry = DerivativeContentEntry(
            DerivativeGenerationId("generation-csv"),
            EvidenceArtifactId("source-csv"),
            TierADerivativePayload.Csv(TierADerivativePayloadFixtures.csv()),
        )
        val decoded = DerivativeContentCodec.decode(DerivativeContentCodec.encode(entry))
        assertEquals(entry, decoded)
    }

    @Test
    fun `EML payload round trips headers mime entities body text and attachment metadata -- never raw bytes`() {
        val entry = DerivativeContentEntry(
            DerivativeGenerationId("generation-eml"),
            EvidenceArtifactId("source-eml"),
            TierADerivativePayload.Eml(TierADerivativePayloadFixtures.eml(), childSourceCandidateCount = 1),
        )
        val decoded = DerivativeContentCodec.decode(DerivativeContentCodec.encode(entry))
        assertEquals(entry.derivativeGenerationId, decoded.derivativeGenerationId)
        assertEquals(entry.rootSourceEvidenceArtifactId, decoded.rootSourceEvidenceArtifactId)
        val decodedEml = (decoded.payload as TierADerivativePayload.Eml)
        assertEquals(1, decodedEml.childSourceCandidateCount)
        val original = TierADerivativePayloadFixtures.eml()
        val reconstructed = decodedEml.value
        // Every governed non-byte field survives exactly. ByteArray fields (Kotlin data class
        // equals() compares arrays by reference, not content, so these are asserted separately
        // below via assertContentEquals rather than folded into a single assertEquals(entry, decoded)).
        assertEquals(original.headers.map { Triple(it.name, it.value, it.rawRepresentation) }, reconstructed.headers.map { Triple(it.name, it.value, it.rawRepresentation) })
        assertEquals(original.from, reconstructed.from)
        assertEquals(original.to, reconstructed.to)
        assertEquals(original.cc, reconstructed.cc)
        assertEquals(original.rawDate, reconstructed.rawDate)
        assertEquals(original.parsedDate, reconstructed.parsedDate)
        assertEquals(original.subject, reconstructed.subject)
        assertEquals(original.messageId, reconstructed.messageId)
        assertEquals(original.mimeVersion, reconstructed.mimeVersion)
        assertEquals(original.contentType, reconstructed.contentType)
        assertEquals(original.mimeEntities, reconstructed.mimeEntities)
        assertEquals(original.bodyAlternatives.map { it.mimeEntityId to it.decodedText }, reconstructed.bodyAlternatives.map { it.mimeEntityId to it.decodedText })
        assertEquals(
            original.attachmentCandidates.map { it.mimeEntityId to it.sha256 },
            reconstructed.attachmentCandidates.map { it.mimeEntityId to it.sha256 },
        )
        assertEquals(original.producerIdentity, reconstructed.producerIdentity)
        assertEquals(original.transformationHistory, reconstructed.transformationHistory)
        assertEquals(original.completenessState, reconstructed.completenessState)
        assertEquals(original.warnings, reconstructed.warnings)
        // Never persisted (Scope Lock §7) -- reconstructed as empty, never the source bytes.
        assertContentEquals(ByteArray(0), reconstructed.bodyAlternatives.single().decodedBytes)
        assertContentEquals(ByteArray(0), reconstructed.attachmentCandidates.single().decodedBytes)
        assertContentEquals(ByteArray(0), reconstructed.headers.first().rawBytes)
    }

    @Test
    fun `DOCX payload round trips paragraphs tables headers footers and metadata exactly`() {
        val entry = DerivativeContentEntry(
            DerivativeGenerationId("generation-docx"),
            EvidenceArtifactId("source-docx"),
            TierADerivativePayload.Docx(TierADerivativePayloadFixtures.docx()),
        )
        val decoded = DerivativeContentCodec.decode(DerivativeContentCodec.encode(entry))
        assertEquals(entry, decoded)
    }

    @Test
    fun `encoding is deterministic for identical input`() {
        val entry = DerivativeContentEntry(
            DerivativeGenerationId("generation-deterministic"),
            EvidenceArtifactId("source-1"),
            TierADerivativePayload.Csv(TierADerivativePayloadFixtures.csv()),
        )
        assertContentEquals(DerivativeContentCodec.encode(entry), DerivativeContentCodec.encode(entry))
    }

    @Test
    fun `a single flipped byte in the body is detected by the trailing digest, never silently decoded`() {
        val entry = DerivativeContentEntry(
            DerivativeGenerationId("generation-tamper"),
            EvidenceArtifactId("source-1"),
            TierADerivativePayload.Csv(TierADerivativePayloadFixtures.csv()),
        )
        val encoded = DerivativeContentCodec.encode(entry)
        encoded[20] = (encoded[20] + 1).toByte()
        assertFailsWith<DerivativeContentCodec.MalformedRepresentationException> { DerivativeContentCodec.decode(encoded) }
    }

    @Test
    fun `truncated content is rejected, never partially decoded as valid`() {
        val entry = DerivativeContentEntry(
            DerivativeGenerationId("generation-truncated"),
            EvidenceArtifactId("source-1"),
            TierADerivativePayload.Pdf(TierADerivativePayloadFixtures.pdf()),
        )
        val encoded = DerivativeContentCodec.encode(entry)
        assertFailsWith<DerivativeContentCodec.MalformedRepresentationException> {
            DerivativeContentCodec.decode(encoded.copyOfRange(0, encoded.size - 40))
        }
    }

    @Test
    fun `content shorter than its own digest trailer is rejected`() {
        assertFailsWith<DerivativeContentCodec.MalformedRepresentationException> {
            DerivativeContentCodec.decode(ByteArray(10))
        }
    }

    @Test
    fun `unsupported representation version is rejected distinctly, not treated as corrupt`() {
        val entry = DerivativeContentEntry(
            DerivativeGenerationId("generation-version"),
            EvidenceArtifactId("source-1"),
            TierADerivativePayload.Csv(TierADerivativePayloadFixtures.csv()),
        )
        val encoded = DerivativeContentCodec.encode(entry).copyOf()
        // Locate the representation-version int: MAGIC(4) + ENVELOPE_VERSION(4) + id string + root
        // string + format byte(1), then 4 bytes of representation version -- computed the same way
        // FileSystemDerivativeGenerationStorageTest locates its own analogous offsets.
        val idBytes = entry.derivativeGenerationId.value.toByteArray().size
        val rootBytes = entry.rootSourceEvidenceArtifactId.value.toByteArray().size
        val versionOffset = 4 + 4 + 4 + idBytes + 4 + rootBytes + 1
        ByteBuffer.wrap(encoded, versionOffset, 4).putInt(99)
        // Re-sign with a fresh digest so this test isolates version rejection from digest rejection.
        val resigned = reSign(encoded)
        assertFailsWith<DerivativeContentCodec.UnsupportedRepresentationVersionException> { DerivativeContentCodec.decode(resigned) }
    }

    @Test
    fun `an unknown format kind byte is rejected as malformed`() {
        val entry = DerivativeContentEntry(
            DerivativeGenerationId("generation-format"),
            EvidenceArtifactId("source-1"),
            TierADerivativePayload.Csv(TierADerivativePayloadFixtures.csv()),
        )
        val encoded = DerivativeContentCodec.encode(entry).copyOf()
        val idBytes = entry.derivativeGenerationId.value.toByteArray().size
        val rootBytes = entry.rootSourceEvidenceArtifactId.value.toByteArray().size
        val formatOffset = 4 + 4 + 4 + idBytes + 4 + rootBytes
        encoded[formatOffset] = 99
        val resigned = reSign(encoded)
        assertFailsWith<DerivativeContentCodec.MalformedRepresentationException> { DerivativeContentCodec.decode(resigned) }
    }

    @Test
    fun `oversized entry is rejected at encode time, never silently persisted`() {
        val entry = DerivativeContentEntry(
            DerivativeGenerationId("generation-oversized"),
            EvidenceArtifactId("source-1"),
            TierADerivativePayload.Pdf(TierADerivativePayloadFixtures.pdf("x".repeat(70 * 1024 * 1024))),
        )
        assertFailsWith<IllegalArgumentException> { DerivativeContentCodec.encode(entry) }
    }

    // ---- Tier B OCR (TIER_B_OCR_DURABLE_REPRESENTATION_BOUNDS_DECISION.md) --------------------

    @Test
    fun `OCR payload round trips every governed field exactly`() {
        val entry = DerivativeContentEntry(
            DerivativeGenerationId("generation-ocr"),
            EvidenceArtifactId("source-ocr"),
            TierADerivativePayload.Ocr(TierADerivativePayloadFixtures.ocr()),
        )
        val decoded = DerivativeContentCodec.decode(DerivativeContentCodec.encode(entry))
        assertEquals(entry, decoded)
    }

    @Test
    fun `OCR partial-or-degraded payload round trips its own degradation reason exactly`() {
        val entry = DerivativeContentEntry(
            DerivativeGenerationId("generation-ocr-degraded"),
            EvidenceArtifactId("source-ocr"),
            TierADerivativePayload.Ocr(
                TierADerivativePayloadFixtures.ocr(
                    outcomeKind = parker.core.interfaces.OcrDerivativeOutcomeKind.PARTIAL_OR_DEGRADED,
                    degradationReason = "page 4 could not be fully processed",
                ),
            ),
        )
        val decoded = DerivativeContentCodec.decode(DerivativeContentCodec.encode(entry))
        assertEquals(entry, decoded)
    }

    @Test
    fun `OCR segment count exactly at the 200-entry bound is accepted, 201 is rejected at encode time`() {
        val atBound = TierADerivativePayloadFixtures.ocr(
            segments = (1..200).map { parker.core.interfaces.OcrRecognitionSegment("segment $it", parker.core.interfaces.TranscriptionFidelity.VERBATIM, it) },
        )
        val entry = DerivativeContentEntry(DerivativeGenerationId("generation-ocr-200"), EvidenceArtifactId("source-ocr"), TierADerivativePayload.Ocr(atBound))
        val decoded = DerivativeContentCodec.decode(DerivativeContentCodec.encode(entry))
        assertEquals(entry, decoded)

        val overBound = TierADerivativePayloadFixtures.ocr(
            segments = (1..201).map { parker.core.interfaces.OcrRecognitionSegment("segment $it", parker.core.interfaces.TranscriptionFidelity.VERBATIM, it) },
        )
        val overEntry = DerivativeContentEntry(DerivativeGenerationId("generation-ocr-201"), EvidenceArtifactId("source-ocr"), TierADerivativePayload.Ocr(overBound))
        assertFailsWith<IllegalArgumentException> { DerivativeContentCodec.encode(overEntry) }
    }

    @Test
    fun `OCR warning count over the 200-entry bound is rejected at encode time, never silently truncated`() {
        val overWarnings = TierADerivativePayloadFixtures.ocr().copy(warnings = (1..201).map { "warning $it" })
        val entry = DerivativeContentEntry(DerivativeGenerationId("generation-ocr-warn"), EvidenceArtifactId("source-ocr"), TierADerivativePayload.Ocr(overWarnings))
        assertFailsWith<IllegalArgumentException> { DerivativeContentCodec.encode(entry) }
    }

    @Test
    fun `OCR recognisedText over the 20 MiB bound is rejected at encode time`() {
        val oversized = TierADerivativePayloadFixtures.ocr(recognisedText = "x".repeat(21 * 1024 * 1024))
        val entry = DerivativeContentEntry(DerivativeGenerationId("generation-ocr-text"), EvidenceArtifactId("source-ocr"), TierADerivativePayload.Ocr(oversized))
        assertFailsWith<IllegalArgumentException> { DerivativeContentCodec.encode(entry) }
    }

    @Test
    fun `OCR individual segment text over the 20 MiB bound is rejected at encode time`() {
        val oversized = TierADerivativePayloadFixtures.ocr(
            segments = listOf(parker.core.interfaces.OcrRecognitionSegment("x".repeat(21 * 1024 * 1024), parker.core.interfaces.TranscriptionFidelity.VERBATIM, 1)),
        )
        val entry = DerivativeContentEntry(DerivativeGenerationId("generation-ocr-segtext"), EvidenceArtifactId("source-ocr"), TierADerivativePayload.Ocr(oversized))
        assertFailsWith<IllegalArgumentException> { DerivativeContentCodec.encode(entry) }
    }

    /** Recomputes and appends a fresh trailing SHA-256 digest over [bytes] minus its own last 32 bytes -- mirrors [DerivativeContentCodec.encode]'s own digest step exactly, for tests that mutate a field and must isolate that mutation from digest rejection. */
    private fun reSign(bytes: ByteArray): ByteArray {
        val body = bytes.copyOfRange(0, bytes.size - 32)
        val digest = java.security.MessageDigest.getInstance("SHA-256").digest(body)
        return body + digest
    }
}
