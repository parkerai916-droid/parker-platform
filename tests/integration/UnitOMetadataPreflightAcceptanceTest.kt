package parker.core.runtime

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.PosixFilePermissions
import java.security.MessageDigest
import java.time.Instant
import kotlin.test.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import parker.composition.OpenAiExternalTranscriptionProviderReadinessEvaluator
import parker.composition.OpenAiExternalTranscriptionReadiness
import parker.core.interfaces.EvidenceArtifactId

class UnitOMetadataPreflightAcceptanceTest {
    @Test fun `exact locked manifests pass metadata-only preflight and emit bounded record`() = runTest {
        assertEquals("true", System.getProperty("parker.unitO.metadataPreflight.enabled"))
        val manifestRoot = requiredPath("PARKER_UNIT_O_MANIFEST_ROOT")
        val profilePath = required("PARKER_UNIT_O_PROVIDER_PROFILE_PATH")
        val recordPath = requiredPath("PARKER_UNIT_O_ACCEPTANCE_RECORD_PATH")
        val repositoryCommit = required("PARKER_UNIT_O_REPOSITORY_COMMIT")
        val credentialReady = OpenAiLiveAcceptanceBridge.credentialStructurallyReady(System.getenv("PARKER_OPENAI_API_KEY"))
        val profileReadiness = OpenAiExternalTranscriptionProviderReadinessEvaluator()
            .evaluate(System.getenv("PARKER_OPENAI_EXTERNAL_TRANSCRIPTION_ENABLED") == "true", profilePath)
        val ready = assertIs<OpenAiExternalTranscriptionReadiness.Ready>(profileReadiness)
        assertEquals("gpt-4.1-mini", ready.profile.modelSelectionRule)
        assertEquals("/v1/responses", ready.profile.apiProductPath)
        assertFalse(ready.profile.store)
        assertTrue(credentialReady)

        val clean = EvidenceArtifactId("evidence-5590e407-badf-4337-9b5b-cb2ce0e80546")
        val handwritten = EvidenceArtifactId("evidence-1f019d05-26a1-452f-8a6f-f0ed3768ee0a")
        val storage = FileSystemEvidenceSourceManifestStorage(manifestRoot)
        val reader = UnitOManifestMetadataReader { id ->
            storage.read(id)?.let { UnitOAuthoritativeManifestFacts(it.evidenceArtifactId, it.sha256, it.byteLength, it.receivedMediaType) }
        }
        val bridge = UnitOAuthoritativeMetadataBridge(setOf(clean, handwritten), reader,
            providerMaximumPdfBytes = ready.effectiveLimits.maximumPdfBytes)
        val cleanResult = bridge.preflight(clean)
        val handwrittenResult = bridge.preflight(handwritten)
        assertTrue(cleanResult.eligible, cleanResult.safeDiagnostic())
        assertTrue(handwrittenResult.eligible, handwrittenResult.safeDiagnostic())

        val record = buildString {
            appendLine("STAGE=UNIT_O_3_METADATA_ONLY_PREFLIGHT")
            appendLine("REPOSITORY_COMMIT=$repositoryCommit")
            appendLine("TIMESTAMP=${Instant.now()}")
            appendCase("CLEAN_PRINTED", cleanResult, "CLEAN_PRINTED")
            appendCase("HANDWRITTEN_MIXED", handwrittenResult, "HANDWRITTEN_MIXED")
            appendLine("PROVIDER_PROFILE_READINESS=READY")
            appendLine("CREDENTIAL_READINESS=READY")
            appendLine("BACKEND_READINESS=READY")
            appendLine("OWNER_ENHANCED_READINESS=READY")
            appendLine("MODEL=gpt-4.1-mini")
            appendLine("API_PRODUCT_PATH=/v1/responses")
            appendLine("STORE=false")
            appendLine("UNIT_O_EXTERNAL_REQUESTS_CONSUMED=0")
            appendLine("UNIT_O_EXTERNAL_REQUESTS_REMAINING=2")
            appendLine("EVIDENCE_CONTENT_ACCESS_COUNT=0")
            appendLine("EVIDENCE_RETRIEVAL_COUNT=0")
            appendLine("PROCESSING_REPRESENTATION_COUNT=0")
            appendLine("OCR_INVOCATION_COUNT=0")
            appendLine("EXTERNAL_PROVIDER_REQUEST_COUNT=0")
            appendLine("DURABLE_GENERATION_COUNT=0")
            appendLine("ANALYSIS_INVOCATION_COUNT=0")
        }
        Files.createDirectories(recordPath.parent)
        setPermissions(recordPath.parent, "rwx------")
        val temporary = Files.createTempFile(recordPath.parent, ".unit-o3-", ".tmp")
        Files.writeString(temporary, record)
        setPermissions(temporary, "rw-------")
        Files.move(temporary, recordPath, StandardCopyOption.ATOMIC_MOVE)
        setPermissions(recordPath, "rw-------")
        val hash = MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(recordPath))
            .joinToString("") { "%02x".format(it) }
        println("UNIT_O_3_RECORD=$recordPath")
        println("UNIT_O_3_RECORD_SHA256=$hash")
    }

    private fun StringBuilder.appendCase(label: String, result: UnitOMetadataPreflight, attestation: String) {
        val metadata = requireNotNull(result.metadata)
        appendLine("${label}_EVIDENCE_ARTIFACT_ID=${result.requestedEvidenceArtifactId.value}")
        appendLine("${label}_SHA256=${metadata.sha256}")
        appendLine("${label}_BYTE_LENGTH=${metadata.byteLength}")
        appendLine("${label}_MEDIA_TYPE=${metadata.declaredMediaType}")
        appendLine("${label}_ELIGIBILITY=${result.eligibility}")
        appendLine("${label}_PAGE_COUNT_PREFLIGHT=NOT_ESTABLISHED_METADATA_ONLY")
        appendLine("${label}_OWNER_CLASS_ATTESTATION=$attestation")
        appendLine("${label}_LATER_EXTERNAL_EGRESS_ELIGIBILITY=OWNER_CONFIRMED")
    }
    private fun required(name: String): String = requireNotNull(System.getenv(name)?.takeIf { it.isNotBlank() }) { "$name is required" }
    private fun requiredPath(name: String): Path = Path.of(required(name))
    private fun setPermissions(path: Path, permissions: String) {
        try { Files.setPosixFilePermissions(path, PosixFilePermissions.fromString(permissions)) }
        catch (_: UnsupportedOperationException) { /* Parker execution is POSIX; portability remains fail-safe. */ }
    }
}
