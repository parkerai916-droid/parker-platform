package parker.composition

import java.io.File
import java.time.Instant
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import parker.core.interfaces.CorrelationId
import parker.core.interfaces.ExecutionResult
import parker.core.interfaces.ExecutionResultStatus
import parker.core.interfaces.InboundOwnerMessage
import parker.core.interfaces.ModuleId
import parker.core.interfaces.PlanningSessionId
import parker.core.interfaces.PlanningSessionResult
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.RequestId
import parker.core.interfaces.ResultId
import parker.core.runtime.GoalPlanningHandoffOutcome
import parker.ui.OwnerInteractionAvailability
import parker.ui.OwnerReply
import parker.ui.OwnerSubmissionDisposition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class OwnerUiRuntimeAdapterTest {
    private val owner = PrincipalId("user.owner-ui-adapter-test")
    private val channel = ModuleId("channel.local-text-owner-ui-test")
    private val instant = Instant.parse("2026-08-10T01:02:03Z")
    private val correlationId = CorrelationId("owner-ui-correlation-1")

    @Test
    fun `submission constructs exact inbound message and invokes capability once`() = runTest {
        val submitted = mutableListOf<InboundOwnerMessage>()
        val adapter = adapter { message ->
            submitted += message
            ParkerRuntimeOutcome.Delivered(executionResult())
        }

        val replies = mutableListOf<OwnerReply>()
        val disposition = adapter.submit("  exact owner text  ", replies::add)

        assertEquals(1, submitted.size)
        assertEquals(
            InboundOwnerMessage(
                channelId = channel,
                senderPrincipalId = owner,
                text = "  exact owner text  ",
                timestamp = instant,
                correlationId = correlationId,
            ),
            submitted.single(),
        )
        assertEquals(OwnerSubmissionDisposition.Delivered("SUCCESS"), disposition)
        assertTrue(replies.isEmpty(), "Delivered must not fabricate reply text")
    }

    @Test
    fun `notification sink text enters reply receiver exactly once`() = runTest {
        val bridge = OwnerUiNotificationBridge()
        val adapter = adapter(bridge) {
            bridge.notify("authorised Parker reply")
            ParkerRuntimeOutcome.Delivered(executionResult())
        }
        val replies = mutableListOf<OwnerReply>()

        adapter.submit("hello", replies::add)

        assertEquals(listOf(OwnerReply("authorised Parker reply")), replies)
    }

    @Test
    fun `NotAccepted replaces arbitrary reason with fixed owner-safe status`() = runTest {
        val rawReason = "sender does not resolve at C:\\secret\\identity.json endpoint model=qwen"
        val disposition = adapter { ParkerRuntimeOutcome.NotAccepted(rawReason) }
            .submit("hello") {}

        assertEquals(
            OwnerSubmissionDisposition.NotAccepted("Parker did not accept this message."),
            disposition,
        )
        assertTrue(rawReason !in disposition.toString())
    }

    @Test
    fun `reasoning failure replaces arbitrary cause with fixed owner-safe status`() = runTest {
        val rawMessage = "POST http://127.0.0.1:11434/api/generate returned raw provider response"
        val disposition = adapter {
            ParkerRuntimeOutcome.Failed(PipelineStage.REASONING, IllegalStateException(rawMessage))
        }.submit("hello") {}

        assertEquals(
            OwnerSubmissionDisposition.Failed(
                "REASONING",
                "Parker could not complete reasoning for this message.",
            ),
            disposition,
        )
        assertTrue(rawMessage !in disposition.toString())
    }

    @Test
    fun `unknown failure replaces filesystem credential and exception detail`() = runTest {
        val rawMessage = "/srv/parker/private/token.txt credential=secret model=qwen raw=NOACTION"
        val disposition = adapter {
            ParkerRuntimeOutcome.Failed(PipelineStage.UNKNOWN, IllegalStateException(rawMessage))
        }.submit("hello") {}

        assertEquals(
            OwnerSubmissionDisposition.Failed("UNKNOWN", "Parker could not complete this message."),
            disposition,
        )
        assertTrue(rawMessage !in disposition.toString())
    }

    @Test
    fun `Planned preserves exhaustive result category`() = runTest {
        val planningResult = PlanningSessionResult.Failed(
            planningSessionId = PlanningSessionId("planning-owner-ui-test"),
            reason = "no viable candidate",
            rejections = emptyList(),
        )
        val disposition = adapter {
            ParkerRuntimeOutcome.Planned(GoalPlanningHandoffOutcome.Planned(planningResult))
        }.submit("make a plan") {}

        assertEquals(OwnerSubmissionDisposition.Planned("Failed"), disposition)
    }

    @Test
    fun `NotRunning maps to Unavailable and remains stopped without a second call`() = runTest {
        var calls = 0
        val adapter = adapter {
            calls++
            throw ParkerRuntimeException.NotRunning(RuntimeLifecycleState.STOPPING)
        }

        assertEquals(
            OwnerSubmissionDisposition.Unavailable("Parker Runtime is not accepting messages"),
            adapter.submit("first") {},
        )
        assertEquals(OwnerInteractionAvailability.STOPPED, adapter.availability)
        assertEquals(
            OwnerSubmissionDisposition.Unavailable("Parker Runtime is not accepting messages"),
            adapter.submit("second") {},
        )
        assertEquals(1, calls)
    }

    @Test
    fun `direct concurrent submission is rejected without a second capability call`() = runTest {
        val outcome = CompletableDeferred<ParkerRuntimeOutcome>()
        var calls = 0
        val adapter = adapter {
            calls++
            outcome.await()
        }

        val first = async { adapter.submit("first") {} }
        runCurrent()
        assertFailsWith<IllegalStateException> { adapter.submit("second") {} }
        assertEquals(1, calls)

        outcome.complete(ParkerRuntimeOutcome.Delivered(executionResult()))
        assertIs<OwnerSubmissionDisposition.Delivered>(first.await())
    }

    @Test
    fun `notification without an active UI submission fails safely`() = runTest {
        val bridge = OwnerUiNotificationBridge()

        assertFailsWith<IllegalStateException> { bridge.notify("orphan reply") }
    }

    @Test
    fun `adapter capability is structurally narrow and offline`() {
        val source = File("src/composition/OwnerUiRuntimeAdapter.kt").readText()
        val forbiddenCalls = listOf(
            "submitEvidence(",
            "retrieveEvidence(",
            "deleteEvidenceAsOwner(",
            "analyseEvidence(",
            ".start(",
            ".shutdown(",
        )

        assertTrue(OwnerUiRuntimeAdapter::class.java.declaredFields.none { it.type == ParkerRuntime::class.java })
        assertTrue(OwnerUiNotificationBridge::class.java.declaredFields.none { it.type == ParkerRuntime::class.java })
        assertTrue(forbiddenCalls.none { it in source }, "adapter source must contain no privileged or lifecycle call")
        assertTrue("java.net" !in source && "http" !in source.lowercase())
        assertTrue("LocalHttpModelInferenceClient" !in source)
        assertTrue("ParkerRuntime(" !in source, "tests and adapter must not construct a runtime graph")
    }

    private fun adapter(
        bridge: OwnerUiNotificationBridge = OwnerUiNotificationBridge(),
        submit: suspend (InboundOwnerMessage) -> ParkerRuntimeOutcome,
    ) = OwnerUiRuntimeAdapter(
        ownerPrincipalId = owner,
        localTextChannelModuleId = channel,
        submitOwnerMessage = submit,
        notificationBridge = bridge,
        clock = { instant },
        correlationIdSource = { correlationId },
    )

    private fun executionResult() = ExecutionResult(
        resultId = ResultId("result-owner-ui-adapter"),
        requestId = RequestId("request-owner-ui-adapter"),
        status = ExecutionResultStatus.SUCCESS,
        startedAt = instant,
        completedAt = instant,
    )
}
