package parker.core.interfaces

/**
 * A single, bounded execution of an Agent Instance against a Goal
 * (`AgentRuntimeSpecification.md` Section 4, "Agent Run"; Section 5,
 * Agent Run lifecycle). First Kotlin representation of this concept --
 * `src/contracts/AgentRunCommand.kt` already named [AgentRunId] because an
 * [AgentRunCommand] must reference it, but deliberately left the full
 * Agent Run type for Sprint 1 coding work
 * (`docs/implementation/SPRINT_1_VERTICAL_SLICE_PLAN.md` Unit 7), not
 * introduced there.
 *
 * **Deliberately collapses "Agent Instance" and "Agent Run" into one
 * type.** Section 4 distinguishes them (an Agent Instance is the running
 * realisation; an Agent Run is its bounded execution against a Goal; "an
 * Agent Instance that is resumed after `SUSPENDED` continues the same
 * Agent Run, not a new one" implies an Agent Instance could in principle
 * outlive or precede a specific Agent Run). [InMemoryAgentRuntime]'s own
 * scope (Unit 7) creates exactly one Agent Run per accepted
 * [AgentRunCommand.START], with no re-use of an Agent Instance across
 * multiple Agent Runs modelled -- Section 8 itself leaves whether that
 * re-use is even possible as an open question (Section 12). Introducing a
 * separate `AgentInstance` type with no unit that reads or writes it
 * distinctly from its one Agent Run would be speculative; this mirrors
 * exactly how `src/contracts/Task.kt` (Unit 6) collapsed several of
 * `TaskManagerRuntimeSpecification.md` Section 4's Core Concepts into one
 * minimal type rather than modelling each independently.
 *
 * Field-by-field provenance:
 *
 * - [agentRunId]: this Agent Run's own identifier -- [AgentRunId]
 *   (`src/contracts/AgentRunCommand.kt`).
 * - [agentIdentityPrincipalId]: Section 4's "Agent Identity" -- the
 *   `PrincipalId` this Agent Instance runs under, resolved through the
 *   Identity Service (Section 7), never a Agent-Runtime-local identity.
 * - [taskId]: the Task Manager Task (`src/contracts/Task.kt`, Unit 6) this
 *   Agent Run operates within, carried from the originating
 *   [AgentRunCommand.taskId].
 * - [status]: **(schema: the Agent Run lifecycle).** See [AgentRunStatus].
 * - [goal]: the Goal (Section 4) this Agent Run is pursuing, carried from
 *   [AgentRunCommand.goalDescription].
 * - [correlationId]: shared with the originating [AgentRunCommand] and
 *   every `agent.*`/`execution.*` event either side publishes about it
 *   (Section 9) -- published for the `agent.*` side by
 *   [InMemoryAgentRuntime] as of Sprint 1, Unit 9.
 */
data class AgentRun(
    val agentRunId: AgentRunId,
    val agentIdentityPrincipalId: PrincipalId,
    val taskId: TaskId,
    val status: AgentRunStatus,
    val goal: String,
    val correlationId: String,
) {
    init {
        require(goal.isNotBlank()) { "AgentRun.goal must not be blank" }
        require(correlationId.isNotBlank()) { "AgentRun.correlationId must not be blank" }
    }
}
