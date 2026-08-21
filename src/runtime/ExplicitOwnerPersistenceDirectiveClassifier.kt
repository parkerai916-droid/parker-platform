package parker.core.runtime

/** A proposition mechanically extracted from one governed Version 1 directive. */
data class ExplicitOwnerPersistenceDirective(val proposition: String) {
    init {
        require(proposition.isNotBlank()) { "proposition must not be blank" }
    }
}

/** Pure, deterministic recognition seam. It has no persistence or model reach. */
fun interface ExplicitOwnerPersistenceDirectiveClassifier {
    fun classify(text: String): ExplicitOwnerPersistenceDirective?
}

/**
 * Frozen Version 1 grammar from the Explicit Owner Persistence Directive
 * Recognition Scope Lock. Wrapper matching is case-insensitive. Extracted
 * proposition wording and case are preserved; only governed wrapper text, outer
 * whitespace, and at most one terminal period are removed.
 */
class DefaultExplicitOwnerPersistenceDirectiveClassifier :
    ExplicitOwnerPersistenceDirectiveClassifier {

    override fun classify(text: String): ExplicitOwnerPersistenceDirective? {
        val input = text.trim()
        if (input.isEmpty() || isExcluded(input)) return null

        val captured = POSITIVE_PATTERNS.firstNotNullOfOrNull { pattern ->
            pattern.matchEntire(input)?.groupValues?.get(1)
        } ?: return null

        val proposition = captured.trim().removeOneTerminalPeriod().trimEnd()
        if (proposition.isBlank()) return null
        return ExplicitOwnerPersistenceDirective(proposition)
    }

    private fun isExcluded(input: String): Boolean {
        val lower = input.lowercase()
        if (NEGATED_PREFIXES.any { lower.startsWith(it) }) return true
        if (QUESTION_PREFIXES.any { lower.startsWith(it) }) return true
        if (input.endsWith("?")) return true
        if (lower.matches(Regex("""^(?:please\s+)?remember\s+when\b.*"""))) return true
        return false
    }

    private fun String.removeOneTerminalPeriod(): String =
        if (endsWith(".")) dropLast(1) else this

    private companion object {
        val OPTIONS = setOf(RegexOption.IGNORE_CASE)

        val POSITIVE_PATTERNS = listOf(
            Regex("""^(?:please\s+)?remember(?:\s+that)?\s+(.+)$""", OPTIONS),
            Regex("""^(?:please\s+)?store\s+in\s+memory\s*:\s*(.+)$""", OPTIONS),
            Regex("""^(?:please\s+)?save\s+to\s+memory\s*:\s*(.+)$""", OPTIONS),
            Regex("""^(?:please\s+)?commit\s+to\s+memory\s*:\s*(.+)$""", OPTIONS),
            Regex("""^(?:please\s+)?commit\s+(.+?)\s+to\s+memory$""", OPTIONS),
            Regex("""^(?:please\s+)?keep(?:\s+this)?\s+in\s+memory\s*:\s*(.+)$""", OPTIONS),
            Regex("""^(?:please\s+)?keep\s+(.+?)\s+in\s+memory$""", OPTIONS),
        )

        val NEGATED_PREFIXES = listOf(
            "don't ",
            "don’t ",
            "do not ",
            "never ",
            "please don't ",
            "please don’t ",
            "please do not ",
            "please never ",
        )

        val QUESTION_PREFIXES = listOf(
            "do you ",
            "can you ",
            "could you ",
            "what do ",
            "what did ",
        )
    }
}
