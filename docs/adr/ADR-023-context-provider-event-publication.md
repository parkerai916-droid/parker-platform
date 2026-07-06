# ADR-023 – Context Provider Event Publication

## Status
Accepted

## Context

Memory Runtime and World Model Runtime are both, per AD-012, read
sources that inform planning and context -- neither is, or may become,
an orchestration system with authority to trigger execution or mutate
Task, Agent Run, or Planning Session state. Agent Runtime and Planner
Runtime both publish a rich set of domain events (`agent.*`,
`planner.*`) onto the `EventBus` for every meaningful lifecycle
transition, per AD-009. Memory Runtime currently publishes nothing --
correctly, since `docs/specifications/volume-03-core-interfaces/MemoryStore.md`'s
Responsibilities list never names event publication as one of Memory's
responsibilities. World Model Runtime also currently publishes
nothing, but `docs/specifications/volume-03-core-interfaces/WorldModel.md`
does name "Publish state change events" as one of the World Model's
five Responsibilities -- a disclosed, open gap
(`docs/architecture/IMPLEMENTATION_GAPS.md` #47), not a silent
omission.

Gap #47 already reasons out, informally, how `InMemoryWorldModel`
could publish `worldmodel.*` events without gaining orchestration
authority, and explicitly declines to implement it without an approved
event-name/payload contract. This ADR exists to give that reasoning a
permanent, platform-wide home -- so that Memory, World Model, and any
future context-provider subsystem answer the same question the same
way, rather than each independently re-deriving it the way gap #47
currently does alone.

This ADR resolves the architectural shape a context provider's event
publication must take. It does not implement anything: `InMemoryWorldModel`
still does not publish `worldmodel.*` events after this ADR is
recorded, and gap #47 remains open until a future implementation unit
closes it against the shape this document defines.

## Decision

A **context provider** -- Memory Runtime, World Model Runtime, and any
future subsystem whose role is to inform reasoning and proposals
without carrying authority to act (AD-012) -- may publish events onto
the `EventBus` under the following rules:

1. **Publication requires authorisation.** A context provider may
   publish events describing its own state changes only where an
   approved architecture or specification document names that
   publication as a responsibility. Memory Runtime is not currently
   authorised to publish events (`MemoryStore.md` names no such
   responsibility); World Model Runtime is authorised
   (`WorldModel.md` does), pending the event-name/payload contract a
   future implementation unit must still define.

2. **Context providers never subscribe for autonomous behaviour.** A
   context provider may publish; it may not subscribe to the
   `EventBus` for the purpose of triggering its own action in
   response to another component's event. A context provider that
   subscribes to anything, for any reason, other than a future,
   separately-authorised read-only observability purpose, has ceased
   to be a context provider in the sense AD-012 defines and must be
   treated as an architectural violation, not a feature.

3. **A published event is an observability signal, never an
   authorisation.** An event published by a context provider states
   only that something changed. It grants no permission, triggers no
   downstream action by itself, and is not a substitute for any step
   in Cognition proposes / Trust authorises / Runtime executes.

4. **No subscriber may treat receipt of a context-provider event as
   permission to act.** Any subscriber -- present or future -- that
   receives a `memory.*` or `worldmodel.*` event and treats that
   receipt alone as authorisation for an action is itself in
   violation of AD-007 (Permission Decisions Belong to the Permission
   Engine), regardless of how the context provider's own publication
   is implemented.

5. **Any action taken as a result of observing such an event must
   still pass through the full chain.** If a subscriber decides,
   having observed a context-provider event, that some action should
   be proposed, that action still originates as a proposal from
   Cognition, is still evaluated by the Permission Engine, and is
   still carried out only by the Execution Pipeline -- exactly as if
   the subscriber had reached the same conclusion by any other means.
   A context-provider event may inform that a subsystem decides to
   propose something; it never authorises anything by itself.

## Reasoning

Context providers are deliberately excluded from the "propose /
orchestrate / authorise / execute" chain AD-002 establishes -- they are
read sources, not participants in that chain. Allowing a context
provider to publish observability events does not, by itself, put it
back into that chain, provided publication remains one-way: a context
provider that only ever calls `EventBus.publish` and never
`EventBus.subscribe` has no path by which observing its own
environment could change its own future behaviour, which is the
property AD-012 actually requires ("neither is, or may become, an
orchestration system"). The risk this ADR guards against is not the
publish call itself -- it is a future subscriber quietly treating a
context-provider event as if it carried more authority than a passive
state-change notice, which would smuggle an authorisation decision
outside the Permission Engine's own sole authority (AD-007) without
either side individually appearing to violate anything.

## Relationship to Gap #47

This ADR gives `docs/architecture/IMPLEMENTATION_GAPS.md` gap #47 its
governing architectural shape. A future implementation unit closing
gap #47 must:

- name the specific `worldmodel.*` event types it introduces (for
  example, `worldmodel.belief_accepted`, `worldmodel.belief_invalidated`)
  in an approved specification or Contract Design addendum, satisfying
  Rule 1 above;
- add an injected, optional `EventBus` dependency to
  `InMemoryWorldModel` that is only ever used to call `publish`, never
  `subscribe`, satisfying Rule 2;
- document, in that unit's own Post-Implementation Review, that the
  new events are observability-only and that no `EventBus` read-back
  path exists, satisfying Rules 3–5.

This ADR does not itself authorise implementing gap #47 -- it removes
the open architectural question standing in front of doing so. Gap
#47's own "Open, pending implementation" status is unchanged by this
ADR; only the shape a closing unit must follow is now settled.

## Consequences

A future Memory Runtime event-publication proposal (should
`MemoryStore.md` ever be revised to name one) is governed by this same
ADR without needing its own, separately-reasoned equivalent. Any future
context-provider subsystem inherits the same five rules by
construction, rather than needing to re-derive them the way gap #47
currently does alone. No existing implementation changes as a result
of this ADR: `InMemoryMemoryStore` and `InMemoryWorldModel` are both
unmodified by this decision, and gap #47 remains open.

## Future Considerations

Whether `memory.*` and `worldmodel.*` should be added to `EventBus.md`'s
trust-sensitive domain list (alongside `permission.*` and
`execution.*`, per `InMemoryEventBus`'s existing signature-requirement
check) is not decided here and remains open for whichever future unit
actually implements publication for either subsystem.
