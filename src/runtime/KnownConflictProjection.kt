package parker.core.runtime

import parker.core.interfaces.EvidenceAnalysisRequest
import parker.core.interfaces.KnownConflictResolutionState
import parker.core.interfaces.KnownConflictType
import parker.core.interfaces.MemoryCoreRecord
import parker.core.interfaces.MemoryCoreRecordStatus
import parker.core.interfaces.MemoryRetrieval
import parker.core.interfaces.Relationship
import parker.core.interfaces.RelationshipEndpoint
import parker.core.interfaces.RelationshipTraversalDirection
import parker.core.interfaces.RelationshipTraversalQuery
import parker.core.interfaces.ReasoningContext
import parker.core.interfaces.ReasoningContextEntry

/** The invocation-scoped, read-only view of an existing governed conflict. */
internal data class KnownConflictProjection(
    val conflictId: String,
    val conflictType: KnownConflictType,
    val resolutionState: KnownConflictResolutionState,
    val participants: List<RelationshipEndpoint>,
    val relationship: Relationship,
)

internal sealed interface KnownConflictProjectionOutcome {
    data class Projected(val conflicts: List<KnownConflictProjection>) : KnownConflictProjectionOutcome
    data object FailedClosed : KnownConflictProjectionOutcome
}

/**
 * Reads only relationships already returned by the governed Memory Core
 * retrieval boundary. It does not compare record contents or create a
 * contradiction fact.
 */
internal object KnownConflictProjector {
    suspend fun project(
        request: EvidenceAnalysisRequest,
        memoryCoreResults: List<Pair<RelationshipEndpoint, MemoryCoreRecord? >>,
        retrieval: MemoryRetrieval,
    ): KnownConflictProjectionOutcome {
        return try {
            val resolvedEndpoints = memoryCoreResults.filter { it.second != null }.map { it.first }.toSet()
            val relationships = buildList {
                memoryCoreResults.forEach { (endpoint, record) ->
                    val resolvedRecord = record ?: return@forEach
                    if (resolvedRecord is MemoryCoreRecord.OfRelationship &&
                        isConflict(resolvedRecord.relationship.relationshipType)
                    ) add(resolvedRecord.relationship)
                    if (endpoint in resolvedEndpoints) {
                        addAll(
                            retrieval.traverseRelationships(
                                RelationshipTraversalQuery(
                                    requestingPrincipalId = request.requestingPrincipalId,
                                    maximumResults = MAX_RELATIONSHIPS,
                                    startingEndpoint = endpoint,
                                    direction = RelationshipTraversalDirection.BOTH,
                                ),
                            ).filter {
                                isConflict(it.relationshipType) &&
                                    (it.fromEndpoint == endpoint || it.toEndpoint == endpoint)
                            },
                        )
                    }
                }
            }
            val projections = relationships.distinctBy { it.relationshipId }.map { relationship ->
                KnownConflictProjection(
                    conflictId = relationship.relationshipId.value,
                    conflictType = when (relationship.relationshipType) {
                        Relationship.CONTRADICTS -> KnownConflictType.CONTRADICTS
                        Relationship.DISPUTES -> KnownConflictType.DISPUTES
                        else -> throw IllegalArgumentException("Unsupported conflict relationship type")
                    },
                    resolutionState = when (relationship.status) {
                        MemoryCoreRecordStatus.ACTIVE, MemoryCoreRecordStatus.DISPUTED ->
                            KnownConflictResolutionState.UNRESOLVED
                        MemoryCoreRecordStatus.SUPERSEDED -> KnownConflictResolutionState.AUTHORITATIVE_RESOLVED
                        // Archival or deletion is lifecycle state, not an
                        // adjudication of the conflict. Keep it fail-closed.
                        MemoryCoreRecordStatus.ARCHIVED, MemoryCoreRecordStatus.DELETED ->
                            KnownConflictResolutionState.UNRESOLVED
                    },
                    participants = listOf(relationship.fromEndpoint, relationship.toEndpoint),
                    relationship = relationship,
                )
            }
            KnownConflictProjectionOutcome.Projected(projections)
        } catch (_: Exception) {
            KnownConflictProjectionOutcome.FailedClosed
        }
    }

    fun contextEntries(
        context: ReasoningContext,
        conflicts: List<KnownConflictProjection>,
    ): List<ReasoningContextEntry> {
        val entries = conflicts.map { conflict ->
            ReasoningContextEntry.GovernedConflict(
                text = "Parker-governed ${conflict.conflictType.name} conflict " +
                    "${conflict.conflictId} involves " +
                    conflict.participants.joinToString(" and ") { "${it.recordKind}/${it.recordId}" } +
                    "; resolution=${conflict.resolutionState.name}",
                conflictId = conflict.conflictId,
                conflictType = conflict.conflictType,
                resolutionState = conflict.resolutionState,
                participants = conflict.participants,
                provenanceId = conflict.relationship.provenanceId,
            )
        }
        return context.typedEntries + entries
    }

    private fun isConflict(type: String): Boolean = type == Relationship.CONTRADICTS || type == Relationship.DISPUTES

    private const val MAX_RELATIONSHIPS = 1_000
}
