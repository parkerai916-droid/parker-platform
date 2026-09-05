package parker.core.runtime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * UI-INGESTION-6: the fresh, per-invocation binding construction that lets the ordinary owner UI
 * "Run enhanced transcription" path supply the [parker.core.interfaces.ExternalTranscriptionExecutionBinding]
 * the fidelity-first adapter requires -- reusing [FidelityFirstExecutionIdentity] exactly as the
 * existing acceptance-bootstrap path already does, without a pre-issued authority.
 */
class OrdinaryFidelityFirstExecutionIdentityTest {
    private fun identity(
        requestId: String = "req-1", attemptId: String = "att-1", executionId: String = "exec-1",
        profileId: String = "openai-fidelity-first-transcription-v1",
    ) = OrdinaryFidelityFirstExecutionIdentity.create(
        evidenceArtifactId = "evidence-abc", sourceSha256 = "a".repeat(64), sourceByteLength = 10L,
        sourceMediaType = "application/pdf", repositoryCommit = "b".repeat(40),
        modelSelectionRule = "gpt-5.6-sol", transcriptionProfileId = profileId,
        instructionSha256 = "c".repeat(64), structuredSchemaSha256 = "d".repeat(64),
        processingProfileIdentity = "external-transcription.direct-authoritative-byte-v1",
        requestId = requestId, attemptId = attemptId, executionId = executionId,
    )

    @Test fun `produces a non-null binding whose profileId exactly matches the accepted profile`() {
        val binding = identity(profileId = "openai-fidelity-first-transcription-v1").toExecutionBinding()
        assertEquals("openai-fidelity-first-transcription-v1", binding.profileId)
    }

    @Test fun `requestId and attemptId are governed opaque identifiers carried through to the binding unchanged`() {
        val binding = identity(requestId = "req-governed-1", attemptId = "att-governed-1").toExecutionBinding()
        assertEquals("req-governed-1", binding.requestId)
        assertEquals("att-governed-1", binding.attemptId)
        // ExternalTranscriptionExecutionBinding's own init already enforces the bounded opaque
        // shape (^[A-Za-z0-9_-]{1,120}$) -- reaching this line without throwing proves it.
    }

    @Test fun `default identities are fresh UUIDs -- server-generated, never a static or shared value`() {
        val a = OrdinaryFidelityFirstExecutionIdentity.create(
            evidenceArtifactId = "evidence-abc", sourceSha256 = "a".repeat(64), sourceByteLength = 10L,
            sourceMediaType = "application/pdf", repositoryCommit = "b".repeat(40),
            modelSelectionRule = "gpt-5.6-sol", transcriptionProfileId = "openai-fidelity-first-transcription-v1",
            instructionSha256 = "c".repeat(64), structuredSchemaSha256 = "d".repeat(64),
            processingProfileIdentity = "external-transcription.direct-authoritative-byte-v1",
        )
        val b = OrdinaryFidelityFirstExecutionIdentity.create(
            evidenceArtifactId = "evidence-abc", sourceSha256 = "a".repeat(64), sourceByteLength = 10L,
            sourceMediaType = "application/pdf", repositoryCommit = "b".repeat(40),
            modelSelectionRule = "gpt-5.6-sol", transcriptionProfileId = "openai-fidelity-first-transcription-v1",
            instructionSha256 = "c".repeat(64), structuredSchemaSha256 = "d".repeat(64),
            processingProfileIdentity = "external-transcription.direct-authoritative-byte-v1",
        )
        assertNotEquals(a.requestId, b.requestId)
        assertNotEquals(a.attemptId, b.attemptId)
        assertNotEquals(a.executionId, b.executionId)
        val uuid = Regex("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")
        assertTrue(uuid.matches(a.requestId)); assertTrue(uuid.matches(a.attemptId))
    }

    @Test fun `a wrong or mismatched profileId is not silently corrected -- it is carried through exactly as supplied`() {
        // OrdinaryFidelityFirstExecutionIdentity performs no reconciliation of its own; the adapter's
        // own require(binding.profileId == profile.transcriptionProfileId) is what fails this closed
        // -- proven directly in FidelityFirstExternalTranscriptionTest against the real adapter.
        val binding = identity(profileId = "some-other-profile-id").toExecutionBinding()
        assertEquals("some-other-profile-id", binding.profileId)
    }
}
