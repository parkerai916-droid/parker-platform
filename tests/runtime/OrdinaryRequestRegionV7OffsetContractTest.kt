package parker.core.runtime

import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.Base64
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import parker.core.interfaces.*
import kotlin.test.*

class OrdinaryRequestRegionV7OffsetContractTest {
    @Test fun `successor identities are distinct acceptance pending and schema describes code points`() {
        val capability=OrdinaryRequestRegionV7Capability()
        assertEquals("ordinary-external-request-region-transcription-v7",capability.capabilityId)
        assertEquals(RequestRegionV7CapabilityLifecycle.ACCEPTANCE_PENDING,capability.lifecycle)
        assertNotEquals(OrdinaryRequestRegionCapability().digest(),capability.digest())
        assertEquals(64,capability.digest().length)
        assertContains(REQUEST_REGION_V7_INSTRUCTION,"exact literal_text")
        assertContains(REQUEST_REGION_V7_INSTRUCTION,"Unicode scalar values/code points")
        assertContains(REQUEST_REGION_V7_INSTRUCTION,"not UTF-8 bytes, UTF-16 code units, grapheme clusters")
        assertContains(REQUEST_REGION_V7_SCHEMA_SOURCE,"Zero-based Unicode scalar-value/code-point start")
        assertContains(REQUEST_REGION_V7_SCHEMA_SOURCE,"must not exceed its code-point length")
        assertEquals("ordinary-external-request-region-transcription-v6",ORDINARY_REQUEST_REGION_CAPABILITY_ID)
        assertEquals("5.0.0",REQUEST_REGION_ADAPTER_VERSION)
        println("OI10R1_IDENTITIES capabilityDigest=${capability.digest()} instructionDigest=$REQUEST_REGION_V7_INSTRUCTION_SHA256 schemaDigest=$REQUEST_REGION_V7_SCHEMA_SHA256")
    }

    @Test fun `strict half-open bounds accept exact end and reject over negative and reversed`() {
        val c=RequestRegionV7ObservationContract
        assertTrue(c.validate("abc",RegionVisualObservationKind.BOLD,0,3).isSuccess)
        assertTrue(c.validate("abc",RegionVisualObservationKind.BOLD,2,3).isSuccess)
        assertTrue(c.validate("abc",RegionVisualObservationKind.BOLD,0,4).isFailure)
        assertTrue(c.validate("abc",RegionVisualObservationKind.BOLD,-1,1).isFailure)
        assertTrue(c.validate("abc",RegionVisualObservationKind.BOLD,2,1).isFailure)
        assertTrue(c.validate("abc",RegionVisualObservationKind.BOLD,1,1).isFailure)
    }

    @Test fun `supplementary scalar is one code point and not two UTF16 coordinates`() {
        val text="A😀B"
        assertEquals(3,text.codePointCount(0,text.length));assertEquals(4,text.length)
        assertTrue(RequestRegionV7ObservationContract.validate(text,RegionVisualObservationKind.BOLD,1,2).isSuccess)
        assertTrue(RequestRegionV7ObservationContract.validate(text,RegionVisualObservationKind.BOLD,0,3).isSuccess)
        assertTrue(RequestRegionV7ObservationContract.validate(text,RegionVisualObservationKind.BOLD,0,4).isFailure)
    }

    @Test fun `combining scalar CRLF and terminal newline remain exact unnormalized coordinates`() {
        val combining="e\u0301";assertEquals(2,combining.codePointCount(0,combining.length))
        assertTrue(RequestRegionV7ObservationContract.validate(combining,RegionVisualObservationKind.UNDERLINE,0,2).isSuccess)
        val crlf="A\r\nB\n";assertEquals(5,crlf.codePointCount(0,crlf.length))
        assertTrue(RequestRegionV7ObservationContract.validate(crlf,RegionVisualObservationKind.BOLD,0,5).isSuccess)
        assertTrue(RequestRegionV7ObservationContract.validate(crlf,RegionVisualObservationKind.BOLD,0,6).isFailure)
        assertTrue(RequestRegionV7ObservationContract.validate(crlf,RegionVisualObservationKind.LINE_BREAK,3,3).isSuccess)
        assertTrue(RequestRegionV7ObservationContract.validate(crlf,RegionVisualObservationKind.LINE_BREAK,5,5).isSuccess)
    }

    @Test fun `point and unanchored forms are exact and LINE_BREAK only`() {
        val c=RequestRegionV7ObservationContract
        assertTrue(c.validate("a\nb",RegionVisualObservationKind.LINE_BREAK,1,1).isSuccess)
        assertTrue(c.validate("a\nb",RegionVisualObservationKind.LINE_BREAK,1,2).isFailure)
        assertTrue(c.validate("a\nb",RegionVisualObservationKind.BOLD,1,1).isFailure)
        assertTrue(c.validate("a\nb",RegionVisualObservationKind.BOLD,null,null).isSuccess)
        assertTrue(c.validate("a\nb",RegionVisualObservationKind.BOLD,null,1).isFailure)
    }

    @Test fun `structured parser retains literal exactly and validates v7 coordinates`() {
        val request=request("a".repeat(64));val literal="A😀e\u0301\r\nB\n"
        val wire=wire(request,literal,listOf(mapOf("kind" to "LINE_BREAK","start_code_point" to 6,"end_code_point_exclusive" to 6)))
        val json=RegionJson.encode(wire);val outcome=RequestRegionV7StructuredValidator().parseExact(request,json)
        val valid=assertIs<RequestRegionV7ValidationOutcome.Valid>(outcome)
        assertEquals(literal,valid.blocksInProviderOrder.single().literalText)
        assertEquals(json,RegionJson.encode(RegionJson.parse(json)))
        val excessive=wire(request,literal,listOf(mapOf("kind" to "LINE_BREAK","start_code_point" to 9,"end_code_point_exclusive" to 9)))
        assertIs<RequestRegionV7ValidationOutcome.Rejected>(RequestRegionV7StructuredValidator().validate(request,excessive))
    }

    @Test fun `exact preserved OI10 response remains malformed under corrected unambiguous contract`() {
        val path=System.getenv("OI10_RESPONSE_PATH")?.let(Path::of)
        assumeTrue(path!=null&&Files.isRegularFile(path),"preserved OI10 response not supplied")
        @Suppress("UNCHECKED_CAST") val outer=RegionJson.parse(Files.readString(path)) as Map<String,Any?>
        @Suppress("UNCHECKED_CAST") val record=outer["record"] as Map<String,Any?>
        val raw=Base64.getDecoder().decode(record["raw_base64"] as String)
        assertEquals(record["raw_sha256"],sha(raw))
        @Suppress("UNCHECKED_CAST") val envelope=RegionJson.parse(raw.toString(Charsets.UTF_8)) as Map<String,Any?>
        @Suppress("UNCHECKED_CAST") val output=envelope["output"] as List<Map<String,Any?>>
        @Suppress("UNCHECKED_CAST") val content=output.single()["content"] as List<Map<String,Any?>>
        val structuredJson=content.single()["text"] as String
        @Suppress("UNCHECKED_CAST") val structured=RegionJson.parse(structuredJson) as Map<String,Any?>
        @Suppress("UNCHECKED_CAST") val block=(structured["blocks"] as List<Map<String,Any?>>).single()
        val id=block["request_region_id"] as String;val request=request(id)
        val v7=structured.toMutableMap().also{it["transcription_profile_id"]=REQUEST_REGION_V7_PROFILE_ID;it["schema_id"]=REQUEST_REGION_V7_SCHEMA_ID;it["schema_version"]=7
            @Suppress("UNCHECKED_CAST") val p=(it["provider_provenance"] as Map<String,Any?>).toMutableMap();p["adapter_version"]=REQUEST_REGION_V7_ADAPTER_VERSION;p["parser_version"]=REQUEST_REGION_V7_PARSER_VERSION;it["provider_provenance"]=p}
        assertIs<RequestRegionV7ValidationOutcome.Rejected>(RequestRegionV7StructuredValidator().validate(request,v7))
        assertEquals(272,(block["literal_text"] as String).codePointCount(0,(block["literal_text"] as String).length))
        @Suppress("UNCHECKED_CAST") val observations=block["visual_observations"] as List<Map<String,Any?>>
        assertEquals(274,(observations.last()["end_code_point_exclusive"] as Number).toInt())
    }

    private fun wire(request:RequestRegionTranscriptionRequest,text:String,observations:List<Map<String,Any?>>)=linkedMapOf<String,Any?>(
        "correlation_id" to request.correlationId,"transcription_profile_id" to REQUEST_REGION_V7_PROFILE_ID,"schema_id" to REQUEST_REGION_V7_SCHEMA_ID,"schema_version" to 7,
        "provider_provenance" to linkedMapOf("provider" to REQUEST_REGION_PROVIDER,"requested_model" to REQUEST_REGION_MODEL,"provider_reported_model" to REQUEST_REGION_MODEL,"provider_response_id" to "resp-offline","adapter_id" to REQUEST_REGION_ADAPTER_ID,"adapter_version" to REQUEST_REGION_V7_ADAPTER_VERSION,"parser_id" to REQUEST_REGION_PARSER_ID,"parser_version" to REQUEST_REGION_V7_PARSER_VERSION),
        "blocks" to listOf(linkedMapOf("request_region_id" to request.regions.single().id.value,"page_number" to 1,"literal_text" to text,"status" to "TRANSCRIBED","uncertainties" to emptyList<Any>(),"warnings" to emptyList<Any>(),"provider_returned_ordinal" to 1,"visual_observations" to observations)))

    private fun request(id:String):RequestRegionTranscriptionRequest{
        val bytes=PDDocument().use{d->d.addPage(PDPage(PDRectangle.LETTER));ByteArrayOutputStream().use{out->d.save(out);out.toByteArray()}}
        val sourceSha=sha(bytes);val renderer=DeterministicSourcePageRenderer();val page=assertIs<SourcePageRepresentationOutcome.Created>(renderer.render(SourcePageRenderRequest(EvidenceArtifactId("evidence-v7"),sourceSha,"application/pdf",bytes,1,PageRenderProfile("authoritative-page-region-raster-v1",1,300)))).representation
        val graph=assertIs<SourceRegionDerivationOutcome.Derived>(DeterministicSourceRegionDeriver().derive(page)).graph
        val base=if(graph.regions.isNotEmpty()) assertIs<RequestRegionShapingOutcome.Shaped>(DeterministicCompleteSetRequestRegionShaper(renderer).shape(listOf(page),listOf(graph))).regions.single() else synthetic(page,renderer)
        return RequestRegionTranscriptionRequest("v7-offset-test",listOf(base.copy(id=RequestRegionId(id))))
    }
    private fun synthetic(page:AuthoritativePageRepresentation,renderer:DeterministicSourcePageRenderer):RequestRegion{
        val bounds=PixelCropBounds(0,0,10,10);val crop=renderer.crop(page,bounds);val source=SourceRegion(SourceRegionId("b".repeat(64)),bounds,SourceRegionStructuralClass.TEXT_LIKE,crop.canonicalPixelDigest,SourceRegionProvenance(EvidenceArtifactId("evidence-v7"),page.provenance.sourceSha256,page.id,1,page.provenance.pixelDimensions,page.provenance.canonicalPixelDigest,"pixel-whitespace-source-regions-v1",1));val encoded=requestRegionPng(crop.dimensions,crop.canonicalPixels())
        return RequestRegion(RequestRegionId("a".repeat(64)),source.provenance.sourceEvidenceArtifactId,source.provenance.sourceSha256,page.id,1,page.provenance.pixelDimensions,bounds,crop.canonicalPixelDigest,source.structuralClass,source.provenance.derivationProfileId,1,listOf(source),RegionTranscriptionImage(page.id,bounds,crop.canonicalPixelDigest,"image/png",sha(encoded),encoded))}
    private fun sha(bytes:ByteArray)=MessageDigest.getInstance("SHA-256").digest(bytes).joinToString(""){"%02x".format(it.toInt()and 255)}
}
