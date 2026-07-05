package parker.core.runtime

import java.time.Instant
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import parker.core.interfaces.AgentRunCommand
import parker.core.interfaces.AgentRunCommandType
import parker.core.interfaces.EventBus
import parker.core.interfaces.EventType
import parker.core.interfaces.IdentityService
import parker.core.interfaces.ParkerEvent
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.Task
import parker.core.interfaces.TaskId
import parker.core.interfaces.TaskLifecycleTransitions
import parker.core.interfaces.TaskProposal
import parker.core.interfaces.TaskProposalDisposition
import parker.core.interfaces.TaskProposalIntake
import parker.core.interfaces.TaskStatus

/**
 * Sprint 1, Unit 6
 * (`docs/implementation/SPRINT_1_VERTICAL_SLICE_PLAN.md` §6): "In-memory
 * Task store; accept-only intake of a `TaskProposal`; Task lifecycle
 * transitions `Created -> Queued`; Agent Run Request construction."
 *
 * The first implementation of [TaskProposalIntake]
 * (`TaskManagerRuntimeSpecification.md` §15) and, per that section, of the
 * construction (not submission) side of §16's Agent Run Command Channel.
 * Lives in `src/runtime/`, not `tests/runtime/`, per the plan's own
 * recommendation: "Task Manager Runtime is an accepted design baseline,
 * not a test-only stand-in" -- unlike Units 3-5's fixtures.
 *
 * Mirrors [InMemoryToolRegistry]'s and [InMemoryToolInvocationBinding]'s
 * existing conventions: a single [Mutex]-guarded in-memory store, and a
 * resolve-or-reject dependency call (here, [IdentityService.resolve] for
 * the proposed owner, mirroring how [InMemoryToolRegistry.register]
 * resolves a `resourceId` via `ResourceRegistry` before proceeding).
 *
 * ## Scope: what this does and does not do
 *
 * **Accept-only, for a resolvable owner.** Per the plan's own "accept-only
 * intake" phrase: every [TaskProposal] whose [TaskProposal.proposedOwnerPrincipalId]
 * resolves through [identityService] is [TaskProposalDisposition.Accepted]
 * -- unconditionally, since Sprint 1's vertical slice has exactly one
 * fixed, always-succeeds happy path (Unit 5's `DeterministicPlannerHarness`,
 * `tests/runtime/`) and no Plan Decision or acceptance-policy logic to
 * weigh against. This is
 * not a claim that a real Task Manager Runtime would always accept --
 * `Deferred`/`Split`/`Merged`, and *business-reason* `Rejected`, are real,
 * specified outcomes (`TaskManagerRuntimeSpecification.md` §15) this
 * minimal runtime does not implement. The one `Rejected` case this class
 * does implement -- an unresolvable owner -- is not a business decision;
 * it is the same "cannot proceed without a resolvable identity" rule
 * `PlannerRuntimeSpecification.md` §8 already states for a Planning
 * Session's initiating Principal, applied here to a Task's proposed
 * owner.
 *
 * **`CREATED -> QUEUED` only.** No later Task Status is driven by this
 * class. [TaskLifecycleTransitions] (`src/contracts/TaskLifecycle.kt`) is
 * still the full, specified 9-state machine -- this class simply never
 * calls it for any edge beyond the one it needs.
 *
 * **Constructs, but never submits, an [AgentRunCommand].** `START` is
 * built and stored so a caller (a future unit's test, or Unit 10's
 * end-to-end test) can observe it, but this class never calls
 * `AgentRunCommandChannel.submit` -- no implementation of that interface
 * exists yet (Unit 7).
 *
 * **Does not resolve the proposed assignee.** Only
 * [TaskProposal.proposedOwnerPrincipalId] is resolved through
 * [identityService], per the plan's own acceptance text ("`ownerPrincipalId`
 * resolved through the Identity Service"). [TaskProposal.proposedAssigneePrincipalId],
 * if present, is carried onto the created [Task] unresolved -- a
 * documented gap, not a silent omission.
 *
 * **Re-submission of an already-processed `taskProposalId` is caller
 * misuse, not a disposition.** `TaskProposalDisposition` is documented
 * elsewhere as terminal per proposal ("reconsideration means a new
 * submission, not a mutation of this result"); this class enforces that
 * by throwing rather than inventing a sixth outcome or silently
 * overwriting the original Task.
 *
 * ## Sprint 1, Unit 9 (Runtime Lifecycle Event Publication)
 *
 * Publishes `task.created` and `task.ready` -- the two real Task Status
 * transitions this class performs (`TaskManagerRuntimeSpecification.md`
 * §10). Not published: `task.agent_run_started` -- its trigger is "an
 * Agent Run is created ... on behalf of this Task," and this class only
 * *constructs* an [AgentRunCommand] value object, never submits it to an
 * [parker.core.interfaces.AgentRunCommandChannel]; no Agent Run exists yet
 * for this method to truthfully report. Also not published: any event for
 * the unresolvable-owner `Rejected` path -- no [Task] record is ever
 * created there, so there is no `taskId` to correlate an event against.
 *
 * `publisherPrincipalId` is [TASK_MANAGER_RUNTIME_PRINCIPAL_ID], the same
 * kind of hardcoded Sprint 1 placeholder as
 * [DeterministicPlannerHarness]'s `PLANNER_RUNTIME_PRINCIPAL_ID`, for "the
 * identity the Task Manager Runtime itself operates under" (§10) -- not a
 * claim about a real allocation scheme. `correlationId` is
 * `proposal.correlationId`, the same shared, already-threaded value Units
 * 5-7 use throughout (see [DeterministicPlannerHarness]'s own "Unit 9"
 * KDoc for the full rationale), not `taskId` (§10's general convention).
 *
 * ## Sprint 2, Track B, Unit B1 (Agent-Event Subscription)
 *
 * Closes the subscription/recording half of `IMPLEMENTATION_GAPS.md` #42:
 * `TaskManagerRuntimeSpecification.md` §6 already specifies that "the Task
 * Manager Runtime subscribes to (or is otherwise informed of) relevant
 * Agent Events... for Agent Runs it has a recorded Agent Run Reference
 * for." This constructor subscribes to exactly the two of the five
 * §6-named event types any production code currently emits --
 * `agent.completed` and `agent.failed` (`InMemoryAgentRuntime`'s own class
 * KDoc: it "only ever drives `CREATED -> INITIALISED -> READY -> RUNNING
 * -> {COMPLETED, FAILED}`"). `agent.cancelled`, `agent.action_denied`, and
 * `agent.action_deferred` have no production emitter today and remain out
 * of this unit's scope, per gap #42's own text.
 *
 * **Correlation is by `taskId`, read from each event's own payload, not a
 * separate Agent Run Reference field.** `AgentRunCommand.agentRunId` is
 * always `null` for `START` (the Agent Run does not exist yet at
 * command-construction time -- see `AgentRunCommand.kt`'s own invariant),
 * so this class has no Agent Run identifier to record ahead of time.
 * `InMemoryAgentRuntime.publish` already carries `"taskId" to
 * run.taskId.value` in every `agent.*` event's payload; reading that back
 * is the only correlation mechanism already available end-to-end, and it
 * is exactly what `SPRINT_2_IMPLEMENTATION_PLAN.md`'s own Unit B1
 * acceptance criterion names ("recording the event against the correct
 * Task (by `taskId`)").
 *
 * **Receipt and recording only -- no Task Status transition.** An
 * incoming event's `taskId` is looked up only to decide whether to record
 * it (see [agentEventsFor]). Recording itself never calls
 * [TaskLifecycleTransitions] or mutates a stored [Task]'s `status` --
 * **Sprint 2, Track B, Unit B2 update:** `agent.completed` now *also*
 * drives a Task Status transition, in addition to being recorded; see
 * this class's own "Unit B2" KDoc section below for exactly what that
 * transition is and is not. `agent.failed` still never mutates `status`,
 * per §6's own "Agent Events may inform Task state, but do not
 * automatically control it" and per Unit B2's own scope.
 *
 * **Unknown or unaddressed events are ignored, not errors.** An
 * `agent.completed`/`agent.failed` event with no `taskId` payload entry,
 * or naming a `taskId` this runtime never created, is silently dropped --
 * consistent with `EventHandler.md`'s fire-and-forget delivery model and
 * with this unit's own "subscription and recording only" scope; it is not
 * a caller error and never throws.
 *
 * **Subscribes exactly once per event type, at construction.** Each of
 * the two `EventBus.subscribe` calls below happens exactly once, in the
 * constructor -- never per-Task, per-proposal, or per-event -- so a single
 * `InMemoryTaskManagerRuntime` instance never creates more than one
 * `Subscription` per event type on its [eventBus].
 *
 * ## Sprint 2, Track B, Unit B2 (Task Status Transitions)
 *
 * Implements the fixed, minimal rule
 * `docs/implementation/SPRINT_2_B2_IMPLEMENTATION_DECISIONS.md` settles,
 * for a Task with exactly one Agent Run Reference (this runtime's only
 * scope so far): on `agent.completed`, a Task at `QUEUED` moves through
 * both already-valid edges in sequence -- `QUEUED -> RUNNING`, then
 * `RUNNING -> COMPLETED` -- publishing `task.started` then `task.completed`
 * (`TaskManagerRuntimeSpecification.md` §10); a Task already `RUNNING`
 * takes only the second edge; a Task already `COMPLETED` is left
 * unmutated. `TaskLifecycleTransitions` has no direct `QUEUED ->
 * COMPLETED` edge, which is why both steps are required -- no new state
 * or edge is introduced (`SPRINT_2_B2_IMPLEMENTATION_DECISIONS.md` items 1
 * and 4). `agent.failed` continues to perform no transition at all
 * (`SPRINT_2_B2_IMPLEMENTATION_DECISIONS.md` item 2): the event is
 * recorded exactly as Unit B1 already did, and nothing else happens.
 *
 * No other starting `TaskStatus` is acted on by `agent.completed` --
 * `CREATED`, `FAILED`, `CANCELLED`, `PAUSED`, `EXPIRED`, and `SUPERSEDED`
 * are all currently unreachable for any Task this runtime creates (no
 * code path drives any of them yet), so attempting a transition for one
 * of them would be inventing behaviour for a case no specification or
 * decision document defines, not implementing an already-specified edge.
 * [applyCompletedTransition] treats all of them, and `COMPLETED`, as a
 * no-op.
 */
class InMemoryTaskManagerRuntime(
    private val identityService: IdentityService,
    private val eventBus: EventBus,
) : TaskProposalIntake {

    private companion object {
        /** Sprint 1 placeholder -- see this class's own KDoc, "Unit 9" section. */
        val TASK_MANAGER_RUNTIME_PRINCIPAL_ID = PrincipalId("system.task-manager-runtime")
    }

    private val mutex = Mutex()
    private val tasks = mutableMapOf<TaskId, Task>()
    private val agentRunCommands = mutableMapOf<TaskId, MutableList<AgentRunCommand>>()

    /** Sprint 2, Track B, Unit B1: `agent.completed`/`agent.failed` events recorded per Task -- see [agentEventsFor]. */
    private val agentEvents = mutableMapOf<TaskId, MutableList<ParkerEvent>>()

    init {
        // Sprint 2, Track B, Unit B1/B2: one subscription per event type, exactly once, at
        // construction -- see this class's own "Unit B1"/"Unit B2" KDoc sections above.
        // agent.completed both records the event (Unit B1) and drives the Task Status
        // transition (Unit B2); agent.failed only records -- these are deliberately two
        // separate subscribe calls, not a shared loop, because the two event types are no
        // longer handled identically.
        eventBus.subscribe(EventType("agent.completed"), TASK_MANAGER_RUNTIME_PRINCIPAL_ID) { event ->
            recordAgentEvent(event)
            applyCompletedTransition(event)
        }
        eventBus.subscribe(EventType("agent.failed"), TASK_MANAGER_RUNTIME_PRINCIPAL_ID) { event ->
            recordAgentEvent(event)
        }
    }

    override suspend fun submitProposal(proposal: TaskProposal): TaskProposalDisposition = mutex.withLock {
        val taskId = TaskId("task-for-${proposal.taskProposalId.value}")
        check(taskId !in tasks) {
            "TaskProposal '${proposal.taskProposalId.value}' has already been submitted; " +
                "reconsideration requires a new TaskProposal, not resubmitting this one"
        }

        val owner = identityService.resolve(proposal.proposedOwnerPrincipalId)
            ?: return@withLock TaskProposalDisposition.Rejected(
                proposal.taskProposalId,
                "proposedOwnerPrincipalId '${proposal.proposedOwnerPrincipalId.value}' does not resolve " +
                    "to a registered Principal",
            )

        // Created -- the Task Manager Task record now exists.
        val created = Task(
            taskId = taskId,
            ownerPrincipalId = owner.principalId,
            assigneePrincipalId = proposal.proposedAssigneePrincipalId,
            status = TaskStatus.CREATED,
            source = proposal.source,
            goal = proposal.goal,
            priority = proposal.priority,
            correlationId = proposal.correlationId,
            originatingTaskProposalId = proposal.taskProposalId,
        )
        tasks[taskId] = created
        publish(
            eventType = "task.created",
            taskId = taskId,
            correlationId = proposal.correlationId,
            payload = mapOf("ownerPrincipalId" to owner.principalId.value, "source" to proposal.source.name),
        )

        // Created -> Queued: accept-only, Sprint 1's fixed happy path.
        TaskLifecycleTransitions.requireValidTransition(TaskStatus.CREATED, TaskStatus.QUEUED)
        tasks[taskId] = created.copy(status = TaskStatus.QUEUED)
        publish(eventType = "task.ready", taskId = taskId, correlationId = proposal.correlationId)

        // Construct (do not submit) exactly one AgentRunCommand.
        val command = AgentRunCommand(
            commandType = AgentRunCommandType.START,
            taskId = taskId,
            requestingPrincipalId = owner.principalId,
            targetAgentCapability = proposal.requiredCapabilities,
            goalDescription = proposal.goal,
            contextReferences = proposal.contextReferences,
            // Sprint 1, Unit 11B: propagate the Task Proposal's own resourceReferences
            // (Unit 11B addition to TaskProposal.kt) forward unchanged -- this is the one
            // missing link that previously left AgentRunCommand.resourceReferences always
            // empty in a real (non-hand-built) chain. See TaskProposal.kt's own KDoc for
            // this field's provenance.
            resourceReferences = proposal.resourceReferences,
            correlationId = proposal.correlationId,
        )
        agentRunCommands.getOrPut(taskId) { mutableListOf() }.add(command)

        TaskProposalDisposition.Accepted(proposal.taskProposalId, taskId)
    }

    /** The current state of [taskId], or `null` if no Task with that ID exists. */
    suspend fun getTask(taskId: TaskId): Task? = mutex.withLock { tasks[taskId] }

    /** Every Task this runtime has created so far, in no particular order. */
    suspend fun listTasks(): List<Task> = mutex.withLock { tasks.values.toList() }

    /** Every [AgentRunCommand] constructed for [taskId] so far -- empty if none, never `null`. */
    suspend fun agentRunCommandsFor(taskId: TaskId): List<AgentRunCommand> = mutex.withLock {
        agentRunCommands[taskId]?.toList() ?: emptyList()
    }

    /**
     * Sprint 2, Track B, Unit B1: every `agent.completed`/`agent.failed`
     * [ParkerEvent] recorded against [taskId] so far -- empty if none, never
     * `null`. Recording only; does not reflect or imply any [TaskStatus]
     * change -- see this class's own "Unit B1" KDoc section.
     */
    suspend fun agentEventsFor(taskId: TaskId): List<ParkerEvent> = mutex.withLock {
        agentEvents[taskId]?.toList() ?: emptyList()
    }

    /**
     * Sprint 2, Track B, Unit B1: registered for both `agent.completed` and
     * `agent.failed` in `init`. Records [event] against the Task named in
     * its own `taskId` payload entry, or does nothing if that entry is
     * absent or names a Task this runtime never created -- see this class's
     * own "Unit B1" KDoc section for why this is not an error.
     */
    private suspend fun recordAgentEvent(event: ParkerEvent) {
        val taskIdValue = event.payload["taskId"] ?: return
        val taskId = TaskId(taskIdValue)
        mutex.withLock {
            if (taskId !in tasks) return@withLock
            agentEvents.getOrPut(taskId) { mutableListOf() }.add(event)
        }
    }

    /**
     * Sprint 2, Track B, Unit B2: the handler registered for
     * `agent.completed` in `init`, applied in addition to (not instead of)
     * [recordAgentEvent]. Implements
     * `docs/implementation/SPRINT_2_B2_IMPLEMENTATION_DECISIONS.md`'s fixed
     * rule exactly -- see this class's own "Unit B2" KDoc section for the
     * full rationale. A missing `taskId` payload entry, or one naming a
     * Task this runtime never created, is ignored the same way
     * [recordAgentEvent] ignores it -- no exception, no new Task, no
     * mutation.
     */
    private suspend fun applyCompletedTransition(event: ParkerEvent) {
        val taskIdValue = event.payload["taskId"] ?: return
        val taskId = TaskId(taskIdValue)
        mutex.withLock {
            val task = tasks[taskId] ?: return@withLock
            when (task.status) {
                TaskStatus.QUEUED -> {
                    TaskLifecycleTransitions.requireValidTransition(TaskStatus.QUEUED, TaskStatus.RUNNING)
                    val running = task.copy(status = TaskStatus.RUNNING)
                    tasks[taskId] = running
                    publish(eventType = "task.started", taskId = taskId, correlationId = task.correlationId)

                    TaskLifecycleTransitions.requireValidTransition(TaskStatus.RUNNING, TaskStatus.COMPLETED)
                    tasks[taskId] = running.copy(status = TaskStatus.COMPLETED)
                    publish(eventType = "task.completed", taskId = taskId, correlationId = task.correlationId)
                }
                TaskStatus.RUNNING -> {
                    TaskLifecycleTransitions.requireValidTransition(TaskStatus.RUNNING, TaskStatus.COMPLETED)
                    tasks[taskId] = task.copy(status = TaskStatus.COMPLETED)
                    publish(eventType = "task.completed", taskId = taskId, correlationId = task.correlationId)
                }
                else -> Unit // COMPLETED (already terminal) and every other currently-unreachable
                // status -- see this class's own "Unit B2" KDoc section for why no transition
                // is attempted for any of them.
            }
        }
    }

    /** Sprint 1, Unit 9: publishes one `task.*` [ParkerEvent] -- see this class's own "Unit 9" KDoc. */
    private suspend fun publish(eventType: String, taskId: TaskId, correlationId: String, payload: Map<String, String> = emptyMap()) {
        eventBus.publish(
            ParkerEvent(
                eventId = "evt-${taskId.value}-$eventType",
                publisherPrincipalId = TASK_MANAGER_RUNTIME_PRINCIPAL_ID,
                eventType = EventType(eventType),
                timestamp = Instant.now(),
                correlationId = correlationId,
                payload = payload,
            ),
        )
    }
}
