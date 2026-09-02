package parker.core.runtime

import java.io.ByteArrayOutputStream
import java.nio.file.Files
import kotlinx.coroutines.test.runTest
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.apache.pdfbox.pdmodel.font.Standard14Fonts
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import parker.core.interfaces.*
import kotlin.test.*

class GovernedCorrectedPreparationServiceTest {
    @TempDir lateinit var temp: java.nio.file.Path
    private val owner = PrincipalId("user.synthetic-preparation-owner")
    private val evidenceId = EvidenceArtifactId("evidence-synthetic-preparation-only")

    @Test fun `governed evidence prepares persists reads back and replays idempotently without execution state`() = runTest {
        val bytes = pdf(); val sha = CanonicalPagePixelDigests.sha256(bytes)
        val storeRoot = temp.resolve("corrected"); val service = service(bytes, sha, storeRoot)
        val first = assertIs<GovernedCorrectedPreparationOutcome.Prepared>(service.prepare(evidenceId)).result
        val second = assertIs<GovernedCorrectedPreparationOutcome.Prepared>(service.prepare(evidenceId)).result
        assertEquals(first, second); assertTrue(first.readbackVerified)
        assertEquals(listOf(1), first.pages.map { it.pageNumber })
        assertEquals(listOf("DETERMINISTIC_SOURCE_ORDER"), first.pages.map { it.orderState })
        assertEquals(1, first.requestRegionCount)
        assertEquals(1, Files.list(storeRoot.resolve("records")).use { it.count() })
        assertEquals(1, Files.list(storeRoot.resolve("transport")).use { it.count() })
        assertEquals("ordinary-external-request-region-transcription-v8", ORDINARY_REQUEST_REGION_V8_CAPABILITY_ID)
        assertEquals("c0479979720455d2de3fc9861eeb5dee323a4770bdb15f807af611ad426f9ec0", OrdinaryRequestRegionV8Capability().digest())
    }

    @Test fun `missing evidence unsupported profile media chromatic risk and conflicts fail closed`() = runTest {
        val bytes = pdf(); val sha = CanonicalPagePixelDigests.sha256(bytes); val root = temp.resolve("failures")
        val service = service(bytes, sha, root)
        assertEquals("UNSUPPORTED_PREPARATION_PROFILE", assertIs<GovernedCorrectedPreparationOutcome.Rejected>(
            service.prepare(evidenceId, "other-profile", 1)).reason)
        assertContains(assertIs<GovernedCorrectedPreparationOutcome.Rejected>(service(missing = true).prepare(evidenceId)).reason,
            "GOVERNED_EVIDENCE_UNAVAILABLE")
        assertEquals("UNSUPPORTED_MEDIA", assertIs<GovernedCorrectedPreparationOutcome.Rejected>(
            service(bytes, sha, temp.resolve("media"), "text/plain").prepare(evidenceId)).reason)
        val prepared = assertIs<GovernedCorrectedPreparationOutcome.Prepared>(service.prepare(evidenceId)).result
        Files.writeString(root.resolve("records/${prepared.preparationIdentity}.json"), "conflict")
        assertContains(assertIs<GovernedCorrectedPreparationOutcome.Rejected>(service.prepare(evidenceId)).reason, "conflict")
    }

    private fun service(
        bytes: ByteArray = pdf(), sha: String = CanonicalPagePixelDigests.sha256(bytes),
        root: java.nio.file.Path = temp.resolve("store"), mediaType: String = "application/pdf", missing: Boolean = false,
    ): GovernedCorrectedPreparationService {
        val custodian = object : EvidenceCustodian {
            override suspend fun accept(requestingPrincipalId: PrincipalId, candidate: CandidateEvidenceArtifact): EvidenceAcceptanceResult = error("not used")
            override suspend fun retrieve(requestingPrincipalId: PrincipalId, evidenceArtifactId: EvidenceArtifactId): EvidenceRetrievalResult =
                if (missing) EvidenceRetrievalResult.NotFound(evidenceArtifactId) else EvidenceRetrievalResult.Found(evidenceArtifactId, bytes)
            override suspend fun retrieveManifest(requestingPrincipalId: PrincipalId, evidenceArtifactId: EvidenceArtifactId): EvidenceManifestRetrievalResult =
                if (missing) EvidenceManifestRetrievalResult.NotFound(evidenceArtifactId) else EvidenceManifestRetrievalResult.Found(
                    EvidenceSourceManifest(evidenceArtifactId, sha, bytes.size.toLong(), mediaType, "synthetic.pdf"))
        }
        return GovernedCorrectedPreparationService(custodian, owner, FileSystemFullPageAchromaticPreparationStore(root))
    }

    private fun pdf(): ByteArray = PDDocument().use { document ->
        val page = PDPage(PDRectangle(240f, 240f)); document.addPage(page)
        PDPageContentStream(document, page).use { stream ->
            stream.beginText(); stream.setFont(PDType1Font(Standard14Fonts.FontName.HELVETICA), 12f)
            stream.newLineAtOffset(30f, 180f); stream.showText("Synthetic preparation-only fixture"); stream.endText()
        }
        ByteArrayOutputStream().use { output -> document.save(output); output.toByteArray() }
    }
}
