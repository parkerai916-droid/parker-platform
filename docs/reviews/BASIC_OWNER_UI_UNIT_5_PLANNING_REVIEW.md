# Basic Owner UI Unit 5 Planning Review

## Fresh baseline assessment

Units 1-4 scope locks, completion reviews, independent reviews, UI/desktop sources, the Unit 4 adapter, runtime/configuration composition, Gradle configuration, tests, branch, and complete working-tree diff were read afresh. The repository remains consistent with the accepted baseline on `ui/basic-owner-interface`.

## Narrow implementation plan

Unit 5 will add:

1. a composition-owned `OwnerUiRuntimeSession` that owns only start/shutdown sequencing and withholds `OwnerInteraction` until startup succeeds;
2. a real composition factory using `ParkerRuntimeConfigLoader`, `ParkerRuntime`, `OwnerUiNotificationBridge`, and `OwnerUiRuntimeAdapter`; and
3. a separately invoked Compose launcher that presents Starting, Ready, or safe startup-failure state and shuts the controller before the runtime.

The existing root `Main.kt`, offline launcher, runtime internals, adapter contract, and UI contract remain unchanged.

## Launcher location

The real graphical entry point belongs in `ui-desktop` because only that isolated subproject may own Compose types. Its runtime factory belongs in `parker.composition`, where configuration and ParkerRuntime construction already belong. The existing one-way desktop-to-root dependency supports this without moving Compose into Parker core.
