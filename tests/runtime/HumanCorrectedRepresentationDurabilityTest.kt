package parker.core.runtime

import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.io.TempDir
import parker.core.interfaces.*
import kotlin.test.*

class HumanCorrectedRepresentationDurabilityTest {
    @TempDir lateinit var directory: Path

    @Test
    fun `codec is deterministic versioned and rejects corruption and trailing bytes`() {
        val representation = representation()
        val first = HumanCorrectedRepresentationCodec.encode(representation)
        val reordered = HumanCorrectedRepresentationCodec.encode(representation.copy(proposals = representation.proposals.reversed()))
        assertContentEquals(first, reordered)
        assertEquals(representation, HumanCorrectedRepresentationCodec.decode(first))
        assertFails { HumanCorrectedRepresentationCodec.decode(first + 0) }
        assertFails { HumanCorrectedRepresentationCodec.decode(first.copyOf(12)) }
        assertFails { HumanCorrectedRepresentationCodec.decode(first.copyOf().also { it[7] = 2 }) }
    }

    @Test
    fun `storage and audit are create-once restart-safe and fail closed on tampering`() = runTest {
        val root = Files.createDirectories(directory.resolve("corrected"))
        val auditRoot = Files.createDirectories(directory.resolve("audit"))
        val value = representation()
        val storage = FileSystemHumanCorrectedRepresentationStorage(root)
        assertEquals(HumanCorrectedRepresentationPrepareResult.Prepared, storage.prepare(value))
        storage.publishPrepared(value.derivativeGenerationId)
        assertEquals(HumanCorrectedRepresentationPrepareResult.AlreadyPublished, storage.prepare(value))
        assertEquals(value, FileSystemHumanCorrectedRepresentationStorage(root).retrieve(value.derivativeGenerationId))

        val audit = FileSystemHumanCorrectionAudit(auditRoot)
        val event = audit(value, HumanCorrectionAuditEventType.CORRECTED_REPRESENTATION_PUBLISHED)
        audit.append(event); audit.append(event)
        assertEquals(listOf(event), FileSystemHumanCorrectionAudit(auditRoot).listForRepresentation(value.derivativeGenerationId))
        val auditFile = Files.list(auditRoot).use { it.iterator().next() }
        Files.writeString(auditFile, "corrupt")
        assertFails { FileSystemHumanCorrectionAudit(auditRoot).listForRepresentation(value.derivativeGenerationId) }
    }

    private fun representation(): HumanCorrectedRegionTranscription {
        val review = HumanFidelityReviewFixture.review()
        val time = Instant.parse("2026-09-04T00:00:00Z")
        val proposals = review.discrepancyOccurrences.map { discrepancy ->
            val source = discrepancy.sourceResolution as HumanSourceResolution.ResolvedAgainstSource
            val id = HumanTranscriptionCorrectionProposal.deriveId(review.reviewId, discrepancy.discrepancyId, review.target,
                "Kellee", source.assertedSourceValue, review.reviewerPrincipalId, time, "Exact accepted source resolution")
            HumanTranscriptionCorrectionProposal(id, review.reviewId, discrepancy.discrepancyId, review.target,
                "Kellee", source.assertedSourceValue, review.reviewerPrincipalId, time, "Exact accepted source resolution")
        }
        val proposalIds = proposals.map { it.proposalId }
        val acceptanceId = HumanTranscriptionCorrectionAcceptance.deriveId(review.reviewId, review.target, proposalIds,
            review.reviewerPrincipalId, time)
        val acceptance = HumanTranscriptionCorrectionAcceptance(acceptanceId, review.reviewId, review.target, proposalIds,
            review.reviewerPrincipalId, time, HUMAN_TRANSCRIPTION_CORRECTION_PURPOSE)
        val blocks = listOf("Michael Gary Kellec", "unchanged", "unchanged", "unchanged", "Michael Gary Kellec")
        val digest = HumanCorrectedRegionTranscription.contentDigest(blocks)
        val id = HumanCorrectedRegionTranscription.deriveGenerationId(review.target, review.reviewId, acceptance, digest)
        return HumanCorrectedRegionTranscription(1, id, target=review.target, reviewId=review.reviewId,
            proposals=proposals, acceptance=acceptance, correctedTranscriptionBlocks=blocks,
            correctedContentSha256=digest, createdAt=time)
    }

    private fun audit(r: HumanCorrectedRegionTranscription, type: HumanCorrectionAuditEventType) = HumanCorrectionAuditRecord(
        HumanCorrectionAuditRecord.deriveId(type, r.acceptance.acceptingPrincipalId, r.derivativeGenerationId,
            r.target, r.reviewId, r.acceptance.acceptanceId, r.correctedContentSha256), type, r.createdAt,
        r.acceptance.acceptingPrincipalId, r.derivativeGenerationId, r.target, r.reviewId, r.acceptance.acceptanceId,
        r.correctedContentSha256,
    )
}
