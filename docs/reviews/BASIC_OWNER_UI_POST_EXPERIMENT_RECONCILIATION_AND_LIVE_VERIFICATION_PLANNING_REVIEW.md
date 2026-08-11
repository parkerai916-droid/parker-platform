# Basic Owner UI Post-Experiment Reconciliation and Live-Verification Planning Review

Date: 2026-08-11 (Pacific/Auckland)
Scope: Windows development checkout inspection and planning only
Repository: `parkerai916-droid/parker-platform`

## 1. Windows environment confirmation

- Checkout: `C:\Projects\Parker\parker-platform\parker-platform`
- Platform/shell: Windows / PowerShell
- Current branch: `ui/basic-owner-interface`
- Local HEAD: `9fd3dbef5da98b5724693380d5dc8d4c5260c70b`
- Remote: `origin` is the GitHub repository only. No Parker-server remote or path was found or accessed.
- Tracking: the UI branch has no configured upstream. Local `main` tracks `origin/main` and is 45 commits behind it.
- Working tree: **not clean before this review began**. It contained 3 modified tracked files and the untracked Units 1–5 implementation, tests, governance/review documents, and `ui-desktop` module. Those pre-existing changes were preserved.
- Network/runtime boundary: the only network action was `git fetch --prune origin` against GitHub. No Parker server, Ollama/Qwen instance, model endpoint, or live runtime was contacted.

The requested final expectation of “exactly one new untracked Planning Review document” cannot literally be met from this starting state without altering, committing, stashing, or deleting pre-existing UI work, all of which are outside this inspection-only authorization. This review is the sole file created by this task.

## 2. Current main baseline

After a read-only fetch of remote refs:

- `origin/main`: `bfa618bece577408b247f76454836947f7257197`
- Subject: `governance: pause reasoning protocol remedy programme`
- This exactly matches the expected baseline.
- The checked-out local `main` ref remains unchanged at `9fd3dbef5da98b5724693380d5dc8d4c5260c70b`.

The current governance disposition is: **NO REMEDY SELECTED; Unit 4 NOT AUTHORIZED; production reasoning unchanged**.

## 3. UI branch baseline

- Branch: `ui/basic-owner-interface`
- Branch ref/HEAD: `9fd3dbef5da98b5724693380d5dc8d4c5260c70b`
- Upstream: none
- Commits unique to the UI branch: **0**
- UI implementation state: present only as modified/untracked working-tree content, not in commits reachable from the branch ref.
- Cleanliness: **dirty**. Before this review, tracked modifications were `build.gradle.kts`, `settings.gradle.kts`, and `tests/contracts/OcrStructuralIsolationTest.kt`; the UI source, desktop module, UI tests, and Units 1–5 governance/review records were untracked.

Consequently, “UI branch HEAD” identifies the pre-UI commit, not a commit containing Units 1–5. The current UI cannot be reconstructed from Git history alone.

## 4. Merge-base and divergence

- Merge-base of `ui/basic-owner-interface` and `origin/main`: `9fd3dbef5da98b5724693380d5dc8d4c5260c70b`
- UI-only commits: 0
- Main-only commits: 45
- Commit divergence (`git rev-list --left-right --count`): `0 45`
- Total committed divergence: 45 commits

The uncommitted UI working tree is additional, non-commit divergence and must be preserved explicitly before any later reconciliation.

## 5. Basic Owner UI Units 1–5 reconstruction

Reconstruction below is from source, build files, and tests rather than prior summaries.

1. **Capability and outcome boundary.** `OwnerInteraction` defines availability and a single suspending `submit(ownerText, onReply)` capability. Typed dispositions cover Delivered, NotAccepted, Failed, Planned, and Unavailable. `OwnerReply` is separate from disposition/status text so only the reply seam creates Parker-authored transcript entries.
2. **Offline interaction and controller.** `OfflineOwnerInteraction` provides deterministic scenarios without runtime, model, network, environment, or Compose dependencies. `OwnerUiController` owns transcript/state, rejects blank or concurrent submissions, launches work off the presentation thread, distinguishes Owner/Parker/System authorship, presents every terminal outcome honestly, sanitizes unexpected failures, and cancels owned work on shutdown.
3. **Graphical owner window.** Compose Desktop renders header/status, typed transcript bubbles, empty state, processing indication, multiline input, Send, Enter-to-submit, Shift+Enter newline, submission disabling, and transcript-end scrolling. A dedicated offline launcher uses only the deterministic interaction.
4. **Runtime adapter.** `OwnerUiRuntimeAdapter` constructs an `InboundOwnerMessage` with configured owner principal and local-text channel, then invokes only an injected `suspend (InboundOwnerMessage) -> ParkerRuntimeOutcome` capability. It maps runtime outcomes exhaustively. `OwnerUiNotificationBridge`, supplied to both runtime and adapter, forwards runtime notifications to the active submission reply callback.
5. **Real launcher and lifecycle.** `createOwnerUiRuntimeSession` loads ordinary production configuration, constructs the existing `ParkerRuntime`, injects `runtime::submitOwnerMessage`, starts it explicitly, and makes shutdown idempotent. The real Compose launcher has Starting/Ready/Failed states and closes in the fixed order controller cancellation then runtime shutdown.

Build integration adds `src/ui` and `tests/ui` to the root source sets, includes the `ui-desktop` subproject, and defines separate `runOfflineOwnerUi` and `runOwnerUi` JavaExec tasks. The desktop module uses Kotlin JVM 1.9.24, Compose 1.6.11, and JDK toolchain 17. It does not replace or alter the existing headless application main.

UI-specific verification comprises 45 tests across seven classes: 10 adapter, 6 runtime-composition, 12 controller, 5 outcome-completeness, 4 offline-interaction, 2 launcher-isolation, and 6 presentation tests. The Windows path normalization change in `OcrStructuralIsolationTest` is ancillary Windows test compatibility, not UI authority.

## 6. Current UI architecture map

```text
Compose ParkerOwnerWindow
  -> OwnerUiController
  -> OwnerInteraction
  -> OwnerUiRuntimeAdapter
  -> injected ParkerRuntime::submitOwnerMessage capability
  -> existing governed conversation/runtime pipeline

existing runtime reply delivery
  -> OwnerNotificationSink
  -> OwnerUiNotificationBridge (active single-flight receiver)
  -> OwnerReply callback
  -> OwnerUiController Parker transcript entry
  -> Compose display
```

The offline launcher substitutes `OfflineOwnerInteraction` at the same `OwnerInteraction` seam and never constructs a runtime graph.

## 7. Governance-boundary result

**Verdict: conforms to the owner-facing-adapter principle in the inspected source.**

The UI introduces no reasoning authority and does not modify constitutional/runtime behaviour. It does not bypass the existing owner-message path, permission/governance checks, or response-delivery seam. It has no direct access to Memory, Goals, Knowledge Submission, Planner, tools, `ReasoningProvider`, `LocalHttpModelInferenceClient`, Ollama, Qwen, or any provider endpoint. The real composition constructs the unchanged runtime and grants the adapter only `runtime::submitOwnerMessage`; replies arrive through the runtime’s existing `OwnerNotificationSink` constructor seam.

The UI does import `GoalPlanningHandoffOutcome` and `PlanningSessionResult` only to convert an already-returned `ParkerRuntimeOutcome.Planned` into a presentation category. This observes a runtime result; it does not reach Goals or Planner or create authority. No source dependency on Unit 3-C experiment classes, tasks, environment gates, results, or remedy mechanisms was found. The implementation assumes no remedy outcome.

One presentation defect should be reconciled before live proof: shared window copy says “local offline preview” and describes deterministic offline interaction even when invoked by the real-runtime launcher. This is misleading mode labelling, not an authority bypass.

## 8. Relevant main changes since divergence

The 45 main-only commits are overwhelmingly governance/review records for Reasoning Protocol Units 2-D through 3-E. Code/build-relevant changes are limited to:

- `build.gradle.kts`: adds three detached, explicit opt-in live-model/experiment Test tasks (`reasoningProtocolUnit2DDiagnostic`, `unit3cControlledRemedyExperiments`, plus their supporting source-set task configuration over the series).
- `tests/integration/ReasoningProtocolDiagnosticCharacterisationTest.kt`: new detached diagnostic instrument.
- `tests/integration/ReasoningProtocolUnit3CControlledRemedyExperimentsTest.kt`: new detached controlled-experiment instrument and later corrections.
- `tests/integration/ReasoningProtocolUnit3COrchestrationTest.kt`: new detached orchestration instrument and later corrections.

No main-only change modifies runtime entry points, `ParkerRuntime`, `submitOwnerMessage`, `OwnerNotificationSink`, runtime composition, conversation APIs, reply delivery, lifecycle/shutdown, source package locations, Kotlin/JVM versions, production dependencies, or ordinary test infrastructure. The closing main commit formally pauses the remedy programme without changing production reasoning.

## 9. Compatibility-impact matrix

| Main change area | Classification | UI impact |
|---|---|---|
| Governance and review documents | NO IMPACT | Records constraints and NO REMEDY; no executable interface change. |
| Detached Unit 2-D/3-C integration tests | NO IMPACT | Not in ordinary `test`/`build`; UI does not reference them. |
| Root `build.gradle.kts` additions | REQUIRES UI RECONCILIATION | Main and UI both edit the same file. Content is conceptually additive/compatible, but the reconciled file must retain both main’s opt-in tasks and UI’s source-set additions. |
| Runtime owner-message interface | NO IMPACT | Signature and package unchanged since merge-base. |
| `OwnerNotificationSink` and response delivery | NO IMPACT | No committed changes since merge-base. |
| Runtime composition and lifecycle | NO IMPACT | No committed changes since merge-base. |
| Kotlin/JVM/dependencies/package moves | NO IMPACT | Root Kotlin 1.9.24 and JDK 17 remain unchanged; no relevant moves. |
| Remedy disposition | COMPATIBLE CHANGE | NO REMEDY confirms the UI must use unchanged production runtime behaviour; current UI is independent of experiment mechanisms. |

**Overall compatibility verdict:** source/API compatible, with a small but mandatory manual Gradle reconciliation. There is no identified runtime breaking change. The dirty, uncommitted UI state is the larger reconciliation risk.

## 10. Current test/build baseline

### Verification attempted in this review

Command:

```text
.\gradlew.bat test --tests "parker.ui.*" --tests "parker.composition.OwnerUiRuntimeAdapterTest" --tests "parker.composition.OwnerUiRuntimeCompositionTest" --console=plain
```

Result: Gradle did not start because `JAVA_HOME` is unset and no `java` executable is on `PATH`. Checks of the common Android Studio JBR location and the user Gradle JDK cache found no usable Java executable. Classification: **ENVIRONMENT ISSUE**. No tests compiled or executed in this task, so current-pass counts are 0 executed and the current build/launcher compile status is **not re-verified**.

### Preserved historical local artifacts (not a current run)

Existing XML stamped 2026-08-10 23:56:15 records:

- Targeted owner UI/runtime boundary: **45 tests, 45 passed, 0 failed, 0 errors, 0 skipped**.
- Full ordinary repository test task: **2,060 tests, 2,052 passed, 0 failed, 0 errors, 8 skipped** across 147 suites.
- Compiled class outputs exist for both root `parker-platform` and `ui-desktop`, including both graphical launcher main classes.

These artifacts support the previous baseline but are not authoritative for the fetched-main reconciliation and do not replace a new run with JDK 17. No live-model, Unit 3-C, server, or graphical execution task was run.

## 11. Recommended reconciliation strategy

Recommend **C, with a prerequisite preservation step**: first place the complete current Units 1–5 working tree into reviewed, atomic UI commits; then create a fresh integration branch from `origin/main @ bfa618bece577408b247f76454836947f7257197` and cherry-pick those UI commits in order. Resolve `build.gradle.kts` by retaining both current-main experiment-task additions and the UI source-set additions. Do not cherry-pick obsolete generated build artifacts.

This is safer than merging or rebasing now because the branch ref contains zero UI commits: merge/rebase would move only the old ref and leave the actual UI as an opaque dirty overlay. A fresh-main integration branch makes the production baseline explicit, gives each UI unit auditable provenance, confines the likely conflict to additive Gradle content, permits verification after each unit, preserves current main, and permits rollback by dropping individual UI commits or the integration branch. A merge can be reconsidered only after the UI exists in commits; rebasing offers little benefit and rewrites the newly established audit trail.

No reconciliation operation was performed in this review.

## 12. Exact live-verification definition

Later live verification must use current production runtime behaviour unchanged and must not enable any Unit 3-C gate or assume experimental behaviour. Minimum proof:

1. On Windows with JDK 17 and explicit local runtime configuration, compile/package and launch `:ui-desktop:runOwnerUi` (or the reproducible packaged equivalent), not the offline launcher.
2. Record that startup constructs the ordinary `ParkerRuntime`, reaches Ready, and exposes only `OwnerInteraction` to the controller.
3. Submit a uniquely identifiable owner message through the visible UI and trace the exact `InboundOwnerMessage` into `ParkerRuntime.submitOwnerMessage`, including configured owner principal, local-text channel, correlation ID, and timestamp.
4. Capture runtime evidence that normal identity, communication intake, conversation continuity, reasoning, permission/governance, execution/planning, and reply-delivery stages remain in control; no UI-specific bypass may appear.
5. For a reply-producing outcome, prove runtime text calls the configured `OwnerNotificationSink`, crosses `OwnerUiNotificationBridge`, becomes a typed Parker transcript entry, and renders once in order.
6. Exercise or deterministically induce Delivered-without-reply, NotAccepted, Planned, Failed, Unavailable/startup-failure, blank input, and concurrent-submit prevention; verify the UI labels system status as system status and never fabricates Parker speech.
7. Close during idle and during an active submission; prove UI work is stopped/cancelled before one idempotent runtime shutdown and the process exits cleanly.
8. Capture source/trace evidence that the UI never directly accesses Memory, Goals, Knowledge Submission, Planner, tools, model/provider, or network APIs.
9. Verify configuration paths are Windows-valid and no server filesystem, shell, or service assumption is embedded.
10. Confirm no Unit 3-C environment enablement, experimental test task, experimental class, or remedy-specific configuration participates.

Offline UI demonstrations and mocked adapter tests are useful prerequisites but are not live end-to-end proof.

## 13. Server-dependency determination

Classification: **A — fully verifiable on Windows without the Parker server**, provided Windows has a deliberately configured compatible model endpoint and writable local evidence/audit/memory paths.

The real UI constructs `ParkerRuntime` in the Windows process. `PARKER_MODEL_ENDPOINT_URL` and `PARKER_MODEL_NAME` are required injected configuration; the source contains no hard-coded Parker-server host, remote filesystem path, Ollama name, or Qwen name. Therefore the governed owner-message and notification path can be proven with a Windows-local compatible model service and local durability paths. The UI itself is model-agnostic and has no direct model dependency.

A later Parker-server check is not architecturally required to verify the UI adapter. If operations ultimately choose a remote production endpoint, that separate deployment/integration check would test only endpoint reachability, credentials/network policy if any, latency/timeout suitability, and production-path configuration—not UI authority or routing correctness. It requires separate authorization.

## 14. Merge-readiness criteria

The UI is not merge-ready until all of the following are true:

- the pre-existing dirty Units 1–5 work is reviewed and preserved in intentional commits;
- reconciliation onto `origin/main @ bfa618bece577408b247f76454836947f7257197` is complete, including both sets of root Gradle additions;
- the reconciled diff shows no architecture/governance drift and no change to production reasoning;
- all 45 targeted UI/runtime-boundary tests pass on a current JDK 17 run;
- relevant runtime-interface tests and all ordinary repository tests pass, with skips explained;
- root build and desktop compile/package tasks succeed reproducibly;
- real owner-message live verification meets every proof item in Section 12 using unchanged production behaviour;
- UI remains model/provider-agnostic, with no direct Ollama/Qwen or Unit 3-C dependency;
- mode copy accurately distinguishes real runtime from offline preview;
- Windows-local configuration and shutdown operation are documented sufficiently for another operator;
- final code, governance, and merge review is complete; and
- main remains unchanged until the separately authorized merge.

## 15. Blocking issues

1. **UI is not committed.** The branch ref contains zero UI commits and the working tree was already dirty. Reconciliation cannot be made auditable or safely cherry-picked until the existing work is preserved in reviewed commits under separate authorization.
2. **No JDK available in this shell.** Current tests/build/desktop compilation could not run. Install or point `JAVA_HOME` to JDK 17 before relying on verification results.
3. **Current-main reconciliation not performed.** Root Gradle changes must be combined and the full reconciled baseline tested.
4. **Live proof remains outstanding by instruction.** Historical offline tests do not establish real runtime end-to-end operation.

## 16. Non-blocking issues

- Shared UI copy labels the real-runtime window as a “local offline preview”; make mode labelling truthful during reconciliation.
- The adapter exposes runtime failure `cause.message` as `safeMessage`. Existing tests assume this is presentation-safe, but reconciliation review should verify the runtime exception-message contract before live use.
- The real UI has no distribution/installer declaration beyond Compose compilation and JavaExec tasks; decide whether `runOwnerUi` is sufficient for the first merge or whether reproducible distributable packaging is required.
- The branch has no upstream tracking configuration. This does not affect local inspection but should be intentional before publication.
- Prior Units 1–5 documents are untracked and should be reviewed for inclusion rather than assumed authoritative.

## 17. Exact next implementation/verification step

After explicit implementation authorization, first restore a usable JDK 17 and run a preservation gate on the untouched UI working tree: targeted 45 tests, relevant runtime tests, full ordinary `test`, root `build`, and `:ui-desktop:compileKotlin`/packaging verification. If green, review and commit the existing Units 1–5 changes as atomic, auditable UI commits without including this planning review accidentally in unrelated units. Then create a fresh integration branch from `origin/main @ bfa618bece577408b247f76454836947f7257197`, cherry-pick those UI commits, manually retain both sets of additive `build.gradle.kts` changes, and repeat the offline verification before seeking authorization for live verification.

No implementation, commit, branch switch, merge, rebase, cherry-pick, staging, push, deployment, server access, model call, or live verification was performed here.
