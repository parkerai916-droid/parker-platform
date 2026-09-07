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
import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.PrincipalId
import parker.core.runtime.AgentGatewayEvidenceManifestResult
import parker.core.runtime.AgentGatewayEvidenceRetrievalResult

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
 * [retrieveEvidenceManifestAsAgent] are the *only* two capabilities it can
 * ever invoke -- both supplied as individually bound functions at
 * construction (mirroring [OwnerEvidenceHttpServer]'s own established
 * "individually bound lambda parameters, never the raw runtime object"
 * construction pattern), each already performing its own complete,
 * Hermes-principal-bound [parker.core.interfaces.PermissionEngine]
 * evaluation internally (AG-1D). This class never constructs an
 * `ExecutionRequest`, never references a `PrincipalId` other than what
 * [authentication] resolves, and never accepts a caller-supplied
 * `PrincipalId`, `AuthorizationPurposeId`, action, or resource of any kind.
 *
 * ## Routes (GET only, R0 read-only)
 *
 * - `GET /agent/evidence/{evidenceArtifactId}` → [retrieveEvidenceAsAgent]
 * - `GET /agent/evidence/{evidenceArtifactId}/manifest` → [retrieveEvidenceManifestAsAgent]
 *
 * No other method or path is recognised. No write, submission, or
 * acquisition route exists anywhere in this class.
 */
class AgentGatewayHttpServer(
    private val bindAddress: String,
    private val port: Int,
    private val authentication: AgentGatewayAuthentication,
    private val retrieveEvidenceAsAgent: suspend (EvidenceArtifactId) -> AgentGatewayEvidenceRetrievalResult,
    private val retrieveEvidenceManifestAsAgent: suspend (EvidenceArtifactId) -> AgentGatewayEvidenceManifestResult,
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
                runCatching { exchange.requestBody.use { it.readBytes() } }

                val token = bearerToken(exchange)
                if (token == null) {
                    recordAudit(correlationId, null, null, null, AgentGatewayAccessOutcome.UNAUTHENTICATED)
                    writeJson(exchange, 401, jsonObject("error" to "unauthorised"))
                    return
                }
                val principalId = authentication.authenticate(token)
                if (principalId == null) {
                    recordAudit(correlationId, null, null, null, AgentGatewayAccessOutcome.AUTHENTICATION_FAILED)
                    writeJson(exchange, 401, jsonObject("error" to "unauthorised"))
                    return
                }

                if (exchange.requestMethod != "GET") {
                    recordAudit(correlationId, principalId, null, null, AgentGatewayAccessOutcome.NOT_FOUND_ROUTE)
                    writeJson(exchange, 404, jsonObject("error" to "not found"))
                    return
                }

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
        const val RETRIEVE_ACTION_NAME = "agent-gateway.evidence.retrieve"
        const val RETRIEVE_MANIFEST_ACTION_NAME = "agent-gateway.evidence.retrieve-manifest"
    }
}
