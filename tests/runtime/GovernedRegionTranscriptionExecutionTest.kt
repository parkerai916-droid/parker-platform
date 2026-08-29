package parker.core.runtime

import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import parker.core.interfaces.*
import kotlin.test.*

class GovernedRegionTranscriptionExecutionTest {
    @TempDir lateinit var temp: Path

    private class FakeMechanism(
        private val store: FileSystemRegionProviderStateStore,
        private val raw: ByteArray,
        private val structured: Map<String, Any?>?,
        private val code: String,
        private val beforeResponse: () -> Unit = {},
    ) : RegionExternalTranscriptionMechanism {
        var calls = 0
        override suspend fun transcribe(request: RegionTranscriptionRequest): RegionExternalTranscriptionOutcome {
            calls++; beforeResponse()
            val receipt = store.persistReceived(request, 200, "application/json", raw)
            if (code != "RAW_ONLY") store.recordAssessment(receipt, code, structured)
            return if (code == "SUCCESS" && structured != null) RegionExternalTranscriptionOutcome.Candidate(structured) else RegionExternalTranscriptionOutcome.Failure(code)
        }
    }

    @Test fun `first attempt marker precedes transport response persists before response stage and result is resumable`() = runTest {
        val f = fixture(); val request = request(); val binding = binding(f.store, request)
        val fake = FakeMechanism(f.store, envelope(validWire()).toByteArray(), validWire(), "SUCCESS") {
            assertEquals(FidelityFirstAttemptStage.PROVIDER_ATTEMPT_STARTED, f.ledger.open(binding.identity).stages.last())
            assertFalse(f.store.responseExistsFor(request))
        }
        val outcome = assertIs<GovernedRegionExecutionOutcome.FirstAttemptCompleted>(coordinator(f, fake).execute(binding))
        assertEquals(GovernedRegionRecoveryState.DOWNSTREAM_RESUMABLE, outcome.state); assertEquals(1, fake.calls)
        assertEquals(FidelityFirstAttemptStage.PROVIDER_RESPONSE_RECEIVED, f.ledger.open(binding.identity).stages.last())
        assertTrue(f.store.responseExistsFor(request)); assertEquals(listOf(ONE), outcome.sourceRegionOrder.map { it.value })
    }

    @Test fun `duplicate and reconstructed objects recover durable response without retransmission`() = runTest {
        val f = fixture(); val r = request(); val b = binding(f.store, r); val fake = FakeMechanism(f.store, envelope(validWire()).toByteArray(), validWire(), "SUCCESS")
        coordinator(f, fake).execute(b); assertEquals(1, fake.calls)
        assertIs<GovernedRegionExecutionOutcome.Recovered>(coordinator(f, fake).execute(b)); assertEquals(1, fake.calls)
        val rebuilt = Fixture(FileSystemFidelityFirstAttemptLedger(f.ledgerRoot), FileSystemRegionProviderStateStore(f.stateRoot), f.ledgerRoot, f.stateRoot)
        val recovered = assertIs<GovernedRegionExecutionOutcome.Recovered>(coordinator(rebuilt, fake).execute(binding(rebuilt.store, r)))
        assertEquals(GovernedRegionRecoveryState.DOWNSTREAM_RESUMABLE, recovered.state); assertEquals(1, fake.calls)
    }

    @Test fun `attempt marker without response is unknown and never retries including timeout`() = runTest {
        val f = fixture(); val r = request(); val b = binding(f.store, r)
        markStarted(f.ledger, b.identity)
        val fake = FakeMechanism(f.store, byteArrayOf(1), null, "RAW_ONLY")
        val outcome = assertIs<GovernedRegionExecutionOutcome.Blocked>(coordinator(f, fake).execute(b))
        assertEquals("ATTEMPT_STARTED_WITHOUT_DURABLE_RESPONSE", outcome.reason); assertEquals(0, fake.calls)

        val f2 = fixture("timeout"); val b2 = binding(f2.store, r.copy(correlationId = "attempt-timeout"))
        val timeout = object : RegionExternalTranscriptionMechanism { var calls=0; override suspend fun transcribe(request: RegionTranscriptionRequest): RegionExternalTranscriptionOutcome { calls++; return RegionExternalTranscriptionOutcome.Failure("PROVIDER_TIMEOUT") } }
        val first = assertIs<GovernedRegionExecutionOutcome.Recovered>(coordinator(f2, timeout).execute(b2)); assertEquals(GovernedRegionRecoveryState.ATTEMPT_OUTCOME_UNKNOWN, first.state)
        coordinator(f2, timeout).execute(b2); assertEquals(1, timeout.calls)
    }

    @Test fun `failure to durably create marker prevents transport`() = runTest {
        val f = fixture(); val r = request(); val good = binding(f.store, r)
        f.ledger.open(good.identity.copy(repositoryCommit = "f".repeat(40)))
        val fake = FakeMechanism(f.store, byteArrayOf(1), null, "RAW_ONLY")
        val blocked = assertIs<GovernedRegionExecutionOutcome.Blocked>(coordinator(f, fake).execute(good))
        assertEquals("ATTEMPT_IDENTITY_CONFLICT", blocked.reason); assertEquals(0, fake.calls)
    }

    @Test fun `provider state persistence failure is explicit and remains consumed`() = runTest {
        val f=fixture(); val r=request(); val b=binding(f.store,r)
        val broken=object:RegionExternalTranscriptionMechanism { var calls=0; override suspend fun transcribe(request:RegionTranscriptionRequest):RegionExternalTranscriptionOutcome { calls++; throw RegionProviderStateException("synthetic persistence failure") } }
        val outcome=assertIs<GovernedRegionExecutionOutcome.Blocked>(coordinator(f,broken).execute(b))
        assertEquals("PROVIDER_STATE_PERSISTENCE_FAILED",outcome.reason); assertEquals(1,broken.calls)
        assertTrue(f.ledger.open(b.identity).providerAttemptStarted)
        coordinator(f,broken).execute(b); assertEquals(1,broken.calls)
    }

    @Test fun `raw parse failure validation failure refusal and valid interrupted states recover exactly`() = runTest {
        suspend fun state(name: String, code: String, structured: Map<String,Any?>?): GovernedRegionExecutionOutcome {
            val f=fixture(name); val r=request(correlation="attempt-$name"); val b=binding(f.store,r); markStarted(f.ledger,b.identity)
            val receipt=f.store.persistReceived(r,200,null,envelope(structured ?: validWire()).toByteArray()); if(code!="RAW_ONLY") f.store.recordAssessment(receipt,code,structured)
            return coordinator(f, FakeMechanism(f.store,byteArrayOf(1),null,"RAW_ONLY")).execute(b)
        }
        assertEquals(GovernedRegionRecoveryState.RAW_RESPONSE_RECOVERED, state("raw","RAW_ONLY",null).state)
        assertEquals(GovernedRegionRecoveryState.PARSE_FAILURE_RECOVERED, state("parse","SCHEMA_INVALID_RESPONSE",null).state)
        val bad=validWire().toMutableMap(); bad["correlation_id"]="wrong"
        assertEquals(GovernedRegionRecoveryState.VALIDATION_FAILURE_RECOVERED, state("validation","VALIDATION_CORRELATION_MISMATCH",bad).state)
        assertEquals(GovernedRegionRecoveryState.PARSE_FAILURE_RECOVERED, state("refusal","PROVIDER_REFUSAL",null).state)
        assertEquals(GovernedRegionRecoveryState.DOWNSTREAM_RESUMABLE, state("valid","SUCCESS",validWire(correlation="attempt-valid")).state)
    }

    @Test fun `request source region and provider configuration mismatches fail before transport`() = runTest {
        val f=fixture(); val r=request(); val base=binding(f.store,r); val fake=FakeMechanism(f.store,byteArrayOf(1),null,"RAW_ONLY")
        val cases=listOf(
            base.copy(identity=base.identity.copy(requestId="0".repeat(64))),
            base.copy(identity=base.identity.copy(sourceSha256="d".repeat(64))),
            base.copy(sourceRegionOrder=listOf(SourceRegionId(TWO))),
            base.copy(identity=base.identity.copy(model="wrong")),
            base.copy(identity=base.identity.copy(adapterVersion="wrong")),
            base.copy(identity=base.identity.copy(instructionSha256="e".repeat(64))),
            base.copy(identity=base.identity.copy(schemaSha256="e".repeat(64))),
        )
        cases.forEach { assertIs<GovernedRegionExecutionOutcome.Blocked>(coordinator(f,fake).execute(it)) }
        assertEquals(0,fake.calls)
    }

    @Test fun `changed region crop context and page representation produce distinct request identity`() {
        val f=fixture(); val a=request(); val base=f.store.requestDigestFor(a)
        val changedCrop=request(targets=listOf(target(ONE,2,crop="d".repeat(64))))
        val changedContext=request(targets=listOf(target(ONE,2,context=true)))
        val changedPage=request(targets=listOf(target(ONE,2,pageId="e".repeat(64))))
        listOf(changedCrop,changedContext,changedPage).forEach { assertNotEquals(base,f.store.requestDigestFor(it)) }
    }

    @Test fun `provider order cannot replace deterministic A1 source order`() = runTest {
        val f=fixture(); val r=request(targets=listOf(target(PAGE2,2),target(PAGE3,3))); val sourceOrder=listOf(SourceRegionId(PAGE2),SourceRegionId(PAGE3)); val b=binding(f.store,r,sourceOrder)
        val wire=validWire(r.correlationId,listOf(block(PAGE3,3,"authorization",1),block(PAGE2,2,"proposition",2)))
        val fake=FakeMechanism(f.store,envelope(wire).toByteArray(),wire,"SUCCESS")
        val out=assertIs<GovernedRegionExecutionOutcome.FirstAttemptCompleted>(coordinator(f,fake).execute(b))
        assertEquals(listOf(PAGE2,PAGE3),out.sourceRegionOrder.map{it.value})
        assertEquals(listOf(PAGE3,PAGE2),out.validated!!.blocksInProviderOrder.map{it.sourceRegionId.value})
    }

    @Test fun `concurrent same execution permits at most one transport`() = runTest {
        val f=fixture(); val r=request(); val b=binding(f.store,r); val fake=FakeMechanism(f.store,envelope(validWire()).toByteArray(),validWire(),"SUCCESS")
        listOf(async { coordinator(f,fake).execute(b) }, async { coordinator(f,fake).execute(b) }).awaitAll()
        assertEquals(1,fake.calls)
    }

    @Test fun `durable artifacts exclude credential and authorization material and configuration forbids fallback`() = runTest {
        val f=fixture(); val r=request(); val fake=FakeMechanism(f.store,envelope(validWire()).toByteArray(),validWire(),"SUCCESS")
        coordinator(f,fake).execute(binding(f.store,r))
        Files.walk(temp).use { paths -> paths.filter(Files::isRegularFile).forEach { p -> val text=runCatching{Files.readString(p)}.getOrDefault(""); assertFalse(text.contains("Authorization")); assertFalse(text.contains("API_KEY")); assertFalse(text.contains("R66_SECRET")) } }
        assertFails { RegionProviderStateRootConfiguration(true,null).validatedRoot() }
        assertNull(RegionProviderStateRootConfiguration(false,null).validatedRoot())
    }

    private data class Fixture(val ledger:FileSystemFidelityFirstAttemptLedger,val store:FileSystemRegionProviderStateStore,val ledgerRoot:Path,val stateRoot:Path)
    private fun fixture(suffix:String="main"):Fixture { val base=Files.createDirectories(temp.resolve(suffix)); val l=Files.createDirectories(base.resolve("ledger")); val s=Files.createDirectories(base.resolve("state")); return Fixture(FileSystemFidelityFirstAttemptLedger(l){Instant.parse("2026-08-29T00:00:00Z")},FileSystemRegionProviderStateStore(s),l,s) }
    private fun coordinator(f:Fixture,m:RegionExternalTranscriptionMechanism)=GovernedRegionTranscriptionExecutionCoordinator(f.ledger,f.store,m)
    private fun markStarted(l:FileSystemFidelityFirstAttemptLedger,i:FidelityFirstExecutionIdentity){ l.advancePreAttempt(i,FidelityFirstAttemptStage.PREFLIGHT_PASSED);l.advancePreAttempt(i,FidelityFirstAttemptStage.SOURCE_RETRIEVED);l.advancePreAttempt(i,FidelityFirstAttemptStage.REQUEST_PREPARED);l.transition(i,FidelityFirstAttemptStage.PROVIDER_ATTEMPT_STARTED) }
    private fun binding(store:FileSystemRegionProviderStateStore,r:RegionTranscriptionRequest,order:List<SourceRegionId> = r.targets.map{it.sourceRegionId}):GovernedRegionExecutionBinding { val t=r.targets.first(); val i=FidelityFirstExecutionIdentity("execution-${r.correlationId}",store.requestDigestFor(r),r.correlationId,t.sourceEvidenceArtifactId.value,t.sourceSha256,1234,"application/pdf","0".repeat(40),"OpenAI",OPENAI_REGION_MODEL,OPENAI_REGION_PROFILE_ID,OPENAI_REGION_INSTRUCTION_SHA256,r.schemaSha256,r.processingProfile,OPENAI_REGION_ADAPTER_VERSION); return GovernedRegionExecutionBinding(i,r,order) }
    private fun request(correlation:String="attempt-r66",targets:List<RegionTranscriptionTarget> = listOf(target(ONE,2)))=RegionTranscriptionRequest(correlation,REGION_TRANSCRIPTION_PROFILE_ID,REGION_TRANSCRIPTION_SCHEMA_ID,REGION_TRANSCRIPTION_WIRE_VERSION,REGION_TRANSCRIPTION_SCHEMA_SHA256,REGION_TRANSCRIPTION_PROCESSING_PROFILE,REGION_LITERAL_TRANSCRIPTION_INSTRUCTION,targets)
    private fun target(id:String,page:Int,crop:String="c".repeat(64),context:Boolean=false,pageId:String="b".repeat(64)):RegionTranscriptionTarget { val bytes=byteArrayOf(1,2,3);val pid=PageRepresentationId(pageId);val bounds=PixelCropBounds(1,2,11,12);val cd=CanonicalPixelDigest(crop);val image=RegionTranscriptionImage(pid,bounds,cd,"image/png",RegionTranscriptionImage.sha256(bytes),bytes);val ctx=if(context)RegionTranscriptionImage(pid,PixelCropBounds(0,0,100,100),CanonicalPixelDigest("f".repeat(64)),"image/png",RegionTranscriptionImage.sha256(bytes),bytes)else null;return RegionTranscriptionTarget(EvidenceArtifactId("evidence-r66"),"a".repeat(64),pid,page,PagePixelDimensions(100,100),SourceRegionId(id),bounds,cd,SourceRegionStructuralClass.TEXT_LIKE,"pixel-whitespace-source-regions-v1",1,image,ctx) }
    private fun validWire(correlation:String="attempt-r66",blocks:List<Map<String,Any?>> = listOf(block(ONE,2,"literal",1)))=linkedMapOf<String,Any?>("correlation_id" to correlation,"transcription_profile_id" to REGION_TRANSCRIPTION_PROFILE_ID,"schema_id" to REGION_TRANSCRIPTION_SCHEMA_ID,"schema_version" to REGION_TRANSCRIPTION_WIRE_VERSION,"provider_provenance" to mapOf("provider" to "OpenAI","requested_model" to OPENAI_REGION_MODEL,"provider_reported_model" to OPENAI_REGION_MODEL,"provider_response_id" to "resp-r66","adapter_id" to OPENAI_REGION_ADAPTER_ID,"adapter_version" to OPENAI_REGION_ADAPTER_VERSION,"parser_id" to OPENAI_REGION_PARSER_ID,"parser_version" to OPENAI_REGION_PARSER_VERSION),"blocks" to blocks)
    private fun block(id:String,page:Int,text:String,ordinal:Int)=mapOf<String,Any?>("source_region_id" to id,"page_number" to page,"literal_text" to text,"status" to "TRANSCRIBED","uncertainties" to emptyList<Any>(),"warnings" to emptyList<String>(),"provider_returned_ordinal" to ordinal,"visual_observations" to emptyList<Any>())
    private fun envelope(wire:Map<String,Any?>)=RegionJson.encode(mapOf("id" to "resp-r66","model" to OPENAI_REGION_MODEL,"output" to listOf(mapOf("type" to "message","content" to listOf(mapOf("type" to "output_text","text" to RegionJson.encode(wire)))))))
    companion object { val ONE="1f"+"0".repeat(62);val TWO="2f"+"0".repeat(62);const val PAGE2="5dfb6c252dd668e7ae9dc1be95c8243c4505916e825ca143a0b02e543a9ab668";const val PAGE3="e2c2d8fe0e894fd2b42f53f75c4c7d1304755db1486a29877c01265a6b3e84ff" }
}
