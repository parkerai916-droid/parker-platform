package parker.composition

import java.security.MessageDigest
import java.nio.charset.StandardCharsets
import parker.core.interfaces.PrincipalId

/**
 * Parker Agent Gateway, AG-1E (R0 Agent Gateway Transport,
 * `docs/architecture/PARKER_AGENT_GATEWAY_SCOPE_LOCK.md` Section 10). The
 * dedicated Agent Gateway credential mechanism Section 10 requires --
 * "entirely separate from `OwnerUiAuthentication`... the one frozen
 * constraint is that it must never be, wrap, or derive from any Owner UI
 * session/pairing credential." This class never imports, constructs, reads,
 * or references [OwnerUiAuthentication] or any of its cookie names at all --
 * there is no code path in this class by which Owner UI authentication
 * material could ever be accepted as valid here.
 *
 * ## Credential shape: static, pre-shared bearer tokens
 *
 * Section 10 leaves exact credential shape Implementation-Plan-tier
 * ("bearer token, mTLS client cert, or another mechanism"). This is the
 * smallest safe choice consistent with an already-documented Parker
 * convention: [parker.composition.OwnerEvidenceHttpServer]'s own class KDoc
 * already describes an identical shape for its (currently unwired)
 * `ownerHttpToken` field -- "a single-owner bearer token... compared via
 * constantTimeEquals so a wrong guess cannot be narrowed by response
 * timing." This class is a fresh, genuinely wired implementation of that
 * same documented shape, scoped to the Agent Gateway's own separate token
 * -- not a reuse of Owner's own (currently dead) token field or code path.
 * No new secret-management system (rotation, hashing, a credential store)
 * is introduced: each credential is an opaque string, held only in runtime
 * configuration and sourced from the existing deployment secret/configuration
 * mechanism, never logged, never returned in any response, and never persisted
 * to disk by Parker itself.
 *
 * ## Resolution is fixed, not caller-supplied
 *
 * [authenticate] takes no principal of any kind as input. It returns only a
 * principal explicitly bound to a configured credential; there is no
 * parameter, header, or request field through which a caller can select
 * another principal (Owner, `INTERNAL_AGENT`, `PLUGIN`, `TOOL`, or arbitrary).
 */
data class AgentGatewayCredentialBinding(
    val token: String,
    val principalId: PrincipalId,
)

class AgentGatewayAuthentication(
    private val credentials: List<AgentGatewayCredentialBinding>,
) {
    constructor(configuredToken: String, hermesPrincipalId: PrincipalId) : this(
        listOf(AgentGatewayCredentialBinding(configuredToken, hermesPrincipalId)),
    )

    init {
        require(credentials.isNotEmpty()) { "AgentGatewayAuthentication requires at least one credential" }
        require(credentials.all { it.token.isNotBlank() }) { "AgentGatewayAuthentication requires non-blank tokens" }
        require(credentials.map { it.principalId }.distinct().size == credentials.size) {
            "AgentGatewayAuthentication requires one credential per principal"
        }
        require(credentials.map { it.token }.distinct().size == credentials.size) {
            "AgentGatewayAuthentication requires distinct credentials"
        }
    }

    /**
     * `null` if [providedToken] is absent or does not match a configured credential exactly.
     * Matching is constant-time for every configured credential; a successful match returns only
     * the principal bound to that credential, never a caller-supplied identity.
     */
    fun authenticate(providedToken: String?): PrincipalId? {
        if (providedToken.isNullOrEmpty()) return null
        val actual = providedToken.toByteArray(StandardCharsets.UTF_8)
        var matched: PrincipalId? = null
        credentials.forEach { credential ->
            val expected = credential.token.toByteArray(StandardCharsets.UTF_8)
            if (MessageDigest.isEqual(expected, actual)) matched = credential.principalId
        }
        return matched
    }
}
