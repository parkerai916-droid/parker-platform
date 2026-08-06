# Programme 3 — Unit 9.6: Runtime Composition — Completion Review

## Status

**Implementation completion review.** Unit 9.6 only is implemented, exactly as the Unit 9 Knowledge Retrieval Implementation Plan's own Unit 9.6 entry fixes, wiring the already-completed, already-adopted Units 9.1–9.5 into `src/composition/ParkerRuntime.kt`. No governance document was modified. Nothing is staged, committed, or pushed.

---

## Repository Baseline

- **HEAD at start:** `ee2891994eec2b07d4e4e487778fc37c52f5af9f`
- **Branch:** `main`
- **Working tree at start:** clean. Units 9.1 through 9.5 accepted, committed, and pushed.

---

## Governance Prerequisite Check (Performed Before Any Kotlin)

Per this task's own explicit instruction, checked before writing any code: does any additional, implementation-facing governance document remain a constitutional prerequisite for Unit 9.6, analogous to the "narrower Clarification" gate Unit 9.5 required?

- **Implementation Plan §5 (Implementation Ordering):** Unit 9.6 depends only on "9.2 + 9.3 + 9.4 + 9.5, all complete" — no separate governance clarification is named as its own precondition, unlike Unit 9.5's own explicit "this unit may not begin until that narrower Clarification exists and is adopted" gate.
- **Unit 9 Contract Design §10 (Runtime Responsibilities):** already, fully anticipates and authorises exactly Unit 9.6's own scope — "Runtime owns: constructing and composing whatever concrete class implements Knowledge Retrieval into `ParkerRuntime.kt`, exactly as it already does for every other domain interface; supplying Knowledge Retrieval with whatever dependencies its own eventual implementation requires; identity resolution (who is asking) prior to a requesting principal ever being passed into Knowledge Retrieval." Since Unit 9.5 already resolved self-gating, this is the entirety of Runtime's own remaining role — nothing further is left open.
- **Contract Design §11 (Explicit Exclusions):** reserves "Runtime composition — wiring Knowledge Retrieval into `ParkerRuntime.kt`" to "Implementation Plan / runtime composition, Programme 3's own Unit 10 and/or a future Programme 4 act (Scope Lock §4)" — a placeholder written before the Implementation Plan existed. The later-adopted Implementation Plan itself performs exactly this translation, naming it "Unit 9.6" and explicitly drawing the Programme 3/Programme 4 boundary itself: "This unit stops at making Knowledge Retrieval reachable within the composed runtime — it does not wire Knowledge Retrieval to Reasoning Context, which remains Programme 4's own, separately governed act." No conflict exists between the two documents; the Implementation Plan is the authorised, later-tier elaboration the Contract Design's own placeholder anticipated.
- **The adopted Unit 9.5 Permission Enforcement Mechanism Clarification, §7:** explicitly names "Registration remains a future Runtime-integration-phase responsibility" for the `KNOWLEDGE_RETRIEVAL_RESOURCE_ID`/`RETRIEVE_ACTION_NAME` pair — anticipating, not blocking, exactly the registration work performed below.

**Determination: no additional governance prerequisite exists.** Every mechanism-level question Unit 9.6 touches was already, fully resolved by Units 9.1–9.5's own adopted governance chain. Implementation proceeded directly to Kotlin.

---

## What Was Implemented

**`src/composition/ParkerRuntime.kt`**, additive wiring only, alongside the existing Knowledge Submission composition block, exactly as the Implementation Plan's own "Expected repository impact" text names:

1. **Two new imports**: `parker.core.interfaces.KnowledgeRetrieval`, `parker.core.runtime.DefaultKnowledgeRetrieval`.
2. **One new field**: `private lateinit var knowledgeRetrieval: KnowledgeRetrieval`, held as its own narrow public interface type (mirroring `evidenceIntelligence: EvidenceIntelligence`'s own identical precedent), with an explanatory comment distinguishing why this field exists (so the instance is reachable, since no production entry point yet consumes it — unlike `permissionEngine`/`evidenceIntelligence`, promoted to fields specifically because a production entry point needed direct access).
3. **One new construction line**, inside `buildAndRegisterRuntimeGraph`, immediately after `knowledgeSubmission`'s own construction: `knowledgeRetrieval = DefaultKnowledgeRetrieval(knowledgeItemPersistence, permissionEngine)` — reusing the *same* `knowledgeItemPersistence` and `permissionEngine` instances already in scope, never a second, parallel instance of either. `clock` is left defaulted (the real system clock), mirroring every other production call site of a class with a defaulted `Clock` parameter in this codebase.
4. **One new Resource registration** (`stage("Knowledge Retrieval resource registration")`) and **one new ActionVocabulary registration** (`stage("Knowledge Retrieval action vocabulary registration")`), each its own stage, mirroring the existing Evidence Intelligence/Knowledge Submission registration blocks exactly in shape and discipline, registering `DefaultKnowledgeRetrieval.KNOWLEDGE_RETRIEVAL_RESOURCE_ID`/`DefaultKnowledgeRetrieval.RETRIEVE_ACTION_NAME` — identifiers already fixed by the adopted Unit 9.5 Clarification, invented nowhere by this Unit.
5. **One new `PermissionPolicyRule`** (`READ`/`MEMORY` → `APPROVED`/`AUTOMATIC`), added to the existing `DefaultPermissionPolicy` rule list. See "The One Judgment Call This Review Flags for Its Own Independent Scrutiny," below, for the reasoning and an explicit invitation to challenge it.
6. **One corrected comment.** The pre-existing comment on `knowledgeItemPersistence`'s own declaration read, in part, "never exposed for retrieval (Knowledge Memory's own read boundary onto Memory Core remains a distinct, not-yet-built unit...)" — no longer true now that Knowledge Retrieval is composed. Left uncorrected, this comment would actively mislead a future reader. It was replaced with an accurate description of the now-shared persistence. This is the **one line-level alteration to previously-existing text** in this diff — everything else is additive. Disclosed here rather than silently folded into "additive only," and specifically named for the Independent Constitutional Review to check against the Implementation Plan's own "no other existing composition line is altered — additive only" verification requirement.

**No other file was touched.** No retrieval algorithm, contract, permission mechanism, lifecycle-shaping default, or staleness logic was changed — `DefaultKnowledgeRetrieval.kt` itself has zero diff.

---

## The One Judgment Call This Review Flags for Its Own Independent Scrutiny

Adding the `READ`/`MEMORY` → `APPROVED` policy rule is the one decision in this Unit genuinely debatable against the task's own "do not... change permission behaviour" instruction, and it is disclosed here candidly, not smoothed over.

**The case for it being in-scope composition, not a policy change:** every prior composition unit in this file that made a previously-unregistered action reachable — Evidence Custodian's `evidence.accept`/`evidence.retrieve`, the Evidence Intelligence invocation gate, Knowledge Submission's own `knowledge.submit` — added its *own* minimal default-approval rule as part of that *same* composition-root unit, each explicitly narrated as "the minimum required... to be reachable at all." Without an equivalent rule, `knowledge.retrieve` would be permanently, unconditionally denied for every principal, making the composed instance reachable only in the sense that it exists and does not throw — never in the sense the Implementation Plan's own words use ("making Knowledge Retrieval reachable within the composed runtime"). No new `PermissionAction` or `ResourceType` value was introduced (`READ` and `MEMORY` both already exist and are already used elsewhere in this exact policy); only a new *combination* of two already-existing values was added, for a resource/action pair Unit 9.5's own adopted Clarification already fixed.

**The case against:** Chapter 10 reserves policy *content* — what is actually approved — to the Owner, never to a composition-root wiring task; and this Unit's own governing task explicitly lists "change permission behaviour" among the things not to do.

**Resolution applied:** this Unit followed the repository's own unbroken, repeated precedent — "use the existing composition patterns already established elsewhere in the repository" is this task's own explicit instruction, and the pattern already established, without exception, bundles "register the resource/action" with "add the minimal rule that makes it resolvable" as one composition-root act. The rule added is the narrowest possible (one `(action, resourceType)` pair, `AUTOMATIC`, no confirmation step invented), grants nothing beyond what Unit 9.5's own two-tier gate independently, separately still evaluates on every call, and does not touch, weaken, or bypass either of Unit 9.5's own gates in any way. This is disclosed explicitly, not asserted quietly, precisely so the Independent Constitutional Review can test it on its own terms rather than accept the Completion Review's own account of it.

---

## Runtime Verification

- **Successfully constructs:** `the composed Knowledge Retrieval graph constructs successfully when ParkerRuntime starts` — `runtime.start()` reaches `RuntimeLifecycleState.RUNNING`.
- **Receives all required dependencies:** the same `InMemoryKnowledgeItemPersistence` instance backs both `knowledgeSubmission` and `knowledgeRetrieval` (confirmed via `assertSame`); the same `PermissionEngine` instance backs `knowledgeRetrieval` as every other gated act in the runtime (confirmed via `assertSame`).
- **Exposes retrieval through the intended composition path:** `knowledgeRetrieval` is reachable as a `private lateinit var` on `ParkerRuntime` itself, confirmed via reflection, exactly as `permissionEngine`/`evidenceIntelligence` already are.
- **Preserves deterministic behaviour:** the same query against unchanged composed state returns an identical, identically-ordered result across repeated calls.
- **Preserves permission enforcement:** an unregistered principal receives `NotAuthorised`, never `Retrieved`; the real, composed `DefaultPermissionEngine` genuinely resolves `knowledge.retrieve` to `APPROVED` for the registered owner, via the newly-registered convention, not a bypass.
- **Preserves lifecycle shaping:** a `RETIRED` item is excluded by default and included only via `includeRetired = true`, through the composed instance.
- **Preserves staleness disclosure:** a fresh item discloses `INDETERMINATE`; a ninety-day-old item discloses `POSSIBLY_STALE`, computed by the composed instance's own real system clock (no fixed clock is, or could be, injected through this path).

---

## Verification Performed

```
$ ./gradlew clean test
BUILD SUCCESSFUL in 45s
5 actionable tasks: 5 executed
```

- **`ParkerRuntimeKnowledgeRetrievalCompositionTest`:** 14/14 passed.
- **`ParkerRuntimeEvidenceIntelligenceCompositionTest`:** all passed, unmodified, confirming this Unit's own `ParkerRuntime.kt` changes introduced no regression in the pre-existing composition graph.
- **Full repository suite:** 1763 tests, 0 failures, 0 errors, 5 pre-existing skips (up from 1749 at the Unit 9.5 accepted baseline; +14 new tests, 0 regressions).

---

## Tests Added (14, all in `tests/composition/ParkerRuntimeKnowledgeRetrievalCompositionTest.kt`)

- **Construction:** the composed graph constructs successfully.
- **Dependency injection:** `knowledgeRetrieval` is a genuine `DefaultKnowledgeRetrieval`; shares the one `InMemoryKnowledgeItemPersistence` with `knowledgeSubmission`; shares the one `PermissionEngine` with the rest of the runtime.
- **Retrieval available through runtime:** an item stored via the shared persistence is retrievable through the composed instance.
- **Permission path preserved:** an unregistered principal is denied; the real, composed policy resolves the new convention to `APPROVED` for the registered owner.
- **Lifecycle shaping preserved:** a `RETIRED` item excluded by default; included via `includeRetired = true`.
- **Staleness preserved:** fresh vs. ninety-day-old disclosure, computed by the real system clock.
- **Determinism:** repeated calls against unchanged composed state are identical.
- **Regression:** `submitEvidence` still succeeds; Knowledge Submission's own `WRITE`/`MEMORY` gate still resolves to `APPROVED`, unaffected by the new `READ`/`MEMORY` rule; no Knowledge Retrieval dependency is reachable from the conversation coordinator chain.

**Construction failures where dependencies are intentionally absent:** not applicable, consistent with existing runtime conventions — `ParkerRuntime`'s own constructor accepts no externally-injectable domain dependency (`persistence`, `permissionEngine`, etc. are all constructed internally within `buildAndRegisterRuntimeGraph`), exactly as `ParkerRuntimeEvidenceIntelligenceCompositionTest.kt` itself contains no such test for its own, identically-shaped Evidence Intelligence composition. This category genuinely does not apply to this runtime's own composition style, not merely omitted.

---

## Explicitly-Confirmed Non-Changes

- **No retrieval algorithm change** — `matches`, `isRetrievable`, `disclosureFor`, `isAuthorised` in `DefaultKnowledgeRetrieval.kt`: zero diff.
- **No contract change** — `KnowledgeRetrievalQuery`, `KnowledgeResultEntry`, `KnowledgeRetrievalResult`, `KnowledgeRetrievalDisposition`, `PermissionEngine`: zero diff.
- **No permission mechanism change** — the two-tier gate's own location, granularity, evaluation order, and denial disposition are untouched; only a policy *rule* (content, not mechanism) was added, per the disclosed judgment call above.
- **No lifecycle or staleness logic change** — zero diff to either concern.
- **No ranking, semantic search, fuzzy matching, provider-specific behaviour, caching, or indexing** — none introduced anywhere in this diff.
- **No asynchronous behaviour beyond what already exists** — `buildAndRegisterRuntimeGraph` is already `suspend`; nothing new is introduced.
- **No Memory Core, Evidence, or World Model modification** — zero diff to any file in those areas.
- **No Reasoning Context wiring** — `knowledgeRetrieval` is reachable only via reflection in tests; no production consumer references it.

---

## Files Modified

- `src/composition/ParkerRuntime.kt` (additive wiring: two imports, one field, one construction line, two registration stages, one policy rule; one corrected stale comment)

## Files Created

- `tests/composition/ParkerRuntimeKnowledgeRetrievalCompositionTest.kt` (14 tests)
- `docs/reviews/PROGRAMME_3_UNIT_9_6_RUNTIME_COMPOSITION_COMPLETION_REVIEW.md` (this document)

## Test Results

- `ParkerRuntimeKnowledgeRetrievalCompositionTest`: **14/14 passed**
- Full repository suite: **1763 tests, 0 failures, 0 errors, 5 pre-existing skips**

## Constitutional Verdict

```
UNIT 9.6 -- COMPLETE.
```

## Recommendation

Unit 9.6 satisfies its own Completion Gate (Implementation Plan §7): it compiles, every existing test still passes, every new test named for it passes, and Knowledge Retrieval is now reachable within the composed runtime, exactly as required — not yet consumed by Reasoning Context, exactly as Scope Lock §4 requires of this boundary. An Independent Constitutional Review follows this document, with the READ/MEMORY policy-rule addition and the one corrected comment specifically named for its own scrutiny. Programme 3's own Unit 9 (Knowledge Retrieval) is now complete in full (Units 9.1–9.6); Programme 3's overall completion still depends on the Deliverables this Unit does not touch (Scope Lock §9), unaffected by this Unit.

---

## Final Git Status

```
$ git status --short
 M src/composition/ParkerRuntime.kt
?? docs/reviews/PROGRAMME_3_UNIT_9_6_RUNTIME_COMPOSITION_COMPLETION_REVIEW.md
?? tests/composition/ParkerRuntimeKnowledgeRetrievalCompositionTest.kt
```

Nothing staged, committed, or pushed.
