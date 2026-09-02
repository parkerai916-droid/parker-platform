package parker.core.runtime

import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.security.MessageDigest
import java.time.Instant
import java.util.Base64
import java.net.URI
import parker.composition.OpenAiApiCredential
import parker.core.interfaces.*

const val ORDINARY_REQUEST_REGION_V8_ACCEPTANCE_EVIDENCE_ID = "acceptance-evidence-ordinary-ingestion-10r7-v8-fidelity"
const val ORDINARY_REQUEST_REGION_V8_ACCEPTANCE_EVIDENCE_SHA256 = "34ec3c703aacb754c45fa58ddf941d7368e2b4cc2e373cb412eb99c4de30902b"
const val REQUEST_REGION_V8_PROVIDER_STATE_FORMAT = "request-region-v8-provider-state-v1"
const val REQUEST_REGION_V8_PROVIDER_ASSESSMENT_FORMAT = "request-region-v8-provider-assessment-v1"
const val REQUEST_REGION_V8_EXACT_AUTHORIZATION_FORMAT = 2
val REQUEST_REGION_V8_AUTHORIZATION_PURPOSE = ExternalTranscriptionInvocationGate.AUTHORIZATION_PURPOSE.value
const val REQUEST_REGION_V8_PROVIDER_PROFILE = "openai-fidelity-first-transcription-v1"

data class OrdinaryRequestRegionV8CapabilityIdentity(
    val capabilityId:String=ORDINARY_REQUEST_REGION_V8_CAPABILITY_ID,
    val capabilityDigest:String=OrdinaryRequestRegionV8Capability().digest(),
    val provider:String=REQUEST_REGION_PROVIDER,
    val endpointOperation:String="POST /v1/responses",
    val model:String=REQUEST_REGION_MODEL,
    val profile:String=REQUEST_REGION_V8_PROFILE_ID,
    val schema:String=REQUEST_REGION_V8_SCHEMA_ID,
    val wireVersion:Int=REQUEST_REGION_V8_WIRE_VERSION,
    val adapterId:String=REQUEST_REGION_ADAPTER_ID,
    val adapterVersion:String=REQUEST_REGION_V8_ADAPTER_VERSION,
    val parserId:String=REQUEST_REGION_PARSER_ID,
    val parserVersion:String=REQUEST_REGION_V8_PARSER_VERSION,
    val processing:String=REQUEST_REGION_V8_PROCESSING_PROFILE,
    val instructionSha256:String=REQUEST_REGION_V8_INSTRUCTION_SHA256,
    val schemaSha256:String=REQUEST_REGION_V8_SCHEMA_SHA256,
    val reasoning:String="none",
    val store:Boolean=false,
    val maximumRegions:Int=REQUEST_REGION_MAXIMUM,
    val maximumBodyBytes:Int=REQUEST_REGION_BODY_MAXIMUM_BYTES,
) {
    init {
        require(capabilityId==ORDINARY_REQUEST_REGION_V8_CAPABILITY_ID&&capabilityDigest=="c0479979720455d2de3fc9861eeb5dee323a4770bdb15f807af611ad426f9ec0")
        require(provider=="OpenAI"&&endpointOperation=="POST /v1/responses"&&model=="gpt-5.6-sol")
        require(profile==REQUEST_REGION_V8_PROFILE_ID&&schema==REQUEST_REGION_V8_SCHEMA_ID&&wireVersion==8)
        require(adapterId==REQUEST_REGION_ADAPTER_ID&&adapterVersion=="7.0.0"&&parserId==REQUEST_REGION_PARSER_ID&&parserVersion=="3.0.0")
        require(processing==REQUEST_REGION_V8_PROCESSING_PROFILE&&instructionSha256==REQUEST_REGION_V8_INSTRUCTION_SHA256&&schemaSha256==REQUEST_REGION_V8_SCHEMA_SHA256)
        require(reasoning=="none"&&!store&&maximumRegions==32&&maximumBodyBytes==16_777_216)
    }
}

data class OrdinaryRequestRegionV8CapabilityAcceptanceRecord(
    val recordId:String,val capabilityId:String,val capabilityDigest:String,val implementationCommit:String,
    val evidenceId:String,val evidenceSha256:String,val acceptedBy:String,val acceptedAt:Instant,
) {
    init {
        require(capabilityId==ORDINARY_REQUEST_REGION_V8_CAPABILITY_ID)
        require(capabilityDigest==OrdinaryRequestRegionV8Capability().digest())
        require(implementationCommit.matches(Regex("^[0-9a-f]{40}$")))
        require(evidenceId==ORDINARY_REQUEST_REGION_V8_ACCEPTANCE_EVIDENCE_ID&&evidenceSha256==ORDINARY_REQUEST_REGION_V8_ACCEPTANCE_EVIDENCE_SHA256)
        require(recordId==identity(capabilityId,capabilityDigest,implementationCommit,evidenceId,evidenceSha256,acceptedBy,acceptedAt))
    }
    companion object {
        fun create(commit:String,by:String,at:Instant):OrdinaryRequestRegionV8CapabilityAcceptanceRecord {
            val c=OrdinaryRequestRegionV8Capability().digest()
            return OrdinaryRequestRegionV8CapabilityAcceptanceRecord(identity(ORDINARY_REQUEST_REGION_V8_CAPABILITY_ID,c,commit,
                ORDINARY_REQUEST_REGION_V8_ACCEPTANCE_EVIDENCE_ID,ORDINARY_REQUEST_REGION_V8_ACCEPTANCE_EVIDENCE_SHA256,by,at),
                ORDINARY_REQUEST_REGION_V8_CAPABILITY_ID,c,commit,ORDINARY_REQUEST_REGION_V8_ACCEPTANCE_EVIDENCE_ID,
                ORDINARY_REQUEST_REGION_V8_ACCEPTANCE_EVIDENCE_SHA256,by,at)
        }
        private fun identity(vararg f:Any)=v8ExecutionDigest("parker.request-region-v8.capability-acceptance.v1",*f)
    }
}

class FileSystemOrdinaryRequestRegionV8CapabilityAcceptanceStore(root:Path) {
    private val root=root.toAbsolutePath().normalize()
    init { require(Files.isDirectory(this.root)&&Files.isReadable(this.root)&&Files.isWritable(this.root)) }
    fun admit(r:OrdinaryRequestRegionV8CapabilityAcceptanceRecord)=v8CreateOnce(path(r.recordId),encode(r))
    fun findExact(commit:String):OrdinaryRequestRegionV8CapabilityAcceptanceRecord? {
        val records=Files.list(root).use{it.filter{p->p.fileName.toString().endsWith(".request-region-v8-capability-acceptance-v1")}.map{p->decode(Files.readString(p))}.toList()}
        return records.filter{it.implementationCommit==commit&&it.capabilityDigest==OrdinaryRequestRegionV8Capability().digest()}.singleOrNull()
            .also{require(records.count{x->x.implementationCommit==commit&&x.capabilityDigest==OrdinaryRequestRegionV8Capability().digest()}<=1)}
    }
    private fun encode(r:OrdinaryRequestRegionV8CapabilityAcceptanceRecord):String {
        val fields=listOf(r.recordId,r.capabilityId,r.capabilityDigest,r.implementationCommit,r.evidenceId,r.evidenceSha256,
            v8B64(r.acceptedBy),r.acceptedAt.toString());val body=fields.joinToString("\t");return "$body\t${v8ExecutionDigest(body)}\n"
    }
    private fun decode(s:String):OrdinaryRequestRegionV8CapabilityAcceptanceRecord {
        require(s.endsWith('\n'));val p=s.trimEnd().split('\t');require(p.size==9);val body=p.take(8).joinToString("\t");require(p[8]==v8ExecutionDigest(body))
        return OrdinaryRequestRegionV8CapabilityAcceptanceRecord(p[0],p[1],p[2],p[3],p[4],p[5],v8Unb64(p[6]),Instant.parse(p[7]))
    }
    private fun path(id:String)=root.resolve("$id.request-region-v8-capability-acceptance-v1").normalize().also{require(it.parent==root)}
}

sealed interface OrdinaryRequestRegionV8AcceptanceEvaluation { data class Accepted(val record:OrdinaryRequestRegionV8CapabilityAcceptanceRecord):OrdinaryRequestRegionV8AcceptanceEvaluation;data object NotAccepted:OrdinaryRequestRegionV8AcceptanceEvaluation }
class OrdinaryRequestRegionV8AcceptanceEvaluator(private val store:FileSystemOrdinaryRequestRegionV8CapabilityAcceptanceStore,private val commit:()->String?) {
    fun evaluate():OrdinaryRequestRegionV8AcceptanceEvaluation=try{commit()?.takeIf{it.matches(Regex("^[0-9a-f]{40}$"))}?.let(store::findExact)?.let(OrdinaryRequestRegionV8AcceptanceEvaluation::Accepted)?:OrdinaryRequestRegionV8AcceptanceEvaluation.NotAccepted}catch(_:Exception){OrdinaryRequestRegionV8AcceptanceEvaluation.NotAccepted}
}
class OrdinaryRequestRegionV8CapabilityAcceptanceCoordinator(private val store:FileSystemOrdinaryRequestRegionV8CapabilityAcceptanceStore,
    private val runtimeCommit:()->String?,private val acceptedBy:String,private val now:()->Instant=Instant::now):OrdinaryRegionCapabilityPromotionPort {
    override fun create(request:OrdinaryRegionCapabilityPromotionRequest):OrdinaryRegionCapabilityPromotionOutcome {
        if(request.capabilityId!=ORDINARY_REQUEST_REGION_V8_CAPABILITY_ID)return OrdinaryRegionCapabilityPromotionOutcome.Blocked("CAPABILITY_ID_MISMATCH")
        if(runtimeCommit()!=request.promotingBuildCommit)return OrdinaryRegionCapabilityPromotionOutcome.Blocked("BUILD_COMMIT_MISMATCH")
        return try{store.findExact(request.promotingBuildCommit)?.let{return OrdinaryRegionCapabilityPromotionOutcome.V8Existing(it)}
            val record=OrdinaryRequestRegionV8CapabilityAcceptanceRecord.create(request.promotingBuildCommit,acceptedBy,now());store.admit(record);OrdinaryRegionCapabilityPromotionOutcome.V8Created(record)
        }catch(_:Exception){OrdinaryRegionCapabilityPromotionOutcome.Blocked("V8_ACCEPTANCE_CREATE_CONFLICT")}
    }
}

data class OrdinaryRequestRegionV8OwnerAuthorization(
    val authorizationId:String,val evidenceArtifactId:String,val sourceSha256:String,val capabilityId:String,
    val capabilityDigest:String,val provider:String,val approvedBy:String,val approvedAt:Instant,val expiresAt:Instant,
    val formatVersion:Int=1,val preparationIdentity:String?=null,val preparationProfile:String?=null,val preparationProfileVersion:Int?=null,
    val requestDigest:String?=null,val providerBodyDigest:String?=null,val correlationId:String?=null,val authorizationPurpose:String?=null,
    val providerProfile:String?=null,val model:String?=null,val maximumProviderCalls:Int?=null,val automaticRetryLimit:Int?=null,
    val externalReasoningAuthorized:Boolean?=null,
) {
    init { require(authorizationId.matches(Regex("^[A-Za-z0-9_.-]{1,120}$")));require(sourceSha256.matches(Regex("^[0-9a-f]{64}$")))
        require(capabilityId==ORDINARY_REQUEST_REGION_V8_CAPABILITY_ID&&capabilityDigest==OrdinaryRequestRegionV8Capability().digest()&&provider==REQUEST_REGION_PROVIDER&&expiresAt.isAfter(approvedAt))
        require(formatVersion in 1..REQUEST_REGION_V8_EXACT_AUTHORIZATION_FORMAT)
        if(formatVersion==REQUEST_REGION_V8_EXACT_AUTHORIZATION_FORMAT){
            require(listOf(preparationIdentity,requestDigest,providerBodyDigest).all{it?.matches(Regex("^[0-9a-f]{64}$"))==true})
            require(preparationProfile==FULL_PAGE_ACHROMATIC_PROFILE_ID&&preparationProfileVersion==FULL_PAGE_ACHROMATIC_PROFILE_VERSION)
            require(correlationId?.matches(Regex("^[A-Za-z0-9_-]{1,120}$"))==true)
            require(authorizationPurpose==REQUEST_REGION_V8_AUTHORIZATION_PURPOSE&&providerProfile==REQUEST_REGION_V8_PROVIDER_PROFILE&&model==REQUEST_REGION_MODEL)
            require(maximumProviderCalls==1&&automaticRetryLimit==0&&externalReasoningAuthorized==false)
            require(authorizationId==exactIdentity(evidenceArtifactId,sourceSha256,preparationIdentity!!,preparationProfile!!,preparationProfileVersion!!,
                requestDigest!!,providerBodyDigest!!,correlationId!!,capabilityId,capabilityDigest,authorizationPurpose!!,provider,providerProfile!!,model!!,
                maximumProviderCalls!!,automaticRetryLimit!!,externalReasoningAuthorized!!,approvedBy))
        }
    }
    companion object {
        fun exactIdentity(evidenceId:String,sourceSha:String,preparationId:String,preparationProfile:String,preparationVersion:Int,
            requestDigest:String,bodyDigest:String,correlationId:String,capabilityId:String,capabilityDigest:String,purpose:String,
            provider:String,providerProfile:String,model:String,maximumCalls:Int,retryLimit:Int,externalReasoning:Boolean,
            approvedBy:String)=v8ExecutionDigest("parker.request-region-v8.exact-owner-authorization.v2",
            evidenceId,sourceSha,preparationId,preparationProfile,preparationVersion,requestDigest,bodyDigest,correlationId,capabilityId,
            capabilityDigest,purpose,provider,providerProfile,model,maximumCalls,retryLimit,externalReasoning,approvedBy)
    }
}
data class OrdinaryRequestRegionV8AuthorizationSnapshot(val grant:OrdinaryRequestRegionV8OwnerAuthorization,val state:OrdinaryRegionAuthorizationState,val executionId:String?=null,val reservedAt:Instant?=null,val revokedAt:Instant?=null,val revocationPostAttempt:Boolean=false)
class FileSystemOrdinaryRequestRegionV8AuthorizationStore(root:Path) {
    private val root=root.toAbsolutePath().normalize()
    init { require(Files.isDirectory(this.root)&&Files.isReadable(this.root)&&Files.isWritable(this.root)) }
    fun create(g:OrdinaryRequestRegionV8OwnerAuthorization)=v8CreateOnce(base(g.authorizationId,g.formatVersion),encode(g))
    fun loadIfPresent(id:String)=when{Files.exists(base(id,2))->load(id);Files.exists(base(id,1))->load(id);else->null}
    fun load(id:String):OrdinaryRequestRegionV8AuthorizationSnapshot {
        val path=when{Files.exists(base(id,2))->base(id,2);Files.exists(base(id,1))->base(id,1);else->error("authorization not found")}
        val g=decode(Files.readString(path));val e=event(id);val events=if(Files.exists(e))Files.readAllLines(e)else emptyList()
        val decoded=events.map(::decodeEvent);val reservation=decoded.filter{it[0]=="RESERVED"}.singleOrNull();val revocation=decoded.filter{it[0]=="REVOKED"}.singleOrNull()
        return OrdinaryRequestRegionV8AuthorizationSnapshot(g,if(reservation==null)OrdinaryRegionAuthorizationState.AVAILABLE else OrdinaryRegionAuthorizationState.RESERVED_FOR_EXECUTION,reservation?.get(1),reservation?.get(2)?.let(Instant::parse),revocation?.get(1)?.let(Instant::parse),revocation?.get(2)?.toBooleanStrict()?:false)
    }
    fun reserve(id:String,executionId:String,at:Instant):OrdinaryRequestRegionV8AuthorizationSnapshot {
        val s=load(id);require(s.revokedAt==null){"OWNER_AUTHORIZATION_REVOKED"};if(s.executionId!=null){require(s.executionId==executionId);return s};require(at.isBefore(s.grant.expiresAt))
        val body=listOf("RESERVED",executionId,at.toString()).joinToString("\t");FileChannel.open(event(id),StandardOpenOption.CREATE,StandardOpenOption.APPEND,StandardOpenOption.WRITE).use{c->val b=ByteBuffer.wrap("$body\t${v8ExecutionDigest(body)}\n".toByteArray());while(b.hasRemaining())c.write(b);c.force(true)};return load(id)
    }
    fun revoke(id:String,at:Instant,providerAttemptStarted:Boolean):OrdinaryRequestRegionV8AuthorizationSnapshot {val s=load(id);if(s.revokedAt!=null)return s;val body=listOf("REVOKED",at.toString(),providerAttemptStarted.toString()).joinToString("\t");FileChannel.open(event(id),StandardOpenOption.CREATE,StandardOpenOption.APPEND,StandardOpenOption.WRITE).use{c->val b=ByteBuffer.wrap("$body\t${v8ExecutionDigest(body)}\n".toByteArray());while(b.hasRemaining())c.write(b);c.force(true)};return load(id)}
    private fun encode(g:OrdinaryRequestRegionV8OwnerAuthorization):String { val f=if(g.formatVersion==1)listOf(g.authorizationId,g.evidenceArtifactId,g.sourceSha256,g.capabilityId,g.capabilityDigest,g.provider,v8B64(g.approvedBy),g.approvedAt.toString(),g.expiresAt.toString()) else listOf(
        g.authorizationId,g.evidenceArtifactId,g.sourceSha256,g.preparationIdentity!!,g.preparationProfile!!,g.preparationProfileVersion.toString(),g.requestDigest!!,g.providerBodyDigest!!,g.correlationId!!,
        g.capabilityId,g.capabilityDigest,g.authorizationPurpose!!,g.provider,g.providerProfile!!,g.model!!,g.maximumProviderCalls.toString(),g.automaticRetryLimit.toString(),g.externalReasoningAuthorized.toString(),
        v8B64(g.approvedBy),g.approvedAt.toString(),g.expiresAt.toString());val b=f.joinToString("\t");return "$b\t${v8ExecutionDigest(b)}\n" }
    private fun decode(s:String):OrdinaryRequestRegionV8OwnerAuthorization { val p=s.trimEnd().split('\t');val b=p.dropLast(1).joinToString("\t");require(p.last()==v8ExecutionDigest(b));return when(p.size){
        10->OrdinaryRequestRegionV8OwnerAuthorization(p[0],p[1],p[2],p[3],p[4],p[5],v8Unb64(p[6]),Instant.parse(p[7]),Instant.parse(p[8]))
        22->OrdinaryRequestRegionV8OwnerAuthorization(p[0],p[1],p[2],p[9],p[10],p[12],v8Unb64(p[18]),Instant.parse(p[19]),Instant.parse(p[20]),2,p[3],p[4],p[5].toInt(),p[6],p[7],p[8],p[11],p[13],p[14],p[15].toInt(),p[16].toInt(),p[17].toBooleanStrict())
        else->error("unsupported request-region-v8 authorization version")}}
    private fun decodeEvent(s:String):List<String>{val p=s.split('\t');val b=p.dropLast(1).joinToString("\t");require(p.last()==v8ExecutionDigest(b));return p.dropLast(1)}
    private fun base(id:String,version:Int)=root.resolve("$id.request-region-v8-owner-authorization-v$version").normalize().also{require(it.parent==root)}
    private fun event(id:String)=root.resolve("$id.request-region-v8-owner-authorization-events-v1").normalize().also{require(it.parent==root)}
}

data class RequestRegionV8ProviderStateReceipt(val recordId:String,val requestDigest:String,val rawDigest:String)
data class RecoveredRequestRegionV8ProviderState(
    val recordId:String,val capabilityId:String,val capabilityDigest:String,val implementationCommit:String,val evidenceArtifactId:String,
    val sourceSha256:String,val executionId:String,val correlationId:String,val requestDigest:String,val providerBodyDigest:String,
    val manifestDigest:String,val rawDigest:String,val rawBytes:ByteArray,val recordDigest:String,val outcomeCode:String?,
    val structuredDigest:String?,val exactStructuredState:Map<String,Any?>?,val downstreamProcessingPending:Boolean,
)

class FileSystemRequestRegionV8ProviderStateStore(root:Path) {
    private val root=root.toAbsolutePath().normalize();private val tmp=this.root.resolve(".tmp")
    init { require(Files.isDirectory(this.root)&&Files.isWritable(this.root));Files.createDirectories(tmp) }
    fun persistReceived(construction:CanonicalRequestRegionV8Construction,capability:OrdinaryRequestRegionV8CapabilityIdentity,
        implementationCommit:String,executionId:String,status:Int,contentType:String?,raw:ByteArray):RequestRegionV8ProviderStateReceipt {
        require(raw.isNotEmpty()&&raw.size<=FileSystemRegionProviderStateStore.MAX_RAW_BYTES);require(implementationCommit.matches(Regex("^[0-9a-f]{40}$")))
        val requestDigest=construction.requestBindingSha256;val bodyDigest=construction.providerBodySha256
        val manifest=manifest(construction.request);val manifestDigest=v8ExecutionDigest(RegionJson.encode(manifest))
        val rawDigest=v8Sha(raw);val recordId=v8ExecutionDigest(REQUEST_REGION_V8_PROVIDER_STATE_FORMAT,capability.capabilityId,capability.capabilityDigest,executionId,requestDigest)
        val record=linkedMapOf<String,Any?>("format" to REQUEST_REGION_V8_PROVIDER_STATE_FORMAT,"record_id" to recordId,
            "evidence_artifact_id" to construction.request.regions.first().sourceEvidenceArtifactId.value,"source_sha256" to construction.request.regions.first().sourceSha256,
            "capability_id" to capability.capabilityId,"capability_digest" to capability.capabilityDigest,"implementation_commit" to implementationCommit,
            "profile" to capability.profile,"schema" to capability.schema,"wire_version" to capability.wireVersion,"adapter_id" to capability.adapterId,
            "adapter_version" to capability.adapterVersion,"parser_id" to capability.parserId,"parser_version" to capability.parserVersion,
            "processing" to capability.processing,"provider" to capability.provider,"model" to capability.model,"operation" to capability.endpointOperation,
            "reasoning" to capability.reasoning,"store" to capability.store,"execution_id" to executionId,"correlation_id" to construction.request.correlationId,
            "request_digest" to requestDigest,"provider_body_digest" to bodyDigest,"manifest_digest" to manifestDigest,"manifest" to manifest,
            "http_status" to status,"content_type" to contentType,"raw_length" to raw.size,"raw_sha256" to rawDigest,"raw_base64" to Base64.getEncoder().encodeToString(raw))
        val bytes=RegionJson.encode(linkedMapOf("record" to record,"record_sha256" to v8Sha(RegionJson.encode(record).toByteArray()))).toByteArray()
        atomicCreate(recordPath(recordId),bytes);return RequestRegionV8ProviderStateReceipt(recordId,requestDigest,rawDigest)
    }
    fun recordAssessment(receipt:RequestRegionV8ProviderStateReceipt,outcome:String,structured:Map<String,Any?>?) {
        val digest=structured?.let{v8Sha(RegionJson.encode(it).toByteArray())};val a=linkedMapOf<String,Any?>("format" to REQUEST_REGION_V8_PROVIDER_ASSESSMENT_FORMAT,
            "record_id" to receipt.recordId,"request_digest" to receipt.requestDigest,"raw_sha256" to receipt.rawDigest,"outcome_code" to outcome,
            "structured_sha256" to digest,"exact_structured_state" to structured);atomicCreate(assessmentPath(receipt.recordId),RegionJson.encode(linkedMapOf("assessment" to a,"assessment_sha256" to v8Sha(RegionJson.encode(a).toByteArray()))).toByteArray())
    }
    fun readFor(c:CanonicalRequestRegionV8Construction,capability:OrdinaryRequestRegionV8CapabilityIdentity,executionId:String):RecoveredRequestRegionV8ProviderState? {
        val id=v8ExecutionDigest(REQUEST_REGION_V8_PROVIDER_STATE_FORMAT,capability.capabilityId,capability.capabilityDigest,executionId,c.requestBindingSha256)
        return if(Files.exists(recordPath(id)))read(id)else null
    }
    fun read(id:String):RecoveredRequestRegionV8ProviderState {
        val top=verified(recordPath(id),"record","record_sha256");require(top["format"]==REQUEST_REGION_V8_PROVIDER_STATE_FORMAT&&top["record_id"]==id)
        val raw=Base64.getDecoder().decode(top["raw_base64"] as String);require(raw.size==(top["raw_length"] as Number).toInt()&&v8Sha(raw)==top["raw_sha256"])
        val ap=assessmentPath(id);val assessment=if(Files.exists(ap))verified(ap,"assessment","assessment_sha256")else null
        @Suppress("UNCHECKED_CAST") val structured=assessment?.get("exact_structured_state") as? Map<String,Any?>
        val structuredDigest=assessment?.get("structured_sha256") as? String;require(structured==null&&structuredDigest==null||structured!=null&&v8Sha(RegionJson.encode(structured).toByteArray())==structuredDigest)
        return RecoveredRequestRegionV8ProviderState(id,top["capability_id"] as String,top["capability_digest"] as String,top["implementation_commit"] as String,
            top["evidence_artifact_id"] as String,top["source_sha256"] as String,top["execution_id"] as String,top["correlation_id"] as String,
            top["request_digest"] as String,top["provider_body_digest"] as String,top["manifest_digest"] as String,top["raw_sha256"] as String,raw,
            v8Sha(RegionJson.encode(top).toByteArray()),assessment?.get("outcome_code") as? String,structuredDigest,structured,assessment==null)
    }
    private fun manifest(r:RequestRegionV8Request)=r.regions.map{linkedMapOf("request_region_id" to it.id.value,"page_number" to it.pageNumber,
        "page_representation_id" to it.pageRepresentationId.value,"bounds" to listOf(it.bounds.left,it.bounds.top,it.bounds.rightExclusive,it.bounds.bottomExclusive),
        "crop_digest" to it.cropDigest.value,"constituent_source_region_ids" to it.constituentIds.map(SourceRegionId::value))}
    private fun verified(path:Path,field:String,digest:String):Map<String,Any?> {@Suppress("UNCHECKED_CAST") val t=RegionJson.parse(Files.readString(path)) as Map<String,Any?>;@Suppress("UNCHECKED_CAST") val v=t[field] as Map<String,Any?>;require(t[digest]==v8Sha(RegionJson.encode(v).toByteArray()));return v}
    private fun atomicCreate(target:Path,bytes:ByteArray){if(Files.exists(target)){require(Files.readAllBytes(target).contentEquals(bytes));return};val temp=Files.createTempFile(tmp,"v8-state-",".tmp");try{FileChannel.open(temp,StandardOpenOption.WRITE).use{c->val b=ByteBuffer.wrap(bytes);while(b.hasRemaining())c.write(b);c.force(true)};Files.createLink(target,temp);Files.delete(temp)}finally{Files.deleteIfExists(temp)}}
    private fun recordPath(id:String)=root.resolve("$id.request-region-v8-provider-state")
    private fun assessmentPath(id:String)=root.resolve("$id.request-region-v8-assessment")
}

data class OrdinaryRequestRegionV8PreparedRequest(
    val construction:CanonicalRequestRegionV8Construction,val identity:FidelityFirstExecutionIdentity,
    val capability:OrdinaryRequestRegionV8CapabilityIdentity,val implementationCommit:String,
)
sealed interface OrdinaryRequestRegionV8PreparationOutcome {
    data class Prepared(val value:OrdinaryRequestRegionV8PreparedRequest):OrdinaryRequestRegionV8PreparationOutcome
    data class Blocked(val disposition:OrdinaryRegionDisposition,val detail:String):OrdinaryRequestRegionV8PreparationOutcome
}
class OrdinaryRequestRegionV8RequestPreparer(
    private val store:FileSystemFullPageAchromaticPreparationStore,
    private val builder:FullPageAchromaticCanonicalRequestRegionV8Builder=FullPageAchromaticCanonicalRequestRegionV8Builder(),
) {
    internal fun find(source:AuthoritativeAcquisitionInput)=store.findExact(source.evidenceArtifactId,source.sha256,FULL_PAGE_ACHROMATIC_PROFILE_ID,FULL_PAGE_ACHROMATIC_PROFILE_VERSION)
    internal fun correlationId(source:AuthoritativeAcquisitionInput):String {
        val input=listOf("parker.corrected-preparation.operation.v1",source.evidenceArtifactId.value,source.sha256,FULL_PAGE_ACHROMATIC_PROFILE_ID,FULL_PAGE_ACHROMATIC_PROFILE_VERSION.toString()).joinToString("\u0000")
        return "preparation-"+v8Sha(input.toByteArray())
    }
    internal fun construct(source:AuthoritativeAcquisitionInput,preparationIdentity:String,correlationId:String):OrdinaryRequestRegionV8PreparationOutcome {
        val mediaType=source.mediaType
        if(mediaType!="application/pdf")return blocked(OrdinaryRegionDisposition.UNSUPPORTED_MEDIA,"request-region v8 accepts application/pdf only")
        val document=try{store.read(preparationIdentity)}catch(e:Exception){return blocked(OrdinaryRegionDisposition.REQUEST_BOUNDS_EXCEEDED,e.message?:"persisted corrected preparation unavailable")}
        if(document.preparationIdentity!=preparationIdentity||document.regions.any{it.provenance.evidenceId!=source.evidenceArtifactId||it.provenance.sourceSha256!=source.sha256})
            return blocked(OrdinaryRegionDisposition.REQUEST_BOUNDS_EXCEEDED,"persisted corrected preparation binding mismatch")
        if(document.regions.any{it.provenance.preparationProfileId!=FULL_PAGE_ACHROMATIC_PROFILE_ID||it.provenance.preparationProfileVersion!=FULL_PAGE_ACHROMATIC_PROFILE_VERSION})
            return blocked(OrdinaryRegionDisposition.REQUEST_BOUNDS_EXCEEDED,"persisted corrected preparation profile mismatch")
        val c=try{builder.buildPersisted(document,correlationId)}catch(e:Exception){return blocked(OrdinaryRegionDisposition.REQUEST_BOUNDS_EXCEEDED,e.message?:"canonical persisted V8 construction failed")}
        if(c.request.regions.size !in 1..REQUEST_REGION_MAXIMUM)return blocked(OrdinaryRegionDisposition.REQUEST_BOUNDS_EXCEEDED,"complete shaped request-region set exceeds 32")
        if(c.providerBody.toByteArray().size>REQUEST_REGION_BODY_MAXIMUM_BYTES)return blocked(OrdinaryRegionDisposition.REQUEST_BOUNDS_EXCEEDED,"exact UTF-8 request body exceeds governed bound")
        return OrdinaryRequestRegionV8PreparationOutcome.Prepared(OrdinaryRequestRegionV8PreparedRequest(c,
            FidelityFirstExecutionIdentity("PREAUTH",c.requestBindingSha256,correlationId,source.evidenceArtifactId.value,source.sha256,source.byteLength,
                mediaType,"0".repeat(40),REQUEST_REGION_PROVIDER,REQUEST_REGION_MODEL,REQUEST_REGION_V8_PROFILE_ID,REQUEST_REGION_V8_INSTRUCTION_SHA256,
                REQUEST_REGION_V8_SCHEMA_SHA256,REQUEST_REGION_V8_PROCESSING_PROFILE,REQUEST_REGION_V8_ADAPTER_VERSION),OrdinaryRequestRegionV8CapabilityIdentity(),"0".repeat(40)))
    }
    internal fun prepare(source:AuthoritativeAcquisitionInput,preparationIdentity:String,executionId:String,correlationId:String,commit:String):OrdinaryRequestRegionV8PreparationOutcome {
        val outcome=construct(source,preparationIdentity,correlationId)
        val base=outcome as? OrdinaryRequestRegionV8PreparationOutcome.Prepared?:return outcome
        val c=base.value.construction;val mediaType=source.mediaType!!
        val cap=OrdinaryRequestRegionV8CapabilityIdentity()
        val i=FidelityFirstExecutionIdentity(executionId,c.requestBindingSha256,correlationId,source.evidenceArtifactId.value,source.sha256,source.byteLength,
            mediaType,commit,cap.provider,cap.model,cap.profile,cap.instructionSha256,cap.schemaSha256,cap.processing,cap.adapterVersion)
        return OrdinaryRequestRegionV8PreparationOutcome.Prepared(OrdinaryRequestRegionV8PreparedRequest(c,i,cap,commit))
    }
    private fun blocked(d:OrdinaryRegionDisposition,s:String)=OrdinaryRequestRegionV8PreparationOutcome.Blocked(d,s)
}

sealed interface RequestRegionV8ProviderExchangeOutcome {
    val state:RecoveredRequestRegionV8ProviderState
    data class Valid(override val state:RecoveredRequestRegionV8ProviderState,val result:RequestRegionV8Result):RequestRegionV8ProviderExchangeOutcome
    data class Invalid(override val state:RecoveredRequestRegionV8ProviderState,val reason:String):RequestRegionV8ProviderExchangeOutcome
}
fun interface RequestRegionV8ProviderExchange { suspend fun exchange(prepared:OrdinaryRequestRegionV8PreparedRequest):RequestRegionV8ProviderExchangeOutcome }

class OpenAiRequestRegionV8ProviderExchange internal constructor(
    private val credential:OpenAiApiCredential,private val transport:OpenAiResponsesTransport,
    private val state:FileSystemRequestRegionV8ProviderStateStore,private val timeoutMillis:Long=300_000,
):RequestRegionV8ProviderExchange {
    override suspend fun exchange(prepared:OrdinaryRequestRegionV8PreparedRequest):RequestRegionV8ProviderExchangeOutcome {
        val response=transport.execute(OpenAiResponsesTransportRequest(URI("https://api.openai.com/v1/responses"),timeoutMillis,
            prepared.construction.providerBody,FileSystemRegionProviderStateStore.MAX_RAW_BYTES.toLong(),credential))
        val receipt=state.persistReceived(prepared.construction,prepared.capability,prepared.implementationCommit,prepared.identity.executionId,response.statusCode,null,response.body)
        var structured:Map<String,Any?>?=null
        val parsed=runCatching {
            require(response.statusCode in 200..299)
            @Suppress("UNCHECKED_CAST") val envelope=RegionJson.parse(response.body.toString(Charsets.UTF_8)) as Map<String,Any?>
            val responseId=envelope["id"] as String;require(envelope["model"]==prepared.capability.model)
            @Suppress("UNCHECKED_CAST") val output=envelope["output"] as List<Map<String,Any?>>
            @Suppress("UNCHECKED_CAST") val content=output.single{it["type"]=="message"}["content"] as List<Map<String,Any?>>
            structured=RegionJson.parse(content.single{it["type"]=="output_text"}["text"] as String) as Map<String,Any?>
            structured = enrichAuthoritativeProviderProvenance(structured!!, responseId, prepared.capability.model)
            val valid=RequestRegionV8StructuredValidator().validate(prepared.construction.request,requireNotNull(structured)) as RequestRegionV8ValidationOutcome.Valid
            valid.result
        }
        val code=if(parsed.isSuccess)"SUCCESS" else "V8_PARSE_OR_VALIDATION_FAILED"
        state.recordAssessment(receipt,code,structured)
        val recovered=state.read(receipt.recordId)
        return parsed.fold({RequestRegionV8ProviderExchangeOutcome.Valid(recovered,it)},{RequestRegionV8ProviderExchangeOutcome.Invalid(recovered,code)})
    }

    private fun enrichAuthoritativeProviderProvenance(
        payload: Map<String, Any?>,
        responseId: String,
        responseModel: String,
    ): Map<String, Any?> {
        @Suppress("UNCHECKED_CAST")
        val provenance = payload["provider_provenance"] as? Map<String, Any?> ?: error("missing provider provenance")
        val structuredResponseId = provenance["provider_response_id"] as? String
        val structuredModel = provenance["provider_reported_model"] as? String
        require(structuredResponseId == null || structuredResponseId == responseId) { "provider response identity conflicts with envelope" }
        require(structuredModel == null || structuredModel == responseModel) { "provider model conflicts with envelope" }
        val enriched = provenance.toMutableMap()
        enriched["provider_response_id"] = responseId
        enriched["provider_reported_model"] = responseModel
        return payload.toMutableMap().also { it["provider_provenance"] = enriched }
    }
}

sealed interface OrdinaryRequestRegionV8ExecutionOutcome {
    data class Valid(val state:RecoveredRequestRegionV8ProviderState,val result:RequestRegionV8Result,val recovered:Boolean):OrdinaryRequestRegionV8ExecutionOutcome
    data class Invalid(val state:RecoveredRequestRegionV8ProviderState?,val reason:String):OrdinaryRequestRegionV8ExecutionOutcome
}
class GovernedRequestRegionV8ExecutionCoordinator(
    private val ledger:FileSystemFidelityFirstAttemptLedger,private val providerState:FileSystemRequestRegionV8ProviderStateStore,
    private val exchange:RequestRegionV8ProviderExchange,
) {
    suspend fun execute(prepared:OrdinaryRequestRegionV8PreparedRequest):OrdinaryRequestRegionV8ExecutionOutcome {
        prepareForGuardedAttempt(prepared)?.let{return it};durablyStartProviderAttempt(prepared)?.let{return it};return transportAfterGuardRelease(prepared)
    }
    fun prepareForGuardedAttempt(prepared:OrdinaryRequestRegionV8PreparedRequest):OrdinaryRequestRegionV8ExecutionOutcome? {
        bindingMismatch(prepared)?.let{return OrdinaryRequestRegionV8ExecutionOutcome.Invalid(null,it)}
        providerState.readFor(prepared.construction,prepared.capability,prepared.identity.executionId)?.let{return recover(prepared,it,true)}
        val initial=try{ledger.open(prepared.identity)}catch(_:Exception){return OrdinaryRequestRegionV8ExecutionOutcome.Invalid(null,"ATTEMPT_IDENTITY_CONFLICT")}
        if(initial.providerAttemptStarted)return OrdinaryRequestRegionV8ExecutionOutcome.Invalid(null,"ATTEMPT_STARTED_WITHOUT_DURABLE_RESPONSE")
        try {
            ledger.advancePreAttempt(prepared.identity,FidelityFirstAttemptStage.PREFLIGHT_PASSED)
            ledger.advancePreAttempt(prepared.identity,FidelityFirstAttemptStage.SOURCE_RETRIEVED)
            ledger.advancePreAttempt(prepared.identity,FidelityFirstAttemptStage.REQUEST_PREPARED)
        }catch(_:Exception){return OrdinaryRequestRegionV8ExecutionOutcome.Invalid(null,"ATTEMPT_MARKER_PERSISTENCE_FAILED")}
        return null
    }
    /** Recovery-only boundary: it cannot start an attempt or invoke [exchange]. */
    fun recoverPersistedPostEgress(prepared:OrdinaryRequestRegionV8PreparedRequest):OrdinaryRequestRegionV8ExecutionOutcome {
        bindingMismatch(prepared)?.let{return OrdinaryRequestRegionV8ExecutionOutcome.Invalid(null,it)}
        val durable=providerState.readFor(prepared.construction,prepared.capability,prepared.identity.executionId)
            ?:return OrdinaryRequestRegionV8ExecutionOutcome.Invalid(null,"DURABLE_PROVIDER_STATE_REQUIRED")
        val attemptStarted=runCatching{ledger.open(prepared.identity).providerAttemptStarted}.getOrDefault(false)
        if(!attemptStarted)return OrdinaryRequestRegionV8ExecutionOutcome.Invalid(durable,"DURABLE_PROVIDER_ATTEMPT_REQUIRED")
        return recover(prepared,durable,true)
    }
    fun durablyStartProviderAttempt(prepared:OrdinaryRequestRegionV8PreparedRequest):OrdinaryRequestRegionV8ExecutionOutcome? {
        bindingMismatch(prepared)?.let{return OrdinaryRequestRegionV8ExecutionOutcome.Invalid(null,it)}
        providerState.readFor(prepared.construction,prepared.capability,prepared.identity.executionId)?.let{return recover(prepared,it,true)}
        return try{val s=ledger.open(prepared.identity);if(s.providerAttemptStarted)OrdinaryRequestRegionV8ExecutionOutcome.Invalid(null,"CONCURRENT_OR_PRIOR_ATTEMPT_STARTED") else {ledger.transition(prepared.identity,FidelityFirstAttemptStage.PROVIDER_ATTEMPT_STARTED);null}}catch(_:Exception){OrdinaryRequestRegionV8ExecutionOutcome.Invalid(null,"ATTEMPT_MARKER_PERSISTENCE_FAILED")}
    }
    suspend fun transportAfterGuardRelease(prepared:OrdinaryRequestRegionV8PreparedRequest):OrdinaryRequestRegionV8ExecutionOutcome {
        providerState.readFor(prepared.construction,prepared.capability,prepared.identity.executionId)?.let{return recover(prepared,it,true)}
        if(!runCatching{ledger.open(prepared.identity).providerAttemptStarted}.getOrDefault(false))return OrdinaryRequestRegionV8ExecutionOutcome.Invalid(null,"PROVIDER_ATTEMPT_NOT_STARTED")
        val outcome=try{exchange.exchange(prepared)}catch(_:Exception){return OrdinaryRequestRegionV8ExecutionOutcome.Invalid(null,"TRANSPORT_OUTCOME_UNKNOWN")}
        runCatching{ledger.transition(prepared.identity,FidelityFirstAttemptStage.PROVIDER_RESPONSE_RECEIVED)}
        return when(outcome){is RequestRegionV8ProviderExchangeOutcome.Valid->OrdinaryRequestRegionV8ExecutionOutcome.Valid(outcome.state,outcome.result,false);is RequestRegionV8ProviderExchangeOutcome.Invalid->OrdinaryRequestRegionV8ExecutionOutcome.Invalid(outcome.state,outcome.reason)}
    }
    private fun recover(p:OrdinaryRequestRegionV8PreparedRequest,s:RecoveredRequestRegionV8ProviderState,recovered:Boolean):OrdinaryRequestRegionV8ExecutionOutcome {
        if(s.capabilityId!=p.capability.capabilityId||s.capabilityDigest!=p.capability.capabilityDigest||s.requestDigest!=p.construction.requestBindingSha256||s.providerBodyDigest!=p.construction.providerBodySha256)return OrdinaryRequestRegionV8ExecutionOutcome.Invalid(s,"RECOVERED_BINDING_MISMATCH")
        val structured=s.exactStructuredState?:return OrdinaryRequestRegionV8ExecutionOutcome.Invalid(s,if(s.downstreamProcessingPending)"RAW_RESPONSE_RECOVERED" else s.outcomeCode?:"PARSE_FAILURE")
        val valid=RequestRegionV8StructuredValidator().validate(p.construction.request,structured) as? RequestRegionV8ValidationOutcome.Valid
            ?:return OrdinaryRequestRegionV8ExecutionOutcome.Invalid(s,"VALIDATION_FAILURE_RECOVERED")
        return OrdinaryRequestRegionV8ExecutionOutcome.Valid(s,valid.result,recovered)
    }
    private fun bindingMismatch(p:OrdinaryRequestRegionV8PreparedRequest):String?=when {
        p.identity.requestId!=p.construction.requestBindingSha256->"REQUEST_DIGEST_MISMATCH"
        p.identity.attemptId!=p.construction.request.correlationId->"ATTEMPT_CORRELATION_MISMATCH"
        p.identity.profileId!=p.capability.profile||p.identity.schemaSha256!=p.capability.schemaSha256||p.identity.adapterVersion!=p.capability.adapterVersion->"CAPABILITY_CONFIGURATION_MISMATCH"
        else->null
    }
}

class OrdinaryRequestRegionV8IngestionWorkflow(
    private val owner:PrincipalId,evidenceCustodian:EvidenceCustodian,private val acceptance:OrdinaryRequestRegionV8AcceptanceEvaluator,
    private val authorizations:FileSystemOrdinaryRequestRegionV8AuthorizationStore,private val guard:OrdinaryRegionAuthorizationGuard,
    private val ledger:FileSystemFidelityFirstAttemptLedger,private val preparer:OrdinaryRequestRegionV8RequestPreparer,
    private val execution:GovernedRequestRegionV8ExecutionCoordinator,private val admission:OrdinaryRegionDerivativeAdmission,
    private val runtimeCommit:()->String,private val now:()->Instant=Instant::now,
):OrdinaryRegionOwnerWorkflowPort {
    private val capability=OrdinaryRequestRegionV8CapabilityIdentity();private val resolver=AuthoritativeAcquisitionSourceResolver(evidenceCustodian)
    override fun capabilityStatus():OrdinaryRegionCapabilityStatus {val accepted=acceptance.evaluate() as? OrdinaryRequestRegionV8AcceptanceEvaluation.Accepted
        return OrdinaryRegionCapabilityStatus(capability.capabilityId,capability.provider,capability.endpointOperation,capability.model,capability.adapterId,
            capability.adapterVersion,capability.profile,capability.wireVersion,"application/pdf",capability.maximumRegions,capability.maximumBodyBytes,false,
            if(accepted==null)OrdinaryRegionCapabilityDisposition.CAPABILITY_NOT_ACCEPTED else OrdinaryRegionCapabilityDisposition.ACCEPTED,runtimeCommit(),accepted?.record?.implementationCommit)}
    override suspend fun proposal(evidenceId:EvidenceArtifactId):OrdinaryRegionProposal?=verified(evidenceId)?.let{OrdinaryRegionProposal(evidenceId.value,capability.capabilityId,capabilityStatus=capabilityStatus())}
    override suspend fun authorizationStatus(evidenceId:EvidenceArtifactId):OrdinaryRegionOwnerAuthorizationView {
        if(acceptance.evaluate() !is OrdinaryRequestRegionV8AcceptanceEvaluation.Accepted)return unavailable(evidenceId,"CAPABILITY_NOT_ACCEPTED")
        val source=verified(evidenceId)?:return unavailable(evidenceId,"SOURCE_UNAVAILABLE_OR_UNSUPPORTED")
        val prepared=preauthorization(source)?:return unavailable(evidenceId,"PERSISTED_PREPARATION_UNAVAILABLE_OR_INVALID");val id=authorizationIdentity(source,prepared)
        val s=try{authorizations.loadIfPresent(id)}catch(_:Exception){return unavailable(evidenceId,"AUTHORIZATION_STORE_UNAVAILABLE_OR_CORRUPT")}
        return s?.let(::view)?:OrdinaryRegionOwnerAuthorizationView(OrdinaryRegionOwnerAuthorizationDisposition.NOT_AUTHORISED,evidenceId.value)
    }
    override suspend fun authorize(evidenceId:EvidenceArtifactId):OrdinaryRegionOwnerAuthorizationOutcome {
        if(acceptance.evaluate() !is OrdinaryRequestRegionV8AcceptanceEvaluation.Accepted)return OrdinaryRegionOwnerAuthorizationOutcome.Blocked(unavailable(evidenceId,"CAPABILITY_NOT_ACCEPTED"))
        val source=verified(evidenceId)?:return OrdinaryRegionOwnerAuthorizationOutcome.Blocked(unavailable(evidenceId,"SOURCE_UNAVAILABLE_OR_UNSUPPORTED"))
        val prepared=preauthorization(source)?:return OrdinaryRegionOwnerAuthorizationOutcome.Blocked(unavailable(evidenceId,"PERSISTED_PREPARATION_UNAVAILABLE_OR_INVALID"));val id=authorizationIdentity(source,prepared)
        return try{guard.locked(id){authorizations.loadIfPresent(id)?.let{return@locked OrdinaryRegionOwnerAuthorizationOutcome.Existing(view(it))};val at=now();authorizations.create(
            exactAuthorization(id,source,prepared,at))
            OrdinaryRegionOwnerAuthorizationOutcome.Created(view(authorizations.load(id)))}}catch(_:Exception){OrdinaryRegionOwnerAuthorizationOutcome.Blocked(unavailable(evidenceId,"AUTHORIZATION_CREATE_FAILED"))}
    }
    override fun createAuthorization(grant:OrdinaryRegionOwnerAuthorization):Nothing=error("V5_AUTHORIZATION_CANNOT_AUTHORIZE_V8")
    override fun reserve(authorizationId:String,executionId:String):OrdinaryRegionAuthorizationSnapshot=guard.locked(authorizationId){legacy(authorizations.reserve(authorizationId,executionId,now()))}
    override fun revoke(authorizationId:String):OrdinaryRegionAuthorizationSnapshot=guard.locked(authorizationId){val s=authorizations.load(authorizationId);val started=s.executionId?.let{ledger.providerAttemptStartedForExecution(it)}?:false;legacy(authorizations.revoke(authorizationId,now(),started))}
    override suspend fun execute(evidenceId:EvidenceArtifactId,authorizationId:String,executionId:String,attemptId:String):OrdinaryRegionOwnerResult {
        val accepted=acceptance.evaluate() as? OrdinaryRequestRegionV8AcceptanceEvaluation.Accepted?:return result(OrdinaryRegionDisposition.CAPABILITY_NOT_ACCEPTED)
        val source=verified(evidenceId)?:return result(OrdinaryRegionDisposition.SOURCE_UNAVAILABLE)
        val snapshot=try{guard.locked(authorizationId){authorizations.load(authorizationId)}}catch(_:Exception){return result(OrdinaryRegionDisposition.OWNER_AUTHORIZATION_REQUIRED)}
        if(!bindingMatches(snapshot.grant,source)||snapshot.revokedAt!=null)return result(OrdinaryRegionDisposition.EXECUTION_CONFLICT,"authorization binding mismatch")
        if(snapshot.grant.formatVersion!=REQUEST_REGION_V8_EXACT_AUTHORIZATION_FORMAT)return result(OrdinaryRegionDisposition.OWNER_AUTHORIZATION_REQUIRED,"EXACT_AUTHORIZATION_REQUIRED")
        val prepared=when(val p=preparer.prepare(source,snapshot.grant.preparationIdentity!!,executionId,snapshot.grant.correlationId!!,runtimeCommit())){is OrdinaryRequestRegionV8PreparationOutcome.Blocked->return result(p.disposition,p.detail);is OrdinaryRequestRegionV8PreparationOutcome.Prepared->p.value}
        if(!exactBindingMatches(snapshot.grant,prepared))return result(OrdinaryRegionDisposition.EXECUTION_CONFLICT,"exact authorization envelope mismatch")
        try{guard.locked(authorizationId){authorizations.reserve(authorizationId,executionId,now())}}catch(e:Exception){return result(if(e.message?.contains("EXPIRED")==true)OrdinaryRegionDisposition.OWNER_AUTHORIZATION_EXPIRED_BEFORE_RESERVATION else OrdinaryRegionDisposition.OWNER_AUTHORIZATION_REQUIRED)}
        val prior=execution.prepareForGuardedAttempt(prepared)
        val outcome=if(prior!=null)prior else {val start=guard.locked(authorizationId){val s=authorizations.load(authorizationId);if(s.revokedAt!=null||s.executionId!=executionId||!bindingMatches(s.grant,source))OrdinaryRequestRegionV8ExecutionOutcome.Invalid(null,"AUTHORIZATION_CHANGED")else execution.durablyStartProviderAttempt(prepared)}
            start?:execution.transportAfterGuardRelease(prepared)}
        val valid=outcome as? OrdinaryRequestRegionV8ExecutionOutcome.Valid?:return result(if((outcome as? OrdinaryRequestRegionV8ExecutionOutcome.Invalid)?.state?.downstreamProcessingPending==true)OrdinaryRegionDisposition.PROVIDER_RESPONSE_AVAILABLE else OrdinaryRegionDisposition.VALIDATION_FAILED,(outcome as? OrdinaryRequestRegionV8ExecutionOutcome.Invalid)?.reason?:"V8_EXECUTION_FAILED")
        return admitValidated(source,prepared,valid,accepted.record.recordId,snapshot.grant)
    }
    override suspend fun continuePostEgress(evidenceId:EvidenceArtifactId,authorizationId:String,executionId:String,providerStateId:String):OrdinaryRegionOwnerResult {
        val accepted=acceptance.evaluate() as? OrdinaryRequestRegionV8AcceptanceEvaluation.Accepted?:return result(OrdinaryRegionDisposition.CAPABILITY_NOT_ACCEPTED)
        val source=verified(evidenceId)?:return result(OrdinaryRegionDisposition.SOURCE_UNAVAILABLE)
        val snapshot=try{guard.locked(authorizationId){authorizations.load(authorizationId)}}catch(_:Exception){return result(OrdinaryRegionDisposition.OWNER_AUTHORIZATION_REQUIRED)}
        if(snapshot.grant.formatVersion!=REQUEST_REGION_V8_EXACT_AUTHORIZATION_FORMAT||snapshot.revokedAt!=null||snapshot.executionId!=executionId||
            !bindingMatches(snapshot.grant,source))return result(OrdinaryRegionDisposition.EXECUTION_CONFLICT,"continuation authorization binding mismatch")
        val prepared=when(val p=preparer.prepare(source,requireNotNull(snapshot.grant.preparationIdentity),executionId,requireNotNull(snapshot.grant.correlationId),runtimeCommit())){
            is OrdinaryRequestRegionV8PreparationOutcome.Blocked->return result(p.disposition,p.detail)
            is OrdinaryRequestRegionV8PreparationOutcome.Prepared->p.value
        }
        if(!exactBindingMatches(snapshot.grant,prepared))return result(OrdinaryRegionDisposition.EXECUTION_CONFLICT,"exact authorization envelope mismatch")
        val recovered=execution.recoverPersistedPostEgress(prepared)
        val valid=recovered as? OrdinaryRequestRegionV8ExecutionOutcome.Valid
            ?:return result(OrdinaryRegionDisposition.VALIDATION_FAILED,(recovered as OrdinaryRequestRegionV8ExecutionOutcome.Invalid).reason)
        if(!valid.recovered||valid.state.recordId!=providerStateId)return result(OrdinaryRegionDisposition.EXECUTION_CONFLICT,"provider-state binding mismatch")
        return admitValidated(source,prepared,valid,accepted.record.recordId,snapshot.grant)
    }
    private suspend fun admitValidated(source:AuthoritativeAcquisitionInput,p:OrdinaryRequestRegionV8PreparedRequest,v:OrdinaryRequestRegionV8ExecutionOutcome.Valid,
        acceptanceId:String,authorization:OrdinaryRequestRegionV8OwnerAuthorization):OrdinaryRegionOwnerResult {
        val derivative=RequestRegionV8DerivativeBinder().bind(p.construction.request,v.result).getOrElse{return result(OrdinaryRegionDisposition.REVIEW_REQUIRED,it.message?:"V8 reconstruction failed")}
        val key=ordinaryRegionGenerationKey(p.identity.executionId,source.evidenceArtifactId.value,source.sha256,acceptanceId,v.state.recordId,derivative.canonicalDigest)
        val payload=payload(source,p,v,derivative,acceptanceId,authorization,key)
        return when(val a=admission.admit(payload,owner)){is OrdinaryRegionAdmissionOutcome.Conflict->result(OrdinaryRegionDisposition.ADMISSION_CONFLICT,a.reason);is OrdinaryRegionAdmissionOutcome.Admitted->{runCatching{ledger.transition(p.identity,FidelityFirstAttemptStage.GENERATION_ADMITTED)};runCatching{ledger.transition(p.identity,FidelityFirstAttemptStage.TERMINAL_SUCCESS,listOf("generationId" to a.record.derivativeGenerationId.value))};result(OrdinaryRegionDisposition.ADMITTED,if(a.recovered)"recovered existing admission" else "admitted",a.record.derivativeGenerationId.value)}}
    }
    private fun payload(source:AuthoritativeAcquisitionInput,p:OrdinaryRequestRegionV8PreparedRequest,v:OrdinaryRequestRegionV8ExecutionOutcome.Valid,d:RequestRegionV8Derivative,acceptanceId:String,authorization:OrdinaryRequestRegionV8OwnerAuthorization,key:String)=
        OrdinaryRegionTranscriptionDerivative(representationVersion=3,capabilityId=capability.capabilityId,capabilityDigest=capability.capabilityDigest,
            evidenceArtifactId=source.evidenceArtifactId.value,sourceSha256=source.sha256,pageBindings=requireNotNull(p.construction.achromaticPreparation).regions.map{"${it.provenance.authoritativePageRepresentationId.value}|${it.provenance.authoritativePixelDigest.value}|${it.provenance.pageNumber}"},
            regionBindings=p.construction.request.regions.map{"${it.id.value}|${it.cropDigest.value}|${it.image.encodedSha256}|${it.constituentIds.joinToString(","){x->x.value}}"},
            transcriptionBlocks=d.blocksInParkerOrder.map{listOf(it.requestRegionId,it.pageNumber.toString(),it.literalText?:"<null>",it.status,it.uncertainties.joinToString("|"){u->"${u.category}:${u.description}:${u.alternatives.joinToString(",")}:${u.providerConfidence}"},it.warnings.joinToString("|")).joinToString("\u001f")},
            providerReturnedOrder=v.result.blocksInProviderOrder.map{it.requestRegionId.value},parkerSourceOrder=d.blocksInParkerOrder.map{it.requestRegionId},provider=capability.provider,model=capability.model,
            adapterId=capability.adapterId,adapterVersion=capability.adapterVersion,providerProfile=requestRegionV8DerivativeProviderProfile(authorization),wireVersion=capability.wireVersion,schemaSha256=capability.schemaSha256,
            instructionSha256=capability.instructionSha256,processingProfile=capability.processing,requestIdentity=p.construction.request.correlationId,requestDigest=v.state.requestDigest,
            responseIdentity=v.state.rawDigest,providerStateRecordIdentity=v.state.recordId,capabilityAcceptanceRecordIdentity=acceptanceId,ownerAuthorizationIdentity=authorization.authorizationId,
            executionIdentity=p.identity.executionId,attemptIdentity=p.identity.attemptId,reconstructedContentDigest=d.canonicalDigest,canonicalGenerationKeyDigest=key,
            admissionProvenance="request-region-v8-capability-acceptance-v1|request-region-v8-owner-authorization-v2|$REQUEST_REGION_V8_PROVIDER_STATE_FORMAT",
            preparationIdentity=authorization.preparationIdentity,preparationProfile=authorization.preparationProfile,preparationProfileVersion=authorization.preparationProfileVersion,
            providerBodyDigest=authorization.providerBodyDigest,authorizationPurpose=authorization.authorizationPurpose,maximumProviderCalls=authorization.maximumProviderCalls,
            automaticRetryLimit=authorization.automaticRetryLimit,externalReasoningAuthorized=authorization.externalReasoningAuthorized)
    private suspend fun verified(id:EvidenceArtifactId)=((resolver.resolve(owner,id) as? AuthoritativeAcquisitionResolution.Verified)?.input)?.takeIf{it.mediaType=="application/pdf"}
    private fun preauthorization(s:AuthoritativeAcquisitionInput):OrdinaryRequestRegionV8PreparedRequest? {
        val document=runCatching{preparer.find(s)}.getOrNull()?:return null
        return (preparer.construct(s,document.preparationIdentity,preparer.correlationId(s)) as? OrdinaryRequestRegionV8PreparationOutcome.Prepared)?.value
    }
    private fun authorizationIdentity(s:AuthoritativeAcquisitionInput,p:OrdinaryRequestRegionV8PreparedRequest)=OrdinaryRequestRegionV8OwnerAuthorization.exactIdentity(
        s.evidenceArtifactId.value,s.sha256,requireNotNull(p.construction.achromaticPreparation).preparationIdentity,FULL_PAGE_ACHROMATIC_PROFILE_ID,FULL_PAGE_ACHROMATIC_PROFILE_VERSION,
        p.construction.requestBindingSha256,p.construction.providerBodySha256,p.construction.request.correlationId,capability.capabilityId,capability.capabilityDigest,
        REQUEST_REGION_V8_AUTHORIZATION_PURPOSE,capability.provider,REQUEST_REGION_V8_PROVIDER_PROFILE,capability.model,1,0,false,owner.value)
    private fun exactAuthorization(id:String,s:AuthoritativeAcquisitionInput,p:OrdinaryRequestRegionV8PreparedRequest,at:Instant)=OrdinaryRequestRegionV8OwnerAuthorization(
        id,s.evidenceArtifactId.value,s.sha256,capability.capabilityId,capability.capabilityDigest,capability.provider,owner.value,at,at.plusSeconds(86_400),2,
        requireNotNull(p.construction.achromaticPreparation).preparationIdentity,FULL_PAGE_ACHROMATIC_PROFILE_ID,FULL_PAGE_ACHROMATIC_PROFILE_VERSION,
        p.construction.requestBindingSha256,p.construction.providerBodySha256,p.construction.request.correlationId,REQUEST_REGION_V8_AUTHORIZATION_PURPOSE,
        REQUEST_REGION_V8_PROVIDER_PROFILE,capability.model,1,0,false)
    private fun bindingMatches(g:OrdinaryRequestRegionV8OwnerAuthorization,s:AuthoritativeAcquisitionInput)=g.evidenceArtifactId==s.evidenceArtifactId.value&&g.sourceSha256==s.sha256&&g.capabilityId==capability.capabilityId&&g.capabilityDigest==capability.capabilityDigest&&g.provider==capability.provider
    private fun exactBindingMatches(g:OrdinaryRequestRegionV8OwnerAuthorization,p:OrdinaryRequestRegionV8PreparedRequest)=
        g.preparationIdentity==p.construction.achromaticPreparation?.preparationIdentity&&g.preparationProfile==FULL_PAGE_ACHROMATIC_PROFILE_ID&&g.preparationProfileVersion==FULL_PAGE_ACHROMATIC_PROFILE_VERSION&&
            g.requestDigest==p.construction.requestBindingSha256&&g.providerBodyDigest==p.construction.providerBodySha256&&g.correlationId==p.construction.request.correlationId&&
            g.capabilityId==p.capability.capabilityId&&g.capabilityDigest==p.capability.capabilityDigest&&g.authorizationPurpose==REQUEST_REGION_V8_AUTHORIZATION_PURPOSE&&
            g.provider==p.capability.provider&&g.providerProfile==REQUEST_REGION_V8_PROVIDER_PROFILE&&g.model==p.capability.model&&g.maximumProviderCalls==1&&g.automaticRetryLimit==0&&g.externalReasoningAuthorized==false
    private fun view(s:OrdinaryRequestRegionV8AuthorizationSnapshot)=OrdinaryRegionOwnerAuthorizationView(if(s.revokedAt==null&&now().isBefore(s.grant.expiresAt))OrdinaryRegionOwnerAuthorizationDisposition.AUTHORISED else OrdinaryRegionOwnerAuthorizationDisposition.UNAVAILABLE,s.grant.evidenceArtifactId,s.grant.provider,"Selected authoritative PDF request-region crops will be transmitted to OpenAI for literal transcription.",s.grant.authorizationId,s.grant.approvedAt,s.grant.expiresAt,when{ s.revokedAt!=null->"OWNER_AUTHORIZATION_REVOKED";!now().isBefore(s.grant.expiresAt)->"OWNER_AUTHORIZATION_EXPIRED";s.executionId!=null->"OWNER_AUTHORIZATION_RESERVED";else->null},if(s.executionId==null)"NOT_STARTED" else "RESERVED")
    private fun unavailable(id:EvidenceArtifactId,detail:String)=OrdinaryRegionOwnerAuthorizationView(OrdinaryRegionOwnerAuthorizationDisposition.UNAVAILABLE,id.value,detail=detail)
    private fun legacy(s:OrdinaryRequestRegionV8AuthorizationSnapshot)=OrdinaryRegionAuthorizationSnapshot(OrdinaryRegionOwnerAuthorization(s.grant.authorizationId,s.grant.evidenceArtifactId,s.grant.sourceSha256,s.grant.capabilityDigest,s.grant.provider,"literal transcription","Selected authoritative PDF request-region crops","Selected authoritative PDF request-region crops will be transmitted to OpenAI for literal transcription.",s.grant.approvedBy,s.grant.approvedAt,s.grant.expiresAt),s.state,s.executionId,s.reservedAt,s.revokedAt,s.revocationPostAttempt)
    private fun result(d:OrdinaryRegionDisposition,s:String=d.name,id:String?=null)=OrdinaryRegionOwnerResult(d,s,id)
}

/** Provider-profile provenance is authorization-bound; acquisition/preparation profiles are separate fields. */
internal fun requestRegionV8DerivativeProviderProfile(
    authorization:OrdinaryRequestRegionV8OwnerAuthorization,
):String {
    require(authorization.formatVersion==REQUEST_REGION_V8_EXACT_AUTHORIZATION_FORMAT)
    val authoritative=requireNotNull(authorization.providerProfile)
    require(authoritative==REQUEST_REGION_V8_PROVIDER_PROFILE)
    return authoritative
}

private fun v8CreateOnce(path:Path,text:String){val bytes=text.toByteArray();try{FileChannel.open(path,StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE).use{c->val b=ByteBuffer.wrap(bytes);while(b.hasRemaining())c.write(b);c.force(true)}}catch(e:java.nio.file.FileAlreadyExistsException){require(Files.readAllBytes(path).contentEquals(bytes))}}
private fun v8ExecutionDigest(vararg fields:Any):String { val md=MessageDigest.getInstance("SHA-256");fields.forEach{f->val b=f.toString().toByteArray(StandardCharsets.UTF_8);md.update(ByteBuffer.allocate(4).putInt(b.size).array());md.update(b)};return md.digest().joinToString(""){"%02x".format(it.toInt()and 255)} }
private fun v8Sha(bytes:ByteArray)=MessageDigest.getInstance("SHA-256").digest(bytes).joinToString(""){"%02x".format(it.toInt()and 255)}
private fun v8B64(s:String)=Base64.getUrlEncoder().withoutPadding().encodeToString(s.toByteArray())
private fun v8Unb64(s:String)=String(Base64.getUrlDecoder().decode(s))
