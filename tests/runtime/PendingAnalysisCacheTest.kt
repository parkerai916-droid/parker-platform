package parker.core.runtime

import java.time.Duration
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlinx.coroutines.test.runTest
import parker.core.interfaces.AnalysisEvidenceItem
import parker.core.interfaces.DerivativeCompletenessState
import parker.core.interfaces.DerivativeContentIdentity
import parker.core.interfaces.DerivativeGenerationId
import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.OwnerDocumentAnalysisResult
import parker.core.interfaces.PendingAnalysisId
import parker.core.interfaces.TierADerivativePayloadFixtures

/**
 * Reviewed Analysis Result — Explicit Owner Save. Behavioural tests for
 * [PendingAnalysisCache]: the entire anti-forgery mechanism (a Save request
 * only ever carries an opaque id, never analysis content), one-shot/
 * concurrency-safe claim semantics, TTL expiry, and bounded eviction.
 */
class PendingAnalysisCacheTest {

    private fun result(analysisText: String = "some analysis text") = OwnerDocumentAnalysisResult(
        analysisText = analysisText,
        evidenceItems = listOf(
            AnalysisEvidenceItem(
                evidenceArtifactId = EvidenceArtifactId("evidence-1"),
                derivativeGenerationId = DerivativeGenerationId("gen-1"),
                derivativeKind = "Searchable PDF literal text",
                contentIdentity = DerivativeContentIdentity.NoCanonicalSerialization,
                producerIdentity = TierADerivativePayloadFixtures.PRODUCER,
                extractedText = "extracted text",
                completenessState = DerivativeCompletenessState.ACCOUNTED_FOR,
                warnings = emptyList(),
            ),
        ),
        mechanismIdentity = null,
        mechanismVersion = null,
        analysedAt = Instant.parse("2026-01-01T00:00:00Z"),
        instruction = "Summarise",
        warnings = emptyList(),
    )

    @Test
    fun `A a newly registered analysis is claimable and returns the exact same result object`() = runTest {
        val cache = PendingAnalysisCache()
        val analysisResult = result()

        val id = cache.register(analysisResult)
        val claimed = assertIs<PendingAnalysisCache.ClaimOutcome.Claimed>(cache.claim(id))

        assertEquals(analysisResult, claimed.result)
    }

    @Test
    fun `B registering twice yields two distinct pending ids`() = runTest {
        val cache = PendingAnalysisCache()
        val id1 = cache.register(result())
        val id2 = cache.register(result())
        assertNotEquals(id1, id2)
    }

    @Test
    fun `an unknown pending id is reported distinctly as UnknownOrExpired`() = runTest {
        val cache = PendingAnalysisCache()
        assertEquals(PendingAnalysisCache.ClaimOutcome.UnknownOrExpired, cache.claim(PendingAnalysisId("never-registered")))
    }

    @Test
    fun `an expired pending id is reported as UnknownOrExpired, never as Claimed`() = runTest {
        var now = Instant.parse("2026-01-01T00:00:00Z")
        val cache = PendingAnalysisCache(ttl = Duration.ofMinutes(15), now = { now })
        val id = cache.register(result())

        now = now.plus(Duration.ofMinutes(16))

        assertEquals(PendingAnalysisCache.ClaimOutcome.UnknownOrExpired, cache.claim(id))
    }

    @Test
    fun `a claimed (in-flight) entry rejects a concurrent second claim with AlreadyInFlight, never allowing two claims on the same result`() = runTest {
        val cache = PendingAnalysisCache()
        val id = cache.register(result())

        assertIs<PendingAnalysisCache.ClaimOutcome.Claimed>(cache.claim(id))
        assertEquals(PendingAnalysisCache.ClaimOutcome.AlreadyInFlight, cache.claim(id))
    }

    @Test
    fun `finalize permanently removes an entry -- one-shot guarantee, a later claim returns UnknownOrExpired`() = runTest {
        val cache = PendingAnalysisCache()
        val id = cache.register(result())
        assertIs<PendingAnalysisCache.ClaimOutcome.Claimed>(cache.claim(id))

        cache.finalize(id)

        assertEquals(PendingAnalysisCache.ClaimOutcome.UnknownOrExpired, cache.claim(id))
    }

    @Test
    fun `release returns an in-flight entry to available -- a legitimate retry with the same id succeeds`() = runTest {
        val cache = PendingAnalysisCache()
        val id = cache.register(result())
        val firstClaim = assertIs<PendingAnalysisCache.ClaimOutcome.Claimed>(cache.claim(id))

        cache.release(id)
        val secondClaim = assertIs<PendingAnalysisCache.ClaimOutcome.Claimed>(cache.claim(id))

        assertEquals(firstClaim.result, secondClaim.result)
    }

    @Test
    fun `registering beyond maxEntries evicts the single oldest entry, never fails registration`() = runTest {
        val cache = PendingAnalysisCache(maxEntries = 2)
        val id1 = cache.register(result("first"))
        cache.register(result("second"))
        cache.register(result("third"))

        assertEquals(PendingAnalysisCache.ClaimOutcome.UnknownOrExpired, cache.claim(id1))
    }
}
