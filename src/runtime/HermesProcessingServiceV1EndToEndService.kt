package parker.core.runtime

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.SequenceInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets
import java.time.Instant
import kotlinx.coroutines.runBlocking
import parker.core.interfaces.FramingException
import parker.core.interfaces.HermesProcessingFailure
import parker.core.interfaces.HermesProcessingFailureKind
import parker.core.interfaces.HermesProcessingResult
import parker.core.interfaces.HermesProcessingServiceV1Framing
import parker.core.interfaces.HermesProcessingServiceV1Limits
import parker.core.interfaces.HermesProcessingServiceV1Request
import parker.core.interfaces.HermesProcessingServiceV1Response
import parker.core.interfaces.HermesProcessingServiceV1ResponseSerializer
import parker.core.interfaces.HermesProcessingStatus
import parker.core.interfaces.HermesV1Failure
import parker.core.interfaces.HermesV1FailureCategory
import parker.core.interfaces.HermesV1FailureDetailCode
import parker.core.interfaces.HermesV1MethodProvenance
import parker.core.interfaces.HermesV1ProcessingMethod
import parker.core.interfaces.HermesV1ProcessingPrincipal
import parker.core.interfaces.HermesV1ProcessorIdentity
import parker.core.interfaces.HermesV1Provenance
import parker.core.interfaces.HermesV1RepresentationType

/**
 * In-process Hermes v1 composition used by Unit 9 and later service wiring.
 * It accepts an authenticated principal from the transport boundary and never
 * performs Parker admission or case/evidence mutation.
 */
fun interface HermesV1VerifiedProcessor {
    suspend fun process(principal: HermesV1ProcessingPrincipal, verified: HermesV1VerifiedSource): HermesV1NativeProcessingOutcome
}

class HermesProcessingServiceV1EndToEndService(
    private val authorizationPolicy: parker.core.interfaces.HermesV1CapabilityPolicy,
    private val sourceReceiver: HermesProcessingServiceV1SourceReceiptReceiver,
    private val ledger: HermesProcessingServiceV1IdempotencyLedger,
    processor: HermesV1VerifiedProcessor? = null,
) {
    private val processor = processor ?: HermesV1VerifiedProcessor { principal, verified ->
        HermesProcessingServiceV1NativeProcessorAdapter(
            parker.core.interfaces.HermesV1CapabilityAuthorizer(authorizationPolicy),
        ).process(principal, verified)
    }
    suspend fun handle(
        principal: HermesV1ProcessingPrincipal,
        input: InputStream,
    ): HermesV1TransportOutcome {
        val (request, metadataFrame) = try {
            readMetadataOnly(input)
        } catch (error: FramingException) {
            return HermesV1TransportOutcome.Failed(error.failure)
        } catch (_: RuntimeException) {
            return HermesV1TransportOutcome.Failed(serviceFailure(HermesV1FailureDetailCode.MALFORMED_JSON, false, "Hermes request envelope failed validation"))
        }

        val authorization = parker.core.interfaces.HermesV1CapabilityAuthorizer(authorizationPolicy).authorize(principal, request)
        if (authorization is parker.core.interfaces.HermesV1AuthorizationResult.Denied) {
            return HermesV1TransportOutcome.Response(frameResponse(failureResponse(request, authorization.failure)))
        }

        val received = when (val outcome = sourceReceiver.receive(SequenceInputStream(ByteArrayInputStream(metadataFrame), input))) {
            is HermesV1SourceReceiptOutcome.Failed -> return HermesV1TransportOutcome.Response(frameResponse(failureResponse(request, outcome.failure)))
            is HermesV1SourceReceiptOutcome.Verified -> outcome.source
        }
        try {
            when (val claim = ledger.claim(principal, request)) {
                is HermesV1LedgerClaim.Rejected -> return HermesV1TransportOutcome.Response(frameResponse(failureResponse(request, claim.failure)))
                is HermesV1LedgerClaim.Existing -> {
                    val existing = claim.record
                    if (existing.terminalResult != null) return HermesV1TransportOutcome.Response(HermesProcessingServiceV1Framing.frameResponseBody(existing.terminalResult.bytes()))
                    if (existing.failure != null) return HermesV1TransportOutcome.Response(frameResponse(failureResponse(request, existing.failure)))
                    return HermesV1TransportOutcome.Failed(serviceFailure(HermesV1FailureDetailCode.INVALID_STATE_TRANSITION, false, "request is already active"))
                }
                is HermesV1LedgerClaim.Created -> Unit
            }
            val verified = ledger.transition(principal, request, HermesV1LedgerState.VERIFIED)
            if (verified is HermesV1LedgerTransition.Rejected) return HermesV1TransportOutcome.Response(frameResponse(failureResponse(request, verified.failure)))
            val processing = ledger.transition(principal, request, HermesV1LedgerState.PROCESSING)
            if (processing is HermesV1LedgerTransition.Rejected) return HermesV1TransportOutcome.Response(frameResponse(failureResponse(request, processing.failure)))

            val processorOutcome = try {
                processor.process(principal, received)
            } catch (_: Exception) {
                HermesV1NativeProcessingOutcome.Rejected(failureResponse(request, serviceFailure(HermesV1FailureDetailCode.PROCESSOR_FAILED, false, "Hermes processor failed")))
            }
            val response = when (processorOutcome) {
                is HermesV1NativeProcessingOutcome.Produced -> processorOutcome.response
                is HermesV1NativeProcessingOutcome.Rejected -> processorOutcome.response
            }
            val body = HermesProcessingServiceV1ResponseSerializer.canonicalJsonUtf8(response)
            val completed = ledger.transition(principal, request, HermesV1LedgerState.COMPLETE, HermesV1LedgerResult(body))
            if (completed is HermesV1LedgerTransition.Rejected) return HermesV1TransportOutcome.Response(frameResponse(failureResponse(request, completed.failure)))
            return HermesV1TransportOutcome.Response(HermesProcessingServiceV1Framing.frameResponseBody(body))
        } catch (error: HermesV1LedgerException) {
            return HermesV1TransportOutcome.Failed(error.failure)
        } catch (error: FramingException) {
            return HermesV1TransportOutcome.Failed(error.failure)
        } finally {
            received.handle.delete()
        }
    }

    private fun readMetadataOnly(input: InputStream): Pair<HermesProcessingServiceV1Request, ByteArray> {
        val prefix = readExactly(input, 4, HermesV1FailureDetailCode.SHORT_METADATA_LENGTH_READ)
        val length = ByteBuffer.wrap(prefix).order(ByteOrder.BIG_ENDIAN).int.toLong() and 0xffffffffL
        if (length == 0L) throw FramingException(serviceFailure(HermesV1FailureDetailCode.MALFORMED_FRAME, false, "metadata length must be positive"))
        if (length > HermesProcessingServiceV1Limits.MAX_METADATA_ENVELOPE_BYTES) throw FramingException(serviceFailure(HermesV1FailureDetailCode.ENVELOPE_TOO_LARGE, false, "metadata envelope exceeds the v1 limit"))
        val metadata = readExactly(input, length.toInt(), HermesV1FailureDetailCode.SHORT_METADATA_READ)
        return HermesProcessingServiceV1Framing.decodeMetadata(metadata) to (prefix + metadata)
    }

    private fun readExactly(input: InputStream, length: Int, code: HermesV1FailureDetailCode): ByteArray {
        val bytes = ByteArray(length)
        var offset = 0
        while (offset < length) {
            val read = input.read(bytes, offset, length - offset)
            if (read < 0) throw FramingException(serviceFailure(code, code.category == HermesV1FailureCategory.TRANSPORT, "request frame ended before its declared length"))
            if (read == 0) continue
            offset += read
        }
        return bytes
    }

    private fun frameResponse(response: HermesProcessingServiceV1Response): ByteArray =
        HermesProcessingServiceV1Framing.frameResponseBody(HermesProcessingServiceV1ResponseSerializer.canonicalJsonUtf8(response))

    private fun failureResponse(request: HermesProcessingServiceV1Request, failure: HermesV1Failure): HermesProcessingServiceV1Response {
        val methods = request.requestedMethods.ifEmpty { listOf(HermesV1ProcessingMethod.DIRECT_TEXT_EXTRACTION) }
        val established = HermesProcessingResult(
            sourceSha256 = request.source.sourceSha256.value,
            batchId = request.batchId.value,
            status = HermesProcessingStatus.FAILED,
            methods = methods.map { it.establishedMethod }.toSet(),
            failure = HermesProcessingFailure(HermesProcessingFailureKind.PROCESSOR_FAILURE, failure.detail),
        )
        val now = Instant.now().let { it.minusNanos(it.nano.toLong() % 1_000_000L) }
        return HermesProcessingServiceV1Response(
            request.protocolVersion, request.requestId, request.jobId, request.occurrenceId, request.batchId, request.source.sourceSha256,
            established, emptyList(), emptyList(), failure,
            HermesV1Provenance(request.source.sourceSha256, HermesV1ProcessorIdentity("hermes-processing-service", "1"), methods.map { HermesV1MethodProvenance(it, now, now) }),
        )
    }
}

private fun serviceFailure(code: HermesV1FailureDetailCode, retryable: Boolean, detail: String): HermesV1Failure =
    HermesV1Failure(code.category, code, retryable, detail)
