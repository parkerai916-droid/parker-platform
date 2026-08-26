package parker.core.runtime

import java.nio.file.Files
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertNull
import parker.composition.OpenAiExternalTranscriptionProviderReadinessEvaluator
import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.EvidenceSourceManifest

class OpenAiLiveAcceptanceBridgeTest {
    private val evaluator = OpenAiExternalTranscriptionProviderReadinessEvaluator { LocalDate.parse("2026-08-26") }

    @Test
    fun `preflight reports every missing prerequisite at once before egress`() {
        val repository = Files.createTempDirectory("unit-n-repository")

        val problems = OpenAiLiveAcceptanceBridge.preflightProblems(emptyMap(), false, repository, evaluator)

        assertEquals(
            listOf(
                "LIVE_OPT_IN_ABSENT",
                "PROFILE_PATH_ABSENT_OR_INVALID",
                "CREDENTIAL_ABSENT_OR_INVALID",
                "RESULT_PATH_ABSENT_OR_INVALID",
            ),
            problems,
        )
    }

    @Test
    fun `complete valid host preflight is network silent and ready`() {
        val repository = Files.createTempDirectory("unit-n-repository")
        val profile = profileFile()
        val result = Files.createTempFile("unit-n-result", ".txt")
        val environment = mapOf(
            "PARKER_OPENAI_EXTERNAL_TRANSCRIPTION_PROVIDER_PROFILE_PATH" to profile.toString(),
            "PARKER_OPENAI_API_KEY" to "synthetic-valid-credential",
            "PARKER_EXTERNAL_TRANSCRIPTION_LIVE_RESULT_PATH" to result.toString(),
        )

        assertTrue(OpenAiLiveAcceptanceBridge.preflightProblems(environment, true, repository, evaluator).isEmpty())
    }

    @Test
    fun `unsafe credential and repository result path fail closed together`() {
        val repository = Files.createTempDirectory("unit-n-repository")
        val result = Files.createFile(repository.resolve("result.txt"))
        val environment = mapOf(
            "PARKER_OPENAI_EXTERNAL_TRANSCRIPTION_PROVIDER_PROFILE_PATH" to profileFile().toString(),
            "PARKER_OPENAI_API_KEY" to "synthetic${0x1b.toChar()}unsafe",
            "PARKER_EXTERNAL_TRANSCRIPTION_LIVE_RESULT_PATH" to result.toString(),
        )

        val problems = OpenAiLiveAcceptanceBridge.preflightProblems(environment, true, repository, evaluator)
        assertTrue("CREDENTIAL_ABSENT_OR_INVALID" in problems)
        assertTrue("RESULT_PATH_INSIDE_REPOSITORY" in problems)
    }

    private fun profileFile() = Files.createTempFile("unit-n-profile", ".properties").also { path ->
        Files.writeString(path, """schemaVersion=1
providerIdentity=OpenAI
apiProductPath=/v1/responses
store=false
modelSelectionRule=gpt-4.1-mini
modelSnapshotPolicy=RECORD_PRESENT_OR_NOT_EXPOSED
maximumPdfBytes=67108864
maximumImageBytes=16777216
maximumOutputBytes=20971520
timeoutMillis=120000
allowedNetworkDestination=https://api.openai.com
retentionTreatment=reviewed
dataUseTrainingTreatment=reviewed
zdrMamStatus=NOT_AVAILABLE_OR_ENABLED
projectAccountStatus=reviewed
projectAccountControls=reviewed
authenticationMechanism=BEARER_API_CREDENTIAL
requestLoggingConsiderations=reviewed
regionalStorageConsiderations=reviewed
verifiedOn=2026-08-01
approvingOwnerReference=owner-review
nextReviewDate=2026-09-01
verificationReferences=provider-review
reverificationTriggers=provider terms change
""")
    }

    @Test
    fun `Unit O read-only adapter retrieves exact ID and exposes no mutation or enumeration surface`() {
        val root = Files.createTempDirectory("unit-o-manifests")
        val clean = EvidenceArtifactId("evidence-clean")
        val difficult = EvidenceArtifactId("evidence-difficult")
        listOf(clean, difficult).forEach { id ->
            val manifest = EvidenceSourceManifest(id, "a".repeat(64), 123, "application/pdf")
            Files.write(root.resolve("${id.value}.manifest"), EvidenceSourceManifestCodec.encode(manifest))
        }

        assertEquals(clean, UnitOReadOnlyManifestAcceptanceBridge.read(root, clean)?.evidenceArtifactId)
        assertEquals(difficult, UnitOReadOnlyManifestAcceptanceBridge.read(root, difficult)?.evidenceArtifactId)
        assertNull(UnitOReadOnlyManifestAcceptanceBridge.read(root, EvidenceArtifactId("arbitrary-third")))
        val methods = UnitOReadOnlyManifestAcceptanceBridge::class.java.methods.map { it.name }
        assertTrue(methods.none { it.contains("write", true) || it.contains("delete", true) || it.contains("list", true) })
    }
}
