**Status:** Independent Constitutional Review of the Unit 3-BF Family F Capture-Proxy Response/Request-Size and Dedicated-Runtime-Growth Bounding Scope Lock — **VERDICT=ACCEPTED**. This is a second-round review of a corrected document: a prior review pass (SHA-256 `7476e4d4...58a146`) found one P2 finding — the document never named `NO_MANUFACTURED_PASS` or cited Alternative Diagnostic Host Requirements Scope Lock Section 9.5, despite the controlling Planning Review's Section 20 explicitly requiring "explicit reaffirmation, unweakened" of that rule as a freeze item. The author added a new Section 11.1 and a `NO_MANUFACTURED_PASS_GATE=PASS` acceptance gate. This review independently re-verified that correction from primary sources — not assumed fixed — and reran the complete original review scope fresh against the corrected file at SHA-256 `80ece2bdae9e05a73ad2f9405a7e1dece1f4a6735ebdc0897dc3cb6a070fa4a1`. `P0=0, P1=0, P2=0, P3=0`. `READINESS=NOT READY` (unchanged).

# Independent Constitutional Review — Unit 3-BF Family F Capture-Proxy Response/Request-Size and Dedicated-Runtime-Growth Bounding Scope Lock

## 1. Preconditions

```text
WORKING_DIRECTORY=/home/steve/parker-platform
BRANCH=governance/reasoning-protocol-family-f-bounding-scope-lock
HEAD=666c54a6e25b3431428a92e94cfb08210840af7b
REVIEWED_FILE_STATUS=untracked (git status --porcelain: "?? docs/architecture/...BOUNDING_SCOPE_LOCK.md")
REVIEWED_FILE_SHA256_BEFORE=80ece2bdae9e05a73ad2f9405a7e1dece1f4a6735ebdc0897dc3cb6a070fa4a1
WORKING_TREE_OTHER_CHANGES=NONE -- exactly one untracked file present, no modified tracked file
```

All preconditions confirmed exactly as specified. No precondition failure.

## 2. Sources independently read

- `docs/architecture/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_CAPTURE_PROXY_RESPONSE_REQUEST_SIZE_AND_DEDICATED_RUNTIME_GROWTH_BOUNDING_SCOPE_LOCK.md` — the Scope Lock, read in full, fresh, in this session (516 lines);
- `docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_RESPONSE_RUNTIME_AND_PARKER_HOST_ISOLATION_PLANNING_REVIEW.md` and its accepted Independent Constitutional Review;
- `docs/architecture/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_ALTERNATIVE_DIAGNOSTIC_HOST_REQUIREMENTS_SCOPE_LOCK.md` and its accepted Independent Constitutional Review, with Sections 9.2, 9.3, and 9.5 re-read line-by-line against the reviewed document's citations;
- `docs/implementation/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_DIAGNOSTIC_IMPLEMENTATION_EXECUTION_PLAN.md` and its accepted Independent Constitutional Review;
- `docs/architecture/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_EXPERIMENTAL_RECLASSIFICATION_AND_QUALIFICATION_BOUNDARY_SCOPE_LOCK.md` and its accepted Independent Constitutional Review — call-arithmetic source;
- `docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_DIAGNOSTIC_IMPLEMENTATION_COMPLETION_REVIEW.md` and its accepted review;
- `docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_DIAGNOSTIC_READINESS_REVIEW.md` and its accepted review;
- `docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_DIAGNOSTIC_READINESS_BLOCKER_RESOLUTION_PLANNING_REVIEW.md` and its accepted review;
- `docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_PARKER_CANDIDATE_HOST_ASSESSMENT_REVIEW.md` and its accepted review;
- `docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_DIAGNOSTIC_RAW_TRANSPORT_CAPTURE_DEFECT_CONFIRMATION_REVIEW.md` and `docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_DIAGNOSTIC_RAW_TRANSPORT_CAPTURE_CORRECTION_COMPLETION_REVIEW.md`, both with their accepted Independent Constitutional Reviews;
- `tests/integration/ReasoningProtocolFamilyFDiagnosticTest.kt`, `tests/integration/ReasoningProtocolFamilyFDiagnosticOrchestrationTest.kt`, `tests/integration/ReasoningProtocolLiveModelEvaluationHarness.kt`;
- `src/runtime/ModelInferenceClient.kt` and `src/runtime/ReasoningPromptBuilder.kt`;
- `docs/architecture/parker-constitution.md`;
- `git log`, confirming baseline `666c54a6e25b3431428a92e94cfb08210840af7b` as the merge of PR #29 ("governance/reasoning-protocol-family-f-bounding-planning").

Every document above was confirmed to exist at its cited path. Every ICR cited as "accepted" was independently opened and confirmed to state `VERDICT=ACCEPTED` (or `ADVERSARIAL_FINDINGS=0`) with `P0=P1=P2=P3=0`.

## 3. `NO_MANUFACTURED_PASS` correction — independently re-verified against all six required checks

The prior P2 finding is fully resolved. Independent re-verification, not reliance on the author's summary:

**(1) Explicit, unweakened reaffirmation.** New Section 11.1 (lines 256–265) states: "Alternative Diagnostic Host Requirements Scope Lock Section 9.5's `NO_MANUFACTURED_PASS` rule remains binding here, explicitly and without weakening." This names the rule and cites Section 9.5 by number, satisfying the controlling Planning Review's Section 20 item 7 requirement to the letter.

**(2) All four original vectors preserved.** Independently read Alternative Diagnostic Host Requirements Scope Lock Section 9.5 (lines 184–195) fresh and confirmed its `NO_MANUFACTURED_PASS` sub-rule (line 193) prohibits: (a) deletion/cleanup to inflate a momentary usable-space reading; (b) pointing at an alternate volume the campaign will not actually use; (c) reducing captured evidence (raw body truncation, dropped ledgers, or a reduced 392-call schedule); (d) substituting a different, smaller model artifact. Section 11.1's four-item list maps each vector one-to-one and broadens rather than narrows every one: item 2 ("alternate volume, filesystem, evidence root, or runtime root") is a strict superset of the source's "alternate volume"; item 3 adds "dropping... mandatory artifacts" to the source's truncation/dropped-ledger/reduced-schedule language; item 4 adds "changing quantization" and "otherwise reducing the frozen workload" to the source's model-substitution language, consistent with (and reinforcing) Section 4's independent `SUBSTITUTION=PROHIBITED`/`QUANTIZATION_CHANGE=PROHIBITED` invariants. No vector is omitted, softened, or narrowed.

**(3) Path/device identity requirement.** Section 11.1 states: "Every usable-space reading, filesystem allocation unit, `E` calculation, and `R` calculation must resolve to the exact paths and devices named for the future governed campaign. Path/device identity must be recorded and independently reproduced before a pass may be declared." Independently confirmed this is a faithful, non-narrowing restatement of Section 9.5's own `ACTUAL_PATH_MEASUREMENT`, `PATH_DEVICE_IDENTITY`, and `NO_PROXY_MEASUREMENT` sub-rules (source lines 187, 188, 191).

**(4) Raw usable space cannot substitute for E+R.** Section 11.1's closing sentence states this explicitly: "Raw usable disk space, however large, never substitutes for computed `E+R` and the applicable governed reserves." Confirmed present verbatim.

**(5) Acceptance gate present.** `NO_MANUFACTURED_PASS_GATE=PASS` is present at Section 18, line 403, correctly positioned inside the block governed by "A bound-selection decision must reject a proposed package unless all applicable gates pass" — independently confirmed binding, consistent with the other eight gates, introducing no formatting or logical inconsistency.

**(6) No new authority introduced.** Independently re-read Section 24's final authority statement in full: every flag remains `NO`/`UNCOMPUTABLE`/`NOT READY` in substance, unchanged from the prior version — `READINESS=NOT READY`, `E_STATUS=UNCOMPUTABLE`, `R_STATUS=UNCOMPUTABLE`, `NUMERIC_BOUND_SELECTED=NONE`, and every authorization flag (`IMPLEMENTATION_AUTHORIZED`, `EVIDENCE_ACQUISITION_AUTHORIZED`, `PROVIDER_OR_MODEL_CONTACT_AUTHORIZED`, `DEDICATED_PROVIDER_LAUNCH_AUTHORIZED`, `HOST_OR_VM_PROVISIONING_AUTHORIZED`, `MODEL_ACQUISITION_AUTHORIZED`, `CAMPAIGN_AUTHORIZED`, `EXPLICIT_EXECUTION_APPROVAL_AUTHORIZED`, `KNOWLEDGE_DISCOVERABILITY_ATTEMPT_3_AUTHORIZED`) reads `NO`. Section 11.1 and the new gate line add only a governance constraint, no grant.

## 4. Complete re-review of the original scope

### 4.1 Scope and authority (Sections 2–5, 22, 24)

Independently confirmed the Scope Lock performs exactly the accepted Planning Review's recommended action and freezes, at minimum, all eight required items: (1) request-bound estimator methodology (Section 6); (2) response-bound evidence requirements (Section 7); (3) bounded-streaming enforcement (Section 9); (4) header-count/aggregate-header-byte requirements (Section 8); (5) oversize failure semantics (Section 10); (6) dedicated-runtime `R` evidence requirements (Sections 12–14); (7) `NO_MANUFACTURED_PASS` (Section 11.1, confirmed above); (8) all frozen Family F invariants (Section 4). Confirmed the document selects no numeric bound, authorizes no estimator/evidence-producing action, no implementation, no provider/model contact, no dedicated daemon launch, no VM/infrastructure change, does not resolve Category 6, and issues no Explicit Execution Approval, campaign authorization, or Knowledge Discoverability Attempt 3 authorization. `READINESS=NOT READY`, `E_STATUS=UNCOMPUTABLE`, `R_STATUS=UNCOMPUTABLE` all consistently preserved throughout.

### 4.2 Frozen invariants (Section 4) — arithmetic independently recomputed

Independently re-derived against the Experimental Reclassification and Qualification Boundary Scope Lock's own source text: 23 fixtures × 2 profiles = 46 fixture/profile cells per role; 46 × 4 scored repetitions = 184 scored calls per role; × 2 roles (subject + control) = 368 scored calls. 8 residency blocks (4 repetitions × 2 models) × 3 warm-up calls per block = 24 warm-up calls. 368 + 24 = 392 total calls — every figure matches the source document's own arithmetic exactly, not merely internally self-consistent. `SUBJECT_MODEL=qwen2.5-coder:7b`, `CONTROL_MODEL=llama3.2:3b`, the absolute subject-only advancement gate, and the ranking/substitution/quantization/reduced-schedule prohibitions all independently confirmed against the Experimental Reclassification Scope Lock's own Section 4.

### 4.3 Request-bound protocol (Section 6)

Independently traced the complete live-path request-construction chain: `FamilyFCorpus.fixtures` (23, confirmed), `profiles` (2, confirmed), `warmupFixture`, and both frozen model-name constants in `ReasoningProtocolFamilyFDiagnosticTest.kt`; `SyntheticContextProfiles.construct` in `ReasoningProtocolLiveModelEvaluationHarness.kt`; `DefaultReasoningPromptBuilder.buildPrompt` and `defaultOllamaRequestBody` in `src/runtime/ReasoningPromptBuilder.kt` and `src/runtime/ModelInferenceClient.kt`. Confirmed no other live-path input affects request-body size — Section 6's enumeration (23 fixtures × 2 profiles × warm-up × 2 model names, via the unmodified production path) is complete, with no missing request-shaping variant. Confirmed Section 6 does not claim the number has already been computed.

### 4.4 Response-bound protocol (Section 7) — adversarial assessment

Independently confirmed both admissible routes require accounting for the complete serialized HTTP response envelope, not merely generated text (Section 7.1: "the complete serialized HTTP response body, not merely generated text"; Section 7.2 item 5: "the response-envelope serializer's complete worst-case overhead is independently bounded"). Confirmed the anti-circularity prohibition ("A transport cap cannot be selected first and then described as provider evidence") is backed by a real enforcement point: Section 16's governance sequence places Independent Constitutional Review before any bound-selection decision. The two routes, together with this enforcement structure, are adequate to establish a genuine hard maximum over the complete response body — not merely the generated-text field.

### 4.5 Header-bound protocol (Section 8)

Independently verified both source citations resolve exactly at this baseline: `ReasoningProtocolFamilyFDiagnosticTest.kt:505-514,543-547` (request/response header-forwarding loops) and `ReasoningProtocolFamilyFDiagnosticOrchestrationTest.kt:569-579` (`familyFEncodeHeaders`) — confirmed unchanged between the prior and current baselines via `git log`, so no stale citation risk. Confirmed both show headers iterated/forwarded/encoded with no count or aggregate-byte limit. No unaddressed header or status-line allocation path found.

### 4.6 Bounded transport and oversize semantics (Sections 9–10)

Independently confirmed the two live unbounded reads at `ReasoningProtocolFamilyFDiagnosticTest.kt:493` and `:516` inside `ProxyHandler.handle`, and the halt path at `ReasoningProtocolFamilyFDiagnosticOrchestrationTest.kt:1634-1640`. Confirmed the proxy uses explicit fixed-length (`Content-Length`) framing when forwarding responses, making the claim that the unmodified production client (`ModelInferenceClient.kt`) can never receive an oversize response — provided the proxy proves it never forwards more than `MAX_RESPONSE_BOUND` — technically sound. Adversarially examined the durable-metadata list in Section 10; every field is bounded by construction or is explicitly subordinate to the section's own "must contain only bounded metadata" governing constraint. `NO_TRUNCATE_AND_FORWARD` and `NO_PARTIAL_DURABLE_BODY`, combined with the required deterministic definition of "observed count," close the misrepresentation risk the task asked this review to probe.

### 4.7 Evidence-budget `E` (Section 11)

Independently re-read Alternative Diagnostic Host Requirements Scope Lock Section 9.2 and confirmed Section 11's component list is complete: 392 maximum-size records, Base64 and JSON-escaping expansion, bounded multi-valued headers, exactly seven chained ledgers (confirmed by independent count of the source's `MANDATORY_COMPONENTS`: transport.jsonl plus six others), control/resource records, campaign definition/identity, sealed reporting, advancement worksheet, manifest/terminal marker, allocation rounding, proven transient coexistence allocation, and the 2 GiB reserve. No omission, no double-counting risk.

### 4.8 Dedicated-runtime budget `R` (Sections 12–14)

Independently re-read Alternative Diagnostic Host Requirements Scope Lock Section 9.3 and confirmed Section 12's exclusions (immutable image layers, immutable model artifacts) and inclusions (every genuinely writable location) match. Adversarially assessed whether Section 13's two evidence routes can establish a genuine upper bound rather than a lucky finite observation: confirmed three compounding safeguards make this adequate — (a) the required "representative load and why it upper-bounds the real 392-call campaign" is a structural-upper-bound argument, not a typical-case sample; (b) Section 12's "unknown writable locations are not zero" defaults unobserved/uncontained paths to failing `R`, not passing it; (c) Section 14 requires enforced containment as the primary mechanism and explicitly preserves the existing dynamic per-block disk gate as an independent, cumulative backstop that would still catch an undercounted static `R` at execution time.

### 4.9 Provenance and governance sequence (Sections 15–16)

Independently confirmed the 12-step sequence correctly compresses, without skipping or misordering, the Alternative Host Requirements Scope Lock's own host-track sequence at its tail (steps 10–12: renewed candidate-host assessment and Readiness Review, Explicit Execution Approval, execution). No step authorizes its successor; missing evidence at any step preserves `NOT READY`.

### 4.10 Host-isolation handoff (Section 17)

Independently re-verified against the accepted Planning Review's own Sections 17–22, 24: same-VM coexistence on Parker VM 102 not recommended on current evidence; dedicated diagnostic VM viable in principle but unprovisioned and unassessed; remaining blocked valid; no VM creation, resize, allocation, provider installation, or model acquisition authorized anywhere in the document. No sentence silently grants a coexistence amendment or provisioning authority.

### 4.11 Citation audit

Every file path, section reference, source-code line citation, quoted or specially named governance rule, baseline/commit reference, claimed accepted verdict, and internal cross-reference was independently checked against its primary source. `NO_MANUFACTURED_PASS` correctly attributed to Section 9.5 (Section 3 above); `NO_COEXISTING_PRODUCTION_WORKLOAD`/`NO_INDETERMINACY_SOURCES` are governed by Section 10 of the Alternative Host Requirements Scope Lock and are correctly left uncited in Section 17, which explicitly declines to resolve that category rather than re-freezing it. All Kotlin line references (`ReasoningProtocolFamilyFDiagnosticTest.kt:493,516,505-514,543-547`; `ReasoningProtocolFamilyFDiagnosticOrchestrationTest.kt:569-579,1634-1640`) independently confirmed accurate at the current baseline. No overbroad range or paraphrase found presented as a verbatim quotation.

```text
CITATION_AUDIT=NO STALE, MISQUOTED, OR UNRESOLVED CITATION FOUND
```

### 4.12 Constitutional review (Section 23)

Independently read `docs/architecture/parker-constitution.md` in full and confirmed: "Cognition proposes. Trust authorises. Runtime executes." (verbatim, line 48), with "No stage may absorb another. Cognition may not authorize itself." (line 56) directly supporting Section 23's "no bound authorizes itself"; "No capability may bypass trust" (verbatim, line 90); "The owner remains in control" (line 36) supporting the claim that "the owner retains the ability to see, limit, stop, and decline every later step." No mischaracterization or missing controlling principle.

## 5. Finding counts

```text
P0=0
P1=0
P2=0
P3=0
```

The prior P2 finding is fully resolved by the new Section 11.1 and the `NO_MANUFACTURED_PASS_GATE`, independently re-verified against all six specific checks required. No finding of any severity survived independent re-derivation of the complete original review scope, across four independent second-round verification passes covering: the `NO_MANUFACTURED_PASS` correction plus governance chain, invariant arithmetic, and host-isolation handoff; the request-bound and header-bound protocols against actual code; oversize semantics and an adversarial assessment of the response-bound evidence routes; and the `E`/`R` budget recomputation, provenance package, governance sequence, and constitutional conformance.

## 6. Authority boundaries preserved

Independently confirmed the Scope Lock:

- selects no numeric request, response, header, `E`, or `R` bound;
- authorizes no estimator or evidence-producing action;
- authorizes no implementation, Kotlin/Gradle/production/test/container/VM/filesystem change;
- authorizes no provider or model contact;
- authorizes no dedicated daemon launch;
- authorizes no VM or infrastructure change;
- does not resolve Category 6 host isolation — explicitly hands it off (Section 17);
- issues no Explicit Execution Approval;
- authorizes no live campaign;
- does not authorize or begin Knowledge Discoverability Attempt 3;
- preserves `READINESS=NOT READY`, `E_STATUS=UNCOMPUTABLE`, and `R_STATUS=UNCOMPUTABLE` consistently throughout.

## 7. Final validation

```text
$ sha256sum docs/architecture/...BOUNDING_SCOPE_LOCK.md   (before this review's own actions)
  80ece2bdae9e05a73ad2f9405a7e1dece1f4a6735ebdc0897dc3cb6a070fa4a1
$ sha256sum docs/architecture/...BOUNDING_SCOPE_LOCK.md   (after this review's own actions)
  80ece2bdae9e05a73ad2f9405a7e1dece1f4a6735ebdc0897dc3cb6a070fa4a1  -- UNCHANGED, file not modified
$ git diff --check
  (no output -- clean, exit 0)
$ grep -cP '[ \t]+$' docs/architecture/...BOUNDING_SCOPE_LOCK.md
  0  -- zero trailing-whitespace matches
$ git status --porcelain --branch
  ## governance/reasoning-protocol-family-f-bounding-scope-lock
  ?? docs/architecture/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_CAPTURE_PROXY_RESPONSE_REQUEST_SIZE_AND_DEDICATED_RUNTIME_GROWTH_BOUNDING_SCOPE_LOCK.md
  ?? docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_CAPTURE_PROXY_RESPONSE_REQUEST_SIZE_AND_DEDICATED_RUNTIME_GROWTH_BOUNDING_SCOPE_LOCK_INDEPENDENT_CONSTITUTIONAL_REVIEW.md
$ git rev-parse HEAD
  666c54a6e25b3431428a92e94cfb08210840af7b
```

```text
READINESS=NOT READY (unchanged)
VERDICT=ACCEPTED
FILES_CREATED=1 (this document)
EXISTING_FILES_MODIFIED=NONE (the Scope Lock's SHA-256 is identical before and after)
STAGED_COMMITTED_PUSHED=NONE
PULL_REQUEST_OPENED=NONE
BRANCH_SWITCHED=NONE
IMPLEMENTATION_CHANGE=NONE
PROVIDER_OR_MODEL_CONTACT=NONE
DEDICATED_DAEMON_LAUNCHED=NONE
CAMPAIGN_OR_RUNTIME_DIRECTORY_CREATED=NONE
CONTAINER_VM_PROCESS_OR_INFRASTRUCTURE_MANIPULATED=NONE
EXPLICIT_EXECUTION_APPROVAL_ISSUED=NO
KNOWLEDGE_DISCOVERABILITY_ATTEMPT_3=NOT STARTED, NOT AUTHORIZED
```

No prohibited action occurred.
