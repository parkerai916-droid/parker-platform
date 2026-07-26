# Sprint 11, Unit 2 — Production Reasoning Context Contract Design — Engineering Checkpoint

## Status

**Engineering Checkpoint, PES-001 Stage 9, contract-only Unit.** Reviews
risk across the Contract Design and Sequence documents before any
implementation begins. Per this Unit's own instruction, risks are
recorded here, not resolved — resolving any of them by implementation
detail would be exceeding this Checkpoint's own authority.

---

## 1. Contract Risks

- **`PipelineStage.UNKNOWN` cannot distinguish an assembly failure from
  any other fault.** `PRODUCTION_REASONING_CONTEXT_CONTRACT_DESIGN.md`
  Section 6 accepts this as the honest consequence of not modifying
  `ParkerRuntimeOutcome.kt`'s frozen `PipelineStage` enum. The risk: an
  operator debugging a real `Failed(UNKNOWN, ...)` outcome cannot tell,
  from the outcome alone, whether context assembly, `ResponseComposer`,
  or anything else in the "everything but `REASONING`" bucket actually
  failed. Not solved here — a future Unit could propose a dedicated
  `PipelineStage` value, but that is a change to frozen production code
  this Unit is not authorised to make.
- **Whether a degraded, partial `ReasoningContext` is ever acceptable is
  undecided.** Contract Design Section 6 names this explicitly as open:
  if one dependency read fails (say, `IdentityService.resolve` for a
  secondary participant, not the sender), should `assemble` fail the
  whole call, or return a `ReasoningContext` missing just that one
  entry? Both are defensible; neither is decided. A future Contract
  Design revision or Implementation Plan must decide this before real
  implementation, not infer it from silence.
- **The illustrative interface shown may not survive contact with real
  dependency shapes.** `fun interface ReasoningContextAssembler { suspend
  fun assemble(message: InboundOwnerMessage): ReasoningContext }` is
  sketched against only the two dependencies this Unit could justify
  today (`IdentityService`, `ToolRegistry`). Once Memory, World Model,
  and Conversation History each gain a real shape (Section 5, Section 6,
  below), the single-method, single-argument shape may prove too thin —
  e.g. if any future dependency needs per-call parameters beyond the
  message itself. Not decided here; flagged so a future Contract Design
  revision does not treat this interface as more final than it is.

## 2. Dependency Risks

- **`IdentityService` and `ToolRegistry` read-only usage is a documented
  discipline, not a type-enforced one.** Contract Design Section 4.1
  states plainly that the Assembler calls `resolve` and
  `listAll`/`findCandidates` only — but both interfaces are injected
  whole, mutating methods included. Nothing in Kotlin's type system
  prevents a future implementation from calling `IdentityService.register`
  or `ToolRegistry.setLifecycleState` from inside `assemble`, silently
  breaking side-effect-freedom (Contract Design Section 5) without
  breaking compilation. This is a real enforcement gap, not merely a
  style preference — recorded, not solved (splitting either interface
  into read/write halves would modify a frozen contract, out of this
  Unit's authority).
- **Conversation History has no existing interface at all — a sharper
  gap than Memory or World Model.** At least Memory and World Model are
  named, if undesigned, elsewhere in this architecture
  (`reasoning-context.md`). Conversation History (prior Turns) has no
  read surface anywhere in this codebase today — `ConversationEngine`
  exposes only the mutating `submitTurn`. A future integration Unit must
  design this from nothing, not merely narrow an existing read method
  the way `IdentityService`/`ToolRegistry` usage was narrowed here.
- **Unknown future shape-fit for Memory and World Model dependencies.**
  Contract Design Section 4.2 defers both entirely. Risk: when each
  integration Unit actually defines its own read-only boundary, its
  natural shape (e.g., a similarity search taking a query string and
  returning ranked results, rather than a simple synchronous read) may
  not fit cleanly as a single constructor-injected collaborator called
  once per `assemble` invocation. Not decided here.

## 3. Lifecycle Risks

- **Conflating the Assembler's own component lifecycle with the
  `ReasoningContext` value's lifecycle.** Contract Design Section 7
  draws this distinction explicitly (constructed once at startup, versus
  produced fresh per message) specifically because the risk of
  conflating them is real: a future implementation could, for instance,
  cache a resolved `Principal` on the Assembler instance itself "since
  it rarely changes," silently violating Statelessness (Contract Design
  Section 5) while appearing to work correctly in every test that
  doesn't exercise a Principal change mid-session. Recorded as a
  discipline risk for whichever future Unit implements this.
- **Assembler construction failure during startup is not explicitly
  walked through.** `ParkerRuntime.buildAndRegisterRuntimeGraph`'s
  existing `stage()` helper wraps construction failures as
  `ParkerRuntimeException.DependencyConstructionFailed` today for every
  other collaborator it builds; this Contract Design assumes, but does
  not explicitly confirm, that a future implementation would register
  Assembler construction the same way. Not a design decision this Unit
  is authorised to make (it would touch `ParkerRuntime.kt`), but worth
  naming so a future implementation Unit does not have to rediscover the
  expectation from first principles.

## 4. Ownership Risks

- **A second future production entry point would need its own
  Assembler-invocation decision, not covered here.**
  `SPRINT_11_UNIT_1_ENGINEERING_CHECKPOINT.md` Section 2 already named
  this as an open assumption (a second Communication Channel module or
  scheduled-task trigger might one day call into the runtime
  independently of `ParkerRuntime.submitOwnerMessage`). This Unit's
  Sequence document (`PRODUCTION_REASONING_CONTEXT_SEQUENCE.md` Section
  3) describes exactly one call site because exactly one exists today.
  If a second is ever added, whether it shares the same Assembler
  instance or requires its own invocation path is not decided by this
  Unit either.
- **Discipline risk: a future implementer reaching around the missing
  Conversation History boundary "just this once."** Section 2, above,
  names the gap; the ownership risk is what happens under real delivery
  pressure — a future implementation could be tempted to inject
  `ConversationEngine` directly into the Assembler to unblock "Current
  conversation" without waiting for a proper Conversation History
  boundary, quietly handing the Assembler the ability to call
  `submitTurn` and violating both Statelessness and Side-effect-freedom
  (Contract Design Section 5) at once. Named here explicitly so review
  of that future implementation has this exact risk to check against.

## 5. Future Memory Integration Risks

- **Shape-fit uncertainty**, restated from Section 2 specifically for
  Memory: a future Memory integration's own natural query shape may not
  match "one more constructor-injected dependency, called once."
- **Scope-creep risk.** Once Memory integration exists and is a real,
  reachable dependency, the temptation to let the Assembler read *more*
  of Memory than a given task genuinely needs — "since it's already
  there" — is exactly what Scope Lock Section 2 (Memory database itself
  excluded) and Section 3 (Projection, not source of truth) exist to
  prevent. Naming the risk now, before the dependency exists, is
  intended to make that boundary easier to hold once it does.

## 6. Future World Model Integration Risks

- **Shape-fit uncertainty**, restated from Section 2 for the World
  Model, same as Memory.
- **Staleness risk unique to the World Model's own "live, frequently
  changing" nature.** `reasoning-context.md` itself describes the World
  Model as "expected to change frequently." If a future Assembler reads
  a World Model excerpt at assembly time, but reasoning, response
  composition, and Trust authorisation all take real time afterward, the
  excerpt could be stale relative to the World Model's own current state
  by the time a proposal is actually authorised. This is not a defect in
  anything this Unit defines — assembly-time-of-read is inherent to the
  "task-scoped snapshot" nature of Reasoning Context itself
  (`reasoning-context.md`'s own framing) — but it is a genuine future
  correctness question a Memory/World-Model-integration Unit will need
  to address (e.g., whether staleness beyond some bound should ever
  block a proposal), not decided here.

## 7. Recommendation

No risk above is assessed as blocking Contract Design acceptance — every
one is either a discipline risk for future implementation, a shape
question a future integration Unit owns, or an explicitly-acknowledged
gap in today's frozen production code this Unit cannot close. This
Checkpoint recommends architectural review of the Contract Design and
Sequence documents before any implementation Unit begins, per PES-001's
own Stage sequencing.
