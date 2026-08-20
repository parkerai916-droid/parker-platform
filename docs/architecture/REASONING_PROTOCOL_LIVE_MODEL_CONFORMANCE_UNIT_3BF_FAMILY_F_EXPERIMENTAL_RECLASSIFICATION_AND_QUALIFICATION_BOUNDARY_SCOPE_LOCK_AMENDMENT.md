**Status:** Unit 3-BF Family F Experimental Reclassification and Qualification-Boundary Scope Lock — Model Role and Research Question Amendment — **PROPOSED — PENDING INDEPENDENT CONSTITUTIONAL REVIEW.** This document amends specific, named sections of the frozen Unit 3-BF Family F Experimental Reclassification and Qualification-Boundary Scope Lock (`docs/architecture/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_EXPERIMENTAL_RECLASSIFICATION_AND_QUALIFICATION_BOUNDARY_SCOPE_LOCK.md`) in place of editing that document directly, preserving its original text as the historical record of what was originally frozen and why — following the identical precedent already established by the Unit 3-C Timeout and Durability Scope Lock Amendment and the Family F Alternative-Model Diagnostic Planning Review Model Role Amendment. It derives its entire authority from the accepted Model Role and Research Question Scope Lock and the accepted Family F Alternative-Model Diagnostic Planning Review Model Role Amendment, both present in the baseline this amendment is drafted against (`d08f836e1dad4487526ddaee2d3d4afee8796918`). It does not become effective merely by being drafted, selects no remedy, adopts no candidate, authorizes no live model call, no live diagnostic, no qualification, and no implementation. It requires its own accepted Independent Constitutional Review. Until that review is accepted, the historical target Scope Lock — read together with its already-committed model-identity-premise status-line correction — remains the accepted record.

# Unit 3-BF Family F Experimental Reclassification and Qualification-Boundary Scope Lock — Model Role and Research Question Amendment

## 1. Baseline and authority

Drafted against committed baseline `d08f836e1dad4487526ddaee2d3d4afee8796918` (`docs(governance): accept Family F planning review model roles`). Amends the frozen Unit 3-BF Family F Experimental Reclassification and Qualification-Boundary Scope Lock, Sections 7 and 8 only — every other section of that document, including Sections 5, 6, 9–25, 26, 27, 28, 29, and 30, remains in full, unamended force.

Controlling authority:

```text
DEPLOYED_BASELINE = qwen2.5-coder:7b
CONTROL_MODEL     = qwen2.5-coder:7b
SUBJECT_MODEL     = llama3.2:3b
```

frozen by the accepted Model Role and Research Question Scope Lock, and the corrected Family F research question it also freezes (quoted verbatim in Section 3 below). Both are independently reaffirmed, unmodified, by the accepted Family F Alternative-Model Diagnostic Planning Review Model Role Amendment, which this document's own Section 7 and Section 8 corrections mirror exactly, one governance tier downstream.

This amendment does not itself implement anything. Section 23 of the target Scope Lock (Governance gates before any execution) is unamended and remains the controlling sequence — this amendment does not advance, skip, or shortcut any of its eleven steps.

## 2. Amendment to Section 8 (Proposed subject and comparison control) — role assignment

**Original text (preserved, not deleted):**

> The only permitted planning identities are:
>
> - diagnostic subject: `qwen2.5-coder:7b`;
> - comparison control: `llama3.2:3b`.
>
> ...
>
> Naming Qwen here is not selection, qualification, availability confirmation, acquisition authorization, load authorization, or run authorization.

**Frozen, amended:**

```text
CONTROL_MODEL (Parker's actual deployed baseline; reference point for the
  Family F diagnostic comparison): qwen2.5-coder:7b
SUBJECT_MODEL (already-identified diagnostic candidate being screened
  against the deployed baseline): llama3.2:3b
```

`SUBJECT_MODEL` does not mean, and must not be read to mean, selected remedy, preferred model, approved replacement, production adoption, or qualification acceptance — restated here in the identical terms the original text already applied to the reversed assignment.

**Naming `llama3.2:3b` here is not selection, qualification, availability confirmation, acquisition authorization, load authorization, or run authorization.** (This sentence is itself amended, not merely reinterpreted: the original text's disclaimer specifically named "Qwen" because Qwen was, under the reversed assignment, the model being introduced as a candidate. Under the corrected assignment, the model actually at risk of being misread as a candidate selection is `llama3.2:3b`, the `SUBJECT_MODEL` — the disclaimer is corrected to name it. The identical disclaimer continues to apply to `qwen2.5-coder:7b` as well, for the avoidance of doubt, though as `CONTROL_MODEL` — Parker's own already-deployed baseline — it was never at meaningful risk of being read as a fresh selection.)

The remainder of Section 8 — that neither mutable model name is sufficient for execution identity; the full identity-pinning requirement (provider, endpoint type, exact model name, immutable digest, artifact size/quantization metadata, runtime/server version, repository commit, production prompt-builder/provider/inference-client/parser identity, inference options, prompt hash, execution-host identity); and the fail-closed rule on any identity mismatch, missing digest, substitution, or silent configuration change — is unamended and remains in full force, unweakened, applying identically to both models under their corrected roles.

## 3. Amendment to Section 7 (Diagnostic purpose) — corrected research question

**Original text (preserved, not deleted):**

> The only permitted purpose of a future campaign operating under this Scope Lock is:
>
> > Determine whether a digest-pinned `qwen2.5-coder:7b` diagnostic subject demonstrates sufficiently broad, repeated, representation-valid, fidelity-preserving action selection across the frozen semantic and safety surface to justify a later proposal for full Unit 3-A qualification investment.
>
> The campaign cannot answer whether Qwen should replace Llama, whether either model is qualified, whether a remedy is selected, whether production should change, or whether Knowledge Discoverability Attempt 3 should occur.

**Frozen, amended — the permitted purpose is superseded by the exact research question the accepted Model Role and Research Question Scope Lock freezes:**

```text
FAMILY_F_RESEARCH_QUESTION=
Given that qwen2.5-coder:7b -- not llama3.2:3b -- is Parker's actual,
continuously deployed reasoning-protocol model, and that this programme's
own governed evidence (Unit 2, Unit 2-D) already demonstrates explicit-
REMEMBER-instruction misclassification, including misclassification as an
ordinary Reply carrying no persistence authority, as a genuine, unremedied
defect surface in that deployed configuration: does llama3.2:3b, evaluated
strictly under the existing bounded, pre-qualification Family F diagnostic
design (frozen corpus, frozen context profiles, frozen 392-call schedule,
absolute advancement gate), show enough reproducible improvement over
qwen2.5-coder:7b on that same defect surface to justify a future, separately
governed full qualification campaign?

This question makes no claim that any remedy has been selected, that model
substitution is the correct remedy family, or that Family F's diagnostic-
only, non-model-selecting scope has changed.
```

This is not a broadening of this Scope Lock's own function: it is the same investment-screening purpose (does the digest-pinned diagnostic subject demonstrate enough action-selection quality across the frozen semantic and safety surface to justify proposing full qualification), with the candidate and baseline correctly identified and the defect surface it is screening against — explicit-REMEMBER misclassification — stated explicitly rather than left generic. This amendment changes no arithmetic, no gate, and no evidentiary threshold anywhere in the target Scope Lock; only the identity of which model plays which role in the already-frozen 392-call design (Sections 9–21, unamended) is corrected.

**The prohibition sentence is amended for accurate directionality:** *"The campaign cannot answer whether Qwen should replace Llama..."* is corrected to *"The campaign cannot answer whether `llama3.2:3b` should replace `qwen2.5-coder:7b` in Parker's deployed configuration, whether either model is qualified, whether a remedy is selected, whether production should change, or whether Knowledge Discoverability Attempt 3 should occur."* The original sentence's own directionality — "Qwen should replace Llama" — silently assumed Llama was the thing deployed and Qwen the candidate; the corrected sentence states the only direction that is actually at issue (whether the diagnostic candidate should replace Parker's own deployed baseline), while preserving, unweakened, the sentence's own function: that this campaign, either way, cannot answer the question.

## 4. Sections independently confirmed to require no amendment

The following sections use role-generic language (`diagnostic subject`, `comparison control`, `proposed subject`) without naming a specific model, and therefore already correctly follow Section 8's corrected assignment without requiring their own textual change — independently re-checked, not assumed:

- Section 6 (Narrow comparison-discipline clarification) — "the proposed subject and comparison control," "the proposed subject satisfies the absolute advancement gate."
- Section 12 (Frozen warm-up and residency schedule) — "diagnostic subject first, comparison control second."
- Section 18 (Absolute advancement gate) — "the diagnostic subject may be reported as eligible... in the complete, valid 184-call subject schedule."
- Section 27 (Decision register) rows "Is Qwen selected or qualified?" / "Is Llama selected or qualified?" — both independently `No` regardless of which model occupies which role; not amended.
- Section 28 (Explicit non-claims) — "Qwen is better than Llama," "Llama is an adequate or inadequate production model" — symmetric non-claims, correct and complete regardless of role assignment; not amended.

## 5. Historical-evidence boundary — independently restated, not altered

This amendment changes no historical fact. Unit 2's frozen 23-fixture corpus (Section 9), the frozen context profiles (Section 10), Knowledge Discoverability Attempts 1–2 and their own recorded use of `llama3.2:3b` (referenced as controlling authority, Section 1 of the target Scope Lock), and Unit 2/Unit 2-D's own `qwen2.5-coder:7b` evidence are not touched, restated, or reinterpreted anywhere in this amendment. Section 15's evidence-provenance-separation requirement (a new Family F artifact root; no pooling with Unit 2, Unit 2-D, Unit 3-C, or Knowledge Discoverability artifacts) is unamended and remains in full force.

## 6. Substantive preservation — independently confirmed unamended

The following, all independently re-checked against the target Scope Lock's own text, are not touched by this amendment and remain in full force:

- Family F's diagnostic-only, non-remedy-selecting reclassification (Sections 3, 4, 5, 6).
- The Unit 3-B item 14 Family F-only exception and its own explicit limits — no qualification credit, no downstream authority (Section 5).
- The comparison-ranking prohibition (Unit 3-B Section 10, restated Section 6).
- The frozen corpus, context profiles, and 392-call schedule arithmetic (Sections 9–12) — 23 fixtures × 2 profiles × 4 repetitions × 2 models = 368 scored calls; 8 residency blocks × 3 warm-up calls = 24 unscored calls; 392 total — unchanged by this amendment, independently re-confirmed to be role-label-agnostic.
- Single-variable isolation (Section 13); production-path fidelity (Section 14); evidence capture and separation (Section 15); the synthetic-data and Knowledge Discoverability boundary (Section 16); the ten measurement axes (Section 17); the absolute advancement gate's eight conditions (Section 18); the resource boundary (Section 19); operational isolation (Section 20); exact-once and durability requirements (Section 21); failure semantics (Section 22); the eleven-step governance-gate sequence (Section 23); the post-campaign interpretation boundary (Section 24); all twenty-one stop conditions (Section 25); the amendment ledger against Unit 3-B (Section 26); the decision register (Section 27, per Section 4 above); the explicit non-claims list (Section 28, per Section 4 above); the exit criteria (Section 29); and the final authority statement's disposition block (Section 30) — `REMEDY_SELECTED=NO`, `MODEL_SELECTED=NO`, `MODEL_QUALIFIED=NO`, `QWEN_RUN_AUTHORIZED=NO`, `LLAMA_RUN_AUTHORIZED=NO`, `MODEL_RUN_AUTHORIZED=NO`, `CAMPAIGN_AUTHORIZED=NO`, `MODEL_ACQUISITION_AUTHORIZED=NO`, `IMPLEMENTATION_AUTHORIZED=NO`, `KNOWLEDGE_DISCOVERABILITY_ATTEMPT_3_AUTHORIZED=NO`, `KNOWLEDGE_DISCOVERABILITY_CLOSURE=BLOCKED` — all unchanged.

No new candidate model is introduced. No new remedy theory is introduced. No new evidence is admitted. The qualification/reclassification boundary this Scope Lock exists to define — that a favourable diagnostic result screens for investment only, never qualifies or selects a model — is not weakened, inverted, or narrowed anywhere by this amendment; correcting which physical model is `SUBJECT_MODEL` does not touch the gate itself (Section 18), only which model the gate is evaluated against.

## 7. Downstream dependency into the Capture-Proxy Bounding Scope Lock — recorded, not amended

Independently checked: the Capture-Proxy Response/Request-Size and Dedicated-Runtime-Growth Bounding Scope Lock's own Section 4 ("Frozen campaign invariants") carries its own frozen table —

```text
SUBJECT_MODEL=qwen2.5-coder:7b
CONTROL_MODEL=llama3.2:3b
FIXTURE_COUNT=23
CONTEXT_PROFILE_COUNT=2
SCORED_REPETITIONS_PER_CELL_PER_ROLE=4
SCORED_CALLS=368
WARMUP_CALLS=24
TOTAL_CALLS=392
FIXTURE_PROFILE_CELLS_PER_ROLE=46
ADVANCEMENT_GATE=absolute subject-only 3-of-4 per cell plus the existing
  zero-false-positive, zero-material-mutation, and representation-validity
  conditions
```

— which its own accepted Independent Constitutional Review states was "independently re-derived against the Experimental Reclassification Scope Lock's own source text." This confirms the dependency directly: `SUBJECT_MODEL=qwen2.5-coder:7b` / `CONTROL_MODEL=llama3.2:3b` in the Capture-Proxy Bounding Scope Lock's own Section 4 is drawn from this Scope Lock's own (now-amended) Section 8, and its `FIXTURE_COUNT`/`SCORED_CALLS`/`TOTAL_CALLS`/`ADVANCEMENT_GATE` figures are drawn from this Scope Lock's own Sections 9–12 and 18 — all unamended, role-label-agnostic arithmetic that this amendment does not disturb.

**This amendment does not edit the Capture-Proxy Response/Request-Size and Dedicated-Runtime-Growth Bounding Scope Lock, and does not edit the Bounding Evidence Acquisition and Offline Estimator Plan**, which independently re-derives the identical pair from the Capture-Proxy Bounding Scope Lock's own Section 4 in turn. Both remain separate, later tasks in the sequence the controlling Model Role and Research Question Scope Lock's own Section G already fixes. Recording this dependency here is disclosure, not action.

## 8. Prohibited interpretations

This amendment must not be read as: selection of `llama3.2:3b` as a remedy; a preference for `llama3.2:3b` over `qwen2.5-coder:7b`; approval of `llama3.2:3b` for deployment; authorization of qualification for either model; authorization of a live diagnostic, model call, or campaign; authorization of implementation; a change to the reclassification decision itself (Section 4 of the target Scope Lock — Family F's status as `INCLUDED FOR PRE-QUALIFICATION DIAGNOSTIC SCOPING ONLY` upon that document's own acceptance is untouched); a rewrite of any historical evidence; an amendment to the Capture-Proxy Response/Request-Size and Dedicated-Runtime-Growth Bounding Scope Lock or the Bounding Evidence Acquisition and Offline Estimator Plan (each remains a separate, unamended task); or a resolution of anything the pending Family F Bounding Evidence Implementation Authorization Decision itself still requires, which this amendment does not touch.

## 9. Status

```text
PROPOSED — PENDING INDEPENDENT CONSTITUTIONAL REVIEW
IMPLEMENTATION_AUTHORIZED = NO
LIVE_MODEL_AUTHORITY = NONE
REMEDY_SELECTED = NO
CANDIDATE_ADOPTED = NO
NEXT_LAWFUL_ACTION = Independent Constitutional Review of this amendment
```

Not effective until that review is accepted and this amendment is merged. Until then, the historical target Scope Lock — read together with its already-committed model-identity-premise status-line correction — remains the accepted record.

## STOP conditions confirmed

```text
NO model called, loaded, or contacted.
NO live diagnostic performed.
NO production code, test, Docker, persistence, QMD, UI, parser, or model
  configuration file touched.
NO downstream document amended (Capture-Proxy Bounding Scope Lock and
  Bounding Evidence Acquisition and Offline Estimator Plan both unedited).
NO edit made to the pending Implementation Authorization Decision.
NO Independent Constitutional Review performed (this document's own, or
  any other).
NO document staged, committed, or pushed.
```
