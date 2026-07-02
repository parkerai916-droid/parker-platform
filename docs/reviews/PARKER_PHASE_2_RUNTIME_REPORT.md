# Parker Platform — Phase 2 Runtime Implementation Report

Produced on `feature/phase-2-runtime` (branched from
`feature/v0.7-architecture-completion` at commit `cabc4b6`). 7 commits,
all listed below. Compilation/test execution could not be run in this
sandbox (no `kotlinc`/`gradle` toolchain and no network access to fetch
one — see §4).

## 1. Files Created

- `src/contracts/ToolLifecycle.kt` — `ToolLifecycleState`,
  `ToolLifecycleTransitions`.
- `src/contracts/ToolResolution.kt` — `ToolResolutionFailureReason`,
  `ToolResolution`, `ToolRegistrationOutcome`.
- `src/contracts/ActionMapping.kt` — `ActionResourceMapping`,
  `ActionVocabularyEntry`, `ActionMappingFailureReason`,
  `ActionMappingResult`, `VocabularyRegistrationOutcome`.
- `src/contracts/EventContracts.kt` — `EventType`, `ParkerEvent`,
  `EventHandler`, `Subscription`, `PublishResult`.
- `src/interfaces/ToolRegistry.kt` — the `ToolRegistry` interface.
- `src/runtime/InMemoryResourceRegistry.kt` — supporting dependency (see §5).
- `src/runtime/InMemoryToolRegistry.kt` — concrete `ToolRegistry`.
- `src/runtime/ActionMapper.kt` — `ActionVocabulary`,
  `InMemoryActionVocabulary`, `ActionMapper`.
- `src/runtime/InMemoryEventBus.kt` — concrete `EventBus`,
  `PrincipalAuthenticator`, `AllowAllPrincipalAuthenticator`.
- `tests/runtime/InMemoryResourceRegistryTest.kt`
- `tests/runtime/InMemoryToolRegistryTest.kt`
- `tests/runtime/ActionMapperTest.kt`
- `tests/runtime/InMemoryEventBusTest.kt`
- `docs/reviews/PARKER_PHASE_2_RUNTIME_REPORT.md` (this document)

## 2. Files Modified

- `src/contracts/ToolDescriptor.kt` — added `supportedActions: Set<PermissionAction>`
  and `supportedResourceTypes: Set<ResourceType>` (both default to empty;
  existing Phase 1 usages unaffected).
- `build.gradle.kts` — added `src/runtime` to the main source set;
  added `tests/runtime` to the test source set; removed `EventBus.kt`
  from the Phase 1 exclude list now that its dependent types exist.
- `docs/architecture/IMPLEMENTATION_GAPS.md` — items 21-29 appended.

## 3. Tests Added

38 new test cases across 4 files:

- `InMemoryResourceRegistryTest` (6 tests) — register/resolve/update/
  listByOwner, duplicate-registration rejection, resolve-of-unknown
  returns null.
- `InMemoryToolRegistryTest` (14 tests) — registration against a missing/
  wrong-type Resource, idempotent duplicate registration, rejection of a
  changed descriptor under an unchanged toolId+version, version
  supersession (old → Deprecated, new → Enabled) and the resulting
  transition restriction, deterministic `resolve()` across
  `TOOL_NOT_FOUND`/`TOOL_DISABLED`/`TOOL_AMBIGUOUS`/success,
  `findCandidates` Enabled-only filtering, `listAll` completeness,
  lifecycle transition enforcement (valid and invalid), unknown-tool
  error.
- `ActionMapperTest` (9 tests) — unknown-action handling (Invalid, not
  Denied), resource-type mismatch, composite actions, multiple actions
  resolved independently, `allResolved()`'s all-or-nothing rule,
  ambiguous-action prevention by construction, idempotent vocabulary
  re-registration.
- `InMemoryEventBusTest` (11 tests) — `EventType` validation, zero-
  subscriber publish, exact-eventType delivery, multi-subscriber
  delivery, handler-failure isolation, idempotent cancellation, publisher
  authentication rejection, trust-sensitive signature requirement
  (present and absent).

Every test proving a documented rule references the specific
architecture/specification section it proves in its test name or a
neighbouring comment.

## 4. Build/Test Result

**Not run — sandbox limitation, disclosed rather than assumed.** This
sandbox has no `gradle`, `gradlew`, or `kotlinc` installed, and no network
access to `repo.maven.apache.org` or `github.com` to fetch a toolchain
(`403 blocked-by-allowlist`, confirmed again this session). This is the
same limitation disclosed in every prior phase's completion report for
this repository.

In place of compilation, verification for this phase consisted of:

- Careful manual cross-referencing of every new type against the
  interfaces/data classes it depends on (exact field names, parameter
  order, nullability, suspend-ness) — e.g. `InMemoryToolRegistry`'s
  methods checked line-by-line against `ToolRegistry.kt`'s signatures,
  `ParkerEvent`'s fields checked against `Event.schema.json`.
  `EventBus`/`ToolRegistry`/`ResourceRegistry` interface conformance was
  the most safety-critical check, since these are pre-existing contracts
  this code must not silently diverge from.
- A scripted brace/paren/bracket balance check across all 14 new/modified
  `.kt` files (string literals and comments stripped first) — all
  balanced.
- Manual review of Kotlin-specific correctness points that are easy to
  get wrong: `val`-overriding-with-`var` legality (`Subscription.active`),
  `singleOrNull`'s null-on-zero-or-multiple semantics (used in version
  supersession detection), smart-casting inside a `when` branch after a
  `null ->` branch (`InMemoryActionVocabulary.register`), and suspend-call
  legality inside inline lambdas (`Iterable.map`, `Mutex.withLock`).

**This is not a substitute for actually compiling and running the test
suite.** The recommendation in §6 reflects that these tests have not
actually been executed.

## 5. Gaps Found

Recorded in full in `docs/architecture/IMPLEMENTATION_GAPS.md` items
21-29. Summary:

- No Volume 3 `ToolRegistry.md` document exists yet (the architecture-level
  `docs/architecture/tool-registry.md` was implemented directly);
  recommended as a backfill follow-up now that the interface has been
  proven against real usage.
- `InMemoryResourceRegistry` was added as a necessary supporting
  dependency — not one of the three requested systems, but required by
  Tool Registry's own registration invariant, and already an earlier
  `IMPLEMENTATION_ORDER.md` phase.
- Tool Registry's discovery surface (`listAll`/`findCandidates`) has no
  Principal-scoped visibility filtering — blocked on IdentityService,
  which remains unimplemented by design.
- Neither Tool registration/lifecycle changes nor `ActionMapper`'s output
  are gated by a live, policy-bearing `PermissionEngine.evaluate` — no
  authorisation policy is specified anywhere yet, so none was invented.
- EventBus authentication and signature-verification are placeholders
  (`AllowAllPrincipalAuthenticator`; presence-check, not cryptographic,
  signature validation) — real IdentityService integration and a signing
  scheme are both prerequisites not yet built/specified.
- `EventBus.subscribe`'s existing Volume 3 interface has no caller
  parameter, so subscriber Principal identity is currently a placeholder
  string — cascading cancellation on Principal Revoke is not implemented
  as a result.

## 6. Remaining Risks

- **Unverified compilation.** As stated in §4, none of this code has been
  compiled or test-run. It should not be treated as working until it has
  been built with an actual Kotlin/Gradle toolchain.
- **Concurrency model is provisional.** `InMemoryToolRegistry`/
  `InMemoryResourceRegistry` use a single coroutine `Mutex` around all
  operations (correct but coarse-grained); `InMemoryEventBus` uses
  `ConcurrentHashMap`/`CopyOnWriteArrayList` for its non-suspend
  `subscribe`. Nothing in the architecture docs specifies a required
  concurrency granularity beyond "safe to call concurrently," so this is
  a reasonable but unvalidated choice.
- **In-memory only.** None of the three systems persist anything —
  appropriate for this phase (no persistence architecture has been
  specified), but worth flagging before anyone assumes otherwise.
- **The four gaps in §5 involving IdentityService/PermissionEngine policy
  are real functional gaps, not just documentation gaps** — this code
  cannot yet enforce the trust guarantees the architecture describes
  end-to-end; it implements the mechanical parts (registries, mapping,
  pub/sub) those guarantees will eventually sit on top of.

## 7. Whether Phase 2 Runtime Foundations Are Complete

**Mechanically complete for the three requested systems; not trust-complete.**

Tool Registry, Action Mapping, and EventBus (with `ParkerEvent`) are each
implemented per their respective architecture documents, with tests
covering every behaviour those documents required (registration,
discovery, lookup, duplicate/version handling, disabled/unavailable
handling for Tool Registry; unknown/ambiguous/multiple/composite action
handling for Action Mapping; publish/subscribe/cancel/PublishResult/
ordering/failure-isolation for EventBus). No tool execution path bypasses
the pattern these systems establish.

What is **not** complete, and cannot be until later phases: real
Permission Engine policy evaluation, IdentityService-backed principal
resolution, and therefore the live trust enforcement (discovery
visibility, registration gating, cascading revocation) the architecture
documents describe as the end state. This phase built the pipes; the
water pressure (policy, identity) isn't connected yet — consistent with
`IMPLEMENTATION_ORDER.md` placing Identity Service and a policy-bearing
Permission Engine in earlier/parallel phases this work assumes rather
than reimplements.

## 8. Recommended Next Branch or Milestone

1. **Compile and run this branch** with a real Kotlin/Gradle toolchain
   before building anything further on top of it — this is the single
   highest-priority next step, since nothing here has been verified to
   compile.
2. **`feature/identity-service`** — implement `IdentityService` per
   `docs/architecture/IdentityService.md`, closing the two largest
   remaining gaps this phase surfaced (§5): discovery visibility and
   authentication being real rather than placeholders.
3. Once Identity Service exists: **wire a real `PermissionEngine.evaluate`**
   using `ActionMapper`'s output plus actual authorisation policy (still
   unspecified — will need its own architecture pass, similar in kind to
   `action-mapping.md`, before implementation).
4. Backfill `docs/specifications/volume-03-core-interfaces/ToolRegistry.md`
   (item 21) now that the interface has been implemented and exercised.
