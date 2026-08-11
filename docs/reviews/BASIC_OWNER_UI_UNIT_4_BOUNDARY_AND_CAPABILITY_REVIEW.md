# Basic Owner UI Unit 4 Boundary and Capability Review

## Capability inventory

The adapter needs exactly one Parker capability: submission of one already-constructed `InboundOwnerMessage`. It needs no lifecycle, configuration loading, evidence, memory, permission, reasoning, tool, execution, retrieval, or analysis capability.

## Dependency direction

`Compose -> OwnerUiController -> OwnerInteraction <- OwnerUiRuntimeAdapter -> submitOwnerMessage capability`

`ParkerRuntime -> OwnerNotificationSink <- OwnerUiNotificationBridge -> active UI reply receiver`

The adapter and bridge are composition code without Compose types. `parker.ui` remains independent of composition and runtime types.

## Outcome and authorship review

- Delivered maps only execution status and remains speechless.
- NotAccepted carries the existing reason unchanged.
- Failed carries the existing stage and available cause message; it does not become Parker speech.
- Planned maps the exhaustive planning-session result category.
- NotRunning alone maps to Unavailable and permanently changes adapter availability to Stopped.
- Parker transcript text can originate only from `OwnerNotificationSink.notify` through the currently bound reply receiver.

## Verdict

The capability shape is narrow enough to prevent access to all other ParkerRuntime public methods by type. No generic runtime facade, semantic judgement, lifecycle ownership, Compose coupling, or real-runtime construction is necessary.

