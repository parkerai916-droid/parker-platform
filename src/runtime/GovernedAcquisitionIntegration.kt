package parker.core.runtime

import parker.core.interfaces.*
import java.util.UUID
import java.util.concurrent.CancellationException

class GovernedAcquisitionCapabilityRegistry(capabilities: Collection<EvidenceAcquisitionCapability>) {
    private val byId: Map<String, EvidenceAcquisitionCapability>
    init {
        require(capabilities.map { it.capabilityId }.distinct().size == capabilities.size) {
            "Acquisition capability identities must be unique"
        }
        byId = capabilities.associateBy { it.capabilityId }.toMap()
    }
    fun capabilities(): List<EvidenceAcquisitionCapability> = byId.values.toList()
    fun capability(id: String): EvidenceAcquisitionCapability? = byId[id]
}

/** Conservative projection only; unknown technical facts remain unknown. */
object AcquisitionSourceCharacteristicsProjector {
    fun project(
        manifest: EvidenceSourceManifest,
        pageCount: AcquisitionPageCount = AcquisitionPageCount.Unknown,
        nativeSearchableText: AcquisitionCharacteristicState = AcquisitionCharacteristicState.UNKNOWN,
        imageOnlyOrScanned: AcquisitionCharacteristicState = AcquisitionCharacteristicState.UNKNOWN,
        mixedTextAndImage: AcquisitionCharacteristicState = AcquisitionCharacteristicState.UNKNOWN,
        handwriting: AcquisitionCharacteristicState = AcquisitionCharacteristicState.UNKNOWN,
        complexLayout: AcquisitionCharacteristicState = AcquisitionCharacteristicState.UNKNOWN,
        tables: AcquisitionCharacteristicState = AcquisitionCharacteristicState.UNKNOWN,
    ): AcquisitionSource? = manifest.receivedMediaType?.let { mediaType ->
        AcquisitionSource(
            manifest.evidenceArtifactId, manifest.sha256, manifest.byteLength, mediaType, pageCount,
            AcquisitionSourceCharacteristics(nativeSearchableText, imageOnlyOrScanned, mixedTextAndImage,
                handwriting, complexLayout, tables), HumanAuthorisedCustody.CONFIRMED,
        )
    }
}

data class AcquisitionExecutorBinding(
    val capabilityId: String,
    val mechanism: EvidenceAcquisitionMechanism,
    val configurationIdentity: String?,
)

internal data class GovernedAcquisitionExecutionRequest(
    val decision: EvidenceAcquisitionRoutingDecision,
    val authoritativeSource: AuthoritativeAcquisitionInput,
    val principalId: PrincipalId,
)

internal sealed interface BoundAcquisitionExecutorOutcome {
    data class Admitted(
        val derivativeGenerationId: DerivativeGenerationId,
        val fidelity: TranscriptionFidelity? = null,
        val completeness: DerivativeCompletenessState? = null,
        val processingProvenance: OcrProcessingProvenance? = null,
    ) : BoundAcquisitionExecutorOutcome
    data class ExecutionFailed(val reason: AcquisitionExecutionFailureReason) : BoundAcquisitionExecutorOutcome
    data class AdmissionFailed(val reason: AcquisitionExecutionFailureReason) : BoundAcquisitionExecutorOutcome
}

internal interface BoundAcquisitionCapabilityExecutor {
    val binding: AcquisitionExecutorBinding
    suspend fun execute(request: GovernedAcquisitionExecutionRequest): BoundAcquisitionExecutorOutcome
}

enum class AcquisitionExecutionFailureReason {
    ROUTING_NO_ELIGIBLE_CAPABILITY,
    ROUTING_INDETERMINATE,
    ROUTING_AMBIGUOUS,
    SOURCE_RESOLUTION_FAILED,
    SOURCE_BINDING_MISMATCH,
    SELECTED_CAPABILITY_UNAVAILABLE,
    CAPABILITY_BINDING_MISMATCH,
    ACQUISITION_EXECUTION_FAILED,
    DERIVATIVE_ADMISSION_FAILED,
}

class AcquisitionRoutingProvenance(
    val evidenceArtifactId: EvidenceArtifactId,
    val sourceSha256: String,
    val capabilityId: String,
    val mechanism: EvidenceAcquisitionMechanism,
    val configurationIdentity: String?,
    val representationClass: AcquisitionRepresentationClass,
    val externalEgressRequired: Boolean,
    selectionReasons: Set<AcquisitionSelectionReason>,
) {
    val selectionReasons: Set<AcquisitionSelectionReason> = selectionReasons.toSet()
    override fun toString(): String =
        "AcquisitionRoutingProvenance(evidenceArtifactId=$evidenceArtifactId, sourceSha256=$sourceSha256, " +
            "capabilityId=$capabilityId, mechanism=$mechanism, configurationIdentity=$configurationIdentity, " +
            "representationClass=$representationClass, externalEgressRequired=$externalEgressRequired, " +
            "selectionReasons=$selectionReasons)"
}

internal sealed interface GovernedAcquisitionExecutionResult {
    data class Admitted(
        val routingProvenance: AcquisitionRoutingProvenance,
        val derivativeGenerationId: DerivativeGenerationId,
        val fidelity: TranscriptionFidelity?,
        val completeness: DerivativeCompletenessState?,
        val processingProvenance: OcrProcessingProvenance?,
    ) : GovernedAcquisitionExecutionResult
    data class Failed(
        val reason: AcquisitionExecutionFailureReason,
        val routingOutcome: EvidenceAcquisitionRoutingOutcome? = null,
        val routingProvenance: AcquisitionRoutingProvenance? = null,
    ) : GovernedAcquisitionExecutionResult
}

/** Routes once, resolves one exact source, and invokes at most one exactly bound executor. */
internal class GovernedAcquisitionExecutionCoordinator(
    private val registry: GovernedAcquisitionCapabilityRegistry,
    private val router: DeterministicEvidenceAcquisitionRouter,
    evidenceCustodian: EvidenceCustodian,
    executors: Collection<BoundAcquisitionCapabilityExecutor>,
) {
    private val sourceResolver = AuthoritativeAcquisitionSourceResolver(evidenceCustodian)
    private val executorsById: Map<String, BoundAcquisitionCapabilityExecutor>
    init {
        require(executors.map { it.binding.capabilityId }.distinct().size == executors.size)
        executorsById = executors.associateBy { it.binding.capabilityId }.toMap()
    }

    suspend fun execute(
        principalId: PrincipalId,
        source: AcquisitionSource,
        egressAuthorisation: ExternalEgressAuthorisation,
    ): GovernedAcquisitionExecutionResult {
        val routed = router.route(source, registry.capabilities(), egressAuthorisation)
        val decision = when (routed) {
            is EvidenceAcquisitionRoutingOutcome.Selected -> routed.decision
            is EvidenceAcquisitionRoutingOutcome.NoEligibleCapability -> return failure(AcquisitionExecutionFailureReason.ROUTING_NO_ELIGIBLE_CAPABILITY, routed)
            is EvidenceAcquisitionRoutingOutcome.Indeterminate -> return failure(AcquisitionExecutionFailureReason.ROUTING_INDETERMINATE, routed)
            is EvidenceAcquisitionRoutingOutcome.Ambiguous -> return failure(AcquisitionExecutionFailureReason.ROUTING_AMBIGUOUS, routed)
        }
        val selected = decision.capability
        val executor = executorsById[selected.capabilityId]
            ?: return failure(AcquisitionExecutionFailureReason.SELECTED_CAPABILITY_UNAVAILABLE, routed, provenance(decision))
        val expectedConfiguration = selected.providerConfiguration?.configurationIdentity
        if (executor.binding.capabilityId != selected.capabilityId || executor.binding.mechanism != selected.mechanism ||
            executor.binding.configurationIdentity != expectedConfiguration) {
            return failure(AcquisitionExecutionFailureReason.CAPABILITY_BINDING_MISMATCH, routed, provenance(decision))
        }
        if (decision.source.evidenceArtifactId != source.evidenceArtifactId) {
            return failure(AcquisitionExecutionFailureReason.SOURCE_BINDING_MISMATCH, routed, provenance(decision))
        }
        val trusted = when (val resolution = sourceResolver.resolve(principalId, source.evidenceArtifactId)) {
            is AuthoritativeAcquisitionResolution.Verified -> resolution.input
            else -> return failure(AcquisitionExecutionFailureReason.SOURCE_RESOLUTION_FAILED, routed, provenance(decision))
        }
        if (trusted.evidenceArtifactId != decision.source.evidenceArtifactId || trusted.sha256 != decision.source.sha256 ||
            trusted.byteLength != decision.source.byteLength || trusted.mediaType != decision.source.mediaType) {
            return failure(AcquisitionExecutionFailureReason.SOURCE_BINDING_MISMATCH, routed, provenance(decision))
        }
        val routingProvenance = provenance(decision)
        return try {
            when (val outcome = executor.execute(GovernedAcquisitionExecutionRequest(decision, trusted, principalId))) {
                is BoundAcquisitionExecutorOutcome.Admitted -> GovernedAcquisitionExecutionResult.Admitted(
                    routingProvenance, outcome.derivativeGenerationId, outcome.fidelity, outcome.completeness,
                    outcome.processingProvenance,
                )
                is BoundAcquisitionExecutorOutcome.ExecutionFailed -> failure(
                    AcquisitionExecutionFailureReason.ACQUISITION_EXECUTION_FAILED, routed, routingProvenance,
                )
                is BoundAcquisitionExecutorOutcome.AdmissionFailed -> failure(
                    AcquisitionExecutionFailureReason.DERIVATIVE_ADMISSION_FAILED, routed, routingProvenance,
                )
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            failure(AcquisitionExecutionFailureReason.ACQUISITION_EXECUTION_FAILED, routed, routingProvenance)
        }
    }

    private fun provenance(decision: EvidenceAcquisitionRoutingDecision) = AcquisitionRoutingProvenance(
        decision.source.evidenceArtifactId, decision.source.sha256, decision.capability.capabilityId,
        decision.capability.mechanism, decision.capability.providerConfiguration?.configurationIdentity,
        decision.selectedRepresentation, decision.capability.egress == AcquisitionEgress.EXTERNAL_EGRESS_REQUIRED,
        decision.selectionReasons,
    )

    private fun failure(
        reason: AcquisitionExecutionFailureReason,
        routing: EvidenceAcquisitionRoutingOutcome? = null,
        provenance: AcquisitionRoutingProvenance? = null,
    ) = GovernedAcquisitionExecutionResult.Failed(reason, routing, provenance)
}

/** Exact bindings to the already-governed owner invocation boundaries; none implements fallback. */
internal class TierANativeAcquisitionExecutor(
    override val binding: AcquisitionExecutorBinding,
    private val coordinator: TierAOwnerInvocationCoordinator,
    private val correlationFactory: () -> String = { UUID.randomUUID().toString() },
) : BoundAcquisitionCapabilityExecutor {
    override suspend fun execute(request: GovernedAcquisitionExecutionRequest): BoundAcquisitionExecutorOutcome =
        when (val outcome = coordinator.invoke(
            request.principalId, request.authoritativeSource.evidenceArtifactId, correlationFactory(),
        )) {
            is TierAOwnerInvocationOutcome.Routed -> when (val routed = outcome.result) {
                is TierADocumentRoutingResult.Admitted -> admitted(routed.record.derivativeGenerationId)
                is TierADocumentRoutingResult.AdmissionFailed,
                is TierADocumentRoutingResult.ReconciliationRequired,
                -> BoundAcquisitionExecutorOutcome.AdmissionFailed(AcquisitionExecutionFailureReason.DERIVATIVE_ADMISSION_FAILED)
                else -> failed()
            }
            else -> failed()
        }
}

internal class LocalOcrAcquisitionExecutor(
    override val binding: AcquisitionExecutorBinding,
    private val coordinator: TierBOcrOwnerInvocationCoordinator,
    private val correlationFactory: () -> String = { UUID.randomUUID().toString() },
) : BoundAcquisitionCapabilityExecutor {
    override suspend fun execute(request: GovernedAcquisitionExecutionRequest): BoundAcquisitionExecutorOutcome =
        when (val outcome = coordinator.invoke(
            request.principalId, request.authoritativeSource.evidenceArtifactId, correlationFactory(),
        )) {
            is TierBOcrOwnerInvocationOutcome.Admitted -> admitted(outcome.record.derivativeGenerationId)
            is TierBOcrOwnerInvocationOutcome.MandatoryProvenanceUnavailable,
            is TierBOcrOwnerInvocationOutcome.PreparationFailed,
            is TierBOcrOwnerInvocationOutcome.AuthorisationAuditFailed,
            is TierBOcrOwnerInvocationOutcome.PublicationFailed,
            is TierBOcrOwnerInvocationOutcome.AdmittedAuditFailed,
            -> BoundAcquisitionExecutorOutcome.AdmissionFailed(AcquisitionExecutionFailureReason.DERIVATIVE_ADMISSION_FAILED)
            else -> failed()
        }
}

internal class ExternalTranscriptionAcquisitionExecutor(
    override val binding: AcquisitionExecutorBinding,
    private val coordinator: ExternalTranscriptionOwnerInvocationCoordinator,
) : BoundAcquisitionCapabilityExecutor {
    override suspend fun execute(request: GovernedAcquisitionExecutionRequest): BoundAcquisitionExecutorOutcome =
        when (val outcome = coordinator.invoke(request.authoritativeSource.evidenceArtifactId)) {
            is ExternalTranscriptionOwnerInvocationOutcome.Admitted -> admitted(outcome.record.derivativeGenerationId)
            is ExternalTranscriptionOwnerInvocationOutcome.AdmissionFailed,
            is ExternalTranscriptionOwnerInvocationOutcome.ReconciliationRequired,
            -> BoundAcquisitionExecutorOutcome.AdmissionFailed(AcquisitionExecutionFailureReason.DERIVATIVE_ADMISSION_FAILED)
            else -> failed()
        }
}

private fun admitted(id: DerivativeGenerationId) = BoundAcquisitionExecutorOutcome.Admitted(id)
private fun failed() = BoundAcquisitionExecutorOutcome.ExecutionFailed(
    AcquisitionExecutionFailureReason.ACQUISITION_EXECUTION_FAILED,
)
