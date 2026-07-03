package parker.core.interfaces

/**
 * Sprint 1 contract closing Blocker 3 of
 * docs/implementation/SPRINT_1_VERTICAL_SLICE_PLAN.md (see
 * docs/implementation/SPRINT_1_BLOCKER_CLOSURE.md for the full closure
 * record). This file is a **pure contract addition**: value-class
 * identifiers, an enum, a data class, a sealed result type, and one small
 * interface. It adds no runtime behaviour and modifies no existing file.
 *
 * This closes both `INTER_SPECIFICATION_CONTRACTS.md` Section 6's Gap 7
 * (Agent Run Request has no named, shaped object) and Gap 11
 * (`Phase3ArchitecturePositionReview.md` Section 6 -- the identical
 * asymmetry for Agent Run *cancellation*) with one contract, since both
 * gaps are the same underlying need: a named channel by which the Task
 * Manager Runtime asks the Agent Runtime to do something to an Agent Run
 * it does not itself own or control directly (AD-006).
 *
 * This is not new architecture. `AgentRuntimeSpecification.md` Section 5
 * already anticipates every command this type names, without saying who
 * issues it:
 * - `SUSPENDED` is reachable "from an explicit suspend request" (source
 *   unnamed).
 * - `SUSPENDED --> RUNNING` is "the only resume path" (trigger unnamed).
 * - `CANCELLED` is reachable "from every non-terminal state" because "an
 *   external cancellation request can legitimately arrive at any point"
 *   (source unnamed).
 * `TaskManagerRuntimeSpecification.md` Section 7's sequence diagram
 * already shows "TM->>AR: create Agent Run" and Section 5's "Cancellation
 * semantics" already states cancelling a Task "MUST cause the Task
 * Manager Runtime to request cancellation of every Agent Run Reference...
 * still active" -- again without naming the object that request takes.
 * This file names that already-anticipated object; it does not invent a
 * new capability for either specification.
 */

/**
 * An Agent Run's identifier (`AgentRuntimeSpecification.md` Section 4,
 * Section 5). First Kotlin representation of this concept -- the Agent
 * Runtime itself has no other Kotlin yet (`IMPLEMENTATION_ORDER.md`
 * Section 3 lists it as a "corrected draft" specification, not yet
 * promoted to an implementation phase). This type exists here only
 * because [AgentRunCommand] must be able to reference an existing Agent
 * Run for `SUSPEND`/`RESUME`/`CANCEL`. A full Agent Run Kotlin type
 * (fields, lifecycle transitions) remains Sprint 1 coding work, not
 * introduced by this change.
 */
@JvmInline
value class AgentRunId(val value: String) {
    init {
        require(value.isNotBlank()) { "AgentRunId must not be blank" }
    }
}

/**
 * The four commands `AgentRuntimeSpecification.md` Section 5 already
 * anticipates an external caller issuing (see file-level comment above).
 * No fifth command is added -- this repository specifies no other
 * externally-triggered Agent Run transition anywhere.
 */
enum class AgentRunCommandType {
    START,
    SUSPEND,
    RESUME,
    CANCEL,
}

/**
 * The object the Task Manager Runtime passes to the Agent Runtime to
 * request that an Agent Run start, suspend, resume, or be cancelled.
 * Field-by-field provenance:
 *
 * - [commandType]: see [AgentRunCommandType].
 * - [taskId]: the Task Manager Task this Agent Run operates within or on
 *   behalf of (`TaskManagerRuntimeSpecification.md` Section 6) --
 *   required for every command type, since even a brand-new Agent Run
 *   (`START`) is always created to progress a specific, already-existing
 *   Task Manager Task, never on its own.
 * - [agentRunId]: identifies the specific Agent Run for `SUSPEND`,
 *   `RESUME`, and `CANCEL`. Deliberately `null` for `START`, and the
 *   [init] block enforces this: a `START` command is what brings an
 *   Agent Run's own ID into existence, so it cannot itself carry one, and
 *   an `AgentRunId` that already exists cannot legally be the target of a
 *   command that creates a new one.
 * - [requestingPrincipalId]: the identity issuing this command --
 *   `TaskManagerRuntimeSpecification.md` Section 8 already requires
 *   "Every Task operation is performed by an authenticated Principal or
 *   service identity"; this restates that requirement for commands
 *   issued about an Agent Run, resolved through the Identity Service like
 *   any other `PrincipalId` in this repository -- never a
 *   Task-Manager-local or Agent-Runtime-local identity.
 * - [targetAgentCapability]: only meaningful for `START` -- the kinds of
 *   Agent Capability (`AgentRuntimeSpecification.md` Section 4) this
 *   Agent Run is expected to need, carried forward from a [TaskProposal]'s
 *   own `requiredCapabilities` where one exists. A planning-time hint
 *   only, per that field's own documentation -- never a grant (Section 7
 *   there: "narrows what an Agent Instance is expected to attempt," does
 *   not expand authority).
 * - [goalDescription]: the Goal (or, for `SUSPEND`/`RESUME`/`CANCEL`, a
 *   short description of the step/reason context) this command concerns.
 *   Free text, mirroring [TaskProposal.goal]'s identical treatment.
 * - [contextReferences]: opaque Task Context references
 *   (`TaskManagerRuntimeSpecification.md` Section 9) the Agent Runtime may
 *   need to resolve its own Agent Context against -- never a copy of Task
 *   Context itself, consistent with Section 9's "Task Context is not
 *   Agent Context."
 * - [permissionScopeReference]: an opaque reference to whatever
 *   permission-relevant labelling already exists for this Task (e.g. a
 *   [TaskProposal.anticipatedPermissionActions] carried forward) -- never
 *   itself a `PermissionDecision`, and never treated as one. Every
 *   `ExecutionRequest` the resulting Agent Run eventually submits is still
 *   independently evaluated by `PermissionEngine.evaluate` (AD-007); this
 *   field is advisory context only, exactly as
 *   `TaskProposal.anticipatedPermissionActions` already is.
 * - [resourceReferences]: Resources (`ResourceRegistry`) already known to
 *   be relevant to this Task, for the Agent Run's own Agent Context
 *   continuity (`AgentRuntimeSpecification.md` Section 8, "Resource
 *   references").
 * - [correlationId]: shared across this command, the Agent Run it
 *   concerns, and every `agent.*`/`task.*` event either side publishes
 *   about it -- consistent with every other correlation-bearing type in
 *   this repository.
 * - [cancellationReason]: required (non-blank) for `CANCEL`, meaningless
 *   for the other three command types -- the [init] block enforces this
 *   split explicitly rather than leaving it to convention.
 */
data class AgentRunCommand(
    val commandType: AgentRunCommandType,
    val taskId: TaskId,
    val agentRunId: AgentRunId? = null,
    val requestingPrincipalId: PrincipalId,
    val targetAgentCapability: Set<PermissionAction> = emptySet(),
    val goalDescription: String,
    val contextReferences: List<String> = emptyList(),
    val permissionScopeReference: String? = null,
    val resourceReferences: List<ResourceId> = emptyList(),
    val correlationId: String,
    val cancellationReason: String? = null,
) {
    init {
        require(goalDescription.isNotBlank()) { "AgentRunCommand.goalDescription must not be blank" }
        require(correlationId.isNotBlank()) { "AgentRunCommand.correlationId must not be blank" }
        when (commandType) {
            AgentRunCommandType.START -> require(agentRunId == null) {
                "AgentRunCommand.START must not reference an existing agentRunId -- the Agent Run does not exist yet"
            }
            AgentRunCommandType.SUSPEND, AgentRunCommandType.RESUME, AgentRunCommandType.CANCEL -> requireNotNull(agentRunId) {
                "AgentRunCommand.$commandType requires an existing agentRunId"
            }
        }
        if (commandType == AgentRunCommandType.CANCEL) {
            require(!cancellationReason.isNullOrBlank()) {
                "AgentRunCommand.CANCEL requires a non-blank cancellationReason"
            }
        }
    }
}

/**
 * The Agent Runtime's response to an [AgentRunCommand]. Mirrors this
 * repository's established success-or-typed-failure pattern
 * ([ToolRegistrationOutcome], [CancellationResult]) rather than throwing,
 * consistent with every other cross-component operation in this
 * repository.
 *
 * Neither outcome grants execution authority by itself (AD-002, AD-007):
 * accepting a `START` command means an Agent Run now exists and will
 * begin its own lifecycle (`AgentRuntimeSpecification.md` Section 5), not
 * that any action it later proposes is pre-approved.
 */
sealed class AgentRunCommandResult {
    data class Accepted(val agentRunId: AgentRunId, val commandType: AgentRunCommandType) : AgentRunCommandResult()
    data class Rejected(val commandType: AgentRunCommandType, val reason: String) : AgentRunCommandResult() {
        init {
            require(reason.isNotBlank()) { "AgentRunCommandResult.Rejected.reason must not be blank" }
        }
    }
}

/**
 * The channel named, but not shaped, by `TaskManagerRuntimeSpecification.md`
 * Section 7's sequence diagram ("create Agent Run") and Section 5's
 * cancellation-cascade requirement, and depended on, but not defined, by
 * `AgentRuntimeSpecification.md`'s own "explicit suspend request" /
 * "external cancellation request" language (Section 5).
 *
 * This interface declares the operation's signature only. **No
 * implementation of this interface exists in this repository as of this
 * change** -- providing one (an in-memory Agent Runtime that actually
 * creates and manages Agent Runs) is Sprint 1 coding work, per
 * `docs/implementation/SPRINT_1_VERTICAL_SLICE_PLAN.md` Unit 7, not this
 * contract-preparation change.
 */
interface AgentRunCommandChannel {
    suspend fun submit(command: AgentRunCommand): AgentRunCommandResult
}
