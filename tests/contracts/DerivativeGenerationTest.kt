package parker.core.interfaces

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DerivativeGenerationTest {
    @Test
    fun `blank generation identity is rejected`() {
        assertFailsWith<IllegalArgumentException> { DerivativeGenerationId(" ") }
    }

    @Test
    fun `generation identity retains opaque value exactly`() {
        assertEquals("opaque_Value-7", DerivativeGenerationId("opaque_Value-7").value)
    }

    @Test
    fun `record preserves immutable lineage and governed facts`() {
        val record = record()
        assertEquals(EvidenceArtifactId("source-1"), record.rootSourceEvidenceArtifactId)
        assertEquals(listOf(DerivativeParentReference.RootEvidenceArtifact(EvidenceArtifactId("source-1"))), record.parents)
        assertEquals(listOf(DerivativeTransformation.STRUCTURAL_PARSING), record.transformationHistory)
        assertEquals(DerivativeContentIdentity.NoCanonicalSerialization, record.contentIdentity)
    }

    @Test
    fun `candidate has no admitted identity or downstream authority fields`() {
        val names = CandidateDerivativeGeneration::class.members.map { it.name }.toSet()
        assertEquals(false, "derivativeGenerationId" in names)
        assertEquals(false, "documentId" in names)
        assertEquals(false, "reviewState" in names)
        assertEquals(false, "evidentialState" in names)
    }

    @Test
    fun `direct evidence parent must equal the declared root source`() {
        val record = record()
        assertFailsWith<IllegalArgumentException> {
            record.copy(
                parents = listOf(
                    DerivativeParentReference.RootEvidenceArtifact(EvidenceArtifactId("different-source")),
                ),
            )
        }
    }

    @Test
    fun `multiple parents are limited to parent generation reconciliation`() {
        val record = record()
        assertFailsWith<IllegalArgumentException> {
            record.copy(
                parents = listOf(
                    DerivativeParentReference.RootEvidenceArtifact(EvidenceArtifactId("source-1")),
                    DerivativeParentReference.ChildSourceEvidenceArtifact(EvidenceArtifactId("source-1")),
                ),
            )
        }
    }

    @Test
    fun `Tier B transformation requires model identity and version`() {
        val record = record()
        assertFailsWith<IllegalArgumentException> {
            record.copy(transformationHistory = listOf(DerivativeTransformation.OCR))
        }
    }

    companion object {
        fun record(id: String = "generation-1") = DerivativeGenerationRecord(
            derivativeGenerationId = DerivativeGenerationId(id),
            rootSourceEvidenceArtifactId = EvidenceArtifactId("source-1"),
            parents = listOf(DerivativeParentReference.RootEvidenceArtifact(EvidenceArtifactId("source-1"))),
            derivativeKind = "CSV structure",
            producerIdentity = DerivativeProducerIdentity(
                pluginIdentity = "test-parser",
                pluginVersion = "1.0",
                configurationIdentity = "test-config-v1",
                adapterIdentity = "test-adapter",
                adapterVersion = "1.0",
            ),
            transformationHistory = listOf(DerivativeTransformation.STRUCTURAL_PARSING),
            generatedAt = Instant.parse("2026-08-23T00:00:00Z"),
            contentIdentity = DerivativeContentIdentity.NoCanonicalSerialization,
            completenessState = DerivativeCompletenessState.ACCOUNTED_FOR,
            operationalOutcome = DerivativeOperationalOutcome.USABLE,
            warnings = emptyList(),
        )
    }
}
