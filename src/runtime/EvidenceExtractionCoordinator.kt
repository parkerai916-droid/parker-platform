package parker.core.runtime

import java.security.MessageDigest
import java.time.Instant
import java.util.UUID
import parker.core.interfaces.CandidateEvidenceArtifact
import parker.core.interfaces.CandidateProvenance
import parker.core.interfaces.ContentNature
import parker.core.interfaces.DerivativeReviewRecord
import parker.core.interfaces.DerivativeReviewRegistry
import parker.core.interfaces.DerivativeReviewState
import parker.core.interfaces.DocumentId
import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.EvidenceCustodian
import parker.core.interfaces.EvidenceExtractor
import parker.core.interfaces.EvidenceRetrievalResult
import parker.core.interfaces.ExtractionOutcome
import parker.core.interfaces.ExtractionResult
import parker.core.interfaces.MemoryRetrieval
import parker.core.interfaces.PrincipalId

/**
 * Evidence Processing (Searchable PDF), Implementation Units 4A ("Identity,
 * Integrity, and Extraction") and 4B ("Derivative Creation, Registration,
 * and Review"). Governed in full by
 * `docs/architecture/EVIDENCE_PROCESSING_SEARCHABLE_PDF_BOUNDARY_CLARIFICATION.md`
 * ("the Boundary Clarification") Sections 4, 5, 7, 9; by
 * `docs/architecture/EVIDENCE_PROCESSING_SEARCHABLE_PDF_SCOPE_LOCK.md`
 * ("the Scope Lock") Sections 5, 6, 8; and by
 * `docs/architecture/EVIDENCE_PROCESSING_SEARCHABLE_PDF_IMPLEMENTATION_PLAN.md`
 * ("the Implementation Plan") Units 4A/4B. The 4A/4B split is a commit
 * boundary only, restated here as a single file and a single class: one
 * five-dependency constructor, one public operation.
 *
 * ## Exactly five dependencies, no `PermissionEngine`
 *
 * [evidenceCustodian], [memoryRetrieval], [evidenceExtractor],
 * [evidenceRegistrationCoordinator], and [derivativeReviewRegistry] are
 * this class's only five dependencies (Scope Lock Section 5). Every
 * governed act this class triggers is already gated by one of these five
 * dependencies' own existing contracts (`EvidenceCustodian.retrieve`,
 * `EvidenceRegistrationCoordinator.register`) -- this class introduces no
 * new Permission Engine evaluation of its own (Boundary Clarification
 * Section 7).
 *
 * ## Sequence (Boundary Clarification Section 7)
 *
 * 1. [MemoryRetrieval.getDocument] -- absent -> [EvidenceExtractionOutcome.SourceDocumentNotFound].
 * 2. Compare the found Document's own `locationReference` against the
 *    supplied [EvidenceArtifactId] -- mismatch ->
 *    [EvidenceExtractionOutcome.SourceIdentityMismatch], before any
 *    retrieval occurs (Boundary Clarification Section 5).
 * 3. [EvidenceCustodian.retrieve] -- rejected or not found -> the
 *    corresponding outcome, wrapping [EvidenceRetrievalResult] unchanged.
 * 4. Compute the retrieved bytes' own SHA-256, then separately verify it
 *    against `Document.integrityHash`, producing one of exactly three
 *    [IntegrityVerificationOutcome] values. Only [IntegrityVerificationOutcome.Verified]
 *    continues; [IntegrityVerificationOutcome.Mismatch] and
 *    [IntegrityVerificationOutcome.Unverifiable] both fail closed
 *    identically -- no extraction is attempted, no candidate of any kind
 *    is constructed (Scope Lock Section 6).
 * 5. [EvidenceExtractor.extract] -- `RequiresOcr`/`Unsupported`/`Malformed`
 *    each stop the pipeline, wrapping the corresponding [ExtractionOutcome]
 *    unchanged; only [ExtractionOutcome.Extracted] continues.
 * 6. Build a new [CandidateEvidenceArtifact] (the extracted text) and
 *    [CandidateProvenance] (content nature `EXTRACTED`, `extractedFrom`
 *    set to the original's own [DocumentId] only now that both identity
 *    and integrity verification have already succeeded, every
 *    reproducibility fact recorded per Boundary Clarification Section 4's
 *    table, no confidence value fabricated).
 * 7. [EvidenceRegistrationCoordinator.register] -- unchanged, exactly as
 *    any other caller would call it. This class adds no new parameter,
 *    method, or outcome variant to that existing, frozen coordinator.
 * 8. On [EvidenceRegistrationOutcome.Registered], record an initial
 *    [DerivativeReviewState.PENDING_REVIEW] state via
 *    [DerivativeReviewRegistry.recordReviewState]. On any other
 *    registration outcome, no review state is recorded at all (Boundary
 *    Clarification Section 9).
 * 9. Return [EvidenceExtractionOutcome.Completed], carrying both the
 *    registration outcome and the extraction result.
 *
 * ## Faults are never swallowed
 *
 * No `try`/`catch` appears anywhere in this class's own operation --
 * mirroring [EvidenceRegistrationCoordinator.register]'s own identical
 * discipline. A genuine exception thrown by any of the five dependencies
 * propagates unchanged, terminating the call.
 *
 * @param evidenceCustodian Called exactly once per [extract] call that
 *   reaches step 3, for retrieval only -- never for acceptance.
 * @param memoryRetrieval Called exactly once per [extract] call, first, to
 *   resolve the original Document.
 * @param evidenceExtractor Called exactly once per [extract] call that
 *   reaches step 5, only after integrity verification has already
 *   succeeded.
 * @param evidenceRegistrationCoordinator Called exactly once per [extract]
 *   call that reaches step 7, only after a successful extraction. This
 *   class never calls [EvidenceCustodian.accept] or `MemoryCore.createProvenance`/
 *   `registerDocument` directly -- both remain [evidenceRegistrationCoordinator]'s
 *   own, unchanged responsibility.
 * @param derivativeReviewRegistry Called exactly once per [extract] call
 *   whose registration succeeds, to record the initial `PENDING_REVIEW`
 *   state. Never called for any other outcome.
 */
class EvidenceExtractionCoordinator(
    private val evidenceCustodian: EvidenceCustodian,
    private val memoryRetrieval: MemoryRetrieval,
    private val evidenceExtractor: EvidenceExtractor,
    private val evidenceRegistrationCoordinator: EvidenceRegistrationCoordinator,
    private val derivativeReviewRegistry: DerivativeReviewRegistry,
) {

    suspend fun extract(
        requestingPrincipalId: PrincipalId,
        documentId: DocumentId,
        evidenceArtifactId: EvidenceArtifactId,
    ): EvidenceExtractionOutcome {
        val document = memoryRetrieval.getDocument(requestingPrincipalId, documentId)
            ?: return EvidenceExtractionOutcome.SourceDocumentNotFound(documentId)

        if (document.locationReference != evidenceArtifactId.value) {
            return EvidenceExtractionOutcome.SourceIdentityMismatch(
                documentId = documentId,
                evidenceArtifactId = evidenceArtifactId,
            )
        }

        val retrievalResult = evidenceCustodian.retrieve(requestingPrincipalId, evidenceArtifactId)
        val content = when (retrievalResult) {
            is EvidenceRetrievalResult.Rejected -> return EvidenceExtractionOutcome.SourceRetrievalRejected(retrievalResult)
            is EvidenceRetrievalResult.NotFound -> return EvidenceExtractionOutcome.SourceNotFound(retrievalResult)
            is EvidenceRetrievalResult.Found -> retrievalResult.content
        }

        val computedDigest = sha256Hex(content)
        val integrityOutcome = verifyIntegrity(computedDigest, document.integrityHash)
        when (integrityOutcome) {
            is IntegrityVerificationOutcome.Mismatch -> return EvidenceExtractionOutcome.IntegrityMismatch(integrityOutcome)
            is IntegrityVerificationOutcome.Unverifiable -> return EvidenceExtractionOutcome.IntegrityUnverifiable(integrityOutcome)
            IntegrityVerificationOutcome.Verified -> Unit
        }

        val extractionOutcome = evidenceExtractor.extract(content)
        val extractionResult: ExtractionResult = when (extractionOutcome) {
            is ExtractionOutcome.RequiresOcr -> return EvidenceExtractionOutcome.RequiresOcr(extractionOutcome)
            is ExtractionOutcome.Unsupported -> return EvidenceExtractionOutcome.Unsupported(extractionOutcome)
            is ExtractionOutcome.Malformed -> return EvidenceExtractionOutcome.Malformed(extractionOutcome)
            is ExtractionOutcome.Extracted -> extractionOutcome.result
        }

        val derivativeContent = extractionResult.extractedText.toByteArray(Charsets.UTF_8)
        val derivativeDigest = sha256Hex(derivativeContent)

        val candidateEvidenceArtifact = CandidateEvidenceArtifact(derivativeContent)
        val candidateProvenance = CandidateProvenance(
            sourceIdentifier = evidenceArtifactId.value,
            sourceType = "extraction",
            acquisitionTime = extractionResult.extractedAt,
            contentNature = ContentNature.EXTRACTED,
            creator = "${extractionResult.extractorName} ${extractionResult.extractorVersion}",
            extractedFrom = documentId,
            processingHistory = buildProcessingHistory(computedDigest, extractionResult),
            confidence = null,
        )

        val registrationOutcome = evidenceRegistrationCoordinator.register(
            requestingPrincipalId = requestingPrincipalId,
            correlationId = UUID.randomUUID().toString(),
            candidateEvidenceArtifact = candidateEvidenceArtifact,
            candidateProvenance = candidateProvenance,
            documentType = EXTRACTED_TEXT_DOCUMENT_TYPE,
            documentIntegrityHash = derivativeDigest,
            documentMetadata = extractionResult.documentMetadata,
        )

        if (registrationOutcome is EvidenceRegistrationOutcome.Registered) {
            derivativeReviewRegistry.recordReviewState(
                DerivativeReviewRecord(
                    evidenceArtifactId = registrationOutcome.acceptedEvidenceArtifact.evidenceArtifactId,
                    documentId = registrationOutcome.document.documentId,
                    state = DerivativeReviewState.PENDING_REVIEW,
                    recordedAt = Instant.now(),
                ),
            )
        }

        return EvidenceExtractionOutcome.Completed(
            registrationOutcome = registrationOutcome,
            extractionResult = extractionResult,
        )
    }

    private fun verifyIntegrity(computedDigest: String, recordedDigest: String?): IntegrityVerificationOutcome = when {
        recordedDigest == null -> IntegrityVerificationOutcome.Unverifiable(
            reason = "No integrity hash is recorded for the original Document -- integrity cannot be verified",
        )
        recordedDigest != computedDigest -> IntegrityVerificationOutcome.Mismatch(
            computedDigest = computedDigest,
            recordedDigest = recordedDigest,
        )
        else -> IntegrityVerificationOutcome.Verified
    }

    /**
     * One structured entry per Boundary Clarification Section 4 table row
     * that this class itself, rather than [EvidenceExtractor], is
     * responsible for recording -- [ExtractionIdentity]'s own three fields
     * serialised as one fixed-form entry, never free prose (Boundary
     * Clarification Section 4, "Extraction identity is one structured
     * record").
     */
    private fun buildProcessingHistory(originalEvidenceDigest: String, extractionResult: ExtractionResult): List<String> {
        val identity = extractionResult.extractionIdentity
        val warningsEntry = if (extractionResult.warnings.isEmpty()) {
            "warnings=none"
        } else {
            "warnings=${extractionResult.warnings.joinToString("; ")}"
        }
        val embeddedResourcesEntry = if (extractionResult.embeddedResources.isEmpty()) {
            "embeddedResources=none"
        } else {
            "embeddedResources=" + extractionResult.embeddedResources.joinToString("; ") {
                "name=${it.declaredFileName ?: "unknown"},type=${it.declaredMediaType ?: "unknown"}"
            }
        }
        return listOf(
            "originalEvidenceDigestSha256=$originalEvidenceDigest",
            "detectedMediaType=${extractionResult.detectedMediaType}",
            "parserIdentity=${identity.parserIdentity}; " +
                "configurationProfile=${identity.configurationProfile}; " +
                "normalisationProfile=${identity.normalisationProfile}",
            "extractionTimestamp=${extractionResult.extractedAt}",
            warningsEntry,
            "ocrUsed=${extractionResult.ocrUsed}",
            embeddedResourcesEntry,
        )
    }

    private fun sha256Hex(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString("") { "%02x".format(it.toInt() and 0xFF) }
    }

    companion object {
        /**
         * A fixed, well-known, disclosed action-name convention -- never a
         * bare string literal at any call site -- mirroring
         * [DefaultEvidenceCustodian.ACCEPT_ACTION_NAME]'s own existing
         * precedent exactly (Scope Lock Section 8). Not registered anywhere
         * by this Unit; a future Unit 5 (Runtime Integration, out of this
         * Unit's own scope) registers it.
         */
        const val EXTRACT_ACTION_NAME: String = "evidence.extract"

        /** The document type every derivative this coordinator produces is registered under. */
        const val EXTRACTED_TEXT_DOCUMENT_TYPE: String = "extracted-text"
    }
}

/**
 * Produced by step 4 of [EvidenceExtractionCoordinator.extract]'s own
 * sequence -- a distinct concern from digest calculation itself, which
 * always succeeds once the bytes are in hand (Boundary Clarification
 * Section 4, Section 7).
 */
sealed class IntegrityVerificationOutcome {

    /** The computed digest matches the original Document's own recorded `integrityHash`. */
    object Verified : IntegrityVerificationOutcome()

    /** A recorded `integrityHash` exists and does not match the computed digest. Fails closed. */
    data class Mismatch(val computedDigest: String, val recordedDigest: String) : IntegrityVerificationOutcome()

    /**
     * No `integrityHash` is recorded to compare against, or the comparison
     * otherwise cannot be performed. Fails closed identically to [Mismatch]
     * -- an unprovable digest is not proof of integrity.
     */
    data class Unverifiable(val reason: String) : IntegrityVerificationOutcome() {
        init {
            require(reason.isNotBlank()) { "IntegrityVerificationOutcome.Unverifiable.reason must not be blank" }
        }
    }
}

/**
 * The terminal outcome of one [EvidenceExtractionCoordinator.extract]
 * call. Every variant that corresponds to an existing dependency's own
 * result wraps that result unchanged, never re-deriving or reinterpreting
 * it (Boundary Clarification Section 8).
 */
sealed class EvidenceExtractionOutcome {

    /**
     * The full sequence ran to completion -- [registrationOutcome] may
     * still be any [EvidenceRegistrationOutcome] variant, including a
     * non-successful one; a review state is recorded only when
     * [registrationOutcome] is [EvidenceRegistrationOutcome.Registered]
     * (Boundary Clarification Section 9).
     */
    data class Completed(
        val registrationOutcome: EvidenceRegistrationOutcome,
        val extractionResult: ExtractionResult,
    ) : EvidenceExtractionOutcome()

    /** No Document exists under the supplied [DocumentId]. No retrieval, no extraction, no registration. */
    data class SourceDocumentNotFound(val documentId: DocumentId) : EvidenceExtractionOutcome()

    /**
     * The found Document's own `locationReference` does not match the
     * supplied [EvidenceArtifactId]. No retrieval beyond the Document
     * lookup already performed, no extraction, no registration, no
     * `extractedFrom`/`derivedFrom` fact created (Boundary Clarification
     * Section 5).
     */
    data class SourceIdentityMismatch(val documentId: DocumentId, val evidenceArtifactId: EvidenceArtifactId) : EvidenceExtractionOutcome()

    /** Wraps [EvidenceRetrievalResult.Rejected] unchanged. */
    data class SourceRetrievalRejected(val rejection: EvidenceRetrievalResult.Rejected) : EvidenceExtractionOutcome()

    /** Wraps [EvidenceRetrievalResult.NotFound] unchanged. */
    data class SourceNotFound(val notFound: EvidenceRetrievalResult.NotFound) : EvidenceExtractionOutcome()

    /** Wraps [IntegrityVerificationOutcome.Mismatch] unchanged. Extraction never runs; no derivative is created. */
    data class IntegrityMismatch(val mismatch: IntegrityVerificationOutcome.Mismatch) : EvidenceExtractionOutcome()

    /** Wraps [IntegrityVerificationOutcome.Unverifiable] unchanged. Extraction never runs; no derivative is created. */
    data class IntegrityUnverifiable(val unverifiable: IntegrityVerificationOutcome.Unverifiable) : EvidenceExtractionOutcome()

    /** Wraps [ExtractionOutcome.RequiresOcr] unchanged. The pipeline stops; no candidate is constructed. */
    data class RequiresOcr(val outcome: ExtractionOutcome.RequiresOcr) : EvidenceExtractionOutcome()

    /** Wraps [ExtractionOutcome.Unsupported] unchanged. The pipeline stops; no candidate is constructed. */
    data class Unsupported(val outcome: ExtractionOutcome.Unsupported) : EvidenceExtractionOutcome()

    /** Wraps [ExtractionOutcome.Malformed] unchanged. The pipeline stops; no candidate is constructed. */
    data class Malformed(val outcome: ExtractionOutcome.Malformed) : EvidenceExtractionOutcome()
}
