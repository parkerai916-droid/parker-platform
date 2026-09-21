package parker.core.runtime

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import parker.core.interfaces.HermesProcessingServiceV1Framing
import parker.core.interfaces.HermesProcessingServiceV1Request
import parker.core.interfaces.HermesV1BatchId
import parker.core.interfaces.HermesV1JobId
import parker.core.interfaces.HermesV1MediaType
import parker.core.interfaces.HermesV1OccurrenceId
import parker.core.interfaces.HermesV1OriginalFilename
import parker.core.interfaces.HermesV1ProcessingMethod
import parker.core.interfaces.HermesV1ProtocolVersion
import parker.core.interfaces.HermesV1RequestId
import parker.core.interfaces.HermesV1Sha256
import parker.core.interfaces.HermesV1SourceReference
import parker.core.interfaces.HermesV1SourceSize

class HermesProcessingServiceV1EntrypointTest {
    @Test
    fun `malformed request has no stdout response and bounded generic diagnostics`() {
        val root = Files.createTempDirectory("hermes-v1-entrypoint")
        val entrypoint = entrypoint(root)
        val stdout = ByteArrayOutputStream()
        val stderr = ByteArrayOutputStream()
        val exit = entrypoint.run(ByteArrayInputStream(byteArrayOf(1, 2, 3)), stdout, stderr)
        assertEquals(HermesProcessingServiceV1ExitCodes.TRANSPORT_FAILURE, exit)
        assertEquals(0, stdout.size())
        assertTrue(stderr.toString(Charsets.UTF_8).contains("detailCode="))
        assertFalse(stderr.toString(Charsets.UTF_8).contains("Exception"))
        deleteTree(root)
    }

    @Test
    fun `application FAILED response is emitted with success transport exit`() {
        val root = Files.createTempDirectory("hermes-v1-entrypoint-failed")
        val entrypoint = entrypoint(root)
        val request = HermesProcessingServiceV1Request(
            HermesV1ProtocolVersion.CURRENT,
            HermesV1RequestId("request-entrypoint-1"),
            HermesV1JobId("job-entrypoint-1"),
            HermesV1OccurrenceId("occurrence-entrypoint-1"),
            HermesV1BatchId("batch-entrypoint-1"),
            parker.core.interfaces.HermesProcessingServiceV1Source(
                HermesV1SourceReference("source-entrypoint-1"),
                HermesV1Sha256("b".repeat(64)),
                HermesV1SourceSize(4),
                HermesV1OriginalFilename("fixture.txt"),
                HermesV1MediaType("text/plain"),
            ),
            listOf(HermesV1ProcessingMethod.DIRECT_TEXT_EXTRACTION),
        )
        val input = ByteArrayOutputStream().also {
            HermesProcessingServiceV1Framing.writeRequestFrame(it, request, ByteArrayInputStream("test".toByteArray()))
        }
        val stdout = ByteArrayOutputStream()
        val stderr = ByteArrayOutputStream()
        val exit = entrypoint.run(ByteArrayInputStream(input.toByteArray()), stdout, stderr)
        assertEquals(HermesProcessingServiceV1ExitCodes.RESPONSE_EMITTED, exit)
        assertTrue(stdout.size() > 4)
        assertTrue(stdout.toString(Charsets.UTF_8).contains("SOURCE_HASH_MISMATCH"))
        assertTrue(stderr.toString(Charsets.UTF_8).contains("requestId=request-entrypoint-1"))
        assertTrue(stderr.toString(Charsets.UTF_8).contains("method=DIRECT_TEXT_EXTRACTION replay=false"))
        assertTrue(stderr.toString(Charsets.UTF_8).contains("outcome=FAILED durationMs="))
        deleteTree(root)
    }

    @Test
    fun `environment config requires service-owned absolute paths`() {
        assertFailsWith<IllegalArgumentException> {
            HermesProcessingServiceV1EntrypointConfig.fromEnvironment(emptyMap())
        }
        val config = HermesProcessingServiceV1EntrypointConfig.fromEnvironment(
            mapOf(
                HermesProcessingServiceV1EntrypointConfig.KEY_LEDGER_ROOT to "/var/lib/hermes-v1/ledger",
                HermesProcessingServiceV1EntrypointConfig.KEY_WORKSPACE_ROOT to "/var/lib/hermes-v1/workspace",
            ),
        )
        assertTrue(config.ledgerRoot.isAbsolute)
        assertTrue(config.workspaceRoot.isAbsolute)
    }

    private fun entrypoint(root: java.nio.file.Path): HermesProcessingServiceV1Entrypoint {
        val ledger = HermesProcessingServiceV1IdempotencyLedger(root.resolve("ledger"))
        return HermesProcessingServiceV1Entrypoint(
            HermesProcessingServiceV1EntrypointConfig(root.resolve("ledger"), root.resolve("workspace")),
            HermesProcessingServiceV1EndToEndService(
                parker.core.interfaces.HermesV1CapabilityPolicy.initial(),
                HermesProcessingServiceV1SourceReceiptReceiver(root.resolve("workspace")),
                ledger,
            ),
            ledger,
        )
    }

    private fun deleteTree(root: java.nio.file.Path) {
        Files.walk(root).sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) }
    }
}
