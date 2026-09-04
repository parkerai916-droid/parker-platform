package parker.core.runtime

import java.nio.file.*
import java.time.Instant
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import parker.core.interfaces.*

class FileSystemHumanCorrectionAudit(private val root: Path) : HumanCorrectionAudit {
    private val mutex = Mutex()
    init { require(Files.isDirectory(root) && Files.isWritable(root)) }
    override suspend fun append(record: HumanCorrectionAuditRecord): Unit = mutex.withLock {
        val path = root.resolve("${record.eventId}.human-correction-audit-v1")
        val bytes = encode(record)
        if (Files.exists(path)) require(Files.readAllBytes(path).contentEquals(bytes)) else Files.write(path, bytes, StandardOpenOption.CREATE_NEW)
        Unit
    }
    override suspend fun listForRepresentation(id: DerivativeGenerationId) = mutex.withLock {
        Files.list(root).use { paths -> paths.filter { it.fileName.toString().endsWith(".human-correction-audit-v1") }
            .map { path -> decode(Files.readAllBytes(path)).also {
                require(path.fileName.toString() == "${it.eventId}.human-correction-audit-v1")
            } }.filter { it.representationId == id }.toList().sortedBy { it.eventType.name } }
    }
    private fun encode(r: HumanCorrectionAuditRecord) = listOf("human-correction-audit-v1", r.eventId, r.eventType.name,
        r.occurredAt.toString(), r.actorPrincipalId.value, r.representationId.value, r.target.evidenceArtifactId.value,
        r.target.sourceSha256.value, r.target.preparationIdentity.value, r.target.derivativeGenerationId.value,
        r.target.derivativeGenerationSha256.value, r.target.derivativeContentSha256.value, r.reviewId.value,
        r.acceptanceId.value, r.contentSha256.value).joinToString("\n", postfix="\n").toByteArray()
    private fun decode(bytes: ByteArray): HumanCorrectionAuditRecord {
        val p = String(bytes).split('\n').dropLastWhile { it.isEmpty() }; require(p.size == 15 && p[0] == "human-correction-audit-v1")
        return HumanCorrectionAuditRecord(p[1], HumanCorrectionAuditEventType.valueOf(p[2]), Instant.parse(p[3]), PrincipalId(p[4]),
            DerivativeGenerationId(p[5]), HumanFidelityReviewTarget(EvidenceArtifactId(p[6]), OcrSha256Digest(p[7]), OcrSha256Digest(p[8]),
                DerivativeGenerationId(p[9]), OcrSha256Digest(p[10]), OcrSha256Digest(p[11])), HumanFidelityReviewId(p[12]),
            CorrectionAcceptanceId(p[13]), OcrSha256Digest(p[14]))
    }
}
