package parker.core.interfaces

import java.nio.charset.StandardCharsets
import java.time.Duration
import java.time.Instant

/** Frozen Unit 1 limits for the Hermes Processing Service v1 protocol domain. */
object HermesProcessingServiceV1Limits {
    const val MAX_SOURCE_BYTES: Long = 500L * 1024L * 1024L
    const val MAX_METADATA_ENVELOPE_BYTES: Long = 64L * 1024L
    const val MAX_INLINE_RESPONSE_BYTES: Long = 8L * 1024L * 1024L
    const val MAX_STRUCTURED_REPRESENTATION_BYTES: Long = 8L * 1024L * 1024L
    const val MAX_EXTRACTED_TEXT_CHARACTERS: Int = 4_000_000
    const val PROCESSING_TIMEOUT_SECONDS: Long = 300L
    val REQUEST_RESULT_RETENTION: Duration = Duration.ofDays(7)
    val TEMPORARY_WORKSPACE_TTL: Duration = Duration.ofHours(24)
    const val MAX_FILENAME_UTF8_BYTES: Int = 255
    const val MAX_ISSUES: Int = 100
    const val MAX_REPRESENTATIONS: Int = 32

    /** Reuses the established Hermes bounded human-text invariant. */
    const val MAX_FAILURE_DETAIL_CHARACTERS: Int = 4_096
    const val MAX_METHODS: Int = MAX_REPRESENTATIONS
}

@JvmInline
value class HermesV1ProtocolVersion(val value: String) {
    init { require(value == VALUE) { "unsupported Hermes protocol version: $value" } }

    companion object {
        const val VALUE = "1"
        val CURRENT = HermesV1ProtocolVersion(VALUE)
    }
}

@JvmInline
value class HermesV1RequestId(val value: String) {
    init { requireOpaqueIdentifier(value, "requestId") }
}

@JvmInline
value class HermesV1JobId(val value: String) {
    init { requireOpaqueIdentifier(value, "jobId") }
}

@JvmInline
value class HermesV1OccurrenceId(val value: String) {
    init { requireOpaqueIdentifier(value, "occurrenceId") }
}

@JvmInline
value class HermesV1BatchId(val value: String) {
    init { requireOpaqueIdentifier(value, "batchId") }
}

@JvmInline
value class HermesV1RepresentationId(val value: String) {
    init { requireOpaqueIdentifier(value, "representationId") }
}

@JvmInline
value class HermesV1Sha256(val value: String) {
    init { require(value.matches(SHA256_PATTERN)) { "SHA-256 must be 64 lowercase hexadecimal characters" } }

    companion object {
        private val SHA256_PATTERN = Regex("^[0-9a-f]{64}$")
    }
}

@JvmInline
value class HermesV1SourceReference(val value: String) {
    init {
        require(value.isNotBlank() && value.length <= HermesProcessingServiceV1Limits.MAX_FAILURE_DETAIL_CHARACTERS) {
            "source.reference must be bounded and non-blank"
        }
        require(value.none { it == '/' || it == '\\' || it == '\u0000' }) {
            "source.reference must be an opaque correlation identity, not a filesystem path"
        }
    }
}

@JvmInline
value class HermesV1MediaType(val value: String) {
    init {
        require(value.length in 3..255 && value.matches(MEDIA_TYPE_PATTERN)) {
            "mediaType must be a valid type/subtype token"
        }
    }

    companion object {
        private val MEDIA_TYPE_PATTERN = Regex("^[A-Za-z0-9!#$&^_.+\\-]+/[A-Za-z0-9!#$&^_.+\\-]+$")
    }
}

@JvmInline
value class HermesV1OriginalFilename(val value: String) {
    init {
        require(value.isNotBlank() && value != "." && value != "..") {
            "originalFilename must be non-blank"
        }
        require(value.none { it == '\u0000' || it == '/' || it == '\\' }) {
            "originalFilename must not contain path separators or NUL"
        }
        require(value.toByteArray(StandardCharsets.UTF_8).size <= HermesProcessingServiceV1Limits.MAX_FILENAME_UTF8_BYTES) {
            "originalFilename exceeds the UTF-8 byte limit"
        }
    }
}

@JvmInline
value class HermesV1SourceSize(val value: Long) {
    init {
        require(value >= 0) { "source size must not be negative" }
        require(value <= HermesProcessingServiceV1Limits.MAX_SOURCE_BYTES) {
            "source size exceeds the Hermes v1 maximum"
        }
    }
}

/** Correlation metadata only; it carries no Parker case or admission authority. */
data class HermesProcessingServiceV1Source(
    val reference: HermesV1SourceReference,
    val sourceSha256: HermesV1Sha256,
    val sizeBytes: HermesV1SourceSize,
    val originalFilename: HermesV1OriginalFilename,
    val mediaType: HermesV1MediaType,
)

/** Governed v1 request. There is deliberately no caseId or arbitrary source path. */
data class HermesProcessingServiceV1Request(
    val protocolVersion: HermesV1ProtocolVersion,
    val requestId: HermesV1RequestId,
    val jobId: HermesV1JobId,
    val occurrenceId: HermesV1OccurrenceId,
    val batchId: HermesV1BatchId,
    val source: HermesProcessingServiceV1Source,
    val requestedMethods: List<HermesV1ProcessingMethod> = emptyList(),
) {
    init {
        require(requestedMethods.size <= HermesProcessingServiceV1Limits.MAX_METHODS) {
            "too many requested processing methods"
        }
        require(requestedMethods.distinct().size == requestedMethods.size) {
            "requested processing methods must be unique"
        }
    }
}

/** Closed initial method registry. OCR, transcription, and TIFF inspection are not enabled here. */
enum class HermesV1ProcessingMethod(
    val wireValue: String,
    val establishedMethod: HermesProcessingMethod,
) {
    DIRECT_TEXT_EXTRACTION("DIRECT_TEXT_EXTRACTION", HermesProcessingMethod.DIRECT_TEXT_EXTRACTION),
    STRUCTURED_DOCUMENT_EXTRACTION("STRUCTURED_DOCUMENT_EXTRACTION", HermesProcessingMethod.STRUCTURED_DOCUMENT_EXTRACTION),
    STRUCTURED_SPREADSHEET_EXTRACTION("STRUCTURED_SPREADSHEET_EXTRACTION", HermesProcessingMethod.STRUCTURED_SPREADSHEET_EXTRACTION),
    STRUCTURED_EMAIL_EXTRACTION("STRUCTURED_EMAIL_EXTRACTION", HermesProcessingMethod.STRUCTURED_EMAIL_EXTRACTION),
    ;

    companion object {
        fun fromWireValue(value: String): HermesV1ProcessingMethod = entries.firstOrNull { it.wireValue == value }
            ?: throw IllegalArgumentException("unknown Hermes v1 processing method: $value")
    }
}

/** Closed initial representation registry. Membership does not enable processing. */
enum class HermesV1RepresentationType(val wireValue: String) {
    TEXT("TEXT"),
    STRUCTURED_DOCUMENT("STRUCTURED_DOCUMENT"),
    ;

    companion object {
        fun fromWireValue(value: String): HermesV1RepresentationType = entries.firstOrNull { it.wireValue == value }
            ?: throw IllegalArgumentException("unknown Hermes v1 representation type: $value")
    }
}

/** Closed initial issue registry for native/structured processing. */
enum class HermesV1IssueCode(val wireValue: String) {
    UNSUPPORTED_CONTENT("UNSUPPORTED_CONTENT"),
    MISSING_CONTENT("MISSING_CONTENT"),
    MALFORMED_STRUCTURE("MALFORMED_STRUCTURE"),
    STRUCTURE_AMBIGUITY("STRUCTURE_AMBIGUITY"),
    CONFLICTING_EXTRACTION("CONFLICTING_EXTRACTION"),
    OUTPUT_LIMIT_EXCEEDED("OUTPUT_LIMIT_EXCEEDED"),
    PROCESSING_QUALIFICATION("PROCESSING_QUALIFICATION"),
    ;

    companion object {
        fun fromWireValue(value: String): HermesV1IssueCode = entries.firstOrNull { it.wireValue == value }
            ?: throw IllegalArgumentException("unknown Hermes v1 issue code: $value")
    }
}

enum class HermesV1FailureCategory(val wireValue: String) {
    TRANSPORT("TRANSPORT"),
    AUTHORIZATION("AUTHORIZATION"),
    INTEGRITY("INTEGRITY"),
    UNSUPPORTED("UNSUPPORTED"),
    MALFORMED("MALFORMED"),
    RESOURCE_LIMIT("RESOURCE_LIMIT"),
    TIMEOUT("TIMEOUT"),
    PROCESSOR("PROCESSOR"),
    INTERNAL("INTERNAL"),
    ;

    companion object {
        fun fromWireValue(value: String): HermesV1FailureCategory = entries.firstOrNull { it.wireValue == value }
            ?: throw IllegalArgumentException("unknown Hermes v1 failure category: $value")
    }
}

/** Closed v1 detail-code registry; each code is permanently tied to one category. */
enum class HermesV1FailureDetailCode(
    val wireValue: String,
    val category: HermesV1FailureCategory,
) {
    TRANSPORT_DISCONNECTED("TRANSPORT_DISCONNECTED", HermesV1FailureCategory.TRANSPORT),
    AUTHENTICATION_FAILED("AUTHENTICATION_FAILED", HermesV1FailureCategory.AUTHORIZATION),
    UNKNOWN_PRINCIPAL("UNKNOWN_PRINCIPAL", HermesV1FailureCategory.AUTHORIZATION),
    CAPABILITY_NOT_ALLOWED("CAPABILITY_NOT_ALLOWED", HermesV1FailureCategory.AUTHORIZATION),
    METHOD_NOT_AUTHORIZED("METHOD_NOT_AUTHORIZED", HermesV1FailureCategory.AUTHORIZATION),
    DISABLED_CAPABILITY("DISABLED_CAPABILITY", HermesV1FailureCategory.AUTHORIZATION),
    POLICY_MALFORMED("POLICY_MALFORMED", HermesV1FailureCategory.AUTHORIZATION),
    POLICY_EMPTY("POLICY_EMPTY", HermesV1FailureCategory.AUTHORIZATION),
    SOURCE_HASH_MISMATCH("SOURCE_HASH_MISMATCH", HermesV1FailureCategory.INTEGRITY),
    SOURCE_SIZE_MISMATCH("SOURCE_SIZE_MISMATCH", HermesV1FailureCategory.INTEGRITY),
    SOURCE_STREAM_FAILED("SOURCE_STREAM_FAILED", HermesV1FailureCategory.TRANSPORT),
    SOURCE_WRITE_FAILED("SOURCE_WRITE_FAILED", HermesV1FailureCategory.INTERNAL),
    MALFORMED_FRAME("MALFORMED_FRAME", HermesV1FailureCategory.MALFORMED),
    UNSUPPORTED_MEDIA_TYPE("UNSUPPORTED_MEDIA_TYPE", HermesV1FailureCategory.UNSUPPORTED),
    UNSUPPORTED_PROTOCOL_VERSION("UNSUPPORTED_PROTOCOL_VERSION", HermesV1FailureCategory.UNSUPPORTED),
    UNSUPPORTED_METHOD("UNSUPPORTED_METHOD", HermesV1FailureCategory.UNSUPPORTED),
    MALFORMED_JSON("MALFORMED_JSON", HermesV1FailureCategory.MALFORMED),
    INVALID_UTF8("INVALID_UTF8", HermesV1FailureCategory.MALFORMED),
    DUPLICATE_FIELD("DUPLICATE_FIELD", HermesV1FailureCategory.MALFORMED),
    UNKNOWN_FIELD("UNKNOWN_FIELD", HermesV1FailureCategory.MALFORMED),
    INVALID_FIELD("INVALID_FIELD", HermesV1FailureCategory.MALFORMED),
    INVALID_SOURCE_LENGTH("INVALID_SOURCE_LENGTH", HermesV1FailureCategory.MALFORMED),
    SHORT_METADATA_LENGTH_READ("SHORT_METADATA_LENGTH_READ", HermesV1FailureCategory.TRANSPORT),
    SHORT_METADATA_READ("SHORT_METADATA_READ", HermesV1FailureCategory.TRANSPORT),
    SHORT_SOURCE_READ("SHORT_SOURCE_READ", HermesV1FailureCategory.TRANSPORT),
    TRAILING_DATA("TRAILING_DATA", HermesV1FailureCategory.MALFORMED),
    ENVELOPE_TOO_LARGE("ENVELOPE_TOO_LARGE", HermesV1FailureCategory.RESOURCE_LIMIT),
    SOURCE_TOO_LARGE("SOURCE_TOO_LARGE", HermesV1FailureCategory.RESOURCE_LIMIT),
    OUTPUT_TOO_LARGE("OUTPUT_TOO_LARGE", HermesV1FailureCategory.RESOURCE_LIMIT),
    RESPONSE_TOO_LARGE("RESPONSE_TOO_LARGE", HermesV1FailureCategory.RESOURCE_LIMIT),
    PROCESSING_DEADLINE_EXCEEDED("PROCESSING_DEADLINE_EXCEEDED", HermesV1FailureCategory.TIMEOUT),
    PROCESSOR_FAILED("PROCESSOR_FAILED", HermesV1FailureCategory.PROCESSOR),
    INTERNAL_STORAGE_FAILURE("INTERNAL_STORAGE_FAILURE", HermesV1FailureCategory.INTERNAL),
    INTERNAL_UNEXPECTED_FAILURE("INTERNAL_UNEXPECTED_FAILURE", HermesV1FailureCategory.INTERNAL),
    REQUEST_IDENTITY_CONFLICT("REQUEST_IDENTITY_CONFLICT", HermesV1FailureCategory.MALFORMED),
    LEDGER_CORRUPT("LEDGER_CORRUPT", HermesV1FailureCategory.INTERNAL),
    LEDGER_SCHEMA_MISMATCH("LEDGER_SCHEMA_MISMATCH", HermesV1FailureCategory.INTERNAL),
    INVALID_STATE_TRANSITION("INVALID_STATE_TRANSITION", HermesV1FailureCategory.INTERNAL),
    RESULT_TOO_LARGE("RESULT_TOO_LARGE", HermesV1FailureCategory.RESOURCE_LIMIT),
    LEDGER_IO_FAILURE("LEDGER_IO_FAILURE", HermesV1FailureCategory.INTERNAL),
    METHOD_SOURCE_MISMATCH("METHOD_SOURCE_MISMATCH", HermesV1FailureCategory.UNSUPPORTED),
    PROCESSOR_TIMEOUT("PROCESSOR_TIMEOUT", HermesV1FailureCategory.TIMEOUT),
    MALFORMED_PROCESSOR_RESULT("MALFORMED_PROCESSOR_RESULT", HermesV1FailureCategory.PROCESSOR),
    TEXT_TOO_LARGE("TEXT_TOO_LARGE", HermesV1FailureCategory.RESOURCE_LIMIT),
    STRUCTURED_RESULT_TOO_LARGE("STRUCTURED_RESULT_TOO_LARGE", HermesV1FailureCategory.RESOURCE_LIMIT),
    REPRESENTATION_COUNT_EXCEEDED("REPRESENTATION_COUNT_EXCEEDED", HermesV1FailureCategory.RESOURCE_LIMIT),
    ISSUE_COUNT_EXCEEDED("ISSUE_COUNT_EXCEEDED", HermesV1FailureCategory.RESOURCE_LIMIT),
    PROCESSOR_UNAVAILABLE("PROCESSOR_UNAVAILABLE", HermesV1FailureCategory.PROCESSOR),
    ;

    companion object {
        fun fromWireValue(value: String): HermesV1FailureDetailCode = entries.firstOrNull { it.wireValue == value }
            ?: throw IllegalArgumentException("unknown Hermes v1 failure detail code: $value")
    }
}

data class HermesV1Issue(
    val code: HermesV1IssueCode,
    val explanation: String,
) {
    init { requireBoundedText(explanation, "issue explanation") }
}

data class HermesV1Failure(
    val category: HermesV1FailureCategory,
    val detailCode: HermesV1FailureDetailCode,
    val retryable: Boolean,
    val detail: String,
) {
    init {
        require(detailCode.category == category) { "failure detail code does not belong to category" }
        requireBoundedText(detail, "failure detail")
    }
}

/** The existing status model remains authoritative for processing semantics. */
object HermesV1StatusRegistry {
    val values: Set<String> = HermesProcessingStatus.entries.map { it.name }.toSet()

    fun fromWireValue(value: String): HermesProcessingStatus = try {
        HermesProcessingStatus.valueOf(value)
    } catch (_: IllegalArgumentException) {
        throw IllegalArgumentException("unknown Hermes v1 status: $value")
    }
}

data class HermesV1ProcessorIdentity(
    val name: String,
    val version: String,
) {
    init {
        requireOpaqueIdentifier(name, "processor name")
        requireOpaqueIdentifier(version, "processor version")
    }
}

data class HermesV1MethodProvenance(
    val method: HermesV1ProcessingMethod,
    val startedAt: Instant,
    val completedAt: Instant,
    val configurationDigest: HermesV1Sha256? = null,
) {
    init { require(!completedAt.isBefore(startedAt)) { "method completion must not precede start" } }
}

data class HermesV1Provenance(
    val sourceSha256: HermesV1Sha256,
    val processor: HermesV1ProcessorIdentity,
    val operations: List<HermesV1MethodProvenance>,
    val provenanceDigest: HermesV1Sha256? = null,
) {
    init {
        require(operations.isNotEmpty()) { "provenance must contain at least one operation" }
        require(operations.size <= HermesProcessingServiceV1Limits.MAX_METHODS) {
            "provenance contains too many operations"
        }
    }
}

data class HermesV1RepresentationDescriptor(
    val representationId: HermesV1RepresentationId,
    val type: HermesV1RepresentationType,
    val method: HermesV1ProcessingMethod,
    val content: HermesStructuredRepresentation? = null,
)

/**
 * Unit 1 service envelope. [establishedResult] is composed unchanged; the v1
 * envelope owns protocol/correlation/provenance and typed representation metadata.
 */
data class HermesProcessingServiceV1Response(
    val protocolVersion: HermesV1ProtocolVersion,
    val requestId: HermesV1RequestId,
    val jobId: HermesV1JobId,
    val occurrenceId: HermesV1OccurrenceId,
    val batchId: HermesV1BatchId,
    val sourceSha256: HermesV1Sha256,
    val establishedResult: HermesProcessingResult,
    val representations: List<HermesV1RepresentationDescriptor>,
    val issues: List<HermesV1Issue>,
    val failure: HermesV1Failure?,
    val provenance: HermesV1Provenance,
) {
    init {
        require(establishedResult.sourceSha256 == sourceSha256.value) { "envelope and established result source hashes differ" }
        require(establishedResult.batchId == batchId.value) { "envelope and established result batch IDs differ" }
        require(representations.size <= HermesProcessingServiceV1Limits.MAX_REPRESENTATIONS) {
            "too many representations"
        }
        require(issues.size <= HermesProcessingServiceV1Limits.MAX_ISSUES) { "too many issues" }
        require((establishedResult.status == HermesProcessingStatus.FAILED) == (failure != null)) {
            "v1 failure presence must match established FAILED status"
        }
    }
}

private fun requireOpaqueIdentifier(value: String, field: String) {
    require(value.isNotBlank() && value.length <= HermesProcessingServiceV1Limits.MAX_FAILURE_DETAIL_CHARACTERS) {
        "$field must be non-blank and bounded"
    }
    require(value.none { it.isWhitespace() || it.isISOControl() }) {
        "$field must not contain whitespace or control characters"
    }
}

private fun requireBoundedText(value: String, field: String) {
    require(value.isNotBlank() && value.length <= HermesProcessingServiceV1Limits.MAX_FAILURE_DETAIL_CHARACTERS) {
        "$field must be non-blank and bounded"
    }
}
