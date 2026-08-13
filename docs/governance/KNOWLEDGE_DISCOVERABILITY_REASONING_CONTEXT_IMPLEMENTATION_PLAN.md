**Status:** Implementation Plan only. Planning and sequencing document. No Kotlin is implemented,
proposed as a diff, or changed by this document. No test is added, removed, or changed by this
document. It implements nothing and authorizes no work outside the accepted Scope Lock. It does not
redesign, reinterpret, weaken, or reopen any Contract Design or Scope Lock decision -- every frozen
value below is transcribed, not chosen, by this document. Gap #54 remains complete and is not
reopened. This document makes no claim of successful implementation, verification, conversational
recall, restart durability, or programme closure.

# Knowledge Discoverability and Governed Retrieval into Reasoning Context — Implementation Plan

---

## 1. Status and Authority

Governing inputs, read completely and cross-checked against current production source:
`docs/governance/KNOWLEDGE_DISCOVERABILITY_REASONING_CONTEXT_PLANNING_REVIEW.md` ("the Planning
Review"), `docs/governance/KNOWLEDGE_DISCOVERABILITY_REASONING_CONTEXT_BOUNDARY_REVIEW.md` ("the
Boundary Review"), `docs/governance/KNOWLEDGE_DISCOVERABILITY_REASONING_CONTEXT_CONTRACT_DESIGN.md`
("the Contract Design"), and `docs/governance/KNOWLEDGE_DISCOVERABILITY_REASONING_CONTEXT_SCOPE_LOCK.md`
("the Scope Lock"), each as currently accepted. Production and test source inspected fresh, in full
or in every relevant section: `src/composition/ParkerRuntime.kt`, `src/runtime/DefaultReasoningContextAssembler.kt`,
`src/runtime/DefaultKnowledgeRetrieval.kt`, `src/runtime/DefaultKnowledgeCandidateEvaluator.kt`,
`src/composition/PermissionFilteredMemoryRetrieval.kt`, `src/interfaces/KnowledgeStore.kt`,
`tests/runtime/DefaultReasoningContextAssemblerTest.kt`, `tests/composition/ParkerRuntimeKnowledgeRetrievalCompositionTest.kt`,
`tests/composition/ParkerRuntimeReasoningContextIntegrationTest.kt`, and
`tests/runtime/DefaultKnowledgeCandidateEvaluatorTest.kt` (for its own established `MemoryRetrieval`
test-double convention).

This Implementation Plan sequences the independently reviewed implementation units the Scope Lock's
own Section 15 requires. It resolves the Scope Lock's own provisional `tests/composition/` allowance
(Section 3, below) as its first obligation, before defining any implementation unit. It authorizes no
file, contract, capability, or alternative beyond what the Scope Lock already locked. Where this
document names an exact filename, test name, or line citation, that citation was verified directly
against current repository source at the commit this plan is drafted from; a discovered drift at
implementation time is a stop condition (Section 19, below), not implementation discretion to resolve
silently.

---

## 2. Purpose and Scope of This Implementation Plan

This document plans and sequences only. It fixes: the resolved composition-test filenames (Section
3); the complete, closed file boundary (Section 4); five implementation units, each independently
reviewable, with exact files, frozen contracts, exclusions, required tests, completion evidence, and
stop conditions (Sections 6-10); the composition cutover guarantee (Section 11); the carried-forward
least-authority, deterministic-matching-and-rendering, timing, and non-claim boundaries (Sections
12-13, 16-17); the required genuine end-to-end proof and denial/failure proof matrix (Sections 14-
15, restated here for the sequencing this document owns); every Scope Lock stop condition, carried
forward unweakened, plus the sequencing-specific stop conditions this document's own review discipline
requires (Section 19); the final programme evidence sequence (Section 20); and a full unit-to-
governing-section traceability table (Section 21).

It does not select an alternative to any Contract Design or Scope Lock decision. It does not begin
implementation. It does not create a Closure Determination. It does not claim the programme is
verified, complete, or safe to close.

---

## 3. Resolved: Composition-Test File Names

The Scope Lock's own Section 9 leaves `tests/composition/` as "provisionally, only the minimum
existing/new composition test files the Implementation Plan itself names," and its own Section 12
requires this resolution "before any implementation unit may begin." Resolved by direct inspection of
`tests/composition/` (every existing file in the directory listed; the one existing file this
resolution extends read in full) to exactly two files, one new and one extended -- no directory-level
allowance remains. `tests/composition/ParkerRuntimeReasoningContextIntegrationTest.kt` (item 2, below)
is an existing file and was read in full. `tests/composition/ParkerRuntimeReasoningKnowledgeSourceCompositionTest.kt`
(item 1, below) is a proposed new file that does not yet exist -- it cannot be, and was not, read; its
filename and responsibility were instead resolved by inspecting the existing composition-test directory
listing and structure (mirroring `ParkerRuntimeKnowledgeRetrievalCompositionTest.kt`'s own established,
read-in-full precedent, below) against the governing requirements this section and Section 9 (below)
fix:

1. **`tests/composition/ParkerRuntimeReasoningKnowledgeSourceCompositionTest.kt` (new file, created
   exclusively by revised Unit 4, Section 9, below, only after revised Unit 3's production composition
   is already complete and independently reviewed).** This file verifies Unit 3's already-accepted
   production composition; it constructs no production wiring of its own. Mirrors
   `tests/composition/ParkerRuntimeKnowledgeRetrievalCompositionTest.kt`'s own established structure
   (Unit 9.6's own precedent for a dedicated, class-scoped composition test) for the already-composed
   `DefaultReasoningKnowledgeSource` graph Unit 3 wires into production: construction proof, shared-instance proof (the same
   `knowledgeItemPersistence` and `permissionEngine` `DefaultKnowledgeRetrieval` already uses), real
   permission-gating proof at both granularities, retrieval-through-the-composed-graph proof,
   lifecycle/staleness/ordering proofs mirroring Unit 9.6's own, Purpose registration-and-active proof,
   the least-authority Document-denial proof (Section 12, below), Evidence Intelligence non-widening
   proof, and the "no legacy `KnowledgeSource` path remains" structural proof (a reflection-based field-
   type check on `DefaultReasoningContextAssembler`, mirroring
   `ParkerRuntimeKnowledgeRetrievalCompositionTest.kt` line 407's own established
   "no ... dependency is reachable" style, applied to `parker.core.interfaces.KnowledgeSource` this
   time).
2. **`tests/composition/ParkerRuntimeReasoningContextIntegrationTest.kt` (extended, existing file).**
   Its own existing test at line 274, `` `KnowledgeSource is wired into the real ParkerRuntime and
   renders no Memory entries, since nothing in this Unit's own scope creates memories` ``, is renamed
   and its inline comment corrected: after cutover, `InMemoryKnowledgeStore` is no longer constructed
   in production at all (its own current rationale, "the `InMemoryKnowledgeStore` ParkerRuntime
   constructs is always empty in production," becomes false, not merely stale), and the dependency is
   `ReasoningKnowledgeSource`, not `KnowledgeSource`. The renamed test keeps its own assertion (no
   `"Memory:"` substring in the real prompt when nothing was ever promoted) -- still true and still the
   correct proof for this exact scenario -- under corrected wording:
   `` `ReasoningKnowledgeSource is wired into the real ParkerRuntime and renders no Memory entries when nothing has been promoted` ``.
   This same file also gains the one new test carrying the required genuine end-to-end proof (Section
   14, restating the obligation; assigned to Unit 5, Section 10, below), since it already holds the
   exact `ParkerRuntime` + `StubModelServer` + real-prompt-
   inspection infrastructure that proof requires, and its own class KDoc already documents this file's
   role as the place every guarantee "hold[s] against the real, running `ParkerRuntime`." This file
   belongs exclusively to revised Unit 5 (Section 10, below), unchanged from this document's prior
   revision.

**Confirmed unaffected, requiring no change and not part of this Implementation Plan's own scope:**
`tests/composition/ParkerRuntimeKnowledgeRetrievalCompositionTest.kt`. Read in full: its own only
reference to a Knowledge-shaped negative proof, line 407's `` `no Knowledge Retrieval dependency is
reachable from the conversation coordinator chain` ``, checks the field types of
`ConversationReplyCoordinator`, `CommunicationConversationCoordinator`, and
`ConversationTurnReasoningCoordinator` against exactly two type names,
`parker.core.interfaces.KnowledgeRetrieval` and `parker.core.runtime.DefaultKnowledgeRetrieval` -- both
untouched by this design (Contract Design Decision Register item 3; Scope Lock Section 12's own stop
condition -- Scope Lock's Section 12, not this Plan's own Section 12, Least-Authority Boundary,
below), and neither of the three checked classes is `DefaultReasoningContextAssembler`. This test's
own assertion remains true, unconditionally, after every unit below -- confirmed by direct inspection,
not assumed.

---

## 4. Locked File Boundary — Confirmed Complete

Verified by direct inspection (every construction site of `DefaultReasoningContextAssembler(` in the
whole repository is one of the three files already in this list; no other file constructs it) against
Scope Lock Section 9. No file beyond this list is authorized; a discovered need for another file is a
stop condition (Section 19), not implementation discretion.

**Production (exactly four):**

```text
src/interfaces/KnowledgeStore.kt                      (additive only)
src/runtime/DefaultReasoningKnowledgeSource.kt        (new file)
src/runtime/DefaultReasoningContextAssembler.kt       (constructor + rendering change)
src/composition/ParkerRuntime.kt                      (composition cutover)
```

**Tests (exactly four, two named by the Scope Lock, two resolved in Section 3, above):**

```text
tests/runtime/DefaultReasoningKnowledgeSourceTest.kt                    (new file)
tests/runtime/DefaultReasoningContextAssemblerTest.kt                   (extended)
tests/composition/ParkerRuntimeReasoningKnowledgeSourceCompositionTest.kt  (new file)
tests/composition/ParkerRuntimeReasoningContextIntegrationTest.kt          (extended)
```

Eight files, total, across the entire programme. No other production or test file may be created or
modified by any unit below.

---

## 5. Implementation Units — Overview

| # | Unit | Files | Depends on |
|---|---|---|---|
| 1 | Knowledge Memory Contract Additions | `src/interfaces/KnowledgeStore.kt` | none |
| 2 | `DefaultReasoningKnowledgeSource` Implementation | `src/runtime/DefaultReasoningKnowledgeSource.kt` (new); `tests/runtime/DefaultReasoningKnowledgeSourceTest.kt` (new) | Unit 1 |
| 3 | Atomic Assembler and Production Composition Cutover | `src/runtime/DefaultReasoningContextAssembler.kt`; `tests/runtime/DefaultReasoningContextAssemblerTest.kt`; `src/composition/ParkerRuntime.kt` | Units 1, 2 |
| 4 | Composition Verification (test-only) | `tests/composition/ParkerRuntimeReasoningKnowledgeSourceCompositionTest.kt` (new) | Unit 3 |
| 5 | Genuine End-to-End Proof (test-only) | `tests/composition/ParkerRuntimeReasoningContextIntegrationTest.kt` | Unit 4 |

**Correction note.** This table revises this document's own prior revision, which split the
assembler's constructor-signature change (originally Unit 3) from its sole production caller's update
(originally Unit 4) across a unit boundary. `DefaultReasoningContextAssembler` has exactly one
constructor, and `src/composition/ParkerRuntime.kt` is its only production call site -- splitting the
two meant the repository could not compile, and Unit 3 could not reach "full local Gradle build
passing," between the two units' own reviews. Revised Unit 3 now performs both changes together,
atomically, in one reviewable boundary; revised Unit 4 becomes strictly test-only, verifying Unit 3's
already-accepted production composition without touching production itself.

Strictly linear: each unit's own Unit Completion Review and Independent Constitutional Review must
both be accepted before the next unit may begin (Scope Lock Section 13; mirroring Gap #54's own
established discipline). No unit consumes a contract an earlier unit has not yet implemented. No unit
leaves two production knowledge feeds active at once: the legacy `memorySource`/`InMemoryKnowledgeStore`
binding is retired in the exact same unit (Unit 3) that wires in the new `ReasoningKnowledgeSource`
binding, and that same unit also changes the assembler's own constructor signature to require it --
there is no commit, and no unit boundary, at which both or neither knowledge feed exist in production,
and no commit at which the assembler's declared dependency type and its production caller's supplied
value disagree (Section 11, below). Unit 4 performs no production change of any kind. If Unit 4's own
tests reveal Unit 3's composition to be incomplete or incorrect, that is a Unit 3 review failure and a
stop condition (Section 19, below) -- never authority for Unit 4 to edit production.

---

## 6. Unit 1 — Knowledge Memory Contract Additions

**Files this unit may create or modify:** `src/interfaces/KnowledgeStore.kt` only. No test file is
created or modified by this unit.

**Frozen contract implemented (Contract Design Section 4, Section 8; Scope Lock Section 4), verbatim:**

```kotlin
// additive, appended after the existing KnowledgeRetrieval interface declaration
interface ReasoningKnowledgeSource {
    suspend fun recall(requestingPrincipalId: PrincipalId, query: KnowledgeRetrievalQuery): List<SafeKnowledgeResultEntry>
}

data class SafeKnowledgeResultEntry(
    val content: String,
    val evidentialState: EvidentialState,
    val status: KnowledgeItemStatus,
    val staleness: StalenessDisclosure,
)
```

No `init` block on `SafeKnowledgeResultEntry` -- no validation beyond ordinary type safety is
authorized (mirroring `KnowledgeItem`'s own identical, already-frozen precedent).

**Explicit exclusions:** no change to any existing declaration in this file
(`KnowledgeId`, `KnowledgeCategory`, `CandidateKnowledge`, `KnowledgeRecord`,
`KnowledgePromotionDecision`, `KnowledgePromotionPolicy`, `KnowledgeQuery`, `KnowledgeStore`,
`KnowledgeItemStatus`, `KnowledgeLifecycleEvent`, `KnowledgePromotion`, `KnowledgeRetirement`,
`KnowledgeRestoration`, `KnowledgeItem`, `KnowledgeReference`, `KnowledgeCandidate`,
`KnowledgeCandidateEvaluation`, `KnowledgeCandidateEvaluator`, `KnowledgeRevisionEvaluation`,
`KnowledgeRevisionEvaluator`, `KnowledgeRetirementEvaluation`, `KnowledgeRetirementEvaluator`,
`KnowledgeSubmissionDisposition`, `KnowledgeSubmission`, `KnowledgeRetrievalQuery`,
`KnowledgeResultEntry`, `StalenessDisclosure`, `KnowledgeRetrievalResult`,
`KnowledgeRetrievalDisposition`, `KnowledgeRetrieval`, `KnowledgeSource`) -- every one remains
byte-for-byte unchanged (Scope Lock Section 9's own explicit clarification). No new file. No
implementation of `ReasoningKnowledgeSource` in this unit -- that is Unit 2's own, separate
responsibility.

**Required tests:** none, beyond the existing full test suite continuing to compile and pass
unchanged -- these two types carry no behaviour and no validation logic of their own, so no dedicated
test is required or authorized. Completion evidence is a full-file diff review proving strictly
additive change, plus a full local Gradle compile-and-test pass proving no existing declaration was
altered.

**Completion evidence:** full-file diff limited to the two additive declarations above; local Gradle
build and full test suite pass unchanged; no other file touched.

**Stop conditions:** halt if the diff touches any line outside the two additive declarations; halt if
either type requires a field, method, or validation rule beyond what is frozen above.

**Reviews required before Unit 2 may begin:** a Unit Completion Review and an Independent
Constitutional Review, both accepted, confirming the diff is strictly additive and matches Section 6
of this document exactly.

---

## 7. Unit 2 — `DefaultReasoningKnowledgeSource` Implementation

**Files this unit may create or modify:** `src/runtime/DefaultReasoningKnowledgeSource.kt` (new);
`tests/runtime/DefaultReasoningKnowledgeSourceTest.kt` (new). No other file.

**Frozen contract implemented:** the exact ten-step retrieval algorithm, authorized-partial semantics,
and step-placement justification (Contract Design Section 4; Scope Lock Section 5); the exact content
normalization, matching, Entity-content construction, and record-status gate (Contract Design Section
5; Scope Lock Section 6, normalization/matching portion only -- rendering/escaping is Unit 3's own
responsibility, not this unit's); the exact constructor signature and companion constants (Contract
Design Section 4; Scope Lock Section 4):

```kotlin
internal class DefaultReasoningKnowledgeSource(
    private val persistence: KnowledgeItemPersistence,
    private val permissionEngine: PermissionEngine,
    private val evidenceMemoryRetrieval: MemoryRetrieval,
    private val authorizationPurpose: AuthorizationPurposeId,
    private val clock: Clock = Clock.systemUTC(),
) : ReasoningKnowledgeSource
```

with companion constants `REASONING_CONTEXT_RETRIEVAL_RESOURCE_ID` (reusing
`DefaultKnowledgeRetrieval.KNOWLEDGE_RETRIEVAL_RESOURCE_ID` unchanged -- no new `Resource`) and
`RETRIEVE_FOR_REASONING_CONTEXT_ACTION_NAME = "knowledge.retrieve_for_reasoning_context"`, owned by
this new file itself, mirroring `DefaultKnowledgeRetrieval`'s own identical companion-constant
ownership pattern.

**Explicit exclusions:** no change to `DefaultKnowledgeRetrieval.kt`, `KnowledgeItemPersistence.kt`,
`PermissionFilteredMemoryRetrieval.kt`, or any Memory Core file. No `ResourceType.DOCUMENT` rule, no
`memory.retrieve_document` proposed action, anywhere in this class. No call to
`MemoryRetrieval.getDocument`, `getRelationship`, `findEntities`, `findDocuments`,
`traverseRelationships`, `findByTimeRange`, `findByMetadata`, or `findByProvenance` -- only
`getAssertion` and `getEntity` are ever invoked. No `Resource` registration and no `AuthorizationPurposeRegistry`
registration in this unit -- both are Unit 3's own, separate, composition-time responsibility; this
class only accepts an already-resolved `authorizationPurpose` value.

**Required tests, exact.** Every bullet below is a distinct, mandatory test -- none is an example, an
alternative to another bullet, or interchangeable with it. No "or" in any bullet below permits one
proof to substitute for another; where two conditions are named together, both must be tested,
separately. **Together, the denial/missing/unsupported-reference bullets below (Item-level denial;
Denied Assertion reference; Denied Entity reference; Missing Assertion reference; Missing Entity
reference; Unsupported reference kinds; Authorized-partial result; Record-status tests) fully discharge
Contract Design Section 13's own Invariant 7 ("missing, denied, deleted, or non-`ACTIVE`-status evidence
is handled deterministically and honestly... never silently fabricated, never silently omitted without
a disclosed, uniform non-disclosure rule") and Scope Lock Section 11's own "Denied, missing, deleted,
and unsupported-reference-kind evidence (silent exclusion, no exception)" and "Item-level denial
(silent exclusion)" requirements -- no later unit re-proves any of them.**

- *Positive:* an `ACTIVE` `Assertion` whose `statement` contains the query's `relevance`
  (case-insensitive) is returned, content equal to `statement`, normalized; an `ACTIVE` `Entity` whose
  `primaryLabel` or an alias contains `relevance` is returned, content equal to
  `listOf(primaryLabel).plus(aliases).joinToString(" | ")`, normalized. This is also the positive half
  of the record-status pairing, below.
- *Unsupported reference kinds:* a `KnowledgeItem` whose `evidenceReference` is `ToDocument` or
  `ToRelationship` never matches any query, and a `RecordingMemoryRetrieval` spy (mirroring
  `DefaultKnowledgeCandidateEvaluatorTest.kt`'s own established `RecordingMemoryRetrieval` pattern)
  proves `getDocument`, `getRelationship`, and every other `MemoryRetrieval` method beyond
  `getEntity`/`getAssertion` is never invoked, even when candidates of those two kinds exist.
- *Authorization, act-level:* act-level denial (a `FakePermissionEngine` configured to deny) returns
  `emptyList()` with a spy `KnowledgeItemPersistence` proving zero `findAll()` invocations.
- *Item-level denial (`DefaultReasoningKnowledgeSource`'s own `KnowledgeItem`-visibility gate, step 6 of
  the algorithm -- distinct from evidence-resolution denial, below), exact, using a controllable
  `FakePermissionEngine` or equivalent established test double (mirroring `DefaultKnowledgeRetrieval`'s
  own identical, already-governed testing precedent for its own structurally identical item-level
  gate):* the test double allows the act-level gate; approves one candidate's item-level gate and
  denies a second candidate's item-level gate for the same query (two candidates, one authorized, one
  denied, distinguished only by the test double's own controllable per-call behaviour -- never by any
  field the real `DefaultPermissionPolicy` itself consults); asserts only the approved candidate's
  entry is returned; asserts the denied candidate is silently excluded -- absent from the result, with
  no exception thrown and no denial detail, reason, or marker of any kind observable in the returned
  `List<SafeKnowledgeResultEntry>`. **This test proves `DefaultReasoningKnowledgeSource`'s own
  item-level gating and silent-exclusion behaviour -- it does not, and cannot, prove that the real,
  composed `DefaultPermissionPolicy` itself can produce a mixed approved/denied outcome for two
  candidates sharing an identical (action, resourceType, Purpose, proposedAction) tuple, since no
  `PermissionPolicyRule` field, and no field `PermissionFilteredMemoryRetrieval.isApproved` consults,
  ever varies by candidate, resource, or evidence identity (confirmed directly against
  `DefaultPermissionPolicy.ruleOutcomeFor` and `PermissionFilteredMemoryRetrieval`'s own `isApproved`
  calls -- both consult only principal, verb, and Purpose).** The real, composed policy's own
  behaviour -- Purpose-level and act-level denial, and the Document-denial guard -- is independently
  proven against the genuine `DefaultPermissionPolicy` in Unit 4 (Section 9, below); this test and that
  one prove two distinct, non-overlapping claims, and neither substitutes for the other.
- *Denied Assertion reference (evidence-resolution-gate denial, step 7 of the algorithm -- distinct
  from item-level denial, above), exact, using a controllable `evidenceMemoryRetrieval` test double:*
  the act-level gate is allowed; a genuine candidate `KnowledgeItem` references an existing `Assertion`;
  the double's own `getAssertion` is configured to deny that specific `AssertionId` (returns `null` for
  it, representing the denial `PermissionFilteredMemoryRetrieval` itself performs in production); the
  candidate is proven silently excluded from the result; no exception is thrown; no denial detail,
  identifier, count, or protected content of any kind is observable in the returned
  `List<SafeKnowledgeResultEntry>`; the denied delegate's own (real, non-null) `Assertion` value is
  proven never returned or leaked into any entry.
- *Denied Entity reference:* identical structure to the Denied Assertion reference test, above, for a
  candidate referencing an existing `Entity` and a denied `getEntity` call.
- *Missing Assertion reference:* both act-level and item-level authorization are allowed; a candidate
  references a nonexistent or deleted `AssertionId`; the double's own `getAssertion` genuinely returns
  `null` for that ID (absence, not denial); silent exclusion is proven with no exception; no fabricated
  content or result entry is produced for that candidate.
- *Missing Entity reference:* identical structure to the Missing Assertion reference test, above, for a
  nonexistent or deleted `EntityId` and a genuinely absent `getEntity` result.
- *Record-status tests, exact and separate:* an `ACTIVE`-status resolved `Assertion`/`Entity` is
  included (the *Positive* test, above, is this pairing's own positive half); each of `DISPUTED`,
  `SUPERSEDED`, `ARCHIVED`, and `DELETED` resolved status is tested separately, each producing silent
  exclusion with no exception. **This status-based exclusion is never described, asserted, or treated
  as a form of permission denial -- it is a distinct, Memory-Core-record-status gate (Contract Design
  Section 5's own binding decision), entirely independent of the item-level and evidence-resolution
  denial tests above, and neither may substitute for the other.**
- *Ordering:* `KnowledgeItemPersistence.findAll`'s own insertion order is preserved through every
  filter stage, proven with a multi-item fixture.
- *Rendering (normalization only, not escaping):* CRLF and lone CR both become LF in `content`; a
  non-CR/LF Unicode character, including outside the Basic Multilingual Plane, is preserved unchanged;
  no trimming or whitespace collapse occurs; case-insensitive matching is verified independent of
  default JVM `Locale` (e.g. under `Locale.forLanguageTag("tr")`).
- *Generic-basis false-match regression, exact:* a candidate `KnowledgeItem` whose own generic
  promotion-basis text (`KnowledgeItem.history`'s own basis string) contains the query's `relevance`,
  while its authorized, resolved `Assertion`/`Entity` content does not contain `relevance` under the
  frozen normalization/matching rules (Section 13, below) -- proves the candidate is excluded,
  demonstrating directly that matching is performed exclusively against resolved, dereferenced content,
  never against `KnowledgeItem`'s own generic basis text -- the exact defect Planning Review Section 5
  and Contract Design Section 5 identify in `DefaultKnowledgeRetrieval.matches()`'s own basis-text
  matching is proven not reintroduced by this design. This test changes nothing in the frozen matching
  algorithm itself (Section 13, below) -- it proves that algorithm's own already-fixed behaviour, never
  an alternative one.
- *Failure:* a genuine `persistence`/`permissionEngine`/`evidenceMemoryRetrieval` exception propagates
  unchanged, uncaught.
- *Bounds:* `maximumResults` truncates only after every authorization/relevance filter, proven with
  more matching, authorized candidates than the bound.
- *Lifecycle:* `RETIRED` excluded by default; included only with `includeRetired = true`.
- *Authorized-partial result, exact and unambiguous:* candidate A references an authorized, existing,
  `ACTIVE` `Assertion` or `Entity` and resolves and matches successfully; candidate B references a
  denied `Assertion` or `Entity`, using the same controllable evidence-resolution double the Denied
  Assertion/Entity reference tests, above, use, configured to deny specifically for this test; only
  candidate A's entry appears in the result; insertion order among surviving entries is preserved; the
  result is a plain, successful `List` containing no denial detail of any kind. **Candidate B in this
  test must be a denied-evidence candidate specifically -- it may not be substituted with a
  non-`ACTIVE`-status candidate, a missing-evidence candidate, an unsupported-reference-kind candidate,
  or a candidate that simply fails to match; each of those is already its own separate, required test,
  above, and none may stand in for this one.**

**Completion evidence:** every test above passing, including each distinct denial, missing-reference,
unsupported-reference, record-status, authorized-partial, and generic-basis-false-match test named
above as its own mandatory test, not an example or an alternative to any other; full local Gradle build
passing; no file outside this unit's own two touched.

**Stop conditions:** halt if any test requires a `getDocument`/`getRelationship` call to pass; halt if
the algorithm's own step order deviates from Contract Design Section 4's ten steps; halt if this class
is found to require a dependency beyond the five frozen constructor parameters; halt if any two of the
distinct tests above (Item-level denial; Denied Assertion reference; Denied Entity reference; Missing
Assertion reference; Missing Entity reference; Unsupported reference kinds; Authorized-partial result;
Record-status tests; Generic-basis false-match regression) are collapsed, conflated, or omitted in favor
of one another -- each proves a distinct, required behaviour, and no "or" may substitute one for
another.

**Reviews required before Unit 3 may begin:** a Unit Completion Review and an Independent
Constitutional Review, both accepted, confirming every required test above exists as its own distinct
test, passes, and that no excluded call or file was touched, and confirming that the denial/missing/
unsupported-reference/record-status/authorized-partial test set together fully discharges Contract
Design Invariant 7 and Scope Lock Section 11's referenced-evidence guarantees.

---

## 8. Unit 3 — Atomic Assembler and Production Composition Cutover

**Files this unit may create or modify:** `src/runtime/DefaultReasoningContextAssembler.kt`;
`tests/runtime/DefaultReasoningContextAssemblerTest.kt`; `src/composition/ParkerRuntime.kt`. No other
file. Unit 3's own tests may modify only `tests/runtime/DefaultReasoningContextAssemblerTest.kt` -- no
composition test file is created or modified by this unit.

**Why this unit now spans both the assembler and the composition root.** This document's own prior
revision split the assembler's constructor-signature change into a separate, earlier unit from its
sole production caller's update, which an independent review correctly rejected as a blocking defect:
`DefaultReasoningContextAssembler` (`src/runtime/DefaultReasoningContextAssembler.kt` line 233) has
exactly one constructor, no overload, and `src/composition/ParkerRuntime.kt` line 409 is its only
production call site. Changing the constructor's fourth-parameter declared type from `KnowledgeSource`
to `ReasoningKnowledgeSource` without updating that one call site in the same reviewable change leaves
`ParkerRuntime.kt` unable to compile -- making "full local Gradle build passing" (required completion
evidence for every unit in this programme) unattainable for a unit confined to the assembler file
alone. This unit therefore performs the assembler contract change and the full production composition
cutover together, atomically, in one commit and one review boundary, so no intermediate, non-compiling
state is ever committed.

**Frozen contract implemented -- assembler side:** the constructor signature change (`memorySource:
KnowledgeSource` replaced by `knowledgeSource: ReasoningKnowledgeSource`, Contract Design Section 12);
the exact rendering and escaping contract, fixed field order, and `renderKnowledgeEntry` format string
(Contract Design Section 8; Scope Lock Section 6), verbatim:

```kotlin
private fun escapeForPrompt(normalized: String): String = buildString {
    for (c in normalized) {
        when {
            c == '\\' -> append("\\\\")
            c == '\n' -> append("\\n")
            c == '\r' -> append("\\r")
            c == '\t' -> append("\\t")
            c.code <= 0x1F || c.code == 0x7F || c.code in 0x80..0x9F || c.code == 0x2028 || c.code == 0x2029 ->
                append("\\u" + c.code.toString(16).uppercase().padStart(4, '0'))
            else -> append(c)
        }
    }
}

private fun renderKnowledgeEntry(entry: SafeKnowledgeResultEntry): String =
    "Memory: ${escapeForPrompt(entry.content)} (evidentialState=${entry.evidentialState.name}, " +
        "status=${entry.status.name}, staleness=${entry.staleness.name})"
```

The existing `memoryQuery`/`memorySource.recall(memoryQuery).forEach { ... }` block (currently lines
296-312 of this file) is replaced by an equivalent `KnowledgeRetrievalQuery` construction from the same
fields (`relevance = message.text`, `correlationId = message.correlationId.value`,
`maximumResults = MEMORY_QUERY_MAXIMUM_RESULTS`, `includeRetired` left at its own `false` default) and
a call to `knowledgeSource.recall(message.senderPrincipalId, knowledgeRetrievalQuery).forEach { entry
-> entries += renderKnowledgeEntry(entry) }`.

**Frozen contract implemented -- production composition side, exact, against current line
citations:**

- **Purpose registration.** Inside the existing `stage("Memory retrieval Authorization Purpose
  registration")` block (current lines 365-368), add a third
  `authorizationPurposeRegistry.register(REASONING_CONTEXT_RETRIEVAL_PURPOSE)` call, alongside the two
  existing registrations, and add the constant
  `val REASONING_CONTEXT_RETRIEVAL_PURPOSE = AuthorizationPurposeId("knowledge-memory.reasoning-context-retrieval")`
  to the private companion object, alongside `KNOWLEDGE_CANDIDATE_EVALUATION_PURPOSE` and
  `EVIDENCE_INTELLIGENCE_INPUT_RESOLUTION_PURPOSE` (current lines 1436-1439).
- **Exactly three new `PermissionPolicyRule` entries**, added to the existing rule list (current lines
  611-675, after the last existing rule at line 675), exactly as Contract Design Section 7 and Scope
  Lock Section 4 freeze them, verbatim:

  ```kotlin
  PermissionPolicyRule(
      action = PermissionAction.READ, resourceType = ResourceType.MEMORY,
      outcome = PermissionDecisionOutcome.DENIED, level = PermissionLevel.AUTOMATIC,
      proposedAction = DefaultReasoningKnowledgeSource.RETRIEVE_FOR_REASONING_CONTEXT_ACTION_NAME,
  )
  PermissionPolicyRule(
      action = PermissionAction.READ, resourceType = ResourceType.MEMORY,
      outcome = PermissionDecisionOutcome.APPROVED, level = PermissionLevel.AUTOMATIC,
      authorizationPurpose = REASONING_CONTEXT_RETRIEVAL_PURPOSE,
      proposedAction = DefaultReasoningKnowledgeSource.RETRIEVE_FOR_REASONING_CONTEXT_ACTION_NAME,
  )
  PermissionPolicyRule(
      action = PermissionAction.READ, resourceType = ResourceType.MEMORY,
      outcome = PermissionDecisionOutcome.APPROVED, level = PermissionLevel.AUTOMATIC,
      authorizationPurpose = REASONING_CONTEXT_RETRIEVAL_PURPOSE,
      proposedAction = PermissionFilteredMemoryRetrieval.RETRIEVE_ACTION_NAME,
  )
  ```

  **No fourth rule** -- no `ResourceType.DOCUMENT`/`RETRIEVE_DOCUMENT_ACTION_NAME` entry for this
  Purpose, anywhere. The pre-existing Gap #54 Unit 2 verb-only `DENIED` guard for
  `memory.retrieve_document` (current lines 630-636) is not touched and remains the applicable,
  fail-closed default for this Purpose -- the existing Document denial is preserved unmodified, not
  re-implemented.
- **Resource/vocabulary registration.** Inside the existing `stage("Knowledge Retrieval resource
  registration")`/`stage("Knowledge Retrieval action vocabulary registration")` blocks (current lines
  991-1014), no new `Resource` (`DefaultKnowledgeRetrieval.KNOWLEDGE_RETRIEVAL_RESOURCE_ID` is reused
  unchanged); one new `ActionVocabularyEntry`
  (`DefaultReasoningKnowledgeSource.RETRIEVE_FOR_REASONING_CONTEXT_ACTION_NAME` -> `(READ, MEMORY)`),
  added alongside the existing `knowledge.retrieve` registration.
- **`DefaultReasoningKnowledgeSource` construction**, alongside the existing `knowledgeRetrieval =
  DefaultKnowledgeRetrieval(...)` construction (current line 905), from the same, already-shared
  `knowledgeItemPersistence` (current line 872) and `permissionEngine` instances, plus a new
  `reasoningContextMemoryRetrieval = permissionFilteredMemoryRetrieval.forAuthorizationPurpose(REASONING_CONTEXT_RETRIEVAL_PURPOSE)`,
  mirroring `candidateEvaluationMemoryRetrieval`'s own identical construction (current lines 860-861)
  exactly.
- **Cutover, atomically, in this same unit and the same reviewable change as the constructor-signature
  change above:** the `InMemoryKnowledgeStore()` construction and `memorySource: KnowledgeSource`
  binding (current lines 391-392) and its use as `DefaultReasoningContextAssembler`'s fourth
  constructor argument (current line 409) are removed; `DefaultReasoningContextAssembler` is
  constructed instead with the new `DefaultReasoningKnowledgeSource` instance in that position, using
  the assembler's own revised constructor signature this same unit already changes. There is no
  intermediate state, and no separately reviewed unit boundary, at which the assembler declares one
  dependency type while production still supplies the other -- both changes are one diff.

**Explicit exclusions:** no change to `IdentityService`, `ToolRegistry`, `ConversationHistorySource`,
or `WorldModelSource` handling anywhere in `DefaultReasoningContextAssembler.kt` -- all four remain
byte-for-byte unchanged, in call order and in rendering. No change to `MEMORY_QUERY_MAXIMUM_RESULTS`'s
own value. No final, owner-facing wording decision -- the format above is the internal model-prompt
projection only. No `KnowledgeSource`/`KnowledgeQuery`/`KnowledgeRecord` reference remains anywhere in
`DefaultReasoningContextAssembler.kt` after this unit. No change to `DefaultKnowledgeRetrieval`'s own
construction (current line 905) or to the `candidateEvaluationMemoryRetrieval`/
`evidenceIntelligenceMemoryRetrieval` constructions (current lines 860-863). No change to the
pre-existing Gap #54 `PermissionPolicyRule` entries (current lines 617-656). No change to any Evidence
Intelligence, Evidence Custodian, or Memory Core Durability wiring. `KnowledgeSource`, `KnowledgeStore`,
and `InMemoryKnowledgeStore` are not deleted anywhere in this repository by this unit -- only the one
production wiring site above. No composition test file is created or modified by this unit --
composition-level verification is exclusively Unit 4's own, later, separately reviewed responsibility
(Section 9, below).

**Required tests, exact** (`tests/runtime/DefaultReasoningContextAssemblerTest.kt`, replacing the
existing "Memory (Sprint 11 Unit 7)" test block, its own current lines 397-571 (ending immediately
before the "14. Sprint 11 Unit 8: World Model rendering" marker), entirely -- every test
in that block targets the now-removed `KnowledgeSource`/`KnowledgeQuery`/`KnowledgeRecord` contract).
This file proves the assembler's own local contract, rendering, ordering, and regression requirements
only; production-composition assertions -- that the real, composed `ParkerRuntime` graph genuinely
wires these values together -- are assigned exclusively to Unit 4 (Section 9, below), never to this
file:

- *Constructor:* `` `the assembler's constructor accepts exactly five dependencies -- IdentityService, ToolRegistry, ConversationHistorySource, ReasoningKnowledgeSource, and WorldModelSource` `` (renamed from the existing test, `KnowledgeSource` replaced by `ReasoningKnowledgeSource`).
- *Positive/regression:* an empty `recall` result produces no `Memory:` entries but calls `recall`
  exactly once; a single returned `SafeKnowledgeResultEntry` is rendered as one `Memory:` entry in the
  exact frozen format; multiple returned entries are rendered in the exact order `recall` returns them,
  never reordered.
- *Rendering, exact:* `evidentialState`, `status`, and `staleness` are each rendered via their own
  `.name`, exactly, never fabricated, never omitted (all three are non-nullable on
  `SafeKnowledgeResultEntry` -- this replaces the removed "confidence omitted when absent" test, whose
  own premise, an optional field, no longer applies).
- *Escaping, exact:* backslash, LF, CR, TAB, every C0 control character, DEL, every C1 control
  character, `U+2028`, and `U+2029` each escape to their exact defined form, including the
  deterministic four-hex-digit `\uXXXX` case for `U+2028`/`U+2029` specifically; a `content` value
  containing a raw LF, CR, `U+2028`, or `U+2029` still yields exactly one `ReasoningContext` entry, with
  no additional prompt line or entry created by the embedded character.
- *Query construction:* the constructed `KnowledgeRetrievalQuery` carries `relevance = message.text`
  and `correlationId = message.correlationId.value`; `requestingPrincipalId` is passed as `recall`'s
  own separate first parameter, equal to `message.senderPrincipalId` -- a distinct test from the query-
  field test, since `KnowledgeRetrievalQuery` itself carries no principal field (unlike the removed
  `KnowledgeQuery`).
- *Bounds:* the constructed `KnowledgeRetrievalQuery` always carries a positive, caller-supplied
  `maximumResults` -- no specific value architecturally asserted, mirroring the removed test's own
  precedent.
- *Failure:* a `ReasoningKnowledgeSource.recall` failure propagates unchanged, not caught or wrapped.
- *Interface shape:* `` `ReasoningKnowledgeSource exposes no mutation operation -- only recall` ``
  (renamed from the removed `KnowledgeSource` version, same reflective-interface-check concept).
- *Genuine end-to-end (Assembler level):* a `KnowledgeItem` promoted through a real
  `DefaultKnowledgeSubmission` (backed by a real `InMemoryKnowledgeItemPersistence` and a real
  `DefaultKnowledgeCandidateEvaluator`) is retrieved through a real `DefaultReasoningKnowledgeSource`
  (Unit 2's own class, wired to the same `InMemoryKnowledgeItemPersistence`, a permissive
  `FakePermissionEngine`, and a real `InMemoryMemoryCore` holding the promoted evidence) and rendered
  as a genuine `Memory:` entry by the real `assemble()` call -- the Assembler-level analog of Section
  14's own required proof (the full, composed-runtime version of which Unit 5, Section 10, below,
  implements), replacing the removed `InMemoryKnowledgeStore`-based end-to-end test.

**Completion evidence:** every test above passing; every test in the removed block (its own current
lines 397-571, verified directly against source rather than assumed from any count) is accounted for by
name above -- constructor-arity, empty-result, single-entry-rendering, multi-entry-ordering,
confidence-omission, query-construction, `maximumResults`-carriage, `recall`-failure-propagation,
interface-shape, and the `InMemoryKnowledgeStore`-based genuine end-to-end test -- each either
renamed-and-adapted or explicitly replaced with its own stated justification, above; full local Gradle
build passing; direct inspection of the `src/composition/ParkerRuntime.kt` diff,
confirming the assembler's constructor-signature change and the production composition cutover landed
together in this unit's own single commit and review boundary, with no broken intermediate commit
between them; grep-level confirmation that `InMemoryKnowledgeStore(` and `memorySource` no longer
appear anywhere in `ParkerRuntime.kt`.

**Stop conditions:** halt if any `KnowledgeSource`/`KnowledgeQuery`/`KnowledgeRecord` symbol remains
referenced anywhere in `DefaultReasoningContextAssembler.kt` or its test after this unit; halt if
escaping coverage is found to omit any character this document's own Section 13 (below) requires; halt
if `IdentityService`, `ToolRegistry`, `ConversationHistorySource`, or `WorldModelSource` rendering
changes in any way; halt if, at any point during this unit, both the legacy and new knowledge-feed
bindings are simultaneously active in a committed state; halt if a fourth `PermissionPolicyRule` naming
`ResourceType.DOCUMENT` for this Purpose is introduced; halt if any pre-existing Gap #54 rule, resource,
or vocabulary entry is modified; halt if `KnowledgeSource`/`KnowledgeStore`/`InMemoryKnowledgeStore`
deletion is proposed (not authorized by this or any prior document); halt if the assembler's
constructor-signature change and the `ParkerRuntime.kt` call-site update cannot be committed together,
atomically, as a single reviewable change -- this unit may not be split, and no other unit may complete
either half of this cutover on its behalf.

**Reviews required before Unit 4 may begin:** a Unit Completion Review and an Independent
Constitutional Review, both accepted, confirming the full replacement test block matches this section
exactly, that no non-memory rendering changed, and that the `src/composition/ParkerRuntime.kt` diff was
directly inspected together with a successful full Gradle suite -- establishing that the sole
production caller was updated atomically alongside the constructor-signature change and that no broken
intermediate commit exists.

---

## 9. Unit 4 — Composition Verification (Test-Only)

**Files this unit may create:** exactly `tests/composition/ParkerRuntimeReasoningKnowledgeSourceCompositionTest.kt`
(new file). No other file, and in particular **no production file, including
`src/composition/ParkerRuntime.kt`, may be created, modified, or repaired by this unit under any
circumstance.** Unit 3 (Section 8, above) has already implemented and been independently reviewed for
the entire production composition this unit verifies -- Purpose registration, the three new
`PermissionPolicyRule` entries, resource/vocabulary registration, `DefaultReasoningKnowledgeSource`
construction, and the single-feed cutover. This unit performs no cutover, no construction, and no
repair of any kind; it exists solely to prove, from outside production code, that Unit 3's own accepted
composition genuinely holds.

**Frozen contract verified, exact** (nothing below is implemented by this unit -- every value was
already fixed and built by Unit 3, Section 8, above, and is restated here only to fix exactly what this
unit's own tests must prove against it):

- Purpose registration and the exactly three new `PermissionPolicyRule` entries (no fourth; no
  `memory.retrieve_document` approval; the pre-existing Gap #54 Document-denial guard remains
  effective, untouched).
- Resource/vocabulary registration and `DefaultReasoningKnowledgeSource` construction, sharing the same
  `knowledgeItemPersistence` and `permissionEngine` instances `DefaultKnowledgeRetrieval` already uses.
- The single-feed production cutover: `DefaultReasoningContextAssembler` receives no `KnowledgeSource`;
  no production path constructs `InMemoryKnowledgeStore`.

**Explicit exclusions:** no production file may be created, modified, or repaired by this unit, under
any circumstance, including in direct response to a failing test this unit's own tests reveal. **If
this unit's own tests fail because Unit 3's production composition is incomplete or incorrect, that
failure is a Unit 3 review failure and a stop condition (Section 19, below), requiring return to Unit
3 -- never authority for this unit to edit `ParkerRuntime.kt` or any other production file itself.** No
change to `DefaultReasoningContextAssembler.kt`, its own test file, or any file outside the one new
composition test file named above.

**Required tests, exact** (`tests/composition/ParkerRuntimeReasoningKnowledgeSourceCompositionTest.kt`,
mirroring `ParkerRuntimeKnowledgeRetrievalCompositionTest.kt`'s own structure -- every test below
verifies Unit 3's already-accepted production composition; none constructs or repairs it):

- *Construction:* the composed graph constructs successfully when `ParkerRuntime` starts; the
  constructed `DefaultReasoningContextAssembler` holds a genuine `DefaultReasoningKnowledgeSource`
  instance (reflection, mirroring line 136-148's own established pattern).
- *Shared instance:* the same `InMemoryKnowledgeItemPersistence` instance backs Knowledge Submission,
  Knowledge Retrieval, and `DefaultReasoningKnowledgeSource` -- proven, not merely asserted (mirroring
  lines 148-169's own pattern), so anything promoted is genuinely reachable through the new surface.
- *Authorization, positive:* a promoted, matching `KnowledgeItem` is retrieved through the composed
  `DefaultReasoningKnowledgeSource`, exactly as the corresponding existing `DefaultKnowledgeRetrieval`
  test (lines 186-208) already proves for that class.
- *Authorization, negative:* act-level denial for an unregistered principal returns `emptyList()`,
  evaluated against the real, composed `DefaultPermissionPolicy`, not a fake. **This unit does not, and
  may not, require an authorized act whose candidate set contains one policy-approved and one
  policy-denied item evaluated against the real, composed `DefaultPermissionPolicy` -- no
  `PermissionPolicyRule` field, and no field `PermissionFilteredMemoryRetrieval.isApproved` consults,
  varies by candidate, resource, or evidence identity (confirmed directly against
  `DefaultPermissionPolicy.ruleOutcomeFor`, lines 228-244, and every `isApproved` call site in
  `PermissionFilteredMemoryRetrieval.kt`); the differing item-level `intent` string is never consulted
  by the real policy. Item-level silent exclusion of a denied candidate is proven once, sufficiently, at
  Unit 2 (Section 7, above) using a controllable `FakePermissionEngine`, mirroring
  `DefaultKnowledgeRetrieval`'s own identical, already-governed precedent for its own structurally
  identical limitation -- this unit independently proves the real production policy and Purpose
  configuration (the bullets below), never a mixed per-item outcome from that same real policy.**
- *Least authority, direct policy proof and direct purpose-bound-view proof, constructed
  independently and genuinely by this test file, never by accessing, reflecting into, or
  otherwise depending on `ParkerRuntime`'s own private `permissionEngine` field or its own
  construction-local `permissionFilteredMemoryRetrieval`/`reasoningContextMemoryRetrieval` values.*
  Both values remain genuinely inaccessible test-side, by design -- no accessor, no reflection, and
  no widened visibility is introduced anywhere in this programme to reach them. **The frozen
  governance (Contract Invariant 13; Scope Lock §4, §7, §11) requires proof through the real
  production mechanism and a real denial outcome -- it does not require object identity with
  `ParkerRuntime`'s own inaccessible local instance.** This unit's own test file instead constructs
  its own independent, real production object graph, using only real, unmodified production classes:

  1. a real `InMemoryAuthorizationPurposeRegistry()`;
  2. registration and activation, on that registry, of the exact frozen Purpose,
     `knowledge-memory.reasoning-context-retrieval` (`REASONING_CONTEXT_RETRIEVAL_PURPOSE`);
  3. **a real `InMemoryActionVocabulary()`, with exactly one entry registered on it** --
     `ActionVocabularyEntry(verbPhrase = PermissionFilteredMemoryRetrieval.RETRIEVE_DOCUMENT_ACTION_NAME,
     mappings = setOf(ActionResourceMapping(PermissionAction.READ, ResourceType.DOCUMENT)))` --
     transcribing, not duplicating or altering, the identical mapping already registered in
     `ParkerRuntime.kt` (current lines 529-533) as a pre-existing Gap #54 Memory Retrieval
     Operationalisation Unit 2 production registration, predating and outside this programme
     entirely. **Unit 3 does not introduce, own, modify, replace, or re-register this vocabulary
     entry** -- Unit 3's own frozen responsibility (Section 8, above) is to preserve this pre-existing
     entry completely unchanged while adding only this programme's own explicitly authorized changes
     (Purpose registration, the three new `PermissionPolicyRule` entries, and the one new
     `knowledge.retrieve_for_reasoning_context` vocabulary entry, current lines 991-1014, a wholly
     separate registration for a different verb). Copying this pre-existing production value into this
     unit's own independent test graph authorizes no new production vocabulary entry or capability of
     any kind -- it reproduces, verbatim, a value that already exists in production before this
     programme begins. **Without this registration, the direct policy assertion below
     (item 10) could return `DENIED` for the wrong reason -- an unresolvable action
     (`ActionMappingFailureReason.UNKNOWN_ACTION`) -- before `DefaultPermissionPolicy`'s own
     Purpose-aware rule is ever evaluated, making the proof pass vacuously.** A mandatory precondition
     assertion, before either denial assertion below (items 9 and 10): `vocabulary.lookup(
     PermissionFilteredMemoryRetrieval.RETRIEVE_DOCUMENT_ACTION_NAME)` resolves to exactly one
     `ActionVocabularyEntry` whose own `mappings` equal exactly `setOf(ActionResourceMapping(
     PermissionAction.READ, ResourceType.DOCUMENT))` -- no additional, narrower, or substitute mapping
     may satisfy this check;
  4. **a real `ActionMapper(vocabulary)`**, constructed from that same vocabulary (item 3), and
     supplied to the real `DefaultPermissionPolicy` below (item 5) -- never omitted, never a fake,
     stub, or hand-rolled mapper; omitting it, or substituting a fake that always resolves, would
     equally let this proof pass vacuously, never genuinely exercising action-mapping resolution;
  5. a real `DefaultPermissionPolicy`, configured with the real `ActionMapper` from item 4, the exact
     frozen `targetlessResourceTypesByProposedAction` mapping (unchanged by this design -- no new
     entry), the pre-existing Gap #54 verb-only `DENIED` rule for `memory.retrieve_document`
     (transcribed verbatim from current `ParkerRuntime.kt` lines 630-636), and the exact three new
     Unit 3 `PermissionPolicyRule` entries (transcribed verbatim, Section 8, above) -- no
     Purpose-specific Document approval, no extra rule, no substitute rule;
  6. a real `DefaultPermissionEngine(identityService, policy)`, backed by a real
     `InMemoryIdentityService`, with a requesting principal that is both **registered and
     transitioned to `ACTIVE`** before either authorization assertion below -- exact, mandatory
     sequence: `identityService.register(principal)` (which, per `InMemoryIdentityService.register`'s
     own enforced precondition, requires `principal.status == PrincipalStatus.CREATED` and throws
     otherwise -- confirmed directly against source; a newly registered principal is never anything
     but `CREATED`), then `identityService.updateStatus(principalId, PrincipalStatus.ACTIVE)`
     (`InMemoryIdentityService`'s own sanctioned lifecycle transition), then a direct assertion that
     `identityService.resolve(principalId)?.status == PrincipalStatus.ACTIVE` before proceeding --
     mirroring `tests/runtime/AuthorizationPurposeEndToEndVerificationTest.kt`'s own established
     `registerAt(identityService, id, status)` helper and its identical
     `register` -> `updateStatus(ACTIVE)` two-step precedent. **Registration alone is insufficient:**
     a principal that is only registered remains `CREATED`, and `CREATED` is not `ACTIVE` --
     `DefaultPermissionEngine.evaluate` (`src/runtime/DefaultPermissionEngine.kt`) denies a
     non-`ACTIVE` principal before `DefaultPermissionPolicy` is ever consulted, so a test using only a
     registered-but-`CREATED`, inactive, unregistered, or entirely absent principal would return
     `DENIED`/`null` at that identity gate alone, never reaching -- and therefore never genuinely
     proving anything about -- `DefaultPermissionPolicy`'s own Purpose/action rule evaluation this
     proof exists to test. This proof must not be satisfied by, and must not pass at, that identity
     short-circuit; it must reach and be decided by the real `DefaultPermissionPolicy` evaluation
     itself, using the confirmed-`ACTIVE` principal. (An inactive, `CREATED`, unregistered, or absent
     principal returning a denial/`null` result is already, separately, the *Authorization, negative*
     test, above, and the Purpose denial matrix, below -- neither substitutes for this proof, and this
     proof does not substitute for them.)
  7. a real `PermissionFilteredMemoryRetrieval(delegate, permissionEngine)`, whose `delegate` returns
     a genuine, well-formed, existing `Document` value for the requested `DocumentId` (mirroring
     `tests/composition/PermissionFilteredMemoryRetrievalTest.kt`'s own established
     `FakeMemoryRetrieval(documentResult = ...)` convention for supplying that one record only --
     never a substitute for the permission-gating behaviour itself, which remains entirely real);
  8. a real purpose-bound view, created through the exact production factory method Unit 3 itself
     uses, never a reimplementation of it -- confirmed already directly callable, without
     reflection, from `tests/composition/*.kt` today by
     `tests/composition/PermissionFilteredMemoryRetrievalTest.kt`'s own existing, compiling use of
     the identical, already-`internal` `forAuthorizationPurpose` factory:
     ```kotlin
     val purposeBoundView = permissionFilteredMemoryRetrieval.forAuthorizationPurpose(
         REASONING_CONTEXT_RETRIEVAL_PURPOSE,
     )
     ```
  9. a direct call, `purposeBoundView.getDocument(activePrincipalId, documentId)` -- using the same
     confirmed-`ACTIVE` principal from item 6, never a different one -- asserted to return `null`,
     against the same fully configured real policy/engine/vocabulary graph items 1-7 construct (never
     a narrower or differently configured graph). Confirmed directly against
     `PermissionFilteredMemoryRetrieval.getDocument`'s own source
     (`src/composition/PermissionFilteredMemoryRetrieval.kt` lines 138-148): the private `getDocument`
     calls `delegate.getDocument(...)` *unconditionally, before* evaluating permission -- the
     delegate's own genuine, non-null `Document` value is always fetched, in both the approved and
     denied case; only the *returned* value, decided by line 148's own `isApproved(...)` check,
     depends on the permission outcome. This proof therefore cannot claim, and must not claim, that
     the delegate's accessor is never reached after denial -- it instead proves the accurate, stronger
     claim actually true of this production wrapper: the delegate's real, well-formed `Document` is
     fetched, and despite that, its content is never disclosed to the caller once `isApproved` returns
     `false` -- `getDocument` returns `null`, not the fetched `Document`, and no field, fragment, or
     derivative of that `Document` is observable in the test's own assertion. **Neither
     `DefaultReasoningKnowledgeSource` nor any component this design adds constructs, issues, or
     mediates this `getDocument` call or its own underlying request -- `DefaultReasoningKnowledgeSource`
     has no `ToDocument` or Document-retrieval path of any kind (Section 7, above; Contract Design
     Section 5); `PermissionFilteredMemoryRetrieval.getDocument` itself invokes its own authorization
     check (`isApproved`, line 148, calling `PermissionFilteredMemoryRetrieval.buildExecutionRequest`,
     `src/composition/PermissionFilteredMemoryRetrieval.kt` lines 253-271) -- this proof exercises that
     existing, unmodified production component directly, never a path this design's own class owns.**
  10. a separate, direct `policy.evaluate(...)` assertion, using the same confirmed-`ACTIVE` principal
      and the same real `ActionMapper`/vocabulary from items 3-4, proving `DENIED`. **`ExecutionRequest`
      carries no `resourceType` field of its own -- it is never described as one this proof "carries."**
      The request instead carries the frozen targetless
      `proposedActions = listOf(PermissionFilteredMemoryRetrieval.RETRIEVE_DOCUMENT_ACTION_NAME)`, an
      **empty** `targetResources` (the targetless form Contract Design Section 7 and
      `PermissionFilteredMemoryRetrieval.buildExecutionRequest`'s own production construction require
      for this verb -- confirmed directly against that method's own source, lines 253-271, which sets
      `targetResources = emptyList()` and `proposedActions = listOf(actionName)`; not a shape
      `DefaultReasoningKnowledgeSource` itself ever constructs, issues, or supplies), and
      `authorizationPurpose = REASONING_CONTEXT_RETRIEVAL_PURPOSE`. `DefaultPermissionPolicy.evaluate`
      itself derives the governed `ResourceType.DOCUMENT` from the frozen
      `targetlessResourceTypesByProposedAction` mapping (`"memory.retrieve_document" to
      setOf(ResourceType.DOCUMENT)`, confirmed directly against
      `DefaultPermissionPolicy`'s own `GOVERNED_TARGETLESS_RESOURCE_TYPES` companion constant) -- this
      proof does not invent a request field, does not modify the production `ExecutionRequest`
      contract, and does not supply `resourceType` itself; it supplies exactly the same targetless
      shape `PermissionFilteredMemoryRetrieval.buildExecutionRequest`'s own production construction
      already supplies for this verb, and lets the real policy derive the rest.
  11. **a separate, direct non-vacuity assertion**, using the same real `actionMapper` from item 4:
      `actionMapper.map(listOf(PermissionFilteredMemoryRetrieval.RETRIEVE_DOCUMENT_ACTION_NAME),
      setOf(ResourceType.DOCUMENT))` (the identical call `DefaultPermissionPolicy.evaluate` itself
      makes internally for this targetless verb) returns exactly
      `listOf(ActionMappingResult.Resolved(PermissionFilteredMemoryRetrieval.RETRIEVE_DOCUMENT_ACTION_NAME,
      setOf(ActionResourceMapping(PermissionAction.READ, ResourceType.DOCUMENT))))` -- never
      `ActionMappingResult.Failed(..., ActionMappingFailureReason.UNKNOWN_ACTION)` and never
      `Failed(..., ActionMappingFailureReason.RESOURCE_TYPE_MISMATCH)`. **`PermissionDecision` itself
      (the type item 10's own `policy.evaluate(...)` call returns) carries no
      `ActionMappingFailureReason` field of any kind -- confirmed directly against
      `DefaultPermissionPolicy.evaluate`'s own source: its `resolvedMappings.isEmpty()` branch returns
      a generic, reason-less `deniedDecision(request)` indistinguishable, by that return value alone,
      from a genuine Purpose-rule `DENIED`. Because the production decision type does not expose a
      mapping-failure reason, this proof does not, and cannot, assert one is absent on
      `PermissionDecision` itself; it instead proves non-vacuity through this item's own direct,
      separate `ActionMapper.map(...)` call -- using the real vocabulary items 3-4 construct -- together
      with item 10's own `DENIED` result, exactly as the two together, not either alone, are required
      to prove.**

  **This proof must distinguish exactly these outcomes, never conflating them:** `UNKNOWN_ACTION` or
  any other action-mapping failure (item 11) is a test failure, full stop -- it never satisfies this
  proof. Identity/status rejection (item 6's own gate) is a test failure for this proof specifically --
  it is proven, separately, by the *Authorization, negative* test, above. Purpose registration/status
  rejection (item 2's own gate) is likewise a test failure for this proof specifically -- it is proven,
  separately, by the Purpose denial matrix, below. Only a real Purpose/action `DefaultPermissionPolicy`
  evaluation genuinely reaching and resulting in `DENIED` (item 10, corroborated by item 11's own
  non-vacuity proof) is the required success this proof exists to demonstrate.

  No reflection is used anywhere in this proof. No test-only or production accessor is added to
  `ParkerRuntime.kt`, `DefaultReasoningKnowledgeSource.kt`, or any other production file. No
  production visibility is widened -- `forAuthorizationPurpose` remains exactly as `internal` as
  Unit 3 already built it. `DefaultReasoningKnowledgeSource` itself is not touched, referenced, or
  extended by this test -- it exposes no raw `MemoryRetrieval` capability of its own, and gains no
  `ToDocument`/`getDocument` path of its own, before or after this test exists. This transcription
  authorizes no production vocabulary rule, capability, or entry beyond the pre-existing Gap #54
  Unit 2 production registration, which Unit 3 must preserve unchanged (item 3, above) -- only the
  frozen rule values (item 5, above) and the frozen vocabulary
  entry (item 3, above) are repeated in this test; the specificity resolution, action-mapping,
  Purpose-activity check, identity-status check, and permission-gating that decide the outcome are the
  real, unmodified `DefaultPermissionPolicy`, `ActionMapper`, `DefaultPermissionEngine`,
  `InMemoryIdentityService`, and `PermissionFilteredMemoryRetrieval` implementations -- never a
  duplicated or reimplemented
  algorithm, and never a test substitute standing in for them.
- *Least authority, non-regression:* `KNOWLEDGE_CANDIDATE_EVALUATION_PURPOSE`'s own existing
  Document-approval behaviour (already proven by the existing candidate-evaluation composition/unit
  tests) is unaffected -- confirmed by running the existing suite unchanged, not by a new assertion in
  this file.
- *Purpose denial matrix:* wrong, absent, inactive, unregistered, and mismatched Purpose each produce
  `emptyList()` through the specificity-1 `DENIED` guard for `knowledge.retrieve_for_reasoning_context`;
  a request carrying `KNOWLEDGE_CANDIDATE_EVALUATION_PURPOSE` or
  `EVIDENCE_INTELLIGENCE_INPUT_RESOLUTION_PURPOSE` against
  `knowledge.retrieve_for_reasoning_context` is denied (coarse-rule/cross-Purpose fall-through
  prevention).
- *Evidence Intelligence non-widening:* same runtime, immediately after a successful `recall` through
  the new surface, `EvidenceIntelligenceInputResolver`'s own existing denial behaviour is unchanged
  (mirroring Gap #54 Unit 5's own same-runtime non-widening proof style).
- *Lifecycle/staleness/ordering:* a `RETIRED` item is excluded by default and included with
  `includeRetired = true`; staleness disclosure is genuinely computed through the composed runtime
  using the real system clock; deterministic ordering is preserved across repeated calls -- each
  mirroring `ParkerRuntimeKnowledgeRetrievalCompositionTest.kt`'s own existing tests for
  `DefaultKnowledgeRetrieval` (lines 262-352), applied to the new surface.
- *No legacy path remains (structural):* `DefaultReasoningContextAssembler`'s own declared field types
  contain no `parker.core.interfaces.KnowledgeSource` (reflection, mirroring line 407's own established
  style); no production code path in `ParkerRuntime.kt` constructs `InMemoryKnowledgeStore` (a
  source-level or reflective proof that the class is never referenced in the composition method).
- *Purpose registration:* `REASONING_CONTEXT_RETRIEVAL_PURPOSE` is registered and `ACTIVE` at
  composition time (`authorizationPurposeRegistry.isActive(...)`, direct proof).
- *Non-regression:* Knowledge Submission's own `WRITE MEMORY` gate remains unaffected by the new `READ
  MEMORY` rules Unit 3 registered (mirroring lines 379-404's own existing non-regression test for the
  analogous Gap #54 addition).

**Completion evidence:** every test above passing; the full existing suite (including
`ParkerRuntimeKnowledgeRetrievalCompositionTest.kt` and every candidate-evaluation test) passing
unchanged; full local Gradle build passing; confirmation that this unit's own diff touches exactly one
file -- the new composition test file -- and no production file of any kind; direct inspection
confirming the Document-denial proof's own principal was registered, transitioned to `ACTIVE`, and
asserted `ACTIVE` before either authorization assertion, and that the direct policy assertion's own
`ExecutionRequest` carries only `proposedActions`/`authorizationPurpose` (empty `targetResources`),
never an invented `resourceType` field; direct inspection confirming a real `InMemoryActionVocabulary`
carrying exactly the transcribed `READ`/`DOCUMENT` mapping was constructed and supplied to a real
`ActionMapper`, itself supplied to the real `DefaultPermissionPolicy`, and that the mandatory
`vocabulary.lookup(...)` precondition and the `actionMapper.map(...)` non-vacuity assertion (item 11,
above) both passed before either denial assertion was evaluated.

**Stop conditions:** halt if this unit's own diff touches any production file, including
`src/composition/ParkerRuntime.kt`; halt if a failing test in this file is resolved by editing
production rather than by returning to Unit 3; halt if any test in this file requires a change to
`DefaultReasoningKnowledgeSource`, `DefaultReasoningContextAssembler`, or any Unit 1-3 frozen contract
to pass -- such a finding means Unit 3's own composition is incomplete, and is that unit's own review
failure, never this unit's authority to repair; halt if, at any point, both the legacy and new
knowledge-feed bindings are found simultaneously active -- that finding is likewise a Unit 3 review
failure, not something this unit may fix; halt if the least-authority direct policy or purpose-bound-
view proof (above) is found to require reflection into `ParkerRuntime`, a new test-only or production
accessor on any production file, or any widening of `forAuthorizationPurpose`'s existing `internal`
visibility -- the independent, real production object graph this proof constructs (above) is the only
authorized mechanism, and a discovered need to reach `ParkerRuntime`'s own private instance instead is
a stop condition requiring return to this Implementation Plan, never a workaround; **halt if this unit,
a later implementer, or any review claims or asserts that the real, composed `DefaultPermissionPolicy`
can produce, or was made to produce, a mixed approved/denied outcome for two candidates sharing an
identical (action, resourceType, Purpose, proposedAction) tuple, without a separately governed
discriminator field being added to `ExecutionRequest`, `PermissionPolicyRule`, or the policy-matching
algorithm itself (Contract Design and Scope Lock amendment required first) -- no such claim is
authorized anywhere in this programme, and the frozen policy design structurally cannot support one;**
**halt if the Document-denial proof uses a principal that is not confirmed `ACTIVE` (by a direct
`identityService.resolve(...)?.status == PrincipalStatus.ACTIVE` assertion, before either authorization
assertion), or if the proof can pass before `DefaultPermissionPolicy` itself evaluates the Purpose/
action rule -- a registered-but-`CREATED`, inactive, unregistered, or absent principal short-circuiting
at `DefaultPermissionEngine`'s own identity gate does not, and cannot, constitute this proof;** **halt
if the Document-denial proof can pass through `UNKNOWN_ACTION`, `RESOURCE_TYPE_MISMATCH`, identity
rejection, or Purpose-registration rejection before the intended `DefaultPermissionPolicy` rule
evaluation -- the mandatory `InMemoryActionVocabulary` registration, `vocabulary.lookup(...)`
precondition, and `actionMapper.map(...)` non-vacuity assertion (item 11, above) are the only
authorized mechanisms for excluding an action-mapping-failure explanation; a discovered need to omit
the action-vocabulary registration, or to substitute a fake `ActionMapper`, is a stop condition
requiring return to this Implementation Plan, never a workaround.**

**Reviews required before Unit 5 may begin:** a Unit Completion Review and an Independent
Constitutional Review, both accepted, confirming exactly three new rules, no Document authority, the
single-feed cutover, every test above passing against Unit 3's already-accepted production composition,
that this unit's own diff introduced no production change, that the least-authority proof was built
from an independently constructed, real production object graph -- never reflection into, an accessor
on, or widened visibility into `ParkerRuntime`'s own private instance -- that no test or claim in
this unit asserts a mixed per-item outcome from the real `DefaultPermissionPolicy`, that the
Document-denial proof's own requesting principal was confirmed `ACTIVE` before either authorization
assertion and that both assertions genuinely reached and were decided by `DefaultPermissionPolicy`
itself (not an identity short-circuit), that the direct policy assertion supplies only the frozen
targetless `proposedActions`/`authorizationPurpose` shape, never an invented `resourceType` request
field, and that a real `InMemoryActionVocabulary`/`ActionMapper` carrying exactly the transcribed
`READ`/`DOCUMENT` mapping was constructed and used throughout, with the `vocabulary.lookup(...)`
precondition and `actionMapper.map(...)` non-vacuity assertion both confirmed passing, so that neither
`UNKNOWN_ACTION` nor `RESOURCE_TYPE_MISMATCH` could have produced the observed `DENIED` result.

---

## 10. Unit 5 — Genuine End-to-End Proof

**Files this unit may create or modify:** `tests/composition/ParkerRuntimeReasoningContextIntegrationTest.kt`
only. No production file. This unit adds no new capability -- it proves, at the full `ParkerRuntime`
level, that the cutover Unit 3 performed, and that Unit 4 already verified in isolation, genuinely
works end to end.

**Frozen contract implemented:** the required genuine end-to-end proof (Planning Review Section 11;
Contract Design Section 14; Scope Lock Section 2), exact:

```text
owner Remember X
  -> genuine evidence and KnowledgeItem promotion (real MemoryAdmissionCoordinator -> DefaultKnowledgeSubmission)
  -> later owner query using X's content, a separate conversational turn
  -> real, same-runtime DefaultReasoningKnowledgeSource.recall, resolved by X's dereferenced content
  -> a real ReasoningContext.entries value genuinely containing X's content, safely rendered
  -> the real assembled model prompt containing X's content
```

**Explicit exclusions:** no synthetic or hand-constructed `KnowledgeItem` may substitute for either
promotion or recall -- both must be real, through the real production pipeline, at the full
`ParkerRuntime` level. No restart or process boundary is introduced or crossed -- same-runtime only.
No claim of restart durability is made by this test or its own name.

**Required tests, exact:**

- The renamed test from Section 3, above (`` `ReasoningKnowledgeSource is wired into the real ParkerRuntime and renders no Memory entries when nothing has been promoted` ``)
  -- corrected wording, unchanged assertion, proving the authorized-empty case at full runtime scale.
- **The required genuine end-to-end proof, new:** using the real `ParkerRuntime`'s own owner-facing
  entry points already exercised elsewhere in this file (`submitOwnerMessage`, and this file's own
  established pattern for reaching the Remember/promotion path through a real conversational turn,
  mirroring how `ParkerRuntimeReasoningContextIntegrationTest.kt`'s sibling conversation-pipeline tests
  already invoke real owner messages), Remember a distinctive proposition X in one turn, then, in a
  genuinely separate `submitOwnerMessage` call carrying text that overlaps X's own content, inspect
  `stub.receivedRequestBodies`' own most recent entry directly and assert it contains a `"Memory: "`
  entry whose content is X, matching the exact rendering format Unit 3 fixed. A friendly reply alone
  is not evidence -- this test inspects the real assembled prompt string directly, mirroring the Unit 5
  Completion Review's own "real persistence, not a friendly reply" discipline this Programme
  inherits by name.

**No composed mixed-evidence negative companion is required or authorized.** This document's own prior
revision required a further negative companion proving that a promoted `KnowledgeItem` whose referenced
evidence the querying owner is not authorized to see is honestly absent from the real prompt, for the
same registered owner and Purpose that the positive proof above already exercises. That companion is
not achievable against the real, composed production stack: for one registered, active, correctly
Purposed owner principal, every Assertion/Entity retrieval under that principal and Purpose is
uniformly approved or uniformly denied by `DefaultPermissionPolicy`/`PermissionFilteredMemoryRetrieval`
-- no field either consults varies by evidence identity (Section 9, above), so the real composed stack
cannot create one visible and one denied reference for the same accepted owner turn without a second,
differently authorized principal (which the required proof's own single-owner-turn definition, above,
does not admit) or an unauthorized test seam. **Denied referenced-evidence exclusion is already fully
proven at Unit 2 (Section 7, above), where a controllable `FakePermissionEngine`/delegate can create the
required differential result no real composed stack can produce for a single principal -- this unit must
not duplicate that proof at a tier where the real policy cannot create the necessary mixed state, and
this correction does not weaken the frozen guarantee that denied Assertion/Entity evidence is silently
excluded (Contract Design §9, §13 Invariant 7; Scope Lock §11), which remains fully and separately
proven at Unit 2.**

**Completion evidence:** the genuine end-to-end test passing, inspected and confirmed by its own
Independent Constitutional Review to be exercising the real pipeline, not a shortcut; full local
Gradle build passing; the full existing suite passing unchanged.

**Stop conditions:** halt if the end-to-end test can only be made to pass by adding a seeding hook,
test-only backdoor, or any change to `MemoryAdmissionCoordinator`/`DefaultKnowledgeSubmission` (both
explicitly excluded, Section 17, below) -- if the real Remember path cannot be reached through this
file's own existing, real `submitOwnerMessage` infrastructure, that is a stop condition requiring
return to this Implementation Plan, not a workaround; **halt if reflection, private-runtime-graph
access, a test-only production seam or accessor, or any change to production policy semantics is
introduced anywhere in this unit merely to manufacture the composed mixed-evidence negative companion
this correction removes (above) -- that proof is not authorized at this tier under any mechanism, and a
discovered wish to reconstruct it here is a stop condition requiring return to this Implementation
Plan, never a workaround.**

**Reviews required before programme evidence sequencing (Section 20) may begin:** a Unit Completion
Review and an Independent Constitutional Review, both accepted, confirming the end-to-end proof is
genuine and same-runtime only, and that no attempt was made to reconstruct the removed composed
mixed-evidence negative companion by any means.

---

## 11. Composition Cutover Guarantee

Restated here as a single, checkable set, each already fixed by an individual unit above:

- `DefaultReasoningContextAssembler` no longer receives the legacy `KnowledgeSource` -- its fourth
  constructor argument's declared type changes to `ReasoningKnowledgeSource` in Unit 3, and its actual
  production argument changes to a `DefaultReasoningKnowledgeSource` instance in that exact same Unit 3
  -- the same unit, the same commit, so no commit exists where the constructor accepts the new type but
  production still supplies the old one, or vice versa. This corrects a defect an independent review
  found in this document's own prior revision, where the two changes were split across separate units
  (originally Unit 3 and Unit 4) with only one production call site to satisfy both -- an
  uncompilable intermediate state. That defect is fixed by allocating both changes to Unit 3 alone
  (Section 8, above); Unit 4 now performs no production change of any kind.
- No production path constructs `InMemoryKnowledgeStore` -- removed in Unit 3, in the same commit as
  the new construction is added.
- Only one production knowledge feed is active at every commit boundary in this programme -- verified
  directly (Unit 3's own stop condition, Section 8, above) and independently confirmed, without any
  production change of its own, by Unit 4's own required tests (Section 9, above).
- `WorldModelSource` and `ConversationHistorySource` inputs to `DefaultReasoningContextAssembler`
  remain completely unchanged -- no unit above touches either dependency, its construction, or its
  rendering.
- Every non-memory `ReasoningContext` entry kind (identity, communication channel, current time,
  current conversation, prior messages, world beliefs, available tools, current request), its
  rendering, and its relative ordering remain unchanged -- proven by the full existing
  `DefaultReasoningContextAssemblerTest.kt` suite (outside the replaced Memory block, untouched by any
  unit) and by every existing test in `tests/composition/ParkerRuntimeReasoningContextIntegrationTest.kt`
  other than the one Memory-specific test Unit 5 renames (Section 3, above), all of which continue to
  pass unmodified. **`ParkerRuntimeReasoningContextIntegrationTest.kt` itself is not unmodified through
  every unit -- it is not touched by Units 1-4 at all, but Unit 5 (Section 10, below) alone extends it
  with the one new genuine end-to-end test and renames the one existing test Section 3, above,
  already names; no other unit may modify this file.** The production composition Unit 3 establishes
  (Section 8, above) must remain unchanged throughout Units 4 and 5 -- neither may edit any production
  file (Section 9's, and Section 10's, own explicit exclusions); Unit 5 adds only the governed positive
  end-to-end proof to this one test file and has no authority to repair production, mirroring Unit 4's
  own identical restriction.

---

## 12. Least-Authority Boundary — Carried Forward, Unweakened

- Exactly three new `PermissionPolicyRule` entries, fixed in Unit 3, Section 8, above -- no fourth.
- No `memory.retrieve_document` approval exists, or may ever be added, for
  `knowledge-memory.reasoning-context-retrieval` -- Contract Invariant 13 and Scope Lock's own
  identical invariant, unweakened.
- The existing Gap #54 Unit 2 verb-only `DENIED` guard for `memory.retrieve_document` remains
  completely untouched by every unit above -- an accidental future document call under this Purpose
  fails closed, by the existing mechanism, with no new code this programme adds.
- `MemoryCoreRecordReference.ToDocument` and `ToRelationship` remain structurally unreachable in
  `DefaultReasoningKnowledgeSource.resolveContent` -- Unit 2's own required negative test (Section 7)
  proves this directly, with a recording spy, not by omission.
- Direct `DefaultPermissionPolicy` and real purpose-bound-view proofs that document retrieval is
  denied are required in Unit 4 (Section 9), not merely inferred from the absence of a rule. Both
  proofs are built from an independently constructed, real production object graph that Unit 4's own
  test file owns -- never from access to `ParkerRuntime`'s own private `permissionEngine` field or its
  own construction-local Purpose-bound-view values, which remain genuinely inaccessible test-side by
  design (no reflection, no accessor, no widened visibility, anywhere in this programme).
- Evidence Intelligence authority is not widened by any unit above -- `EVIDENCE_INTELLIGENCE_INPUT_RESOLUTION_PURPOSE`
  and `knowledge-memory.reasoning-context-retrieval` remain distinct values no rule this programme adds
  can conflate.

---

## 13. Deterministic Matching and Rendering — Carried Forward, No Implementation Discretion

- CRLF and lone CR both become LF, once, in `DefaultReasoningKnowledgeSource.normalize` (Unit 2) --
  ordinary authorized Unicode text otherwise preserved unchanged; no trimming, whitespace collapse,
  stemming, tokenization, synonym expansion, classification, or semantic ranking, anywhere.
- Case-insensitive substring matching via Kotlin's own per-`Char` case folding, locale-independent by
  construction -- no `Locale` parameter is threaded through this design.
- Entity content joins `primaryLabel` and every alias with the fixed, literal `" | "` separator, in
  `Entity.aliases`' own order.
- The exact, fixed rendering format string, field order (`content`, `evidentialState`, `status`,
  `staleness`), and full escaping table (backslash, LF, CR, TAB, every C0/C1/DEL control character,
  `U+2028`, `U+2029`) are fixed in Unit 3, Section 8, above, verbatim from the Contract Design -- no
  alternative format, field order, or escaping scope is authorized.
- Insertion order is preserved through every filter stage in `DefaultReasoningKnowledgeSource`; no
  ranking, scoring, or reordering exists anywhere in this design.
- `maximumResults` is applied once, last, after every authorization, visibility, and relevance filter
  -- never before.

No implementation unit may treat any value in this section as discretionary. A discovered need to
deviate from any of them is a stop condition (Section 19), requiring return to the Contract Design,
never a silent implementation choice.

---

## 14. Required Genuine End-to-End Proof — Restated

Fixed exactly as Section 10, above (Unit 5's own frozen contract) and Planning Review Section 11: real
promotion, a genuinely separate conversational turn, real `DefaultReasoningKnowledgeSource.recall`,
real `ReasoningContext.entries` or the real assembled model prompt inspected directly. No synthetic
`KnowledgeItem` may substitute for either promotion or recall, in any unit's own tests, anywhere in
this programme -- this restriction applies to every unit's own tests, not only Unit 5's, though only
Unit 5's own test satisfies the full end-to-end chain; Units 1-4's own tests may use real, in-unit
constructions (real `InMemoryKnowledgeItemPersistence`, real `InMemoryMemoryCore`, real
`DefaultKnowledgeSubmission`) but are not required to reach the full `ParkerRuntime` level themselves.

---

## 15. Required Denial and Failure Proof Matrix

Consolidated across the units above; each already assigned to its own unit and re-stated here as a
single completeness check:

| Proof | Unit |
|---|---|
| Act-level denial before persistence inspection (zero `findAll()` calls proven) | 2 |
| Wrong, absent, inactive, unregistered, mismatched Purpose denial | 4 |
| Coarse-rule and cross-Purpose fall-through prevention | 4 |
| Item-level denial and silent exclusion (`DefaultReasoningKnowledgeSource`'s own `KnowledgeItem`-visibility gate) | 2 (via controllable `FakePermissionEngine` -- not re-proven at Unit 4, since the real, composed `DefaultPermissionPolicy` cannot produce a mixed per-item outcome) |
| Denied Assertion reference, silent exclusion, no leaked content | 2 (distinct test, evidence-resolution-gate denial, separate from item-level denial above) |
| Denied Entity reference, silent exclusion, no leaked content | 2 (distinct test) |
| Missing Assertion reference, silent exclusion, no fabricated content | 2 (distinct test) |
| Missing Entity reference, silent exclusion, no fabricated content | 2 (distinct test) |
| Unsupported reference kinds (`ToDocument`/`ToRelationship`), no Memory Core call | 2 (distinct test) |
| Authorized-partial results, using a denied-evidence candidate specifically | 2 (distinct test -- may not be substituted with a non-`ACTIVE`, missing-evidence, unsupported-kind, or non-matching candidate) |
| `ACTIVE`-only record-status gating (`DISPUTED`/`SUPERSEDED`/`ARCHIVED`/`DELETED`), silent, never described as permission denial | 2 |
| Generic promotion-basis text does not create a false content match | 2 (distinct regression test, `tests/runtime/DefaultReasoningKnowledgeSourceTest.kt`) |
| Document-denial via the real `DefaultPermissionPolicy`, non-vacuous (action mapping resolved to `READ`/`DOCUMENT`, not `UNKNOWN_ACTION`/`RESOURCE_TYPE_MISMATCH`; active principal; active Purpose; genuine `DENIED` from the Purpose-aware rule evaluation itself) | 4 (real `InMemoryActionVocabulary`/`ActionMapper`, real `InMemoryIdentityService`, real `DefaultPermissionPolicy` -- no fake, no vacuous pass) |
| Empty and `maximumResults` bounding behaviour | 2, 3 |
| No raw Memory Core capability reaching Reasoning Context or the model | 3 (type-level: `SafeKnowledgeResultEntry` carries no such handle, confirmed by its own frozen field list, Section 6) |

The nine Unit 2 denial/missing/unsupported-reference/record-status rows above are each a distinct,
mandatory test (Section 7, above); together they fully discharge Contract Design Section 13's own
Invariant 7 and Scope Lock Section 11's referenced-evidence guarantees, and no row may be satisfied by
another row's own test.

Every row above must be satisfied before Unit 5 may be considered complete; no row may be deferred
past this programme's own Closure Determination.

---

## 16. Timing and Auditability — Carried Forward, Unweakened

- No explicit timing field, count, denial marker, deliberate delay, or deliberately encoded
  protected-state timing signal crosses the `recall` result boundary, in any unit's own implementation.
- Ordinary wall-clock latency may vary naturally between denial, authorized-empty, filtering, and
  dereference paths -- disclosed, never concealed, and never eliminated by any unit's own design.
- No unit, test, or review in this programme may claim constant-time execution or resistance to active
  timing analysis. No elapsed-time-threshold test may be presented as a security or timing-resistance
  proof anywhere in this programme -- tests may prove only the absence of explicit timing metadata and
  the absence of intentional timing encoding (Contract Design Section 9, Section 11; Scope Lock Section
  8).
- No durable permission-decision audit claim is made by any unit. `DefaultPermissionEngine` retains no
  decision history and publishes no event for the direct, self-gating calls this design makes,
  unchanged by any unit above. Adding audit persistence or event publication remains a future,
  separately governed concern this programme does not design, implement, or authorize.

---

## 17. Explicit Exclusions

Carried forward from the Contract Design and Scope Lock, unweakened; no unit above touches, adds, or
claims any of the following:

- Restart durability, in any form -- every type and algorithm in this programme operates only on
  already-in-memory, same-runtime state; no unit's own test may claim or lock in restart behaviour as
  required or intended.
- Durable permission-decision audit infrastructure (persistence or event publication).
- Changes to `DefaultKnowledgeRetrieval`, `KnowledgeRetrieval`, `KnowledgeRetrievalResult`, or
  `KnowledgeResultEntry`.
- Changes to `PermissionFilteredMemoryRetrieval`.
- Changes to the Remember/promotion path (`MemoryAdmissionCoordinator`, `DefaultKnowledgeSubmission`,
  `DefaultKnowledgeCandidateEvaluator`).
- Deletion of `KnowledgeSource`, `KnowledgeStore`, or `InMemoryKnowledgeStore`.
- Semantic search, embeddings, databases, remote services, or indexes.
- Representation Engine work, or any final, owner-facing explanation design.
- World Model or Conversation History changes.
- Evidence Intelligence capability expansion.
- Constant-time padding, batching, obfuscation, or any other timing-channel mitigation.
- Broader Programme 4 propositional-integrity or burden-of-justification work.
- Creation or reservation of a new numbered gap, or a new programme identity. Gap #54 remains complete
  and is not reopened by any unit above.

---

## 18. Every Scope Lock Stop Condition — Carried Forward

Every stop condition the Scope Lock's own Section 12 fixes applies to every unit above, unconditionally,
with no implementation-stage discretion added that conflicts with any of them:

- No Kotlin implementation may begin before this Implementation Plan itself is accepted.
- Halt if `DefaultKnowledgeRetrieval`, `KnowledgeRetrieval`, `KnowledgeRetrievalResult`, or
  `KnowledgeResultEntry` is found to require a change.
- Halt if `KnowledgeSource`, `KnowledgeStore`, or `InMemoryKnowledgeStore` deletion is proposed without
  a fresh repository check proving zero remaining consumers.
- Halt if remembered content would be duplicated outside Memory Core, in any form.
- Halt if Reasoning Context or the model would gain raw Memory Core access, or a reusable
  `MemoryRetrieval`-shaped capability.
- Halt if Evidence Intelligence authority would widen, in any form.
- Halt if authorization would occur after persistence or content disclosure, at any stage.
- Halt if a broad or coarse rule is found to override absent, inactive, unregistered, wrong, or
  mismatched Purpose for `knowledge.retrieve_for_reasoning_context`, `memory.retrieve`, or
  `memory.retrieve_document`.
- Halt if two production knowledge feeds are found active simultaneously.
- Halt if a frozen Programme 3 or Memory Core guarantee is found to require reopening beyond what the
  Contract Design explicitly authorises.
- Halt if live verification cannot inspect real `ReasoningContext.entries` or the real assembled model
  prompt directly.
- Halt if unescaped content, or content containing an unescaped LF/CR, other unescaped control
  character, or an unescaped `U+2028`/`U+2029`, is found reaching `ReasoningContext.entries`.
- Halt if the `ACTIVE`-only Memory Core record-status gate is treated as freely revisable,
  configurable, or implementation-defined without a future Contract Design revision.
- Halt if implementation introduces an intentional timing signal, a deliberate delay, or explicit
  protected-state timing metadata.
- Halt if any later review or test claims constant-time execution or resistance to timing analysis
  without a separately governed mitigation mechanism and its own, matching verification.
- Halt if durable audit persistence or event publication for `permissionEngine.evaluate` decisions is
  added without separate, future governance authorising it.
- Halt if `knowledge-memory.reasoning-context-retrieval` gains `memory.retrieve_document` or any other
  operation not reachable in the locked algorithm.
- Halt if implementation adds a `ToDocument`/`getDocument` path without a future Contract Design
  revision and its own corresponding Scope Lock amendment.
- Halt if any proposed implementation unit requires a file outside the locked scope (Section 4, above).
- Halt if the exact rendering contract, escaping scheme, Purpose identifier, action verb, or any locked
  `PermissionPolicyRule` shape is treated as implementation discretion rather than a frozen value.
- Halt if any test substitutes a synthetic, hand-constructed `KnowledgeItem` for the required genuine
  promotion-to-recall end-to-end proof.

---

## 19. Sequencing-Specific Stop Conditions — This Implementation Plan Adds

- Halt if any unit's own diff touches a file not listed in that unit's own Section (6-10, above).
- Halt if a unit begins before both required reviews for the immediately preceding unit are accepted.
- Halt if `tests/composition/ParkerRuntimeReasoningKnowledgeSourceCompositionTest.kt` or
  `tests/composition/ParkerRuntimeReasoningContextIntegrationTest.kt` is found to require a name or
  scope different from Section 3, above, once implementation begins -- return to this Implementation
  Plan for an amendment, never a silent rename.
- Halt if `ParkerRuntimeKnowledgeRetrievalCompositionTest.kt`'s own existing negative-proof test
  (Section 3, above) is found, at implementation time, to require modification -- that finding would
  itself mean this plan's own repository inspection was incomplete, and requires a return to this
  document, not a silent additional file touch.
- Halt if Unit 3's cutover cannot be committed as a single, atomic change retiring the legacy binding,
  introducing the new one, and changing the assembler's own constructor signature all together -- this
  corrects the defect an independent review found in this document's own prior revision, where the
  constructor-signature change and its sole production caller's update were split across separate
  units with only one production call site to satisfy both.
- Halt if Unit 4 creates, modifies, or repairs any production file, including
  `src/composition/ParkerRuntime.kt`, for any reason, including in response to its own failing tests --
  an incomplete or incorrect Unit 3 production composition is a Unit 3 review failure and a stop
  condition, never authority for Unit 4 to edit production.

---

## 20. Final Programme Evidence Sequence

After Unit 5's own two reviews are accepted:

1. Local full Gradle suite (`./gradlew test` or the repository's own established equivalent), full
   run, passing.
2. Hosted Kotlin/Gradle check (the repository's own existing CI gate), passing.
3. Real, same-runtime live verification of the required end-to-end proof (Section 10, Section 14,
   above), independently observed, not merely re-run from the automated suite.
4. A final programme Completion Review, covering every unit's own accepted Unit Completion Reviews and
   confirming the complete file boundary (Section 4) was not exceeded.
5. A final programme Independent Constitutional Review, confirming every carried-forward boundary
   (Sections 12-13, 16-18) held throughout, unweakened, across every unit.
6. A Closure Determination document -- not authorized, begun, or drafted by this Implementation Plan.

No step in this sequence is performed, and no claim of its outcome is made, by this document.

---

## 21. Decision and Traceability Table

| Unit | Governing section | Exact files | Exact tests (Section) | Required reviews | Entry criteria | Exit criteria |
|---|---|---|---|---|---|---|
| 1 | Contract Design §4, §8; Scope Lock §4 | `src/interfaces/KnowledgeStore.kt` | none (Section 6) | Unit Completion + Independent Constitutional | none (first unit) | Strictly additive diff; full suite passes unchanged |
| 2 | Contract Design §4, §5, §7, §9, §13 Invariant 7; Scope Lock §5, §6, §11 | `src/runtime/DefaultReasoningKnowledgeSource.kt` (new); `tests/runtime/DefaultReasoningKnowledgeSourceTest.kt` (new) | Section 7 | Unit Completion + Independent Constitutional | Unit 1 reviews accepted | Every Section 7 test passing as its own distinct test -- item-level denial, denied Assertion reference, denied Entity reference, missing Assertion reference, missing Entity reference, unsupported reference kinds, record-status gating, authorized-partial result, and generic-basis false-match regression each proven separately, with no substitution between them; no excluded call made |
| 3 | Contract Design §7, §8, §12; Scope Lock §4, §6, §7, §9 | `src/runtime/DefaultReasoningContextAssembler.kt`; `tests/runtime/DefaultReasoningContextAssemblerTest.kt`; `src/composition/ParkerRuntime.kt` | Section 8 | Unit Completion + Independent Constitutional (plus direct `ParkerRuntime.kt` diff inspection and a successful full Gradle suite) | Units 1-2 reviews accepted | Every Section 8 test passing; no legacy symbol remains; assembler constructor change and production cutover committed atomically in this one unit; single-feed cutover confirmed; exactly three new rules; no Document authority |
| 4 | Scope Lock §11; Contract Design §14 | `tests/composition/ParkerRuntimeReasoningKnowledgeSourceCompositionTest.kt` (new) -- test-only, no production file | Section 9 | Unit Completion + Independent Constitutional | Unit 3 reviews accepted | Every Section 9 test passing against Unit 3's already-accepted production composition; this unit's own diff touches no production file; least-authority proof built from an independently constructed, real production object graph, with no reflection into, accessor on, or widened visibility into `ParkerRuntime`'s own private instance; Document-denial proof's own principal confirmed `ACTIVE` before either authorization assertion, both assertions genuinely decided by `DefaultPermissionPolicy`, no invented `resourceType` request field; real `InMemoryActionVocabulary`/`ActionMapper` constructed with the exact transcribed `READ`/`DOCUMENT` mapping, `vocabulary.lookup(...)` precondition and `actionMapper.map(...)` non-vacuity assertion both confirmed passing, so `UNKNOWN_ACTION`/`RESOURCE_TYPE_MISMATCH` are excluded as the cause of the observed `DENIED`; no claim that the real `DefaultPermissionPolicy` produces a mixed per-item outcome |
| 5 | Planning Review §11; Contract Design §14; Scope Lock §2 | `tests/composition/ParkerRuntimeReasoningContextIntegrationTest.kt` -- test-only | Section 10 | Unit Completion + Independent Constitutional | Unit 4 reviews accepted | Genuine end-to-end proof passing; authorized-empty proof passing; no synthetic substitution; no composed mixed-evidence negative companion attempted or reconstructed by any means |

---

## 22. Explicit Non-Claims

- This document implements nothing -- no Kotlin, no test, no configuration.
- It does not redesign, reinterpret, weaken, or reopen any Contract Design or Scope Lock decision --
  every frozen value in Sections 6-19 is transcribed, not chosen.
- It does not authorize any file, capability, or alternative beyond the locked file boundary (Section
  4).
- It does not begin implementation of any unit.
- It does not claim any unit has been completed, reviewed, or accepted.
- It does not claim conversational recall, restart durability, or programme closure, in any form.
- It does not claim constant-time execution, resistance to active timing analysis, or durable
  permission-decision auditing.
- It does not reopen Gap #54, which remains complete.
- It does not create or reserve a new numbered gap, or a new programme identity.
- It does not modify any of the four governing documents it reads from, or any Kotlin or test file.

---

## 23. Next Stage

Independent Completion Review and Independent Constitutional Review of this Implementation Plan
itself, by Steve and Codex. If accepted, Unit 1 (Section 6, above) may begin -- not authorized or
begun by this document.

```
KNOWLEDGE DISCOVERABILITY AND GOVERNED RETRIEVAL INTO REASONING CONTEXT
IMPLEMENTATION PLAN -- COMPLETE, PENDING REVIEW AND UNIT 1 AUTHORIZATION
```
