package parker.core.runtime

import parker.core.interfaces.*

sealed interface GovernedAcquisitionOwnerEvaluation {
    data class Evaluated(
        val source: AcquisitionSource,
        val routing: EvidenceAcquisitionRoutingOutcome,
    ) : GovernedAcquisitionOwnerEvaluation
    data class SourceUnavailable(val evidenceArtifactId: EvidenceArtifactId, val reason: String) : GovernedAcquisitionOwnerEvaluation
}

sealed interface GovernedAcquisitionOwnerExecution {
    data class Executed(
        val source: AcquisitionSource,
        val decision: EvidenceAcquisitionRoutingDecision,
        val result: GovernedAcquisitionExecutionResult,
    ) : GovernedAcquisitionOwnerExecution
    data class StaleOrUnavailable(val current: GovernedAcquisitionOwnerEvaluation) : GovernedAcquisitionOwnerExecution
}

/** Read-only decision plus explicit expected-decision execution. Browser input is never routing authority. */
internal class GovernedAcquisitionOwnerWorkflow(
    private val ownerPrincipalId: PrincipalId,
    private val evidenceCustodian: EvidenceCustodian,
    private val registry: GovernedAcquisitionCapabilityRegistry,
    private val router: DeterministicEvidenceAcquisitionRouter,
    private val executionCoordinator: GovernedAcquisitionExecutionCoordinator,
) {
    suspend fun evaluate(evidenceArtifactId: EvidenceArtifactId): GovernedAcquisitionOwnerEvaluation {
        val manifest = when (val retrieved = evidenceCustodian.retrieveManifest(ownerPrincipalId, evidenceArtifactId)) {
            is EvidenceManifestRetrievalResult.Found -> retrieved.manifest
            is EvidenceManifestRetrievalResult.NotFound -> return unavailable(evidenceArtifactId, "SOURCE_MANIFEST_NOT_FOUND")
            is EvidenceManifestRetrievalResult.Rejected -> return unavailable(evidenceArtifactId, "SOURCE_MANIFEST_UNAVAILABLE")
        }
        if (manifest.evidenceArtifactId != evidenceArtifactId) return unavailable(evidenceArtifactId, "SOURCE_IDENTITY_MISMATCH")
        val source = projectTechnicalFacts(manifest)
            ?: return unavailable(evidenceArtifactId, "SOURCE_MEDIA_TYPE_UNKNOWN")
        return GovernedAcquisitionOwnerEvaluation.Evaluated(
            source, router.route(source, registry.capabilities(), ExternalEgressAuthorisation.NOT_AUTHORISED),
        )
    }

    suspend fun execute(
        evidenceArtifactId: EvidenceArtifactId,
        expectedCapabilityId: String,
    ): GovernedAcquisitionOwnerExecution {
        val current = evaluate(evidenceArtifactId)
        val evaluated = current as? GovernedAcquisitionOwnerEvaluation.Evaluated
            ?: return GovernedAcquisitionOwnerExecution.StaleOrUnavailable(current)
        val selected = evaluated.routing as? EvidenceAcquisitionRoutingOutcome.Selected
            ?: return GovernedAcquisitionOwnerExecution.StaleOrUnavailable(current)
        if (selected.decision.capability.capabilityId != expectedCapabilityId) {
            return GovernedAcquisitionOwnerExecution.StaleOrUnavailable(current)
        }
        return GovernedAcquisitionOwnerExecution.Executed(
            evaluated.source,
            selected.decision,
            executionCoordinator.execute(ownerPrincipalId, evaluated.source, ExternalEgressAuthorisation.NOT_AUTHORISED),
        )
    }

    private fun projectTechnicalFacts(manifest: EvidenceSourceManifest): AcquisitionSource? {
        val media = manifest.receivedMediaType?.lowercase() ?: return null
        val nativeStructured = media == "text/csv" || media == "message/rfc822" ||
            media == "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        val image = media.startsWith("image/")
        return AcquisitionSourceCharacteristicsProjector.project(
            manifest = manifest,
            pageCount = if (image) AcquisitionPageCount.Known(1) else AcquisitionPageCount.Unknown,
            nativeSearchableText = when { nativeStructured -> AcquisitionCharacteristicState.PRESENT; image -> AcquisitionCharacteristicState.ABSENT; else -> AcquisitionCharacteristicState.UNKNOWN },
            imageOnlyOrScanned = when { image -> AcquisitionCharacteristicState.PRESENT; nativeStructured -> AcquisitionCharacteristicState.ABSENT; else -> AcquisitionCharacteristicState.UNKNOWN },
            mixedTextAndImage = if (nativeStructured || image) AcquisitionCharacteristicState.ABSENT else AcquisitionCharacteristicState.UNKNOWN,
        )
    }

    private fun unavailable(id: EvidenceArtifactId, reason: String) =
        GovernedAcquisitionOwnerEvaluation.SourceUnavailable(id, reason)
}
