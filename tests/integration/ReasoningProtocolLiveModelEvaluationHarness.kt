package parker.integration

import kotlinx.coroutines.TimeoutCancellationException
import parker.core.interfaces.ConversationId
import parker.core.interfaces.CorrelationId
import parker.core.interfaces.InboundOwnerMessage
import parker.core.interfaces.ModuleId
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.ReasoningContext
import parker.core.interfaces.ReasoningProviderRequest
import parker.core.interfaces.ReasoningProviderResponse
import parker.core.interfaces.ReasoningSubject
import parker.core.interfaces.Turn
import parker.core.interfaces.TurnId
import parker.core.runtime.DefaultReasoningPromptBuilder
import parker.core.runtime.LocalHttpModelInferenceClient
import parker.core.runtime.ModelReasoningProvider
import parker.core.runtime.TaggedReasoningResponseParser
import parker.core.runtime.UnclassifiableModelResponseException
import parker.core.runtime.defaultOllamaRequestBody
import parker.core.runtime.defaultOllamaResponseBody
import java.net.URI
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.time.Instant
import java.util.UUID

enum class ExpectedAction { GOAL, REPLY, REMEMBER, NOACTION }

enum class FixtureCategory { GOAL, REPLY, REMEMBER, NOACTION, ADVERSARIAL }

enum class ContentFidelity { EXACT, DEVIATION_OR_PARAPHRASE, NOT_APPLICABLE, INDETERMINATE }

enum class PrimaryClassification { A, B, C, D, E, F, G, H, I }

data class ConformanceFixture(
    val id: String,
    val ownerMessage: String,
    val expectedAction: ExpectedAction,
    val expectedContent: String? = null,
    val category: FixtureCategory,
    val consequential: Boolean,
    val synthetic: Boolean = true,
) {
    init {
        require(id.matches(Regex("[a-z0-9][a-z0-9.-]*"))) { "fixture id must be stable and machine-safe" }
        require(ownerMessage.isNotBlank()) { "fixture owner message must not be blank" }
        require(synthetic) { "live-model evaluation fixtures must be explicitly synthetic" }
    }
}

enum class ContextProfileId(val externalId: String) {
    MINIMAL_PRODUCTION_CONTEXT("minimal-production-context"),
    PRODUCTION_SELECTION_GUIDANCE("production-selection-guidance"),
    IDENTITY_CHANNEL_TIME_CONVERSATION("identity-channel-time-conversation"),
    DUPLICATED_CURRENT_REQUEST("duplicated-current-request"),
    CONVERSATION_HISTORY("conversation-history"),
    SYNTHETIC_MEMORY("synthetic-memory"),
    SYNTHETIC_WORLD_BELIEF("synthetic-world-belief"),
    SYNTHETIC_TOOL_CONTEXT("synthetic-tool-context"),
    MIXED_FULL_PRODUCTION_LIKE("mixed-full-production-like"),
}

data class SyntheticTrialInput(
    val fixture: ConformanceFixture,
    val profileId: ContextProfileId,
    val request: ReasoningProviderRequest,
    val stableInputHash: String,
)

object SyntheticConformanceCorpus {
    val fixtures: List<ConformanceFixture> = listOf(
        ConformanceFixture(
            id = "remember.synthetic-mug",
            ownerMessage = "Remember that my synthetic test mug is black.",
            expectedAction = ExpectedAction.REMEMBER,
            expectedContent = "My synthetic test mug is black.",
            category = FixtureCategory.REMEMBER,
            consequential = true,
        ),
        ConformanceFixture(
            id = "reply.synthetic-question",
            ownerMessage = "What colour is the synthetic test triangle?",
            expectedAction = ExpectedAction.REPLY,
            category = FixtureCategory.REPLY,
            consequential = false,
        ),
        ConformanceFixture(
            id = "goal.synthetic-checklist",
            ownerMessage = "Create a two-step checklist for the synthetic bench test.",
            expectedAction = ExpectedAction.GOAL,
            category = FixtureCategory.GOAL,
            consequential = true,
        ),
        ConformanceFixture(
            id = "noaction.synthetic-heartbeat",
            ownerMessage = "Synthetic protocol heartbeat; no response or action is required.",
            expectedAction = ExpectedAction.NOACTION,
            category = FixtureCategory.NOACTION,
            consequential = false,
        ),
        ConformanceFixture(
            id = "adversarial.quoted-remember",
            ownerMessage = "What does the synthetic phrase 'Remember that X' mean?",
            expectedAction = ExpectedAction.REPLY,
            category = FixtureCategory.ADVERSARIAL,
            consequential = false,
        ),
    )
}

object SyntheticContextProfiles {
    private val fixedInstant = Instant.parse("2026-01-15T12:00:00Z")

    fun construct(fixture: ConformanceFixture, profileId: ContextProfileId): SyntheticTrialInput {
        require(fixture.synthetic) { "non-synthetic fixture rejected" }
        val entries = when (profileId) {
            ContextProfileId.MINIMAL_PRODUCTION_CONTEXT,
            ContextProfileId.PRODUCTION_SELECTION_GUIDANCE,
            -> emptyList()

            ContextProfileId.IDENTITY_CHANNEL_TIME_CONVERSATION -> identityEntries()
            ContextProfileId.DUPLICATED_CURRENT_REQUEST ->
                identityEntries() + "Current request: ${fixture.ownerMessage}"
            ContextProfileId.CONVERSATION_HISTORY -> listOf(
                "Conversation history: Owner: The synthetic marker is ALPHA.",
                "Conversation history: Parker: Acknowledged as synthetic context.",
            )
            ContextProfileId.SYNTHETIC_MEMORY ->
                listOf("Relevant memory: The synthetic archive label is ORBIT-SEVEN.")
            ContextProfileId.SYNTHETIC_WORLD_BELIEF ->
                listOf("Current world belief: synthetic-lab-light is on (confidence 0.90).")
            ContextProfileId.SYNTHETIC_TOOL_CONTEXT ->
                listOf("Available tool: synthetic.checklist.create — creates a synthetic checklist.")
            ContextProfileId.MIXED_FULL_PRODUCTION_LIKE ->
                identityEntries() +
                    "Current request: ${fixture.ownerMessage}" +
                    listOf(
                        "Conversation history: Owner: The synthetic marker is ALPHA.",
                        "Conversation history: Parker: Acknowledged as synthetic context.",
                        "Relevant memory: The synthetic archive label is ORBIT-SEVEN.",
                        "Current world belief: synthetic-lab-light is on (confidence 0.90).",
                        "Available tool: synthetic.checklist.create — creates a synthetic checklist.",
                    )
        }
        val turn = Turn(
            turnId = TurnId("turn-eval-${fixture.id}"),
            conversationId = ConversationId("conversation-eval-fixed"),
            message = InboundOwnerMessage(
                channelId = ModuleId("channel.synthetic-evaluation"),
                senderPrincipalId = PrincipalId("user.synthetic-evaluation"),
                text = fixture.ownerMessage,
                timestamp = fixedInstant,
                correlationId = CorrelationId("correlation-eval-${fixture.id}"),
                metadata = mapOf("synthetic" to "true"),
            ),
            receivedAt = fixedInstant,
        )
        val request = ReasoningProviderRequest(
            subject = ReasoningSubject.OfTurn(turn),
            reasoningContext = ReasoningContext(entries),
        )
        val stableMaterial = buildString {
            append(fixture.id).append('\n')
            append(profileId.externalId).append('\n')
            append(fixture.ownerMessage).append('\n')
            entries.forEach { append(it).append('\n') }
        }
        return SyntheticTrialInput(fixture, profileId, request, sha256(stableMaterial))
    }

    private fun identityEntries(): List<String> = listOf(
        "Requesting principal: user.synthetic-evaluation",
        "Communication channel: channel.synthetic-evaluation",
        "Current time: $fixedInstant",
        "Conversation identifier: conversation-eval-fixed",
    )
}

data class LiveEvaluationConfig(
    val endpointUrl: String,
    val sanitizedEndpointIdentifier: String,
    val modelName: String,
    val timeoutMs: Long,
    val outputPath: Path,
    val repositoryCommit: String,
    val modelDigest: String?,
    val runtimeImageId: String?,
    val repetitions: Int,
)

sealed class EvaluationConfigLoad {
    object Absent : EvaluationConfigLoad()
    data class Present(val config: LiveEvaluationConfig) : EvaluationConfigLoad()
}

class EvaluationConfigurationException(message: String) : IllegalArgumentException(message)

object LiveEvaluationConfigLoader {
    const val ENDPOINT = "PARKER_REASONING_EVAL_ENDPOINT_URL"
    const val MODEL = "PARKER_REASONING_EVAL_MODEL_NAME"
    const val TIMEOUT = "PARKER_REASONING_EVAL_TIMEOUT_MS"
    const val OUTPUT = "PARKER_REASONING_EVAL_OUTPUT_PATH"
    const val COMMIT = "PARKER_REASONING_EVAL_REPOSITORY_COMMIT"
    const val DIGEST = "PARKER_REASONING_EVAL_MODEL_DIGEST"
    const val IMAGE = "PARKER_REASONING_EVAL_RUNTIME_IMAGE_ID"
    const val REPETITIONS = "PARKER_REASONING_EVAL_REPETITIONS"

    private val required = listOf(ENDPOINT, MODEL, TIMEOUT, OUTPUT, COMMIT)
    private val recognised = required + listOf(DIGEST, IMAGE, REPETITIONS)

    fun load(environment: Map<String, String>, repositoryRoot: Path): EvaluationConfigLoad {
        val presentKeys = recognised.filter { !environment[it].isNullOrBlank() }
        val requiredPresent = required.filter { !environment[it].isNullOrBlank() }
        if (requiredPresent.isEmpty()) return EvaluationConfigLoad.Absent

        val missing = required.filter { environment[it].isNullOrBlank() }
        if (missing.isNotEmpty()) {
            throw EvaluationConfigurationException(
                "Incomplete live-model evaluation configuration; missing keys: ${missing.joinToString()}",
            )
        }

        try {
            val endpoint = environment.getValue(ENDPOINT).trim()
            val uri = URI.create(endpoint)
            require(uri.scheme.equals("http", true) || uri.scheme.equals("https", true))
            require(uri.host != null)
            require(uri.userInfo == null && uri.rawQuery == null && uri.rawFragment == null)
            val sanitized = URI(uri.scheme.lowercase(), null, uri.host, uri.port, uri.path, null, null).toString()

            val timeout = environment.getValue(TIMEOUT).trim().toLong()
            require(timeout > 0)
            val repetitions = environment[REPETITIONS]?.trim()?.toInt() ?: 1
            require(repetitions > 0)

            val root = repositoryRoot.toAbsolutePath().normalize()
            val output = root.resolve(environment.getValue(OUTPUT).trim()).toAbsolutePath().normalize()
            val forbiddenRoots = listOf("src", "tests", "docs").map { root.resolve(it).normalize() }
            require(forbiddenRoots.none { output.startsWith(it) })

            val model = environment.getValue(MODEL).trim()
            val commit = environment.getValue(COMMIT).trim()
            require(model.isNotBlank() && commit.isNotBlank())

            return EvaluationConfigLoad.Present(
                LiveEvaluationConfig(
                    endpointUrl = endpoint,
                    sanitizedEndpointIdentifier = sanitized,
                    modelName = model,
                    timeoutMs = timeout,
                    outputPath = output,
                    repositoryCommit = commit,
                    modelDigest = environment[DIGEST]?.trim()?.takeIf { it.isNotBlank() },
                    runtimeImageId = environment[IMAGE]?.trim()?.takeIf { it.isNotBlank() },
                    repetitions = repetitions,
                ),
            )
        } catch (exception: EvaluationConfigurationException) {
            throw exception
        } catch (_: Exception) {
            throw EvaluationConfigurationException(
                "Invalid live-model evaluation configuration in keys: ${presentKeys.joinToString()}; values redacted",
            )
        }
    }
}

data class EndpointMetadata(
    val promptEvalCount: Long? = null,
    val evalCount: Long? = null,
    val totalDuration: Long? = null,
    val loadDuration: Long? = null,
    val promptEvalDuration: Long? = null,
    val evalDuration: Long? = null,
)

data class TrialObservation(
    val runId: String,
    val fixtureId: String,
    val contextProfileId: String,
    val trialSequence: Int,
    val stableInputHash: String,
    val repositoryCommit: String,
    val modelName: String,
    val modelDigest: String?,
    val runtimeImageId: String?,
    val endpointIdentifier: String,
    val timeoutMs: Long,
    val prompt: String,
    val promptSha256: String,
    val requestBody: String?,
    val rawOllamaEnvelope: String?,
    val extractedResponse: String?,
    val parsedVariant: String?,
    val parserExceptionType: String?,
    val parserExceptionClassification: String?,
    val expectedAction: ExpectedAction,
    val actualAction: ExpectedAction?,
    val representationValid: Boolean,
    val contentFidelity: ContentFidelity,
    val latencyNanos: Long,
    val endpointMetadata: EndpointMetadata,
    val primaryClassification: PrimaryClassification,
    val contextSensitiveDrift: Boolean = false,
    val repeatabilityFailure: Boolean = false,
) {
    val passed: Boolean
        get() = primaryClassification == PrimaryClassification.A &&
            !contextSensitiveDrift && !repeatabilityFailure
}

private class TransportCapture {
    var prompt: String? = null
    var requestBody: String? = null
    var rawEnvelope: String? = null
    var extractedResponse: String? = null

    fun formatRequest(prompt: String, modelName: String): String {
        val body = defaultOllamaRequestBody(prompt, modelName)
        this.prompt = prompt
        requestBody = body
        return body
    }

    fun parseResponse(raw: String): String {
        val extracted = defaultOllamaResponseBody(raw)
        rawEnvelope = raw
        extractedResponse = extracted
        return extracted
    }
}

class ReasoningProtocolLiveModelEvaluationHarness(
    private val config: LiveEvaluationConfig,
    private val runId: String = UUID.randomUUID().toString(),
) {
    suspend fun execute(input: SyntheticTrialInput, trialSequence: Int): TrialObservation {
        require(trialSequence > 0)
        val capture = TransportCapture()
        val builder = DefaultReasoningPromptBuilder()
        val expectedPrompt = builder.buildPrompt(
            (input.request.subject as ReasoningSubject.OfTurn).turn,
            input.request.reasoningContext,
        )
        val client = LocalHttpModelInferenceClient(
            endpointUrl = config.endpointUrl,
            modelName = config.modelName,
            requestBodyFormatter = capture::formatRequest,
            responseBodyParser = capture::parseResponse,
        )
        val provider = ModelReasoningProvider(
            promptBuilder = builder,
            modelInferenceClient = client,
            responseParser = TaggedReasoningResponseParser(),
            timeoutMs = config.timeoutMs,
        )

        var parsed: ReasoningProviderResponse? = null
        var failure: Throwable? = null
        val startedAt = System.nanoTime()
        try {
            parsed = provider.reason(input.request)
        } catch (throwable: Throwable) {
            failure = throwable
        }
        val latency = System.nanoTime() - startedAt
        check(capture.prompt == null || capture.prompt == expectedPrompt) {
            "transparent request capture changed the production prompt"
        }
        return classify(input, trialSequence, expectedPrompt, capture, parsed, failure, latency)
    }

    fun writeArtifact(observations: List<TrialObservation>) {
        val parent = config.outputPath.parent
            ?: throw EvaluationConfigurationException("Evaluation output path must have a parent directory")
        Files.createDirectories(parent)
        Files.newBufferedWriter(config.outputPath, StandardCharsets.UTF_8).use { writer ->
            writer.append(EvaluationJsonLines.runHeader(runId, config)).append('\n')
            observations.forEach {
                writer.append(EvaluationJsonLines.trial(it)).append('\n')
                writer.flush()
            }
        }
    }

    private fun classify(
        input: SyntheticTrialInput,
        sequence: Int,
        expectedPrompt: String,
        capture: TransportCapture,
        parsed: ReasoningProviderResponse?,
        failure: Throwable?,
        latency: Long,
    ): TrialObservation {
        val raw = capture.extractedResponse
        val actualAction = parsed?.action()
        val multiple = raw?.let(::containsMultipleTaggedOutputs) == true
        val primary = when {
            failure is TimeoutCancellationException -> PrimaryClassification.H
            failure != null && failure !is UnclassifiableModelResponseException &&
                !(failure is IllegalArgumentException && raw != null) -> PrimaryClassification.I
            multiple -> PrimaryClassification.F
            failure != null -> classifyRejected(raw.orEmpty())
            actualAction != input.fixture.expectedAction -> PrimaryClassification.D
            contentFidelity(input.fixture, parsed) == ContentFidelity.EXACT ||
                contentFidelity(input.fixture, parsed) == ContentFidelity.NOT_APPLICABLE -> PrimaryClassification.A
            else -> PrimaryClassification.B
        }
        val fidelity = contentFidelity(input.fixture, parsed)
        val representationValid = failure == null && !multiple
        return TrialObservation(
            runId = runId,
            fixtureId = input.fixture.id,
            contextProfileId = input.profileId.externalId,
            trialSequence = sequence,
            stableInputHash = input.stableInputHash,
            repositoryCommit = config.repositoryCommit,
            modelName = config.modelName,
            modelDigest = config.modelDigest,
            runtimeImageId = config.runtimeImageId,
            endpointIdentifier = config.sanitizedEndpointIdentifier,
            timeoutMs = config.timeoutMs,
            prompt = expectedPrompt,
            promptSha256 = sha256(expectedPrompt),
            requestBody = capture.requestBody,
            rawOllamaEnvelope = capture.rawEnvelope,
            extractedResponse = raw,
            parsedVariant = parsed?.variantName(),
            parserExceptionType = failure?.javaClass?.name,
            parserExceptionClassification = when (failure) {
                is UnclassifiableModelResponseException -> "UNCLASSIFIABLE_MODEL_RESPONSE"
                is IllegalArgumentException -> "INVALID_RESPONSE_CONTENT"
                else -> null
            },
            expectedAction = input.fixture.expectedAction,
            actualAction = actualAction,
            representationValid = representationValid,
            contentFidelity = fidelity,
            latencyNanos = latency,
            endpointMetadata = extractEndpointMetadata(capture.rawEnvelope),
            primaryClassification = primary,
        )
    }
}

fun deriveCrossTrialObservations(observations: List<TrialObservation>): List<TrialObservation> {
    val repeatabilityKeys = observations.groupBy {
        listOf(it.fixtureId, it.contextProfileId, it.modelName, it.modelDigest, it.endpointIdentifier, it.timeoutMs)
    }.filterValues { group ->
        group.map { listOf(it.primaryClassification, it.actualAction, it.extractedResponse) }.distinct().size > 1
    }.keys
    val driftFixtures = observations.groupBy { listOf(it.fixtureId, it.modelName, it.modelDigest) }
        .filterValues { group -> group.mapNotNull { it.actualAction }.distinct().size > 1 }
        .keys
    return observations.map {
        val repeatKey = listOf(it.fixtureId, it.contextProfileId, it.modelName, it.modelDigest, it.endpointIdentifier, it.timeoutMs)
        val driftKey = listOf(it.fixtureId, it.modelName, it.modelDigest)
        it.copy(
            contextSensitiveDrift = driftKey in driftFixtures,
            repeatabilityFailure = repeatKey in repeatabilityKeys,
        )
    }
}

private fun ReasoningProviderResponse.action(): ExpectedAction = when (this) {
    is ReasoningProviderResponse.Goal -> ExpectedAction.GOAL
    is ReasoningProviderResponse.Reply -> ExpectedAction.REPLY
    is ReasoningProviderResponse.Remember -> ExpectedAction.REMEMBER
    ReasoningProviderResponse.NoAction -> ExpectedAction.NOACTION
}

private fun ReasoningProviderResponse.variantName(): String = when (this) {
    is ReasoningProviderResponse.Goal -> "Goal"
    is ReasoningProviderResponse.Reply -> "Reply"
    is ReasoningProviderResponse.Remember -> "Remember"
    ReasoningProviderResponse.NoAction -> "NoAction"
}

private fun contentFidelity(
    fixture: ConformanceFixture,
    response: ReasoningProviderResponse?,
): ContentFidelity {
    val expected = fixture.expectedContent ?: return ContentFidelity.NOT_APPLICABLE
    val actual = when (response) {
        is ReasoningProviderResponse.Goal -> response.text
        is ReasoningProviderResponse.Reply -> response.text
        is ReasoningProviderResponse.Remember -> response.text
        else -> return ContentFidelity.INDETERMINATE
    }
    return if (actual == expected) ContentFidelity.EXACT else ContentFidelity.DEVIATION_OR_PARAPHRASE
}

private fun classifyRejected(raw: String): PrimaryClassification {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return PrimaryClassification.G
    if (containsMultipleTaggedOutputs(trimmed)) return PrimaryClassification.F
    val exactTags = listOf("GOAL:", "REPLY:", "REMEMBER:")
    if (exactTags.any { trimmed == it } || listOf("GOAL", "REPLY", "REMEMBER", "NOACTION").any { it.startsWith(trimmed) }) {
        return PrimaryClassification.G
    }
    if (trimmed.startsWith("NOACTION")) return PrimaryClassification.C
    if (!trimmed.matches(Regex("^[A-Z]+:.*", RegexOption.DOT_MATCHES_ALL))) return PrimaryClassification.E
    return PrimaryClassification.C
}

private fun containsMultipleTaggedOutputs(raw: String): Boolean =
    Regex("(?m)^\\s*(?:GOAL:|REPLY:|REMEMBER:|NOACTION(?:\\s*$))").findAll(raw).count() > 1

private fun extractEndpointMetadata(raw: String?): EndpointMetadata {
    fun number(key: String): Long? = raw?.let {
        Regex("\\\"${Regex.escape(key)}\\\"\\s*:\\s*(\\d+)").find(it)?.groupValues?.get(1)?.toLongOrNull()
    }
    return EndpointMetadata(
        promptEvalCount = number("prompt_eval_count"),
        evalCount = number("eval_count"),
        totalDuration = number("total_duration"),
        loadDuration = number("load_duration"),
        promptEvalDuration = number("prompt_eval_duration"),
        evalDuration = number("eval_duration"),
    )
}

object EvaluationJsonLines {
    fun runHeader(runId: String, config: LiveEvaluationConfig): String = objectJson(
        listOf(
            "recordType" to "run",
            "runId" to runId,
            "repositoryCommit" to config.repositoryCommit,
            "modelName" to config.modelName,
            "modelDigest" to config.modelDigest,
            "runtimeImageId" to config.runtimeImageId,
            "endpointIdentifier" to config.sanitizedEndpointIdentifier,
            "timeoutMs" to config.timeoutMs,
            "repetitions" to config.repetitions,
        ),
    )

    fun trial(value: TrialObservation): String = objectJson(
        listOf(
            "recordType" to "trial",
            "runId" to value.runId,
            "fixtureId" to value.fixtureId,
            "contextProfileId" to value.contextProfileId,
            "trialSequence" to value.trialSequence,
            "stableInputHash" to value.stableInputHash,
            "repositoryCommit" to value.repositoryCommit,
            "modelName" to value.modelName,
            "modelDigest" to value.modelDigest,
            "runtimeImageId" to value.runtimeImageId,
            "endpointIdentifier" to value.endpointIdentifier,
            "timeoutMs" to value.timeoutMs,
            "prompt" to value.prompt,
            "promptSha256" to value.promptSha256,
            "requestBody" to value.requestBody,
            "rawOllamaEnvelope" to value.rawOllamaEnvelope,
            "extractedResponse" to value.extractedResponse,
            "parsedVariant" to value.parsedVariant,
            "parserExceptionType" to value.parserExceptionType,
            "parserExceptionClassification" to value.parserExceptionClassification,
            "expectedAction" to value.expectedAction.name,
            "actualAction" to value.actualAction?.name,
            "representationValid" to value.representationValid,
            "contentFidelity" to value.contentFidelity.name,
            "latencyNanos" to value.latencyNanos,
            "promptEvalCount" to value.endpointMetadata.promptEvalCount,
            "evalCount" to value.endpointMetadata.evalCount,
            "totalDuration" to value.endpointMetadata.totalDuration,
            "loadDuration" to value.endpointMetadata.loadDuration,
            "promptEvalDuration" to value.endpointMetadata.promptEvalDuration,
            "evalDuration" to value.endpointMetadata.evalDuration,
            "primaryClassification" to value.primaryClassification.name,
            "contextSensitiveDrift" to value.contextSensitiveDrift,
            "repeatabilityFailure" to value.repeatabilityFailure,
            "passed" to value.passed,
        ),
    )

    private fun objectJson(fields: List<Pair<String, Any?>>): String = fields.joinToString(
        prefix = "{",
        postfix = "}",
        separator = ",",
    ) { (key, value) -> "${quote(key)}:${jsonValue(value)}" }

    private fun jsonValue(value: Any?): String = when (value) {
        null -> "null"
        is Boolean, is Number -> value.toString()
        else -> quote(value.toString())
    }

    private fun quote(value: String): String = buildString {
        append('"')
        value.forEach { character ->
            when (character) {
                '"' -> append("\\\"")
                '\\' -> append("\\\\")
                '\b' -> append("\\b")
                '\u000C' -> append("\\f")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> if (character.code < 0x20) append("\\u%04x".format(character.code)) else append(character)
            }
        }
        append('"')
    }
}

fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
    .digest(value.toByteArray(StandardCharsets.UTF_8))
    .joinToString("") { "%02x".format(it.toInt() and 0xff) }
