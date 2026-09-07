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
import parker.core.runtime.AgentGatewayEvidenceManifestResult
import parker.core.runtime.AgentGatewayEvidenceRetrievalResult
import parker.core.runtime.AgentGatewaySourceSubmissionResult

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
 * [retrieveEvidenceManifestAsAgent]/[submitSourceAsAgent] are the *only*
 * three capabilities it can ever invoke -- all supplied as individually
 * bound functions at construction (mirroring [OwnerEvidenceHttpServer]'s
 * own established "individually bound lambda parameters, never the raw
 * runtime object" construction pattern), each already performing its own
 * complete, Hermes-principal-bound [parker.core.interfaces.PermissionEngine]
 * evaluation internally (AG-1D/AG-1F). This class never constructs an
 * `ExecutionRequest`, never references a `PrincipalId` other than what
 * [authentication] resolves, never computes an authoritative source hash
 * itself, never invents an `EvidenceArtifactId`, and never accepts a
 * caller-supplied `PrincipalId`, `AuthorizationPurposeId`, action, or
 * resource of any kind.
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
 *
 * No other method or path is recognised. No acquisition, transcription, HFR, case, or deletion
 * route exists anywhere in this class.
 */
class AgentGatewayHttpServer(
    private val bindAddress: String,
    private val port: Int,
    private val authentication: AgentGatewayAuthentication,
    private val retrieveEvidenceAsAgent: suspend (EvidenceArtifactId) -> AgentGatewayEvidenceRetrievalResult,
    private val retrieveEvidenceManifestAsAgent: suspend (EvidenceArtifactId) -> AgentGatewayEvidenceManifestResult,
    private val submitSourceAsAgent: suspend (CandidateEvidenceArtifact, String?) -> AgentGatewaySourceSubmissionResult,
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
        httpServer.start()
        server = httpServer
        executor = fixedThreadPool
        logger.info("Agent Gateway HTTP server started on $bindAddress:${boundPort}")
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

                if (exchange.requestMethod == "POST" && exchange.requestURI.path == "/agent/evidence") {
                    handleSubmit(exchange, correlationId, principalId)
                    return
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
                    1 -> handleRetrieve(exchange, correlationId, principalId, segments[0])
                    2 -> if (segments[1] == "manifest") {
                        handleRetrieveManifest(exchange, correlationId, principalId, segments[0])
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

        private fun handleRetrieve(exchange: HttpExchange, correlationId: String, principalId: PrincipalId, rawId: String) {
            val id = parseEvidenceId(exchange, correlationId, principalId, rawId) ?: return
            when (val result = runBlocking { retrieveEvidenceAsAgent(id) }) {
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

        private fun handleRetrieveManifest(exchange: HttpExchange, correlationId: String, principalId: PrincipalId, rawId: String) {
            val id = parseEvidenceId(exchange, correlationId, principalId, rawId) ?: return
            when (val result = runBlocking { retrieveEvidenceManifestAsAgent(id) }) {
                is AgentGatewayEvidenceManifestResult.Found -> {
                    recordAudit(correlationId, principalId, RETRIEVE_MANIFEST_ACTION_NAME, id.value, AgentGatewayAccessOutcome.APPROVED)
                    writeJson(exchange, 200, jsonObject(
                        "status" to "FOUND",
                        "evidenceArtifactId" to result.manifest.evidenceArtifactId.value,
                        "sha256" to result.manifest.sha256,
                        "byteLength" to result.manifest.byteLength,
                        "receivedMediaType" to result.manifest.receivedMediaType,
                        "originalFileName" to result.manifest.originalFileName,
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

            when (val result = runBlocking { submitSourceAsAgent(candidate, advisorySha256Raw) }) {
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
        const val ORIGINAL_FILENAME_HEADER = "X-Parker-Original-Filename"
        const val ADVISORY_SHA256_HEADER = "X-Parker-Advisory-Sha256"

        /**
         * Mirrors `OwnerEvidenceHttpServer.MAX_PART_BYTES` -- the same 64 MiB ingress bound
         * Section 8's own text cites ("the 64 MiB `MAX_SOURCE_BYTES` bound already present for
         * owner-local ingress"). No larger allowance is invented for the Agent Gateway.
         */
        const val MAX_SUBMISSION_BYTES: Long = 64L * 1024L * 1024L
    }
}
