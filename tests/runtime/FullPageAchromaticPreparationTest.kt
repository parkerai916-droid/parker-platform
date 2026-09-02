package parker.core.runtime

import java.awt.Color
import java.awt.Font
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import javax.imageio.ImageIO
import kotlin.io.path.createTempDirectory
import kotlin.test.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assumptions.assumeTrue
import kotlinx.coroutines.test.runTest
import parker.composition.OpenAiApiCredential
import parker.core.interfaces.*

class FullPageAchromaticPreparationTest {
    @Test fun `frozen integer conversion covers full page and PNG bytes repeat exactly`() {
        val pixels=byteArrayOf(0,0,0, 0xff.toByte(),0xff.toByte(),0xff.toByte(), 100,120,140.toByte())
        val expected=byteArrayOf(0,0xff.toByte(),((77*100+150*120+29*140+128) shr 8).toByte())
        assertContentEquals(expected,FullPageAchromaticPreparer.convert(pixels,PagePixelDimensions(3,1)))
        val page=page(1,3,1,pixels);val first=prepared(listOf(page));val second=prepared(listOf(page))
        assertEquals(first.preparationIdentity,second.preparationIdentity);assertEquals(first.regionSetDigest,second.regionSetDigest)
        assertEquals(first.regions.single().preparationId,second.regions.single().preparationId)
        assertEquals(first.regions.single().provenance,second.regions.single().provenance)
        assertContentEquals(first.regions.single().image.encodedBytes(),second.regions.single().image.encodedBytes())
        val decoded=ImageIO.read(first.regions.single().image.encodedBytes().inputStream())
        assertEquals(BufferedImage.TYPE_BYTE_GRAY,decoded.type);assertEquals(3,decoded.width);assertEquals(1,decoded.height)
        assertEquals(PixelCropBounds(0,0,3,1),first.regions.single().sourceRegion.bounds)
        assertEquals("DETERMINISTIC_SOURCE_ORDER",first.regions.single().orderState.disposition)
    }

    @Test fun `fixture surface preserves visible strokes margins blanks noise handwriting signatures and page order`() {
        val pages=(1..3).map{number->fixturePage(number,640,880)};val document=prepared(pages)
        assertEquals(listOf(1,2,3),document.regions.map{it.provenance.pageNumber})
        assertEquals(List(3){PixelCropBounds(0,0,640,880)},document.regions.map{it.sourceRegion.bounds})
        assertTrue(document.regions.all{it.provenance.authoritativeDimensions.pixelCount==640L*880L})
        document.regions.forEach { region ->
            val gray=ImageIO.read(region.image.encodedBytes().inputStream());var dark=0
            for(y in 0 until gray.height)for(x in 0 until gray.width)if((gray.getRGB(x,y)and 255)<245)dark++
            assertTrue(dark>1_000,"clean/faint/dense/heading/handwriting/signature/noise strokes remain visible")
            assertEquals(0,gray.getRGB(1,0)and 255,"top-left margin mark remains represented")
            assertEquals(0,gray.getRGB(gray.width-2,gray.height-2)and 255,"bottom-right margin mark remains represented")
        }
        val blank=prepared(listOf(page(1,20,20,ByteArray(20*20*3){0xff.toByte()})))
        assertEquals(PixelCropBounds(0,0,20,20),blank.regions.single().sourceRegion.bounds)
        val nearly=ByteArray(20*20*3){0xff.toByte()}.also{it[0]=0;it[1]=0;it[2]=0}
        assertEquals(PixelCropBounds(0,0,20,20),prepared(listOf(page(1,20,20,nearly))).regions.single().sourceRegion.bounds)
    }

    @Test fun `equal and near equal luminance chromatic distinctions fail closed without fallback`() {
        listOf(colorRisk(255,0,0,0,131,0),colorRisk(255,0,0,0,134,0),colorRisk(0,90,180,180,36,0)).forEach { rgb ->
            val outcome=FullPageAchromaticPreparer().prepare(listOf(page(1,16,16,rgb)))
            assertIs<FullPageAchromaticPreparationOutcome.Rejected>(outcome);assertContains(outcome.reason,"CHROMATIC_RISK")
        }
    }

    @Test fun `create once codec readback preserves complete provenance and rejects conflict`() {
        val document=prepared(listOf(fixturePage(1,320,440)));val root=createTempDirectory("r5i-store-");val store=FileSystemFullPageAchromaticPreparationStore(root)
        store.persist(document);store.persist(document);val read=store.read(document.preparationIdentity)
        assertEquals(FullPageAchromaticPreparationCodec.encode(document),FullPageAchromaticPreparationCodec.encode(read))
        assertContentEquals(document.regions.single().image.encodedBytes(),read.regions.single().image.encodedBytes())
        val record=root.resolve("records/${document.preparationIdentity}.json");Files.writeString(record,"conflict")
        assertFails{store.persist(document)}
    }

    @Test fun `persisted preparation is the sole five-page request input and validates end to end through fake provider`()=runTest {
        val pages=(1..5).map{number->page(number,24,32,ByteArray(24*32*3){0xff.toByte()},5)}
        val document=prepared(pages);val root=createTempDirectory("r6a-persisted-");val store=FileSystemFullPageAchromaticPreparationStore(root)
        store.persist(document);val exact=store.findExact(EvidenceArtifactId("evidence-r5i"),"a".repeat(64),FULL_PAGE_ACHROMATIC_PROFILE_ID,1)
        assertEquals(document.preparationIdentity,exact?.preparationIdentity)
        val construction=FullPageAchromaticCanonicalRequestRegionV8Builder().buildPersisted(requireNotNull(exact),"persisted-five")
        assertTrue(construction.pages.isEmpty(),"persisted execution must not render authoritative pages again")
        assertEquals(listOf(1,2,3,4,5),construction.request.regions.map{it.pageNumber})
        assertEquals(document.regions.map{it.provenance.transportSha256},construction.request.regions.map{it.image.encodedSha256})
        assertEquals(document.preparationIdentity,construction.achromaticPreparation?.preparationIdentity)
        val cap=OrdinaryRequestRegionV8CapabilityIdentity();val identity=FidelityFirstExecutionIdentity("execution-five",construction.requestBindingSha256,
            construction.request.correlationId,"evidence-r5i","a".repeat(64),100,"application/pdf","b".repeat(40),cap.provider,cap.model,cap.profile,
            cap.instructionSha256,cap.schemaSha256,cap.processing,cap.adapterVersion)
        val prepared=OrdinaryRequestRegionV8PreparedRequest(construction,identity,cap,"b".repeat(40));val state=FileSystemRequestRegionV8ProviderStateStore(createTempDirectory("r6a-five-state-"));var calls=0
        val blocks=construction.request.regions.mapIndexed{i,r->mapOf("request_region_id" to r.id.value,"page_number" to r.pageNumber,"literal_text" to "page-$i",
            "status" to "TRANSCRIBED","uncertainties" to emptyList<Any>(),"warnings" to emptyList<String>(),"provider_returned_ordinal" to (5-i))}.reversed()
        val structured=mapOf("correlation_id" to construction.request.correlationId,"transcription_profile_id" to REQUEST_REGION_V8_PROFILE_ID,"schema_id" to REQUEST_REGION_V8_SCHEMA_ID,
            "schema_version" to 8,"provider_provenance" to mapOf("provider" to REQUEST_REGION_PROVIDER,"requested_model" to REQUEST_REGION_MODEL,"provider_reported_model" to null,
                "provider_response_id" to null,"adapter_id" to REQUEST_REGION_ADAPTER_ID,"adapter_version" to REQUEST_REGION_V8_ADAPTER_VERSION,"parser_id" to REQUEST_REGION_PARSER_ID,"parser_version" to REQUEST_REGION_V8_PARSER_VERSION),"blocks" to blocks)
        val raw=RegionJson.encode(mapOf("id" to "resp-five","model" to REQUEST_REGION_MODEL,"output" to listOf(mapOf("type" to "message","content" to listOf(mapOf("type" to "output_text","text" to RegionJson.encode(structured))))))).toByteArray()
        val exchange=OpenAiRequestRegionV8ProviderExchange(OpenAiApiCredential.fromEnvironment("SYNTHETIC_V8_KEY")!!,OpenAiResponsesTransport{calls++;OpenAiResponsesTransportResponse(200,raw)},state)
        val outcome=assertIs<RequestRegionV8ProviderExchangeOutcome.Valid>(exchange.exchange(prepared));assertEquals(1,calls);assertEquals(5,outcome.result.blocksInProviderOrder.size)
        assertContentEquals(raw,state.read(outcome.state.recordId).rawBytes);assertEquals(listOf(1,2,3,4,5),RequestRegionV8DerivativeBinder().bind(construction.request,outcome.result).getOrThrow().blocksInParkerOrder.map{it.pageNumber})
        assertNull(store.findExact(EvidenceArtifactId("other"),"a".repeat(64),FULL_PAGE_ACHROMATIC_PROFILE_ID,1))
        assertNull(store.findExact(EvidenceArtifactId("evidence-r5i"),"b".repeat(64),FULL_PAGE_ACHROMATIC_PROFILE_ID,1))
    }

    @Test fun `full page request retains frozen V8 semantics and capability digest`() {
        assertEquals("c0479979720455d2de3fc9861eeb5dee323a4770bdb15f807af611ad426f9ec0",OrdinaryRequestRegionV8Capability().digest())
        assertEquals("ordinary-external-request-region-transcription-v8",ORDINARY_REQUEST_REGION_V8_CAPABILITY_ID)
        val pdf=java.nio.file.Path.of("tests/fixtures/document-ingestion-bakeoff/fixtures/02-multicolumn-complex.pdf");val bytes=Files.readAllBytes(pdf);val sha=CanonicalPagePixelDigests.sha256(bytes)
        val construction=FullPageAchromaticCanonicalRequestRegionV8Builder().build(EvidenceArtifactId("evidence-r5i-pdf-path"),sha,"application/pdf",bytes,"r5i-pdf-path")
        assertEquals(construction.pages.size,construction.request.regions.size);assertEquals(construction.pages.indices.map{it+1},construction.request.regions.map{it.pageNumber})
        assertTrue(construction.providerBody.toByteArray().size<=FULL_PAGE_ACHROMATIC_BODY_MAXIMUM)
    }

    @Test fun `registered Deed local acceptance constructs canonical request and review package without transport`() {
        val source=java.nio.file.Path.of("/tmp/oi11r5i-governed-pages")
        assumeTrue(Files.isDirectory(source),"registered Deed pages are not staged locally")
        FullPageAchromaticLocalAcceptanceCli.main(arrayOf(source.toString(),"/tmp/oi11r5i-review"))
        val review=java.nio.file.Path.of("/tmp/oi11r5i-review")
        assertTrue(Files.isRegularFile(review.resolve("review-manifest.txt")))
        assertEquals(11,Files.list(review).use{it.count()})
        val first=Files.readAllBytes(review.resolve("review-manifest.txt"));FullPageAchromaticLocalAcceptanceCli.main(arrayOf(source.toString(),review.toString()))
        assertContentEquals(first,Files.readAllBytes(review.resolve("review-manifest.txt")))
    }

    private fun prepared(pages:List<AuthoritativePageRepresentation>)=assertIs<FullPageAchromaticPreparationOutcome.Prepared>(FullPageAchromaticPreparer().prepare(pages)).document
    private fun colorRisk(r1:Int,g1:Int,b1:Int,r2:Int,g2:Int,b2:Int)=ByteArray(16*16*3).also{bytes->for(y in 0 until 16)for(x in 0 until 16){val first=x%2==0;val i=(y*16+x)*3;bytes[i]=(if(first)r1 else r2).toByte();bytes[i+1]=(if(first)g1 else g2).toByte();bytes[i+2]=(if(first)b1 else b2).toByte()}}
    private fun fixturePage(number:Int,width:Int,height:Int):AuthoritativePageRepresentation {
        val image=BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);val g=image.createGraphics();g.color=Color.WHITE;g.fillRect(0,0,width,height)
        g.color=Color.BLACK;g.font=Font("Dialog",Font.BOLD,28);g.drawString("HEADING",20,45);g.font=Font("Dialog",Font.PLAIN,14)
        repeat(25){g.drawString("Dense printed paragraph line $it with punctuation, dates 17 July 2026.",20,80+it*18)}
        g.color=Color(170,170,170);g.drawString("faint printed text",20,550);g.color=Color.BLACK;g.drawOval(20,590,180,60);g.drawString("handwriting signature",35,625)
        g.drawLine(0,0,5,0);g.drawLine(width-6,height-2,width-1,height-2);repeat(100){i->val x=(i*37)%width;val y=(i*71)%height;g.fillRect(x,y,1,1)};g.dispose()
        val rgb=ByteArray(width*height*3);var i=0;for(y in 0 until height)for(x in 0 until width){val c=image.getRGB(x,y);rgb[i++]=(c ushr 16).toByte();rgb[i++]=(c ushr 8).toByte();rgb[i++]=c.toByte()}
        return page(number,width,height,rgb,pageCount=3)
    }
    private fun page(number:Int,width:Int,height:Int,rgb:ByteArray,pageCount:Int=1):AuthoritativePageRepresentation {
        val dims=PagePixelDimensions(width,height);val pixel=CanonicalPagePixelDigests.digest(dims,PagePixelFormat.SRGB_8_RGB_OPAQUE,rgb);val encoded=ByteArrayOutputStream().use{out->val image=BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);var i=0;for(y in 0 until height)for(x in 0 until width){image.setRGB(x,y,((rgb[i++].toInt()and 255)shl 16)or((rgb[i++].toInt()and 255)shl 8)or(rgb[i++].toInt()and 255))};ImageIO.write(image,"png",out);out.toByteArray()}
        val p=PageRepresentationProvenance(EvidenceArtifactId("evidence-r5i"),"a".repeat(64),100,"application/pdf",number,pageCount,"test-renderer","1","test-build",PageRenderProfile("authoritative-page-region-raster-v1",1,300),SourcePageDimensions(width.toString(),height.toString(),"SOURCE_PIXEL"),0,dims,pixel,CanonicalPagePixelDigests.sha256(encoded))
        return AuthoritativePageRepresentation(PageRepresentationId("%064x".format(number)),p,encoded,rgb)
    }
}
