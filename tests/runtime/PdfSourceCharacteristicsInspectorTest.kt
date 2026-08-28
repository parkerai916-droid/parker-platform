package parker.core.runtime

import java.io.ByteArrayOutputStream
import java.awt.image.BufferedImage
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import kotlin.test.*
import kotlinx.coroutines.test.runTest
import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.encryption.AccessPermission
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy
import org.apache.pdfbox.pdmodel.font.Standard14Fonts
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory
import parker.core.interfaces.*

class PdfSourceCharacteristicsInspectorTest {
    private val owner = PrincipalId("owner")
    private val id = EvidenceArtifactId("pdf-characteristic-source")
    private val inspector = PdfSourceCharacteristicsInspector()

    @Test fun `born digital PDF establishes native traits but does not select source-inaccurate native routing`() = runTest {
        val bytes = pdf(textPages = 1, imagePages = 0)
        val inspected = assertIs<PdfSourceCharacteristicsInspection.Established>(inspect(bytes))
        assertEquals(AcquisitionCharacteristicState.PRESENT, inspected.nativeSearchableText)
        assertEquals(AcquisitionCharacteristicState.ABSENT, inspected.imageOnlyOrScanned)
        assertEquals(AcquisitionCharacteristicState.ABSENT, inspected.mixedTextAndImage)
        assertEquals(AcquisitionPageCount.Known(1), inspected.pageCount)
        val source = source(bytes, inspected)
        val routed = assertIs<EvidenceAcquisitionRoutingOutcome.NoEligibleCapability>(
            DeterministicEvidenceAcquisitionRouter().route(
                source, ProductionAcquisitionCapabilityCatalogue.create().capabilities(),
                ExternalEgressAuthorisation.NOT_AUTHORISED,
            ),
        )
        assertContains(routed.reasons, AcquisitionNoSelectionReason.NO_ACCEPTED_FIDELITY_SUITABLE_CAPABILITY)
    }

    @Test fun `image only PDF establishes scan traits and never selects native`() = runTest {
        val bytes = pdf(textPages = 0, imagePages = 1)
        val inspected = assertIs<PdfSourceCharacteristicsInspection.Established>(inspect(bytes))
        assertEquals(AcquisitionCharacteristicState.ABSENT, inspected.nativeSearchableText)
        assertEquals(AcquisitionCharacteristicState.PRESENT, inspected.imageOnlyOrScanned)
        val routed = assertIs<EvidenceAcquisitionRoutingOutcome.Selected>(
            DeterministicEvidenceAcquisitionRouter().route(
                source(bytes, inspected), ProductionAcquisitionCapabilityCatalogue.create().capabilities(),
                ExternalEgressAuthorisation.NOT_AUTHORISED,
            ),
        )
        assertEquals(ProductionAcquisitionCapabilityCatalogue.LOCAL_OCR_CAPABILITY_ID, routed.decision.capability.capabilityId)
    }

    @Test fun `mixed text and image-only pages are classified deterministically`() = runTest {
        val bytes = pdf(textPages = 1, imagePages = 1)
        val first = assertIs<PdfSourceCharacteristicsInspection.Established>(inspect(bytes))
        val second = assertIs<PdfSourceCharacteristicsInspection.Established>(inspect(bytes))
        assertEquals(first, second)
        assertEquals(AcquisitionCharacteristicState.PRESENT, first.nativeSearchableText)
        assertEquals(AcquisitionCharacteristicState.PRESENT, first.mixedTextAndImage)
        assertEquals(AcquisitionPageCount.Known(2), first.pageCount)
    }

    @Test fun `malformed and encrypted PDFs fail closed without fabricated traits`() = runTest {
        assertIs<PdfSourceCharacteristicsInspection.Indeterminate>(inspect("%PDF-not-valid".toByteArray()))
        val encrypted = pdf(textPages = 1, imagePages = 0, encrypted = true)
        assertIs<PdfSourceCharacteristicsInspection.Indeterminate>(inspect(encrypted))
    }

    @Test fun `digest mismatch admits no inspection result`() = runTest {
        val bytes = pdf(textPages = 1, imagePages = 0)
        val resolution = AuthoritativeAcquisitionSourceResolver(custodian(bytes, manifestDigest = "0".repeat(64)))
            .resolve(owner, id)
        assertIs<AuthoritativeAcquisitionResolution.DigestMismatch>(resolution)
    }

    @Test fun `inspection result contains provenance and technical facts but no text carrier`() = runTest {
        val bytes = pdf(textPages = 1, imagePages = 0)
        val result = assertIs<PdfSourceCharacteristicsInspection.Established>(inspect(bytes))
        assertEquals(PdfSourceCharacteristicsInspector.MECHANISM_IDENTITY, result.mechanismIdentity)
        assertEquals(digest(bytes), result.sourceSha256)
        val fieldNames = result::class.java.declaredFields.map { it.name.lowercase() }
        assertFalse(fieldNames.any { it in setOf("text", "content", "transcript", "extractedtext") })
        assertFalse(result.toString().contains("structural-marker"))
    }

    @Test fun `authorised external acceptance witness can be inspected and routed without execution`() = runTest {
        val path = System.getenv("PARKER_FA92R_PDF_PATH")?.let(Path::of) ?: return@runTest
        val expectedDigest = requireNotNull(System.getenv("PARKER_FA92R_PDF_SHA256"))
        val expectedLength = requireNotNull(System.getenv("PARKER_FA92R_PDF_LENGTH")).toLong()
        val bytes = Files.readAllBytes(path)
        assertEquals(expectedLength, bytes.size.toLong())
        assertEquals(expectedDigest, digest(bytes))
        val inspected = assertIs<PdfSourceCharacteristicsInspection.Established>(inspect(bytes))
        assertEquals(AcquisitionCharacteristicState.PRESENT, inspected.nativeSearchableText)
        val routed = assertIs<EvidenceAcquisitionRoutingOutcome.Selected>(
            DeterministicEvidenceAcquisitionRouter().route(
                source(bytes, inspected), ProductionAcquisitionCapabilityCatalogue.create().capabilities(),
                ExternalEgressAuthorisation.NOT_AUTHORISED,
            ),
        )
        assertEquals(ProductionAcquisitionCapabilityCatalogue.NATIVE_CAPABILITY_ID, routed.decision.capability.capabilityId)
        assertEquals(EvidenceAcquisitionMechanism.DIRECT_NATIVE_EXTRACTION, routed.decision.capability.mechanism)
        assertEquals(AcquisitionEgress.LOCAL_ONLY, routed.decision.capability.egress)
        println("FA92R_TECHNICAL_FACTS pageCount=${inspected.pageCount.value} " +
            "nativeSearchableText=${inspected.nativeSearchableText} imageOnlyOrScanned=${inspected.imageOnlyOrScanned} " +
            "mixedTextAndImage=${inspected.mixedTextAndImage} capability=${routed.decision.capability.capabilityId} " +
            "mechanism=${routed.decision.capability.mechanism} egress=${routed.decision.capability.egress}")
    }

    private suspend fun inspect(bytes: ByteArray): PdfSourceCharacteristicsInspection {
        val resolved = assertIs<AuthoritativeAcquisitionResolution.Verified>(
            AuthoritativeAcquisitionSourceResolver(custodian(bytes)).resolve(owner, id),
        )
        return inspector.inspect(resolved.input)
    }

    private fun source(bytes: ByteArray, facts: PdfSourceCharacteristicsInspection.Established) =
        AcquisitionSource(id, digest(bytes), bytes.size.toLong(), "application/pdf", facts.pageCount,
            AcquisitionSourceCharacteristics(facts.nativeSearchableText, facts.imageOnlyOrScanned,
                facts.mixedTextAndImage, AcquisitionCharacteristicState.ABSENT,
                AcquisitionCharacteristicState.ABSENT, AcquisitionCharacteristicState.ABSENT),
            HumanAuthorisedCustody.CONFIRMED)

    private fun custodian(bytes: ByteArray, manifestDigest: String = digest(bytes)) = object : EvidenceCustodian {
        override suspend fun accept(requestingPrincipalId: PrincipalId, candidate: CandidateEvidenceArtifact) =
            EvidenceAcceptanceResult.Rejected("unused")
        override suspend fun retrieve(requestingPrincipalId: PrincipalId, evidenceArtifactId: EvidenceArtifactId) =
            EvidenceRetrievalResult.Found(id, bytes)
        override suspend fun retrieveManifest(requestingPrincipalId: PrincipalId, evidenceArtifactId: EvidenceArtifactId) =
            EvidenceManifestRetrievalResult.Found(EvidenceSourceManifest(id, manifestDigest, bytes.size.toLong(), "application/pdf"))
    }

    private fun pdf(textPages: Int, imagePages: Int, encrypted: Boolean = false): ByteArray {
        val output = ByteArrayOutputStream()
        PDDocument().use { document ->
            repeat(textPages) {
                val page = PDPage(); document.addPage(page)
                PDPageContentStream(document, page).use { stream ->
                    stream.beginText(); stream.setFont(PDType1Font(Standard14Fonts.FontName.HELVETICA), 12f)
                    stream.newLineAtOffset(72f, 700f); stream.showText("structural-marker"); stream.endText()
                }
            }
            repeat(imagePages) {
                val page = PDPage(); document.addPage(page)
                val image = LosslessFactory.createFromImage(document, BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB))
                PDPageContentStream(document, page).use { it.drawImage(image, 72f, 700f, 1f, 1f) }
            }
            if (encrypted) document.protect(StandardProtectionPolicy("owner-password", "user-password", AccessPermission()))
            document.save(output)
        }
        return output.toByteArray()
    }

    private fun digest(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes)
        .joinToString("") { "%02x".format(it.toInt() and 0xff) }
}
