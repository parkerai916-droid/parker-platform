package parker.core.interfaces

import kotlin.reflect.KParameter
import kotlin.reflect.KVisibility
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.declaredMemberProperties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Programme 3, Knowledge Memory, Implementation Unit 9.1 (Knowledge Query
 * and Knowledge Result Contracts). Structural and pure data-shape tests
 * for [KnowledgeRetrievalQuery], [KnowledgeResultEntry],
 * [KnowledgeRetrievalResult], [KnowledgeRetrievalDisposition], and
 * [KnowledgeRetrieval], mirroring `KnowledgeSubmissionScopeTest.kt`'s own
 * reflection-based discipline for structural claims and
 * `KnowledgeLifecycleEventTest.kt`'s own discipline for pure data-shape
 * claims. This suite proves only the type contract the Unit 9 Contract
 * Design §4 and the Unit 9 Implementation Plan's own Unit 9.1 require; it
 * does not exercise query execution, filtering, ordering, staleness
 * detection, or permission enforcement -- none of that is implemented by
 * this Unit, and none of it is exercised here.
 */
class KnowledgeRetrievalContractsTest {

    // --- KnowledgeRetrievalQuery: construction-time validation ---

    @Test
    fun `a KnowledgeRetrievalQuery with blank relevance is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            KnowledgeRetrievalQuery(relevance = "", correlationId = "corr-1", maximumResults = 5)
        }
        assertFailsWith<IllegalArgumentException> {
            KnowledgeRetrievalQuery(relevance = "   ", correlationId = "corr-1", maximumResults = 5)
        }
    }

    @Test
    fun `a KnowledgeRetrievalQuery with blank correlationId is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            KnowledgeRetrievalQuery(relevance = "shopping list", correlationId = "", maximumResults = 5)
        }
        assertFailsWith<IllegalArgumentException> {
            KnowledgeRetrievalQuery(relevance = "shopping list", correlationId = "   ", maximumResults = 5)
        }
    }

    @Test
    fun `a KnowledgeRetrievalQuery with a non-positive maximumResults is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            KnowledgeRetrievalQuery(relevance = "shopping list", correlationId = "corr-1", maximumResults = 0)
        }
        assertFailsWith<IllegalArgumentException> {
            KnowledgeRetrievalQuery(relevance = "shopping list", correlationId = "corr-1", maximumResults = -1)
        }
    }

    @Test
    fun `a KnowledgeRetrievalQuery with maximumResults of exactly 1 is accepted`() {
        val query = KnowledgeRetrievalQuery(relevance = "shopping list", correlationId = "corr-1", maximumResults = 1)
        assertEquals(1, query.maximumResults)
    }

    @Test
    fun `a valid KnowledgeRetrievalQuery preserves every field unchanged`() {
        val query = KnowledgeRetrievalQuery(relevance = "grocery preferences", correlationId = "corr-42", maximumResults = 10)

        assertEquals("grocery preferences", query.relevance)
        assertEquals("corr-42", query.correlationId)
        assertEquals(10, query.maximumResults)
    }

    @Test
    fun `includeRetired defaults to false, so an existing three-argument construction site keeps its new, deliberate exclusion behaviour`() {
        val query = KnowledgeRetrievalQuery(relevance = "grocery preferences", correlationId = "corr-42", maximumResults = 10)

        assertFalse(query.includeRetired)
    }

    @Test
    fun `includeRetired is preserved unchanged when explicitly set true`() {
        val query = KnowledgeRetrievalQuery(
            relevance = "grocery preferences",
            correlationId = "corr-42",
            maximumResults = 10,
            includeRetired = true,
        )

        assertTrue(query.includeRetired)
    }

    // --- KnowledgeRetrievalQuery: no ranking, semantic, or permission field ---

    @Test
    fun `KnowledgeRetrievalQuery carries exactly relevance, correlationId, maximumResults, and includeRetired -- no other field`() {
        val declaredProperties = KnowledgeRetrievalQuery::class.declaredMemberProperties.map { it.name }.toSet()

        assertEquals(
            setOf("relevance", "correlationId", "maximumResults", "includeRetired"),
            declaredProperties,
            "KnowledgeRetrievalQuery must declare exactly these four properties -- found: $declaredProperties",
        )
    }

    @Test
    fun `includeRetired is a non-nullable Boolean -- a structural criterion, never a nullable permission-shaped field`() {
        val includeRetiredProperty = KnowledgeRetrievalQuery::class.declaredMemberProperties
            .single { it.name == "includeRetired" }

        assertEquals(Boolean::class, includeRetiredProperty.returnType.classifier)
        assertFalse(includeRetiredProperty.returnType.isMarkedNullable)
    }

    @Test
    fun `no ranking, semantic, or permission-assertion property exists on KnowledgeRetrievalQuery`() {
        val declaredProperties = KnowledgeRetrievalQuery::class.declaredMemberProperties.map { it.name.lowercase() }
        listOf(
            // ranking / scoring
            "rank", "score", "weight", "order", "sort", "priority",
            // semantic matching
            "embedding", "vector", "semantic", "similarity",
            // permission assertion embedded on the payload
            "approved", "authorised", "authorized", "decision", "permission", "requestingprincipalid",
        ).forEach { forbidden ->
            assertFalse(
                declaredProperties.any { it.contains(forbidden) },
                "KnowledgeRetrievalQuery must not declare a property resembling '$forbidden' -- found: $declaredProperties",
            )
        }
    }

    // --- KnowledgeResultEntry ---

    private val knowledgeItem = KnowledgeItem(
        knowledgeId = KnowledgeId("item-1"),
        evidenceReference = MemoryCoreRecordReference.ToEntity(EntityId("entity-1")),
        provenanceReference = ProvenanceReference(ProvenanceId("prov-1")),
        evidentialState = EvidentialState.UNKNOWN,
    )

    @Test
    fun `KnowledgeResultEntry carries its item and staleness disclosure unchanged`() {
        val entry = KnowledgeResultEntry(item = knowledgeItem, staleness = StalenessDisclosure.POSSIBLY_STALE)

        assertEquals(knowledgeItem, entry.item)
        assertEquals(StalenessDisclosure.POSSIBLY_STALE, entry.staleness)
    }

    @Test
    fun `KnowledgeResultEntry staleness is a non-nullable StalenessDisclosure -- always present, never absent`() {
        val stalenessProperty = KnowledgeResultEntry::class.declaredMemberProperties.single { it.name == "staleness" }

        assertEquals(StalenessDisclosure::class, stalenessProperty.returnType.classifier)
        assertFalse(
            stalenessProperty.returnType.isMarkedNullable,
            "staleness must be non-nullable -- an absent staleness disclosure is not a representable state",
        )
    }

    @Test
    fun `StalenessDisclosure is closed to exactly four variants -- two reserved, two assignable by Unit 9-3's own mechanism`() {
        val variantNames = StalenessDisclosure::class.java.enumConstants.map { it.name }.toSet()

        assertEquals(
            setOf("CONFIRMED_CURRENT", "CONFIRMED_STALE", "POSSIBLY_STALE", "INDETERMINATE"),
            variantNames,
            "StalenessDisclosure must declare exactly these four values -- found: $variantNames",
        )
    }

    // --- KnowledgeRetrievalResult ---

    @Test
    fun `a KnowledgeRetrievalResult with no entries constructs validly -- an empty result is not an error`() {
        val result = KnowledgeRetrievalResult(entries = emptyList())

        assertEquals(emptyList<KnowledgeResultEntry>(), result.entries)
    }

    @Test
    fun `a KnowledgeRetrievalResult preserves entry order exactly as given, with no re-ordering`() {
        val first = KnowledgeResultEntry(item = knowledgeItem, staleness = StalenessDisclosure.INDETERMINATE)
        val second = KnowledgeResultEntry(
            item = knowledgeItem.copy(knowledgeId = KnowledgeId("item-2")),
            staleness = StalenessDisclosure.POSSIBLY_STALE,
        )

        val result = KnowledgeRetrievalResult(entries = listOf(first, second))

        assertEquals(listOf(first, second), result.entries)
    }

    // --- KnowledgeRetrievalDisposition: closed to exactly two variants ---

    @Test
    fun `KnowledgeRetrievalDisposition is closed to exactly two variants -- Retrieved and NotAuthorised`() {
        val variantNames = KnowledgeRetrievalDisposition::class.sealedSubclasses.map { it.simpleName }.toSet()

        assertEquals(
            setOf("Retrieved", "NotAuthorised"),
            variantNames,
            "KnowledgeRetrievalDisposition must declare exactly Retrieved and NotAuthorised -- found: $variantNames",
        )
    }

    @Test
    fun `an exhaustive when over KnowledgeRetrievalDisposition compiles without an else branch`() {
        // Source-level closure proof, mirroring KnowledgeLifecycleEventTest's own identical
        // discipline: this would fail to compile were a third variant ever added.
        val dispositions: List<KnowledgeRetrievalDisposition> = listOf(
            KnowledgeRetrievalDisposition.Retrieved(KnowledgeRetrievalResult(emptyList())),
            KnowledgeRetrievalDisposition.NotAuthorised("permission denied"),
        )
        dispositions.forEach { disposition ->
            val describedKind = when (disposition) {
                is KnowledgeRetrievalDisposition.Retrieved -> "retrieved"
                is KnowledgeRetrievalDisposition.NotAuthorised -> "not-authorised"
            }
            assertTrue(describedKind.isNotBlank())
        }
    }

    @Test
    fun `a NotAuthorised with a blank reason is rejected at construction`() {
        assertFailsWith<IllegalArgumentException> { KnowledgeRetrievalDisposition.NotAuthorised("") }
        assertFailsWith<IllegalArgumentException> { KnowledgeRetrievalDisposition.NotAuthorised("   ") }
    }

    @Test
    fun `Retrieved with an empty result is structurally distinct from NotAuthorised -- emptiness never implies denial`() {
        val emptyRetrieval: KnowledgeRetrievalDisposition =
            KnowledgeRetrievalDisposition.Retrieved(KnowledgeRetrievalResult(emptyList()))
        val denial: KnowledgeRetrievalDisposition = KnowledgeRetrievalDisposition.NotAuthorised("not permitted")

        assertTrue(emptyRetrieval is KnowledgeRetrievalDisposition.Retrieved)
        assertTrue(denial is KnowledgeRetrievalDisposition.NotAuthorised)
        assertFalse(emptyRetrieval == denial)
    }

    // --- KnowledgeRetrieval: interface shape ---

    @Test
    fun `KnowledgeRetrieval declares exactly one domain operation -- retrieve`() {
        val declared = KnowledgeRetrieval::class.declaredFunctions

        assertEquals(
            setOf("retrieve"),
            declared.map { it.name }.toSet(),
            "KnowledgeRetrieval must declare exactly retrieve -- found: ${declared.map { it.name }}",
        )
        assertEquals(1, declared.size, "no domain operation name may be declared more than once")
    }

    @Test
    fun `retrieve has the expected public, suspend shape`() {
        val retrieve = KnowledgeRetrieval::class.declaredFunctions.single { it.name == "retrieve" }

        assertEquals(KVisibility.PUBLIC, retrieve.visibility, "retrieve must be a public operation")
        assertTrue(retrieve.isSuspend, "retrieve must be a suspend function -- Permission Engine evaluation is async")

        val valueParameters = retrieve.parameters.filter { it.kind == KParameter.Kind.VALUE }
        assertEquals(2, valueParameters.size, "retrieve must take exactly two value parameters")
        assertEquals(
            PrincipalId::class,
            valueParameters[0].type.classifier,
            "retrieve's first parameter must be the requesting principal, supplied explicitly",
        )
        assertEquals(
            KnowledgeRetrievalQuery::class,
            valueParameters[1].type.classifier,
            "retrieve's second parameter must be the KnowledgeRetrievalQuery",
        )
        assertEquals(
            KnowledgeRetrievalDisposition::class,
            retrieve.returnType.classifier,
            "retrieve must return the sealed KnowledgeRetrievalDisposition",
        )
    }

    @Test
    fun `no storage, submission, lifecycle, or evaluation-policy operation exists on KnowledgeRetrieval`() {
        val declaredNames = KnowledgeRetrieval::class.declaredFunctions.map { it.name }
        listOf(
            // storage mechanics
            "store", "persist", "find", "read", "write",
            // submission (Unit 8's own, closed responsibility)
            "submit",
            // lifecycle operations (Unit 7's own, closed responsibility)
            "revise", "retire", "restore",
            // evaluation policy (Unit 6's own, closed responsibility)
            "evaluate", "promote", "reject",
            // legacy path
            "remember", "recall", "forget",
        ).forEach { excludedName ->
            assertTrue(
                excludedName !in declaredNames,
                "KnowledgeRetrieval must not declare '$excludedName' -- found: $declaredNames",
            )
        }
    }
}
