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

/**
 * Trust Framework Authorization Purpose (`docs/architecture/AUTHORIZATION_PURPOSE_SCOPE_LOCK.md`
 * §2.2): a distinct, closed value type, never a raw `String` directly on
 * `ExecutionRequest`. A registered vocabulary value, not a freely-assigned
 * identifier -- content/naming-structure validation is Vocabulary Registry
 * territory (Scope Lock §2.3), not this type's own concern; this
 * constructor enforces only the same non-blank floor every sibling type
 * in this file already does.
 *
 * Note for whichever future unit designs the Vocabulary Registry: `EventType`
 * (`src/contracts/EventContracts.kt`) is a closer structural analogue than
 * `PrincipalId`/`ResourceId` above -- also a namespaced, closed-vocabulary-style
 * value, not a freely-assigned instance identifier -- and it embeds its own
 * namespace-structure validation directly in its constructor
 * (`require('.' in value)`), rather than deferring it to a separate registry.
 * This type deliberately does not follow that pattern, per the Authorization
 * Purpose Implementation Plan's own explicit stop condition reserving naming
 * validation to the registry unit; recorded here so that choice is weighed
 * deliberately, not rediscovered from scratch, when that unit is designed.
 */
@JvmInline
value class AuthorizationPurposeId(val value: String) {
    init {
        require(value.isNotBlank()) { "AuthorizationPurposeId must not be blank" }
    }
}
