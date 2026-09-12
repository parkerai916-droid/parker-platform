package parker.core.runtime

import java.io.DataInputStream
import java.io.DataOutputStream
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.security.MessageDigest
import java.time.Instant
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import parker.core.interfaces.PendingReviewSource
import parker.core.interfaces.PendingReviewSourceStorage
import parker.core.interfaces.PendingReviewSourceStoreResult

private const val MAX_PENDING_SOURCE_BYTES = 64L * 1024L * 1024L

class InMemoryPendingReviewSourceStorage : PendingReviewSourceStorage {
    private val mutex = Mutex()
    private val values = mutableMapOf<String, PendingReviewSource>()

    override suspend fun store(source: PendingReviewSource): PendingReviewSourceStoreResult = mutex.withLock {
        val key = key(source.batchId, source.sourceSha256)
        val existing = values[key]
        when {
            existing == null -> { values[key] = source.copy(bytes = source.copyBytes()); PendingReviewSourceStoreResult.Stored(source) }
            existing.bytes.contentEquals(source.bytes) -> PendingReviewSourceStoreResult.AlreadyStored(existing)
            else -> PendingReviewSourceStoreResult.Conflict(existing)
        }
    }

    override suspend fun find(batchId: String, sourceSha256: String): PendingReviewSource? = mutex.withLock {
        values[key(batchId, sourceSha256)]?.let { it.copy(bytes = it.copyBytes()) }
    }
}

class FileSystemPendingReviewSourceStorage(rootPath: Path) : PendingReviewSourceStorage {
    private val root = rootPath.toAbsolutePath().normalize()
    private val mutex = Mutex()

    init {
        Files.createDirectories(root.resolve("records"))
        Files.createDirectories(root.resolve(".tmp"))
        require(Files.isDirectory(root) && Files.isWritable(root)) { "pending-review storage root is not writable" }
    }

    override suspend fun store(source: PendingReviewSource): PendingReviewSourceStoreResult = mutex.withLock {
        val target = target(source.batchId, source.sourceSha256)
        withFileLock {
            if (Files.exists(target)) {
                val existing = read(target)
                return@withFileLock if (existing.bytes.contentEquals(source.bytes)) {
                    PendingReviewSourceStoreResult.AlreadyStored(existing)
                } else PendingReviewSourceStoreResult.Conflict(existing)
            }
            val temporary = Files.createTempFile(root.resolve(".tmp"), "pending-", ".tmp")
            try {
                FileChannel.open(temporary, StandardOpenOption.WRITE).use { channel ->
                    DataOutputStream(java.nio.channels.Channels.newOutputStream(channel)).let { output ->
                        output.writeInt(MAGIC); output.writeInt(VERSION)
                        output.writeUTF(source.batchId); output.writeUTF(source.sourceSha256)
                        output.writeLong(source.byteLength); output.writeBoolean(source.mediaType != null)
                        source.mediaType?.let(output::writeUTF); output.writeBoolean(source.originalDisplayName != null)
                        source.originalDisplayName?.let(output::writeUTF); output.writeUTF(source.createdAt.toString())
                        output.writeInt(source.bytes.size); output.write(source.bytes); output.flush()
                    }
                    channel.force(true)
                }
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE)
                PendingReviewSourceStoreResult.Stored(source.copy(bytes = source.copyBytes()))
            } finally { Files.deleteIfExists(temporary) }
        }
    }

    override suspend fun find(batchId: String, sourceSha256: String): PendingReviewSource? = mutex.withLock {
        val target = target(batchId, sourceSha256)
        if (!Files.exists(target)) null else read(target)
    }

    private fun target(batchId: String, sourceSha256: String): Path = root.resolve("records").resolve("${digest(batchId + "\u0000" + sourceSha256)}.prs")
    private fun read(path: Path): PendingReviewSource = Files.newInputStream(path).use { input ->
        DataInputStream(input).let { data ->
            require(data.readInt() == MAGIC && data.readInt() == VERSION) { "unsupported pending-review record" }
            val batch = data.readUTF(); val sha = data.readUTF(); val length = data.readLong()
            val media = if (data.readBoolean()) data.readUTF() else null
            val name = if (data.readBoolean()) data.readUTF() else null
            val created = Instant.parse(data.readUTF()); val size = data.readInt()
            require(size.toLong() in 1L..MAX_PENDING_SOURCE_BYTES) { "invalid pending-review byte length" }
            val bytes = ByteArray(size); data.readFully(bytes)
            require(length == size.toLong() && sha256(bytes) == sha) { "pending-review record integrity failure" }
            PendingReviewSource(batch, sha, length, media, name, created, bytes)
        }
    }

    private fun withFileLock(block: () -> PendingReviewSourceStoreResult): PendingReviewSourceStoreResult =
        FileChannel.open(root.resolve(".registry.lock"), StandardOpenOption.CREATE, StandardOpenOption.WRITE).use { it.lock().use { block() } }

    companion object {
        private const val MAGIC = 0x50525331
        private const val VERSION = 1
        private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
        private fun digest(value: String): String = sha256(value.toByteArray())
    }
}

private fun key(batchId: String, sourceSha256: String): String = "$batchId\u0000$sourceSha256"
