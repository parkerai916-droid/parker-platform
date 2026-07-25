# World Model Source — Query Construction Implementation Decision

## Status

**Superseded by implementation -- historical record preserved below,
unedited, per this Unit's own reconciliation instruction not to rewrite
history.** The blocker this document identified was separately resolved
(commit `eb25d64`; see this document's own Outcome section, below), and
Sprint 11 Unit 8 itself has since been implemented using exactly the
adopted construction that Outcome section records. See
`docs/implementation/IMPLEMENTATION_HISTORY.md`'s own "Unit 8 -- World
Model Source Integration Implementation" entry for the full record. Not
yet accepted pending Steven's own local build verification.

**Sprint 11, Unit 8. PES-001 Stage 4 (Implementation Decision), narrow
scope.** Resolves — or, as found below, determines it cannot yet
resolve — the one open question
`docs/architecture/WORLD_MODEL_SOURCE_CONTRACT_DESIGN.md` Section 3 named
and deliberately declined to answer: how
`DefaultReasoningContextAssembler` constructs the `WorldQuery` it would
pass to `WorldModelSource.recall`, specifically what value to supply for
`subjectMatch`.

**This document does not write Kotlin, does not modify tests, and does
not modify production code.** It records a repository-evidenced finding
and, because that finding is a blocking incompatibility, stops rather
than resolving around it, per this Unit's own explicit instruction.

---

## Context

Memory Source's own Contract Design (`MEMORY_SOURCE_CONTRACT_DESIGN.md`
Section 5) resolved an analogous question by supplying the Assembler's
current request text as `MemoryQuery.relevance`. That resolution was
defensible for a concrete, stated reason: `MemoryQuery.relevance` is
matched, by `InMemoryMemoryStore.retrieve`'s own already-implemented
logic, against `MemoryRecord.knowledgePayload` — free-text knowledge
content a natural-language request plausibly overlaps with. Reusing the
request's own text there did not invent a new correspondence; it supplied
a value into an existing, already-tested substring-match behaviour whose
target (free-text content) is the same kind of thing as the value
supplied (free-text request).

`WorldQuery.subjectMatch` cannot simply reuse that same resolution,
because it is matched against a categorically different kind of value:
`WorldBelief.subject` is a **structured topic key** (for example, a
device-state key or a location key), not free-text content. Supplying an
inbound request's natural-language text as a match target for a
structured key has no analogous existing correspondence to reuse — it
would require inventing a mapping from arbitrary request text to a
specific topic key, which is a classification or inference act, not a
value substitution. This Unit's own decision rule explicitly forbids
exactly that ("Do not use the request's free-form text as subjectMatch,"
"Do not introduce topic extraction, classification, semantic search,
embeddings, heuristics, or inference"). This is why Memory Source's own
resolution cannot simply be copied, and why this question was left open
rather than answered by analogy in the Contract Design.

---

## Existing Contract Facts

Read fresh, directly from `src/interfaces/WorldModel.kt` and
`src/runtime/InMemoryWorldModel.kt`, immediately before this document was
written:

```kotlin
data class WorldQuery(
    val subjectMatch: String,
    val maximumResults: Int,
    val minimumConfidence: Double? = null,
) {
    init {
        require(subjectMatch.isNotBlank()) { "WorldQuery.subjectMatch must not be blank" }
        require(maximumResults >= 1) {
            "WorldQuery.maximumResults must be at least 1, was $maximumResults"
        }
        require(minimumConfidence == null || minimumConfidence in 0.0..1.0) {
            "WorldQuery.minimumConfidence must be between 0.0 and 1.0 if present, was $minimumConfidence"
        }
    }
}
```

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

```kotlin
override suspend fun current(subject: String): WorldBelief? {
    require(subject.isNotBlank()) { "current(subject) requires a non-blank subject" }
    return mutex.withLock {
        val belief = beliefs[subject] ?: return@withLock null
        if (updatePolicy.isStillCurrent(belief)) belief else null
    }
}
```

Each of the six determinations this task requires, answered directly
against this text and against `tests/runtime/InMemoryWorldModelTest.kt`'s
20 existing tests:

**1. Whether `subjectMatch` is nullable, optional, or mandatory.**
**Mandatory.** `subjectMatch: String` — not `String?`, and carries no
default value, so every `WorldQuery` construction site must supply one.
It is additionally constrained to be non-blank by the `init` block's own
`require`. `current(subject: String)` imposes the identical constraint on
its own `subject` parameter.

**2. What absence or null means in the existing `WorldModel`
implementation.** **Nothing — the case cannot occur.** There is no
null-handling branch for `subjectMatch` in `query`'s filter predicate
(unlike `minimumConfidence`, which explicitly checks
`query.minimumConfidence == null` and treats null as "no floor"). Because
`subjectMatch`'s declared type is non-nullable, the Kotlin compiler
itself makes a null value impossible to construct, let alone interpret —
this is not an unimplemented case, it is a structurally unrepresentable
one under the current type.

**3. Whether an unfiltered, bounded read is already supported.** **No.**
Every existing read operation on `WorldModel` — both `current(subject)`
and `query(WorldQuery)` — requires a non-blank subject or `subjectMatch`
string. No operation exists, or could be called, with "no subject
constraint, just a bound and/or a confidence floor." Confirmed by
`InMemoryWorldModelTest.kt`: every one of its 20 tests supplies a
concrete, non-blank subject or `subjectMatch` value; none exercises an
unfiltered case, because none can under the current type.

**4. Whether `WorldQuery` has an existing result-limit field.** **Yes.**
`maximumResults: Int`, required, validated `>= 1`
(`InMemoryWorldModelTest.kt`: `"query never returns more than maximumResults"`).
This part of the preferred resolution ("caller supplies an
implementation-defined result bound") is already fully supported and
requires no contract change.

**5. Whether the World Model already defines deterministic ordering.**
**No.** `InMemoryWorldModel.query`'s own KDoc states plainly: "No ranking
or scoring formula is applied — results are returned in whatever order
the underlying map iterates... a caller must not depend on any
particular ordering beyond the filters and bound stated here." This is
weaker than `MemoryStore.retrieve`'s own explicit, deterministic
most-recently-promoted-first guarantee. World Model Source Contract
Design Section 6 already discloses this; nothing here changes that
finding.

**6. Whether any structured subject identifier is genuinely available to
the Assembler without inference, classification, parsing, or new
policy.** **No.** Read fresh against
`src/runtime/DefaultReasoningContextAssembler.kt` and the input types
available to it (`ResolvedInboundMessage`, `InboundOwnerMessage`): the
only fields available are `message.senderPrincipalId` (a `PrincipalId`),
`message.channelId` (a `ModuleId`), `message.text` (free-form natural
language), `message.timestamp`, `message.correlationId`, and
`resolvedMessage.conversationId`. None of these is, represents, or maps
onto a world-model topic key (a device-state key, a location key, or any
other `WorldBelief.subject` shape) without inventing a translation from
one namespace to the other. `PrincipalId`, `ModuleId`, `ConversationId`,
and `CorrelationId` are each identifiers for a different concept
entirely (a Principal, a communication channel, a Conversation, a
request correlation) — none is, or was ever intended to be, a world-model
subject key. Confirmed by `reasoning-context.md`'s own three-layer
separation and `PRODUCTION_REASONING_CONTEXT_CONTRACT_DESIGN.md` Section
2 ("no field of `InboundOwnerMessage` is a world-model concept") — no
governing document proposes such a field, and none should be invented
here.

---

## Options Considered

1. **Request text as `subjectMatch`.** Rejected outright by this Unit's
   own decision rule ("Do not use the request's free-form text as
   subjectMatch") and by the Context section above: `subjectMatch`
   matches a structured key, not free-text content, so this option would
   require inventing a text-to-key classification step — inference,
   explicitly excluded.
2. **`null` or absent `subjectMatch`.** Not available under the existing
   contract. `subjectMatch: String` is non-nullable and validated
   non-blank at construction; there is no code path, in either
   `WorldQuery`'s own `init` block or `InMemoryWorldModel.query`'s filter,
   that treats null or absence as "no subject filter." This option would
   require a contract change (see Option 4).
3. **Externally supplied structured subject.** Not genuinely available.
   No field on `InboundOwnerMessage`, `ResolvedInboundMessage`, or any
   other type reachable by `DefaultReasoningContextAssembler` represents
   a world-model subject key (Determination 6, above). No architecture
   document proposes associating a channel, Principal, or Conversation
   with a world-model subject namespace. Inventing such an association
   now, to unblock this Unit, would itself be new architecture decided in
   an implementation task — exactly what PES-001's "Architecture decides.
   Implementation follows." rule prohibits.
4. **Contract revision.** The only option with a concrete, minimal,
   evidenced path forward — detailed in Decision, below.

---

## Decision

**Implementation cannot proceed under the existing `WorldQuery` contract.
This document stops here, per this Unit's own explicit instruction, and
identifies the smallest contract revision that would unblock it — it does
not make that revision, and does not implement around it.**

Findings 1–3 and 6 together establish the exact incompatibility the task
asked this document to detect: `subjectMatch` is mandatory and
non-nullable (Finding 1); null already has no meaning under the current
type and cannot be given one without a signature change (Finding 2); no
existing `WorldModel` operation supports an unfiltered, bounded read
(Finding 3); and no structured subject identifier exists anywhere on the
Assembler's own input that could be supplied instead, without inventing
one (Finding 6). None of the four options above is available without
either violating this Unit's own decision rule (Options 1, 3) or
revising an already-approved Sprint 4 contract (Option 2, which is not
distinct in substance from Option 4 — achieving it requires the same
contract change).

**The smallest identified contract revision, named here for a future,
separate approval — not adopted by this document:**

- Change `WorldQuery.subjectMatch` from `String` to `String? = null`.
- Change `InMemoryWorldModel.query`'s filter predicate from
  `belief.subject.contains(query.subjectMatch, ignoreCase = true)` to
  `(query.subjectMatch == null || belief.subject.contains(query.subjectMatch, ignoreCase = true))`
  — the identical null-means-"no filter" pattern `minimumConfidence`
  already uses one line below it in the same predicate, so this
  introduces no new pattern, only extends an already-established one to
  a second field.
- No change would be required to `maximumResults` (Determination 4,
  already supports a caller-supplied bound) or to `current`'s own
  signature (unaffected — it is not part of `WorldModelSource`'s
  interface, per `WORLD_MODEL_SOURCE_CONTRACT_DESIGN.md` Section 2.2).

This revision, if approved, would make Option 2 (null/absent
`subjectMatch` meaning an unfiltered read) available, and would then
allow the Contract Design's own "preferred resolution" to be adopted
exactly as originally hoped: `subjectMatch = null`, caller supplies
`maximumResults`, the World Model implementation owns selection and
(non-guaranteed) ordering, and the Assembler preserves returned order
without ranking, scoring, or interpreting it.

**This document does not make that revision.** `WorldQuery` is an
already-approved Sprint 4, Track B contract
(`WORLD_MODEL_CONTRACT_DESIGN.md` §4), and changing its signature is not
within this narrow Implementation Decision's own authority — it requires
its own governance step (at minimum, a revision to
`WORLD_MODEL_CONTRACT_DESIGN.md` itself, since that document's own §4
currently states `subjectMatch` as a required field with no null case
considered). Proceeding to change `WorldQuery.kt`/`InMemoryWorldModel.kt`
now, without that separate approval, would be implementing around the
blocker this Unit's own instructions explicitly forbid.

---

## Rejected Options — Why Each Fails

- **Option 1 (request text as `subjectMatch`).** Fails architecturally,
  not merely by repository gap: it requires inventing a classification
  step (mapping arbitrary natural language to a specific structured
  topic key) with no existing precedent anywhere in this codebase. This
  is precisely the "inference" this Unit's own decision rule names and
  excludes, and precisely the kind of scope creep
  `WORLD_MODEL_SOURCE_INTEGRATION_SCOPE_LOCK.md`'s own Excluded list
  already rules out ("Inference... any mapping from a request to a
  specific `WorldQuery` must be non-inventive and disclosed").
- **Option 2 (null/absent `subjectMatch`), as currently written.** Fails
  on repository evidence alone: the field is non-nullable and validated
  non-blank at construction (Determination 1, 2). No amount of Assembler-
  side cleverness can supply a null value to a `String`-typed
  constructor parameter; this is a compile-time impossibility, not a
  behavioural gap.
- **Option 3 (externally supplied structured subject).** Fails on
  repository evidence: no such identifier exists anywhere on
  `InboundOwnerMessage` or `ResolvedInboundMessage` (Determination 6).
  Inventing one now would itself be a new architectural decision — a new
  concept ("what is this request's world-model subject?") with no
  antecedent in `reasoning-context.md`, `WORLD_MODEL_RUNTIME_ARCHITECTURE.md`,
  or any Architecture Decision — and PES-001's own "Architecture decides.
  Implementation follows." rule requires that decision to be made
  explicitly, at the Architecture stage, by Steven, not assumed inside an
  Implementation Decision meant only to resolve wiring.

---

## Consequences

- **Assembler responsibility.** Unaffected for now: `DefaultReasoningContextAssembler`
  gains no new responsibility, because it cannot yet construct a valid
  `WorldQuery` at all under the current contract. If the identified
  revision is later approved, the Assembler's own responsibility remains
  exactly what `MEMORY_SOURCE_CONTRACT_DESIGN.md`'s own precedent already
  established for query construction: express retrieval intent only
  (here, "no subject filter, just a bound"), never rank, score, reorder,
  or interpret what comes back.
- **World Model ownership.** Unaffected. `WorldModel`/`InMemoryWorldModel`
  remains the sole authoritative owner of world-model state and of the
  retrieval algorithm; nothing in this document proposes otherwise.
- **Runtime wiring.** Blocked. `ParkerRuntime` cannot construct a
  meaningful, always-valid `WorldQuery`-consuming integration until this
  question resolves — Unit 8's implementation stage (Contract Design
  Section 8's wiring shape) remains correctly specified but not yet
  executable end-to-end.
- **Test strategy.** Blocked in the same way. `MEMORY_SOURCE_CONTRACT_DESIGN.md`
  Section 9/10's own precedent (a `FakeWorldModelSource`, plus one
  real-`InMemoryWorldModel` integration test) remains the right shape once
  query construction is resolved; no test can yet be written that
  constructs a real, valid, unfiltered `WorldQuery` against the current
  contract.
- **Future subject-aware retrieval.** Left entirely open, and not
  foreclosed by this document. If a genuine, non-inventive structured
  subject identifier is ever introduced elsewhere in this architecture
  (for example, if a future Module or Tool begins tagging requests with a
  device or resource identifier that doubles as a world-model subject),
  `WorldQuery.subjectMatch` remaining a real (if now-nullable) field means
  a future Unit could supply it then, narrowing retrieval, without any
  further contract change.

---

## Outcome (post-implementation update — supersedes the "blocked" framing above)

**Everything above this section is preserved as the historical record of
this Unit's own investigation and is not rewritten.** Statements above
such as "implementation cannot proceed under the existing `WorldQuery`
contract" and "Implementation therefore remains blocked pending that
approval" describe the state **at the time of investigation**, not the
current state. They are left in place deliberately, not as an oversight —
this section exists precisely to mark where the situation changed and why,
rather than quietly rewriting the record to look as though the answer was
always known.

**The identified contract revision was separately governed, approved, and
implemented:**

- Governed through its own, narrowly-scoped review —
  `docs/architecture/WORLD_QUERY_OPTIONAL_SUBJECT_GOVERNANCE_REVIEW.md`,
  `docs/architecture/WORLD_QUERY_OPTIONAL_SUBJECT_CONTRACT_REVISION.md`,
  and `docs/implementation/WORLD_QUERY_OPTIONAL_SUBJECT_SCOPE_LOCK.md` —
  exactly the separate approval this document's own Decision section said
  would be required, since `WorldQuery` was an already-approved Sprint 4
  contract this narrow Implementation Decision was not itself authorised
  to revise.
- Approved, then implemented exactly as this document's own "smallest
  identified contract revision" described: `WorldQuery.subjectMatch`
  widened from `String` to `String? = null`; `InMemoryWorldModel.query`'s
  filter predicate changed to
  `(query.subjectMatch == null || belief.subject.contains(query.subjectMatch, ignoreCase = true))`,
  the identical shape `minimumConfidence` already used one line below it.
- Passed local Gradle verification (BUILD SUCCESSFUL), committed, and
  pushed as commit `eb25d64` ("feat: allow unfiltered world model
  queries").

**The original blocking condition is resolved.** `subjectMatch` is no
longer mandatory or non-nullable; a null/absent case is now representable
and treated as "no subject filter" throughout `InMemoryWorldModel.query`'s
own filtering logic — the exact gap Determination 2 and Determination 3,
above, found missing.

**The adopted construction:**

- `subjectMatch = null` — no subject filter.
- `maximumResults` — a caller-supplied, implementation-defined bound
  (policy, not architecture), mirroring `MEMORY_QUERY_MAXIMUM_RESULTS`'s
  own already-accepted treatment for Memory Source.
- No subject inference, no classification, and no retrieval policy in
  the Assembler — `null` is a literal absence of a filter, not a value
  computed, guessed, or derived from request text. Subject matching,
  confidence filtering, staleness exclusion, and result selection remain
  entirely `InMemoryWorldModel.query`'s own, unchanged, internal
  responsibility.

**Ordering — unchanged, not fixed by this revision.** `WorldModelSource`
returns beliefs in whatever order the World Model implementation supplies
them, and the Assembler preserves that order without reordering it. The
existing `WorldModel` contract does not guarantee deterministic ordering
(Determination 5, above, unchanged by this revision) — a future
implementation Unit's own tests must not assume a stable order unless the
backing implementation already guarantees one elsewhere.

**Scan cost — disclosed, not addressed.** `maximumResults` bounds the
*returned* result count; it does not bound the *internal scan cost* of
filtering every currently-held belief before that bound is applied. This
is a pre-existing characteristic of `InMemoryWorldModel.query`, not
introduced or worsened by this revision, and performance policy for a
larger World Model remains explicitly outside this Unit's own scope — no
optimisation work is proposed or required here.

**What this does, and does not, mean for Unit 8.** This resolves the one
blocker this document identified — nothing more. `WorldModelSource` itself,
the Assembler's fifth constructor dependency, and `ParkerRuntime`'s own
wiring step remain entirely unimplemented, exactly as disclosed in
`docs/implementation/WORLD_MODEL_SOURCE_INTEGRATION_SCOPE_LOCK.md`'s own
Status section.

---

## Required Document Refinements

Applied to `docs/architecture/WORLD_MODEL_SOURCE_CONTRACT_DESIGN.md`
Section 3 and `docs/implementation/WORLD_MODEL_SOURCE_INTEGRATION_SCOPE_LOCK.md`'s
own "Status" open-question note (see diffs applied alongside this
document): both are updated to record that this open question has now
been formally investigated, the specific blocking incompatibility found,
and that resolution depends on a separate, explicit approval of the
minimal `WorldQuery` revision identified above — not resolved by adopting
one of the four originally-listed options as-is. No other section of
either document is changed; no scope is expanded.
