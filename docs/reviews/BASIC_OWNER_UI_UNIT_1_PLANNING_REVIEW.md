# Basic Owner UI — Unit 1 Planning Review

**Status:** Complete — repository baseline confirmed at `9fd3dbe` on the local Windows branch `ui/basic-owner-interface`.

## Finding

Fresh inspection confirms the Basic Owner UI Planning and Architecture Review remains accurate. No material contradiction was found.

The implemented owner path remains:

`InboundOwnerMessage -> ParkerRuntime.submitOwnerMessage -> governed conversation pipeline -> OwnerNotificationSink`

`ParkerRuntime.submitOwnerMessage` separately returns `ParkerRuntimeOutcome.Delivered`, `NotAccepted`, `Failed`, or `Planned`. `Delivered` contains an `ExecutionResult`, never reply text. Parker-authored reply text reaches an owner adapter only through `OwnerNotificationSink.notify(text)`.

Because `OwnerNotificationSink` carries text without a correlation identifier, the first owner UI must permit only one in-flight submission. The existing `InteractiveConsole` confirms that injected submission, reply, clock, and presentation functions are the established offline-testable adapter style.

## Build and source findings

- Kotlin/JVM 1.9.24, Gradle 8.10, and JVM toolchain 17 remain configured.
- `src/contracts`, `src/interfaces`, `src/runtime`, and `src/composition` remain the production source boundaries.
- `tests/contracts`, `tests/runtime`, and `tests/composition` remain the test boundaries.
- `liveModelEvaluation` remains an explicit detached source set. It is not attached to `test`, `check`, `build`, or `assemble`.
- No Compose, Swing, JavaFX, desktop, `src/ui`, or `tests/ui` implementation exists.

## Unit 1 consequence

Unit 1 may add a plain-Kotlin `parker.ui` boundary, deterministic offline interaction fake, presentation state/controller, and deterministic tests. It may add `src/ui` and `tests/ui` to the existing source sets. It may not add Compose, construct `ParkerRuntime`, introduce network access, or expose privileged runtime methods.

**Decision:** READY FOR UI UNIT 1 SCOPE LOCK.
