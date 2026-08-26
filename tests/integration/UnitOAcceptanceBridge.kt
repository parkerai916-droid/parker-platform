package parker.core.runtime

import java.security.MessageDigest
import java.time.Instant
import parker.core.interfaces.DerivativeGenerationId
import parker.core.interfaces.EvidenceArtifactId

enum class UnitODocumentCase { CLEAN_PRINTED, HANDWRITTEN_MIXED }
enum class UnitOProducer { LOCAL, EXTERNAL }
enum class UnitOReviewClassification {
    EXACT_CORRECT, SUBSTANTIVELY_CORRECT, INCORRECT, OMITTED,
    INVENTED_HALLUCINATED, GENUINELY_UNREADABLE_UNCERTAIN,
}
enum class UnitOCriticality { CRITICAL_FACT, SUBSTANTIVE_WORDING, ORDINARY_TEXT }

data class UnitOSyntheticEvidence(
    val evidenceArtifactId: EvidenceArtifactId,
    val documentCase: UnitODocumentCase,
    val content: ByteArray,
    val declaredMediaType: String = "application/pdf",
) {
    val sha256: String = MessageDigest.getInstance("SHA-256").digest(content)
        .joinToString("") { "%02x".format(it) }
    val byteLength: Long = content.size.toLong()
}

data class UnitOBoundedMetadata(
    val evidenceArtifactId: EvidenceArtifactId,
    val sha256: String,
    val byteLength: Long,
    val declaredMediaType: String,
    val eligible: Boolean,
)

/** Acceptance-only metadata bridge. It deliberately has no method capable of returning bytes. */
class UnitOAcceptanceBridge(
    fixtures: List<UnitOSyntheticEvidence>,
    private val maximumBytes: Long = 64L * 1024L * 1024L,
) {
    private val byId = fixtures.associateBy { it.evidenceArtifactId }

    fun preflight(evidenceArtifactId: EvidenceArtifactId): UnitOBoundedMetadata? = byId[evidenceArtifactId]?.let {
        UnitOBoundedMetadata(
            it.evidenceArtifactId, it.sha256, it.byteLength, it.declaredMediaType,
            it.byteLength in 1..maximumBytes && it.declaredMediaType == "application/pdf",
        )
    }
}

data class UnitOSyntheticGeneration(
    val evidenceArtifactId: EvidenceArtifactId,
    val derivativeGenerationId: DerivativeGenerationId,
    val documentCase: UnitODocumentCase,
    val producer: UnitOProducer,
    val pageCount: Int,
)

/** Write-once acceptance store whose restart projection retains only durable generation facts. */
class UnitOSyntheticGenerationStore private constructor(
    private val generations: MutableMap<DerivativeGenerationId, UnitOSyntheticGeneration>,
) {
    constructor() : this(linkedMapOf())

    fun admit(generation: UnitOSyntheticGeneration) {
        require(generations.putIfAbsent(generation.derivativeGenerationId, generation) == null)
    }

    fun retrieve(evidenceArtifactId: EvidenceArtifactId, generationId: DerivativeGenerationId): UnitOSyntheticGeneration? =
        generations[generationId]?.takeIf { it.evidenceArtifactId == evidenceArtifactId }

    fun restart(): UnitOSyntheticGenerationStore = UnitOSyntheticGenerationStore(generations.toMutableMap())
}

data class UnitOOperationCounters(
    var localOperations: Int = 0,
    var providerRequests: Int = 0,
    var retries: Int = 0,
    var fallbackCalls: Int = 0,
    var modelSwitches: Int = 0,
    var analysisInvocations: Int = 0,
)

data class UnitOCaseGenerations(
    val documentCase: UnitODocumentCase,
    val evidenceArtifactId: EvidenceArtifactId,
    val localGenerationId: DerivativeGenerationId,
    val externalGenerationId: DerivativeGenerationId,
)

class UnitOOfflineAcceptanceInstrument(
    private val store: UnitOSyntheticGenerationStore,
    private val idFactory: () -> DerivativeGenerationId,
    val counters: UnitOOperationCounters = UnitOOperationCounters(),
) {
    fun process(fixture: UnitOSyntheticEvidence, pageCount: Int, externalSucceeds: Boolean = true): UnitOCaseGenerations? {
        val identityBefore = Triple(fixture.evidenceArtifactId, fixture.sha256, fixture.byteLength)
        counters.localOperations++
        val local = UnitOSyntheticGeneration(
            fixture.evidenceArtifactId, idFactory(), fixture.documentCase, UnitOProducer.LOCAL, pageCount,
        )
        store.admit(local)

        counters.providerRequests++
        if (!externalSucceeds) return null
        val external = UnitOSyntheticGeneration(
            fixture.evidenceArtifactId, idFactory(), fixture.documentCase, UnitOProducer.EXTERNAL, pageCount,
        )
        store.admit(external)
        check(identityBefore == Triple(fixture.evidenceArtifactId, fixture.sha256, fixture.byteLength))
        return UnitOCaseGenerations(
            fixture.documentCase, fixture.evidenceArtifactId,
            local.derivativeGenerationId, external.derivativeGenerationId,
        )
    }
}

data class UnitOReviewRow(
    val pageNumber: Int,
    val criticality: UnitOCriticality,
    val sourceGenuinelyUnreadable: Boolean,
    val localClassification: UnitOReviewClassification,
    val externalClassification: UnitOReviewClassification,
)

data class UnitOWorksheet(
    val documentCase: UnitODocumentCase,
    val reviewerIdentity: String,
    val reviewedAt: Instant,
    val evidenceArtifactId: EvidenceArtifactId,
    val sourceSha256: String,
    val sourceByteLength: Long,
    val sourcePageCount: Int,
    val localGenerationId: DerivativeGenerationId,
    val externalGenerationId: DerivativeGenerationId,
    val localUsability: Int,
    val externalUsability: Int,
    val localPageCoverage: Int,
    val externalPageCoverage: Int,
    val localReadingOrder: Int,
    val externalReadingOrder: Int,
    val rows: List<UnitOReviewRow>,
) {
    init {
        require(localUsability in 1..5 && externalUsability in 1..5)
        require(sourcePageCount > 0)
        require(rows.isNotEmpty() && rows.all { it.pageNumber in 1..sourcePageCount })
        require((1..sourcePageCount).all { page -> rows.any { it.pageNumber == page } })
        require(localPageCoverage in 0..sourcePageCount && externalPageCoverage in 0..sourcePageCount)
    }
}

data class UnitOGovernanceFacts(
    val selectedCaseCount: Int,
    val sourceIdentityInvariant: Boolean,
    val exactRetrievalPassed: Boolean,
    val restartRetrievalPassed: Boolean,
    val counters: UnitOOperationCounters,
)

data class UnitOAcceptanceDecision(val accepted: Boolean, val reason: String) {
    fun safeDiagnostic(): String = "ACCEPTED=$accepted CATEGORY=$reason"
}

object UnitOAcceptanceCalculator {
    private val defects = setOf(
        UnitOReviewClassification.INCORRECT,
        UnitOReviewClassification.OMITTED,
        UnitOReviewClassification.INVENTED_HALLUCINATED,
    )

    fun evaluate(worksheets: List<UnitOWorksheet>, governance: UnitOGovernanceFacts): UnitOAcceptanceDecision {
        val byCase = worksheets.associateBy { it.documentCase }
        val clean = byCase[UnitODocumentCase.CLEAN_PRINTED]
        val difficult = byCase[UnitODocumentCase.HANDWRITTEN_MIXED]
        val c = governance.counters
        val governancePassed = governance.selectedCaseCount == 2 && worksheets.size == 2 && clean != null && difficult != null &&
            governance.sourceIdentityInvariant && governance.exactRetrievalPassed && governance.restartRetrievalPassed &&
            c.localOperations == 2 && c.providerRequests == 2 && c.retries == 0 && c.fallbackCalls == 0 &&
            c.modelSwitches == 0 && c.analysisInvocations == 0 &&
            worksheets.none { sheet -> sheet.rows.any { it.criticality == UnitOCriticality.CRITICAL_FACT && it.externalClassification == UnitOReviewClassification.INVENTED_HALLUCINATED } }
        if (!governancePassed) return UnitOAcceptanceDecision(false, "GOVERNANCE_GATE_FAILED")

        fun UnitOWorksheet.localHighImpact() = rows.count { it.criticality != UnitOCriticality.ORDINARY_TEXT && it.localClassification in defects }
        fun UnitOWorksheet.externalHighImpact() = rows.count { it.criticality != UnitOCriticality.ORDINARY_TEXT && it.externalClassification in defects }
        fun UnitOWorksheet.localInvented() = rows.count { it.localClassification == UnitOReviewClassification.INVENTED_HALLUCINATED }
        fun UnitOWorksheet.externalInvented() = rows.count { it.externalClassification == UnitOReviewClassification.INVENTED_HALLUCINATED }
        fun UnitOWorksheet.unreadableHonest() = rows.filter { it.sourceGenuinelyUnreadable }
            .all { it.externalClassification == UnitOReviewClassification.GENUINELY_UNREADABLE_UNCERTAIN }

        val cleanPassed = clean!!.externalHighImpact() <= clean.localHighImpact() &&
            clean.externalPageCoverage >= clean.localPageCoverage && clean.externalReadingOrder >= clean.localReadingOrder &&
            clean.externalUsability >= clean.localUsability
        val difficultDelta = difficult!!.localHighImpact() - difficult.externalHighImpact()
        val difficultPassed = difficultDelta >= 2 && difficult.externalUsability >= difficult.localUsability + 1
        val localCombined = worksheets.sumOf { it.localHighImpact() }
        val externalCombined = worksheets.sumOf { it.externalHighImpact() }
        val combinedDelta = localCombined - externalCombined
        val combinedPassed = combinedDelta >= 3 && localCombined > 0 && combinedDelta * 100 >= localCombined * 25
        val hallucinationPassed = worksheets.sumOf { it.externalInvented() } <= worksheets.sumOf { it.localInvented() }
        val uncertaintyPassed = worksheets.all { it.unreadableHonest() }
        return if (cleanPassed && difficultPassed && combinedPassed && hallucinationPassed && uncertaintyPassed) {
            UnitOAcceptanceDecision(true, "LOCKED_CRITERIA_PASSED")
        } else {
            UnitOAcceptanceDecision(false, "QUALITY_GATE_FAILED")
        }
    }
}
