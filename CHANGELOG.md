# Changelog

## v0.4

Added consolidated architecture snapshot containing Chapters 1-20.

## v0.6

Engineering Specification Volumes 1-3 authored (Core Contracts, Core
Schemas, Core Interfaces). Phase 1 Kotlin core contracts implemented on
`feature/phase-1-core-contracts` with unit tests. Specification cleanup
pass: ExecutionRequest schema fixed, Principal schema reconciled to
prose/Kotlin, Volume 2 schema docs filled in, ADR-005 dangling citation
removed. Full consistency review completed
(`docs/reviews/PARKER_V0_6_CONSISTENCY_REVIEW.md`): Phase 1 found
internally consistent; Phase 2 blocked pending missing Tool Registry,
EventBus supporting types, and action-mapping architecture.

## v0.7

Architecture Completion Phase. Closes the Phase 2 blockers found by the
v0.6 consistency review, without writing Kotlin:

- New: `docs/architecture/tool-registry.md` (Tool Registry architecture).
- New: `docs/architecture/action-mapping.md` (ExecutionRequest.proposedActions
  -> PermissionDecision.action mapping; part of Trust Architecture).
- Completed: EventBus supporting types (`EventType`, `EventHandler`,
  `Subscription`, `PublishResult`), `EventBus.md` rewritten with lifecycle,
  authentication, authorisation, ordering, delivery guarantee, failure
  handling, cancellation, and security sections.
- New: `docs/architecture/IdentityService.md` (proposed interface and
  integration model; IdentityService itself remains unimplemented, per
  ADR-022).
- New: lifecycle diagrams and specifications for Session, Task, and
  Workflow (`docs/diagrams/*-lifecycle-state-machine.mmd`, appended to
  the corresponding Volume 2 schema docs).
- Specification consistency fixes: ADR-004 dangling citation removed from
  `Agent.md`; `Permission.schema.json` labelled deprecated in favour of
  `PermissionDecision.schema.json`; `VOLUME_1_INDEX.md` corrected to
  reference the latter; `ExecutionResult.schema.json` gained
  `toolResults`/`reflectionCandidate`; `Resource.schema.json` gained
  `createdAt`/`updatedAt`/`source`; all 12 original Volume 3 interface
  docs stamped with a version/status header; `RequestOrigin.AGENT` vs
  `PrincipalType.INTERNAL_AGENT` terminology clarified; this README and
  CHANGELOG brought current.
- See `docs/reviews/PARKER_V0_7_ARCHITECTURE_COMPLETION_REPORT.md` for
  the full report, remaining human decisions, and Phase 2 readiness
  assessment.
