# Basic Owner UI Units 1–5 Preservation and Current-Main Reconciliation Review

Date: 2026-08-11 (Pacific/Auckland)  
Repository: `parkerai916-droid/parker-platform`  
Scope: Windows preservation, current-main reconciliation, and offline verification only

## 1. Windows environment

- Windows checkout: `C:\Projects\Parker\parker-platform\parker-platform`
- Starting branch: `ui/basic-owner-interface`
- Starting HEAD: `9fd3dbef5da98b5724693380d5dc8d4c5260c70b`
- Remote: GitHub `origin` only (`https://github.com/parkerai916-droid/parker-platform.git`)
- No Parker server path, remote, model endpoint, Ollama/Qwen instance, or live runtime was accessed.

## 2. Starting UI branch state

The branch ref contained no UI commits. The working tree contained the completed Units 1–5 as three modified tracked files and untracked source, tests, desktop module, and governance/review records. The prior post-experiment planning review was also untracked. There was no unexpected unrelated change, so preservation proceeded.

Modified tracked files:

- `build.gradle.kts`
- `settings.gradle.kts`
- `tests/contracts/OcrStructuralIsolationTest.kt`

Untracked implementation groups:

- `src/ui/parker/ui`: five plain-Kotlin UI boundary/controller/presentation files
- `src/composition/OwnerUiRuntimeAdapter.kt`
- `src/composition/OwnerUiRuntimeComposition.kt`
- `tests/ui`: five test files
- two composition boundary test files
- `ui-desktop`: subproject build file and four Compose Desktop source files
- Units 1–5 scope locks and their planning, scope, completion, boundary/gate, and independent reviews

Generated `build/`, `.gradle/`, compiled classes, JARs, reports, caches, and local environment files were not staged or committed.

## 3. Pre-preservation forensic inventory

Before staging, `git diff --stat`, `git diff --name-status`, `git status --short`, and SHA-256 hashes of important UI files were captured. Representative hashes were:

| File | SHA-256 |
|---|---|
| `build.gradle.kts` | `2A7716C6E0B3A99C64A1C778C9BAF220DF4B93CAC643C903E5F5614C0C2CF913` |
| `src/ui/parker/ui/OwnerInteraction.kt` | `3967D220EFC5F2CA26D1792A4440A35EBE7983CE0AE7F3E78181F2F3BC2B49A4` |
| `src/ui/parker/ui/OwnerUiController.kt` | `7BB8705AF4415092ABD424F1FD768CEEBD6C04A4AB702B0C5AC9ABF2F147AF88` |
| `src/composition/OwnerUiRuntimeAdapter.kt` | `3DD65666088D8D0B5D2340E0C821582882D6F32B21CEDBC219A7FABF61CE223D` |
| `src/composition/OwnerUiRuntimeComposition.kt` | `6C48DF6C8EF335A10B3664229B83D1DCA36FE67F9950E99CC2E018865E681597` |
| `ui-desktop/src/main/kotlin/parker/ui/OwnerUiMain.kt` | `3D013ABE6F37DAEF1645B29D2C60B6D2CAE909E38D78CE0C83EC54E29F658499` |
| `ui-desktop/src/main/kotlin/parker/ui/ParkerOwnerWindow.kt` | `40D1B5F7A920B337B17F945CF3E07A249A773667BA1A4F38E5507DE86E449544` |

After cherry-picking, a scoped diff between the preserved branch and integration branch showed no difference in UI source, UI tests, settings, or `ui-desktop`. The only expected branch-level implementation difference is current-main content retained in root `build.gradle.kts`.

## 4. JDK 17 result

No usable Java was initially present in `PATH`, `JAVA_HOME`, checked registry locations, Android Studio JBR, or the Gradle JDK cache. Windows Package Manager was unavailable. An official Eclipse Temurin JDK archive was downloaded from the Adoptium API and extracted locally.

- Vendor/runtime: Eclipse Temurin
- Version: OpenJDK `17.0.20`, Temurin build `17.0.20+8`
- `JAVA_HOME`: `C:\Users\steve\.codex\jdks\temurin-17\jdk-17.0.20+8`
- Java executable: `C:\Users\steve\.codex\jdks\temurin-17\jdk-17.0.20+8\bin\java.exe`
- Compiler: `javac 17.0.20`
- Process-local Gradle cache: `C:\Users\steve\.gradle`

No project Java, Kotlin, Gradle, or Compose version was changed.

## 5. Pre-preservation test/build result

All tasks ran offline and without a configured runtime/model endpoint.

| Verification | Result |
|---|---|
| Targeted UI/runtime-boundary suite | PASS — 45 tests, 0 failures, 0 errors, 0 skipped |
| Ordinary `test` | PASS — 2,060 tests, 0 failures, 0 errors, 8 skipped, 147 suites |
| Root `build` | PASS |
| `:ui-desktop:compileKotlin` | PASS; both graphical main classes compiled |
| `:ui-desktop:jar` / desktop build | PASS |

The first two environment attempts failed before Gradle execution because Java was absent and then because the inherited Gradle cache resolved to `C:\.gradle`; both were classified as Windows/JDK environment issues. After setting the local JDK and user Gradle cache, no implementation or build-configuration failure occurred.

## 6. Units 1–5 preservation matrix

| Unit | Files/purpose | Tests | Dependencies |
|---|---|---|---|
| 1 | Root UI source-set entries; Windows OCR test path normalization; `OwnerInteraction`, typed state, controller, deterministic offline interaction; Unit 1 records | `OwnerUiControllerTest`, `OfflineOwnerInteractionTest` | Existing plain Kotlin/coroutines runtime project |
| 2 | `ui-desktop` inclusion/build, presentation helpers, theme, window, offline launcher; Unit 2 records | `OwnerWindowPresentationTest`, `OfflineOwnerUiLauncherIsolationTest` | Unit 1 boundary/controller |
| 3 | Outcome ordering, recovery, repeat, and stopped-state coverage; Unit 3 records | `OwnerUiOutcomeCompletenessTest` | Units 1–2 behaviour |
| 4 | Narrow `submitOwnerMessage` adapter and notification bridge; Unit 4 records | `OwnerUiRuntimeAdapterTest` | Unit 1 `OwnerInteraction`; existing composition interfaces |
| 5 | Runtime session/composition, real launcher, lifecycle tests; Unit 5 records | `OwnerUiRuntimeCompositionTest` | Units 1, 2, and 4 |

Two final-state files could not honestly be split into reconstructed intermediate bytes: Unit 1 controller/tests include Unit 2’s Delivered-silence refinement, and `ui-desktop/build.gradle.kts` contains the foundational Unit 2 module plus Unit 5’s real launcher task. Each was committed once at its foundational unit. No invented reverse-engineered history was created.

## 7. Preservation commits created

On `ui/basic-owner-interface`:

1. `4106b7a` — `feat(ui): preserve Basic Owner UI Unit 1 boundary`
2. `755f3c3` — `feat(ui): preserve Basic Owner UI Unit 2 desktop window`
3. `c18243e` — `test(ui): preserve Basic Owner UI Unit 3 outcome coverage`
4. `50004d8` — `feat(ui): preserve Basic Owner UI Unit 4 runtime adapter`
5. `870bf38` — `feat(ui): preserve Basic Owner UI Unit 5 runtime launcher`

Each commit was inspected with `git show --stat`; no generated, campaign, environment, or unrelated file entered any commit.

## 8. Verification after preservation

The committed preservation branch was freshly re-run:

- Targeted suite: PASS, 45/45
- Ordinary suite: PASS, 2,060 tests; 0 failures; 0 errors; 8 skipped
- Root build: PASS
- Desktop compile/JAR/build: PASS

Results match the untouched working-tree baseline. Preservation introduced no semantic change.

## 9. Current origin/main used

`origin` was fetched at task start and again immediately before branch creation. Both fresh reads returned:

`bfa618bece577408b247f76454836947f7257197` — `governance: pause reasoning protocol remedy programme`

This exact commit, not an assumed historical value, was used as the integration base.

## 10. Integration branch

- Branch: `ui/basic-owner-interface-integration`
- Created directly from: `origin/main @ bfa618bece577408b247f76454836947f7257197`
- Tracking: `origin/main` (local branch is five UI commits ahead before this review commit)
- Local `main` was not checked out, moved, merged, rebased, or modified.

## 11. Cherry-pick sequence

| Preserved commit | Integration commit | Result |
|---|---|---|
| `4106b7a` | `a0a54f7` | Success; root Gradle hunk auto-merged additively |
| `755f3c3` | `00d87d0` | Success |
| `c18243e` | `ab910f5` | Success |
| `50004d8` | `b0461ab` | Success |
| `870bf38` | `c357000` | Success |

## 12. Conflicts and resolutions

No cherry-pick produced a conflict or required an architectural decision. Git auto-merged the Unit 1 `build.gradle.kts` source-set hunk because main’s experiment-task additions occur later in the file. The result was inspected rather than accepted by assumption.

## 13. build.gradle.kts reconciliation

The reconciled root build retains both required sets of changes:

- UI: `src/ui` and `tests/ui` remain in ordinary Kotlin source sets.
- Current main: `reasoningProtocolUnit2DDiagnostic` and `unit3cControlledRemedyExperiments` remain detached explicit tasks, alongside the existing live-model evaluation task.

No experiment task was wired into `test`, `check`, `build`, desktop tasks, or either UI launcher. No Unit 3-C property was enabled.

## 14. Current-main compatibility audit

Result: **compatible**.

- `ParkerRuntime`, `submitOwnerMessage`, `OwnerNotificationSink`, conversation APIs, response delivery, and lifecycle signatures remain unchanged from the UI’s dependency baseline.
- Owner text remains `Compose -> OwnerUiController -> OwnerInteraction -> OwnerUiRuntimeAdapter -> ParkerRuntime::submitOwnerMessage`.
- Reply text remains `runtime delivery -> OwnerNotificationSink -> OwnerUiNotificationBridge -> reply callback -> typed Parker transcript -> Compose`.
- UI source has no direct Memory, Goal, Knowledge Submission, Planner, tool, model/provider, Ollama, Qwen, network, or Unit 3-C invocation.
- Source and commit comparison confirms UI implementation intent was preserved byte-for-byte through reconciliation.

## 15. Post-reconciliation tests/build

| Verification | Result |
|---|---|
| Targeted UI/runtime-boundary suite | PASS — 45 tests, 0 failures, 0 errors, 0 skipped |
| Ordinary `test` | PASS — 2,060 tests, 0 failures, 0 errors, 8 skipped, 147 suites |
| Root `build` | PASS |
| Desktop compile | PASS |
| Desktop JAR/build | PASS |

The ordinary task graph showed only ordinary root/desktop compilation and tests. It did not execute the Unit 2-D diagnostic, Unit 3-C controlled experiments, or any live-model task.

## 16. Governance/architecture boundary result

**PASS.** The UI remains an owner-facing adapter over existing governed interfaces. It creates no parallel reasoning, permission, execution, memory, planning, knowledge, tool, or model authority. Current main’s **NO REMEDY SELECTED**, Unit 4 not-authorized, and production-reasoning-unchanged disposition is preserved. No campaign artifact was modified by the UI commits or reconciliation.

## 17. Non-blocking issue disposition

| Item | Classification | Result |
|---|---|---|
| Real runtime shares “local offline preview” copy | STILL PRESENT — NON-BLOCKING | Misleading mode copy remains; no behaviour or authority impact. Correct under separate UI authorization before operational release. |
| Runtime `cause.message` becomes `safeMessage` | REQUIRES SEPARATE GOVERNANCE/IMPLEMENTATION | Existing contract assumes permitted messages; verify/sanitize policy separately before production exposure. |
| No installer/native distribution declaration | NOT ACTUALLY A DEFECT for Units 1–5 | Native packaging/installers were explicitly excluded; reproducible compile and JAR/task launch are green. Product distribution remains later scope. |
| Original UI branch has no upstream | STILL PRESENT — NON-BLOCKING | Local preservation is complete; publication policy remains separate. |
| Units 1–5 records were untracked | RESOLVED BY PRESERVATION | Records are included in their corresponding commits. |

## 18. Live-verification readiness

Classification: **READY FOR LIVE VERIFICATION**, subject to separate explicit authorization.

All offline prerequisites are satisfied: auditable unit commits, current-main integration, green targeted/full tests, green root/desktop builds, intact adapter boundary, detached experiment tasks, and understood Windows JDK/configuration requirements. The two presentation/safe-message issues above should be recorded during the live-verification authorization but do not prevent exercising and proving the existing governed path.

## 19. Blockers

No offline preservation or reconciliation blocker remains. Live execution itself remains prohibited by this task and requires a separately scoped authorization. Merge, push, deployment, mode-copy correction, and failure-message policy work also remain outside this task.

## 20. Exact next step

Hard stop on this local integration branch. Seek a separate live-verification authorization that uses current production runtime behaviour unchanged on Windows, explicitly configures local runtime paths/model endpoint, traces one UI submission through `ParkerRuntime.submitOwnerMessage` and one reply through `OwnerNotificationSink`, exercises honest non-success outcomes and shutdown, and makes no Unit 3-C or remedy assumption. Do not merge or deploy before that evidence and subsequent merge review are complete.

No branch was pushed. No live owner message, live model, Parker server, Ollama/Qwen, `/api/generate`, Unit 2-D diagnostic, or Unit 3-C task was invoked.
