package parker.ui

import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.delay

/** Deterministic scripts supported by the offline owner interaction harness. */
sealed interface OfflineOwnerScenario {
    val delayMillis: Long

    data class ReplyDelivered(
        val replyText: String,
        val executionStatus: String = "SUCCESS",
        override val delayMillis: Long = 0,
    ) : OfflineOwnerScenario

    data class Delivered(
        val executionStatus: String = "SUCCESS",
        override val delayMillis: Long = 0,
    ) : OfflineOwnerScenario

    data class NotAccepted(
        val reason: String,
        override val delayMillis: Long = 0,
    ) : OfflineOwnerScenario

    data class Failed(
        val stage: String,
        val safeMessage: String,
        override val delayMillis: Long = 0,
    ) : OfflineOwnerScenario

    data class Planned(
        val resultCategory: String,
        override val delayMillis: Long = 0,
    ) : OfflineOwnerScenario

    data class Unavailable(
        val reason: String,
        override val delayMillis: Long = 0,
    ) : OfflineOwnerScenario
}

/**
 * Scripted, deterministic development implementation. Its only side effects
 * are recording submitted text, invoking the supplied reply receiver, and
 * suspending through the injected delay function.
 */
class OfflineOwnerInteraction(
    scenarios: List<OfflineOwnerScenario>,
    private val delayFor: suspend (Long) -> Unit = { delay(it) },
    initiallyAvailable: Boolean = true,
) : OwnerInteraction {
    private val scripts = ArrayDeque(scenarios)
    private val active = AtomicBoolean(false)
    private val recordedSubmissions = CopyOnWriteArrayList<String>()

    @Volatile
    private var currentAvailability = if (initiallyAvailable) {
        OwnerInteractionAvailability.AVAILABLE
    } else {
        OwnerInteractionAvailability.STOPPED
    }

    override val availability: OwnerInteractionAvailability
        get() = currentAvailability

    val submissions: List<String>
        get() = recordedSubmissions.toList()

    override suspend fun submit(
        ownerText: String,
        onReply: suspend (OwnerReply) -> Unit,
    ): OwnerSubmissionDisposition {
        require(ownerText.isNotBlank()) { "ownerText must not be blank" }
        check(active.compareAndSet(false, true)) { "Only one offline owner submission may be active" }
        try {
            recordedSubmissions.add(ownerText)
            if (currentAvailability == OwnerInteractionAvailability.STOPPED) {
                return OwnerSubmissionDisposition.Unavailable("Offline owner interaction is stopped")
            }

            val scenario = synchronized(scripts) { scripts.removeFirstOrNull() }
                ?: return OwnerSubmissionDisposition.Failed("OFFLINE", "No scripted scenario available")
            if (scenario.delayMillis > 0) delayFor(scenario.delayMillis)

            return when (scenario) {
                is OfflineOwnerScenario.ReplyDelivered -> {
                    onReply(OwnerReply(scenario.replyText))
                    OwnerSubmissionDisposition.Delivered(scenario.executionStatus)
                }
                is OfflineOwnerScenario.Delivered ->
                    OwnerSubmissionDisposition.Delivered(scenario.executionStatus)
                is OfflineOwnerScenario.NotAccepted ->
                    OwnerSubmissionDisposition.NotAccepted(scenario.reason)
                is OfflineOwnerScenario.Failed ->
                    OwnerSubmissionDisposition.Failed(scenario.stage, scenario.safeMessage)
                is OfflineOwnerScenario.Planned ->
                    OwnerSubmissionDisposition.Planned(scenario.resultCategory)
                is OfflineOwnerScenario.Unavailable -> {
                    currentAvailability = OwnerInteractionAvailability.STOPPED
                    OwnerSubmissionDisposition.Unavailable(scenario.reason)
                }
            }
        } finally {
            active.set(false)
        }
    }

    fun stop() {
        currentAvailability = OwnerInteractionAvailability.STOPPED
    }
}
