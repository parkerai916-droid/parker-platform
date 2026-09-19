package parker.core.runtime

import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import parker.core.interfaces.*

/** Write-once, restart-safe storage for completed owner case-analysis results. */
class FileSystemGovernedAnalysisResultStorage(storageRoot: Path) : GovernedAnalysisResultStorage {
    private val root = storageRoot.toAbsolutePath().normalize()
    private val temp = root.resolve(".tmp")
    private val mutex = Mutex()

    init {
        try {
            Files.createDirectories(root); Files.createDirectories(temp)
            if (!Files.isDirectory(root) || !Files.isWritable(root)) throw IOException("root is not writable")
        } catch (e: Exception) {
            throw GovernedAnalysisResultStorageException.InvalidStorageRoot(storageRoot.toString(), e.message ?: "unusable")
        }
    }

    override suspend fun createOrGet(result: GovernedAnalysisResult): GovernedAnalysisResultCreationOutcome = mutex.withLock {
        requireSafe(result.resultId)
        withFileLock(result.resultId) {
            val target = target(result.resultId)
            if (Files.exists(target)) {
                val existing = read(result.resultId)
                return@withFileLock if (existing == result) GovernedAnalysisResultCreationOutcome.AlreadyPresent(existing)
                else GovernedAnalysisResultCreationOutcome.ConflictingResult(result.analysisRequestId)
            }
            val bytes = GovernedAnalysisResultCodec.encode(result)
            val temporary = try { Files.createTempFile(temp, "governed-analysis-", ".tmp") } catch (e: IOException) {
                throw GovernedAnalysisResultStorageException.PersistenceFailure("could not create analysis result temporary file", e)
            }
            try {
                FileChannel.open(temporary, StandardOpenOption.WRITE).use { channel ->
                    val buffer = ByteBuffer.wrap(bytes); while (buffer.hasRemaining()) channel.write(buffer); channel.force(true)
                }
                try { Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE) }
                catch (e: java.nio.file.FileAlreadyExistsException) {
                    val existing = read(result.resultId)
                    return@withFileLock if (existing == result) GovernedAnalysisResultCreationOutcome.AlreadyPresent(existing) else GovernedAnalysisResultCreationOutcome.ConflictingResult(result.analysisRequestId)
                }
                GovernedAnalysisResultCreationOutcome.Created(result)
            } catch (e: GovernedAnalysisResultStorageException) { throw e }
            catch (e: Exception) { throw GovernedAnalysisResultStorageException.PersistenceFailure("could not publish governed analysis result", e) }
            finally { Files.deleteIfExists(temporary) }
        }
    }

    override suspend fun findByAnalysisRequestId(analysisRequestId: AnalysisRequestId): GovernedAnalysisResult? {
        val id = GovernedAnalysisResultId.forRequest(analysisRequestId); requireSafe(id)
        return mutex.withLock { withFileLock(id) { if (!Files.exists(target(id))) null else read(id) } }
    }

    private fun read(id: GovernedAnalysisResultId): GovernedAnalysisResult {
        val path = target(id)
        return try {
            val size = Files.size(path); if (size > MAX_RECORD_BYTES) throw IOException("record exceeds bound")
            val result = GovernedAnalysisResultCodec.decode(Files.readAllBytes(path))
            if (result.resultId != id) throw IOException("stored identity mismatch")
            result
        } catch (e: GovernedAnalysisResultStorageException) { throw e }
        catch (e: Exception) { throw GovernedAnalysisResultStorageException.CorruptRecord(id, e.message ?: "record unreadable", e) }
    }

    private fun <T> withFileLock(id: GovernedAnalysisResultId, action: () -> T): T {
        val lockPath = root.resolve(".${id.value}.lock")
        return try { FileChannel.open(lockPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE).use { channel -> channel.lock().use { action() } } }
        catch (e: GovernedAnalysisResultStorageException) { throw e }
        catch (e: Exception) { throw GovernedAnalysisResultStorageException.PersistenceFailure("could not lock governed analysis result", e) }
    }
    private fun target(id: GovernedAnalysisResultId): Path = root.resolve("${id.value}.analysis")
    private fun requireSafe(id: GovernedAnalysisResultId) { if (!id.value.matches(Regex("^governed-analysis-[a-fA-F0-9-]{36}$"))) throw GovernedAnalysisResultStorageException.UnsafeIdentifier(id) }
    private companion object { const val MAX_RECORD_BYTES = 32L * 1024L * 1024L }
}
