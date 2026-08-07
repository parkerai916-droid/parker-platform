# Memory Core Durability — Unit 8: Runtime Composition — Independent Constitutional Review

## Status

**Genuine Independent Constitutional Review**, performed as if by another reviewer, against the governing documents re-read fresh and against the actual, current file contents — not against the Completion Review's own summary of them. Performed against the state *after* the one correction recorded in the accompanying Defect Confirmation Review; this document does not itself amend `src/composition/ParkerRuntime.kt`, `src/composition/ParkerRuntimeConfig.kt`, any test file, the Completion Review, the Defect Confirmation Review, or any governance document.

---

## 1. Baseline and Diff-Shape Re-Verification

Independently re-confirmed: `git status --short` shows exactly `src/composition/ParkerRuntime.kt` and `src/composition/ParkerRuntimeConfig.kt` modified among production files, eight composition test files modified, one new test file, and three new review documents — `Dockerfile`, `docker-compose.yml`, and `.env.example` are **not** present in the status output, confirming the Defect Confirmation Review's own claimed revert actually took effect, not merely claimed. `git diff src/composition/ParkerRuntime.kt`, read in full, independently, shows exactly: two new imports, one removed import (`InMemoryMemoryCore`, now unused), one KDoc update, the single construction-site replacement (`InMemoryMemoryCore()` → `FileSystemMemoryCoreDurabilityLog` + `DurableMemoryCore.create`, both `stage()`-wrapped), and the two reference updates it necessitates (`permissionFilteredMemoryRetrieval`'s constructor argument; one comment's stale variable name). No line of `evidenceRegistrationCoordinator`'s or `evidenceIntelligenceAcceptanceCoordinator`'s own construction changed — both still read `memoryCore`, unchanged, now bound to a different value. **This independently confirms Unit 8's own completion criterion literally: "no line altered or reordered beyond the one construction-site replacement and the reference updates it necessitates."**

---

## 2. Re-Verification of the Defect Confirmation Review's Own Central Claim — the Most Important Question This Unit Raises

**Test:** was the Docker/compose change genuinely out of this Unit's own scope, or could the Defect Confirmation Review's own citation itself be the misreading?

Re-read the Implementation Plan's own Unit 8 section fresh, independently, in full: "**Explicit non-responsibilities.** Does not add a `PermissionGatedMemoryCore` wrapper. Does not change any method signature on `MemoryCore`/`MemoryRetrieval`. Does not touch Docker (Unit 9)." — an exact, unambiguous, three-sentence list, the third sentence naming Docker and naming Unit 9 by number in the same breath. Independently read the Plan's own separate "Unit 9 — Container Durability" section immediately following: its own "Inputs" line reads "`ParkerRuntimeConfig.memoryCoreDurabilityLogPath` (Unit 8)," confirming Unit 9 *consumes* Unit 8's own output rather than Unit 8 producing Unit 9's own Docker artifacts itself — a producer/consumer relationship between adjacent units, not a single unit spanning both. Cross-checked the task's own current instruction: "Do not begin Unit 9" appears verbatim. **The Defect Confirmation Review's determination is independently confirmed correct**, and the correction (reverting `Dockerfile`, `docker-compose.yml`, `.env.example`) was the right one, not an overcorrection — `.env.example`'s own revert is independently justified too, since it is named in neither Unit 8's nor Unit 9's own "files expected to change" lists, and its only stated purpose (documenting a Docker default) no longer existed once the Docker change itself was reverted.

---

## 3. Challenge — Does Reverting `Dockerfile`/`docker-compose.yml` Leave the Repository in a Self-Consistent State, or Does It Silently Break Something Unit 8 Itself Claims to Guarantee?

Traced directly: with `PARKER_MEMORY_CORE_DURABILITY_LOG_PATH` now a required key (`ParkerRuntimeConfigLoader.requireKey`) that `docker-compose.yml` never supplies, `docker compose up` today would fail at `ParkerRuntimeConfigLoader.load` with `MissingConfiguration`, before `ParkerRuntime.start()` is even reached. Checked whether this contradicts anything this Unit's own governing task requires: it does not — the task's own required behaviour is scoped to "the live Parker runtime" generally, verified by this Unit's own live-runtime verification (Section 6, Completion Review), which used a direct host process, not `docker compose up`; no requirement anywhere in this Unit's own task text, the Contract Design, or the Scope Lock obligates `docker compose up` to succeed as of this specific Unit. **This is the correct, disclosed, intended Unit 8→Unit 9 handoff state, not a silently-introduced regression** — and the Completion Review's own Section 9 discloses it explicitly rather than leaving a reader to discover it independently.

---

## 4. Challenge — Is the "No Double Gating" Resolution Genuinely Sound, Independent of the User's Own Confirmation, or Merely Deferred to Authority?

Re-derived independently, not merely re-cited: `EvidenceRegistrationCoordinator`'s own `register` method and `EvidenceIntelligenceAcceptanceCoordinator`'s own acceptance-dispatch method were read directly (not from memory) — both hold their own `permissionEngine: PermissionEngine` field and both call `permissionEngine.evaluate(...)` against their own specifically-named resource/action pairs before their own respective `MemoryCore` calls. A `PermissionGatedMemoryCore` wrapper placed around `memoryCore` in either constructor call would insert a *second*, structurally identical `permissionEngine.evaluate` call (per `PermissionGatedMemoryCore`'s own KDoc, read directly) between the coordinator's own already-passed gate and the actual `MemoryCore` mutation — two independent evaluations of what is, from the caller's own point of view, one logical write. This is a genuine, structural double-evaluation, not a hypothetical one. **Sound, independent of any citation — the resolution the user selected is the one a fresh, from-first-principles trace also arrives at.**

---

## 5. Challenge — Does `stage()`'s Own Exception Handling Actually Guarantee `FAILED`, Not `RUNNING`, on a Recovery Fault, or Was This Merely Asserted?

Traced the full call chain directly, independent of the test suite's own passing result: `DurableMemoryCore.create` (read directly) calls `MemoryCoreRecovery.recover`, which throws `MemoryCoreRecoveryException` (a plain `RuntimeException`, confirmed by its own class declaration) on any decode or referential-integrity fault. `stage()` (read directly, lines 1387–1395) catches `ParkerRuntimeException` and `CancellationException` specially, and wraps every other `Exception` — `MemoryCoreRecoveryException` included — as `ParkerRuntimeException.DependencyConstructionFailed`. `start()`'s own outer `try`/`catch` (read directly, lines 322–346) catches `ParkerRuntimeException`, sets `state = RuntimeLifecycleState.FAILED`, and rethrows, before `state = RuntimeLifecycleState.RUNNING` is ever reached (that assignment sits later in the same `try` block, never executed once an earlier step throws). **Sound, confirmed by direct code trace, not merely by the fact that a test asserting this happened to pass.**

---

## 6. Challenge — Is the Live-Verification Gap Finding Accurate, or Does It Overstate/Understate What Was Found?

Independently re-checked the specific claim "no path from conversation to a `MemoryCore` write exists anywhere in this runtime": grepped `MemoryCore\b` against `ConversationTurnReasoningCoordinator.kt`, `CommunicationConversationCoordinator.kt`, `ConversationReplyCoordinator.kt`, and `ResponseComposer.kt` directly — zero matches in all four, independently reproduced. Separately confirmed no `Tool` implementation anywhere in `src/runtime/*.kt` references `MemoryCore` (`grep -rln MemoryCore src/runtime/*.kt | xargs grep -l Tool` — empty). This is not an overstatement: the finding is scoped precisely to "conversation," not to the runtime as a whole, and `EvidenceRegistrationCoordinator`/`EvidenceIntelligenceAcceptanceCoordinator` are correctly identified elsewhere in the same review as the two paths that *do* reach `MemoryCore`, just not from conversation. Nor is it an understatement dressed up as a bigger claim than warranted — the review does not claim Memory Core writing is entirely unreachable, only that the specific conversational path the task's own acceptance criterion assumes does not exist. **Accurate, and independently reproducible.**

---

## 7. Challenge — Was the Alternate Durability Proof (Two Independent `ParkerRuntime` Instances, Same Log Path) Genuinely Free of Any Shared-State Shortcut That Could Make It Pass Falsely?

Checked directly for any static or companion-object state anywhere in the durability stack that could let a second `DurableMemoryCore.create` call observe the first instance's own in-memory state without genuinely reading the shared file: `DurableMemoryCore`'s own companion `object` (read directly) declares only the `create` factory function, no stored state; `InMemoryMemoryCore` (read directly) has no companion object at all; `FileSystemMemoryCoreDurabilityLog` holds only its own `logFile: Path` and a per-instance `Mutex`. Two separate `ParkerRuntime` instances, two separate `FileSystemMemoryCoreDurabilityLog` instances, two separate `DurableMemoryCore` instances — the only channel between them is the file on disk, read fresh by the second instance's own `MemoryCoreRecovery.recover` call. **The test genuinely exercises file-backed persistence, not object-graph sharing.**

---

## 8. Full, Independent Quotation Audit

| Quoted fragment | Cited source | Verified |
| --- | --- | --- |
| "Does not touch Docker (Unit 9)" | Implementation Plan, Unit 8, Explicit non-responsibilities | Exact match, re-verified independently in Section 2, above. |
| "SHALL NOT introduce double permission gating on any write path already gated internally by its own caller" | Scope Lock §18 | Exact match, confirmed by direct re-read of the current document text. |
| "no `PermissionGatedMemoryCore` wrapper, since each caller already self-gates" | Implementation Plan, Unit 8, Outputs (the Plan's own paraphrase of the pre-existing `ParkerRuntime.kt` comment reasoning) | **Correction found during this audit**: this exact phrase is the Plan's own paraphrase, not a literal quotation from `ParkerRuntime.kt` itself. The pre-edit comment's own actual words (confirmed against `git diff`'s removed-lines content) are "each already gate their own MemoryCore writes internally, so wrapping either's raw memoryCore dependency in PermissionGatedMemoryCore would double-gate an already-gated call" — the same reasoning, genuinely reaffirmed by this Unit's own edited comment, but a paraphrase, not a verbatim quotation of the code comment. Neither the Completion nor Defect Confirmation Review actually quotes this phrase as a code quotation (the Completion Review's own diagram uses it only as informal shorthand); this row exists to record that the phrase's precise origin was checked and corrected during this audit, not to flag a defect in either review's own body text. |
| "load-before-`RUNNING`" | Completion Review's own paraphrase of this composition root's own established `stage()` discipline, not a direct quotation from any governing document | Correctly presented as description, not quotation — no quotation marks used for it in the reviewed text; consistent with the established convention in this session of distinguishing paraphrase from quotation. |

No further quoted fragment appears in the Completion Review or Defect Confirmation Review beyond ordinary code identifiers. **No defect found.**

---

## 9. Challenge — Did Any Work Belonging to a Different, Already-Completed Unit (1–7) Leak In, or Was Anything Beyond Unit 8's Own Scope Implemented Besides the Already-Corrected Docker Defect?

Checked directly: `git diff --stat` across every modified file shows no change to any file under `src/runtime/` (Units 1–7's own files: `DurableMemoryCoreEntry.kt`, `FileSystemMemoryCoreDurabilityLog.kt`, `DurableMemoryCore.kt`, `InMemoryMemoryCore.kt`) — confirmed empty. No change to `MemoryCore.kt`/`MemoryRetrieval.kt` (the interfaces) — confirmed empty, independently satisfying the Plan's own "Does not change any method signature" non-responsibility. No `PermissionGatedMemoryCore` construction anywhere in the diff — confirmed by direct text search of the full diff for the string, zero occurrences. **Sound, and — beyond the one already-found-and-corrected Docker defect — no further out-of-scope work was found.**

---

## Findings

One genuine defect was found and corrected prior to this review (an unauthorised Docker/compose change reserved for Unit 9); this review independently re-confirms both that the defect was real (Section 2) and that the correction was complete and left the repository self-consistent (Section 3). No further defect was found. The central engineering claims this Unit rests on — correct `FAILED`-not-`RUNNING` behaviour on recovery failure (Section 5), the soundness of the no-double-gating resolution independent of citation (Section 4), the accuracy of the live-verification gap finding (Section 6), and the genuineness of the alternate durability proof (Section 7) — are each independently re-derived from first principles, not merely re-accepted from the Completion Review's own account.

---

## Constitutional Verdict

```
ACCEPTED
```

No further correction required.

---

## Recommended Next Step

Per this task's own explicit stop point: work halts here. Unit 9 (Container Durability) is not begun. Nothing is staged, committed, or pushed.

---

## Final Git Status at Time of This Review

```
$ git status --short
 M src/composition/ParkerRuntime.kt
 M src/composition/ParkerRuntimeConfig.kt
 M tests/composition/ParkerRuntimeConfigLoaderTest.kt
 M tests/composition/ParkerRuntimeConversationPipelineTest.kt
 M tests/composition/ParkerRuntimeEvidenceCustodianIntegrationTest.kt
 M tests/composition/ParkerRuntimeEvidenceIntelligenceCompositionTest.kt
 M tests/composition/ParkerRuntimeFailureHandlingTest.kt
 M tests/composition/ParkerRuntimeKnowledgeRetrievalCompositionTest.kt
 M tests/composition/ParkerRuntimeReasoningContextIntegrationTest.kt
 M tests/composition/ParkerRuntimeStartupAndShutdownTest.kt
?? docs/reviews/MEMORY_CORE_DURABILITY_UNIT_8_RUNTIME_COMPOSITION_COMPLETION_REVIEW.md
?? docs/reviews/MEMORY_CORE_DURABILITY_UNIT_8_RUNTIME_COMPOSITION_DEFECT_CONFIRMATION_REVIEW.md
?? docs/reviews/MEMORY_CORE_DURABILITY_UNIT_8_RUNTIME_COMPOSITION_INDEPENDENT_CONSTITUTIONAL_REVIEW.md
?? tests/composition/ParkerRuntimeMemoryCoreDurabilityCompositionTest.kt
```

Nothing staged, committed, or pushed.
