# Basic Owner UI Live Verification Blocker Resolution Planning Review

Date: 2026-08-11 (Pacific/Auckland)

## 1. Status and scope

Status: **PLANNING COMPLETE — IMPLEMENTATION NOT AUTHORIZED BY THIS TASK**.

This review defines the minimum lawful resolution of the three blockers recorded by the Windows live-verification reviews. It does not install or start a model runtime, make an API/model call, change source, reopen the Reasoning Protocol remedy programme, or authorize live verification.

## 2. Baseline and repository state

- Repository: `C:\Projects\Parker\parker-platform\parker-platform`
- Branch: `ui/basic-owner-interface-integration`
- HEAD: `06a5703cf0bd4121dd0a6b76ff78fc3d35e710d7`
- HEAD subject: `governance: record owner UI live verification blockers`
- `origin/main`: `bfa618bece577408b247f76454836947f7257197`
- Divergence at start: integration 8 commits ahead, main 0 commits ahead
- Worktree at start: clean
- Remote: GitHub `origin` only

The Units 1–5 implementation and reviews, preservation/current-main reconciliation review, and both committed Windows live-verification reviews were read fresh. No Parker-server path, remote, process, endpoint, credential, or artifact was accessed.

## 3. Blocker 1 — Windows-local model endpoint

### 3.1 Does Parker require Ollama?

Parker does **not** require the Ollama executable, daemon, brand, or model catalogue as an architectural dependency. `ModelReasoningProvider` depends on the `ModelInferenceClient` interface. The production composition currently instantiates `LocalHttpModelInferenceClient`, whose request formatter and response parser are overridable in its constructor.

However, the existing `ParkerRuntime` composition does not override those collaborators. Therefore the current production launcher, without code changes, requires an HTTP endpoint compatible with the client’s default **Ollama `/api/generate` wire shape**. This is protocol compatibility, not an Ollama-product requirement.

### 3.2 Exact endpoint/API contract

The existing production path expects:

- an absolute endpoint URL supplied verbatim through configuration, normally ending in `/api/generate`;
- HTTP `POST`;
- header `Content-Type: application/json`;
- request body exactly equivalent to:

```json
{"model":"<configured model name>","prompt":"<Parker prompt>","stream":false}
```

- JSON string escaping for backslash, quote, newline, carriage return, and tab;
- a response body containing a top-level-looking compact token sequence `"response":"<text>"`; and
- response escaping compatible with the same five sequences.

The current parser is deliberately narrow: it scans text for the exact substring `"response":"` and does not use a general JSON parser. Whitespace between the key, colon, and opening quote can therefore make an otherwise valid JSON response incompatible. The HTTP client also does not branch on status code before parsing the body. A future endpoint used without production changes must conform to these actual constraints rather than merely claim general Ollama compatibility.

The extracted response text then enters `TaggedReasoningResponseParser`, which accepts only:

- `REPLY:<non-blank text>`;
- `GOAL:<non-blank text>`;
- `REMEMBER:<non-blank text>`; or
- exactly `NOACTION`.

For a minimal full round-trip reply proof, the local model must reliably return a parsable `REPLY:` response for the single benign verification message. Returning an invalid tag would still prove an attempted reasoning call and honest failure handling, but it would not prove the `OwnerNotificationSink -> UI` reply link.

### 3.3 Runtime configuration required

The real Windows launcher passes `System.getenv()` into `ParkerRuntimeConfigLoader`. Required configuration is:

- `PARKER_MODEL_ENDPOINT_URL`: loopback/Windows-local compatible endpoint URL;
- `PARKER_MODEL_NAME`: model identifier understood by that local endpoint;
- `PARKER_OWNER_PRINCIPAL_ID`: dedicated local verification owner identity;
- `PARKER_EVIDENCE_STORAGE_ROOT`: existing writable Windows-local directory;
- `PARKER_EVIDENCE_DELETION_AUDIT_LOG_PATH`: Windows-local file path whose parent exists and is writable; and
- `PARKER_MEMORY_CORE_DURABILITY_LOG_PATH`: Windows-local file path whose parent exists and is writable.

Optional configuration includes `PARKER_MODEL_TIMEOUT_MS` (default 30,000), `PARKER_OWNER_DISPLAY_NAME`, `PARKER_LOCAL_TEXT_CHANNEL_MODULE_ID` (default `channel.local-text`), and `PARKER_LOG_LEVEL`.

All durability paths for verification must be new/local, outside the repository’s historical Unit 3 campaign artifacts and outside all Parker-server storage. No secret is required by the current client contract; it supplies no authorization header. An endpoint needing authentication would require separate design and is not the minimum path.

### 3.4 Minimum Windows-local runtime/model

The minimum lawful prerequisite is an already-running Windows-local or loopback-only service that:

1. hosts any instruction-capable model small enough for the Windows machine;
2. accepts the exact request above at a local URL;
3. emits the exact compatible response envelope;
4. can follow Parker’s existing tagged response protocol for one benign turn; and
5. requires no UI or Parker production-code dependency on its provider implementation.

An Ollama installation would be one possible provider, but is not uniquely required. Any local service with an Ollama-compatible endpoint is sufficient. A non-model scripted stub can verify HTTP plumbing but must not be represented as genuine model-backed live verification; the requested future proof should use an actual locally hosted model.

### 3.5 Is `qwen2.5-coder:7b` required?

No. Requiring the same `qwen2.5-coder:7b` model would improperly couple Basic Owner UI transport verification to the paused Reasoning Protocol programme. The UI is model-agnostic, and its architectural claim is that it uses the existing governed runtime boundary, not that a particular model is semantically qualified.

A smaller or different instruction-following model is valid for architectural live-path proof if it can return the existing tagged protocol through the compatible envelope. Its exact model name must be disclosed in the verification evidence so nobody mistakes the result for production-model parity.

### 3.6 What remains explicitly unproven

Live UI verification with a smaller/different model must not be cited as evidence of:

- semantic correctness, usefulness, safety, or reliability of that model;
- equivalence to `qwen2.5-coder:7b` or any Parker-server model;
- Reasoning Protocol conformance or remedy efficacy;
- production latency, capacity, availability, or deployment readiness;
- quality of Goal, Remember, NoAction, planning, tool, Memory, or multi-turn behaviour;
- authorization correctness beyond observing that the existing governed path remains in control; or
- qualification of any provider/runtime for production use.

It proves only the narrow owner-UI-to-runtime-to-notification path described in Section 8.

## 4. Blocker 2 — real-runtime mode labelling

### 4.1 Finding

`ParkerOwnerWindow` is shared by both launchers and hard-codes:

- header: `Owner conversation · local offline preview`; and
- empty state: `Messages in this preview use Parker’s deterministic offline interaction.`

`OfflineOwnerUiMain` makes those statements true. `OwnerUiMain` also calls the same window after `OwnerUiRuntimeSession.start()` returns `Ready`, making both statements false in real mode.

The real launcher already has honest Starting and Failed screens. It constructs the controller/window only after successful runtime startup. The correction therefore must not claim a model health check or completed inference; runtime Ready means the runtime started and accepts submissions, not that the configured endpoint/model has successfully answered.

### 4.2 Minimum correction

Add one closed presentation-only mode value, for example `OwnerUiPresentationMode.OFFLINE_PREVIEW` and `OwnerUiPresentationMode.PARKER_RUNTIME`, and require it when rendering `ParkerOwnerWindow`/content.

Minimum truthful wording:

| Mode | Header | Empty-state support text |
|---|---|---|
| Offline | `Owner conversation · deterministic offline preview` | `Messages in this preview use Parker’s deterministic offline interaction.` |
| Real runtime | `Owner conversation · Parker Runtime` | `Messages are submitted through the local Parker Runtime.` |

The real text deliberately does not say “model connected,” “model online,” “ready model,” or name any provider. It states only the established fact: the real `ParkerRuntime` started and the message path is the runtime path. Existing Ready/Processing/Error/Stopped state remains unchanged.

### 4.3 Exact expected files

Production/presentation:

- `src/ui/parker/ui/OwnerWindowPresentation.kt` — closed mode type and pure label/copy mapping;
- `ui-desktop/src/main/kotlin/parker/ui/ParkerOwnerWindow.kt` — accept the mode and render mapped copy;
- `ui-desktop/src/main/kotlin/parker/ui/OfflineOwnerUiMain.kt` — pass offline mode; and
- `ui-desktop/src/main/kotlin/parker/ui/OwnerUiMain.kt` — pass real-runtime mode only for the Ready window.

Tests:

- `tests/ui/OwnerWindowPresentationTest.kt` — exact truthful copy for both modes and exhaustive distinction;
- `tests/ui/OfflineOwnerUiLauncherIsolationTest.kt` — offline launcher passes only offline mode and remains runtime/network isolated; and
- `tests/composition/OwnerUiRuntimeCompositionTest.kt` — real launcher passes real-runtime mode and does not alter startup/lifecycle composition.

No controller, runtime, model client, governance, Gradle, or packaging change is required.

## 5. Blocker 3 — runtime error presentation / `safeMessage`

### 5.1 Complete owner-visible paths

Owner-visible text can enter through these paths:

| Source | Current mapping | Risk |
|---|---|---|
| Governed reply text | `OwnerNotificationSink -> OwnerReply -> Parker transcript` | Intended Parker-authored content, not diagnostic status |
| `ParkerRuntimeOutcome.Failed.cause.message` | Adapter `safeMessage` -> controller System entry | Arbitrary exception text; unsafe |
| `ParkerRuntimeOutcome.NotAccepted.reason` | Adapter reason -> controller System entry | Untyped internal rejection reason; not guaranteed owner-safe |
| `Planned` result | Exhaustive enum-derived category -> System entry | Fixed category; owner-safe at this scope |
| `Delivered` execution status | Enum-derived disposition, no transcript text | Not owner-visible speech |
| Runtime unavailable | Fixed adapter string -> System entry | Owner-safe |
| Startup/config failure | Fixed launcher/session strings | Owner-safe |
| Unexpected UI interaction exception | Fixed controller string | Owner-safe |
| Offline scenarios and shutdown | Fixed/scripted local text | Safe when test fixture authors keep it intentional |

### 5.2 Demonstrated diagnostic risks

Arbitrary exception text can contain all risk classes named in the task:

- **raw provider response:** `defaultOllamaResponseBody` includes the entire raw response when no response field is found; `UnclassifiableModelResponseException` includes the raw extracted model text;
- **endpoint/model detail:** HTTP connection, URI, timeout, or provider exceptions may contain host/port/request information;
- **filesystem/internal detail:** any uncaught conversation-pipeline exception, including durability or component failures, is returned as `ParkerRuntimeOutcome.Failed` and may carry local paths, class/component names, identifiers, or implementation state;
- **sensitive information:** raw provider bodies or dependency messages may echo prompt/content, credentials embedded in a URI, or upstream diagnostic material; and
- **misleading technical text:** low-level parser, HTTP, identity, resource, and coordinator messages are not written as owner guidance.

`NotAccepted.reason` is also diagnostic, not contractually presentation-safe. Current reasons can expose configured channel IDs, owner principal IDs, resource counts, module states, and internal variant names such as `reasoningResponse was NoAction`.

### 5.3 Minimum presentation policy

The Basic Owner UI boundary must use an **allowlist-only owner-status policy**:

1. Never display `Throwable.message`, stack traces, raw provider responses, endpoint/model identifiers, filesystem paths, or arbitrary dependency diagnostics.
2. Map `ParkerRuntimeOutcome.Failed` by its existing typed stage only:
   - `REASONING` -> fixed owner status such as `Parker could not complete reasoning for this message.`
   - `UNKNOWN` -> fixed owner status such as `Parker could not complete this message.`
3. Do not display raw `NotAccepted.reason`; map it to a fixed owner status such as `Parker did not accept this message.` until a separately governed typed, owner-safe rejection contract exists.
4. Preserve fixed `Unavailable`, startup, unexpected-controller, and Planned category statuses.
5. Preserve governed reply text through `OwnerNotificationSink` unchanged. The UI must not sanitize or reinterpret Parker-authored conversation text under the guise of error handling.

Useful feedback is preserved through the typed outcome (`NotAccepted`, `Failed`, `Planned`, `Unavailable`) and the safe reasoning/unknown distinction. More detailed diagnosis belongs in logs, not in the transcript.

### 5.4 Diagnostic and observability boundary

`ParkerRuntime.submitOwnerMessage` already logs failures with correlation ID and the real cause attached. It also logs NotAccepted reasons. Those logs are the developer/operator diagnostic channel and must remain outside Parker-authored transcript content. This planning unit does not redesign logging, introduce an error registry, add telemetry, change exception types, or modify runtime coordinators.

Future observability hardening—redaction, retention, access control, structured event schemas, or secure correlation lookup—is a separate governance/security concern. It is not required to make the Basic Owner UI stop exposing arbitrary diagnostics.

### 5.5 Exact expected files

Production boundary:

- `src/composition/OwnerUiRuntimeAdapter.kt` — replace raw Failed and NotAccepted text propagation with fixed allowlisted owner-safe mappings.

Tests:

- `tests/composition/OwnerUiRuntimeAdapterTest.kt` — update NotAccepted/Failed expectations and add adversarial cases proving endpoint URLs, Windows paths, raw provider bodies, model output, credentials, and arbitrary messages never enter dispositions;
- `tests/ui/OwnerUiControllerTest.kt` — retain/prove typed System rendering of the sanitized fixed statuses and absence of diagnostic strings; and
- `tests/ui/OwnerUiOutcomeCompletenessTest.kt` — update any raw-reason transcript expectation while retaining ordering/recovery semantics.

`OwnerInteraction.kt` need not change if `safeMessage` remains an explicitly presentation-safe adapter product. `ParkerRuntimeOutcome`, `ParkerRuntime`, exception types, model client/parser, and runtime logging need not change.

## 6. Exact proposed implementation boundary

Authorized future implementation should contain only two corrections:

1. a closed presentation mode passed by the two existing launchers to shared Compose presentation; and
2. allowlisted mapping of existing runtime Failed/NotAccepted outcomes at `OwnerUiRuntimeAdapter` before they cross into `parker.ui`.

No new UI feature, runtime capability, endpoint abstraction, provider integration, retry, health check, settings UI, error architecture, or model-selection UI is required.

Expected production files: five total (`OwnerWindowPresentation.kt`, `ParkerOwnerWindow.kt`, both launcher files, and `OwnerUiRuntimeAdapter.kt`). Expected test files: six total (`OwnerWindowPresentationTest.kt`, `OfflineOwnerUiLauncherIsolationTest.kt`, `OwnerUiRuntimeCompositionTest.kt`, `OwnerUiRuntimeAdapterTest.kt`, `OwnerUiControllerTest.kt`, and `OwnerUiOutcomeCompletenessTest.kt`). Scope/review documents are additional governance artifacts, not production changes.

## 7. Governance and boundary determination

Both corrections are presentation/composition-boundary corrections:

- mode labelling decides only what already-established launcher mode is called;
- safe status mapping decides only which text may cross the composition boundary into owner-visible System presentation.

Neither grants reasoning, permission, Memory, Goals, Planner, Knowledge Submission, tool, model, provider, HTTP, or runtime authority to the UI. Neither changes `ParkerRuntime.submitOwnerMessage`, `OwnerNotificationSink`, production reasoning, or any coordinator.

No proposed resolution depends on Unit 3-C evidence, a Reasoning Protocol remedy, `qwen2.5-coder:7b`, Memory, Goals, Planner, Knowledge Submission, tools, or direct provider access from the UI.

### Is a Boundary Review required?

**Yes — a focused Basic Owner UI Presentation and Diagnostic Boundary Review is required before implementation.** It should freeze:

- the two presentation modes and exact truthful claims;
- the allowlisted owner-visible outcome strings;
- the prohibition on raw exception/rejection diagnostics;
- preservation of `OwnerNotificationSink` reply authorship; and
- the explicit exclusion of runtime/model/governance changes.

This is not a constitutional architecture redesign and does not require reopening Unit 3-C or the paused remedy programme. A narrow scope lock plus independent boundary review is proportionate because the change determines what runtime facts and diagnostics may be represented to the owner.

## 8. Verification plan

### 8.1 Offline verification after corrections

Run under JDK 17:

1. focused mode-copy presentation tests;
2. offline-launcher isolation tests;
3. real-launcher composition/lifecycle tests;
4. adapter tests, including adversarial secret/path/endpoint/raw-response messages;
5. controller and outcome-completeness tests;
6. the complete targeted 45-test baseline plus all added tests;
7. relevant runtime failure-handling and conversation pipeline tests;
8. ordinary `\.\gradlew.bat test` with exact counts;
9. root `build`;
10. `:ui-desktop:compileKotlin` and desktop JAR/build; and
11. source searches proving no direct model/provider/runtime-subsystem dependency and no Unit 3-C task wiring.

Required assertions include:

- offline mode displays only deterministic/offline wording;
- real mode displays only Parker Runtime wording and never claims model connectivity;
- the real mode value is supplied only after the existing Ready transition;
- raw Failed causes and NotAccepted reasons cannot reach transcript state;
- fixed System statuses never become Parker transcript entries;
- governed reply text still enters only through `OwnerNotificationSink`; and
- startup, concurrency, cancellation, and shutdown behaviour remains unchanged.

### 8.2 Subsequent live verification

After separate authorization and a separately provisioned compatible local model/runtime:

1. fetch and confirm a fresh current-main reconciliation baseline;
2. confirm all offline gates green;
3. record the Windows-local endpoint implementation, endpoint URL, model name, and local durability paths without secrets;
4. launch only `:ui-desktop:runOwnerUi`;
5. verify Starting does not claim availability and Ready says only Parker Runtime;
6. submit exactly one benign uniquely identifiable message through the visible UI;
7. correlate the `InboundOwnerMessage` with `ParkerRuntime.submitOwnerMessage` logs;
8. observe the existing governed runtime remaining in control;
9. obtain one parsable Reply and prove `OwnerNotificationSink -> OwnerUiNotificationBridge -> typed Parker transcript -> visible UI` exactly once;
10. rely on deterministic tests—not repeated inference—to cover non-success outcomes;
11. close while idle and prove one orderly runtime shutdown and process exit; and
12. record that no Parker server, server model, Unit 3-C task, remedy mechanism, or direct UI provider access participated.

The live verdict must be phrased narrowly:

```text
owner UI
-> ParkerRuntime.submitOwnerMessage
-> existing governed runtime
-> OwnerNotificationSink
-> owner UI
```

It is not semantic model qualification and must not reopen or supply evidence to the paused Reasoning Protocol remedy programme.

## 9. Windows-local model/runtime prerequisite

Still external and blocking for live execution: an authorized, already-running loopback-only model service and disclosed model that meet the exact wire/tag contract. Installation, model download, service startup, firewall changes, and provider selection require a separate task. The preferred proof model is the smallest instruction-capable model that reliably emits the existing tags; production-model parity is neither necessary nor desirable for this UI boundary proof.

## 10. Implementation readiness

Technical scope is sufficiently precise to proceed, but governance sequence is not complete.

Classification: **READY FOR SCOPE LOCK AND FOCUSED BOUNDARY REVIEW; NOT YET AUTHORIZED FOR IMPLEMENTATION**.

After the scope lock and independent Boundary Review accept the exact mode claims and safe-status allowlist, the two corrections are implementation-ready. Local model provisioning is not required to implement or offline-test them, but remains required before the later live-verification run.

## 11. Blocking issues

Before correction implementation:

- focused presentation/diagnostic scope lock and Boundary Review approval.

Before live verification:

- corrections implemented and all offline verification green; and
- separately authorized Windows-local compatible model service/model and isolated local durability paths.

Before final merge review:

- successful narrowly scoped live proof with the corrected real-mode label and safe non-success presentation.

## 12. Non-blocking issues

- Exact provider product and model may vary if the contract and disclosure requirements are met.
- Native installer/distribution work remains outside Units 1–5 and is not needed for the Gradle-based live proof.
- Original-branch upstream/publication policy remains separate.
- Broader structured/redacted observability improvements are desirable but outside this UI correction.

## 13. Exact next governance step

Create and independently review a **Basic Owner UI Live-Verification Blocker Corrections Scope Lock and Presentation/Diagnostic Boundary Review** authorizing only:

1. closed offline-versus-runtime presentation mode labelling;
2. fixed allowlisted Failed and NotAccepted owner statuses at the adapter boundary;
3. the exact offline tests in Section 8; and
4. no runtime, model, reasoning, Unit 3-C, Memory, Goals, Planner, Knowledge Submission, tool, or provider-access change.

Only after that review is accepted should a separate implementation task be authorized. Model-runtime installation/provisioning and the live-verification execution must remain separately authorized tasks.
