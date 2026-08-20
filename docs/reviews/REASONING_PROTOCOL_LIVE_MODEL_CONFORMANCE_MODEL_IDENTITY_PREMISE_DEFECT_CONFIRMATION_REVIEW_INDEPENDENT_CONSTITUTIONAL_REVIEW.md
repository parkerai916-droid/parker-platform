**Status:** Independent Constitutional Review of the Reasoning Protocol Live-Model Conformance Model-Identity Premise Defect Confirmation Review — **ACCEPTED, with one non-blocking qualification.** Every material proposition required for the target review's verdict was independently re-derived from primary repository history and current working-tree state, fresh, rather than accepted from the target document's own restatement. One genuine but non-blocking completeness gap was found in the target document's own affected-document list (Section 3 below); it does not falsify the confirmed defect, does not change the hold on the pending Implementation Authorization Decision, and does not require the target document to be rewritten. No historical document was edited. The pending Implementation Authorization Decision was not edited and did not receive its own ICR. No model was called. Nothing was staged, committed, or pushed.

# Model-Identity Premise Defect Confirmation Review — Independent Constitutional Review

## 1. Independent evidence checked

Every command below was run fresh in this session, against the live repository, not copied from the target document's own text:

```text
$ git show -s --format="%H %cI %s" 6fd57dcb54d3eb2e06b0504eb3441dbfded6cea7
6fd57dcb54d3eb2e06b0504eb3441dbfded6cea7 2026-08-03T02:31:07Z feat: add runnable interactive Parker runtime

$ git log -p --follow -- docker-compose.yml | grep "PARKER_MODEL_NAME\|PARKER_MODEL_ENDPOINT_URL"
+      PARKER_MODEL_ENDPOINT_URL: "http://host.docker.internal:11434/api/generate"
+      PARKER_MODEL_NAME: "${OLLAMA_MODEL:-qwen2.5-coder:7b}"
(exactly one occurrence across the file's full history, at commit 6fd57dc; three later
commits touching docker-compose.yml — 94fcc05, c837479, 728b2d2 — do not touch this line)

$ git log -p --follow -- .env.example | grep "OLLAMA_MODEL"
+OLLAMA_MODEL=qwen2.5-coder:7b
(exactly one value, ever, across both commits touching this file: 6fd57dc, 1a0333d)

$ git show -s --format="%H %cI" b34f8d0cadf4b8ee45db0b1c351a79f1d530162d
b34f8d0cadf4b8ee45db0b1c351a79f1d530162d 2026-08-09T15:57:10+12:00

$ git cat-file -t 7ad3afa01f6ac3e3d76c4af5e2107b70bfc0855c
commit
$ git merge-base --is-ancestor 7ad3afa01f6ac3e3d76c4af5e2107b70bfc0855c HEAD
(exit 0 — is ancestor of HEAD)

$ git cat-file -t 642cba214bcf207e72e855694d9c2dd35f8d31cf
commit

$ grep -n "model:\|endpoint\|commit:" docs/reviews/KNOWLEDGE_DISCOVERABILITY_REASONING_CONTEXT_LIVE_VERIFICATION_ATTEMPTS_1_2_REVIEW.md
model:  llama3.2:3b  /  endpoint: http://127.0.0.1:11500/api/generate  (Attempt 1)
model:  llama3.2:3b  /  endpoint: http://127.0.0.1:11501/api/generate  (Attempt 2)

$ grep -m1 "^\*\*Status" docs/reviews/KNOWLEDGE_DISCOVERABILITY_REASONING_CONTEXT_LIVE_VERIFICATION_ATTEMPTS_1_2_INDEPENDENT_REVIEW.md
"...accepts the record as evidence-faithful..."

$ grep -n "Corrected in place against" docs/architecture/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_ALTERNATIVE_DIAGNOSTIC_HOST_REQUIREMENTS_SCOPE_LOCK.md
(quote independently re-derived, matches the target document's own citation verbatim)

$ grep -rl "llama3.2" docs/architecture/REASONING_PROTOCOL* docs/reviews/REASONING_PROTOCOL* \
    docs/implementation/REASONING_PROTOCOL* docs/decisions/REASONING_PROTOCOL* | sort
(34 files — independently enumerated, compared against the target document's own Section 5
table, see Section 3 below)

$ docker compose config 2>&1 | grep PARKER_MODEL_NAME
      PARKER_MODEL_NAME: qwen2.5-coder:7b
$ cat .env | grep OLLAMA_MODEL
OLLAMA_MODEL=qwen2.5-coder:7b
$ docker inspect parker-runtime --format '{{range .Config.Env}}{{println .}}{{end}}' | grep MODEL_NAME
PARKER_MODEL_NAME=qwen2.5-coder:7b

$ git status --short --branch
## main...origin/main
?? docs/decisions/..._IMPLEMENTATION_AUTHORIZATION_DECISION.md
?? docs/reviews/..._MODEL_IDENTITY_PREMISE_DEFECT_CONFIRMATION_REVIEW.md
```

## 2. Findings against each material proposition

| # | Proposition | Independent finding |
|---|---|---|
| 1 | `docker-compose.yml` introduced and has always defaulted to `qwen2.5-coder:7b` | **Confirmed.** One occurrence in the file's entire history, at `6fd57dc` (2026-08-03T02:31:07Z), never subsequently touched. |
| 2 | `.env.example` has recommended `qwen2.5-coder:7b` since its first commit | **Confirmed.** One value, ever, across both commits touching the file. |
| 3 | RPLMC began after that deployment default was set | **Confirmed.** `6fd57dc` (2026-08-03T02:31:07Z) precedes `b34f8d0` (2026-08-09T15:57:10+12:00) by 6 days, ~15 hours. |
| 4 | Attempts 1–2 recorded `llama3.2:3b` via `127.0.0.1:11500`/`:11501` | **Confirmed**, exact text match against the target document's own citation. |
| 5 | Those endpoints could not have been the deployed Docker endpoint | **Confirmed.** `PARKER_MODEL_ENDPOINT_URL` in `docker-compose.yml` is a fixed literal, `"http://host.docker.internal:11434/api/generate"`, with no `${VAR}` substitution — structurally non-overridable by any `.env` or shell value, at every point in its committed history. |
| 6 | Attempts 1–2's own historical records are accurate and properly excluded from correction | **Confirmed.** The record's own accepted Independent Review states it "accepts the record as evidence-faithful." Nothing in the target document, or in this independent check, disputes that record's own self-contained claims (model, endpoint, captured tags, RAM figures). The defect is entirely in later documents' *characterization* of what that record represents, never in the record itself. |
| 7 | Downstream Reopening/Family F governance converted Attempts 1–2's diagnostic identity into "current/live/production model" | **Confirmed**, by direct citation re-check: Reopening Decision line 50 ("the current, unmodified `llama3.2:3b` configuration"); Reopening Assessment lines 109/114/139 (same phrase, repeated); Family F Planning Review line 87 ("the current live model"). All independently re-read fresh, quotes match the target document's citations exactly. |
| 8 | `qwen2.5-coder:7b` was correspondingly characterized as merely an alternative/proposed diagnostic model | **Confirmed** — every occurrence independently checked (Planning Review, Diagnostic Host Requirements Scope Lock, Capture-Proxy Bounding Scope Lock, Experimental Reclassification Scope Lock, Bounding Plan, pending Decision) uses "proposed diagnostic subject" / "SUBJECT_MODEL" language, never "already deployed." |
| 9–11 | Completeness/correctness of the affected-document list | **Partially confirmed — see Section 3, non-blocking gap identified.** |
| 12 | Correction mechanism consistent with established precedent | **Confirmed** — the target document's citation of the raw-transport-capture correction precedent is verbatim accurate, and independently re-checked to be a Status-line addendum, not a body rewrite, in the actual `Diagnostic Host Requirements Scope Lock` file. |
| 13 | Holding the pending Decision is constitutionally required | **Confirmed as reasoned, not merely asserted** — see Section 6. |
| 14 | Target review avoids deciding CONTROL_MODEL/SUBJECT_MODEL, model replacement, live execution, lighthouse status | **Confirmed** — Section 7 and the disposition block (`CONTROL_SUBJECT_ROLES_DECIDED=NO`, `LIVE_CALL_AUTHORIZED=NO`, `LIGHTHOUSE_OBSERVATION_POOLED=NO`) independently re-read, internally consistent with the rest of the document's text; no sentence elsewhere in the document contradicts these. |
| 15 | Lighthouse treatment properly bounded | **Confirmed** — recorded as bare fact only (model, execution path, input, outcome, persistence result), explicitly and repeatedly stated not to be pooled into any existing dataset, and explicitly stated to authorize nothing by itself. |

## 3. Affected-document completeness assessment — non-blocking gap found

Independently enumerating every RPLMC document mentioning `llama3.2` (34 files, full list re-derived fresh in Section 1) and comparing against the target document's own Section 5 table surfaces a real, internally-inconsistent gap.

The target document's Section 5 already includes several documents whose cited text is a **bare `CONTROL_MODEL`/"control identity" label**, with no independent restatement of "current" anywhere in the cited line — e.g. Diagnostic Host Requirements Scope Lock ("control identity: llama3.2:3b — no substitution, regardless of host"), Capture-Proxy Bounding Scope Lock (`CONTROL_MODEL=llama3.2:3b`), Experimental Reclassification Scope Lock ("comparison control: `llama3.2:3b`"), the Bounding Plan and the pending Decision (`CONTROL_MODEL=llama3.2:3b`).

Applying that same, actually-used inclusion criterion consistently, the following documents — independently confirmed to use `llama3.2:3b` as the identical frozen "control"/"comparison" identity, in the identical structural sense — were **not** listed in Section 5 but arguably should have been, for consistency with the criterion the target document itself applies to the documents it did include:

- `docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_DIAGNOSTIC_READINESS_REVIEW.md`
- `docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_DIAGNOSTIC_READINESS_BLOCKER_RESOLUTION_PLANNING_REVIEW.md` (+ its ICR)
- `docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_PARKER_CANDIDATE_HOST_ASSESSMENT_REVIEW.md`
- `docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_RESPONSE_RUNTIME_AND_PARKER_HOST_ISOLATION_PLANNING_REVIEW.md`
- `docs/implementation/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_DIAGNOSTIC_IMPLEMENTATION_EXECUTION_PLAN.md` (+ its ICR)

Each of these was independently checked for an explicit "current"-type claim (`current live model`, `current, unmodified`, `current production`) near any `llama3.2` mention — none was found; each uses `llama3.2:3b` purely as an already-frozen input value inherited from upstream governance, never independently asserting or re-deriving "current production" itself. This is precisely why they are lower-severity than the programme-level documents (Reopening Assessment/Decision, Planning Review) that originated the claim — but it is also precisely why, under the target document's own stated criterion (documents that "inherited" the identity, not only documents that independently restate "current"), they belong in the same list as the other bare-label documents already included.

**This is non-blocking, not falsifying:**
- It does not affect the confirmed defect itself (Section 2, propositions 1–8, all independently confirmed).
- It does not affect the hold on the pending Implementation Authorization Decision (Section 6 below) — that hold is grounded in the Decision's own `CONTROL_MODEL=llama3.2:3b` line, correctly identified regardless of this list's completeness elsewhere.
- The stated correction mechanism (Section 8 of the target document: a Status-line addendum applied "at its own next governed touch") is inherently incremental and self-healing — it does not depend on Section 5 being a closed, exhaustive enumeration to be applied correctly to a document later found to need it.

**Disposition of this finding:** the target document's Section 5 table should be treated as **representative, not exhaustive**, when read by any future task performing the downstream correction sequence. This is recorded here as a non-blocking qualification rather than grounds for `REVISE BEFORE ACCEPTANCE`, mirroring this programme's own established precedent for a real-but-non-falsifying completeness gap (e.g. the Programme Disposition Closure Review's own Independent Constitutional Review, Challenge 7: "non-blocking gap noted... not a falsifying defect, since the substantive protection exists elsewhere").

## 4. Constitutional-boundary assessment

**A — Preserves historical truth rather than rewriting it.** Confirmed. Section 4 of the target document explicitly excludes Attempts 1–2, Unit 2, and Unit 2-D from correction; independently re-verified those records are in fact accurate and untouched by this review or the target document.

**B — Distinguishes diagnostic evidence from deployed-production identity.** Confirmed. Section 3 of the target document's own five-way fact classification (historical / deployed-production / diagnostic configuration / inference / unknown) is independently reproducible from the same primary sources checked in Section 1 above; the inference/unknown categories are honestly bounded (the exact Attempts 1–2 launch mechanism is correctly labeled inference, not fact; `.env`'s pre-existing history is correctly labeled unknown, not assumed).

**C — Preserves provenance boundaries.** Confirmed. The lighthouse observation is recorded as its own, separately-provenanced fact, explicitly distinguished from Attempts 1–2 (different model, different execution path) and from every existing corpus.

**D — Avoids retroactive pooling of evidence.** Confirmed. `LIGHTHOUSE_OBSERVATION_POOLED=NO` is asserted and independently consistent with the surrounding text; no sentence anywhere merges the lighthouse observation into an existing dataset.

**E — Avoids granting implementation or live-model authority.** Confirmed. `LIVE_CALL_AUTHORIZED=NO`, `REMEDY_SELECTED=NO`, `MODEL_SELECTED=NO` all independently consistent with the document's own text; nothing in the target document authorizes any model contact.

**F — Keeps the correction bounded to the demonstrated factual-premise defect.** Confirmed. Section 6 of the target document explicitly states `CAPTURED_EXPERIMENTAL_EVIDENCE_AFFECTED=NO`, `PARKER_DEPLOYMENT_AFFECTED=NO`, `PERSISTENCE_AFFECTED=NO`, `QMD_AFFECTED=NO`, `UI_AFFECTED=NO`, `PARSER_AFFECTED=NO`, `MODEL_IMPLEMENTATION_AFFECTED=NO` — each independently verified true by this review's own reading of the target document and by this review's own confirmation that no `src/`, test, Docker, or configuration file was touched by the drafting task (`git diff --stat` for those paths is empty, per Section 8 below).

**G — Preserves all existing RPLMC hard boundaries not directly implicated.** Confirmed. No Knowledge Discoverability Attempt 3 claim; no Family C work touched; no QMD reference beyond the target document's own explicit "not affected" statement.

**H — Correctly identifies the lawful next action if accepted.** Confirmed, and independently agreed with: an Independent Constitutional Review of the target document (this document) is the correct next lawful action, and the target document correctly states that its own downstream correction sequence (Section 13/10 of the target document) may not proceed until this ICR is itself accepted.

## 5. Correction-mechanism assessment

Independently confirmed consistent with established repository precedent. The `UNIT_3BF_FAMILY_F_DIAGNOSTIC_RAW_TRANSPORT_CAPTURE_DEFECT_CONFIRMATION_REVIEW` → correction → its own ICR → downstream Status-line addendum on the `Diagnostic Host Requirements Scope Lock` is structurally identical to what the target document proposes here, and the quoted precedent text is verbatim accurate. No new governance mechanism is invented; none is required.

## 6. Pending Implementation Authorization Decision — hold assessment

Independently re-confirmed required, not merely asserted. The Decision's own text (independently re-read: `docs/decisions/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_BOUNDING_EVIDENCE_IMPLEMENTATION_AUTHORIZATION_DECISION.md`, line 194, `CONTROL_MODEL=llama3.2:3b`) already states its own `NEXT_LAWFUL_ACTION=Independent Constitutional Review of this decision`, and states that authorization is not effective merely by being drafted. Proceeding to that ICR before the confirmed premise defect is itself resolved would risk exactly the failure mode this repository's own ICR discipline exists to catch — an ICR silently re-certifying an inherited, unexamined false premise. Holding is the correct, minimal, already-self-consistent outcome; nothing about it goes beyond what the Decision's own text already requires before it can be effective.

## 7. Lighthouse-provenance assessment

Independently confirmed properly bounded. The observation is recorded as five bare facts (model, execution path, exact input, observed outcome, persistence result) with no interpretive claim attached, explicitly stated not to be pooled into any existing dataset, and explicitly stated to authorize nothing. This does not convert it into governed evidence — it remains exactly what Section 10 of the target document calls it: an operational observation available only as a possible future reopening-trigger input under its own, separately-justified governance act.

## 8. Verification performed for this review

```text
$ git status --short --branch
## main...origin/main
?? docs/decisions/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_BOUNDING_EVIDENCE_IMPLEMENTATION_AUTHORIZATION_DECISION.md
?? docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_MODEL_IDENTITY_PREMISE_DEFECT_CONFIRMATION_REVIEW.md
?? docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_MODEL_IDENTITY_PREMISE_DEFECT_CONFIRMATION_REVIEW_INDEPENDENT_CONSTITUTIONAL_REVIEW.md
(the pending Decision remains exactly as it was: untouched, uncommitted)

$ git diff --stat
(no output — no tracked file modified)

$ git diff --stat -- src/ tests/ build.gradle.kts settings.gradle.kts docker-compose.yml Dockerfile .env.example
(no output — no production, test, build, or deployment configuration file touched)
```

No model process, campaign, experiment, or implementation action occurred before or during this review. No Gradle build was run. No Ollama server was contacted. No Parker server runtime was touched. The target Defect Confirmation Review was not edited by this review. Neither historical Attempts 1–2 document was edited. The pending Implementation Authorization Decision was not edited and did not receive its own ICR.

## 9. Verdict

```text
ACCEPTED, WITH ONE NON-BLOCKING QUALIFICATION
```

Every material proposition required for the target review's core verdict (a factual-premise defect exists, is correctly bounded to governance prose, and is correctly excluded from touching captured evidence, deployment, persistence, QMD, UI, parser, or model implementation) is independently re-derived and confirmed true. The one qualification (Section 3: the affected-document list is representative, not exhaustive — five additional documents independently identified as using the same inherited `llama3.2:3b` control-identity label under the target document's own applied criterion) does not falsify the defect finding, does not weaken the hold on the pending Implementation Authorization Decision, and does not require the target document to be rewritten — it is recorded here as guidance for whichever future task performs the downstream correction sequence.

## 10. What becomes established by acceptance, and what remains NOT AUTHORIZED

**Established by this acceptance:**
```text
DEFECT_CONFIRMED = YES (independently re-verified)
TARGET_REVIEW_STATUS = ACCEPTED
NEXT_LAWFUL_ACTION = Governed superseding-reference addition to each affected document
                      (Section 5 of the target document, read as representative per
                      Section 3 of this review), at each document's own next governed touch;
                      then a separate governance act resolving CONTROL_MODEL/SUBJECT_MODEL
                      roles and restating the Family F research question.
PENDING_IMPLEMENTATION_AUTHORIZATION_DECISION = REMAINS HELD, its own ICR still not authorized
```

**Remains NOT AUTHORIZED by this acceptance:**
```text
MODEL_CALL = NO
MODEL_REPLACEMENT = NO
CONTROL_MODEL/SUBJECT_MODEL_ROLE_DECISION = NO
FAMILY_F_LIVE_EXECUTION = NO
KNOWLEDGE_DISCOVERABILITY_ATTEMPT_3 = NO
LIGHTHOUSE_OBSERVATION_AS_GOVERNED_EVIDENCE = NO
IMPLEMENTATION_AUTHORIZATION_DECISION_ICR = NO
REWRITE_OF_ANY_HISTORICAL_DOCUMENT_BODY = NO
```

## 11. STOP conditions confirmed

```text
NO production code, test, Docker, configuration, persistence, QMD, UI, parser,
  or runtime file touched.
NO model called.
NO historical governance document edited.
NO edit made to the pending Implementation Authorization Decision.
NO Independent Constitutional Review performed on the pending Decision.
NO document staged, committed, or pushed.
```
