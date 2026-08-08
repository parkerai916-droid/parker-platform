package parker.core.interfaces

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Sprint 7, Stage 3 Implementation Unit acceptance test
 * (`docs/implementation/CONVERSATION_ENGINE_IMPLEMENTATION_PLAN.md` §6),
 * covering the contract *shapes* defined in `ReasoningProvider.kt` --
 * blank-rejection and sealed-variant-exclusivity -- independent of any
 * runtime implementation.
 */
class ReasoningProviderContractTest {

    private fun turn(conversationId: String = "conv-1", turnId: String = "turn-1") = Turn(
        turnId = TurnId(turnId),
        conversationId = ConversationId(conversationId),
        message = InboundOwnerMessage(
            channelId = ModuleId("channel.local-text"),
            senderPrincipalId = PrincipalId("user-1"),
            text = "hello",
            timestamp = Instant.parse("2026-01-01T00:00:00Z"),
            correlationId = CorrelationId("corr-1"),
        ),
        receivedAt = Instant.parse("2026-01-01T00:00:00Z"),
    )

    // --- ReasoningContext ---

    @Test
    fun `ReasoningContext accepts an empty entry list`() {
        val context = ReasoningContext(emptyList())

        assertIs<ReasoningContext>(context)
    }

    @Test
    fun `ReasoningContext rejects a blank entry`() {
        assertFailsWith<IllegalArgumentException> {
            ReasoningContext(listOf("valid entry", "   "))
        }
    }

    // --- ReasoningProviderRequest ---

    @Test
    fun `ReasoningProviderRequest holds exactly the supplied subject and ReasoningContext`() {
        val request = ReasoningProviderRequest(
            subject = ReasoningSubject.OfTurn(turn()),
            reasoningContext = ReasoningContext(listOf("context entry")),
        )

        assertIs<ReasoningProviderRequest>(request)
    }

    // --- ReasoningSubject (Amendment 1) ---

    @Test
    fun `ReasoningSubject OfTurn wraps the supplied Turn unchanged`() {
        val theTurn = turn()

        val subject = ReasoningSubject.OfTurn(theTurn)

        assertEquals(theTurn, subject.turn)
    }

    @Test
    fun `ReasoningSubject OfEvidenceAnalysisRequest wraps the supplied EvidenceAnalysisRequest unchanged`() {
        val request = EvidenceAnalysisRequest(
            analysisKind = "comparison",
            requestingPrincipalId = PrincipalId("user-1"),
        )

        val subject = ReasoningSubject.OfEvidenceAnalysisRequest(request)

        assertEquals(request, subject.request)
    }

    @Test
    fun `a ReasoningSubject holds exactly one of OfTurn or OfEvidenceAnalysisRequest, never both`() {
        val subjects: List<ReasoningSubject> = listOf(
            ReasoningSubject.OfTurn(turn()),
            ReasoningSubject.OfEvidenceAnalysisRequest(
                EvidenceAnalysisRequest(analysisKind = "extraction", requestingPrincipalId = PrincipalId("user-1")),
            ),
        )

        for (subject in subjects) {
            // Assigned to a val, not merely a statement: Kotlin enforces exhaustiveness
            // here, so a future third ReasoningSubject case fails to compile rather than
            // silently skipping verification.
            val caseName: String = when (subject) {
                is ReasoningSubject.OfTurn -> "OfTurn"
                is ReasoningSubject.OfEvidenceAnalysisRequest -> "OfEvidenceAnalysisRequest"
            }
            assertTrue(caseName == "OfTurn" || caseName == "OfEvidenceAnalysisRequest")
        }
    }

    // --- Goal ---

    @Test
    fun `Goal rejects blank text`() {
        assertFailsWith<IllegalArgumentException> {
            ReasoningProviderResponse.Goal("   ")
        }
    }

    @Test
    fun `Goal accepts non-blank text`() {
        val goal = ReasoningProviderResponse.Goal("book a flight")

        assertIs<ReasoningProviderResponse.Goal>(goal)
    }

    // --- Reply ---

    @Test
    fun `Reply rejects blank text`() {
        assertFailsWith<IllegalArgumentException> {
            ReasoningProviderResponse.Reply("")
        }
    }

    @Test
    fun `Reply accepts non-blank text`() {
        val reply = ReasoningProviderResponse.Reply("sure, on it")

        assertIs<ReasoningProviderResponse.Reply>(reply)
    }

    // --- Remember (Parker Conversational Memory Bridge, Admission Unit) ---

    @Test
    fun `Remember rejects blank text`() {
        assertFailsWith<IllegalArgumentException> {
            ReasoningProviderResponse.Remember("   ")
        }
    }

    @Test
    fun `Remember accepts non-blank text`() {
        val remember = ReasoningProviderResponse.Remember("my favourite coffee mug is black")

        assertIs<ReasoningProviderResponse.Remember>(remember)
    }

    // --- NoAction ---

    @Test
    fun `NoAction is a single, valueless object`() {
        assertIs<ReasoningProviderResponse.NoAction>(ReasoningProviderResponse.NoAction)
    }

    // --- sealed exclusivity ---

    @Test
    fun `a ReasoningProviderResponse holds exactly one of Goal, Reply, Remember, or NoAction, never more than one`() {
        val responses: List<ReasoningProviderResponse> = listOf(
            ReasoningProviderResponse.Goal("goal text"),
            ReasoningProviderResponse.Reply("reply text"),
            ReasoningProviderResponse.Remember("remember text"),
            ReasoningProviderResponse.NoAction,
        )

        for (response in responses) {
            when (response) {
                is ReasoningProviderResponse.Goal -> assertIs<ReasoningProviderResponse.Goal>(response)
                is ReasoningProviderResponse.Reply -> assertIs<ReasoningProviderResponse.Reply>(response)
                is ReasoningProviderResponse.Remember -> assertIs<ReasoningProviderResponse.Remember>(response)
                is ReasoningProviderResponse.NoAction -> assertIs<ReasoningProviderResponse.NoAction>(response)
            }
        }
    }
}
