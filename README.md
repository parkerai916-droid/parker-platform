# Parker Platform

Parker is a privacy-first Personal Intelligence Platform designed around trust, structured reasoning, secure execution, local-first operation and long-term extensibility.

## Status

**Architecture version:** v0.8 (Phase 2 Runtime Complete)
**Repository status:** Private development
**Future intent:** Open source once architecture, security model and licence are ready.

Current implementation state, verified by a real Gradle build and test
run in Android Studio (BUILD SUCCESSFUL, 101 tests passed, 0 failed):

- **Phase 1 Core Contracts: complete.** Volume 1 core contracts
  (`Principal`, `Resource`, `ExecutionRequest`, `ExecutionResult`,
  `Permission`/`PermissionDecision`, identifiers, lifecycle state
  machines) are implemented in Kotlin under `src/contracts/`, with tests
  under `tests/contracts/`.
- **Phase 2 Runtime: complete.** Tool Registry, Action Mapping, EventBus,
  and Runtime Integration (`DefaultExecutionPipeline`) are implemented
  under `src/interfaces/` and `src/runtime/`, with tests under
  `tests/runtime/`. Every runtime component depends only on
  already-specified interfaces, injected via constructor — no
  authorisation policy has been invented; `PermissionEngine.evaluate`
  itself remains unimplemented.
- **Identity Service: complete** (foundation only). Principal
  registration, resolution, lifecycle enforcement, `lastSeenAt` tracking,
  and ownership queries are implemented (`src/interfaces/IdentityService.kt`,
  `src/runtime/InMemoryIdentityService.kt`). Not yet wired into
  Permission Engine, EventBus, or Tool Registry — see
  `docs/reviews/PARKER_PHASE_2_RUNTIME_VERIFICATION_REPORT.md`.
- **101 automated tests passing**, 0 failing, across Phase 1 contracts,
  Tool Registry, Action Mapping, EventBus, Runtime Integration, and
  Identity Service.
- **Architecture Chapters 1–50** are present under `docs/architecture/`.
- **20 Architecture Decision Records** are present under `docs/adr/`
  (numbered ADR-001 through ADR-022; ADR-004 and ADR-005 do not exist —
  see `docs/architecture/IMPLEMENTATION_GAPS.md` for why).
- **Engineering Specification Volumes 1–3** (Core Contracts, Core Schemas,
  Core Interfaces) are present under `docs/specifications/`, with
  canonical JSON Schemas under `docs/schemas/`.
- Full consistency and completion reviews are recorded at
  `docs/reviews/PARKER_V0_6_CONSISTENCY_REVIEW.md`,
  `docs/reviews/PARKER_V0_7_ARCHITECTURE_COMPLETION_REPORT.md`, and
  `docs/reviews/PARKER_PHASE_2_RUNTIME_VERIFICATION_REPORT.md` (current).
- **Not yet implemented:** `PermissionEngine.evaluate`'s authorisation
  policy, any concrete `Tool`, Identity Service integration, cascading
  Principal revocation, Agent runtime, Memory, World Model, and Phase 3
  generally — see `docs/architecture/IMPLEMENTATION_GAPS.md`.

## Core Rule

> Cognition proposes. Trust authorises. Runtime executes.

Parker is not built around a single language model. Models are replaceable components. Trust is the architecture.

## Architecture Chapters

`docs/architecture/` currently contains Chapters 1–50 of the Parker
Platform Architecture, spanning the core trust/runtime chapters (1–15),
cognition (16–24), integrations (25–32), advanced reasoning (33–36),
task/session management (37–41), platform services (42–48), and
verification/deployment (49–50). See `docs/architecture/00-index.md` for
the full chapter list.

## Repository Layout

```text
docs/architecture/     Platform architecture chapters (1-50) and
                        cross-cutting architecture specs (Tool Registry,
                        Action Mapping, Identity Service, gap tracking)
docs/adr/               Architecture Decision Records
docs/specifications/    Engineering Specification Volumes 1-3
docs/schemas/           Canonical JSON Schemas for Volume 1/2 contracts
docs/diagrams/          Lifecycle and flow diagrams (.mmd)
docs/reviews/           Consistency and completion reports
docs/roadmap/           Development roadmap
docs/development/       Claude Code and implementation guidance
docs/security/          Security notes and threat model
docs/glossary/          Glossary
src/interfaces/         Volume 3 interfaces: implemented (ExecutionPipeline,
                        PermissionEngine, ResourceRegistry, Tool,
                        ToolRegistry, EventBus, IdentityService) and
                        later-phase stubs still excluded from the build
                        (ADR-022: Agent, AuditService, MemoryStore,
                        ModelManager, NotificationService, Plugin, WorldModel)
src/contracts/          Phase 1 core contracts, plus lifecycle transition
                        validators and runtime-supporting types (implemented)
src/runtime/            Phase 2 concrete implementations: InMemoryToolRegistry,
                        ActionMapper, InMemoryEventBus, DefaultExecutionPipeline,
                        InMemoryResourceRegistry, InMemoryIdentityService
tests/contracts/        Phase 1 unit tests (implemented)
tests/runtime/          Phase 2 runtime unit tests (implemented)
gradle/wrapper/         Gradle Wrapper (Gradle 8.10)
plugins/                Future plugins
agents/                 Future internal agents
tools/                  Future tools
examples/               Example flows
```

## Engineering Slogan

> Think clearly. Act safely. Learn continuously.
