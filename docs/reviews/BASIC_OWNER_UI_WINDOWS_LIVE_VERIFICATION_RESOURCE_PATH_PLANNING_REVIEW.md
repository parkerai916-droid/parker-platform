# Basic Owner UI Windows Live Verification Resource-Path Planning Review

Date: 2026-08-11 (Pacific/Auckland)

## Determination

**A lower-overhead, constitutionally equivalent real launch is available from existing outputs, with operational qualifications:** invoke Java 17 directly with `parker.ui.OwnerUiMainKt` and the already-resolved `ui-desktop` runtime classpath, after stopping Gradle. This enters the same production main function, calls the same `createOwnerUiRuntimeSession(System.getenv())`, constructs the same `ParkerRuntime`/`OwnerUiRuntimeAdapter`/`OwnerUiNotificationBridge`, uses the same environment semantics, and retains the same window-close shutdown path.

The expected launcher-overhead saving is **A — MATERIAL** (several hundred MiB). It is not guaranteed to be sufficient by itself: prior evidence shows a 1.308 GiB pre-submission state, about 708 MiB below the frozen 2.0 GiB gate, while the observed Gradle-only process working sets total roughly 568 MiB. Another bounded live attempt is justified only after additional Windows RAM is freed and the unchanged 2.0 GiB gate passes immediately before submission.

This is a planning determination only. No UI, ParkerRuntime, Ollama, model, HTTP endpoint, or live verification was started or called.

## Baseline and authority

- Repository: `C:\Projects\Parker\parker-platform\parker-platform`.
- Branch: `ui/basic-owner-interface-integration`.
- HEAD: `c8d011c2c730ea0e6515810d9a0e733e2b3d590f`.
- Fetched `origin/main`: `bfa618bece577408b247f76454836947f7257197`.
- Initial state: clean, 16 commits ahead of `origin/main`, nothing staged, no merge/rebase/cherry-pick operation.
- Initial runtime state: no Java, Ollama, or Llama process and no port 11434 listener.

The committed human-in-the-loop review was read fresh. It establishes a real visible Runtime/Ready UI and normal shutdown, but records zero submissions and zero model calls because RAM fell from about 2.127 GiB before Parker launch to about 1.308 GiB before submission. Its independent review correctly classifies that result as partially verified and not merge-ready.

This task authorizes inspection and this one new review only. It does not authorize a live attempt, build, source/test/Gradle change, model call, deployment, stage, commit, push, merge, or rebase.

## Current Gradle launch architecture

`ui-desktop/build.gradle.kts` registers `runOwnerUi` as a Gradle `JavaExec` task:

- main class: `parker.ui.OwnerUiMainKt`;
- classpath: `sourceSets.main.runtimeClasspath`;
- Java toolchain: 17;
- explicit `jvmArgs`: none;
- application/distribution plugin: not applied;
- working directory under the observed Gradle launch: repository root;
- environment: inherited by the forked Java process and read by `OwnerUiMain` through `System.getenv()`.

`OwnerUiMainKt` calls `createOwnerUiRuntimeSession(System.getenv())`. The composition function constructs `ParkerRuntime`, `OwnerUiRuntimeAdapter(runtime::submitOwnerMessage)`, and the same `OwnerUiNotificationBridge` supplied as `OwnerNotificationSink`. Direct invocation of this exact main class does not bypass any governed boundary.

### Processes attributable to the Gradle path

The prior live review preserved one contemporaneous process snapshot after Ready:

| Process role | Observed working set | Required after Ready? |
|---|---:|---|
| hidden `cmd.exe` wrapper | about 9 MiB | No; launch wrapper only |
| Gradle client/wrapper JVM | about 107 MiB | No Parker responsibility |
| single-use Gradle daemon JVM | about 427 MiB | No Parker responsibility after JavaExec fork |
| UI/Parker JVM | about 206 MiB | Yes |

The roles are inferred from start order, process lifetime, Gradle architecture, and the UI task's child-JVM behavior. The exact working sets are observations, not forecasts. The avoidable processes totaled approximately 543 MiB of Java working set plus the 9 MiB command wrapper (roughly 568 MiB in decimal-byte-to-MiB conversion across the recorded values). They exited together when Gradle's JavaExec task completed.

Gradle 8.10 was configured with `--no-daemon`, but that means a disposable single-use daemon, not no daemon process. During the window session both the client and single-use daemon remained alive awaiting the JavaExec child. A direct Java launch removes those resident launcher processes.

## Existing build outputs

Existing, inspected outputs:

| Artifact | Contents/entry point | Assessment |
|---|---|---|
| `ui-desktop/build/libs/ui-desktop.jar` (109,743 bytes) | Contains `parker.ui.OwnerUiMainKt`, `ParkerOwnerWindow`, and offline classes; manifest contains only `Manifest-Version` | Thin UI JAR; viable only with a complete external runtime classpath |
| `ui-desktop/build/classes/kotlin/main` | Compiled `OwnerUiMainKt` and Compose UI classes | Same bytecode used by `runOwnerUi`; viable with runtime classpath but less artifact-like than the JAR |
| `build/libs/parker-platform-0.8.0-runtime-complete.jar` (1,554,478 bytes) | Root Parker production classes; manifest contains no `Main-Class` | Required root project artifact on UI classpath; not itself a UI executable |
| `build/distributions/parker-0.8.0-runtime-complete.zip` / `.tar` | Root distribution and scripts | Existing `parker.bat` launches `parker.composition.MainKt`, not `OwnerUiMainKt`, and omits Compose Desktop dependencies; not the Basic Owner UI |

There is no existing UI executable distribution, UI start script, packaged runtime image, fat/uber UI JAR, or `java -jar` UI artifact. Both inspected JAR manifests lack `Main-Class` and `Class-Path` entries.

## Resolved direct-Java classpath

An offline, disposable Gradle inspection resolved `ui-desktop`'s current `runtimeClasspath` without launching Parker. It contained 60 entries and a 9,504-character Windows classpath:

- `ui-desktop/build/classes/java/main` (currently absent because there is no Java source);
- `ui-desktop/build/classes/kotlin/main`;
- `ui-desktop/build/resources/main` (currently absent because there are no resources);
- `build/libs/parker-platform-0.8.0-runtime-complete.jar`;
- 57 existing cached dependency JARs.

The dependency JARs include Kotlin 1.9.24, coroutines 1.8.1, Compose Desktop 1.6.11, Skiko 0.8.4, `skiko-awt-runtime-windows-x64`, lifecycle/collection/annotation dependencies, and the root Parker runtime's Tika/PDFBox/BouncyCastle/JAXB dependency set. Compose native support is supplied by the existing Windows-x64 Skiko runtime JAR; the Gradle task declares no extra module flags or native-library path.

For a more artifact-shaped direct command, replace the three UI build-output directories at the head of the resolved classpath with the existing `ui-desktop.jar`; retain the root runtime JAR and all 57 dependency JARs unchanged. Every required JAR was already in the Gradle cache. No download or compilation is currently required.

## Launcher equivalence matrix

| Launcher | Classification | Reason |
|---|---|---|
| `:ui-desktop:runOwnerUi` | **CONSTITUTIONALLY EQUIVALENT REAL LAUNCHER** | Current governed development launcher; exact main and classpath, but retains Gradle processes |
| Direct Java 17 `-cp … parker.ui.OwnerUiMainKt` using exact resolved runtime classpath | **EQUIVALENT WITH QUALIFICATIONS** | Same production main/composition/config/shutdown; qualification is fragile explicit classpath tied to current built outputs and cache |
| Existing `ui-desktop.jar` plus root/runtime dependencies and Compose classpath | **EQUIVALENT WITH QUALIFICATIONS** | Same classes/main; no manifest launcher, so classpath must be resolved exactly |
| `java -jar ui-desktop.jar` | **NOT CURRENTLY AVAILABLE** | No `Main-Class` and no packaged dependency classpath |
| Existing root `parker.bat`/distribution | **NOT EQUIVALENT** | Launches `parker.composition.MainKt`; not the graphical Owner UI and lacks Compose dependencies |
| Existing root runtime JAR via `java -jar` | **NOT CURRENTLY AVAILABLE / NOT UI** | No manifest main and no `OwnerUiMainKt` UI packaging |
| `OfflineOwnerUiMainKt` | **NOT EQUIVALENT** | Deterministic offline preview and fake interaction |
| Tests/test launchers/direct HTTP/direct runtime method | **NOT EQUIVALENT** | Bypass visible real UI and/or production composition |
| New Compose distribution or native executable | **NOT CURRENTLY AVAILABLE** | Would require separately governed build configuration/artifact work |

## Memory-overhead assessment

### Required components

- Parker UI/runtime JVM: required; observed around 206 MiB working set at Ready, but total committed/private/system effects are not isolated by working set alone.
- Base Ollama supervisor: required; observed around 51 MiB working set during the human attempt.
- Model worker/load: required only after submission; the fixture preflight observed a detached worker around 484–503 MiB working set and an Ollama response reporting roughly 374 MiB GPU model buffer plus 138 MiB host buffer.

### Avoidable launcher components

- Gradle client JVM, single-use daemon JVM, and command wrapper are not part of Parker's production runtime composition.
- Direct Java removes those processes from the Ready/inference interval.
- Saving classification: **A — MATERIAL**, because the observed launcher-only working sets are several hundred MiB.

### Sufficiency qualification

The prior Ready-to-submission state was about 1.308 GiB, requiring roughly 708 MiB more to reach 2.0 GiB. The observed avoidable launcher processes account for roughly 568 MiB, and process working-set release does not translate one-for-one into available physical RAM. Therefore the direct launch is likely helpful but **not evidence-supported as sufficient on its own**. Additional Windows RAM must be freed before another attempt; the gate must decide, not this forecast.

## Governance assessment

Directly invoking `parker.ui.OwnerUiMainKt` with the exact existing runtime classpath:

- does not alter production code, UI code, tests, or Gradle;
- does not alter `OwnerUiMain` or `createOwnerUiRuntimeSession`;
- does not bypass `OwnerUiRuntimeAdapter`, `ParkerRuntime.submitOwnerMessage`, or `OwnerNotificationSink`;
- does not change endpoint/model/owner/environment loading semantics;
- does not touch Unit 3-C or reopen the paused remedy programme;
- is not deployment; it is an alternate local invocation of already-built development artifacts.

It remains qualified rather than unqualified because the project does not publish this classpath as a supported UI start script/distribution. A missing, stale, reordered, or wrong-platform dependency would invalidate the launch. A future attempt must re-resolve and validate the exact current classpath from a clean baseline rather than globbing arbitrary Gradle-cache JARs.

## Proposed future launch procedure — do not execute in this task

1. Verify the separately authorized baseline is clean and that existing `ui-desktop.jar`/root JAR correspond to that source baseline. If source has changed since those artifacts, perform a separately authorized offline build first, let it finish, then stop Gradle; **no build is required for the currently inspected `c8d011c` outputs**.
2. In one PowerShell coordinator process, resolve `:ui-desktop`'s exact `sourceSets.main.runtimeClasspath.asPath` via a disposable external Gradle inspection script. Replace the UI class/resource entries with the existing absolute `ui-desktop.jar` path, or retain the exact compiled-output entries. Do not use a broad Gradle-cache wildcard.
3. Run `gradlew --stop`; wait for every Gradle client/daemon JVM to exit. The coordinator PowerShell may remain, but no Gradle process should remain during the UI session.
4. Free additional nonessential Windows RAM. Measure available physical RAM and require at least 2.0 GiB before Ollama; a higher initial margin is necessary because direct-launch savings alone are not proven sufficient.
5. Start only the installed Windows Ollama fixture at `127.0.0.1:11434`; verify version, exact model/digest, and loopback-only listener without generation.
6. Set the same isolated `PARKER_*` environment used by the governed human test, including `PARKER_MODEL_ENDPOINT_URL=http://127.0.0.1:11434/api/generate`, exact model, timeout, owner identity, and isolated evidence/audit/memory paths.
7. From repository root with Microsoft/OpenJDK 17, launch the single required JVM conceptually as:

   ```powershell
   & "$env:JAVA_HOME\bin\java.exe" -cp $parkerUiRuntimeClasspath parker.ui.OwnerUiMainKt
   ```

   `$parkerUiRuntimeClasspath` must be the exact validated semicolon-separated current classpath described above, not a hand-curated or wildcard approximation.
8. Expected Ready process set: one Parker UI/runtime Java process and the base Ollama supervisor, plus the coordinating shell; no Gradle client or daemon. After submission, one Ollama `llama-server.exe` worker is expected temporarily.
9. Measure RAM after Gradle stops, before Ollama, after Ollama/before UI, at UI Ready, and **immediately before authorizing submission**. The frozen submission gate remains **at least 2.0 GiB**; stop if it fails.
10. Preserve the existing one-human-message/no-retry, loopback-only, owner-visible safety, normal-close, teardown, and isolation requirements.

## Risks, justification, and next governance step

Risks are classpath staleness, dependence on the local Gradle cache, Windows command-line/classpath length, and insufficient total RAM even after Gradle removal. PowerShell/CreateProcess can accommodate the observed 9,504-character classpath, whereas routing the command through legacy `cmd.exe` risks its shorter command-line limit and should be avoided.

Another bounded attempt is **conditionally justified**, because direct Java removes demonstrated non-Parker overhead while retaining the same main/composition. It is not justified until additional RAM is freed and a pre-attempt estimate indicates a realistic chance of passing the immediate pre-submission gate.

**Exact next governance step:** review and explicitly authorize one human-in-the-loop attempt using the direct-Java procedure above, only after freeing additional Windows RAM. The authorization must preserve the 2.0 GiB immediate pre-submission gate and require a hard stop with zero submissions if it fails. Do not create a distribution or modify Gradle merely for that attempt; a governed desktop distribution may be considered later if a durable supported launcher is desired.
