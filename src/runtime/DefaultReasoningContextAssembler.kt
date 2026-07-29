package parker.core.runtime

import parker.core.interfaces.ConversationHistorySource
import parker.core.interfaces.IdentityService
import parker.core.interfaces.KnowledgeQuery
import parker.core.interfaces.KnowledgeSource
import parker.core.interfaces.ReasoningContext
import parker.core.interfaces.ReasoningContextAssembler
import parker.core.interfaces.ResolvedInboundMessage
import parker.core.interfaces.ToolRegistry
import parker.core.interfaces.WorldModelSource
import parker.core.interfaces.WorldQuery

/**
 * Default, production [ReasoningContextAssembler] implementation, Sprint
 * 11 Unit 3, revised Sprint 11 Unit 5 (Conversation Continuity
 * Implementation) for the [ResolvedInboundMessage] input-shape change
 * described in [ReasoningContextAssembler]'s own KDoc, revised again
 * Sprint 11 Unit 6 (Conversation History Source) to add
 * [conversationHistorySource], revised again Sprint 11 Unit 7 (Memory
 * Source Integration) to add [memorySource], and revised again Sprint 11
 * Unit 8 (World Model Source Integration) to add [worldModelSource].
 * Implements exactly what
 * `docs/architecture/PRODUCTION_REASONING_CONTEXT_CONTRACT_DESIGN.md`
 * ("the Contract Design") Section 4.3 authorises -- constructor injection
 * of [identityService] and [toolRegistry] -- plus the three additional,
 * narrow, read-only collaborators
 * `docs/architecture/CONVERSATION_HISTORY_SOURCE_CONTRACT_DESIGN.md`
 * Section 5, `docs/architecture/MEMORY_SOURCE_CONTRACT_DESIGN.md`
 * Section 4, and `docs/architecture/WORLD_MODEL_SOURCE_CONTRACT_DESIGN.md`
 * Section 2 now authorise. [conversationHistorySource]'s own declared
 * type is deliberately not [parker.core.interfaces.ConversationEngine] --
 * it is the separate, narrower [ConversationHistorySource] interface,
 * structurally incapable of resolving continuity or submitting a Turn,
 * even though the same production instance happens to implement both
 * (in particular, [parker.core.interfaces.ConversationEngine] itself is
 * still never referenced by this class -- the
 * [parker.core.interfaces.ConversationId] this class reads off
 * [ResolvedInboundMessage] was already resolved elsewhere; this class
 * performs no lookup, no resolution, and no mutation of it).
 *
 * Holds only its five collaborators as fields -- no cache of any prior
 * resolution, no mutable state of any kind (Contract Design Section 5,
 * Statelessness). [assemble] reads each collaborator fresh, once, every
 * call.
 *
 * ## What this class renders, and why
 *
 * Per Contract Design Section 2, four of the seven Scope Lock Section 1
 * items require no dependency at all -- each is already a field on the
 * one input:
 *
 * - "Current request" -- `InboundOwnerMessage.text`.
 * - "Active communication channel" -- `InboundOwnerMessage.channelId`.
 * - "Current time" -- `InboundOwnerMessage.timestamp`.
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
 * One item, revised by Sprint 11 Unit 5, is now rendered from the
 * envelope directly, with no dependency at all:
 *
 * - "Current conversation" -- rendered as the resolved
 *   `ConversationId`'s own value only (`resolvedMessage.conversationId`),
 *   never prior Turns, never conversation history. Continuity Contract
 *   Design Section 4.4 authorises this class to *read* the already-resolved
 *   identifier; it does not authorise a history read, which remains a
 *   real, named, future Conversation History Source boundary this Unit
 *   is not authorised to design or work around (`IMPLEMENTATION_GAPS.md`,
 *   Gap #53 or successor). No lookup, no `ConversationEngine` call, and
 *   no mutation occur to produce this entry -- it is a direct projection
 *   of a value already present on this method's own input, exactly like
 *   "Current time" or "Current request" above.
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
 * ## Conversation History (Sprint 11 Unit 6)
 *
 * One further item is rendered from [conversationHistorySource]:
 *
 * - "Prior messages" -- [ConversationHistorySource.history], called once
 *   with `resolvedMessage.conversationId`
 *   (`docs/architecture/CONVERSATION_HISTORY_SOURCE_CONTRACT_DESIGN.md`
 *   Section 5). Zero or more entries are rendered, one per returned
 *   `Turn`, oldest first, immediately after "Current conversation." An
 *   empty result renders no entries at all -- not an empty placeholder
 *   entry -- exactly as "no tools" would render nothing from
 *   [toolRegistry] today.
 *
 * No truncation, ranking, or volume limit is applied here -- every `Turn`
 * [ConversationHistorySource.history] returns is rendered (Contract Design
 * Section 5, a disclosed, deliberate scope boundary, not an oversight).
 * This class performs no lookup beyond the one [ConversationHistorySource.history]
 * call -- no second call, no caching across invocations (Statelessness,
 * Contract Design Section 5 restated).
 *
 * ## Memory (Sprint 11 Unit 7)
 *
 * One further item is rendered from [memorySource]. Unlike
 * [conversationHistorySource], `KnowledgeSource.recall` requires a full
 * [KnowledgeQuery] -- there is no already-resolved key to read the way
 * `resolvedMessage.conversationId` already is. This class therefore
 * constructs the [KnowledgeQuery] itself, using no new derived concept, only
 * fields already present on its own input
 * (`docs/architecture/MEMORY_SOURCE_CONTRACT_DESIGN.md` Section 5):
 *
 * - `requestingPrincipalId` = `message.senderPrincipalId` -- the same
 *   `PrincipalId` already used to render "Requesting principal."
 * - `relevance` = `message.text` -- the current request's own text,
 *   reusing [KnowledgeStore][parker.core.interfaces.KnowledgeStore]'s existing,
 *   already-implemented, already-tested case-insensitive substring match.
 *   This is not semantic search and invents no ranking algorithm; it
 *   supplies the one value [KnowledgeQuery.relevance] already, contractually,
 *   requires every caller to provide.
 * - `correlationId` = `message.correlationId.value`.
 * - `maximumResults` = [MEMORY_QUERY_MAXIMUM_RESULTS] -- an
 *   implementation-defined bound (Contract Design Section 5); not
 *   architecturally significant, and may change without a Contract Design
 *   revision.
 * - `category` = `null` -- no category narrowing (Contract Design
 *   Section 5).
 *
 * - "Memories" -- [KnowledgeSource.recall], called once with the constructed
 *   `KnowledgeQuery`. Zero or more entries are rendered, one per returned
 *   `KnowledgeRecord`, **in the order [KnowledgeSource.recall] returns them** --
 *   this class does not rank, score, reorder, summarise, or interpret
 *   what it receives (Contract Design Section 6, Section 8). An empty
 *   result renders no entries at all, exactly as "no tools" and "no prior
 *   Turns" already render nothing. Each rendered entry surfaces only
 *   fields [parker.core.interfaces.KnowledgeRecord] already carries
 *   (`knowledgePayload`, `sourceSubsystem`, and `confidence` where
 *   present) -- no confidence is computed, estimated, or defaulted where
 *   absent, and no provenance is fabricated.
 *
 * ## World Model (Sprint 11 Unit 8)
 *
 * One further item is rendered from [worldModelSource]. Unlike
 * [conversationHistorySource], `WorldModelSource.recall` requires a full
 * [WorldQuery] -- there is no already-resolved key to read the way
 * `resolvedMessage.conversationId` already is, and unlike [memorySource],
 * no field on this method's own input represents a world-model subject:
 * `WorldQuery.subjectMatch` matches a structured topic key, not free-text
 * request content, so reusing the request's own text the way `KnowledgeQuery.relevance`
 * does would require inventing a classification step this Unit's own
 * Scope Lock excludes
 * (`docs/architecture/WORLD_MODEL_SOURCE_QUERY_CONSTRUCTION_DECISION.md`).
 * This class therefore constructs a [WorldQuery] with:
 *
 * - `subjectMatch = null` -- no subject filter, per
 *   `docs/architecture/WORLD_QUERY_OPTIONAL_SUBJECT_CONTRACT_REVISION.md`.
 *   This is a literal absence of a filter, not an inferred, classified, or
 *   parsed value -- this class does not examine `message.text` (or any
 *   other field) to decide a subject.
 * - `maximumResults` = [WORLD_QUERY_MAXIMUM_RESULTS] -- an
 *   implementation-defined bound, mirroring [MEMORY_QUERY_MAXIMUM_RESULTS]'s
 *   identical treatment; not architecturally significant.
 * - `minimumConfidence` = `null` -- no confidence floor. Nothing in the
 *   approved design requires this class to supply one, and inventing a
 *   threshold here would be exactly the kind of unrequested filtering
 *   policy `WORLD_MODEL_SOURCE_CONTRACT_DESIGN.md` reserves entirely for
 *   the World Model implementation, never the Assembler.
 *
 * - "World beliefs" -- [WorldModelSource.recall], called once with the
 *   constructed `WorldQuery`. Zero or more entries are rendered, one per
 *   returned `WorldBelief`, **in the order [WorldModelSource.recall]
 *   returns them** -- this class does not rank, score, reorder,
 *   summarise, reconcile, or interpret what it receives, and the World
 *   Model's own `query` makes no ordering guarantee at all (unlike
 *   Memory's deterministic ordering), so no particular order is ever
 *   assumed here beyond "whatever `recall` returned." An empty result
 *   renders no entries at all, exactly as "no tools," "no prior Turns,"
 *   and "no memories" already render nothing. Each rendered entry
 *   surfaces only fields [parker.core.interfaces.WorldBelief] already
 *   carries (`subject`, `value`, `confidence`, `source`) -- `confidence`
 *   is a required field on `WorldBelief` (unlike `KnowledgeRecord`'s optional
 *   one), so it is always rendered, never fabricated or computed; no
 *   provenance beyond `source` is invented.
 *
 * @param identityService Read use only (`resolve`) -- Contract Design
 *   Section 4.1.
 * @param toolRegistry Read use only (`listAll`) -- Contract Design
 *   Section 4.1.
 * @param conversationHistorySource Read use only (`history`) -- Sprint 11
 *   Unit 6 Contract Design Section 5. This class never calls
 *   `ConversationEngine`; [conversationHistorySource] is a separate,
 *   narrower type that cannot reach `resolveConversationId` or
 *   `submitTurn` even if this class wished to.
 * @param memorySource Read use only (`recall`) -- Sprint 11 Unit 7
 *   `docs/architecture/MEMORY_SOURCE_CONTRACT_DESIGN.md` Section 5. This
 *   class never calls `KnowledgeStore.remember` or `KnowledgeStore.forget`;
 *   [memorySource] is a separate, narrower type that cannot reach either
 *   even if this class wished to.
 * @param worldModelSource Read use only (`recall`) -- Sprint 11 Unit 8
 *   `docs/architecture/WORLD_MODEL_SOURCE_CONTRACT_DESIGN.md` Section 2.
 *   This class never calls `WorldModel.observe`; [worldModelSource] is a
 *   separate, narrower type that cannot reach it even if this class
 *   wished to.
 */
class DefaultReasoningContextAssembler(
    private val identityService: IdentityService,
    private val toolRegistry: ToolRegistry,
    private val conversationHistorySource: ConversationHistorySource,
    private val memorySource: KnowledgeSource,
    private val worldModelSource: WorldModelSource,
) : ReasoningContextAssembler {

    private companion object {
        /**
         * The `maximumResults` bound this Assembler supplies on every
         * [KnowledgeQuery] it constructs. **Implementation policy, not
         * architecture** -- `docs/architecture/MEMORY_SOURCE_CONTRACT_DESIGN.md`
         * Section 5 deliberately does not fix this figure, so that the
         * architecture remains neutral to whatever bounding policy a future
         * revision chooses (a fixed limit, a dynamic token budget, a
         * model-specific limit, a configurable policy value). This
         * constant may be changed freely by a future implementation unit
         * without requiring an architecture or Contract Design revision --
         * changing it is an implementation change, not an architectural
         * one.
         */
        const val MEMORY_QUERY_MAXIMUM_RESULTS = 5

        /**
         * The `maximumResults` bound this Assembler supplies on every
         * [WorldQuery] it constructs. **Implementation policy, not
         * architecture**, mirroring [MEMORY_QUERY_MAXIMUM_RESULTS]'s
         * identical treatment -- `docs/architecture/WORLD_MODEL_SOURCE_CONTRACT_DESIGN.md`
         * does not fix this figure either. May be changed freely by a
         * future implementation unit without requiring an architecture or
         * Contract Design revision.
         */
        const val WORLD_QUERY_MAXIMUM_RESULTS = 5
    }

    override suspend fun assemble(resolvedMessage: ResolvedInboundMessage): ReasoningContext {
        val message = resolvedMessage.message
        val entries = mutableListOf<String>()

        val requester = identityService.resolve(message.senderPrincipalId)
        entries += if (requester != null) {
            "Requesting principal: ${requester.displayName} (${message.senderPrincipalId.value})"
        } else {
            "Requesting principal: ${message.senderPrincipalId.value} (identity not resolved)"
        }

        entries += "Communication channel: ${message.channelId.value}"
        entries += "Current time: ${message.timestamp}"
        // Sprint 11 Unit 5: the already-resolved ConversationId, read only -- no lookup, no
        // resolution, no mutation (Continuity Contract Design Section 4.4/11). This class never
        // calls ConversationEngine; resolvedMessage.conversationId was decided entirely by the
        // Production Composition Root before assemble() was invoked.
        entries += "Current conversation: ${resolvedMessage.conversationId.value}"

        // Sprint 11 Unit 6: prior Turns for this conversation, oldest first, rendered as-is --
        // no ranking, no truncation, no summarisation (Conversation History Source Contract
        // Design Section 5). One-sided: only the owner's own prior messages are ever available
        // (Contract Design Section 4.3) -- no Turn record anywhere captures Parker's own replies.
        conversationHistorySource.history(resolvedMessage.conversationId).forEach { turn ->
            entries += "Prior message: ${turn.message.text} (from ${turn.message.senderPrincipalId.value}, at ${turn.receivedAt})"
        }

        // Sprint 11 Unit 7: memories relevant to the current request, rendered in the exact
        // order KnowledgeSource.recall returns them -- no ranking, no scoring, no reordering, no
        // summarisation, no interpretation (Memory Source Contract Design Section 6/8). The
        // KnowledgeQuery below is constructed entirely from fields already present on this
        // method's own input; maximumResults is implementation-defined, never architecturally
        // significant (Contract Design Section 5).
        val memoryQuery = KnowledgeQuery(
            requestingPrincipalId = message.senderPrincipalId,
            relevance = message.text,
            correlationId = message.correlationId.value,
            maximumResults = MEMORY_QUERY_MAXIMUM_RESULTS,
            category = null,
        )
        memorySource.recall(memoryQuery).forEach { record ->
            val confidencePart = record.confidence?.let { ", confidence: $it" }.orEmpty()
            entries += "Memory: ${record.knowledgePayload} (source: ${record.sourceSubsystem}$confidencePart)"
        }

        // Sprint 11 Unit 8: current world-model beliefs, rendered in the exact order
        // WorldModelSource.recall returns them -- no ranking, no scoring, no reordering, no
        // reconciliation, no interpretation (World Model Source Contract Design Section 3/6).
        // subjectMatch is null (no subject filter, per the Optional Subject contract revision) --
        // a literal absence of a filter, never an inferred, classified, or parsed subject.
        val worldQuery = WorldQuery(
            subjectMatch = null,
            maximumResults = WORLD_QUERY_MAXIMUM_RESULTS,
            minimumConfidence = null,
        )
        worldModelSource.recall(worldQuery).forEach { belief ->
            entries += "World belief: ${belief.subject} = ${belief.value} (confidence: ${belief.confidence}, source: ${belief.source})"
        }

        toolRegistry.listAll().forEach { tool ->
            entries += "Available tool: ${tool.displayName} -- ${tool.description}"
        }

        entries += "Current request: ${message.text}"

        return ReasoningContext(entries.toList())
    }
}
