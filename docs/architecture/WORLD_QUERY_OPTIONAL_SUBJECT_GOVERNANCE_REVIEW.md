# WorldQuery Optional Subject — Governance Review

## Status

**Implemented**, following this review's own recommendation and
Steven's explicit, separate go-ahead. See
`docs/architecture/WORLD_QUERY_OPTIONAL_SUBJECT_CONTRACT_REVISION.md`
Section 1 for the as-implemented field shape and
`docs/implementation/IMPLEMENTATION_HISTORY.md`, "WorldQuery Optional
Subject — Contract Revision Implementation," for the full implementation
record, including this session's own inability to complete Gradle
verification in its sandbox.

**Sprint 11, narrowly-scoped contract-revision review, separate from and
preceding Unit 8 implementation.** Evaluated, on architecture,
compatibility, and scope grounds only, the proposed revision
`docs/architecture/WORLD_MODEL_SOURCE_QUERY_CONSTRUCTION_DECISION.md`
identified as the smallest fix for Unit 8's blocking incompatibility:
`WorldQuery.subjectMatch: String` → `subjectMatch: String? = null`, with
`null` meaning "no subject filter." **This document, on its own, did not
approve implementation** — that approval was given separately,
afterward, per the Status note above.

---

## Existing Behaviour

Read fresh, directly, immediately before this document was written:
`src/interfaces/WorldModel.kt`, `src/runtime/InMemoryWorldModel.kt`,
`docs/architecture/WORLD_MODEL_CONTRACT_DESIGN.md` Section 4,
`tests/contracts/WorldModelContractsTest.kt`, and
`tests/runtime/InMemoryWorldModelTest.kt`.

**Current `WorldQuery` fields:**

```kotlin
data class WorldQuery(
    val subjectMatch: String,
    val maximumResults: Int,
    val minimumConfidence: Double? = null,
)
```

**Validation rules** (the type's own `init` block):

- `subjectMatch` must not be blank (`require(subjectMatch.isNotBlank())`).
  No default value exists; every construction site must supply one.
- `maximumResults` must be `>= 1`.
- `minimumConfidence`, if present, must be in `0.0..1.0`. Already
  optional, already defaulted to `null`.

**Filtering behaviour** (`InMemoryWorldModel.query`):

```kotlin
override suspend fun query(query: WorldQuery): List<WorldBelief> = mutex.withLock {
    beliefs.values
        .filter { belief ->
            belief.subject.contains(query.subjectMatch, ignoreCase = true) &&
                (query.minimumConfidence == null || belief.confidence >= query.minimumConfidence) &&
                updatePolicy.isStillCurrent(belief)
        }
        .take(query.maximumResults)
}
```

Three filter conditions, all `&&`-combined: a case-insensitive substring
match of `subjectMatch` against `belief.subject`; an optional
`minimumConfidence` floor, already skipped when `null`; and a call to
`updatePolicy.isStillCurrent`, excluding any belief judged stale. Note
precisely: **the existing code already contains the exact
null-means-skip-this-filter pattern the proposed revision would extend
to a second field** — `minimumConfidence == null` short-circuits its own
condition to `true`. No equivalent branch exists for `subjectMatch`,
because its type does not permit `null` today.

**Result limiting:** `.take(query.maximumResults)`, applied last, after
every filter — unconditional and unaffected by any other field.

**Confidence filtering:** `query.minimumConfidence == null || belief.confidence >= query.minimumConfidence`
— already optional, already the exact pattern this review recommends
mirroring for `subjectMatch`.

**Ordering semantics:** none guaranteed. `InMemoryWorldModel.query`'s own
KDoc: "No ranking or scoring formula is applied — results are returned in
whatever order the underlying map iterates... a caller must not depend on
any particular ordering." Confirmed unaffected by this proposal — nothing
about making `subjectMatch` nullable touches ordering.

**All known callers, confirmed by repository-wide search
(`grep -rn "WorldQuery(" --include="*.kt" .`):**

- `src/interfaces/WorldModel.kt` — the type's own declaration, not a call
  site.
- `tests/contracts/WorldModelContractsTest.kt`, `worldQuery(...)` helper
  (line 143) — six tests, every one supplying an explicit, non-blank
  `subjectMatch` (default `"device"` at the *test helper* level, not the
  type level).
- `tests/runtime/InMemoryWorldModelTest.kt`, `query(...)` helper (line
  55) — five call sites via `model.query(query(...))`, every one
  supplying an explicit, non-blank `subjectMatch`.
- **No production call site exists anywhere.** `WorldModel`/`InMemoryWorldModel`
  has no production wiring (confirmed previously,
  `WORLD_MODEL_SOURCE_GOVERNANCE_REVIEW.md` Finding 1, and reconfirmed by
  this review's own fresh grep of `ParkerRuntime.kt`, no match).

**Whether any caller depends on `subjectMatch` always being non-null:**
**No.** Every existing call site — test or production — already supplies
an explicit, non-blank `String` literal or parameter. None constructs a
`WorldQuery` by omitting `subjectMatch` (impossible today, no default
exists) and none inspects `WorldQuery.subjectMatch` for nullity anywhere
in the reviewed source. Widening the field's declared type to `String?`
changes nothing observable for any of them, because a non-null `String`
value remains a perfectly valid value for a `String?`-typed parameter or
property in Kotlin.

---

## Proposed Semantics — Assessed Exactly As Stated

- **`subjectMatch` becomes nullable with default `null`.** Consistent
  with `minimumConfidence`'s own existing shape on the same type — no new
  pattern is introduced, an existing one is extended to a second field.
- **`null` means no subject filtering.** Requires one code change in
  `InMemoryWorldModel.query`: the subject-match condition becomes
  `(query.subjectMatch == null || belief.subject.contains(query.subjectMatch, ignoreCase = true))`,
  the identical shape already used for `minimumConfidence` one line below
  it.
- **Non-null values remain subject to the existing non-blank
  validation.** The `init` block's `require` becomes
  `require(subjectMatch == null || subjectMatch.isNotBlank())` — a
  supplied, non-null value must still be non-blank; only the
  never-supply-one case changes.
- **`maximumResults` remains mandatory and positive.** Confirmed:
  nothing in this proposal touches `maximumResults` or its own `require`.
- **`minimumConfidence` remains unchanged.** Confirmed: this proposal
  adds no new interaction between `subjectMatch` and `minimumConfidence`
  — the three filter conditions remain independently `&&`-combined,
  exactly as today.
- **No ranking or ordering guarantee is introduced.** Confirmed: nothing
  in this proposal touches `InMemoryWorldModel.query`'s own "whatever
  order the underlying map iterates" behaviour, or adds a new guarantee
  where none exists today.
- **Existing subject-filtered queries retain identical behaviour.**
  Confirmed: a `WorldQuery` constructed with a non-null `subjectMatch`
  takes the identical code path as today — the `||` short-circuits to the
  existing `.contains(...)` check whenever `subjectMatch != null`, so
  every currently-passing test's own behaviour is bitwise unchanged.

**This review does not broaden the proposal.** No additional field,
method, or behaviour beyond the seven bullets above is assessed or
recommended.

---

## Compatibility

- **Source compatibility.** Fully preserved. Every existing call site
  supplies a named, non-null `String` argument for `subjectMatch`; a
  non-null `String` is a valid argument for a `String?`-typed parameter
  in Kotlin with no call-site change required. No existing `.kt` file
  needs to change to keep compiling.
- **Binary compatibility, where relevant to this repository.** This
  repository has no external consumers of `WorldQuery`'s compiled
  bytecode (it is a single-module Gradle build, not a published library),
  so binary compatibility in the "external consumer" sense does not
  apply. Within the module itself: Kotlin nullability is enforced at the
  call site by the compiler and via injected `Intrinsics.checkNotNullParameter`
  calls for non-null parameters, not encoded in the JVM descriptor itself
  (`String` and `String?` both erase to `Ljava/lang/String;`); relaxing
  `subjectMatch` to nullable removes a compile-time non-null check that
  was never actually exercised by any caller supplying `null` (none does,
  and none could, today), so no existing compiled or interpreted
  behaviour changes. Adding a default value (`= null`) does add a
  synthetic default-parameters bridge the class does not currently have
  for this field — additive, not breaking.
- **Behavioural compatibility.** Fully preserved for every subject-filtered
  query, per the "existing subject-filtered queries retain identical
  behaviour" assessment above. A genuinely new behaviour (an unfiltered
  read) becomes reachable only when a caller deliberately constructs a
  `WorldQuery` with `subjectMatch = null` — impossible before this
  revision, so this is additive capability, not a change to any existing
  path.
- **Test compatibility.** Fully preserved. Both `WorldModelContractsTest.kt`
  and `InMemoryWorldModelTest.kt` were read in full for this review; zero
  existing tests supply `subjectMatch = null`, expect a compile error for
  omitting it, or otherwise depend on non-nullability. All existing
  assertions remain valid, including
  `` `a WorldQuery with a blank subjectMatch is rejected` `` (still
  rejected, since `""` is non-null but blank, and the revised validation
  preserves the non-blank check for any supplied, non-null value).
- **Effect on existing callers.** None. Zero production callers exist;
  the two test files' own helper functions (`worldQuery(...)`,
  `query(...)`) already supply explicit values by name and require no
  edit to keep compiling or passing.
- **Whether default-null creates any accidental unbounded read risk.**
  Bounded, not unbounded — see Risks, below, for the precise reasoning:
  `maximumResults` remains mandatory and positive regardless of
  `subjectMatch`, so the *result size* is always capped even when the
  *candidate set considered* becomes every currently-non-stale belief.
- **Whether `maximumResults` is sufficient to keep reads bounded.**
  **Yes**, for result size. It does not bound the *cost* of producing
  that result (see Risks, below, for the distinction), but it does
  guarantee the caller-visible result list can never exceed the
  caller-stated size, with or without a subject filter — this guarantee
  is entirely independent of `subjectMatch`'s own value.

---

## Ownership and Responsibility

- **World Model implementation owns filtering.** Confirmed unchanged:
  the only code that decides how `subjectMatch` (when present) is
  matched, how staleness is judged, and in what order results are
  produced remains `InMemoryWorldModel.query`'s own, sole implementation.
  This proposal adds one more branch to that same implementation's own
  filter predicate; it does not move that responsibility anywhere else.
- **Callers express retrieval intent only.** Confirmed unchanged: a
  caller supplying `subjectMatch = null` is expressing "I have no subject
  to narrow by," exactly as a caller supplying `minimumConfidence = null`
  today expresses "I have no confidence floor." Neither expresses, nor
  could express, *how* the World Model should search — that remains
  entirely internal.
- **`null` does not mean "infer a subject."** Confirmed by construction:
  there is no code path, existing or proposed, in which `InMemoryWorldModel.query`
  reacts to `subjectMatch == null` by attempting to guess, derive, or
  classify a subject from anything. `null` collapses the subject
  condition to `true` — "match every belief regardless of subject" — the
  literal opposite of inference; it removes a criterion rather than
  synthesising one.
- **`DefaultReasoningContextAssembler` does not classify or parse request
  text.** Confirmed as a consequence, not merely an intention: this
  proposal is evaluated, and would be implemented, entirely inside
  `WorldQuery`/`InMemoryWorldModel` — no change to
  `DefaultReasoningContextAssembler` is included in this review's own
  scope (Scope Lock, below). Whatever the Assembler eventually does with
  a nullable `subjectMatch` remains Unit 8's own, separate, still-open
  question; this review does not resolve it and does not need to in order
  to evaluate the `WorldQuery` revision on its own terms.
- **The revision does not move reasoning into `WorldModelSource`.**
  Confirmed: `WorldModelSource` does not exist yet (Unit 8 remains
  blocked); this proposal touches only `WorldQuery` and
  `InMemoryWorldModel.query`, both pre-existing Sprint 4 contracts. No
  reasoning, decision, or interpretation is added anywhere by this
  change — it removes a mandatory-value constraint, it does not add a
  behaviour.

---

## Alternatives Considered

1. **Nullable `subjectMatch` (the proposal).** Minimal: one field's type
   widened, one existing null-check pattern extended by one field, one
   `require` relaxed. Fully backward compatible (Compatibility, above).
   Architecturally clean: reuses an already-established convention on the
   same type rather than introducing a new one.
2. **A separate unfiltered query method** (for example,
   `WorldModel.queryAll(maximumResults, minimumConfidence)`). Adds a
   fourth public operation to an interface `WORLD_MODEL_CONTRACT_DESIGN.md`
   Section 6 already determined needs no additional wrapper or sibling
   method ("no separate `WorldModelRuntime`... this remains the World
   Model's one public interface"). Two methods expressing overlapping
   capability (`query` with `subjectMatch = null` vs. a dedicated
   `queryAll`) is more surface, not less, for no behavioural gain over
   Option 1 — rejected for the same minimalism reasoning
   `MEMORY_SOURCE_CONTRACT_DESIGN.md` already applied when it declined to
   expose both `MemoryStore.retrieve` and a hypothetical narrower
   `MemoryStore.retrieveOne` through `MemorySource`.
3. **A wildcard sentinel string** (for example, `subjectMatch = "*"` or
   `subjectMatch = ""` with special-cased meaning). Strictly worse than
   Option 1: it repurposes a value already given a different, tested
   meaning (`""` is already asserted, by an existing, passing test, to be
   *rejected* as invalid) or invents a magic-string convention with no
   type-level enforcement — a caller could misspell or otherwise fail to
   use the sentinel and silently get ordinary substring-match behaviour
   instead of the intended unfiltered read, a class of bug nullable types
   do not admit. Rejected on both correctness and clarity grounds.
4. **A new query-mode enum** (for example,
   `enum class WorldQueryMode { SUBJECT_MATCH, UNFILTERED }` alongside a
   still-mandatory `subjectMatch` field, or a sealed `WorldQuery` type
   with `BySubject`/`All` variants). The most structurally elaborate
   option, and unjustified by any repository evidence reviewed here: no
   caller, test, or governing document identifies a need for more than
   two states ("filtered" / "unfiltered") that `String?` does not already
   express exactly as economically as a two-case enum or sealed type
   would, at strictly more implementation and call-site cost (every
   existing caller would need to additionally specify a mode, not merely
   continue passing what it already passes). Rejected as unjustified
   structural complexity — the same "do not create types merely for
   symmetry or anticipated need" discipline `WORLD_MODEL_CONTRACT_DESIGN.md`
   Section 4 itself already applied when it declined a requesting-Principal
   or correlation field for `WorldQuery`.
5. **Leave the contract unchanged.** Viable only if Unit 8 is abandoned
   or a genuinely non-inventive structured subject identifier is later
   found to exist on the Assembler's own input — neither is the case
   today (`WORLD_MODEL_SOURCE_QUERY_CONSTRUCTION_DECISION.md` Determination
   6). Leaving the contract unchanged leaves Unit 8 permanently blocked
   with no identified path forward; rejected as a non-resolution, not
   because the option itself is unsafe.

**Selected for recommendation: Option 1 (nullable `subjectMatch`).** It is
the least complex, most architecturally consistent (reuses an existing
pattern on the same type), and fully compatible option among those that
actually unblock Unit 8. No repository evidence reviewed here requires
adopting a more complex alternative.

---

## Risks

- **Accidental broad reads.** Possible in the sense that any caller
  supplying `subjectMatch = null` receives every currently-non-stale
  belief (subject to `minimumConfidence` and `maximumResults`), which is
  broader than any query possible today. This is the proposal's own,
  intended new capability, not a side effect — the risk is contained by
  `maximumResults` remaining mandatory (Compatibility, above) and by this
  revision, on its own, introducing no caller that actually passes `null`
  (Scope Lock excludes `WorldModelSource`/Assembler integration from this
  review's own included work).
- **Callers omitting subject unintentionally.** Mitigated by Kotlin's own
  named-argument convention already used throughout this codebase's test
  suites and, were this ever wired into production, would be mitigated
  further by requiring any future call site to state `subjectMatch = null`
  explicitly (a default of `null` does not silently activate itself for
  an existing named-argument call supplying a real value — it only
  changes behaviour for a call site that either omits the argument
  entirely or explicitly passes `null`).
- **Future performance implications.** An unfiltered `query` call, once a
  real caller exists, must still `.filter` every entry in `beliefs`
  before `.take(maximumResults)` — the filter pass itself is not skipped,
  only the subject-substring predicate within it. For the in-memory,
  single-process implementation this repository has today, this is the
  same cost class `query` already pays for any broad, low-selectivity
  `subjectMatch` (for example, a single-character substring already
  matches most entries) — this proposal does not introduce a new
  performance category, only a new instance of an existing one
  (unfiltered is the limiting case of "broad substring," not a distinct
  algorithm).
- **Ambiguity between `null` and blank.** Explicitly avoided by the
  proposal's own stated validation rule: `null` and `""` remain distinct,
  differently-handled values — `null` means "no filter," `""` remains
  invalid (rejected by `require`, preserving the existing, tested "blank
  subjectMatch is rejected" behaviour exactly). No caller can confuse the
  two, because only one of them (`""`) is even a legal non-null `String`
  value, and it continues to be rejected.
- **Impact of no ordering guarantee.** Unchanged, and arguably more
  visible once an unfiltered read is possible: a caller requesting "every
  belief" with no ordering guarantee receives a genuinely arbitrary
  subset (whichever `maximumResults` entries the underlying map happens
  to iterate first) whenever the true match count exceeds
  `maximumResults`. This is a pre-existing characteristic of
  `InMemoryWorldModel.query` this proposal does not change or worsen —
  disclosed here because an unfiltered query makes hitting that
  `maximumResults` ceiling on a large `beliefs` map more likely in
  practice, not because the proposal introduces a new absence of
  guarantee.
- **Whether the result limit sufficiently contains current risk.** Yes,
  for the result the caller ultimately sees (Compatibility, above). It
  does not contain the cost of the internal filter pass over every
  currently-held belief, which scales with however many beliefs the
  World Model holds in total, independent of `maximumResults` — a
  pre-existing characteristic of `query`'s own linear-scan
  implementation, not introduced or worsened by this proposal, and not
  identified by any reviewed test or document as a current problem at
  this repository's present scale.

---

## Readiness Determination

This proposal is narrow, additive, fully backward compatible with every
existing caller and test, reuses an already-established pattern on the
same type, and is supported directly by repository evidence (Existing
Behaviour, above). No risk identified above is unmitigated or requires a
more complex alternative than Option 1. This review recommends the
revision proceed to its own Contract Revision document and Scope Lock —
produced alongside this document — but **does not itself authorise
implementation**; see
`docs/architecture/WORLD_QUERY_OPTIONAL_SUBJECT_CONTRACT_REVISION.md` for
the accepted field-level shape and
`docs/implementation/WORLD_QUERY_OPTIONAL_SUBJECT_SCOPE_LOCK.md` for the
binding Included/Excluded terms.
