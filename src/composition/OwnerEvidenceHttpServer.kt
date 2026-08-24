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
import parker.core.interfaces.EvidenceArtifactId
import parker.ui.EvidenceImportOutcome
import parker.ui.OwnerEvidenceOperations
import parker.ui.TierAProcessingOutcome
import parker.ui.TierBProcessingOutcome

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
    private val token: String,
    private val operations: OwnerEvidenceOperations,
    private val logger: ParkerLogger,
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
        httpServer.createContext("/owner/evidence", EvidenceHandler())
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
        val header = exchange.requestHeaders.getFirst("Authorization") ?: return false
        val prefix = "Bearer "
        if (!header.startsWith(prefix)) return false
        return constantTimeEquals(header.substring(prefix.length), token)
    }

    private fun constantTimeEquals(a: String, b: String): Boolean =
        MessageDigest.isEqual(a.toByteArray(StandardCharsets.UTF_8), b.toByteArray(StandardCharsets.UTF_8))

    private fun rejectUnauthorised(exchange: HttpExchange) {
        // Drain and discard the body without ever parsing it, per Phase 3's "reject missing/invalid
        // credentials before accepting file bodies" -- no part is ever streamed to a temp file for
        // an unauthorised request.
        runCatching { exchange.requestBody.use { it.readBytes() } }
        writeJson(exchange, 401, jsonObject("error" to "unauthorised"))
    }

    // ---- static owner page ---------------------------------------------------------------

    private inner class RootPageHandler : HttpHandler {
        override fun handle(exchange: HttpExchange) {
            try {
                if (exchange.requestURI.path != "/" || exchange.requestMethod != "GET") {
                    writeJson(exchange, 404, jsonObject("error" to "not found"))
                    return
                }
                val bytes = OWNER_EVIDENCE_PAGE_HTML.toByteArray(StandardCharsets.UTF_8)
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
                    segments.isEmpty() && method == "POST" -> handleUpload(exchange)
                    segments.size == 2 && segments[1] == "process" && method == "POST" ->
                        handleProcess(exchange, segments[0])
                    segments.size == 2 && segments[1] == "ocr" && method == "POST" ->
                        handleOcr(exchange, segments[0])
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
                        val importOutcome = runBlocking { operations.importFile(part.tempPath.toString(), part.declaredContentType) }
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
                is TierAProcessingOutcome.Admitted -> jsonObject("status" to "TIER_A_COMPLETE", "format" to outcome.format)
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
    }

    // ---- minimal JSON writer (no JSON library exists anywhere in this repository; every value
    // written below is either a server-controlled literal or passed through String escaping) ------

    private sealed interface JsonValue
    private class JsonObject(val fields: List<Pair<String, Any?>>) : JsonValue
    private class JsonArray(val items: List<JsonValue>) : JsonValue

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
        /** Mirrors `OwnerLocalFileIngressCoordinator.MAX_SOURCE_BYTES` -- the same 64 MiB ingress bound, enforced here during streaming so an oversized upload never even reaches a temp file in full. */
        const val MAX_PART_BYTES: Long = 64L * 1024L * 1024L

        /** A conservative bound on files per request -- convenience-only multi-select, never a batch evidence authority. */
        const val MAX_FILES_PER_REQUEST: Int = 32
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
  input[type=text] { width: 100%; padding: 0.4rem; box-sizing: border-box; }
  table { width: 100%; border-collapse: collapse; margin-top: 1rem; }
  th, td { text-align: left; padding: 0.4rem; border-bottom: 1px solid #333; font-size: 0.85rem; }
  button { padding: 0.3rem 0.7rem; margin-right: 0.3rem; }
  .row-actions button { font-size: 0.8rem; }
  #status { color: #f88; }
</style>
</head>
<body>
<h1>Parker Owner Evidence Upload</h1>
<p>Owner token: <input type="text" id="token" placeholder="paste owner token"></p>
<p>Select Files: <input type="file" id="filePicker" multiple> <button id="uploadButton">Upload</button></p>
<p id="status"></p>
<table>
  <thead><tr><th>File</th><th>Size</th><th>Status</th><th>Evidence ID</th><th>Result</th><th>Actions</th></tr></thead>
  <tbody id="rows"></tbody>
</table>
<script>
let rows = [];

function render() {
  const tbody = document.getElementById('rows');
  tbody.innerHTML = '';
  rows.forEach((row, index) => {
    const tr = document.createElement('tr');
    const actions = document.createElement('td');
    actions.className = 'row-actions';
    if (row.status === 'IMPORTED' || row.status === 'READY_TO_PROCESS') {
      const b = document.createElement('button');
      b.textContent = 'Process';
      b.onclick = () => processRow(index);
      actions.appendChild(b);
    } else if (row.status === 'REQUIRES_OCR') {
      const b = document.createElement('button');
      b.textContent = 'Run OCR';
      b.onclick = () => ocrRow(index);
      actions.appendChild(b);
    }
    tr.innerHTML = `<td>${'$'}{escapeHtml(row.originalFileName)}</td><td>${'$'}{row.byteLength}</td><td>${'$'}{row.status}</td><td>${'$'}{row.evidenceArtifactId || ''}</td><td>${'$'}{row.message || ''}</td>`;
    tr.appendChild(actions);
    tbody.appendChild(tr);
  });
}

function escapeHtml(s) {
  return s.replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
}

function authHeaders() {
  return { 'Authorization': 'Bearer ' + document.getElementById('token').value };
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
    if (resp.status === 401) { document.getElementById('status').textContent = 'Unauthorised: check owner token.'; return; }
    const results = await resp.json();
    const startIndex = rows.length - selected.length;
    results.forEach((result, i) => {
      const target = rows[startIndex + i];
      target.status = result.status;
      target.evidenceArtifactId = result.evidenceArtifactId;
      target.message = result.message;
    });
    render();
  } catch (e) {
    document.getElementById('status').textContent = 'Upload failed: ' + e;
  }
};

async function processRow(index) {
  const row = rows[index];
  row.status = 'PROCESSING';
  render();
  const resp = await fetch(`/owner/evidence/${'$'}{row.evidenceArtifactId}/process`, { method: 'POST', headers: authHeaders() });
  const result = await resp.json();
  row.status = result.status;
  row.tierAFormat = result.format;
  row.message = result.message;
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
</script>
</body>
</html>
""".trimIndent()
