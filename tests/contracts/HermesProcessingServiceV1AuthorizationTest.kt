package parker.core.interfaces

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class HermesProcessingServiceV1AuthorizationTest {
    private val policy = HermesV1CapabilityPolicy.initial()
    private val authorizer = HermesV1CapabilityAuthorizer(policy)
    private val unknownPrincipal = HermesV1ProcessingPrincipal("unknown-processing-principal")

    @Test
    fun `known principal authorizes enabled native and structured methods`() {
        assertIs<HermesV1AuthorizationResult.Authorized>(
            authorizer.authorize(HermesV1ProcessingPrincipal.PARKER_PROCESSING, HermesV1ProcessingMethod.DIRECT_TEXT_EXTRACTION),
        )
        assertIs<HermesV1AuthorizationResult.Authorized>(
            authorizer.authorize(HermesV1ProcessingPrincipal.PARKER_PROCESSING, HermesV1ProcessingMethod.STRUCTURED_DOCUMENT_EXTRACTION),
        )
    }

    @Test
    fun `unknown principal denies with stable non-retryable authorization failure`() {
        val denied = assertIs<HermesV1AuthorizationResult.Denied>(
            authorizer.authorize(unknownPrincipal, HermesV1ProcessingMethod.DIRECT_TEXT_EXTRACTION),
        )
        assertEquals(HermesV1FailureCategory.AUTHORIZATION, denied.failure.category)
        assertEquals(HermesV1FailureDetailCode.UNKNOWN_PRINCIPAL, denied.failure.detailCode)
        assertEquals(false, denied.failure.retryable)
    }

    @Test
    fun `known principal cannot use a capability absent from its allowlist`() {
        val restricted = HermesV1CapabilityPolicy.fromTypedGrants(
            mapOf(HermesV1ProcessingPrincipal.PARKER_PROCESSING to setOf(HermesV1ProcessingCapability.NATIVE_TEXT_EXTRACTION)),
        )
        val denied = assertIs<HermesV1AuthorizationResult.Denied>(
            HermesV1CapabilityAuthorizer(restricted).authorize(
                HermesV1ProcessingPrincipal.PARKER_PROCESSING,
                HermesV1ProcessingMethod.STRUCTURED_DOCUMENT_EXTRACTION,
            ),
        )
        assertEquals(HermesV1FailureDetailCode.CAPABILITY_NOT_ALLOWED, denied.failure.detailCode)
    }

    @Test
    fun `missing mapping denies and unknown registry values cannot bypass authorization`() {
        val withoutMapping = HermesV1CapabilityAuthorizer.forTesting(
            policy,
            HermesV1MethodCapabilityRegistry.requiredCapabilities - HermesV1ProcessingMethod.DIRECT_TEXT_EXTRACTION,
        )
        val denied = assertIs<HermesV1AuthorizationResult.Denied>(
            withoutMapping.authorize(HermesV1ProcessingPrincipal.PARKER_PROCESSING, HermesV1ProcessingMethod.DIRECT_TEXT_EXTRACTION),
        )
        assertEquals(HermesV1FailureDetailCode.METHOD_NOT_AUTHORIZED, denied.failure.detailCode)
        assertFailsWith<IllegalArgumentException> { HermesV1ProcessingMethod.fromWireValue("OCR") }
        assertEquals(HermesV1ProcessingCapability.OCR, HermesV1ProcessingCapability.fromWireValue("OCR"))
        assertEquals(false, HermesV1CapabilityPolicy.initial().capabilitiesFor(HermesV1ProcessingPrincipal.PARKER_PROCESSING).contains(HermesV1ProcessingCapability.OCR))
    }

    @Test
    fun `empty and malformed policies deny all`() {
        val emptyDenied = assertIs<HermesV1AuthorizationResult.Denied>(
            HermesV1CapabilityAuthorizer(HermesV1CapabilityPolicy.empty()).authorize(
                HermesV1ProcessingPrincipal.PARKER_PROCESSING,
                HermesV1ProcessingMethod.DIRECT_TEXT_EXTRACTION,
            ),
        )
        assertEquals(HermesV1FailureDetailCode.POLICY_EMPTY, emptyDenied.failure.detailCode)
        val malformed = HermesV1CapabilityPolicy.fromWireGrants(mapOf("parker" to listOf("NOT_A_CAPABILITY")))
        val malformedDenied = assertIs<HermesV1AuthorizationResult.Denied>(
            HermesV1CapabilityAuthorizer(malformed).authorize(unknownPrincipal, HermesV1ProcessingMethod.DIRECT_TEXT_EXTRACTION),
        )
        assertEquals(HermesV1FailureDetailCode.POLICY_MALFORMED, malformedDenied.failure.detailCode)
    }

    @Test
    fun `request metadata cannot manufacture authority`() {
        val request = HermesProcessingServiceV1Request(
            HermesV1ProtocolVersion.CURRENT,
            HermesV1RequestId("request-1"),
            HermesV1JobId("job-1"),
            HermesV1OccurrenceId("occurrence-1"),
            HermesV1BatchId("batch-1"),
            HermesProcessingServiceV1Source(
                HermesV1SourceReference("source-1"),
                HermesV1Sha256("a".repeat(64)),
                HermesV1SourceSize(0),
                HermesV1OriginalFilename("file.txt"),
                HermesV1MediaType("text/plain"),
            ),
            listOf(HermesV1ProcessingMethod.DIRECT_TEXT_EXTRACTION),
        )
        assertIs<HermesV1AuthorizationResult.Denied>(authorizer.authorize(unknownPrincipal, request))
        assertIs<HermesV1AuthorizationResult.Authorized>(
            authorizer.authorize(HermesV1ProcessingPrincipal.PARKER_PROCESSING, request),
        )
    }

    @Test
    fun `future media filename and correlation values do not alter authorization`() {
        val base = HermesV1ProcessingServiceV1TestFixtures.requestWithMethod(HermesV1ProcessingMethod.DIRECT_TEXT_EXTRACTION)
        val changed = base.copy(
            requestId = HermesV1RequestId("request-attacker"),
            jobId = HermesV1JobId("job-attacker"),
            occurrenceId = HermesV1OccurrenceId("occurrence-attacker"),
            batchId = HermesV1BatchId("batch-attacker"),
            source = base.source.copy(
                reference = HermesV1SourceReference("reference-attacker"),
                originalFilename = HermesV1OriginalFilename("attacker.csv"),
                mediaType = HermesV1MediaType("application/octet-stream"),
            ),
        )
        assertIs<HermesV1AuthorizationResult.Authorized>(
            authorizer.authorize(HermesV1ProcessingPrincipal.PARKER_PROCESSING, changed),
        )
        val restricted = HermesV1CapabilityAuthorizer(
            HermesV1CapabilityPolicy.fromTypedGrants(
                mapOf(HermesV1ProcessingPrincipal.PARKER_PROCESSING to setOf(HermesV1ProcessingCapability.NATIVE_TEXT_EXTRACTION)),
            ),
        )
        val attemptedEscalation = changed.copy(
            requestedMethods = listOf(HermesV1ProcessingMethod.STRUCTURED_DOCUMENT_EXTRACTION),
        )
        val denied = assertIs<HermesV1AuthorizationResult.Denied>(
            restricted.authorize(HermesV1ProcessingPrincipal.PARKER_PROCESSING, attemptedEscalation),
        )
        assertEquals(HermesV1FailureDetailCode.CAPABILITY_NOT_ALLOWED, denied.failure.detailCode)
    }

    @Test
    fun `disabled OCR and transcription capabilities cannot be authorized`() {
        val ocrDenied = assertIs<HermesV1AuthorizationResult.Denied>(
            HermesV1CapabilityAuthorizer.forTesting(
                policy,
                mapOf(HermesV1ProcessingMethod.DIRECT_TEXT_EXTRACTION to HermesV1ProcessingCapability.OCR),
            ).authorize(HermesV1ProcessingPrincipal.PARKER_PROCESSING, HermesV1ProcessingMethod.DIRECT_TEXT_EXTRACTION),
        )
        assertEquals(HermesV1FailureDetailCode.DISABLED_CAPABILITY, ocrDenied.failure.detailCode)
        val transcriptionDenied = assertIs<HermesV1AuthorizationResult.Denied>(
            HermesV1CapabilityAuthorizer.forTesting(
                policy,
                mapOf(HermesV1ProcessingMethod.DIRECT_TEXT_EXTRACTION to HermesV1ProcessingCapability.TRANSCRIPTION),
            ).authorize(HermesV1ProcessingPrincipal.PARKER_PROCESSING, HermesV1ProcessingMethod.DIRECT_TEXT_EXTRACTION),
        )
        assertEquals(HermesV1FailureDetailCode.DISABLED_CAPABILITY, transcriptionDenied.failure.detailCode)
    }

    @Test
    fun `principal policy and request contain no key material or case identity`() {
        assertEquals(false, HermesProcessingServiceV1Request::class.members.any { it.name == "caseId" })
        assertEquals(false, HermesProcessingServiceV1Request::class.members.any { it.name.contains("key", ignoreCase = true) })
        assertEquals(false, HermesV1ProcessingPrincipal::class.members.any { it.name.contains("key", ignoreCase = true) })
    }

    @Test
    fun `caller-owned policy collections cannot mutate Hermes policy`() {
        val capabilities = mutableSetOf(HermesV1ProcessingCapability.NATIVE_TEXT_EXTRACTION)
        val grants = mutableMapOf(HermesV1ProcessingPrincipal.PARKER_PROCESSING to capabilities)
        val immutablePolicy = HermesV1CapabilityPolicy.fromTypedGrants(grants)
        capabilities.clear()
        grants.clear()
        assertEquals(
            setOf(HermesV1ProcessingCapability.NATIVE_TEXT_EXTRACTION),
            immutablePolicy.capabilitiesFor(HermesV1ProcessingPrincipal.PARKER_PROCESSING),
        )
    }
}

private object HermesV1ProcessingServiceV1TestFixtures {
    fun requestWithMethod(method: HermesV1ProcessingMethod) = HermesProcessingServiceV1Request(
        HermesV1ProtocolVersion.CURRENT,
        HermesV1RequestId("request-1"),
        HermesV1JobId("job-1"),
        HermesV1OccurrenceId("occurrence-1"),
        HermesV1BatchId("batch-1"),
        HermesProcessingServiceV1Source(
            HermesV1SourceReference("source-1"),
            HermesV1Sha256("a".repeat(64)),
            HermesV1SourceSize(0),
            HermesV1OriginalFilename("file.txt"),
            HermesV1MediaType("text/plain"),
        ),
        listOf(method),
    )
}
