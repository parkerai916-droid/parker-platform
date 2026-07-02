# Parker Platform

Parker is a privacy-first Personal Intelligence Platform designed around trust, structured reasoning, secure execution, local-first operation and long-term extensibility.

## Status

**Architecture version:** v0.7-alpha (Architecture Completion Phase)
**Repository status:** Private development
**Future intent:** Open source once architecture, security model and licence are ready.

This repository has moved beyond the original v0.4 architecture snapshot.
Current state:

- **Architecture Chapters 1–50** are present under `docs/architecture/`.
- **20 Architecture Decision Records** are present under `docs/adr/`
  (numbered ADR-001 through ADR-022; ADR-004 and ADR-005 do not exist —
  see `docs/architecture/IMPLEMENTATION_GAPS.md` for why).
- **Engineering Specification Volumes 1–3** (Core Contracts, Core Schemas,
  Core Interfaces) are present under `docs/specifications/`, with
  canonical JSON Schemas under `docs/schemas/`.
- **Phase 1 (Volume 1 Core Contracts) is implemented in Kotlin** under
  `src/contracts/`, with tests under `tests/contracts/`, on branch
  `feature/phase-1-core-contracts`. This is a contracts-only
  implementation — no runtime engines (Execution Pipeline, Permission
  Engine, Tool Registry, Event Bus) are implemented yet; see
  `docs/architecture/phase1-assessment.md`.
- A full consistency review of the specification set is recorded at
  `docs/reviews/PARKER_V0_6_CONSISTENCY_REVIEW.md`.
- The v0.7 Architecture Completion Phase (`docs/reviews/PARKER_V0_7_ARCHITECTURE_COMPLETION_REPORT.md`)
  closes the architectural gaps that review identified — Tool Registry,
  Action Mapping, Event Bus supporting types, and an Identity Service
  proposal — before further Kotlin implementation resumes.

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
src/interfaces/         Later-phase Kotlin interface stubs (ADR-022)
src/contracts/          Phase 1 Kotlin core contracts (implemented)
tests/contracts/        Phase 1 unit tests (implemented)
plugins/                Future plugins
agents/                 Future internal agents
tools/                  Future tools
examples/               Example flows
```

## Engineering Slogan

> Think clearly. Act safely. Learn continuously.
