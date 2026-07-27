package parker.core.runtime

import kotlinx.coroutines.test.runTest
import parker.core.interfaces.ActionResourceMapping
import parker.core.interfaces.ActionVocabularyEntry
import parker.core.interfaces.AgentPolicy
import parker.core.interfaces.AgentRunId
import parker.core.interfaces.AgentRunStatus
import parker.core.interfaces.EventType
import parker.core.interfaces.PermissionAction
import parker.core.interfaces.PermissionDecisionOutcome
import parker.core.interfaces.PermissionLevel
import parker.core.interfaces.PlanningSessionId
import parker.core.interfaces.Principal
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.PrincipalStatus
import parker.core.interfaces.PrincipalType
import parker.core.interfaces.RequestOrigin
import parker.core.interfaces.RequestPriority
import parker.core.interfaces.Resource
import parker.core.interfaces.ResourceId
import parker.core.interfaces.ResourceLifecycleState
import parker.core.interfaces.ResourceSensitivity
import parker.core.interfaces.ResourceType
import parker.core.interfaces.TaskProposal
import parker.core.interfaces.TaskProposalDisposition
import parker.core.interfaces.TaskProposalId
import parker.core.interfaces.TaskStatus
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Controlled Agent Run Submission
 * (`docs/implementation/CONTROLLED_AGENT_RUN_SUBMISSION_SCOPE_LOCK.md`
 * Section 11, item 4): the one integration test spanning the full chain
 * neither `InMemoryTaskManagerRuntimeTest.kt` nor `InMemoryAgentRuntimeTest.kt`
 * exercises on its own -- `TaskProposal` -> Task creation -> `START`
 * authorisation -> `AgentRunCommandChannel.submit()` -> Agent Run creation
 * -> Task transition -> emitted audit events (Scope Lock Sections 5, 6, 13
 * item 7). Wires real `InMemoryTaskManagerRuntime` and `InMemoryAgentRuntime`
 * instances together -- not fakes on either side -- with the real production
 * `ActionVocabulary` entry, `PermissionPolicyRule`, and Agent Runtime
 * Execution Boundary `Resource` shape from Scope Lock Sections 3-4,
 * reproduced here (not imported from `ParkerRuntime.kt`, which keeps them
 * `private`) exactly as that composition root configures them.
 */
class TaskManagerAgentRunSubmissionIntegrationTest {

    private val boundaryResourceId = ResourceId("resource-agent-runtime-boundary")
    private val startVerbPhrase = "start agent run"
    private val systemPrincipalId = PrincipalId("system.parker")

    private fun proposal(
        taskProposalId: String = "proposal-1",
        ownerPrincipalId: String = "user-1",
        correlationId: String = "corr-1",
    ) = TaskProposal(
        taskProposalId = TaskProposalId(taskProposalId),
        planningSessionId = PlanningSessionId("session-1"),
        initiatingPrincipalId = PrincipalId(ownerPrincipalId),
        proposedOwnerPrincipalId = PrincipalId(ownerPrincipalId),
        goal = "read today's calendar",
        source = RequestOrigin.TEXT,
        priority = RequestPriority.NORMAL,
        correlationId = correlationId,
    )

    private fun owner(id: String = "user-1") = Principal(
        principalId = PrincipalId(id),
        principalType = PrincipalType.USER,
        displayName = "Test Owner",
        owner = null,
        status = PrincipalStatus.CREATED,
        createdAt = Instant.parse("2026-01-01T00:00:00Z"),
        lastSeenAt = Instant.parse("2026-01-01T00:00:00Z"),
    )

    private fun agentIdentity(taskId: String) = Principal(
        principalId = PrincipalId("agent-for-$taskId"),
        principalType = PrincipalType.INTERNAL_AGENT,
        displayName = "Test Agent",
        owner = PrincipalId("user-1"),
        status = PrincipalStatus.CREATED,
        createdAt = Instant.parse("2026-01-01T00:00:00Z"),
        lastSeenAt = Instant.parse("2026-01-01T00:00:00Z"),
    )

    /**
     * Builds the real production shape from Scope Lock Sections 3-4:
     * the Agent Runtime Execution Boundary Resource, the `start agent run`
     * ActionVocabulary entry mapping to `(EXECUTE, AGENT)`, and -- only when
     * [approveRunInitiation] is true -- the one `PermissionPolicyRule`
     * production authorises for it. When false, the policy has no rule for
     * `(EXECUTE, AGENT)` at all, reproducing "a policy configured to" not
     * approve (Scope Lock Section 11, item 4's second option), since the
     * real rule itself is not Principal-scoped (Section 3.3) and so cannot
     * be made to deny a specific Principal by construction alone.
     */
    private suspend fun buildRuntimes(approveRunInitiation: Boolean): Triple<InMemoryTaskManagerRuntime, InMemoryAgentRuntime, InMemoryEventBus> {
        val identityService = InMemoryIdentityService()
        // DefaultPermissionEngine.evaluate denies any Principal not ACTIVE (including CREATED,
        // register()'s own required initial status) as its first step -- both the owner
        // (requestingPrincipalId, the run-initiation check's own Principal) and the Agent
        // Identity must be activated, exactly mirroring ParkerRuntime.kt's own
        // registerActive/register-then-updateStatus convention, or every evaluate() call in
        // this test would deny on identity status alone, before this milestone's own policy
        // logic is ever reached.
        identityService.register(owner())
        identityService.updateStatus(PrincipalId("user-1"), PrincipalStatus.ACTIVE)
        val eventBus = InMemoryEventBus()

        val resourceRegistry = InMemoryResourceRegistry()
        val now = Instant.parse("2026-01-01T00:00:00Z")
        resourceRegistry.register(
            Resource(
                resourceId = boundaryResourceId,
                resourceType = ResourceType.AGENT,
                displayName = "Agent Runtime Execution Boundary",
                ownerPrincipalId = systemPrincipalId,
                sensitivity = ResourceSensitivity.PUBLIC,
                lifecycleState = ResourceLifecycleState.REGISTERED,
                createdAt = now,
                updatedAt = now,
                source = "composition-root:agent-runtime-boundary",
            ),
        )

        val vocabulary = InMemoryActionVocabulary()
        vocabulary.register(
            ActionVocabularyEntry(
                verbPhrase = startVerbPhrase,
                mappings = setOf(ActionResourceMapping(PermissionAction.EXECUTE, ResourceType.AGENT)),
            ),
        )
        val actionMapper = ActionMapper(vocabulary)

        val rules = if (approveRunInitiation) {
            listOf(
                PermissionPolicyRule(
                    action = PermissionAction.EXECUTE,
                    resourceType = ResourceType.AGENT,
                    outcome = PermissionDecisionOutcome.APPROVED,
                    level = PermissionLevel.AUTOMATIC,
                ),
            )
        } else {
            emptyList()
        }
        val permissionPolicy = DefaultPermissionPolicy(actionMapper, resourceRegistry, rules)
        val permissionEngine = DefaultPermissionEngine(identityService, permissionPolicy)

        val toolRegistry = InMemoryToolRegistry(resourceRegistry)
        val toolInvocationBinding = InMemoryToolInvocationBinding()
        val executionPipeline = DefaultExecutionPipeline(
            resourceRegistry,
            actionMapper,
            permissionEngine,
            toolRegistry,
            eventBus,
            toolInvocationBinding,
        )

        val agentRuntime = InMemoryAgentRuntime(
            identityService = identityService,
            executionPipeline = executionPipeline,
            eventBus = eventBus,
            agentStepSource = DeterministicAgentStepSource(),
            agentPolicy = AgentPolicy(maxAgentSteps = 10),
            permissionEngine = permissionEngine,
            runInitiationResourceId = boundaryResourceId,
            runInitiationVerbPhrase = startVerbPhrase,
        )
        // Two-Phase Acceptance/Execution Amendment (Scope Lock Amendment, A.2): the same
        // agentRuntime instance is passed for both the AgentRunCommandChannel and
        // AgentRunExecutionTrigger arguments -- InMemoryAgentRuntime implements both; no second
        // implementation exists or is composed here, mirroring ParkerRuntime.kt's own wiring.
        val taskManagerRuntime = InMemoryTaskManagerRuntime(identityService, eventBus, agentRuntime, agentRuntime)

        // Registered after both runtimes exist, exactly as InMemoryAgentRuntime.start() itself
        // requires the Agent Identity to resolve before proceeding past run-initiation approval
        // (Scope Lock Section 1, step 5) -- unaffected by whether run-initiation is approved or
        // denied in this fixture, since a denied run never reaches this check at all.
        identityService.register(agentIdentity("task-for-proposal-1"))
        identityService.updateStatus(PrincipalId("agent-for-task-for-proposal-1"), PrincipalStatus.ACTIVE)

        return Triple(taskManagerRuntime, agentRuntime, eventBus)
    }

    @Test
    fun `accepted flow -- TaskProposal through to a running, real Agent Run and task-agent_run_started`() = runTest {
        val (taskManagerRuntime, agentRuntime, eventBus) = buildRuntimes(approveRunInitiation = true)
        val publishedEventTypes = mutableListOf<String>()
        listOf(
            "task.created", "task.ready", "task.agent_run_started", "task.agent_run_rejected", "task.started", "task.completed",
            "agent.created", "agent.initialised", "agent.ready", "agent.started",
            "agent.step_started", "agent.action_proposed", "agent.permission_required",
            "agent.action_approved", "agent.action_denied", "agent.action_deferred",
            "agent.step_completed", "agent.completed", "agent.failed",
            "execution.request_received", "execution.started", "execution.completed", "execution.failed",
            "permission.requested", "permission.granted", "permission.denied",
        ).forEach { type ->
            eventBus.subscribe(EventType(type), PrincipalId("test-subscriber")) { publishedEventTypes += type }
        }

        val disposition = taskManagerRuntime.submitProposal(proposal())

        // Task creation -> Task QUEUED -> AgentRunCommand.START constructed -> run-initiation
        // permission evaluated -> Agent Identity resolved -> Agent Run created and accepted ->
        // task.agent_run_started published -> Task QUEUED to RUNNING -> Task Manager mutex
        // released -> execute(agentRunId) invoked -> runLoop() (Scope Lock Amendment, A.3).
        //
        // Two-Phase Acceptance/Execution Amendment (Scope Lock Amendment, A.6 item 3, as
        // corrected -- see that item's own provenance note): this fixture's Task goal
        // ("read today's calendar") has no matching ActionVocabulary entry -- only
        // "start agent run", the run-initiation verb phrase, is registered -- so the accepted
        // flow here is a genuine execution-failure path, terminating at agent.failed, not
        // agent.completed. This is this fixture's actual, pre-existing behaviour, unaffected by
        // this amendment; it is not extended to reach agent.completed (A.3 freezes both as
        // legitimate accepted-flow outcomes).
        val accepted = assertIs<TaskProposalDisposition.Accepted>(disposition)
        val task = taskManagerRuntime.getTask(accepted.taskId)
        assertNotNull(task)
        // task.completed is never reached on the agent.failed path (Unit B2, unchanged:
        // agent.failed performs no Task Status transition) -- the Task remains at RUNNING,
        // exactly where task.agent_run_started's own QUEUED -> RUNNING transition left it.
        assertEquals(TaskStatus.RUNNING, task.status)

        val commands = taskManagerRuntime.agentRunCommandsFor(accepted.taskId)
        assertEquals(1, commands.size)

        val agentRunId = AgentRunId("run-for-${accepted.taskId.value}")
        val agentRun = agentRuntime.getAgentRun(agentRunId)
        assertNotNull(agentRun)
        // The observed terminal event matches the fixture's configured execution behaviour
        // (A.6 item 3, as corrected).
        assertEquals(AgentRunStatus.FAILED, agentRun.status)

        // The complete frozen event ordering through execution (A.3), ending at the terminal
        // Agent lifecycle event this fixture actually produces. The accepted flow completing at
        // all -- this assertion running rather than the test hanging -- is itself the proof the
        // original self-deadlock (docs/architecture/
        // CONTROLLED_AGENT_RUN_SUBMISSION_DEADLOCK_DESIGN_RECONCILIATION.md §1) does not
        // reoccur (A.6 item 5's own reasoning).
        assertEquals(
            listOf(
                "task.created",
                "task.ready",
                "agent.created",
                "agent.initialised",
                "agent.ready",
                "task.agent_run_started",
                "agent.started",
                "agent.step_started",
                "agent.action_proposed",
                "agent.permission_required",
                "execution.request_received",
                "agent.step_completed",
                "agent.failed",
            ),
            publishedEventTypes,
        )
        // A.6 item 3's always-required orderings, restated as independent assertions so a future
        // reordering of the exact list above cannot silently drop them.
        val startedIndex = publishedEventTypes.indexOf("task.agent_run_started")
        assertTrue(startedIndex >= 0)
        assertTrue(startedIndex < publishedEventTypes.indexOf("agent.started"))
        assertTrue(startedIndex < publishedEventTypes.indexOf("agent.failed"))
    }

    @Test
    fun `rejected flow -- an unapproved run-initiation policy leaves the Task QUEUED and publishes task-agent_run_rejected`() = runTest {
        val (taskManagerRuntime, agentRuntime, eventBus) = buildRuntimes(approveRunInitiation = false)
        var rejectedPayload: Map<String, String>? = null
        eventBus.subscribe(EventType("task.agent_run_rejected"), PrincipalId("test-subscriber")) { event ->
            rejectedPayload = event.payload
        }

        val disposition = taskManagerRuntime.submitProposal(proposal())

        // Task creation -> Task QUEUED -> AgentRunCommand.START constructed -> run-initiation
        // permission denied -> no Agent Run created -> task.agent_run_rejected published -> Task
        // remains QUEUED -> TaskProposalDisposition.Accepted returned (Scope Lock Section 6).
        val accepted = assertIs<TaskProposalDisposition.Accepted>(disposition)
        assertEquals(TaskStatus.QUEUED, taskManagerRuntime.getTask(accepted.taskId)?.status)
        assertNull(agentRuntime.getAgentRun(AgentRunId("run-for-${accepted.taskId.value}")))
        assertNotNull(rejectedPayload)
        assertEquals("START", rejectedPayload?.get("commandType"))
        assertEquals(true, rejectedPayload?.get("reason")?.contains("DENIED"))
    }
}
