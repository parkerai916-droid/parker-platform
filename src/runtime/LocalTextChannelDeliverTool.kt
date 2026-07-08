package parker.core.runtime

import parker.core.interfaces.ExecutionRequest
import parker.core.interfaces.PermissionAction
import parker.core.interfaces.ResourceType
import parker.core.interfaces.Tool
import parker.core.interfaces.ToolDescriptor
import parker.core.interfaces.ToolResult
import parker.core.interfaces.ValidationResult

/**
 * The Local Text Channel's real "deliver" Tool. Sprint 8, per
 * `docs/architecture/LOCAL_TEXT_CHANNEL_CONTRACT_DESIGN.md` (revised
 * Section 4), `docs/architecture/RESPONSE_DELIVERY_CONTRACT_DESIGN.md`
 * Section 4, and `docs/implementation/LOCAL_TEXT_CHANNEL_DELIVER_TOOL_IMPLEMENTATION_PLAN.md`
 * (Scope Locked). A concrete, non-interface-backed implementation of the
 * already-existing [Tool] interface -- no new public contract is
 * introduced (Plan Status; Locked Decision 1/2).
 *
 * **`toolId = "deliver"` is a plain `String`, not a wrapped type**
 * (Locked Decision 14, corrected: no `ToolId` value class exists
 * anywhere in this repository -- confirmed by direct reading of
 * `src/contracts/ToolDescriptor.kt`, whose `toolId` field is `String`).
 *
 * **Reads response text only from `request.metadata[RESPONSE_TEXT_METADATA_KEY]`
 * (Plan Decisions 5/6; Locked Decisions 4/5).** No other metadata key,
 * and no other field of [ExecutionRequest], is ever read by this class.
 *
 * **Pass-through, not reinterpretation (Plan Decision 7; Locked Decision
 * 6).** The text [execute] hands to the injected callback is exactly the
 * string read from `request.metadata` -- no trim, case change,
 * truncation, or reformatting of any kind.
 *
 * **Exactly one side effect (Plan Decision 8; Locked Decision 7).**
 * [execute] invokes [onOwnerNotified] exactly once, with the exact text
 * read. No file write, network call, `EventBus` publication, or other
 * observable effect exists anywhere in this class. This makes no claim
 * that a response is shown to a human being -- [onOwnerNotified] is
 * supplied entirely by the caller; a real, human-visible display
 * mechanism is a future, separately-scoped unit's responsibility, not
 * built or simulated here.
 *
 * **`descriptor` is the single canonical [ToolDescriptor] for this Unit
 * (Plan Decision 9's ownership rule; Locked Decisions 10/11/12).** It is
 * a computed property (`get() = ...`), not a stored value, so this class
 * declares no backing field for it -- every access returns a fresh but
 * structurally-equal [ToolDescriptor], satisfying
 * `InMemoryToolInvocationBinding.bind`'s `tool.descriptor == descriptor`
 * check (data-class structural equality, not reference equality) and
 * keeping this class's own declared fields limited to exactly
 * [onOwnerNotified]. Every other reference to this Tool's descriptor --
 * `ModuleDescriptor.toolsExposed`, `ToolInvocationBinding.bind`'s second
 * argument, and any test fixture -- must read it from [descriptor]
 * directly. No second `ToolDescriptor(...)` literal exists anywhere in
 * this class or its own tests.
 *
 * **Validation, not defensive re-checking in [execute] (Plan Decision 5;
 * Locked Decisions 8/9).** [validate] rejects a request whose
 * `RESPONSE_TEXT_METADATA_KEY` entry is missing or blank, before
 * [execute] is ever reached in ordinary use --
 * `DefaultExecutionPipeline.executeResolvedTool` already calls
 * [validate] before [execute] and short-circuits to a `FAILED`
 * `ExecutionResult` on `Invalid`, confirmed by direct reading of
 * `DefaultExecutionPipeline.kt`. [execute] does not duplicate that
 * check, mirroring `MockTool`'s own established precedent
 * (`tests/runtime/MockTool.kt`) of trusting the pipeline's own
 * validate-then-execute ordering rather than re-checking internally.
 *
 * **Statelessness.** The only declared field is [onOwnerNotified] -- no
 * `var`, no mutable collection, no cache of any prior request or result.
 */
class LocalTextChannelDeliverTool(
    private val onOwnerNotified: suspend (text: String) -> Unit,
) : Tool {

    override val descriptor: ToolDescriptor
        get() = ToolDescriptor(
            toolId = "deliver",
            displayName = "Local Text Channel Deliver Tool",
            description = "Delivers an already-authorised OutboundParkerResponse's text " +
                "to the Local Text Channel's owner.",
            supportedActions = setOf(PermissionAction.NOTIFY),
            supportedResourceTypes = setOf(ResourceType.TOOL),
        )

    /**
     * Rejects a request whose `metadata[RESPONSE_TEXT_METADATA_KEY]` is
     * missing or blank (Plan Decisions 5-6; Locked Decisions 8/9).
     * Accepts otherwise. This is the Tool's one and only structural
     * precondition -- no other check is performed.
     */
    override suspend fun validate(request: ExecutionRequest): ValidationResult {
        val text = request.metadata[RESPONSE_TEXT_METADATA_KEY]
        return if (text.isNullOrBlank()) {
            ValidationResult.Invalid(
                listOf("request.metadata[\"$RESPONSE_TEXT_METADATA_KEY\"] is missing or blank"),
            )
        } else {
            ValidationResult.Valid
        }
    }

    /**
     * Reads `request.metadata[RESPONSE_TEXT_METADATA_KEY]` unchanged
     * (Plan Decision 7; Locked Decision 6), invokes [onOwnerNotified]
     * with it exactly once (Plan Decision 8; Locked Decision 7), and
     * returns a successful [ToolResult]. Only ever reached, in ordinary
     * use, after [validate] has already returned
     * [ValidationResult.Valid] -- see this class's own KDoc.
     * `request.metadata.getValue` is used deliberately, not a
     * null-tolerant fallback: if this method is ever reached without
     * [validate] having passed first, that is a genuine precondition
     * violation this class does not silently paper over.
     */
    override suspend fun execute(request: ExecutionRequest): ToolResult {
        val text = request.metadata.getValue(RESPONSE_TEXT_METADATA_KEY)
        onOwnerNotified(text)
        return ToolResult(
            toolId = descriptor.toolId,
            success = true,
            output = mapOf("delivered" to "true"),
        )
    }
}
