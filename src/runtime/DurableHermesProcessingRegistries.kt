package parker.core.runtime

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.security.MessageDigest
import java.time.Instant
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.HermesProcessingCorrection
import parker.core.interfaces.HermesProcessingCompleteness
import parker.core.interfaces.HermesProcessingDecisionRegistry
import parker.core.interfaces.HermesProcessingFailure
import parker.core.interfaces.HermesProcessingFailureKind
import parker.core.interfaces.HermesProcessingHumanDecision
import parker.core.interfaces.HermesProcessingHumanDecisionType
import parker.core.interfaces.HermesProcessingIssue
import parker.core.interfaces.HermesProcessingIssueKind
import parker.core.interfaces.HermesProcessingIssueLocation
import parker.core.interfaces.HermesProcessingMethod
import parker.core.interfaces.HermesProcessingResult
import parker.core.interfaces.HermesProcessingResultRecordOutcome
import parker.core.interfaces.HermesProcessingResultRegistry
import parker.core.interfaces.HermesProcessingStatus
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.TranscriptionFidelity

/** Durable Hermes state failures are deliberately not converted into empty registries. */
sealed class HermesProcessingStorageException(message: String, cause: Throwable? = null) : RuntimeException(message, cause) {
    class InvalidStorageRoot(path: String, reason: String) : HermesProcessingStorageException("Hermes storage root '$path' is invalid: $reason")
    class CorruptRecord(path: Path, reason: String, cause: Throwable? = null) : HermesProcessingStorageException("Hermes record '$path' is corrupt: $reason", cause)
    class PersistenceFailure(path: Path, cause: Throwable) : HermesProcessingStorageException("Could not persist Hermes record '$path'", cause)
}

class FileSystemHermesProcessingResultRegistry(storageRoot: Path) : HermesProcessingResultRegistry {
    private val root = validateRoot(storageRoot.resolve("processing-results"))
    private val mutex = Mutex()

    init { rejectOrphanedTemporaryFiles(root) }

    override suspend fun record(result: HermesProcessingResult): HermesProcessingResultRecordOutcome = mutex.withLock {
        withFileLock(root) {
            val target = target(result.batchId, result.sourceSha256)
            if (Files.exists(target)) {
                val existing = readResult(target)
                return@withFileLock if (existing == result) HermesProcessingResultRecordOutcome.AlreadyRecorded(existing)
                else HermesProcessingResultRecordOutcome.Conflict(existing, result)
            }
            writeAtomically(target) { HermesBinaryCodec.writeResult(this, result) }
            HermesProcessingResultRecordOutcome.Recorded(result)
        }
    }

    override suspend fun find(batchId: String, sourceSha256: String): HermesProcessingResult? = mutex.withLock {
        val target = target(batchId, sourceSha256)
        if (!Files.exists(target)) null else readResult(target)
    }

    override suspend fun listForBatch(batchId: String): List<HermesProcessingResult> = mutex.withLock {
        listResults().filter { it.batchId == batchId }
    }

    override suspend fun listAll(): List<HermesProcessingResult> = mutex.withLock { listResults() }

    private fun listResults(): List<HermesProcessingResult> = Files.list(root).use { stream ->
        stream.filter { Files.isRegularFile(it) && it.fileName.toString().endsWith(".hpr") }
            .sorted().map(::readResult).toList()
    }

    private fun readResult(path: Path): HermesProcessingResult = decode(path) { HermesBinaryCodec.readResult(this) }

    private fun target(batchId: String, sha256: String): Path = root.resolve("${digest(batchId + "\u0000" + sha256)}.hpr")
}

class FileSystemHermesProcessingDecisionRegistry(storageRoot: Path) : HermesProcessingDecisionRegistry {
    private val root = validateRoot(storageRoot.resolve("processing-decisions"))
    private val mutex = Mutex()
    private var nextSequence: Long = discoverNextSequence()

    init { rejectOrphanedTemporaryFiles(root) }

    override suspend fun record(decision: HermesProcessingHumanDecision): HermesProcessingHumanDecision = mutex.withLock {
        withFileLock(root) {
            nextSequence = maxOf(nextSequence, discoverNextSequence())
            val sequence = nextSequence++
            val target = root.resolve("%020d-%s.hpd".format(sequence, digest(decision.batchId + "\u0000" + decision.sourceSha256)))
            writeAtomically(target) { HermesBinaryCodec.writeDecision(this, decision) }
            decision
        }
    }

    override suspend fun latest(batchId: String, sourceSha256: String): HermesProcessingHumanDecision? = mutex.withLock {
        listDecisions().filter { it.batchId == batchId && it.sourceSha256 == sourceSha256 }.lastOrNull()
    }

    override suspend fun history(batchId: String, sourceSha256: String): List<HermesProcessingHumanDecision> = mutex.withLock {
        listDecisions().filter { it.batchId == batchId && it.sourceSha256 == sourceSha256 }
    }

    private fun listDecisions(): List<HermesProcessingHumanDecision> = Files.list(root).use { stream ->
        stream.filter { Files.isRegularFile(it) && it.fileName.toString().endsWith(".hpd") }
            .sorted().map { decode(it) { HermesBinaryCodec.readDecision(this) } }.toList()
    }

    private fun discoverNextSequence(): Long = Files.list(root).use { stream ->
        (stream.filter { it.fileName.toString().endsWith(".hpd") }
            .map { it.fileName.toString().substringBefore('-').toLongOrNull() ?: throw HermesProcessingStorageException.CorruptRecord(it, "invalid decision sequence") }
            .max(java.util.Comparator.naturalOrder()).orElse(-1L) + 1L)
    }
}

private fun validateRoot(root: Path): Path {
    val normalized = root.toAbsolutePath().normalize()
    try {
        Files.createDirectories(normalized)
        if (!Files.isDirectory(normalized) || !Files.isWritable(normalized)) throw IOException("not a writable directory")
    } catch (e: Exception) {
        throw HermesProcessingStorageException.InvalidStorageRoot(root.toString(), e.message ?: "unusable directory")
    }
    return normalized
}

private fun rejectOrphanedTemporaryFiles(root: Path) {
    Files.list(root).use { stream ->
        stream.filter { Files.isRegularFile(it) && it.fileName.toString().endsWith(".tmp") }
            .findFirst().ifPresent { throw HermesProcessingStorageException.CorruptRecord(it, "orphaned temporary file indicates an incomplete write") }
    }
}

private inline fun <T> withFileLock(root: Path, block: () -> T): T {
    val lockPath = root.resolve(".registry.lock")
    return FileChannel.open(lockPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE).use { channel ->
        channel.lock().use { block() }
    }
}

private fun <T> decode(path: Path, reader: DataInputStream.() -> T): T = try {
    Files.newInputStream(path, StandardOpenOption.READ).use { DataInputStream(it).reader() }
} catch (e: Exception) {
    if (e is HermesProcessingStorageException) throw e
    throw HermesProcessingStorageException.CorruptRecord(path, e.message ?: "invalid binary record", e)
}

private fun writeAtomically(target: Path, writer: DataOutputStream.() -> Unit) {
    val temporary = Files.createTempFile(target.parent, ".${target.fileName}", ".tmp")
    try {
        FileChannel.open(temporary, StandardOpenOption.WRITE).use { channel ->
            DataOutputStream(java.nio.channels.Channels.newOutputStream(channel)).let { output ->
                output.writer()
                output.flush()
            }
            channel.force(true)
        }
        Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE)
    } catch (e: Exception) {
        throw HermesProcessingStorageException.PersistenceFailure(target, e)
    } finally {
        Files.deleteIfExists(temporary)
    }
}

private fun digest(value: String): String = MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString("") { "%02x".format(it) }

private object HermesBinaryCodec {
    private const val MAGIC = 0x48524D53
    private const val VERSION = 2
    private const val LEGACY_VERSION = 1
    fun writeResult(output: DataOutputStream, r: HermesProcessingResult) = with(output) {
        writeInt(MAGIC); writeInt(VERSION); writeUTF(r.sourceSha256); writeUTF(r.batchId); writeUTF(r.status.name)
        writeInt(r.methods.size); r.methods.map { it.name }.sorted().forEach(::writeUTF)
        writeNullable(r.proposedEvidenceArtifactId?.value) { writeUTF(it) }
        writeInt(r.issues.size); r.issues.forEach { writeIssue(it) }
        writeNullable(r.failure) { writeFailure(it) }
        writeNullable(r.reviewConfidenceThreshold) { writeDouble(it) }
        writeNullable(r.processingCompleteness) { writeUTF(it.name) }
        writeInt(r.processingWarnings.size); r.processingWarnings.forEach(::writeUTF)
    }
    fun readResult(input: DataInputStream): HermesProcessingResult = with(input) {
        require(readInt() == MAGIC) { "unsupported result header" }
        val version = readInt()
        require(version == LEGACY_VERSION || version == VERSION) { "unsupported result header" }
        val source = readUTF(); val batch = readUTF(); val status = HermesProcessingStatus.valueOf(readUTF())
        val methods = (0 until readCount("methods", 32)).map { HermesProcessingMethod.valueOf(readUTF()) }.toSet()
        val artifact = readNullable { EvidenceArtifactId(readUTF()) }
        val issues = (0 until readCount("issues", 1_000)).map { readIssue(version) }
        val failure = readNullable { readFailure() }
        val threshold = if (version >= VERSION) readNullable { readDouble() } else null
        val completeness = if (version >= VERSION) readNullable { HermesProcessingCompleteness.valueOf(readUTF()) } else null
        val warnings = if (version >= VERSION) (0 until readCount("processing warnings", 1_000)).map { readUTF() } else emptyList()
        return HermesProcessingResult(source, batch, status, methods, artifact, issues, failure, threshold, completeness, warnings)
    }
    fun writeDecision(output: DataOutputStream, d: HermesProcessingHumanDecision) = with(output) {
        writeInt(MAGIC); writeInt(VERSION); writeUTF(d.batchId); writeUTF(d.sourceSha256); writeUTF(d.decision.name)
        writeUTF(d.decidedBy.value); writeUTF(d.decidedAt.toString()); writeNullable(d.reason) { writeUTF(it) }
        writeNullable(d.correction) { writeInt(it.issueIndex); writeUTF(it.correctedInterpretation); writeUTF(it.reason) }
    }
    fun readDecision(input: DataInputStream): HermesProcessingHumanDecision = with(input) {
        require(readInt() == MAGIC) { "unsupported decision header" }
        val version = readInt()
        require(version == LEGACY_VERSION || version == VERSION) { "unsupported decision header" }
        val batch = readUTF(); val sha = readUTF(); val type = HermesProcessingHumanDecisionType.valueOf(readUTF())
        val principal = PrincipalId(readUTF()); val at = Instant.parse(readUTF()); val reason = readNullable { readUTF() }
        val correction = readNullable { HermesProcessingCorrection(readInt(), readUTF(), readUTF()) }
        return HermesProcessingHumanDecision(batch, sha, type, principal, at, reason, correction)
    }
    private fun DataOutputStream.writeIssue(i: HermesProcessingIssue) {
        writeUTF(i.kind.name); writeUTF(i.explanation); writeNullable(i.location) { writeLocation(it) }
        writeNullable(i.hermesInterpretation) { writeUTF(it) }; writeNullable(i.transcriptionFidelity) { writeUTF(it.name) }
        writeNullable(i.observedConfidence) { writeDouble(it) }
    }
    private fun DataInputStream.readIssue(version: Int): HermesProcessingIssue = HermesProcessingIssue(
        HermesProcessingIssueKind.valueOf(readUTF()), readUTF(), readNullable { readLocation() }, readNullable { readUTF() },
        readNullable { TranscriptionFidelity.valueOf(readUTF()) },
        if (version >= VERSION) readNullable { readDouble() } else null,
    )
    private fun DataOutputStream.writeLocation(l: HermesProcessingIssueLocation) { when (l) { is HermesProcessingIssueLocation.DocumentPage -> { writeUTF("DocumentPage"); writeInt(l.pageNumber); writeNullable(l.startOffsetInclusive) { writeInt(it) }; writeNullable(l.endOffsetExclusive) { writeInt(it) }; writeNullable(l.regionDescription) { writeUTF(it) } } } }
    private fun DataInputStream.readLocation(): HermesProcessingIssueLocation { require(readUTF() == "DocumentPage") { "unsupported issue location" }; return HermesProcessingIssueLocation.DocumentPage(readInt(), readNullable { readInt() }, readNullable { readInt() }, readNullable { readUTF() }) }
    private fun DataOutputStream.writeFailure(f: HermesProcessingFailure) { writeUTF(f.kind.name); writeNullable(f.detail) { writeUTF(it) } }
    private fun DataInputStream.readFailure() = HermesProcessingFailure(HermesProcessingFailureKind.valueOf(readUTF()), readNullable { readUTF() })
    private fun <T> DataOutputStream.writeNullable(value: T?, write: DataOutputStream.(T) -> Unit) { writeBoolean(value != null); if (value != null) write(value) }
    private fun <T> DataInputStream.readNullable(read: DataInputStream.() -> T): T? = if (readBoolean()) read.invoke(this) else null
    private fun DataInputStream.readCount(name: String, max: Int): Int { val count = readInt(); require(count in 0..max) { "invalid $name count" }; return count }
}
