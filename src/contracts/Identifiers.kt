package parker.core.interfaces

/**
 * Typed identifiers for Parker's core contracts (Volume 1). Plain value
 * classes (zero runtime overhead) rather than bare String everywhere, so
 * e.g. a PrincipalId can't be accidentally passed where a ResourceId is
 * expected.
 *
 * Blank values are rejected at construction -- an id that exists but is
 * empty is a bug, not a valid state.
 */
@JvmInline
value class PrincipalId(val value: String) {
    init {
        require(value.isNotBlank()) { "PrincipalId must not be blank" }
    }
}

@JvmInline
value class ResourceId(val value: String) {
    init {
        require(value.isNotBlank()) { "ResourceId must not be blank" }
    }
}

@JvmInline
value class RequestId(val value: String) {
    init {
        require(value.isNotBlank()) { "RequestId must not be blank" }
    }
}

@JvmInline
value class DecisionId(val value: String) {
    init {
        require(value.isNotBlank()) { "DecisionId must not be blank" }
    }
}

@JvmInline
value class ResultId(val value: String) {
    init {
        require(value.isNotBlank()) { "ResultId must not be blank" }
    }
}
