package parker.core.runtime

import java.security.MessageDigest
import parker.core.interfaces.*

/** Raster geometry only: no OCR, PDF text objects, language, provider, or semantic inference. */
class DeterministicSourceRegionDeriver(
    private val profile: SourceRegionDerivationProfile = SourceRegionDerivationProfile(),
    private val cropper: DeterministicSourcePageRenderer = DeterministicSourcePageRenderer(),
) : SourceRegionDeriver {
    override fun derive(page: AuthoritativePageRepresentation): SourceRegionDerivationOutcome {
        val provenance = page.provenance
        val pixels = page.canonicalPixels()
        if (pixels.size.toLong() != provenance.pixelDimensions.pixelCount * 3L) return SourceRegionDerivationOutcome.InvalidPageRepresentation
        val actual = CanonicalPagePixelDigests.digest(provenance.pixelDimensions, provenance.renderProfile.pixelFormat, pixels)
        if (actual != provenance.canonicalPixelDigest) return SourceRegionDerivationOutcome.PageDigestMismatch
        val lineBoxes = findLineBoxes(pixels, provenance.pixelDimensions)
        val blocks = mergeLineBoxes(lineBoxes, provenance.pixelDimensions)
        if (blocks.size > profile.maximumRegionsPerPage) return SourceRegionDerivationOutcome.ExcessiveRegions
        val regionProvenance = SourceRegionProvenance(
            provenance.sourceEvidenceArtifactId, provenance.sourceSha256, page.id, provenance.pageNumber,
            provenance.pixelDimensions, provenance.canonicalPixelDigest, profile.profileId, profile.version,
        )
        val regions = blocks.map { bounds ->
            val crop = try { cropper.crop(page, bounds) } catch (_: Exception) { return SourceRegionDerivationOutcome.InvalidGeometry }
            val structuralClass = classify(pixels, provenance.pixelDimensions, bounds)
            SourceRegion(identity(regionProvenance, bounds, structuralClass, crop.canonicalPixelDigest), bounds, structuralClass, crop.canonicalPixelDigest, regionProvenance)
        }.sortedWith(compareBy<SourceRegion>({ it.bounds.top }, { it.bounds.left }, { it.bounds.bottomExclusive }, { it.bounds.rightExclusive }, { it.id.value }))
        if (regions.map { it.id }.distinct().size != regions.size) return SourceRegionDerivationOutcome.RegionIdentityCollision
        return SourceRegionDerivationOutcome.Derived(buildGraph(page.id, regions))
    }

    private data class Box(val left: Int, val top: Int, val right: Int, val bottom: Int)

    private fun findLineBoxes(pixels: ByteArray, dims: PagePixelDimensions): List<Box> {
        val occupied = BooleanArray(dims.height)
        for (y in 0 until dims.height) {
            var count = 0
            for (x in 0 until dims.width) if (dark(pixels, (y * dims.width + x) * 3)) {
                count++; if (count >= profile.minimumDarkPixelsPerRow) { occupied[y] = true; break }
            }
        }
        val bands = mutableListOf<IntRange>(); var start = -1; var last = -1
        for (y in occupied.indices) if (occupied[y]) {
            if (start < 0 || y - last - 1 > profile.maximumIntraBandBlankRows) { if (start >= 0) bands += start..last; start = y }
            last = y
        }
        if (start >= 0) bands += start..last
        val result = mutableListOf<Box>()
        for (band in bands) {
            val cols = BooleanArray(dims.width)
            for (y in band) for (x in 0 until dims.width) if (dark(pixels, (y * dims.width + x) * 3)) cols[x] = true
            var xStart = -1; var xLast = -1
            for (x in cols.indices) if (cols[x]) {
                if (xStart < 0 || x - xLast - 1 > profile.horizontalSplitGapPixels) {
                    if (xStart >= 0) result += Box(xStart, band.first, xLast + 1, band.last + 1)
                    xStart = x
                }
                xLast = x
            }
            if (xStart >= 0) result += Box(xStart, band.first, xLast + 1, band.last + 1)
        }
        return result
    }

    private fun mergeLineBoxes(lines: List<Box>, dims: PagePixelDimensions): List<PixelCropBounds> {
        val pending = lines.sortedWith(compareBy<Box>({ it.top }, { it.left })).toMutableList(); val blocks = mutableListOf<Box>()
        while (pending.isNotEmpty()) {
            var block = pending.removeAt(0); var changed: Boolean
            do {
                changed = false
                val iterator = pending.iterator()
                while (iterator.hasNext()) {
                    val candidate = iterator.next()
                    val gap = candidate.top - block.bottom
                    val aligned = overlap(block.left, block.right, candidate.left, candidate.right) > 0 ||
                        kotlin.math.abs(candidate.left - block.left) <= profile.alignmentTolerancePixels
                    if (gap in 0..profile.maximumInterlineGapPixels && aligned) {
                        block = Box(minOf(block.left, candidate.left), minOf(block.top, candidate.top), maxOf(block.right, candidate.right), maxOf(block.bottom, candidate.bottom))
                        iterator.remove(); changed = true
                    }
                }
            } while (changed)
            val left = maxOf(0, block.left - profile.paddingPixels); val top = maxOf(0, block.top - profile.paddingPixels)
            val right = minOf(dims.width, block.right + profile.paddingPixels); val bottom = minOf(dims.height, block.bottom + profile.paddingPixels)
            if (right - left >= profile.minimumRegionWidthPixels && bottom - top >= profile.minimumRegionHeightPixels) blocks += Box(left, top, right, bottom)
        }
        return blocks.map { PixelCropBounds(it.left, it.top, it.right, it.bottom) }
    }

    private fun classify(pixels: ByteArray, dims: PagePixelDimensions, b: PixelCropBounds): SourceRegionStructuralClass {
        val width = b.rightExclusive - b.left; val height = b.bottomExclusive - b.top
        var darkCount = 0; var longRows = 0; val columnCounts = IntArray(width)
        for (y in b.top until b.bottomExclusive) {
            var row = 0
            for (x in b.left until b.rightExclusive) if (dark(pixels, (y * dims.width + x) * 3)) { darkCount++; row++; columnCounts[x - b.left]++ }
            if (row * 100 >= width * 60) longRows++
        }
        val longCols = columnCounts.count { it * 100 >= height * 60 }
        if (height <= 12 && width >= height * 10) return SourceRegionStructuralClass.RULE_OR_SEPARATOR
        if (longRows >= 2 && longCols >= 2) return SourceRegionStructuralClass.TABLE_LIKE
        val density = darkCount.toDouble() / (width.toLong() * height.toLong()).toDouble()
        return when { density >= 0.35 -> SourceRegionStructuralClass.IMAGE_LIKE; density >= 0.18 -> SourceRegionStructuralClass.MIXED; else -> SourceRegionStructuralClass.TEXT_LIKE }
    }

    private fun buildGraph(pageId: PageRepresentationId, regions: List<SourceRegion>): SourceRegionOrderGraph {
        val edges = linkedSetOf<SourceRegionOrderEdge>(); var ambiguous = false
        for (i in regions.indices) for (j in i + 1 until regions.size) {
            val a = regions[i]; val b = regions[j]; val xOverlap = overlap(a.bounds.left, a.bounds.rightExclusive, b.bounds.left, b.bounds.rightExclusive)
            val yOverlap = overlap(a.bounds.top, a.bounds.bottomExclusive, b.bounds.top, b.bounds.bottomExclusive)
            when {
                contains(a.bounds, b.bounds) -> edges += SourceRegionOrderEdge(a.id, b.id, SourceRegionOrderRelation.CONTAINS)
                contains(b.bounds, a.bounds) -> edges += SourceRegionOrderEdge(b.id, a.id, SourceRegionOrderRelation.CONTAINS)
                xOverlap > 0 && yOverlap > 0 -> ambiguous = true
                xOverlap > 0 -> {
                    val first = if (a.bounds.top < b.bounds.top) a else b; val second = if (first === a) b else a
                    edges += SourceRegionOrderEdge(first.id, second.id, SourceRegionOrderRelation.BEFORE)
                }
                yOverlap > 0 -> {
                    val left = if (a.bounds.left < b.bounds.left) a else b; val right = if (left === a) b else a
                    edges += SourceRegionOrderEdge(left.id, right.id, SourceRegionOrderRelation.COLUMN_PEER)
                }
            }
        }
        return SourceRegionOrderGraph(pageId, regions, edges,
            if (ambiguous) SourceRegionAmbiguityState.HUMAN_ORDER_REQUIRED else SourceRegionAmbiguityState.UNAMBIGUOUS,
            if (ambiguous) "Overlapping non-contained source regions admit competing order" else null)
    }

    private fun identity(p: SourceRegionProvenance, b: PixelCropBounds, c: SourceRegionStructuralClass, crop: CanonicalPixelDigest): SourceRegionId {
        val fields = listOf("parker.source-region.identity.v1", p.sourceEvidenceArtifactId.value, p.sourceSha256, p.pageRepresentationId.value,
            p.pageNumber.toString(), p.pagePixelDimensions.width.toString(), p.pagePixelDimensions.height.toString(),
            b.left.toString(), b.top.toString(), b.rightExclusive.toString(), b.bottomExclusive.toString(),
            p.derivationProfileId, p.derivationProfileVersion.toString(), c.name, crop.value)
        val md = MessageDigest.getInstance("SHA-256"); fields.forEach { value -> val bytes = value.toByteArray(); md.update(intBytes(bytes.size)); md.update(bytes) }
        return SourceRegionId(md.digest().joinToString("") { "%02x".format(it) })
    }
    private fun dark(p: ByteArray, i: Int) = (p[i].toInt() and 255) < profile.darkChannelThreshold || (p[i + 1].toInt() and 255) < profile.darkChannelThreshold || (p[i + 2].toInt() and 255) < profile.darkChannelThreshold
    private fun overlap(a0: Int, a1: Int, b0: Int, b1: Int) = maxOf(0, minOf(a1, b1) - maxOf(a0, b0))
    private fun contains(a: PixelCropBounds, b: PixelCropBounds) = a.left <= b.left && a.top <= b.top && a.rightExclusive >= b.rightExclusive && a.bottomExclusive >= b.bottomExclusive
    private fun intBytes(v: Int) = byteArrayOf((v ushr 24).toByte(), (v ushr 16).toByte(), (v ushr 8).toByte(), v.toByte())
}
