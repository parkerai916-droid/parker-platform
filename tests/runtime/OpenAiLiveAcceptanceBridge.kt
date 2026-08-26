package parker.core.runtime

import parker.composition.OpenAiApiCredential
import parker.composition.OpenAiExternalTranscriptionReadiness
import parker.core.interfaces.ExternalTranscriptionMechanism

/** Test-only friend bridge: keeps every Unit H transport/credential type internal to main. */
object OpenAiLiveAcceptanceBridge {
    data class State(var calls: Int = 0, var storeFalse: Boolean = false, var approvedEndpoint: Boolean = false)
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
        return Handle(OpenAiResponsesExternalTranscriptionAdapter(readiness, credential, transport), state)
    }
}
