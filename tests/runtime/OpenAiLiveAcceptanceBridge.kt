package parker.core.runtime

import java.nio.file.Files
import java.nio.file.Path
import parker.composition.OpenAiApiCredential
import parker.composition.OpenAiExternalTranscriptionProviderReadinessEvaluator
import parker.composition.OpenAiExternalTranscriptionReadiness
import parker.core.interfaces.ExternalTranscriptionMechanism

/** Test-only friend bridge: keeps every Unit H transport/credential type internal to main. */
object OpenAiLiveAcceptanceBridge {
    /** Bounded test-only credential predicate; the supplied value is never retained or rendered. */
    fun credentialStructurallyReady(value: String?): Boolean =
        OpenAiApiCredential.fromEnvironment(value) != null

    fun preflightProblems(
        environment: Map<String, String>,
        liveEnabled: Boolean,
        repositoryRoot: Path,
        readinessEvaluator: OpenAiExternalTranscriptionProviderReadinessEvaluator =
            OpenAiExternalTranscriptionProviderReadinessEvaluator(),
    ): List<String> = buildList {
        if (!liveEnabled) add("LIVE_OPT_IN_ABSENT")

        val profileValue = environment["PARKER_OPENAI_EXTERNAL_TRANSCRIPTION_PROVIDER_PROFILE_PATH"]
        val profilePath = profileValue?.takeIf { it.isNotBlank() }?.let { runCatching { Path.of(it) }.getOrNull() }
        if (profilePath == null) {
            add("PROFILE_PATH_ABSENT_OR_INVALID")
        } else if (!Files.isRegularFile(profilePath) || !Files.isReadable(profilePath)) {
            add("PROFILE_NOT_READABLE")
        } else {
            when (val readiness = readinessEvaluator.evaluate(true, profilePath.toString())) {
                is OpenAiExternalTranscriptionReadiness.Ready ->
                    if (readiness.profile.modelSelectionRule != "gpt-4.1-mini") add("PROFILE_MODEL_CHANGED")
                else -> add("PROFILE_NOT_READY_${readiness::class.simpleName ?: "UNKNOWN"}")
            }
        }

        if (OpenAiApiCredential.fromEnvironment(environment["PARKER_OPENAI_API_KEY"]) == null) {
            add("CREDENTIAL_ABSENT_OR_INVALID")
        }

        val resultValue = environment["PARKER_EXTERNAL_TRANSCRIPTION_LIVE_RESULT_PATH"]
        val resultPath = resultValue?.takeIf { it.isNotBlank() }?.let { runCatching { Path.of(it) }.getOrNull() }
        if (resultPath == null) {
            add("RESULT_PATH_ABSENT_OR_INVALID")
        } else {
            val absolute = resultPath.toAbsolutePath().normalize()
            val repository = repositoryRoot.toAbsolutePath().normalize()
            if (absolute.startsWith(repository)) add("RESULT_PATH_INSIDE_REPOSITORY")
            if (!Files.isRegularFile(absolute) || !Files.isWritable(absolute)) add("RESULT_PATH_NOT_WRITABLE_FILE")
        }
    }

    data class State(
        var calls: Int = 0,
        var storeFalse: Boolean = false,
        var approvedEndpoint: Boolean = false,
        var failureFingerprint: String? = null,
        var providerRejectionFingerprint: String? = null,
        var responseFailureFingerprint: String? = null,
    )
    data class Handle(val mechanism: ExternalTranscriptionMechanism, val state: State)

    fun create(
        readiness: OpenAiExternalTranscriptionReadiness.Ready,
        onEgress: () -> Unit,
    ): Handle {
        val credential = OpenAiApiCredential.fromEnvironment(System.getenv("PARKER_OPENAI_API_KEY"))
            ?: error("credential is absent")
        val state = State()
        val delegate = JdkOpenAiResponsesTransport()
        val transport = OpenAiResponsesTransport { request ->
            check(state.calls == 0) { "Unit N forbids retry or a second request" }
            state.calls += 1
            onEgress()
            state.approvedEndpoint = request.endpoint.toString() == "https://api.openai.com/v1/responses"
            state.storeFalse = request.body.contains("\"store\":false") && !request.body.contains("\"store\":true")
            check(state.approvedEndpoint) { "unapproved endpoint" }
            check(state.storeFalse) { "store=false missing" }
            listOf("\"tools\"", "web_search", "file_search", "mcp", "previous_response_id")
                .forEach { check(!request.body.contains(it, ignoreCase = true)) { "forbidden request capability" } }
            delegate.execute(request)
        }
        return Handle(
            OpenAiResponsesExternalTranscriptionAdapter(
                readiness, credential, transport,
                transportFailureObserver = { state.failureFingerprint = it.render() },
                providerRejectionObserver = { state.providerRejectionFingerprint = it.render() },
                responseFailureObserver = { state.responseFailureFingerprint = it.render() },
            ),
            state,
        )
    }
}
