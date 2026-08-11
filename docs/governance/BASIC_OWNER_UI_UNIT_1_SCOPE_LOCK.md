# Basic Owner UI — Unit 1 — Owner UI Boundary and Offline Harness Scope Lock

**Status:** Scope Locked

## 1. Purpose

UI Unit 1 establishes only:

- a narrow, UI-owned owner-conversation interaction port;
- presentation-safe submission dispositions;
- deterministic offline interaction behaviour; and
- plain-Kotlin controller and presentation-state behaviour sufficient to develop UI Unit 2 without `ParkerRuntime`.

It creates no graphical UI and grants no runtime capability.

## 2. Non-authority

`parker.ui` has no authority to reason, authorise, execute, inspect, or alter governed Parker state. It may not access or alter Memory Core, Knowledge Memory, Evidence Custodian, Evidence Intelligence, Conversation Engine decisions, permissions, Tools, Execution Pipeline, Reasoning Provider, Ollama, Qwen, a model endpoint, or a Parker server.

It may not construct Parker-authored responses, bypass `ParkerRuntime`, expose arbitrary `ParkerRuntime` methods, or introduce a generic command, capability, RPC, or runtime facade.

## 3. Owner interaction port

The UI-owned `OwnerInteraction` boundary supports only:

1. submitting non-blank owner text;
2. returning one presentation-safe submission disposition;
3. delivering Parker reply text through a separate injected reply receiver; and
4. reporting whether owner interaction is available or stopped.

The boundary must not expose `execute(command)`, `invoke(capability)`, `callRuntime`, arbitrary payloads, or runtime-internal types.

## 4. Single-flight submission

Only one owner submission may be in flight at a time. The controller must enforce this structurally before invoking `OwnerInteraction`. The offline implementation must independently reject direct concurrent use.

This restriction exists because `OwnerNotificationSink.notify(text)` carries no correlation identity with which concurrent replies could be paired safely.

## 5. Reply authorship

Parker-authored transcript text may enter presentation state only through the reply receiver representing `OwnerNotificationSink` or its deterministic fake equivalent.

A `Delivered` disposition never contains reply text and must never create a Parker-authored transcript entry. Owner text, Parker reply text, and presentation/system status are distinct transcript types. Status, failure, rejection, planning, availability, and lifecycle text must never masquerade as Parker speech.

## 6. Presentation-safe dispositions

The UI boundary represents the production boundary one-to-one without importing it:

- `Delivered` preserves the execution status string supplied by a future composition adapter.
- `NotAccepted` preserves the rejecting reason.
- `Failed` preserves the honest failure stage and a safe failure message.
- `Planned` preserves only the planning-result category supplied by the adapter.
- `Unavailable` represents `ParkerRuntimeException.NotRunning` or an offline stopped state and preserves a safe reason.

No disposition may fabricate reply content, causal detail, authority, or a more precise classification than its source provides.

## 7. Offline harness

The Unit 1 default is a deterministic scripted `OfflineOwnerInteraction`. It must:

- construct no `ParkerRuntime` and read no `ParkerRuntimeConfig`;
- read no process environment or endpoint configuration;
- perform no network, HTTP, model, Ollama, Qwen, server, file, or live-runtime access;
- use deterministic injected delay behaviour;
- record submissions for assertions;
- support reply-plus-delivery, delivery without reply, rejection, failure, planning, delayed processing, and unavailable/stopped scenarios; and
- reject concurrent direct submissions.

## 8. Controller and presentation state

The controller may own typed owner, Parker, and system transcript entries; `Ready`, `Processing`, `Stopped`, and `Error` states; blank-input rejection; single-flight enforcement; submission lifecycle; and deterministic coroutine cancellation/shutdown.

It performs presentation sequencing only. It does not decide what Parker means, whether an action is permitted, or how Parker produces a response.

## 9. Source and dependency boundaries

Unit 1 code belongs under `src/ui/parker/ui`; tests belong under `tests/ui`.

Dependency direction is frozen:

- `parker.ui` must not depend on `ParkerRuntime`, concrete runtime coordinators, runtime configuration, model clients, or privileged APIs.
- `parker.runtime` must not depend on `parker.ui`.
- future real-runtime UI integration belongs in `parker.composition` and is excluded from this Unit.
- the Unit 1 offline implementation remains inside `parker.ui`.

## 10. Compose and packaging exclusion

Unit 1 must not add or use a Compose plugin, Compose dependency, Compose state type, Compose window, rendering, preview, packaging, installer, or native distribution task. Compose Desktop begins only after Unit 1 completes and UI Unit 2 is separately planned.

## 11. Verification

Verification must prove blank rejection, single-flight behaviour, authorship separation, every disposition, processing visibility, lifecycle recovery, cancellation-safe shutdown, deterministic fake timing, absence of runtime/network/model dependencies, and continued detachment of live-model tasks from ordinary offline lifecycle tasks.

No live-model or Unit 3-C task may be invoked.

## 12. Completion boundary

Unit 1 completes with the boundary, offline harness, controller, deterministic tests, ordinary offline repository verification, completion review, and independent constitutional review. It stops before Compose, graphical rendering, a real `ParkerRuntime` adapter, deployment, push, or merge.
