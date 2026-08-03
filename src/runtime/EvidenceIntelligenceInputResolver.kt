package parker.core.runtime

import parker.core.interfaces.AssertionId
import parker.core.interfaces.DocumentId
import parker.core.interfaces.EntityId
import parker.core.interfaces.EvidenceAnalysisRequest
import parker.core.interfaces.EvidenceCustodian
import parker.core.interfaces.EvidenceRetrievalResult
import parker.core.interfaces.MemoryCoreRecord
import parker.core.interfaces.MemoryRetrieval
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.RelationshipEndpoint
import parker.core.interfaces.RelationshipId

/**
 * Evidence Intelligence, Implementation Unit 2 ("Governed Input
 * Resolution"). Governed in full by
 * `docs/implementation/EVIDENCE_INTELLIGENCE_IMPLEMENTATION_PLAN.md`
 * ("the Implementation Plan"), §8 Unit 2 -- this class implements exactly
 * that Unit's own responsibilities, and only those.
 *
 * ## What this Unit implements
 *
 * Resolution, and only resolution, of an [EvidenceAnalysisRequest]'s two
 * reference lists through the two existing read boundaries the
 * Implementation Plan names -- [EvidenceCustodian.retrieve] for
 * [EvidenceAnalysisRequest.evidenceArtifactIds], and [MemoryRetrieval]
 * for [EvidenceAnalysisRequest.memoryCoreReferences] -- with per-input
 * identity preserved throughout, exactly as Unit 1's own governance
 * remediation (`GCR-EI-UNIT2-001`) required before this Unit was
 * authorised to begin. The two boundaries are not equally gated today --
 * see "`MemoryRetrieval`'s permission truth," below -- so this Unit
 * deliberately does not describe both as "already-gated" alike.
 *
 * `internal`, not public -- this is Unit 2 implementation plumbing, not
 * one of the four public runtime types the amended Contract Design and
 * Scope Lock authorise (`EvidenceAnalysisRequest`, `EvidenceAnalysisResult`,
 * the "Candidate record produced" payload selector, and the future
 * `EvidenceIntelligence` operation). No interface, and no public
 * replacement type, is introduced to compensate -- callers within this
 * module construct and call this class directly.
 *
 * A concrete class, not an interface -- mirroring `EvidenceRegistrationCoordinator`'s
 * own precedent for "composition-level sequencing of existing boundaries,
 * with no new public contract of its own" (Implementation Plan §5). This
 * is deliberately not one of the Implementation Plan's own named "New
 * Components" (§6) -- it is ordinary internal resolution plumbing
 * fulfilling an already-authorised responsibility, introducing no new
 * public contract, no new interface, and no new type of any kind.
 *
 * ## Why the return shape is a bare `Pair` of two lists, never a new type
 *
 * [resolve] returns
 * `Pair<List<EvidenceRetrievalResult>, List<Pair<RelationshipEndpoint, MemoryCoreRecord?>>>`
 * -- composed entirely from existing Kotlin stdlib (`Pair`, `List`) and
 * existing Parker types ([EvidenceRetrievalResult], [RelationshipEndpoint],
 * [MemoryCoreRecord]). No new public contract is created to wrap this
 * shape, consistent with this Unit's own governing instruction.
 *
 * ## Per-input identity, preserved for both boundaries, not only the one
 * `GCR-EI-UNIT2-001` originally found defective
 *
 * Every [EvidenceRetrievalResult] value ([EvidenceRetrievalResult.Found],
 * [EvidenceRetrievalResult.NotFound], and -- since the Evidence Custodian
 * retrieval contract amendment resolving `GCR-EI-UNIT2-001` --
 * [EvidenceRetrievalResult.Rejected]) already carries its own
 * `evidenceArtifactId`, so the evidence-artefact half of [resolve]'s
 * return value is self-identifying by construction; no change was needed
 * here to achieve that.
 *
 * [MemoryRetrieval]'s own four direct-lookup methods, by contrast, each
 * return a bare nullable record (`Entity?`, `Document?`, `Assertion?`,
 * `Relationship?`) with no disposition type of any kind, and a `null`
 * result carries no data identifying which reference produced it.
 * Modifying [MemoryRetrieval] is out of scope -- it is Memory Core's own
 * contract, reused unmodified, per this Programme's dependency freeze.
 * Relying on list position to recover that identity would repeat exactly
 * the defect `GCR-EI-UNIT2-001` identified and rejected for
 * [EvidenceRetrievalResult.Rejected]. Instead, each Memory Core
 * resolution result is paired, per entry, with the [RelationshipEndpoint]
 * that produced it (`endpoint to result`) -- self-identifying on both
 * the found and the not-found path, using only the existing
 * [RelationshipEndpoint] type and the stdlib [Pair] it is carried in.
 * This is an internal implementation choice within this Unit's own
 * discretion, not a change to [MemoryRetrieval] or to any governed
 * contract.
 *
 * ## `MemoryRetrieval`'s permission truth
 *
 * Inspected directly, not assumed: [MemoryRetrieval]'s own KDoc
 * (`src/interfaces/MemoryCore.kt`) states its `PrincipalId` parameters
 * "exist for auditability only... A caller receiving more than it should
 * ever see is prevented entirely externally, by a future
 * permission-filtering decorator (`PermissionFilteredMemoryRetrieval`,
 * not implemented by this Unit)." `InMemoryMemoryCore` -- the sole
 * concrete implementation of [MemoryRetrieval] in this repository --
 * confirms this directly: its own KDoc states it performs "no permission
 * evaluation or owner-authority check (entirely external...); a future
 * `PermissionGatedMemoryCore`/`PermissionFilteredMemoryRetrieval`
 * decorator, not built here, is where that lives," its constructor takes
 * no `PermissionEngine` dependency at all, and
 * `InMemoryMemoryCoreTest.kt` carries a structural test proving exactly
 * that. No `PermissionFilteredMemoryRetrieval`, or any other
 * permission-gating decorator over [MemoryRetrieval], exists anywhere in
 * this repository today, and `ParkerRuntime.kt`'s current composition
 * root never references the [MemoryRetrieval] type at all. The Memory
 * Core Scope Lock's own Risks section independently discloses the same
 * design as deliberate, not an oversight: "Memory Core itself has no
 * internal safety net by design... the future Implementation Plan must
 * additionally specify, and test at the composition level, exactly
 * where Runtime's own permission gate is wired."
 *
 * **Conclusion: (B) permission gating for [MemoryRetrieval] must be
 * supplied later by the composing caller or a decorator, never by
 * [MemoryRetrieval] itself.** Consequently:
 *
 * - This Unit implements resolution logic only -- it does not, and
 *   cannot, supply the missing permission gate itself; doing so would be
 *   a new Permission Engine invocation this Unit's own scope excludes.
 * - This class is `internal`, not public.
 * - **This resolver is not production-reachable until the required,
 *   governed `MemoryRetrieval` composition (a permission-gating
 *   decorator, wired at the composition root) is provided.** Injecting
 *   an ungated `MemoryRetrieval` -- `InMemoryMemoryCore` as it exists
 *   today -- into a running system would let any caller able to
 *   construct an `EvidenceAnalysisRequest` read Memory Core records with
 *   no permission evaluation at all.
 * - This Unit does not implement that composition. Providing it is a
 *   Memory Core-tier or Unit 8 (Runtime Composition) concern, outside
 *   this Unit's own scope.
 *
 * ## What this Unit deliberately does not implement
 *
 * - **No reasoning.** No [parker.core.interfaces.ReasoningProvider] is
 *   invoked anywhere -- that is Unit 3's own responsibility.
 * - **No orchestration.** Resolution is sequential, one boundary call per
 *   referenced input; no coroutine concurrency, retry, or multi-step
 *   sequencing of independent subsystems is introduced.
 * - **No acceptance, no candidate production.** Nothing here calls
 *   `EvidenceCustodian.accept`, `MemoryCore`'s write interface, or
 *   Knowledge Memory's Knowledge Submission interface, and nothing here
 *   constructs an `EvidenceAnalysisResult` value of any kind -- that is
 *   Unit 4's (output discipline) and Unit 5's (the operation) own
 *   responsibility.
 * - **No runtime composition.** This class is not wired into
 *   `ParkerRuntime` or any other composition root by this Unit -- that is
 *   Unit 8's own responsibility, and per "`MemoryRetrieval`'s permission
 *   truth" above, Unit 8 cannot wire this class to a real
 *   `MemoryRetrieval` implementation until a permission-gating decorator
 *   for it exists.
 * - **No Permission Engine invocation of its own.** This class holds no
 *   `PermissionEngine` reference and never calls one directly; the only
 *   permission evaluation reachable from here today is the one already
 *   inside [EvidenceCustodian.retrieve]'s own existing implementation --
 *   [MemoryRetrieval] contributes none (above).
 *
 * ## Rejecting a request naming nothing to analyse
 *
 * Contract Design §11 requires a request naming no retrievable evidence
 * and no resolvable Memory Core reference to be rejected "before invoking
 * any analytical or Reasoning Provider step." [resolve] enforces this as
 * its own first action, via `require` -- the same construction-time
 * precondition idiom every other type in this repository already uses
 * (for example, [EvidenceAnalysisRequest.analysisKind]'s own blank
 * check) -- rather than a new result type or exception class.
 */
internal class EvidenceIntelligenceInputResolver(
    private val evidenceCustodian: EvidenceCustodian,
    private val memoryRetrieval: MemoryRetrieval,
) {

    /**
     * Resolves every reference [request] names, through
     * [EvidenceCustodian.retrieve] and [MemoryRetrieval] only, in the
     * order each list is declared. Requires at least one of
     * [EvidenceAnalysisRequest.evidenceArtifactIds] or
     * [EvidenceAnalysisRequest.memoryCoreReferences] to be non-empty.
     */
    suspend fun resolve(
        request: EvidenceAnalysisRequest,
    ): Pair<List<EvidenceRetrievalResult>, List<Pair<RelationshipEndpoint, MemoryCoreRecord?>>> {
        require(request.evidenceArtifactIds.isNotEmpty() || request.memoryCoreReferences.isNotEmpty()) {
            "EvidenceAnalysisRequest must name at least one evidence artefact or Memory Core " +
                "reference -- a request naming neither has nothing to analyse"
        }

        val evidenceResults = request.evidenceArtifactIds.map { evidenceArtifactId ->
            evidenceCustodian.retrieve(request.requestingPrincipalId, evidenceArtifactId)
        }

        val memoryCoreResults = request.memoryCoreReferences.map { endpoint ->
            endpoint to resolveMemoryCoreReference(request.requestingPrincipalId, endpoint)
        }

        return evidenceResults to memoryCoreResults
    }

    /**
     * Dispatches [endpoint] to the one [MemoryRetrieval] direct-lookup
     * method its own [RelationshipEndpoint.recordKind] names, converting
     * the found record (if any) into the existing [MemoryCoreRecord]
     * wrapper unchanged. A recognised kind with no record under the
     * supplied identifier resolves to `null` -- an ordinary, expected
     * not-found outcome.
     *
     * A [RelationshipEndpoint.recordKind] Memory Core does not itself own
     * (`conversation-turn`, `knowledge-record`, or any other
     * caller-defined value) is a different case entirely, and is never
     * conflated with an ordinary not-found: it fails explicitly, via
     * [IllegalArgumentException] -- this repository's own existing
     * argument-failure convention (for example,
     * [EvidenceAnalysisRequest.analysisKind]'s own blank check) -- since
     * no [MemoryRetrieval] method exists to answer it, and inventing one
     * is explicitly this Unit's own excluded capability ("no code path
     * resolves ... by any means other than the two named boundaries").
     * No [MemoryRetrieval] method, including the six query-based methods
     * this Unit never calls, is reached for an unsupported kind.
     */
    private suspend fun resolveMemoryCoreReference(
        requestingPrincipalId: PrincipalId,
        endpoint: RelationshipEndpoint,
    ): MemoryCoreRecord? = when (endpoint.recordKind) {
        RelationshipEndpoint.ENTITY ->
            memoryRetrieval.getEntity(requestingPrincipalId, EntityId(endpoint.recordId))
                ?.let { MemoryCoreRecord.OfEntity(it) }

        RelationshipEndpoint.DOCUMENT ->
            memoryRetrieval.getDocument(requestingPrincipalId, DocumentId(endpoint.recordId))
                ?.let { MemoryCoreRecord.OfDocument(it) }

        RelationshipEndpoint.ASSERTION ->
            memoryRetrieval.getAssertion(requestingPrincipalId, AssertionId(endpoint.recordId))
                ?.let { MemoryCoreRecord.OfAssertion(it) }

        RelationshipEndpoint.RELATIONSHIP ->
            memoryRetrieval.getRelationship(requestingPrincipalId, RelationshipId(endpoint.recordId))
                ?.let { MemoryCoreRecord.OfRelationship(it) }

        else -> throw IllegalArgumentException(
            "RelationshipEndpoint.recordKind '${endpoint.recordKind}' is not a Memory-Core-owned " +
                "kind resolvable through MemoryRetrieval (recordId='${endpoint.recordId}')",
        )
    }
}
