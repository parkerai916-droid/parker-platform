# ResourceRegistry Interface

## Status
Version: 0.6-alpha3 (content unchanged since the v0.6 Volume 3 release;
stamped during the v0.7 Architecture Completion Phase for consistency
with other Volume 3 documents -- see
docs/reviews/PARKER_V0_6_CONSISTENCY_REVIEW.md §3.6).

## Purpose

The ResourceRegistry is the authoritative catalogue of Parker Resources.

## Responsibilities

- Register Resources
- Resolve Resources
- Update lifecycle state
- Provide ownership and sensitivity metadata
- Prevent hidden Resource access

## Required Operations

```kotlin
interface ResourceRegistry {
    suspend fun register(resource: Resource): ResourceId
    suspend fun resolve(resourceId: ResourceId): Resource?
    suspend fun update(resource: Resource): Resource
    suspend fun listByOwner(owner: PrincipalId): List<Resource>
}
```

## Normative Requirements

- Undeclared Resources MUST NOT be accessed.
- Every Resource MUST have an owner.
- Every Resource MUST have sensitivity classification.
- Resource lifecycle changes MUST be auditable.

## Related

- Chapter 8 – Resource Registry
- Resource Contract
