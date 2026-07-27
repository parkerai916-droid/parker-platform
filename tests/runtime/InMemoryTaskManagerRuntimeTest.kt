package parker.core.runtime

import kotlinx.coroutines.test.runTest
import parker.core.interfaces.AgentRunCommand
import parker.core.interfaces.AgentRunCommandChannel
import parker.core.interfaces.AgentRunCommandResult
import parker.core.interfaces.AgentRunCommandType
import parker.core.interfaces.AgentRunExecutionTrigger
import parker.core.interfaces.AgentRunId
import parker.core.interfaces.EventType
import parker.core.interfaces.ParkerEvent
import parker.core.interfaces.PermissionAction
import parker.core.interfaces.PlanningSessionId
import parker.core.interfaces.Principal
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.PrincipalStatus
import parker.core.interfaces.PrincipalType
import parker.core.interfaces.RequestOrigin
import parker.core.interfaces.RequestPriority
import parker.core.interfaces.ResourceId
import parker.core.interfaces.Task
import parker.core.interfaces.TaskId
import parker.core.interfaces.TaskLifecycleTransitions
import parker.core.interfaces.TaskProposal
import parker.core.interfaces.TaskProposalDisposition
import parker.core.interfaces.TaskProposalId
import parker.core.interfaces.TaskStatus
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Sprint 1, Unit 6 acceptance test
 * (`docs/implementation/SPRINT_1_VERTICAL_SLICE_PLAN.md` §6, Unit 6; §7's
 * "Task Manager accepts proposal and creates Task" / "Task Manager
 * requests Agent Run" rows): "submitting a well-formed `TaskProposal`
 * results in exactly one Task in `Queued` state and exactly one
 * `AgentRunRequest` constructed" (realised as `AgentRunCommand`, per
 * `AgentRunCommand.kt`'s own note that it closes the "Agent Run Request
 * has no named, shaped object" gap), "with `ownerPrincipalId` resolved
 * through the Identity Service (not a Task-Manager-local store)."
 *
 * Scope note: this file proves Unit 6 (accept-only intake for a
 * resolvable owner, `Created -> Queued`, one constructed
 * `AgentRunCommand`). It does not call `AgentRunCommandChannel.submit`
 * (no implementation exists -- Unit 7), and it does not exercise any Task
 * Status beyond `Created`/`Queued` -- see `TaskLifecycleTransitionsTest.kt`
 * for the full 9-state lifecycle's own coverage, independent of this
 * runtime.
 *
 * Sprint 2, Track B, Unit B1 adds coverage (below, its own section) for
 * `InMemoryTaskManagerRuntime`'s new `agent.completed`/`agent.failed`
 * subscription -- recording only, per `IMPLEMENTATION_GAPS.md` #42.
 *
 * Sprint 2, Track B, Unit B2 adds further coverage (its own section,
 * below Unit B1's) for the fixed, minimal `TaskStatus` transition rule
 * `docs/implementation/SPRINT_2_B2_IMPLEMENTATION_DECISIONS.md` settles
 * for `agent.completed` -- `agent.failed` still causes no transition.
 */
class InMemoryTaskManagerRuntimeTest {

    private fun principal(id: String = "user-1") = Principal(
        principalId = PrincipalId(id),
        principalType = PrincipalType.USER,
        displayName = "Test Principal",
        owner = null,
        status = PrincipalStatus.CREATED,
        createdAt = Instant.parse("2026-01-01T00:00:00Z"),
        lastSeenAt = Instant.parse("2026-01-01T00:00:00Z"),
    )

    private fun proposal(
        taskProposalId: String = "proposal-1",
        ownerPrincipalId: String = "user-1",
        goal: String = "read today's calendar",
        correlationId: String = "corr-1",
    ) = TaskProposal(
        taskProposalId = TaskProposalId(taskProposalId),
        planningSessionId = PlanningSessionId("session-1"),
        initiatingPrincipalId = PrincipalId(ownerPrincipalId),
        proposedOwnerPrincipalId = PrincipalId(ownerPrincipalId),
        goal = goal,
        source = RequestOrigin.TEXT,
        priority = RequestPriority.NORMAL,
        correlationId = correlationId,
    )

    /**
     * Controlled Agent Run Submission (`docs/implementation/CONTROLLED_AGENT_RUN_SUBMISSION_SCOPE_LOCK.md`
     * Section 11, item 1): a fake [AgentRunCommandChannel], threaded through every test in this
     * file via the new third constructor parameter. Defaults to [AgentRunCommandResult.Rejected]
     * -- not [AgentRunCommandResult.Accepted] -- specifically so every pre-existing test's
     * assertions (most of which expect a Task to remain `QUEUED` immediately after
     * `submitProposal`, per this class's own Sprint 1/Sprint 2 scope) continue to hold exactly
     * as before: a default-`Accepted` fake would make every proposal's Task jump straight to
     * `RUNNING`, which is not what any pre-existing test was written to exercise. Tests that
     * specifically target the new `Accepted`/`Rejected` branches (below) configure this fake
     * explicitly instead of relying on the default.
     */
    private class FakeAgentRunCommandChannel(
        private val resultFor: (AgentRunCommand) -> AgentRunCommandResult = {
            AgentRunCommandResult.Rejected(it.commandType, "test fixture default -- no Agent Run started")
        },
    ) : AgentRunCommandChannel {
        val submittedCommands = mutableListOf<AgentRunCommand>()

        override suspend fun submit(command: AgentRunCommand): AgentRunCommandResult {
            submittedCommands += command
            return resultFor(command)
        }
    }

    /**
     * Two-Phase Acceptance/Execution Amendment
     * (`docs/implementation/CONTROLLED_AGENT_RUN_SUBMISSION_SCOPE_LOCK.md`,
     * "Amendment -- Two-Phase Agent Run Operation," A.1/A.2/A.6 item 4): a fake
     * [AgentRunExecutionTrigger], threaded through every test in this file via the new fourth
     * constructor parameter. Records every `agentRunId` it is called with; performs no other
     * behaviour, since no test in this file observes anything `execute()` itself would do
     * (`InMemoryAgentRuntimeTest.kt` covers that). Most tests in this file use
     * [FakeAgentRunCommandChannel]'s default `Rejected` result, so this trigger is never called
     * for them -- only the dedicated `Accepted`-branch test below expects a call.
     */
    private class FakeAgentRunExecutionTrigger : AgentRunExecutionTrigger {
        val executedAgentRunIds = mutableListOf<AgentRunId>()

        override suspend fun execute(agentRunId: AgentRunId) {
            executedAgentRunIds += agentRunId
        }
    }

    // --- accept path ---

    @Test
    fun `submitting a well-formed proposal with a resolvable owner results in exactly one Task in Queued state`() = runTest {
        val identity = InMemoryIdentityService()
        identity.register(principal())
        val runtime = InMemoryTaskManagerRuntime(identity, InMemoryEventBus(), FakeAgentRunCommandChannel(), FakeAgentRunExecutionTrigger())

        val disposition = runtime.submitProposal(proposal())

        val accepted = assertIs<TaskProposalDisposition.Accepted>(disposition)
        val task = runtime.getTask(accepted.taskId)
        assertNotNull(task)
        assertEquals(TaskStatus.QUEUED, task.status)
        assertEquals(PrincipalId("user-1"), task.ownerPrincipalId)
        assertEquals("read today's calendar", task.goal)
        assertEquals("corr-1", task.correlationId)
        assertEquals(TaskProposalId("proposal-1"), task.originatingTaskProposalId)

        assertEquals(listOf(task), runtime.listTasks())
    }

    @Test
    fun `ownerPrincipalId is resolved through the Identity Service, not trusted as-is`() = runTest {
        val identity = InMemoryIdentityService()
        val registered = principal("user-1")
        identity.register(registered)
        val runtime = InMemoryTaskManagerRuntime(identity, InMemoryEventBus(), FakeAgentRunCommandChannel(), FakeAgentRunExecutionTrigger())

        val disposition = runtime.submitProposal(proposal(ownerPrincipalId = "user-1"))

        val accepted = assertIs<TaskProposalDisposition.Accepted>(disposition)
        val task = runtime.getTask(accepted.taskId)
        assertEquals(registered.principalId, task?.ownerPrincipalId)
    }

    // --- Agent Run Command construction ---

    @Test
    fun `accepting a proposal constructs exactly one AgentRunCommand referencing the created Task`() = runTest {
        val identity = InMemoryIdentityService()
        identity.register(principal())
        val runtime = InMemoryTaskManagerRuntime(identity, InMemoryEventBus(), FakeAgentRunCommandChannel(), FakeAgentRunExecutionTrigger())

        val disposition = runtime.submitProposal(proposal())
        val accepted = assertIs<TaskProposalDisposition.Accepted>(disposition)

        val commands = runtime.agentRunCommandsFor(accepted.taskId)
        assertEquals(1, commands.size)
        val command = commands.single()
        assertEquals(AgentRunCommandType.START, command.commandType)
        assertEquals(accepted.taskId, command.taskId)
        assertEquals(null, command.agentRunId)
        assertEquals("read today's calendar", command.goalDescription)
        assertEquals("corr-1", command.correlationId)
        assertEquals(PrincipalId("user-1"), command.requestingPrincipalId)
    }

    @Test
    fun `requiredCapabilities on the proposal carry forward to targetAgentCapability on the command`() = runTest {
        val identity = InMemoryIdentityService()
        identity.register(principal())
        val runtime = InMemoryTaskManagerRuntime(identity, InMemoryEventBus(), FakeAgentRunCommandChannel(), FakeAgentRunExecutionTrigger())

        val withCapabilities = proposal().copy(requiredCapabilities = setOf(PermissionAction.READ))
        val disposition = runtime.submitProposal(withCapabilities)
        val accepted = assertIs<TaskProposalDisposition.Accepted>(disposition)

        val command = runtime.agentRunCommandsFor(accepted.taskId).single()
        assertEquals(setOf(PermissionAction.READ), command.targetAgentCapability)
    }

    // --- resourceReferences propagation (Sprint 1, Unit 11B) ---

    @Test
    fun `proposal resourceReferences propagate unchanged to the command's resourceReferences`() = runTest {
        val identity = InMemoryIdentityService()
        identity.register(principal())
        val runtime = InMemoryTaskManagerRuntime(identity, InMemoryEventBus(), FakeAgentRunCommandChannel(), FakeAgentRunExecutionTrigger())
        val calendarResourceId = ResourceId("res.calendar.1")

        val withResources = proposal().copy(resourceReferences = listOf(calendarResourceId))
        val disposition = runtime.submitProposal(withResources)
        val accepted = assertIs<TaskProposalDisposition.Accepted>(disposition)

        val command = runtime.agentRunCommandsFor(accepted.taskId).single()
        assertEquals(listOf(calendarResourceId), command.resourceReferences)
    }

    @Test
    fun `a proposal with no resourceReferences produces a command with an empty resourceReferences, not a default fabrication`() = runTest {
        val identity = InMemoryIdentityService()
        identity.register(principal())
        val runtime = InMemoryTaskManagerRuntime(identity, InMemoryEventBus(), FakeAgentRunCommandChannel(), FakeAgentRunExecutionTrigger())

        val disposition = runtime.submitProposal(proposal())
        val accepted = assertIs<TaskProposalDisposition.Accepted>(disposition)

        val command = runtime.agentRunCommandsFor(accepted.taskId).single()
        assertEquals(emptyList(), command.resourceReferences)
    }

    // --- unresolvable owner ---

    @Test
    fun `an unresolvable owner is Rejected, and no Task or AgentRunCommand is created`() = runTest {
        val identity = InMemoryIdentityService() // no Principal registered
        val runtime = InMemoryTaskManagerRuntime(identity, InMemoryEventBus(), FakeAgentRunCommandChannel(), FakeAgentRunExecutionTrigger())

        val disposition = runtime.submitProposal(proposal(ownerPrincipalId = "ghost-user"))

        val rejected = assertIs<TaskProposalDisposition.Rejected>(disposition)
        assertEquals(TaskProposalId("proposal-1"), rejected.taskProposalId)
        assertTrue(rejected.reason.isNotBlank())
        assertTrue(runtime.listTasks().isEmpty())
    }

    // --- unknown Task lookup ---

    @Test
    fun `getTask returns null for an unknown taskId, not an exception`() = runTest {
        val runtime = InMemoryTaskManagerRuntime(InMemoryIdentityService(), InMemoryEventBus(), FakeAgentRunCommandChannel(), FakeAgentRunExecutionTrigger())

        assertNull(runtime.getTask(TaskId("task-for-nonexistent")))
        assertTrue(runtime.agentRunCommandsFor(TaskId("task-for-nonexistent")).isEmpty())
    }

    // --- duplicate submission ---

    @Test
    fun `resubmitting the same taskProposalId is rejected as caller misuse`() = runTest {
        val identity = InMemoryIdentityService()
        identity.register(principal())
        val runtime = InMemoryTaskManagerRuntime(identity, InMemoryEventBus(), FakeAgentRunCommandChannel(), FakeAgentRunExecutionTrigger())

        runtime.submitProposal(proposal())

        assertFailsWith<IllegalStateException> {
            runtime.submitProposal(proposal())
        }
    }

    // --- isolation between independent proposals (no regression / no cross-contamination) ---

    @Test
    fun `two independent proposals produce two independent Tasks and command lists`() = runTest {
        val identity = InMemoryIdentityService()
        identity.register(principal("user-1"))
        identity.register(principal("user-2"))
        val runtime = InMemoryTaskManagerRuntime(identity, InMemoryEventBus(), FakeAgentRunCommandChannel(), FakeAgentRunExecutionTrigger())

        val first = assertIs<TaskProposalDisposition.Accepted>(
            runtime.submitProposal(proposal(taskProposalId = "proposal-1", ownerPrincipalId = "user-1", correlationId = "corr-1")),
        )
        val second = assertIs<TaskProposalDisposition.Accepted>(
            runtime.submitProposal(proposal(taskProposalId = "proposal-2", ownerPrincipalId = "user-2", correlationId = "corr-2")),
        )

        assertTrue(first.taskId != second.taskId)
        assertEquals(2, runtime.listTasks().size)
        assertEquals(PrincipalId("user-1"), runtime.getTask(first.taskId)?.ownerPrincipalId)
        assertEquals(PrincipalId("user-2"), runtime.getTask(second.taskId)?.ownerPrincipalId)
        assertEquals(1, runtime.agentRunCommandsFor(first.taskId).size)
        assertEquals(1, runtime.agentRunCommandsFor(second.taskId).size)
    }

    // ================= Sprint 2, Track B, Unit B1: Agent-Event Subscription =================
    //
    // Closes the subscription/recording half of IMPLEMENTATION_GAPS.md #42
    // (TaskManagerRuntimeSpecification.md §6/§11). Only agent.completed/agent.failed are
    // exercised -- the only two of the five §6-named event types any production code
    // currently emits (InMemoryAgentRuntime never drives CANCELLED, and
    // agent.action_denied/agent.action_deferred have no corresponding AgentRunStatus or
    // code path at all). No test in this section asserts a TaskStatus change -- that is
    // Unit B2's scope, not this one's.

    /**
     * A synthetic `agent.*` [ParkerEvent] carrying `taskId` in its payload, exactly as
     * `InMemoryAgentRuntime.publish` already does. [agentRunId] is optional and defaulted to
     * `null` (omitted from the payload entirely) so every existing call site that predates
     * Agent Run Reference Exposure (`docs/implementation/AGENT_RUN_REFERENCE_EXPOSURE_IMPLEMENTATION_PLAN.md`)
     * is unaffected -- when supplied, it mirrors `InMemoryAgentRuntime.publish`'s own
     * `"agentRunId" to run.agentRunId.value` payload entry.
     */
    private fun agentEvent(
        eventType: String,
        taskId: TaskId,
        agentIdentityPrincipalId: String = "agent-1",
        correlationId: String = "corr-1",
        agentRunId: String? = null,
    ) = ParkerEvent(
        eventId = "evt-test-$eventType-${taskId.value}",
        publisherPrincipalId = PrincipalId(agentIdentityPrincipalId),
        eventType = EventType(eventType),
        timestamp = Instant.parse("2026-01-01T00:00:00Z"),
        correlationId = correlationId,
        payload = if (agentRunId != null) {
            mapOf("taskId" to taskId.value, "agentRunId" to agentRunId)
        } else {
            mapOf("taskId" to taskId.value)
        },
    )

    @Test
    fun `an agent-completed event is recorded against the correct Task`() = runTest {
        val identity = InMemoryIdentityService()
        identity.register(principal())
        val eventBus = InMemoryEventBus()
        val runtime = InMemoryTaskManagerRuntime(identity, eventBus, FakeAgentRunCommandChannel(), FakeAgentRunExecutionTrigger())
        val accepted = assertIs<TaskProposalDisposition.Accepted>(runtime.submitProposal(proposal()))

        eventBus.publish(agentEvent("agent.completed", accepted.taskId))

        val recorded = runtime.agentEventsFor(accepted.taskId)
        assertEquals(1, recorded.size)
        assertEquals(EventType("agent.completed"), recorded.single().eventType)
    }

    @Test
    fun `an agent-failed event is recorded against the correct Task`() = runTest {
        val identity = InMemoryIdentityService()
        identity.register(principal())
        val eventBus = InMemoryEventBus()
        val runtime = InMemoryTaskManagerRuntime(identity, eventBus, FakeAgentRunCommandChannel(), FakeAgentRunExecutionTrigger())
        val accepted = assertIs<TaskProposalDisposition.Accepted>(runtime.submitProposal(proposal()))

        eventBus.publish(agentEvent("agent.failed", accepted.taskId))

        val recorded = runtime.agentEventsFor(accepted.taskId)
        assertEquals(1, recorded.size)
        assertEquals(EventType("agent.failed"), recorded.single().eventType)
    }

    @Test
    fun `agent events for two different Tasks are not cross-contaminated`() = runTest {
        val identity = InMemoryIdentityService()
        identity.register(principal("user-1"))
        identity.register(principal("user-2"))
        val eventBus = InMemoryEventBus()
        val runtime = InMemoryTaskManagerRuntime(identity, eventBus, FakeAgentRunCommandChannel(), FakeAgentRunExecutionTrigger())
        val first = assertIs<TaskProposalDisposition.Accepted>(
            runtime.submitProposal(proposal(taskProposalId = "proposal-1", ownerPrincipalId = "user-1")),
        )
        val second = assertIs<TaskProposalDisposition.Accepted>(
            runtime.submitProposal(proposal(taskProposalId = "proposal-2", ownerPrincipalId = "user-2")),
        )

        eventBus.publish(agentEvent("agent.completed", first.taskId))
        eventBus.publish(agentEvent("agent.failed", second.taskId))

        assertEquals(1, runtime.agentEventsFor(first.taskId).size)
        assertEquals(EventType("agent.completed"), runtime.agentEventsFor(first.taskId).single().eventType)
        assertEquals(1, runtime.agentEventsFor(second.taskId).size)
        assertEquals(EventType("agent.failed"), runtime.agentEventsFor(second.taskId).single().eventType)
    }

    @Test
    fun `publishing one agent-completed event records exactly one event -- no duplicate subscription`() = runTest {
        val identity = InMemoryIdentityService()
        identity.register(principal())
        val eventBus = InMemoryEventBus()
        val runtime = InMemoryTaskManagerRuntime(identity, eventBus, FakeAgentRunCommandChannel(), FakeAgentRunExecutionTrigger())
        val accepted = assertIs<TaskProposalDisposition.Accepted>(runtime.submitProposal(proposal()))

        eventBus.publish(agentEvent("agent.completed", accepted.taskId))

        // A duplicate `EventBus.subscribe` call in the constructor would cause this same
        // handler to run twice per publish, recording the event twice for one publish call.
        assertEquals(1, runtime.agentEventsFor(accepted.taskId).size)
    }

    @Test
    fun `an agent-completed event with no taskId payload is ignored safely, not an exception`() = runTest {
        val identity = InMemoryIdentityService()
        identity.register(principal())
        val eventBus = InMemoryEventBus()
        val runtime = InMemoryTaskManagerRuntime(identity, eventBus, FakeAgentRunCommandChannel(), FakeAgentRunExecutionTrigger())
        val accepted = assertIs<TaskProposalDisposition.Accepted>(runtime.submitProposal(proposal()))

        val malformed = ParkerEvent(
            eventId = "evt-test-malformed",
            publisherPrincipalId = PrincipalId("agent-1"),
            eventType = EventType("agent.completed"),
            timestamp = Instant.parse("2026-01-01T00:00:00Z"),
            correlationId = "corr-1",
            payload = emptyMap(), // no "taskId" entry
        )

        eventBus.publish(malformed) // must not throw

        assertTrue(runtime.agentEventsFor(accepted.taskId).isEmpty())
    }

    @Test
    fun `an agent-completed event naming an unknown taskId is ignored safely and creates no Task`() = runTest {
        val identity = InMemoryIdentityService()
        identity.register(principal())
        val eventBus = InMemoryEventBus()
        val runtime = InMemoryTaskManagerRuntime(identity, eventBus, FakeAgentRunCommandChannel(), FakeAgentRunExecutionTrigger())

        eventBus.publish(agentEvent("agent.completed", TaskId("task-for-nonexistent"))) // must not throw

        assertTrue(runtime.listTasks().isEmpty())
        assertTrue(runtime.agentEventsFor(TaskId("task-for-nonexistent")).isEmpty())
    }

    // NOTE: A Unit B1-era test previously stood here --
    // `recording an agent-completed event does not change the Task's status` --
    // asserting the Task remained QUEUED after `agent.completed`. That was
    // correct for Unit B1's own scope (recording only, no transition wiring
    // existed yet). Sprint 2, Track B, Unit B2 deliberately superseded this:
    // per `SPRINT_2_IMPLEMENTATION_PLAN.md`'s own Unit B2 Definition of Done
    // ("A Task with one Agent Run transitions Queued -> Completed on
    // agent.completed") and `SPRINT_2_B2_IMPLEMENTATION_DECISIONS.md` item 1,
    // `agent.completed` is now required to drive exactly that transition. The
    // old assertion is removed, not rewritten, because rewriting it to expect
    // COMPLETED would duplicate the Unit B2 test below verbatim
    // (`agent-completed transitions a QUEUED Task through valid lifecycle
    // edges to COMPLETED`), which already covers this exact scenario with the
    // correct, current assertion.

    @Test
    fun `agentEventsFor returns empty for a Task with no recorded events, not an exception`() = runTest {
        val runtime = InMemoryTaskManagerRuntime(InMemoryIdentityService(), InMemoryEventBus(), FakeAgentRunCommandChannel(), FakeAgentRunExecutionTrigger())

        assertTrue(runtime.agentEventsFor(TaskId("task-for-nonexistent")).isEmpty())
    }

    // ================= Sprint 2, Track B, Unit B2: Task Status Transitions =================
    //
    // Implements the fixed rule docs/implementation/SPRINT_2_B2_IMPLEMENTATION_DECISIONS.md
    // settles: agent.completed drives QUEUED -> RUNNING -> COMPLETED (or RUNNING -> COMPLETED
    // only, if already RUNNING; or no mutation, if already COMPLETED); agent.failed still
    // performs no transition. No test in this section relies on, or introduces, a
    // QUEUED -> COMPLETED edge -- TaskLifecycleTransitions has none.

    /**
     * Test-only arrangement helper. There is no public path to observe a
     * Task sitting at `RUNNING`: the only way [InMemoryTaskManagerRuntime]
     * currently reaches `RUNNING` is via `agent.completed` for a `QUEUED`
     * Task, and that same event handler immediately continues on to
     * `COMPLETED` before returning -- see that class's own "Unit B2" KDoc
     * section. This helper only arranges the precondition, via reflection
     * on the private `tasks` map; it does not call, stub, or bypass
     * `applyCompletedTransition` itself. The transition under test --
     * `applyCompletedTransition`'s `TaskStatus.RUNNING` branch, validated
     * by the real, unmodified `TaskLifecycleTransitions.requireValidTransition`
     * -- still runs exactly as production code would when the test
     * publishes `agent.completed`.
     */
    @Suppress("UNCHECKED_CAST")
    private fun forceTaskStatus(runtime: InMemoryTaskManagerRuntime, taskId: TaskId, status: TaskStatus) {
        val tasksField = InMemoryTaskManagerRuntime::class.java.getDeclaredField("tasks")
        tasksField.isAccessible = true
        val tasksMap = tasksField.get(runtime) as MutableMap<TaskId, Task>
        val current = tasksMap.getValue(taskId)
        tasksMap[taskId] = current.copy(status = status)
    }

    // --- 1. agent.completed on a QUEUED Task reaches COMPLETED via both real edges ---

    @Test
    fun `agent-completed transitions a QUEUED Task through valid lifecycle edges to COMPLETED`() = runTest {
        val identity = InMemoryIdentityService()
        identity.register(principal())
        val eventBus = InMemoryEventBus()
        val runtime = InMemoryTaskManagerRuntime(identity, eventBus, FakeAgentRunCommandChannel(), FakeAgentRunExecutionTrigger())
        val accepted = assertIs<TaskProposalDisposition.Accepted>(runtime.submitProposal(proposal()))
        assertEquals(TaskStatus.QUEUED, runtime.getTask(accepted.taskId)?.status)

        eventBus.publish(agentEvent("agent.completed", accepted.taskId))

        assertEquals(TaskStatus.COMPLETED, runtime.getTask(accepted.taskId)?.status)
    }

    @Test
    fun `agent-completed for a QUEUED Task publishes both task-started and task-completed, proving both edges fired`() = runTest {
        val identity = InMemoryIdentityService()
        identity.register(principal())
        val eventBus = InMemoryEventBus()
        val publishedTypes = mutableListOf<String>()
        val publishedPayloads = mutableMapOf<String, Map<String, String>>()
        eventBus.subscribe(EventType("task.started"), PrincipalId("test-subscriber")) { event ->
            publishedTypes += "task.started"
            publishedPayloads["task.started"] = event.payload
        }
        eventBus.subscribe(EventType("task.completed"), PrincipalId("test-subscriber")) { event ->
            publishedTypes += "task.completed"
            publishedPayloads["task.completed"] = event.payload
        }
        val runtime = InMemoryTaskManagerRuntime(identity, eventBus, FakeAgentRunCommandChannel(), FakeAgentRunExecutionTrigger())
        val accepted = assertIs<TaskProposalDisposition.Accepted>(runtime.submitProposal(proposal()))

        eventBus.publish(agentEvent("agent.completed", accepted.taskId, agentRunId = "run-for-${accepted.taskId.value}"))

        assertEquals(listOf("task.started", "task.completed"), publishedTypes)
        // Task Event Payload Completion (docs/implementation/TASK_EVENT_PAYLOAD_COMPLETION_IMPLEMENTATION_PLAN.md
        // Section 8's decided, conservative option): task.completed carries a Task Result summary.
        assertEquals(
            mapOf("taskId" to accepted.taskId.value, "status" to "COMPLETED"),
            publishedPayloads["task.completed"],
        )
        // Agent Run Reference Exposure (docs/implementation/AGENT_RUN_REFERENCE_EXPOSURE_IMPLEMENTATION_PLAN.md):
        // task.started now threads the triggering agent.completed event's own agentRunId
        // through unchanged -- not reconstructed, not derived, read directly from the event.
        assertEquals(
            mapOf("agentRunId" to "run-for-${accepted.taskId.value}"),
            publishedPayloads["task.started"],
        )
    }

    @Test
    fun `agent-completed with no agentRunId payload entry leaves task-started's payload empty, not a fabricated value`() = runTest {
        val identity = InMemoryIdentityService()
        identity.register(principal())
        val eventBus = InMemoryEventBus()
        var startedPayload: Map<String, String>? = null
        eventBus.subscribe(EventType("task.started"), PrincipalId("test-subscriber")) { event ->
            startedPayload = event.payload
        }
        val runtime = InMemoryTaskManagerRuntime(identity, eventBus, FakeAgentRunCommandChannel(), FakeAgentRunExecutionTrigger())
        val accepted = assertIs<TaskProposalDisposition.Accepted>(runtime.submitProposal(proposal()))

        // No agentRunId supplied -- mirrors a triggering agent.completed event that, for
        // whatever reason, carries no agentRunId entry of its own.
        eventBus.publish(agentEvent("agent.completed", accepted.taskId))

        assertEquals(emptyMap(), startedPayload)
    }

    // --- 2. agent.completed on an already-RUNNING Task takes only the second edge ---

    @Test
    fun `agent-completed transitions an already-RUNNING Task to COMPLETED, taking only the second edge`() = runTest {
        val identity = InMemoryIdentityService()
        identity.register(principal())
        val eventBus = InMemoryEventBus()
        val publishedTypes = mutableListOf<String>()
        var completedPayload: Map<String, String>? = null
        eventBus.subscribe(EventType("task.started"), PrincipalId("test-subscriber")) { publishedTypes += "task.started" }
        eventBus.subscribe(EventType("task.completed"), PrincipalId("test-subscriber")) { event ->
            publishedTypes += "task.completed"
            completedPayload = event.payload
        }
        val runtime = InMemoryTaskManagerRuntime(identity, eventBus, FakeAgentRunCommandChannel(), FakeAgentRunExecutionTrigger())
        val accepted = assertIs<TaskProposalDisposition.Accepted>(runtime.submitProposal(proposal()))
        forceTaskStatus(runtime, accepted.taskId, TaskStatus.RUNNING)
        assertEquals(TaskStatus.RUNNING, runtime.getTask(accepted.taskId)?.status)

        eventBus.publish(agentEvent("agent.completed", accepted.taskId))

        assertEquals(TaskStatus.COMPLETED, runtime.getTask(accepted.taskId)?.status)
        assertEquals(listOf("task.completed"), publishedTypes) // task.started not re-published
        assertEquals(mapOf("taskId" to accepted.taskId.value, "status" to "COMPLETED"), completedPayload)
    }

    // --- 3. agent.completed on an already-COMPLETED Task is a no-op ---

    @Test
    fun `agent-completed does not mutate an already-COMPLETED Task`() = runTest {
        val identity = InMemoryIdentityService()
        identity.register(principal())
        val eventBus = InMemoryEventBus()
        val runtime = InMemoryTaskManagerRuntime(identity, eventBus, FakeAgentRunCommandChannel(), FakeAgentRunExecutionTrigger())
        val accepted = assertIs<TaskProposalDisposition.Accepted>(runtime.submitProposal(proposal()))
        eventBus.publish(agentEvent("agent.completed", accepted.taskId))
        assertEquals(TaskStatus.COMPLETED, runtime.getTask(accepted.taskId)?.status)

        eventBus.publish(agentEvent("agent.completed", accepted.taskId)) // must not throw

        assertEquals(TaskStatus.COMPLETED, runtime.getTask(accepted.taskId)?.status)
    }

    // --- 4. agent.failed still performs no transition (Unit B1 behaviour, restated for B2) ---

    @Test
    fun `agent-failed records the event but leaves Task status unchanged`() = runTest {
        val identity = InMemoryIdentityService()
        identity.register(principal())
        val eventBus = InMemoryEventBus()
        val runtime = InMemoryTaskManagerRuntime(identity, eventBus, FakeAgentRunCommandChannel(), FakeAgentRunExecutionTrigger())
        val accepted = assertIs<TaskProposalDisposition.Accepted>(runtime.submitProposal(proposal()))
        assertEquals(TaskStatus.QUEUED, runtime.getTask(accepted.taskId)?.status)

        eventBus.publish(agentEvent("agent.failed", accepted.taskId))

        assertEquals(TaskStatus.QUEUED, runtime.getTask(accepted.taskId)?.status)
        assertEquals(1, runtime.agentEventsFor(accepted.taskId).size)
        assertEquals(EventType("agent.failed"), runtime.agentEventsFor(accepted.taskId).single().eventType)
    }

    // --- 5/6. Missing/unknown taskId are ignored safely for the transition path too ---

    @Test
    fun `agent-completed with a missing taskId payload mutates no Task`() = runTest {
        val identity = InMemoryIdentityService()
        identity.register(principal())
        val eventBus = InMemoryEventBus()
        val runtime = InMemoryTaskManagerRuntime(identity, eventBus, FakeAgentRunCommandChannel(), FakeAgentRunExecutionTrigger())
        val accepted = assertIs<TaskProposalDisposition.Accepted>(runtime.submitProposal(proposal()))
        val malformed = ParkerEvent(
            eventId = "evt-test-malformed-b2",
            publisherPrincipalId = PrincipalId("agent-1"),
            eventType = EventType("agent.completed"),
            timestamp = Instant.parse("2026-01-01T00:00:00Z"),
            correlationId = "corr-1",
            payload = emptyMap(), // no "taskId" entry
        )

        eventBus.publish(malformed) // must not throw

        assertEquals(TaskStatus.QUEUED, runtime.getTask(accepted.taskId)?.status)
    }

    @Test
    fun `agent-completed with an unknown taskId is ignored safely and creates no Task`() = runTest {
        val eventBus = InMemoryEventBus()
        val runtime = InMemoryTaskManagerRuntime(InMemoryIdentityService(), eventBus, FakeAgentRunCommandChannel(), FakeAgentRunExecutionTrigger())

        eventBus.publish(agentEvent("agent.completed", TaskId("task-for-nonexistent"))) // must not throw

        assertTrue(runtime.listTasks().isEmpty())
    }

    // --- 7. B1 event recording still works once B2's transition logic runs alongside it ---

    @Test
    fun `agent-completed both records the event and transitions status`() = runTest {
        val identity = InMemoryIdentityService()
        identity.register(principal())
        val eventBus = InMemoryEventBus()
        val runtime = InMemoryTaskManagerRuntime(identity, eventBus, FakeAgentRunCommandChannel(), FakeAgentRunExecutionTrigger())
        val accepted = assertIs<TaskProposalDisposition.Accepted>(runtime.submitProposal(proposal()))

        eventBus.publish(agentEvent("agent.completed", accepted.taskId))

        assertEquals(TaskStatus.COMPLETED, runtime.getTask(accepted.taskId)?.status)
        assertEquals(1, runtime.agentEventsFor(accepted.taskId).size)
        assertEquals(EventType("agent.completed"), runtime.agentEventsFor(accepted.taskId).single().eventType)
    }

    // --- 8. No direct QUEUED -> COMPLETED edge exists or is relied on ---

    @Test
    fun `TaskLifecycleTransitions has no direct QUEUED to COMPLETED edge -- two edges are required`() {
        assertFalse(TaskLifecycleTransitions.isValidTransition(TaskStatus.QUEUED, TaskStatus.COMPLETED))
        assertTrue(TaskLifecycleTransitions.isValidTransition(TaskStatus.QUEUED, TaskStatus.RUNNING))
        assertTrue(TaskLifecycleTransitions.isValidTransition(TaskStatus.RUNNING, TaskStatus.COMPLETED))
    }

    // ================= Task Event Payload Completion (closes IMPLEMENTATION_GAPS.md #43, in part) =================
    //
    // docs/implementation/TASK_EVENT_PAYLOAD_COMPLETION_IMPLEMENTATION_PLAN.md Section 8's decided,
    // conservative option: task.completed's payload is populated. The test below is the plan's own
    // Section 4 "dedicated test" requirement: a scope-discipline proof that task.completed's payload
    // never claims more than this class actually tracks.

    @Test
    fun `task-completed's payload never claims an Execution Reference or Agent Result field this class does not track`() = runTest {
        val identity = InMemoryIdentityService()
        identity.register(principal())
        val eventBus = InMemoryEventBus()
        var completedPayload: Map<String, String>? = null
        eventBus.subscribe(EventType("task.completed"), PrincipalId("test-subscriber")) { event ->
            completedPayload = event.payload
        }
        val runtime = InMemoryTaskManagerRuntime(identity, eventBus, FakeAgentRunCommandChannel(), FakeAgentRunExecutionTrigger())
        val accepted = assertIs<TaskProposalDisposition.Accepted>(runtime.submitProposal(proposal()))

        eventBus.publish(agentEvent("agent.completed", accepted.taskId))

        // Exactly taskId + status -- the only two components of a Task Result this class has
        // evidence for (TaskManagerRuntimeSpecification.md §4). No "executionReferences",
        // "agentResults", or similar key is fabricated for data this class never tracks.
        assertEquals(setOf("taskId", "status"), completedPayload?.keys)
    }

    // NOTE: A Task Event Payload Completion-era test previously stood here --
    // `task-started's payload remains deliberately empty -- Agent Run Reference is an
    // intentional deferral, not an oversight` -- asserting task.started's payload was always
    // emptyMap(). That was correct for that unit's own scope (task.completed closed; the
    // task.started half of IMPLEMENTATION_GAPS.md #43 deliberately deferred, per that plan's own
    // Section 8 decision). Agent Run Reference Exposure
    // (docs/implementation/AGENT_RUN_REFERENCE_EXPOSURE_IMPLEMENTATION_PLAN.md) deliberately
    // supersedes this: task.started's payload is no longer always empty -- see, earlier in this
    // file (Unit B2 section), `agent-completed for a QUEUED Task publishes both task-started and
    // task-completed, proving both edges fired` (now asserts a populated agentRunId) and
    // `agent-completed with no agentRunId payload entry leaves task-started's payload empty, not
    // a fabricated value`, which together cover both the populated and the absent-value paths
    // this old test's single, always-empty assertion no longer reflects. The old assertion is
    // removed, not rewritten, for the same reason this file's own Unit B1-->B2 supersession note
    // above gives: rewriting it in place would duplicate coverage those two tests already provide
    // correctly.

    // ================= Controlled Agent Run Submission =================
    // docs/implementation/CONTROLLED_AGENT_RUN_SUBMISSION_SCOPE_LOCK.md Sections 5, 6.1, 6.2, 10;
    // Definition of Complete items 3 and 5.

    @Test
    fun `an Accepted AgentRunCommandResult publishes task-agent_run_started and transitions the Task to RUNNING`() = runTest {
        val identity = InMemoryIdentityService()
        identity.register(principal())
        val eventBus = InMemoryEventBus()
        var startedPayload: Map<String, String>? = null
        eventBus.subscribe(EventType("task.agent_run_started"), PrincipalId("test-subscriber")) { event ->
            startedPayload = event.payload
        }
        val channel = FakeAgentRunCommandChannel { command ->
            AgentRunCommandResult.Accepted(AgentRunId("run-for-${command.taskId.value}"), command.commandType)
        }
        val executionTrigger = FakeAgentRunExecutionTrigger()
        val runtime = InMemoryTaskManagerRuntime(identity, eventBus, channel, executionTrigger)

        val disposition = runtime.submitProposal(proposal())

        val accepted = assertIs<TaskProposalDisposition.Accepted>(disposition)
        assertEquals(TaskStatus.RUNNING, runtime.getTask(accepted.taskId)?.status)
        assertEquals(mapOf("agentRunId" to "run-for-${accepted.taskId.value}"), startedPayload)
        assertEquals(1, channel.submittedCommands.size)
        assertEquals(AgentRunCommandType.START, channel.submittedCommands.single().commandType)
        // Two-Phase Acceptance/Execution Amendment (Scope Lock Amendment, A.3/A.6 item 3):
        // AgentRunExecutionTrigger.execute() is invoked exactly once, with the accepted
        // AgentRunId, after mutex.withLock releases.
        assertEquals(listOf(AgentRunId("run-for-${accepted.taskId.value}")), executionTrigger.executedAgentRunIds)
    }

    @Test
    fun `a Rejected AgentRunCommandResult publishes task-agent_run_rejected, preserves the reason, and leaves the Task QUEUED`() = runTest {
        val identity = InMemoryIdentityService()
        identity.register(principal())
        val eventBus = InMemoryEventBus()
        var rejectedPayload: Map<String, String>? = null
        eventBus.subscribe(EventType("task.agent_run_rejected"), PrincipalId("test-subscriber")) { event ->
            rejectedPayload = event.payload
        }
        val channel = FakeAgentRunCommandChannel { command ->
            AgentRunCommandResult.Rejected(command.commandType, "run-initiation permission DENIED for requestingPrincipalId 'user-1'")
        }
        val executionTrigger = FakeAgentRunExecutionTrigger()
        val runtime = InMemoryTaskManagerRuntime(identity, eventBus, channel, executionTrigger)

        val disposition = runtime.submitProposal(proposal())

        // Scope Lock Section 6.1: TaskProposalDisposition.Accepted is returned regardless of the
        // AgentRunCommandResult -- proposal intake and run authorisation are independently
        // reported outcomes.
        val accepted = assertIs<TaskProposalDisposition.Accepted>(disposition)
        assertEquals(TaskStatus.QUEUED, runtime.getTask(accepted.taskId)?.status)
        assertEquals(
            mapOf(
                "reason" to "run-initiation permission DENIED for requestingPrincipalId 'user-1'",
                "commandType" to "START",
            ),
            rejectedPayload,
        )
        // Two-Phase Acceptance/Execution Amendment (Scope Lock Amendment, A.6 item 4):
        // AgentRunExecutionTrigger.execute() is never invoked on the Rejected branch --
        // structurally guaranteed, proven here with a call-count assertion of zero.
        assertTrue(executionTrigger.executedAgentRunIds.isEmpty())
    }
}
