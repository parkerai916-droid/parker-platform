package parker.core.runtime

import parker.core.interfaces.*

class DefaultGovernedHumanCorrectionService(
    private val permission: HumanCorrectionPermissionEvaluator,
    private val reviews: HumanFidelityReviewStorage,
    private val reviewProjector: EffectiveHumanFidelityReviewProjector,
    private val providers: HumanCorrectionProviderResolver,
    private val corrected: HumanCorrectedRepresentationStorage,
    private val audit: HumanCorrectionAudit,
) : GovernedHumanCorrectionService {
    override suspend fun create(request: GovernedHumanCorrectionRequest): GovernedHumanCorrectionResult {
        if (request.authority.target != request.target || request.authority.reviewId != request.reviewId ||
            request.acceptance.target != request.target || request.acceptance.reviewId != request.reviewId ||
            request.acceptance.acceptingPrincipalId != request.authority.principalId) return failed(GovernedHumanCorrectionFailureReason.TARGET_OR_REVIEW_MISMATCH)
        val permissionResult = try { permission.evaluate(request.authority, request.target, request.reviewId) }
        catch (_: Exception) { return failed(GovernedHumanCorrectionFailureReason.AUTHORITY_EVALUATION_FAILED) }
        if (permissionResult is HumanCorrectionPermissionResult.Denied) return GovernedHumanCorrectionResult.AuthorizationDenied(permissionResult.reason)

        val review = try { reviews.retrieve(request.reviewId) } catch (_: Exception) { null }
            ?: return failed(GovernedHumanCorrectionFailureReason.PROVIDER_OR_REVIEW_NOT_FOUND)
        if (review.target != request.target || review.reviewState != HumanFidelityReviewState.HUMAN_REVIEWED_WITH_DISCREPANCY ||
            review.coverage.kind != HumanFidelityCoverageKind.FULL_GENERATION) return failed(GovernedHumanCorrectionFailureReason.INVALID_CORRECTION)
        val projection = try { reviewProjector.project(request.target, HumanFidelityEligibilityUse.SOURCE_CONFIRMED_WHOLE_GENERATION) }
        catch (_: Exception) { return failed(GovernedHumanCorrectionFailureReason.REVIEW_CONFLICT) }
        if (projection !is EffectiveHumanFidelityReviewProjectionOutcome.Projected || projection.summary.unresolvedConflict)
            return failed(GovernedHumanCorrectionFailureReason.REVIEW_CONFLICT)

        val resolvedProvider = try { providers.resolve(request.target) } catch (_: Exception) { null }
            ?: return failed(GovernedHumanCorrectionFailureReason.PROVIDER_OR_REVIEW_NOT_FOUND)
        if (resolvedProvider.target != request.target)
            return failed(GovernedHumanCorrectionFailureReason.INVALID_CORRECTION)
        val provider = resolvedProvider.transcription

        val material = review.discrepancyOccurrences.filter { it.severity == FidelityDiscrepancySeverity.MATERIAL }
        if (request.proposals.size != material.size || request.proposals.map { it.discrepancyId }.toSet() != material.map { it.discrepancyId }.toSet() ||
            request.acceptance.proposalIds.toSet() != request.proposals.map { it.proposalId }.toSet())
            return failed(GovernedHumanCorrectionFailureReason.INVALID_CORRECTION)
        val byDiscrepancy = review.discrepancyOccurrences.associateBy { it.discrepancyId }
        val applications = mutableListOf<Pair<FidelityDiscrepancyOccurrence, HumanTranscriptionCorrectionProposal>>()
        for (proposal in request.proposals) {
            val discrepancy = byDiscrepancy[proposal.discrepancyId] ?: return failed(GovernedHumanCorrectionFailureReason.INVALID_CORRECTION)
            val resolution = discrepancy.sourceResolution as? HumanSourceResolution.ResolvedAgainstSource
                ?: return failed(GovernedHumanCorrectionFailureReason.INVALID_CORRECTION)
            if (proposal.reviewId != review.reviewId || proposal.target != review.target ||
                proposal.providerValue != discrepancy.location.originalProviderSubstring ||
                proposal.acceptedSourceValue != resolution.assertedSourceValue)
                return failed(GovernedHumanCorrectionFailureReason.INVALID_CORRECTION)
            applications += discrepancy to proposal
        }
        val resolvedApplications = try { applications.map { (discrepancy, proposal) ->
            val regionIndex = provider.regionBindings.indexOf(discrepancy.location.derivativeRegionId.value)
            require(regionIndex >= 0 && discrepancy.location.transcriptionBlockIndex == 0)
            Triple(regionIndex, discrepancy, proposal)
        } } catch (_: Exception) { return failed(GovernedHumanCorrectionFailureReason.INVALID_CORRECTION) }
        val overlap = resolvedApplications.groupBy { it.first }.values.any { items ->
            val sorted = items.sortedBy { it.second.location.startCodePointInclusive }
            sorted.zipWithNext().any { (a, b) -> a.second.location.endCodePointExclusive > b.second.location.startCodePointInclusive }
        }
        if (overlap) return failed(GovernedHumanCorrectionFailureReason.INVALID_CORRECTION)

        val blocks = provider.transcriptionBlocks.toMutableList()
        try {
            resolvedApplications.sortedWith(compareByDescending<Triple<Int, FidelityDiscrepancyOccurrence, HumanTranscriptionCorrectionProposal>> { it.first }
                .thenByDescending { it.second.location.startCodePointInclusive }).forEach { (blockIndex, d, p) ->
                val l = d.location; val block = blocks[blockIndex]
                val start = block.offsetByCodePoints(0, l.startCodePointInclusive); val end = block.offsetByCodePoints(0, l.endCodePointExclusive)
                require(block.substring(start, end) == p.providerValue)
                blocks[blockIndex] = block.substring(0, start) + p.acceptedSourceValue + block.substring(end)
            }
        } catch (_: Exception) { return failed(GovernedHumanCorrectionFailureReason.INVALID_CORRECTION) }

        val digest = HumanCorrectedRegionTranscription.contentDigest(blocks)
        val id = HumanCorrectedRegionTranscription.deriveGenerationId(request.target, request.reviewId, request.acceptance, digest)
        val representation = try { HumanCorrectedRegionTranscription(1, id, target=request.target, reviewId=request.reviewId,
            proposals=request.proposals, acceptance=request.acceptance, correctedTranscriptionBlocks=blocks,
            correctedContentSha256=digest, createdAt=request.acceptance.acceptedAt) }
        catch (_: Exception) { return failed(GovernedHumanCorrectionFailureReason.INVALID_CORRECTION) }

        val preparation = try { corrected.prepare(representation) } catch (_: Exception) { return failed(GovernedHumanCorrectionFailureReason.STORAGE_FAILURE) }
        if (preparation != HumanCorrectedRepresentationPrepareResult.AlreadyPublished) {
            try {
                audit.append(auditRecord(HumanCorrectionAuditEventType.CORRECTED_REPRESENTATION_PREPARED, representation))
                corrected.publishPrepared(id)
            } catch (_: Exception) { return failed(GovernedHumanCorrectionFailureReason.STORAGE_FAILURE) }
        }
        val canonical = try { corrected.retrieve(id) } catch (_: Exception) { null }
            ?: return failed(GovernedHumanCorrectionFailureReason.CANONICAL_READBACK_FAILED)
        if (!HumanCorrectedRepresentationCodec.encode(canonical).contentEquals(HumanCorrectedRepresentationCodec.encode(representation)))
            return failed(GovernedHumanCorrectionFailureReason.CANONICAL_READBACK_FAILED)
        try { audit.append(auditRecord(HumanCorrectionAuditEventType.CORRECTED_REPRESENTATION_PUBLISHED, representation)) }
        catch (_: Exception) { return failed(GovernedHumanCorrectionFailureReason.STORAGE_FAILURE) }
        val auditFacts = try { audit.listForRepresentation(id) } catch (_: Exception) { emptyList() }
        if (auditFacts.map { it.eventType }.toSet() != HumanCorrectionAuditEventType.entries.toSet())
            return failed(GovernedHumanCorrectionFailureReason.STORAGE_FAILURE)
        return if (preparation == HumanCorrectedRepresentationPrepareResult.AlreadyPublished)
            GovernedHumanCorrectionResult.AlreadyCreated(canonical) else GovernedHumanCorrectionResult.Created(canonical)
    }

    private fun auditRecord(type: HumanCorrectionAuditEventType, r: HumanCorrectedRegionTranscription): HumanCorrectionAuditRecord {
        val eventId = HumanCorrectionAuditRecord.deriveId(type, r.acceptance.acceptingPrincipalId,
            r.derivativeGenerationId, r.target, r.reviewId, r.acceptance.acceptanceId, r.correctedContentSha256)
        return HumanCorrectionAuditRecord(eventId, type, r.createdAt, r.acceptance.acceptingPrincipalId,
            r.derivativeGenerationId, r.target, r.reviewId, r.acceptance.acceptanceId, r.correctedContentSha256)
    }
    private fun failed(r: GovernedHumanCorrectionFailureReason) = GovernedHumanCorrectionResult.Failed(r)
}

class StoredHumanCorrectionProviderResolver(
    private val generations: DerivativeGenerationStorage,
    private val contents: DerivativeContentStorage,
) : HumanCorrectionProviderResolver {
    override suspend fun resolve(target: HumanFidelityReviewTarget): ResolvedProviderTranscription? {
        val generation = generations.retrieve(target.derivativeGenerationId) ?: return null
        val entry = contents.retrieve(target.derivativeGenerationId) ?: return null
        val provider = (entry.payload as? TierADerivativePayload.RegionTranscription)?.value ?: return null
        if (generation.rootSourceEvidenceArtifactId != target.evidenceArtifactId ||
            digest(DerivativeGenerationRecordCodec.encode(generation)) != target.derivativeGenerationSha256.value ||
            digest(DerivativeContentCodec.encode(entry)) != target.derivativeContentSha256.value ||
            provider.evidenceArtifactId != target.evidenceArtifactId.value ||
            provider.sourceSha256 != target.sourceSha256.value ||
            provider.preparationIdentity != target.preparationIdentity.value) return null
        return ResolvedProviderTranscription(target, provider)
    }

    private fun digest(bytes: ByteArray) = java.security.MessageDigest.getInstance("SHA-256")
        .digest(bytes).joinToString("") { "%02x".format(it.toInt() and 255) }
}

class DefaultHumanCorrectedRepresentationEligibilityEvaluator(
    private val reviews: HumanFidelityReviewStorage,
    private val projector: EffectiveHumanFidelityReviewProjector,
) : HumanCorrectedRepresentationEligibilityEvaluator {
    override suspend fun evaluate(representation: HumanCorrectedRegionTranscription): SourceConfirmedEligibility {
        val review = try { reviews.retrieve(representation.reviewId) } catch (_: Exception) { null }
            ?: return denied()
        val projection = try {
            projector.project(representation.target, HumanFidelityEligibilityUse.SOURCE_CONFIRMED_WHOLE_GENERATION)
        } catch (_: Exception) { return denied() }
        if (review.target != representation.target || review.coverage.kind != HumanFidelityCoverageKind.FULL_GENERATION ||
            projection !is EffectiveHumanFidelityReviewProjectionOutcome.Projected || projection.summary.unresolvedConflict ||
            representation.proposals.map { it.discrepancyId }.toSet() != review.discrepancyOccurrences
                .filter { it.severity == FidelityDiscrepancySeverity.MATERIAL }.map { it.discrepancyId }.toSet()) return denied()
        return SourceConfirmedEligibility(SourceConfirmedEligibilityState.ALLOWED)
    }

    private fun denied() = SourceConfirmedEligibility(
        SourceConfirmedEligibilityState.DENIED,
        SourceConfirmedDenialReason.MALFORMED_OR_UNSUPPORTED_STATE,
    )
}

class HumanCorrectedRepresentationRetrievalService(
    private val storage: HumanCorrectedRepresentationStorage,
    private val eligibility: HumanCorrectedRepresentationEligibilityEvaluator,
) {
    suspend fun retrieve(id: DerivativeGenerationId): HumanCorrectedRepresentationPresentation? =
        storage.retrieve(id)?.let { HumanCorrectedRepresentationPresentation(it, eligibility.evaluate(it)) }
}
