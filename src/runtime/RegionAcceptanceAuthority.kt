package parker.core.runtime

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.security.MessageDigest
import java.time.Instant
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream as ImageByteArrayOutputStream
import javax.imageio.ImageIO
import parker.core.interfaces.*

const val REGION_ACCEPTANCE_AUTHORITY_SCHEMA = "parker.region-transcription-acceptance-authority.v1"
const val REGION_ACCEPTANCE_AUTHORITY_PRODUCTION_CONTAINER_ROOT = "/data/region-transcription-acceptance-authorities"
const val REGION_ACCEPTANCE_AUTHORITY_RECOMMENDED_HOST_ROOT = "/mnt/parker-data/parker/region-transcription-acceptance-authorities"

/** A canonical, deliberately provider-neutral fact in a reconstructed region acquisition plan. */
data class RegionAcceptanceFact(val name: String, val value: String) {
    init {
        require(name.matches(Regex("^[a-z][a-z0-9_.-]{0,199}$")))
        require(value.length <= 1_000_000 && value.none { it == '\u0000' })
    }
}

/**
 * Complete facts Parker must reconstruct from custody bytes and effective runtime configuration.
 * Names are sorted before encoding, so neither map iteration nor locale can alter the digest.
 */
data class RegionAcceptanceManifest(val facts: List<RegionAcceptanceFact>) {
    init {
        require(facts.isNotEmpty())
        require(facts == facts.sortedBy { it.name }) { "region acceptance facts must be canonical-name ordered" }
        require(facts.map { it.name }.distinct().size == facts.size)
        val names = facts.mapTo(mutableSetOf()) { it.name }
        REQUIRED.forEach { require(it in names) { "missing region acceptance fact: $it" } }
        require(names.any { it.startsWith("page.") }) { "page representation facts required" }
        require(names.any { it.startsWith("region.") }) { "region geometry facts required" }
        require(names.any { it.startsWith("order.") }) { "source order graph facts required" }
    }

    fun canonicalBytes(): ByteArray = ByteArrayOutputStream().use { bytes ->
        DataOutputStream(bytes).use { out ->
            out.writeUTF(REGION_ACCEPTANCE_AUTHORITY_SCHEMA)
            out.writeInt(facts.size)
            facts.forEach { fact -> write(out, fact.name); write(out, fact.value) }
        }
        bytes.toByteArray()
    }

    fun sha256(): String = digest(canonicalBytes())

    companion object {
        private val REQUIRED = setOf(
            "source.evidence_artifact_id", "source.sha256", "source.byte_length", "source.media_type",
            "deployment.source_commit", "deployment.build_commit", "deployment.runtime_commit", "deployment.image_id",
            "request.correlation_id", "request.profile_id", "request.processing_profile", "request.schema_id",
            "request.schema_version", "request.schema_sha256", "request.instruction_sha256",
            "provider.name", "provider.model", "provider.adapter_id", "provider.adapter_version",
            "provider.endpoint", "provider.wire_version", "provider.image_detail", "provider.store",
            "context.policy", "attempt.maximum_provider_attempts",
        )

        fun canonical(facts: Iterable<RegionAcceptanceFact>) = RegionAcceptanceManifest(facts.sortedBy { it.name })

        internal fun decode(bytes: ByteArray): RegionAcceptanceManifest = DataInputStream(ByteArrayInputStream(bytes)).use { input ->
            require(input.readUTF() == REGION_ACCEPTANCE_AUTHORITY_SCHEMA)
            val count = input.readInt(); require(count in 1..20_000)
            val facts = List(count) { RegionAcceptanceFact(read(input), read(input)) }
            require(input.read() == -1)
            RegionAcceptanceManifest(facts)
        }

        private fun write(out: DataOutputStream, value: String) {
            val bytes = value.toByteArray(StandardCharsets.UTF_8); out.writeInt(bytes.size); out.write(bytes)
        }
        private fun read(input: DataInputStream): String {
            val size = input.readInt(); require(size in 0..1_000_000)
            return String(ByteArray(size).also { input.readFully(it) }, StandardCharsets.UTF_8)
        }
    }
}

/** Separate from, and intentionally incompatible with, the legacy A1 acceptance authority. */
data class RegionTranscriptionAcceptanceAuthority(
    val authorityId: String,
    val programmeUnit: String,
    val executionId: String,
    val manifest: RegionAcceptanceManifest,
    val maximumProviderAttempts: Int,
    val authorisedBy: String,
    val authorisedAt: Instant,
) {
    init {
        val id = Regex("^[A-Za-z0-9_.-]{1,120}$")
        require(id.matches(authorityId) && id.matches(programmeUnit) && id.matches(executionId))
        require(maximumProviderAttempts == 1)
        require(authorisedBy.isNotBlank() && authorisedBy.length <= 256)
    }
    val manifestSha256: String get() = manifest.sha256()
    val recordId: String get() = digest(canonicalPayload())
    internal fun canonicalPayload(): ByteArray = ByteArrayOutputStream().use { bytes ->
        DataOutputStream(bytes).use { out ->
            listOf(authorityId, programmeUnit, executionId, maximumProviderAttempts.toString(), authorisedBy, authorisedAt.toString())
                .forEach { value -> val encoded = value.toByteArray(StandardCharsets.UTF_8); out.writeInt(encoded.size); out.write(encoded) }
            val manifestBytes = manifest.canonicalBytes(); out.writeInt(manifestBytes.size); out.write(manifestBytes)
        }
        bytes.toByteArray()
    }
}

/** Create-once, fsync-backed storage under the dedicated region authority root. */
class FileSystemRegionAcceptanceAuthorityStorage(storageRoot: Path) {
    private val root = storageRoot.toAbsolutePath().normalize()
    init { require(Files.isDirectory(root) && Files.isReadable(root) && Files.isWritable(root)) }

    fun admit(authority: RegionTranscriptionAcceptanceAuthority) {
        val payload = authority.canonicalPayload()
        val envelope = ByteArrayOutputStream().use { bytes ->
            DataOutputStream(bytes).use { out ->
                out.writeUTF(REGION_ACCEPTANCE_AUTHORITY_SCHEMA); out.writeInt(payload.size); out.write(payload)
                out.writeUTF(digest(payload)); out.writeUTF(authority.recordId)
            }; bytes.toByteArray()
        }
        try {
            FileChannel.open(target(authority.authorityId), StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE).use { channel ->
                val buffer = ByteBuffer.wrap(envelope); while (buffer.hasRemaining()) channel.write(buffer); channel.force(true)
            }
            if (!System.getProperty("os.name").startsWith("Windows", true)) FileChannel.open(root, StandardOpenOption.READ).use { it.force(true) }
        } catch (_: java.nio.file.FileAlreadyExistsException) {
            require(load(authority.authorityId) == authority) { "region acceptance authority identity conflict" }
        }
    }

    fun load(authorityId: String): RegionTranscriptionAcceptanceAuthority? {
        require(ID.matches(authorityId)); val target = target(authorityId)
        if (!Files.exists(target)) return null
        require(Files.isRegularFile(target) && Files.size(target) in 1..MAX_BYTES)
        return DataInputStream(Files.newInputStream(target)).use { input ->
            require(input.readUTF() == REGION_ACCEPTANCE_AUTHORITY_SCHEMA)
            val size = input.readInt(); require(size in 1..MAX_BYTES.toInt())
            val payload = ByteArray(size).also { input.readFully(it) }
            require(input.readUTF() == digest(payload)) { "region acceptance authority checksum mismatch" }
            val expectedRecordId = input.readUTF(); require(input.read() == -1)
            decodeAuthority(payload).also { require(it.recordId == expectedRecordId) { "region acceptance record identity mismatch" } }
        }
    }

    private fun target(id: String) = root.resolve("$id.region-acceptance-authority").normalize().also { require(it.parent == root) }
    private fun decodeAuthority(payload: ByteArray): RegionTranscriptionAcceptanceAuthority = DataInputStream(ByteArrayInputStream(payload)).use { input ->
        fun text(): String { val size = input.readInt(); require(size in 0..1_000_000); return String(ByteArray(size).also { input.readFully(it) }, StandardCharsets.UTF_8) }
        val authorityId = text(); val unit = text(); val executionId = text(); val attempts = text().toInt(); val by = text(); val at = Instant.parse(text())
        val manifestSize = input.readInt(); require(manifestSize in 1..MAX_BYTES.toInt())
        val manifest = RegionAcceptanceManifest.decode(ByteArray(manifestSize).also { input.readFully(it) }); require(input.read() == -1)
        RegionTranscriptionAcceptanceAuthority(authorityId, unit, executionId, manifest, attempts, by, at)
    }
    private companion object { val ID = Regex("^[A-Za-z0-9_.-]{1,120}$"); const val MAX_BYTES = 16L * 1024L * 1024L }
}

data class RegionAcceptanceReconstruction(val manifest: RegionAcceptanceManifest, val binding: GovernedRegionExecutionBinding)
fun interface RegionAcceptanceReconstructor { suspend fun reconstruct(authority: RegionTranscriptionAcceptanceAuthority): RegionAcceptanceReconstruction? }
fun interface GovernedRegionExecutionPort { suspend fun execute(binding: GovernedRegionExecutionBinding): GovernedRegionExecutionOutcome }

enum class RegionAcceptanceContextPolicy { REGION_ONLY, FULL_PAGE_CONTEXT }

data class RegionAcceptanceDeploymentFacts(
    val sourceCommit: String, val buildCommit: String, val runtimeCommit: String, val imageId: String,
    val providerEndpoint: String,
)

/** Builds the same complete manifest for authority creation and invocation-time reconstruction. */
object RegionAcceptanceManifestFactory {
    fun create(
        sourceId: EvidenceArtifactId, sourceSha256: String, sourceByteLength: Long, sourceMediaType: String,
        pages: List<AuthoritativePageRepresentation>, graphs: List<SourceRegionOrderGraph>, request: RegionTranscriptionRequest,
        contextPolicy: RegionAcceptanceContextPolicy, deployment: RegionAcceptanceDeploymentFacts,
    ): RegionAcceptanceManifest {
        require(pages.isNotEmpty() && pages.size == graphs.size)
        val facts = mutableListOf(
            RegionAcceptanceFact("source.evidence_artifact_id", sourceId.value), RegionAcceptanceFact("source.sha256", sourceSha256),
            RegionAcceptanceFact("source.byte_length", sourceByteLength.toString()), RegionAcceptanceFact("source.media_type", sourceMediaType),
            RegionAcceptanceFact("deployment.source_commit", deployment.sourceCommit), RegionAcceptanceFact("deployment.build_commit", deployment.buildCommit),
            RegionAcceptanceFact("deployment.runtime_commit", deployment.runtimeCommit), RegionAcceptanceFact("deployment.image_id", deployment.imageId),
            RegionAcceptanceFact("request.correlation_id", request.correlationId), RegionAcceptanceFact("request.profile_id", request.transcriptionProfileId),
            RegionAcceptanceFact("request.processing_profile", request.processingProfile), RegionAcceptanceFact("request.schema_id", request.schemaId),
            RegionAcceptanceFact("request.schema_version", request.schemaVersion.toString()), RegionAcceptanceFact("request.schema_sha256", request.schemaSha256),
            RegionAcceptanceFact("request.instruction_sha256", regionSha256(request.literalInstruction.toByteArray(StandardCharsets.UTF_8))),
            RegionAcceptanceFact("provider.name", "OpenAI"), RegionAcceptanceFact("provider.model", OPENAI_REGION_MODEL),
            RegionAcceptanceFact("provider.adapter_id", OPENAI_REGION_ADAPTER_ID), RegionAcceptanceFact("provider.adapter_version", OPENAI_REGION_ADAPTER_VERSION),
            RegionAcceptanceFact("provider.endpoint", deployment.providerEndpoint), RegionAcceptanceFact("provider.wire_version", REGION_TRANSCRIPTION_WIRE_VERSION.toString()),
            RegionAcceptanceFact("provider.image_detail", OPENAI_REGION_IMAGE_DETAIL), RegionAcceptanceFact("provider.store", "false"),
            RegionAcceptanceFact("context.policy", contextPolicy.name), RegionAcceptanceFact("attempt.maximum_provider_attempts", "1"),
        )
        pages.sortedBy { it.provenance.pageNumber }.forEachIndexed { index, page ->
            val p = page.provenance; val prefix = "page.${index + 1}"
            facts += RegionAcceptanceFact("$prefix.number", p.pageNumber.toString()); facts += RegionAcceptanceFact("$prefix.representation_id", page.id.value)
            facts += RegionAcceptanceFact("$prefix.pixel_dimensions", "${p.pixelDimensions.width},${p.pixelDimensions.height}")
            facts += RegionAcceptanceFact("$prefix.pixel_digest", p.canonicalPixelDigest.value); facts += RegionAcceptanceFact("$prefix.encoded_sha256", p.encodedRepresentationSha256)
            facts += RegionAcceptanceFact("$prefix.renderer", "${p.rendererIdentity}|${p.rendererVersion}|${p.rendererBuildIdentity}")
            facts += RegionAcceptanceFact("$prefix.render_profile", "${p.renderProfile.profileId}|${p.renderProfile.profileVersion}|${p.renderProfile.dpi}|${p.renderProfile.pixelFormat}|${p.renderProfile.orientationPolicy}|${p.renderProfile.transparencyPolicy}")
        }
        val targets = request.targets.associateBy { it.sourceRegionId }
        graphs.sortedBy { it.regions.firstOrNull()?.provenance?.pageNumber ?: Int.MAX_VALUE }.flatMap { it.regions }.forEachIndexed { index, region ->
            val prefix = "region.${index + 1}"; val target = targets.getValue(region.id); val b = region.bounds
            facts += RegionAcceptanceFact("$prefix.id", region.id.value); facts += RegionAcceptanceFact("$prefix.page_number", region.provenance.pageNumber.toString())
            facts += RegionAcceptanceFact("$prefix.page_representation_id", region.provenance.pageRepresentationId.value)
            facts += RegionAcceptanceFact("$prefix.page_dimensions", "${region.provenance.pagePixelDimensions.width},${region.provenance.pagePixelDimensions.height}")
            facts += RegionAcceptanceFact("$prefix.bounds", "${b.left},${b.top},${b.rightExclusive},${b.bottomExclusive}")
            facts += RegionAcceptanceFact("$prefix.crop_digest", region.cropDigest.value); facts += RegionAcceptanceFact("$prefix.structural_class", region.structuralClass.name)
            facts += RegionAcceptanceFact("$prefix.derivation_profile", "${region.provenance.derivationProfileId}|${region.provenance.derivationProfileVersion}")
            facts += RegionAcceptanceFact("$prefix.encoded_sha256", target.regionImage.encodedSha256)
            facts += RegionAcceptanceFact("$prefix.context_encoded_sha256", target.pageContextImage?.encodedSha256 ?: "NONE")
        }
        graphs.forEachIndexed { pageIndex, graph ->
            facts += RegionAcceptanceFact("order.${pageIndex + 1}.page_representation_id", graph.pageRepresentationId.value)
            facts += RegionAcceptanceFact("order.${pageIndex + 1}.ambiguity", graph.ambiguityState.name)
            facts += RegionAcceptanceFact("order.${pageIndex + 1}.reason", graph.reason ?: "NONE")
            graph.edges.sortedWith(compareBy({ it.from.value }, { it.to.value }, { it.relation.name })).forEachIndexed { edgeIndex, edge ->
                facts += RegionAcceptanceFact("order.${pageIndex + 1}.edge.${edgeIndex + 1}", "${edge.from.value}|${edge.to.value}|${edge.relation.name}")
            }
        }
        request.targets.forEachIndexed { index, target -> facts += RegionAcceptanceFact("order.source.${index + 1}", target.sourceRegionId.value) }
        return RegionAcceptanceManifest.canonical(facts)
    }
}

/** Reconstructs only from Evidence Custodian bytes plus fixed runtime configuration. */
internal class CustodyRegionAcceptanceReconstructor(
    evidenceCustodian: EvidenceCustodian,
    private val ownerPrincipalId: PrincipalId,
    private val deployment: RegionAcceptanceDeploymentFacts,
    private val contextPolicy: RegionAcceptanceContextPolicy,
    private val renderer: DeterministicSourcePageRenderer = DeterministicSourcePageRenderer(),
    private val deriver: DeterministicSourceRegionDeriver = DeterministicSourceRegionDeriver(),
) : RegionAcceptanceReconstructor {
    private val sourceResolver = AuthoritativeAcquisitionSourceResolver(evidenceCustodian)

    override suspend fun reconstruct(authority: RegionTranscriptionAcceptanceAuthority): RegionAcceptanceReconstruction? {
        val authorisedFacts = authority.manifest.facts.associate { it.name to it.value }
        val evidenceId = runCatching { EvidenceArtifactId(authorisedFacts.getValue("source.evidence_artifact_id")) }.getOrNull() ?: return null
        val source = (sourceResolver.resolveSourceThenManifest(ownerPrincipalId, evidenceId) as? AuthoritativeAcquisitionResolution.Verified)?.input ?: return null
        val mediaType = source.mediaType ?: return null; val bytes = source.bytes()
        val first = renderer.render(SourcePageRenderRequest(evidenceId, source.sha256, mediaType, bytes, 1, renderProfile(mediaType))) as? SourcePageRepresentationOutcome.Created ?: return null
        val pageCount = first.representation.provenance.declaredPageCount
        val pages = mutableListOf(first.representation)
        for (pageNumber in 2..pageCount) {
            val rendered = renderer.render(SourcePageRenderRequest(evidenceId, source.sha256, mediaType, bytes, pageNumber, renderProfile(mediaType))) as? SourcePageRepresentationOutcome.Created ?: return null
            pages += rendered.representation
        }
        val graphs = pages.map { (deriver.derive(it) as? SourceRegionDerivationOutcome.Derived)?.graph ?: return null }
        val correlation = authorisedFacts["request.correlation_id"] ?: return null
        val targets = pages.zip(graphs).flatMap { (page, graph) -> graph.regions.map { region -> target(source, page, region) } }
        if (targets.size !in 1..RegionTranscriptionRequest.MAX_REGIONS_PER_REQUEST) return null
        val request = RegionTranscriptionRequest(correlation, REGION_TRANSCRIPTION_PROFILE_ID, REGION_TRANSCRIPTION_SCHEMA_ID, REGION_TRANSCRIPTION_WIRE_VERSION,
            REGION_TRANSCRIPTION_SCHEMA_SHA256, REGION_TRANSCRIPTION_PROCESSING_PROFILE, REGION_LITERAL_TRANSCRIPTION_INSTRUCTION, targets)
        val manifest = RegionAcceptanceManifestFactory.create(evidenceId, source.sha256, source.byteLength, mediaType, pages, graphs, request, contextPolicy, deployment)
        val requestDigest = regionAcceptanceRequestDigest(request)
        val identity = FidelityFirstExecutionIdentity(authority.executionId, requestDigest, correlation, evidenceId.value, source.sha256, source.byteLength, mediaType,
            deployment.runtimeCommit, "OpenAI", OPENAI_REGION_MODEL, OPENAI_REGION_PROFILE_ID, OPENAI_REGION_INSTRUCTION_SHA256, request.schemaSha256,
            request.processingProfile, OPENAI_REGION_ADAPTER_VERSION)
        return RegionAcceptanceReconstruction(manifest, GovernedRegionExecutionBinding(identity, request, targets.map { it.sourceRegionId }))
    }

    private fun renderProfile(mediaType: String) = PageRenderProfile("authoritative-page-region-raster-v1", 1, if (mediaType == "application/pdf") 300 else null)
    private fun target(source: AuthoritativeAcquisitionInput, page: AuthoritativePageRepresentation, region: SourceRegion): RegionTranscriptionTarget {
        val crop = renderer.crop(page, region.bounds); val regionBytes = encode(crop.dimensions, crop.canonicalPixels())
        val regionImage = RegionTranscriptionImage(page.id, region.bounds, region.cropDigest, "image/png", RegionTranscriptionImage.sha256(regionBytes), regionBytes)
        val context = if (contextPolicy == RegionAcceptanceContextPolicy.FULL_PAGE_CONTEXT) RegionTranscriptionImage(page.id,
            PixelCropBounds(0, 0, page.provenance.pixelDimensions.width, page.provenance.pixelDimensions.height), page.provenance.canonicalPixelDigest,
            "image/png", page.provenance.encodedRepresentationSha256, page.encodedBytes()) else null
        return RegionTranscriptionTarget(source.evidenceArtifactId, source.sha256, page.id, page.provenance.pageNumber, page.provenance.pixelDimensions,
            region.id, region.bounds, region.cropDigest, region.structuralClass, region.provenance.derivationProfileId, region.provenance.derivationProfileVersion, regionImage, context)
    }
    private fun encode(dimensions: PagePixelDimensions, pixels: ByteArray): ByteArray {
        val image = BufferedImage(dimensions.width, dimensions.height, BufferedImage.TYPE_INT_RGB); var i = 0
        for (y in 0 until dimensions.height) for (x in 0 until dimensions.width) image.setRGB(x, y, ((pixels[i++].toInt() and 255) shl 16) or ((pixels[i++].toInt() and 255) shl 8) or (pixels[i++].toInt() and 255))
        return ImageByteArrayOutputStream().use { output -> check(ImageIO.write(image, "png", output)); output.toByteArray() }
    }
}

private fun regionAcceptanceRequestDigest(r: RegionTranscriptionRequest): String = regionSha256(RegionJson.encode(linkedMapOf<String, Any?>(
    "correlation_id" to r.correlationId, "transcription_profile" to r.transcriptionProfileId,
    "schema_id" to r.schemaId, "schema_version" to r.schemaVersion, "schema_sha256" to r.schemaSha256,
    "processing_profile" to r.processingProfile, "instruction_sha256" to regionSha256(r.literalInstruction.toByteArray()),
    "provider" to "OpenAI", "model" to OPENAI_REGION_MODEL, "adapter_id" to OPENAI_REGION_ADAPTER_ID,
    "adapter_version" to OPENAI_REGION_ADAPTER_VERSION, "reasoning" to "none", "store" to false, "image_detail" to OPENAI_REGION_IMAGE_DETAIL,
    "targets" to r.targets.map { t -> linkedMapOf("evidence_artifact_id" to t.sourceEvidenceArtifactId.value, "source_sha256" to t.sourceSha256,
        "page_representation_id" to t.pageRepresentationId.value, "page_number" to t.pageNumber, "source_region_id" to t.sourceRegionId.value,
        "crop_digest" to t.cropDigest.value, "region_image_sha256" to t.regionImage.encodedSha256,
        "page_context_supplied" to (t.pageContextImage != null), "page_context_sha256" to t.pageContextImage?.encodedSha256) },
)).toByteArray(StandardCharsets.UTF_8))

sealed interface RegionAcceptanceExecutionOutcome {
    data class Executed(val authorityId: String, val recordId: String, val outcome: GovernedRegionExecutionOutcome) : RegionAcceptanceExecutionOutcome
    data class Blocked(val reason: String) : RegionAcceptanceExecutionOutcome
}

/** The only acceptance bridge: callers supply an authority id, never source/request/provider facts. */
class RegionAcceptanceExecutionCoordinator(
    private val authorities: FileSystemRegionAcceptanceAuthorityStorage,
    private val lifecycle: () -> FidelityFirstAcceptanceLifecycle,
    private val deployedSourceCommit: () -> String?,
    private val deployedBuildCommit: () -> String?,
    private val deployedRuntimeCommit: () -> String?,
    private val deployedImageId: () -> String?,
    private val reconstructor: RegionAcceptanceReconstructor,
    private val execution: GovernedRegionExecutionPort,
) {
    suspend fun invoke(authorityId: String): RegionAcceptanceExecutionOutcome {
        val authority = try { authorities.load(authorityId) } catch (_: Exception) { return blocked("AUTHORITY_CORRUPT") }
            ?: return blocked("AUTHORITY_MISSING")
        if (lifecycle() != FidelityFirstAcceptanceLifecycle.ACCEPTANCE_PENDING) return blocked("LIFECYCLE_NOT_ACCEPTANCE_PENDING")
        val facts = authority.manifest.facts.associate { it.name to it.value }
        if (deployedSourceCommit() != facts["deployment.source_commit"] || deployedBuildCommit() != facts["deployment.build_commit"] ||
            deployedRuntimeCommit() != facts["deployment.runtime_commit"] || deployedImageId() != facts["deployment.image_id"]
        ) return blocked("DEPLOYMENT_IDENTITY_MISMATCH")
        val current = try { reconstructor.reconstruct(authority) } catch (_: Exception) { null }
            ?: return blocked("CURRENT_FACTS_UNAVAILABLE")
        if (current.manifest.sha256() != authority.manifestSha256 || current.manifest != authority.manifest) return blocked("AUTHORITY_FACTS_MISMATCH")
        if (current.binding.identity.executionId != authority.executionId) return blocked("EXECUTION_IDENTITY_MISMATCH")
        return RegionAcceptanceExecutionOutcome.Executed(authority.authorityId, authority.recordId, execution.execute(current.binding))
    }
    private fun blocked(reason: String) = RegionAcceptanceExecutionOutcome.Blocked(reason)
}

private fun digest(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
