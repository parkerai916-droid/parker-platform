package parker.core.interfaces

import kotlin.math.sqrt
import kotlin.reflect.full.declaredMemberProperties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import parker.composition.QmdRealEmbeddingFixtures

/**
 * Programme 3, Unit 9.7 Section 13/13.1 mechanism-selection spike -- QMD
 * candidate. DISPOSABLE spike evidence, not a production adapter: Unit 9.7.3
 * (the governed concrete `RelevanceMechanism` implementation) remains
 * unimplemented regardless of this spike's outcome; this file exists only to
 * gather the mandatory semantic-fitness evidence §13.1 requires, and is
 * expected to be removed, or superseded by Unit 9.7.3's own adapter and its
 * own tests, once this spike's evidence record is accepted.
 *
 * See `docs/reviews/PROGRAMME_3_UNIT_9_7_SECTION_13_MECHANISM_SELECTION_SPIKE_EVIDENCE_RECORD.md`
 * for the full account this file is one piece of, including the bounded
 * defect correction this file's own [MechanismFacingCandidate] type and the
 * `buildMechanismFacingCandidates` / `score` split below were introduced to
 * demonstrate: the original harness passed a combined
 * `{token, knowledgeId, content, vector}`-shaped object through the same
 * pipeline that computed the semantic score, even though `knowledgeId` was
 * never read by the arithmetic -- the mechanism-facing representation itself
 * must be structurally incapable of carrying a canonical Parker identifier,
 * not merely unobserved to use one.
 *
 * **Reuses QMD's own real, already-captured embedding-model output.**
 * [QmdRealEmbeddingFixtures] (`tests/composition/QmdRealEmbeddingFixtures.kt`
 * / `tests/composition/fixtures/qmd-real-embedding-vectors.json`) was
 * generated once, by hand, on the Windows development machine, against
 * QMD's real embedding model (`embeddinggemma-300M-Q8_0.gguf`, 768
 * dimensions), via QMD's own `llm.js` `embed()` path -- the same code path
 * `store.searchVector` uses in production. No live QMD model inference
 * occurs in this test, exactly as the adopted
 * `PROGRAMME_3_UNIT_9_7_BOUNDED_SEMANTIC_RELEVANCE_IMPLEMENTATION_PLAN.md`
 * §13.1 itself directs: "captured data, not live model inference, exactly as
 * the adopted Proposal's own evidence was produced."
 *
 * **Why cosine similarity is computed here in plain Kotlin, not through
 * QMD's own `store.searchVector`/`exactVecScanByHashSeq`.** The two are
 * mathematically identical (cosine similarity = 1 − cosine distance;
 * `sqlite-vec`'s `vec0` virtual table computes the same dot-product /
 * magnitude-ratio this file computes directly) -- but genuinely invoking
 * QMD's own store/vector-search code in this spike's Linux execution
 * environment was attempted first and found blocked: `sqlite-vec-linux-x64`
 * (the native `vec0` extension for this platform) is not installed --
 * `node_modules` carries only `sqlite-vec-windows-x64` -- and `node-llama-cpp`
 * carries only unresolved `*.moved.txt` placeholders for every platform,
 * including `_linux-x64.moved.txt`, with no network access available to fetch
 * a real binary for any of them. This is a genuine, disclosed environmental
 * limitation of this specific spike execution environment (mirroring the
 * git-push and Gradle-plugin-cache limitations already disclosed earlier in
 * this engagement), not a defect in QMD, in the captured evidence, or in the
 * arithmetic below. Bypassing the native extension changes nothing about the
 * real, already-captured model output or the resulting numbers; it only
 * avoids a native binary this specific environment cannot supply. See the
 * evidence record's Phase 3 / Phase 7 / Phase 11 sections for the full,
 * disclosed account, including the numeric cross-check against this exact
 * computation performed independently in Node.js.
 */
class RelevanceMechanismSpikeQmdCandidateTest {

    /**
     * Exactly what a `RelevanceMechanism` implementation may receive per
     * candidate: an opaque, request-scoped token and the minimum relevance
     * material this spike's computation needs (here, the candidate's
     * already-embedded vector). No canonical Parker identifier, content
     * string, or any other field is declared -- the type itself is
     * structurally incapable of carrying one, proven by this file's own
     * reflection-based structural test, below.
     */
    private data class MechanismFacingCandidate(
        val token: RelevanceCandidateToken,
        val vector: List<Double>,
    )

    private fun cosineSimilarity(a: List<Double>, b: List<Double>): Double {
        var dot = 0.0
        var normA = 0.0
        var normB = 0.0
        for (i in a.indices) {
            dot += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }
        return dot / (sqrt(normA) * sqrt(normB))
    }

    /**
     * Parker/evidence-side mapping only, from opaque token to the synthetic
     * fixture's own memory identifier -- used solely to (a) build each
     * [MechanismFacingCandidate]'s vector by looking it up in
     * [QmdRealEmbeddingFixtures], and (b) label results for human-readable
     * assertions/reporting below. This map, and the memory-identifier
     * strings it carries, are never passed into [score] -- the function
     * standing in for "the mechanism" -- and never appear in a
     * [RelevanceResult]. This is the corrected boundary: the earlier version
     * of this evidence threaded a combined
     * `{token, knowledgeId, content, vector}` object through the scoring
     * step itself; this map exists precisely so that no longer needs to
     * happen.
     */
    private val tokenToMemoryId: Map<RelevanceCandidateToken, String> =
        listOf("memory-1", "memory-2", "memory-3", "memory-4", "memory-5", "memory-6")
            .mapIndexed { index, id -> RelevanceCandidateToken("candidate-${index + 1}") to id }
            .toMap()

    /**
     * Parker-side step: assembles the closed candidate set [score] (the
     * mechanism) will receive. Reads [tokenToMemoryId] and
     * [QmdRealEmbeddingFixtures] to resolve each token's vector, but returns
     * only [MechanismFacingCandidate] values -- the memory-identifier string
     * used to perform the lookup does not survive into the returned list.
     */
    private fun buildMechanismFacingCandidates(): List<MechanismFacingCandidate> =
        tokenToMemoryId.map { (token, memoryId) ->
            MechanismFacingCandidate(token, QmdRealEmbeddingFixtures.memoryVectors.getValue(memoryId))
        }

    /**
     * Stands in for "the mechanism": receives only a query vector and
     * [MechanismFacingCandidate] values (opaque token + vector, nothing
     * else -- the type itself makes anything more architecturally
     * impossible to pass in), and returns only a [RelevanceResult] (an
     * ordering of the exact tokens supplied). Mirrors
     * [RelevanceMechanism.rank]'s own shape.
     */
    private fun score(queryVector: List<Double>, candidates: List<MechanismFacingCandidate>): RelevanceResult {
        val ranked = candidates
            .map { candidate -> candidate.token to cosineSimilarity(queryVector, candidate.vector) }
            .sortedByDescending { it.second }
            .map { it.first }
        return RelevanceResult(rankedTokens = ranked)
    }

    private fun rank(): RelevanceResult =
        score(QmdRealEmbeddingFixtures.paraphraseQueryVector, buildMechanismFacingCandidates())

    @Test
    fun `MechanismFacingCandidate declares only token and vector, nothing else -- no KnowledgeId or other canonical identifier can cross the mechanism boundary`() {
        val properties = MechanismFacingCandidate::class.declaredMemberProperties
        assertEquals(setOf("token", "vector"), properties.map { it.name }.toSet())
        properties.forEach { property ->
            assertTrue(
                property.returnType.classifier != KnowledgeId::class,
                "MechanismFacingCandidate must not carry a KnowledgeId-typed field",
            )
        }
    }

    @Test
    fun `QMD's real captured embeddings rank the emergency-vet target first among six genuine distractors -- positive semantic recall, ranking fidelity`() {
        val result = rank()
        assertEquals(
            listOf("candidate-1", "candidate-2", "candidate-3", "candidate-4", "candidate-5", "candidate-6"),
            result.rankedTokens.map { it.value },
        )
        // Reproduces the already-accepted governance figures exactly:
        // PROGRAMME_3_UNIT_9_7_BOUNDED_SEMANTIC_RELEVANCE_IMPLEMENTATION_PLAN.md
        // §13.1 and PROGRAMME_3_UNIT_9_SEMANTIC_RELEVANCE_SCOPE_LOCK_REVISION_PROPOSAL.md
        // §4 both cite "0.586 for the intended match vs. 0.414 for the
        // strongest distractor" -- this test's own computation over the same
        // captured vectors yields 0.5858... and 0.4137..., the same evidence,
        // not a fresh, unrelated experiment.
        val target = cosineSimilarity(QmdRealEmbeddingFixtures.paraphraseQueryVector, QmdRealEmbeddingFixtures.memoryVectors.getValue("memory-1"))
        val strongestDistractor = cosineSimilarity(QmdRealEmbeddingFixtures.paraphraseQueryVector, QmdRealEmbeddingFixtures.memoryVectors.getValue("memory-2"))
        assertTrue(target in 0.585..0.587, "expected ~0.586, was $target")
        assertTrue(strongestDistractor in 0.413..0.415, "expected ~0.414, was $strongestDistractor")
    }

    @Test
    fun `repeated computation over the same frozen captured vectors is bit-identical -- repetition stability and determinism`() {
        val first = rank()
        val second = rank()
        val third = rank()
        assertEquals(first, second)
        assertEquals(first, third)
    }

    @Test
    fun `the lexically-similar Harbour Pharmacy distractor is not treated as propositionally equivalent to the target -- proposition fidelity`() {
        val result = rank().rankedTokens.map { it.value }
        val targetPosition = result.indexOf("candidate-1")
        val harbourPharmacyPosition = result.indexOf("candidate-5")
        // memory-5 ("the owner's synthetic preferred pharmacy is Harbour
        // Pharmacy") shares the "Harbour" brand token with the target
        // ("...is Harbour Animal Clinic") but is propositionally distinct --
        // a pharmacy, not an animal clinic. QMD's real semantic embedding
        // ranks it fifth of six (score ~0.213), not elevated by the shared
        // brand token -- genuine semantic, not merely lexical, discrimination.
        assertTrue(harbourPharmacyPosition > targetPosition)
        assertTrue(harbourPharmacyPosition >= 4, "expected Harbour Pharmacy ranked no higher than 5th of 6, was position ${harbourPharmacyPosition + 1}")
    }

    @Test
    fun `only the Parker-supplied opaque tokens are ever returned, in the same closed set that was supplied`() {
        val result = rank()
        val supplied = tokenToMemoryId.keys
        assertEquals(supplied, result.rankedTokens.toSet())
    }
}
