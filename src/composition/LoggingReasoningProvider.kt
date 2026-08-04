package parker.composition

import parker.core.interfaces.ReasoningProvider
import parker.core.interfaces.ReasoningProviderRequest
import parker.core.interfaces.ReasoningProviderResponse
import parker.core.interfaces.ReasoningSubject

/**
 * Sprint 10, Unit 4 (Production Composition Root). A transparent,
 * delegating [ReasoningProvider] decorator that logs "Reasoning completed"
 * once [ReasoningProvider.reason] returns -- [ReasoningProvider] is an
 * interface (`REASONING_PROVIDER_CONTRACT_DESIGN.md`), so this composition
 * root wraps it via delegation exactly as [LoggingCommunicationIntake]
 * wraps `CommunicationIntake`, without modifying `ModelReasoningProvider`.
 *
 * Logs the reasoning response's own variant name (`Reply`/`Goal`/`NoAction`)
 * and an identifier drawn from [ReasoningProviderRequest.subject] only --
 * never `ReasoningProviderResponse.Reply.text` or
 * `ReasoningProviderResponse.Goal.text` (see `ParkerLogger`'s own
 * logging-discipline KDoc). **Amendment 1 (ReasoningSubject).** For
 * [ReasoningSubject.OfTurn], the identifier logged is the wrapped
 * [parker.core.interfaces.Turn]'s own `correlationId`, unchanged from
 * before this amendment; for
 * [ReasoningSubject.OfEvidenceAnalysisRequest], no `correlationId`
 * exists, so the wrapped `EvidenceAnalysisRequest`'s own
 * `requestingPrincipalId` is logged instead -- an identifier, never
 * evidentiary content. Does not log on a thrown exception -- `reason()`'s
 * own KDoc documents it "may still fault -- throw -- for genuine
 * implementation-level failures"; such an exception propagates unchanged
 * through this decorator (no `try`/`catch` exists here), reaching
 * `ParkerRuntime.submitOwnerMessage`'s own single, authorised
 * failure-handling boundary rather than being logged (and potentially
 * misclassified) twice.
 */
class LoggingReasoningProvider(
    private val delegate: ReasoningProvider,
    private val logger: ParkerLogger,
) : ReasoningProvider {

    override suspend fun reason(request: ReasoningProviderRequest): ReasoningProviderResponse {
        val response = delegate.reason(request)
        val variant = when (response) {
            is ReasoningProviderResponse.Reply -> "Reply"
            is ReasoningProviderResponse.Goal -> "Goal"
            ReasoningProviderResponse.NoAction -> "NoAction"
        }
        val subjectIdentifier = when (val subject = request.subject) {
            is ReasoningSubject.OfTurn -> "correlationId=${subject.turn.message.correlationId.value}"
            is ReasoningSubject.OfEvidenceAnalysisRequest ->
                "requestingPrincipalId=${subject.request.requestingPrincipalId.value}"
        }
        logger.info("Reasoning completed ($subjectIdentifier, outcome=$variant)")
        return response
    }
}
