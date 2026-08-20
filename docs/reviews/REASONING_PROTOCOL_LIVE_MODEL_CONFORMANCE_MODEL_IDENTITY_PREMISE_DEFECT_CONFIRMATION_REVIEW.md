**Status:** Reasoning Protocol Live-Model Conformance — Model-Identity Premise Defect Confirmation Review — **DEFECT CONFIRMED.** Standalone, programme-level governance document — peer to the Reopening Decision and the Programme Disposition Closure Review, not a new "Unit 3-F" and not scoped to Unit 3-BF alone, since the defect confirmed here spans programme-level documents (the Reopening Assessment, the Reopening Decision) as well as Unit 3-BF Family F documents. Drafted against working baseline `git status --short --branch` = `## main...origin/main` with exactly one pre-existing untracked file (the pending, unmodified Family F Bounding Evidence Implementation Authorization Decision). This review confirms a factual-premise defect only: downstream Family F/Reopening governance inherited and repeated the inaccurate proposition that `llama3.2:3b` is Parker's current, deployed production model. It does not touch, question, or reopen any captured experimental evidence, any Parker deployment configuration, persistence, QMD, the UI, the reasoning parser, or any model implementation. It selects no remedy, ranks no model, decides no CONTROL_MODEL/SUBJECT_MODEL role, and authorizes no live call, campaign, or Knowledge Discoverability Attempt 3.

# Reasoning Protocol Live-Model Conformance — Model-Identity Premise Defect Confirmation Review

## 1. Purpose and origin

This review implements the correction mechanism identified by the accepted Model-Identity Factual Correction Planning Review: a standalone Defect Confirmation Review, of the same kind and structure already established in this programme's own history (`UNIT_2_OLLAMA_IDENTITY_EXTRACTION_DEFECT_CONFIRMATION_REVIEW.md`; `UNIT_3BF_FAMILY_F_DIAGNOSTIC_RAW_TRANSPORT_CAPTURE_DEFECT_CONFIRMATION_REVIEW.md`), each followed by its own Independent Constitutional Review and, where required, a downstream Status-line correction reference on affected documents — never a rewrite of their frozen bodies.

## 2. The exact defect (Requirement A)

**Downstream Family F/Reopening governance inherited an inaccurate model-identity premise: that `llama3.2:3b` is Parker's current, deployed, production model, and that `qwen2.5-coder:7b` is merely a proposed alternative distinct from production.** Both halves of that premise are false. `qwen2.5-coder:7b` is, and has continuously been, Parker's actual deployed default model. `llama3.2:3b` was the model configured for two specific diagnostic invocations that did not run through Parker's deployed Docker composition at all.

## 3. Fact classification (Requirement B)

Each claim below is labeled by evidentiary kind, per the governing task's own instruction not to conflate them.

### 3.1 Historical fact (proven from committed git history, re-verified fresh for this review)

```text
$ git show -s --format="%H %cI %s" 6fd57dcb54d3eb2e06b0504eb3441dbfded6cea7
6fd57dcb54d3eb2e06b0504eb3441dbfded6cea7 2026-08-03T02:31:07Z feat: add runnable interactive Parker runtime

$ git log -p --follow -- docker-compose.yml | grep "PARKER_MODEL_NAME\|PARKER_MODEL_ENDPOINT_URL"
+      PARKER_MODEL_ENDPOINT_URL: "http://host.docker.internal:11434/api/generate"
+      PARKER_MODEL_NAME: "${OLLAMA_MODEL:-qwen2.5-coder:7b}"
(appears exactly once across the file's entire history — introduced at 6fd57dc, never
subsequently touched by any of the three later commits that modified docker-compose.yml:
94fcc05 [2026-08-07], c837479 [2026-08-16], 728b2d2 [2026-08-20])

$ git log -p --follow -- .env.example | grep "OLLAMA_MODEL"
+OLLAMA_MODEL=qwen2.5-coder:7b
(exactly one historical value, ever, across two commits: 6fd57dc, 1a0333d)

$ git show -s --format="%H %cI %s" b34f8d0cadf4b8ee45db0b1c351a79f1d530162d
b34f8d0cadf4b8ee45db0b1c351a79f1d530162d 2026-08-09T15:57:10+12:00 feat: add reasoning protocol live-model evaluation harness
```

Therefore: `docker-compose.yml`'s `PARKER_MODEL_NAME` default of `qwen2.5-coder:7b` was introduced 2026-08-03T02:31:07Z and has never changed. RPLMC's own first commit (Unit 1) postdates that introduction by 6 days, 13 hours. This deployment default is not a Family F artifact, not an RPLMC artifact, and predates the programme entirely.

`PARKER_MODEL_ENDPOINT_URL` in the same file is a fixed literal, `"http://host.docker.internal:11434/api/generate"` — no `${VAR}` substitution of any kind. It has never been parameterized at any point in its committed history. This value cannot be overridden by `.env`, by the shell, or by any docker-compose invocation.

```text
$ grep "commit:\|model:\|endpoint" docs/reviews/KNOWLEDGE_DISCOVERABILITY_REASONING_CONTEXT_LIVE_VERIFICATION_ATTEMPTS_1_2_REVIEW.md
commit:            7ad3afa01f6ac3e3d76c4af5e2107b70bfc0855c
model:              llama3.2:3b
model endpoint (Parker-configured): http://127.0.0.1:11500/api/generate
commit:            7ad3afa01f6ac3e3d76c4af5e2107b70bfc0855c
model:              llama3.2:3b
model endpoint (Parker-configured): http://127.0.0.1:11501/api/generate
```

Both Attempts 1 and 2 recorded a `model endpoint (Parker-configured)` of `127.0.0.1:11500`/`:11501` — addresses structurally distinct from, and unreachable through, `docker-compose.yml`'s fixed `host.docker.internal:11434` literal. This is a direct textual comparison between two committed documents, not an inference about intent.

### 3.2 Deployed-production fact (current state, re-observed directly, not git history)

```text
$ docker compose config 2>&1 | grep PARKER_MODEL_NAME
      PARKER_MODEL_NAME: qwen2.5-coder:7b

$ cat .env | grep OLLAMA_MODEL
OLLAMA_MODEL=qwen2.5-coder:7b

$ docker inspect parker-runtime --format '{{range .Config.Env}}{{println .}}{{end}}' | grep MODEL_NAME
PARKER_MODEL_NAME=qwen2.5-coder:7b
```

The currently deployed (though presently stopped) `parker-runtime` container, and the `docker compose config` rendering of the current committed `docker-compose.yml` against the current, gitignored `.env`, both resolve to `qwen2.5-coder:7b`. `.env` itself is listed in `.gitignore` and has no recoverable git history; the values above are observed present-state facts, not historical claims.

### 3.3 Diagnostic configuration (proven fact about what Attempts 1–2 used; not a claim about deployment)

`llama3.2:3b`, reached via a capture proxy at `127.0.0.1:11500`/`:11501` forwarding to a real local Ollama at `127.0.0.1:11434`, is the model and transport Knowledge Discoverability's Attempts 1 and 2 actually used. This is proven directly from that review's own committed text (Section 3.1 above) — it is not disputed or reopened by this review. What is corrected is only the *later inference*, made by other documents, that this diagnostic configuration represents "the current live model."

### 3.4 Inference (reasoned, not directly proven by a single document)

The exact mechanism by which Attempts 1–2 launched `ParkerRuntime` against a non-default endpoint and non-default model — most plausibly a bare host invocation of `Main.kt`/`bin/parker --interactive` (a locally-built `build/install/parker` distribution is present on this host) with `PARKER_MODEL_ENDPOINT_URL`/`PARKER_MODEL_NAME` exported directly in the invoking shell, since `ParkerRuntimeConfigLoader` reads `System.getenv()` directly and a bare host process is not constrained by `docker-compose.yml`'s fixed endpoint literal — is a reasoned reconstruction, not a proven fact. No committed document states the exact launch command used for Attempts 1–2. This review does not require that mechanism to be established to confirm the defect: the proven facts in 3.1 (endpoint literal is fixed and non-overridable) and 3.3 (a different endpoint was actually used) already suffice to prove Attempts 1–2 did not run through the deployed Docker composition, regardless of exactly how they were launched.

### 3.5 Unknown history (explicitly not knowable from committed evidence)

Whether `.env`'s real (gitignored) `OLLAMA_MODEL` value was ever set to anything other than `qwen2.5-coder:7b` at any point between 2026-08-03 and today is **not knowable from git** — `.env` is listed in `.gitignore` (`git log --all -- .env` returns no commits; `git ls-files .env` returns nothing). This review makes no claim about `.env`'s historical value at any specific past date. It relies only on `.env.example`'s committed recommendation (3.1, constant since first commit) and the current, directly-observed `.env` (3.2) — never on an assumed continuous history for the gitignored file in between.

## 4. Confirmed NOT defective — must remain untouched (Requirement C)

The following are independently confirmed accurate and are explicitly **out of scope for any correction**:

1. **Attempts 1–2's own recorded model identity** (`llama3.2:3b`) — accurate; this is what those attempts used.
2. **Attempts 1–2's own recorded capture-proxy endpoints** (`127.0.0.1:11500`, `127.0.0.1:11501`) — accurate.
3. **Attempts 1–2's own captured tags and raw evidence** (Attempt 1's tag unobserved; Attempt 2's two directly-captured `REPLY` selections; the RAM figures ~4.1 GiB → ~1.5 GiB; all associated file hashes) — accurate, independently re-verified by that review's own accepted Independent Review, itself unchallenged by this review.
4. **Attempts 1–2's own conclusions about `llama3.2:3b`'s behavior under that specific harness** — accurate as a statement about that harness; this review does not reinterpret, weaken, or extend what those two single-exposure trials do or do not establish about `llama3.2:3b` generally.
5. **Unit 2's own `qwen2.5-coder:7b` baseline evidence** (the `R01-direct` characterization, the frozen fixture matrix, the confusion-matrix findings) — accurate; Unit 2's own Scope Lock correctly and explicitly named `qwen2.5-coder:7b` as its sole authorized baseline subject, with no reference to production status one way or the other.
6. **Unit 2-D's DQ4 comparison** (`llama3.2:3b` and `qwen2.5-coder:7b`, one attempt each, both missed `R01-direct`) — accurate as a record of what was tested; the Unit 2-D Scope Lock's own disclosed confound (size and specialization differ simultaneously) remains correctly stated and is not disturbed.

None of the above documents require any edit, correction, or Status-line addendum. The defect identified in Section 2 lies entirely in how *other, later* documents characterized what these accurate records represent — never in the records themselves.

## 5. Documents that inherited the inaccurate premise (Requirement D)

Each citation below was re-read fresh from the working tree for this review, not copied from a prior summary.

| Document | Inaccurate premise, as stated |
|---|---|
| `docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_REOPENING_ASSESSMENT_FOR_KNOWLEDGE_DISCOVERABILITY.md` | Line 109, 114, 139: "the current, unmodified `llama3.2:3b` configuration" |
| `docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_REOPENING_ASSESSMENT_FOR_KNOWLEDGE_DISCOVERABILITY_INDEPENDENT_CONSTITUTIONAL_REVIEW.md` | Line 67: "the current `llama3.2:3b` configuration" (inherited without independent re-derivation against `docker-compose.yml`) |
| `docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_REOPENING_DECISION.md` | Line 50: "the current, unmodified `llama3.2:3b` configuration" |
| `docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_REOPENING_DECISION_INDEPENDENT_CONSTITUTIONAL_REVIEW.md` | Inherits the Decision's own premise without independent challenge |
| `docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_FAMILY_F_ALTERNATIVE_MODEL_DIAGNOSTIC_PLANNING_REVIEW.md` | Line 87: "the current live model"; line 193, 304: `llama3.2:3b` as "proposed comparison control" framed against that "current" model |
| `docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_FAMILY_F_ALTERNATIVE_MODEL_DIAGNOSTIC_PLANNING_REVIEW_INDEPENDENT_CONSTITUTIONAL_REVIEW.md` | Inherits the Planning Review's own premise without independent challenge |
| `docs/architecture/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_ALTERNATIVE_DIAGNOSTIC_HOST_REQUIREMENTS_SCOPE_LOCK.md` | Line 46: "control identity: llama3.2:3b" |
| ...and its own Independent Constitutional Review | Inherits without independent challenge |
| `docs/architecture/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_CAPTURE_PROXY_RESPONSE_REQUEST_SIZE_AND_DEDICATED_RUNTIME_GROWTH_BOUNDING_SCOPE_LOCK.md` | Line 62: `CONTROL_MODEL=llama3.2:3b` |
| ...and its own Independent Constitutional Review | Inherits without independent challenge |
| `docs/architecture/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_EXPERIMENTAL_RECLASSIFICATION_AND_QUALIFICATION_BOUNDARY_SCOPE_LOCK.md` | Line 109: "comparison control: `llama3.2:3b`" |
| `docs/implementation/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_BOUNDING_EVIDENCE_ACQUISITION_AND_OFFLINE_ESTIMATOR_PLAN.md` | Line 71: `CONTROL_MODEL=llama3.2:3b` |
| ...and its own Independent Constitutional Review | Inherits without independent challenge |
| `docs/decisions/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_BOUNDING_EVIDENCE_IMPLEMENTATION_AUTHORIZATION_DECISION.md` (pending, uncommitted) | Line 194: `CONTROL_MODEL=llama3.2:3b` |

No document in this list is being rewritten by this review. Section 8 below states the exact, narrower correction mechanism.

## 6. Scope confirmation — what this is not a defect in (Requirement E)

This is a factual-premise defect in prose governance text only. It is specifically confirmed **not** to be a defect in:

- **captured experimental evidence** — Attempts 1–2's own record, and Unit 2/Unit 2-D's own baseline findings, are accurate (Section 4);
- **Parker's deployment** — `docker-compose.yml`'s `qwen2.5-coder:7b` default is not itself a defect; it is, and has always been, the intended, working deployment default, unrelated to and unaffected by RPLMC;
- **persistence** — Memory Core / Knowledge Item durability are untouched and not implicated;
- **QMD** — wholly unrelated, not touched by this review;
- **UI** — wholly unrelated, not touched by this review;
- **the reasoning parser** — `TaggedReasoningResponseParser`, `DefaultReasoningPromptBuilder`, `ModelReasoningProvider` remain exactly as they were; this review does not allege any defect in them;
- **any model implementation** — neither `llama3.2:3b` nor `qwen2.5-coder:7b` is alleged to be defective, miscalibrated, or mischaracterized in its own behavior anywhere in this review.

## 7. What this review explicitly does not decide (Requirement H)

This review confirms a factual-premise defect only. It does **not** decide, and no statement anywhere above should be read as deciding:

- whether `llama3.2:3b` or `qwen2.5-coder:7b` should hold the `CONTROL_MODEL`/`SUBJECT_MODEL` role, or whether those roles must be redefined or reversed;
- whether either model should be replaced, retained, or deployed differently;
- whether a new candidate model should be introduced to Family F;
- whether Family F should proceed to any live test, campaign, or diagnostic call;
- whether the lighthouse observation (Section 9) becomes governed evidence for any future purpose.

Each of these is reserved, per the accepted Planning Review, to a separate, dedicated governance act following this review's own acceptance.

## 8. Correction mechanism (Requirement F)

**Status-line / cross-reference correction only, against this Defect Confirmation Review — no historical document body is rewritten.** This mirrors the precedent already established by `UNIT_3BF_FAMILY_F_DIAGNOSTIC_RAW_TRANSPORT_CAPTURE_DEFECT_CONFIRMATION_REVIEW.md`, whose correction was subsequently referenced by the Diagnostic Host Requirements Scope Lock's own Status line ("Corrected in place against the now-merged raw transport capture defect correction... which superseded...") without altering that document's own frozen body text.

Each document listed in Section 5 is expected, at the time of its own next governed touch, to receive a Status-line addendum of the form: *"Corrected in place against the accepted Model-Identity Premise Defect Confirmation Review (`[commit]`), which established that `qwen2.5-coder:7b`, not `llama3.2:3b`, is Parker's continuously deployed production model, and that Attempts 1–2's `llama3.2:3b` configuration was a diagnostic invocation outside the deployed Docker composition."* This review does not perform that addition itself for any document — doing so is reserved to the downstream correction sequence in Section 10, following this review's own acceptance.

## 9. The pending Implementation Authorization Decision — held, not modified (Requirement G)

The Family F Bounding Evidence Implementation Authorization Decision (`docs/decisions/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_BOUNDING_EVIDENCE_IMPLEMENTATION_AUTHORIZATION_DECISION.md`) remains exactly as it was before this review — untouched, uncommitted, `CONTROL_MODEL=llama3.2:3b` unedited. This review confirms it must remain **held from its own Independent Constitutional Review** until, in order:

```text
1. this Defect Confirmation Review is accepted;
2. this Defect Confirmation Review's own Independent Constitutional Review is accepted;
3. the affected documents listed in Section 5 receive their governed superseding
   reference (Section 8), each at its own next governed touch;
4. a separate governance act resolves the CONTROL_MODEL/SUBJECT_MODEL relationship
   and restates the Family F research question in light of the corrected premise.
```

Only after all four are satisfied may the Implementation Authorization Decision's own Independent Constitutional Review lawfully proceed — and even then, whether the Decision's own body requires a textual edit or only an accompanying corrective reference is a determination for that future task, not this one.

## 10. The lighthouse observation — recorded, not pooled (Requirement I)

The following operational observation is recorded here for provenance purposes only. It is not analyzed, weighted, or used to support any conclusion in this review beyond its own bare recording:

```text
model:                qwen2.5-coder:7b
execution path:       deployed Docker composition (docker compose run --rm parker --interactive)
owner input:          "Remember that the test lighthouse is painted orange."
observed outcome:     Reasoning completed (outcome=Reply)
persistence result:   Memory Core durability.log = 0 bytes; Knowledge Item durability.log = 0 bytes
```

**This observation is explicitly not pooled into, merged with, or treated as extending: Knowledge Discoverability's Attempts 1–2 evidence; any Family F evidence (existing or future); Unit 2 or Unit 2-D's own characterization campaigns; or any other governed dataset in this programme's history.** It is a separately-provenanced operational observation of the actually-deployed system, distinguished from every other dataset in this programme precisely because it is the only one on record that ran through the real, deployed Docker composition rather than a diagnostic harness. It authorizes nothing by itself. It may become admissible as a future reopening trigger or evidentiary input only under its own, separately-justified governance act (mirroring how the Reopening Assessment itself was required for Knowledge Discoverability's own operational trigger) — never automatically, and not by this review.

## 11. Controlling commits (Requirement K)

```text
6fd57dcb54d3eb2e06b0504eb3441dbfded6cea7  2026-08-03T02:31:07Z  introduces docker-compose.yml's
                                                                  qwen2.5-coder:7b default and the
                                                                  fixed, non-overridable model endpoint
1a0333d1e4cc56eae036fb8db5576ce5dd1ec9d9  2026-08-03T04:00:44Z  .env.example second touch, model
                                                                  recommendation unchanged
b34f8d0cadf4b8ee45db0b1c351a79f1d530162d  2026-08-09T15:57:10+12:00  RPLMC Unit 1 opens (postdates
                                                                       the above by 6 days, 13 hours)
7ad3afa01f6ac3e3d76c4af5e2107b70bfc0855c  (repository commit cited by Attempts 1-2 review)
                                            Knowledge Discoverability Attempts 1-2 recorded, using
                                            llama3.2:3b via capture-proxy endpoints structurally
                                            distinct from the fixed Docker endpoint
642cba214bcf207e72e855694d9c2dd35f8d31cf  Reopening Decision baseline — first document in the
                                            affected list to restate "current... llama3.2:3b" at
                                            programme level
86736dcaf1840d8c1003a6d56e8b0924300d865c  (PR #31) merged baseline the pending, held Implementation
                                            Authorization Decision is drafted against
```

## 12. Disposition (Requirement J)

```text
DEFECT_TYPE = FACTUAL-PREMISE DEFECT (governance prose only)
DEFECT_SCOPE = Model-identity characterization inherited across Reopening + Family F documents
DEFECT_STATUS = DEFECT CONFIRMED

CAPTURED_EXPERIMENTAL_EVIDENCE_AFFECTED = NO
PARKER_DEPLOYMENT_AFFECTED = NO
PERSISTENCE_AFFECTED = NO
QMD_AFFECTED = NO
UI_AFFECTED = NO
PARSER_AFFECTED = NO
MODEL_IMPLEMENTATION_AFFECTED = NO

REMEDY_SELECTED = NO
MODEL_SELECTED = NO
CONTROL_SUBJECT_ROLES_DECIDED = NO
RESEARCH_QUESTION_RESTATED = NO
LIVE_CALL_AUTHORIZED = NO
LIGHTHOUSE_OBSERVATION_POOLED = NO
IMPLEMENTATION_AUTHORIZATION_DECISION_MODIFIED = NO
IMPLEMENTATION_AUTHORIZATION_DECISION_ICR_PERFORMED = NO

READINESS = NOT READY (unchanged)
```

## 13. Required next governance action (Requirement K)

```text
NEXT_LAWFUL_ACTION = Independent Constitutional Review of this Defect Confirmation Review
```

Downstream correction sequence, in order, following that ICR's acceptance:

```text
1. Independent Constitutional Review of this document — pending, not performed by this task.
2. Governed superseding-reference addition to each document in Section 5, at its own
   next governed touch (Section 8) — not performed by this task.
3. A separate, dedicated governance act resolving CONTROL_MODEL/SUBJECT_MODEL roles and
   restating the Family F research question — not performed by this task.
4. Only then: the pending Implementation Authorization Decision's own first
   Independent Constitutional Review.
```

## 14. STOP conditions confirmed

```text
NO production code changed.
NO test file changed.
NO Docker configuration changed.
NO model configuration changed.
NO persistence, QMD, UI, or parser file touched.
NO model called, loaded, or contacted.
NO historical Attempts 1-2 document edited.
NO pending Implementation Authorization Decision edited.
NO Independent Constitutional Review performed (this document's own, or any other).
NO document staged, committed, or pushed.
NO CONTROL_MODEL/SUBJECT_MODEL role decided.
NO lighthouse observation pooled into any existing dataset.
```
