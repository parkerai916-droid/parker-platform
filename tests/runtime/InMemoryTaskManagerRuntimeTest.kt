package parker.core.runtime

import kotlinx.coroutines.test.runTest
import parker.core.interfaces.AgentRunCommandType
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

    // --- accept path ---

    @Test
    fun `submitting a well-formed proposal with a resolvable owner results in exactly one Task in Queued state`() = runTest {
        val identity = InMemoryIdentityService()
        identity.register(principal())
        val runtime = InMemoryTaskManagerRuntime(identity, InMemoryEventBus())

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
        val runtime = InMemoryTaskManagerRuntime(identity, InMemoryEventBus())

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
        val runtime = InMemoryTaskManagerRuntime(identity, InMemoryEventBus())

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
        val runtime = InMemoryTaskManagerRuntime(identity, InMemoryEventBus())

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
        val runtime = InMemoryTaskManagerRuntime(identity, InMemoryEventBus())
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
        val runtime = InMemoryTaskManagerRuntime(identity, InMemoryEventBus())

        val disposition = runtime.submitProposal(proposal())
        val accepted = assertIs<TaskProposalDisposition.Accepted>(disposition)

        val command = runtime.agentRunCommandsFor(accepted.taskId).single()
        assertEquals(emptyList(), command.resourceReferences)
    }

    // --- unresolvable owner ---

    @Test
    fun `an unresolvable owner is Rejected, and no Task or AgentRunCommand is created`() = runTest {
        val identity = InMemoryIdentityService() // no Principal registered
        val runtime = InMemoryTaskManagerRuntime(identity, InMemoryEventBus())

        val disposition = runtime.submitProposal(proposal(ownerPrincipalId = "ghost-user"))

        val rejected = assertIs<TaskProposalDisposition.Rejected>(disposition)
        assertEquals(TaskProposalId("proposal-1"), rejected.taskProposalId)
        assertTrue(rejected.reason.isNotBlank())
        assertTrue(runtime.listTasks().isEmpty())
    }

    // --- unknown Task lookup ---

    @Test
    fun `getTask returns null for an unknown taskId, not an exception`() = runTest {
        val runtime = InMemoryTaskManagerRuntime(InMemoryIdentityService(), InMemoryEventBus())

        assertNull(runtime.getTask(TaskId("task-for-nonexistent")))
        assertTrue(runtime.agentRunCommandsFor(TaskId("task-for-nonexistent")).isEmpty())
    }

    // --- duplicate submission ---

    @Test
    fun `resubmitting the same taskProposalId is rejected as caller misuse`() = runTest {
        val identity = InMemoryIdentityService()
        identity.register(principal())
        val runtime = InMemoryTaskManagerRuntime(identity, InMemoryEventBus())

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
        val runtime = InMemoryTaskManagerRuntime(identity, InMemoryEventBus())

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

    /** A synthetic `agent.*` [ParkerEvent] carrying `taskId` in its payload, exactly as `InMemoryAgentRuntime.publish` already does. */
    private fun agentEvent(
        eventType: String,
        taskId: TaskId,
        agentIdentityPrincipalId: String = "agent-1",
        correlationId: String = "corr-1",
    ) = ParkerEvent(
        eventId = "evt-test-$eventType-${taskId.value}",
        publisherPrincipalId = PrincipalId(agentIdentityPrincipalId),
        eventType = EventType(eventType),
        timestamp = Instant.parse("2026-01-01T00:00:00Z"),
        correlationId = correlationId,
        payload = mapOf("taskId" to taskId.value),
    )

    @Test
    fun `an agent-completed event is recorded against the correct Task`() = runTest {
        val identity = InMemoryIdentityService()
        identity.register(principal())
        val eventBus = InMemoryEventBus()
        val runtime = InMemoryTaskManagerRuntime(identity, eventBus)
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
        val runtime = InMemoryTaskManagerRuntime(identity, eventBus)
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
        val runtime = InMemoryTaskManagerRuntime(identity, eventBus)
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
        val runtime = InMemoryTaskManagerRuntime(identity, eventBus)
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
        val runtime = InMemoryTaskManagerRuntime(identity, eventBus)
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
        val runtime = InMemoryTaskManagerRuntime(identity, eventBus)

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
        val runtime = InMemoryTaskManagerRuntime(InMemoryIdentityService(), InMemoryEventBus())

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
        val runtime = InMemoryTaskManagerRuntime(identity, eventBus)
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
        val runtime = InMemoryTaskManagerRuntime(identity, eventBus)
        val accepted = assertIs<TaskProposalDisposition.Accepted>(runtime.submitProposal(proposal()))

        eventBus.publish(agentEvent("agent.completed", accepted.taskId))

        assertEquals(listOf("task.started", "task.completed"), publishedTypes)
        // Task Event Payload Completion (docs/implementation/TASK_EVENT_PAYLOAD_COMPLETION_IMPLEMENTATION_PLAN.md
        // Section 8's decided, conservative option): task.completed now carries a Task Result
        // summary; task.started is deliberately left unpopulated, not silently untested.
        assertEquals(
            mapOf("taskId" to accepted.taskId.value, "status" to "COMPLETED"),
            publishedPayloads["task.completed"],
        )
        assertEquals(emptyMap(), publishedPayloads["task.started"])
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
        val runtime = InMemoryTaskManagerRuntime(identity, eventBus)
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
        val runtime = InMemoryTaskManagerRuntime(identity, eventBus)
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
        val runtime = InMemoryTaskManagerRuntime(identity, eventBus)
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
        val runtime = InMemoryTaskManagerRuntime(identity, eventBus)
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
        val runtime = InMemoryTaskManagerRuntime(InMemoryIdentityService(), eventBus)

        eventBus.publish(agentEvent("agent.completed", TaskId("task-for-nonexistent"))) // must not throw

        assertTrue(runtime.listTasks().isEmpty())
    }

    // --- 7. B1 event recording still works once B2's transition logic runs alongside it ---

    @Test
    fun `agent-completed both records the event and transitions status`() = runTest {
        val identity = InMemoryIdentityService()
        identity.register(principal())
        val eventBus = InMemoryEventBus()
        val runtime = InMemoryTaskManagerRuntime(identity, eventBus)
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
    // conservative option: task.completed's payload is populated now; task.started's Agent Run
    // Reference is deliberately left unpopulated, not silently untested. The two tests above
    // (Unit B2 section) already assert task.completed's exact payload contents inline; the two
    // tests below are the plan's own Section 4 "dedicated test" requirements: a scope-discipline
    // proof that task.completed's payload never claims more than this class actually tracks, and
    // an explicit proof that task.started's emptiness is intentional.

    @Test
    fun `task-completed's payload never claims an Execution Reference or Agent Result field this class does not track`() = runTest {
        val identity = InMemoryIdentityService()
        identity.register(principal())
        val eventBus = InMemoryEventBus()
        var completedPayload: Map<String, String>? = null
        eventBus.subscribe(EventType("task.completed"), PrincipalId("test-subscriber")) { event ->
            completedPayload = event.payload
        }
        val runtime = InMemoryTaskManagerRuntime(identity, eventBus)
        val accepted = assertIs<TaskProposalDisposition.Accepted>(runtime.submitProposal(proposal()))

        eventBus.publish(agentEvent("agent.completed", accepted.taskId))

        // Exactly taskId + status -- the only two components of a Task Result this class has
        // evidence for (TaskManagerRuntimeSpecification.md §4). No "executionReferences",
        // "agentResults", or similar key is fabricated for data this class never tracks.
        assertEquals(setOf("taskId", "status"), completedPayload?.keys)
    }

    @Test
    fun `task-started's payload remains deliberately empty -- Agent Run Reference is an intentional deferral, not an oversight`() = runTest {
        val identity = InMemoryIdentityService()
        identity.register(principal())
        val eventBus = InMemoryEventBus()
        var startedPayload: Map<String, String>? = null
        var startedPublishCount = 0
        eventBus.subscribe(EventType("task.started"), PrincipalId("test-subscriber")) { event ->
            startedPublishCount++
            startedPayload = event.payload
        }
        val runtime = InMemoryTaskManagerRuntime(identity, eventBus)
        val accepted = assertIs<TaskProposalDisposition.Accepted>(runtime.submitProposal(proposal()))

        eventBus.publish(agentEvent("agent.completed", accepted.taskId))

        assertEquals(1, startedPublishCount)
        // Per the Implementation Plan's Section 8 decision: no AgentRunId is reconstructed
        // locally, and InMemoryAgentRuntime is not modified to supply one -- task.started's
        // payload is exactly emptyMap(), unchanged from before this unit.
        assertEquals(emptyMap(), startedPayload)
    }
}
