package parker.core.runtime

import parker.core.interfaces.GroundedPropositionClassification
import parker.core.interfaces.GroundedReply
import parker.core.interfaces.GroundedReviewReason
import parker.core.interfaces.KnownConflictResolutionState
import parker.core.interfaces.ReasoningContext
import parker.core.interfaces.ReasoningContextEntry

enum class KnownConflictEnforcementFailure {
    UNRESOLVED_MATERIAL_CONFLICT,
}

internal sealed interface KnownConflictEnforcementOutcome {
    data object Valid : KnownConflictEnforcementOutcome
    data class Invalid(val reason: KnownConflictEnforcementFailure) : KnownConflictEnforcementOutcome
}

/** Prevents an unresolved governed conflict from being hidden by a citation. */
internal object KnownConflictEnforcer {
    fun enforce(
        reply: GroundedReply,
        context: ReasoningContext,
        conflicts: List<KnownConflictProjection>,
    ): KnownConflictEnforcementOutcome {
        val unresolved = conflicts.filter { it.resolutionState == KnownConflictResolutionState.UNRESOLVED }
        if (unresolved.isEmpty()) return KnownConflictEnforcementOutcome.Valid

        val conflictEntries = context.suppliedGovernedEntries.filterIsInstance<ReasoningContextEntry.GovernedConflict>()
        reply.propositions.forEach { proposition ->
            if (proposition.classification != GroundedPropositionClassification.SUPPORTED_FACT) return@forEach
            val referencedParticipants = proposition.supportReferences.flatMap { reference ->
                when (reference) {
                    is ReasoningContextEntry.GovernedMemoryCore -> listOf(reference.reference) +
                        unresolved.filter { it.conflictId == reference.reference.recordId }
                            .flatMap { it.participants }
                    is ReasoningContextEntry.GovernedConflict -> reference.participants
                    else -> emptyList()
                }
            }.toSet()
            val materiallyAffected = unresolved.any { conflict ->
                conflict.participants.any { it in referencedParticipants } ||
                    conflictEntries.any { entry -> entry.conflictId == conflict.conflictId && entry in proposition.supportReferences }
            }
            if (materiallyAffected) {
                return KnownConflictEnforcementOutcome.Invalid(
                    KnownConflictEnforcementFailure.UNRESOLVED_MATERIAL_CONFLICT,
                )
            }
        }

        // An explicitly disclosed conflict is valid; enforcement does not
        // rewrite or upgrade/downgrade the provider's proposition.
        return KnownConflictEnforcementOutcome.Valid
    }

    fun disclosed(reply: GroundedReply): Boolean = reply.propositions.any {
        it.classification == GroundedPropositionClassification.HUMAN_REVIEW_REQUIRED &&
            it.reviewReason == GroundedReviewReason.CONFLICT
    }
}
