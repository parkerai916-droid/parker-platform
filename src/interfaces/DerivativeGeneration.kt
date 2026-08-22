package parker.core.interfaces

import java.time.Instant

@JvmInline
value class DerivativeGenerationId(val value: String) {
    init {
        require(value.isNotBlank()) { "DerivativeGenerationId must not be blank" }
    }
}

sealed class DerivativeParentReference {
    data class RootEvidenceArtifact(val evidenceArtifactId: EvidenceArtifactId) : DerivativeParentReference()
    data class ChildSourceEvidenceArtifact(val evidenceArtifactId: EvidenceArtifactId) : DerivativeParentReference()
    data class ParentGeneration(val derivativeGenerationId: DerivativeGenerationId) : DerivativeParentReference()
}

data class DerivativeProducerIdentity(
    val pluginIdentity: String,
    val pluginVersion: String,
    val configurationIdentity: String,
    val adapterIdentity: String? = null,
    val adapterVersion: String? = null,
    val modelIdentity: String? = null,
    val modelVersion: String? = null,
) {
    init {
        require(pluginIdentity.isNotBlank()) { "DerivativeProducerIdentity.pluginIdentity must not be blank" }
        require(pluginVersion.isNotBlank()) { "DerivativeProducerIdentity.pluginVersion must not be blank" }
        require(configurationIdentity.isNotBlank()) { "DerivativeProducerIdentity.configurationIdentity must not be blank" }
        require((adapterIdentity == null) == (adapterVersion == null)) {
            "DerivativeProducerIdentity adapter identity and version must either both be present or both be absent"
        }
        require(adapterIdentity == null || adapterIdentity.isNotBlank()) {
            "DerivativeProducerIdentity.adapterIdentity must not be blank when present"
        }
        require(adapterVersion == null || adapterVersion.isNotBlank()) {
            "DerivativeProducerIdentity.adapterVersion must not be blank when present"
        }
        require((modelIdentity == null) == (modelVersion == null)) {
            "DerivativeProducerIdentity model identity and version must either both be present or both be absent"
        }
        require(modelIdentity == null || modelIdentity.isNotBlank()) {
            "DerivativeProducerIdentity.modelIdentity must not be blank when present"
        }
        require(modelVersion == null || modelVersion.isNotBlank()) {
            "DerivativeProducerIdentity.modelVersion must not be blank when present"
        }
    }
}

enum class DerivativeTransformation {
    CHARACTER_DECODING,
    MIME_TRANSFER_DECODING,
    DECOMPRESSION,
    OCR,
    STRUCTURAL_PARSING,
    WHITESPACE_NORMALISATION,
    LINE_ENDING_NORMALISATION,
    DATE_PARSING,
    NUMERIC_INTERPRETATION,
    FIELD_UNQUOTING,
    ENTITY_DECODING,
    IMAGE_RASTERISATION,
    PAGE_RENDERING,
    TABLE_RECONSTRUCTION,
    READING_ORDER_INFERENCE,
    MODEL_INFERENCE,
    METADATA_INTERPRETATION,
}

enum class DerivativeCompletenessState {
    ACCOUNTED_FOR,
    ACCOUNTED_FOR_WITH_QUALIFICATIONS,
    KNOWN_INCOMPLETE,
    NOT_ASSESSABLE,
    NOT_APPLICABLE,
}

enum class DerivativeOperationalOutcome {
    USABLE,
    NOT_USABLE,
}

sealed class DerivativeContentIdentity {
    object NoCanonicalSerialization : DerivativeContentIdentity()

    data class Digest(val algorithm: String, val digest: String) : DerivativeContentIdentity() {
        init {
            require(algorithm == "SHA-256") {
                "DerivativeContentIdentity.Digest algorithm must currently be SHA-256"
            }
            require(digest.matches(Regex("^[0-9a-f]{64}$"))) {
                "DerivativeContentIdentity.Digest digest must be 64 lowercase hexadecimal characters"
            }
        }
    }
}

data class CandidateDerivativeGeneration(
    val rootSourceEvidenceArtifactId: EvidenceArtifactId,
    val parents: List<DerivativeParentReference>,
    val derivativeKind: String,
    val producerIdentity: DerivativeProducerIdentity,
    val transformationHistory: List<DerivativeTransformation>,
    val contentIdentity: DerivativeContentIdentity,
    val completenessState: DerivativeCompletenessState,
    val operationalOutcome: DerivativeOperationalOutcome,
    val warnings: List<String> = emptyList(),
    val confidence: Double? = null,
) {
    init {
        require(parents.isNotEmpty()) { "CandidateDerivativeGeneration.parents must not be empty" }
        require(parents.size == 1 || parents.all { it is DerivativeParentReference.ParentGeneration }) {
            "CandidateDerivativeGeneration may have multiple parents only when reconciling parent generations"
        }
        parents.forEach { parent ->
            val directEvidenceArtifactId = when (parent) {
                is DerivativeParentReference.RootEvidenceArtifact -> parent.evidenceArtifactId
                is DerivativeParentReference.ChildSourceEvidenceArtifact -> parent.evidenceArtifactId
                is DerivativeParentReference.ParentGeneration -> null
            }
            require(directEvidenceArtifactId == null || directEvidenceArtifactId == rootSourceEvidenceArtifactId) {
                "CandidateDerivativeGeneration direct evidence parent must equal its root source"
            }
        }
        require(derivativeKind.isNotBlank()) { "CandidateDerivativeGeneration.derivativeKind must not be blank" }
        require(transformationHistory.isNotEmpty()) {
            "CandidateDerivativeGeneration.transformationHistory must not be empty"
        }
        require(warnings.none { it.isBlank() }) { "CandidateDerivativeGeneration.warnings must not contain blank values" }
        confidence?.let { require(it in 0.0..1.0) { "CandidateDerivativeGeneration.confidence must fall within 0.0..1.0" } }
        if (DerivativeTransformation.OCR in transformationHistory ||
            DerivativeTransformation.MODEL_INFERENCE in transformationHistory
        ) {
            require(producerIdentity.modelIdentity != null) {
                "CandidateDerivativeGeneration Tier B transformations require model identity and version"
            }
        }
    }
}

data class DerivativeGenerationRecord(
    val derivativeGenerationId: DerivativeGenerationId,
    val rootSourceEvidenceArtifactId: EvidenceArtifactId,
    val parents: List<DerivativeParentReference>,
    val derivativeKind: String,
    val producerIdentity: DerivativeProducerIdentity,
    val transformationHistory: List<DerivativeTransformation>,
    val generatedAt: Instant,
    val contentIdentity: DerivativeContentIdentity,
    val completenessState: DerivativeCompletenessState,
    val operationalOutcome: DerivativeOperationalOutcome,
    val warnings: List<String> = emptyList(),
    val confidence: Double? = null,
) {
    init {
        CandidateDerivativeGeneration(
            rootSourceEvidenceArtifactId = rootSourceEvidenceArtifactId,
            parents = parents,
            derivativeKind = derivativeKind,
            producerIdentity = producerIdentity,
            transformationHistory = transformationHistory,
            contentIdentity = contentIdentity,
            completenessState = completenessState,
            operationalOutcome = operationalOutcome,
            warnings = warnings,
            confidence = confidence,
        )
    }
}

sealed class DerivativeGenerationStorageException(message: String, cause: Throwable? = null) :
    RuntimeException(message, cause) {
    class DuplicateIdentifier(val derivativeGenerationId: DerivativeGenerationId) :
        DerivativeGenerationStorageException("Derivative generation '${derivativeGenerationId.value}' already exists")
    class UnsafeIdentifier(val derivativeGenerationId: DerivativeGenerationId) :
        DerivativeGenerationStorageException("Derivative generation identifier '${derivativeGenerationId.value}' is unsafe for storage")
    class InvalidStorageRoot(path: String, reason: String) :
        DerivativeGenerationStorageException("Derivative generation storage root '$path' is invalid: $reason")
    class PersistenceFailure(message: String, cause: Throwable) : DerivativeGenerationStorageException(message, cause)
    class CorruptRecord(val derivativeGenerationId: DerivativeGenerationId, message: String, cause: Throwable? = null) :
        DerivativeGenerationStorageException("Derivative generation '${derivativeGenerationId.value}' is corrupt: $message", cause)
}

interface DerivativeGenerationStorage {
    suspend fun admit(record: DerivativeGenerationRecord)
    suspend fun retrieve(derivativeGenerationId: DerivativeGenerationId): DerivativeGenerationRecord?
}
