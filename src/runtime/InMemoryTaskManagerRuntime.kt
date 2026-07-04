package parker.core.runtime

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import parker.core.interfaces.AgentRunCommand
import parker.core.interfaces.AgentRunCommandType
import parker.core.interfaces.IdentityService
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
 */
class InMemoryTaskManagerRuntime(
    private val identityService: IdentityService,
) : TaskProposalIntake {

    private val mutex = Mutex()
    private val tasks = mutableMapOf<TaskId, Task>()
    private val agentRunCommands = mutableMapOf<TaskId, MutableList<AgentRunCommand>>()

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

        // Created -> Queued: accept-only, Sprint 1's fixed happy path.
        TaskLifecycleTransitions.requireValidTransition(TaskStatus.CREATED, TaskStatus.QUEUED)
        tasks[taskId] = created.copy(status = TaskStatus.QUEUED)

        // Construct (do not submit) exactly one AgentRunCommand.
        val command = AgentRunCommand(
            commandType = AgentRunCommandType.START,
            taskId = taskId,
            requestingPrincipalId = owner.principalId,
            targetAgentCapability = proposal.requiredCapabilities,
            goalDescription = proposal.goal,
            contextReferences = proposal.contextReferences,
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
}
