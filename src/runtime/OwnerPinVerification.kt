package parker.core.runtime

import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.params.Argon2Parameters
import parker.core.interfaces.PrincipalId
import java.nio.charset.StandardCharsets
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.security.MessageDigest
import java.time.Clock
import java.time.Instant
import java.util.Base64
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.min

/** A six-digit PIN which deliberately never exposes its value through rendering or debugging. */
@JvmInline
value class OwnerPin private constructor(internal val secret: String) {
    override fun toString(): String = "OwnerPin([REDACTED])"

    companion object {
        private val FORMAT = Regex("^[0-9]{6}$")

        fun parse(value: String?): OwnerPin? =
            value?.takeIf { FORMAT.matches(it) }?.let(::OwnerPin)
    }
}

/** Bounded parser result used by callers that need to distinguish public format rejection. */
sealed interface OwnerPinInput {
    data class Valid(val pin: OwnerPin) : OwnerPinInput
    data object Invalid : OwnerPinInput

    companion object {
        fun parse(value: String?): OwnerPinInput =
            OwnerPin.parse(value)?.let { Valid(it) } ?: Invalid
    }
}

enum class OwnerPinVerificationResult {
    VERIFIED,
    REJECTED,
    TEMPORARILY_LOCKED,
    UNAVAILABLE,
}

enum class OwnerPinAuditEvent {
    PIN_VERIFICATION_SUCCEEDED,
    PIN_VERIFICATION_REJECTED,
    PIN_VERIFICATION_LOCKED,
    PIN_VERIFIER_UNAVAILABLE,
}

data class OwnerPinAuditRecord(
    val event: OwnerPinAuditEvent,
    val principalId: PrincipalId,
    val occurredAt: Instant,
    val reason: String,
)

fun interface OwnerPinSecurityAudit {
    fun record(record: OwnerPinAuditRecord)
}

object NoOpOwnerPinSecurityAudit : OwnerPinSecurityAudit {
    override fun record(record: OwnerPinAuditRecord) = Unit
}

/** Audit sink with bounded, non-secret tab-separated records. */
class FileSystemOwnerPinSecurityAudit(
    private val logFile: Path,
    private val maximumBytes: Long = DEFAULT_MAXIMUM_BYTES,
) : OwnerPinSecurityAudit {
    init {
        require(maximumBytes >= 4096L) { "Owner PIN audit limit is too small" }
        logFile.parent?.let(Files::createDirectories)
    }

    @Synchronized
    override fun record(record: OwnerPinAuditRecord) {
        require(record.reason.length <= 120) { "Owner PIN audit reason is too long" }
        require(!Files.isSymbolicLink(logFile)) { "Owner PIN audit path may not be a symlink" }
        val line = listOf(
            "OWNER_PIN_AUDIT_V1",
            record.event.name,
            record.principalId.value,
            record.occurredAt.toString(),
            record.reason,
        ).joinToString("\t") + "\n"
        val encoded = StandardCharsets.UTF_8.encode(line)
        if (Files.exists(logFile) && Files.size(logFile) + encoded.remaining() > maximumBytes) {
            val rotated = logFile.resolveSibling("${logFile.fileName}.1")
            if (Files.isSymbolicLink(rotated)) throw IllegalStateException("Owner PIN audit rotation path may not be a symlink")
            Files.move(logFile, rotated, StandardCopyOption.REPLACE_EXISTING)
        }
        Files.newByteChannel(
            logFile,
            StandardOpenOption.CREATE,
            StandardOpenOption.WRITE,
            StandardOpenOption.APPEND,
        ).use { channel ->
            while (encoded.hasRemaining()) channel.write(encoded)
        }
    }

    companion object {
        const val DEFAULT_MAXIMUM_BYTES: Long = 1L shl 20
    }
}

data class OwnerPinAttemptState(
    val failedCount: Int = 0,
    val lockoutUntilEpochMillis: Long? = null,
    val lockoutCycles: Int = 0,
    val lastOutcomeEpochMillis: Long? = null,
)

/**
 * Durable per-principal verifier state. The lock file serializes updates across runtime
 * processes; the state file contains no PIN, hash, request body, or evidence identity.
 */
class FileSystemOwnerPinAttemptStateStore(private val root: Path) {
    init {
        require(!Files.exists(root) || !Files.isSymbolicLink(root)) { "Owner PIN state root may not be a symlink" }
        Files.createDirectories(root)
    }

    fun <T> withLocked(principalId: PrincipalId, block: (OwnerPinAttemptState) -> Pair<OwnerPinAttemptState, T>): T {
        val safeKey = sha256(principalId.value.toByteArray(StandardCharsets.UTF_8))
        synchronized(inProcessLocks.computeIfAbsent(safeKey) { Any() }) {
            val lockPath = root.resolve("$safeKey.lock")
            require(!Files.isSymbolicLink(lockPath)) { "Owner PIN lock path may not be a symlink" }
            Files.createDirectories(root)
            FileChannel.open(lockPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE).use { channel ->
                channel.lock().use {
                    val current = read(root.resolve("$safeKey.state"))
                    val (next, result) = block(current)
                    write(root.resolve("$safeKey.state"), next)
                    return result
                }
            }
        }
    }

    private fun read(path: Path): OwnerPinAttemptState {
        require(!Files.isSymbolicLink(path)) { "Owner PIN state path may not be a symlink" }
        if (!Files.isRegularFile(path)) return OwnerPinAttemptState()
        val fields = Files.readAllLines(path, StandardCharsets.UTF_8)
            .flatMap { it.split('=', limit = 2).takeIf { pair -> pair.size == 2 }?.let { pair -> listOf(pair[0], pair[1]) } ?: emptyList() }
            .chunked(2).associate { it[0] to it[1] }
        require(fields["version"] == "1") { "Unsupported Owner PIN state" }
        return OwnerPinAttemptState(
            failedCount = fields["failedCount"]?.toIntOrNull()?.coerceIn(0, MAX_FAILED_COUNT)
                ?: error("Invalid Owner PIN failed count"),
            lockoutUntilEpochMillis = fields["lockoutUntil"]?.takeIf { it != "" }?.toLongOrNull()?.also { require(it >= 0) },
            lockoutCycles = fields["lockoutCycles"]?.toIntOrNull()?.coerceIn(0, MAX_LOCKOUT_CYCLES)
                ?: error("Invalid Owner PIN lockout cycles"),
            lastOutcomeEpochMillis = fields["lastOutcome"]?.takeIf { it != "" }?.toLongOrNull()?.also { require(it >= 0) },
        )
    }

    private fun write(path: Path, state: OwnerPinAttemptState) {
        require(!Files.isSymbolicLink(path)) { "Owner PIN state path may not be a symlink" }
        val temporary = Files.createTempFile(root, ".owner-pin-state-", "-${UUID.randomUUID()}.tmp")
        val body = buildString {
            appendLine("version=1")
            appendLine("failedCount=${state.failedCount}")
            appendLine("lockoutUntil=${state.lockoutUntilEpochMillis ?: ""}")
            appendLine("lockoutCycles=${state.lockoutCycles}")
            appendLine("lastOutcome=${state.lastOutcomeEpochMillis ?: ""}")
        }
        Files.writeString(temporary, body, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
        try {
            Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING)
        }
    }

    companion object {
        private const val MAX_FAILED_COUNT = 100
        private const val MAX_LOCKOUT_CYCLES = 16
        private val inProcessLocks = ConcurrentHashMap<String, Any>()

        private fun sha256(bytes: ByteArray): String =
            MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it.toInt() and 255) }
    }
}

data class OwnerPinArgon2Limits(
    val minimumMemoryKiB: Int = 8 * 1024,
    val maximumMemoryKiB: Int = 64 * 1024,
    val minimumIterations: Int = 1,
    val maximumIterations: Int = 5,
    val minimumParallelism: Int = 1,
    val maximumParallelism: Int = 2,
)

/** Strict Argon2id PHC record. The PHC text is never exposed by toString or audit output. */
class OwnerPinArgon2Hash private constructor(
    private val memoryKiB: Int,
    private val iterations: Int,
    private val parallelism: Int,
    private val salt: ByteArray,
    private val expected: ByteArray,
) {
    fun verify(pin: OwnerPin): Boolean {
        val output = ByteArray(expected.size)
        val parameters = Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
            .withVersion(Argon2Parameters.ARGON2_VERSION_13)
            .withMemoryAsKB(memoryKiB)
            .withIterations(iterations)
            .withParallelism(parallelism)
            .withSalt(salt)
            .build()
        Argon2BytesGenerator().apply { init(parameters) }
            .generateBytes(pin.secret.toCharArray(), output)
        return MessageDigest.isEqual(expected, output)
    }

    companion object {
        private val PHC = Regex("""^\${'$'}argon2id\${'$'}v=19\${'$'}m=(\d+),t=(\d+),p=(\d+)\${'$'}([A-Za-z0-9+/]+={0,2})\${'$'}([A-Za-z0-9+/]+={0,2})${'$'}""")

        fun parse(value: String, limits: OwnerPinArgon2Limits = OwnerPinArgon2Limits()): OwnerPinArgon2Hash {
            require(value.isNotEmpty() && value.length <= 512 && !value.any { it.isWhitespace() }) { "Invalid Owner PIN hash record" }
            val match = PHC.matchEntire(value) ?: error("Unsupported Owner PIN hash record")
            val memory = match.groupValues[1].toIntOrNull() ?: error("Invalid Argon2 memory")
            val iterations = match.groupValues[2].toIntOrNull() ?: error("Invalid Argon2 iterations")
            val parallelism = match.groupValues[3].toIntOrNull() ?: error("Invalid Argon2 parallelism")
            require(memory in limits.minimumMemoryKiB..limits.maximumMemoryKiB)
            require(iterations in limits.minimumIterations..limits.maximumIterations)
            require(parallelism in limits.minimumParallelism..limits.maximumParallelism)
            val salt = decode(match.groupValues[4])
            val expected = decode(match.groupValues[5])
            require(salt.size in 8..64 && expected.size in 16..64) { "Argon2 record length is unsupported" }
            return OwnerPinArgon2Hash(memory, iterations, parallelism, salt, expected)
        }

        private fun decode(value: String): ByteArray =
            runCatching { Base64.getDecoder().decode(value) }.getOrElse { error("Invalid Argon2 encoding") }
    }
}

sealed interface OwnerPinHashLoad {
    data class Loaded(val hash: OwnerPinArgon2Hash) : OwnerPinHashLoad
    data class Unavailable(val reason: String) : OwnerPinHashLoad
}

object OwnerPinHashFileLoader {
    fun load(path: Path): OwnerPinHashLoad = runCatching {
        require(Files.isRegularFile(path) && !Files.isSymbolicLink(path)) { "PIN hash file unavailable" }
        require(Files.size(path) <= 512L) { "PIN hash file too large" }
        val raw = Files.readString(path, StandardCharsets.UTF_8)
        require(raw.length <= 512) { "PIN hash file too large" }
        val record = raw.removeSuffix("\n").removeSuffix("\r")
        require(record.isNotEmpty() && !record.contains('\n') && !record.contains('\r')) { "PIN hash file must contain one record" }
        OwnerPinArgon2Hash.parse(record)
    }.fold({ OwnerPinHashLoad.Loaded(it) }, { OwnerPinHashLoad.Unavailable("PIN_HASH_UNAVAILABLE") })
}

class OwnerPinVerifier(
    private val principalId: PrincipalId,
    hashFile: Path,
    private val stateStore: FileSystemOwnerPinAttemptStateStore,
    private val audit: OwnerPinSecurityAudit = NoOpOwnerPinSecurityAudit,
    private val clock: Clock = Clock.systemUTC(),
    private val failedAttemptLockoutThreshold: Int = 5,
    private val initialBackoffMillis: Long = 50L,
    private val initialLockoutMillis: Long = 30_000L,
) {
    private val loadedHash = OwnerPinHashFileLoader.load(hashFile)

    fun verify(input: OwnerPinInput): OwnerPinVerificationResult = when (input) {
        OwnerPinInput.Invalid -> {
            audit.record(OwnerPinAuditRecord(OwnerPinAuditEvent.PIN_VERIFICATION_REJECTED, principalId, clock.instant(), "INVALID_FORMAT"))
            OwnerPinVerificationResult.REJECTED
        }
        is OwnerPinInput.Valid -> verify(input.pin)
    }

    fun verify(pin: OwnerPin): OwnerPinVerificationResult {
        val hash = (loadedHash as? OwnerPinHashLoad.Loaded)?.hash
        if (hash == null) {
            audit.record(OwnerPinAuditRecord(OwnerPinAuditEvent.PIN_VERIFIER_UNAVAILABLE, principalId, clock.instant(), "PIN_HASH_UNAVAILABLE"))
            return OwnerPinVerificationResult.UNAVAILABLE
        }
        return runCatching { stateStore.withLocked(principalId) { current ->
            val now = clock.millis()
            val lockedUntil = current.lockoutUntilEpochMillis
            if (lockedUntil != null && lockedUntil > now) {
                audit.record(OwnerPinAuditRecord(OwnerPinAuditEvent.PIN_VERIFICATION_LOCKED, principalId, Instant.ofEpochMilli(now), "TEMPORARILY_LOCKED"))
                return@withLocked current to OwnerPinVerificationResult.TEMPORARILY_LOCKED
            }
            val backoff = min(initialBackoffMillis * (1L shl min(current.failedCount, 6)), 2_000L)
            if (current.failedCount > 0) Thread.sleep(backoff)
            if (hash.verify(pin)) {
                audit.record(OwnerPinAuditRecord(OwnerPinAuditEvent.PIN_VERIFICATION_SUCCEEDED, principalId, Instant.ofEpochMilli(now), "VERIFIED"))
                return@withLocked OwnerPinAttemptState(lastOutcomeEpochMillis = now) to OwnerPinVerificationResult.VERIFIED
            }
            val failures = (current.failedCount + 1).coerceAtMost(100)
            val lock = failures >= failedAttemptLockoutThreshold
            val cycles = if (lock) (current.lockoutCycles + 1).coerceAtMost(16) else current.lockoutCycles
            val lockoutDuration = initialLockoutMillis * (1L shl min(cycles - 1, 6))
            val lockoutUntil = if (lock) safeAdd(now, lockoutDuration) else null
            val next = OwnerPinAttemptState(failures, lockoutUntil, cycles, now)
            val event = if (lock) OwnerPinAuditEvent.PIN_VERIFICATION_LOCKED else OwnerPinAuditEvent.PIN_VERIFICATION_REJECTED
            audit.record(OwnerPinAuditRecord(event, principalId, Instant.ofEpochMilli(now), if (lock) "LOCKOUT_THRESHOLD_REACHED" else "REJECTED"))
            next to if (lock) OwnerPinVerificationResult.TEMPORARILY_LOCKED else OwnerPinVerificationResult.REJECTED
        } }.getOrElse {
            audit.record(OwnerPinAuditRecord(OwnerPinAuditEvent.PIN_VERIFIER_UNAVAILABLE, principalId, clock.instant(), "PIN_STATE_UNAVAILABLE"))
            OwnerPinVerificationResult.UNAVAILABLE
        }
    }

    private fun safeAdd(left: Long, right: Long): Long =
        if (right > 0L && left > Long.MAX_VALUE - right) Long.MAX_VALUE else left + right
}
