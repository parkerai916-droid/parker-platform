# Basic Owner UI — Unit 2 Independent Constitutional Review

**Status:** Accepted

## Findings

### Did Compose gain Parker decision authority?

No. Compose renders `OwnerUiState` and sends plain owner text only through `OwnerUiController.submit`. It has no reasoning, permission, execution, memory, evidence, planning, or runtime authority.

### Can graphical controls bypass the controller or call ParkerRuntime?

No. Both button and keyboard submission call the same local `submitDraft`, which checks pure presentation enablement and delegates only to `OwnerUiController`. No graphical source or launcher references `ParkerRuntime` or runtime internals.

### Is single-flight structural?

Yes. Compose disables Send and consumes Enter without submission during Processing. The controller remains authoritative and the fake independently rejects concurrent direct use. Manual and deterministic tests confirm no bypass.

### Is authorship preserved?

Yes. Rendering switches exhaustively over Unit 1's typed Owner, Parker, and System entries. It never infers authorship from content. A genuine defect was identified and resolved during this review: Delivered is now completely transcript-silent. Only reply-receiver delivery can create Parker speech.

### Is the graphical launcher provably offline?

Yes. `OfflineOwnerUiMain` directly constructs only scripted `OfflineOwnerInteraction`, `OwnerUiController`, and Compose presentation. It reads no environment, exposes no mode flag, and contains no network, model, server, or runtime reference.

### Did Compose alter Parker's existing runtime behavior?

No. The initial single-module attempt revealed Compose compiler bytecode effects and was rejected. The final `ui-desktop` subproject contains the compiler and runtime dependency. A clean rebuild passes every root structural test. Root `MainKt`, application main, runtime graph, and ordinary `run` responsibility are unchanged.

### Did verification become coupled to live-model evaluation?

No. The existing detached live-model source set and opt-in tasks were untouched. Root and desktop offline lifecycle tasks completed without invoking them.

### Did a privileged capability leak into the UI?

No. The UI surface contains conversation presentation only. There are no memory, evidence, permission, Tool, Agent, Task, audit, model, server, experiment, authentication, voice, RPC, or settings controls.

### Did Unit 2 remain independent of Unit 3-C?

Yes. Work and execution remained local to Windows on `ui/basic-owner-interface`. No Parker server, Ollama, Qwen, experiment state, deployment, push, or merge was accessed or performed.

## Residual boundary

Unit 3 planning may consider presentation-state refinement only under separate authorization. Real ParkerRuntime integration remains explicitly absent and must not be inferred from this completion.

**Decision:** ACCEPTED — NO CONSTITUTIONAL DEFECT REMAINS IN UI UNIT 2.
