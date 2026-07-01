# Chapter 47 – Diagnostics and Telemetry

## Purpose

Diagnostics and Telemetry help Parker detect, explain and recover from faults.

## Responsibilities

- Health checks
- Crash reporting
- Performance metrics
- Model diagnostics
- Tool diagnostics
- Integration diagnostics
- User-facing troubleshooting

## Diagnostic Events

Examples:

- ModelUnavailable
- ToolFailed
- HomeAssistantDisconnected
- PermissionDenied
- MemoryStoreUnavailable
- PluginCrashed

## Privacy

Telemetry is local-first by default.

External telemetry must be explicit, optional and transparent.

## Architectural Rules

- Diagnostics never bypass privacy controls.
- Sensitive payloads are redacted.
- Health status should be machine-readable.
- Critical failures should trigger Safe Mode where appropriate.

## Summary

Diagnostics make Parker maintainable without turning it into a data vacuum. Humanity may yet recover.
