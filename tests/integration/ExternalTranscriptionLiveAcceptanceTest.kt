package parker.core.runtime

import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.apache.pdfbox.pdmodel.font.Standard14Fonts
import parker.composition.OpenAiExternalTranscriptionProviderReadinessEvaluator
import parker.composition.OpenAiExternalTranscriptionReadiness
import parker.core.interfaces.*

/** Unit N: one opt-in, synthetic-only, real-provider acceptance through Units D/E/I/H/C/J. */
class ExternalTranscriptionLiveAcceptanceTest {
    private val owner = PrincipalId("owner.unit-n.synthetic-live")
    private val evidenceId = EvidenceArtifactId("evidence-unit-n-synthetic-two-page")
    private val events = mutableListOf<String>()

    @Test
    fun `one governed synthetic request validates admits and survives restart`() = runTest {
        require(System.getProperty("parker.externalTranscription.live.enabled") == "true") { "live acceptance is not explicitly enabled" }
        val profilePath = System.getenv("PARKER_OPENAI_EXTERNAL_TRANSCRIPTION_PROVIDER_PROFILE_PATH")
            ?: error("provider profile path is absent")
        val readiness = OpenAiExternalTranscriptionProviderReadinessEvaluator().evaluate(true, profilePath)
        val ready = readiness as? OpenAiExternalTranscriptionReadiness.Ready
            ?: error("provider profile is not Ready: ${readiness::class.simpleName}")
        require(ready.profile.modelSelectionRule == "gpt-4.1-mini") { "profile model changed; no silent switch is permitted" }
        val resultPath = System.getenv("PARKER_EXTERNAL_TRANSCRIPTION_LIVE_RESULT_PATH")?.let(Path::of)
            ?: error("bounded live result path is absent")
        require(!resultPath.toAbsolutePath().normalize().startsWith(Path.of("").toAbsolutePath().normalize())) {
            "live result must remain outside the repository"
        }

        val source = syntheticPdf()
        val sourceBefore = source.copyOf()
        val digest = sha256(source)
        val manifest = EvidenceSourceManifest(evidenceId, digest, source.size.toLong(), "application/pdf")
        val custodian = SyntheticCustodian(source, manifest)
        val permission = ExactPurposePermission()
        val adapterHandle = OpenAiLiveAcceptanceBridge.create(ready) { events += "egress" }
        val generationRoot = Files.createTempDirectory("unit-n-live-generations")
        val contentRoot = Files.createTempDirectory("unit-n-live-content")
        val generationStorage = FileSystemDerivativeGenerationStorage(generationRoot)
        val contentStorage = FileSystemDerivativeContentStorage(contentRoot)
        val admission = DerivativeGenerationCoordinator(
            CsvStructuralExtractor { error("CSV extraction is unreachable") },
            generationStorage,
            DocumentIngestionAudit { },
            now = { Instant.now() },
            contentStorage = contentStorage,
        )
        val coordinator = ExternalTranscriptionOwnerInvocationCoordinator(
            owner, permission, custodian, adapterHandle.mechanism, OcrStructuredResultValidator(), admission,
            correlationFactory = { "unit-n-live-correlation" },
        )

        val outcome = coordinator.invoke(evidenceId)
        val admitted = outcome as? ExternalTranscriptionOwnerInvocationOutcome.Admitted
            ?: error(
                "live invocation did not admit: ${safeOutcome(outcome)}" +
                    (adapterHandle.state.failureFingerprint?.let { " $it" } ?: "") +
                    (adapterHandle.state.providerRejectionFingerprint?.let { " $it" } ?: ""),
            )

        assertEquals(listOf("authorize", "source", "manifest", "egress"), events)
        assertEquals(1, adapterHandle.state.calls)
        assertEquals(1, custodian.sourceCalls)
        assertEquals(1, custodian.manifestCalls)
        assertContentEquals(sourceBefore, source)
        assertEquals(digest, sha256(source))
        assertTrue(adapterHandle.state.storeFalse)
        assertTrue(adapterHandle.state.approvedEndpoint)

        val extracted = admitted.extracted
        assertEquals(TranscriptionFidelity.UNVERIFIED_LITERAL_TRANSCRIPTION, extracted.fidelity)
        assertTrue(extracted.recognisedText.contains("SYNTHETIC_PAGE_ONE_MARKER"))
        assertTrue(extracted.recognisedText.contains("SYNTHETIC_PAGE_TWO_MARKER"))
        assertTrue(extracted.recognisedText.contains("Synthetic Person Alpha"))
        assertTrue(extracted.recognisedText.contains("14 September 2026"))
        assertTrue(extracted.recognisedText.contains("$123.45"))
        val accounting = requireNotNull(extracted.pageAccounting)
        assertEquals(listOf(1, 2), accounting.requestedScope.pageNumbers)
        assertEquals(listOf(1, 2), accounting.submittedScope.pageNumbers)
        assertEquals(listOf(1, 2), accounting.returnedScope.pageNumbers)
        assertEquals(setOf(1, 2), accounting.pageOutcomes.map { it.pageNumber }.toSet())
        val pageTwo = accounting.pageOutcomes.single { it.pageNumber == 2 }
        assertTrue(
            pageTwo.outcome == OcrPageOutcomeKind.TRANSCRIBED_WITH_QUALIFICATIONS ||
                pageTwo.outcome == OcrPageOutcomeKind.ILLEGIBLE_OR_NO_RECOGNISABLE_CONTENT ||
                pageTwo.uncertaintySpans.isNotEmpty() || pageTwo.warnings.isNotEmpty(),
            "page two obscured region was not qualified",
        )
        val provider = requireNotNull(extracted.providerProvenance)
        assertEquals("OpenAI", provider.providerIdentity)
        assertTrue(provider.providerReportedModelIdentifier.isNotBlank())
        assertTrue(provider.providerCorrelationIdentifier.isNotBlank())
        val processing = requireNotNull(extracted.processingProvenance)
        assertTrue(processing.byteExactCopy)
        assertEquals(digest, processing.sourceManifestSha256.value)
        assertEquals(digest, processing.representationSha256.value)

        val restarted = TierBOcrContentRetrievalCoordinator(
            FileSystemDerivativeGenerationStorage(generationRoot),
            FileSystemDerivativeContentStorage(contentRoot),
        )
        val restored = restarted.retrieve(evidenceId, admitted.record.derivativeGenerationId) as? TierBOcrContentRetrievalOutcome.Retrieved
            ?: error("restart retrieval failed")
        assertEquals(extracted, restored.extracted)

        Files.createDirectories(resultPath.parent)
        Files.writeString(
            resultPath,
            listOf(
                "status=PASS",
                "commit=${System.getenv("PARKER_LIVE_ACCEPTANCE_COMMIT") ?: "not-supplied"}",
                "profileModel=${ready.profile.modelSelectionRule}",
                "fixtureSha256=$digest",
                "fixtureBytes=${source.size}",
                "evidenceArtifactId=${evidenceId.value}",
                "derivativeGenerationId=${admitted.record.derivativeGenerationId.value}",
                "provider=${provider.providerIdentity}",
                "returnedModel=${provider.providerReportedModelIdentifier}",
                "responseId=${provider.providerCorrelationIdentifier}",
                "requestedPages=${accounting.requestedScope.pageNumbers.joinToString(",")}",
                "returnedPages=${accounting.returnedScope.pageNumbers.joinToString(",")}",
                "pageTwoOutcome=${pageTwo.outcome}",
                "fidelity=${extracted.fidelity}",
                "completeness=${extracted.completenessState}",
                "requestCount=${adapterHandle.state.calls}",
                "localOcrInvocationCount=0",
                "analysisInvocationCount=0",
                "timeoutMillis=${ready.effectiveLimits.timeoutMillis}",
            ).joinToString("\n", postfix = "\n"),
        )
        println("UNIT_N_LIVE_ACCEPTANCE_PASS evidence=${evidenceId.value} generation=${admitted.record.derivativeGenerationId.value} requestCount=1")
    }

    private inner class ExactPurposePermission : PermissionEngine {
        override suspend fun evaluate(request: ExecutionRequest): PermissionDecision {
            events += "authorize"
            assertEquals(ExternalTranscriptionInvocationGate.AUTHORIZATION_PURPOSE, request.authorizationPurpose)
            assertEquals(ExternalTranscriptionInvocationGate.ACTION_NAME, request.proposedActions.single())
            assertEquals(evidenceId.value, request.metadata[ExternalTranscriptionInvocationGate.EVIDENCE_ARTIFACT_ID_METADATA_KEY])
            return PermissionDecision(
                DecisionId("unit-n-approved"), request.principalId, request.targetResources.single(),
                PermissionAction.EXECUTE, PermissionDecisionOutcome.APPROVED, PermissionLevel.AUTOMATIC, Instant.now(),
            )
        }
        override suspend fun explain(decisionId: DecisionId): PermissionExplanation = error("unreachable")
    }

    private inner class SyntheticCustodian(
        source: ByteArray,
        private val manifest: EvidenceSourceManifest,
    ) : EvidenceCustodian {
        private val immutableSource = source.copyOf()
        var sourceCalls = 0
        var manifestCalls = 0
        override suspend fun accept(requestingPrincipalId: PrincipalId, candidate: CandidateEvidenceArtifact) = error("unreachable")
        override suspend fun retrieve(requestingPrincipalId: PrincipalId, evidenceArtifactId: EvidenceArtifactId): EvidenceRetrievalResult {
            events += "source"; sourceCalls += 1
            return EvidenceRetrievalResult.Found(evidenceArtifactId, immutableSource.copyOf())
        }
        override suspend fun retrieveManifest(requestingPrincipalId: PrincipalId, evidenceArtifactId: EvidenceArtifactId): EvidenceManifestRetrievalResult {
            events += "manifest"; manifestCalls += 1
            return EvidenceManifestRetrievalResult.Found(manifest)
        }
    }

    private fun syntheticPdf(): ByteArray {
        val output = ByteArrayOutputStream()
        PDDocument().use { document ->
            val font = PDType1Font(Standard14Fonts.FontName.HELVETICA)
            fun page(draw: PDPageContentStream.() -> Unit) {
                val page = PDPage(PDRectangle.LETTER)
                document.addPage(page)
                PDPageContentStream(document, page).use { it.draw() }
            }
            page {
                writeLine(font, 12f, 72f, 720f, "SYNTHETIC_PAGE_ONE_MARKER")
                writeLine(font, 12f, 72f, 690f, "Synthetic Person Alpha and Synthetic Person Beta met on 14 September 2026.")
                writeLine(font, 12f, 72f, 660f, "The synthetic amount recorded was $123.45; punctuation: commas, colons, and semicolons.")
                writeLine(font, 12f, 72f, 630f, "For avoidance of doubt, this synthetic clause creates no rights, duties, or legal obligations.")
            }
            page {
                writeLine(font, 12f, 72f, 720f, "SYNTHETIC_PAGE_TWO_MARKER")
                writeLine(font, 12f, 72f, 690f, "This is ordinary readable synthetic text on page two.")
                setNonStrokingColor(0.6f)
                writeLine(font, 12f, 72f, 660f, "This lower-contrast sentence remains intentionally readable.")
                setNonStrokingColor(0f)
                writeLine(font, 12f, 72f, 625f, "The region below is deliberately obscured and must not be guessed:")
                addRect(72f, 575f, 360f, 28f)
                fill()
                writeLine(font, 12f, 72f, 545f, "Clear text resumes after the obscured region.")
            }
            document.save(output)
        }
        return output.toByteArray()
    }

    private fun PDPageContentStream.writeLine(font: PDType1Font, size: Float, x: Float, y: Float, text: String) {
        beginText(); setFont(font, size); newLineAtOffset(x, y); showText(text); endText()
    }

    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes).joinToString("") { "%02x".format(it) }

    private fun safeOutcome(outcome: ExternalTranscriptionOwnerInvocationOutcome): String = when (outcome) {
        ExternalTranscriptionOwnerInvocationOutcome.NotAuthorised -> "NOT_AUTHORISED"
        is ExternalTranscriptionOwnerInvocationOutcome.SourceNotFound -> "SOURCE_NOT_FOUND"
        is ExternalTranscriptionOwnerInvocationOutcome.SourceRetrievalRejected -> "SOURCE_RETRIEVAL_REJECTED"
        is ExternalTranscriptionOwnerInvocationOutcome.ManifestNotFound -> "MANIFEST_NOT_FOUND"
        is ExternalTranscriptionOwnerInvocationOutcome.ManifestRejected -> "MANIFEST_REJECTED"
        is ExternalTranscriptionOwnerInvocationOutcome.ByteLengthMismatch -> "BYTE_LENGTH_MISMATCH"
        is ExternalTranscriptionOwnerInvocationOutcome.DigestMismatch -> "DIGEST_MISMATCH"
        is ExternalTranscriptionOwnerInvocationOutcome.UnsupportedOrOutOfBounds -> "UNSUPPORTED_OR_OUT_OF_BOUNDS"
        is ExternalTranscriptionOwnerInvocationOutcome.MechanismFailure -> "MECHANISM_FAILURE:${outcome.reason}"
        is ExternalTranscriptionOwnerInvocationOutcome.ValidationRejected -> "VALIDATION_REJECTED"
        is ExternalTranscriptionOwnerInvocationOutcome.AdmissionFailed -> "ADMISSION_FAILED"
        is ExternalTranscriptionOwnerInvocationOutcome.ReconciliationRequired -> "RECONCILIATION_REQUIRED"
        is ExternalTranscriptionOwnerInvocationOutcome.Admitted -> "ADMITTED"
    }
}
