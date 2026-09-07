package parker.core.interfaces

import java.time.Instant

/**
 * Parker Agent Gateway, AG-1E (R0 Agent Gateway Transport,
 * `docs/architecture/PARKER_AGENT_GATEWAY_SCOPE_LOCK.md` Section 12, Section
 * 20 bullet 5). Section 12's own governance-collision finding is that no
 * canonical `AuditService` implementation exists anywhere in this
 * repository (`src/interfaces/AuditService.kt` is an unimplemented stub) --
 * the live audit mechanism is domain-fragmented by design, one narrow,
 * purpose-built, append-only `FileSystem*Audit`-pattern port per subsystem
 * (mirroring [EvidenceDeletionAudit]/[DocumentIngestionAudit] exactly).
 * This port is that same pattern applied to Section 12's own named gap:
 * "authentication success/failure, a malformed/rejected request, and the
 * permission decision for a call that never reaches a domain coordinator at
 * all... have no existing writer." It is additive to, never a replacement
 * for, the existing per-domain audits -- it never re-records what
 * `DocumentIngestionAudit`/`EvidenceDeletionAudit`/`CaseGovernanceAudit`
 * already record once a request reaches their own coordinator.
 *
 * ## One record type, one boundary
 *
 * [AgentGatewayAccessAuditRecord] is the only record type this port ever
 * produces, written once per inbound Agent Gateway HTTP request, at the
 * point that request's outcome is finally known (authentication rejection,
 * malformed identifier, permission denial, not-found, success, or internal
 * failure) -- never re-derived or corrected after the fact.
 */
enum class AgentGatewayAccessOutcome {
    /** No credential was presented at all. */
    UNAUTHENTICATED,

    /** A credential was presented but did not match the configured Gateway token. */
    AUTHENTICATION_FAILED,

    /** The route matched no known Agent Gateway operation. */
    NOT_FOUND_ROUTE,

    /** A path-supplied identifier failed typed-id construction before any operation ran. */
    MALFORMED_IDENTIFIER,

    /** [PermissionEngine] did not approve the request. */
    DENIED,

    /** The request was approved but no data exists under the requested identity. */
    NOT_FOUND,

    /** The request was approved and the requested projection was returned. */
    APPROVED,

    /** An unexpected failure occurred after authentication succeeded. */
    INTERNAL_FAILURE,

    /**
     * Parker Agent Gateway, AG-1F (R1 Candidate-Source Submission). The request body failed a
     * pre-registration validity check (empty, malformed advisory hash shape, or oversized) before
     * [EvidenceCustodian.submitSource] was ever called.
     */
    INVALID_SOURCE,

    /** AG-1F. A submitted source's authoritative SHA-256 had never been registered before -- newly, durably accepted. */
    REGISTERED,

    /** AG-1F. A submitted source's authoritative SHA-256 was already registered -- no new identity was minted. */
    ALREADY_REGISTERED,

    /** AG-1F. The caller's advisory SHA-256 disagreed with Parker's own computed authoritative hash -- nothing was registered. */
    HASH_MISMATCH,

    /**
     * Crash-safe idempotency review correction (AG-1F). The source-identity reservation for the
     * submitted content's authoritative SHA-256 named an identity whose existing canonical
     * Evidence Custodian state did not match what this submission would produce -- an internal
     * consistency fault, failed closed. Never an ordinary outcome of resubmitting identical bytes.
     */
    SOURCE_IDENTITY_CONFLICT,
}

/**
 * One durable fact about one inbound Agent Gateway HTTP request. Section
 * 12's own recommended minimum field list: "agent principal id,
 * request/session/correlation identity..., requested Parker operation (verb
 * phrase), exact resource/evidence target, permission decision where
 * applicable, execution result, and timestamp."
 *
 * @param principalId The resolved caller principal, if authentication
 *   succeeded -- `null` for [AgentGatewayAccessOutcome.UNAUTHENTICATED]/
 *   [AgentGatewayAccessOutcome.AUTHENTICATION_FAILED], since no principal
 *   was ever resolved for those two outcomes. Every non-null value is
 *   Hermes's own fixed principal (AG-1E's authentication resolves to no
 *   other) -- this port does not itself enforce that; it records whatever
 *   [parker.composition.AgentGatewayHttpServer] resolved.
 * @param correlationId Minted once per inbound HTTP request by
 *   [parker.composition.AgentGatewayHttpServer], threaded through
 *   explicitly to this one record -- Section 12's own named gap ("today's
 *   domain audits do not do this uniformly, and the Gateway must not
 *   repeat that gap for its own boundary events").
 * @param operation The exact AG-1C `proposedAction` verb phrase this
 *   request resolved to, e.g. `"agent-gateway.evidence.retrieve"` -- `null`
 *   if the request never resolved to a known operation
 *   ([AgentGatewayAccessOutcome.UNAUTHENTICATED]/
 *   [AgentGatewayAccessOutcome.AUTHENTICATION_FAILED]/
 *   [AgentGatewayAccessOutcome.NOT_FOUND_ROUTE]).
 * @param targetId The exact opaque target identifier this request named,
 *   e.g. an [EvidenceArtifactId]'s own `.value` -- `null` if no identifier
 *   was ever parsed.
 */
data class AgentGatewayAccessAuditRecord(
    val principalId: PrincipalId?,
    val correlationId: String,
    val operation: String?,
    val targetId: String?,
    val outcome: AgentGatewayAccessOutcome,
    val recordedAt: Instant,
) {
    init {
        require(correlationId.isNotBlank()) { "AgentGatewayAccessAuditRecord.correlationId must not be blank" }
    }
}

/**
 * Typed failure for [AgentGatewayAccessAudit.record] -- thrown, not
 * returned as a result type, mirroring [EvidenceDeletionAuditException]'s
 * own established convention for a genuine, unexpected fault at this layer.
 */
sealed class AgentGatewayAccessAuditException(message: String, cause: Throwable? = null) :
    RuntimeException(message, cause) {

    /** A durable write genuinely failed -- disk I/O, permissions, or an equivalent fault. */
    class PersistenceFailure(message: String, cause: Throwable) : AgentGatewayAccessAuditException(message, cause)
}

/**
 * The one operation this port exposes. Durably persists [record] before
 * returning; throws [AgentGatewayAccessAuditException.PersistenceFailure]
 * if it cannot. Never overwrites or removes a previously written record --
 * append-only, exactly as Section 12's own "durable, append-only audit
 * record" recommendation requires.
 */
interface AgentGatewayAccessAudit {
    suspend fun record(record: AgentGatewayAccessAuditRecord)
}
