package parker.core.interfaces

import java.time.Duration

/**
 * Sprint 3, Track C, Unit C2. Pure contract additions specified in full by
 * `docs/architecture/MULTI_STEP_AGENT_RUN_DESIGN.md` Section 4.2 (`AgentStepSource`,
 * `AgentStepContext`, `AgentStepDecision`) and Section 4.3 (`AgentPolicy`).
 * This file introduces no architecture beyond what that design document
 * already settles -- every shape below matches its "Public Interfaces"
 * section field-for-field.
 *
 * Per the design document's Section 2 ("Cognition proposes"), `AgentStepSource`
 * is the seam by which "what should this Agent Run's next step be" is
 * decided, kept strictly separate from "should that step be allowed to
 * have an effect" (the Permission Engine's unchanged job). Per AD-010
 * (Model Independence), nothing here or in [InMemoryAgentRuntime]'s
 * consumption of it assumes a specific reasoning approach, model, or
 * Planner sits behind this interface -- see the design document's Section
 * 11 for what Unit C2 supplies as a fixed, deterministic, non-Planner
 * stand-in, mirroring [DeterministicPlannerHarness]'s exact precedent.
 */

/**
 * The read-only view [AgentStepSource] is given each time it is consulted
 * (design document Section 4.2). Every field maps to an existing concept
 * already named by `AgentRuntimeSpecification.md` Section 8 (Agent
 * Context Model) -- this type gives that existing field list a concrete
 * shape; it invents no new Agent Context concept.
 *
 * - [stepNumber]: maps to Section 8's "Agent Step state." 1-indexed; `1`
 *   on the first call for a given Agent Run, continuing (not resetting)
 *   across a `SUSPEND`/`RESUME` pair, since resuming continues the same
 *   Agent Run (`AgentRuntimeSpecification.md` Section 4, "Agent Run").
 * - [priorResult]: the most recent [ExecutionResult] this Agent Run
 *   received, or `null` before the first step -- the natural reference an
 *   adaptive step-2-or-later decision needs to consult.
 * - [resourceReferences]: maps to Section 8's "Resource references" --
 *   accumulated from every step's [AgentStepDecision.Propose.targetResources]
 *   and every [ExecutionResult.affectedResources] so far, "for continuity
 *   across Agent Steps."
 * - [deniedActions]: maps to Section 8's "Permission scope" -- every
 *   `proposedAction` for which the corresponding step reached `DENIED`,
 *   "useful... to avoid re-proposing already-denied actions." This is a
 *   cache of past decisions for planning convenience, never itself a
 *   source of authority (design document Section 9) -- every new proposed
 *   action from [AgentStepSource] is still independently evaluated by
 *   `PermissionEngine.evaluate` via `ExecutionPipeline.submit`.
 */
data class AgentStepContext(
    val agentRunId: AgentRunId,
    val taskId: TaskId,
    val goal: String,
    val stepNumber: Int,
    val priorResult: ExecutionResult?,
    val resourceReferences: List<ResourceId>,
    val deniedActions: List<String>,
) {
    init {
        require(stepNumber >= 1) { "AgentStepContext.stepNumber must be at least 1" }
    }
}

/**
 * What [AgentStepSource] decides for a given [AgentStepContext] (design
 * document Section 4.2). Exactly three variants -- deliberately no
 * `Suspend`/"pause pending external input" variant, since Unit C2 does
 * not reach `WAITING_FOR_INPUT` (design document Sections 5.2, 6.6, 11).
 *
 * **Forward compatibility note**, carried from the design document
 * verbatim: future versions of this seam may introduce additional
 * [AgentStepDecision] variants -- for example, a variant corresponding to
 * "stop here until external information exists," once a real
 * input-supply channel exists to resume from -- without affecting the
 * runtime architecture the design document specifies. Adding a variant
 * later is an additive change to this sealed type and its one call site
 * ([InMemoryAgentRuntime]'s per-step loop), not a redesign of anything
 * above it.
 */
sealed class AgentStepDecision {
    /** Propose one more Agent Step's action. Carries no authority -- see this file's own KDoc. */
    data class Propose(
        val proposedAction: String,
        val targetResources: List<ResourceId> = emptyList(),
    ) : AgentStepDecision()

    /**
     * The Agent Run's Goal has been achieved. [InMemoryAgentRuntime]
     * accepts this only if at least one Agent Step has already completed
     * successfully for this Agent Run -- `AgentRuntimeSpecification.md`
     * Section 4 states an Agent Run "consists of one or more Agent
     * Steps"; a claim of completion after zero steps is treated as
     * [Fail], not silently honoured (design document Section 5.1, item 4).
     */
    object Complete : AgentStepDecision()

    /** The Agent Run cannot continue. [reason] must be non-blank, mirroring this repository's established error-shape convention. */
    data class Fail(val reason: String) : AgentStepDecision() {
        init {
            require(reason.isNotBlank()) { "AgentStepDecision.Fail.reason must not be blank" }
        }
    }
}

/**
 * The seam by which an Agent Run's next step is decided (design document
 * Section 4.2). A **decision provider, not an authority** -- mirroring
 * exactly how [DefaultPermissionPolicy] is an injected, data-shaped
 * decision provider for the Permission Engine (Sprint 2, Unit A2) and how
 * [ActionMapper] is an injected vocabulary lookup for the Execution
 * Pipeline. No variant of [AgentStepDecision] approves, denies, or
 * bypasses anything -- every [AgentStepDecision.Propose] still becomes an
 * `ExecutionRequest` independently evaluated by `PermissionEngine.evaluate`
 * via `ExecutionPipeline.submit`, exactly as any other origin's request
 * already is.
 *
 * This interface does not specify what implements it. Unit C2 supplies
 * only a fixed, deterministic, non-Planner stand-in for testing and for
 * any production wiring that exists before a real Planner does
 * (`FixedSequenceAgentStepSource`, `tests/runtime/`) -- a real Planner
 * (Chapter 20) implementing this same interface is the seam this design
 * reserves, without specifying it (design document Section 11).
 */
interface AgentStepSource {
    suspend fun nextStep(context: AgentStepContext): AgentStepDecision
}

/**
 * The bounded configuration governing an Agent Instance's operation,
 * already named as a concept, unshaped, by `AgentRuntimeSpecification.md`
 * Section 4 ("the bounded configuration governing an Agent Instance's
 * operation -- for example, maximum Agent Steps per Agent Run, maximum
 * Agent Run duration, or which Agent Capabilities are in scope"). This
 * design document (Section 4.3) gives it the minimal shape Unit C2 needs
 * and no more.
 *
 * [maxAgentSteps] is enforced by [InMemoryAgentRuntime]: reaching it after
 * a successful step transitions the Agent Run `RUNNING -> SUSPENDED`, per
 * `AgentRuntimeSpecification.md` Section 10 ("Timeout": "An Agent Run
 * exceeding an Agent Policy-defined maximum... Agent Step count... MUST
 * transition to `SUSPENDED` (recoverable) rather than `FAILED`, since
 * exceeding a configured bound is not itself evidence of an unrecoverable
 * error") -- never `FAILED`.
 *
 * [maxAgentRunDuration] is present for shape-completeness with Section
 * 4's own text, but its enforcement is explicitly deferred (design
 * document Section 11, "Unit C2 will NOT implement" -- "wall-clock
 * timeout while a step is in flight, mid-Tool-execution"). No Agent
 * Capability scoping is proposed here -- no `AgentCapability` Kotlin type
 * exists in this repository, and this unit does not add one (design
 * document Section 4.3).
 */
data class AgentPolicy(
    val maxAgentSteps: Int,
    val maxAgentRunDuration: Duration? = null,
) {
    init {
        require(maxAgentSteps >= 1) { "AgentPolicy.maxAgentSteps must be at least 1" }
    }
}
