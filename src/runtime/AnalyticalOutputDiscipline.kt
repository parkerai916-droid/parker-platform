package parker.core.runtime

import parker.core.interfaces.EvidenceAnalysisResult

/**
 * Evidence Intelligence, Implementation Unit 4 ("Analytical Output
 * Discipline"). Governed in full by
 * `docs/implementation/EVIDENCE_INTELLIGENCE_IMPLEMENTATION_PLAN.md`
 * ("the Implementation Plan"), §8 Unit 4 -- this file implements exactly
 * that Unit's own responsibilities, and only those.
 *
 * Contract Design §5 requires content-type and transcription-fidelity
 * distinctions to be honoured wherever analytical output is produced,
 * but explicitly "does not prescribe the mechanism by which that
 * distinction is expressed," and requires that no new artefact class,
 * field, or type be introduced to carry it. Scope Lock §9 confirms the
 * resulting requirement is that these distinctions be "structurally or
 * textually distinguishable," not that a new field carry them. This
 * object realises that discipline entirely through a disclosed, textual
 * labelling convention applied to the existing, unmodified
 * [EvidenceAnalysisResult.TransientOutput.text] field -- never a new
 * field, type, or `EvidenceAnalysisResult` variant.
 *
 * `internal`, not public -- Unit 4 implementation plumbing, not one of
 * the four public runtime types the Contract Design and Scope Lock
 * authorise, mirroring [EvidenceIntelligenceInputResolver]'s and
 * [EvidenceIntelligenceReasoningCoordinator]'s own identical precedent.
 *
 * ## What this Unit deliberately does not implement
 *
 * - **No new public type, field, or `EvidenceAnalysisResult` variant.**
 *   Every function below operates entirely on the existing, unmodified
 *   `TransientOutput` shape (Unit 1); the labels themselves are plain
 *   `String` constants, not an enum or sealed type.
 * - **No analysis-kind algorithm.** This object does not decide what a
 *   claim says, whether two claims contradict, or how a transcription is
 *   produced -- those remain a specific analysis kind's own internal
 *   algorithm, permanently out of scope for this Programme (Scope Lock
 *   §3).
 * - **No durable confidence or evidential-state field.** Nothing here
 *   constructs a `CandidateAssertion`, `CandidateRelationship`, or any
 *   other durable record; analytical confidence remains expressible only
 *   as ordinary prose within a `TransientOutput`'s own `text` (Contract
 *   Design §9).
 * - **No operation assembly.** This file does not accept an
 *   `EvidenceAnalysisRequest`, does not invoke `EvidenceCustodian`,
 *   `MemoryRetrieval`, or `ReasoningProvider`, and does not return an
 *   `EvidenceAnalysisResult` list for a whole invocation -- that remains
 *   Unit 5's own, exclusive responsibility.
 */
internal object AnalyticalOutputDiscipline {

    /** Content-kind labels (Contract Design §5) -- plain `String` constants, not a new type. */
    const val EXTRACTED = "EXTRACTED"
    const val OBSERVED = "OBSERVED"
    const val INFERRED = "INFERRED"
    const val MODEL_GENERATED = "MODEL"

    /** Transcription-fidelity labels (Contract Design §5) -- plain `String` constants, not a new type. */
    const val VERBATIM = "VERBATIM"
    const val NORMALISED = "NORMALISED"
    const val RECONSTRUCTED = "RECONSTRUCTED"

    private val CONTENT_KINDS = setOf(EXTRACTED, OBSERVED, INFERRED, MODEL_GENERATED)
    private val TRANSCRIPTION_KINDS = setOf(VERBATIM, NORMALISED, RECONSTRUCTED)

    private val LABEL_START_PATTERN = Regex("""\[(\w+)]\s*""")

    /**
     * Labels one segment of prose with its content kind (Contract Design
     * §5's four-way distinction: extracted content, observed content,
     * inferred analytical conclusions, model-generated explanatory
     * language), so a reader -- or [discoverSegments] -- is never left
     * to assume a model's own explanatory language is itself extracted
     * or observed content.
     */
    fun labelContent(kind: String, text: String): String {
        require(kind in CONTENT_KINDS) { "Unrecognised content kind: '$kind'" }
        require(text.isNotBlank()) { "labelContent text must not be blank" }
        return "[$kind] $text"
    }

    /**
     * Labels one segment of transcribed prose with its fidelity kind
     * (Contract Design §5's three-way distinction: verbatim, normalised,
     * inferred reconstruction), so a `CandidateEvidenceArtifact` produced
     * by transcription never presents all three uniformly as if each
     * were an equally direct rendering of the original.
     */
    fun labelTranscription(kind: String, text: String): String {
        require(kind in TRANSCRIPTION_KINDS) { "Unrecognised transcription-fidelity kind: '$kind'" }
        require(text.isNotBlank()) { "labelTranscription text must not be blank" }
        return "[$kind] $text"
    }

    /**
     * Discovers every labelled segment within [text], structurally --
     * satisfying Scope Lock §9's "structurally or textually
     * distinguishable" requirement for content composed via
     * [labelContent] or [labelTranscription], rather than leaving the
     * distinction merely asserted within an undifferentiated block of
     * prose.
     *
     * Each segment's content runs up to the *next recognised label
     * marker* (not merely the next literal `[`), so a labelled claim
     * whose own text legitimately contains a bracket -- a verbatim
     * quotation citing `[Exhibit A]`, a reconstructed transcription
     * marked `[illegible: likely 'thanks']` -- is never silently
     * truncated at that bracket.
     *
     * Known, deliberately out-of-scope limitation: a label's own text
     * that itself contains a second, well-formed label marker (e.g.
     * literal content of `[INFERRED] fake`) is indistinguishable from a
     * genuine second, sequential label -- no caller in this repository
     * constructs such content, and no governing document requires
     * defending against it; resolving that would require an escaping
     * scheme no governance document authorises.
     */
    fun discoverSegments(text: String): List<Pair<String, String>> {
        val starts = LABEL_START_PATTERN.findAll(text).toList()
        return starts.mapIndexed { index, match ->
            val kind = match.groupValues[1]
            val contentStart = match.range.last + 1
            val contentEnd = starts.getOrNull(index + 1)?.range?.first ?: text.length
            kind to text.substring(contentStart, contentEnd).trim()
        }
    }

    /**
     * Discloses two claims together -- ordinarily, the two sides of a
     * genuine contradiction -- never one alone (Contract Design §1,
     * "Preserving contradictory evidence"). The function's own signature
     * makes silent suppression of either side structurally impossible:
     * there is no overload accepting only one
     * [EvidenceAnalysisResult.TransientOutput] for a contradiction
     * disclosure. This function does not itself determine whether two
     * claims contradict -- that determination remains a specific
     * analysis kind's own internal algorithm, out of this Unit's scope;
     * it only guarantees that, once two conflicting claims are supplied,
     * both are represented, never fewer.
     */
    fun discloseContradiction(
        firstClaim: EvidenceAnalysisResult.TransientOutput,
        secondClaim: EvidenceAnalysisResult.TransientOutput,
    ): List<EvidenceAnalysisResult.TransientOutput> = listOf(firstClaim, secondClaim)
}
