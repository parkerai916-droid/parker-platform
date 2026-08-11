# Basic Owner UI Unit 4 Planning Review

## Baseline finding

Fresh inspection of Units 1-3, the UI source, desktop subproject, composition root, contracts, runtime delivery path, tests, branch, and complete working-tree diff found no material contradiction.

- `ParkerRuntime.submitOwnerMessage(InboundOwnerMessage)` remains the complete governed owner-conversation entry point.
- `LocalTextChannelDeliverTool` invokes the composition-supplied `OwnerNotificationSink`; this is the sole reply-text delivery seam.
- `ParkerRuntimeOutcome.Delivered` contains an `ExecutionResult`, not reply text.
- owner and local-text identities are read from `ParkerRuntimeConfig` and constructed as `PrincipalId`/`ModuleId` by composition.
- `submitOwnerMessage` retains the existing conversation, reasoning, permission, response, execution, and delivery pipeline internally.

## Narrow implementation determination

The adapter will live in `parker.composition`, implement the unchanged UI-owned `OwnerInteraction`, and receive only:

- fixed `PrincipalId` and `ModuleId` values;
- a single `suspend (InboundOwnerMessage) -> ParkerRuntimeOutcome` capability;
- a composition-owned `OwnerUiNotificationBridge` also supplied to `ParkerRuntime` as its `OwnerNotificationSink`;
- an injected clock; and
- an injected correlation-ID source.

It will never hold a `ParkerRuntime` reference. This makes every non-conversation runtime method unreachable by construction and permits complete deterministic testing without constructing a runtime graph.

## Notification/lifecycle determination

The bridge temporarily binds the one active UI reply receiver while submission is in flight. This follows the frozen single-flight rule and does not add correlation to `OwnerNotificationSink`. The adapter neither starts nor stops ParkerRuntime. A real graphical runtime launcher is deferred to Unit 5 because it adds lifecycle/configuration wiring that Unit 4 may not execute end to end.
