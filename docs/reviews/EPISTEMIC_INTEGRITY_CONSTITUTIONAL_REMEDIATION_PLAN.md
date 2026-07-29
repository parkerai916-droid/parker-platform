**Status:** Governance — Constitutional Remediation Plan. Sequences implementation work required to bring Parker into compliance with Constitutional Amendment No. 1 — Epistemic Integrity, Version 1.0. Does not itself modify production code, tests, or the ratified instrument. Nothing is staged, committed, or pushed by this document.

# Epistemic Integrity — Constitutional Remediation Plan

Programme: **Programme 2 — Constitutional Remediation Plan.**

**Normative inputs, frozen, not redefined:** `docs/architecture/epistemic-integrity.md` (the ratified instrument), `docs/reviews/EPISTEMIC_INTEGRITY_EXECUTABLE_TEST_COMPLIANCE_AUDIT.md` (the authoritative statement of current compliance), `docs/reviews/EPISTEMIC_INTEGRITY_CONSTITUTIONAL_AUDIT_V0_3.md`, `docs/reviews/EPISTEMIC_INTEGRITY_CONSTITUTIONAL_REGISTER.md`, `docs/reviews/EPISTEMIC_INTEGRITY_RATIFICATION_RECORD.md`, `docs/reviews/EPISTEMIC_INTEGRITY_V0_4_RATIFICATION_SOURCE.md`. This plan does not reopen any constitutional decision those documents already settled. It also treats `docs/architecture/MEMORY_CORE_GOVERNANCE_REVIEW.md`, `MEMORY_ARCHITECTURE_RECONCILIATION.md`, `MEMORY_CORE_CONTRACT_DESIGN.md`, `MEMORY_CORE_SCOPE_LOCK.md`, and `docs/implementation/MEMORY_CORE_IMPLEMENTATION_PLAN.md` as frozen architectural context for Memory Core — this plan does not redesign, revise, or reopen any layering, permission-boundary, or scope decision those documents already made, including the permanent, explicit exclusion of any World Model dependency on or from Memory Core (Reconciliation §10; Scope Lock §4, §15).

This is not a redesign and not a second audit. It is the roadmap by which the findings of the Executable Test Compliance Audit are closed over time.

---

## 1. Executive Summary

**Current constitutional state.** Constitutional Amendment No. 1 — Epistemic Integrity, Version 1.0, is ratified and constitutionally frozen. The codebase it governs predates the Amendment and was not built against its vocabulary. The Compliance Audit found 0 of 69 Constitutional Tests FULLY ENFORCED, 8 PARTIALLY ENFORCED, 3 COMPATIBLE BUT UNTESTED, 52 NOT IMPLEMENTED, 4 CONFLICTING TEST OR BEHAVIOUR, and 2 NOT YET APPLICABLE.

**Audit outcome.** The audit's Final Classification is **CONSTITUTIONAL NON-COMPLIANCE IDENTIFIED**, driven by four genuine, tested, load-bearing conflicts in the World Model, and by the structural finding that Memory Core — the one subsystem that already embodies strong evidential discipline — sits outside the live reasoning pipeline, which instead runs on the legacy `MemoryStore`. This plan treats both findings as authoritative and does not re-litigate them.

**Overall remediation strategy.** Remediation proceeds in four tiers, in this order: (1) stop the bleeding — correct or explicitly quarantine the four active conflicts so no new capability is built on top of behaviour that contradicts the Constitution; (2) finish what is already in flight — close the Memory Core partial/untested findings, since Memory Core (Programme 2) is already scope-locked and its implementation plan is marked `READY FOR IMPLEMENTATION`; (3) build the foundation the remaining 52 NOT IMPLEMENTED findings depend on — principally the Article IV evidential-state taxonomy, without which most reasoning- and representation-layer obligations cannot be meaningfully tested; (4) extend compliance outward, Programme by Programme, as each subsystem (Knowledge Memory, Reasoning Context, World Model, Document Intelligence, Representation Engine) is itself built or revised, never pulling a finding's remediation earlier than the Programme it naturally belongs to. No remediation item in this plan proposes weakening any Article, and no remediation item proposes routing World Model through Memory Core or vice versa — that boundary is frozen, permanent architecture, not an open question this plan may revisit.

---

## 2. Classification of Findings

Findings are classified by *when* their remediation should occur, not by audit category. A finding's audit classification (Section 5 of the compliance audit) determines *what kind* of work remains; this section determines *when*.

### Immediate — before significant new platform capability

| Finding | CT(s) | Reason |
| --- | --- | --- |
| World Model: weaker competing observation rejected/discarded | CT-EI-05, 24 | Active, tested, load-bearing conflict; building new capability on the World Model now (e.g. Programme 5 work, or any consumer of `WorldBelief`) would inherit and compound the conflict |
| World Model: required non-nullable confidence forcing false precision | CT-EI-56 | Same reasoning; a public contract shape, changing it later is more disruptive the more callers exist |
| World Model: single-scalar confidence as sole update gate | CT-EI-48 | Same reasoning |
| World Model: belief overwrite with no history, source timestamp discarded | CT-EI-58, 59 | Same reasoning |
| Existing tests that lock in the four conflicts (Register A) | — | Must be corrected in lockstep with the conflicts themselves; leaving them in place after a fix would silently reintroduce the conflict as the "proven" behaviour |
| Memory Core / reasoning-pipeline disconnection (structural finding, Compliance Audit §15 Finding 1) | CT-EI-27, 36, and the false-positive risk itself | Not itself a single CT, but the audit's single highest-value finding; every Programme built on top of "Parker's memory is constitutionally governed" while the disconnection persists risks compounding a false positive. Immediate here means *disclosed and scheduled*, not *fixed now* — the fix itself is Medium-term (Section 9), since it depends on Knowledge Memory (Programme 3) existing first |

### Near-term — should be completed during Programme 2

| Finding | CT(s) | Reason |
| --- | --- | --- |
| Provenance inspectable at retrieval | CT-EI-27 | Memory Core is Programme 2's own subject; this is a test-only gap against already-built structure |
| Provenance preserved where reasonably practical | CT-EI-28 | Same |
| Original source distinguished from derivative (`ContentNature`) | CT-EI-29, 31 | Same |
| Provenance defects/unknowns disclosed | CT-EI-30 | Same |
| Transformation history preserved | CT-EI-32 | Compatible but untested; test-only |
| Evidential integrity cannot be established, disclosed as a conclusion | CT-EI-39 | Test-only, sharpens an existing passing test |
| Temporal concepts distinguished (acquisition/ingestion/claimed) | CT-EI-40 | Partial; the missing piece (amendment/disclosure timestamps) is a small, additive field |
| Integrity verifiers preserved | CT-EI-38 | Compatible but untested; test-only |
| Correction preserves original and reason for revision | CT-EI-63 | Compatible but untested; requires only a combined test, no new capability |
| Register B new tests (9 items, Section 11) | Various | All close gaps in capability that already exists inside Memory Core |

### Medium-term — naturally belongs in later Programmes

| Finding | CT(s) | Programme |
| --- | --- | --- |
| Article IV fourteen-state evidential taxonomy as a first-class type | CT-EI-02, 03, 04, 55 | Knowledge Memory (Programme 3), since the taxonomy must live where evaluated knowledge is represented, not inside Memory Core's raw system-of-record layer |
| MemoryStore → Knowledge Memory rename and Memory-Core-backed reconstruction | Structural precondition for CT-EI-27, 29, 36 reaching real consumers | Knowledge Memory (Programme 3) — already the Reconciliation's own planned next step |
| Staged wiring of Reasoning Context onto Knowledge Memory instead of legacy `MemoryStore` | CT-EI-01, 27, 36, 54 | Reasoning Context (Programme 4) |
| Article V propositional-integrity examination stage | CT-EI-06, 08–16, 66 | Reasoning Context (Programme 4) |
| Article VII burden-of-justification computation | CT-EI-17–26, 64, 68 | Reasoning Context (Programme 4), depends on the Article IV taxonomy |
| Article IX representation-layer obligations (authenticity vs. truth, extract-vs-source disclosure) | CT-EI-36, 37, 67 | Representation Engine (Programme 7), depends on Article IV |
| Article XI independence/corroboration tracking | CT-EI-49 | Knowledge Memory (Programme 3) / World Model (Programme 5), split by subsystem (Section 8) |
| World Model structural remediation (the four conflicts' actual implementation) | CT-EI-05, 24, 48, 56, 58, 59 | World Model (Programme 5) |

### Long-term — depends on capabilities not yet present

| Finding | CT(s) | Reason |
| --- | --- | --- |
| OCR/transcription/translation transformation disclosure | CT-EI-33, 35 | No document-transformation capability exists; belongs to a future Document Intelligence Programme (Programme 6) |
| Preservation of original source material | CT-EI-34 | Same — depends on `Document` gaining real content-fetch/preservation capability, a Document Intelligence concern |
| Contemporaneity and capture-type distinctions | CT-EI-41–47 | Depend on a capture-type taxonomy that does not exist in any subsystem yet; naturally arrives with Document Intelligence and/or World Model observation-capture work |
| Negative evidence reasoning | CT-EI-50–53 | An entirely new reasoning capability with no present analogue anywhere in the codebase |
| Explanatory confidence ("principal evidence supporting/limiting confidence") | CT-EI-57 | Depends on Article IV and Article V existing first |
| Retrospective-strengthening prevention, at-the-time justification explanation | CT-EI-61, 62 | Depend on Article XVI history-retention existing first (itself Medium-term for World Model, but the *explanation* obligation is a Representation Engine concern layered on top) |
| Narrative/framing evolution preservation following dispute | CT-EI-60 | New capability, no current analogue |
| Full Article XV bypass-prevention gates | CT-EI-64–68 (residual, beyond what Medium-term already resolves) | Depend on Article IV, V, VII, and IX all existing first — this is a capstone, not a starting point |

---

## 3. Conflict Remediation

All four CONFLICTING TEST OR BEHAVIOUR findings are in the World Model. Each is treated individually below; Section 8 sequences their combined implementation.

### CT-EI-05 / CT-EI-24 — Competing propositions collapsed to one belief

- **Subsystem:** World Model (`InMemoryWorldModel`, `DefaultWorldModelUpdatePolicy`).
- **Architectural cause:** `WorldModel` was designed around "current, replaceable belief about present reality" (`docs/architecture/WORLD_MODEL_RUNTIME_ARCHITECTURE.md`) — a single-belief-per-subject model, adopted before the Amendment existed and without an evidential-integrity requirement to preserve unresolved competing propositions.
- **Constitutional articles affected:** Article IV (evidential representation must preserve competing explanations where evidence does not justify exclusivity), Article VII (burden of justification requires preserving competing propositions where neither is discharged).
- **Recommended remediation:** additive, not destructive — introduce an explicit "unresolved" outcome alongside `Accepted`/`Rejected` in `WorldModelUpdatePolicy`'s decision type, and allow `WorldModel` to retain a bounded set of competing observations per subject when a policy returns "unresolved," rather than forcing a single winner. This extends the existing policy-seam pattern (already used for `Accepted`/`Rejected`) rather than replacing it.
- **Estimated implementation unit:** Medium — one new decision outcome, one storage-shape change (`beliefs[subject]` becomes capable of holding more than one entry only when unresolved), no change to the `Accepted`/`Rejected` paths that already behave correctly.
- **Estimated executable tests required:** 4–6 (decision-outcome construction, storage of an unresolved pair, retrieval exposing both, and a corrected version of the existing rejection test).

### CT-EI-56 — Required non-nullable confidence forces false precision

- **Subsystem:** World Model (`WorldBelief`, `WorldObservation`).
- **Architectural cause:** `confidence: Double` was specified as mandatory in the original World Model contract design, before any requirement existed to express "uncertainty that cannot be meaningfully quantified." Memory Core's `Assertion.confidence: Double?` shows the nullable pattern was already available elsewhere in the codebase and simply wasn't applied here.
- **Constitutional articles affected:** Article XIII (transparency of uncertainty; no false precision).
- **Recommended remediation:** make `confidence` nullable on both `WorldBelief` and `WorldObservation`, mirroring Memory Core's existing precedent exactly — an additive, low-risk change to a field's type, not a redesign of either type.
- **Estimated implementation unit:** Small — a type-signature change plus every call site's null-handling.
- **Estimated executable tests required:** 3–4 (null construction accepted, null does not crash comparison/update logic, existing non-null behaviour unchanged).

### CT-EI-48 — Single-scalar confidence as sole update gate

- **Subsystem:** World Model (`DefaultWorldModelUpdatePolicy`).
- **Architectural cause:** the update policy was designed around one comparison (`confidence >= existing.confidence`) as its entire decision surface — a reasonable minimal seam at the time, but one that now functions as exactly the "single factor... automatically determine[s]" pattern Article XI §1 prohibits absent an express governing-rule declaration.
- **Constitutional articles affected:** Article XI (evidential weight and independence).
- **Recommended remediation:** two acceptable paths, either satisfies the Constitution — (a) extend the policy to weigh provenance, contemporaneity, and independence alongside confidence (requires CT-EI-49's independence-tracking work first, see Section 8), or (b) retain the single-factor comparison but explicitly document and test it as a declared governing rule under Article XI §1's own exception clause. Recommendation: (a), because a documented exception is a permanent compliance liability that must be re-justified indefinitely, while (b) [a genuine multi-factor policy] is buildable once CT-EI-49 exists and is a one-time cost.
- **Estimated implementation unit:** Medium, and dependent on CT-EI-49 (Section 8 sequences this dependency explicitly).
- **Estimated executable tests required:** 5–7 (one per weighing factor, plus at least one adversarial case where confidence alone would have chosen wrongly).

### CT-EI-58 / CT-EI-59 — Belief overwrite discards history and source timestamp

- **Subsystem:** World Model (`InMemoryWorldModel.observe()`).
- **Architectural cause:** `beliefs[subject]` is a flat map entry, replaced on every `Accepted` transition, with the authoritative timestamp always set to acceptance time — a deliberate anti-spoofing design decision (never trust a source-reported time) that has the unintended side effect of discarding the observation's own claimed time entirely, and of retaining no prior-belief history at all.
- **Constitutional articles affected:** Article XVI (temporal integrity — revision without erasure), Article X (contemporaneity — distinguishing acquisition/claimed/authoritative time).
- **Recommended remediation:** retain both timestamps (authoritative acceptance time *and* the observation's own claimed time, as two distinct fields — this does not weaken the anti-spoofing decision, since the authoritative timestamp remains authoritative for ordering; it only stops discarding the second, distinct fact). Separately, retain a bounded prior-belief history (e.g. the immediately preceding belief per subject) rather than none at all — additive to the existing map-based storage, not a replacement of it.
- **Estimated implementation unit:** Medium — two additive fields plus one bounded-history retention mechanism.
- **Estimated executable tests required:** 4–5 (claimed timestamp preserved and distinguishable from authoritative timestamp; prior belief retrievable after overwrite; history bound enforced; existing anti-spoofing test unchanged).

---

## 4. Partial Enforcement Remediation

All 8 PARTIALLY ENFORCED findings concern Memory Core except CT-EI-54 (Reasoning Context). Memory Core Version 1 is already scope-locked and its implementation plan is `READY FOR IMPLEMENTATION` (`docs/implementation/MEMORY_CORE_IMPLEMENTATION_PLAN.md`) — these findings are gaps in *proof*, not in *capability*, for the seven of eight that concern Memory Core.

| CT | What already exists | What remains missing | Implementation sequence | Verification sequence |
| --- | --- | --- | --- | --- |
| 27 | Mandatory `Provenance` reference enforced and tested at creation | Nothing proves provenance survives a full retrieval round trip | None — test-only | Add a round-trip retrieval test (Section 11, Register B item 2) |
| 28 | `Provenance` immutable data class, mandatory-field validation tested | Same as CT-EI-27 | None — test-only | Same test covers both |
| 29 | `ContentNature` five-value enum, tested for exact values | No test maps `ContentNature` explicitly onto Article IX's original/derivative vocabulary | None — test-only | Add an explicit mapping/documentation test (Register B item — see Section 6, this is folded into the Article IX alignment work) |
| 30 | Nullable fields, `ContentNature.UNKNOWN` tested as valid | Disclosure is structural (field absence) only; no test frames it as an active disclosed conclusion | None — test-only | Add a test asserting the absence is surfaced as a conclusion, not merely permitted (Register B) |
| 31 | `ContentNature` values constructible and tested | Not framed against Article IX's specific vocabulary | Documentation/alignment note plus a mapping test | Same test as CT-EI-29 |
| 39 | Nullable `integrityInformation`, `ContentNature.UNKNOWN` | No test frames "integrity cannot be established" as an explicit disclosed conclusion rather than an absent field | None — test-only | Register B item |
| 40 | `acquisitionTime`, `ingestionTime` (server-assigned), `claimedCreationTime` (nullable) — three of five temporal concepts | No "amended" or "disclosed" timestamp field | Small additive field pair on `Provenance` | New field-presence and field-behaviour tests |
| 54 | `DefaultReasoningContextAssembler` renders confidence honestly, never fabricates when absent (tested) | Nothing traces that honesty through to the free-text `Reply`/`Goal` a user receives | Depends on Article IV/structured-response work (Medium-term, Section 9) | End-to-end test once `ReasoningProviderResponse` carries a structured field (deferred to Register C) |

Seven of eight partial findings require no production-code change at all — only new tests against structure that already exists. This is the lowest-risk, highest-leverage remediation category in this plan and should be completed first among Near-term work, ahead of CT-EI-40's small additive field and well ahead of CT-EI-54's dependency on Medium-term work.

---

## 5. Compatible-but-Untested Remediation

| CT | Existing implementation | Missing executable proof | Required tests | Expected implementation changes |
| --- | --- | --- | --- | --- |
| 32 | `Provenance.processingHistory: List<String>`, `derivedFrom: List<ProvenanceId>`, both default to empty | No test constructs a populated chain and proves it round-trips through creation and retrieval | One construction test with a populated chain, one retrieval round-trip test | None expected — fields already exist and are already typed correctly |
| 38 | `Provenance.integrityInformation: String?`, validated (rejects blank-when-present) | No test proves a real integrity-verification value is preserved or distinguishable from "not checked" | One populated-value round-trip test; one test distinguishing null ("not evaluated") from a genuine failed-verification value once that state exists | Possibly — if "verification failed" needs to be distinguishable from "not evaluated," a third state may be needed (flagged, not decided, in Register B) |
| 63 | `transitionStatus` (status-only), `AMENDS`/`SUPERSEDES` relationship types documented as the intended correction-linking mechanism | No test proves `transitionStatus` and a linking `Relationship` are actually used together in one correction workflow | One combined test: create a superseding Assertion, transition the original's status, link both via an `AMENDS`/`SUPERSEDES` `Relationship`, and verify both records and the link are independently retrievable | None expected — this exercises existing capability end-to-end, it does not add capability |

None of the three items in this category requires new production capability. All three are closeable by writing tests against structure Memory Core Version 1 already provides, and should be scheduled alongside the Section 4 items as Near-term, Programme 2 work.

---

## 6. Not-Implemented Register

The 52 NOT IMPLEMENTED findings are grouped by the future subsystem that naturally owns them, per the layering principle (do not force work into Memory Core because it exists today).

| Group | CTs | Future subsystem | Future Programme | Dependencies | Estimated order |
| --- | --- | --- | --- | --- | --- |
| Evidential-state taxonomy | 02, 03, 04, 55 | Knowledge Memory | Programme 3 | None — foundational | 1st (unlocks most other groups) |
| Materiality and disclosure of reliance on exceptions | 01, 06, 07 | Reasoning Context / Representation Layer | Programme 4 / 7 | Evidential-state taxonomy | 2nd |
| Propositional integrity (framing, decomposition, reformulation) | 08–16, 66 | Reasoning Context | Programme 4 | Evidential-state taxonomy | 2nd |
| Evidential sufficiency (plural-conclusion handling) | 17 | Reasoning Context / World Model | Programme 4 / 5 | Evidential-state taxonomy; World Model conflict remediation (Section 3) | 3rd |
| Burden of justification | 18–26, 64, 68 | Reasoning Context | Programme 4 | Evidential-state taxonomy | 3rd |
| Evidential integrity in representation (authenticity vs. truth, extract disclosure) | 34, 35, 36, 37, 67 | Representation Engine (34–35 partly Document Intelligence) | Programme 7 (34–35: Programme 6) | Evidential-state taxonomy; `ContentNature` alignment (Section 4) | 4th |
| Contemporaneity and capture-type distinctions | 41–47 | World Model / Document Intelligence | Programme 5 / 6 | A capture-type taxonomy (new, shared concept) | 4th |
| Evidential weight and independence | 49 | Knowledge Memory / World Model | Programme 3 / 5 | None — additive to `Relationship` (Knowledge Memory side) and to update-policy weighing (World Model side, see CT-EI-48) | 2nd (Knowledge Memory side), 3rd (World Model side) |
| Negative evidence | 50–53 | Reasoning Context | Programme 4 | Evidential-state taxonomy | 5th (new capability, no existing analogue) |
| Confidence explanation | 57 | Representation Engine | Programme 7 | Evidential-state taxonomy; propositional integrity | 5th |
| Temporal integrity — evidence-at-the-time preservation, retrospective-strengthening prevention, at-the-time explanation | 59, 61, 62 | World Model / Representation Engine | Programme 5 / 7 | World Model conflict remediation (history retention, Section 3) | 4th |
| Narrative evolution preservation following dispute | 60 | Knowledge Memory | Programme 3 | `Relationship` types (`DISPUTES`) already exist; needs a narrative-history concept | 4th |
| Constitutional separation bypass-prevention gates (residual) | 64, 65, 68 | Cross-cutting (Reasoning Context enforcement point) | Programme 4 | Evidential-state taxonomy, burden-of-justification, evidential-integrity classification all existing first | 6th, capstone |

This register converts the audit's flat list into a roadmap: the evidential-state taxonomy is the single highest-leverage piece of new capability in this entire plan, since 4 of the 13 groups above (and the majority of individual CTs) cite it as a direct dependency.

---

## 7. Memory Core Implications

**Already complete** (per the audit's own PARTIALLY ENFORCED and COMPATIBLE BUT UNTESTED findings, all of which found existing, working, tested capability, not absent capability):

- Mandatory, validated `Provenance` on every `Entity`/`Document`/`Assertion`/`Relationship`.
- `ContentNature`'s original/extracted/summarised/inferred/unknown distinction.
- Explicit, non-defaulted `UNKNOWN` handling for provenance fields.
- Non-erasing status lifecycle (`DELETED` preserves the record; nothing removes content to express a status change).
- Structural, non-semantic, unranked retrieval, with no `PermissionEngine` dependency inside Memory Core itself.

**Genuinely required future enhancements**, all additive to the existing frozen contracts, none reopening the Scope Lock:

- Round-trip proof tests for provenance, transformation history, and integrity information surviving retrieval (Sections 4–5).
- Two small additive `Provenance` fields for amendment/disclosure timestamps (CT-EI-40).
- A test proving `SUPPORTS`/`CONTRADICTS`/`DISPUTES` relationship creation never itself changes a referenced record's status (Compliance Audit §15, Finding 6 — a false-positive risk, not a failing test, but currently unproven).
- The already-planned rename of `MemoryStore` to Knowledge Memory, sitting atop Memory Core (Reconciliation §1, §8) — this is Programme 3's work, not further Memory Core work, and is listed here only to make the boundary explicit.

**Explicitly not reopened:** the permission boundary, event scope, retrieval scope, and lifecycle rules the Scope Lock froze (`MEMORY_CORE_SCOPE_LOCK.md` §§5–10) are not constitutional findings and are not touched by this plan. The permanent exclusion of any World Model dependency on or from Memory Core (Reconciliation §10) is preserved without exception — no remediation item anywhere in this plan proposes crossing that boundary.

---

## 8. World Model Remediation

The audit identified four conflicts (Section 3, above). This section sequences their implementation without redesigning the World Model.

**Implementation units, in dependency order:**

1. **Nullable confidence** (CT-EI-56). No dependency on anything else in this list. Ships first because it is the smallest unit and because CT-EI-48's remediation needs `confidence` to already tolerate null before it can meaningfully be one factor among several.
2. **Claimed-timestamp preservation and bounded prior-belief history** (CT-EI-58/59). No dependency on Unit 1. Can proceed in parallel.
3. **Unresolved-outcome decision type and competing-belief retention** (CT-EI-05/24). Depends on Unit 1 only in the sense that an unresolved pair of beliefs may legitimately include one with unquantified confidence — sequencing after Unit 1 avoids rework.
4. **Multi-factor update-policy weighing** (CT-EI-48). Depends on Unit 3 (an "unresolved" outcome must exist before a policy can legitimately return it in place of a forced win/loss) and on CT-EI-49's independence-tracking work landing in Knowledge Memory first (Section 6), since one of the new weighing factors is corroboration-independence.

**Migration order:** Units 1 and 2 are non-breaking additive changes to existing fields and can land independently of everything else in this plan. Unit 3 changes `WorldModelUpdatePolicy`'s return shape, which is an internal, injected policy seam (`docs/architecture/WORLD_MODEL_RUNTIME_ARCHITECTURE.md`) — its callers are limited and known, making this a contained migration. Unit 4 is the only unit genuinely dependent on work outside the World Model (Knowledge Memory's independence tracking) and should not begin before that dependency lands.

**Expected test changes:** the three Register A corrections (Section 11) land alongside Units 1–3 respectively, not before — a test should never be rewritten to describe intended future behaviour before that behaviour exists. `WorldModelContractsTest.kt`'s confidence-boundary tests are extended, not replaced, to additionally cover the null case. `InMemoryWorldModelTest.kt`'s rejection and timestamp tests are corrected in place once Units 2 and 3 land.

---

## 9. Reasoning Pipeline Remediation

The audit's central structural finding: Memory Core is well-tested but sits outside the live reasoning pipeline, which runs on the legacy `MemoryStore` via `MemorySource`. This section stages the integration the audit calls for, consistent with the Reconciliation's own already-decided layering (Conversation → Memory Core → Knowledge Memory → Reasoning Context, World Model independent and parallel).

**Stage 1 — Knowledge Memory exists (Programme 3).** `MemoryStore`/`InMemoryMemoryStore`/`CandidateMemory`/`MemoryRecord`/`MemoryPromotionPolicy`/`MemorySource` are renamed to their Knowledge Memory equivalents, rebuilt on top of Memory Core rather than as an independent flat store, exactly as the Reconciliation already specifies (`MEMORY_ARCHITECTURE_RECONCILIATION.md` §8, §16). This is a Programme 3 deliverable, not a Programme 2 (Memory Core) or Programme 4 (Reasoning Context) one — Memory Core itself does not change.

**Stage 2 — Evidence retrieval through Knowledge Memory.** Knowledge Memory's own retrieval surface (successor to `MemorySource.recall`) returns records that carry Memory Core's provenance and evidential metadata forward, rather than the bare `MemoryRecord` shape used today. This is where `ContentNature`, provenance, and (once built, Section 6) evidential state first become visible to anything outside Memory Core itself.

**Stage 3 — Reasoning Context wired onto Knowledge Memory (Programme 4).** `DefaultReasoningContextAssembler` is adapted to consume Knowledge Memory's retrieval surface instead of the legacy `MemorySource`. Its already-correct behaviour (never fabricating confidence, rendering it honestly when present) is preserved and extended to render the additional evidential metadata Stage 2 makes available. World Model's own, separate feed into Reasoning Context is unaffected — this stage does not touch the World Model integration point at all, consistent with the permanent Memory Core / World Model separation.

**Stage 4 — Response generation carries structured evidential state (Programme 4/7 boundary).** `ReasoningProviderResponse` gains a structured field (the Article IV taxonomy, once built per Section 6) alongside its existing `Goal`/`Reply`/`NoAction` text. This is the step that finally lets CT-EI-54's already-correct assembler-level honesty reach something a user actually receives, and is a precondition for the majority of the Section 6 register.

**Objective confirmed:** future Parker reasoning operates on Memory-Core-descended, constitutionally governed Knowledge Memory content — never on a flat, provenance-free store — once Stages 1–3 are complete. Stage 4 is what makes that governance visible in Parker's actual output, not merely present in its storage layer.

---

## 10. Representation Remediation (Planning Only)

No implementation is proposed here — this section identifies what future work must achieve, for Programme 7 (Representation Engine) to plan against.

- **Provenance disclosure:** a representation that draws on a material fact must be able to disclose, on request or by design, the provenance behind it — achievable once Stage 2 (Section 9) makes provenance visible outside Memory Core, but the disclosure mechanism itself (surfacing it in what Parker actually says) is Representation Engine's own work.
- **Uncertainty disclosure:** extending CT-EI-54's existing assembler-level honesty (Section 4) through to free text, once Stage 4 (Section 9) exists.
- **Evidential distinction:** surfacing `ContentNature`/evidential-state so that a summary is never presented as equivalent to a complete source without disclosure (CT-EI-36) — depends on the Article IV taxonomy (Section 6) and on Stage 2.
- **Assumption disclosure:** surfacing the outcome of Article V's propositional-integrity examination (Section 6, Reasoning Context group) wherever a reformulated or examined proposition is represented.
- **Contradiction preservation:** representing an unresolved competing proposition (once CT-EI-05/24 is remediated, Section 3) as genuinely unresolved, rather than silently picking one side at the representation layer even after the World Model correctly retains both.
- **Historical preservation:** representing a revised conclusion (once World Model history retention, Section 3 Unit 2, and Knowledge Memory's narrative-evolution work, Section 6, exist) without erasing what was previously represented.

Each item above is downstream of a Medium- or Long-term item elsewhere in this plan; none is independently actionable before its dependency lands.

---

## 11. Executable Test Roadmap

Master schedule, consolidating Sections 3–10. Every remediation item is linked to the tests that prove it complete.

**Existing tests needing correction (3 — Register A, all Immediate, sequenced with Section 8):**

1. `InMemoryWorldModelTest.kt` — "a weaker contradictory observation is rejected and does not alter current belief" → corrected alongside Section 8 Unit 3.
2. `DefaultWorldModelUpdatePolicyTest.kt` — "a weaker contradictory observation is rejected, not silently discarded" → corrected alongside Section 8 Unit 3.
3. `InMemoryWorldModelTest.kt` — "the World Model assigns the authoritative timestamp, never trusting a source-reported one" → corrected alongside Section 8 Unit 2 (extended, not reversed — the authoritative timestamp remains authoritative; the test is corrected to also assert the claimed timestamp is preserved).

Plus the confidence-boundary tests in `WorldModelContractsTest.kt` and `DefaultWorldModelUpdatePolicyTest.kt`, extended (not replaced) alongside Section 8 Unit 1.

**New tests required now (9 — Register B, all Near-term, Section 4/5):** the nine items listed in the Compliance Audit §17, closing CT-EI-27, 28, 32, 38, 39, 63, plus the relationship-status-independence and Memory-Core-pipeline-disconnection characterization tests.

**Deferred tests (Register C, Medium/Long-term, Section 6):** the six items listed in the Compliance Audit §18, each gated on the capability its own row in Section 6 identifies as a dependency — no deferred test should be written before its dependency exists, per this plan's own "do not move work earlier than necessary" principle.

**New tests implied by this plan beyond the audit's own two registers**, all traceable to a Section 3 or Section 8 remediation unit: World Model unresolved-outcome tests (4–6, Section 3 CT-EI-05/24), nullable-confidence tests (3–4, Section 3 CT-EI-56), multi-factor weighing tests (5–7, Section 3 CT-EI-48), claimed-timestamp/history tests (4–5, Section 3 CT-EI-58/59).

No remediation item in this plan lacks a named test or test category above. Completion of any item is defined exactly by those tests passing — never by code review, architecture-document update, or successful compilation alone, consistent with the audit's own evidentiary discipline.

---

## 12. Programme Roadmap

Allocated using only Programme numbers already implied by the existing roadmap (`docs/architecture/MEMORY_CORE_*`, `docs/implementation/MEMORY_CORE_IMPLEMENTATION_PLAN.md` all confirm **Programme 2 = Memory Core**; the remaining numbers below extend that same sequence and are not invented independently by this plan).

**Programme 2 — Memory Core.** Already scope-locked and implementation-planned. Remaining constitutional work: Section 4 and Section 5 items (round-trip tests, two additive `Provenance` fields, the relationship-status-independence test). No new architecture.

**Programme 3 — Knowledge Memory.** The `MemoryStore` → Knowledge Memory rename and Memory-Core-backed reconstruction (already the Reconciliation's own next step); the Article IV evidential-state taxonomy (Section 6); independence/corroboration tracking on `Relationship` (CT-EI-49, Knowledge Memory side); narrative-evolution preservation following dispute (CT-EI-60); Reasoning Pipeline Stages 1–2 (Section 9).

**Programme 4 — Reasoning Context.** Article V propositional integrity (CT-EI-08–16, 66); Article VII burden of justification (CT-EI-18–26, 64, 68); materiality and exception-reliance disclosure (CT-EI-01, 06, 07); Reasoning Pipeline Stage 3 (Section 9); residual Article XV enforcement gates (CT-EI-65).

**Programme 5 — World Model.** All four conflict remediations (Section 3, Section 8); World Model side of CT-EI-49 (independence as a weighing factor); contemporaneity/capture-type work shared with Programme 6 (CT-EI-41–47, World Model portion — observation-capture metadata); temporal-integrity items dependent on history retention (CT-EI-59, 61).

**Programme 6 — Document Intelligence.** OCR/transcription/translation disclosure (CT-EI-33, 35); source-material preservation (CT-EI-34); Document-side contemporaneity/capture-type work (CT-EI-41–47, Document portion) — all explicitly deferred here already by `MEMORY_CORE_SCOPE_LOCK.md` §4's own "Document Handling Programme" language, not a new designation invented by this plan.

**Programme 7 — Representation Engine.** Reasoning Pipeline Stage 4 (Section 9); all of Section 10's representation obligations; Article IX representation-layer obligations (CT-EI-36, 37, 67); confidence explanation (CT-EI-57); at-the-time explanation and retrospective-strengthening prevention (CT-EI-61, 62).

**Negative evidence reasoning (CT-EI-50–53)** does not map cleanly onto any Programme named above; it is new reasoning capability most naturally owned by Programme 4 (Reasoning Context) once that Programme's foundational work (Article IV, V, VII) exists, and is recorded there in Section 2 and Section 6 rather than given an invented Programme of its own.

---

## 13. Risk Assessment

- **Highest constitutional risk:** the World Model conflicts (Section 3) remaining unaddressed while new capability is built on top of the World Model. Every day this persists, more callers may come to depend on the very behaviour (single-belief collapse, forced confidence precision) that must eventually change — increasing the migration cost of a fix that is currently still small.
- **Highest implementation risk:** Reasoning Pipeline Stage 4 (Section 9) — changing `ReasoningProviderResponse`'s public shape is the most structurally invasive item in this entire plan, since it is consumed by every downstream response-composition and delivery component. It must not begin before the Article IV taxonomy it depends on is itself stable.
- **Highest migration risk:** the `MemoryStore` → Knowledge Memory rename (Programme 3, Section 9 Stage 1). It touches a production-wired subsystem (`MemorySource` is live in `ParkerRuntime.kt` today) rather than dormant scaffolding, and the Reconciliation itself already flags this as requiring careful, staged execution, not a mechanical rename.
- **Highest regression risk:** Section 8 Unit 3 (introducing an "unresolved" outcome to the World Model update policy). Any caller currently assuming exactly one belief per subject could behave incorrectly if a competing pair is returned instead — this unit requires an audit of every current `WorldModel` consumer before it ships, not only new tests.
- **Highest testing risk:** the deferred Register C tests (Section 11), several of which describe capability that does not yet exist. Writing them prematurely (before their dependency lands) risks encoding a guess at the eventual API shape that the real implementation then has to either honour awkwardly or break — this plan's own "do not move work earlier than necessary" principle exists specifically to prevent this.

---

## 14. Success Criteria

Parker may legitimately claim constitutional compliance with Amendment No. 1 only when all of the following are objectively true, verified the same way the Compliance Audit itself verified non-compliance — by executable test, not by architecture document, naming, or successful compilation:

1. Zero Constitutional Tests remain classified CONFLICTING TEST OR BEHAVIOUR.
2. Every Constitutional Test applicable to a subsystem that exists is classified FULLY ENFORCED, except where a documented, Article-XI-style express governing-rule exception has itself been constitutionally justified and tested as such (this plan recommends against relying on this exception anywhere — Section 3, CT-EI-48).
3. CT-EI-33 and CT-EI-69 remain the only permissible NOT YET APPLICABLE classifications, and only for as long as Document Intelligence (Programme 6) has not yet been built and the Foundational Principle's interpretive-hierarchy clause remains, as it is today, a textual rather than executable rule.
4. A re-run of the Executable Test Compliance Audit's own methodology (Section 2 of that document) against the then-current codebase returns a Final Classification of **ALL CURRENT EXECUTABLE TESTS CONSTITUTIONALLY COMPLIANT**.
5. The false-positive risks the audit identified (its Section 15) are individually closed — in particular, Memory Core's epistemic scaffolding must be provably reachable by the live reasoning pipeline (Section 9, Stage 3 complete), not merely present in storage.

Compliance is not claimed Programme-by-Programme as each one closes its own findings; it is claimed once, at the point all five criteria above hold simultaneously, following a fresh audit — consistent with this plan's own instruction not to equate partial progress with constitutional compliance.

---

## Final Recommendation

**Proceed incrementally alongside planned Programmes.**

The audit found real, tested conflicts, but they are architecturally contained (four findings, one subsystem) and none currently produces an unsafe or unrecoverable state — they produce representations that are less epistemically careful than the Constitution requires, not representations that are actively false or destructive. Suspending feature development entirely would halt Programme 3 (Knowledge Memory) and beyond, which are themselves the vehicles by which the majority of the 52 NOT IMPLEMENTED findings get resolved — pausing them would delay compliance, not accelerate it. Proceeding immediately and exhaustively, ahead of the Programmes each finding naturally belongs to, would violate this plan's own second and third planning principles (prefer additive change; do not move work earlier than necessary) by forcing Reasoning Context and Representation Engine-scale work into the current Programme before their own foundations exist.

The recommended path is therefore: complete the Immediate items (Section 2) — the four World Model conflicts and their corresponding test corrections — inside the current Programme, before any new capability is built on the World Model; complete the Near-term items (Memory Core round-trip tests and small additive fields) alongside Programme 2's own remaining work, since they cost little and close real gaps; and let every Medium- and Long-term item ride alongside the Programme that already, independently, owns the subsystem it belongs to (Programme 3 Knowledge Memory, Programme 4 Reasoning Context, Programme 5 World Model, Programme 6 Document Intelligence, Programme 7 Representation Engine), exactly as Sections 6 and 12 sequence them. This is the only path consistent with both the audit's finding of genuine non-compliance and this plan's own constitutional obligation not to weaken, redesign, or rush the architecture the Constitution governs.
