package parker.core.runtime

import java.io.*
import java.time.Instant
import parker.core.interfaces.*

internal object HumanCorrectedRepresentationCodec {
    private const val MAGIC = 0x48435231
    private const val VERSION = 1
    const val MAX_BYTES = 8_000_000

    fun encode(value: HumanCorrectedRegionTranscription): ByteArray = ByteArrayOutputStream().use { out ->
        DataOutputStream(out).use { data ->
            data.writeInt(MAGIC); data.writeInt(VERSION)
            data.writeUTF(value.derivativeGenerationId.value); data.writeTarget(value.target)
            data.writeUTF(value.reviewId.value); data.writeInt(value.proposals.size)
            value.proposals.sortedBy { it.proposalId.value }.forEach { data.writeProposal(it) }
            data.writeAcceptance(value.acceptance)
            data.writeInt(value.correctedTranscriptionBlocks.size)
            value.correctedTranscriptionBlocks.forEach { data.writeBounded(it) }
            data.writeUTF(value.correctedContentSha256.value); data.writeUTF(value.createdAt.toString())
            data.writeUTF(value.representationKind); data.writeUTF(value.producerIdentity); data.writeUTF(value.schemaIdentity)
        }; out.toByteArray()
    }

    fun decode(bytes: ByteArray): HumanCorrectedRegionTranscription {
        require(bytes.size <= MAX_BYTES)
        return DataInputStream(ByteArrayInputStream(bytes)).use { data ->
            require(data.readInt() == MAGIC); require(data.readInt() == VERSION)
            val id = DerivativeGenerationId(data.readUTF()); val target = data.readTarget()
            val reviewId = HumanFidelityReviewId(data.readUTF())
            val proposals = List(data.readCount()) { data.readProposal() }
            val acceptance = data.readAcceptance()
            val blocks = List(data.readCount()) { data.readBounded() }
            val content = OcrSha256Digest(data.readUTF()); val createdAt = Instant.parse(data.readUTF())
            val kind = data.readUTF(); val producer = data.readUTF(); val schema = data.readUTF()
            require(data.read() == -1) { "Trailing corrected-representation bytes" }
            HumanCorrectedRegionTranscription(1, id, kind, target, reviewId, proposals, acceptance,
                blocks, content, createdAt, producer, schema)
        }
    }

    private fun DataOutputStream.writeTarget(t: HumanFidelityReviewTarget) {
        writeUTF(t.evidenceArtifactId.value); writeUTF(t.sourceSha256.value); writeUTF(t.preparationIdentity.value)
        writeUTF(t.derivativeGenerationId.value); writeUTF(t.derivativeGenerationSha256.value); writeUTF(t.derivativeContentSha256.value)
    }
    private fun DataInputStream.readTarget() = HumanFidelityReviewTarget(EvidenceArtifactId(readUTF()),
        OcrSha256Digest(readUTF()), OcrSha256Digest(readUTF()), DerivativeGenerationId(readUTF()),
        OcrSha256Digest(readUTF()), OcrSha256Digest(readUTF()))
    private fun DataOutputStream.writeProposal(p: HumanTranscriptionCorrectionProposal) {
        writeUTF(p.proposalId.value); writeUTF(p.reviewId.value); writeUTF(p.discrepancyId.value); writeTarget(p.target)
        writeBounded(p.providerValue); writeBounded(p.acceptedSourceValue); writeUTF(p.proposerPrincipalId.value)
        writeUTF(p.proposedAt.toString()); writeBounded(p.reason)
    }
    private fun DataInputStream.readProposal() = HumanTranscriptionCorrectionProposal(CorrectionProposalId(readUTF()),
        HumanFidelityReviewId(readUTF()), FidelityDiscrepancyId(readUTF()), readTarget(), readBounded(), readBounded(),
        PrincipalId(readUTF()), Instant.parse(readUTF()), readBounded())
    private fun DataOutputStream.writeAcceptance(a: HumanTranscriptionCorrectionAcceptance) {
        writeUTF(a.acceptanceId.value); writeUTF(a.reviewId.value); writeTarget(a.target); writeInt(a.proposalIds.size)
        a.proposalIds.sortedBy { it.value }.forEach { writeUTF(it.value) }; writeUTF(a.acceptingPrincipalId.value)
        writeUTF(a.acceptedAt.toString()); writeUTF(a.authorizationPurpose.value)
        writeBoolean(a.supersedesAcceptanceId != null); a.supersedesAcceptanceId?.let { writeUTF(it.value) }
    }
    private fun DataInputStream.readAcceptance(): HumanTranscriptionCorrectionAcceptance {
        val id = CorrectionAcceptanceId(readUTF()); val review = HumanFidelityReviewId(readUTF()); val target = readTarget()
        val proposals = List(readCount()) { CorrectionProposalId(readUTF()) }; val principal = PrincipalId(readUTF())
        val at = Instant.parse(readUTF()); val purpose = AuthorizationPurposeId(readUTF())
        val predecessor = if (readBoolean()) CorrectionAcceptanceId(readUTF()) else null
        return HumanTranscriptionCorrectionAcceptance(id, review, target, proposals, principal, at, purpose, predecessor)
    }
    private fun DataOutputStream.writeBounded(value: String) { val b = value.toByteArray(Charsets.UTF_8); require(b.size <= 2_000_000); writeInt(b.size); write(b) }
    private fun DataInputStream.readBounded(): String { val n = readInt(); require(n in 0..2_000_000); return String(readNBytes(n).also { require(it.size == n) }, Charsets.UTF_8) }
    private fun DataInputStream.readCount(): Int = readInt().also { require(it in 1..1000) }
}
