package parker.composition

import kotlin.io.path.createTempDirectory
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import parker.core.runtime.OwnerPinArgon2Hash
import parker.core.runtime.OwnerPinHashFileLoader
import parker.core.runtime.OwnerPinInput
import parker.core.runtime.OwnerPinVerificationResult
import parker.core.runtime.OwnerPinVerifier
import parker.core.runtime.FileSystemOwnerPinAttemptStateStore
import parker.core.interfaces.PrincipalId

private class FakePinIo(private val hidden: ArrayDeque<CharArray>) : OwnerPinAdminIo {
    val output = mutableListOf<String>()
    val errors = mutableListOf<String>()
    override fun readHidden(prompt: String): CharArray = hidden.removeFirst().copyOf()
    override fun println(line: String) { output += line }
    override fun error(line: String) { errors += line }
}

class OwnerPinAdminToolTest {
    private class RecordingRuntimeAclProvisioner : OwnerPinRuntimeAclProvisioner {
        val sources = mutableListOf<java.nio.file.Path?>()
        val targets = mutableListOf<java.nio.file.Path>()
        override fun provision(source: java.nio.file.Path?, target: java.nio.file.Path) {
            sources.add(source); targets.add(target)
        }
    }

    @Test fun set_writes_valid_argon2id_phc_without_exposing_value() {
        val root = createTempDirectory("owner-pin-admin-")
        val hash = root.resolve("owner-high-authority-pin.hash")
        val io = FakePinIo(ArrayDeque(listOf("012345".toCharArray(), "012345".toCharArray())))
        val acl = RecordingRuntimeAclProvisioner()
        val result = OwnerPinAdmin(OwnerPinAdminOptions(hash, root.resolve("recovery"), false), io = io, runtimeAclProvisioner = acl).run(OwnerPinAdminOperation.SET)
        assertEquals(0, result)
        assertTrue(hash.exists())
        assertEquals(1, acl.targets.size)
        assertEquals(null, acl.sources.single())
        assertEquals(
            setOf(
                java.nio.file.attribute.PosixFilePermission.OWNER_READ,
                java.nio.file.attribute.PosixFilePermission.GROUP_READ,
            ),
            java.nio.file.Files.getPosixFilePermissions(hash),
        )
        val record = hash.readText().trim()
        OwnerPinArgon2Hash.parse(record)
        assertFalse(record.contains("012345"))
        assertTrue(io.output.single().contains("provisioned"))
        assertTrue(io.errors.isEmpty())
    }

    @Test fun change_requires_recovery_and_preserves_old_hash_on_failed_recovery() {
        val root = createTempDirectory("owner-pin-admin-")
        val hash = root.resolve("owner-high-authority-pin.hash")
        val recovery = root.resolve("recovery")
        recovery.writeText("recovery-secret-value-that-is-long-enough-1234567890")
        val options = OwnerPinAdminOptions(hash, recovery, false)
        val acl = RecordingRuntimeAclProvisioner()
        assertEquals(0, OwnerPinAdmin(options, io = FakePinIo(ArrayDeque(listOf("012345".toCharArray(), "012345".toCharArray()))), runtimeAclProvisioner = acl).run(OwnerPinAdminOperation.SET))
        val before = hash.readText()
        val failedIo = FakePinIo(ArrayDeque(listOf("wrong-recovery".toCharArray())))
        assertEquals(1, OwnerPinAdmin(options, io = failedIo).run(OwnerPinAdminOperation.CHANGE))
        assertEquals(before, hash.readText())
        assertTrue(failedIo.errors.single().contains("failed"))
        val changeIo = FakePinIo(ArrayDeque(listOf(
            "recovery-secret-value-that-is-long-enough-1234567890".toCharArray(),
            "654321".toCharArray(), "654321".toCharArray(),
        )))
        assertEquals(0, OwnerPinAdmin(options, io = changeIo, runtimeAclProvisioner = acl).run(OwnerPinAdminOperation.CHANGE))
        assertEquals(2, acl.targets.size)
        assertEquals(hash, acl.sources.last())
    }

    @Test fun change_with_recovery_replaces_hash_and_status_is_redacted() {
        val root = createTempDirectory("owner-pin-admin-")
        val hash = root.resolve("owner-high-authority-pin.hash")
        val recovery = root.resolve("recovery")
        recovery.writeText("recovery-secret-value-that-is-long-enough-1234567890")
        val options = OwnerPinAdminOptions(hash, recovery, false)
        assertEquals(0, OwnerPinAdmin(options, io = FakePinIo(ArrayDeque(listOf("012345".toCharArray(), "012345".toCharArray())))).run(OwnerPinAdminOperation.SET))
        val before = hash.readText()
        val io = FakePinIo(ArrayDeque(listOf("recovery-secret-value-that-is-long-enough-1234567890".toCharArray(), "654321".toCharArray(), "654321".toCharArray())))
        assertEquals(0, OwnerPinAdmin(options, io = io).run(OwnerPinAdminOperation.CHANGE))
        assertTrue(hash.readText() != before)
        val status = FakePinIo(ArrayDeque())
        assertEquals(0, OwnerPinAdmin(options, io = status).run(OwnerPinAdminOperation.VERIFY_STATUS))
        assertTrue(status.output.single().contains("CONFIGURED"))
        assertFalse(status.output.single().contains("654321"))
        assertFalse(status.output.single().contains(hash.readText().trim()))
    }

    @Test fun malformed_existing_hash_is_reported_without_exposing_contents() {
        val root = createTempDirectory("owner-pin-admin-")
        val hash = root.resolve("owner-high-authority-pin.hash")
        hash.writeText("not-a-hash")
        val io = FakePinIo(ArrayDeque())
        assertEquals(0, OwnerPinAdmin(OwnerPinAdminOptions(hash, root.resolve("recovery"), false), io = io).run(OwnerPinAdminOperation.VERIFY_STATUS))
        assertTrue(io.output.single().contains("INVALID_OR_UNAVAILABLE"))
        assertFalse(io.output.single().contains("not-a-hash"))
    }

    @Test fun reset_repairs_missing_hash_only_after_recovery() {
        val root = createTempDirectory("owner-pin-admin-")
        val hash = root.resolve("pin.hash")
        val recovery = root.resolve("recovery")
        recovery.writeText("recovery-secret-value-that-is-long-enough-1234567890")
        val acl = RecordingRuntimeAclProvisioner()
        val io = FakePinIo(ArrayDeque(listOf(
            "recovery-secret-value-that-is-long-enough-1234567890".toCharArray(),
            "012345".toCharArray(), "012345".toCharArray(),
        )))
        assertEquals(0, OwnerPinAdmin(OwnerPinAdminOptions(hash, recovery, false), io = io, runtimeAclProvisioner = acl).run(OwnerPinAdminOperation.RESET))
        assertTrue(hash.exists())
        assertEquals(1, acl.targets.size)
        assertEquals(null, acl.sources.single())
    }

    @Test fun acl_application_failure_fails_closed_before_replacing_target() {
        val root = createTempDirectory("owner-pin-admin-")
        val hash = root.resolve("owner-high-authority-pin.hash")
        val recovery = root.resolve("recovery")
        recovery.writeText("recovery-secret-value-that-is-long-enough-1234567890")
        val failingAcl = OwnerPinRuntimeAclProvisioner { _, _ -> throw IllegalStateException("synthetic ACL failure") }
        val io = FakePinIo(ArrayDeque(listOf(
            "recovery-secret-value-that-is-long-enough-1234567890".toCharArray(),
            "012345".toCharArray(), "012345".toCharArray(),
        )))
        assertEquals(1, OwnerPinAdmin(OwnerPinAdminOptions(hash, recovery, false), io = io, runtimeAclProvisioner = failingAcl).run(OwnerPinAdminOperation.RESET))
        assertFalse(hash.exists())
        assertTrue(io.errors.single().contains("failed safely") || io.errors.single().contains("Owner PIN operation failed"))
    }

    @Test fun acl_application_failure_preserves_previous_valid_hash() {
        val root = createTempDirectory("owner-pin-admin-")
        val hash = root.resolve("pin.hash")
        val recovery = root.resolve("recovery")
        recovery.writeText("recovery-secret-value-that-is-long-enough-1234567890")
        val options = OwnerPinAdminOptions(hash, recovery, false)
        assertEquals(0, OwnerPinAdmin(options, io = FakePinIo(ArrayDeque(listOf("012345".toCharArray(), "012345".toCharArray())))).run(OwnerPinAdminOperation.SET))
        val before = java.nio.file.Files.readAllBytes(hash)
        val io = FakePinIo(ArrayDeque(listOf(
            "recovery-secret-value-that-is-long-enough-1234567890".toCharArray(),
            "654321".toCharArray(), "654321".toCharArray(),
        )))
        val failingAcl = OwnerPinRuntimeAclProvisioner { _, _ -> throw IllegalStateException("synthetic ACL failure") }
        assertEquals(1, OwnerPinAdmin(options, io = io, runtimeAclProvisioner = failingAcl).run(OwnerPinAdminOperation.CHANGE))
        assertTrue(before.contentEquals(java.nio.file.Files.readAllBytes(hash)))
    }

    @Test fun generated_hash_is_accepted_by_the_real_unit_one_verifier() {
        val root = createTempDirectory("owner-pin-admin-")
        val hash = root.resolve("pin.hash")
        val io = FakePinIo(ArrayDeque(listOf("012345".toCharArray(), "012345".toCharArray())))
        assertEquals(0, OwnerPinAdmin(OwnerPinAdminOptions(hash, root.resolve("recovery"), false), io = io).run(OwnerPinAdminOperation.SET))
        val verifier = OwnerPinVerifier(
            principalId = PrincipalId("owner-pin-admin-test"),
            hashFile = hash,
            stateStore = FileSystemOwnerPinAttemptStateStore(root.resolve("attempt-state")),
        )
        assertEquals(OwnerPinVerificationResult.VERIFIED, verifier.verify(OwnerPinInput.parse("012345")))
        assertEquals(OwnerPinVerificationResult.REJECTED, verifier.verify(OwnerPinInput.parse("654321")))
    }

    @Test fun isolated_test_target_remains_outside_canonical_production_policy() {
        val root = createTempDirectory("owner-pin-admin-")
        val hash = root.resolve("pin.hash")
        val io = FakePinIo(ArrayDeque(listOf("012345".toCharArray(), "012345".toCharArray())))
        assertEquals(0, OwnerPinAdmin(OwnerPinAdminOptions(hash, root.resolve("recovery"), false), io = io).run(OwnerPinAdminOperation.SET))
        assertTrue(OwnerPinHashFileLoader.load(hash) is parker.core.runtime.OwnerPinHashLoad.Loaded)
    }
}
