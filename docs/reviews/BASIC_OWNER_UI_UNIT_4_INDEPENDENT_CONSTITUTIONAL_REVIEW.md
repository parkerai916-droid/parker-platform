# Basic Owner UI Unit 4 Independent Constitutional Review

## Independent findings

### Did the UI gain new Parker authority?

No. `OwnerInteraction` is unchanged. The graphical surface still submits only owner text and receives typed presentation state.

### Can the adapter call anything except submitOwnerMessage?

No. It receives one function capability accepting only `InboundOwnerMessage`; it has no ParkerRuntime reference, generic facade, reflection dispatcher, or capability map.

### Can it access evidence, memory, permission, model, or runtime internals?

No. Those types and methods are absent from adapter dependencies and are unreachable by its constructor shape. It cannot bypass the existing governed conversation pipeline.

### Is identity composition-owned?

Yes. Fixed typed owner and channel identities are constructor dependencies. The UI supplies only exact text and cannot choose either identity.

### Is reply authorship still exclusive?

Yes. Only `OwnerUiNotificationBridge.notify`, implementing `OwnerNotificationSink`, can invoke the active reply receiver. Delivered maps execution status only and cannot fabricate Parker text.

### Are runtime outcomes honest?

Yes. Mapping is explicit and exhaustive. Rejection reason, failure stage/message, and planning category remain distinct System outcomes. NotRunning alone becomes Unavailable/Stopped and is never presented as a reasoning failure.

### Does the adapter own lifecycle or concurrency expansion?

No. It has no start/shutdown capability. It supports only the frozen single-flight model and defensively rejects concurrent direct use; the notification seam was not altered with correlation metadata.

### Did Compose or CLI/headless behavior change?

No. Composition code imports no Compose type. `ui-desktop` remains isolated, `OfflineOwnerUiMain` remains the safe launcher, and `parker.composition.MainKt` is unchanged. No real graphical launcher was introduced speculatively.

### Was any real model/server execution performed?

No. Tests inject a deterministic function and in-memory notification bridge. No ParkerRuntime graph, network/HTTP class, model, Ollama, Qwen, server, Ubuntu environment, live evaluation, or Unit 3-C task was invoked. Unit 3-C remained untouched and no deployment occurred.

## Verification assessment

Ten focused adapter tests and the full 2,054-test offline suite pass with zero failures or errors. Root and desktop build/check tasks pass, and structural verification proves the capability boundary.

## Independent verdict

PASS. No authority escalation, lifecycle leak, authorship defect, runtime bypass, Compose leak, CLI regression, or experiment interference remains within Unit 4 scope.

