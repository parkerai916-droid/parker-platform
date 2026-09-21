package parker.core.runtime

import parker.core.interfaces.HermesProcessingResult

/** Result of the authenticated prepared-handoff -> Hermes v1 operation. */
sealed interface HermesProcessingV1AgentOutcome {
    data class Accepted(val result: HermesProcessingResult, val resultSubmission: String) : HermesProcessingV1AgentOutcome
    data class Disabled(val reason: String = "Hermes v1 routing is disabled") : HermesProcessingV1AgentOutcome
    data class Unsupported(val reason: String) : HermesProcessingV1AgentOutcome
    data class Failed(val category: String, val detailCode: String, val detail: String) : HermesProcessingV1AgentOutcome
}
