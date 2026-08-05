package parker.core.interfaces

import java.time.Instant
import kotlin.reflect.KClass
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.primaryConstructor
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * OCR Mechanism, Implementation Unit 6 ("OCR Output Model"). Governed in
 * full by `docs/architecture/OCR_MECHANISM_CONTRACT_DESIGN.md` ("the
 * Contract Design") Section 5, Section 7; by
 * `docs/architecture/OCR_MECHANISM_SCOPE_LOCK.md` ("the Scope Lock")
 * Section 6, Section 9, Section 10, Section 11; and by
 * `docs/implementation/OCR_MECHANISM_IMPLEMENTATION_PLAN.md`
 * ("the Implementation Plan") Unit 6.
 *
 * **What Unit 6 refined, and why.** Unit 1 already carried recognised
 * text, a single whole-result fidelity, a structured identity, an optional
 * transient confidence, a timestamp, and warnings. Unit 6's own
 * Responsibilities authorise exactly two elaborations of Unit 1's own
 * categories, never a new category: recognised text "optionally
 * page-aligned" (Scope Lock Section 6: "an elaboration of 'recognised
 * text,' not a new category"), and a fidelity disclosure "distinguishing
 * verbatim/normalised/inferred-reconstruction portions where more than one
 * applies within a single recognition" (Evidence Intelligence Contract
 * Design Section 5: "which portions are which, where more than one
 * applies within the same artefact"). [OcrRecognitionSegment] and
 * [OcrRecognitionResult.segments] are the smallest additive shape carrying
 * both at once, without duplicating [OcrRecognitionResult.recognisedText]
 * or replacing [OcrRecognitionResult.fidelity].
 *
 * **What Unit 6 deliberately did not add.** A dedicated "partial or
 * degraded output" indicator is one of Scope Lock Section 10's own seven
 * non-collapsible failure distinctions -- Implementation Plan Unit 7's
 * own, later, exclusive territory -- so no such field exists here; the
 * existing [OcrRecognitionResult.warnings] and
 * [OcrRecognitionResult.confidence] remain the sole lawful vehicles for
 * that honest disclosure at this tier. An output hash is Scope Lock
 * Section 9's own "where applicable" provenance-supporting fact, not named
 * by the Contract Design as this Unit's own field, so none exists here
 * either -- both omissions are verified structurally below, not merely
 * asserted in prose.
 */
class OcrOutputModelTest {

    private fun sampleIdentity() = OcrRecognitionIdentity(mechanismIdentity = "mechanism-a", configurationProfile = "profile-a")

    private fun sampleResult(
        recognisedText: String = "recognised text",
        fidelity: TranscriptionFidelity = TranscriptionFidelity.VERBATIM,
        confidence: Double? = null,
        warnings: List<String> = emptyList(),
        segments: List<OcrRecognitionSegment> = emptyList(),
    ) = OcrRecognitionResult(
        recognisedText = recognisedText,
        fidelity = fidelity,
        identity = sampleIdentity(),
        confidence = confidence,
        recognisedAt = Instant.EPOCH,
        warnings = warnings,
        segments = segments,
    )

    // -- Immutable output structure ------------------------------------------

    @Test
    fun `OcrRecognitionResult and OcrRecognitionSegment are both data classes, declaring only val properties`() {
        assertTrue(OcrRecognitionResult::class.isData, "OcrRecognitionResult must remain a data class")
        assertTrue(OcrRecognitionSegment::class.isData, "OcrRecognitionSegment must remain a data class")

        listOf(OcrRecognitionResult::class, OcrRecognitionSegment::class).forEach { type ->
            val constructorParameterNames = type.primaryConstructor?.parameters?.map { it.name }.orEmpty().toSet()
            val mutableProperties = type.declaredMemberProperties.filter { it.name in constructorParameterNames }
                .filterIsInstance<kotlin.reflect.KMutableProperty<*>>()
            assertTrue(mutableProperties.isEmpty(), "${type.simpleName} must declare every constructor property as val, never var -- found mutable: ${mutableProperties.map { it.name }}")
        }
    }

    // -- Additive compatibility: Unit 1's own public contract is unbroken ----

    @Test
    fun `OcrRecognitionResult can still be constructed exactly as Units 1-5 already do, with segments defaulting to empty`() {
        val result = OcrRecognitionResult(
            recognisedText = "sample text",
            fidelity = TranscriptionFidelity.VERBATIM,
            identity = sampleIdentity(),
            recognisedAt = Instant.EPOCH,
        )

        assertEquals(emptyList(), result.segments, "segments must default to an empty list when a caller supplies none -- the pre-Unit-6 construction shape must remain fully meaningful on its own")
    }

    @Test
    fun `OcrRecognitionResult still declares exactly the same non-segment properties Unit 1 already froze`() {
        val propertyNames = OcrRecognitionResult::class.declaredMemberProperties.map { it.name }.toSet()

        setOf("recognisedText", "fidelity", "identity", "confidence", "recognisedAt", "warnings").forEach { expected ->
            assertTrue(propertyNames.contains(expected), "OcrRecognitionResult must not remove or rename its own Unit 1 property '$expected' -- additive refinement only")
        }
    }

    // -- Recognised text preservation -----------------------------------------

    @Test
    fun `recognisedText is preserved unchanged, independent of whether segments are supplied`() {
        val withoutSegments = sampleResult(recognisedText = "document level text")
        val withSegments = sampleResult(
            recognisedText = "document level text",
            segments = listOf(OcrRecognitionSegment(text = "document level text", fidelity = TranscriptionFidelity.VERBATIM)),
        )

        assertEquals("document level text", withoutSegments.recognisedText)
        assertEquals("document level text", withSegments.recognisedText, "recognisedText must remain the complete, document-level text even when segments are also present")
    }

    // -- All three transcription-fidelity values, at both granularities ------

    @Test
    fun `all three TranscriptionFidelity values are accepted as a result's own whole-recognition fidelity`() {
        TranscriptionFidelity.values().forEach { fidelity ->
            val result = sampleResult(fidelity = fidelity)
            assertEquals(fidelity, result.fidelity)
        }
    }

    @Test
    fun `all three TranscriptionFidelity values are accepted as a segment's own portion-level fidelity, independent of the result's own value`() {
        TranscriptionFidelity.values().forEach { fidelity ->
            val segment = OcrRecognitionSegment(text = "portion", fidelity = fidelity)
            assertEquals(fidelity, segment.fidelity)
        }
    }

    @Test
    fun `a single recognition may carry segments of differing fidelity, discoverable per segment rather than only in prose`() {
        val segments = listOf(
            OcrRecognitionSegment(text = "clean passage", fidelity = TranscriptionFidelity.VERBATIM, pageNumber = 1),
            OcrRecognitionSegment(text = "standardised passage", fidelity = TranscriptionFidelity.NORMALISED, pageNumber = 1),
            OcrRecognitionSegment(text = "reconstructed passage", fidelity = TranscriptionFidelity.INFERRED_RECONSTRUCTION, pageNumber = 2),
        )
        val result = sampleResult(fidelity = TranscriptionFidelity.VERBATIM, segments = segments)

        assertEquals(
            listOf(TranscriptionFidelity.VERBATIM, TranscriptionFidelity.NORMALISED, TranscriptionFidelity.INFERRED_RECONSTRUCTION),
            result.segments.map { it.fidelity },
            "each segment's own fidelity must remain independently discoverable -- a mixed-fidelity recognition must not collapse to one value",
        )
    }

    // -- Mechanism identity preservation --------------------------------------

    @Test
    fun `mechanism identity is preserved unchanged through the result`() {
        val identity = OcrRecognitionIdentity(mechanismIdentity = "mechanism-x", configurationProfile = "profile-x", mechanismVersion = "1.2.3")
        val result = sampleResult().copy(identity = identity)

        assertEquals(identity, result.identity)
        assertEquals("mechanism-x", result.identity.mechanismIdentity)
        assertEquals("1.2.3", result.identity.mechanismVersion)
    }

    // -- Optional confidence bounds --------------------------------------------

    @Test
    fun `confidence accepts both closed-interval boundary values`() {
        assertEquals(0.0, sampleResult(confidence = 0.0).confidence)
        assertEquals(1.0, sampleResult(confidence = 1.0).confidence)
    }

    @Test
    fun `confidence remains null when genuinely unavailable, never fabricated`() {
        assertEquals(null, sampleResult(confidence = null).confidence)
    }

    @Test
    fun `confidence below zero is rejected`() {
        assertFailsWith<IllegalArgumentException> { sampleResult(confidence = -0.0001) }
    }

    @Test
    fun `confidence above one is rejected`() {
        assertFailsWith<IllegalArgumentException> { sampleResult(confidence = 1.0001) }
    }

    // -- Processing timestamp preservation -------------------------------------

    @Test
    fun `recognisedAt is preserved unchanged`() {
        val timestamp = Instant.parse("2026-01-15T10:30:00Z")
        val result = sampleResult().copy(recognisedAt = timestamp)

        assertEquals(timestamp, result.recognisedAt)
    }

    // -- Technical warnings: preservation and ordering -------------------------

    @Test
    fun `warnings are preserved in the exact order supplied, never sorted or deduplicated`() {
        val warnings = listOf("low contrast detected", "low contrast detected", "possible skew")
        val result = sampleResult(warnings = warnings)

        assertContentEquals(warnings, result.warnings, "warnings must preserve caller order and duplicates exactly, never silently reordered or collapsed")
    }

    @Test
    fun `an empty warnings list means genuinely no warnings, not an omission`() {
        assertEquals(emptyList(), sampleResult().warnings)
    }

    // -- Page/document alignment and ordering ----------------------------------

    @Test
    fun `segments with increasing page numbers are accepted and preserve their own order`() {
        val segments = listOf(
            OcrRecognitionSegment(text = "page one", fidelity = TranscriptionFidelity.VERBATIM, pageNumber = 1),
            OcrRecognitionSegment(text = "page two", fidelity = TranscriptionFidelity.VERBATIM, pageNumber = 2),
            OcrRecognitionSegment(text = "page three", fidelity = TranscriptionFidelity.VERBATIM, pageNumber = 3),
        )
        val result = sampleResult(segments = segments)

        assertEquals(listOf(1, 2, 3), result.segments.map { it.pageNumber })
        assertEquals(listOf("page one", "page two", "page three"), result.segments.map { it.text })
    }

    @Test
    fun `multiple segments may share the same page number, for distinct same-page portions`() {
        val segments = listOf(
            OcrRecognitionSegment(text = "top of page one", fidelity = TranscriptionFidelity.VERBATIM, pageNumber = 1),
            OcrRecognitionSegment(text = "bottom of page one, damaged", fidelity = TranscriptionFidelity.INFERRED_RECONSTRUCTION, pageNumber = 1),
        )
        val result = sampleResult(segments = segments)

        assertEquals(listOf(1, 1), result.segments.map { it.pageNumber })
    }

    @Test
    fun `a segment with no page number is accepted, for providers without page-level detail`() {
        val segment = OcrRecognitionSegment(text = "undated portion", fidelity = TranscriptionFidelity.VERBATIM, pageNumber = null)
        val result = sampleResult(segments = listOf(segment))

        assertEquals(null, result.segments.single().pageNumber)
    }

    @Test
    fun `segments naming a later page followed by an earlier page are rejected -- page order must be preserved`() {
        val outOfOrder = listOf(
            OcrRecognitionSegment(text = "page two", fidelity = TranscriptionFidelity.VERBATIM, pageNumber = 2),
            OcrRecognitionSegment(text = "page one", fidelity = TranscriptionFidelity.VERBATIM, pageNumber = 1),
        )

        assertFailsWith<IllegalArgumentException> { sampleResult(segments = outOfOrder) }
    }

    @Test
    fun `a segment page number of zero is rejected -- page numbering is one-based`() {
        assertFailsWith<IllegalArgumentException> { OcrRecognitionSegment(text = "portion", fidelity = TranscriptionFidelity.VERBATIM, pageNumber = 0) }
    }

    @Test
    fun `a negative segment page number is rejected`() {
        assertFailsWith<IllegalArgumentException> { OcrRecognitionSegment(text = "portion", fidelity = TranscriptionFidelity.VERBATIM, pageNumber = -1) }
    }

    // -- Invalid construction rejection -----------------------------------------

    @Test
    fun `a blank segment text is rejected`() {
        assertFailsWith<IllegalArgumentException> { OcrRecognitionSegment(text = "   ", fidelity = TranscriptionFidelity.VERBATIM) }
    }

    @Test
    fun `OcrRecognitionResult still rejects blank recognisedText, unchanged from Unit 1`() {
        assertFailsWith<IllegalArgumentException> { sampleResult(recognisedText = "") }
    }

    // -- Provider-neutral public signatures --------------------------------------

    @Test
    fun `no field or type name on OcrRecognitionResult or OcrRecognitionSegment names a concrete OCR provider`() {
        val forbiddenProviderFragments = listOf("tesseract", "ocrmypdf", "paddleocr", "easyocr", "docling")
        val typesToCheck = listOf(OcrRecognitionResult::class, OcrRecognitionSegment::class)

        typesToCheck.forEach { type ->
            val namesToCheck = listOf(type.simpleName.orEmpty().lowercase()) +
                type.declaredMemberProperties.map { it.name.lowercase() }

            namesToCheck.forEach { name ->
                forbiddenProviderFragments.forEach { forbidden ->
                    assertFalse(
                        name.contains(forbidden),
                        "No name on ${type.simpleName} may reference a concrete provider -- found '$name' containing '$forbidden' (Scope Lock Section 14).",
                    )
                }
            }
        }
    }

    // -- No truth, reliability, or evidential-authority fields -------------------

    @Test
    fun `no property on OcrRecognitionResult or OcrRecognitionSegment names a truth, reliability, or acceptance judgement`() {
        val forbiddenFragments = listOf("truth", "reliab", "evidential", "authoritative", "trustworth", "accept", "reject", "valid")
        val typesToCheck = listOf(OcrRecognitionResult::class, OcrRecognitionSegment::class)

        typesToCheck.forEach { type ->
            type.declaredMemberProperties.map { it.name.lowercase() }.forEach { name ->
                forbiddenFragments.forEach { forbidden ->
                    assertFalse(
                        name.contains(forbidden),
                        "${type.simpleName} must carry no truth, reliability, evidential-authority, or acceptance-judgement " +
                            "property -- found '$name' containing '$forbidden' (Contract Design Section 6, Section 8; Scope " +
                            "Lock Section 11: the OCR mechanism 'does not decide whether its own output is sufficiently " +
                            "trustworthy for downstream use').",
                    )
                }
            }
        }
    }

    // -- No dedicated partial/degraded-output or output-hash field --------------

    @Test
    fun `no dedicated partial-or-degraded-output indicator field exists -- that concrete distinction remains Unit 7's own, later territory`() {
        val propertyNames = OcrRecognitionResult::class.declaredMemberProperties.map { it.name.lowercase() }
        val forbiddenFragments = listOf("partial", "degrad", "complete", "iscomplete")

        propertyNames.forEach { name ->
            forbiddenFragments.forEach { forbidden ->
                assertFalse(
                    name.contains(forbidden),
                    "OcrRecognitionResult must carry no dedicated partial/degraded/completeness field -- 'partial or " +
                        "technically degraded output' is one of Scope Lock Section 10's own seven non-collapsible failure " +
                        "distinctions, Implementation Plan Unit 7's own exclusive, later territory. Found '$name'.",
                )
            }
        }
    }

    @Test
    fun `no output-hash or digest field exists -- Scope Lock Section 9 names it a provenance-supporting fact, not a Unit 6 field`() {
        val propertyNames = OcrRecognitionResult::class.declaredMemberProperties.map { it.name.lowercase() }
        val forbiddenFragments = listOf("hash", "digest", "checksum")

        propertyNames.forEach { name ->
            forbiddenFragments.forEach { forbidden ->
                assertFalse(
                    name.contains(forbidden),
                    "OcrRecognitionResult must carry no output-hash field -- Scope Lock Section 9 names an output hash " +
                        "as a provenance-supporting fact 'not named as its own field by the Contract Design,' never " +
                        "authorised as this Unit's own addition. Found '$name'.",
                )
            }
        }
    }

    // -- Structural safeguards: cannot represent a governed or unrelated record --

    @Test
    fun `no property reachable from OcrRecognitionResult or OcrRecognitionSegment references a governed record, provenance-writing type, or prohibited dependency`() {
        val excludedQualifiedNames = setOf(
            CandidateEvidenceArtifact::class.qualifiedName,
            EvidenceAnalysisResult::class.qualifiedName,
            Provenance::class.qualifiedName,
            CandidateProvenance::class.qualifiedName,
            EvidenceCustodian::class.qualifiedName,
            MemoryCore::class.qualifiedName,
            MemoryRetrieval::class.qualifiedName,
            KnowledgeSubmission::class.qualifiedName,
            PermissionEngine::class.qualifiedName,
            EvidenceIntelligence::class.qualifiedName,
            OwnerEvidenceDeletionAuthority::class.qualifiedName,
        )

        listOf(OcrRecognitionResult::class, OcrRecognitionSegment::class).forEach { type ->
            val propertyTypeNames = type.declaredMemberProperties.mapNotNull { (it.returnType.classifier as? KClass<*>)?.qualifiedName }

            propertyTypeNames.forEach { qualifiedName ->
                assertFalse(
                    excludedQualifiedNames.contains(qualifiedName),
                    "${type.simpleName} must not reference '$qualifiedName' -- the output model is never itself a " +
                        "governed record, a provenance-writing type, or a type carrying any prohibited dependency " +
                        "(Contract Design Section 5; Scope Lock Section 6, Section 13).",
                )
            }
        }
    }

    @Test
    fun `OcrRecognitionResult is not, and cannot be treated as, a CandidateEvidenceArtifact or an EvidenceAnalysisResult subtype`() {
        assertFalse(OcrRecognitionResult::class.isSubclassOf(CandidateEvidenceArtifact::class))
        assertFalse(OcrRecognitionResult::class.isSubclassOf(EvidenceAnalysisResult::class))
    }

    @Test
    fun `OcrRecognitionResult and OcrRecognitionSegment declare no function beyond ordinary structural operations -- no orchestration or acceptance decision of any kind`() {
        val ordinaryStructuralOperations = setOf("equals", "hashCode", "toString", "copy") +
            (1..10).map { "component$it" }.toSet()

        listOf(OcrRecognitionResult::class, OcrRecognitionSegment::class).forEach { type ->
            val declaredFunctionNames = type.declaredFunctions.map { it.name }.toSet()
            assertTrue(
                ordinaryStructuralOperations.containsAll(declaredFunctionNames),
                "${type.simpleName} must declare no function beyond data-class structural operations -- any further " +
                    "function could represent an orchestration, acceptance, or validation decision this Unit forbids. " +
                    "Found: $declaredFunctionNames",
            )
        }
    }
}
