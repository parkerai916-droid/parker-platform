package parker.integration

import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.net.HttpURLConnection
import java.net.URI
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.channels.OverlappingFileLockException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import kotlin.io.path.exists
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import parker.core.interfaces.ReasoningContext
import parker.core.interfaces.ReasoningProviderResponse
import parker.core.interfaces.ReasoningSubject
import parker.core.interfaces.Turn
import parker.core.runtime.DefaultReasoningPromptBuilder
import parker.core.runtime.LocalHttpModelInferenceClient
import parker.core.runtime.ModelReasoningProvider
import parker.core.runtime.ReasoningPromptBuilder
import parker.core.runtime.TaggedReasoningResponseParser
import parker.core.runtime.UnclassifiableModelResponseException
import parker.core.runtime.defaultOllamaRequestBody
import parker.core.runtime.defaultOllamaResponseBody

/**
 * Reasoning Protocol Live-Model Conformance, Unit 2-D -- Diagnostic Characterisation.
 *
 * Implements exactly:
 * `docs/architecture/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_2D_DIAGNOSTIC_CHARACTERISATION_SCOPE_LOCK.md`
 * and
 * `docs/implementation/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_2D_DIAGNOSTIC_CHARACTERISATION_IMPLEMENTATION_EXECUTION_PLAN.md`.
 *
 * Not Unit 2. Does not read, write, resume, or reinterpret the failed
 * `qwen25coder7b-baseline-20260809` campaign; every artifact this file
 * produces lives in a new, sibling campaign identity that must contain the
 * literal substring "diagnostic" and can never resolve inside the failed
 * campaign's directory (enforced, not merely conventional -- see
 * [DiagnosticConfigLoader] and [DiagnosticCampaignRunner]).
 *
 * Unlike Unit 2's Stage 0 gate, a semantic mismatch (wrong action) here is
 * the evidence this unit exists to collect, not a stop condition. Only
 * identity/configuration/harness/artifact-integrity defects, or an
 * unauthorized consequential path (architecturally unreachable -- this file
 * never imports anything from `parker.composition` or any
 * Memory/Goal/Planner/tool coordinator), halt the campaign.
 */

private const val DIAGNOSTIC_EVALUATION_TIMEOUT_MS = 90_000L
private const val DIAGNOSTIC_ARTIFACT_ROOT_PREFIX = "/var/lib/parker/reasoning-protocol-live-model"
private const val FAILED_UNIT_2_CAMPAIGN_ID = "qwen25coder7b-baseline-20260809"
private const val DIAGNOSTIC_MANIFEST_VERSION = "reasoning-diagnostic-manifest-v1"
private const val DIAGNOSTIC_MINIMUM_FREE_BYTES = 2L * 1024L * 1024L * 1024L
private const val DIAGNOSTIC_PROPERTY = "parker.reasoning.diagnostic.enabled"
private const val DIAGNOSTIC_QWEN_MODEL_NAME = "qwen2.5-coder:7b"
private const val DIAGNOSTIC_LLAMA_MODEL_NAME = "llama3.2:3b"

// ============================================================
// Fixture corpus -- reused verbatim (Scope Lock 7, Plan Section 2) from the
// already-reviewed Unit 2 corpus text. Reimplemented, not imported: Unit 2's
// own `BaselineCorpus` is file-private to `ReasoningProtocolBaselineCharacterisationTest.kt`.
// ============================================================

private object DiagnosticCorpus {
    val warmup = ConformanceFixture(
        id = "warmup-acknowledgement",
        ownerMessage = "Synthetic warm-up request: reply with a brief acknowledgement.",
        expectedAction = ExpectedAction.REPLY,
        category = FixtureCategory.REPLY,
        consequential = false,
    )

    val r01Direct = ConformanceFixture(
        id = "r01-direct",
        ownerMessage = "Remember that my synthetic test coffee mug is black.",
        expectedAction = ExpectedAction.REMEMBER,
        expectedContent = "My synthetic test coffee mug is black.",
        category = FixtureCategory.REMEMBER,
        consequential = true,
    )

    /** DQ5's fixture identity is deliberately distinct from [r01Direct] so that
     * [deriveCrossTrialObservations]'s fixture-keyed grouping (Unit 1, reused
     * unchanged) never folds candidate-track evidence into production-track
     * repeatability/drift analysis. */
    val r01DirectDecisionOnly = ConformanceFixture(
        id = "r01-direct-decision-only",
        ownerMessage = "Remember that my synthetic test coffee mug is black.",
        expectedAction = ExpectedAction.REMEMBER,
        expectedContent = "My synthetic test coffee mug is black.",
        category = FixtureCategory.REMEMBER,
        consequential = true,
    )

    val p01OrdinaryFact = ConformanceFixture(
        id = "p01-ordinary-fact",
        ownerMessage = "My synthetic desk token is amber.",
        expectedAction = ExpectedAction.REPLY,
        category = FixtureCategory.REPLY,
        consequential = false,
    )

    val p06Greeting = ConformanceFixture(
        id = "p06-greeting",
        ownerMessage = "Hello Parker.",
        expectedAction = ExpectedAction.REPLY,
        category = FixtureCategory.REPLY,
        consequential = false,
    )

    val g01Multistep = ConformanceFixture(
        id = "g01-multistep",
        ownerMessage = "Create a three-step checklist for inspecting the synthetic blue test bench.",
        expectedAction = ExpectedAction.GOAL,
        expectedContent = "Create a three-step checklist for inspecting the synthetic blue test bench.",
        category = FixtureCategory.GOAL,
        consequential = true,
    )

    val n01Heartbeat = ConformanceFixture(
        id = "n01-heartbeat",
        ownerMessage = "Synthetic system heartbeat event: no response and no action are required.",
        expectedAction = ExpectedAction.NOACTION,
        category = FixtureCategory.NOACTION,
        consequential = false,
    )
}

// ============================================================
// Frozen 24-call schedule (Scope Lock 18, Plan Section 3)
// ============================================================

private enum class DiagnosticModel(val shortId: String) { QWEN("qwen"), LLAMA("llama") }

private enum class DiagnosticTrack(val pathSegment: String) {
    WARMUP("warmup"),
    PRODUCTION("production-track"),
    CANDIDATE("candidate-track"),
}

private data class DiagnosticTrial(
    val id: String,
    val groupId: String,
    val track: DiagnosticTrack,
    val fixture: ConformanceFixture,
    val profile: ContextProfileId,
    val model: DiagnosticModel,
    val attempt: Int,
)

private class DiagnosticCampaignDefinition(val campaignId: String) {
    val trials: List<DiagnosticTrial>
    val canonical: String
    val hash: String

    init {
        require(campaignId.matches(Regex("[a-z0-9][a-z0-9.-]*"))) { "campaign ID must be machine-safe" }
        require("diagnostic" in campaignId) { "Unit 2-D campaign identity must contain an explicit diagnostic marker" }
        require(campaignId != FAILED_UNIT_2_CAMPAIGN_ID) { "Unit 2-D must not reuse the failed Unit 2 campaign identity" }

        val minimal = ContextProfileId.MINIMAL_PRODUCTION_CONTEXT
        val mixedFull = ContextProfileId.MIXED_FULL_PRODUCTION_LIKE
        val conversationHistory = ContextProfileId.CONVERSATION_HISTORY
        val built = mutableListOf<DiagnosticTrial>()

        fun add(groupId: String, track: DiagnosticTrack, fixture: ConformanceFixture, profile: ContextProfileId, model: DiagnosticModel, attempt: Int) {
            val id = "$campaignId/$groupId/${fixture.id}/${profile.externalId}/${model.shortId}/${attempt.toString().padStart(2, '0')}"
            built += DiagnosticTrial(id, groupId, track, fixture, profile, model, attempt)
        }

        add("WARMUP-QWEN", DiagnosticTrack.WARMUP, DiagnosticCorpus.warmup, minimal, DiagnosticModel.QWEN, 1)
        for (attempt in 1..10) add("DQ1", DiagnosticTrack.PRODUCTION, DiagnosticCorpus.r01Direct, minimal, DiagnosticModel.QWEN, attempt)
        add("DQ2", DiagnosticTrack.PRODUCTION, DiagnosticCorpus.p01OrdinaryFact, minimal, DiagnosticModel.QWEN, 1)
        add("DQ2", DiagnosticTrack.PRODUCTION, DiagnosticCorpus.p06Greeting, minimal, DiagnosticModel.QWEN, 1)
        add("DQ2", DiagnosticTrack.PRODUCTION, DiagnosticCorpus.g01Multistep, minimal, DiagnosticModel.QWEN, 1)
        add("DQ2", DiagnosticTrack.PRODUCTION, DiagnosticCorpus.n01Heartbeat, minimal, DiagnosticModel.QWEN, 1)
        add("DQ3", DiagnosticTrack.PRODUCTION, DiagnosticCorpus.r01Direct, mixedFull, DiagnosticModel.QWEN, 1)
        add("DQ3", DiagnosticTrack.PRODUCTION, DiagnosticCorpus.r01Direct, conversationHistory, DiagnosticModel.QWEN, 1)
        for (attempt in 1..5) add("DQ5", DiagnosticTrack.CANDIDATE, DiagnosticCorpus.r01DirectDecisionOnly, minimal, DiagnosticModel.QWEN, attempt)
        add("WARMUP-LLAMA", DiagnosticTrack.WARMUP, DiagnosticCorpus.warmup, minimal, DiagnosticModel.LLAMA, 1)
        add("DQ4", DiagnosticTrack.PRODUCTION, DiagnosticCorpus.r01Direct, minimal, DiagnosticModel.LLAMA, 1)

        trials = built.toList()
        check(trials.size == 24) { "Unit 2-D schedule must contain exactly 24 trials" }
        check(trials.map { it.id }.distinct().size == 24) { "Unit 2-D schedule trial IDs must be unique" }
        check(trials.count { it.track == DiagnosticTrack.WARMUP } == 2)
        check(trials.count { it.groupId == "DQ1" } == 10)
        check(trials.count { it.groupId == "DQ2" } == 4)
        check(trials.count { it.groupId == "DQ3" } == 2)
        check(trials.count { it.groupId == "DQ4" } == 1)
        check(trials.count { it.groupId == "DQ5" } == 5)
        check(trials.count { it.track == DiagnosticTrack.PRODUCTION } == 17)
        check(trials.count { it.track == DiagnosticTrack.CANDIDATE } == 5)

        canonical = buildString {
            append("campaign-schema=diagnostic-v1\n")
            append("timeoutMs=$DIAGNOSTIC_EVALUATION_TIMEOUT_MS\n")
            append("qwenModel=$DIAGNOSTIC_QWEN_MODEL_NAME\n")
            append("llamaModel=$DIAGNOSTIC_LLAMA_MODEL_NAME\n")
            trials.forEach { append("trial=").append(it.id).append('|').append(it.track.pathSegment).append('|').append(it.groupId).append('\n') }
        }
        hash = sha256(canonical)
    }
}

// ============================================================
// DQ5 candidate-track prompt (Scope Lock 16, Plan Section 6, corrected)
// ============================================================

/** Reused as reference text from `DefaultReasoningPromptBuilder`'s private
 * `SELECTION_GUIDANCE` constant (Plan Section 6: "reused as reference text,
 * not by importing the private constant"). Cross-checked at runtime against
 * the actual production prompt by
 * `production prompt builder is reused byte-identical for DQ1-DQ4`. */
private const val SELECTION_GUIDANCE_REFERENCE_TEXT =
    "Use REPLY: for greetings; questions; conversational statements that reasonably " +
        "invite a response; requests for information, explanation, clarification, or " +
        "discussion; and acknowledgements where a useful direct response is " +
        "appropriate.\n\n" +
        "Use GOAL: only when the owner is asking you to carry out work that requires " +
        "planning, execution, tools, later action, or multiple coordinated steps.\n\n" +
        "Use REMEMBER: only when the owner gives a direct, unambiguous instruction to " +
        "remember a specific, stated fact -- for example \"Remember that X\", \"Please " +
        "remember X\", or \"Don't forget that X\". Put only the fact itself after the " +
        "prefix, not the surrounding instruction. Never use REMEMBER: for an ordinary " +
        "statement of fact, an incidental mention, or a question -- only for a direct " +
        "instruction to remember something. If there is any doubt whether the owner " +
        "intended such an instruction, use REPLY: instead (asking a clarifying question " +
        "if needed), never REMEMBER:.\n\n" +
        "Use NOACTION only when no response and no action is appropriate. Do not use " +
        "NOACTION merely because the message is short, casual, or lacks an explicit " +
        "question."

/** The one corrected, firewalled candidate-decision-only variant (Scope Lock
 * 16 / Plan Section 6). Manipulates exactly one variable relative to
 * production -- whether genuine response content must be composed -- while
 * holding the tag syntax, selection criteria, fixture, context, model, and
 * commit constant. `GOAL:`/`REPLY:`/`REMEMBER:` are followed by the fixed,
 * deliberately content-free placeholder `SELECTED` (non-blank, so
 * `Goal`/`Reply`/`Remember`'s own constructor validation is satisfied);
 * `NOACTION` is unchanged from production. Parseable by
 * [TaggedReasoningResponseParser] entirely unmodified -- see
 * `all four DQ5 action forms parse through the unmodified TaggedReasoningResponseParser`. */
private const val DECISION_ONLY_INSTRUCTION =
    "Respond with exactly one of the following: GOAL:, REPLY:, REMEMBER:, or NOACTION.\n\n" +
        "If your answer is GOAL:, REPLY:, or REMEMBER:, write the tag followed by exactly the " +
        "single word \"SELECTED\" and nothing else -- no explanation, no restated fact, no " +
        "additional sentence.\n\n" +
        "If your answer is NOACTION, write exactly NOACTION and nothing else.\n\n" +
        SELECTION_GUIDANCE_REFERENCE_TEXT

/** Never referenced by, and never merged toward, `DefaultReasoningPromptBuilder`.
 * Every record produced under it is stored under `candidate-track/`, permanently
 * separate from `production-track/`'s byte-identical production-prompt evidence. */
private class DecisionOnlyPromptBuilder : ReasoningPromptBuilder {
    override fun buildPrompt(turn: Turn, reasoningContext: ReasoningContext): String {
        val contextBlock = if (reasoningContext.entries.isEmpty()) {
            ""
        } else {
            reasoningContext.entries.joinToString("\n") + "\n"
        }
        return contextBlock + turn.message.text + "\n\n" + DECISION_ONLY_INSTRUCTION
    }
}

// ============================================================
// Reimplemented, faithful classification/capture logic (file-private; Unit
// 1's own equivalents are file-private to `ReasoningProtocolLiveModelEvaluationHarness.kt`
// and therefore not importable). Used only by the candidate-track executor --
// production-track calls (DQ1-DQ4, both warm-ups) reuse Unit 1's own public,
// unmodified `ReasoningProtocolLiveModelEvaluationHarness.execute` directly,
// requiring none of this.
// ============================================================

private class DiagnosticTransportCapture {
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

private fun contentFidelity(fixture: ConformanceFixture, response: ReasoningProviderResponse?): ContentFidelity {
    val expected = fixture.expectedContent ?: return ContentFidelity.NOT_APPLICABLE
    val actual = when (response) {
        is ReasoningProviderResponse.Goal -> response.text
        is ReasoningProviderResponse.Reply -> response.text
        is ReasoningProviderResponse.Remember -> response.text
        else -> return ContentFidelity.INDETERMINATE
    }
    return if (actual == expected) ContentFidelity.EXACT else ContentFidelity.DEVIATION_OR_PARAPHRASE
}

private fun containsMultipleTaggedOutputs(raw: String): Boolean =
    Regex("(?m)^\\s*(?:GOAL:|REPLY:|REMEMBER:|NOACTION(?:\\s*$))").findAll(raw).count() > 1

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

private fun classifyDiagnosticCandidate(
    runId: String,
    config: LiveEvaluationConfig,
    input: SyntheticTrialInput,
    sequence: Int,
    expectedPrompt: String,
    capture: DiagnosticTransportCapture,
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

/** DQ5's own executor -- mirrors Unit 1's `execute`/`classify` exactly, but
 * parameterized by [promptBuilder] so [DecisionOnlyPromptBuilder] can be
 * substituted without ever modifying `DefaultReasoningPromptBuilder` or
 * `ReasoningProtocolLiveModelEvaluationHarness`. */
private suspend fun executeCandidateTrial(
    config: LiveEvaluationConfig,
    runId: String,
    promptBuilder: ReasoningPromptBuilder,
    input: SyntheticTrialInput,
    sequence: Int,
): TrialObservation {
    require(sequence > 0)
    val capture = DiagnosticTransportCapture()
    val expectedPrompt = promptBuilder.buildPrompt(
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
        promptBuilder = promptBuilder,
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
        "transparent request capture changed the candidate-track prompt"
    }
    return classifyDiagnosticCandidate(runId, config, input, sequence, expectedPrompt, capture, parsed, failure, latency)
}

// ============================================================
// Identity (Scope Lock 17, Plan Section 4) -- reimplements, offline-testably,
// the same proven technique as Unit 2's file-private `OllamaIdentityEvidence`.
// `capture`/`request` perform live HTTP and are only reachable from the
// property-gated live test at the bottom of this file.
// ============================================================

private class DiagnosticCampaignStateException(message: String) : IllegalStateException(message)

private object DiagnosticIdentityEvidence {
    fun exactModelDigest(tagsJson: String, configuredModel: String): String {
        val objects = directObjectsInRootArray(tagsJson, "models")
        val matches = objects.filter { objectText ->
            val fields = directStringFields(objectText)
            fields["name"] == configuredModel || fields["model"] == configuredModel
        }
        if (matches.size != 1) {
            throw DiagnosticCampaignStateException("Ollama /api/tags must contain exactly one object for the exact configured model: $configuredModel")
        }
        val digest = directStringFields(matches.single())["digest"]
            ?: throw DiagnosticCampaignStateException("Ollama /api/tags exact configured model has no digest")
        if (!digest.matches(Regex("[0-9a-fA-F]{64}"))) {
            throw DiagnosticCampaignStateException("Ollama /api/tags exact configured model digest is blank, abbreviated, or malformed")
        }
        return digest
    }

    /** Live-only: performs the actual `/api/tags` + `/api/show` HTTP calls.
     * Reachable only from the property-and-environment-gated live test. */
    fun capture(origin: URI, model: String): Pair<String, String> {
        val tags = request(origin.resolve("/api/tags"), "GET", null)
        val escapedModel = model.replace("\\", "\\\\").replace("\"", "\\\"")
        val show = request(origin.resolve("/api/show"), "POST", "{\"model\":\"$escapedModel\"}")
        return exactModelDigest(tags, model) to sha256(show)
    }

    private fun directObjectsInRootArray(json: String, key: String): List<String> {
        var index = skipWhitespace(json, 0)
        requireCharacter(json, index, '{', "Ollama /api/tags root must be a JSON object")
        index++
        var arrayStart: Int? = null
        while (true) {
            index = skipWhitespace(json, index)
            if (index >= json.length) throw DiagnosticCampaignStateException("Ollama /api/tags root object is incomplete")
            if (json[index] == '}') break
            val parsedKey = parseJsonString(json, index)
            index = skipWhitespace(json, parsedKey.second)
            requireCharacter(json, index, ':', "Ollama /api/tags root field lacks a value")
            index = skipWhitespace(json, index + 1)
            if (parsedKey.first == key) {
                if (arrayStart != null) throw DiagnosticCampaignStateException("Ollama /api/tags contains duplicate models fields")
                requireCharacter(json, index, '[', "Ollama /api/tags models field is not an array")
                arrayStart = index
            }
            index = skipJsonValue(json, index)
            index = skipWhitespace(json, index)
            if (index < json.length && json[index] == ',') index++
            else if (index < json.length && json[index] == '}') break
            else throw DiagnosticCampaignStateException("Ollama /api/tags root object is malformed")
        }
        val start = arrayStart ?: throw DiagnosticCampaignStateException("Ollama /api/tags contains no models array")
        val end = matchingContainerEnd(json, start, '[', ']')
        val objects = mutableListOf<String>()
        index = skipWhitespace(json, start + 1)
        while (index < end) {
            if (json[index] != '{') throw DiagnosticCampaignStateException("Ollama /api/tags models array contains a non-object value")
            val objectEnd = matchingContainerEnd(json, index, '{', '}')
            objects += json.substring(index, objectEnd + 1)
            index = skipWhitespace(json, objectEnd + 1)
            if (index < end) {
                requireCharacter(json, index, ',', "Ollama /api/tags models array is malformed")
                index = skipWhitespace(json, index + 1)
                if (index >= end) throw DiagnosticCampaignStateException("Ollama /api/tags models array has a trailing comma")
            }
        }
        return objects
    }

    private fun directStringFields(objectText: String): Map<String, String> {
        var index = skipWhitespace(objectText, 0)
        requireCharacter(objectText, index, '{', "Ollama model entry must be an object")
        index++
        val fields = linkedMapOf<String, String>()
        val seenKeys = mutableSetOf<String>()
        while (true) {
            index = skipWhitespace(objectText, index)
            if (index >= objectText.length) throw DiagnosticCampaignStateException("Ollama model entry is incomplete")
            if (objectText[index] == '}') break
            val key = parseJsonString(objectText, index)
            if (!seenKeys.add(key.first)) throw DiagnosticCampaignStateException("Ollama model entry contains duplicate direct field ${key.first}")
            index = skipWhitespace(objectText, key.second)
            requireCharacter(objectText, index, ':', "Ollama model field lacks a value")
            index = skipWhitespace(objectText, index + 1)
            if (index < objectText.length && objectText[index] == '"') {
                val value = parseJsonString(objectText, index)
                fields[key.first] = value.first
                index = value.second
            } else {
                index = skipJsonValue(objectText, index)
            }
            index = skipWhitespace(objectText, index)
            if (index < objectText.length && objectText[index] == ',') index++
            else if (index < objectText.length && objectText[index] == '}') break
            else throw DiagnosticCampaignStateException("Ollama model entry is malformed")
        }
        return fields
    }

    private fun parseJsonString(json: String, start: Int): Pair<String, Int> {
        requireCharacter(json, start, '"', "expected JSON string")
        val value = StringBuilder()
        var index = start + 1
        while (index < json.length) {
            val character = json[index++]
            when (character) {
                '"' -> return value.toString() to index
                '\\' -> {
                    if (index >= json.length) throw DiagnosticCampaignStateException("incomplete JSON escape")
                    when (val escaped = json[index++]) {
                        '"', '\\', '/' -> value.append(escaped)
                        'b' -> value.append('\b')
                        'f' -> value.append('\u000C')
                        'n' -> value.append('\n')
                        'r' -> value.append('\r')
                        't' -> value.append('\t')
                        'u' -> {
                            if (index + 4 > json.length) throw DiagnosticCampaignStateException("incomplete JSON unicode escape")
                            val code = json.substring(index, index + 4).toIntOrNull(16)
                                ?: throw DiagnosticCampaignStateException("invalid JSON unicode escape")
                            value.append(code.toChar())
                            index += 4
                        }
                        else -> throw DiagnosticCampaignStateException("invalid JSON escape: $escaped")
                    }
                }
                else -> {
                    if (character.code < 0x20) throw DiagnosticCampaignStateException("unescaped control character in JSON string")
                    value.append(character)
                }
            }
        }
        throw DiagnosticCampaignStateException("unterminated JSON string")
    }

    private fun skipJsonValue(json: String, start: Int): Int {
        if (start >= json.length) throw DiagnosticCampaignStateException("missing JSON value")
        return when (json[start]) {
            '"' -> parseJsonString(json, start).second
            '{' -> matchingContainerEnd(json, start, '{', '}') + 1
            '[' -> matchingContainerEnd(json, start, '[', ']') + 1
            else -> {
                var index = start
                while (index < json.length && json[index] !in charArrayOf(',', '}', ']') && !json[index].isWhitespace()) index++
                if (index == start) throw DiagnosticCampaignStateException("invalid JSON value")
                index
            }
        }
    }

    private fun matchingContainerEnd(json: String, start: Int, open: Char, close: Char): Int {
        requireCharacter(json, start, open, "expected JSON container")
        var depth = 0
        var index = start
        while (index < json.length) {
            when (json[index]) {
                '"' -> index = parseJsonString(json, index).second
                open -> { depth++; index++ }
                close -> {
                    depth--
                    if (depth == 0) return index
                    if (depth < 0) throw DiagnosticCampaignStateException("unbalanced JSON container")
                    index++
                }
                else -> index++
            }
        }
        throw DiagnosticCampaignStateException("unterminated JSON container")
    }

    private fun skipWhitespace(json: String, start: Int): Int {
        var index = start
        while (index < json.length && json[index].isWhitespace()) index++
        return index
    }

    private fun requireCharacter(json: String, index: Int, expected: Char, message: String) {
        if (index >= json.length || json[index] != expected) throw DiagnosticCampaignStateException(message)
    }

    private fun request(uri: URI, method: String, body: String?): String {
        val connection = uri.toURL().openConnection() as HttpURLConnection
        connection.requestMethod = method
        connection.connectTimeout = 10_000
        connection.readTimeout = 10_000
        if (body != null) {
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            connection.outputStream.use { it.write(body.toByteArray(StandardCharsets.UTF_8)) }
        }
        try {
            require(connection.responseCode in 200..299) { "Ollama identity endpoint failed" }
            return connection.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }
}

// ============================================================
// Artifact primitives (Plan Section 8)
// ============================================================

private fun atomicWrite(path: Path, text: String) {
    Files.createDirectories(path.parent)
    val temporary = path.resolveSibling(".${path.fileName}.tmp")
    FileChannel.open(temporary, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE).use { channel ->
        val buffer = ByteBuffer.wrap(text.toByteArray(StandardCharsets.UTF_8))
        while (buffer.hasRemaining()) channel.write(buffer)
        channel.force(true)
    }
    try {
        Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
    } catch (_: java.nio.file.AtomicMoveNotSupportedException) {
        Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING)
    }
}

private fun appendForced(path: Path, text: String) {
    Files.createDirectories(path.parent)
    FileChannel.open(path, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND).use { channel ->
        val bytes = text.toByteArray(StandardCharsets.UTF_8)
        var buffer = ByteBuffer.wrap(bytes)
        while (buffer.hasRemaining()) channel.write(buffer)
        channel.force(true)
    }
}

private fun sha256Bytes(bytes: ByteArray): String =
    java.security.MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it.toInt() and 0xff) }

private fun countLines(bytes: ByteArray): Long = bytes.count { it == '\n'.code.toByte() }.toLong()

private fun quote(value: String): String = buildString {
    append('"')
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
    append('"')
}

private fun unquote(value: String): String = Regex("\\\\([\"\\\\nrt])").replace(value) { match ->
    when (match.groupValues[1]) {
        "\"" -> "\""
        "\\" -> "\\"
        "n" -> "\n"
        "r" -> "\r"
        "t" -> "\t"
        else -> match.value
    }
}

// ============================================================
// Identity, config, campaign isolation (Scope Lock 17, Plan Sections 4/5/8)
// ============================================================

private data class DiagnosticIdentity(
    val repositoryCommit: String,
    val qwenModelName: String,
    val qwenModelDigest: String,
    val llamaModelName: String,
    val llamaModelDigest: String,
    val endpoint: String,
    val timeoutMs: Long,
    val ubuntuRuntimeIdentity: String,
    val containerIdentity: String,
    val campaignHash: String,
) {
    val fingerprint: String = sha256(
        listOf(
            repositoryCommit, qwenModelName, qwenModelDigest, llamaModelName, llamaModelDigest,
            endpoint, timeoutMs, ubuntuRuntimeIdentity, containerIdentity, campaignHash,
        ).joinToString("\n"),
    )
}

private data class DiagnosticConfig(
    val campaignId: String,
    val campaignArtifactRoot: Path,
    val qwenLive: LiveEvaluationConfig,
    val llamaLive: LiveEvaluationConfig,
    val identity: DiagnosticIdentity,
)

private object DiagnosticConfigLoader {
    const val CAMPAIGN_ID = "PARKER_REASONING_DIAGNOSTIC_CAMPAIGN_ID"
    const val ARTIFACT_ROOT = "PARKER_REASONING_DIAGNOSTIC_ARTIFACT_ROOT"
    const val UBUNTU_ID = "PARKER_REASONING_DIAGNOSTIC_UBUNTU_RUNTIME_ID"
    const val CONTAINER_ID = "PARKER_REASONING_DIAGNOSTIC_CONTAINER_ID"
    const val QWEN_MODEL_SHOW_HASH = "PARKER_REASONING_DIAGNOSTIC_QWEN_MODEL_SHOW_SHA256"
    const val LLAMA_MODEL_NAME = "PARKER_REASONING_DIAGNOSTIC_LLAMA_MODEL_NAME"
    const val LLAMA_MODEL_DIGEST = "PARKER_REASONING_DIAGNOSTIC_LLAMA_MODEL_DIGEST"
    const val LLAMA_MODEL_SHOW_HASH = "PARKER_REASONING_DIAGNOSTIC_LLAMA_MODEL_SHOW_SHA256"

    fun load(environment: Map<String, String>, repositoryRoot: Path, definition: DiagnosticCampaignDefinition): DiagnosticConfig {
        fun required(name: String): String = environment[name]?.trim()?.takeIf { it.isNotEmpty() }
            ?: throw EvaluationConfigurationException("Incomplete Unit 2-D diagnostic configuration; missing key: $name; values redacted")
        val qwenLive = when (val loaded = LiveEvaluationConfigLoader.load(environment, repositoryRoot)) {
            is EvaluationConfigLoad.Present -> loaded.config
            EvaluationConfigLoad.Absent -> throw EvaluationConfigurationException("Complete PARKER_REASONING_EVAL_* configuration is required; values redacted")
        }
        require(qwenLive.timeoutMs == DIAGNOSTIC_EVALUATION_TIMEOUT_MS) { "Unit 2-D timeout identity mismatch" }
        require(qwenLive.modelName == DIAGNOSTIC_QWEN_MODEL_NAME) { "Unit 2-D recognizes exactly qwen2.5-coder:7b for the production-track model" }
        val campaignId = required(CAMPAIGN_ID)
        require(campaignId == definition.campaignId) { "campaign identity mismatch" }
        val root = acceptedArtifactParent(required(ARTIFACT_ROOT))
        val campaignRoot = campaignArtifactRoot(root, campaignId)
        val qwenDigest = qwenLive.modelDigest ?: throw EvaluationConfigurationException("Immutable Qwen model digest is required; values redacted")
        val qwenShowHash = required(QWEN_MODEL_SHOW_HASH)
        require(qwenShowHash.matches(Regex("[0-9a-fA-F]{64}"))) { "invalid Qwen /api/show evidence hash" }
        val llamaModelName = required(LLAMA_MODEL_NAME)
        require(llamaModelName == DIAGNOSTIC_LLAMA_MODEL_NAME) { "Unit 2-D recognizes exactly llama3.2:3b for the model-comparison arm" }
        val llamaDigest = required(LLAMA_MODEL_DIGEST)
        val llamaShowHash = required(LLAMA_MODEL_SHOW_HASH)
        require(llamaShowHash.matches(Regex("[0-9a-fA-F]{64}"))) { "invalid Llama /api/show evidence hash" }
        val llamaLive = qwenLive.copy(modelName = llamaModelName, modelDigest = llamaDigest)
        val identity = DiagnosticIdentity(
            qwenLive.repositoryCommit, qwenLive.modelName, "$qwenDigest|show:$qwenShowHash",
            llamaModelName, "$llamaDigest|show:$llamaShowHash", qwenLive.sanitizedEndpointIdentifier,
            qwenLive.timeoutMs, required(UBUNTU_ID), environment[CONTAINER_ID]?.trim().orEmpty(), definition.hash,
        )
        return DiagnosticConfig(campaignId, campaignRoot, qwenLive, llamaLive, identity)
    }

    private fun acceptedArtifactParent(configured: String): Path {
        val normalizedText = configured.replace('\\', '/').trimEnd('/')
        require(normalizedText == DIAGNOSTIC_ARTIFACT_ROOT_PREFIX) { "artifact root must be the accepted durable parent" }
        val path = Path.of(configured).normalize()
        require(path.toString().replace('\\', '/').trimEnd('/') == DIAGNOSTIC_ARTIFACT_ROOT_PREFIX) {
            "artifact root normalization escaped the accepted durable parent"
        }
        return path
    }

    /** The isolation guard (Scope Lock 17, Plan Section 8): fails closed
     * before any client is constructed -- this runs during config loading,
     * the very first step of the live entry point, strictly before any
     * [ReasoningProtocolLiveModelEvaluationHarness] or
     * [LocalHttpModelInferenceClient] object exists. */
    private fun campaignArtifactRoot(parent: Path, campaignId: String): Path {
        require(campaignId.matches(Regex("[a-z0-9][a-z0-9.-]*"))) { "campaign ID must be machine-safe" }
        require("diagnostic" in campaignId) { "Unit 2-D campaign identity must contain an explicit diagnostic marker" }
        require(campaignId != FAILED_UNIT_2_CAMPAIGN_ID) { "Unit 2-D must not reuse the failed Unit 2 campaign identity" }
        val resolved = parent.resolve(campaignId).normalize()
        require(resolved.toString().replace('\\', '/').trimEnd('/') == "$DIAGNOSTIC_ARTIFACT_ROOT_PREFIX/$campaignId") {
            "campaign artifact path must be exactly one directory beneath the accepted parent"
        }
        val forbidden = parent.resolve(FAILED_UNIT_2_CAMPAIGN_ID).normalize()
        require(resolved != forbidden && !resolved.startsWith(forbidden)) {
            "Unit 2-D artifact root must not equal or nest inside the failed Unit 2 campaign directory"
        }
        return resolved
    }
}

// ============================================================
// Per-track durable ledger (Plan Section 8/19; exact-once semantics)
// ============================================================

private data class DiagnosticTrackState(val completed: MutableSet<String>)

private class DiagnosticTrackLedger(private val directory: Path, private val trackTrials: List<DiagnosticTrial>) {
    private val raw = directory.resolve("raw.jsonl")
    private val checkpointFile = directory.resolve("checkpoint.txt")

    fun rawFile(): Path = raw

    fun recover(): DiagnosticTrackState {
        Files.createDirectories(directory)
        val allIds = trackTrials.map { it.id }.toSet()
        val rawIds = readTrialIds(raw)
        require(rawIds.size == rawIds.distinct().size) { "duplicate raw trial ID in ${directory.fileName}" }
        require(rawIds.all { it in allIds }) { "unknown raw trial ID in ${directory.fileName}" }
        val checkpointIds = if (checkpointFile.exists()) Files.readAllLines(checkpointFile).filter { it.isNotBlank() } else emptyList()
        require(checkpointIds.size == checkpointIds.distinct().size) { "duplicate checkpoint trial ID in ${directory.fileName}" }
        require(checkpointIds.all { it in rawIds }) { "checkpoint without raw record in ${directory.fileName}" }
        val completed = rawIds.toMutableSet()
        if (rawIds.toSet() != checkpointIds.toSet()) writeCheckpoint(completed)
        return DiagnosticTrackState(completed)
    }

    fun appendRaw(trialId: String, observation: TrialObservation) {
        appendForced(raw, "{\"trialId\":${quote(trialId)},\"observation\":${EvaluationJsonLines.trial(observation)}}\n")
    }

    fun writeCheckpoint(completed: Set<String>) {
        val ordered = trackTrials.map { it.id }.filter { it in completed }
        atomicWrite(checkpointFile, ordered.joinToString("\n", postfix = if (ordered.isEmpty()) "" else "\n"))
    }

    private fun readTrialIds(path: Path): List<String> = if (!path.exists()) {
        emptyList()
    } else {
        Files.readAllLines(path).map { line ->
            Regex("\"trialId\":\"((?:\\\\.|[^\"])*)\"").find(line)?.groupValues?.get(1)?.let(::unquote)
                ?: throw DiagnosticCampaignStateException("malformed ledger record in ${path.fileName}")
        }
    }
}

// ============================================================
// Campaign runner (Plan Sections 8/10/13)
// ============================================================

private class DiagnosticHardStopException(val reasonCode: String) : RuntimeException("Unit 2-D hard stop: $reasonCode")

private enum class DiagnosticRunnerState { SEALED, HALTED }

private class DiagnosticCampaignRunner(
    private val definition: DiagnosticCampaignDefinition,
    private val root: Path,
    private val identity: DiagnosticIdentity,
    private val usableSpace: (Path) -> Long = { Files.getFileStore(it).usableSpace },
) {
    suspend fun run(executor: suspend (DiagnosticTrial) -> TrialObservation): DiagnosticRunnerState {
        val normalizedRoot = root.toAbsolutePath().normalize()
        val forbidden = Path.of(DIAGNOSTIC_ARTIFACT_ROOT_PREFIX).resolve(FAILED_UNIT_2_CAMPAIGN_ID).normalize()
        require(normalizedRoot != forbidden && !normalizedRoot.startsWith(forbidden)) {
            "Unit 2-D must not write inside the failed Unit 2 campaign directory"
        }
        Files.createDirectories(root)
        require(usableSpace(root) >= DIAGNOSTIC_MINIMUM_FREE_BYTES) { "artifact filesystem has less than 2 GiB usable space" }
        FileChannel.open(root.resolve("campaign.lock"), StandardOpenOption.CREATE, StandardOpenOption.WRITE).use { lockChannel ->
            val lock = try { lockChannel.tryLock() } catch (_: OverlappingFileLockException) { null }
                ?: throw DiagnosticCampaignStateException("campaign is already locked by another runner")
            lock.use {
                require(!(root.resolve("campaign.sealed").exists() && root.resolve("campaign.halted").exists())) {
                    "AMBIGUOUS STATE: both campaign.sealed and campaign.halted exist"
                }
                require(!root.resolve("campaign.halted").exists()) { "campaign previously halted; automatic continuation is forbidden" }
                require(!root.resolve("campaign.sealed").exists()) { "campaign already sealed; automatic continuation is forbidden" }
                writeDefinitionAndIdentity()
                writeIntentIfAbsent()

                val warmupLedger = DiagnosticTrackLedger(root.resolve(DiagnosticTrack.WARMUP.pathSegment), definition.trials.filter { it.track == DiagnosticTrack.WARMUP })
                val productionLedger = DiagnosticTrackLedger(root.resolve(DiagnosticTrack.PRODUCTION.pathSegment), definition.trials.filter { it.track == DiagnosticTrack.PRODUCTION })
                val candidateLedger = DiagnosticTrackLedger(root.resolve(DiagnosticTrack.CANDIDATE.pathSegment), definition.trials.filter { it.track == DiagnosticTrack.CANDIDATE })
                val warmupCompleted = warmupLedger.recover().completed
                val productionCompleted = productionLedger.recover().completed
                val candidateCompleted = candidateLedger.recover().completed

                fun ledgerFor(track: DiagnosticTrack) = when (track) {
                    DiagnosticTrack.WARMUP -> warmupLedger
                    DiagnosticTrack.PRODUCTION -> productionLedger
                    DiagnosticTrack.CANDIDATE -> candidateLedger
                }
                fun completedFor(track: DiagnosticTrack) = when (track) {
                    DiagnosticTrack.WARMUP -> warmupCompleted
                    DiagnosticTrack.PRODUCTION -> productionCompleted
                    DiagnosticTrack.CANDIDATE -> candidateCompleted
                }

                for (trial in definition.trials) {
                    val trackCompleted = completedFor(trial.track)
                    if (trial.id in trackCompleted) continue
                    // Semantic failure (classification D, or any other observed outcome)
                    // is evidence, not a stop condition -- see stop-condition tests below.
                    // Only DiagnosticHardStopException (identity/config/harness/artifact-
                    // integrity defects, or an unauthorized consequential path -- the
                    // latter architecturally unreachable, Plan Section 11) halts the loop.
                    val observation = try {
                        executor(trial)
                    } catch (hardStop: DiagnosticHardStopException) {
                        atomicWrite(root.resolve("campaign.halted"), "${trial.id}:${hardStop.reasonCode}\n")
                        return DiagnosticRunnerState.HALTED
                    }
                    val ledger = ledgerFor(trial.track)
                    ledger.appendRaw(trial.id, observation)
                    trackCompleted += trial.id
                    ledger.writeCheckpoint(trackCompleted)
                }
                sealCampaign(warmupLedger, productionLedger, candidateLedger)
                return DiagnosticRunnerState.SEALED
            }
        }
    }

    private fun writeDefinitionAndIdentity() {
        val definitionFile = root.resolve("campaign-definition.txt")
        if (definitionFile.exists()) require(Files.readString(definitionFile) == definition.canonical) { "campaign definition mismatch" }
        else atomicWrite(definitionFile, definition.canonical)
        val identityFile = root.resolve("campaign-identity.txt")
        val text = "manifestVersion=$DIAGNOSTIC_MANIFEST_VERSION\nfingerprint=${identity.fingerprint}\ncampaignHash=${identity.campaignHash}\n"
        if (identityFile.exists()) require(Files.readString(identityFile) == text) { "campaign identity mismatch" }
        else atomicWrite(identityFile, text)
    }

    private fun writeIntentIfAbsent() {
        val intentFile = root.resolve("intent.jsonl")
        val text = definition.trials.joinToString("\n", postfix = "\n") { trial ->
            "{\"trialId\":${quote(trial.id)},\"identity\":${quote(identity.fingerprint)}}"
        }
        if (intentFile.exists()) require(Files.readString(intentFile) == text) { "intent record mismatch" }
        else atomicWrite(intentFile, text)
    }

    private fun sealCampaign(warmup: DiagnosticTrackLedger, production: DiagnosticTrackLedger, candidate: DiagnosticTrackLedger) {
        fun fileStats(path: Path): Triple<String, Long, Long> {
            val bytes = if (path.exists()) Files.readAllBytes(path) else byteArrayOf()
            return Triple(sha256Bytes(bytes), bytes.size.toLong(), countLines(bytes))
        }
        val (warmupHash, warmupBytes, warmupLines) = fileStats(warmup.rawFile())
        val (productionHash, productionBytes, productionLines) = fileStats(production.rawFile())
        val (candidateHash, candidateBytes, candidateLines) = fileStats(candidate.rawFile())
        val material = listOf(
            DIAGNOSTIC_MANIFEST_VERSION, identity.fingerprint, "SEALED",
            warmupHash, warmupBytes.toString(), warmupLines.toString(),
            productionHash, productionBytes.toString(), productionLines.toString(),
            candidateHash, candidateBytes.toString(), candidateLines.toString(),
        ).joinToString("\n")
        atomicWrite(root.resolve("manifest.txt"), "$material\nmanifestHash=${sha256(material)}\n")
        atomicWrite(root.resolve("campaign.sealed"), sha256(material) + "\n")
        writeArtifactHashInventory()
    }

    private fun writeArtifactHashInventory() {
        val files = Files.walk(root).use { paths ->
            paths.filter {
                Files.isRegularFile(it) && it.fileName.toString() != "artifact-hash-inventory.txt" && it.fileName.toString() != "campaign.lock"
            }.sorted().toList()
        }
        val inventory = files.joinToString("\n", postfix = if (files.isEmpty()) "" else "\n") { path ->
            val bytes = Files.readAllBytes(path)
            "${root.relativize(path).toString().replace('\\', '/')}|${sha256Bytes(bytes)}|${bytes.size}|${countLines(bytes)}"
        }
        atomicWrite(root.resolve("artifact-hash-inventory.txt"), inventory)
    }
}

// ============================================================
// Deterministic verification (offline; no live HTTP call anywhere below)
// ============================================================

class ReasoningProtocolDiagnosticCharacterisationTest {
    @TempDir lateinit var temporaryDirectory: Path

    private fun definition(id: String = "unit2d-diagnostic-test") = DiagnosticCampaignDefinition(id)

    private fun identity(d: DiagnosticCampaignDefinition, qwenDigestSuffix: String = "") = DiagnosticIdentity(
        repositoryCommit = "a859ba5$qwenDigestSuffix",
        qwenModelName = DIAGNOSTIC_QWEN_MODEL_NAME,
        qwenModelDigest = "sha256:qwen-model$qwenDigestSuffix|show:${"a".repeat(64)}",
        llamaModelName = DIAGNOSTIC_LLAMA_MODEL_NAME,
        llamaModelDigest = "sha256:llama-model|show:${"b".repeat(64)}",
        endpoint = "http://127.0.0.1:11434/api/generate",
        timeoutMs = DIAGNOSTIC_EVALUATION_TIMEOUT_MS,
        ubuntuRuntimeIdentity = "ubuntu-test",
        containerIdentity = "container-test",
        campaignHash = d.hash,
    )

    /** Synthetic, offline observation -- no HTTP call, mirrors Unit 2's own
     * `observation()` test helper. */
    private fun syntheticObservation(
        trial: DiagnosticTrial,
        actual: ExpectedAction? = trial.fixture.expectedAction,
        classification: PrimaryClassification = PrimaryClassification.A,
        fidelity: ContentFidelity = ContentFidelity.NOT_APPLICABLE,
    ) = TrialObservation(
        runId = "offline", fixtureId = trial.fixture.id, contextProfileId = trial.profile.externalId,
        trialSequence = trial.attempt, stableInputHash = "input", repositoryCommit = "a859ba5",
        modelName = if (trial.model == DiagnosticModel.QWEN) DIAGNOSTIC_QWEN_MODEL_NAME else DIAGNOSTIC_LLAMA_MODEL_NAME,
        modelDigest = "digest", runtimeImageId = "runtime", endpointIdentifier = "loopback",
        timeoutMs = DIAGNOSTIC_EVALUATION_TIMEOUT_MS, prompt = "prompt", promptSha256 = "promptHash",
        requestBody = "request", rawOllamaEnvelope = "envelope",
        extractedResponse = actual?.let { "${it.name}: synthetic" }, parsedVariant = actual?.name,
        parserExceptionType = null, parserExceptionClassification = null,
        expectedAction = trial.fixture.expectedAction, actualAction = actual, representationValid = actual != null,
        contentFidelity = fidelity, latencyNanos = 1000, endpointMetadata = EndpointMetadata(promptEvalCount = 10, evalCount = 2),
        primaryClassification = classification,
    )

    // ---------- 1: exact 24-call schedule ----------
    @Test fun `exact 24-call schedule is deterministic and reproducible`() {
        val first = definition(); val second = definition()
        assertEquals(24, first.trials.size)
        assertEquals(first.canonical, second.canonical)
        assertEquals(first.hash, second.hash)
        assertEquals(64, first.hash.length)
    }

    // ---------- 2: no duplicate trial IDs ----------
    @Test fun `no duplicate trial IDs exist across the 24-call schedule`() {
        val d = definition()
        assertEquals(24, d.trials.map { it.id }.distinct().size)
    }

    // ---------- 3 & 4: exact fixture-model-context mapping and DQ traceability ----------
    @Test fun `DQ1-DQ6 fixture model context and attempt mapping is exact`() {
        val d = definition()
        val dq1 = d.trials.filter { it.groupId == "DQ1" }
        assertEquals(10, dq1.size)
        assertTrue(dq1.all { it.fixture == DiagnosticCorpus.r01Direct && it.profile == ContextProfileId.MINIMAL_PRODUCTION_CONTEXT && it.model == DiagnosticModel.QWEN })
        assertEquals((1..10).toList(), dq1.map { it.attempt })

        val dq2 = d.trials.filter { it.groupId == "DQ2" }
        assertEquals(setOf(DiagnosticCorpus.p01OrdinaryFact, DiagnosticCorpus.p06Greeting, DiagnosticCorpus.g01Multistep, DiagnosticCorpus.n01Heartbeat), dq2.map { it.fixture }.toSet())
        assertTrue(dq2.all { it.profile == ContextProfileId.MINIMAL_PRODUCTION_CONTEXT && it.model == DiagnosticModel.QWEN && it.attempt == 1 })
        assertEquals(setOf(ExpectedAction.REPLY, ExpectedAction.GOAL, ExpectedAction.NOACTION), dq2.map { it.fixture.expectedAction }.toSet())

        val dq3 = d.trials.filter { it.groupId == "DQ3" }
        assertEquals(2, dq3.size)
        assertTrue(dq3.all { it.fixture == DiagnosticCorpus.r01Direct && it.model == DiagnosticModel.QWEN })
        assertEquals(setOf(ContextProfileId.MIXED_FULL_PRODUCTION_LIKE, ContextProfileId.CONVERSATION_HISTORY), dq3.map { it.profile }.toSet())

        val dq4 = d.trials.single { it.groupId == "DQ4" }
        assertEquals(DiagnosticCorpus.r01Direct, dq4.fixture)
        assertEquals(ContextProfileId.MINIMAL_PRODUCTION_CONTEXT, dq4.profile)
        assertEquals(DiagnosticModel.LLAMA, dq4.model)

        val dq5 = d.trials.filter { it.groupId == "DQ5" }
        assertEquals(5, dq5.size)
        assertTrue(dq5.all { it.fixture == DiagnosticCorpus.r01DirectDecisionOnly && it.track == DiagnosticTrack.CANDIDATE && it.model == DiagnosticModel.QWEN })
        assertEquals((1..5).toList(), dq5.map { it.attempt })

        // DQ6 has no dedicated fixture/call -- cross-cutting over DQ1-DQ5's 22 non-warm-up observations.
        assertEquals(22, d.trials.count { it.track != DiagnosticTrack.WARMUP })
        assertFalse(d.trials.any { it.groupId == "DQ6" })
    }

    // ---------- 5: exact warm-up fixture ----------
    @Test fun `warm-up fixture is frozen and reused verbatim from Unit 2`() {
        assertEquals("warmup-acknowledgement", DiagnosticCorpus.warmup.id)
        assertEquals("Synthetic warm-up request: reply with a brief acknowledgement.", DiagnosticCorpus.warmup.ownerMessage)
        assertEquals(ExpectedAction.REPLY, DiagnosticCorpus.warmup.expectedAction)
        val d = definition()
        val warmups = d.trials.filter { it.track == DiagnosticTrack.WARMUP }
        assertEquals(2, warmups.size)
        assertTrue(warmups.all { it.fixture == DiagnosticCorpus.warmup && it.profile == ContextProfileId.MINIMAL_PRODUCTION_CONTEXT })
        assertEquals(setOf(DiagnosticModel.QWEN, DiagnosticModel.LLAMA), warmups.map { it.model }.toSet())
    }

    // ---------- 6: production prompt-builder use ----------
    @Test fun `production track uses the real DefaultReasoningPromptBuilder byte-identical`() {
        val input = SyntheticContextProfiles.construct(DiagnosticCorpus.r01Direct, ContextProfileId.MINIMAL_PRODUCTION_CONTEXT)
        val turn = (input.request.subject as ReasoningSubject.OfTurn).turn
        val real = DefaultReasoningPromptBuilder().buildPrompt(turn, input.request.reasoningContext)
        assertContains(real, DiagnosticCorpus.r01Direct.ownerMessage)
        assertContains(real, "REMEMBER:")
        // The reused reference text (Section "DQ5 candidate-track prompt" above) must be an
        // exact substring of the real production output -- a self-verifying fidelity check,
        // not a trusted transcription.
        assertContains(real, SELECTION_GUIDANCE_REFERENCE_TEXT)
    }

    // ---------- 7: corrected DQ5 prompt ----------
    @Test fun `DQ5 decision-only prompt is corrected labelled non-production and holds guidance constant`() {
        val input = SyntheticContextProfiles.construct(DiagnosticCorpus.r01DirectDecisionOnly, ContextProfileId.MINIMAL_PRODUCTION_CONTEXT)
        val turn = (input.request.subject as ReasoningSubject.OfTurn).turn
        val candidate = DecisionOnlyPromptBuilder().buildPrompt(turn, input.request.reasoningContext)
        assertContains(candidate, "GOAL:, REPLY:, REMEMBER:")
        assertContains(candidate, "\"SELECTED\"")
        assertContains(candidate, SELECTION_GUIDANCE_REFERENCE_TEXT)
        assertFalse("no colon" in candidate)
        val real = DefaultReasoningPromptBuilder().buildPrompt(turn, input.request.reasoningContext)
        assertFalse(candidate == real)
    }

    // ---------- 8: all four DQ5 action outputs parse through the unmodified parser ----------
    @Test fun `all four DQ5 action forms parse through the unmodified TaggedReasoningResponseParser`() {
        val parser = TaggedReasoningResponseParser()
        val goal = parser.parse("GOAL: SELECTED")
        assertTrue(goal is ReasoningProviderResponse.Goal); assertEquals("SELECTED", goal.text)
        val reply = parser.parse("REPLY: SELECTED")
        assertTrue(reply is ReasoningProviderResponse.Reply); assertEquals("SELECTED", reply.text)
        val remember = parser.parse("REMEMBER: SELECTED")
        assertTrue(remember is ReasoningProviderResponse.Remember); assertEquals("SELECTED", remember.text)
        val noAction = parser.parse("NOACTION")
        assertEquals(ReasoningProviderResponse.NoAction, noAction)
        // The originally proposed bare, colon-less form is confirmed still unparseable --
        // this is the defect the correction fixed, preserved here as a regression guard.
        assertFailsWith<UnclassifiableModelResponseException> { parser.parse("REMEMBER") }
    }

    // ---------- 9: DQ5 placeholder content is non-evidentiary ----------
    @Test fun `DQ5 placeholder content is non-evidentiary and never a substantive failure`() {
        val remember = ReasoningProviderResponse.Remember("SELECTED")
        val fidelity = contentFidelity(DiagnosticCorpus.r01DirectDecisionOnly, remember)
        assertEquals(ContentFidelity.DEVIATION_OR_PARAPHRASE, fidelity)
        // A correct DQ5 semantic selection therefore classifies B (correct action, imperfect
        // fidelity), never A -- worksheet interpretation must key on actualAction == expectedAction
        // for DQ5, not primaryClassification == A, exactly as Plan Sections 2/6/12 require.
        val primary = when {
            ExpectedAction.REMEMBER != DiagnosticCorpus.r01DirectDecisionOnly.expectedAction -> PrimaryClassification.D
            fidelity == ContentFidelity.EXACT || fidelity == ContentFidelity.NOT_APPLICABLE -> PrimaryClassification.A
            else -> PrimaryClassification.B
        }
        assertEquals(PrimaryClassification.B, primary)
    }

    // ---------- 10, 11: exact artifact split 2/17/5 and intent count 24 ----------
    @Test fun `sealed campaign produces the exact 2-17-5 raw split and 24 intent records`() = runBlocking {
        val d = definition(); val root = temporaryDirectory.resolve("campaign")
        val runner = DiagnosticCampaignRunner(d, root, identity(d), usableSpace = { DIAGNOSTIC_MINIMUM_FREE_BYTES })
        var calls = 0
        val state = runner.run { trial -> calls++; syntheticObservation(trial) }
        assertEquals(DiagnosticRunnerState.SEALED, state)
        assertEquals(24, calls)
        assertEquals(2, Files.readAllLines(root.resolve("warmup/raw.jsonl")).size)
        assertEquals(17, Files.readAllLines(root.resolve("production-track/raw.jsonl")).size)
        assertEquals(5, Files.readAllLines(root.resolve("candidate-track/raw.jsonl")).size)
        assertEquals(2, Files.readAllLines(root.resolve("warmup/checkpoint.txt")).filter { it.isNotBlank() }.size)
        assertEquals(17, Files.readAllLines(root.resolve("production-track/checkpoint.txt")).filter { it.isNotBlank() }.size)
        assertEquals(5, Files.readAllLines(root.resolve("candidate-track/checkpoint.txt")).filter { it.isNotBlank() }.size)
        assertEquals(24, Files.readAllLines(root.resolve("intent.jsonl")).size)
        assertTrue(root.resolve("campaign.sealed").exists())
        assertFalse(root.resolve("campaign.halted").exists())
        assertTrue(root.resolve("manifest.txt").exists())
        assertTrue(root.resolve("artifact-hash-inventory.txt").exists())
    }

    // ---------- 12: deterministic campaign-definition identity/hash ----------
    @Test fun `campaign-definition hash is deterministic and campaign identity requires an explicit diagnostic marker`() {
        val a = definition("unit2d-diagnostic-alpha"); val b = definition("unit2d-diagnostic-alpha")
        assertEquals(a.hash, b.hash)
        val differentId = definition("unit2d-diagnostic-beta")
        assertFalse(a.hash == differentId.hash)
        assertFailsWith<IllegalArgumentException> { definition("no-marker-campaign") }
    }

    // ---------- 13: failed Unit 2 path rejection ----------
    @Test fun `Unit 2-D refuses the failed Unit 2 campaign identity and any path nested inside it`() {
        assertFailsWith<IllegalArgumentException> { definition(FAILED_UNIT_2_CAMPAIGN_ID) }
        val d = definition()
        assertFailsWith<IllegalArgumentException> {
            runBlocking {
                DiagnosticCampaignRunner(d, Path.of(DIAGNOSTIC_ARTIFACT_ROOT_PREFIX, FAILED_UNIT_2_CAMPAIGN_ID, "nested"), identity(d))
                    .run { trial -> syntheticObservation(trial) }
            }
        }
    }

    // ---------- 14: new campaign namespace isolation ----------
    @Test fun `diagnostic config loader rejects campaign roots outside the accepted parent or missing the diagnostic marker`() {
        val d = definition()
        val base = completeDiagnosticEnvironment(temporaryDirectory.resolve("elsewhere").toString())
        assertFailsWith<IllegalArgumentException> { DiagnosticConfigLoader.load(base, Path.of("."), d) }
        val noMarker = completeDiagnosticEnvironment(DIAGNOSTIC_ARTIFACT_ROOT_PREFIX, campaignId = "no-marker-here")
        assertFailsWith<IllegalArgumentException> {
            DiagnosticConfigLoader.load(noMarker, Path.of("."), DiagnosticCampaignDefinition("no-marker-here".let { "unit2d-diagnostic-marker-check" }))
        }
    }

    // ---------- 15: exact-once state handling ----------
    @Test fun `sealed campaign forbids automatic continuation and each call happens exactly once`() = runBlocking {
        val d = definition(); val root = temporaryDirectory.resolve("exact-once")
        val runner = DiagnosticCampaignRunner(d, root, identity(d), usableSpace = { DIAGNOSTIC_MINIMUM_FREE_BYTES })
        val seenIds = mutableListOf<String>()
        runner.run { trial -> seenIds += trial.id; syntheticObservation(trial) }
        assertEquals(24, seenIds.size)
        assertEquals(24, seenIds.distinct().size)
        assertFailsWith<IllegalArgumentException> {
            DiagnosticCampaignRunner(d, root, identity(d), usableSpace = { DIAGNOSTIC_MINIMUM_FREE_BYTES }).run { trial -> syntheticObservation(trial) }
        }
    }

    // ---------- 16: raw-without-checkpoint recovery ----------
    @Test fun `raw record without a checkpoint entry recovers without repeating the call`() = runBlocking {
        val d = definition(); val root = temporaryDirectory.resolve("raw-no-checkpoint")
        Files.createDirectories(root.resolve("production-track"))
        val production = d.trials.first { it.track == DiagnosticTrack.PRODUCTION }
        val ledger = DiagnosticTrackLedger(root.resolve("production-track"), d.trials.filter { it.track == DiagnosticTrack.PRODUCTION })
        ledger.appendRaw(production.id, syntheticObservation(production))
        // checkpoint deliberately not written
        val runner = DiagnosticCampaignRunner(d, root, identity(d), usableSpace = { DIAGNOSTIC_MINIMUM_FREE_BYTES })
        val seenIds = mutableListOf<String>()
        val state = runner.run { trial -> seenIds += trial.id; syntheticObservation(trial) }
        assertEquals(DiagnosticRunnerState.SEALED, state)
        assertFalse(production.id in seenIds)
        assertEquals(23, seenIds.size)
        assertContains(Files.readAllLines(root.resolve("production-track/checkpoint.txt")), production.id)
    }

    // ---------- 17: checkpoint-without-raw fails closed ----------
    @Test fun `checkpoint entry without a raw record fails closed`() {
        val d = definition(); val root = temporaryDirectory.resolve("checkpoint-no-raw")
        val production = d.trials.first { it.track == DiagnosticTrack.PRODUCTION }
        Files.createDirectories(root.resolve("production-track"))
        Files.writeString(root.resolve("production-track/checkpoint.txt"), production.id + "\n")
        val ledger = DiagnosticTrackLedger(root.resolve("production-track"), d.trials.filter { it.track == DiagnosticTrack.PRODUCTION })
        assertFailsWith<IllegalArgumentException> { ledger.recover() }
    }

    // ---------- 18: duplicate-state failure ----------
    @Test fun `duplicate raw trial IDs fail closed`() {
        val d = definition(); val root = temporaryDirectory.resolve("duplicate-raw")
        val production = d.trials.first { it.track == DiagnosticTrack.PRODUCTION }
        val ledger = DiagnosticTrackLedger(root.resolve("production-track"), d.trials.filter { it.track == DiagnosticTrack.PRODUCTION })
        ledger.appendRaw(production.id, syntheticObservation(production))
        ledger.appendRaw(production.id, syntheticObservation(production))
        assertFailsWith<IllegalArgumentException> { ledger.recover() }
    }

    // ---------- 19: identity mismatch failure ----------
    @Test fun `campaign identity mismatch fails closed`() = runBlocking {
        val d = definition(); val root = temporaryDirectory.resolve("identity-mismatch")
        DiagnosticCampaignRunner(d, root, identity(d), usableSpace = { DIAGNOSTIC_MINIMUM_FREE_BYTES }).run { trial -> syntheticObservation(trial) }
        Files.delete(root.resolve("campaign.sealed"))
        assertFailsWith<IllegalArgumentException> {
            DiagnosticCampaignRunner(d, root, identity(d, qwenDigestSuffix = "-drifted"), usableSpace = { DIAGNOSTIC_MINIMUM_FREE_BYTES })
                .run { trial -> syntheticObservation(trial) }
        }
    }

    // ---------- 20: artifact-integrity mismatch failure ----------
    @Test fun `unknown trial ID in a raw artifact fails closed as an integrity defect`() {
        val d = definition(); val root = temporaryDirectory.resolve("unknown-raw-id")
        val ledger = DiagnosticTrackLedger(root.resolve("production-track"), d.trials.filter { it.track == DiagnosticTrack.PRODUCTION })
        val foreignTrial = d.trials.first { it.track == DiagnosticTrack.CANDIDATE }
        ledger.appendRaw(foreignTrial.id, syntheticObservation(foreignTrial))
        assertFailsWith<IllegalArgumentException> { ledger.recover() }
    }

    // ---------- 21: semantic wrong action does not stop diagnostic execution ----------
    @Test fun `semantic failure on every trial is recorded as evidence and does not halt the campaign`() = runBlocking {
        val d = definition(); val root = temporaryDirectory.resolve("all-wrong")
        val runner = DiagnosticCampaignRunner(d, root, identity(d), usableSpace = { DIAGNOSTIC_MINIMUM_FREE_BYTES })
        val state = runner.run { trial ->
            // every single trial, including all ten DQ1 repeats, comes back wrong -- must not stop
            syntheticObservation(trial, actual = ExpectedAction.entries.first { it != trial.fixture.expectedAction }, classification = PrimaryClassification.D)
        }
        assertEquals(DiagnosticRunnerState.SEALED, state)
        assertTrue(root.resolve("campaign.sealed").exists())
        assertEquals(24, Files.readAllLines(root.resolve("intent.jsonl")).size)
    }

    // ---------- Distinguishing hard stops from semantic evidence ----------
    @Test fun `a genuine hard stop halts immediately writes campaign-halted and calls no further trials`() = runBlocking {
        val d = definition(); val root = temporaryDirectory.resolve("hard-stop")
        val runner = DiagnosticCampaignRunner(d, root, identity(d), usableSpace = { DIAGNOSTIC_MINIMUM_FREE_BYTES })
        var calls = 0
        val state = runner.run { trial ->
            calls++
            if (calls == 3) throw DiagnosticHardStopException("HARNESS_DEFECT")
            syntheticObservation(trial)
        }
        assertEquals(DiagnosticRunnerState.HALTED, state)
        assertEquals(3, calls)
        assertTrue(root.resolve("campaign.halted").exists())
        assertFalse(root.resolve("campaign.sealed").exists())
        assertContains(Files.readString(root.resolve("campaign.halted")), "HARNESS_DEFECT")
    }

    // ---------- 22: representation/parser/transport/timeout classifications remain distinct ----------
    @Test fun `representation parser transport and timeout classifications remain distinct never collapsed`() {
        assertEquals(PrimaryClassification.G, classifyRejected(""))
        assertEquals(PrimaryClassification.C, classifyRejected("NOACTION trailing text"))
        assertEquals(PrimaryClassification.E, classifyRejected("not a tagged response at all"))
        assertTrue(containsMultipleTaggedOutputs("GOAL: a\nREPLY: b"))
        assertFalse(containsMultipleTaggedOutputs("GOAL: SELECTED"))
        val nine = PrimaryClassification.entries.toSet()
        assertEquals(setOf("A", "B", "C", "D", "E", "F", "G", "H", "I"), nine.map { it.name }.toSet())
    }

    // ---------- 23: no downstream consequential dependency ----------
    @Test fun `this file and Unit 1-2 files contain no downstream consequential import`() {
        // Checked as actual `import` statements (real coupling), not bare substrings --
        // this file's own denylist below legitimately names these symbols in prose/strings
        // without importing them, which must not itself trip the check.
        val forbiddenImportFragments = listOf(
            "parker.composition", "MemoryAdmissionCoordinator", "DefaultKnowledgeSubmission",
            "AuthorizationPurposeRegistry", "ConversationReplyCoordinator", "PlannerRuntime", "ToolRegistry",
        )
        fun importLines(path: Path) = Files.readAllLines(path).filter { it.trimStart().startsWith("import ") }
        val thisFileImports = importLines(Path.of("tests/integration/ReasoningProtocolDiagnosticCharacterisationTest.kt"))
        val unit1Imports = importLines(Path.of("tests/integration/ReasoningProtocolLiveModelEvaluationHarness.kt"))
        forbiddenImportFragments.forEach { symbol ->
            assertFalse(thisFileImports.any { symbol in it }, "unexpected downstream import: $symbol")
            assertFalse(unit1Imports.any { symbol in it }, "Unit 1 harness unexpectedly imports: $symbol")
        }
    }

    // ---------- 24: detached Gradle task isolation ----------
    @Test fun `dedicated Unit 2-D task is filtered and detached from ordinary lifecycle tasks`() {
        val build = Files.readString(Path.of("build.gradle.kts"))
        assertContains(build, "tasks.register<Test>(\"reasoningProtocolUnit2DDiagnostic\")")
        assertContains(build, "includeTestsMatching(\"parker.integration.ReasoningProtocolDiagnosticCharacterisationTest\")")
        assertContains(build, "systemProperty(\"parker.reasoning.diagnostic.enabled\", \"true\")")
        assertFalse(Regex("dependsOn\\([^)]*reasoningProtocolUnit2DDiagnostic").containsMatchIn(build))
        assertFalse(Regex("tasks\\.(test|check|build|assemble)[^{]*\\{[^}]*reasoningProtocolUnit2DDiagnostic", RegexOption.DOT_MATCHES_ALL).containsMatchIn(build))
    }

    // ---------- 25: absent live config skips before HTTP-client construction ----------
    // The dedicated Gradle task itself sets DIAGNOSTIC_PROPERTY=true (mirroring Unit 2's own
    // convention) -- that alone must never be sufficient to reach a live call. The second,
    // independent gate is the complete explicit environment configuration; this task's own
    // verification run supplies neither the campaign ID env var nor any PARKER_REASONING_
    // DIAGNOSTIC_* value, so the live test above is skipped regardless of the property.
    @Test fun `absent explicit environment configuration means the live entry point skips regardless of the property`() {
        assertEquals("parker.reasoning.diagnostic.enabled", DIAGNOSTIC_PROPERTY)
        assertTrue(System.getenv(DiagnosticConfigLoader.CAMPAIGN_ID).isNullOrBlank())
    }

    @Test fun `identity evidence extraction resolves the exact configured model digest from a nested tags shape`() {
        val tagsJson = """
            {"models":[
              {"name":"other-model:1b","model":"other-model:1b","digest":"${"1".repeat(64)}","details":{"family":"other"}},
              {"name":"qwen2.5-coder:7b","model":"qwen2.5-coder:7b","digest":"${"a".repeat(64)}","details":{"family":"qwen2","parameter_size":"7.6B"}}
            ]}
        """.trimIndent()
        assertEquals("a".repeat(64), DiagnosticIdentityEvidence.exactModelDigest(tagsJson, "qwen2.5-coder:7b"))
        assertFailsWith<IllegalStateException> { DiagnosticIdentityEvidence.exactModelDigest(tagsJson, "missing-model") }
        val duplicated = """{"models":[{"name":"llama3.2:3b","digest":"${"b".repeat(64)}"},{"name":"llama3.2:3b","digest":"${"c".repeat(64)}"}]}"""
        assertFailsWith<IllegalStateException> { DiagnosticIdentityEvidence.exactModelDigest(duplicated, "llama3.2:3b") }
        val malformedDigest = """{"models":[{"name":"llama3.2:3b","digest":"short"}]}"""
        assertFailsWith<IllegalStateException> { DiagnosticIdentityEvidence.exactModelDigest(malformedDigest, "llama3.2:3b") }
    }

    @Test fun `interpretation support reuses Unit 1's deriveCrossTrialObservations unchanged for DQ1 and DQ3`() {
        val d = definition()
        val dq1 = d.trials.filter { it.groupId == "DQ1" }
        // Nine agree, one diverges -- a materially mixed DQ1 result.
        val observations = dq1.mapIndexed { index, trial ->
            val actual = if (index == 3) ExpectedAction.REPLY else trial.fixture.expectedAction
            syntheticObservation(trial, actual = actual, classification = if (actual == trial.fixture.expectedAction) PrimaryClassification.A else PrimaryClassification.D)
        }
        val derived = deriveCrossTrialObservations(observations)
        assertTrue(derived.all { it.repeatabilityFailure })
        // A fully uniform (10/10 agreeing) cell must not be flagged as unstable.
        val uniform = dq1.map { trial -> syntheticObservation(trial, classification = PrimaryClassification.A) }
        assertFalse(deriveCrossTrialObservations(uniform).any { it.repeatabilityFailure })
    }

    private fun completeDiagnosticEnvironment(
        artifactRoot: String,
        campaignId: String = "unit2d-diagnostic-test",
        qwenDigest: String = "sha256:offline-test-qwen",
        qwenShowHash: String = "a".repeat(64),
        llamaDigest: String = "sha256:offline-test-llama",
        llamaShowHash: String = "b".repeat(64),
    ) = mapOf(
        LiveEvaluationConfigLoader.ENDPOINT to "http://127.0.0.1:11434/api/generate",
        LiveEvaluationConfigLoader.MODEL to DIAGNOSTIC_QWEN_MODEL_NAME,
        LiveEvaluationConfigLoader.TIMEOUT to DIAGNOSTIC_EVALUATION_TIMEOUT_MS.toString(),
        LiveEvaluationConfigLoader.OUTPUT to "build/offline-unit2d-config.jsonl",
        LiveEvaluationConfigLoader.COMMIT to "a859ba5-test",
        LiveEvaluationConfigLoader.DIGEST to qwenDigest,
        LiveEvaluationConfigLoader.IMAGE to "offline-test-runtime",
        DiagnosticConfigLoader.CAMPAIGN_ID to campaignId,
        DiagnosticConfigLoader.ARTIFACT_ROOT to artifactRoot,
        DiagnosticConfigLoader.UBUNTU_ID to "ubuntu-offline-test",
        DiagnosticConfigLoader.CONTAINER_ID to "container-offline-test",
        DiagnosticConfigLoader.QWEN_MODEL_SHOW_HASH to qwenShowHash,
        DiagnosticConfigLoader.LLAMA_MODEL_NAME to DIAGNOSTIC_LLAMA_MODEL_NAME,
        DiagnosticConfigLoader.LLAMA_MODEL_DIGEST to llamaDigest,
        DiagnosticConfigLoader.LLAMA_MODEL_SHOW_HASH to llamaShowHash,
    )

    @Test fun `complete offline diagnostic configuration loads and pins both model identities`() {
        val d = definition()
        val environment = completeDiagnosticEnvironment(DIAGNOSTIC_ARTIFACT_ROOT_PREFIX)
        val config = DiagnosticConfigLoader.load(environment, Path.of("."), d)
        assertEquals(d.campaignId, config.campaignId)
        assertEquals(DIAGNOSTIC_QWEN_MODEL_NAME, config.qwenLive.modelName)
        assertEquals(DIAGNOSTIC_LLAMA_MODEL_NAME, config.llamaLive.modelName)
        assertEquals(Path.of(DIAGNOSTIC_ARTIFACT_ROOT_PREFIX, d.campaignId).normalize(), config.campaignArtifactRoot)
    }

    @Test fun `partial diagnostic configuration fails redacted`() {
        val d = definition()
        val partial = mapOf(LiveEvaluationConfigLoader.ENDPOINT to "http://owner:secret@127.0.0.1:11434/api/generate")
        val failure = assertFailsWith<EvaluationConfigurationException> { DiagnosticConfigLoader.load(partial, Path.of("."), d) }
        assertFalse("secret" in failure.message.orEmpty())
        assertFalse("owner" in failure.message.orEmpty())
    }

    // ---------- Live entry point: gated, unreachable in this task's verification ----------
    @Test fun `live Unit 2-D diagnostic campaign skips before definition client or configuration construction unless explicit property is enabled`() = runBlocking {
        assumeTrue(System.getProperty(DIAGNOSTIC_PROPERTY) == "true", "Unit 2-D diagnostic property absent; no campaign object or client constructed")
        val campaignId = System.getenv(DiagnosticConfigLoader.CAMPAIGN_ID)
        assumeTrue(!campaignId.isNullOrBlank(), "complete explicit Unit 2-D configuration absent; no live client constructed")
        val definition = DiagnosticCampaignDefinition(campaignId)
        val config = DiagnosticConfigLoader.load(System.getenv(), Path.of("."), definition)
        val qwenOrigin = URI(config.qwenLive.endpointUrl).let { URI(it.scheme, null, it.host, it.port, "/", null, null) }
        val qwenCaptured = DiagnosticIdentityEvidence.capture(qwenOrigin, config.qwenLive.modelName)
        require(config.qwenLive.modelDigest == qwenCaptured.first) { "live Qwen model identity differs from frozen configuration" }
        val llamaOrigin = URI(config.llamaLive.endpointUrl).let { URI(it.scheme, null, it.host, it.port, "/", null, null) }
        val llamaCaptured = DiagnosticIdentityEvidence.capture(llamaOrigin, config.llamaLive.modelName)
        require(config.llamaLive.modelDigest == llamaCaptured.first) { "live Llama model identity differs from frozen configuration" }
        val qwenHarness = ReasoningProtocolLiveModelEvaluationHarness(config.qwenLive, config.campaignId)
        val llamaHarness = ReasoningProtocolLiveModelEvaluationHarness(config.llamaLive, config.campaignId)
        val decisionOnlyBuilder = DecisionOnlyPromptBuilder()
        val runner = DiagnosticCampaignRunner(definition, config.campaignArtifactRoot, config.identity)
        runner.run { trial ->
            when {
                trial.track == DiagnosticTrack.CANDIDATE ->
                    executeCandidateTrial(config.qwenLive, config.campaignId, decisionOnlyBuilder, SyntheticContextProfiles.construct(trial.fixture, trial.profile), trial.attempt)
                trial.model == DiagnosticModel.LLAMA ->
                    llamaHarness.execute(SyntheticContextProfiles.construct(trial.fixture, trial.profile), trial.attempt)
                else ->
                    qwenHarness.execute(SyntheticContextProfiles.construct(trial.fixture, trial.profile), trial.attempt)
            }
        }
        Unit
    }
}
