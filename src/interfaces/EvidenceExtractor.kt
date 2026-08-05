package parker.core.interfaces

import java.time.Instant

/**
 * Evidence Processing (Searchable PDF), Implementation Unit 1 ("Extraction
 * Contracts"). Governed in full by
 * `docs/architecture/EVIDENCE_PROCESSING_SEARCHABLE_PDF_BOUNDARY_CLARIFICATION.md`
 * ("the Boundary Clarification") Sections 3, 4, and 8; by
 * `docs/architecture/EVIDENCE_PROCESSING_SEARCHABLE_PDF_SCOPE_LOCK.md`
 * ("the Scope Lock") Section 3 item 1, Section 6; and by
 * `docs/architecture/EVIDENCE_PROCESSING_SEARCHABLE_PDF_IMPLEMENTATION_PLAN.md`
 * ("the Implementation Plan") Unit 1. Mirrors [ReasoningProvider]'s own
 * "narrow, dependency-free interface plus its own sealed response type,
 * one file" shape exactly (Implementation Plan Unit 1).
 *
 * ## What this Unit implements
 *
 * [EvidenceExtractor] -- one operation, given already-retrieved content
 * bytes, return an [ExtractionOutcome]. Declares no constructor, no
 * property, and no dependency of any kind -- mirroring [ReasoningProvider]'s
 * own "pure callee, calls nothing" shape (Boundary Clarification Section
 * 3). No Apache Tika type, or any other third-party type, appears
 * anywhere in this file's own shape -- every field below is a JDK type, a
 * Parker-owned type, or a primitive (Boundary Clarification Section 3;
 * Scope Lock Verification Question 1).
 *
 * ## Why `extract` is `suspend`
 *
 * For consistency with every other Parker interface a coordinator calls
 * ([EvidenceCustodian.accept]/[EvidenceCustodian.retrieve],
 * [MemoryCore.createProvenance]/[MemoryCore.registerDocument]), even though
 * the first production implementation performs no genuinely asynchronous
 * work itself -- a shape decision, not a behavioural one (Implementation
 * Plan Unit 1).
 *
 * ## No digest field anywhere in this file
 *
 * Digests are computed by the coordinator ([EvidenceExtractionCoordinator],
 * Unit 4A), never by the extractor (Implementation Plan Unit 1: "carries
 * no digest field of any kind"). [ExtractionResult] carries no confidence
 * field either -- extraction is deterministic under [ExtractionIdentity]'s
 * own fixed conditions, not probabilistic, so a numeric confidence value
 * is deferred to `Provenance.confidence`, left `null` by the coordinator,
 * never fabricated here (Boundary Clarification Section 4).
 */
interface EvidenceExtractor {
    suspend fun extract(content: ByteArray): ExtractionOutcome
}

/**
 * One flat, structured value answering a single question -- exactly what
 * produced a derivative's extracted text -- never three independently-
 * worded free-text sentences (Boundary Clarification Section 4, "Extraction
 * identity is one structured record, not three independently-worded
 * sentences"; Scope Lock Section 6). All three fields are fixed facts
 * about one extraction run, never free narrative prose.
 *
 * @param parserIdentity The specific parser implementation that handled
 *   this document (for example, the concrete Tika parser class name).
 * @param configurationProfile A named tag identifying the specific parser
 *   configuration in force for this extraction (embedded-resource
 *   handling, OCR strategy explicitly disabled, and any other
 *   configuration the extractor sets) -- a different configuration could
 *   produce different output from the same extractor version.
 * @param normalisationProfile Whether, and which, post-extraction text
 *   normalisation was applied. Never omitted -- a run that applies none
 *   states that explicitly (for example, `"none"`), so a future unit that
 *   begins normalising text changes a recorded fact rather than silently
 *   altering behaviour no one can see happened.
 */
data class ExtractionIdentity(
    val parserIdentity: String,
    val configurationProfile: String,
    val normalisationProfile: String,
) {
    init {
        require(parserIdentity.isNotBlank()) { "ExtractionIdentity.parserIdentity must not be blank" }
        require(configurationProfile.isNotBlank()) { "ExtractionIdentity.configurationProfile must not be blank" }
        require(normalisationProfile.isNotBlank()) { "ExtractionIdentity.normalisationProfile must not be blank" }
    }
}

/**
 * The presence, declared file name, and declared media type of one
 * embedded file or attachment observed inside the original document --
 * never its content (Boundary Clarification Section 3, "Embedded
 * resources"). [declaredFileName] and [declaredMediaType] are each
 * `null` only when the embedded resource itself discloses no such fact --
 * never a placeholder for "not observed."
 */
data class EmbeddedResourceObservation(
    val declaredFileName: String?,
    val declaredMediaType: String?,
)

/**
 * Every fact Boundary Clarification Section 4's table assigns to the
 * extractor's own responsibility. Carries no digest field (the
 * coordinator's own responsibility, Unit 4A) and no confidence field (left
 * to `Provenance.confidence`, `null`, by the coordinator).
 *
 * @param extractedText The already-encoded PDF text layer, read directly
 *   -- never OCR output, never a translation, never a summary. Must be
 *   non-blank: text falling below the searchable-versus-scanned threshold
 *   is classified [ExtractionOutcome.RequiresOcr] instead of reaching this
 *   type at all (Implementation Plan Unit 2).
 * @param detectedMediaType What the extractor determined the original to
 *   be (for example, `"application/pdf"`).
 * @param extractorName The overall extraction framework (for example,
 *   `"Apache Tika"`).
 * @param extractorVersion The framework's own version (for example,
 *   `"3.3.1"`).
 * @param extractionIdentity The structured, single-entry reproducibility
 *   record (parser identity, configuration profile, normalisation
 *   profile).
 * @param extractedAt When this extraction was performed.
 * @param warnings Any non-fatal condition the extractor observed. An empty
 *   list means genuinely no warnings, recorded explicitly by the
 *   coordinator as `"warnings=none"` rather than silently omitted
 *   (Boundary Clarification Section 4).
 * @param ocrUsed Always `false` for this capability (Determination 2).
 * @param embeddedResources Presence, declared name, and declared media
 *   type of any embedded file or attachment -- never its content, and
 *   never folded into [warnings].
 * @param documentMetadata The original document's own metadata as the
 *   extractor itself reports it (page count, declared producer, declared
 *   title, and similar) -- never a duplicate of any fact [extractionIdentity]
 *   or a digest already carries.
 */
data class ExtractionResult(
    val extractedText: String,
    val detectedMediaType: String,
    val extractorName: String,
    val extractorVersion: String,
    val extractionIdentity: ExtractionIdentity,
    val extractedAt: Instant,
    val warnings: List<String> = emptyList(),
    val ocrUsed: Boolean,
    val embeddedResources: List<EmbeddedResourceObservation> = emptyList(),
    val documentMetadata: Map<String, String> = emptyMap(),
) {
    init {
        require(extractedText.isNotBlank()) { "ExtractionResult.extractedText must not be blank" }
        require(detectedMediaType.isNotBlank()) { "ExtractionResult.detectedMediaType must not be blank" }
        require(extractorName.isNotBlank()) { "ExtractionResult.extractorName must not be blank" }
        require(extractorVersion.isNotBlank()) { "ExtractionResult.extractorVersion must not be blank" }
    }
}

/**
 * The sealed outcome of one [EvidenceExtractor.extract] call -- exactly
 * four variants, no fifth, no default/empty result standing in for a
 * genuine failure to classify a document (Boundary Clarification Section
 * 3).
 */
sealed class ExtractionOutcome {

    /** A successful, deterministic extraction of an already-encoded text layer. */
    data class Extracted(val result: ExtractionResult) : ExtractionOutcome()

    /**
     * A terminal, explicit, non-silent outcome -- never attempted further
     * by this capability (Determination 2). [pageCount] is `null` only
     * when the page count itself could not be determined -- never a
     * placeholder for zero.
     */
    data class RequiresOcr(val detectedMediaType: String, val pageCount: Int? = null) : ExtractionOutcome() {
        init {
            require(detectedMediaType.isNotBlank()) { "ExtractionOutcome.RequiresOcr.detectedMediaType must not be blank" }
        }
    }

    /**
     * The candidate is not a searchable PDF at all -- a different,
     * undetected, or encrypted format. [detectedMediaType] is `null` only
     * when detection itself could not determine one.
     */
    data class Unsupported(val detectedMediaType: String?, val reason: String) : ExtractionOutcome() {
        init {
            require(reason.isNotBlank()) { "ExtractionOutcome.Unsupported.reason must not be blank" }
        }
    }

    /** A genuine parse failure -- corrupt or truncated bytes. */
    data class Malformed(val reason: String) : ExtractionOutcome() {
        init {
            require(reason.isNotBlank()) { "ExtractionOutcome.Malformed.reason must not be blank" }
        }
    }
}
