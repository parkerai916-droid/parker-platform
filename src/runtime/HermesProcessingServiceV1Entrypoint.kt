package parker.core.runtime

import java.io.InputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.io.SequenceInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermission
import kotlinx.coroutines.runBlocking
import parker.core.interfaces.HermesProcessingServiceV1Framing
import parker.core.interfaces.HermesProcessingServiceV1Limits
import parker.core.interfaces.HermesProcessingServiceV1Request
import parker.core.interfaces.HermesV1Failure
import parker.core.interfaces.HermesV1ProcessingPrincipal

/** Fixed process exit contract for the SSH forced-command boundary. */
object HermesProcessingServiceV1ExitCodes {
    const val RESPONSE_EMITTED = 0
    const val RUNTIME_FAILURE = 70
    const val TRANSPORT_FAILURE = 75
}

/** Only values from the authenticated deployment mapping may construct this config. */
data class HermesProcessingServiceV1EntrypointConfig(
    val ledgerRoot: Path,
    val workspaceRoot: Path,
    val principal: HermesV1ProcessingPrincipal = HermesV1ProcessingPrincipal.PARKER_PROCESSING,
    val maxDiagnosticBytes: Int = 16 * 1024,
) {
    init {
        require(maxDiagnosticBytes in 256..64 * 1024) { "diagnostic bound is outside the v1 range" }
        require(ledgerRoot.isAbsolute && workspaceRoot.isAbsolute) { "service paths must be absolute" }
        require(principal == HermesV1ProcessingPrincipal.PARKER_PROCESSING) {
            "production entrypoint principal is fixed to Parker processing"
        }
    }

    fun initializeStorage() {
        prepareOwnedDirectory(ledgerRoot)
        prepareOwnedDirectory(workspaceRoot)
    }

    private fun prepareOwnedDirectory(path: Path) {
        Files.createDirectories(path)
        require(Files.isDirectory(path) && Files.isWritable(path)) { "service path is not a writable directory" }
        if (Files.getFileStore(path).supportsFileAttributeView("posix")) {
            // Tighten newly-created service directories rather than accepting
            // the process umask as the security boundary.
            Files.setPosixFilePermissions(path, setOf(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE))
            val permissions = Files.getPosixFilePermissions(path)
            require(permissions.none { it in setOf(PosixFilePermission.GROUP_READ, PosixFilePermission.GROUP_WRITE, PosixFilePermission.GROUP_EXECUTE, PosixFilePermission.OTHERS_READ, PosixFilePermission.OTHERS_WRITE, PosixFilePermission.OTHERS_EXECUTE) }) {
                "service path permissions must be owner-only"
            }
        }
    }

    companion object {
        const val KEY_LEDGER_ROOT = "HERMES_PROCESSING_V1_LEDGER_ROOT"
        const val KEY_WORKSPACE_ROOT = "HERMES_PROCESSING_V1_WORKSPACE_ROOT"

        fun fromEnvironment(environment: Map<String, String>): HermesProcessingServiceV1EntrypointConfig {
            val ledger = requiredAbsolutePath(environment, KEY_LEDGER_ROOT)
            val workspace = requiredAbsolutePath(environment, KEY_WORKSPACE_ROOT)
            return HermesProcessingServiceV1EntrypointConfig(ledger, workspace)
        }

        private fun requiredAbsolutePath(environment: Map<String, String>, key: String): Path {
            val raw = environment[key]?.takeIf { it.isNotBlank() }
                ?: throw IllegalArgumentException("missing Hermes v1 configuration: $key")
            val path = Path.of(raw).toAbsolutePath().normalize()
            require(path.root != null) { "$key must be an absolute path" }
            return path
        }
    }
}

/**
 * Host-side forced-command adapter. SSH supplies the principal by selecting
 * this fixed executable; stdin is the only request channel and no request
 * field is interpreted as a command, path, or identity.
 */
class HermesProcessingServiceV1Entrypoint(
    private val config: HermesProcessingServiceV1EntrypointConfig,
    private val service: HermesProcessingServiceV1EndToEndService,
    private val ledger: HermesProcessingServiceV1IdempotencyLedger,
) {
    fun run(input: InputStream, output: OutputStream, diagnostics: OutputStream): Int {
        val started = System.nanoTime()
        val probe = probeMetadata(input)
        return try {
            config.initializeStorage()
            val replay = probe.request?.let { request ->
                runCatching { ledger.find(config.principal, request.requestId)?.terminalResult != null }.getOrDefault(false)
            } ?: false
            val serviceInput = SequenceInputStream(ByteArrayInputStream(probe.consumedBytes), input)
            val outcome = runBlocking { service.handle(config.principal, serviceInput) }
            when (outcome) {
                is HermesV1TransportOutcome.Response -> {
                    output.write(outcome.framedBytes)
                    output.flush()
                    val status = responseStatus(outcome.framedBytes)
                    writeDiagnostic(diagnostics, "hermes_v1 requestId=${probe.request?.requestId?.value ?: "unknown"} method=${probe.request?.requestedMethods?.joinToString(",") { it.wireValue } ?: "unknown"} replay=$replay outcome=$status durationMs=${elapsedMillis(started)}")
                    HermesProcessingServiceV1ExitCodes.RESPONSE_EMITTED
                }
                is HermesV1TransportOutcome.Failed -> {
                    writeDiagnostic(diagnostics, "hermes_v1 requestId=${probe.request?.requestId?.value ?: "unknown"} method=${probe.request?.requestedMethods?.joinToString(",") { it.wireValue } ?: "unknown"} replay=$replay outcome=TRANSPORT_FAILURE category=${outcome.failure.category.wireValue} detailCode=${outcome.failure.detailCode.wireValue} durationMs=${elapsedMillis(started)}")
                    HermesProcessingServiceV1ExitCodes.TRANSPORT_FAILURE
                }
            }
        } catch (error: Throwable) {
            // The forced command must never put exception text or a stack trace
            // on stdout. Keep stderr bounded and deliberately non-sensitive.
            writeDiagnostic(diagnostics, "hermes_v1 requestId=${probe.request?.requestId?.value ?: "unknown"} method=${probe.request?.requestedMethods?.joinToString(",") { it.wireValue } ?: "unknown"} replay=false outcome=RUNTIME_FAILURE detailCode=ENTRYPOINT_RUNTIME_FAILURE durationMs=${elapsedMillis(started)}")
            HermesProcessingServiceV1ExitCodes.RUNTIME_FAILURE
        }
    }

    private data class MetadataProbe(val consumedBytes: ByteArray, val request: HermesProcessingServiceV1Request?)

    private fun probeMetadata(input: InputStream): MetadataProbe {
        val consumed = ByteArrayOutputStream()
        return try {
            val prefix = readExactly(input, 4)
            consumed.write(prefix)
            val length = ByteBuffer.wrap(prefix).order(ByteOrder.BIG_ENDIAN).int.toLong() and 0xffffffffL
            if (length <= 0L || length > HermesProcessingServiceV1Limits.MAX_METADATA_ENVELOPE_BYTES) {
                MetadataProbe(consumed.toByteArray(), null)
            } else {
                val metadata = readExactly(input, length.toInt())
                consumed.write(metadata)
                MetadataProbe(consumed.toByteArray(), HermesProcessingServiceV1Framing.decodeMetadata(metadata))
            }
        } catch (_: Exception) {
            MetadataProbe(consumed.toByteArray(), null)
        }
    }

    private fun readExactly(input: InputStream, length: Int): ByteArray {
        val bytes = ByteArray(length)
        var offset = 0
        while (offset < length) {
            val count = input.read(bytes, offset, length - offset)
            if (count < 0) break
            if (count > 0) offset += count
        }
        return bytes.copyOf(offset)
    }

    private fun responseStatus(frame: ByteArray): String {
        val body = frame.drop(4).toByteArray().toString(Charsets.UTF_8)
        return when {
            body.contains("\"status\":\"PASS\"") -> "PASS"
            body.contains("\"status\":\"REVIEW_REQUIRED\"") -> "REVIEW_REQUIRED"
            body.contains("\"status\":\"FAILED\"") -> "FAILED"
            else -> "RESPONSE_EMITTED"
        }
    }

    private fun elapsedMillis(started: Long): Long = (System.nanoTime() - started).coerceAtLeast(0L) / 1_000_000L

    private fun writeDiagnostic(output: OutputStream, value: String) {
        val bounded = value.take(config.maxDiagnosticBytes - 1) + "\n"
        output.write(bounded.toByteArray(Charsets.UTF_8))
        output.flush()
    }
}

/** Constructs the Unit 9 composition only after service-owned paths validate. */
fun buildHermesProcessingServiceV1Entrypoint(config: HermesProcessingServiceV1EntrypointConfig): HermesProcessingServiceV1Entrypoint {
    config.initializeStorage()
    val policy = parker.core.interfaces.HermesV1CapabilityPolicy.initial()
    val ledger = HermesProcessingServiceV1IdempotencyLedger(config.ledgerRoot)
    val service = HermesProcessingServiceV1EndToEndService(
        authorizationPolicy = policy,
        sourceReceiver = HermesProcessingServiceV1SourceReceiptReceiver(config.workspaceRoot),
        ledger = ledger,
    )
    return HermesProcessingServiceV1Entrypoint(config, service, ledger)
}
