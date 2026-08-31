package parker.core.runtime

import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.junit.jupiter.api.Test
import parker.core.interfaces.*
import kotlin.test.*

class OrdinaryRequestRegionV6Test {
    private val renderer = DeterministicSourcePageRenderer()

    @Test fun `v6 is distinct acceptance-pending capability and v5 identities remain unchanged`() {
        val v6 = OrdinaryRequestRegionCapability()
        assertEquals("ordinary-external-request-region-transcription-v6", v6.capabilityId)
        assertEquals(RequestRegionCapabilityLifecycle.ACCEPTANCE_PENDING, v6.lifecycle)
        assertEquals(32, v6.maximumRequestRegions); assertEquals(16_777_216, v6.maximumBodyBytes); assertFalse(v6.batching)
        assertEquals(64,v6.digest().length);assertEquals(v6.digest(),OrdinaryRequestRegionCapability().digest())
        assertEquals("ordinary-external-region-transcription-v5", ORDINARY_REGION_CAPABILITY_ID)
        assertEquals(5, REGION_TRANSCRIPTION_WIRE_VERSION)
        assertEquals("4.0.0", OPENAI_REGION_ADAPTER_VERSION)
    }

    @Test fun `hybrid shaping maps 1 and 32 one-to-one and 33 deterministically to 32`() {
        listOf(1 to 1, 32 to 32, 33 to 32).forEach { (sourceCount, expected) ->
            val fixture = fixture(listOf(sourceCount)); val first = shaped(fixture); val second = shaped(fixture)
            assertEquals(expected, first.regions.size); assertEquals(signature(first), signature(second))
            assertEquals(sourceCount, first.regions.sumOf { it.constituentIds.size })
            assertEquals(sourceCount, first.regions.flatMap { it.constituentIds }.distinct().size)
            if (sourceCount <= 32) assertTrue(first.regions.all { it.constituentIds.size == 1 })
        }
    }

    @Test fun `Sprint 2 shape 16 14 6 produces exact quotas complete coverage and stable request artifacts`() {
        val fixture = fixture(listOf(16, 14, 6)); val codec = OpenAiRequestRegionCodec()
        val repetitions = (1..10).map {
            val result = shaped(fixture); val request = RequestRegionTranscriptionRequest("attempt-sprint2", result.regions)
            listOf(result.regions.map { r -> r.id.value to r.constituentIds.map { it.value } }, codec.buildRequestBody(request), codec.requestDigest(request))
        }
        assertEquals(1, repetitions.distinct().size)
        val result = shaped(fixture)
        assertEquals(listOf(14, 12, 6), result.regions.groupingBy { it.pageNumber }.eachCount().toSortedMap().values.toList())
        assertEquals(32, result.regions.size); assertEquals(36, result.regions.sumOf { it.constituentIds.size })
        assertEquals(36, result.regions.flatMap { it.constituentIds }.distinct().size)
        val request = RequestRegionTranscriptionRequest("attempt-sprint2", result.regions)
        assertTrue(codec.buildRequestBody(request).toByteArray().size < REQUEST_REGION_BODY_MAXIMUM_BYTES)
        assertEquals(result.sourceOrder, result.regions.flatMap { it.constituentIds })
    }

    @Test fun `complete-set validator rejects missing duplicate unknown wrong-page cross-page non-adjacent and bounds`() {
        val fixture = fixture(listOf(4, 2)); val result = shaped(fixture); val validator = CompleteRequestRegionSetValidator()
        assertTrue(validator.validate(fixture.graphs, result.regions).isSuccess)
        assertContains(validator.validate(fixture.graphs, result.regions.dropLast(1)).exceptionOrNull()!!.message!!, "missing")
        assertContains(validator.validate(fixture.graphs, result.regions + result.regions.last()).exceptionOrNull()!!.message!!, "duplicate")
        val first = result.regions.first()
        val duplicateMember = assertFails { first.copy(constituentSourceRegions = listOf(first.constituentSourceRegions.first(), first.constituentSourceRegions.first())) }
        assertNotNull(duplicateMember)
        val wrongPage = fixture.graphs[1].regions.first()
        assertFails { first.copy(constituentSourceRegions = listOf(wrongPage), bounds = wrongPage.bounds) }
        val nonAdjacent = listOf(fixture.graphs[0].regions[0], fixture.graphs[0].regions[2])
        val union = unionRequestRegionBounds(nonAdjacent.map { it.bounds }); val page = fixture.pages[0]; val crop = renderer.crop(page, union)
        val image = image(page, crop)
        val malformed = RequestRegion(requestRegionIdentity(page, nonAdjacent, union, crop.canonicalPixelDigest), first.sourceEvidenceArtifactId,
            first.sourceSha256, page.id, 1, page.provenance.pixelDimensions, union, crop.canonicalPixelDigest,
            SourceRegionStructuralClass.TEXT_LIKE, first.derivationProfileId, 1, nonAdjacent, image)
        assertContains(validator.validate(fixture.graphs, listOf(malformed) + result.regions.drop(3)).exceptionOrNull()!!.message!!, "adjacent")
        val unknownSource = fixture.graphs[0].regions[0].copy(id = SourceRegionId("f".repeat(64)))
        val unknownCrop = renderer.crop(page, unknownSource.bounds)
        val unknown = RequestRegion(requestRegionIdentity(page,listOf(unknownSource),unknownSource.bounds,unknownCrop.canonicalPixelDigest),first.sourceEvidenceArtifactId,
            first.sourceSha256,page.id,1,page.provenance.pixelDimensions,unknownSource.bounds,unknownCrop.canonicalPixelDigest,unknownSource.structuralClass,
            first.derivationProfileId,1,listOf(unknownSource),image(page,unknownCrop))
        assertContains(validator.validate(fixture.graphs,listOf(unknown)+result.regions.drop(1)).exceptionOrNull()!!.message!!,"unknown")
        assertFails { first.copy(bounds = PixelCropBounds(first.bounds.left, first.bounds.top, first.bounds.rightExclusive + 1, first.bounds.bottomExclusive)) }
    }

    @Test fun `ambiguous and unsupported source order and more than 32 pages fail closed`() {
        val fixture = fixture(listOf(3)); val ambiguous = fixture.graphs[0].copy(ambiguityState=SourceRegionAmbiguityState.HUMAN_ORDER_REQUIRED,reason="review")
        val outcome = DeterministicCompleteSetRequestRegionShaper().shape(fixture.pages,listOf(ambiguous))
        assertEquals(OrdinaryRegionDisposition.SOURCE_ORDER_REVIEW_REQUIRED, assertIs<RequestRegionShapingOutcome.Unsupported>(outcome).disposition)
        val unsupported = fixture.graphs[0].copy(ambiguityState=SourceRegionAmbiguityState.NOT_YET_SUPPORTED,reason="unsupported")
        assertEquals(OrdinaryRegionDisposition.SOURCE_ORDER_NOT_SUPPORTED,
            assertIs<RequestRegionShapingOutcome.Unsupported>(DeterministicCompleteSetRequestRegionShaper().shape(fixture.pages,listOf(unsupported))).disposition)
        val base=fixture(listOf(1));val manyGraphs=(1..33).map{i->val id=PageRepresentationId("%064x".format(i));val source=base.graphs[0].regions[0];val region=source.copy(provenance=source.provenance.copy(pageRepresentationId=id,pageNumber=i));SourceRegionOrderGraph(id,listOf(region),emptySet(),SourceRegionAmbiguityState.UNAMBIGUOUS)}
        assertEquals(OrdinaryRegionDisposition.REQUEST_BOUNDS_EXCEEDED,
            assertIs<RequestRegionShapingOutcome.Unsupported>(DeterministicCompleteSetRequestRegionShaper().shape(base.pages,manyGraphs)).disposition)
    }

    @Test fun `provider validator rejects missing duplicate unknown wrong page and provider order is non-authoritative`() {
        val fixture = fixture(listOf(3)); val request = RequestRegionTranscriptionRequest("attempt-validator", shaped(fixture).regions)
        val canonical = wire(request, request.regions.reversed().mapIndexed { i, r -> r to i + 1 })
        val valid = assertIs<RequestRegionValidationOutcome.Valid>(RequestRegionTranscriptionValidator().validate(request,canonical)).result
        assertEquals(request.regions.map { it.id }, RequestRegionSourceOrderReconstructor().reconstruct(request,valid).getOrThrow().map { it.requestRegionId })
        fun blocks() = (canonical["blocks"] as List<*>).map { @Suppress("UNCHECKED_CAST") (it as Map<String,Any?>).toMutableMap() }.toMutableList()
        val missing=canonical.toMutableMap().also{it["blocks"]=blocks().dropLast(1)}
        assertEquals(RequestRegionValidationRejection.MISSING,assertIs<RequestRegionValidationOutcome.Rejected>(RequestRegionTranscriptionValidator().validate(request,missing)).reason)
        val duplicate=canonical.toMutableMap().also{val b=blocks();b[1]["request_region_id"]=b[0]["request_region_id"];it["blocks"]=b}
        assertEquals(RequestRegionValidationRejection.DUPLICATE,assertIs<RequestRegionValidationOutcome.Rejected>(RequestRegionTranscriptionValidator().validate(request,duplicate)).reason)
        val unknown=canonical.toMutableMap().also{val b=blocks();b[0]["request_region_id"]="f".repeat(64);it["blocks"]=b}
        assertEquals(RequestRegionValidationRejection.UNKNOWN,assertIs<RequestRegionValidationOutcome.Rejected>(RequestRegionTranscriptionValidator().validate(request,unknown)).reason)
        val wrongPage=canonical.toMutableMap().also{val b=blocks();b[0]["page_number"]=99;it["blocks"]=b}
        assertEquals(RequestRegionValidationRejection.PAGE_MISMATCH,assertIs<RequestRegionValidationOutcome.Rejected>(RequestRegionTranscriptionValidator().validate(request,wrongPage)).reason)
    }

    @Test fun `codec manifest state and derivative bind request membership without fabricated constituent text`() {
        val fixture=fixture(listOf(33));val shaped=shaped(fixture);val request=RequestRegionTranscriptionRequest("attempt-binding",shaped.regions)
        val codec=OpenAiRequestRegionCodec();val body=codec.buildRequestBody(request);val digest=codec.requestDigest(request)
        assertContains(body,"constituent_source_region_ids=");assertEquals(64,digest.length)
        val state=requestRegionProviderStateBinding(request);assertEquals(digest,state["request_digest"]);assertEquals(REQUEST_REGION_ADAPTER_VERSION,state["adapter_version"])
        val result=assertIs<RequestRegionValidationOutcome.Valid>(RequestRegionTranscriptionValidator().validate(request,wire(request,request.regions.mapIndexed{i,r->r to i+1}))).result
        val derivative=RequestRegionDerivativeBinder().bind(request,result).getOrThrow()
        assertEquals(request.regions.map{it.constituentIds.map(SourceRegionId::value)},derivative.blocksInParkerOrder.map{it.constituentSourceRegionIds})
        assertTrue(derivative.blocksInParkerOrder.none { block -> block.constituentSourceRegionIds.any { it == block.literalText } })
    }

    @Test fun `property complete deterministic adjacent same-page coverage for generated shapes`() {
        for (pages in 1..4) for (total in pages..52 step 11) {
            val counts=IntArray(pages){1};repeat(total-pages){counts[it%pages]++};val fixture=fixture(counts.toList())
            val first=shaped(fixture);val second=shaped(fixture);assertEquals(signature(first),signature(second));assertTrue(first.regions.size<=32)
            assertEquals(total,first.regions.flatMap{it.constituentIds}.size);assertEquals(total,first.regions.flatMap{it.constituentIds}.distinct().size)
            assertEquals(first.sourceOrder,first.regions.flatMap{it.constituentIds})
            assertTrue(first.regions.all{r->r.constituentSourceRegions.all{it.provenance.pageRepresentationId==r.pageRepresentationId}})
        }
    }

    private data class Fixture(val pages:List<AuthoritativePageRepresentation>,val graphs:List<SourceRegionOrderGraph>)
    private fun fixture(counts:List<Int>):Fixture{
        val bytes=pdf(counts.size);val sha=sha(bytes);val profile=PageRenderProfile("authoritative-page-region-raster-v1",1,300)
        val pages=counts.indices.map{i->assertIs<SourcePageRepresentationOutcome.Created>(renderer.render(SourcePageRenderRequest(EvidenceArtifactId("evidence-v6"),sha,"application/pdf",bytes,i+1,profile))).representation}
        val graphs=pages.zip(counts).map{(page,count)->graph(page,count)};return Fixture(pages,graphs)
    }
    private fun graph(page:AuthoritativePageRepresentation,count:Int):SourceRegionOrderGraph{
        val regions=(0 until count).map{i->val left=100+(i%3)*300;val top=50+i*25;val b=PixelCropBounds(left,top,left+200,top+18);val crop=renderer.crop(page,b)
            SourceRegion(SourceRegionId(sha("${page.provenance.pageNumber}:$i".toByteArray())),b,SourceRegionStructuralClass.TEXT_LIKE,crop.canonicalPixelDigest,
                SourceRegionProvenance(EvidenceArtifactId("evidence-v6"),page.provenance.sourceSha256,page.id,page.provenance.pageNumber,page.provenance.pixelDimensions,page.provenance.canonicalPixelDigest,"pixel-whitespace-source-regions-v1",1))}
        return SourceRegionOrderGraph(page.id,regions,regions.zipWithNext().map{(a,b)->SourceRegionOrderEdge(a.id,b.id,SourceRegionOrderRelation.BEFORE)}.toSet(),SourceRegionAmbiguityState.UNAMBIGUOUS)
    }
    private fun shaped(f:Fixture)=assertIs<RequestRegionShapingOutcome.Shaped>(DeterministicCompleteSetRequestRegionShaper().shape(f.pages,f.graphs))
    private fun signature(s:RequestRegionShapingOutcome.Shaped)=s.regions.map{r->listOf(r.id.value,r.pageNumber,r.bounds,r.cropDigest.value,r.constituentIds.map{it.value},r.image.encodedSha256)}
    private fun wire(request:RequestRegionTranscriptionRequest,ordered:List<Pair<RequestRegion,Int>>)=linkedMapOf<String,Any?>(
        "correlation_id" to request.correlationId,"transcription_profile_id" to REQUEST_REGION_PROFILE_ID,"schema_id" to REQUEST_REGION_SCHEMA_ID,"schema_version" to 6,
        "provider_provenance" to linkedMapOf("provider" to "OpenAI","requested_model" to REQUEST_REGION_MODEL,"provider_reported_model" to REQUEST_REGION_MODEL,"provider_response_id" to "resp-offline","adapter_id" to REQUEST_REGION_ADAPTER_ID,"adapter_version" to REQUEST_REGION_ADAPTER_VERSION,"parser_id" to REQUEST_REGION_PARSER_ID,"parser_version" to REQUEST_REGION_PARSER_VERSION),
        "blocks" to ordered.map{(r,i)->linkedMapOf<String,Any?>("request_region_id" to r.id.value,"page_number" to r.pageNumber,"literal_text" to "literal-$i","status" to "TRANSCRIBED","uncertainties" to emptyList<Any>(),"warnings" to emptyList<String>(),"provider_returned_ordinal" to i,"visual_observations" to emptyList<Any>())})
    private fun image(page:AuthoritativePageRepresentation,crop:DeterministicPageCrop):RegionTranscriptionImage{val encoded=requestRegionPng(crop.dimensions,crop.canonicalPixels());return RegionTranscriptionImage(page.id,crop.bounds,crop.canonicalPixelDigest,"image/png",sha(encoded),encoded)}
    private fun pdf(pages:Int):ByteArray=PDDocument().use{d->repeat(pages){d.addPage(PDPage(PDRectangle.LETTER))};ByteArrayOutputStream().use{out->d.save(out);out.toByteArray()}}
    private fun sha(bytes:ByteArray)=MessageDigest.getInstance("SHA-256").digest(bytes).joinToString(""){"%02x".format(it.toInt()and 255)}
}
