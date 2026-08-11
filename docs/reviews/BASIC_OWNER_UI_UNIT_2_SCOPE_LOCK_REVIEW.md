# Basic Owner UI — Unit 2 Scope Lock Review

**Status:** Accepted for implementation

The Unit 2 Scope Lock was reviewed against the complete Unit 1 boundary, current controller and fake, existing `Main.kt`, Gradle source sets, detached live-model tasks, and the verified Compose compatibility result.

The review confirms:

- Unit 1 authority does not expand.
- Compose receives immutable presentation state and emits only owner submission intent.
- The controller remains the single-flight authority.
- Authorship remains structural, never text-inferred.
- Delivered cannot create Parker speech.
- The graphical launcher is explicitly and exclusively offline.
- `parker.composition.MainKt` and ordinary `run` remain unchanged.
- No runtime, model, network, privileged capability, packaging, or generic settings surface is admitted.
- Ordinary lifecycle tasks remain independent of opt-in live-model tasks.

No Scope Lock defect was found.

## Verification correction

The first full-build verification showed that applying the Compose compiler to the root JVM project adds generated stability metadata to existing Parker bytecode, violating twelve established structural field-count tests. The Scope Lock was therefore tightened to require an isolated `ui-desktop` subproject. Compose now transforms only graphical presentation classes; Unit 1, contracts, runtime, and composition remain plain Kotlin. This correction narrows dependency impact and grants no new authority.

**Decision:** SCOPE LOCK ACCEPTED — UI UNIT 2 IMPLEMENTATION AUTHORISED.
