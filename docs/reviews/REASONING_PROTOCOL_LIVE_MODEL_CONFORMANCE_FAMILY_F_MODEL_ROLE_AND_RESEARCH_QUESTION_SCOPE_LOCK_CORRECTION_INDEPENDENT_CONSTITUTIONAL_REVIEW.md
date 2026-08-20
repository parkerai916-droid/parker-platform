**Status:** Independent Constitutional Review of the corrected Family F Model Role and Research Question Scope Lock — **ACCEPTED.** This is a fresh, genuinely independent review of the Scope Lock following the bounded Section G correction, not a re-endorsement of the corrected text on the strength of the correction having been made. Every material proposition — the frozen model roles, the corrected research question, the historical-evidence boundary, the lighthouse boundary, the preserved-machinery claims, the constitutional safeguards, and the corrected Section G dependency chain — was independently re-derived from primary repository evidence read fresh for this task, not accepted from the Scope Lock's own restatement or from the prior `REVISE BEFORE ACCEPTANCE` review's own conclusions. No new defect was found. No P0–P3 finding survives independent verification.

# Family F Model Role and Research Question Scope Lock — Correction Independent Constitutional Review

## 1. Independent evidence reviewed

Read fresh for this task, not inherited from the prior review:

```text
$ git status --short --branch
## main...origin/main
?? docs/architecture/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_FAMILY_F_MODEL_ROLE_AND_RESEARCH_QUESTION_SCOPE_LOCK.md
?? docs/decisions/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_BOUNDING_EVIDENCE_IMPLEMENTATION_AUTHORIZATION_DECISION.md
```

- The target Scope Lock, in full, as it currently stands (post-correction).
- The historical first ICR (`REVISE BEFORE ACCEPTANCE`) — read for what it found, not treated as authoritative for what the corrected document now says.
- `docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_FAMILY_F_ALTERNATIVE_MODEL_DIAGNOSTIC_PLANNING_REVIEW.md` — Sections 12, 18, and the full decision-register table, re-read fresh.
- `docs/architecture/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_EXPERIMENTAL_RECLASSIFICATION_AND_QUALIFICATION_BOUNDARY_SCOPE_LOCK.md` — Sections 7–8, re-read fresh.
- `docs/architecture/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_CAPTURE_PROXY_RESPONSE_REQUEST_SIZE_AND_DEDICATED_RUNTIME_GROWTH_BOUNDING_SCOPE_LOCK.md` — Section 4 ("Frozen campaign invariants"), re-read fresh.
- `docs/implementation/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_BOUNDING_EVIDENCE_ACQUISITION_AND_OFFLINE_ESTIMATOR_PLAN.md` — Section 4 ("Frozen inputs and statuses"), re-read fresh.
- `docs/decisions/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_BOUNDING_EVIDENCE_IMPLEMENTATION_AUTHORIZATION_DECISION.md` — Section 1's own controlling-authority statement and its `CONTROL_MODEL=` line, re-read fresh.
- `docs/implementation/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_DIAGNOSTIC_IMPLEMENTATION_EXECUTION_PLAN.md` and `docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3C_CONTROLLED_REMEDY_EXPERIMENTS_IMPLEMENTATION_PLAN_INDEPENDENT_CONSTITUTIONAL_REVIEW.md` — checked as candidate additional dependencies (see Section 4 below).
- A broad, independent regex sweep across every RPLMC/Family F document (`docs/architecture`, `docs/implementation`, `docs/decisions`, `docs/reviews`) for `SUBJECT_MODEL=|CONTROL_MODEL=|diagnostic subject:|comparison control:|proposed.{0,20}subject|proposed.{0,20}control` — 15 files matched; each was individually attributed (Section 4 below).
- `docs/architecture/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_MODEL_IDENTITY_PREMISE_DEFECT_CONFIRMATION_REVIEW.md`'s parent — the accepted commit `083b94dbd25c4fe74ed4e6310b8646c07a8cb00a` — re-confirmed as `HEAD`'s ancestor.

## 2. Findings A–D (model identity, research question, historical evidence, terminology)

These sections of the Scope Lock were **not** touched by the bounded correction and were already found sound by the prior ICR; this review independently re-confirms them rather than assuming that finding still holds.

**A. Model identity.** `DEPLOYED_BASELINE = qwen2.5-coder:7b` remains independently supported: `docker-compose.yml`'s `PARKER_MODEL_NAME` default has been fixed to `qwen2.5-coder:7b` since commit `6fd57dc` (2026-08-03T02:31:07Z), six days before RPLMC's own first commit (`b34f8d0`, 2026-08-09T15:57:10+12:00) — re-verified by direct re-derivation from `git log`, not inherited. `CONTROL_MODEL = qwen2.5-coder:7b` is a coherent policy choice, not an independently falsifiable fact, and no cited governance contradicts it. `SUBJECT_MODEL = llama3.2:3b` is independently confirmed not a fresh nomination: the already-accepted Family F Alternative-Model Diagnostic Planning Review (Section 12, re-read fresh) already placed this exact model into Family F's evidentiary frame — "because it produced the accepted Attempt 2 operational evidence and is the current comparison point in DQ4" — before this Scope Lock existed. Section B's disclaimers (not preferred, not selected, not approved, not production-track) are consistent with, and do not overstate, this history.

**B. Research question.** Independently re-read against Unit 3-B's own original Family F classification (model/provider substitution, one candidate remedy family, not the remedy programme itself) and Unit 3-A §11's qualification-tier requirement. The frozen question stays within that scope: it compares `llama3.2:3b` against the actual deployed `qwen2.5-coder:7b`; it is phrased as a screening question ("does... show enough reproducible improvement... to justify a future, separately governed full qualification campaign"); and Section D's six explicit disclaimers (no remedy selection, no replacement authorization, no outcome assumption, no evidentiary-tier inflation, no proof-of-impossibility-on-failure, no automatic-adoption-on-success) are independently confirmed present, unweakened, and internally consistent with the question's own text.

**C. Historical evidence.** Independently re-checked: the Scope Lock does not restate, recompute, or reinterpret any specific historical figure (fixture counts, `DQ4`'s outcome, Attempt 2's captured tag, RAM readings). Section C's boundary language is generic and does not touch the underlying numbers. No historical document was found modified anywhere in the working tree (`git status` shows only the target Scope Lock, this review's own new file, and the untouched pending Decision as untracked — nothing else).

**D. CONTROL_MODEL/SUBJECT_MODEL semantics.** Independently confirmed a factual/experimental-role correction, not disguised selection: Section B's disclaimer list and Section I.1/I.6's restated safeguards are consistent throughout, and the frozen definitions are policy-neutral about outcome. Retaining the vocabulary remains consistent with existing machinery: the same two role slots are used, unmodified, in the Capture-Proxy Bounding Scope Lock's and Bounding Evidence Acquisition Plan's own frozen tables (re-read fresh, Section 4 of this review below) — nothing about those tables' own arithmetic depends on which physical model occupies which slot.

## 3. Section G dependency chain — independently re-derived

Not compared against the prior ICR's own list; re-derived from the five controlling documents' own text, read fresh:

```text
CONCEPTUAL ORIGIN (introduces this model pairing into Family F; not in the
pending Decision's own citation chain):
  Family F Alternative-Model Diagnostic Planning Review (Section 12, 18) --
  independently re-confirmed: "qwen2.5-coder:7b may be named as the proposed
  diagnostic subject... llama3.2:3b may be named as the proposed comparison
  control..."

ORIGINATING FROZEN TABLE (Section 8, first formal freeze as governance fact):
  Experimental Reclassification and Qualification-Boundary Scope Lock --
  independently re-confirmed, Section 8: "diagnostic subject: qwen2.5-coder:7b
  ... comparison control: llama3.2:3b."

FIRST RE-FREEZE (independently re-derived against the originating table,
per its own accepted ICR):
  Capture-Proxy Response/Request-Size and Dedicated-Runtime-Growth Bounding
  Scope Lock, Section 4 -- independently re-confirmed, fresh:
  "SUBJECT_MODEL=qwen2.5-coder:7b / CONTROL_MODEL=llama3.2:3b" under the
  heading "Frozen campaign invariants."

SECOND RE-FREEZE (independently checked against the first re-freeze, per
its own accepted ICR):
  Bounding Evidence Acquisition and Offline Estimator Plan, Section 4 --
  independently re-confirmed, fresh: the identical pair under the heading
  "Frozen inputs and statuses."

DIRECT CONTROLLING AUTHORITY OF THE PENDING DECISION (independently
re-confirmed from the Decision's own Section 1, fresh):
  "This decision must be read with the controlling Plan, the accepted
  Capture-Proxy Response/Request-Size and Dedicated-Runtime-Growth Bounding
  Scope Lock and its ICR..." -- naming the second and first re-freeze
  documents directly, and the originating/conceptual-origin documents only
  transitively.
```

This independently reproduces, without relying on it, the exact four-instrument chain the corrected Scope Lock's Section G now states.

## 4. Search for any additional controlling dependency (Question E, "do not merely compare Section G with the first ICR")

A broad, independent sweep (Section 1 above) found 15 files matching model-role-naming patterns. Each was individually attributed:

- **4 already accounted for** in Section G's corrected list (Planning Review, Reclassification Scope Lock, Capture-Proxy Bounding Scope Lock, Bounding Evidence Acquisition Plan).
- **The pending Decision itself** — the destination of the chain, not an upstream dependency; correctly held, not listed as requiring "correction before itself."
- **4 ICR companions** of the above (Planning Review ICR, Capture-Proxy Bounding Scope Lock ICR, Bounding Evidence Acquisition Plan ICR, and this Scope Lock's own two ICRs) — these accept/confirm their parent's frozen text; none independently originates new `SUBJECT_MODEL`/`CONTROL_MODEL` content of its own beyond what its parent already states.
- **`docs/architecture/..._MODEL_IDENTITY_PREMISE_DEFECT_CONFIRMATION_REVIEW.md` and its ICR** — the root defect-confirmation pair; matched only because the regex pattern is broad, not because either independently freezes a `SUBJECT_MODEL`/`CONTROL_MODEL` table.
- **`docs/implementation/..._DIAGNOSTIC_IMPLEMENTATION_EXECUTION_PLAN.md`** — independently re-checked, fresh: carries its own prose "subject:"/"control:" naming (already status-line-corrected in the earlier propagation pass) but is **not named anywhere in the pending Decision's own Section 1** — confirmed by a direct, fresh `grep` returning no match. It governs a structurally separate live-campaign-execution track, distinct from the offline estimator track the pending Decision actually authorizes. Correctly excluded.
- **`docs/reviews/..._UNIT_3C_CONTROLLED_REMEDY_EXPERIMENTS_IMPLEMENTATION_PLAN_INDEPENDENT_CONSTITUTIONAL_REVIEW.md`** — independently re-checked, fresh: this is a Unit 3-C (Families A/B/C) document; its match was a false positive on the phrase "common control," an unrelated experimental-design term, not a `CONTROL_MODEL` reference. Confirmed irrelevant.
- **`docs/reviews/..._UNIT_3BF_FAMILY_F_EXPERIMENTAL_RECLASSIFICATION_AND_QUALIFICATION_BOUNDARY_SCOPE_LOCK_INDEPENDENT_CONSTITUTIONAL_REVIEW.md`** — independently checked directly (not merely assumed): a targeted `grep` for the exact matched phrases inside this specific file returned no output — it does not itself restate `SUBJECT_MODEL`/`CONTROL_MODEL` content; it only reviews and accepts its parent Scope Lock's Section 8. Correctly not separately listed in Section G, exactly as the (uncorrected) original Section G also never listed it, for the same valid reason.

**No fifth controlling dependency was found.** The four-instrument list is independently confirmed complete.

## 5. Downstream consequences and silent-implementation check (Question F)

Independently re-verified, fresh, that none of the four named instruments — nor the pending Decision — has had its actual `SUBJECT_MODEL=`/`CONTROL_MODEL=` values swapped:

```text
Planning Review Section 12/18:                    still qwen=subject, llama=control (unswapped)
Reclassification Scope Lock Section 8:             still qwen=subject, llama=control (unswapped)
Capture-Proxy Bounding Scope Lock Section 4:        still SUBJECT_MODEL=qwen / CONTROL_MODEL=llama (unswapped)
Bounding Evidence Acquisition Plan Section 4:       still SUBJECT_MODEL=qwen / CONTROL_MODEL=llama (unswapped)
Pending Decision line 194:                          still CONTROL_MODEL=llama3.2:3b (unswapped)
```

No downstream correction has been silently performed. Only the target Scope Lock's own Section G text (plus two single-word cross-reference fixes in Sections H and J) changed; every downstream document remains exactly as it was.

## 6. Pending Implementation Authorization Decision (Question G)

```text
$ git status --short -- docs/decisions/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_BOUNDING_EVIDENCE_IMPLEMENTATION_AUTHORIZATION_DECISION.md
?? docs/decisions/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_BOUNDING_EVIDENCE_IMPLEMENTATION_AUTHORIZATION_DECISION.md
```

Independently confirmed: remains untracked, unedited (its own `CONTROL_MODEL=llama3.2:3b` line is unchanged), held. Its own Section 1 already states its own effectiveness is conditioned on its own future ICR, which this document does not authorize.

## 7. Lighthouse observation (Question H)

Independently re-read Section F: the observation is stated as five bare facts with no interpretive claim, explicitly excluded from every existing corpus (Family F, Attempts 1–2, Unit 2/Unit 2-D), explicitly not promoted to governed evidence, and explicitly granted no execution authority. Confirmed capable, in principle, only of illustrating that the corrected research question addresses the same general defect class — not treated as evidence, admission, or trigger for anything.

## 8. Constitutional boundaries (Question I)

Independently checked the full corrected document, plus a fresh `grep -n "AUTHORIZ"` sweep (one hit: the pending Decision's own filename, referenced by name only, not an affirmative grant). None of the following is authorized anywhere in the corrected text: remedy selection, new candidate-model selection, live model call, Family F execution, qualification, production implementation, parser change, REMEMBER-keyword interception, persistence change, QMD change, UI change, Family C modification, historical-evidence modification, or any bypass of an existing downstream gate (Unit 3-A qualification tier, the Reclassification Scope Lock's own advancement gate, a future Readiness Review, Explicit Execution Approval).

## 9. Correction integrity (Question J)

Independently compared the corrected document's current Section G against the defect the historical first ICR identified: the correction is scoped exactly to that defect — the "exactly two instruments" claim and the inaccurate "drawn directly from them" causal statement — plus two single-word cross-reference fixes in Sections H ("the two instruments" → "the instruments") and J ("the two instruments" → "any instrument") that are strictly necessary consequences of Section G no longer naming exactly two. Sections A–F, H (apart from that one word), I, and the STOP block are confirmed unchanged from the version the first ICR reviewed. The frozen role values (`DEPLOYED_BASELINE`/`CONTROL_MODEL`/`SUBJECT_MODEL`), role definitions, historical-evidence boundary, research question, diagnostic-only boundary, lighthouse boundary, and constitutional safeguards are byte-identical to what the first ICR already found sound. This is not a redesign of Family F.

## 10. Verdict

```text
ACCEPTED
```

The Section G defect identified by the historical first ICR is cured: the corrected dependency chain (Planning Review as conceptual origin; Reclassification Scope Lock as originating frozen table; Capture-Proxy Bounding Scope Lock and Bounding Evidence Acquisition Plan as successive re-freezes; the pending Decision's own actual, correctly-identified controlling authority) is independently reproduced from primary sources in this review without relying on the corrected document's own restatement. No new defect was found in Section G or in any other section. No downstream correction has been silently performed. The pending Decision remains correctly held.

## 11. What becomes established by this acceptance

```text
DEPLOYED_BASELINE = qwen2.5-coder:7b
CONTROL_MODEL     = qwen2.5-coder:7b
SUBJECT_MODEL     = llama3.2:3b
```

become frozen governance facts. The Family F research question frozen in Section E of the target Scope Lock becomes the authoritative downstream basis for any future Family F governance act. The four-instrument correction sequence in Section G becomes the authorized, complete list of instruments requiring bounded superseding amendment before the pending Implementation Authorization Decision may proceed to its own Independent Constitutional Review.

## 12. What remains NOT AUTHORIZED

```text
REMEDY_SELECTED = NO
CANDIDATE_MODEL_ADOPTED = NO
NEW_CANDIDATE_MODEL_SELECTION = NO
LIVE_MODEL_CALL = NO
FAMILY_F_EXECUTION = NO
QUALIFICATION = NO
PRODUCTION_IMPLEMENTATION = NO
PARSER_CHANGE = NO
PERSISTENCE_CHANGE = NO
QMD_CHANGE = NO
UI_CHANGE = NO
FAMILY_C_MODIFICATION = NO
HISTORICAL_EVIDENCE_MODIFICATION = NO
DOWNSTREAM_BOUNDED_AMENDMENTS_PERFORMED = NO -- may now be prepared, per this
  acceptance, but were not performed by this review
PENDING_DECISION_ICR = NO -- remains held until the four bounded amendments
  identified in Section G are themselves accepted
```

## 13. Exact next lawful action

```text
NEXT_LAWFUL_ACTION = Prepare the four bounded superseding amendments identified
  in Section G of the now-accepted Scope Lock (Family F Alternative-Model
  Diagnostic Planning Review; Experimental Reclassification and
  Qualification-Boundary Scope Lock; Capture-Proxy Response/Request-Size and
  Dedicated-Runtime-Growth Bounding Scope Lock; Bounding Evidence Acquisition
  and Offline Estimator Plan) -- each its own separately reviewed task, each
  requiring its own Independent Constitutional Review before the next.
```

Not performed by this review.

## 14. Verification performed for this review

```text
$ git status --short --branch
## main...origin/main
?? docs/architecture/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_FAMILY_F_MODEL_ROLE_AND_RESEARCH_QUESTION_SCOPE_LOCK.md
?? docs/decisions/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_BOUNDING_EVIDENCE_IMPLEMENTATION_AUTHORIZATION_DECISION.md
?? docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_FAMILY_F_MODEL_ROLE_AND_RESEARCH_QUESTION_SCOPE_LOCK_INDEPENDENT_CONSTITUTIONAL_REVIEW.md

$ git diff --stat
(no output -- no tracked file modified)
```

(This review's own new file will appear as a fourth untracked entry once written; nothing else in the tree changes as a result of writing it.)

No model process, campaign, experiment, or implementation action occurred before or during this review. No Gradle build was run. No Ollama server was contacted. No Parker server runtime was touched. The target Scope Lock was not edited by this review. The historical first ICR was not edited. No downstream Family F document was edited. The pending Implementation Authorization Decision was not edited.

## 15. STOP conditions confirmed

```text
NO production code, test, Docker, persistence, QMD, UI, parser, or model
  configuration file touched.
NO model called.
NO edit to the target Scope Lock.
NO edit to the historical first ICR.
NO edit to any downstream Family F governance document.
NO edit to the pending Implementation Authorization Decision.
NO downstream correction implemented.
NO document staged, committed, or pushed.
```
