package parker.core.runtime

import java.nio.file.Files
import java.nio.file.Path
import parker.core.interfaces.*

const val REGION_PROVIDER_STATE_PRODUCTION_CONTAINER_ROOT = "/data/external-region-provider-state"
const val REGION_PROVIDER_STATE_RECOMMENDED_HOST_ROOT = "/mnt/parker-data/parker/external-region-provider-state"

data class RegionProviderStateRootConfiguration(val enabled: Boolean, val root: Path?) {
    fun validatedRoot(): Path? {
        if (!enabled) return null
        val value = requireNotNull(root) { "region execution enabled without provider-state root" }.toAbsolutePath().normalize()
        require(Files.isDirectory(value) && Files.isWritable(value)) { "invalid provider-state root" }
        return value
    }
}

enum class GovernedRegionRecoveryState {
    READY_FOR_FIRST_ATTEMPT,
    ATTEMPT_OUTCOME_UNKNOWN,
    RAW_RESPONSE_RECOVERED,
    PARSE_FAILURE_RECOVERED,
    VALIDATION_FAILURE_RECOVERED,
    VALID_PROVIDER_STATE_RECOVERED,
    DOWNSTREAM_RESUMABLE,
}

data class GovernedRegionExecutionBinding(
    val identity: FidelityFirstExecutionIdentity,
    val request: RegionTranscriptionRequest,
    /** Parker's deterministic source order, never provider-returned order. */
    val sourceRegionOrder: List<SourceRegionId>,
)

sealed interface GovernedRegionExecutionOutcome {
    val state: GovernedRegionRecoveryState
    data class FirstAttemptCompleted(
        override val state: GovernedRegionRecoveryState,
        val recovered: RecoveredRegionProviderState,
        val validated: RegionTranscriptionResult?,
        val sourceRegionOrder: List<SourceRegionId>,
    ) : GovernedRegionExecutionOutcome
    data class Recovered(
        override val state: GovernedRegionRecoveryState,
        val providerState: RecoveredRegionProviderState?,
        val validated: RegionTranscriptionResult? = null,
        val sourceRegionOrder: List<SourceRegionId> = emptyList(),
        val detail: String? = null,
    ) : GovernedRegionExecutionOutcome
    data class Blocked(override val state: GovernedRegionRecoveryState, val reason: String) : GovernedRegionExecutionOutcome
}

/**
 * Offline-composable execution boundary. The existing FA ledger owns attempt stages; the R6.5
 * store owns exact responses. No ambiguous state is retryable and no provider is called before
 * PROVIDER_ATTEMPT_STARTED is durably forced by the ledger.
 */
class GovernedRegionTranscriptionExecutionCoordinator(
    private val ledger: FileSystemFidelityFirstAttemptLedger,
    private val providerStateStore: FileSystemRegionProviderStateStore,
    private val mechanism: RegionExternalTranscriptionMechanism,
    private val validator: RegionTranscriptionValidator = RegionTranscriptionValidator(),
) {
    suspend fun execute(binding: GovernedRegionExecutionBinding): GovernedRegionExecutionOutcome {
        prepareForGuardedAttempt(binding)?.let { return it }
        durablyStartProviderAttempt(binding)?.let { return it }
        return transportAfterGuardRelease(binding)
    }

    /** Preparation phase. It is provider-free and never records attempt-start. */
    fun prepareForGuardedAttempt(binding: GovernedRegionExecutionBinding): GovernedRegionExecutionOutcome? {
        val mismatch = bindingMismatch(binding)
        if (mismatch != null) return GovernedRegionExecutionOutcome.Blocked(GovernedRegionRecoveryState.ATTEMPT_OUTCOME_UNKNOWN, mismatch)
        val request = binding.request

        providerStateStore.readFor(request)?.let { return recover(it, binding, firstAttempt = false) }

        val initial = try { ledger.open(binding.identity) }
        catch (_: Exception) { return blocked("ATTEMPT_IDENTITY_CONFLICT") }
        if (initial.providerAttemptStarted) return blocked("ATTEMPT_STARTED_WITHOUT_DURABLE_RESPONSE")

        try {
            ledger.advancePreAttempt(binding.identity, FidelityFirstAttemptStage.PREFLIGHT_PASSED)
            ledger.advancePreAttempt(binding.identity, FidelityFirstAttemptStage.SOURCE_RETRIEVED)
            ledger.advancePreAttempt(binding.identity, FidelityFirstAttemptStage.REQUEST_PREPARED)
        } catch (_: Exception) {
            providerStateStore.readFor(request)?.let { return recover(it, binding, firstAttempt = false) }
            val started = runCatching { ledger.open(binding.identity).providerAttemptStarted }.getOrDefault(false)
            return if (started) blocked("CONCURRENT_OR_PRIOR_ATTEMPT_STARTED") else blocked("ATTEMPT_MARKER_PERSISTENCE_FAILED")
        }
        return null
    }

    /** Must be called while the owner-authorization guard is held. */
    fun durablyStartProviderAttempt(binding: GovernedRegionExecutionBinding): GovernedRegionExecutionOutcome? {
        val mismatch = bindingMismatch(binding)
        if (mismatch != null) return blocked(mismatch)
        providerStateStore.readFor(binding.request)?.let { return recover(it, binding, firstAttempt = false) }
        return try {
            val state = ledger.open(binding.identity)
            if (state.providerAttemptStarted) blocked("CONCURRENT_OR_PRIOR_ATTEMPT_STARTED")
            else { ledger.transition(binding.identity, FidelityFirstAttemptStage.PROVIDER_ATTEMPT_STARTED); null }
        } catch (_: Exception) { blocked("ATTEMPT_MARKER_PERSISTENCE_FAILED") }
    }

    /** Transport occurs only after the caller has released the owner-authorization guard. */
    suspend fun transportAfterGuardRelease(binding: GovernedRegionExecutionBinding): GovernedRegionExecutionOutcome {
        val request = binding.request
        providerStateStore.readFor(request)?.let { return recover(it, binding, firstAttempt = false) }
        val started = runCatching { ledger.open(binding.identity).providerAttemptStarted }.getOrDefault(false)
        if (!started) return blocked("PROVIDER_ATTEMPT_NOT_STARTED")
        val mechanismOutcome = try { mechanism.transcribe(request) }
        catch (_: RegionProviderStateException) { return blocked("PROVIDER_STATE_PERSISTENCE_FAILED") }
        catch (_: Exception) { return blocked("TRANSPORT_OUTCOME_UNKNOWN") }
        val durable = try { providerStateStore.readFor(request) }
        catch (_: Exception) { return blocked("PROVIDER_STATE_CORRUPT") }
        if (durable == null) {
            val detail = (mechanismOutcome as? RegionExternalTranscriptionOutcome.Failure)?.code ?: "PROVIDER_STATE_PERSISTENCE_MISSING"
            return GovernedRegionExecutionOutcome.Recovered(GovernedRegionRecoveryState.ATTEMPT_OUTCOME_UNKNOWN, null, detail = detail)
        }
        try { ledger.transition(binding.identity, FidelityFirstAttemptStage.PROVIDER_RESPONSE_RECEIVED) }
        catch (_: Exception) { return blocked("PROVIDER_RESPONSE_STAGE_PERSISTENCE_FAILED") }
        return recover(durable, binding, firstAttempt = true)
    }

    private fun recover(
        state: RecoveredRegionProviderState,
        binding: GovernedRegionExecutionBinding,
        firstAttempt: Boolean,
    ): GovernedRegionExecutionOutcome {
        val requestDigest = providerStateStore.requestDigestFor(binding.request)
        if (state.requestDigest != requestDigest) return blocked("RECOVERED_REQUEST_DIGEST_MISMATCH")
        if (state.downstreamProcessingPending) return recovered(GovernedRegionRecoveryState.RAW_RESPONSE_RECOVERED, state, binding, null, firstAttempt)
        val structured = state.exactStructuredState
        val code = state.outcomeCode ?: return recovered(GovernedRegionRecoveryState.RAW_RESPONSE_RECOVERED, state, binding, null, firstAttempt)
        if (structured == null) return recovered(GovernedRegionRecoveryState.PARSE_FAILURE_RECOVERED, state, binding, null, firstAttempt, code)
        val validation = validator.validate(binding.request, structured)
        if (code != "SUCCESS" || validation is RegionTranscriptionValidationOutcome.Rejected) {
            return recovered(GovernedRegionRecoveryState.VALIDATION_FAILURE_RECOVERED, state, binding, null, firstAttempt, code)
        }
        val result = (validation as RegionTranscriptionValidationOutcome.Valid).result
        return recovered(GovernedRegionRecoveryState.DOWNSTREAM_RESUMABLE, state, binding, result, firstAttempt)
    }

    private fun recovered(
        state: GovernedRegionRecoveryState,
        provider: RecoveredRegionProviderState,
        binding: GovernedRegionExecutionBinding,
        result: RegionTranscriptionResult?,
        first: Boolean,
        detail: String? = null,
    ): GovernedRegionExecutionOutcome = if (first) {
        GovernedRegionExecutionOutcome.FirstAttemptCompleted(state, provider, result, binding.sourceRegionOrder)
    } else {
        GovernedRegionExecutionOutcome.Recovered(state, provider, result, binding.sourceRegionOrder, detail)
    }

    private fun blocked(reason: String) = GovernedRegionExecutionOutcome.Blocked(GovernedRegionRecoveryState.ATTEMPT_OUTCOME_UNKNOWN, reason)

    private fun bindingMismatch(binding: GovernedRegionExecutionBinding): String? {
        val i = binding.identity; val r = binding.request; val targets = r.targets
        if (i.requestId != providerStateStore.requestDigestFor(r)) return "REQUEST_DIGEST_MISMATCH"
        if (i.attemptId != r.correlationId) return "ATTEMPT_CORRELATION_MISMATCH"
        if (targets.any { it.sourceEvidenceArtifactId.value != i.evidenceArtifactId || it.sourceSha256 != i.sourceSha256 }) return "SOURCE_BINDING_MISMATCH"
        if (binding.sourceRegionOrder.size != targets.size || binding.sourceRegionOrder.toSet() != targets.map { it.sourceRegionId }.toSet()) return "REGION_SET_MISMATCH"
        if (i.provider != "OpenAI" || i.model != OPENAI_REGION_MODEL || i.profileId != OPENAI_REGION_PROFILE_ID ||
            i.adapterVersion != OPENAI_REGION_ADAPTER_VERSION || i.instructionSha256 != OPENAI_REGION_INSTRUCTION_SHA256 ||
            i.schemaSha256 != r.schemaSha256 || i.processingProfile != r.processingProfile
        ) return "PROVIDER_CONFIGURATION_MISMATCH"
        return null
    }
}
