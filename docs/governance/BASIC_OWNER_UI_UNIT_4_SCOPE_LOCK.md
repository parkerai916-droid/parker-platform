# Basic Owner UI Unit 4 Scope Lock

## 1. Purpose

Unit 4 introduces only the composition-layer bridge connecting the existing `OwnerInteraction` capability to `ParkerRuntime.submitOwnerMessage` and `OwnerNotificationSink`. No new Parker capability is authorised.

## 2. Adapter authority

The adapter may construct one `InboundOwnerMessage` from exact owner text, configured identities, an injected clock, and a newly supplied correlation ID; invoke the narrow submit capability exactly once; map its outcome; and forward notification-sink text through the existing reply receiver.

It may not directly invoke permission, reasoning, conversation, response composition/delivery, execution, tools, memory, knowledge, evidence, model, Ollama, Qwen, or any server operation, and may not bypass or reinterpret ParkerRuntime.

## 3. Runtime method allowlist

The only production runtime capability authorised is `submitOwnerMessage(InboundOwnerMessage)`. The adapter must receive that method capability rather than a ParkerRuntime reference. Generic execute/invoke/call/command mechanisms, reflection dispatch, or method maps are forbidden.

## 4. Message construction and identity

The message must contain the configured local-text `ModuleId`, configured owner `PrincipalId`, exact unmodified accepted text, injected clock value, and newly supplied `CorrelationId`. Identity is fixed at composition and cannot be supplied or altered by the graphical UI.

## 5. Outcome mapping

- Delivered -> Delivered with execution status.
- NotAccepted -> NotAccepted with unchanged reason.
- Failed -> Failed with stage and permitted available message.
- Planned -> Planned with the exhaustive planning-session result category.
- `ParkerRuntimeException.NotRunning` -> Unavailable and Stopped availability.

No result may fabricate reply content or new semantic judgement.

## 6. Reply delivery and single flight

Parker-authored reply text enters only through `OwnerNotificationSink`. Delivered remains silent. The bridge binds only one active reply receiver; the adapter independently rejects direct concurrent use rather than inventing reply correlation.

## 7. Lifecycle and launcher

The adapter owns neither `ParkerRuntime.start()` nor `shutdown()`. Lifecycle belongs to a later composition launcher. The existing CLI/headless `Main.kt` and safe/default `OfflineOwnerUiMain` remain unchanged. A real graphical runtime launcher is deferred to Unit 5.

## 8. Isolation and verification

The adapter belongs under `src/composition`, requires no Compose types, and preserves the isolated `ui-desktop` project. All tests are deterministic and inject the narrow submit capability. No runtime graph, HTTP/network class, model, endpoint, server, Ubuntu environment, live evaluation, deployment, or Unit 3-C artefact may be accessed.

## 9. Stop boundary

Unit 4 stops after adapter implementation, offline verification, completion review, and independent constitutional review. No Unit 5 execution, commit, push, merge, or deployment is authorised.

