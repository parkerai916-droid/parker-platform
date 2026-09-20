package parker.core.interfaces

/**
 * Identity established by the authenticated SSH forced-command boundary.
 * This is a service principal, not a Parker user, case, or request field.
 */
@JvmInline
value class HermesV1ProcessingPrincipal(val value: String) {
    init {
        require(value.isNotBlank() && value.length <= HermesProcessingServiceV1Limits.MAX_FAILURE_DETAIL_CHARACTERS) {
            "processing principal must be non-blank and bounded"
        }
        require(value.none { it.isWhitespace() || it.isISOControl() }) {
            "processing principal must not contain whitespace or control characters"
        }
    }

    companion object {
        /** The dedicated principal bound externally by the SSH forced-command deployment. */
        val PARKER_PROCESSING = HermesV1ProcessingPrincipal("parker-hermes-processing")
    }
}

/** Closed capability vocabulary. Registry membership is not proof of authorization. */
enum class HermesV1ProcessingCapability(
    val wireValue: String,
    val enabled: Boolean,
) {
    NATIVE_TEXT_EXTRACTION("NATIVE_TEXT_EXTRACTION", true),
    STRUCTURED_DOCUMENT_EXTRACTION("STRUCTURED_DOCUMENT_EXTRACTION", true),
    STRUCTURED_SPREADSHEET_EXTRACTION("STRUCTURED_SPREADSHEET_EXTRACTION", true),
    STRUCTURED_EMAIL_EXTRACTION("STRUCTURED_EMAIL_EXTRACTION", true),
    OCR("OCR", false),
    TRANSCRIPTION("TRANSCRIPTION", false),
    ;

    companion object {
        fun fromWireValue(value: String): HermesV1ProcessingCapability = entries.firstOrNull { it.wireValue == value }
            ?: throw IllegalArgumentException("unknown Hermes v1 processing capability: $value")
    }
}

/**
 * Hermes-owned immutable principal policy. Wire loading is fail-closed: any
 * malformed principal/capability entry produces a deny-all policy.
 */
class HermesV1CapabilityPolicy private constructor(
    private val grants: Map<HermesV1ProcessingPrincipal, Set<HermesV1ProcessingCapability>>,
    val loadFailure: HermesV1FailureDetailCode? = null,
) {
    init {
        require(loadFailure == null || grants.isEmpty()) { "failed policy must not retain grants" }
    }

    fun capabilitiesFor(principal: HermesV1ProcessingPrincipal): Set<HermesV1ProcessingCapability> =
        grants[principal] ?: emptySet()

    fun containsPrincipal(principal: HermesV1ProcessingPrincipal): Boolean = grants.containsKey(principal)

    fun containsAnyPrincipal(): Boolean = grants.isNotEmpty()

    companion object {
        fun initial(): HermesV1CapabilityPolicy = fromTypedGrants(
            mapOf(
                HermesV1ProcessingPrincipal.PARKER_PROCESSING to HermesV1ProcessingCapability.entries.filter { it.enabled }.toSet(),
            ),
        )

        fun empty(): HermesV1CapabilityPolicy = fromTypedGrants(emptyMap())

        /** Constructs a typed immutable policy; callers cannot mutate the stored collections. */
        fun fromTypedGrants(
            grants: Map<HermesV1ProcessingPrincipal, Set<HermesV1ProcessingCapability>>,
        ): HermesV1CapabilityPolicy = HermesV1CapabilityPolicy(
            grants = grants.mapValues { (_, capabilities) -> capabilities.toSet() }.toMap(),
        )

        /** Decodes only the closed wire vocabulary; malformed input becomes deny-all. */
        fun fromWireGrants(grants: Map<String, List<String>>): HermesV1CapabilityPolicy = try {
            val typed = grants.map { (principal, capabilities) ->
                val typedPrincipal = HermesV1ProcessingPrincipal(principal)
                require(capabilities.distinct().size == capabilities.size) { "duplicate capability" }
                typedPrincipal to capabilities.map { HermesV1ProcessingCapability.fromWireValue(it) }.toSet()
            }.toMap()
            fromTypedGrants(typed)
        } catch (_: IllegalArgumentException) {
            HermesV1CapabilityPolicy(emptyMap(), HermesV1FailureDetailCode.POLICY_MALFORMED)
        }
    }
}

/** Exact closed mapping from registered v1 methods to the capability they require. */
object HermesV1MethodCapabilityRegistry {
    val requiredCapabilities: Map<HermesV1ProcessingMethod, HermesV1ProcessingCapability> = mapOf(
        HermesV1ProcessingMethod.DIRECT_TEXT_EXTRACTION to HermesV1ProcessingCapability.NATIVE_TEXT_EXTRACTION,
        HermesV1ProcessingMethod.STRUCTURED_DOCUMENT_EXTRACTION to HermesV1ProcessingCapability.STRUCTURED_DOCUMENT_EXTRACTION,
        HermesV1ProcessingMethod.STRUCTURED_SPREADSHEET_EXTRACTION to HermesV1ProcessingCapability.STRUCTURED_SPREADSHEET_EXTRACTION,
        HermesV1ProcessingMethod.STRUCTURED_EMAIL_EXTRACTION to HermesV1ProcessingCapability.STRUCTURED_EMAIL_EXTRACTION,
    ).toMap()

    fun requiredCapability(method: HermesV1ProcessingMethod): HermesV1ProcessingCapability? =
        requiredCapabilities[method]
}

sealed interface HermesV1AuthorizationResult {
    data object Authorized : HermesV1AuthorizationResult
    data class Denied(val failure: HermesV1Failure) : HermesV1AuthorizationResult
}

/**
 * Hermes-side capability enforcement. The principal must be supplied by the
 * authenticated transport seam; no request metadata participates in identity.
 */
class HermesV1CapabilityAuthorizer private constructor(
    private val policy: HermesV1CapabilityPolicy,
    private val methodCapabilities: Map<HermesV1ProcessingMethod, HermesV1ProcessingCapability>,
) {
    constructor(policy: HermesV1CapabilityPolicy) : this(policy, HermesV1MethodCapabilityRegistry.requiredCapabilities)

    init {
        require(methodCapabilities.keys.all { it in HermesV1ProcessingMethod.entries }) {
            "method capability policy contains an unknown method"
        }
    }

    fun authorize(principal: HermesV1ProcessingPrincipal, method: HermesV1ProcessingMethod): HermesV1AuthorizationResult {
        if (policy.loadFailure != null) return denied(policy.loadFailure)
        if (!policy.containsAnyPrincipal()) return denied(HermesV1FailureDetailCode.POLICY_EMPTY)
        if (!policy.containsPrincipal(principal)) return denied(HermesV1FailureDetailCode.UNKNOWN_PRINCIPAL)
        val capability = methodCapabilities[method]
            ?: return denied(HermesV1FailureDetailCode.METHOD_NOT_AUTHORIZED)
        if (!capability.enabled) {
            return denied(HermesV1FailureDetailCode.DISABLED_CAPABILITY)
        }
        if (capability !in policy.capabilitiesFor(principal)) {
            return denied(HermesV1FailureDetailCode.CAPABILITY_NOT_ALLOWED)
        }
        return HermesV1AuthorizationResult.Authorized
    }

    fun authorize(
        principal: HermesV1ProcessingPrincipal,
        request: HermesProcessingServiceV1Request,
    ): HermesV1AuthorizationResult {
        if (request.requestedMethods.isEmpty()) return denied(HermesV1FailureDetailCode.METHOD_NOT_AUTHORIZED)
        request.requestedMethods.forEach { method ->
            val result = authorize(principal, method)
            if (result is HermesV1AuthorizationResult.Denied) return result
        }
        return HermesV1AuthorizationResult.Authorized
    }

    private fun denied(code: HermesV1FailureDetailCode): HermesV1AuthorizationResult.Denied =
        HermesV1AuthorizationResult.Denied(
            HermesV1Failure(
                category = HermesV1FailureCategory.AUTHORIZATION,
                detailCode = code,
                retryable = false,
                detail = "Hermes processing authorization denied",
            ),
        )

    companion object {
        /** Test-only seam proving a missing closed mapping cannot become an implicit allow. */
        internal fun forTesting(
            policy: HermesV1CapabilityPolicy,
            methodCapabilities: Map<HermesV1ProcessingMethod, HermesV1ProcessingCapability>,
        ): HermesV1CapabilityAuthorizer = HermesV1CapabilityAuthorizer(policy, methodCapabilities.toMap())
    }
}
