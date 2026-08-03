package parker.core.interfaces

import kotlin.reflect.KClass
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.memberProperties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Evidence Intelligence, Implementation Unit 1 ("Input/Output Shape
 * Foundation"), reimplemented against the amended governance that
 * resolved GCR-EI-UNIT1-001. Construction-time and structural tests for
 * exactly the types `EvidenceIntelligence.kt`'s own Unit 1 KDoc
 * authorises -- [EvidenceAnalysisRequest], [EvidenceAnalysisResult] (and
 * its four variants), and [CandidateMemoryCoreRecord] -- proving this
 * Unit introduced exactly what its own governing instruction authorised,
 * and nothing this Unit's own instruction explicitly excluded (raw
 * evidence, confidence, evidential state, a fifth output category, a
 * separate claim type, a generic union type, acceptance behaviour,
 * retrieval behaviour, or any later-Unit interface) exists anywhere on
 * these types or in the compiled repository.
 */
class EvidenceIntelligenceTest {

    // ================= EvidenceAnalysisRequest =================

    private fun minimalRequest(
        analysisKind: String = "comparison",
        requestingPrincipalId: PrincipalId = PrincipalId("owner-1"),
    ) = EvidenceAnalysisRequest(
        analysisKind = analysisKind,
        requestingPrincipalId = requestingPrincipalId,
    )

    @Test
    fun `an EvidenceAnalysisRequest can be constructed with only mandatory fields`() {
        val request = minimalRequest()

        assertEquals("comparison", request.analysisKind)
        assertEquals(PrincipalId("owner-1"), request.requestingPrincipalId)
        assertEquals(emptyList(), request.evidenceArtifactIds)
        assertEquals(emptyList(), request.memoryCoreReferences)
        assertEquals(null, request.reasoningContext)
    }

    @Test
    fun `an EvidenceAnalysisRequest rejects a blank analysisKind`() {
        assertFailsWith<IllegalArgumentException> { minimalRequest(analysisKind = "") }
        assertFailsWith<IllegalArgumentException> { minimalRequest(analysisKind = "   ") }
    }

    @Test
    fun `an EvidenceAnalysisRequest may name no evidence artefact and no Memory Core reference -- rejecting that combination is a later Unit's own responsibility`() {
        // Contract Design Section 11 assigns rejection of "nothing to analyse" to a concrete
        // implementation of the operation (Unit 5), never to this shape's own constructor.
        val request = minimalRequest()

        assertTrue(request.evidenceArtifactIds.isEmpty())
        assertTrue(request.memoryCoreReferences.isEmpty())
    }

    @Test
    fun `EvidenceAnalysisRequest carries evidence and Memory Core references and an optional reasoning context`() {
        val request = EvidenceAnalysisRequest(
            analysisKind = "chronology",
            requestingPrincipalId = PrincipalId("owner-1"),
            evidenceArtifactIds = listOf(EvidenceArtifactId("artifact-1")),
            memoryCoreReferences = listOf(RelationshipEndpoint(RelationshipEndpoint.ASSERTION, "assertion-1")),
            reasoningContext = ReasoningContext(listOf("prior context entry")),
        )

        assertEquals(listOf(EvidenceArtifactId("artifact-1")), request.evidenceArtifactIds)
        assertEquals(
            listOf(RelationshipEndpoint(RelationshipEndpoint.ASSERTION, "assertion-1")),
            request.memoryCoreReferences,
        )
        assertEquals(ReasoningContext(listOf("prior context entry")), request.reasoningContext)
    }

    @Test
    fun `EvidenceAnalysisRequest exposes exactly the five authorised fields -- no confidence, no evidential state, no raw content`() {
        val expectedFields = setOf(
            "analysisKind",
            "requestingPrincipalId",
            "evidenceArtifactIds",
            "memoryCoreReferences",
            "reasoningContext",
        )

        assertEquals(expectedFields, EvidenceAnalysisRequest::class.memberProperties.map { it.name }.toSet())
    }

    @Test
    fun `EvidenceAnalysisRequest reuses existing Parker types for every reference field`() {
        val properties = EvidenceAnalysisRequest::class.declaredMemberProperties.associateBy { it.name }

        assertEquals(String::class, properties.getValue("analysisKind").returnType.classifier)
        assertEquals(PrincipalId::class, properties.getValue("requestingPrincipalId").returnType.classifier)
        assertEquals(List::class, properties.getValue("evidenceArtifactIds").returnType.classifier)
        assertEquals(
            EvidenceArtifactId::class,
            properties.getValue("evidenceArtifactIds").returnType.arguments.single().type?.classifier,
        )
        assertEquals(List::class, properties.getValue("memoryCoreReferences").returnType.classifier)
        assertEquals(
            RelationshipEndpoint::class,
            properties.getValue("memoryCoreReferences").returnType.arguments.single().type?.classifier,
        )
        assertEquals(ReasoningContext::class, properties.getValue("reasoningContext").returnType.classifier)
        assertTrue(
            properties.getValue("reasoningContext").returnType.isMarkedNullable,
            "reasoningContext must be nullable -- supplied only when analysis internally invokes a Reasoning Provider",
        )
    }

    @Test
    fun `no field anywhere on EvidenceAnalysisRequest carries raw evidence bytes, a confidence value, or an evidential-state value`() {
        assertNoExcludedField(EvidenceAnalysisRequest::class)
    }

    // ================= CandidateMemoryCoreRecord (the payload selector) =================

    @Test
    fun `CandidateMemoryCoreRecord declares exactly two cases -- OfAssertion and OfRelationship`() {
        val cases = CandidateMemoryCoreRecord::class.sealedSubclasses

        assertEquals(setOf("OfAssertion", "OfRelationship"), cases.map { it.simpleName }.toSet())
        assertEquals(2, cases.size, "no third case -- CandidateEntity and CandidateDocument are not authorised here")
    }

    @Test
    fun `CandidateMemoryCoreRecord wraps an existing, unmodified CandidateAssertion or CandidateRelationship`() {
        val candidateAssertion = CandidateAssertion(statement = "the document was signed on 2026-01-01", provenanceId = ProvenanceId("provenance-1"))
        val candidateRelationship = CandidateRelationship(
            relationshipType = Relationship.CONTRADICTS,
            fromEndpoint = RelationshipEndpoint(RelationshipEndpoint.ASSERTION, "assertion-1"),
            toEndpoint = RelationshipEndpoint(RelationshipEndpoint.ASSERTION, "assertion-2"),
            directional = false,
            provenanceId = ProvenanceId("provenance-1"),
        )

        val ofAssertion = CandidateMemoryCoreRecord.OfAssertion(candidateAssertion)
        val ofRelationship = CandidateMemoryCoreRecord.OfRelationship(candidateRelationship)

        assertEquals(candidateAssertion, ofAssertion.candidateAssertion)
        assertEquals(candidateRelationship, ofRelationship.candidateRelationship)
    }

    @Test
    fun `each CandidateMemoryCoreRecord case wraps its existing Parker candidate type unmodified, and carries no other field`() {
        val ofAssertionField = CandidateMemoryCoreRecord.OfAssertion::class.declaredMemberProperties.single()
        assertEquals("candidateAssertion", ofAssertionField.name)
        assertEquals(CandidateAssertion::class, ofAssertionField.returnType.classifier)

        val ofRelationshipField = CandidateMemoryCoreRecord.OfRelationship::class.declaredMemberProperties.single()
        assertEquals("candidateRelationship", ofRelationshipField.name)
        assertEquals(CandidateRelationship::class, ofRelationshipField.returnType.classifier)
    }

    @Test
    fun `CandidateMemoryCoreRecord is not a generic union abstraction -- it declares no type parameter`() {
        assertTrue(
            CandidateMemoryCoreRecord::class.typeParameters.isEmpty(),
            "CandidateMemoryCoreRecord must be closed to the two named candidate types, never a " +
                "type-parameterised mechanism reusable for any other pair of types",
        )
    }

    @Test
    fun `CandidateMemoryCoreRecord is sealed -- no third case can be added from outside this file`() {
        assertTrue(CandidateMemoryCoreRecord::class.isSealed)
    }

    // ================= EvidenceAnalysisResult =================

    @Test
    fun `EvidenceAnalysisResult declares exactly four variants -- one per CDR-007 Section 6 category`() {
        val variants = EvidenceAnalysisResult::class.sealedSubclasses

        assertEquals(
            setOf("TransientOutput", "CandidateArtifactProduced", "CandidateRecordProduced", "CandidateKnowledgeProduced"),
            variants.map { it.simpleName }.toSet(),
        )
        assertEquals(4, variants.size, "no fifth EvidenceAnalysisResult variant may exist")
    }

    @Test
    fun `EvidenceAnalysisResult is sealed -- no subclass outside this file is possible`() {
        assertTrue(
            EvidenceAnalysisResult::class.isSealed,
            "EvidenceAnalysisResult must remain sealed so no fifth variant can be added from outside this file",
        )
    }

    @Test
    fun `a TransientOutput requires non-blank text`() {
        assertFailsWith<IllegalArgumentException> {
            EvidenceAnalysisResult.TransientOutput(
                text = "",
                evidenceArtifactReferences = listOf(EvidenceArtifactId("artifact-1")),
            )
        }
        assertFailsWith<IllegalArgumentException> {
            EvidenceAnalysisResult.TransientOutput(
                text = "   ",
                evidenceArtifactReferences = listOf(EvidenceArtifactId("artifact-1")),
            )
        }
    }

    @Test
    fun `a TransientOutput requires at least one governed reference`() {
        assertFailsWith<IllegalArgumentException> {
            EvidenceAnalysisResult.TransientOutput(text = "an untraceable claim")
        }
    }

    @Test
    fun `a TransientOutput represents exactly one claim, traceable by either reference list`() {
        val withEvidenceReference = EvidenceAnalysisResult.TransientOutput(
            text = "the two accounts disagree on the meeting time",
            evidenceArtifactReferences = listOf(EvidenceArtifactId("artifact-1")),
        )
        val withMemoryCoreReference = EvidenceAnalysisResult.TransientOutput(
            text = "the two accounts disagree on the meeting time",
            memoryCoreReferences = listOf(RelationshipEndpoint(RelationshipEndpoint.ASSERTION, "assertion-1")),
        )

        assertEquals(listOf(EvidenceArtifactId("artifact-1")), withEvidenceReference.evidenceArtifactReferences)
        assertEquals(
            listOf(RelationshipEndpoint(RelationshipEndpoint.ASSERTION, "assertion-1")),
            withMemoryCoreReference.memoryCoreReferences,
        )
    }

    @Test
    fun `a multi-claim analysis is represented by multiple TransientOutput values, never one bundling several claims`() {
        val claimOne = EvidenceAnalysisResult.TransientOutput(
            text = "claim one",
            evidenceArtifactReferences = listOf(EvidenceArtifactId("artifact-1")),
        )
        val claimTwo = EvidenceAnalysisResult.TransientOutput(
            text = "claim two",
            memoryCoreReferences = listOf(RelationshipEndpoint(RelationshipEndpoint.ASSERTION, "assertion-1")),
        )

        val results: List<EvidenceAnalysisResult> = listOf(claimOne, claimTwo)

        assertEquals(2, results.size)
        assertEquals("claim one", (results[0] as EvidenceAnalysisResult.TransientOutput).text)
        assertEquals("claim two", (results[1] as EvidenceAnalysisResult.TransientOutput).text)
    }

    @Test
    fun `TransientOutput declares exactly the three authorised fields -- no internal claim list, no confidence, no evidential state`() {
        val expectedFields = setOf("text", "evidenceArtifactReferences", "memoryCoreReferences")

        assertEquals(
            expectedFields,
            EvidenceAnalysisResult.TransientOutput::class.declaredMemberProperties.map { it.name }.toSet(),
        )
        assertNoExcludedField(EvidenceAnalysisResult.TransientOutput::class)
    }

    @Test
    fun `no AnalyticalClaim, or any other separate claim type, exists anywhere in the compiled repository`() {
        assertFailsWith<ClassNotFoundException> { Class.forName("parker.core.interfaces.AnalyticalClaim") }
    }

    @Test
    fun `a CandidateArtifactProduced wraps an existing, unmodified CandidateEvidenceArtifact`() {
        val candidate = CandidateEvidenceArtifact(byteArrayOf(1, 2, 3))
        val result = EvidenceAnalysisResult.CandidateArtifactProduced(candidate)

        assertEquals(candidate, result.candidateEvidenceArtifact)
    }

    @Test
    fun `a CandidateRecordProduced wraps a CandidateMemoryCoreRecord`() {
        val candidateAssertion = CandidateAssertion(statement = "a claim", provenanceId = ProvenanceId("provenance-1"))
        val result = EvidenceAnalysisResult.CandidateRecordProduced(CandidateMemoryCoreRecord.OfAssertion(candidateAssertion))

        assertEquals(candidateAssertion, (result.candidateRecord as CandidateMemoryCoreRecord.OfAssertion).candidateAssertion)
    }

    @Test
    fun `a CandidateKnowledgeProduced wraps an existing, unmodified KnowledgeCandidate`() {
        val knowledgeCandidate = KnowledgeCandidate(
            evidenceReference = MemoryCoreRecordReference.ToAssertion(AssertionId("assertion-1")),
        )
        val result = EvidenceAnalysisResult.CandidateKnowledgeProduced(knowledgeCandidate)

        assertEquals(knowledgeCandidate, result.knowledgeCandidate)
    }

    @Test
    fun `each EvidenceAnalysisResult variant reuses an existing Parker type unmodified, wherever it carries one`() {
        val candidateArtifactField = EvidenceAnalysisResult.CandidateArtifactProduced::class
            .declaredMemberProperties.single { it.name == "candidateEvidenceArtifact" }
        assertEquals(CandidateEvidenceArtifact::class, candidateArtifactField.returnType.classifier)

        val candidateRecordField = EvidenceAnalysisResult.CandidateRecordProduced::class
            .declaredMemberProperties.single { it.name == "candidateRecord" }
        assertEquals(CandidateMemoryCoreRecord::class, candidateRecordField.returnType.classifier)

        val candidateKnowledgeField = EvidenceAnalysisResult.CandidateKnowledgeProduced::class
            .declaredMemberProperties.single { it.name == "knowledgeCandidate" }
        assertEquals(KnowledgeCandidate::class, candidateKnowledgeField.returnType.classifier)
    }

    @Test
    fun `no field anywhere on EvidenceAnalysisResult or its variants carries a caller-declared confidence or evidential-state value`() {
        assertNoExcludedField(EvidenceAnalysisResult::class)
        EvidenceAnalysisResult::class.sealedSubclasses.forEach { assertNoExcludedField(it) }
    }

    // ================= Programme-wide public runtime type count =================

    @Test
    fun `Unit 1 creates exactly the first three of the four eventually-authorised public runtime types`() {
        // The fourth, EvidenceIntelligence, is Unit 5's own responsibility -- see the test below.
        assertEquals("EvidenceAnalysisRequest", EvidenceAnalysisRequest::class.simpleName)
        assertEquals("EvidenceAnalysisResult", EvidenceAnalysisResult::class.simpleName)
        assertEquals("CandidateMemoryCoreRecord", CandidateMemoryCoreRecord::class.simpleName)
    }

    @Test
    fun `no EvidenceIntelligence operation interface exists yet -- it remains Unit 5's own responsibility`() {
        assertFailsWith<ClassNotFoundException> { Class.forName("parker.core.interfaces.EvidenceIntelligence") }
    }

    // ================= No acceptance behaviour, no retrieval behaviour, no later-Unit component =================

    @Test
    fun `EvidenceAnalysisRequest, EvidenceAnalysisResult, and CandidateMemoryCoreRecord declare no operation of any kind`() {
        val typesToCheck = listOf(
            EvidenceAnalysisRequest::class,
            EvidenceAnalysisResult::class,
            CandidateMemoryCoreRecord::class,
        ) + EvidenceAnalysisResult::class.sealedSubclasses + CandidateMemoryCoreRecord::class.sealedSubclasses

        typesToCheck.forEach { type ->
            val declaredNames = type.declaredFunctions.map { it.name }
            listOf(
                "accept", "submit", "write", "promote", "persist", "commit", "save",
                "retrieve", "resolve", "fetch", "read", "find", "search", "get", "load",
                "analyse", "analyze", "reason", "invoke", "orchestrate",
            ).forEach { excludedName ->
                assertTrue(
                    declaredNames.none { it.equals(excludedName, ignoreCase = true) },
                    "${type.simpleName} must not declare a '$excludedName' operation -- Unit 1 defines shapes " +
                        "only, no acceptance, retrieval, or analytical behaviour of any kind",
                )
            }
        }
    }

    // ================= Shared helper =================

    /**
     * Confirms no declared, non-synthetic property anywhere on [type] is named (or contains, as a
     * whole word) a term this Unit's own governing instruction excludes -- raw content, confidence,
     * or evidential state. Deliberately checks only [type]'s own declared properties, never those of
     * a wrapped, pre-existing Parker type ([CandidateAssertion.confidence], for example, is Memory
     * Core's own already-governed field, entirely outside Evidence Intelligence's own prohibition).
     */
    private fun assertNoExcludedField(type: KClass<*>) {
        val fieldNames = type.declaredMemberProperties.map { it.name.lowercase() }
        listOf("content", "bytes", "rawcontent", "rawevidence", "confidence", "evidentialstate", "evidential_state")
            .forEach { excluded ->
                assertTrue(
                    fieldNames.none { it == excluded || it.contains(excluded) },
                    "${type.simpleName} must not declare a field related to '$excluded'",
                )
            }
    }
}
