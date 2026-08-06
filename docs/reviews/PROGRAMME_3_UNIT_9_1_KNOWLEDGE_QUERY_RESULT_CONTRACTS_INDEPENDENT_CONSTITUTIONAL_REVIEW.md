# Programme 3 Unit 9.1 — Knowledge Query / Result Contracts — Independent Constitutional Review

## 1. Status

**Independent constitutional review only.** No production code or test was modified during this review. Unit 9.2 or any later Unit 9 work was not begun. Nothing is staged, committed, or pushed. This review does not rely on `docs/reviews/PROGRAMME_3_UNIT_9_1_KNOWLEDGE_QUERY_RESULT_CONTRACTS_COMPLETION_REVIEW.md`'s own account — every claim below was independently re-verified against the actual diff, the actual test file, and primary governance text.

---

## 2. Repository Baseline

- **HEAD:** `0e1dc58c037042b96802ead7336fd3b305fe50f3` (`0e1dc58`)
- **Branch:** `main`
- **Working tree, confirmed before this review began:**
  ```
   M src/interfaces/KnowledgeStore.kt
  ?? docs/reviews/PROGRAMME_3_UNIT_9_1_KNOWLEDGE_QUERY_RESULT_CONTRACTS_COMPLETION_REVIEW.md
  ?? tests/contracts/KnowledgeRetrievalContractsTest.kt
  ```
  Exactly as expected. No discrepancy.
- **Staged changes:** none.

---

## 3. Authorities Reviewed

Read fresh for this review: `docs/governance/PROGRAMME_3_UNIT_9_SCOPE_LOCK_CLARIFICATION.md` (Adopted, full); `docs/governance/PROGRAMME_3_UNIT_9_KNOWLEDGE_RETRIEVAL_CONTRACT_DESIGN.md` (Adopted, full — §2, §4, §5, §6, §7, §9 read exactly); `docs/governance/PROGRAMME_3_UNIT_9_KNOWLEDGE_RETRIEVAL_IMPLEMENTATION_PLAN.md` (§4, Unit 9.1's own objective, dependencies, repository impact, and verification requirements, read exactly); `docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_CONTRACT_DESIGN_V2.md` (full); `docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_SCOPE_LOCK.md` (full); `docs/decisions/CDR-005_CONSTITUTIONAL_ADMISSION_OF_PERMISSIONENGINE_PROPOSAL_CLASSES.md` and `docs/architecture/10-permission-engine.md` (both full); `src/interfaces/KnowledgeStore.kt`'s own committed diff (`git diff`, read exactly, not merely the completed file); `tests/contracts/KnowledgeRetrievalContractsTest.kt` (full); `docs/reviews/PROGRAMME_3_UNIT_9_1_KNOWLEDGE_QUERY_RESULT_CONTRACTS_COMPLETION_REVIEW.md` (full, treated as a claim to test, not a source of truth); repository precedent re-verified directly: `src/runtime/DefaultKnowledgeSubmission.kt`, `tests/contracts/KnowledgeSubmissionScopeTest.kt`, `tests/contracts/KnowledgeLifecycleEventTest.kt`, and `MEMORY_CORE_CONTRACT_DESIGN_ERRATA_003.md`/`_004.md`'s own thrown-exception precedent, all previously read in full this session and re-confirmed here against the specific claims the Completion Review makes about them.

---

## 4. Unit Boundary Review

Confirmed directly from the diff: exactly five declarations added, all in `src/interfaces/KnowledgeStore.kt`, all plain data holders or a bare, unimplemented interface. `KnowledgeRetrieval` has no implementing class anywhere in `src/` — confirmed by the absence of any new file under `src/runtime/` or `src/composition/` in the working tree. No `PermissionEngine` reference, no `MemoryRetrieval`/`MemoryCore` reference, no ranking logic, no staleness computation, no lifecycle filtering, and no `ParkerRuntime.kt` change exist anywhere in the diff — `git diff --stat` confirms only `src/interfaces/KnowledgeStore.kt` and the new test file changed. Nothing began prematurely.

---

## 5. Public Contract Review

Requesting principal (explicit parameter, never on the payload), correlation identity (`correlationId`, satisfying the Contract Design §4's own "query identity" requirement — the two are the same concept in the adopted Contract Design's own text, not two separate fields), structural matching criteria (`relevance`), provenance disclosure (via the bundled `KnowledgeItem.provenanceReference`, unmodified), and lifecycle/supersession disclosure (via the bundled `KnowledgeItem.status`/`.history`, unmodified) are each represented. "Result identity" is satisfied at entry level via each `KnowledgeItem.knowledgeId`; no document requires a separate, whole-result identity, and none was invented. "Deterministic contract expectations" are correctly out of this Unit's own scope — determinism is a behavioural property of the retrieval engine (Contract Design §8), not something a type declaration can express, and Unit 9.1 introduces no code that could violate or claim it either way.

**One genuine premature freeze found.** `KnowledgeResultEntry.stale: Boolean` fixes staleness's own expressiveness to a binary signal before Unit 9.3 (the not-yet-begun staleness-detection-mechanism unit) has had any occasion to determine whether a richer representation (a reason, a computed-at timestamp, a confidence) is needed. This differs materially from the `KnowledgeItem`-versus-`KnowledgeReference` choice the Completion Review also discloses: bundling the fuller `KnowledgeItem` type is non-breaking to narrow or supplement later, while narrowing or widening a already-shipped `Boolean` field is a breaking change to this Unit's own public contract. Neither the code's own KDoc nor the Completion Review discloses whether `Boolean` is intended as final or as a revisable placeholder Unit 9.3 remains free to widen.

---

## 6. Error Model Review

The five governed outcomes, and where each is represented, verified directly against Contract Design §9's own text and the actual code:

| Outcome | Representation | Verified |
| --- | --- | --- |
| Invalid query | `KnowledgeRetrievalQuery`'s own construction-time `require` checks (thrown `IllegalArgumentException`) | Directly tested (3 tests) |
| Unavailable data | Not yet represented in code — reserved, by KDoc, for a future thrown exception once a concrete `retrieve()` implementation exists | Not testable at this tier; not tested |
| Permission denial | `KnowledgeRetrievalDisposition.NotAuthorised` | Directly tested (construction validation; distinctness from `Retrieved`) |
| Empty result | `KnowledgeRetrievalDisposition.Retrieved(KnowledgeRetrievalResult(emptyList()))` | Directly tested, including distinctness from `NotAuthorised` |
| Implementation failure | Not yet represented in code — reserved, by KDoc, for a future thrown exception | Not testable at this tier; not tested |

**Tested directly against the adopted Contract Design, not accepted by analogy alone, per this review's own instruction.** Contract Design §9 states explicitly: "Not fixed here: any exception type, sealed result type, field, or status code." This is a direct, textual authorisation for representing some outcomes via exception and others via a sealed type — choosing two sealed variants plus exception-based propagation for the remaining three does not narrow this requirement; it exercises latitude the Contract Design itself grants. Invalid query and implementation/storage failure remain reliably distinguishable by call-site position alone (construction-time failure occurs before a query object can exist at all; a runtime failure occurs only after a valid query was already accepted by `retrieve()`) — sound, though it depends on a caller structuring separate handling around construction versus the call itself, a Kotlin idiom this repository has never required a dedicated exception hierarchy to enforce elsewhere (Errata 003's own "thrown, never silently repaired" precedent settles for the same). Permission denial can never be confused with an empty authorised result — directly tested, not merely argued.

**Was Unit 9.1 authorised to decide this representation?** Yes, unambiguously — the adopted Implementation Plan's own Unit 9.1 objective text assigns exactly this: "a Knowledge Result capable of... expressing the five Error Model outcomes... as distinguishable from one another." This is not an encroachment on Unit 9.2's own territory; it is Unit 9.1's own charter.

**Two disclosure gaps found, neither a defect in the code's own correctness:**

1. The 2-variant `KnowledgeRetrievalDisposition` design, while authorised and Contract-Design-consistent, is presented as though settled rather than as a revisable choice. If Unit 9.2's own concrete engineering later shows a genuine need for a third variant (for example, to let a caller distinguish transient store-unavailability from a hard fault without inspecting exception types), extending this sealed type is a breaking change to a contract Unit 9.1 has now shipped — this forward risk is not disclosed anywhere.
2. The Completion Review's own claim that the exception-based outcomes "mirror `InMemoryMemoryCore`'s and `DefaultKnowledgeSubmission`'s own shared... discipline" is more specific than what those classes actually establish. Both were re-checked directly: neither contains a genuinely matching per-call, runtime "store unavailable" exception precedent — what both actually establish is the more general "no `try`/`catch`, faults propagate uncaught" discipline, which does support this Unit's choice, but is not the same as a directly matching precedent for this specific outcome. A minor overstatement of specificity, not a false claim.

---

## 7. Permission Boundary Review

Confirmed: explicit principal handling is preserved (a required, separate parameter to `retrieve`, never a field on `KnowledgeRetrievalQuery` — directly tested by the structural "no `requestingPrincipalId`-shaped property" assertion). No self-authorisation exists — no code path anywhere in this Unit makes, simulates, or assumes a permission decision. No resource identifier, action string, or enforcement mechanic is fixed — confirmed by direct search of the diff; none appears. The public contract is capable of carrying the adopted classification into a future Unit 9.5: the `requestingPrincipalId` parameter and the `NotAuthorised` variant together provide exactly the shape a future enforcement mechanism needs to populate, mirroring `KnowledgeSubmission`'s own identical pre-implementation shape. No defect found.

---

## 8. Lifecycle and Supersession Review

Confirmed directly: because `KnowledgeResultEntry` bundles the full `KnowledgeItem` rather than a stripped projection, every lifecycle distinction — `status` (active/retired), and the complete, unfiltered `history` (promotion, revision, retirement, restoration events, including any multi-hop supersession chain recorded as a sequence of revision events within it) — passes through this Unit's own types entirely unmodified and unfiltered. Multi-hop chain retrievability is preserved by construction, not by any logic this Unit adds, since nothing in this Unit touches, truncates, or projects `.history` at all. No "latest only" semantics are invented — there is no filtering or selection logic anywhere in this Unit, since query execution itself is Unit 9.2's own, later responsibility. No new lifecycle state or event kind is introduced — confirmed by direct inspection; the existing two-value `KnowledgeItemStatus` and four event kinds are untouched. This section's own findings reinforce, rather than undercut, the Public Contract Review's discussion of the `KnowledgeItem`-versus-`KnowledgeReference` choice: bundling the fuller type is precisely what makes this section's own guarantees trivially, structurally true. No defect found.

---

## 9. Provenance Review

Confirmed: every disclosed provenance reference is `KnowledgeItem.provenanceReference`, already existing, never constructed anew by this Unit — no code path anywhere in the diff constructs a `ProvenanceReference` value. No result implies truth, reliability, evidential authority, or acceptance — `KnowledgeItem.evidentialState` is carried through unmodified, remaining subject to Contract Design V2's own unchanged "never a truth determination" discipline. No defect found.

---

## 10. Structural Boundary Review

Confirmed by direct search of the diff for each named dependency: no reference to `MemoryCore`, `MemoryRetrieval`, any `PermissionEngine` implementation, Evidence Intelligence, Reasoning Context, `ParkerRuntime`, any persistence technology, any ranking algorithm, or any staleness engine exists anywhere in this Unit's five new declarations. `MemoryCoreRecordReference` appears only as the type of `KnowledgeItem.evidenceReference` — a field this Unit's own new types never touch, declare, or hold independently; it arrives already attached to the `KnowledgeItem` values this Unit merely bundles. No defect found.

---

## 11. Test Quality Review

18 tests, all independently re-run and confirmed passing. None enforces behaviour beyond what governance already requires — each traces to a specific Contract Design or Implementation Plan clause. The reflection-based structural tests (`declaredMemberProperties`, `declaredFunctions`) operate on compiled type declarations, not raw source text — these are genuinely structural checks, not naive whole-file substring searches, and mirror `KnowledgeSubmissionScopeTest.kt`'s own already-accepted technique exactly. The "exhaustive `when` compiles" test's own runtime assertions are close to vacuous in isolation, but its real value — a compile-time closure proof that would break the build if a third variant were ever added without updating this test — is genuine and directly mirrors `KnowledgeLifecycleEventTest.kt`'s own identical, already-accepted pattern; not a defect.

**One coverage gap confirmed, unavoidable at this tier but under-disclosed.** No test exercises "unavailable data" or "implementation failure" distinguishability, because no concrete `retrieve()` implementation exists yet to throw from — this is correctly outside what Unit 9.1 can test, not a defect in the test suite's own design. The gap is in disclosure: neither the test file's own header KDoc nor the Completion Review states plainly that only 2 of the 5 Error Model outcomes are actually exercised by a runtime test at this tier, leaving a reader to assume broader coverage than exists.

---

## 12. Findings

| # | Severity | Finding |
| --- | --- | --- |
| 1 | Moderate | `KnowledgeResultEntry.stale: Boolean` fixes staleness's own representation to a binary signal before Unit 9.3 has had any occasion to determine whether a richer shape is needed; widening it later is a breaking change this Unit does not disclose as a risk. |
| 2 | Moderate | The 2-variant `KnowledgeRetrievalDisposition` is presented as settled rather than disclosed as revisable; extending it later, if Unit 9.2's engineering shows genuine need, is a breaking change to an already-shipped public contract, not flagged anywhere. |
| 3 | Minor | The Completion Review's precedent claim for exception-based outcomes is more specific than `InMemoryMemoryCore`/`DefaultKnowledgeSubmission` actually establish — both support the general "faults propagate" discipline, not a directly matching "store unavailable" precedent. |
| 4 | Minor | Neither the code nor the Completion Review discloses that only 2 of the 5 Error Model outcomes are exercised by a runtime test at this tier; the remaining three (invalid query is in fact tested — properly 2 of 5 are not: unavailable data, implementation failure) rest on KDoc reasoning alone, necessarily. |
| 5 | None (confirmed sound) | Unit boundary discipline, permission-boundary shape, lifecycle/supersession preservation, provenance handling, and all eight named structural-boundary exclusions are each independently verified and found correct without qualification. |

---

## 13. Required Corrections

Two corrections are required before full acceptance, both documentation-only — no code correction is required, since nothing found above is incorrect, only under-disclosed:

1. **Add a KDoc note to `KnowledgeResultEntry.stale`** stating explicitly whether `Boolean` is the final representation or a placeholder Unit 9.3 remains free to widen, and if the latter, that doing so is understood in advance as an authorised, non-breaking-in-intent revision rather than a silent contract change.
2. **Add a note to `KnowledgeRetrievalDisposition`'s own KDoc (or the Completion Review)** disclosing that its 2-variant shape is a reasoned, authorised choice, not an immutable one — extending it later, should Unit 9.2 show genuine need for a third variant, requires its own disclosed amendment, not a silent addition.

Findings 3 and 4 are recommended clarifications, not required corrections — neither affects the correctness of what was built, only the precision of how it was described.

---

## 14. Constitutional Verdict

```
REQUIRES REVISION
```

Every structural, permission-boundary, lifecycle, provenance, and unit-scope guarantee this review tested was independently confirmed sound — nothing built exceeds Unit 9.1's own authorised scope, and nothing built contradicts the adopted Contract Design or Clarification. What blocks full acceptance is narrower: two forward-looking design commitments (the `Boolean` staleness representation and the closed 2-variant disposition) that are individually defensible and authorised but are not disclosed as the revisable, forward-risk-bearing choices they actually are. Both corrections are additive documentation, not architectural rework.

---

## 15. Recommended Next Step

Add the two required KDoc/documentation corrections directly, then request a narrow defect-confirmation review — not a full re-review — verifying only that the two disclosures were added and that nothing else changed, mirroring this repository's own established "narrow correction pass, then defect-confirmation review" pattern already used repeatedly this session. Only after that confirmation should Unit 9.1 be treated as fully accepted, and only then should Unit 9.2 (Deterministic Retrieval Engine) begin.

---

## 16. Git Confirmations

- No production code or test was modified during this review.
- Unit 9.2 and no later Unit 9 work was begun.
- Nothing was staged during this review.
- Nothing was committed during this review.
- Nothing was pushed during this review.

## 17. Final Git Status

```
$ git status --short
 M src/interfaces/KnowledgeStore.kt
?? docs/reviews/PROGRAMME_3_UNIT_9_1_KNOWLEDGE_QUERY_RESULT_CONTRACTS_COMPLETION_REVIEW.md
?? docs/reviews/PROGRAMME_3_UNIT_9_1_KNOWLEDGE_QUERY_RESULT_CONTRACTS_INDEPENDENT_CONSTITUTIONAL_REVIEW.md
?? tests/contracts/KnowledgeRetrievalContractsTest.kt
```
