# ADR-024 – Module, Event, Audit, and Durability Boundary

## Status
Accepted

## Context

Pre-Module Readiness Units 1 and 2 closed two of the four findings the
independent architecture audit
(`docs/reviews/ARCHITECTURE_V2_INDEPENDENT_AUDIT_TRIAGE.md`) routed to an
ADR/design track: gap #49 (Planner Runtime publisher identity) and gap #48
(deterministic ID multiplicity). Three findings remain, and all three
share a single underlying question this ADR exists to answer once,
platform-wide, rather than three times: **what is Parker allowed to let a
module see, ask for, or depend on, before the platform can actually
support it safely?**

- **Gap #47** — `InMemoryWorldModel` does not publish state-change events,
  though `WorldModel.md` names this as a Responsibility. ADR-023 already
  settled the *shape* any context-provider event publication must take
  (publish-only, never subscribe, observability not authorisation) but
  left open whether Memory Runtime should ever be authorised to publish,
  and did not decide anything about module access.
- **Gap #50** — `InMemoryEventBus.publish()` delivers to every subscriber
  synchronously, inside the publisher's own coroutine. No subscriber
  today performs slow or blocking work, so this is correct at current
  scale, but a future subscriber that does (an Audit subsystem, or any
  module) would block every other publisher sharing the same `EventBus`
  instance.
- **Gap #51** — No subsystem's persistence boundary is defined.
  `MemoryStore.md` calls Memory "durable long-term knowledge," but
  `InMemoryMemoryStore` and `InMemoryIdentityService` both lose all state
  on process restart, and no `AuditService` implementation exists, so the
  Constitution's Auditability principle currently has no durable
  mechanism behind it anywhere in the platform.

No module system exists in this repository today. This ADR does not
create one. It defines the boundary a future module system must respect,
and the precise conditions under which each of the three gaps above must
be resolved before a given kind of module may be introduced.

## Decision

### A. Module Access Boundary

1. **Modules are capability providers, by constitutional definition.**
   The Constitution's own law — "Parker owns authority. Modules provide
   capability." — is not amended or reinterpreted by this ADR. A module's
   primary role is to be reached as a `Tool` (or an equivalent capability
   surface) via `ToolRegistry`/`ExecutionPipeline`, exactly as any other
   Tool is reached today.
2. **A module MAY also be an event subscriber, under the same discipline
   AD-012 and ADR-023 already impose on context providers.** Subscribing
   is permitted only for observability; a module that treats receipt of
   any event as authorisation to act is in violation of AD-007, exactly
   as ADR-023 Rule 4 already states for context providers.
3. **A module is never a fourth category with its own authority.** There
   is no path by which a module receives implicit trust, self-approves an
   action, bypasses `PermissionEngine.evaluate`, or writes directly to
   Task, Agent Run, Planning Session, Memory, or World Model state. Every
   module is an ordinary `Principal`, subject to the same Identity and
   Permission evaluation as any other actor.
4. **What a module may observe:** `EventBus` events it is subscribed to,
   under Rule 2 above, and only once gap #50's delivery-isolation
   precondition (Section C) is satisfied for that module.
5. **What a module may request:** exactly what any other proposer may
   request — submission of an `ExecutionRequest` via
   `ExecutionPipeline.submit`, evaluated by `PermissionEngine` like any
   other request. A module has no private or parallel request path.
6. **What a module may never do directly:** call `Tool.execute` without
   going through `ExecutionPipeline`; read or write Memory or World Model
   state directly rather than through their own published interfaces;
   mutate Task, Agent Run, or Planning Session state; grant itself or any
   other Principal a permission; or treat an observed event as
   authorisation (Rule 2).

### B. Context-Provider Event Publication

7. **World Model's publication shape is unchanged and reaffirmed.**
   ADR-023 already governs this fully (publish-only, no subscribe,
   observability not authorisation, full Cognition/Trust/Runtime chain
   for any resulting action). This ADR does not reopen it. Gap #47
   remains open until a future unit implements it against ADR-023's
   existing five rules.
8. **Memory Runtime is not authorised to publish events at this time.**
   `MemoryStore.md`'s Responsibilities list does not name event
   publication, and no consumer in this repository needs it today. Per
   ADR-023 Rule 1 ("publication requires authorisation... where an
   approved architecture or specification document names that
   publication as a responsibility"), Memory Runtime publishing anything
   would itself require a specification revision first — a documentation
   change outside this ADR's own scope, not decided here. If Memory
   Runtime is ever authorised to publish, ADR-023's existing five rules
   govern it without needing a new equivalent, exactly as ADR-023's own
   "Consequences" section already anticipates.

### C. EventBus Delivery Isolation

9. **EventBus remains synchronous for now.** No subscriber in this
   repository today performs slow or blocking work; changing delivery
   semantics with no subscriber to validate the change against would be
   the same unvalidated speculative generality rejected for gap #48.
10. **Delivery isolation is a precondition, not a concurrent task, for
    any subscriber that is not fast, non-blocking, and in-process.** Before
    a real Audit subsystem, or any module, is added as an `EventBus`
    subscriber, `InMemoryEventBus` must first be changed so that one
    subscriber's latency cannot delay delivery to any other subscriber or
    block the publisher's own `publish` call from returning.
11. **The target semantic is per-subscriber isolated dispatch, not a
    durable queue.** Each subscriber's `deliver` call must be isolated
    from every other subscriber's (concurrently dispatched, and/or
    timeout-bounded) rather than delivered strictly sequentially as
    today. This is explicitly **not** the same question as gap #51's
    durability boundary (Section D) — isolation protects against
    *latency*, durability protects against *loss*; a future
    implementation unit may address them separately or together, but
    must not conflate "isolated" with "durable" as though solving one
    solves the other.
12. **This precondition applies per-subscriber-kind, not platform-wide.**
    Adding one specific slow subscriber does not require redesigning
    delivery for every existing fast subscriber; it requires that the
    specific subscriber in question cannot degrade the others. This
    leaves room for an incremental implementation (e.g., isolating only
    non-core subscribers) rather than mandating a single, all-at-once
    `EventBus` rewrite.

### D. Audit and Durability Boundary

13. **What must eventually be durable:** Memory Records (`MemoryStore.md`
    already calls Memory "durable long-term knowledge" — the word itself
    creates the obligation), Principal records (`InMemoryIdentityService`
    is the trust foundation every other subsystem's identity claims rest
    on), and an Audit log satisfying the Constitution's "every authorized
    action leaves a record sufficient to reconstruct" guarantee.
14. **What is allowed to remain in-memory:** World Model beliefs
    (transience is part of `WorldModel.md`'s own definition — "current,
    replaceable belief about present reality" — not a durability gap
    needing closure), and any subsystem's internal, per-request working
    state that is not itself one of the three items in Rule 13.
15. **Memory MAY NOT be treated as durable, in the sense a caller can rely
    on across a process restart, before a real persistence layer exists
    and is verified.** `MemoryStore.md`'s "durable" language describes an
    intended property, not a currently-delivered guarantee. This ADR does
    not amend `MemoryStore.md` (out of this unit's own scope), but records
    the correct interpretation for any future unit that does: "durable"
    should be read as "logically durable within process lifetime; physical
    durability is a reserved seam," until a persistence-boundary ADR and
    implementation exist.
16. **What must be true before a real user module can rely on Memory:** a
    real, tested persistence backing that survives process restart. Until
    then, any module whose correctness depends on a Memory Record
    surviving a restart must not be granted access that assumes it does.
17. **What must be true before an audit-reconstruction claim is made:** a
    real, durable Audit mechanism — not `InMemoryEventBus` alone, which is
    explicitly at-most-once with no replay — must exist and receive events
    from every subsystem whose actions the claim covers. No document in
    this repository may state or imply that Parker's current
    implementation satisfies AD-009's Auditability guarantee in a durable,
    reconstructable sense; today's `EventBus` publication satisfies only
    the weaker, already-disclosed "publication occurs" reading gap #43's
    own text already uses.

### E. Pre-Module Rule

18. **No module access exists today.** This ADR authorises none. It
    defines the rule a future module-access unit must follow once one is
    proposed under its own Contract Design pass.
19. **Allowed, once module access is introduced, without waiting for gaps
    #47, #50, or #51:** a module that acts purely as a capability/Tool
    provider reached only via `ExecutionPipeline.submit` →
    `PermissionEngine.evaluate` → `Tool.execute`; never subscribes to
    `EventBus`; never reads Memory or World Model directly (receiving
    context, if any, only through whatever an existing runtime already
    passes it as ordinary request parameters); and is an ordinary
    Principal with no special implicit trust. Such a module depends on
    none of the three open gaps.
20. **Not allowed until the corresponding gap is closed:**
    - A module that subscribes to `EventBus` in any capacity — blocked on
      gap #50 (Section C): delivery isolation does not exist, so a slow
      or malicious module subscriber could degrade every other publisher
      and subscriber sharing the same `EventBus`.
    - A module whose behaviour depends on receiving a World-Model-change
      notification — blocked on gap #47: the event does not exist yet.
    - A module whose correctness depends on Memory, Identity, or an audit
      trail surviving a process restart — blocked on gap #51 (Section D):
      no persistence layer exists yet.
21. **Never allowed, independent of #47/#50/#51:** a module granted
    implicit trust, self-approval authority, a private execution path
    bypassing the Permission Engine, or direct write access to Task, Agent
    Run, Planning Session, Memory, or World Model state (Section A, Rules
    3 and 6).

## Reasoning

Every rule above is either a direct restatement of an already-settled
constitutional principle (Owner authority, Cognition proposes / Trust
authorises / Runtime executes, AD-007, AD-009, AD-012, ADR-023) applied to
the word "module" for the first time, or a disclosure of a precondition
gap #47, #50, or #51 already names. Nothing here invents new authority,
new capability, or new architecture; it only says, precisely, which
already-open gap blocks which future kind of module access, so that a
future module-access proposal does not have to re-derive this reasoning
or discover a blocking dependency mid-implementation the way gap #48's
undisclosed ID-multiplicity cap was discovered by audit rather than by
design.

The Section C delivery-isolation decision deliberately keeps latency
isolation (gap #50) and durability (gap #51) as two separate concerns,
even though both will likely be touched by whatever unit eventually adds
a real Audit subscriber. Conflating them risks a future implementation
believing that making delivery concurrent also makes it durable, which is
false, or that making storage durable also protects against a slow
subscriber blocking others, which is equally false.

## Relationship to Gaps #47, #50, and #51

This ADR does not close any of the three gaps. Each remains open, with
its implementation still to be authorised by a future Contract Design
pass:

- **Gap #47** — governed by ADR-023 already; this ADR adds only the
  module-access framing (Section A) and the explicit non-authorisation of
  Memory publication (Section B, Rule 8). `IMPLEMENTATION_GAPS.md` #47's
  own status is updated to record that an architectural decision now
  exists, not that implementation has occurred.
- **Gap #50** — this ADR settles the target semantic (per-subscriber
  isolated dispatch, Section C) and the precondition rule (isolation
  before a slow subscriber is added), but implements nothing.
  `IMPLEMENTATION_GAPS.md` #50 is updated the same way.
- **Gap #51** — this ADR settles what must eventually be durable, what
  may remain in-memory, and the two preconditions (module reliance on
  Memory; audit-reconstruction claims), but implements no persistence or
  audit storage. `IMPLEMENTATION_GAPS.md` #51 is updated the same way.

## Consequences

No Kotlin, test, or existing specification changes result from this ADR.
A future unit implementing gap #47, #50, or #51 has a settled shape to
implement against rather than an open design question. A future
module-access proposal has a settled boundary (Section A, Section E) to
build within, and a precise list of which module kinds require which gap
to close first, rather than discovering the dependency mid-implementation.

## Future Considerations

- Whether `memory.*` should ever be added to `EventBus.md`'s
  trust-sensitive domain list is not decided here (mirrors ADR-023's own
  identical open item for `worldmodel.*`), and remains moot until Memory
  Runtime publication is ever authorised (Section B, Rule 8).
- Whether Section C's per-subscriber isolation should eventually converge
  with whatever durable delivery mechanism gap #51's own resolution
  produces (e.g., a durable audit log that also happens to isolate slow
  consumers) is an open implementation question for whichever future unit
  addresses both — this ADR requires only that the two decisions remain
  analytically separate (Section C, Rule 11), not that their eventual
  implementations must be unrelated.
- The exact mechanism for Section A, Rule 2's "future, separately
  authorised read-only observability purpose" for module event
  subscription (e.g., whether module subscriptions require their own ADR
  per module, or one general module-subscription ADR covering all of
  them) is left to whichever future unit first proposes a
  module-subscribing module.
