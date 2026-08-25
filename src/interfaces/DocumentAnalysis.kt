package parker.core.interfaces

import java.time.Instant

/**
 * Minimum Production Document Pipeline — Local Reasoning Implementation.
 * Owner-selected, already-admitted durable derivative generations (Tier A
 * or Tier B) submitted, as a bounded package, to Parker's currently
 * configured *local* [parker.core.runtime.ModelInferenceClient] for a
 * human-reviewable analysis. Never Evidence, Memory, Knowledge, QMD, or
 * RKS -- the analysis is provider-generated material for human review
 * only, never automatically promoted anywhere.
 *
 * Deliberately reuses [DerivativeContentIdentity]/[DerivativeProducerIdentity]/
 * [DerivativeCompletenessState] directly rather than inventing parallel
 * provenance concepts -- every provenance fact these new types carry is
 * already governed elsewhere; only the derived extracted-text field and
 * the request/result envelope shapes are new.
 */

/** One owner-selected, already-admitted derivative generation to include in an analysis. */
data class EvidenceGenerationSelection(
    val evidenceArtifactId: EvidenceArtifactId,
    val derivativeGenerationId: DerivativeGenerationId,
)

/**
 * One successfully retrieved item of the bounded evidence package actually
 * submitted for analysis. [derivativeKind]/[contentIdentity]/[producerIdentity]/
 * [completenessState]/[warnings] are the same already-governed facts
 * [DerivativeGenerationRecord] already carries -- read directly from the
 * resolved record, never re-derived or duplicated as a new concept.
 * [extractedText] is the one genuinely new, derived fact: a plain-text
 * projection of the specific Tier A/Tier B payload shape actually
 * retrieved, mirroring the existing owner-facing "extracted content"
 * projection precedent
 * ([parker.composition.OwnerUiEvidenceRuntimeAdapter.toOwnerContent]/`toOwnerOcrContent`)
 * rather than inventing a new text-extraction concept.
 */
data class AnalysisEvidenceItem(
    val evidenceArtifactId: EvidenceArtifactId,
    val derivativeGenerationId: DerivativeGenerationId,
    val derivativeKind: String,
    val contentIdentity: DerivativeContentIdentity,
    val producerIdentity: DerivativeProducerIdentity,
    val extractedText: String,
    val completenessState: DerivativeCompletenessState,
    val warnings: List<String>,
)

/**
 * The owner's request for one document-analysis invocation. [selections]
 * is bounded (`DocumentAnalysisCoordinator.MAX_SELECTIONS`) -- enforced
 * before any derivative retrieval begins. Provider selection is
 * deliberately absent: this request always uses whichever local
 * [parker.core.runtime.ModelInferenceClient] Parker's own runtime
 * composition is configured with; the owner never chooses, and no
 * external provider is reachable through this type.
 */
data class OwnerDocumentAnalysisRequest(
    val selections: List<EvidenceGenerationSelection>,
    val instruction: String,
) {
    init {
        require(selections.isNotEmpty()) { "OwnerDocumentAnalysisRequest.selections must not be empty" }
        require(instruction.isNotBlank()) { "OwnerDocumentAnalysisRequest.instruction must not be blank" }
    }
}

/**
 * The truthful result of one completed document-analysis invocation.
 * [analysisText] is the local model's own raw response, presented for
 * human review -- never Evidence, Memory, Knowledge, a canonical fact, or
 * a modification of any source. [mechanismIdentity]/[mechanismVersion]
 * are `null` when genuinely not truthfully knowable (Parker's current
 * local model-inference seam discloses no model identity/version of its
 * own -- see this unit's own human-review report) -- never fabricated,
 * never the literal string `"unknown"`.
 */
data class OwnerDocumentAnalysisResult(
    val analysisText: String,
    val evidenceItems: List<AnalysisEvidenceItem>,
    val mechanismIdentity: String?,
    val mechanismVersion: String?,
    val analysedAt: Instant,
    val instruction: String,
    val warnings: List<String>,
) {
    init {
        require(analysisText.isNotBlank()) { "OwnerDocumentAnalysisResult.analysisText must not be blank" }
        require((mechanismIdentity == null) == (mechanismVersion == null)) {
            "OwnerDocumentAnalysisResult mechanism identity and version must either both be present or both be absent"
        }
    }
}

/**
 * The truthful result of one structurally owner-only, Permission-Engine-authorised
 * document-analysis invocation attempt -- mirrors
 * [TierBOcrOwnerInvocationOutcome]'s own established shape: every failure
 * distinct and honest, no durable side effect of any kind (this
 * capability creates none at all, in either branch).
 */
sealed class DocumentAnalysisOutcome {
    data class Completed(val result: OwnerDocumentAnalysisResult) : DocumentAnalysisOutcome()

    /** The Permission Engine did not approve this invocation. No retrieval, no inference. */
    data class NotAuthorised(val reason: String) : DocumentAnalysisOutcome()

    /** [OwnerDocumentAnalysisRequest.selections] exceeded the frozen maximum -- checked before any retrieval. */
    data class TooManySelections(val requested: Int, val max: Int) : DocumentAnalysisOutcome()

    /** [OwnerDocumentAnalysisRequest.instruction] exceeded the frozen maximum -- checked before any retrieval, never truncated. */
    data class InstructionTooLarge(val actualCharacters: Int, val max: Int) : DocumentAnalysisOutcome()

    data class UnknownGeneration(val derivativeGenerationId: DerivativeGenerationId) : DocumentAnalysisOutcome()
    data class SourceMismatch(val evidenceArtifactId: EvidenceArtifactId, val derivativeGenerationId: DerivativeGenerationId) : DocumentAnalysisOutcome()
    data class ContentMissing(val derivativeGenerationId: DerivativeGenerationId) : DocumentAnalysisOutcome()
    data class ContentCorrupt(val derivativeGenerationId: DerivativeGenerationId, val reason: String) : DocumentAnalysisOutcome()
    data class UnsupportedRepresentationVersion(val derivativeGenerationId: DerivativeGenerationId, val version: Int) : DocumentAnalysisOutcome()

    /** The resolved generation's own payload shape has no supported text-extraction mapping (defensive; every currently governed Tier A/Tier B kind is supported). */
    data class UnsupportedDerivativeKind(val derivativeGenerationId: DerivativeGenerationId, val derivativeKind: String) : DocumentAnalysisOutcome()

    /** The assembled evidence package's total extracted-text size exceeded the frozen maximum -- checked before inference. */
    data class ContentTooLarge(val actualCharacters: Int, val max: Int) : DocumentAnalysisOutcome()

    /**
     * The fully assembled model prompt (fixed instructions + owner instruction + document
     * framing/identifiers + evidence content) exceeded the frozen maximum -- checked after prompt
     * construction, before model invocation. Not merely inferred from the evidence-content and
     * instruction bounds individually: framing/identifier/encoding overhead means the complete
     * prompt is never assumed to equal their sum.
     */
    data class PromptTooLarge(val actualCharacters: Int, val max: Int) : DocumentAnalysisOutcome()

    /** The local model's own raw response exceeded the frozen maximum -- rejected, never truncated and presented as complete. */
    data class ResponseTooLarge(val actualCharacters: Int, val max: Int) : DocumentAnalysisOutcome()

    /** [parker.core.runtime.ModelInferenceClient.infer] faulted (timeout, connection failure, malformed/empty response) -- a safe, non-leaking, non-internal-detail message only. */
    data class ModelInvocationFailed(val safeMessage: String) : DocumentAnalysisOutcome()
}
