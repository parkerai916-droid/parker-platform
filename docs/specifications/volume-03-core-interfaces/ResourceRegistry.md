# ResourceRegistry Interface

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
