# EventType Contract (Supporting Type)

## Status
Version: 0.7-alpha
Status: New — closes IMPLEMENTATION_GAPS.md / consistency review §2.2
(v0.7 Architecture Completion Phase, Priority 3).

## Purpose
The type of `EventBus.subscribe(eventType: EventType, ...)`'s first
parameter (see `EventBus.md`) and of `ParkerEvent.eventType`
(`docs/schemas/Event.schema.json`, currently typed as a bare `string`).

## Design Decision
`EventType` is an **open, namespaced string identifier**, not a closed
enum. A closed enum would require a Volume 3 revision (and ADR, per
ADR-019) every time a new event category is introduced — untenable given
Plugins (Chapter 15) and future subsystems both need to introduce new
event categories without modifying core specification.

## Naming Convention
- Core event types are namespaced `<domain>.<event>`, e.g.
  `execution.completed`, `permission.denied`, `resource.updated`,
  `session.expired`. Domains correspond to existing chapters/subsystems.
- Plugin-supplied event types **MUST** be namespaced
  `plugin:<pluginId>.<event>` to prevent collision with core event types
  or other plugins — the same namespacing rule used for plugin-supplied
  action vocabulary entries in `docs/architecture/action-mapping.md`.
- `EventType` values are case-sensitive and MUST NOT be blank.

## Required Fields
- value (the namespaced string itself)

## Normative Requirements
- An `EventType` used in `ParkerEvent.eventType` MUST match the namespacing
  convention above.
- `EventBus.subscribe` MUST support exact-match subscription at minimum.
  Wildcard/prefix subscription (e.g. subscribing to all of `execution.*`)
  is useful but not specified here — see Open Questions.

## Open Questions (not resolved by this entry)
- Whether `subscribe` should support prefix/wildcard matching on the
  domain portion of an `EventType`, or only exact match.
- Whether a registry of known core `EventType` values should be
  maintained centrally (analogous to the Tool Registry's capability
  declarations) rather than living implicitly in each publisher's code.

## Related
- EventBus.md
- Event-Schema.md (`docs/schemas/Event.schema.json`)
- docs/architecture/action-mapping.md (parallel namespacing precedent)
