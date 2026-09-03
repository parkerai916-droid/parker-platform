package parker.core.runtime

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import kotlin.test.*
import parker.core.interfaces.*

class HumanFidelityReviewRecordCodecTest {
    @Test
    fun `version one encoding is deterministic and roundtrips all exact R6 facts`() {
        val record = HumanFidelityReviewFixture.review()
        val encoded = HumanFidelityReviewRecordCodec.encode(record)
        val decoded = HumanFidelityReviewRecordCodec.decode(encoded)
        assertContentEquals(encoded, HumanFidelityReviewRecordCodec.encode(decoded))
        assertEquals(record.reviewId, decoded.reviewId)
        assertEquals(record.target, decoded.target)
        assertEquals(record.reviewerPrincipalId, decoded.reviewerPrincipalId)
        assertEquals(2, decoded.discrepancyOccurrences.size)
        assertEquals(1, decoded.systematicPatterns.size)
        assertEquals(listOf(1, 5), decoded.discrepancyOccurrences.map { it.location.pageNumber })
        assertEquals(listOf("Kellee", "Kellee"), decoded.discrepancyOccurrences.map { it.location.originalProviderSubstring })
        assertTrue(decoded.discrepancyOccurrences.all { it.severity == FidelityDiscrepancySeverity.MATERIAL })
        assertTrue(decoded.discrepancyOccurrences.all { it.causeAssessment.state == FidelityCauseState.UNKNOWN })
        assertTrue(decoded.discrepancyOccurrences.all {
            (it.sourceResolution as HumanSourceResolution.ResolvedAgainstSource).assertedSourceValue == "Kellec"
        })
        assertEquals(HumanFidelityReviewFixture.HIGH_FIDELITY, decoded.descriptiveFidelity)
    }

    @Test
    fun `canonical collection ordering is independent from caller iteration order`() {
        assertContentEquals(
            HumanFidelityReviewRecordCodec.encode(HumanFidelityReviewFixture.review(reverseInput = false)),
            HumanFidelityReviewRecordCodec.encode(HumanFidelityReviewFixture.review(reverseInput = true)),
        )
    }

    @Test
    fun `non-BMP code-point location roundtrips exactly`() {
        val decoded = HumanFidelityReviewRecordCodec.decode(HumanFidelityReviewRecordCodec.encode(HumanFidelityReviewFixture.review()))
        assertEquals(6, decoded.discrepancyOccurrences.single { it.location.pageNumber == 1 }.location.codePointLength)
    }

    @Test
    fun `supersession and adjudication references roundtrip`() {
        val predecessor = HumanFidelityReviewId("review-${"a".repeat(64)}")
        val other = HumanFidelityReviewId("review-${"b".repeat(64)}")
        val selected = HumanFidelityReviewId("review-${"c".repeat(64)}")
        val record = HumanFidelityReviewFixture.review(
            supersession = HumanFidelityReviewSupersession(predecessor, HumanFidelityReviewFixture.target),
            adjudication = HumanFidelityReviewAdjudicationReference(
                listOf(other, selected), selected, HumanFidelityReviewFixture.target,
            ),
        )
        val decoded = HumanFidelityReviewRecordCodec.decode(HumanFidelityReviewRecordCodec.encode(record))
        assertEquals(predecessor, decoded.supersession?.predecessorReviewId)
        assertEquals(setOf(other, selected), decoded.adjudication?.conflictingReviewIds)
        assertEquals(selected, decoded.adjudication?.selectedReviewId)
    }

    @Test
    fun `unknown version truncation trailing bytes and integrity corruption fail closed`() {
        val encoded = HumanFidelityReviewRecordCodec.encode(HumanFidelityReviewFixture.review())
        assertFailsWith<UnsupportedHumanFidelityReviewRepresentationVersionException> {
            HumanFidelityReviewRecordCodec.decode(withVersion(encoded, 99))
        }
        assertFails { HumanFidelityReviewRecordCodec.decode(encoded.copyOf(encoded.size - 1)) }
        assertFails { HumanFidelityReviewRecordCodec.decode(encoded + 0) }
        assertFails { HumanFidelityReviewRecordCodec.decode(encoded.copyOf().also { it[20] = (it[20].toInt() xor 1).toByte() }) }
    }

    @Test
    fun `excessive string and collection lengths fail before allocation`() {
        val encoded = HumanFidelityReviewRecordCodec.encode(HumanFidelityReviewFixture.review())
        val excessiveString = mutatePayload(encoded) { payload -> ByteBuffer.wrap(payload).putInt(0, Int.MAX_VALUE) }
        assertFails { HumanFidelityReviewRecordCodec.decode(excessiveString) }

        val excessiveCount = mutatePayload(encoded) { payload ->
            val marker = "FULL_GENERATION".toByteArray(StandardCharsets.UTF_8)
            val at = payload.indexOf(marker)
            require(at >= 0)
            ByteBuffer.wrap(payload).putInt(at + marker.size, 1_001)
        }
        assertFails { HumanFidelityReviewRecordCodec.decode(excessiveCount) }
    }

    @Test
    fun `unknown enum and nested semantic identity contradictions fail despite valid outer integrity`() {
        val encoded = HumanFidelityReviewRecordCodec.encode(HumanFidelityReviewFixture.review())
        assertFails { HumanFidelityReviewRecordCodec.decode(replaceAscii(encoded, "MATERIAL", "INVALIDX")) }

        val record = HumanFidelityReviewFixture.review()
        val discrepancy = record.discrepancyOccurrences.first().discrepancyId.value
        assertFails { HumanFidelityReviewRecordCodec.decode(replaceAscii(encoded, discrepancy, discrepancy.dropLast(1) + alternate(discrepancy.last()))) }

        val pattern = record.systematicPatterns.single().patternId.value
        assertFails { HumanFidelityReviewRecordCodec.decode(replaceAscii(encoded, pattern, pattern.dropLast(1) + alternate(pattern.last()))) }

        assertFails { HumanFidelityReviewRecordCodec.decode(replaceAscii(encoded, "Kellee", "Xellee")) }
        assertFails { HumanFidelityReviewRecordCodec.decode(replaceAscii(encoded, "Kellec", "Xellec")) }

        val review = record.reviewId.value
        assertFails { HumanFidelityReviewRecordCodec.decode(replaceAscii(encoded, review, review.dropLast(1) + alternate(review.last()))) }
    }

    @Test
    fun `audit codec is canonical versioned and rejects malformed state`() {
        val record = HumanFidelityReviewFixture.auditRecord()
        val encoded = HumanFidelityGovernanceAuditCodec.encode(record)
        assertEquals(record, HumanFidelityGovernanceAuditCodec.decode(encoded))
        assertContentEquals(encoded, HumanFidelityGovernanceAuditCodec.encode(HumanFidelityGovernanceAuditCodec.decode(encoded)))
        assertFailsWith<UnsupportedHumanFidelityAuditRepresentationVersionException> {
            HumanFidelityGovernanceAuditCodec.decode(withVersion(encoded, 2))
        }
        assertFails { HumanFidelityGovernanceAuditCodec.decode(encoded.copyOf(encoded.size - 2)) }
        assertFails { HumanFidelityGovernanceAuditCodec.decode(encoded + 1) }
    }

    private fun replaceAscii(envelope: ByteArray, before: String, after: String): ByteArray {
        require(before.length == after.length)
        return mutatePayload(envelope) { payload ->
            val needle = before.toByteArray(StandardCharsets.UTF_8)
            val at = payload.indexOf(needle)
            require(at >= 0) { "fixture value not found: $before" }
            after.toByteArray(StandardCharsets.UTF_8).copyInto(payload, at)
        }
    }

    private fun mutatePayload(envelope: ByteArray, mutation: (ByteArray) -> Unit): ByteArray {
        val result = envelope.copyOf()
        val payloadSize = ByteBuffer.wrap(result).getInt(8)
        val payload = result.copyOfRange(12, 12 + payloadSize)
        mutation(payload); payload.copyInto(result, 12)
        recomputeEnvelopeIntegrity(result)
        return result
    }

    private fun withVersion(envelope: ByteArray, version: Int): ByteArray = envelope.copyOf().also {
        ByteBuffer.wrap(it).putInt(4, version)
        recomputeEnvelopeIntegrity(it)
    }

    private fun recomputeEnvelopeIntegrity(envelope: ByteArray) {
        val buffer = ByteBuffer.wrap(envelope)
        val magic = buffer.getInt(0)
        val version = buffer.getInt(4)
        val payloadSize = buffer.getInt(8)
        val payload = envelope.copyOfRange(12, 12 + payloadSize)
        val bound = ByteBuffer.allocate(8 + payloadSize).putInt(magic).putInt(version).put(payload).array()
        MessageDigest.getInstance("SHA-256").digest(bound).copyInto(envelope, 12 + payloadSize)
    }

    private fun ByteArray.indexOf(needle: ByteArray): Int = indices.firstOrNull { start ->
        start + needle.size <= size && needle.indices.all { this[start + it] == needle[it] }
    } ?: -1

    private fun alternate(value: Char): Char = if (value == 'a') 'b' else 'a'
}
