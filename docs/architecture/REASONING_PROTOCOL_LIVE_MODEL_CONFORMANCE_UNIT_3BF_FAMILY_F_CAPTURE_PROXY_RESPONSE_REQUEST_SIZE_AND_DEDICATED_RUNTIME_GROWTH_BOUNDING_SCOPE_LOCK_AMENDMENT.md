**Status:** Unit 3-BF Family F Capture-Proxy Response/Request-Size and Dedicated-Runtime-Growth Bounding Scope Lock — Model Role Amendment — **PROPOSED — PENDING INDEPENDENT CONSTITUTIONAL REVIEW.** This document amends one specific, named section of the frozen Unit 3-BF Family F Capture-Proxy Response/Request-Size and Dedicated-Runtime-Growth Bounding Scope Lock (`docs/architecture/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_CAPTURE_PROXY_RESPONSE_REQUEST_SIZE_AND_DEDICATED_RUNTIME_GROWTH_BOUNDING_SCOPE_LOCK.md`) in place of editing that document directly, preserving its original text as the historical record of what was originally frozen and why — following the identical precedent already established by the Unit 3-C Timeout and Durability Scope Lock Amendment, the Family F Alternative-Model Diagnostic Planning Review Model Role Amendment, and the Experimental Reclassification and Qualification-Boundary Scope Lock Amendment. It derives its entire authority from the accepted Model Role and Research Question Scope Lock and the accepted Experimental Reclassification and Qualification-Boundary Scope Lock Amendment, both present in the baseline this amendment is drafted against (`2b0b54ee8b19a9b457903872b3ad26ff1fa5b589`). It does not become effective merely by being drafted, selects no remedy, adopts no candidate, creates no new empirical resource evidence, changes no resource bound, authorizes no live model call, no live diagnostic, no evidence acquisition, and no implementation. It requires its own accepted Independent Constitutional Review. Until that review is accepted, the historical target Scope Lock — read together with its already-committed model-identity-premise status-line correction — remains the accepted record.

# Unit 3-BF Family F Capture-Proxy Response/Request-Size and Dedicated-Runtime-Growth Bounding Scope Lock — Model Role Amendment

## 1. Baseline and authority

Drafted against committed baseline `2b0b54ee8b19a9b457903872b3ad26ff1fa5b589` (`docs(governance): accept Family F reclassification model roles`). Amends the frozen target Scope Lock, **Section 4 only** — every other section, including Sections 1–3 and 5–24, remains in full, unamended force.

Controlling authority:

```text
DEPLOYED_BASELINE = qwen2.5-coder:7b
CONTROL_MODEL     = qwen2.5-coder:7b
SUBJECT_MODEL     = llama3.2:3b
```

frozen by the accepted Model Role and Research Question Scope Lock, and independently reaffirmed by the accepted Experimental Reclassification and Qualification-Boundary Scope Lock Amendment, whose own Section 8 correction this document's Section 2 directly re-freezes at this instrument's own level, one governance tier downstream.

`CONTROL_MODEL` means Parker's actual deployed reasoning-protocol baseline and the reference point for the Family F diagnostic comparison. `SUBJECT_MODEL` means the already-identified Family F diagnostic candidate being screened against that baseline. `SUBJECT_MODEL` does not mean selected remedy, preferred model, qualified model, approved replacement, or production-adoption candidate.

This amendment does not itself implement anything, acquire evidence, or select any numeric bound. Section 16's twelve-step measurement-and-decision sequence and Section 22's exit criteria and next-lawful-action statement are both unamended and remain fully controlling.

## 2. Amendment to Section 4 (Frozen campaign invariants) — role assignment only

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
> FIXTURE_PROFILE_CELLS_PER_ROLE=46
> ADVANCEMENT_GATE=absolute subject-only 3-of-4 per cell plus the existing zero-false-positive,
>   zero-material-mutation, and representation-validity conditions
> RANKING=PROHIBITED
> SUBSTITUTION=PROHIBITED
> QUANTIZATION_CHANGE=PROHIBITED
> REDUCED_CORPUS_PROFILE_REPETITION_OR_CALL_SCHEDULE=PROHIBITED
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
FIXTURE_PROFILE_CELLS_PER_ROLE=46
ADVANCEMENT_GATE=absolute subject-only 3-of-4 per cell plus the existing zero-false-positive,
  zero-material-mutation, and representation-validity conditions
RANKING=PROHIBITED
SUBSTITUTION=PROHIBITED
QUANTIZATION_CHANGE=PROHIBITED
REDUCED_CORPUS_PROFILE_REPETITION_OR_CALL_SCHEDULE=PROHIBITED
```

Every other line of this table is unamended: `FIXTURE_COUNT`, `CONTEXT_PROFILE_COUNT`, `SCORED_REPETITIONS_PER_CELL_PER_ROLE`, `SCORED_CALLS`, `WARMUP_CALLS`, `TOTAL_CALLS`, and `FIXTURE_PROFILE_CELLS_PER_ROLE` are role-symmetric campaign-structure constants (identical count applies to whichever model occupies either role — "per role," "per cell") and do not depend on which physical model is `SUBJECT_MODEL` or `CONTROL_MODEL`. `ADVANCEMENT_GATE` is stated in role-generic terms ("subject-only") and automatically applies to the corrected `SUBJECT_MODEL` without its own edit. `RANKING`, `SUBSTITUTION`, `QUANTIZATION_CHANGE`, and `REDUCED_CORPUS_PROFILE_REPETITION_OR_CALL_SCHEDULE` are symmetric prohibitions, unaffected by which model holds which role.

## 3. Independent confirmation: no other section requires amendment

An exhaustive search of the entire historical Scope Lock for every occurrence of `qwen`/`llama` (case-insensitive) found exactly one substantive hit beyond the already-existing status-line correction: Section 4's two-line table, corrected above. Every other section (5–21) discusses request/response/header/evidence/runtime bounds exclusively in role-generic terms (`subject`, `control`, `both frozen model-name strings`) or in terms that do not reference model identity at all (proxy mechanics, ledger fields, provenance-package structure, containment contract, stop conditions). None requires its own edit; each already, and automatically, follows the corrected Section 4 assignment.

One incidental false-positive match is noted for completeness, not correction: Section 6 item 4 references the production function name `defaultOllamaRequestBody` — a string that contains the substring "llama" only because it names the Ollama HTTP client's request-body serializer, not the `llama3.2:3b` model. This is not a role-dependent proposition and is not touched.

## 4. Critical stop condition — independently checked, not triggered

Per this task's own governing instruction, this amendment independently verifies whether correcting `SUBJECT_MODEL`/`CONTROL_MODEL` requires changing any model-specific numeric bound for which current evidence does not exist.

**It does not.** Independently confirmed from the historical Scope Lock's own Section 5 and Section 24 disposition blocks: at this baseline, `NUMERIC_BOUND_SELECTED=NONE`; `MAX_REQUEST_BOUND_STATUS=COMPUTABLE IN PRINCIPLE; NOT COMPUTED OR SELECTED`; `MAX_RESPONSE_BOUND_STATUS=UNCOMPUTABLE`; `HEADER_BOUNDS_STATUS=UNRESOLVED`; `E_STATUS=UNCOMPUTABLE`; `R_STATUS=UNCOMPUTABLE`. No numeric request, response, header, evidence-budget, or runtime-growth value has ever been computed, proposed, or selected against either model under this Scope Lock — the entire document defines only the *protocol* by which such a value could later be lawfully proposed (Sections 6–15), not any actual measured value. There is therefore nothing in this document that was computed against the wrong model and now requires re-derivation: the role correction has zero numeric consequence. No new measurement, estimator run, or model contact is required, authorized, or performed by this amendment.

## 5. Bounding methodology — independently confirmed unchanged in substance

The following, all independently re-checked against the target Scope Lock's own text, are not touched by this amendment and remain in full force: the `MAX_REQUEST_BOUND` selection protocol and its eight requirements (Section 6); the `MAX_RESPONSE_BOUND` admissible-evidence routes and fail-closed default (Section 7); the header-bound selection protocol (Section 8); the bounded capture-proxy enforcement contract, including `STREAMING_COUNT`, `REJECT_BEFORE_EXCESS_ALLOCATION`, `NO_TRUNCATE_AND_FORWARD`, and the remaining five enforcement properties (Section 9); oversize-failure and durable-evidence requirements (Section 10); the `E` recomputation rule and the reaffirmed `NO_MANUFACTURED_PASS` prohibition (Section 11); `R`'s scope and admissible-evidence routes (Sections 12–13); the runtime containment contract (Section 14); the evidence-provenance package structure (Section 15); the measurement-and-decision separation sequence (Section 16); the host-isolation handoff (Section 17); the nine acceptance gates for any future proposed bound (Section 18); all twelve stop conditions (Section 19); the decision register (Section 20); the explicit non-claims list (Section 21); and the exit criteria (Section 22). This amendment is a model-role correction only, not a resource-study redesign, and changes no constant, formula, gate, or evidentiary requirement anywhere in these sections.

## 6. Historical-evidence and provenance boundary — unaltered

This amendment changes no historical fact and creates no new one. No measurement has ever been captured under either model's identity by this Scope Lock (Section 4 above) — there is accordingly no historical measurement to relabel, and none is relabeled. Knowledge Discoverability Attempts 1–2, Unit 2/Unit 2-D evidence, and every prior host/resource observation cited as controlling authority (Section 1 of the target Scope Lock) are not restated, reinterpreted, or touched anywhere in this amendment. The provenance-package requirement that "no value may inherit acceptance from another provider version, model, host, filesystem, serializer, or campaign shape" (Section 15) is unamended and remains fully controlling for any future bound proposal under either corrected role.

## 7. Downstream dependency into the Bounding Evidence Acquisition and Offline Estimator Plan — recorded, not amended

Independently checked: the Bounding Evidence Acquisition and Offline Estimator Plan's own Section 4 ("Frozen inputs and statuses") carries its own frozen table —

```text
SUBJECT_MODEL=qwen2.5-coder:7b
CONTROL_MODEL=llama3.2:3b
FIXTURE_COUNT=23
CONTEXT_PROFILE_COUNT=2
SCORED_REPETITIONS_PER_CELL_PER_ROLE=4
SCORED_CALLS=368
WARMUP_CALLS=24
TOTAL_CALLS=392
FROZEN_SCHEDULE=the accepted deterministic Family F schedule, including AB/BA alternation
PRODUCTION_FORMATTER=DefaultReasoningPromptBuilder
PRODUCTION_REQUEST_SERIALIZER=defaultOllamaRequestBody
```

— which its own accepted Independent Constitutional Review states was "independently checked against the Bounding Scope Lock's own Section 4 and matches exactly." This confirms the dependency directly: the Plan's own `SUBJECT_MODEL`/`CONTROL_MODEL` values and its `FIXTURE_COUNT`/`SCORED_CALLS`/`TOTAL_CALLS` figures are drawn from this Scope Lock's own (now-amended) Section 4, unchanged in every respect except the two corrected role-assignment lines.

**This amendment does not edit the Bounding Evidence Acquisition and Offline Estimator Plan.** That instrument's own correction is a separate, later task in the sequence the controlling Model Role and Research Question Scope Lock's own Section G already fixes, requiring its own independent drafting and its own Independent Constitutional Review. Recording this dependency here is disclosure, not action.

## 8. Prohibited interpretations

This amendment must not be read as: selection of `llama3.2:3b` as a remedy; a preference for `llama3.2:3b` over `qwen2.5-coder:7b`; approval of `llama3.2:3b` for deployment; computation, selection, or approval of any numeric request, response, header, `E`, or `R` bound (none is performed here, exactly as the historical Scope Lock's own Section 3 already states of itself); authorization of an estimator run, benchmark, provider query, provider launch, or model contact; authorization of any Kotlin, Gradle, production, test, container, VM, filesystem, or infrastructure change; a change to `READINESS=NOT READY`; issuance or implication of Explicit Execution Approval; authorization of Knowledge Discoverability Attempt 3; an amendment to the Bounding Evidence Acquisition and Offline Estimator Plan (a separate, unamended task); or a resolution of anything the pending Family F Bounding Evidence Implementation Authorization Decision itself still requires, which this amendment does not touch.

## 9. Status

```text
PROPOSED — PENDING INDEPENDENT CONSTITUTIONAL REVIEW
IMPLEMENTATION_AUTHORIZED = NO
EVIDENCE_ACQUISITION_AUTHORIZED = NO
LIVE_MODEL_AUTHORITY = NONE
NEW_MEASUREMENTS_AUTHORIZED = NO
NUMERIC_BOUND_CHANGED = NONE (no bound existed to change)
REMEDY_SELECTED = NO
CANDIDATE_ADOPTED = NO
NEXT_LAWFUL_ACTION = Independent Constitutional Review of this amendment
```

Not effective until that review is accepted and this amendment is merged. Until then, the historical target Scope Lock — read together with its already-committed model-identity-premise status-line correction — remains the accepted record.

## STOP conditions confirmed

```text
NO model called, loaded, or contacted.
NO live diagnostic performed.
NO new measurement, estimator run, or evidence acquisition performed.
NO numeric bound computed, selected, or changed.
NO production code, test, Docker, persistence, QMD, UI, parser, or model
  configuration file touched.
NO downstream document amended (Bounding Evidence Acquisition and Offline
  Estimator Plan unedited).
NO edit made to the pending Implementation Authorization Decision.
NO Independent Constitutional Review performed (this document's own, or
  any other).
NO document staged, committed, or pushed.
```
