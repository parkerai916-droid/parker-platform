# Basic Owner UI — Unit 2 Completion Review

**Status:** Complete

## Platform selection

- Compose Desktop plugin: `org.jetbrains.compose` 1.6.11
- Desktop dependency: `compose.desktop.currentOs` 1.6.11
- Kotlin/JVM: 1.9.24, unchanged
- JVM toolchain/runtime: 17 / Microsoft OpenJDK 17.0.19
- Gradle wrapper: 8.10, unchanged
- Required repositories in the isolated desktop project: Maven Central and Google Maven

Compose 1.6.11 was selected as the conservative pre-K2 line. Official metadata and a disposable exact-version spike were followed by successful compilation and execution in this repository. No Kotlin, JVM, or Gradle migration occurred.

## Architecture delivered

Compose is isolated in the `ui-desktop` subproject, which depends one-way on the root plain-Kotlin project. This isolation was required after initial full verification proved that applying the Compose compiler to the root module generated stability metadata on existing Parker classes and broke twelve structural field-count invariants. The corrected architecture leaves contracts, runtime, composition, and Unit 1 bytecode untouched by the Compose compiler.

The root `application` configuration still names `parker.composition.MainKt`. The graphical path is explicit only:

`gradlew.bat :ui-desktop:runOfflineOwnerUi --offline`

## Files added for Unit 2

- `docs/governance/BASIC_OWNER_UI_UNIT_2_SCOPE_LOCK.md`
- `docs/reviews/BASIC_OWNER_UI_UNIT_2_PLANNING_REVIEW.md`
- `docs/reviews/BASIC_OWNER_UI_UNIT_2_SCOPE_LOCK_REVIEW.md`
- `docs/reviews/BASIC_OWNER_UI_UNIT_2_COMPLETION_REVIEW.md`
- `docs/reviews/BASIC_OWNER_UI_UNIT_2_INDEPENDENT_CONSTITUTIONAL_REVIEW.md`
- `src/ui/parker/ui/OwnerWindowPresentation.kt`
- `ui-desktop/build.gradle.kts`
- `ui-desktop/src/main/kotlin/parker/ui/ParkerTheme.kt`
- `ui-desktop/src/main/kotlin/parker/ui/ParkerOwnerWindow.kt`
- `ui-desktop/src/main/kotlin/parker/ui/OfflineOwnerUiMain.kt`
- `tests/ui/OwnerWindowPresentationTest.kt`
- `tests/ui/OfflineOwnerUiLauncherIsolationTest.kt`

## Files modified for Unit 2

- `settings.gradle.kts` includes only the isolated `ui-desktop` project.
- `src/ui/parker/ui/OwnerUiController.kt` now treats Delivered as transcript-silent, as Unit 2 explicitly requires.
- `tests/ui/OwnerUiControllerTest.kt` proves Delivered alone adds no transcript entry beyond the submitted owner message.
- Unit 2 Scope Lock and review documents record the verified module-isolation correction.

The existing `src/composition/Main.kt` was not modified. Unit 1's Windows path-normalization correction remains unchanged and unrelated to UI semantics.

## UI capability

The resizable Parker window provides:

- Parker identity and local-offline presentation label;
- Ready, Processing, Stopped, and Error display;
- scrollable typed transcript;
- visually distinct Owner, Parker, and System presentation;
- multiline owner input;
- Send and Enter submission;
- Shift+Enter newline;
- blank and Processing submission gating;
- restrained processing indicator;
- automatic scrolling; and
- controller shutdown on window close.

The launcher directly constructs deterministic `OfflineOwnerInteraction` scripts and `OwnerUiController`. It has no runtime mode, environment inspection, or hidden connection switch.

## Tests and verification

Eight Unit 2 tests were added. Together with Unit 1 tests they verify typed authorship, Ready/Processing enablement, Enter/Shift+Enter behavior, blank prevention, scrolling decisions, Delivered silence, reply-channel authorship, outcome System status, shutdown, offline launcher construction, and existing-main preservation.

Final clean/offline verification:

- Root `test`, `check`, and `build`: passed.
- `ui-desktop` `check` and `build`: passed.
- 2,039 tests, 0 failures, 0 errors, 8 skipped, 144 suites.
- `git diff --check`: passed.
- Root source/build scan: no Compose runtime/compiler dependency.
- Runtime/contracts/interfaces/composition scan: no `parker.ui` or Compose dependency.
- Desktop launcher scan: no ParkerRuntime, runtime configuration, model, permission, conversation, memory, evidence, network, environment, Ollama, or Qwen reference.
- No live-model or Unit 3-C task was invoked.

## Manual Windows verification

The explicit offline task opened a 980×720 resizable Parker window. Manual verification confirmed:

- transcript and identity rendering;
- owner typing and Enter submission;
- visible Processing state and disabled Send;
- deterministic delayed Parker reply through the fake reply receiver;
- an Enter attempt during Processing did not submit a second message and remained only as draft text;
- automatic Ready restoration;
- Delivered added no status or Parker transcript text;
- normal window close terminated cleanly.

The launch used only `OfflineOwnerInteraction`. No Parker server, runtime, model, Ollama, Qwen, or Unit 3-C access occurred.

## Explicit exclusions

No real ParkerRuntime adapter, privileged capability, remote transport, model/server control, settings system, packaging, installer, deployment, commit, push, or merge was introduced.

**Decision:** UI UNIT 2 ENGINEERING COMPLETE.
