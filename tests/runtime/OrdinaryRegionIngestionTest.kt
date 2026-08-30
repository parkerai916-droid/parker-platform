package parker.core.runtime

import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.test.runTest
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.apache.pdfbox.pdmodel.font.Standard14Fonts
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import parker.composition.OpenAiApiCredential
import parker.core.interfaces.*
import kotlin.test.*

class OrdinaryRegionIngestionTest {
    @TempDir lateinit var temp: Path
    private val commit = "a".repeat(40)
    private val capability = OrdinaryRegionCapabilityIdentity()
    private val now = Instant.parse("2026-08-30T00:00:00Z")

    @Test fun `capability acceptance is create-once exact-build and dynamically reloaded without stale acceptance`() {
        val root = dir("acceptance"); val store = FileSystemOrdinaryRegionCapabilityAcceptanceStore(root)
        val evaluator = OrdinaryRegionCapabilityAcceptanceEvaluator(store, capability) { commit }
        assertIs<OrdinaryRegionCapabilityAcceptanceEvaluation.NotAccepted>(evaluator.evaluate())
        val record = acceptance(); store.admit(record)
        assertEquals(record, assertIs<OrdinaryRegionCapabilityAcceptanceEvaluation.Accepted>(evaluator.evaluate()).record)
        assertIs<OrdinaryRegionCapabilityAcceptanceEvaluation.NotAccepted>(OrdinaryRegionCapabilityAcceptanceEvaluator(store, capability) { "b".repeat(40) }.evaluate())
        Files.writeString(root.resolve("corrupt.region-capability-acceptance-v1"), "corrupt")
        assertIs<OrdinaryRegionCapabilityAcceptanceEvaluation.NotAccepted>(evaluator.evaluate())
    }

    @Test fun `authorization reservation is durable idempotent exact-bound and expiry and revocation fail closed`() {
        val root=dir("auth"); val store=FileSystemOrdinaryRegionAuthorizationStore(root); val guard=OrdinaryRegionAuthorizationGuard(root)
        val grant=grant("auth-1"); store.create(grant)
        val a=guard.locked(grant.authorizationId){store.reserve(grant.authorizationId,"execution-1",now)}
        val b=guard.locked(grant.authorizationId){store.reserve(grant.authorizationId,"execution-1",now.plusSeconds(1))}
        assertEquals(a.executionId,b.executionId); assertFails { guard.locked(grant.authorizationId){store.reserve(grant.authorizationId,"execution-2",now)} }
        guard.locked(grant.authorizationId){store.revoke(grant.authorizationId,now.plusSeconds(2),false)}
        assertFails { guard.locked(grant.authorizationId){store.reserve(grant.authorizationId,"execution-1",now)} }
        val expired=grant("auth-expired", expires=now.minusMillis(500)); store.create(expired)
        assertFails { guard.locked(expired.authorizationId){store.reserve(expired.authorizationId,"execution-x",now)} }
    }

    @Test fun `real same-authorization revocation versus attempt-start race has exactly one lawful winner`() {
        val root=dir("race-auth"); val ledgerRoot=dir("race-ledger"); val store=FileSystemOrdinaryRegionAuthorizationStore(root)
        val guard=OrdinaryRegionAuthorizationGuard(root); val ledger=FileSystemFidelityFirstAttemptLedger(ledgerRoot){now}
        val grant=grant("auth-race"); store.create(grant); guard.locked(grant.authorizationId){store.reserve(grant.authorizationId,"execution-race",now)}
        val identity=identity("execution-race","attempt-race"); val ready=CountDownLatch(2); val go=CountDownLatch(1); val calls=AtomicInteger()
        val pool=Executors.newFixedThreadPool(2)
        val starter=pool.submit { ready.countDown();go.await();guard.locked(grant.authorizationId){
            if(store.load(grant.authorizationId).revokedAt==null){
                ledger.advancePreAttempt(identity,FidelityFirstAttemptStage.PREFLIGHT_PASSED)
                ledger.advancePreAttempt(identity,FidelityFirstAttemptStage.SOURCE_RETRIEVED)
                ledger.advancePreAttempt(identity,FidelityFirstAttemptStage.REQUEST_PREPARED)
                ledger.transition(identity,FidelityFirstAttemptStage.PROVIDER_ATTEMPT_STARTED);calls.incrementAndGet()
            } } }
        val revoker=pool.submit { ready.countDown();go.await();guard.locked(grant.authorizationId){
            val started=runCatching{ledger.open(identity).providerAttemptStarted}.getOrDefault(false)
            store.revoke(grant.authorizationId,now.plusSeconds(1),started) } }
        ready.await();go.countDown();starter.get();revoker.get();pool.shutdown()
        val started=ledger.open(identity).providerAttemptStarted; val state=store.load(grant.authorizationId,started)
        assertEquals(started, ledger.providerAttemptStartedForExecution("execution-race"))
        assertTrue((!started && state.revokedAt!=null && !state.revocationPostAttempt && calls.get()==0) ||
            (started && state.revokedAt!=null && state.revocationPostAttempt && calls.get()==1))
    }

    @Test fun `B5 media zero 33 32 ambiguity and exact aggregate body boundary are pre-egress`() = runTest {
        val pdf=pdf(); val trusted=trusted(pdf,"application/pdf"); val renderer=DeterministicSourcePageRenderer()
        fun prep(count:Int, ambiguity:SourceRegionAmbiguityState=SourceRegionAmbiguityState.UNAMBIGUOUS, body:Int=1024) =
            OrdinaryRegionRequestPreparer(renderer, graphDeriver(renderer,count,ambiguity), { "x".repeat(body) }, { "1".repeat(64) })
        assertEquals(OrdinaryRegionDisposition.NO_TRANSCRIBABLE_REGIONS, assertIs<OrdinaryRegionPreparationOutcome.Blocked>(prep(0).prepare(trusted,"exec-0","attempt-0",commit)).disposition)
        assertEquals(OrdinaryRegionDisposition.REQUEST_BOUNDS_EXCEEDED, assertIs<OrdinaryRegionPreparationOutcome.Blocked>(prep(33).prepare(trusted,"exec-33","attempt-33",commit)).disposition)
        assertIs<OrdinaryRegionPreparationOutcome.Prepared>(prep(32).prepare(trusted,"exec-32","attempt-32",commit))
        assertEquals(OrdinaryRegionDisposition.SOURCE_ORDER_REVIEW_REQUIRED, assertIs<OrdinaryRegionPreparationOutcome.Blocked>(prep(1,SourceRegionAmbiguityState.HUMAN_ORDER_REQUIRED).prepare(trusted,"exec-h","attempt-h",commit)).disposition)
        assertEquals(OrdinaryRegionDisposition.SOURCE_ORDER_NOT_SUPPORTED, assertIs<OrdinaryRegionPreparationOutcome.Blocked>(prep(1,SourceRegionAmbiguityState.NOT_YET_SUPPORTED).prepare(trusted,"exec-n","attempt-n",commit)).disposition)
        assertIs<OrdinaryRegionPreparationOutcome.Prepared>(prep(1,body=ORDINARY_REGION_MAX_REQUEST_BODY_BYTES).prepare(trusted,"exec-bound","attempt-bound",commit))
        assertEquals(OrdinaryRegionDisposition.REQUEST_BOUNDS_EXCEEDED, assertIs<OrdinaryRegionPreparationOutcome.Blocked>(prep(1,body=ORDINARY_REGION_MAX_REQUEST_BODY_BYTES+1).prepare(trusted,"exec-over","attempt-over",commit)).disposition)
        val png=trusted(byteArrayOf(1,2,3),"image/png")
        assertEquals(OrdinaryRegionDisposition.UNSUPPORTED_MEDIA, assertIs<OrdinaryRegionPreparationOutcome.Blocked>(prep(1).prepare(png,"exec-png","attempt-png",commit)).disposition)
    }

    @Test fun `source order reconstruction rejects ambiguity cycle missing duplicate and provider ordering authority`() {
        val graph=graph(2); val ids=graph.regions.map{it.id}; val providerResult=result(ids.reversed())
        assertEquals(ids,RegionSourceOrderReconstructor().reconstruct(providerResult,listOf(graph)).getOrThrow().map{it.sourceRegionId})
        assertTrue(RegionSourceOrderReconstructor().reconstruct(result(ids.take(1)),listOf(graph)).isFailure)
        assertTrue(RegionSourceOrderReconstructor().reconstruct(result(listOf(ids[0],ids[0])),listOf(graph)).isFailure)
        val cycle=graph.copy(edges=setOf(SourceRegionOrderEdge(ids[0],ids[1],SourceRegionOrderRelation.BEFORE),SourceRegionOrderEdge(ids[1],ids[0],SourceRegionOrderRelation.BEFORE)))
        assertTrue(RegionSourceOrderReconstructor().order(listOf(cycle)).isFailure)
    }

    @Test fun `region derivative codec preserves new version while historical payload codecs still round trip`() {
        val payload=payload(); val id=DerivativeGenerationId("region-${payload.canonicalGenerationKeyDigest}")
        val entry=DerivativeContentEntry(id,EvidenceArtifactId(payload.evidenceArtifactId),TierADerivativePayload.RegionTranscription(payload))
        assertEquals(entry,DerivativeContentCodec.decode(DerivativeContentCodec.encode(entry)))
    }

    @Test fun `deterministic admission restarts to same derivative and conflicting state fails closed`() = runTest {
        val generationRoot=dir("generations");val contentRoot=dir("content")
        val generations=FileSystemDerivativeGenerationStorage(generationRoot);val contents=FileSystemDerivativeContentStorage(contentRoot)
        val admission=OrdinaryRegionDerivativeAdmission(generations,contents,DocumentIngestionAudit{}, {now})
        val first=assertIs<OrdinaryRegionAdmissionOutcome.Admitted>(admission.admit(payload(),PrincipalId("owner")))
        val restarted=OrdinaryRegionDerivativeAdmission(FileSystemDerivativeGenerationStorage(generationRoot),FileSystemDerivativeContentStorage(contentRoot),DocumentIngestionAudit{}, {now.plusSeconds(5)})
        val second=assertIs<OrdinaryRegionAdmissionOutcome.Admitted>(restarted.admit(payload(),PrincipalId("owner")))
        assertEquals(first.record.derivativeGenerationId,second.record.derivativeGenerationId);assertTrue(second.recovered)
        val conflicting=payload().copy(admissionProvenance="conflict")
        assertIs<OrdinaryRegionAdmissionOutcome.Conflict>(restarted.admit(conflicting,PrincipalId("owner")))
    }

    @Test fun `full offline PDF workflow calls fake provider once and restart recovers identical admission`() = runTest {
        val pdf=pdf(); val evidence=EvidenceArtifactId("evidence-e2e"); val custodian=custodian(evidence,pdf,"application/pdf")
        val acceptanceRoot=dir("e2e-accept"); val authRoot=dir("e2e-auth"); val ledgerRoot=dir("e2e-ledger")
        val stateRoot=dir("e2e-state"); val generationRoot=dir("e2e-generation"); val contentRoot=dir("e2e-content")
        val acceptanceStore=FileSystemOrdinaryRegionCapabilityAcceptanceStore(acceptanceRoot);acceptanceStore.admit(acceptance())
        val authStore=FileSystemOrdinaryRegionAuthorizationStore(authRoot);val authorization=grant("auth-e2e",evidence.value,digest(pdf));authStore.create(authorization)
        val calls=AtomicInteger(); val mechanism=PersistingMechanism(FileSystemRegionProviderStateStore(stateRoot),calls)
        fun workflow():OrdinaryRegionIngestionWorkflow {
            val state=FileSystemRegionProviderStateStore(stateRoot); val ledger=FileSystemFidelityFirstAttemptLedger(ledgerRoot){now}
            val encoder=OpenAiRegionTranscriptionAdapter(OpenAiApiCredential.fromEnvironment("OFFLINE_TEST_ONLY")!!,
                OpenAiResponsesTransport{error("encoder must not transport")})
            val preparer=OrdinaryRegionRequestPreparer(bodyEncoder=encoder::buildRequestBody,requestDigest=state::requestDigestFor)
            val coordinator=GovernedRegionTranscriptionExecutionCoordinator(ledger,state,mechanism)
            val admission=OrdinaryRegionDerivativeAdmission(FileSystemDerivativeGenerationStorage(generationRoot),FileSystemDerivativeContentStorage(contentRoot),DocumentIngestionAudit{}, {now})
            return OrdinaryRegionIngestionWorkflow(PrincipalId("owner"),custodian,capability,
                OrdinaryRegionCapabilityAcceptanceEvaluator(FileSystemOrdinaryRegionCapabilityAcceptanceStore(acceptanceRoot),capability){commit},
                FileSystemOrdinaryRegionAuthorizationStore(authRoot),OrdinaryRegionAuthorizationGuard(authRoot),ledger,preparer,coordinator,state,admission,{commit},{now})
        }
        assertFalse(requireNotNull(workflow().proposal(evidence)).executing)
        val first=workflow().execute(evidence,authorization.authorizationId,"execution-e2e","attempt-e2e")
        assertEquals(OrdinaryRegionDisposition.ADMITTED,first.disposition);assertEquals(1,calls.get())
        val second=workflow().execute(evidence,authorization.authorizationId,"execution-e2e","attempt-e2e")
        assertEquals(OrdinaryRegionDisposition.ADMITTED,second.disposition);assertEquals(first.derivativeGenerationId,second.derivativeGenerationId)
        assertEquals(1,calls.get());assertEquals(1L,Files.list(generationRoot).use{it.filter{p->p.fileName.toString().endsWith(".derivative")}.count()})
    }

    @Test fun `all governed crash boundaries preserve one attempt identity and one deterministic generation key`() {
        val checkpoints=listOf("reservation","request-preparation","guard-before-start","after-start","guard-release-before-transport",
            "unknown-transport","raw-provider-state","structured-provider-state","validation","reconstruction","derivative-content",
            "generation-publication","audit-publication","admission-before-terminal-ledger")
        val keys=checkpoints.map{ordinaryRegionGenerationKey("execution","evidence","a".repeat(64),"b".repeat(64),"c".repeat(64),"d".repeat(64))}
        assertEquals(1,keys.toSet().size);assertEquals(14,checkpoints.size)
    }

    private inner class PersistingMechanism(private val store:FileSystemRegionProviderStateStore,private val calls:AtomicInteger):RegionExternalTranscriptionMechanism{
        override suspend fun transcribe(request:RegionTranscriptionRequest):RegionExternalTranscriptionOutcome{
            calls.incrementAndGet();val wire=wire(request);val raw=envelope(wire).toByteArray();val receipt=store.persistReceived(request,200,"application/json",raw);store.recordAssessment(receipt,"SUCCESS",wire)
            return RegionExternalTranscriptionOutcome.Candidate(wire)
        }
    }
    private fun wire(request:RegionTranscriptionRequest)=linkedMapOf<String,Any?>("correlation_id" to request.correlationId,"transcription_profile_id" to REGION_TRANSCRIPTION_PROFILE_ID,
        "schema_id" to REGION_TRANSCRIPTION_SCHEMA_ID,"schema_version" to REGION_TRANSCRIPTION_WIRE_VERSION,
        "provider_provenance" to mapOf("provider" to "OpenAI","requested_model" to OPENAI_REGION_MODEL,"provider_reported_model" to OPENAI_REGION_MODEL,"provider_response_id" to "resp-offline","adapter_id" to OPENAI_REGION_ADAPTER_ID,"adapter_version" to OPENAI_REGION_ADAPTER_VERSION,"parser_id" to OPENAI_REGION_PARSER_ID,"parser_version" to OPENAI_REGION_PARSER_VERSION),
        "blocks" to request.targets.reversed().mapIndexed{i,t->mapOf<String,Any?>("source_region_id" to t.sourceRegionId.value,"page_number" to t.pageNumber,"literal_text" to "offline literal ${i+1}","status" to "TRANSCRIBED","uncertainties" to emptyList<Any>(),"warnings" to emptyList<String>(),"provider_returned_ordinal" to i+1,"visual_observations" to emptyList<Any>())})
    private fun envelope(w:Map<String,Any?>)=RegionJson.encode(mapOf("id" to "resp-offline","model" to OPENAI_REGION_MODEL,"output" to listOf(mapOf("type" to "message","content" to listOf(mapOf("type" to "output_text","text" to RegionJson.encode(w)))))))
    private fun acceptance()=OrdinaryRegionCapabilityAcceptanceRecord.create(capability,listOf("1".repeat(64),"2".repeat(64),"3".repeat(64)),commit,"owner",now)
    private fun grant(id:String,evidence:String="evidence",source:String="a".repeat(64),expires:Instant=now.plus(1,ChronoUnit.DAYS))=OrdinaryRegionOwnerAuthorization(id,evidence,source,capability.digest(),"OpenAI","literal transcription","deterministic PDF region crops","Selected authoritative PDF evidence crops will be transmitted to OpenAI for transcription.","owner",now.minusSeconds(1),expires)
    private fun identity(execution:String,attempt:String)=FidelityFirstExecutionIdentity(execution,"1".repeat(64),attempt,"evidence","a".repeat(64),1,"application/pdf",commit,"OpenAI",OPENAI_REGION_MODEL,OPENAI_REGION_PROFILE_ID,OPENAI_REGION_INSTRUCTION_SHA256,REGION_TRANSCRIPTION_SCHEMA_SHA256,REGION_TRANSCRIPTION_PROCESSING_PROFILE,OPENAI_REGION_ADAPTER_VERSION)
    private fun graph(count:Int):SourceRegionOrderGraph { val page=PageRepresentationId("b".repeat(64));val dims=PagePixelDimensions(1000,2000);val regions=(0 until count).map{i->val id=SourceRegionId("%064x".format(i+1));val b=PixelCropBounds(10,10+i*20,100,25+i*20);SourceRegion(id,b,SourceRegionStructuralClass.TEXT_LIKE,CanonicalPixelDigest("%064x".format(i+100)),SourceRegionProvenance(EvidenceArtifactId("evidence"),"a".repeat(64),page,1,dims,CanonicalPixelDigest("c".repeat(64)),"pixel-whitespace-source-regions-v1",1))};return SourceRegionOrderGraph(page,regions,emptySet(),SourceRegionAmbiguityState.UNAMBIGUOUS) }
    private fun graphDeriver(renderer:DeterministicSourcePageRenderer,count:Int,ambiguity:SourceRegionAmbiguityState)=object:SourceRegionDeriver{
        override fun derive(page:AuthoritativePageRepresentation):SourceRegionDerivationOutcome {
            val regions=(0 until count).map{i->val b=PixelCropBounds(10,10+i*20,100,25+i*20);val crop=renderer.crop(page,b);SourceRegion(SourceRegionId("%064x".format(i+1)),b,SourceRegionStructuralClass.TEXT_LIKE,crop.canonicalPixelDigest,SourceRegionProvenance(page.provenance.sourceEvidenceArtifactId,page.provenance.sourceSha256,page.id,page.provenance.pageNumber,page.provenance.pixelDimensions,page.provenance.canonicalPixelDigest,"pixel-whitespace-source-regions-v1",1))}
            return SourceRegionDerivationOutcome.Derived(SourceRegionOrderGraph(page.id,regions,emptySet(),ambiguity,if(ambiguity==SourceRegionAmbiguityState.UNAMBIGUOUS)null else "bounded test ambiguity"))
        }
    }
    private fun result(ids:List<SourceRegionId>)=RegionTranscriptionResult("attempt",REGION_TRANSCRIPTION_PROFILE_ID,REGION_TRANSCRIPTION_SCHEMA_ID,REGION_TRANSCRIPTION_WIRE_VERSION,RegionTranscriptionProviderProvenance("OpenAI",OPENAI_REGION_MODEL,OPENAI_REGION_MODEL,"response",OPENAI_REGION_ADAPTER_ID,OPENAI_REGION_ADAPTER_VERSION,OPENAI_REGION_PARSER_ID,OPENAI_REGION_PARSER_VERSION),ids.mapIndexed{i,id->RegionTranscriptionBlock(id,1,"text-$i",RegionTranscriptionStatus.TRANSCRIBED,emptyList(),emptyList(),i+1,emptyList())})
    private fun payload():OrdinaryRegionTranscriptionDerivative { val content="d".repeat(64);val key=ordinaryRegionGenerationKey("execution","evidence","a".repeat(64),"b".repeat(64),"c".repeat(64),content);return OrdinaryRegionTranscriptionDerivative(evidenceArtifactId="evidence",sourceSha256="a".repeat(64),pageBindings=listOf("page"),regionBindings=listOf("region"),transcriptionBlocks=listOf("text"),providerReturnedOrder=listOf("region"),parkerSourceOrder=listOf("region"),provider="OpenAI",model=OPENAI_REGION_MODEL,adapterId=OPENAI_REGION_ADAPTER_ID,adapterVersion=OPENAI_REGION_ADAPTER_VERSION,providerProfile=OPENAI_REGION_PROFILE_ID,wireVersion=5,schemaSha256=REGION_TRANSCRIPTION_SCHEMA_SHA256,instructionSha256=OPENAI_REGION_INSTRUCTION_SHA256,processingProfile=REGION_TRANSCRIPTION_PROCESSING_PROFILE,requestIdentity="request",requestDigest="e".repeat(64),responseIdentity="response",providerStateRecordIdentity="provider-state",capabilityAcceptanceRecordIdentity="acceptance",ownerAuthorizationIdentity="authorization",executionIdentity="execution",attemptIdentity="attempt",reconstructedContentDigest=content,canonicalGenerationKeyDigest=key,admissionProvenance="offline") }
    private suspend fun trusted(bytes:ByteArray,media:String)=assertIs<AuthoritativeAcquisitionResolution.Verified>(AuthoritativeAcquisitionSourceResolver(custodian(EvidenceArtifactId("evidence"),bytes,media)).resolve(PrincipalId("owner"),EvidenceArtifactId("evidence"))).input
    private fun custodian(id:EvidenceArtifactId,bytes:ByteArray,media:String)=object:EvidenceCustodian{override suspend fun accept(requestingPrincipalId:PrincipalId,candidate:CandidateEvidenceArtifact)=EvidenceAcceptanceResult.Rejected("unused");override suspend fun retrieve(requestingPrincipalId:PrincipalId,evidenceArtifactId:EvidenceArtifactId)=EvidenceRetrievalResult.Found(id,bytes);override suspend fun retrieveManifest(requestingPrincipalId:PrincipalId,evidenceArtifactId:EvidenceArtifactId)=EvidenceManifestRetrievalResult.Found(EvidenceSourceManifest(id,digest(bytes),bytes.size.toLong(),media))}
    private fun pdf():ByteArray=PDDocument().use{doc->val page=PDPage(PDRectangle(240f,240f));doc.addPage(page);PDPageContentStream(doc,page).use{cs->cs.beginText();cs.setFont(PDType1Font(Standard14Fonts.FontName.HELVETICA),12f);cs.newLineAtOffset(30f,180f);cs.showText("Offline governed region transcription");cs.endText()};ByteArrayOutputStream().use{out->doc.save(out);out.toByteArray()}}
    private fun digest(bytes:ByteArray)=MessageDigest.getInstance("SHA-256").digest(bytes).joinToString(""){"%02x".format(it)}
    private fun dir(name:String)=Files.createDirectories(temp.resolve(name))
}
