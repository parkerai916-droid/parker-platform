package parker.composition

import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.time.Instant
import org.junit.jupiter.api.io.TempDir
import parker.core.interfaces.PrincipalId
import kotlin.test.*

class OwnerUiAuthenticationTest {
    @TempDir lateinit var root: Path
    private var now = Instant.parse("2026-09-04T12:00:00Z")
    private fun auth(lifetime: Duration = Duration.ofMinutes(30)) =
        OwnerUiAuthentication(root, PrincipalId("owner-${"a".repeat(64)}"), { now }, sessionLifetime = lifetime)

    @Test fun `pairing requires active correct unexpired challenge and is single use`() {
        val authentication = auth()
        assertNull(authentication.pair("reachable-on-lan"))
        val code = authentication.initiatePairing()
        assertNull(authentication.pair("wrong"))
        val paired = assertNotNull(authentication.pair(code))
        assertNull(authentication.pair(code))
        assertTrue(paired.deviceId.matches(Regex("owner-device-[0-9a-f]{64}")))
        val persisted = Files.readString(root.resolve("devices/${paired.deviceId}.device"))
        assertFalse(persisted.contains(paired.deviceCredential))
        assertFalse(persisted.contains(code))
        assertNotNull(authentication.authenticate(paired.sessionId))
    }

    @Test fun `expired pairing device revocation and session expiry fail closed`() {
        val authentication = auth(Duration.ofMinutes(1))
        val expiredCode = authentication.initiatePairing()
        now = now.plus(Duration.ofMinutes(5))
        assertNull(authentication.pair(expiredCode))
        val code = authentication.initiatePairing()
        val paired = assertNotNull(authentication.pair(code))
        assertNull(authentication.establishSession(paired.deviceId, "wrong"))
        val renewed = assertNotNull(authentication.establishSession(paired.deviceId, paired.deviceCredential))
        now = now.plus(Duration.ofMinutes(2))
        assertNull(authentication.authenticate(renewed))
        val active = assertNotNull(authentication.establishSession(paired.deviceId, paired.deviceCredential))
        assertTrue(authentication.revoke(paired.deviceId))
        assertNull(authentication.authenticate(active))
        assertNull(authentication.establishSession(paired.deviceId, paired.deviceCredential))
    }

    @Test fun `browser page retires reusable token and high authority secret`() {
        val source = Files.readString(Path.of("src/composition/OwnerEvidenceHttpServer.kt"))
        assertFalse(source.contains("id=\"token\""))
        assertFalse(source.contains("localStorage."))
        assertTrue(source.contains("HttpOnly; SameSite=Strict"))
        assertFalse(source.contains("OWNER_HIGH_AUTHORITY_VERIFICATION_SECRET"))
    }
}
