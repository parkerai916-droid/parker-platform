package parker.core.runtime

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import parker.core.interfaces.*

class FileSystemHumanVerificationStorage(storageRoot: Path) : HumanVerificationStorage {
    private val root = storageRoot.toAbsolutePath().normalize()
    private val prepared = root.resolve(".prepared")
    private val mutex = Mutex()

    init {
        try { Files.createDirectories(root); Files.createDirectories(prepared) }
        catch (e: IOException) { throw HumanVerificationStorageException.InvalidStorageRoot(root.toString(), "could not create storage directories") }
        if (!Files.isDirectory(root) || !Files.isWritable(root)) throw HumanVerificationStorageException.InvalidStorageRoot(root.toString(), "root is not a writable directory")
    }

    override suspend fun prepare(record: HumanVerificationRecord) = mutex.withLock {
        safe(record.humanVerificationRecordId)
        val target = target(record.humanVerificationRecordId); val staged = staged(record.humanVerificationRecordId)
        if (Files.exists(target) || Files.exists(staged)) throw HumanVerificationStorageException.DuplicateIdentifier(record.humanVerificationRecordId)
        try { Files.write(staged, HumanVerificationRecordCodec.encode(record), StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE) }
        catch (e: HumanVerificationStorageException) { throw e }
        catch (e: Exception) { throw HumanVerificationStorageException.PersistenceFailure("Failed to prepare human verification record", e) }
        Unit
    }

    override suspend fun publishPrepared(humanVerificationRecordId: HumanVerificationRecordId) = mutex.withLock {
        safe(humanVerificationRecordId)
        val source = staged(humanVerificationRecordId); val target = target(humanVerificationRecordId)
        if (Files.exists(target)) throw HumanVerificationStorageException.DuplicateIdentifier(humanVerificationRecordId)
        try { Files.move(source, target, StandardCopyOption.ATOMIC_MOVE) }
        catch (e: Exception) { throw HumanVerificationStorageException.PersistenceFailure("Failed to publish human verification record", e) }
        Unit
    }

    override suspend fun retrieve(humanVerificationRecordId: HumanVerificationRecordId): HumanVerificationRecord? = mutex.withLock {
        safe(humanVerificationRecordId); val path = target(humanVerificationRecordId)
        if (!Files.exists(path)) return@withLock null
        decode(humanVerificationRecordId, path)
    }

    override suspend fun listForExactGeneration(evidenceArtifactId: EvidenceArtifactId, derivativeGenerationId: DerivativeGenerationId): List<HumanVerificationRecord> = mutex.withLock {
        try {
            Files.list(root).use { paths -> paths.filter { Files.isRegularFile(it) && it.fileName.toString().endsWith(EXTENSION) }.toList() }
                .map { path ->
                    val id = HumanVerificationRecordId(path.fileName.toString().removeSuffix(EXTENSION)); decode(id, path)
                }
                .filter { it.evidenceArtifactId == evidenceArtifactId && it.derivativeGenerationId == derivativeGenerationId }
                .sortedBy { it.humanVerificationRecordId.value }
        } catch (e: HumanVerificationStorageException) { throw e }
        catch (e: Exception) { throw HumanVerificationStorageException.PersistenceFailure("Failed to list human verification records", e) }
    }

    private fun decode(id: HumanVerificationRecordId, path: Path): HumanVerificationRecord = try {
        require(Files.size(path) <= MAX_RECORD_BYTES) { "record exceeds bounded size" }
        HumanVerificationRecordCodec.decode(Files.readAllBytes(path)).also { require(it.humanVerificationRecordId == id) }
    } catch (e: UnsupportedHumanVerificationRepresentationVersionException) {
        throw HumanVerificationStorageException.UnsupportedRepresentationVersion(id, e.version)
    } catch (e: Exception) { throw HumanVerificationStorageException.CorruptRecord(id, "record could not be decoded", e) }
    private fun target(id: HumanVerificationRecordId) = root.resolve("${id.value}$EXTENSION")
    private fun staged(id: HumanVerificationRecordId) = prepared.resolve("${id.value}$EXTENSION")
    private fun safe(id: HumanVerificationRecordId) { if (!SAFE.matches(id.value)) throw HumanVerificationStorageException.UnsafeIdentifier(id) }
    private companion object {
        const val EXTENSION = ".human-verification"
        const val MAX_RECORD_BYTES = 1024L * 1024L
        val SAFE = Regex("^[a-z0-9_-]+$")
    }
}
