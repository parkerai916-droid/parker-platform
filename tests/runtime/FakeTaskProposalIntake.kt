package parker.core.runtime

import parker.core.interfaces.TaskProposal
import parker.core.interfaces.TaskProposalDisposition
import parker.core.interfaces.TaskProposalIntake

/**
 * Lambda-based fake, mirroring `FakeAgentStepSource`/`FakePermissionEngine`'s
 * own precedent, for testing [InMemoryPlannerRuntime]'s own
 * disposition-handling branches independent of
 * [InMemoryTaskManagerRuntime]'s actual (narrower, accept-only) behaviour.
 * In particular, this lets tests exercise the `REJECTED`,
 * `Deferred`/`Split`/`Merged` -> `COMPLETED` classification paths that
 * `InMemoryTaskManagerRuntime` itself cannot currently produce (it only
 * ever returns `Accepted` or an unresolvable-owner `Rejected`).
 */
class FakeTaskProposalIntake(
    private val dispositionFor: (TaskProposal) -> TaskProposalDisposition,
) : TaskProposalIntake {

    var submitCallCount: Int = 0
        private set

    var lastProposal: TaskProposal? = null
        private set

    override suspend fun submitProposal(proposal: TaskProposal): TaskProposalDisposition {
        submitCallCount++
        lastProposal = proposal
        return dispositionFor(proposal)
    }
}
