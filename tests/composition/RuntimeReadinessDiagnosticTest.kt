package parker.composition

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RuntimeReadinessDiagnosticTest {
    @Test
    fun `accepted complete configuration is ready`() {
        val config = config(profileFile(ExternalTranscriptionAcceptanceState.ACCEPTED), credential = "secret-value")
        val result = RuntimeReadinessDiagnostic.evaluate(config, COMMIT)
        assertTrue(result.overallReady)
        assertTrue(result.ordinaryExecutionReady)
        assertTrue(result.providerProfileAccepted)
        assertTrue(result.buildIdentityMatches)
    }

    @Test
    fun `pending profile is visible and blocks readiness`() {
        val result = RuntimeReadinessDiagnostic.evaluate(config(profileFile(ExternalTranscriptionAcceptanceState.ACCEPTANCE_PENDING), credential = "secret-value"), COMMIT)
        assertFalse(result.providerProfileAccepted)
        assertFalse(result.ordinaryExecutionReady)
        assertTrue(result.overallReady)
        assertTrue(result.reasons.containsKey("providerProfileAccepted"))
    }

    @Test
    fun `missing credential blocks readiness`() {
        val result = RuntimeReadinessDiagnostic.evaluate(config(profileFile(ExternalTranscriptionAcceptanceState.ACCEPTED), credential = null), COMMIT)
        assertFalse(result.credentialPresent)
        assertFalse(result.overallReady)
    }

    @Test
    fun `build mismatch blocks readiness`() {
        val result = RuntimeReadinessDiagnostic.evaluate(config(profileFile(ExternalTranscriptionAcceptanceState.ACCEPTED), credential = "secret-value"), "b".repeat(40))
        assertFalse(result.buildIdentityMatches)
        assertFalse(result.overallReady)
    }

    @Test
    fun `invalid profile blocks readiness`() {
        val path = Files.createTempFile("parker-profile-invalid", ".properties")
        Files.writeString(path, profile(ExternalTranscriptionAcceptanceState.ACCEPTED).replace("imageDetail=original", "imageDetail=bad"))
        val result = RuntimeReadinessDiagnostic.evaluate(config(path.toString(), "secret-value"), COMMIT)
        assertFalse(result.providerProfileStructurallyValid)
        assertFalse(result.overallReady)
    }

    @Test
    fun `diagnostic representation never contains credential`() {
        val secret = "super-secret-bearer-value"
        val result = RuntimeReadinessDiagnostic.evaluate(config(profileFile(ExternalTranscriptionAcceptanceState.ACCEPTED), secret), COMMIT)
        assertFalse(result.toString().contains(secret))
    }

    private fun config(profilePath: String, credential: String?) = ParkerRuntimeConfig(
        modelEndpointUrl = "http://localhost:11434/api/generate", modelName = "test", ownerPrincipalId = "user.steven",
        evidenceStorageRootPath = "/tmp/evidence", evidenceDeletionAuditLogPath = "/tmp/evidence-delete.log",
        evidenceSourceManifestStorageRootPath = "/tmp/manifests", derivativeGenerationStorageRootPath = "/tmp/generations",
        derivativeContentStorageRootPath = "/tmp/content", savedAnalysisStorageRootPath = "/tmp/analyses",
        documentIngestionAuditLogPath = "/tmp/ingestion.log", memoryCoreDurabilityLogPath = "/tmp/memory.log",
        knowledgeItemDurabilityLogPath = "/tmp/knowledge.log", openAiExternalTranscriptionEnabled = true,
        openAiExternalTranscriptionProviderProfilePath = profilePath,
        openAiApiCredential = credential?.let { OpenAiApiCredential.fromEnvironment(it) },
        fidelityFirstAcceptanceAuthorityStorageRootPath = "/tmp/authorities",
        fidelityFirstAttemptStorageRootPath = "/tmp/attempts", regionProviderStateStorageRootPath = "/tmp/provider-state",
        productionCommit = COMMIT,
    )

    private fun profileFile(state: ExternalTranscriptionAcceptanceState): String {
        val path = Files.createTempFile("parker-profile", ".properties")
        Files.writeString(path, profile(state))
        return path.toString()
    }

    private fun profile(state: ExternalTranscriptionAcceptanceState): String = """
        schemaVersion=3
        providerIdentity=OpenAI
        apiProductPath=/v1/responses
        store=false
        modelSelectionRule=gpt-5.6-sol
        modelSnapshotPolicy=RECORD_PRESENT_OR_NOT_EXPOSED
        maximumPdfBytes=1000000
        maximumImageBytes=1000000
        maximumOutputBytes=1000000
        timeoutMillis=30000
        allowedNetworkDestination=https://api.openai.com
        retentionTreatment=retention
        dataUseTrainingTreatment=disabled
        zdrMamStatus=not-established
        projectAccountStatus=active
        projectAccountControls=controlled
        authenticationMechanism=BEARER_API_CREDENTIAL
        requestLoggingConsiderations=none
        regionalStorageConsiderations=global
        verifiedOn=2026-08-26
        approvingOwnerReference=owner
        nextReviewDate=2026-09-26
        verificationReferences=reference
        reverificationTriggers=change
        transcriptionProfileId=openai-fidelity-first-transcription-v1
        instructionSha256=${"a".repeat(64)}
        structuredSchemaSha256=${"b".repeat(64)}
        processingProfileIdentity=external-transcription.direct-authoritative-byte-v1
        acceptanceState=${state.name}
        reasoningEffort=none
        pdfDetail=high
        imageDetail=original
    """.trimIndent()

    companion object { private val COMMIT = "a".repeat(40) }
}
