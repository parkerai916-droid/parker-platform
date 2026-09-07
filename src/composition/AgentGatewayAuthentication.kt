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
 * ## Credential shape: a single, static, pre-shared bearer token
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
 * is introduced: the token is a single opaque string, held only in
 * [ParkerRuntimeConfig.agentGatewayHttpToken], sourced only from the
 * `PARKER_AGENT_GATEWAY_HTTP_TOKEN` environment variable, never logged,
 * never returned in any response, never persisted to disk by Parker itself.
 *
 * ## Resolution is fixed, not caller-supplied
 *
 * [authenticate] takes no principal of any kind as input and returns
 * exactly one of two things: `null` (no match), or [hermesPrincipalId] --
 * the one fixed value this class is constructed with. There is no
 * parameter, header, or request field through which a caller could ever
 * cause this method to resolve to any other principal (Owner,
 * `INTERNAL_AGENT`, `PLUGIN`, `TOOL`, or an arbitrary one).
 */
class AgentGatewayAuthentication(
    private val configuredToken: String,
    private val hermesPrincipalId: PrincipalId,
) {
    init {
        require(configuredToken.isNotBlank()) { "AgentGatewayAuthentication requires a non-blank configured token" }
    }

    /**
     * `null` if [providedToken] is absent or does not match [configuredToken] exactly (compared
     * in constant time via [MessageDigest.isEqual], the JDK's own standard constant-time
     * byte-array comparison, so a wrong guess cannot be narrowed by response timing); otherwise
     * [hermesPrincipalId] -- the only principal this method can ever return.
     */
    fun authenticate(providedToken: String?): PrincipalId? {
        if (providedToken.isNullOrEmpty()) return null
        val expected = configuredToken.toByteArray(StandardCharsets.UTF_8)
        val actual = providedToken.toByteArray(StandardCharsets.UTF_8)
        return if (MessageDigest.isEqual(expected, actual)) hermesPrincipalId else null
    }
}
