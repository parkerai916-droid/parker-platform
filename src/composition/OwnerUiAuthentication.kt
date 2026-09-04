package parker.composition

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Duration
import java.time.Instant
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap
import parker.core.interfaces.PrincipalId

data class OwnerUiPairingResult(val deviceId: String, val deviceCredential: String, val sessionId: String)

/** Filesystem-backed local pairing/device registry plus process-local short-lived sessions. */
class OwnerUiAuthentication(
    private val root: Path,
    private val ownerPrincipalId: PrincipalId,
    private val clock: () -> Instant = Instant::now,
    private val random: SecureRandom = SecureRandom(),
    private val sessionLifetime: Duration = Duration.ofHours(8),
) {
    private data class Session(val principalId: PrincipalId, val expiresAt: Instant, val deviceId: String)
    private val sessions = ConcurrentHashMap<String, Session>()
    private val devices = root.resolve("devices")
    private val challenge = root.resolve("active-pairing.challenge")

    init {
        require(ownerPrincipalId.value.matches(Regex("^owner-[0-9a-f]{64}$")))
        require(Files.isDirectory(root) && Files.isWritable(root))
        Files.createDirectories(devices)
    }

    /** Host-admin operation. The returned one-time code is deliberately never logged here. */
    @Synchronized fun initiatePairing(): String {
        val code = randomToken(24)
        val expires = clock().plus(Duration.ofMinutes(5))
        val content = expires.toString() + "\n" + sha256(code) + "\n"
        Files.writeString(challenge, content, StandardCharsets.UTF_8,
            StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)
        return code
    }

    @Synchronized fun pair(code: String): OwnerUiPairingResult? {
        if (code.isBlank() || !Files.isRegularFile(challenge)) return null
        val fields = Files.readAllLines(challenge, StandardCharsets.UTF_8)
        if (fields.size != 2 || !clock().isBefore(runCatching { Instant.parse(fields[0]) }.getOrNull() ?: return null) ||
            !constantEquals(fields[1], sha256(code))) return null
        Files.delete(challenge) // consume before issuing any durable credential
        val credential = randomToken(32)
        val deviceId = "owner-device-" + sha256(randomToken(32))
        Files.writeString(devices.resolve("$deviceId.device"),
            listOf(ownerPrincipalId.value, sha256(credential), clock().toString(), "ACTIVE").joinToString("\n", postfix = "\n"),
            StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)
        return OwnerUiPairingResult(deviceId, credential, createSession(deviceId))
    }

    fun authenticate(sessionId: String?): PrincipalId? {
        val session = sessionId?.let(sessions::get) ?: return null
        if (!clock().isBefore(session.expiresAt) || !deviceActive(session.deviceId)) {
            sessions.remove(sessionId); return null
        }
        return session.principalId
    }

    fun establishSession(deviceId: String?, credential: String?): String? {
        if (deviceId == null || credential == null) return null
        val path = devices.resolve("$deviceId.device")
        if (!deviceId.matches(Regex("^owner-device-[0-9a-f]{64}$")) || !Files.isRegularFile(path)) return null
        val fields = Files.readAllLines(path, StandardCharsets.UTF_8)
        if (fields.size != 4 || fields[0] != ownerPrincipalId.value || fields[3] != "ACTIVE" ||
            !constantEquals(fields[1], sha256(credential))) return null
        return createSession(deviceId)
    }

    fun listDeviceIds(): List<String> = Files.list(devices).use { paths -> paths.filter(Files::isRegularFile)
        .map { it.fileName.toString().removeSuffix(".device") }.sorted().toList() }

    @Synchronized fun revoke(deviceId: String): Boolean {
        val path = devices.resolve("$deviceId.device")
        if (!Files.isRegularFile(path)) return false
        val fields = Files.readAllLines(path, StandardCharsets.UTF_8)
        if (fields.size != 4) return false
        Files.writeString(path, fields.take(3).plus("REVOKED").joinToString("\n", postfix="\n"),
            StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)
        sessions.entries.removeIf { it.value.deviceId == deviceId }
        return true
    }

    fun revokeAll() { listDeviceIds().forEach(::revoke); sessions.clear() }
    fun logout(sessionId: String?) { if (sessionId != null) sessions.remove(sessionId) }
    private fun deviceActive(id: String) = runCatching { Files.readAllLines(devices.resolve("$id.device")).last() == "ACTIVE" }.getOrDefault(false)
    private fun createSession(deviceId: String): String = randomToken(32).also {
        sessions[it] = Session(ownerPrincipalId, clock().plus(sessionLifetime), deviceId)
    }
    private fun randomToken(bytes: Int) = ByteArray(bytes).also(random::nextBytes).let { Base64.getUrlEncoder().withoutPadding().encodeToString(it) }
    private fun sha256(value: String) = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(StandardCharsets.UTF_8))
        .joinToString("") { "%02x".format(it.toInt() and 255) }
    private fun constantEquals(a: String, b: String) = MessageDigest.isEqual(a.toByteArray(), b.toByteArray())
}
