package parker.core.runtime

import java.nio.file.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import parker.core.interfaces.*

class FileSystemHumanCorrectedRepresentationStorage(private val root: Path) : HumanCorrectedRepresentationStorage {
    private val mutex = Mutex(); private val prepared = root.resolve("prepared")
    init { require(Files.isDirectory(root) && Files.isWritable(root)); Files.createDirectories(prepared) }
    override suspend fun prepare(representation: HumanCorrectedRegionTranscription) = mutex.withLock {
        val bytes = HumanCorrectedRepresentationCodec.encode(representation); val target = published(representation.derivativeGenerationId)
        if (Files.exists(target)) { require(Files.readAllBytes(target).contentEquals(bytes)) { "Conflicting corrected representation" }; return@withLock HumanCorrectedRepresentationPrepareResult.AlreadyPublished }
        val staged = staged(representation.derivativeGenerationId)
        if (Files.exists(staged)) { require(Files.readAllBytes(staged).contentEquals(bytes)); return@withLock HumanCorrectedRepresentationPrepareResult.AlreadyPrepared }
        Files.write(staged, bytes, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)
        HumanCorrectedRepresentationPrepareResult.Prepared
    }
    override suspend fun publishPrepared(id: DerivativeGenerationId) = mutex.withLock {
        val staged = staged(id); val target = published(id)
        if (Files.exists(target)) { if (Files.exists(staged)) require(Files.readAllBytes(target).contentEquals(Files.readAllBytes(staged))); return@withLock }
        require(Files.exists(staged)); Files.move(staged, target, StandardCopyOption.ATOMIC_MOVE)
    }
    override suspend fun retrieve(id: DerivativeGenerationId) = mutex.withLock {
        val path = published(id); if (!Files.exists(path)) null else HumanCorrectedRepresentationCodec.decode(Files.readAllBytes(path)).also { require(it.derivativeGenerationId == id) }
    }
    override suspend fun listForExactTarget(target: HumanFidelityReviewTarget) = mutex.withLock {
        Files.list(root).use { paths ->
            paths.filter { Files.isRegularFile(it) && it.fileName.toString().endsWith(".human-corrected-v1") }
                .map { HumanCorrectedRepresentationCodec.decode(Files.readAllBytes(it)) }
                .filter { it.target == target }
                .sorted(compareBy { it.derivativeGenerationId.value })
                .toList()
        }
    }
    private fun published(id: DerivativeGenerationId) = safe(id).let { root.resolve("${it.value}.human-corrected-v1") }
    private fun staged(id: DerivativeGenerationId) = safe(id).let { prepared.resolve("${it.value}.human-corrected-v1") }
    private fun safe(id: DerivativeGenerationId) = id.also { require(it.value.matches(Regex("^human-corrected-[0-9a-f]{64}$"))) }
}
