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
import parker.core.interfaces.EvidenceDeletionAudit
import parker.core.interfaces.EvidenceDeletionAuditException
import parker.core.interfaces.EvidenceDeletionAuditRecord

/**
 * Evidence Custodian, Implementation Plan Phase 7 ("Deletion workflow").
 * The one production [EvidenceDeletionAudit] implementation, governed by
 * `docs/architecture/EVIDENCE_CUSTODIAN_PHASE_7_BOUNDARY_CLARIFICATION.md`
 * ("the Boundary Clarification") Section 2: a narrow, purpose-built
 * durable log -- not a new storage technology, and not `AuditService`.
 *
 * ## Format: one tab-separated line per record, no new dependency
 *
 * This repository has exactly one runtime dependency
 * (`kotlinx-coroutines-core`; see [FileSystemEvidenceArtifactStorage]'s
 * own KDoc). Introducing a JSON or other structured-serialisation
 * library for one small, fixed-shape record is not warranted. Each
 * [EvidenceDeletionAuditRecord] is written as one line of tab-separated
 * `key=value` fields (`deletionRequestId`/`evidenceArtifactId`/
 * `requestingPrincipalId`/`recordedAt`/`stage`), in that fixed order,
 * terminated by a single `\n`. [EvidenceDeletionAuditRecord.recordedAt]
 * is formatted via [Instant.toString], already ISO-8601 with no embedded
 * tab or newline. No field value may itself contain a tab or newline --
 * [parker.core.interfaces.EvidenceArtifactId] and
 * [parker.core.interfaces.PrincipalId] are already non-blank value
 * classes with no whitespace-injection concern this Unit introduces.
 *
 * ## Durability: append, then force -- mirroring [FileSystemEvidenceArtifactStorage]'s
 * own [FileChannel.force] discipline, adapted for append rather than
 * create-once
 *
 * Every [record] call opens [logFile] in append mode, writes exactly one
 * line, and calls [FileChannel.force] before returning -- so a
 * successful call has genuinely reached durable storage, not merely an
 * in-memory OS buffer, before this method returns. [record] never
 * truncates, rewrites, or removes a previously written line -- append
 * only, matching Scope Lock Section 7's "durable, append-only audit
 * record" requirement and Determination 2's "no tombstone" rule (there
 * is nothing here to make a tombstone claim about; this file records
 * deletion *events*, never evidence content itself).
 *
 * ## What this does not guarantee (disclosed, not claimed closed)
 *
 * - **No cross-process concurrency guarantee.** [mutex] is an in-process
 *   guard only, exactly as [FileSystemEvidenceArtifactStorage] discloses
 *   for the same reason.
 * - **No query capability.** This class exposes no read/search
 *   operation -- Boundary Clarification Section 2 explicitly excludes
 *   one. A human reviewer inspects [logFile] directly.
 * - **No protection against OS- or administrator-level access**, for the
 *   same reason [FileSystemEvidenceArtifactStorage] discloses this
 *   limitation.
 *
 * @param logFile The exact file path this instance appends to. Its
 *   parent directory must already exist and be writable -- validated
 *   once, at construction, exactly mirroring
 *   [FileSystemEvidenceArtifactStorage]'s own "fail fast at
 *   construction, not deferred to the first write" discipline. If
 *   [logFile] itself does not yet exist, it is created, empty, at
 *   construction; if it already exists (a prior process's own log), its
 *   existing content is preserved, never truncated.
 */
class FileSystemEvidenceDeletionAudit(private val logFile: Path) : EvidenceDeletionAudit {

    private val mutex = Mutex()

    init {
        val parent = logFile.toAbsolutePath().normalize().parent
        if (parent == null || !Files.exists(parent)) {
            throw EvidenceDeletionAuditException.PersistenceFailure(
                "Evidence deletion audit log parent directory does not exist: '$parent'",
                IOException("missing parent directory"),
            )
        }
        if (!Files.isDirectory(parent)) {
            throw EvidenceDeletionAuditException.PersistenceFailure(
                "Evidence deletion audit log parent path is not a directory: '$parent'",
                IOException("parent path is not a directory"),
            )
        }
        if (!Files.isWritable(parent)) {
            throw EvidenceDeletionAuditException.PersistenceFailure(
                "Evidence deletion audit log parent directory is not writable: '$parent'",
                IOException("parent directory is not writable"),
            )
        }
        try {
            if (!Files.exists(logFile)) {
                Files.createFile(logFile)
            }
        } catch (e: IOException) {
            throw EvidenceDeletionAuditException.PersistenceFailure(
                "Failed to create evidence deletion audit log '$logFile'",
                e,
            )
        }
    }

    override suspend fun record(record: EvidenceDeletionAuditRecord) {
        val line = formatLine(record)

        mutex.withLock {
            try {
                FileChannel.open(logFile, StandardOpenOption.WRITE, StandardOpenOption.APPEND).use { channel ->
                    val buffer = ByteBuffer.wrap(line.toByteArray(StandardCharsets.UTF_8))
                    // FileChannel.write's general contract permits a partial write per call --
                    // loop until every byte is actually written, mirroring
                    // FileSystemEvidenceArtifactStorage.writeAndForce's own identical discipline.
                    while (buffer.hasRemaining()) {
                        channel.write(buffer)
                    }
                    channel.force(true)
                }
            } catch (e: IOException) {
                throw EvidenceDeletionAuditException.PersistenceFailure(
                    "Failed to durably record evidence deletion audit entry for identifier " +
                        "'${record.evidenceArtifactId.value}' (deletionRequestId=${record.deletionRequestId})",
                    e,
                )
            }
        }
    }

    private fun formatLine(record: EvidenceDeletionAuditRecord): String = buildString {
        append("deletionRequestId=").append(record.deletionRequestId).append('\t')
        append("evidenceArtifactId=").append(record.evidenceArtifactId.value).append('\t')
        append("requestingPrincipalId=").append(record.requestingPrincipalId.value).append('\t')
        append("recordedAt=").append(record.recordedAt.toString()).append('\t')
        append("stage=").append(record.stage.name)
        append('\n')
    }
}
