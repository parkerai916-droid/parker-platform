package parker.core.interfaces

/**
 * Reasoning Context Assembler contract (Sprint 11, Unit 3 -- Production
 * Reasoning Context Implementation), implementing exactly the shape
 * approved by `docs/architecture/PRODUCTION_REASONING_CONTEXT_CONTRACT_DESIGN.md`
 * ("the Contract Design") Section 3, itself built on
 * `docs/implementation/PRODUCTION_REASONING_CONTEXT_SCOPE_LOCK.md`
 * ("the Scope Lock", frozen in Unit 1) and
 * `docs/implementation/PRODUCTION_REASONING_CONTEXT_IMPLEMENTATION_PLAN.md`.
 * No concept here is new: this file is the Contract Design's Section 3
 * illustrative signature promoted, unchanged, to real production code.
 *
 * One operation, mirroring this codebase's own established convention --
 * [ConversationEngine], [ReasoningProvider], and [CommunicationIntake]
 * are each exactly one method (Contract Design Section 3).
 *
 * **One input:** the raw [InboundOwnerMessage] -- and nothing else.
 * Contract Design Section 2 explains why: the Production Composition
 * Root invokes this Assembler *before*
 * `CommunicationConversationCoordinator.submitAndReason` runs, which is,
 * in turn, before [ConversationEngine.submitTurn] constructs a `Turn` --
 * no `Turn` exists yet at the point an implementation of this interface
 * runs.
 *
 * **One output:** a [ReasoningContext] -- the existing, frozen,
 * unmodified shape defined in `ReasoningProvider.kt`. This interface
 * introduces no new return type.
 *
 * **Ownership (Scope Lock Section 4, restated here as the contract's own
 * binding term).** An implementation of this interface is the sole
 * constructor of [ReasoningContext] in production. The Production
 * Composition Root ([parker.composition.ParkerRuntime]) is the sole
 * production invoker of [assemble]. No frozen coordinator between
 * `ParkerRuntime.submitOwnerMessage` and the Reasoning Provider
 * constructs one itself -- each already states, in its own governing
 * document, that it does not.
 *
 * **Contract principles (Contract Design Section 5), binding on every
 * implementation, not merely aspirational:** deterministic; stateless
 * (no mutable field retained between calls); side-effect free (never
 * writes to Memory, the World Model, `IdentityService`, `ToolRegistry`,
 * or anywhere else); assembles a projection only (Scope Lock Section 3)
 * -- it does not cache, persist, plan, invoke a Tool, invoke the
 * Permission Engine, or invoke the Execution Pipeline.
 *
 * **Failure behaviour (Contract Design Section 6).** An implementation
 * is not authorised to define its own error-handling mechanism. A
 * genuine failure during [assemble] is not caught here -- it propagates
 * unchanged to the Production Composition Root's own existing outer
 * `try`/`catch` (`ParkerRuntime.submitOwnerMessage`), exactly the same
 * "propagates unchanged to the caller" discipline every frozen
 * coordinator already holds itself to. An implementation must never
 * swallow such a failure to substitute a degraded-but-valid
 * [ReasoningContext] -- Contract Design Section 6 leaves that question
 * open for a future revision, not silently decided here.
 */
fun interface ReasoningContextAssembler {
    suspend fun assemble(message: InboundOwnerMessage): ReasoningContext
}
