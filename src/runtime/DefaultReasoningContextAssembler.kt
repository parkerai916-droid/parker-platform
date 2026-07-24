package parker.core.runtime

import parker.core.interfaces.IdentityService
import parker.core.interfaces.InboundOwnerMessage
import parker.core.interfaces.ReasoningContext
import parker.core.interfaces.ReasoningContextAssembler
import parker.core.interfaces.ToolRegistry

/**
 * Default, production [ReasoningContextAssembler] implementation
 * (Sprint 11, Unit 3), implementing exactly what
 * `docs/architecture/PRODUCTION_REASONING_CONTEXT_CONTRACT_DESIGN.md`
 * ("the Contract Design") Section 4.3 authorises -- constructor injection
 * of [identityService] and [toolRegistry] only. **No** Memory Source,
 * World Model Source, or Conversation History Source dependency exists
 * anywhere in this class (Contract Design Section 4.2): each is a real,
 * named, future boundary this Unit is not authorised to design or work
 * around, and none is added here by injecting a broader existing
 * component (in particular, [parker.core.interfaces.ConversationEngine]
 * is deliberately never referenced by this class -- injecting it would
 * hand this Assembler the ability to call `submitTurn`, a mutating
 * operation flatly incompatible with Statelessness and Side-effect
 * freedom, Contract Design Section 5).
 *
 * Holds only its two collaborators as fields -- no cache of any prior
 * resolution, no mutable state of any kind (Contract Design Section 5,
 * Statelessness). [assemble] reads each collaborator fresh, once, every
 * call.
 *
 * ## What this class renders, and why
 *
 * Per Contract Design Section 2, four of the seven Scope Lock Section 1
 * items require no dependency at all -- each is already a field on the
 * one input parameter:
 *
 * - "Current request" -- [InboundOwnerMessage.text].
 * - "Active communication channel" -- [InboundOwnerMessage.channelId].
 * - "Current time" -- [InboundOwnerMessage.timestamp].
 *
 * Two more collapse into a single rendered entry today:
 *
 * - "Requesting principal identity" and "Participant identities" --
 *   Contract Design Section 4.1 justifies `IdentityService.resolve` for
 *   both together, "both require resolving a `PrincipalId`... to a
 *   `Principal`." [InboundOwnerMessage] carries exactly one
 *   [parker.core.interfaces.PrincipalId] (`senderPrincipalId`); no other
 *   participant's identity is available anywhere on the one input this
 *   Assembler receives (Contract Design Section 2). "Participant
 *   identities" therefore renders identically to "Requesting principal
 *   identity" today -- a single entry, not two -- until a Conversation
 *   History Source (Contract Design Section 4.2) can supply any other
 *   participant's `PrincipalId`. This is a disclosed, implementation-time
 *   scope observation, not a silent narrowing: see this Unit's own
 *   Implementation Review / architectural-observations record.
 *
 * One item is rendered from [toolRegistry]:
 *
 * - "Available tool descriptions" -- [ToolRegistry.listAll], the "full
 *   visible catalogue" read (Contract Design Section 4.1 chose this over
 *   [ToolRegistry.findCandidates] because the Assembler has no
 *   action/resourceType filter criteria to narrow by). Never
 *   [ToolRegistry.resolve] -- that method is the Execution-Pipeline-only
 *   lookup ([ToolRegistry]'s own KDoc), out of bounds for this Assembler
 *   (Contract Design Section 4.1).
 *
 * One item is **not** rendered at all:
 *
 * - "Current conversation" (prior Turns) -- Contract Design Section 4.2
 *   names a Conversation History Source boundary but confirms no
 *   existing interface in this codebase can supply it without handing
 *   this Assembler a mutating `ConversationEngine` reference. This class
 *   does not work around that gap; the assembled [ReasoningContext]
 *   simply carries no "current conversation" entry until a future Unit
 *   defines that boundary. Documented in `IMPLEMENTATION_GAPS.md`, not
 *   silently omitted.
 *
 * ## Failure behaviour
 *
 * No `try`/`catch` anywhere in this class (Contract Design Section 6):
 * if [identityService].resolve or [toolRegistry].listAll throws, the
 * fault propagates unchanged to [assemble]'s own caller (in production,
 * `ParkerRuntime.submitOwnerMessage`'s existing outer `try`/`catch`).
 * [IdentityService.resolve] returning `null` for an unresolvable sender
 * is not itself treated as a failure -- its own KDoc guarantees it never
 * throws for a not-found lookup ("mirroring `ResourceRegistry.resolve`'s
 * established pattern"); this class renders an explicit
 * "identity not resolved" fallback entry in that case rather than
 * fabricating a `displayName` or omitting the entry.
 *
 * @param identityService Read use only (`resolve`) -- Contract Design
 *   Section 4.1.
 * @param toolRegistry Read use only (`listAll`) -- Contract Design
 *   Section 4.1.
 */
class DefaultReasoningContextAssembler(
    private val identityService: IdentityService,
    private val toolRegistry: ToolRegistry,
) : ReasoningContextAssembler {

    override suspend fun assemble(message: InboundOwnerMessage): ReasoningContext {
        val entries = mutableListOf<String>()

        val requester = identityService.resolve(message.senderPrincipalId)
        entries += if (requester != null) {
            "Requesting principal: ${requester.displayName} (${message.senderPrincipalId.value})"
        } else {
            "Requesting principal: ${message.senderPrincipalId.value} (identity not resolved)"
        }

        entries += "Communication channel: ${message.channelId.value}"
        entries += "Current time: ${message.timestamp}"

        toolRegistry.listAll().forEach { tool ->
            entries += "Available tool: ${tool.displayName} -- ${tool.description}"
        }

        entries += "Current request: ${message.text}"

        return ReasoningContext(entries.toList())
    }
}
