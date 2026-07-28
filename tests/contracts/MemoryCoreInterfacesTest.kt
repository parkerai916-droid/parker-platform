package parker.core.interfaces

import java.time.Instant
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.reflect.full.memberProperties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Programme 2, Memory Core, Implementation Unit 7 (as amended --
 * `docs/architecture/MEMORY_CORE_CONTRACT_DESIGN_ERRATA_003.md`).
 * Structural and signature-level tests for [MemoryCore] and
 * [MemoryRetrieval] themselves, and for every supporting query/result
 * type they directly require ([EntityLookupQuery], [DocumentLookupQuery],
 * [RelationshipTraversalQuery], [RelationshipTraversalDirection],
 * [ChronologicalLookupQuery], [MetadataLookupQuery],
 * [ProvenanceLookupQuery], [MemoryCoreRecord], and, following the Errata
 * 003 amendment, [MemoryCoreRecordReference] and
 * [MemoryCore.transitionStatus]). Neither interface has an
 * implementation anywhere yet -- these tests check shape, not behaviour:
 * no `InMemoryMemoryCore`, no persistence, no event publication, and no
 * permission evaluation exists to test. Candidate-type tests live
 * separately, in `MemoryCoreCandidatesTest.kt`.
 */
class MemoryCoreInterfacesTest {

    // ================= MemoryCore: exact write-operation surface =================

    @Test
    fun `MemoryCore exposes exactly the five candidate-to-record operations plus transitionStatus`() {
        val expectedNames = setOf(
            "createProvenance",
            "createEntity",
            "registerDocument",
            "createAssertion",
            "createRelationship",
            "transitionStatus",
        )

        assertEquals(expectedNames, MemoryCore::class.declaredMemberFunctions.map { it.name }.toSet())
    }

    @Test
    fun `every MemoryCore operation is a suspend function`() {
        MemoryCore::class.declaredMemberFunctions.forEach { function ->
            assertTrue(function.isSuspend, "MemoryCore.${function.name} must be a suspend function")
        }
    }

    @Test
    fun `each MemoryCore write operation accepts requestingPrincipalId first, then its own candidate type, and returns the matching stored record type -- Errata 004`() {
        val functionsByName = MemoryCore::class.declaredMemberFunctions.associateBy { it.name }

        fun assertShape(name: String, parameterType: KClass<*>, returnType: KClass<*>) {
            val function = functionsByName.getValue(name)
            val valueParameters = function.parameters.drop(1)
            assertEquals(2, valueParameters.size, "$name must accept exactly two value parameters after Errata 004")
            assertEquals(PrincipalId::class, valueParameters[0].type.classifier, "$name requestingPrincipalId parameter type")
            assertEquals(parameterType, valueParameters[1].type.classifier, "$name candidate parameter type")
            assertEquals(returnType, function.returnType.classifier, "$name return type")
        }

        assertShape("createProvenance", CandidateProvenance::class, Provenance::class)
        assertShape("createEntity", CandidateEntity::class, Entity::class)
        assertShape("registerDocument", CandidateDocument::class, Document::class)
        assertShape("createAssertion", CandidateAssertion::class, Assertion::class)
        assertShape("createRelationship", CandidateRelationship::class, Relationship::class)
    }

    @Test
    fun `transitionStatus accepts requestingPrincipalId, a MemoryCoreRecordReference, and a MemoryCoreRecordStatus, and returns a MemoryCoreRecord -- Errata 004`() {
        val function = MemoryCore::class.declaredMemberFunctions.single { it.name == "transitionStatus" }
        val valueParameters = function.parameters.drop(1)

        assertEquals(3, valueParameters.size, "transitionStatus must accept exactly three value parameters after Errata 004")
        assertEquals(PrincipalId::class, valueParameters[0].type.classifier, "transitionStatus requestingPrincipalId parameter type")
        assertEquals(MemoryCoreRecordReference::class, valueParameters[1].type.classifier, "transitionStatus reference parameter type")
        assertEquals(MemoryCoreRecordStatus::class, valueParameters[2].type.classifier, "transitionStatus targetStatus parameter type")
        assertEquals(MemoryCoreRecord::class, function.returnType.classifier, "transitionStatus return type")
        assertTrue(function.isSuspend, "transitionStatus must be a suspend function")
    }

    @Test
    fun `MemoryCore declares no supertype beyond Any -- no runtime, persistence, or permission base type`() {
        assertEquals(setOf(Any::class), MemoryCore::class.supertypes.map { it.classifier }.toSet())
    }

    @Test
    fun `no MemoryCore operation references any type whose name suggests permission, persistence, or runtime coupling`() {
        val referencedTypeNames = MemoryCore::class.declaredMemberFunctions.flatMap { function ->
            function.parameters.map { it.type.classifier } + function.returnType.classifier
        }.filterIsInstance<KClass<*>>().mapNotNull { it.simpleName?.lowercase() }

        val forbiddenSubstrings = listOf("permission", "runtime", "persist", "event", "repository", "database")
        referencedTypeNames.forEach { typeName ->
            forbiddenSubstrings.forEach { forbidden ->
                assertTrue(
                    forbidden !in typeName,
                    "MemoryCore must not reference a type named '$typeName' (contains forbidden substring '$forbidden')",
                )
            }
        }
    }

    // ================= MemoryRetrieval: exact retrieval-mode surface =================

    @Test
    fun `MemoryRetrieval exposes exactly the ten methods realising the seven approved retrieval modes`() {
        // 1. identifier lookup   -> getEntity, getDocument, getAssertion, getRelationship (4 methods, 1 mode)
        // 2. entity lookup       -> findEntities
        // 3. document lookup     -> findDocuments
        // 4. relationship traversal -> traverseRelationships
        // 5. chronological lookup   -> findByTimeRange
        // 6. metadata filtering     -> findByMetadata
        // 7. provenance-aware lookup -> findByProvenance
        val expectedNames = setOf(
            "getEntity",
            "getDocument",
            "getAssertion",
            "getRelationship",
            "findEntities",
            "findDocuments",
            "traverseRelationships",
            "findByTimeRange",
            "findByMetadata",
            "findByProvenance",
        )

        assertEquals(10, expectedNames.size)
        assertEquals(expectedNames, MemoryRetrieval::class.declaredMemberFunctions.map { it.name }.toSet())
    }

    @Test
    fun `every MemoryRetrieval operation is a suspend function`() {
        MemoryRetrieval::class.declaredMemberFunctions.forEach { function ->
            assertTrue(function.isSuspend, "MemoryRetrieval.${function.name} must be a suspend function")
        }
    }

    @Test
    fun `identifier lookup methods accept requestingPrincipalId first, then the matching identifier type, and return a nullable stored record -- Errata 004`() {
        val functionsByName = MemoryRetrieval::class.declaredMemberFunctions.associateBy { it.name }

        fun assertNullableShape(name: String, parameterType: KClass<*>, returnType: KClass<*>) {
            val function = functionsByName.getValue(name)
            val valueParameters = function.parameters.drop(1)
            assertEquals(2, valueParameters.size, "$name must accept exactly two value parameters after Errata 004")
            assertEquals(PrincipalId::class, valueParameters[0].type.classifier, "$name requestingPrincipalId parameter type")
            assertEquals(parameterType, valueParameters[1].type.classifier, "$name identifier parameter type")
            assertEquals(returnType, function.returnType.classifier, "$name return type")
            assertTrue(function.returnType.isMarkedNullable, "$name must return a nullable type")
        }

        assertNullableShape("getEntity", EntityId::class, Entity::class)
        assertNullableShape("getDocument", DocumentId::class, Document::class)
        assertNullableShape("getAssertion", AssertionId::class, Assertion::class)
        assertNullableShape("getRelationship", RelationshipId::class, Relationship::class)
    }

    @Test
    fun `list-returning retrieval methods accept their own query type and return a List`() {
        val functionsByName = MemoryRetrieval::class.declaredMemberFunctions.associateBy { it.name }

        fun assertListShape(name: String, parameterType: KClass<*>) {
            val function = functionsByName.getValue(name)
            assertEquals(parameterType, function.parameters.last().type.classifier, "$name parameter type")
            assertEquals(List::class, function.returnType.classifier, "$name return type")
        }

        assertListShape("findEntities", EntityLookupQuery::class)
        assertListShape("findDocuments", DocumentLookupQuery::class)
        assertListShape("traverseRelationships", RelationshipTraversalQuery::class)
        assertListShape("findByTimeRange", ChronologicalLookupQuery::class)
        assertListShape("findByMetadata", MetadataLookupQuery::class)
        assertListShape("findByProvenance", ProvenanceLookupQuery::class)
    }

    @Test
    fun `MemoryRetrieval declares no supertype beyond Any -- no runtime, persistence, or permission base type`() {
        assertEquals(setOf(Any::class), MemoryRetrieval::class.supertypes.map { it.classifier }.toSet())
    }

    @Test
    fun `no MemoryRetrieval operation references any type whose name suggests permission, persistence, or runtime coupling`() {
        val referencedTypeNames = MemoryRetrieval::class.declaredMemberFunctions.flatMap { function ->
            function.parameters.map { it.type.classifier } + function.returnType.classifier
        }.filterIsInstance<KClass<*>>().mapNotNull { it.simpleName?.lowercase() }

        val forbiddenSubstrings = listOf("permission", "runtime", "persist", "event", "repository", "database")
        referencedTypeNames.forEach { typeName ->
            forbiddenSubstrings.forEach { forbidden ->
                assertTrue(
                    forbidden !in typeName,
                    "MemoryRetrieval must not reference a type named '$typeName' (contains forbidden substring '$forbidden')",
                )
            }
        }
    }

    // ================= EntityLookupQuery =================

    @Test
    fun `an EntityLookupQuery can be constructed with only mandatory fields -- every filter genuinely optional`() {
        val query = EntityLookupQuery(requestingPrincipalId = PrincipalId("principal-1"), maximumResults = 10)

        assertNull(query.labelOrAliasMatch)
        assertNull(query.entityType)
        assertNull(query.status)
    }

    @Test
    fun `an EntityLookupQuery rejects maximumResults below 1 and a blank optional filter`() {
        assertFailsWith<IllegalArgumentException> {
            EntityLookupQuery(requestingPrincipalId = PrincipalId("principal-1"), maximumResults = 0)
        }
        assertFailsWith<IllegalArgumentException> {
            EntityLookupQuery(requestingPrincipalId = PrincipalId("principal-1"), maximumResults = 10, labelOrAliasMatch = "")
        }
    }

    // ================= DocumentLookupQuery =================

    @Test
    fun `a DocumentLookupQuery can be constructed with only mandatory fields`() {
        val query = DocumentLookupQuery(requestingPrincipalId = PrincipalId("principal-1"), maximumResults = 5)

        assertNull(query.documentType)
        assertNull(query.locationReferenceMatch)
        assertNull(query.processingStatus)
    }

    @Test
    fun `a DocumentLookupQuery rejects maximumResults below 1`() {
        assertFailsWith<IllegalArgumentException> {
            DocumentLookupQuery(requestingPrincipalId = PrincipalId("principal-1"), maximumResults = -1)
        }
    }

    // ================= RelationshipTraversalQuery =================

    @Test
    fun `a RelationshipTraversalQuery defaults direction to BOTH`() {
        val query = RelationshipTraversalQuery(
            requestingPrincipalId = PrincipalId("principal-1"),
            maximumResults = 5,
            startingEndpoint = RelationshipEndpoint(RelationshipEndpoint.ENTITY, "entity-1"),
        )

        assertEquals(RelationshipTraversalDirection.BOTH, query.direction)
    }

    @Test
    fun `RelationshipTraversalDirection has exactly the three expected values`() {
        assertEquals(
            setOf("FORWARD", "REVERSE", "BOTH"),
            RelationshipTraversalDirection.entries.map { it.name }.toSet(),
        )
    }

    @Test
    fun `a RelationshipTraversalQuery rejects maximumResults below 1`() {
        assertFailsWith<IllegalArgumentException> {
            RelationshipTraversalQuery(
                requestingPrincipalId = PrincipalId("principal-1"),
                maximumResults = 0,
                startingEndpoint = RelationshipEndpoint(RelationshipEndpoint.ENTITY, "entity-1"),
            )
        }
    }

    // ================= ChronologicalLookupQuery =================

    @Test
    fun `a ChronologicalLookupQuery accepts an unspecified recordKind, spanning all four kinds`() {
        val query = ChronologicalLookupQuery(
            requestingPrincipalId = PrincipalId("principal-1"),
            maximumResults = 5,
            from = Instant.parse("2026-01-01T00:00:00Z"),
            to = Instant.parse("2026-01-02T00:00:00Z"),
        )

        assertNull(query.recordKind)
    }

    @Test
    fun `a ChronologicalLookupQuery rejects a from later than to`() {
        assertFailsWith<IllegalArgumentException> {
            ChronologicalLookupQuery(
                requestingPrincipalId = PrincipalId("principal-1"),
                maximumResults = 5,
                from = Instant.parse("2026-01-02T00:00:00Z"),
                to = Instant.parse("2026-01-01T00:00:00Z"),
            )
        }
    }

    // ================= MetadataLookupQuery =================

    @Test
    fun `a MetadataLookupQuery requires a non-blank recordKind and a non-empty metadataFilter`() {
        assertFailsWith<IllegalArgumentException> {
            MetadataLookupQuery(
                requestingPrincipalId = PrincipalId("principal-1"),
                maximumResults = 5,
                recordKind = "",
                metadataFilter = mapOf("topic" to "billing"),
            )
        }
        assertFailsWith<IllegalArgumentException> {
            MetadataLookupQuery(
                requestingPrincipalId = PrincipalId("principal-1"),
                maximumResults = 5,
                recordKind = RelationshipEndpoint.ASSERTION,
                metadataFilter = emptyMap(),
            )
        }
    }

    @Test
    fun `a MetadataLookupQuery with a non-blank recordKind and a non-empty filter is accepted`() {
        val query = MetadataLookupQuery(
            requestingPrincipalId = PrincipalId("principal-1"),
            maximumResults = 5,
            recordKind = RelationshipEndpoint.ASSERTION,
            metadataFilter = mapOf("topic" to "billing"),
        )

        assertEquals(mapOf("topic" to "billing"), query.metadataFilter)
    }

    // ================= ProvenanceLookupQuery =================

    @Test
    fun `a ProvenanceLookupQuery requires at least one criterion`() {
        assertFailsWith<IllegalArgumentException> {
            ProvenanceLookupQuery(
                requestingPrincipalId = PrincipalId("principal-1"),
                maximumResults = 5,
                recordKind = RelationshipEndpoint.ENTITY,
            )
        }
    }

    @Test
    fun `a ProvenanceLookupQuery is accepted with only sensitivity supplied as its sole criterion`() {
        val query = ProvenanceLookupQuery(
            requestingPrincipalId = PrincipalId("principal-1"),
            maximumResults = 5,
            recordKind = RelationshipEndpoint.ENTITY,
            sensitivity = ResourceSensitivity.FINANCIAL,
        )

        assertEquals(ResourceSensitivity.FINANCIAL, query.sensitivity)
    }

    @Test
    fun `a ProvenanceLookupQuery rejects a blank recordKind`() {
        assertFailsWith<IllegalArgumentException> {
            ProvenanceLookupQuery(
                requestingPrincipalId = PrincipalId("principal-1"),
                maximumResults = 5,
                recordKind = "",
                creator = "someone",
            )
        }
    }

    // ================= MemoryCoreRecord =================

    @Test
    fun `MemoryCoreRecord wraps exactly one of the four Memory-Core-owned record kinds it names`() {
        val entity = Entity(
            entityId = EntityId("entity-1"),
            entityType = "person",
            primaryLabel = "Jane Doe",
            provenanceId = ProvenanceId("provenance-1"),
            createdAt = Instant.parse("2026-01-01T00:00:00Z"),
        )

        val wrapped: MemoryCoreRecord = MemoryCoreRecord.OfEntity(entity)

        assertTrue(wrapped is MemoryCoreRecord.OfEntity)
        assertEquals(entity, (wrapped as MemoryCoreRecord.OfEntity).entity)
    }

    @Test
    fun `MemoryCoreRecord has exactly the four expected subclasses`() {
        val expectedSubclassNames = setOf("OfEntity", "OfDocument", "OfAssertion", "OfRelationship")

        assertEquals(
            expectedSubclassNames,
            MemoryCoreRecord::class.sealedSubclasses.mapNotNull { it.simpleName }.toSet(),
        )
    }

    // ================= MemoryCoreRecordReference (Errata 003 amendment) =================

    @Test
    fun `MemoryCoreRecordReference has exactly the four expected subclasses -- one per lifecycle-bearing record kind`() {
        val expectedSubclassNames = setOf("ToEntity", "ToDocument", "ToAssertion", "ToRelationship")

        assertEquals(
            expectedSubclassNames,
            MemoryCoreRecordReference::class.sealedSubclasses.mapNotNull { it.simpleName }.toSet(),
        )
    }

    @Test
    fun `MemoryCoreRecordReference carries no case referencing ProvenanceId -- Provenance has no lifecycle status`() {
        val referencedIdentifierTypes = MemoryCoreRecordReference::class.sealedSubclasses.flatMap { subclass ->
            subclass.memberProperties.map { it.returnType.classifier }
        }.toSet()

        assertEquals(setOf(EntityId::class, DocumentId::class, AssertionId::class, RelationshipId::class), referencedIdentifierTypes)
        assertTrue(ProvenanceId::class !in referencedIdentifierTypes, "MemoryCoreRecordReference must not reference ProvenanceId")
    }

    @Test
    fun `each MemoryCoreRecordReference variant wraps exactly one identifier of the matching type`() {
        val entityReference = MemoryCoreRecordReference.ToEntity(EntityId("entity-1"))
        val documentReference = MemoryCoreRecordReference.ToDocument(DocumentId("document-1"))
        val assertionReference = MemoryCoreRecordReference.ToAssertion(AssertionId("assertion-1"))
        val relationshipReference = MemoryCoreRecordReference.ToRelationship(RelationshipId("relationship-1"))

        assertEquals(EntityId("entity-1"), entityReference.entityId)
        assertEquals(DocumentId("document-1"), documentReference.documentId)
        assertEquals(AssertionId("assertion-1"), assertionReference.assertionId)
        assertEquals(RelationshipId("relationship-1"), relationshipReference.relationshipId)
    }

    @Test
    fun `two MemoryCoreRecordReference values wrapping the same identifier are equal, and differ when the identifier differs`() {
        assertEquals(MemoryCoreRecordReference.ToEntity(EntityId("entity-1")), MemoryCoreRecordReference.ToEntity(EntityId("entity-1")))
        assertNotEquals(MemoryCoreRecordReference.ToEntity(EntityId("entity-1")), MemoryCoreRecordReference.ToEntity(EntityId("entity-2")))
        assertNotEquals<MemoryCoreRecordReference>(
            MemoryCoreRecordReference.ToEntity(EntityId("entity-1")),
            MemoryCoreRecordReference.ToDocument(DocumentId("entity-1")),
        )
    }

    // ================= Immutability, shared across every new query/result type =================

    @Test
    fun `every new query and result type exposes no mutable (var) property`() {
        val typesToCheck = listOf(
            EntityLookupQuery::class,
            DocumentLookupQuery::class,
            RelationshipTraversalQuery::class,
            ChronologicalLookupQuery::class,
            MetadataLookupQuery::class,
            ProvenanceLookupQuery::class,
            MemoryCoreRecord.OfEntity::class,
            MemoryCoreRecord.OfDocument::class,
            MemoryCoreRecord.OfAssertion::class,
            MemoryCoreRecord.OfRelationship::class,
            MemoryCoreRecordReference.ToEntity::class,
            MemoryCoreRecordReference.ToDocument::class,
            MemoryCoreRecordReference.ToAssertion::class,
            MemoryCoreRecordReference.ToRelationship::class,
        )

        typesToCheck.forEach { type ->
            val mutableProperties = type.memberProperties.filterIsInstance<KMutableProperty1<*, *>>()
            assertTrue(
                mutableProperties.isEmpty(),
                "${type.simpleName} must expose only immutable (val) properties, found: ${mutableProperties.map { it.name }}",
            )
        }
    }

    @Test
    fun `copy() produces a distinct EntityLookupQuery instance without mutating the original`() {
        val original = EntityLookupQuery(requestingPrincipalId = PrincipalId("principal-1"), maximumResults = 5)
        val copy = original.copy(maximumResults = 10)

        assertEquals(5, original.maximumResults)
        assertEquals(10, copy.maximumResults)
        assertNotEquals(original, copy)
    }
}
