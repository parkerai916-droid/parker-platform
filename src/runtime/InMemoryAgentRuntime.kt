package parker.core.runtime

import java.time.Instant
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import parker.core.interfaces.AgentRun
import parker.core.interfaces.AgentRunCommand
import parker.core.interfaces.AgentRunCommandChannel
import parker.core.interfaces.AgentRunCommandResult
import parker.core.interfaces.AgentRunCommandType
import parker.core.interfaces.AgentRunId
import parker.core.interfaces.AgentRunLifecycleTransitions
import parker.core.interfaces.AgentRunStatus
import parker.core.interfaces.ExecutionPipeline
import parker.core.interfaces.ExecutionRequest
import parker.core.interfaces.ExecutionResultStatus
import parker.core.interfaces.IdentityService
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.PrincipalType
import parker.core.interfaces.RequestId
import parker.core.interfaces.RequestOrigin
import parker.core.interfaces.RequestPriority

/**
 * Sprint 1, Unit 7
 * (`docs/implementation/SPRINT_1_VERTICAL_SLICE_PLAN.md` §6): "Agent
 * Instance/Agent Run creation and lifecycle
 * (`CREATED -> INITIALISED -> READY -> RUNNING -> COMPLETED`); one Agent
 * Step constructing and submitting one `ExecutionRequest`."
 *
 * The first implementation of [AgentRunCommandChannel]
 * (`TaskManagerRuntimeSpecification.md` §16). Lives in `src/runtime/`, not
 * `tests/runtime/` -- an accepted design baseline, mirroring
 * [InMemoryTaskManagerRuntime]'s identical placement (Unit 6).
 *
 * Mirrors this codebase's established conventions: a single
 * [Mutex]-guarded in-memory store; a resolve-or-reject dependency call
 * ([IdentityService.resolve] for the Agent Identity, exactly as
 * [InMemoryTaskManagerRuntime] resolves a Task's owner); and every other
 * dependency injected as an *interface* ([IdentityService],
 * [ExecutionPipeline]) rather than a concrete class, per
 * [DefaultExecutionPipeline]'s own established pattern.
 *
 * ## Scope: what this does and does not do
 *
 * **Only `START` is implemented.** `SUSPEND`, `RESUME`, and `CANCEL` are
 * real, specified command types (`AgentRunCommand.kt`) this class
 * deliberately does not implement -- each returns
 * [AgentRunCommandResult.Rejected] with an explicit reason, never a
 * silent no-op or an exception. Implementing them is future work, not
 * this unit's (the Vertical Slice Plan's Unit 7 row describes only
 * Agent Run *creation* and one Agent Step).
 *
 * **Only `CREATED -> INITIALISED -> READY -> RUNNING -> {COMPLETED,
 * FAILED}` is driven.** [AgentRunLifecycleTransitions]
 * (`src/contracts/AgentRunLifecycle.kt`) is still the full, specified
 * 10-state machine -- this class simply never calls it for any edge
 * beyond the ones it needs. In particular, `WAITING_FOR_PERMISSION` and
 * `WAITING_FOR_INPUT` model an Agent Run *pausing* for something
 * asynchronous; since [ExecutionPipeline.submit] in this codebase is
 * synchronous and returns a complete `ExecutionResult` in the same call
 * (`DefaultExecutionPipeline.submit`'s own implementation), there is
 * nothing to pause for -- these two states legitimately do not apply to
 * this unit's flow, not omitted for convenience.
 *
 * **A non-`SUCCESS` `ExecutionResult` ends the Agent Run at `FAILED`.**
 * `AgentRuntimeSpecification.md` §10 ("Tool failure," "Permission
 * denial") names two legitimate outcomes for a failed Agent Step: end the
 * Agent Run, or continue via an alternate proposed action. This class
 * always does the former -- continuing via an alternate action requires
 * Planner-level retry logic this repository does not specify or
 * implement anywhere yet.
 *
 * **Publishes no Agent Events.** `agent.*` EventBus publication (§9) is
 * real, specified behaviour this unit does not implement -- that is
 * Unit 9's "Event assertion harness" territory.
 *
 * **Derives `AgentRunId` and the Agent Identity deterministically from
 * `taskId`**, exactly the same documented Sprint-1 placeholder pattern as
 * [InMemoryTaskManagerRuntime]'s `TaskId` derivation from `taskProposalId`
 * -- not a claim about the real allocation scheme, and, per that same
 * precedent, replaceable later without changing any public contract
 * (`AgentRunId`/`PrincipalId` are both opaque string wrappers).
 */
class InMemoryAgentRuntime(
    private val identityService: IdentityService,
    private val executionPipeline: ExecutionPipeline,
) : AgentRunCommandChannel {

    private val mutex = Mutex()
    private val agentRuns = mutableMapOf<AgentRunId, AgentRun>()

    override suspend fun submit(command: AgentRunCommand): AgentRunCommandResult = mutex.withLock {
        when (command.commandType) {
            AgentRunCommandType.START -> start(command)
            AgentRunCommandType.SUSPEND, AgentRunCommandType.RESUME, AgentRunCommandType.CANCEL ->
                AgentRunCommandResult.Rejected(
                    command.commandType,
                    "${command.commandType} is not implemented by this Sprint 1 runtime -- " +
                        "only START is supported (Unit 7 scope)",
                )
        }
    }

    private suspend fun start(command: AgentRunCommand): AgentRunCommandResult {
        val agentRunId = AgentRunId("run-for-${command.taskId.value}")
        check(agentRunId !in agentRuns) {
            "AgentRunCommand.START for taskId '${command.taskId.value}' has already been processed; " +
                "this deterministic runtime does not support starting a second Agent Run for the same Task"
        }
        val agentIdentityPrincipalId = PrincipalId("agent-for-${command.taskId.value}")

        // CREATED -- the Agent Instance record now exists.
        var run = AgentRun(
            agentRunId = agentRunId,
            agentIdentityPrincipalId = agentIdentityPrincipalId,
            taskId = command.taskId,
            status = AgentRunStatus.CREATED,
            goal = command.goalDescription,
            correlationId = command.correlationId,
        )
        agentRuns[agentRunId] = run

        val identity = identityService.resolve(agentIdentityPrincipalId)
            ?: return AgentRunCommandResult.Rejected(
                command.commandType,
                "agentIdentityPrincipalId '${agentIdentityPrincipalId.value}' does not resolve to a " +
                    "registered Principal -- per AgentRuntimeSpecification.md Section 7, an Agent " +
                    "Instance cannot proceed past CREATED",
            )
        if (identity.principalType != PrincipalType.INTERNAL_AGENT || identity.owner == null) {
            return AgentRunCommandResult.Rejected(
                command.commandType,
                "Principal '${agentIdentityPrincipalId.value}' is not a valid Agent Identity -- " +
                    "AgentRuntimeSpecification.md Section 7 requires principalType INTERNAL_AGENT " +
                    "with a non-null owner (Delegated Authority)",
            )
        }

        // INITIALISED -- Agent Identity resolved.
        run = advance(run, AgentRunStatus.INITIALISED)
        // READY -- Sprint 1 placeholder: no Agent Capability/Policy binding logic exists yet, so
        // this transition is unconditional rather than a real validation step.
        run = advance(run, AgentRunStatus.READY)
        // RUNNING -- begin the one Agent Step this unit models.
        run = advance(run, AgentRunStatus.RUNNING)

        val executionRequest = ExecutionRequest(
            requestId = RequestId("exec-for-${agentRunId.value}"),
            principalId = identity.principalId,
            origin = RequestOrigin.AGENT,
            intent = command.goalDescription,
            targetResources = command.resourceReferences,
            proposedActions = listOf(command.goalDescription),
            priority = RequestPriority.NORMAL,
            createdAt = Instant.now(),
            correlationId = command.correlationId,
        )
        val result = executionPipeline.submit(executionRequest)

        run = if (result.status == ExecutionResultStatus.SUCCESS) {
            advance(run, AgentRunStatus.COMPLETED)
        } else {
            advance(run, AgentRunStatus.FAILED)
        }

        return AgentRunCommandResult.Accepted(run.agentRunId, command.commandType)
    }

    private fun advance(run: AgentRun, next: AgentRunStatus): AgentRun {
        AgentRunLifecycleTransitions.requireValidTransition(run.status, next)
        val updated = run.copy(status = next)
        agentRuns[run.agentRunId] = updated
        return updated
    }

    /** The current state of [agentRunId], or `null` if no Agent Run with that ID exists. */
    suspend fun getAgentRun(agentRunId: AgentRunId): AgentRun? = mutex.withLock { agentRuns[agentRunId] }

    /** Every Agent Run this runtime has created so far, in no particular order. */
    suspend fun listAgentRuns(): List<AgentRun> = mutex.withLock { agentRuns.values.toList() }
}
