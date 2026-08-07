package parker.core.interfaces

/**
 * Trust Framework Authorization Purpose vocabulary contracts
 * (`docs/architecture/AUTHORIZATION_PURPOSE_SCOPE_LOCK.md` §2.3;
 * `docs/governance/AUTHORIZATION_PURPOSE_VOCABULARY_GOVERNANCE_CONTRACT_DESIGN.md`).
 * Administratively separate from the Action Vocabulary contracts
 * (`src/contracts/ActionMapping.kt`) -- parallel, not overlapping
 * (Vocabulary Governance Contract Design §13): this vocabulary answers
 * *for what governed reason* an act is proposed, never *what kind* of
 * act it is.
 */

/**
 * A registered value's own lifecycle status. `RETIRED` values are never
 * deleted (Scope Lock §2.3: "retirement without deletion") -- they remain
 * looked-up, auditable, and ineligible for new policy authorship.
 */
enum class AuthorizationPurposeStatus {
    ACTIVE,
    RETIRED,
}

/** A registered Authorization Purpose value and its current lifecycle status. */
data class AuthorizationPurposeEntry(
    val id: AuthorizationPurposeId,
    val status: AuthorizationPurposeStatus,
)

/**
 * Result of a registration attempt. Mirrors `VocabularyRegistrationOutcome`'s
 * own three-variant shape (`src/contracts/ActionMapping.kt`) exactly, since
 * both vocabularies share the identical additive, reject-on-conflict
 * registration discipline (Scope Lock §2.3).
 */
sealed class AuthorizationPurposeRegistrationOutcome {
    data class Registered(val id: AuthorizationPurposeId) : AuthorizationPurposeRegistrationOutcome()
    data class AlreadyRegistered(val id: AuthorizationPurposeId) : AuthorizationPurposeRegistrationOutcome()
    data class Rejected(val reason: String) : AuthorizationPurposeRegistrationOutcome()
}

/**
 * Result of a retirement attempt. Retirement is a one-way lifecycle
 * transition (Scope Lock §2.3: "a registered value's own meaning cannot
 * change") -- retiring an already-retired value is `AlreadyRetired`, never
 * an error; retiring a value never registered at all is `Rejected`, never
 * a silent no-op.
 */
sealed class AuthorizationPurposeRetirementOutcome {
    data class Retired(val id: AuthorizationPurposeId) : AuthorizationPurposeRetirementOutcome()
    data class AlreadyRetired(val id: AuthorizationPurposeId) : AuthorizationPurposeRetirementOutcome()
    data class Rejected(val reason: String) : AuthorizationPurposeRetirementOutcome()
}
