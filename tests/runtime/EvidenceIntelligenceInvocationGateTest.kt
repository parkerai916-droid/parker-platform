package parker.core.runtime

import parker.core.interfaces.PrincipalId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Evidence Intelligence, Implementation Unit 6 ("Invocation Permission
 * Gating"). Behavioural and structural tests of
 * [EvidenceIntelligenceInvocationGate] -- see that object's own header
 * KDoc for the full governance rationale. This suite proves exactly what
 * this Unit is responsible for: the disclosed resourceId/action-name
 * convention, the [ExecutionRequest] this convention builds, and the
 * structural absence of any `PermissionEngine`/`EvidenceIntelligence`
 * dependency -- it does not, and cannot, prove anything about evaluation,
 * denial handling, or acceptance, since none of those exist in this file
 * (Unit 8's own, later responsibility).
 */
class EvidenceIntelligenceInvocationGateTest {

    private val principalId = PrincipalId("principal-1")

    // --- disclosed convention ---

    @Test
    fun `the disclosed resource identifier and action name are non-blank and distinct from every existing convention`() {
        assertTrue(EvidenceIntelligenceInvocationGate.EVIDENCE_INTELLIGENCE_INVOCATION_RESOURCE_ID.value.isNotBlank())
        assertTrue(EvidenceIntelligenceInvocationGate.ANALYSE_ACTION_NAME.isNotBlank())

        // Distinct from every other disclosed-but-unregistered convention already in this
        // repository, so registering this one at a future Unit 8 cannot silently collide with
        // an existing ActionVocabulary/ResourceRegistry entry.
        val existingResourceIds = setOf(
            DefaultEvidenceCustodian.EVIDENCE_INTAKE_RESOURCE_ID.value,
            DefaultEvidenceCustodian.EVIDENCE_RETRIEVAL_RESOURCE_ID.value,
            DefaultOwnerEvidenceDeletionAuthority.EVIDENCE_DELETION_RESOURCE_ID.value,
            EvidenceRegistrationCoordinator.MEMORY_CORE_PROVENANCE_RESOURCE_ID.value,
            EvidenceRegistrationCoordinator.MEMORY_CORE_DOCUMENT_REGISTRATION_RESOURCE_ID.value,
            // ParkerRuntime's own three composition-root resource/verb-phrase conventions
            // (AGENT_RUNTIME_BOUNDARY_RESOURCE_ID, NOTIFY_OWNER_VERB_PHRASE,
            // AGENT_RUN_START_VERB_PHRASE) live in a `private companion object` there,
            // deliberately not exposed to avoid "a second literal... via loosened visibility"
            // (ParkerRuntime's own KDoc) -- duplicated here as literal copies instead, mirroring
            // the same composition-root-owns-its-own-copy convention this repository already
            // applies to PLANNER_RUNTIME_PRINCIPAL_ID/TASK_MANAGER_RUNTIME_PRINCIPAL_ID, so this
            // check is genuinely exhaustive rather than merely a subset.
            "resource-agent-runtime-boundary",
        )
        assertTrue(
            EvidenceIntelligenceInvocationGate.EVIDENCE_INTELLIGENCE_INVOCATION_RESOURCE_ID.value !in existingResourceIds,
        )

        val existingActionNames = setOf(
            DefaultEvidenceCustodian.ACCEPT_ACTION_NAME,
            DefaultEvidenceCustodian.RETRIEVE_ACTION_NAME,
            DefaultOwnerEvidenceDeletionAuthority.DELETE_ACTION_NAME,
            EvidenceRegistrationCoordinator.CREATE_PROVENANCE_ACTION_NAME,
            EvidenceRegistrationCoordinator.REGISTER_DOCUMENT_ACTION_NAME,
            // ParkerRuntime's own private verb phrases -- see the comment above.
            "notify owner",
            "start agent run",
        )
        assertTrue(EvidenceIntelligenceInvocationGate.ANALYSE_ACTION_NAME !in existingActionNames)
    }

    // --- the ExecutionRequest this convention builds ---

    @Test
    fun `buildExecutionRequest names exactly the disclosed resource and action, for the supplied principal`() {
        val request = EvidenceIntelligenceInvocationGate.buildExecutionRequest(principalId)

        assertEquals(principalId, request.principalId)
        assertEquals(
            listOf(EvidenceIntelligenceInvocationGate.EVIDENCE_INTELLIGENCE_INVOCATION_RESOURCE_ID),
            request.targetResources,
        )
        assertEquals(listOf(EvidenceIntelligenceInvocationGate.ANALYSE_ACTION_NAME), request.proposedActions)
        assertTrue(request.intent.isNotBlank())
        assertTrue(request.correlationId.isNotBlank())
    }

    @Test
    fun `two calls mint distinct requestId and correlationId`() {
        val first = EvidenceIntelligenceInvocationGate.buildExecutionRequest(principalId)
        val second = EvidenceIntelligenceInvocationGate.buildExecutionRequest(principalId)

        assertNotEquals(first.requestId, second.requestId)
        assertNotEquals(first.correlationId, second.correlationId)
    }

    // --- structural: no PermissionEngine/EvidenceIntelligence reachability of this Unit's own ---

    @Test
    fun `EvidenceIntelligenceInvocationGate declares no constructor -- an object, not a class`() {
        val constructors = EvidenceIntelligenceInvocationGate::class.java.declaredConstructors

        // Kotlin `object` still compiles to exactly one private, no-argument constructor.
        assertEquals(1, constructors.size)
        assertEquals(0, constructors.single().parameterCount)
    }

    @Test
    fun `EvidenceIntelligenceInvocationGate implements no interface`() {
        val implementedInterfaces = EvidenceIntelligenceInvocationGate::class.java.interfaces

        assertEquals(emptyList(), implementedInterfaces.toList())
    }

    @Test
    fun `EvidenceIntelligenceInvocationGate holds no field of type PermissionEngine or EvidenceIntelligence`() {
        val fieldTypeNames = EvidenceIntelligenceInvocationGate::class.java.declaredFields
            .map { it.type.simpleName }
            .toSet()

        assertTrue("PermissionEngine" !in fieldTypeNames)
        assertTrue("EvidenceIntelligence" !in fieldTypeNames)
        assertTrue("DefaultEvidenceIntelligence" !in fieldTypeNames)
    }
}
