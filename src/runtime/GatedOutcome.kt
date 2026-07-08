package parker.core.runtime

/**
 * A small, generic upstream-admission-gate wrapper (Sprint 7, Unit C2 —
 * Communication-to-Conversation Wiring), per
 * `docs/implementation/COMMUNICATION_TO_CONVERSATION_WIRING_IMPLEMENTATION_PLAN.md`
 * Section 5, Decision 1.
 *
 * Models exactly one thing: an upstream gate that either admits work,
 * producing exactly one value of type [T], or rejects it, with a reason.
 * Nothing in its shape refers to messages, channels, or Communication —
 * it is deliberately not a Communication-specific type, despite being
 * introduced by [CommunicationConversationCoordinator]. It is suitable
 * for reuse by future coordinators with the same gating semantics;
 * future units should reuse it where appropriate rather than introducing
 * an equivalent wrapper type of their own. This document does not claim
 * such reuse will occur — only that the shape does not preclude it.
 *
 * This type is a generic implementation-level utility, not a domain
 * contract: it carries no Parker-specific field, names no
 * Parker-specific concept, and is not shared across an architectural
 * trust boundary (Plan Section 5, Decision 1's own "why this does not
 * require a Stage 2A Contract Design pass" reasoning).
 */
sealed class GatedOutcome<out T> {

    /** The upstream gate admitted the work; [value] is what the downstream step produced. */
    data class Produced<out T>(val value: T) : GatedOutcome<T>()

    /** The upstream gate rejected the work before any downstream step ran. */
    data class NotAccepted(val reason: String) : GatedOutcome<Nothing>() {
        init {
            require(reason.isNotBlank()) { "GatedOutcome.NotAccepted.reason must not be blank" }
        }
    }
}
