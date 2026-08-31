package parker.core.runtime

import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.Base64
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import kotlin.test.*
import parker.core.interfaces.*

class OrdinaryRequestRegionV8TruthfulContractTest {
    @Test fun `v8 is distinct acceptance-pending and provider observations are absent`() {
        val capability=OrdinaryRequestRegionV8Capability()
        assertEquals(ORDINARY_REQUEST_REGION_V8_CAPABILITY_ID,capability.capabilityId)
        assertEquals(RequestRegionV8CapabilityLifecycle.ACCEPTANCE_PENDING,capability.lifecycle)
        assertNotEquals(OrdinaryRequestRegionV7Capability().digest(),capability.digest())
        assertFalse(REQUEST_REGION_V8_SCHEMA_SOURCE.contains("visual_observations"))
        assertContains(REQUEST_REGION_V8_INSTRUCTION,"Never")
        assertContains(REQUEST_REGION_V8_INSTRUCTION,"character-level visual observations")
        println("OI10R3_V8 capabilityDigest=${capability.digest()} instructionDigest=$REQUEST_REGION_V8_INSTRUCTION_SHA256 schemaDigest=$REQUEST_REGION_V8_SCHEMA_SHA256")
    }

    @Test fun `feasibility projection preserves literals identity order uncertainty and drops anchors`() {
        val v7=linkedMapOf<String,Any?>("correlation_id" to "x","transcription_profile_id" to REQUEST_REGION_V7_PROFILE_ID,
            "schema_id" to REQUEST_REGION_V7_SCHEMA_ID,"schema_version" to 7,"provider_provenance" to linkedMapOf("provider" to "OpenAI",
                "requested_model" to REQUEST_REGION_MODEL,"provider_reported_model" to REQUEST_REGION_MODEL,"provider_response_id" to null,
                "adapter_id" to REQUEST_REGION_ADAPTER_ID,"adapter_version" to REQUEST_REGION_V7_ADAPTER_VERSION,"parser_id" to REQUEST_REGION_PARSER_ID,"parser_version" to REQUEST_REGION_V7_PARSER_VERSION),
            "blocks" to listOf(linkedMapOf("request_region_id" to "a".repeat(64),"page_number" to 1,"literal_text" to "A\nB","status" to "TRANSCRIBED",
                "uncertainties" to emptyList<Any>(),"warnings" to emptyList<Any>(),"provider_returned_ordinal" to 1,
                "visual_observations" to listOf(mapOf("kind" to "LINE_BREAK","start_code_point" to 99,"end_code_point_exclusive" to 99)))))
        val projected=RequestRegionV8FeasibilityProjector.projectHistoricalV7(v7)
        @Suppress("UNCHECKED_CAST") val block=(projected["blocks"] as List<Map<String,Any?>>).single()
        assertEquals("A\nB",block["literal_text"]);assertEquals("a".repeat(64),block["request_region_id"])
        assertFalse(block.containsKey("visual_observations"));assertEquals(REQUEST_REGION_V8_SCHEMA_ID,projected["schema_id"])
    }

    @Test fun `preserved OI10R2 responses replay without converting anchors into evidence`() {
        val paths=listOf("OI10R2_A_RESPONSE_PATH","OI10R2_B_RESPONSE_PATH").mapNotNull{System.getenv(it)?.let(Path::of)}
        assumeTrue(paths.size==2&&paths.all(Files::isRegularFile),"preserved OI10R2 responses not supplied")
        paths.forEach { path ->
            @Suppress("UNCHECKED_CAST") val outer=RegionJson.parse(Files.readString(path)) as Map<String,Any?>
            @Suppress("UNCHECKED_CAST") val record=outer["record"] as Map<String,Any?>;val raw=Base64.getDecoder().decode(record["raw_base64"] as String)
            assertEquals(record["raw_sha256"],sha(raw));@Suppress("UNCHECKED_CAST") val envelope=RegionJson.parse(raw.toString(Charsets.UTF_8)) as Map<String,Any?>
            @Suppress("UNCHECKED_CAST") val output=envelope["output"] as List<Map<String,Any?>>;@Suppress("UNCHECKED_CAST") val content=output.single{it["type"]=="message"}["content"] as List<Map<String,Any?>>
            @Suppress("UNCHECKED_CAST") val v7=RegionJson.parse(content.single{it["type"]=="output_text"}["text"] as String) as Map<String,Any?>;val v8=RequestRegionV8FeasibilityProjector.projectHistoricalV7(v7)
            @Suppress("UNCHECKED_CAST") val oldBlocks=v7["blocks"] as List<Map<String,Any?>>;@Suppress("UNCHECKED_CAST") val newBlocks=v8["blocks"] as List<Map<String,Any?>>
            assertEquals(oldBlocks.map{it["request_region_id"] to it["literal_text"]},newBlocks.map{it["request_region_id"] to it["literal_text"]})
            assertTrue(newBlocks.none{it.containsKey("visual_observations")})
        }
    }
    @Test fun `Sprint 2 v8 request is deterministic complete and bounded`() {
        val path=System.getenv("OI10R2_B_SOURCE_PATH")?.let(Path::of);assumeTrue(path!=null&&Files.isRegularFile(path),"Sprint 2 source not supplied")
        val bytes=Files.readAllBytes(path);val sourceSha=sha(bytes);assertEquals("ce8bd4b53d8b007026575974014e71f648f045bf3970b0e984605cf842a7b4a5",sourceSha)
        val canonical=CanonicalRequestRegionV8Builder().build(EvidenceArtifactId("evidence-4c6f2ee8-2f62-47be-bd7a-946c744b2766"),sourceSha,"application/pdf",bytes,"oi10r4-sprint2-offline")
        val graphs=canonical.sourceGraphs;val request=canonical.request;val codec=OpenAiRequestRegionV8Codec();val body1=canonical.providerBody
        assertEquals(listOf(16,14,6),graphs.map{it.regions.size});assertEquals(listOf(14,12,6),request.regions.groupingBy{it.pageNumber}.eachCount().toSortedMap().values.toList())
        assertEquals(36,request.regions.flatMap{it.constituentIds}.distinct().size);assertEquals(body1,codec.buildRequestBody(request));assertTrue(body1.toByteArray().size<=REQUEST_REGION_BODY_MAXIMUM_BYTES)
        fun block(r:RequestRegion,i:Int)=linkedMapOf<String,Any?>("request_region_id" to r.id.value,"page_number" to r.pageNumber,"literal_text" to if(i==0)"A😀e\u0301\r\nB\n" else "literal-$i","status" to "TRANSCRIBED",
            "uncertainties" to if(i==0)listOf(linkedMapOf("category" to "AMBIGUOUS","description" to "visual ambiguity","alternatives" to listOf("A","B"),"provider_confidence" to "low"))else emptyList<Any>(),"warnings" to emptyList<Any>(),"provider_returned_ordinal" to i+1)
        fun wire(blocks:List<Map<String,Any?>>)=linkedMapOf<String,Any?>("correlation_id" to request.correlationId,"transcription_profile_id" to REQUEST_REGION_V8_PROFILE_ID,"schema_id" to REQUEST_REGION_V8_SCHEMA_ID,"schema_version" to 8,
            "provider_provenance" to linkedMapOf("provider" to REQUEST_REGION_PROVIDER,"requested_model" to REQUEST_REGION_MODEL,"provider_reported_model" to REQUEST_REGION_MODEL,"provider_response_id" to null,"adapter_id" to REQUEST_REGION_ADAPTER_ID,"adapter_version" to REQUEST_REGION_V8_ADAPTER_VERSION,"parser_id" to REQUEST_REGION_PARSER_ID,"parser_version" to REQUEST_REGION_V8_PARSER_VERSION),"blocks" to blocks)
        val canonicalBlocks:List<Map<String,Any?>> = request.regions.mapIndexed{i,r->block(r,i)};val reversed=canonicalBlocks.reversed();val valid=assertIs<RequestRegionV8ValidationOutcome.Valid>(RequestRegionV8StructuredValidator().validate(request,wire(reversed)))
        assertEquals("A😀e\u0301\r\nB\n",valid.result.blocksInProviderOrder.last().literalText);assertEquals("visual ambiguity",valid.result.blocksInProviderOrder.last().uncertainties.single().description)
        val derivative=RequestRegionV8DerivativeBinder().bind(request,valid.result).getOrThrow();assertEquals(request.regions.map{it.id.value},derivative.blocksInParkerOrder.map{it.requestRegionId});assertEquals(36,derivative.blocksInParkerOrder.flatMap{it.constituentSourceRegionIds}.distinct().size)
        assertTrue(derivative.blocksInParkerOrder.none{it.literalText?.contains("visual_observations")==true});assertEquals(derivative.canonicalDigest,RequestRegionV8DerivativeBinder().bind(request,valid.result).getOrThrow().canonicalDigest)
        assertIs<RequestRegionV8ValidationOutcome.Rejected>(RequestRegionV8StructuredValidator().validate(request,wire(canonicalBlocks.dropLast(1))))
        assertIs<RequestRegionV8ValidationOutcome.Rejected>(RequestRegionV8StructuredValidator().validate(request,wire(canonicalBlocks.dropLast(1)+canonicalBlocks.first())))
        val unknown=canonicalBlocks.toMutableList();unknown[0]=unknown[0].toMutableMap().also{it["request_region_id"]="f".repeat(64)};assertIs<RequestRegionV8ValidationOutcome.Rejected>(RequestRegionV8StructuredValidator().validate(request,wire(unknown)))
        val legacy=canonicalBlocks.toMutableList();legacy[0]=legacy[0].toMutableMap().also{it["visual_observations"]=emptyList<Any>()};assertIs<RequestRegionV8ValidationOutcome.Rejected>(RequestRegionV8StructuredValidator().validate(request,wire(legacy)))
        println("OI10R4_SPRINT2 sourceRegions=36 requestRegions=32 bodyBytes=${body1.toByteArray().size} requestDigest=${codec.requestDigest(request)} manifestDigest=${sha(body1.toByteArray())}")
    }
    @Test fun `canonical builder proves R4 R5 divergence is only governed correlation id and repeats ten times`() {
        val path=System.getenv("OI10R2_B_SOURCE_PATH")?.let(Path::of);assumeTrue(path!=null&&Files.isRegularFile(path),"Sprint 2 source not supplied")
        val bytes=Files.readAllBytes(path);val sha="ce8bd4b53d8b007026575974014e71f648f045bf3970b0e984605cf842a7b4a5";val id=EvidenceArtifactId("evidence-4c6f2ee8-2f62-47be-bd7a-946c744b2766");val builder=CanonicalRequestRegionV8Builder()
        val r4=builder.build(id,sha,"application/pdf",bytes,"oi10r4-sprint2-offline");val r5=builder.build(id,sha,"application/pdf",bytes,"oi10r5-v8-b-acceptance")
        assertEquals("a5ef4672e07493ae870cc798fe24a87651ffc007446e40716f48ea5e62356d03",r4.requestBindingSha256);assertEquals("8bf1c47066c6c13bd2a72757712ceebe5497b3929153b0c5befc0a56c362690a",r4.providerBodySha256);assertEquals(1165499,r4.providerBody.toByteArray().size)
        assertEquals("d87de41366052eff66b45ede428536e56dd54df8a9efcfdbab4dce41a6858768",r5.requestBindingSha256);assertEquals("826119f3e767c5da008f9237c2111f21013254e8d8553f22b48ba26e66ce6f7e",r5.providerBodySha256);assertEquals(1165499,r5.providerBody.toByteArray().size)
        fun ladder(x:CanonicalRequestRegionV8Construction)=listOf(x.pages.map{it.provenance.canonicalPixelDigest.value},x.sourceGraphs.flatMap{it.regions}.map{it.id.value},x.request.regions.map{it.id.value},x.request.regions.map{it.constituentIds.map(SourceRegionId::value)},x.request.regions.map{it.image.encodedSha256})
        assertEquals(ladder(r4),ladder(r5))
        val r4Wire=RegionJson.parse(r4.providerBody) as Map<*,*>;val r5Wire=RegionJson.parse(r5.providerBody) as Map<*,*>;assertEquals(r4Wire.keys,r5Wire.keys);assertNotEquals(r4Wire,r5Wire)
        repeat(10){val repeated=CanonicalRequestRegionV8Builder().build(id,sha,"application/pdf",bytes,"oi10r4-sprint2-offline");assertEquals(ladder(r4),ladder(repeated));assertEquals(r4.providerBody,repeated.providerBody);assertEquals(r4.requestBindingSha256,repeated.requestBindingSha256);assertEquals(r4.providerBodySha256,repeated.providerBodySha256)}
        assertEquals("parker.request-region-v8.canonical-binding.v1",CanonicalRequestRegionV8Construction.REQUEST_DIGEST_DOMAIN);assertEquals("parker.request-region-v8.provider-body.utf8.v1",CanonicalRequestRegionV8Construction.BODY_DIGEST_DOMAIN)
    }

    @Test fun `provider ordinal is preserved forensic metadata while governed identity and Parker order remain authoritative`() {
        val path=Path.of("tests/fixtures/document-ingestion-bakeoff/fixtures/02-multicolumn-complex.pdf")
        val bytes=Files.readAllBytes(path);val sourceSha=sha(bytes)
        val request=CanonicalRequestRegionV8Builder().build(EvidenceArtifactId("evidence-oi11r2a-provider-ordinal"),sourceSha,"application/pdf",bytes,"oi11r2a-provider-ordinal").request
        assertTrue(request.regions.size>=3)
        fun block(r:RequestRegion,i:Int,ordinal:Int):Map<String,Any?> = linkedMapOf("request_region_id" to r.id.value,"page_number" to r.pageNumber,
            "literal_text" to "literal-$i","status" to "TRANSCRIBED","uncertainties" to emptyList<Any>(),"warnings" to emptyList<Any>(),"provider_returned_ordinal" to ordinal)
        fun blocks(ordinals:List<Int>)=request.regions.mapIndexed{i,r->block(r,i,ordinals[i])}
        fun wire(providerBlocks:List<Map<String,Any?>>)=linkedMapOf<String,Any?>("correlation_id" to request.correlationId,
            "transcription_profile_id" to REQUEST_REGION_V8_PROFILE_ID,"schema_id" to REQUEST_REGION_V8_SCHEMA_ID,"schema_version" to 8,
            "provider_provenance" to linkedMapOf("provider" to REQUEST_REGION_PROVIDER,"requested_model" to REQUEST_REGION_MODEL,
                "provider_reported_model" to REQUEST_REGION_MODEL,"provider_response_id" to "resp-offline","adapter_id" to REQUEST_REGION_ADAPTER_ID,
                "adapter_version" to REQUEST_REGION_V8_ADAPTER_VERSION,"parser_id" to REQUEST_REGION_PARSER_ID,"parser_version" to REQUEST_REGION_V8_PARSER_VERSION),
            "blocks" to providerBlocks)
        fun valid(providerBlocks:List<Map<String,Any?>>)=assertIs<RequestRegionV8ValidationOutcome.Valid>(RequestRegionV8StructuredValidator().validate(request,wire(providerBlocks))).result
        fun derivative(providerBlocks:List<Map<String,Any?>>)=RequestRegionV8DerivativeBinder().bind(request,valid(providerBlocks)).getOrThrow()

        val canonicalOrdinals=request.regions.indices.map{it+1};valid(blocks(canonicalOrdinals))
        val reversedOrdinals=canonicalOrdinals.reversed();val reversedOrdinalResult=valid(blocks(reversedOrdinals))
        assertEquals(reversedOrdinals,reversedOrdinalResult.blocksInProviderOrder.map{it.providerReturnedOrdinal})
        val nonMonotonicOrdinals=request.regions.indices.map{if(it%3==0)2 else if(it%3==1)1 else 3}
        val nonMonotonicResult=valid(blocks(nonMonotonicOrdinals))
        assertEquals(nonMonotonicOrdinals,nonMonotonicResult.blocksInProviderOrder.map{it.providerReturnedOrdinal})
        val repeatedOrdinals=List(request.regions.size){1};assertEquals(repeatedOrdinals,valid(blocks(repeatedOrdinals)).blocksInProviderOrder.map{it.providerReturnedOrdinal})

        val canonicalDerivative=derivative(blocks(canonicalOrdinals));val variedDerivative=derivative(blocks(nonMonotonicOrdinals).reversed())
        assertEquals(request.regions.map{it.id.value},variedDerivative.blocksInParkerOrder.map{it.requestRegionId})
        assertEquals(request.regions.map{it.pageNumber},variedDerivative.blocksInParkerOrder.map{it.pageNumber})
        assertEquals(request.regions.map{it.constituentIds.map(SourceRegionId::value)},variedDerivative.blocksInParkerOrder.map{it.constituentSourceRegionIds})
        assertEquals(canonicalDerivative.blocksInParkerOrder,variedDerivative.blocksInParkerOrder)
        assertEquals(canonicalDerivative.canonicalDigest,variedDerivative.canonicalDigest)

        val canonicalBlocks=blocks(canonicalOrdinals)
        assertIs<RequestRegionV8ValidationOutcome.Rejected>(RequestRegionV8StructuredValidator().validate(request,wire(canonicalBlocks.dropLast(1))))
        assertIs<RequestRegionV8ValidationOutcome.Rejected>(RequestRegionV8StructuredValidator().validate(request,wire(canonicalBlocks.dropLast(1)+canonicalBlocks.first())))
        val unknown=canonicalBlocks.toMutableList();unknown[0]=unknown[0].toMutableMap().also{it["request_region_id"]="f".repeat(64);it["provider_returned_ordinal"]=1}
        assertIs<RequestRegionV8ValidationOutcome.Rejected>(RequestRegionV8StructuredValidator().validate(request,wire(unknown)))
        val missingIdentity=canonicalBlocks.toMutableList();missingIdentity[0]=missingIdentity[0].filterKeys{it!="request_region_id"}
        assertIs<RequestRegionV8ValidationOutcome.Rejected>(RequestRegionV8StructuredValidator().validate(request,wire(missingIdentity)))
    }
    private fun sha(bytes:ByteArray)=MessageDigest.getInstance("SHA-256").digest(bytes).joinToString(""){"%02x".format(it.toInt()and 255)}
}
