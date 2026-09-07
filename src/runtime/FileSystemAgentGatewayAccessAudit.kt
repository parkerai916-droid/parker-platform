package parker.core.runtime

import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import parker.core.interfaces.AgentGatewayAccessAudit
import parker.core.interfaces.AgentGatewayAccessAuditException
import parker.core.interfaces.AgentGatewayAccessAuditRecord

/**
 * Parker Agent Gateway, AG-1E. The one production [AgentGatewayAccessAudit]
 * implementation, mirroring [FileSystemEvidenceDeletionAudit]'s own
 * established shape exactly: one tab-separated line per record, in a fixed
 * field order, append-then-force durability, no query capability, no new
 * runtime dependency. See that class's own KDoc for the full rationale this
 * class does not repeat verbatim.
 *
 * ## Format
 *
 * `principalId\tcorrelationId\toperation\ttargetId\toutcome\trecordedAt`,
 * one line per record, terminated by `\n`. A `null` field
 * ([AgentGatewayAccessAuditRecord.principalId]/`operation`/`targetId`)
 * is written as the literal three-character token `-` (a value none of
 * these fields could ever authentically take -- [PrincipalId]/typed-id
 * `.value` strings and verb phrases are all required non-blank).
 *
 * @param logFile The exact file path this instance appends to, following
 *   [FileSystemEvidenceDeletionAudit]'s own identical fail-fast-at-
 *   construction discipline.
 */
class FileSystemAgentGatewayAccessAudit(private val logFile: Path) : AgentGatewayAccessAudit {

    private val mutex = Mutex()

    init {
        val parent = logFile.toAbsolutePath().normalize().parent
        if (parent == null || !Files.exists(parent)) {
            throw AgentGatewayAccessAuditException.PersistenceFailure(
                "Agent Gateway access audit log parent directory does not exist: '$parent'",
                IOException("missing parent directory"),
            )
        }
        if (!Files.isDirectory(parent)) {
            throw AgentGatewayAccessAuditException.PersistenceFailure(
                "Agent Gateway access audit log parent path is not a directory: '$parent'",
                IOException("parent path is not a directory"),
            )
        }
        if (!Files.isWritable(parent)) {
            throw AgentGatewayAccessAuditException.PersistenceFailure(
                "Agent Gateway access audit log parent directory is not writable: '$parent'",
                IOException("parent directory is not writable"),
            )
        }
        try {
            if (!Files.exists(logFile)) {
                Files.createFile(logFile)
            }
        } catch (e: IOException) {
            throw AgentGatewayAccessAuditException.PersistenceFailure(
                "Failed to create Agent Gateway access audit log '$logFile'",
                e,
            )
        }
    }

    override suspend fun record(record: AgentGatewayAccessAuditRecord) {
        val line = formatLine(record)

        mutex.withLock {
            try {
                FileChannel.open(logFile, StandardOpenOption.WRITE, StandardOpenOption.APPEND).use { channel ->
                    val buffer = ByteBuffer.wrap(line.toByteArray(StandardCharsets.UTF_8))
                    while (buffer.hasRemaining()) {
                        channel.write(buffer)
                    }
                    channel.force(true)
                }
            } catch (e: IOException) {
                throw AgentGatewayAccessAuditException.PersistenceFailure(
                    "Failed to durably record Agent Gateway access audit entry (correlationId=${record.correlationId})",
                    e,
                )
            }
        }
    }

    private fun formatLine(record: AgentGatewayAccessAuditRecord): String = buildString {
        append("principalId=").append(record.principalId?.value ?: "-").append('\t')
        append("correlationId=").append(record.correlationId).append('\t')
        append("operation=").append(record.operation ?: "-").append('\t')
        append("targetId=").append(record.targetId ?: "-").append('\t')
        append("outcome=").append(record.outcome.name).append('\t')
        append("recordedAt=").append(record.recordedAt.toString())
        append('\n')
    }
}
