package parker.core.runtime

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import parker.core.interfaces.AcceptedEvidenceArtifact
import parker.core.interfaces.CandidateEvidenceArtifact
import parker.core.interfaces.CandidateProvenance
import parker.core.interfaces.ContentNature
import parker.core.interfaces.DecisionId
import parker.core.interfaces.EvidenceAcceptanceResult
import parker.core.interfaces.PermissionAction
import parker.core.interfaces.PermissionDecision
import parker.core.interfaces.PermissionDecisionOutcome
import parker.core.interfaces.PermissionLevel
import parker.core.interfaces.PrincipalId

/**
 * Evidence Custodian, Implementation Plan Phase 6 ("Derivative relationship
 * support") -- behavioural verification only. The architectural review
 * concluded that Phase 6 is already implemented: every Scope Lock Section 6
 * and Contract Design Section 4/6.2 requirement traces to already-existing
 * code ([DefaultEvidenceCustodian], [InMemoryMemoryCore],
 * [EvidenceRegistrationCoordinator]), with the sole gap being that no test
 * demonstrated it end-to-end. This file closes that gap. No production code
 * is changed, added, or modified by this file.
 *
 * Uses the real [DefaultEvidenceCustodian], [EvidenceRegistrationCoordinator],
 * [InMemoryMemoryCore], and [InMemoryEvidenceArtifactStorage] -- never a
 * hand-written fake for any of these. The one fake used,
 * [FakePermissionEngine] (already existing, `tests/runtime/FakePermissionEngine.kt`),
 * is unavoidable: no production [parker.core.interfaces.PermissionEngine]
 * wiring exists yet for the Evidence Custodian or Coordinator's own
 * disclosed, unregistered resource/action conventions (Phase 10, not yet
 * begun) -- a real [DefaultPermissionEngine]/[DefaultPermissionPolicy] would
 * deny every request through this path by design (their own documented
 * conservative "Unknown Resource" -> `DENIED` behaviour), which would test
 * nothing about Phase 6 itself. [FakePermissionEngine] is configured here
 * only to approve every request, so what is actually being exercised is the
 * real subsystems' own orchestration and identity/traceability behaviour,
 * not permission policy content.
 */
class EvidenceDerivativeRelationshipTest {

    private val fixedInstant: Instant = Instant.parse("2026-01-01T00:00:00Z")
    private val principalId = PrincipalId("owner-1")

    private fun approvingPermissionEngine() = FakePermissionEngine { request ->
        PermissionDecision(
            decisionId = DecisionId("dec-${request.requestId.value}"),
            principalId = request.principalId,
            resourceId = request.targetResources.first(),
            action = PermissionAction.WRITE,
            decision = PermissionDecisionOutcome.APPROVED,
            level = PermissionLevel.AUTOMATIC,
            timestamp = fixedInstant,
        )
    }

    private fun coordinator(permissionEngine: FakePermissionEngine): EvidenceRegistrationCoordinator {
        val storage = InMemoryEvidenceArtifactStorage()
        val evidenceCustodian = DefaultEvidenceCustodian(storage, permissionEngine)
        val memoryCore = InMemoryMemoryCore()
        return EvidenceRegistrationCoordinator(evidenceCustodian, memoryCore, permissionEngine)
    }

    // ================= 1. Original registration succeeds =================

    @Test
    fun `original registration succeeds against the real subsystems`() = runTest {
        val coordinator = coordinator(approvingPermissionEngine())

        val outcome = coordinator.register(
            requestingPrincipalId = principalId,
            correlationId = "corr-original",
            candidateEvidenceArtifact = CandidateEvidenceArtifact(byteArrayOf(1, 2, 3)),
            candidateProvenance = CandidateProvenance(
                sourceIdentifier = "scan-1",
                sourceType = "upload",
                acquisitionTime = fixedInstant,
                contentNature = ContentNature.ORIGINAL,
            ),
            documentType = "pdf",
        )

        assertIs<EvidenceRegistrationOutcome.Registered>(outcome)
    }

    // ================= 2 & 3. Derivative registration succeeds, with distinct identifiers =================

    @Test
    fun `derivative registration succeeds and receives a distinct EvidenceArtifactId, Provenance, and Document from the original`() = runTest {
        val coordinator = coordinator(approvingPermissionEngine())

        val originalOutcome = coordinator.register(
            requestingPrincipalId = principalId,
            correlationId = "corr-original",
            candidateEvidenceArtifact = CandidateEvidenceArtifact(byteArrayOf(1, 2, 3)),
            candidateProvenance = CandidateProvenance(
                sourceIdentifier = "scan-1",
                sourceType = "upload",
                acquisitionTime = fixedInstant,
                contentNature = ContentNature.ORIGINAL,
            ),
            documentType = "pdf",
        )
        val original = assertIs<EvidenceRegistrationOutcome.Registered>(originalOutcome)

        val derivativeOutcome = coordinator.register(
            requestingPrincipalId = principalId,
            correlationId = "corr-derivative",
            candidateEvidenceArtifact = CandidateEvidenceArtifact(byteArrayOf(4, 5, 6)),
            candidateProvenance = CandidateProvenance(
                sourceIdentifier = "scan-1-ocr",
                sourceType = "ocr-output",
                acquisitionTime = fixedInstant,
                contentNature = ContentNature.EXTRACTED,
                derivedFrom = listOf(original.provenance.provenanceId),
                extractedFrom = original.document.documentId,
            ),
            documentType = "ocr-text",
        )
        val derivative = assertIs<EvidenceRegistrationOutcome.Registered>(derivativeOutcome)

        assertNotEquals(
            original.acceptedEvidenceArtifact.evidenceArtifactId,
            derivative.acceptedEvidenceArtifact.evidenceArtifactId,
            "original and derivative must receive distinct EvidenceArtifactIds",
        )
        assertNotEquals(
            original.provenance.provenanceId,
            derivative.provenance.provenanceId,
            "original and derivative must receive distinct Provenance records",
        )
        assertNotEquals(
            original.document.documentId,
            derivative.document.documentId,
            "original and derivative must receive distinct Document records",
        )
    }

    // ================= 4. Derivative Provenance preserves derivedFrom/extractedFrom =================

    @Test
    fun `derivative Provenance correctly preserves derivedFrom and extractedFrom, naming the original's own already-minted identifiers`() = runTest {
        val coordinator = coordinator(approvingPermissionEngine())

        val originalOutcome = coordinator.register(
            requestingPrincipalId = principalId,
            correlationId = "corr-original",
            candidateEvidenceArtifact = CandidateEvidenceArtifact(byteArrayOf(1, 2, 3)),
            candidateProvenance = CandidateProvenance(
                sourceIdentifier = "scan-1",
                sourceType = "upload",
                acquisitionTime = fixedInstant,
                contentNature = ContentNature.ORIGINAL,
            ),
            documentType = "pdf",
        )
        val original = assertIs<EvidenceRegistrationOutcome.Registered>(originalOutcome)

        val derivativeOutcome = coordinator.register(
            requestingPrincipalId = principalId,
            correlationId = "corr-derivative",
            candidateEvidenceArtifact = CandidateEvidenceArtifact(byteArrayOf(4, 5, 6)),
            candidateProvenance = CandidateProvenance(
                sourceIdentifier = "scan-1-ocr",
                sourceType = "ocr-output",
                acquisitionTime = fixedInstant,
                contentNature = ContentNature.EXTRACTED,
                derivedFrom = listOf(original.provenance.provenanceId),
                extractedFrom = original.document.documentId,
            ),
            documentType = "ocr-text",
        )
        val derivative = assertIs<EvidenceRegistrationOutcome.Registered>(derivativeOutcome)

        assertEquals(
            listOf(original.provenance.provenanceId),
            derivative.provenance.derivedFrom,
            "the derivative's own Provenance.derivedFrom must name exactly the original's own ProvenanceId",
        )
        assertEquals(
            original.document.documentId,
            derivative.provenance.extractedFrom,
            "the derivative's own Provenance.extractedFrom must name exactly the original's own DocumentId",
        )
    }

    // ================= 5, 6 & 7. No independent traceability -- all traceability is via Memory Core Provenance =================

    @Test
    fun `no Evidence-Custodian-side or Coordinator-side type declares an independent traceability field -- all traceability exists solely through Memory Core Provenance`() {
        // The forbidden name fragments below would indicate a parallel, Custodian- or
        // Coordinator-owned relationship mechanism -- exactly what Scope Lock Section 6
        // forbids ("no parallel or custodian-owned traceability mechanism is authorised or
        // required"). Mirrors EvidenceCustodianScopeTest.kt's own existing field-name-check
        // convention.
        val forbiddenFragments = listOf(
            "derivedfrom", "extractedfrom", "linkedto", "relatedto",
            "parentid", "originalid", "derivativeof", "traceability",
        )

        val typesToCheck = listOf(
            AcceptedEvidenceArtifact::class.java,
            EvidenceAcceptanceResult.Accepted::class.java,
            EvidenceAcceptanceResult.Rejected::class.java,
            EvidenceRegistrationOutcome.Registered::class.java,
            EvidenceRegistrationOutcome.NotAccepted::class.java,
            EvidenceRegistrationOutcome.ProvenanceNotAuthorised::class.java,
            EvidenceRegistrationOutcome.DocumentRegistrationNotAuthorised::class.java,
            EvidenceRegistrationCoordinator::class.java,
        )

        typesToCheck.forEach { type ->
            val fieldNames = type.declaredFields.filter { !it.isSynthetic }.map { it.name.lowercase() }
            forbiddenFragments.forEach { forbidden ->
                assertTrue(
                    fieldNames.none { it.contains(forbidden) },
                    "${type.simpleName} must not carry a field related to '$forbidden' -- any relationship " +
                        "between an original and a derivative must exist solely through Memory Core's own " +
                        "Provenance.derivedFrom/extractedFrom fields, never through a field on an " +
                        "Evidence-Custodian-side or Coordinator-side type",
                )
            }
        }

        // Positive confirmation, not merely an absence check: EvidenceRegistrationOutcome.Registered's
        // only relationship-bearing value is `provenance`, and its declared type is exactly
        // Memory Core's own Provenance class -- confirming where traceability actually lives,
        // not only where it does not.
        val provenanceField = EvidenceRegistrationOutcome.Registered::class.java
            .getDeclaredField("provenance")
        assertEquals(
            "parker.core.interfaces.Provenance",
            provenanceField.type.name,
            "EvidenceRegistrationOutcome.Registered's traceability-bearing field must be Memory Core's own Provenance type",
        )
    }
}
