**Status:** Governance-only reconciliation. No Kotlin is implemented, proposed as a diff, or changed by this document. Neither `src/` nor `tests/` is touched. Nothing is staged, committed, or pushed. This document does not amend, redefine, or reopen any decision in any document it reviews; it identifies precedence and conflict only, per its own governing task's explicit instruction not to reconcile by interpretation unless the reviewed documents themselves permit it.

# Programme 3 — Unit 6 Constitutional Reconciliation

Programme: **Programme 3 — Knowledge Memory, Unit 6 Constitutional Reconciliation.**

This document performs a governance-only reconciliation among `docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_CONTRACT_DESIGN_V2.md` ("Contract Design V2"), `docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_SCOPE_LOCK.md` ("Scope Lock"), `docs/implementation/PROGRAMME_3_KNOWLEDGE_MEMORY_IMPLEMENTATION_PLAN.md` ("Implementation Plan"), `docs/governance/PROGRAMME_3_UNIT_6_SCOPE_LOCK_CLARIFICATION.md` ("Unit 6 Clarification"), and `docs/reviews/EPISTEMIC_INTEGRITY_CONSTITUTIONAL_REMEDIATION_PLAN.md` ("Remediation Plan"), using the prior Unit 6 Compliance Reconciliation report as mandatory input. Its objective is not to defend or repair the current Unit 6 implementation, but to determine which governance document is authoritative where the documents above disagree.

---

## 1. Document Hierarchy

Each document states its own place in this hierarchy explicitly; none of the following is inferred beyond what each document says of itself.

1. **Contract Design V2** — highest authority among the five. Its own header: "This document supersedes `...CONTRACT_DESIGN.md` ('Version 1')... No constitutional requirement is weakened anywhere in this revision." It is architecture-defining: it fixes the public model, the promotion boundary, the evidential-state rules, and the permission-boundary description.
2. **Scope Lock** — binding, but explicitly non-originating: "This document is binding. It does not redefine any decision already made. `...CONTRACT_DESIGN_V2.md`... [is a] frozen, normative input — this document does not reopen the layering, the terminology, the public model, the eight resolved amendments, or any constitutional question those documents already settled." Scope Lock's own §10 ("Out-of-Scope Change Policy") additionally states that any proposal that "changes any public contract Contract Design Version 2 froze... requires formal governance before implementation — a Scope Lock revision."
3. **Implementation Plan** — sequencing only, explicitly zero new architecture: "This document describes **how** the already-approved Knowledge Memory is built... It introduces no contract, no field, no lifecycle rule, no permission model, and no constitutional obligation beyond what the Scope Lock and Contract Design Version 2 already froze. Where this plan makes a genuinely new decision (a unit boundary, a phase grouping, an ordering choice), it is disclosed as an implementation-sequencing choice, not presented as an architectural one."
4. **Unit 6 Clarification** — narrowest, self-limited authority of the four: "Status: Narrow governance clarification only. Does not reopen Programme 3's architecture, layering, public model, or any of the eight amendments `...CONTRACT_DESIGN_V2.md` resolved. Does not alter constitutional doctrine."

**Consequence for this reconciliation:** where the Unit 6 Clarification's own text conflicts with Contract Design V2, Contract Design V2 controls, because the Clarification itself disclaims any power to reopen what Contract Design V2 fixed. This is not this document's own interpretive judgment — it is the Clarification's own stated scope, taken at its word.

The Remediation Plan sits earlier in provenance than all four Programme-3-specific documents (it is what assigned Programme 3 the Article IV evidential-state taxonomy and part of Article XI independence/corroboration tracking in the first place — Section 6 of that plan, "Knowledge Memory (Programme 3)" against both findings). It is treated here as background authority establishing *why* Programme 3 owns these obligations, not as a document that itself resolves the Model A/Model B question below.

---

## 2. Issue 1 — Promotion Model: A or B

**Model A** (evaluate criteria → Reject or Promote → assign state) is stated directly and repeatedly in Contract Design V2:

- §1: "Evaluating a candidate, submitted with reference to Memory Core evidence, **against promotion criteria**, and deciding whether it becomes durable, retrievable knowledge."
- §3 (Promotion): "A Knowledge Candidate, referencing existing Memory Core evidence, is evaluated against promotion criteria (Section 5). **If it meets them**, a Knowledge Item is created... **A candidate that is not promoted produces no Knowledge Item and no Knowledge Memory record at all**; its underlying evidence remains exactly where it already was, inspectable in Memory Core on its own terms."
- §5 (Promotion Boundary): a two-part list headed **"What qualifies evidence for promotion"** and **"What never qualifies"** — the structure of a genuine gate, not a default-promote posture.

Scope Lock and the Implementation Plan are consistent with Model A without restating it fully: Scope Lock §3 authorises "the promotion pipeline, implemented to satisfy Contract Design Version 2's binding multi-factor, independence-aware weighing requirement," and the Implementation Plan's Unit 6 objective is to "extend the promotion evaluation to weigh more than one factor... and to source confidence exclusively from Memory Core's own recorded evidence or from this evaluation itself" — language describing a weighing process that feeds a promotion decision, not an unconditional pass-through.

**Model B** (resolve record → always promote → assign state) is not stated anywhere in Contract Design V2, Scope Lock, or the Implementation Plan. It appears only in the Unit 6 Clarification:

- §6: "No single factor may determine promotion by itself, **except denied permission, missing referenced record, and invalid or unusable provenance** — which are structural or constitutional prerequisites, not evidential weighing, checked before any evidential factor is considered." By naming only three possible rejection grounds, all purely structural, this section implies no other rejection ground exists once they are satisfied.
- §7: "a resolved record carrying neither is promoted as `UNKNOWN`... Rejection is reserved solely for the structural prerequisite failure (Section 6)."

**Conflict identified:** genuine, not merely apparent. This is not a vocabulary mismatch — it changes which candidates become Knowledge Items at all. Contract Design V2 anticipates real, evidence-quality-based rejection (a resolved, provenance-valid candidate that simply does not "meet" the named promotion criteria); the Unit 6 Clarification's own logic forecloses that possibility entirely, reducing "not promoted" to only the three named structural failures.

**Precedence:** Contract Design V2. Per Section 1 above, the Unit 6 Clarification cannot narrow or override Contract Design V2's own promotion-boundary model while simultaneously disclaiming, in its own Status line, any power to do so.

---

## 3. Issue 2 — Status of the Six Named Promotion Criteria

Contract Design V2 §5's exact text: "The evidence must meet the promotion criteria **already established for this layer** (repetition, importance, relevance, frequency, confidence, explicit request)."

**Every occurrence traced:**

- Contract Design V2 §5 — the sole place all six are named together, in this exact form, in any of the five reviewed documents.
- Scope Lock §6 restates the constraint on *weighing* them ("Weighing must consider more than one factor; no single factor may, by itself, determine promotion or classification...") but does not re-list the six by name.
- Implementation Plan Unit 6 names two of the six specifically — repetition and frequency — for their common-origin risk ("to treat repetition/frequency as contributing no more weight than a single mention when mentions share a common origin"), and separately names confidence's sourcing rule. It does not mention importance, relevance, or explicit request at all, and does not state that any of the six is retired, replaced, or deferred.
- Unit 6 Clarification — none of the six is named anywhere in this document. Its own §6 ("Evidential Factors, Authorised Categories") lists a wholly different set (record existence, provenance availability, evidential content type, recorded confidence, corroborative/contradictory relationships, uncertainty, permission outcome) without cross-referencing Contract Design V2 §5's list at all.

**Determination:** the six factors read as the established, closed set of legitimate promotion-criteria categories inherited from Version 1 ("already established for this layer") — not illustrative examples (no "for example" or "such as" language attaches to them) and not marked as future placeholders (no "reserved" or "future" language attaches to them either). **No document among the five supersedes them.** The Unit 6 Clarification does not claim to supersede this list; it simply does not mention it. Per Section 1's hierarchy, an unacknowledged omission by the lowest-authority document in this set does not constitute supersession — it is an undisclosed gap.

---

## 4. Issue 3 — Corroboration and Contradiction: Replace, Supplement, or Different Layer

Neither concept originates with Unit 6; both already exist in frozen governance, in two different homes:

- **Contradiction** lives principally at the **lifecycle/disclosure layer**, Contract Design V2 §3 ("Contradiction"): "**Unchanged from Version 1**: where Memory Core holds an unresolved `CONTRADICTS`/`DISPUTES` relationship between two Assertions each capable of supporting a promotable proposition, Knowledge Memory must not promote one and silently omit the other." This is older than Unit 6 and is not one of the six §5 promotion-boundary factors by name — it is a disclosure obligation about already-classified (or classifiable) Knowledge Items.
- **Corroboration** is invoked only once in the five documents, tied specifically to repetition/frequency and gated by a check the Unit 6 implementation omits: Scope Lock §6 (Article XI): "Repetition and frequency must never be treated as independent corroboration **without first determining whether repeated mentions share a common origin**." Contract Design V2 §5 states the identical rule.

**Determination, using document evidence only:** corroboration and contradiction were not intended, by any of the four Programme-3 documents, to *replace* the six-factor promotion-boundary list — nothing states or implies that. They were also not clearly intended to operate at a wholly separate architectural layer immune from §5's weighing constraint — contradiction is described at the lifecycle-disclosure layer but Contract Design V2 §3 itself cross-references §5's own weighing rule directly ("Reinforced by Amendment 1's revised Section 5"), meaning the two layers are explicitly linked, not independent. The most textually accurate characterization is that Unit 6 took two already-existing, narrower concepts — contradiction-disclosure (§3) and common-origin-gated corroboration (§5/Article XI) — and used them as a **substitute** for the full six-factor promotion gate, while also dropping the one safeguard (common-origin checking) frozen governance explicitly attaches to corroboration. This is neither a clean replacement nor a genuine supplement (the other four §5 factors are addressed nowhere), and it is not confined to "a different layer" either, since contradiction's own textual home already cross-references the promotion-weighing rule directly.

---

## 5. Issue 4 — Scope of "No Single Factor May Determine Promotion"

Contract Design V2 §5, verbatim: "no single factor may, by itself, determine **promotion or the resulting evidential-state classification**, absent an express, documented governing-rule exception stated and justified at Scope Lock."

Scope Lock §6, verbatim: "no single factor may, by itself, determine **promotion or classification** absent an express, documented, Article-XI-conditioned exception."

**Determination:** both documents name the prohibition against *both* targets explicitly and in the same sentence — it is not confined to the promote/reject decision alone. Evidential-state assignment (which specific `EvidentialState` is attached) is named as an equally covered target in both instances. No document narrows this to "promotion only." This directly bears on Issue 6, below: an implementation that never rejects (per Issue 1's Model B) but lets a single factor alone select which non-neutral state to assign is not exempted from this prohibition merely because the promote/reject question has been removed from consideration — the classification question remains fully covered.

---

## 6. Issue 5 — Does `UNKNOWN` Itself Justify Promotion?

Contract Design V2 §4 ("Mandatory expressiveness"), the only passage addressing `UNKNOWN`-equivalent representation directly: "Whatever evidential-state representation is designed... it **must be capable of expressing** an 'insufficiently supported' or 'unresolved' outcome — this is a binding requirement of this Contract Design, not an option the representation may or may not include." This is phrased as a property of the *representation* (the type must be able to carry this state) — it is not phrased as a licence to promote whenever no stronger state is justified.

Read together with Issue 1's Model A texts (§1, §3, §5 — promotion requires meeting real criteria; a candidate not meeting them "produces no Knowledge Item... at all"), the more textually supported reading across Contract Design V2 and Scope Lock is: **`UNKNOWN` must remain representable once a separately justified promotion has occurred; it is not itself a sufficient ground for promotion.**

The only document suggesting the contrary reading is the Unit 6 Clarification §7: "a resolved record carrying neither [corroboration nor contradiction] is promoted as `UNKNOWN`." Per Section 1's hierarchy, this reading cannot stand on the Clarification's own authority alone against Contract Design V2's higher-precedence text, and the Clarification does not identify Contract Design V2 §4 or §5 as authorizing this specific conclusion — it derives the conclusion from Article IV's general definition of `UNKNOWN` alone, without addressing §5's promotion gate at all.

---

## 7. Issue 6 — Do `COMPETING_EXPLANATIONS` and `CORROBORATED_EVIDENCE` Have Equal Textual Exceptions?

**`COMPETING_EXPLANATIONS`:** Contract Design V2 §3 provides a specific, named textual anchor: detecting an unresolved `CONTRADICTS`/`DISPUTES` relationship must produce an honest "unresolved" disclosure, and §3 explicitly cross-references §5's weighing rule to forbid any single factor from *resolving* that tie in favor of one side. Critically, this exception concerns only the obligation to disclose non-resolution — it does not grant contradiction-detection the power to select a *stronger, decisive* classification; it only prevents a *false* resolution.

**`CORROBORATED_EVIDENCE`:** no comparable passage exists anywhere in the five documents. The only frozen text touching "corroboration" ties it strictly to repetition/frequency, explicitly conditioned on a common-origin check (Contract Design V2 §5; Scope Lock §6, Article XI) — a check the current implementation never performs. Assigning `CORROBORATED_EVIDENCE` — a specific, non-neutral, support-implying classification — from the mere presence of one `SUPPORTS` relationship has no express, documented governing-rule exception anywhere in frozen governance.

**Determination:** only `COMPETING_EXPLANATIONS` has a textually grounded exception, and even that exception is narrower than the implementation's own KDoc characterizes it (a duty to disclose non-resolution, not a general licence for single-factor decisiveness). `CORROBORATED_EVIDENCE`'s single-factor assignment has no exception at all and stands in direct, unexcused tension with Contract Design V2 §5 and Scope Lock §6's identical prohibition (Issue 4, above, having already established that this prohibition explicitly covers classification, not only promotion).

---

## 8. Issue 7 — Does the Unit 6 Clarification Hold Up?

- **Faithfully implements earlier governance?** Partially. It correctly preserves the legacy-path freeze, correctly reuses `MemoryRetrieval` without redesigning Memory Core, correctly derives `ProvenanceReference` construction from each record's own mandatory field, and correctly and honestly discloses the `ContentNature`/permission-lookup gaps as genuine limitations deferred to a later unit. It does **not** faithfully implement Contract Design V2 §5's promotion-boundary model (Issue 1) or its six-factor list (Issue 2).
- **Introduces new constitutional doctrine?** Its Status line disclaims this, and mostly it does not — with one significant exception: deciding that promotion becomes unconditional once three structural prerequisites are met is a substantive change to the promotion boundary itself, which is exactly the category of change Scope Lock §10 reserves to "formal governance... a Scope Lock revision," not a narrow clarification.
- **Silently narrows or broadens existing doctrine?** Both, simultaneously and undisclosed as such: it narrows the authorized factor set (six named factors down to two relationship-derived signals, dropping the common-origin safeguard on the one it keeps) while broadening the promotion gate itself (from "must meet real qualifying criteria" to "promote unless structurally unresolvable").
- **Resolves ambiguity correctly?** Only in part. It correctly resolves the specific, narrow gap it was asked to resolve — no frozen document names an evaluator interface or result type compatible with the new `KnowledgeItem`/`KnowledgePromotion` shapes, and inventing `KnowledgeCandidateEvaluator`/`KnowledgeCandidateEvaluation` to fill that gap is a reasonable, disclosed engineering response. But in the same document it also resolved a second, larger, more consequential question — whether structural resolution alone suffices for promotion — that Contract Design V2 §5 does not actually leave open, without flagging that second resolution as carrying different, heavier governance weight than the first.
- **Unintentionally conflicts with earlier frozen documents?** Yes, on the two points above (Issues 1–2, and the `CORROBORATED_EVIDENCE` single-factor issue in Issues 4/6).

---

## 9. Deliverable Summary

**Document hierarchy (highest to lowest):** Contract Design V2 → Scope Lock → Implementation Plan → Unit 6 Clarification. The Remediation Plan is background provenance for *why* Programme 3 owns Article IV/XI, not a resolver of the Model A/B question.

**Every identified conflict:**

1. Promotion Model A (Contract Design V2 §1, §3, §5) vs. Model B (Unit 6 Clarification §6, §7) — genuine.
2. Six named promotion-criteria categories (Contract Design V2 §5) vs. the Unit 6 Clarification's substituted two-signal factor set (§6), with no cross-reference or reconciliation between them — genuine.
3. `CORROBORATED_EVIDENCE` assigned from a single, common-origin-unchecked factor vs. "no single factor may... determine... the resulting evidential-state classification absent an express, documented exception" (Contract Design V2 §5; Scope Lock §6) — genuine, and unlike `COMPETING_EXPLANATIONS`, unexcused by any textual carve-out.
4. Implementation Plan's repository-impact table (Units 6–8 all extending the same renamed `InMemoryKnowledgeStore`, per its Section 4 row for that file) vs. the actual chosen path (a new, parallel `KnowledgeCandidateEvaluator`/`DefaultKnowledgeCandidateEvaluator` that never touches the legacy class) — **apparent, not conclusively resolved here.** The Implementation Plan explicitly describes its own unit boundaries as "engineering precedent, not a source of new decisions," and the parallel-track approach is consistent with a pattern this project's own Unit 4/5 Scope Lock Clarifications reportedly already established (referenced repeatedly by the Unit 6 Clarification but not among this task's five reviewed documents, so not independently verified here). Flagged, not adjudicated.

**Every resolved conflict:** none. Consistent with this task's own instruction not to reconcile by interpretation unless the documents explicitly permit it, this document identifies precedence and genuineness only; it does not itself amend, waive, or settle any of the four conflicts above. All four remain open.

**Every unresolved conflict:** all four listed above.

**Recommendation:**

```
The Unit 6 Clarification requires amendment before Unit 6 implementation may continue.
```

**Minimum amendments required (not made by this document):**

1. Restore a genuine promotion-qualifying gate in Unit 6 Clarification §6/§7, consistent with Contract Design V2 §5's "what qualifies"/"what never qualifies" framing, rather than treating the three named structural prerequisites as the only possible grounds for non-promotion.
2. Either (a) document how corroboration/contradiction relationships satisfy or lawfully supersede Contract Design V2 §5's six named factor categories (repetition, importance, relevance, frequency, confidence, explicit request), or (b) extend the authorised factor set to address the remaining four factors, or explicitly and individually disclose each as currently unreachable, the way `ContentNature` already is.
3. Add an express, documented governing-rule exception for single-factor `CORROBORATED_EVIDENCE` assignment (mirroring the kind of textual anchor Contract Design V2 §3 gives `COMPETING_EXPLANATIONS`), or require a second, genuinely independent factor before `CORROBORATED_EVIDENCE` may be assigned, so that corroboration is never sufficient alone.
4. Add the common-origin check Contract Design V2 §5 and Scope Lock §6 (Article XI) require before a `SUPPORTS` relationship may contribute corroborative weight at all.
5. Determine, per Scope Lock §10's own Out-of-Scope Change Policy, whether narrowing the promotion-boundary model and factor set (as the current Unit 6 Clarification §6–§7 do) exceeds a "narrow clarification's" authority and instead requires a formal Scope Lock revision, given §10's own text that any change to "a public contract Contract Design Version 2 froze" requires formal governance.
