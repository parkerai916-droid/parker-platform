package parker.core.runtime

import parker.core.interfaces.PrincipalId
import parker.core.interfaces.ReasoningProvider
import parker.core.interfaces.ReasoningProviderRequest
import parker.core.interfaces.ReasoningProviderResponse
import parker.core.interfaces.ReasoningSubject

/**
 * Owner-bound deterministic intent decorator. It establishes only the existing
 * Remember intent and has no admission, Memory Core, knowledge, or durability
 * dependency. Every request outside the governed owner/turn/directive
 * intersection is delegated exactly once and returned unchanged.
 */
class ExplicitOwnerPersistenceDirectiveReasoningProvider(
    private val ownerPrincipalId: PrincipalId,
    private val classifier: ExplicitOwnerPersistenceDirectiveClassifier,
    private val delegate: ReasoningProvider,
) : ReasoningProvider {

    override suspend fun reason(request: ReasoningProviderRequest): ReasoningProviderResponse {
        val turn = (request.subject as? ReasoningSubject.OfTurn)?.turn
            ?: return delegate.reason(request)

        if (turn.message.senderPrincipalId != ownerPrincipalId) {
            return delegate.reason(request)
        }

        val directive = classifier.classify(turn.message.text)
            ?: return delegate.reason(request)

        return ReasoningProviderResponse.Remember(directive.proposition)
    }
}
