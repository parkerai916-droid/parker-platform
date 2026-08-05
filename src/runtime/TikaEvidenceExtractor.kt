package parker.core.runtime

import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.time.Instant
import org.apache.tika.Tika
import org.apache.tika.exception.TikaException
import org.apache.tika.extractor.EmbeddedDocumentExtractor
import org.apache.tika.metadata.Metadata
import org.apache.tika.parser.ParseContext
import org.apache.tika.parser.pdf.PDFParser
import org.apache.tika.sax.BodyContentHandler
import org.xml.sax.ContentHandler
import parker.core.interfaces.EmbeddedResourceObservation
import parker.core.interfaces.EvidenceExtractor
import parker.core.interfaces.ExtractionIdentity
import parker.core.interfaces.ExtractionOutcome
import parker.core.interfaces.ExtractionResult

/**
 * Evidence Processing (Searchable PDF), Implementation Unit 2 ("Apache
 * Tika Adapter"). Governed in full by
 * `docs/architecture/EVIDENCE_PROCESSING_SEARCHABLE_PDF_BOUNDARY_CLARIFICATION.md`
 * ("the Boundary Clarification") Section 3, Determination 2; by
 * `docs/architecture/EVIDENCE_PROCESSING_SEARCHABLE_PDF_SCOPE_LOCK.md`
 * ("the Scope Lock") Section 4; and by
 * `docs/architecture/EVIDENCE_PROCESSING_SEARCHABLE_PDF_IMPLEMENTATION_PLAN.md`
 * ("the Implementation Plan") Unit 2.
 *
 * **The sole production implementation of [EvidenceExtractor], and the
 * only file in this repository permitted to import `org.apache.tika.*`.**
 * Nothing Tika-typed ever crosses [EvidenceExtractor]'s own boundary --
 * every Tika object constructed here is converted into a Parker-owned
 * shape before this class's own [extract] returns.
 *
 * ## Metadata keys: plain string literals, deliberately, not versioned constant classes
 *
 * `"resourceName"` and `"Content-Type"` (embedded-resource observation) and
 * `"xmpTPg:NPages"` (page count) are Tika's own long-stable metadata key
 * names, used here as plain string literals via [Metadata.get]'s string
 * overload rather than through `TikaCoreProperties`/`PagedText`-style typed
 * constant classes, whose exact class location has moved between Tika
 * major versions. This is a deliberate implementation choice to minimise
 * compile-time risk against the exact `3.3.1` API surface, not a
 * correctness shortcut -- Unit 6's own verification step confirms these
 * keys actually populate against the real dependency.
 *
 * ## No constructor parameter
 *
 * Constructs its own internal Tika detector/parser/metadata objects per
 * [extract] call -- matching [EvidenceExtractor]'s own "no dependency"
 * contract exactly (an internal Tika object is not a Parker-owned
 * dependency, Implementation Plan Unit 2).
 *
 * ## Media detection, before parsing
 *
 * Uses Tika's own facade detector ([Tika.detect]) first. Any non-PDF
 * result becomes [ExtractionOutcome.Unsupported], carrying the detected
 * type and a reason. Only a detected `application/pdf` proceeds to
 * parsing.
 *
 * ## Parsing: [PDFParser] explicitly, never [org.apache.tika.parser.AutoDetectParser]
 *
 * Because media detection above already confirms the candidate is a PDF,
 * this class invokes [PDFParser] directly -- never Tika's own
 * auto-detecting composite parser. This makes the recorded "actual parser
 * identity" ([ExtractionIdentity.parserIdentity]) a certain, structural
 * fact (always [PDFParser]'s own fully-qualified class name), never a
 * value read back out of parser-reported metadata whose key name can vary
 * across Tika versions.
 *
 * ## Configuration: one fixed profile, tagged, never user-configurable
 *
 * [PDFParser] is invoked through a [ParseContext] configured once,
 * identically for every call: embedded-document handling records
 * presence/declared name/declared media type only, via
 * [RecordingEmbeddedDocumentExtractor] below, and never parses embedded
 * content; no OCR strategy is set (Tika's own default: OCR only activates
 * if a Tesseract binary is present *and* an OCR strategy is explicitly
 * configured -- neither is true here, and `tika-parser-ocr-module` is not
 * even on this repository's classpath, Boundary Clarification
 * Determination 2). This fixed configuration is tagged
 * [CONFIGURATION_PROFILE] -- one constant string, recorded verbatim as
 * every [ExtractionResult.extractionIdentity]'s own configuration-profile
 * field, never varied per call (Scope Lock Section 4).
 *
 * Extraction performs no network communication of any kind: [PDFParser]
 * operates entirely in-process against the bytes already supplied
 * (Implementation Plan Section 2, Rule 6).
 *
 * ## Searchable-versus-scanned detection
 *
 * After a successful parse, if the extracted text, trimmed, falls below
 * [SEARCHABLE_TEXT_THRESHOLD] characters while the document reports at
 * least one page, the outcome becomes [ExtractionOutcome.RequiresOcr] --
 * never a false, emptily "successful" [ExtractionOutcome.Extracted].
 *
 * ## Failure classification
 *
 * A [TikaException] or [IOException] genuinely indicating corrupt or
 * truncated bytes becomes [ExtractionOutcome.Malformed], carrying the
 * exception's own message -- never silently retried, never reinterpreted
 * as [ExtractionOutcome.Unsupported].
 */
class TikaEvidenceExtractor : EvidenceExtractor {

    override suspend fun extract(content: ByteArray): ExtractionOutcome {
        val detectedMediaType = detectMediaType(content)

        if (detectedMediaType != PDF_MEDIA_TYPE) {
            return ExtractionOutcome.Unsupported(
                detectedMediaType = detectedMediaType,
                reason = "Detected media type '$detectedMediaType' is not a searchable PDF -- only $PDF_MEDIA_TYPE is supported by this capability",
            )
        }

        val metadata = Metadata()
        val embeddedResourceObserver = RecordingEmbeddedDocumentExtractor()
        val parseContext = ParseContext()
        parseContext.set(EmbeddedDocumentExtractor::class.java, embeddedResourceObserver)

        val handler: ContentHandler = BodyContentHandler(-1)

        try {
            ByteArrayInputStream(content).use { input ->
                PDFParser().parse(input, handler, metadata, parseContext)
            }
        } catch (e: TikaException) {
            return ExtractionOutcome.Malformed(reason = e.message ?: "Apache Tika reported a malformed PDF with no further detail")
        } catch (e: IOException) {
            return ExtractionOutcome.Malformed(reason = e.message ?: "Apache Tika reported an I/O failure reading the candidate PDF")
        } catch (e: org.xml.sax.SAXException) {
            return ExtractionOutcome.Malformed(reason = e.message ?: "Apache Tika reported a content-handling failure with no further detail")
        }

        val extractedText = handler.toString()
        val pageCount = metadata.get(PAGE_COUNT_METADATA_KEY)?.toIntOrNull()

        if (extractedText.trim().length < SEARCHABLE_TEXT_THRESHOLD) {
            return ExtractionOutcome.RequiresOcr(detectedMediaType = detectedMediaType, pageCount = pageCount)
        }

        val documentMetadata = buildMap {
            pageCount?.let { put("pageCount", it.toString()) }
            metadata.get(PRODUCER_METADATA_KEY)?.let { put("producer", it) }
            metadata.get(TITLE_METADATA_KEY)?.let { put("title", it) }
        }

        return ExtractionOutcome.Extracted(
            ExtractionResult(
                extractedText = extractedText,
                detectedMediaType = detectedMediaType,
                extractorName = EXTRACTOR_NAME,
                extractorVersion = EXTRACTOR_VERSION,
                extractionIdentity = ExtractionIdentity(
                    parserIdentity = PDFParser::class.java.name,
                    configurationProfile = CONFIGURATION_PROFILE,
                    normalisationProfile = NORMALISATION_PROFILE,
                ),
                extractedAt = Instant.now(),
                warnings = emptyList(),
                ocrUsed = false,
                embeddedResources = embeddedResourceObserver.observations,
                documentMetadata = documentMetadata,
            ),
        )
    }

    private fun detectMediaType(content: ByteArray): String =
        ByteArrayInputStream(content).use { input -> Tika().detect(input) }

    companion object {
        const val EXTRACTOR_NAME: String = "Apache Tika"
        const val EXTRACTOR_VERSION: String = "3.3.1"
        const val PDF_MEDIA_TYPE: String = "application/pdf"

        /**
         * A compiled-in constant, not user-configurable (Scope Lock Section
         * 4). Twenty characters -- small enough that no genuine
         * searchable-text PDF is misclassified, large enough that Tika's
         * own incidental whitespace/artefact output from a scanned page is
         * not mistaken for real text (Implementation Plan Unit 2). Unit 7's
         * own real-file proof is this constant's first real validation; any
         * misclassification found there is a defect in this constant to fix
         * before Unit 6 is complete, never silently adjusted after
         * acceptance.
         */
        const val SEARCHABLE_TEXT_THRESHOLD: Int = 20

        /**
         * Tagged verbatim onto every [ExtractionResult.extractionIdentity]'s
         * own configuration-profile field -- one fixed profile, embedded-
         * document handling records presence/name/type only, no OCR
         * strategy configured.
         */
        const val CONFIGURATION_PROFILE: String = "tika-pdf-only-v1;embeddedResources=recordOnly;ocrStrategy=none"

        /** This first unit applies no post-extraction normalisation -- stated explicitly, never omitted. */
        const val NORMALISATION_PROFILE: String = "none"

        private const val PAGE_COUNT_METADATA_KEY = "xmpTPg:NPages"
        private const val PRODUCER_METADATA_KEY = "producer"
        private const val TITLE_METADATA_KEY = "title"
    }
}

/** File-scoped: Tika's own long-stable embedded-resource metadata key names, shared by [RecordingEmbeddedDocumentExtractor] below. */
private const val RESOURCE_NAME_METADATA_KEY = "resourceName"
private const val CONTENT_TYPE_METADATA_KEY = "Content-Type"

/**
 * Records the presence, declared file name, and declared media type of
 * every embedded resource [PDFParser] encounters -- never parses,
 * extracts, or otherwise reads embedded content (Boundary Clarification
 * Section 3). A no-op [EmbeddedDocumentExtractor]: [shouldParseEmbedded]
 * always returns `false`, so [parseEmbedded] is never invoked by Tika to
 * do any actual parsing; the observation is recorded from the embedded
 * resource's own declared [Metadata] alone.
 */
private class RecordingEmbeddedDocumentExtractor : EmbeddedDocumentExtractor {

    private val mutableObservations = mutableListOf<EmbeddedResourceObservation>()
    val observations: List<EmbeddedResourceObservation> get() = mutableObservations

    override fun shouldParseEmbedded(metadata: Metadata): Boolean {
        mutableObservations.add(
            EmbeddedResourceObservation(
                declaredFileName = metadata.get(RESOURCE_NAME_METADATA_KEY),
                declaredMediaType = metadata.get(CONTENT_TYPE_METADATA_KEY),
            ),
        )
        return false
    }

    override fun parseEmbedded(stream: InputStream, handler: ContentHandler, metadata: Metadata, outputHtml: Boolean) {
        // Never invoked -- shouldParseEmbedded always returns false.
    }
}
