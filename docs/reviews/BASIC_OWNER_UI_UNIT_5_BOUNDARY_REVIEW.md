# Basic Owner UI Unit 5 Boundary Review

## Real graphical composition path

`OwnerUiMain -> ParkerRuntimeConfigLoader -> ParkerRuntime + OwnerUiNotificationBridge -> OwnerUiRuntimeAdapter -> OwnerUiController -> ParkerOwnerWindow`

The factory constructs composition only; it does not duplicate permission, reasoning, model-client, tool, memory, evidence, or pipeline internals. ParkerRuntime remains the sole owner of those internals.

## Lifecycle

The launcher session owns one start capability and one shutdown capability. It exposes `OwnerInteraction` only in a successful start result. Window close first stops the controller (preventing new submissions and cancelling controller work), then invokes idempotent runtime shutdown, then exits the application.

Startup failure returns a fixed safe presentation message, performs best-effort shutdown once, and never constructs a Ready controller/window state.

## Configuration and authority

The real factory uses `ParkerRuntimeConfigLoader.load(environment)` and derives typed owner/channel identity solely from the resulting config. The graphical UI has no identity or endpoint controls. The adapter still receives only `runtime::submitOwnerMessage`; the UI still receives only `OwnerInteraction`.

## Verdict

The launcher is a composition root, not a coordinator or alternative runtime path. It adds no Parker authority and preserves the CLI, offline launcher, Compose isolation, and Unit 4 capability boundary.
