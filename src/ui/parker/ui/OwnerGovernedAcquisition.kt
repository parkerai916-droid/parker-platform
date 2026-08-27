package parker.ui

import parker.core.interfaces.DerivativeGenerationId

data class OwnerAcquisitionSourceFacts(
    val evidenceArtifactId: String,
    val mediaType: String?,
    val byteLength: Long?,
    val pageCount: Int?,
    val nativeSearchableText: String,
    val imageOnlyOrScanned: String,
    val mixedTextAndImage: String?,
    val handwriting: String,
    val complexLayout: String,
    val tables: String,
)

data class OwnerAcquisitionCapabilityView(
    val capabilityId: String,
    val mechanismLabel: String,
    val executionLocation: String,
    val externalEgressRequired: Boolean,
    val provider: String?,
    val modelRule: String?,
    val profile: String?,
    val representationLabel: String,
    val availability: String,
    val selectionReasons: List<String>,
)

sealed interface OwnerAcquisitionDecisionView {
    val source: OwnerAcquisitionSourceFacts
    data class Selected(
        override val source: OwnerAcquisitionSourceFacts,
        val capability: OwnerAcquisitionCapabilityView,
        val explanation: String,
    ) : OwnerAcquisitionDecisionView
    data class NoEligible(
        override val source: OwnerAcquisitionSourceFacts,
        val reasons: List<String>,
    ) : OwnerAcquisitionDecisionView
    data class Indeterminate(
        override val source: OwnerAcquisitionSourceFacts,
        val reasons: List<String>,
    ) : OwnerAcquisitionDecisionView
    data class Ambiguous(
        override val source: OwnerAcquisitionSourceFacts,
        val capabilityIds: List<String>,
        val reasons: List<String>,
    ) : OwnerAcquisitionDecisionView
}

sealed interface OwnerAcquisitionExecutionView {
    data class Admitted(
        val derivativeGenerationId: DerivativeGenerationId,
        val evidenceArtifactId: String,
        val capability: OwnerAcquisitionCapabilityView,
        val fidelity: String,
        val completeness: String,
        val humanReviewState: String = "UNREVIEWED",
    ) : OwnerAcquisitionExecutionView
    data class StaleDecision(val currentDecision: OwnerAcquisitionDecisionView) : OwnerAcquisitionExecutionView
    data class Failed(val reason: String, val capability: OwnerAcquisitionCapabilityView? = null) : OwnerAcquisitionExecutionView
}
