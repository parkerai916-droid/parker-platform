package parker.core.runtime

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import java.time.Instant
import parker.core.interfaces.DerivativeGenerationId
import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.SavedAnalysisEvidenceReference
import parker.core.interfaces.SavedAnalysisId
import parker.core.interfaces.SavedAnalysisRecord

/**
 * Reviewed Analysis Result — Explicit Owner Save. Mirrors
 * [DerivativeGenerationRecordCodec]'s own exact byte-level discipline
 * (magic + version header, length-prefixed UTF-8 strings, bounded
 * collection sizes, `require(input.available() == 0)` trailing-byte check)
 * -- a genuinely new codec, not a reuse of the derivative-generation one,
 * since this record's own shape and authority are distinct.
 */
/** Thrown by [SavedAnalysisRecordCodec.decode] specifically for a representation-version mismatch, so [FileSystemSavedAnalysisStorage] can distinguish it from ordinary corruption -- never a message-string-sniffing distinction. */
internal class UnsupportedSavedAnalysisRepresentationVersionException(val version: Int) :
    Exception("unsupported saved analysis representation version: $version")

internal object SavedAnalysisRecordCodec {
    private const val MAGIC = 0x50444153 // "PDAS" -- Parker Document Analysis, Saved
    private const val VERSION = 1
    private const val MAX_COLLECTION_SIZE = 10_000
    private const val MAX_STRING_BYTES = 8 * 1024 * 1024

    fun encode(record: SavedAnalysisRecord): ByteArray = ByteArrayOutputStream().use { bytes ->
        DataOutputStream(bytes).use { output ->
            output.writeInt(MAGIC)
            output.writeInt(VERSION)
            output.writeString(record.savedAnalysisId.value)
            output.writeString(record.savedAt.toString())
            output.writeString(record.analysedAt.toString())
            output.writeString(record.instruction)
            output.writeString(record.analysisText)
            output.writeCollectionSize(record.evidenceReferences.size)
            record.evidenceReferences.forEach { reference ->
                output.writeString(reference.evidenceArtifactId.value)
                output.writeString(reference.derivativeGenerationId.value)
                output.writeString(reference.derivativeKind)
            }
            output.writeNullableString(record.mechanismIdentity)
            output.writeNullableString(record.mechanismVersion)
        }
        bytes.toByteArray()
    }

    fun decode(content: ByteArray): SavedAnalysisRecord = DataInputStream(ByteArrayInputStream(content)).use { input ->
        require(input.readInt() == MAGIC) { "invalid saved analysis magic" }
        val version = input.readInt()
        if (version != VERSION) throw UnsupportedSavedAnalysisRepresentationVersionException(version)
        val id = SavedAnalysisId(input.readString())
        val savedAt = Instant.parse(input.readString())
        val analysedAt = Instant.parse(input.readString())
        val instruction = input.readString()
        val analysisText = input.readString()
        val evidenceReferences = List(input.readCollectionSize()) {
            SavedAnalysisEvidenceReference(
                evidenceArtifactId = EvidenceArtifactId(input.readString()),
                derivativeGenerationId = DerivativeGenerationId(input.readString()),
                derivativeKind = input.readString(),
            )
        }
        val mechanismIdentity = input.readNullableString()
        val mechanismVersion = input.readNullableString()
        require(input.available() == 0) { "unexpected trailing bytes" }
        SavedAnalysisRecord(
            savedAnalysisId = id,
            savedAt = savedAt,
            analysedAt = analysedAt,
            instruction = instruction,
            analysisText = analysisText,
            evidenceReferences = evidenceReferences,
            mechanismIdentity = mechanismIdentity,
            mechanismVersion = mechanismVersion,
        )
    }

    private fun DataOutputStream.writeString(value: String) {
        val encoded = value.toByteArray(StandardCharsets.UTF_8)
        require(encoded.size <= MAX_STRING_BYTES) { "string exceeds codec limit" }
        writeInt(encoded.size)
        write(encoded)
    }

    private fun DataInputStream.readString(): String {
        val size = readInt()
        require(size in 0..MAX_STRING_BYTES) { "invalid string length" }
        val encoded = ByteArray(size).also(::readFully)
        return StandardCharsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(java.nio.ByteBuffer.wrap(encoded))
            .toString()
    }

    private fun DataOutputStream.writeNullableString(value: String?) {
        writeBoolean(value != null)
        value?.let { writeString(it) }
    }

    private fun DataInputStream.readNullableString(): String? = if (readBoolean()) readString() else null

    private fun DataInputStream.readCollectionSize(): Int = readInt().also {
        require(it in 0..MAX_COLLECTION_SIZE) { "invalid collection size" }
    }

    private fun DataOutputStream.writeCollectionSize(size: Int) {
        require(size in 0..MAX_COLLECTION_SIZE) { "collection exceeds codec limit" }
        writeInt(size)
    }
}
