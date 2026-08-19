package parker.core.interfaces

import kotlin.math.ln
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Programme 3, Unit 9.7 Section 13/13.1 mechanism-selection spike -- the
 * governed comparator: "a simpler, dependency-light, in-process JVM lexical
 * or embedding mechanism"
 * (`PROGRAMME_3_UNIT_9_7_BOUNDED_SEMANTIC_RELEVANCE_IMPLEMENTATION_PLAN.md`
 * §13). Classic TF-IDF cosine similarity over whitespace/punctuation-
 * tokenised, lowercased terms -- the standard, well-known "smallest
 * credible" lexical baseline, chosen deliberately because it is standard,
 * not because it is weak: the task's own instruction is "do not deliberately
 * cripple it merely to make QMD win." Zero external dependency beyond the
 * Kotlin standard library; inverse document frequency is computed only over
 * the six-candidate closed set a single `RelevanceRequest` supplies -- no
 * external corpus statistics, no persisted index, no network, no ML model.
 *
 * DISPOSABLE spike evidence, not a production adapter -- see this file's
 * QMD-candidate sibling test
 * (`RelevanceMechanismSpikeQmdCandidateTest.kt`) for the shared disclosure:
 * mechanism selection is this spike's own purpose; Unit 9.7.3 remains
 * unimplemented regardless of this spike's outcome, and this file is
 * expected to be removed once the spike's evidence record is accepted.
 */
class RelevanceMechanismSpikeJvmComparatorTest {

    private fun tokenize(text: String): List<String> =
        Regex("[a-z0-9]+").findAll(text.lowercase()).map { it.value }.toList()

    private fun idf(term: String, documentFrequency: Map<String, Int>, candidateCount: Int): Double =
        ln((candidateCount + 1).toDouble() / ((documentFrequency[term] ?: 0) + 1).toDouble()) + 1.0

    private fun tfidfVector(tokens: List<String>, documentFrequency: Map<String, Int>, candidateCount: Int): Map<String, Double> {
        val termFrequency = tokens.groupingBy { it }.eachCount()
        return termFrequency.mapValues { (term, frequency) -> frequency * idf(term, documentFrequency, candidateCount) }
    }

    private fun cosineSimilarity(a: Map<String, Double>, b: Map<String, Double>): Double {
        var dot = 0.0
        for ((term, weight) in a) {
            val otherWeight = b[term] ?: continue
            dot += weight * otherWeight
        }
        val normA = sqrt(a.values.sumOf { it * it })
        val normB = sqrt(b.values.sumOf { it * it })
        if (normA == 0.0 || normB == 0.0) return 0.0
        return dot / (normA * normB)
    }

    private val candidateContents = listOf(
        "candidate-1" to "the owner's synthetic emergency vet is Harbour Animal Clinic",
        "candidate-2" to "the owner's synthetic regular vet is Riverside Veterinary Centre",
        "candidate-3" to "the owner's synthetic dog groomer is Central City Grooming",
        "candidate-4" to "the owner's synthetic emergency plumber is Wellington Rapid Plumbing",
        "candidate-5" to "the owner's synthetic preferred pharmacy is Harbour Pharmacy",
        "candidate-6" to "the owner's synthetic favourite hiking trail is Widow's Peak Ridge",
    )

    private val queryText = "Which animal clinic did I tell you to use in an emergency?"

    /** RelevanceRequest-shaped input in, RelevanceResult-shaped output out -- see this file's QMD-candidate sibling. */
    private fun rank(): RelevanceResult {
        val tokenizedCandidates = candidateContents.map { (token, content) -> token to tokenize(content) }
        val documentFrequency = mutableMapOf<String, Int>()
        for ((_, tokens) in tokenizedCandidates) {
            for (term in tokens.toSet()) {
                documentFrequency[term] = (documentFrequency[term] ?: 0) + 1
            }
        }
        val candidateCount = tokenizedCandidates.size
        val queryVector = tfidfVector(tokenize(queryText), documentFrequency, candidateCount)
        val ranked = tokenizedCandidates
            .map { (token, tokens) -> token to cosineSimilarity(queryVector, tfidfVector(tokens, documentFrequency, candidateCount)) }
            .sortedByDescending { it.second }
            .map { RelevanceCandidateToken(it.first) }
        return RelevanceResult(rankedTokens = ranked)
    }

    @Test
    fun `TF-IDF cosine similarity ranks the emergency-vet target first -- positive semantic recall`() {
        val result = rank()
        assertEquals("candidate-1", result.rankedTokens.first().value)
    }

    @Test
    fun `repeated computation over the same frozen candidate set is deterministic`() {
        assertEquals(rank(), rank())
    }

    @Test
    fun `the emergency-plumber distractor -- propositionally unrelated -- outranks the genuinely related regular-vet distractor, a proposition-fidelity failure`() {
        val ranked = rank().rankedTokens.map { it.value }
        val plumberPosition = ranked.indexOf("candidate-4")
        val regularVetPosition = ranked.indexOf("candidate-2")
        // This is the disqualifying finding this spike's evidence record
        // reports for this candidate: a purely lexical mechanism ranks
        // "emergency plumber" (candidate-4) above "regular vet" (candidate-2)
        // for a query about an emergency animal clinic, solely because
        // "emergency" is a token both share. TF-IDF has no representation of
        // a vet being a closer match to an animal clinic than a plumber is --
        // only of shared surface tokens. This is exactly mandatory criterion
        // 4 (proposition fidelity) failing under a fair, non-crippled
        // implementation of the governed simpler comparator: candidate-2
        // scores exactly 0.0 (zero token overlap with the query at all) while
        // candidate-4 scores above zero purely on the strength of one
        // incidentally shared word.
        assertTrue(plumberPosition < regularVetPosition, "expected the plumber distractor NOT to outrank the regular-vet distractor, but it did (plumber at $plumberPosition, regular vet at $regularVetPosition) -- this is the recorded proposition-fidelity failure")
    }

    @Test
    fun `only the Parker-supplied opaque tokens are ever returned, in the same closed set that was supplied`() {
        val result = rank()
        val supplied = candidateContents.map { RelevanceCandidateToken(it.first) }.toSet()
        assertEquals(supplied, result.rankedTokens.toSet())
    }
}
