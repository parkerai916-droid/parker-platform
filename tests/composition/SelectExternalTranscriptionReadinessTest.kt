package parker.composition

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import parker.core.runtime.EML_PROCESSING_PROFILE_IDENTITY
import parker.core.runtime.EML_TRANSCRIPTION_PROFILE_ID
import parker.core.runtime.EML_VERIFICATION_INSTRUCTION_SHA256
import parker.core.runtime.EML_VERIFICATION_SCHEMA_SHA256

/**
 * STEP 4G ACCEPTANCE CORRECTION: [selectExternalTranscriptionReadiness] is
 * [ParkerRuntime.invokeExternalTranscriptionWithFreshBinding]'s own sole selection point,
 * exercised here directly and purely -- no running [ParkerRuntime], no credential, no transport,
 * so there is no possibility of a real (or attempted) provider call anywhere in this file.
 */
class SelectExternalTranscriptionReadinessTest {
    private val limits = OpenAiExternalTranscriptionEffectiveLimits(
        maximumPdfBytes = 1, maximumImageBytes = 1, maximumOutputBytes = 1, timeoutMillis = 1,
    )

    private fun fidelityFirstProfile(state: ExternalTranscriptionAcceptanceState) = OpenAiExternalTranscriptionProviderProfile(
        schemaVersion = "3", providerIdentity = "OpenAI", apiProductPath = "/v1/responses", store = false,
        modelSelectionRule = "gpt-5.6-sol", modelSnapshotPolicy = "RECORD_PRESENT_OR_NOT_EXPOSED",
        maximumPdfBytes = 1, maximumImageBytes = 1, maximumOutputBytes = 1, timeoutMillis = 1,
        allowedNetworkDestination = "https://api.openai.com", retentionTreatment = "x", dataUseTrainingTreatment = "x",
        zdrMamStatus = "x", projectAccountStatus = "x", projectAccountControls = "x",
        authenticationMechanism = "BEARER_API_CREDENTIAL", requestLoggingConsiderations = "x", regionalStorageConsiderations = "x",
        verifiedOn = LocalDate.parse("2026-01-01"), approvingOwnerReference = "x",
        nextReviewDate = LocalDate.parse("2027-01-01"), verificationReferences = listOf("x"), reverificationTriggers = listOf("x"),
        transcriptionProfileId = FIDELITY_FIRST_TRANSCRIPTION_PROFILE_ID,
        instructionSha256 = "a".repeat(64), structuredSchemaSha256 = "b".repeat(64),
        processingProfileIdentity = DIRECT_AUTHORITATIVE_PROCESSING_PROFILE_ID,
        acceptanceState = state, reasoningEffort = "none", pdfDetail = "high", imageDetail = "original",
    )

    private fun emlProfile(state: ExternalTranscriptionAcceptanceState) = OpenAiExternalTranscriptionProviderProfile(
        schemaVersion = "4", providerIdentity = "OpenAI", apiProductPath = "/v1/responses", store = false,
        modelSelectionRule = "gpt-5.6-sol", modelSnapshotPolicy = "RECORD_PRESENT_OR_NOT_EXPOSED",
        maximumPdfBytes = 1, maximumImageBytes = 1, maximumOutputBytes = 1, timeoutMillis = 1,
        allowedNetworkDestination = "https://api.openai.com", retentionTreatment = "x", dataUseTrainingTreatment = "x",
        zdrMamStatus = "x", projectAccountStatus = "x", projectAccountControls = "x",
        authenticationMechanism = "BEARER_API_CREDENTIAL", requestLoggingConsiderations = "x", regionalStorageConsiderations = "x",
        verifiedOn = LocalDate.parse("2026-01-01"), approvingOwnerReference = "x",
        nextReviewDate = LocalDate.parse("2027-01-01"), verificationReferences = listOf("x"), reverificationTriggers = listOf("x"),
        transcriptionProfileId = EML_TRANSCRIPTION_PROFILE_ID,
        instructionSha256 = EML_VERIFICATION_INSTRUCTION_SHA256, structuredSchemaSha256 = EML_VERIFICATION_SCHEMA_SHA256,
        processingProfileIdentity = EML_PROCESSING_PROFILE_IDENTITY,
        acceptanceState = state, reasoningEffort = "none", pdfDetail = "NOT_APPLICABLE", imageDetail = "NOT_APPLICABLE",
    )

    private fun ready(state: ExternalTranscriptionAcceptanceState, eml: Boolean) =
        OpenAiExternalTranscriptionReadiness.Ready(if (eml) emlProfile(state) else fidelityFirstProfile(state), limits)

    private val fidelityFirstAccepted = ready(ExternalTranscriptionAcceptanceState.ACCEPTED, eml = false)
    private val emlAccepted = ready(ExternalTranscriptionAcceptanceState.ACCEPTED, eml = true)

    @Test
    fun `1 -- message-rfc822 selects EML readiness and profile, not fidelity-first`() {
        val selected = selectExternalTranscriptionReadiness("message/rfc822", fidelityFirstAccepted, emlAccepted)
        assertSame(emlAccepted, selected)
        assertEquals(EML_TRANSCRIPTION_PROFILE_ID, selected?.profile?.transcriptionProfileId)
    }

    @Test
    fun `2 -- EML unavailable (Disabled, InvalidProfile, StaleProfile) yields null -- zero provider calls possible`() {
        assertNull(selectExternalTranscriptionReadiness("message/rfc822", fidelityFirstAccepted, OpenAiExternalTranscriptionReadiness.Disabled))
        assertNull(selectExternalTranscriptionReadiness("message/rfc822", fidelityFirstAccepted, OpenAiExternalTranscriptionReadiness.InvalidProfile("bad")))
        assertNull(selectExternalTranscriptionReadiness("message/rfc822", fidelityFirstAccepted, OpenAiExternalTranscriptionReadiness.StaleProfile(LocalDate.parse("2026-01-01"))))
    }

    @Test
    fun `3 -- EML ACCEPTANCE_PENDING yields null -- zero provider calls possible`() {
        val pending = ready(ExternalTranscriptionAcceptanceState.ACCEPTANCE_PENDING, eml = true)
        assertNull(selectExternalTranscriptionReadiness("message/rfc822", fidelityFirstAccepted, pending))
    }

    @Test
    fun `4 -- EML SUSPENDED yields null -- zero provider calls possible`() {
        val suspended = ready(ExternalTranscriptionAcceptanceState.SUSPENDED, eml = true)
        assertNull(selectExternalTranscriptionReadiness("message/rfc822", fidelityFirstAccepted, suspended))
    }

    @Test
    fun `5 -- fidelity-first ACCEPTED but EML unavailable does not leak acceptance to EML`() {
        val selected = selectExternalTranscriptionReadiness("message/rfc822", fidelityFirstAccepted, OpenAiExternalTranscriptionReadiness.Disabled)
        assertNull(selected, "fidelity-first's own ACCEPTED state must never substitute for EML's own")
    }

    @Test
    fun `6 -- EML ACCEPTED but fidelity-first unavailable can still select EML correctly`() {
        val selected = selectExternalTranscriptionReadiness("message/rfc822", OpenAiExternalTranscriptionReadiness.Disabled, emlAccepted)
        assertSame(emlAccepted, selected)
    }

    @Test
    fun `7 -- PDF, image, and CSV continue selecting fidelity-first readiness, never EML`() {
        listOf("application/pdf", "image/png", "image/jpeg", "text/csv").forEach { mediaType ->
            val selected = selectExternalTranscriptionReadiness(mediaType, fidelityFirstAccepted, emlAccepted)
            assertSame(fidelityFirstAccepted, selected, "media type $mediaType must select fidelity-first, not EML")
        }
        // And when fidelity-first itself is unavailable, these media types get null -- never a
        // silent fallback to the (unrelated) EML profile.
        listOf("application/pdf", "image/png", "text/csv").forEach { mediaType ->
            assertNull(selectExternalTranscriptionReadiness(mediaType, OpenAiExternalTranscriptionReadiness.Disabled, emlAccepted))
        }
    }
}
