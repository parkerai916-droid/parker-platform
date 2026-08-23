package parker.core.interfaces

/**
 * Document Ingestion, Derivative-to-Memory-Core Registration. The
 * terminal outcome of one explicit, owner-triggered act registering an
 * already-admitted Tier A derivative into Memory Core, governed by
 * `docs/architecture/DOCUMENT_INGESTION_DERIVATIVE_TO_MEMORY_CORE_REGISTRATION_SCOPE_LOCK.md`
 * ("the Scope Lock") Section 15's failure-sequencing rules. Mirrors
 * [EvidenceRegistrationOutcome]'s own non-collapsing, two-gate shape
 * (`src/runtime/EvidenceRegistrationCoordinator.kt`) exactly, adapted
 * for a single already-existing candidate (the admitted derivative)
 * rather than a fresh acceptance.
 *
 * Neither variant carries the derivative's own extracted content (Scope
 * Lock Section 8) -- only [Provenance] and [Document], the two records
 * this act is authorized to create.
 */
sealed class DerivativeMemoryRegistrationOutcome {

    /**
     * `createProvenance` and `registerDocument` both succeeded, in that
     * order. [provenance] and [document] are exactly what those two
     * calls returned, unmodified.
     */
    data class Registered(val provenance: Provenance, val document: Document) : DerivativeMemoryRegistrationOutcome()

    /**
     * The Permission Engine did not authorize `createProvenance` for
     * this derivative. `registerDocument` is never called. No
     * [Provenance] and no [Document] exist as a result of this call.
     */
    data class ProvenanceNotAuthorised(val reason: String) : DerivativeMemoryRegistrationOutcome()

    /**
     * `createProvenance` succeeded, but the Permission Engine did not
     * authorize `registerDocument`. [provenance] is carried because it
     * already, truthfully, durably exists -- Memory Core's own history
     * is append-only, and this coordinator never attempts to roll it
     * back (Scope Lock Section 15's own "no distributed transaction, no
     * rollback" rule, restated here).
     */
    data class DocumentRegistrationNotAuthorised(val provenance: Provenance, val reason: String) : DerivativeMemoryRegistrationOutcome()
}
