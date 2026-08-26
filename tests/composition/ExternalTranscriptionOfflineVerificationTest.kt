package parker.composition

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import parker.core.runtime.ExternalTranscriptionOwnerInvocationCoordinator
import parker.core.runtime.OpenAiResponsesExternalTranscriptionAdapter

/**
 * Unit M — Offline Verification matrix index and structural gate.
 *
 * Behavioral proofs remain in their owning Unit A–L suites and are selected by the dedicated
 * Gradle task. This class makes that reuse auditable: each adopted governance surface maps to
 * concrete loaded test classes, and the gate itself is proven to use only the ordinary offline
 * test source set rather than the detached live-acceptance source set.
 */
class ExternalTranscriptionOfflineVerificationTest {
    private val matrix = linkedMapOf(
        "authorization" to listOf(
            "parker.core.runtime.AuthorizationPurposeEndToEndVerificationTest",
            "parker.core.runtime.ExternalTranscriptionOwnerInvocationCoordinatorTest",
        ),
        "custody-and-immutability" to listOf("parker.core.runtime.ExternalTranscriptionOwnerInvocationCoordinatorTest"),
        "processing-representation" to listOf("parker.core.runtime.OcrProcessingRepresentationFactoryTest"),
        "provider-request-and-failures" to listOf("parker.core.runtime.OpenAiResponsesExternalTranscriptionAdapterTest"),
        "secrets" to listOf(
            "parker.composition.OpenAiApiCredentialTest",
            "parker.composition.OwnerUiEvidenceRuntimeAdapterTest",
        ),
        "provider-profile" to listOf("parker.composition.OpenAiExternalTranscriptionProviderProfileTest"),
        "structured-result-and-fidelity" to listOf(
            "parker.core.runtime.OcrStructuredResultValidatorTest",
            "parker.core.interfaces.OcrTranscriptionProvenanceTest",
            "parker.core.interfaces.OcrOutputModelTest",
        ),
        "durable-admission-and-restart" to listOf(
            "parker.core.runtime.ExternalTranscriptionDurableAdmissionTest",
            "parker.composition.DerivativeContentPersistenceRestartAcceptanceTest",
        ),
        "owner-ui" to listOf(
            "parker.composition.OwnerEvidenceHttpServerTest",
            "parker.composition.OwnerUiEvidenceRuntimeAdapterTest",
        ),
        "exact-generation-analysis-and-save" to listOf(
            "parker.core.runtime.DocumentAnalysisCoordinatorTest",
            "parker.core.runtime.TierBOcrContentRetrievalCoordinatorTest",
            "parker.core.runtime.SavedAnalysisCoordinatorTest",
        ),
        "local-ocr-regression" to listOf(
            "parker.core.runtime.DoclingOcrProviderAdapterTest",
            "parker.composition.ParkerRuntimeOcrCompositionTest",
            "parker.composition.ParkerRuntimeTierBOcrDurableGenerationTest",
        ),
        "production-disabled-and-isolation" to listOf(
            "parker.core.runtime.OpenAiResponsesExternalTranscriptionAdapterTest",
            "parker.composition.OpenAiExternalTranscriptionProviderProfileTest",
        ),
    )

    @Test
    fun `every adopted governance surface maps to concrete compiled offline proofs`() {
        val required = setOf(
            "authorization", "custody-and-immutability", "processing-representation",
            "provider-request-and-failures", "secrets", "provider-profile",
            "structured-result-and-fidelity", "durable-admission-and-restart", "owner-ui",
            "exact-generation-analysis-and-save", "local-ocr-regression", "production-disabled-and-isolation",
        )
        assertTrue(matrix.keys.containsAll(required))
        matrix.values.flatten().distinct().forEach { Class.forName(it) }
    }

    @Test
    fun `dedicated gate uses ordinary tests only and cannot include live acceptance`() {
        val build = File("build.gradle.kts").readText()
        val start = build.indexOf("tasks.register<Test>(\"externalTranscriptionOfflineVerification\")")
        assertTrue(start >= 0)
        val block = build.substring(start, build.indexOf("\n}", start) + 2)
        assertTrue(block.contains("sourceSets.test.get().output.classesDirs"))
        assertFalse(block.contains("liveModelEvaluation"))
        assertFalse(block.contains("tests/integration"))
        assertFalse(block.contains("live.enabled"))
    }

    @Test
    fun `provider adapter coordinator and owner UI remain capability isolated`() {
        val adapterFields = OpenAiResponsesExternalTranscriptionAdapter::class.java.declaredFields.map { it.type.name }
        val coordinatorFields = ExternalTranscriptionOwnerInvocationCoordinator::class.java.declaredFields.map { it.type.name }
        listOf("EvidenceCustodian", "PermissionEngine", "Memory", "Knowledge", "Analysis", "OwnerUi", "DerivativeStorage", "Docling")
            .forEach { forbidden -> adapterFields.forEach { assertFalse(it.contains(forbidden)) } }
        listOf("Docling", "Fallback", "ProviderRegistry", "Analysis")
            .forEach { forbidden -> coordinatorFields.forEach { assertFalse(it.contains(forbidden)) } }

        val uiSource = File("src/composition/OwnerEvidenceHttpServer.kt").readText()
        listOf("OpenAiResponsesTransport", "OpenAiApiCredential", "provider raw JSON")
            .forEach { assertFalse(uiSource.contains(it)) }
    }

    @Test
    fun `production composition is fail closed and sentinel values are absent from production`() {
        val runtime = File("src/composition/ParkerRuntime.kt").readText()
        assertTrue(runtime.contains("DisabledExternalTranscriptionMechanism"))
        assertTrue(runtime.contains("OpenAiResponsesExternalTranscriptionAdapter("))
        assertTrue(runtime.contains("OpenAiExternalTranscriptionBackendReadiness.Ready"))

        val sentinels = listOf("SOURCE_SECRET_SENTINEL", "TRANSCRIPT_SECRET_SENTINEL", "API_KEY_SENTINEL")
        File("src").walkTopDown().filter { it.isFile && it.extension == "kt" }.forEach { file ->
            val text = file.readText()
            sentinels.forEach { assertFalse(text.contains(it), "${file.path} contains a Unit M sentinel") }
        }
    }
}
