package parker.composition

import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.params.Argon2Parameters
import parker.core.runtime.OwnerPin
import parker.core.runtime.OwnerPinArgon2Hash
import java.io.Console
import java.nio.charset.StandardCharsets
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.nio.file.attribute.GroupPrincipal
import java.nio.file.attribute.AclFileAttributeView
import java.nio.file.attribute.PosixFilePermission
import java.nio.file.attribute.UserPrincipal
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

private const val DEFAULT_HASH_FILE = "/mnt/parker-secrets/parker/owner-high-authority-pin.hash"
private const val DEFAULT_RECOVERY_FILE = "/mnt/parker-secrets/parker/owner-high-authority-verification.secret"
private const val ARGON_MEMORY_KIB = 32 * 1024
private const val ARGON_ITERATIONS = 3
private const val ARGON_PARALLELISM = 1
private const val SALT_BYTES = 16
private const val HASH_BYTES = 32
private const val MAX_RECOVERY_BYTES = 4096L

interface OwnerPinAdminIo {
    fun readHidden(prompt: String): CharArray
    fun println(line: String)
    fun error(line: String)
}

private class ConsoleOwnerPinAdminIo(private val console: Console) : OwnerPinAdminIo {
    override fun readHidden(prompt: String): CharArray =
        console.readPassword("$prompt ") ?: throw IllegalStateException("interactive hidden input is unavailable")
    override fun println(line: String) {
        console.writer().println(line)
        console.writer().flush()
    }
    override fun error(line: String) {
        console.writer().println(line)
        console.writer().flush()
    }
}

data class OwnerPinAdminOptions(
    val hashFile: Path = Path.of(DEFAULT_HASH_FILE),
    val recoveryFile: Path = Path.of(DEFAULT_RECOVERY_FILE),
    val requireRootForCanonicalTarget: Boolean = true,
)

enum class OwnerPinAdminOperation { SET, CHANGE, RESET, VERIFY_STATUS }

/** Host-only administrative PIN provisioning; never constructs ParkerRuntime or an HTTP path. */
class OwnerPinAdmin(
    private val options: OwnerPinAdminOptions = OwnerPinAdminOptions(),
    private val random: SecureRandom = SecureRandom(),
    private val io: OwnerPinAdminIo,
) {
    fun run(operation: OwnerPinAdminOperation): Int = try {
        when (operation) {
            OwnerPinAdminOperation.SET -> setInitial()
            OwnerPinAdminOperation.CHANGE -> changeWithRecovery()
            OwnerPinAdminOperation.RESET -> resetWithRecovery()
            OwnerPinAdminOperation.VERIFY_STATUS -> verifyStatus()
        }
        0
    } catch (e: OwnerPinAdminFailure) {
        io.error(e.message ?: "Owner PIN operation failed")
        1
    } catch (_: Exception) {
        io.error("Owner PIN operation failed safely")
        1
    }

    private fun setInitial() {
        requireAdministrativeTarget()
        require(!Files.exists(options.hashFile, LinkOption.NOFOLLOW_LINKS)) {
            "Owner PIN hash already exists; use change or reset with recovery verification"
        }
        writeNewHash(readNewPin())
        io.println("Owner PIN hash provisioned.")
    }

    private fun changeWithRecovery() {
        requireAdministrativeTarget()
        requireValidExistingHash()
        verifyRecovery(readRecovery())
        writeNewHash(readNewPin())
        io.println("Owner PIN hash replaced.")
    }

    private fun resetWithRecovery() {
        requireAdministrativeTarget()
        require(!Files.isSymbolicLink(options.hashFile)) { "hash target may not be a symlink" }
        verifyRecovery(readRecovery())
        writeNewHash(readNewPin())
        io.println("Owner PIN hash reset.")
    }

    private fun requireValidExistingHash() {
        require(!Files.isSymbolicLink(options.hashFile) &&
            Files.isRegularFile(options.hashFile, LinkOption.NOFOLLOW_LINKS)) {
            "Owner PIN hash is not provisioned; use set or reset"
        }
        val bytes = Files.readAllBytes(options.hashFile)
        require(bytes.size <= 512) { "existing Owner PIN hash is invalid" }
        val record = String(bytes, StandardCharsets.UTF_8).removeSuffix("\n").removeSuffix("\r")
        runCatching { OwnerPinArgon2Hash.parse(record) }
            .getOrElse { throw OwnerPinAdminFailure("existing Owner PIN hash is invalid; use reset") }
    }

    private fun verifyStatus() {
        val path = options.hashFile
        if (Files.isSymbolicLink(path) || !Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
            io.println("Owner PIN status: NOT_CONFIGURED")
            return
        }
        try {
            val bytes = Files.readAllBytes(path)
            require(bytes.size <= 512)
            val record = String(bytes, StandardCharsets.UTF_8).removeSuffix("\n").removeSuffix("\r")
            OwnerPinArgon2Hash.parse(record)
            io.println("Owner PIN status: CONFIGURED (Argon2id PHC valid; value redacted)")
        } catch (_: Exception) {
            io.println("Owner PIN status: INVALID_OR_UNAVAILABLE (value redacted)")
        }
    }

    private fun readNewPin(): String {
        val first = io.readHidden("Enter new six-digit Owner PIN:")
        val second = io.readHidden("Confirm new six-digit Owner PIN:")
        try {
            val a = String(first)
            val b = String(second)
            require(OwnerPin.parse(a) != null && a == b) {
                "new PIN rejected: six ASCII digits and matching confirmation required"
            }
            return a
        } finally {
            first.fill('\u0000')
            second.fill('\u0000')
        }
    }

    private fun readRecovery(): CharArray = io.readHidden("Enter existing Owner high-authority recovery credential:")

    private fun verifyRecovery(presented: CharArray) {
        try {
            require(!Files.isSymbolicLink(options.recoveryFile) &&
                Files.isRegularFile(options.recoveryFile, LinkOption.NOFOLLOW_LINKS)) {
                "recovery credential unavailable"
            }
            val expected = Files.readAllBytes(options.recoveryFile)
            require(expected.size in 32..MAX_RECOVERY_BYTES) { "recovery credential unavailable" }
            val normalized = trimSingleLineEnding(expected)
            val actual = String(presented).toByteArray(StandardCharsets.UTF_8)
            val matches = fixedWidthConstantTimeEquals(normalized, actual)
            expected.fill(0)
            normalized.fill(0)
            actual.fill(0)
            require(matches) { "recovery verification failed" }
        } finally {
            presented.fill('\u0000')
        }
    }

    private fun writeNewHash(pin: String) {
        val salt = ByteArray(SALT_BYTES)
        random.nextBytes(salt)
        val output = ByteArray(HASH_BYTES)
        val parameters = Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
            .withVersion(Argon2Parameters.ARGON2_VERSION_13)
            .withMemoryAsKB(ARGON_MEMORY_KIB)
            .withIterations(ARGON_ITERATIONS)
            .withParallelism(ARGON_PARALLELISM)
            .withSalt(salt)
            .build()
        Argon2BytesGenerator().apply { init(parameters) }.generateBytes(pin.toCharArray(), output)
        val saltText = Base64.getEncoder().withoutPadding().encodeToString(salt)
        val hashText = Base64.getEncoder().withoutPadding().encodeToString(output)
        val record = "\$argon2id\$v=19\$m=$ARGON_MEMORY_KIB,t=$ARGON_ITERATIONS,p=$ARGON_PARALLELISM\$$saltText\$$hashText\n"
        OwnerPinArgon2Hash.parse(record.removeSuffix("\n"))
        atomicReplace(record.toByteArray(StandardCharsets.UTF_8))
        output.fill(0)
        salt.fill(0)
    }

    private fun atomicReplace(bytes: ByteArray) {
        val target = options.hashFile.toAbsolutePath().normalize()
        val parent = target.parent ?: throw OwnerPinAdminFailure("hash path has no parent")
        require(!Files.isSymbolicLink(target)) { "hash target may not be a symlink" }
        require(Files.isDirectory(parent, LinkOption.NOFOLLOW_LINKS)) { "hash parent directory unavailable" }
        require(parent.toRealPath() == parent) { "hash parent path may not contain symlinks" }
        val temporary = Files.createTempFile(parent, ".owner-high-authority-pin-", ".tmp")
        try {
            Files.write(temporary, bytes, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)
            java.nio.channels.FileChannel.open(temporary, StandardOpenOption.WRITE).use { it.force(true) }
            setRestrictedPermissions(temporary)
            copyExistingAcl(target, temporary)
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
            } catch (_: AtomicMoveNotSupportedException) {
                throw OwnerPinAdminFailure("atomic replacement is unavailable on this filesystem")
            }
            // Some supported filesystems deny opening a directory as a FileChannel even though
            // the atomic same-directory rename succeeded; the file itself is already forced.
            runCatching {
                java.nio.channels.FileChannel.open(parent, StandardOpenOption.READ).use { it.force(true) }
            }
        } finally {
            Files.deleteIfExists(temporary)
        }
    }

    private fun copyExistingAcl(source: Path, target: Path) {
        if (!Files.exists(source, LinkOption.NOFOLLOW_LINKS)) return
        val sourceView = Files.getFileAttributeView(source, AclFileAttributeView::class.java, LinkOption.NOFOLLOW_LINKS)
        val targetView = Files.getFileAttributeView(target, AclFileAttributeView::class.java, LinkOption.NOFOLLOW_LINKS)
        if (sourceView != null && targetView != null) {
            targetView.setAcl(sourceView.acl)
        }
    }

    private fun setRestrictedPermissions(path: Path) {
        runCatching {
            Files.setPosixFilePermissions(path, setOf(PosixFilePermission.OWNER_READ, PosixFilePermission.GROUP_READ))
        }.getOrElse { throw OwnerPinAdminFailure("cannot set protected hash permissions") }
        if (options.requireRootForCanonicalTarget) {
            require(System.getProperty("user.name") == "root") { "run canonical operation as root/operator" }
            val view = FileSystems.getDefault().userPrincipalLookupService
            val root: UserPrincipal = view.lookupPrincipalByName("root")
            val group: GroupPrincipal = view.lookupPrincipalByGroupName("root")
            Files.setOwner(path, root)
            Files.getFileAttributeView(path, java.nio.file.attribute.PosixFileAttributeView::class.java,
                LinkOption.NOFOLLOW_LINKS)?.setGroup(group)
        }
    }

    private fun requireAdministrativeTarget() {
        if (options.requireRootForCanonicalTarget) {
            require(options.hashFile.toAbsolutePath().normalize() == Path.of(DEFAULT_HASH_FILE)) {
                "canonical target is fixed"
            }
        }
    }

    companion object {
        private fun fixedWidthConstantTimeEquals(left: ByteArray, right: ByteArray): Boolean {
            val width = MAX_RECOVERY_BYTES.toInt()
            val paddedLeft = ByteArray(width)
            val paddedRight = ByteArray(width)
            left.copyInto(paddedLeft, endIndex = minOf(left.size, width))
            right.copyInto(paddedRight, endIndex = minOf(right.size, width))
            var difference = left.size xor right.size
            for (index in 0 until width) difference = difference or (paddedLeft[index].toInt() xor paddedRight[index].toInt())
            paddedLeft.fill(0)
            paddedRight.fill(0)
            return difference == 0
        }

        private fun trimSingleLineEnding(bytes: ByteArray): ByteArray {
            var end = bytes.size
            if (end > 0 && bytes[end - 1] == '\n'.code.toByte()) end--
            if (end > 0 && bytes[end - 1] == '\r'.code.toByte()) end--
            return bytes.copyOf(end)
        }
    }
}

private class OwnerPinAdminFailure(message: String) : RuntimeException(message)

fun main(args: Array<String>) {
    val console = System.console() ?: run {
        System.err.println("Owner PIN tooling requires an interactive terminal; input will not be read from a pipe.")
        return
    }
    if (args.size != 1) {
        System.err.println("usage: owner-pin-admin.sh set|change|reset|verify-status")
        return
    }
    val operation = when (args.firstOrNull()) {
        "set" -> OwnerPinAdminOperation.SET
        "change" -> OwnerPinAdminOperation.CHANGE
        "reset" -> OwnerPinAdminOperation.RESET
        "verify-status" -> OwnerPinAdminOperation.VERIFY_STATUS
        else -> {
            System.err.println("usage: owner-pin-admin.sh set|change|reset|verify-status")
            return
        }
    }
    val exit = OwnerPinAdmin(io = ConsoleOwnerPinAdminIo(console)).run(operation)
    if (exit != 0) kotlin.system.exitProcess(exit)
}
