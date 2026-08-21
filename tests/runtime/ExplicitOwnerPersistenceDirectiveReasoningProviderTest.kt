package parker.core.runtime

import java.time.Instant
import kotlinx.coroutines.test.runTest
import parker.core.interfaces.ConversationId
import parker.core.interfaces.CorrelationId
import parker.core.interfaces.EvidenceAnalysisRequest
import parker.core.interfaces.InboundOwnerMessage
import parker.core.interfaces.ModuleId
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.ReasoningContext
import parker.core.interfaces.ReasoningProvider
import parker.core.interfaces.ReasoningProviderRequest
import parker.core.interfaces.ReasoningProviderResponse
import parker.core.interfaces.ReasoningSubject
import parker.core.interfaces.Turn
import parker.core.interfaces.TurnId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class ExplicitOwnerPersistenceDirectiveReasoningProviderTest {
    private val owner = PrincipalId("owner-1")

    private fun request(text: String, principalId: PrincipalId = owner) =
        ReasoningProviderRequest(
            subject = ReasoningSubject.OfTurn(
                Turn(
                    turnId = TurnId("turn-1"),
                    conversationId = ConversationId("conversation-1"),
                    message = InboundOwnerMessage(
                        channelId = ModuleId("channel-1"),
                        senderPrincipalId = principalId,
                        text = text,
                        timestamp = Instant.parse("2026-01-01T00:00:00Z"),
                        correlationId = CorrelationId("correlation-1"),
                    ),
                    receivedAt = Instant.parse("2026-01-01T00:00:01Z"),
                ),
            ),
            reasoningContext = ReasoningContext(emptyList()),
        )

    private class CountingProvider(
        private val result: ReasoningProviderResponse,
    ) : ReasoningProvider {
        var calls = 0
        var lastRequest: ReasoningProviderRequest? = null

        override suspend fun reason(request: ReasoningProviderRequest): ReasoningProviderResponse {
            calls += 1
            lastRequest = request
            return result
        }
    }

    @Test
    fun ownerDirectiveReturnsExistingRememberAndModelCannotDowngradeIt() = runTest {
        val delegate = CountingProvider(ReasoningProviderResponse.Reply("downgraded"))
        val provider = ExplicitOwnerPersistenceDirectiveReasoningProvider(
            owner,
            DefaultExplicitOwnerPersistenceDirectiveClassifier(),
            delegate,
        )

        val result = provider.reason(request("Remember the test lighthouse is painted orange."))

        assertEquals(
            ReasoningProviderResponse.Remember("the test lighthouse is painted orange"),
            result,
        )
        assertEquals(0, delegate.calls)
    }

    @Test
    fun nonOwnerDelegatesExactlyOnceAndPreservesTheExactResult() = runTest {
        val delegated = ReasoningProviderResponse.Goal("inspect the lighthouse")
        val delegate = CountingProvider(delegated)
        val provider = ExplicitOwnerPersistenceDirectiveReasoningProvider(
            owner,
            DefaultExplicitOwnerPersistenceDirectiveClassifier(),
            delegate,
        )
        val request = request(
            "Remember the test lighthouse is painted orange.",
            PrincipalId("someone-else"),
        )

        val result = provider.reason(request)

        assertSame(delegated, result)
        assertEquals(1, delegate.calls)
        assertSame(request, delegate.lastRequest)
    }

    @Test
    fun ambiguousOwnerInputDelegatesExactlyOnceAndPreservesReply() = runTest {
        val delegated = ReasoningProviderResponse.Reply("yes")
        val delegate = CountingProvider(delegated)
        val provider = ExplicitOwnerPersistenceDirectiveReasoningProvider(
            owner,
            DefaultExplicitOwnerPersistenceDirectiveClassifier(),
            delegate,
        )

        val result = provider.reason(request("Remember when the lighthouse was orange?"))

        assertSame(delegated, result)
        assertEquals(1, delegate.calls)
    }

    @Test
    fun nonTurnSubjectDelegatesExactlyOnce() = runTest {
        val delegated = ReasoningProviderResponse.NoAction
        val delegate = CountingProvider(delegated)
        val provider = ExplicitOwnerPersistenceDirectiveReasoningProvider(
            owner,
            DefaultExplicitOwnerPersistenceDirectiveClassifier(),
            delegate,
        )
        val request = ReasoningProviderRequest(
            ReasoningSubject.OfEvidenceAnalysisRequest(
                EvidenceAnalysisRequest("comparison", owner),
            ),
            ReasoningContext(emptyList()),
        )

        assertSame(delegated, provider.reason(request))
        assertEquals(1, delegate.calls)
    }

    @Test
    fun classifierExceptionPropagatesWithoutCallingDelegate() = runTest {
        val delegate = CountingProvider(ReasoningProviderResponse.NoAction)
        val provider = ExplicitOwnerPersistenceDirectiveReasoningProvider(
            owner,
            ExplicitOwnerPersistenceDirectiveClassifier { throw IllegalStateException("bad grammar") },
            delegate,
        )

        assertFailsWith<IllegalStateException> { provider.reason(request("Remember X")) }
        assertEquals(0, delegate.calls)
    }
}
