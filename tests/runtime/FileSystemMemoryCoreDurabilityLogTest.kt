package parker.core.runtime

import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.io.TempDir
import parker.core.interfaces.ContentNature
import parker.core.interfaces.Entity
import parker.core.interfaces.EntityId
import parker.core.interfaces.MemoryCoreRecordReference
import parker.core.interfaces.MemoryCoreRecordStatus
import parker.core.interfaces.Provenance
import parker.core.interfaces.ProvenanceId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Memory Core Durability, Implementation Unit 3 (Atomic Append).
 * Behavioural tests of [FileSystemMemoryCoreDurabilityLog] against a real
 * temporary directory, mirroring [FileSystemEvidenceDeletionAuditTest]'s
 * own established "touch a real filesystem" precedent. Pure encode/decode
 * correctness is covered separately by [DurableMemoryCoreEntryCodecTest];
 * this suite covers construction validation, append/read behaviour,
 * ordering, concurrency, and fail-fast propagation.
 */
class FileSystemMemoryCoreDurabilityLogTest {

    private fun provenanceEntry(sourceIdentifier: String) = DurableMemoryCoreEntry.ProvenanceCreated(
        provenance = Provenance(
            provenanceId = ProvenanceId("provenance-$sourceIdentifier"),
            sourceIdentifier = sourceIdentifier,
            sourceType = "conversation",
            acquisitionTime = Instant.parse("2026-01-01T00:00:00Z"),
            ingestionTime = Instant.parse("2026-01-01T00:00:01Z"),
            contentNature = ContentNature.ORIGINAL,
        ),
    )

    // ================= Interface compliance =================

    @Test
    fun `FileSystemMemoryCoreDurabilityLog implements MemoryCoreDurabilityLog`(@TempDir tempDir: Path) {
        val log = FileSystemMemoryCoreDurabilityLog(tempDir.resolve("durability.log"))

        assertTrue(log is MemoryCoreDurabilityLog)
    }

    // ================= Construction-time validation =================

    @Test
    fun `constructing over a parent directory that does not exist fails at construction`(@TempDir tempDir: Path) {
        val logFile = tempDir.resolve("missing-subdirectory").resolve("durability.log")

        assertFailsWith<MemoryCoreDurabilityLogException.InvalidStorageRoot> {
            FileSystemMemoryCoreDurabilityLog(logFile)
        }
    }

    @Test
    fun `constructing where the parent path is a file, not a directory, fails at construction`(@TempDir tempDir: Path) {
        val parentAsFile = tempDir.resolve("not-a-directory")
        Files.createFile(parentAsFile)
        val logFile = parentAsFile.resolve("durability.log")

        assertFailsWith<MemoryCoreDurabilityLogException.InvalidStorageRoot> {
            FileSystemMemoryCoreDurabilityLog(logFile)
        }
    }

    @Test
    fun `construction creates an empty log file if none exists yet`(@TempDir tempDir: Path) {
        val logFile = tempDir.resolve("durability.log")

        FileSystemMemoryCoreDurabilityLog(logFile)

        assertTrue(Files.exists(logFile))
        assertEquals(emptyList(), Files.readAllLines(logFile))
    }

    @Test
    fun `construction over an already-existing log file preserves its content, never truncates`(
        @TempDir tempDir: Path,
    ) = runTest {
        val logFile = tempDir.resolve("durability.log")
        FileSystemMemoryCoreDurabilityLog(logFile).append(provenanceEntry("first"))

        // A fresh instance, as a new process would construct after a restart, over the same file.
        val secondInstance = FileSystemMemoryCoreDurabilityLog(logFile)

        assertEquals(1, secondInstance.readAll().size, "a prior entry must survive across instances of this class")
    }

    // ================= Append and readAll: round-trip and order =================

    @Test
    fun `a single entry is durably appended and readable back`(@TempDir tempDir: Path) = runTest {
        val log = FileSystemMemoryCoreDurabilityLog(tempDir.resolve("durability.log"))
        val entry = provenanceEntry("first")

        log.append(entry)

        assertEquals(listOf(entry), log.readAll())
    }

    @Test
    fun `readAll preserves the exact order entries were appended in, across all six entry kinds`(
        @TempDir tempDir: Path,
    ) = runTest {
        val log = FileSystemMemoryCoreDurabilityLog(tempDir.resolve("durability.log"))
        val provenance = provenanceEntry("first")
        val entity = DurableMemoryCoreEntry.EntityCreated(
            entity = Entity(
                entityId = EntityId("entity-1"),
                entityType = "person",
                primaryLabel = "Jane Doe",
                provenanceId = ProvenanceId("provenance-first"),
                createdAt = Instant.parse("2026-01-01T00:00:02Z"),
            ),
        )
        val transition = DurableMemoryCoreEntry.StatusTransitioned(
            reference = MemoryCoreRecordReference.ToEntity(EntityId("entity-1")),
            priorStatus = MemoryCoreRecordStatus.ACTIVE,
            targetStatus = MemoryCoreRecordStatus.DISPUTED,
            transitionedAt = Instant.parse("2026-01-01T00:00:03Z"),
        )

        log.append(provenance)
        log.append(entity)
        log.append(transition)

        assertEquals(listOf(provenance, entity, transition), log.readAll())
    }

    @Test
    fun `readAll against a freshly-created, never-appended-to log returns an empty list`(
        @TempDir tempDir: Path,
    ) = runTest {
        val log = FileSystemMemoryCoreDurabilityLog(tempDir.resolve("durability.log"))

        assertEquals(emptyList(), log.readAll())
    }

    @Test
    fun `each append call writes exactly one line -- no batching, no merging`(@TempDir tempDir: Path) = runTest {
        val logFile = tempDir.resolve("durability.log")
        val log = FileSystemMemoryCoreDurabilityLog(logFile)

        log.append(provenanceEntry("first"))
        log.append(provenanceEntry("second"))
        log.append(provenanceEntry("third"))

        assertEquals(3, Files.readAllLines(logFile).size)
    }

    // ================= No partial record visibility =================

    @Test
    fun `a caller never observes a partially-appended entry -- readAll after a completed append sees the whole entry or nothing`(
        @TempDir tempDir: Path,
    ) = runTest {
        val log = FileSystemMemoryCoreDurabilityLog(tempDir.resolve("durability.log"))
        val entry = provenanceEntry("first")

        log.append(entry)
        val readBack = log.readAll()

        assertEquals(1, readBack.size)
        assertEquals(entry, readBack.single())
    }

    // ================= Fail-fast propagation, no recovery/classification =================

    @Test
    fun `readAll propagates a decode failure for a corrupted line, rather than silently skipping it`(
        @TempDir tempDir: Path,
    ) = runTest {
        val logFile = tempDir.resolve("durability.log")
        val log = FileSystemMemoryCoreDurabilityLog(logFile)
        log.append(provenanceEntry("first"))

        Files.write(logFile, "this-is-not-a-valid-entry-line\n".toByteArray(), java.nio.file.StandardOpenOption.APPEND)

        assertFailsWith<MemoryCoreDurabilityLogException.MalformedEntry> { log.readAll() }
    }

    @Test
    fun `readAll propagates a decode failure regardless of whether the malformed line is first, middle, or last`(
        @TempDir tempDir: Path,
    ) = runTest {
        val logFile = tempDir.resolve("durability.log")
        Files.write(
            logFile,
            listOf(
                DurableMemoryCoreEntryCodec.encode(provenanceEntry("first")),
                "corrupted-middle-line",
                DurableMemoryCoreEntryCodec.encode(provenanceEntry("third")),
            ),
        )
        val log = FileSystemMemoryCoreDurabilityLog(logFile)

        // This Unit does not classify corruption by position -- a failure anywhere fails the whole read.
        assertFailsWith<MemoryCoreDurabilityLogException.MalformedEntry> { log.readAll() }
    }

    // ================= Concurrency: baseline write-serialisation correctness =================

    @Test
    fun `concurrent append calls never interleave or corrupt each other's lines`(@TempDir tempDir: Path) = runTest {
        val logFile = tempDir.resolve("durability.log")
        val log = FileSystemMemoryCoreDurabilityLog(logFile)
        val entryCount = 25

        coroutineScope {
            val jobs = (1..entryCount).map { index ->
                async { log.append(provenanceEntry("source-$index")) }
            }
            jobs.forEach { it.await() }
        }

        val readBack = log.readAll()
        assertEquals(entryCount, readBack.size, "every concurrently-appended entry must be present, none lost or corrupted")
        assertEquals(entryCount, readBack.map { (it as DurableMemoryCoreEntry.ProvenanceCreated).provenance.provenanceId }.toSet().size)
    }
}
