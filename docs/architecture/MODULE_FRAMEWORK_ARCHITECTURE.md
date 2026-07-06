# Parker Platform — Module Framework Architecture

## Status

Architecture document. Defines Parker's module framework conceptually,
now that Architecture v2.0 is frozen
(`docs/architecture/ARCHITECTURE_V2_FROZEN_BASELINE.md`) and pre-module
readiness has been verified (Pre-Module Readiness Units 1–3: gap #49
closed, gap #48 formally constrained, and ADR-024 settling the
architectural shape of gaps #47, #50, and #51). This document does not
implement anything. No Kotlin interface, class name, package layout, or
loading mechanism is defined here — see Section 10.

## Purpose

Parker Core — Identity Runtime, EventBus, Planner Runtime, Agent Runtime,
Memory Runtime, and World Model Runtime — is complete and reviewed. The
next phase of platform growth is capability, not foundation
(`ARCHITECTURE_V2_FROZEN_BASELINE.md` Section 8, "Next Phase," names
Plugins explicitly as one such capability-growth direction). Before any
module can be proposed, described, or implemented, Parker needs one
settled answer to "what is a module, and what may it do" — this document
is that answer, at the architecture level, building directly on ADR-024's
module-access boundary rather than reopening it.

---

## 1. What a Parker Module Is

**A Parker Module is a capability provider. It is never an authority
layer.**

This restates, for "module" specifically, the Constitution's own law:
"Parker owns authority. Modules provide capability." A module extends
what Parker can *do* — new tools, new integrations, new adapters to
external systems — without extending *who may decide* that Parker should
do it. Every module operates as an ordinary `Principal`, evaluated by the
same trust chain (Cognition proposes, Trust authorises, Runtime executes)
as every other actor in this platform, per ADR-024 Section A, Rule 3.

A module is not a second Planner, not a second Runtime, and not a second
source of permission decisions. Its presence in the system changes what
Parker is *capable* of proposing and executing; it never changes *how*
a proposal becomes permitted action.

## 2. Module Responsibilities

A module may provide:

- **Tools** — capability surfaces reached through `ToolRegistry`/
  `ExecutionPipeline`, exactly as any other Tool is reached today
  (ADR-024 Section A, Rule 1).
- **Capabilities** — named units of "what this module can do," declared
  in its manifest (Section 5) and discoverable through the Tool Registry,
  mirroring the discovery model `docs/architecture/tool-registry.md`
  already specifies.
- **Adapters** — components that translate between Parker's internal
  contracts (`ExecutionRequest`, `ToolResult`, and similar) and an
  external system's own protocol or data shape, without themselves
  becoming a second execution path.
- **Integrations** — connections to external services or devices, always
  reached through the same Tool/ExecutionPipeline surface as any other
  capability, never through a private channel a module invents for
  itself.
- **Optional, read-only event subscriptions, if authorised.** A module
  MAY subscribe to `EventBus` for observability, under the exact
  discipline AD-012 and ADR-023 already impose on context providers, and
  only once ADR-024's own delivery-isolation precondition (Section C of
  that ADR, gap #50) is satisfied for that module (ADR-024 Section A,
  Rule 2; Section E, Rule 20).

## 3. What Modules May Not Do

Restated explicitly, as this document's own normative floor:

- **Modules do not bypass Trust.** Every module-originated action is
  evaluated by `PermissionEngine.evaluate`, reached only via
  `ExecutionPipeline.submit`, exactly like every other proposer.
- **Modules do not execute directly.** A module never calls `Tool.execute`
  itself, and never holds a live, invocable reference to a Tool outside
  the Execution Pipeline — the same boundary `tool-registry.md` already
  enforces for Planner and every other caller.
- **Modules do not write Memory directly.** Memory Runtime remains a
  context provider (AD-012); a module's only relationship to Memory is
  through whatever read interface Memory itself exposes, never a direct
  write path a module constructs on its own.
- **Modules do not mutate World Model directly, unless authorised through
  approved interfaces.** The same AD-012 boundary applies: a module may
  observe World Model state through its existing read operations, and may
  submit an observation only through whatever interface World Model's own
  specification authorises for that purpose — never through a module-owned
  mutation path.
- **Modules do not become planners.** Deciding what should happen next,
  and why, remains Planner Runtime's (and, upstream of it, Cognition's)
  role. A module may be *invoked as part of* a plan's execution; it may
  never generate, rank, or select Plan Candidates itself.
- **Modules do not own authority.** No module is ever granted implicit
  trust, self-approval, or a bypass of Identity/Permission evaluation
  (ADR-024 Section A, Rule 3; Section E, Rule 21).

## 4. Module Lifecycle (Conceptual)

Presented as a conceptual sequence, not a state machine with named Kotlin
states — that shaping is Module Contract Design's job (Section 11), not
this document's:

1. **Discovered.** Parker becomes aware a module exists (e.g. present in
   a known location, or announced through some future discovery
   mechanism). Discovery alone grants nothing.
2. **Described.** The module's manifest (Section 5) is read and
   validated — what it claims to provide, what permissions it requires,
   what compatibility constraints apply. A described module is still
   inert.
3. **Registered.** The module's declared capabilities are recorded
   against the Tool Registry (and, where applicable, the Resource
   Registry) — mirroring the existing registration model
   `tool-registry.md` already specifies for any Tool. Registration
   records what the module *could* do; it does not yet grant permission
   to do it.
4. **Enabled.** An explicit, attributable decision (an authenticated
   Principal's action, never the module's own) makes a registered
   module's capabilities reachable. This is the step at which the
   module's declared required permissions actually become live
   `PermissionEngine` evaluation criteria for its own future invocations.
5. **Invoked.** A capability the module provides is reached through the
   normal `ExecutionPipeline.submit` → `PermissionEngine.evaluate` →
   `Tool.execute`-equivalent path (Section 3). Invocation is per-action,
   not a standing grant — every invocation is still independently
   evaluated.
6. **Disabled.** The reverse of Enabled: an explicit, attributable
   decision makes a registered module's capabilities unreachable again,
   without un-registering it. A disabled module retains its registration
   record but nothing it declares may be invoked.
7. **Removed.** The module's registration itself is withdrawn. A removed
   module must be re-discovered, re-described, and re-registered before
   it can be enabled again — there is no "removed but still latently
   enabled" state.

No lifecycle step here grants authority by itself; every step only
changes what is *discoverable*, *knowable*, or *reachable*. Whether an
action a module's capability makes possible is actually permitted is
still, every time, the Permission Engine's decision alone.

## 5. Module Manifest Concept

A module must declare, conceptually (field shapes are Module Contract
Design's job, not this document's):

- **Module ID** — a stable identifier distinguishing this module from
  every other, mirroring how every other runtime-significant entity in
  this platform (`TaskId`, `AgentRunId`, `PrincipalId`) already has one.
- **Name** — a human-readable label, distinct from the ID, mirroring
  `Principal.displayName`'s existing precedent.
- **Version** — enabling compatibility and upgrade decisions (Section 5's
  own "compatibility requirements," below) to be made against a specific,
  declared version, not an assumed latest one.
- **Provided capabilities** — the set of capabilities (Section 2) this
  module claims to offer, described precisely enough for the Tool
  Registry's discovery model to catalogue them.
- **Required permissions** — the `PermissionAction`/`ResourceType`-shaped
  (or equivalent) set of permissions this module's capabilities will need
  evaluated at invocation time, declared up front so Enabling a module is
  an informed decision, not a blind one.
- **Tools exposed** — the specific Tool-equivalent surfaces this module
  registers with the Tool Registry, distinct from the broader
  "capabilities" claim above in the same way `ToolDescriptor` is already
  distinct from a Tool's own free-text description.
- **Event subscriptions, if any** — declared explicitly, since any
  subscription is subject to Section 2's own "if authorised" condition and
  ADR-024's delivery-isolation precondition; a module with no subscription
  need declare none.
- **Compatibility requirements** — whatever minimum platform version,
  dependency, or capability this module assumes is present, so Discovery
  and Description (Section 4) can reject an incompatible module before it
  is ever Registered.

## 6. Module Registration Boundary

A module connects to the rest of the platform only through already-
existing, already-governed surfaces — it introduces no second copy of any
of them:

- **Tool Registry.** A module's declared Tools and capabilities are
  registered here, using the same registration model every other Tool
  uses (`tool-registry.md`). No module-specific registry is introduced.
- **Permission Engine.** Every module-originated action is evaluated here,
  exactly as any other proposer's action is (Section 3). A module's
  manifest-declared required permissions inform what an Enabling decision
  is agreeing to, but the Permission Engine's own evaluation at invocation
  time is not replaced, cached, or pre-approved by that declaration.
- **Resource Registry.** Where a module's capability concerns a Resource,
  it is catalogued through the existing Resource Registry, mirroring
  `tool-registry.md`'s own "every registered Tool MUST also have a
  corresponding Resource Registry entry" invariant.
- **EventBus, if authorised.** A module may subscribe for observability
  only under Section 2's own condition and ADR-024's precondition; it
  never gains a private publish/subscribe channel of its own.
- **Memory and World Model, as read-only context providers, unless
  explicitly authorised otherwise.** A module consults Memory and World
  Model exactly as any other reader does, through their own existing read
  operations. Any exception to read-only access would itself require the
  same kind of explicit architectural authorisation ADR-023/ADR-024
  already require for a context provider's own event publication — it is
  not something a module manifest can request into existence
  unilaterally.

## 7. Security and Trust

Module access must remain permission-gated at every step, not only at
invocation:

- Every module is an ordinary `Principal`, resolved and evaluated by
  Identity Runtime like any other (Section 1).
- A module's declared required permissions (Section 5) are evaluated, not
  merely recorded, at Enable time and at every subsequent invocation.
- No module receives a permission it did not declare in its manifest, and
  declaring a permission does not itself grant it — the Permission
  Engine's evaluation is the sole authority for whether a specific
  invocation is permitted, exactly as AD-007 already requires for every
  other actor.
- A module that is Disabled or Removed (Section 4) loses reachability
  immediately; no invocation in flight is granted a grace period beyond
  what the Execution Pipeline's own existing cancellation semantics
  already provide for any request.

## 8. Local-First and Model-Independent Operation

- A module must not create a dependency on a specific cloud service,
  network endpoint, or always-on connectivity requirement unless that
  dependency is explicitly declared in its manifest (Section 5) and
  explicitly authorised at Enable time (Section 4). Local-first is
  Parker's default posture; a module does not get to quietly narrow it.
- A module must not assume, or require, a specific reasoning or model
  implementation sits behind any Planner- or Agent-originated request
  that eventually reaches it. Model independence (AD-010) applies to
  every seam in this platform a module might be invoked through, exactly
  as it already applies to `AgentStepSource`, `PlanDecision`,
  `MemoryPromotionPolicy`, and `WorldModelUpdatePolicy`.

## 9. Relationship to Future Plugin Loading

**"Module" is the architectural concept this document defines. "Plugin"
is an existing, unimplemented Volume 3 interface
(`docs/specifications/volume-03-core-interfaces/Plugin.md`;
`src/interfaces/Plugin.kt`, currently excluded from build scope) that
already names a specific shape for one — `manifest`/`initialise`/
`shutdown` — without yet being connected to Tool Registry, Permission
Engine, or any other runtime surface this document describes.**

This document does not decide that `Plugin.kt` is, unchanged, the module
loading and distribution mechanism — that decision, along with whatever
revision `Plugin.md` needs to align with Sections 4–6 above, belongs to a
future Module Contract Design pass (Section 11), mirroring exactly how
`AgentRuntimeSpecification.md` and `Agent.md`'s own relationship needed a
dedicated terminology clarification (Sprint 5 Cleanup) rather than being
assumed. What this document does establish is the direction of that
relationship: "module" names the concept (Sections 1–8); "plugin," if
that term is kept, would name one concrete implementation/distribution
mechanism for it — not a second, competing concept requiring its own
separate authority model.

## 10. Out of Scope

This document does not define, and Module Contract Design (Section 11) is
where each of the following belongs:

- Kotlin interfaces or type shapes for any concept named above.
- Class names, package layout, or module structure within `src/`.
- The dynamic loading implementation (how a module's code actually enters
  a running Parker process).
- Android integration specifics (Chapter 27) — a module framework
  consumer, not a reason to change anything above.
- Any actual module implementation, reference or otherwise.

## 11. Engineering Review — What Module Contract Design Must Resolve

A future Module Contract Design document, following PES-001 Stage 2A,
must settle:

- The concrete Kotlin shape of a Module interface and its manifest type
  (Section 5), and whether `Plugin.kt`/`Plugin.md` (Section 9) are
  revised into that shape, superseded by it, or kept as a distinct,
  narrower concept.
- The concrete registration mechanism connecting a described module to
  Tool Registry and Resource Registry (Section 6) — what "registration"
  actually calls, and what it returns.
- How Enable/Disable (Section 4) are actually triggered, by whom, and
  what record of that decision Auditability (AD-009) requires — this
  depends on gap #51's own durability boundary (ADR-024 Section D) for
  whether that record must survive a process restart.
- The precise precondition check gap #50 (ADR-024 Section C) requires
  before any module is granted an EventBus subscription — Module Contract
  Design cannot authorise a subscribing module ahead of that
  precondition being met.
- Whether, and how, a module's declared "required permissions" (Section
  5) map onto existing `PermissionAction`/`ResourceType` vocabulary
  (`action-mapping.md`) or require an extension to it.
- The exact compatibility-checking mechanism (Section 5's "compatibility
  requirements") — version comparison semantics are not decided here.
- Whether Memory or World Model ever need a module-specific read
  interface distinct from what existing callers already use, or whether
  today's interfaces are sufficient (Section 6).

## 12. Conclusion

**Parker is ready for Module Contract Design, on the basis of this
document together with ADR-024, once Module Contract Design itself is
scoped to resolve exactly the open items Section 11 names — not to
revisit Sections 1–9's own settled conceptual boundary.**

Architecture v2.0's runtime foundation is frozen and stable
(`ARCHITECTURE_V2_FROZEN_BASELINE.md`), pre-module readiness is verified
(gaps #49 and #48 resolved; #47, #50, and #51 each have a governing
architectural decision, per ADR-024), and this document now gives "module"
itself a settled definition, responsibility set, prohibition list,
conceptual lifecycle, manifest concept, registration boundary, security
posture, and local-first/model-independence constraint — every question
`PARKER_ENGINEERING_STANDARD.md` Stage 1 (Architecture) is meant to answer
before Stage 2A (Contract Design) begins. No Kotlin was written, no
Contract Design was begun, and no module was implemented by this document.
