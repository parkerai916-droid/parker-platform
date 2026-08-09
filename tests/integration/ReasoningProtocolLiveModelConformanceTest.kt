package parker.integration

import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import parker.core.interfaces.ReasoningProviderResponse
import parker.core.runtime.DefaultReasoningPromptBuilder
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.io.path.readLines
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ReasoningProtocolLiveModelConformanceTest {
    @TempDir
    lateinit var temporaryDirectory: Path

    private fun completeEnvironment(endpoint: String, output: Path = temporaryDirectory.resolve("result.jsonl")) = mapOf(
        LiveEvaluationConfigLoader.ENDPOINT to endpoint,
        LiveEvaluationConfigLoader.MODEL to "synthetic-test-model",
        LiveEvaluationConfigLoader.TIMEOUT to "2000",
        LiveEvaluationConfigLoader.OUTPUT to output.toString(),
        LiveEvaluationConfigLoader.COMMIT to "05d4c2a-test",
        LiveEvaluationConfigLoader.DIGEST to "sha256:synthetic-model-digest",
        LiveEvaluationConfigLoader.IMAGE to "synthetic-runtime-image",
        LiveEvaluationConfigLoader.REPETITIONS to "2",
    )

    private fun config(endpoint: String, output: Path = temporaryDirectory.resolve("result.jsonl")): LiveEvaluationConfig =
        assertIs<EvaluationConfigLoad.Present>(
            LiveEvaluationConfigLoader.load(completeEnvironment(endpoint, output), Path.of(".")),
        ).config

    private fun rememberInput(profile: ContextProfileId = ContextProfileId.MINIMAL_PRODUCTION_CONTEXT) =
        SyntheticContextProfiles.construct(SyntheticConformanceCorpus.fixtures.first(), profile)

    @Test
    fun `all live configuration absent returns Absent before an HTTP client can be constructed`() {
        assertIs<EvaluationConfigLoad.Absent>(LiveEvaluationConfigLoader.load(emptyMap(), Path.of(".")))
        assertIs<EvaluationConfigLoad.Absent>(
            LiveEvaluationConfigLoader.load(
                mapOf(LiveEvaluationConfigLoader.DIGEST to "optional-identity-without-a-live-run"),
                Path.of("."),
            ),
        )
    }

    @Test
    fun `partial and invalid configuration fails with values redacted`() {
        val secret = "http://owner:very-secret@127.0.0.1:11434/api/generate"
        val partial = assertFailsWith<EvaluationConfigurationException> {
            LiveEvaluationConfigLoader.load(mapOf(LiveEvaluationConfigLoader.ENDPOINT to secret), Path.of("."))
        }
        assertFalse(secret in partial.message.orEmpty())
        assertFalse("very-secret" in partial.message.orEmpty())

        val invalid = assertFailsWith<EvaluationConfigurationException> {
            LiveEvaluationConfigLoader.load(completeEnvironment(secret), Path.of("."))
        }
        assertFalse(secret in invalid.message.orEmpty())
        assertFalse("very-secret" in invalid.message.orEmpty())
    }

    @Test
    fun `configured output beneath source test or docs is rejected`() {
        listOf("src/eval.jsonl", "tests/eval.jsonl", "docs/eval.jsonl").forEach { output ->
            assertFailsWith<EvaluationConfigurationException> {
                LiveEvaluationConfigLoader.load(
                    completeEnvironment("http://127.0.0.1:11434/api/generate", Path.of(output)),
                    Path.of("."),
                )
            }
        }
    }

    @Test
    fun `synthetic fixtures and all nine context profiles are stable and immutable`() {
        assertEquals(4, SyntheticConformanceCorpus.fixtures.map { it.expectedAction }.distinct().size)
        assertTrue(SyntheticConformanceCorpus.fixtures.all { it.synthetic })
        assertEquals(9, ContextProfileId.entries.size)
        ContextProfileId.entries.forEach { profile ->
            val first = rememberInput(profile)
            val second = rememberInput(profile)
            assertEquals(first, second)
            assertEquals(first.stableInputHash, second.stableInputHash)
        }
    }

    @Test
    fun `real production builder client and parser are used and transparent capture preserves traffic`() = runBlocking {
        StubOllamaEndpoint.start("REMEMBER: My synthetic test mug is black.").use { endpoint ->
            val input = rememberInput()
            val observation = ReasoningProtocolLiveModelEvaluationHarness(
                config(endpoint.endpointUrl),
                runId = "run-production-chain",
            ).execute(input, 1)
            val turn = (input.request.subject as parker.core.interfaces.ReasoningSubject.OfTurn).turn
            val exactPrompt = DefaultReasoningPromptBuilder().buildPrompt(turn, input.request.reasoningContext)

            assertEquals(exactPrompt, observation.prompt)
            assertEquals(sha256(exactPrompt), observation.promptSha256)
            assertEquals(endpoint.requestBodies.single(), observation.requestBody)
            assertEquals(endpoint.responseBodies.single(), observation.rawOllamaEnvelope)
            assertEquals("REMEMBER: My synthetic test mug is black.", observation.extractedResponse)
            assertEquals("Remember", observation.parsedVariant)
            assertEquals(ExpectedAction.REMEMBER, observation.actualAction)
            assertEquals(PrimaryClassification.A, observation.primaryClassification)
            assertTrue(observation.representationValid)
            assertEquals(17, observation.endpointMetadata.promptEvalCount)
            assertEquals(5, observation.endpointMetadata.evalCount)
        }
    }

    @Test
    fun `correct valid and wrong valid actions remain distinct semantic classifications`() = runBlocking {
        StubOllamaEndpoint.start("REMEMBER: My synthetic test mug is black.").use { endpoint ->
            val result = ReasoningProtocolLiveModelEvaluationHarness(config(endpoint.endpointUrl), "correct")
                .execute(rememberInput(), 1)
            assertEquals(PrimaryClassification.A, result.primaryClassification)
        }
        StubOllamaEndpoint.start("NOACTION").use { endpoint ->
            val result = ReasoningProtocolLiveModelEvaluationHarness(config(endpoint.endpointUrl), "wrong-noaction")
                .execute(rememberInput(), 1)
            assertEquals(PrimaryClassification.D, result.primaryClassification)
            assertTrue(result.representationValid)
            assertEquals(ExpectedAction.NOACTION, result.actualAction)
        }
        StubOllamaEndpoint.start("REPLY: Understood.").use { endpoint ->
            val result = ReasoningProtocolLiveModelEvaluationHarness(config(endpoint.endpointUrl), "wrong-reply")
                .execute(rememberInput(), 1)
            assertEquals(PrimaryClassification.D, result.primaryClassification)
            assertTrue(result.representationValid)
            assertEquals(ExpectedAction.REPLY, result.actualAction)
        }
    }

    @Test
    fun `content deviation is B and is not silently treated as exact`() = runBlocking {
        StubOllamaEndpoint.start("REMEMBER: Black synthetic mug.").use { endpoint ->
            val result = ReasoningProtocolLiveModelEvaluationHarness(config(endpoint.endpointUrl), "paraphrase")
                .execute(rememberInput(), 1)
            assertEquals(PrimaryClassification.B, result.primaryClassification)
            assertEquals(ContentFidelity.DEVIATION_OR_PARAPHRASE, result.contentFidelity)
        }
    }

    @Test
    fun `malformed unknown tag and untagged prose retain parser failure classifications`() = runBlocking {
        StubOllamaEndpoint.start("MEMBER: black").use { endpoint ->
            val result = ReasoningProtocolLiveModelEvaluationHarness(config(endpoint.endpointUrl), "malformed")
                .execute(rememberInput(), 1)
            assertEquals(PrimaryClassification.C, result.primaryClassification)
            assertFalse(result.representationValid)
            assertContains(result.parserExceptionType.orEmpty(), "UnclassifiableModelResponseException")
            assertEquals("UNCLASSIFIABLE_MODEL_RESPONSE", result.parserExceptionClassification)
            assertNull(result.actualAction)
        }
        StubOllamaEndpoint.start("I will remember the synthetic mug.").use { endpoint ->
            val result = ReasoningProtocolLiveModelEvaluationHarness(config(endpoint.endpointUrl), "prose")
                .execute(rememberInput(), 1)
            assertEquals(PrimaryClassification.E, result.primaryClassification)
            assertFalse(result.representationValid)
        }
    }

    @Test
    fun `multiple tagged outputs and blank partial outputs are recorded without repair`() = runBlocking {
        StubOllamaEndpoint.start("REMEMBER: synthetic fact\nREPLY: done").use { endpoint ->
            val result = ReasoningProtocolLiveModelEvaluationHarness(config(endpoint.endpointUrl), "multiple")
                .execute(rememberInput(), 1)
            assertEquals(PrimaryClassification.F, result.primaryClassification)
            assertFalse(result.representationValid)
        }
        StubOllamaEndpoint.start("").use { endpoint ->
            val result = ReasoningProtocolLiveModelEvaluationHarness(config(endpoint.endpointUrl), "blank")
                .execute(rememberInput(), 1)
            assertEquals(PrimaryClassification.G, result.primaryClassification)
            assertFalse(result.representationValid)
        }
        StubOllamaEndpoint.start("REMEM").use { endpoint ->
            val result = ReasoningProtocolLiveModelEvaluationHarness(config(endpoint.endpointUrl), "partial")
                .execute(rememberInput(), 1)
            assertEquals(PrimaryClassification.G, result.primaryClassification)
        }
    }

    @Test
    fun `timeout is H and remains separate from semantic and transport failure`() = runBlocking {
        StubOllamaEndpoint.start("REMEMBER: My synthetic test mug is black.", delayMillis = 500).use { endpoint ->
            val shortConfig = config(endpoint.endpointUrl).copy(timeoutMs = 20)
            val result = ReasoningProtocolLiveModelEvaluationHarness(shortConfig, "timeout")
                .execute(rememberInput(), 1)
            assertEquals(PrimaryClassification.H, result.primaryClassification)
            assertNull(result.actualAction)
            assertFalse(result.representationValid)
        }
    }

    @Test
    fun `transport failure is I and remains separate from timeout and semantic failure`() = runBlocking {
        val unusedPort = ServerSocket(0).use { it.localPort }
        val result = ReasoningProtocolLiveModelEvaluationHarness(
            config("http://127.0.0.1:$unusedPort/api/generate").copy(timeoutMs = 2_000),
            "transport",
        ).execute(rememberInput(), 1)
        assertEquals(PrimaryClassification.I, result.primaryClassification)
        assertNull(result.actualAction)
        assertFalse(result.representationValid)
    }

    @Test
    fun `repeated trials preserve input identity and J K are derived without erasing primary outcomes`() = runBlocking {
        StubOllamaEndpoint.startSequence(
            listOf(
                "REMEMBER: My synthetic test mug is black.",
                "REPLY: changed outcome",
                "NOACTION",
            ),
        ).use { endpoint ->
            val harness = ReasoningProtocolLiveModelEvaluationHarness(config(endpoint.endpointUrl), "derived")
            val minimalOne = harness.execute(rememberInput(), 1)
            val minimalTwo = harness.execute(rememberInput(), 2)
            val context = harness.execute(rememberInput(ContextProfileId.SYNTHETIC_MEMORY), 1)
            assertEquals(minimalOne.stableInputHash, minimalTwo.stableInputHash)
            assertNotEquals(minimalOne.stableInputHash, context.stableInputHash)

            val derived = deriveCrossTrialObservations(listOf(minimalOne, minimalTwo, context))
            assertTrue(derived.all { it.contextSensitiveDrift })
            assertTrue(derived.filter { it.contextProfileId == ContextProfileId.MINIMAL_PRODUCTION_CONTEXT.externalId }
                .all { it.repeatabilityFailure })
            assertEquals(
                listOf(PrimaryClassification.A, PrimaryClassification.D, PrimaryClassification.D),
                derived.map { it.primaryClassification },
            )
        }
    }

    @Test
    fun `JSONL serialization is deterministic escaped credential-free and writes run plus trials`() = runBlocking {
        StubOllamaEndpoint.start("REMEMBER: My synthetic test mug is black.").use { endpoint ->
            val output = temporaryDirectory.resolve("nested/evaluation.jsonl")
            val cfg = config(endpoint.endpointUrl, output)
            val harness = ReasoningProtocolLiveModelEvaluationHarness(cfg, "run-json")
            val measured = harness.execute(rememberInput(), 1)
            val deterministic = measured.copy(latencyNanos = 1234)
            val first = EvaluationJsonLines.trial(deterministic)
            val second = EvaluationJsonLines.trial(deterministic)
            assertEquals(first, second)
            assertTrue(first.startsWith("{\"recordType\":\"trial\",\"runId\":\"run-json\""))
            assertContains(first, "\\n")
            assertFalse("very-secret" in first)

            harness.writeArtifact(listOf(deterministic))
            val lines = output.readLines(StandardCharsets.UTF_8)
            assertEquals(2, lines.size)
            assertTrue(lines[0].startsWith("{\"recordType\":\"run\""))
            assertEquals(first, lines[1])
        }
    }

    @Test
    fun `Remember and Goal results terminate as observations with no consequential destination`() = runBlocking {
        listOf(
            "REMEMBER: My synthetic test mug is black." to ExpectedAction.REMEMBER,
            "GOAL: Create the synthetic checklist." to ExpectedAction.GOAL,
        ).forEach { (response, expected) ->
            StubOllamaEndpoint.start(response).use { endpoint ->
                val fixture = if (expected == ExpectedAction.REMEMBER) {
                    SyntheticConformanceCorpus.fixtures.first { it.expectedAction == ExpectedAction.REMEMBER }
                } else {
                    SyntheticConformanceCorpus.fixtures.first { it.expectedAction == ExpectedAction.GOAL }
                }
                val result = ReasoningProtocolLiveModelEvaluationHarness(config(endpoint.endpointUrl), "side-effect-$expected")
                    .execute(SyntheticContextProfiles.construct(fixture, ContextProfileId.MINIMAL_PRODUCTION_CONTEXT), 1)
                assertEquals(expected, result.actualAction)
            }
        }

        val dependencyNames = ReasoningProtocolLiveModelEvaluationHarness::class.java.declaredFields
            .map { it.type.name }
            .joinToString(" ")
        listOf("ParkerRuntime", "ConversationReplyCoordinator", "MemoryAdmissionCoordinator", "MemoryCore", "KnowledgeSubmission")
            .forEach { forbidden -> assertFalse(forbidden in dependencyNames) }
    }

    @Test
    fun `detached Gradle task is not connected to ordinary lifecycle tasks`() {
        val buildText = Files.readString(Path.of("build.gradle.kts"))
        assertContains(buildText, "val liveModelEvaluation by sourceSets.creating")
        assertContains(buildText, "tasks.register<Test>(\"reasoningProtocolLiveModelEvaluation\")")
        assertFalse(Regex("dependsOn\\([^)]*reasoningProtocolLiveModelEvaluation").containsMatchIn(buildText))
        assertFalse(Regex("tasks\\.(test|check|build|assemble)[^{]*\\{[^}]*reasoningProtocolLiveModelEvaluation", RegexOption.DOT_MATCHES_ALL)
            .containsMatchIn(buildText))
    }

    @Test
    fun `explicit opt-in live smoke evaluation`() = runBlocking {
        val loaded = LiveEvaluationConfigLoader.load(System.getenv(), Path.of("."))
        assumeTrue(loaded is EvaluationConfigLoad.Present, "PARKER_REASONING_EVAL_* configuration absent; live smoke not executed")
        val cfg = (loaded as EvaluationConfigLoad.Present).config
        val harness = ReasoningProtocolLiveModelEvaluationHarness(cfg, "unit-1-live-smoke")
        val input = rememberInput()
        val observations = (1..cfg.repetitions).map { sequence -> harness.execute(input, sequence) }
        harness.writeArtifact(deriveCrossTrialObservations(observations))
        assertTrue(Files.isRegularFile(cfg.outputPath))
        assertTrue(Files.size(cfg.outputPath) > 0)
    }
}

private class StubOllamaEndpoint private constructor(
    private val server: HttpServer,
    private val responses: List<String>,
    private val delayMillis: Long,
) : AutoCloseable {
    private var responseIndex = 0
    val endpointUrl: String get() = "http://127.0.0.1:${server.address.port}/api/generate"
    val requestBodies = CopyOnWriteArrayList<String>()
    val responseBodies = CopyOnWriteArrayList<String>()

    override fun close() = server.stop(0)

    companion object {
        fun start(response: String, delayMillis: Long = 0): StubOllamaEndpoint =
            startSequence(listOf(response), delayMillis)

        fun startSequence(responses: List<String>, delayMillis: Long = 0): StubOllamaEndpoint {
            require(responses.isNotEmpty())
            val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
            lateinit var stub: StubOllamaEndpoint
            server.createContext("/api/generate") { exchange ->
                try {
                    stub.requestBodies.add(exchange.requestBody.readBytes().toString(StandardCharsets.UTF_8))
                    if (stub.delayMillis > 0) Thread.sleep(stub.delayMillis)
                    val index = synchronized(stub) {
                        val selected = stub.responseIndex.coerceAtMost(stub.responses.lastIndex)
                        stub.responseIndex++
                        selected
                    }
                    val escaped = jsonEscape(stub.responses[index])
                    val bodyText = "{\"model\":\"synthetic-test-model\",\"response\":\"$escaped\",\"done\":true," +
                        "\"prompt_eval_count\":17,\"eval_count\":5,\"total_duration\":1000}"
                    stub.responseBodies.add(bodyText)
                    val body = bodyText.toByteArray(StandardCharsets.UTF_8)
                    exchange.responseHeaders.add("Content-Type", "application/json")
                    exchange.sendResponseHeaders(200, body.size.toLong())
                    exchange.responseBody.write(body)
                } finally {
                    exchange.close()
                }
            }
            stub = StubOllamaEndpoint(server, responses, delayMillis)
            server.start()
            return stub
        }

        private fun jsonEscape(value: String): String = buildString {
            value.forEach { character ->
                when (character) {
                    '"' -> append("\\\"")
                    '\\' -> append("\\\\")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> append(character)
                }
            }
        }
    }
}
