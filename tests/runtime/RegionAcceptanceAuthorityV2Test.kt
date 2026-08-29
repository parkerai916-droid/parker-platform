package parker.core.runtime

import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.io.TempDir
import parker.core.interfaces.*
import kotlin.test.*

class RegionAcceptanceAuthorityV2Test {
    @TempDir lateinit var temp: Path

    @Test fun `v2 canonicalization is deterministic regardless of fact input order`() {
        val original = authority().manifest
        val reversed = RegionAcceptanceManifestV2.canonical(original.facts.reversed())
        assertContentEquals(original.canonicalBytes(), reversed.canonicalBytes())
        assertEquals(original.sha256(), reversed.sha256())
    }

    @Test fun `v2 distinctly binds complete provider surface both instructions and typed purpose`() {
        val authority = authority()
        val facts = authority.manifest.facts.associate { it.name to it.value }
        assertEquals(REGION_ACCEPTANCE_AUTHORITY_SCHEMA_V2, "parker.region-transcription-acceptance-authority.v2")
        assertEquals("5e96163553ebb56982bff8735ee3ab58f7b16d0c1ead4c282a970c915dfb2b43", facts["request.provider_neutral_instruction_sha256"])
        assertEquals("3e1c1c647d011f748fc2cc81cb9e17a4354b0ca879abd28888005ef8d05d71e2", facts["adapter.provider_instruction_sha256"])
        assertEquals(OPENAI_REGION_PROFILE_ID, facts["provider.profile_id"]); assertEquals("none", facts["provider.reasoning"])
        assertEquals("POST /v1/responses", facts["provider.operation"]); assertEquals("Responses API", facts["provider.endpoint_family"])
        assertEquals(RegionAcceptancePurposeCode.CONTROLLED_LIVE_FIDELITY_ACCEPTANCE.name, facts["authority.purpose_code"])
        assertFalse(facts.containsKey("request.instruction_sha256"))
    }

    @TestFactory fun `every corrected provider or purpose fact changes manifest and authority identity`() = listOf(
        "request.provider_neutral_instruction_sha256", "adapter.provider_instruction_sha256", "provider.profile_id",
        "provider.reasoning", "provider.operation", "provider.endpoint_family", "authority.purpose_code", "authority.purpose_detail",
    ).map { field -> DynamicTest.dynamicTest("$field is identity-bearing") {
        val original = authority(); val changedManifest = RegionAcceptanceManifestV2.canonical(original.manifest.facts.map { if (it.name == field) it.copy(value = it.value + "-changed") else it })
        assertNotEquals(original.manifestSha256, changedManifest.sha256())
        if (field.startsWith("authority.purpose")) {
            assertFails { original.copy(manifest = changedManifest) }
        } else {
            assertNotEquals(original.recordId, original.copy(manifest = changedManifest).recordId)
        }
    } }

    @Test fun `v2 store is deterministic create-once restart-readable and separate from v1`() {
        val root = Files.createDirectory(temp.resolve("store")); val store = FileSystemRegionAcceptanceAuthorityStorageV2(root); val authority = authority()
        store.admit(authority); store.admit(authority)
        assertEquals(authority, FileSystemRegionAcceptanceAuthorityStorageV2(root).load(authority.authorityId))
        assertTrue(Files.exists(root.resolve("authority-v2.region-acceptance-authority-v2")))
        assertFalse(Files.exists(root.resolve("authority-v2.region-acceptance-authority")))
        assertFails { store.admit(authority.copy(authorisedBy = "other")) }
    }

    @Test fun `governed creation reconstructs internally admits once and creates no attempt provider state or call`() = runTest {
        val store = store(); var reconstructions = 0; var attemptChecks = 0; var stateChecks = 0; var providerCalls = 0
        val coordinator = RegionTranscriptionAcceptanceAuthorityCreationCoordinator(
            store, RegionAcceptanceCurrentFactsReconstructor { evidence, correlation, execution ->
                reconstructions++; assertEquals(EvidenceArtifactId("evidence-v2"), evidence); assertEquals("attempt-v2", correlation); assertEquals("execution-v2", execution); reconstruction()
            }, { RegionAcceptanceProviderSurface().also { providerCalls++ } }, { attemptChecks++; false }, { stateChecks++; false },
        )
        val outcome = assertIs<RegionAcceptanceAuthorityCreationOutcome.Created>(coordinator.create(request()))
        assertEquals(1, reconstructions); assertEquals(1, providerCalls); assertEquals(1, attemptChecks); assertEquals(1, stateChecks)
        val admitted = store.load(outcome.authorityId)!!
        assertEquals(outcome.recordId, admitted.recordId)
        val facts = admitted.manifest.facts.associate { it.name to it.value }
        assertEquals("1", facts["page.1.number"]); assertEquals(ONE, facts["region.1.id"])
        assertEquals("REGION_ONLY", facts["context.policy"]); assertEquals(COMMIT, facts["deployment.runtime_commit"])
        assertEquals("AUTHORITY_ALREADY_EXISTS", assertIs<RegionAcceptanceAuthorityCreationOutcome.Blocked>(coordinator.create(request())).reason)
    }

    @Test fun `creation rejects invalid purpose surface incomplete provider configuration and inconsistent instruction identity`() = runTest {
        suspend fun create(reconstruction: RegionAcceptanceReconstruction, provider: () -> RegionAcceptanceProviderSurface?) =
            RegionTranscriptionAcceptanceAuthorityCreationCoordinator(store(), RegionAcceptanceCurrentFactsReconstructor { _,_,_ -> reconstruction }, provider, { false }, { false }).create(request(authorityId = "authority-${Files.list(temp).use { it.count() }}"))

        val noRegions = reconstruction().let { source -> source.copy(manifest = RegionAcceptanceManifest.canonical(source.manifest.facts.map { fact -> when (fact.name) {
            "page.1.number" -> fact.copy(name = "page.x.number")
            "region.1.id" -> fact.copy(name = "region.x.id")
            else -> fact
        } })) }
        assertEquals("PURPOSE_INVALID", assertIs<RegionAcceptanceAuthorityCreationOutcome.Blocked>(create(noRegions) { RegionAcceptanceProviderSurface() }).reason)
        assertEquals("PROVIDER_CONFIGURATION_UNAVAILABLE", assertIs<RegionAcceptanceAuthorityCreationOutcome.Blocked>(create(reconstruction()) { null }).reason)
        val inconsistent = reconstruction().let { it.copy(binding = it.binding.copy(identity = it.binding.identity.copy(instructionSha256 = "0".repeat(64)))) }
        assertEquals("CURRENT_FACTS_INCONSISTENT", assertIs<RegionAcceptanceAuthorityCreationOutcome.Blocked>(create(inconsistent) { RegionAcceptanceProviderSurface() }).reason)
    }

    @Test fun `creation rejects existing attempt or provider state before admission`() = runTest {
        suspend fun result(attempt: Boolean, state: Boolean): RegionAcceptanceAuthorityCreationOutcome {
            val coordinator = RegionTranscriptionAcceptanceAuthorityCreationCoordinator(store(), RegionAcceptanceCurrentFactsReconstructor { _,_,_ -> reconstruction() },
                { RegionAcceptanceProviderSurface() }, { attempt }, { state })
            return coordinator.create(request(authorityId = "authority-${attempt}-${state}"))
        }
        assertEquals("ATTEMPT_ALREADY_EXISTS", assertIs<RegionAcceptanceAuthorityCreationOutcome.Blocked>(result(true,false)).reason)
        assertEquals("PROVIDER_STATE_ALREADY_EXISTS", assertIs<RegionAcceptanceAuthorityCreationOutcome.Blocked>(result(false,true)).reason)
    }

    @Test fun `v2 bridge reaches execution only after exact complete reconstruction`() = runTest {
        val store = store(); val authority = authority(); store.admit(authority); var executions = 0
        val bridge = RegionAcceptanceExecutionCoordinatorV2(store, { FidelityFirstAcceptanceLifecycle.ACCEPTANCE_PENDING },
            RegionAcceptanceCurrentFactsReconstructor { _,_,_ -> reconstruction() }, { RegionAcceptanceProviderSurface() },
            GovernedRegionExecutionPort { executions++; GovernedRegionExecutionOutcome.Blocked(GovernedRegionRecoveryState.ATTEMPT_OUTCOME_UNKNOWN,"synthetic") })
        assertIs<RegionAcceptanceExecutionOutcomeV2.Executed>(bridge.invoke(authority.authorityId)); assertEquals(1, executions)
    }

    @TestFactory fun `v2 bridge rejects corrected-surface drift before execution`() = listOf(
        "request.provider_neutral_instruction_sha256", "adapter.provider_instruction_sha256", "provider.profile_id",
        "provider.reasoning", "provider.operation",
    ).map { field -> DynamicTest.dynamicTest("$field mismatch rejected") { runTest {
        val store = store(); val base = authority(); val changed = base.copy(manifest = RegionAcceptanceManifestV2.canonical(base.manifest.facts.map { if (it.name == field) it.copy(value=it.value+"-drift") else it }))
        // Purpose fields are unchanged, so the authority remains structurally constructible but cannot match current facts.
        store.admit(changed); var executions = 0
        val bridge = RegionAcceptanceExecutionCoordinatorV2(store,{FidelityFirstAcceptanceLifecycle.ACCEPTANCE_PENDING},RegionAcceptanceCurrentFactsReconstructor{_,_,_->reconstruction()},
            {RegionAcceptanceProviderSurface()},GovernedRegionExecutionPort{executions++;error("must not execute")})
        assertEquals("AUTHORITY_FACTS_MISMATCH",assertIs<RegionAcceptanceExecutionOutcomeV2.Blocked>(bridge.invoke(changed.authorityId)).reason);assertEquals(0,executions)
    } } }

    @Test fun `purpose detail inconsistent with reconstructed surface is rejected pre-attempt`() = runTest {
        val store=store(); val base=authority(); val purpose=RegionAcceptancePurpose.controlled(2,1)
        val manifest=RegionAcceptanceManifestV2.canonical(base.manifest.facts.map { when(it.name){"authority.purpose_detail"->it.copy(value=purpose.detail);else->it} })
        val changed=base.copy(purpose=purpose,manifest=manifest);store.admit(changed);var executions=0
        val bridge=RegionAcceptanceExecutionCoordinatorV2(store,{FidelityFirstAcceptanceLifecycle.ACCEPTANCE_PENDING},RegionAcceptanceCurrentFactsReconstructor{_,_,_->reconstruction()},
            {RegionAcceptanceProviderSurface()},GovernedRegionExecutionPort{executions++;error("must not execute")})
        assertEquals("PURPOSE_FACTS_MISMATCH",assertIs<RegionAcceptanceExecutionOutcomeV2.Blocked>(bridge.invoke(changed.authorityId)).reason);assertEquals(0,executions)
    }

    @Test fun `credential material is absent from v2 manifest payload and failures`() {
        val secret="R68A1_SYNTHETIC_SECRET";val authority=authority();assertFalse(authority.manifest.facts.any{it.name.contains("credential")||it.value.contains(secret)})
        assertFalse(String(authority.canonicalPayload()).contains(secret));val root=Files.createDirectory(temp.resolve("security"));val store=FileSystemRegionAcceptanceAuthorityStorageV2(root);store.admit(authority)
        Files.walk(root).use{paths->paths.filter(Files::isRegularFile).forEach{assertFalse(String(Files.readAllBytes(it)).contains(secret))}}
    }

    private fun store()=FileSystemRegionAcceptanceAuthorityStorageV2(Files.createDirectory(temp.resolve("store-${Files.list(temp).use{it.count()}}")))
    private fun request(authorityId:String="authority-v2")=RegionAcceptanceAuthorityCreationRequest(authorityId,"FA.9.4P-A1E-R6.8A1","execution-v2","attempt-v2",EvidenceArtifactId("evidence-v2"),"owner",Instant.parse("2026-08-30T00:00:00Z"))
    private fun authority():RegionTranscriptionAcceptanceAuthorityV2 { val r=reconstruction();val purpose=RegionAcceptancePurpose.controlled(1,1);return RegionTranscriptionAcceptanceAuthorityV2("authority-v2","FA.9.4P-A1E-R6.8A1","execution-v2",purpose,RegionAcceptanceManifestV2Factory.create(r,purpose,RegionAcceptanceProviderSurface()),1,"owner",Instant.parse("2026-08-30T00:00:00Z")) }
    private fun reconstruction():RegionAcceptanceReconstruction {
        val bytes=byteArrayOf(1,2,3);val page=PageRepresentationId("b".repeat(64));val bounds=PixelCropBounds(1,2,11,12);val crop=CanonicalPixelDigest("c".repeat(64));val id=SourceRegionId(ONE)
        val target=RegionTranscriptionTarget(EvidenceArtifactId("evidence-v2"),"a".repeat(64),page,1,PagePixelDimensions(100,100),id,bounds,crop,SourceRegionStructuralClass.TEXT_LIKE,"pixel-whitespace-source-regions-v1",1,RegionTranscriptionImage(page,bounds,crop,"image/png",RegionTranscriptionImage.sha256(bytes),bytes))
        val request=RegionTranscriptionRequest("attempt-v2",REGION_TRANSCRIPTION_PROFILE_ID,REGION_TRANSCRIPTION_SCHEMA_ID,REGION_TRANSCRIPTION_WIRE_VERSION,REGION_TRANSCRIPTION_SCHEMA_SHA256,REGION_TRANSCRIPTION_PROCESSING_PROFILE,REGION_LITERAL_TRANSCRIPTION_INSTRUCTION,listOf(target))
        val identity=FidelityFirstExecutionIdentity("execution-v2","d".repeat(64),"attempt-v2","evidence-v2","a".repeat(64),1234,"application/pdf",COMMIT,"OpenAI",OPENAI_REGION_MODEL,OPENAI_REGION_PROFILE_ID,OPENAI_REGION_INSTRUCTION_SHA256,REGION_TRANSCRIPTION_SCHEMA_SHA256,REGION_TRANSCRIPTION_PROCESSING_PROFILE,OPENAI_REGION_ADAPTER_VERSION)
        val facts=listOf(
            "source.evidence_artifact_id" to "evidence-v2","source.sha256" to "a".repeat(64),"source.byte_length" to "1234","source.media_type" to "application/pdf",
            "deployment.source_commit" to COMMIT,"deployment.build_commit" to COMMIT,"deployment.runtime_commit" to COMMIT,"deployment.image_id" to IMAGE,
            "request.correlation_id" to "attempt-v2","request.profile_id" to REGION_TRANSCRIPTION_PROFILE_ID,"request.processing_profile" to REGION_TRANSCRIPTION_PROCESSING_PROFILE,
            "request.schema_id" to REGION_TRANSCRIPTION_SCHEMA_ID,"request.schema_version" to REGION_TRANSCRIPTION_WIRE_VERSION.toString(),"request.schema_sha256" to REGION_TRANSCRIPTION_SCHEMA_SHA256,
            "request.instruction_sha256" to regionSha256(REGION_LITERAL_TRANSCRIPTION_INSTRUCTION.toByteArray()),"provider.name" to "OpenAI","provider.model" to OPENAI_REGION_MODEL,
            "provider.adapter_id" to OPENAI_REGION_ADAPTER_ID,"provider.adapter_version" to OPENAI_REGION_ADAPTER_VERSION,"provider.endpoint" to OpenAiRegionTranscriptionAdapter.ENDPOINT.toString(),
            "provider.wire_version" to REGION_TRANSCRIPTION_WIRE_VERSION.toString(),"provider.image_detail" to OPENAI_REGION_IMAGE_DETAIL,"provider.store" to "false","context.policy" to "REGION_ONLY","attempt.maximum_provider_attempts" to "1",
            "page.1.number" to "1","page.1.representation_id" to page.value,"region.1.id" to ONE,"region.1.bounds" to "1,2,11,12","order.source.1" to ONE,
        ).map{RegionAcceptanceFact(it.first,it.second)}
        return RegionAcceptanceReconstruction(RegionAcceptanceManifest.canonical(facts),GovernedRegionExecutionBinding(identity,request,listOf(id)))
    }
    companion object { const val COMMIT="7f254f7240fced981b92b773ad3e05b3b4b7d808";val IMAGE="sha256:"+"1".repeat(64);const val ONE="1f00000000000000000000000000000000000000000000000000000000000000" }
}
