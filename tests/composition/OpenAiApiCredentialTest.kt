package parker.composition

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.OcrProcessingProvenance
import parker.core.interfaces.OcrProviderProvenance
import parker.core.interfaces.OcrRecognitionRequest
import parker.core.interfaces.OcrRecognitionResult
import parker.core.runtime.ExternalTranscriptionInvocationGate
import parker.core.runtime.OcrStructuredResultValidator
import parker.core.interfaces.PrincipalId

class OpenAiApiCredentialTest {
    private val sentinel = "unit-g-fake-secret-sentinel"

    @Test
    fun `credential is non-printing identity-valued and raw access is narrowly scoped`() {
        val first = OpenAiApiCredential.fromEnvironment(sentinel)!!
        val second = OpenAiApiCredential.fromEnvironment(sentinel)!!

        assertTrue(!first.toString().contains(sentinel))
        assertNotEquals(first, second)
        assertEquals(sentinel, first.useValue { it })
    }

    @Test
    fun `credential rejects header-unsafe deployment values without normalization or disclosure`() {
        val malformed = listOf(
            "unit-g${0x1b.toChar()}sentinel",
            "unit-g\nsentinel",
            " unit-g-sentinel",
            "unit-g-sentinel ",
            "unit-g-\u200b-sentinel",
            "Bearer unit-g-sentinel",
        )

        malformed.forEach { value ->
            assertEquals(null, OpenAiApiCredential.fromEnvironment(value))
            assertTrue(!OpenAiApiCredential.isStructurallyValidBearerCredential(value))
        }
        assertTrue(OpenAiApiCredential.isStructurallyValidBearerCredential(sentinel))
    }

    @Test
    fun `backend readiness keeps profile and credential readiness distinct`() {
        val profileReady = OpenAiExternalTranscriptionReadiness.Ready(
            profile().copy(acceptanceState = ExternalTranscriptionAcceptanceState.ACCEPTED),
            effectiveLimits(),
        )

        assertIs<OpenAiExternalTranscriptionBackendReadiness.MissingCredential>(
            externalTranscriptionBackendReadiness(profileReady, null),
        )
        assertIs<OpenAiExternalTranscriptionBackendReadiness.Ready>(
            externalTranscriptionBackendReadiness(profileReady, OpenAiApiCredential.fromEnvironment(sentinel)),
        )
        assertIs<OpenAiExternalTranscriptionBackendReadiness.Disabled>(
            externalTranscriptionBackendReadiness(OpenAiExternalTranscriptionReadiness.Disabled, null),
        )
        listOf(
            ExternalTranscriptionAcceptanceState.CONFIGURATION_READY,
            ExternalTranscriptionAcceptanceState.ACCEPTANCE_PENDING,
            ExternalTranscriptionAcceptanceState.SUSPENDED,
            ExternalTranscriptionAcceptanceState.DISABLED,
        ).forEach { state ->
            val outcome = externalTranscriptionBackendReadiness(
                OpenAiExternalTranscriptionReadiness.Ready(profile().copy(acceptanceState = state), effectiveLimits()),
                OpenAiApiCredential.fromEnvironment(sentinel),
            )
            assertEquals(state, assertIs<OpenAiExternalTranscriptionBackendReadiness.ConfigurationNotAccepted>(outcome).state)
        }
    }

    @Test
    fun `restart omission does not retain an earlier credential`() {
        val firstStart = OpenAiApiCredential.fromEnvironment(sentinel)
        val nextStart = OpenAiApiCredential.fromEnvironment(null)

        assertTrue(firstStart != null)
        assertEquals(null, nextStart)
        assertIs<OpenAiExternalTranscriptionBackendReadiness.MissingCredential>(
            externalTranscriptionBackendReadiness(
                OpenAiExternalTranscriptionReadiness.Ready(
                    profile().copy(acceptanceState = ExternalTranscriptionAcceptanceState.ACCEPTED), effectiveLimits(),
                ),
                nextStart,
            ),
        )
    }

    @Test
    fun `credential type is absent from governed OCR evidence authorization and validator structures`() {
        val forbiddenType = OpenAiApiCredential::class.java.name
        val governed = listOf(
            OcrRecognitionRequest::class.java,
            OcrRecognitionResult::class.java,
            OcrProviderProvenance::class.java,
            OcrProcessingProvenance::class.java,
            EvidenceArtifactId::class.java,
            OcrStructuredResultValidator::class.java,
        )
        governed.forEach { type ->
            assertTrue(type.declaredFields.none { it.type.name == forbiddenType }, "${type.name} references credential")
        }
        val request = ExternalTranscriptionInvocationGate.buildExecutionRequest(
            PrincipalId("owner.unit-g"), EvidenceArtifactId("evidence-unit-g"),
        )
        assertTrue(request.metadata.values.none { it.contains(sentinel) })
        assertTrue(!profile().toString().contains(sentinel))
    }

    private fun profile() = OpenAiExternalTranscriptionProviderProfile(
        "1", "OpenAI", "/v1/responses", false, "synthetic-model-rule", "RECORD_PRESENT_OR_NOT_EXPOSED",
        1, 1, 1, 1, "https://api.openai.com", "reviewed retention", "reviewed training", "not available",
        "reviewed account", "reviewed controls", "BEARER_API_CREDENTIAL", "reviewed logging", "reviewed region",
        LocalDate.parse("2026-08-01"), "owner-review", LocalDate.parse("2026-09-01"), listOf("reference"), listOf("terms change"),
    )

    private fun effectiveLimits() = OpenAiExternalTranscriptionEffectiveLimits(1, 1, 1, 1)
}
