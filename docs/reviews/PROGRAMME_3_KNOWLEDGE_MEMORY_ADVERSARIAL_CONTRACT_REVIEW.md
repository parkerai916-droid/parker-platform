**Status:** Adversarial architectural review. Read-only. This review does not modify `docs/architecture/`, `docs/governance/`, production code, or tests. Nothing is staged, committed, or pushed. This is not an editorial pass — it is a deliberate attempt to break `docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_CONTRACT_DESIGN.md`, assuming a competent, well-intentioned implementation team building exactly to the letter of that document.

# Programme 3 — Knowledge Memory Adversarial Contract Design Review

Subject reviewed: `docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_CONTRACT_DESIGN.md`, in light of `docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_GOVERNANCE_REVIEW.md`, `docs/architecture/parker-constitution.md`, `user-authorship-and-evidence.md`, `reasoning-context.md`, `epistemic-integrity.md`, `MEMORY_CORE_GOVERNANCE_REVIEW.md`, `MEMORY_CORE_CONTRACT_DESIGN.md`, `docs/reviews/EPISTEMIC_INTEGRITY_EXECUTABLE_TEST_COMPLIANCE_AUDIT.md`, and `docs/reviews/EPISTEMIC_INTEGRITY_CONSTITUTIONAL_REMEDIATION_PLAN.md`. All treated as authoritative and unmodified by this review. No new constitutional requirement is introduced anywhere below — every finding is tested against an Article, a prior audit finding, or an internal inconsistency in the Contract Design's own text, never against a standard invented for this review.

---

## 1. Executive Assessment

- **Internally coherent?** Not fully. Section 7 of the Contract Design asserts a "single Trust boundary, not a second one," while its own text in the same section describes two distinct permission evaluations (once at Memory Core's boundary, once at Knowledge Memory's own submission boundary). The design may be defensible, but the claim of a *single* boundary is inaccurate against the design's own description — a genuine internal inconsistency, not merely an ambiguity (Section 4, below).
- **Constitutionally coherent?** The Contract Design's stated intent does not contradict any Article. But intent is not the same as constraint: several sections explicitly defer constitutionally load-bearing decisions to "implementation, not design" (most consequentially, promotion-criteria weighting, Section 5) without first establishing the constitutional boundary that deferral must still respect. A contract that is constitutionally *coherent in aspiration* but leaves the actual constitutional guarantee unenforced is not yet constitutionally *sufficient*.
- **Implementable?** Yes. Every concept in the Contract Design is concrete enough to build against; nothing is so abstract that a team could not begin.
- **Sufficiently constrained?** No. Section 9 of this review identifies at least five independent points at which two competent, compliant implementations could diverge in ways that matter constitutionally, not merely stylistically.

**Summary determination:** the architecture is sound and the Contract Design's own governing principle ("Memory Core remains the constitutional source of stored evidence; Knowledge Memory becomes the constitutional source of promoted knowledge") is not violated by anything designed. What is missing is not a redesign but a set of tightening amendments, concentrated at exactly the seams the Contract Design's own Final Recommendation flagged as candidates for adversarial scrutiny — this review confirms three of those four candidate seams are genuine gaps, not merely hypothetical ones, and finds several further gaps the Contract Design did not itself anticipate.

---

## 2. Responsibility Leakage

**Toward Memory Core.** The Knowledge Promotion record (Contract Design §2, §12) is described as documenting "the basis on which a Knowledge Item was promoted... including which Memory Core evidence justified it." Nothing in the contract limits this disclosure to identifiers. An implementation could embed descriptive content about the cited evidence directly inside the Promotion record — a summary of what the evidence said, not merely a pointer to it — which would be a second, Knowledge-Memory-native copy of content Memory Core already owns, dressed as a "disclosure" rather than a "duplicate." This is genuine leakage risk, not merely permitted phrasing: the contract's own Section 1 forbids Knowledge Memory from "duplicating Memory Core's responsibilities," but nowhere constrains the Promotion record's shape tightly enough to prevent exactly that.

**Toward Reasoning Context.** The Knowledge Query capability (Contract Design §2, §6) is described as expressing "a task-scoped request for relevant promoted knowledge." The word "relevant" is unconstrained. Two readings are equally defensible from the contract's own text: (a) Reasoning Context determines relevance and Knowledge Memory merely filters by explicit, caller-supplied criteria, or (b) Knowledge Memory itself interprets a task description and judges relevance. Reading (b) would hand Knowledge Memory a cognitive, interpretive function — deciding what matters for a task — that `reasoning-context.md` assigns to Reasoning Context assembly, not to a memory layer. This is the most serious responsibility-leakage risk identified in this review: relevance-judgment is reasoning, and the Contract Design does not say clearly enough that Knowledge Memory performs no interpretation of a task, only structural matching against caller-supplied criteria.

**Toward World Model.** No direct dependency exists (correctly forbidden, Contract Design §9). But the "competing propositions" capability (§3, §6) risks a subtler leakage: nothing prevents an implementation from treating a Knowledge Item's evidential-state classification as a *current-belief* indicator — effectively reproducing World Model's "what is true right now" function inside a "what has been learned" layer — provided it never literally calls a World Model contract. The Contract Design defines the boundary by dependency, not by function; a functionally World-Model-like behavior that never technically depends on World Model would not violate the letter of Section 9 while still violating its purpose. Worth flagging as a lower-severity, wording-level gap rather than a structural defect.

**Toward Document Intelligence.** No leakage found. Section 10's explicit absence of any document-named concept in the Contract Inventory is a genuine, verifiable structural guarantee, not merely a stated intention — this is the one boundary in this section that survives adversarial reading cleanly.

**Toward Representation.** No leakage found. Every disclosure obligation in the Contract Design (promotion traceability, evidential-history retrieval) is phrased as data being made *retrievable*, never as Knowledge Memory composing explanatory prose. This boundary holds.

---

## 3. Promotion Boundary

- **Can an implementation promote unsupported knowledge?** Yes, as currently worded. Section 5 explicitly declines to specify how promotion factors are weighed ("how those factors are weighed is implementation, not design"). Nothing in the contract requires the resulting evidential-state classification's confidence to be proportionate to what the underlying Memory Core evidence actually supports. An implementation could promote on a single weak submission and classify it at a strong evidential state, and no sentence in the Contract Design is violated by doing so.
- **Can an implementation infer truth?** No — this boundary holds. Knowledge Item's lifecycle vocabulary (promoted / superseded / retired / contradicted) contains no "verified" or "true" status, mirroring Memory Core's own Assertion design. This is a genuine, structural protection, not merely a stated intention.
- **Can an implementation discard uncertainty?** Yes, as currently worded. Section 11's guarantee that evidential state "may" express "insufficiently supported/unresolved" uses permissive language, and the Contract Design never mandates that whatever evidential-state representation is eventually designed must be *capable* of expressing that outcome at all. An implementation using a bare confidence scalar with no "unresolved" value would not violate any binding sentence in this Contract Design as written.
- **Can an implementation silently strengthen evidence?** Yes, and this is the review's most serious finding. Section 5 lists "repetition" and "frequency" as promotion factors, inherited unchanged from the legacy `MemoryPromotionPolicy`. Article XI requires distinguishing independent corroboration from repeated accounts sharing a common origin. Nothing in the Contract Design requires promotion to make that distinction — an implementation could promote something more confidently purely because it was mentioned often, even if every mention traces to the same single original source, which is precisely the corroboration-independence failure Article XI exists to prevent, and precisely the kind of single/unweighted-factor pattern the Compliance Audit already found, and condemned, in the World Model (CT-EI-48). This Contract Design, as written, permits the same defect to be built fresh into Knowledge Memory.
- **Can an implementation bypass provenance?** No — this boundary holds structurally, inherited from Memory Core's own mandatory-provenance enforcement. A Knowledge Candidate cannot reference evidence that does not already exist, with provenance, in Memory Core.

**Determination: the promotion boundary is not forbidden strongly enough.** Two of five bypass attempts succeed against the contract as written (unsupported promotion, evidence-strength inflation via non-independent repetition); a third (discarding uncertainty) succeeds by omission rather than by an affirmative loophole.

---

## 4. Permission Boundary

- **Could the two evaluations diverge?** Yes, under a scenario the Contract Design does not address: the Memory Core evidence a Knowledge Candidate references may have been authorized for writing at one point in time (when originally recorded); the Knowledge Memory-level submission check evaluates a separate act (submitting the candidate) at a later time. If the contract intends the second check to be a fresh, independent evaluation of the submission act alone, that should be stated explicitly — as written, it is ambiguous whether the second check re-evaluates the underlying evidence's own authorization or evaluates only the new act. Under policy drift between the two points in time, these readings produce different, potentially contradictory outcomes for identical evidence.
- **Are contradictory outcomes possible?** Yes, under the ambiguity above: evidence legitimately recorded in Memory Core could become permanently unpromotable if a later, differently-scoped Knowledge Memory-level check is misread as re-litigating the original authorization rather than authorizing only the new submission act.
- **Is a single constitutional Trust boundary preserved?** Not as literally claimed. The Contract Design's own §7 and §11 assert "a single Trust boundary, not a second one," while describing two evaluation points. This is not merely a philosophical quibble — a Scope Lock or implementation reading the guarantee literally could either (a) mistakenly implement only one check, silently dropping the other, or (b) implement two checks and believe the constitutional guarantee is nonetheless "a single boundary," obscuring the divergence risk above from ever being examined.
- **Recommendation:** clarification genuinely required (Section 12, Amendment 8) — not because two checks are inherently wrong, but because the contract's own description of them contradicts its own guarantee language, and because the ambiguity in what each check evaluates creates a real divergence path.

---

## 5. Provenance Forwarding

- **Hidden copies?** Not prevented. Section 6 states a provenance reference is "never the provenance content itself," but nothing defines how thin the reference object itself must be. An implementation could embed several provenance field values (source type, acquisition time, and so on) directly inside what it calls a "reference," satisfying the letter of "this is a reference" while functionally shipping a copy. The contract needs a closed, minimal definition of what a reference may contain (an identifier, and nothing else that duplicates provenance content) to close this.
- **Stale references?** The pointer itself cannot go stale, because Memory Core provenance is immutable once created and its records are never deleted outright (only status-flagged). But the *evidence* a Knowledge Item was promoted from can change status (disputed, superseded) after promotion, and nothing in the Contract Design requires a Knowledge Result to disclose that the evidence behind it has moved since the Item's classification was last computed. This is a "stale trust" risk, not a "dangling pointer" risk, and it is real: a caller reading a Knowledge Result today has no way to know, from the result alone, whether the underlying evidence has since been disputed.
- **Mutable references?** Not explicitly prevented. The Contract Design never states that a reference, once issued, is an immutable pointer that can never later resolve to a different target. This should be stated affirmatively, not left to be inferred from Memory Core's own immutability rules, since the reference object itself is a Knowledge Memory concept, not a Memory Core one.
- **Implicit ownership transfer?** A softer, non-structural risk. Because Reasoning Context never talks to Memory Core directly (§8), and a provenance reference is the only path through which most callers will ever encounter provenance, Knowledge Memory risks becoming the *practical*, if not the *formal*, provenance authority for nearly every consumer. Ownership is not technically transferred by anything in the contract, but the contract should say explicitly that Memory Core's own retrieval surface remains directly reachable by any caller that needs full provenance detail, so that "Knowledge Memory is the only door most people use" does not calcify into "Knowledge Memory is the door."

**Determination:** two of four scenarios (hidden copies, stale trust) are not prevented by the contract as written; one (mutable references) is unaddressed rather than prevented; one (ownership transfer) is a documentation gap rather than a structural one.

---

## 6. Knowledge Lifecycle

- **Multiple revisions.** The contract states a prior classification is "retained and reachable" after revision, implying a sequence, but never states that sequence is ordered, or by what (chronology of Memory-Core-evidence arrival? chronology of re-evaluation completion? these can differ). Two implementations could reasonably disagree about ordering, and "retrieve evidential history" (§6) gives no guidance on what order it returns.
- **Competing revisions.** The contract does not address concurrent revision triggers at all. If two pieces of new Memory Core evidence arrive for the same Knowledge Item close together, does the design require them to be serialized into one linear history, or could an implementation reasonably (if unhelpfully) produce two divergent "next" classifications with no defined resolution? The contract's silence here is a genuine gap, not a deliberately deferred implementation detail — the *shape* of the history (linear vs. forking) is an architectural question this document should answer, even without specifying mechanism.
- **Superseded chains.** Reasonably, if implicitly, supported — each supersession links to "the Memory Core relationship that justified it," which by extension of Memory Core's own chain-capable Relationship model should support arbitrary chain length. The contract should say this affirmatively rather than leaving it to be inferred by analogy.
- **Restored knowledge.** A genuine, unaddressed gap. Memory Core's own lifecycle makes `ARCHIVED` explicitly reversible. Knowledge Memory's "retirement" concept (§3) has no stated reversibility rule at all — nothing says whether a retired Knowledge Item can be reinstated if the evidence that caused its retirement is itself later reversed (for example, if an owner-requested deletion path is understood to be otherwise final, but the analogous question for retirement specifically is never answered). Two implementations could reasonably differ: one treats retirement as reversible (mirroring `ARCHIVED`), one treats it as terminal (mirroring `DELETED`). This is exactly the kind of silent divergence Section 9 exists to catch.
- **Contradictory promotion histories.** A genuine gap. The contract describes exactly one Knowledge Promotion record, attached at initial promotion. Section 3's description of "Revision" describes a new classification but never states whether revision produces its own new disclosure record. Without one, a caller reading the (singular, original) Knowledge Promotion record for a Knowledge Item that has since been revised downward would read a justification that no longer matches the Item's current classification — an honest disclosure mechanism that becomes dishonest through the passage of time, purely because the contract did not require it to be refreshed.

**Determination: the lifecycle is not unambiguous.** Three of six constructed scenarios (competing revisions, restored knowledge, contradictory promotion histories) expose genuine, currently unanswered questions, not merely deferred implementation mechanics.

---

## 7. Promotion Criteria

- **Truth determination risk.** Not found directly — no promotion criterion, read individually, asserts or computes truth. The structural protection identified in Section 3 (no "verified" status exists) holds here too.
- **Factual certification risk.** A real risk, indirectly. "Confidence" is listed as a promotion factor (Section 5, inherited from the legacy promotion policy) without stating its source. If "confidence" is read as a value the *submitter* supplies alongside a Knowledge Candidate, then a submitter — potentially a reasoning provider proposing its own candidate — could inflate its own confidence to secure promotion, which is functionally self-certification: the submitter influencing how authoritatively its own submission will later be represented. Article XV requires that no subsystem determine its own evidential status. The contract does not currently rule this reading out.
- **Evidential weighting beyond constitutional authority.** The most consequential finding in this section, restating and sharpening Section 3's independence finding: by explicitly declining to constrain how promotion factors are weighed, the Contract Design permits an implementation to weigh by a single factor (confidence alone, or explicit-request alone, or repetition alone) and still claim full compliance, because nothing in the contract requires the weighing itself to be multi-factor or independence-aware. This is not a hypothetical concern — it is the exact defect the Compliance Audit already documented in the World Model (CT-EI-48, a single-scalar-confidence gate), about to be reproduced in a newly designed subsystem that had the opportunity to learn from that finding and, as currently worded, does not.

**Determination: additional wording is required**, specifically: the weighing of promotion factors must itself be bound by Article XI's own no-single-factor rule (absent an express, documented governing-rule exception), and "confidence" as a factor must be sourced from Memory Core's own recorded evidence or Knowledge Memory's own independent evaluation, never from a submitter's self-declared value.

---

## 8. Contract Completeness

Reported only where genuinely necessary — this section does not repeat every stylistic preference, only omissions that carry constitutional or architectural weight.

- **Missing guarantee: determinism/repeatability.** Memory Core's own frozen architecture requires repeatable retrieval (`MEMORY_CORE_SCOPE_LOCK.md` §11) — identical queries against unchanged state return identical results. No equivalent guarantee exists anywhere in the Knowledge Memory Contract Design. This is a genuine omission, not a stylistic one: without it, an implementation could legitimately return different results for the same Knowledge Query against unchanged state, undermining the same auditability the rest of the design otherwise takes seriously.
- **Missing disambiguation: two confidence-like signals.** Memory Core's own `Assertion.confidence` (nullable) already exists as a recorded fact. Knowledge Memory's own evidential-state classification is a separate, richer judgment. Nothing in the Contract Design states which is authoritative for which purpose, or whether a downstream consumer should ever see both for the same fact and, if so, how to reconcile an apparent disagreement between them. This is a real, moderate-severity completeness gap.
- **Missing prohibition: candidate without evidence.** Section 5 implies, but never flatly states, that a Knowledge Candidate must be rejected outright if it carries no Memory Core evidence reference at all (for example, a candidate consisting only of free text). This should be an explicit prohibition, not an inference from context.
- **Not a genuine omission:** the algorithm for evaluating promotion factors, the exact evidential-state enumeration, and any persistence or indexing detail are correctly left out of this Contract Design — these belong to Scope Lock and implementation, and their absence here is by design, not a gap.

---

## 9. Implementation Freedom

**Yes — two competent teams could build materially different, both-compliant Knowledge Memory systems.** The specific wording that permits divergence:

1. Section 5's "how those factors are weighed is implementation, not design" — permits anything from a rigorous, Article-XI-compliant multi-factor policy to a single-scalar confidence gate identical to the World Model's own already-documented defect.
2. Section 5/7's unstated source for the "confidence" promotion factor — permits either a submitter-supplied value (risking self-certification) or an internally derived one.
3. Section 7's "once at Memory Core's boundary... plus once at Knowledge Memory's own submission boundary," combined with the "single Trust boundary" guarantee language — permits either two intentionally distinct checks or a misreading that collapses them into one, unpredictably, depending on which sentence an implementer weights more heavily.
4. Section 6/8's undefined reference-object shape — permits a minimal identifier-only reference or a content-bearing "reference" that is a de facto copy.
5. Section 3's silence on revision ordering, concurrency, and retirement reversibility — permits a strictly linear, fully reversible lifecycle in one implementation and a forking, terminal-retirement lifecycle in another, with both readings equally defensible from the text.

Each of these five points was independently identified in Sections 2–7 above; their recurrence here confirms they are not isolated nitpicks but a consistent pattern — the Contract Design is precise about *what concepts exist* and *who owns what*, and imprecise about *how strongly evidence must justify a conclusion*, which is exactly the axis Epistemic Integrity cares about most.

---

## 10. Constitutional Stress Test

| Scenario | Guarantee tested | Survives as written? |
| --- | --- | --- |
| Contradictory evidence | Contradiction preserved (§3, §11) | Partial — the concept is designed correctly, but Section 6's competing-revisions gap could let an implementation collapse a fork into a single "winner" before the contradiction ever reaches the retrieval surface |
| Partially trusted evidence (`ContentNature` = `UNKNOWN`) | Uncertainty preserved | Partial — ContentNature is carried forward honestly, but the unbounded confidence-weighting freedom (Section 7) could still let weak evidence be promoted at an unjustifiably strong classification |
| Superseded evidence | Historical revision preserved | Partial — supersession itself is handled, but the missing per-revision disclosure record (Section 6) means the *reason* history can go stale even where the *fact* history does not |
| Unknown provenance | Provenance traceable | Survives — Memory Core's own mandatory-provenance rule makes "unknown provenance" unreachable at Knowledge Memory's boundary in the first place |
| Evolving knowledge | Evidential state preserved and exclusively Knowledge-Memory-assigned | Partial — assignment ownership is correctly exclusive, but ordering/forking ambiguity (Section 6) means "evolving" is not yet a well-defined sequence |
| Revoked knowledge | No silent rewriting | Partial — retirement is non-destructive as a concept, but its reversibility is undefined, so whether "revoked" can honestly become "un-revoked" is currently implementation's guess, not the Constitution's answer |
| Competing hypotheses | Contradiction preserved, uncertainty preserved | Partial — same as "contradictory evidence" above; also newly at risk from the promotion-weighting gap, which could let one hypothesis be promoted confidently before its competitor is ever evaluated, foreclosing the comparison entirely |

**No scenario fails outright.** No scenario passes cleanly, either. Every partial result traces back to one of the eight amendments in Section 12 — this table is not seven independent problems, it is the same handful of root gaps expressed seven different ways.

---

## 11. Risk Register

- **Highest constitutional risk:** unconstrained promotion-factor weighting (Section 7, Section 3), which permits an implementation to reproduce the World Model's own already-documented Article XI single-factor defect (CT-EI-48) in a newly designed subsystem, and permits a submitter to potentially self-certify its own evidential confidence (Article XV risk).
- **Highest architectural risk:** the absence of a synchronicity guarantee between Memory Core's evidence lifecycle and Knowledge Memory's own classification (Section 6) — a promoted Knowledge Item can silently outlive the continued validity of the evidence it was promoted from, for an unbounded window, with no disclosure that this has happened.
- **Highest implementation ambiguity:** the double-permission-gate wording contradiction (Section 4) — the single most likely point at which two implementations, both reading the contract carefully, would build genuinely different permission-evaluation logic while each believing it matches "a single Trust boundary."
- **Highest future maintenance risk:** undefined revision ordering, forking, and retirement reversibility (Section 6) — low-cost to fix now, at the design stage, and expensive to retrofit once real Knowledge Items with real revision histories exist, mirroring the Reconciliation's own already-stated lesson that "provenance capture cannot be retrofitted after the fact with any integrity," applied here to revision history rather than provenance.

---

## 12. Required Amendments

Eight amendments are required. Each is described only — none is drafted, redesigned, or rewritten here.

1. **Affected section:** Contract Design §5 (Promotion Boundary) and §7 (Promotion Criteria). **Constitutional reason:** Article XI §1 prohibits a single, undeclared factor from determining evidential weight; promotion-factor weighing as currently left fully to "implementation, not design" permits exactly that. **Implementation consequence:** the eventual promotion policy must be required, at minimum, to weigh multiple factors and to distinguish independent corroboration from repeated mentions of a common source, before Scope Lock may treat weighing as a pure implementation detail.

2. **Affected section:** Contract Design §5/§7 ("confidence" as a promotion factor). **Constitutional reason:** Article XV forbids a subsystem from determining its own evidential status; an unsourced "confidence" factor risks a submitter self-certifying its own candidate. **Implementation consequence:** the Knowledge Candidate contract must not expose a caller-settable confidence value; confidence, where used as a factor, must derive from Memory Core's own recorded `Assertion.confidence` or from Knowledge Memory's own independent evaluation.

3. **Affected section:** Contract Design §4 (Evidential State). **Constitutional reason:** Article XIII forbids false precision and requires uncertainty to remain expressible; a permissive "may express unresolved" leaves this optional rather than guaranteed. **Implementation consequence:** whatever evidential-state representation is eventually designed must be structurally required to support an "insufficiently supported/unresolved" outcome, not only graduated confidence.

4. **Affected section:** Contract Design §3 (Knowledge Lifecycle, Revision). **Constitutional reason:** Article XVI requires an honest, inspectable record of what was known and when; unordered or forkable revision history cannot honestly answer that question, and a stale, un-refreshed disclosure record cannot either. **Implementation consequence:** the lifecycle design must require a single, chronologically ordered, non-forking classification history per Knowledge Item, with concurrent triggers serialized, and must require each revision to produce its own disclosed basis record, not only the initial promotion.

5. **Affected section:** Contract Design §3 (Knowledge Lifecycle, Retirement). **Constitutional reason:** Articles XVI/XVII require a clear, non-ambiguous answer to whether previously-retired knowledge can honestly be reinstated — this is a constitutional question about historical honesty, not merely a design preference. **Implementation consequence:** the lifecycle design must state explicitly whether retirement is reversible or terminal.

6. **Affected section:** Contract Design §6/§8 (Provenance Forwarding). **Constitutional reason:** Articles VIII/IX reserve provenance exclusively to Memory Core; an unbounded "reference" shape risks a de facto duplicate. **Implementation consequence:** the eventual Knowledge Reference/provenance-reference shape must be defined as a minimal identifier only, explicitly excluding any duplicated provenance field content, and must be specified as immutable once issued.

7. **Affected section:** Contract Design §3/§6 (staleness disclosure). **Constitutional reason:** Article XIII forbids concealing uncertainty; silently serving a Knowledge Result whose underlying evidence has since changed status conceals exactly the uncertainty the Constitution requires disclosed. **Implementation consequence:** Scope Lock must choose one of: a staleness-flag on Knowledge Results, or a bounded-latency synchronous re-evaluation guarantee tied to the underlying Memory Core transition.

8. **Affected section:** Contract Design §7/§11 (Permission Boundary wording). **Constitutional reason:** an inaccurate guarantee statement ("a single Trust boundary, not a second one" describing what the same document elsewhere describes as two evaluations) is itself a governance defect, independent of whether the underlying two-check design is sound. **Implementation consequence:** the guarantee language must be corrected to accurately describe two distinct, purposefully-scoped evaluations sharing one enforcement mechanism, and Scope Lock must specify exactly which action/resource pairing each evaluates, confirming they do not re-litigate the same authorization at different times.

No amendment above proposes new constitutional doctrine; each traces to an Article, a prior Compliance Audit finding, or an internal inconsistency already present in the Contract Design's own text.

---

## Decision

```
FURTHER CONTRACT REFINEMENT REQUIRED
```

Eight amendments are required, none of which redesigns the architecture the Governance Review and Contract Design already established. Sections 3, 6, and 7 of this review each found at least one bypass or gap a competent, compliant implementation could exploit without violating any sentence currently in the Contract Design — most seriously, the unconstrained promotion-weighting freedom (Section 3, Section 7, Amendment 1), which would let a newly designed subsystem reproduce a defect the Compliance Audit already documented and condemned in the World Model. Section 9 confirms genuine implementation-freedom divergence exists at five independent points, which on its own is sufficient to withhold a `READY FOR SCOPE LOCK` conclusion under this review's own governing standard: whenever two reasonable implementations could differ, the contract is insufficiently precise until proven otherwise, and proof was not found for at least five of the points examined.
