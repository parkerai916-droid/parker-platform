package parker.composition

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.UUID
import java.util.concurrent.Executors
import kotlinx.coroutines.runBlocking
import parker.core.interfaces.AgentGatewayAccessAudit
import parker.core.interfaces.AgentGatewayAccessAuditRecord
import parker.core.interfaces.AgentGatewayAccessOutcome
import parker.core.interfaces.CandidateEvidenceArtifact
import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.PermissionDecisionOutcome
import parker.core.interfaces.PendingReviewSourceStoreResult
import parker.core.runtime.AgentGatewayAcquisitionResult
import parker.core.runtime.AgentGatewayEvidenceManifestResult
import parker.core.runtime.AgentGatewayEvidenceRetrievalResult
import parker.core.runtime.AgentGatewaySourceSubmissionResult
import parker.core.runtime.AgentGatewayBulkBindingResult
import parker.core.runtime.SteveReviewQueueItem

/**
 * Parker Agent Gateway, AG-1E — R0 Agent Gateway Transport
 * (`docs/architecture/PARKER_AGENT_GATEWAY_SCOPE_LOCK.md` Section 18,
 * Section 20). The separate `AgentGatewayHttpServer` Section 18
 * recommends and Section 20's AG-1E bullet list requires -- reusing
 * [OwnerEvidenceHttpServer]'s own established template exactly (thin HTTP
 * dispatch, typed-id parsing rejecting malformed input before any
 * operation runs, delegation to already-governed `ParkerRuntime`/
 * coordinator methods, narrow JSON projection, no business logic of its
 * own) while remaining **structurally separate** from it:
 *
 * - Never constructs, reads, or validates an [OwnerUiAuthentication]
 *   session/pairing/device cookie -- this class has no reference to
 *   [OwnerUiAuthentication] at all, so no code path exists by which an
 *   authenticated Hermes request could ever resolve to the owner
 *   principal, and no code path exists by which an Owner UI cookie could
 *   ever be accepted here (Section 18's own "accidental Owner-authority
 *   inheritance... structurally impossible" guarantee).
 * - Binds its own address/port, entirely independent of
 *   [OwnerEvidenceHttpServer]'s.
 * - Exposes routes under `/agent/...` only -- never a permissive alias
 *   layered onto `/owner/...`.
 *
 * ## Thin adapter only
 *
 * This class holds no reference to `ParkerRuntime`, `PermissionEngine`, or
 * any coordinator/storage type. [retrieveEvidenceAsAgent]/
 * [retrieveEvidenceManifestAsAgent]/[submitSourceAsAgent]/[requestAcquisitionAsAgent] are the
 * *only* four capabilities it can ever invoke -- all supplied as individually
 * bound functions at construction (mirroring [OwnerEvidenceHttpServer]'s
 * own established "individually bound lambda parameters, never the raw
 * runtime object" construction pattern), each already performing its own
 * complete, Hermes-principal-bound [parker.core.interfaces.PermissionEngine]
 * evaluation internally (AG-1D/AG-1F/AG-1G). This class never constructs an
 * `ExecutionRequest`, never references a `PrincipalId` other than what
 * [authentication] resolves, never computes an authoritative source hash
 * itself, never invents an `EvidenceArtifactId`, and never accepts a
 * caller-supplied `PrincipalId`, `AuthorizationPurposeId`, action, resource,
 * acquisition mode, provider, or model of any kind.
 *
 * ## Routes
 *
 * - `GET /agent/evidence/{evidenceArtifactId}` → [retrieveEvidenceAsAgent]
 * - `GET /agent/evidence/{evidenceArtifactId}/manifest` → [retrieveEvidenceManifestAsAgent]
 * - `POST /agent/evidence` (AG-1F) → [submitSourceAsAgent] -- raw request body bytes as the
 *   candidate source, narrow headers for metadata (`Content-Type` → received media type,
 *   `X-Parker-Original-Filename` → original filename, `X-Parker-Advisory-Sha256` → optional
 *   advisory hash), never multipart -- one file per request, matching Hermes's own one-source-
 *   at-a-time submission shape, never the Owner UI's own multi-file convenience-upload shape.
 * - `POST /agent/evidence/{evidenceArtifactId}/acquire` (AG-1G) → [requestAcquisitionAsAgent] --
 *   no request body of any kind is read as input; `evidenceArtifactId` is the only
 *   caller-supplied fact. Fully synchronous: the response is the final governed acquisition
 *   result, never a "started"/"accepted" placeholder requiring a separate poll.
 *
 * No other method or path is recognised. No transcription, HFR, case, or deletion route exists
 * anywhere in this class, and no acquisition, provider, or egress-authorisation *logic* exists
 * here either -- [requestAcquisitionAsAgent] is a single opaque delegation, exactly like the
 * other three capabilities.
 */
class AgentGatewayHttpServer(
    private val bindAddress: String,
    private val port: Int,
    private val authentication: AgentGatewayAuthentication,
    private val ingestionPrincipalId: PrincipalId = PrincipalId("agent.hermes-ingestion-operator"),
    private val analysisPrincipalId: PrincipalId? = null,
    private val retrieveEvidenceAsAgent: suspend (EvidenceArtifactId) -> AgentGatewayEvidenceRetrievalResult,
    private val retrieveEvidenceManifestAsAgent: suspend (EvidenceArtifactId) -> AgentGatewayEvidenceManifestResult,
    private val retrieveEvidenceAsAnalysisAgent: (suspend (EvidenceArtifactId) -> AgentGatewayEvidenceRetrievalResult)? = null,
    private val retrieveEvidenceManifestAsAnalysisAgent: (suspend (EvidenceArtifactId) -> AgentGatewayEvidenceManifestResult)? = null,
    private val submitSourceAsAgent: suspend (CandidateEvidenceArtifact, String?) -> AgentGatewaySourceSubmissionResult,
    private val requestAcquisitionAsAgent: suspend (EvidenceArtifactId) -> AgentGatewayAcquisitionResult,
    private val bindIngestionEvidenceAsAgent: suspend (String, EvidenceArtifactId) -> AgentGatewayBulkBindingResult = { _, _ -> AgentGatewayBulkBindingResult.Denied },
    private val submitSourceWithBatchAsAgent: (suspend (CandidateEvidenceArtifact, String?, String?) -> AgentGatewaySourceSubmissionResult)? = null,
    private val listReadyIngestionBatchesAsAgent: suspend () -> List<parker.core.runtime.ReadyBulkIngestionBatch> = { emptyList() },
    private val steveReviewQueueProjection: parker.core.runtime.SteveReviewQueueProjection? = null,
    /** Hermes Processing Result Intake, Task 2. See [handleSubmitProcessingResult]. */
    private val submitProcessingResultAsAgent: suspend (String, parker.core.interfaces.HermesProcessingResult) -> parker.core.runtime.AgentGatewayProcessingResultSubmissionResult =
        { _, _ -> parker.core.runtime.AgentGatewayProcessingResultSubmissionResult.Denied(parker.core.interfaces.PermissionDecisionOutcome.DENIED) },
    /** Pre-ingestion REVIEW_REQUIRED source custody; never creates an EvidenceArtifactId. */
    private val submitPendingReviewSourceAsAgent: suspend (String, String, ByteArray, String?, String?) -> PendingReviewSourceStoreResult =
        { _, _, _, _, _ -> throw IllegalStateException("pending-review source custody unavailable") },
    /** Hermes Processing Result Intake, Task 2. See [handleListProcessingResults]. */
    private val listProcessingResultsForBatchAsAgent: suspend (String) -> parker.core.runtime.AgentGatewayProcessingResultListResult =
        { parker.core.runtime.AgentGatewayProcessingResultListResult.Denied(parker.core.interfaces.PermissionDecisionOutcome.DENIED) },
    /** Hermes Governed Ingestion, Task 3. See [handleSubmitGovernedIngestion]. */
        private val submitGovernedIngestionAsAgent: suspend (String, String, CandidateEvidenceArtifact) -> parker.core.runtime.AgentGatewayGovernedIngestionResult =
        { _, _, _ -> parker.core.runtime.AgentGatewayGovernedIngestionResult.Denied(parker.core.interfaces.PermissionDecisionOutcome.DENIED) },
    /** GA-4 retrieval-only text analysis request boundary. */
    private val submitAnalysisRequestAsAgent: (suspend (parker.core.interfaces.AnalysisRequest) -> parker.core.runtime.AnalysisRequestResult)? = null,
    private val audit: AgentGatewayAccessAudit,
    private val logger: ParkerLogger,
) {
    private var server: HttpServer? = null
    private var executor: java.util.concurrent.ExecutorService? = null

    /** The actual bound TCP port -- equal to [port] unless [port] was `0` (ephemeral, test-only use). */
    val boundPort: Int
        get() = server?.address?.port ?: port

    fun start() {
        val httpServer = HttpServer.create(InetSocketAddress(bindAddress, port), 0)
        val fixedThreadPool = Executors.newFixedThreadPool(4)
        httpServer.executor = fixedThreadPool
        httpServer.createContext("/agent/evidence", EvidenceHandler())
        httpServer.createContext("/agent/ingestion-batches", ReadyBatchesHandler())
        httpServer.createContext("/agent/analysis", AnalysisHandler())
        httpServer.createContext("/agent/review-queue", SteveReviewQueueHandler())
        httpServer.start()
        server = httpServer
        executor = fixedThreadPool
        logger.info("Agent Gateway HTTP server started on $bindAddress:${boundPort}")
    }

    private inner class AnalysisHandler : HttpHandler {
        override fun handle(exchange: HttpExchange) {
            val correlationId = UUID.randomUUID().toString()
            try {
                val token = bearerToken(exchange)
                val principalId = token?.let(authentication::authenticate)
                if (principalId == null) {
                    runCatching { exchange.requestBody.use { it.readBytes() } }
                    recordAudit(correlationId, null, ANALYSIS_REQUEST_ACTION_NAME, null, if (token == null) AgentGatewayAccessOutcome.UNAUTHENTICATED else AgentGatewayAccessOutcome.AUTHENTICATION_FAILED)
                    writeJson(exchange, 401, jsonObject("error" to "unauthorised")); return
                }
                if (analysisPrincipalId == null || principalId != analysisPrincipalId) {
                    runCatching { exchange.requestBody.use { it.readBytes() } }
                    recordAudit(correlationId, principalId, ANALYSIS_REQUEST_ACTION_NAME, null, AgentGatewayAccessOutcome.DENIED)
                    writeJson(exchange, 403, jsonObject("error" to "denied")); return
                }
                if (exchange.requestMethod != "POST" || exchange.requestURI.path != "/agent/analysis") {
                    runCatching { exchange.requestBody.use { it.readBytes() } }
                    recordAudit(correlationId, principalId, ANALYSIS_REQUEST_ACTION_NAME, null, AgentGatewayAccessOutcome.NOT_FOUND_ROUTE)
                    writeJson(exchange, 404, jsonObject("error" to "not found")); return
                }
                val request = try {
                    parseAnalysisRequest(readBounded(exchange.requestBody, MAX_ANALYSIS_REQUEST_BODY_BYTES))
                } catch (_: RequestBodyTooLargeException) {
                    recordAudit(correlationId, principalId, ANALYSIS_REQUEST_ACTION_NAME, null, AgentGatewayAccessOutcome.INVALID_SOURCE)
                    writeJson(exchange, 413, jsonObject("error" to "request body too large")); return
                } catch (e: Exception) {
                    recordAudit(correlationId, principalId, ANALYSIS_REQUEST_ACTION_NAME, null, AgentGatewayAccessOutcome.INVALID_SOURCE)
                    writeJson(exchange, 400, jsonObject("error" to "malformed analysis request", "detail" to (e.message ?: "invalid request"))); return
                }
                val submit = submitAnalysisRequestAsAgent
                if (submit == null) {
                    recordAudit(correlationId, principalId, ANALYSIS_REQUEST_ACTION_NAME, request.requestId.value, AgentGatewayAccessOutcome.DENIED)
                    writeJson(exchange, 503, jsonObject("error" to "analysis boundary unavailable")); return
                }
                when (val result = runBlocking { submit(request) }) {
                    is parker.core.runtime.AnalysisRequestResult.Accepted -> {
                        recordAudit(correlationId, principalId, ANALYSIS_REQUEST_ACTION_NAME, request.requestId.value, AgentGatewayAccessOutcome.APPROVED)
                        writeJson(exchange, 200, analysisPackageJson(result.retrievalPackage))
                    }
                    is parker.core.runtime.AnalysisRequestResult.ScopeRejected -> {
                        recordAudit(correlationId, principalId, ANALYSIS_REQUEST_ACTION_NAME, request.requestId.value, AgentGatewayAccessOutcome.NOT_FOUND)
                        writeJson(exchange, 404, jsonObject("status" to "SCOPE_REJECTED", "requestId" to request.requestId.value, "reason" to result.reason, "evidenceArtifactId" to result.evidenceArtifactId?.value))
                    }
                    is parker.core.runtime.AnalysisRequestResult.Denied -> {
                        recordAudit(correlationId, principalId, ANALYSIS_REQUEST_ACTION_NAME, request.requestId.value, AgentGatewayAccessOutcome.DENIED)
                        writeJson(exchange, 403, jsonObject("status" to "DENIED", "requestId" to request.requestId.value, "reason" to result.reason))
                    }
                }
            } catch (e: Exception) {
                logger.error("Agent Gateway HTTP: analysis request failed safely (correlationId=$correlationId)", e)
                runCatching { writeJson(exchange, 500, jsonObject("error" to "internal error")) }
            } finally { exchange.close() }
        }
    }

    private inner class SteveReviewQueueHandler : HttpHandler {
        override fun handle(exchange: HttpExchange) {
            val correlationId = UUID.randomUUID().toString()
            try {
                val token = bearerToken(exchange)
                val principalId = token?.let(authentication::authenticate)
                if (principalId == null) {
                    recordAudit(correlationId, null, null, null, if (token == null) AgentGatewayAccessOutcome.UNAUTHENTICATED else AgentGatewayAccessOutcome.AUTHENTICATION_FAILED)
                    writeJson(exchange, 401, jsonObject("error" to "unauthorised")); return
                }
                if (principalId != ingestionPrincipalId) {
                    recordAudit(correlationId, principalId, null, null, AgentGatewayAccessOutcome.DENIED)
                    writeJson(exchange, 403, jsonObject("error" to "denied")); return
                }
                if (exchange.requestMethod != "GET" || exchange.requestURI.path != "/agent/review-queue") {
                    writeJson(exchange, 404, jsonObject("error" to "not found")); return
                }
                val items = runBlocking { steveReviewQueueProjection?.enumerate() ?: emptyList() }
                recordAudit(correlationId, principalId, "agent.review-queue.enumerate", null, AgentGatewayAccessOutcome.APPROVED)
                writeJson(exchange, 200, jsonObject("items" to JsonArray(items.map(::queueItemJson))))
            } catch (e: Exception) {
                logger.error("Agent Gateway HTTP: review queue enumeration failed safely", e)
                runCatching { writeJson(exchange, 500, jsonObject("error" to "internal error")) }
            } finally { exchange.close() }
        }
    }

    private fun queueItemJson(item: SteveReviewQueueItem): JsonObject = jsonObject(
        "status" to item.status.name,
        "failureReason" to item.failureReason,
        "evidenceArtifactId" to item.evidence.evidenceArtifactId.value,
        "sourceSha256" to item.evidence.sha256,
        "caseId" to item.caseId?.value,
        "batchIds" to JsonArray(item.batchIds),
        "derivativeGenerationId" to item.generation.derivativeGenerationId.value,
        "derivativeReviewState" to item.derivativeReviewState?.name,
        "humanFidelityState" to (item.humanFidelity as? parker.core.interfaces.EffectiveHumanFidelityReviewProjectionOutcome.Projected)?.summary?.projection?.effectiveState?.name,
        "sourceConfirmedEligibility" to item.sourceConfirmedEligibility.state.name,
        "denialReason" to item.sourceConfirmedEligibility.denialReason?.name,
        "discrepancyIds" to JsonArray(item.discrepancies.map { it.discrepancyId.value }),
        "discrepancies" to JsonArray(item.discrepancies.map { d -> jsonObject(
            "discrepancyId" to d.discrepancyId.value,
            "pageNumber" to d.location.pageNumber,
            "preparationRegionId" to d.location.preparationRegionId.value,
            "derivativeRegionId" to d.location.derivativeRegionId.value,
            "transcriptionBlockIndex" to d.location.transcriptionBlockIndex,
            "reason" to d.reason,
            "classification" to d.classification.name,
            "severity" to d.severity.name,
        ) }),
        "uncertaintySpans" to JsonArray(item.uncertainty.map { u -> jsonObject(
            "pageNumber" to u.pageNumber, "startOffsetInclusive" to u.startOffsetInclusive,
            "endOffsetExclusive" to u.endOffsetExclusive, "kind" to u.kind.name, "disclosure" to u.disclosure,
        ) }),
        "correctionRepresentations" to JsonArray(item.corrections.map { c -> jsonObject(
            "derivativeGenerationId" to c.derivativeGenerationId.value,
            "reviewId" to c.reviewId.value,
            "proposalIds" to JsonArray(c.proposals.map { it.proposalId.value }),
            "acceptanceId" to c.acceptance.acceptanceId.value,
            "acceptingPrincipalId" to c.acceptance.acceptingPrincipalId.value,
            "published" to true,
        ) }),
    )

    /**
     * Hermes Processing Result Intake, Task 2 note: this handler now also serves
     * `POST`/`GET /agent/ingestion-batches/{batchId}/processing-results` -- both necessarily route
     * here rather than to a second registered context, since `com.sun.net.httpserver.HttpServer`
     * dispatches by longest-registered-prefix match and `/agent/ingestion-batches` is the only
     * context registered for this whole subtree (mirroring [EvidenceHandler]'s own established
     * "one context, method-and-path-segment dispatch inside `handle`" shape for `/agent/evidence`).
     */
    private inner class ReadyBatchesHandler : HttpHandler {
        override fun handle(exchange: HttpExchange) {
            val correlationId = UUID.randomUUID().toString()
            try {
                val token = bearerToken(exchange)
                val principalId = token?.let(authentication::authenticate)
                if (principalId == null) {
                    runCatching { exchange.requestBody.use { it.readBytes() } }
                    recordAudit(correlationId, null, null, null, if (token == null) AgentGatewayAccessOutcome.UNAUTHENTICATED else AgentGatewayAccessOutcome.AUTHENTICATION_FAILED)
                    writeJson(exchange, 401, jsonObject("error" to "unauthorised")); return
                }
                if (principalId != ingestionPrincipalId) {
                    runCatching { exchange.requestBody.use { it.readBytes() } }
                    recordAudit(correlationId, principalId, null, null, AgentGatewayAccessOutcome.DENIED)
                    writeJson(exchange, 403, jsonObject("error" to "denied")); return
                }
                if (exchange.requestMethod == "GET" && exchange.requestURI.path == "/agent/ingestion-batches") {
                    val batches = runBlocking { listReadyIngestionBatchesAsAgent() }
                    recordAudit(correlationId, principalId, "agent.ingestion-batches.ready", null, AgentGatewayAccessOutcome.APPROVED)
                    writeJson(exchange, 200, jsonObject("batches" to JsonArray(batches.map { jsonObject("batchId" to it.batchId, "caseName" to it.caseName, "status" to "READY") })))
                    return
                }
                val segments = exchange.requestURI.path.removePrefix("/agent/ingestion-batches/").split('/').filter { it.isNotEmpty() }
                if (segments.size == 2 && segments[1] == "processing-results") {
                    when (exchange.requestMethod) {
                        "POST" -> handleSubmitProcessingResult(exchange, correlationId, principalId, segments[0])
                        "GET" -> handleListProcessingResults(exchange, correlationId, principalId, segments[0])
                        else -> {
                            runCatching { exchange.requestBody.use { it.readBytes() } }
                            recordAudit(correlationId, principalId, null, null, AgentGatewayAccessOutcome.NOT_FOUND_ROUTE)
                            writeJson(exchange, 404, jsonObject("error" to "not found"))
                        }
                    }
                    return
                }
                if (segments.size == 3 && segments[1] == "sources" && exchange.requestMethod == "POST") {
                    handleSubmitGovernedIngestion(exchange, correlationId, principalId, segments[0], segments[2])
                    return
                }
                if (segments.size == 3 && segments[1] == "pending-review-sources" && exchange.requestMethod == "POST") {
                    handleSubmitPendingReviewSource(exchange, correlationId, principalId, segments[0], segments[2])
                    return
                }
                runCatching { exchange.requestBody.use { it.readBytes() } }
                recordAudit(correlationId, principalId, null, null, AgentGatewayAccessOutcome.NOT_FOUND_ROUTE)
                writeJson(exchange, 404, jsonObject("error" to "not found"))
            } catch (e: Exception) {
                logger.error("Agent Gateway HTTP: ready batch / processing-result request failed safely (correlationId=$correlationId)", e)
                runCatching { recordAudit(correlationId, null, null, null, AgentGatewayAccessOutcome.INTERNAL_FAILURE) }
                runCatching { writeJson(exchange, 500, jsonObject("error" to "internal error")) }
            } finally { exchange.close() }
        }

        private fun handleSubmitPendingReviewSource(exchange: HttpExchange, correlationId: String, principalId: PrincipalId, batchId: String, sourceSha256: String) {
            if (!SAFE_ROUTE_ID.matches(batchId) || !SHA256_PATTERN.matches(sourceSha256)) {
                runCatching { exchange.requestBody.use { it.readBytes() } }
                recordAudit(correlationId, principalId, PENDING_REVIEW_SOURCE_SUBMIT_ACTION_NAME, batchId, AgentGatewayAccessOutcome.INVALID_SOURCE)
                writeJson(exchange, 400, jsonObject("error" to "invalid pending-review source identity")); return
            }
            val body = try { readBounded(exchange.requestBody, MAX_SUBMISSION_BYTES) } catch (_: RequestBodyTooLargeException) {
                recordAudit(correlationId, principalId, PENDING_REVIEW_SOURCE_SUBMIT_ACTION_NAME, batchId, AgentGatewayAccessOutcome.INVALID_SOURCE)
                writeJson(exchange, 413, jsonObject("error" to "request body too large")); return
            }
            val mediaType = exchange.requestHeaders.getFirst("Content-Type")?.takeIf { it.isNotBlank() }
            val displayName = exchange.requestHeaders.getFirst(ORIGINAL_FILENAME_HEADER)?.takeIf { it.isNotBlank() }
            try {
                when (runBlocking { submitPendingReviewSourceAsAgent(batchId, sourceSha256, body, mediaType, displayName) }) {
                    is PendingReviewSourceStoreResult.Stored -> {
                        recordAudit(correlationId, principalId, PENDING_REVIEW_SOURCE_SUBMIT_ACTION_NAME, batchId, AgentGatewayAccessOutcome.REGISTERED)
                        writeJson(exchange, 201, jsonObject("status" to "STORED", "batchId" to batchId, "sourceSha256" to sourceSha256, "byteLength" to body.size))
                    }
                    is PendingReviewSourceStoreResult.AlreadyStored -> {
                        recordAudit(correlationId, principalId, PENDING_REVIEW_SOURCE_SUBMIT_ACTION_NAME, batchId, AgentGatewayAccessOutcome.ALREADY_REGISTERED)
                        writeJson(exchange, 200, jsonObject("status" to "ALREADY_STORED", "batchId" to batchId, "sourceSha256" to sourceSha256, "byteLength" to body.size))
                    }
                    is PendingReviewSourceStoreResult.Conflict -> {
                        recordAudit(correlationId, principalId, PENDING_REVIEW_SOURCE_SUBMIT_ACTION_NAME, batchId, AgentGatewayAccessOutcome.SOURCE_IDENTITY_CONFLICT)
                        writeJson(exchange, 409, jsonObject("status" to "CONFLICT"))
                    }
                }
            } catch (_: IllegalArgumentException) {
                recordAudit(correlationId, principalId, PENDING_REVIEW_SOURCE_SUBMIT_ACTION_NAME, batchId, AgentGatewayAccessOutcome.INVALID_SOURCE)
                writeJson(exchange, 400, jsonObject("error" to "pending-review source hash mismatch or invalid metadata"))
            }
        }

        /**
         * Hermes Processing Result Intake, Task 2. Batch-scoped, never evidence-ID-scoped -- the
         * source may not yet have an authoritative [EvidenceArtifactId] at all when Hermes reports
         * its result. [batchId] comes only from the route, never redundantly re-asserted in the
         * body -- [parseHermesProcessingResultRequest] rejects a request body naming a `batchId`
         * (or a `caseId`) field at all, so there is no field anywhere through which a caller could
         * assert or alter case/batch binding.
         */
        private fun handleSubmitProcessingResult(exchange: HttpExchange, correlationId: String, principalId: PrincipalId, batchId: String) {
            val body = try {
                readBounded(exchange.requestBody, MAX_PROCESSING_RESULT_REQUEST_BODY_BYTES)
            } catch (_: RequestBodyTooLargeException) {
                recordAudit(correlationId, principalId, PROCESSING_RESULT_SUBMIT_ACTION_NAME, batchId, AgentGatewayAccessOutcome.INVALID_SOURCE)
                writeJson(exchange, 413, jsonObject("error" to "request body too large"))
                return
            }
            val result = try {
                parseHermesProcessingResultRequest(body, batchId)
            } catch (e: Exception) {
                recordAudit(correlationId, principalId, PROCESSING_RESULT_SUBMIT_ACTION_NAME, batchId, AgentGatewayAccessOutcome.INVALID_SOURCE)
                writeJson(exchange, 400, jsonObject("error" to "malformed processing result", "detail" to (e.message ?: "invalid request")))
                return
            }
            when (val outcome = runBlocking { submitProcessingResultAsAgent(batchId, result) }) {
                is parker.core.runtime.AgentGatewayProcessingResultSubmissionResult.Recorded -> {
                    recordAudit(correlationId, principalId, PROCESSING_RESULT_SUBMIT_ACTION_NAME, batchId, AgentGatewayAccessOutcome.REGISTERED)
                    writeJson(exchange, 201, jsonObject("status" to "RECORDED", "result" to hermesProcessingResultJson(outcome.result)))
                }
                is parker.core.runtime.AgentGatewayProcessingResultSubmissionResult.AlreadyRecorded -> {
                    recordAudit(correlationId, principalId, PROCESSING_RESULT_SUBMIT_ACTION_NAME, batchId, AgentGatewayAccessOutcome.ALREADY_REGISTERED)
                    writeJson(exchange, 200, jsonObject("status" to "ALREADY_RECORDED", "result" to hermesProcessingResultJson(outcome.result)))
                }
                is parker.core.runtime.AgentGatewayProcessingResultSubmissionResult.Conflict -> {
                    recordAudit(correlationId, principalId, PROCESSING_RESULT_SUBMIT_ACTION_NAME, batchId, AgentGatewayAccessOutcome.SOURCE_IDENTITY_CONFLICT)
                    writeJson(exchange, 409, jsonObject("status" to "CONFLICT", "existing" to hermesProcessingResultJson(outcome.existing)))
                }
                parker.core.runtime.AgentGatewayProcessingResultSubmissionResult.UnknownBatch -> {
                    recordAudit(correlationId, principalId, PROCESSING_RESULT_SUBMIT_ACTION_NAME, batchId, AgentGatewayAccessOutcome.NOT_FOUND)
                    writeJson(exchange, 404, jsonObject("error" to "unknown batch"))
                }
                is parker.core.runtime.AgentGatewayProcessingResultSubmissionResult.Denied -> {
                    recordAudit(correlationId, principalId, PROCESSING_RESULT_SUBMIT_ACTION_NAME, batchId, AgentGatewayAccessOutcome.DENIED)
                    writeJson(exchange, 403, jsonObject("error" to "denied"))
                }
            }
        }

        /**
         * Hermes Processing Result Intake, Task 2. The narrow, authorised read-back path -- only
         * results already recorded for [batchId], never a cross-batch or broader search.
         */
        private fun handleListProcessingResults(exchange: HttpExchange, correlationId: String, principalId: PrincipalId, batchId: String) {
            runCatching { exchange.requestBody.use { it.readBytes() } }
            when (val outcome = runBlocking { listProcessingResultsForBatchAsAgent(batchId) }) {
                is parker.core.runtime.AgentGatewayProcessingResultListResult.Found -> {
                    recordAudit(correlationId, principalId, PROCESSING_RESULT_LIST_ACTION_NAME, batchId, AgentGatewayAccessOutcome.APPROVED)
                    writeJson(exchange, 200, jsonObject("results" to JsonArray(outcome.results.map(::hermesProcessingResultJson))))
                }
                parker.core.runtime.AgentGatewayProcessingResultListResult.UnknownBatch -> {
                    recordAudit(correlationId, principalId, PROCESSING_RESULT_LIST_ACTION_NAME, batchId, AgentGatewayAccessOutcome.NOT_FOUND)
                    writeJson(exchange, 404, jsonObject("error" to "unknown batch"))
                }
                is parker.core.runtime.AgentGatewayProcessingResultListResult.Denied -> {
                    recordAudit(correlationId, principalId, PROCESSING_RESULT_LIST_ACTION_NAME, batchId, AgentGatewayAccessOutcome.DENIED)
                    writeJson(exchange, 403, jsonObject("error" to "denied"))
                }
            }
        }

        /**
         * Hermes Governed Ingestion, Task 3. [expectedSha256] comes from the route -- the caller's
         * own declared identity for the source it is submitting, established earlier by its own
         * stored [parker.core.interfaces.HermesProcessingResult]. Raw request body bytes are the
         * candidate source, exactly mirroring [handleSubmit]'s own `POST /agent/evidence` shape
         * (`Content-Type`/`X-Parker-Original-Filename` headers, no advisory-hash header needed here
         * since the route itself already names the expected hash).
         */
        private fun handleSubmitGovernedIngestion(exchange: HttpExchange, correlationId: String, principalId: PrincipalId, batchId: String, expectedSha256: String) {
            if (!SHA256_PATTERN.matches(expectedSha256)) {
                runCatching { exchange.requestBody.use { it.readBytes() } }
                recordAudit(correlationId, principalId, GOVERNED_INGESTION_ACTION_NAME, batchId, AgentGatewayAccessOutcome.INVALID_SOURCE)
                writeJson(exchange, 400, jsonObject("error" to "invalid source sha256")); return
            }
            val content = try {
                readBounded(exchange.requestBody, MAX_SUBMISSION_BYTES)
            } catch (_: RequestBodyTooLargeException) {
                recordAudit(correlationId, principalId, GOVERNED_INGESTION_ACTION_NAME, batchId, AgentGatewayAccessOutcome.INVALID_SOURCE)
                writeJson(exchange, 413, jsonObject("error" to "request body too large")); return
            }
            if (content.isEmpty()) {
                recordAudit(correlationId, principalId, GOVERNED_INGESTION_ACTION_NAME, batchId, AgentGatewayAccessOutcome.INVALID_SOURCE)
                writeJson(exchange, 400, jsonObject("error" to "invalid source")); return
            }
            val receivedMediaType = exchange.requestHeaders.getFirst("Content-Type")?.trim()?.takeIf { it.isNotEmpty() }
            val originalFileName = exchange.requestHeaders.getFirst(ORIGINAL_FILENAME_HEADER)?.trim()?.takeIf { it.isNotEmpty() }
            val candidate = CandidateEvidenceArtifact(content, receivedMediaType, originalFileName)

            when (val outcome = runBlocking { submitGovernedIngestionAsAgent(batchId, expectedSha256, candidate) }) {
                is parker.core.runtime.AgentGatewayGovernedIngestionResult.Ingested -> {
                    recordAudit(correlationId, principalId, GOVERNED_INGESTION_ACTION_NAME, outcome.projection.evidenceArtifactId.value, AgentGatewayAccessOutcome.REGISTERED)
                    writeJson(exchange, 201, governedIngestionJson("INGESTED", outcome.projection, outcome.correctionRepresentationId))
                }
                is parker.core.runtime.AgentGatewayGovernedIngestionResult.AlreadyIngested -> {
                    recordAudit(correlationId, principalId, GOVERNED_INGESTION_ACTION_NAME, outcome.projection.evidenceArtifactId.value, AgentGatewayAccessOutcome.ALREADY_REGISTERED)
                    writeJson(exchange, 200, governedIngestionJson("ALREADY_INGESTED", outcome.projection, outcome.correctionRepresentationId))
                }
                parker.core.runtime.AgentGatewayGovernedIngestionResult.HeldForReview -> {
                    recordAudit(correlationId, principalId, GOVERNED_INGESTION_ACTION_NAME, batchId, AgentGatewayAccessOutcome.DENIED)
                    writeJson(exchange, 409, jsonObject("status" to "HELD_FOR_REVIEW"))
                }
                is parker.core.runtime.AgentGatewayGovernedIngestionResult.ProcessingFailed -> {
                    recordAudit(correlationId, principalId, GOVERNED_INGESTION_ACTION_NAME, batchId, AgentGatewayAccessOutcome.DENIED)
                    writeJson(exchange, 409, jsonObject("status" to "PROCESSING_FAILED", "failure" to jsonObject("kind" to outcome.failure.kind.name, "detail" to outcome.failure.detail)))
                }
                parker.core.runtime.AgentGatewayGovernedIngestionResult.ProcessingResultRequired -> {
                    recordAudit(correlationId, principalId, GOVERNED_INGESTION_ACTION_NAME, batchId, AgentGatewayAccessOutcome.INVALID_SOURCE)
                    writeJson(exchange, 409, jsonObject("status" to "PROCESSING_RESULT_REQUIRED"))
                }
                // Hermes Exception Decision Backend, Task 4: the three new gate outcomes an Owner
                // decision (recorded through the separate, Owner-only OwnerEvidenceHttpServer
                // path) can now produce here. All three are DENIED-shaped audit outcomes, mirroring
                // HELD_FOR_REVIEW/PROCESSING_FAILED above -- no evidence admission occurs.
                parker.core.runtime.AgentGatewayGovernedIngestionResult.HumanRejected -> {
                    recordAudit(correlationId, principalId, GOVERNED_INGESTION_ACTION_NAME, batchId, AgentGatewayAccessOutcome.DENIED)
                    writeJson(exchange, 409, jsonObject("status" to "HUMAN_REJECTED"))
                }
                parker.core.runtime.AgentGatewayGovernedIngestionResult.ReprocessRequired -> {
                    recordAudit(correlationId, principalId, GOVERNED_INGESTION_ACTION_NAME, batchId, AgentGatewayAccessOutcome.DENIED)
                    writeJson(exchange, 409, jsonObject("status" to "REPROCESS_REQUIRED"))
                }
                parker.core.runtime.AgentGatewayGovernedIngestionResult.InvalidHumanDecision -> {
                    recordAudit(correlationId, principalId, GOVERNED_INGESTION_ACTION_NAME, batchId, AgentGatewayAccessOutcome.DENIED)
                    writeJson(exchange, 409, jsonObject("status" to "INVALID_HUMAN_DECISION"))
                }
                is parker.core.runtime.AgentGatewayGovernedIngestionResult.CorrectionLineageFailed -> {
                    recordAudit(correlationId, principalId, GOVERNED_INGESTION_ACTION_NAME, batchId, AgentGatewayAccessOutcome.DENIED)
                    writeJson(exchange, 409, jsonObject("status" to "CORRECTION_LINEAGE_FAILED", "reason" to outcome.reason))
                }
                is parker.core.runtime.AgentGatewayGovernedIngestionResult.HashMismatch -> {
                    recordAudit(correlationId, principalId, GOVERNED_INGESTION_ACTION_NAME, batchId, AgentGatewayAccessOutcome.HASH_MISMATCH)
                    writeJson(exchange, 409, jsonObject("status" to "HASH_MISMATCH", "computedSha256" to outcome.computedSha256, "expectedSha256" to outcome.expectedSha256))
                }
                parker.core.runtime.AgentGatewayGovernedIngestionResult.UnknownBatch -> {
                    recordAudit(correlationId, principalId, GOVERNED_INGESTION_ACTION_NAME, batchId, AgentGatewayAccessOutcome.NOT_FOUND)
                    writeJson(exchange, 404, jsonObject("error" to "unknown batch"))
                }
                is parker.core.runtime.AgentGatewayGovernedIngestionResult.Denied -> {
                    recordAudit(correlationId, principalId, GOVERNED_INGESTION_ACTION_NAME, batchId, AgentGatewayAccessOutcome.DENIED)
                    writeJson(exchange, 403, jsonObject("error" to "denied"))
                }
                is parker.core.runtime.AgentGatewayGovernedIngestionResult.Conflict -> {
                    recordAudit(correlationId, principalId, GOVERNED_INGESTION_ACTION_NAME, outcome.evidenceArtifactId.value, AgentGatewayAccessOutcome.SOURCE_IDENTITY_CONFLICT)
                    writeJson(exchange, 500, jsonObject("error" to "source identity conflict"))
                }
                is parker.core.runtime.AgentGatewayGovernedIngestionResult.CaseBindingRejected -> {
                    recordAudit(correlationId, principalId, GOVERNED_INGESTION_ACTION_NAME, batchId, AgentGatewayAccessOutcome.DENIED)
                    writeJson(exchange, 409, jsonObject("status" to "CASE_BINDING_REJECTED", "reason" to outcome.reason))
                }
            }
        }
    }

    private fun hermesProcessingResultJson(result: parker.core.interfaces.HermesProcessingResult): JsonObject = jsonObject(
        "sourceSha256" to result.sourceSha256,
        "batchId" to result.batchId,
        "status" to result.status.name,
        "methods" to JsonArray(result.methods.map { it.name }),
        "proposedEvidenceArtifactId" to result.proposedEvidenceArtifactId?.value,
        "reviewConfidenceThreshold" to result.reviewConfidenceThreshold,
        "processingCompleteness" to result.processingCompleteness?.name,
        "processingWarnings" to JsonArray(result.processingWarnings),
        "issues" to JsonArray(result.issues.map { issue ->
            jsonObject(
                "kind" to issue.kind.name,
                "explanation" to issue.explanation,
                "location" to (issue.location as? parker.core.interfaces.HermesProcessingIssueLocation.DocumentPage)?.let { location ->
                    jsonObject(
                        "pageNumber" to location.pageNumber,
                        "startOffsetInclusive" to location.startOffsetInclusive,
                        "endOffsetExclusive" to location.endOffsetExclusive,
                        "regionDescription" to location.regionDescription,
                    )
                },
                "hermesInterpretation" to issue.hermesInterpretation,
                "transcriptionFidelity" to issue.transcriptionFidelity?.name,
                "observedConfidence" to issue.observedConfidence,
            )
        }),
        "failure" to result.failure?.let { failure -> jsonObject("kind" to failure.kind.name, "detail" to failure.detail) },
    )

    /** Hermes Governed Ingestion, Task 3. Mirrors [EvidenceHandler]'s own `submissionJson` shape exactly -- the same flat, opaque-identifier-only manifest fields, plus the governed-ingestion-specific status token. */
    private fun governedIngestionJson(
        status: String,
        projection: parker.core.runtime.AgentGatewayEvidenceManifestProjection,
        correctionRepresentationId: parker.core.interfaces.HermesPreIngestionCorrectionId? = null,
    ) = jsonObject(
        "status" to status,
        "evidenceArtifactId" to projection.evidenceArtifactId.value,
        "sha256" to projection.sha256,
        "byteLength" to projection.byteLength,
        "receivedMediaType" to projection.receivedMediaType,
        "originalFileName" to projection.originalFileName,
        "correctionRepresentationId" to correctionRepresentationId?.value,
    )

    private fun analysisPackageJson(packageValue: parker.core.runtime.AnalysisRetrievalPackage) = jsonObject(
        "status" to "ACCEPTED",
        "requestId" to packageValue.requestId.value,
        "question" to packageValue.question,
        "analysisType" to packageValue.analysisType.name,
        "scope" to jsonObject(
            "evidenceArtifactIds" to JsonArray(packageValue.scope.evidenceArtifactIds.map { it.value }),
            "derivativeGenerationIds" to jsonObject(*packageValue.scope.derivativeGenerationIds.map { (evidence, generation) -> evidence to generation.value }.toTypedArray()),
        ),
        "evidence" to JsonArray(packageValue.evidence.map { item ->
            jsonObject(
                "evidenceArtifactId" to item.evidenceArtifactId.value,
                "manifest" to jsonObject(
                    "evidenceArtifactId" to item.manifest.evidenceArtifactId.value,
                    "sha256" to item.manifest.sha256,
                    "byteLength" to item.manifest.byteLength,
                    "receivedMediaType" to item.manifest.receivedMediaType,
                    "originalFileName" to item.manifest.originalFileName,
                    "correctionLineage" to correctionLineageJson(item.manifest.correctionLineage),
                    "correctedContent" to correctedContentJson(item.manifest.correctedContent),
                ),
                "governedContent" to item.governedContent?.let { governedContentJson(it) },
            )
        }),
    )

    private fun correctionLineageJson(lineage: parker.core.interfaces.HermesPreIngestionCorrectionLineage?): JsonObject? = lineage?.let {
        jsonObject(
            "correctionId" to it.correction.representationId.value,
            "evidenceArtifactId" to it.binding.evidenceArtifactId.value,
            "sourceSha256" to it.binding.sourceSha256,
            "createdAt" to it.binding.createdAt.toString(),
            "batchId" to it.correction.batchId,
            "issueIndex" to it.correction.issueIndex,
            "machineIssueKind" to it.correction.machineIssueKind.name,
            "machineIssueExplanation" to it.correction.machineIssueExplanation,
            "machineInterpretation" to it.correction.machineInterpretation,
            "correctedInterpretation" to it.correction.correctedInterpretation,
            "ownerExplanation" to it.correction.ownerExplanation,
            "ownerPrincipalId" to it.correction.ownerPrincipalId.value,
            "decisionAt" to it.correction.decisionAt.toString(),
        )
    }

    private fun correctedContentJson(content: parker.core.interfaces.HermesOwnerCorrectedContent?): JsonObject? = content?.let {
        jsonObject(
            "authority" to it.authority,
            "scope" to it.scope,
            "evidenceArtifactId" to it.evidenceArtifactId.value,
            "correctionId" to it.correctionId.value,
            "sourceSha256" to it.sourceSha256,
            "issueIndex" to it.issueIndex,
            "machineInterpretation" to it.machineInterpretation,
            "correctedInterpretation" to it.correctedInterpretation,
            "ownerExplanation" to it.ownerExplanation,
            "ownerPrincipalId" to it.ownerPrincipalId.value,
            "decisionAt" to it.decisionAt.toString(),
        )
    }

    private fun governedContentJson(content: parker.core.runtime.AnalysisGovernedContent) = jsonObject(
        "derivativeGenerationId" to content.derivativeGenerationId.value,
        "rootSourceEvidenceArtifactId" to content.record.rootSourceEvidenceArtifactId.value,
        "derivativeKind" to content.record.derivativeKind,
        "generatedAt" to content.record.generatedAt.toString(),
        "parents" to JsonArray(content.record.parents.map { parent ->
            when (parent) {
                is parker.core.interfaces.DerivativeParentReference.RootEvidenceArtifact -> jsonObject("kind" to "ROOT_EVIDENCE_ARTIFACT", "evidenceArtifactId" to parent.evidenceArtifactId.value)
                is parker.core.interfaces.DerivativeParentReference.ChildSourceEvidenceArtifact -> jsonObject("kind" to "CHILD_SOURCE_EVIDENCE_ARTIFACT", "evidenceArtifactId" to parent.evidenceArtifactId.value)
                is parker.core.interfaces.DerivativeParentReference.ParentGeneration -> jsonObject("kind" to "PARENT_GENERATION", "derivativeGenerationId" to parent.derivativeGenerationId.value)
            }
        }),
        "producerIdentity" to jsonObject(
            "pluginIdentity" to content.record.producerIdentity.pluginIdentity,
            "pluginVersion" to content.record.producerIdentity.pluginVersion,
            "configurationIdentity" to content.record.producerIdentity.configurationIdentity,
            "adapterIdentity" to content.record.producerIdentity.adapterIdentity,
            "adapterVersion" to content.record.producerIdentity.adapterVersion,
            "modelIdentity" to content.record.producerIdentity.modelIdentity,
            "modelVersion" to content.record.producerIdentity.modelVersion,
        ),
        "contentIdentity" to when (val identity = content.record.contentIdentity) {
            is parker.core.interfaces.DerivativeContentIdentity.NoCanonicalSerialization -> jsonObject("kind" to "NO_CANONICAL_SERIALIZATION")
            is parker.core.interfaces.DerivativeContentIdentity.Digest -> jsonObject("kind" to "DIGEST", "algorithm" to identity.algorithm, "digest" to identity.digest)
        },
        "confidence" to content.record.confidence,
        "transformationHistory" to JsonArray(content.record.transformationHistory.map { it.name }),
        "completenessState" to content.record.completenessState.name,
        "operationalOutcome" to content.record.operationalOutcome.name,
        "warnings" to JsonArray(content.record.warnings),
        "payload" to governedPayloadJson(content.payload),
    )

    private fun governedPayloadJson(payload: parker.core.interfaces.TierADerivativePayload): JsonObject = when (payload) {
        is parker.core.interfaces.TierADerivativePayload.Pdf -> jsonObject(
            "kind" to "PDF",
            "documentText" to payload.value.documentText,
            "pageCount" to payload.value.pageCount,
            "pageTextAssociationAvailable" to payload.value.pageTextAssociationAvailable,
            "metadata" to JsonArray(payload.value.metadata.map { jsonObject("name" to it.name, "value" to it.value, "representation" to it.representation) }),
            "warnings" to JsonArray(payload.value.warnings),
        )
        is parker.core.interfaces.TierADerivativePayload.Docx -> jsonObject(
            "kind" to "DOCX",
            "paragraphs" to JsonArray(payload.value.paragraphs.map { paragraph ->
                jsonObject("order" to paragraph.order, "text" to paragraph.text, "styleId" to paragraph.styleId, "numberingId" to paragraph.numberingId, "numberingLevel" to paragraph.numberingLevel, "hardPageBreakCount" to paragraph.hardPageBreakCount, "runs" to JsonArray(paragraph.runs.map { run -> jsonObject("order" to run.order, "text" to run.text, "bold" to run.bold, "italic" to run.italic) }))
            }),
            "tables" to JsonArray(payload.value.tables.map { table -> jsonObject("order" to table.order, "styleId" to table.styleId, "rows" to JsonArray(table.rows.map { row -> jsonObject("order" to row.order, "cells" to JsonArray(row.cells.map { cell -> jsonObject("order" to cell.order, "text" to cell.text) })) })) }),
            "headers" to JsonArray(payload.value.headers.map { header -> jsonObject("kind" to header.kind, "order" to header.order, "relationshipId" to header.relationshipId, "paragraphs" to JsonArray(header.paragraphs.map { it.text })) }),
            "footers" to JsonArray(payload.value.footers.map { footer -> jsonObject("kind" to footer.kind, "order" to footer.order, "relationshipId" to footer.relationshipId, "paragraphs" to JsonArray(footer.paragraphs.map { it.text })) }),
            "metadata" to jsonObject("title" to payload.value.metadata.title, "author" to payload.value.metadata.author, "subject" to payload.value.metadata.subject, "parsedCreated" to payload.value.metadata.parsedCreated?.toString(), "application" to payload.value.metadata.application, "applicationVersion" to payload.value.metadata.applicationVersion),
            "parts" to JsonArray(payload.value.parts.map { jsonObject("name" to it.name, "contentType" to it.contentType, "uncompressedBytes" to it.uncompressedBytes) }),
            "relationshipCount" to payload.value.relationshipCount,
            "relationshipTypes" to JsonArray(payload.value.relationshipTypes),
            "mediaPartNames" to JsonArray(payload.value.mediaPartNames),
            "warnings" to JsonArray(payload.value.warnings),
        )
        is parker.core.interfaces.TierADerivativePayload.Csv -> jsonObject(
            "kind" to "CSV",
            "headers" to JsonArray(payload.value.headers),
            "rows" to JsonArray(payload.value.rows.map { JsonArray(it) }),
            "delimiter" to payload.value.delimiter.toString(),
            "quoteCharacter" to payload.value.quoteCharacter.toString(),
            "lineEnding" to payload.value.lineEnding,
            "warnings" to JsonArray(payload.value.warnings),
        )
        is parker.core.interfaces.TierADerivativePayload.Eml -> jsonObject(
            "kind" to "EML",
            "from" to payload.value.from,
            "to" to payload.value.to,
            "cc" to payload.value.cc,
            "rawDate" to payload.value.rawDate,
            "parsedDate" to payload.value.parsedDate?.toString(),
            "subject" to payload.value.subject,
            "messageId" to payload.value.messageId,
            "mimeVersion" to payload.value.mimeVersion,
            "contentType" to payload.value.contentType,
            "headers" to JsonArray(payload.value.headers.map { jsonObject("name" to it.name, "value" to it.value, "rawRepresentation" to it.rawRepresentation) }),
            "mimeEntities" to JsonArray(payload.value.mimeEntities.map { jsonObject("entityId" to it.entityId, "parentEntityId" to it.parentEntityId, "order" to it.order, "mediaType" to it.mediaType, "disposition" to it.disposition, "transferEncoding" to it.transferEncoding, "filename" to it.filename, "charset" to it.charset, "childEntityIds" to JsonArray(it.childEntityIds), "contentId" to it.contentId) }),
            "bodyAlternatives" to JsonArray(payload.value.bodyAlternatives.map { jsonObject("mimeEntityId" to it.mimeEntityId, "mediaType" to it.mediaType, "charset" to it.charset, "decodedText" to it.decodedText) }),
            "attachmentCandidates" to JsonArray(payload.value.attachmentCandidates.map { jsonObject("mimeEntityId" to it.mimeEntityId, "parentMimeEntityId" to it.parentMimeEntityId, "filename" to it.filename, "declaredMimeType" to it.declaredMimeType, "disposition" to it.disposition, "transferEncoding" to it.transferEncoding, "charset" to it.charset, "byteLength" to it.byteLength, "sha256" to it.sha256, "contentId" to it.contentId) }),
            "childSourceCandidateCount" to payload.childSourceCandidateCount,
            "warnings" to JsonArray(payload.value.warnings),
        )
        is parker.core.interfaces.TierADerivativePayload.Ocr -> jsonObject(
            "kind" to "OCR",
            "recognisedText" to payload.value.recognisedText,
            "fidelity" to payload.value.fidelity.name,
            "outcomeKind" to payload.value.outcomeKind.name,
            "degradationReason" to payload.value.degradationReason,
            "warnings" to JsonArray(payload.value.warnings),
            "segments" to JsonArray(payload.value.segments.map { jsonObject("text" to it.text, "fidelity" to it.fidelity.name, "pageNumber" to it.pageNumber) }),
        )
        is parker.core.interfaces.TierADerivativePayload.RegionTranscription -> jsonObject(
            "kind" to "REGION_TRANSCRIPTION",
            "evidenceArtifactId" to payload.value.evidenceArtifactId,
            "sourceSha256" to payload.value.sourceSha256,
            "pageBindings" to JsonArray(payload.value.pageBindings),
            "regionBindings" to JsonArray(payload.value.regionBindings),
            "transcriptionBlocks" to JsonArray(payload.value.transcriptionBlocks),
            "providerReturnedOrder" to JsonArray(payload.value.providerReturnedOrder),
            "parkerSourceOrder" to JsonArray(payload.value.parkerSourceOrder),
            "provider" to payload.value.provider,
            "model" to payload.value.model,
            "processingProfile" to payload.value.processingProfile,
            "admissionProvenance" to payload.value.admissionProvenance,
        )
    }

    fun stop() {
        server?.stop(1)
        server = null
        executor?.shutdownNow()
        executor = null
        logger.info("Agent Gateway HTTP server stopped")
    }

    // ---- authentication -----------------------------------------------------------------

    /** `Authorization: Bearer <token>` only -- never a cookie of any kind. */
    private fun bearerToken(exchange: HttpExchange): String? {
        val header = exchange.requestHeaders.getFirst("Authorization") ?: return null
        return header.removePrefix("Bearer ").takeIf { header.startsWith("Bearer ") && it.isNotEmpty() }
    }

    private inner class EvidenceHandler : HttpHandler {
        override fun handle(exchange: HttpExchange) {
            val correlationId = UUID.randomUUID().toString()
            try {
                val token = bearerToken(exchange)
                if (token == null) {
                    runCatching { exchange.requestBody.use { it.readBytes() } }
                    recordAudit(correlationId, null, null, null, AgentGatewayAccessOutcome.UNAUTHENTICATED)
                    writeJson(exchange, 401, jsonObject("error" to "unauthorised"))
                    return
                }
                val principalId = authentication.authenticate(token)
                if (principalId == null) {
                    runCatching { exchange.requestBody.use { it.readBytes() } }
                    recordAudit(correlationId, null, null, null, AgentGatewayAccessOutcome.AUTHENTICATION_FAILED)
                    writeJson(exchange, 401, jsonObject("error" to "unauthorised"))
                    return
                }

                val analysisRequest = analysisPrincipalId != null && principalId == analysisPrincipalId

                if (exchange.requestMethod == "POST" && exchange.requestURI.path == "/agent/evidence") {
                    if (analysisRequest) {
                        runCatching { exchange.requestBody.use { it.readBytes() } }
                        recordAudit(correlationId, principalId, null, null, AgentGatewayAccessOutcome.DENIED)
                        writeJson(exchange, 403, jsonObject("error" to "denied")); return
                    }
                    handleSubmit(exchange, correlationId, principalId)
                    return
                }

                if (exchange.requestMethod == "POST") {
                    if (analysisRequest) {
                        runCatching { exchange.requestBody.use { it.readBytes() } }
                        recordAudit(correlationId, principalId, null, null, AgentGatewayAccessOutcome.DENIED)
                        writeJson(exchange, 403, jsonObject("error" to "denied")); return
                    }
                    val postSegments = exchange.requestURI.path.removePrefix("/agent/evidence/").split('/').filter { it.isNotEmpty() }
                    if (postSegments.size == 2 && postSegments[1] == "acquire") {
                        handleAcquire(exchange, correlationId, principalId, postSegments[0])
                        return
                    }
                    if (postSegments.size == 2 && postSegments[1] == "assign") {
                        handleAssign(exchange, correlationId, principalId, postSegments[0])
                        return
                    }
                }

                if (exchange.requestMethod != "GET") {
                    runCatching { exchange.requestBody.use { it.readBytes() } }
                    recordAudit(correlationId, principalId, null, null, AgentGatewayAccessOutcome.NOT_FOUND_ROUTE)
                    writeJson(exchange, 404, jsonObject("error" to "not found"))
                    return
                }
                runCatching { exchange.requestBody.use { it.readBytes() } }

                val segments = exchange.requestURI.path.removePrefix("/agent/evidence/").split('/').filter { it.isNotEmpty() }
                when (segments.size) {
                    1 -> handleRetrieve(exchange, correlationId, principalId, segments[0], analysisRequest)
                    2 -> if (segments[1] == "manifest") {
                        handleRetrieveManifest(exchange, correlationId, principalId, segments[0], analysisRequest)
                    } else {
                        recordAudit(correlationId, principalId, null, null, AgentGatewayAccessOutcome.NOT_FOUND_ROUTE)
                        writeJson(exchange, 404, jsonObject("error" to "not found"))
                    }
                    else -> {
                        recordAudit(correlationId, principalId, null, null, AgentGatewayAccessOutcome.NOT_FOUND_ROUTE)
                        writeJson(exchange, 404, jsonObject("error" to "not found"))
                    }
                }
            } catch (e: Exception) {
                logger.error("Agent Gateway HTTP: request failed safely (correlationId=$correlationId)", e)
                runCatching { recordAudit(correlationId, null, null, null, AgentGatewayAccessOutcome.INTERNAL_FAILURE) }
                runCatching { writeJson(exchange, 500, jsonObject("error" to "internal error")) }
            } finally {
                exchange.close()
            }
        }

        private fun parseEvidenceId(exchange: HttpExchange, correlationId: String, principalId: PrincipalId, rawId: String): EvidenceArtifactId? {
            if (!SAFE_ROUTE_ID.matches(rawId)) {
                recordAudit(correlationId, principalId, RETRIEVE_ACTION_NAME, rawId, AgentGatewayAccessOutcome.MALFORMED_IDENTIFIER)
                writeJson(exchange, 400, jsonObject("error" to "invalid evidence artefact id"))
                return null
            }
            return try {
                EvidenceArtifactId(rawId)
            } catch (_: IllegalArgumentException) {
                recordAudit(correlationId, principalId, RETRIEVE_ACTION_NAME, rawId, AgentGatewayAccessOutcome.MALFORMED_IDENTIFIER)
                writeJson(exchange, 400, jsonObject("error" to "invalid evidence artefact id"))
                null
            }
        }

        private fun handleRetrieve(exchange: HttpExchange, correlationId: String, principalId: PrincipalId, rawId: String, analysisRequest: Boolean) {
            val id = parseEvidenceId(exchange, correlationId, principalId, rawId) ?: return
            when (val result = runBlocking {
                if (analysisRequest) retrieveEvidenceAsAnalysisAgent?.invoke(id)
                    ?: AgentGatewayEvidenceRetrievalResult.Denied(id, PermissionDecisionOutcome.DENIED)
                else retrieveEvidenceAsAgent(id)
            }) {
                is AgentGatewayEvidenceRetrievalResult.Found -> {
                    recordAudit(correlationId, principalId, RETRIEVE_ACTION_NAME, id.value, AgentGatewayAccessOutcome.APPROVED)
                    writeJson(exchange, 200, jsonObject(
                        "status" to "FOUND",
                        "evidenceArtifactId" to result.evidenceArtifactId.value,
                        "byteLength" to result.byteLength,
                    ))
                }
                is AgentGatewayEvidenceRetrievalResult.NotFound -> {
                    recordAudit(correlationId, principalId, RETRIEVE_ACTION_NAME, id.value, AgentGatewayAccessOutcome.NOT_FOUND)
                    writeJson(exchange, 404, jsonObject("error" to "not found"))
                }
                is AgentGatewayEvidenceRetrievalResult.Denied -> {
                    recordAudit(correlationId, principalId, RETRIEVE_ACTION_NAME, id.value, AgentGatewayAccessOutcome.DENIED)
                    writeJson(exchange, 403, jsonObject("error" to "denied"))
                }
            }
        }

        private fun handleRetrieveManifest(exchange: HttpExchange, correlationId: String, principalId: PrincipalId, rawId: String, analysisRequest: Boolean) {
            val id = parseEvidenceId(exchange, correlationId, principalId, rawId) ?: return
            when (val result = runBlocking {
                if (analysisRequest) retrieveEvidenceManifestAsAnalysisAgent?.invoke(id)
                    ?: AgentGatewayEvidenceManifestResult.Denied(id, PermissionDecisionOutcome.DENIED)
                else retrieveEvidenceManifestAsAgent(id)
            }) {
                is AgentGatewayEvidenceManifestResult.Found -> {
                    recordAudit(correlationId, principalId, RETRIEVE_MANIFEST_ACTION_NAME, id.value, AgentGatewayAccessOutcome.APPROVED)
                    writeJson(exchange, 200, jsonObject(
                        "status" to "FOUND",
                        "evidenceArtifactId" to result.manifest.evidenceArtifactId.value,
                        "sha256" to result.manifest.sha256,
                        "byteLength" to result.manifest.byteLength,
                        "receivedMediaType" to result.manifest.receivedMediaType,
                        "originalFileName" to result.manifest.originalFileName,
                        "correctionLineage" to correctionLineageJson(result.manifest.correctionLineage),
                        "correctedContent" to correctedContentJson(result.manifest.correctedContent),
                    ))
                }
                is AgentGatewayEvidenceManifestResult.NotFound -> {
                    recordAudit(correlationId, principalId, RETRIEVE_MANIFEST_ACTION_NAME, id.value, AgentGatewayAccessOutcome.NOT_FOUND)
                    writeJson(exchange, 404, jsonObject("error" to "not found"))
                }
                is AgentGatewayEvidenceManifestResult.Denied -> {
                    recordAudit(correlationId, principalId, RETRIEVE_MANIFEST_ACTION_NAME, id.value, AgentGatewayAccessOutcome.DENIED)
                    writeJson(exchange, 403, jsonObject("error" to "denied"))
                }
            }
        }

        /**
         * Parker Agent Gateway, AG-1F (R1 Candidate-Source Submission). Raw request body bytes
         * are the candidate source; `Content-Type`/`X-Parker-Original-Filename`/
         * `X-Parker-Advisory-Sha256` are the only metadata read. Enforces size and shape
         * constraints -- oversized body, empty body, malformed advisory-hash header -- *before*
         * [submitSourceAsAgent] is ever called, exactly as [parseEvidenceId] already rejects a
         * malformed identifier before either GET operation runs. Never computes a hash itself,
         * never invents an [EvidenceArtifactId] -- both remain entirely
         * [parker.core.interfaces.EvidenceCustodian]'s own responsibility, reached only through
         * [submitSourceAsAgent].
         */
        private fun handleSubmit(exchange: HttpExchange, correlationId: String, principalId: PrincipalId) {
            val advisorySha256Raw = exchange.requestHeaders.getFirst(ADVISORY_SHA256_HEADER)?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }
            if (advisorySha256Raw != null && !SHA256_PATTERN.matches(advisorySha256Raw)) {
                runCatching { exchange.requestBody.use { it.readBytes() } }
                recordAudit(correlationId, principalId, SUBMIT_ACTION_NAME, null, AgentGatewayAccessOutcome.INVALID_SOURCE)
                writeJson(exchange, 400, jsonObject("error" to "invalid advisory sha256"))
                return
            }

            val content = try {
                readBounded(exchange.requestBody, MAX_SUBMISSION_BYTES)
            } catch (_: RequestBodyTooLargeException) {
                recordAudit(correlationId, principalId, SUBMIT_ACTION_NAME, null, AgentGatewayAccessOutcome.INVALID_SOURCE)
                writeJson(exchange, 413, jsonObject("error" to "request body too large"))
                return
            }
            if (content.isEmpty()) {
                recordAudit(correlationId, principalId, SUBMIT_ACTION_NAME, null, AgentGatewayAccessOutcome.INVALID_SOURCE)
                writeJson(exchange, 400, jsonObject("error" to "invalid source"))
                return
            }

            val receivedMediaType = exchange.requestHeaders.getFirst("Content-Type")?.trim()?.takeIf { it.isNotEmpty() }
            val originalFileName = exchange.requestHeaders.getFirst(ORIGINAL_FILENAME_HEADER)?.trim()?.takeIf { it.isNotEmpty() }
            val candidate = CandidateEvidenceArtifact(content, receivedMediaType, originalFileName)

            val batchId = exchange.requestHeaders.getFirst(BATCH_ID_HEADER)?.trim()?.takeIf { it.isNotEmpty() }
            when (val result = runBlocking { submitSourceWithBatchAsAgent?.invoke(candidate, advisorySha256Raw, batchId) ?: submitSourceAsAgent(candidate, advisorySha256Raw) }) {
                is AgentGatewaySourceSubmissionResult.Registered -> {
                    recordAudit(correlationId, principalId, SUBMIT_ACTION_NAME, result.projection.evidenceArtifactId.value, AgentGatewayAccessOutcome.REGISTERED)
                    writeJson(exchange, 201, submissionJson("REGISTERED", result.projection))
                }
                is AgentGatewaySourceSubmissionResult.AlreadyRegistered -> {
                    recordAudit(correlationId, principalId, SUBMIT_ACTION_NAME, result.projection.evidenceArtifactId.value, AgentGatewayAccessOutcome.ALREADY_REGISTERED)
                    writeJson(exchange, 200, submissionJson("ALREADY_REGISTERED", result.projection))
                }
                is AgentGatewaySourceSubmissionResult.HashMismatch -> {
                    recordAudit(correlationId, principalId, SUBMIT_ACTION_NAME, null, AgentGatewayAccessOutcome.HASH_MISMATCH)
                    writeJson(exchange, 409, jsonObject(
                        "error" to "hash mismatch",
                        "computedSha256" to result.computedSha256,
                        "advisorySha256" to result.advisorySha256,
                    ))
                }
                is AgentGatewaySourceSubmissionResult.Denied -> {
                    recordAudit(correlationId, principalId, SUBMIT_ACTION_NAME, null, AgentGatewayAccessOutcome.DENIED)
                    writeJson(exchange, 403, jsonObject("error" to "denied"))
                }
                is AgentGatewaySourceSubmissionResult.Conflict -> {
                    recordAudit(correlationId, principalId, SUBMIT_ACTION_NAME, result.evidenceArtifactId.value, AgentGatewayAccessOutcome.SOURCE_IDENTITY_CONFLICT)
                    writeJson(exchange, 500, jsonObject("error" to "source identity conflict"))
                }
            }
        }

        private fun handleAssign(exchange: HttpExchange, correlationId: String, principalId: PrincipalId, rawId: String) {
            runCatching { exchange.requestBody.use { it.readBytes() } }
            val id = parseEvidenceId(exchange, correlationId, principalId, rawId) ?: return
            val batchId = exchange.requestHeaders.getFirst(BATCH_ID_HEADER)?.trim()
            if (batchId.isNullOrBlank()) {
                recordAudit(correlationId, principalId, BIND_ACTION_NAME, id.value, AgentGatewayAccessOutcome.INVALID_SOURCE)
                writeJson(exchange, 400, jsonObject("error" to "missing ingestion batch id")); return
            }
            when (val result = runBlocking { bindIngestionEvidenceAsAgent(batchId, id) }) {
                is AgentGatewayBulkBindingResult.Assigned -> {
                    recordAudit(correlationId, principalId, BIND_ACTION_NAME, id.value, AgentGatewayAccessOutcome.APPROVED)
                    writeJson(exchange, 200, jsonObject("status" to "ASSIGNED", "evidenceArtifactId" to id.value))
                }
                AgentGatewayBulkBindingResult.Denied -> { recordAudit(correlationId, principalId, BIND_ACTION_NAME, id.value, AgentGatewayAccessOutcome.DENIED); writeJson(exchange, 403, jsonObject("error" to "denied")) }
                AgentGatewayBulkBindingResult.UnknownBatch, AgentGatewayBulkBindingResult.EvidenceNotSubmitted -> { recordAudit(correlationId, principalId, BIND_ACTION_NAME, id.value, AgentGatewayAccessOutcome.NOT_FOUND); writeJson(exchange, 404, jsonObject("error" to "batch or evidence not found")) }
                is AgentGatewayBulkBindingResult.Rejected -> { recordAudit(correlationId, principalId, BIND_ACTION_NAME, id.value, AgentGatewayAccessOutcome.DENIED); writeJson(exchange, 409, jsonObject("error" to result.reason)) }
                is AgentGatewayBulkBindingResult.Failed -> { recordAudit(correlationId, principalId, BIND_ACTION_NAME, id.value, AgentGatewayAccessOutcome.INTERNAL_FAILURE); writeJson(exchange, 500, jsonObject("error" to result.reason)) }
            }
        }

        /**
         * Parker Agent Gateway, AG-1G (R2 Governed-Acquisition Request). No request body of any
         * kind is read as input -- any body present is discarded, unread, exactly like the GET
         * routes above. `evidenceArtifactId` is the only caller-supplied fact; no acquisition
         * mode, provider, or model may ever be selected here. Contains no acquisition, provider,
         * or egress logic of its own -- [requestAcquisitionAsAgent] is the entire delegation.
         */
        private fun handleAcquire(exchange: HttpExchange, correlationId: String, principalId: PrincipalId, rawId: String) {
            runCatching { exchange.requestBody.use { it.readBytes() } }
            val id = parseEvidenceId(exchange, correlationId, principalId, rawId) ?: return
            when (val result = runBlocking { requestAcquisitionAsAgent(id) }) {
                is AgentGatewayAcquisitionResult.Completed -> {
                    recordAudit(correlationId, principalId, ACQUIRE_ACTION_NAME, id.value, AgentGatewayAccessOutcome.ACQUISITION_COMPLETED)
                    writeJson(exchange, 200, jsonObject(
                        "status" to "COMPLETED",
                        "evidenceArtifactId" to result.evidenceArtifactId.value,
                        "derivativeGenerationId" to result.derivativeGenerationId.value,
                        "capabilityId" to result.capabilityId,
                        "mechanism" to result.mechanism.name,
                    ))
                }
                is AgentGatewayAcquisitionResult.AuthorizationRequired -> {
                    recordAudit(correlationId, principalId, ACQUIRE_ACTION_NAME, id.value, AgentGatewayAccessOutcome.ACQUISITION_AUTHORIZATION_REQUIRED)
                    writeJson(exchange, 409, jsonObject("status" to "AUTHORIZATION_REQUIRED", "evidenceArtifactId" to id.value))
                }
                is AgentGatewayAcquisitionResult.ProviderNotReady -> {
                    recordAudit(correlationId, principalId, ACQUIRE_ACTION_NAME, id.value, AgentGatewayAccessOutcome.ACQUISITION_PROVIDER_NOT_READY)
                    writeJson(exchange, 409, jsonObject("status" to "PROVIDER_NOT_READY", "evidenceArtifactId" to id.value))
                }
                is AgentGatewayAcquisitionResult.NotFound -> {
                    recordAudit(correlationId, principalId, ACQUIRE_ACTION_NAME, id.value, AgentGatewayAccessOutcome.NOT_FOUND)
                    writeJson(exchange, 404, jsonObject("error" to "not found"))
                }
                is AgentGatewayAcquisitionResult.Failed -> {
                    recordAudit(correlationId, principalId, ACQUIRE_ACTION_NAME, id.value, AgentGatewayAccessOutcome.ACQUISITION_FAILED)
                    writeJson(exchange, 409, jsonObject("status" to "FAILED", "evidenceArtifactId" to id.value, "reason" to result.reason))
                }
                is AgentGatewayAcquisitionResult.Denied -> {
                    recordAudit(correlationId, principalId, ACQUIRE_ACTION_NAME, id.value, AgentGatewayAccessOutcome.DENIED)
                    writeJson(exchange, 403, jsonObject("error" to "denied"))
                }
            }
        }

        private fun submissionJson(status: String, projection: parker.core.runtime.AgentGatewayEvidenceManifestProjection) = jsonObject(
            "status" to status,
            "evidenceArtifactId" to projection.evidenceArtifactId.value,
            "sha256" to projection.sha256,
            "byteLength" to projection.byteLength,
            "receivedMediaType" to projection.receivedMediaType,
            "originalFileName" to projection.originalFileName,
        )
    }

    private fun recordAudit(
        correlationId: String,
        principalId: PrincipalId?,
        operation: String?,
        targetId: String?,
        outcome: AgentGatewayAccessOutcome,
    ) {
        runCatching {
            runBlocking {
                audit.record(
                    AgentGatewayAccessAuditRecord(
                        principalId = principalId,
                        correlationId = correlationId,
                        operation = operation,
                        targetId = targetId,
                        outcome = outcome,
                        recordedAt = Instant.now(),
                    ),
                )
            }
        }.onFailure { e -> logger.error("Agent Gateway HTTP: failed to record access audit (correlationId=$correlationId)", e) }
    }

    // ---- minimal JSON writer (mirrors OwnerEvidenceHttpServer's own identical, per-file
    // private helper -- no JSON library exists anywhere in this repository) ------------------

    private sealed interface JsonValue
    private class JsonObject(val fields: List<Pair<String, Any?>>) : JsonValue
    private class JsonArray(val items: List<Any?>) : JsonValue

    private fun jsonObject(vararg fields: Pair<String, Any?>): JsonObject = JsonObject(fields.toList())

    private fun writeJsonValue(sb: StringBuilder, value: Any?) {
        when (value) {
            null -> sb.append("null")
            is JsonObject -> {
                sb.append('{')
                value.fields.forEachIndexed { index, (key, v) ->
                    if (index > 0) sb.append(',')
                    sb.append(jsonString(key)).append(':')
                    writeJsonValue(sb, v)
                }
                sb.append('}')
            }
            is JsonArray -> {
                sb.append('[')
                value.items.forEachIndexed { index, item -> if (index > 0) sb.append(','); writeJsonValue(sb, item) }
                sb.append(']')
            }
            is String -> sb.append(jsonString(value))
            is Int -> sb.append(value)
            is Long -> sb.append(value)
            is Boolean -> sb.append(value)
            else -> sb.append(jsonString(value.toString()))
        }
    }

    private fun jsonString(raw: String): String {
        val sb = StringBuilder(raw.length + 2)
        sb.append('"')
        for (c in raw) {
            when (c) {
                '"' -> sb.append("\\\"")
                '\\' -> sb.append("\\\\")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                else -> if (c.code < 0x20) sb.append("\\u%04x".format(c.code)) else sb.append(c)
            }
        }
        sb.append('"')
        return sb.toString()
    }

    private fun writeJson(exchange: HttpExchange, status: Int, body: JsonValue) {
        val sb = StringBuilder()
        writeJsonValue(sb, body)
        val bytes = sb.toString().toByteArray(StandardCharsets.UTF_8)
        exchange.responseHeaders.set("Content-Type", "application/json; charset=utf-8")
        exchange.sendResponseHeaders(status, bytes.size.toLong())
        exchange.responseBody.use { it.write(bytes) }
    }

    private companion object {
        val SAFE_ROUTE_ID = Regex("^[A-Za-z0-9._-]{1,1024}$")
        val SHA256_PATTERN = Regex("^[0-9a-f]{64}$")
        const val RETRIEVE_ACTION_NAME = "agent-gateway.evidence.retrieve"
        const val RETRIEVE_MANIFEST_ACTION_NAME = "agent-gateway.evidence.retrieve-manifest"
        const val SUBMIT_ACTION_NAME = "agent-gateway.evidence.submit"
        const val ACQUIRE_ACTION_NAME = "agent-gateway.evidence.acquire"
        const val ORIGINAL_FILENAME_HEADER = "X-Parker-Original-Filename"
        const val ADVISORY_SHA256_HEADER = "X-Parker-Advisory-Sha256"
        const val BATCH_ID_HEADER = "X-Parker-Ingestion-Batch-Id"
        const val BIND_ACTION_NAME = "agent-gateway.ingestion.bind"
        const val PROCESSING_RESULT_SUBMIT_ACTION_NAME = "agent-gateway.processing-result.submit"
        const val PROCESSING_RESULT_LIST_ACTION_NAME = "agent-gateway.processing-result.list"
        const val PENDING_REVIEW_SOURCE_SUBMIT_ACTION_NAME = "agent-gateway.pending-review-source.submit"
        /**
         * Hermes Governed Ingestion, Task 3. An audit-log label only -- distinct from
         * [AGENT_GATEWAY_EVIDENCE_SUBMIT_ACTION_NAME], which is what [handleSubmitGovernedIngestion]
         * actually authorises against (reused unchanged, per "must be denied if PermissionEngine
         * denies the existing governed source-submission action"). Keeping this label distinct
         * lets an audit-log reviewer tell a governed-ingestion attempt apart from an ordinary
         * `/agent/evidence` submission even though both share one permission verb.
         */
        const val GOVERNED_INGESTION_ACTION_NAME = "agent-gateway.governed-ingestion.submit"
        const val ANALYSIS_REQUEST_ACTION_NAME = "agent-gateway.analysis.request"

        /**
         * Hermes Processing Result Intake, Task 2. A processing result is metadata only (no raw
         * source bytes), so this bound is far smaller than [MAX_SUBMISSION_BYTES] -- sized with
         * headroom above [parker.core.interfaces.HermesProcessingResult]'s own worst-case shape
         * (up to 1,000 issues, each up to 4,096-character bounded text fields) without being
         * effectively unbounded.
         */
        const val MAX_PROCESSING_RESULT_REQUEST_BODY_BYTES: Long = 8L * 1024L * 1024L
        const val MAX_ANALYSIS_REQUEST_BODY_BYTES: Long = 64L * 1024L

        /**
         * Mirrors `OwnerEvidenceHttpServer.MAX_PART_BYTES` -- the same 64 MiB ingress bound
         * Section 8's own text cites ("the 64 MiB `MAX_SOURCE_BYTES` bound already present for
         * owner-local ingress"). No larger allowance is invented for the Agent Gateway.
         */
        const val MAX_SUBMISSION_BYTES: Long = 64L * 1024L * 1024L
    }
}

// ---- Hermes Processing Result Intake, Task 2: request-body parsing -----------------------------
//
// Domain/transport separation: everything below only ever parses JSON into plain Kotlin values
// (Map/List/String/Long) and then maps those into Task 1's own, already-fully-validated
// parker.core.interfaces.HermesProcessingResult -- it never constructs a partially-valid domain
// value, and every [HermesProcessingResult]/[HermesProcessingIssue]/[HermesProcessingFailure]
// invariant is enforced exactly once, by those types' own `init` blocks, not duplicated here.
// [JsonParseException] is [OwnerEvidenceHttpServer.kt]'s own existing, `internal`-visible type,
// reused unchanged (same module, same package) rather than declared a second time.

private const val MAX_HERMES_JSON_NESTING_DEPTH = 10

/**
 * The smallest generic JSON value reader Task 2's own request shape needs -- objects, arrays,
 * strings, bounded non-negative integers (page numbers, character offsets), and finite doubles
 * for provider confidence. Deliberately not a reuse of [OwnerEvidenceHttpServer.kt]'s own
 * `SimpleJsonReader`
 * -- that class is `private` to its own file (this repository's own established convention that
 * each HTTP server file owns its own private JSON helpers, exactly as this file's own JSON
 * *writer* below already mirrors that file's writer, per its own KDoc).
 */
private class AgentGatewayJsonReader(private val text: String) {
    private var pos = 0
    private var depth = 0

    fun parseRootValue(): Any {
        val value = parseValue()
        skipWhitespace()
        if (pos != text.length) throw JsonParseException("unexpected trailing content after JSON value at position $pos")
        return value
    }

    private fun parseValue(): Any {
        skipWhitespace()
        if (pos >= text.length) throw JsonParseException("unexpected end of JSON")
        return when (val c = text[pos]) {
            '{' -> parseObject()
            '[' -> parseArray()
            '"' -> parseString()
            else -> if (c == '-' || c.isDigit()) parseNumber() else throw JsonParseException("unexpected token at position $pos")
        }
    }

    private fun enterNestedStructure() {
        depth++
        if (depth > MAX_HERMES_JSON_NESTING_DEPTH) {
            throw JsonParseException("JSON nesting exceeds the maximum permitted depth of $MAX_HERMES_JSON_NESTING_DEPTH")
        }
    }

    private fun parseObject(): Map<String, Any> {
        enterNestedStructure()
        try {
            expect('{')
            val map = LinkedHashMap<String, Any>()
            skipWhitespace()
            if (peek() == '}') { pos++; return map }
            while (true) {
                skipWhitespace()
                val key = parseString()
                if (map.containsKey(key)) throw JsonParseException("duplicate key '$key' in object")
                skipWhitespace()
                expect(':')
                map[key] = parseValue()
                skipWhitespace()
                when (peek()) {
                    ',' -> pos++
                    '}' -> { pos++; return map }
                    else -> throw JsonParseException("expected ',' or '}' in object")
                }
            }
        } finally {
            depth--
        }
    }

    private fun parseArray(): List<Any> {
        enterNestedStructure()
        try {
            expect('[')
            val list = mutableListOf<Any>()
            skipWhitespace()
            if (peek() == ']') { pos++; return list }
            while (true) {
                list += parseValue()
                skipWhitespace()
                when (peek()) {
                    ',' -> pos++
                    ']' -> { pos++; return list }
                    else -> throw JsonParseException("expected ',' or ']' in array")
                }
            }
        } finally {
            depth--
        }
    }

    private fun parseString(): String {
        expect('"')
        val sb = StringBuilder()
        while (true) {
            if (pos >= text.length) throw JsonParseException("unterminated string")
            val c = text[pos]
            pos++
            when {
                c == '"' -> return sb.toString()
                c == '\\' -> {
                    if (pos >= text.length) throw JsonParseException("unterminated escape sequence")
                    val esc = text[pos]
                    pos++
                    when (esc) {
                        '"' -> sb.append('"')
                        '\\' -> sb.append('\\')
                        '/' -> sb.append('/')
                        'n' -> sb.append('\n')
                        'r' -> sb.append('\r')
                        't' -> sb.append('\t')
                        'b' -> sb.append('\b')
                        'u' -> {
                            if (pos + 4 > text.length) throw JsonParseException("truncated unicode escape")
                            val hex = text.substring(pos, pos + 4)
                            val code = hex.toIntOrNull(16) ?: throw JsonParseException("invalid unicode escape '\\u$hex'")
                            sb.append(code.toChar())
                            pos += 4
                        }
                        else -> throw JsonParseException("invalid escape character '\\$esc'")
                    }
                }
                else -> sb.append(c)
            }
        }
    }

    /** Parses integral values as Long and finite fractional values as Double. */
    private fun parseNumber(): Any {
        val start = pos
        if (peek() == '-') pos++
        if (pos >= text.length || !text[pos].isDigit()) throw JsonParseException("invalid number at position $start")
        while (pos < text.length && text[pos].isDigit()) pos++
        if (pos < text.length && (text[pos] == '.' || text[pos] == 'e' || text[pos] == 'E')) {
            if (text[pos] == '.') {
                pos++
                while (pos < text.length && text[pos].isDigit()) pos++
            }
            if (pos < text.length && (text[pos] == 'e' || text[pos] == 'E')) {
                pos++
                if (pos < text.length && (text[pos] == '+' || text[pos] == '-')) pos++
                if (pos >= text.length || !text[pos].isDigit()) throw JsonParseException("invalid number at position $start")
                while (pos < text.length && text[pos].isDigit()) pos++
            }
            val value = text.substring(start, pos).toDoubleOrNull()
                ?: throw JsonParseException("number out of range at position $start")
            if (!value.isFinite()) throw JsonParseException("non-finite number at position $start")
            return value
        }
        return text.substring(start, pos).toLongOrNull() ?: throw JsonParseException("number out of range at position $start")
    }

    private fun peek(): Char { skipWhitespace(); return if (pos < text.length) text[pos] else ' ' }
    private fun expect(c: Char) { skipWhitespace(); if (pos >= text.length || text[pos] != c) throw JsonParseException("expected '$c' at position $pos"); pos++ }
    private fun skipWhitespace() { while (pos < text.length && text[pos].isWhitespace()) pos++ }
}

private val HERMES_PROCESSING_RESULT_REQUEST_FIELDS =
    setOf("sourceSha256", "status", "methods", "proposedEvidenceArtifactId", "issues", "failure", "reviewConfidenceThreshold", "processingCompleteness", "processingWarnings")
private val HERMES_PROCESSING_ISSUE_FIELDS =
    setOf("kind", "explanation", "location", "hermesInterpretation", "transcriptionFidelity", "observedConfidence")
private val HERMES_PROCESSING_ISSUE_LOCATION_FIELDS =
    setOf("pageNumber", "startOffsetInclusive", "endOffsetExclusive", "regionDescription")
private val HERMES_PROCESSING_FAILURE_FIELDS = setOf("kind", "detail")

private val ANALYSIS_REQUEST_FIELDS = setOf("question", "analysisType", "scope")
private val ANALYSIS_SCOPE_FIELDS = setOf("evidenceArtifactIds", "derivativeGenerationIds")

private fun parseAnalysisRequest(bodyBytes: ByteArray): parker.core.interfaces.AnalysisRequest {
    val root = AgentGatewayJsonReader(bodyBytes.toString(StandardCharsets.UTF_8)).parseRootValue()
    val obj = requireJsonObject(root)
    requireExactJsonFields(obj, ANALYSIS_REQUEST_FIELDS, "analysis request")
    val question = obj["question"] as? String ?: throw JsonParseException("expected a 'question' string")
    val analysisTypeName = obj["analysisType"] as? String ?: throw JsonParseException("expected an 'analysisType' string")
    val analysisType = try {
        parker.core.interfaces.AnalysisType.valueOf(analysisTypeName)
    } catch (_: IllegalArgumentException) {
        throw JsonParseException("invalid analysisType '$analysisTypeName'")
    }
    val scope = requireJsonObject(obj["scope"])
    requireKeysSubsetOf(scope.keys, ANALYSIS_SCOPE_FIELDS, "analysis scope")
    if ("evidenceArtifactIds" !in scope.keys) throw JsonParseException("missing analysis scope field(s): [evidenceArtifactIds]")
    val rawIds = scope["evidenceArtifactIds"] as? List<*> ?: throw JsonParseException("expected a 'evidenceArtifactIds' array")
    val ids = rawIds.map { raw ->
        val value = raw as? String ?: throw JsonParseException("expected evidence artifact IDs as strings")
        try { parker.core.interfaces.EvidenceArtifactId(value) }
        catch (e: IllegalArgumentException) { throw JsonParseException(e.message ?: "invalid evidence artifact ID") }
    }
    val derivativeGenerationIds = (scope["derivativeGenerationIds"] as? Map<*, *>)?.map { (rawEvidence, rawGeneration) ->
        val evidence = rawEvidence as? String ?: throw JsonParseException("expected derivative-generation map keys as strings")
        val generation = rawGeneration as? String ?: throw JsonParseException("expected derivative-generation map values as strings")
        try {
            evidence to parker.core.interfaces.DerivativeGenerationId(generation)
        } catch (e: IllegalArgumentException) {
            throw JsonParseException(e.message ?: "invalid derivative generation ID")
        }
    }?.toMap() ?: emptyMap()
    return try {
        parker.core.interfaces.AnalysisRequest(
            requestId = parker.core.interfaces.AnalysisRequestId.new(),
            question = question,
            analysisType = analysisType,
            scope = parker.core.interfaces.AnalysisEvidenceScope(ids, derivativeGenerationIds),
        )
    } catch (e: IllegalArgumentException) {
        throw JsonParseException(e.message ?: "invalid analysis request")
    }
}

/**
 * Parses one `POST /agent/ingestion-batches/{batchId}/processing-results` request body into a
 * fully-validated [parker.core.interfaces.HermesProcessingResult]. [batchId] always comes from
 * the route (never from the body): a `batchId` field in the body -- and, deliberately, any
 * `caseId` field, since Hermes never asserts case membership -- is an unexpected field and is
 * rejected exactly like any other, never silently accepted or cross-checked against the route.
 */
private fun parseHermesProcessingResultRequest(bodyBytes: ByteArray, batchId: String): parker.core.interfaces.HermesProcessingResult {
    val obj = requireJsonObject(AgentGatewayJsonReader(String(bodyBytes, StandardCharsets.UTF_8)).parseRootValue())
    requireKeysSubsetOf(obj.keys, HERMES_PROCESSING_RESULT_REQUEST_FIELDS, "processing result")

    val sourceSha256 = obj["sourceSha256"] as? String ?: throw JsonParseException("expected a 'sourceSha256' string")
    val status = parseEnum<parker.core.interfaces.HermesProcessingStatus>(obj["status"], "status")
    val methods = (obj["methods"] as? List<*> ?: throw JsonParseException("expected a 'methods' array"))
        .map { parseEnum<parker.core.interfaces.HermesProcessingMethod>(it, "methods") }
        .toSet()
    val proposedEvidenceArtifactId = (obj["proposedEvidenceArtifactId"] as? String)?.let { raw ->
        try { EvidenceArtifactId(raw) } catch (e: IllegalArgumentException) { throw JsonParseException(e.message ?: "invalid proposedEvidenceArtifactId") }
    }
    val issues = (obj["issues"] as? List<*> ?: emptyList<Any?>()).map(::parseHermesProcessingIssue)
    val failure = (obj["failure"] as? Map<*, *>)?.let(::parseHermesProcessingFailure)
    val reviewConfidenceThreshold = parseOptionalConfidence(obj["reviewConfidenceThreshold"], "reviewConfidenceThreshold")
    val processingCompleteness = obj["processingCompleteness"]?.let { parseEnum<parker.core.interfaces.HermesProcessingCompleteness>(it, "processingCompleteness") }
    val processingWarnings = (obj["processingWarnings"] as? List<*> ?: emptyList<Any?>()).map {
        it as? String ?: throw JsonParseException("expected processing warnings as strings")
    }

    return try {
        parker.core.interfaces.HermesProcessingResult(
            sourceSha256 = sourceSha256,
            batchId = batchId,
            status = status,
            methods = methods,
            proposedEvidenceArtifactId = proposedEvidenceArtifactId,
            issues = issues,
            failure = failure,
            reviewConfidenceThreshold = reviewConfidenceThreshold,
            processingCompleteness = processingCompleteness,
            processingWarnings = processingWarnings,
        )
    } catch (e: IllegalArgumentException) {
        throw JsonParseException(e.message ?: "invalid processing result")
    }
}

private fun parseHermesProcessingIssue(raw: Any?): parker.core.interfaces.HermesProcessingIssue {
    val obj = requireJsonObject(raw)
    requireKeysSubsetOf(obj.keys, HERMES_PROCESSING_ISSUE_FIELDS, "processing issue")
    val kind = parseEnum<parker.core.interfaces.HermesProcessingIssueKind>(obj["kind"], "issue kind")
    val explanation = obj["explanation"] as? String ?: throw JsonParseException("expected an issue 'explanation' string")
    val location = (obj["location"] as? Map<*, *>)?.let(::parseHermesProcessingIssueLocation)
    val hermesInterpretation = obj["hermesInterpretation"] as? String
    val transcriptionFidelity = obj["transcriptionFidelity"]?.let { parseEnum<parker.core.interfaces.TranscriptionFidelity>(it, "transcriptionFidelity") }
    val observedConfidence = parseOptionalConfidence(obj["observedConfidence"], "observedConfidence")
    return try {
        parker.core.interfaces.HermesProcessingIssue(kind, explanation, location, hermesInterpretation, transcriptionFidelity, observedConfidence)
    } catch (e: IllegalArgumentException) {
        throw JsonParseException(e.message ?: "invalid processing issue")
    }
}

private fun parseOptionalConfidence(raw: Any?, field: String): Double? = when (raw) {
    null -> null
    is Long -> raw.toDouble()
    is Double -> raw
    else -> throw JsonParseException("expected numeric '$field'")
}

private fun parseHermesProcessingIssueLocation(obj: Map<*, *>): parker.core.interfaces.HermesProcessingIssueLocation.DocumentPage {
    requireKeysSubsetOf(obj.keys, HERMES_PROCESSING_ISSUE_LOCATION_FIELDS, "issue location")
    val pageNumber = (obj["pageNumber"] as? Long)?.let(Long::toInt) ?: throw JsonParseException("expected a numeric 'pageNumber'")
    val startOffsetInclusive = (obj["startOffsetInclusive"] as? Long)?.let(Long::toInt)
    val endOffsetExclusive = (obj["endOffsetExclusive"] as? Long)?.let(Long::toInt)
    val regionDescription = obj["regionDescription"] as? String
    return try {
        parker.core.interfaces.HermesProcessingIssueLocation.DocumentPage(pageNumber, startOffsetInclusive, endOffsetExclusive, regionDescription)
    } catch (e: IllegalArgumentException) {
        throw JsonParseException(e.message ?: "invalid issue location")
    }
}

private fun parseHermesProcessingFailure(obj: Map<*, *>): parker.core.interfaces.HermesProcessingFailure {
    requireKeysSubsetOf(obj.keys, HERMES_PROCESSING_FAILURE_FIELDS, "processing failure")
    val kind = parseEnum<parker.core.interfaces.HermesProcessingFailureKind>(obj["kind"], "failure kind")
    val detail = obj["detail"] as? String
    return try {
        parker.core.interfaces.HermesProcessingFailure(kind, detail)
    } catch (e: IllegalArgumentException) {
        throw JsonParseException(e.message ?: "invalid processing failure")
    }
}

private fun requireJsonObject(raw: Any?): Map<*, *> = raw as? Map<*, *> ?: throw JsonParseException("expected a JSON object")

private fun requireKeysSubsetOf(keys: Set<*>, allowed: Set<String>, label: String) {
    val unexpected = keys.filterNot { it in allowed }
    if (unexpected.isNotEmpty()) throw JsonParseException("unexpected $label field(s): $unexpected")
}

private fun requireExactJsonFields(obj: Map<*, *>, expected: Set<String>, label: String) {
    requireKeysSubsetOf(obj.keys, expected, label)
    val missing = expected - obj.keys.filterIsInstance<String>().toSet()
    if (missing.isNotEmpty()) throw JsonParseException("missing $label field(s): $missing")
}

private inline fun <reified T : Enum<T>> parseEnum(raw: Any?, fieldName: String): T {
    val name = raw as? String ?: throw JsonParseException("expected a '$fieldName' string")
    return try { enumValueOf<T>(name) } catch (_: IllegalArgumentException) { throw JsonParseException("invalid $fieldName '$name'") }
}
