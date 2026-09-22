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
    /**
     * UI-INGESTION-5: exact-target external-egress authorization, reused from
     * [ExternalTranscriptionOwnerAuthorizationCoordinator.isAuthorized]. Defaults to "never
     * authorised," preserving every existing caller's behavior unchanged.
     */
    private val externalEgressAuthorised: suspend (EvidenceArtifactId) -> Boolean = { false },
    private val derivativeDiscoveryProjection: DerivativeGenerationDiscoveryProjection? = null,
) {
    private val authoritativeSourceResolver = AuthoritativeAcquisitionSourceResolver(evidenceCustodian)
    private val pdfCharacteristicsInspector = PdfSourceCharacteristicsInspector()

    suspend fun evaluate(evidenceArtifactId: EvidenceArtifactId): GovernedAcquisitionOwnerEvaluation {
        val manifest = when (val retrieved = evidenceCustodian.retrieveManifest(ownerPrincipalId, evidenceArtifactId)) {
            is EvidenceManifestRetrievalResult.Found -> retrieved.manifest
            is EvidenceManifestRetrievalResult.NotFound -> return unavailable(evidenceArtifactId, "SOURCE_MANIFEST_NOT_FOUND")
            is EvidenceManifestRetrievalResult.Rejected -> return unavailable(evidenceArtifactId, "SOURCE_MANIFEST_UNAVAILABLE")
        }
        if (manifest.evidenceArtifactId != evidenceArtifactId) return unavailable(evidenceArtifactId, "SOURCE_IDENTITY_MISMATCH")
        val source = projectTechnicalFacts(manifest)
            ?: return unavailable(evidenceArtifactId, "SOURCE_MEDIA_TYPE_UNKNOWN")
        val persistedExternalOcr = usablePersistedExternalOcrRepresentation(evidenceArtifactId)
        // A persisted PDF representation is a retrieval capability, not an execution request.
        // When it exists for a scanned PDF, suppress the otherwise equivalent execution
        // capability for this decision so an enabled local OCR executor cannot outrank or make
        // the already-admitted derivative ambiguous.
        val persistedExternalIsRetrievable = persistedExternalOcr != null && (
            source.mediaType.startsWith("image/") ||
                (source.mediaType == "application/pdf" &&
                    source.characteristics.imageOnlyOrScanned == AcquisitionCharacteristicState.PRESENT)
            )
        val executionCapabilities = registry.capabilities().filterNot {
            // Local OCR is diagnostic/preliminary only. It is never an authoritative governed
            // representation and never a fallback when external OCR is unavailable.
            (!OcrAuthorityPolicy.LOCAL_OCR_AUTHORITATIVE &&
                it.mechanism == EvidenceAcquisitionMechanism.LOCAL_OCR &&
                it.availability is AcquisitionAvailability.Available) ||
                (persistedExternalIsRetrievable &&
                    (it.mechanism == EvidenceAcquisitionMechanism.EXTERNAL_TRANSCRIPTION ||
                        it.mechanism == EvidenceAcquisitionMechanism.EXTERNAL_VISION_TRANSCRIPTION))
        }
        val capabilities = executionCapabilities +
            (if (persistedExternalOcr != null) {
                listOf(ProductionAcquisitionCapabilityCatalogue.persistedExternalOcrRepresentationCapability())
            } else emptyList())
        return GovernedAcquisitionOwnerEvaluation.Evaluated(
            source, router.route(source, capabilities, egressAuthorisation(evidenceArtifactId)),
        )
    }

    private suspend fun egressAuthorisation(evidenceArtifactId: EvidenceArtifactId): ExternalEgressAuthorisation =
        if (externalEgressAuthorised(evidenceArtifactId)) ExternalEgressAuthorisation.AUTHORISED else ExternalEgressAuthorisation.NOT_AUTHORISED

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
        if (expectedCapabilityId == ProductionAcquisitionCapabilityCatalogue.PERSISTED_EXTERNAL_OCR_REPRESENTATION_CAPABILITY_ID
        ) {
            val candidate = usablePersistedExternalOcrRepresentation(evidenceArtifactId)
                ?: return GovernedAcquisitionOwnerExecution.StaleOrUnavailable(current)
            val provenance = AcquisitionRoutingProvenance(
                evaluated.source.evidenceArtifactId,
                evaluated.source.sha256,
                selected.decision.capability.capabilityId,
                selected.decision.capability.mechanism,
                selected.decision.capability.providerConfiguration?.configurationIdentity,
                selected.decision.selectedRepresentation,
                selected.decision.capability.egress == AcquisitionEgress.EXTERNAL_EGRESS_REQUIRED,
                selected.decision.selectionReasons,
            )
            return GovernedAcquisitionOwnerExecution.Executed(
                evaluated.source,
                selected.decision,
                GovernedAcquisitionExecutionResult.Admitted(
                    routingProvenance = provenance,
                    derivativeGenerationId = candidate.derivativeGenerationId,
                    fidelity = TranscriptionFidelity.UNVERIFIED_LITERAL_TRANSCRIPTION,
                    completeness = candidate.completenessState,
                    processingProvenance = null,
                ),
            )
        }
        return GovernedAcquisitionOwnerExecution.Executed(
            evaluated.source,
            selected.decision,
            executionCoordinator.execute(ownerPrincipalId, evaluated.source, egressAuthorisation(evidenceArtifactId)),
        )
    }

    private suspend fun projectTechnicalFacts(manifest: EvidenceSourceManifest): AcquisitionSource? {
        val media = manifest.receivedMediaType?.lowercase() ?: return null
        val nativeStructured = media == "text/csv" || media == "message/rfc822" ||
            media == "text/plain" || media == "application/msword" ||
            media == "application/vnd.ms-excel" ||
            media == "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" ||
            media == "application/vnd.ms-outlook" || media == "application/x-ole-storage" ||
            media == "application/rtf" || media == "text/rtf" || media == "application/x-rtf"
        val image = media.startsWith("image/")
        val docxInspection = if (media == "application/vnd.openxmlformats-officedocument.wordprocessingml.document") {
            when (val resolved = authoritativeSourceResolver.resolve(ownerPrincipalId, manifest.evidenceArtifactId)) {
                is AuthoritativeAcquisitionResolution.Verified -> DocxEmbeddedImageExtractor.inspect(resolved.input.bytes())
                else -> null
            }
        } else null
        val docxReady = docxInspection as? DocxEmbeddedImageInspection.Ready
        val docxNative = docxReady?.readableNativeText == true
        val docxImageOnly = docxReady != null && !docxReady.readableNativeText && docxReady.images.isNotEmpty()
        val pdfInspection = if (media == "application/pdf") {
            when (val resolved = authoritativeSourceResolver.resolve(ownerPrincipalId, manifest.evidenceArtifactId)) {
                is AuthoritativeAcquisitionResolution.Verified -> pdfCharacteristicsInspector.inspect(resolved.input)
                else -> PdfSourceCharacteristicsInspection.Indeterminate("AUTHORITATIVE_SOURCE_UNAVAILABLE")
            }
        } else null
        val establishedPdf = pdfInspection as? PdfSourceCharacteristicsInspection.Established
        return AcquisitionSourceCharacteristicsProjector.project(
            manifest = manifest,
            // STEP 3: text/csv gets the same Known(1) treatment as image/* -- a single, undivided
            // source submitted as one atomic unit for operational page-limit bounding only; this
            // is not a claim about physical pages. Without it, the external capability's
            // maximumPages check leaves CSV's page count PAGE_COUNT_UNKNOWN, which the router
            // reports as Indeterminate rather than Selected -- CSV would never actually reach the
            // external mechanism despite being an otherwise-eligible, accepted media type.
            pageCount = establishedPdf?.pageCount
                ?: if (image || nativeStructured || docxReady != null) AcquisitionPageCount.Known(1) else AcquisitionPageCount.Unknown,
            nativeSearchableText = establishedPdf?.nativeSearchableText ?: when {
                docxNative || nativeStructured -> AcquisitionCharacteristicState.PRESENT
                image || docxImageOnly -> AcquisitionCharacteristicState.ABSENT
                else -> AcquisitionCharacteristicState.UNKNOWN
            },
            imageOnlyOrScanned = establishedPdf?.imageOnlyOrScanned ?: when {
                image || docxImageOnly -> AcquisitionCharacteristicState.PRESENT
                docxNative || nativeStructured -> AcquisitionCharacteristicState.ABSENT
                else -> AcquisitionCharacteristicState.UNKNOWN
            },
            mixedTextAndImage = establishedPdf?.mixedTextAndImage ?: if (nativeStructured || image || docxReady != null) AcquisitionCharacteristicState.ABSENT else AcquisitionCharacteristicState.UNKNOWN,
        )
    }

    private suspend fun usablePersistedExternalOcrRepresentation(evidenceArtifactId: EvidenceArtifactId): DerivativeCandidateSummary? =
        derivativeDiscoveryProjection?.discover(evidenceArtifactId)
            ?.filter {
                it.rootSourceEvidenceArtifactId == evidenceArtifactId &&
                    it.derivativeKind in setOf("External transcription recognised text", "OCR recognised text") &&
                    it.operationalOutcome == DerivativeOperationalOutcome.USABLE &&
                    it.contentAvailable &&
                    // A local-preliminary OCR derivative is diagnostic material only.  It must
                    // never satisfy this production projection: doing so exposes the synthetic
                    // persisted capability (whose historical retrieval executor is local-only)
                    // as the selected mechanism for an OCR-required source.  Only a derivative
                    // already admitted as externally authoritative may suppress a fresh
                    // external-OCR decision.
                    it.authority == OcrAuthorityClassification.EXTERNAL_AUTHORITATIVE &&
                    DerivativeTransformation.OCR in it.transformationHistory &&
                    it.completenessState in setOf(
                        DerivativeCompletenessState.ACCOUNTED_FOR,
                        DerivativeCompletenessState.ACCOUNTED_FOR_WITH_QUALIFICATIONS,
                    )
            }
            ?.singleOrNull()

    private fun unavailable(id: EvidenceArtifactId, reason: String) =
        GovernedAcquisitionOwnerEvaluation.SourceUnavailable(id, reason)
}
