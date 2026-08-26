package parker.composition

import java.nio.file.Files
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class OpenAiExternalTranscriptionProviderProfileTest {
    private val today = LocalDate.parse("2026-08-26")
    private val evaluator = OpenAiExternalTranscriptionProviderReadinessEvaluator { today }

    @Test
    fun `disabled wins with no profile or even a valid profile`() {
        assertIs<OpenAiExternalTranscriptionReadiness.Disabled>(evaluator.evaluate(false, null))
        assertIs<OpenAiExternalTranscriptionReadiness.Disabled>(evaluator.evaluate(false, profileFile().toString()))
    }

    @Test
    fun `enabled missing and malformed profiles fail closed`() {
        assertIs<OpenAiExternalTranscriptionReadiness.InvalidProfile>(evaluator.evaluate(true, null))
        assertIs<OpenAiExternalTranscriptionReadiness.InvalidProfile>(evaluator.evaluate(true, "missing-profile.properties"))
        assertInvalid(mapOf("providerIdentity" to null))
        assertInvalid(mapOf("maximumPdfBytes" to "not-a-number"))
        assertInvalid(mapOf("verifiedOn" to "not-a-date"))
        assertInvalid(mapOf("verifiedOn" to today.plusDays(1).toString(), "nextReviewDate" to today.plusDays(2).toString()))
        assertInvalid(mapOf("apiKey" to "must-never-be-accepted"))
    }

    @Test
    fun `provider pathway storage model and limits are validated`() {
        listOf(
            mapOf("providerIdentity" to "Other"),
            mapOf("apiProductPath" to "/v1/chat/completions"),
            mapOf("store" to "true"),
            mapOf("modelSelectionRule" to "unknown"),
            mapOf("modelSnapshotPolicy" to "unknown"),
            mapOf("maximumPdfBytes" to "0"),
            mapOf("maximumImageBytes" to "-1"),
            mapOf("maximumOutputBytes" to "0"),
            mapOf("timeoutMillis" to "0"),
        ).forEach(::assertInvalid)
    }

    @Test
    fun `only bounded credential-free OpenAI HTTPS destination is accepted`() {
        listOf(
            "http://api.openai.com",
            "https://*.openai.com",
            "https://user:password@api.openai.com",
            "https://example.com",
            "https://api.openai.com/other",
        ).forEach { assertInvalid(mapOf("allowedNetworkDestination" to it)) }
        assertIs<OpenAiExternalTranscriptionReadiness.Ready>(evaluator.evaluate(true, profileFile().toString()))
    }

    @Test
    fun `retention training account and ZDR MAM review fields are mandatory but explicit unavailability is valid`() {
        listOf("retentionTreatment", "dataUseTrainingTreatment", "zdrMamStatus", "projectAccountStatus", "projectAccountControls")
            .forEach { assertInvalid(mapOf(it to null)) }
        assertIs<OpenAiExternalTranscriptionReadiness.Ready>(
            evaluator.evaluate(true, profileFile(mapOf("zdrMamStatus" to "NOT_AVAILABLE_OR_ENABLED")).toString()),
        )
    }

    @Test
    fun `review date boundary is deterministic and passed date is stale`() {
        assertIs<OpenAiExternalTranscriptionReadiness.Ready>(
            evaluator.evaluate(true, profileFile(mapOf("nextReviewDate" to today.toString())).toString()),
        )
        assertIs<OpenAiExternalTranscriptionReadiness.StaleProfile>(
            evaluator.evaluate(true, profileFile(mapOf("nextReviewDate" to today.minusDays(1).toString())).toString()),
        )
        assertInvalid(mapOf("verifiedOn" to today.toString(), "nextReviewDate" to today.minusDays(1).toString()))
    }

    @Test
    fun `effective limits use lower of reviewed profile and Parker ceilings`() {
        val lower = assertIs<OpenAiExternalTranscriptionReadiness.Ready>(
            evaluator.evaluate(true, profileFile(mapOf("maximumPdfBytes" to "1024", "maximumOutputBytes" to "2048")).toString()),
        )
        assertEquals(1024, lower.effectiveLimits.maximumPdfBytes)
        assertEquals(2048, lower.effectiveLimits.maximumOutputBytes)

        val higher = assertIs<OpenAiExternalTranscriptionReadiness.Ready>(
            evaluator.evaluate(true, profileFile(mapOf("maximumPdfBytes" to "999999999", "maximumOutputBytes" to "999999999")).toString()),
        )
        assertEquals(64L * 1024 * 1024, higher.effectiveLimits.maximumPdfBytes)
        assertEquals(20L * 1024 * 1024, higher.effectiveLimits.maximumOutputBytes)
    }

    @Test
    fun `profile and evaluator contain no credential or network dependencies and Ready needs no API key`() {
        val fieldNames = OpenAiExternalTranscriptionProviderProfile::class.java.declaredFields.map { it.name.lowercase() }
        assertTrue(fieldNames.none { it.contains("key") || it.contains("token") || it.contains("secret") || it.contains("credentialreference") })
        val dependencyNames = OpenAiExternalTranscriptionProviderReadinessEvaluator::class.java.declaredFields.map { it.type.name }
        dependencyNames.forEach { name ->
            listOf("Http", "OpenAIAdapter", "EvidenceCustodian", "PermissionEngine", "Memory", "Knowledge", "Analysis", "OwnerUi")
                .forEach { assertTrue(!name.contains(it), "$name contains forbidden $it") }
        }
        assertIs<OpenAiExternalTranscriptionReadiness.Ready>(evaluator.evaluate(true, profileFile().toString()))
    }

    private fun assertInvalid(overrides: Map<String, String?>) {
        assertIs<OpenAiExternalTranscriptionReadiness.InvalidProfile>(evaluator.evaluate(true, profileFile(overrides).toString()))
    }

    private fun profileFile(overrides: Map<String, String?> = emptyMap()): java.nio.file.Path {
        val values = linkedMapOf(
            "schemaVersion" to "1",
            "providerIdentity" to "OpenAI",
            "apiProductPath" to "/v1/responses",
            "store" to "false",
            "modelSelectionRule" to "synthetic-reviewed-model-rule",
            "modelSnapshotPolicy" to "RECORD_PRESENT_OR_NOT_EXPOSED",
            "maximumPdfBytes" to "67108864",
            "maximumImageBytes" to "16777216",
            "maximumOutputBytes" to "20971520",
            "timeoutMillis" to "120000",
            "allowedNetworkDestination" to "https://api.openai.com",
            "retentionTreatment" to "SYNTHETIC_REVIEWED_POSITION",
            "dataUseTrainingTreatment" to "SYNTHETIC_REVIEWED_POSITION",
            "zdrMamStatus" to "NOT_AVAILABLE_OR_ENABLED",
            "projectAccountStatus" to "SYNTHETIC_REVIEWED_STATUS",
            "projectAccountControls" to "SYNTHETIC_REVIEWED_CONTROLS",
            "authenticationMechanism" to "BEARER_API_CREDENTIAL",
            "requestLoggingConsiderations" to "SYNTHETIC_REVIEWED_LOGGING_POSITION",
            "regionalStorageConsiderations" to "SYNTHETIC_REVIEWED_REGIONAL_POSITION",
            "verifiedOn" to "2026-08-01",
            "approvingOwnerReference" to "synthetic-owner-review-reference",
            "nextReviewDate" to "2026-09-01",
            "verificationReferences" to "synthetic-provider-material-review|synthetic-account-review",
            "reverificationTriggers" to "provider terms change|retention changes|API path changes|model rule changes|account controls change|ZDR MAM changes",
        )
        overrides.forEach { (key, value) -> if (value == null) values.remove(key) else values[key] = value }
        return Files.createTempFile("openai-transcription-profile", ".properties").also { path ->
            Files.writeString(path, values.entries.joinToString("\n") { "${it.key}=${it.value}" })
        }
    }
}
