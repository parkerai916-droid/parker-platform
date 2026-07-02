# ModelManager Interface

## Purpose

The ModelManager provides access to AI model capabilities without coupling services to specific models.

## Responsibilities

- Route inference requests
- Select local or cloud models according to policy
- Monitor model health
- Handle fallbacks
- Preserve capability contracts

## Required Operations

```kotlin
interface ModelManager {
    suspend fun infer(request: ModelRequest): ModelResponse
    suspend fun capabilityStatus(capability: ModelCapability): CapabilityStatus
}
```

## Normative Requirements

- Services SHOULD request capabilities, not specific models.
- Local models SHOULD be preferred where practical.
- Model failures MUST degrade safely.

## Related

- Chapter 22 – Model Manager
