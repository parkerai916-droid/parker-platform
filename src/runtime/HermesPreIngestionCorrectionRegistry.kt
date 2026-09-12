package parker.core.runtime

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.time.Instant
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import parker.core.interfaces.HermesPreIngestionCorrectedRepresentation
import parker.core.interfaces.HermesPreIngestionCorrectionBindingResult
import parker.core.interfaces.HermesPreIngestionCorrectionEvidenceBinding
import parker.core.interfaces.HermesPreIngestionCorrectionLineage
import parker.core.interfaces.HermesPreIngestionCorrectionPublication
import parker.core.interfaces.HermesPreIngestionCorrectionRegistry
import parker.core.interfaces.HermesProcessingHumanDecision
import parker.core.interfaces.HermesProcessingHumanDecisionType
import parker.core.interfaces.HermesProcessingIssueKind
import parker.core.interfaces.HermesProcessingResult
import parker.core.interfaces.HermesProcessingStatus
import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.PrincipalId

class HermesPreIngestionCorrectionContentUnavailableException(message: String) : RuntimeException(message)

class InMemoryHermesPreIngestionCorrectionRegistry : HermesPreIngestionCorrectionRegistry {
    private val mutex = Mutex()
    private val records = linkedMapOf<String, HermesPreIngestionCorrectedRepresentation>()
    private val bindings = linkedMapOf<String, HermesPreIngestionCorrectionEvidenceBinding>()

    override suspend fun publish(representation: HermesPreIngestionCorrectedRepresentation): HermesPreIngestionCorrectionPublication = mutex.withLock {
        val existing = records[representation.representationId.value]
        if (existing == null) {
            records[representation.representationId.value] = representation
            HermesPreIngestionCorrectionPublication.Created(representation)
        } else if (existing.sameCorrectionAs(representation)) {
            HermesPreIngestionCorrectionPublication.AlreadyPublished(existing)
        } else {
            HermesPreIngestionCorrectionPublication.Failed("CORRECTION_ID_CONFLICT")
        }
    }

    override suspend fun findForDecision(result: HermesProcessingResult, decision: HermesProcessingHumanDecision): HermesPreIngestionCorrectedRepresentation? {
        if (decision.decision != HermesProcessingHumanDecisionType.CORRECT) return null
        val correction = decision.correction ?: return null
        val issue = result.issues.getOrNull(correction.issueIndex) ?: return null
        val reason = decision.reason ?: return null
        val id = HermesPreIngestionCorrectedRepresentation.deriveId(
            result.batchId, result.sourceSha256, correction.issueIndex, issue.kind, issue.explanation,
            issue.hermesInterpretation, correction.correctedInterpretation, reason, decision.decidedBy,
        )
        return mutex.withLock { records[id.value] }
    }

    override suspend fun bindToEvidence(
        representation: HermesPreIngestionCorrectedRepresentation,
        evidenceArtifactId: EvidenceArtifactId,
        evidenceSourceSha256: String,
        createdAt: Instant,
    ): HermesPreIngestionCorrectionBindingResult = mutex.withLock {
        if (evidenceSourceSha256 != representation.sourceSha256) {
            return@withLock HermesPreIngestionCorrectionBindingResult.Conflict("EVIDENCE_SOURCE_SHA256_MISMATCH")
        }
        if (records[representation.representationId.value] == null) {
            return@withLock HermesPreIngestionCorrectionBindingResult.Failed("CORRECTION_NOT_PUBLISHED")
        }
        val existingForCorrection = bindings[representation.representationId.value]
        if (existingForCorrection != null) {
            return@withLock if (existingForCorrection.evidenceArtifactId == evidenceArtifactId) {
                HermesPreIngestionCorrectionBindingResult.AlreadyBound(existingForCorrection)
            } else {
                HermesPreIngestionCorrectionBindingResult.Conflict("CORRECTION_ALREADY_BOUND_TO_DIFFERENT_EVIDENCE")
            }
        }
        val existingForEvidence = bindings.values.firstOrNull { it.evidenceArtifactId == evidenceArtifactId }
        if (existingForEvidence != null) {
            return@withLock HermesPreIngestionCorrectionBindingResult.Conflict("EVIDENCE_ALREADY_BOUND_TO_DIFFERENT_CORRECTION")
        }
        val binding = HermesPreIngestionCorrectionEvidenceBinding(
            representation.representationId, evidenceArtifactId, representation.sourceSha256, createdAt,
        )
        bindings[representation.representationId.value] = binding
        HermesPreIngestionCorrectionBindingResult.Bound(binding)
    }

    override suspend fun findLineageForEvidence(evidenceArtifactId: EvidenceArtifactId): HermesPreIngestionCorrectionLineage? = mutex.withLock {
        val binding = bindings.values.firstOrNull { it.evidenceArtifactId == evidenceArtifactId } ?: return@withLock null
        val correction = records[binding.correctionId.value]
            ?: throw HermesPreIngestionCorrectionContentUnavailableException("correction content missing for evidence binding")
        HermesPreIngestionCorrectionLineage(binding, correction)
    }
}

class FileSystemHermesPreIngestionCorrectionRegistry(storageRoot: Path) : HermesPreIngestionCorrectionRegistry {
    private val root = validateCorrectionRoot(storageRoot.resolve("pre-ingestion-corrections"))
    private val mutex = Mutex()

    override suspend fun publish(representation: HermesPreIngestionCorrectedRepresentation): HermesPreIngestionCorrectionPublication = mutex.withLock {
        withFileLock(root) {
            val target = root.resolve(representation.representationId.value + ".hpc")
            if (Files.exists(target)) {
                val existing = read(target)
                if (existing.sameCorrectionAs(representation)) HermesPreIngestionCorrectionPublication.AlreadyPublished(existing)
                else HermesPreIngestionCorrectionPublication.Failed("CORRECTION_ID_CONFLICT")
            } else {
                writeAtomically(target, representation)
                HermesPreIngestionCorrectionPublication.Created(representation)
            }
        }
    }

    override suspend fun findForDecision(result: HermesProcessingResult, decision: HermesProcessingHumanDecision): HermesPreIngestionCorrectedRepresentation? {
        if (decision.decision != HermesProcessingHumanDecisionType.CORRECT) return null
        val correction = decision.correction ?: return null
        val issue = result.issues.getOrNull(correction.issueIndex) ?: return null
        val reason = decision.reason ?: return null
        val id = HermesPreIngestionCorrectedRepresentation.deriveId(
            result.batchId, result.sourceSha256, correction.issueIndex, issue.kind, issue.explanation,
            issue.hermesInterpretation, correction.correctedInterpretation, reason, decision.decidedBy,
        )
        return mutex.withLock {
            val target = root.resolve(id.value + ".hpc")
            if (Files.exists(target)) read(target) else null
        }
    }

    override suspend fun bindToEvidence(
        representation: HermesPreIngestionCorrectedRepresentation,
        evidenceArtifactId: EvidenceArtifactId,
        evidenceSourceSha256: String,
        createdAt: Instant,
    ): HermesPreIngestionCorrectionBindingResult = mutex.withLock {
        withFileLock(root) {
            if (evidenceSourceSha256 != representation.sourceSha256) {
                return@withFileLock HermesPreIngestionCorrectionBindingResult.Conflict("EVIDENCE_SOURCE_SHA256_MISMATCH")
            }
            val correctionTarget = bindingTarget(representation.representationId)
            if (!Files.exists(root.resolve(representation.representationId.value + ".hpc"))) {
                return@withFileLock HermesPreIngestionCorrectionBindingResult.Failed("CORRECTION_NOT_PUBLISHED")
            }
            val existingForCorrection = if (Files.exists(correctionTarget)) readBinding(correctionTarget) else null
            if (existingForCorrection != null) {
                return@withFileLock if (existingForCorrection.evidenceArtifactId == evidenceArtifactId) {
                    HermesPreIngestionCorrectionBindingResult.AlreadyBound(existingForCorrection)
                } else {
                    HermesPreIngestionCorrectionBindingResult.Conflict("CORRECTION_ALREADY_BOUND_TO_DIFFERENT_EVIDENCE")
                }
            }
            val existingForEvidence = Files.list(root).use { stream ->
                stream.filter { Files.isRegularFile(it) && it.fileName.toString().endsWith(".hcb") }
                    .map(::readBinding).filter { it.evidenceArtifactId == evidenceArtifactId }.findFirst().orElse(null)
            }
            if (existingForEvidence != null) {
                return@withFileLock HermesPreIngestionCorrectionBindingResult.Conflict("EVIDENCE_ALREADY_BOUND_TO_DIFFERENT_CORRECTION")
            }
            val binding = HermesPreIngestionCorrectionEvidenceBinding(
                representation.representationId, evidenceArtifactId, representation.sourceSha256, createdAt,
            )
            writeBindingAtomically(correctionTarget, binding)
            HermesPreIngestionCorrectionBindingResult.Bound(binding)
        }
    }

    override suspend fun findLineageForEvidence(evidenceArtifactId: EvidenceArtifactId): HermesPreIngestionCorrectionLineage? = mutex.withLock {
        val binding = Files.list(root).use { stream ->
            stream.filter { Files.isRegularFile(it) && it.fileName.toString().endsWith(".hcb") }
                .map(::readBinding).filter { it.evidenceArtifactId == evidenceArtifactId }.findFirst().orElse(null)
        } ?: return@withLock null
        val correctionPath = root.resolve(binding.correctionId.value + ".hpc")
        if (!Files.exists(correctionPath)) throw HermesPreIngestionCorrectionContentUnavailableException("correction content missing for evidence binding")
        HermesPreIngestionCorrectionLineage(binding, read(correctionPath))
    }

    private fun writeAtomically(target: Path, representation: HermesPreIngestionCorrectedRepresentation) {
        val temporary = target.resolveSibling(target.fileName.toString() + ".tmp")
        try {
            Files.deleteIfExists(temporary)
            Files.newOutputStream(temporary, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE).use { output ->
                DataOutputStream(output).use { write(it, representation) }
            }
            Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE)
        } catch (e: Exception) {
            throw IOException("Could not persist pre-ingestion correction", e)
        } finally { Files.deleteIfExists(temporary) }
    }

    private fun read(path: Path): HermesPreIngestionCorrectedRepresentation =
        Files.newInputStream(path, StandardOpenOption.READ).use { DataInputStream(it).use(::read) }

    private fun readBinding(path: Path): HermesPreIngestionCorrectionEvidenceBinding =
        Files.newInputStream(path, StandardOpenOption.READ).use { DataInputStream(it).use(::readBinding) }

    private fun write(out: DataOutputStream, r: HermesPreIngestionCorrectedRepresentation) = with(out) {
        writeInt(MAGIC); writeInt(VERSION); writeUTF(r.representationId.value); writeUTF(r.batchId); writeUTF(r.sourceSha256)
        writeUTF(r.machineResultStatus.name); writeInt(r.issueIndex); writeUTF(r.machineIssueKind.name); writeUTF(r.machineIssueExplanation)
        writeNullable(r.machineInterpretation) { writeUTF(it) }; writeUTF(r.correctedInterpretation); writeUTF(r.ownerExplanation)
        writeUTF(r.ownerPrincipalId.value); writeUTF(r.decisionAt.toString())
    }

    private fun read(input: DataInputStream): HermesPreIngestionCorrectedRepresentation = with(input) {
        require(readInt() == MAGIC && readInt() == VERSION) { "unsupported pre-ingestion correction header" }
        val id = parker.core.interfaces.HermesPreIngestionCorrectionId(readUTF())
        val batch = readUTF(); val sha = readUTF(); val status = HermesProcessingStatus.valueOf(readUTF()); val index = readInt()
        val kind = HermesProcessingIssueKind.valueOf(readUTF()); val issue = readUTF(); val interpretation = readNullable { readUTF() }
        return HermesPreIngestionCorrectedRepresentation(id, batch, sha, status, index, kind, issue, interpretation,
            readUTF(), readUTF(), PrincipalId(readUTF()), Instant.parse(readUTF()))
    }

    private fun writeBindingAtomically(target: Path, binding: HermesPreIngestionCorrectionEvidenceBinding) {
        val temporary = target.resolveSibling(target.fileName.toString() + ".tmp")
        try {
            Files.deleteIfExists(temporary)
            Files.newOutputStream(temporary, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE).use { output ->
                DataOutputStream(output).use {
                    it.writeInt(BINDING_MAGIC); it.writeInt(VERSION); it.writeUTF(binding.correctionId.value)
                    it.writeUTF(binding.evidenceArtifactId.value); it.writeUTF(binding.sourceSha256); it.writeUTF(binding.createdAt.toString())
                }
            }
            Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE)
        } catch (e: Exception) {
            throw IOException("Could not persist pre-ingestion correction evidence binding", e)
        } finally { Files.deleteIfExists(temporary) }
    }

    private fun readBinding(input: DataInputStream): HermesPreIngestionCorrectionEvidenceBinding = with(input) {
        require(readInt() == BINDING_MAGIC && readInt() == VERSION) { "unsupported pre-ingestion correction binding header" }
        HermesPreIngestionCorrectionEvidenceBinding(
            parker.core.interfaces.HermesPreIngestionCorrectionId(readUTF()),
            EvidenceArtifactId(readUTF()), readUTF(), Instant.parse(readUTF()),
        )
    }

    private fun bindingTarget(id: parker.core.interfaces.HermesPreIngestionCorrectionId): Path =
        root.resolve(id.value + ".hcb")

    private fun <T> DataOutputStream.writeNullable(value: T?, write: DataOutputStream.(T) -> Unit) { writeBoolean(value != null); if (value != null) write(value) }
    private fun <T> DataInputStream.readNullable(read: DataInputStream.() -> T): T? = if (readBoolean()) read.invoke(this) else null

    companion object {
        private const val MAGIC = 0x48504352
        private const val BINDING_MAGIC = 0x48504244
        private const val VERSION = 1
    }
}

private fun validateCorrectionRoot(root: Path): Path {
    val normalized = root.toAbsolutePath().normalize()
    Files.createDirectories(normalized)
    require(Files.isDirectory(normalized) && Files.isWritable(normalized)) { "invalid pre-ingestion correction storage root" }
    return normalized
}

private inline fun <T> withFileLock(root: Path, block: () -> T): T =
    FileChannel.open(root.resolve(".registry.lock"), StandardOpenOption.CREATE, StandardOpenOption.WRITE).use { channel -> channel.lock().use { block() } }
