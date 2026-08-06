package parker.core.runtime

import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Memory Core Durability, Implementation Unit 3 (Atomic Append). The
 * production, filesystem-backed implementation of [MemoryCoreDurabilityLog]
 * (Unit 2) -- the first Unit in this Programme where Parker actually
 * begins persisting Memory Core operations rather than merely describing
 * them. Governed by `docs/architecture/MEMORY_CORE_DURABILITY_CONTRACT_DESIGN.md`
 * ("the Contract Design") Section 4 (one operation, one atomic durable
 * act) and Section 6 (partial-write tolerance versus corruption), and
 * `docs/architecture/MEMORY_CORE_DURABILITY_SCOPE_LOCK.md` ("the Scope
 * Lock") Section 7 (Atomicity Rules) and Section 8 (Recovery Rules,
 * applied here only to the extent this Unit's own governing task
 * authorises -- see below).
 *
 * ## A disclosed unit-numbering divergence from
 * `docs/implementation/MEMORY_CORE_DURABILITY_IMPLEMENTATION_PLAN.md`
 * ("the Implementation Plan")
 *
 * The Implementation Plan's own Unit 2 section, not its own Unit 3
 * section, is what originally specified this exact class: "a single
 * concrete class, `FileSystemMemoryCoreDurabilityLog`... Two operations:
 * `suspend fun append(entry: DurableMemoryCoreEntry)`... and
 * `suspend fun readAll(): List<DurableMemoryCoreEntry>`." This session's
 * own prior Unit (labelled "Unit 2" for this session) redirected that
 * Plan-Unit-2 scope to build only the interface half
 * (`MemoryCoreDurabilityLog`, plus a test-only fake), explicitly
 * instructing that "Do not create a production implementation yet."
 * This session's own
 * current task, labelled "Unit 3: Atomic Append," is what completes that
 * deferred work -- it is not the Implementation Plan's own,
 * differently-scoped "Unit 3" (which builds `DurableMemoryCore`, the
 * `MemoryCore`/`MemoryRetrieval`-implementing decorator wrapping this
 * class together with `InMemoryMemoryCore`). This governing task's own
 * explicit exclusion list confirms this reading directly: "no Memory Core
 * decorator" and "no runtime composition" are named as explicitly out of
 * scope for this session's Unit 3, exactly what the Implementation Plan's
 * own Unit 3 would otherwise require. This class fulfils the Implementation
 * Plan's own Unit 2 output specification, under this session's own Unit 3
 * label -- disclosed here, and in full in this Unit's own Completion
 * Review, rather than silently reconciled. No Boundary Review was required
 * to proceed: the actual capability built here was already fully
 * authorised by the Implementation Plan's own Unit 2 text; only the
 * session-level label attached to building it has changed.
 *
 * ## Mechanism: direct append-then-force, not temp-file-then-atomic-move
 *
 * Unlike [FileSystemEvidenceArtifactStorage]'s own create-once,
 * temp-file/atomic-move discipline, [append] mirrors
 * [FileSystemEvidenceDeletionAudit.record]'s own simpler append-then-force
 * discipline exactly: open in append mode, write the encoded line, call
 * [FileChannel.force]. This is not a lesser guarantee chosen for
 * convenience -- it is the mechanism the Contract Design's own Section 6
 * already anticipates and requires: "a write that did not complete before
 * an interruption is treated as a write that never happened -- discarded
 * during replay, never partially applied." A durability design built
 * around a future replay step (Implementation Unit 4, not built by this
 * Unit) that already tolerates and discards an interrupted trailing write
 * has no need for the stronger, more expensive temp-file/atomic-move
 * guarantee [FileSystemEvidenceArtifactStorage] uses to avoid a
 * *different* problem (a create-once artefact left ambiguously
 * "maybe-written" under a caller-visible identifier) that does not apply
 * to an append-only sequential log.
 *
 * ## Fail-fast: this Unit does not implement recovery, replay, or
 * corruption classification
 *
 * [readAll] decodes every line, in file order, via
 * [DurableMemoryCoreEntryCodec.decode], and propagates the first decode
 * failure as a thrown exception -- it does **not** attempt to determine
 * whether a failing line is the file's own last, plausibly-interrupted
 * line (discardable) or genuine corruption of an earlier, already-
 * committed line (unrecoverable). That position-aware classification is
 * Implementation Unit 4's own, later, explicitly separate responsibility
 * (Contract Design Section 7). This Unit's own governing task explicitly
 * excludes both "no recovery logic" and "no replay logic." [readAll]'s own uniform,
 * fail-fast behaviour is the correct, minimal contract for this Unit
 * alone -- never silently returning a truncated result, and never
 * guessing which lines to keep.
 *
 * ## Concurrency: one [Mutex], serialising both operations -- a baseline
 * correctness guard, not Implementation Unit 7's own later guarantee
 *
 * [mutex] prevents two concurrent [append] calls from interleaving their
 * own writes to [logFile] (which would corrupt the log at the byte
 * level, independent of any higher-level concern), and prevents [readAll]
 * from ever observing a write that is only partially flushed. This
 * mirrors [FileSystemEvidenceDeletionAudit]'s own identical, already-accepted
 * `Mutex` precedent for the exact same class of concern -- a single
 * class's own internal write-serialisation, not a new capability. This is
 * distinct from, and does not attempt to satisfy, Implementation Unit 7's
 * own later requirement that "a durable write and the corresponding
 * in-memory update occur as one atomic unit from the caller's own
 * perspective," which
 * necessarily spans this class together with a not-yet-built
 * `InMemoryMemoryCore` interaction this Unit has no access to.
 *
 * ## What this Unit deliberately does not implement
 *
 * Recovery; replay; restore functions; identifier restoration; lifecycle
 * replay; runtime composition; a `MemoryCore`/`MemoryRetrieval` decorator;
 * schema-version migration/evolution (only [DurableMemoryCoreEntry.CURRENT_SCHEMA_VERSION]
 * is ever produced or accepted); batching; compaction; checkpointing; any
 * concurrency beyond the single [Mutex] described above.
 *
 * @param logFile The exact file path this instance appends to and reads
 *   from. Its parent directory must already exist and be writable --
 *   validated once, at construction, mirroring
 *   [FileSystemEvidenceDeletionAudit]'s own identical "fail fast at
 *   construction" discipline. If [logFile] itself does not yet exist, it
 *   is created, empty, at construction; if it already exists (a prior
 *   process's own log), its existing content is preserved, never
 *   truncated.
 */
internal class FileSystemMemoryCoreDurabilityLog(private val logFile: Path) : MemoryCoreDurabilityLog {

    private val mutex = Mutex()

    init {
        val parent = logFile.toAbsolutePath().normalize().parent
        if (parent == null || !Files.exists(parent)) {
            throw MemoryCoreDurabilityLogException.InvalidStorageRoot(
                logFile.toString(),
                "durability log parent directory does not exist: '$parent'",
            )
        }
        if (!Files.isDirectory(parent)) {
            throw MemoryCoreDurabilityLogException.InvalidStorageRoot(
                logFile.toString(),
                "durability log parent path is not a directory: '$parent'",
            )
        }
        if (!Files.isWritable(parent)) {
            throw MemoryCoreDurabilityLogException.InvalidStorageRoot(
                logFile.toString(),
                "durability log parent directory is not writable: '$parent'",
            )
        }
        try {
            if (!Files.exists(logFile)) {
                Files.createFile(logFile)
            }
        } catch (e: IOException) {
            throw MemoryCoreDurabilityLogException.InvalidStorageRoot(
                logFile.toString(),
                "failed to create durability log file: ${e.message}",
            )
        }
    }

    override suspend fun append(entry: DurableMemoryCoreEntry) {
        val line = DurableMemoryCoreEntryCodec.encode(entry) + "\n"

        mutex.withLock {
            try {
                FileChannel.open(logFile, StandardOpenOption.WRITE, StandardOpenOption.APPEND).use { channel ->
                    val buffer = ByteBuffer.wrap(line.toByteArray(StandardCharsets.UTF_8))
                    // FileChannel.write's general contract permits a partial write per call --
                    // loop until every byte is actually written, mirroring
                    // FileSystemEvidenceDeletionAudit.record's own identical discipline.
                    while (buffer.hasRemaining()) {
                        channel.write(buffer)
                    }
                    channel.force(true)
                }
            } catch (e: IOException) {
                throw MemoryCoreDurabilityLogException.StorageIOFailure(
                    "Failed to durably append a Memory Core durability log entry",
                    e,
                )
            }
        }
    }

    override suspend fun readAll(): List<DurableMemoryCoreEntry> = mutex.withLock {
        val lines = try {
            Files.readAllLines(logFile, StandardCharsets.UTF_8)
        } catch (e: IOException) {
            throw MemoryCoreDurabilityLogException.StorageIOFailure(
                "Failed to read the Memory Core durability log",
                e,
            )
        }
        lines.mapIndexed { index, line -> DurableMemoryCoreEntryCodec.decode(line, index + 1) }
    }
}

/**
 * Typed failures for [FileSystemMemoryCoreDurabilityLog] and
 * [DurableMemoryCoreEntryCodec]. Sealed and thrown -- not returned as a
 * sealed result type -- mirroring [EvidenceArtifactStorageException]'s
 * own identical, already-established convention for this class of
 * failure. `internal`, matching [MemoryCoreDurabilityLog]'s own
 * visibility exactly: this exception hierarchy is never surfaced through
 * `MemoryCore`'s or `MemoryRetrieval`'s own public contracts.
 */
internal sealed class MemoryCoreDurabilityLogException(message: String, cause: Throwable? = null) :
    RuntimeException(message, cause) {

    /** The supplied log file's own path, or its parent directory, failed validation at construction time. */
    class InvalidStorageRoot(val path: String, val reason: String) :
        MemoryCoreDurabilityLogException("Memory Core durability log path '$path' is invalid: $reason")

    /** An underlying I/O failure occurred while appending or reading. [cause] is always the original exception, never discarded. */
    class StorageIOFailure(message: String, cause: Throwable) : MemoryCoreDurabilityLogException(message, cause)

    /**
     * A durability log line could not be decoded into a [DurableMemoryCoreEntry] --
     * a missing field, an unparseable value, or invalid Base64. Deliberately
     * carries no claim about *why* the line is malformed beyond [reason];
     * distinguishing a partial trailing write from genuine corruption is
     * Implementation Unit 4's own, later responsibility, not this
     * exception's own.
     */
    class MalformedEntry(val lineNumber: Int, val reason: String, cause: Throwable? = null) :
        MemoryCoreDurabilityLogException("Memory Core durability log line $lineNumber is malformed: $reason", cause)

    /**
     * A durability log entry carries a `schemaVersion` this code does not
     * recognise (Contract Design Section 8: "unknown future versions must
     * never be silently interpreted"). Distinct from [MalformedEntry]:
     * the entry decoded its own envelope fields correctly enough to know
     * its own kind and version -- it is refused on principle, not because
     * it could not be parsed.
     */
    class UnrecognizedSchemaVersion(val kind: String, val schemaVersion: Int) :
        MemoryCoreDurabilityLogException(
            "Memory Core durability log entry of kind '$kind' carries schema version $schemaVersion, which this " +
                "code does not recognise -- refusing to guess how to interpret it",
        )
}
