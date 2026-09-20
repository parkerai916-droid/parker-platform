package parker.core.runtime

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.security.MessageDigest
import parker.core.interfaces.HermesProcessingServiceV1Framing
import parker.core.interfaces.HermesProcessingServiceV1Limits
import parker.core.interfaces.HermesProcessingServiceV1Request
import parker.core.interfaces.FramingException
import parker.core.interfaces.HermesV1Failure
import parker.core.interfaces.HermesV1FailureCategory
import parker.core.interfaces.HermesV1FailureDetailCode
import parker.core.interfaces.HermesV1Sha256

/** The result of one bounded source receipt; no Parker admission or processor result is implied. */
sealed interface HermesV1SourceReceiptOutcome {
    data class Verified(val source: HermesV1VerifiedSource) : HermesV1SourceReceiptOutcome
    data class Failed(val failure: HermesV1Failure) : HermesV1SourceReceiptOutcome
}

/**
 * A service-owned temporary source handle. Its generated path never incorporates
 * originalFilename or any other caller-controlled path component.
 */
class HermesV1SourceHandle internal constructor(internal val path: Path) {
    fun open(): InputStream = Files.newInputStream(path, StandardOpenOption.READ)

    internal fun delete() {
        Files.deleteIfExists(path)
    }
}

data class HermesV1VerifiedSource(
    val request: HermesProcessingServiceV1Request,
    val expectedSha256: HermesV1Sha256,
    val calculatedSha256: HermesV1Sha256,
    val verifiedSizeBytes: Long,
    val handle: HermesV1SourceHandle,
) {
    init {
        require(expectedSha256 == calculatedSha256) { "verified source digests must match" }
        require(verifiedSizeBytes == request.source.sizeBytes.value) { "verified source size must match metadata" }
        require(verifiedSizeBytes in 0L..HermesProcessingServiceV1Limits.MAX_SOURCE_BYTES) {
            "verified source size is outside the v1 limit"
        }
    }
}

/**
 * Unit 4 receiver. It hashes the exact bytes copied by the Unit 2 framing
 * parser, so no second complete source read is needed for verification.
 */
class HermesProcessingServiceV1SourceReceiptReceiver(workspaceRoot: Path) {
    private val workspace: Path = workspaceRoot.toAbsolutePath().normalize()

    init {
        Files.createDirectories(workspace)
        require(Files.isDirectory(workspace) && Files.isWritable(workspace)) {
            "Hermes source workspace must be a writable directory"
        }
    }

    fun receive(input: InputStream): HermesV1SourceReceiptOutcome {
        val temporary = try {
            Files.createTempFile(workspace, "hermes-source-", ".bin")
        } catch (_: IOException) {
            return failed(HermesV1FailureDetailCode.SOURCE_WRITE_FAILED, retryable = false)
        }
        require(temporary.toAbsolutePath().normalize().startsWith(workspace)) {
            "Hermes source temporary path escaped its workspace"
        }

        try {
            Files.newOutputStream(temporary, StandardOpenOption.WRITE).use { rawOutput ->
                val digest = MessageDigest.getInstance("SHA-256")
                var receivedBytes = 0L
                val output = ReceiptOutputStream(rawOutput)
                val request = try {
                    HermesProcessingServiceV1Framing.readRequestFrame(input, output) { bytes, offset, length ->
                        digest.update(bytes, offset, length)
                        receivedBytes += length.toLong()
                    }
                } catch (error: FramingException) {
                    return failAndDelete(temporary, error.failure)
                } catch (_: SourceWriteException) {
                    return failAndDelete(temporary, failure(HermesV1FailureDetailCode.SOURCE_WRITE_FAILED, retryable = false))
                } catch (_: IOException) {
                    return failAndDelete(temporary, failure(HermesV1FailureDetailCode.SOURCE_STREAM_FAILED, retryable = true))
                }

                val calculated = HermesV1Sha256(hex(digest.digest()))
                val storedBytes = try {
                    Files.size(temporary)
                } catch (_: IOException) {
                    return failAndDelete(temporary, failure(HermesV1FailureDetailCode.SOURCE_WRITE_FAILED, retryable = false))
                }
                if (receivedBytes != request.source.sizeBytes.value || storedBytes != receivedBytes) {
                    return failAndDelete(
                        temporary,
                        failure(HermesV1FailureDetailCode.SOURCE_SIZE_MISMATCH, retryable = false),
                    )
                }
                if (calculated != request.source.sourceSha256) {
                    return failAndDelete(
                        temporary,
                        failure(HermesV1FailureDetailCode.SOURCE_HASH_MISMATCH, retryable = false),
                    )
                }
                return HermesV1SourceReceiptOutcome.Verified(
                    HermesV1VerifiedSource(
                        request = request,
                        expectedSha256 = request.source.sourceSha256,
                        calculatedSha256 = calculated,
                        verifiedSizeBytes = receivedBytes,
                        handle = HermesV1SourceHandle(temporary),
                    ),
                )
            }
        } catch (_: IOException) {
            return failAndDelete(temporary, failure(HermesV1FailureDetailCode.SOURCE_WRITE_FAILED, retryable = false))
        } catch (_: RuntimeException) {
            return failAndDelete(temporary, failure(HermesV1FailureDetailCode.INTERNAL_UNEXPECTED_FAILURE, retryable = false))
        }
    }

    private fun failAndDelete(path: Path, failure: HermesV1Failure): HermesV1SourceReceiptOutcome.Failed {
        Files.deleteIfExists(path)
        return HermesV1SourceReceiptOutcome.Failed(failure)
    }

    private fun failed(code: HermesV1FailureDetailCode, retryable: Boolean): HermesV1SourceReceiptOutcome.Failed =
        HermesV1SourceReceiptOutcome.Failed(failure(code, retryable))

    private fun failure(code: HermesV1FailureDetailCode, retryable: Boolean): HermesV1Failure =
        HermesV1Failure(code.category, code, retryable, "Hermes source receipt failed")

    private class SourceWriteException(cause: IOException) : IOException(cause)

    private class ReceiptOutputStream(private val delegate: OutputStream) : OutputStream() {
        override fun write(value: Int) = try { delegate.write(value) } catch (error: IOException) { throw SourceWriteException(error) }
        override fun write(bytes: ByteArray, offset: Int, length: Int) = try {
            delegate.write(bytes, offset, length)
        } catch (error: IOException) {
            throw SourceWriteException(error)
        }
        override fun flush() = try { delegate.flush() } catch (error: IOException) { throw SourceWriteException(error) }
    }

    private companion object {
        private fun hex(bytes: ByteArray): String = buildString(bytes.size * 2) {
            bytes.forEach { byte ->
                val value = byte.toInt() and 0xff
                append("0123456789abcdef"[value ushr 4])
                append("0123456789abcdef"[value and 0x0f])
            }
        }
    }
}
