package parker.core.runtime

import java.time.Instant
import java.util.UUID
import parker.core.interfaces.PendingAnalysisId
import parker.core.interfaces.RetrieveSavedAnalysisOutcome
import parker.core.interfaces.SaveAnalysisOutcome
import parker.core.interfaces.SavedAnalysisEvidenceReference
import parker.core.interfaces.SavedAnalysisId
import parker.core.interfaces.SavedAnalysisRecord
import parker.core.interfaces.SavedAnalysisStorage
import parker.core.interfaces.SavedAnalysisStorageException
import parker.core.interfaces.SavedAnalysisSummary

/**
 * Reviewed Analysis Result — Explicit Owner Save. The smallest coordinator
 * necessary to save an already-completed, server-held analysis by its
 * opaque [PendingAnalysisId] (never by resubmitted content -- see
 * [PendingAnalysisCache]'s own KDoc for the full anti-forgery argument),
 * and to retrieve/list already-saved analyses afterward. Structurally
 * owner-only (no caller-supplied principal anywhere on this class, mirroring
 * [DocumentAnalysisCoordinator]'s own analyse method), and deliberately
 * carries no [parker.core.interfaces.PermissionEngine] dependency: saving an
 * already-produced, already-authorised analysis result touches no Evidence,
 * no OCR, and no reasoning-model invocation, exactly the same "durable write
 * of a subordinate, non-Evidence artifact" shape [TierAOwnerInvocationCoordinator]
 * already establishes needs no Permission Engine gate of its own.
 */
class SavedAnalysisCoordinator(
    private val pendingAnalysisCache: PendingAnalysisCache,
    private val storage: SavedAnalysisStorage,
    private val now: () -> Instant = Instant::now,
    private val idFactory: () -> SavedAnalysisId = { SavedAnalysisId(UUID.randomUUID().toString()) },
) {
    /**
     * 1. Claim the exact server-held pending result (fails closed if unknown/expired/already
     *    consumed/concurrently in-flight -- never trusts any caller-submitted analysis content,
     *    since none is ever accepted). 2. Defensively re-validate bounds. 3. Mint a fresh
     * [SavedAnalysisId] and durably publish. 4. Only on genuine success, permanently consume the
     * pending entry -- a failure at step 3 releases the claim back to `AVAILABLE` so a legitimate
     * retry with the same [PendingAnalysisId] remains possible, never silently losing a
     * still-saveable reviewed result.
     */
    suspend fun save(pendingAnalysisId: PendingAnalysisId): SaveAnalysisOutcome {
        val result = when (val claimOutcome = pendingAnalysisCache.claim(pendingAnalysisId)) {
            is PendingAnalysisCache.ClaimOutcome.Claimed -> claimOutcome.result
            PendingAnalysisCache.ClaimOutcome.UnknownOrExpired -> return SaveAnalysisOutcome.UnknownOrExpiredPendingAnalysis
            PendingAnalysisCache.ClaimOutcome.AlreadyInFlight -> return SaveAnalysisOutcome.SaveAlreadyInProgress
        }

        if (result.instruction.length > DocumentAnalysisCoordinator.MAX_INSTRUCTION_CHARACTERS) {
            pendingAnalysisCache.release(pendingAnalysisId)
            return SaveAnalysisOutcome.SavedRecordTooLarge("instruction", result.instruction.length, DocumentAnalysisCoordinator.MAX_INSTRUCTION_CHARACTERS)
        }
        if (result.analysisText.length > DocumentAnalysisCoordinator.MAX_RESPONSE_CHARACTERS) {
            pendingAnalysisCache.release(pendingAnalysisId)
            return SaveAnalysisOutcome.SavedRecordTooLarge("analysisText", result.analysisText.length, DocumentAnalysisCoordinator.MAX_RESPONSE_CHARACTERS)
        }
        if (result.evidenceItems.size > DocumentAnalysisCoordinator.MAX_SELECTIONS) {
            pendingAnalysisCache.release(pendingAnalysisId)
            return SaveAnalysisOutcome.SavedRecordTooLarge("evidenceReferences", result.evidenceItems.size, DocumentAnalysisCoordinator.MAX_SELECTIONS)
        }

        val savedAnalysisId = idFactory()
        val record = SavedAnalysisRecord(
            savedAnalysisId = savedAnalysisId,
            savedAt = now(),
            analysedAt = result.analysedAt,
            instruction = result.instruction,
            analysisText = result.analysisText,
            evidenceReferences = result.evidenceItems.map {
                SavedAnalysisEvidenceReference(it.evidenceArtifactId, it.derivativeGenerationId, it.derivativeKind)
            },
            mechanismIdentity = result.mechanismIdentity,
            mechanismVersion = result.mechanismVersion,
        )

        try {
            storage.prepare(record)
            storage.publishPrepared(savedAnalysisId)
        } catch (e: SavedAnalysisStorageException) {
            pendingAnalysisCache.release(pendingAnalysisId)
            return SaveAnalysisOutcome.PersistenceFailed("Saved analysis could not be durably published")
        }

        pendingAnalysisCache.finalize(pendingAnalysisId)
        return SaveAnalysisOutcome.Saved(savedAnalysisId)
    }

    /** Never re-runs analysis, never invokes the model -- resolves durable storage only. */
    suspend fun retrieve(savedAnalysisId: SavedAnalysisId): RetrieveSavedAnalysisOutcome {
        val record = try {
            storage.retrieve(savedAnalysisId)
        } catch (e: SavedAnalysisStorageException.UnsupportedRepresentationVersion) {
            return RetrieveSavedAnalysisOutcome.UnsupportedRepresentationVersion(e.version)
        } catch (e: SavedAnalysisStorageException.CorruptRecord) {
            return RetrieveSavedAnalysisOutcome.CorruptRecord(e.message ?: "corrupt")
        }
        return record?.let { RetrieveSavedAnalysisOutcome.Retrieved(it) } ?: RetrieveSavedAnalysisOutcome.UnknownSavedAnalysis
    }

    /** The most recently saved analyses, newest first, capped at [LISTING_MAX_COUNT] -- metadata only, never full analysis text or evidence references. */
    suspend fun listRecent(): List<SavedAnalysisSummary> {
        val ids = storage.listRecentIds(LISTING_MAX_COUNT)
        return ids.mapNotNull { id ->
            when (val outcome = retrieve(id)) {
                is RetrieveSavedAnalysisOutcome.Retrieved -> outcome.record.let {
                    SavedAnalysisSummary(
                        savedAnalysisId = it.savedAnalysisId,
                        savedAt = it.savedAt,
                        instructionPreview = it.instruction.take(INSTRUCTION_PREVIEW_MAX_CHARACTERS),
                    )
                }
                // A record that vanished, corrupted, or version-mismatched between listing its id
                // and retrieving it is simply omitted from the listing -- never surfaced as a
                // half-populated or fabricated summary.
                else -> null
            }
        }
    }

    companion object {
        /** A modest, frozen bound on how many recent saved analyses one listing call returns -- no pagination architecture, no search. */
        const val LISTING_MAX_COUNT: Int = 20

        /** A frozen bound on the presentation-only instruction excerpt a listing entry carries -- the stored [SavedAnalysisRecord.instruction] itself is never truncated. */
        const val INSTRUCTION_PREVIEW_MAX_CHARACTERS: Int = 120
    }
}
