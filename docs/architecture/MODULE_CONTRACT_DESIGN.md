# Module Contract Design

## Status

Design proposal, not yet reviewed or accepted. **This document is
contract design only.** No Kotlin is implemented, proposed as a diff, or
changed by it — every shape below is described in prose, not as a
`kotlin`-fenced signature block, precisely so nothing here can be
mistaken for an implementation. Neither `src/` nor `tests/` is touched.
`IMPLEMENTATION_HISTORY.md` and `IMPLEMENTATION_GAPS.md` are both
untouched. No module, module loading, plugin, or dynamic discovery
mechanism is implemented. No other document is modified.

### Why this unit exists

`docs/architecture/MODULE_FRAMEWORK_ARCHITECTURE.md` defined what a
Parker Module is, its responsibilities and prohibitions, its conceptual
lifecycle, its manifest concept, its registration boundary, its security
posture, and its relationship to Plugin — deliberately stopping short of
any field-level shape (its own Section 10, "Out of Scope"). This document
performs the field-level design pass that document deferred, so a future
implementation unit builds the Module Framework by implementing an
already-approved contract set, never by inventing one mid-Kotlin —
exactly the same relationship `MEMORY_CONTRACT_DESIGN.md` bears to
`MEMORY_RUNTIME_ARCHITECTURE.md`, and `PLANNER_RUNTIME_CONTRACT_DESIGN.md`
bears to Track D's own architecture-level design.

This document performs the contract minimalism review PES-001 and this
Sprint's own precedent require throughout, not as a final pass: eight
named candidates from this unit's own exclusion list are rejected
outright, and every contract proposed for inclusion is checked against a
concrete, cited need in `MODULE_FRAMEWORK_ARCHITECTURE.md` before it is
accepted.

## Review

Reviewed, in authority order:

1. `docs/architecture/parker-constitution.md`.
2. `docs/architecture/ARCHITECTURE_DECISIONS.md` — AD-007 (Permission
   Decisions Belong to the Permission Engine), AD-009 (Everything
   Important Is Auditable), AD-012 (Memory and World Model Are Context
   Providers), AD-013 (Specifications Define Contracts).
3. `docs/architecture/PARKER_ENGINEERING_STANDARD.md` (PES-001).
4. `docs/architecture/MODULE_FRAMEWORK_ARCHITECTURE.md` — the architecture
   this document implements as contracts and does not redefine.
5. `docs/adr/ADR-024-module-event-audit-durability-boundary.md` — the
   module-access boundary and the gap #47/#50/#51 preconditions this
   document's contracts must respect, especially for event subscription
   (Section 8, below).
6. `docs/architecture/ARCHITECTURE_V2_FROZEN_BASELINE.md` — the runtime
   foundation this contract set is built on top of, not a redesign of.
7. `docs/architecture/tool-registry.md`,
   `docs/specifications/volume-03-core-interfaces/ToolRegistry.md`,
   `src/interfaces/ToolRegistry.kt` — the existing registration model this
   document's own Module Registry contract must reuse, not duplicate.
8. `docs/specifications/volume-03-core-interfaces/Plugin.md`,
   `src/interfaces/Plugin.kt` (excluded from build scope) — the existing,
   unimplemented sibling concept this document's Section 10 addresses.
9. `docs/architecture/MEMORY_CONTRACT_DESIGN.md`,
   `docs/architecture/PLANNER_RUNTIME_CONTRACT_DESIGN.md` — this
   document's own direct precedent and required point of stylistic and
   structural symmetry.
10. `src/contracts/Identifiers.kt`, `src/contracts/PrincipalLifecycle.kt`,
    `src/contracts/AgentRunLifecycle.kt`,
    `src/contracts/PlannerSessionLifecycle.kt` — the identifier and
    lifecycle-transition shape every contract below must match in pattern.
11. `docs/architecture/IMPLEMENTATION_GAPS.md` #24 — Tool Registry's own,
    already-specified-but-not-yet-wired intent that registration and
    lifecycle changes be evaluated as a Permission Engine decision, a
    direct precedent for this document's own Module Registry lifecycle
    operations (Section 5, below).

---

## Constitutional Boundaries

Restated up front, identical in substance to `MODULE_FRAMEWORK_ARCHITECTURE.md`'s
own Sections 1–3, not re-derived differently here:

- **A module provides capability. It never owns authority.** No contract
  below grants a module implicit trust, self-approval, or a bypass of
  Identity/Permission evaluation.
- **A module never executes directly.** No contract below gives a module
  a live, invocable `Tool` reference outside `ExecutionPipeline`.
- **A module never plans.** No contract below generates, ranks, or
  selects a Plan Candidate.
- **A module never writes Memory or mutates World Model directly.** No
  contract below gives a module anything but the same read interfaces
  every other caller already uses.
- **A module is registered and governed through already-existing
  surfaces.** No contract below duplicates Tool Registry, Permission
  Engine, Resource Registry, or EventBus — each contract either reuses an
  existing type directly, or is a thin, module-specific record that is
  itself registered *with* one of those existing surfaces.

---

## Contract Minimalism Review — Summary

| Candidate | Determination |
| --- | --- |
| `ModuleId` | **Include.** Follows the established identifier pattern. |
| `ModuleDescriptor` | **Include.** The manifest, field-shaped. |
| `ModulePermissionRequirement` | **Include.** A minimal, two-field declaration record. |
| `ModuleConnectivityDeclaration` | **Include.** A small, closed enum — the only genuinely new concept this document introduces. |
| `ModuleStatus` | **Include**, narrowed to four values, not six. |
| `ModuleLifecycleTransitions` | **Include**, mirroring every other lifecycle-transitions precedent. |
| `ModuleRegistry` | **Include.** The single public interface. |
| `ModuleRuntime` | **Exclude — merge into `ModuleRegistry`.** No distinct responsibility identified. |
| A freestanding `ModuleCapability` type | **Exclude.** Capabilities are expressed entirely through `ModuleDescriptor.toolsExposed: List<ToolDescriptor>`. |
| A freestanding `ModuleAdapter`/`ModuleIntegration` type | **Exclude.** Both are descriptive labels for a `ToolDescriptor`-shaped registration, not a structurally distinct contract. |
| `PluginLoader` | **Exclude.** Dynamic loading is explicitly out of scope. |
| `DynamicModuleLoader` | **Exclude.** Same reason. |
| `RemoteModule` | **Exclude.** No cross-process or cross-machine module concept is authorised anywhere in `MODULE_FRAMEWORK_ARCHITECTURE.md`. |
| `Marketplace` | **Exclude.** A distribution/discovery-UI concept, not a runtime contract. |
| `CloudModule` | **Exclude.** Connectivity is a declaration on an ordinary module (`ModuleConnectivityDeclaration`), not a separate module category. |
| `ModuleMemoryAccess` | **Exclude.** Memory is reached through its own existing `MemoryStore` read interface, unchanged. |
| `ModuleWorldModelAccess` | **Exclude.** Same reasoning, for World Model. |

Net result: **seven required contracts** (`ModuleId`, `ModuleDescriptor`,
`ModulePermissionRequirement`, `ModuleConnectivityDeclaration`,
`ModuleStatus`, `ModuleLifecycleTransitions`, `ModuleRegistry`), and
**eleven candidates excluded outright**, each reasoned below.

---

## 1. Module Identity — `ModuleId`

- **Purpose.** Identifies one module across its entire lifecycle, from
  Registration through Removal.
- **Authorised by** `MODULE_FRAMEWORK_ARCHITECTURE.md` Section 5 ("Module
  ID — a stable identifier distinguishing this module from every other").
- **Required.** Yes. Every other long-lived entity this platform tracks
  has one (`PrincipalId`, `TaskId`, `AgentRunId`, `PlanningSessionId`,
  `MemoryId`) — a module is no exception.
- **What it represents.** A stable, caller-declared identity, not a
  runtime-minted one. This is a deliberate, precedent-consistent choice,
  not a break from pattern: a module exists independently of Parker
  before it is ever Discovered (`MODULE_FRAMEWORK_ARCHITECTURE.md` Section
  4), so, exactly as a `PrincipalId` is supplied by whatever registers a
  Principal rather than generated by `IdentityService`, a `ModuleId` is
  declared in the module's own `ModuleDescriptor` (Section 2, below) and
  validated for uniqueness by `ModuleRegistry` at Registration — it is
  never minted by Parker itself.
- **Shape.** A single, non-blank string value, validated at construction,
  matching `PrincipalId`/`ResourceId`/`MemoryId`'s identical established
  shape.
- **Lifecycle.** Declared once, in the manifest; checked for uniqueness at
  Registration (duplicate registration fails deterministically, mirroring
  `InMemoryIdentityService.register`'s existing "already registered"
  precedent); retained unchanged through Enable/Disable/Removal.

## 2. Module Descriptor / Manifest — `ModuleDescriptor`

- **Purpose.** What a module declares about itself before it can be
  Registered — the field-level shape of
  `MODULE_FRAMEWORK_ARCHITECTURE.md` Section 5's "manifest concept."
- **Naming.** `ModuleDescriptor`, not `ModuleManifest` — chosen for
  structural symmetry with `ToolDescriptor` (the shape `ToolRegistry`
  already catalogues a Tool by), since `ModuleRegistry` catalogues a
  module the same way. `Plugin.md`'s own, pre-existing `PluginManifest`
  name is a related but not-yet-reconciled sibling; this document does
  not rename or restructure it (Section 10).
- **Authorised by** `MODULE_FRAMEWORK_ARCHITECTURE.md` Section 5, field by
  field.

**Required fields:**

- **`moduleId: ModuleId`** — Section 1, above.
- **`name`** — a non-blank string, distinct from `moduleId`, mirroring
  `Principal.displayName`'s existing precedent (Section 5, "Name").
- **`version`** — a non-blank string. Kept as a plain string, not a
  structured version type, because `MODULE_FRAMEWORK_ARCHITECTURE.md`
  Section 11 explicitly defers "the exact compatibility-checking
  mechanism... version comparison semantics are not decided here" — this
  document does not decide it either, and inventing a structured version
  type now, with no comparison algorithm to use it, would be exactly the
  kind of premature structure this review exists to catch.
- **`toolsExposed: List<ToolDescriptor>`** — the module's declared Tools,
  reusing the existing `ToolDescriptor` type directly rather than
  inventing a module-specific equivalent. See Section 3, below, for why
  this single field also satisfies "provided capabilities" without a
  second, freestanding field.
- **`requiredPermissions: List<ModulePermissionRequirement>`** — Section
  6, below.
- **`eventSubscriptions: List<EventType>`**, defaulted to empty — Section
  8, below. Reuses the existing `EventType` value type directly; no
  module-specific wrapper is introduced.
- **`connectivityDeclaration: ModuleConnectivityDeclaration`** — Section
  9, below.
- **`minimumPlatformVersion: String?`**, optional — the one, minimal piece
  of "compatibility requirements" (Section 5's own list) this document
  commits to. A richer dependency or feature-flag model is explicitly not
  designed here, for the same reason `version` above is kept as a plain
  string.

**What it must not carry:** anything resembling a permission *grant*, an
event subscription *activation*, or a live reference to a `Tool`,
`EventBus`, `MemoryStore`, or `WorldModel` instance. A `ModuleDescriptor`
is a declaration, read by `ModuleRegistry` at Registration time; it never
itself performs an action.

- **Lifecycle.** Constructed once, before Registration; supplied whole to
  `ModuleRegistry.register` (Section 5); retained by the registry for the
  life of the module's registration, re-readable via lookup (Section 5).
- **Future extensibility.** Additional optional fields (a richer
  compatibility model, additional metadata) can be added additively,
  exactly as every other descriptor/record type in this codebase already
  allows.

## 3. Module Capability — Distinguishing Capability, Tool, Adapter, and Integration

`MODULE_FRAMEWORK_ARCHITECTURE.md` Section 2 names four things a module
may provide: tools, capabilities, adapters, and integrations. This
document's contract-level finding: **only one of the four needs its own
field or type. The other three are the same contract, described
differently in prose.**

- **Tool** is the one concrete, registrable unit: `ToolDescriptor`, already
  specified by `tool-registry.md` and already implemented
  (`src/interfaces/ToolRegistry.kt`). A module's declared Tools populate
  `ModuleDescriptor.toolsExposed` (Section 2) directly.
- **Capability** is the umbrella architectural term
  `MODULE_FRAMEWORK_ARCHITECTURE.md` Section 2 itself uses — "named units
  of 'what this module can do,' declared in its manifest... discoverable
  through the Tool Registry." At the contract level, every capability a
  module offers is discoverable precisely because it is registered as a
  `ToolDescriptor`. No concrete need has been identified for a
  freestanding `capabilities: List<String>` (or similar) field distinct
  from `toolsExposed` — one would either duplicate the same information in
  a second, unstructured shape, or introduce a capability that has no
  corresponding registrable Tool, which contradicts Section 2's own "a
  module's primary role is to be reached as a Tool." **Excluded as a
  separate field.**
- **Adapter** and **Integration** are descriptive *purposes* a
  `ToolDescriptor`-shaped registration serves (translating a protocol;
  connecting to an external system), not structurally different
  contracts. `MODULE_FRAMEWORK_ARCHITECTURE.md` Section 2 itself states
  both are "always reached through the same Tool/ExecutionPipeline
  surface." Introducing `ModuleAdapter`/`ModuleIntegration` types now,
  with no field identified that either would need and a plain
  `ToolDescriptor` would not, would be inventing structure to match a
  taxonomy, not a concrete requirement. **Excluded as separate types.**
  A future unit may choose to tag a `ToolDescriptor`'s purpose informally
  (naming convention, documentation, or an optional free-text field on
  `ToolDescriptor` itself, which is outside this document's own scope to
  propose) if distinguishing them in tooling or documentation becomes a
  concrete need.

## 4. Module Lifecycle — `ModuleStatus` and `ModuleLifecycleTransitions`

`MODULE_FRAMEWORK_ARCHITECTURE.md` Section 4 names seven conceptual
steps: Discovered, Described, Registered, Enabled, Invoked, Disabled,
Removed. This document narrows that to the states `ModuleRegistry`
itself must actually track.

**Is a lifecycle enum required? Yes — narrowed to four values, not
six or seven.**

- **Discovered and Described are not tracked `ModuleStatus` values.**
  Both are, by `MODULE_FRAMEWORK_ARCHITECTURE.md` Section 4's own words,
  stages before a module is "still inert" and before `ModuleRegistry` has
  any record of it at all. Dynamic discovery is explicitly out of this
  unit's scope (and this document's own instructions), so nothing in this
  contract set ever needs to represent, query, or transition into or out
  of either state through `ModuleRegistry` — they remain informal,
  pre-registry concepts belonging to whatever future discovery mechanism
  produces a validated `ModuleDescriptor` in the first place. Reifying
  them as enum values `ModuleRegistry` never actually stores or checks
  would be exactly the kind of speculative structure this review exists
  to reject.
- **Invoked is not a `ModuleStatus` value.** Invocation is a transient
  action performed while a module is `ENABLED` (reached through
  `ExecutionPipeline`/`ToolRegistry`, Section 7, below), not a status a
  module persists in. No module is ever "in the Invoked state" the way it
  is "in the Enabled state."
- **`ModuleStatus` has exactly four values: `REGISTERED`, `ENABLED`,
  `DISABLED`, `REMOVED`** — the four states `ModuleRegistry` itself
  actually assigns, stores, and transitions between.

**`ModuleLifecycleTransitions`** — required, mirroring
`PrincipalLifecycleTransitions`/`AgentRunLifecycleTransitions`/
`PlannerSessionLifecycleTransitions`'s identical existing precedent (every
other tracked lifecycle in this codebase has a matching transitions
validator; a module's should not be the exception):

```
REGISTERED -> {ENABLED, REMOVED}
ENABLED    -> {DISABLED}
DISABLED   -> {ENABLED, REMOVED}
REMOVED    -> {}  (terminal)
```

`DISABLED -> ENABLED` (re-enabling) is a legal edge: Disable exists
precisely to be a reversible, non-destructive step distinct from Remove
(`MODULE_FRAMEWORK_ARCHITECTURE.md` Section 4, "the reverse of Enabled...
without un-registering it"). `REMOVED` has no outgoing edge —
re-introducing a removed module requires a new Registration (a new
`ModuleDescriptor` submission), per Section 4's own "must be
re-discovered, re-described, and re-registered."

## 5. Module Registry — `ModuleRegistry`

**Is `ModuleRegistry` the single public module interface? Yes.**

Mirroring `MEMORY_CONTRACT_DESIGN.md`'s own central minimalism finding: no
subsystem in this codebase has two public interfaces where one already
suffices (`ToolRegistry`, `IdentityService`, `MemoryStore`, `WorldModel`,
`PlannerRuntime`, `AgentRunCommandChannel` are each their subsystem's one
public surface). `ModuleRegistry` is Parker's one public contract for the
Module Framework.

**Is a separate `ModuleRuntime` necessary? No — excluded, merged into
`ModuleRegistry`, for the same reason `MemoryRuntime` was excluded from
Memory's contract set.** No distinct responsibility has been identified
that `ModuleRegistry` cannot already express. "The Module Runtime" (or
"the Module Framework") remains the correct informal name for the
subsystem as a whole; it does not require a second formal interface.

**Scope boundary: `ModuleRegistry` manages lifecycle and lookup only. It
never invokes a module's capability itself.** Reaching a module's Tool is
`ExecutionPipeline`/`ToolRegistry`'s job (Section 7, below), exactly as
`MODULE_FRAMEWORK_ARCHITECTURE.md` Section 6 requires — `ModuleRegistry`
is not a second execution path.

**Required public members** (described in prose; no signature is
proposed as Kotlin):

- **Register** a `ModuleDescriptor`, returning its `ModuleId`. Fails
  deterministically on a duplicate `moduleId`, mirroring
  `InMemoryIdentityService.register`'s and `InMemoryPlannerRuntime.plan`'s
  identical existing "already registered/submitted" precedent. Internally,
  Registration is the point at which each of the module's declared
  `toolsExposed` entries is registered with `ToolRegistry` (Section 7) —
  `ModuleRegistry` is a caller of `ToolRegistry`'s own existing
  registration operation, not a replacement for it.
- **Enable** a registered module, given its `ModuleId` and the
  requesting Principal's identity. Transitions `REGISTERED -> ENABLED` or
  `DISABLED -> ENABLED` (Section 4). This is the step that makes the
  module's registered Tools reachable through `ToolRegistry`'s own
  discovery surface — it does not itself evaluate or pre-approve any
  individual future invocation (Section 6, below).
- **Disable** an enabled module, given its `ModuleId` and the requesting
  Principal's identity. Transitions `ENABLED -> DISABLED`. Makes the
  module's registered Tools unreachable again without removing its
  registration.
- **Remove** a registered module, given its `ModuleId` and the requesting
  Principal's identity. Transitions `REGISTERED -> REMOVED` or
  `DISABLED -> REMOVED`. Withdraws the module's registration and its
  `toolsExposed` entries from `ToolRegistry` entirely.
- **Look up** a module's `ModuleDescriptor` by `ModuleId`, returning
  `null` if none is registered.
- **Look up** a module's current `ModuleStatus` by `ModuleId`, returning
  `null` if none is registered.
- **List** every registered module (or its `ModuleId`s) known to the
  registry.

Lookup and listing are part of `ModuleRegistry`'s own core public
interface, not an implementation-specific inspection method outside it
(unlike `InMemoryPlannerRuntime.getSessionStatus`/
`InMemoryAgentRuntime.getAgentRun`'s "observability only" precedent) —
the difference is deliberate: a registry's fundamental job includes
lookup for whatever else needs to discover what is available (a future
Tool Registry consumer, a future administrative surface), whereas an
Agent Run's or Planning Session's own status accessor exists only for
that one runtime's own debugging convenience.

**Enable/Disable/Remove and Permission Engine evaluation — a disclosed
design decision, not a silent assumption.** `docs/architecture/IMPLEMENTATION_GAPS.md`
#24 already records that Tool Registry's own specification
(`tool-registry.md`) requires registering a Tool, or changing its
lifecycle state, to be evaluated as a `PermissionAction.CONTROL` decision
— a requirement `InMemoryToolRegistry` does not yet enforce, a disclosed,
known scope reduction. This document adopts the identical rule for
`ModuleRegistry`'s own Enable/Disable/Remove: each is, architecturally, a
`PermissionAction.CONTROL`-equivalent decision requiring evaluation, not
an operation any caller may invoke unconditionally merely by knowing a
`ModuleId`. Whether a first implementation actually wires this
evaluation in, or defers it exactly as `InMemoryToolRegistry` currently
does, is an implementation-unit decision this document does not make —
but it must be disclosed as a scope reduction if deferred, mirroring gap
#24's own treatment, not silently assumed to be either present or absent.

- **Lifecycle.** `ModuleRegistry` itself has no lifecycle of its own
  (mirroring `MemoryStore`'s identical "no internal state machine of its
  own beyond whatever a given record moves through") — it is the
  boundary each module's own `ModuleStatus` transitions happen behind.
- **Future extensibility.** Additional lookup operations (e.g. filtered
  listing by `ModuleStatus` or by declared capability) can be added
  additively without changing the core register/enable/disable/remove
  surface.

## 6. Permissions — Declared, Never Granted; Modules Never Self-Authorise

- **`ModulePermissionRequirement`** — a minimal, two-field record: a
  `PermissionAction` and a `ResourceType` (both existing types, reused
  directly, not re-specified), naming one permission a module's
  capabilities will need evaluated at invocation time. A module's
  `requiredPermissions` (Section 2) is a list of these.
- **Declaring a `ModulePermissionRequirement` grants nothing.** It informs
  whatever Principal decides to Enable the module (Section 5) what that
  decision is agreeing to expose the possibility of — it is not itself an
  evaluation, and it does not pre-approve any future invocation.
- **Enabling a module grants nothing either.** Enable (Section 5) makes a
  module's Tools *reachable*; it does not make any specific future action
  *permitted*. Every subsequent invocation of a module's Tool is
  independently evaluated by `PermissionEngine.evaluate`, exactly as any
  other `ExecutionRequest` is (Section 7, below) — `ModulePermissionRequirement`
  is consulted only as informative context for whoever decides to Enable,
  never cached, reused, or substituted for that per-invocation evaluation.
- **A module never self-authorises.** No contract in this document gives
  a module a method, field, or return value that grants a permission, to
  itself or to anything else. Every permission-relevant decision remains
  the Permission Engine's alone (AD-007), exactly as
  `MODULE_FRAMEWORK_ARCHITECTURE.md` Section 3 and Section 7 already
  require.

## 7. Tool Registration — Without Bypassing Permission Engine or Execution Pipeline

- A module's `toolsExposed` (Section 2) are registered with `ToolRegistry`
  by `ModuleRegistry` itself, at Registration time (Section 5), using
  `ToolRegistry`'s own existing registration operation — no second
  registration path, and no module-specific registry, is introduced.
  `ModuleRegistry` is a caller of `ToolRegistry`, exactly as
  `InMemoryToolRegistry` is already itself a caller of
  `InMemoryResourceRegistry` (`docs/architecture/IMPLEMENTATION_GAPS.md`
  #22's own precedent for one registry depending on another without
  inventing new architecture).
- **Invocation never touches `ModuleRegistry`.** Once a module's Tool is
  registered and the module is `ENABLED`, reaching that Tool is
  identical, in every respect, to reaching any other Tool: a caller
  constructs an `ExecutionRequest`, `ExecutionPipeline.submit` calls
  `PermissionEngine.evaluate`, and only on a permitted outcome does
  `Tool.execute` run, via `ToolInvocationBinding`, exactly as
  `DefaultExecutionPipeline` already does for every existing Tool. A
  module's Tool is not a special case reached through a different path.
- **This is the entire tool-registration contract.** No new field,
  method, or type beyond reusing `ToolDescriptor` (Section 2) and calling
  `ToolRegistry`'s existing registration operation (Section 5) is
  required.

## 8. Event Subscription — Declared Now, Activated Later

**Is event subscription part of the initial contract, or deferred? Both,
precisely split between declaration and activation, per ADR-024.**

- **The declaration is part of the initial contract.**
  `ModuleDescriptor.eventSubscriptions: List<EventType>` (Section 2)
  exists from the first version of this contract set, so a module can
  state its intended subscriptions honestly, and so `ModuleRegistry`'s
  own validation logic has something concrete to check a request against,
  rather than discovering the need for this field only once a module
  actually needs to subscribe.
- **The activation — a live `EventBus.subscribe` call on the module's
  behalf — is deferred**, per ADR-024 Section A (Rule 2, Rule 4) and
  Section E (Rule 20): a module may only be granted a live subscription
  once ADR-024's own gap #50 precondition (per-subscriber delivery
  isolation) is satisfied. `ModuleRegistry` does not itself manage
  subscriptions; if and when activation is authorised, it flows through
  `EventBus.subscribe` directly, with the module's own `PrincipalId` as
  `subscriberPrincipalId` — no new subscription-management surface is
  introduced on `ModuleRegistry`.
- **A module declaring a non-empty `eventSubscriptions` list is not,
  by that declaration alone, entitled to receive anything.** Enable
  (Section 5) makes a module's Tools reachable; it does not, by itself,
  activate any declared subscription. Whether Enable ever implies
  subscription activation, once gap #50 is resolved, is left to a future
  Module Contract Design revision or implementation unit — not decided
  here, consistent with ADR-024's own "left to whichever future unit
  first proposes a module-subscribing module."

## 9. Local-First and Cloud Declaration — `ModuleConnectivityDeclaration`

- **Required.** `MODULE_FRAMEWORK_ARCHITECTURE.md` Section 8 requires a
  module to declare, and be explicitly authorised for, any dependency
  beyond Parker's local-first default. This document gives that
  requirement a shape: a small, closed enum with exactly three values —
  `LOCAL_ONLY`, `CLOUD_CAPABLE`, `CLOUD_REQUIRED`.
  - `LOCAL_ONLY` — the module makes no network dependency claim; Parker's
    default local-first posture is unaffected.
  - `CLOUD_CAPABLE` — the module may use a network dependency but can
    operate, at reduced capability, without one.
  - `CLOUD_REQUIRED` — the module cannot function at all without a
    declared network dependency.
- **This is the one genuinely new concept this document introduces** — no
  existing type in this codebase represents connectivity posture, unlike
  every other manifest field, which reuses an existing type
  (`ToolDescriptor`, `PermissionAction`/`ResourceType`, `EventType`).
- **A `CLOUD_CAPABLE`/`CLOUD_REQUIRED` declaration is not itself an
  authorisation.** Exactly as a declared `ModulePermissionRequirement`
  (Section 6) is only ever consulted, never self-executing, a module
  claiming `CLOUD_REQUIRED` must still be explicitly authorised for that
  dependency at Enable time (Section 5) — Registration alone does not
  grant it. This document does not shape the specific mechanism by which
  "explicitly authorised" is enforced beyond noting it is checked at
  Enable, the same point every other permission-adjacent declaration is
  consulted.
- **No separate `CloudModule` type is required or introduced** — see
  Section 11 (Minimalism Review). Connectivity is a declaration on an
  ordinary module, not a distinct module category.

## 10. Relationship to Plugin

**Plugin remains a later distribution/loading mechanism. It is not
implemented, extended, or restructured by this contract.**

Every contract in this document is deliberately named `Module*`, not
`Plugin*`, for the same reason `MODULE_FRAMEWORK_ARCHITECTURE.md` Section
9 gives: "module" names the concept this document gives a contract shape
to; "plugin" (`Plugin.md`, `src/interfaces/Plugin.kt`, currently excluded
from build scope) already names one candidate, unimplemented
distribution/loading shape (`manifest`/`initialise`/`shutdown`) for a
related concept, without being connected to any of the registration,
lifecycle, or permission machinery this document defines.

This document does not decide whether `Plugin.kt` is revised to implement
`ModuleDescriptor`/`ModuleRegistry`, superseded by them, or kept as a
narrower, later concept describing only *how* a module's code enters a
running process. That decision — along with `PluginManifest`'s own
relationship to `ModuleDescriptor` (Section 2) — belongs to whichever
future unit actually proposes dynamic module loading, which this
document's own instructions explicitly exclude.

## 11. Minimalism Review

Every candidate this unit's own instructions named for exclusion is
addressed, with a concrete reason, not merely asserted:

- **`ModuleRuntime`** — excluded (Section 5): no distinct responsibility
  beyond `ModuleRegistry` has been identified, mirroring `MemoryRuntime`'s
  identical exclusion in `MEMORY_CONTRACT_DESIGN.md`.
- **`PluginLoader`** — excluded: dynamic loading is explicitly out of
  this unit's scope (Section 10; this unit's own "do not implement
  dynamic discovery" instruction).
- **`DynamicModuleLoader`** — excluded, for the identical reason.
- **`RemoteModule`** — excluded: nothing in
  `MODULE_FRAMEWORK_ARCHITECTURE.md` anticipates a module executing on a
  different process or machine; inventing that concept now would be
  speculative structure with no cited need.
- **`Marketplace`** — excluded: a distribution or discovery *user
  experience* concept, not a runtime contract; no operation this document
  defines requires it.
- **`CloudModule`** — excluded (Section 9): connectivity is a declaration
  on an ordinary `ModuleDescriptor`, not a separate module category —
  introducing a distinct type would imply modules come in structurally
  different kinds, which nothing here supports.
- **`ModuleMemoryAccess`** — excluded (Constitutional Boundaries, above;
  `MODULE_FRAMEWORK_ARCHITECTURE.md` Section 6): a module reads Memory
  through `MemoryStore`'s own existing interface, unchanged, exactly as
  any other reader does — no module-specific wrapper is needed.
- **`ModuleWorldModelAccess`** — excluded, identical reasoning, for World
  Model.

Beyond the named list, this document's own review additionally excluded:
a freestanding `ModuleCapability` type and separate `ModuleAdapter`/
`ModuleIntegration` types (Section 3); `Discovered`/`Described`/`Invoked`
as tracked `ModuleStatus` values (Section 4); and a structured `version`
or dependency-graph type for compatibility (Section 2) — each with its
own reasoning above, not merely bundled into this summary.

## 12. Self-Traceability Review

Every proposed contract, traced to its authorising section in
`MODULE_FRAMEWORK_ARCHITECTURE.md`:

| Contract | Authorised by (`MODULE_FRAMEWORK_ARCHITECTURE.md`) |
| --- | --- |
| `ModuleId` | Section 5, "Module ID" |
| `ModuleDescriptor` | Section 5 (manifest concept), all named fields |
| `ModulePermissionRequirement` | Section 5 ("Required permissions"); Section 7 ("Security and Trust") |
| `ModuleConnectivityDeclaration` | Section 8 ("Local-First and Model-Independent Operation") |
| `ModuleStatus` | Section 4 (Module Lifecycle), narrowed per Section 4 of this document |
| `ModuleLifecycleTransitions` | Section 4 (Module Lifecycle) |
| `ModuleRegistry` | Section 1 ("capability provider... reached as a Tool"); Section 6 (Module Registration Boundary); Section 4 (lifecycle steps) |

No contract in this document introduces a concept
`MODULE_FRAMEWORK_ARCHITECTURE.md` did not already anticipate at the
architectural level. Every exclusion in Section 11 is traceable to that
same document's own Section 10 (Out of Scope) or to this unit's explicit
exclusion list, not to an undocumented judgment call.

---

## Engineering Review

**Architectural consistency.** Every included contract traces to a named
`MODULE_FRAMEWORK_ARCHITECTURE.md` section (Section 12, above). Nothing
here introduces a concept that document did not already name at the
concept level.

**Model independence.** No contract in this document assumes a specific
reasoning or model implementation sits behind any module-originated
request, consistent with AD-010 and
`MODULE_FRAMEWORK_ARCHITECTURE.md` Section 8.

**Minimalism.** Performed throughout, not as an afterthought: eleven
candidates are excluded outright (Section 11), three additional
candidates were excluded on this document's own initiative (`ModuleCapability`,
`ModuleAdapter`/`ModuleIntegration`, and the narrower `ModuleStatus`), and
only one genuinely new concept (`ModuleConnectivityDeclaration`) is
introduced — every other required contract reuses an existing type or
mirrors an already-established pattern exactly.

**Traceability.** Every required contract's authorising section is named
in Section 12. Every exclusion's reasoning is stated in place, not merely
tabulated.

**Consistency with ADR-024.** Section 8's split between declared and
activated event subscription is checked directly against ADR-024 Section
A (Rules 2, 4) and Section E (Rule 20); no contract here authorises a live
module subscription ahead of gap #50's own precondition.

**Open contract questions.** None remain unresolved at the architecture
level this document is responsible for. Three items are explicitly left
to a future implementation or Contract Design revision, each already
disclosed as such rather than silently assumed: whether Enable/Disable/
Remove's Permission Engine evaluation is wired in immediately or deferred
as a disclosed scope reduction (Section 5, mirroring gap #24); the exact
compatibility/version-comparison mechanism (Section 2); and whether Enable
ever implies event-subscription activation once gap #50 is resolved
(Section 8). None of these blocks a first implementation from proceeding
against the contracts this document approves — each is a bounded,
disclosed follow-up, not an open design question standing in front of
implementation.

---

**Module Framework is ready for implementation.**
