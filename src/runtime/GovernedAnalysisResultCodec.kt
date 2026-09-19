package parker.core.runtime

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import java.time.Instant
import parker.core.interfaces.*

internal class UnsupportedGovernedAnalysisResultVersionException(val version: Int) : Exception("unsupported governed analysis result version: $version")

internal object GovernedAnalysisResultCodec {
    private const val MAGIC = 0x50474152 // PGAR
    private const val VERSION = 1
    private const val MAX_COLLECTION = 10_000
    private const val MAX_STRING_BYTES = 8 * 1024 * 1024

    fun encode(result: GovernedAnalysisResult): ByteArray = ByteArrayOutputStream().use { bytes ->
        DataOutputStream(bytes).use { out ->
            out.writeInt(MAGIC); out.writeInt(VERSION)
            out.writeString(result.resultId.value); out.writeString(result.analysisRequestId.value)
            out.writeString(result.caseId.value); out.writeNullableString(result.caseName)
            out.writeString(result.question); out.writeString(result.analysisType.name); out.writeString(result.generatedAt.toString())
            out.writeNullableString(result.providerIdentity); out.writeNullableString(result.modelIdentity); out.writeNullableString(result.hermesSessionId)
            out.writeString(result.profile); out.writeScope(result.evidenceScope); out.writeString(result.analysisText)
            out.writeStructured(result.structuredResult); out.writeStrings(result.warnings)
        }
        bytes.toByteArray()
    }

    fun decode(content: ByteArray): GovernedAnalysisResult = DataInputStream(ByteArrayInputStream(content)).use { input ->
        require(input.readInt() == MAGIC) { "invalid governed analysis result magic" }
        val version = input.readInt(); if (version !in 1..VERSION) throw UnsupportedGovernedAnalysisResultVersionException(version)
        val resultId = GovernedAnalysisResultId(input.readString())
        val requestId = AnalysisRequestId(input.readString())
        val caseId = CaseId(input.readString()); val caseName = input.readNullableString()
        val question = input.readString(); val type = AnalysisType.valueOf(input.readString()); val generatedAt = Instant.parse(input.readString())
        val provider = input.readNullableString(); val model = input.readNullableString(); val session = input.readNullableString()
        val profile = input.readString(); val scope = input.readScope(); val text = input.readString(); val structured = input.readStructured(); val warnings = input.readStrings()
        require(input.available() == 0) { "unexpected trailing bytes" }
        require(resultId == GovernedAnalysisResultId.forRequest(requestId)) { "result identity does not match analysis request" }
        GovernedAnalysisResult(1, resultId, requestId, caseId, caseName, question, type, generatedAt, provider, model, session, profile, scope, text, structured, warnings)
    }

    private fun DataOutputStream.writeScope(values: List<GovernedAnalysisEvidenceScopeEntry>) { writeCount(values.size); values.forEach { v ->
        writeString(v.evidenceArtifactId.value); writeNullableString(v.associationId?.value); writeStrings(v.occurrenceIds.map { it.value }); writeString(v.derivativeGenerationId.value); writeNullableString(v.sourceSha256); writeNullableString(v.sourceLabel)
    } }
    private fun DataInputStream.readScope(): List<GovernedAnalysisEvidenceScopeEntry> = List(readCount()) {
        GovernedAnalysisEvidenceScopeEntry(EvidenceArtifactId(readString()), readNullableString()?.let(::CaseEvidenceAssociationId), readStrings().map(::EvidenceOccurrenceId), DerivativeGenerationId(readString()), readNullableString(), readNullableString())
    }
    private fun DataOutputStream.writeStructured(v: StructuredAnalysisResult) {
        writeString(v.answer); writeFindings(v.findings.map { it.text to it.supportReferences }); writeFindings(v.contraryEvidence.map { it.text to it.references }); writeFindings(v.uncertainties.map { it.text to it.references }); writeCount(v.evidenceGaps.size); v.evidenceGaps.forEach { writeString(it.text); writeStrings(it.relatedEvidenceArtifactIds.map { id -> id.value }) }; writeString(v.conclusion)
    }
    private fun DataInputStream.readStructured(): StructuredAnalysisResult {
        fun refs(): List<Pair<String, List<AnalysisEvidenceReference>>> = List(readCount()) { readString() to readReferences() }
        val answer = readString(); val findings = refs().map { StructuredAnalysisFinding(it.first, it.second) }; val contrary = refs().map { StructuredAnalysisContraryEvidence(it.first, it.second) }; val uncertainties = refs().map { StructuredAnalysisUncertainty(it.first, it.second) }; val gaps = List(readCount()) { StructuredAnalysisEvidenceGap(readString(), readStrings().map(::EvidenceArtifactId)) }; val conclusion = readString()
        return StructuredAnalysisResult(answer, findings, contrary, uncertainties, gaps, conclusion)
    }
    private fun DataOutputStream.writeFindings(values: List<Pair<String, List<AnalysisEvidenceReference>>>) { writeCount(values.size); values.forEach { writeString(it.first); writeReferences(it.second) } }
    private fun DataOutputStream.writeReferences(values: List<AnalysisEvidenceReference>) { writeCount(values.size); values.forEach { v -> writeString(v.evidenceArtifactId.value); writeString(v.derivativeGenerationId.value); writeString(v.sourceSha256); writeNullableString(v.originalFilename); writeString(v.precision.name); writeNullableInt(v.pageNumber); writeNullableString(v.regionId); writeString(v.authority.name); writeNullableString(v.correctionId?.value); writeNullableString(v.correctionScope); writeNullableString(v.sectionHeading); writeNullableInt(v.startOffset); writeNullableInt(v.endOffset); writeNullableString(v.quotedText) } }
    private fun DataInputStream.readReferences(): List<AnalysisEvidenceReference> = List(readCount()) {
        AnalysisEvidenceReference(EvidenceArtifactId(readString()), DerivativeGenerationId(readString()), readString(), readNullableString(), AnalysisReferencePrecision.valueOf(readString()), readNullableInt(), readNullableString(), AnalysisReferenceAuthority.valueOf(readString()), readNullableString()?.let(::HermesPreIngestionCorrectionId), readNullableString(), readNullableString(), readNullableInt(), readNullableInt(), readNullableString())
    }
    private fun DataOutputStream.writeStrings(values: List<String>) { writeCount(values.size); values.forEach { writeString(it) } }
    private fun DataInputStream.readStrings(): List<String> = List(readCount()) { readString() }
    private fun DataOutputStream.writeCount(value: Int) { require(value in 0..MAX_COLLECTION); writeInt(value) }
    private fun DataInputStream.readCount(): Int = readInt().also { require(it in 0..MAX_COLLECTION) }
    private fun DataOutputStream.writeNullableString(value: String?) { writeBoolean(value != null); value?.let { writeString(it) } }
    private fun DataInputStream.readNullableString(): String? = if (readBoolean()) readString() else null
    private fun DataOutputStream.writeNullableInt(value: Int?) { writeBoolean(value != null); value?.let(::writeInt) }
    private fun DataInputStream.readNullableInt(): Int? = if (readBoolean()) readInt() else null
    private fun DataOutputStream.writeString(value: String) { val bytes = value.toByteArray(StandardCharsets.UTF_8); require(bytes.size <= MAX_STRING_BYTES); writeInt(bytes.size); write(bytes) }
    private fun DataInputStream.readString(): String { val size = readInt(); require(size in 0..MAX_STRING_BYTES); val bytes = ByteArray(size).also(::readFully); return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT).decode(java.nio.ByteBuffer.wrap(bytes)).toString() }
}
