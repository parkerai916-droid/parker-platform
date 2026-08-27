package parker.core.runtime

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import java.time.Instant
import parker.core.interfaces.*

internal class UnsupportedHumanVerificationRepresentationVersionException(val version: Int) : Exception()

internal object HumanVerificationRecordCodec {
    private const val MAGIC = 0x50485652 // PHVR
    private const val VERSION = 1
    private const val MAX_STRING_BYTES = 16 * 1024
    private const val MAX_SCOPES = 1_000

    fun encode(record: HumanVerificationRecord): ByteArray = ByteArrayOutputStream().use { bytes ->
        DataOutputStream(bytes).use { output ->
            output.writeInt(MAGIC); output.writeInt(VERSION)
            output.writeString(record.humanVerificationRecordId.value)
            output.writeString(record.evidenceArtifactId.value)
            output.writeString(record.derivativeGenerationId.value)
            output.writeInt(record.reviewedPageScope.pageNumbers.size)
            record.reviewedPageScope.pageNumbers.forEach(output::writeInt)
            output.writeInt(record.reviewedCharacterScopes.size)
            record.reviewedCharacterScopes.forEach { output.writeInt(it.pageNumber); output.writeInt(it.startOffsetInclusive); output.writeInt(it.endOffsetExclusive) }
            output.writeString(record.reviewerPrincipalId.value)
            output.writeString(record.reviewedAt.toString())
            output.writeString(record.outcome.name)
            output.writeString(record.reviewArtifactSha256.value)
            output.writeBoolean(record.sensitiveNotes != null); record.sensitiveNotes?.let { output.writeString(it) }
        }
        bytes.toByteArray()
    }

    fun decode(bytes: ByteArray): HumanVerificationRecord = DataInputStream(ByteArrayInputStream(bytes)).use { input ->
        require(input.readInt() == MAGIC)
        val version = input.readInt(); if (version != VERSION) throw UnsupportedHumanVerificationRepresentationVersionException(version)
        val id = HumanVerificationRecordId(input.readString())
        val evidence = EvidenceArtifactId(input.readString())
        val generation = DerivativeGenerationId(input.readString())
        val pages = OcrPageScope(List(input.readCount()) { input.readInt() })
        val characters = List(input.readCount()) { HumanVerificationCharacterScope(input.readInt(), input.readInt(), input.readInt()) }
        val reviewer = PrincipalId(input.readString())
        val reviewedAt = Instant.parse(input.readString())
        val outcome = enumValueOf<HumanVerificationOutcome>(input.readString())
        val artifact = OcrSha256Digest(input.readString())
        val notes = if (input.readBoolean()) input.readString() else null
        require(input.available() == 0)
        HumanVerificationRecord(id, evidence, generation, pages, characters, reviewer, reviewedAt, outcome, artifact, notes)
    }

    private fun DataOutputStream.writeString(value: String) {
        val encoded = value.toByteArray(StandardCharsets.UTF_8); require(encoded.size <= MAX_STRING_BYTES)
        writeInt(encoded.size); write(encoded)
    }
    private fun DataInputStream.readString(): String {
        val size = readInt(); require(size in 0..MAX_STRING_BYTES)
        val encoded = ByteArray(size).also(::readFully)
        return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(encoded)).toString()
    }
    private fun DataInputStream.readCount(): Int = readInt().also { require(it in 0..MAX_SCOPES) }
}
