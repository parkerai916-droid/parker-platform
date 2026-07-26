package parker.core.runtime

import parker.core.interfaces.PlanCandidate
import parker.core.interfaces.PlanCandidateGenerator
import parker.core.interfaces.PlanCandidateId
import parker.core.interfaces.PlanningRequest

/**
 * Fixed, non-caller-supplied rationale for every [PlanCandidate]
 * [DefaultPlanCandidateGenerator] produces. A private, top-level,
 * file-scoped constant -- deliberately not a companion-object member, so
 * [DefaultPlanCandidateGenerator]'s own `declaredFields` remains empty,
 * matching `docs/implementation/CANDIDATE_GENERATION_SCOPE_LOCK.md`
 * Section 3.2's "zero-argument constructor, no stored dependencies"
 * requirement exactly -- a companion object would add a synthetic
 * `Companion` field to the class, the same reasoning already applied to
 * `GoalPlanningHandoffCoordinator`'s own `DEFERRAL_DETAIL` constant.
 */
private const val VERBATIM_CANDIDATE_RATIONALE = "This is a direct, undecomposed Plan Candidate derived " +
    "verbatim from PlanningRequest.goal. No decomposition, no alternative generation, and no deliberation " +
    "were performed."

/**
 * The concrete, deterministic [PlanCandidateGenerator] this Unit supplies,
 * per `docs/architecture/CANDIDATE_GENERATION_CONTRACT_DESIGN.md` and
 * `docs/implementation/CANDIDATE_GENERATION_SCOPE_LOCK.md` Section 3.
 * Named `Default*`, matching this codebase's own established convention
 * for the single, non-configurable, production-quality reference
 * implementation of a contract ([DefaultPlanDecision],
 * [DefaultExecutionPipeline], [DefaultReasoningContextAssembler]).
 *
 * ## What this class does, in full
 *
 * For any well-formed [PlanningRequest] (already guaranteed non-blank on
 * `goal` and `correlationId` by that type's own `init` check), always
 * produces exactly **one** [PlanCandidate]:
 *
 * - `planCandidateId`: `"${request.planningSessionId.value}-candidate-1"`,
 *   wrapped as [PlanCandidateId] -- deterministic, parent-derived from the
 *   already-existing [PlanningRequest.planningSessionId], mirroring
 *   [parker.core.interfaces.TaskProposalId]'s own identical minting shape
 *   in [InMemoryPlannerRuntime.buildProposal]
 *   (`"${request.planningSessionId.value}-proposal-1"`) and
 *   `DeterministicPlannerHarness.run`'s own test-precedent
 *   `PlanCandidateId` scheme (`"$planningSessionId-candidate-1"`). No
 *   randomness and therefore no injected ID factory is needed here, unlike
 *   [GoalPlanningHandoffCoordinator]'s `planningSessionIdFactory`: that
 *   factory exists because `PlanningSessionId` has no parent identifier to
 *   derive from and must be freshly, unpredictably minted; a
 *   [PlanCandidateId] here always has one already (`planningSessionId`
 *   itself), so a deterministic derivation is both possible and, per
 *   `docs/architecture/PRE_MODULE_ID_MULTIPLICITY_DECISION.md` and
 *   `docs/architecture/IMPLEMENTATION_GAPS.md` #48, this codebase's own
 *   established convention for exactly this situation.
 * - `goal`: `request.goal`, copied verbatim, unmodified -- not trimmed,
 *   not normalised, not reinterpreted.
 * - `rationale`: the fixed [VERBATIM_CANDIDATE_RATIONALE] literal above,
 *   never derived from, or claiming insight into, the Goal's own content.
 * - Every other optional field (`riskEstimate`, `requiredCapabilities`,
 *   `anticipatedPermissionActions`, `constraints`, `dependencies`,
 *   `contextReferences`, `resourceReferences`, `expectedOutputs`) is left
 *   at [PlanCandidate]'s own existing constructor default. No value is
 *   invented for any of them, and no Assumption is recorded beyond the
 *   fixed rationale above -- `PlanningRequest` carries no information that
 *   would legitimately populate any one, and inventing content here is
 *   exactly the fabrication `CANDIDATE_GENERATION_SCOPE_LOCK.md` Section
 *   3.7 exists to prevent.
 *
 * **This class never inspects [PlanningRequest.source] or
 * [PlanningRequest.priority] to change its own behaviour** -- both are
 * read by nothing in this method's body; the produced [PlanCandidate] does
 * not carry either field at all ([PlanCandidate] has no `source`/`priority`
 * field of its own).
 *
 * **Never enriches, ranks, or generates an alternative.** Exactly one
 * [PlanCandidate] is returned, always, for every well-formed
 * [PlanningRequest] -- never zero, never more than one. This is a
 * disclosed limitation of this specific implementation, not of
 * [PlanCandidateGenerator] itself: this class has no ambiguity-detection,
 * no insufficient-information detection, and no safety-refusal logic --
 * building any of those would require exactly the decomposition
 * intelligence this Unit is not authorised to invent
 * (`CANDIDATE_GENERATION_SCOPE_LOCK.md` Section 3.3, Section 3.6). A
 * future, richer [PlanCandidateGenerator] implementation may legitimately
 * exercise the zero- or many-candidate paths this one does not.
 *
 * ## Zero-argument constructor; no stored dependencies
 *
 * No constructor parameter of any kind -- the strongest possible
 * structural guarantee, stronger than a minimum-dependency list, that
 * this class holds no reference to `IdentityService`, `EventBus`,
 * `MemorySource`, `WorldModelSource`, `ReasoningContext`,
 * `PermissionEngine`, `ToolRegistry`, `PlannerRuntime`, or
 * `TaskManagerRuntime`. [generate] never calls `PlannerRuntime.plan`, and
 * cannot: it has no way to reach it.
 *
 * ## Determinism; no fault surface
 *
 * Given the same [PlanningRequest], [generate] returns a content-equal
 * result every time -- no clock read, no random value, no external state
 * is consulted, and output order is trivial by construction (`listOf`
 * over a single, freshly-built value, never derived from a `Map`, a
 * `Set`, or any other unordered collection). [generate] cannot throw for
 * any well-formed [PlanningRequest], provable by construction:
 * `request.planningSessionId.value` is already guaranteed non-blank
 * before this method is ever called, so the derived [PlanCandidateId]
 * constructed above can never fail its own non-blank check.
 *
 * **`suspend`, per [PlanCandidateGenerator]'s own contract.** This body
 * never actually suspends -- the signature is `suspend` only because the
 * interface itself is (to leave room for a future model-backed
 * implementation without a breaking interface change), not because this
 * deterministic implementation needs it.
 */
class DefaultPlanCandidateGenerator : PlanCandidateGenerator {

    override suspend fun generate(request: PlanningRequest): List<PlanCandidate> {
        val candidate = PlanCandidate(
            planCandidateId = PlanCandidateId("${request.planningSessionId.value}-candidate-1"),
            goal = request.goal,
            rationale = VERBATIM_CANDIDATE_RATIONALE,
        )
        return listOf(candidate)
    }
}
