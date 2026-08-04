package parker.core.runtime

import java.time.Instant
import java.util.UUID
import parker.core.interfaces.ExecutionRequest
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.RequestId
import parker.core.interfaces.RequestOrigin
import parker.core.interfaces.RequestPriority
import parker.core.interfaces.ResourceId

/**
 * Evidence Intelligence, Implementation Unit 6 ("Invocation Permission
 * Gating"). Governed in full by
 * `docs/architecture/EVIDENCE_INTELLIGENCE_SCOPE_LOCK.md` ("the Scope
 * Lock") §6 step 0, §7, §11; by
 * `docs/implementation/EVIDENCE_INTELLIGENCE_IMPLEMENTATION_PLAN.md`
 * ("the Implementation Plan") §8 Unit 6; by this Unit's own Planning and
 * Boundary Review (conducted at this Unit's own governance-first workflow
 * start; delivered as review output, not itself committed as a standalone
 * document); and by
 * `docs/reviews/UNIT_6_INVOCATION_PERMISSION_DENIAL_REPRESENTATION_STUDY.md`,
 * whose own Verdict B was subsequently reconciled to **Verdict A --
 * existing governance already sufficient** (independent reconciliation
 * review): denial representation is not this Unit's responsibility to
 * define, because ownership of "what happens on denial" already belongs,
 * by the Scope Lock's own text and by unbroken repository precedent
 * (`EvidenceRegistrationCoordinator`, `InMemoryAgentRuntime`,
 * `DefaultExecutionPipeline`, each already owning its own outcome type),
 * to whichever operation Unit 8 introduces as Evidence Intelligence's
 * composing caller -- never to this file.
 *
 * ## What this Unit is: a disclosed proposal-class convention, not a mechanism
 *
 * The Implementation Plan (§6, item 6) is explicit that the invocation gate
 * is **"not a class or a component in its own right -- a governance
 * requirement that whatever composes Evidence Intelligence into the
 * running system evaluates a dedicated Permission Engine proposal class...
 * before calling the Evidence Intelligence operation."** This file is the
 * smallest possible artefact consistent with that framing: it discloses
 * the frozen `PermissionAction.EXECUTE`/`ResourceType.DOCUMENT` proposal
 * class's own resourceId/action-name convention (mirroring
 * `DefaultEvidenceCustodian.EVIDENCE_INTAKE_RESOURCE_ID`/`ACCEPT_ACTION_NAME`
 * and `DefaultOwnerEvidenceDeletionAuthority.EVIDENCE_DELETION_RESOURCE_ID`/
 * `DELETE_ACTION_NAME` exactly) and supplies one pure, stateless
 * [ExecutionRequest] builder so a future Unit 8 caller need not re-derive
 * that convention by hand -- nothing more.
 *
 * **A Kotlin `object`, not a `class`.** An `object` has no constructor of
 * any kind, so it is structurally impossible -- not merely undocumented --
 * for this artefact to ever acquire a `PermissionEngine`, an
 * `EvidenceIntelligence`, or any other constructor-injected dependency.
 * This is the same "no Permission Engine reachability of Evidence
 * Intelligence's own" guarantee the Scope Lock (§4, §9) already requires
 * of `EvidenceIntelligence` itself, extended here by construction rather
 * than by convention, even though this file is not part of Evidence
 * Intelligence's own model at all (see "Why this is not a fifth public
 * type," below).
 *
 * ## What this Unit deliberately does not do
 *
 * - **Does not call [parker.core.interfaces.PermissionEngine.evaluate].**
 *   Building the request and evaluating it are two different
 *   responsibilities; this file performs only the first. Evaluating it,
 *   branching on the resulting `PermissionDecisionOutcome`, and deciding
 *   whether to proceed are the composing caller's own responsibility
 *   (Scope Lock §6 step 0).
 * - **Does not call [parker.core.interfaces.EvidenceIntelligence.analyse].**
 *   That call belongs entirely to whatever Unit 8 introduces.
 * - **Does not define, or even anticipate the shape of, a denial
 *   representation.** Per the reconciliation review cited above: that
 *   responsibility already belongs to Unit 8's own composing operation,
 *   exactly as `EvidenceRegistrationCoordinator` already owns
 *   `EvidenceRegistrationOutcome` for its own, comparable, sequencing
 *   responsibility. Pre-empting that choice here would exceed this Unit's
 *   own, deliberately narrow scope.
 * - **Registers nothing.** Neither [EVIDENCE_INTELLIGENCE_INVOCATION_RESOURCE_ID]
 *   nor [ANALYSE_ACTION_NAME] is registered against `ResourceRegistry` or
 *   `ActionVocabulary` anywhere by this Unit -- exactly as none of Evidence
 *   Custodian's own five disclosed conventions were registered by the
 *   Units that first disclosed them. Registration, and deciding which
 *   `PermissionPolicyRule` applies, is Implementation Plan Phase
 *   8/"Runtime Integration" work (mirroring Evidence Custodian's own
 *   Phase 10), performed only once Unit 8 exists. Until that registration
 *   occurs, a real `DefaultPermissionPolicy` conservatively denies every
 *   request built from this convention (`DefaultPermissionPolicy`'s own
 *   "Unknown Action"/"Unknown Resource" handling) -- the same safe,
 *   fail-closed default every other disclosed-but-unregistered convention
 *   in this repository already starts from.
 *
 * ## Why this is not a fifth public type
 *
 * The Scope Lock (§4) caps Evidence Intelligence's own new public surface
 * at exactly four types: [parker.core.interfaces.EvidenceAnalysisRequest],
 * [parker.core.interfaces.EvidenceAnalysisResult], the "Candidate record
 * produced" payload selector, and
 * [parker.core.interfaces.EvidenceIntelligence] itself -- all four already
 * introduced by Units 1 and 5. That ceiling governs Evidence Intelligence's
 * own model (Contract Design §3); it does not reach, and was never intended
 * to reach, a composition-level artefact that Evidence Intelligence itself
 * neither depends upon nor is depended upon by. This file imports no type
 * from Evidence Intelligence's own model at all, implements no interface,
 * and is never referenced from
 * [parker.core.interfaces.EvidenceAnalysisRequest],
 * [parker.core.interfaces.EvidenceAnalysisResult], or
 * [parker.core.interfaces.EvidenceIntelligence] -- the same footing Unit
 * 7's own, separately authorised acceptance coordinator already occupies
 * (Scope Lock §6 step 4; Implementation Plan §8 Unit 7).
 *
 * ## Resource identity: invocation-level, not per-artefact
 *
 * [EVIDENCE_INTELLIGENCE_INVOCATION_RESOURCE_ID] is a single, fixed,
 * well-known [ResourceId] standing for the "invoke Evidence Intelligence"
 * capability itself -- never a per-`EvidenceArtifactId` or
 * per-Memory-Core-reference identity, exactly mirroring
 * [DefaultEvidenceCustodian.EVIDENCE_INTAKE_RESOURCE_ID]'s own KDoc ("not
 * a per-artefact identity... a single, stable stand-in for the capability
 * itself"). Scope Lock §6 step 0 names the gated act as "the 'invoke
 * Evidence Intelligence' domain act" (singular); an `EvidenceAnalysisRequest`
 * may reference zero, one, or many evidence artefacts or Memory Core
 * records, each already separately gated at its own boundary
 * (`EvidenceCustodian.retrieve`, `MemoryRetrieval`'s own composing-caller
 * gate) -- this convention gates the invocation as a whole, once, never
 * each reference within it.
 */
object EvidenceIntelligenceInvocationGate {

    /**
     * A fixed, well-known [ResourceId] this convention's own
     * [ExecutionRequest] always names as its target -- not a per-artefact
     * identity. Not registered anywhere by this Unit; see this file's own
     * header KDoc.
     */
    val EVIDENCE_INTELLIGENCE_INVOCATION_RESOURCE_ID: ResourceId =
        ResourceId("evidence-intelligence-invocation")

    /**
     * A fixed proposed-action name a future `ActionVocabulary`
     * registration is expected to map to `(PermissionAction.EXECUTE,
     * ResourceType.DOCUMENT)` (Implementation Plan §8 Unit 6). Not
     * registered anywhere by this Unit.
     */
    const val ANALYSE_ACTION_NAME: String = "evidence-intelligence.analyse"

    /**
     * Builds the [ExecutionRequest] a future Unit 8 composing caller
     * evaluates via `PermissionEngine.evaluate` before calling
     * [parker.core.interfaces.EvidenceIntelligence.analyse] -- mirroring
     * [DefaultEvidenceCustodian.buildExecutionRequest]'s own shared-helper
     * shape exactly, made `public` (rather than `private`) only because,
     * unlike that precedent, no single implementing class exists yet to
     * keep it private within.
     *
     * Mints a fresh `requestId`/`correlationId` per call, exactly as
     * [DefaultEvidenceCustodian] and [DefaultOwnerEvidenceDeletionAuthority]
     * already do for their own conventions -- `EvidenceAnalysisRequest`
     * (Unit 1) carries no correlation identifier of its own for this
     * function to reuse instead (contrast
     * [EvidenceRegistrationCoordinator], which reuses a caller-supplied
     * `correlationId` because one already exists at its own call site).
     *
     * This function only builds the request. It does not call
     * `PermissionEngine.evaluate`, and does not itself hold, import, or
     * reference `PermissionEngine` in any form.
     *
     * @param requestingPrincipalId [parker.core.interfaces.EvidenceAnalysisRequest.requestingPrincipalId],
     *   passed through unchanged -- the only principal Contract Design §4
     *   supplies for audit purposes on an analysis invocation.
     */
    fun buildExecutionRequest(requestingPrincipalId: PrincipalId): ExecutionRequest {
        val now = Instant.now()
        return ExecutionRequest(
            requestId = RequestId("evidence-intelligence-invoke-${UUID.randomUUID()}"),
            principalId = requestingPrincipalId,
            origin = RequestOrigin.REMOTE_INTERFACE,
            intent = "Invoke Evidence Intelligence analysis",
            targetResources = listOf(EVIDENCE_INTELLIGENCE_INVOCATION_RESOURCE_ID),
            proposedActions = listOf(ANALYSE_ACTION_NAME),
            priority = RequestPriority.NORMAL,
            createdAt = now,
            correlationId = "evidence-intelligence-invoke-${UUID.randomUUID()}",
        )
    }
}
