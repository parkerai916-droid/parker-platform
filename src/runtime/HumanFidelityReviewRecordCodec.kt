package parker.core.runtime

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Instant
import parker.core.interfaces.*

internal class UnsupportedHumanFidelityReviewRepresentationVersionException(val version: Int) : Exception()
internal class UnsupportedHumanFidelityAuditRepresentationVersionException(val version: Int) : Exception()

internal object HumanFidelityReviewRecordCodec {
    private const val MAGIC = 0x50484652 // PHFR
    internal const val VERSION = 1
    internal const val MAX_RECORD_BYTES = 16 * 1024 * 1024
    private const val MAX_STRING_BYTES = 512 * 1024
    private const val MAX_ITEMS = 1_000

    fun encode(record: HumanFidelityReviewRecord): ByteArray = envelope(MAGIC, encodePayload(record))

    fun payloadSha256(record: HumanFidelityReviewRecord): OcrSha256Digest =
        OcrSha256Digest(fidelityStorageSha256Hex(encodePayload(record)))

    fun decode(bytes: ByteArray): HumanFidelityReviewRecord = decodeEnvelope(bytes, MAGIC) { version, payload ->
        if (version != VERSION) throw UnsupportedHumanFidelityReviewRepresentationVersionException(version)
        decodePayload(payload)
    }

    private fun encodePayload(record: HumanFidelityReviewRecord): ByteArray = ByteArrayOutputStream().use { bytes ->
        DataOutputStream(bytes).use { output ->
            output.writeString(record.reviewId.value)
            output.writeTarget(record.target)
            output.writeString(record.reviewerPrincipalId.value)
            output.writeString(record.reviewedAt.toString())
            output.writeString(record.artifacts.worksheetSha256.value)
            output.writeString(record.artifacts.ownerReviewRecordSha256.value)
            output.writeString(record.coverage.kind.name)
            output.writeStrings(record.coverage.reviewedPages.map(Int::toString))
            output.writeCount(record.coverage.reviewedCharacterScopes.size)
            record.coverage.reviewedCharacterScopes.forEach {
                output.writeInt(it.pageNumber); output.writeInt(it.transcriptionBlockIndex)
                output.writeInt(it.startCodePointInclusive); output.writeInt(it.endCodePointExclusive)
            }
            output.writeString(record.reviewState.name)
            output.writeString(record.descriptiveFidelity)
            output.writeCount(record.discrepancyOccurrences.size)
            record.discrepancyOccurrences.sortedBy { it.discrepancyId.value }.forEach { output.writeDiscrepancy(it) }
            output.writeCount(record.systematicPatterns.size)
            record.systematicPatterns.sortedBy { it.patternId.value }.forEach { output.writePattern(it) }
            output.writeBoolean(record.supersession != null)
            record.supersession?.let {
                output.writeString(it.predecessorReviewId.value); output.writeTarget(it.target)
            }
            output.writeBoolean(record.adjudication != null)
            record.adjudication?.let {
                output.writeStrings(it.conflictingReviewIds.map { id -> id.value }.sorted())
                output.writeString(it.selectedReviewId.value); output.writeTarget(it.target)
            }
        }
        bytes.toByteArray()
    }

    private fun decodePayload(payload: ByteArray): HumanFidelityReviewRecord = DataInputStream(ByteArrayInputStream(payload)).use { input ->
        val reviewId = HumanFidelityReviewId(input.readString())
        val target = input.readTarget()
        val reviewer = PrincipalId(input.readString())
        val reviewedAt = Instant.parse(input.readString())
        val artifacts = HumanFidelityReviewArtifacts(OcrSha256Digest(input.readString()), OcrSha256Digest(input.readString()))
        val coverageKind = input.readEnum<HumanFidelityCoverageKind>()
        val pages = input.readStrings().map(String::toInt)
        val scopes = List(input.readCount()) {
            HumanFidelityCharacterScope(input.readInt(), input.readInt(), input.readInt(), input.readInt())
        }
        val coverage = HumanFidelityReviewCoverage(coverageKind, pages, scopes)
        val reviewState = input.readEnum<HumanFidelityReviewState>()
        val descriptiveFidelity = input.readString()
        val discrepancies = List(input.readCount()) { input.readDiscrepancy(reviewId) }
        val patterns = List(input.readCount()) { input.readPattern(reviewId) }
        val supersession = if (input.readBoolean()) {
            HumanFidelityReviewSupersession(HumanFidelityReviewId(input.readString()), input.readTarget())
        } else null
        val adjudication = if (input.readBoolean()) {
            HumanFidelityReviewAdjudicationReference(
                input.readStrings().map(::HumanFidelityReviewId), HumanFidelityReviewId(input.readString()), input.readTarget(),
            )
        } else null
        require(input.available() == 0) { "Human fidelity review payload contains trailing bytes" }
        HumanFidelityReviewRecord(
            reviewId, target, reviewer, reviewedAt, artifacts, coverage, reviewState, descriptiveFidelity,
            discrepancies, patterns, supersession, adjudication,
        )
    }

    private fun DataOutputStream.writeTarget(target: HumanFidelityReviewTarget) {
        writeString(target.evidenceArtifactId.value); writeString(target.sourceSha256.value)
        writeString(target.preparationIdentity.value); writeString(target.derivativeGenerationId.value)
        writeString(target.derivativeGenerationSha256.value); writeString(target.derivativeContentSha256.value)
    }

    private fun DataInputStream.readTarget() = HumanFidelityReviewTarget(
        EvidenceArtifactId(readString()), OcrSha256Digest(readString()), OcrSha256Digest(readString()),
        DerivativeGenerationId(readString()), OcrSha256Digest(readString()), OcrSha256Digest(readString()),
    )

    private fun DataOutputStream.writeDiscrepancy(value: FidelityDiscrepancyOccurrence) {
        writeString(value.discrepancyId.value); writeString(value.reviewId.value)
        with(value.location) {
            writeString(evidenceArtifactId.value); writeString(sourceSha256.value); writeInt(pageNumber)
            writeString(preparationIdentity.value); writeString(preparationRegionId.value)
            writeString(derivativeGenerationId.value); writeString(derivativeGenerationSha256.value)
            writeString(derivativeContentSha256.value); writeString(derivativeRegionId.value)
            writeInt(transcriptionBlockIndex); writeInt(startCodePointInclusive); writeInt(endCodePointExclusive)
            writeString(originalProviderSubstring); writeString(originalProviderSubstringSha256.value)
        }
        writeString(value.classification.name); writeString(value.severity.name); writeString(value.reason)
        writeNullableString(value.explicitClassificationDetail)
        when (val resolution = value.sourceResolution) {
            HumanSourceResolution.Unresolved -> writeString("UNRESOLVED")
            is HumanSourceResolution.ResolvedAgainstSource -> {
                writeString("RESOLVED"); writeString(resolution.assertedSourceValue)
                writeString(resolution.assertedSourceValueSha256.value); writeString(resolution.sourcePageRepresentationId.value)
                writeString(resolution.resolvingReviewerPrincipalId.value)
            }
        }
        writeString(value.causeAssessment.state.name); writeNullableString(value.causeAssessment.mechanism)
        writeNullableString(value.causeAssessment.supportingBasis); writeNullableString(value.systematicPatternId?.value)
    }

    private fun DataInputStream.readDiscrepancy(expectedReviewId: HumanFidelityReviewId): FidelityDiscrepancyOccurrence {
        val discrepancyId = FidelityDiscrepancyId(readString())
        val reviewId = HumanFidelityReviewId(readString())
        require(reviewId == expectedReviewId) { "Nested discrepancy review identity mismatch" }
        val location = FidelityDiscrepancyLocation.fromPersistedFacts(
            EvidenceArtifactId(readString()), OcrSha256Digest(readString()), readInt(), OcrSha256Digest(readString()),
            SourceRegionId(readString()), DerivativeGenerationId(readString()), OcrSha256Digest(readString()),
            OcrSha256Digest(readString()), SourceRegionId(readString()), readInt(), readInt(), readInt(),
            readString(), OcrSha256Digest(readString()),
        )
        val classification = readEnum<FidelityDiscrepancyClassification>()
        val severity = readEnum<FidelityDiscrepancySeverity>()
        val reason = readString()
        val detail = readNullableString()
        val resolution = when (val state = readString()) {
            "UNRESOLVED" -> HumanSourceResolution.Unresolved
            "RESOLVED" -> HumanSourceResolution.ResolvedAgainstSource(
                readString(), OcrSha256Digest(readString()), PageRepresentationId(readString()), PrincipalId(readString()),
            )
            else -> error("Unknown human source resolution '$state'")
        }
        val cause = FidelityCauseAssessment(readEnum(), readNullableString(), readNullableString())
        val patternId = readNullableString()?.let(::SystematicDiscrepancyPatternId)
        return FidelityDiscrepancyOccurrence(
            discrepancyId, reviewId, location, classification, severity, reason, detail, resolution, cause, patternId,
        )
    }

    private fun DataOutputStream.writePattern(value: SystematicDiscrepancyPattern) {
        writeString(value.patternId.value); writeString(value.reviewId.value)
        writeStrings(value.memberDiscrepancyIds.map { it.value }.sorted())
        writeString(value.observedPatternDescription); writeInt(value.occurrenceCount)
        writeString(value.reviewerPrincipalId.value)
    }

    private fun DataInputStream.readPattern(expectedReviewId: HumanFidelityReviewId): SystematicDiscrepancyPattern {
        val patternId = SystematicDiscrepancyPatternId(readString())
        val reviewId = HumanFidelityReviewId(readString())
        require(reviewId == expectedReviewId) { "Nested pattern review identity mismatch" }
        return SystematicDiscrepancyPattern(
            patternId, reviewId, readStrings().map(::FidelityDiscrepancyId), readString(), readInt(), PrincipalId(readString()),
        )
    }

    private fun DataOutputStream.writeStrings(values: Collection<String>) {
        writeCount(values.size); values.forEach { writeString(it) }
    }

    private fun DataInputStream.readStrings(): List<String> = List(readCount()) { readString() }

    private fun DataOutputStream.writeNullableString(value: String?) {
        writeBoolean(value != null); value?.let { writeString(it) }
    }

    private fun DataInputStream.readNullableString(): String? = if (readBoolean()) readString() else null

    private fun DataOutputStream.writeCount(value: Int) {
        require(value in 0..MAX_ITEMS); writeInt(value)
    }

    private fun DataInputStream.readCount(): Int = readInt().also { require(it in 0..MAX_ITEMS) { "Collection count is outside bounds" } }

    private fun DataOutputStream.writeString(value: String) {
        val encoded = value.toByteArray(StandardCharsets.UTF_8); require(encoded.size <= MAX_STRING_BYTES)
        writeInt(encoded.size); write(encoded)
    }

    private fun DataInputStream.readString(): String {
        val size = readInt(); require(size in 0..MAX_STRING_BYTES) { "String length is outside bounds" }
        val encoded = ByteArray(size).also(::readFully)
        return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(encoded)).toString()
    }

    private inline fun <reified E : Enum<E>> DataInputStream.readEnum(): E = enumValueOf(readString())
}

internal object HumanFidelityGovernanceAuditCodec {
    private const val MAGIC = 0x50484641 // PHFA
    internal const val VERSION = 1
    internal const val MAX_RECORD_BYTES = 1024 * 1024
    private const val MAX_STRING_BYTES = 16 * 1024

    fun encode(record: HumanFidelityGovernanceAuditRecord): ByteArray = envelope(MAGIC, ByteArrayOutputStream().use { bytes ->
        DataOutputStream(bytes).use { output ->
            output.writeBoundedString(record.eventId.value, MAX_STRING_BYTES)
            output.writeBoundedString(record.eventType.name, MAX_STRING_BYTES)
            output.writeBoundedString(record.recordedAt.toString(), MAX_STRING_BYTES)
            output.writeBoundedString(record.actorPrincipalId.value, MAX_STRING_BYTES)
            output.writeBoundedString(record.reviewId.value, MAX_STRING_BYTES)
            output.writeAuditTarget(record.target)
            output.writeBoundedString(record.reviewPayloadSha256.value, MAX_STRING_BYTES)
            output.writeBoundedString(record.outcome.name, MAX_STRING_BYTES)
            output.writeBoolean(record.factualDetail != null)
            record.factualDetail?.let { output.writeBoundedString(it, MAX_STRING_BYTES) }
        }
        bytes.toByteArray()
    })

    fun decode(bytes: ByteArray): HumanFidelityGovernanceAuditRecord = decodeEnvelope(bytes, MAGIC) { version, payload ->
        if (version != VERSION) throw UnsupportedHumanFidelityAuditRepresentationVersionException(version)
        DataInputStream(ByteArrayInputStream(payload)).use { input ->
            val record = HumanFidelityGovernanceAuditRecord(
                HumanFidelityGovernanceAuditEventId(input.readBoundedString(MAX_STRING_BYTES)),
                enumValueOf(input.readBoundedString(MAX_STRING_BYTES)),
                Instant.parse(input.readBoundedString(MAX_STRING_BYTES)),
                PrincipalId(input.readBoundedString(MAX_STRING_BYTES)),
                HumanFidelityReviewId(input.readBoundedString(MAX_STRING_BYTES)),
                input.readAuditTarget(),
                OcrSha256Digest(input.readBoundedString(MAX_STRING_BYTES)),
                enumValueOf(input.readBoundedString(MAX_STRING_BYTES)),
                if (input.readBoolean()) input.readBoundedString(MAX_STRING_BYTES) else null,
            )
            require(input.available() == 0) { "Human fidelity audit payload contains trailing bytes" }
            record
        }
    }

    private fun DataOutputStream.writeAuditTarget(target: HumanFidelityReviewTarget) {
        listOf(
            target.evidenceArtifactId.value, target.sourceSha256.value, target.preparationIdentity.value,
            target.derivativeGenerationId.value, target.derivativeGenerationSha256.value, target.derivativeContentSha256.value,
        ).forEach { writeBoundedString(it, MAX_STRING_BYTES) }
    }

    private fun DataInputStream.readAuditTarget() = HumanFidelityReviewTarget(
        EvidenceArtifactId(readBoundedString(MAX_STRING_BYTES)), OcrSha256Digest(readBoundedString(MAX_STRING_BYTES)),
        OcrSha256Digest(readBoundedString(MAX_STRING_BYTES)), DerivativeGenerationId(readBoundedString(MAX_STRING_BYTES)),
        OcrSha256Digest(readBoundedString(MAX_STRING_BYTES)), OcrSha256Digest(readBoundedString(MAX_STRING_BYTES)),
    )
}

private fun envelope(magic: Int, payload: ByteArray): ByteArray = ByteArrayOutputStream().use { bytes ->
    DataOutputStream(bytes).use { output ->
        output.writeInt(magic); output.writeInt(1); output.writeInt(payload.size); output.write(payload)
        output.write(envelopeIntegrity(magic, 1, payload))
    }
    bytes.toByteArray()
}

private inline fun <T> decodeEnvelope(bytes: ByteArray, expectedMagic: Int, decode: (Int, ByteArray) -> T): T =
    DataInputStream(ByteArrayInputStream(bytes)).use { input ->
        require(input.readInt() == expectedMagic) { "Stored record magic is invalid" }
        val version = input.readInt()
        val payloadSize = input.readInt(); require(payloadSize in 0..(16 * 1024 * 1024)) { "Payload length is outside bounds" }
        val payload = ByteArray(payloadSize).also(input::readFully)
        val expectedDigest = ByteArray(32).also(input::readFully)
        require(envelopeIntegrity(expectedMagic, version, payload).contentEquals(expectedDigest)) { "Stored record integrity mismatch" }
        require(input.available() == 0) { "Stored record contains trailing bytes" }
        decode(version, payload)
    }

private fun DataOutputStream.writeBoundedString(value: String, maximum: Int) {
    val bytes = value.toByteArray(StandardCharsets.UTF_8); require(bytes.size <= maximum)
    writeInt(bytes.size); write(bytes)
}

private fun DataInputStream.readBoundedString(maximum: Int): String {
    val size = readInt(); require(size in 0..maximum) { "String length is outside bounds" }
    val bytes = ByteArray(size).also(::readFully)
    return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
        .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString()
}

private fun fidelityStorageSha256Hex(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes)
    .joinToString("") { "%02x".format(it.toInt() and 0xff) }

private fun envelopeIntegrity(magic: Int, version: Int, payload: ByteArray): ByteArray =
    MessageDigest.getInstance("SHA-256").digest(ByteBuffer.allocate(8 + payload.size).putInt(magic).putInt(version).put(payload).array())
