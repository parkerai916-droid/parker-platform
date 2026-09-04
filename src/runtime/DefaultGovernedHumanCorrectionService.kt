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
        val literalRegionBlocks = try {
            require(provider.regionBindings.size == provider.transcriptionBlocks.size)
            provider.transcriptionBlocks.mapIndexed { index, block ->
                V8LiteralRegionBlock.decode(block).also {
                    require(provider.regionBindings[index].substringBefore('|') == it.regionId)
                }
            }
        } catch (_: Exception) { return failed(GovernedHumanCorrectionFailureReason.INVALID_CORRECTION) }
        val resolvedApplications = try { applications.map { (discrepancy, proposal) ->
            require(discrepancy.location.transcriptionBlockIndex == 0)
            val matching = literalRegionBlocks.withIndex()
                .filter { it.value.regionId == discrepancy.location.derivativeRegionId.value }
            require(matching.size == 1 && matching.single().value.pageNumber == discrepancy.location.pageNumber)
            Triple(matching.single().index, discrepancy, proposal)
        } } catch (_: Exception) { return failed(GovernedHumanCorrectionFailureReason.INVALID_CORRECTION) }
        val overlap = resolvedApplications.groupBy { it.first }.values.any { items ->
            val sorted = items.sortedBy { it.second.location.startCodePointInclusive }
            sorted.zipWithNext().any { (a, b) -> a.second.location.endCodePointExclusive > b.second.location.startCodePointInclusive }
        }
        if (overlap) return failed(GovernedHumanCorrectionFailureReason.INVALID_CORRECTION)

        val blocks = literalRegionBlocks.toMutableList()
        try {
            resolvedApplications.sortedWith(compareByDescending<Triple<Int, FidelityDiscrepancyOccurrence, HumanTranscriptionCorrectionProposal>> { it.first }
                .thenByDescending { it.second.location.startCodePointInclusive }).forEach { (blockIndex, d, p) ->
                val l = d.location; val block = blocks[blockIndex]
                blocks[blockIndex] = block.copy(literalText = replaceExactCodePointRange(
                    block.literalText, l.startCodePointInclusive, l.endCodePointExclusive,
                    p.providerValue, p.acceptedSourceValue,
                ))
            }
        } catch (_: Exception) { return failed(GovernedHumanCorrectionFailureReason.INVALID_CORRECTION) }
        val encodedBlocks = blocks.map(V8LiteralRegionBlock::encode)

        val digest = HumanCorrectedRegionTranscription.contentDigest(encodedBlocks)
        val id = HumanCorrectedRegionTranscription.deriveGenerationId(request.target, request.reviewId, request.acceptance, digest)
        val representation = try { HumanCorrectedRegionTranscription(1, id, target=request.target, reviewId=request.reviewId,
            proposals=request.proposals, acceptance=request.acceptance, correctedTranscriptionBlocks=encodedBlocks,
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

private const val V8_BLOCK_FIELD_SEPARATOR = '\u001f'

private data class V8LiteralRegionBlock(
    val regionId: String,
    val pageNumber: Int,
    val literalText: String,
    val status: String,
    val uncertainties: String,
    val warnings: String,
) {
    init {
        require(regionId.isNotBlank() && pageNumber > 0)
        require(listOf(regionId, literalText, status, uncertainties, warnings).none { V8_BLOCK_FIELD_SEPARATOR in it })
    }

    fun encode(): String = listOf(regionId, pageNumber.toString(), literalText, status, uncertainties, warnings)
        .joinToString(V8_BLOCK_FIELD_SEPARATOR.toString())

    companion object {
        fun decode(encoded: String): V8LiteralRegionBlock {
            val fields = encoded.split(V8_BLOCK_FIELD_SEPARATOR, limit = 6)
            require(fields.size == 6)
            return V8LiteralRegionBlock(fields[0], fields[1].toInt(), fields[2], fields[3], fields[4], fields[5])
        }
    }
}

internal fun replaceExactCodePointRange(
    literalText: String,
    startCodePointInclusive: Int,
    endCodePointExclusive: Int,
    expectedProviderValue: String,
    acceptedSourceValue: String,
): String {
    require(startCodePointInclusive >= 0 && endCodePointExclusive > startCodePointInclusive)
    require(endCodePointExclusive <= literalText.codePointCount(0, literalText.length))
    val start = literalText.offsetByCodePoints(0, startCodePointInclusive)
    val end = literalText.offsetByCodePoints(0, endCodePointExclusive)
    require(literalText.substring(start, end) == expectedProviderValue)
    return literalText.substring(0, start) + acceptedSourceValue + literalText.substring(end)
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

    /** Fail closed on conflicting corrected representations for one immutable target. */
    suspend fun retrieveForExactTarget(target: HumanFidelityReviewTarget): HumanCorrectedRepresentationPresentation? {
        val matches = storage.listForExactTarget(target)
        if (matches.isEmpty()) return null
        require(matches.size == 1) { "Ambiguous corrected representations for exact target" }
        return matches.single().let { HumanCorrectedRepresentationPresentation(it, eligibility.evaluate(it)) }
    }
}
