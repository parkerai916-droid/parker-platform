# Basic Owner UI — Unit 1 Independent Constitutional Review

**Status:** Accepted

## Independent questions

### Did Unit 1 create decision authority?

No. `OwnerUiController` performs presentation sequencing only. Neither the port, state, controller, nor fake can reason, authorise, execute, or inspect governed state.

### Can `parker.ui` bypass `ParkerRuntime` or invoke internals?

No. It has no `ParkerRuntime` reference and no runtime, coordinator, permission, execution, model, memory, evidence, Tool, environment, or network dependency. Future real integration remains reserved to `parker.composition`.

### Did a generic command or RPC abstraction appear?

No. The only operation is `submit(ownerText, onReply)` on `OwnerInteraction`. It cannot name a capability or arbitrary runtime method.

### Can Delivered fabricate Parker speech?

No. `Delivered` contains only `executionStatus`. The controller adds a typed System entry for that status and can add a typed Parker entry only from the separate reply receiver.

### Can system text masquerade as Parker-authored text?

No. Authorship is a sealed structural distinction: Owner, Parker, and System are separate types. Rejection, failure, planning, delivery status, stopped state, and unexpected failure all use System entries. The review found and corrected one disclosure risk: unexpected exceptions now produce fixed safe system text rather than exposing a raw exception message.

### Is single-flight structurally enforced?

Yes. The controller rejects a second start while `submissionActive`; the fake independently uses an atomic active guard and rejects concurrent direct calls. Deterministic tests prove both layers.

### Can the offline path accidentally access live runtime or Unit 3-C?

No. The fake's complete dependency surface is in-memory collections, atomics, and injected coroutine delay. It reads no files, environment, endpoints, network, model, runtime configuration, or server state.

### Did ordinary verification become coupled to live evaluation?

No. Only existing main/test source sets gained UI directories. The detached live-model source set and its opt-in tasks were unchanged. Ordinary `test`, `check`, and `build` passed without invoking them.

### Did work remain independent and local?

Yes. Work occurred solely in the Windows checkout on `ui/basic-owner-interface`. No server, Ollama, Qwen, Unit 3-C, deployment, push, merge, or main-branch modification occurred.

## Residual boundary

UI Unit 2 may consume this boundary for Compose presentation only after separate planning. It must not weaken authorship typing, single-flight enforcement, offline-default behaviour, or dependency direction. Real runtime adaptation remains a later composition-layer unit.

**Decision:** ACCEPTED — NO CONSTITUTIONAL DEFECT REMAINS IN UI UNIT 1.
