# Sprint 2 Track B Unit B1 Retrospective

Unit B1 is the first Sprint 2 unit outside Track A's identity/permission work, and the first to coordinate two already-existing runtime components (Task Manager, Agent Runtime) rather than build a new one in isolation. That makes it a reasonable point to look back at the process, not just the code, while it's fresh.

## What went well

The Track B Readiness Review and gap #42's own logging happened before any Kotlin was written, and between them they absorbed nearly all of the unit's ambiguity in advance: which two of the five specified `agent.*` events actually have a production emitter, why `AgentRunCommand.agentRunId` can't be used for correlation, and that `taskId` from the event payload was the only mechanism already available end-to-end. By the time implementation started, there was one real design question, and it had an existing, non-invented answer (`InMemoryAgentRuntime.publish` already carried `taskId`), so no second design was needed.

Test coverage landed complete on the first pass — all eight required behaviors were covered without a follow-up "you missed a case" round, and the full suite passed (261/261) the first time it was run, with no build-fix round needed. That's a contrast worth naming: Unit A2 needed a repair round for a coroutine/default-parameter compiler limitation; Unit B1 didn't need one at all.

## What surprised me

The `Commit: pending` placeholder in both `IMPLEMENTATION_GAPS.md` and `IMPLEMENTATION_HISTORY.md` survived two full documentation passes and only got corrected in a third, separate follow-up — even though the real commit hash was already known and stated as the review baseline by the time the post-implementation review happened. Nothing in the process was actually broken (I was never asked to fill it in earlier, and didn't invent a hash I didn't have), but it's a small gap in the workflow: nothing currently prompts "reconcile the placeholder once the real value is known," so it just waits for someone to notice and ask.

Separately, the historical wording I wrote into gap #42 before implementation — that `agent.action_denied`/`agent.action_deferred` "correspond to no `AgentRunStatus` value at all" — turned out to be slightly imprecise once checked against `AgentRuntimeSpecification.md` §9 in full during the post-implementation review. Both events do map to existing values (`FAILED`, `SUSPENDED`); what's actually missing is the per-action permission-evaluation code path that would ever produce them. The substantive conclusion (no emitter exists) was right the whole time; only the stated reason needed tightening. A gap entry written from a partial recollection of an adjacent specification is worth a second look once that specification gets read in full for the unit's own implementation.

## What slowed implementation

Not much, for the coding turn itself — that's the point of the readiness review having already done its job. The real cost in this unit was documentation-to-code ratio: a gap entry, an implementation, a full post-implementation review, a documentation follow-up round, and now this retrospective, for roughly forty lines of production code. That is a deliberate tradeoff — governance discipline over raw velocity — not an accident, but it is worth stating plainly rather than leaving implicit, since it will recur at the same rate for every future unit unless something changes.

## What should change for Unit B2

Unit B2 needs a fixed, minimal rule the same way `DeterministicPlannerHarness` and Unit B1 needed one — but `TaskManagerRuntimeSpecification.md` §6/§11 leaves "what does a received Agent Event do to Task Status" as a Task Manager rule to be decided, not an answer already sitting in the specification. That's the same shape of gap Unit A2 hit before coding started. Deciding the rule (at minimum: what happens on `agent.completed` with exactly one Agent Run Reference, and what happens on `agent.failed`) before Unit B2 begins would avoid a mid-unit stop-and-report.

Two smaller items worth a decision, not necessarily action, before B2: whether the `taskId` payload convention (an untyped `Map<String, String>` key) is solid enough to keep building on, or should be formalized; and whether `agent.cancelled`/`agent.action_denied`/`agent.action_deferred` need real emitters added to `InMemoryAgentRuntime` before a later unit is blocked on their absence, rather than that being discovered mid-unit the way Unit B1 discovered it for itself.

## Does the process itself need refinement?

Mostly, no — the loop (readiness review, gap log, implement and test, static verification, Android-Studio-gated documentation, full review) produced a unit that needed zero rework of production code and passed review cleanly on the first attempt. I'd keep it as-is.

One concrete refinement: treat the post-implementation review's own repository baseline (which already names the real commit) as the trigger to reconcile any `Commit: pending` placeholders in the same pass, rather than waiting for a separate, explicit follow-up request. The second, smaller one: when a gap entry is written before implementation and cites a specification section from memory rather than a fresh full read, flag it inline as provisional, so a wording correction during the post-implementation review reads as an expected step rather than a surprise.
