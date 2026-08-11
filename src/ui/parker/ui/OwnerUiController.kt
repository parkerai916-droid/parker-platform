package parker.ui

import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Plain-Kotlin presentation controller for the first owner UI. It owns no
 * runtime authority and knows only [OwnerInteraction].
 */
class OwnerUiController(
    private val interaction: OwnerInteraction,
    coroutineContext: CoroutineContext = Dispatchers.Default,
) {
    private val controllerJob = SupervisorJob()
    private val scope = CoroutineScope(coroutineContext.minusKey(Job) + controllerJob)
    private val stateLock = Any()
    private val mutableState = MutableStateFlow(
        OwnerUiState(
            status = if (interaction.availability == OwnerInteractionAvailability.AVAILABLE) {
                OwnerUiStatus.READY
            } else {
                OwnerUiStatus.STOPPED
            },
        ),
    )
    private var activeJob: Job? = null

    val state: StateFlow<OwnerUiState> = mutableState.asStateFlow()

    /** Starts one submission without blocking the presentation thread. */
    fun submit(ownerText: String): OwnerSubmissionAttempt = synchronized(stateLock) {
        if (ownerText.isBlank()) return OwnerSubmissionAttempt.BLANK_REJECTED
        if (mutableState.value.submissionActive) return OwnerSubmissionAttempt.ALREADY_PROCESSING
        if (interaction.availability != OwnerInteractionAvailability.AVAILABLE || !controllerJob.isActive) {
            mutableState.update { it.copy(status = OwnerUiStatus.STOPPED, submissionActive = false) }
            return OwnerSubmissionAttempt.UNAVAILABLE
        }

        mutableState.update {
            it.copy(
                transcript = it.transcript + OwnerTranscriptEntry.Owner(ownerText),
                status = OwnerUiStatus.PROCESSING,
                submissionActive = true,
            )
        }

        activeJob = scope.launch {
            try {
                val disposition = interaction.submit(ownerText, ::receiveReply)
                applyDisposition(disposition)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (failure: Exception) {
                addSystemStatus("Owner interaction failed unexpectedly")
                mutableState.update { it.copy(status = OwnerUiStatus.ERROR) }
            } finally {
                synchronized(stateLock) {
                    mutableState.update {
                        it.copy(
                            status = if (it.status == OwnerUiStatus.PROCESSING) OwnerUiStatus.READY else it.status,
                            submissionActive = false,
                        )
                    }
                    activeJob = null
                }
            }
        }
        OwnerSubmissionAttempt.STARTED
    }

    /** Cancels owned work and leaves presentation state structurally stopped. */
    fun shutdown(reason: String = "Owner interaction stopped") {
        synchronized(stateLock) {
            activeJob?.cancel()
            mutableState.update {
                it.copy(
                    transcript = it.transcript + OwnerTranscriptEntry.System(reason),
                    status = OwnerUiStatus.STOPPED,
                    submissionActive = false,
                )
            }
            scope.cancel()
        }
    }

    private suspend fun receiveReply(reply: OwnerReply) {
        mutableState.update {
            it.copy(transcript = it.transcript + OwnerTranscriptEntry.Parker(reply.text))
        }
    }

    private fun applyDisposition(disposition: OwnerSubmissionDisposition) {
        when (disposition) {
            is OwnerSubmissionDisposition.Delivered -> Unit
            is OwnerSubmissionDisposition.NotAccepted ->
                addSystemStatus("Not accepted: ${disposition.reason}")
            is OwnerSubmissionDisposition.Failed -> {
                addSystemStatus("Failed (${disposition.stage}): ${disposition.safeMessage}")
                mutableState.update { it.copy(status = OwnerUiStatus.ERROR) }
            }
            is OwnerSubmissionDisposition.Planned ->
                addSystemStatus("Planned: ${disposition.resultCategory}")
            is OwnerSubmissionDisposition.Unavailable -> {
                addSystemStatus("Stopped: ${disposition.reason}")
                mutableState.update { it.copy(status = OwnerUiStatus.STOPPED) }
            }
        }
    }

    private fun addSystemStatus(text: String) {
        mutableState.update {
            it.copy(transcript = it.transcript + OwnerTranscriptEntry.System(text))
        }
    }
}
