package parker.core.runtime

import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import parker.core.interfaces.AgentPolicy
import parker.core.interfaces.AgentRun
import parker.core.interfaces.AgentRunCommand
import parker.core.interfaces.AgentRunCommandChannel
import parker.core.interfaces.AgentRunCommandResult
import parker.core.interfaces.AgentRunCommandType
import parker.core.interfaces.AgentRunId
import parker.core.interfaces.AgentRunLifecycleTransitions
import parker.core.interfaces.AgentRunStatus
import parker.core.interfaces.AgentStepContext
import parker.core.interfaces.AgentStepDecision
import parker.core.interfaces.AgentStepSource
import parker.core.interfaces.EventBus
import parker.core.interfaces.EventType
import parker.core.interfaces.ExecutionPipeline
import parker.core.interfaces.ExecutionRequest
import parker.core.interfaces.ExecutionResult
import parker.core.interfaces.ExecutionResultStatus
import parker.core.interfaces.IdentityService
import parker.core.interfaces.ParkerEvent
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.PrincipalType
import parker.core.interfaces.RequestId
import parker.core.interfaces.RequestOrigin
import parker.core.interfaces.RequestPriority
import parker.core.interfaces.ResourceId

/**
 * Sprint 1, Unit 7 origin
 * (`docs/implementation/SPRINT_1_VERTICAL_SLICE_PLAN.md` §6), rewritten in
 * full by **Sprint 3, Track C, Unit C2** to implement the multi-step Agent
 * Run model specified by
 * `docs/architecture/MULTI_STEP_AGENT_RUN_DESIGN.md` (design accepted,
 * committed, tagged as Unit C1). This class remains the only
 * implementation of [AgentRunCommandChannel] in this repository and still
 * lives in `src/runtime/`, not `tests/runtime/` -- an accepted design
 * baseline, exactly as before.
 *
 * ## What changed from Unit 7 (see the design document for the full
 * rationale -- this KDoc only summarises)
 *
 * - **Multi-step, not one-step.** A `START`ed Agent Run now repeatedly
 *   consults an injected [AgentStepSource] (design document §4.2) for
 *   `AgentStepDecision.Propose`/`Complete`/`Fail`, submitting one
 *   [ExecutionRequest] per `Propose` via [executionPipeline], looping back
 *   to `RUNNING` after every successful step, until `Complete`, `Fail`, a
 *   non-`SUCCESS`/`PARTIAL_SUCCESS` [ExecutionResult], [AgentPolicy]'s
 *   `maxAgentSteps` bound, or an external `SUSPEND`/`CANCEL` ends the loop
 *   (design document §5.1, §6).
 * - **`SUSPEND`, `RESUME`, `CANCEL` are now implemented** -- see
 *   [suspendRun], [resumeRun], [cancelRun] below. `SUSPEND` is deferred to
 *   the next step boundary (design document §6.3); `CANCEL` applies
 *   immediately from any non-terminal state, followed by a best-effort
 *   [ExecutionPipeline.cancel] of any in-flight step (design document
 *   §6.5).
 * - **`DENIED` and `DEFERRED` are now distinct**, not both collapsed into
 *   `FAILED`: `DENIED` still ends the Agent Run at `FAILED` (a proposed
 *   action that is not permitted is not, by itself, evidence the Agent Run
 *   can recover from); `DEFERRED` ends the current step at `SUSPENDED`,
 *   consistent with `AgentRuntimeSpecification.md` §10's own treatment of
 *   a recoverable pause (design document §6.4).
 * - **`AgentPolicy.maxAgentSteps` is enforced**: reaching it after a
 *   successful step transitions `RUNNING -> SUSPENDED`, never `FAILED`
 *   (design document §7; `AgentPolicy`'s own KDoc). `maxAgentRunDuration`
 *   is accepted but deliberately not enforced -- design document §11,
 *   "Unit C2 will NOT implement."
 * - **Locking model redesigned** (design document §8): [mutex] now guards
 *   only short, synchronous map reads/writes on [agentRuns] and
 *   [runStates] -- it is never held while [executionPipeline].`submit` (a
 *   suspend call that may take arbitrarily long) is in flight. This is
 *   what allows independent Agent Runs to progress independently, and
 *   what allows a `SUSPEND`/`CANCEL` command for one Agent Run to be
 *   accepted and recorded while a different (or the same) Agent Run's
 *   step is still executing.
 *
 * ## What is still deliberately not implemented (design document §11,
 * "Unit C2 will NOT implement" -- unchanged from the design)
 *
 * No Planner (`agentStepSource` is supplied by the caller; this class
 * assumes nothing about what implements it -- AD-010). No
 * `WAITING_FOR_INPUT` -- this class never transitions to it, and
 * [AgentStepDecision] has no variant that would request it (see that
 * sealed class's own forward-compatibility note). No Agent Capability
 * system. No enforcement of `maxAgentRunDuration`. No World Model or
 * Memory integration -- [AgentStepContext] carries only what this
 * runtime itself accumulates. No Workflow Engine or cross-Agent
 * orchestration -- Agent Runs remain fully independent of one another.
 *
 * [AgentRunLifecycleTransitions] itself is unchanged: still the full,
 * specified 10-state machine, transcribed once in
 * `src/contracts/AgentRunLifecycle.kt`. This class now drives more of it
 * (`WAITING_FOR_PERMISSION`, `SUSPENDED`, and cancellation from more
 * states) than Unit 7 did, but adds no new state or edge.
 */
class InMemoryAgentRuntime(
    private val identityService: IdentityService,
    private val executionPipeline: ExecutionPipeline,
    private val eventBus: EventBus,
    private val agentStepSource: AgentStepSource,
    private val agentPolicy: AgentPolicy,
) : AgentRunCommandChannel {

    /**
     * Per-Agent-Run accumulated state that is not part of the [AgentRun]
     * record itself (design document §6.2, §8): the running step count,
     * the most recent [ExecutionResult] (for the next [AgentStepContext]),
     * accumulated resource references and denied actions (mirroring
     * [AgentStepContext]'s own KDoc field-by-field), a pending-`SUSPEND`
     * flag (design document §6.3 -- `SUSPEND` sets this rather than
     * mutating [AgentRunStatus] immediately), and the [RequestId] of
     * whatever step is currently in flight, if any (so [cancelRun] can
     * make a best-effort [ExecutionPipeline.cancel] call).
     */
    private class RunState {
        var successfulSteps: Int = 0
        var priorResult: ExecutionResult? = null
        val resourceReferences: MutableList<ResourceId> = mutableListOf()
        val deniedActions: MutableList<String> = mutableListOf()
        var pendingSuspend: Boolean = false
        var inFlightRequestId: RequestId? = null
    }

    private val mutex = Mutex()
    private val agentRuns = mutableMapOf<AgentRunId, AgentRun>()
    private val runStates = mutableMapOf<AgentRunId, RunState>()

    override suspend fun submit(command: AgentRunCommand): AgentRunCommandResult = when (command.commandType) {
        AgentRunCommandType.START -> start(command)
        AgentRunCommandType.SUSPEND -> suspendRun(command)
        AgentRunCommandType.RESUME -> resumeRun(command)
        AgentRunCommandType.CANCEL -> cancelRun(command)
    }

    // --- START --------------------------------------------------------

    private suspend fun start(command: AgentRunCommand): AgentRunCommandResult {
        // Deterministic, parent-derived ID: exactly one Agent Run per Task, by construction, for
        // the current platform phase -- a deliberate, documented decision
        // (docs/architecture/IMPLEMENTATION_GAPS.md #48, docs/architecture/PRE_MODULE_ID_MULTIPLICITY_DECISION.md),
        // not an accidental consequence of this ID scheme. TaskManagerRuntimeSpecification.md's own
        // "zero, one, or many Agent Run References" language is deliberately not narrowed by this
        // decision -- only this implementation's current behaviour is constrained.
        val agentRunId = AgentRunId("run-for-${command.taskId.value}")
        val agentIdentityPrincipalId = PrincipalId("agent-for-${command.taskId.value}")

        // CREATED -- the Agent Instance record now exists. Check-and-insert happens atomically
        // under one lock acquisition so two concurrent STARTs for the same taskId cannot both pass
        // the check before either inserts.
        var run = AgentRun(
            agentRunId = agentRunId,
            agentIdentityPrincipalId = agentIdentityPrincipalId,
            taskId = command.taskId,
            status = AgentRunStatus.CREATED,
            goal = command.goalDescription,
            correlationId = command.correlationId,
        )
        mutex.withLock {
            check(agentRunId !in agentRuns) {
                "AgentRunCommand.START for taskId '${command.taskId.value}' has already been processed; " +
                    "this runtime supports exactly one Agent Run per Task for the current platform phase -- " +
                    "a deliberate, documented constraint (docs/architecture/IMPLEMENTATION_GAPS.md #48, " +
                    "docs/architecture/PRE_MODULE_ID_MULTIPLICITY_DECISION.md), not an accidental limitation"
            }
            agentRuns[agentRunId] = run
        }
        publish(run, "agent.created")

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
        run = advanceInitial(run, AgentRunStatus.INITIALISED)
        publish(run, "agent.initialised")
        // READY -- Sprint 1 placeholder: no Agent Capability/Policy binding logic exists yet, so
        // this transition is unconditional rather than a real validation step.
        run = advanceInitial(run, AgentRunStatus.READY)
        publish(run, "agent.ready")
        // RUNNING -- begin the multi-step loop.
        run = advanceInitial(run, AgentRunStatus.RUNNING)
        publish(run, "agent.started")

        mutex.withLock { runStates[agentRunId] = RunState() }

        val finalRun = runLoop(run, identity.principalId, command.resourceReferences, startingStepNumber = 1)
        return AgentRunCommandResult.Accepted(finalRun.agentRunId, command.commandType)
    }

    /**
     * Unconditional transition used only during `START`'s
     * `CREATED -> INITIALISED -> READY -> RUNNING` setup, before this
     * Agent Run's [RunState] exists and before any `SUSPEND`/`RESUME`/
     * `CANCEL` command could legally target it (every such command
     * requires an already-known `agentRunId` -- `AgentRunCommand`'s own
     * `init` block). From `RUNNING` onward, every transition instead goes
     * through [tryAdvance], which is safe against a concurrent `CANCEL`.
     */
    private suspend fun advanceInitial(run: AgentRun, next: AgentRunStatus): AgentRun {
        AgentRunLifecycleTransitions.requireValidTransition(run.status, next)
        val updated = run.copy(status = next)
        mutex.withLock { agentRuns[run.agentRunId] = updated }
        return updated
    }

    /**
     * Race-safe transition (design document §8): applies `from -> to` only
     * if this Agent Run's stored status is still exactly `from` at the
     * moment the lock is held. Returns the updated [AgentRun], or `null`
     * if the stored status had already moved on -- the only way that
     * happens is a concurrent `CANCEL` (the one command that can apply
     * from any non-terminal state at any time). Callers that receive
     * `null` stop their own loop and defer entirely to whatever the
     * concurrent command already recorded.
     */
    private suspend fun tryAdvance(agentRunId: AgentRunId, from: AgentRunStatus, to: AgentRunStatus): AgentRun? =
        mutex.withLock {
            val current = agentRuns[agentRunId] ?: return@withLock null
            if (current.status != from) return@withLock null
            AgentRunLifecycleTransitions.requireValidTransition(from, to)
            val updated = current.copy(status = to)
            agentRuns[agentRunId] = updated
            updated
        }

    // --- the multi-step loop (design document Section 5.1, Section 6) -

    /**
     * Runs Agent Steps sequentially until this Agent Run reaches a
     * terminal state (`COMPLETED`/`FAILED`/`CANCELLED`) or `SUSPENDED`.
     * Used by both `START` (from step 1) and `RESUME` (continuing with
     * the next step number after whatever was already taken). Per the
     * design document's Section 8 locking model, [mutex] is only ever
     * held for the short map reads/writes inside [tryAdvance] and the
     * small state-bookkeeping blocks below -- never across the
     * [executionPipeline] `.submit` call itself.
     */
    private suspend fun runLoop(
        initialRun: AgentRun,
        agentPrincipalId: PrincipalId,
        initialResourceReferences: List<ResourceId>,
        startingStepNumber: Int,
    ): AgentRun {
        var run = initialRun
        val state = mutex.withLock { runStates.getOrPut(run.agentRunId) { RunState() } }
        if (state.resourceReferences.isEmpty() && initialResourceReferences.isNotEmpty()) {
            mutex.withLock { state.resourceReferences += initialResourceReferences }
        }
        var stepNumber = startingStepNumber

        while (true) {
            // A pending SUSPEND (design document Section 6.3) is honoured at the next step
            // boundary, before any further Agent Step is attempted.
            val pendingSuspend = mutex.withLock { state.pendingSuspend }
            if (pendingSuspend) {
                val suspended = tryAdvance(run.agentRunId, AgentRunStatus.RUNNING, AgentRunStatus.SUSPENDED)
                if (suspended == null) return getRun(run.agentRunId) ?: run
                mutex.withLock { state.pendingSuspend = false }
                publish(suspended, "agent.suspended")
                return suspended
            }

            // AgentPolicy.maxAgentSteps (design document Section 7): reaching the bound after a
            // successful step ends this attempt at SUSPENDED, never FAILED, and never silently
            // continues past the configured limit.
            if (state.successfulSteps >= agentPolicy.maxAgentSteps) {
                val suspended = tryAdvance(run.agentRunId, AgentRunStatus.RUNNING, AgentRunStatus.SUSPENDED)
                if (suspended == null) return getRun(run.agentRunId) ?: run
                publish(suspended, "agent.suspended")
                return suspended
            }

            publish(run, "agent.step_started")

            val context = mutex.withLock {
                AgentStepContext(
                    agentRunId = run.agentRunId,
                    taskId = run.taskId,
                    goal = run.goal,
                    stepNumber = stepNumber,
                    priorResult = state.priorResult,
                    resourceReferences = state.resourceReferences.toList(),
                    deniedActions = state.deniedActions.toList(),
                )
            }
            val decision = agentStepSource.nextStep(context)

            when (decision) {
                is AgentStepDecision.Complete -> {
                    if (state.successfulSteps < 1) {
                        // design document Section 5.1, item 4: a Complete claim before any
                        // successful Agent Step is treated as Fail, not silently honoured.
                        val failed = tryAdvance(run.agentRunId, AgentRunStatus.RUNNING, AgentRunStatus.FAILED)
                        if (failed == null) return getRun(run.agentRunId) ?: run
                        publish(failed, "agent.failed")
                        return failed
                    }
                    val completed = tryAdvance(run.agentRunId, AgentRunStatus.RUNNING, AgentRunStatus.COMPLETED)
                    if (completed == null) return getRun(run.agentRunId) ?: run
                    publish(completed, "agent.completed")
                    return completed
                }

                is AgentStepDecision.Fail -> {
                    val failed = tryAdvance(run.agentRunId, AgentRunStatus.RUNNING, AgentRunStatus.FAILED)
                    if (failed == null) return getRun(run.agentRunId) ?: run
                    publish(failed, "agent.failed")
                    return failed
                }

                is AgentStepDecision.Propose -> {
                    publish(run, "agent.action_proposed")
                    val waiting = tryAdvance(run.agentRunId, AgentRunStatus.RUNNING, AgentRunStatus.WAITING_FOR_PERMISSION)
                    if (waiting == null) return getRun(run.agentRunId) ?: run
                    publish(waiting, "agent.permission_required")

                    val requestId = RequestId("exec-for-${run.agentRunId.value}-step-$stepNumber")
                    val executionRequest = ExecutionRequest(
                        requestId = requestId,
                        principalId = agentPrincipalId,
                        origin = RequestOrigin.AGENT,
                        intent = decision.proposedAction,
                        targetResources = decision.targetResources,
                        proposedActions = listOf(decision.proposedAction),
                        priority = RequestPriority.NORMAL,
                        createdAt = Instant.now(),
                        correlationId = run.correlationId,
                    )
                    mutex.withLock { state.inFlightRequestId = requestId }

                    // The one call in this loop that may suspend for an unbounded time -- deliberately
                    // made with the mutex released (design document Section 8). A concurrent SUSPEND
                    // or CANCEL for this or any other Agent Run can be accepted and recorded while this
                    // call is in flight.
                    val result = executionPipeline.submit(executionRequest)

                    mutex.withLock { state.inFlightRequestId = null }

                    // If this Agent Run was CANCELLED while the step was in flight, the CANCEL has
                    // already applied and published agent.cancelled (design document Section 6.5); the
                    // ExecutionResult that just arrived has no further effect on the Agent Run.
                    val currentStatus = mutex.withLock { agentRuns[run.agentRunId]?.status }
                    if (currentStatus != AgentRunStatus.WAITING_FOR_PERMISSION) {
                        return getRun(run.agentRunId) ?: run
                    }
                    mutex.withLock { state.priorResult = result }

                    when (result.status) {
                        ExecutionResultStatus.SUCCESS, ExecutionResultStatus.PARTIAL_SUCCESS -> {
                            mutex.withLock {
                                state.successfulSteps += 1
                                state.resourceReferences += decision.targetResources
                                state.resourceReferences += result.affectedResources
                            }
                            publish(waiting, "agent.action_approved")
                            publish(waiting, "agent.step_completed")
                            val backToRunning = tryAdvance(run.agentRunId, AgentRunStatus.WAITING_FOR_PERMISSION, AgentRunStatus.RUNNING)
                            if (backToRunning == null) return getRun(run.agentRunId) ?: run
                            run = backToRunning
                            stepNumber += 1
                            // loop continues with the next Agent Step
                        }

                        ExecutionResultStatus.DENIED -> {
                            // design document Section 6.4: DENIED ends the Agent Run at FAILED --
                            // distinct from DEFERRED below, unlike Unit 7's collapsed handling.
                            mutex.withLock { state.deniedActions += decision.proposedAction }
                            publish(waiting, "agent.action_denied")
                            val failed = tryAdvance(run.agentRunId, AgentRunStatus.WAITING_FOR_PERMISSION, AgentRunStatus.FAILED)
                            if (failed == null) return getRun(run.agentRunId) ?: run
                            publish(failed, "agent.failed")
                            return failed
                        }

                        ExecutionResultStatus.DEFERRED -> {
                            // design document Section 6.4: DEFERRED ends the current step at
                            // SUSPENDED -- a recoverable pause, distinct from DENIED.
                            publish(waiting, "agent.action_deferred")
                            val suspended = tryAdvance(run.agentRunId, AgentRunStatus.WAITING_FOR_PERMISSION, AgentRunStatus.SUSPENDED)
                            if (suspended == null) return getRun(run.agentRunId) ?: run
                            publish(suspended, "agent.suspended")
                            return suspended
                        }

                        ExecutionResultStatus.FAILED, ExecutionResultStatus.CANCELLED, ExecutionResultStatus.EXPIRED -> {
                            publish(waiting, "agent.step_completed")
                            val failed = tryAdvance(run.agentRunId, AgentRunStatus.WAITING_FOR_PERMISSION, AgentRunStatus.FAILED)
                            if (failed == null) return getRun(run.agentRunId) ?: run
                            publish(failed, "agent.failed")
                            return failed
                        }
                    }
                }
            }
        }
    }

    // --- SUSPEND / RESUME / CANCEL -------------------------------------

    /**
     * Records a pending suspend request against this Agent Run's
     * [RunState] and returns immediately -- it does not itself transition
     * [AgentRunStatus] (design document Section 6.3: `SUSPEND` is honoured
     * at the *next step boundary*, by [runLoop] itself, so an Agent Step
     * already in flight always runs to completion). Rejected if the Agent
     * Run does not exist or is not currently `RUNNING`.
     */
    private suspend fun suspendRun(command: AgentRunCommand): AgentRunCommandResult {
        val agentRunId = requireNotNull(command.agentRunId) { "AgentRunCommand.SUSPEND requires an agentRunId" }
        val outcome = mutex.withLock {
            val run = agentRuns[agentRunId] ?: return@withLock null
            if (run.status != AgentRunStatus.RUNNING) return@withLock false to run.status
            val state = runStates.getOrPut(agentRunId) { RunState() }
            state.pendingSuspend = true
            true to run.status
        }
        return when (outcome) {
            null -> AgentRunCommandResult.Rejected(
                command.commandType,
                "no Agent Run found for agentRunId '${agentRunId.value}'",
            )
            else -> {
                val (accepted, status) = outcome
                if (accepted) {
                    AgentRunCommandResult.Accepted(agentRunId, command.commandType)
                } else {
                    AgentRunCommandResult.Rejected(
                        command.commandType,
                        "cannot SUSPEND Agent Run '${agentRunId.value}': current status is $status, not RUNNING",
                    )
                }
            }
        }
    }

    /**
     * Transitions a `SUSPENDED` Agent Run back to `RUNNING` and resumes
     * the step loop with the next step number after whatever was already
     * taken (design document Section 6.3, "the only resume path" per
     * `AgentRunLifecycleTransitions`). Rejected if the Agent Run does not
     * exist or is not currently `SUSPENDED`.
     */
    private suspend fun resumeRun(command: AgentRunCommand): AgentRunCommandResult {
        val agentRunId = requireNotNull(command.agentRunId) { "AgentRunCommand.RESUME requires an agentRunId" }
        val existing = getRun(agentRunId)
            ?: return AgentRunCommandResult.Rejected(
                command.commandType,
                "no Agent Run found for agentRunId '${agentRunId.value}'",
            )

        val resumed = tryAdvance(agentRunId, AgentRunStatus.SUSPENDED, AgentRunStatus.RUNNING)
            ?: return AgentRunCommandResult.Rejected(
                command.commandType,
                "cannot RESUME Agent Run '${agentRunId.value}': current status is ${existing.status}, not SUSPENDED",
            )
        publish(resumed, "agent.resumed")

        // No re-resolution needed: agentIdentityPrincipalId was already resolved through
        // IdentityService once, at START -- an Agent Run's identity does not change over its
        // lifetime (AgentRuntimeSpecification.md Section 7 has no re-resolution or identity-change
        // concept anywhere in the lifecycle).
        val state = mutex.withLock { runStates.getOrPut(agentRunId) { RunState() } }
        val nextStepNumber = state.successfulSteps + 1

        val finalRun = runLoop(resumed, resumed.agentIdentityPrincipalId, emptyList(), startingStepNumber = nextStepNumber)
        return AgentRunCommandResult.Accepted(finalRun.agentRunId, command.commandType)
    }

    /**
     * Cancels an Agent Run immediately from any non-terminal state
     * (`AgentRunLifecycleTransitions`: `CANCELLED` is reachable from every
     * non-terminal state), then makes a best-effort attempt to cancel
     * whatever [ExecutionRequest] is currently in flight for it (design
     * document Section 6.5) -- the cancellation of the Agent Run itself
     * does not wait on, or depend on, that attempt succeeding. Rejected if
     * the Agent Run does not exist or is already terminal.
     */
    private suspend fun cancelRun(command: AgentRunCommand): AgentRunCommandResult {
        val agentRunId = requireNotNull(command.agentRunId) { "AgentRunCommand.CANCEL requires an agentRunId" }

        val cancelled = mutex.withLock {
            val run = agentRuns[agentRunId] ?: return@withLock null
            if (AgentRunLifecycleTransitions.isTerminal(run.status)) return@withLock null
            AgentRunLifecycleTransitions.requireValidTransition(run.status, AgentRunStatus.CANCELLED)
            val updated = run.copy(status = AgentRunStatus.CANCELLED)
            agentRuns[agentRunId] = updated
            updated
        } ?: return AgentRunCommandResult.Rejected(
            command.commandType,
            "cannot CANCEL Agent Run '${agentRunId.value}': it does not exist or is already terminal",
        )

        publish(cancelled, "agent.cancelled")

        val inFlightRequestId = mutex.withLock { runStates[agentRunId]?.inFlightRequestId }
        if (inFlightRequestId != null) {
            // Best-effort only: whether or not the Execution Pipeline actually manages to cancel an
            // in-flight step, this Agent Run's own CANCELLED status (just published above) already
            // stands (design document Section 6.5).
            executionPipeline.cancel(inFlightRequestId)
        }

        return AgentRunCommandResult.Accepted(agentRunId, command.commandType)
    }

    // --- lookups / events ----------------------------------------------

    /** The current state of [agentRunId], or `null` if no Agent Run with that ID exists. */
    suspend fun getAgentRun(agentRunId: AgentRunId): AgentRun? = getRun(agentRunId)

    /** Every Agent Run this runtime has created so far, in no particular order. */
    suspend fun listAgentRuns(): List<AgentRun> = mutex.withLock { agentRuns.values.toList() }

    private suspend fun getRun(agentRunId: AgentRunId): AgentRun? = mutex.withLock { agentRuns[agentRunId] }

    /**
     * Sprint 1, Unit 9 (extended by Unit C2; extended again by Agent Run
     * Reference Exposure): publishes one `agent.*` [ParkerEvent]. Unlike
     * Unit 7 (one Agent Step per Agent Run, so
     * `"evt-<agentRunId>-<eventType>"` was already unique), a multi-step
     * Agent Run publishes the same `eventType` more than once (e.g.
     * `agent.step_completed` once per successful step) -- a random UUID
     * suffix keeps every `eventId` unique, mirroring
     * [InMemoryEventBus.subscribe]'s own existing `UUID.randomUUID()` use
     * for `subscriptionId`.
     *
     * **Agent Run Reference Exposure** (`docs/implementation/AGENT_RUN_REFERENCE_EXPOSURE_IMPLEMENTATION_PLAN.md`):
     * every event this method publishes now also carries `"agentRunId"`
     * in its payload, alongside the existing `"taskId"`. Because every
     * one of this class's emitted event types calls this single shared
     * helper, this exposes `agentRunId` uniformly across all of them --
     * not `agent.completed` alone -- per that plan's own Section 2/3
     * consistency finding. `correlationId` (`run.correlationId`) is
     * unchanged by this addition; the pre-existing, documented
     * `correlationId`-vs-`AgentRunId` wording tension between this
     * class's own established convention and
     * `AgentRuntimeSpecification.md` Section 9's prose is not resolved,
     * addressed, or touched by this change (see that plan's own Section
     * 9).
     */
    private suspend fun publish(run: AgentRun, eventType: String) {
        eventBus.publish(
            ParkerEvent(
                eventId = "evt-${run.agentRunId.value}-$eventType-${UUID.randomUUID()}",
                publisherPrincipalId = run.agentIdentityPrincipalId,
                eventType = EventType(eventType),
                timestamp = Instant.now(),
                correlationId = run.correlationId,
                payload = mapOf("taskId" to run.taskId.value, "agentRunId" to run.agentRunId.value),
            ),
        )
    }
}
