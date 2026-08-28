package parker.core.runtime

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import java.time.Instant
import parker.core.interfaces.DerivativeGenerationId
import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.SavedAnalysisEvidenceReference
import parker.core.interfaces.SavedAnalysisId
import parker.core.interfaces.SavedAnalysisRecord
import parker.core.interfaces.AnalysisAcquisitionAssurance
import parker.core.interfaces.AnalysisAcquisitionMechanism
import parker.core.interfaces.AnalysisHumanReviewState
import parker.core.interfaces.DerivativeCompletenessState
import parker.core.interfaces.TranscriptionFidelity

/**
 * Reviewed Analysis Result — Explicit Owner Save. Mirrors
 * [DerivativeGenerationRecordCodec]'s own exact byte-level discipline
 * (magic + version header, length-prefixed UTF-8 strings, bounded
 * collection sizes, `require(input.available() == 0)` trailing-byte check)
 * -- a genuinely new codec, not a reuse of the derivative-generation one,
 * since this record's own shape and authority are distinct.
 */
/** Thrown by [SavedAnalysisRecordCodec.decode] specifically for a representation-version mismatch, so [FileSystemSavedAnalysisStorage] can distinguish it from ordinary corruption -- never a message-string-sniffing distinction. */
internal class UnsupportedSavedAnalysisRepresentationVersionException(val version: Int) :
    Exception("unsupported saved analysis representation version: $version")

internal object SavedAnalysisRecordCodec {
    private const val MAGIC = 0x50444153 // "PDAS" -- Parker Document Analysis, Saved
    private const val VERSION = 2
    private const val MAX_COLLECTION_SIZE = 10_000
    private const val MAX_STRING_BYTES = 8 * 1024 * 1024

    fun encode(record: SavedAnalysisRecord): ByteArray = ByteArrayOutputStream().use { bytes ->
        DataOutputStream(bytes).use { output ->
            output.writeInt(MAGIC)
            output.writeInt(VERSION)
            output.writeString(record.savedAnalysisId.value)
            output.writeString(record.savedAt.toString())
            output.writeString(record.analysedAt.toString())
            output.writeString(record.instruction)
            output.writeString(record.analysisText)
            output.writeCollectionSize(record.evidenceReferences.size)
            record.evidenceReferences.forEach { reference ->
                output.writeString(reference.evidenceArtifactId.value)
                output.writeString(reference.derivativeGenerationId.value)
                output.writeString(reference.derivativeKind)
                output.writeAssurance(reference.assurance)
            }
            output.writeNullableString(record.mechanismIdentity)
            output.writeNullableString(record.mechanismVersion)
        }
        bytes.toByteArray()
    }

    fun decode(content: ByteArray): SavedAnalysisRecord = DataInputStream(ByteArrayInputStream(content)).use { input ->
        require(input.readInt() == MAGIC) { "invalid saved analysis magic" }
        val version = input.readInt()
        if (version !in 1..VERSION) throw UnsupportedSavedAnalysisRepresentationVersionException(version)
        val id = SavedAnalysisId(input.readString())
        val savedAt = Instant.parse(input.readString())
        val analysedAt = Instant.parse(input.readString())
        val instruction = input.readString()
        val analysisText = input.readString()
        val evidenceReferences = List(input.readCollectionSize()) {
            SavedAnalysisEvidenceReference(
                evidenceArtifactId = EvidenceArtifactId(input.readString()),
                derivativeGenerationId = DerivativeGenerationId(input.readString()),
                derivativeKind = input.readString(),
                assurance = if (version >= 2) input.readAssurance() else null,
            )
        }
        val mechanismIdentity = input.readNullableString()
        val mechanismVersion = input.readNullableString()
        require(input.available() == 0) { "unexpected trailing bytes" }
        SavedAnalysisRecord(
            savedAnalysisId = id,
            savedAt = savedAt,
            analysedAt = analysedAt,
            instruction = instruction,
            analysisText = analysisText,
            evidenceReferences = evidenceReferences,
            mechanismIdentity = mechanismIdentity,
            mechanismVersion = mechanismVersion,
        )
    }

    private fun DataOutputStream.writeString(value: String) {
        val encoded = value.toByteArray(StandardCharsets.UTF_8)
        require(encoded.size <= MAX_STRING_BYTES) { "string exceeds codec limit" }
        writeInt(encoded.size)
        write(encoded)
    }

    private fun DataInputStream.readString(): String {
        val size = readInt()
        require(size in 0..MAX_STRING_BYTES) { "invalid string length" }
        val encoded = ByteArray(size).also(::readFully)
        return StandardCharsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(java.nio.ByteBuffer.wrap(encoded))
            .toString()
    }

    private fun DataOutputStream.writeNullableString(value: String?) {
        writeBoolean(value != null)
        value?.let { writeString(it) }
    }

    private fun DataInputStream.readNullableString(): String? = if (readBoolean()) readString() else null

    private fun DataOutputStream.writeNullableLong(value: Long?) {
        writeBoolean(value != null)
        value?.let(::writeLong)
    }

    private fun DataInputStream.readNullableLong(): Long? = if (readBoolean()) readLong() else null

    private fun DataOutputStream.writeStringList(values: Collection<String>) {
        writeCollectionSize(values.size)
        values.forEach { writeString(it) }
    }

    private fun DataInputStream.readStringList(): List<String> = List(readCollectionSize()) { readString() }

    private fun DataOutputStream.writeIntList(values: Collection<Int>?) {
        writeBoolean(values != null)
        if (values != null) {
            writeCollectionSize(values.size)
            values.forEach(::writeInt)
        }
    }

    private fun DataInputStream.readIntList(): List<Int>? = if (readBoolean()) List(readCollectionSize()) { readInt() } else null

    private fun DataOutputStream.writeAssurance(value: AnalysisAcquisitionAssurance?) {
        writeBoolean(value != null)
        if (value == null) return
        writeNullableString(value.sourceSha256)
        writeNullableLong(value.sourceByteLength)
        writeNullableString(value.sourceMediaType)
        writeString(value.mechanism.name)
        writeNullableString(value.capabilityIdentity)
        writeStringList(value.routingReasons)
        writeNullableString(value.providerIdentity)
        writeNullableString(value.adapterIdentity)
        writeNullableString(value.adapterVersion)
        writeNullableString(value.modelIdentity)
        writeNullableString(value.modelSnapshot)
        writeNullableString(value.configurationProfile)
        writeNullableString(value.processingProfile)
        writeNullableString(value.fidelity?.name)
        writeString(value.completenessState.name)
        writeIntList(value.requestedPages)
        writeIntList(value.submittedPages)
        writeIntList(value.returnedPages)
        writeStringList(value.pageOutcomes)
        writeBoolean(value.containsUncertaintyOrIllegibility)
        writeStringList(value.humanReviewStates.map { it.name }.sorted())
        writeIntList(value.reviewedPages.sorted())
        writeInt(value.reviewedCharacterScopeCount)
    }

    private fun DataInputStream.readAssurance(): AnalysisAcquisitionAssurance? {
        if (!readBoolean()) return null
        return AnalysisAcquisitionAssurance(
            sourceSha256 = readNullableString(),
            sourceByteLength = readNullableLong(),
            sourceMediaType = readNullableString(),
            mechanism = AnalysisAcquisitionMechanism.valueOf(readString()),
            capabilityIdentity = readNullableString(),
            routingReasons = readStringList(),
            providerIdentity = readNullableString(),
            adapterIdentity = readNullableString(),
            adapterVersion = readNullableString(),
            modelIdentity = readNullableString(),
            modelSnapshot = readNullableString(),
            configurationProfile = readNullableString(),
            processingProfile = readNullableString(),
            fidelity = readNullableString()?.let(TranscriptionFidelity::valueOf),
            completenessState = DerivativeCompletenessState.valueOf(readString()),
            requestedPages = readIntList(),
            submittedPages = readIntList(),
            returnedPages = readIntList(),
            pageOutcomes = readStringList(),
            containsUncertaintyOrIllegibility = readBoolean(),
            humanReviewStates = readStringList().mapTo(linkedSetOf(), AnalysisHumanReviewState::valueOf),
            reviewedPages = readIntList().orEmpty().toSet(),
            reviewedCharacterScopeCount = readInt(),
        )
    }

    private fun DataInputStream.readCollectionSize(): Int = readInt().also {
        require(it in 0..MAX_COLLECTION_SIZE) { "invalid collection size" }
    }

    private fun DataOutputStream.writeCollectionSize(size: Int) {
        require(size in 0..MAX_COLLECTION_SIZE) { "collection exceeds codec limit" }
        writeInt(size)
    }
}
