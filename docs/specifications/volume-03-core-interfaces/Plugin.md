# Plugin Interface

## Purpose

A Plugin extends Parker through declared capabilities and explicit permissions.

## Responsibilities

- Declare manifest
- Register capabilities
- Respect sandbox boundaries
- Use platform APIs rather than internal shortcuts

## Required Operations

```kotlin
interface Plugin {
    val manifest: PluginManifest
    suspend fun initialise(context: PluginContext): PluginStatus
    suspend fun shutdown()
}
```

## Normative Requirements

- Plugins MUST declare required Resources.
- Plugins MUST declare required permissions.
- Plugins MUST NOT bypass ExecutionPipeline.
- Plugins MUST NOT modify Trust settings.

## Related

- Chapter 15 – Plugin SDK
