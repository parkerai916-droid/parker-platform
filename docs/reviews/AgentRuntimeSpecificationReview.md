# Agent Runtime Specification — Review Report

Review of `docs/specifications/volume-04-agent-runtime/AgentRuntimeSpecification.md`
(Version 0.1-draft) against the existing Parker Platform architecture,
performed by re-reading the specification alongside `IdentityService.md`,
`EventType.md`, `EventBus.md`, ADR-002, `docs/architecture/tool-registry.md`,
`docs/specifications/volume-03-core-interfaces/ToolRegistry.md`,
`docs/architecture/action-mapping.md`, and — because the review surfaced a
collision the specification itself does not address — Chapter 37 (Task
Manager), `Task-Schema.md`, `docs/diagrams/task-lifecycle-state-machine.mmd`,
ADR-012, and Chapter 18 (Context Engine). This is a review only: no code
was written, read for the purpose of changing, or modified, and no file
other than this report was created.

## Summary

The specification is well-grounded where it deliberately restates existing
architecture: identity-first design, single Execution Pipeline, no direct
Tool access for Agents, and the Permission Engine as sole source of
authority are all consistent with `IdentityService.md`, `ADR-001`,
`ADR-003`, `tool-registry.md`, and `action-mapping.md`. No instance of an
Agent holding a live `Tool` reference, self-authorising, or bypassing the
Execution Pipeline was found.

Two issues are significant enough to resolve before this document is
treated as settled: the specification's "Task" concept is never
reconciled with the platform's existing, independently-specified Task
Manager (`Task-Schema.md`, ADR-12) — including five reused lifecycle
-state tokens — and the "revoked authority must stop future execution"
requirement has no currently-specified detection mechanism, because both
of the platform capabilities that could supply one (`identity.*` audit
events, and `IdentityService.resolve()` suppressing non-Active
Principals) are themselves still-open items in
`IMPLEMENTATION_GAPS.md` (#39 and #37 respectively). A further set of
medium-severity gaps exists in the Event Model: one event conflates two
lifecycle-distinct outcomes, and three lifecycle states have no
corresponding event at all, which undercuts the document's own
"auditable decisions" design goal. Everything else found is minor
naming/consistency polish, listed below for completeness.

None of the issues found require a redesign. All are the kind of
"tighten before implementation" findings this repository's other
specification passes (`IMPLEMENTATION_GAPS.md`) have already handled the
same way — recorded, not silently patched over.

## Confirmed alignments

- **No direct Tool execution by Agents.** Every mention of Tool access
  (Sections 1, 2, 6, 11) routes through `ExecutionPipeline.submit` →
  `PermissionEngine.evaluate` → `ToolRegistry.resolve`, with an explicit
  statement that an Agent Instance never queries `ToolRegistry.resolve`
  directly and never holds a `Tool` reference. Consistent with ADR-001
  and `tool-registry.md`'s "nothing except the Execution Pipeline ever
  holds a live `Tool` reference."
- **No new `PrincipalType`.** Agent Identity resolves to the existing
  `PrincipalType.INTERNAL_AGENT` (`src/contracts/Principal.kt`); no new
  enum value or parallel identity concept is proposed. Consistent with
  `IdentityService.md`.
- **Delegated Authority reuses the existing `owner` field exactly.**
  Section 7's requirement that an Agent Instance's Principal have a
  non-null `owner` established at registration matches
  `IdentityService.md`'s "Trust Relationships" section
  ("requires an already-established owning context ... for any
  PrincipalType other than USER or SYSTEM") without inventing a second
  delegation mechanism.
- **`PermissionEngine.evaluate` called exactly once per
  `ExecutionRequest`.** Section 6 matches `action-mapping.md`'s corrected
  "Multiple Actions" text precisely, rather than the earlier, superseded
  "once per resolved action" description.
- **Malformed/unresolvable actions treated as invalid, not denied.**
  Section 10 ("Malformed action") restates `action-mapping.md`'s "Unknown
  Actions" distinction correctly and does not conflate it with a
  `PermissionDecisionOutcome.DENIED` outcome.
- **Agent Context correctly distinguished from Memory** via ADR-002's own
  three-way split (Context / World Model / Memory), and Section 3
  explicitly disclaims any Memory or World Model read/write.
- **`agent.*` `EventType` values are syntactically valid** under
  `EventType.kt`'s actual validator (non-blank, contains a `.`) and follow
  `EventType.md`'s `<domain>.<event>` convention at the top level.
- **No Agent-invocable operation mutates identity or permission state.**
  Section 11 states this explicitly, and no operation named anywhere in
  the document (Sections 4, 6, 7) contradicts it — `IdentityService.updateStatus`
  is correctly described as remaining an exclusively platform-service
  operation.
- **Terminal lifecycle states have no outgoing edges.** `COMPLETED`,
  `FAILED`, `CANCELLED` match the no-resurrection convention already used
  by `ExecutionLifecycleState`, `ToolLifecycleTransitions`, and
  `PrincipalLifecycleTransitions`.
- **Every listed Agent Event ties to something the document itself
  defines** (a lifecycle transition or a Section 6 Step activity) — no
  event was found that lacks any grounding at all (see Issues below for
  the narrower problem of *coverage gaps*, which is different from
  ungrounded events).
- **Non-Goals correctly exclude Memory, World Model, Planner, Android
  integration, direct external-system access, and unrestricted autonomy**,
  matching the task's explicit scope.

## Issues found

### High severity

**1. "Task" is introduced without reconciling it against the existing,
independently-specified Task Manager.** ADR-012 already decides that
"Tasks track work. Workflows define structured multi-step behaviour," and
`Task-Schema.md`/`docs/diagrams/task-lifecycle-state-machine.mmd` already
give Task a full shape: required fields `taskId, ownerPrincipalId, status,
createdAt, updatedAt`, and a nine-state lifecycle (`CREATED, QUEUED,
RUNNING, PAUSED, COMPLETED, FAILED, CANCELLED, EXPIRED, SUPERSEDED`). The
new specification's Section 4 defines "Task" informally ("a bounded unit
of work an Agent Instance undertakes toward a Goal") with no reference to
ADR-012, `Task-Schema.md`, or Chapter 37 anywhere in the document —
including the "assumes familiarity with" list and the Related section,
both of which omit Chapter 37 entirely. It is not stated whether an Agent
Instance's Task *is* a Task Manager `Task` (same `taskId`), a specialised
subtype of one, or a deliberately unrelated, coincidentally-named concept.

Compounding this, the Agent lifecycle (Section 5) reuses five of the same
state-name tokens as the canonical Task lifecycle — `CREATED`, `RUNNING`,
`COMPLETED`, `FAILED`, `CANCELLED` — for a state machine with different
transitions and different meaning (e.g. `RUNNING` means "an Agent Instance
is actively proposing Steps" here, versus "this unit of tracked work is
currently executing" in `Task-Schema.md`). Two independently-specified
state machines sharing five identical tokens is a realistic source of
confusion in audit logs, cross-document references, and any future code
that assumes a single `status`/lifecycle vocabulary.

**2. The "revoked authority must stop future execution" requirement has
no specified detection mechanism, and the two platform capabilities that
would naturally supply one are both still open.** Section 7 states the
Agent Runtime "MUST NOT allow that Agent Instance to enter or remain in
`RUNNING`" once its Principal is Suspended/Revoked, and Section 10 repeats
this as a `FAILED` transition. Neither section says how the Agent Runtime
learns that the status changed. This is not a hypothetical gap:

- `IMPLEMENTATION_GAPS.md` #37 records that `InMemoryIdentityService.resolve()`
  "does not suppress Revoked or Archived Principals" and explicitly defers
  the "cannot act" decision to callers.
- `IMPLEMENTATION_GAPS.md` #39 records that `identity.*` audit events
  (`identity.principal.status_changed` etc.) are not published by
  `InMemoryIdentityService` at all.

With neither a poisoned-resolve signal nor an event stream currently
available, Section 7's MUST requirement is unenforceable exactly as
specified today, and the document does not flag this dependency or say
what an implementation should do in the meantime (poll `resolve()` before
every Step? something else?).

### Medium severity

**3. `agent.action_denied` conflates two lifecycle-distinct outcomes.**
Section 9's table states this event fires for both
`PermissionDecisionOutcome.DENIED` **and** `DEFERRED`. But Section 5
routes these to different, non-equivalent transitions:
`WAITING_FOR_PERMISSION --> FAILED` on `DENIED` (terminal), versus
`WAITING_FOR_PERMISSION --> SUSPENDED` on `DEFERRED` (recoverable). Naming
both cases "denied" would mislead any subscriber (Audit, a future
dashboard) filtering on that event into treating a still-pending,
recoverable decision as a hard denial.

**4. Three lifecycle states have no dedicated entry event.**
Cross-referencing Section 5's ten states against Section 9's table:
`INITIALISED`, `READY`, and entry into `WAITING_FOR_INPUT` have no
corresponding row, unlike every other state (`CREATED`, first `RUNNING`,
`WAITING_FOR_PERMISSION`, `SUSPENDED`, `COMPLETED`, `FAILED`, `CANCELLED`,
all of which do). This directly undercuts Section 2's own "Auditable
decisions" design goal ("every lifecycle transition ... is observable as
an Agent Event").

**5. No event marks resumption into `RUNNING` from `WAITING_FOR_PERMISSION`
or `WAITING_FOR_INPUT`.** `AgentResumed`/`agent.resumed` is scoped in the
table specifically to "Lifecycle transitions `SUSPENDED --> RUNNING`."
The other two paths back into `RUNNING` (post-approval,
post-input-received) have no equivalent — `AgentActionApproved` records
the permission decision itself, not a lifecycle re-entry, and nothing
marks input having arrived.

**6. The trust boundary for `WAITING_FOR_INPUT` is unspecified.** The
document never states whether a mechanism that supplies input during this
state must itself be mediated by Section 6's `ExecutionRequest`/Permission
Engine path when the input's origin is a protected Resource (e.g., input
that is actually "the contents of a sensitive document"). Left this
implicit, it is a plausible channel through which a Resource read could
occur without a corresponding `PermissionDecision` — the kind of gap
Section 11 ("No direct external effects") is meant to close everywhere
else.

**7. "Agent Context" is never related to the existing Context Engine
(Chapter 18).** Section 8 grounds "Agent Context is not long-term Memory"
in ADR-002, but ADR-002's own "Context" side of that split is Chapter 18's
domain ("Context Engine ... resolves ambiguous references using recent
conversation, current task, World Model, Memory, calendar, device state
and location. Context is temporary."). The specification defines Agent
Context as an analogous-but-separate concept without saying whether it is
a specialisation of, an input to, or entirely independent from the
Context Engine. Chapter 18 is also absent from the Related section.

### Low severity

**8. Agent Event naming diverges from the one nested-domain precedent
that exists.** `EventType.md`'s only worked compound-name examples are
dot-nested (`identity.principal.registered`,
`identity.principal.status_changed`) rather than a flat
domain-then-underscored-phrase shape. The new `agent.*` events
(`agent.step_started`, `agent.action_proposed`, etc.) are valid under
`EventType.kt`'s loose validator, but follow a different shape than the
only existing worked example, which matters for `EventType.md`'s own open
question about prefix/wildcard subscription (`agent.step.*` reads
naturally; a flat `agent.step_started`/`agent.step_completed` pair does
not).

**9. "Agent Type" is defined but never used again.** No other section —
not Agent Context (Section 8), not the Event Model (Section 9), not
Agent Result — references it, leaving its runtime role (catalogue key?
a field somewhere?) entirely unspecified.

**10. Section 6's sequence diagram doesn't match `ToolRegistry.md`'s real
signature.** The diagram shows `EP->>TR: resolve(decision.action,
request.targetResources)`, but
`docs/specifications/volume-03-core-interfaces/ToolRegistry.md` specifies
`resolve(action: PermissionAction, resourceTypes: Set<ResourceType>):
ToolResolution` — the diagram passes Resource references where the real
interface takes `Set<ResourceType>`. This exact shorthand is inherited
from `docs/architecture/tool-registry.md`'s own sequence diagram (so it
predates this document and is not a new defect), but since this review
was specifically asked to check against `ToolRegistry.md`, it is worth
tightening here rather than propagating it further.

**11. Chapter 19 (Conversation Engine) and Chapter 37 (Task Manager) are
implicated but absent from the prerequisite/Related lists.** Section 6
names "a Planner or Conversation Engine" as another consumer of Tool
Registry's discovery surface, but Chapter 19 appears nowhere else in the
document. Chapter 37's absence is the more significant instance and is
covered by Issue #1 above.

## Recommended fixes

1. Add a short subsection reconciling Agent Runtime "Task" with Chapter
   37/ADR-012/`Task-Schema.md` — at minimum, state explicitly whether an
   Agent Instance's Task is the same `Task` type, and either way, rename
   the overlapping lifecycle-state tokens (or the Agent lifecycle's states
   generally) so `CREATED`/`RUNNING`/`COMPLETED`/`FAILED`/`CANCELLED` are
   not simultaneously two different state machines' vocabulary.
2. Add a subsection specifying the intended revocation-detection
   mechanism (e.g. "the Agent Runtime MUST call `IdentityService.resolve`
   before each Step and treat a non-Active result as revocation," or
   defer explicitly to `identity.*` events once `IMPLEMENTATION_GAPS.md`
   #39 is implemented), and cross-reference #37/#39 directly so the
   dependency is visible to whoever implements this next.
3. Split `agent.action_denied` into two table rows: keep it for `DENIED`
   only, and add a distinct `AgentActionDeferred`/`agent.action_deferred`
   event for the `DEFERRED` case, updating Section 5's cross-reference to
   match.
4. Add `AgentInitialised`/`agent.initialised`, `AgentReady`/`agent.ready`,
   and `AgentInputRequired`/`agent.input_required` rows to Section 9 (or,
   if the omission is deliberate, say so explicitly and why).
5. Add an event (or an explicit note in Section 9's `AgentActionApproved`
   row) covering resumption from `WAITING_FOR_PERMISSION`/
   `WAITING_FOR_INPUT` back into `RUNNING`, so `AgentResumed` is not the
   only "back to running" signal.
6. Add a sentence to Section 11 (or a bullet in Section 8) requiring that
   any input-supplying mechanism reading a protected Resource during
   `WAITING_FOR_INPUT` go through the same Section 6 path — no
   input-shaped exception to Permission Engine mediation.
7. Add Chapter 18 to the Related section and add one clarifying sentence
   in Section 8 about Agent Context's relationship (or lack of one) to
   the Context Engine.
8. Add Chapter 19 and Chapter 37 to "assumes familiarity with"/Related,
   or remove the Conversation Engine reference if it isn't meant to be
   load-bearing.
9. Correct Section 6's sequence diagram to `resolve(decision.action,
   requiredResourceTypes)` (or equivalent), matching `ToolRegistry.md`'s
   actual parameter types.
10. Optional: reconsider `agent.*` event naming toward the nested-dot
    shape (`agent.step.started`) for consistency with `identity.principal.*`
    and easier future wildcard subscription; not required, since
    `EventType.md` does not mandate it.
11. Optional: either give "Agent Type" a concrete runtime home (a field
    on Agent Context or Agent Result) or remove it from Core Concepts
    until it has one.

## Open questions requiring human decision

- Is an Agent Instance's Task the same `Task` type Chapter 37/`Task-Schema.md`
  already defines, a subtype of it, or an intentionally separate concept
  that should be renamed to avoid the collision? (Issue #1)
- Should the Agent lifecycle's state names be changed to avoid overlapping
  with `Task.status`'s vocabulary, given both currently use `CREATED`,
  `RUNNING`, `COMPLETED`, `FAILED`, `CANCELLED`? (Issue #1)
- What should the actual revocation-detection mechanism be — poll
  `IdentityService.resolve` before each Step, wait for `identity.*` events
  once `IMPLEMENTATION_GAPS.md` #39 is implemented, or something else —
  given both currently-available signals are incomplete? (Issue #2)
- Should `DEFERRED` get its own distinct Agent Event, separate from
  `DENIED`? (Issue #3)
- Should `INITIALISED`, `READY`, and `WAITING_FOR_INPUT` entry have
  dedicated events, or is coarser-grained auditing an intentional,
  accepted tradeoff for this phase? (Issue #4)
- Is "Agent Context" meant to be a specialisation of, an input to, or
  independent from the future Context Engine (Chapter 18) implementation?
  (Issue #7)
- Should `agent.*` be added now to `EventBus.md`'s trust-sensitive domain
  list (already flagged as an open question inside the specification
  itself), and should its naming shape change to the nested-dot
  convention? (Issues #8, plus the specification's own Open Questions
  section)

## Final readiness assessment

**Not yet ready to be treated as settled/normative for an implementation
phase to build against as-is.** The specification succeeds at its core
job — it introduces no permission bypass, no direct Tool access for
Agents, and no new identity mechanism, and its Non-Goals section
successfully keeps Memory, World Model, and Planner out of scope. But one
real naming/architecture collision (Task, Issue #1) and one requirement
with no currently-enforceable mechanism (revocation detection, Issue #2)
should be resolved first, since both affect how a future implementation
phase would actually be built, not just how the document reads. The
Event Model gaps (Issues #3–#5) are cheap to fix and should be folded in
at the same time. None of the findings require reworking the lifecycle,
the execution model, or the safety boundaries — this is a revision pass,
not a redesign, and every issue above is recorded rather than silently
patched, consistent with how this repository has handled every prior
specification gap.
