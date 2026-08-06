package parker.core.interfaces

import java.security.MessageDigest
import java.time.Instant
import kotlin.reflect.full.declaredMemberProperties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import parker.core.runtime.EvidenceRegistrationOutcome

/**
 * OCR Mechanism, Implementation Unit 8 ("Provenance-Supporting
 * Disclosure"). Governed in full by
 * `docs/architecture/OCR_MECHANISM_CONTRACT_DESIGN.md` ("the Contract
 * Design") Section 7; by `docs/architecture/OCR_MECHANISM_SCOPE_LOCK.md`
 * ("the Scope Lock") Section 9; and by
 * `docs/implementation/OCR_MECHANISM_IMPLEMENTATION_PLAN.md`
 * ("the Implementation Plan") Unit 8.
 *
 * **This Unit adds no production file.** Implementation Plan Unit 8's own
 * Outputs field is fixed as "Confirmation (by test, not by new code) that
 * the disclosure is provenance-sufficient," and its own Files-expected-
 * to-change field authorises touching Unit 6's own file "only... unless a
 * field is found missing during this unit's own verification." No field
 * was found missing (see this file's own construction tests below, which
 * successfully build a hypothetical [CandidateProvenance] from Unit 1 and
 * Unit 6's own already-existing fields alone) -- so `OcrMechanism.kt` is
 * untouched. This file is that verification, mirroring exactly how
 * `EvidenceExtractionCoordinator.kt` already builds a real
 * [CandidateProvenance] from `ExtractionResult`'s own analogous plain
 * fields (`extractedText`, `extractorName`, `extractorVersion`,
 * `extractedAt`, `warnings`) -- the same mapping shape, applied here to
 * [OcrRecognitionResult]'s own fields, as a *test-only* proof of
 * sufficiency, never as new production code.
 *
 * **Why this file may reference [CandidateProvenance] when production
 * code may not.** Implementation Plan Unit 8's own Verification
 * requirements explicitly ask for "a test constructing a hypothetical
 * `Provenance` value entirely from Unit 6's own disclosure fields" --
 * this is only possible if the *test* references [CandidateProvenance].
 * Its own "Files explicitly prohibited: any file referencing `Provenance`,
 * `CandidateProvenance`, or `MemoryCore` directly" governs Units 1-8's own
 * *production* files (`OcrMechanism.kt`, `OcrProviderAdapter.kt`,
 * `OcrExecutionSequencer.kt`) -- verified structurally, below -- not this
 * Unit's own required verification test, exactly as Implementation Plan
 * Unit 9's own "hand-written fakes... not the real thing" carve-out
 * already establishes the same test/production distinction for
 * structural-isolation tests generally.
 *
 * **No digest field was added, and none was required.** Scope Lock
 * Section 9 names "output hash, where applicable" as "not named as its
 * own field by the Contract Design, but consistent with... the already-
 * existing, generic `CandidateDocument.integrityHash` mechanism every
 * other... derivative already populates" -- exactly mirroring
 * `EvidenceExtractionCoordinator.kt`'s own established pattern, where the
 * derivative digest is computed entirely *outside* `ExtractionResult`'s
 * own contract (`sha256Hex(extractionResult.extractedText.toByteArray(...))`),
 * from a plain `String` field, never a dedicated hash field the extractor
 * itself carries. [OcrRecognitionResult.recognisedText] is that same
 * plain `String` field; "sufficient information for a future output-hash
 * computation" is demonstrated below to already be satisfied by it alone,
 * with no algorithm, canonicalisation, or hashing choice fixed by this
 * Unit -- the SHA-256 computation below exists only to prove sufficiency
 * of input, mirroring `EvidenceExtractionCoordinator`'s own algorithm
 * choice for citation purposes, never to mandate it for a future caller.
 */
class OcrProvenanceDisclosureTest {

    private val ocrUnitFiles = listOf(
        java.io.File("src/interfaces/OcrMechanism.kt"),
        java.io.File("src/interfaces/OcrProviderAdapter.kt"),
        java.io.File("src/runtime/OcrExecutionSequencer.kt"),
    )

    private fun String.codeOnly(): String =
        replace(Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL), "")
            .lines()
            .joinToString("\n") { line -> line.substringBefore("//") }

    private fun sampleRequest(sourceEvidenceId: String = "evidence-42") = OcrRecognitionRequest(
        sourceEvidenceId = EvidenceArtifactId(sourceEvidenceId),
        content = byteArrayOf(1, 2, 3, 4),
        mediaType = "image/png",
        pageCount = 2,
    )

    private fun sampleResult(
        text: String = "recognised text",
        fidelity: TranscriptionFidelity = TranscriptionFidelity.VERBATIM,
        warnings: List<String> = emptyList(),
        segments: List<OcrRecognitionSegment> = emptyList(),
        confidence: Double? = 0.87,
    ) = OcrRecognitionResult(
        recognisedText = text,
        fidelity = fidelity,
        identity = OcrRecognitionIdentity(mechanismIdentity = "mechanism-a", configurationProfile = "profile-a", mechanismVersion = "1.4.0"),
        confidence = confidence,
        recognisedAt = Instant.parse("2026-01-15T10:30:00Z"),
        warnings = warnings,
        segments = segments,
    )

    /**
     * Exactly `EvidenceExtractionCoordinator.kt`'s own established mapping
     * (`sourceIdentifier`/`extractedFrom` from the caller's own already-held
     * request context; `acquisitionTime` from the produced result's own
     * timestamp; `creator` a free-text join of mechanism identity and
     * version; `confidence` carried through unchanged), applied here to an
     * [OcrRecognitionRequest]/[OcrRecognitionResult] pair instead of a
     * `documentId`/`ExtractionResult` pair. `sourceType` and `contentNature`
     * are the *caller's own* editorial classification choices in the real
     * coordinator too (`"extraction"`, `ContentNature.EXTRACTED` are literal,
     * hand-chosen constants there, derived from no `ExtractionResult` field)
     * -- placeholder values are used here for the same reason, not because
     * OCR's own disclosure is expected to supply them.
     */
    private fun buildHypotheticalCandidateProvenance(
        request: OcrRecognitionRequest,
        result: OcrRecognitionResult,
    ): CandidateProvenance = CandidateProvenance(
        sourceIdentifier = request.sourceEvidenceId.value,
        sourceType = "ocr-recognition",
        acquisitionTime = result.recognisedAt,
        contentNature = ContentNature.EXTRACTED,
        creator = "${result.identity.mechanismIdentity} ${result.identity.mechanismVersion.orEmpty()}".trim(),
        confidence = result.confidence,
    )

    // -- Every required technical fact is disclosed: hypothetical CandidateProvenance construction succeeds --

    @Test
    fun `a hypothetical CandidateProvenance is constructible from a VERBATIM Recognised outcome using only already-existing Unit 1 and Unit 6 fields`() {
        val request = sampleRequest()
        val result = sampleResult(fidelity = TranscriptionFidelity.VERBATIM)

        val provenance = buildHypotheticalCandidateProvenance(request, result)

        assertEquals("evidence-42", provenance.sourceIdentifier)
        assertEquals(result.recognisedAt, provenance.acquisitionTime)
        assertEquals("mechanism-a 1.4.0", provenance.creator)
        assertEquals(0.87, provenance.confidence)
    }

    @Test
    fun `a hypothetical CandidateProvenance is constructible for each of the three fidelity categories, mirroring Unit 5's own representative recognitions`() {
        TranscriptionFidelity.values().forEach { fidelity ->
            val request = sampleRequest()
            val result = sampleResult(fidelity = fidelity)

            val provenance = buildHypotheticalCandidateProvenance(request, result)

            assertEquals("evidence-42", provenance.sourceIdentifier, "sufficiency must hold for $fidelity")
        }
    }

    @Test
    fun `a hypothetical CandidateProvenance is constructible from a PartialOrDegradedOutput's own partialResult, exactly as from a plain Recognised`() {
        val request = sampleRequest()
        val partial = sampleResult(text = "only the top half of the page was legible")
        val outcome = OcrRecognitionOutcome.PartialOrDegradedOutput(partialResult = partial, reason = "bottom half illegible")

        val provenance = buildHypotheticalCandidateProvenance(request, outcome.partialResult)

        assertEquals("evidence-42", provenance.sourceIdentifier)
        assertEquals(partial.recognisedAt, provenance.acquisitionTime)
    }

    // -- Source evidence identity: available via the request, never duplicated into the result --

    @Test
    fun `OcrRecognitionResult carries no sourceEvidenceId field of its own -- the caller's own already-held request supplies it, never duplicated`() {
        val fieldNames = OcrRecognitionResult::class.declaredMemberProperties.map { it.name.lowercase() }

        assertFalse(
            fieldNames.any { it.contains("sourceevidence") || it.contains("evidenceid") },
            "OcrRecognitionResult must not duplicate sourceEvidenceId -- the caller already holds it from the " +
                "OcrRecognitionRequest it itself constructed, exactly as EvidenceExtractionCoordinator already " +
                "holds documentId/evidenceArtifactId from its own method parameters rather than from ExtractionResult.",
        )
    }

    @Test
    fun `the request's own sourceEvidenceId is declared as EvidenceArtifactId -- a plain identifier reference, never a claim that the original is itself an accepted candidate`() {
        val property = OcrRecognitionRequest::class.declaredMemberProperties.single { it.name == "sourceEvidenceId" }

        assertEquals(
            EvidenceArtifactId::class,
            property.returnType.classifier,
            "sourceEvidenceId must remain declared as EvidenceArtifactId -- a plain identifier reference, never CandidateEvidenceArtifact or any other custody/acceptance-shaped type",
        )
    }

    // -- Mechanism identity, configuration, version preserved unchanged --

    @Test
    fun `mechanism identity, configuration profile, and version are preserved unchanged for provenance-supporting disclosure`() {
        val identity = OcrRecognitionIdentity(mechanismIdentity = "mechanism-x", configurationProfile = "profile-x", mechanismVersion = "9.9.9")
        val result = sampleResult().copy(identity = identity)

        assertEquals("mechanism-x", result.identity.mechanismIdentity)
        assertEquals("profile-x", result.identity.configurationProfile)
        assertEquals("9.9.9", result.identity.mechanismVersion)
    }

    @Test
    fun `mechanism version remains null, never fabricated, when genuinely unavailable -- 'where available' is honoured`() {
        val identity = OcrRecognitionIdentity(mechanismIdentity = "mechanism-x", configurationProfile = "profile-x", mechanismVersion = null)
        assertEquals(null, identity.mechanismVersion)
    }

    // -- Processing timestamp preserved --

    @Test
    fun `processing timestamp is preserved unchanged for provenance-supporting disclosure`() {
        val timestamp = Instant.parse("2026-03-01T00:00:00Z")
        val result = sampleResult().copy(recognisedAt = timestamp)

        assertEquals(timestamp, result.recognisedAt)
    }

    // -- Fidelity and confidence remain unchanged --

    @Test
    fun `fidelity and confidence remain unchanged and discoverable without reinterpretation`() {
        val result = sampleResult(fidelity = TranscriptionFidelity.INFERRED_RECONSTRUCTION, confidence = 0.42)

        assertEquals(TranscriptionFidelity.INFERRED_RECONSTRUCTION, result.fidelity)
        assertEquals(0.42, result.confidence)
    }

    // -- Warnings and page ordering remain unchanged --

    @Test
    fun `warnings remain in their original order for provenance-supporting disclosure, never reordered`() {
        val warnings = listOf("low contrast detected", "possible skew", "low contrast detected")
        val result = sampleResult(warnings = warnings)

        assertEquals(warnings, result.warnings, "warnings must reach provenance-supporting disclosure in original order, duplicates included")
    }

    @Test
    fun `page ordering remains unchanged and available for provenance-supporting disclosure`() {
        val segments = listOf(
            OcrRecognitionSegment(text = "page one", fidelity = TranscriptionFidelity.VERBATIM, pageNumber = 1),
            OcrRecognitionSegment(text = "page two", fidelity = TranscriptionFidelity.VERBATIM, pageNumber = 2),
        )
        val result = sampleResult(segments = segments)

        assertEquals(listOf(1, 2), result.segments.map { it.pageNumber }, "page ordering must remain discoverable, unmodified, from Unit 6's own segments field")
    }

    // -- Partial-result status is discoverable from the outcome, not fabricated on results where processing succeeded cleanly --

    @Test
    fun `partial-result status is discoverable from the outcome type itself, distinguishing PartialOrDegradedOutput from plain Recognised`() {
        val plain: OcrRecognitionOutcome = OcrRecognitionOutcome.Recognised(sampleResult())
        val degraded: OcrRecognitionOutcome = OcrRecognitionOutcome.PartialOrDegradedOutput(sampleResult(), "degraded")

        assertTrue(plain is OcrRecognitionOutcome.Recognised)
        assertTrue(degraded is OcrRecognitionOutcome.PartialOrDegradedOutput)
        assertFalse(plain is OcrRecognitionOutcome.PartialOrDegradedOutput, "a clean recognition must never itself carry partial-result status")
    }

    // -- Output digest: recognisedText alone is sufficient input, no dedicated field required or added --

    private fun sha256Hex(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it.toInt() and 0xFF) }

    @Test
    fun `recognisedText alone is sufficient input for a future, downstream output-hash computation -- deterministic, and unique to its own content`() {
        val resultA = sampleResult(text = "identical content")
        val resultB = sampleResult(text = "identical content")
        val resultC = sampleResult(text = "different content")

        val digestA1 = sha256Hex(resultA.recognisedText.toByteArray(Charsets.UTF_8))
        val digestA2 = sha256Hex(resultA.recognisedText.toByteArray(Charsets.UTF_8))
        val digestB = sha256Hex(resultB.recognisedText.toByteArray(Charsets.UTF_8))
        val digestC = sha256Hex(resultC.recognisedText.toByteArray(Charsets.UTF_8))

        assertEquals(digestA1, digestA2, "the same recognisedText must always produce the same digest -- determinism")
        assertEquals(digestA1, digestB, "identical recognisedText content across two distinct results must produce the same digest")
        assertNotEquals(digestA1, digestC, "different recognisedText content must produce a different digest")
    }

    @Test
    fun `OcrRecognitionResult carries no dedicated digest or hash field of its own -- Unit 8 added none, none was required`() {
        val fieldNames = OcrRecognitionResult::class.declaredMemberProperties.map { it.name.lowercase() }
        listOf("hash", "digest", "checksum").forEach { forbidden ->
            assertFalse(
                fieldNames.any { it.contains(forbidden) },
                "OcrRecognitionResult must carry no '$forbidden' field -- Scope Lock Section 9 names an output hash " +
                    "as consistent with the existing, generic CandidateDocument.integrityHash mechanism, computed " +
                    "downstream from recognisedText, never a field this Unit adds.",
            )
        }
    }

    // -- Outcomes without output do not fabricate output disclosure --

    @Test
    fun `outcomes carrying no OcrRecognitionResult expose no property of that type -- no output disclosure is fabricated where no output exists`() {
        val outcomesWithoutOutput = listOf(
            OcrRecognitionOutcome.Failed::class,
            OcrRecognitionOutcome.NotAuthorised::class,
            OcrRecognitionOutcome.UnsupportedOrInaccessibleInput::class,
            OcrRecognitionOutcome.NoRecognisableContent::class,
            OcrRecognitionOutcome.ValidationRejection::class,
            OcrRecognitionOutcome.ProcessingOrDependencyFailure::class,
            OcrRecognitionOutcome.GenuineImplementationFault::class,
        )

        outcomesWithoutOutput.forEach { type ->
            val hasResultProperty = type.declaredMemberProperties.any { it.returnType.classifier == OcrRecognitionResult::class }
            assertFalse(
                hasResultProperty,
                "${type.simpleName} must carry no OcrRecognitionResult-typed property -- no recognition occurred, " +
                    "so no output, and therefore no output-hash-sufficient or provenance-supporting disclosure, may " +
                    "be fabricated for it.",
            )
        }
    }

    // -- No canonical Provenance/CandidateProvenance/registration/Memory/Knowledge type in the public OCR model --

    @Test
    fun `no OCR outcome or result type references Provenance, CandidateProvenance, MemoryCore, or any evidence-registration type`() {
        val excludedQualifiedNames = setOf(
            Provenance::class.qualifiedName,
            CandidateProvenance::class.qualifiedName,
            MemoryCore::class.qualifiedName,
            MemoryRetrieval::class.qualifiedName,
            CandidateDocument::class.qualifiedName,
            CandidateEvidenceArtifact::class.qualifiedName,
            EvidenceRegistrationOutcome::class.qualifiedName,
            KnowledgeSubmission::class.qualifiedName,
            PermissionEngine::class.qualifiedName,
            EvidenceCustodian::class.qualifiedName,
            EvidenceIntelligence::class.qualifiedName,
        )

        val typesToCheck = listOf(
            OcrRecognitionRequest::class, OcrRecognitionIdentity::class, OcrRecognitionSegment::class,
            OcrRecognitionResult::class, OcrRecognitionOutcome::class,
        ) + OcrRecognitionOutcome::class.sealedSubclasses

        typesToCheck.forEach { type ->
            val propertyTypeNames = type.declaredMemberProperties.mapNotNull { (it.returnType.classifier as? kotlin.reflect.KClass<*>)?.qualifiedName }
            propertyTypeNames.forEach { qualifiedName ->
                assertFalse(
                    excludedQualifiedNames.contains(qualifiedName),
                    "${type.simpleName} must not reference '$qualifiedName' -- provenance construction, evidence " +
                        "registration/acceptance, Memory, Knowledge, Permission, Evidence Custodian, and Evidence " +
                        "Intelligence all remain outside the OCR mechanism's own public model (Scope Lock Section 13; " +
                        "Implementation Plan Unit 8's own Constitutional constraints).",
                )
            }
        }
    }

    @Test
    fun `no file implementing Units 1-8 references Provenance, CandidateProvenance, or MemoryCore -- the prohibition named by Unit 8 governs production code, not this required verification test`() {
        val forbiddenFragments = listOf("Provenance", "MemoryCore")

        ocrUnitFiles.forEach { file ->
            check(file.exists()) { "${file.path} not found from working directory ${java.io.File(".").absolutePath}" }
            val codeOnly = file.readText().codeOnly()
            forbiddenFragments.forEach { forbidden ->
                assertFalse(
                    codeOnly.contains(forbidden),
                    "${file.path} must not reference '$forbidden' -- Implementation Plan Unit 8's own Files " +
                        "explicitly prohibited names exactly this for Units 1-8's own production files.",
                )
            }
        }
    }

    // -- No provider-specific, truth, reliability, validation, or acceptance field in this Unit's own additions --

    @Test
    fun `this Unit introduced no new public production field -- OcrRecognitionResult's own field set is unchanged from Unit 6-7`() {
        val fieldNames = OcrRecognitionResult::class.declaredMemberProperties.map { it.name }.toSet()

        assertEquals(
            setOf("recognisedText", "fidelity", "identity", "confidence", "recognisedAt", "warnings", "segments"),
            fieldNames,
            "Implementation Plan Unit 8 is a verification unit -- 'Outputs: Confirmation (by test, not by new " +
                "code)' -- and this test confirms no field was added: found $fieldNames",
        )
    }

    // -- Units 1-7 behaviour remains intact --

    @Test
    fun `OcrRecognitionOutcome retains exactly its own nine Unit 1 and Unit 7 variants -- Unit 8 added no tenth`() {
        val subclassNames = OcrRecognitionOutcome::class.sealedSubclasses.map { it.simpleName }.toSet()

        assertEquals(
            setOf(
                "Recognised", "Failed", "NotAuthorised", "UnsupportedOrInaccessibleInput", "NoRecognisableContent",
                "PartialOrDegradedOutput", "ValidationRejection", "ProcessingOrDependencyFailure", "GenuineImplementationFault",
            ),
            subclassNames,
        )
    }
}
