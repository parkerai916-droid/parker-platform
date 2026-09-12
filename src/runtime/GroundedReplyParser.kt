package parker.core.runtime

import java.nio.charset.StandardCharsets
import java.util.Base64
import parker.core.interfaces.GroundedProposition
import parker.core.interfaces.GroundedPropositionClassification
import parker.core.interfaces.GroundedReply
import parker.core.interfaces.GroundedReviewReason
import parker.core.interfaces.ExactGovernedIdentity
import parker.core.interfaces.ExactStructuredClaimField
import parker.core.interfaces.ReasoningContext
import parker.core.interfaces.ReasoningContextEntry
import parker.core.interfaces.StructuredClaimSupport

/**
 * Deterministic R0 wire codec for the separate structured grounded-reply
 * path. Ordinary [TaggedReasoningResponseParser] behavior is untouched.
 *
 * The provider returns only context-entry indexes; this parser resolves them
 * to the already-supplied structural entries and fails closed on malformed
 * classes, indexes, or invariant combinations. It never creates a governed
 * context entry.
 */
class GroundedReplyParser {

    fun parse(raw: String, context: ReasoningContext): GroundedReply {
        val lines = raw.split('\n')
        require(lines.firstOrNull() == HEADER) { "Grounded reply must begin with $HEADER" }
        require(lines.size > 1) { "Grounded reply must contain at least one proposition" }

        val propositions = lines.drop(1).mapIndexed { lineIndex, line ->
            val fields = line.split('\t')
            require(fields.size == FIELD_COUNT) {
                "Grounded reply proposition line ${lineIndex + 1} has the wrong field count"
            }
            require(fields[0] == PROPOSITION_TAG) { "Unknown grounded reply record at line ${lineIndex + 1}" }

            val classification = enumValueOrFail<GroundedPropositionClassification>(fields[1], "classification")
            val text = decode(fields[2], "text")
            val support = resolveIndexes(fields[3], context, lineIndex, "support")
            val basis = resolveIndexes(fields[4], context, lineIndex, "basis")
            val reasoning = decodeOptional(fields[5], "reasoning")
            val structuredSupports = resolveStructuredSupports(fields[6], context, lineIndex)
            val reviewReason = when (classification) {
                GroundedPropositionClassification.HUMAN_REVIEW_REQUIRED -> {
                    val encodedReason = reasoning ?: throw IllegalArgumentException(
                        "HUMAN_REVIEW_REQUIRED proposition ${lineIndex + 1} requires a review reason",
                    )
                    enumValueOrFail<GroundedReviewReason>(encodedReason, "review reason")
                }
                else -> null
            }

            GroundedProposition(
                text = text,
                classification = classification,
                supportReferences = support,
                basisReferences = basis,
                structuredSupports = structuredSupports,
                reasoning = if (classification == GroundedPropositionClassification.HUMAN_REVIEW_REQUIRED) null else reasoning,
                reviewReason = reviewReason,
            )
        }

        return GroundedReply.fromContext(context, propositions)
    }

    /** Encodes the bound reply using indexes into the same supplied context. */
    fun encode(reply: GroundedReply, context: ReasoningContext): String {
        // Indexes are positions in typedEntries, which is also the index
        // space parse() resolves. Plain-text entries remain unreferencable;
        // they simply occupy their structural positions.
        val supplied = context.typedEntries
        return buildString {
            append(HEADER)
            reply.propositions.forEach { proposition ->
                append('\n').append(PROPOSITION_TAG).append('\t')
                    .append(proposition.classification.name).append('\t')
                    .append(encode(proposition.text)).append('\t')
                    .append(indexes(proposition.supportReferences, supplied)).append('\t')
                    .append(indexes(proposition.basisReferences, supplied)).append('\t')
                    .append(
                        when {
                            proposition.classification == GroundedPropositionClassification.HUMAN_REVIEW_REQUIRED ->
                                encode(proposition.reviewReason!!.name)
                            else -> encodeOptional(proposition.reasoning)
                        },
                    ).append('\t').append(structuredSupports(proposition.structuredSupports, supplied))
            }
        }
    }

    private fun resolveStructuredSupports(
        encoded: String,
        context: ReasoningContext,
        lineIndex: Int,
    ): List<StructuredClaimSupport> {
        if (encoded == EMPTY) return emptyList()
        return encoded.split(';').map { token ->
            val fields = token.split(',')
            require(fields.size == 4) { "Malformed structured support at line ${lineIndex + 1}" }
            val entryIndex = fields[1].toIntOrNull()
                ?: throw IllegalArgumentException("Invalid structured support context index at line ${lineIndex + 1}")
            require(entryIndex in context.typedEntries.indices)
            val entry = context.typedEntries[entryIndex]
            require(entry !is ReasoningContextEntry.PlainText)
            val value = decode(fields[3], "structured support value")
            when (fields[0]) {
                "SOURCE" -> StructuredClaimSupport.ExactSourceValue(entry, value)
                "STRUCTURED" -> StructuredClaimSupport.ExactStructuredValue(
                    entry,
                    enumValueOrFail<ExactStructuredClaimField>(fields[2], "structured support field"),
                    value,
                )
                "IDENTITY" -> StructuredClaimSupport.ExactIdentity(
                    entry,
                    enumValueOrFail<ExactGovernedIdentity>(fields[2], "structured support identity"),
                    value,
                )
                else -> throw IllegalArgumentException("Unknown structured support kind '${fields[0]}'")
            }
        }
    }

    private fun resolveIndexes(
        encoded: String,
        context: ReasoningContext,
        lineIndex: Int,
        field: String,
    ): List<ReasoningContextEntry> {
        if (encoded == EMPTY) return emptyList()
        return encoded.split(',').map { token ->
            val index = token.toIntOrNull()
                ?: throw IllegalArgumentException("Invalid $field context index at line ${lineIndex + 1}")
            require(index >= 0 && index < context.typedEntries.size) {
                "${field.replaceFirstChar { it.uppercase() }} context index $index is not supplied"
            }
            val entry = context.typedEntries[index]
            require(entry !is ReasoningContextEntry.PlainText) {
                "${field.replaceFirstChar { it.uppercase() }} context index $index is not governed"
            }
            entry
        }
    }

    private fun indexes(
        references: List<ReasoningContextEntry>,
        supplied: List<ReasoningContextEntry>,
    ): String {
        if (references.isEmpty()) return EMPTY
        return references.joinToString(",") { reference ->
            val index = supplied.indexOf(reference)
            require(index >= 0) { "Grounded reply reference is not present in supplied context" }
            index.toString()
        }
    }

    private fun structuredSupports(
        supports: List<StructuredClaimSupport>,
        supplied: List<ReasoningContextEntry>,
    ): String = supports.joinToString(";") { support ->
        val index = supplied.indexOf(support.contextEntry)
        require(index >= 0) { "Structured support is not present in supplied context" }
        val (kind, field) = when (support) {
            is StructuredClaimSupport.ExactSourceValue -> "SOURCE" to "-"
            is StructuredClaimSupport.ExactStructuredValue -> "STRUCTURED" to support.field.name
            is StructuredClaimSupport.ExactIdentity -> "IDENTITY" to support.identity.name
        }
        "$kind,$index,$field,${encode(supportValue(support))}"
    }.ifEmpty { EMPTY }

    private fun supportValue(support: StructuredClaimSupport): String = when (support) {
        is StructuredClaimSupport.ExactSourceValue -> support.value
        is StructuredClaimSupport.ExactStructuredValue -> support.value
        is StructuredClaimSupport.ExactIdentity -> support.value
    }

    private fun encode(value: String): String = Base64.getEncoder().encodeToString(value.toByteArray(StandardCharsets.UTF_8))

    private fun encodeOptional(value: String?): String = value?.let(::encode) ?: EMPTY

    private fun decode(value: String, field: String): String {
        require(value != EMPTY) { "Grounded reply $field must not be empty" }
        return try {
            String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8)
        } catch (error: IllegalArgumentException) {
            throw IllegalArgumentException("Grounded reply $field is not valid base64", error)
        }
    }

    private fun decodeOptional(value: String, field: String): String? = if (value == EMPTY) null else decode(value, field)

    private inline fun <reified T : Enum<T>> enumValueOrFail(value: String, field: String): T =
        try {
            enumValueOf<T>(value)
        } catch (error: IllegalArgumentException) {
            throw IllegalArgumentException("Unknown grounded reply $field '$value'", error)
        }

    private companion object {
        const val HEADER = "GROUNDED_REPLY_R0"
        const val PROPOSITION_TAG = "P"
        const val FIELD_COUNT = 7
        const val EMPTY = "-"
    }
}
