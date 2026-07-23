package parker.core.runtime

import kotlinx.coroutines.test.runTest
import parker.core.interfaces.ActionResourceMapping
import parker.core.interfaces.ActionVocabularyEntry
import parker.core.interfaces.CorrelationId
import parker.core.interfaces.DecisionId
import parker.core.interfaces.ExecutionResult
import parker.core.interfaces.ExecutionResultStatus
import parker.core.interfaces.InboundOwnerMessage
import parker.core.interfaces.ModuleConnectivityDeclaration
import parker.core.interfaces.ModuleDescriptor
import parker.core.interfaces.ModuleId
import parker.core.interfaces.OutboundParkerResponse
import parker.core.interfaces.PermissionAction
import parker.core.interfaces.PermissionDecision
import parker.core.interfaces.PermissionDecisionOutcome
import parker.core.interfaces.PermissionLevel
import parker.core.interfaces.Principal
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.PrincipalStatus
import parker.core.interfaces.PrincipalType
import parker.core.interfaces.ReasoningProviderResponse
import parker.core.interfaces.ResourceType
import java.lang.reflect.Modifier
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Sprint 10, Unit 1 acceptance test, per
 * `docs/implementation/RESPONSE_COMPOSER_SCOPE_LOCK.md` Section 11 (Scope
 * Locked). [ResponseComposer] is exercised in isolation via
 * [FakeIdentityService] for every structural, branch, and invariant-level
 * assertion, and once, end-to-end, against the real
 * `InMemoryResourceRegistry` / `InMemoryToolRegistry` /
 * `InMemoryModuleRegistry` / `InMemoryToolInvocationBinding` /
 * `DefaultExecutionPipeline` / `LocalTextChannelDeliverTool` /
 * `ResponseDelivery` stack (mirroring `LocalTextChannelDeliverToolTest.kt`'s
 * own established end-to-end pattern), to prove a composed response is
 * delivery-ready today -- without [ResponseComposer] itself ever calling
 * `ResponseDelivery`.
 */
class ResponseComposerTest {

    private val fixedTimestamp: Instant = Instant.parse("2026-01-01T00:00:00Z")
    private val channelId = ModuleId("channel.local-text")

    private fun message(
        text: String = "hello",
        correlationId: String = "corr-1",
        senderPrincipalId: String = "user-1",
        channel: ModuleId = channelId,
    ) = InboundOwnerMessage(
        channelId = channel,
        senderPrincipalId = PrincipalId(senderPrincipalId),
        text = text,
        timestamp = fixedTimestamp,
        correlationId = CorrelationId(correlationId),
    )

    private fun responseComposerPrincipal() = Principal(
        principalId = PrincipalId("system.response-composer"),
        principalType = PrincipalType.SYSTEM,
        displayName = "Response Composer",
        owner = null,
        status = PrincipalStatus.CREATED,
        createdAt = fixedTimestamp,
        lastSeenAt = fixedTimestamp,
    )

    /** A [FakeIdentityService] with `system.response-composer` registered. */
    private fun registeredIdentityService() = FakeIdentityService { principalId ->
        if (principalId == PrincipalId("system.response-composer")) responseComposerPrincipal() else null
    }

    /** A [FakeIdentityService] that resolves nothing -- simulates "unregistered". */
    private fun unregisteredIdentityService() = FakeIdentityService { null }

    // ================= Reply path =================

    @Test
    fun `a Reply composes into a Produced OutboundParkerResponse with correct fields, resolving identity exactly once`() = runTest {
        val identityService = registeredIdentityService()
        val composer = ResponseComposer(identityService)
        val originalMessage = message(text = "ignored", correlationId = "corr-1")

        val outcome = composer.compose(originalMessage, ReasoningProviderResponse.Reply("hello, owner"))

        val produced = assertIs<GatedOutcome.Produced<OutboundParkerResponse>>(outcome)
        assertEquals("hello, owner", produced.value.text)
        assertEquals(channelId, produced.value.channelId)
        assertEquals(CorrelationId("corr-1"), produced.value.correlationId)
        assertEquals(PrincipalId("system.response-composer"), produced.value.senderPrincipalId)
        assertEquals(1, identityService.resolveCallCount)
    }

    // ================= Goal path =================

    @Test
    fun `a Goal returns NotAccepted naming the variant, and never resolves identity`() = runTest {
        val identityService = registeredIdentityService()
        val composer = ResponseComposer(identityService)

        val outcome = composer.compose(message(), ReasoningProviderResponse.Goal("book a flight"))

        val notAccepted = assertIs<GatedOutcome.NotAccepted>(outcome)
        assertTrue("Goal" in notAccepted.reason)
        assertEquals(0, identityService.resolveCallCount)
    }

    // ================= NoAction path =================

    @Test
    fun `a NoAction returns NotAccepted naming the variant, and never resolves identity`() = runTest {
        val identityService = registeredIdentityService()
        val composer = ResponseComposer(identityService)

        val outcome = composer.compose(message(), ReasoningProviderResponse.NoAction)

        val notAccepted = assertIs<GatedOutcome.NotAccepted>(outcome)
        assertTrue("NoAction" in notAccepted.reason)
        assertEquals(0, identityService.resolveCallCount)
    }

    // ================= Goal/NoAction never throw, even when unregistered (Section 4c) =================

    @Test
    fun `a Goal returns NotAccepted normally even when the operating identity is unregistered`() = runTest {
        val identityService = unregisteredIdentityService()
        val composer = ResponseComposer(identityService)

        val outcome = composer.compose(message(), ReasoningProviderResponse.Goal("book a flight"))

        assertIs<GatedOutcome.NotAccepted>(outcome)
        assertEquals(0, identityService.resolveCallCount)
    }

    @Test
    fun `a NoAction returns NotAccepted normally even when the operating identity is unregistered`() = runTest {
        val identityService = unregisteredIdentityService()
        val composer = ResponseComposer(identityService)

        val outcome = composer.compose(message(), ReasoningProviderResponse.NoAction)

        assertIs<GatedOutcome.NotAccepted>(outcome)
        assertEquals(0, identityService.resolveCallCount)
    }

    // ================= Field pass-through / non-mutation (Section 4b) =================

    @Test
    fun `channelId and correlationId on the composed response exactly equal the original message's fields`() = runTest {
        val identityService = registeredIdentityService()
        val composer = ResponseComposer(identityService)
        val originalMessage = message(channel = ModuleId("channel.some-other"), correlationId = "corr-distinct")

        val outcome = composer.compose(originalMessage, ReasoningProviderResponse.Reply("reply text"))

        val produced = assertIs<GatedOutcome.Produced<OutboundParkerResponse>>(outcome)
        assertEquals(originalMessage.channelId, produced.value.channelId)
        assertEquals(originalMessage.correlationId, produced.value.correlationId)
    }

    // ================= Unregistered operating identity -- Reply branch only =================

    @Test
    fun `an unregistered operating identity causes Reply to throw IllegalStateException`() = runTest {
        val identityService = unregisteredIdentityService()
        val composer = ResponseComposer(identityService)

        assertFailsWith<IllegalStateException> {
            composer.compose(message(), ReasoningProviderResponse.Reply("hello, owner"))
        }
    }

    // ================= Statelessness (Section 4a) =================

    @Test
    fun `the composer declares no instance field beyond its one constructor-injected dependency`() {
        // Measures the architectural property -- instance state -- rather
        // than JVM code generation: this filters to non-static fields
        // only. The compiler-generated field backing the private,
        // class-scoped RESPONSE_COMPOSER_PRINCIPAL_ID companion constant
        // is always static (it is a single, class-wide value, not
        // per-instance state), regardless of what name a given Kotlin
        // compiler version happens to generate for it -- so filtering by
        // staticness is robust where filtering by a specific generated
        // name is not. `identityService` is the one non-static,
        // constructor-injected instance field this class carries per
        // object; nothing else does, which is exactly what "the composer
        // holds exactly its one constructor-injected dependency as its
        // only field" (Scope Lock Section 6/7) means architecturally.
        val instanceFieldNames = ResponseComposer::class.java.declaredFields
            .filterNot { Modifier.isStatic(it.modifiers) }
            .map { it.name }
            .toSet()

        assertEquals(setOf("identityService"), instanceFieldNames)
    }

    // ================= Structural composition-only test (Section 4d) =================

    @Test
    fun `the composer's constructor accepts exactly one dependency -- IdentityService`() {
        val constructor = ResponseComposer::class.java.declaredConstructors.single()
        val parameterTypes = constructor.parameterTypes.map { it.simpleName }.toSet()

        assertEquals(setOf("IdentityService"), parameterTypes)
    }

    // ================= Exactly-once identity resolution, corrected per branch =================

    @Test
    fun `resolveCallCount only increments on the Reply branch, across sequential calls of different variants`() = runTest {
        val identityService = registeredIdentityService()
        val composer = ResponseComposer(identityService)

        composer.compose(message(), ReasoningProviderResponse.Reply("first reply"))
        assertEquals(1, identityService.resolveCallCount)

        composer.compose(message(), ReasoningProviderResponse.Goal("some goal"))
        assertEquals(1, identityService.resolveCallCount)

        composer.compose(message(), ReasoningProviderResponse.NoAction)
        assertEquals(1, identityService.resolveCallCount)

        composer.compose(message(), ReasoningProviderResponse.Reply("second reply"))
        assertEquals(2, identityService.resolveCallCount)
    }

    // ================= Compatibility test: real ResponseDelivery stack =================

    @Test
    fun `compatibility -- a composed response is accepted unchanged by the real ResponseDelivery stack, without ResponseComposer calling it`() = runTest {
        val resources = InMemoryResourceRegistry()
        val tools = InMemoryToolRegistry(resources)
        val moduleRegistry = InMemoryModuleRegistry(tools, resources)
        val toolInvocationBinding = InMemoryToolInvocationBinding()

        val delivered = mutableListOf<String>()
        val tool = LocalTextChannelDeliverTool { text -> delivered.add(text) }

        val moduleDescriptor = ModuleDescriptor(
            moduleId = channelId,
            name = "Local Text Channel",
            version = "0.1.0",
            toolsExposed = listOf(tool.descriptor),
            requiredPermissions = emptyList(),
            connectivityDeclaration = ModuleConnectivityDeclaration.LOCAL_ONLY,
        )
        moduleRegistry.register(moduleDescriptor)
        moduleRegistry.enable(channelId, PrincipalId("system.parker"))
        toolInvocationBinding.bind(tool.descriptor, tool)

        val ownedResources = resources.listByOwner(PrincipalId(channelId.value))
        assertEquals(1, ownedResources.size)
        assertEquals(ResourceType.TOOL, ownedResources.single().resourceType)

        val vocabulary = InMemoryActionVocabulary()
        vocabulary.register(
            ActionVocabularyEntry(
                verbPhrase = "notify owner",
                mappings = setOf(ActionResourceMapping(PermissionAction.NOTIFY, ResourceType.TOOL)),
            ),
        )
        val actionMapper = ActionMapper(vocabulary)

        val eventBus = InMemoryEventBus()
        val permissionEngine = FakePermissionEngine { request ->
            PermissionDecision(
                decisionId = DecisionId("dec-response-composer-compat-1"),
                principalId = request.principalId,
                resourceId = request.targetResources.single(),
                action = PermissionAction.NOTIFY,
                decision = PermissionDecisionOutcome.APPROVED,
                level = PermissionLevel.AUTOMATIC,
                timestamp = fixedTimestamp,
            )
        }

        val pipeline = DefaultExecutionPipeline(resources, actionMapper, permissionEngine, tools, eventBus, toolInvocationBinding)
        val delivery = ResponseDelivery(resources, pipeline)

        // A real InMemoryIdentityService is used here, not FakeIdentityService --
        // this test's own purpose is to exercise real production wiring
        // end-to-end (Scope Lock Section 11, item 10), mirroring the
        // Plan's own Section 7 compatibility-test disclosure.
        val identityService = InMemoryIdentityService()
        identityService.register(responseComposerPrincipal())
        val composer = ResponseComposer(identityService)

        val originalMessage = message(correlationId = "corr-compat-1")
        val composeOutcome = composer.compose(originalMessage, ReasoningProviderResponse.Reply("hello, owner"))
        val composed = assertIs<GatedOutcome.Produced<OutboundParkerResponse>>(composeOutcome).value

        val deliverOutcome = delivery.deliver(composed)

        val produced = assertIs<GatedOutcome.Produced<ExecutionResult>>(deliverOutcome)
        assertEquals(ExecutionResultStatus.SUCCESS, produced.value.status)
        assertEquals(listOf("hello, owner"), delivered)
    }
}
