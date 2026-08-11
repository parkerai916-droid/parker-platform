# Basic Owner UI — Unit 2 Planning and Compatibility Review

**Status:** Complete — Unit 2 may proceed to Scope Lock

## Unit 1 baseline

The complete uncommitted Unit 1 working tree was inspected on `ui/basic-owner-interface`. Its Scope Lock, implementation, sixteen tests, Completion Review, and Independent Constitutional Review remain mutually consistent.

The only non-UI change, in `tests/contracts/OcrStructuralIsolationTest.kt`, replaces Windows `\\` path separators with `/` before comparing the unchanged expected path `src/runtime/OcrExecutionSequencer.kt`. The implementation-discovery regex, sole expected implementer, and all OCR production code remain unchanged. The correction is platform-neutral test handling only.

## Build baseline

- Kotlin/JVM: 1.9.24
- JVM toolchain: 17
- Gradle wrapper: 8.10
- Local Windows JDK: Microsoft OpenJDK 17.0.19
- Existing application main: `parker.composition.MainKt`
- Live-model evaluation: detached source set with explicit opt-in tasks, unchanged

## Compose compatibility verification

Official Gradle Plugin Portal metadata identifies `org.jetbrains.compose:1.6.11` as a stable published plugin. JetBrains documentation identifies the 1.6.11 line as based on Jetpack Compose 1.6.7; the 1.6.10/1.6.11 generation uses Compose compiler 1.5.14, compatible with Kotlin 1.9.24. Current Compose releases require K2-era Kotlin, so they are intentionally not introduced here.

A disposable build, separate from Parker's production build, applied exactly Kotlin/JVM 1.9.24 and `org.jetbrains.compose` 1.6.11, selected `compose.desktop.currentOs`, targeted JVM 17, and compiled a minimal Compose Desktop `Window` using the repository's Gradle 8.10 wrapper. `compileKotlin` passed on Windows with JDK 17.0.19. The spike sources were removed afterward.

No Kotlin, Gradle, or JVM migration is required. Compose compiler application and the single `compose.desktop.currentOs` dependency must remain isolated in the dedicated `ui-desktop` subproject so existing Parker bytecode remains unaffected.

**Decision:** READY FOR UI UNIT 2 SCOPE LOCK.
