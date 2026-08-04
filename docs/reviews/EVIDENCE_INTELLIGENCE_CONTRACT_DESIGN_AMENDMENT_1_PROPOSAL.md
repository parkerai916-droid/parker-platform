# Evidence Intelligence Contract Design — Amendment 1 (ReasoningSubject Integration) Proposal

## Status

**Proposal only. Not an amendment.** No canonical governance document is amended by this document. No Kotlin is implemented, proposed as a diff, or changed. Neither `src/` nor `tests/` is touched. Nothing is staged, committed, or pushed.

**Repository baseline confirmed before this analysis:** `main` at `7c06064` ("governance: complete Reasoning Provider Amendment 1"), working tree clean, matching origin/main. That commit touched exactly `docs/architecture/REASONING_PROVIDER_CONTRACT_DESIGN.md` plus six review documents; `docs/architecture/EVIDENCE_INTELLIGENCE_CONTRACT_DESIGN.md`, `EVIDENCE_INTELLIGENCE_SCOPE_LOCK.md`, `EVIDENCE_INTELLIGENCE_IMPLEMENTATION_PLAN.md`, and `src/interfaces/EvidenceIntelligence.kt` are unmodified since this session's original review of them.

This document identifies exactly what a future Evidence Intelligence Contract Design amendment would need to change so that Evidence Intelligence can lawfully orchestrate the now-frozen `ReasoningSubject`-based Reasoning Provider contract. It is a planning input to that future amendment, not the amendment itself.

---

## 1. Amendment Objective

Bring `docs/architecture/EVIDENCE_INTELLIGENCE_CONTRACT_DESIGN.md` current with Reasoning Provider Contract Design Amendment 1: acknowledge that `ReasoningProviderRequest` now carries `subject: ReasoningSubject` rather than `turn: Turn`; state that Evidence Intelligence's own orchestration path is `ReasoningSubject.OfEvidenceAnalysisRequest`, wrapping `EvidenceAnalysisRequest` unmodified; and restate, from Evidence Intelligence's own side, the frozen invariant that `ReasoningProviderRequest.reasoningContext` (not `EvidenceAnalysisRequest.reasoningContext`) is the sole context `ReasoningProvider.reason` consults. No responsibility, ownership, dependency, or public-surface change to Evidence Intelligence itself is required or proposed — this is a downstream currency update, not a redesign.

---

## 2. Exact Paragraphs Requiring Amendment

### 2.1 — "Governing sources, by section" and the "reviewed, in full" list (top of document)

**Current text:** "...and `docs/architecture/REASONING_PROVIDER_CONTRACT_DESIGN.md`. It does not reopen, re-argue, or narrow any decision in any of those documents." / "**`REASONING_PROVIDER_CONTRACT_DESIGN.md`** — controlling for the Reasoning Provider shapes this document reuses when Evidence Intelligence internally orchestrates one (§3, §4, §12)."

**Why stale:** These citations point to the Reasoning Provider Contract Design as it stood before Amendment 1. The document reused is now the amended version.

**Smallest amendment:** Append references to `docs/reviews/EVIDENCE_INTELLIGENCE_UNIT_3_CONSTITUTIONAL_DECISION_MEMORANDUM.md`, `docs/reviews/REASONING_PROVIDER_CONTRACT_DESIGN_AMENDMENT_PROPOSAL.md`, `docs/reviews/REASONINGSUBJECT_CONTRACT_DESIGN_STUDY.md`, and `docs/reviews/REASONING_PROVIDER_CONTRACT_DESIGN_AMENDMENT_1_INDEPENDENT_REVIEW.md` to the reviewed-sources list, and note that the Reasoning Provider Contract Design citation now refers to its Amendment-1 state. No re-argument of anything already settled.

### 2.2 — §3 Public Model, the Reasoning Provider row

**Current text:** "`ReasoningProvider`, `ReasoningProviderRequest`, `ReasoningProviderResponse`, `ReasoningContext` | Reasoning Provider Contract Design | Reuses, unmodified, as an orchestrated dependency"

**Why stale:** `ReasoningProviderRequest` is no longer "unmodified" as a matter of present fact — Reasoning Provider Contract Design Amendment 1 changed its `turn: Turn` field to `subject: ReasoningSubject`. Evidence Intelligence itself still does not modify anything (this remains true), but the row's plain reading ("Reuses, unmodified") now describes a contract that has, in fact, been amended by its own owning document.

**Smallest amendment:** Add a parenthetical: "Reuses, as amended by Reasoning Provider Contract Design Amendment 1 (`ReasoningProviderRequest.subject: ReasoningSubject`) — unmodified *by Evidence Intelligence*." This preserves the row's actual meaning (Evidence Intelligence is a non-modifying consumer) while correcting the now-inaccurate implication that the contract itself is presently unchanged.

### 2.3 — §4 Inputs, the "An assembled Reasoning Context, where relevant" bullet

**Current text:** "An optional `ReasoningContext` (Reasoning Provider Contract Design §2, reused unchanged), supplied only when this analysis internally invokes one or more Reasoning Providers. Its assembly remains, exactly as the Reasoning Provider Contract Design already discloses, an unassigned responsibility this document does not resolve."

**Why stale:** This is the exact paragraph the Independent Constitutional Review (`REASONING_PROVIDER_CONTRACT_DESIGN_AMENDMENT_1_INDEPENDENT_REVIEW.md` §3, Dual ReasoningContext Analysis) identified as creating a cross-document tension: it describes `EvidenceAnalysisRequest.reasoningContext` as "supplied only when this analysis internally invokes one or more Reasoning Providers," which now reads as though this field is what reaches `ReasoningProvider.reason` — but Reasoning Provider Contract Design's own Amendment 1 invariant (Section 2) fixes that the *top-level* `ReasoningProviderRequest.reasoningContext` is the sole field ever consulted, and that `EvidenceAnalysisRequest.reasoningContext` is not independently read for that purpose.

**Smallest amendment:** Append one clause acknowledging the frozen invariant: "...an unassigned responsibility this document does not resolve. Where this analysis internally invokes a `ReasoningProvider` via `ReasoningSubject.OfEvidenceAnalysisRequest` (Reasoning Provider Contract Design, Amendment 1), that invocation's own top-level `ReasoningProviderRequest.reasoningContext` — not this field — is the sole context `ReasoningProvider.reason` consults, per that document's own frozen invariant; this field retains its existing meaning and ownership within Evidence Intelligence's own analysis, independent of that invocation." This resolves the cross-document tension entirely from Evidence Intelligence's own side, without touching Reasoning Provider Contract Design again.

### 2.4 — §12 Dependency Model, the `ReasoningProvider` row

**Current text:** "`ReasoningProvider` (zero or more) | Evidence Intelligence → Reasoning Provider(s) | Internal analytical mechanism, orchestrated, never itself | Reasoning Provider Contract Design"

**Why stale:** Not stale in substance — the dependency edge, direction, and purpose are all unchanged. Worth a footnote only, for readers tracing the exact case used.

**Smallest amendment:** Append a footnote: "(as amended, Amendment 1: invoked via `ReasoningSubject.OfEvidenceAnalysisRequest`, wrapping `EvidenceAnalysisRequest` unmodified)." No structural change to the row itself.

### 2.5 — §13 Repository Reuse

**Current text:** "`ReasoningProvider`, `ReasoningProviderRequest`, `ReasoningProviderResponse`, `ReasoningContext` — the Reasoning Provider's own contract, unmodified and unbroadened."

**Why stale:** Same defect as §2.2 above — "unmodified" describes the contract's own present state, which has changed at its owning tier.

**Smallest amendment:** "...the Reasoning Provider's own contract — `ReasoningProviderRequest` now amended (Amendment 1) to carry `subject: ReasoningSubject`; `ReasoningProvider`, `ReasoningProviderResponse`, and `ReasoningContext` remain unbroadened; none modified *by Evidence Intelligence*."

### 2.6 — §15 Verification Requirements, "Reasoning Provider orchestration, not identity"

**Current text:** "No class implementing `EvidenceIntelligence` itself implements `ReasoningProvider`, and no `ReasoningProvider` implementation holds a reference back to `EvidenceIntelligence` (Reasoning Provider Contract Design's own statelessness and non-retention guarantees, unmodified)."

**Why this one is *not* stale:** The "unmodified" here refers to the statelessness and non-retention guarantees (Reasoning Provider Contract Design Sections 5 and 7), which Amendment 1 did not touch at all. This paragraph remains fully accurate and requires **no change**. It is listed here only to show it was checked, not skipped.

---

## 3. Exact Paragraphs Remaining Unchanged

- **§1 Responsibilities, "Orchestrating existing Reasoning Providers"** — unaffected; cites `ConversationTurnReasoningCoordinator` as a pattern precedent and `REASONING_PROVIDER_CONTRACT_DESIGN.md` generically, neither of which depends on the request's internal field shape.
- **§2 Explicit Non-Responsibilities, "Constitute a Reasoning Provider"** — unaffected; Evidence Intelligence's non-identity with `ReasoningProvider` is untouched by Amendment 1.
- **§5, §6, §8, §9, §10, §11** in full — none discusses `ReasoningProviderRequest`'s internal shape.
- **§14 Architectural Boundaries diagram** — unaffected; it already omits Conversation Engine entirely, correctly, and continues to.
- **§15, all verification items except the one addressed in §2.6 above** — unaffected.
- The document's own **public-type count** ("exactly three new public types," §3) — confirmed unchanged; see §5, below.

---

## 4. Ownership Analysis

- **Evidence Intelligence continues to own `EvidenceAnalysisRequest`.** Confirmed — Reasoning Provider Contract Design Amendment 1 itself states this explicitly (Section 4 table: "owned by Evidence Intelligence, reused here unmodified"), and nothing in that amendment or this proposal alters it.
- **Reasoning Provider Contract Design owns `ReasoningSubject`.** Confirmed — fixed by that document's own text ("Owned by this Contract Design... never by Conversation Engine, never by Evidence Intelligence, and never left ownerless").
- **Evidence Intelligence reuses `ReasoningSubject` unmodified.** Confirmed — Evidence Intelligence only ever constructs the `OfEvidenceAnalysisRequest` case's payload (its own `EvidenceAnalysisRequest`); it never extends, subclasses, or amends `ReasoningSubject` itself, consistent with that type's own frozen "does not modify... this type only references them" property.
- **Conversation Engine retains exclusive ownership of `Turn` and `Conversation`.** Confirmed, unaffected by this proposal — nothing here touches `CONVERSATION_ENGINE_CONTRACT_DESIGN.md` or `CONVERSATION_CONTINUITY_CONTRACT_DESIGN.md`.
- **Evidence Intelligence gains no dependency on Conversation Engine.** Confirmed — the `OfEvidenceAnalysisRequest` case never references `Turn`, `Conversation`, or `ConversationEngine`; Evidence Intelligence's dependency table (§12) gains no new row.
- **Evidence Intelligence remains an orchestrator of `ReasoningProvider`, never an implementation of it.** Confirmed — unaffected by Amendment 1, restated identically in §15 (see §2.6 above, unchanged).

---

## 5. Public Contract Analysis

**Does reusing `ReasoningSubject` require any new Evidence-Intelligence-owned public type? No.** `ReasoningSubject` is owned by the Reasoning Provider Contract Design (§4, above), not by Evidence Intelligence. Evidence Intelligence's own public model remains exactly what the Contract Design's §3 and the Scope Lock's §4 already froze: `EvidenceAnalysisRequest`, `EvidenceAnalysisResult`, the "Candidate record produced" payload selector, and (via Unit 5) the `EvidenceIntelligence` operation — four total, none added, none removed. The Scope Lock's own governing text is exact on this point: "No fifth new public type or interface is authorised. These four are the entire new public surface this implementation may create." This amendment proposal introduces no fifth type, and nothing in Reasoning Provider Contract Design Amendment 1 required one — this was the specific design property Option 1 was chosen for, over the rejected Option 3 (a dedicated Evidence-Intelligence-specific reasoning contract), precisely to avoid this outcome.

---

## 6. Dependency Analysis

| Subsystem | Structural change? | Descriptive change? |
|---|---|---|
| Evidence Custodian | No | No — unrelated to this amendment |
| Memory Core | No | No — unrelated to this amendment |
| Reasoning Provider | No — same single row, same direction, same purpose | Yes — a footnote naming the `OfEvidenceAnalysisRequest` case (§2.4, above) |
| Conversation Engine | No — still zero dependency, no row exists or is added | No |
| Permission Engine | No | No — Evidence Intelligence still holds no Permission Engine dependency of its own |
| Knowledge Memory | No | No — unrelated to this amendment |

**Conclusion: the Evidence Intelligence dependency table changes descriptively only, not structurally.** No row is added, removed, or redirected; the closed, three-subsystem dependency set (`EvidenceCustodian.retrieve`, `MemoryRetrieval`, `ReasoningProvider`) is exactly what it was before Amendment 1.

---

## 7. Unit 3 Orchestration Implications

Established, without inventing any implementation class or Kotlin name beyond already-frozen public contracts:

- Evidence Intelligence supplies a `ReasoningProviderRequest` whose `subject` is `ReasoningSubject.OfEvidenceAnalysisRequest`, wrapping its own, already-frozen `EvidenceAnalysisRequest` unmodified.
- The top-level `ReasoningProviderRequest.reasoningContext` — not `EvidenceAnalysisRequest.reasoningContext` — is the sole context `ReasoningProvider.reason` consults, per Reasoning Provider Contract Design's own frozen invariant (restated from Evidence Intelligence's own side at §2.3, above).
- `EvidenceAnalysisRequest.reasoningContext` retains its existing ownership and meaning entirely within Evidence Intelligence's own analysis (per `EVIDENCE_INTELLIGENCE_CONTRACT_DESIGN.md` §4's own, unchanged field definition); it is simply not the field a `ReasoningProvider` invocation reads.
- Unit 3 performs orchestration only: constructing the request, invoking `ReasoningProvider.reason`, receiving the response. Nothing about this changes Unit 3's own stop condition.
- Unit 3 continues to perform no output shaping (Unit 4's own responsibility), no acceptance (the separate acceptance coordinator, Unit 7), no permission composition (Unit 6), and no runtime wiring (Unit 8) — all four boundaries are exactly as frozen by the Implementation Plan already, untouched by Reasoning Provider Contract Design Amendment 1 or by this proposal.

---

## 8. Scope Lock Assessment

**No amendment to `EVIDENCE_INTELLIGENCE_SCOPE_LOCK.md` is required.** The Scope Lock's own dependency freeze (§4) is authored at the level of *which subsystems* Evidence Intelligence may depend on — a closed, three-row table naming `EvidenceCustodian.retrieve`, `MemoryRetrieval`, and `ReasoningProvider` — never at the level of those subsystems' own internal field shapes. Its header phrase, "Reused, unmodified (Evidence Intelligence → the named subsystem)," describes Evidence Intelligence's own behaviour as a non-modifying consumer — a claim that remains true regardless of `ReasoningProviderRequest`'s field-level amendment, since Evidence Intelligence itself still modifies nothing. This is a materially different phrasing from the Contract Design's own §3/§13 language (which describes the *contract's own present state* as "unmodified," now stale — §2.2, §2.5, above); the Scope Lock's phrasing was already future-proof against exactly this kind of downstream change. The freeze already accommodates Amendment 1 without needing to say anything new.

---

## 9. Implementation Plan Assessment

**Exact Unit 3 passage requiring later change** (not edited here): `EVIDENCE_INTELLIGENCE_IMPLEMENTATION_PLAN.md` §8 Unit 3, Responsibilities: "Invoke an existing `ReasoningProvider` via its own, **unmodified** request/response contract; treat each invocation as a pure, stateless callee with no obligations beyond what the Reasoning Provider Contract Design already defines." The word "unmodified" is now stale in the same way identified at §2.2/§2.5 above, and should eventually read something like "via its own request/response contract (as amended, Reasoning Provider Contract Design Amendment 1: `subject = ReasoningSubject.OfEvidenceAnalysisRequest(request)`)."

**Not requiring change:** Unit 3's own Dependencies line ("Unit 1; `ReasoningProvider`, `ReasoningProviderRequest`, `ReasoningProviderResponse`") names the same three types by name, unaffected; its Verification Goals and Completion Criteria are both already framed at the level of identity/statelessness, unaffected by the field-shape amendment.

**Other Units checked, found not stale:** Unit 1 (shape foundation — no reference to `ReasoningProviderRequest`'s internal shape), Unit 2 (input resolution — no relationship to Reasoning Provider at all), Unit 4 (output discipline — governs `EvidenceAnalysisResult`, not the request side), Unit 5 (assembles Units 1–4 — no reasoningContext-specific language), Unit 6, Unit 7, Unit 8 (gating, acceptance, composition — none references `ReasoningProviderRequest`'s field shape). §5 ("Existing Components Reused") names `ReasoningProviderRequest`, `ReasoningProviderResponse`, `ReasoningContext` by name only, without characterising their shape — accurate as written, though a footnote noting the `ReasoningSubject` selector's existence would aid a future reader; not required.

---

## 10. CDR and Constitutional Assessment

- **No operative CDR-007 decision changes.** CDR-007's classification of Evidence Intelligence, its exclusive-responsibility table, and its Decision Rule that the `ReasoningProvider` abstraction is "not broadened, extended, or reinterpreted to describe Evidence Intelligence" are all untouched — Evidence Intelligence still never implements `ReasoningProvider`, and this proposal changes nothing about that.
- **No constitutional-tier document changes.** The Parker Constitution and `reasoning-context.md` are unaffected and unread-from in any new way beyond what Reasoning Provider Contract Design Amendment 1 already established.
- **This remains a Contract Design evolution only.** Every change identified in §2 is a Contract-Design-tier currency correction, mirroring exactly the same tier and character of correction Reasoning Provider Contract Design Amendment 1 itself required and received.

---

## 11. Recommended Amendment Sequence

Mirroring the now-precedented sequence Reasoning Provider Contract Design Amendment 1 itself followed:

1. Draft an in-place "Amendment 1" section in `EVIDENCE_INTELLIGENCE_CONTRACT_DESIGN.md`, applying exactly the six paragraph-level changes identified in §2 (five substantive, one confirmed-unnecessary).
2. Independent Constitutional Review of that draft, specifically re-examining the §2.3 cross-reference for internal consistency with Reasoning Provider Contract Design's own frozen invariant.
3. Final Confirmation Review.
4. Commit and freeze, matching the `7c06064` precedent.
5. Only then, as a separate, later act: the Implementation Plan's Unit 3 wording (§9, above) may be updated to match.

---

## 12. Confirmation No Canonical File Changed

No governance document was modified in the preparation of this proposal. `docs/architecture/EVIDENCE_INTELLIGENCE_CONTRACT_DESIGN.md`, `EVIDENCE_INTELLIGENCE_SCOPE_LOCK.md`, `EVIDENCE_INTELLIGENCE_IMPLEMENTATION_PLAN.md`, `REASONING_PROVIDER_CONTRACT_DESIGN.md`, and every other file reviewed remain exactly as they were at `7c06064`. No Kotlin was implemented. No production code or test was touched.

## 13. No Git Actions

No git command beyond read-only inspection (`status`, `log`, `show --stat`) was run. Nothing was staged, committed, or pushed.
