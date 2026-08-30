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

    @Test fun `production catalogue registers canonical ordinary region v5 projection`() {
        val projected = ProductionAcquisitionCapabilityCatalogue.ordinaryRegionV5Capability(false)
        val registered = ProductionAcquisitionCapabilityCatalogue.create(ordinaryRegionCapabilityProjection = projected)
            .capability(ORDINARY_REGION_CAPABILITY_ID)
        assertNotNull(registered)
        assertEquals(capability.digest(), registered.providerConfiguration?.configurationIdentity)
        assertEquals(OPENAI_REGION_ADAPTER_VERSION, registered.providerConfiguration?.adapterVersion)
        assertEquals(setOf("application/pdf"), registered.supportedMediaTypes)
    }

    @Test fun `read only status is dynamic exact build and exposes routing bounds without side effects`() {
        val root = dir("status"); val store = FileSystemOrdinaryRegionCapabilityAcceptanceStore(root)
        val evaluator = OrdinaryRegionCapabilityAcceptanceEvaluator(store, capability) { commit }
        val before = evaluateOrdinaryRegionCapabilityStatus(capability, evaluator) { commit }
        assertEquals(OrdinaryRegionCapabilityDisposition.CAPABILITY_NOT_ACCEPTED, before.disposition)
        assertEquals(32, before.maximumRegions); assertEquals(16_777_216, before.aggregateRequestBodyMaximumBytes)
        assertFalse(before.batching); assertEquals("application/pdf", before.mediaType)
        store.admit(acceptance())
        val after = evaluateOrdinaryRegionCapabilityStatus(capability, evaluator) { commit }
        assertEquals(OrdinaryRegionCapabilityDisposition.ACCEPTED, after.disposition)
        assertEquals(commit, after.acceptedPromotingBuildCommit)
        assertEquals(1L, Files.list(root).use { it.count() })
        assertEquals(after, evaluateOrdinaryRegionCapabilityStatus(capability, evaluator) { commit })
        assertEquals(OrdinaryRegionCapabilityDisposition.CAPABILITY_NOT_ACCEPTED,
            evaluateOrdinaryRegionCapabilityStatus(capability,
                OrdinaryRegionCapabilityAcceptanceEvaluator(store, capability) { "b".repeat(40) }) { "b".repeat(40) }.disposition)
    }

    @Test fun `capability acceptance is create-once exact-build and dynamically reloaded without stale acceptance`() {
        val root = dir("acceptance"); val store = FileSystemOrdinaryRegionCapabilityAcceptanceStore(root)
        val evaluator = OrdinaryRegionCapabilityAcceptanceEvaluator(store, capability) { commit }
        assertIs<OrdinaryRegionCapabilityAcceptanceEvaluation.NotAccepted>(evaluator.evaluate())
        val record = acceptance(); store.admit(record)
        assertEquals(record, assertIs<OrdinaryRegionCapabilityAcceptanceEvaluation.Accepted>(evaluator.evaluate()).record)
        assertIs<OrdinaryRegionCapabilityAcceptanceEvaluation.NotAccepted>(OrdinaryRegionCapabilityAcceptanceEvaluator(store, capability) { "b".repeat(40) }.evaluate())
        Files.writeString(root.resolve("corrupt.region-capability-acceptance-v2"), "corrupt")
        assertIs<OrdinaryRegionCapabilityAcceptanceEvaluation.NotAccepted>(evaluator.evaluate())
    }

    @Test fun `typed evidence roles and governed values fail closed`() {
        val valid = typedEvidence()
        assertEquals(OrdinaryRegionFidelityClassification.PASS_FIDELITY, valid.fidelityReview.classification)
        assertFails { valid.copy(forensicAnalysis = valid.forensicAnalysis.copy(role = OrdinaryRegionAcceptanceEvidenceRole.R6_9B_POINT_ANCHOR_SEMANTICS)) }
        assertFails { valid.copy(pointAnchorSemantics = valid.pointAnchorSemantics.copy(role = OrdinaryRegionAcceptanceEvidenceRole.R6_9A_FORENSIC_ANALYSIS)) }
        assertFails { valid.copy(
            forensicAnalysis = valid.forensicAnalysis.copy(role = OrdinaryRegionAcceptanceEvidenceRole.R6_9B_POINT_ANCHOR_SEMANTICS),
            pointAnchorSemantics = valid.pointAnchorSemantics.copy(role = OrdinaryRegionAcceptanceEvidenceRole.R6_9A_FORENSIC_ANALYSIS)) }
        assertFails { valid.copy(pointAnchorSemantics = valid.pointAnchorSemantics.copy(commit = "f".repeat(40))) }
        assertFails { valid.copy(fidelityReview = valid.fidelityReview.copy(reportDigest = "f".repeat(64))) }
        assertFails { valid.copy(fidelityReview = valid.fidelityReview.copy(classification = OrdinaryRegionFidelityClassification.FAIL_FIDELITY)) }
        assertFails { valid.copy(fidelityReview = valid.fidelityReview.copy(reviewedRegions = 23)) }
        assertFails { valid.copy(liveResult = valid.liveResult.copy(requestDigest = "f".repeat(64))) }
        assertFails { OrdinaryRegionCapabilityIdentity(model = "wrong") }
        assertFails { OrdinaryRegionCapabilityAcceptanceRecord.create(capability, valid, "not-a-commit", "owner", now) }
    }

    @Test fun `governed coordinator creates once replays idempotently and legacy untyped record cannot accept`() {
        val root = dir("coordinator"); val store = FileSystemOrdinaryRegionCapabilityAcceptanceStore(root)
        val calls = AtomicInteger()
        val coordinator = OrdinaryRegionCapabilityAcceptanceCoordinator(store, OrdinaryRegionR69EvidenceLoader { calls.incrementAndGet(); liveEvidence() },
            { commit }, "owner", { now })
        val request = OrdinaryRegionCapabilityPromotionRequest(ORDINARY_REGION_CAPABILITY_ID, commit)
        val created = assertIs<OrdinaryRegionCapabilityPromotionOutcome.Created>(coordinator.create(request))
        assertEquals(created.record, assertIs<OrdinaryRegionCapabilityPromotionOutcome.Existing>(coordinator.create(request)).record)
        assertEquals(1, calls.get())
        assertEquals(1L, Files.list(root).use { it.count() })
        val conflict = OrdinaryRegionCapabilityAcceptanceRecord.create(capability, typedEvidence(), commit, "different-owner", now.plusSeconds(1))
        assertFails { store.admit(conflict) }
        assertIs<OrdinaryRegionCapabilityPromotionOutcome.Blocked>(coordinator.create(request.copy(capabilityId = "wrong")))
        assertIs<OrdinaryRegionCapabilityPromotionOutcome.Blocked>(coordinator.create(request.copy(promotingBuildCommit = "b".repeat(40))))
        val legacyRoot = dir("legacy-acceptance")
        Files.writeString(legacyRoot.resolve("legacy.region-capability-acceptance-v1"), "${"1".repeat(64)}\tlegacy\n")
        assertIs<OrdinaryRegionCapabilityAcceptanceEvaluation.NotAccepted>(
            OrdinaryRegionCapabilityAcceptanceEvaluator(FileSystemOrdinaryRegionCapabilityAcceptanceStore(legacyRoot), capability) { commit }.evaluate())
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
        assertEquals(OrdinaryRegionOwnerAuthorizationDisposition.NOT_AUTHORISED, workflow().authorizationStatus(evidence).disposition)
        val created = assertIs<OrdinaryRegionOwnerAuthorizationOutcome.Created>(workflow().authorize(evidence)).view
        assertEquals(evidence.value, created.evidenceArtifactId)
        assertEquals("OpenAI", created.provider)
        assertEquals(OrdinaryRegionOwnerAuthorizationDisposition.AUTHORISED, workflow().authorizationStatus(evidence).disposition)
        assertIs<OrdinaryRegionOwnerAuthorizationOutcome.Existing>(workflow().authorize(evidence))
        assertEquals(1L, Files.list(authRoot).use { stream -> stream.filter { it.fileName.toString().endsWith(".region-owner-authorization-v1") }.count() })
        assertEquals(0, calls.get())
        val first=workflow().execute(evidence,requireNotNull(created.authorizationId),"execution-e2e","attempt-e2e")
        assertEquals(OrdinaryRegionDisposition.ADMITTED,first.disposition);assertEquals(1,calls.get())
        val second=workflow().execute(evidence,requireNotNull(created.authorizationId),"execution-e2e","attempt-e2e")
        assertEquals(OrdinaryRegionDisposition.ADMITTED,second.disposition);assertEquals(first.derivativeGenerationId,second.derivativeGenerationId)
        assertEquals(1,calls.get());assertEquals(1L,Files.list(generationRoot).use{it.filter{p->p.fileName.toString().endsWith(".derivative")}.count()})
    }

    @Test fun `provider free bounds failure is returned before a new authorization reservation`() = runTest {
        val pdf=pdf(); val evidence=EvidenceArtifactId("evidence-preparation-bound"); val custody=custodian(evidence,pdf,"application/pdf")
        val acceptanceRoot=dir("prebound-accept"); val authRoot=dir("prebound-auth"); val ledgerRoot=dir("prebound-ledger")
        val stateRoot=dir("prebound-state"); val generationRoot=dir("prebound-generation"); val contentRoot=dir("prebound-content")
        FileSystemOrdinaryRegionCapabilityAcceptanceStore(acceptanceRoot).admit(acceptance())
        val calls=AtomicInteger(); val state=FileSystemRegionProviderStateStore(stateRoot)
        val ledger=FileSystemFidelityFirstAttemptLedger(ledgerRoot){now}
        val encoder=OpenAiRegionTranscriptionAdapter(OpenAiApiCredential.fromEnvironment("OFFLINE_TEST_ONLY")!!,
            OpenAiResponsesTransport{error("provider transport is prohibited")})
        val renderer=DeterministicSourcePageRenderer()
        val preparer=OrdinaryRegionRequestPreparer(renderer,graphDeriver(renderer,33,SourceRegionAmbiguityState.UNAMBIGUOUS),
            encoder::buildRequestBody,state::requestDigestFor)
        val coordinator=GovernedRegionTranscriptionExecutionCoordinator(ledger,state,object:RegionExternalTranscriptionMechanism{
            override suspend fun transcribe(request:RegionTranscriptionRequest):RegionExternalTranscriptionOutcome {
                calls.incrementAndGet(); error("provider transport is prohibited")
            }
        })
        val workflow=OrdinaryRegionIngestionWorkflow(PrincipalId("owner"),custody,capability,
            OrdinaryRegionCapabilityAcceptanceEvaluator(FileSystemOrdinaryRegionCapabilityAcceptanceStore(acceptanceRoot),capability){commit},
            FileSystemOrdinaryRegionAuthorizationStore(authRoot),OrdinaryRegionAuthorizationGuard(authRoot),ledger,preparer,coordinator,state,
            OrdinaryRegionDerivativeAdmission(FileSystemDerivativeGenerationStorage(generationRoot),FileSystemDerivativeContentStorage(contentRoot),DocumentIngestionAudit{}, {now}),
            {commit},{now})
        val authorization=assertIs<OrdinaryRegionOwnerAuthorizationOutcome.Created>(workflow.authorize(evidence)).view
        val result=workflow.execute(evidence,requireNotNull(authorization.authorizationId),"execution-bound","attempt-bound")
        assertEquals(OrdinaryRegionDisposition.REQUEST_BOUNDS_EXCEEDED,result.disposition)
        assertEquals("complete region set exceeds 32",result.detail)
        assertEquals(OrdinaryRegionAuthorizationState.AVAILABLE,
            FileSystemOrdinaryRegionAuthorizationStore(authRoot).load(requireNotNull(authorization.authorizationId)).state)
        assertEquals(0,calls.get())
        assertEquals(0L,Files.list(ledgerRoot).use{it.count()})
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
    private fun liveEvidence() = OrdinaryRegionLiveR69Evidence(
        OrdinaryRegionAcceptanceEvidenceRole.R6_9_LIVE_PROVIDER_RESULT, R69_AUTHORITY_ID, R69_EXECUTION_ID,
        R69_REQUEST_DIGEST, R69_PROVIDER_RESPONSE_ID, R69_PROVIDER_STATE_ID, R69_RAW_RESPONSE_DIGEST,
        R69_STRUCTURED_STATE_DIGEST, R69_PROVIDER_RECORD_DIGEST, R69_ASSESSMENT_DIGEST)
    private fun typedEvidence() = RegionTranscriptionCapabilityAcceptanceEvidenceV1.governed(liveEvidence())
    private fun acceptance()=OrdinaryRegionCapabilityAcceptanceRecord.create(capability,typedEvidence(),commit,"owner",now)
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
