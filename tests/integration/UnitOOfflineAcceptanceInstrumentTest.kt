package parker.core.runtime

import java.time.Instant
import kotlin.test.*
import org.junit.jupiter.api.Test
import parker.core.interfaces.DerivativeGenerationId
import parker.core.interfaces.EvidenceArtifactId

class UnitOOfflineAcceptanceInstrumentTest {
    private val clean = UnitOSyntheticEvidence(
        EvidenceArtifactId("unit-o-clean-synthetic"), UnitODocumentCase.CLEAN_PRINTED,
        "SYNTHETIC_CLEAN_SOURCE_SENTINEL".toByteArray(),
    )
    private val difficult = UnitOSyntheticEvidence(
        EvidenceArtifactId("unit-o-difficult-synthetic"), UnitODocumentCase.HANDWRITTEN_MIXED,
        "SYNTHETIC_DIFFICULT_SOURCE_SENTINEL".toByteArray(),
    )

    @Test
    fun `bounded metadata bridge exposes facts and eligibility but no bytes`() {
        val bridge = UnitOAcceptanceBridge(listOf(clean, difficult), maximumBytes = 1024)
        val metadata = requireNotNull(bridge.preflight(clean.evidenceArtifactId))
        assertEquals(clean.evidenceArtifactId, metadata.evidenceArtifactId)
        assertEquals(clean.sha256, metadata.sha256)
        assertEquals(clean.byteLength, metadata.byteLength)
        assertEquals("application/pdf", metadata.declaredMediaType)
        assertTrue(metadata.eligible)
        assertFalse(UnitOAcceptanceBridge::class.java.methods.any { it.returnType == ByteArray::class.java })
    }

    @Test
    fun `two cases mint four independent generations and exact retrieval never substitutes latest`() {
        val ids = ArrayDeque((1..4).map { DerivativeGenerationId("unit-o-generation-$it") })
        val store = UnitOSyntheticGenerationStore()
        val instrument = UnitOOfflineAcceptanceInstrument(store, { ids.removeFirst() })
        val cleanIds = requireNotNull(instrument.process(clean, 2))
        val difficultIds = requireNotNull(instrument.process(difficult, 3))

        assertNotEquals(cleanIds.localGenerationId, cleanIds.externalGenerationId)
        assertNotEquals(difficultIds.localGenerationId, difficultIds.externalGenerationId)
        assertEquals(UnitOProducer.LOCAL, store.retrieve(clean.evidenceArtifactId, cleanIds.localGenerationId)?.producer)
        assertEquals(UnitOProducer.EXTERNAL, store.retrieve(clean.evidenceArtifactId, cleanIds.externalGenerationId)?.producer)
        assertNull(store.retrieve(clean.evidenceArtifactId, difficultIds.externalGenerationId))
        assertEquals(clean.sha256, UnitOAcceptanceBridge(listOf(clean)).preflight(clean.evidenceArtifactId)?.sha256)
        assertEquals(2, instrument.counters.localOperations)
        assertEquals(2, instrument.counters.providerRequests)
    }

    @Test
    fun `restart retrieves all four exact generations without changing counters`() {
        val ids = ArrayDeque((1..4).map { DerivativeGenerationId("restart-generation-$it") })
        val store = UnitOSyntheticGenerationStore()
        val instrument = UnitOOfflineAcceptanceInstrument(store, { ids.removeFirst() })
        val pairs = listOf(requireNotNull(instrument.process(clean, 2)), requireNotNull(instrument.process(difficult, 3)))
        val before = instrument.counters.copy()
        val restarted = store.restart()

        pairs.forEach { pair ->
            assertNotNull(restarted.retrieve(pair.evidenceArtifactId, pair.localGenerationId))
            assertNotNull(restarted.retrieve(pair.evidenceArtifactId, pair.externalGenerationId))
        }
        assertEquals(before, instrument.counters)
    }

    @Test
    fun `worksheet fixes classifications criticality pages identity and reviewer facts`() {
        val worksheet = worksheet(clean, DerivativeGenerationId("local"), DerivativeGenerationId("external"), cleanRows())
        assertEquals("owner", worksheet.reviewerIdentity)
        assertEquals(Instant.EPOCH, worksheet.reviewedAt)
        assertEquals(clean.sha256, worksheet.sourceSha256)
        assertEquals(setOf(1, 2), worksheet.rows.map { it.pageNumber }.toSet())
        assertEquals(UnitOCriticality.CRITICAL_FACT, worksheet.rows.first().criticality)
        assertEquals(UnitOReviewClassification.EXACT_CORRECT, worksheet.rows.first().externalClassification)
        assertEquals(6, UnitOReviewClassification.entries.size)
        assertEquals(3, UnitOCriticality.entries.size)
        assertFailsWith<IllegalArgumentException> { worksheet.copy(rows = worksheet.rows.filter { it.pageNumber == 1 }) }
    }

    @Test
    fun `locked criteria pass only at clean difficult and combined thresholds`() {
        val counters = UnitOOperationCounters(localOperations = 2, providerRequests = 2)
        val decision = UnitOAcceptanceCalculator.evaluate(
            listOf(
                worksheet(clean, DerivativeGenerationId("clean-local"), DerivativeGenerationId("clean-external"), cleanRows()),
                worksheet(difficult, DerivativeGenerationId("difficult-local"), DerivativeGenerationId("difficult-external"), difficultRows(), 2, 3),
            ),
            UnitOGovernanceFacts(2, true, true, true, counters),
        )
        assertEquals(UnitOAcceptanceDecision(true, "LOCKED_CRITERIA_PASSED"), decision)
    }

    @Test
    fun `each quality threshold fails independently`() {
        val facts = UnitOGovernanceFacts(2, true, true, true, UnitOOperationCounters(localOperations = 2, providerRequests = 2))
        val baseClean = worksheet(clean, DerivativeGenerationId("cl"), DerivativeGenerationId("ce"), cleanRows())
        val baseDifficult = worksheet(difficult, DerivativeGenerationId("dl"), DerivativeGenerationId("de"), difficultRows(), 2, 3)
        val cleanInferior = baseClean.copy(externalUsability = 2)
        val difficultOnlyOneBetter = baseDifficult.copy(rows = baseDifficult.rows.mapIndexed { i, row -> if (i < 2) row.copy(externalClassification = row.localClassification) else row })
        val combinedClean = baseClean.copy(rows = baseClean.rows.map { row -> row.copy(localClassification = UnitOReviewClassification.EXACT_CORRECT) })
        val combinedOnlyTwoBetter = baseDifficult.copy(rows = baseDifficult.rows.drop(1))
        assertFalse(UnitOAcceptanceCalculator.evaluate(listOf(cleanInferior, baseDifficult), facts).accepted)
        assertFalse(UnitOAcceptanceCalculator.evaluate(listOf(baseClean, difficultOnlyOneBetter), facts).accepted)
        assertFalse(UnitOAcceptanceCalculator.evaluate(listOf(combinedClean, combinedOnlyTwoBetter), facts).accepted)
    }

    @Test
    fun `critical hallucination and governance failure override quality`() {
        val good = listOf(
            worksheet(clean, DerivativeGenerationId("cl"), DerivativeGenerationId("ce"), cleanRows()),
            worksheet(difficult, DerivativeGenerationId("dl"), DerivativeGenerationId("de"), difficultRows(), 2, 3),
        )
        val counters = UnitOOperationCounters(localOperations = 2, providerRequests = 2)
        val hallucinated = good.toMutableList().also { sheets ->
            sheets[0] = sheets[0].copy(rows = sheets[0].rows.toMutableList().also { rows ->
                rows[0] = rows[0].copy(externalClassification = UnitOReviewClassification.INVENTED_HALLUCINATED)
            })
        }
        assertEquals("GOVERNANCE_GATE_FAILED", UnitOAcceptanceCalculator.evaluate(hallucinated, UnitOGovernanceFacts(2, true, true, true, counters)).reason)
        assertEquals("GOVERNANCE_GATE_FAILED", UnitOAcceptanceCalculator.evaluate(good, UnitOGovernanceFacts(2, false, true, true, counters)).reason)
        assertEquals("GOVERNANCE_GATE_FAILED", UnitOAcceptanceCalculator.evaluate(good, UnitOGovernanceFacts(2, true, true, true, counters.copy(retries = 1))).reason)
    }

    @Test
    fun `external failure stops after one request with no retry fallback switch analysis or generation`() {
        val ids = ArrayDeque(listOf(DerivativeGenerationId("local-only"), DerivativeGenerationId("must-not-be-used")))
        val store = UnitOSyntheticGenerationStore()
        val instrument = UnitOOfflineAcceptanceInstrument(store, { ids.removeFirst() })
        assertNull(instrument.process(clean, 2, externalSucceeds = false))
        assertEquals(UnitOOperationCounters(localOperations = 1, providerRequests = 1), instrument.counters)
        assertEquals(UnitOProducer.LOCAL, store.retrieve(clean.evidenceArtifactId, DerivativeGenerationId("local-only"))?.producer)
        assertNull(store.retrieve(clean.evidenceArtifactId, DerivativeGenerationId("must-not-be-used")))
    }

    @Test
    fun `bounded diagnostics never contain source transcript worksheet or secret sentinels`() {
        val sentinels = listOf("SOURCE_SENTINEL", "TRANSCRIPT_SENTINEL", "WORKSHEET_SENTINEL", "API_KEY_SENTINEL")
        val decision = UnitOAcceptanceDecision(false, "GOVERNANCE_GATE_FAILED")
        val diagnostic = decision.safeDiagnostic()
        sentinels.forEach { assertFalse(diagnostic.contains(it)) }
        assertEquals("ACCEPTED=false CATEGORY=GOVERNANCE_GATE_FAILED", diagnostic)
    }

    private fun worksheet(
        fixture: UnitOSyntheticEvidence,
        localId: DerivativeGenerationId,
        externalId: DerivativeGenerationId,
        rows: List<UnitOReviewRow>,
        localUsability: Int = 4,
        externalUsability: Int = 4,
    ) = UnitOWorksheet(
        fixture.documentCase, "owner", Instant.EPOCH, fixture.evidenceArtifactId, fixture.sha256,
        fixture.byteLength, 2, localId, externalId, localUsability, externalUsability,
        localPageCoverage = 2, externalPageCoverage = 2, localReadingOrder = 2, externalReadingOrder = 2,
        rows = rows,
    )

    private fun cleanRows() = listOf(
        UnitOReviewRow(1, UnitOCriticality.CRITICAL_FACT, false, UnitOReviewClassification.INCORRECT, UnitOReviewClassification.EXACT_CORRECT),
        UnitOReviewRow(2, UnitOCriticality.ORDINARY_TEXT, false, UnitOReviewClassification.EXACT_CORRECT, UnitOReviewClassification.EXACT_CORRECT),
    )

    private fun difficultRows() = listOf(
        UnitOReviewRow(1, UnitOCriticality.CRITICAL_FACT, false, UnitOReviewClassification.INCORRECT, UnitOReviewClassification.EXACT_CORRECT),
        UnitOReviewRow(1, UnitOCriticality.SUBSTANTIVE_WORDING, false, UnitOReviewClassification.OMITTED, UnitOReviewClassification.SUBSTANTIVELY_CORRECT),
        UnitOReviewRow(2, UnitOCriticality.CRITICAL_FACT, false, UnitOReviewClassification.INVENTED_HALLUCINATED, UnitOReviewClassification.EXACT_CORRECT),
        UnitOReviewRow(2, UnitOCriticality.ORDINARY_TEXT, true, UnitOReviewClassification.INVENTED_HALLUCINATED, UnitOReviewClassification.GENUINELY_UNREADABLE_UNCERTAIN),
    )
}
