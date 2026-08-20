**Status:** Family F Alternative-Model Diagnostic Planning Review — Model Role and Research Question Amendment — **PROPOSED — PENDING INDEPENDENT CONSTITUTIONAL REVIEW.** This document amends specific, named sections of the frozen Family F Alternative-Model Diagnostic Planning Review (`docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_FAMILY_F_ALTERNATIVE_MODEL_DIAGNOSTIC_PLANNING_REVIEW.md`, commit `6ecf8965`) in place of editing that document directly, preserving its original text as the historical record of what was originally proposed and why — mirroring the established Unit 3-C Timeout and Durability Scope Lock Amendment's own precedent for correcting a frozen instrument without rewriting it. It derives its entire authority from the accepted Model Role and Research Question Scope Lock (`docs/architecture/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_FAMILY_F_MODEL_ROLE_AND_RESEARCH_QUESTION_SCOPE_LOCK.md`) and its accepted Correction Independent Constitutional Review, both present in the baseline this amendment is drafted against (`b22e733f23503a427dad3a614d7fbe1e9a8e3995`). It does not become effective merely by being drafted, selects no remedy, adopts no candidate, authorizes no live model call, no live diagnostic, no qualification, and no implementation, and does not itself edit the pending Family F Bounding Evidence Implementation Authorization Decision. It requires its own accepted Independent Constitutional Review. Until that review is accepted, the historical Planning Review — read together with its already-committed model-identity-premise status-line correction — remains the accepted record.

# Family F Alternative-Model Diagnostic Planning Review — Model Role and Research Question Amendment

## 1. Baseline and authority

Drafted against committed baseline `b22e733f23503a427dad3a614d7fbe1e9a8e3995` (`docs(governance): accept Family F model role scope lock`). Amends the frozen Family F Alternative-Model Diagnostic Planning Review, Sections 7, 9, 12, and 18 only, and adds one clarifying note to Section 5 item 2 — every other section of that document remains in full, unamended force, including its own already-committed model-identity-premise status-line correction (line 3 of the target document), which this amendment does not restate or duplicate.

Controlling authority: the accepted Model Role and Research Question Scope Lock, which freezes:

```text
DEPLOYED_BASELINE = qwen2.5-coder:7b
CONTROL_MODEL     = qwen2.5-coder:7b
SUBJECT_MODEL     = llama3.2:3b
```

and the corrected Family F research question (quoted verbatim in Section 3 below), and its own accepted Correction Independent Constitutional Review, which independently re-derived and confirmed both.

This amendment does not itself implement anything. Per this programme's own established pattern (Planning Review → Scope Lock → Implementation Plan → review chain, restated at Section 15 of the target document), the governance sequence the target Planning Review itself already froze is unchanged by this amendment — only the model-role premise that sequence's own text rests on is corrected.

## 2. Amendment to Section 7 (Planning decision), items 2 and 4 — role assignment

**Original text (preserved, not deleted):**

> 2. `qwen2.5-coder:7b` may be named as the proposed diagnostic subject because it is the alternative model already present in DQ4's historical comparison and in the programme's originating evidence.
> 4. `llama3.2:3b` may be named as the proposed control identity because it produced the accepted Attempt 2 operational evidence and is the current comparison point in DQ4.

**Frozen, amended:** these two role assignments are reversed. `qwen2.5-coder:7b` is `CONTROL_MODEL` — Parker's actual, committed deployed Docker baseline (unchanged since before this programme began), not a proposed subject. `llama3.2:3b` is `SUBJECT_MODEL` — the already-identified diagnostic candidate, not a control identity, and not because it is "the current comparison point" (a characterization the accepted Model-Identity Premise Defect Confirmation Review already found false) but because it already carries the only exploratory-tier comparative data this programme has ever gathered on any alternative (`DQ4`, and separately the accepted Knowledge Discoverability Attempts 1–2) — the same evidence-reuse rationale the original text correctly identified, now attached to the correct role. Naming either model in either corrected role remains, exactly as the original text already stated of the reversed assignment, not model selection, not remedy selection, not qualification, not deployment approval, and not permission to confirm current availability by loading or invoking it. Items 1, 3, 5, and 6 of Section 7 are unamended and remain in full force, including item 6's prohibition on any live or offline model inference under the Planning Review.

## 3. Amendment to Section 9 (Proposed diagnostic purpose) — corrected research question

**Original text (preserved, not deleted):**

> Determine whether a digest-pinned alternative model shows enough bounded, reproducible improvement over the digest-pinned current comparison model across the frozen semantic and safety surface to justify a later, separately governed full qualification campaign.

**Frozen, amended — superseded by the exact research question the accepted Scope Lock freezes:**

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

This is not a broadening of the original question's substance — it is the same screening question (alternative candidate vs. deployed baseline, on the frozen surface, toward a possible future qualification proposal), with the candidate and baseline correctly identified and the defect surface stated explicitly rather than left generic. The original Section 9's own list of what the diagnostic must not answer (deployment selection, production-threshold satisfaction, remedy-selection status, Knowledge Discoverability closure, Attempt 3 authorization) is unamended and remains in full force.

## 4. Amendment to Section 12 (Candidate and control identity)

**Original text (preserved, not deleted):**

> - proposed alternative-model subject: `qwen2.5-coder:7b`;
> - proposed comparison control: `llama3.2:3b`.

**Frozen, amended:**

```text
CONTROL_MODEL (Parker's actual deployed baseline): qwen2.5-coder:7b
SUBJECT_MODEL (already-identified diagnostic candidate): llama3.2:3b
```

The remainder of Section 12 — that these are planning labels only, that immutable identity evidence (provider, exact model name, digest, runtime version, repository commit, prompt hash, inference settings, hardware/environment identity) remains required before any later authorized execution, that historical presence on a server is not evidence of current availability, and that a model-name match without required identity evidence must fail closed — is unamended and remains in full force, unweakened.

## 5. Amendment to Section 18 (Decision register)

**Original text (preserved, not deleted):**

> | Is `qwen2.5-coder:7b` selected? | No; it is only the proposed diagnostic subject. | RESOLVED |
> | Is `llama3.2:3b` qualified? | No; it is only the proposed comparison control. | RESOLVED |

**Frozen, amended:**

| Item | Decision | Status |
|---|---|---|
| Is `qwen2.5-coder:7b` selected as a remedy? | No; it is Parker's actual deployed baseline (`CONTROL_MODEL`), named as the comparison reference point, not selected as anything. | RESOLVED |
| Is `llama3.2:3b` qualified? | No; it is only the already-identified diagnostic candidate (`SUBJECT_MODEL`). | RESOLVED |

Every other row of Section 18's decision register is unamended and remains in full force, including the rows resolving that Family F execution remains excluded, that no remedy or model run is authorized, that implementation is not authorized, and that Knowledge Discoverability Attempt 3 is not authorized.

## 6. Clarifying note to Section 5, item 2 — not a substantive amendment

**Original text (preserved, not deleted):** *"the blocker reproduced twice operationally with the current live model, with Attempt 2 directly observing the wrong valid action tag."*

**Clarification, not a rewrite:** "the current live model" in this sentence refers to the model Knowledge Discoverability Attempts 1–2 actually used (`llama3.2:3b`), not to Parker's deployed Docker baseline (`qwen2.5-coder:7b`) — a distinction the accepted Model-Identity Premise Defect Confirmation Review establishes and this amendment does not restate at length, since the target document's own line 3 status-line correction already covers it. This note exists only so a future reader of this amendment does not mistake Section 5 item 2's own unamended wording for a fresh assertion that `llama3.2:3b` is deployed. The historical fact the sentence records — that Attempts 1–2 reproduced the blocker twice, and that Attempt 2 directly observed the wrong valid action tag — is accurate and is not amended.

## 7. Historical-evidence boundary — independently restated, not altered

This amendment changes no historical fact:

- Knowledge Discoverability Attempts 1–2 used `llama3.2:3b`, through their own recorded capture-proxy endpoints; this remains exactly as recorded, in this document and in the Attempts 1–2 review itself.
- Attempt 2's own captured `REPLY` observation on both turns remains exactly as recorded.
- Unit 2's and Unit 2-D's own `qwen2.5-coder:7b` baseline-characterization evidence (including `DQ4`'s single-attempt comparison) remains exactly as recorded.
- No evidence corpus is pooled, renamed, reweighted, or reclassified by this amendment. Section 4 of the target Planning Review (Evidence admitted for this planning decision) is **not amended by this document** — every subsection (4.1 `DQ4`, 4.2 Attempts 1–2, 4.3 existing Unit 3 remedy evidence, 4.4 absence of qualification evidence) stands exactly as originally written.

## 8. Substantive preservation — independently confirmed unamended

The following, all independently re-checked against the target document's own text, are not touched by this amendment and remain in full force:

- Family F's diagnostic-only, non-remedy-selecting status (Section 2 item 6; Section 14; Section 19; Section 20 disposition block).
- The existing Unit 3-B item 14 qualification-first contradiction and its resolution requirement (Section 6).
- The recommended Path 2 pre-qualification diagnostic exception framing and its own prohibitions (Section 8).
- The minimum evidence surface requirement — full blind 23-fixture corpus, all four action families, negative/adversarial Remember fixtures, content-fidelity and representation-validity separation (Section 10).
- The Knowledge Discoverability dependency-probe boundary (Section 11).
- The resource and operational boundary, including the requirement for a model-specific resource budget rather than the prior generic 2.0 GiB gate (Section 13).
- The evidence-tier and interpretation limits (Section 14).
- The proposed governance sequence, unchanged in its ten steps and its "no step authorizes its successor automatically" rule (Section 15).
- The scope reserved for a future diagnostic Implementation Plan (Section 16).
- Every one of the seventeen stop conditions (Section 17).
- The explicit non-claims list (Section 19) — in particular, "Qwen is better than Llama" and "model substitution is the cause or remedy for PF01" remain explicitly disclaimed, exactly as before; this amendment introduces no comparative claim of its own.
- The planning verdict's own disposition block (Section 20): `FAMILY_F_EXECUTION_STATUS=EXCLUDED_ON_CURRENT_EVIDENCE`, `REMEDY_SELECTED=NO`, `MODEL_SELECTED=NO`, `MODEL_RUN_AUTHORIZED=NO`, `CAMPAIGN_AUTHORIZED=NO`, `IMPLEMENTATION_AUTHORIZED=NO`, `KNOWLEDGE_DISCOVERABILITY_ATTEMPT_3_AUTHORIZED=NO`, `KNOWLEDGE_DISCOVERABILITY_CLOSURE=BLOCKED` — all unchanged.

No new candidate model is introduced. No new remedy theory is introduced. No new evidence is admitted.

## 9. Dependency into the Experimental Reclassification and Qualification-Boundary Scope Lock — recorded, not amended

Independently checked: the Experimental Reclassification and Qualification-Boundary Scope Lock's own Section 7 permitted-purpose statement is drawn directly from this Planning Review's own (now-amended) Section 9, and that Scope Lock's own Section 8 "Proposed subject and comparison control" naming is drawn directly from this Planning Review's own (now-amended) Section 12 — both quoting the identical backward role assignment this amendment corrects. This is exactly the dependency the accepted Model Role and Research Question Scope Lock's own Section G already identifies as requiring its own, separate bounded superseding amendment.

**This amendment does not amend the Experimental Reclassification and Qualification-Boundary Scope Lock.** That instrument's own correction is a separate, later task in the sequence the controlling Scope Lock's Section G already fixes, requiring its own independent drafting and its own Independent Constitutional Review. Recording this dependency here is disclosure, not action.

## 10. Prohibited interpretations

This amendment must not be read as: selection of `llama3.2:3b` as a remedy; a preference for `llama3.2:3b` over `qwen2.5-coder:7b`; approval of `llama3.2:3b` for deployment; authorization of qualification for either model; authorization of a live diagnostic, model call, or campaign; authorization of implementation; abandonment or replacement of Family F; a rewrite of any historical evidence, including Attempts 1–2's own recorded observations; an amendment to the Experimental Reclassification and Qualification-Boundary Scope Lock, the Capture-Proxy Bounding Scope Lock, or the Bounding Evidence Acquisition and Offline Estimator Plan (each remains a separate, unamended task); or a resolution of anything the pending Family F Bounding Evidence Implementation Authorization Decision itself still requires, which this amendment does not touch.

## 11. Status

```text
PROPOSED — PENDING INDEPENDENT CONSTITUTIONAL REVIEW
IMPLEMENTATION_AUTHORIZED = NO
LIVE_MODEL_AUTHORITY = NONE
REMEDY_SELECTED = NO
CANDIDATE_ADOPTED = NO
NEXT_LAWFUL_ACTION = Independent Constitutional Review of this amendment
```

Not effective until that review is accepted and this amendment is merged. Until then, the historical Planning Review — read together with its already-committed model-identity-premise status-line correction — remains the accepted record.

## STOP conditions confirmed

```text
NO model called, loaded, or contacted.
NO live diagnostic performed.
NO production code, test, Docker, persistence, QMD, UI, parser, or model
  configuration file touched.
NO downstream document amended (Experimental Reclassification Scope Lock,
  Capture-Proxy Bounding Scope Lock, Bounding Evidence Acquisition Plan all
  unedited).
NO edit made to the pending Implementation Authorization Decision.
NO Independent Constitutional Review performed (this document's own, or
  any other).
NO document staged, committed, or pushed.
```
