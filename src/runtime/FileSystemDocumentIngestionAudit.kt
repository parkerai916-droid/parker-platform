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
import parker.core.interfaces.DocumentIngestionAudit
import parker.core.interfaces.DocumentIngestionAuditException
import parker.core.interfaces.DocumentIngestionAuditRecord

class FileSystemDocumentIngestionAudit(private val logFile: Path) : DocumentIngestionAudit {
    private val mutex = Mutex()

    init {
        val parent = logFile.toAbsolutePath().normalize().parent
        if (parent == null || !Files.exists(parent) || !Files.isDirectory(parent) || !Files.isWritable(parent)) {
            throw DocumentIngestionAuditException.PersistenceFailure(
                "Document ingestion audit log parent directory is unavailable: '$parent'",
                IOException("invalid audit log parent directory"),
            )
        }
        try {
            if (!Files.exists(logFile)) Files.createFile(logFile)
        } catch (e: IOException) {
            throw DocumentIngestionAuditException.PersistenceFailure("Failed to create document ingestion audit log '$logFile'", e)
        }
    }

    override suspend fun record(record: DocumentIngestionAuditRecord) {
        val line = buildString {
            append("correlationValue=").append(encode(record.correlationValue)).append('\t')
            append("sourceEvidenceArtifactId=").append(encode(record.sourceEvidenceArtifactId.value)).append('\t')
            append("requestingPrincipalId=").append(encode(record.requestingPrincipalId.value)).append('\t')
            append("operationalOutcome=").append(encode(record.operationalOutcome)).append('\t')
            append("recordedAt=").append(record.recordedAt).append('\t')
            append("derivativeGenerationId=").append(record.derivativeGenerationId?.value?.let(::encode) ?: "")
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
                throw DocumentIngestionAuditException.PersistenceFailure(
                    "Failed to durably record document ingestion audit entry (correlationValue=${record.correlationValue})",
                    e,
                )
            }
        }
    }

    private fun encode(value: String): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(value.toByteArray(StandardCharsets.UTF_8))
}
