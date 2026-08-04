# Independent Constitutional Review — Reasoning Provider Contract Design Amendment 1 (ReasoningSubject)

This review reads the amended document as it actually stands (re-read in full, 691 lines, above), not the prior implementation summary. No file was edited in the course of this review.

---

## 1. Exact Files Inspected

- `docs/architecture/parker-constitution.md`
- `docs/architecture/reasoning-context.md`
- `docs/decisions/CDR-007_CONSTITUTIONAL_CLASSIFICATION_OF_EVIDENCE_INTELLIGENCE.md`
- `docs/architecture/EVIDENCE_INTELLIGENCE_CONTRACT_DESIGN.md`
- `docs/architecture/EVIDENCE_INTELLIGENCE_SCOPE_LOCK.md`
- `docs/implementation/EVIDENCE_INTELLIGENCE_IMPLEMENTATION_PLAN.md`
- `docs/reviews/EVIDENCE_INTELLIGENCE_UNIT_3_CONSTITUTIONAL_DECISION_MEMORANDUM.md`
- `docs/reviews/REASONING_PROVIDER_CONTRACT_DESIGN_AMENDMENT_PROPOSAL.md`
- `docs/reviews/REASONINGSUBJECT_CONTRACT_DESIGN_STUDY.md`
- `docs/architecture/REASONING_PROVIDER_CONTRACT_DESIGN.md` — the amended document, read in full, current state, line-by-line
- `src/interfaces/EvidenceIntelligence.kt` (`EvidenceAnalysisRequest`'s actual field list, for the Dual ReasoningContext analysis)

---

## 2. Amendment Fidelity Findings

**Fidelity to the frozen-property list is high.** The eight substantive Frozen Properties in the Study (`REASONINGSUBJECT_CONTRACT_DESIGN_STUDY.md` §4) are all present in the amended Section 2, in the same order, with faithful wording. The Study's ninth bullet ("No Kotlin name... assigned by this study") was correctly not carried over verbatim, since this document is the tier that does assign the name — it was replaced with an ownership statement drawn directly from the Study's §2, which is the right substitution, not an omission.

**One added element not among the Amendment Proposal's enumerated paragraphs:** the Amendment Proposal (`REASONING_PROVIDER_CONTRACT_DESIGN_AMENDMENT_PROPOSAL.md` §2) identifies exactly seven paragraphs requiring change, and its §3 explicitly lists the **Status section** as one of the paragraphs "confirmed unaffected" ("still describes a Stage 2A Contract Design"). The actual amendment inserts an entirely new "Amendment 1 — ReasoningSubject" subsection (lines 13–36) directly under Status — content not identified by the Amendment Proposal at all. This is a scope departure from "modify only the paragraphs identified in the Amendment Proposal."

**A more serious consequence of that same insertion:** the new banner asserts, in bold, "**Applied**," while the pre-existing, untouched Status paragraph immediately above it still reads "Design proposal, not yet reviewed or accepted." The amended document now asserts, in the same breath, that it is both unreviewed/unaccepted and that an amendment to it has already been "Applied." This is exactly the category the task asks to detect: "wording that converts an implementation choice into a constitutional rule" — here, a governance-status claim (acceptance) is self-declared by the document rather than earned through the review process this very task is conducting.

**No added property beyond what was authorised**, no widened responsibility, and no changed ownership or dependency were found anywhere else in the document — Sections 1, 3, 5, 7 (substance), 8, and 9 are untouched, and every dependency-freedom claim in Section 7 remains textually true of the amended shapes (verified in §6, below, with one caveat).

---

## 3. Public Contract Findings

| Check | Result |
|---|---|
| `ReasoningSubject` has exactly two cases | **Confirmed** (`OfTurn`, `OfEvidenceAnalysisRequest`, Section 2) |
| Owned solely by the Reasoning Provider Contract Design | **Confirmed** ("Owned by this Contract Design... never by Conversation Engine, never by Evidence Intelligence, and never left ownerless") |
| Behaviour-free | **Confirmed** (Frozen Property 1) |
| Not a reasoning engine | Confirmed, but only **implicitly** — Property 5 ("grants no... reasoning... authority of its own") covers this; no explicit exclusion is stated |
| Not a registry, router, provider selector, or workflow model | **Not explicitly stated anywhere in the amended text.** The Study's own §5 ("Explicit Exclusions") named all four of these individually — including a specific disambiguation that `ReasoningSubject` "never chooses, ranks, or filters among multiple configured `ReasoningProvider` implementations," distinguishing *subject* selection from *provider* selection. None of the Study's §5 content was carried into the Contract Design amendment. The Frozen Properties actually adopted (behaviour-free; grants no authority; not a generic union) jointly entail these exclusions logically, but the document itself never says so, and the "selector" terminology used for `ReasoningSubject` is left exposed to exactly the provider-selector conflation the Study took care to rule out by name. |
| Not a general-purpose union | **Confirmed, explicitly** (Frozen Property 7) |
| `ReasoningProvider` unchanged in identity and signature | **Confirmed** (Section 1 untouched) |
| `ReasoningProviderResponse` unchanged | **Confirmed** (Section 3 untouched in full) |
| Conversation Engine retains exclusive ownership/construction of `Turn`/`Conversation` | **Confirmed** (Section 5 untouched; banner and `OfTurn` bullet both reaffirm) |
| Evidence Intelligence retains ownership of `EvidenceAnalysisRequest` | **Confirmed** (Section 4 table: "owned by Evidence Intelligence, reused here unmodified") |

---

## 4. Contract-Count and Consistency Findings

Checked across the Summary, Section 2, Section 4, Section 6, Section 10, Section 11, Conclusion, and Related: **all counts touched by Amendment 1 agree with each other** — five new contracts, six existing reused, one modified, consistently stated in the Summary, Section 10, and the Conclusion, with matching type names in every location. The Section 4 table's own "six" figure correctly continues the pre-existing convention (established before this amendment) of excluding `PlanningRequest`/`OutboundParkerResponse` from that tally, since those two were already categorised separately ("referenced, not constructed") before Amendment 1 — not a new inconsistency.

**One pre-existing, unrelated defect noted for completeness, not attributable to Amendment 1:** the Conclusion's "eleven deferred items" does not match Section 9's actual count of twelve bulleted items. This mismatch exists in text the Amendment Proposal correctly left untouched and predates Amendment 1 entirely; it is out of this amendment's own scope and does not affect this review's verdict on Amendment 1 itself, but is disclosed here since the task asked for every count to be checked.

**One incompleteness found in Section 7's own verification claim:** Section 7 states "neither `ReasoningProviderRequest` nor `ReasoningProviderResponse` references `PlannerRuntime`, `AgentRuntime`... anywhere, at any depth (`Turn` and its own transitively-embedded types were already verified free of any such dependency...)" — this sentence was not updated to also name `EvidenceAnalysisRequest` as a second transitively-embedded type now requiring the identical verification. The underlying guarantee still holds (`EvidenceAnalysisRequest`'s own fields — `analysisKind`, `requestingPrincipalId`, `evidenceArtifactIds`, `memoryCoreReferences`, `reasoningContext` — reference none of the forbidden subsystems), but the document's own stated verification is now incomplete relative to what it actually claims to guarantee.

---

## 5. Dual ReasoningContext Analysis (Mandatory)

**A. Are the two fields semantically distinct under existing governance? No.**

`ReasoningProviderRequest.reasoningContext` (Section 2, unchanged text) is described as: an already-assembled working set a `ReasoningProvider` reasons over, supplied by the caller. `EvidenceAnalysisRequest.reasoningContext` (`src/interfaces/EvidenceIntelligence.kt`, unchanged by this amendment) is described, verbatim: "An optional, already-assembled `ReasoningContext`... supplied only when this analysis internally invokes one or more `ReasoningProvider`s." Both descriptions name the identical role — the context passed into a `ReasoningProvider` invocation — attached to two different structural positions that, before this amendment, could never be simultaneously reachable from one `ReasoningProviderRequest`. Amendment 1 is what first makes both reachable from a single request at once (`ReasoningProviderRequest.reasoningContext` directly; `EvidenceAnalysisRequest.reasoningContext` two levels down, via `subject.request.reasoningContext`, once `subject` is `OfEvidenceAnalysisRequest`). Notably, the amended Section 2's own sentence introducing `OfEvidenceAnalysisRequest` ("Carries Evidence Intelligence's own already-governed evidence-artefact references, Memory Core references, analysis classification, and requesting principal") does not even mention that the wrapped type carries its own `reasoningContext` field — an omission that itself suggests the collision was not noticed during drafting.

**B.** Not applicable — the fields are not distinct.

**C. Failure modes live in the amended contract, all five:**

- **Precedence** — unaddressed. No text states which field a `ReasoningProvider` implementation, or the composing caller, should treat as authoritative if both are populated.
- **Duplication** — unaddressed. Nothing requires, forbids, or even acknowledges that both fields might carry identical content.
- **Conflict** — unaddressed. No structural or textual rule prevents the two fields containing contradictory prose entries for the same invocation.
- **Enrichment** — unaddressed. No rule states whether an implementation should merge the two, or that one is meant to supplement rather than replace the other.
- **Omission** — unaddressed, and structurally asymmetric: `EvidenceAnalysisRequest.reasoningContext` is nullable/optional, while `ReasoningProviderRequest.reasoningContext` is mandatory. A well-typed request can populate either field while leaving the other empty, with no stated rule for which pattern is correct or what an implementation should do in either case.

**D. This is not safely ordinary implementation discretion; the Contract Design must state an invariant.** Three reasons, not conclusory:

1. It is a genuinely **new** structural possibility, not a pre-existing, already-deferred one. The Reasoning Provider Contract Design's existing deferral ("How Memory and World Model excerpts become these entries... remains entirely out of scope") concerns *how one `ReasoningContext` is assembled* — a single-field question. It was never written to cover *what happens when two independently-constructed `ReasoningContext` values are simultaneously reachable from one request* — a scenario that could not arise before Amendment 1 created it.
2. Left unresolved, different concrete `ReasoningProvider` implementations could reasonably make different, incompatible choices (one honours the outer field, another the inner, a third concatenates both) for the identical request shape — precisely the provider-dependent behavioural divergence the Constitution's Replaceable Reasoning Providers principle exists to foreclose ("Parker's authority, safety guarantees, and behavioral contracts do not depend on which provider is plugged in").
3. It is a structural-type ambiguity, not an algorithmic one, and this repository's own established discipline (e.g., the `GCR-EI-UNIT2-001` remediation for `EvidenceRetrievalResult`) treats exactly this class of silently-reachable inconsistency as something requiring an explicit, stated resolution rather than being left to convention.

**Classified as a review blocker.** The smallest required governance correction is a stated invariant in Section 2 (or a restated one in Section 6/8) fixing the relationship between `ReasoningProviderRequest.reasoningContext` and `EvidenceAnalysisRequest.reasoningContext` once `subject` is `OfEvidenceAnalysisRequest` — for example, declaring one field authoritative and the other to be treated as either absorbed into it or structurally forbidden from independently diverging. This review does not propose which resolution is correct, per instruction.

---

## 6. Compatibility Findings

**Conversation Engine migration:** clean. `OfTurn(turn: Turn)` wraps `Turn` unmodified; the one-line call-site change required (wrapping `disposition.turn` in `ReasoningSubject.OfTurn`) fabricates no data, loses no structure, changes no ownership, and `ReasoningProviderResponse` (Section 3) is untouched.

**Evidence Intelligence usage:** clean on three of four checks — no Conversation Engine dependency is added (confirmed in the banner, Section 5, Section 7); no synthetic `Turn` is required; Unit 4's own output discipline is untouched (this amendment governs only the request side). On the fourth check — "introducing another public contract" — `ReasoningSubject` is a new public type, but it is owned by the Reasoning Provider Contract Design, not by Evidence Intelligence, so it does not violate Evidence Intelligence's own "exactly four new public types" freeze (Scope Lock §4). This is correct as far as it goes, but is inseparable from the Dual ReasoningContext finding above: Evidence Intelligence's own already-existing `EvidenceAnalysisRequest.reasoningContext` field is what creates the ambiguity once it is reachable through this new contract.

---

## 7. Constitutional Findings

- Implements the Decision Memorandum: **confirmed**, accurately cited throughout.
- Does not reinterpret the Constitution: **confirmed** — the Constitutional Boundaries section is untouched, and no Constitution text is cited differently than before.
- Does not alter any operative CDR-007 decision: **confirmed** — CDR-007's Decision Rule ("not broadened... to describe Evidence Intelligence") is not violated, since Evidence Intelligence never implements `ReasoningProvider` under this amendment. CDR-007's own Repository Reuse Summary line describing `ReasoningProvider` as "unmodified" is now stale, but this staleness was already identified and accepted as a routine, non-blocking currency correction in the Memorandum and Follow-up Review, not something this amendment needed to resolve itself.
- Remains a Contract Design evolution only: **confirmed** — `git status` shows only `REASONING_PROVIDER_CONTRACT_DESIGN.md` as modified; no Scope Lock or Implementation Plan was touched.

---

## 8. Final Verdict

**C — AMENDMENT REQUIRES GOVERNANCE CORRECTION**

The verdict rubric itself gates Verdict A on "the dual-ReasoningContext issue is already unambiguous" — Section 5 of this review demonstrates it is not. This alone is sufficient to exclude Verdict A. It is a substantive contract-meaning defect (a genuine, unaddressed structural ambiguity reachable by any caller constructing `ReasoningSubject.OfEvidenceAnalysisRequest`), not a wording or formatting issue, which excludes Verdict B as the primary classification. The smallest required correction is narrow (Section 5.D, above) and does not require reopening CDR-007, the Evidence Intelligence Contract Design, or any constitutional-tier document — but it does require a further, targeted amendment to this Contract Design before the document can be considered internally consistent.

Secondary, non-blocking findings that should accompany that same correction pass, since they are already identified and inexpensive to fix alongside it: the omitted explicit exclusions (registry/router/provider-selector/workflow-model, Study §5) should be carried into Section 2; the premature "**Applied**" self-declaration should be revised to reflect that acceptance follows, not precedes, independent constitutional review; and Section 7's dependency-freedom verification sentence should name `EvidenceAnalysisRequest` alongside `Turn`.

---

## 9. Confirmation No File Changed

No file was edited in the course of this review. `docs/architecture/REASONING_PROVIDER_CONTRACT_DESIGN.md` was read only.

## 10. No Git Actions

No git command beyond read-only status verification was run. Nothing was staged, committed, or pushed.
