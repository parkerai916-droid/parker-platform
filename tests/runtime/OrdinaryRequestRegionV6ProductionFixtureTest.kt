package parker.core.runtime

import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import parker.core.interfaces.*
import kotlin.io.path.*
import kotlin.test.*

class OrdinaryRequestRegionV6ProductionFixtureTest {
    @Test fun `authoritative offline PDF fixture reproduces governed request shaping`() {
        val root=Path.of(System.getenv("OI9_CORPUS_DIR") ?: "/tmp/oi9-corpus")
        assumeTrue(root.isDirectory(),"OI9 read-only corpus fixture not supplied")
        val ids=System.getenv("OI9_SINGLE_ID")?.split(',') ?: Files.list(root).use { stream -> stream.filter { it.isRegularFile() }
            .filter { it.readBytes().take(4).map(Byte::toInt) == listOf(37,80,68,70) }.map { it.name.removeSuffix(".evidence") }.sorted().toList() }
        ids.forEach { id -> inspect(root, id); System.gc() }
    }
    private fun inspect(root:Path,id:String){val path=root.resolve("$id.evidence")
        val bytes=path.readBytes();assertEquals(listOf(37,80,68,70),bytes.take(4).map(Byte::toInt));val sourceSha=sha(bytes)
        val renderer=DeterministicSourcePageRenderer();val deriver=DeterministicSourceRegionDeriver();val profile=PageRenderProfile("authoritative-page-region-raster-v1",1,300)
        val first=assertIs<SourcePageRepresentationOutcome.Created>(renderer.render(SourcePageRenderRequest(EvidenceArtifactId(id),sourceSha,"application/pdf",bytes,1,profile))).representation
        val pages=(1..first.provenance.declaredPageCount).map{n->if(n==1)first else assertIs<SourcePageRepresentationOutcome.Created>(renderer.render(SourcePageRenderRequest(EvidenceArtifactId(id),sourceSha,"application/pdf",bytes,n,profile))).representation}
        val graphs=pages.map{p->assertIs<SourceRegionDerivationOutcome.Derived>(deriver.derive(p)).graph};val original=graphs.sumOf{it.regions.size}
        val outcome=DeterministicCompleteSetRequestRegionShaper(renderer).shape(pages,graphs)
        when(outcome){
            is RequestRegionShapingOutcome.Unsupported->println("OI9_DOC=$id original=$original pages=${pages.size} perPage=${graphs.joinToString(","){it.regions.size.toString()}} unsupported=${outcome.disposition} detail=${outcome.detail}")
            is RequestRegionShapingOutcome.Shaped->{
                val request=RequestRegionTranscriptionRequest("oi9-offline-fixture",outcome.regions);val codec=OpenAiRequestRegionCodec();val body=codec.buildRequestBody(request).toByteArray().size
                println("OI9_DOC=$id original=$original pages=${pages.size} perPage=${graphs.joinToString(","){it.regions.size.toString()}} shaped=${outcome.regions.size} body=$body requestDigest=${codec.requestDigest(request)} manifestDigest=${requestRegionSha256(codec.buildRequestBody(request).toByteArray())}")
                if(id==TARGET){
                    assertEquals(TARGET_SHA,sourceSha);assertEquals(listOf(16,14,6),graphs.map{it.regions.size});assertEquals(36,original);assertEquals(32,outcome.regions.size)
                    assertEquals(36,outcome.regions.flatMap{it.constituentIds}.size);assertEquals(36,outcome.regions.flatMap{it.constituentIds}.distinct().size);assertTrue(body<=REQUEST_REGION_BODY_MAXIMUM_BYTES)
                    val signatures=(1..3).map{run->val again=assertIs<RequestRegionShapingOutcome.Shaped>(DeterministicCompleteSetRequestRegionShaper(renderer).shape(pages,graphs));val r=RequestRegionTranscriptionRequest("oi9-offline-fixture",again.regions);listOf(again.regions.map{x->x.id.value to x.constituentIds.map{it.value}},codec.requestDigest(r),requestRegionSha256(codec.buildRequestBody(r).toByteArray())).also{println("OI9_TARGET_REPEAT=$run digest=${it[1]} manifest=${it[2]}")}}
                    assertEquals(1,signatures.distinct().size);println("OI9_TARGET_COVERAGE=36/36 missing=0 duplicate=0 deterministic=PASS")
                }
            }
        }
    }
    private fun sha(b:ByteArray)=MessageDigest.getInstance("SHA-256").digest(b).joinToString(""){"%02x".format(it.toInt()and 255)}
    companion object{const val TARGET="evidence-4c6f2ee8-2f62-47be-bd7a-946c744b2766";const val TARGET_SHA="ce8bd4b53d8b007026575974014e71f648f045bf3970b0e984605cf842a7b4a5"}
}
