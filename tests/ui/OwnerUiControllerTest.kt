package parker.ui

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class OwnerUiControllerTest {

    @Test
    fun `owner text is submitted only through OwnerInteraction`() = runTest {
        val interaction = OfflineOwnerInteraction(listOf(OfflineOwnerScenario.Delivered()))
        val controller = OwnerUiController(interaction, coroutineContext)

        assertEquals(OwnerSubmissionAttempt.STARTED, controller.submit("hello"))
        runCurrent()

        assertEquals(listOf("hello"), interaction.submissions)
        assertEquals("hello", assertIs<OwnerTranscriptEntry.Owner>(controller.state.value.transcript.first()).text)
    }

    @Test
    fun `blank input is rejected without submission or transcript mutation`() = runTest {
        val interaction = OfflineOwnerInteraction(listOf(OfflineOwnerScenario.Delivered()))
        val controller = OwnerUiController(interaction, coroutineContext)

        assertEquals(OwnerSubmissionAttempt.BLANK_REJECTED, controller.submit("  \t"))

        assertTrue(interaction.submissions.isEmpty())
        assertTrue(controller.state.value.transcript.isEmpty())
    }

    @Test
    fun `second submission cannot begin while first is processing`() = runTest {
        val interaction = OfflineOwnerInteraction(
            listOf(OfflineOwnerScenario.Delivered(delayMillis = 1_000)),
        )
        val controller = OwnerUiController(interaction, coroutineContext)

        assertEquals(OwnerSubmissionAttempt.STARTED, controller.submit("first"))
        runCurrent()
        assertEquals(OwnerUiStatus.PROCESSING, controller.state.value.status)
        assertEquals(OwnerSubmissionAttempt.ALREADY_PROCESSING, controller.submit("second"))

        assertEquals(listOf("first"), interaction.submissions)
    }

    @Test
    fun `Parker transcript text enters only through reply receiver`() = runTest {
        val interaction = OfflineOwnerInteraction(
            listOf(OfflineOwnerScenario.ReplyDelivered("Parker reply")),
        )
        val controller = OwnerUiController(interaction, coroutineContext)

        controller.submit("owner message")
        runCurrent()

        val parkerEntries = controller.state.value.transcript.filterIsInstance<OwnerTranscriptEntry.Parker>()
        assertEquals(listOf("Parker reply"), parkerEntries.map { it.text })
    }

    @Test
    fun `Delivered alone never fabricates Parker reply text`() = runTest {
        val controller = OwnerUiController(
            OfflineOwnerInteraction(listOf(OfflineOwnerScenario.Delivered("SUCCESS"))),
            coroutineContext,
        )

        controller.submit("hello")
        runCurrent()

        assertEquals(listOf(OwnerTranscriptEntry.Owner("hello")), controller.state.value.transcript)
    }

    @Test
    fun `NotAccepted is system status preserving reason`() = runTest {
        val controller = OwnerUiController(
            OfflineOwnerInteraction(listOf(OfflineOwnerScenario.NotAccepted("sender does not resolve"))),
            coroutineContext,
        )

        controller.submit("hello")
        runCurrent()

        assertTrue(controller.state.value.transcript.none { it is OwnerTranscriptEntry.Parker })
        assertTrue(controller.state.value.transcript.filterIsInstance<OwnerTranscriptEntry.System>()
            .any { it.text == "Not accepted: sender does not resolve" })
        assertEquals(OwnerUiStatus.READY, controller.state.value.status)
    }

    @Test
    fun `Failed preserves stage and safe message as system status`() = runTest {
        val controller = OwnerUiController(
            OfflineOwnerInteraction(listOf(OfflineOwnerScenario.Failed("REASONING", "model unavailable"))),
            coroutineContext,
        )

        controller.submit("hello")
        runCurrent()

        assertTrue(controller.state.value.transcript.none { it is OwnerTranscriptEntry.Parker })
        assertTrue(controller.state.value.transcript.filterIsInstance<OwnerTranscriptEntry.System>()
            .any { it.text == "Failed (REASONING): model unavailable" })
        assertEquals(OwnerUiStatus.ERROR, controller.state.value.status)
        assertFalse(controller.state.value.submissionActive)
    }

    @Test
    fun `Planned preserves result category as system status`() = runTest {
        val controller = OwnerUiController(
            OfflineOwnerInteraction(listOf(OfflineOwnerScenario.Planned("Completed"))),
            coroutineContext,
        )

        controller.submit("make a plan")
        runCurrent()

        assertTrue(controller.state.value.transcript.none { it is OwnerTranscriptEntry.Parker })
        assertTrue(controller.state.value.transcript.filterIsInstance<OwnerTranscriptEntry.System>()
            .any { it.text == "Planned: Completed" })
    }

    @Test
    fun `unavailable interaction produces stopped state`() = runTest {
        val controller = OwnerUiController(
            OfflineOwnerInteraction(listOf(OfflineOwnerScenario.Unavailable("runtime stopped"))),
            coroutineContext,
        )

        controller.submit("hello")
        runCurrent()

        assertEquals(OwnerUiStatus.STOPPED, controller.state.value.status)
        assertFalse(controller.state.value.submissionActive)
        assertTrue(controller.state.value.transcript.filterIsInstance<OwnerTranscriptEntry.System>()
            .any { it.text == "Stopped: runtime stopped" })
    }

    @Test
    fun `delayed scenario exposes Processing before deterministic completion`() = runTest {
        val controller = OwnerUiController(
            OfflineOwnerInteraction(listOf(OfflineOwnerScenario.Delivered(delayMillis = 1_000))),
            coroutineContext,
        )

        controller.submit("hello")
        runCurrent()
        assertEquals(OwnerUiStatus.PROCESSING, controller.state.value.status)
        assertTrue(controller.state.value.submissionActive)

        advanceTimeBy(1_000)
        runCurrent()
        assertEquals(OwnerUiStatus.READY, controller.state.value.status)
        assertFalse(controller.state.value.submissionActive)
    }

    @Test
    fun `shutdown cancels delayed submission and cannot leave it active`() = runTest {
        val controller = OwnerUiController(
            OfflineOwnerInteraction(listOf(OfflineOwnerScenario.Delivered(delayMillis = 10_000))),
            coroutineContext,
        )
        controller.submit("hello")
        runCurrent()

        controller.shutdown("Window closed")
        runCurrent()

        assertEquals(OwnerUiStatus.STOPPED, controller.state.value.status)
        assertFalse(controller.state.value.submissionActive)
        assertEquals(OwnerSubmissionAttempt.UNAVAILABLE, controller.submit("too late"))
    }

    @Test
    fun `unexpected adapter failure does not expose raw exception text`() = runTest {
        val interaction = object : OwnerInteraction {
            override val availability = OwnerInteractionAvailability.AVAILABLE
            override suspend fun submit(
                ownerText: String,
                onReply: suspend (OwnerReply) -> Unit,
            ): OwnerSubmissionDisposition = error("sensitive internal detail")
        }
        val controller = OwnerUiController(interaction, coroutineContext)

        controller.submit("hello")
        runCurrent()

        val systemText = controller.state.value.transcript.filterIsInstance<OwnerTranscriptEntry.System>().map { it.text }
        assertEquals(listOf("Owner interaction failed unexpectedly"), systemText)
        assertTrue(systemText.none { "sensitive" in it })
        assertEquals(OwnerUiStatus.ERROR, controller.state.value.status)
        assertFalse(controller.state.value.submissionActive)
    }
}
