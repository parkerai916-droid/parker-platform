# Model-Backed ReasoningProvider — Implementation Plan & Scope Lock

## Status

**Stage 3 Implementation Plan + Stage 5 Scope Lock, PES-001. Locked.**
This Plan implements exactly what
`docs/implementation/reviews/ModelReasoningProvider_Implementation_Review.md`
("the Review") authorises. The Review is the completed Stage 1/Stage 2
Architecture Review artefact for this unit — approved, frozen, and not
revisited here. This Plan does not redesign anything the Review already
decided; it restates the Review's own decisions as frozen scope, resolves
the small number of Kotlin-literal-level choices the Review named but did
not pin down exactly, and records the file list, testing strategy, and
Scope Lock confirmation PES-001 Stage 3/5 require.

**Grounded exclusively in:** the Review (all sections), `REASONING_PROVIDER_ARCHITECTURE.md`,
`REASONING_PROVIDER_CONTRACT_DESIGN.md`, `docs/architecture/reasoning-context.md`,
ADR-001, ADR-010, `src/interfaces/ReasoningProvider.kt`,
`src/interfaces/ConversationEngine.kt` (`Turn`), `src/interfaces/ModelManager.kt`,
`build.gradle.kts`, `docs/architecture/PARKER_ENGINEERING_STANDARD.md`
(PES-001), and the established `src/runtime`/`tests/runtime` conventions
(`fun interface` precedent: `PrincipalAuthenticator`, `EventHandler`;
lambda-based test fakes: `FakeReasoningProvider`, `FakePermissionEngine`).

**No Contract Design (Stage 2A) is required.** The Review's Section 2
already established this: the existing `ReasoningProvider` contract
supports a production implementation without modification, and this Plan
introduces no new file under `src/interfaces/` (Review Section 13). Every
new type is either a `src/runtime` concrete implementation of an existing
contract, or a new `src/runtime`-local collaborator interface — not a
Parker contract.

---

## 1. Executive Summary

This Plan implements the Review's three-collaborator design in full:
`ModelReasoningProvider` (pure orchestrator, implements `ReasoningProvider`),
`ReasoningPromptBuilder`/`DefaultReasoningPromptBuilder`,
`ModelInferenceClient`/`LocalHttpModelInferenceClient` (plus its two
named default formatting functions), and
`ReasoningResponseParser`/`TaggedReasoningResponseParser` (plus
`UnclassifiableModelResponseException`). No architecture, ADR, or
existing contract file changes. No third-party dependency is added — the
JDK's own `java.net.http.HttpClient` (available under `jvmToolchain(17)`)
is the only new capability this Plan draws on, and JSON formatting for
the Ollama-shaped default is minimal, hand-rolled string construction,
not a general-purpose JSON library.

## 2. Scope

The field-level Kotlin shape of the four collaborators the Review
names, their default implementations, and unit tests for every piece
except live HTTP calls (Review Risk 1 — no real model server exists in
this sandbox). Nothing else. This Plan does not wire `ModelReasoningProvider`
into `ConversationTurnReasoningCoordinator`, `CommunicationConversationCoordinator`,
or any production caller — none of those files are modified.

## 3. Included Work

- `ModelReasoningProvider` (`src/runtime/ModelReasoningProvider.kt`) —
  implements `ReasoningProvider`; constructor-injects the three
  collaborators plus `timeoutMs`; `reason()` performs exactly the three
  delegated steps the Review's Section 4 sequence names, with no
  try/catch anywhere in the class (Review Section 8).
- `ReasoningPromptBuilder` (`fun interface`) and `DefaultReasoningPromptBuilder`
  (`src/runtime/ReasoningPromptBuilder.kt`).
- `ModelInferenceClient` (`fun interface`) and `LocalHttpModelInferenceClient`,
  plus `defaultOllamaRequestBody`/`defaultOllamaResponseBody`
  (`src/runtime/ModelInferenceClient.kt`).
- `ReasoningResponseParser` (`fun interface`), `TaggedReasoningResponseParser`,
  and `UnclassifiableModelResponseException`
  (`src/runtime/ReasoningResponseParser.kt`).
- Test fakes `FakeModelInferenceClient`, `FakeReasoningPromptBuilder`,
  `FakeReasoningResponseParser` (`tests/runtime/`), mirroring
  `FakeReasoningProvider`'s lambda-based precedent.
- Unit tests for all of the above except `LocalHttpModelInferenceClient`'s
  own live HTTP path (Review Risk 1) — its two pure formatting functions
  are tested directly.

## 4. Excluded Work

Restating the Review's own boundaries, each grounded in why, not merely
named:

- **`ModelManager` involvement.** Remains unusable and untouched (Review
  Section 3) — `ModelRequest`/`ModelResponse`/etc. do not exist in this
  repository, and `ModelManager.kt` remains excluded from compilation
  (`build.gradle.kts`, ADR-022).
- **Any production caller.** `ConversationTurnReasoningCoordinator`,
  `CommunicationConversationCoordinator`, and every other existing
  `src/runtime` file are unmodified. No composition root exists in this
  repository (Review precedent, `LOCAL_TEXT_CHANNEL_DELIVER_TOOL_IMPLEMENTATION_PLAN.md`
  Decision 9) and this Plan does not create one.
- **`IdentityService` dependency.** None of the four collaborators
  reference it (Review Section 4, Decision 1).
- **`ReasoningContext` assembly.** Remains unassigned (`reasoning-context.md`;
  Review Section 1); this unit only ever consumes an already-assembled
  `ReasoningContext`.
- **Streaming.** Named as a future extension shape only (Review Section 9)
  — no `Flow`-returning interface, no partial-result type, is introduced.
- **A general-purpose JSON library.** `defaultOllamaRequestBody`/
  `defaultOllamaResponseBody` use minimal, hand-rolled string
  construction/extraction sufficient only for Ollama's fixed
  `/api/generate` request/response shape (Decision J, below) — not a
  reusable JSON capability, and no Gradle dependency is added (Review
  Section 3).
- **Trust, Planner, Memory, World Model, Execution Pipeline.** No
  reference anywhere in this unit's code (Review Section 4's explicit
  "not a dependency" list, restated).

## 5. Dependencies

**Already satisfied, reused unchanged:** `ReasoningProvider`,
`ReasoningProviderRequest`, `ReasoningProviderResponse` (`Goal`/`Reply`/`NoAction`),
`ReasoningContext` (`src/interfaces/ReasoningProvider.kt`); `Turn`
(`src/interfaces/ConversationEngine.kt`); `kotlinx-coroutines-core`
(`withTimeout`, `suspendCancellableCoroutine`); JDK `java.net.http.HttpClient`
(`jvmToolchain(17)`, already satisfied — no Gradle change).

**Not a dependency of this unit:** `IdentityService`, `PlannerRuntime`,
`ExecutionPipeline`, `PermissionEngine`, `ToolRegistry`,
`ToolInvocationBinding`, `MemoryStore`, `WorldModel`, `ModuleRegistry`,
`ConversationEngine`, `ResponseDelivery`, `ModelManager` (Review Section
4, restated).

## 6. Required Implementation Decisions

Decisions 1–7 are the Review's own Section 12, **restated here as frozen,
not reopened.** Decisions A–J below are the smaller, Kotlin-literal-level
forks the Review named in substance but did not pin to an exact value —
each is a genuine interpretive fork, named explicitly per PES-001's
"code should implement decisions, not invent them" principle, resolved
here with a conservative, testable default before any Kotlin is written.

1. No `IdentityService` dependency. *(Review Section 4.)*
2. `DefaultReasoningPromptBuilder`: context entries, one per line, then
   the owner's message, then a fixed instruction. *(Review Section 12.)*
3. `ReasoningResponseParser` is the frozen interface;
   `TaggedReasoningResponseParser` is its first, replaceable default.
   *(Review Section 12.)*
4. Timeout: `withTimeout(timeoutMs)` around `infer` only, default
   `30_000` ms. *(Review Section 12.)*
5. Cancellation: `suspendCancellableCoroutine` wrapping `HttpClient.sendAsync`.
   *(Review Section 12.)*
6. `LocalHttpModelInferenceClient`: `endpointUrl`/`modelName` required, no
   default; formatter functions default to Ollama-shaped, overridable.
   *(Review Section 12.)*
7. Class/file naming exactly as the Review's Section 13 table. *(Review
   Section 12.)*

### Decision A — Exact `DefaultReasoningPromptBuilder` template

```
<reasoningContext.entries, one per line, in order>
<turn.message.text>

Respond with exactly one of the following prefixes: GOAL:, REPLY:, or NOACTION, followed by your response text. Use NOACTION alone, with no text after it.
```

If `reasoningContext.entries` is empty, the first block is omitted (no
leading blank line beyond the one blank line already separating the
owner's message from the instruction). No further prompt engineering,
persona, or few-shot content — matching the Review's own "no further
prompt engineering... proposed" (Section 12, Decision 2).

### Decision B — Tag matching exactness (`TaggedReasoningResponseParser`)

The raw string is trimmed once. Matching is case-sensitive and
prefix-based:

- Starts with `"GOAL:"` → `Goal(text = remainder.trim())`, re-validated
  non-blank by `Goal`'s own `init` block (a blank remainder therefore
  surfaces as `IllegalArgumentException`, not `UnclassifiableModelResponseException`
  — an existing contract-level check, not duplicated here).
- Starts with `"REPLY:"` → `Reply(text = remainder.trim())`, same
  non-blank behaviour.
- Exactly equals `"NOACTION"` (after the one outer trim, nothing else
  present) → `ReasoningProviderResponse.NoAction`. Trailing text after
  `NOACTION` does not match this branch (the Review's "NOACTION takes no
  trailing text" is read as *no trailing text is accepted*, not *trailing
  text is silently ignored*) and falls through to the next rule.
- Anything else → throws `UnclassifiableModelResponseException(raw)`,
  carrying the original, untrimmed `raw` string.

### Decision C — `UnclassifiableModelResponseException` shape

`class UnclassifiableModelResponseException(val raw: String) : RuntimeException("Unclassifiable model response: $raw")`.
Plain `RuntimeException` subtype (Review Section 12, Decision 3), no new
Parker contract, carries the original raw text for diagnostic purposes.

### Decision D — `LocalHttpModelInferenceClient` HTTP mechanics

One `java.net.http.HttpClient` instance per `LocalHttpModelInferenceClient`,
constructed once (`HttpClient.newHttpClient()`) and reused across calls —
an `HttpClient` is itself immutable/thread-safe, so this does not
introduce mutable state (Review Section 10, Concurrency). Each `infer`
call builds one `HttpRequest`: `POST` to `endpointUrl`, header
`Content-Type: application/json`, body `requestBodyFormatter(prompt, modelName)`.
`HttpClient.sendAsync(request, BodyHandlers.ofString())` is wrapped in
`suspendCancellableCoroutine`, registering
`continuation.invokeOnCancellation { future.cancel(true) }` (Review
Section 8). On completion, `responseBodyParser(response.body())` is
returned. No status-code check is performed by this class itself — a
non-2xx response's body is passed to `responseBodyParser` unchanged, and
a malformed/unexpected body is `defaultOllamaResponseBody`'s own concern
(Decision J). No retry, no health check (Review Section 10, Loading).

### Decision E — `requestBodyFormatter`/`responseBodyParser` function types

`requestBodyFormatter: (prompt: String, modelName: String) -> String`,
`responseBodyParser: (rawResponseBody: String) -> String` — exactly the
shapes the Review's Section 6 names, as plain Kotlin function types
(no new `fun interface`), defaulted to `::defaultOllamaRequestBody` /
`::defaultOllamaResponseBody`.

### Decision F — `endpointUrl`/`modelName` validation

Both are plain, unvalidated `String` constructor parameters — no
non-blank `require()` is added. `endpointUrl`/`modelName` are explicit,
required, caller-supplied configuration (Review Section 6); a caller
providing an invalid value discovers this through `HttpRequest.newBuilder().uri(...)`
throwing `IllegalArgumentException`/`URISyntaxException` at call time or
the HTTP call itself failing — not a bespoke pre-check this class invents.

### Decision G — Test fakes' exact shape

Mirroring `FakeReasoningProvider` exactly: each fake takes one
constructor-injected lambda, exposes a call-count and last-input for
assertions.

```kotlin
class FakeModelInferenceClient(
    private val responseFor: (String) -> String,
) : ModelInferenceClient {
    var inferCallCount: Int = 0; private set
    var lastPrompt: String? = null; private set
    override suspend fun infer(prompt: String): String { ... }
}

class FakeReasoningPromptBuilder(
    private val promptFor: (Turn, ReasoningContext) -> String,
) : ReasoningPromptBuilder { ... }

class FakeReasoningResponseParser(
    private val responseFor: (String) -> ReasoningProviderResponse,
) : ReasoningResponseParser { ... }
```

### Decision H — `ModelReasoningProvider` timeout exception

`withTimeout` throwing `TimeoutCancellationException` is not caught or
translated (Review Section 8) — propagates to the caller exactly as any
other `reason()` fault.

### Decision I — Package and file layout

Unchanged from the Review (Section 14): all new production classes in
`parker.core.runtime` under `src/runtime/`; all new test fakes/tests in
`tests/runtime/`. No new package.

### Decision J — `defaultOllamaRequestBody`/`defaultOllamaResponseBody` exact shape

No JSON library is added (Section 4, Excluded Work), so both functions
are minimal, hand-rolled string operations, sufficient only for Ollama's
fixed `/api/generate` convention — not general JSON support:

- `defaultOllamaRequestBody(prompt, modelName)` returns
  `"""{"model":"${jsonEscape(modelName)}","prompt":"${jsonEscape(prompt)}","stream":false}"""`,
  where a private `jsonEscape` helper escapes exactly `\`, `"`, newline,
  carriage return, and tab — the minimum required for well-formed JSON
  string content, not a general escaper.
- `defaultOllamaResponseBody(rawResponseBody)` extracts the value of the
  top-level `"response"` string field via a direct scan for
  `"response":"` followed by characters up to the next unescaped `"`,
  un-escaping the same five sequences `jsonEscape` escapes. If the key is
  not found, throws `IllegalArgumentException` (an implementation-level
  fault, propagating unchanged per Section 8 — this class performs no
  fallback or default text).

## 7. Files Expected to Change

**All additions. No existing `src/` or `tests/` file is modified.**

| File | New/Modified | Contents |
| --- | --- | --- |
| `src/runtime/ModelReasoningProvider.kt` | New | `ModelReasoningProvider`. |
| `src/runtime/ReasoningPromptBuilder.kt` | New | `ReasoningPromptBuilder`, `DefaultReasoningPromptBuilder`. |
| `src/runtime/ModelInferenceClient.kt` | New | `ModelInferenceClient`, `LocalHttpModelInferenceClient`, `defaultOllamaRequestBody`, `defaultOllamaResponseBody`. |
| `src/runtime/ReasoningResponseParser.kt` | New | `ReasoningResponseParser`, `TaggedReasoningResponseParser`, `UnclassifiableModelResponseException`. |
| `tests/runtime/FakeModelInferenceClient.kt` | New | Test fake. |
| `tests/runtime/FakeReasoningPromptBuilder.kt` | New | Test fake. |
| `tests/runtime/FakeReasoningResponseParser.kt` | New | Test fake. |
| `tests/runtime/ModelReasoningProviderTest.kt` | New | Orchestration tests, against fakes only. |
| `tests/runtime/ReasoningPromptBuilderTest.kt` | New | `DefaultReasoningPromptBuilder` tests. |
| `tests/runtime/ModelInferenceClientTest.kt` | New | `defaultOllamaRequestBody`/`defaultOllamaResponseBody` tests only — no live HTTP (Review Risk 1). |
| `tests/runtime/ReasoningResponseParserTest.kt` | New | `TaggedReasoningResponseParser` tests. |

## 8. Testing Strategy

- **`ModelReasoningProviderTest.kt`.** Using the three fakes: proves
  `reason()` calls `buildPrompt` with the exact `turn`/`reasoningContext`
  from the request, passes the resulting prompt to `infer` unchanged,
  passes `infer`'s raw result to `parse` unchanged, and returns `parse`'s
  result unchanged. Proves a `parse` exception propagates uncaught.
  Proves an `infer` exception propagates uncaught. Proves a timeout
  (a fake `infer` that suspends past `timeoutMs`) surfaces
  `TimeoutCancellationException`, with `parse` never called (Decision H).
- **`ReasoningPromptBuilderTest.kt`.** Proves the exact template
  (Decision A) for: multiple context entries, zero context entries, and
  confirms `turn.message.text` appears unmodified.
- **`ModelInferenceClientTest.kt`.** Pure-function tests for
  `defaultOllamaRequestBody` (valid JSON shape; `\`, `"`, newline
  escaping) and `defaultOllamaResponseBody` (extracts `"response"`'s
  value; un-escapes the same sequences; throws on a missing key).
- **`ReasoningResponseParserTest.kt`.** `GOAL:`/`REPLY:` with trailing
  text produce the correct variant with trimmed text; blank remainder
  after either tag throws `IllegalArgumentException` (Decision B);
  `NOACTION` alone produces `NoAction`; `NOACTION` with trailing text,
  an unrecognised tag, and an empty string all throw
  `UnclassifiableModelResponseException` carrying the original raw text;
  matching is proven case-sensitive (a lowercase `goal:` is
  unclassifiable).
- **Full Gradle test suite.** Run via `./gradlew test` in this session as
  an informative check (PES-001 Stage 7: "Shell builds are informative.
  Android Studio verification is the authoritative verification
  environment"). The resulting count is reported honestly as
  Gradle-verified, not Android-Studio-verified — `IMPLEMENTATION_HISTORY.md`
  will record it as such, pending Steven's own Android Studio
  confirmation before this Sprint is considered closed under PES-001's
  human-authority model.

## 9. Acceptance Criteria

- All four collaborators and their default implementations exist exactly
  as Section 6/7 specify.
- `ModelReasoningProvider` contains no `try`/`catch` (Review Section 8) —
  verified by direct inspection of the committed file.
- No existing `src/` or `tests/` file is modified.
- No new Gradle dependency is added; `build.gradle.kts` is unmodified.
- No new file exists under `src/interfaces/`.
- All tests listed in Section 8 pass under `./gradlew test` in this
  session, and the existing 541 tests continue to pass unmodified
  (no regressions).
- `LocalHttpModelInferenceClient`'s live HTTP path remains unexercised by
  the automated suite, disclosed as such (Review Risk 1) — not a false
  completeness claim.

## 10. Architectural Traceability

| Implemented element | Authorised by |
| --- | --- |
| `ModelReasoningProvider` exists, implements `ReasoningProvider` | `REASONING_PROVIDER_CONTRACT_DESIGN.md`; Review Section 2 |
| Three-collaborator orchestration, no try/catch | Review Section 4, Section 8 |
| `ReasoningPromptBuilder`/`DefaultReasoningPromptBuilder` | Review Section 4, Section 12 Decision 2 |
| `ModelInferenceClient`/`LocalHttpModelInferenceClient`, configurable endpoint | Review Section 4, Section 6 |
| `ReasoningResponseParser` frozen interface / `TaggedReasoningResponseParser` first default | Review Section 4, Section 12 Decision 3 |
| Timeout (Decision 4/H), cancellation (Decision 5/D) | Review Section 8 |
| No `IdentityService`, no `ModelManager`, no Trust/Planner/Memory/World Model reference | Review Section 4, Section 3 |
| Prompts are not Memory/World Model/Reasoning Context | Review Section 4a |
| No new Gradle dependency; hand-rolled JSON (Decision J) | Review Section 3 |
| Test fakes mirror `FakeReasoningProvider` (Decision G) | Established `tests/runtime` convention |

## 11. Completion Criteria

- This unit reaches its own Acceptance Criteria (Section 9).
- `./gradlew test` passes in full in this session (informative, PES-001
  Stage 7) — Android Studio verification by Steven remains the
  authoritative gate and is recorded as pending, not simulated.
- `IMPLEMENTATION_HISTORY.md` records this unit, honestly distinguishing
  Gradle-verified from Android-Studio-verified.
- `IMPLEMENTATION_GAPS.md` #53's "No concrete, model-backed `ReasoningProvider`
  implementation exists" line is closed. Every other open item under
  gap #53 (Reply→`OutboundParkerResponse` construction, `Goal`/Planner
  Runtime routing, production composition root, `ReasoningContext`
  assembly ownership) remains open, untouched by this unit.
- No architecture, Contract Design, or ADR document is modified at any
  point during this unit's implementation.

## 12. Scope Lock

**Locked.** Steven's own message opening this implementation session
("Parker Platform – Sprint 9 Scope Lock & Implementation") is the
explicit human instruction PES-001's Stage 3–5 human-authority model
requires: it names the Review as approved, states no further
architectural redesign is required, and directs Phase 1 (this document)
to be followed immediately by Phase 2 (implementation) in one sitting —
satisfying the same "separate, explicit human instruction" precedent
`LOCAL_TEXT_CHANNEL_DELIVER_TOOL_IMPLEMENTATION_PLAN.md` Section 12
establishes, without requiring a second round-trip message.

**What is frozen as of this lock:** the file list (Section 7), the
dependency list (Section 5), Decisions 1–7 and A–J (Section 6), the
testing strategy (Section 8), and the Excluded Work list (Section 4).
Any change to any of these after this point requires a new planning
pass, not a silent adjustment during implementation. If implementation
reveals a contradiction with this Plan or with the Review, implementation
stops and the contradiction is reported, not resolved in Kotlin.

## Related

- `docs/implementation/reviews/ModelReasoningProvider_Implementation_Review.md`
- `docs/architecture/REASONING_PROVIDER_ARCHITECTURE.md`
- `docs/architecture/REASONING_PROVIDER_CONTRACT_DESIGN.md`
- `docs/architecture/reasoning-context.md`
- `docs/architecture/PARKER_ENGINEERING_STANDARD.md`
- `src/interfaces/ReasoningProvider.kt`, `src/interfaces/ConversationEngine.kt`
- `tests/runtime/FakeReasoningProvider.kt` (fake-shape precedent)
- `docs/implementation/LOCAL_TEXT_CHANNEL_DELIVER_TOOL_IMPLEMENTATION_PLAN.md`
  (Scope Lock precedent, Section 12)
