package parker.core.runtime

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import parker.core.interfaces.*

/** Write-once local persistence for page/geometry/order derivatives. */
class FileSystemGovernedPageRepresentationPersistence(root: Path) : GovernedPageRepresentationPersistence {
    private val root = root.toAbsolutePath().normalize()
    private val pages = this.root.resolve("pages")
    private val geometry = this.root.resolve("geometry")
    private val order = this.root.resolve("order")
    init { Files.createDirectories(pages); Files.createDirectories(geometry); Files.createDirectories(order) }

    override fun persistPage(representation: AuthoritativePageRepresentation) {
        val target = pages.resolve("${representation.id.value}.bin")
        if (Files.exists(target)) { require(Files.readAllBytes(target).contentEquals(representation.encodedBytes())) { "page representation identity conflict" }; return }
        atomicWrite(target, representation.encodedBytes())
        val p = representation.provenance
        atomicWrite(pages.resolve("${representation.id.value}.json"), "{\"format\":1,\"source_evidence_id\":\"${p.sourceEvidenceArtifactId.value}\",\"source_sha256\":\"${p.sourceSha256}\",\"source_page\":${p.pageNumber},\"page_count\":${p.declaredPageCount},\"renderer\":\"${p.rendererIdentity}\",\"renderer_version\":\"${p.rendererVersion}\",\"render_profile\":\"${p.renderProfile.profileId}\",\"render_profile_version\":${p.renderProfile.profileVersion},\"dpi\":${p.renderProfile.dpi},\"media_type\":\"${p.renderProfile.encodedMediaType}\",\"width\":${p.pixelDimensions.width},\"height\":${p.pixelDimensions.height},\"representation_sha256\":\"${p.encodedRepresentationSha256}\"}".toByteArray())
    }
    override fun persistGeometry(graph: SourceRegionOrderGraph) {
        val target = geometry.resolve("${graph.pageRepresentationId.value}.json")
        if (Files.exists(target)) return
        val body = buildString {
            append("{\"format\":1,\"page_representation_id\":\"").append(graph.pageRepresentationId.value).append("\",\"region_set_digest\":\"")
            append(SourceRegionSetIdentity.digest(graph)).append("\",\"regions\":[")
            graph.regions.forEachIndexed { i, r -> if (i > 0) append(','); append("{\"id\":\"").append(r.id.value).append("\",\"left\":").append(r.bounds.left).append(",\"top\":").append(r.bounds.top).append(",\"right\":").append(r.bounds.rightExclusive).append(",\"bottom\":").append(r.bounds.bottomExclusive).append('}') }
            append("]}")
        }
        atomicWrite(target, body.toByteArray())
    }
    override fun persistOrderState(state: SourceRegionOrderState) {
        val target = order.resolve("${state.pageRepresentationId.value}.json")
        if (Files.exists(target)) { require(Files.readString(target).contains(state.regionSetDigest)) { "order state identity conflict" }; return }
        val ids = state.regionIds.joinToString(",") { "\"${it.value}\"" }
        val reason = state.reason?.let { "\"${it.replace("\\", "\\\\").replace("\"", "\\\"")}\"" } ?: "null"
        val body = "{\"format\":1,\"page_representation_id\":\"${state.pageRepresentationId.value}\",\"region_set_digest\":\"${state.regionSetDigest}\",\"disposition\":\"${state.disposition}\",\"region_ids\":[$ids],\"derivation_profile_id\":\"${state.derivationProfileId}\",\"derivation_profile_version\":${state.derivationProfileVersion},\"reason\":$reason}"
        atomicWrite(target, body.toByteArray())
    }
    override fun readOrderState(pageRepresentationId: PageRepresentationId): SourceRegionOrderState? {
        val text = order.resolve("${pageRepresentationId.value}.json").let { if (Files.exists(it)) Files.readString(it) else return null }
        val digest = Regex("\"region_set_digest\":\"([0-9a-f]{64})").find(text)!!.groupValues[1]
        val disposition = Regex("\"disposition\":\"([^\"]+)").find(text)!!.groupValues[1]
        val ids = Regex("\"region_ids\":\\[(.*?)\\]").find(text)!!.groupValues[1].split(',').filter { it.isNotBlank() }.map { SourceRegionId(it.trim().trim('"')) }
        val profile = Regex("\"derivation_profile_id\":\"([^\"]+)").find(text)!!.groupValues[1]
        val version = Regex("\"derivation_profile_version\":(\\d+)").find(text)!!.groupValues[1].toInt()
        val reason = Regex("\"reason\":(null|\"([^\"]*)\")").find(text)?.groupValues?.get(2)
        return SourceRegionOrderState(pageRepresentationId, digest, ids, disposition, profile, version, reason)
    }
    private fun atomicWrite(target: Path, bytes: ByteArray) { val tmp = Files.createTempFile(root, ".tmp-", ".bin"); try { Files.write(tmp, bytes, StandardOpenOption.TRUNCATE_EXISTING); Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE) } finally { Files.deleteIfExists(tmp) } }
}

class InMemoryGovernedPageRepresentationPersistence : GovernedPageRepresentationPersistence {
    val pages = linkedMapOf<PageRepresentationId, AuthoritativePageRepresentation>()
    val geometries = linkedMapOf<PageRepresentationId, SourceRegionOrderGraph>()
    val orders = linkedMapOf<PageRepresentationId, SourceRegionOrderState>()
    override fun persistPage(representation: AuthoritativePageRepresentation) { require(pages.putIfAbsent(representation.id, representation) == null) { "duplicate page representation" } }
    override fun persistGeometry(graph: SourceRegionOrderGraph) { require(geometries.putIfAbsent(graph.pageRepresentationId, graph) == null) { "duplicate geometry" } }
    override fun persistOrderState(state: SourceRegionOrderState) { require(orders.putIfAbsent(state.pageRepresentationId, state) == null) { "duplicate order state" } }
    override fun readOrderState(pageRepresentationId: PageRepresentationId) = orders[pageRepresentationId]
}

data class OwnerSourceOrderInput(val ownerIdentity: String, val acceptedAt: String, val orderedRegionIds: List<SourceRegionId>)

object OwnerSourceOrderValidator {
    fun validate(graph: SourceRegionOrderGraph, input: OwnerSourceOrderInput): Result<OwnerSourceOrderResolution> = runCatching {
        require(graph.ambiguityState == SourceRegionAmbiguityState.HUMAN_ORDER_REQUIRED)
        val expected = graph.regions.map { it.id }.toSet()
        require(input.orderedRegionIds.size == expected.size && input.orderedRegionIds.toSet() == expected)
        val setDigest = SourceRegionSetIdentity.digest(graph)
        val recordId = sha256(listOf("parker.owner-source-order.v1", graph.pageRepresentationId.value, setDigest, input.ownerIdentity, input.acceptedAt, input.orderedRegionIds.joinToString(",")))
        val recordDigest = sha256("$recordId|${graph.provenanceEvidenceId()}|$setDigest".toByteArray())
        OwnerSourceOrderResolution(recordId, graph.regions.first().provenance.sourceEvidenceArtifactId, graph.regions.first().provenance.sourceSha256, graph.regions.first().provenance.pageNumber, graph.pageRepresentationId, graph.regions.first().provenance.pageCanonicalPixelDigest.value, setDigest, input.orderedRegionIds, "SOURCE_ORDER_REVIEW_REQUIRED", input.ownerIdentity, input.acceptedAt, recordSha256 = recordDigest)
    }
    private fun sha256(bytes: ByteArray) = java.security.MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it.toInt() and 255) }
    private fun sha256(values: List<String>) = sha256(values.joinToString("\u0000").toByteArray())
    private fun SourceRegionOrderGraph.provenanceEvidenceId() = regions.firstOrNull()?.provenance?.sourceEvidenceArtifactId?.value ?: "none"
}

fun SourceRegionOrderGraph.withOwnerOrder(order: List<SourceRegionId>): SourceRegionOrderGraph {
    require(regions.map { it.id }.toSet() == order.toSet() && order.size == regions.size)
    val edges = order.zipWithNext().map { SourceRegionOrderEdge(it.first, it.second, SourceRegionOrderRelation.BEFORE) }.toSet()
    return copy(edges = edges, ambiguityState = SourceRegionAmbiguityState.UNAMBIGUOUS, reason = null)
}
