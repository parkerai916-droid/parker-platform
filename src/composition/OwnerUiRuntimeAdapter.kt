package parker.composition

import java.time.Instant
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import parker.core.interfaces.CorrelationId
import parker.core.interfaces.InboundOwnerMessage
import parker.core.interfaces.ModuleId
import parker.core.interfaces.PlanningSessionResult
import parker.core.interfaces.PrincipalId
import parker.core.runtime.GoalPlanningHandoffOutcome
import parker.ui.OwnerInteraction
import parker.ui.OwnerInteractionAvailability
import parker.ui.OwnerReply
import parker.ui.OwnerSubmissionDisposition

/**
 * Composition-owned notification seam for one single-flight owner UI.
 * The same instance is supplied to ParkerRuntime and [OwnerUiRuntimeAdapter].
 */
class OwnerUiNotificationBridge : OwnerNotificationSink {
    private val receiverLock = Any()
    private var activeReceiver: (suspend (OwnerReply) -> Unit)? = null

    override suspend fun notify(text: String) {
        val receiver = synchronized(receiverLock) {
            checkNotNull(activeReceiver) { "Owner UI notification arrived without an active submission" }
        }
        receiver(OwnerReply(text))
    }

    internal fun attach(receiver: suspend (OwnerReply) -> Unit) = synchronized(receiverLock) {
        check(activeReceiver == null) { "Owner UI reply receiver is already active" }
        activeReceiver = receiver
    }

    internal fun detach() = synchronized(receiverLock) {
        activeReceiver = null
    }
}

/**
 * Narrow composition adapter. It receives only ParkerRuntime's owner-message
 * method capability, never ParkerRuntime itself or any other runtime method.
 */
class OwnerUiRuntimeAdapter(
    private val ownerPrincipalId: PrincipalId,
    private val localTextChannelModuleId: ModuleId,
    private val submitOwnerMessage: suspend (InboundOwnerMessage) -> ParkerRuntimeOutcome,
    private val notificationBridge: OwnerUiNotificationBridge,
    private val clock: () -> Instant = Instant::now,
    private val correlationIdSource: () -> CorrelationId = { CorrelationId(UUID.randomUUID().toString()) },
) : OwnerInteraction {
    private val active = AtomicBoolean(false)

    @Volatile
    private var currentAvailability = OwnerInteractionAvailability.AVAILABLE

    override val availability: OwnerInteractionAvailability
        get() = currentAvailability

    override suspend fun submit(
        ownerText: String,
        onReply: suspend (OwnerReply) -> Unit,
    ): OwnerSubmissionDisposition {
        require(ownerText.isNotBlank()) { "ownerText must not be blank" }
        if (currentAvailability == OwnerInteractionAvailability.STOPPED) {
            return OwnerSubmissionDisposition.Unavailable("Parker Runtime is not accepting messages")
        }
        check(active.compareAndSet(false, true)) { "Only one owner UI runtime submission may be active" }

        notificationBridge.attach(onReply)
        return try {
            mapOutcome(
                submitOwnerMessage(
                    InboundOwnerMessage(
                        channelId = localTextChannelModuleId,
                        senderPrincipalId = ownerPrincipalId,
                        text = ownerText,
                        timestamp = clock(),
                        correlationId = correlationIdSource(),
                    ),
                ),
            )
        } catch (notRunning: ParkerRuntimeException.NotRunning) {
            currentAvailability = OwnerInteractionAvailability.STOPPED
            OwnerSubmissionDisposition.Unavailable("Parker Runtime is not accepting messages")
        } finally {
            notificationBridge.detach()
            active.set(false)
        }
    }

    private fun mapOutcome(outcome: ParkerRuntimeOutcome): OwnerSubmissionDisposition = when (outcome) {
        is ParkerRuntimeOutcome.Delivered ->
            OwnerSubmissionDisposition.Delivered(outcome.executionResult.status.name)
        is ParkerRuntimeOutcome.NotAccepted ->
            OwnerSubmissionDisposition.NotAccepted("Parker did not accept this message.")
        is ParkerRuntimeOutcome.Failed ->
            OwnerSubmissionDisposition.Failed(
                stage = outcome.stage.name,
                safeMessage = when (outcome.stage) {
                    PipelineStage.REASONING -> "Parker could not complete reasoning for this message."
                    else -> "Parker could not complete this message."
                },
            )
        is ParkerRuntimeOutcome.Planned ->
            OwnerSubmissionDisposition.Planned(planningResultCategory(outcome.outcome))
    }

    private fun planningResultCategory(outcome: GoalPlanningHandoffOutcome): String = when (outcome) {
        is GoalPlanningHandoffOutcome.Planned -> when (outcome.planningSessionResult) {
            is PlanningSessionResult.Completed -> "Completed"
            is PlanningSessionResult.Rejected -> "Rejected"
            is PlanningSessionResult.Failed -> "Failed"
        }
    }
}
