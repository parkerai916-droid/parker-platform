package parker.core.runtime

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import java.time.Instant
import parker.core.interfaces.CaseAssignmentRecord
import parker.core.interfaces.CaseId
import parker.core.interfaces.EvidenceArtifactId

/** CASE-1. Mirrors [CaseRecordCodec]'s own shape; [CaseAssignmentRecord.caseId] is nullable (Unassigned). */
internal object CaseAssignmentRecordCodec {
    private const val MAGIC = 0x43415349 // "CASI" (case assignment)
    private const val VERSION = 1
    private const val MAX_STRING_BYTES = 1024 * 1024

    fun encode(assignment: CaseAssignmentRecord): ByteArray = ByteArrayOutputStream().use { bytes ->
        DataOutputStream(bytes).use { output ->
            output.writeInt(MAGIC)
            output.writeInt(VERSION)
            output.writeString(assignment.evidenceArtifactId.value)
            output.writeNullableString(assignment.caseId?.value)
            output.writeString(assignment.assignedAt.toString())
        }
        bytes.toByteArray()
    }

    fun decode(content: ByteArray): CaseAssignmentRecord = DataInputStream(ByteArrayInputStream(content)).use { input ->
        require(input.readInt() == MAGIC) { "invalid case assignment record magic" }
        require(input.readInt() == VERSION) { "unsupported case assignment record version" }
        val evidenceArtifactId = EvidenceArtifactId(input.readString())
        val caseId = input.readNullableString()?.let(::CaseId)
        val assignedAt = Instant.parse(input.readString())
        require(input.available() == 0) { "unexpected trailing bytes" }
        CaseAssignmentRecord(evidenceArtifactId = evidenceArtifactId, caseId = caseId, assignedAt = assignedAt)
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
