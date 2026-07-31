package parker.core.interfaces

/**
 * Evidence Custodian -- Implementation Unit 1 (Foundational Identity),
 * corrected. Opens `src/interfaces/EvidenceCustodian.kt`, following the
 * "one file per subsystem" precedent [MemoryCore] and [WorldModel] already
 * established. Governed by the frozen Evidence Custodian documentation
 * stack: `docs/architecture/parker-constitution.md`;
 * `docs/architecture/epistemic-integrity.md` (Article IX, as amended);
 * `docs/decisions/CDR-006_CONSTITUTIONAL_CLASSIFICATION_OF_ORIGINAL_EVIDENCE_CUSTODY_AND_IMMUTABILITY.md`
 * ("CDR-006"); `docs/architecture/EVIDENCE_ARTIFACT_CONTRACT_DESIGN.md`
 * ("the Contract Design"); `docs/architecture/EVIDENCE_CUSTODIAN_SCOPE_LOCK.md`
 * ("the Scope Lock"); and `docs/architecture/EVIDENCE_CUSTODIAN_IMPLEMENTATION_PLAN.md`
 * ("the Implementation Plan").
 *
 * ## Correction history
 *
 * This Unit originally also introduced an `EvidenceArtifact` data class
 * carrying `artifactType` and `acceptedAt` fields. Architectural review
 * found both fields exceeded what Phase 1 may truthfully represent:
 * `acceptedAt` asserted a governed acceptance fact with no acceptance
 * mechanism (Permission Engine gating, Implementation Plan Phase 3) yet in
 * existence to produce it, and nothing prevented arbitrary direct
 * construction of a value falsely claiming an artefact had been accepted;
 * `artifactType` had no frozen Phase 1 requirement, only a design
 * inference from the Contract Design's descriptive examples (Section 2).
 * `EvidenceArtifact` has been removed entirely, without replacement, per
 * that review. This Unit now implements exactly one thing: [EvidenceArtifactId].
 *
 * ## What this Unit implements
 *
 * A single, non-vacuous identity value ([EvidenceArtifactId]) and the
 * foundational terminology later phases will build on. Nothing else.
 *
 * ## What this Unit deliberately does not implement, and does not claim
 *
 * - **No custody record, artefact shape, or acceptance fact of any kind.**
 *   This file contains no type representing "an artefact in custody" --
 *   that concept is not yet safe to represent, since the governed
 *   acceptance path (Implementation Plan Section 4, Phase 3) that alone
 *   may produce it does not exist yet.
 * - **No storage, persistence, or location mechanism.** No field anywhere
 *   in this file names a file path, byte content, object-store key, or
 *   database reference (Scope Lock Section 9).
 * - **No hashing, integrity-verification, or encryption scheme.** (Scope
 *   Lock Section 9; Implementation Plan Section 6.)
 * - **No lifecycle status field or enum.** The Contract Design's own
 *   Section 7 states its seven conceptual lifecycle stages are "not
 *   necessarily exclusive or strictly sequential" and explicitly that "no
 *   implementation state, Kotlin enum, or status field is defined or
 *   implied by the stages above." Nothing here represents any of them.
 * - **No interface, method, or API of any kind.** An `EvidenceCustodian`
 *   interface -- the surface a future acceptance/retrieval/deletion
 *   operation will live on -- is not introduced by this Unit
 *   (Implementation Plan Section 4, Phases 3, 4, 7).
 * - **No Permission Engine integration.** No type in this file is ever
 *   constructed, accepted, or authorised by anything yet.
 * - **No ownership, provenance, or derivative-link field.** (CDR-006,
 *   Contract Design Section 5; Contract Design Section 6.2.)
 * - **No uniqueness or non-reassignment guarantee.** [EvidenceArtifactId]
 *   is a value type only; nothing in this file tracks which identifiers
 *   have already been issued or prevents two different constructions from
 *   reusing the same identifier. Scope Lock Section 6's "assigned once, at
 *   acceptance, and never reassigned" guarantee is a property of a future
 *   governed system over time, not a property this bare value type can, or
 *   does, establish on its own.
 *
 * ## The accurate Phase 1 claim
 *
 * Phase 1 establishes the value-level representation of an evidence
 * artefact identifier. It does not yet establish issuance, uniqueness,
 * acceptance, custody, storage, or constitutional immutability
 * enforcement.
 */

/**
 * A non-vacuous identity value within the Evidence Custodian domain.
 * Rejects blank content at construction and compares by value, exactly
 * like every other identifier value class already established in this
 * repository ([EntityId], [DocumentId], [ProvenanceId], [RelationshipId]).
 *
 * This type identifies an artefact within the Evidence Custodian domain
 * and nothing more. Holding, or being able to construct, an
 * [EvidenceArtifactId] value proves none of the following:
 *
 * - that any artefact has been accepted into custody;
 * - that custody exists, or ever existed, for this value;
 * - that this value is unique -- nothing here prevents two different,
 *   unrelated constructions from using the same string;
 * - that this value, once used, will never be reassigned or reused by a
 *   future system;
 * - that any storage, provenance, or integrity guarantee attaches to it.
 *
 * Governed acceptance (Implementation Plan Section 4, Phase 3), storage
 * (Phase 2), and any guarantee of stable, non-reassigned, unique issuance
 * are future phases' responsibility, not this type's. This type supplies
 * only the shared vocabulary -- "an identity of this shape exists" -- that
 * those later phases will need in order to be built at all.
 */
@JvmInline
value class EvidenceArtifactId(val value: String) {
    init {
        require(value.isNotBlank()) { "EvidenceArtifactId must not be blank" }
    }
}
