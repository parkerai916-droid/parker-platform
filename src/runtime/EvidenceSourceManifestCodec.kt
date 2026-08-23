package parker.core.runtime

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.EvidenceSourceManifest

/**
 * Binary encode/decode for [EvidenceSourceManifest], mirroring
 * [DerivativeGenerationRecordCodec]'s own magic/version/`DataOutputStream`
 * discipline exactly -- no new serialisation library is introduced for
 * one small, fixed-shape record (the same reasoning
 * `FileSystemEvidenceDeletionAudit`'s own KDoc already gives).
 */
internal object EvidenceSourceManifestCodec {
    private const val MAGIC = 0x45534d46 // "ESMF"
    private const val VERSION = 1
    private const val MAX_STRING_BYTES = 1024 * 1024

    fun encode(manifest: EvidenceSourceManifest): ByteArray = ByteArrayOutputStream().use { bytes ->
        DataOutputStream(bytes).use { output ->
            output.writeInt(MAGIC)
            output.writeInt(VERSION)
            output.writeString(manifest.evidenceArtifactId.value)
            output.writeString(manifest.sha256)
            output.writeLong(manifest.byteLength)
            output.writeNullableString(manifest.receivedMediaType)
            output.writeNullableString(manifest.originalFileName)
        }
        bytes.toByteArray()
    }

    fun decode(content: ByteArray): EvidenceSourceManifest = DataInputStream(ByteArrayInputStream(content)).use { input ->
        require(input.readInt() == MAGIC) { "invalid manifest magic" }
        require(input.readInt() == VERSION) { "unsupported manifest version" }
        val evidenceArtifactId = EvidenceArtifactId(input.readString())
        val sha256 = input.readString()
        val byteLength = input.readLong()
        val receivedMediaType = input.readNullableString()
        val originalFileName = input.readNullableString()
        require(input.available() == 0) { "unexpected trailing bytes" }
        EvidenceSourceManifest(
            evidenceArtifactId = evidenceArtifactId,
            sha256 = sha256,
            byteLength = byteLength,
            receivedMediaType = receivedMediaType,
            originalFileName = originalFileName,
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
            .decode(ByteBuffer.wrap(encoded))
            .toString()
    }

    private fun DataOutputStream.writeNullableString(value: String?) {
        writeBoolean(value != null)
        value?.let { writeString(it) }
    }

    private fun DataInputStream.readNullableString(): String? = if (readBoolean()) readString() else null
}
