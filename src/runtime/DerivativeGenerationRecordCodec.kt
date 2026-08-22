package parker.core.runtime

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.nio.charset.StandardCharsets
import java.nio.charset.CodingErrorAction
import java.time.Instant
import parker.core.interfaces.DerivativeCompletenessState
import parker.core.interfaces.DerivativeContentIdentity
import parker.core.interfaces.DerivativeGenerationId
import parker.core.interfaces.DerivativeGenerationRecord
import parker.core.interfaces.DerivativeOperationalOutcome
import parker.core.interfaces.DerivativeParentReference
import parker.core.interfaces.DerivativeProducerIdentity
import parker.core.interfaces.DerivativeTransformation
import parker.core.interfaces.EvidenceArtifactId

internal object DerivativeGenerationRecordCodec {
    private const val MAGIC = 0x50444752
    private const val VERSION = 1
    private const val MAX_COLLECTION_SIZE = 10_000
    private const val MAX_STRING_BYTES = 1024 * 1024

    fun encode(record: DerivativeGenerationRecord): ByteArray = ByteArrayOutputStream().use { bytes ->
        DataOutputStream(bytes).use { output ->
            output.writeInt(MAGIC)
            output.writeInt(VERSION)
            output.writeString(record.derivativeGenerationId.value)
            output.writeString(record.rootSourceEvidenceArtifactId.value)
            output.writeCollectionSize(record.parents.size)
            record.parents.forEach { parent ->
                when (parent) {
                    is DerivativeParentReference.RootEvidenceArtifact -> {
                        output.writeByte(1)
                        output.writeString(parent.evidenceArtifactId.value)
                    }
                    is DerivativeParentReference.ChildSourceEvidenceArtifact -> {
                        output.writeByte(2)
                        output.writeString(parent.evidenceArtifactId.value)
                    }
                    is DerivativeParentReference.ParentGeneration -> {
                        output.writeByte(3)
                        output.writeString(parent.derivativeGenerationId.value)
                    }
                }
            }
            output.writeString(record.derivativeKind)
            output.writeProducer(record.producerIdentity)
            output.writeCollectionSize(record.transformationHistory.size)
            record.transformationHistory.forEach { output.writeString(it.name) }
            output.writeString(record.generatedAt.toString())
            when (val identity = record.contentIdentity) {
                DerivativeContentIdentity.NoCanonicalSerialization -> output.writeByte(0)
                is DerivativeContentIdentity.Digest -> {
                    output.writeByte(1)
                    output.writeString(identity.algorithm)
                    output.writeString(identity.digest)
                }
            }
            output.writeString(record.completenessState.name)
            output.writeString(record.operationalOutcome.name)
            output.writeCollectionSize(record.warnings.size)
            record.warnings.forEach { output.writeString(it) }
            output.writeBoolean(record.confidence != null)
            record.confidence?.let(output::writeDouble)
        }
        bytes.toByteArray()
    }

    fun decode(content: ByteArray): DerivativeGenerationRecord = DataInputStream(ByteArrayInputStream(content)).use { input ->
        require(input.readInt() == MAGIC) { "invalid record magic" }
        require(input.readInt() == VERSION) { "unsupported record version" }
        val id = DerivativeGenerationId(input.readString())
        val root = EvidenceArtifactId(input.readString())
        val parents = List(input.readCollectionSize()) {
            when (input.readUnsignedByte()) {
                1 -> DerivativeParentReference.RootEvidenceArtifact(EvidenceArtifactId(input.readString()))
                2 -> DerivativeParentReference.ChildSourceEvidenceArtifact(EvidenceArtifactId(input.readString()))
                3 -> DerivativeParentReference.ParentGeneration(DerivativeGenerationId(input.readString()))
                else -> error("unknown parent kind")
            }
        }
        val derivativeKind = input.readString()
        val producer = input.readProducer()
        val transformations = List(input.readCollectionSize()) { enumValueOf<DerivativeTransformation>(input.readString()) }
        val generatedAt = Instant.parse(input.readString())
        val contentIdentity = when (input.readUnsignedByte()) {
            0 -> DerivativeContentIdentity.NoCanonicalSerialization
            1 -> DerivativeContentIdentity.Digest(input.readString(), input.readString())
            else -> error("unknown content identity kind")
        }
        val completeness = enumValueOf<DerivativeCompletenessState>(input.readString())
        val outcome = enumValueOf<DerivativeOperationalOutcome>(input.readString())
        val warnings = List(input.readCollectionSize()) { input.readString() }
        val confidence = if (input.readBoolean()) input.readDouble() else null
        require(input.available() == 0) { "unexpected trailing bytes" }
        DerivativeGenerationRecord(
            derivativeGenerationId = id,
            rootSourceEvidenceArtifactId = root,
            parents = parents,
            derivativeKind = derivativeKind,
            producerIdentity = producer,
            transformationHistory = transformations,
            generatedAt = generatedAt,
            contentIdentity = contentIdentity,
            completenessState = completeness,
            operationalOutcome = outcome,
            warnings = warnings,
            confidence = confidence,
        )
    }

    private fun DataOutputStream.writeProducer(value: DerivativeProducerIdentity) {
        writeString(value.pluginIdentity)
        writeString(value.pluginVersion)
        writeString(value.configurationIdentity)
        writeNullableString(value.adapterIdentity)
        writeNullableString(value.adapterVersion)
        writeNullableString(value.modelIdentity)
        writeNullableString(value.modelVersion)
    }

    private fun DataInputStream.readProducer() = DerivativeProducerIdentity(
        pluginIdentity = readString(),
        pluginVersion = readString(),
        configurationIdentity = readString(),
        adapterIdentity = readNullableString(),
        adapterVersion = readNullableString(),
        modelIdentity = readNullableString(),
        modelVersion = readNullableString(),
    )

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

    private fun DataInputStream.readCollectionSize(): Int = readInt().also {
        require(it in 0..MAX_COLLECTION_SIZE) { "invalid collection size" }
    }

    private fun DataOutputStream.writeCollectionSize(size: Int) {
        require(size in 0..MAX_COLLECTION_SIZE) { "collection exceeds codec limit" }
        writeInt(size)
    }
}
