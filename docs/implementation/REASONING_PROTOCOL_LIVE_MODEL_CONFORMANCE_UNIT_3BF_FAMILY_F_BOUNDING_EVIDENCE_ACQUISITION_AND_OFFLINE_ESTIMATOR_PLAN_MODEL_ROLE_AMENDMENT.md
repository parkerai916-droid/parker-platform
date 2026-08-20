**Status:** Unit 3-BF Family F Bounding Evidence Acquisition and Offline Estimator Plan — Model Role Amendment — **PROPOSED — PENDING INDEPENDENT CONSTITUTIONAL REVIEW.** This document amends one specific, named section of the frozen Unit 3-BF Family F Bounding Evidence Acquisition and Offline Estimator Plan (`docs/implementation/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_BOUNDING_EVIDENCE_ACQUISITION_AND_OFFLINE_ESTIMATOR_PLAN.md`) in place of editing that document directly, preserving its original text as the historical record of what was originally frozen and why — following the identical precedent already established by the Unit 3-C Timeout and Durability Scope Lock Amendment, the Family F Alternative-Model Diagnostic Planning Review Model Role Amendment, the Experimental Reclassification and Qualification-Boundary Scope Lock Amendment, and the Capture-Proxy Bounding Scope Lock Amendment. This is instrument (d), the fourth and final downstream correction identified by the accepted Family F Model Role and Research Question Scope Lock. It derives its entire authority from that Scope Lock and from the accepted Capture-Proxy Bounding Scope Lock Amendment, both present in the baseline this amendment is drafted against (`a378f320d4eb9134764d85d230996ad8f5d9de3f`). It does not become effective merely by being drafted, selects no remedy, adopts no candidate, creates no new empirical resource evidence, changes no resource bound, runs no estimator, authorizes no live model call, no live diagnostic, no evidence acquisition, and no implementation. It requires its own accepted Independent Constitutional Review. Until that review is accepted, the historical target Plan — read together with its already-committed model-identity-premise status-line correction — remains the accepted record.

# Unit 3-BF Family F Bounding Evidence Acquisition and Offline Estimator Plan — Model Role Amendment

## 1. Baseline and authority

Drafted against committed baseline `a378f320d4eb9134764d85d230996ad8f5d9de3f` (`docs(governance): accept Family F capture-proxy model roles`). Amends the frozen Unit 3-BF Family F Bounding Evidence Acquisition and Offline Estimator Plan, **Section 4 only** — every other section, including Sections 1–3 and 5–33, remains in full, unamended force.

Controlling authority:

```text
DEPLOYED_BASELINE = qwen2.5-coder:7b
CONTROL_MODEL     = qwen2.5-coder:7b
SUBJECT_MODEL     = llama3.2:3b
```

frozen by the accepted Family F Model Role and Research Question Scope Lock, and independently reaffirmed by the accepted Capture-Proxy Bounding Scope Lock Amendment, whose own Section 2 correction this document's Section 2 directly re-freezes at this instrument's own level — the fourth and final link in the amendment chain the controlling Scope Lock's own Section G fixes. These roles are already governed and are not reconsidered here.

`CONTROL_MODEL` means Parker's actual deployed reasoning-protocol baseline and the reference point for the Family F diagnostic comparison. `SUBJECT_MODEL` means the already-identified Family F diagnostic candidate being screened against that baseline. `SUBJECT_MODEL` does not mean selected remedy, preferred model, qualified model, approved replacement, or production-adoption candidate.

This amendment does not itself implement anything, run the estimator, or acquire evidence. Section 30's fifteen-step governance sequence and Section 24's offline-verification requirements are both unamended and remain fully controlling.

## 2. Amendment to Section 4 (Frozen inputs and statuses) — role assignment only

**Original text (preserved, not deleted):**

> ```text
> SUBJECT_MODEL=qwen2.5-coder:7b
> CONTROL_MODEL=llama3.2:3b
> FIXTURE_COUNT=23
> CONTEXT_PROFILE_COUNT=2
> SCORED_REPETITIONS_PER_CELL_PER_ROLE=4
> SCORED_CALLS=368
> WARMUP_CALLS=24
> TOTAL_CALLS=392
> FROZEN_SCHEDULE=the accepted deterministic Family F schedule, including AB/BA alternation
> PRODUCTION_FORMATTER=DefaultReasoningPromptBuilder
> PRODUCTION_REQUEST_SERIALIZER=defaultOllamaRequestBody
> RANKING=PROHIBITED
> MODEL_SUBSTITUTION=PROHIBITED
> QUANTIZATION_CHANGE=PROHIBITED
> ```

**Frozen, amended — exactly two lines corrected, nothing else in this table changed:**

```text
SUBJECT_MODEL=llama3.2:3b
CONTROL_MODEL=qwen2.5-coder:7b
FIXTURE_COUNT=23
CONTEXT_PROFILE_COUNT=2
SCORED_REPETITIONS_PER_CELL_PER_ROLE=4
SCORED_CALLS=368
WARMUP_CALLS=24
TOTAL_CALLS=392
FROZEN_SCHEDULE=the accepted deterministic Family F schedule, including AB/BA alternation
PRODUCTION_FORMATTER=DefaultReasoningPromptBuilder
PRODUCTION_REQUEST_SERIALIZER=defaultOllamaRequestBody
RANKING=PROHIBITED
MODEL_SUBSTITUTION=PROHIBITED
QUANTIZATION_CHANGE=PROHIBITED
```

Every other line of this table is unamended: `FIXTURE_COUNT`, `CONTEXT_PROFILE_COUNT`, `SCORED_REPETITIONS_PER_CELL_PER_ROLE`, `SCORED_CALLS`, `WARMUP_CALLS`, and `TOTAL_CALLS` are role-symmetric campaign-structure constants (identical count applies regardless of which model occupies either role) inherited unchanged from the Capture-Proxy Bounding Scope Lock's own (now-amended) Section 4. `FROZEN_SCHEDULE`, `PRODUCTION_FORMATTER`, and `PRODUCTION_REQUEST_SERIALIZER` name fixed implementation identities with no model-role dependency of their own. `RANKING`, `MODEL_SUBSTITUTION`, and `QUANTIZATION_CHANGE` are symmetric prohibitions, unaffected by which model holds which role. The "Current statuses" block immediately following Section 4's frozen-inputs table (`MAX_REQUEST_BOUND_STATUS`, `MAX_RESPONSE_BOUND_STATUS`, `HEADER_BOUNDS_STATUS`, `E_STATUS`, `R_STATUS`, `READINESS`) is likewise unamended — none of these status values names or depends on either model's identity.

## 3. Independent confirmation: no other section requires amendment

An exhaustive search of the entire historical Plan for every occurrence of `qwen`/`llama` (case-insensitive) found exactly one substantive hit beyond the already-existing status-line correction: Section 4's two-line table, corrected above. A second, broader sweep for `SUBJECT_MODEL`, `CONTROL_MODEL`, `deployed baseline`, `current model`, `candidate`, `alternative model`, `comparison`, `replace`/`replacement`, `qualification`, `advancement`, and `research question` found no further materially affected proposition:

- Sections 12–19 (WP-A through WP-D, the offline estimator and evidence-inventory work packages) describe their procedures exclusively in role-generic terms — "both model names," "2 roles," "role" as a schema field — never naming a specific model in a directional way.
- Every occurrence of "candidate" in the document (Sections 1, 16, 19, 30, 31) refers to a **candidate host** or **candidate source**, never a candidate model.
- Several incidental matches on "llama" are false positives: `defaultOllamaRequestBody` (Sections 4, 12) and bare "Ollama" (Sections 6, 20) name the Ollama provider/client, not the `llama3.2:3b` model.
- No occurrence of "research question," "diagnostic purpose," or any restatement of *why* Family F exists appears anywhere in this Plan — see Section 4 below.

## 4. Research-question check — no correction required

Independently determined: **this Plan does not state, quote, incorporate, or operationalize the Family F research question anywhere in its text.** Its own Section 2 (Purpose) describes only *what evidence-gathering mechanics* the Plan defines — a deterministic offline request-bound estimator and a governed evidence-inventory protocol for response, header, and runtime bounds — never *why* Family F is investigating llama3.2:3b against qwen2.5-coder:7b, and never restates or paraphrases the research question the controlling Scope Lock freezes. No proposition in this Plan requires the research-question text itself to be superseded. This finding is stated explicitly, per this task's own requirement, rather than left implicit.

## 5. Numeric/resource-bound finding — independently re-derived, not inherited

Per this task's own governing instruction, this amendment independently re-derives, from this Plan's own text alone, whether correcting `SUBJECT_MODEL`/`CONTROL_MODEL` requires changing any model-specific numeric or resource value — **it does not assume the Capture-Proxy Bounding Scope Lock Amendment's identical finding carries over.**

Independently confirmed from the historical Plan's own Section 4 "Current statuses" block and Section 33 Final Authority Statement, both read fresh: `MAX_REQUEST_BOUND_STATUS=NOT COMPUTED OR SELECTED`; `MAX_RESPONSE_BOUND_STATUS=UNCOMPUTABLE`; `HEADER_BOUNDS_STATUS=UNRESOLVED`; `E_STATUS=UNCOMPUTABLE`; `R_STATUS=UNCOMPUTABLE`; `NUMERIC_BOUND_SELECTED=NONE`. No numeric request, response, header, evidence-budget, or runtime-growth value has ever been computed, proposed, or selected under this Plan — the entire document defines only *how* a future, separately authorized evidence-production task would produce such values (Sections 5–28), never any actual measured value. Section 12's own coverage requirement ("23 fixtures × 2 profiles × 4 repetitions × 2 roles = 368 scored records") is independently confirmed to be schedule arithmetic, stated in role-generic terms, not a measured byte count against either model. There is therefore nothing in this document that was computed against the wrong model and now requires re-derivation: **the role correction has zero numeric consequence.** No new measurement, estimator run, or model contact is required, authorized, or performed by this amendment.

## 6. Campaign-arithmetic finding

Independently re-verified: `FIXTURE_COUNT=23`, `CONTEXT_PROFILE_COUNT=2`, `SCORED_REPETITIONS_PER_CELL_PER_ROLE=4` → `23 × 2 × 4 = 184` scored calls per role → `× 2` roles `= 368`; `8` residency blocks `× 3` warm-up calls `= 24`; `368 + 24 = 392` — matches Section 12's own coverage statement exactly, and matches the (now-amended) Capture-Proxy Bounding Scope Lock's own identical figures. Every one of these values is role-symmetric and unaffected by which physical model is `SUBJECT_MODEL` or `CONTROL_MODEL`.

## 7. Offline-estimator methodology — independently confirmed unchanged in substance

The following, all independently re-checked against the target Plan's own text, are not touched by this amendment and remain in full force: the exact future implementation boundary (`build.gradle.kts` plus one new offline test file, Section 5); the detached-task and network/model isolation contract, including the double-gated entry test (Section 6); the five-work-package model, WP-A through WP-E (Section 7); the evidence-root and atomic-completion protocol (Section 8); the mandatory artifact set (Section 9); evidence identity and repository-input preservation (Sections 10–11); WP-A's exact schedule-derived request estimator and its output schema (Sections 12–14); WP-B's response primary-evidence inventory and derivation worksheet (Sections 15–16); WP-C's header primary-evidence inventory (Section 17); WP-D's runtime provider-documentation inventory (Section 18); the negative-evidence protocol (Section 19); the source-collection boundary (Section 20); WP-E's integrity and report generation (Section 21); exact-once and recovery behavior (Section 22); failure semantics (Section 23); offline-verification requirements (Section 24); the evidence-completion-review requirements (Section 25); bound-selection separation (Section 26); the reaffirmed `NO_MANUFACTURED_PASS` rule (Section 27); all stop conditions (Section 28); the decision register (Section 29); the fifteen-step governance sequence (Section 30); the explicit non-claims list (Section 31); and the final authority statement's disposition block (Section 33) — `NUMERIC_BOUND_SELECTED=NONE`, `PROVIDER_OR_MODEL_CONTACT_AUTHORIZED=NO`, `DEDICATED_PROVIDER_LAUNCH_AUTHORIZED=NO`, `R_OBSERVATION_AUTHORIZED=NO`, `HOST_OR_VM_PROVISIONING_AUTHORIZED=NO`, `MODEL_ACQUISITION_AUTHORIZED=NO`, `CAMPAIGN_AUTHORIZED=NO`, `EXPLICIT_EXECUTION_APPROVAL_AUTHORIZED=NO`, `KNOWLEDGE_DISCOVERABILITY_ATTEMPT_3_AUTHORIZED=NO` — all unchanged. No resource-study redesign has occurred; this amendment is a model-role correction only.

## 8. Historical evidence and provenance boundary — unaltered

This amendment changes no historical fact and creates no new one. No measurement has ever been captured under either model's identity by this Plan (Section 5 above), so there is no historical measurement to relabel, and none is relabeled. Unit 2, Unit 2-D, and Knowledge Discoverability Attempts 1–2 evidence — referenced only transitively, through Section 1's controlling-authority citations, never restated in this Plan's own body — are not touched anywhere in this amendment. The lighthouse observation is not mentioned anywhere in the historical Plan and is not introduced, referenced, or pooled by this amendment; it remains, as established by prior governance, a separately-provenanced operational observation outside every existing corpus, and it authorizes nothing here.

## 9. Dependency into the pending Family F Bounding Evidence Implementation Authorization Decision — recorded, not acted upon

Independently inspected, read-only: the pending, untracked Family F Bounding Evidence Implementation Authorization Decision (`docs/decisions/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_BOUNDING_EVIDENCE_IMPLEMENTATION_AUTHORIZATION_DECISION.md`) states, in its own Section 1: *"it authorizes the exact two-file offline implementation defined by the accepted Family F Bounding Evidence Acquisition and Offline Estimator Plan"* and *"this decision must be read with the controlling Plan, the accepted Capture-Proxy Response/Request-Size and Dedicated-Runtime-Growth Bounding Scope Lock and its ICR."* Its own frozen input table carries `CONTROL_MODEL=llama3.2:3b` — the same backward assignment this amendment corrects at the Plan level, inherited directly from this Plan's own (pre-correction) Section 4.

**The pending Decision remains HELD and untouched by this amendment.** It may not proceed to its own Independent Constitutional Review until:

```text
1. this amendment receives its own accepted Independent Constitutional Review;
2. this amendment and that ICR are committed and pushed to main; and
3. the complete four-instrument downstream correction chain (Planning Review,
   Experimental Reclassification Scope Lock, Capture-Proxy Bounding Scope
   Lock, and this Plan) is thereby fully accepted.
```

This amendment does not edit the pending Decision and does not perform its ICR.

## 10. Four-instrument closure — conditional, not yet complete

```text
(a) Planning Review Model-Role Amendment                          -- ACCEPTED, committed, pushed
(b) Experimental Reclassification Scope Lock Amendment             -- ACCEPTED, committed, pushed
(c) Capture-Proxy Bounding Scope Lock Amendment                    -- ACCEPTED, committed, pushed
(d) Bounding Evidence Acquisition and Offline Estimator Plan
    Model Role Amendment (this document)                           -- DRAFTED ONLY; NOT YET REVIEWED
```

**Drafting this amendment does not complete the four-instrument sequence.** Completion requires, in addition to drafting: this amendment's own Independent Constitutional Review returning `ACCEPTED`; and this amendment together with that ICR being committed and pushed to `main`. Only once all four instruments have each independently received an accepted ICR and been committed does the downstream correction chain the controlling Model Role and Research Question Scope Lock's own Section G identifies become fully accepted — and only then does the pending Implementation Authorization Decision become eligible to be revisited under the corrected governance chain.

## 11. Prohibited interpretations

This amendment must not be read as: selection of `llama3.2:3b` as a remedy; a preference for `llama3.2:3b` over `qwen2.5-coder:7b`; approval of `llama3.2:3b` for deployment; computation, selection, or approval of any numeric request, response, header, `E`, or `R` bound (none is performed here, exactly as the historical Plan's own Section 3 already states of itself); authorization of an estimator run, evidence acquisition, provider query, provider launch, or model contact; authorization of any Kotlin, Gradle, production, test, container, VM, filesystem, or infrastructure change; a change to `READINESS=NOT READY`; issuance or implication of Explicit Execution Approval; authorization of Knowledge Discoverability Attempt 3; completion of the four-instrument correction sequence merely by being drafted (Section 10 above); an edit to the pending Implementation Authorization Decision or performance of its ICR; or a resolution of anything that Decision itself still requires.

## 12. Status

```text
PROPOSED — PENDING INDEPENDENT CONSTITUTIONAL REVIEW
IMPLEMENTATION_AUTHORIZED = NO
EVIDENCE_ACQUISITION_AUTHORIZED = NO
LIVE_MODEL_AUTHORITY = NONE
NEW_MEASUREMENTS_AUTHORIZED = NO
NUMERIC_BOUND_CHANGED = NONE (no bound existed to change)
REMEDY_SELECTED = NO
CANDIDATE_ADOPTED = NO
FOUR_INSTRUMENT_CHAIN_COMPLETE = NO (pending this amendment's own ICR,
  commit, and push)
NEXT_LAWFUL_ACTION = Independent Constitutional Review of this amendment
```

Not effective until that review is accepted and this amendment is merged. Until then, the historical target Plan — read together with its already-committed model-identity-premise status-line correction — remains the accepted record.

## STOP conditions confirmed

```text
NO model called, loaded, or contacted.
NO live diagnostic performed.
NO new measurement, estimator run, or evidence acquisition performed.
NO numeric bound computed, selected, or changed.
NO production code, test, Docker, persistence, QMD, UI, parser, or model
  configuration file touched.
NO other governance instrument amended (Planning Review amendment,
  Experimental Reclassification Scope Lock amendment, Capture-Proxy
  Bounding Scope Lock amendment all left exactly as previously accepted).
NO edit made to the pending Implementation Authorization Decision.
NO Independent Constitutional Review performed (this document's own, or
  any other).
NO document staged, committed, or pushed.
```
