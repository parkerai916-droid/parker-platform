package parker.core.runtime

import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.security.MessageDigest
import java.time.Instant

/** Immutable authority for one FA-specific provider attempt; no historical Unit O authority is reused. */
data class FidelityFirstExecutionIdentity(
    val executionId: String,
    val requestId: String,
    val attemptId: String,
    val evidenceArtifactId: String,
    val sourceSha256: String,
    val sourceByteLength: Long,
    val sourceMediaType: String,
    val repositoryCommit: String,
    val provider: String,
    val model: String,
    val profileId: String,
    val instructionSha256: String,
    val schemaSha256: String,
    val processingProfile: String,
    val adapterVersion: String,
) {
    val safeExecutionId: String = FidelityFirstExecutionSafeIdentity.derive(executionId)
    val safeRequestId: String = FidelityFirstExecutionSafeIdentity.derive(requestId)
    val safeAttemptId: String = FidelityFirstExecutionSafeIdentity.derive(attemptId)

    internal fun fields() = listOf(
        "executionId" to executionId, "requestId" to requestId, "attemptId" to attemptId,
        "evidenceArtifactId" to evidenceArtifactId, "sourceSha256" to sourceSha256,
        "sourceByteLength" to sourceByteLength.toString(), "sourceMediaType" to sourceMediaType,
        "repositoryCommit" to repositoryCommit, "provider" to provider, "model" to model,
        "profileId" to profileId, "instructionSha256" to instructionSha256,
        "schemaSha256" to schemaSha256, "processingProfile" to processingProfile,
        "adapterVersion" to adapterVersion,
    )
    init {
        require(listOf(executionId, requestId, attemptId).all { GOVERNED_OPAQUE.matches(it) })
        require(sourceByteLength > 0 && sourceSha256.matches(SHA256))
        require(repositoryCommit.matches(COMMIT) && instructionSha256.matches(SHA256) && schemaSha256.matches(SHA256))
        require(fields().all { (_, value) -> value.isNotBlank() && value.length <= 1_024 && value.none { it in "\r\n\t" } })
    }
    private companion object {
        val GOVERNED_OPAQUE = Regex("^[A-Za-z0-9_.-]{1,120}$")
        val SHA256 = Regex("^[0-9a-f]{64}$")
        val COMMIT = Regex("^[0-9a-f]{40}$")
    }
}

/**
 * Derives the restricted, filesystem-safe identity used by the attempt store while leaving
 * the governed identity intact in the ledger payload and reporting surfaces.
 */
internal object FidelityFirstExecutionSafeIdentity {
    private val SAFE = Regex("^[A-Za-z0-9_-]{1,120}$")
    private const val MAX_LENGTH = 120
    private const val DIGEST_LENGTH = 64
    private const val SEPARATOR = "--"
    private const val ENCODED_PREFIX = "parker-encoded-"

    fun derive(governed: String): String {
        require(governed.isNotBlank() && governed.length <= 4_096 && governed.none { it.isISOControl() })
        if (SAFE.matches(governed) && !governed.startsWith(ENCODED_PREFIX)) return governed
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(governed.toByteArray(StandardCharsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        val prefixLimit = MAX_LENGTH - ENCODED_PREFIX.length - SEPARATOR.length - DIGEST_LENGTH
        val prefix = governed.asSequence()
            .map { if (it.isAsciiSafe()) it else '-' }
            .joinToString("")
            .replace(Regex("-+"), "-")
            .trim('-', '_')
            .take(prefixLimit)
            .ifEmpty { "id" }
        return "$ENCODED_PREFIX$prefix$SEPARATOR$digest".also { require(SAFE.matches(it)) }
    }

    private fun Char.isAsciiSafe() = this in 'A'..'Z' || this in 'a'..'z' || this in '0'..'9' || this == '_' || this == '-'
}

enum class FidelityFirstAttemptStage {
    AUTHORISED, PREFLIGHT_PASSED, SOURCE_RETRIEVED, REQUEST_PREPARED,
    PROVIDER_ATTEMPT_STARTED, PROVIDER_RESPONSE_RECEIVED, GENERATION_ADMITTED,
    TERMINAL_SUCCESS, TERMINAL_FAILURE,
}

data class FidelityFirstAttemptSnapshot(
    val identity: FidelityFirstExecutionIdentity,
    val stages: List<FidelityFirstAttemptStage>,
    val admittedGenerationId: String? = null,
) {
    val providerAttemptStarted get() = FidelityFirstAttemptStage.PROVIDER_ATTEMPT_STARTED in stages
}

/** Check-summed, locked, forced, atomic-replace ledger. Corruption and identity conflicts fail closed. */
class FileSystemFidelityFirstAttemptLedger(storageRoot: Path, private val now: () -> Instant = Instant::now) {
    private val root = storageRoot.toAbsolutePath().normalize()
    init { require(Files.isDirectory(root) && Files.isWritable(root)) }

    /** Read-only pre-creation guard; unlike [open], this never creates a lock or ledger record. */
    fun exists(identity: FidelityFirstExecutionIdentity): Boolean = Files.exists(path(identity.safeExecutionId))

    /** Reads one existing governed execution without creating either a lock or ledger file. */
    fun readExisting(executionId: String): FidelityFirstAttemptSnapshot? {
        val file = path(FidelityFirstExecutionSafeIdentity.derive(executionId))
        return if (Files.exists(file)) decode(file) else null
    }

    /** Read-only execution-bound revocation check. It never creates attempt state. */
    fun providerAttemptStartedForExecution(executionId: String): Boolean {
        val safe = FidelityFirstExecutionSafeIdentity.derive(executionId)
        if (!Files.exists(path(safe))) return false
        return locked(safe) { decode(path(safe)).providerAttemptStarted }
    }

    fun open(identity: FidelityFirstExecutionIdentity): FidelityFirstAttemptSnapshot = locked(identity.safeExecutionId) {
        val file = path(identity.safeExecutionId)
        if (!Files.exists(file)) create(file, identity)
        decode(file).also { require(it.identity == identity) { "fidelity-first attempt identity conflict" } }
    }

    fun requireAvailable(identity: FidelityFirstExecutionIdentity) = open(identity).also {
        require(!it.providerAttemptStarted) { "provider attempt already started; automatic second request prohibited" }
        require(it.stages.last() !in TERMINAL) { "fidelity-first execution is terminal" }
    }

    fun transition(
        identity: FidelityFirstExecutionIdentity,
        next: FidelityFirstAttemptStage,
        metadata: List<Pair<String, String>> = emptyList(),
    ): FidelityFirstAttemptSnapshot =
        locked(identity.safeExecutionId) {
            require(metadata.map { it.first }.distinct().size == metadata.size)
            require(metadata.all { (key, value) -> key.matches(Regex("^[a-zA-Z][a-zA-Z0-9]*$")) && value.isNotBlank() && value.length <= 1_024 && value.none { it in "\r\n\t" } })
            val file = path(identity.safeExecutionId)
            if (!Files.exists(file)) create(file, identity)
            val current = decode(file).also { require(it.identity == identity) }
            require(current.stages.last() !in TERMINAL)
            require(next == FidelityFirstAttemptStage.TERMINAL_FAILURE || next.ordinal == current.stages.last().ordinal + 1)
            replace(file, checked("STAGE", listOf("stage" to next.name, "timestamp" to now().toString()) + metadata))
            decode(file)
        }

    /** Idempotent reconstruction support before consumption; never skips or repeats attempt start. */
    fun advancePreAttempt(identity: FidelityFirstExecutionIdentity, next: FidelityFirstAttemptStage): FidelityFirstAttemptSnapshot =
        locked(identity.safeExecutionId) {
            require(next in PRE_ATTEMPT)
            val file = path(identity.safeExecutionId)
            if (!Files.exists(file)) create(file, identity)
            val current = decode(file).also { require(it.identity == identity) }
            require(!current.providerAttemptStarted && current.stages.last() !in TERMINAL)
            if (current.stages.last().ordinal >= next.ordinal) return@locked current
            require(next.ordinal == current.stages.last().ordinal + 1)
            replace(file, checked("STAGE", listOf("stage" to next.name, "timestamp" to now().toString())))
            decode(file)
        }

    private fun create(file: Path, identity: FidelityFirstExecutionIdentity) {
        val text = checked("IDENTITY", identity.fields()) + checked("STAGE", listOf(
            "stage" to FidelityFirstAttemptStage.AUTHORISED.name, "timestamp" to now().toString()))
        FileChannel.open(file, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE).use { write(it, text.toByteArray()); it.force(true) }
        forceDirectory()
    }
    private fun replace(file: Path, suffix: String) {
        val temporary = Files.createTempFile(root, ".fidelity-attempt-", ".tmp")
        try {
            FileChannel.open(temporary, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING).use {
                write(it, Files.readAllBytes(file)); write(it, suffix.toByteArray()); it.force(true)
            }
            Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
            forceDirectory()
        } finally { Files.deleteIfExists(temporary) }
    }
    private fun decode(file: Path): FidelityFirstAttemptSnapshot {
        require(Files.size(file) in 1..1_048_576)
        val text = Files.readString(file); require(text.endsWith('\n'))
        val lines = text.lineSequence().filter(String::isNotEmpty).toList(); require(lines.size >= 2)
        val identity = identity(parse(lines.first(), "IDENTITY"))
        val stageFields = lines.drop(1).map { parse(it, "STAGE") }
        val stages = stageFields.map { FidelityFirstAttemptStage.valueOf(it.getValue("stage")) }
        require(stages.first() == FidelityFirstAttemptStage.AUTHORISED)
        stages.zipWithNext().forEach { (a, b) -> require(b == FidelityFirstAttemptStage.TERMINAL_FAILURE || b.ordinal == a.ordinal + 1) }
        require(stages.count { it == FidelityFirstAttemptStage.PROVIDER_ATTEMPT_STARTED } <= 1)
        val generationId = stageFields.lastOrNull { it["stage"] == FidelityFirstAttemptStage.TERMINAL_SUCCESS.name }?.get("generationId")
        return FidelityFirstAttemptSnapshot(identity, stages, generationId)
    }
    private fun identity(f: Map<String, String>) = FidelityFirstExecutionIdentity(
        f.getValue("executionId"), f.getValue("requestId"), f.getValue("attemptId"), f.getValue("evidenceArtifactId"),
        f.getValue("sourceSha256"), f.getValue("sourceByteLength").toLong(), f.getValue("sourceMediaType"),
        f.getValue("repositoryCommit"), f.getValue("provider"), f.getValue("model"), f.getValue("profileId"),
        f.getValue("instructionSha256"), f.getValue("schemaSha256"), f.getValue("processingProfile"), f.getValue("adapterVersion"),
    ).also { require(f.keys == setOf("kind") + it.fields().map(Pair<String, String>::first)) }
    private fun checked(kind: String, fields: List<Pair<String, String>>): String {
        val payload = (listOf("kind" to kind) + fields).joinToString("\t") { "${it.first}=${it.second}" }
        return "$payload\tchecksum=${sha256(payload)}\n"
    }
    private fun parse(line: String, kind: String): Map<String, String> {
        val parts = line.split('\t'); require(parts.size >= 3)
        val payload = parts.dropLast(1).joinToString("\t")
        require(parts.last() == "checksum=${sha256(payload)}") { "fidelity-first attempt ledger checksum mismatch" }
        val pairs = parts.dropLast(1).map { val i = it.indexOf('='); require(i > 0); it.substring(0, i) to it.substring(i + 1) }
        require(pairs.map(Pair<String, String>::first).distinct().size == pairs.size)
        return pairs.toMap().also { require(it["kind"] == kind) }
    }
    private fun <T> locked(id: String, action: () -> T): T = FileChannel.open(
        root.resolve(".$id.fidelity-attempt.lock"), StandardOpenOption.CREATE, StandardOpenOption.WRITE,
    ).use { channel -> channel.lock().use { action() } }
    private fun path(id: String) = root.resolve("$id.fidelity-attempt-ledger").normalize().also { require(it.parent == root) }
    private fun forceDirectory() {
        // Windows does not expose directory handles through FileChannel; file force + atomic replace
        // is the strongest applicable primitive there. Unix filesystems additionally force the directory.
        if (System.getProperty("os.name").startsWith("Windows", ignoreCase = true)) return
        FileChannel.open(root, StandardOpenOption.READ).use { it.force(true) }
    }
    private fun write(channel: FileChannel, bytes: ByteArray) { val buffer = ByteBuffer.wrap(bytes); while (buffer.hasRemaining()) channel.write(buffer) }
    private fun sha256(value: String) = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(StandardCharsets.UTF_8)).joinToString("") { "%02x".format(it) }
    private companion object {
        val TERMINAL = setOf(FidelityFirstAttemptStage.TERMINAL_SUCCESS, FidelityFirstAttemptStage.TERMINAL_FAILURE)
        val PRE_ATTEMPT = setOf(FidelityFirstAttemptStage.PREFLIGHT_PASSED, FidelityFirstAttemptStage.SOURCE_RETRIEVED, FidelityFirstAttemptStage.REQUEST_PREPARED)
    }
}

class FidelityFirstAttemptTracker(
    private val ledger: FileSystemFidelityFirstAttemptLedger,
    val identity: FidelityFirstExecutionIdentity,
) : ExternalTranscriptionInvocationObserver, OpenAiTransportLifecycleObserver {
    fun authorised() = ledger.requireAvailable(identity)
    fun preflightPassed() = ledger.advancePreAttempt(identity, FidelityFirstAttemptStage.PREFLIGHT_PASSED)
    override fun sourceRetrieved() { ledger.advancePreAttempt(identity, FidelityFirstAttemptStage.SOURCE_RETRIEVED) }
    override fun representationBuilt() = Unit
    override fun requestPrepared() { ledger.advancePreAttempt(identity, FidelityFirstAttemptStage.REQUEST_PREPARED) }
    override fun providerAttemptStarting() { ledger.transition(identity, FidelityFirstAttemptStage.PROVIDER_ATTEMPT_STARTED) }
    override fun providerResponseReceived() { ledger.transition(identity, FidelityFirstAttemptStage.PROVIDER_RESPONSE_RECEIVED) }
    override fun generationAdmitted() { ledger.transition(identity, FidelityFirstAttemptStage.GENERATION_ADMITTED) }
    fun terminalSuccess(generationId: String? = null) = ledger.transition(
        identity, FidelityFirstAttemptStage.TERMINAL_SUCCESS,
        generationId?.let { listOf("generationId" to it) } ?: emptyList(),
    )
    fun terminalFailure() = ledger.transition(identity, FidelityFirstAttemptStage.TERMINAL_FAILURE)
    fun snapshot() = ledger.open(identity)
}
