package parker.core.runtime

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.time.Instant
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import parker.core.interfaces.DerivativeGenerationId
import parker.core.interfaces.EvidenceArtifactId

/** The one persisted processing projection consumed by Owner and Dual presentations. */
enum class EvidenceProcessingState {
    REGISTERED,
    PROCESSING,
    REQUIRES_OCR,
    CAPABILITY_UNAVAILABLE,
    REVIEW_REQUIRED,
    FAILED,
    ANALYSIS_READY,
}

data class EvidenceProcessingStateRecord(
    val evidenceArtifactId: EvidenceArtifactId,
    val state: EvidenceProcessingState,
    val reason: String? = null,
    val derivativeGenerationId: DerivativeGenerationId? = null,
    val updatedAt: Instant = Instant.now(),
)

interface EvidenceProcessingStateStore {
    suspend fun find(evidenceArtifactId: EvidenceArtifactId): EvidenceProcessingStateRecord?
    suspend fun record(record: EvidenceProcessingStateRecord): EvidenceProcessingStateRecord
}

/** Small, atomic, evidence-keyed projection. It stores no content and is rebuildable from Parker facts. */
class FileSystemEvidenceProcessingStateStore(root: Path) : EvidenceProcessingStateStore {
    private val directory = root.toAbsolutePath().normalize().also { Files.createDirectories(it) }
    private val mutex = Mutex()

    override suspend fun find(evidenceArtifactId: EvidenceArtifactId): EvidenceProcessingStateRecord? = mutex.withLock {
        val path = path(evidenceArtifactId)
        if (!Files.exists(path)) return@withLock null
        val fields = Files.readAllLines(path).associate { line ->
            val i = line.indexOf('=')
            require(i > 0) { "invalid processing-state record" }
            line.substring(0, i) to line.substring(i + 1)
        }
        EvidenceProcessingStateRecord(
            evidenceArtifactId,
            EvidenceProcessingState.valueOf(requireNotNull(fields["state"])),
            fields["reason"]?.takeUnless { it == "" },
            fields["derivative"]?.takeUnless { it == "" }?.let(::DerivativeGenerationId),
            Instant.parse(requireNotNull(fields["updatedAt"])),
        )
    }

    override suspend fun record(record: EvidenceProcessingStateRecord): EvidenceProcessingStateRecord = mutex.withLock {
        val target = path(record.evidenceArtifactId)
        val temporary = target.resolveSibling(target.fileName.toString() + ".tmp")
        val text = buildString {
            appendLine("evidence=${record.evidenceArtifactId.value}")
            appendLine("state=${record.state.name}")
            appendLine("reason=${record.reason ?: ""}")
            appendLine("derivative=${record.derivativeGenerationId?.value ?: ""}")
            appendLine("updatedAt=${record.updatedAt}")
        }
        Files.writeString(temporary, text, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
        try {
            Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        } catch (_: Exception) {
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING)
        }
        record
    }

    private fun path(id: EvidenceArtifactId): Path {
        require(id.value.matches(Regex("[A-Za-z0-9_-]+"))) { "invalid evidence identity" }
        return directory.resolve("${id.value}.state")
    }
}
