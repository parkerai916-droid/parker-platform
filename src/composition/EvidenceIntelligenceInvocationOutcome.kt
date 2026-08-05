package parker.composition

import parker.core.runtime.EvidenceIntelligenceAcceptanceOutcome

/**
 * Programme 4, Evidence Intelligence, Implementation Unit 8 ("Runtime
 * Composition and Full Verification"). The result of one
 * [ParkerRuntime.analyseEvidence] call -- a composition-root-level
 * outcome, mirroring [ParkerRuntimeOutcome]'s own precedent exactly:
 * `EvidenceIntelligenceInvocationGate`'s own accepted reconciliation
 * review states plainly that denial representation belongs "to
 * whichever operation Unit 8 introduces as Evidence Intelligence's
 * composing caller -- never to this file," and neither
 * `EvidenceIntelligence` nor `EvidenceIntelligenceAcceptanceCoordinator`
 * defines one of its own (Scope Lock Section 4's "no fifth new public
 * type" ceiling governs Evidence Intelligence's own four-type model; it
 * does not reach this composition-level artefact, on the same footing
 * `EvidenceIntelligenceInvocationGate` and `EvidenceIntelligenceAcceptanceCoordinator`
 * themselves already occupy).
 *
 * ## Why [Completed]'s constructor and property are `internal`, even
 * though this class is `public`
 *
 * [EvidenceIntelligenceAcceptanceOutcome] -- the exact, unreinterpreted
 * per-candidate outcome list [EvidenceIntelligenceAcceptanceCoordinator.dispatch]
 * returns -- is itself `internal`, deliberately (its own KDoc: composition-level,
 * never part of Evidence Intelligence's own four-type public model). This
 * class must be `public` because it is the return type of a `public`
 * [ParkerRuntime] method (matching `submitEvidence`/`retrieveEvidence`/
 * `deleteEvidenceAsOwner`'s own existing style) -- but a `public` class
 * cannot expose a `public` property of an `internal` type (Kotlin
 * visibility rules). [Completed]'s constructor and its one property are
 * therefore explicitly `internal`, the narrowest resolution available:
 * the type is visible everywhere (so `is Completed`/`is NotAuthorised`
 * pattern matching works for any caller), but only code within this same
 * module can construct one or read [Completed.acceptanceOutcomes] --
 * exactly as narrow as the list it carries already is, never wider.
 */
sealed class EvidenceIntelligenceInvocationOutcome {

    /**
     * [EvidenceIntelligenceInvocationGate]'s own proposal class was not
     * `APPROVED`/`APPROVED_WITH_CONFIRMATION`. Neither [parker.core.interfaces.EvidenceIntelligence.analyse]
     * nor [EvidenceIntelligenceAcceptanceCoordinator.dispatch] was ever
     * called. [reason] carries the same descriptive text
     * [DefaultKnowledgeSubmission]'s and [EvidenceIntelligenceAcceptanceCoordinator]'s
     * own `NotAuthorised` variants already use for an identical situation.
     */
    data class NotAuthorised(val reason: String) : EvidenceIntelligenceInvocationOutcome()

    /**
     * The invocation gate approved, [parker.core.interfaces.EvidenceIntelligence.analyse]
     * was called exactly once, and its returned list was dispatched,
     * completely unchanged and in the same order, to
     * [EvidenceIntelligenceAcceptanceCoordinator.dispatch]. [acceptanceOutcomes]
     * is that dispatch's own returned list, unchanged -- one
     * [EvidenceIntelligenceAcceptanceOutcome] per [parker.core.interfaces.EvidenceAnalysisResult]
     * Evidence Intelligence produced for this invocation, in the same
     * order. See this class's own header KDoc, "Why `Completed`'s
     * constructor and property are `internal`," for why this type's
     * visibility is narrower than [EvidenceIntelligenceInvocationOutcome]
     * itself.
     */
    data class Completed internal constructor(
        internal val acceptanceOutcomes: List<EvidenceIntelligenceAcceptanceOutcome>,
    ) : EvidenceIntelligenceInvocationOutcome()
}
