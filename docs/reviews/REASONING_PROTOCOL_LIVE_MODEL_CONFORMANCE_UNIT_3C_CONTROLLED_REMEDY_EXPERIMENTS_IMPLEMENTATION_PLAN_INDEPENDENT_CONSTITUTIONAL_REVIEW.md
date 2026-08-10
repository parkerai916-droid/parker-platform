**Status:** Independent Constitutional Review of the Unit 3-C Controlled Remedy Experiments Implementation/Execution Plan — **ACCEPTED WITH QUALIFICATIONS.** The Plan was treated as evidence, not authority. The exact call total was independently re-derived by arithmetic, not accepted from the Plan's own table. The Family B candidate prompt's SHA-256 was independently re-extracted from the file's own literal text block and re-hashed, not accepted from the Plan's own claimed digest. The Family C mechanism's manual trace table was independently re-traced by hand against its own written specification for a sample of rows, not accepted on the Plan's word. Every internal section cross-reference was checked against the Plan's own actual header numbering. No live model call, no HTTP call, no campaign, and no repository mutation beyond this document occurred. The Plan itself was not modified by this review.

# Unit 3-C Controlled Remedy Experiments Implementation/Execution Plan — Independent Constitutional Review

## 1. Method

Re-read the Plan in full against the frozen Unit 3-C Scope Lock (re-extracting the Scope Lock's own current, corrected section headers directly, not from memory), the Unit 3-C Planning Review, and the Family C Fixture Coverage Audit. Independently recomputed the exact live-call total by arithmetic. Independently re-extracted the literal candidate-prompt text block from the Plan file itself and recomputed its SHA-256, comparing against the digest the Plan claims. Independently re-traced five rows of the Family C mechanism's predicted-outcome table by hand against the mechanism's own written five-step specification. Extracted every `Section N`-style reference in the Plan (both `Scope Lock Section N` and bare `Section N` self-references) and checked each against the correct ground-truth target.

## 2. Scope Lock compliance

Confirmed, section by section: campaign structure (Scope Lock §3) → Plan §3, with the required arm-level fault-isolation binding requirement restated and a concrete mechanism proposed; common control (§4) → Plan §13, using the real production classes; authoritative corpus (§5) → Plan §4, with `SyntheticConformanceCorpus` correctly excluded; Family A/B/C purposes (§6–8) → Plan §7–9, with no production architecture selected for any family; the fixture gate (§9) → satisfied by Plan §5–6's six supplemental fixtures with independent adversarial review; the fixture-governance-tier requirement (§10, requiring exact text, expected action, rationale, category mapping, and adversarial purpose for each supplemental fixture, subject to its own dedicated Independent Constitutional Review before Family C execution) → satisfied in form by Plan §5, though satisfying that dedicated review is this document's own job, addressed in Section 5 below; evidence tier (§11) → Plan §2 and throughout; repetition principle (§12) → Plan §10, with the exact count now resolved and justified; model/inference strategy (§13–14) → Plan §12; pre-registration (§15) → Plan §15; stop conditions (§16) → Plan §18; semantic/representation separation (§17) → preserved throughout Plan §7 and §16's artifact schema; Unit 3-A traceability (§18) → not separately restated as its own Plan section but implicit in the per-family measurement definitions, a minor completeness gap noted in Section 15 below; Unit 3-D comparability (§19) → Plan §20; artifact/provenance (§20) → Plan §16; downstream isolation (§21) → Plan §19; exact-once (§22) → Plan §17; no-winner (§23) → Plan §2's explicit denial; deferred/excluded families (§24) → implicitly respected (no Family D/E/F/G/H/I experiment appears anywhere), though not restated as its own Plan section, another minor completeness gap noted below.

## 3. Family A neutrality

DQ5 is not elevated beyond its bounded role; the Plan's decision/rendering split explicitly measures both the decision step and the rendering step against the full base corpus, not merely the one fixture DQ5 used. **Qualification, non-blocking:** unlike the Scope Lock's and Planning Review's own explicit "what result would count as evidence against Family A" framing, the Plan does not restate this failure criterion explicitly as its own sentence — it is implicit in the measurement axes (a lower decision-step accuracy, a worse false-positive rate, or near-perfect joint-task rendering fidelity would all constitute evidence against the hypothesis) but a future reader would need to infer this rather than read it stated plainly. Recommended for a future revision, not blocking.

## 4. Family B neutrality

Independently re-read the full candidate text against the fixture-specificity prohibition: no fixture content (no specific synthetic value from any of the 23 base fixtures) appears anywhere in it. The one substantive addition beyond current production wording — an explicit checklist of near-miss categories to check before choosing REMEMBER: (question, quotation, hypothetical, statement of fact, mention of the topic) — was independently checked for disguised fixture-category leakage: these are general, protocol-level distinctions a prompt engineer would reasonably arrive at without reference to this programme's specific corpus, not equivalent to naming a fixture or its expected label. Judged compliant, not a violation of the "must not mention expected fixture labels" requirement.

## 5. Supplemental fixture semantics

Independently re-derived, fresh, not merely re-reading the Plan's own stated conclusions: `P14`, `P15`, and `P16` were each re-checked for a lurking instructional or interrogative-turned-instruction reading and found clean. `P17` was re-checked for a disguised present instruction inside its conditional framing and found to genuinely ask a capability question, not issue one. `P18` was re-checked against `R03-dont-forget`'s double-negative to confirm the two fixtures are not in tension, and the distinction (`P18`'s single, genuine negation versus `R03`'s idiomatic double negative netting positive) is independently confirmed sound. `G06` — flagged as the highest-residual-risk fixture in the Plan's own drafting notes — was independently re-read fresh: the governing verb and direct object ("Create a two-item synthetic checklist") dominate the sentence structure, with the "note" clauses subordinate as list-item content; no plausible hostile reading elevates "note" into an independent REMEMBER instruction. All six fixtures are judged constitutionally defensible and non-ambiguous as specified. No fixture requires further revision.

## 6. Absence of hidden production design

Confirmed: Plan §21 explicitly determines no `src/` change is required, and every family's mechanism (Family A's two prompt builders, Family B's candidate, Family C's classifier) is specified as test-tier code against already-frozen, unmodified production interfaces (`ReasoningPromptBuilder`), never a production file. No Boundary Review is silently skipped — the Plan explicitly states the determination that triggers or does not trigger one.

## 7. Exact repetition justification

n=5 is independently judged adequately justified (DQ5 precedent reuse, deliberate distinction from Unit 2's 30-per-cell qualification scale, uniform application across directly comparable cells) and n=1 for Family C's deterministic arm is independently judged correctly reasoned (repetition of a deterministic function adds no information) — not an unjustified departure from uniformity, but an explicitly reasoned, narrow exception exactly matching the category the Scope Lock's own repetition section anticipates.

## 8. Exact call accounting

Independently re-derived by direct arithmetic, not accepted from the Plan's table: warm-up 3 + control-base (23×5) 115 + control-supplemental (6×5) 30 + Family A decision (23×5) 115 + Family A rendering (21×5) 105 + Family B (23×5) 115 + Family C 0 = **483**, matching the Plan's own stated total exactly. Family C's zero-model-call status is stated with the explicit clarity the task requires.

## 9. No hidden retries

Confirmed: Plan §11 and §17 both explicitly prohibit hidden retries and automatic reruns of completed evidence; the exact-once design's "rerun prohibition" item directly forecloses this.

## 10. Model/inference freeze

Confirmed unchanged from current production: request-body shape, timeout default, endpoint, no sampling parameters added. Matches Scope Lock §13–14 exactly.

## 11. No Family E leakage

Confirmed: no inference-control parameter is touched by any family's design; Family B varies only prompt text, Family A varies only prompt structure/call sequencing, Family C makes no model call at all.

## 12. No Family F leakage

Confirmed: single model (`qwen2.5-coder:7b`) throughout; no comparison or replication arm on any other model.

## 13. No Family D leakage

Confirmed: Family B's candidate preserves the exact existing tag grammar; no structured/schema-constrained output format is introduced anywhere.

## 14. Matched-control validity

Confirmed: the control uses the real, unmodified production classes (not a hand-copied approximation), and the one legitimate corpus asymmetry (control extended to the six supplemental fixtures, to give Family C a meaningful comparison baseline) is explicitly reasoned and does not violate the Scope Lock's own comparability rules, which exempt but do not forbid this extension.

## 15. Unit 3-D comparability

Confirmed largely sufficient: Plan §20 separates numerically-comparable metrics (shared base-corpus semantic accuracy, representation validity, false-positive rate, content fidelity) from family-specific, not-directly-comparable results (Family C's supplemental-corpus results, Family A's call-count/cost structure, and the n=5-versus-n=1 stochastic/deterministic mismatch). **Qualification, non-blocking:** the Plan does not include its own explicit "Unit 3-A traceability" section restating which Unit 3-A contract dimensions each family's measurements map to, nor an explicit restatement of the "deferred and excluded families are not reopened" boundary as its own section — both are implicitly honored throughout (no family experiment touches an excluded dimension or family) but a future reader benefits from an explicit statement, as the Scope Lock itself provides in its own §18 and §24. Recommended for a future revision, not blocking, since no violation of either principle was found on inspection — only the absence of a dedicated restatement.

## 16. Artifact completeness

Confirmed: Plan §16's field table covers every field the task's Phase 17 enumerated, with nullability specified per arm, including the correct null-for-Family-C treatment of every model-call-specific field.

## 17. Exact-once semantics

Confirmed: Plan §17 addresses every element the task's Phase 18 required (intent record, raw persistence, checkpoint timing, duplicate prevention, crash recovery, raw-without-checkpoint, checkpoint-without-raw, identity mismatch, rerun prohibition, seal/halt), each with a specific, non-vague handling rule.

## 18. Downstream isolation

Confirmed absolute and unqualified in substance (Plan §19); the isolation *requirement* itself is not weakened by the one citation-pointer error found within that section (Section 22 below).

## 19. Execution gates

Confirmed: Plan §23 reproduces the task's own ten-gate sequence exactly and in order, with an explicit statement that no earlier gate substitutes for a later one and that none of gates 2–10 is performed by this Plan.

## 20. No remedy winner selected

Confirmed: Plan §2 explicitly denies remedy selection, Unit 3-D comparison, Unit 3-E selection, and Unit 4 implementation; Family C's honestly-reported predicted false positives are presented as evidence to be gathered, not as a disqualifying verdict; Family B's candidate is explicitly labeled not a production recommendation.

## 21. Independent verification of the Family B prompt hash

Independently re-extracted the literal text block between the Plan's own ` ```text ` fences and recomputed its SHA-256 directly from the file: `cfd5cb7f07d0d7da941b069e00b3f479fc5faf5e71662a83bda51f25bd629d60`, length 1525 — matching the digest the Plan states exactly. No transcription drift between the specified text and the hash claimed for it.

## 22. Independent verification of the Family C mechanism trace

Independently re-traced, by hand, against the mechanism's own five-step written specification (not against the Plan's claimed table): `R03-dont-forget` (predicted false negative via the negation mitigation firing on "don't," an idiomatic double negative) — confirmed. `P03-ambiguous-memory` (predicted false positive, no mitigation fires) — confirmed. `P04-embedded-tags` (predicted false positive, no quote character present despite the colon-suffixed tag syntax) — confirmed. `P05-mixed-memory-discussion` (predicted false positive) — confirmed. `P17-hypothetical-remember` (predicted no-signal via the interrogative mitigation, since the sentence ends in `?`) — confirmed. All five independently re-traced rows match the Plan's own table exactly. The mechanism is judged genuinely deterministic and reproducible as specified, and the predicted false-positive/false-negative profile is judged honestly reported rather than tuned to look favorable — a deliberately naive, mitigated baseline that fails in a realistic, informative way.

## 23. Blocking defects

None found.

## 24. Non-blocking qualifications — internal cross-reference errors

Independently swept every internal `Section N` reference in the Plan against its own true header numbering (twenty-four sections total) and found **eight distinct citation-pointer errors**, all purely navigational, none altering any requirement:

1. Plan §4 ("Section 15 below states precisely which comparisons remain valid across this asymmetry") should read **Section 20** (Unit 3-D comparability) — Plan §15 is actually Pre-registration.
2. Plan §7 ("the unmodified production path (Section 9 below)") should read **Section 13** (Common control) — Plan §9 is actually Family C experiment design.
3. Plan §9 ("exactly as Section 13 (Downstream isolation) requires") should read **Section 19** — Plan §13 is actually Common control.
4. Plan §9 ("well-specified and reproducible (Section 18 below)") should read **Section 22** (Verification plan) — Plan §18 is actually Stop conditions.
5. Plan §10 ("per Scope Lock Section 15's own requirement" for holding repetition identical across arms) — the frozen Scope Lock's own §12 and §15 require repetition to be *pre-registered and justified*, but do not literally state an "identical across arms unless justified" rule; that specific formulation derives from this task's own instruction text, not verbatim from the Scope Lock. The underlying practice is not wrong and is consistent with Scope Lock §12's spirit, but the citation overstates what is explicitly frozen there.
6. Plan §11 ("per Section 12 (Adaptive-experimentation prohibition, inherited from Scope Lock Section 15)") should read **Section 15** (Pre-registration) — Plan §12 is actually Model and inference identity; the Scope Lock cross-reference within the same parenthetical is correct.
7. Plan §15 ("the stop rules (Section 17)") should read **Section 18** (Stop conditions) — Plan §17 is actually Exact-once and durability design.
8. Plan §19 ("Implementation-level verification required (Section 21, verification plan)") should read **Section 22** — Plan §21 is actually Implementation surface.

**Severity assessment:** in every one of the eight cases, the sentence containing the bad pointer remains fully self-contained and correct on its own terms; no requirement is misstated, weakened, or rendered ambiguous — only the forward/backward navigation aid resolves to the wrong (but real, correctly-labeled-in-its-own-right) section. This is judged non-blocking, consistent with this programme's established treatment of citation-precision defects in the Scope Lock's own prior review (six distinct errors there). The count here is higher (eight), reflecting this document's greater length and cross-reference density; a dedicated correction pass, mirroring the Scope Lock's own precedent, is recommended before this Plan is relied upon as a navigation reference, though not required before treating it as substantively ready to freeze.

## 25. Non-blocking qualifications — completeness

Two additional, non-blocking completeness notes, both already covered above: Family A's "what would count as evidence against" criterion is implicit rather than explicitly restated (Section 3); Unit 3-A traceability and the deferred/excluded-families boundary are honored throughout but not restated as their own dedicated Plan sections (Section 15). Neither reflects an actual violation found on inspection.

## 26. Verdict

```text
ACCEPTED WITH QUALIFICATIONS
```

The Unit 3-C Implementation/Execution Plan is independently confirmed to: faithfully translate every binding requirement of the frozen Scope Lock into a fully specified, pre-registerable instrument; keep Families A, B, and C free of premature architecture selection, hidden favoritism, or production design; impose a genuinely adversarial, independently-reverified supplemental fixture set and mechanism specification for Family C, including an honestly-reported predicted-failure profile; hold model and inference configuration frozen with no Family D/E/F leakage; produce a correctly-derived, exact 483-call total with zero hidden retries; specify a complete artifact schema, exact-once design, and absolute downstream isolation; and select no remedy anywhere. The eight cross-reference errors and two completeness notes catalogued above are the only defects found, are all non-blocking, and are not preconditions for treating this Plan as ready to freeze on the merits — but are recorded precisely, per this task's instruction, for correction in a future dedicated pass rather than silently repaired here.

## 27. Confirmation

No model or HTTP call occurred during this review. No campaign was created, resumed, or modified. No production, test, or Gradle file changed. No fixture was added or modified beyond the specification text already present in the Plan under review. No remedy was selected, prototyped, or endorsed. The Plan document itself was not modified by this review.
