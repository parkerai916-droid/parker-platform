package parker.core.runtime

import java.io.IOException
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Coroutine-safe append log for the single production persistence instance.
 * The mutex below is process-local: multiple Parker OS processes must not share
 * one Knowledge Item log. No cross-process lock is provided; violating this
 * single-writer deployment requirement can append competing sequence numbers and
 * cause the next recovery to fail closed.
 *
 * A write or [FileChannel.force] failure is always reported as failure and the
 * caller must not update its in-memory projection. Filesystems may nevertheless
 * have written complete bytes before `force` reports failure; a later restart
 * validates whatever complete durable bytes exist. The failed call never reports
 * success and this component never guesses whether those bytes reached stable media.
 */
internal class FileSystemKnowledgeItemDurabilityLog(private val logFile: Path) : KnowledgeItemDurabilityLog {
    private val mutex = Mutex()

    init {
        val absolute = logFile.toAbsolutePath().normalize()
        val parent = absolute.parent
        try {
            if (parent == null || !Files.isDirectory(parent) || !Files.isWritable(parent)) {
                throw KnowledgeItemDurabilityException.InvalidStorage(logFile.toString(), "parent must exist and be writable")
            }
            if (Files.exists(absolute)) {
                if (!Files.isRegularFile(absolute) || !Files.isReadable(absolute) || !Files.isWritable(absolute)) {
                    throw KnowledgeItemDurabilityException.InvalidStorage(logFile.toString(), "file must be a readable, writable regular file")
                }
            } else Files.createFile(absolute)
        } catch (e: KnowledgeItemDurabilityException.InvalidStorage) { throw e }
        catch (e: IOException) { throw KnowledgeItemDurabilityException.InvalidStorage(logFile.toString(), "cannot open or create file", e) }
    }

    override suspend fun append(entry: DurableKnowledgeItemEntry) = mutex.withLock {
        val bytes = (DurableKnowledgeItemEntryCodec.encode(entry) + "\n").toByteArray(StandardCharsets.UTF_8)
        try {
            FileChannel.open(logFile, StandardOpenOption.WRITE, StandardOpenOption.APPEND).use { channel ->
                val buffer = ByteBuffer.wrap(bytes)
                while (buffer.hasRemaining()) channel.write(buffer)
                channel.force(true)
            }
        } catch (e: IOException) { throw KnowledgeItemDurabilityException.StorageFailure("Failed to append Knowledge Item durably", e) }
    }

    override suspend fun readAll(): List<DurableKnowledgeItemEntry> = mutex.withLock {
        val bytes = try { Files.readAllBytes(logFile) } catch (e: IOException) {
            throw KnowledgeItemDurabilityException.StorageFailure("Failed to read Knowledge Item durability log", e)
        }
        if (bytes.isEmpty()) return@withLock emptyList()
        if (bytes.last() != '\n'.code.toByte()) throw KnowledgeItemDurabilityException.CorruptLog("truncated final record (missing newline framing)")
        val text = try {
            StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString()
        } catch (e: Exception) { throw KnowledgeItemDurabilityException.CorruptLog("log is not valid UTF-8", e) }
        text.dropLast(1).split('\n').mapIndexed { index, line ->
            if (line.isEmpty()) throw KnowledgeItemDurabilityException.CorruptLog("empty record at line ${index + 1}")
            DurableKnowledgeItemEntryCodec.decode(line, index + 1)
        }
    }
}
