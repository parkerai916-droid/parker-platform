package parker.core.runtime

import java.nio.file.Files
import java.time.LocalDate
import kotlin.test.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import parker.composition.*
import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.ExternalTranscriptionOwnerInvocationOutcome

class UnitOLiteralV2CleanAcceptanceBoundaryTest {
    private val id = EvidenceArtifactId(UnitOLiteralV2CleanLock.EVIDENCE_ID)

    @Test fun `exact locked authorization metadata tuple and pending state permit one governed invocation`() = runTest {
        var custodyReads = 0
        var invocations = 0
        val boundary = UnitOLiteralV2CleanAcceptanceBoundary(UnitOManifestMetadataReader { custodyReads++; metadata() }) {
            invocations++; ExternalTranscriptionOwnerInvocationOutcome.NotAuthorised
        }
        val input = input()
        assertTrue(boundary.preflight(input).ready)
        assertEquals(0, boundary.requestCount)
        assertIs<ExternalTranscriptionOwnerInvocationOutcome.NotAuthorised>(boundary.execute(input))
        assertEquals(1, boundary.requestCount)
        assertEquals(1, invocations)
        val second = boundary.preflight(input)
        assertEquals(listOf("ALLOCATION_ALREADY_ATTEMPTED"), second.problems)
        assertFailsWith<IllegalArgumentException> { boundary.execute(input) }
        assertEquals(1, invocations)
        assertTrue(custodyReads >= 1)
    }

    @Test fun `wrong evidence fails before custody and invocation`() = runTest {
        var custodyReads = 0; var invocations = 0
        val boundary = UnitOLiteralV2CleanAcceptanceBoundary(UnitOManifestMetadataReader { custodyReads++; metadata() }) {
            invocations++; ExternalTranscriptionOwnerInvocationOutcome.NotAuthorised
        }
        val wrong = input().copy(requestedEvidenceArtifactId = EvidenceArtifactId("evidence-wrong"))
        assertTrue("EVIDENCE_ID_MISMATCH" in boundary.preflight(wrong).problems)
        assertEquals(0, custodyReads); assertEquals(0, invocations); assertEquals(0, boundary.requestCount)
    }

    @Test fun `aggregate preflight reports every static metadata lifecycle and result defect with zero invocation`() = runTest {
        var invocations = 0
        val badMetadata = metadata().copy(sha256 = "0".repeat(64), byteLength = 1, declaredMediaType = "text/plain")
        val boundary = UnitOLiteralV2CleanAcceptanceBoundary(UnitOManifestMetadataReader { badMetadata }) {
            invocations++; ExternalTranscriptionOwnerInvocationOutcome.NotAuthorised
        }
        val path = Files.createTempFile("unit-o4r-result", ".txt"); Files.delete(path)
        val bad = input().copy(optIn = false, authorization = null, credentialReady = false, resultPath = path,
            profileReadiness = ready(ExternalTranscriptionAcceptanceState.CONFIGURATION_READY))
        val problems = boundary.preflight(bad).problems
        listOf("OPT_IN_MISSING", "AUTHORIZATION_RECORD_MISSING", "STATE_NOT_ACCEPTANCE_PENDING", "CREDENTIAL_NOT_READY",
            "RESULT_PATH_NOT_READY", "SOURCE_SHA256_MISMATCH", "SOURCE_LENGTH_MISMATCH", "SOURCE_MEDIA_TYPE_MISMATCH")
            .forEach { assertTrue(it in problems, "missing $it from $problems") }
        assertEquals(0, invocations); assertEquals(0, boundary.requestCount)
    }

    @Test fun `all non-pending lifecycle states fail and execution never promotes profile`() = runTest {
        ExternalTranscriptionAcceptanceState.entries.filterNot { it == ExternalTranscriptionAcceptanceState.ACCEPTANCE_PENDING }.forEach { state ->
            val profile = ready(state)
            val boundary = UnitOLiteralV2CleanAcceptanceBoundary(UnitOManifestMetadataReader { metadata() }) {
                ExternalTranscriptionOwnerInvocationOutcome.NotAuthorised
            }
            assertTrue("STATE_NOT_ACCEPTANCE_PENDING" in boundary.preflight(input().copy(profileReadiness = profile)).problems)
            assertEquals(state, profile.profile.acceptanceState)
        }
    }

    @Test fun `allocation tuple and commit substitutions fail closed`() = runTest {
        val variants = listOf(
            authorization().copy(allocation = "HANDWRITTEN_MIXED_O5") to "ALLOCATION_MISMATCH",
            authorization().copy(requestOrdinal = 3) to "REQUEST_ORDINAL_MISMATCH",
            authorization().copy(profileId = "wrong") to "AUTHORIZATION_TUPLE_MISMATCH",
            authorization().copy(instructionSha256 = "0".repeat(64)) to "AUTHORIZATION_TUPLE_MISMATCH",
            authorization().copy(schemaSha256 = "0".repeat(64)) to "AUTHORIZATION_TUPLE_MISMATCH",
            authorization().copy(processingProfile = "wrong") to "AUTHORIZATION_TUPLE_MISMATCH",
            authorization().copy(modelRule = "wrong") to "AUTHORIZATION_TUPLE_MISMATCH",
            authorization().copy(endpoint = "/wrong") to "AUTHORIZATION_TUPLE_MISMATCH",
            authorization().copy(store = true) to "AUTHORIZATION_TUPLE_MISMATCH",
            authorization().copy(repositoryCommit = "wrong") to "COMMIT_BINDING_MISMATCH",
        )
        variants.forEach { (auth, expected) ->
            val boundary = UnitOLiteralV2CleanAcceptanceBoundary(UnitOManifestMetadataReader { metadata() }) { error("no invocation") }
            assertTrue(expected in boundary.preflight(input().copy(authorization = auth)).problems)
            assertEquals(0, boundary.requestCount)
        }
    }

    @Test fun `authorization source substitutions fail aggregate preflight before invocation`() = runTest {
        val variants = listOf(
            authorization().copy(sourceSha256 = "0".repeat(64)) to "AUTHORIZATION_SOURCE_SHA256_MISMATCH",
            authorization().copy(sourceSha256 = "not-a-digest") to "AUTHORIZATION_SOURCE_SHA256_MISMATCH",
            authorization().copy(sourceSha256 = UnitOLiteralV2CleanLock.SOURCE_SHA256.uppercase()) to "AUTHORIZATION_SOURCE_SHA256_MISMATCH",
            authorization().copy(sourceSha256 = " ${UnitOLiteralV2CleanLock.SOURCE_SHA256}") to "AUTHORIZATION_SOURCE_SHA256_MISMATCH",
            authorization().copy(sourceByteLength = 0) to "AUTHORIZATION_SOURCE_LENGTH_MISMATCH",
            authorization().copy(sourceByteLength = -1) to "AUTHORIZATION_SOURCE_LENGTH_MISMATCH",
            authorization().copy(sourceByteLength = 810038) to "AUTHORIZATION_SOURCE_LENGTH_MISMATCH",
            authorization().copy(sourceMediaType = "text/plain") to "AUTHORIZATION_SOURCE_MEDIA_TYPE_MISMATCH",
            authorization().copy(sourceMediaType = "") to "AUTHORIZATION_SOURCE_MEDIA_TYPE_MISMATCH",
            authorization().copy(sourceMediaType = " application/pdf") to "AUTHORIZATION_SOURCE_MEDIA_TYPE_MISMATCH",
        )
        variants.forEach { (authorization, expected) ->
            var invocations = 0
            val boundary = UnitOLiteralV2CleanAcceptanceBoundary(UnitOManifestMetadataReader { metadata() }) {
                invocations++; ExternalTranscriptionOwnerInvocationOutcome.NotAuthorised
            }
            assertTrue(expected in boundary.preflight(input().copy(authorization = authorization)).problems)
            assertEquals(0, boundary.requestCount); assertEquals(0, invocations)
        }
    }

    @Test fun `strict parser accepts complete record and rejects missing unknown duplicate and malformed source fields`() {
        assertEquals(authorization(), parse(authorization().render()))
        listOf("sourceSha256", "sourceByteLength", "sourceMediaType").forEach { missing ->
            assertFails { parse(authorization().render().lineSequence().filterNot { it.startsWith("$missing=") }.joinToString("\n", postfix = "\n")) }
        }
        assertFails { parse(authorization().render() + "unknownField=value\n") }
        assertFails { parse(authorization().render() + "sourceSha256=${UnitOLiteralV2CleanLock.SOURCE_SHA256}\n") }
        assertFails { parse(authorization().render().replace("sourceByteLength=${UnitOLiteralV2CleanLock.SOURCE_BYTES}", "sourceByteLength=not-a-number")) }
        assertFails { parse(authorization().render().replace("sourceByteLength=${UnitOLiteralV2CleanLock.SOURCE_BYTES}", "sourceByteLength=9223372036854775808")) }
    }

    @Test fun `exact authorization remains independently bound to authoritative manifest source tuple`() = runTest {
        val variants = listOf(
            metadata().copy(sha256 = "0".repeat(64)) to "SOURCE_SHA256_MISMATCH",
            metadata().copy(byteLength = 810038) to "SOURCE_LENGTH_MISMATCH",
            metadata().copy(declaredMediaType = "text/plain") to "SOURCE_MEDIA_TYPE_MISMATCH",
        )
        variants.forEach { (manifest, expected) ->
            var invocations = 0
            val boundary = UnitOLiteralV2CleanAcceptanceBoundary(UnitOManifestMetadataReader { manifest }) {
                invocations++; ExternalTranscriptionOwnerInvocationOutcome.NotAuthorised
            }
            assertTrue(expected in boundary.preflight(input()).problems)
            assertEquals(0, boundary.requestCount); assertEquals(0, invocations)
        }
    }

    @Test fun `bounded authorization rendering cannot contain credential source transcript or provider error sentinels`() {
        val rendered = authorization().render()
        listOf("SECRET_SENTINEL", "SOURCE_SENTINEL", "TRANSCRIPT_SENTINEL", "PROVIDER_ERROR_SENTINEL", "Authorization", "Bearer")
            .forEach { assertFalse(rendered.contains(it)) }
        assertTrue(rendered.contains(UnitOLiteralV2CleanLock.ALLOCATION))
        assertTrue(rendered.contains("sourceSha256=${UnitOLiteralV2CleanLock.SOURCE_SHA256}"))
        assertTrue(rendered.contains("sourceByteLength=${UnitOLiteralV2CleanLock.SOURCE_BYTES}"))
        assertTrue(rendered.contains("sourceMediaType=${UnitOLiteralV2CleanLock.MEDIA_TYPE}"))
    }

    @Test fun `provider failure consumes exactly one attempt with no retry fallback or model switch`() = runTest {
        var invocations = 0
        val boundary = UnitOLiteralV2CleanAcceptanceBoundary(UnitOManifestMetadataReader { metadata() }) {
            invocations++
            ExternalTranscriptionOwnerInvocationOutcome.MechanismFailure("PROVIDER_UNAVAILABLE")
        }
        assertIs<ExternalTranscriptionOwnerInvocationOutcome.MechanismFailure>(boundary.execute(input()))
        assertEquals(1, invocations); assertEquals(1, boundary.requestCount)
        assertFailsWith<IllegalArgumentException> { boundary.execute(input()) }
        assertEquals(1, invocations)
    }

    @Test fun `bounded terminal result contains provenance counters but never sensitive payloads`() {
        val rendered = UnitOLiteralV2AcceptanceResult(
            "FAILED", COMMIT, null, null, null, null, null, null, null, requestCount = 1,
        ).render()
        listOf("SECRET_SENTINEL", "SOURCE_SENTINEL", "TRANSCRIPT_SENTINEL", "PROVIDER_ERROR_SENTINEL", "Bearer", "file_data")
            .forEach { assertFalse(rendered.contains(it)) }
        assertTrue(rendered.contains("requestCount=1"))
        assertTrue(rendered.contains("retryCount=0"))
        assertTrue(rendered.contains("fallbackCount=0"))
        assertTrue(rendered.contains("modelSwitchCount=0"))
        assertTrue(rendered.contains("analysisCount=0"))
    }

    private fun input() = UnitOLiteralV2AcceptancePreflightInput(
        true, authorization(), id, COMMIT, ready(ExternalTranscriptionAcceptanceState.ACCEPTANCE_PENDING), true,
        Files.createTempFile("unit-o4r-result", ".txt"),
    )
    private fun authorization() = UnitOLiteralV2AcceptanceAuthorization(
        UnitOLiteralV2CleanLock.STAGE, UnitOLiteralV2CleanLock.ALLOCATION, UnitOLiteralV2CleanLock.EVIDENCE_ID,
        UnitOLiteralV2CleanLock.SOURCE_SHA256, UnitOLiteralV2CleanLock.SOURCE_BYTES, UnitOLiteralV2CleanLock.MEDIA_TYPE,
        UnitOLiteralV2CleanLock.PROVIDER, UnitOLiteralV2CleanLock.MODEL_RULE, UnitOLiteralV2CleanLock.PROFILE,
        UnitOLiteralV2CleanLock.INSTRUCTION_SHA256, UnitOLiteralV2CleanLock.SCHEMA_SHA256,
        UnitOLiteralV2CleanLock.PROCESSING_PROFILE, UnitOLiteralV2CleanLock.ENDPOINT, false,
        UnitOLiteralV2CleanLock.ADAPTER_VERSION, UnitOLiteralV2CleanLock.REQUEST_ORDINAL, COMMIT, "owner-governance", "2026-08-27T00:00:00Z",
    )
    private fun metadata() = UnitOAuthoritativeManifestFacts(id, UnitOLiteralV2CleanLock.SOURCE_SHA256,
        UnitOLiteralV2CleanLock.SOURCE_BYTES, UnitOLiteralV2CleanLock.MEDIA_TYPE)
    private fun parse(text: String): UnitOLiteralV2AcceptanceAuthorization {
        val path = Files.createTempFile("unit-o4r-authorization", ".txt")
        Files.writeString(path, text)
        return readUnitOLiteralV2Authorization(path)
    }
    private fun ready(state: ExternalTranscriptionAcceptanceState): OpenAiExternalTranscriptionReadiness.Ready =
        OpenAiExternalTranscriptionReadiness.Ready(
            OpenAiExternalTranscriptionProviderProfile(
                "2", "OpenAI", "/v1/responses", false, "gpt-4.1-mini", "RECORD_PRESENT_OR_NOT_EXPOSED",
                64L * 1024 * 1024, 20L * 1024 * 1024, 20L * 1024 * 1024, 30_000, "https://api.openai.com",
                "reviewed", "reviewed", "reviewed", "reviewed", "reviewed", "BEARER_API_CREDENTIAL", "reviewed", "reviewed",
                LocalDate.parse("2026-08-01"), "owner", LocalDate.parse("2026-09-01"), listOf("ref"), listOf("trigger"),
                UnitOLiteralV2CleanLock.PROFILE, UnitOLiteralV2CleanLock.INSTRUCTION_SHA256, UnitOLiteralV2CleanLock.SCHEMA_SHA256,
                UnitOLiteralV2CleanLock.PROCESSING_PROFILE, state,
            ),
            OpenAiExternalTranscriptionEffectiveLimits(64L * 1024 * 1024, 20L * 1024 * 1024, 20L * 1024 * 1024, 30_000),
        )
    private companion object { const val COMMIT = "future-deployed-commit" }
}
