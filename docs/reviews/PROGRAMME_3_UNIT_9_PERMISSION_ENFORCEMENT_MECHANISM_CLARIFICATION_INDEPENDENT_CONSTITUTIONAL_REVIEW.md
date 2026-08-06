# Programme 3 — Unit 9 Permission Enforcement Mechanism Clarification — Independent Constitutional Review

## Status

**Genuine Independent Constitutional Review**, performed as if by another reviewer, against the governing documents re-read fresh, not against the Clarification's own summary of them. This document does not amend the Clarification (`docs/governance/PROGRAMME_3_UNIT_9_PERMISSION_ENFORCEMENT_MECHANISM_CLARIFICATION.md`, "the Clarification"), Contract Design V2, the Scope Lock, the Unit 9 Contract Design, the Unit 9 Clarification, the Unit 8 Clarification, Chapter 10, or CDR-005. It identifies conflict, or its absence, and states a determination. No Kotlin is implemented, proposed, or changed. No `src/` or `tests/` file is touched. Nothing is staged, committed, or pushed.

---

## 1. Baseline Confirmation

`HEAD` is `8b929c223ba11af5df66da061373239002b12269`, unchanged since the Clarification was drafted. The working tree carries exactly one new file, the Clarification itself; no other file is touched. No production or test file is modified by either the Clarification or this review.

---

## 2. Scope and Method

This review reads the Clarification in full against: the Unit 9 Knowledge Retrieval Implementation Plan (Unit 9.5 entry, in full); the adopted Unit 9 Knowledge Retrieval Contract Design (§4, §5, §9, §11); the adopted Unit 9 Scope Lock Clarification (§6, §7, §8, §10, §12); Chapter 10 (in full); CDR-005 (in full, including its own Non-Decisions); the Unit 8 Scope Lock Clarification (in full, as the Clarification's own primary structural precedent); `src/interfaces/PermissionEngine.kt`; `src/contracts/ExecutionRequest.kt`; `src/interfaces/KnowledgeStore.kt` (`KnowledgeRetrievalQuery`, `KnowledgeResultEntry`, `KnowledgeRetrievalResult`, `KnowledgeRetrievalDisposition`, all currently frozen); `src/runtime/DefaultKnowledgeRetrieval.kt`; `src/runtime/DefaultKnowledgeSubmission.kt`; `src/runtime/DefaultEvidenceCustodian.kt`; `src/composition/PermissionFilteredMemoryRetrieval.kt`. Every quotation the Clarification makes of any of the above was independently checked, word-for-word, against its cited source — not accepted on the Clarification's own word, following the same discipline the immediately preceding Unit 9.4 Independent Constitutional Review applied and that itself found one genuine defect by.

---

## 3. Does the Clarification Reopen the Admission Classification?

**Test:** does the Clarification re-argue, re-derive, or weaken the Unit 9 Clarification's own positive PermissionEngine proposal-class classification for Knowledge Retrieval?

No. Section 1's own status block states this classification is "treated here as settled, frozen authority"; Section 13 explicitly declines to perform a fresh CDR-005 self-certification, citing CDR-005's own "Non-Decisions" list (checked directly against CDR-005's own text: "any implementation interface, Kotlin type, or method signature for PermissionEngine or any request representation... Runtime's own wiring or composition-root sequencing... the content of any permission policy" — confirmed present, verbatim, in CDR-005's own Non-Decisions section). The Clarification resolves mechanism only. **Sound.**

---

## 4. Does the Clarification Stay Within Its Own Governance Precondition?

**Test:** is the Clarification actually the document the Implementation Plan's Unit 9.5 entry requires, or does it exceed or fall short of that scope?

The Implementation Plan's Unit 9.5 entry, checked directly: *"Implement whichever enforcement mechanism — Knowledge Retrieval self-gating... or external gating by Runtime... — a prior, narrower, implementation-facing Unit 9 Scope Lock Clarification determines... This unit may not begin until that narrower Clarification exists and is adopted."* The Clarification determines exactly this — enforcement location — plus the mechanics that determination necessarily entails (granularity, resource/action disclosure, evaluation order, denial disposition, principal and correlation propagation), each of which the adopted Unit 9 Contract Design §5 or §9, or the adopted Unit 9 Clarification §8 or §12, independently and explicitly defers to this tier. No section resolves anything Contract Design §11's own Explicit Exclusions table reserves to a *later* tier (runtime composition, storage technology, Reasoning Context). **Sound.**

---

## 5. Enforcement Location — Self-Gating Determination

**Test:** does the self-gating conclusion (Section 6.1) follow from the cited precedent, or is it asserted?

Checked directly against the Unit 8 Clarification §5's own two-precedent test (multi-operation-surface-serving-many-callers → external; single-self-contained-boundary → self-gating) and the Unit 9 Contract Design §4's own text fixing "the retrieval interface" as "exactly one operation." `KnowledgeRetrieval` has exactly one public operation (confirmed directly in `src/interfaces/KnowledgeStore.kt`: `interface KnowledgeRetrieval { suspend fun retrieve(...) }`), matching the `EvidenceCustodian`/`DefaultKnowledgeSubmission` shape precisely. The Clarification does not merely assert this; it cites the adopted Unit 9 Clarification's own already-recorded observation ("reads closer to the first shape") as corroboration, correctly framed as corroboration rather than as the sole basis. **Sound.**

---

## 6. The Two-Tier Granularity Mechanism — Necessity, Not Invention

**Test:** is the two-tier (act-level plus item-level) mechanism actually compelled by governance, or is it a novel design choice dressed as compulsion — and is it within an ordinary Clarification's authority to resolve, or does it require CDR escalation?

Independently re-derived, not merely re-read: a pure per-query gate would leave every individual `KnowledgeItem`'s own disclosure ungated once a query is approved, contradicting the Unit 9 Clarification's own stated basis for classifying Knowledge Retrieval as a proposal class at all (anchored explicitly to per-item Memory-Core-evidence disclosure risk, checked word-for-word against Unit 9 Clarification §7). A pure per-item gate would make `KnowledgeRetrievalDisposition.NotAuthorised` (Unit 9.1, frozen, confirmed directly in `KnowledgeStore.kt`) permanently unreachable for any principal denied every matching item, since an all-filtered result is type-identical to a genuinely empty one — directly contradicting Contract Design §9's own binding text (checked word-for-word: *"A caller receiving an empty Knowledge Result must be able to trust that the query was valid, the data was available, and permission was granted — not merely infer this from the absence of an error"*). Neither tier alone satisfies both already-adopted constraints; both are independently, already-fixed obligations, not preferences the Clarification could have traded off differently. This is not a genuinely contested admission question (Section 3, above, confirms the admission itself is untouched) — it is a mechanism synthesis of two already-accepted repository patterns (`DefaultKnowledgeSubmission`'s stop-before-read discipline; `PermissionFilteredMemoryRetrieval`'s per-record filtering), exactly the kind of question the Unit 8 Clarification resolved for submission without CDR escalation, and CDR-005's own Decision Rules reserve escalation for admission-classification contests, not mechanism synthesis. **Sound**, and correctly not escalated.

---

## 7. Resource and Action Disclosure

**Test:** does reusing one fixed resource/action pair across both granularities understate the item-level disclosure risk the admission classification itself identified, or invent an unauthorised second identifier?

Checked against `PermissionFilteredMemoryRetrieval`'s own actual code (`src/composition/PermissionFilteredMemoryRetrieval.kt`): it evaluates the *same* fixed `RETRIEVE_ACTION_NAME` for every `Entity`, `Assertion`, and `Relationship` record, varying only by record *kind* (a second constant, `RETRIEVE_DOCUMENT_ACTION_NAME`, exists only for `Document`, not per individual record). The Clarification's choice to reuse one action name across every `KnowledgeItem`, varying only by *how many times* it is evaluated, is a faithful application of this precedent, not an understatement of it — the disclosure risk is captured by evaluation count and target (which record), never by the action string's own content, exactly as the precedent already establishes. Minting a second action name for the item-level tier would not have added any protection this design lacks; Section 7's own reasoning that doing so "would create a Knowledge Memory permission vocabulary broader than this document's own minimal, disclosed authority" is correct. **Sound**, subject to the cross-reference defect in Finding 1, below (a citation-location error, not a substantive one).

---

## 8. Evaluation Order, Count, and the Bounding-Placement Choice

**Test:** is the "permission-filter-before-bounding" choice (Section 8, step 6) actually disclosed as a choice, with its own cost stated, or presented as though compelled when it is not?

Checked directly: Section 8 states plainly, *"This ordering... is a disclosed design choice, not compelled by any single existing precedent, and its own reasoning is stated here rather than left implicit,"* and separately discloses its cost (*"an implementation may evaluate permission for more items than are ultimately returned"*) rather than concealing it. This is the correct discipline — the Clarification does not claim false compulsion for a choice that is, honestly, underdetermined by prior precedent, mirroring this Programme's own established practice (for example, Unit 9.3's own disclosed, non-compelled thirty-day threshold). The evaluation-count formula (`1` on act-level denial; `1 + N` on approval, `N` measured before bounding) is precise, internally consistent between Sections 6.2 and 8, and independently verifiable by a future test using the fake-`PermissionEngine`-with-call-counter pattern `DefaultKnowledgeSubmissionTest` already establishes. **Sound.**

---

## 9. Principal and Correlation Identifier Propagation

**Test:** is the claim that Knowledge Retrieval's own correlation-identifier treatment must differ from `DefaultKnowledgeSubmission`'s own precedent actually correct, or is a difference asserted where none exists?

Checked directly: `KnowledgeCandidate`'s own frozen field list (confirmed via Unit 5 Scope Lock Clarification's own frozen fields, restated correctly by the Clarification) carries no correlation identifier, so `DefaultKnowledgeSubmission` mints one (confirmed directly in `src/runtime/DefaultKnowledgeSubmission.kt`: `correlationId = "knowledge-submission-$id"`). `KnowledgeRetrievalQuery` does carry one, added specifically, per the Unit 9 Contract Design §4's own text (checked word-for-word: *"a Knowledge Query must additionally be capable of carrying an explicit correlation identifier, sufficient to correlate the retrieval request with its own permission evaluation... This identifier must be explicit, never ambient or inferred"*). The Clarification's freeze — reuse `query.correlationId` unchanged across every constructed `ExecutionRequest` for the same query, rather than minting a fresh one — is the only reading consistent with that already-adopted text; minting a fresh one per evaluation, as `DefaultKnowledgeSubmission` does, would silently discard a field the Contract Design added for exactly this correlating purpose. **Sound.**

---

## 10. Permission Denial Disposition — No New Type, No Exception

**Test:** does the Clarification introduce, or require, any change to `KnowledgeRetrievalDisposition`'s own frozen shape, or any exception-based denial path inconsistent with existing precedent?

Checked directly against `KnowledgeStore.kt`: `KnowledgeRetrievalDisposition` is confirmed to already carry exactly `Retrieved(result)` and `NotAuthorised(reason: String)`, with `reason` already required non-blank. The Clarification's mechanism uses both variants exactly as they already exist, introduces no third variant, and correctly identifies that item-level filtering requires no result-type change at all (mirroring `PermissionFilteredMemoryRetrieval`'s own precedent, correctly cited). No exception-based denial path is authorised anywhere in the Clarification; its explicit rejection of one is consistent with `DefaultKnowledgeSubmission`'s and `DefaultEvidenceCustodian`'s own identical, already-accepted discipline. **Sound.**

---

## 11. Unit Boundary — No Leakage Into 9.6, Runtime, or Reasoning Context

**Test:** does the Clarification authorise, anticipate, or perform any runtime composition, `ParkerRuntime.kt` change, or Reasoning Context integration?

Checked directly: Section 5 names runtime composition and Reasoning Context integration as explicitly outside the governed act; Section 11 (Explicit Non-Expansions) repeats this; Section 12 (Programme Boundary) restates it a third time. No section names a `ParkerRuntime.kt` line, a composition-root sequencing detail, or any Reasoning Context type. `git diff --name-only` confirms exactly one file, the Clarification itself, changed. **Sound.**

---

## 12. Freeze-Status Disclosure

**Test:** does the Clarification honestly disclose that it relies on Chapter 10 and CDR-005 while both remain Draft, or does it overstate their authority?

Checked word-for-word against both source documents' own status headers (Chapter 10: *"a draft, prepared to complete Chapter 10's own position... pending independent constitutional review and a Final Freeze Verification... It is not yet frozen"*; CDR-005: *"Draft. This record is not Accepted, not Canonical, and not Frozen..."*) — both quotations are accurate. Section 14 mirrors the Unit 8 Clarification's own identical disclosure discipline exactly, correctly extends the "disclosed exception" lineage to a fourth instance, and correctly states that completing that broader Final Freeze Verification task is not a precondition for this document's own validity, consistent with how the Unit 8 Clarification and the Unit 9 Clarification were each themselves treated. **Sound.**

---

## 13. General Consistency Check

Checked against the broader corpus for any conflict not already covered above: no inconsistency found with Contract Design V2, the Scope Lock, the Unit 4–8 Clarifications, or `docs/architecture/parker-constitution.md`. The Clarification modifies no existing governance document (confirmed by `git diff --name-only`: one new file only). It invents no new lifecycle state, ranking mechanism, or Permission Engine outcome.

---

## Findings

### Finding 1 (Required Correction) — a wrong section cross-reference

Section 7's own closing sentence reads: *"Inventing a second, distinct action name for the item-level tier would create a Knowledge Memory permission vocabulary broader than this document's own minimal, disclosed authority (Section 12)."* Section 12 of the Clarification is "Programme Boundary." The "Explicit Non-Expansions" section — the one actually naming this constraint ("any Knowledge Memory permission framework broader than the single resource/action pair...") — is **Section 11**, confirmed by direct re-count of the Clarification's own numbered headings. The parenthetical citation points to the wrong section.

**Required correction:** change `(Section 12)` to `(Section 11)` at this one location. No other text requires change; the substantive claim itself is correct (Section 6, above).

### Finding 2 (Required Correction) — two citations to non-existent subsection headings

Section 8, steps 2 and 6, cite "(Section 6.2.1)" and "(Section 6.2.2)" respectively. Section 6.2 of the Clarification ("Two granularities, not one") contains an unlabeled, un-subheaded numbered list — items "1." (the act-level gate) and "2." (the item-level gate) — not separately headed subsections `6.2.1`/`6.2.2`. No such headings exist anywhere in the document, confirmed by direct search. A reader following either citation would find nothing at the named location.

**Required correction:** replace both citations with a form that does not imply a non-existent heading — for example, "Section 6.2's own first item" and "Section 6.2's own second item," or by adding explicit `6.2.1`/`6.2.2` subheadings to the two list items so the existing citations resolve correctly. Either approach is acceptable; the substantive content pointed to is, in both cases, correctly identified in substance.

No other required correction was found. The two findings above are citation-location defects, not substantive or architectural ones — they do not affect the correctness of the self-gating determination, the two-tier mechanism, the resource/action disclosure, the evaluation order and count, the correlation-identifier treatment, the denial disposition, or the Unit boundary, all confirmed sound in Sections 3–12, above.

---

## Constitutional Verdict

```
REQUIRES REVISION
```

Two required corrections, both citation-location defects (a wrong section number; two references to non-existent subsection headings). No substantive, architectural, or constitutional defect was found in the enforcement-location determination, the two-tier granularity mechanism, the resource/action disclosure, the evaluation order and count, the principal and correlation-identifier treatment, the denial disposition, the Unit boundary, or the freeze-status disclosure.

---

## Recommended Next Step

Apply Findings 1 and 2 only — three text edits, no substantive change. A narrow Defect Confirmation Review follows, confirming both corrections were applied precisely, without repeating this full review. Only once that Defect Confirmation Review is complete should the Clarification be treated as adopted and Unit 9.5 implementation begin on the enforcement-mechanism dimension it settles.

---

## Final Git Status at Time of This Review

```
$ git status --short
?? docs/governance/PROGRAMME_3_UNIT_9_PERMISSION_ENFORCEMENT_MECHANISM_CLARIFICATION.md
?? docs/reviews/PROGRAMME_3_UNIT_9_PERMISSION_ENFORCEMENT_MECHANISM_CLARIFICATION_INDEPENDENT_CONSTITUTIONAL_REVIEW.md
```

Nothing staged, committed, or pushed.
