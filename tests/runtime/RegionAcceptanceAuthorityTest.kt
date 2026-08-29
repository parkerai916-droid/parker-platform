package parker.core.runtime

import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.io.TempDir
import parker.core.interfaces.*
import kotlin.test.*

class RegionAcceptanceAuthorityTest {
    @TempDir lateinit var temp: Path

    @Test fun `separate authority is canonical create-once and checksum protected`() {
        val root = Files.createDirectory(temp.resolve("authorities")); val store = FileSystemRegionAcceptanceAuthorityStorage(root)
        val authority = authority(); store.admit(authority); store.admit(authority)
        assertEquals(authority, store.load(authority.authorityId)); assertEquals(64, authority.recordId.length)
        val path = root.resolve("authority-r68a.region-acceptance-authority")
        Files.write(path, Files.readAllBytes(path).also { it[it.lastIndex] = (it.last() + 1).toByte() })
        assertFails { store.load(authority.authorityId) }
    }

    @Test fun `canonical manifest is independent of supplied fact order and rejects incomplete bindings`() {
        val facts = facts(); assertEquals(RegionAcceptanceManifest.canonical(facts), RegionAcceptanceManifest.canonical(facts.reversed()))
        assertFails { RegionAcceptanceManifest.canonical(facts.filterNot { it.name == "deployment.image_id" }) }
    }

    @Test fun `bridge fails closed before reconstruction for lifecycle and deployment drift`() = runTest {
        val store = storage(); val authority = authority(); store.admit(authority); var reconstructions = 0
        fun coordinator(lifecycle: FidelityFirstAcceptanceLifecycle, image: String) = RegionAcceptanceExecutionCoordinator(
            store, { lifecycle }, { COMMIT }, { COMMIT }, { COMMIT }, { image },
            RegionAcceptanceReconstructor { reconstructions++; reconstruction(authority) },
            GovernedRegionExecutionPort { GovernedRegionExecutionOutcome.Blocked(GovernedRegionRecoveryState.ATTEMPT_OUTCOME_UNKNOWN, "synthetic") },
        )
        assertEquals("LIFECYCLE_NOT_ACCEPTANCE_PENDING", assertIs<RegionAcceptanceExecutionOutcome.Blocked>(coordinator(FidelityFirstAcceptanceLifecycle.ACCEPTED, IMAGE).invoke(authority.authorityId)).reason)
        assertEquals("DEPLOYMENT_IDENTITY_MISMATCH", assertIs<RegionAcceptanceExecutionOutcome.Blocked>(coordinator(FidelityFirstAcceptanceLifecycle.ACCEPTANCE_PENDING, "sha256:" + "9".repeat(64)).invoke(authority.authorityId)).reason)
        assertEquals(0, reconstructions)
    }

    @Test fun `bridge reconstructs exact current facts then reaches existing governed execution port once`() = runTest {
        val store = storage(); val authority = authority(); store.admit(authority); var executions = 0
        val coordinator = RegionAcceptanceExecutionCoordinator(
            store, { FidelityFirstAcceptanceLifecycle.ACCEPTANCE_PENDING }, { COMMIT }, { COMMIT }, { COMMIT }, { IMAGE },
            RegionAcceptanceReconstructor { reconstruction(authority) },
            GovernedRegionExecutionPort { executions++; GovernedRegionExecutionOutcome.Blocked(GovernedRegionRecoveryState.ATTEMPT_OUTCOME_UNKNOWN, "synthetic") },
        )
        val result = assertIs<RegionAcceptanceExecutionOutcome.Executed>(coordinator.invoke(authority.authorityId))
        assertEquals(authority.recordId, result.recordId); assertEquals(1, executions)
    }

    @Test fun `manifest drift never reaches governed execution`() = runTest {
        val store = storage(); val authority = authority(); store.admit(authority); var executions = 0
        val drifted = authority.copy(manifest = RegionAcceptanceManifest.canonical(facts().map { if (it.name == "context.policy") it.copy(value = "FULL_PAGE") else it }))
        val coordinator = RegionAcceptanceExecutionCoordinator(store, { FidelityFirstAcceptanceLifecycle.ACCEPTANCE_PENDING }, { COMMIT }, { COMMIT }, { COMMIT }, { IMAGE },
            RegionAcceptanceReconstructor { reconstruction(drifted) }, GovernedRegionExecutionPort { executions++; error("must not execute") })
        assertEquals("AUTHORITY_FACTS_MISMATCH", assertIs<RegionAcceptanceExecutionOutcome.Blocked>(coordinator.invoke(authority.authorityId)).reason)
        assertEquals(0, executions)
    }

    @TestFactory fun `every governed authority field changes canonical identity`() = listOf(
        "immutable image" to "deployment.image_id", "page pixel digest" to "page.1.pixel_digest",
        "region id" to "region.1.id", "bounds" to "region.1.bounds", "crop digest" to "region.1.crop_digest",
        "classification" to "region.1.structural_class", "source order graph" to "order.1.ambiguity",
        "context policy" to "context.policy", "endpoint family" to "provider.endpoint", "wire version" to "provider.wire_version",
        "model" to "provider.model", "adapter" to "provider.adapter_version", "profile" to "request.profile_id",
        "schema" to "request.schema_sha256", "instruction" to "request.instruction_sha256",
    ).map { (label, field) -> DynamicTest.dynamicTest("$label mutation changes checksum and record identity") {
        val original = authority(); val changed = original.copy(manifest = mutate(original.manifest, field))
        assertNotEquals(original.manifestSha256, changed.manifestSha256); assertNotEquals(original.recordId, changed.recordId)
    } }

    @TestFactory fun `bridge rejects each reconstructed governed-surface mismatch before execution`() = listOf(
        "source" to "source.sha256", "page representation" to "page.1.representation_id", "page digest" to "page.1.pixel_digest",
        "region count" to "region.1.id", "region id" to "region.1.id", "bounds" to "region.1.bounds",
        "crop digest" to "region.1.crop_digest", "source order" to "order.source.1", "context policy" to "context.policy",
        "provider" to "provider.name", "model" to "provider.model", "adapter profile" to "provider.adapter_version",
        "schema wire" to "request.schema_sha256", "instruction" to "request.instruction_sha256",
    ).map { (label, field) -> DynamicTest.dynamicTest("$label mismatch is rejected before execution") {
        runTest {
            val store = storage(); val authority = authority(); store.admit(authority); var calls = 0
            val drifted = authority.copy(manifest = mutate(authority.manifest, field))
            val coordinator = RegionAcceptanceExecutionCoordinator(store, { FidelityFirstAcceptanceLifecycle.ACCEPTANCE_PENDING }, { COMMIT }, { COMMIT }, { COMMIT }, { IMAGE },
                RegionAcceptanceReconstructor { reconstruction(drifted) }, GovernedRegionExecutionPort { calls++; error("must not execute") })
            assertEquals("AUTHORITY_FACTS_MISMATCH", assertIs<RegionAcceptanceExecutionOutcome.Blocked>(coordinator.invoke(authority.authorityId)).reason)
            assertEquals(0, calls)
        }
    } }

    @Test fun `conflicting duplicate is rejected and restart read is byte equivalent`() {
        val root = Files.createDirectory(temp.resolve("restart")); val first = FileSystemRegionAcceptanceAuthorityStorage(root); val authority = authority(); first.admit(authority)
        assertFails { first.admit(authority.copy(authorisedBy = "different-owner")) }
        assertEquals(authority, FileSystemRegionAcceptanceAuthorityStorage(root).load(authority.authorityId))
    }

    @Test fun `missing and legacy document authority cannot authorize region execution`() = runTest {
        val root = Files.createDirectory(temp.resolve("legacy-separation")); Files.writeString(root.resolve("authority-r68a.acceptance-authority"), "legacy\n")
        val store = FileSystemRegionAcceptanceAuthorityStorage(root)
        val coordinator = RegionAcceptanceExecutionCoordinator(store, { FidelityFirstAcceptanceLifecycle.ACCEPTANCE_PENDING }, { COMMIT }, { COMMIT }, { COMMIT }, { IMAGE },
            RegionAcceptanceReconstructor { error("must not reconstruct") }, GovernedRegionExecutionPort { error("must not execute") })
        assertEquals("AUTHORITY_MISSING", assertIs<RegionAcceptanceExecutionOutcome.Blocked>(coordinator.invoke("authority-r68a")).reason)
        assertEquals("legacy\n", Files.readString(root.resolve("authority-r68a.acceptance-authority")))
    }

    @Test fun `credential material never enters authority manifest checksum record or failures`() {
        val secret = "R68A_SYNTHETIC_CREDENTIAL_MUST_NOT_PERSIST"; val root = Files.createDirectory(temp.resolve("secret-scan")); val store = FileSystemRegionAcceptanceAuthorityStorage(root)
        val authority = authority(); store.admit(authority)
        assertFalse(String(authority.canonicalPayload()).contains(secret)); assertFalse(authority.manifest.facts.any { it.value.contains(secret) })
        Files.walk(root).use { paths -> paths.filter(Files::isRegularFile).forEach { assertFalse(String(Files.readAllBytes(it)).contains(secret)) } }
        val failure = runCatching { store.admit(authority.copy(authorisedBy = secret)) }.exceptionOrNull()?.message.orEmpty()
        assertFalse(failure.contains(secret))
    }

    private fun storage() = FileSystemRegionAcceptanceAuthorityStorage(Files.createDirectory(temp.resolve("store-${Files.list(temp).use { it.count() }}")))
    private fun authority() = RegionTranscriptionAcceptanceAuthority("authority-r68a", "FA.9.4P-A1E-R6.8A", "execution-r68a", RegionAcceptanceManifest.canonical(facts()), 1, "owner", Instant.parse("2026-08-30T00:00:00Z"))
    private fun facts() = listOf(
        "source.evidence_artifact_id" to "evidence-r68a", "source.sha256" to "a".repeat(64), "source.byte_length" to "1234", "source.media_type" to "application/pdf",
        "deployment.source_commit" to COMMIT, "deployment.build_commit" to COMMIT, "deployment.runtime_commit" to COMMIT, "deployment.image_id" to IMAGE,
        "request.correlation_id" to "attempt-r68a", "request.profile_id" to REGION_TRANSCRIPTION_PROFILE_ID, "request.processing_profile" to REGION_TRANSCRIPTION_PROCESSING_PROFILE,
        "request.schema_id" to REGION_TRANSCRIPTION_SCHEMA_ID, "request.schema_version" to REGION_TRANSCRIPTION_WIRE_VERSION.toString(), "request.schema_sha256" to REGION_TRANSCRIPTION_SCHEMA_SHA256,
        "request.instruction_sha256" to OPENAI_REGION_INSTRUCTION_SHA256, "provider.name" to "OpenAI", "provider.model" to OPENAI_REGION_MODEL,
        "provider.adapter_id" to OPENAI_REGION_ADAPTER_ID, "provider.adapter_version" to OPENAI_REGION_ADAPTER_VERSION, "provider.endpoint" to "https://api.openai.com/v1/responses",
        "provider.wire_version" to REGION_TRANSCRIPTION_WIRE_VERSION.toString(), "provider.image_detail" to OPENAI_REGION_IMAGE_DETAIL, "provider.store" to "false",
        "context.policy" to "REGION_ONLY", "attempt.maximum_provider_attempts" to "1", "page.1.representation_id" to "b".repeat(64),
        "page.1.pixel_digest" to "e".repeat(64), "region.1.id" to ONE, "region.1.bounds" to "1,2,11,12",
        "region.1.crop_digest" to "c".repeat(64), "region.1.structural_class" to "TEXT_LIKE",
        "order.1.ambiguity" to "UNAMBIGUOUS", "order.source.1" to ONE,
    ).map { RegionAcceptanceFact(it.first, it.second) }
    private fun mutate(manifest: RegionAcceptanceManifest, name: String): RegionAcceptanceManifest = RegionAcceptanceManifest.canonical(
        manifest.facts.map { if (it.name == name) it.copy(value = it.value + "-changed") else it },
    )
    private fun reconstruction(authority: RegionTranscriptionAcceptanceAuthority): RegionAcceptanceReconstruction {
        val bytes = byteArrayOf(1,2,3); val page = PageRepresentationId("b".repeat(64)); val bounds = PixelCropBounds(1,2,11,12); val crop = CanonicalPixelDigest("c".repeat(64))
        val target = RegionTranscriptionTarget(EvidenceArtifactId("evidence-r68a"), "a".repeat(64), page, 1, PagePixelDimensions(100,100), SourceRegionId(ONE), bounds, crop,
            SourceRegionStructuralClass.TEXT_LIKE, "pixel-whitespace-source-regions-v1", 1, RegionTranscriptionImage(page,bounds,crop,"image/png",RegionTranscriptionImage.sha256(bytes),bytes))
        val request = RegionTranscriptionRequest("attempt-r68a",REGION_TRANSCRIPTION_PROFILE_ID,REGION_TRANSCRIPTION_SCHEMA_ID,REGION_TRANSCRIPTION_WIRE_VERSION,REGION_TRANSCRIPTION_SCHEMA_SHA256,REGION_TRANSCRIPTION_PROCESSING_PROFILE,REGION_LITERAL_TRANSCRIPTION_INSTRUCTION,listOf(target))
        val identity = FidelityFirstExecutionIdentity("execution-r68a","d".repeat(64),"attempt-r68a","evidence-r68a","a".repeat(64),1234,"application/pdf",COMMIT,"OpenAI",OPENAI_REGION_MODEL,OPENAI_REGION_PROFILE_ID,OPENAI_REGION_INSTRUCTION_SHA256,REGION_TRANSCRIPTION_SCHEMA_SHA256,REGION_TRANSCRIPTION_PROCESSING_PROFILE,OPENAI_REGION_ADAPTER_VERSION)
        return RegionAcceptanceReconstruction(authority.manifest, GovernedRegionExecutionBinding(identity,request,listOf(SourceRegionId(ONE))))
    }
    companion object { const val COMMIT="4ff7e13a184af28128a38200054895df4f76ed0e"; const val ONE="1f00000000000000000000000000000000000000000000000000000000000000"; val IMAGE="sha256:"+"2".repeat(64) }
}
