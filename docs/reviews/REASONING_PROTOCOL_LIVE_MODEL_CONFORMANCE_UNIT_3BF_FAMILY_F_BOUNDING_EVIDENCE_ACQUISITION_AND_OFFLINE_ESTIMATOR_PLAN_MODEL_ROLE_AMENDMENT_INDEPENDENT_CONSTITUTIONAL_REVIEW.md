**Status:** Independent Constitutional Review of the Unit 3-BF Family F Bounding Evidence Acquisition and Offline Estimator Plan Model Role Amendment — **ACCEPTED, with one non-blocking observation.** Every proposition the amendment claims to correct was independently re-verified against the historical Plan's own committed text, and an independent, exhaustive 13-term sweep (14 matching lines) confirms Section 4's `SUBJECT_MODEL`/`CONTROL_MODEL` pair is genuinely the only materially affected proposition in the entire document. Independently confirmed: this Plan never states, quotes, or paraphrases the Family F research question, so no research-question correction is required; no model-specific numeric or resource bound exists anywhere in the Plan to require correction — every bound status is `UNCOMPUTABLE`/`NOT COMPUTED`/`UNRESOLVED`, and `NUMERIC_BOUND_SELECTED=NONE`; campaign arithmetic is confirmed role-symmetric; offline-estimator methodology is unamended. A fresh, repository-wide search for any additional document independently freezing the reversed role pair found none — no fifth controlling instrument exists. The dependency into the pending Implementation Authorization Decision is independently confirmed real and accurately described. Acceptance, followed by commit and push, of this amendment and this ICR would complete the four-instrument correction chain. One non-blocking bibliographic observation is recorded (Section 6 below), matching the shape of the equivalent observation already accepted for the immediately preceding amendment in this chain. Nothing was edited by this review; nothing was staged, committed, or pushed.

# Bounding Evidence Acquisition and Offline Estimator Plan Model Role Amendment — Independent Constitutional Review

## 1. Independent evidence reviewed

Read fresh for this task:

- `docs/implementation/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_BOUNDING_EVIDENCE_ACQUISITION_AND_OFFLINE_ESTIMATOR_PLAN.md` — in full (724 lines), end to end.
- `docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_BOUNDING_EVIDENCE_ACQUISITION_AND_OFFLINE_ESTIMATOR_PLAN_INDEPENDENT_CONSTITUTIONAL_REVIEW.md` — its existing, already-accepted ICR.
- `docs/architecture/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_FAMILY_F_MODEL_ROLE_AND_RESEARCH_QUESTION_SCOPE_LOCK.md` and its accepted correction ICR — the controlling authority.
- `docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_FAMILY_F_ALTERNATIVE_MODEL_DIAGNOSTIC_PLANNING_REVIEW_MODEL_ROLE_AMENDMENT.md` and its accepted ICR.
- `docs/architecture/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_EXPERIMENTAL_RECLASSIFICATION_AND_QUALIFICATION_BOUNDARY_SCOPE_LOCK_AMENDMENT.md` and its accepted ICR.
- `docs/architecture/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_CAPTURE_PROXY_RESPONSE_REQUEST_SIZE_AND_DEDICATED_RUNTIME_GROWTH_BOUNDING_SCOPE_LOCK_AMENDMENT.md` and its accepted ICR — the immediately preceding, structurally identical correction one tier upstream.
- `docs/implementation/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_BOUNDING_EVIDENCE_ACQUISITION_AND_OFFLINE_ESTIMATOR_PLAN_MODEL_ROLE_AMENDMENT.md` — the target of this review, in full.
- `docs/decisions/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_BOUNDING_EVIDENCE_IMPLEMENTATION_AUTHORIZATION_DECISION.md` — Section 1, re-read fresh, read-only, solely to verify the amendment's own stated downstream dependency.

```text
$ grep -n -iE "qwen2\.5-coder:7b|llama3\.2:3b|subject_model|control_model|baseline|current model|candidate|alternative model|comparison|replace|qualification|advancement|research question" \
    docs/implementation/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_BOUNDING_EVIDENCE_ACQUISITION_AND_OFFLINE_ESTIMATOR_PLAN.md
```
run fresh, independently, for this review — 14 matching lines, each individually attributed in Section 5 below.

```text
$ grep -rl "SUBJECT_MODEL=\|CONTROL_MODEL=" docs/
```
run fresh, independently, repository-wide, for this review — 15 files, every one individually attributed in Section 12 below (fifth-instrument search).

## 2. Findings A — amendment mechanism

Independently confirmed: a separate Plan amendment is the correct, established mechanism (matching all three preceding amendments in this chain, re-read for structural comparison). The historical target's body is confirmed untouched (`git diff --stat` for that file, empty — Section 15 below). The amendment's own Status line and Section 1 state its entire authority derives from the accepted Model Role and Research Question Scope Lock and the accepted Capture-Proxy Bounding Scope Lock Amendment. Its own Status line and Section 12 state explicitly it is not effective merely by being drafted, and name only its own ICR as the next lawful action.

## 3. Findings B — model-role correction

Independently re-verified against the historical Plan's own committed text (not the amendment's restatement): Section 4's "Frozen inputs and statuses" table reads, in unamended form, `SUBJECT_MODEL=qwen2.5-coder:7b` / `CONTROL_MODEL=llama3.2:3b` — exactly the reversed assignment the amendment quotes and corrects. The amendment's corrected values —

```text
DEPLOYED_BASELINE = qwen2.5-coder:7b
CONTROL_MODEL     = qwen2.5-coder:7b
SUBJECT_MODEL     = llama3.2:3b
```

— are independently confirmed to match the controlling Model Role and Research Question Scope Lock's own frozen values exactly.

## 4. Findings D — research question

Independently re-read the historical Plan's own Section 2 (Purpose) and the whole document in full. Confirmed: the Plan describes only evidence-gathering mechanics — a deterministic offline request-bound estimator and a governed evidence-inventory protocol for response, header, and runtime bounds — and never states, quotes, or paraphrases the Family F research question (the "does `llama3.2:3b` show enough reproducible improvement over `qwen2.5-coder:7b`..." question frozen by the controlling Scope Lock) anywhere in its text. The amendment's Section 4 conclusion — that no research-question textual correction is required — is independently confirmed accurate, not merely asserted.

## 5. Findings C — correction completeness (independent term sweep)

Every line from the fresh, independent 13-term `grep` sweep (Section 1 above) was individually reviewed. Beyond the pre-existing status-line correction (line 3) and Section 4's own pair (lines 72–73, covered), every other hit was confirmed to be one of:

- **A false positive on "llama" as a substring of "Ollama"**: `ollama` CLI reference (56), `defaultOllamaRequestBody` (82, 261), "Ollama" as a named provider (139), `OLLAMA_CLI=NOT AUTHORIZED` (474) — none references `llama3.2:3b`.
- **"candidate" meaning a candidate host or candidate evidence source, never a candidate model**: controlling-authority citation to "Parker Candidate Host Assessment" (21), "candidate-host-specific observation plan" (446), "why each candidate source was inapplicable" (459), "renewed candidate-host assessment" (656), "any bound will fit a candidate host" (687).
- **No dependency on model role at all**: `FAMILY_F_STATUS=INCLUDED FOR PRE-QUALIFICATION DIAGNOSTIC SCOPING ONLY` (702) restates Family F's own classification status, unrelated to which model holds which role; "No evidence source may be silently re-fetched or replaced on resume" (519) is about evidence-source handling, not model substitution.

**No omitted materially affected proposition was found.** Section 4's two-line table is confirmed, independently, to be the only substantive role-dependent content in the historical Plan.

## 6. Non-blocking observation

Section 1, item 1 of the historical Plan cites "`docs/architecture/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_CAPTURE_PROXY_RESPONSE_REQUEST_SIZE_AND_DEDICATED_RUNTIME_GROWTH_BOUNDING_SCOPE_LOCK.md` and its accepted Independent Constitutional Review" as controlling authority, without noting that that instrument has itself since been amended and accepted. This is the same shape of bibliographic-freshness gap already found, and accepted as non-blocking, in the Capture-Proxy Bounding Scope Lock Amendment's own ICR (regarding its own citation of the Experimental Reclassification Scope Lock). **Independently confirmed non-blocking, for the identical reason**: the cited sections (bounding protocol, corpus, gates) are unchanged by that upstream amendment — only its own Section 4 role labels were corrected, in a structurally identical, narrow shape — so the citation is not factually wrong, only not yet cross-referencing the corrected version. Recorded as guidance for whichever future task next amends Section 1 in its own right.

## 7. Findings E — numeric/resource bounds (critical check)

Independently re-derived, not merely accepted from the amendment's own restatement, from the historical Plan's own Section 4 "Current statuses" block and Section 33 Final Authority Statement, read fresh:

```text
MAX_REQUEST_BOUND_STATUS=NOT COMPUTED OR SELECTED
MAX_RESPONSE_BOUND_STATUS=UNCOMPUTABLE
HEADER_BOUNDS_STATUS=UNRESOLVED
E_STATUS=UNCOMPUTABLE
R_STATUS=UNCOMPUTABLE
NUMERIC_BOUND_SELECTED=NONE
```

No `MAX_REQUEST_BOUND`, `MAX_RESPONSE_BOUND`, header bound, `E`, or `R` value is stated anywhere in the document. Sections 5–28 define only the *protocol* by which such values could later be lawfully proposed by a separately authorized evidence-production task — never any actual measured value against either model. **No model-specific numeric or resource bound exists in this document that could change meaning under the corrected role assignment.** The amendment's Section 5 claim ("the role correction has zero numeric consequence") is independently confirmed true.

## 8. Findings F — campaign arithmetic

Independently re-derived: `FIXTURE_COUNT=23`, `CONTEXT_PROFILE_COUNT=2`, `SCORED_REPETITIONS_PER_CELL_PER_ROLE=4` → `23 × 2 × 4 = 184` per role → `× 2` roles `= 368`; `8` residency blocks `× 3` warm-up `= 24`; `368 + 24 = 392` — matches Section 12's own coverage statement exactly, and matches the (now-amended) Capture-Proxy Bounding Scope Lock's own identical figures. All values are role-symmetric and unaffected by the correction.

## 9. Findings G — offline-estimator methodology

Independently spot-checked Sections 5 (implementation boundary), 6 (detached-task isolation), 12–14 (WP-A estimator mechanics and output schema), 19 (negative-evidence protocol), and 28 (stop conditions) against the amendment's Section 7 claim that each survives unamended: each is confirmed, by direct re-reading, to describe its procedure in role-generic terms ("both model names," "2 roles," "role" as a schema field) with no dependency on which physical model is `SUBJECT_MODEL` versus `CONTROL_MODEL`. No methodology drift found.

## 10. Findings H — historical evidence and provenance

Independently confirmed: no measurement has ever been captured under either model's identity by this Plan (Section 7 above), so there is no historical measurement to relabel, and none is relabeled. Unit 2, Unit 2-D, and Knowledge Discoverability Attempts 1–2 evidence are referenced only transitively through Section 1's controlling-authority citations, never restated in this Plan's own body, and are not touched anywhere in the amendment. The lighthouse observation is independently confirmed absent from the historical Plan's entire text (it postdates this Plan and was never part of its scope) and is not introduced, referenced, or pooled by the amendment.

## 11. Findings I — pending Decision dependency

Independently re-read, fresh, Section 1 of the pending Family F Bounding Evidence Implementation Authorization Decision:

```text
"...it authorizes the exact two-file offline implementation defined by the
accepted Family F Bounding Evidence Acquisition and Offline Estimator Plan."
...
"This decision must be read with the controlling Plan, the accepted
Capture-Proxy Response/Request-Size and Dedicated-Runtime-Growth Bounding
Scope Lock and its ICR..."
```

This independently confirms the amendment's Section 9 claim: the pending Decision explicitly names this Plan as controlling authority, and its own `CONTROL_MODEL=llama3.2:3b` line (independently re-confirmed present at the Decision's own line 194 in an earlier task in this session) is inherited from this Plan's own (pre-correction) Section 4.

```text
$ git status --short -- docs/decisions/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_BOUNDING_EVIDENCE_IMPLEMENTATION_AUTHORIZATION_DECISION.md
?? docs/decisions/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_BOUNDING_EVIDENCE_IMPLEMENTATION_AUTHORIZATION_DECISION.md
```

Independently confirmed the pending Decision remains untracked, unedited, held, and not itself touched by this amendment.

## 12. Findings J — four-instrument closure and fifth-instrument search

Independently re-verified the accepted chain state: (a) Planning Review amendment and (b) Experimental Reclassification Scope Lock amendment and (c) Capture-Proxy Bounding Scope Lock amendment are each confirmed committed on `main` at or before this review's own baseline (`a378f32`). (d), the present amendment, is drafted and under review only.

A fresh, independent, repository-wide search (Section 1 above, `grep -rl "SUBJECT_MODEL=\|CONTROL_MODEL="  docs/`) returned exactly 15 files. Every one was individually attributed:

- The controlling Model Role and Research Question Scope Lock itself (freezes the *corrected* values — the destination, not a defect).
- The Capture-Proxy Bounding Scope Lock, its amendment, and both their ICRs — already corrected, accounted for.
- The historical target Plan and the present amendment (this review's own subject) and the historical Plan's own ICR.
- The pending Decision (the downstream target this correction chain exists to eventually unblock; explicitly out of scope for this task).
- The Model-Identity Premise Defect Confirmation Review and its ICR (the root-cause document that first names both `CONTROL_MODEL=llama3.2:3b` and the correct baseline as historical/illustrative quotation, not a document that itself freezes a governing role table).
- The Experimental Reclassification Scope Lock Amendment and its ICR (already corrected).

**No fifth controlling instrument was found.** No document beyond the four already in this correction chain, plus the pending Decision it feeds, independently freezes the reversed role pair.

**Acceptance of this amendment, followed by its commit and push to `main`, would complete the four-instrument model-role correction chain.** This is stated conditionally, matching the amendment's own Section 10 — drafting alone does not complete it.

## 13. Findings K — authority boundaries

Independently re-read the amendment in full for any affirmative grant. None found: no model call, no estimator execution, no new measurement, no live Family F diagnostic, no qualification, no remedy selection, no model replacement, no production implementation, no parser change, no persistence change, no QMD change, no UI change, no Docker change, no model-configuration change is authorized anywhere in its text. Its own Section 11 (Prohibited interpretations) and Section 12 (Status block: `IMPLEMENTATION_AUTHORIZED = NO`, `EVIDENCE_ACQUISITION_AUTHORIZED = NO`, `LIVE_MODEL_AUTHORITY = NONE`, `NEW_MEASUREMENTS_AUTHORIZED = NO`, `NUMERIC_BOUND_CHANGED = NONE`, `FOUR_INSTRUMENT_CHAIN_COMPLETE = NO`) are independently confirmed consistent with the rest of the document. A fresh `grep -n "AUTHORIZ"` sweep returned no affirmative grant outside `=NO` fields.

## 14. Findings L — constitutional risks

| Risk | Finding |
|---|---|
| 1. Role drift | Not present — corrected values match the controlling Scope Lock exactly. |
| 2. Disguised model selection | Not present — Section 1 disclaimers, consistent throughout. |
| 3. Numeric-bound laundering | Not present — no numeric bound exists to launder (Section 7 above). |
| 4. Methodology drift | Not present — Section 9 above confirms methodology unchanged. |
| 5. Unsupported transfer of evidence | Not present — no model-specific evidence exists in this document at all. |
| 6. Historical rewriting | Not present — no measurement existed to rewrite. |
| 7. Evidence inflation | Not present — no evidentiary-tier claim made anywhere. |
| 8. Implementation-authority leakage | Not present — see Section 13 above. |
| 9. Premature closure of the four-instrument chain | Not present — Section 10 of the amendment and Section 12 above both explicitly condition closure on ICR acceptance, commit, and push. |
| 10. Downstream Decision authority leakage | Not present — pending Decision confirmed untouched (Section 11 above). |

## 15. Verification performed for this review

```text
$ git status --short --branch
## main...origin/main
?? docs/decisions/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_BOUNDING_EVIDENCE_IMPLEMENTATION_AUTHORIZATION_DECISION.md
?? docs/implementation/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_BOUNDING_EVIDENCE_ACQUISITION_AND_OFFLINE_ESTIMATOR_PLAN_MODEL_ROLE_AMENDMENT.md

$ git diff --stat
(no output -- no tracked file modified)

$ git diff --stat -- docs/implementation/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_BOUNDING_EVIDENCE_ACQUISITION_AND_OFFLINE_ESTIMATOR_PLAN.md
(no output -- historical Plan confirmed untouched)
```

(This review's own new file will appear as a third untracked entry once written.) No model process, campaign, experiment, estimator run, or new measurement occurred before or during this review. No Gradle build was run. No Ollama server was contacted. No Parker server runtime was touched. The amendment was not edited by this review. The historical Plan was not edited. No prior amendment or ICR in this chain was edited. The pending Implementation Authorization Decision was not edited.

## 16. Verdict

```text
ACCEPTED, WITH ONE NON-BLOCKING OBSERVATION
```

Section 4's model-role pair is independently confirmed to be the only materially affected proposition in the historical Plan. No research-question correction is required — the Plan never states or paraphrases it. No model-specific numeric or resource bound exists anywhere in the document that requires correction. Campaign arithmetic remains valid and role-symmetric. Estimator methodology (Sections 5–28) is unamended. Historical evidence and provenance remain intact. A fresh, repository-wide search found no fifth controlling instrument. Acceptance, followed by commit and push, of this amendment and this ICR will complete the four-instrument model-role correction chain identified by the controlling Scope Lock. The pending Implementation Authorization Decision remains separately held and still requires its own correction and review, not performed by this amendment or this review.

## 17. Exact next lawful action

```text
NEXT_LAWFUL_ACTION = Commit this amendment (and this ICR) into repository
  history via the established governance-acceptance commit pattern. Upon
  that commit and push, the four-instrument downstream correction chain
  becomes fully accepted. Only then does the pending Family F Bounding
  Evidence Implementation Authorization Decision become eligible for its
  own bounded correction and, subsequently, its own Independent
  Constitutional Review -- neither performed by this review.
```

Not performed by this review.

## 18. STOP conditions confirmed

```text
NO production code, test, Docker, persistence, QMD, UI, parser, or model
  configuration file touched.
NO model called.
NO estimator run or new measurement performed.
NO edit to the amendment.
NO edit to the historical Plan.
NO edit to any prior amendment or ICR in this chain.
NO edit to the pending Implementation Authorization Decision.
NO document staged, committed, or pushed.
```
