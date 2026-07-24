package parker.composition

import parker.core.interfaces.CommunicationIntake
import parker.core.interfaces.CommunicationIntakeDisposition
import parker.core.interfaces.InboundOwnerMessage

/**
 * Sprint 10, Unit 4 (Production Composition Root). A transparent,
 * delegating [CommunicationIntake] decorator that logs "Conversation
 * accepted" / "Conversation rejected" at the one seam this composition
 * root is structurally able to observe it from without modifying any
 * frozen production file: [CommunicationIntake] is an interface, so
 * wrapping it via delegation adds an observability seam without altering
 * `InMemoryCommunicationIntake`'s own behaviour in any way -- the exact
 * technique `tests/runtime/VerticalSliceEndToEndTest.kt`'s own
 * `RecordingExecutionPipeline` already establishes as an accepted pattern
 * in this repository for the identical reason (observing a real
 * component's real inputs/outputs without modifying it).
 *
 * Logs [InboundOwnerMessage.correlationId] and [InboundOwnerMessage.channelId]
 * only -- never [InboundOwnerMessage.text] (see `ParkerLogger`'s own
 * logging-discipline KDoc).
 */
class LoggingCommunicationIntake(
    private val delegate: CommunicationIntake,
    private val logger: ParkerLogger,
) : CommunicationIntake {

    override suspend fun submitInboundMessage(message: InboundOwnerMessage): CommunicationIntakeDisposition {
        val disposition = delegate.submitInboundMessage(message)
        when (disposition) {
            is CommunicationIntakeDisposition.Accepted -> logger.info(
                "Conversation accepted (correlationId=${message.correlationId.value}, channelId=${message.channelId.value})",
            )
            is CommunicationIntakeDisposition.Rejected -> logger.warn(
                "Conversation rejected (correlationId=${message.correlationId.value}, " +
                    "channelId=${message.channelId.value}, reason=${disposition.reason})",
            )
        }
        return disposition
    }
}
