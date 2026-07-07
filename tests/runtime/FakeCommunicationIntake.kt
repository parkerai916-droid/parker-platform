package parker.core.runtime

import parker.core.interfaces.CommunicationIntake
import parker.core.interfaces.CommunicationIntakeDisposition
import parker.core.interfaces.InboundOwnerMessage

/**
 * Test-only fake, mirroring [FakePermissionEngine]/[FakeMemoryPromotionPolicy]'s
 * lambda-based fake precedent. Exists so [DefaultLocalTextChannelTest]
 * can prove [DefaultLocalTextChannel]'s own *orchestration* (does it
 * consult [CommunicationIntake] internally, exactly once per call, with
 * a correctly constructed [InboundOwnerMessage], and return whatever
 * disposition comes back unchanged) independently of
 * [InMemoryCommunicationIntake]'s own channel-status/sender-resolution
 * logic, which [InMemoryCommunicationIntakeTest] already covers on its
 * own.
 */
class FakeCommunicationIntake(
    private val dispositionFor: (InboundOwnerMessage) -> CommunicationIntakeDisposition,
) : CommunicationIntake {

    var submitInboundMessageCallCount: Int = 0
        private set

    var lastSubmittedMessage: InboundOwnerMessage? = null
        private set

    override suspend fun submitInboundMessage(message: InboundOwnerMessage): CommunicationIntakeDisposition {
        submitInboundMessageCallCount++
        lastSubmittedMessage = message
        return dispositionFor(message)
    }
}
