# Claude Code Brief

Build Parker according to the architecture.

Non-negotiables:

- Models never execute tools.
- All execution goes through ExecutionPipeline.
- Protected resources are declared in ResourceRegistry.
- PermissionEngine authorises sensitive actions.
- Tools validate before execution.
- Internal agents are principals.
