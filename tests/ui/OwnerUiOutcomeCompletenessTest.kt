package parker.ui

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@OptIn(ExperimentalCoroutinesApi::class)
class OwnerUiOutcomeCompletenessTest {

    @Test
    fun `sequential submissions preserve transcript order after terminal cleanup`() = runTest {
        val interaction = OfflineOwnerInteraction(
            listOf(
                OfflineOwnerScenario.NotAccepted("Parker did not accept this message."),
                OfflineOwnerScenario.Planned("Completed"),
            ),
        )
        val controller = OwnerUiController(interaction, coroutineContext)

        assertEquals(OwnerSubmissionAttempt.STARTED, controller.submit("first"))
        runCurrent()
        assertEquals(OwnerUiStatus.READY, controller.state.value.status)

        assertEquals(OwnerSubmissionAttempt.STARTED, controller.submit("second"))
        runCurrent()

        assertEquals(listOf("first", "second"), interaction.submissions)
        assertEquals(
            listOf(
                OwnerTranscriptEntry.Owner("first"),
                OwnerTranscriptEntry.System("Not accepted: Parker did not accept this message."),
                OwnerTranscriptEntry.Owner("second"),
                OwnerTranscriptEntry.System("Planned: Completed"),
            ),
            controller.state.value.transcript,
        )
        assertEquals(OwnerUiStatus.READY, controller.state.value.status)
        assertFalse(controller.state.value.submissionActive)
    }

    @Test
    fun `a new submission clears Error through Processing and recovers to Ready`() = runTest {
        val controller = OwnerUiController(
            OfflineOwnerInteraction(
                listOf(
                    OfflineOwnerScenario.Failed(
                        "REASONING",
                        "Parker could not complete reasoning for this message.",
                    ),
                    OfflineOwnerScenario.Delivered(),
                ),
            ),
            coroutineContext,
        )

        controller.submit("first")
        runCurrent()
        assertEquals(OwnerUiStatus.ERROR, controller.state.value.status)

        assertEquals(OwnerSubmissionAttempt.STARTED, controller.submit("retry"))
        assertEquals(OwnerUiStatus.PROCESSING, controller.state.value.status)
        runCurrent()

        assertEquals(OwnerUiStatus.READY, controller.state.value.status)
        assertFalse(controller.state.value.submissionActive)
        assertEquals(
            listOf(
                OwnerTranscriptEntry.Owner("first"),
                OwnerTranscriptEntry.System(
                    "Failed (REASONING): Parker could not complete reasoning for this message.",
                ),
                OwnerTranscriptEntry.Owner("retry"),
            ),
            controller.state.value.transcript,
        )
    }

    @Test
    fun `initially stopped interaction rejects without transcript mutation`() = runTest {
        val interaction = OfflineOwnerInteraction(emptyList(), initiallyAvailable = false)
        val controller = OwnerUiController(interaction, coroutineContext)

        assertEquals(OwnerUiStatus.STOPPED, controller.state.value.status)
        assertEquals(OwnerSubmissionAttempt.UNAVAILABLE, controller.submit("hello"))
        assertEquals(emptyList(), interaction.submissions)
        assertEquals(emptyList(), controller.state.value.transcript)
    }

    @Test
    fun `reply callback is ordered before silent Delivered completion`() = runTest {
        val controller = OwnerUiController(
            OfflineOwnerInteraction(listOf(OfflineOwnerScenario.ReplyDelivered("reply"))),
            coroutineContext,
        )

        controller.submit("question")
        runCurrent()

        assertEquals(
            listOf(
                OwnerTranscriptEntry.Owner("question"),
                OwnerTranscriptEntry.Parker("reply"),
            ),
            controller.state.value.transcript,
        )
        assertEquals(OwnerUiStatus.READY, controller.state.value.status)
    }

    @Test
    fun `Unavailable outcome remains stopped and blocks later submission`() = runTest {
        val interaction = OfflineOwnerInteraction(
            listOf(OfflineOwnerScenario.Unavailable("offline stopped")),
        )
        val controller = OwnerUiController(interaction, coroutineContext)

        controller.submit("first")
        runCurrent()
        assertEquals(OwnerUiStatus.STOPPED, controller.state.value.status)

        assertEquals(OwnerSubmissionAttempt.UNAVAILABLE, controller.submit("second"))
        assertEquals(listOf("first"), interaction.submissions)
        assertEquals(
            listOf(
                OwnerTranscriptEntry.Owner("first"),
                OwnerTranscriptEntry.System("Stopped: offline stopped"),
            ),
            controller.state.value.transcript,
        )
    }
}
