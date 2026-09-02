package parker.core.runtime

import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import parker.composition.OpenAiApiCredential
import parker.core.interfaces.*
import kotlin.test.*

class OrdinaryRequestRegionV8ExecutionConvergenceTest {
    @TempDir lateinit var temp:Path
    private val commit="a".repeat(40);private val now=Instant.parse("2026-08-31T00:00:00Z")

    @Test fun `exact V8 capability is selectable but not accepted by source presence`() {
        val c=OrdinaryRequestRegionV8CapabilityIdentity()
        assertEquals(ORDINARY_REQUEST_REGION_V8_CAPABILITY_ID,c.capabilityId)
        assertEquals("c0479979720455d2de3fc9861eeb5dee323a4770bdb15f807af611ad426f9ec0",c.capabilityDigest)
        assertEquals(REQUEST_REGION_V8_PROFILE_ID,c.profile);assertEquals(REQUEST_REGION_V8_SCHEMA_ID,c.schema);assertEquals(8,c.wireVersion)
        assertEquals("7.0.0",c.adapterVersion);assertEquals("3.0.0",c.parserVersion);assertEquals(REQUEST_REGION_V8_PROCESSING_PROFILE,c.processing)
        val store=FileSystemOrdinaryRequestRegionV8CapabilityAcceptanceStore(dir("acceptance"));val evaluator=OrdinaryRequestRegionV8AcceptanceEvaluator(store){commit}
        assertEquals(OrdinaryRequestRegionV8AcceptanceEvaluation.NotAccepted,evaluator.evaluate())
        val projected=ProductionAcquisitionCapabilityCatalogue.ordinaryRequestRegionV8Capability(false)
        assertEquals(c.capabilityDigest,projected.providerConfiguration?.configurationIdentity)
        assertIs<AcquisitionAvailability.Unavailable>(projected.availability)
    }

    @Test fun `V8 acceptance and authorization bind exact capability and reject substitution`() {
        val acceptanceRoot=dir("acceptance-binding");val store=FileSystemOrdinaryRequestRegionV8CapabilityAcceptanceStore(acceptanceRoot)
        val coordinator=OrdinaryRequestRegionV8CapabilityAcceptanceCoordinator(store,{commit},"owner",{now})
        assertIs<OrdinaryRegionCapabilityPromotionOutcome.Blocked>(coordinator.create(OrdinaryRegionCapabilityPromotionRequest(ORDINARY_REGION_CAPABILITY_ID,commit)))
        val created=assertIs<OrdinaryRegionCapabilityPromotionOutcome.V8Created>(coordinator.create(OrdinaryRegionCapabilityPromotionRequest(ORDINARY_REQUEST_REGION_V8_CAPABILITY_ID,commit)))
        assertEquals(ORDINARY_REQUEST_REGION_V8_ACCEPTANCE_EVIDENCE_SHA256,created.record.evidenceSha256)
        assertIs<OrdinaryRegionCapabilityPromotionOutcome.V8Existing>(coordinator.create(OrdinaryRegionCapabilityPromotionRequest(ORDINARY_REQUEST_REGION_V8_CAPABILITY_ID,commit)))
        val auth=FileSystemOrdinaryRequestRegionV8AuthorizationStore(dir("auth-binding"));val grant=grant("auth-v8")
        auth.create(grant);assertEquals(ORDINARY_REQUEST_REGION_V8_CAPABILITY_ID,auth.load("auth-v8").grant.capabilityId)
        assertFails{OrdinaryRequestRegionV8OwnerAuthorization("wrong","evidence","b".repeat(64),ORDINARY_REGION_CAPABILITY_ID,
            OrdinaryRegionCapabilityIdentity().digest(),"OpenAI","owner",now,now.plusSeconds(60))}
        assertFails{OrdinaryRequestRegionV8OwnerAuthorization("wrong2","evidence","b".repeat(64),ORDINARY_REQUEST_REGION_V8_CAPABILITY_ID,
            OrdinaryRegionCapabilityIdentity().digest(),"OpenAI","owner",now,now.plusSeconds(60))}
    }

    @Test fun `exact authorization codec binds complete envelope and historical v1 remains weaker history`() {
        val store=FileSystemOrdinaryRequestRegionV8AuthorizationStore(dir("auth-v2"));val exact=exactGrant()
        store.create(exact);assertEquals(exact,store.load(exact.authorizationId).grant)
        val historical=grant("historical-v1");store.create(historical);assertEquals(1,store.load("historical-v1").grant.formatVersion)
        assertNull(store.load("historical-v1").grant.preparationIdentity)
        val mutations=listOf<(OrdinaryRequestRegionV8OwnerAuthorization)->OrdinaryRequestRegionV8OwnerAuthorization>(
            {it.copy(evidenceArtifactId="other-evidence")},{it.copy(sourceSha256="c".repeat(64))},{it.copy(preparationIdentity="d".repeat(64))},{it.copy(requestDigest="0".repeat(64))},
            {it.copy(capabilityDigest="f".repeat(64))},{it.copy(authorizationPurpose="wrong")},{it.copy(provider="Claude")},
            {it.copy(providerProfile="wrong")},{it.copy(model="wrong")},{it.copy(maximumProviderCalls=2)},{it.copy(automaticRetryLimit=1)})
        mutations.forEach{change->assertFails{change(exact)}}
        val v3=payload(2,ORDINARY_REQUEST_REGION_V8_CAPABILITY_ID,OrdinaryRequestRegionV8Capability().digest(),8).copy(representationVersion=3,
            providerProfile=REQUEST_REGION_V8_PROVIDER_PROFILE,preparationIdentity=exact.preparationIdentity,preparationProfile=exact.preparationProfile,
            preparationProfileVersion=1,providerBodyDigest=exact.providerBodyDigest,authorizationPurpose=exact.authorizationPurpose,
            maximumProviderCalls=1,automaticRetryLimit=0,externalReasoningAuthorized=false)
        val roundTrip=(DerivativeContentCodec.decode(DerivativeContentCodec.encode(entry("v3",v3))).payload as TierADerivativePayload.RegionTranscription).value
        assertEquals(v3,roundTrip)
    }

    @Test fun `provider attempt budget survives reentry and provider failure is never retried`()=runTest {
        suspend fun scenario(name:String,raw:ByteArray,valid:Boolean){
            val c=construction();val state=FileSystemRequestRegionV8ProviderStateStore(dir("state-$name"));val prepared=prepared(c);var calls=0
            val exchange=OpenAiRequestRegionV8ProviderExchange(OpenAiApiCredential.fromEnvironment("SYNTHETIC_V8_KEY")!!,
                OpenAiResponsesTransport{calls++;OpenAiResponsesTransportResponse(200,raw)},state)
            val coordinator=GovernedRequestRegionV8ExecutionCoordinator(FileSystemFidelityFirstAttemptLedger(dir("ledger-$name")),state,exchange)
            val first=coordinator.execute(prepared);if(valid)assertIs<OrdinaryRequestRegionV8ExecutionOutcome.Valid>(first)else assertIs<OrdinaryRequestRegionV8ExecutionOutcome.Invalid>(first)
            val second=coordinator.execute(prepared);if(valid)assertIs<OrdinaryRequestRegionV8ExecutionOutcome.Valid>(second)else assertIs<OrdinaryRequestRegionV8ExecutionOutcome.Invalid>(second)
            assertEquals(1,calls,"persistent provider state/attempt ledger must prohibit a second call")
        }
        val c=construction();scenario("success",envelope(wire(c.request,c.request.regions.indices.map{it+1},false)).toByteArray(),true)
        scenario("failure","{malformed".toByteArray(),false)
    }

    @Test fun `V8 provider state truthfully binds capability request manifest body and raw before parse`() {
        val c=construction();val store=FileSystemRequestRegionV8ProviderStateStore(dir("provider-state"));val cap=OrdinaryRequestRegionV8CapabilityIdentity()
        val raw="not-json-provider-evidence".toByteArray();val receipt=store.persistReceived(c,cap,commit,"execution-v8",200,"application/json",raw)
        val before=store.read(receipt.recordId)
        assertTrue(before.downstreamProcessingPending);assertContentEquals(raw,before.rawBytes)
        assertEquals(cap.capabilityId,before.capabilityId);assertEquals(cap.capabilityDigest,before.capabilityDigest)
        assertEquals(c.requestBindingSha256,before.requestDigest);assertEquals(c.providerBodySha256,before.providerBodyDigest)
        assertTrue(before.manifestDigest.matches(Regex("^[0-9a-f]{64}$")))
        store.recordAssessment(receipt,"V8_PARSE_OR_VALIDATION_FAILED",null)
        val after=store.read(receipt.recordId);assertFalse(after.downstreamProcessingPending);assertContentEquals(raw,after.rawBytes)
        assertNull(after.exactStructuredState);assertEquals("V8_PARSE_OR_VALIDATION_FAILED",after.outcomeCode)
    }

    @Test fun `offline V8 exchange persists raw before validation and replays Parker order independent of provider order and ordinal`()=runTest {
        val c=construction();val state=FileSystemRequestRegionV8ProviderStateStore(dir("exchange"));val prepared=prepared(c)
        val wire=wire(c.request,c.request.regions.indices.map{if(it%2==0)2 else 1},reverse=true)
        val raw=envelope(wire).toByteArray();var calls=0
        val transport=OpenAiResponsesTransport{calls++;OpenAiResponsesTransportResponse(200,raw)}
        val exchange=OpenAiRequestRegionV8ProviderExchange(OpenAiApiCredential.fromEnvironment("SYNTHETIC_V8_KEY")!!,transport,state)
        val outcome=assertIs<RequestRegionV8ProviderExchangeOutcome.Valid>(exchange.exchange(prepared));assertEquals(1,calls)
        assertContentEquals(raw,outcome.state.rawBytes);assertEquals(REQUEST_REGION_V8_PROVIDER_STATE_FORMAT,
            (RegionJson.parse(Files.readString(dirPath("exchange").resolve("${outcome.state.recordId}.request-region-v8-provider-state"))) as Map<*,*>)["record"]?.let{(it as Map<*,*>)["format"]})
        val derivative=RequestRegionV8DerivativeBinder().bind(c.request,outcome.result).getOrThrow()
        assertEquals(c.request.regions.map{it.id.value},derivative.blocksInParkerOrder.map{it.requestRegionId})
        assertEquals(c.request.regions.map{it.constituentIds.map(SourceRegionId::value)},derivative.blocksInParkerOrder.map{it.constituentSourceRegionIds})
        assertEquals(c.request.regions.indices.map{if(it%2==0)2 else 1}.reversed(),outcome.result.blocksInProviderOrder.map{it.providerReturnedOrdinal})
    }

    @Test fun `offline V8 exchange enriches null provenance from authoritative provider envelope`()=runTest {
        val c=construction();val state=FileSystemRequestRegionV8ProviderStateStore(dir("enrichment"));val prepared=prepared(c)
        val raw=envelope(wire(c.request,c.request.regions.indices.map{it+1},reverse=false,providerResponseId=null,providerReportedModel=null)).toByteArray();var calls=0
        val exchange=OpenAiRequestRegionV8ProviderExchange(OpenAiApiCredential.fromEnvironment("SYNTHETIC_V8_KEY")!!,
            OpenAiResponsesTransport{calls++;OpenAiResponsesTransportResponse(200,raw)},state)
        val outcome=assertIs<RequestRegionV8ProviderExchangeOutcome.Valid>(exchange.exchange(prepared));assertEquals(1,calls)
        assertEquals("resp-offline",outcome.result.providerProvenance.providerResponseId)
        assertEquals(REQUEST_REGION_MODEL,outcome.result.providerProvenance.providerReportedModel)
        val persisted=state.read(outcome.state.recordId).exactStructuredState!!
        val p=persisted["provider_provenance"] as Map<*,*>
        assertEquals("resp-offline",p["provider_response_id"]);assertEquals(REQUEST_REGION_MODEL,p["provider_reported_model"])
    }

    @Test fun `offline V8 exchange rejects conflicting structured provenance`()=runTest {
        val c=construction();val state=FileSystemRequestRegionV8ProviderStateStore(dir("conflict"));val prepared=prepared(c)
        val raw=envelope(wire(c.request,c.request.regions.indices.map{it+1},reverse=false,providerResponseId="resp-other",providerReportedModel=REQUEST_REGION_MODEL)).toByteArray()
        val exchange=OpenAiRequestRegionV8ProviderExchange(OpenAiApiCredential.fromEnvironment("SYNTHETIC_V8_KEY")!!,
            OpenAiResponsesTransport{OpenAiResponsesTransportResponse(200,raw)},state)
        assertIs<RequestRegionV8ProviderExchangeOutcome.Invalid>(exchange.exchange(prepared))
    }

    @Test fun `offline V8 exchange rejects conflicting structured model`()=runTest {
        val c=construction();val state=FileSystemRequestRegionV8ProviderStateStore(dir("model-conflict"));val prepared=prepared(c)
        val raw=envelope(wire(c.request,c.request.regions.indices.map{it+1},false,providerReportedModel="other-model")).toByteArray()
        val exchange=OpenAiRequestRegionV8ProviderExchange(OpenAiApiCredential.fromEnvironment("SYNTHETIC_V8_KEY")!!,
            OpenAiResponsesTransport{OpenAiResponsesTransportResponse(200,raw)},state)
        assertIs<RequestRegionV8ProviderExchangeOutcome.Invalid>(exchange.exchange(prepared))
    }

    @Test fun `offline V8 exchange rejects missing authoritative envelope identity`()=runTest {
        val c=construction();val state=FileSystemRequestRegionV8ProviderStateStore(dir("missing-envelope-id"));val prepared=prepared(c)
        val structured=wire(c.request,c.request.regions.indices.map{it+1},false,providerResponseId=null,providerReportedModel=null)
        val raw=RegionJson.encode(mapOf("model" to REQUEST_REGION_MODEL,"output" to listOf(mapOf("type" to "message","content" to listOf(mapOf("type" to "output_text","text" to RegionJson.encode(structured))))))).toByteArray()
        val exchange=OpenAiRequestRegionV8ProviderExchange(OpenAiApiCredential.fromEnvironment("SYNTHETIC_V8_KEY")!!,
            OpenAiResponsesTransport{OpenAiResponsesTransportResponse(200,raw)},state)
        assertIs<RequestRegionV8ProviderExchangeOutcome.Invalid>(exchange.exchange(prepared))
    }

    @Test fun `malformed V8 exchange preserves raw and historical V5 plus V8 derivative readback remain truthful`()=runTest {
        val c=construction();val state=FileSystemRequestRegionV8ProviderStateStore(dir("malformed"));val prepared=prepared(c);val raw="{malformed".toByteArray()
        val exchange=OpenAiRequestRegionV8ProviderExchange(OpenAiApiCredential.fromEnvironment("SYNTHETIC_V8_KEY")!!,
            OpenAiResponsesTransport{OpenAiResponsesTransportResponse(200,raw)},state)
        val invalid=assertIs<RequestRegionV8ProviderExchangeOutcome.Invalid>(exchange.exchange(prepared));assertContentEquals(raw,invalid.state.rawBytes)
        val historical=payload(1,ORDINARY_REGION_CAPABILITY_ID,"historical-v5-capability-digest-not-persisted",5)
        val current=payload(2,ORDINARY_REQUEST_REGION_V8_CAPABILITY_ID,OrdinaryRequestRegionV8Capability().digest(),8)
        val oldRoundTrip=DerivativeContentCodec.decode(DerivativeContentCodec.encode(entry("old",historical))).payload as TierADerivativePayload.RegionTranscription
        val newRoundTrip=DerivativeContentCodec.decode(DerivativeContentCodec.encode(entry("new",current))).payload as TierADerivativePayload.RegionTranscription
        assertEquals(ORDINARY_REGION_CAPABILITY_ID,oldRoundTrip.value.capabilityId);assertEquals(5,oldRoundTrip.value.wireVersion)
        assertEquals(ORDINARY_REQUEST_REGION_V8_CAPABILITY_ID,newRoundTrip.value.capabilityId);assertEquals(8,newRoundTrip.value.wireVersion)
        assertNotEquals(oldRoundTrip.value.capabilityId,newRoundTrip.value.capabilityId)
    }

    private fun construction():CanonicalRequestRegionV8Construction {val p=Path.of("tests/fixtures/document-ingestion-bakeoff/fixtures/02-multicolumn-complex.pdf");val b=Files.readAllBytes(p);return CanonicalRequestRegionV8Builder().build(EvidenceArtifactId("evidence-v8-convergence"),sha(b),"application/pdf",b,"oi11r2-offline")}
    private fun prepared(c:CanonicalRequestRegionV8Construction):OrdinaryRequestRegionV8PreparedRequest {val cap=OrdinaryRequestRegionV8CapabilityIdentity();val r=c.request.regions.first();return OrdinaryRequestRegionV8PreparedRequest(c,FidelityFirstExecutionIdentity("execution-v8",c.requestBindingSha256,c.request.correlationId,r.sourceEvidenceArtifactId.value,r.sourceSha256,1,"application/pdf",commit,cap.provider,cap.model,cap.profile,cap.instructionSha256,cap.schemaSha256,cap.processing,cap.adapterVersion),cap,commit)}
    private fun wire(r:RequestRegionV8Request,ordinals:List<Int>,reverse:Boolean,providerResponseId:String?="resp-offline",providerReportedModel:String?=REQUEST_REGION_MODEL):Map<String,Any?> {val blocks=r.regions.mapIndexed{i,x->linkedMapOf<String,Any?>("request_region_id" to x.id.value,"page_number" to x.pageNumber,"literal_text" to "literal-$i","status" to "TRANSCRIBED","uncertainties" to if(i==0)listOf(mapOf("category" to "AMBIGUOUS","description" to "uncertain","alternatives" to listOf("x"),"provider_confidence" to "low"))else emptyList<Any>(),"warnings" to if(i==1)listOf("warning")else emptyList<String>(),"provider_returned_ordinal" to ordinals[i])}.let{if(reverse)it.reversed()else it};return linkedMapOf("correlation_id" to r.correlationId,"transcription_profile_id" to REQUEST_REGION_V8_PROFILE_ID,"schema_id" to REQUEST_REGION_V8_SCHEMA_ID,"schema_version" to 8,"provider_provenance" to linkedMapOf("provider" to REQUEST_REGION_PROVIDER,"requested_model" to REQUEST_REGION_MODEL,"provider_reported_model" to providerReportedModel,"provider_response_id" to providerResponseId,"adapter_id" to REQUEST_REGION_ADAPTER_ID,"adapter_version" to REQUEST_REGION_V8_ADAPTER_VERSION,"parser_id" to REQUEST_REGION_PARSER_ID,"parser_version" to REQUEST_REGION_V8_PARSER_VERSION),"blocks" to blocks)}
    private fun envelope(w:Map<String,Any?>,id:String="resp-offline",model:String=REQUEST_REGION_MODEL)=RegionJson.encode(mapOf("id" to id,"model" to model,"output" to listOf(mapOf("type" to "message","content" to listOf(mapOf("type" to "output_text","text" to RegionJson.encode(w)))))))
    private fun grant(id:String)=OrdinaryRequestRegionV8OwnerAuthorization(id,"evidence","b".repeat(64),ORDINARY_REQUEST_REGION_V8_CAPABILITY_ID,OrdinaryRequestRegionV8Capability().digest(),"OpenAI","owner",now,now.plusSeconds(60))
    private fun exactGrant():OrdinaryRequestRegionV8OwnerAuthorization {val args=listOf("evidence","b".repeat(64),"c".repeat(64),FULL_PAGE_ACHROMATIC_PROFILE_ID);val correlation="preparation-"+"d".repeat(64);val expires=now.plusSeconds(60)
        val id=OrdinaryRequestRegionV8OwnerAuthorization.exactIdentity(args[0],args[1],args[2],args[3],1,"e".repeat(64),"f".repeat(64),correlation,
            ORDINARY_REQUEST_REGION_V8_CAPABILITY_ID,OrdinaryRequestRegionV8Capability().digest(),REQUEST_REGION_V8_AUTHORIZATION_PURPOSE,"OpenAI",REQUEST_REGION_V8_PROVIDER_PROFILE,
            REQUEST_REGION_MODEL,1,0,false,"owner")
        return OrdinaryRequestRegionV8OwnerAuthorization(id,args[0],args[1],ORDINARY_REQUEST_REGION_V8_CAPABILITY_ID,OrdinaryRequestRegionV8Capability().digest(),"OpenAI","owner",now,expires,2,
            args[2],args[3],1,"e".repeat(64),"f".repeat(64),correlation,REQUEST_REGION_V8_AUTHORIZATION_PURPOSE,REQUEST_REGION_V8_PROVIDER_PROFILE,REQUEST_REGION_MODEL,1,0,false)}
    private fun payload(v:Int,id:String,digest:String,wire:Int)=OrdinaryRegionTranscriptionDerivative(v,id,digest,"evidence","a".repeat(64),listOf("page"),listOf("region"),listOf("text"),listOf("region"),listOf("region"),"OpenAI","gpt-5.6-sol","adapter",if(wire==8)"7.0.0" else "4.0.0","profile",wire,"b".repeat(64),"c".repeat(64),"processing","request","d".repeat(64),"response","provider-state","acceptance","authorization","execution","attempt","e".repeat(64),"f".repeat(64),"offline")
    private fun entry(s:String,p:OrdinaryRegionTranscriptionDerivative)=DerivativeContentEntry(DerivativeGenerationId("region-$s"),EvidenceArtifactId("evidence"),TierADerivativePayload.RegionTranscription(p))
    private val dirs=mutableMapOf<String,Path>();private fun dir(n:String)=temp.resolve(n).also{Files.createDirectories(it);dirs[n]=it};private fun dirPath(n:String)=dirs.getValue(n)
    private fun sha(b:ByteArray)=MessageDigest.getInstance("SHA-256").digest(b).joinToString(""){"%02x".format(it.toInt()and 255)}
}
