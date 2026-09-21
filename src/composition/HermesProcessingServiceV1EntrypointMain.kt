package parker.composition

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlin.system.exitProcess
import parker.core.interfaces.HermesProcessingServiceV1Framing
import parker.core.interfaces.HermesProcessingServiceV1Request
import parker.core.interfaces.HermesV1BatchId
import parker.core.interfaces.HermesV1JobId
import parker.core.interfaces.HermesV1MediaType
import parker.core.interfaces.HermesV1OccurrenceId
import parker.core.interfaces.HermesV1OriginalFilename
import parker.core.interfaces.HermesV1ProtocolVersion
import parker.core.interfaces.HermesV1RequestId
import parker.core.interfaces.HermesV1Sha256
import parker.core.interfaces.HermesV1SourceReference
import parker.core.interfaces.HermesV1SourceSize
import parker.core.runtime.HermesProcessingServiceV1EntrypointConfig
import parker.core.runtime.HermesProcessingServiceV1ExitCodes
import parker.core.runtime.buildHermesProcessingServiceV1Entrypoint

/**
 * Fixed Hermes host executable. The authorized_keys entry invokes this with no
 * caller-controlled arguments. --readiness is a local operator probe only.
 */
fun main(args: Array<String>) {
    if (args.size > 1 || (args.isNotEmpty() && args[0] != "--readiness")) {
        System.err.println("Hermes v1 entrypoint rejects caller arguments")
        exitProcess(70)
    }
    val config = try {
        HermesProcessingServiceV1EntrypointConfig.fromEnvironment(System.getenv())
    } catch (_: Exception) {
        System.err.println("Hermes v1 configuration invalid")
        exitProcess(70)
    }
    val entrypoint = try {
        buildHermesProcessingServiceV1Entrypoint(config)
    } catch (_: Exception) {
        System.err.println("Hermes v1 service storage unavailable")
        exitProcess(70)
    }
    if (args.firstOrNull() == "--readiness") {
        val request = HermesProcessingServiceV1Request(
            HermesV1ProtocolVersion.CURRENT,
            HermesV1RequestId("readiness-request"),
            HermesV1JobId("readiness-job"),
            HermesV1OccurrenceId("readiness-occurrence"),
            HermesV1BatchId("readiness-batch"),
            parker.core.interfaces.HermesProcessingServiceV1Source(
                HermesV1SourceReference("readiness-source"),
                HermesV1Sha256("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"),
                HermesV1SourceSize(0),
                HermesV1OriginalFilename("readiness.txt"),
                HermesV1MediaType("text/plain"),
            ),
            emptyList(),
        )
        val requestFrame = ByteArrayOutputStream().also {
            HermesProcessingServiceV1Framing.writeRequestFrame(it, request, ByteArrayInputStream(ByteArray(0)))
        }
        val response = ByteArrayOutputStream()
        val exit = entrypoint.run(ByteArrayInputStream(requestFrame.toByteArray()), response, ByteArrayOutputStream())
        check(exit == HermesProcessingServiceV1ExitCodes.RESPONSE_EMITTED && response.size() > 4) {
            "Hermes v1 readiness protocol response failed"
        }
        println("HERMES_PROCESSING_V1_READY")
        return
    }
    exitProcess(entrypoint.run(System.`in`, System.out, System.err))
}
