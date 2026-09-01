package parker.core.interfaces

/** Immutable, governed persistence boundary for rendered pages, source regions and order state. */
interface GovernedPageRepresentationPersistence {
    fun persistPage(representation: AuthoritativePageRepresentation)
    fun persistGeometry(graph: SourceRegionOrderGraph)
    fun persistOrderState(state: SourceRegionOrderState)
    fun readOrderState(pageRepresentationId: PageRepresentationId): SourceRegionOrderState?
}

data class SourceRegionOrderState(
    val pageRepresentationId: PageRepresentationId,
    val regionSetDigest: String,
    val regionIds: List<SourceRegionId>,
    val disposition: String,
    val derivationProfileId: String,
    val derivationProfileVersion: Int,
    val reason: String? = null,
) {
    init {
        require(regionSetDigest.matches(Regex("^[0-9a-f]{64}$")))
        require(regionIds.isNotEmpty() && regionIds.distinct().size == regionIds.size)
        require(disposition in setOf("DETERMINISTIC_SOURCE_ORDER", "SOURCE_ORDER_REVIEW_REQUIRED"))
        require(disposition == "DETERMINISTIC_SOURCE_ORDER" || reason != null)
    }
}

data class OwnerSourceOrderResolution(
    val recordId: String,
    val sourceEvidenceArtifactId: EvidenceArtifactId,
    val sourceSha256: String,
    val pageNumber: Int,
    val pageRepresentationId: PageRepresentationId,
    val pageRepresentationDigest: String,
    val regionSetDigest: String,
    val orderedRegionIds: List<SourceRegionId>,
    val priorDisposition: String,
    val ownerIdentity: String,
    val acceptedAt: String,
    val status: String = "OWNER_RESOLVED_SOURCE_ORDER",
    val formatVersion: Int = 1,
    val recordSha256: String,
) {
    init {
        require(recordId.isNotBlank() && sourceSha256.matches(Regex("^[0-9a-f]{64}$")))
        require(pageNumber > 0 && pageRepresentationDigest.matches(Regex("^[0-9a-f]{64}$")))
        require(regionSetDigest.matches(Regex("^[0-9a-f]{64}$")) && orderedRegionIds.isNotEmpty())
        require(priorDisposition == "SOURCE_ORDER_REVIEW_REQUIRED" && status == "OWNER_RESOLVED_SOURCE_ORDER")
        require(ownerIdentity.isNotBlank() && acceptedAt.isNotBlank() && formatVersion > 0)
        require(recordSha256.matches(Regex("^[0-9a-f]{64}$")))
    }
}

object SourceRegionSetIdentity {
    fun digest(graph: SourceRegionOrderGraph): String = digest(listOf(graph))
    fun digest(graphs: List<SourceRegionOrderGraph>): String {
        val fields = buildList {
            add("parker.source-region-set.v1")
            graphs.sortedBy { it.regions.firstOrNull()?.provenance?.pageNumber ?: Int.MAX_VALUE }.forEach { graph ->
                add(graph.pageRepresentationId.value)
                graph.regions.sortedBy { it.id.value }.forEach { region ->
                    add(region.id.value); add(region.bounds.left.toString()); add(region.bounds.top.toString())
                    add(region.bounds.rightExclusive.toString()); add(region.bounds.bottomExclusive.toString())
                    add(region.cropDigest.value); add(region.provenance.derivationProfileId)
                    add(region.provenance.derivationProfileVersion.toString())
                }
            }
        }
        return Digest.sha256(fields.joinToString("\u0000").toByteArray())
    }
}

private object Digest {
    fun sha256(bytes: ByteArray): String = java.security.MessageDigest.getInstance("SHA-256").digest(bytes)
        .joinToString("") { "%02x".format(it.toInt() and 255) }
}
