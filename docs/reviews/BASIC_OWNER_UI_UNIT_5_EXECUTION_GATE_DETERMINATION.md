# Basic Owner UI Unit 5 Execution-Gate Determination

## Safe offline

- compile the explicit real launcher and its root composition factory;
- verify existing config-loader use and configured identity propagation structurally;
- verify start-before-interaction, safe startup failure, controller-first shutdown, and exactly-once runtime shutdown using injected lifecycle functions;
- verify adapter/controller/notification wiring by type and source structure;
- verify no privileged runtime method, direct model/network client, or hardcoded server endpoint reaches the UI;
- preserve and verify the existing CLI and offline launcher;
- run ordinary root and isolated desktop offline test/check/build tasks.

## Requires real model-backed execution

- invoke the real graphical launch task with production-like environment configuration;
- execute `ParkerRuntime.start()` far enough to construct the real `LocalHttpModelInferenceClient` against a live endpoint;
- submit owner text through the window and obtain inference;
- observe an actual governed model-generated reply delivered through `OwnerNotificationSink` into the graphical transcript;
- verify real shutdown following that run.

## Gate decision

The Unit 3-C isolation constraint remains active. Every real-execution item is deferred. Unit 5 may complete implementation and offline verification only; it must not claim live end-to-end verification.

