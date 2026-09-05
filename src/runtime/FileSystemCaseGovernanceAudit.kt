package parker.core.runtime

import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.Base64
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import parker.core.interfaces.CaseGovernanceAudit
import parker.core.interfaces.CaseGovernanceAuditException
import parker.core.interfaces.CaseGovernanceAuditRecord

/** CASE-1. Mirrors [FileSystemDocumentIngestionAudit]'s own single append-only log file, base64-encoded tab-separated line shape exactly. Write-only: no read-back capability exists because none is required. */
class FileSystemCaseGovernanceAudit(private val logFile: Path) : CaseGovernanceAudit {
    private val mutex = Mutex()

    init {
        val parent = logFile.toAbsolutePath().normalize().parent
        if (parent == null || !Files.exists(parent) || !Files.isDirectory(parent) || !Files.isWritable(parent)) {
            throw CaseGovernanceAuditException.PersistenceFailure(
                "Case governance audit log parent directory is unavailable: '$parent'",
                IOException("invalid audit log parent directory"),
            )
        }
        try {
            if (!Files.exists(logFile)) Files.createFile(logFile)
        } catch (e: IOException) {
            throw CaseGovernanceAuditException.PersistenceFailure("Failed to create case governance audit log '$logFile'", e)
        }
    }

    override suspend fun record(record: CaseGovernanceAuditRecord) {
        val line = buildString {
            append("eventType=").append(record.eventType.name).append('\t')
            append("caseId=").append(encode(record.caseId?.value)).append('\t')
            append("previousCaseId=").append(encode(record.previousCaseId?.value)).append('\t')
            append("evidenceArtifactId=").append(encode(record.evidenceArtifactId?.value)).append('\t')
            append("actorPrincipalId=").append(encode(record.actorPrincipalId.value)).append('\t')
            append("recordedAt=").append(record.recordedAt)
            append('\n')
        }.toByteArray(StandardCharsets.UTF_8)
        mutex.withLock {
            try {
                FileChannel.open(logFile, StandardOpenOption.WRITE, StandardOpenOption.APPEND).use { channel ->
                    val buffer = ByteBuffer.wrap(line)
                    while (buffer.hasRemaining()) channel.write(buffer)
                    channel.force(true)
                }
            } catch (e: IOException) {
                throw CaseGovernanceAuditException.PersistenceFailure(
                    "Failed to durably record case governance audit entry (eventType=${record.eventType})",
                    e,
                )
            }
        }
    }

    private fun encode(value: String?): String =
        value?.let { Base64.getUrlEncoder().withoutPadding().encodeToString(it.toByteArray(StandardCharsets.UTF_8)) } ?: "-"
}
