**Status:** Architectural design study only. No governance is amended, no implementation is proposed, and no API or algorithm is defined. No Kotlin is implemented, proposed, or changed. Neither `src/` nor `tests/` is touched. Nothing is staged, committed, or pushed. This document evaluates candidate constitutional models; it does not select one for implementation and does not itself become binding governance.

# CDR-003 — Comparison Model Evaluation

Programme: **Parker Constitutional Design Study 003.**

This study proceeds from `docs/decisions/CDR-001_MEMORY_RECORD_COMPARISON_VS_SEMANTIC_RETRIEVAL.md` (no proven contradiction) and `docs/decisions/CDR-002_CONSTITUTIONAL_INTERPRETATION_OF_COMPARISON.md` (the Constitution deliberately leaves the comparison mechanism undefined). It reviewed both CDRs, `docs/architecture/MEMORY_CORE_SCOPE_LOCK.md`, `docs/architecture/MEMORY_CONTRACT_DESIGN.md`, `docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_CONTRACT_DESIGN_V2.md`, `docs/governance/PROGRAMME_3_CONSTITUTIONAL_REMEDIATION_ROADMAP.md`, and Article XI (`docs/architecture/epistemic-integrity.md`) in full, all previously read in this audit trail.

---

## Model A — Structural Comparison

Comparison is limited to constitutional facts already present: identifiers, provenance, document identity, entity identity, explicit relationships, exact assertions, hashes, metadata. Meaning is never evaluated.

**Constitutional consistency.** Highly consistent with Memory Core Scope Lock, whose entire retrieval and architecture doctrine already insists on "structural criteria only... no scoring, no ranking... no relevance judgment of any kind" (§10) and "Deterministic behaviour... Repeatable retrieval" (§11). Model A requires no new capability *category* — only an additive structural check using data Memory Core already owns. It aligns closely with Article XI's own treatment of "repeated" accounts, which reasons in source-lineage terms ("common upstream source") — itself a structural, provenance-traceable property, not a meaning judgment (CDR-002, Question 3). Its risk to Contract Design V2 is not conflict but hollowness: exact-match-only comparison may satisfy the letter of "comparing a submission against Memory's own existing records" while rarely firing in practice, since genuine repeated information is seldom worded identically.

**Architectural simplicity.** Lowest coupling of the three models — no new subsystem, no new dependency, no new Runtime wiring beyond what Memory Core already requires.

**Long-term flexibility.** Weakest of the three. A ceiling is built in: if real-world usage shows exact/structural matching under-detects genuine repetition, evolving past that ceiling would require reopening this same governance question later, exactly the outcome `docs/architecture/MEMORY_CONTRACT_DESIGN.md`'s own "must not be foreclosed" language was written to avoid.

**Provenance integrity.** Strongest — fully deterministic given fixed inputs, by construction.

**Auditability.** Strongest — every match or non-match traces to an explicit, inspectable criterion.

**Human review.** Strongest — a human can verify any outcome by direct inspection, with no interpretive step to second-guess.

**Trust.** Strongest — nothing interprets meaning on the owner's behalf; there is no new capability whose own trustworthiness must separately be established.

**Runtime impact.** Minimal.

**Future World Model integration.** Composes cleanly with any future subsystem precisely because it is narrow and purely structural, but its limited detection power may make it a poor foundation if Reasoning Context or World Model later need richer cross-referencing.

---

## Model B — Canonical Record Comparison

Comparison is performed against a constitutional representation of knowledge. The Constitution defines only the guarantees comparison must uphold; the mechanism (structural, rule-based, model-assisted, human-assisted) is left open and may change over time without altering constitutional behaviour.

**Constitutional consistency.** This model is the direct architectural expression of CDR-002's own finding: `docs/architecture/MEMORY_CONTRACT_DESIGN.md` already states that a future comparison mechanism "must not be foreclosed," naming "model-backed, human-in-the-loop, or simply... persistence layer with real I/O" as anticipated alternatives. Model B does not invent this openness — it operationalises language already present in frozen governance. Consistency with Contract Design V2 (no single factor, multi-factor weighing) and Article XI (independence, common-origin) is preserved *only if* the constitutional guarantees explicitly require it of every future mechanism — consistency here is designed-in, not automatic.

**Architectural simplicity.** Moderate. It introduces a genuine guarantee-bound seam, which is more structure than Model A, but this is a well-precedented idiom in this codebase (`KnowledgePromotionPolicy`, `PermissionEngine` are both already swappable-implementation seams behind a fixed contract) — the incremental complexity is low because it fits an idiom Parker already uses elsewhere.

**Long-term flexibility.** Strongest of the three. Explicitly designed so Parker's comparison quality can improve over time without requiring a constitutional amendment each time the underlying technique changes.

**Provenance integrity.** Determinism is not automatic — it must be one of the fixed guarantees (mirroring Memory Core Scope Lock §11's existing "given the same stored state and the same query, every... mode returns the same result" language), binding on whichever mechanism is active at any time.

**Auditability.** Strong, conditioned on the guarantees requiring every comparison outcome that feeds a promotion or classification decision to disclose its basis — mirroring `KnowledgePromotion.basis`'s already-established disclosure discipline.

**Human review.** Strong but qualitatively different from Model A: a human can verify that a disclosed basis is consistent with a deterministic, replayable outcome, even where a more sophisticated mechanism's own internal process is not independently re-derivable. This asymmetry is a genuine, honestly-disclosed weakness relative to Model A, not eliminated by strong guarantees, only bounded by them.

**Trust.** The owner retains control at the constitutional layer, since guarantees cannot be silently altered by swapping mechanisms — but each new mechanism introduces its own operational trust question (for example, trusting a specific model provider's behaviour) that Model A never raises. Model B also surfaces a genuine Article XV question left open by its own design: if a future mechanism is "model-assisted," care is required that the mechanism supplies a *comparison signal* Knowledge Memory itself still weighs, rather than a *pre-formed evidential judgment* that quietly displaces Knowledge Memory's own, exclusively-assigned evaluation ("no subsystem determines its own evidential status"). Model B's guarantees must foreclose this specifically; the model itself only creates the space where the question could arise.

**Runtime impact.** Minimal to moderate — comparable to any other Memory Core capability; no evidence found that this seam alone requires new Runtime wiring.

**Future World Model integration.** Strongest — a guarantee-only, mechanism-agnostic seam composes cleanly with Reasoning Context, World Model, and Retrieval evolving independently, since none of them need to know which technique is currently active.

---

## Model C — Semantic Comparison

Comparison explicitly evaluates meaning. Memory Core cannot own it under its own frozen exclusions; ownership must move elsewhere.

**Constitutional consistency.** Weakest of the three, and the only one requiring a change to an existing ownership boundary. Memory Core Scope Lock's own repeated language — "the one exclusion most likely to be quietly reintroduced under a different name" (§10) — describes almost exactly what Model C proposes, even though CDR-001/CDR-002 found that exclusion textually confined to a differently-purposed retrieval capability, not comparison. By explicitly requiring meaning-evaluation as its defining operation, Model C moves the underlying question from "is this excluded?" (CDR-001/002: not proven) to "does this require relocating a responsibility Memory Core Scope Lock §5 currently assigns?" (yes, by the model's own admission — "ownership must move elsewhere"), which is a materially larger governance act than either other model requires. It also raises a direct Article XV question: relocating meaning-evaluation to another subsystem risks that subsystem effectively pre-judging evidential status ahead of, or instead of, Knowledge Memory's own exclusively-assigned classification — the separation Article XV exists to protect.

**Architectural simplicity.** Highest coupling of the three — requires a new or newly-burdened owning subsystem, a new dependency on Memory Core's record population from outside Memory Core (risking the "no duplicate sources of truth" principle unless very carefully scoped), and plausibly new Runtime/permission wiring.

**Long-term flexibility.** Deceptively low despite appearing the most "advanced": baking meaning-evaluation directly into the constitutional model (rather than treating it as one swappable option under fixed guarantees, as Model B does) risks tightly coupling governance itself to one technology approach — precisely the "premature technology commitment" Memory Core Scope Lock explicitly declines elsewhere (embeddings, vector databases) for lack of "concrete need."

**Provenance integrity.** Weakest — meaning-based judgments (however produced) are the least naturally deterministic of the three; a model or technique update could change comparison outcomes for unchanged stored data, directly threatening the "Deterministic behaviour... Repeatable retrieval" guarantee Memory Core Scope Lock §11 requires elsewhere in the system, unless extensively, separately engineered around.

**Auditability.** Weakest unless heavily constrained — a meaning judgment is harder to explain in terms a human can verify independently of also trusting the judgment mechanism itself.

**Human review.** Weakest — independently checking a semantic judgment requires a human to re-form their own meaning-judgment, a qualitatively different, more subjective act than checking a structural fact.

**Trust.** Weakest — introduces reliance on a new capability's own trustworthiness that the owner has the least native visibility into, among the three models.

**Runtime impact.** Most complicated — a new owning subsystem plausibly requires new composition wiring and its own permission considerations.

**Future World Model integration.** Mixed: it could integrate with Reasoning Context, which already performs interpretive reasoning, but risks blurring the exact boundary Article XV protects if not very carefully scoped; World Model integration remains permanently excluded regardless of which model is chosen (Scope Lock §4).

---

## Comparative Analysis

| Criterion | Model A | Model B | Model C |
| --- | --- | --- | --- |
| Constitutional consistency | High | High (contingent on guarantee design) | Low–Moderate (requires an ownership change; raises Article XV questions) |
| Simplicity | Highest | Moderate | Lowest |
| Auditability | Highest | High (contingent on a disclosure guarantee) | Low |
| Provenance | Highest (fully deterministic) | High (contingent on a determinism guarantee) | Low (technique changes threaten repeatability) |
| Trust | Highest | High (mechanism-level trust still varies) | Lowest |
| Extensibility | Lowest (hard ceiling) | Highest (designed for evolution) | Moderate (extensible but technology-coupled) |
| Human review | Highest | High (qualitatively weaker than Model A) | Lowest |
| Future AI compatibility | Lowest (structurally excludes it) | Highest (explicitly accommodates it under fixed guarantees) | Nominally high, poorly governed (ownership/trust unresolved) |
| Risk | Low (risk of substantively hollow compliance) | Moderate (risk concentrated in guarantee-design quality) | High (Article XV tension, determinism loss, technology lock-in) |

---

## Stress Testing

Each scenario is evaluated for constitutional behaviour only — what each model would honestly claim to have determined, never how it would be implemented.

**Scenario 1 — Two identical assertions.**
- *Model A:* Detected reliably; this is exactly what structural comparison is built for. Article XI's common-origin check still applies before the match may be treated as corroboration.
- *Model B:* Detected reliably by any conforming mechanism, since the constitutional guarantees would require this trivial case to succeed regardless of implementation.
- *Model C:* Also detected, but this scenario does not exercise the added capability Model C exists to provide — an exact match needs no meaning-interpretation at all.

**Scenario 2 — Different wording describing the same event.**
- *Model A:* Not detected. This is Model A's central, honestly-disclosed weakness — paraphrase defeats exact/structural matching, understating true repetition.
- *Model B:* May or may not be detected, depending on which mechanism is currently active; the guarantees ensure whatever is detected is deterministic and disclosed, not that paraphrase is always caught. This is an honest limit, not a claimed capability.
- *Model C:* Detected — this is the scenario Model C is designed to solve. Detection of similarity is not, however, the same as correctly assessing independence: two differently-worded accounts of the same event may or may not share a common origin, and meaning-similarity alone does not answer that separate, Article XI-governed question.

**Scenario 3 — Conflicting evidence.**
- *Model A:* Out of scope by construction — contradiction is already handled structurally, via explicit `CONTRADICTS`/`DISPUTES` relationship records (Contract Design V2 §3), not inferred from content comparison. No conflict with Model A's own boundaries.
- *Model B:* Same separation must hold under any mechanism; the constitutional guarantees must explicitly keep "comparison" and "contradiction resolution" distinct so a future mechanism cannot conflate them.
- *Model C:* Highest risk. A meaning-similarity judgment could rate two substantively opposed statements as "similar" (they may share most of their content while asserting opposite facts), risking exactly the "silent preference for one side" Contract Design V2 §3 forbids, if similarity and agreement are not kept rigorously distinct.

**Scenario 4 — Multiple independent witnesses.**
- *Model A:* Maps naturally onto the independence question, since checking shared `Provenance.sourceIdentifier` or common upstream links is the same kind of traceable, structural fact Article XI itself is written in terms of ("common upstream source").
- *Model B:* Sound only if the guarantees require an independent, provenance-based common-origin check regardless of mechanism — this cannot be assumed, it must be specified.
- *Model C:* Weakest fit. Meaning-similarity and source-independence are orthogonal: two genuinely independent witnesses may describe an event identically in substance while being fully independent in origin, and Model C's own premise (comparison = meaning evaluation) does not by itself answer the independence question at all.

**Scenario 5 — Future AI reasoning provider changes.**
- *Model A:* Entirely unaffected, but also entirely unable to benefit from any future improvement — the ceiling is permanent.
- *Model B:* This is precisely the scenario Model B is built for: a new reasoning-provider capability could be adopted as one more mechanism satisfying the same fixed guarantees, without any constitutional amendment, provided it actually satisfies them.
- *Model C:* Most exposed. Because the mechanism *is* the meaning-evaluator, a provider change directly and immediately changes comparison's own behaviour for unchanged stored data — the same "premature technology commitment" risk Memory Core Scope Lock already declined to accept for embeddings and vector databases, now built into the constitutional model itself rather than merely into one swappable implementation choice.

---

## Recommendation

```
Model B — Canonical Record Comparison
```

Ranked against the stated priorities: constitutional integrity is preserved because Model B is not a new invention but the direct architectural expression of language already present in frozen governance (`docs/architecture/MEMORY_CONTRACT_DESIGN.md`'s "must not be foreclosed"), confirmed by CDR-002 as deliberate, not accidental. Trust-first architecture is preserved by binding every present and future mechanism to the same fixed guarantees rather than trusting any one mechanism's internal workings. Long-term maintainability is the model's defining strength — Parker's comparison quality can improve over time without repeatedly reopening governance, unlike Model A's hard ceiling. Separation of responsibilities is preserved provided the guarantees explicitly foreclose any mechanism from displacing Knowledge Memory's own, exclusively-assigned evidential judgment (Article XV) — a condition this study states as a requirement, not an assumption. Explainability and auditability are preserved by a mandatory disclosure guarantee mirroring `KnowledgePromotion.basis`'s already-established discipline. Model A was not rejected for being harder to build — it was rejected because its long-term flexibility and future-AI-compatibility scores are the lowest of the three, and because its simplicity is a form of implementation ease this study was instructed to weigh least. Model C was rejected on constitutional grounds specifically: it requires relocating an assigned responsibility, raises an unresolved Article XV tension, and threatens the determinism guarantee Memory Core's architecture relies on throughout.

### Constitutional Guarantees Model B Must Provide

No algorithm, API, or implementation is defined here — only the behavioural guarantees any future mechanism must satisfy:

1. **Determinism.** Given the same stored Memory Core state and the same candidate, comparison shall yield the same result every time, mirroring Memory Core Scope Lock §11's existing determinism and repeatability guarantee.
2. **No mutation.** Comparison shall never alter any Memory Core record, provenance, or lifecycle status.
3. **No promotion, no classification.** Comparison shall never itself decide promotion or assign an evidential state; its output is one input among several to Knowledge Memory's own, separately-governed, multi-factor weighing (Contract Design V2 §5).
4. **Disclosed basis.** Every comparison outcome that contributes to a promotion or classification decision shall be disclosed in terms a human can inspect, mirroring `KnowledgePromotion.basis`'s existing discipline. A mechanism whose basis cannot be disclosed does not satisfy this guarantee, regardless of technique.
5. **Independence preserved.** Comparison shall never, by itself, determine or imply independent corroboration. Article XI's own common-origin/source-lineage test remains a separate, mandatory check applied to whatever repetition comparison identifies, regardless of mechanism.
6. **Comparison is not contradiction resolution.** Comparison shall never conflate content similarity with agreement. Contract Design V2 §3's existing contradiction handling, via explicit `CONTRADICTS`/`DISPUTES` relationships, remains entirely separate and is never superseded or reinterpreted by a comparison outcome.
7. **No ownership transfer.** Regardless of which mechanism satisfies these guarantees, Knowledge Memory remains the sole party that weighs comparison's output into a promotion or classification decision (Article XV). No mechanism may itself assert an evidential judgment.
8. **No technology commitment.** These guarantees bind behaviour, not technique. Adopting or replacing a mechanism (structural, rule-based, model-assisted, human-assisted) never requires amending these guarantees themselves, and never requires reopening Memory Core Scope Lock or Contract Design V2, provided the guarantees remain satisfied.
9. **Disclosed asymmetry, never concealed.** Where a mechanism's own internal process is not independently re-derivable by a human even though its outcome is deterministic and disclosed, that asymmetry must itself be disclosed, consistent with Article XIII's transparency-of-uncertainty discipline — never presented as equivalent to Model A's trivial verifiability.

---

## Final Report

**Document created:** `docs/studies/CDR-003_COMPARISON_MODEL_EVALUATION.md` (only file created; no other file modified).

**Models evaluated:** three — Structural Comparison (A), Canonical Record Comparison (B), Semantic Comparison (C).

**Constitutional strengths:** Model A — full determinism, auditability, human-verifiability, zero new coupling. Model B — matches frozen governance's own deliberate open-endedness (CDR-002); highest long-term flexibility; preserves separation of responsibilities if its guarantees are honoured. Model C — the only model that could detect paraphrased repetition directly, but at the cost of every other constitutional property examined.

**Constitutional weaknesses:** Model A — a permanent detection ceiling; risk of substantively hollow compliance. Model B — every strength is conditional on the guarantees being specified correctly, particularly the Article XV foreclosure; qualitatively weaker human-review than Model A. Model C — requires relocating an assigned responsibility, threatens determinism, raises an unresolved Article XV tension, and repeats the "premature technology commitment" risk Memory Core Scope Lock already declined elsewhere.

**Stress-test outcomes:** Model A fails only the paraphrase scenario (Scenario 2); Model B's behaviour in every scenario depends on guarantee quality rather than the model itself; Model C succeeds at paraphrase detection (Scenario 2) but is the weakest fit for independence (Scenario 4) and carries the highest risk in the conflicting-evidence scenario (Scenario 3) and the future-provider-change scenario (Scenario 5).

**Recommended constitutional model:** Model B — Canonical Record Comparison, with the nine constitutional guarantees listed above as mandatory, non-negotiable properties of any future mechanism.

CDR-003 COMPARISON MODEL EVALUATION COMPLETE

Confirmed: no production code modified; no tests modified; no governance documents modified; only the new study document created; nothing staged; nothing committed; nothing pushed; Unit 7 not started.
