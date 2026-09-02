package parker.core.runtime

import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.security.MessageDigest
import java.util.Base64
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriter
import javax.imageio.stream.ImageOutputStream
import parker.core.interfaces.*

const val FULL_PAGE_ACHROMATIC_PROFILE_ID = "full-page-achromatic-png-preparation-v1"
const val FULL_PAGE_ACHROMATIC_PROFILE_VERSION = 1
const val FULL_PAGE_ACHROMATIC_TRANSFORM_ID = "srgb-integer-luma-77-150-29-v1"
const val FULL_PAGE_ACHROMATIC_ENCODER_ID = "jdk17-imageio-png-byte-gray-max-compression-v1"
const val FULL_PAGE_ACHROMATIC_IMAGE_BINARY_MAXIMUM = 10_875_000
const val FULL_PAGE_ACHROMATIC_BASE64_MAXIMUM = 14_500_000
const val FULL_PAGE_ACHROMATIC_BODY_MAXIMUM = 16_000_000

data class AchromaticTransportProvenance(
    val evidenceId: EvidenceArtifactId,
    val sourceSha256: String,
    val pageNumber: Int,
    val authoritativePageRepresentationId: PageRepresentationId,
    val authoritativePixelDigest: CanonicalPixelDigest,
    val authoritativeDimensions: PagePixelDimensions,
    val sourceBounds: PixelCropBounds,
    val preparationProfileId: String,
    val preparationProfileVersion: Int,
    val transformId: String,
    val transformVersion: Int,
    val conversionRule: String,
    val outputDimensions: PagePixelDimensions,
    val colorModel: String,
    val encoderId: String,
    val encoderProviderClass: String,
    val jdkRuntimeIdentity: String,
    val compressionMode: String,
    val compressionQuality: String,
    val interlaced: Boolean,
    val ancillaryMetadata: Boolean,
    val encodedByteLength: Int,
    val transportSha256: String,
) {
    init {
        require(sourceSha256.matches(Regex("^[0-9a-f]{64}$")))
        require(pageNumber > 0 && sourceBounds == PixelCropBounds(0, 0, authoritativeDimensions.width, authoritativeDimensions.height))
        require(authoritativeDimensions == outputDimensions)
        require(preparationProfileId == FULL_PAGE_ACHROMATIC_PROFILE_ID && preparationProfileVersion == 1)
        require(transformId == FULL_PAGE_ACHROMATIC_TRANSFORM_ID && transformVersion == 1)
        require(conversionRule == "Y=(77R+150G+29B+128)>>8")
        require(colorModel == "TYPE_BYTE_GRAY" && encoderId == FULL_PAGE_ACHROMATIC_ENCODER_ID)
        require(compressionMode == "MODE_EXPLICIT" && compressionQuality == "0.0")
        require(!interlaced && !ancillaryMetadata && encodedByteLength > 0)
        require(transportSha256.matches(Regex("^[0-9a-f]{64}$")))
    }
}

data class FullPageAchromaticPreparationRegion(
    val preparationId: String,
    val sourceRegion: SourceRegion,
    val orderState: SourceRegionOrderState,
    val provenance: AchromaticTransportProvenance,
    val image: RegionTranscriptionImage,
) {
    init {
        require(preparationId.matches(Regex("^[0-9a-f]{64}$")))
        require(sourceRegion.bounds == provenance.sourceBounds)
        require(sourceRegion.id in orderState.regionIds && orderState.regionIds.size == 1)
        require(orderState.disposition == "DETERMINISTIC_SOURCE_ORDER")
        require(image.bounds == provenance.sourceBounds && image.encodedSha256 == provenance.transportSha256)
    }
}

data class FullPageAchromaticPreparationDocument(
    val preparationIdentity: String,
    val regionSetDigest: String,
    val regions: List<FullPageAchromaticPreparationRegion>,
) {
    init {
        require(preparationIdentity.matches(Regex("^[0-9a-f]{64}$")))
        require(regionSetDigest.matches(Regex("^[0-9a-f]{64}$")))
        require(regions.isNotEmpty() && regions.size <= REQUEST_REGION_MAXIMUM)
        require(regions.map { it.provenance.pageNumber } == (1..regions.size).toList())
        require(regions.map { it.sourceRegion.id }.distinct().size == regions.size)
    }
}

sealed interface FullPageAchromaticPreparationOutcome {
    data class Prepared(val document: FullPageAchromaticPreparationDocument) : FullPageAchromaticPreparationOutcome
    data class Rejected(val reason: String) : FullPageAchromaticPreparationOutcome
}

class FullPageAchromaticPreparer {
    fun prepare(pages: List<AuthoritativePageRepresentation>): FullPageAchromaticPreparationOutcome = try {
        require(pages.isNotEmpty() && pages.size <= REQUEST_REGION_MAXIMUM) { "page count outside 1..32" }
        val ordered = pages.sortedBy { it.provenance.pageNumber }
        require(ordered.map { it.provenance.pageNumber } == (1..ordered.size).toList()) { "page sequence is not complete" }
        require(ordered.map { it.provenance.sourceEvidenceArtifactId }.distinct().size == 1)
        require(ordered.map { it.provenance.sourceSha256 }.distinct().size == 1)
        val prepared = ordered.map { page -> preparePage(page) }
        val identity = digest(fields("parker.full-page-achromatic.document.v1", prepared.flatMap { regionFields(it) }))
        val setDigest = digest(fields("parker.full-page-achromatic.region-set.v1", prepared.flatMap {
            listOf(it.sourceRegion.id.value, it.sourceRegion.bounds.left.toString(), it.sourceRegion.bounds.top.toString(),
                it.sourceRegion.bounds.rightExclusive.toString(), it.sourceRegion.bounds.bottomExclusive.toString(), it.preparationId)
        }))
        FullPageAchromaticPreparationOutcome.Prepared(FullPageAchromaticPreparationDocument(identity, setDigest, prepared))
    } catch (e: ChromaticRiskException) {
        FullPageAchromaticPreparationOutcome.Rejected("CHROMATIC_RISK:${e.message}")
    } catch (e: Exception) {
        FullPageAchromaticPreparationOutcome.Rejected(e.message ?: "PREPARATION_FAILED")
    }

    private fun preparePage(page: AuthoritativePageRepresentation): FullPageAchromaticPreparationRegion {
        val p = page.provenance
        require(p.renderProfile.dpi == 300 && p.renderProfile.pixelFormat == PagePixelFormat.SRGB_8_RGB_OPAQUE)
        val dimensions = p.pixelDimensions
        val bounds = PixelCropBounds(0, 0, dimensions.width, dimensions.height)
        val rgb = page.canonicalPixels()
        require(rgb.size.toLong() == dimensions.pixelCount * 3L)
        ChromaticRiskGate.requireSafe(rgb, dimensions)
        val grayscale = convert(rgb, dimensions)
        val encoded = DeterministicAchromaticPngEncoder.encode(grayscale, dimensions)
        val transportSha = sha256(encoded.bytes)
        val provenance = AchromaticTransportProvenance(p.sourceEvidenceArtifactId, p.sourceSha256, p.pageNumber, page.id,
            p.canonicalPixelDigest, dimensions, bounds, FULL_PAGE_ACHROMATIC_PROFILE_ID, 1,
            FULL_PAGE_ACHROMATIC_TRANSFORM_ID, 1, "Y=(77R+150G+29B+128)>>8", dimensions, "TYPE_BYTE_GRAY",
            FULL_PAGE_ACHROMATIC_ENCODER_ID, encoded.providerClass, runtimeIdentity(), "MODE_EXPLICIT", "0.0", false, false,
            encoded.bytes.size, transportSha)
        val preparationId = digest(fields("parker.full-page-achromatic.preparation.v1", provenanceFields(provenance)))
        val regionId = SourceRegionId(digest(fields("parker.full-page-achromatic.region.v1", provenanceFields(provenance) + preparationId)))
        val source = SourceRegion(regionId, bounds, SourceRegionStructuralClass.MIXED, p.canonicalPixelDigest,
            SourceRegionProvenance(p.sourceEvidenceArtifactId, p.sourceSha256, page.id, p.pageNumber, dimensions,
                p.canonicalPixelDigest, FULL_PAGE_ACHROMATIC_PROFILE_ID, 1))
        val graph = SourceRegionOrderGraph(page.id, listOf(source), emptySet(), SourceRegionAmbiguityState.UNAMBIGUOUS)
        val order = SourceRegionOrderState(page.id, SourceRegionSetIdentity.digest(graph), listOf(regionId),
            "DETERMINISTIC_SOURCE_ORDER", FULL_PAGE_ACHROMATIC_PROFILE_ID, 1)
        val image = RegionTranscriptionImage(page.id, bounds, p.canonicalPixelDigest, "image/png", transportSha, encoded.bytes)
        return FullPageAchromaticPreparationRegion(preparationId, source, order, provenance, image)
    }

    companion object {
        fun convert(rgb: ByteArray, dimensions: PagePixelDimensions): ByteArray {
            require(rgb.size.toLong() == dimensions.pixelCount * 3L)
            val out = ByteArray(dimensions.pixelCount.toInt()); var source = 0
            for (i in out.indices) {
                val r = rgb[source++].toInt() and 255; val g = rgb[source++].toInt() and 255; val b = rgb[source++].toInt() and 255
                out[i] = ((77 * r + 150 * g + 29 * b + 128) shr 8).toByte()
            }
            return out
        }
    }
}

/** Rejects sustained adjacent chromatic distinctions that the frozen luma map would collapse. */
object ChromaticRiskGate {
    private const val MINIMUM_COLLAPSING_EDGES = 32
    fun requireSafe(rgb: ByteArray, dimensions: PagePixelDimensions) {
        require(rgb.size.toLong() == dimensions.pixelCount * 3L)
        var collapsingEdges = 0
        fun channel(pixel: Int, channel: Int) = rgb[pixel * 3 + channel].toInt() and 255
        fun luma(pixel: Int): Int { val r=channel(pixel,0);val g=channel(pixel,1);val b=channel(pixel,2);return (77*r+150*g+29*b+128) shr 8 }
        fun risky(a: Int, b: Int): Boolean {
            val dr=kotlin.math.abs(channel(a,0)-channel(b,0));val dg=kotlin.math.abs(channel(a,1)-channel(b,1));val db=kotlin.math.abs(channel(a,2)-channel(b,2))
            return dr + dg + db >= 160 && kotlin.math.abs(luma(a)-luma(b)) <= 4
        }
        for (y in 0 until dimensions.height) for (x in 0 until dimensions.width) {
            val pixel=y*dimensions.width+x
            if (x+1<dimensions.width && risky(pixel,pixel+1)) collapsingEdges++
            if (y+1<dimensions.height && risky(pixel,pixel+dimensions.width)) collapsingEdges++
            if (collapsingEdges >= MINIMUM_COLLAPSING_EDGES) throw ChromaticRiskException("$collapsingEdges collapsing adjacent edges")
        }
    }
}
private class ChromaticRiskException(message: String) : IllegalArgumentException(message)

object DeterministicAchromaticPngEncoder {
    data class Encoded(val bytes: ByteArray, val providerClass: String)
    const val PROVIDER_CLASS = "com.sun.imageio.plugins.png.PNGImageWriter"
    fun encode(samples: ByteArray, dimensions: PagePixelDimensions): Encoded {
        require(samples.size.toLong() == dimensions.pixelCount)
        val writers=ImageIO.getImageWritersByFormatName("png");val writer=generateSequence { if(writers.hasNext())writers.next() else null }.filter { it.javaClass.name == PROVIDER_CLASS }.toList().singleOrNull()
            ?: error("required PNG writer unavailable")
        val image = BufferedImage(dimensions.width, dimensions.height, BufferedImage.TYPE_BYTE_GRAY)
        image.raster.setDataElements(0, 0, dimensions.width, dimensions.height, samples)
        val parameter=writer.defaultWriteParam;parameter.compressionMode=javax.imageio.ImageWriteParam.MODE_EXPLICIT;parameter.compressionQuality=0.0f
        val output=ByteArrayOutputStream()
        ImageIO.createImageOutputStream(output).use { stream -> writer.output=stream;writer.write(null,IIOImage(image,null,null),parameter) }
        writer.dispose()
        return Encoded(output.toByteArray(), PROVIDER_CLASS)
    }
}

private fun runtimeIdentity() = listOf(System.getProperty("java.vendor"),System.getProperty("java.version"),System.getProperty("java.runtime.name"),System.getProperty("java.runtime.version")).joinToString("|")
private fun provenanceFields(p: AchromaticTransportProvenance)=listOf(p.evidenceId.value,p.sourceSha256,p.pageNumber.toString(),p.authoritativePageRepresentationId.value,
    p.authoritativePixelDigest.value,p.authoritativeDimensions.width.toString(),p.authoritativeDimensions.height.toString(),p.sourceBounds.left.toString(),p.sourceBounds.top.toString(),
    p.sourceBounds.rightExclusive.toString(),p.sourceBounds.bottomExclusive.toString(),p.preparationProfileId,p.preparationProfileVersion.toString(),p.transformId,p.transformVersion.toString(),
    p.conversionRule,p.outputDimensions.width.toString(),p.outputDimensions.height.toString(),p.colorModel,p.encoderId,p.encoderProviderClass,p.jdkRuntimeIdentity,p.compressionMode,
    p.compressionQuality,p.interlaced.toString(),p.ancillaryMetadata.toString(),p.encodedByteLength.toString(),p.transportSha256)
private fun regionFields(r:FullPageAchromaticPreparationRegion)=listOf(r.preparationId,r.sourceRegion.id.value)+provenanceFields(r.provenance)
private fun fields(domain:String, values:List<String>)=listOf(domain)+values
private fun digest(values:List<String>):String { val md=MessageDigest.getInstance("SHA-256");values.forEach{v->val b=v.toByteArray();md.update(ByteBuffer.allocate(4).putInt(b.size).array());md.update(b)};return md.digest().joinToString(""){"%02x".format(it.toInt()and 255)} }
private fun sha256(bytes:ByteArray)=MessageDigest.getInstance("SHA-256").digest(bytes).joinToString(""){"%02x".format(it.toInt()and 255)}

class FileSystemFullPageAchromaticPreparationStore(root:Path) {
    private val root=root.toAbsolutePath().normalize();private val records=this.root.resolve("records");private val images=this.root.resolve("transport")
    init { Files.createDirectories(records);Files.createDirectories(images) }
    fun persist(document:FullPageAchromaticPreparationDocument) {
        document.regions.forEach { region -> atomicCreate(images.resolve("${region.provenance.transportSha256}.png"),region.image.encodedBytes()) }
        val body=FullPageAchromaticPreparationCodec.encode(document).toByteArray();atomicCreate(records.resolve("${document.preparationIdentity}.json"),body)
    }
    fun read(identity:String):FullPageAchromaticPreparationDocument {
        require(identity.matches(Regex("^[0-9a-f]{64}$")));val document=FullPageAchromaticPreparationCodec.decode(Files.readString(records.resolve("$identity.json")))
        require(document.preparationIdentity==identity);document.regions.forEach{r->require(Files.readAllBytes(images.resolve("${r.provenance.transportSha256}.png")).contentEquals(r.image.encodedBytes()))};return document
    }
    fun findExact(evidenceId:EvidenceArtifactId,sourceSha256:String,profileId:String,profileVersion:Int):FullPageAchromaticPreparationDocument? {
        require(sourceSha256.matches(Regex("^[0-9a-f]{64}$")))
        val matches=Files.list(records).use { stream -> stream.filter { it.fileName.toString().endsWith(".json") }.map { path ->
            read(path.fileName.toString().removeSuffix(".json"))
        }.filter { document -> document.regions.all { region ->
            region.provenance.evidenceId==evidenceId && region.provenance.sourceSha256==sourceSha256 &&
                region.provenance.preparationProfileId==profileId && region.provenance.preparationProfileVersion==profileVersion
        } }.toList() }
        require(matches.size<=1) { "conflicting corrected preparations for governed execution boundary" }
        return matches.singleOrNull()
    }
    private fun atomicCreate(target:Path,bytes:ByteArray){if(Files.exists(target)){require(Files.readAllBytes(target).contentEquals(bytes)){"preparation identity conflict"};return};val tmp=Files.createTempFile(root,".r5i-",".tmp");try{FileChannel.open(tmp,StandardOpenOption.WRITE).use{c->val b=ByteBuffer.wrap(bytes);while(b.hasRemaining())c.write(b);c.force(true)};Files.createLink(target,tmp);Files.delete(tmp)}finally{Files.deleteIfExists(tmp)}}
}

object FullPageAchromaticPreparationCodec {
    fun encode(d:FullPageAchromaticPreparationDocument)=RegionJson.encode(linkedMapOf<String,Any?>("format" to 1,"preparation_identity" to d.preparationIdentity,"region_set_digest" to d.regionSetDigest,"regions" to d.regions.map { r ->
        linkedMapOf("preparation_id" to r.preparationId,"source_region_id" to r.sourceRegion.id.value,"page_representation_id" to r.provenance.authoritativePageRepresentationId.value,"evidence_id" to r.provenance.evidenceId.value,
            "source_sha256" to r.provenance.sourceSha256,"page" to r.provenance.pageNumber,"pixel_digest" to r.provenance.authoritativePixelDigest.value,"width" to r.provenance.authoritativeDimensions.width,"height" to r.provenance.authoritativeDimensions.height,
            "profile" to r.provenance.preparationProfileId,"profile_version" to r.provenance.preparationProfileVersion,"transform" to r.provenance.transformId,"transform_version" to r.provenance.transformVersion,"conversion_rule" to r.provenance.conversionRule,
            "color_model" to r.provenance.colorModel,"encoder" to r.provenance.encoderId,"provider_class" to r.provenance.encoderProviderClass,"jdk" to r.provenance.jdkRuntimeIdentity,"compression_mode" to r.provenance.compressionMode,"compression_quality" to r.provenance.compressionQuality,
            "interlaced" to r.provenance.interlaced,"ancillary_metadata" to r.provenance.ancillaryMetadata,"encoded_length" to r.provenance.encodedByteLength,"transport_sha256" to r.provenance.transportSha256,"png_base64" to Base64.getEncoder().encodeToString(r.image.encodedBytes())) }))
    fun decode(text:String):FullPageAchromaticPreparationDocument {
        @Suppress("UNCHECKED_CAST") val root=RegionJson.parse(text) as Map<String,Any?>;require(root.keys==setOf("format","preparation_identity","region_set_digest","regions")&&(root["format"]as Number).toInt()==1)
        @Suppress("UNCHECKED_CAST") val raw=root["regions"] as List<Map<String,Any?>>;val regions=raw.map{v->
            val dims=PagePixelDimensions((v["width"]as Number).toInt(),(v["height"]as Number).toInt());val bounds=PixelCropBounds(0,0,dims.width,dims.height);val page=PageRepresentationId(v["page_representation_id"]as String);val pixel=CanonicalPixelDigest(v["pixel_digest"]as String);val bytes=Base64.getDecoder().decode(v["png_base64"]as String)
            val provenance=AchromaticTransportProvenance(EvidenceArtifactId(v["evidence_id"]as String),v["source_sha256"]as String,(v["page"]as Number).toInt(),page,pixel,dims,bounds,v["profile"]as String,(v["profile_version"]as Number).toInt(),v["transform"]as String,(v["transform_version"]as Number).toInt(),v["conversion_rule"]as String,dims,v["color_model"]as String,v["encoder"]as String,v["provider_class"]as String,v["jdk"]as String,v["compression_mode"]as String,v["compression_quality"]as String,v["interlaced"]as Boolean,v["ancillary_metadata"]as Boolean,(v["encoded_length"]as Number).toInt(),v["transport_sha256"]as String)
            val id=SourceRegionId(v["source_region_id"]as String);val source=SourceRegion(id,bounds,SourceRegionStructuralClass.MIXED,pixel,SourceRegionProvenance(provenance.evidenceId,provenance.sourceSha256,page,provenance.pageNumber,dims,pixel,FULL_PAGE_ACHROMATIC_PROFILE_ID,1));val graph=SourceRegionOrderGraph(page,listOf(source),emptySet(),SourceRegionAmbiguityState.UNAMBIGUOUS);val order=SourceRegionOrderState(page,SourceRegionSetIdentity.digest(graph),listOf(id),"DETERMINISTIC_SOURCE_ORDER",FULL_PAGE_ACHROMATIC_PROFILE_ID,1);val image=RegionTranscriptionImage(page,bounds,pixel,"image/png",provenance.transportSha256,bytes)
            FullPageAchromaticPreparationRegion(v["preparation_id"]as String,source,order,provenance,image)}
        return FullPageAchromaticPreparationDocument(root["preparation_identity"]as String,root["region_set_digest"]as String,regions)
    }
}

/** Explicit local-only R5I acceptance entry point. It constructs no attempt and owns no transport. */
object FullPageAchromaticLocalAcceptanceCli {
    @JvmStatic fun main(args:Array<String>) {
        require(args.size==2){"usage: <registered-pdf> <owner-review-directory>"}
        val source=Path.of(args[0]).toAbsolutePath().normalize();val review=Path.of(args[1]).toAbsolutePath().normalize()
        val sourceSha="5d73e6e55d3491e94aa9d6c02a0735572f9840fe8185a71546dba9f2258e237e"
        val builder=FullPageAchromaticCanonicalRequestRegionV8Builder()
        val construction=if(Files.isDirectory(source))builder.buildPages(loadGovernedDeedPages(source,sourceSha),"oi11r5i-local-acceptance") else {
            val bytes=Files.readAllBytes(source);require(sha256(bytes)==sourceSha);builder.build(EvidenceArtifactId("evidence-a51887d1-1a40-4b68-b340-c60e02e9a8d9"),sourceSha,"application/pdf",bytes,"oi11r5i-local-acceptance")}
        val preparation=requireNotNull(construction.achromaticPreparation);require(construction.pages.size==5&&preparation.regions.size==5&&construction.request.regions.size==5)
        require(construction.request.regions.map{it.pageNumber}==listOf(1,2,3,4,5));require(preparation.regions.all{it.sourceRegion.bounds==PixelCropBounds(0,0,2479,3508)})
        Files.createDirectories(review);val manifest=StringBuilder()
        construction.pages.zip(preparation.regions).forEach{(page,region)->
            val prefix="page-${region.provenance.pageNumber}";val rgb=review.resolve("$prefix-authoritative-rgb.png");val gray=review.resolve("$prefix-achromatic-transport.png")
            Files.write(rgb,page.encodedBytes());Files.write(gray,region.image.encodedBytes())
            manifest.append(prefix).append(" authoritative_file=").append(rgb.fileName).append(" authoritative_sha256=").append(sha256(page.encodedBytes()))
                .append(" authoritative_representation_id=").append(page.id.value).append(" transport_file=").append(gray.fileName)
                .append(" transport_sha256=").append(region.provenance.transportSha256).append(" preparation_id=").append(region.preparationId).append('\n')
        }
        val manifestPath=review.resolve("review-manifest.txt");Files.writeString(manifestPath,manifest.toString())
        println("profile=$FULL_PAGE_ACHROMATIC_PROFILE_ID version=$FULL_PAGE_ACHROMATIC_PROFILE_VERSION pages=${construction.pages.size} preparationRegions=${preparation.regions.size} requestRegions=${construction.request.regions.size}")
        println("preparationIdentity=${preparation.preparationIdentity} regionSetDigest=${preparation.regionSetDigest} order=${construction.request.regions.map{it.pageNumber}}")
        preparation.regions.forEachIndexed{i,r->println("page=${i+1} pngBytes=${r.provenance.encodedByteLength} transportSha256=${r.provenance.transportSha256} preparationId=${r.preparationId} requestRegionId=${construction.request.regions[i].id.value}")}
        println("aggregatePngBytes=${construction.aggregateImageBytes} aggregateBase64Characters=${construction.aggregateBase64Characters} bodyBytes=${construction.providerBody.toByteArray().size} requestDigest=${construction.requestBindingSha256} bodyDigest=${construction.providerBodySha256}")
        println("reviewManifest=$manifestPath reviewManifestSha256=${sha256(Files.readAllBytes(manifestPath))} capability=${OrdinaryRequestRegionV8Capability().capabilityId} capabilityDigest=${OrdinaryRequestRegionV8Capability().digest()}")
    }
    private fun loadGovernedDeedPages(directory:Path,sourceSha:String):List<AuthoritativePageRepresentation> {
        val ids=listOf("33d341f5f169ea09a6cdeffc50c731a6b9d58e2a646ffb1ac32532bee2afff1e","669f1af75d9cdd4768305258e4f73de441a5d71342e7e043ed7d7b8276568c39","8e6751ee97d3c1d66983aef8c2c72c8735714d148db0cde4b54b54b9467e09a8","e65b472cd7fd30d22a470ad6c1fbb22122754443cc1585ba915ecf9e546eeecc","eb7ea4a7c78af09554f52ad63fcfe7b122b9bb5b23563a488b269fdd9bf23c44")
        return ids.mapIndexed{index,id->val encoded=Files.readAllBytes(directory.resolve("$id.bin"));val image=ImageIO.read(encoded.inputStream());require(image.width==2479&&image.height==3508);val rgb=ByteArray(image.width*image.height*3);var at=0;for(y in 0 until image.height)for(x in 0 until image.width){val c=image.getRGB(x,y);rgb[at++]=(c ushr 16).toByte();rgb[at++]=(c ushr 8).toByte();rgb[at++]=c.toByte()};val dims=PagePixelDimensions(image.width,image.height);val pixel=CanonicalPagePixelDigests.digest(dims,PagePixelFormat.SRGB_8_RGB_OPAQUE,rgb);val provenance=PageRepresentationProvenance(EvidenceArtifactId("evidence-a51887d1-1a40-4b68-b340-c60e02e9a8d9"),sourceSha,1_887_733,"application/pdf",index+1,5,DeterministicSourcePageRenderer.PDFBOX_ID,DeterministicSourcePageRenderer.PDFBOX_VERSION,DeterministicSourcePageRenderer.PDFBOX_BUILD,PageRenderProfile("authoritative-page-region-raster-v1",1,300),SourcePageDimensions("595.0","842.0","PDF_POINT"),0,dims,pixel,sha256(encoded));AuthoritativePageRepresentation(PageRepresentationId(id),provenance,encoded,rgb)}
    }
}
