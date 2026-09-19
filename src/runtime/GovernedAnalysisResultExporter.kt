package parker.core.runtime

import parker.core.interfaces.AnalysisEvidenceReference
import parker.core.interfaces.AnalysisRequestId
import parker.core.interfaces.GovernedAnalysisResult
import parker.core.interfaces.GovernedAnalysisResultStorage
import parker.core.interfaces.StructuredAnalysisEvidenceGap

enum class GovernedAnalysisExportFormat { MARKDOWN, JSON }

data class GovernedAnalysisExport(
    val contentType: String,
    val fileName: String,
    val body: String,
)

/** Deterministic projection of an immutable governed result; never invokes analysis or evidence retrieval. */
class GovernedAnalysisResultExporter(
    private val storage: GovernedAnalysisResultStorage,
) {
    suspend fun export(requestId: AnalysisRequestId, format: GovernedAnalysisExportFormat): GovernedAnalysisExport? {
        val result = storage.findByAnalysisRequestId(requestId) ?: return null
        return when (format) {
            GovernedAnalysisExportFormat.MARKDOWN -> GovernedAnalysisExport(
                "text/markdown; charset=utf-8", "parker-analysis-${requestId.value}.md", markdown(result),
            )
            GovernedAnalysisExportFormat.JSON -> GovernedAnalysisExport(
                "application/json; charset=utf-8", "parker-analysis-${requestId.value}.json", json(result),
            )
        }
    }

    private fun markdown(result: GovernedAnalysisResult): String = buildString {
        appendLine("# Parker Case Analysis Export")
        appendLine()
        appendLine("## Analysis Metadata")
        appendLine()
        appendLine("- Schema version: ${result.schemaVersion}")
        appendLine("- Analysis request ID: `${result.analysisRequestId.value}`")
        appendLine("- Governed result ID: `${result.resultId.value}`")
        appendLine("- Case ID: `${result.caseId.value}`")
        result.caseName?.let { appendLine("- Case name: ${escapeMarkdown(it)}") }
        appendLine("- Question/task: ${escapeMarkdown(result.question)}")
        appendLine("- Analysis type: `${result.analysisType.name}`")
        appendLine("- Generated at: `${result.generatedAt}`")
        result.providerIdentity?.let { appendLine("- Provider: `${escapeCode(it)}`") }
        result.modelIdentity?.let { appendLine("- Model: `${escapeCode(it)}`") }
        result.hermesSessionId?.let { appendLine("- Hermes session: `${escapeCode(it)}`") }
        appendLine()
        appendLine("## Answer / Analysis")
        appendLine()
        appendLine(result.analysisText)
        appendLine()
        appendLine("### Structured Answer")
        appendLine()
        appendLine(result.structuredResult.answer)
        appendLine()
        appendFindings("## Supporting Evidence", result.structuredResult.findings.filter { it.supportReferences.isNotEmpty() }.map { it.text to it.supportReferences }, "SUPPORTED", result)
        appendFindings("## Inferences", result.structuredResult.findings.filter { it.supportReferences.isEmpty() }.map { it.text to it.supportReferences }, "INFERENCE", result)
        appendFindings("## Contradictions / Conflicts", result.structuredResult.contraryEvidence.map { it.text to it.references }, "CONFLICT", result)
        appendGaps(result.structuredResult.evidenceGaps)
        appendFindings("## Review Required", result.structuredResult.uncertainties.map { it.text to it.references }, "REVIEW_REQUIRED", result)
        appendLine("## Chronology")
        appendLine()
        appendLine("NOT REPRESENTED IN DURABLE ANALYSIS RESULT")
        appendLine()
        appendLine("## Financial / Quantitative Findings")
        appendLine()
        appendLine("NOT REPRESENTED IN DURABLE ANALYSIS RESULT")
        appendLine()
        appendLine("## Source Index")
        appendLine()
        result.evidenceScope.forEachIndexed { index, source ->
            append("${index + 1}. artifact=`${source.evidenceArtifactId.value}`")
            source.associationId?.let { append(" association=`${it.value}`") }
            append(" derivative=`${source.derivativeGenerationId.value}`")
            if (source.occurrenceIds.isNotEmpty()) append(" occurrences=${source.occurrenceIds.joinToString(",") { "`${it.value}`" }}")
            source.sourceLabel?.let { append(" source=${escapeMarkdown(it)}") }
            source.sourceSha256?.let { append(" sha256=`$it`") }
            appendLine()
        }
        if (result.warnings.isNotEmpty()) {
            appendLine()
            appendLine("## Warnings")
            appendLine()
            result.warnings.forEach { appendLine("- ${escapeMarkdown(it)}") }
        }
    }

    private fun StringBuilder.appendFindings(
        heading: String,
        findings: List<Pair<String, List<AnalysisEvidenceReference>>>,
        status: String,
        result: GovernedAnalysisResult,
    ) {
        appendLine(heading); appendLine()
        if (findings.isEmpty()) { appendLine("NOT FOUND IN CURRENT EVIDENCE SCOPE"); appendLine(); return }
        findings.forEachIndexed { index, (text, references) ->
            appendLine("${index + 1}. **[$status]** ${escapeMarkdown(text)}")
            references.forEach { appendLine("   - ${markdownReference(it, result)}") }
        }
        appendLine()
    }

    private fun StringBuilder.appendGaps(gaps: List<StructuredAnalysisEvidenceGap>) {
        appendLine("## Gaps / Not Found"); appendLine()
        if (gaps.isEmpty()) { appendLine("NOT FOUND IN CURRENT EVIDENCE SCOPE"); appendLine(); return }
        gaps.forEach { gap ->
            appendLine("- **[NOT_FOUND]** ${escapeMarkdown(gap.text)}")
            if (gap.relatedEvidenceArtifactIds.isNotEmpty()) appendLine("  - Related artifacts: ${gap.relatedEvidenceArtifactIds.joinToString(", ") { "`${it.value}`" }}")
        }
        appendLine()
    }

    private fun markdownReference(reference: AnalysisEvidenceReference, result: GovernedAnalysisResult): String = buildString {
        val scope = result.evidenceScope.firstOrNull { it.evidenceArtifactId == reference.evidenceArtifactId }
        append("[EVIDENCE: artifact=${reference.evidenceArtifactId.value}; derivative=${reference.derivativeGenerationId.value}")
        scope?.associationId?.let { append("; association=${it.value}") }
        scope?.occurrenceIds?.takeIf { it.isNotEmpty() }?.let { append("; occurrence=${it.joinToString(",") { occurrence -> occurrence.value }}") }
        append("; location=${location(reference)}; precision=${reference.precision.name}; authority=${reference.authority.name}")
        append("]")
        reference.originalFilename?.let { append(" ${escapeMarkdown(it)}") }
    }

    private fun location(reference: AnalysisEvidenceReference): String = buildString {
        reference.pageNumber?.let { append("page:$it") }
        reference.regionId?.let { if (isNotEmpty()) append(",") ; append("region:$it") }
        reference.sectionHeading?.let { if (isNotEmpty()) append(",") ; append("section:$it") }
        if (isEmpty()) append("document")
    }

    private fun json(result: GovernedAnalysisResult): String = JsonWriter().apply {
        obj {
            field("schemaVersion", 1)
            field("analysisRequestId", result.analysisRequestId.value)
            field("governedAnalysisResultId", result.resultId.value)
            fieldObject("case") { field("caseId", result.caseId.value); fieldNullable("caseName", result.caseName) }
            fieldObject("request") {
                field("question", result.question); field("analysisType", result.analysisType.name); field("generatedAt", result.generatedAt.toString())
            }
            fieldObject("provider") {
                fieldNullable("providerIdentity", result.providerIdentity); fieldNullable("modelIdentity", result.modelIdentity); fieldNullable("hermesSessionId", result.hermesSessionId); field("profile", result.profile)
            }
            fieldObject("analysis") {
                field("narrative", result.analysisText); field("answer", result.structuredResult.answer); field("conclusion", result.structuredResult.conclusion)
            }
            fieldArray("findings") {
                result.structuredResult.findings.forEachIndexed { index, finding ->
                    obj { field("findingId", "finding-${index + 1}"); field("statement", finding.text); field("status", if (finding.supportReferences.isEmpty()) "INFERENCE" else "SUPPORTED"); fieldReferences("evidence", finding.supportReferences, result) }
                }
            }
            fieldArray("conflicts") {
                result.structuredResult.contraryEvidence.forEachIndexed { index, conflict ->
                    obj { field("findingId", "conflict-${index + 1}"); field("statement", conflict.text); field("status", "CONFLICT"); fieldReferences("evidence", conflict.references, result) }
                }
            }
            fieldArray("gaps") {
                result.structuredResult.evidenceGaps.forEachIndexed { index, gap ->
                    obj { field("findingId", "gap-${index + 1}"); field("statement", gap.text); field("status", "NOT_FOUND"); fieldArray("relatedEvidenceArtifactIds") { gap.relatedEvidenceArtifactIds.forEach { value(it.value) } } }
                }
            }
            fieldArray("reviewRequired") {
                result.structuredResult.uncertainties.forEachIndexed { index, uncertainty ->
                    obj { field("findingId", "review-${index + 1}"); field("statement", uncertainty.text); field("status", "REVIEW_REQUIRED"); fieldReferences("evidence", uncertainty.references, result) }
                }
            }
            fieldArray("evidenceScope") { result.evidenceScope.forEach { scopeJson(it) } }
            fieldArray("sourceIndex") { result.evidenceScope.forEach { scopeJson(it) } }
            fieldArray("warnings") { result.warnings.forEach(::value) }
            field("chronology", null)
            field("financialQuantitativeFindings", null)
        }.toString()
    }.toString()

    private fun JsonWriter.scopeJson(scope: parker.core.interfaces.GovernedAnalysisEvidenceScopeEntry) = obj {
        field("evidenceArtifactId", scope.evidenceArtifactId.value); fieldNullable("associationId", scope.associationId?.value); fieldArray("occurrenceIds") { scope.occurrenceIds.forEach { value(it.value) } }; field("derivativeGenerationId", scope.derivativeGenerationId.value); fieldNullable("sourceSha256", scope.sourceSha256); fieldNullable("sourceLabel", scope.sourceLabel)
    }

    private fun JsonWriter.fieldReferences(name: String, refs: List<AnalysisEvidenceReference>, result: GovernedAnalysisResult) = fieldArray(name) {
        refs.forEach { reference -> obj {
            val scope = result.evidenceScope.firstOrNull { it.evidenceArtifactId == reference.evidenceArtifactId }
            field("evidenceArtifactId", reference.evidenceArtifactId.value); fieldNullable("associationId", scope?.associationId?.value); fieldArray("occurrenceIds") { scope?.occurrenceIds?.forEach { value(it.value) } }; field("derivativeGenerationId", reference.derivativeGenerationId.value); field("sourceSha256", reference.sourceSha256); fieldNullable("originalFilename", reference.originalFilename); field("precision", reference.precision.name); field("pageNumber", reference.pageNumber); fieldNullable("regionId", reference.regionId); field("authority", reference.authority.name); fieldNullable("sectionHeading", reference.sectionHeading); field("startOffset", reference.startOffset); field("endOffset", reference.endOffset); fieldNullable("quotedText", reference.quotedText)
        } }
    }

    private fun escapeCode(value: String): String = value.replace("`", "'" ).replace("\n", " ")
    private fun escapeMarkdown(value: String): String = value.replace("\\", "\\\\").replace("`", "\\`").replace("\n", "  \n")

    private class JsonWriter {
        private val out = StringBuilder()
        private val stack = ArrayDeque<Boolean>()
        fun obj(block: JsonWriter.() -> Unit): JsonWriter { comma(); out.append('{'); stack.addLast(true); block(); stack.removeLast(); out.append('}'); return this }
        fun field(name: String, value: Any?) { comma(); out.append(string(name)).append(':'); rawValue(value) }
        fun fieldNullable(name: String, value: String?) = field(name, value)
        fun fieldObject(name: String, block: JsonWriter.() -> Unit) { comma(); out.append(string(name)).append(':'); JsonWriter().apply { obj(block) }.also { out.append(it.out) } }
        fun fieldArray(name: String, block: JsonWriter.() -> Unit) { comma(); out.append(string(name)).append(':'); JsonWriter().apply { array(block) }.also { out.append(it.out) } }
        fun value(value: Any?) { comma(); rawValue(value) }
        private fun array(block: JsonWriter.() -> Unit) { out.append('['); stack.addLast(true); block(); stack.removeLast(); out.append(']') }
        private fun rawValue(value: Any?) { when (value) { null -> out.append("null"); is String -> out.append(string(value)); is Int -> out.append(value); is Long -> out.append(value); is Boolean -> out.append(value); else -> out.append(string(value.toString())) } }
        override fun toString(): String = out.toString()
        private fun comma() {
            if (stack.isEmpty()) return
            val first = stack.removeLast()
            if (!first) out.append(',')
            stack.addLast(false)
        }
        private fun string(value: String): String = buildString { append('"'); value.forEach { c -> when (c) { '"' -> append("\\\""); '\\' -> append("\\\\"); '\n' -> append("\\n"); '\r' -> append("\\r"); '\t' -> append("\\t"); else -> if (c.code < 0x20) append("\\u%04x".format(c.code)) else append(c) } }; append('"') }
    }
}
