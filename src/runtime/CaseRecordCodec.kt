package parker.core.runtime

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import java.time.Instant
import parker.core.interfaces.CaseId
import parker.core.interfaces.CaseRecord

/**
 * CASE-1. Mirrors [EvidenceSourceManifestCodec]'s own established shape exactly: magic + version +
 * length-prefixed UTF-8 strings, no trailing digest (atomic temp-file-then-move writes already
 * guard against a torn write; this is a flat three-field record, not a large nested structure).
 */
internal object CaseRecordCodec {
    private const val MAGIC = 0x43415345 // "CASE"
    private const val VERSION = 1
    private const val MAX_STRING_BYTES = 1024 * 1024

    fun encode(case: CaseRecord): ByteArray = ByteArrayOutputStream().use { bytes ->
        DataOutputStream(bytes).use { output ->
            output.writeInt(MAGIC)
            output.writeInt(VERSION)
            output.writeString(case.caseId.value)
            output.writeString(case.caseName)
            output.writeString(case.createdAt.toString())
        }
        bytes.toByteArray()
    }

    fun decode(content: ByteArray): CaseRecord = DataInputStream(ByteArrayInputStream(content)).use { input ->
        require(input.readInt() == MAGIC) { "invalid case record magic" }
        require(input.readInt() == VERSION) { "unsupported case record version" }
        val caseId = CaseId(input.readString())
        val caseName = input.readString()
        val createdAt = Instant.parse(input.readString())
        require(input.available() == 0) { "unexpected trailing bytes" }
        CaseRecord(caseId = caseId, caseName = caseName, createdAt = createdAt)
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
}
