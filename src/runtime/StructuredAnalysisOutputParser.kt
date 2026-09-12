package parker.core.runtime

import parker.core.interfaces.*

open class StructuredAnalysisOutputException(message: String) : RuntimeException(message)
class InvalidAnalysisReferenceException(message: String) : StructuredAnalysisOutputException(message)

/** Strict, bounded parser/validator for the Analysis Agent's RA-5 JSON envelope. */
class StructuredAnalysisOutputParser(
    private val maxBytes: Int = 2_000_000,
) {
    fun parse(raw: String, governedPackage: AnalysisRetrievalPackage): StructuredAnalysisResult {
        if (raw.toByteArray(Charsets.UTF_8).size > maxBytes) throw StructuredAnalysisOutputException("structured analysis output exceeded the bounded limit")
        try {
            val root = Json(raw).parse()
            val obj = root as? JObj ?: throw StructuredAnalysisOutputException("structured analysis output must be an object")
            val allowed = setOf("answer", "findings", "contraryEvidence", "uncertainties", "evidenceGaps", "conclusion")
            if (obj.values.keys != allowed) throw StructuredAnalysisOutputException("structured analysis envelope has unexpected or missing fields")
            val answer = obj.string("answer")
            val conclusion = obj.string("conclusion")
            val findings = obj.array("findings").map { parseFinding(it, governedPackage) }
            val contrary = obj.array("contraryEvidence").map { parseContrary(it, governedPackage) }
            val uncertainty = obj.array("uncertainties").map { parseUncertainty(it, governedPackage) }
            val gaps = obj.array("evidenceGaps").map { parseGap(it, governedPackage) }
            return StructuredAnalysisResult(answer, findings, contrary, uncertainty, gaps, conclusion)
        } catch (e: InvalidAnalysisReferenceException) {
            throw e
        } catch (e: StructuredAnalysisOutputException) {
            throw e
        } catch (_: IllegalArgumentException) {
            throw StructuredAnalysisOutputException("structured analysis output is malformed or violates bounds")
        }
    }

    private fun parseFinding(value: JValue, packageValue: AnalysisRetrievalPackage): StructuredAnalysisFinding {
        val obj = value.obj()
        requireKeys(obj, setOf("text", "supportReferences"))
        return StructuredAnalysisFinding(obj.string("text"), obj.array("supportReferences").map { reference(it, packageValue) })
    }

    private fun parseContrary(value: JValue, packageValue: AnalysisRetrievalPackage): StructuredAnalysisContraryEvidence {
        val obj = value.obj(); requireKeys(obj, setOf("text", "references"))
        return StructuredAnalysisContraryEvidence(obj.string("text"), obj.array("references").map { reference(it, packageValue) })
    }

    private fun parseUncertainty(value: JValue, packageValue: AnalysisRetrievalPackage): StructuredAnalysisUncertainty {
        val obj = value.obj(); requireKeys(obj, setOf("text", "references"))
        return StructuredAnalysisUncertainty(obj.string("text"), obj.array("references").map { reference(it, packageValue) })
    }

    private fun parseGap(value: JValue, packageValue: AnalysisRetrievalPackage): StructuredAnalysisEvidenceGap {
        val obj = value.obj()
        requireKeys(obj, setOf("text", "relatedEvidenceArtifactIds"))
        val ids = obj.array("relatedEvidenceArtifactIds").map { EvidenceArtifactId(it.stringValue()) }
        val known = packageValue.evidence.map { it.evidenceArtifactId }.toSet()
        if (!known.containsAll(ids)) throw InvalidAnalysisReferenceException("evidence gap names an evidence artifact outside the governed package")
        return StructuredAnalysisEvidenceGap(obj.string("text"), ids)
    }

    private fun reference(value: JValue, packageValue: AnalysisRetrievalPackage): AnalysisEvidenceReference {
        val obj = value.obj()
        requireKeys(obj, setOf("evidenceArtifactId", "derivativeGenerationId", "precision", "authority", "correctionId", "correctionScope", "pageNumber", "regionId"))
        val evidenceId = EvidenceArtifactId(obj.string("evidenceArtifactId"))
        val item = packageValue.evidence.singleOrNull { it.evidenceArtifactId == evidenceId }
            ?: throw InvalidAnalysisReferenceException("analysis reference names evidence outside the governed package")
        val generation = DerivativeGenerationId(obj.string("derivativeGenerationId"))
        val content = item.governedContent ?: throw InvalidAnalysisReferenceException("analysis reference has no governed derivative")
        if (content.derivativeGenerationId != generation) throw InvalidAnalysisReferenceException("analysis reference pairs the wrong derivative with evidence")
        val precision = try { AnalysisReferencePrecision.valueOf(obj.string("precision")) }
            catch (_: Exception) { throw InvalidAnalysisReferenceException("unknown analysis reference precision") }
        val authority = try { AnalysisReferenceAuthority.valueOf(obj.string("authority")) }
            catch (_: Exception) { throw InvalidAnalysisReferenceException("unknown analysis reference authority") }
        val page = obj.intOrNull("pageNumber")
        val region = obj.stringOrNull("regionId")
        val correctionId = obj.stringOrNull("correctionId")?.let {
            try { HermesPreIngestionCorrectionId(it) } catch (_: Exception) { throw InvalidAnalysisReferenceException("invalid correction reference") }
        }
        val scope = obj.stringOrNull("correctionScope")
        if (authority == AnalysisReferenceAuthority.OWNER_AUTHORIZED_CORRECTION) {
            val lineage = item.manifest.correctionLineage
                ?: throw InvalidAnalysisReferenceException("owner correction is not present in the governed package")
            if (correctionId != lineage.correction.representationId || scope != "ISSUE") throw InvalidAnalysisReferenceException("owner correction reference does not match governed correction scope")
        } else if (correctionId != null || scope != null) throw InvalidAnalysisReferenceException("machine reference cannot carry correction provenance")
        val pages = supportedPages(item)
        val regions = supportedRegions(item)
        when (precision) {
            AnalysisReferencePrecision.DOCUMENT -> if (page != null || region != null) invalid("document reference carries page or region precision")
            AnalysisReferencePrecision.PAGE -> if (page == null || page !in pages || region != null) invalid("page precision exceeds governed page provenance")
            AnalysisReferencePrecision.REGION -> if (page == null || page !in pages || region == null || supportedRegionPages(item)[region] != page) invalid("region precision exceeds governed region provenance")
        }
        if (precision == AnalysisReferencePrecision.REGION && region !in regions) invalid("unknown governed region")
        return AnalysisEvidenceReference(evidenceId, generation, item.manifest.sha256, item.manifest.originalFileName, precision, page, region, authority, correctionId, scope)
    }

    private fun supportedPages(item: AnalysisRetrievedEvidence): Set<Int> = when (val payload = item.governedContent?.payload) {
        is TierADerivativePayload.Pdf -> if (payload.value.pageTextAssociationAvailable && payload.value.pageCount != null) (1..payload.value.pageCount).toSet() else emptySet()
        is TierADerivativePayload.Ocr -> payload.value.segments.mapNotNull { it.pageNumber }.toSet()
        is TierADerivativePayload.RegionTranscription -> supportedRegionPages(item).values.toSet()
        else -> emptySet()
    }

    private fun supportedRegionPages(item: AnalysisRetrievedEvidence): Map<String, Int> = when (val payload = item.governedContent?.payload) {
        is TierADerivativePayload.RegionTranscription -> payload.value.regionBindings.zip(payload.value.pageBindings).mapNotNull { (region, page) -> page.toIntOrNull()?.let { region to it } }.toMap()
        else -> emptyMap()
    }

    private fun supportedRegions(item: AnalysisRetrievedEvidence): Set<String> = supportedRegionPages(item).keys
    private fun invalid(message: String): Nothing = throw InvalidAnalysisReferenceException(message)
    private fun requireKeys(obj: JObj, keys: Set<String>) { if (obj.values.keys != keys) throw StructuredAnalysisOutputException("structured analysis item has unexpected or missing fields") }

    private sealed interface JValue { fun stringValue(): String = throw IllegalArgumentException("expected string")
        fun obj(): JObj = this as? JObj ?: throw IllegalArgumentException("expected object") }
    private data class JObj(val values: Map<String, JValue>) : JValue {
        fun string(key: String) = values[key]?.stringValue() ?: throw StructuredAnalysisOutputException("missing string field '$key'")
        fun stringOrNull(key: String) = when (val value = values[key]) { null, JNull -> null; else -> value.stringValue() }
        fun intOrNull(key: String) = when (val value = values[key]) { null, JNull -> null; is JNum -> value.raw.toIntOrNull()?.takeIf { value.raw == it.toString() }; else -> throw StructuredAnalysisOutputException("field '$key' must be an integer or null") }
        fun array(key: String) = values[key] as? JArr ?: throw StructuredAnalysisOutputException("field '$key' must be an array")
    }
    private data class JArr(val values: List<JValue>) : JValue { fun <T> map(transform: (JValue) -> T): List<T> = values.map(transform) }
    private data class JStr(val value: String) : JValue { override fun stringValue() = value }
    private data class JNum(val raw: String) : JValue
    private data object JNull : JValue
    private data class JBool(val value: Boolean) : JValue

    private class Json(private val text: String) {
        private var p = 0
        fun parse(): JValue { val v = value(); ws(); if (p != text.length) throw IllegalArgumentException("trailing JSON"); return v }
        private fun value(): JValue { ws(); if (p >= text.length) throw IllegalArgumentException("eof"); return when (text[p]) { '{' -> obj(); '[' -> arr(); '"' -> JStr(str()); 'n' -> literal("null", JNull); 't' -> literal("true", JBool(true)); 'f' -> literal("false", JBool(false)); else -> num() } }
        private fun obj(): JObj { p++; ws(); val m = linkedMapOf<String, JValue>(); if (peek('}')) { p++; return JObj(m) }; while (true) { val k = str(); ws(); expect(':'); val v = value(); if (m.put(k, v) != null) throw IllegalArgumentException("duplicate key"); ws(); if (peek('}')) { p++; return JObj(m) }; expect(',') } }
        private fun arr(): JArr { p++; ws(); val a = mutableListOf<JValue>(); if (peek(']')) { p++; return JArr(a) }; while (true) { a += value(); ws(); if (peek(']')) { p++; return JArr(a) }; expect(',') } }
        private fun str(): String { expect('"'); val b = StringBuilder(); while (p < text.length) { val c = text[p++]; when (c) { '"' -> return b.toString(); '\\' -> { if (p >= text.length) throw IllegalArgumentException("escape"); when (val e = text[p++]) { '"' -> b.append('"'); '\\' -> b.append('\\'); '/' -> b.append('/'); 'b' -> b.append('\b'); 'f' -> b.append('\u000c'); 'n' -> b.append('\n'); 'r' -> b.append('\r'); 't' -> b.append('\t'); 'u' -> { val h = text.substring(p, p + 4); p += 4; b.append(h.toInt(16).toChar()) }; else -> throw IllegalArgumentException("escape") } }; else -> if (c.code < 0x20) throw IllegalArgumentException("control") else b.append(c) } }; throw IllegalArgumentException("string") }
        private fun num(): JNum { val start = p; while (p < text.length && text[p] !in " \t\r\n,]}") p++; val raw = text.substring(start, p); if (!raw.matches(Regex("-?(0|[1-9][0-9]*)(\\.[0-9]+)?([eE][+-]?[0-9]+)?"))) throw IllegalArgumentException("number"); return JNum(raw) }
        private fun <T : JValue> literal(s: String, v: T): T { if (!text.startsWith(s, p)) throw IllegalArgumentException("literal"); p += s.length; return v }
        private fun expect(c: Char) { ws(); if (p >= text.length || text[p++] != c) throw IllegalArgumentException("expected") }
        private fun peek(c: Char) = p < text.length && text[p] == c
        private fun ws() { while (p < text.length && text[p].isWhitespace()) p++ }
    }
}
