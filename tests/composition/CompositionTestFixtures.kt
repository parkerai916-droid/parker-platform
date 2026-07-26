package parker.composition

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Sprint 10, Unit 4 test fixtures. No tests of their own -- mirrors this
 * repository's own established `Fake*`/`Recording*` fixture-file
 * convention (e.g. `tests/runtime/FakeExecutionPipeline.kt`,
 * `VerticalSliceEndToEndTest.kt`'s own `RecordingExecutionPipeline`).
 */

/** Records every log call, in order, so a test can assert on exactly what this Unit's production code actually logged, without depending on stdout formatting. */
class RecordingParkerLogger : ParkerLogger {
    data class Entry(val level: LogLevel, val message: String, val throwable: Throwable?)

    private val backing = CopyOnWriteArrayList<Entry>()
    val entries: List<Entry> get() = backing.toList()

    override fun log(level: LogLevel, message: String, throwable: Throwable?) {
        backing.add(Entry(level, message, throwable))
    }

    fun messages(level: LogLevel? = null): List<String> =
        entries.filter { level == null || it.level == level }.map { it.message }

    fun hasMessageContaining(substring: String, level: LogLevel? = null): Boolean =
        messages(level).any { substring in it }
}

/** Records every notification text delivered, and can optionally be configured to throw -- the composition root's own real "tool failure" seam, exercised without touching any frozen production file. */
class RecordingOwnerNotificationSink(
    private val onNotify: (suspend (text: String) -> Unit)? = null,
) : OwnerNotificationSink {
    private val backing = CopyOnWriteArrayList<String>()
    val notifications: List<String> get() = backing.toList()

    override suspend fun notify(text: String) {
        backing.add(text)
        onNotify?.invoke(text)
    }
}

/**
 * A minimal local HTTP server standing in for a real model endpoint,
 * speaking exactly the Ollama-shaped JSON
 * `LocalHttpModelInferenceClient`'s own default request/response
 * formatters already expect (`defaultOllamaRequestBody`/
 * `defaultOllamaResponseBody`, `src/runtime/ModelInferenceClient.kt`) --
 * this fixture invents no new wire format. Backed by
 * `com.sun.net.httpserver.HttpServer`, part of the JDK standard library --
 * no new Gradle dependency is introduced for this Unit's own tests.
 *
 * Using this fixture exercises `LocalHttpModelInferenceClient`'s real,
 * live HTTP path end-to-end for the first time in this repository's own
 * test suite -- a disclosed, welcome byproduct of this Unit's own
 * composition-root tests, narrowing (not claimed to close in full)
 * `IMPLEMENTATION_GAPS.md` #53's "`LocalHttpModelInferenceClient`'s own
 * live HTTP path is not exercised by the automated test suite" item. See
 * this Unit's own Implementation Review for the precise, honest scope of
 * that narrowing.
 */
class StubModelServer private constructor(private val server: HttpServer) : AutoCloseable {

    val port: Int get() = server.address.port
    val endpointUrl: String get() = "http://127.0.0.1:$port/api/generate"

    /**
     * Every request body this server has received, in arrival order, as
     * raw UTF-8 text -- the literal Ollama-shaped JSON
     * `LocalHttpModelInferenceClient` sends, prompt field included
     * (Sprint 11, Unit 3 addition, additive only: every prior caller of
     * [start] that never reads [receivedRequestBodies] is unaffected).
     * Exists so a test can confirm a real, real-production-assembled
     * `ReasoningContext`'s own entries actually reached the real prompt
     * sent over this real HTTP call, rather than merely trusting that
     * they did.
     */
    private val backingRequestBodies = CopyOnWriteArrayList<String>()
    val receivedRequestBodies: List<String> get() = backingRequestBodies.toList()

    override fun close() = server.stop(0)

    companion object {
        /**
         * Starts a server that responds to every request with [responseFieldValue]
         * as the Ollama `"response"` field's value, after waiting [delayMillis]
         * (default `0`, no delay -- used by timeout tests to force a real,
         * observable `TimeoutCancellationException`).
         */
        fun start(responseFieldValue: String, delayMillis: Long = 0L): StubModelServer {
            val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
            lateinit var stub: StubModelServer
            server.createContext("/api/generate") { exchange ->
                try {
                    val requestText = exchange.requestBody.readBytes().toString(Charsets.UTF_8)
                    stub.backingRequestBodies.add(requestText)
                    if (delayMillis > 0) Thread.sleep(delayMillis)
                    val escaped = responseFieldValue.replace("\\", "\\\\").replace("\"", "\\\"")
                    val body = "{\"response\":\"$escaped\"}".toByteArray(Charsets.UTF_8)
                    exchange.responseHeaders.add("Content-Type", "application/json")
                    exchange.sendResponseHeaders(200, body.size.toLong())
                    exchange.responseBody.write(body)
                } finally {
                    exchange.close()
                }
            }
            server.start()
            stub = StubModelServer(server)
            return stub
        }
    }
}
