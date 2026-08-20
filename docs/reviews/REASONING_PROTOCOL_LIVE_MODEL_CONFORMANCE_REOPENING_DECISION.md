**Status:** Reasoning Protocol Live-Model Conformance — Reopening Decision — **PROGRAMME STATUS CHANGED: PAUSED → ACTIVE. GOVERNANCE-ONLY. NO REMEDY SELECTED. NO MODEL SELECTED OR QUALIFIED. NO IMPLEMENTATION, MODEL RUN, OR CAMPAIGN AUTHORIZED.** Against committed baseline `642cba214bcf207e72e855694d9c2dd35f8d31cf` (`HEAD` == `origin/main`, clean). This document performs the status decision the accepted Reopening Assessment and its Independent Constitutional Review identified, but did not themselves perform: whether the programme moves from PAUSED to ACTIVE, and, if so, which single already-classified path is authorized to proceed to its own dedicated Planning Review.

**Model-identity premise correction.** Corrected in place against the accepted Model-Identity Premise Defect Confirmation Review and its Independent Constitutional Review (commit `4d8d5012243df955683fe929a6cf7a0dc6766ffc`): any statement in this document characterizing, or accepting without independent challenge a characterization of, `llama3.2:3b` as Parker's current, live, or production model is superseded — `qwen2.5-coder:7b`, not `llama3.2:3b`, was Parker's committed deployed Docker baseline throughout this programme. Knowledge Discoverability Attempts 1–2's own historical use of `llama3.2:3b` remains accurate and unchanged. CONTROL_MODEL/SUBJECT_MODEL roles and the Family F research question remain unresolved, pending separate governance. The remainder of this document's body is unmodified and remains the historical record of this review.

# Reasoning Protocol Live-Model Conformance — Reopening Decision

## 1. Status and scope

This is a standalone, programme-level governance document — peer to the Post-Selection Disposition Planning Review, not a new "Unit 3-F," not a Scope Lock, not an Implementation Plan. It performs exactly one act: changing the programme's own recorded status. It does not select a remedy, does not select or qualify a model, does not authorize any live model call, campaign, code change, or configuration change, and does not authorize a third Knowledge Discoverability live-verification attempt. Where it names a single path as authorized to proceed, that authorization is bounded to a Planning Review only.

## 2. Baseline

```text
git rev-parse HEAD    = 642cba214bcf207e72e855694d9c2dd35f8d31cf
git rev-parse origin/main = 642cba214bcf207e72e855694d9c2dd35f8d31cf
git status --short --branch = ## governance/reasoning-protocol-reopening-decision (clean)
```

No model process, campaign, experiment, or implementation action occurred before or during this task. No Gradle build was run. No Ollama server was contacted. No Parker server runtime was touched.

## 3. Documents read fresh, in full, for this task

`docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_REOPENING_ASSESSMENT_FOR_KNOWLEDGE_DISCOVERABILITY.md` (commit `2965250b9844e909e4a851fa3e9dd15ea8033c6c`); its Independent Constitutional Review (`VERDICT=ACCEPTED`); `docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_PROGRAMME_DISPOSITION_CLOSURE_REVIEW.md` (baseline `0d9ae8b`, Sections 10, 11, 14, 16, 17 especially); its Independent Constitutional Review (`VERDICT=ACCEPTED`); the primary Family F / model-qualification governance the Assessment itself cites — Unit 3-A Reliability Contract Definition Scope Lock §11 ("A model is not qualified merely because it passed a single, isolated execution... Changing models is not, by itself, a remedy; it is one candidate family among several... subject to the same qualification obligation as the current configuration"); Unit 3-B Remedy Experiment Scoping Scope Lock §3 (Family F classified **excluded on current evidence**, not prohibited, "revisable... at any time new evidence arises... a separate, future governance act"); Unit 3-E Remedy Selection Review §8 (Family F not tested by any Unit 3-C experiment); and Post-Selection Disposition Planning Review §6, Path D ("reconsider excluded families... REQUIRES NEW GOVERNANCE; CURRENTLY NOT JUSTIFIED BY EVIDENCE" for any specific family named at that time).

## 4. The decision

**Steve, the governance authority for this programme, has decided to change the programme's own recorded status from PAUSED to ACTIVE.** This document records that decision; it does not itself manufacture the justification for it independently of the accepted Assessment — the trigger is the one the Assessment already established and its Independent Constitutional Review already accepted: Knowledge Discoverability, a separately-governed downstream programme, is now concretely and demonstrably blocked, by two preserved, evidence-hashed live attempts, exactly on the reasoning-boundary defect this programme exists to address (`PF01`/`R01-direct`).

This is not an automatic consequence of the Assessment. The Assessment and its Independent Constitutional Review both explicitly held that "REMAIN PAUSED, TRIGGER NOTED" was an equally lawful outcome and that "no source requires an ACTIVE outcome merely because the trigger is satisfied" (Independent Constitutional Review §8). This document exists because the governance authority has now made that separate, further, non-automatic choice — the choice the Assessment itself could not and did not make.

```text
REOPENING_TRIGGER=SATISFIED
PROGRAMME_STATUS=ACTIVE
```

## 5. Trigger reaffirmed, not re-litigated

This document does not re-derive the reopening trigger from scratch — it adopts, unmodified, the Assessment's own accepted determination (Assessment §5; Independent Constitutional Review §3): the trigger qualifies under the Programme Disposition Closure Review §16's own catch-all bullet ("another specific, precisely-stated development... a future task proposing reopening on a basis not named here must still state its own specific justification") and is excluded by none of §17's eight prohibited grounds (desire for a winner, passage of time, unmodified rerun, standards-lowering, least-bad selection, untested combinations, absence-of-evidence-as-retry-trigger, or implementation pressure from this programme's own Unit 4 — Knowledge Discoverability is a separate programme with no standing authority over this one, not an instance of the eighth prohibited ground).

This document draws no new evidence and performs no new comparative analysis. Attempts 1–2 remain exactly as provenance-bounded as the Assessment established: operational-impact/trigger evidence only, never pooled into Control/Family A/Family B/Family C's own matched-subset or full-corpus figures, and Attempt 1's own raw tag remains unobserved and is not assigned retroactively.

## 6. Authorized path: Family F, Planning Review only

Of the paths the Assessment classified (its own §7 table), this decision authorizes exactly one to proceed — and only to its own dedicated Planning Review, not beyond:

```text
AUTHORIZED_PATH=FAMILY_F_ALTERNATIVE_MODEL
```

**Why Family F, and only Family F:** Path B (Family A decision-step extension) was independently re-examined by its own dedicated Planning Review and found not proportionate on evidentiary grounds the new trigger does not touch (Assessment §10; Family A Extension Review §8–9) — reopening the programme does not revisit a conclusion that was never about proportionality-under-pause in the first place. Path D (a new or narrower Family C variant) is a genuinely untested candidate that cannot inherit the existing Family C candidate's own evidence and would itself require its own separately-justified Unit 3-C-tier governance chain before any safety claim — no fresh basis for prioritizing it over Family F is identified here or in the Assessment. A selection-bar revision (Path E) carries the sharpest outcome-driven-governance risk of any path, and this document explicitly declines it, for the same reason the Assessment already gave: the presence of new downstream pressure is exactly the condition under which a bar revision would be most suspect (Assessment §12; Post-Selection Disposition Review §9). Family F is authorized to proceed to a Planning Review specifically because it is the one path never yet examined by any dedicated task in this programme's own history — Unit 3-B's own original exclusion rested on a single, doubly-confounded exploratory data point (DQ4), and no Unit 3-C/3-D/3-E experiment ever tested model substitution at all. Whether that gap is now worth closing, and how, is a question this document does not answer — it is the question the authorized Planning Review exists to answer.

**This is not a selection of `qwen2.5-coder:7b`, or of any model, as a remedy.** Nothing in this document states or implies that any alternative model is better, safer, more conformant, or closer to qualified than the current, unmodified `llama3.2:3b` configuration. No comparative claim of any kind is made about any model anywhere in this document.

```text
FAMILY_F_CURRENT_AUTHORITY=
EXCLUDED ON CURRENT EVIDENCE (UNIT 3-B §3) — UNCHANGED BY THIS DECISION. This document does not reverse, suspend, or narrow that exclusion. It authorizes only a dedicated Planning Review to determine, on its own fresh analysis, whether the new downstream dependency (Section 5) is sufficient, specific justification to reconsider the exclusion — the reconsideration itself, and any governance that might follow it, remains entirely that future task's own decision, not pre-decided here.
PROCEED TO A DEDICATED PLANNING REVIEW ONLY
```

## 7. What ACTIVE means, and what it does not mean

**ACTIVE means governance work on this programme may resume** — specifically, a Family F Planning Review may now be authored, whereas under PAUSED it could not have been (Post-Selection Disposition Review §6, Path D: "REQUIRES NEW GOVERNANCE"). ACTIVE does **not** mean:

- that any experiment, campaign, or live model call may begin — the Planning Review named in Section 6 is itself governance-only and, per every planning precedent in this programme's own history (the Unit 3 Planning Review, the Post-Selection Disposition Planning Review, the Family A Extension Planning Review), may reach a conclusion of "not proportionate" or "not justified" exactly as those predecessors sometimes did;
- that Unit 4 or Unit 5 authority exists — both remain strictly downstream of an actual, affirmative Unit 3-E-tier selection, which has not occurred and is not performed by this document;
- that Control, Family A, Family B, or Family C's own existing evidentiary status changes in any way — each remains exactly as tested, not-selected, and not-eliminated as the Assessment and the Programme Disposition Closure Review already established;
- that the current production reasoning protocol changes in any way — it remains unmodified, exactly as it has been throughout this entire programme's history (Closure Review §12).

`REMAIN PAUSED, TRIGGER NOTED` is preserved here only as the disposition this decision supersedes, not as a live alternative this document is still choosing between.

```text
PROGRAMME_STATUS_HISTORY=PAUSED (Programme Disposition Closure Review, `0d9ae8b`) → ACTIVE (this document, effective on commit)
```

## 8. Provenance separation, binding on all future Family F work

Restated directly, per the Assessment's own requirement (Assessment §6) and the Unit 3-B §10 comparison discipline this programme has applied without exception since Unit 3-D: **any future Family F evidence — diagnostic, exploratory, or otherwise — must never be retrospectively pooled into Attempt 6's own Control/Family A/Family B/Family C figures, into the Unit 2/Unit 2-D characterisation campaigns, or into Knowledge Discoverability's own Attempts 1–2.** Each remains its own separately-provenanced evidence source, exactly as Family C's own live/offline split has been kept separate throughout this programme's history. Any future Family F Planning Review, Scope Lock, or evidence-gathering task must state this separation explicitly in its own text, not merely by cross-reference.

## 9. Scope of the authorized Planning Review — not pre-decided here

The Family F Planning Review this document authorizes must determine, on its own fresh analysis and without this document's own conclusion presupposed: whether a diagnostic evaluation of Family F is justified at all, given the new trigger and the existing DQ4 limitation; what governance chain (Unit 3-B-tier reclassification, a fresh Unit 3-C-tier Scope Lock, or some other structure) any authorized evaluation would require; what fixture set, corpus, and evidence-tier requirements would apply; what resource gates (the ≥2.0 GiB RAM safety gate and equivalent operational constraints already established in this programme's own Unit 3-C history) would need to be satisfied; and what approvals (Readiness Review, Explicit Execution Approval, or equivalent) would be needed before any live call could occur. **This document does not pre-decide that a Scope Lock, campaign, or execution approval must follow** — the Planning Review determines the lawful structure, including the possibility that it concludes, as the Family A Extension Review once did, that no further action is currently proportionate.

## 10. Non-claims

```text
REMEDY_SELECTED=NO
MODEL_SELECTED=NO
MODEL_QUALIFIED=NO

IMPLEMENTATION_AUTHORIZED=NO
MODEL_RUN_AUTHORIZED=NO
CAMPAIGN_AUTHORIZED=NO

QWEN_2_5_CODER_7B_RUN_AUTHORIZED=NO
KNOWLEDGE_DISCOVERABILITY_ATTEMPT_3_AUTHORIZED=NO

SELECTION_THRESHOLD_LOWERED=NO
EXISTING_CAMPAIGN_EVIDENCE_MODIFIED_OR_POOLED=NO
ATTEMPTS_1_2_PROMOTED_TO_REMEDY_COMPARISON_EVIDENCE=NO
FAMILY_C_RECHARACTERIZED=NO — remains TESTED, NOT SELECTED, NOT ELIMINATED, exactly as the Assessment established
CONTROL_FAMILY_A_FAMILY_B_EVIDENCE_MODIFIED=NO
```

## 11. Knowledge Discoverability status

```text
KNOWLEDGE_DISCOVERABILITY_CLOSURE=REMAINS BLOCKED
```

Nothing in this document satisfies Knowledge Discoverability's own required live-verification proof (Scope Lock §13, §2). Reopening this programme's own governance status is not itself evidence of anything about the reasoning boundary's behavior, and does not authorize a third Knowledge Discoverability live-verification attempt.

## 12. Verification performed

```text
$ git diff --check
(no output — clean)

$ git status --short --branch
## governance/reasoning-protocol-reopening-decision
?? docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_REOPENING_DECISION.md
(exactly one new, untracked file; no existing file modified)

$ git diff --stat -- src/ tests/ build.gradle.kts settings.gradle.kts
(no output — no production, test, or build file touched)
```

Every commit hash and section citation above was independently re-derived from the primary document's own text read fresh for this task (Section 3); none is copied from any prior summary. `FAMILY_F_CURRENT_AUTHORITY`'s "excluded on current evidence, unchanged" wording is quoted directly from Unit 3-B §3's own text, re-read fresh in this task.

```text
CITATION_AUDIT=PASS — every cited section/quotation independently re-derived from primary source read fresh in this task (Section 3); no citation trusted solely from a prior summary.
DIFF_CHECK=PASS (no output)
FILES_CHANGED=1 (docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_REOPENING_DECISION.md, new, untracked)
GIT_STATUS=clean except the one new untracked file; not staged, committed, or pushed
```

## 13. Disposition

```text
REOPENING_TRIGGER=SATISFIED
PROGRAMME_STATUS=ACTIVE
AUTHORIZED_PATH=FAMILY_F_ALTERNATIVE_MODEL

FAMILY_F_CURRENT_AUTHORITY=EXCLUDED ON CURRENT EVIDENCE (UNCHANGED) —
PROCEED TO A DEDICATED PLANNING REVIEW ONLY

REMEDY_SELECTED=NO
MODEL_SELECTED=NO
MODEL_QUALIFIED=NO

IMPLEMENTATION_AUTHORIZED=NO
MODEL_RUN_AUTHORIZED=NO
CAMPAIGN_AUTHORIZED=NO

QWEN_2_5_CODER_7B_RUN_AUTHORIZED=NO
KNOWLEDGE_DISCOVERABILITY_ATTEMPT_3_AUTHORIZED=NO

NEXT_LAWFUL_ACTION=
FAMILY F ALTERNATIVE-MODEL DIAGNOSTIC PLANNING REVIEW
```

Pending this document's own Independent Constitutional Review, not yet performed and not authorized to be assumed.
