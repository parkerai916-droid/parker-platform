package parker.composition

import java.time.Instant
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
import kotlin.test.assertTrue

/**
 * Focused tests for [runInteractiveConsole] -- deliberately exercised
 * without any real [ParkerRuntime]: [readLine]/[writeLine]/[submit] are all
 * fakes, per this function's own KDoc ("decoupled from ParkerRuntime on
 * purpose").
 */
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
}
