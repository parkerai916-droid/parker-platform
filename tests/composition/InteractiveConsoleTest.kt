package parker.composition

import java.time.Instant
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import parker.core.interfaces.ExecutionResult
import parker.core.interfaces.ExecutionResultStatus
import parker.core.interfaces.InboundOwnerMessage
import parker.core.interfaces.ModuleId
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.RequestId
import parker.core.interfaces.ResultId
import parker.core.interfaces.PlanningSessionId
import parker.core.runtime.GoalPlanningHandoffOutcome
import parker.core.interfaces.PlanningSessionResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Focused tests for [runInteractiveConsole] -- deliberately exercised
 * without any real [ParkerRuntime]: [readLine]/[writeLine]/[submit] are all
 * fakes, per this function's own KDoc ("decoupled from ParkerRuntime on
 * purpose").
 */
@OptIn(ExperimentalCoroutinesApi::class)
class InteractiveConsoleTest {

    private val channelId = ModuleId("channel.local-text-console-test")
    private val ownerPrincipalId = PrincipalId("user.owner-console-test")
    private val fixedInstant = Instant.parse("2026-01-01T00:00:00Z")

    private fun linesThenEof(vararg lines: String): () -> String? {
        val remaining = ArrayDeque(lines.toList())
        return { remaining.removeFirstOrNull() }
    }

    private fun sampleExecutionResult() = ExecutionResult(
        resultId = ResultId("result-console-test"),
        requestId = RequestId("request-console-test"),
        status = ExecutionResultStatus.SUCCESS,
        startedAt = fixedInstant,
        completedAt = fixedInstant,
    )

    @Test
    fun `a blank line is skipped -- submit is never called for it`() = runTest {
        val submittedMessages = mutableListOf<InboundOwnerMessage>()
        val writtenLines = mutableListOf<String>()

        runInteractiveConsole(
            channelId = channelId,
            ownerPrincipalId = ownerPrincipalId,
            readLine = linesThenEof("   ", "hello"),
            writeLine = writtenLines::add,
            submit = { message ->
                submittedMessages.add(message)
                ParkerRuntimeOutcome.Delivered(sampleExecutionResult())
            },
            clock = { fixedInstant },
        )

        assertEquals(listOf("hello"), submittedMessages.map { it.text })
    }

    @Test
    fun `EOF ends the loop immediately without calling submit`() = runTest {
        var submitCalls = 0
        val writtenLines = mutableListOf<String>()

        runInteractiveConsole(
            channelId = channelId,
            ownerPrincipalId = ownerPrincipalId,
            readLine = linesThenEof(),
            writeLine = writtenLines::add,
            submit = { submitCalls++; ParkerRuntimeOutcome.Delivered(sampleExecutionResult()) },
            clock = { fixedInstant },
        )

        assertEquals(0, submitCalls)
        assertTrue(writtenLines.any { it.startsWith("[eof]") })
    }

    @Test
    fun `a non-blank line is submitted as an InboundOwnerMessage carrying the configured channel and owner identities`() = runTest {
        val submittedMessages = mutableListOf<InboundOwnerMessage>()

        runInteractiveConsole(
            channelId = channelId,
            ownerPrincipalId = ownerPrincipalId,
            readLine = linesThenEof("good morning parker"),
            writeLine = {},
            submit = { message ->
                submittedMessages.add(message)
                ParkerRuntimeOutcome.Delivered(sampleExecutionResult())
            },
            clock = { fixedInstant },
        )

        assertEquals(1, submittedMessages.size)
        val message = submittedMessages.single()
        assertEquals(channelId, message.channelId)
        assertEquals(ownerPrincipalId, message.senderPrincipalId)
        assertEquals("good morning parker", message.text)
        assertEquals(fixedInstant, message.timestamp)
    }

    @Test
    fun `Delivered produces no console output of its own -- the reply is printed by the OwnerNotificationSink instead`() = runTest {
        val writtenLines = mutableListOf<String>()

        runInteractiveConsole(
            channelId = channelId,
            ownerPrincipalId = ownerPrincipalId,
            readLine = linesThenEof("hello"),
            writeLine = writtenLines::add,
            submit = { ParkerRuntimeOutcome.Delivered(sampleExecutionResult()) },
            clock = { fixedInstant },
        )

        assertTrue(writtenLines.none { "hello" in it })
        assertTrue(writtenLines.any { it.startsWith("[eof]") })
        assertEquals(1, writtenLines.size)
    }

    @Test
    fun `NotAccepted prints its reason`() = runTest {
        val writtenLines = mutableListOf<String>()

        runInteractiveConsole(
            channelId = channelId,
            ownerPrincipalId = ownerPrincipalId,
            readLine = linesThenEof("hello"),
            writeLine = writtenLines::add,
            submit = { ParkerRuntimeOutcome.NotAccepted("sender does not resolve") },
            clock = { fixedInstant },
        )

        assertTrue(writtenLines.any { it == "[not accepted] sender does not resolve" })
    }

    @Test
    fun `Failed prints its stage and cause message`() = runTest {
        val writtenLines = mutableListOf<String>()

        runInteractiveConsole(
            channelId = channelId,
            ownerPrincipalId = ownerPrincipalId,
            readLine = linesThenEof("hello"),
            writeLine = writtenLines::add,
            submit = { ParkerRuntimeOutcome.Failed(PipelineStage.REASONING, RuntimeException("model unavailable")) },
            clock = { fixedInstant },
        )

        assertTrue(writtenLines.any { it == "[error] (REASONING) model unavailable" })
    }

    @Test
    fun `Planned prints the underlying PlanningSessionResult variant`() = runTest {
        val writtenLines = mutableListOf<String>()
        val planningSessionResult = PlanningSessionResult.Failed(
            planningSessionId = PlanningSessionId("session-console-test"),
            reason = "no viable Plan Candidate",
            rejections = emptyList(),
        )

        runInteractiveConsole(
            channelId = channelId,
            ownerPrincipalId = ownerPrincipalId,
            readLine = linesThenEof("book a dentist appointment"),
            writeLine = writtenLines::add,
            submit = { ParkerRuntimeOutcome.Planned(GoalPlanningHandoffOutcome.Planned(planningSessionResult)) },
            clock = { fixedInstant },
        )

        assertTrue(writtenLines.any { it == "[planned] Failed" })
    }

    @Test
    fun `NotRunning ends the loop cleanly without propagating`() = runTest {
        val writtenLines = mutableListOf<String>()
        var submitCalls = 0

        runInteractiveConsole(
            channelId = channelId,
            ownerPrincipalId = ownerPrincipalId,
            readLine = linesThenEof("hello", "this should never be read"),
            writeLine = writtenLines::add,
            submit = {
                submitCalls++
                throw ParkerRuntimeException.NotRunning(RuntimeLifecycleState.STOPPING)
            },
            clock = { fixedInstant },
        )

        assertEquals(1, submitCalls)
        assertTrue(writtenLines.any { it.startsWith("[stopped]") })
    }

    @Test
    fun `the spinner starts and prints frames while submit is still suspended`() = runTest {
        val spinnerFrames = mutableListOf<String>()
        val outcomeDeferred = CompletableDeferred<ParkerRuntimeOutcome>()

        val job = launch {
            runInteractiveConsole(
                channelId = channelId,
                ownerPrincipalId = ownerPrincipalId,
                readLine = linesThenEof("hello"),
                writeLine = {},
                submit = { outcomeDeferred.await() },
                clock = { fixedInstant },
                spinnerOutput = spinnerFrames::add,
            )
        }

        // submit() is still suspended (outcomeDeferred is uncompleted) -- advancing virtual time
        // lets the spinner's own child coroutine actually run and print frames.
        advanceTimeBy(250)
        runCurrent()

        assertTrue(spinnerFrames.isNotEmpty(), "expected at least one spinner frame while submit() was still suspended")
        assertTrue(spinnerFrames.all { it.startsWith("\rParker is thinking... ") })
        assertTrue(spinnerFrames.none { '\n' in it }, "spinner frames must update in place, never a permanent new line")

        outcomeDeferred.complete(ParkerRuntimeOutcome.Delivered(sampleExecutionResult()))
        job.join()
    }

    @Test
    fun `the spinner stops (via finally) once submit returns, and writes exactly one clear-line after it`() = runTest {
        val spinnerFrames = mutableListOf<String>()
        val outcomeDeferred = CompletableDeferred<ParkerRuntimeOutcome>()

        val job = launch {
            runInteractiveConsole(
                channelId = channelId,
                ownerPrincipalId = ownerPrincipalId,
                readLine = linesThenEof("hello"),
                writeLine = {},
                submit = { outcomeDeferred.await() },
                clock = { fixedInstant },
                spinnerOutput = spinnerFrames::add,
            )
        }

        advanceTimeBy(250)
        runCurrent()
        val framesWhileSuspended = spinnerFrames.size

        outcomeDeferred.complete(ParkerRuntimeOutcome.Delivered(sampleExecutionResult()))
        job.join()

        // Exactly one more write after submit() returns: the clear-line itself -- no further
        // animated frame sneaks in between the outcome resolving and the spinner job's own
        // cancellation taking effect.
        assertEquals(framesWhileSuspended + 1, spinnerFrames.size)
        val lastWrite = spinnerFrames.last()
        assertFalse("Parker is thinking" in lastWrite)
        assertTrue(lastWrite.all { it == '\r' || it == ' ' })
    }

    @Test
    fun `the spinner stops (via finally) when submit throws NotRunning`() = runTest {
        val spinnerFrames = mutableListOf<String>()

        runInteractiveConsole(
            channelId = channelId,
            ownerPrincipalId = ownerPrincipalId,
            readLine = linesThenEof("hello", "this should never be read"),
            writeLine = {},
            submit = { throw ParkerRuntimeException.NotRunning(RuntimeLifecycleState.STOPPING) },
            clock = { fixedInstant },
            spinnerOutput = spinnerFrames::add,
        )

        // The clear-line write (finally's own unconditional last step) must have happened, and
        // whatever was written last must not still be showing an animated frame.
        assertTrue(spinnerFrames.isNotEmpty())
        assertFalse("Parker is thinking" in spinnerFrames.last())
    }

    @Test
    fun `the spinner is fully cleared before the result line is written`() = runTest {
        val events = mutableListOf<String>()

        runInteractiveConsole(
            channelId = channelId,
            ownerPrincipalId = ownerPrincipalId,
            readLine = linesThenEof("hello"),
            writeLine = { events.add("RESULT:$it") },
            submit = { ParkerRuntimeOutcome.NotAccepted("sender does not resolve") },
            clock = { fixedInstant },
            spinnerOutput = { events.add("SPINNER:$it") },
        )

        val lastSpinnerIndex = events.indexOfLast { it.startsWith("SPINNER:") }
        val resultIndex = events.indexOfFirst { it.startsWith("RESULT:") }
        assertTrue(lastSpinnerIndex >= 0, "expected at least the clear-line spinner write")
        assertTrue(resultIndex >= 0, "expected the [not accepted] result line")
        assertTrue(lastSpinnerIndex < resultIndex, "the spinner's last write must precede the result line")
    }

    @Test
    fun `blank input does not start the spinner`() = runTest {
        val spinnerFrames = mutableListOf<String>()

        runInteractiveConsole(
            channelId = channelId,
            ownerPrincipalId = ownerPrincipalId,
            readLine = linesThenEof("   "),
            writeLine = {},
            submit = { ParkerRuntimeOutcome.Delivered(sampleExecutionResult()) },
            clock = { fixedInstant },
            spinnerOutput = spinnerFrames::add,
        )

        assertTrue(spinnerFrames.isEmpty())
    }

    @Test
    fun `EOF does not start the spinner`() = runTest {
        val spinnerFrames = mutableListOf<String>()

        runInteractiveConsole(
            channelId = channelId,
            ownerPrincipalId = ownerPrincipalId,
            readLine = linesThenEof(),
            writeLine = {},
            submit = { ParkerRuntimeOutcome.Delivered(sampleExecutionResult()) },
            clock = { fixedInstant },
            spinnerOutput = spinnerFrames::add,
        )

        assertTrue(spinnerFrames.isEmpty())
    }

    @Test
    fun `no spinner coroutine remains active once a message finishes processing`() = runTest {
        val spinnerFrames = mutableListOf<String>()

        runInteractiveConsole(
            channelId = channelId,
            ownerPrincipalId = ownerPrincipalId,
            readLine = linesThenEof("hello"),
            writeLine = {},
            submit = { ParkerRuntimeOutcome.Delivered(sampleExecutionResult()) },
            clock = { fixedInstant },
            spinnerOutput = spinnerFrames::add,
        )
        val countAfterCompletion = spinnerFrames.size

        // If the spinner's child coroutine had leaked (not actually cancelled+joined), advancing
        // virtual time well past several frame intervals here would let it keep ticking.
        advanceTimeBy(5_000)
        runCurrent()

        assertEquals(countAfterCompletion, spinnerFrames.size)
    }

    @Test
    fun `spinnerEnabled = false suppresses the spinner entirely, with no functional change`() = runTest {
        val spinnerFrames = mutableListOf<String>()
        val writtenLines = mutableListOf<String>()

        runInteractiveConsole(
            channelId = channelId,
            ownerPrincipalId = ownerPrincipalId,
            readLine = linesThenEof("hello"),
            writeLine = writtenLines::add,
            submit = { ParkerRuntimeOutcome.NotAccepted("sender does not resolve") },
            clock = { fixedInstant },
            spinnerEnabled = false,
            spinnerOutput = spinnerFrames::add,
        )

        assertTrue(spinnerFrames.isEmpty())
        assertTrue(writtenLines.any { it == "[not accepted] sender does not resolve" })
    }

    // --- Reply/spinner interleaving correction --------------------------------------------
    //
    // These tests wire a real BufferingOwnerNotificationSink (not a fake) between a recording
    // delegate and runInteractiveConsole's own beginNotificationBuffering/
    // endNotificationBufferingAndFlush parameters, and have the fake `submit` call
    // bufferingSink.notify(...) itself, from inside submit -- exactly mirroring how the real
    // pipeline calls OwnerNotificationSink.notify from deep inside ParkerRuntime.submitOwnerMessage,
    // before ParkerRuntimeOutcome.Delivered is even returned to this loop.

    @Test
    fun `a Delivered reply is printed only after the spinner has already been cleared, exactly once`() = runTest {
        val events = mutableListOf<String>()
        val bufferingSink = BufferingOwnerNotificationSink(OwnerNotificationSink { text -> events.add("REPLY:$text") })

        runInteractiveConsole(
            channelId = channelId,
            ownerPrincipalId = ownerPrincipalId,
            readLine = linesThenEof("hello"),
            writeLine = { events.add("RESULT:$it") },
            submit = {
                // Simulates the real pipeline: OwnerNotificationSink.notify fires from inside
                // submit(), before the ParkerRuntimeOutcome is even returned.
                bufferingSink.notify("Hello! How can I assist you today?")
                ParkerRuntimeOutcome.Delivered(sampleExecutionResult())
            },
            clock = { fixedInstant },
            spinnerOutput = { events.add("SPINNER:$it") },
            beginNotificationBuffering = bufferingSink::beginBuffering,
            endNotificationBufferingAndFlush = bufferingSink::endBufferingAndFlush,
        )

        val replyIndices = events.withIndex().filter { it.value.startsWith("REPLY:") }.map { it.index }
        assertEquals(1, replyIndices.size, "the reply must be printed exactly once")
        assertEquals(listOf("REPLY:Hello! How can I assist you today?"), events.filter { it.startsWith("REPLY:") })

        val replyIndex = replyIndices.single()
        val lastSpinnerIndex = events.indexOfLast { it.startsWith("SPINNER:") }
        assertTrue(lastSpinnerIndex >= 0, "expected at least the spinner's own clear-line write")
        assertTrue(lastSpinnerIndex < replyIndex, "the spinner's own clear-line write must precede the reply")
        assertTrue(events.drop(replyIndex + 1).none { it.startsWith("SPINNER:") }, "no spinner frame may appear after the reply")
    }

    @Test
    fun `animated spinner frames appear while submit is suspended, and never after the reply is printed`() = runTest {
        val events = mutableListOf<String>()
        val bufferingSink = BufferingOwnerNotificationSink(OwnerNotificationSink { text -> events.add("REPLY:$text") })
        val outcomeDeferred = CompletableDeferred<ParkerRuntimeOutcome>()

        val job = launch {
            runInteractiveConsole(
                channelId = channelId,
                ownerPrincipalId = ownerPrincipalId,
                readLine = linesThenEof("hello"),
                writeLine = {},
                submit = {
                    val outcome = outcomeDeferred.await()
                    bufferingSink.notify("Hello! How can I assist you today?")
                    outcome
                },
                clock = { fixedInstant },
                spinnerOutput = { events.add("SPINNER:$it") },
                beginNotificationBuffering = bufferingSink::beginBuffering,
                endNotificationBufferingAndFlush = bufferingSink::endBufferingAndFlush,
            )
        }

        advanceTimeBy(250)
        runCurrent()
        assertTrue(events.any { it.startsWith("SPINNER:") && "Parker is thinking" in it }, "expected animated frames while submit() was suspended")
        assertTrue(events.none { it.startsWith("REPLY:") }, "the reply must not appear before submit() resolves")

        outcomeDeferred.complete(ParkerRuntimeOutcome.Delivered(sampleExecutionResult()))
        job.join()

        val replyIndex = events.indexOfFirst { it.startsWith("REPLY:") }
        assertTrue(replyIndex >= 0)
        assertTrue(events.drop(replyIndex + 1).none { it.startsWith("SPINNER:") }, "no spinner frame may appear after the reply")
    }

    @Test
    fun `NotAccepted is unaffected by the notification-buffering wiring -- no reply text, unchanged result line`() = runTest {
        val events = mutableListOf<String>()
        val bufferingSink = BufferingOwnerNotificationSink(OwnerNotificationSink { text -> events.add("REPLY:$text") })

        runInteractiveConsole(
            channelId = channelId,
            ownerPrincipalId = ownerPrincipalId,
            readLine = linesThenEof("hello"),
            writeLine = { events.add("RESULT:$it") },
            submit = { ParkerRuntimeOutcome.NotAccepted("sender does not resolve") },
            clock = { fixedInstant },
            spinnerOutput = { events.add("SPINNER:$it") },
            beginNotificationBuffering = bufferingSink::beginBuffering,
            endNotificationBufferingAndFlush = bufferingSink::endBufferingAndFlush,
        )

        assertTrue(events.none { it.startsWith("REPLY:") })
        assertTrue(events.any { it == "RESULT:[not accepted] sender does not resolve" })
    }

    @Test
    fun `multiple sequential messages each clear their own spinner before their own reply, in order, without merging`() = runTest {
        val events = mutableListOf<String>()
        val bufferingSink = BufferingOwnerNotificationSink(OwnerNotificationSink { text -> events.add("REPLY:$text") })
        var submitCalls = 0

        runInteractiveConsole(
            channelId = channelId,
            ownerPrincipalId = ownerPrincipalId,
            readLine = linesThenEof("first message", "second message"),
            writeLine = { events.add("RESULT:$it") },
            submit = { message ->
                submitCalls++
                bufferingSink.notify("reply to ${message.text}")
                ParkerRuntimeOutcome.Delivered(sampleExecutionResult())
            },
            clock = { fixedInstant },
            spinnerOutput = { events.add("SPINNER:$it") },
            beginNotificationBuffering = bufferingSink::beginBuffering,
            endNotificationBufferingAndFlush = bufferingSink::endBufferingAndFlush,
        )

        assertEquals(2, submitCalls)
        assertEquals(
            listOf("REPLY:reply to first message", "REPLY:reply to second message"),
            events.filter { it.startsWith("REPLY:") },
        )

        val firstReplyIndex = events.indexOf("REPLY:reply to first message")
        val secondReplyIndex = events.indexOf("REPLY:reply to second message")
        assertTrue(firstReplyIndex < secondReplyIndex)

        val spinnerBeforeFirstReply = events.subList(0, firstReplyIndex).count { it.startsWith("SPINNER:") }
        val spinnerBetweenReplies = events.subList(firstReplyIndex + 1, secondReplyIndex).count { it.startsWith("SPINNER:") }
        assertTrue(spinnerBeforeFirstReply >= 1, "expected the first message's own spinner clear-line before its reply")
        assertTrue(spinnerBetweenReplies >= 1, "expected the second message's own spinner clear-line before its reply, not shared with the first")
    }

    @Test
    fun `BufferingOwnerNotificationSink forwards immediately when not buffering -- the headless-equivalent, unaffected case`() = runTest {
        val notifications = mutableListOf<String>()
        val sink = BufferingOwnerNotificationSink(OwnerNotificationSink { text -> notifications.add(text) })

        // No beginBuffering() call at all -- mirrors headless mode, which constructs a
        // BufferingOwnerNotificationSink in Main.kt but never passes it into ParkerRuntime, so
        // its beginBuffering/endBufferingAndFlush are never invoked. Every notify() call must
        // reach the delegate immediately, unbuffered, exactly as LoggingOwnerNotificationSink's
        // own direct notify() already does in headless mode.
        sink.notify("first")
        sink.notify("second")

        assertEquals(listOf("first", "second"), notifications)
    }

    @Test
    fun `BufferingOwnerNotificationSink flushes buffered notifications in order, each exactly once`() = runTest {
        val notifications = mutableListOf<String>()
        val sink = BufferingOwnerNotificationSink(OwnerNotificationSink { text -> notifications.add(text) })

        sink.beginBuffering()
        sink.notify("one")
        sink.notify("two")
        assertTrue(notifications.isEmpty(), "notify() must not reach the delegate while buffering")

        sink.endBufferingAndFlush()

        assertEquals(listOf("one", "two"), notifications)
    }
}
