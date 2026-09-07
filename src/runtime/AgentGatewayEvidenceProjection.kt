package parker.core.runtime

import java.time.Instant
import java.util.UUID
import parker.core.interfaces.AuthorizationPurposeId
import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.EvidenceCustodian
import parker.core.interfaces.EvidenceManifestRetrievalResult
import parker.core.interfaces.EvidenceRetrievalResult
import parker.core.interfaces.ExecutionRequest
import parker.core.interfaces.PermissionDecision
import parker.core.interfaces.PermissionDecisionOutcome
import parker.core.interfaces.PermissionEngine
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.RequestId
import parker.core.interfaces.RequestOrigin
import parker.core.interfaces.RequestPriority
import parker.core.interfaces.ResourceId

/**
 * Parker Agent Gateway, AG-1D (R0 Governed Runtime Projections,
 * `docs/architecture/PARKER_AGENT_GATEWAY_SCOPE_LOCK.md` Section 7 item 2,
 * Section 20). The minimum, thin, read-only "AsAgent" projection layer for
 * Hermes's own future R0 supervision reads -- evidence retrieval and
 * evidence source-manifest retrieval, both by an already-known
 * [EvidenceArtifactId].
 *
 * ## Structural principal binding (AG-1C security review's own hard requirement)
 *
 * [hermesPrincipalId] and [agentGatewayPurpose] are fixed at construction --
 * there is no method on this class, public or otherwise, through which any
 * caller could substitute a different `PrincipalId`, `AuthorizationPurposeId`,
 * `proposedAction`, or target `ResourceId`. Every [ExecutionRequest] this
 * class ever constructs always names exactly:
 * - `principalId` = [hermesPrincipalId] (production composition: AG-1B's
 *   own `PrincipalId("agent.hermes-ingestion-operator")`)
 * - `authorizationPurpose` = [agentGatewayPurpose] (production composition:
 *   AG-1B's own `AuthorizationPurposeId("agent-gateway.hermes-ingestion")`)
 * - the exact AG-1C `proposedAction`/target `ResourceId` pair for the
 *   operation requested -- never a caller-supplied verb or resource
 *
 * This mirrors exactly how `ParkerRuntime.deleteEvidenceAsOwner`/Controlled
 * Agent Run Submission already enforce principal-sensitive operations by
 * call-site structure, never by `DefaultPermissionPolicy` content -- AG-1C's
 * own documented, frozen invariant is that this policy mechanism has "no
 * per-principal matching capability at all." A `PLUGIN`, `INTERNAL_AGENT`,
 * `TOOL`, or Owner principal cannot reach either method below *as
 * themselves* -- there is no parameter through which their own identity
 * could ever be substituted for Hermes's.
 *
 * ## No duplicated logic
 *
 * This class invents no manifest, provenance, source-resolution, or
 * permission logic of its own. Each method performs exactly one
 * [PermissionEngine.evaluate] call using the Agent-Gateway-specific shape
 * above and, only if that decision is `APPROVED`/`APPROVED_WITH_CONFIRMATION`,
 * delegates unchanged to the existing, already-governed
 * [EvidenceCustodian.retrieve]/[EvidenceCustodian.retrieveManifest] -- the
 * same methods Owner UI itself already calls, via the same shared
 * [permissionEngine] instance. Those methods perform their own, separate,
 * pre-existing internal permission check under Owner's own
 * `evidence.retrieve`/`evidence.retrieve-manifest` verbs; this class adds
 * an additional, narrower, Agent-Gateway-specific gate in front of them --
 * it never replaces, weakens, or bypasses their own check.
 *
 * ## Narrow projection, not raw content
 *
 * [retrieveEvidence] deliberately projects only a byte length, never the
 * retrieved bytes themselves -- Hermes's own stated supervision need
 * (confirm a submission's presence/status across a batch) does not require
 * raw content, and returning it here would make this "thin projection
 * layer" a second content-retrieval surface the Scope Lock's own API
 * projection rules (Section 14: "never a serialized internal object")
 * caution against growing informally. [retrieveEvidenceManifest] projects
 * the manifest's own already-narrow, already-opaque fields.
 */
internal class AgentGatewayEvidenceProjection(
    private val hermesPrincipalId: PrincipalId,
    private val agentGatewayPurpose: AuthorizationPurposeId,
    private val permissionEngine: PermissionEngine,
    private val evidenceCustodian: EvidenceCustodian,
    private val clock: () -> Instant = Instant::now,
) {

    suspend fun retrieveEvidence(evidenceArtifactId: EvidenceArtifactId): AgentGatewayEvidenceRetrievalResult {
        val decision = permissionEngine.evaluate(
            buildRequest(
                resourceId = AGENT_GATEWAY_EVIDENCE_RETRIEVAL_RESOURCE_ID,
                actionName = AGENT_GATEWAY_EVIDENCE_RETRIEVE_ACTION_NAME,
                requestIdPrefix = "agent-gateway-evidence-retrieve",
                evidenceArtifactId = evidenceArtifactId,
            ),
        )
        if (!decision.isApproved()) {
            return AgentGatewayEvidenceRetrievalResult.Denied(evidenceArtifactId, decision.decision)
        }
        return when (val result = evidenceCustodian.retrieve(hermesPrincipalId, evidenceArtifactId)) {
            is EvidenceRetrievalResult.Found -> AgentGatewayEvidenceRetrievalResult.Found(
                evidenceArtifactId = result.evidenceArtifactId,
                byteLength = result.content.size,
            )
            is EvidenceRetrievalResult.NotFound -> AgentGatewayEvidenceRetrievalResult.NotFound(evidenceArtifactId)
            is EvidenceRetrievalResult.Rejected -> AgentGatewayEvidenceRetrievalResult.Denied(evidenceArtifactId, PermissionDecisionOutcome.DENIED)
        }
    }

    suspend fun retrieveEvidenceManifest(evidenceArtifactId: EvidenceArtifactId): AgentGatewayEvidenceManifestResult {
        val decision = permissionEngine.evaluate(
            buildRequest(
                resourceId = AGENT_GATEWAY_EVIDENCE_MANIFEST_RETRIEVAL_RESOURCE_ID,
                actionName = AGENT_GATEWAY_EVIDENCE_RETRIEVE_MANIFEST_ACTION_NAME,
                requestIdPrefix = "agent-gateway-evidence-retrieve-manifest",
                evidenceArtifactId = evidenceArtifactId,
            ),
        )
        if (!decision.isApproved()) {
            return AgentGatewayEvidenceManifestResult.Denied(evidenceArtifactId, decision.decision)
        }
        return when (val result = evidenceCustodian.retrieveManifest(hermesPrincipalId, evidenceArtifactId)) {
            is EvidenceManifestRetrievalResult.Found -> AgentGatewayEvidenceManifestResult.Found(
                AgentGatewayEvidenceManifestProjection(
                    evidenceArtifactId = result.manifest.evidenceArtifactId,
                    sha256 = result.manifest.sha256,
                    byteLength = result.manifest.byteLength,
                    receivedMediaType = result.manifest.receivedMediaType,
                    originalFileName = result.manifest.originalFileName,
                ),
            )
            is EvidenceManifestRetrievalResult.NotFound -> AgentGatewayEvidenceManifestResult.NotFound(evidenceArtifactId)
            is EvidenceManifestRetrievalResult.Rejected -> AgentGatewayEvidenceManifestResult.Denied(evidenceArtifactId, PermissionDecisionOutcome.DENIED)
        }
    }

    private fun PermissionDecision.isApproved(): Boolean =
        decision == PermissionDecisionOutcome.APPROVED || decision == PermissionDecisionOutcome.APPROVED_WITH_CONFIRMATION

    private fun buildRequest(
        resourceId: ResourceId,
        actionName: String,
        requestIdPrefix: String,
        evidenceArtifactId: EvidenceArtifactId,
    ): ExecutionRequest {
        val now = clock()
        return ExecutionRequest(
            requestId = RequestId("$requestIdPrefix-${evidenceArtifactId.value}-${UUID.randomUUID()}"),
            principalId = hermesPrincipalId,
            origin = RequestOrigin.AGENT,
            intent = "Agent Gateway R0 evidence supervision read",
            targetResources = listOf(resourceId),
            proposedActions = listOf(actionName),
            priority = RequestPriority.NORMAL,
            createdAt = now,
            correlationId = "$requestIdPrefix-${evidenceArtifactId.value}",
            authorizationPurpose = agentGatewayPurpose,
        )
    }

    companion object {
        const val AGENT_GATEWAY_EVIDENCE_RETRIEVE_ACTION_NAME = "agent-gateway.evidence.retrieve"
        const val AGENT_GATEWAY_EVIDENCE_RETRIEVE_MANIFEST_ACTION_NAME = "agent-gateway.evidence.retrieve-manifest"
        val AGENT_GATEWAY_EVIDENCE_RETRIEVAL_RESOURCE_ID = ResourceId("agent-gateway-evidence-retrieval")
        val AGENT_GATEWAY_EVIDENCE_MANIFEST_RETRIEVAL_RESOURCE_ID = ResourceId("agent-gateway-evidence-manifest-retrieval")
    }
}

/**
 * AG-1D's own narrow projection of [EvidenceRetrievalResult] -- never the
 * retrieved bytes, never an internal storage/manifest type. `byteLength`
 * is the one fact this projection carries beyond identity/outcome, useful
 * for Hermes to sanity-check a prior submission's size without granting
 * content access through this path.
 */
sealed class AgentGatewayEvidenceRetrievalResult {
    data class Found(val evidenceArtifactId: EvidenceArtifactId, val byteLength: Int) : AgentGatewayEvidenceRetrievalResult()
    data class NotFound(val evidenceArtifactId: EvidenceArtifactId) : AgentGatewayEvidenceRetrievalResult()
    data class Denied(val evidenceArtifactId: EvidenceArtifactId, val decision: PermissionDecisionOutcome) : AgentGatewayEvidenceRetrievalResult()
}

/** AG-1D's own narrow projection of [EvidenceManifestRetrievalResult] -- a flat copy of the manifest's own already-opaque fields, never the internal type itself. */
sealed class AgentGatewayEvidenceManifestResult {
    data class Found(val manifest: AgentGatewayEvidenceManifestProjection) : AgentGatewayEvidenceManifestResult()
    data class NotFound(val evidenceArtifactId: EvidenceArtifactId) : AgentGatewayEvidenceManifestResult()
    data class Denied(val evidenceArtifactId: EvidenceArtifactId, val decision: PermissionDecisionOutcome) : AgentGatewayEvidenceManifestResult()
}

/**
 * A flat, opaque-identifier-only projection of [parker.core.interfaces.EvidenceSourceManifest]
 * -- no filesystem path, no storage/database key, no internal object
 * reference. `receivedMediaType`/`originalFileName` remain nullable exactly
 * as the source manifest itself declares them (Scope Lock: "never inferred,
 * computed, or fabricated").
 */
data class AgentGatewayEvidenceManifestProjection(
    val evidenceArtifactId: EvidenceArtifactId,
    val sha256: String,
    val byteLength: Long,
    val receivedMediaType: String?,
    val originalFileName: String?,
)
