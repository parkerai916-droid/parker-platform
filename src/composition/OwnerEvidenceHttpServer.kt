package parker.composition

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import com.sun.net.httpserver.HttpServer
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.concurrent.Executors
import kotlinx.coroutines.runBlocking
import parker.core.interfaces.DerivativeContentStorageException
import parker.core.interfaces.DerivativeGenerationId
import parker.core.interfaces.DerivativeGenerationStorageException
import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.EvidenceGenerationSelection
import parker.core.interfaces.PendingAnalysisId
import parker.core.interfaces.SavedAnalysisId
import parker.core.interfaces.SavedAnalysisStorageException
import parker.ui.EvidenceImportOutcome
import parker.ui.OwnerDerivativeProducerSummary
import parker.ui.OwnerDocumentAnalysisOutcome
import parker.ui.OwnerEvidenceOperations
import parker.ui.OwnerRetrieveSavedAnalysisOutcome
import parker.ui.OwnerSaveAnalysisOutcome
import parker.ui.OwnerSavedAnalysisPresentation
import parker.ui.OwnerSavedAnalysisSummary
import parker.ui.OwnerTierAContent
import parker.ui.OwnerTierBOcrContent
import parker.ui.TierAContentRetrievalResult
import parker.ui.TierAProcessingOutcome
import parker.ui.TierBDurableProcessingOutcome
import parker.ui.TierBOcrContentRetrievalResult
import parker.ui.TierBProcessingOutcome
import parker.ui.EnhancedTranscriptionOutcome
import parker.ui.EnhancedTranscriptionReadiness
import parker.ui.OwnerAcquisitionDecisionView
import parker.ui.OwnerAcquisitionExecutionView
import parker.ui.OwnerAcquisitionSourceFacts
import parker.ui.OwnerAcquisitionCapabilityView
import parker.core.runtime.FidelityFirstAcceptanceOutcome
import parker.core.runtime.ORDINARY_REGION_CAPABILITY_ID
import parker.core.runtime.OrdinaryRegionCapabilityPromotionOutcome
import parker.core.runtime.OrdinaryRegionCapabilityPromotionRequest
import parker.core.runtime.OrdinaryRegionCapabilityStatus

/**
 * Owner LAN Evidence Upload. Pure HTTP transport for the exact same
 * [OwnerEvidenceOperations] the Compose Desktop owner UI already drives
 * (`ParkerEvidencePanel.kt` / `OwnerEvidenceUiController.kt`) -- this class
 * implements no Tier A/B logic, no evidence-custody write, and no OCR of
 * its own. It exists solely so a browser on a Windows laptop, on the same
 * trusted LAN as the headless Ubuntu Parker server, can select files, get
 * them into Evidence Custody, and drive processing -- without X11, a
 * desktop environment, Compose Desktop, VNC, or RDP anywhere on the server.
 *
 * Built on the JDK's own bundled `com.sun.net.httpserver.HttpServer` --
 * Parker has no production HTTP framework/server anywhere else in the
 * dependency graph (confirmed by inspection of both `build.gradle.kts`
 * files); this package is the only place `com.sun.net.httpserver` already
 * appears, and only in test fixtures faking a model-inference endpoint.
 * Reusing the JDK's own bundled server rather than adding Ktor/Spring/etc.
 * mirrors that precedent and needs no new Gradle dependency.
 *
 * **Single-owner bearer token.** Every owner-evidence API request (except
 * the static shell page itself, which carries no evidence data) must present
 * `Authorization: Bearer <token>` matching [token] exactly, compared via
 * [constantTimeEquals] so a wrong guess cannot be narrowed by response
 * timing. There is no per-request client-supplied principal anywhere in
 * this class -- every call into [operations] resolves the owner identity
 * [OwnerUiEvidenceRuntimeAdapter] was itself already constructed with, the
 * same structural guarantee the Compose Desktop UI already has.
 *
 * **Never a second evidence-ingress mechanism.** This class never writes
 * Evidence Custodian storage directly and never calls Tier A/B logic
 * itself -- it only ever streams an uploaded part to a bounded, randomly
 * named server temp file, then calls [OwnerEvidenceOperations.importFile]
 * with that temp path, exactly as [operations] already requires.
 */
class OwnerEvidenceHttpServer(
    private val bindAddress: String,
    private val port: Int,
    private val authentication: OwnerUiAuthentication,
    private val operations: OwnerEvidenceOperations,
    private val logger: ParkerLogger,
    private val invokeFidelityFirstAcceptance: suspend (String) -> FidelityFirstAcceptanceOutcome = {
        FidelityFirstAcceptanceOutcome.Blocked("ACCEPTANCE_LANE_NOT_CONFIGURED")
    },
    private val createOrdinaryRegionCapabilityAcceptance: (OrdinaryRegionCapabilityPromotionRequest) -> OrdinaryRegionCapabilityPromotionOutcome = {
        OrdinaryRegionCapabilityPromotionOutcome.Blocked("ACCEPTANCE_LANE_NOT_CONFIGURED")
    },
    private val evaluateOrdinaryRegionCapability: () -> OrdinaryRegionCapabilityStatus? = { null },
    private val prepareCorrectedEvidence: suspend (EvidenceArtifactId, String, Int) -> parker.core.runtime.GovernedCorrectedPreparationOutcome =
        { _, _, _ -> parker.core.runtime.GovernedCorrectedPreparationOutcome.Rejected("PREPARATION_LANE_NOT_CONFIGURED") },
    private val continuePostEgress: suspend (EvidenceArtifactId, String, String, String) -> parker.core.runtime.OrdinaryRegionOwnerResult =
        { _, _, _, _ -> parker.core.runtime.OrdinaryRegionOwnerResult(parker.core.runtime.OrdinaryRegionDisposition.VALIDATION_FAILED,"CONTINUATION_LANE_NOT_CONFIGURED") },
) {
    private var server: HttpServer? = null
    private var executor: java.util.concurrent.ExecutorService? = null
    private val uploadTempDirectory: Path by lazy { Files.createTempDirectory("parker-owner-http-upload") }

    /** The actual bound TCP port -- equal to [port] unless [port] was `0` (ephemeral, test-only use). */
    val boundPort: Int
        get() = server?.address?.port ?: port

    /** Idempotent-in-intent: starts the listener; throws if it cannot bind. */
    fun start() {
        val httpServer = HttpServer.create(InetSocketAddress(bindAddress, port), 0)
        val fixedThreadPool = Executors.newFixedThreadPool(8)
        httpServer.executor = fixedThreadPool
        httpServer.createContext("/", RootPageHandler())
        httpServer.createContext("/owner/pair", PairingHandler())
        httpServer.createContext("/owner/logout", LogoutHandler())
        httpServer.createContext("/owner/evidence", EvidenceHandler())
        httpServer.createContext("/owner/analyse", AnalyseHandler())
        httpServer.createContext("/owner/saved-analyses", SavedAnalysisHandler())
        httpServer.createContext("/owner/admin/region-capability-acceptance", RegionCapabilityAcceptanceHandler())
        httpServer.createContext("/owner/admin/corrected-preparation", CorrectedPreparationHandler())
        httpServer.createContext("/owner/admin/region-transcription-continuation", RegionTranscriptionContinuationHandler())
        httpServer.start()
        server = httpServer
        executor = fixedThreadPool
        logger.info("Owner LAN Evidence Upload HTTP server listening on $bindAddress:${httpServer.address.port}")
    }

    /**
     * Stops the listener (a short grace period lets in-flight requests
     * finish) and cleans up temp state. `HttpServer.stop` does not shut
     * down a custom executor supplied via `setExecutor` -- its own javadoc
     * says so explicitly -- so this shuts [executor] down itself;
     * otherwise every [start] call leaks eight non-daemon threads forever
     * (a real leak this class hit in its own test suite: many short-lived
     * servers in one JVM run, each leaking its pool, eventually starved
     * native/direct memory).
     */
    fun stop() {
        server?.stop(1)
        server = null
        executor?.shutdownNow()
        executor = null
        runCatching {
            if (Files.isDirectory(uploadTempDirectory)) {
                Files.walk(uploadTempDirectory).use { paths ->
                    paths.sorted(Comparator.reverseOrder()).forEach { runCatching { Files.deleteIfExists(it) } }
                }
            }
        }
        logger.info("Owner LAN Evidence Upload HTTP server stopped")
    }

    // ---- authentication -----------------------------------------------------------------

    private fun isAuthorised(exchange: HttpExchange): Boolean {
        val cookies = cookies(exchange)
        if (authentication.authenticate(cookies[SESSION_COOKIE]) != null) return true
        val session = authentication.establishSession(cookies[DEVICE_ID_COOKIE], cookies[DEVICE_CREDENTIAL_COOKIE]) ?: return false
        setCookie(exchange, SESSION_COOKIE, session, 8 * 60 * 60)
        return authentication.authenticate(session) != null
    }

    private fun cookies(exchange: HttpExchange): Map<String, String> = exchange.requestHeaders["Cookie"].orEmpty()
        .flatMap { it.split(';') }.mapNotNull { part -> part.trim().split('=', limit=2).takeIf { it.size == 2 }?.let { it[0] to it[1] } }.toMap()
    private fun setCookie(exchange: HttpExchange, name: String, value: String, maxAge: Int) {
        exchange.responseHeaders.add("Set-Cookie", "$name=$value; Path=/; Max-Age=$maxAge; HttpOnly; SameSite=Strict")
    }

    private fun rejectUnauthorised(exchange: HttpExchange) {
        // Drain and discard the body without ever parsing it, per Phase 3's "reject missing/invalid
        // credentials before accepting file bodies" -- no part is ever streamed to a temp file for
        // an unauthorised request.
        runCatching { exchange.requestBody.use { it.readBytes() } }
        writeJson(exchange, 401, jsonObject("error" to "unauthorised"))
    }

    private inner class RegionCapabilityAcceptanceHandler : HttpHandler {
        override fun handle(exchange: HttpExchange) {
            try {
                if (!isAuthorised(exchange)) { rejectUnauthorised(exchange); return }
                if (exchange.requestURI.path != "/owner/admin/region-capability-acceptance") {
                    writeJson(exchange, 404, jsonObject("error" to "not found")); return
                }
                if (exchange.requestMethod == "GET") {
                    val status = evaluateOrdinaryRegionCapability()
                    if (status == null) writeJson(exchange, 409, jsonObject("status" to "NOT_CONFIGURED"))
                    else writeJson(exchange, 200, capabilityStatusJson(status))
                    return
                }
                if (exchange.requestMethod != "POST") { writeJson(exchange, 404, jsonObject("error" to "not found")); return }
                val body = try { readBounded(exchange.requestBody, MAX_PROMOTION_REQUEST_BODY_BYTES) }
                catch (_: RequestBodyTooLargeException) { writeJson(exchange, 413, jsonObject("error" to "request body too large")); return }
                val request = try { parseCapabilityPromotionRequest(body) }
                catch (_: Exception) { writeJson(exchange, 400, jsonObject("error" to "invalid promotion request")); return }
                when (val outcome = createOrdinaryRegionCapabilityAcceptance(request)) {
                    is OrdinaryRegionCapabilityPromotionOutcome.Created -> writeJson(exchange, 201, promotionJson("CREATED", outcome.record))
                    is OrdinaryRegionCapabilityPromotionOutcome.Existing -> writeJson(exchange, 200, promotionJson("EXISTING", outcome.record))
                    is OrdinaryRegionCapabilityPromotionOutcome.V8Created -> writeJson(exchange, 201, promotionJsonV8("CREATED", outcome.record))
                    is OrdinaryRegionCapabilityPromotionOutcome.V8Existing -> writeJson(exchange, 200, promotionJsonV8("EXISTING", outcome.record))
                    is OrdinaryRegionCapabilityPromotionOutcome.Blocked -> writeJson(exchange, 409,
                        jsonObject("status" to "BLOCKED", "reason" to outcome.reason))
                }
            } catch (e: Exception) {
                logger.error("Owner HTTP: capability promotion failed safely", e)
                runCatching { writeJson(exchange, 500, jsonObject("error" to "internal error")) }
            } finally { exchange.close() }
        }
        private fun promotionJson(status: String, record: parker.core.runtime.OrdinaryRegionCapabilityAcceptanceRecord) = jsonObject(
            "status" to status, "recordId" to record.recordId, "recordDigest" to record.recordId,
            "capabilityDigest" to record.capabilityDigest, "promotingBuildCommit" to record.promotingBuildCommit,
        )
        private fun promotionJsonV8(status:String,record:parker.core.runtime.OrdinaryRequestRegionV8CapabilityAcceptanceRecord)=jsonObject(
            "status" to status,"recordId" to record.recordId,"recordDigest" to record.recordId,
            "capabilityId" to record.capabilityId,"capabilityDigest" to record.capabilityDigest,
            "promotingBuildCommit" to record.implementationCommit,"acceptedBy" to record.acceptedBy,"acceptedAt" to record.acceptedAt.toString())
        private fun capabilityStatusJson(status: OrdinaryRegionCapabilityStatus) = jsonObject(
            "capabilityId" to status.capabilityId, "provider" to status.provider,
            "operation" to status.endpointOperation, "model" to status.model,
            "adapterId" to status.adapterId, "adapterVersion" to status.adapterVersion,
            "profile" to status.providerProfile, "wireVersion" to status.wireVersion,
            "mediaType" to status.mediaType, "maximumRegions" to status.maximumRegions,
            "aggregateRequestBodyMaximumBytes" to status.aggregateRequestBodyMaximumBytes,
            "batching" to status.batching, "disposition" to status.disposition.name,
            "runtimeEmbeddedBuildCommit" to status.runtimeEmbeddedBuildCommit,
            "acceptedPromotingBuildCommit" to status.acceptedPromotingBuildCommit,
        )
    }

    private inner class CorrectedPreparationHandler : HttpHandler {
        override fun handle(exchange: HttpExchange) {
            try {
                if (!isAuthorised(exchange)) { rejectUnauthorised(exchange); return }
                if (exchange.requestMethod != "POST") { writeJson(exchange, 404, jsonObject("error" to "not found")); return }
                val prefix = "/owner/admin/corrected-preparation/"
                val rawId = exchange.requestURI.path.takeIf { it.startsWith(prefix) }?.removePrefix(prefix)
                if (rawId.isNullOrBlank() || '/' in rawId) { writeJson(exchange, 404, jsonObject("error" to "not found")); return }
                val body = try { readBounded(exchange.requestBody, MAX_PROMOTION_REQUEST_BODY_BYTES) }
                catch (_: RequestBodyTooLargeException) { writeJson(exchange, 413, jsonObject("error" to "request body too large")); return }
                val (profile, version) = try { parseCorrectedPreparationRequest(body) }
                catch (_: Exception) { writeJson(exchange, 400, jsonObject("error" to "invalid preparation request")); return }
                when (val outcome = runBlocking { prepareCorrectedEvidence(EvidenceArtifactId(rawId), profile, version) }) {
                    is parker.core.runtime.GovernedCorrectedPreparationOutcome.Rejected ->
                        writeJson(exchange, 409, jsonObject("status" to "REJECTED", "reason" to outcome.reason))
                    is parker.core.runtime.GovernedCorrectedPreparationOutcome.Prepared -> {
                        val result = outcome.result
                        writeJson(exchange, 200, jsonObject(
                            "status" to "PREPARED", "evidenceId" to result.evidenceId,
                            "profileId" to result.profileId, "profileVersion" to result.profileVersion,
                            "preparationIdentity" to result.preparationIdentity, "regionSetDigest" to result.regionSetDigest,
                            "pageCount" to result.pages.size, "preparationRegionCount" to result.pages.size,
                            "requestRegionCount" to result.requestRegionCount, "requestDigest" to result.requestDigest,
                            "requestBodyDigest" to result.requestBodyDigest, "requestBodyByteLength" to result.requestBodyByteLength,
                            "aggregatePngByteLength" to result.aggregatePngByteLength,
                            "aggregateBase64Characters" to result.aggregateBase64Characters,
                            "readbackVerified" to result.readbackVerified,
                            "pages" to jsonArray(result.pages.map { page -> jsonObject(
                                "page" to page.pageNumber, "authoritativeRepresentationId" to page.authoritativeRepresentationId,
                                "authoritativePixelDigest" to page.authoritativePixelDigest, "preparationId" to page.preparationId,
                                "preparationRegionId" to page.preparationRegionId, "transportSha256" to page.transportSha256,
                                "transportByteLength" to page.transportByteLength, "width" to page.width, "height" to page.height,
                                "orderState" to page.orderState,
                            ) }),
                        ))
                    }
                }
            } catch (e: Exception) {
                logger.error("Owner HTTP: corrected preparation failed safely", e)
                runCatching { writeJson(exchange, 500, jsonObject("error" to "internal error")) }
            } finally { exchange.close() }
        }
    }

    private inner class RegionTranscriptionContinuationHandler : HttpHandler {
        override fun handle(exchange: HttpExchange) {
            try {
                if (!isAuthorised(exchange)) { rejectUnauthorised(exchange); return }
                if (exchange.requestURI.path != "/owner/admin/region-transcription-continuation" || exchange.requestMethod != "POST") {
                    writeJson(exchange, 404, jsonObject("error" to "not found")); return
                }
                val body = try { readBounded(exchange.requestBody, MAX_PROMOTION_REQUEST_BODY_BYTES) }
                catch (_: RequestBodyTooLargeException) { writeJson(exchange, 413, jsonObject("error" to "request body too large")); return }
                val request = try { parsePostEgressContinuationRequest(body) }
                catch (_: Exception) { writeJson(exchange, 400, jsonObject("error" to "invalid continuation request")); return }
                val result = runBlocking { continuePostEgress(request.evidenceId,request.authorizationId,request.executionId,request.providerStateId) }
                writeJson(exchange,if(result.disposition==parker.core.runtime.OrdinaryRegionDisposition.ADMITTED)200 else 409,jsonObject(
                    "status" to result.disposition.name,"detail" to result.detail,
                    "evidenceArtifactId" to request.evidenceId.value,"derivativeGenerationId" to result.derivativeGenerationId,
                    "providerInvoked" to false,
                ))
            } catch (e: Exception) {
                logger.error("Owner HTTP: post-egress continuation failed safely",e)
                runCatching { writeJson(exchange,500,jsonObject("error" to "internal error")) }
            } finally { exchange.close() }
        }
    }

    // ---- static owner page ---------------------------------------------------------------

    private inner class RootPageHandler : HttpHandler {
        override fun handle(exchange: HttpExchange) {
            try {
                if (exchange.requestURI.path != "/" || exchange.requestMethod != "GET") {
                    writeJson(exchange, 404, jsonObject("error" to "not found"))
                    return
                }
                val bytes = (if (isAuthorised(exchange)) OWNER_EVIDENCE_PAGE_HTML else OWNER_PAIRING_PAGE_HTML)
                    .toByteArray(StandardCharsets.UTF_8)
                exchange.responseHeaders.set("Content-Type", "text/html; charset=utf-8")
                exchange.sendResponseHeaders(200, bytes.size.toLong())
                exchange.responseBody.use { it.write(bytes) }
            } catch (e: IOException) {
                logger.warn("Owner HTTP: failed to serve root page", e)
            } finally {
                exchange.close()
            }
        }
    }

    private inner class PairingHandler : HttpHandler {
        override fun handle(exchange: HttpExchange) {
            try {
                if (exchange.requestMethod != "POST") { writeJson(exchange, 404, jsonObject("error" to "not found")); return }
                val code = String(readBounded(exchange.requestBody, 1024), StandardCharsets.UTF_8).trim()
                val paired = authentication.pair(code)
                if (paired == null) { writeJson(exchange, 401, jsonObject("error" to "pairing denied")); return }
                setCookie(exchange, DEVICE_ID_COOKIE, paired.deviceId, 365 * 24 * 60 * 60)
                setCookie(exchange, DEVICE_CREDENTIAL_COOKIE, paired.deviceCredential, 365 * 24 * 60 * 60)
                setCookie(exchange, SESSION_COOKIE, paired.sessionId, 8 * 60 * 60)
                writeJson(exchange, 200, jsonObject("status" to "PAIRED", "deviceId" to paired.deviceId))
            } catch (_: Exception) { runCatching { writeJson(exchange, 401, jsonObject("error" to "pairing denied")) } }
            finally { exchange.close() }
        }
    }

    private inner class LogoutHandler : HttpHandler {
        override fun handle(exchange: HttpExchange) {
            try {
                if (exchange.requestMethod != "POST" || !isAuthorised(exchange)) { rejectUnauthorised(exchange); return }
                authentication.logout(cookies(exchange)[SESSION_COOKIE])
                setCookie(exchange, SESSION_COOKIE, "", 0)
                writeJson(exchange, 200, jsonObject("status" to "LOGGED_OUT"))
            } finally { exchange.close() }
        }
    }

    // ---- /owner/evidence, /owner/evidence/{id}/process, /owner/evidence/{id}/ocr ----------

    private inner class EvidenceHandler : HttpHandler {
        override fun handle(exchange: HttpExchange) {
            try {
                if (!isAuthorised(exchange)) {
                    rejectUnauthorised(exchange)
                    return
                }
                val path = exchange.requestURI.path
                val method = exchange.requestMethod
                val segments = path.removePrefix("/owner/evidence").trim('/').split('/').filter { it.isNotEmpty() }

                when {
                    segments.isEmpty() && method == "GET" -> handleEvidenceList(exchange)
                    segments.isEmpty() && method == "POST" -> handleUpload(exchange)
                    segments.size == 2 && segments[1] == "process" && method == "POST" ->
                        handleProcess(exchange, segments[0])
                    segments.size == 2 && segments[1] == "ocr" && method == "POST" ->
                        handleOcr(exchange, segments[0])
                    segments.size == 2 && segments[1] == "ocr-durable" && method == "POST" ->
                        handleOcrDurable(exchange, segments[0])
                    segments.size == 2 && segments[1] == "acquisition" && method == "GET" ->
                        handleAcquisitionDecision(exchange, segments[0])
                    segments.size == 2 && segments[1] == "authorize-region-transcription" && method == "POST" ->
                        handleOrdinaryRegionAuthorization(exchange, segments[0])
                    segments.size == 2 && segments[1] == "execute-region-transcription" && method == "POST" ->
                        handleOrdinaryRegionExecution(exchange, segments[0])
                    segments.size == 2 && segments[1] == "acquire" && method == "POST" ->
                        handleGovernedAcquisition(exchange, segments[0])
                    segments.size == 2 && segments[1] == "transcribe-external" && method == "POST" ->
                        handleExternalTranscription(exchange, segments[0])
                    segments.size == 1 && segments[0] == "transcription-readiness" && method == "GET" ->
                        handleExternalReadiness(exchange)
                    segments.size == 2 && segments[0] == "acceptance-executions" && method == "POST" ->
                        handleAcceptanceExecution(exchange, segments[1])
                    segments.size == 3 && segments[1] == "content" && method == "GET" ->
                        handleRetrieveContent(exchange, segments[0], segments[2])
                    segments.size == 3 && segments[1] == "ocr-content" && method == "GET" ->
                        handleRetrieveOcrContent(exchange, segments[0], segments[2])
                    else -> {
                        runCatching { exchange.requestBody.use { it.readBytes() } }
                        writeJson(exchange, 404, jsonObject("error" to "not found"))
                    }
                }
            } catch (e: Exception) {
                logger.error("Owner HTTP: unexpected failure handling ${exchange.requestURI}", e)
                runCatching { writeJson(exchange, 500, jsonObject("error" to "internal error")) }
            } finally {
                exchange.close()
            }
        }

        private fun handleEvidenceList(exchange: HttpExchange) {
            runCatching { exchange.requestBody.use { it.readBytes() } }
            val evidence = runBlocking { operations.listRegisteredEvidence() }
            writeJson(exchange, 200, jsonObject("evidence" to jsonArray(evidence.map { item ->
                jsonObject(
                    "evidenceArtifactId" to item.evidenceArtifactId,
                    "sha256" to item.sha256,
                    "byteLength" to item.byteLength,
                    "mediaType" to item.mediaType,
                    "originalFileName" to item.originalFileName,
                    "registeredAt" to item.registeredAt,
                )
            })))
        }

        private fun handleUpload(exchange: HttpExchange) {
            val contentType = exchange.requestHeaders.getFirst("Content-Type")
            val boundary = contentType?.let { extractBoundary(it) }
            if (boundary == null) {
                runCatching { exchange.requestBody.use { it.readBytes() } }
                writeJson(exchange, 400, jsonObject("error" to "expected multipart/form-data with a boundary"))
                return
            }

            val parser = BoundedMultipartParser(boundary, MAX_PART_BYTES, uploadTempDirectory, MAX_FILES_PER_REQUEST)
            val outcome = try {
                parser.parse(exchange.requestBody)
            } catch (e: MultipartParseException) {
                writeJson(exchange, 400, jsonObject("error" to (e.message ?: "malformed multipart body")))
                return
            }
            val fileParts = outcome.fileParts

            try {
                val importedResults = fileParts.map { part ->
                    try {
                        val importOutcome = runBlocking {
                            operations.importUploadedFile(part.tempPath.toString(), part.declaredContentType, part.originalFileName)
                        }
                        when (importOutcome) {
                            is EvidenceImportOutcome.Imported -> jsonObject(
                                "originalFileName" to part.originalFileName,
                                "status" to "IMPORTED",
                                "evidenceArtifactId" to importOutcome.evidenceArtifactId.value,
                            )
                            is EvidenceImportOutcome.Rejected -> jsonObject(
                                "originalFileName" to part.originalFileName,
                                "status" to "IMPORT_FAILED",
                                "message" to importOutcome.reason,
                            )
                            is EvidenceImportOutcome.Failed -> jsonObject(
                                "originalFileName" to part.originalFileName,
                                "status" to "IMPORT_FAILED",
                                "message" to importOutcome.safeMessage,
                            )
                        }
                    } finally {
                        runCatching { Files.deleteIfExists(part.tempPath) }
                    }
                }
                val rejectedResults = outcome.rejectedParts.map { rejected ->
                    jsonObject(
                        "originalFileName" to rejected.originalFileName,
                        "status" to "IMPORT_FAILED",
                        "message" to rejected.reason,
                    )
                }
                writeJson(exchange, 200, JsonArray(importedResults + rejectedResults))
            } finally {
                fileParts.forEach { runCatching { Files.deleteIfExists(it.tempPath) } }
            }
        }

        private fun handleProcess(exchange: HttpExchange, rawId: String) {
            runCatching { exchange.requestBody.use { it.readBytes() } }
            val id = try {
                EvidenceArtifactId(rawId)
            } catch (e: IllegalArgumentException) {
                writeJson(exchange, 400, jsonObject("error" to "invalid evidence artefact id"))
                return
            }
            val outcome = runBlocking { operations.processTierA(id) }
            val body = when (outcome) {
                is TierAProcessingOutcome.Admitted -> jsonObject(
                    "status" to "TIER_A_COMPLETE",
                    "format" to outcome.format,
                    "content" to outcome.content?.let { contentJson(it) },
                    "derivativeGenerationId" to outcome.derivativeGenerationId?.value,
                )
                TierAProcessingOutcome.RequiresTierB -> jsonObject("status" to "REQUIRES_OCR")
                is TierAProcessingOutcome.Unsupported -> jsonObject("status" to "FAILED", "message" to "Unsupported: ${outcome.reason}")
                is TierAProcessingOutcome.IntegrityFailure -> jsonObject(
                    "status" to "FAILED",
                    "message" to "Integrity check failed: ${outcome.reason}",
                )
                is TierAProcessingOutcome.Failed -> jsonObject(
                    "status" to "FAILED",
                    "message" to "Failed (${outcome.stage}): ${outcome.safeMessage}",
                )
            }
            writeJson(exchange, 200, body)
        }

        private fun handleAcquisitionDecision(exchange: HttpExchange, rawId: String) {
            runCatching { exchange.requestBody.use { it.readBytes() } }
            val id = parseEvidenceId(exchange, rawId) ?: return
            writeJson(exchange, 200, acquisitionDecisionJson(runBlocking { operations.governedAcquisitionDecision(id) }))
        }

        private fun handleOrdinaryRegionAuthorization(exchange: HttpExchange, rawId: String) {
            runCatching { exchange.requestBody.use { it.readBytes() } }
            val id = parseEvidenceId(exchange, rawId) ?: return
            val view = runBlocking { operations.authorizeOrdinaryRegionTranscription(id) }
            val status = if (view.status == "UNAVAILABLE") 409 else 200
            writeJson(exchange, status, jsonObject(
                "status" to view.status, "evidenceArtifactId" to view.evidenceArtifactId,
                "provider" to view.provider, "disclosure" to view.disclosure, "detail" to view.detail,
                "authorizationId" to view.authorizationId, "expiresAt" to view.expiresAt,
                "executionStarted" to false,
            ))
        }

        private fun handleOrdinaryRegionExecution(exchange: HttpExchange, rawId: String) {
            val body = try { readBounded(exchange.requestBody, MAX_SAVE_REQUEST_BODY_BYTES) } catch (_: RequestBodyTooLargeException) {
                writeJson(exchange, 413, jsonObject("error" to "request body too large")); return
            }
            if (body.isNotEmpty()) {
                writeJson(exchange, 400, jsonObject("error" to "ordinary execution accepts no browser-supplied fields")); return
            }
            val id = parseEvidenceId(exchange, rawId) ?: return
            val view = runBlocking { operations.executeOrdinaryRegionTranscription(id) }
            val status = if (view.status == "ADMITTED") 200 else 409
            writeJson(exchange, status, jsonObject(
                "status" to view.status, "detail" to view.detail,
                "evidenceArtifactId" to view.evidenceArtifactId,
                "derivativeGenerationId" to view.derivativeGenerationId,
            ))
        }

        private fun handleGovernedAcquisition(exchange: HttpExchange, rawId: String) {
            val id = parseEvidenceId(exchange, rawId) ?: return
            val body = try { readBounded(exchange.requestBody, MAX_SAVE_REQUEST_BODY_BYTES) } catch (_: RequestBodyTooLargeException) {
                writeJson(exchange, 413, jsonObject("error" to "request body too large")); return
            }
            val expected = try { parseExpectedCapabilityId(body) } catch (_: Exception) {
                writeJson(exchange, 400, jsonObject("error" to "invalid acquisition request")); return
            }
            when (val outcome = runBlocking { operations.executeGovernedAcquisition(id, expected) }) {
                is OwnerAcquisitionExecutionView.Admitted -> writeJson(exchange, 200, acquisitionExecutionJson(outcome))
                is OwnerAcquisitionExecutionView.StaleDecision -> writeJson(exchange, 409, jsonObject(
                    "status" to "STALE_DECISION", "currentDecision" to acquisitionDecisionJson(outcome.currentDecision),
                ))
                is OwnerAcquisitionExecutionView.Failed -> writeJson(exchange, 409, jsonObject(
                    "status" to "FAILED", "reason" to outcome.reason,
                    "capability" to outcome.capability?.let(::acquisitionCapabilityJson),
                ))
            }
        }

        private fun parseEvidenceId(exchange: HttpExchange, rawId: String): EvidenceArtifactId? {
            if (!SAFE_ROUTE_ID.matches(rawId)) {
                writeJson(exchange, 400, jsonObject("error" to "invalid evidence artefact id")); return null
            }
            return try { EvidenceArtifactId(rawId) } catch (_: IllegalArgumentException) {
                writeJson(exchange, 400, jsonObject("error" to "invalid evidence artefact id")); null
            }
        }

        private fun handleOcr(exchange: HttpExchange, rawId: String) {
            runCatching { exchange.requestBody.use { it.readBytes() } }
            val id = try {
                EvidenceArtifactId(rawId)
            } catch (e: IllegalArgumentException) {
                writeJson(exchange, 400, jsonObject("error" to "invalid evidence artefact id"))
                return
            }
            val outcome = runBlocking { operations.processTierB(id) }
            val body = when (outcome) {
                is TierBProcessingOutcome.Completed -> jsonObject(
                    "status" to "COMPLETE",
                    "message" to if (outcome.resultCount > 0) "${outcome.resultCount} result(s) produced" else "No recognisable content",
                )
                is TierBProcessingOutcome.NotAuthorised -> jsonObject("status" to "FAILED", "message" to "Not authorised: ${outcome.reason}")
                is TierBProcessingOutcome.Failed -> jsonObject("status" to "FAILED", "message" to outcome.safeMessage)
            }
            writeJson(exchange, 200, body)
        }

        /**
         * Document Ingestion — Derivative Content Persistence and Retrieval.
         * Retrieves an already-persisted Tier A derivative's durable content
         * by known [EvidenceArtifactId] + [DerivativeGenerationId] -- never
         * re-runs Tier A extraction. No arbitrary filesystem path is ever
         * accepted; both identifiers are parsed exactly as
         * [handleProcess]/[handleOcr] already parse [EvidenceArtifactId],
         * rejecting a blank/malformed value before it ever reaches
         * [operations].
         */
        private fun handleRetrieveContent(exchange: HttpExchange, rawEvidenceArtifactId: String, rawDerivativeGenerationId: String) {
            runCatching { exchange.requestBody.use { it.readBytes() } }
            val evidenceArtifactId = try {
                EvidenceArtifactId(rawEvidenceArtifactId)
            } catch (e: IllegalArgumentException) {
                writeJson(exchange, 400, jsonObject("error" to "invalid evidence artefact id"))
                return
            }
            val derivativeGenerationId = try {
                DerivativeGenerationId(rawDerivativeGenerationId)
            } catch (e: IllegalArgumentException) {
                writeJson(exchange, 400, jsonObject("error" to "invalid derivative generation id"))
                return
            }
            // A caller-supplied derivativeGenerationId shaped in a way the durable stores' own
            // safe-identifier discipline rejects (e.g. path-traversal-shaped, a reserved device
            // name) is a malformed request, not an internal fault -- caught here specifically so
            // it never falls through to the outer handler's generic 500/error-log path.
            val outcome = try {
                runBlocking { operations.retrieveTierAExtractedContent(evidenceArtifactId, derivativeGenerationId) }
            } catch (e: DerivativeGenerationStorageException.UnsafeIdentifier) {
                writeJson(exchange, 400, jsonObject("error" to "invalid derivative generation id"))
                return
            } catch (e: DerivativeContentStorageException.UnsafeIdentifier) {
                writeJson(exchange, 400, jsonObject("error" to "invalid derivative generation id"))
                return
            }
            val body = when (outcome) {
                is TierAContentRetrievalResult.Retrieved -> jsonObject(
                    "status" to "RETRIEVED",
                    "content" to contentJson(outcome.content),
                )
                TierAContentRetrievalResult.UnknownGeneration -> jsonObject("status" to "UNKNOWN_GENERATION")
                TierAContentRetrievalResult.SourceMismatch -> jsonObject("status" to "SOURCE_MISMATCH")
                TierAContentRetrievalResult.ContentMissing -> jsonObject("status" to "CONTENT_MISSING")
                is TierAContentRetrievalResult.ContentCorrupt -> jsonObject(
                    "status" to "CONTENT_CORRUPT",
                    "message" to outcome.safeMessage,
                )
                is TierAContentRetrievalResult.UnsupportedRepresentationVersion -> jsonObject(
                    "status" to "UNSUPPORTED_VERSION",
                    "version" to outcome.version,
                )
                is TierAContentRetrievalResult.Failed -> jsonObject("status" to "FAILED", "message" to outcome.safeMessage)
            }
            writeJson(exchange, 200, body)
        }

        /**
         * Document Ingestion — Tier B Durable OCR Derivative Content.
         * Explicit, owner-triggered durable Tier B OCR -- distinct from
         * [handleOcr] (the existing, unchanged transient-only path). On
         * success, durably admits a new `DerivativeGenerationRecord` and
         * subordinate content entry, retrievable after restart.
         */
        private fun handleOcrDurable(exchange: HttpExchange, rawId: String) {
            runCatching { exchange.requestBody.use { it.readBytes() } }
            val id = try {
                EvidenceArtifactId(rawId)
            } catch (e: IllegalArgumentException) {
                writeJson(exchange, 400, jsonObject("error" to "invalid evidence artefact id"))
                return
            }
            val outcome = runBlocking { operations.processTierBDurable(id) }
            val body = when (outcome) {
                is TierBDurableProcessingOutcome.Admitted -> jsonObject(
                    "status" to "TIER_B_DURABLE_COMPLETE",
                    "content" to ocrContentJson(outcome.content),
                    "derivativeGenerationId" to outcome.derivativeGenerationId.value,
                )
                is TierBDurableProcessingOutcome.NotAuthorised -> jsonObject("status" to "FAILED", "message" to "Not authorised: ${outcome.reason}")
                is TierBDurableProcessingOutcome.MandatoryProvenanceUnavailable -> jsonObject(
                    "status" to "FAILED",
                    "message" to "OCR model provenance was not available for this invocation -- durable admission fails closed.",
                )
                is TierBDurableProcessingOutcome.OcrNotAdmissible -> jsonObject("status" to "FAILED", "message" to outcome.reason)
                is TierBDurableProcessingOutcome.IntegrityFailure -> jsonObject("status" to "FAILED", "message" to "Integrity check failed: ${outcome.reason}")
                is TierBDurableProcessingOutcome.Failed -> jsonObject("status" to "FAILED", "message" to "Failed (${outcome.stage}): ${outcome.safeMessage}")
            }
            writeJson(exchange, 200, body)
        }

        private fun handleExternalReadiness(exchange: HttpExchange) {
            runCatching { exchange.requestBody.use { it.readBytes() } }
            writeJson(exchange, 200, readinessJson(operations.enhancedTranscriptionReadiness()))
        }

        private fun handleExternalTranscription(exchange: HttpExchange, rawId: String) {
            runCatching { exchange.requestBody.use { it.readBytes() } }
            if (!SAFE_ROUTE_ID.matches(rawId)) {
                writeJson(exchange, 400, jsonObject("error" to "invalid evidence artefact id"))
                return
            }
            val id = try { EvidenceArtifactId(rawId) } catch (_: IllegalArgumentException) {
                writeJson(exchange, 400, jsonObject("error" to "invalid evidence artefact id")); return
            }
            val readiness = operations.enhancedTranscriptionReadiness()
            if (readiness !is EnhancedTranscriptionReadiness.Ready) {
                writeJson(exchange, 409, readinessJson(readiness)); return
            }
            val body = when (val outcome = runBlocking { operations.transcribeExternal(id) }) {
                is EnhancedTranscriptionOutcome.Admitted -> jsonObject(
                    "status" to "TIER_B_DURABLE_COMPLETE", "evidenceArtifactId" to id.value,
                    "derivativeGenerationId" to outcome.derivativeGenerationId.value, "content" to ocrContentJson(outcome.content),
                )
                is EnhancedTranscriptionOutcome.ReconciliationRequired -> jsonObject(
                    "status" to "RECONCILIATION_REQUIRED", "evidenceArtifactId" to id.value,
                    "derivativeGenerationId" to outcome.derivativeGenerationId.value, "content" to ocrContentJson(outcome.content),
                    "message" to outcome.safeMessage,
                )
                is EnhancedTranscriptionOutcome.NotReady -> readinessJson(outcome.readiness)
                is EnhancedTranscriptionOutcome.Failed -> jsonObject("status" to "FAILED", "message" to outcome.safeMessage)
            }
            writeJson(exchange, 200, body)
        }

        private fun handleAcceptanceExecution(exchange: HttpExchange, authorityId: String) {
            runCatching { exchange.requestBody.use { it.readBytes() } }
            if (!Regex("^[A-Za-z0-9_.-]{1,120}$").matches(authorityId)) {
                writeJson(exchange, 400, jsonObject("error" to "invalid acceptance authority id")); return
            }
            val outcome = runBlocking { invokeFidelityFirstAcceptance(authorityId) }
            val (status, body) = when (outcome) {
                is FidelityFirstAcceptanceOutcome.Admitted -> 200 to jsonObject(
                    "status" to "ACCEPTANCE_GENERATION_ADMITTED", "authorityId" to outcome.authorityId,
                    "attemptId" to outcome.attemptId, "derivativeGenerationId" to outcome.generationId.value,
                    "humanReviewState" to "UNREVIEWED",
                )
                is FidelityFirstAcceptanceOutcome.Blocked -> 409 to jsonObject("status" to "BLOCKED", "reason" to outcome.reason)
                is FidelityFirstAcceptanceOutcome.Failed -> 200 to jsonObject(
                    "status" to "FAILED", "reason" to outcome.reason, "providerAttemptStarted" to outcome.attemptStarted,
                )
            }
            writeJson(exchange, status, body)
        }

        /**
         * Document Ingestion — Tier B Durable OCR Derivative Content.
         * Retrieves an already-persisted Tier B durable OCR generation's
         * content by known [EvidenceArtifactId] + [DerivativeGenerationId]
         * -- never reruns OCR. Mirrors [handleRetrieveContent]'s own
         * identical unsafe-identifier handling exactly.
         */
        private fun handleRetrieveOcrContent(exchange: HttpExchange, rawEvidenceArtifactId: String, rawDerivativeGenerationId: String) {
            runCatching { exchange.requestBody.use { it.readBytes() } }
            val evidenceArtifactId = try {
                EvidenceArtifactId(rawEvidenceArtifactId)
            } catch (e: IllegalArgumentException) {
                writeJson(exchange, 400, jsonObject("error" to "invalid evidence artefact id"))
                return
            }
            val derivativeGenerationId = try {
                DerivativeGenerationId(rawDerivativeGenerationId)
            } catch (e: IllegalArgumentException) {
                writeJson(exchange, 400, jsonObject("error" to "invalid derivative generation id"))
                return
            }
            val outcome = try {
                runBlocking { operations.retrieveTierBOcrContent(evidenceArtifactId, derivativeGenerationId) }
            } catch (e: DerivativeGenerationStorageException.UnsafeIdentifier) {
                writeJson(exchange, 400, jsonObject("error" to "invalid derivative generation id"))
                return
            } catch (e: DerivativeContentStorageException.UnsafeIdentifier) {
                writeJson(exchange, 400, jsonObject("error" to "invalid derivative generation id"))
                return
            }
            val body = when (outcome) {
                is TierBOcrContentRetrievalResult.Retrieved -> jsonObject(
                    "status" to "RETRIEVED",
                    "content" to ocrContentJson(outcome.content),
                )
                TierBOcrContentRetrievalResult.UnknownGeneration -> jsonObject("status" to "UNKNOWN_GENERATION")
                TierBOcrContentRetrievalResult.SourceMismatch -> jsonObject("status" to "SOURCE_MISMATCH")
                TierBOcrContentRetrievalResult.WrongDerivativeKind -> jsonObject("status" to "WRONG_DERIVATIVE_KIND")
                TierBOcrContentRetrievalResult.ContentMissing -> jsonObject("status" to "CONTENT_MISSING")
                is TierBOcrContentRetrievalResult.ContentCorrupt -> jsonObject("status" to "CONTENT_CORRUPT", "message" to outcome.safeMessage)
                is TierBOcrContentRetrievalResult.UnsupportedRepresentationVersion -> jsonObject(
                    "status" to "UNSUPPORTED_VERSION",
                    "version" to outcome.version,
                )
                is TierBOcrContentRetrievalResult.Failed -> jsonObject("status" to "FAILED", "message" to outcome.safeMessage)
            }
            writeJson(exchange, 200, body)
        }
    }

    // ---- /owner/analyse -------------------------------------------------------------------

    /**
     * Minimum Production Document Pipeline — Local Reasoning Implementation.
     * The one owner-authenticated HTTP route through which one or more
     * already-admitted evidence derivative generations may be submitted for
     * analysis via [OwnerEvidenceOperations.analyseDocuments]. Accepts only
     * a JSON body of already-known identifiers plus the owner's own
     * instruction text -- never a filesystem path, never an enumeration
     * request. Mirrors [EvidenceHandler]'s own authentication/error-shape
     * discipline exactly: missing/wrong token is 401, a malformed body is a
     * clean 400, and an unexpected fault is a safe 500 that never exposes
     * an internal path, stack trace, credential, prompt, or raw exception
     * message.
     */
    private inner class AnalyseHandler : HttpHandler {
        override fun handle(exchange: HttpExchange) {
            try {
                if (!isAuthorised(exchange)) {
                    rejectUnauthorised(exchange)
                    return
                }
                if (exchange.requestURI.path != "/owner/analyse" || exchange.requestMethod != "POST") {
                    runCatching { exchange.requestBody.use { it.readBytes() } }
                    writeJson(exchange, 404, jsonObject("error" to "not found"))
                    return
                }

                val bodyBytes = try {
                    exchange.requestBody.use { readBounded(it, MAX_ANALYSE_REQUEST_BODY_BYTES) }
                } catch (e: RequestBodyTooLargeException) {
                    // The stream is deliberately left un-drained here (readBounded stops reading the
                    // instant it confirms the body exceeds the limit, never buffering the rest of an
                    // oversized body) -- exchange.close() below closes the underlying connection
                    // instead of attempting to reuse it, which is exactly the documented, safe
                    // HttpExchange behaviour for a request whose body was never fully read.
                    writeJson(exchange, 400, jsonObject("error" to "request body too large"))
                    return
                }
                val parsed = try {
                    parseAnalyseRequestBody(bodyBytes)
                } catch (e: JsonParseException) {
                    writeJson(exchange, 400, jsonObject("error" to "malformed request body"))
                    return
                } catch (e: IllegalArgumentException) {
                    writeJson(exchange, 400, jsonObject("error" to "malformed request body"))
                    return
                }
                val (selections, instruction) = parsed

                val invocation = try {
                    runBlocking { operations.analyseDocuments(selections, instruction) }
                } catch (e: DerivativeGenerationStorageException.UnsafeIdentifier) {
                    writeJson(exchange, 400, jsonObject("error" to "invalid derivative generation id"))
                    return
                } catch (e: DerivativeContentStorageException.UnsafeIdentifier) {
                    writeJson(exchange, 400, jsonObject("error" to "invalid derivative generation id"))
                    return
                } catch (e: IllegalArgumentException) {
                    // OwnerDocumentAnalysisRequest's own init{} require() blocks (empty selections,
                    // blank instruction) surface here as a caller error, never an internal fault.
                    writeJson(exchange, 400, jsonObject("error" to "malformed request body"))
                    return
                }
                writeJson(exchange, 200, analysisOutcomeJson(invocation.outcome, invocation.pendingAnalysisId))
            } catch (e: Exception) {
                logger.error("Owner HTTP: unexpected failure handling ${exchange.requestURI}", e)
                runCatching { writeJson(exchange, 500, jsonObject("error" to "internal error")) }
            } finally {
                exchange.close()
            }
        }
    }

    // ---- /owner/saved-analyses, /owner/saved-analyses/{savedAnalysisId} -------------------

    /**
     * Reviewed Analysis Result — Explicit Owner Save. `POST /owner/saved-analyses` durably saves
     * the pending analysis named by the request body's `pendingAnalysisId` -- never accepts, and
     * never trusts, resubmitted analysis text (the browser sends only the opaque id
     * `/owner/analyse` already returned). `GET /owner/saved-analyses` returns a bounded, metadata-
     * only listing; `GET /owner/saved-analyses/{savedAnalysisId}` retrieves one full saved
     * analysis by known id. Mirrors [AnalyseHandler]'s own authentication/bounded-body/error-shape
     * discipline exactly.
     */
    private inner class SavedAnalysisHandler : HttpHandler {
        override fun handle(exchange: HttpExchange) {
            try {
                if (!isAuthorised(exchange)) {
                    rejectUnauthorised(exchange)
                    return
                }
                val path = exchange.requestURI.path
                val method = exchange.requestMethod
                val segments = path.removePrefix("/owner/saved-analyses").trim('/').split('/').filter { it.isNotEmpty() }

                when {
                    segments.isEmpty() && method == "POST" -> handleSave(exchange)
                    segments.isEmpty() && method == "GET" -> handleList(exchange)
                    segments.size == 1 && method == "GET" -> handleRetrieve(exchange, segments[0])
                    else -> {
                        runCatching { exchange.requestBody.use { it.readBytes() } }
                        writeJson(exchange, 404, jsonObject("error" to "not found"))
                    }
                }
            } catch (e: Exception) {
                logger.error("Owner HTTP: unexpected failure handling ${exchange.requestURI}", e)
                runCatching { writeJson(exchange, 500, jsonObject("error" to "internal error")) }
            } finally {
                exchange.close()
            }
        }

        private fun handleSave(exchange: HttpExchange) {
            val bodyBytes = try {
                exchange.requestBody.use { readBounded(it, MAX_SAVE_REQUEST_BODY_BYTES) }
            } catch (e: RequestBodyTooLargeException) {
                writeJson(exchange, 400, jsonObject("error" to "request body too large"))
                return
            }
            val pendingAnalysisIdRaw = try {
                parseSaveRequestBody(bodyBytes)
            } catch (e: JsonParseException) {
                writeJson(exchange, 400, jsonObject("error" to "malformed request body"))
                return
            }
            val pendingAnalysisId = try {
                PendingAnalysisId(pendingAnalysisIdRaw)
            } catch (e: IllegalArgumentException) {
                writeJson(exchange, 400, jsonObject("error" to "malformed request body"))
                return
            }

            val outcome = runBlocking { operations.saveAnalysis(pendingAnalysisId) }
            val body = when (outcome) {
                is OwnerSaveAnalysisOutcome.Saved -> jsonObject("status" to "SAVED", "savedAnalysisId" to outcome.savedAnalysisId.value)
                OwnerSaveAnalysisOutcome.UnknownOrExpiredPendingAnalysis ->
                    jsonObject("status" to "FAILED", "message" to "Unknown or expired pending analysis.")
                OwnerSaveAnalysisOutcome.SaveAlreadyInProgress ->
                    jsonObject("status" to "FAILED", "message" to "This analysis is already being saved.")
                is OwnerSaveAnalysisOutcome.Failed -> jsonObject("status" to "FAILED", "message" to outcome.safeMessage)
            }
            writeJson(exchange, 200, body)
        }

        private fun handleRetrieve(exchange: HttpExchange, rawId: String) {
            runCatching { exchange.requestBody.use { it.readBytes() } }
            val savedAnalysisId = try {
                SavedAnalysisId(rawId)
            } catch (e: IllegalArgumentException) {
                writeJson(exchange, 400, jsonObject("error" to "invalid saved analysis id"))
                return
            }
            val outcome = try {
                runBlocking { operations.retrieveSavedAnalysis(savedAnalysisId) }
            } catch (e: SavedAnalysisStorageException.UnsafeIdentifier) {
                writeJson(exchange, 400, jsonObject("error" to "invalid saved analysis id"))
                return
            }
            val body = when (outcome) {
                is OwnerRetrieveSavedAnalysisOutcome.Retrieved -> jsonObject(
                    "status" to "RETRIEVED",
                    "result" to savedAnalysisJson(outcome.presentation),
                )
                OwnerRetrieveSavedAnalysisOutcome.UnknownSavedAnalysis -> jsonObject("status" to "UNKNOWN_SAVED_ANALYSIS")
                is OwnerRetrieveSavedAnalysisOutcome.Failed -> jsonObject("status" to "FAILED", "message" to outcome.safeMessage)
            }
            writeJson(exchange, 200, body)
        }

        private fun handleList(exchange: HttpExchange) {
            runCatching { exchange.requestBody.use { it.readBytes() } }
            val summaries = runBlocking { operations.listSavedAnalyses() }
            writeJson(
                exchange,
                200,
                jsonObject(
                    "savedAnalyses" to jsonArray(
                        summaries.map {
                            jsonObject(
                                "savedAnalysisId" to it.savedAnalysisId.value,
                                "savedAt" to it.savedAt.toString(),
                                "instructionPreview" to it.instructionPreview,
                            )
                        },
                    ),
                ),
            )
        }
    }

    private fun savedAnalysisJson(presentation: OwnerSavedAnalysisPresentation): JsonObject = jsonObject(
        "savedAnalysisId" to presentation.savedAnalysisId.value,
        "savedAt" to presentation.savedAt.toString(),
        "analysedAt" to presentation.analysedAt.toString(),
        "instruction" to presentation.instruction,
        "analysisText" to presentation.analysisText,
        "evidenceReferences" to jsonArray(
            presentation.evidenceReferences.map {
                jsonObject(
                    "evidenceArtifactId" to it.evidenceArtifactId.value,
                    "derivativeGenerationId" to it.derivativeGenerationId.value,
                    "derivativeKind" to it.derivativeKind,
                    "assurance" to it.assurance?.let(::analysisAssuranceJson),
                )
            },
        ),
        "mechanismIdentity" to presentation.mechanismIdentity,
        "mechanismVersion" to presentation.mechanismVersion,
    )

    private fun analysisOutcomeJson(outcome: OwnerDocumentAnalysisOutcome, pendingAnalysisId: PendingAnalysisId?): JsonObject = when (outcome) {
        is OwnerDocumentAnalysisOutcome.Completed -> jsonObject(
            "status" to "COMPLETED",
            "pendingAnalysisId" to pendingAnalysisId?.value,
            "result" to jsonObject(
                "analysisText" to outcome.result.analysisText,
                "evidenceReferences" to jsonArray(
                    outcome.result.evidenceReferences.map {
                        jsonObject(
                            "evidenceArtifactId" to it.evidenceArtifactId.value,
                            "derivativeGenerationId" to it.derivativeGenerationId.value,
                            "derivativeKind" to it.derivativeKind,
                            "assurance" to it.assurance?.let(::analysisAssuranceJson),
                        )
                    },
                ),
                "mechanismIdentity" to outcome.result.mechanismIdentity,
                "mechanismVersion" to outcome.result.mechanismVersion,
                "instruction" to outcome.result.instruction,
                "warnings" to jsonArray(outcome.result.warnings),
            ),
        )
        is OwnerDocumentAnalysisOutcome.NotAuthorised ->
            jsonObject("status" to "FAILED", "message" to "Not authorised: ${outcome.reason}")
        is OwnerDocumentAnalysisOutcome.TooManySelections ->
            jsonObject("status" to "FAILED", "message" to "Too many documents selected (max ${outcome.max}).")
        is OwnerDocumentAnalysisOutcome.InstructionTooLarge ->
            jsonObject("status" to "FAILED", "message" to "The analysis instruction exceeds the maximum accepted length (max ${outcome.max} characters).")
        is OwnerDocumentAnalysisOutcome.PromptTooLarge ->
            jsonObject("status" to "FAILED", "message" to "The assembled analysis request exceeds the maximum accepted size.")
        is OwnerDocumentAnalysisOutcome.UnknownGeneration ->
            jsonObject("status" to "FAILED", "message" to "Unknown derivative generation.")
        is OwnerDocumentAnalysisOutcome.SourceMismatch ->
            jsonObject("status" to "FAILED", "message" to "A derivative generation does not belong to the specified evidence artefact.")
        is OwnerDocumentAnalysisOutcome.ContentMissing ->
            jsonObject("status" to "FAILED", "message" to "Derivative content is missing.")
        is OwnerDocumentAnalysisOutcome.ContentCorrupt ->
            jsonObject("status" to "FAILED", "message" to "Derivative content is corrupt: ${outcome.safeMessage}")
        is OwnerDocumentAnalysisOutcome.UnsupportedRepresentationVersion ->
            jsonObject("status" to "FAILED", "message" to "Unsupported derivative representation version (${outcome.version}).")
        is OwnerDocumentAnalysisOutcome.UnsupportedDerivativeKind ->
            jsonObject("status" to "FAILED", "message" to "Unsupported derivative kind (${outcome.derivativeKind}).")
        is OwnerDocumentAnalysisOutcome.UnverifiedExternalAcknowledgementRequired ->
            jsonObject(
                "status" to "ACKNOWLEDGEMENT_REQUIRED",
                "message" to "Explicit acknowledgement is required for the selected unverified machine transcription.",
                "evidenceArtifactId" to outcome.evidenceArtifactId.value,
                "derivativeGenerationId" to outcome.derivativeGenerationId.value,
            )
        is OwnerDocumentAnalysisOutcome.ContentTooLarge ->
            jsonObject(
                "status" to "FAILED",
                "message" to "Selected evidence content exceeds the maximum accepted size " +
                    "(${outcome.actualCharacters}/${outcome.max} characters).",
            )
        is OwnerDocumentAnalysisOutcome.ResponseTooLarge ->
            jsonObject("status" to "FAILED", "message" to "The local model's response exceeded the maximum accepted size.")
        is OwnerDocumentAnalysisOutcome.ModelInvocationFailed ->
            jsonObject("status" to "FAILED", "message" to outcome.safeMessage)
    }

    private fun analysisAssuranceJson(value: parker.core.interfaces.AnalysisAcquisitionAssurance): JsonObject = jsonObject(
        "sourceSha256" to value.sourceSha256,
        "sourceByteLength" to value.sourceByteLength,
        "sourceMediaType" to value.sourceMediaType,
        "mechanism" to value.mechanism.name,
        "capabilityIdentity" to value.capabilityIdentity,
        "routingReasons" to jsonArray(value.routingReasons),
        "providerIdentity" to value.providerIdentity,
        "adapterIdentity" to value.adapterIdentity,
        "adapterVersion" to value.adapterVersion,
        "modelIdentity" to value.modelIdentity,
        "modelSnapshot" to value.modelSnapshot,
        "configurationProfile" to value.configurationProfile,
        "processingProfile" to value.processingProfile,
        "fidelity" to value.fidelity?.name,
        "completeness" to value.completenessState.name,
        "requestedPages" to value.requestedPages?.let(::jsonArray),
        "submittedPages" to value.submittedPages?.let(::jsonArray),
        "returnedPages" to value.returnedPages?.let(::jsonArray),
        "pageOutcomes" to jsonArray(value.pageOutcomes),
        "containsUncertaintyOrIllegibility" to value.containsUncertaintyOrIllegibility,
        "humanReviewStates" to jsonArray(value.humanReviewStates.map { it.name }.sorted()),
        "reviewedPages" to jsonArray(value.reviewedPages.sorted()),
        "reviewedCharacterScopeCount" to value.reviewedCharacterScopeCount,
    )

    private fun ocrContentJson(content: OwnerTierBOcrContent): JsonObject = jsonObject(
        "recognisedText" to content.recognisedText,
        "fidelity" to content.fidelity,
        "outcomeKind" to content.outcomeKind,
        "degradationReason" to content.degradationReason,
        "warnings" to jsonArray(content.warnings),
        "segments" to jsonArray(
            content.segments.map { jsonObject("text" to it.text, "fidelity" to it.fidelity, "pageNumber" to it.pageNumber) },
        ),
        "producer" to producerJson(content.producer),
        "completenessState" to content.completenessState,
        "sourceEvidenceArtifactId" to content.sourceEvidenceArtifactId,
        "providerIdentity" to content.providerIdentity,
        "returnedModelIdentifier" to content.returnedModelIdentifier,
        "transcriptionProfileIdentity" to content.transcriptionProfileIdentity,
        "humanReviewStates" to jsonArray(content.humanReviewStates.ifEmpty { listOf("UNREVIEWED") }),
        "modelSnapshot" to content.modelSnapshot,
        "requestedPages" to content.requestedPages?.let(::jsonArray),
        "submittedPages" to content.submittedPages?.let(::jsonArray),
        "returnedPages" to content.returnedPages?.let(::jsonArray),
        "pageOutcomes" to jsonArray(content.pageOutcomes.map { jsonObject("pageNumber" to it.pageNumber, "outcome" to it.outcome, "reason" to it.reason, "warnings" to jsonArray(it.warnings)) }),
        "containsUncertaintyOrIllegibility" to content.containsUncertaintyOrIllegibility,
        "externalTranscription" to content.externalTranscription,
    )

    private fun readinessJson(readiness: EnhancedTranscriptionReadiness): JsonObject = when (readiness) {
        EnhancedTranscriptionReadiness.Disabled -> jsonObject("status" to "DISABLED", "message" to "Enhanced transcription is not enabled in this runtime.")
        is EnhancedTranscriptionReadiness.ProfileNotReady -> jsonObject("status" to "PROFILE_NOT_READY", "message" to readiness.safeReason)
        EnhancedTranscriptionReadiness.MissingCredential -> jsonObject("status" to "MISSING_CREDENTIAL", "message" to "Enhanced transcription credential is not configured.")
        EnhancedTranscriptionReadiness.Ready -> jsonObject("status" to "READY", "message" to "Enhanced transcription is available.")
    }

    private fun acquisitionDecisionJson(decision: OwnerAcquisitionDecisionView): JsonObject = when (decision) {
        is OwnerAcquisitionDecisionView.Selected -> jsonObject(
            "status" to "SELECTED", "source" to acquisitionSourceJson(decision.source),
            "capability" to acquisitionCapabilityJson(decision.capability), "explanation" to decision.explanation,
            "executeAvailable" to (decision.capability.availability == "READY"),
        )
        is OwnerAcquisitionDecisionView.Proposed -> jsonObject(
            "status" to "PROPOSED", "source" to acquisitionSourceJson(decision.source),
            "capability" to acquisitionCapabilityJson(decision.capability), "explanation" to decision.explanation,
            "disclosure" to decision.disclosure, "egressAuthorization" to decision.egressAuthorization,
            "nextStep" to decision.nextStep, "executeAvailable" to decision.executeAvailable,
            "authorizationAvailable" to decision.authorizationAvailable,
            "authorizationId" to decision.authorizationId,
            "authorizationExpiresAt" to decision.authorizationExpiresAt,
            "executionState" to decision.executionState,
        )
        is OwnerAcquisitionDecisionView.NoEligible -> jsonObject(
            "status" to "NO_ELIGIBLE_CAPABILITY", "source" to acquisitionSourceJson(decision.source),
            "reasons" to jsonArray(decision.reasons), "executeAvailable" to false,
        )
        is OwnerAcquisitionDecisionView.Indeterminate -> jsonObject(
            "status" to "INDETERMINATE", "source" to acquisitionSourceJson(decision.source),
            "reasons" to jsonArray(decision.reasons), "executeAvailable" to false,
        )
        is OwnerAcquisitionDecisionView.Ambiguous -> jsonObject(
            "status" to "AMBIGUOUS", "source" to acquisitionSourceJson(decision.source),
            "capabilityIds" to jsonArray(decision.capabilityIds), "reasons" to jsonArray(decision.reasons),
            "executeAvailable" to false,
        )
    }

    private fun acquisitionSourceJson(source: OwnerAcquisitionSourceFacts) = jsonObject(
        "evidenceArtifactId" to source.evidenceArtifactId, "mediaType" to source.mediaType,
        "byteLength" to source.byteLength, "pageCount" to source.pageCount,
        "nativeSearchableText" to source.nativeSearchableText, "imageOnlyOrScanned" to source.imageOnlyOrScanned,
        "mixedTextAndImage" to source.mixedTextAndImage, "handwriting" to source.handwriting,
        "complexLayout" to source.complexLayout, "tables" to source.tables,
    )

    private fun acquisitionCapabilityJson(capability: OwnerAcquisitionCapabilityView) = jsonObject(
        "capabilityId" to capability.capabilityId, "mechanism" to capability.mechanismLabel,
        "executionLocation" to capability.executionLocation, "externalEgressRequired" to capability.externalEgressRequired,
        "provider" to capability.provider, "modelRule" to capability.modelRule, "profile" to capability.profile,
        "representation" to capability.representationLabel, "availability" to capability.availability,
        "selectionReasons" to jsonArray(capability.selectionReasons),
        "configurationIdentity" to capability.configurationIdentity,
        "processingProfile" to capability.processingProfile,
        "instructionSha256" to capability.instructionSha256,
        "schemaSha256" to capability.schemaSha256,
        "reasoningEffort" to capability.reasoningEffort,
        "store" to capability.store,
        "pdfDetail" to capability.pdfDetail,
        "imageDetail" to capability.imageDetail,
    )

    private fun acquisitionExecutionJson(outcome: OwnerAcquisitionExecutionView.Admitted) = jsonObject(
        "status" to "COMPLETED", "evidenceArtifactId" to outcome.evidenceArtifactId,
        "derivativeGenerationId" to outcome.derivativeGenerationId.value,
        "capability" to acquisitionCapabilityJson(outcome.capability), "fidelity" to outcome.fidelity,
        "completeness" to outcome.completeness, "humanReviewState" to outcome.humanReviewState,
        "sourceLink" to "/owner/evidence/${outcome.evidenceArtifactId}",
    )

    // ---- Owner Tier A Extracted Content Presentation: safe owner-facing content -> JSON ----------
    // Every value below is either a server-controlled literal or threaded through jsonString's own
    // escaping (writeJsonValue), so extracted text/metadata can never break out of its JSON string
    // context or inject markup -- the browser page also never inserts it as raw HTML (Phase 7 renders
    // it via textContent, never innerHTML).

    private fun contentJson(content: OwnerTierAContent): JsonObject = when (content) {
        is OwnerTierAContent.Pdf -> jsonObject(
            "kind" to "PDF",
            "documentText" to content.documentText,
            "pageCount" to content.pageCount,
            "pageTextAssociationAvailable" to content.pageTextAssociationAvailable,
            "producer" to producerJson(content.producer),
            "transformationHistory" to jsonArray(content.transformationHistory),
            "completenessState" to content.completenessState,
            "warnings" to jsonArray(content.warnings),
            "metadata" to jsonArray(content.metadata.map { jsonObject("name" to it.name, "value" to it.value) }),
        )
        is OwnerTierAContent.Csv -> jsonObject(
            "kind" to "CSV",
            "headers" to jsonArray(content.headers),
            "previewRows" to jsonArray(content.previewRows.map { jsonArray(it) }),
            "totalRowCount" to content.totalRowCount,
            "rowsTruncatedForDisplay" to content.rowsTruncatedForDisplay,
            "producer" to producerJson(content.producer),
            "completenessState" to content.completenessState,
            "warnings" to jsonArray(content.warnings),
        )
        is OwnerTierAContent.Eml -> jsonObject(
            "kind" to "EML",
            "from" to content.from,
            "to" to content.to,
            "cc" to content.cc,
            "subject" to content.subject,
            "rawDate" to content.rawDate,
            "messageId" to content.messageId,
            "bodyAlternatives" to jsonArray(
                content.bodyAlternatives.map {
                    jsonObject("mediaType" to it.mediaType, "charset" to it.charset, "text" to it.text)
                },
            ),
            "attachmentCandidateCount" to content.attachmentCandidateCount,
            "attachmentCandidates" to jsonArray(
                content.attachmentCandidates.map {
                    jsonObject("filename" to it.filename, "declaredMimeType" to it.declaredMimeType, "byteLength" to it.byteLength)
                },
            ),
            "producer" to producerJson(content.producer),
            "completenessState" to content.completenessState,
            "warnings" to jsonArray(content.warnings),
        )
        is OwnerTierAContent.Docx -> jsonObject(
            "kind" to "DOCX",
            "paragraphs" to jsonArray(content.paragraphs),
            "tables" to jsonArray(content.tables.map { table -> jsonArray(table.rows.map { row -> jsonArray(row) }) }),
            "headers" to jsonArray(content.headers),
            "footers" to jsonArray(content.footers),
            "producer" to producerJson(content.producer),
            "completenessState" to content.completenessState,
            "warnings" to jsonArray(content.warnings),
        )
        is OwnerTierAContent.RegionTranscription -> jsonObject(
            "kind" to "REGION_TRANSCRIPTION",
            "derivativeGenerationId" to content.derivativeGenerationId,
            "derivativeKind" to content.derivativeKind,
            "contentIdentityAlgorithm" to content.contentIdentityAlgorithm,
            "contentIdentityDigest" to content.contentIdentityDigest,
            "evidenceArtifactId" to content.evidenceArtifactId,
            "sourceSha256" to content.sourceSha256,
            "representationVersion" to content.representationVersion,
            "capabilityId" to content.capabilityId,
            "capabilityDigest" to content.capabilityDigest,
            "pageBindings" to jsonArray(content.pageBindings),
            "regionBindings" to jsonArray(content.regionBindings),
            "transcriptionBlocks" to jsonArray(content.transcriptionBlocks),
            "providerReturnedOrder" to jsonArray(content.providerReturnedOrder),
            "parkerSourceOrder" to jsonArray(content.parkerSourceOrder),
            "provider" to content.provider,
            "providerProfile" to content.providerProfile,
            "model" to content.model,
            "responseIdentity" to content.responseIdentity,
            "providerStateRecordIdentity" to content.providerStateRecordIdentity,
            "ownerAuthorizationIdentity" to content.ownerAuthorizationIdentity,
            "executionIdentity" to content.executionIdentity,
            "attemptIdentity" to content.attemptIdentity,
            "requestIdentity" to content.requestIdentity,
            "requestDigest" to content.requestDigest,
            "preparationIdentity" to content.preparationIdentity,
            "preparationProfile" to content.preparationProfile,
            "preparationProfileVersion" to content.preparationProfileVersion,
            "authorizationPurpose" to content.authorizationPurpose,
            "processingProfile" to content.processingProfile,
            "producer" to producerJson(content.producer),
            "transformationHistory" to jsonArray(content.transformationHistory),
            "completenessState" to content.completenessState,
            "warnings" to jsonArray(content.warnings),
            "humanFidelityStatus" to jsonObject(
                "effectiveReviewState" to content.humanFidelityStatus.effectiveReviewState,
                "coverage" to content.humanFidelityStatus.coverage,
                "materialDiscrepancyCount" to content.humanFidelityStatus.materialDiscrepancyCount,
                "systematicPatternCount" to content.humanFidelityStatus.systematicPatternCount,
                "unresolvedConflict" to content.humanFidelityStatus.unresolvedConflict,
                "sourceConfirmedEligibility" to content.humanFidelityStatus.sourceConfirmedEligibility,
                "sourceConfirmedDenialReason" to content.humanFidelityStatus.sourceConfirmedDenialReason,
            ),
            "humanCorrectedRepresentation" to content.humanCorrectedRepresentation?.let { corrected -> jsonObject(
                "derivativeGenerationId" to corrected.derivativeGenerationId,
                "representationKind" to corrected.representationKind,
                "reviewId" to corrected.reviewId,
                "correctedTranscriptionBlocks" to jsonArray(corrected.correctedTranscriptionBlocks),
                "correctedContentSha256" to corrected.correctedContentSha256,
                "correctionCount" to corrected.correctionCount,
                "sourceConfirmedEligibility" to corrected.sourceConfirmedEligibility,
                "sourceConfirmedDenialReason" to corrected.sourceConfirmedDenialReason,
            ) },
        )
    }

    private fun producerJson(producer: OwnerDerivativeProducerSummary): JsonObject = jsonObject(
        "pluginIdentity" to producer.pluginIdentity,
        "pluginVersion" to producer.pluginVersion,
        "configurationIdentity" to producer.configurationIdentity,
        "adapterIdentity" to producer.adapterIdentity,
        "adapterVersion" to producer.adapterVersion,
        "modelIdentity" to producer.modelIdentity,
        "modelVersion" to producer.modelVersion,
    )

    // ---- minimal JSON writer (no JSON library exists anywhere in this repository; every value
    // written below is either a server-controlled literal or passed through String escaping) ------

    private sealed interface JsonValue
    private class JsonObject(val fields: List<Pair<String, Any?>>) : JsonValue
    private class JsonArray(val items: List<Any?>) : JsonValue

    private fun jsonObject(vararg fields: Pair<String, Any?>): JsonObject = JsonObject(fields.toList())

    /** An array of plain values (strings, numbers, nested objects/arrays) -- not only [JsonObject]s. */
    private fun jsonArray(items: List<Any?>): JsonArray = JsonArray(items)

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
                value.items.forEachIndexed { index, item ->
                    if (index > 0) sb.append(',')
                    writeJsonValue(sb, item)
                }
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

    companion object {
        const val SESSION_COOKIE = "ParkerOwnerSession"
        const val DEVICE_ID_COOKIE = "ParkerOwnerDeviceId"
        const val DEVICE_CREDENTIAL_COOKIE = "ParkerOwnerDeviceCredential"
        private val SAFE_ROUTE_ID = Regex("^[A-Za-z0-9._-]{1,1024}$")
        /** Mirrors `OwnerLocalFileIngressCoordinator.MAX_SOURCE_BYTES` -- the same 64 MiB ingress bound, enforced here during streaming so an oversized upload never even reaches a temp file in full. */
        const val MAX_PART_BYTES: Long = 64L * 1024L * 1024L

        /** A conservative bound on files per request -- convenience-only multi-select, never a batch evidence authority. */
        const val MAX_FILES_PER_REQUEST: Int = 32

        /**
         * Minimum Production Document Pipeline — Local Reasoning Implementation. A finite bound on
         * the `/owner/analyse` JSON request body -- deliberately not MAX_PART_BYTES (a 64 MiB upload
         * bound; this route never carries evidence content, only already-known identifiers plus the
         * owner's own instruction text). Sized generously for the actual request shape: up to
         * `DocumentAnalysisCoordinator.MAX_SELECTIONS` (20) identifier pairs plus an instruction up
         * to `DocumentAnalysisCoordinator.MAX_INSTRUCTION_CHARACTERS` (4,000 characters, worst case
         * 4 bytes/char in UTF-8), with headroom for JSON structure/escaping overhead.
         */
        const val MAX_ANALYSE_REQUEST_BODY_BYTES: Long = 32L * 1024L

        /**
         * Reviewed Analysis Result — Explicit Owner Save. A finite bound on the
         * `POST /owner/saved-analyses` JSON request body -- this route never carries analysis
         * content, only one opaque pending-analysis identifier (a UUID string), so a small,
         * generous bound comfortably covers it with headroom for JSON structure.
         */
        const val MAX_SAVE_REQUEST_BODY_BYTES: Long = 4L * 1024L
        const val MAX_PROMOTION_REQUEST_BODY_BYTES: Long = 1024L
    }
}

/**
 * Minimum Production Document Pipeline — Local Reasoning Implementation.
 * Thrown by [readBounded] the instant more than [RequestBodyTooLargeException.limitBytes] bytes
 * have been read -- never after buffering an unbounded body and inspecting its size afterward.
 */
internal class RequestBodyTooLargeException(val limitBytes: Long) : Exception("request body exceeds $limitBytes bytes")

/**
 * Reads at most `limit + 1` bytes from [input], throwing [RequestBodyTooLargeException] the
 * instant the `(limit + 1)`th byte is confirmed present -- this function never calls an unbounded
 * `readBytes()` and inspects the resulting size afterward, and never buffers more than `limit + 1`
 * bytes of an oversized body before rejecting it.
 */
internal fun readBounded(input: InputStream, limit: Long): ByteArray {
    val out = ByteArrayOutputStream(minOf(limit + 1, 8192L).toInt())
    val chunk = ByteArray(8192)
    var total = 0L
    while (true) {
        val remainingToConfirmOverflow = limit + 1 - total
        if (remainingToConfirmOverflow <= 0) throw RequestBodyTooLargeException(limit)
        val toRead = minOf(chunk.size.toLong(), remainingToConfirmOverflow).toInt()
        val n = input.read(chunk, 0, toRead)
        if (n == -1) break
        total += n
        if (total > limit) throw RequestBodyTooLargeException(limit)
        out.write(chunk, 0, n)
    }
    return out.toByteArray()
}

/**
 * Minimum Production Document Pipeline — Local Reasoning Implementation.
 * Parses the fixed `{"selections":[{"evidenceArtifactId":"...","derivativeGenerationId":"..."}],"instruction":"..."}`
 * request-body shape [OwnerEvidenceHttpServer.AnalyseHandler] accepts.
 * Deliberately not a general-purpose JSON deserializer -- [SimpleJsonReader]
 * below is the smallest generic value parser that lets this function
 * reject a malformed body cleanly (`JsonParseException`, mapped by the
 * caller to a 400) rather than risk misparsing a hand-rolled, shape-specific
 * scan.
 */
private fun parseAnalyseRequestBody(bodyBytes: ByteArray): Pair<List<EvidenceGenerationSelection>, String> {
    val text = String(bodyBytes, StandardCharsets.UTF_8)
    val root = SimpleJsonReader(text).parseRootValue()
    val obj = root as? Map<*, *> ?: throw JsonParseException("expected a JSON object")

    val selectionsRaw = obj["selections"] as? List<*> ?: throw JsonParseException("expected a 'selections' array")
    if (selectionsRaw.isEmpty()) throw JsonParseException("'selections' must not be empty")
    val selections = selectionsRaw.map { item ->
        val itemObj = item as? Map<*, *> ?: throw JsonParseException("expected a selection object")
        val evidenceArtifactIdRaw = itemObj["evidenceArtifactId"] as? String
            ?: throw JsonParseException("expected an 'evidenceArtifactId' string")
        val derivativeGenerationIdRaw = itemObj["derivativeGenerationId"] as? String
            ?: throw JsonParseException("expected a 'derivativeGenerationId' string")
        val acknowledgement = itemObj["acknowledgesUnverifiedExternalTranscription"] as? Boolean ?: false
        EvidenceGenerationSelection(
            EvidenceArtifactId(evidenceArtifactIdRaw),
            DerivativeGenerationId(derivativeGenerationIdRaw),
            acknowledgement,
        )
    }

    val instruction = obj["instruction"] as? String ?: throw JsonParseException("expected an 'instruction' string")
    return selections to instruction
}

/** Reviewed Analysis Result — Explicit Owner Save. The `POST /owner/saved-analyses` request body's own tiny, single-field shape -- `{"pendingAnalysisId":"..."}` -- never the analysis content itself. */
private fun parseSaveRequestBody(bodyBytes: ByteArray): String {
    val text = String(bodyBytes, StandardCharsets.UTF_8)
    val root = SimpleJsonReader(text).parseRootValue()
    val obj = root as? Map<*, *> ?: throw JsonParseException("expected a JSON object")
    return obj["pendingAnalysisId"] as? String ?: throw JsonParseException("expected a 'pendingAnalysisId' string")
}

private fun parseExpectedCapabilityId(bodyBytes: ByteArray): String {
    val root = SimpleJsonReader(String(bodyBytes, StandardCharsets.UTF_8)).parseRootValue()
    val obj = root as? Map<*, *> ?: throw JsonParseException("expected a JSON object")
    val value = obj["expectedCapabilityId"] as? String
        ?: throw JsonParseException("expected an 'expectedCapabilityId' string")
    if (value.isBlank() || value.length > 256) throw JsonParseException("invalid expected capability identity")
    return value
}

private fun parseCapabilityPromotionRequest(bodyBytes: ByteArray): OrdinaryRegionCapabilityPromotionRequest {
    val root = SimpleJsonReader(String(bodyBytes, StandardCharsets.UTF_8)).parseRootValue()
    val obj = root as? Map<*, *> ?: throw JsonParseException("expected a JSON object")
    if (obj.keys != setOf("capabilityId", "promotingBuildCommit")) throw JsonParseException("unexpected promotion fields")
    val capability = obj["capabilityId"] as? String ?: throw JsonParseException("missing capabilityId")
    val commit = obj["promotingBuildCommit"] as? String ?: throw JsonParseException("missing promotingBuildCommit")
    if (capability !in setOf(ORDINARY_REGION_CAPABILITY_ID, parker.core.runtime.ORDINARY_REQUEST_REGION_V8_CAPABILITY_ID) || !commit.matches(Regex("^[0-9a-f]{40}$")))
        throw JsonParseException("invalid governed promotion identity")
    return OrdinaryRegionCapabilityPromotionRequest(capability, commit)
}

private fun parseCorrectedPreparationRequest(bodyBytes: ByteArray): Pair<String, Int> {
    val root = SimpleJsonReader(String(bodyBytes, StandardCharsets.UTF_8)).parseRootValue()
    val obj = root as? Map<*, *> ?: throw JsonParseException("expected a JSON object")
    if (obj.keys != setOf("profileId", "profileVersion")) throw JsonParseException("unexpected preparation fields")
    val profile = obj["profileId"] as? String ?: throw JsonParseException("missing profileId")
    val version = (obj["profileVersion"] as? String)?.toIntOrNull() ?: throw JsonParseException("missing profileVersion")
    if (profile.isBlank() || profile.length > 160 || version < 1) throw JsonParseException("invalid preparation profile")
    return profile to version
}

private data class PostEgressContinuationRequest(val evidenceId:EvidenceArtifactId,val authorizationId:String,val executionId:String,val providerStateId:String)
private fun parsePostEgressContinuationRequest(bodyBytes:ByteArray):PostEgressContinuationRequest {
    val root=SimpleJsonReader(String(bodyBytes,StandardCharsets.UTF_8)).parseRootValue()
    val obj=root as? Map<*,*>?:throw JsonParseException("expected a JSON object")
    if(obj.keys!=setOf("evidenceId","authorizationId","executionId","providerStateId"))throw JsonParseException("unexpected continuation fields")
    fun field(name:String)=(obj[name] as? String)?.takeIf{it.isNotBlank()&&it.length<=256}?:throw JsonParseException("invalid $name")
    return PostEgressContinuationRequest(EvidenceArtifactId(field("evidenceId")),field("authorizationId"),field("executionId"),field("providerStateId"))
}

/** `internal`, not `private`, mirroring [MultipartParseException]'s own identical friend-source-set reasoning. */
internal class JsonParseException(message: String) : Exception(message)

/**
 * Final correction pass §3: the `/owner/analyse` request schema is genuinely shallow -- a root
 * object, containing a "selections" array, containing selection objects (three levels) -- so this
 * bound need not be large. Sized with reasonable headroom above that legitimate depth, not merely
 * exactly at it, while still ruling out unbounded/pathological nesting (a StackOverflowError from
 * unbounded recursion is never the effective bound here; this check fires first).
 */
private const val MAX_JSON_NESTING_DEPTH = 10

/**
 * The smallest generic JSON value reader [parseAnalyseRequestBody] needs --
 * objects, arrays, and strings only (this route's own request shape never
 * carries a number or boolean); any other token is a malformed request,
 * reported as [JsonParseException] rather than silently misparsed.
 */
private class SimpleJsonReader(private val text: String) {
    private var pos = 0
    private var depth = 0

    /**
     * Parses exactly one JSON value, then requires that only whitespace (if
     * anything) remains -- unlike [parseValue] alone, which stops the
     * instant a complete value is recognised and never inspects what
     * follows it. A well-formed root JSON document has no trailing
     * non-whitespace content; anything else is a malformed request, never
     * silently accepted.
     */
    fun parseRootValue(): Any {
        val value = parseValue()
        skipWhitespace()
        if (pos != text.length) throw JsonParseException("unexpected trailing content after JSON value at position $pos")
        return value
    }

    fun parseValue(): Any {
        skipWhitespace()
        if (pos >= text.length) throw JsonParseException("unexpected end of JSON")
        return when (text[pos]) {
            '{' -> parseObject()
            '[' -> parseArray()
            '"' -> parseString()
            else -> throw JsonParseException("unexpected token at position $pos")
        }
    }

    /**
     * Every object/array level enters here (or [parseArray]) before recursing into [parseValue]
     * for its own children -- this is the one place nesting depth actually grows, so it is the one
     * place it needs to be bounded. [depth] is decremented in `finally` so sibling structures (not
     * nested inside one another) are never incorrectly accumulated against the same bound.
     */
    private fun enterNestedStructure() {
        depth++
        if (depth > MAX_JSON_NESTING_DEPTH) {
            throw JsonParseException("JSON nesting exceeds the maximum permitted depth of $MAX_JSON_NESTING_DEPTH")
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
                    ',' -> { pos++ }
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
                    ',' -> { pos++ }
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
                            sb.append(hex.toInt(16).toChar())
                            pos += 4
                        }
                        else -> throw JsonParseException("invalid escape sequence")
                    }
                }
                else -> sb.append(c)
            }
        }
    }

    private fun skipWhitespace() {
        while (pos < text.length && text[pos].isWhitespace()) pos++
    }

    private fun peek(): Char {
        if (pos >= text.length) throw JsonParseException("unexpected end of JSON")
        return text[pos]
    }

    private fun expect(c: Char) {
        skipWhitespace()
        if (pos >= text.length || text[pos] != c) throw JsonParseException("expected '$c'")
        pos++
    }
}

private fun extractBoundary(contentType: String): String? {
    if (!contentType.startsWith("multipart/form-data")) return null
    val marker = "boundary="
    val index = contentType.indexOf(marker)
    if (index < 0) return null
    var value = contentType.substring(index + marker.length).substringBefore(';').trim()
    if (value.startsWith('"') && value.endsWith('"') && value.length >= 2) value = value.substring(1, value.length - 1)
    return value.takeIf { it.isNotBlank() }
}

/** `internal`, not `private`, purely so `tests/composition` (a friend source set of `src/composition`, per [resolveEffectiveLogLevel]'s own established precedent) can exercise bounded/truncated multipart handling directly, with a small [BoundedMultipartParser.maxPartBytes] instead of an actual 64 MiB payload. */
internal class MultipartParseException(message: String) : Exception(message)

internal class ParsedFilePart(
    val fieldName: String,
    val originalFileName: String,
    val declaredContentType: String?,
    val tempPath: Path,
)

/** One file part that arrived but was rejected before import -- never counted against, or capable of rolling back, any sibling part in the same request. */
internal class RejectedFilePart(
    val fieldName: String,
    val originalFileName: String,
    val reason: String,
)

internal class MultipartParseOutcome(
    val fileParts: List<ParsedFilePart>,
    val rejectedParts: List<RejectedFilePart>,
)

/**
 * Narrow, bounded, streaming `multipart/form-data` parser scoped exactly to
 * what a browser's own `FormData` produces -- not a general-purpose
 * library. Every file part is streamed directly to a randomly named temp
 * file under [tempDirectory] as its bytes arrive (never buffered whole in
 * heap). A single part exceeding [maxPartBytes] rejects only that part
 * (Phase 6: "one failure must not roll back successful siblings") -- the
 * parser keeps scanning forward to the part's own boundary so every
 * subsequent part in the same request is parsed normally. Only a genuinely
 * malformed or truncated stream (unreadable header, missing boundary, the
 * stream ending mid-part) throws [MultipartParseException] and aborts the
 * whole request -- there is no way to locate a later part's boundary once
 * the stream itself cannot be trusted. Exceeding [maxFiles] also aborts
 * the whole request: a conservative request-level abuse bound, not a
 * per-file outcome.
 */
internal class BoundedMultipartParser(
    private val boundary: String,
    private val maxPartBytes: Long,
    private val tempDirectory: Path,
    private val maxFiles: Int,
) {
    private val delimiter = "\r\n--$boundary".toByteArray(StandardCharsets.US_ASCII)

    fun parse(rawInput: InputStream): MultipartParseOutcome {
        val input = BufferedInputStream(rawInput, 8192)
        val firstLine = readLine(input) ?: return MultipartParseOutcome(emptyList(), emptyList())
        if (firstLine != "--$boundary") throw MultipartParseException("malformed multipart body: missing initial boundary")

        val parts = mutableListOf<ParsedFilePart>()
        val rejected = mutableListOf<RejectedFilePart>()
        try {
            while (true) {
                val headers = readHeaders(input)
                val disposition = headers["content-disposition"]
                    ?: throw MultipartParseException("multipart part missing Content-Disposition")
                val fieldName = extractDispositionParam(disposition, "name")
                val fileName = extractDispositionParam(disposition, "filename")
                val declaredContentType = headers["content-type"]

                if (fieldName != null && fileName != null) {
                    if (parts.size + rejected.size >= maxFiles) {
                        throw MultipartParseException("too many files in one request (max $maxFiles)")
                    }
                    val originalFileName = sanitizeFileName(fileName)
                    val tempPath = Files.createTempFile(tempDirectory, "owner-upload-", ".part")
                    val result = try {
                        Files.newOutputStream(tempPath).use { out -> copyUntilDelimiter(input, delimiter, out, maxPartBytes) }
                    } catch (e: Exception) {
                        runCatching { Files.deleteIfExists(tempPath) }
                        throw e
                    }
                    when (result) {
                        is CopyResult.Ok -> parts += ParsedFilePart(fieldName, originalFileName, declaredContentType, tempPath)
                        CopyResult.TooLarge -> {
                            runCatching { Files.deleteIfExists(tempPath) }
                            rejected += RejectedFilePart(fieldName, originalFileName, "exceeds the maximum accepted size")
                        }
                    }
                } else {
                    copyUntilDelimiter(input, delimiter, null, maxPartBytes)
                }

                val terminator = ByteArray(2)
                if (readFully(input, terminator) < 2) throw MultipartParseException("truncated multipart body")
                when {
                    terminator[0] == '-'.code.toByte() && terminator[1] == '-'.code.toByte() -> return MultipartParseOutcome(parts, rejected)
                    terminator[0] == '\r'.code.toByte() && terminator[1] == '\n'.code.toByte() -> continue
                    else -> throw MultipartParseException("malformed multipart boundary terminator")
                }
            }
        } catch (e: MultipartParseException) {
            parts.forEach { runCatching { Files.deleteIfExists(it.tempPath) } }
            throw e
        } catch (e: IOException) {
            parts.forEach { runCatching { Files.deleteIfExists(it.tempPath) } }
            throw MultipartParseException("failed to read multipart body: ${e.message}")
        }
    }
}

private fun sanitizeFileName(raw: String): String {
    val normalized = raw.replace('\\', '/')
    val lastSegment = normalized.substringAfterLast('/').trim()
    return lastSegment.ifBlank { "unnamed" }
}

private fun extractDispositionParam(disposition: String, paramName: String): String? {
    val regex = Regex("""$paramName="((?:[^"\\]|\\.)*)"""")
    val match = regex.find(disposition) ?: return null
    return match.groupValues[1].replace("\\\"", "\"").replace("\\\\", "\\")
}

private fun readLine(input: InputStream): String? {
    val out = ByteArrayOutputStream()
    var sawAny = false
    while (true) {
        val b = input.read()
        if (b == -1) return if (sawAny) out.toString(StandardCharsets.UTF_8) else null
        sawAny = true
        if (b == '\n'.code) {
            val bytes = out.toByteArray()
            val end = if (bytes.isNotEmpty() && bytes.last() == '\r'.code.toByte()) bytes.size - 1 else bytes.size
            return String(bytes, 0, end, StandardCharsets.UTF_8)
        }
        out.write(b)
    }
}

private fun readHeaders(input: InputStream): Map<String, String> {
    val headers = mutableMapOf<String, String>()
    while (true) {
        val line = readLine(input) ?: throw MultipartParseException("truncated multipart headers")
        if (line.isEmpty()) return headers
        val separator = line.indexOf(':')
        if (separator < 0) throw MultipartParseException("malformed multipart header line")
        headers[line.substring(0, separator).trim().lowercase()] = line.substring(separator + 1).trim()
    }
}

private fun readFully(input: InputStream, buffer: ByteArray): Int {
    var total = 0
    while (total < buffer.size) {
        val n = input.read(buffer, total, buffer.size - total)
        if (n == -1) break
        total += n
    }
    return total
}

private sealed interface CopyResult {
    data class Ok(val bytesWritten: Long) : CopyResult
    data object TooLarge : CopyResult
}

/**
 * Streams bytes from [input] into [sink] (discarded when `null`) until
 * [delimiter] is found; delimiter bytes are consumed but never written.
 * If more than [maxBytes] would be written before the delimiter appears,
 * writing to [sink] stops (already-written bytes are the caller's to
 * discard) but scanning continues until the real delimiter is actually
 * found, so the input stream's position stays correctly aligned for
 * whatever follows -- returning [CopyResult.TooLarge] rather than throwing,
 * so one oversized part never prevents the parser from reading the parts
 * after it. Throws only if the stream ends before the delimiter is ever
 * found (a genuinely truncated/aborted request) -- that failure mode
 * really does abort the whole parse, since the stream can no longer be
 * trusted to locate anything after it.
 */
private fun copyUntilDelimiter(input: InputStream, delimiter: ByteArray, sink: OutputStream?, maxBytes: Long): CopyResult {
    val window = IntArray(delimiter.size)
    var filled = 0
    var written = 0L
    var exceeded = false
    while (true) {
        val b = input.read()
        if (b == -1) throw MultipartParseException("unexpected end of stream (truncated multipart body)")
        if (filled < delimiter.size) {
            window[filled] = b
            filled++
        } else {
            val flushed = window[0]
            for (i in 1 until delimiter.size) window[i - 1] = window[i]
            window[delimiter.size - 1] = b
            if (!exceeded) {
                written++
                if (written > maxBytes) {
                    exceeded = true
                } else {
                    sink?.write(flushed)
                }
            }
        }
        if (filled == delimiter.size) {
            var matches = true
            for (i in delimiter.indices) {
                if (window[i] != (delimiter[i].toInt() and 0xFF)) {
                    matches = false
                    break
                }
            }
            if (matches) return if (exceeded) CopyResult.TooLarge else CopyResult.Ok(written)
        }
    }
}

private val OWNER_PAIRING_PAGE_HTML = """
<!doctype html><html><head><meta charset="utf-8"><title>Pair Parker Owner Device</title></head>
<body><h1>Pair this owner device</h1>
<p>Initiate a one-time pairing code from the authenticated Parker host, then enter it here within five minutes.</p>
<input id="pairingCode" type="password" autocomplete="one-time-code"><button id="pairButton">Pair device</button>
<p id="pairStatus"></p><script>
document.getElementById('pairButton').onclick = async () => {
 const code = document.getElementById('pairingCode').value;
 const response = await fetch('/owner/pair', {method:'POST', credentials:'same-origin', body:code});
 document.getElementById('pairingCode').value = '';
 if (response.ok) location.reload(); else document.getElementById('pairStatus').textContent='Pairing denied or expired.';
};
</script></body></html>
""".trimIndent()

/** The minimum useful owner-facing browser page: select files, upload, process, run OCR. */
private val OWNER_EVIDENCE_PAGE_HTML = """
<!doctype html>
<html>
<head>
<meta charset="utf-8">
<title>Parker Owner Evidence Upload</title>
<style>
  body { font-family: system-ui, sans-serif; max-width: 900px; margin: 2rem auto; padding: 0 1rem; background: #111; color: #eee; }
  h1 { font-size: 1.3rem; }
  input[type=text], input[type=password] { padding: 0.4rem; box-sizing: border-box; }
  table { width: 100%; border-collapse: collapse; margin-top: 1rem; }
  th, td { text-align: left; padding: 0.4rem; border-bottom: 1px solid #333; font-size: 0.85rem; }
  button { padding: 0.3rem 0.7rem; margin-right: 0.3rem; }
  .row-actions button { font-size: 0.8rem; }
  #status { color: #f88; }
  .note { color: #fd8; font-size: 0.85rem; }
  .content-panel { background: #1a1a1a; padding: 0.75rem; margin: 0.5rem 0; border: 1px solid #333; }
  .content-panel p { margin: 0.25rem 0; font-size: 0.85rem; }
  .content-panel .note { color: #fd8; }
  .extracted-text { max-height: 24rem; overflow: auto; white-space: pre-wrap; word-break: break-word; background: #000; padding: 0.5rem; font-size: 0.8rem; }
  .content-panel table { margin-top: 0.5rem; }
  .library-controls { display: flex; flex-wrap: wrap; gap: 0.6rem; align-items: center; }
  .document-name { font-weight: 650; }
  .technical-id { color: #aaa; font-size: 0.75rem; margin-top: 0.2rem; }
  .status-label { white-space: nowrap; }
</style>
</head>
<body>
<h1>Parker Owner Evidence Upload</h1>
<p><button id="logoutButton">Log out</button></p>
<p><button id="checkEnhancedReadinessButton">Check enhanced transcription readiness</button> <span id="enhancedReadinessStatus" class="note"></span></p>
<p>Select Files: <input type="file" id="filePicker" multiple> <button id="uploadButton">Upload</button></p>
<p><button id="refreshEvidenceButton">Refresh existing evidence</button></p>
<p id="status"></p>
<div class="library-controls">
  <label>Search <input id="evidenceSearch" type="search" placeholder="Filename or Evidence ID"></label>
  <label>Sort <select id="evidenceSort"><option value="newest">Newest first</option><option value="oldest">Oldest first</option><option value="filename">Filename A–Z</option><option value="status">Status</option></select></label>
  <label>Filter <select id="evidenceFilter"><option value="all">All</option><option value="needs-processing">Needs processing</option><option value="processed">Processed</option><option value="human-reviewed">Human reviewed</option><option value="corrected">Corrected</option></select></label>
</div>
<table>
  <thead><tr><th>Document</th><th>Uploaded / Imported</th><th>Status</th><th>Pages</th><th>Size</th><th>Analyse</th><th>Actions</th></tr></thead>
  <tbody id="rows"></tbody>
</table>
<h2>Analyse Selected Documents</h2>
<p class="note">Select one or more processed documents above (checkbox in the "Analyse" column), enter an instruction, then click Analyse. The result is provider-generated material for human review -- not Evidence, Memory, Knowledge, or canonical Parker truth. It is not saved unless you explicitly click Save Analysis afterward.</p>
<p><textarea id="analysisInstruction" rows="3" style="width:100%; box-sizing:border-box;" placeholder="What would you like Parker to look for or summarise across the selected documents?"></textarea></p>
<p><button id="analyseButton">Analyse Selected</button></p>
<div id="analysisResults"></div>
<h2>Saved Analyses</h2>
<p class="note">Analyses you have explicitly saved. Selecting one retrieves it from durable storage -- it never re-runs analysis or invokes the model.</p>
<p><button id="refreshSavedAnalysesButton">Refresh</button></p>
<table>
  <thead><tr><th>Saved</th><th>Instruction</th><th>Actions</th></tr></thead>
  <tbody id="savedAnalysisRows"></tbody>
</table>
<div id="savedAnalysisDetail"></div>
<script>
let rows = [];
let expandedIndex = null;
const detailsExpanded = new Set();
let enhancedReadiness = { status: 'DISABLED', message: 'Enhanced transcription readiness has not been loaded.' };

document.getElementById('logoutButton').onclick = async () => {
  await fetch('/owner/logout', { method: 'POST', credentials: 'same-origin' });
  location.reload();
};

function isInternalUploadName(name) {
  return /^owner-upload-[A-Za-z0-9_-]+\.part$/i.test((name || '').trim());
}

function documentName(row) {
  const name = (row.originalFileName || '').trim();
  return name && !isInternalUploadName(name) ? name : 'Unnamed evidence';
}

function shortEvidenceId(id) {
  if (!id) return '';
  return id.length <= 20 ? id : id.slice(0, 12) + '…' + id.slice(-8);
}

function humanStatus(row) {
  if (row.correctedRepresentation || row.correctedRepresentationAvailable) return 'Corrected representation available';
  const review = row.humanFidelityStatus || row.humanFidelityReview;
  if (review && review.effectiveReviewState === 'HUMAN_REVIEWED_WITH_DISCREPANCY') return 'Human reviewed — discrepancies found';
  if (review && review.effectiveReviewState === 'HUMAN_REVIEWED_PASS') return 'Human reviewed';
  return ({UPLOADING:'Uploading', IMPORTED:'Registered', READY_TO_PROCESS:'Ready to process', PROCESSING:'Processing',
    TIER_A_COMPLETE:'Processed', TIER_B_DURABLE_COMPLETE:'Processed', COMPLETE:'Processed', REQUIRES_OCR:'Enhanced transcription available',
    OCR_PROCESSING:'Processing', IMPORT_FAILED:'Failed closed', FAILED:'Failed closed'})[row.status] || (row.status || 'Unknown');
}

function isProcessed(row) { return ['TIER_A_COMPLETE','TIER_B_DURABLE_COMPLETE','COMPLETE'].includes(row.status); }
function filterMatches(row, filter) {
  if (filter === 'needs-processing') return ['IMPORTED','READY_TO_PROCESS','REQUIRES_OCR'].includes(row.status);
  if (filter === 'processed') return isProcessed(row);
  if (filter === 'human-reviewed') return !!(row.humanFidelityStatus || row.humanFidelityReview);
  if (filter === 'corrected') return !!(row.correctedRepresentation || row.correctedRepresentationAvailable);
  return true;
}

function visibleRows() {
  const query = document.getElementById('evidenceSearch').value.trim().toLocaleLowerCase();
  const filter = document.getElementById('evidenceFilter').value;
  const sort = document.getElementById('evidenceSort').value;
  const result = rows.map((row, index) => ({row, index})).filter(({row}) =>
    filterMatches(row, filter) && (!query || documentName(row).toLocaleLowerCase().includes(query) ||
      (row.evidenceArtifactId || '').toLocaleLowerCase().includes(query)));
  result.sort((a, b) => {
    if (sort === 'filename') return documentName(a.row).localeCompare(documentName(b.row));
    if (sort === 'status') return humanStatus(a.row).localeCompare(humanStatus(b.row));
    const left = Date.parse(a.row.registeredAt || '') || 0;
    const right = Date.parse(b.row.registeredAt || '') || 0;
    return sort === 'oldest' ? left - right : right - left;
  });
  return result;
}

function appendTextCell(tr, value, className) {
  const td = document.createElement('td');
  if (className) td.className = className;
  td.textContent = value;
  tr.appendChild(td);
  return td;
}

function render() {
  document.getElementById('enhancedReadinessStatus').textContent = enhancedReadiness.message;
  const tbody = document.getElementById('rows');
  tbody.innerHTML = '';
  visibleRows().forEach(({row, index}) => {
    const tr = document.createElement('tr');
    const actions = document.createElement('td');
    actions.className = 'row-actions';
    if (row.evidenceArtifactId && !row.externalResultRow) {
      const acquire = document.createElement('button');
      acquire.textContent = 'Process document';
      acquire.title = 'Show Parker’s governed acquisition selection; this does not process the document.';
      acquire.onclick = () => loadAcquisitionDecision(index);
      actions.appendChild(acquire);
    }
    if (row.status === 'REQUIRES_OCR') {
      const b = document.createElement('button');
      b.textContent = 'Run local OCR';
      b.title = 'Legacy/manual compatibility control; this is not a fallback from governed acquisition.';
      b.disabled = true;
      b.onclick = () => ocrRow(index);
      actions.appendChild(b);
    }
    // Document Ingestion -- Tier B Durable OCR Derivative Content. A separate, explicit owner
    // action, available both before and after ordinary transient OCR. A transient COMPLETE result
    // carries no durable generation identity and remains ineligible for analysis; only this action's
    // existing TIER_B_DURABLE_COMPLETE + real ocrDerivativeGenerationId path changes that.
    if (row.status === 'REQUIRES_OCR' || row.status === 'COMPLETE') {
      const bd = document.createElement('button');
      bd.textContent = 'Run local OCR (Durable)';
      bd.title = 'Legacy/manual specialist operation; this is not automatic fallback.';
      bd.disabled = true;
      bd.onclick = () => ocrDurableRow(index);
      actions.appendChild(bd);
    }
    if (row.evidenceArtifactId && !row.externalResultRow) {
      const external = document.createElement('button');
      external.textContent = row.externalProcessing ? 'Enhanced transcription processing…' : 'Run enhanced transcription';
      external.dataset.classification = 'legacy-manual-compatibility';
      external.disabled = true;
      external.title = enhancedReadiness.status === 'READY' ? 'Explicitly submit this evidence for enhanced transcription' : enhancedReadiness.message;
      external.onclick = () => transcribeExternalRow(index);
      actions.appendChild(external);
    }
    if (row.status === 'TIER_A_COMPLETE' && row.derivativeGenerationId) {
      const b = document.createElement('button');
      b.textContent = expandedIndex === index ? 'Hide Extracted Content' : 'View Extracted Content';
      b.onclick = () => viewContent(index);
      actions.appendChild(b);
    }
    if (row.status === 'TIER_B_DURABLE_COMPLETE' && row.ocrDerivativeGenerationId) {
      const b = document.createElement('button');
      b.textContent = expandedIndex === index ? 'Hide Extracted Content' : 'View Extracted Content';
      b.onclick = () => viewOcrContent(index);
      actions.appendChild(b);
    }
    const documentTd = document.createElement('td');
    const name = document.createElement('div');
    name.className = 'document-name';
    name.textContent = documentName(row);
    documentTd.appendChild(name);
    if (row.evidenceArtifactId) {
      const id = document.createElement('div');
      id.className = 'technical-id';
      id.textContent = 'Evidence: ' + shortEvidenceId(row.evidenceArtifactId);
      documentTd.appendChild(id);
    }
    tr.appendChild(documentTd);
    appendTextCell(tr, row.registeredAt ? new Date(row.registeredAt).toLocaleString() : 'Unknown');
    appendTextCell(tr, humanStatus(row), 'status-label');
    appendTextCell(tr, row.pageCount == null ? '—' : String(row.pageCount));
    appendTextCell(tr, formatBytes(row.byteLength));

    // Minimum Production Document Pipeline -- a row is selectable for analysis once it carries a
    // durable derivative generation identity (Tier A or Tier B durable OCR) the owner can already
    // retrieve content for -- never for a row that has not reached that state yet.
    const analyseTd = document.createElement('td');
    if ((row.status === 'TIER_A_COMPLETE' && row.derivativeGenerationId && !row.providerRegionTranscription) ||
        (row.status === 'TIER_B_DURABLE_COMPLETE' && row.ocrDerivativeGenerationId)) {
      const cb = document.createElement('input');
      cb.type = 'checkbox';
      cb.setAttribute('aria-label', 'Select ' + documentName(row) + ' for analysis');
      cb.checked = !!row.selectedForAnalysis;
      cb.onchange = () => {
        row.selectedForAnalysis = cb.checked;
        if (!cb.checked) row.acknowledgesUnverifiedExternalTranscription = false;
        render();
      };
      analyseTd.appendChild(cb);
      if (row.externalResultRow) {
        const acknowledgement = document.createElement('label');
        const acknowledgementCheckbox = document.createElement('input');
        acknowledgementCheckbox.type = 'checkbox';
        acknowledgementCheckbox.checked = !!row.acknowledgesUnverifiedExternalTranscription;
        acknowledgementCheckbox.onchange = () => { row.acknowledgesUnverifiedExternalTranscription = acknowledgementCheckbox.checked; };
        acknowledgement.appendChild(acknowledgementCheckbox);
        acknowledgement.appendChild(document.createTextNode(' I acknowledge this exact unverified machine transcription'));
        analyseTd.appendChild(document.createElement('br'));
        analyseTd.appendChild(acknowledgement);
      }
    }
    const details = document.createElement('button');
    details.textContent = detailsExpanded.has(row.evidenceArtifactId || index) ? 'Hide details' : 'Details';
    details.onclick = () => {
      const key = row.evidenceArtifactId || index;
      if (detailsExpanded.has(key)) detailsExpanded.delete(key); else detailsExpanded.add(key);
      render();
    };
    actions.appendChild(details);
    tr.appendChild(analyseTd);
    tr.appendChild(actions);
    tbody.appendChild(tr);

    if (detailsExpanded.has(row.evidenceArtifactId || index)) {
      const detailsTr = document.createElement('tr');
      const detailsTd = document.createElement('td');
      detailsTd.colSpan = 7;
      detailsTd.appendChild(buildEvidenceDetails(row));
      detailsTr.appendChild(detailsTd);
      tbody.appendChild(detailsTr);
    }

    if (row.acquisitionDecision || row.acquisitionError || row.acquisitionResult) {
      const decisionTr = document.createElement('tr');
      const decisionTd = document.createElement('td');
      decisionTd.colSpan = 7;
      decisionTd.appendChild(buildAcquisitionPanel(row, index));
      decisionTr.appendChild(decisionTd);
      tbody.appendChild(decisionTr);
    }

    if (expandedIndex === index && (row.content || row.contentError)) {
      const detailTr = document.createElement('tr');
      const detailTd = document.createElement('td');
      detailTd.colSpan = 7;
      if (row.content) {
        detailTd.appendChild(buildContentPanel(row.content));
      } else {
        const p = document.createElement('p');
        p.className = 'note';
        p.textContent = 'Could not retrieve extracted content: ' + row.contentError;
        detailTd.appendChild(p);
      }
      detailTr.appendChild(detailTd);
      tbody.appendChild(detailTr);
    }
    if (expandedIndex === index && (row.ocrContent || row.ocrContentError)) {
      const detailTr = document.createElement('tr');
      const detailTd = document.createElement('td');
      detailTd.colSpan = 7;
      if (row.ocrContent) {
        detailTd.appendChild(buildOcrContentPanel(row.ocrContent, row.ocrDerivativeGenerationId));
      } else {
        const p = document.createElement('p');
        p.className = 'note';
        p.textContent = 'Could not retrieve durable OCR content: ' + row.ocrContentError;
        detailTd.appendChild(p);
      }
      detailTr.appendChild(detailTd);
      tbody.appendChild(detailTr);
    }
  });
}

function formatBytes(value) {
  if (!Number.isFinite(Number(value))) return 'Unknown';
  const bytes = Number(value);
  if (bytes < 1024) return bytes + ' B';
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
}

function buildEvidenceDetails(row) {
  const panel = document.createElement('div');
  panel.className = 'content-panel';
  appendField(panel, 'Evidence ID', row.evidenceArtifactId || 'Not registered');
  appendField(panel, 'Original/source filename', documentName(row));
  if (isInternalUploadName(row.originalFileName)) appendField(panel, 'Raw storage name', row.originalFileName);
  if (row.sha256) appendField(panel, 'Content SHA-256', row.sha256);
  appendField(panel, 'Byte size', String(row.byteLength));
  if (row.mediaType) appendField(panel, 'Media type', row.mediaType);
  if (row.registeredAt) appendField(panel, 'Uploaded / imported', row.registeredAt);
  appendField(panel, 'Internal processing state', row.status || 'UNKNOWN');
  if (row.derivativeGenerationId) appendField(panel, 'Representation', 'Tier A — ' + row.derivativeGenerationId);
  if (row.ocrDerivativeGenerationId) appendField(panel, 'Representation', 'Durable OCR — ' + row.ocrDerivativeGenerationId);
  if (row.pageCount != null) appendField(panel, 'Page count', String(row.pageCount));
  const fidelity = row.humanFidelityStatus || row.humanFidelityReview;
  if (fidelity) {
    appendField(panel, 'Human fidelity review', fidelity.effectiveReviewState || fidelity.reviewState || 'Available');
    if (fidelity.materialDiscrepancyCount != null) appendField(panel, 'Material discrepancies', String(fidelity.materialDiscrepancyCount));
    if (fidelity.systematicPatternCount != null) appendField(panel, 'Systematic patterns', String(fidelity.systematicPatternCount));
    if (fidelity.sourceConfirmedEligibility) appendField(panel, 'Source-confirmed eligibility', fidelity.sourceConfirmedEligibility);
  }
  if (row.correctedRepresentation || row.correctedRepresentationAvailable) appendField(panel, 'Corrected representation', 'Available');
  if (row.message) appendField(panel, 'Result', row.message);
  return panel;
}

function escapeHtml(s) {
  return s.replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
}

// Every field below is inserted via textContent (never innerHTML), so extracted text or metadata
// (filenames, headers, warnings -- all owner/adversary-influenceable) can never be interpreted as
// HTML or script, no matter what characters it contains.
function appendField(container, label, value) {
  const p = document.createElement('p');
  p.textContent = label + ': ' + value;
  container.appendChild(p);
}

function appendWarnings(container, warnings) {
  if (!warnings || !warnings.length) return;
  const p = document.createElement('p');
  p.textContent = 'Warnings: ' + warnings.join('; ');
  container.appendChild(p);
}

function appendProducer(container, producer) {
  if (!producer) return;
  let text = producer.pluginIdentity + ' ' + producer.pluginVersion;
  if (producer.adapterIdentity) text += ' / ' + producer.adapterIdentity + ' ' + producer.adapterVersion;
  if (producer.modelIdentity) text += ' / model ' + producer.modelIdentity + ' ' + producer.modelVersion;
  appendField(container, 'Producer', text);
}

function appendTable(container, headerCells, bodyRows) {
  const table = document.createElement('table');
  if (headerCells) {
    const thead = document.createElement('thead');
    const headRow = document.createElement('tr');
    headerCells.forEach(h => { const th = document.createElement('th'); th.textContent = h; headRow.appendChild(th); });
    thead.appendChild(headRow);
    table.appendChild(thead);
  }
  const tbody2 = document.createElement('tbody');
  bodyRows.forEach(row => {
    const tr2 = document.createElement('tr');
    row.forEach(cell => { const td2 = document.createElement('td'); td2.textContent = cell; tr2.appendChild(td2); });
    tbody2.appendChild(tr2);
  });
  table.appendChild(tbody2);
  container.appendChild(table);
}

function appendExtractedText(container, label, text) {
  const div = document.createElement('div');
  div.textContent = label;
  container.appendChild(div);
  const pre = document.createElement('pre');
  pre.className = 'extracted-text';
  pre.textContent = text;
  container.appendChild(pre);
}

function buildContentPanel(content) {
  const container = document.createElement('div');
  container.className = 'content-panel';
  if (content.kind === 'PDF') {
    appendField(container, 'Page count', content.pageCount != null ? String(content.pageCount) : 'unknown');
    appendField(container, 'Page/text association available', String(content.pageTextAssociationAvailable));
    appendField(container, 'Completeness', content.completenessState);
    appendWarnings(container, content.warnings);
    appendProducer(container, content.producer);
    const note = document.createElement('p');
    note.className = 'note';
    note.textContent = 'Whole-document text -- does not imply page-level association or coordinates.';
    container.appendChild(note);
    appendExtractedText(container, 'Extracted text:', content.documentText);
  } else if (content.kind === 'CSV') {
    appendField(container, 'Total rows', String(content.totalRowCount));
    if (content.rowsTruncatedForDisplay) {
      const note = document.createElement('p');
      note.className = 'note';
      note.textContent = 'PREVIEW -- showing ' + content.previewRows.length + ' of ' + content.totalRowCount + ' rows.';
      container.appendChild(note);
    }
    appendField(container, 'Completeness', content.completenessState);
    appendWarnings(container, content.warnings);
    appendProducer(container, content.producer);
    appendTable(container, content.headers, content.previewRows);
  } else if (content.kind === 'EML') {
    appendField(container, 'From', content.from || '');
    appendField(container, 'To', content.to || '');
    appendField(container, 'Cc', content.cc || '');
    appendField(container, 'Subject', content.subject || '');
    appendField(container, 'Date', content.rawDate || '');
    appendField(container, 'Attachment candidates', String(content.attachmentCandidateCount));
    appendField(container, 'Completeness', content.completenessState);
    appendWarnings(container, content.warnings);
    appendProducer(container, content.producer);
    content.bodyAlternatives.forEach(alt => appendExtractedText(container, 'Body (' + alt.mediaType + '):', alt.text));
    if (content.attachmentCandidates.length) {
      const label = document.createElement('div');
      label.textContent = 'Attachments:';
      container.appendChild(label);
      const ul = document.createElement('ul');
      content.attachmentCandidates.forEach(a => {
        const li = document.createElement('li');
        li.textContent = (a.filename || '(unnamed)') + ' -- ' + a.declaredMimeType + ' -- ' + a.byteLength + ' bytes';
        ul.appendChild(li);
      });
      container.appendChild(ul);
    }
  } else if (content.kind === 'DOCX') {
    appendField(container, 'Completeness', content.completenessState);
    appendWarnings(container, content.warnings);
    appendProducer(container, content.producer);
    appendExtractedText(container, 'Paragraphs:', content.paragraphs.join('\n\n'));
    if (content.headers.length) appendExtractedText(container, 'Headers:', content.headers.join('\n'));
    if (content.footers.length) appendExtractedText(container, 'Footers:', content.footers.join('\n'));
    if (content.tables.length) {
      const tlabel = document.createElement('div');
      tlabel.textContent = 'Tables (' + content.tables.length + '):';
      container.appendChild(tlabel);
      content.tables.forEach(t => appendTable(container, null, t));
    }
  } else if (content.kind === 'REGION_TRANSCRIPTION') {
    const providerHeading = document.createElement('h3');
    providerHeading.textContent = 'Raw provider representation';
    container.appendChild(providerHeading);
    appendField(container, 'Representation', 'REGION_TRANSCRIPTION / immutable provider output');
    appendField(container, 'Derivative generation', content.derivativeGenerationId);
    appendField(container, 'Source evidence', content.evidenceArtifactId);
    appendField(container, 'Provider', content.provider);
    appendField(container, 'Model', content.model);
    appendField(container, 'Parker source order', (content.parkerSourceOrder || []).join(', '));
    (content.transcriptionBlocks || []).forEach((text, i) =>
      appendExtractedText(container, 'Provider region ' + (i + 1) + ':', text));

    const review = content.humanFidelityStatus;
    const reviewHeading = document.createElement('h3');
    reviewHeading.textContent = 'Human fidelity review status';
    container.appendChild(reviewHeading);
    appendField(container, 'Review state', review.effectiveReviewState || 'UNREVIEWED');
    appendField(container, 'Coverage', review.coverage || 'NONE');
    appendField(container, 'Material discrepancies', String(review.materialDiscrepancyCount));
    appendField(container, 'Systematic patterns', String(review.systematicPatternCount));
    appendField(container, 'Unresolved conflict', String(review.unresolvedConflict));
    appendField(container, 'Provider source-confirmed eligibility', review.sourceConfirmedEligibility);
    if (review.sourceConfirmedDenialReason) appendField(container, 'Denial reason', review.sourceConfirmedDenialReason);

    const corrected = content.humanCorrectedRepresentation;
    const correctedHeading = document.createElement('h3');
    correctedHeading.textContent = 'Human-corrected representation';
    container.appendChild(correctedHeading);
    if (!corrected) {
      appendField(container, 'Availability', 'NOT AVAILABLE');
    } else {
      appendField(container, 'Representation', corrected.representationKind + ' / separate immutable representation');
      appendField(container, 'Derivative generation', corrected.derivativeGenerationId);
      appendField(container, 'Canonical human review', corrected.reviewId);
      appendField(container, 'Corrections applied', String(corrected.correctionCount));
      appendField(container, 'Source-confirmed eligibility', corrected.sourceConfirmedEligibility);
      if (corrected.sourceConfirmedDenialReason) appendField(container, 'Denial reason', corrected.sourceConfirmedDenialReason);
      (corrected.correctedTranscriptionBlocks || []).forEach((text, i) =>
        appendExtractedText(container, 'Human-corrected region ' + (i + 1) + ':', text));
    }
  }
  return container;
}

// Document Ingestion -- Tier B Durable OCR Derivative Content. Mirrors buildContentPanel's own
// discipline exactly: every field inserted via appendField/appendExtractedText (textContent, never
// innerHTML), so OCR-recognised text can never be interpreted as HTML or script (Tier B scope lock
// §27), no matter what characters the source document contained.
function buildOcrContentPanel(content, derivativeGenerationId) {
  const container = document.createElement('div');
  container.className = 'content-panel';
  appendField(container, 'Outcome', content.outcomeKind);
  if (content.degradationReason) appendField(container, 'Degradation reason', content.degradationReason);
  appendField(container, 'Fidelity', content.fidelity);
  appendField(container, 'Completeness', content.completenessState);
  if (derivativeGenerationId) appendField(container, 'Derivative Generation ID', derivativeGenerationId);
  if (content.sourceEvidenceArtifactId) appendField(container, 'Source Evidence ID', content.sourceEvidenceArtifactId);
  if (content.providerIdentity) appendField(container, 'Provider', content.providerIdentity + ' (provenance only — not verification)');
  if (content.returnedModelIdentifier) appendField(container, 'Returned model', content.returnedModelIdentifier);
  if (content.transcriptionProfileIdentity) appendField(container, 'Transcription profile', content.transcriptionProfileIdentity);
  if (content.externalTranscription) {
    appendField(container, 'Machine transcription — unverified', 'Machine transcription — unverified');
    appendField(container, 'Safety warning', 'Fluent machine transcription may contain plausible text that is inconsistent with the source.');
    appendField(container, 'Human review state', (content.humanReviewStates || ['UNREVIEWED']).join(', '));
  }
  if (content.modelSnapshot) appendField(container, 'Model snapshot', content.modelSnapshot);
  if (content.requestedPages) appendField(container, 'Requested pages', content.requestedPages.join(', ') || 'None');
  if (content.submittedPages) appendField(container, 'Submitted pages', content.submittedPages.join(', ') || 'None');
  if (content.returnedPages) appendField(container, 'Returned pages', content.returnedPages.join(', ') || 'None');
  if (content.containsUncertaintyOrIllegibility) appendField(container, 'Qualification', 'Transcription contains uncertain or illegible text');
  if (content.pageOutcomes && content.pageOutcomes.length) {
    appendField(container, 'Page outcomes', content.pageOutcomes.map(p => 'Page ' + p.pageNumber + ': ' + p.outcome + (p.reason ? ' (' + p.reason + ')' : '')).join('; '));
  }
  appendWarnings(container, content.warnings);
  appendProducer(container, content.producer);
  appendExtractedText(container, 'Recognised text:', content.recognisedText);
  if (content.segments && content.segments.length) {
    const label = document.createElement('div');
    label.textContent = 'Segments (' + content.segments.length + '):';
    container.appendChild(label);
    content.segments.forEach(s => {
      appendExtractedText(container, (s.pageNumber != null ? 'Page ' + s.pageNumber : 'Segment') + ':', s.text);
    });
  }
  return container;
}

function authHeaders() {
  return {};
}

document.getElementById('uploadButton').onclick = async () => {
  const files = document.getElementById('filePicker').files;
  if (!files.length) return;
  const formData = new FormData();
  const selected = [];
  for (const f of files) {
    formData.append('files', f, f.name);
    selected.push({ originalFileName: f.name, byteLength: f.size, status: 'UPLOADING' });
  }
  rows = rows.concat(selected);
  render();
  document.getElementById('status').textContent = '';
  try {
    const resp = await fetch('/owner/evidence', { method: 'POST', headers: authHeaders(), body: formData });
    if (resp.status === 401) { document.getElementById('status').textContent = 'Owner session expired or unavailable.'; return; }
    const results = await resp.json();
    const startIndex = rows.length - selected.length;
    results.forEach((result, i) => {
      const target = rows[startIndex + i];
      target.status = result.status;
      target.evidenceArtifactId = result.evidenceArtifactId;
      target.message = result.message;
    });
    const registered = results.filter(result => result.status === 'IMPORTED').length;
    if (registered) {
      await loadExistingEvidence('Registered ' + registered + (registered === 1 ? ' document.' : ' documents.'));
    } else {
      render();
    }
  } catch (e) {
    document.getElementById('status').textContent = 'Upload failed: ' + e;
  }
};

async function loadExistingEvidence(successMessage) {
  const status = document.getElementById('status');
  try {
    const resp = await fetch('/owner/evidence', { method: 'GET', headers: authHeaders() });
    if (resp.status === 401) { rows = []; render(); return; }
    const result = await resp.json();
    if (!resp.ok) { status.textContent = result.error || 'Existing evidence list unavailable.'; return; }
    const existingById = new Map(rows.filter(r => r.evidenceArtifactId).map(r => [r.evidenceArtifactId, r]));
    rows = (result.evidence || []).map(item => Object.assign({
      originalFileName: item.originalFileName,
      byteLength: item.byteLength,
      status: 'READY_TO_PROCESS',
      evidenceArtifactId: item.evidenceArtifactId,
      message: 'Durably registered evidence',
      sha256: item.sha256,
      mediaType: item.mediaType,
      registeredAt: item.registeredAt,
    }, existingById.get(item.evidenceArtifactId) || {}));
    status.textContent = successMessage || '';
    render();
  } catch (e) { status.textContent = 'Existing evidence list request failed safely.'; }
}

document.getElementById('refreshEvidenceButton').onclick = () => loadExistingEvidence();
document.getElementById('evidenceSearch').oninput = render;
document.getElementById('evidenceSort').onchange = render;
document.getElementById('evidenceFilter').onchange = render;

function buildAcquisitionPanel(row, index) {
  const panel = document.createElement('div');
  panel.className = 'content-panel';
  const heading = document.createElement('h3');
  heading.textContent = 'Governed acquisition decision';
  panel.appendChild(heading);
  if (row.acquisitionError) { appendField(panel, 'Status', row.acquisitionError); return panel; }
  const d = row.acquisitionResult || row.acquisitionDecision;
  appendField(panel, 'Status', d.status);
  if (d.explanation) appendField(panel, 'Why Parker selected this mechanism', d.explanation);
  if (d.status === 'PROPOSED') {
    appendField(panel, 'External transmission disclosure', d.disclosure);
    appendField(panel, 'Evidence-specific egress authorization', d.egressAuthorization);
    appendField(panel, 'Next step', 'Owner review of this proposal; authorization remains a separate action.');
  }
  if (d.source) {
    appendField(panel, 'Evidence filename', row.name || 'UNKNOWN');
    appendField(panel, 'Source', 'Original Parker evidence artifact ' + d.source.evidenceArtifactId);
    appendField(panel, 'Media type', d.source.mediaType || 'UNKNOWN');
    appendField(panel, 'Native searchable text', d.source.nativeSearchableText);
    appendField(panel, 'Image/scanned', d.source.imageOnlyOrScanned);
    appendField(panel, 'Handwriting', d.source.handwriting);
    appendField(panel, 'Complex layout', d.source.complexLayout);
    appendField(panel, 'Tables', d.source.tables);
  }
  if (d.capability) {
    appendField(panel, 'Selected mechanism', d.capability.mechanism);
    appendField(panel, 'Capability', d.capability.capabilityId);
    appendField(panel, 'Execution', d.capability.executionLocation);
    appendField(panel, 'External processing required', d.capability.externalEgressRequired ? 'YES' : 'NO');
    if (d.capability.provider) appendField(panel, 'Provider', d.capability.provider);
    if (d.capability.modelRule) appendField(panel, 'Model rule', d.capability.modelRule);
    if (d.capability.profile) appendField(panel, 'Profile', d.capability.profile);
    if (d.capability.configurationIdentity) appendField(panel, 'Configuration', d.capability.configurationIdentity);
    if (d.capability.processingProfile) appendField(panel, 'Processing profile', d.capability.processingProfile);
    if (d.capability.instructionSha256) appendField(panel, 'Instruction digest', d.capability.instructionSha256);
    if (d.capability.schemaSha256) appendField(panel, 'Schema digest', d.capability.schemaSha256);
    if (d.capability.reasoningEffort) appendField(panel, 'Reasoning effort', d.capability.reasoningEffort);
    if (d.capability.store !== null && d.capability.store !== undefined) appendField(panel, 'Provider storage', d.capability.store ? 'ENABLED' : 'DISABLED');
    if (d.capability.pdfDetail) appendField(panel, 'PDF detail', d.capability.pdfDetail);
    if (d.capability.imageDetail) appendField(panel, 'Image detail', d.capability.imageDetail);
    appendField(panel, 'Processing representation', d.capability.representation);
    appendField(panel, 'Availability', d.capability.availability);
    appendField(panel, 'Routing reasons', (d.capability.selectionReasons || []).join(', '));
  }
  if (d.reasons) appendField(panel, 'Why execution is unavailable', d.reasons.join(', '));
  if (d.capabilityIds) appendField(panel, 'Equally suitable capabilities', d.capabilityIds.join(', '));
  if (d.derivativeGenerationId) {
    appendField(panel, 'Derivative generation', d.derivativeGenerationId);
    appendField(panel, 'Fidelity', d.fidelity);
    appendField(panel, 'Completeness', d.completeness);
    appendField(panel, 'Human review', d.humanReviewState);
    appendField(panel, 'Authoritative source link', d.sourceLink);
    if (d.capability && d.capability.executionLocation === 'EXTERNAL') {
      appendField(panel, 'Machine transcription — unverified', 'Machine transcription — unverified');
      appendField(panel, 'Safety warning', 'Fluent machine transcription may contain plausible text that is inconsistent with the source.');
    }
  } else if (d.status === 'PROPOSED' && d.authorizationAvailable) {
    const authorize = document.createElement('button');
    authorize.textContent = 'Authorize external transcription';
    authorize.onclick = () => authorizeRegionTranscription(index, d);
    panel.appendChild(authorize);
  } else if (d.status === 'PROPOSED' && d.executeAvailable) {
    const execute = document.createElement('button');
    execute.textContent = 'Execute external transcription';
    execute.onclick = () => executeRegionTranscription(index, d);
    panel.appendChild(execute);
  } else if (d.status === 'SELECTED' && d.executeAvailable) {
    const execute = document.createElement('button');
    execute.textContent = 'Execute selected acquisition';
    execute.onclick = () => executeAcquisition(index, d.capability.capabilityId);
    panel.appendChild(execute);
  }
  return panel;
}

async function loadAcquisitionDecision(index, preserveExecutionError = false) {
  const row = rows[index];
  if (!preserveExecutionError) row.acquisitionError = null;
  row.acquisitionResult = null;
  try {
    const resp = await fetch(`/owner/evidence/${'$'}{row.evidenceArtifactId}/acquisition`, { method: 'GET', headers: authHeaders() });
    const result = await resp.json();
    if (!resp.ok) row.acquisitionError = result.error || 'Acquisition decision unavailable.';
    else {
      row.acquisitionDecision = result;
      if (result.derivativeGenerationId) {
        row.derivativeGenerationId = result.derivativeGenerationId;
        row.status = 'TIER_A_COMPLETE';
        row.tierAFormat = 'REGION_TRANSCRIPTION';
        row.providerRegionTranscription = true;
        row.message = 'Governed transcription available';
      }
    }
  } catch (e) { row.acquisitionError = 'Acquisition decision request failed safely.'; }
  render();
}

async function authorizeRegionTranscription(index, decision) {
  const row = rows[index];
  const disclosure = decision.disclosure || 'Selected authoritative PDF evidence crops will be transmitted to OpenAI for literal transcription.';
  const confirmed = window.confirm(
    'Authorize external transcription for evidence "' + (row.name || row.evidenceArtifactId) + '" (' + row.evidenceArtifactId + ')?\n\n' +
    'Provider: OpenAI\n' + disclosure + '\n\nThis creates authorization only; it does not transmit or execute.'
  );
  if (!confirmed) return;
  try {
    const resp = await fetch(`/owner/evidence/${'$'}{row.evidenceArtifactId}/authorize-region-transcription`, {
      method: 'POST', headers: authHeaders(),
    });
    const result = await resp.json();
    if (!resp.ok) row.acquisitionError = result.detail || 'Authorization was not created.';
    await loadAcquisitionDecision(index);
  } catch (e) { row.acquisitionError = 'Authorization request failed safely.'; render(); }
}

async function executeAcquisition(index, expectedCapabilityId) {
  const row = rows[index];
  try {
    const resp = await fetch(`/owner/evidence/${'$'}{row.evidenceArtifactId}/acquire`, {
      method: 'POST', headers: Object.assign({ 'Content-Type': 'application/json' }, authHeaders()),
      body: JSON.stringify({ expectedCapabilityId }),
    });
    const result = await resp.json();
    if (result.status === 'STALE_DECISION') {
      row.acquisitionDecision = result.currentDecision;
      row.acquisitionError = 'The acquisition decision changed. Review the current decision before executing.';
    } else if (!resp.ok) row.acquisitionError = result.reason || result.error || 'Acquisition failed safely.';
    else {
      row.acquisitionResult = result; row.acquisitionError = null;
      if (result.derivativeGenerationId) {
        row.derivativeGenerationId = result.derivativeGenerationId;
        row.status = 'TIER_A_COMPLETE';
        row.tierAFormat = 'REGION_TRANSCRIPTION';
        row.providerRegionTranscription = true;
        row.message = 'Governed transcription available';
      }
    }
  } catch (e) { row.acquisitionError = 'Acquisition request failed safely.'; }
  render();
}

async function executeRegionTranscription(index, decision) {
  const row = rows[index];
  const confirmed = window.confirm(
    'Execute authorized external transcription for evidence ID ' + row.evidenceArtifactId + '?\n\n' +
    'Evidence-specific authorization already exists.\nProvider: OpenAI\nPurpose: literal transcription\n' +
    'Selected authoritative PDF evidence crops will be transmitted.\n\n' +
    'This action initiates the authorized external transcription. It will initiate external processing; the authorization will be reserved/consumed according to governed execution semantics and may create provider-attempt, provider-state, and transcription derivative records.'
  );
  if (!confirmed) return;
  try {
    const resp = await fetch(`/owner/evidence/${'$'}{row.evidenceArtifactId}/execute-region-transcription`, {
      method: 'POST', headers: authHeaders(),
    });
    const result = await resp.json();
    row.acquisitionError = resp.ok ? null : (result.detail || result.error || 'Execution failed safely.');
    await loadAcquisitionDecision(index, true);
  } catch (e) { row.acquisitionError = 'Execution request failed safely.'; render(); }
}

async function processRow(index) {
  const row = rows[index];
  row.status = 'PROCESSING';
  render();
  const resp = await fetch(`/owner/evidence/${'$'}{row.evidenceArtifactId}/process`, { method: 'POST', headers: authHeaders() });
  const result = await resp.json();
  row.status = result.status;
  row.tierAFormat = result.format;
  row.message = result.message;
  row.derivativeGenerationId = result.derivativeGenerationId || null;
  render();
}

// Document Ingestion -- Derivative Content Persistence and Retrieval. "View Extracted Content"
// always fetches from the durable retrieval endpoint by (evidenceArtifactId, derivativeGenerationId)
// -- never from the transient /process response -- so what the owner sees is proven to come from
// persisted storage, not extraction held only in this page's own memory.
async function viewContent(index) {
  const row = rows[index];
  if (expandedIndex === index) {
    expandedIndex = null;
    render();
    return;
  }
  if (!row.content && !row.contentError) {
    const resp = await fetch(
      `/owner/evidence/${'$'}{row.evidenceArtifactId}/content/${'$'}{row.derivativeGenerationId}`,
      { method: 'GET', headers: authHeaders() },
    );
    const result = await resp.json();
    if (result.status === 'RETRIEVED') {
      row.content = result.content;
      row.pageCount = result.content.pageCount;
      row.humanFidelityStatus = result.content.humanFidelityStatus;
      row.correctedRepresentation = result.content.humanCorrectedRepresentation;
    } else {
      row.contentError = result.status + (result.message ? (': ' + result.message) : '');
    }
  }
  expandedIndex = index;
  render();
}

async function ocrRow(index) {
  const row = rows[index];
  row.status = 'OCR_PROCESSING';
  render();
  const resp = await fetch(`/owner/evidence/${'$'}{row.evidenceArtifactId}/ocr`, { method: 'POST', headers: authHeaders() });
  const result = await resp.json();
  row.status = result.status;
  row.message = result.message;
  render();
}

// Document Ingestion -- Tier B Durable OCR Derivative Content. Explicit, owner-triggered durable
// OCR -- distinct from ocrRow (the existing, unchanged transient-only path) above. On success,
// carries the new DerivativeGenerationId; "View Extracted Content" thereafter always fetches from
// the durable retrieval endpoint (viewOcrContent, below), never from this response held only in
// this page's own memory -- mirroring viewContent's own identical discipline for Tier A.
async function ocrDurableRow(index) {
  const row = rows[index];
  row.status = 'OCR_DURABLE_PROCESSING';
  render();
  const resp = await fetch(`/owner/evidence/${'$'}{row.evidenceArtifactId}/ocr-durable`, { method: 'POST', headers: authHeaders() });
  const result = await resp.json();
  row.status = result.status;
  row.message = result.message;
  row.ocrDerivativeGenerationId = result.derivativeGenerationId || null;
  render();
}

async function loadEnhancedReadiness() {
  try {
    const resp = await fetch('/owner/evidence/transcription-readiness', { method: 'GET', headers: authHeaders() });
    if (resp.status === 401) return;
    enhancedReadiness = await resp.json();
    render();
  } catch (e) {
    enhancedReadiness = { status: 'DISABLED', message: 'Enhanced transcription readiness could not be checked.' };
    render();
  }
}
document.getElementById('checkEnhancedReadinessButton').onclick = loadEnhancedReadiness;

async function transcribeExternalRow(index) {
  const source = rows[index];
  if (source.externalProcessing || enhancedReadiness.status !== 'READY') return;
  source.externalProcessing = true;
  render();
  try {
    const resp = await fetch(`/owner/evidence/${'$'}{source.evidenceArtifactId}/transcribe-external`, { method: 'POST', headers: authHeaders() });
    const result = await resp.json();
    if (result.status === 'TIER_B_DURABLE_COMPLETE') {
      rows.push({
        originalFileName: source.originalFileName + ' — Enhanced transcription', byteLength: source.byteLength,
        evidenceArtifactId: source.evidenceArtifactId, status: result.status, message: 'Enhanced transcription durably admitted',
        ocrDerivativeGenerationId: result.derivativeGenerationId, ocrContent: result.content,
        externalResultRow: true, selectedForAnalysis: false, acknowledgesUnverifiedExternalTranscription: false,
      });
    } else {
      source.message = result.message || 'Enhanced transcription failed safely.';
    }
  } catch (e) {
    source.message = 'Enhanced transcription request failed safely.';
  } finally {
    source.externalProcessing = false;
    render();
  }
}

async function viewOcrContent(index) {
  const row = rows[index];
  if (expandedIndex === index) {
    expandedIndex = null;
    render();
    return;
  }
  if (!row.ocrContent && !row.ocrContentError) {
    const resp = await fetch(
      `/owner/evidence/${'$'}{row.evidenceArtifactId}/ocr-content/${'$'}{row.ocrDerivativeGenerationId}`,
      { method: 'GET', headers: authHeaders() },
    );
    const result = await resp.json();
    if (result.status === 'RETRIEVED') {
      row.ocrContent = result.content;
    } else {
      row.ocrContentError = result.status + (result.message ? (': ' + result.message) : '');
    }
  }
  expandedIndex = index;
  render();
}

// Minimum Production Document Pipeline -- Local Reasoning Implementation. Submits the owner's own
// selected, already-admitted derivative generations plus their instruction to the one owner-authenticated
// /owner/analyse route. Never persisted anywhere by this page; a fresh analysis is requested every time.
document.getElementById('analyseButton').onclick = analyseSelected;

function collectAnalysisSelections() {
  const selections = [];
  rows.forEach(row => {
    if (!row.selectedForAnalysis) return;
    if (row.status === 'TIER_A_COMPLETE' && row.derivativeGenerationId) {
      selections.push({ evidenceArtifactId: row.evidenceArtifactId, derivativeGenerationId: row.derivativeGenerationId });
    } else if (row.status === 'TIER_B_DURABLE_COMPLETE' && row.ocrDerivativeGenerationId) {
      selections.push({
        evidenceArtifactId: row.evidenceArtifactId,
        derivativeGenerationId: row.ocrDerivativeGenerationId,
        acknowledgesUnverifiedExternalTranscription: !!row.acknowledgesUnverifiedExternalTranscription,
      });
    }
  });
  return selections;
}

function showAnalysisNote(text) {
  const resultsDiv = document.getElementById('analysisResults');
  resultsDiv.innerHTML = '';
  const p = document.createElement('p');
  p.className = 'note';
  p.textContent = text;
  resultsDiv.appendChild(p);
}

async function analyseSelected() {
  const selections = collectAnalysisSelections();
  const instruction = document.getElementById('analysisInstruction').value;
  if (!selections.length) {
    showAnalysisNote('Select at least one processed document above before analysing.');
    return;
  }
  if (!instruction.trim()) {
    showAnalysisNote('Enter an analysis instruction before analysing.');
    return;
  }
  showAnalysisNote('Analysing...');
  try {
    const resp = await fetch('/owner/analyse', {
      method: 'POST',
      headers: Object.assign({ 'Content-Type': 'application/json' }, authHeaders()),
      body: JSON.stringify({ selections, instruction }),
    });
    if (resp.status === 401) {
      showAnalysisNote('Owner session expired or unavailable.');
      return;
    }
    const result = await resp.json();
    const resultsDiv = document.getElementById('analysisResults');
    resultsDiv.innerHTML = '';
    renderAnalysisResult(resultsDiv, result);
  } catch (e) {
    showAnalysisNote('Analysis request failed: ' + e);
  }
}

// Reviewed Analysis Result -- Explicit Owner Save. Holds only the opaque pendingAnalysisId the
// server returned alongside the most recently completed analysis -- never the analysis text
// itself, which is never resubmitted to Save it.
let currentPendingAnalysisId = null;

// Every field below is inserted via appendField/appendExtractedText/textContent (never innerHTML),
// so the local model's own response -- provider-generated, human-review-only material -- can never
// be interpreted as HTML or script, no matter what characters it contains.
function renderAnalysisResult(container, result) {
  currentPendingAnalysisId = null;
  if (result.status !== 'COMPLETED') {
    const p = document.createElement('p');
    p.className = 'note';
    p.textContent = 'Analysis failed: ' + (result.message || result.status);
    container.appendChild(p);
    return;
  }
  currentPendingAnalysisId = result.pendingAnalysisId || null;
  const panel = document.createElement('div');
  panel.className = 'content-panel';
  const disclaimer = document.createElement('p');
  disclaimer.className = 'note';
  disclaimer.textContent = 'Provider-generated material for human review -- not Evidence, Memory, Knowledge, or canonical Parker truth.';
  panel.appendChild(disclaimer);
  appendExtractedText(panel, 'Analysis:', result.result.analysisText);
  if (result.result.mechanismIdentity) {
    appendField(panel, 'Model', result.result.mechanismIdentity + ' ' + (result.result.mechanismVersion || ''));
  }
  appendWarnings(panel, result.result.warnings);
  const label = document.createElement('div');
  label.textContent = 'Evidence references supplied:';
  panel.appendChild(label);
  const ul = document.createElement('ul');
  result.result.evidenceReferences.forEach(ref => {
    const li = document.createElement('li');
    li.textContent = analysisEvidenceReferenceText(ref);
    ul.appendChild(li);
  });
  panel.appendChild(ul);
  if (currentPendingAnalysisId) {
    const saveButton = document.createElement('button');
    saveButton.textContent = 'Save Analysis';
    saveButton.onclick = saveCurrentAnalysis;
    panel.appendChild(saveButton);
    const saveStatus = document.createElement('p');
    saveStatus.id = 'saveAnalysisStatus';
    saveStatus.className = 'note';
    panel.appendChild(saveStatus);
  }
  container.appendChild(panel);
}

function analysisEvidenceReferenceText(ref) {
  let text = ref.evidenceArtifactId + ' / ' + ref.derivativeGenerationId + ' (' + ref.derivativeKind + ')';
  const a = ref.assurance;
  if (!a) return text + ' — historical assurance unavailable';
  text += ' — ' + a.mechanism + ', completeness=' + a.completeness;
  if (a.fidelity) text += ', fidelity=' + a.fidelity;
  if (a.providerIdentity) text += ', provider=' + a.providerIdentity;
  text += ', review=' + (a.humanReviewStates || ['UNREVIEWED']).join('/');
  if (a.containsUncertaintyOrIllegibility) text += ', uncertainty/illegibility present';
  return text;
}

// Reviewed Analysis Result -- Explicit Owner Save. Sends only the opaque pendingAnalysisId, never
// analysis text -- the server resolves and persists the exact result it already produced and
// holds; nothing this page submits here can ever become the saved content.
async function saveCurrentAnalysis() {
  const statusEl = document.getElementById('saveAnalysisStatus');
  if (!currentPendingAnalysisId) {
    if (statusEl) statusEl.textContent = 'No analysis available to save.';
    return;
  }
  if (statusEl) statusEl.textContent = 'Saving...';
  try {
    const resp = await fetch('/owner/saved-analyses', {
      method: 'POST',
      headers: Object.assign({ 'Content-Type': 'application/json' }, authHeaders()),
      body: JSON.stringify({ pendingAnalysisId: currentPendingAnalysisId }),
    });
    if (resp.status === 401) {
      if (statusEl) statusEl.textContent = 'Owner session expired or unavailable.';
      return;
    }
    const result = await resp.json();
    if (result.status === 'SAVED') {
      if (statusEl) statusEl.textContent = 'Saved. Saved analysis ID: ' + result.savedAnalysisId;
      currentPendingAnalysisId = null;
      refreshSavedAnalyses();
    } else if (statusEl) {
      statusEl.textContent = 'Save failed: ' + (result.message || result.status);
    }
  } catch (e) {
    if (statusEl) statusEl.textContent = 'Save request failed: ' + e;
  }
}

// Reviewed Analysis Result -- Explicit Owner Save. Bounded, metadata-only listing -- never the
// full analysis text of any listed entry.
async function refreshSavedAnalyses() {
  const tbody = document.getElementById('savedAnalysisRows');
  try {
    const resp = await fetch('/owner/saved-analyses', { method: 'GET', headers: authHeaders() });
    if (resp.status === 401) {
      tbody.innerHTML = '';
      return;
    }
    const result = await resp.json();
    tbody.innerHTML = '';
    (result.savedAnalyses || []).forEach(entry => {
      const tr = document.createElement('tr');
      const savedTd = document.createElement('td');
      savedTd.textContent = entry.savedAt;
      const instructionTd = document.createElement('td');
      instructionTd.textContent = entry.instructionPreview;
      const actionsTd = document.createElement('td');
      const viewButton = document.createElement('button');
      viewButton.textContent = 'View';
      viewButton.onclick = () => viewSavedAnalysis(entry.savedAnalysisId);
      actionsTd.appendChild(viewButton);
      tr.appendChild(savedTd);
      tr.appendChild(instructionTd);
      tr.appendChild(actionsTd);
      tbody.appendChild(tr);
    });
  } catch (e) {
    // best-effort refresh only -- the owner can retry via the Refresh button.
  }
}

// Reviewed Analysis Result -- Explicit Owner Save. Retrieval only -- never re-runs analysis, never
// invokes the model.
async function viewSavedAnalysis(savedAnalysisId) {
  const detail = document.getElementById('savedAnalysisDetail');
  detail.innerHTML = '';
  try {
    const resp = await fetch('/owner/saved-analyses/' + encodeURIComponent(savedAnalysisId), { method: 'GET', headers: authHeaders() });
    if (resp.status === 401) {
      const p = document.createElement('p');
      p.className = 'note';
      p.textContent = 'Owner session expired or unavailable.';
      detail.appendChild(p);
      return;
    }
    const result = await resp.json();
    if (result.status !== 'RETRIEVED') {
      const p = document.createElement('p');
      p.className = 'note';
      p.textContent = 'Could not retrieve saved analysis: ' + (result.message || result.status);
      detail.appendChild(p);
      return;
    }
    const panel = document.createElement('div');
    panel.className = 'content-panel';
    const disclaimer = document.createElement('p');
    disclaimer.className = 'note';
    disclaimer.textContent = 'Provider-generated material for human review -- not Evidence, Memory, Knowledge, or canonical Parker truth.';
    panel.appendChild(disclaimer);
    appendField(panel, 'Saved at', result.result.savedAt);
    appendField(panel, 'Analysed at', result.result.analysedAt);
    appendField(panel, 'Instruction', result.result.instruction);
    if (result.result.mechanismIdentity) {
      appendField(panel, 'Model', result.result.mechanismIdentity + ' ' + (result.result.mechanismVersion || ''));
    }
    appendExtractedText(panel, 'Analysis:', result.result.analysisText);
    const label = document.createElement('div');
    label.textContent = 'Evidence references supplied:';
    panel.appendChild(label);
    const ul = document.createElement('ul');
    result.result.evidenceReferences.forEach(ref => {
      const li = document.createElement('li');
      li.textContent = analysisEvidenceReferenceText(ref);
      ul.appendChild(li);
    });
    panel.appendChild(ul);
    detail.appendChild(panel);
  } catch (e) {
    const p = document.createElement('p');
    p.className = 'note';
    p.textContent = 'Retrieval request failed: ' + e;
    detail.appendChild(p);
  }
}

document.getElementById('refreshSavedAnalysesButton').onclick = refreshSavedAnalyses;
loadExistingEvidence();
refreshSavedAnalyses();
</script>
</body>
</html>
""".trimIndent()
