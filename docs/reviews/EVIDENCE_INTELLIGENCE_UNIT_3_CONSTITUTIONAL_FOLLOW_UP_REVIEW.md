# Evidence Intelligence Unit 3 — Constitutional Follow-up Review: An Attempt to Falsify Option 1

This is analysis only. No Kotlin was written, no governance document was amended, no production code or test was touched, and no Git action was performed. The prior remediation analysis on disk (`docs/reviews/EVIDENCE_INTELLIGENCE_UNIT_3_CONSTITUTIONAL_REMEDIATION_ANALYSIS.md`) was **not modified** — this review's findings are presented here only, as instructed.

**Method note, stated up front for transparency:** this review required re-reading `docs/architecture/reasoning-context.md` in full (previously only cited secondhand) and re-grepping `CDR-007` for the literal string `Turn` (previously not checked). Both produced evidence that materially changes the confidence level of the prior analysis's CDR-required conclusion for Option 1. This is disclosed honestly below rather than smoothed over.

---

## 1. Repository Evidence Reviewed

- `docs/architecture/parker-constitution.md` — re-read for its exact "Cognition proposes" language
- `docs/architecture/reasoning-context.md` — **read in full for the first time in this review chain**; its `Status:` header ("Constitutional — subordinate to `parker-constitution.md`") had not previously been weighed
- `docs/architecture/REASONING_PROVIDER_ARCHITECTURE.md` — re-read, specifically §1 (Purpose) and Status
- `docs/architecture/REASONING_PROVIDER_CONTRACT_DESIGN.md` — re-read, specifically §2 and its own Status header
- `src/interfaces/ReasoningProvider.kt` — `Turn`/`ReasoningProviderRequest` KDoc re-read
- `docs/decisions/CDR-007_CONSTITUTIONAL_CLASSIFICATION_OF_EVIDENCE_INTELLIGENCE.md` — **re-grepped for the literal string `Turn`** (zero hits — new finding); both "not broadened" clauses re-read in exact grammatical context
- `docs/architecture/EVIDENCE_INTELLIGENCE_CONTRACT_DESIGN.md`, `EVIDENCE_INTELLIGENCE_SCOPE_LOCK.md`, `EVIDENCE_INTELLIGENCE_IMPLEMENTATION_PLAN.md` — grepped for `Turn` (one incidental hit, analysed below)
- `docs/decisions/CDR-004_CONSTITUTIONAL_CLASSIFICATION_OF_PROVENANCE_IDENTIFIER_RESOLUTION.md` — opening sections re-read, to test whether its "generalise without reopening" precedent actually supports skipping a CDR (it does not, cleanly — see §7 below)
- `src/interfaces/EvidenceIntelligence.kt` (`CandidateMemoryCoreRecord`) — re-examined as the closest structural precedent for a closed selector type
- `docs/architecture/CONVERSATION_CONTINUITY_CONTRACT_DESIGN.md` §6 — re-examined as precedent for additive interface evolution

---

## 2. Exact Governing Clauses Relied Upon

**Constitution (highest authority):**
> "Cognition proposes. A reasoning provider — whichever model or engine is configured — **interprets a request** and proposes an action, a draft, or a plan."

No mention of Turn, Conversation, or conversational context anywhere in the Constitution's treatment of reasoning providers.

**`reasoning-context.md` (Status: "Constitutional — subordinate to `parker-constitution.md`" — the same tier marker Epistemic Integrity Amendment No. 1 carries, one tier below the Constitution itself and above every Architecture/Contract Design document reviewed elsewhere in this Programme):**
> "Reasoning Context — 'What matters for the current **task**.' ... the temporary, **task**-scoped working set that a reasoning provider actually reasons over. It is assembled specifically for the **task** at hand..."
> "When a **task** begins, the relevant portions of Memory and the World Model are assembled into a fresh Reasoning Context scoped to that **task**. The reasoning provider — whichever model or engine is configured — reasons over that context and produces a proposal."
> "Reasoning providers are responsible for reasoning over the Reasoning Context they are given and producing a proposal."

The word used throughout, at this tier, is **"task"** — never "Turn," never "Conversation." This document defines the entire three-layer knowledge architecture (Memory / World Model / Reasoning Context) that everything downstream — including `ReasoningProvider` — is a specialisation of, per its own "Relationship to Existing Parker Components" section: *"This document specializes the cognition stage of the Parker Constitution... by defining precisely what cognition is given to reason over."*

**`REASONING_PROVIDER_ARCHITECTURE.md` (Stage 1, PES-001 — a lower, non-constitutional tier):**
> "A Reasoning Provider is the replaceable service that **interprets conversational context** and produces reasoning output — nothing else."
> "Concretely, it must fit cleanly between the Conversation Engine (which invokes it) and Planner Runtime..."

Its own Status section states plainly why it was scoped this way:
> "Produced as Milestone 2 of the roadmap the most recent governance review named. That review found the missing reasoning-provider contract to be the highest remaining architectural dependency within the communication/conversation track... This document is the Stage 1 prerequisite that dependency needs."

**`REASONING_PROVIDER_CONTRACT_DESIGN.md` §2** gives `Turn` its field-level place in `ReasoningProviderRequest` citing the Architecture document above as its authority — a derivative, not an independent, decision.

**`CDR-007` — the literal word "Turn" does not appear anywhere in this document** (confirmed by direct grep, zero matches). Its two "not broadened" clauses, quoted exactly:

> §1 (narrative): "The `ReasoningProvider` abstraction itself is **not broadened, extended, or reinterpreted by this record**: it remains exactly what the Reasoning Provider Contract Design and `src/interfaces/ReasoningProvider.kt` already define it to be..."

> Decision Rules (operative): "the `ReasoningProvider` abstraction itself is not broadened, extended, or reinterpreted **to describe Evidence Intelligence**."

Both are grammatically narrow: the first disclaims that *this record* does any broadening (a statement about CDR-007's own restraint, not a permanent prohibition on any future document); the second forbids broadening *specifically so as to describe/classify Evidence Intelligence as a Reasoning Provider* (an identity/classification guard, not a field-shape freeze).

**CDR-007's Decision block, the clause actually on point for Option 3:**
> "A new Reasoning Provider abstraction **specific to Evidence Intelligence** — Not adopted; the existing ReasoningProvider interface is adopted unmodified, and Evidence Intelligence is classified as its orchestrator where used, never as an instance of it."

---

## 3. Is Turn Fundamentally the Subject of Reasoning, or the First Concrete Subject?

**Answer: the first concrete subject, not the fundamental one — demonstrated, not inferred, by the tier hierarchy itself.**

The governing documents form a clean chain of progressively narrower framing:

| Tier | Document | Language used for "what reasoning operates over" |
|---|---|---|
| Constitution (highest) | `parker-constitution.md` | "a request" |
| Constitutional (2nd tier) | `reasoning-context.md` | "a **task**" / "the current task" |
| Architecture (Stage 1, PES-001) | `REASONING_PROVIDER_ARCHITECTURE.md` | "**conversational** context" / "a Turn's content" |
| Contract Design (Stage 2A) | `REASONING_PROVIDER_CONTRACT_DESIGN.md` | `turn: Turn` (field-level) |

The narrowing happens at exactly one point — the transition from the Constitutional tier (`reasoning-context.md`, "task") to the Architecture tier (`REASONING_PROVIDER_ARCHITECTURE.md`, "conversational context"/"Turn") — and that document's own Status section explains why: it was commissioned specifically to unblock Conversation Engine's own named dependency ("the highest remaining architectural dependency within the **communication/conversation track**"), not derived from a first-principles definition of what reasoning is for Parker generally. Nothing at the Constitutional tier was ever amended to narrow "task" down to "Turn." An Evidence Intelligence analysis invocation is, in the ordinary sense `reasoning-context.md` uses the word, unambiguously **a task** — bounded, scoped to one invocation, discarded once concluded, exactly matching that document's own definition of what Reasoning Context is assembled for.

**This survives the falsification attempt.** If `reasoning-context.md` had used "Turn" or "conversational exchange" instead of "task," this finding would collapse — it does not.

---

## 4. Does Option 1 Broaden the Abstraction, or Correct an Assumption?

**Answer: it corrects an assumption, and this is demonstrable strictly from governance, not asserted.**

The chain in §3 shows the "subject must be a Turn" requirement was introduced at the Architecture tier as a byproduct of *who happened to be the only caller at the time that document was written*, never as a considered restriction on what reasoning conceptually is. The Architecture document's own Purpose section states "'Reasoning Provider' names a role, not a product" and insists on model-independence — deliberate generality on the *implementation* axis (which model, local/remote) — while narrowing on the *subject* axis (conversational Turns only) without ever arguing that the second narrowing was principled rather than incidental. No governing document anywhere states, as a reasoned position, "the reasoning subject must always be a Turn because reasoning is inherently conversational." That claim is nowhere made; it is only ever implicit in one Stage-1 document's own scoping decision, made for a different, narrower reason (closing one specific named dependency).

CDR-007 itself already implicitly relied on a broader reading than "Turn-only" reasoning when it authorised Evidence Intelligence — an unambiguously non-conversational subsystem — to orchestrate `ReasoningProvider` "as internal analytical mechanisms... a pattern mirroring `ConversationTurnReasoningCoordinator`'s own existing use." CDR-007 never paused to ask how `ReasoningProviderRequest.turn: Turn` would actually be populated for this authorised use — because CDR-007 operates at the classification tier and never inspects field-level shapes (consistent with it never mentioning "Turn" at all). The conceptual broadening (reasoning as applicable to analytical, non-conversational tasks) was already made, at a higher governance tier, the moment CDR-007 authorised this orchestration. Option 1 does not introduce that broadening — it belatedly reconciles the field-level contract with a conceptual scope CDR-007 already assumed.

---

## 5. Exact CDR-007 Clauses Requiring Amendment (If Any)

Separating precisely, as the review demands:

| CDR-007 element | Type | Affected by Option 1? |
|---|---|---|
| "Evidence Intelligence — classified as a first-class, downstream, analytical subsystem, peer to Evidence Custodian and Knowledge Memory" | Architectural role | **No.** Unaffected. |
| Exclusive-responsibility table (§5): custody/registration/promotion ownership | Ownership | **No.** Untouched — Option 1 changes nothing about who owns custody, registration, or promotion. |
| "One or more Reasoning Providers (optional, internal), through the already-frozen `ReasoningProvider` interface" (§3) | Dependency | **No.** The dependency edge (Evidence Intelligence → `ReasoningProvider`) and the interface signature `reason(request): response` are both unchanged under Option 1. |
| Repository Reuse Summary: "`ReasoningProvider` (as an unmodified, orchestrated abstraction...)" | Public contract (narrative/descriptive) | **Yes, but only as a currency update.** This is a descriptive record of the state of affairs when CDR-007 was written, not itself an operative Decision Rule. It would need updating to remain accurate, the same way any changelog needs updating — this is not "reopening a decision," it is correcting a now-stale factual statement. |
| "the `ReasoningProvider` abstraction itself is not broadened, extended, or reinterpreted **to describe Evidence Intelligence**" (Decision Rules) | Implementation assumption about identity, not request shape | **No.** Option 1 never classifies Evidence Intelligence as a `ReasoningProvider`; it remains, exactly as before, an orchestrator that calls `reason(...)`, never an implementer of the interface. |
| "A new Reasoning Provider abstraction **specific to Evidence Intelligence** — Not adopted" (Decision block) | Public contract / architectural role | **No, for Option 1 specifically** — a shared, sealed subject type used identically by Conversation Engine and Evidence Intelligence alike is definitionally not "specific to Evidence Intelligence"; it is general to the Reasoning Provider tier. (This clause is squarely what **Option 3** would need to reverse — see §6.) |

**Conclusion: no operative CDR-007 Decision Rule requires amendment for Option 1.** Only a narrative/descriptive line in the Repository Reuse Summary needs updating for currency — a routine correction, not a constitutional reopening.

---

## 6. Detailed Comparison of Options 1 and 3

| Axis | Option 1 (Sealed Subject) | Option 3 (Split Contract) |
|---|---|---|
| Constitutional consistency | No operative CDR-007 Decision Rule is violated (§5); only a Contract-Design-tier amendment plus a narrative currency fix | Directly reverses an explicit "Not adopted" Decision — the single most serious governance-consistency cost of any option surveyed |
| Ownership clarity | Clean, and precedented: mirrors `CandidateMemoryCoreRecord`'s own frozen shape exactly — "a closed, two-case selector type... owned by [one subsystem]... creates no independent dependency entitlement for any other subsystem" (`src/interfaces/EvidenceIntelligence.kt`) | Also clean (a wholly new, unambiguously-owned contract) — but doubles the number of Reasoning-Provider-shaped contracts a maintainer must reconcile |
| Dependency impact | Zero new dependency edges; Evidence Intelligence's three-row dependency table (Contract Design §12) is untouched in structure, only the referenced request type's internal shape changes | Evidence Intelligence's dependency table gains a wholly new contract, replacing its `ReasoningProvider` row rather than amending it |
| Implementation complexity | One-time, compiler-enforced migration of existing call sites (Kotlin's exhaustive `when` over a sealed class cannot silently miss a case); no perpetual duplicate stack | Zero short-term migration risk to existing, shipped code — but a permanent, second implementation stack (its own prompt-construction logic, its own tests, its own maintenance) must be built and kept reconciled with the first, forever |
| Governance impact | Contract Design amendment + Scope Lock dependency-table note + a narrative correction to CDR-007's Repository Reuse Summary; a CDR is advisable for process discipline but not compelled by any operative clause (§7) | Requires CDR-007's own Decision block to flip from "Not adopted" to "Adopted" — unambiguously requires a full Independent Constitutional Review cycle, the heaviest process cost of any option surveyed |
| Long-term architectural convergence | A single, unified `ReasoningProvider` abstraction remains platform-wide, interchangeable across every caller — directly realises the Constitution's "replaceable reasoning providers" principle at the caller level, not merely the implementation level | Risks normalising "one bespoke Reasoning-Provider-shaped contract per subsystem" — a pattern that does not converge as more callers appear |
| Consistency with Parker's existing architectural patterns | Directly precedented: (a) sealed selectors (`CandidateMemoryCoreRecord`, `ReasoningProviderResponse`, `EvidenceAnalysisResult`, `MemoryCoreRecord`) are this repository's dominant idiom for "one of several existing, unmodified types, closed and behaviour-free"; (b) additive interface evolution is directly precedented by `CONVERSATION_CONTINUITY_CONTRACT_DESIGN.md` §6, which added a second operation to `ConversationEngine` "additively... not a reassignment... not a redesign," without requiring a fresh CDR | Cuts against this repository's own repeated, explicit design rule: "prefer reuse; prefer composition... avoid duplication... avoid mirrors... avoid shadow models" (stated verbatim in the Evidence Intelligence Contract Design's own Design Rules Compliance section) — a second Reasoning-Provider-shaped contract is close to the literal thing "avoid mirrors" warns against |

**The one axis where Option 3 is genuinely not dominated:** zero short-term touch to already-shipped, already-tested Conversation Engine code (`ModelReasoningProvider`, `LoggingReasoningProvider`, `ConversationTurnReasoningCoordinator`, and their tests). This is a real, non-trivial advantage — but it is an implementation-risk axis, not a constitutional one, and it is offset by a permanent duplication cost Option 1 never incurs.

---

## 7. Does Option 1 Constitutionally Dominate Option 3?

**Yes, on every constitutional/governance axis; the dominance claim is qualified only by one non-constitutional axis, disclosed rather than concealed.**

Applying the task's own test — "at least as strong on every constitutional axis while materially stronger on one or more":

- **Constitutional consistency:** Option 1 strictly stronger (no operative Decision Rule violated vs. an explicit reversal required).
- **Ownership clarity:** Materially equal — both are clean, both precedented.
- **Dependency impact:** Option 1 strictly stronger (zero new dependency edges vs. a wholly new contract).
- **Governance impact:** Option 1 strictly stronger (Contract Design amendment vs. full CDR reversal).
- **Long-term architectural convergence:** Option 1 strictly stronger (platform-wide interchangeability vs. a diverging, per-subsystem pattern).
- **Consistency with Parker's existing architectural patterns:** Option 1 strictly stronger (directly precedented by `CandidateMemoryCoreRecord` and the Continuity Contract Design's additive-extension precedent; Option 3 cuts against an explicitly stated design rule).
- **Implementation complexity:** Not strictly dominant — Option 3 has lower short-term migration risk; Option 1 has lower long-term duplication cost. This is the one axis preventing an unqualified "dominates on every single axis without exception" claim.

Because the one axis where Option 1 is not strictly ahead (short-term implementation-touch) is an engineering-cost axis rather than a constitutional one, and because Option 1 is strictly ahead on every axis that is constitutional in nature (consistency, ownership, dependency, governance process, convergence, precedent-alignment), **the honest conclusion is that Option 1 constitutionally dominates Option 3**, with the single non-constitutional trade-off disclosed rather than hidden.

**This is a reversal of the prior remediation analysis's "no option is objectively dominant" verdict, specifically as between Options 1 and 3.** That verdict rested on an assessment that Option 1 "very likely" required a CDR amendment of comparable weight to Option 3's. Direct re-reading of CDR-007's exact grammatical objects (§2, §5 above) and of `reasoning-context.md`'s own Constitutional-tier text shows that assessment was too conservative. The second independent review's challenge is, on this evidence, **well-founded**, and the correction runs in Option 1's favour.

---

## 8. Is a CDR Required?

**Not for Option 1, as a matter of operative constitutional necessity — advisable as a matter of process discipline, and self-certifiable rather than mandatory.**

Re-evaluating precisely, per the review's own framing: does Option 1 change **the architectural role of Evidence Intelligence**, or only **the request subject carried by the existing Reasoning Provider contract**?

**Only the latter.** Evidence Intelligence's classification (first-class, downstream, analytical subsystem, peer to Custodian/Knowledge Memory, bound by Epistemic Integrity, no custody/promotion/truth authority) is completely untouched by Option 1. What changes is a single internal field of `ReasoningProviderRequest` — a type owned by the Reasoning Provider Contract Design, not by CDR-007 or the Evidence Intelligence Contract Design. This distinction matters directly to governance tier: CDR-007's own Constitutional Constraints section states that a model "that does not force reopening an already-frozen document without a repository-grounded reason is preferred over one that does" (the minimal-reopening discipline it inherits from CDR-004's own precedent). Since no operative CDR-007 clause is violated (§5), reopening CDR-007 as a full Decision-block matter is not compelled — it would be reopening without the "repository-grounded reason" this Programme's own discipline requires.

The closer, more honest analogy is `CONVERSATION_CONTINUITY_CONTRACT_DESIGN.md` §6's own additive extension of `ConversationEngine` — a genuine interface change, handled entirely at Contract-Design tier, explicitly justified as "an additive extension of *how many operations* the same, single owner exposes... not a reassignment... not a redesign," with no fresh CDR. Option 1's change to `ReasoningProviderRequest` is the same shape of act: additive, non-narrowing, leaving the interface signature and every existing constitutional obligation intact.

Where a CDR-tier act still has value: this repository's own practice (CDR-004) is to have genuinely contested or non-obvious "does this require reopening" questions settled by a Constitutional Decision Record rather than assumed unilaterally by a Contract Design author. Given that a prior review (this Programme's own previous remediation analysis) reached the opposite conclusion, this question is evidently non-obvious enough that CDR-005's own **Model C self-certification pattern** — "self-certification against Chapter 10's published criteria, escalating to a further CDR only if genuinely contested" — is the appropriate, proportionate governance act: a disclosed, reasoned self-certification (of the kind this review itself constitutes) that no operative CDR-007 clause is violated, filed alongside the Contract Design amendment, escalating to a full CDR only if that self-certification is contested. A full, substantive CDR reversal — the kind Option 3 requires — is not warranted for Option 1.

---

## 9. Final Constitutional Recommendation

**Option 1 survives the falsification attempt and is recommended as constitutionally superior to Option 3**, on the evidence assembled in this review:

1. Reasoning's conceptual subject is defined, at the Constitutional tier (`reasoning-context.md`), as a **task** — a strictly more general concept than `Turn`, which was introduced one tier lower, for a historically contingent reason (Conversation Engine being the Architecture document's one named client), never as a considered restriction on what reasoning fundamentally operates over.
2. Option 1 therefore **corrects an implementation assumption**, rather than broadening a deliberately-scoped abstraction.
3. CDR-007 never mentions `Turn` and its two "not broadened" clauses are both grammatically narrow — one disclaiming CDR-007's own restraint, one guarding against Evidence Intelligence's *identity* being redefined as a Reasoning Provider. Neither reaches the internal field shape of `ReasoningProviderRequest`.
4. Option 1 is directly precedented by this repository's own dominant idioms — closed selector types (`CandidateMemoryCoreRecord`) and additive, non-reopening interface evolution (`CONVERSATION_CONTINUITY_CONTRACT_DESIGN.md` §6) — while Option 3 cuts against an explicitly stated design rule ("avoid mirrors... avoid shadow models") and requires reversing a named CDR-007 rejection outright.
5. Option 1 constitutionally dominates Option 3 on every governance axis, qualified only by a disclosed, non-constitutional, one-time implementation-migration cost that is more than offset by avoiding Option 3's permanent duplicate-contract maintenance burden.
6. A CDR is not compelled for Option 1; a disclosed, self-certified confirmation (mirroring CDR-005's Model C pattern) is the proportionate governance act, escalating to a full CDR only if contested.

This finding supersedes, specifically with respect to the Option 1 vs. Option 3 comparison, the prior remediation analysis's "no option is objectively dominant" conclusion. The prior analysis's treatment of Options 2, 4, 5, and 6 as dominated or weakened is unaffected by this review and stands as previously stated.

---

**No Kotlin was written. No governance document was amended. Nothing was implemented. No file was modified — including the previously saved remediation analysis, which remains exactly as written. No Git action was performed.**
