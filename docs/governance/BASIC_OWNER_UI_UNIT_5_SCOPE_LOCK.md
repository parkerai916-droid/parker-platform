# Basic Owner UI Unit 5 Scope Lock

## 1. Purpose

Unit 5 integrates the existing graphical owner UI with the existing ParkerRuntime owner-conversation boundary. It creates no new Parker capability.

## 2. Explicit real graphical launcher

A separate `OwnerUiMain` launcher is authorised. It must use ParkerRuntime and `OwnerUiRuntimeAdapter`, remain explicitly invoked, and neither replace `parker.composition.MainKt` nor become the offline launcher. `OfflineOwnerUiMain` remains deterministic and unchanged in purpose.

## 3. Lifecycle ownership

The launcher/session may call runtime start and shutdown; the adapter may not. Interaction becomes available only after start succeeds. Shutdown ordering is fixed: stop/cancel the UI controller, invoke idempotent runtime shutdown exactly once, then terminate the window/application. No competing shutdown hook or second path is authorised.

## 4. Configuration

The launcher must use `ParkerRuntimeConfigLoader` semantics. It may not add UI-specific endpoint configuration, hardcode server addresses, or expose model endpoint, owner `PrincipalId`, or channel `ModuleId` as GUI controls.

## 5. Interaction and outcome paths

Owner text follows only `OwnerUiController -> OwnerUiRuntimeAdapter -> InboundOwnerMessage -> ParkerRuntime.submitOwnerMessage -> governed pipeline`. Reply text follows only governed delivery through `OwnerNotificationSink -> bridge -> adapter reply receiver -> controller`. Unit 1-4 outcome mapping remains unchanged.

## 6. Authority and concurrency

The UI receives only `OwnerInteraction`; the adapter receives only `submitOwnerMessage`. The launcher may construct ParkerRuntime but may not bypass it or separately construct its internals. Evidence, memory, permission, model, network, and arbitrary runtime capabilities remain unavailable to UI. Single-flight remains unchanged.

## 7. CLI, offline launcher, and Compose isolation

`Main.kt` headless/CLI behavior remains unchanged. The offline launcher remains safe and explicit. Compose stays entirely in `ui-desktop`; composition/runtime sources import no Compose type.

## 8. Live execution gate

No real model-backed execution, endpoint access, Parker server/Ubuntu access, Ollama/Qwen access, live evaluation, or Unit 3-C task/access may occur while the isolation constraint remains active. Implementation status and live end-to-end verification status must be reported separately.

## 9. Stop boundary

Stop after offline implementation, deterministic verification, completion review, and independent constitutional review. Do not commit, push, merge, deploy, or begin any further unit.

