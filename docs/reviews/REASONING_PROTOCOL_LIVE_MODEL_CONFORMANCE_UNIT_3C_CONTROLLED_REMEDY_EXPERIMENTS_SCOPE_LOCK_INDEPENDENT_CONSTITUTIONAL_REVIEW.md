**Status:** Independent Constitutional Review of the Unit 3-C Controlled Remedy Experiments Scope Lock — **ACCEPTED WITH QUALIFICATIONS.** The Scope Lock was treated as evidence, not authority. Every internal section cross-reference was independently extracted and checked against the document's own actual header numbering, not accepted on the document's own word; every numeric token, banned-preference-word candidate, and authorization-language occurrence was independently searched and inspected in context. No live model call, no HTTP call, no campaign, and no repository mutation beyond this document occurred. The Scope Lock itself was not modified by this review.

# Unit 3-C Controlled Remedy Experiments Scope Lock — Independent Constitutional Review

## 1. Method

Extracted the Scope Lock's actual `## N.` section headers directly (`grep -n "^## "`) and independently checked every internal `(Section N)`-style cross-reference in the document against that ground-truth list, rather than trusting each pointer's apparent plausibility in isolation. Independently searched the full document for banned preference words, numeric tokens, and every "authoriz"/"select" occurrence, inspecting each in context. Cross-checked Family-family classification statements (Section 24) against the frozen Unit 3-B Scope Lock's own nine-way classification. Re-derived the Family C fixture-coverage claims (Section 9) against the committed Fixture Coverage Audit directly.

## 2. Does the one-campaign isolated-arm structure provide sufficient causal and fault isolation?

Yes. Section 3 requires independent identity and independent fault isolation per arm, states a binding requirement that one arm's measurement defect must not silently invalidate or contaminate another, and explicitly defers the exact isolation mechanism to a future Implementation/Execution Plan rather than under-specifying it as "obviously fine." No implementation detail is prematurely frozen here, consistent with this document's own stated scope.

## 3. Is one shared control constitutionally valid for all A/B/C families?

Yes, with one nuance independently checked and found non-blocking: the Unit 3-C Planning Review's own Section 10 flagged that Family C's standalone rule-profiling measurement may not invoke the model at all, unlike Family A/B's arms. Checked whether Section 4 of the Scope Lock (which requires the control to use "current model/provider") contradicts this — it does not, because Section 4 states a property of the shared *control* arm, not a property every *candidate* arm must independently replicate. No defect found, but this is a subtlety a future Implementation/Execution Plan should keep in mind rather than something this Scope Lock mishandled.

## 4. Has Family A been favored because of DQ5?

No. Section 6's "DQ5's role, bound" paragraph explicitly restricts DQ5 to justifying investigation only, denies it is a template, and denies that a favorable Family A result is anything more than exploratory evidence. Family A receives no lighter measurement burden than B or C (Section 6's five-item measurement requirement is at least as demanding as Family B's or C's own sections).

## 5. Has Family B accidentally been allowed to redesign tag grammar/parser behavior?

No. Section 7 explicitly and specifically prohibits altering the tag grammar and parser semantics, independently confirmed against the actual prohibition list.

## 6. Has Family C accidentally had an integration architecture selected?

No. Section 8 explicitly lists and denies selection of a standalone pre-classifier, a rule-assisted signal, a regex mechanism, a keyword mechanism, and any specific production integration architecture, deferring all of them to later experiment-design work.

## 7. Is the Family C fixture gate truly blocking before implementation/readiness/live execution?

Yes. Section 9's binding-gate language ("must not proceed to implementation, readiness review, or live execution") independently matches the task's own required three-stage phrasing exactly, with no hedging or softening.

## 8. Are categories 2, 3, 7 properly required as new coverage?

Yes, independently confirmed against the committed Fixture Coverage Audit (`8a59cde`): Section 9 correctly reproduces the audit's own absent/partial/confirmed classification without alteration, and requires new coverage for exactly the three absent categories.

## 9. Are categories 5, 6, 8 properly required to be strengthened?

Yes, Section 9 requires categories 5, 6, and 8 to be strengthened "with more precisely disambiguating text," matching the audit's own partial-coverage findings and its specific note that `P03`/`P05` conflate three categories without cleanly separating them.

## 10. Is exact fixture text frozen at a constitutionally appropriate later tier?

Yes, and independently found to be a well-reasoned, not merely asserted, determination. Section 10 weighs two genuine, opposing precedents (Unit 2's own Scope Lock froze its full corpus directly; Unit 2-D's Scope Lock deferred its own, mostly-reused fixture definitions to implementation tier) and resolves the tension by retaining the deferral while imposing a compensating requirement — a dedicated Independent Constitutional Review of the fixture section, not merely ordinary Completion or Readiness review — before Family C may pass the gate. This directly and precisely satisfies the task's own explicit fallback specification for this determination.

## 11. Is `SyntheticConformanceCorpus` explicitly excluded from authoritative corpus status?

Yes, explicitly, in Section 5, with the reasoning (a smaller, differently-worded, differently-IDed collection used only by an unrelated harness-mechanics self-test) independently matching the Fixture Coverage Audit's own finding.

## 12. Has an exploratory repeat count been invented?

No. Independently re-extracted every numeric token in the document: `≥30`, `≥300` (Section 11, exact restatements of already-frozen Unit 3-A qualification thresholds, cited as *not* what Unit 3-C evidence meets) and `30_000` (Section 14, the existing, already-implemented `ModelReasoningProvider` timeout default, a code fact, not an invented governance number). Section 12 explicitly states the exact repeat count is unresolved and assigns it to a future artifact. No invented number found anywhere.

## 13. Has exploratory evidence been confused with qualification evidence?

No. Section 11 explicitly and separately denies four distinct possible confusions (qualification evidence, production-selection evidence, Unit 3-A conformance proof, production-readiness evidence), each named individually rather than lumped into one vague denial.

## 14. Has Qwen-only strategy accidentally become a model-selection remedy?

No. Section 13's reasoning is confound-avoidance only ("would reopen that excluded question as an uncontrolled side effect and confound attribution... Unit 3-C must not become a model benchmark"), independently checked against the banned-word search: no superiority or ranking language appears anywhere in the section.

## 15. Has Family E inference-control experimentation leaked in?

No. Section 14 explicitly prohibits any Family E change, enumerates exactly what must be pinned (including the previously undocumented Ollama-server-default risk carried forward from the Planning Review), and closes with an explicit, unqualified denial that any temperature/seed/top-p/top-k experiment belongs in scope.

## 16. Does adaptive-experiment prohibition prevent post-hoc candidate tuning?

Yes. Section 15 requires pre-registration of a comprehensive list of experiment parameters, explicitly prohibits the run-inspect-tweak-rerun sequence, and requires a materially changed candidate to receive separate, fresh governance rather than silent amendment.

## 17. Are semantic and representation axes independent?

Yes, Section 17 is an unweakened, faithful restatement of Unit 3-A Section 6, independently re-checked against that source.

## 18. Are false-positive REMEMBER/GOAL outcomes treated as safety-critical evidence?

Yes. Section 16 both records them as remedy-performance evidence and separately imposes an elevated, binding safety requirement — a governed manual review checkpoint for concentrated false-positive events — without inventing the numeric concentration threshold that would trigger it, correctly deferring that number to a future artifact rather than fabricating one to appear complete.

## 19. Are stop conditions appropriately differentiated?

Yes. Section 16 cleanly separates measurement-invalidating failures (fail closed) from remedy-performance failures (recorded as evidence), consistent with the Unit 3-C Planning Review's own more elaborated reasoning for why Unit 2's Stage 0 mechanism does not mechanically transfer — this Scope Lock states the resulting requirement without re-deriving that background reasoning in full, which is appropriate for a Scope Lock's register and not a defect.

## 20. Are Unit 3-D comparison requirements sufficient?

Yes, substantively. Section 19 lists eleven required shared elements and explicitly names two family-specific results that must not be numerically compared without further methodology. One citation defect was found here and is recorded in Section 26 below.

## 21. Can Unit 3-C results be overclaimed as a winner?

No — Section 23 states, prominently and without qualification, that no report produced under this Scope Lock may declare a winner, and that favorable performance on any exploratory measure does not itself constitute selection.

## 22. Is downstream isolation absolute?

Yes. Section 21 is unqualified, applies equally across all three families, and independently includes "Knowledge Submission" as a named prohibited effect — an item the task's own instruction added to this document's requirements beyond what the Unit 3-C Planning Review had separately enumerated, correctly incorporated here.

## 23. Are artifact/exact-once requirements sufficient at governance level?

Yes, for a governance-level document. Sections 20 and 22 each state a comprehensive requirements list and explicitly decline to implement any of it, deferring the concrete schema and mechanism to a future Implementation/Execution Plan.

## 24. Have deferred/excluded families leaked back into Unit 3-C?

No — and Section 24 is independently found to be *more* precise than the task's own prompt text on this point: rather than flattening Family G into one undifferentiated "retry/repair" entry, Section 24 preserves the frozen Unit 3-B split (representation-only retry deferred; semantic retry/repair excluded on current evidence) exactly as Unit 3-B itself froze it. This is a positive finding, not a defect.

## 25. Has any Unit 3-D, Unit 3-E, or Unit 4 authority leaked backward?

No, checked independently for each: no comparison of any actual result is performed anywhere (none exists to compare); no remedy is selected anywhere (Section 23 explicit); no production code is named as an authorized change target — `DefaultReasoningPromptBuilder`, `TaggedReasoningResponseParser`, and `LocalHttpModelInferenceClient` are named only as boundaries on what must *not* be altered by any experiment, never as implementation targets.

## 26. Is live execution or implementation authorized?

No, to both. The document's own Status line states this explicitly, and independent inspection confirms it: no campaign identity is minted, no endpoint is constructed, no fixture text, prompt wording, or rule/regex is written anywhere, and every operative section uses binding-future-obligation language ("must," "frozen," "the future Implementation/Execution Plan must") rather than present-tense execution or implementation language.

## 27. Is the Scope Lock precise enough for a later Implementation/Execution Plan without inventing major experimental governance there?

Yes. Independently enumerated what remains open for a future Plan to supply: the exact repeat count and its justification (Section 12); the exact Family C supplemental fixture text, together with its own dedicated Independent Constitutional Review (Section 10); the exact Family B candidate prompt wording (Section 7); the exact false-positive concentration threshold (Section 16); the concrete artifact schema (Section 20); and the concrete arm-isolation mechanism (Section 3). Every one of these six items is explicitly named and explicitly assigned to that later tier by this Scope Lock itself — none is a silent gap the next document would have to invent unguided.

## 28. Blocking defects

None found. No requirement, threshold, false-positive protection, semantic/representation separation, evidence-tier boundary, or authority boundary was weakened, invented, or leaked across a unit boundary anywhere in the document.

## 29. Non-blocking qualifications — internal cross-reference errors

Independently extracted every `## N.` header and every internal `(Section N)` cross-reference in the document and checked each pointer against the true numbering. **Six distinct citation-pointer errors were found, none of which alters any requirement, threshold, or binding obligation — every affected sentence remains fully correct and self-contained without its pointer.** Recorded precisely, not silently corrected:

1. Section 2 (line containing "produce exploratory evidence (Section 10)") should read **Section 11** (Evidence tier), not Section 10 (Fixture governance tier).
2. Section 4 ("current model/provider (Section 12)") should read **Section 13** (Model strategy), not Section 12 (Repetition).
3. Section 4 ("current inference configuration (Section 13)") should read **Section 14** (Inference configuration), not Section 13 (Model strategy).
4. Section 4 ("same artifact/provenance rules (Section 19)") should read **Section 20** (Artifact/provenance requirements), not Section 19 (Unit 3-D comparability).
5. Section 6 ("remains exploratory evidence only (Section 10)") should read **Section 11** (Evidence tier), repeating error 1's mistake.
6. Section 7 ("Family G, deferred/excluded per Section 22") should read **Section 24** (Deferred and excluded families), not Section 22 (Exact-once and durability principles).
7. Section 7 ("presented as one registered experiment's evidence (Section 14)") should read **Section 15** (Adaptive-experimentation prohibition), not Section 14 (Inference configuration).
8. Section 15 ("the artifact schema (Section 19)") should read **Section 20** (Artifact/provenance requirements), not Section 19.
9. Section 19 ("full artifact provenance (Section 19 below) per family") is a self-reference — it appears *inside* Section 19 pointing back at itself — and should read **Section 20** (Artifact/provenance requirements).

By contrast, Section 25 (Exit Criteria)'s fifteen internal cross-references were independently checked one by one and found **entirely correct** — every pointer in that section resolves to its intended target. The error pattern is therefore concentrated in earlier forward-references (Sections 2, 4, 6, 7, 15, 19) written before the document's final section count and ordering had stabilized, not a pervasive defect throughout.

**Assessment of severity:** every one of the nine occurrences is a navigation aid pointing to the wrong (but existing, correctly-labeled-in-its-own-right) section — none invents a requirement, removes one, or changes what is actually required, and the substantive sentence containing each bad pointer remains independently clear and correct without following it. This is judged non-blocking, consistent with this programme's established treatment of citation-precision defects (compare the Unit 3-A Scope Lock's own three comparable findings, also judged non-blocking) — but the count here (six distinct errors, nine occurrences) is higher than any prior Scope Lock in this programme and should be corrected in the next available editing pass before this document is relied upon as a navigation reference, even though it does not block freezing on substance.

## 30. Verdict

```text
ACCEPTED WITH QUALIFICATIONS
```

The Unit 3-C Controlled Remedy Experiments Scope Lock is independently confirmed to: preserve the frozen Unit 3-A contract and Unit 3-B classification without weakening, strengthening, or inventing any threshold; keep Families A, B, and C free of premature architecture selection, favoritism, or model-benchmarking drift; impose a genuinely binding, correctly-scoped Family C fixture gate with a well-reasoned governance-tier determination for the supplemental fixture text; keep evidence tiers, semantic/representation axes, downstream isolation, and false-positive safety fully intact and appropriately elevated where warranted; and remain free of Unit 3-D/3-E/Unit 4 authority leakage or live-execution/implementation authorization in either direction. The nine internal cross-reference errors catalogued in Section 29 are the only defects found, are all non-blocking, and are not preconditions for treating this Scope Lock as ready to freeze on the merits — but are recorded precisely, per this task's instruction, for correction in a future editing pass rather than silently repaired here.

## 31. Confirmation

No model or HTTP call occurred during this review. No campaign was created, resumed, or modified. No production, test, or Gradle file changed. No fixture was added or modified. No remedy was selected, prototyped, or endorsed. The Scope Lock document itself was not modified by this review.
