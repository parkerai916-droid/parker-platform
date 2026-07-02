# Plugin Interface

## Status
Version: 0.6-alpha3 (content unchanged since the v0.6 Volume 3 release;
stamped during the v0.7 Architecture Completion Phase for consistency
with other Volume 3 documents -- see
docs/reviews/PARKER_V0_6_CONSISTENCY_REVIEW.md §3.6).

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
