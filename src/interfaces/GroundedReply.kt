package parker.core.interfaces

/** The exact claim-level classifications available in Grounded Reply R0. */
enum class GroundedPropositionClassification {
    SUPPORTED_FACT,
    INFERENCE,
    NOT_ESTABLISHED,
    HUMAN_REVIEW_REQUIRED,
}

/** Reasons that make a material proposition require human resolution. */
enum class GroundedReviewReason {
    CONFLICT,
    UNRESOLVED_AMBIGUITY,
    MALFORMED_GROUNDING,
    MATERIAL_UNCERTAINTY,
}

/** Exact typed fields that may be asserted without parsing natural language. */
enum class ExactStructuredClaimField {
    PAGE_NUMBER,
    SOURCE_SHA256,
}

/** Exact governed identities that may be asserted without parsing natural language. */
enum class ExactGovernedIdentity {
    EVIDENCE_ARTIFACT_ID,
    DERIVATIVE_GENERATION_ID,
    SOURCE_REGION_ID,
}

/**
 * An explicit, provider-independent semantic support assertion. Its
 * [claimText] is canonical and must be the enclosing proposition text;
 * Parker never infers this structure from arbitrary prose.
 */
sealed interface StructuredClaimSupport {
    val contextEntry: ReasoningContextEntry
    val claimText: String

    data class ExactSourceValue(
        override val contextEntry: ReasoningContextEntry,
        val value: String,
    ) : StructuredClaimSupport {
        override val claimText: String = "EXACT_SOURCE_VALUE:$value"
    }

    data class ExactStructuredValue(
        override val contextEntry: ReasoningContextEntry,
        val field: ExactStructuredClaimField,
        val value: String,
    ) : StructuredClaimSupport {
        override val claimText: String = "EXACT_STRUCTURED_VALUE:${field.name}:$value"
    }

    data class ExactIdentity(
        override val contextEntry: ReasoningContextEntry,
        val identity: ExactGovernedIdentity,
        val value: String,
    ) : StructuredClaimSupport {
        override val claimText: String = "EXACT_IDENTITY:${identity.name}:$value"
    }
}

/**
 * One material proposition in a structured grounded reply.
 *
 * References are actual [ReasoningContextEntry] values, not caller-created
 * evidence identifiers. [GroundedReply.fromContext] performs the separate
 * invocation binding check against the context supplied to cognition.
 */
data class GroundedProposition(
    val text: String,
    val classification: GroundedPropositionClassification,
    val supportReferences: List<ReasoningContextEntry> = emptyList(),
    val basisReferences: List<ReasoningContextEntry> = emptyList(),
    val structuredSupports: List<StructuredClaimSupport> = emptyList(),
    val reasoning: String? = null,
    val reviewReason: GroundedReviewReason? = null,
) {
    init {
        require(text.isNotBlank()) { "GroundedProposition.text must not be blank" }
        require(supportReferences.all { it !is ReasoningContextEntry.PlainText }) {
            "GroundedProposition supportReferences must be governed context entries"
        }
        require(basisReferences.all { it !is ReasoningContextEntry.PlainText }) {
            "GroundedProposition basisReferences must be governed context entries"
        }
        require(structuredSupports.all { it.contextEntry !is ReasoningContextEntry.PlainText }) {
            "GroundedProposition structuredSupports must be governed context entries"
        }
        require(structuredSupports.all { it.claimText == text }) {
            "GroundedProposition structured support must canonically describe the proposition text"
        }
        when (classification) {
            GroundedPropositionClassification.SUPPORTED_FACT -> {
                require(supportReferences.isNotEmpty()) {
                    "SUPPORTED_FACT requires at least one governed support reference"
                }
                require(basisReferences.isEmpty()) {
                    "SUPPORTED_FACT must not carry inference basis references"
                }
                require(reviewReason == null) {
                    "SUPPORTED_FACT must not carry a human-review reason"
                }
            }
            GroundedPropositionClassification.INFERENCE -> {
                require(supportReferences.isEmpty()) {
                    "INFERENCE must use basisReferences, not supportReferences"
                }
                require(basisReferences.isNotEmpty()) {
                    "INFERENCE requires at least one governed basis reference"
                }
                require(reviewReason == null) {
                    "INFERENCE must not carry a human-review reason"
                }
                require(structuredSupports.isEmpty()) {
                    "INFERENCE must not carry structured supported-fact assertions"
                }
            }
            GroundedPropositionClassification.NOT_ESTABLISHED -> {
                require(supportReferences.isEmpty() && basisReferences.isEmpty()) {
                    "NOT_ESTABLISHED must not carry support or basis references"
                }
                require(reviewReason == null) {
                    "NOT_ESTABLISHED must not carry a human-review reason"
                }
                require(structuredSupports.isEmpty()) {
                    "NOT_ESTABLISHED must not carry structured supported-fact assertions"
                }
            }
            GroundedPropositionClassification.HUMAN_REVIEW_REQUIRED -> {
                require(reviewReason != null) {
                    "HUMAN_REVIEW_REQUIRED requires a review reason"
                }
                require(structuredSupports.isEmpty()) {
                    "HUMAN_REVIEW_REQUIRED must not carry structured supported-fact assertions"
                }
            }
        }
        require(reasoning == null || reasoning.isNotBlank()) {
            "GroundedProposition.reasoning must not be blank when present"
        }
    }
}

/**
 * A non-durable structured grounded reply bound to one supplied reasoning
 * context. Construction is intentionally context-bound: a proposition may
 * reference only governed entries present in that context, and the provider
 * cannot enlarge the supplied set by returning prose or fabricated entries.
 */
data class GroundedReply private constructor(
    val propositions: List<GroundedProposition>,
) {
    init {
        require(propositions.isNotEmpty()) { "GroundedReply.propositions must not be empty" }
    }

    companion object {
        fun fromContext(
            context: ReasoningContext,
            propositions: List<GroundedProposition>,
        ): GroundedReply {
            val supplied = context.suppliedGovernedEntries.toSet()
            propositions.forEachIndexed { index, proposition ->
                require(proposition.supportReferences.all { it in supplied }) {
                    "GroundedProposition[$index] references a governed entry not supplied to cognition"
                }
                require(proposition.basisReferences.all { it in supplied }) {
                    "GroundedProposition[$index] cites an inference basis not supplied to cognition"
                }
                require(proposition.structuredSupports.all { it.contextEntry in supplied }) {
                    "GroundedProposition[$index] carries structured support not supplied to cognition"
                }
            }
            return GroundedReply(propositions.toList())
        }
    }
}
