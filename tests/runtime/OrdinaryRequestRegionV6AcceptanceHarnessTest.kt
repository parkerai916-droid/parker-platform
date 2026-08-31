package parker.core.runtime

import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.security.MessageDigest
import java.time.Instant
import java.util.Base64
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import parker.composition.OpenAiApiCredential
import parker.core.interfaces.*
import kotlin.io.path.*
import kotlin.test.*

/**
 * OI10-only, explicitly detached acceptance instrument. It is never part of an ordinary lifecycle
 * task and never opens a production store. PREPARE is provider-free; EXECUTE is one fixture/call;
 * REPLAY is provider-free and consumes only the immutable acceptance record.
 */
class OrdinaryRequestRegionV6AcceptanceHarnessTest {
    private val enabled = System.getenv("OI10_ENABLED") == "true"
    private val action = System.getenv("OI10_ACTION") ?: "OFF"
    private val fixtureName = System.getenv("OI10_FIXTURE")
    private val root = Path.of(System.getenv("OI10_ACCEPTANCE_ROOT") ?: "/home/steve/parker-acceptance/oi10").toAbsolutePath().normalize()
    private val corpus = Path.of(System.getenv("OI10_CORPUS_ROOT") ?: "/tmp/oi10-corpus").toAbsolutePath().normalize()
    private val codec = OpenAiRequestRegionCodec()

    @Test fun `governed OI10 request-region v6 acceptance`() {
        assumeTrue(enabled, "detached OI10 acceptance not explicitly enabled")
        require(System.getenv("OI10_IMPLEMENTATION_COMMIT") == IMPLEMENTATION_COMMIT)
        require(System.getenv("OI10_EXPECTED_HEAD")?.matches(Regex("^[0-9a-f]{40}$")) == true)
        require(root != corpus && !root.startsWith(Path.of("/mnt/parker-data")) && !root.startsWith(Path.of("/data")))
        when (action) {
            "PREPARE" -> prepareAll()
            "EXECUTE" -> execute(requireNotNull(fixtureName).let(::fixture))
            "REPLAY" -> replay(requireNotNull(fixtureName).let(::fixture))
            "ASSESS_FAILURE" -> assessFailure(requireNotNull(fixtureName).let(::fixture))
            "REPLAY_FAILURE" -> replayFailure(requireNotNull(fixtureName).let(::fixture))
            else -> error("unsupported OI10 action")
        }
    }

    private fun prepareAll() {
        Files.createDirectories(root); private(root)
        require(FIXTURES.map { it.name }.toSet() == setOf("A", "B", "C", "D"))
        FIXTURES.forEach { f ->
            val source = corpus.resolve("${f.evidenceId}.evidence")
            require(source.isRegularFile()); val bytes = source.readBytes(); require(sha(bytes) == f.sha256)
            val prepared = prepare(f, bytes); val dir = root.resolve(f.name); Files.createDirectories(dir); private(dir)
            createOnce(dir.resolve("source.pdf"), bytes)
            val crops = dir.resolve("crops"); Files.createDirectories(crops); private(crops)
            prepared.request.regions.forEachIndexed { index, r -> createOnce(crops.resolve("%02d-%s.png".format(index + 1, r.id.value)), r.image.encodedBytes()) }
            val manifest = manifest(f, prepared)
            createOnce(dir.resolve("manifest.json"), envelope("oi10-frozen-manifest-v1", manifest))
            createOnce(dir.resolve("authority.json"), envelope("oi10-v6-acceptance-authority-v1", authority(f, prepared)))
            println("OI10_PREPARED=${f.name} source=${f.evidenceId} sourceRegions=${prepared.graphs.sumOf { it.regions.size }} requests=${prepared.request.regions.size} body=${prepared.bodyBytes} requestDigest=${prepared.requestDigest} manifestDigest=${sha(RegionJson.encode(manifest).toByteArray())}")
        }
    }

    private fun execute(f: Fixture) {
        val p = loadPrepared(f); val dir = root.resolve(f.name)
        verifyFrozen(dir, f, p)
        require(Files.isRegularFile(dir.resolve("authority.json")))
        require(!Files.exists(dir.resolve("attempt.json")) && !Files.exists(dir.resolve("response.json")) && !Files.exists(dir.resolve("assessment.json")))
        val totalAttempts = FIXTURES.count { Files.exists(root.resolve(it.name).resolve("attempt.json")) }
        require(totalAttempts < 4)
        val attempt = linkedMapOf<String,Any?>("format" to "oi10-provider-attempt-v1", "fixture" to f.name,
            "request_digest" to p.requestDigest, "maximum_fixture_attempts" to 1, "maximum_total_attempts" to 4,
            "implementation_commit" to IMPLEMENTATION_COMMIT, "state" to "PROVIDER_ATTEMPT_STARTING", "started_at" to Instant.now().toString())
        createOnce(dir.resolve("attempt.json"), envelope("oi10-provider-attempt-v1", attempt))

        val credential = OpenAiApiCredential.fromEnvironment(System.getenv("PARKER_OPENAI_API_KEY")) ?: error("credential absent")
        val body = codec.buildRequestBody(p.request)
        require(body.toByteArray().size == p.bodyBytes && body.contains("\"store\":false") && !body.contains("\"store\":true"))
        val response = runBlocking { JdkOpenAiResponsesTransport().execute(OpenAiResponsesTransportRequest(
            java.net.URI("https://api.openai.com/v1/responses"), 300_000, body, 20L * 1024L * 1024L, credential)) }
        val rawRecord = linkedMapOf<String,Any?>("format" to "oi10-raw-provider-response-v1", "fixture" to f.name,
            "request_digest" to p.requestDigest, "http_status" to response.statusCode, "received_at" to Instant.now().toString(),
            "raw_length" to response.body.size, "raw_sha256" to sha(response.body), "raw_base64" to Base64.getEncoder().encodeToString(response.body))
        createOnce(dir.resolve("response.json"), envelope("oi10-raw-provider-response-v1", rawRecord))
        require(response.statusCode in 200..299) { "provider HTTP ${response.statusCode}; preserved, no retry" }
        val parsed = parse(p.request, response.body)
        val ordered = RequestRegionSourceOrderReconstructor().reconstruct(p.request, parsed.result).getOrThrow()
        val derivative = RequestRegionDerivativeBinder().bind(p.request, parsed.result).getOrThrow()
        val assessment = assessment(f, p, parsed, ordered, derivative, "STRUCTURAL_PASS_FIDELITY_REVIEW_PENDING")
        createOnce(dir.resolve("assessment.json"), envelope("oi10-v6-assessment-v1", assessment))
        println("OI10_EXECUTED=${f.name} response=${parsed.responseId} rawSha=${sha(response.body)} blocks=${ordered.size} result=STRUCTURAL_PASS_FIDELITY_REVIEW_PENDING")
    }

    private fun replay(f: Fixture) {
        val p = loadPrepared(f); val dir = root.resolve(f.name); verifyFrozen(dir, f, p)
        val raw = objectField(readEnvelope(dir.resolve("response.json")), "record")
        require(raw.string("request_digest") == p.requestDigest)
        val bytes = Base64.getDecoder().decode(raw.string("raw_base64")); require(sha(bytes) == raw.string("raw_sha256"))
        val parsed = parse(p.request, bytes); val ordered = RequestRegionSourceOrderReconstructor().reconstruct(p.request, parsed.result).getOrThrow()
        val derivative = RequestRegionDerivativeBinder().bind(p.request, parsed.result).getOrThrow()
        val expected = assessment(f, p, parsed, ordered, derivative, "STRUCTURAL_PASS_FIDELITY_REVIEW_PENDING")
        val actual = objectField(readEnvelope(dir.resolve("assessment.json")), "record")
        require(RegionJson.encode(actual) == RegionJson.encode(expected))
        println("OI10_REPLAY=${f.name} requestDigest=${p.requestDigest} rawSha=${sha(bytes)} assessmentDigest=${sha(RegionJson.encode(actual).toByteArray())} PASS")
    }

    private fun assessFailure(f: Fixture) {
        val p=loadPrepared(f);val dir=root.resolve(f.name);verifyFrozen(dir,f,p)
        val raw=objectField(readEnvelope(dir.resolve("response.json")),"record")
        val bytes=Base64.getDecoder().decode(raw.string("raw_base64"));require(sha(bytes)==raw.string("raw_sha256"))
        @Suppress("UNCHECKED_CAST") val providerEnvelope=RegionJson.parse(bytes.toString(Charsets.UTF_8)) as Map<String,Any?>
        val text=(providerEnvelope["output"] as List<*>).asSequence().mapNotNull{it as? Map<*,*>}.flatMap{(it["content"] as? List<*>?:emptyList<Any?>()).asSequence()}
            .mapNotNull{it as? Map<*,*>}.first{it["type"]=="output_text"}["text"] as String
        @Suppress("UNCHECKED_CAST") val structured=RegionJson.parse(text) as Map<String,Any?>
        val validation=RequestRegionTranscriptionValidator().validate(p.request,structured)
        require(validation==RequestRegionValidationOutcome.Rejected(RequestRegionValidationRejection.MALFORMED))
        @Suppress("UNCHECKED_CAST") val block=(structured["blocks"] as List<Map<String,Any?>>).single()
        val literal=block["literal_text"] as String;@Suppress("UNCHECKED_CAST") val observations=block["visual_observations"] as List<Map<String,Any?>>
        val assessment=linkedMapOf<String,Any?>("format" to "oi10-v6-fidelity-failure-assessment-v1","fixture" to f.name,"request_digest" to p.requestDigest,
            "raw_sha256" to sha(bytes),"structured_sha256" to sha(RegionJson.encode(structured).toByteArray()),"provider_response_id" to providerEnvelope["id"],
            "provider_reported_model" to providerEnvelope["model"],"literal_code_points" to literal.codePointCount(0,literal.length),"literal_fidelity" to "EXACT_VISUAL_REVIEW",
            "validation_result" to "MALFORMED","failure_boundary" to "VISUAL_OBSERVATION_SPAN_OUT_OF_RANGE_AND_MISALIGNED",
            "observation_count" to observations.size,"last_observation" to observations.last(),"classification" to "FAIL_FIDELITY",
            "provider_calls_total" to 1,"retry_count" to 0,"remaining_fixtures_called" to false,"production_promotion" to false,"ordinary_production_ingestion" to false)
        createOnce(dir.resolve("assessment.json"),envelope("oi10-v6-fidelity-failure-assessment-v1",assessment))
        println("OI10_FAILURE_ASSESSED=${f.name} classification=FAIL_FIDELITY boundary=VISUAL_OBSERVATION_SPAN_OUT_OF_RANGE_AND_MISALIGNED calls=1 retries=0")
    }

    private fun replayFailure(f:Fixture){val p=loadPrepared(f);val dir=root.resolve(f.name);verifyFrozen(dir,f,p)
        val raw=objectField(readEnvelope(dir.resolve("response.json")),"record");val bytes=Base64.getDecoder().decode(raw.string("raw_base64"));require(sha(bytes)==raw.string("raw_sha256"))
        @Suppress("UNCHECKED_CAST") val providerEnvelope=RegionJson.parse(bytes.toString(Charsets.UTF_8)) as Map<String,Any?>
        val text=(providerEnvelope["output"] as List<*>).asSequence().mapNotNull{it as? Map<*,*>}.flatMap{(it["content"] as? List<*>?:emptyList<Any?>()).asSequence()}.mapNotNull{it as? Map<*,*>}.first{it["type"]=="output_text"}["text"] as String
        @Suppress("UNCHECKED_CAST") val structured=RegionJson.parse(text) as Map<String,Any?>;require(RequestRegionTranscriptionValidator().validate(p.request,structured)==RequestRegionValidationOutcome.Rejected(RequestRegionValidationRejection.MALFORMED))
        val assessment=objectField(readEnvelope(dir.resolve("assessment.json")),"record");require(assessment.string("classification")=="FAIL_FIDELITY");require(assessment.string("request_digest")==p.requestDigest);require(assessment.string("raw_sha256")==sha(bytes));require(assessment.string("failure_boundary")=="VISUAL_OBSERVATION_SPAN_OUT_OF_RANGE_AND_MISALIGNED")
        println("OI10_FAILURE_REPLAY=${f.name} requestDigest=${p.requestDigest} rawSha=${sha(bytes)} classification=FAIL_FIDELITY PASS")}

    private data class Parsed(val responseId:String,val model:String,val structured:Map<String,Any?>,val result:RequestRegionTranscriptionResult)
    private fun parse(request: RequestRegionTranscriptionRequest, bytes: ByteArray): Parsed {
        @Suppress("UNCHECKED_CAST") val envelope = RegionJson.parse(bytes.toString(Charsets.UTF_8)) as? Map<String,Any?> ?: error("malformed envelope")
        val responseId = envelope["id"] as? String ?: error("missing response id"); val model = envelope["model"] as? String ?: error("missing model")
        require(model == REQUEST_REGION_MODEL)
        val output = envelope["output"] as? List<*> ?: error("missing output")
        val text = output.asSequence().mapNotNull { it as? Map<*,*> }.filter { it["type"] == "message" }
            .flatMap { (it["content"] as? List<*> ?: emptyList<Any?>()).asSequence() }.mapNotNull { it as? Map<*,*> }
            .firstOrNull { it["type"] == "output_text" }?.get("text") as? String ?: error("missing structured output")
        @Suppress("UNCHECKED_CAST") val structured = RegionJson.parse(text) as? Map<String,Any?> ?: error("malformed structured output")
        val validation = RequestRegionTranscriptionValidator().validate(request, structured)
        val valid = validation as? RequestRegionValidationOutcome.Valid ?: run {
            @Suppress("UNCHECKED_CAST")
            val withoutObservations = structured.toMutableMap().also { top ->
                top["blocks"] = (top["blocks"] as List<Map<String,Any?>>).map { it.toMutableMap().also { b -> b["visual_observations"] = emptyList<Any?>() } }
            }
            val diagnostic = RequestRegionTranscriptionValidator().validate(request, withoutObservations)
            error("v6 validation rejected: ${(validation as RequestRegionValidationOutcome.Rejected).reason}; without_observations=$diagnostic")
        }
        val provenance = valid.result.providerProvenance
        require(provenance.providerReportedModel == model && provenance.providerResponseId == responseId)
        return Parsed(responseId, model, structured, valid.result)
    }

    private data class Fixture(val name:String,val evidenceId:String,val sha256:String,val role:String)
    private data class Prepared(val request:RequestRegionTranscriptionRequest,val pages:List<AuthoritativePageRepresentation>,val graphs:List<SourceRegionOrderGraph>,val bodyBytes:Int,val requestDigest:String)
    private fun fixture(name:String)=FIXTURES.single { it.name == name }
    private fun loadPrepared(f:Fixture):Prepared { val bytes=root.resolve(f.name).resolve("source.pdf").readBytes();require(sha(bytes)==f.sha256);return prepare(f,bytes) }
    private fun prepare(f:Fixture,bytes:ByteArray):Prepared {
        val renderer=DeterministicSourcePageRenderer();val deriver=DeterministicSourceRegionDeriver();val profile=PageRenderProfile("authoritative-page-region-raster-v1",1,300)
        fun page(n:Int)=assertIs<SourcePageRepresentationOutcome.Created>(renderer.render(SourcePageRenderRequest(EvidenceArtifactId(f.evidenceId),f.sha256,"application/pdf",bytes,n,profile))).representation
        val first=page(1);val pages=(1..first.provenance.declaredPageCount).map { if(it==1) first else page(it) }
        val graphs=pages.map { assertIs<SourceRegionDerivationOutcome.Derived>(deriver.derive(it)).graph }
        val shaped=assertIs<RequestRegionShapingOutcome.Shaped>(DeterministicCompleteSetRequestRegionShaper(renderer).shape(pages,graphs))
        val correlationId=if(f.name=="B") "oi9-offline-fixture" else "oi10-${f.name.lowercase()}-acceptance"
        val request=RequestRegionTranscriptionRequest(correlationId,shaped.regions);val body=codec.buildRequestBody(request).toByteArray().size
        require(body <= REQUEST_REGION_BODY_MAXIMUM_BYTES);return Prepared(request,pages,graphs,body,codec.requestDigest(request))
    }

    private fun manifest(f:Fixture,p:Prepared)=linkedMapOf<String,Any?>("fixture" to f.name,"role" to f.role,"evidence_artifact_id" to f.evidenceId,"source_sha256" to f.sha256,
        "page_count" to p.pages.size,"page_digests" to p.pages.map{it.provenance.canonicalPixelDigest.value},"source_region_count" to p.graphs.sumOf{it.regions.size},
        "request_region_count" to p.request.regions.size,"request_region_ids" to p.request.regions.map{it.id.value},"request_order" to p.request.regions.map{it.id.value},
        "memberships" to p.request.regions.map{it.constituentIds.map(SourceRegionId::value)},"bounds" to p.request.regions.map{listOf(it.bounds.left,it.bounds.top,it.bounds.rightExclusive,it.bounds.bottomExclusive)},
        "crop_digests" to p.request.regions.map{it.cropDigest.value},"image_digests" to p.request.regions.map{it.image.encodedSha256},"body_bytes" to p.bodyBytes,
        "request_digest" to p.requestDigest,"encoded_manifest_digest" to sha(codec.buildRequestBody(p.request).toByteArray()),"source_truth_basis" to "AUTHORITATIVE_CROP_PIXELS_VISUAL_REVIEW_ONLY",
        "review_questions" to listOf("literal fidelity","omission","insertion","substitution","Parker order","uncertainty","visual anchors","request/page binding","constituent provenance"))
    private fun authority(f:Fixture,p:Prepared)=linkedMapOf<String,Any?>("authority_id" to "authority-ordinary-ingestion-10-${f.name.lowercase()}","programme_unit" to "ORDINARY-INGESTION-10",
        "capability_id" to ORDINARY_REQUEST_REGION_CAPABILITY_ID,"capability_digest" to OrdinaryRequestRegionCapability().digest(),"lifecycle" to "ACCEPTANCE_PENDING",
        "implementation_commit" to IMPLEMENTATION_COMMIT,"expected_head" to System.getenv("OI10_EXPECTED_HEAD"),"fixture" to f.name,"request_digest" to p.requestDigest,
        "maximum_fixture_attempts" to 1,"maximum_total_attempts" to 4,"provider" to REQUEST_REGION_PROVIDER,"model" to REQUEST_REGION_MODEL,"endpoint" to "https://api.openai.com/v1/responses",
        "store" to false,"reasoning" to "none","schema_id" to REQUEST_REGION_SCHEMA_ID,"wire_version" to 6,"adapter_id" to REQUEST_REGION_ADAPTER_ID,"adapter_version" to REQUEST_REGION_ADAPTER_VERSION)
    private fun assessment(f:Fixture,p:Prepared,x:Parsed,ordered:List<RequestRegionTranscriptionBlock>,d:RequestRegionTranscriptionDerivative,result:String)=linkedMapOf<String,Any?>(
        "format" to "oi10-v6-assessment-v1","fixture" to f.name,"request_digest" to p.requestDigest,"response_id" to x.responseId,"provider_reported_model" to x.model,
        "structured_sha256" to sha(RegionJson.encode(x.structured).toByteArray()),"block_count" to ordered.size,"parker_order_ids" to ordered.map{it.requestRegionId.value},
        "provider_ordinals_in_parker_order" to ordered.map{it.providerReturnedOrdinal},"derivative_capability_id" to d.capabilityId,
        "derivative_memberships" to d.blocksInParkerOrder.map{it.constituentSourceRegionIds},"result" to result)
    private fun verifyFrozen(dir:Path,f:Fixture,p:Prepared){val actual=objectField(readEnvelope(dir.resolve("manifest.json")),"record");require(RegionJson.encode(actual)==RegionJson.encode(manifest(f,p)));if(f.name=="B"){
        require(p.graphs.map{it.regions.size}==listOf(16,14,6));require(p.request.regions.groupingBy{it.pageNumber}.eachCount().toSortedMap().values.toList()==listOf(14,12,6));require(p.requestDigest==B_REQUEST_DIGEST);require(p.bodyBytes==1_169_528);require(sha(codec.buildRequestBody(p.request).toByteArray())==B_MANIFEST_DIGEST)}}
    private fun envelope(format:String,record:Map<String,Any?>):ByteArray{val digest=sha(RegionJson.encode(record).toByteArray());return RegionJson.encode(linkedMapOf("format" to format,"record" to record,"record_sha256" to digest)).toByteArray()}
    private fun readEnvelope(path:Path):Map<String,Any?>{@Suppress("UNCHECKED_CAST") val top=RegionJson.parse(path.readText()) as Map<String,Any?>;val record=objectField(top,"record");require(top.string("record_sha256")==sha(RegionJson.encode(record).toByteArray()));return top}
    @Suppress("UNCHECKED_CAST") private fun objectField(m:Map<String,Any?>,k:String)=m[k] as? Map<String,Any?>?:error("missing $k")
    private fun Map<String,Any?>.string(k:String)=this[k] as? String?:error("missing $k")
    private fun createOnce(path:Path,bytes:ByteArray){require(!Files.exists(path)){"immutable OI10 record already exists: $path"};FileChannel.open(path,StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE).use{c->val b=ByteBuffer.wrap(bytes);while(b.hasRemaining())c.write(b);c.force(true)};private(path);if(!System.getProperty("os.name").startsWith("Windows",true))FileChannel.open(path.parent,StandardOpenOption.READ).use{it.force(true)}}
    private fun private(path:Path)=runCatching{Files.setPosixFilePermissions(path,if(path.isDirectory())setOf(java.nio.file.attribute.PosixFilePermission.OWNER_READ,java.nio.file.attribute.PosixFilePermission.OWNER_WRITE,java.nio.file.attribute.PosixFilePermission.OWNER_EXECUTE)else setOf(java.nio.file.attribute.PosixFilePermission.OWNER_READ,java.nio.file.attribute.PosixFilePermission.OWNER_WRITE))}
    private fun sha(bytes:ByteArray)=MessageDigest.getInstance("SHA-256").digest(bytes).joinToString(""){"%02x".format(it.toInt()and 255)}
    companion object{
        const val IMPLEMENTATION_COMMIT="87764757a35a6df8d2491a0fe69608bafee5bca0"
        const val B_REQUEST_DIGEST="b2f50f15d0f80eb66590a160de6e4a910609545214d89f04b67f66c671190968"
        const val B_MANIFEST_DIGEST="11baaf1818581d3e50c1a6a3069e52407eacb9e06a52d4131d8e484a14af9851"
        private val FIXTURES=listOf(
            Fixture("A","evidence-28c076ae-0666-45ef-a04e-297d74a639c9","64039b3200b34c67ce1f993c1cb1f98a115390d6aa25541ceb8a60e674441149","singleton_non_coalesced"),
            Fixture("B","evidence-4c6f2ee8-2f62-47be-bd7a-946c744b2766","ce8bd4b53d8b007026575974014e71f648f045bf3970b0e984605cf842a7b4a5","sprint2_36_to_32"),
            Fixture("C","evidence-0275472f-535a-4cf1-b30d-f45ac7684743","7373ad403b4fae5bf5c777deb8524eaa3ba38594ce9fabfa8fcbce22fbd33182","source_order_sensitive"),
            Fixture("D","evidence-230b3f36-ff88-4adf-a224-081d2fd8fdee","12b6f1b7c18d1ee3623c5dcf8da0d1c9f61b9bd89dde9e86a74e6398157df885","mixed_structure_coalesced"))
    }
}
