# MemoryStore Interface

## Purpose

The MemoryStore manages Parker's durable long-term knowledge.

## Responsibilities

- Store memories
- Retrieve memories
- Promote candidate memories
- Archive or forget memories
- Preserve source attribution

## Required Operations

```kotlin
interface MemoryStore {
    suspend fun addCandidate(candidate: CandidateMemory): MemoryId
    suspend fun promote(memoryId: MemoryId): Memory
    suspend fun retrieve(query: MemoryQuery): List<Memory>
    suspend fun forget(memoryId: MemoryId): ForgetResult
}
```

## Normative Requirements

- Memory promotion MUST respect policy.
- Sensitive memories MUST require appropriate permission.
- Memories MUST preserve provenance.
- Forgetting MUST be auditable.

## Related

- Chapter 17 – Memory Architecture
- Chapter 33 – Memory Consolidation
