package parker.core.runtime

import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.time.Instant

@JvmInline value class AcceptanceExecutionId(val value: String) {
    init { require(value.matches(Regex("^[a-z0-9_-]{1,120}$"))) }
}

data class AcceptanceExecutionIdentity(
    val executionId: AcceptanceExecutionId,
    val stage: String,
    val allocation: String,
    val requestOrdinal: Int,
    val evidenceArtifactId: String,
    val sourceSha256: String,
    val sourceByteLength: Long,
    val sourceMediaType: String,
    val repositoryCommit: String,
    val provider: String,
    val modelRule: String,
    val profileId: String,
    val instructionSha256: String,
    val schemaSha256: String,
    val processingProfile: String,
    val adapterVersion: String,
) {
    fun fields(): List<Pair<String, String>> = listOf(
        "executionId" to executionId.value, "programmeStage" to stage, "allocation" to allocation,
        "requestOrdinal" to requestOrdinal.toString(), "evidenceArtifactId" to evidenceArtifactId,
        "sourceSha256" to sourceSha256, "sourceByteLength" to sourceByteLength.toString(), "sourceMediaType" to sourceMediaType,
        "repositoryCommit" to repositoryCommit, "provider" to provider, "modelRule" to modelRule, "profileId" to profileId,
        "instructionSha256" to instructionSha256, "schemaSha256" to schemaSha256,
        "processingProfile" to processingProfile, "adapterVersion" to adapterVersion,
    )
    init {
        require(requestOrdinal > 0 && sourceByteLength > 0)
        require(sourceSha256.matches(SHA256) && instructionSha256.matches(SHA256) && schemaSha256.matches(SHA256))
        require(repositoryCommit.matches(GIT_COMMIT))
        require(fields().all { (_, value) -> value.isNotBlank() && value.length <= 1024 && '\n' !in value && '\r' !in value && '\t' !in value })
    }
    private companion object {
        val SHA256 = Regex("^[0-9a-f]{64}$")
        val GIT_COMMIT = Regex("^[0-9a-f]{40}$")
    }
}

enum class AcceptanceAttemptStage {
    AUTHORIZED, PREFLIGHT_PASSED, SOURCE_RETRIEVED, REQUEST_PREPARED,
    PROVIDER_ATTEMPT_STARTED, PROVIDER_RESPONSE_RECEIVED, GENERATION_ADMITTED,
    TERMINAL_SUCCESS, TERMINAL_FAILURE,
}

data class AcceptanceAttemptFailure(
    val category: String,
    val exceptionClass: String,
    val rootCauseClass: String,
    val transportStage: String,
) {
    init { listOf(category, exceptionClass, rootCauseClass, transportStage).forEach { require(SAFE.matches(it)) } }
    companion object {
        private val SAFE = Regex("^[A-Za-z0-9_.-]{1,128}$")
        fun bounded(category: String, error: Throwable, transportStage: String): AcceptanceAttemptFailure {
            var root = error
            repeat(16) { root = root.cause ?: return@repeat }
            fun name(value: Throwable) = value::class.java.simpleName.take(128).ifBlank { "UnknownThrowable" }
            fun safe(value: String) = value.replace(Regex("[^A-Za-z0-9_.-]"), "_").take(128).ifBlank { "UNKNOWN" }
            return AcceptanceAttemptFailure(safe(category), name(error), name(root), safe(transportStage))
        }
    }
}

data class AcceptanceAttemptEntry(
    val stage: AcceptanceAttemptStage,
    val timestamp: Instant,
    val failure: AcceptanceAttemptFailure? = null,
)
data class AcceptanceAttemptSnapshot(val identity: AcceptanceExecutionIdentity, val entries: List<AcceptanceAttemptEntry>) {
    val latest: AcceptanceAttemptStage get() = entries.last().stage
    val providerAttemptStarted get() = entries.any { it.stage == AcceptanceAttemptStage.PROVIDER_ATTEMPT_STARTED }
    val providerResponseReceived get() = entries.any { it.stage == AcceptanceAttemptStage.PROVIDER_RESPONSE_RECEIVED }
}

interface AcceptanceLedgerDurability {
    fun forceFile(channel: FileChannel) = channel.force(true)
    fun forceDirectory(directory: Path) { FileChannel.open(directory, StandardOpenOption.READ).use { it.force(true) } }
    companion object { val SYSTEM = object : AcceptanceLedgerDurability {} }
}

class FileSystemAcceptanceAttemptLedger(
    storageRoot: Path,
    private val now: () -> Instant = Instant::now,
    private val durability: AcceptanceLedgerDurability = AcceptanceLedgerDurability.SYSTEM,
) {
    private val root = storageRoot.toAbsolutePath().normalize()
    init { require(Files.isDirectory(root) && Files.isWritable(root)) { "attempt-ledger root must already exist and be writable" } }

    @Synchronized fun open(identity: AcceptanceExecutionIdentity): AcceptanceAttemptSnapshot = withExecutionLock(identity.executionId) {
        openUnlocked(identity)
    }

    private fun openUnlocked(identity: AcceptanceExecutionIdentity): AcceptanceAttemptSnapshot {
        val path = path(identity.executionId)
        if (!Files.exists(path)) create(path, identity)
        return decode(path).also { require(it.identity == identity) { "attempt-ledger identity conflict" } }
    }

    @Synchronized fun transition(identity: AcceptanceExecutionIdentity, next: AcceptanceAttemptStage, failure: AcceptanceAttemptFailure? = null): AcceptanceAttemptSnapshot = withExecutionLock(identity.executionId) {
        val current = openUnlocked(identity)
        require(current.latest !in setOf(AcceptanceAttemptStage.TERMINAL_SUCCESS, AcceptanceAttemptStage.TERMINAL_FAILURE)) { "attempt ledger is terminal" }
        val legal = next == AcceptanceAttemptStage.TERMINAL_FAILURE || next.ordinal == current.latest.ordinal + 1
        require(legal) { "illegal attempt-stage transition ${current.latest} -> $next" }
        require((next == AcceptanceAttemptStage.TERMINAL_FAILURE) == (failure != null)) { "failure facts must match terminal failure" }
        append(path(identity.executionId), encodeStage(AcceptanceAttemptEntry(next, now(), failure)))
        decode(path(identity.executionId))
    }

    fun requireExecutionAvailable(identity: AcceptanceExecutionIdentity): AcceptanceAttemptSnapshot = withExecutionLock(identity.executionId) { openUnlocked(identity) }.also {
        require(!it.providerAttemptStarted) { "provider attempt already recorded for governed execution" }
        require(it.latest !in setOf(AcceptanceAttemptStage.TERMINAL_SUCCESS, AcceptanceAttemptStage.TERMINAL_FAILURE)) { "governed execution is terminal" }
    }

    private fun create(path: Path, identity: AcceptanceExecutionIdentity) {
        val bytes = (checked("IDENTITY", identity.fields()) + checked("STAGE", stageFields(AcceptanceAttemptEntry(AcceptanceAttemptStage.AUTHORIZED, now()))) ).toByteArray(StandardCharsets.UTF_8)
        FileChannel.open(path, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE).use { channel ->
            writeFully(channel, bytes); durability.forceFile(channel)
        }
        durability.forceDirectory(root)
    }
    private fun append(path: Path, text: String) {
        val temporary = Files.createTempFile(root, ".attempt-ledger-", ".tmp")
        try {
            FileChannel.open(temporary, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING).use { channel ->
                writeFully(channel, Files.readAllBytes(path))
                writeFully(channel, text.toByteArray(StandardCharsets.UTF_8))
                durability.forceFile(channel)
            }
            Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
            durability.forceDirectory(root)
        } finally {
            Files.deleteIfExists(temporary)
        }
    }
    private fun decode(path: Path): AcceptanceAttemptSnapshot {
        require(Files.size(path) in 1..MAX_BYTES) { "attempt ledger is empty or oversized" }
        val text = Files.readString(path)
        require(text.endsWith("\n")) { "attempt ledger is truncated" }
        val lines = text.lineSequence().filter { it.isNotEmpty() }.toList()
        require(lines.size >= 2) { "attempt ledger is incomplete" }
        val identityFields = parseChecked(lines.first(), "IDENTITY")
        val identity = identity(identityFields)
        val entries = lines.drop(1).map { parseStage(parseChecked(it, "STAGE")) }
        require(entries.first().stage == AcceptanceAttemptStage.AUTHORIZED)
        entries.zipWithNext().forEach { (a, b) ->
            require(b.stage == AcceptanceAttemptStage.TERMINAL_FAILURE || b.stage.ordinal == a.stage.ordinal + 1) { "attempt ledger stages are not monotonic" }
            require(a.stage !in setOf(AcceptanceAttemptStage.TERMINAL_SUCCESS, AcceptanceAttemptStage.TERMINAL_FAILURE))
        }
        require(entries.count { it.stage == AcceptanceAttemptStage.PROVIDER_ATTEMPT_STARTED } <= 1)
        return AcceptanceAttemptSnapshot(identity, entries)
    }
    private fun checked(kind: String, fields: List<Pair<String, String>>): String {
        val payload = (listOf("kind" to kind) + fields).joinToString("\t") { "${it.first}=${it.second}" }
        return "$payload\tchecksum=${sha256(payload)}\n"
    }
    private fun parseChecked(line: String, kind: String): Map<String, String> {
        val parts = line.split('\t'); require(parts.size >= 3)
        val checksum = parts.last(); require(checksum.startsWith("checksum="))
        val payload = parts.dropLast(1).joinToString("\t")
        require(checksum.removePrefix("checksum=") == sha256(payload)) { "attempt ledger checksum mismatch" }
        val pairs = parts.dropLast(1).map { part -> val i = part.indexOf('='); require(i > 0); part.substring(0, i) to part.substring(i + 1) }
        require(pairs.map { it.first }.distinct().size == pairs.size)
        return pairs.toMap().also { require(it["kind"] == kind) }
    }
    private fun encodeStage(entry: AcceptanceAttemptEntry) = checked("STAGE", stageFields(entry))
    private fun stageFields(entry: AcceptanceAttemptEntry) = buildList {
        add("stage" to entry.stage.name); add("timestamp" to entry.timestamp.toString())
        entry.failure?.let { add("category" to it.category); add("exceptionClass" to it.exceptionClass); add("rootCauseClass" to it.rootCauseClass); add("transportStage" to it.transportStage) }
    }
    private fun parseStage(fields: Map<String, String>): AcceptanceAttemptEntry {
        val failureKeys = setOf("category", "exceptionClass", "rootCauseClass", "transportStage")
        val allowed = setOf("kind", "stage", "timestamp") + failureKeys
        require(fields.keys.all { it in allowed })
        val stage = enumValueOf<AcceptanceAttemptStage>(fields.getValue("stage"))
        val present = failureKeys.filter { it in fields }
        require(present.isEmpty() || present.size == failureKeys.size)
        val failure = if (present.isEmpty()) null else AcceptanceAttemptFailure(fields.getValue("category"), fields.getValue("exceptionClass"), fields.getValue("rootCauseClass"), fields.getValue("transportStage"))
        require((stage == AcceptanceAttemptStage.TERMINAL_FAILURE) == (failure != null))
        return AcceptanceAttemptEntry(stage, Instant.parse(fields.getValue("timestamp")), failure)
    }
    private fun identity(f: Map<String, String>) = AcceptanceExecutionIdentity(
        AcceptanceExecutionId(f.getValue("executionId")), f.getValue("programmeStage"), f.getValue("allocation"), f.getValue("requestOrdinal").toInt(),
        f.getValue("evidenceArtifactId"), f.getValue("sourceSha256"), f.getValue("sourceByteLength").toLong(), f.getValue("sourceMediaType"),
        f.getValue("repositoryCommit"), f.getValue("provider"), f.getValue("modelRule"), f.getValue("profileId"), f.getValue("instructionSha256"),
        f.getValue("schemaSha256"), f.getValue("processingProfile"), f.getValue("adapterVersion"),
    ).also { require(f.keys == setOf("kind") + it.fields().map { pair -> pair.first }) }
    private fun path(id: AcceptanceExecutionId) = root.resolve("${id.value}.attempt-ledger")
    private fun <T> withExecutionLock(id: AcceptanceExecutionId, action: () -> T): T {
        val lockPath = root.resolve(".${id.value}.attempt-ledger.lock")
        FileChannel.open(lockPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE).use { channel ->
            channel.lock().use { return action() }
        }
    }
    private fun sha256(value: String) = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(StandardCharsets.UTF_8)).joinToString("") { "%02x".format(it) }
    private fun writeFully(channel: FileChannel, bytes: ByteArray) { val buffer = ByteBuffer.wrap(bytes); while (buffer.hasRemaining()) channel.write(buffer) }
    private companion object { const val MAX_BYTES = 1024L * 1024L }
}

class UnitOAcceptanceAttemptTracker(
    private val ledger: FileSystemAcceptanceAttemptLedger,
    val identity: AcceptanceExecutionIdentity,
) : ExternalTranscriptionInvocationObserver, OpenAiTransportLifecycleObserver {
    fun authorized() = ledger.requireExecutionAvailable(identity)
    fun preflightPassed() = ledger.transition(identity, AcceptanceAttemptStage.PREFLIGHT_PASSED)
    override fun sourceRetrieved() { ledger.transition(identity, AcceptanceAttemptStage.SOURCE_RETRIEVED) }
    override fun representationBuilt() = Unit
    override fun requestPrepared() { ledger.transition(identity, AcceptanceAttemptStage.REQUEST_PREPARED) }
    override fun providerAttemptStarting() { ledger.transition(identity, AcceptanceAttemptStage.PROVIDER_ATTEMPT_STARTED) }
    override fun providerResponseReceived() { ledger.transition(identity, AcceptanceAttemptStage.PROVIDER_RESPONSE_RECEIVED) }
    override fun generationAdmitted() { ledger.transition(identity, AcceptanceAttemptStage.GENERATION_ADMITTED) }
    fun terminalSuccess() = ledger.transition(identity, AcceptanceAttemptStage.TERMINAL_SUCCESS)
    fun terminalFailure(error: Throwable, category: String = "UNEXPECTED_ACCEPTANCE_FAILURE") = runCatching {
        ledger.transition(identity, AcceptanceAttemptStage.TERMINAL_FAILURE,
            AcceptanceAttemptFailure.bounded(category, error, ledger.open(identity).latest.name))
    }
    fun snapshot() = ledger.open(identity)
}
