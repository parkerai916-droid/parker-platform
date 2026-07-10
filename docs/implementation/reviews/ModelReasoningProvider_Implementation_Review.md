# Model-Backed ReasoningProvider — Implementation Review

## Status

**Implementation review, PES-001. Not yet Scope Locked. No Kotlin written by
this document.**

**Revision note.** Following review, five refinements were requested and are
applied throughout this revision, each with real design consequences, not
label changes only: (1) prompt construction is separated from orchestration
into its own injected seam; (2) response parsing is likewise separated into
its own injected seam — `ModelReasoningProvider` is now a pure orchestrator
with three small, independently-testable collaborators, not one class doing
four jobs; (3) the local HTTP client's hard-coded Ollama assumption is
replaced with a fully configurable endpoint, model name, and request/response
formatting, with an Ollama-shaped default supplied but overridable, never
silently assumed; (4) a new Future Considerations section names the shape a
later streaming addition would take, without implementing it; (5) a new
section names this unit's model lifecycle assumptions (ownership, loading,
concurrency) explicitly, since none of these were previously stated.

**Second revision note.** Following an independent architectural review of
the revision above, two further refinements are applied, both wording/
scope-clarification only — no collaborator is added, removed, or
redesigned: (1) `ReasoningResponseParser` (the interface) is now stated
throughout as the frozen architectural component; `TaggedReasoningResponseParser`
(the tag convention) is now stated throughout as its first, replaceable
default implementation, not a permanent protocol — see Section 4, Section
12 Decision 3, and Section 13; (2) a new subsection (4a) states explicitly
that prompts are transient implementation artefacts, not Parker Memory,
not the World Model, and not Reasoning Context, and are never persistent
platform state. Everything else — the three-collaborator decomposition,
the configurable inference endpoint design, model lifecycle assumptions,
and streaming considerations — is unchanged from the immediately prior
revision.

**Naming note, unchanged from the prior revision, restated for continuity.**
A different, still-pending unit from earlier in this session ("Reply to
OutboundParkerResponse Construction," implemented by `ResponseComposer`) was
also called "Sprint 9." That unit remains open, un-Scope-Locked, and
untouched by this document. This review proceeds under its own name and does
not resolve the numbering collision — left to Steven to sequence.

**Grounded exclusively in documents read fresh this session.** No Kotlin
exists yet for this unit.

## Review

Unchanged from the prior revision — see that section's own citation list.
Re-confirmed as still accurate for this revision: `REASONING_PROVIDER_ARCHITECTURE.md`,
`REASONING_PROVIDER_CONTRACT_DESIGN.md`, `reasoning-context.md`, ADR-001,
ADR-010, `src/interfaces/ReasoningProvider.kt`, `src/interfaces/ModelManager.kt`,
`build.gradle.kts`, and the established `src/runtime` implementation
conventions.

---

## 1. Existing Architecture

Unchanged from the prior revision. A Reasoning Provider is a replaceable
role, not a runtime subsystem; it interprets a Turn plus an assembled
`ReasoningContext` and returns reasoning output; it never executes,
authorises, plans, or touches Memory/World Model; it owns no persistent
Parker-modelled state; its lifecycle is exactly one invocation with no
retained state between invocations. `reasoning-context.md` confirms Reasoning
Context assembly is a separate, still-unassigned responsibility this unit
does not take on. `REASONING_PROVIDER_CONTRACT_DESIGN.md` confirms
implementation-level faults are signalled by throwing, never by a domain
variant, and `NoAction` must never be a catch-all.

## 2. Current Contract Responsibilities — do the existing contracts already support a production implementation?

**Yes, without modification** — restated unchanged from the prior revision.
`ReasoningProvider.reason(request: ReasoningProviderRequest): ReasoningProviderResponse`,
`ReasoningProviderRequest(turn, reasoningContext)`, and the sealed
`ReasoningProviderResponse` (`Goal`/`Reply`/`NoAction`) already carry
everything a production implementation needs. This unit requires zero
Contract Design changes, zero ADR changes, and zero new architecture.

## 3. What "model-backed" can honestly mean in this repository, right now

Unchanged in substance from the prior revision, with one correction applied
throughout the rest of this document (Section 6, Decision 6): the earlier
draft named Ollama's specific local API convention as the *design's own*
assumption. That assumption is now demoted to a **default, not a
hard-coded requirement** — see Section 6.

- **`ModelManager` remains unusable for this unit** — references
  `ModelRequest`/`ModelResponse`/`ModelCapability`/`CapabilityStatus`, none
  of which exist anywhere in this repository; `build.gradle.kts` confirms
  it is explicitly excluded from compilation (ADR-022). This unit does not
  depend on it, extend it, or partially implement it.
- **No third-party dependency is added.** `build.gradle.kts`'s only
  dependency remains `kotlinx-coroutines-core`. The proposed local HTTP path
  uses the JDK's own built-in `java.net.http.HttpClient` (Java 11+; this
  project targets `jvmToolchain(17)`) — zero new Gradle dependency.

## 4. Orchestration, Separated from Its Two Collaborators

**This section replaces the prior revision's inline "prompt construction is
part of `ModelReasoningProvider`" design.** `ModelReasoningProvider` is now
a pure orchestrator with exactly three constructor-injected collaborators,
each with one job:

```
ModelReasoningProvider.reason(request)
    1. prompt = promptBuilder.buildPrompt(request.turn, request.reasoningContext)
    2. raw    = withTimeout(timeoutMs) { modelInferenceClient.infer(prompt) }
    3. return responseParser.parse(raw)
```

- **`ReasoningPromptBuilder`** (new — Section 12) — one method,
  `fun buildPrompt(turn: Turn, reasoningContext: ReasoningContext): String`.
  Pure, synchronous (no I/O, no `suspend`) — turns already-in-memory data
  into a prompt string. `ModelReasoningProvider` never constructs a prompt
  itself; it only calls this collaborator.
- **`ModelInferenceClient`** (unchanged from the prior revision) — one
  method, `suspend fun infer(prompt: String): String`. The entire model-call
  seam. `ModelReasoningProvider` never knows or cares what is behind it.
- **`ReasoningResponseParser`** (new — Section 12) — one method,
  `fun parse(raw: String): ReasoningProviderResponse`. Pure, synchronous —
  classifies a raw model string into `Goal`/`Reply`/`NoAction`, or throws
  `UnclassifiableModelResponseException` (Section 8). `ModelReasoningProvider`
  never inspects or branches on `raw` itself; it only calls this
  collaborator and returns its result. **This interface, not any particular
  parsing convention, is the stable architectural component.** The tagged
  convention proposed in Section 12 Decision 3
  (`TaggedReasoningResponseParser`) is this interface's first, default
  production implementation — not the permanent protocol. Any
  implementation satisfying `ReasoningResponseParser`'s one-method shape is
  a legitimate replacement, including structured JSON, grammar-constrained
  output, function-calling, or schema-based responses — none of which would
  require a change to `ModelReasoningProvider`, to this interface, or to
  any architecture document.

**Why separate, rather than three private methods on one class.** Each
collaborator is independently testable without a model, without HTTP, and
without the other two — a prompt-construction test never needs a fake
model response; a parsing test never needs a `Turn` or `ReasoningContext`
at all. This also makes each piece independently swappable: a different
prompt strategy, a different classification convention, or a different
model transport can each change without touching the other two or
`ModelReasoningProvider` itself — the same "small, single-purpose,
independently testable" discipline this program has applied to every
coordinator since Sprint 7 (`CommunicationConversationCoordinator`, and the
still-pending `ResponseComposer` Plan), now applied one layer further down,
inside a single unit rather than only between units.

**Dependency list, revised:** `ModelReasoningProvider`'s constructor takes
exactly three parameters — `ReasoningPromptBuilder`, `ModelInferenceClient`,
`ReasoningResponseParser` — plus the `timeoutMs: Long` configuration value
(Section 8). **No `IdentityService` dependency**, unchanged reasoning from
the prior revision (Architecture Open Question 5 remains genuinely open;
nothing this unit does requires attribution).

**Explicitly not a dependency, unchanged:** `PlannerRuntime`,
`ExecutionPipeline`, `PermissionEngine`, `ToolRegistry`,
`ToolInvocationBinding`, `MemoryStore`, `WorldModel`, `ModuleRegistry`,
`ConversationEngine`, `ResponseDelivery`, `ModelManager`.

## 4a. Prompts Are Transient Implementation Artefacts, Not Platform State

**Stated explicitly because Parker's three-layer knowledge architecture —
Memory, the World Model, and Reasoning Context (`reasoning-context.md`) —
is a core architectural principle, and a prompt string must not be
mistaken for a fourth layer, or for an instance of any of the three
existing ones:**

- **A prompt exists only for the duration of one `reason` invocation.**
  `ReasoningPromptBuilder.buildPrompt` constructs it fresh from
  `request.turn` and `request.reasoningContext` on every call (Section 5);
  nothing in this design retains a prompt after the `modelInferenceClient.infer`
  call it was built for returns or faults.
- **A prompt is not Parker Memory.** It is never written to, read from, or
  otherwise touches `MemoryStore` — restating Section 4's own dependency
  list: `ModelReasoningProvider`, `ReasoningPromptBuilder`, and
  `ModelInferenceClient` hold no dependency on `MemoryStore` at all.
- **A prompt is not the World Model.** It is never written to, read from,
  or otherwise touches `WorldModel` — same structural absence.
- **A prompt is not Reasoning Context.** `ReasoningContext` (`reasoning-context.md`;
  `src/interfaces/ReasoningProvider.kt`) is an existing, already-governed
  Parker contract — an already-assembled, task-scoped working set this
  unit only ever *consumes* (`request.reasoningContext`), never
  constructs. A prompt is a *derivative* of a `ReasoningContext` (and of a
  `Turn`), produced by formatting them into a string for one specific
  model call — it is not, and does not become, a `ReasoningContext` value
  itself, is never substituted for one, and is never passed to anything
  expecting one.
- **A prompt is a disposable implementation artefact of
  `ReasoningPromptBuilder` alone.** It has no identifier, no persistence
  mechanism, no lifecycle beyond a single method call's own return value,
  and no Parker contract gives it a shape — restating Section 2's own
  finding that this unit introduces no new public contract type.
- **A prompt is never treated as persistent platform state, under any
  circumstance.** No proposed class (Section 13) logs, caches, stores, or
  otherwise retains a prompt beyond the `buildPrompt` call that produced
  it and the `infer` call that consumed it. If a future need for retaining
  prompts (for example, for audit or debugging) is ever identified, that
  is a separate, later, explicitly-scoped decision — governed by the same
  discipline `reasoning-context.md`'s own "promotion into Memory is never
  automatic" principle already establishes for Reasoning Context itself —
  not something this unit does by default or by omission.

## 5. Lifecycle

Revised to reflect the three-collaborator orchestration (Section 4):

1. **Invocation.** `ModelReasoningProvider.reason(request)` is called by
   whatever caller holds standing to do so.
2. **Prompt construction (synchronous, delegated).**
   `promptBuilder.buildPrompt(request.turn, request.reasoningContext)`
   returns a prompt string. No I/O; cannot itself time out or need
   cancellation.
3. **Interpretation (opaque to the architecture, delegated, time-bounded).**
   `withTimeout(timeoutMs) { modelInferenceClient.infer(prompt) }` returns a
   raw string, or the call is cancelled and faults (Section 8).
4. **Response parsing (synchronous, delegated).**
   `responseParser.parse(raw)` returns a classified
   `ReasoningProviderResponse`, or throws (Section 8).
5. **Return.** The classified response is returned unchanged.
   `ModelReasoningProvider`'s involvement ends here.
6. **No retained state.** `ModelReasoningProvider` holds only its three
   constructor-injected collaborators and `timeoutMs` as fields — no cache
   of any prior Turn, prompt, or response, on this class or on either
   collaborator (both proposed default implementations, Section 12, are
   likewise stateless).

## 6. Configurable Inference Endpoints — Replacing the Hard-Coded Ollama Assumption

**This section replaces the prior revision's Decision 6 in full.** The
earlier draft proposed `LocalHttpModelInferenceClient` with Ollama's
`/api/generate` convention effectively baked in. That is corrected here:

- **`endpointUrl: String` and `modelName: String` are required constructor
  parameters, with no default value.** The prior draft's `localhost:11434`
  default is removed — a caller must state its own actual local endpoint
  explicitly. This is a deliberate change from "sane default" to "explicit,
  required configuration," precisely because the prior default was a real,
  unverified guess about Steven's own machine (disclosed as Risk 6 in the
  prior revision) rather than a safe, universally-correct value.
- **Request/response formatting is itself injectable, not hard-coded into
  `LocalHttpModelInferenceClient`.** Two further constructor parameters:
  `requestBodyFormatter: (prompt: String, modelName: String) -> String` and
  `responseBodyParser: (rawResponseBody: String) -> String`. Each has a
  named, top-level **default** implementation shaped like Ollama's
  `/api/generate` convention (`defaultOllamaRequestBody`,
  `defaultOllamaResponseBody`) — supplied so the class ships usable
  out of the box, but overridable by passing different functions for any
  other local server's request/response shape (LM Studio, a llama.cpp
  server, text-generation-webui, or anything else) **without modifying
  `LocalHttpModelInferenceClient`'s own code.**
- **Net effect:** `LocalHttpModelInferenceClient` itself contains no
  server-specific assumption anywhere in its own class body — only in the
  two named, separately-swappable default functions supplied alongside it,
  which are themselves plain, pure, synchronous string-transform functions,
  independently testable without HTTP.

This directly addresses the risk the prior revision disclosed but did not
yet fix: the "genuine assumption about Steven's own local environment" is
no longer silently load-bearing — it is now a visible, named, overridable
default rather than an implicit requirement.

## 7. Model Independence

Unchanged in substance from the prior revision, strengthened by Section 4's
separation: **nothing in `ModelReasoningProvider`'s own code names a
specific model, vendor, prompt convention, or response format.** Swapping
`ModelInferenceClient` implementations changes zero lines of
`ModelReasoningProvider`; so does swapping `ReasoningPromptBuilder` or
`ReasoningResponseParser` implementations, independently of each other and
of `ModelInferenceClient`. Model independence is now enforced at three
separate seams instead of one, each individually swappable.

## 8. Error Handling, Cancellation, and Timeout Behaviour

Unchanged in substance from the prior revision, restated against the
three-collaborator design:

- **Error handling.** If `modelInferenceClient.infer` throws, or
  `responseParser.parse` throws (including the proposed
  `UnclassifiableModelResponseException` for an unrecognised leading tag,
  Section 12 Decision), `ModelReasoningProvider.reason` does not catch
  either — no `try`/`catch` anywhere in this class. `NoAction` is never
  used as a catch-all for either kind of failure, restating Contract Design
  Section 3's own explicit warning.
- **Cancellation.** Ordinary structured-concurrency cancellation applies
  throughout `reason`. The one place needing explicit design is
  `LocalHttpModelInferenceClient`'s use of
  `java.net.http.HttpClient.sendAsync` (`CompletableFuture`-based, not
  coroutine-native) — wrapped in `suspendCancellableCoroutine`, registering
  `continuation.invokeOnCancellation { future.cancel(true) }`, so a
  cancelled `reason` call actually cancels the in-flight HTTP request.
- **Timeout.** `withTimeout(timeoutMs)` wraps only the
  `modelInferenceClient.infer` call (Section 5, Step 3) — not prompt
  construction or response parsing, both synchronous and effectively
  instantaneous. `timeoutMs` is a `ModelReasoningProvider` constructor
  parameter, proposed default `30_000`. On expiry, `withTimeout` throws
  `TimeoutCancellationException` (an ordinary Kotlin coroutines exception,
  no new type needed), propagating exactly like any other fault.

## 9. Future Considerations: Streaming (Not Implemented, Not Designed in Detail)

**This section names an extension shape. It implements, designs, or commits
to nothing.**

Today, `ModelInferenceClient.infer` returns one complete `String`, and
`ReasoningProvider.reason` returns one complete `ReasoningProviderResponse`
— matching `REASONING_PROVIDER_CONTRACT_DESIGN.md` Section 9's own
already-decided deferral: "Streaming. Not decided... `ReasoningProvider.reason`
as defined here returns one complete `ReasoningProviderResponse`, not a
stream." This unit does not revisit that deferral.

**If a future unit adds streaming, the shape most consistent with this
design's own separation (Section 4) would be additive, not a modification
of today's contracts:** a separate interface (for example,
`StreamingModelInferenceClient`, with a method returning
`kotlinx.coroutines.flow.Flow<String>` of incremental text) that a future
`ModelReasoningProvider`-like orchestrator could consume — collecting the
stream into a final string before handing it to a `ReasoningResponseParser`
(unchanged), since `ReasoningProviderResponse` itself is not shaped to
carry partial/incremental results, and changing that would be a Contract
Design decision squarely outside this Sprint's authority. Today's
`ModelInferenceClient`, `ReasoningPromptBuilder`, and `ReasoningResponseParser`
would not need to change to support this — a streaming variant would sit
alongside them, not replace them. **No file, class, or interface for this
is proposed or created by this Sprint.**

## 10. Model Lifecycle Assumptions: Ownership, Loading, and Concurrency

**Named explicitly here because no prior document in this review states
them, and this unit's own correctness depends on the reader understanding
what it does and does not take responsibility for.**

- **Ownership.** `ModelReasoningProvider`, `ModelInferenceClient`, and
  `LocalHttpModelInferenceClient` own no model process, no model file, and
  no model weights — restating `REASONING_PROVIDER_ARCHITECTURE.md` Section
  4 directly: "A concrete implementation may hold model weights... none of
  that is a Parker-owned contract." Starting, stopping, installing,
  updating, or configuring the local model server this unit calls is
  entirely the owner's/operator's own responsibility, outside this unit's
  code and outside Parker's ownership model.
- **Loading.** This unit assumes the configured `endpointUrl` already has a
  model loaded and ready to serve inference requests at the moment
  `infer` is called. `LocalHttpModelInferenceClient` performs no health
  check, no warm-up call, and no "is a model loaded" verification before
  calling. If the endpoint is not ready — connection refused, a
  not-yet-loaded-model error response, or any other failure — that surfaces
  through the ordinary error-handling path (Section 8) as a thrown
  exception, not a distinct "not loaded" case this unit special-cases.
- **Concurrency.** `ModelReasoningProvider` and `LocalHttpModelInferenceClient`
  hold no mutable state (Section 5, Section 4) — Parker-side, they are safe
  to invoke concurrently; multiple simultaneous `reason` calls do not
  interfere with each other structurally. **Whether the underlying model
  server itself can actually serve concurrent requests — versus queueing,
  serialising, or rejecting them — is entirely outside this unit's
  knowledge or control.** This is a real characteristic of whatever server
  is actually configured at `endpointUrl`, not something this design
  measures, guarantees, or should be read as claiming.

## 11. Implementation Risks

Revised from the prior revision — Risk 6 (hard-coded Ollama assumption) is
resolved by Section 6 and removed; the remaining risks are restated,
renumbered:

1. **The sandbox this review is being produced in cannot exercise a real
   local model server.** The automated test suite must exercise
   `ModelReasoningProvider`'s orchestration, and each of its three
   collaborators, through fakes — never a real HTTP call.
   `LocalHttpModelInferenceClient` and the two default Ollama-shaped
   formatting functions (Section 6) will be implemented but not exercised
   by the automated test suite — a disclosed, unverified gap, not a false
   completeness claim.
2. **Response classification remains the single riskiest, most
   interpretive design choice**, now isolated inside the
   `ReasoningResponseParser` interface (Section 4) rather than buried in
   orchestration — an improvement in blast radius (a wrong convention now
   only affects one small, focused, replaceable implementation, not the
   frozen interface or `ModelReasoningProvider` itself) but not a
   reduction in the underlying risk for whichever implementation is
   actually configured: the proposed default,
   `TaggedReasoningResponseParser` (Section 12 Decision 3), still depends
   on successfully instructing a real model to follow its tag convention,
   which this review cannot verify without one. Because
   `ReasoningResponseParser` is the frozen abstraction and
   `TaggedReasoningResponseParser` is only its first, replaceable
   implementation (Section 4), a future, more reliable parsing strategy —
   structured JSON, grammar-constrained output, function-calling, or
   schema-based responses — can supersede it without reopening this
   review or any architecture document.
3. **`ModelManager` confusion risk**, unchanged from the prior revision.
4. **Operating identity left unresolved (Architecture Open Question 5)**,
   unchanged from the prior revision.
5. **Local-only scope sidesteps, but does not resolve, Architecture Open
   Question 4 (remote-provider data egress)**, unchanged from the prior
   revision — restated with one addition: because `endpointUrl` is now
   fully configurable (Section 6) rather than hard-coded to `localhost`,
   nothing in `LocalHttpModelInferenceClient`'s own code *prevents* it
   being pointed at a remote address. This is a direct, disclosed
   consequence of Section 6's own fix — configurability that solves the
   hard-coding problem also removes the accidental safety a hard-coded
   `localhost` default provided. **This unit does not add a check
   restricting `endpointUrl` to local addresses**, since doing so would be
   inventing a new security policy this Sprint has no architectural
   authority to decide (Architecture Open Question 4 remains genuinely
   open). This is named here explicitly so it is not mistaken for an
   oversight.
6. **`Turn`/`ReasoningContext` construction in tests has no production
   caller yet**, unchanged from the prior revision.

## 12. Required Implementation Decisions

Renumbered and revised to reflect Sections 4–6:

1. **No operating identity (`IdentityService`)** for this first
   implementation (Section 4).
2. **`ReasoningPromptBuilder` default implementation** (proposed name:
   `DefaultReasoningPromptBuilder`): a simple, deterministic template —
   `reasoningContext.entries` (in order, one per line) followed by the
   owner's own message (`turn.message.text`), plus a fixed instruction
   requiring the model to prefix its reply with exactly one of `GOAL:`,
   `REPLY:`, or `NOACTION`. No further prompt engineering, few-shot
   examples, or persona instructions proposed.
3. **`ReasoningResponseParser` — the interface is the frozen architectural
   component; `TaggedReasoningResponseParser` is its default, first
   production implementation only, not the permanent parsing protocol.**
   The proposed default implementation, `TaggedReasoningResponseParser`,
   uses a required leading tag (`GOAL:`/`REPLY:`/`NOACTION`,
   case-sensitive, whitespace-trimmed before matching). Text after
   `GOAL:`/`REPLY:` becomes `Goal.text`/`Reply.text` (trimmed, re-validated
   non-blank via each type's own existing `init` block). `NOACTION` takes
   no trailing text. Any other leading token throws
   `UnclassifiableModelResponseException` (carrying the raw text). **This
   convention is a starting-point default, chosen for its simplicity and
   testability — not a claim that it is the best or only viable parsing
   strategy.** A future implementation may legitimately replace it with
   structured JSON, grammar-constrained output, function-calling, or
   schema-based responses, so long as it satisfies `ReasoningResponseParser`'s
   one-method shape — no architectural change, and no change to
   `ModelReasoningProvider`, would be required to do so.
4. **Timeout:** `withTimeout(timeoutMs)` around the `infer` call only
   (Section 8), constructor parameter, proposed default `30_000` ms.
5. **Cancellation:** `suspendCancellableCoroutine` wrapping
   `HttpClient.sendAsync`'s `CompletableFuture` inside
   `LocalHttpModelInferenceClient` (Section 8).
6. **`LocalHttpModelInferenceClient` configuration:** `endpointUrl` and
   `modelName` required, no default (Section 6). `requestBodyFormatter`/
   `responseBodyParser` default to Ollama-shaped functions, overridable.
   **This remains the one item most likely to need a different default at
   Scope Lock — now correctable without touching the class itself, only
   the arguments passed to it.**
7. **Class/file naming:** `ModelReasoningProvider`, `ReasoningPromptBuilder`,
   `DefaultReasoningPromptBuilder`, `ModelInferenceClient`,
   `LocalHttpModelInferenceClient`, `ReasoningResponseParser`,
   `TaggedReasoningResponseParser`, `UnclassifiableModelResponseException`
   (Section 13).

## 13. Proposed Kotlin Classes

| Class | Location | Role |
| --- | --- | --- |
| `ModelReasoningProvider` | `src/runtime/ModelReasoningProvider.kt` | Concrete `ReasoningProvider` implementation. Pure orchestrator (Section 4) — owns nothing but the timeout wrap. |
| `ReasoningPromptBuilder` | `src/runtime/ReasoningPromptBuilder.kt` | New `fun interface` — `fun buildPrompt(turn: Turn, reasoningContext: ReasoningContext): String`. Generic, non-Parker-contract utility. |
| `DefaultReasoningPromptBuilder` | same file | Proposed default implementation (Section 12, Decision 2). |
| `ModelInferenceClient` | `src/runtime/ModelInferenceClient.kt` | `fun interface` — `suspend fun infer(prompt: String): String`. The model-call seam. |
| `LocalHttpModelInferenceClient` | `src/runtime/LocalHttpModelInferenceClient.kt` | Concrete, production, local-first `ModelInferenceClient` — fully configurable endpoint/model/formatting (Section 6). Not exercised by the automated test suite (Risk 1). |
| `defaultOllamaRequestBody`, `defaultOllamaResponseBody` | same file | Named, top-level, overridable default formatting functions (Section 6) — not hard-coded into the class body. |
| `ReasoningResponseParser` | `src/runtime/ReasoningResponseParser.kt` | New `fun interface` — `fun parse(raw: String): ReasoningProviderResponse`. **The frozen architectural component** — any implementation satisfying this one-method shape is a legitimate parsing strategy (Section 4, Section 12 Decision 3). |
| `TaggedReasoningResponseParser` | same file | **First, default production implementation only — not the permanent protocol.** A future JSON/grammar-constrained/function-calling/schema-based implementation may replace it without touching `ReasoningResponseParser` or `ModelReasoningProvider` (Section 12, Decision 3). |
| `UnclassifiableModelResponseException` | same file | Thrown when `raw` does not match the classification convention. Plain `RuntimeException` subtype, no new Parker contract. |
| `FakeModelInferenceClient`, `FakeReasoningPromptBuilder`, `FakeReasoningResponseParser` | `tests/runtime/` | Test-only fakes, lambda-based, mirroring `FakeReasoningProvider`'s own established precedent — one per collaborator, enabling `ModelReasoningProviderTest` to exercise orchestration independently of any real model, and each collaborator's own default implementation to be tested independently in its own test file. |

No new file under `src/interfaces/` — this unit adds no public Parker
contract type (Section 2).

## 14. Proposed Package Layout

Unchanged from the prior revision. No new package. Everything above lives
in `parker.core.runtime` (`src/runtime/`), reusing the existing convention.
`ReasoningProvider` itself remains, unmodified, in `parker.core.interfaces`.
Test fakes live in `tests/runtime/`.

---

## Conclusion

**The existing `ReasoningProvider` contract already fully supports a
production implementation.** This revision separates what the prior draft
folded into one class into three small, independently-testable,
independently-swappable collaborators — prompt construction
(`ReasoningPromptBuilder`), model inference (`ModelInferenceClient`), and
response parsing (`ReasoningResponseParser`) — with `ModelReasoningProvider`
reduced to pure orchestration. The prior draft's hard-coded Ollama
assumption is replaced with required, explicit endpoint/model configuration
and overridable, named default formatting functions, so no server-specific
assumption is silently load-bearing. A future streaming addition and this
unit's own model lifecycle assumptions (ownership, loading, concurrency)
are now named explicitly rathe