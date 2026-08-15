**Status:** Independent Constitutional Review of the Unit 3-BF Family F Response/Request-Size, Dedicated-Runtime-Growth, and Parker-Host Isolation Planning Review — **VERDICT=ACCEPTED**. This is a fresh, independent review pass performed against the reviewed file's exact content at SHA-256 `88b3c59702245e32a615e57099a1713a0fff24f7a7253f9dbb03d6a0c1981a0a`. No claim in the Planning Review, including any prior reviewer's conclusion or the Planning Review's own author-supplied correction summary, was treated as verified without independent re-derivation against primary sources. `P0=0, P1=0, P2=0, P3=0`. `READINESS=NOT READY` (unchanged).

# Independent Constitutional Review — Unit 3-BF Family F Response/Request-Size, Dedicated-Runtime-Growth, and Parker-Host Isolation Planning Review

## 1. Preconditions

```text
WORKING_DIRECTORY=/home/steve/parker-platform
BRANCH=governance/reasoning-protocol-family-f-bounding-planning
HEAD=3038bf7c7b0d9dfdf53d51b41fe94c4cd5585423
REVIEWED_FILE_STATUS=untracked (git status --porcelain: "?? docs/reviews/...PLANNING_REVIEW.md")
REVIEWED_FILE_SHA256_BEFORE=88b3c59702245e32a615e57099a1713a0fff24f7a7253f9dbb03d6a0c1981a0a
WORKING_TREE_OTHER_CHANGES=NONE -- exactly one untracked file present, no modified tracked file
```

All preconditions confirmed exactly as specified. No precondition failure.

## 2. Sources independently read

- `docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_RESPONSE_RUNTIME_AND_PARKER_HOST_ISOLATION_PLANNING_REVIEW.md` — the Planning Review, read in full, fresh, in this session;
- `docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_PARKER_CANDIDATE_HOST_ASSESSMENT_REVIEW.md` and its accepted Independent Constitutional Review;
- `docs/architecture/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_ALTERNATIVE_DIAGNOSTIC_HOST_REQUIREMENTS_SCOPE_LOCK.md` and its accepted Independent Constitutional Review, read with line numbers and independently re-mapped section-by-section;
- `docs/architecture/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_EXPERIMENTAL_RECLASSIFICATION_AND_QUALIFICATION_BOUNDARY_SCOPE_LOCK.md` and its accepted Independent Constitutional Review;
- `docs/implementation/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_DIAGNOSTIC_IMPLEMENTATION_EXECUTION_PLAN.md` and its accepted Independent Constitutional Review;
- `docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_DIAGNOSTIC_IMPLEMENTATION_COMPLETION_REVIEW.md` and `docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_DIAGNOSTIC_IMPLEMENTATION_INDEPENDENT_CONSTITUTIONAL_REVIEW.md`;
- `docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_DIAGNOSTIC_READINESS_REVIEW.md` and its accepted Independent Constitutional Review;
- `docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_DIAGNOSTIC_READINESS_BLOCKER_RESOLUTION_PLANNING_REVIEW.md` and its accepted Independent Constitutional Review, read in full;
- `docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_DIAGNOSTIC_RAW_TRANSPORT_CAPTURE_DEFECT_CONFIRMATION_REVIEW.md` and its accepted Independent Constitutional Review;
- `docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_DIAGNOSTIC_RAW_TRANSPORT_CAPTURE_CORRECTION_COMPLETION_REVIEW.md` and `docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_DIAGNOSTIC_RAW_TRANSPORT_CAPTURE_CORRECTION_INDEPENDENT_CONSTITUTIONAL_REVIEW.md`, including their specific §3.1/§3.3 and §6 subsections;
- `tests/integration/ReasoningProtocolFamilyFDiagnosticTest.kt`, `tests/integration/ReasoningProtocolFamilyFDiagnosticOrchestrationTest.kt`, `tests/integration/ReasoningProtocolLiveModelEvaluationHarness.kt`;
- `src/runtime/ModelInferenceClient.kt`;
- `docs/architecture/parker-constitution.md`;
- `git log`, confirming commit hashes `c5b90fa7c187e68d0d7fb2a9c165202932d482c7` and `c419db3e570bef101c200637fb6668837d77b148` (PR #26) exist in this repository's history and match the raw-transport correction.

Every document above was confirmed to exist at its cited path. Every ICR cited as "accepted" was independently opened and confirmed to state `VERDICT=ACCEPTED` (or equivalent) with `P0=P1=P2=P3=0`.

## 3. Full citation audit

Every quotation, path, section reference, item reference, source-code line citation, document line range, commit reference, and claimed verdict in the complete Planning Review was audited — not limited to the previously corrected passages.

```text
CITATION_AUDIT=NO STALE, MISQUOTED, OR UNRESOLVED CITATION FOUND
```

### A. `NO_MANUFACTURED_PASS` (Section 9.5, not Section 9.4)

Independently mapped the Scope Lock's section headers: Section 9.4 ("Pass rules," lines 173–183) contains only `SEPARATE_FILESYSTEMS` / `SHARED_FILESYSTEM` / `RESERVE_COMPOSITION` — no mention of `NO_MANUFACTURED_PASS` anywhere in that range. Section 9.5 ("Measurement and integrity requirements," lines 184–195) contains `NO_MANUFACTURED_PASS` at line 193, verbatim. The Planning Review's Section 15, Section 20 item 7, and Section 27 all correctly attribute the rule to Section 9.5. A full-document grep for `NO_MANUFACTURED_PASS`, `Section 9.4`, and `Section 9.5` confirmed all occurrences are internally consistent and none still misattributes the rule to Section 9.4.

### B. `NO_INDETERMINACY_SOURCES`

Scope Lock line 211 reads exactly: "no unrelated, ungoverned workload on the candidate host may run concurrently with the campaign in a way that makes any resource-gate reading indeterminate or unrepresentative of what the campaign itself would actually experience during a real 392-call run." The Planning Review's Section 17.1 quotation matches this byte-for-byte, including the previously-missing trailing clause "during a real 392-call run." Every occurrence of `NO_INDETERMINACY_SOURCES` in the document was checked; only one is a direct quotation (Section 17.1) and it is exact.

### C. Scope Lock source ranges

Every claimed range in Section 27 was independently re-derived by mapping every `##`/`###` section header in the Scope Lock with line numbers, and confirmed exact under a consistent convention (content ends at the section's own last non-blank/fence line, before the next header's blank separator):

```text
Section 4:   lines 40-56   -- CONFIRMED
Section 6:   lines 76-88   -- CONFIRMED (starts exactly at line 76, no oversweep into Section 5, lines 58-75)
Section 7:   lines 89-99   -- CONFIRMED
Section 9.2: lines 122-162 -- CONFIRMED
Section 9.3: lines 164-171 -- CONFIRMED
Section 9.4: lines 173-183 -- CONFIRMED
Section 9.5: lines 184-195 -- CONFIRMED
Section 10:  lines 204-212 -- CONFIRMED
Section 13:  lines 240-252 -- CONFIRMED (present, correctly scoped, not omitted)
```

Section 27 cites Sections 6, 7, and 13 as three separate, distinctly scoped ranges (not a combined range), and explicitly states this omits Section 5 and includes Section 13 — independently confirmed accurate.

### D. Raw Transport Capture Correction references

The Raw Transport Capture Correction Independent Constitutional Review's Section 6 ("Independent re-verification of the fifteen enumerated items," approx. lines 75–94) was independently read and confirmed to contain the `BYTE_ENCODING_AND_HASHES`/`HEADER_ENCODING_DETERMINISM` findings the Planning Review's Section 9 relies on. The Correction Completion Review's §3.1/§3.3 (numbered subsections of top-level Section 3, approx. lines 37–45 and 56–65) were independently read; §3.3 explicitly states that Base64's own alphabet requires no JSON escaping, with no intervening `familyFJsonEscape` call — directly supporting the Planning Review's Section 9 claim that Base64-encoded fields contribute zero JSON-escaping expansion. The Planning Review's own characterization of §3.1/§3.3 as "un-numbered as a standalone top-level section" (i.e., numbered only as subsections, not as their own top-level section) is accurate.

### E. Earlier launch-history correction

Independently re-verified:

- The Parker Candidate Host Assessment's Section 7.2 states "no dedicated (non-production) Ollama instance has ever been launched on this host under any governance to date," where "this host" resolves (via that Assessment's own Section 2 `CANDIDATE_HOST` fixing) to Parker VM 102 specifically — not "any host."
- The Planning Review's Section 14 now scopes this finding explicitly to "Parker VM 102" and states separately that "this Planning Review found no separately governed evidence establishing a reusable runtime-growth ceiling for another candidate host" — a correctly bounded claim about the absence of evidence found by this review, not a claim that no instance has ever been launched anywhere.
- A full-document grep for `READINESS_BLOCKER_RESOLUTION`, `Path 1`, and `Section 6` confirmed the Planning Review no longer cites the Readiness Blocker Resolution Planning Review anywhere in connection with dedicated-instance launch history. The only remaining references to that document are to its Section 8 (Path 3, model-acquisition governance discipline) at Planning Review Sections 18.2/27, and its Section 9 (Path 4, remain-blocked/defer) at Planning Review Sections 19/27 — both independently read and confirmed to say exactly what is attributed to them.
- Section 27 explicitly states this source "is not cited in Section 14," and independently confirmed accurate.

## 4. Technical verification

- **Two unbounded live-path reads**: `ReasoningProtocolFamilyFDiagnosticTest.kt:493` (`exchange.requestBody.readBytes()`) and `:516` (`httpClient.send(...HttpResponse.BodyHandlers.ofByteArray())`), both inside `FamilyFCaptureProxy.ProxyHandler.handle`, both unbounded, both on the only path any of the 392 governed live calls traverses — independently confirmed by direct reading, not grep alone.
- **No currently enforced byte ceiling**: independently re-grepped both diagnostic files for `MAX_RESPONSE|MAX_REQUEST|maxResponseBytes|maxRequestBytes|responseByteLimit|MAX_BODY|MAX_HEADER` — zero matches, confirming no bound-naming constant exists anywhere in either file.
- **Request-bound derivability**: independently confirmed all four fixed inputs (23-fixture corpus, 2 profiles, 1 warm-up fixture, 2 model names) and the unmodified `DefaultReasoningPromptBuilder`/`defaultOllamaRequestBody` formatter path are frozen and present in the working tree, making the request ceiling a deterministic, reproducible quantity computable by a future disclosed estimator — not computed by this document.
- **Response ceiling uncomputability**: independently re-confirmed neither admissible evidence source (provider-published maximum output, or an already-governed enforceable transport bound) exists anywhere in the governance chain read for this task.
- **`E_STATUS=UNCOMPUTABLE`** and **`R_STATUS=UNCOMPUTABLE`**: both independently re-confirmed unchanged from the accepted Parker Candidate Host Assessment's own findings; the Planning Review adopts no numeric value for either.
- **Provider/runtime evidence required to bound `R`**: independently confirmed neither documented provider resource behavior nor a read-only-observed writable-growth ceiling from an actual dedicated launch exists in the governance chain, for VM 102 or any other host.
- **Capture-proxy header and raw-body handling**: `familyFEncodeHeaders` (`OrchestrationTest.kt:569-579`) and the proxy's header-forwarding loops (`DiagnosticTest.kt:505-514, 543-547`) independently confirmed unbounded on both header count and aggregate header bytes.
- **Fail-closed and no-retry requirements**: independently confirmed the oversize-semantics requirements in Section 6 (fail-closed abort before allocation, durable evidence limited to observed byte count and rejection fact only, no retry, no partial classification, deterministic campaign halt) are internally consistent and do not conflict with any cited primary source.
- **Halt path**: `FamilyFArtifactIntegrityException -> ledger.halt(...)` at `OrchestrationTest.kt:1634-1640` independently confirmed as the existing halt mechanism every other integrity failure uses, matching Section 6's routing requirement.
- **Eight-block, 368-scored plus 24-warm-up, 392-call schedule**; **frozen subject/control identities and campaign invariants**: independently confirmed against the Experimental Reclassification and Qualification Boundary Scope Lock's Section 4, unaffected by anything in this Planning Review.
- **Production isolation and memory gates**: `FamilyFProductionIsolationGuard` (`OrchestrationTest.kt:334-363`) and `FamilyFMemoryGate` (`OrchestrationTest.kt:168-229`) independently confirmed to exist and behave exactly as characterized — identity-distinctness and liveness checks only, pre-campaign, with no structural capability to guard against resource contention.
- **Same-VM production-workload coexistence**: independently re-derived the CPU (4 cores) and RAM (~11.7 GB fixed, three MemAvailable readings 10,695,912–11,054,204 kB) figures against the Candidate Host Assessment and confirmed the Planning Review's conclusion that an express safe-isolation amendment is not currently achievable on VM 102's established resource envelope, without foreclosing the possibility in the abstract.
- **Dedicated diagnostic VM path**: independently confirmed this path remains conditional and evaluative only — no VM is created, sized, or provisioned, and five categories of further evidence are named as outstanding before any candidate-host assessment could proceed.
- **Proxmox host and Parker VM arithmetic**: independently re-derived every cited figure (VM 102: 4 cores, MemTotal 11,718,700 kB / 12,288 MiB; Proxmox host: MemTotal=16,233,988 kB, MemAvailable=5,387,672 kB; ext4 filesystem; subject artifact ~4.68 GB; unused swap ~8 GiB) against the Candidate Host Assessment's own Sections 2, 3, 6.1, 6.2, and 8 — all match exactly, including the production container identifiers `736a6f38...` and `f795e46c...` both confirmed resident on VM 102.
- **Category 6 hand-off**: independently confirmed the document never claims Category 6 is resolved or closed — Sections 17.2, 19, 22, and 30's `CATEGORY_6_COEXISTENCE_GAP=ADDRESSED BY EXPLICIT HAND-OFF` line consistently use "viable in principle" and hand-off framing throughout.
- **Exactly one primary next governance action**: independently confirmed Section 23 recommends exactly one governance action (the bounding Scope Lock), with the Category 6 gap explicitly handed off to a distinct future host-track document rather than resolved by this one.

## 5. Constitutional and governance compliance

Independently read `docs/architecture/parker-constitution.md` in full and confirmed the following principles exist verbatim, as cited by the Planning Review's Section 29: "the owner remains in control" (line 36); "Cognition proposes. Trust authorises. Runtime executes." (line 48, with the proposal/authorization mechanics at lines 52–53); "No capability may bypass trust," with "There is no path from proposal to execution that does not pass through the Permission Engine" (line 90). The Planning Review's Section 29 characterization is an accurate reflection of this text, not a paraphrase that drifts from it.

## 6. Authority boundaries preserved

Independently confirmed the Planning Review:

- selects no numeric request or response bound (Sections 7.1, 7.2, 10);
- selects no runtime-growth bound (Section 14, Part B generally);
- changes no implementation — no test, source, or Gradle file is modified by this document;
- provisions or selects no VM or host — Path 2 (Section 18) is evaluative only, creating nothing;
- launches no provider, acquires, loads, or contacts no model;
- manipulates no production process — Section 17's own text forecloses, not merely omits, any silent authorization to stop or reconfigure production Parker or production Ollama;
- issues no Explicit Execution Approval;
- authorizes no live campaign;
- does not authorize or begin Knowledge Discoverability Attempt 3;
- preserves `READINESS=NOT READY` throughout, unchanged from the accepted chain it builds on.

## 7. Finding counts

```text
P0=0
P1=0
P2=0
P3=0
```

No finding of any severity survived independent re-derivation against primary sources, across four independent verification passes covering: Part A (bounded request/response capture, Sections 4–10); Part B (bounded dedicated-runtime growth, Sections 11–15, including the two specifically re-audited prior corrections); Part C (Parker physical-server isolation, Sections 16–22, including the `NO_INDETERMINACY_SOURCES` quote correction); and the full governance chain, Section 27 line-range audit, and constitutional conformance.

## 8. Final validation

```text
$ sha256sum docs/reviews/...PLANNING_REVIEW.md   (before this review's own actions)
  88b3c59702245e32a615e57099a1713a0fff24f7a7253f9dbb03d6a0c1981a0a
$ sha256sum docs/reviews/...PLANNING_REVIEW.md   (after this review's own actions)
  88b3c59702245e32a615e57099a1713a0fff24f7a7253f9dbb03d6a0c1981a0a  -- UNCHANGED, file not modified
$ git diff --check
  (no output -- clean, exit 0)
$ grep -nP '[ \t]+$' docs/reviews/...PLANNING_REVIEW.md
  (no output -- zero trailing-whitespace matches)
$ git status --porcelain --branch
  ## governance/reasoning-protocol-family-f-bounding-planning
  ?? docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_RESPONSE_RUNTIME_AND_PARKER_HOST_ISOLATION_PLANNING_REVIEW.md
  ?? docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_RESPONSE_RUNTIME_AND_PARKER_HOST_ISOLATION_PLANNING_REVIEW_INDEPENDENT_CONSTITUTIONAL_REVIEW.md
$ git rev-parse HEAD
  3038bf7c7b0d9dfdf53d51b41fe94c4cd5585423
```

```text
READINESS=NOT READY (unchanged)
VERDICT=ACCEPTED
FILES_CREATED=1 (this document)
EXISTING_FILES_MODIFIED=NONE (the Planning Review's SHA-256 is identical before and after)
STAGED_COMMITTED_PUSHED=NONE
PULL_REQUEST_OPENED=NONE
BRANCH_SWITCHED=NONE
LIVE_FAMILY_F_DIAGNOSTIC_RUN=NONE
MODEL_OR_MODEL_ENDPOINT_CONTACT=NONE
CAMPAIGN_OR_RUNTIME_DIRECTORY_CREATED=NONE
CONTAINER_VM_PROCESS_OR_INFRASTRUCTURE_MANIPULATED=NONE
EXPLICIT_EXECUTION_APPROVAL_ISSUED=NO
KNOWLEDGE_DISCOVERABILITY_ATTEMPT_3=NOT STARTED, NOT AUTHORIZED
```

No prohibited action occurred.
