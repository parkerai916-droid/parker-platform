# Sprint 6, Track A, Unit M1 — Post-Implementation Review

## Module Registry Runtime

## 1. Summary

Implemented `ModuleRegistry`/`InMemoryModuleRegistry`, the first concrete
realisation of Parker's Module Framework, exactly as defined by
`docs/architecture/MODULE_CONTRACT_DESIGN.md` (the accepted Contract
Design) and bounded by `docs/architecture/MODULE_FRAMEWORK_ARCHITECTURE.md`,
`docs/architecture/ARCHITECTURE_V2_FROZEN_BASELINE.md`, and PES-001. No
architecture was redesigned, no contract was modified, and no plugin,
module loading, discovery, dependency-injection framework, Home
Assistant, Weather, or Gmail integration was implemented — all five were
explicitly out of this Unit's scope and none is present in the diff.

## 2. Scope

**In scope, and done:**

- `src/contracts/Module.kt`: `ModuleId`, `ModuleConnectivityDeclaration`,
  `ModulePermissionRequirement`, `ModuleDescriptor`, `ModuleStatus`,
  `ModuleLifecycleTransitions` — the seven contracts Contract Design's own
  Minimalism Review Summary approved for inclusion, field-by-field, no
  more and no fewer.
- `src/interfaces/ModuleRegistry.kt`: the single public interface
  (register/enable/disable/remove/lookup), with no separate `ModuleRuntime`.
- `src/runtime/InMemoryModuleRegistry.kt`: the first in-memory
  implementation — duplicate-`moduleId` rejection, lifecycle enforcement
  via `ModuleLifecycleTransitions`, Tool Registry/Resource Registry wiring
  at Registration, and lookup.
- `tests/runtime/InMemoryModuleRegistryTest.kt`: 23 tests covering
  registration, every legal and illegal lifecycle edge this Unit's own
  instructions named, lookup, and two constitutional-boundary assertions.
- `docs/implementation/IMPLEMENTATION_HISTORY.md`: a new entry for this
  Unit.
- `docs/architecture/IMPLEMENTATION_GAPS.md`: a new entry (#52) recording
  the genuine interpretive gaps this implementation surfaced.

**Explicitly out of scope, and not done, per this Unit's own
instructions:**

- No plugin, module loading, or module discovery was implemented.
  `src/interfaces/Plugin.kt` is untouched and remains excluded from the
  build (`build.gradle.kts`'s exclusion list is unchanged).
- No dependency-injection framework was introduced. `InMemoryModuleRegistry`
  takes `ToolRegistry`/`ResourceRegistry` as plain constructor parameters,
  the same pattern every other `InMemory*` runtime class already uses
  (e.g. `InMemoryToolRegistry(resourceRegistry)`) — this is not the kind
  of "dependency injection" the instruction excludes.
- No Home Assistant, Weather, or Gmail integration was implemented or
  referenced.
- No live `PermissionEngine` gating of `enable`/`disable`/`remove` was
  wired in — deferred exactly as Contract Design's own Section 5 left
  open, mirroring gap #24's identical, pre-existing treatment for Tool
  Registry.
- No architecture document (`MODULE_FRAMEWORK_ARCHITECTURE.md`,
  `ARCHITECTURE_V2_FROZEN_BASELINE.md`, PES-001, the Parker Constitution,
  any ADR) was modified.
- No field, method, or type in `MODULE_CONTRACT_DESIGN.md`'s seven
  approved contracts was changed, renamed, or extended beyond what
  Contract Design itself specified.

## 3. What Was Built

### 3.1 Contracts (`src/contracts/Module.kt`)

Every type matches Contract Design's own field list exactly: `ModuleId`
(blank-rejecting, caller-declared); `ModuleDescriptor` (`moduleId`, `name`,
`version`, `toolsExposed: List<ToolDescriptor>`, `requiredPermissions`,
`connectivityDeclaration`, `eventSubscriptions` defaulted empty, optional
`minimumPlatformVersion`); `ModulePermissionRequirement`
(`PermissionAction`/`ResourceType` pair); `ModuleConnectivityDeclaration`
(`LOCAL_ONLY`/`CLOUD_CAPABLE`/`CLOUD_REQUIRED`); `ModuleStatus` (exactly
`REGISTERED`/`ENABLED`/`DISABLED`/`REMOVED`, narrowed from
`MODULE_FRAMEWORK_ARCHITECTURE.md`'s seven conceptual steps per Contract
Design Section 4's own reasoning); and `ModuleLifecycleTransitions`
(`REGISTERED -> {ENABLED, REMOVED}`, `ENABLED -> {DISABLED}`,
`DISABLED -> {ENABLED, REMOVED}`, `REMOVED -> {}`), following the identical
adjacency-map/`isValidTransition`/`requireValidTransition` pattern already
established by `PrincipalLifecycleTransitions` and
`ToolLifecycleTransitions`.

### 3.2 Interface (`src/interfaces/ModuleRegistry.kt`)

`register`/`enable`/`disable`/`remove`/`getModuleDescriptor`/
`getModuleStatus`/`listModules`. Error handling was a genuine Kotlin-shape
decision Contract Design deliberately left open (it describes behaviour
in prose, not signatures): rather than inventing a new sealed
Accepted/Rejected outcome type, this Unit chose the throw-based pattern
`IdentityService.register`/`.updateStatus` and
`ToolRegistry.setLifecycleState` already use — `IllegalArgumentException`
for a duplicate `moduleId` or an illegal lifecycle edge,
`NoSuchElementException` for an unknown `moduleId`. This is the more
minimal choice (no new sealed type) and the one Contract Design's own
Section 1 explicitly cited as the precedent for duplicate-registration
handling.

### 3.3 Runtime (`src/runtime/InMemoryModuleRegistry.kt`)

`register` checks for a duplicate `moduleId` first, then — for each
declared Tool — registers a backing `Resource` (`ResourceType.TOOL`,
owned nominally by `PrincipalId(moduleId.value)`, `ResourceSensitivity.PUBLIC`)
and registers the `ToolDescriptor` itself with the injected `ToolRegistry`,
tracking the resulting `ToolLifecycleState` locally (Tool Registry exposes
no way to read this back later). `enable`/`disable` drive every tracked
Tool between `REGISTERED`/`DISABLED` and `ENABLED` alongside the module's
own status transition. `remove` drives every tracked Tool to `REMOVED` via
the shortest legal path from its current tracked state (`REGISTERED` is
three hops: `ENABLED -> DISABLED -> REMOVED`; `DISABLED` is one hop).
Lookup operations are plain, `Mutex`-guarded map reads.

### 3.4 Tests (`tests/runtime/InMemoryModuleRegistryTest.kt`, 23 tests)

Registration (6): success + status, duplicate rejection, no-tools module,
Tool Registry wiring proof (registered but not yet resolvable),
Tool-descriptor conflict rejection, exact-duplicate (`AlreadyRegistered`)
rejection, and the disclosed multi-tool non-atomicity case. Enable (4):
success + resolvability, unregistered-`moduleId` throw, already-`ENABLED`
illegal transition, and the disclosed no-Permission-Engine-check
assertion. Disable (2): success + unresolvability, never-enabled illegal
transition. Re-enable (1): a full disable-then-enable round trip. Remove
(5): from `REGISTERED`, from `DISABLED`, illegal directly from `ENABLED`,
illegal when already `REMOVED`, unregistered-`moduleId` throw. Lookup (2):
`null` for unregistered, `listModules` including a `REMOVED` module.
Constitutional boundaries (2): `requiredPermissions` returned unchanged by
`enable` (no automatic grant), and a module's Tool reachable only through
`ToolRegistry.resolve`, never through any `ModuleRegistry` method.

## 4. Testing

Static count only — no working Kotlin/Gradle toolchain was available in
this session's sandbox, and Android Studio verification is Human
authority per PES-001. Prior confirmed/unchanged total: 418/418
(Pre-Module Readiness Unit 3). This Unit adds 23 new tests, 0 removed.
Expected total: **441/441** — an arithmetic projection, not a verified
run, disclosed as such in `IMPLEMENTATION_HISTORY.md`'s own entry for this
Unit and not to be treated as authoritative until confirmed in Android
Studio.

## 5. Self-Traceability Review

Every contract in `src/contracts/Module.kt` and every operation in
`src/interfaces/ModuleRegistry.kt` traces directly to
`MODULE_CONTRACT_DESIGN.md`'s own numbered sections (1 for `ModuleId`, 2
for `ModuleDescriptor`, 3 for the capability/tool merge reflected in
`toolsExposed`, 4 for `ModuleStatus`/`ModuleLifecycleTransitions`, 5 for
`ModuleRegistry`'s member list and scope boundary, 6 for
`ModulePermissionRequirement`'s declared-not-granted semantics, 7 for the
Tool Registry wiring, 9 for `ModuleConnectivityDeclaration`). Nothing in
this Unit introduces a concept Contract Design did not already approve;
every implementation-level decision this Unit itself had to make (error
handling shape, Resource-ownership scheme, lifecycle-state tracking) is
disclosed in Section 6 below and in `IMPLEMENTATION_GAPS.md` #52, not
silently assumed.

## 6. Interpretive Decisions Disclosed (not defects)

- **Error-handling shape** (Section 3.2): throw-based, matching
  `IdentityService`/`ToolRegistry` precedent, since Contract Design left
  this Kotlin-level decision open.
- **Resource ownership for module-exposed Tools**: `PrincipalId(moduleId.value)`
  is used as a nominal owner, without the module being a verified,
  `IdentityService`-registered Principal. Disclosed in `IMPLEMENTATION_GAPS.md`
  #52.
- **Resource sensitivity default**: `ResourceSensitivity.PUBLIC` for every
  module-exposed Tool's backing Resource, since neither `ModuleDescriptor`
  nor `ToolDescriptor` carries a sensitivity field. Disclosed in #52.
- **Multi-tool registration non-atomicity**: a later Tool's registration
  failure leaves earlier Tools in this same `register()` call already
  registered with `ToolRegistry`/`ResourceRegistry`, with no corresponding
  Module entry. Demonstrated by its own test, disclosed in #52.
- **Locally-tracked Tool lifecycle state**: `ToolRegistry` exposes no way
  to read a specific Tool's current state, so `InMemoryModuleRegistry`
  mirrors it locally; a cross-module version collision could make that
  local copy stale. Disclosed in #52, not solved by this Unit.
- **No live Permission Engine gating of `enable`/`disable`/`remove`**:
  deferred, mirroring gap #24's identical, pre-existing scope reduction
  for Tool Registry, and explicitly left open by Contract Design itself.

## 7. Confirmation

- **The Module Registry is implemented exactly as Contract Design
  defines it** — every required contract and interface member is present;
  none was added, removed, or reshaped beyond what Contract Design
  approved.
- **No architecture was redesigned.** `MODULE_FRAMEWORK_ARCHITECTURE.md`,
  `MODULE_CONTRACT_DESIGN.md`, `ARCHITECTURE_V2_FROZEN_BASELINE.md`,
  PES-001, the Parker Constitution, and every ADR are unmodified.
- **No contract was modified.** `src/contracts/Module.kt`'s shapes match
  `MODULE_CONTRACT_DESIGN.md`'s Section 1–9 field lists exactly.
- **No plugin, module loading, discovery, or dependency-injection
  framework was implemented.** `Plugin.kt`/`Plugin.md` are untouched;
  `build.gradle.kts`'s exclusion list is unchanged.
- **No Home Assistant, Weather, or Gmail integration was implemented.**
- **Constitutional boundaries are preserved.** Modules remain capability
  providers only: no module self-authorises (`enable` never grants a
  permission — proven by test), no module executes directly (a module's
  Tool is reachable only via `ToolRegistry`/`ExecutionPipeline`, never via
  `ModuleRegistry` — proven by test), and `ModuleRegistry` introduces no
  new authority of any kind.
- **All tests pass:** not yet confirmed by Android Studio (see Section
  4). The static, arithmetic projection is 441/441, carried forward
  unauthoritative into `IMPLEMENTATION_HISTORY.md`'s own entry for this
  Unit pending human verification.
- **IMPLEMENTATION_GAPS.md was updated only because a genuine gap was
  discovered** — #52, covering the disclosed interpretive decisions and
  scope reductions in Section 6 above. No pre-existing gap was closed,
  reopened, or contradicted by this addition.

Not committed, per this Unit's own instruction.
