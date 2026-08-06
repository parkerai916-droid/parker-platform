package parker.core.runtime

import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.Base64
import parker.core.interfaces.Assertion
import parker.core.interfaces.AssertionId
import parker.core.interfaces.ContentNature
import parker.core.interfaces.Document
import parker.core.interfaces.DocumentId
import parker.core.interfaces.DocumentProcessingStatus
import parker.core.interfaces.Entity
import parker.core.interfaces.EntityId
import parker.core.interfaces.MemoryCoreRecordReference
import parker.core.interfaces.MemoryCoreRecordStatus
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.Provenance
import parker.core.interfaces.ProvenanceId
import parker.core.interfaces.Relationship
import parker.core.interfaces.RelationshipEndpoint
import parker.core.interfaces.RelationshipId
import parker.core.interfaces.ResourceSensitivity

/**
 * Memory Core Durability, Implementation Unit 3 (Atomic Append). The
 * one-line-per-entry text encoding [FileSystemMemoryCoreDurabilityLog]
 * durably appends and reads back. This is the round-trip-safe text
 * encoding `src/runtime/DurableMemoryCoreEntry.kt`'s own KDoc explicitly
 * deferred out of Unit 1 -- this is that later unit.
 *
 * ## Format: tab-separated `key=value` fields, one line per entry,
 * extending [FileSystemEvidenceDeletionAudit]'s own established
 * convention
 *
 * Every field is written as `key=value`; fields are joined with a single
 * tab; the caller ([FileSystemMemoryCoreDurabilityLog]) appends a
 * terminating `\n`. `kind` (which of the six [DurableMemoryCoreEntry]
 * cases) and `schemaVersion` always appear first, mirroring
 * [FileSystemEvidenceDeletionAudit]'s own "fixed order" discipline.
 * A nullable field that is absent is simply omitted from the line, never
 * written with an empty or sentinel value -- decode treats a missing key
 * exactly as the original `null`.
 *
 * ## Every string-valued field is Base64-encoded, without exception
 *
 * `EntityId`, `ProvenanceId`, and every other identifier type this
 * repository defines validates only that its own value is non-blank
 * (`src/interfaces/MemoryCore.kt`) -- nothing prevents a caller from
 * constructing one containing an embedded tab or newline, and the same is
 * true of every free-text field this Programme's five record types carry
 * (`Entity.primaryLabel`, `Assertion.statement`, `metadata` keys and
 * values, and so on). Rather than hand-rolling a backslash-style escape
 * scheme for tab/newline/backslash -- a well-known source of subtle
 * round-trip bugs at exactly the edge cases (a value ending in a
 * backslash, a value containing the escape character itself) -- every
 * string-valued field is Base64-encoded (`java.util.Base64`, already
 * part of the JDK; no new runtime dependency) before being placed in a
 * `key=value` pair. Base64 output is guaranteed, by construction, to
 * contain none of this format's own structural characters (tab, comma,
 * colon, newline), which eliminates the escaping problem entirely rather
 * than attempting to solve it correctly. Numeric fields ([Int], [Double]),
 * [Instant] (already ISO-8601, structurally safe), [Boolean], and closed
 * enum values (encoded via `.name`) are written raw, since none of those
 * representations can ever contain this format's own structural
 * characters.
 *
 * ## Lists and maps
 *
 * A `List<String>`-shaped field (`Entity.aliases`,
 * `Provenance.processingHistory`) is encoded as its own Base64-encoded
 * elements joined by a comma; a `List<ProvenanceId>`-shaped field
 * (`Provenance.derivedFrom`) the same way, over each identifier's own
 * `value`. A `Map<String, String>`-shaped field (every record type's own
 * `metadata`) is encoded as Base64-encoded `key:value` pairs joined by a
 * comma. An empty list or empty map is omitted from the line entirely,
 * exactly like a null field -- decode treats a missing key as the empty
 * collection, matching every record type's own established default.
 *
 * ## Failure is fail-fast, and deliberately does not classify corruption
 *
 * [decode] throws [MemoryCoreDurabilityLogException.MalformedEntry] for
 * any field that cannot be parsed (a missing required key, an
 * unparseable number or timestamp, an unrecognised enum value, invalid
 * Base64), and [MemoryCoreDurabilityLogException.UnrecognizedSchemaVersion]
 * for a `schemaVersion` other than [DurableMemoryCoreEntry.CURRENT_SCHEMA_VERSION]
 * -- consistent with the Contract Design's own Section 8: "unknown future
 * versions must never be silently interpreted." **This Unit deliberately
 * does not distinguish a genuinely corrupted, already-committed entry
 * from a partial, not-yet-complete write interrupted mid-append** --
 * that classification (position-in-file-aware: the last line only may be
 * discarded as an interrupted write; any earlier line failing to decode
 * is genuine corruption) is explicitly Implementation Unit 4's own,
 * later, Contract-Design-Section-7-governed responsibility. This Unit's
 * own [decode] simply throws for any unparseable line, uniformly,
 * matching this Unit's own governing task's explicit instruction: "no
 * recovery logic."
 */
internal object DurableMemoryCoreEntryCodec {

    private const val FIELD_SEPARATOR = "\t"
    private const val LIST_SEPARATOR = ","
    private const val MAP_ENTRY_SEPARATOR = ":"

    fun encode(entry: DurableMemoryCoreEntry): String {
        val fields = mutableListOf<String>()
        fields += rawField("kind", entry.kindName())
        fields += rawField("schemaVersion", entry.schemaVersion.toString())
        when (entry) {
            is DurableMemoryCoreEntry.ProvenanceCreated -> encodeProvenance(entry.provenance, fields)
            is DurableMemoryCoreEntry.EntityCreated -> encodeEntity(entry.entity, fields)
            is DurableMemoryCoreEntry.DocumentRegistered -> encodeDocument(entry.document, fields)
            is DurableMemoryCoreEntry.AssertionCreated -> encodeAssertion(entry.assertion, fields)
            is DurableMemoryCoreEntry.RelationshipCreated -> encodeRelationship(entry.relationship, fields)
            is DurableMemoryCoreEntry.StatusTransitioned -> encodeStatusTransitioned(entry, fields)
        }
        return fields.joinToString(FIELD_SEPARATOR)
    }

    fun decode(line: String, lineNumber: Int): DurableMemoryCoreEntry {
        try {
            val map = parseFields(line, lineNumber)
            val kind = requireField(map, "kind", lineNumber)
            val schemaVersion = requireField(map, "schemaVersion", lineNumber).toIntOrNull()
                ?: throw MemoryCoreDurabilityLogException.MalformedEntry(lineNumber, "schemaVersion is not a valid integer")
            if (schemaVersion != DurableMemoryCoreEntry.CURRENT_SCHEMA_VERSION) {
                throw MemoryCoreDurabilityLogException.UnrecognizedSchemaVersion(kind, schemaVersion)
            }
            return when (kind) {
                "ProvenanceCreated" -> DurableMemoryCoreEntry.ProvenanceCreated(schemaVersion, decodeProvenance(map, lineNumber))
                "EntityCreated" -> DurableMemoryCoreEntry.EntityCreated(schemaVersion, decodeEntity(map, lineNumber))
                "DocumentRegistered" -> DurableMemoryCoreEntry.DocumentRegistered(schemaVersion, decodeDocument(map, lineNumber))
                "AssertionCreated" -> DurableMemoryCoreEntry.AssertionCreated(schemaVersion, decodeAssertion(map, lineNumber))
                "RelationshipCreated" -> DurableMemoryCoreEntry.RelationshipCreated(schemaVersion, decodeRelationship(map, lineNumber))
                "StatusTransitioned" -> decodeStatusTransitioned(map, schemaVersion, lineNumber)
                else -> throw MemoryCoreDurabilityLogException.MalformedEntry(lineNumber, "unrecognised entry kind '$kind'")
            }
        } catch (e: MemoryCoreDurabilityLogException) {
            throw e
        } catch (e: Exception) {
            throw MemoryCoreDurabilityLogException.MalformedEntry(lineNumber, "failed to decode entry: ${e.message}", e)
        }
    }

    // ================= kind dispatch =================

    private fun DurableMemoryCoreEntry.kindName(): String = when (this) {
        is DurableMemoryCoreEntry.ProvenanceCreated -> "ProvenanceCreated"
        is DurableMemoryCoreEntry.EntityCreated -> "EntityCreated"
        is DurableMemoryCoreEntry.DocumentRegistered -> "DocumentRegistered"
        is DurableMemoryCoreEntry.AssertionCreated -> "AssertionCreated"
        is DurableMemoryCoreEntry.RelationshipCreated -> "RelationshipCreated"
        is DurableMemoryCoreEntry.StatusTransitioned -> "StatusTransitioned"
    }

    // ================= Provenance =================

    private fun encodeProvenance(provenance: Provenance, fields: MutableList<String>) {
        fields += field("provenanceId", provenance.provenanceId.value)
        fields += field("sourceIdentifier", provenance.sourceIdentifier)
        fields += field("sourceType", provenance.sourceType)
        fields += rawField("acquisitionTime", provenance.acquisitionTime.toString())
        fields += rawField("ingestionTime", provenance.ingestionTime.toString())
        fields += rawField("contentNature", provenance.contentNature.name)
        provenance.creator?.let { fields += field("creator", it) }
        provenance.creatorPrincipalId?.let { fields += field("creatorPrincipalId", it.value) }
        provenance.claimedCreationTime?.let { fields += rawField("claimedCreationTime", it.toString()) }
        if (provenance.derivedFrom.isNotEmpty()) {
            fields += rawField("derivedFrom", provenance.derivedFrom.joinToString(LIST_SEPARATOR) { b64(it.value) })
        }
        provenance.extractedFrom?.let { fields += field("extractedFrom", it.value) }
        if (provenance.processingHistory.isNotEmpty()) {
            fields += rawField("processingHistory", encodeStringList(provenance.processingHistory))
        }
        provenance.integrityInformation?.let { fields += field("integrityInformation", it) }
        provenance.confidence?.let { fields += rawField("confidence", it.toString()) }
        provenance.sensitivity?.let { fields += rawField("sensitivity", it.name) }
    }

    private fun decodeProvenance(map: Map<String, String>, lineNumber: Int): Provenance = Provenance(
        provenanceId = ProvenanceId(decodeField(map, "provenanceId", lineNumber)),
        sourceIdentifier = decodeField(map, "sourceIdentifier", lineNumber),
        sourceType = decodeField(map, "sourceType", lineNumber),
        acquisitionTime = Instant.parse(requireField(map, "acquisitionTime", lineNumber)),
        ingestionTime = Instant.parse(requireField(map, "ingestionTime", lineNumber)),
        contentNature = ContentNature.valueOf(requireField(map, "contentNature", lineNumber)),
        creator = map["creator"]?.let { unb64(it) },
        creatorPrincipalId = map["creatorPrincipalId"]?.let { PrincipalId(unb64(it)) },
        claimedCreationTime = map["claimedCreationTime"]?.let { Instant.parse(it) },
        derivedFrom = map["derivedFrom"]?.let { decodeStringList(it).map(::ProvenanceId) } ?: emptyList(),
        extractedFrom = map["extractedFrom"]?.let { DocumentId(unb64(it)) },
        processingHistory = map["processingHistory"]?.let { decodeStringList(it) } ?: emptyList(),
        integrityInformation = map["integrityInformation"]?.let { unb64(it) },
        confidence = map["confidence"]?.toDouble(),
        sensitivity = map["sensitivity"]?.let { ResourceSensitivity.valueOf(it) },
    )

    // ================= Entity =================

    private fun encodeEntity(entity: Entity, fields: MutableList<String>) {
        fields += field("entityId", entity.entityId.value)
        fields += field("entityType", entity.entityType)
        fields += field("primaryLabel", entity.primaryLabel)
        fields += field("provenanceId", entity.provenanceId.value)
        fields += rawField("createdAt", entity.createdAt.toString())
        if (entity.aliases.isNotEmpty()) fields += rawField("aliases", encodeStringList(entity.aliases))
        entity.relatedPrincipalId?.let { fields += field("relatedPrincipalId", it.value) }
        fields += rawField("status", entity.status.name)
        if (entity.metadata.isNotEmpty()) fields += rawField("metadata", encodeMap(entity.metadata))
    }

    private fun decodeEntity(map: Map<String, String>, lineNumber: Int): Entity = Entity(
        entityId = EntityId(decodeField(map, "entityId", lineNumber)),
        entityType = decodeField(map, "entityType", lineNumber),
        primaryLabel = decodeField(map, "primaryLabel", lineNumber),
        provenanceId = ProvenanceId(decodeField(map, "provenanceId", lineNumber)),
        createdAt = Instant.parse(requireField(map, "createdAt", lineNumber)),
        aliases = map["aliases"]?.let { decodeStringList(it) } ?: emptyList(),
        relatedPrincipalId = map["relatedPrincipalId"]?.let { PrincipalId(unb64(it)) },
        status = MemoryCoreRecordStatus.valueOf(requireField(map, "status", lineNumber)),
        metadata = map["metadata"]?.let { decodeMap(it) } ?: emptyMap(),
    )

    // ================= Document =================

    private fun encodeDocument(document: Document, fields: MutableList<String>) {
        fields += field("documentId", document.documentId.value)
        fields += field("documentType", document.documentType)
        fields += field("locationReference", document.locationReference)
        fields += field("provenanceId", document.provenanceId.value)
        fields += rawField("registeredAt", document.registeredAt.toString())
        document.integrityHash?.let { fields += field("integrityHash", it) }
        fields += rawField("processingStatus", document.processingStatus.name)
        fields += rawField("status", document.status.name)
        if (document.metadata.isNotEmpty()) fields += rawField("metadata", encodeMap(document.metadata))
    }

    private fun decodeDocument(map: Map<String, String>, lineNumber: Int): Document = Document(
        documentId = DocumentId(decodeField(map, "documentId", lineNumber)),
        documentType = decodeField(map, "documentType", lineNumber),
        locationReference = decodeField(map, "locationReference", lineNumber),
        provenanceId = ProvenanceId(decodeField(map, "provenanceId", lineNumber)),
        registeredAt = Instant.parse(requireField(map, "registeredAt", lineNumber)),
        integrityHash = map["integrityHash"]?.let { unb64(it) },
        processingStatus = DocumentProcessingStatus.valueOf(requireField(map, "processingStatus", lineNumber)),
        status = MemoryCoreRecordStatus.valueOf(requireField(map, "status", lineNumber)),
        metadata = map["metadata"]?.let { decodeMap(it) } ?: emptyMap(),
    )

    // ================= Assertion =================

    private fun encodeAssertion(assertion: Assertion, fields: MutableList<String>) {
        fields += field("assertionId", assertion.assertionId.value)
        fields += field("statement", assertion.statement)
        fields += field("provenanceId", assertion.provenanceId.value)
        assertion.confidence?.let { fields += rawField("confidence", it.toString()) }
        fields += rawField("status", assertion.status.name)
        if (assertion.metadata.isNotEmpty()) fields += rawField("metadata", encodeMap(assertion.metadata))
    }

    private fun decodeAssertion(map: Map<String, String>, lineNumber: Int): Assertion = Assertion(
        assertionId = AssertionId(decodeField(map, "assertionId", lineNumber)),
        statement = decodeField(map, "statement", lineNumber),
        provenanceId = ProvenanceId(decodeField(map, "provenanceId", lineNumber)),
        confidence = map["confidence"]?.toDouble(),
        status = MemoryCoreRecordStatus.valueOf(requireField(map, "status", lineNumber)),
        metadata = map["metadata"]?.let { decodeMap(it) } ?: emptyMap(),
    )

    // ================= Relationship =================

    private fun encodeRelationship(relationship: Relationship, fields: MutableList<String>) {
        fields += field("relationshipId", relationship.relationshipId.value)
        fields += field("relationshipType", relationship.relationshipType)
        fields += field("fromEndpointKind", relationship.fromEndpoint.recordKind)
        fields += field("fromEndpointId", relationship.fromEndpoint.recordId)
        fields += field("toEndpointKind", relationship.toEndpoint.recordKind)
        fields += field("toEndpointId", relationship.toEndpoint.recordId)
        fields += rawField("directional", relationship.directional.toString())
        fields += field("provenanceId", relationship.provenanceId.value)
        fields += rawField("createdAt", relationship.createdAt.toString())
        fields += rawField("status", relationship.status.name)
    }

    private fun decodeRelationship(map: Map<String, String>, lineNumber: Int): Relationship = Relationship(
        relationshipId = RelationshipId(decodeField(map, "relationshipId", lineNumber)),
        relationshipType = decodeField(map, "relationshipType", lineNumber),
        fromEndpoint = RelationshipEndpoint(
            recordKind = decodeField(map, "fromEndpointKind", lineNumber),
            recordId = decodeField(map, "fromEndpointId", lineNumber),
        ),
        toEndpoint = RelationshipEndpoint(
            recordKind = decodeField(map, "toEndpointKind", lineNumber),
            recordId = decodeField(map, "toEndpointId", lineNumber),
        ),
        directional = requireField(map, "directional", lineNumber).toBooleanStrict(),
        provenanceId = ProvenanceId(decodeField(map, "provenanceId", lineNumber)),
        createdAt = Instant.parse(requireField(map, "createdAt", lineNumber)),
        status = MemoryCoreRecordStatus.valueOf(requireField(map, "status", lineNumber)),
    )

    // ================= StatusTransitioned =================

    private fun encodeStatusTransitioned(entry: DurableMemoryCoreEntry.StatusTransitioned, fields: MutableList<String>) {
        val (referenceKind, referenceId) = encodeReference(entry.reference)
        fields += rawField("referenceKind", referenceKind)
        fields += field("referenceId", referenceId)
        fields += rawField("priorStatus", entry.priorStatus.name)
        fields += rawField("targetStatus", entry.targetStatus.name)
        fields += rawField("transitionedAt", entry.transitionedAt.toString())
    }

    private fun encodeReference(reference: MemoryCoreRecordReference): Pair<String, String> = when (reference) {
        is MemoryCoreRecordReference.ToEntity -> "Entity" to reference.entityId.value
        is MemoryCoreRecordReference.ToDocument -> "Document" to reference.documentId.value
        is MemoryCoreRecordReference.ToAssertion -> "Assertion" to reference.assertionId.value
        is MemoryCoreRecordReference.ToRelationship -> "Relationship" to reference.relationshipId.value
    }

    private fun decodeStatusTransitioned(
        map: Map<String, String>,
        schemaVersion: Int,
        lineNumber: Int,
    ): DurableMemoryCoreEntry.StatusTransitioned {
        val referenceKind = requireField(map, "referenceKind", lineNumber)
        val referenceId = decodeField(map, "referenceId", lineNumber)
        val reference = when (referenceKind) {
            "Entity" -> MemoryCoreRecordReference.ToEntity(EntityId(referenceId))
            "Document" -> MemoryCoreRecordReference.ToDocument(DocumentId(referenceId))
            "Assertion" -> MemoryCoreRecordReference.ToAssertion(AssertionId(referenceId))
            "Relationship" -> MemoryCoreRecordReference.ToRelationship(RelationshipId(referenceId))
            else -> throw MemoryCoreDurabilityLogException.MalformedEntry(lineNumber, "unrecognised reference kind '$referenceKind'")
        }
        return DurableMemoryCoreEntry.StatusTransitioned(
            schemaVersion = schemaVersion,
            reference = reference,
            priorStatus = MemoryCoreRecordStatus.valueOf(requireField(map, "priorStatus", lineNumber)),
            targetStatus = MemoryCoreRecordStatus.valueOf(requireField(map, "targetStatus", lineNumber)),
            transitionedAt = Instant.parse(requireField(map, "transitionedAt", lineNumber)),
        )
    }

    // ================= Field-level primitives =================

    private fun rawField(key: String, value: String): String = "$key=$value"

    private fun field(key: String, value: String): String = "$key=${b64(value)}"

    private fun decodeField(map: Map<String, String>, key: String, lineNumber: Int): String =
        unb64(requireField(map, key, lineNumber))

    private fun requireField(map: Map<String, String>, key: String, lineNumber: Int): String =
        map[key] ?: throw MemoryCoreDurabilityLogException.MalformedEntry(lineNumber, "missing required field '$key'")

    private fun parseFields(line: String, lineNumber: Int): Map<String, String> =
        line.split(FIELD_SEPARATOR).associate { raw ->
            val separatorIndex = raw.indexOf('=')
            if (separatorIndex < 0) {
                throw MemoryCoreDurabilityLogException.MalformedEntry(lineNumber, "field '$raw' is not in key=value form")
            }
            raw.substring(0, separatorIndex) to raw.substring(separatorIndex + 1)
        }

    private fun b64(value: String): String = Base64.getEncoder().encodeToString(value.toByteArray(StandardCharsets.UTF_8))

    private fun unb64(value: String): String = String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8)

    private fun encodeStringList(values: List<String>): String = values.joinToString(LIST_SEPARATOR) { b64(it) }

    private fun decodeStringList(value: String): List<String> =
        if (value.isEmpty()) emptyList() else value.split(LIST_SEPARATOR).map { unb64(it) }

    private fun encodeMap(values: Map<String, String>): String =
        values.entries.joinToString(LIST_SEPARATOR) { (key, value) -> "${b64(key)}$MAP_ENTRY_SEPARATOR${b64(value)}" }

    private fun decodeMap(value: String): Map<String, String> =
        if (value.isEmpty()) {
            emptyMap()
        } else {
            value.split(LIST_SEPARATOR).associate {
                val parts = it.split(MAP_ENTRY_SEPARATOR)
                unb64(parts[0]) to unb64(parts[1])
            }
        }
}
