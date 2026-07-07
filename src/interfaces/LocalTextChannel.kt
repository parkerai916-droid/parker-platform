package parker.core.interfaces

import java.time.Instant

/**
 * Local Text Channel (Sprint 7, Unit C3), implementing exactly the shape
 * approved by `docs/architecture/LOCAL_TEXT_CHANNEL_CONTRACT_DESIGN.md`
 * ("the Contract Design"), Section 1. This file introduces **no new
 * public data type** — the Contract Design's own Minimalism Review found
 * none required (Section 2), and this file does not depart from that
 * finding.
 *
 * `LocalTextChannel` is the field-level shape of "the local text
 * channel" `docs/architecture/COMMUNICATION_CHANNEL_ARCHITECTURE.md`
 * Section 6 names as Parker's first concrete Communication Channel. It
 * is the boundary between raw owner text — already obtained, by
 * whatever mechanism a future, separately-scoped unit supplies (Contract
 * Design Section 10, Deferred Item 1; this document's own "must not
 * implement CLI terminal behaviour" instruction) — and the already
 * approved and implemented `CommunicationIntake` (Sprint 7, Unit C1).
 *
 * **Scope, restated from the Contract Design.** An implementation of
 * this interface:
 * - mints a fresh [CorrelationId] once per call (Contract Design Section
 *   5), per IDR-001 (`docs/implementation/LOCAL_TEXT_CHANNEL_IMPLEMENTATION_PLAN.md`
 *   Section 4): a randomly generated UUID, since this channel has no
 *   stable parent identifier to derive one from deterministically;
 * - constructs an [InboundOwnerMessage] using this channel's own fixed
 *   [ModuleId] as `channelId` (Contract Design Section 1's "one channel,
 *   one `ModuleId`");
 * - submits that message, unchanged, to the injected
 *   [CommunicationIntake]'s [CommunicationIntake.submitInboundMessage];
 *   and
 * - returns the resulting [CommunicationIntakeDisposition] unchanged —
 *   no translation, wrapping, or reinterpretation of it.
 *
 * **What an implementation must not do (Contract Design Constitutional
 * Boundaries, Section 1):** interpret `text` beyond what constructing an
 * [InboundOwnerMessage] requires; decide whether the message implies an
 * action; construct or reference an `ExecutionRequest`; perform outbound
 * delivery or construct an `OutboundParkerResponse`; or call anything
 * other than [CommunicationIntake].
 *
 * **Dependencies.** An implementation's only collaborator is
 * [CommunicationIntake]. No dependency on [ModuleRegistry],
 * [IdentityService], `ExecutionPipeline`, `ToolRegistry`,
 * `PermissionEngine`, `PlannerRuntime`, `AgentRuntime`, `MemoryStore`, or
 * `WorldModel` is introduced or authorised by the Contract Design.
 *
 * **Validation.** No new validation rule is introduced. `text` must be
 * non-blank, enforced by [InboundOwnerMessage]'s own existing
 * constructor validation (Contract Design Section 3) — an
 * implementation must not duplicate this check itself, and a blank
 * `text` fails exactly where it already fails today, via that
 * constructor, before [CommunicationIntake] is ever called.
 *
 * **Thread safety.** This operation is `suspend`-declared (Contract
 * Design Section 8, PES-001 Chapter 7.2's "suspend-capable" guidance).
 * Per IDR-001, a concrete implementation mints each [CorrelationId] with
 * no shared, mutable state, so no additional synchronisation is required
 * beyond what [CommunicationIntake]'s own implementation already
 * guarantees for concurrent submissions.
 */
interface LocalTextChannel {

    /**
     * Submits [text], on behalf of [senderPrincipalId], as one inbound
     * owner message.
     *
     * @param text The owner's already-obtained input text. Must be
     *   non-blank — enforced by [InboundOwnerMessage]'s own constructor,
     *   not by this operation itself.
     * @param senderPrincipalId The `Principal` responsible for [text]
     *   (Contract Design Section 6). Not resolved, authenticated, or
     *   verified by this operation — that remains
     *   [CommunicationIntake.submitInboundMessage]'s own, existing
     *   sender-resolution check. Supplied by the caller; how a caller
     *   determines which `PrincipalId` is "the owner" is Deferred
     *   (Contract Design Section 10, Deferred Item 2).
     * @param timestamp When the owner sent [text], not when this
     *   operation happens to be called. Defaults to the current time.
     * @param metadata A channel-specific, non-authoritative extension
     *   point, threaded unchanged into the constructed
     *   [InboundOwnerMessage]. Defaults to empty.
     * @return The [CommunicationIntakeDisposition] [CommunicationIntake]
     *   produced, unchanged.
     */
    suspend fun submitOwnerText(
        text: String,
        senderPrincipalId: PrincipalId,
        timestamp: Instant = Instant.now(),
        metadata: Map<String, String> = emptyMap(),
    ): CommunicationIntakeDisposition
}
