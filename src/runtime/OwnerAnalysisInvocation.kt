package parker.core.runtime

import java.io.InputStream
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import parker.core.interfaces.AnalysisRequest
import parker.core.interfaces.AnalysisRequestId
import parker.core.interfaces.AnalysisType
import parker.core.interfaces.DerivativeGenerationId
import parker.core.interfaces.EvidenceArtifactId

data class OwnerAnalysisInvocationRequest(
    val question: String,
    val evidenceArtifactIds: List<EvidenceArtifactId>,
    val analysisType: AnalysisType = AnalysisType.ISSUE_ANALYSIS,
) {
    init {
        require(question.isNotBlank()) { "analysis question must not be blank" }
        require(question.length <= 8_000) { "analysis question exceeds the maximum length" }
        require(question.none { it == '\u0000' }) { "analysis question contains a control character" }
        require(evidenceArtifactIds.isNotEmpty()) { "analysis scope must contain at least one evidence artifact" }
        require(evidenceArtifactIds.size <= 100) { "analysis scope may contain at most 100 evidence artifacts" }
        require(evidenceArtifactIds.distinct().size == evidenceArtifactIds.size) { "analysis scope contains duplicate evidence artifacts" }
    }
}

data class HermesAnalysisInvocation(
    val analysisText: String,
    val sessionId: String?,
)

interface HermesAnalysisInvoker {
    suspend fun invoke(
        question: String,
        analysisType: AnalysisType,
        governedPackage: AnalysisRetrievalPackage,
    ): HermesAnalysisInvocation
}

sealed interface OwnerAnalysisInvocationOutcome {
    data class Completed(
        val analysisRequestId: AnalysisRequestId,
        val analysisType: AnalysisType,
        val question: String,
        val selectedEvidenceArtifactIds: List<EvidenceArtifactId>,
        val resolvedDerivativeGenerationIds: Map<EvidenceArtifactId, DerivativeGenerationId>,
        val profile: String,
        val analysisText: String,
        val hermesSessionId: String?,
        val governedPackage: AnalysisRetrievalPackage,
        val structuredResult: parker.core.interfaces.StructuredAnalysisResult,
    ) : OwnerAnalysisInvocationOutcome

    data class DerivativeAmbiguous(
        val evidenceArtifactId: EvidenceArtifactId,
        val candidates: List<DerivativeCandidateSummary>,
        val reason: String,
    ) : OwnerAnalysisInvocationOutcome

    data class NoUsableDerivative(val evidenceArtifactId: EvidenceArtifactId, val reason: String) : OwnerAnalysisInvocationOutcome
    data class GovernedRetrievalFailed(val evidenceArtifactId: EvidenceArtifactId?, val reason: String) : OwnerAnalysisInvocationOutcome
    data class ReasoningFailed(val analysisRequestId: AnalysisRequestId, val reason: String, val governedPackage: AnalysisRetrievalPackage) : OwnerAnalysisInvocationOutcome
    data class ReasoningTimeout(val analysisRequestId: AnalysisRequestId, val governedPackage: AnalysisRetrievalPackage) : OwnerAnalysisInvocationOutcome
    data class StructuredOutputInvalid(val analysisRequestId: AnalysisRequestId, val reason: String, val governedPackage: AnalysisRetrievalPackage) : OwnerAnalysisInvocationOutcome
    data class InvalidAnalysisReference(val analysisRequestId: AnalysisRequestId, val reason: String, val governedPackage: AnalysisRetrievalPackage) : OwnerAnalysisInvocationOutcome
}

/** Owner-side logical analysis orchestration. It never retrieves content directly. */
class OwnerAnalysisInvocationCoordinator(
    private val resolvePreferredDerivative: suspend (EvidenceArtifactId) -> PreferredDerivativeResolution,
    private val submitGovernedAnalysis: suspend (AnalysisRequest) -> AnalysisRequestResult,
    private val hermesInvoker: HermesAnalysisInvoker,
    private val profile: String = ANALYSIS_PROFILE,
) {
    suspend fun invoke(request: OwnerAnalysisInvocationRequest, priorContext: String? = null): OwnerAnalysisInvocationOutcome {
        val mapping = linkedMapOf<EvidenceArtifactId, DerivativeGenerationId>()
        for (evidenceId in request.evidenceArtifactIds) {
            when (val resolution = resolvePreferredDerivative(evidenceId)) {
                is PreferredDerivativeResolution.Preferred -> mapping[evidenceId] = resolution.derivative.derivativeGenerationId
                is PreferredDerivativeResolution.Ambiguous -> return OwnerAnalysisInvocationOutcome.DerivativeAmbiguous(evidenceId, resolution.candidates, resolution.reason)
                is PreferredDerivativeResolution.NoUsableDerivative -> return OwnerAnalysisInvocationOutcome.NoUsableDerivative(evidenceId, resolution.reason)
            }
        }
        val analysisRequest = AnalysisRequest(
            AnalysisRequestId.new(), request.question, request.analysisType,
            parker.core.interfaces.AnalysisEvidenceScope(request.evidenceArtifactIds, mapping.mapKeys { it.key.value }),
        )
        val governedPackage = when (val result = submitGovernedAnalysis(analysisRequest)) {
            is AnalysisRequestResult.Accepted -> result.retrievalPackage
            is AnalysisRequestResult.ScopeRejected -> return OwnerAnalysisInvocationOutcome.GovernedRetrievalFailed(result.evidenceArtifactId, result.reason)
            is AnalysisRequestResult.Denied -> return OwnerAnalysisInvocationOutcome.GovernedRetrievalFailed(null, result.reason)
        }
        val reasoning = try {
            hermesInvoker.invoke(
                if (priorContext.isNullOrBlank()) request.question else
                    "The following is bounded conversational context only, not evidence and not a source.\n$priorContext\n\nCurrent follow-up question: ${request.question}",
                request.analysisType,
                governedPackage,
            )
        } catch (e: HermesAnalysisTimeoutException) {
            return OwnerAnalysisInvocationOutcome.ReasoningTimeout(analysisRequest.requestId, governedPackage)
        } catch (e: Exception) {
            return OwnerAnalysisInvocationOutcome.ReasoningFailed(analysisRequest.requestId, e.message ?: "Hermes reasoning failed", governedPackage)
        }
        val structured = try {
            StructuredAnalysisOutputParser().parse(reasoning.analysisText, governedPackage)
        } catch (e: InvalidAnalysisReferenceException) {
            return OwnerAnalysisInvocationOutcome.InvalidAnalysisReference(analysisRequest.requestId, e.message ?: "invalid analysis reference", governedPackage)
        } catch (e: StructuredAnalysisOutputException) {
            return OwnerAnalysisInvocationOutcome.StructuredOutputInvalid(analysisRequest.requestId, e.message ?: "invalid structured analysis output", governedPackage)
        }
        return OwnerAnalysisInvocationOutcome.Completed(
            analysisRequest.requestId, request.analysisType, request.question, request.evidenceArtifactIds,
            mapping, profile, reasoning.analysisText, reasoning.sessionId, governedPackage, structured,
        )
    }

    companion object { const val ANALYSIS_PROFILE = "parker-analysis-agent" }
}

class HermesAnalysisTimeoutException(message: String) : RuntimeException(message)

/** Synchronous, shell-free invocation of Hermes' existing profile-scoped CLI. */
class ProcessBuilderHermesAnalysisInvoker(
    private val executable: String = "hermes",
    private val timeoutMillis: Long = 120_000,
    private val outputLimitBytes: Long = 2_000_000,
) : HermesAnalysisInvoker {
    override suspend fun invoke(question: String, analysisType: AnalysisType, governedPackage: AnalysisRetrievalPackage): HermesAnalysisInvocation {
        val prompt = StructuredAnalysisPrompt.forHermes(question, analysisType, GovernedAnalysisPackagePrompt.json(governedPackage))
        val process = ProcessBuilder(
            executable, "--profile", OwnerAnalysisInvocationCoordinator.ANALYSIS_PROFILE,
            "chat", "-q", prompt, "-Q", "--max-turns", "1",
        ).redirectErrorStream(false).start()
        val pool = Executors.newFixedThreadPool(2)
        try {
            val out = pool.submit<String> { readBounded(process.inputStream) }
            val err = pool.submit<String> { readBounded(process.errorStream) }
            if (!process.waitFor(timeoutMillis, TimeUnit.MILLISECONDS)) {
                process.destroyForcibly()
                process.waitFor(2, TimeUnit.SECONDS)
                throw HermesAnalysisTimeoutException("Hermes analysis timed out")
            }
            val stdout = out.get(2, TimeUnit.SECONDS)
            err.get(2, TimeUnit.SECONDS)
            if (process.exitValue() != 0) throw IllegalStateException("Hermes analysis exited unsuccessfully")
            val session = Regex("(?m)^session_id:\\s*(\\S+)\\s*$").find(stdout)?.groupValues?.get(1)
            val text = stdout.replace(Regex("(?m)^session_id:\\s*\\S+\\s*$"), "").trim()
            if (text.isBlank()) throw IllegalStateException("Hermes analysis returned no reasoning text")
            return HermesAnalysisInvocation(text, session)
        } finally {
            pool.shutdownNow()
            if (process.isAlive) process.destroyForcibly()
        }
    }

    private fun readBounded(input: InputStream): String {
        val bytes = input.readNBytes((outputLimitBytes + 1).coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
        if (bytes.size > outputLimitBytes) throw IllegalStateException("Hermes analysis output exceeded the bounded limit")
        return bytes.toString(Charsets.UTF_8)
    }
}

/** Parker-to-Hermes transport. The remote authorized key forces the wrapper; no command is sent. */
class HermesSshAnalysisInvoker(
    private val keyPath: String,
    private val knownHostsPath: String,
    private val host: String = "192.168.178.45",
    private val user: String = "steve",
    private val timeoutMillis: Long = 125_000,
    private val outputLimitBytes: Long = 2_000_000,
) : HermesAnalysisInvoker {
    override suspend fun invoke(question: String, analysisType: AnalysisType, governedPackage: AnalysisRetrievalPackage): HermesAnalysisInvocation {
        val request = "{\"question\":${jsonQuote(StructuredAnalysisPrompt.task(question, analysisType))},\"analysisType\":${jsonQuote(analysisType.name)},\"governedPackage\":${GovernedAnalysisPackagePrompt.json(governedPackage)}}"
        val process = ProcessBuilder(
            "ssh", "-T", "-i", keyPath, "-o", "BatchMode=yes", "-o", "IdentitiesOnly=yes",
            "-o", "StrictHostKeyChecking=yes", "-o", "UserKnownHostsFile=$knownHostsPath", "$user@$host",
        ).redirectErrorStream(false).start()
        val pool = Executors.newFixedThreadPool(2)
        try {
            process.outputStream.use { it.write(request.toByteArray(Charsets.UTF_8)); it.flush() }
            val out = pool.submit<String> { readBounded(process.inputStream) }
            val err = pool.submit<String> { readBounded(process.errorStream) }
            if (!process.waitFor(timeoutMillis, TimeUnit.MILLISECONDS)) {
                process.destroyForcibly(); process.waitFor(2, TimeUnit.SECONDS)
                throw HermesAnalysisTimeoutException("Hermes SSH analysis timed out")
            }
            val stdout = out.get(2, TimeUnit.SECONDS)
            err.get(2, TimeUnit.SECONDS)
            if (process.exitValue() != 0) throw IllegalStateException("Hermes SSH transport failed")
            val status = jsonStringField(stdout, "status")
            if (status != "COMPLETED") throw IllegalStateException(jsonStringField(stdout, "detail") ?: "Hermes reasoning failed")
            val analysisEncoded = jsonStringField(stdout, "analysis")
                ?: throw IllegalStateException("Hermes response omitted analysis")
            val session = jsonStringField(stdout, "sessionId")
            return HermesAnalysisInvocation(analysisEncoded, session)
        } finally {
            pool.shutdownNow()
            if (process.isAlive) process.destroyForcibly()
        }
    }

    private fun readBounded(input: InputStream): String {
        val bytes = input.readNBytes((outputLimitBytes + 1).coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
        if (bytes.size > outputLimitBytes) throw IllegalStateException("Hermes SSH output exceeded the bounded limit")
        return bytes.toString(Charsets.UTF_8)
    }
}

/** Bounded linear extraction for the fixed wrapper response; avoids regex backtracking on hostile output. */
private fun jsonStringField(json: String, field: String): String? {
    var from = 0
    val needle = "\"$field\""
    while (true) {
        val key = json.indexOf(needle, from)
        if (key < 0) return null
        var p = key + needle.length
        while (p < json.length && json[p].isWhitespace()) p++
        if (p >= json.length || json[p++] != ':') { from = key + needle.length; continue }
        while (p < json.length && json[p].isWhitespace()) p++
        if (p >= json.length || json[p] == 'n') return null
        if (json[p++] != '"') { from = key + needle.length; continue }
        val value = StringBuilder()
        while (p < json.length) {
            when (val c = json[p++]) {
                '"' -> return value.toString()
                '\\' -> {
                    if (p >= json.length) return null
                    when (val escaped = json[p++]) {
                        '"' -> value.append('"'); '\\' -> value.append('\\'); '/' -> value.append('/')
                        'b' -> value.append('\b'); 'f' -> value.append('\u000c'); 'n' -> value.append('\n')
                        'r' -> value.append('\r'); 't' -> value.append('\t')
                        'u' -> if (p + 4 <= json.length) { value.append(json.substring(p, p + 4).toIntOrNull(16)?.toChar() ?: return null); p += 4 } else return null
                        else -> return null
                    }
                }
                else -> value.append(c)
            }
        }
        return null
    }
}

private fun jsonQuote(value: String): String = buildString {
    append('"'); value.forEach { c -> when (c) { '"' -> append("\\\""); '\\' -> append("\\\\"); '\n' -> append("\\n"); '\r' -> append("\\r"); '\t' -> append("\\t"); else -> if (c.code < 0x20) append("\\u%04x".format(c.code)) else append(c) } }; append('"')
}

private object StructuredAnalysisPrompt {
    fun task(question: String, analysisType: AnalysisType): String = """
        Return ONLY one valid JSON object with exactly these fields: answer (string), findings (array of {text:string, supportReferences:array}), contraryEvidence (array of {text:string, references:array}), uncertainties (array of {text:string, references:array}), evidenceGaps (array of {text:string, relatedEvidenceArtifactIds:array}), conclusion (string).
        Every reference object must contain exactly: evidenceArtifactId, derivativeGenerationId, precision (DOCUMENT, PAGE, or REGION), authority (MACHINE_DERIVED or OWNER_AUTHORIZED_CORRECTION), correctionId (string or null), correctionScope (string or null), pageNumber (integer or null), regionId (string or null). Use only IDs and locations present in the governed package. Do not invent references or facts; use evidenceGaps for unsupported claims. Analysis type: ${analysisType.name}. Question: $question
    """.trimIndent()
    fun forHermes(question: String, analysisType: AnalysisType, packageJson: String): String = "Analyse the following governed Parker evidence package and return the strict structured JSON envelope requested.\n\n${task(question, analysisType)}\n\nGoverned package:\n$packageJson"
}

private fun unescapeJsonString(value: String): String = buildString {
    var i = 0
    while (i < value.length) {
        if (value[i] != '\\') { append(value[i++]); continue }
        i++
        when (val c = value[i++]) {
            '"' -> append('"'); '\\' -> append('\\'); '/' -> append('/')
            'b' -> append('\b'); 'f' -> append('\u000c'); 'n' -> append('\n'); 'r' -> append('\r'); 't' -> append('\t')
            'u' -> { append(value.substring(i, i + 4).toInt(16).toChar()); i += 4 }
            else -> throw IllegalArgumentException("invalid JSON escape: $c")
        }
    }
}

private object GovernedAnalysisPackagePrompt {
    fun json(value: AnalysisRetrievalPackage): String = buildString {
        append("{\"requestId\":").quoted(value.requestId.value)
        append(",\"question\":").quoted(value.question)
        append(",\"analysisType\":").quoted(value.analysisType.name)
        append(",\"evidence\":[")
        value.evidence.forEachIndexed { index, item ->
            if (index > 0) append(',')
            append("{\"evidenceArtifactId\":").quoted(item.evidenceArtifactId.value)
            append(",\"manifest\":{\"evidenceArtifactId\":").quoted(item.manifest.evidenceArtifactId.value)
            append(",\"sha256\":").quoted(item.manifest.sha256)
            item.manifest.originalFileName?.let { append(",\"originalFileName\":").quoted(it) }
            item.manifest.correctionLineage?.let { lineage ->
                append(",\"correctionLineage\":{")
                append("\"correctionId\":").quoted(lineage.correction.representationId.value)
                append(",\"scope\":\"ISSUE\",\"issueIndex\":${lineage.correction.issueIndex}")
                append(",\"machineInterpretation\":").quoted(lineage.correction.machineInterpretation ?: "")
                append(",\"correctedInterpretation\":").quoted(lineage.correction.correctedInterpretation ?: "")
                append(",\"ownerExplanation\":").quoted(lineage.correction.ownerExplanation ?: "")
                append('}')
            }
            item.manifest.correctedContent?.let { correction ->
                append(",\"correctedContent\":{")
                append("\"authority\":").quoted(correction.authority)
                append(",\"scope\":").quoted(correction.scope)
                append(",\"correctionId\":").quoted(correction.correctionId.value)
                append(",\"issueIndex\":${correction.issueIndex}")
                append(",\"machineInterpretation\":").quoted(correction.machineInterpretation ?: "")
                append(",\"correctedInterpretation\":").quoted(correction.correctedInterpretation ?: "")
                append(",\"ownerExplanation\":").quoted(correction.ownerExplanation ?: "")
                append('}')
            }
            append('}')
            item.governedContent?.let { content ->
                append(",\"governedContent\":{\"derivativeGenerationId\":").quoted(content.derivativeGenerationId.value)
                append(",\"derivativeKind\":").quoted(content.record.derivativeKind)
                append(",\"content\":").quoted(readableContent(content.payload))
                append('}')
            }
            append('}')
        }
        append("]}")
    }
    private fun readableContent(payload: parker.core.interfaces.TierADerivativePayload): String = when (payload) {
        is parker.core.interfaces.TierADerivativePayload.Pdf -> payload.value.documentText
        is parker.core.interfaces.TierADerivativePayload.Ocr -> payload.value.recognisedText
        is parker.core.interfaces.TierADerivativePayload.Docx -> payload.value.paragraphs.joinToString("\n") { it.text }
        is parker.core.interfaces.TierADerivativePayload.Csv -> payload.value.rows.joinToString("\n") { it.joinToString(",") }
        is parker.core.interfaces.TierADerivativePayload.Eml -> payload.value.bodyAlternatives.joinToString("\n") { it.decodedText }
        is parker.core.interfaces.TierADerivativePayload.RegionTranscription -> payload.value.transcriptionBlocks.joinToString("\n")
    }
    private fun StringBuilder.quoted(value: String) { append('"'); value.forEach { c -> when (c) { '"' -> append("\\\""); '\\' -> append("\\\\"); '\n' -> append("\\n"); '\r' -> append("\\r"); '\t' -> append("\\t"); else -> if (c.code < 0x20) append("\\u%04x".format(c.code)) else append(c) } }; append('"') }
}
