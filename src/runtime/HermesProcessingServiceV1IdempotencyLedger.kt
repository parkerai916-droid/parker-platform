package parker.core.runtime

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException
import java.io.IOException
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.security.MessageDigest
import java.time.Instant
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import parker.core.interfaces.HermesProcessingServiceV1Limits
import parker.core.interfaces.HermesProcessingServiceV1Request
import parker.core.interfaces.HermesProcessingServiceV1Framing
import parker.core.interfaces.HermesV1BatchId
import parker.core.interfaces.HermesV1Failure
import parker.core.interfaces.HermesV1FailureCategory
import parker.core.interfaces.HermesV1FailureDetailCode
import parker.core.interfaces.HermesV1JobId
import parker.core.interfaces.HermesV1MediaType
import parker.core.interfaces.HermesV1OccurrenceId
import parker.core.interfaces.HermesV1OriginalFilename
import parker.core.interfaces.HermesV1ProcessingMethod
import parker.core.interfaces.HermesV1ProcessingPrincipal
import parker.core.interfaces.HermesV1RequestId
import parker.core.interfaces.HermesV1Sha256
import parker.core.interfaces.HermesV1SourceReference
import parker.core.interfaces.HermesV1SourceSize

/** Closed durable lifecycle for one logical Hermes request. */
enum class HermesV1LedgerState {
    RECEIVED,
    VERIFIED,
    PROCESSING,
    COMPLETE,
    FAILED,
}

/**
 * Identity compared for a requestId retry. The original filename is deliberately
 * excluded: it is descriptive metadata, not authoritative source identity.
 */
data class HermesV1RequestIdentity(
    val principal: HermesV1ProcessingPrincipal,
    val requestId: HermesV1RequestId,
    val jobId: HermesV1JobId,
    val occurrenceId: HermesV1OccurrenceId,
    val batchId: HermesV1BatchId,
    val sourceReference: HermesV1SourceReference,
    val sourceSha256: HermesV1Sha256,
    val sizeBytes: HermesV1SourceSize,
    val mediaType: HermesV1MediaType,
    val requestedMethods: List<HermesV1ProcessingMethod>,
) {
    companion object {
        fun from(principal: HermesV1ProcessingPrincipal, request: HermesProcessingServiceV1Request) = HermesV1RequestIdentity(
            principal = principal,
            requestId = request.requestId,
            jobId = request.jobId,
            occurrenceId = request.occurrenceId,
            batchId = request.batchId,
            sourceReference = request.source.reference,
            sourceSha256 = request.source.sourceSha256,
            sizeBytes = request.source.sizeBytes,
            mediaType = request.source.mediaType,
            requestedMethods = request.requestedMethods,
        )
    }
}

/** Defensive, bounded bytes for a canonical serialized v1 response. */
class HermesV1LedgerResult(bytes: ByteArray) {
    private val value = bytes.copyOf()

    init {
        require(value.isNotEmpty()) { "durable result must not be empty" }
        require(value.size.toLong() <= HermesProcessingServiceV1Limits.MAX_INLINE_RESPONSE_BYTES) {
            "durable result exceeds the v1 inline response limit"
        }
        require(value.toString(Charsets.UTF_8).toByteArray(Charsets.UTF_8).contentEquals(value)) {
            "durable result must be valid UTF-8"
        }
        HermesProcessingServiceV1Framing.frameResponseBody(value)
    }

    fun bytes(): ByteArray = value.copyOf()

    override fun equals(other: Any?): Boolean = other is HermesV1LedgerResult && value.contentEquals(other.value)
    override fun hashCode(): Int = value.contentHashCode()
}

data class HermesV1LedgerRecord(
    val identity: HermesV1RequestIdentity,
    val state: HermesV1LedgerState,
    val createdAt: Instant,
    val updatedAt: Instant,
    val expiresAt: Instant?,
    val terminalResult: HermesV1LedgerResult? = null,
    val failure: HermesV1Failure? = null,
) {
    init {
        require(!updatedAt.isBefore(createdAt)) { "ledger update timestamp precedes creation" }
        require(expiresAt == null || !expiresAt.isBefore(updatedAt)) { "ledger expiry precedes update" }
        when (state) {
            HermesV1LedgerState.COMPLETE -> require(terminalResult != null && failure == null) { "COMPLETE requires only a terminal result" }
            HermesV1LedgerState.FAILED -> require(failure != null && terminalResult == null) { "FAILED requires only a failure" }
            else -> require(terminalResult == null && failure == null && expiresAt == null) { "active records cannot carry terminal data or expiry" }
        }
    }
}

sealed interface HermesV1LedgerClaim {
    data class Created(val record: HermesV1LedgerRecord) : HermesV1LedgerClaim
    data class Existing(val record: HermesV1LedgerRecord) : HermesV1LedgerClaim
    data class Rejected(val failure: HermesV1Failure) : HermesV1LedgerClaim
}

sealed interface HermesV1LedgerTransition {
    data class Updated(val record: HermesV1LedgerRecord) : HermesV1LedgerTransition
    data class Rejected(val failure: HermesV1Failure) : HermesV1LedgerTransition
}

class HermesV1LedgerException(val failure: HermesV1Failure, cause: Throwable? = null) : RuntimeException(failure.detail, cause)

/**
 * Separate requestId ledger. It does not replace the existing
 * (batchId, sourceSha256) HermesProcessingResult registry.
 */
class HermesProcessingServiceV1IdempotencyLedger(storageRoot: Path) {
    private val root = validateRoot(storageRoot.toAbsolutePath().normalize())
    private val records = root.resolve("records")
    private val mutex = ReentrantLock()

    init {
        Files.createDirectories(records)
        validateExistingRecords()
    }

    fun claim(principal: HermesV1ProcessingPrincipal, request: HermesProcessingServiceV1Request, now: Instant = Instant.now()): HermesV1LedgerClaim = locked {
        val identity = HermesV1RequestIdentity.from(principal, request)
        val target = target(identity)
        if (Files.exists(target)) {
            val existing = readRecord(target)
            return@locked if (existing.identity == identity) HermesV1LedgerClaim.Existing(existing)
            else HermesV1LedgerClaim.Rejected(failure(HermesV1FailureDetailCode.REQUEST_IDENTITY_CONFLICT, false))
        }
        val record = HermesV1LedgerRecord(identity, HermesV1LedgerState.RECEIVED, now, now, null)
        writeRecord(target, record)
        HermesV1LedgerClaim.Created(record)
    }

    fun find(principal: HermesV1ProcessingPrincipal, requestId: HermesV1RequestId): HermesV1LedgerRecord? = locked {
        val target = target(principal, requestId)
        if (!Files.exists(target)) null else readRecord(target)
    }

    fun transition(
        principal: HermesV1ProcessingPrincipal,
        request: HermesProcessingServiceV1Request,
        next: HermesV1LedgerState,
        result: HermesV1LedgerResult? = null,
        failure: HermesV1Failure? = null,
        now: Instant = Instant.now(),
    ): HermesV1LedgerTransition = locked {
        val identity = HermesV1RequestIdentity.from(principal, request)
        val target = target(identity)
        if (!Files.exists(target)) throw HermesV1LedgerException(failure(HermesV1FailureDetailCode.LEDGER_IO_FAILURE, true))
        val current = readRecord(target)
        if (current.identity != identity) return@locked HermesV1LedgerTransition.Rejected(failure(HermesV1FailureDetailCode.REQUEST_IDENTITY_CONFLICT, false))
        if (!allowed(current.state, next)) return@locked HermesV1LedgerTransition.Rejected(failure(HermesV1FailureDetailCode.INVALID_STATE_TRANSITION, false))
        if (next == HermesV1LedgerState.COMPLETE && result == null) return@locked HermesV1LedgerTransition.Rejected(failure(HermesV1FailureDetailCode.INVALID_STATE_TRANSITION, false))
        if (next == HermesV1LedgerState.FAILED && failure == null) return@locked HermesV1LedgerTransition.Rejected(failure(HermesV1FailureDetailCode.INVALID_STATE_TRANSITION, false))
        if (next != HermesV1LedgerState.COMPLETE && result != null) return@locked HermesV1LedgerTransition.Rejected(failure(HermesV1FailureDetailCode.INVALID_STATE_TRANSITION, false))
        if (next != HermesV1LedgerState.FAILED && failure != null) return@locked HermesV1LedgerTransition.Rejected(failure(HermesV1FailureDetailCode.INVALID_STATE_TRANSITION, false))
        val updated = HermesV1LedgerRecord(
            identity = identity,
            state = next,
            createdAt = current.createdAt,
            updatedAt = now,
            expiresAt = if (next == HermesV1LedgerState.COMPLETE || next == HermesV1LedgerState.FAILED) now.plus(HermesProcessingServiceV1Limits.REQUEST_RESULT_RETENTION) else null,
            terminalResult = result,
            failure = failure,
        )
        writeRecord(target, updated)
        HermesV1LedgerTransition.Updated(updated)
    }

    /** Deletes at most [maxRecords] expired terminal records and returns the count. */
    fun cleanupExpired(now: Instant = Instant.now(), maxRecords: Int = 100): Int {
        require(maxRecords >= 0) { "cleanup limit must not be negative" }
        return locked {
            var deleted = 0
            recordPaths().forEach { path ->
                if (deleted == maxRecords) return@forEach
                val record = readRecord(path)
                if (record.expiresAt != null && !record.expiresAt.isAfter(now)) {
                    Files.delete(path)
                    deleted++
                }
            }
            deleted
        }
    }

    internal fun recordPathForTesting(principal: HermesV1ProcessingPrincipal, requestId: HermesV1RequestId): Path = target(principal, requestId)

    private fun allowed(current: HermesV1LedgerState, next: HermesV1LedgerState): Boolean = when (current) {
        HermesV1LedgerState.RECEIVED -> next == HermesV1LedgerState.VERIFIED
        HermesV1LedgerState.VERIFIED -> next == HermesV1LedgerState.PROCESSING || next == HermesV1LedgerState.FAILED
        HermesV1LedgerState.PROCESSING -> next == HermesV1LedgerState.COMPLETE || next == HermesV1LedgerState.FAILED
        HermesV1LedgerState.COMPLETE, HermesV1LedgerState.FAILED -> false
    }

    private fun validateExistingRecords() {
        Files.list(records).use { stream ->
            stream.forEach { path ->
                if (!Files.isRegularFile(path) || !path.fileName.toString().endsWith(RECORD_SUFFIX)) {
                    throw HermesV1LedgerException(failure(HermesV1FailureDetailCode.LEDGER_CORRUPT, false))
                }
                readRecord(path)
            }
        }
    }

    private fun recordPaths(): List<Path> = Files.list(records).use { stream ->
        stream.sorted().toList().also { paths ->
            paths.forEach { path ->
                if (!Files.isRegularFile(path) || !path.fileName.toString().endsWith(RECORD_SUFFIX)) {
                    throw HermesV1LedgerException(failure(HermesV1FailureDetailCode.LEDGER_CORRUPT, false))
                }
            }
        }
    }

    private fun readRecord(path: Path): HermesV1LedgerRecord = try {
        Files.newInputStream(path, StandardOpenOption.READ).use { input ->
            DataInputStream(input).use { data ->
                require(data.readInt() == MAGIC) { "ledger magic mismatch" }
                val version = data.readInt()
                if (version != SCHEMA_VERSION) throw HermesV1LedgerException(failure(HermesV1FailureDetailCode.LEDGER_SCHEMA_MISMATCH, false))
                val record = readRecordBody(data)
                require(data.read() == -1) { "ledger record has trailing bytes" }
                record
            }
        }
    } catch (e: HermesV1LedgerException) {
        throw e
    } catch (e: Exception) {
        throw HermesV1LedgerException(failure(HermesV1FailureDetailCode.LEDGER_CORRUPT, false), e)
    }

    private fun readRecordBody(data: DataInputStream): HermesV1LedgerRecord {
        val identity = HermesV1RequestIdentity(
            HermesV1ProcessingPrincipal(data.readUTF()),
            HermesV1RequestId(data.readUTF()),
            HermesV1JobId(data.readUTF()),
            HermesV1OccurrenceId(data.readUTF()),
            HermesV1BatchId(data.readUTF()),
            HermesV1SourceReference(data.readUTF()),
            HermesV1Sha256(data.readUTF()),
            HermesV1SourceSize(data.readLong()),
            HermesV1MediaType(data.readUTF()),
            (0 until readCount(data, HermesProcessingServiceV1Limits.MAX_METHODS)).map { HermesV1ProcessingMethod.fromWireValue(data.readUTF()) },
        )
        val state = try { HermesV1LedgerState.valueOf(data.readUTF()) } catch (e: Exception) { throw IllegalArgumentException("invalid ledger state", e) }
        val created = Instant.parse(data.readUTF())
        val updated = Instant.parse(data.readUTF())
        val expires = if (data.readBoolean()) Instant.parse(data.readUTF()) else null
        val result = if (data.readBoolean()) {
            val length = data.readInt()
            require(length in 1..HermesProcessingServiceV1Limits.MAX_INLINE_RESPONSE_BYTES) { "invalid durable result length" }
            HermesV1LedgerResult(ByteArray(length).also(data::readFully))
        } else null
        val failure = if (data.readBoolean()) {
            val category = HermesV1FailureCategory.fromWireValue(data.readUTF())
            val detailCode = HermesV1FailureDetailCode.fromWireValue(data.readUTF())
            HermesV1Failure(category, detailCode, data.readBoolean(), data.readUTF())
        } else null
        return HermesV1LedgerRecord(identity, state, created, updated, expires, result, failure)
    }

    private fun writeRecord(path: Path, record: HermesV1LedgerRecord) {
        val temporary = try { Files.createTempFile(records, ".record-", ".tmp") } catch (e: IOException) {
            throw HermesV1LedgerException(failure(HermesV1FailureDetailCode.LEDGER_IO_FAILURE, true), e)
        }
        try {
            FileChannel.open(temporary, StandardOpenOption.WRITE).use { channel ->
                val data = DataOutputStream(java.nio.channels.Channels.newOutputStream(channel))
                data.writeInt(MAGIC)
                data.writeInt(SCHEMA_VERSION)
                writeRecordBody(data, record)
                data.flush()
                channel.force(true)
            }
            Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        } catch (e: Exception) {
            throw HermesV1LedgerException(failure(HermesV1FailureDetailCode.LEDGER_IO_FAILURE, true), e)
        } finally {
            Files.deleteIfExists(temporary)
        }
    }

    private fun writeRecordBody(data: DataOutputStream, record: HermesV1LedgerRecord) {
        val i = record.identity
        data.writeUTF(i.principal.value); data.writeUTF(i.requestId.value); data.writeUTF(i.jobId.value)
        data.writeUTF(i.occurrenceId.value); data.writeUTF(i.batchId.value); data.writeUTF(i.sourceReference.value)
        data.writeUTF(i.sourceSha256.value); data.writeLong(i.sizeBytes.value); data.writeUTF(i.mediaType.value)
        data.writeInt(i.requestedMethods.size); i.requestedMethods.forEach { data.writeUTF(it.wireValue) }
        data.writeUTF(record.state.name); data.writeUTF(record.createdAt.toString()); data.writeUTF(record.updatedAt.toString())
        data.writeBoolean(record.expiresAt != null); if (record.expiresAt != null) data.writeUTF(record.expiresAt.toString())
        data.writeBoolean(record.terminalResult != null)
        if (record.terminalResult != null) { val bytes = record.terminalResult.bytes(); data.writeInt(bytes.size); data.write(bytes) }
        data.writeBoolean(record.failure != null)
        if (record.failure != null) { data.writeUTF(record.failure.category.wireValue); data.writeUTF(record.failure.detailCode.wireValue); data.writeBoolean(record.failure.retryable); data.writeUTF(record.failure.detail) }
    }

    private fun target(identity: HermesV1RequestIdentity): Path = target(identity.principal, identity.requestId)

    private fun target(principal: HermesV1ProcessingPrincipal, requestId: HermesV1RequestId): Path = records.resolve(digest(principal.value + "\u0000" + requestId.value) + RECORD_SUFFIX)

    private fun <T> locked(block: () -> T): T = mutex.withLock {
        try {
            FileChannel.open(root.resolve(LOCK_FILE), StandardOpenOption.CREATE, StandardOpenOption.WRITE).use { channel ->
                channel.lock().use { block() }
            }
        } catch (e: HermesV1LedgerException) {
            throw e
        } catch (e: Exception) {
            throw HermesV1LedgerException(failure(HermesV1FailureDetailCode.LEDGER_IO_FAILURE, true), e)
        }
    }

    private fun failure(code: HermesV1FailureDetailCode, retryable: Boolean) = HermesV1Failure(code.category, code, retryable, "Hermes request ledger operation failed")

    private companion object {
        const val MAGIC = 0x48564C47
        const val SCHEMA_VERSION = 1
        const val RECORD_SUFFIX = ".record"
        const val LOCK_FILE = ".ledger.lock"

        fun validateRoot(root: Path): Path {
            try {
                Files.createDirectories(root)
                require(Files.isDirectory(root) && Files.isWritable(root)) { "ledger root is not writable" }
            } catch (e: Exception) {
                throw HermesV1LedgerException(HermesV1Failure(HermesV1FailureCategory.INTERNAL, HermesV1FailureDetailCode.LEDGER_IO_FAILURE, true, "Hermes request ledger is unavailable"), e)
            }
            return root
        }

        fun readCount(data: DataInputStream, max: Int): Int {
            val count = data.readInt()
            require(count in 0..max) { "invalid ledger count" }
            return count
        }

        fun digest(value: String): String = MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString("") { "%02x".format(it.toInt() and 0xff) }
    }
}
