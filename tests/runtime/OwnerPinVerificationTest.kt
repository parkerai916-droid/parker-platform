package parker.core.runtime

import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.params.Argon2Parameters
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import parker.core.interfaces.PrincipalId
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.Base64
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OwnerPinVerificationTest {
    @TempDir lateinit var directory: Path

    @Test
    fun `valid and leading zero PINs verify without exposing value`() {
        val pin = "012345"
        val verifier = verifier(pin)
        assertTrue(verifier.verify(OwnerPinInput.parse(pin)) == OwnerPinVerificationResult.VERIFIED)
        assertEquals("OwnerPin([REDACTED])", (OwnerPin.parse(pin) ?: error("missing")).toString())
        assertFalse(Files.readString(directory.resolve("state").resolve(stateFile())).contains(pin))
    }

    @Test
    fun `malformed PIN is rejected before verification`() {
        val verifier = verifier("123456")
        listOf(null, "12345", "1234567", "12 3456", "abc123", " 123456").forEach {
            assertEquals(OwnerPinVerificationResult.REJECTED, verifier.verify(OwnerPinInput.parse(it)))
        }
    }

    @Test
    fun `wrong PIN increments state and fifth failure locks`() {
        val audit = RecordingAudit()
        val verifier = verifier("123456", audit = audit)
        repeat(4) { assertEquals(OwnerPinVerificationResult.REJECTED, verifier.verify(OwnerPinInput.parse("000000"))) }
        assertEquals(OwnerPinVerificationResult.TEMPORARILY_LOCKED, verifier.verify(OwnerPinInput.parse("000000")))
        assertTrue(audit.events.any { it.event == OwnerPinAuditEvent.PIN_VERIFICATION_LOCKED })
        assertEquals(OwnerPinVerificationResult.TEMPORARILY_LOCKED, verifier.verify(OwnerPinInput.parse("123456")))
    }

    @Test
    fun `success resets failures and expiry restores verification`() {
        val clock = MutableClock(Instant.parse("2026-01-01T00:00:00Z"))
        val verifier = verifier("123456", clock = clock, initialLockoutMillis = 1000L)
        repeat(5) { verifier.verify(OwnerPinInput.parse("000000")) }
        assertEquals(OwnerPinVerificationResult.TEMPORARILY_LOCKED, verifier.verify(OwnerPinInput.parse("123456")))
        clock.advanceMillis(1001)
        assertEquals(OwnerPinVerificationResult.VERIFIED, verifier.verify(OwnerPinInput.parse("123456")))
        repeat(4) { assertEquals(OwnerPinVerificationResult.REJECTED, verifier.verify(OwnerPinInput.parse("000000"))) }
        assertEquals(OwnerPinVerificationResult.TEMPORARILY_LOCKED, verifier.verify(OwnerPinInput.parse("000000")))
    }

    @Test
    fun `missing and malformed hash fail closed`() {
        val missing = OwnerPinVerifier(PrincipalId("owner-test"), directory.resolve("missing"), stateStore(), NoOpOwnerPinSecurityAudit)
        assertEquals(OwnerPinVerificationResult.UNAVAILABLE, missing.verify(OwnerPinInput.parse("123456")))
        val malformed = directory.resolve("bad").also { Files.writeString(it, "not-a-phc") }
        val unavailable = OwnerPinVerifier(PrincipalId("owner-test"), malformed, stateStore(), NoOpOwnerPinSecurityAudit)
        assertEquals(OwnerPinVerificationResult.UNAVAILABLE, unavailable.verify(OwnerPinInput.parse("123456")))
    }

    @Test
    fun `hash loader rejects excessive parameters fields and unsafe content`() {
        val valid = phc("123456")
        listOf(
            valid.replace("m=8192", "m=1048576"),
            valid.replace("t=1", "t=6"),
            valid.replace("p=1", "p=3"),
            valid.replace("argon2id", "argon2i"),
            valid.replace("$", "!", ignoreCase = false),
            valid + "\nsecond-record",
            valid + "\u0000",
        ).forEachIndexed { index, content ->
            val path = directory.resolve("bad-$index")
            Files.writeString(path, content)
            assertEquals(OwnerPinHashLoad.Unavailable::class, OwnerPinHashFileLoader.load(path)::class)
        }
        assertEquals(OwnerPinInput.Invalid, OwnerPinInput.parse("１２３４５６"))
    }

    @Test
    fun `hash symlink and corrupt state fail closed`() {
        val target = directory.resolve("real.hash").also { Files.writeString(it, phc("123456")) }
        val link = directory.resolve("hash-link")
        runCatching { Files.createSymbolicLink(link, target) }.getOrNull()?.let {
            val verifier = OwnerPinVerifier(PrincipalId("owner-test"), link, stateStore(), NoOpOwnerPinSecurityAudit)
            assertEquals(OwnerPinVerificationResult.UNAVAILABLE, verifier.verify(OwnerPinInput.parse("123456")))
        }
        val state = directory.resolve("state")
        Files.createDirectories(state)
        val stateName = stateFile()
        Files.writeString(state.resolve(stateName), "version=1\nfailedCount=not-a-number\n")
        val verifier = OwnerPinVerifier(PrincipalId("owner-test"), target, stateStore(), NoOpOwnerPinSecurityAudit)
        assertEquals(OwnerPinVerificationResult.UNAVAILABLE, verifier.verify(OwnerPinInput.parse("123456")))
    }

    @Test
    fun `audit log rotates and contains no submitted PIN`() {
        val log = directory.resolve("pin-audit.log")
        val audit = FileSystemOwnerPinSecurityAudit(log, maximumBytes = 4096L)
        repeat(80) {
            audit.record(OwnerPinAuditRecord(OwnerPinAuditEvent.PIN_VERIFICATION_REJECTED, PrincipalId("owner-test"), Instant.EPOCH, "REJECTED"))
        }
        assertTrue(Files.size(log) <= 4096L)
        assertTrue(Files.exists(log.resolveSibling("pin-audit.log.1")))
        assertFalse(Files.readString(log).contains("123456"))
    }

    @Test
    fun `lockout survives verifier reconstruction`() {
        val first = verifier("123456", initialLockoutMillis = 60_000L)
        repeat(5) { first.verify(OwnerPinInput.parse("000000")) }
        val second = verifier("123456", initialLockoutMillis = 60_000L)
        assertEquals(OwnerPinVerificationResult.TEMPORARILY_LOCKED, second.verify(OwnerPinInput.parse("123456")))
    }

    @Test
    fun `concurrent failures cannot lose counter updates`() {
        val verifier = verifier("123456", initialBackoffMillis = 0L)
        val pool = Executors.newFixedThreadPool(8)
        val start = CountDownLatch(1)
        val futures = (1..8).map {
            pool.submit<OwnerPinVerificationResult> { start.await(); verifier.verify(OwnerPinInput.parse("000000")) }
        }
        start.countDown()
        val results = futures.map { it.get() }
        pool.shutdown()
        assertTrue(results.any { it == OwnerPinVerificationResult.TEMPORARILY_LOCKED })
        assertTrue(Files.readString(directory.resolve("state").resolve(stateFile())).contains("failedCount=5"))
    }

    @Test
    fun `audit records contain outcomes but not PIN or hash`() {
        val audit = RecordingAudit()
        val verifier = verifier("123456", audit = audit)
        verifier.verify(OwnerPinInput.parse("000000"))
        verifier.verify(OwnerPinInput.parse("123456"))
        assertTrue(audit.events.any { it.event == OwnerPinAuditEvent.PIN_VERIFICATION_REJECTED })
        assertTrue(audit.events.any { it.event == OwnerPinAuditEvent.PIN_VERIFICATION_SUCCEEDED })
        assertTrue(audit.events.all { "123456" !in it.reason && "000000" !in it.reason })
    }

    private fun verifier(
        pin: String,
        audit: OwnerPinSecurityAudit = NoOpOwnerPinSecurityAudit,
        clock: Clock = Clock.systemUTC(),
        initialBackoffMillis: Long = 0L,
        initialLockoutMillis: Long = 30_000L,
    ): OwnerPinVerifier {
        val hash = directory.resolve("pin.hash")
        Files.writeString(hash, phc(pin))
        return OwnerPinVerifier(
            PrincipalId("owner-test"), hash, stateStore(), audit, clock,
            initialBackoffMillis = initialBackoffMillis,
            initialLockoutMillis = initialLockoutMillis,
        )
    }

    private fun stateStore() = FileSystemOwnerPinAttemptStateStore(directory.resolve("state"))

    private fun stateFile(): String = java.security.MessageDigest.getInstance("SHA-256")
        .digest("owner-test".toByteArray()).joinToString("") { "%02x".format(it.toInt() and 255) } + ".state"

    private fun phc(pin: String): String {
        val salt = "owner-pin-salt".toByteArray(StandardCharsets.UTF_8)
        val output = ByteArray(32)
        val params = Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
            .withVersion(Argon2Parameters.ARGON2_VERSION_13).withMemoryAsKB(8192)
            .withIterations(1).withParallelism(1).withSalt(salt).build()
        Argon2BytesGenerator().apply { init(params) }.generateBytes(pin.toCharArray(), output)
        val encoder = Base64.getEncoder().withoutPadding()
        return "${'$'}argon2id${'$'}v=19${'$'}m=8192,t=1,p=1${'$'}${encoder.encodeToString(salt)}${'$'}${encoder.encodeToString(output)}"
    }

    private class RecordingAudit : OwnerPinSecurityAudit {
        val events = mutableListOf<OwnerPinAuditRecord>()
        override fun record(record: OwnerPinAuditRecord) { synchronized(events) { events += record } }
    }

    private class MutableClock(private var value: Instant) : Clock() {
        override fun getZone() = ZoneOffset.UTC
        override fun withZone(zone: java.time.ZoneId) = this
        override fun instant() = value
        fun advanceMillis(millis: Long) { value = value.plusMillis(millis) }
    }
}
