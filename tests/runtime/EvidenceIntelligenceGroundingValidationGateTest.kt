package parker.core.runtime

import parker.core.interfaces.EvidenceAnalysisRequest
import parker.core.interfaces.EvidenceAnalysisResult
import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.EvidenceRetrievalResult
import parker.core.interfaces.Entity
import parker.core.interfaces.EntityId
import parker.core.interfaces.MemoryCoreRecord
import parker.core.interfaces.ProvenanceId
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.RelationshipEndpoint
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class EvidenceIntelligenceGroundingValidationGateTest {

    private val principal = PrincipalId("owner-grounding-test")

    private fun request(
        evidence: List<EvidenceArtifactId> = emptyList(),
        memory: List<RelationshipEndpoint> = emptyList(),
    ) = EvidenceAnalysisRequest(
        analysisKind = "comparison",
        requestingPrincipalId = principal,
        evidenceArtifactIds = evidence,
        memoryCoreReferences = memory,
    )

    private fun output(
        evidence: List<EvidenceArtifactId> = emptyList(),
        memory: List<RelationshipEndpoint> = emptyList(),
    ) = EvidenceAnalysisResult.TransientOutput(
        text = "[MODEL] finding",
        evidenceArtifactReferences = evidence,
        memoryCoreReferences = memory,
    )

    @Test
    fun `valid resolved EvidenceArtifactId passes unchanged`() {
        val artifact = EvidenceArtifactId("artifact-resolved")
        val result = output(evidence = listOf(artifact))

        val validated = EvidenceIntelligenceGroundingValidationGate.validate(
            request( evidence = listOf(artifact)),
            listOf(EvidenceRetrievalResult.Found(artifact, byteArrayOf(1))),
            emptyList(),
            listOf(result),
        )

        assertSame(result, validated.single())
    }

    @Test
    fun `valid resolved Memory Core reference passes unchanged`() {
        val reference = RelationshipEndpoint(RelationshipEndpoint.ENTITY, "entity-resolved")
        val record: MemoryCoreRecord = MemoryCoreRecord.OfEntity(
            Entity(
                entityId = EntityId("entity-resolved"),
                entityType = "person",
                primaryLabel = "Resolved",
                provenanceId = ProvenanceId("provenance-resolved"),
                createdAt = Instant.EPOCH,
            ),
        )
        val result = output(memory = listOf(reference))

        val validated = EvidenceIntelligenceGroundingValidationGate.validate(
            request(memory = listOf(reference)),
            emptyList(),
            listOf(reference to record),
            listOf(result),
        )

        assertSame(result, validated.single())
    }

    @Test
    fun `fabricated EvidenceArtifactId fails closed`() {
        val requested = EvidenceArtifactId("artifact-requested")
        val fabricated = EvidenceArtifactId("artifact-fabricated")

        assertFailsWith<IllegalStateException> {
            EvidenceIntelligenceGroundingValidationGate.validate(
                request(evidence = listOf(requested)),
                listOf(EvidenceRetrievalResult.Found(requested, byteArrayOf(1))),
                emptyList(),
                listOf(output(evidence = listOf(fabricated))),
            )
        }
    }

    @Test
    fun `fabricated Memory Core reference fails closed`() {
        val requested = RelationshipEndpoint(RelationshipEndpoint.ENTITY, "entity-requested")
        val fabricated = RelationshipEndpoint(RelationshipEndpoint.ENTITY, "entity-fabricated")
        val record: MemoryCoreRecord = MemoryCoreRecord.OfEntity(
            Entity(
                entityId = EntityId("entity-requested"),
                entityType = "person",
                primaryLabel = "Requested",
                provenanceId = ProvenanceId("provenance-requested"),
                createdAt = Instant.EPOCH,
            ),
        )

        assertFailsWith<IllegalStateException> {
            EvidenceIntelligenceGroundingValidationGate.validate(
                request(memory = listOf(requested)),
                emptyList(),
                listOf(requested to record),
                listOf(output(memory = listOf(fabricated))),
            )
        }
    }

    @Test
    fun `requested but unresolved references fail closed`() {
        val artifact = EvidenceArtifactId("artifact-not-found")
        val reference = RelationshipEndpoint(RelationshipEndpoint.ENTITY, "entity-not-found")

        assertFailsWith<IllegalStateException> {
            EvidenceIntelligenceGroundingValidationGate.validate(
                request(evidence = listOf(artifact), memory = listOf(reference)),
                listOf(EvidenceRetrievalResult.NotFound(artifact)),
                listOf(reference to null),
                listOf(output(evidence = listOf(artifact), memory = listOf(reference))),
            )
        }
    }

    @Test
    fun `a reference resolved in another invocation is not valid for this invocation`() {
        val otherInvocationArtifact = EvidenceArtifactId("artifact-other-invocation")

        assertFailsWith<IllegalStateException> {
            EvidenceIntelligenceGroundingValidationGate.validate(
                request(),
                emptyList(),
                emptyList(),
                listOf(output(evidence = listOf(otherInvocationArtifact))),
            )
        }
    }

    @Test
    fun `TransientOutput with no governed reference remains rejected by its existing invariant`() {
        assertFailsWith<IllegalArgumentException> {
            EvidenceAnalysisResult.TransientOutput(text = "[MODEL] untraceable finding")
        }
    }

    @Test
    fun `invalid output is rejected before a downstream acceptance or delivery step`() {
        val requested = EvidenceArtifactId("artifact-requested")
        val fabricated = EvidenceArtifactId("artifact-fabricated")
        var downstreamCalls = 0

        assertFailsWith<IllegalStateException> {
            val validated = EvidenceIntelligenceGroundingValidationGate.validate(
                request(evidence = listOf(requested)),
                listOf(EvidenceRetrievalResult.Found(requested, byteArrayOf(1))),
                emptyList(),
                listOf(output(evidence = listOf(fabricated))),
            )
            downstreamCalls += validated.size
        }

        assertEquals(0, downstreamCalls)
    }
}
