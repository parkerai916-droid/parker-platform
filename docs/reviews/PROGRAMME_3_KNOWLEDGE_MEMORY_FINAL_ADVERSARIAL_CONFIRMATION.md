**Status:** Final independent adversarial confirmation. Read-only. This review does not modify `docs/architecture/`, `docs/governance/`, production code, or tests. Nothing is staged, committed, or pushed. This is the final governance gate before Scope Lock — not a redesign, not a further contract revision.

# Programme 3 — Knowledge Memory Final Adversarial Confirmation

Subject reviewed: `docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_CONTRACT_DESIGN_V2.md` ("Version 2"), against `docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_GOVERNANCE_REVIEW.md`, `docs/reviews/PROGRAMME_3_KNOWLEDGE_MEMORY_ADVERSARIAL_CONTRACT_REVIEW.md` ("the First Review"), `docs/architecture/parker-constitution.md`, `reasoning-context.md`, `epistemic-integrity.md`, `docs/architecture/MEMORY_CORE_CONTRACT_DESIGN.md`, and `docs/reviews/EPISTEMIC_INTEGRITY_CONSTITUTIONAL_REMEDIATION_PLAN.md`. None of these is altered by this review. This review assumes Version 2 is complete and tests only for remaining constitutional ambiguity — it does not search for style, wording preference, or unnecessary tightening, and does not recommend any change that is not necessary to prevent constitutional divergence.

---

## 1. Overall Assessment

- **Internally consistent?** Substantially, with one wording-level tension identified and resolved by analysis rather than by amendment (Section 2, Amendment 4; Section 6). No other internal contradiction was found between any two sections of Version 2.
- **Constitutionally complete?** Yes. Every Article the First Review invoked (IV, VII, VIII, IX, XI, XIII, XV, XVI, XVII) is now addressed by a specific, binding sentence in Version 2, not merely by a stated intention.
- **Sufficiently constrained?** Yes, with the residual mechanism-level latitude identified in Section 6 and 7 below — latitude this review finds to be legitimate implementation freedom rather than constitutional risk, for the reasons given there.
- **Ready for implementation?** Yes, at the Contract Design level. Several mechanism-level decisions remain correctly deferred to Scope Lock (Section 6, 7) — their deferral is disclosed and bounded, not silent.

**Overall finding: Version 2 resolves the First Review's eight findings. This review's own independent search surfaced one narrow, self-contained wording tension not previously flagged (Section 2, Amendment 4), and re-examined the one previously disclosed open item (Section 3). Neither rises to a level requiring further contract revision, for the reasons documented below.**

---

## 2. Regression Check

Each of the eight amendments evaluated independently against Version 2's actual text, not assumed resolved because Version 2 claims resolution.

1. **Promotion weighting.** Version 2 §5 requires weighing to "consider more than one factor" and forbids treating repetition/frequency as independent corroboration without first checking common origin. This is a binding, testable requirement — an implementation that weighs by a single factor without an express, documented, Article-XI-justified exception is non-compliant on the contract's own terms. **Genuinely resolved.**
2. **Confidence sourcing.** Version 2 §2, §4, §5 state a Knowledge Candidate carrying a caller-declared confidence or evidential-state value is malformed and must be rejected; confidence must come from Memory Core's own record or Knowledge Memory's own evaluation. This closes the self-certification path the First Review identified. **Genuinely resolved.**
3. **Uncertainty expression.** Version 2 §4 replaces "may express unresolved" with "must be capable of expressing" an insufficiently-supported/unresolved outcome, stated as a binding requirement on whatever representation Scope Lock designs. **Genuinely resolved.**
4. **Revision history.** Version 2 §3 requires a single, chronologically ordered, non-forking history and a disclosure record per revision, not only at initial promotion. **Substantially resolved**, with one qualification: the sentence "both are incorporated into the same re-evaluation and serialized into one next classification, by a consistent, disclosed ordering rule" describes two mechanisms that are not obviously the same thing — "incorporated into the same re-evaluation" suggests a single, joint, atomic evaluation of combined evidence (no ordering needed), while "serialized... by an ordering rule" suggests sequential, order-dependent application (where ordering matters to the outcome). This is a genuine wording tension this review found independently, not previously flagged by the First Review. On analysis (Section 6), it does not constitute an open constitutional loophole, because Amendment 1's own standing requirement — "no single factor may, by itself, determine promotion or the resulting evidential-state classification" — already reaches and forbids an ordering rule that functions as a disguised single-factor determinant, regardless of which of the two readings an implementation adopts. The tension is real; its constitutional consequence is already closed by a different, standing provision.
5. **Retirement reversibility.** Version 2 §3 defines retirement as non-deletive and reversible via a new "restoration" event, except where the underlying evidence was itself erased, in which case only a new promotion is possible. Tested against Memory Core's own frozen lifecycle rule that `DELETED` is terminal with no transition out — the two are consistent, not merely parallel. **Genuinely resolved.** (A minor completeness note, not a defect: the text illustrates one retire-restore cycle and does not explicitly state that arbitrarily many cycles are supported, though the surrounding append-only principle stated throughout Version 2 makes this the only reasonable reading. Noted in Section 7, not treated as unresolved.)
6. **Provenance reference.** Version 2 §6 defines a provenance reference as identifier-only, immutable once issued, with no ownership transfer and continued direct reachability of Memory Core's own retrieval surface. Tested against all four sub-concerns the First Review raised (hidden copies, staleness, mutability, ownership transfer) individually — each is addressed by a specific clause. **Genuinely resolved.**
7. **Staleness.** Version 2 §3, §6, §11, §12 require every Knowledge Result to carry a mandatory staleness disclosure, not an opt-in query. Tested against the First Review's specific concern (a caller must not need to remember to ask) — the mandatory-field framing directly answers this. **Genuinely resolved.**
8. **Permission boundary.** Version 2 §7 replaces the "single Trust boundary" claim with two named, non-overlapping evaluations (Evaluation A over the evidence, Evaluation B over the submission act), and states B never re-litigates A. Tested against the First Review's specific divergence scenario (policy drift between the time evidence is recorded and the time a candidate referencing it is submitted) — because B evaluates only the submission act and never re-evaluates A's already-settled outcome, policy drift on A's own action/resource pairing cannot retroactively alter a decision already made. **Genuinely resolved.**

**Regression result: 7 of 8 amendments resolved without qualification; 1 (Revision history) resolved in substance with a self-contained wording tension identified and independently found not to reopen a constitutional gap.**

---

## 3. Remaining Open Concern — "Relevant" in Knowledge Query

The First Review's Section 2 flagged that "relevant" in Knowledge Query's description is undefined, risking a reading in which Knowledge Memory performs task-interpretation (a cognitive act) rather than structural matching.

- **Does it genuinely permit task interpretation inside Knowledge Memory, read in isolation?** On the Knowledge Memory Contract Design's own text alone — yes, nothing in Version 2 explicitly forecloses that reading.
- **Is the wording already sufficiently constrained elsewhere?** Yes. `reasoning-context.md` — an authoritative, already-ratified document Version 2's own Status header commits not to contradict — assigns "exposing only the portions relevant to a given task" as one of **Memory's own architectural responsibilities**, distinct from "Reasoning Context assembly," which is separately responsible for "combining the relevant portions of Memory and the World Model... into a single, bounded working set." Read together, this establishes a two-stage division that already exists in ratified constitutional material: Knowledge Memory (as the successor to "Memory" in that document, per the Governance Review's own accepted terminology) performs structural, task-parameter-scoped exposure; Reasoning Context assembly performs the further combination and bounding. `reasoning-context.md` also states directly that reasoning providers "have no standing access to Memory or the World Model outside what has been assembled into their Reasoning Context" — confirming that genuine task interpretation is reserved to the reasoning/assembly stage, never to Memory itself. An implementation that had Knowledge Memory perform semantic or cognitive interpretation of a task's meaning, rather than structural filtering against caller-supplied task parameters, would violate `reasoning-context.md` directly — independent of anything Version 2 itself says.
- **Is amendment constitutionally required?** No. The constraint already exists in ratified, binding material that governs Knowledge Memory regardless of this Contract Design's own wording. A cross-reference to `reasoning-context.md`'s own responsibility split would improve Version 2's self-containedness, but its absence does not leave a genuine constitutional gap — it leaves a document that must be read in its proper architectural context, which every governance document in this Programme already requires (each one explicitly disclaims re-deriving what a prior, frozen document already settled).

**Determination: the "relevant" wording does not require amendment. It is resolved by an already-ratified, authoritative document Version 2 is bound not to contradict, not by anything new this review proposes.**

---

## 4. Cross-Boundary Review

Re-examined against all five external boundaries, independently of the First Review's own findings.

- **Memory Core.** No leakage. Identity, provenance, and relationships remain exclusively Memory Core's; Version 2's new Evaluation A/B language reinforces rather than blurs this, since Evaluation A remains explicitly Memory Core's own act.
- **World Model.** No leakage. No contract in Version 2 references or is referenced by any World Model contract; the retirement/restoration mechanics added in Version 2 remain framed as durability status, never as current-belief tracking.
- **Reasoning Context.** No new leakage beyond the pre-existing, disclosed "relevant" question, resolved in Section 3 above by reference to already-ratified material.
- **Representation.** No leakage. Every new disclosure obligation Version 2 adds (staleness, per-revision Knowledge Promotion records) remains data made retrievable, never prose Knowledge Memory composes.
- **Document Intelligence.** No leakage. No document-specific concept, field, or capability appears anywhere in Version 2; Section 10 is unchanged from Version 1 and remains accurate.

**No cross-boundary leakage found, beyond the single item already addressed in Section 3.**

---

## 5. Constitutional Guarantees

Every guarantee Version 1 stated is present in Version 2, either unchanged or strengthened; none is weakened. Specifically checked: provenance traceability (strengthened — now minimal and immutable); evidential-state exclusivity (strengthened — now explicitly not a truth determination); historical revision preservation (strengthened — now ordered and non-forking); contradiction preservation (unchanged, and reinforced by the promotion-weighting fix, which prevents contradiction from being pre-collapsed at promotion time); uncertainty preservation (strengthened — now mandatory rather than permissive); promotion traceability (strengthened — now refreshed at every revision, not only initial promotion); no silent rewriting (unchanged, reinforced by the restoration rule); no caller-facing promotion (strengthened — now explicitly excludes caller-supplied confidence/evidential state); the Trust boundary guarantee (corrected in wording, not weakened in substance — the underlying enforcement, via the Permission Engine, is identical; only the inaccurate "single boundary" description was corrected); no document-specific leakage (unchanged); staleness disclosure (new, and additive).

**No guarantee identified in either Version 1 or the First Review is weakened, narrowed, or removed in Version 2.**

---

## 6. Implementation Determinism

**Central question: could two competent engineering teams implement this contract differently while both claiming constitutional compliance?**

**In the mechanism sense — yes, and this is expected, not a defect.** Version 2 deliberately leaves several mechanisms to Scope Lock: the exact algorithm and weights used within the required multi-factor promotion policy; the specific method for detecting staleness; the specific tie-break/ordering rule for concurrently-arriving revision evidence; whether to invoke Article XI's own express-governing-rule exception for a documented single-factor case. Two teams could reasonably choose different mechanisms here. `MEMORY_CORE_CONTRACT_DESIGN.md` §19 establishes the same pattern for Memory Core itself — naming open, Scope-Lock-level questions explicitly and declining to let them block Contract Design acceptance — and this review applies the same standard to Knowledge Memory rather than holding it to a stricter one.

**In the constitutionally consequential sense — no.** For every mechanism-level choice named above, a standing, binding requirement elsewhere in Version 2 already constrains what any chosen mechanism may do: the promotion algorithm, whatever its specific weights, may never let a single factor alone determine the outcome (Amendment 1); staleness, however detected, must always be disclosed once found (Amendment 7); the revision ordering rule, whatever it is, cannot function as a hidden single-factor determinant without violating the same Amendment 1 constraint that governs promotion generally, since that constraint is written in terms of "the resulting evidential-state classification," not narrowly scoped to the initial promotion act alone. This is the specific reasoning that resolves Section 2's Amendment 4 finding: the wording tension identified there permits mechanism-level divergence (joint vs. sequential evaluation), but not constitutionally consequential divergence, because Amendment 1's own prohibition travels with either mechanism.

**Precise wording permitting mechanism-level (not constitutional) divergence:** Version 2 §5's "the specific rule... is a Scope Lock decision, not fixed here"; §3's "both are incorporated into the same re-evaluation and serialized into one next classification" (the joint/sequential tension); §5's own express-governing-rule exception clause, inherited directly from Article XI §1's own conditional structure rather than invented by this Contract Design.

---

## 7. Residual Risks

Only risks that remain after Version 2, excluding hypothetical future enhancements:

- **Mechanism-level mixing in Version 2 §3's revision-serialization wording** (Section 2, Amendment 4; Section 6) — narrow, self-contained, and already bounded by Amendment 1's standing constraint. Worth a one-sentence clarification at Scope Lock (state explicitly whether concurrent evidence is evaluated jointly or sequentially) but not a constitutional risk as it stands.
- **Provenance reference minimality wording** (Version 2 §6: "contains only the identifier... and nothing else") does not explicitly address whether a record-kind/type tag (needed to address the reference at all, and not itself provenance content) is permitted. Almost certainly permitted on a sensible reading, but worth Scope Lock stating explicitly for implementability.
- **Retirement/restoration cycle count.** Version 2 illustrates one retire-restore cycle; arbitrarily many are the only reasonable reading given the surrounding append-only principle, but this is inferred, not stated. Low severity; a one-sentence generalization would remove all doubt.
- **`reasoning-context.md` cross-reference absent from Knowledge Query's own text** (Section 3) — a documentation completeness item, not a constitutional gap, since the binding constraint already exists in ratified material Version 2 cannot contradict.

None of the four items above is assessed as constitutionally consequential; each is disclosed here as a Scope-Lock-attention item, consistent with `MEMORY_CORE_CONTRACT_DESIGN.md`'s own precedent of naming genuinely open, non-blocking questions explicitly rather than silently.

---

## 8. Decision

```
READY FOR SCOPE LOCK
```

All eight findings from the First Review are independently confirmed resolved by binding, testable language in Version 2 (Section 2). The one previously disclosed open item — the "relevant" wording in Knowledge Query — is resolved by reference to `reasoning-context.md`'s own already-ratified division of responsibility between Memory and Reasoning Context assembly, not by any new amendment (Section 3). This review's own independent search surfaced one further wording tension (concurrent-revision serialization, Section 2 Amendment 4) not previously flagged; on analysis, its constitutional consequence is already foreclosed by Amendment 1's own standing, generally-worded prohibition on single-factor determination, leaving only a mechanism-level ambiguity of the same kind Memory Core's own Contract Design already treated as non-blocking (Section 6, Section 7). No constitutional guarantee is weakened anywhere in Version 2 (Section 5), and no responsibility has leaked across any of the five external boundaries examined (Section 4). The residual items in Section 7 are appropriately addressed to Scope Lock, not to a further Contract Design revision — proceeding to Scope Lock is the correct next governance step.
