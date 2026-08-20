**Status:** Independent Constitutional Review of the Family F Alternative-Model Diagnostic Planning Review Model Role and Research Question Amendment — **ACCEPTED, with one non-blocking observation.** Every proposition the amendment claims to correct was independently re-verified against the historical Planning Review's own committed text, and an independent, exhaustive term-level sweep of that document found no materially reversed model-role proposition the amendment omits. The research-question text is independently confirmed byte-identical to the controlling Scope Lock's own frozen text. The downstream dependency into the Experimental Reclassification and Qualification-Boundary Scope Lock is independently confirmed real and correctly left unedited. One non-blocking observation is recorded (Section 6 below): a single stop-condition item in the unamended body now names a less-relevant model, though the protective function it serves is already independently and explicitly restated, for the corrected model, by the amendment's own Section 10. No historical evidence was found altered. No authority beyond the amendment's own drafting was found granted. Nothing was edited by this review; nothing was staged, committed, or pushed.

# Family F Alternative-Model Diagnostic Planning Review Model Role Amendment — Independent Constitutional Review

## 1. Independent evidence reviewed

Read fresh for this task:

- `docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_FAMILY_F_ALTERNATIVE_MODEL_DIAGNOSTIC_PLANNING_REVIEW.md` — in full (356 lines), independently re-read end to end, not sampled.
- `docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_FAMILY_F_ALTERNATIVE_MODEL_DIAGNOSTIC_PLANNING_REVIEW_INDEPENDENT_CONSTITUTIONAL_REVIEW.md` — its existing, already-accepted ICR, re-read for scope and baseline commit.
- `docs/architecture/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_FAMILY_F_MODEL_ROLE_AND_RESEARCH_QUESTION_SCOPE_LOCK.md` — the controlling authority, re-read in full.
- `docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_FAMILY_F_MODEL_ROLE_AND_RESEARCH_QUESTION_SCOPE_LOCK_CORRECTION_INDEPENDENT_CONSTITUTIONAL_REVIEW.md` — its fresh, accepted correction ICR.
- `docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_FAMILY_F_ALTERNATIVE_MODEL_DIAGNOSTIC_PLANNING_REVIEW_MODEL_ROLE_AMENDMENT.md` — the target of this review, in full.
- `docs/architecture/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_EXPERIMENTAL_RECLASSIFICATION_AND_QUALIFICATION_BOUNDARY_SCOPE_LOCK.md` — Sections 7–8, re-read fresh, solely to verify the amendment's own stated dependency claim.
- `docs/architecture/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3C_TIMEOUT_AND_DURABILITY_SCOPE_LOCK_AMENDMENT.md` — the established amendment-mechanism precedent, re-read for structural comparison.

```text
$ grep -n -iE "current model|live model|current configuration|control model|subject model|candidate model|qwen2\.5-coder:7b|llama3\.2:3b|alternative model|comparison model" \
    docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_FAMILY_F_ALTERNATIVE_MODEL_DIAGNOSTIC_PLANNING_REVIEW.md
```
run fresh, independently, for this review — 14 matching lines, each individually attributed in Section 5 below.

```text
$ diff <(the FAMILY_F_RESEARCH_QUESTION block extracted from the Scope Lock) \
       <(the FAMILY_F_RESEARCH_QUESTION block extracted from the amendment)
(no difference within the block itself — the two blocks are byte-identical;
the only reported diff lines were past each block's own closing fence,
an artifact of the extraction pattern, not a text difference)
```

## 2. Findings A — amendment mechanism

1. **A separate superseding amendment is the correct mechanism**, independently confirmed against the Unit 3-C Timeout and Durability Scope Lock Amendment precedent, re-read fresh: that precedent is structured identically — Status line naming what it amends and its controlling authority, a "Baseline and authority" section naming exact amended sections, per-section "original text preserved / frozen, amended" pairs, an explicit "prohibited interpretations" section, and a Status block stating it is not effective until its own ICR is accepted. The target amendment reproduces this exact shape.
2. **The historical Planning Review body is preserved intact** — independently confirmed via `git diff --stat` for that file, empty (no tracked change).
3. **The amendment is clearly subordinate to the accepted Scope Lock** — its own Status line and Section 1 state its entire authority derives from the Scope Lock and its correction ICR, and it introduces no role or question content of its own beyond what the Scope Lock already froze.
4. **The amendment is clearly not effective merely by being drafted** — its own Status line and Section 11 both state this explicitly, and its own `NEXT_LAWFUL_ACTION` field names only its own ICR.

## 3. Findings B — role correction

Independently re-verified against the historical Planning Review's own committed text (not the amendment's restatement): Section 7 items 2 and 4, Section 12, and Section 18's two named rows are, in their original, unamended form, exactly the reversed assignment the amendment quotes and corrects. The amendment's corrected values —

```text
DEPLOYED_BASELINE = qwen2.5-coder:7b
CONTROL_MODEL     = qwen2.5-coder:7b
SUBJECT_MODEL     = llama3.2:3b
```

— are independently confirmed to match the controlling Scope Lock's own frozen values exactly (Section 1 of the amendment quotes them verbatim; independently checked against the Scope Lock's own Section B).

**No remaining Planning Review proposition still substantively assigns the roles backwards without being covered.** This is the central finding of Section 5 below, reached by exhaustive term sweep, not by trusting the amendment's own claimed scope.

## 4. Findings C — research question

Independently confirmed byte-identical (Section 1 above) to the controlling Scope Lock's own frozen `FAMILY_F_RESEARCH_QUESTION` block — no paraphrase, no broadening, no narrowing. The amendment's own surrounding prose (Section 3) correctly characterizes this as the same screening question with the candidate/baseline correctly identified, not a new or broader question. Independently re-confirmed the question itself: compares `llama3.2:3b` against the deployed `qwen2.5-coder:7b`; is phrased as a screening question toward a possible *future*, *separately governed* qualification campaign; explicitly disclaims remedy selection; and nowhere assumes or implies `llama3.2:3b` will outperform `qwen2.5-coder:7b`. No qualification or implementation authority is granted anywhere in the amendment.

## 5. Findings E — correction completeness (independent term sweep)

Every line the fresh, independent `grep` sweep (Section 1 above) returned, individually attributed:

| Line | Content | Disposition |
|---|---|---|
| 3 | Existing (already-committed, separately governed) model-identity status-line correction | Pre-existing, not this amendment's concern — correctly not touched |
| 57 | `DQ4`: both models tested once each, both missed `R01-direct` | Historical fact, no role claim — correctly untouched |
| 68 | Attempt 2's own captured `REPLY` from `llama3.2:3b` | Historical fact — correctly untouched |
| 80 | Neither model has completed Unit 3-A qualification | No role claim — correctly untouched |
| 89 | "the current live model" (Section 5 item 2) | **Covered** — amendment's Section 6 clarifying note |
| 112 | Section 7 item 2 | **Covered** — amendment's Section 2 |
| 114 | Section 7 item 4 | **Covered** — amendment's Section 2 |
| 147 | Section 9 research question (generic phrasing) | **Covered** — amendment's Section 3 |
| 194–195 | Section 12 candidate/control identity | **Covered** — amendment's Section 4 |
| 203 | RAM/resource figures naming both models | Resource fact, no role claim — correctly untouched |
| 283 | Section 17 stop condition #2: "treats naming `qwen2.5-coder:7b` as model or remedy selection" | **Not amended — see Section 6 below (non-blocking)** |
| 305–306 | Section 18 rows | **Covered** — amendment's Section 5 |

Thirteen of fourteen matches are either correctly untouched historical/non-role fact, or correctly covered by the amendment. One (line 283) is discussed below.

Independently re-checked a subtler point the amendment's own Section 2 relies on without stating explicitly: Section 7 item 3 ("Naming that subject is not model selection...") uses a pronoun ("that subject") whose antecedent is item 2. Because item 2 is corrected in place (its own referent changes from `qwen2.5-coder:7b` to `llama3.2:3b`), item 3's pronoun correctly resolves to the corrected referent without requiring its own textual edit. This is independently confirmed to work as intended — a genuine, non-trivial correctness property of the amendment's minimal-edit approach, not an oversight.

## 6. Non-blocking observation

Section 17 (Stop conditions), item 2 of the historical Planning Review reads: *"treats naming `qwen2.5-coder:7b` as model or remedy selection."* This guard was written when `qwen2.5-coder:7b` was (incorrectly) the model being "named" as a candidate subject — its purpose was to prevent that naming from being misread as selection. Under the corrected roles, the analogous risk is misreading `llama3.2:3b`'s `SUBJECT_MODEL` naming as selection, not `qwen2.5-coder:7b`'s. The amendment does not add item 2 to its own list of amended propositions.

**Independently confirmed non-blocking, not a defect requiring revision:** the amendment's own Section 10 (Prohibited interpretations) already states, explicitly and directly, that the amendment "must not be read as: selection of `llama3.2:3b` as a remedy; a preference for `llama3.2:3b`..." — providing the equivalent protection for the corrected model at the amendment level, even though the historical stop condition's own wording (a frozen, preserved artifact of the original document) still names the other model. No safety gap results. This is recorded as guidance for whichever future task next amends Section 17 in its own right — not grounds for `REVISE BEFORE ACCEPTANCE`, consistent with this programme's own established treatment of comparably-shaped, non-load-bearing completeness observations.

## 7. Findings D — historical evidence

Independently re-confirmed unchanged: Knowledge Discoverability Attempts 1–2's use of `llama3.2:3b`, their capture-proxy facts (governed by the separate Attempts 1–2 review, not touched by this amendment or its target), Attempt 2's own captured `REPLY` observation, `DQ4`'s single-attempt comparison, and Unit 2/Unit 2-D's `qwen2.5-coder:7b` evidence. The amendment's own Section 4 explicitly does not amend the historical Planning Review's own Section 4 (Evidence admitted), and this review independently confirms Section 4 of the historical document is in fact untouched (Section 1 above, `git diff --stat`, empty). No evidence is renamed, reweighted, reinterpreted, pooled, or promoted anywhere in the amendment.

## 8. Findings F — substantive preservation

Independently spot-checked the historical Planning Review's own Sections 2, 6, 8, 10, 13, 15, 17, and 19 against the amendment's Section 8 claim that each survives unamended: each is confirmed, by direct re-reading, to contain no model-role-dependent content that the correction would have to touch — diagnostic-only status (Section 2 item 6, Section 19), the qualification-first contradiction (Section 6), the resource/RAM boundary (Section 13), the ten-step governance sequence (Section 15), and all seventeen stop conditions except the one non-blocking item in Section 6 above (Section 17) are all confirmed genuinely untouched and unaffected by the role correction.

## 9. Findings G — downstream dependency

Independently re-read, fresh, Sections 7–8 of the Experimental Reclassification and Qualification-Boundary Scope Lock:

```text
Section 7: "The only permitted purpose of a future campaign operating under
this Scope Lock is: > Determine whether a digest-pinned qwen2.5-coder:7b
diagnostic subject demonstrates sufficiently broad, repeated,
representation-valid, fidelity-preserving action selection across the
frozen semantic and safety surface to justify a later proposal for full
Unit 3-A qualification investment."

Section 8: "The only permitted planning identities are: - diagnostic
subject: qwen2.5-coder:7b; - comparison control: llama3.2:3b."
```

This independently confirms the amendment's Section 9 claim: the Reclassification Scope Lock's own Section 7 purpose statement is a specific-model instantiation of the historical Planning Review's own (now-amended) Section 9 generic phrasing, and its Section 8 directly mirrors the historical Planning Review's own (now-amended) Section 12. The dependency is real, not asserted loosely.

**Confirmed: the amendment does not edit or silently correct the Reclassification Scope Lock** — `git diff --stat` for that file is empty. The amendment's own Section 9 explicitly states this correction is reserved to a separate, later task, matching the sequence the controlling Scope Lock's own Section G already fixes.

## 10. Findings H — authority boundaries

Independently re-read the amendment in full for any affirmative grant. None found: no live model call, no Family F execution, no qualification, no remedy selection, no model replacement, no parser change, no persistence change, no QMD change, no UI change, no Docker change, no model-configuration change, and no implementation is authorized anywhere in its text. Its own Section 10 (Prohibited interpretations) and Section 11 (Status block, `IMPLEMENTATION_AUTHORIZED = NO`, `LIVE_MODEL_AUTHORITY = NONE`, `REMEDY_SELECTED = NO`, `CANDIDATE_ADOPTED = NO`) are independently confirmed consistent with the rest of the document.

## 11. Findings I — pending Decision

```text
$ git status --short -- docs/decisions/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_BOUNDING_EVIDENCE_IMPLEMENTATION_AUTHORIZATION_DECISION.md
?? docs/decisions/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_BOUNDING_EVIDENCE_IMPLEMENTATION_AUTHORIZATION_DECISION.md
```

Independently confirmed untracked, unedited, held. Not referenced by, or dependent on, this amendment's own content.

## 12. Findings J — constitutional risks

| Risk | Finding |
|---|---|
| 1. Disguised model selection | Not present — Section 2 and Section 10 explicit disclaimers, consistent throughout. |
| 2. Historical rewriting | Not present — Section 4 (evidence) of the historical document confirmed untouched. |
| 3. Evidence inflation | Not present — no evidentiary-tier claim made anywhere. |
| 4. Role terminology drift | Not present — `CONTROL_MODEL`/`SUBJECT_MODEL` definitions match the controlling Scope Lock exactly. |
| 5. Downstream authority leakage | Not present — Reclassification Scope Lock confirmed unedited; dependency recorded as disclosure only. |
| 6. Research-question scope creep | Not present — question confirmed byte-identical to the frozen text (Section 4 above). |
| 7. Accidental execution authority | Not present — see Section 10 above. |

## 13. Verdict

```text
ACCEPTED, WITH ONE NON-BLOCKING OBSERVATION
```

## 14. What becomes suitable to supersede, and what remains NOT AUTHORIZED

Upon acceptance and commit of this amendment, it becomes suitable to supersede, within the historical Family F Alternative-Model Diagnostic Planning Review: Section 7 items 2 and 4 (subject/control naming); Section 9 (research-question text); Section 12 (candidate/control identity table); Section 18's two named decision-register rows; and Section 5 item 2's "current live model" phrase (by clarifying note, not textual rewrite). Every other provision of that historical document remains exactly as originally written and accepted.

```text
REMEDY_SELECTED = NO
CANDIDATE_MODEL_ADOPTED = NO
LIVE_MODEL_CALL = NO
FAMILY_F_EXECUTION = NO
QUALIFICATION = NO
PRODUCTION_IMPLEMENTATION = NO
PARSER_CHANGE = NO
PERSISTENCE_CHANGE = NO
QMD_CHANGE = NO
UI_CHANGE = NO
DOCKER_CHANGE = NO
MODEL_CONFIGURATION_CHANGE = NO
DOWNSTREAM_INSTRUMENTS_AMENDED (Reclassification Scope Lock, Capture-Proxy
  Bounding Scope Lock, Bounding Evidence Acquisition Plan) = NO -- remain
  their own separate, later tasks
PENDING_DECISION_EDITED = NO -- remains held
```

## 15. Exact next lawful action

```text
NEXT_LAWFUL_ACTION = Commit this amendment (and this ICR) into repository
  history via the established governance-acceptance commit pattern; then
  proceed, as a separate task, to the next instrument in the accepted
  Scope Lock's own Section G sequence -- the Experimental Reclassification
  and Qualification-Boundary Scope Lock's own bounded superseding
  amendment.
```

Not performed by this review.

## 16. Verification performed for this review

```text
$ git status --short --branch
## main...origin/main
?? docs/decisions/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_BOUNDING_EVIDENCE_IMPLEMENTATION_AUTHORIZATION_DECISION.md
?? docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_FAMILY_F_ALTERNATIVE_MODEL_DIAGNOSTIC_PLANNING_REVIEW_MODEL_ROLE_AMENDMENT.md

$ git diff --stat
(no output -- no tracked file modified)
```

(This review's own new file will appear as a third untracked entry once written.) No model process, campaign, experiment, or implementation action occurred before or during this review. No Gradle build was run. No Ollama server was contacted. No Parker server runtime was touched. The amendment was not edited by this review. The historical Planning Review was not edited. No downstream Family F instrument was edited. The pending Implementation Authorization Decision was not edited.

## 17. STOP conditions confirmed

```text
NO production code, test, Docker, persistence, QMD, UI, parser, or model
  configuration file touched.
NO model called.
NO edit to the amendment.
NO edit to the historical Planning Review.
NO edit to any downstream Family F instrument.
NO edit to the pending Implementation Authorization Decision.
NO document staged, committed, or pushed.
```
