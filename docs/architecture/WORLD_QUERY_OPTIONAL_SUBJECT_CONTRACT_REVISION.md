# WorldQuery Optional Subject — Contract Revision

## Status

**Implemented.** Following Steven's explicit go-ahead ("The following
documents are now approved... Proceed with implementation only within
the approved scope"), the revision described below has been implemented
exactly as shaped here: `src/interfaces/WorldModel.kt`'s `WorldQuery.subjectMatch`
is now `String? = null`, and `src/runtime/InMemoryWorldModel.kt`'s `query`
treats `null` as "no subject filter," exactly as Section 2 and Section 3,
below, illustrated. Tests were added to `tests/contracts/WorldModelContractsTest.kt`
and `tests/runtime/InMemoryWorldModelTest.kt`. **Build/test verification
could not be completed in this session's sandbox** (Gradle did not
execute meaningfully — every invocation, including a deliberately invalid
task name, returned exit code 0 with zero output); Steven's own local
`.\gradlew.bat test` run remains the authoritative verification, per
PES-001 Stage 7. See `docs/implementation/IMPLEMENTATION_HISTORY.md`,
"WorldQuery Optional Subject — Contract Revision Implementation," for the
full implementation record.

**Sprint 11, narrowly-scoped contract revision to an already-approved
Sprint 4, Track B contract.** Companion to
`docs/architecture/WORLD_QUERY_OPTIONAL_SUBJECT_GOVERNANCE_REVIEW.md`
(the compatibility and risk assessment this revision's field-level shape
implements) and
`docs/implementation/WORLD_QUERY_OPTIONAL_SUBJECT_SCOPE_LOCK.md` (the
binding Included/Excluded terms).

Revises `docs/architecture/WORLD_MODEL_CONTRACT_DESIGN.md` Section 4
(`WorldQuery`) in the one respect described below. No other section of
that document, and no other contract it approves
(`WorldBelief`, `WorldObservation`, `ObservationResult`,
`WorldModelUpdatePolicy`, `WorldModel`), is touched.

---

## 1. What Changes

**Field:** `WorldQuery.subjectMatch`

| | Before | After |
| --- | --- | --- |
| Declared type | `String` | `String?` |
| Default value | none (mandatory at every call site) | `null` |
| Validation | `require(subjectMatch.isNotBlank())` | `require(subjectMatch == null \|\| subjectMatch.isNotBlank())` |
| Meaning of a supplied, non-null value | substring-matched against `belief.subject` | unchanged |
| Meaning of absence/`null` | not representable | "no subject filter — match every belief regardless of subject" |

**No other field of `WorldQuery` changes.** `maximumResults: Int`
(mandatory, `>= 1`) and `minimumConfidence: Double? = null` (already
optional) are both unaffected, per the Governance Review's own explicit
confirmation.

**Corresponding change to `InMemoryWorldModel.query`'s filter predicate:**

| | Before | After |
| --- | --- | --- |
| Subject condition | `belief.subject.contains(query.subjectMatch, ignoreCase = true)` | `query.subjectMatch == null \|\| belief.subject.contains(query.subjectMatch, ignoreCase = true)` |

This is the identical `== null \|\| ...` short-circuit shape the same
predicate already uses, one line below, for `query.minimumConfidence` —
no new pattern is introduced to this class.

**No change to `WorldModel.current(subject: String): WorldBelief?`.** Per
this task's own explicit instruction to treat `current` as outside scope
"unless the review proves it must change for consistency" — it does not.
`current` answers a categorically different question ("what is the
belief for this one, specific, already-known subject?") than `query`
("what beliefs match this criterion, if any?"). `current`'s own
`subject` parameter has no analogous "no subject" case that would make
sense: a caller of `current` already has one particular subject in mind
by definition of calling that operation; there is no meaningful
"current(no subject)" question to ask, unlike `query`, whose entire
purpose is already to accept a *criterion* rather than a single known
key. Making `subjectMatch` optional on `WorldQuery` creates no
inconsistency with `current` remaining mandatory, because the two
operations answer different questions and always have — `WorldModel.kt`'s
own KDoc for `query` already documents it as the broader, criterion-based
operation, and `current` as the narrower, single-key one. No repository
evidence reviewed (Governance Review's own "Existing Behaviour" section)
identifies any caller, test, or document that treats the two operations'
parameter shapes as required to match.

---

## 2. Revised `WorldQuery` (implemented)

```kotlin
// Implemented, verbatim, in src/interfaces/WorldModel.kt.
data class WorldQuery(
    val subjectMatch: String? = null,
    val maximumResults: Int,
    val minimumConfidence: Double? = null,
) {
    init {
        require(subjectMatch == null || subjectMatch.isNotBlank()) {
            "WorldQuery.subjectMatch must not be blank if present"
        }
        require(maximumResults >= 1) {
            "WorldQuery.maximumResults must be at least 1, was $maximumResults"
        }
        require(minimumConfidence == null || minimumConfidence in 0.0..1.0) {
            "WorldQuery.minimumConfidence must be between 0.0 and 1.0 if present, was $minimumConfidence"
        }
    }
}
```

Field order is preserved (`subjectMatch`, `maximumResults`,
`minimumConfidence`) so every existing named-argument call site remains
valid with no reordering. `maximumResults` remains the only field without
a default — deliberately: per `WORLD_MODEL_CONTRACT_DESIGN.md` Section
4's own reasoning ("`query` must not imply 'return every belief
matching'... without a caller-stated bound, nothing in this contract
prevents an unbounded result set"), a caller must always state a bound,
even when it states no subject filter. This is not relaxed by this
revision.

---

## 3. Revised `InMemoryWorldModel.query` (implemented)

```kotlin
// Implemented, verbatim, in src/runtime/InMemoryWorldModel.kt.
override suspend fun query(query: WorldQuery): List<WorldBelief> = mutex.withLock {
    beliefs.values
        .filter { belief ->
            (query.subjectMatch == null || belief.subject.contains(query.subjectMatch, ignoreCase = true)) &&
                (query.minimumConfidence == null || belief.confidence >= query.minimumConfidence) &&
                updatePolicy.isStillCurrent(belief)
        }
        .take(query.maximumResults)
}
```

One line changed, mirroring the line below it exactly in shape. No
change to `.take(query.maximumResults)`, to `updatePolicy.isStillCurrent`
consultation, to lock acquisition, or to any other part of this method.

---

## 4. Why This Is a Revision, Not Merely a Bug Fix

`WORLD_MODEL_CONTRACT_DESIGN.md` Section 4's own original text describes
`subjectMatch` as "at minimum, a description of which subject or subjects
a caller is asking about" — framed as something every caller has, not as
a criterion a caller might sometimes lack entirely. The original Contract
Design (Unit B2) did not consider, and does not rule out, an "I have no
subject in mind, just show me what's current" case; it simply did not
anticipate one. This revision is therefore a genuine, if narrow,
extension of that document's own stated intent — not a correction of a
defect in it — which is exactly why this proposal was routed through its
own governance review and Scope Lock rather than treated as an obvious
fix requiring no separate process.

---

## 5. Traceability

- Authorised, once accepted, as a targeted amendment to
  `docs/architecture/WORLD_MODEL_CONTRACT_DESIGN.md` Section 4 — that
  document's own text should be updated to reflect this shape only if and
  when this revision is separately approved for implementation, per the
  Scope Lock's own Included/Excluded terms.
- Unblocks, but does not itself complete, Sprint 11 Unit 8 — see
  `docs/architecture/WORLD_MODEL_SOURCE_QUERY_CONSTRUCTION_DECISION.md`
  for the original blocking finding this revision resolves, and
  `docs/architecture/WORLD_MODEL_SOURCE_CONTRACT_DESIGN.md` Section 3 for
  where the resulting query-construction decision (once this revision is
  implemented) would still need to be finalised — "`subjectMatch = null`,
  caller supplies `maximumResults`, World Model owns selection and
  ordering, Assembler preserves returned order" — itself a separate,
  still-future Unit 8 implementation step, not part of this document's
  own scope.
