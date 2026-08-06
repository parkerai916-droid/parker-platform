# Programme 3 — Unit 9.6: Runtime Composition — Independent Constitutional Review

## Status

**Genuine Independent Constitutional Review**, performed as if by another reviewer, against the adopted governance re-read fresh, and against the actual, current file contents — not against the Completion Review's own summary of them. This document does not amend `src/composition/ParkerRuntime.kt`, `tests/composition/ParkerRuntimeKnowledgeRetrievalCompositionTest.kt`, the Unit 9.5 Clarification, the Unit 9 Contract Design, or any other governance document. It identifies conflict, or its absence, and states a determination.

---

## 1. Baseline Confirmation

`HEAD` is `ee2891994eec2b07d4e4e487778fc37c52f5af9f`, unchanged since implementation began. The working tree carries exactly the expected set: one modified file (`src/composition/ParkerRuntime.kt`) and two untracked files (the new composition test, the Completion Review). No other file is touched.

---

## 2. Scope and Method

This review re-reads the Unit 9 Knowledge Retrieval Implementation Plan's own Unit 9.6 entry, the Unit 9 Contract Design §10, and the adopted Unit 9.5 Clarification fresh, and checks `git diff -- src/composition/ParkerRuntime.kt` line-by-line against each. It independently re-derives the governance-prerequisite determination the Completion Review states, rather than accepting it. It re-reads the new test file in full and checks each test's own body against what its name claims to prove. Every quotation the diff's own new comments make of any governing document is checked word-for-word against its cited source — the same discipline that caught a real defect in each of the three immediately preceding Independent Constitutional Reviews this Programme has performed (Units 9.4, 9.5, and the Unit 9.5 Clarification itself).

---

## 3. Governance Prerequisite Determination — Independently Re-Derived

**Test:** does Unit 9.6 genuinely require no further governance, or did the Completion Review assert this without adequate support?

Re-derived independently: the Implementation Plan's own dependency graph (§5) names only "9.2 + 9.3 + 9.4 + 9.5, all complete" as Unit 9.6's precondition — no narrower Clarification is named, unlike Unit 9.5's own explicit gate. The Contract Design §10's own "Runtime owns" text, checked directly against its full sentence, already anticipates precisely composing-and-supplying-dependencies plus pre-call identity resolution as Runtime's entire remaining role once self-gating was chosen — and Unit 9.5 already chose self-gating. The apparent tension with Contract Design §11's own "Programme 3's own Unit 10 and/or a future Programme 4 act" placeholder resolves cleanly: that Explicit Exclusions table row predates the Implementation Plan and is superseded, not contradicted, by the Implementation Plan's own later, more granular "Unit 9.6" naming and its own explicit Programme 3/Programme 4 boundary statement. **Sound** — no additional governance prerequisite exists, confirmed independently, not merely accepted.

---

## 4. Additive-Only Discipline

**Test:** is the diff genuinely additive, and is the one disclosed exception (the corrected `knowledgeItemPersistence` comment) actually necessary, or could it have been avoided?

Checked directly via `git diff`: every changed block is either a pure addition (`+` lines only) or the one disclosed comment replacement. The replaced comment's own prior text — "never exposed for retrieval (Knowledge Memory's own read boundary onto Memory Core remains a distinct, not-yet-built unit...)" — would have been actively false the moment this diff landed (Knowledge Retrieval is now built and now shares this exact persistence instance). Leaving it would not have preserved "additive only" in any meaningful sense; it would have left a now-incorrect claim standing uncorrected in production code, the same "silent drift" this Programme's own governing discipline has repeatedly forbidden elsewhere. Correcting it, and disclosing the correction explicitly rather than folding it silently into "additive," is the more honest choice, not a violation of the Implementation Plan's own "no other existing composition line is altered — additive only" verification requirement, whose evident purpose (no *behavioural* line altered without disclosure) is fully honoured. **Sound.**

---

## 5. The READ/MEMORY Policy Rule — The One Disclosed Judgment Call, Independently Tested

**Test:** does adding `PermissionPolicyRule(READ, MEMORY, APPROVED, AUTOMATIC)` cross into "changing permission behaviour," which the governing task explicitly forbids?

This is the correct question to press hardest, since the Completion Review itself flags it as debatable rather than asserting it is obviously fine. Independently re-examined against the precedent it claims: `git show HEAD:src/composition/ParkerRuntime.kt` confirms the WRITE/MEMORY rule (Knowledge Submission), the EXECUTE/AGENT rule (Controlled Agent Run Submission), the WRITE/DOCUMENT and READ/DOCUMENT rules, and the EXECUTE/AGENT-adjacent Evidence Intelligence invocation rule were *each* added by the *same* composition-root unit that first made its own action reachable — none was deferred to a separate governance or policy-authoring step. This is not a single precedent but the *only* pattern this file has ever used, without exception, for every prior "make X reachable" unit. The rule added is the narrowest possible shape available (one already-existing `PermissionAction` combined with one already-existing `ResourceType`, `AUTOMATIC` for the same disclosed "no confirmation mechanism exists" reason every sibling rule already gives, no principal-specific or content-specific carve-out invented). It does not touch, wrap, weaken, or bypass either of `DefaultKnowledgeRetrieval`'s own two independently-evaluated gates (confirmed by `DefaultKnowledgeRetrieval.kt`'s own zero diff) — a principal still must pass both the act-level and the item-level gate on every call; this rule only supplies what the underlying `DefaultPermissionPolicy` needs to resolve either gate's `ExecutionRequest` to anything other than an unconditional, structural denial. **Sound** — this is composition using an existing, repository-established pattern, exactly as this task's own instruction required, not an invented policy decision.

---

## 6. Resource/Action Registration Correctness

**Test:** does the registration reuse Unit 9.5's own already-fixed identifiers without retyping or drifting from them?

Checked directly: both new registration stages reference `DefaultKnowledgeRetrieval.KNOWLEDGE_RETRIEVAL_RESOURCE_ID` and `DefaultKnowledgeRetrieval.RETRIEVE_ACTION_NAME` as compiled symbol references, never as re-typed string literals — structurally immune to transcription drift between the governed constant and its registration. The ActionVocabularyEntry's own mapping (`READ`/`MEMORY`) matches the `PermissionPolicyRule` added in Section 5, above, exactly — no mismatch between what the vocabulary maps an action to and what the policy actually approves for that same pair. **Sound.**

---

## 7. Dependency Sharing

**Test:** does `knowledgeRetrieval` genuinely reuse the same `knowledgeItemPersistence` and `permissionEngine` instances already composed, or does it construct parallel ones?

Checked directly: `knowledgeRetrieval = DefaultKnowledgeRetrieval(knowledgeItemPersistence, permissionEngine)` references the two local/field values already in scope at that point in `buildAndRegisterRuntimeGraph` — no `InMemoryKnowledgeItemPersistence()` or `DefaultPermissionEngine(...)` construction appears anywhere near this line. Confirmed additionally, not merely by code reading, by the new test suite's own `assertSame` assertions for both dependencies, both passing. **Sound.**

---

## 8. Unit Boundary

**Test:** does this diff wire Knowledge Retrieval to Reasoning Context, add any production entry point, or touch Memory Core/Evidence/World Model?

Checked directly: `knowledgeRetrieval` is a `private` field with no corresponding public `ParkerRuntime` method; no `ReasoningContextAssembler`, `ConversationEngine`, or Reasoning-Context-shaped class references it anywhere (confirmed both by direct reading and by the new suite's own "no Knowledge Retrieval dependency is reachable from the conversation coordinator chain" structural test, passing). No file under Memory Core, Evidence, or World Model's own paths appears in the diff. **Sound.**

---

## 9. Test Coverage Against the Task's Own Required List

Checked one-by-one: successful construction (present); dependency injection (present, both dependencies); retrieval available through runtime (present, a genuine stored item comes back through the real, composed instance); permission path preserved (present, both a denial and an approval case, against the *real* engine); lifecycle shaping preserved (present, both directions); staleness preserved (present, using the real system clock, not a fixed one — the only clock available through this path); construction failures where dependencies are absent (correctly identified as not applicable to this runtime's own composition style, matching the identical absence in the Evidence Intelligence precedent suite); regression coverage (present, both a full `submitEvidence` round-trip and a direct policy-resolution check for Knowledge Submission's own gate). **All eight categories genuinely, not nominally, covered.**

---

## Findings

### Finding 1 (Required Correction) — a fabricated quotation, splicing two separate sentences into one

**The defect.** `src/composition/ParkerRuntime.kt`'s own new "Knowledge Retrieval resource registration" comment reads, in part: *"The same fixed pair is named by both `DefaultKnowledgeRetrieval`'s own act-level and item-level gates (Unit 9.5's own Section 7: 'one resource identity, one action name... evaluated at two granularities, not two pairs') -- registering it once here suffices for both."*

Checked word-for-word against the adopted Clarification's own Section 7: no sentence reading "one resource identity, one action name... evaluated at two granularities, not two pairs" exists anywhere in it. What exists are two *separate* bullets: **"One pair, evaluated at two granularities, not two pairs."** (a bolded lead-in, followed by prose about the act-level/item-level gates sharing one pair) and, in a *different* bullet, **"One resource identity, one action name, evaluated identically for every query and every candidate item..."** The comment's own quotation marks splice the opening words of the second bullet onto the closing words of the first, presenting the result as one continuous quotation from "Section 7" — precisely the same defect class the Unit 9.4 Independent Constitutional Review found and corrected (a fabricated quotation blending two non-adjacent sentences), recurring here in a different file.

**Why this is a genuine defect, not pedantry.** The underlying claim — that Section 7 fixes one pair reused at both granularities — is true, and both real sentences independently support it; this is not a substantive misrepresentation of what governance requires. But presenting fabricated text inside quotation marks, attributed to a specific numbered section, fails this repository's own repeatedly-applied quotation-fidelity discipline the moment a reader checks it, exactly as the two prior instances this Programme has already caught and corrected this cycle.

**Required correction:** rewrite the parenthetical to either quote one of the two real sentences accurately, or drop the quotation marks and paraphrase both bullets' combined effect in the comment's own words, citing Section 7 without claiming a verbatim quotation that does not exist.

No other required correction was found. The governance-prerequisite determination, the additive-only discipline (including the one disclosed exception), the READ/MEMORY policy-rule judgment call, resource/action registration correctness, dependency sharing, the Unit boundary, and the full test-coverage picture are all confirmed sound.

---

## Constitutional Verdict

```
REQUIRES REVISION
```

One required correction — a fabricated-quotation defect confined to a single code comment, not a behavioural, architectural, or scope-boundary one. The governance-sufficiency determination, the composition wiring itself, the disclosed policy-rule judgment call, and the full verification picture are all confirmed sound and require no change.

---

## Recommended Next Step

Correct the one fabricated quotation in `src/composition/ParkerRuntime.kt`'s own new resource-registration comment; touch nothing else. A narrow Defect Confirmation Review follows, confirming the correction was applied precisely and that no regression was introduced, without repeating this full review.

---

## Final Git Status at Time of This Review

```
$ git status --short
 M src/composition/ParkerRuntime.kt
?? docs/reviews/PROGRAMME_3_UNIT_9_6_RUNTIME_COMPOSITION_COMPLETION_REVIEW.md
?? docs/reviews/PROGRAMME_3_UNIT_9_6_RUNTIME_COMPOSITION_INDEPENDENT_CONSTITUTIONAL_REVIEW.md
?? tests/composition/ParkerRuntimeKnowledgeRetrievalCompositionTest.kt
```

Nothing staged, committed, or pushed.
