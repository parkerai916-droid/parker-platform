package parker.core.runtime

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import parker.core.interfaces.AuthorizationPurposeEntry
import parker.core.interfaces.AuthorizationPurposeId
import parker.core.interfaces.AuthorizationPurposeRegistrationOutcome
import parker.core.interfaces.AuthorizationPurposeRetirementOutcome
import parker.core.interfaces.AuthorizationPurposeStatus

/**
 * Trust Framework Authorization Purpose vocabulary
 * (`docs/architecture/AUTHORIZATION_PURPOSE_SCOPE_LOCK.md` §2.3;
 * Authorization Purpose Implementation Plan, Unit 3). Administratively
 * separate from [ActionVocabulary] (Vocabulary Governance Contract Design
 * §13) -- a distinct registry, never a shared table, never touching
 * [ActionVocabulary]'s own state or interface.
 */
interface AuthorizationPurposeRegistry {
    suspend fun register(id: AuthorizationPurposeId): AuthorizationPurposeRegistrationOutcome
    suspend fun retire(id: AuthorizationPurposeId): AuthorizationPurposeRetirementOutcome
    suspend fun lookup(id: AuthorizationPurposeId): AuthorizationPurposeEntry?
    suspend fun isActive(id: AuthorizationPurposeId): Boolean
}

/**
 * In-memory Authorization Purpose registry, mirroring
 * [InMemoryActionVocabulary]'s own established shape (`Mutex`-protected
 * `mutableMapOf`, additive, reject-on-conflict) with two additions Scope
 * Lock §2.3 requires that Action Vocabulary has never needed: retirement
 * without deletion, and registration-time namespace validation (deferred
 * here from `AuthorizationPurposeId`'s own constructor, per that type's
 * own KDoc and the Implementation Plan's own Unit 1 stop condition).
 *
 * ## Namespace convention -- frozen by Vocabulary Governance Contract
 * Design §12, not `EventType`'s own different convention
 *
 * `<domain>.<purpose>` for core-platform values; `plugin:<pluginId>:<purpose>`
 * for Plugin-supplied ones (colon-separated after `plugin:`, matching
 * `action-mapping.md`'s own "Plugin Supplied Actions" precedent exactly --
 * not `EventType`'s own `plugin:<pluginId>.<event>` dot-separated
 * convention, a different, already-existing, and equally valid convention
 * for a different vocabulary, governed by a different specification).
 *
 * ## No registration-time access control
 *
 * Mirrors [InMemoryActionVocabulary] exactly: this class enforces no
 * access control of its own on who may call [register]/[retire] -- access
 * is governed entirely by who holds a reference to this instance, decided
 * at Runtime composition time (a later Unit's own responsibility), never
 * by this registry itself. A Plugin's own "never exceeding what its own
 * Principal could ever be granted" ceiling (Scope Lock §2.3) is likewise a
 * governance-process discipline this class does not, and structurally
 * cannot, evaluate -- it holds no `PermissionEngine`/`Principal` dependency
 * of any kind.
 */
class InMemoryAuthorizationPurposeRegistry : AuthorizationPurposeRegistry {

    private val mutex = Mutex()
    private val entries = mutableMapOf<AuthorizationPurposeId, AuthorizationPurposeEntry>()

    override suspend fun register(id: AuthorizationPurposeId): AuthorizationPurposeRegistrationOutcome = mutex.withLock {
        if (!hasGovernedNamespaceShape(id.value)) {
            return@withLock AuthorizationPurposeRegistrationOutcome.Rejected(
                "AuthorizationPurposeId '${id.value}' must be namespaced as <domain>.<purpose> or " +
                    "plugin:<pluginId>:<purpose>",
            )
        }
        when (val existing = entries[id]) {
            null -> {
                entries[id] = AuthorizationPurposeEntry(id, AuthorizationPurposeStatus.ACTIVE)
                AuthorizationPurposeRegistrationOutcome.Registered(id)
            }
            else -> when (existing.status) {
                AuthorizationPurposeStatus.ACTIVE -> AuthorizationPurposeRegistrationOutcome.AlreadyRegistered(id)
                AuthorizationPurposeStatus.RETIRED -> AuthorizationPurposeRegistrationOutcome.Rejected(
                    "AuthorizationPurposeId '${id.value}' is retired -- a retired value's own meaning is " +
                        "immutable and may not be re-registered",
                )
            }
        }
    }

    override suspend fun retire(id: AuthorizationPurposeId): AuthorizationPurposeRetirementOutcome = mutex.withLock {
        when (val existing = entries[id]) {
            null -> AuthorizationPurposeRetirementOutcome.Rejected(
                "AuthorizationPurposeId '${id.value}' is not registered -- only a registered value may be retired",
            )
            else -> when (existing.status) {
                AuthorizationPurposeStatus.RETIRED -> AuthorizationPurposeRetirementOutcome.AlreadyRetired(id)
                AuthorizationPurposeStatus.ACTIVE -> {
                    entries[id] = existing.copy(status = AuthorizationPurposeStatus.RETIRED)
                    AuthorizationPurposeRetirementOutcome.Retired(id)
                }
            }
        }
    }

    override suspend fun lookup(id: AuthorizationPurposeId): AuthorizationPurposeEntry? = mutex.withLock {
        entries[id]
    }

    override suspend fun isActive(id: AuthorizationPurposeId): Boolean = mutex.withLock {
        entries[id]?.status == AuthorizationPurposeStatus.ACTIVE
    }

    /**
     * Vocabulary Governance Contract Design §12's own two governed shapes,
     * checked structurally: a non-blank segment before a `.` and a
     * non-blank segment after it (`<domain>.<purpose>`), or a non-blank
     * Plugin id and a non-blank purpose either side of a second `:`
     * following a literal `plugin:` prefix (`plugin:<pluginId>:<purpose>`).
     * `AuthorizationPurposeId`'s own constructor already rejects a blank
     * value entirely (Unit 1); this check is strictly about namespace
     * *shape*, deliberately deferred here per that type's own stop
     * condition.
     */
    private fun hasGovernedNamespaceShape(value: String): Boolean {
        if (value.startsWith("plugin:")) {
            val afterPrefix = value.removePrefix("plugin:")
            val pluginId = afterPrefix.substringBefore(':', missingDelimiterValue = "")
            val purpose = afterPrefix.substringAfter(':', missingDelimiterValue = "")
            return pluginId.isNotBlank() && purpose.isNotBlank()
        }
        val domain = value.substringBefore('.', missingDelimiterValue = "")
        val purpose = value.substringAfter('.', missingDelimiterValue = "")
        return domain.isNotBlank() && purpose.isNotBlank()
    }
}
