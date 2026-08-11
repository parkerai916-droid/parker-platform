# Basic Owner UI — Unit 2 — Basic Compose Conversation Window Scope Lock

**Status:** Scope Locked

## 1. Purpose

Unit 2 graphically presents the already-governed Unit 1 owner interaction capability. It creates no new Parker capability, authority, runtime path, or decision.

## 2. Toolkit

Compose Desktop 1.6.11 is authorised as this UI programme's presentation implementation choice only. It is not a constitutional Parker platform dependency. Kotlin remains 1.9.24, Gradle remains 8.10, and the JVM toolchain remains 17.

Compose compiler application is isolated to the `ui-desktop` subproject. The root Parker JVM module remains plain Kotlin and must not receive Compose compiler transformation or Compose runtime dependencies. The desktop subproject depends one-way on the root project solely to consume the Unit 1 UI boundary.

## 3. Offline-only composition

The explicit graphical launcher constructs only Compose presentation, `OwnerUiController`, and `OfflineOwnerInteraction` with deterministic scripts. It must not construct or inspect `ParkerRuntime`, `ParkerRuntimeConfig`, model clients, Reasoning Provider, Permission Engine, Conversation Engine, Memory Core, Knowledge Memory, Evidence components, Ollama, Qwen, environment configuration, networking clients, or server state.

There is no mode detection, hidden real-runtime switch, or environment-based connection path.

## 4. Existing main preservation

`parker.composition.MainKt` remains Parker's existing CLI/headless runtime entry point and retains its current responsibility unchanged. The `application` plugin's ordinary main class remains `parker.composition.MainKt`.

Unit 2 adds a separately named, explicit offline UI run task. Ordinary `run` must never start the graphical UI automatically.

The explicit task is `:ui-desktop:runOfflineOwnerUi`; the root project's `run` task and application main remain unchanged.

## 5. Presentation authorship

Compose renders the typed Unit 1 authorship variants directly: Owner, Parker, and System. It must not infer authorship from text. System status must remain visibly subordinate and must never masquerade as Parker speech.

## 6. Single-flight submission

`OwnerUiController` remains the authoritative single-flight enforcement point. While state is Processing, Compose must disable Send and prevent Enter-key submission. UI reinforcement does not replace controller enforcement.

## 7. Delivered semantics

Delivered creates no transcript text of any authorship. Only a typed Parker entry produced through Unit 1's reply receiver may render as Parker speech.

## 8. UI state

The window may render only Unit 1's Ready, Processing, Stopped, and Error presentation states. It may not imply runtime health, model status, server status, or any other fact Unit 1 does not provide.

## 9. Initial window capability

Unit 2 authorises only:

- Parker identity/header and an explicit local-offline presentation label;
- presentation status;
- scrollable transcript with distinct Owner, Parker, and System entries;
- multiline text input and Send;
- Enter to submit and Shift+Enter for a newline where cleanly supported;
- blank-input prevention;
- disabled submission while Processing or Stopped;
- restrained processing indication;
- automatic transcript scrolling when entries are appended; and
- orderly controller shutdown when the window closes.

NotAccepted, Failed, and Planned remain typed System entries produced by Unit 1. Delivered alone remains speechless.

## 10. Exclusions

Unit 2 excludes real runtime integration; memory, knowledge, evidence, permission, Tool, Agent, Task, audit, model, Ollama, Qwen, server, and Unit 3-C controls; authentication; voice; REST; WebSocket; RPC; remote access; generic settings; native packaging; installers; deployment; and any capability selector or diagnostic console.

## 11. Dependency direction

Dependency direction is frozen:

`Compose presentation -> OwnerUiController -> OwnerInteraction -> OfflineOwnerInteraction`

No Compose component may depend on `ParkerRuntime`. No `src/runtime`, contract, or interface class may depend on Compose or `parker.ui`. Future real-runtime UI composition remains excluded and belongs only in a later `parker.composition` unit.

## 12. Verification and stop

Verification must cover typed rendering, enablement rules, keyboard gating, blank prevention, outcome authorship, scrolling triggers, shutdown, and structural offline-launcher isolation. Ordinary offline `test`, `check`, and `build` must remain detached from every live-model task. A local manual launch must use only the deterministic offline launcher.

Unit 2 stops before a real runtime adapter, packaging, Unit 3, commit, push, merge, or deployment.
