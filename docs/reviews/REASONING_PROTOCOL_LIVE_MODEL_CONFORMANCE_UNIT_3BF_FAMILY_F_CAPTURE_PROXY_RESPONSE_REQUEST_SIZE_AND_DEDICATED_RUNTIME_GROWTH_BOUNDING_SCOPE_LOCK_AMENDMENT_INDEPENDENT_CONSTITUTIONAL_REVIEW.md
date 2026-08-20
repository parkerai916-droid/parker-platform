**Status:** Independent Constitutional Review of the Unit 3-BF Family F Capture-Proxy Response/Request-Size and Dedicated-Runtime-Growth Bounding Scope Lock Model Role Amendment — **ACCEPTED, with one non-blocking observation.** Every proposition the amendment claims to correct was independently re-verified against the historical Scope Lock's own committed text, and an independent, exhaustive term-level sweep (58 matching lines across 10 broad search terms) confirms Section 4's `SUBJECT_MODEL`/`CONTROL_MODEL` pair is genuinely the only materially affected proposition in the entire document. Independently confirmed: no model-specific numeric or resource bound exists anywhere in the historical Scope Lock to require correction — every bound status is `UNCOMPUTABLE`/`NOT COMPUTED`, and `NUMERIC_BOUND_SELECTED=NONE`. Campaign arithmetic is independently confirmed role-symmetric and unaffected by the correction. The downstream dependency into the Bounding Evidence Acquisition and Offline Estimator Plan is independently confirmed real, accurately described, and correctly left unedited. No historical evidence was found altered. One non-blocking bibliographic observation is recorded (Section 5 below). Nothing was edited by this review; nothing was staged, committed, or pushed.

# Capture-Proxy Bounding Scope Lock Amendment — Independent Constitutional Review

## 1. Independent evidence reviewed

Read fresh for this task:

- `docs/architecture/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_CAPTURE_PROXY_RESPONSE_REQUEST_SIZE_AND_DEDICATED_RUNTIME_GROWTH_BOUNDING_SCOPE_LOCK.md` — in full (518 lines), end to end.
- `docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_CAPTURE_PROXY_RESPONSE_REQUEST_SIZE_AND_DEDICATED_RUNTIME_GROWTH_BOUNDING_SCOPE_LOCK_INDEPENDENT_CONSTITUTIONAL_REVIEW.md` — its existing, already-accepted (second-round, corrected) ICR.
- `docs/architecture/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_FAMILY_F_MODEL_ROLE_AND_RESEARCH_QUESTION_SCOPE_LOCK.md` and its accepted correction ICR — the controlling authority.
- `docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_FAMILY_F_ALTERNATIVE_MODEL_DIAGNOSTIC_PLANNING_REVIEW_MODEL_ROLE_AMENDMENT.md` and its accepted ICR.
- `docs/architecture/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_EXPERIMENTAL_RECLASSIFICATION_AND_QUALIFICATION_BOUNDARY_SCOPE_LOCK_AMENDMENT.md` and its accepted ICR — the immediately preceding, structurally identical correction one tier upstream.
- `docs/architecture/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_CAPTURE_PROXY_RESPONSE_REQUEST_SIZE_AND_DEDICATED_RUNTIME_GROWTH_BOUNDING_SCOPE_LOCK_AMENDMENT.md` — the target of this review, in full.
- `docs/implementation/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_BOUNDING_EVIDENCE_ACQUISITION_AND_OFFLINE_ESTIMATOR_PLAN.md` — Section 4, re-read fresh, solely to verify the amendment's own stated downstream dependency.
- `docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_BOUNDING_EVIDENCE_ACQUISITION_AND_OFFLINE_ESTIMATOR_PLAN_INDEPENDENT_CONSTITUTIONAL_REVIEW.md` — re-read for its own citation of the historical target's Section 4.

```text
$ grep -n -iE "qwen|llama|subject|control|baseline|candidate|model|current|deployed|comparison" \
    docs/architecture/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_CAPTURE_PROXY_RESPONSE_REQUEST_SIZE_AND_DEDICATED_RUNTIME_GROWTH_BOUNDING_SCOPE_LOCK.md
```
run fresh, independently, for this review — 58 matching lines, each individually attributed in Section 5 below.

## 2. Findings A — amendment mechanism

Independently confirmed: a separate `*_SCOPE_LOCK_AMENDMENT` artifact is the correct, established mechanism (matching the Unit 3-C Timeout and Durability Scope Lock Amendment and the two immediately preceding Family F amendments, both re-read for structural comparison). The historical target's body is confirmed untouched (`git diff --stat` for that file, empty — Section 12 below). The amendment's own Status line and Section 1 state its entire authority derives from the accepted upstream Model Role and Research Question Scope Lock and the accepted Experimental Reclassification Scope Lock Amendment. Its own Status line and Section 9 state explicitly it is not effective merely by being drafted, and name only its own ICR as the next lawful action.

## 3. Findings B — model-role correction

Independently re-verified against the historical Scope Lock's own committed text (not the amendment's restatement): Section 4's "Frozen campaign invariants" table reads, in unamended form, `SUBJECT_MODEL=qwen2.5-coder:7b` / `CONTROL_MODEL=llama3.2:3b` — exactly the reversed assignment the amendment quotes and corrects. The amendment's corrected values —

```text
DEPLOYED_BASELINE = qwen2.5-coder:7b
CONTROL_MODEL     = qwen2.5-coder:7b
SUBJECT_MODEL     = llama3.2:3b
```

— are independently confirmed to match the controlling Model Role and Research Question Scope Lock's own frozen values exactly.

## 4. Findings D — numeric/resource bounds (critical check)

Independently re-derived, not merely accepted from the amendment's own restatement, from the historical Scope Lock's own Section 5 and Section 24 disposition blocks, read fresh:

```text
NUMERIC_BOUND_SELECTED=NONE
MAX_REQUEST_BOUND_STATUS=COMPUTABLE IN PRINCIPLE; NOT COMPUTED OR SELECTED
MAX_RESPONSE_BOUND_STATUS=UNCOMPUTABLE
HEADER_BOUNDS_STATUS=UNRESOLVED
E_STATUS=UNCOMPUTABLE
R_STATUS=UNCOMPUTABLE
```

Independently confirmed: every numeric constant actually present in the document (`FIXTURE_COUNT=23`, `CONTEXT_PROFILE_COUNT=2`, `SCORED_REPETITIONS_PER_CELL_PER_ROLE=4`, `SCORED_CALLS=368`, `WARMUP_CALLS=24`, `TOTAL_CALLS=392`, `FIXTURE_PROFILE_CELLS_PER_ROLE=46`) is a role-symmetric campaign-structure constant (Section 4's own "per role"/"per cell" phrasing) — none is, or purports to be, a value empirically measured against either model's actual request, response, or runtime behavior. No `MAX_REQUEST_BOUND`, `MAX_RESPONSE_BOUND`, header bound, `E`, or `R` value is stated anywhere in the document; Sections 6–15 define only the *protocol* by which such values could later be lawfully proposed. **No model-specific numeric bound exists in this document that could change meaning under the corrected role assignment.** The amendment's Section 4 claim ("the role correction has zero numeric consequence") is independently confirmed true, not merely asserted.

## 5. Findings C — correction completeness (independent term sweep)

Every line from the fresh, independent 10-term `grep` sweep (Section 1 above) was individually reviewed. Beyond the pre-existing status-line correction (line 3) and Section 4's own pair (lines 63–64, covered), every other hit was confirmed to be one of:

- **Role-generic language already following the corrected Section 4 assignment automatically**, requiring no edit of its own: "the exact frozen subject and control artifacts" (141), "subject/control comparison" (217), "control and resource records" / "subject and control sealed reporting" (245, 247), "format each input for both frozen model-name strings" (113), "ADVANCEMENT_GATE=absolute subject-only..." (72).
- **A use of "current"/"candidate"/"model"/"comparison" in a sense with no dependency on which physical model occupies which Family F role** — "current" meaning presently-active status, evidence, provider, or filesystem state (lines 21, 96, 100, 387, 452, 458), never "currently deployed model"; "candidate" meaning a candidate *host* or *filesystem* (91, 260, 296, 335, 377), never a candidate model; generic references to "model contact," "model artifact," "model name," "model-generated," "model acquisition" with no specific identity attached (49, 93, 114, 116, 127, 133, 168, 172, 208, 265, 274, 287, 303, 331, 350, 362, 370, 390, 420, 426, 462, 486, 507, 510).
- One incidental false-positive: `defaultOllamaRequestBody` (114) contains the substring "llama" only as part of "Ollama," unrelated to `llama3.2:3b`.

**No omitted materially affected proposition was found.** Section 4's two-line table is confirmed, independently, to be the only substantive role-dependent content in the historical Scope Lock.

## 6. Non-blocking observation

Section 1, item 6 of the historical Scope Lock cites "`docs/architecture/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_EXPERIMENTAL_RECLASSIFICATION_AND_QUALIFICATION_BOUNDARY_SCOPE_LOCK.md` and its accepted Independent Constitutional Review — the frozen Family F campaign invariants" as controlling authority. That instrument has itself since been amended and accepted (`2b0b54ee8b19a9b457903872b3ad26ff1fa5b589`). The present amendment's own Section 1 does not update this citation to note the amendment. **Independently confirmed non-blocking:** the cited invariants (corpus, profiles, schedule, gate) are unchanged by that upstream amendment — only its own Section 4/8 role labels were corrected, mirroring this amendment's own identical, narrow shape — so the citation is not factually wrong, only not yet cross-referencing the corrected version. Recorded as guidance for whichever future task next amends Section 1 in its own right, consistent with this programme's own established treatment of comparably-shaped completeness observations (e.g. the Planning Review amendment's own Section 17 stop-condition observation).

## 7. Findings E — campaign arithmetic

Independently re-verified: `FIXTURE_COUNT=23`, `CONTEXT_PROFILE_COUNT=2`, `SCORED_REPETITIONS_PER_CELL_PER_ROLE=4` → `23 × 2 × 4 = 184` scored calls per role → `× 2` roles `= 368` scored calls; `FIXTURE_PROFILE_CELLS_PER_ROLE=46` (`23 × 2`); `8` residency blocks (`4` repetitions `× 2` models) `× 3` warm-up calls `= 24`; `368 + 24 = 392`. Every one of these figures is defined "per role" or "per cell," symmetric across whichever model occupies either role. Correcting which physical model is `SUBJECT_MODEL` versus `CONTROL_MODEL` changes none of this arithmetic.

## 8. Findings F — bounding methodology preservation

Independently spot-checked Sections 6 (`MAX_REQUEST_BOUND` protocol), 9 (capture-proxy enforcement contract), 11 (`E` recomputation and `NO_MANUFACTURED_PASS`), 13 (`R` admissible evidence), and 18 (acceptance gates) against the amendment's Section 5 claim that each survives unamended: each is confirmed, by direct re-reading, to contain no model-role-dependent content the correction would have to touch. No resource-study redesign occurred.

## 9. Findings G — historical evidence and provenance

Independently confirmed: no measurement has ever been captured under either model's identity by this Scope Lock (Section 4 above), so there is no historical measurement to relabel, and none is relabeled. The provenance-package requirement (Section 15, "no value may inherit acceptance from another provider version, model, host, filesystem, serializer, or campaign shape") and the controlling-authority citations to Attempts 1–2 and prior host/resource observations (Section 1) are not restated, reinterpreted, or touched anywhere in the amendment.

## 10. Findings H — downstream dependency

Independently re-read, fresh, Section 4 of the Bounding Evidence Acquisition and Offline Estimator Plan:

```text
SUBJECT_MODEL=qwen2.5-coder:7b
CONTROL_MODEL=llama3.2:3b
FIXTURE_COUNT=23
CONTEXT_PROFILE_COUNT=2
SCORED_REPETITIONS_PER_CELL_PER_ROLE=4
SCORED_CALLS=368
WARMUP_CALLS=24
TOTAL_CALLS=392
...
```

and its own accepted ICR, which states: "The Plan's Section 4 frozen-input table... was independently checked against the Bounding Scope Lock's own Section 4 and matches exactly." This independently confirms the amendment's Section 7 dependency claim is accurate — the Plan's own table is drawn directly from this Scope Lock's own (now-amended) Section 4.

**Confirmed: the amendment does not edit the Bounding Evidence Acquisition and Offline Estimator Plan** — `git diff --stat` for that file is empty (Section 12 below). The amendment's own Section 7 explicitly reserves that correction to a separate, later task.

## 11. Findings I — authority boundaries

Independently re-read the amendment in full for any affirmative grant. None found: no live model call, no new measurement, no Family F execution, no qualification, no remedy selection, no model replacement, no implementation, no production change, no parser/reasoning change, no persistence change, no QMD change, no UI change, no Docker change, no model-configuration change is authorized anywhere in its text. Its own Section 8 (Prohibited interpretations) and Section 9 (Status block: `IMPLEMENTATION_AUTHORIZED = NO`, `EVIDENCE_ACQUISITION_AUTHORIZED = NO`, `LIVE_MODEL_AUTHORITY = NONE`, `NEW_MEASUREMENTS_AUTHORIZED = NO`, `NUMERIC_BOUND_CHANGED = NONE`) are independently confirmed consistent with the rest of the document. A fresh `grep -n "AUTHORIZ"` sweep returned no affirmative grant outside `=NO` fields.

## 12. Findings J — pending Decision

```text
$ git status --short -- docs/decisions/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_BOUNDING_EVIDENCE_IMPLEMENTATION_AUTHORIZATION_DECISION.md
?? docs/decisions/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_BOUNDING_EVIDENCE_IMPLEMENTATION_AUTHORIZATION_DECISION.md
```

Independently confirmed untracked, unedited, unstaged, held, and not referenced by or dependent on this amendment's own content.

## 13. Findings K — constitutional risks

| Risk | Finding |
|---|---|
| 1. Model-role drift | Not present — corrected values match the controlling Scope Lock exactly. |
| 2. Disguised model selection | Not present — Section 1 disclaimers, consistent throughout. |
| 3. Numeric-bound laundering | Not present — no numeric bound exists to launder (Section 4 above). |
| 4. Unsupported transfer of model-specific evidence | Not present — no model-specific evidence exists in this document at all. |
| 5. Historical rewriting | Not present — no measurement existed to rewrite. |
| 6. Evidence inflation | Not present — no evidentiary-tier claim made anywhere. |
| 7. Qualification leakage | Not present — Sections 16–18 unamended. |
| 8. Downstream authority leakage | Not present — Bounding Evidence Acquisition Plan confirmed unedited. |
| 9. Accidental implementation authority | Not present — see Section 11 above. |
| 10. Resource-study scope creep | Not present — Section 8 above confirms methodology unchanged. |

## 14. Verification performed for this review

```text
$ git status --short --branch
## main...origin/main
?? docs/architecture/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_CAPTURE_PROXY_RESPONSE_REQUEST_SIZE_AND_DEDICATED_RUNTIME_GROWTH_BOUNDING_SCOPE_LOCK_AMENDMENT.md
?? docs/decisions/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_BOUNDING_EVIDENCE_IMPLEMENTATION_AUTHORIZATION_DECISION.md

$ git diff --stat
(no output -- no tracked file modified)

$ git diff --stat -- docs/architecture/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_CAPTURE_PROXY_RESPONSE_REQUEST_SIZE_AND_DEDICATED_RUNTIME_GROWTH_BOUNDING_SCOPE_LOCK.md docs/implementation/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_BOUNDING_EVIDENCE_ACQUISITION_AND_OFFLINE_ESTIMATOR_PLAN.md
(no output -- both confirmed untouched)
```

(This review's own new file will appear as a third untracked entry once written.) No model process, campaign, experiment, or new measurement occurred before or during this review. No Gradle build was run. No Ollama server was contacted. No Parker server runtime was touched. The amendment was not edited by this review. The historical Scope Lock was not edited. The Bounding Evidence Acquisition and Offline Estimator Plan was not edited. The pending Implementation Authorization Decision was not edited.

## 15. Verdict

```text
ACCEPTED, WITH ONE NON-BLOCKING OBSERVATION
```

Section 4's role assignment is independently confirmed to be the only materially affected proposition in the historical Scope Lock. No model-specific numeric or resource bound exists anywhere in the document that requires correction — every bound remains `UNCOMPUTABLE`/`NOT COMPUTED`, exactly as before this amendment. Campaign arithmetic remains valid because it is role-symmetric throughout. Bounding methodology (Sections 6–21) is unchanged in substance. The downstream Bounding Evidence Acquisition and Offline Estimator Plan's own correction remains a separate, later, separately-governed task. No live execution, evidence acquisition, or implementation authority is created anywhere in this amendment.

## 16. Exact next lawful action

```text
NEXT_LAWFUL_ACTION = Commit this amendment (and this ICR) into repository
  history via the established governance-acceptance commit pattern; then
  proceed, as a separate task, to the final instrument in the accepted
  Model Role and Research Question Scope Lock's own Section G sequence --
  the Bounding Evidence Acquisition and Offline Estimator Plan's own
  bounded superseding amendment.
```

Not performed by this review.

## 17. STOP conditions confirmed

```text
NO production code, test, Docker, persistence, QMD, UI, parser, or model
  configuration file touched.
NO model called.
NO new measurement performed.
NO edit to the amendment.
NO edit to the historical Scope Lock.
NO edit to the Bounding Evidence Acquisition and Offline Estimator Plan.
NO edit to the pending Implementation Authorization Decision.
NO document staged, committed, or pushed.
```
