package parker.core.runtime

import java.security.MessageDigest
import parker.core.interfaces.*

/** Exactly the three Hermes-facing derived states; never persisted or used as a gate. */
enum class SteveReviewQueueStatus { PASS, REVIEW_REQUIRED, FAILED }

/** Read-only presentation of already-governed Parker facts for one OCR generation. */
data class SteveReviewQueueItem(
    val evidence: OwnerRegisteredEvidence,
    val caseId: CaseId?,
    val batchIds: List<String>,
    val generation: DerivativeGenerationRecord,
    val derivativeReviewState: DerivativeReviewState?,
    val humanFidelity: EffectiveHumanFidelityReviewProjectionOutcome,
    val discrepancies: List<FidelityDiscrepancyOccurrence>,
    val uncertainty: List<OcrUncertaintySpan>,
    val pageAccounting: OcrPageAccounting?,
    val processingProvenance: OcrProcessingProvenance?,
    val corrections: List<HumanCorrectedRegionTranscription>,
    val sourceConfirmedEligibility: SourceConfirmedEligibility,
    val status: SteveReviewQueueStatus,
    val failureReason: String? = null,
)

/**
 * R0 read-only queue projection. It enumerates only through the existing owner evidence listing,
 * discovers only exact-evidence OCR generations, and reads every other domain by exact identity.
 * No method here writes, accepts, assigns, reviews, corrects, or creates provenance.
 */
class SteveReviewQueueProjection internal constructor(
    private val listEvidence: suspend () -> List<OwnerRegisteredEvidence>,
    private val assignments: CaseAssignmentStorage?,
    private val cases: CaseStorage?,
    private val batches: BulkIngestionBindingCoordinator?,
    private val generations: OcrDerivativeGenerationDiscovery,
    private val contents: DerivativeContentStorage,
    private val derivativeReviews: DerivativeReviewRegistry?,
    private val fidelityReviews: HumanFidelityReviewStorage?,
    private val fidelityProjector: EffectiveHumanFidelityReviewProjector?,
    private val corrections: HumanCorrectedRepresentationStorage?,
) {
    suspend fun enumerate(limit: Int = 100): List<SteveReviewQueueItem> {
        require(limit in 1..1000)
        val evidence = listEvidence().sortedBy { it.evidenceArtifactId.value }
        val output = mutableListOf<SteveReviewQueueItem>()
        for (source in evidence) {
            val discovered = try { generations.findOcrGenerationsForEvidence(source.evidenceArtifactId) }
            catch (e: Exception) { listOf<DerivativeGenerationRecord>() }
            for (generation in discovered.sortedBy { it.derivativeGenerationId.value }) {
                if (output.size == limit) return output
                output += project(source, generation)
            }
        }
        return output
    }

    private suspend fun project(source: OwnerRegisteredEvidence, generation: DerivativeGenerationRecord): SteveReviewQueueItem {
        fun failed(reason: String, content: OcrDerivativeExtractedResult? = null) = SteveReviewQueueItem(
            source, null, emptyList(), generation, null,
            EffectiveHumanFidelityReviewProjectionOutcome.FailedClosed(
                unavailableTarget(source.evidenceArtifactId, generation), HumanFidelityEligibilityUse.SOURCE_CONFIRMED_WHOLE_GENERATION,
            ), emptyList(), content?.pageAccounting?.pageOutcomes?.flatMap { it.uncertaintySpans } ?: emptyList(),
            content?.pageAccounting, content?.processingProvenance, emptyList(),
            SourceConfirmedEligibility(SourceConfirmedEligibilityState.DENIED, SourceConfirmedDenialReason.MALFORMED_OR_UNSUPPORTED_STATE),
            SteveReviewQueueStatus.FAILED, reason,
        )
        val entry = try { contents.retrieve(generation.derivativeGenerationId) } catch (_: Exception) { null }
            ?: return failed("DERIVATIVE_CONTENT_UNAVAILABLE")
        if (entry.rootSourceEvidenceArtifactId != source.evidenceArtifactId || generation.rootSourceEvidenceArtifactId != source.evidenceArtifactId)
            return failed("SOURCE_IDENTITY_MISMATCH")
        val ocr = (entry.payload as? TierADerivativePayload.Ocr)?.value ?: return failed("OCR_CONTENT_UNAVAILABLE")
        val processing = ocr.processingProvenance ?: return failed("PROCESSING_PROVENANCE_UNAVAILABLE", ocr)
        if (processing.sourceEvidenceArtifactId != source.evidenceArtifactId || processing.sourceManifestSha256.value != source.sha256)
            return failed("SOURCE_PROVENANCE_MISMATCH", ocr)
        val target = HumanFidelityReviewTarget(
            source.evidenceArtifactId, processing.sourceManifestSha256, processing.representationSha256,
            generation.derivativeGenerationId, digest(DerivativeGenerationRecordCodec.encode(generation)),
            digest(DerivativeContentCodec.encode(entry)),
        )
        val fidelity = try {
            fidelityProjector?.project(target, HumanFidelityEligibilityUse.SOURCE_CONFIRMED_WHOLE_GENERATION)
                ?: EffectiveHumanFidelityReviewProjectionOutcome.FailedClosed(target, HumanFidelityEligibilityUse.SOURCE_CONFIRMED_WHOLE_GENERATION)
        } catch (_: Exception) { EffectiveHumanFidelityReviewProjectionOutcome.FailedClosed(target, HumanFidelityEligibilityUse.SOURCE_CONFIRMED_WHOLE_GENERATION) }
        val reviewRecords = try { fidelityReviews?.listForExactTarget(target) ?: emptyList() } catch (_: Exception) { return failed("HUMAN_FIDELITY_FACTS_UNAVAILABLE", ocr) }
        val eligibility = (fidelity as? EffectiveHumanFidelityReviewProjectionOutcome.Projected)?.summary?.projection?.eligibility
            ?: SourceConfirmedEligibility(SourceConfirmedEligibilityState.DENIED, SourceConfirmedDenialReason.MALFORMED_OR_UNSUPPORTED_STATE)
        val correctionList = try { corrections?.listForExactTarget(target) ?: emptyList() } catch (_: Exception) { return failed("CORRECTION_FACTS_UNAVAILABLE", ocr) }
        val derivativeState = try { derivativeReviews?.currentReviewState(source.evidenceArtifactId) } catch (_: Exception) { return failed("DERIVATIVE_REVIEW_FACTS_UNAVAILABLE", ocr) }
        val status = when {
            fidelity is EffectiveHumanFidelityReviewProjectionOutcome.FailedClosed -> SteveReviewQueueStatus.FAILED
            eligibility.state == SourceConfirmedEligibilityState.ALLOWED &&
                (derivativeState == null || derivativeState == DerivativeReviewState.APPROVED) -> SteveReviewQueueStatus.PASS
            derivativeState == DerivativeReviewState.REJECTED -> SteveReviewQueueStatus.FAILED
            else -> SteveReviewQueueStatus.REVIEW_REQUIRED
        }
        val assignmentStore = assignments
        val assignment = try { assignmentStore?.readAssignment(source.evidenceArtifactId) } catch (_: Exception) { return failed("CASE_ASSIGNMENT_UNRESOLVED", ocr) }
        val caseId = assignment?.caseId
        val resolvedCase = try { caseId?.takeIf { cases?.read(it) != null } } catch (_: Exception) { return failed("CASE_ID_UNRESOLVED", ocr) }
        if (caseId != null && resolvedCase == null) return failed("CASE_ID_UNRESOLVED", ocr)
        return SteveReviewQueueItem(
            source, resolvedCase, try { batches?.batchesForEvidence(source.evidenceArtifactId) ?: emptyList() } catch (_: Exception) { return failed("BULK_BINDING_FACTS_UNAVAILABLE", ocr) },
            generation, derivativeState, fidelity, reviewRecords.flatMap { it.discrepancyOccurrences },
            ocr.pageAccounting?.pageOutcomes?.flatMap { it.uncertaintySpans } ?: emptyList(), ocr.pageAccounting, processing,
            correctionList, eligibility, status,
        )
    }

    private fun unavailableTarget(evidence: EvidenceArtifactId, generation: DerivativeGenerationRecord) =
        HumanFidelityReviewTarget(evidence, digest(source = "0"), digest(source = generation.derivativeGenerationId.value), generation.derivativeGenerationId, digest(source = "1"), digest(source = "2"))

    private fun digest(bytes: ByteArray) = OcrSha256Digest(MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it.toInt() and 255) })
    private fun digest(source: String) = digest(source.toByteArray())
}
