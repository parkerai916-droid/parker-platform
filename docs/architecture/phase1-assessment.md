# Phase 3 — Implementation Assessment

Branch `feature/phase-1-core-contracts`, tag `v0.6-implementation-baseline` created off `main` at `7c6f201`. Written before any Kotlin was added, per Phase 3's requirement.

## Current source structure

`src/` contains only `src/interfaces/` (12 Kotlin interface stubs, package `parker.core.interfaces`, all `suspend fun`, no imports, referencing dozens of types — `ExecutionRequest`, `PermissionDecision`, `ToolDescriptor`, `RequestId`, etc. — that don't exist as Kotlin anywhere yet) plus a placeholder `src/README.md`. `tests/` contains only a placeholder `README.md`. **No Gradle/Maven project exists.** This matches what I found during the earlier reconciliation pass; nothing has changed since.

## Which Engineering Spec v0.6 package to implement first

**Volume 1 — Core Contracts** (`Principal`, `Resource`, `Permission`/`PermissionDecision`, `ExecutionRequest`, `ExecutionResult`), plus the Volume 3 interfaces that directly consume them (`Tool`, `PermissionEngine`, `ExecutionPipeline`, `ResourceRegistry`). This is the only package that unblocks everything else: every existing stub in `src/interfaces` fails to compile today because these types don't exist, and Volume 1 is explicitly the first volume ("Volume 1 establishes Parker's core runtime contracts").

I'm treating this task's "Phase 1" (a spec-package label — Volume 1 Core Contracts) as distinct from `IMPLEMENTATION_ORDER.md`'s "Phase 1" (a build-order label — Resource Registry, Identity Service, Permission Engine as running systems). They're both legitimately called "Phase 1" but mean different things; I'm implementing the former (contracts + interfaces, no concrete engines/registries) per this task's explicit "Allowed implementation areas" list.

## Interfaces already specified (Volume 3 + `src/interfaces`)

`ExecutionPipeline`, `PermissionEngine`, `ResourceRegistry`, `Tool` — full prose spec (purpose/responsibilities/required operations/normative requirements) plus a matching `.kt` stub. `EventBus`, `Agent`, `Plugin`, `MemoryStore`, `WorldModel`, `ModelManager`, `NotificationService`, `AuditService` also have specs + stubs but are out of scope this round (see below).

## Schemas already specified

Real, filled-in JSON Schema files exist at `docs/schemas/*.schema.json` for `Principal`, `Resource`, `Permission`, `PermissionDecision`, `ExecutionRequest`, `ExecutionResult`, `Event`, `Session`, `Workflow`, `Task`, `AuditRecord` (plus worked examples for several). **Volume 2's markdown wrappers under `docs/specifications/volume-02-core-schemas/` are templated placeholders** — identical generic boilerplate, not real content — so I'm implementing against the actual `.schema.json` files, not their markdown wrappers.

## Open architectural blockers (full detail in `IMPLEMENTATION_GAPS.md`, added this session)

1. No `IdentityService` interface exists anywhere (Volume 3's own "Included Interfaces" list omits it, despite Chapter 41 and `IMPLEMENTATION_ORDER.md` both naming it). Per Phase 7's "do not invent missing architecture," I am **not** writing this interface this round.
2. `ExecutionRequest.md` (prose) lists `expiresAt`/`correlationId` as required; `ExecutionRequest.schema.json` defines neither. I'm following the prose (both fields are load-bearing elsewhere — the `Expired` lifecycle state and `ExecutionResult.Expired` status only make sense if something tracks an expiry), flagged as a schema-file gap to fix, not invented from nothing.
3. `ToolResult`, `PermissionExplanation`, and `Resource.sensitivity`'s value set are referenced by name but never given a shape anywhere I could find. I'm adding minimal, explicitly-provisional shapes for the first two and typing `sensitivity` as a plain `String` rather than guessing at an enum that isn't specified.
4. `ADR-005` is cited by `EventBus.md` but the file doesn't exist (ADR numbering jumps 003→006). Not blocking for this round since EventBus is out of scope, but recorded.
5. `Principal`/`Resource` lifecycles are stated only as a bare linear chain in prose (no `.mmd` diagram, unlike `ExecutionRequest`'s, which has one) — I'm implementing the state enums but **not** a transition validator for these two, since inferring branch/re-entry rules (e.g. can a Suspended principal return to Active?) would be inventing architecture, not implementing it.

## Files I propose to create

- `settings.gradle.kts`, `build.gradle.kts` (repo root — no build system exists; needed to compile anything and to run tests per Phase 6/8). Configured to compile `src/contracts/` plus only the four in-scope files from `src/interfaces/` (`ExecutionPipeline.kt`, `PermissionEngine.kt`, `ResourceRegistry.kt`, `Tool.kt`), explicitly excluding the eight out-of-scope stubs (`Agent`, `AuditService`, `EventBus`, `MemoryStore`, `ModelManager`, `NotificationService`, `Plugin`, `WorldModel`) so the module actually builds without pulling in Phase 2+ types that don't exist yet. Nothing on disk in `src/interfaces` is touched by this exclusion — it's a build-scope decision, reversible by editing one file.
- `src/contracts/Identifiers.kt` — `PrincipalId`, `ResourceId`, `RequestId`, `DecisionId`, `ResultId` value classes.
- `src/contracts/Principal.kt` — `PrincipalType`, `PrincipalStatus`, `Principal`.
- `src/contracts/Resource.kt` — `ResourceType`, `ResourceLifecycleState`, `Resource`.
- `src/contracts/Permission.kt` — `PermissionAction`, `PermissionLevel`, `PermissionDecisionOutcome`, `PermissionDecision`, `PermissionExplanation` (provisional).
- `src/contracts/ExecutionRequest.kt` — `RequestOrigin`, `RequestPriority`, `RiskEstimate`, `ExecutionRequest`.
- `src/contracts/ExecutionLifecycle.kt` — `ExecutionLifecycleState`, `ExecutionLifecycleTransitions` (validated state machine, matching `docs/diagrams/execution-state-machine.mmd` exactly).
- `src/contracts/ExecutionResult.kt` — `ExecutionResultStatus`, `ToolResult` (provisional), `ExecutionResult`.
- `src/contracts/ToolDescriptor.kt` — `ToolDescriptor`, `ValidationResult` (needed by `Tool.kt`).
- `src/contracts/ExecutionStatusTypes.kt` — `CancellationResult`, `ExecutionStatus` (needed by `ExecutionPipeline.kt`).
- `tests/contracts/*Test.kt` — one test file per contract area (construction, equality, required-field/validation enforcement, lifecycle transitions, interface-shape reflection checks).
- `docs/architecture/IMPLEMENTATION_GAPS.md` — the five blockers above, in the required format.

## Files I propose to modify

- `src/interfaces/ExecutionPipeline.kt`, `PermissionEngine.kt`, `ResourceRegistry.kt`, `Tool.kt` — **add `import` statements only**, so they resolve the new contract types. No signatures, method names, or `suspend` modifiers change. `Agent.kt`, `AuditService.kt`, `EventBus.kt`, `MemoryStore.kt`, `ModelManager.kt`, `NotificationService.kt`, `Plugin.kt`, `WorldModel.kt` are **not** modified at all.

Proceeding to Phase 4 on this basis.
