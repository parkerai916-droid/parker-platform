**Status:** Independent Constitutional Review of the Unit 3-BF Family F Bounding Evidence Implementation Correction Completion Review and the corrected implementation it accepts — **REVISE BEFORE ACCEPTANCE.** The historical `REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_BOUNDING_EVIDENCE_IMPLEMENTATION_COMPLETION_REVIEW.md` (`REVISE BEFORE ACCEPTANCE`) and the later `REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_BOUNDING_EVIDENCE_IMPLEMENTATION_CORRECTION_COMPLETION_REVIEW.md` (`ACCEPTED`) are both preserved unchanged as separate audit records. This review independently confirms that the six defects carried forward from the historical review are cured, but finds that the later Completion Review misclassified its malformed-ledger finding as non-blocking. The accepted Implementation Authorization Decision Section 11 expressly requires a malformed record and duplicate completion to fail closed; the accepted Plan Section 22 requires duplicate completion, unknown state, and conflicting recovery state to halt permanently. The implementation instead silently drops malformed, unterminated, and unknown-step ledger lines, collapses duplicate completions into a set, and can continue from an under-reported resume position. Further related recovery-integrity gaps are recorded below. The implementation is correctable within the existing authorized `ReasoningProtocolFamilyFBoundingEvidenceTest.kt` surface; rejection is therefore unnecessary, but acceptance and merge are constitutionally premature.

# Family F Bounding Evidence Implementation Correction Completion Review — Independent Constitutional Review

## 1. Review authority and repository state

Read fresh from the authoritative Parker-server working tree:

```text
BRANCH=main
HEAD=a88e2e3d5f2022c340618783d5b6b9d97d7a21d6
ORIGIN_MAIN=a88e2e3d5f2022c340618783d5b6b9d97d7a21d6
HEAD_EQUALS_ORIGIN_MAIN=YES
```

Initial status:

```text
 M build.gradle.kts
 M tests/integration/ReasoningProtocolFamilyFDiagnosticTest.kt
?? docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_BOUNDING_EVIDENCE_IMPLEMENTATION_COMPLETION_REVIEW.md
?? docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_BOUNDING_EVIDENCE_IMPLEMENTATION_CORRECTION_COMPLETION_REVIEW.md
?? tests/integration/ReasoningProtocolFamilyFBoundingEvidenceTest.kt
```

Decision Section 15 was independently re-read. It requires, in order: Implementation Completion Review; Independent Constitutional Review of that review and implementation; acceptance and merge; and a separate Evidence Production Authorization Decision. The present ICR is therefore mandatory before implementation acceptance or merge. Neither Completion Review may be erased: the later correction review evaluates a corrected state and does not rewrite the earlier failed state.

## 2. Governance sources read fresh

1. Family F Model Role and Research Question Scope Lock, its first ICR, and accepted correction ICR.
2. Family F Alternative-Model Diagnostic Planning Review, accepted Model Role Amendment, and amendment ICR.
3. Experimental Reclassification and Qualification-Boundary Scope Lock, accepted Amendment, and amendment ICR.
4. Capture-Proxy Response/Request-Size and Dedicated-Runtime-Growth Bounding Scope Lock, accepted Amendment, and amendment ICR.
5. Bounding Evidence Acquisition and Offline Estimator Plan and its original accepted ICR.
6. Plan Model Role Amendment and accepted amendment ICR.
7. Bounding Evidence Implementation Authorization Decision and accepted ICR.
8. `FamilyFRole` Source Correction Amendment and accepted ICR.
9. Historical Bounding Evidence Implementation Completion Review (`REVISE BEFORE ACCEPTANCE`).
10. Bounding Evidence Implementation Correction Completion Review (`ACCEPTED`).
11. `docs/architecture/parker-constitution.md` where required by the controlling instruments.

The accepted commit lineage was independently confirmed through `a88e2e3`: model-role Scope Lock (`b22e733`), Planning Review roles (`d08f836`), Reclassification roles (`2b0b54e`), Capture-Proxy roles (`a378f32`), Bounding Plan roles (`eec0492`), Implementation Decision (`2c8842b`), and `FamilyFRole` correction (`a88e2e3`).

## 3. Implementation sources reviewed

Read completely:

- `build.gradle.kts`;
- `tests/integration/ReasoningProtocolFamilyFDiagnosticTest.kt`;
- `tests/integration/ReasoningProtocolFamilyFBoundingEvidenceTest.kt`.

Referenced implementation was inspected as required, including `FamilyFCampaignDefinition`, `SyntheticContextProfiles`, `DefaultReasoningPromptBuilder`, `defaultOllamaRequestBody`, ledger consumers, manifest verification, terminal-state handling, source-inspection guards, and task registration.

No test, Gradle compilation, runtime path, evidence producer, model, provider, network endpoint, Ollama process, Docker command, or Parker runtime was invoked by this review.

## 4. Authorized file surface

**Finding: CONFORMING.**

- `build.gradle.kts`: the 13-line detached task registration is within Decision Section 6.
- `tests/integration/ReasoningProtocolFamilyFBoundingEvidenceTest.kt`: the new implementation is within Decision Sections 5–12.
- `tests/integration/ReasoningProtocolFamilyFDiagnosticTest.kt`: the only diff is the exact two-line constant-value correction separately authorized by the accepted `FamilyFRole` Source Correction Amendment.
- No file under `src/`, and no production runtime, parser, persistence, Memory Core, Knowledge Item, QMD, UI, Docker, or model-configuration file is modified.

## 5. Model-role correctness

```text
DEPLOYED_BASELINE = qwen2.5-coder:7b
CONTROL_MODEL     = qwen2.5-coder:7b
SUBJECT_MODEL     = llama3.2:3b
```

**Finding: CONFORMING.** `FamilyFRole.SUBJECT` resolves through `FAMILY_F_SUBJECT_MODEL_NAME` to `llama3.2:3b`; `FamilyFRole.CONTROL` resolves through `FAMILY_F_CONTROL_MODEL_NAME` to `qwen2.5-coder:7b`. The estimator directly reads `trial.role.modelName` and `trial.role.name`. The Bounding Evidence file contains no second literal model-role table or output-layer relabeling. Generated schedule records therefore inherit the corrected physical model identities directly from the accepted source of truth.

## 6. Historical defect closure

### 6.1 Real double-gated evidence-producing entry test

- **Original defect:** neither entry test could reach the real producer under the satisfied-gate path.
- **Current implementation:** four explicit gate combinations call the actual entry function; the both-true case wraps but delegates to `FamilyFBoundingEvidenceProducer.produce` and asserts one invocation and 392 records.
- **Governance requirement:** Decision Sections 7–9 require both positive gates, refusal when the live-execution approval variable is present, and exactly one real entry invocation.
- **ICR finding:** source tracing confirms both positive gates are necessary, the negative gate is checked first, and only the both-true/negative-absent branch reaches the producer.
- **Disposition:** **CURED.**

### 6.2 WP-B/C/D validators

- **Original defect:** non-empty inputs received unconditional fixed statuses without substantive validation.
- **Current implementation:** WP-B validates category and required provenance; WP-C rejects limit-kind/target conflation; WP-D admits only official provider documentation not derived from prohibited observation.
- **Governance requirement:** Plan Sections 15–18 require offline admissibility and applicability checks without retrieval or manufactured resolution.
- **ICR finding:** positive and negative branches are mechanically distinct and remain unresolved rather than manufacturing a numeric answer.
- **Disposition:** **CURED for the authorized offline admissibility layer.**

### 6.3 Recovery/resume

- **Original defect:** no resume path existed.
- **Current implementation:** `produceOrResume` handles clean, COMPLETE, FAILED, conflicting terminal, torn WP-E, and WP-A artifact inconsistency paths.
- **Governance requirement:** Plan Section 22 and Decision Section 11 require exact-once, verified skip, append-only behavior, and fail-closed malformed/duplicate/unknown/conflicting handling.
- **ICR finding:** basic recovery exists, but the complete governance requirement is not satisfied for malformed, duplicate, unknown, and incompletely verified completed-step states (Sections 7 and 9).
- **Disposition:** **NOT FULLY CURED; BLOCKING.**

### 6.4 Source-inspection exclusion scope

- **Original defect:** the exclusion covered the full import block.
- **Current implementation:** the exclusion is confined to the forbidden-symbol declaration and synthetic tests prove detection immediately before and after it.
- **Governance requirement:** narrow, testable isolation guard without hiding substantive implementation.
- **ICR finding:** imports and estimator implementation remain within scanned text; the exclusion is bounded.
- **Disposition:** **CURED.**

### 6.5 Model-role source mismatch

- **Original defect:** `FamilyFRole` encoded the reversed physical models.
- **Current implementation:** the separately authorized two-line correction matches the controlling Scope Lock exactly and is consumed directly.
- **Governance requirement:** accepted role table and no substituted model mapping.
- **ICR finding:** mechanically correct end to end.
- **Disposition:** **CURED.**

### 6.6 Stale documentation

- **Original defect:** header prose described the old role mapping and obsolete full byte-freeze.
- **Current implementation:** header records the corrected mapping and narrow amendment exception.
- **Governance requirement:** accurate authority and execution status.
- **ICR finding:** materially accurate.
- **Disposition:** **CURED.**

## 7. Critical malformed-ledger fail-closed finding

Decision Section 11 states verbatim in substance that an attempted post-finalization append, **malformed record**, **duplicate completion**, unknown file, hash mismatch, final/temporary conflict, or copied-directory verification failure must fail closed. Plan Section 22 independently requires duplicate completion, an unknown file, conflicting state, and hash mismatch to halt permanently.

`FamilyFBoundingEvidenceLedgerReader.readCompletedSteps()` does not parse and validate each JSONL record. It searches for the substring `"step":"`, returns `null` for a missing or unterminated step value, catches an unknown enum value with `runCatching(...).getOrNull()`, drops every such line through `mapNotNull`, and finally converts the result to a `Set`.

**Mechanical classification:** malformed/unknown/unterminated records are silently ignored and execution continues. Duplicate valid step records are silently collapsed.

**Consequences:**

- completed steps can be under-reported;
- resume position can be moved backward;
- a step previously represented in the ledger can be run or recorded again;
- duplicate completion can be concealed rather than halted;
- malformed state can be converted into an apparently clean resumable state;
- a recognized `step` substring inside otherwise malformed JSON can be treated as completed because status, schema, record termination, field uniqueness, and full-record validity are not checked.

This is a direct violation of accepted Decision Section 11 and Plan Section 22. It weakens the evidence-integrity guarantee at the point where the ledger determines what is safe to skip or repeat. The absence of present evidence-production authority does not relax an implementation requirement the accepted Decision explicitly made part of implementation acceptance.

```text
MALFORMED_LEDGER_FINDING=BLOCKING
```

## 8. Temporary copy cleanup

`verifyFromFreshCopy(root, tempParent)` creates `bounding-evidence-copy-*` directly under the supplied `tempParent`, which production passes as the campaign root's parent. No `finally` block or recursive cleanup exists on success or exception. Current tests place both campaign and copy under JUnit `@TempDir`, so eventual outer cleanup masks the leak. A future evidence-producing invocation could retain an evidence-bearing sibling copy outside the governed campaign directory.

This does not invalidate the current detached, test-owned offline verification because its enclosing temporary tree is disposable and no evidence production is authorized. It does create confidentiality, storage, and undisclosed-copy concerns for future real use.

```text
CURRENT_IMPLEMENTATION_ACCEPTANCE_CLASSIFICATION=NON-BLOCKING QUALIFICATION
MUST_BE_CURED_BEFORE_EVIDENCE_PRODUCTION_AUTHORIZATION=YES
```

Existing implementation authority permits the cleanup because `verifyFromFreshCopy` is inside the already authorized Bounding Evidence file and implements Decision Section 11 / Plan Section 21 copy verification.

## 9. Additional ledger and recovery integrity findings

### 9.1 Duplicate completion is not rejected — BLOCKING

The conversion to `Set<FamilyFBoundingEvidenceStep>` destroys multiplicity. A pre-existing ledger containing the same completed step twice is accepted as one completion, contrary to the explicit duplicate-completion fail-closed requirement.

### 9.2 Completed WP-B/C/D steps are not fully verified — BLOCKING

On resume, only WP-A receives an explicit mandatory-artifact existence and deterministic-content check. If the ledger says WP-B, WP-C, or WP-D completed but its corresponding final index is absent, `executeSteps` silently creates that missing final artifact and then skips the ledger record. This is reconstruction of final state after accepting a completion record, not verification-and-skip. The doc comment claiming every completed step is independently re-derived and byte-compared is inaccurate for these paths.

The gap register and report are also unconditionally rewritten during resume. Accepted Plan Section 22 permits restart only for interrupted steps writing named temporary artifacts and requires completed steps to be verified before skipping; no such temporary-artifact protocol exists here.

### 9.3 Unknown files are not rejected — BLOCKING

Neither `produceOrResume` nor `verifyManifest` enumerates the campaign directory and compares it with the exact permitted artifact set. An unrelated or evidence-bearing unknown file can remain present while resume continues or COMPLETE is accepted, contrary to Plan Sections 9, 21–22 and Decision Section 11.

### 9.4 Manifest path confinement is not validated — BLOCKING integrity gap

`verifyManifest` resolves the manifest-provided relative string with `root.resolve(relative)` but does not reject absolute paths, `..` traversal, normalization outside `root`, duplicate entries, or an unexpected manifest member set. A hand-edited manifest can therefore address a file outside the campaign root and pass if its recorded hash matches. Generated manifests use safe `root.relativize(...)` values, but resume/COMPLETE verification is specifically the adversarial or corrupted-state boundary that must fail closed.

### 9.5 Sound aspects

- ledger writes are append-only until in-memory finalization;
- post-finalization writes through the same ledger instance throw;
- WP-E is recorded before manifest creation;
- COMPLETE and FAILED writers reject the opposite marker;
- COMPLETE with a simple hash mismatch is rejected;
- FAILED and conflicting terminal states halt;
- a WP-E ledger record without a terminal marker halts;
- valid partial WP-A recovery checks records and missing artifacts.

These sound paths do not cure the blocking fail-open paths above.

## 10. WP-A/B/C/D conformance

| Work package | Governance requirement | Implementation and coverage | ICR finding |
|---|---|---|---|
| WP-A | Derive all request bodies from the frozen schedule and production formatter/serializer; exact counts, hashes, Base64, maxima; no accepted bound | Iterates `FamilyFCampaignDefinition.allTrials`; uses `SyntheticContextProfiles`, `DefaultReasoningPromptBuilder`, and `defaultOllamaRequestBody`; tests cover IDs, coverage, determinism, Unicode, Base64/hash, ties, overflow | Conforming for fresh execution; resume integrity remains subject to Section 9 |
| WP-B | Validate pre-supplied primary response evidence; preserve unresolved status without retrieval | Category/provenance validator with admissible and rejected tests; admissible input remains unresolved pending full worksheet | Conforming; no manufactured pass |
| WP-C | Keep count and aggregate-byte semantics distinct; reject conflation | Target-to-limit-kind validation with positive and negative tests | Conforming; no numeric resolution |
| WP-D | Documentation-only provider evidence; reject observation-derived evidence | Official-documentation and observation-origin checks with positive and negative tests | Conforming; remains unresolved |

No validator promotes an accepted numeric value or converts missing evidence into a pass.

## 11. Double gate and execution authority

**Finding: CONFORMING.** `familyFBoundingEvidenceEntryPoint` refuses whenever the live Family F execution approval variable is present, then requires the system property and evidence approval environment value both to equal `true`, then requires an output root. Only afterward does it call the injected/default producer exactly once. One-gate paths return before producer invocation. The positive offline test uses a test-owned temporary directory and delegates to the real producer; it does not set the real approval environment variable and creates no governed evidence or authority.

## 12. Source inspection, detached build, and isolation

**Source-inspection finding:** the exclusion region is narrow and leaves imports and implementation scanned. No substantive blind spot equivalent to the historical import-block exclusion was found.

**Detached-build finding:** the new Gradle task is registered independently, filtered to `ReasoningProtocolFamilyFBoundingEvidenceTest`, uses only the expected system property, and has only `shouldRunAfter(tasks.test)`. No `dependsOn`, `finalizedBy`, lifecycle attachment, or indirect coupling to `test`, `check`, `build`, or `assemble` appears in the task or surrounding build configuration.

**Network/model/provider finding:** the producer path is in-process and filesystem-only. Request construction ends at pure prompt/request-body serialization. WP-B/C/D consume caller-supplied in-memory records. No socket, HTTP client, process builder, Docker, provider, Ollama, or model call is reachable from the Bounding Evidence producer or its offline validators.

## 13. Campaign structure and model mapping

The actual schedule is constructed by four repetitions. Within each repetition it creates one SUBJECT block and one CONTROL block; every block contains three warm-ups plus every combination of 23 fixtures and two profiles.

```text
23 fixtures × 2 profiles × 4 repetitions × 2 roles = 368 scored
4 repetitions × 2 role blocks × 3 warm-ups           = 24 warm-up
TOTAL                                                   = 392
```

The estimator iterates this actual schedule rather than a second handwritten copy. Corrected `FamilyFRole` values flow directly into each record.

## 14. Numeric-bound and provenance findings

```text
NUMERIC_BOUND_SELECTED=NONE
```

The only request maximum emitted is labeled `PROPOSED_MAX_REQUEST_BOUND` and immediately identified in JSON and Markdown as an evidence result only, not an accepted bound. No response, header, runtime-growth, RAM, disk, `E`, or `R` value is selected or promoted.

No Knowledge Discoverability Attempts 1–2 evidence is imported or pooled; no Unit 2/2-D record is rewritten; the lighthouse observation is absent; no historical evidence is reclassified; and no governed Family F evidence was produced during implementation or this review.

## 15. Constitutional assessment of the correction Completion Review

The correction Completion Review is accurate in these material respects:

- it preserves the historical failed review;
- it correctly confirms the role correction and five other historical correction areas;
- it correctly identifies the temporary-copy leak;
- it correctly states that evidence production and numeric-bound selection remain unauthorized.

It is constitutionally inaccurate in its classification of malformed-ledger handling. The accepted Decision does not reserve malformed-record fail-closed behavior for a future evidence-production decision; it imposes that behavior directly on this implementation. The review also did not identify duplicate-step collapsing, incomplete verification/reconstruction of completed WP-B/C/D state, unknown-file acceptance, or manifest path-confinement weakness.

Therefore its `ACCEPTED` verdict is constitutionally unsustainable in its current form.

## 16. Verdict

```text
REVISE BEFORE ACCEPTANCE
```

The defects are serious but localized and correctable within the existing architecture and authorization; `REJECTED` would be disproportionate.

## 17. Existing authority and exact next lawful action

The accepted Implementation Authorization Decision already authorizes ledger parsing, recovery/resume, manifest integrity, unknown-file rejection, copied-directory verification, and their offline tests inside:

```text
tests/integration/ReasoningProtocolFamilyFBoundingEvidenceTest.kt
```

Therefore no new governance amendment is required to:

1. replace permissive ledger substring extraction with strict full-record validation;
2. reject malformed, unterminated, unknown-step, duplicate-step, invalid-status, or out-of-order ledger records;
3. verify every completed step's mandatory artifacts without silently reconstructing or overwriting final state;
4. reject unknown campaign files;
5. confine and validate every manifest path and exact member set;
6. clean the fresh verification copy in a `finally` path on success and failure; and
7. add bounded offline tests for every corrected path.

The next lawful action is a bounded implementation-correction task confined to the already authorized Bounding Evidence file. It must not edit either Completion Review or this ICR. After correction: perform proportionate offline verification; create a fresh Implementation Completion Review preserving both existing reviews; then create a fresh Independent Constitutional Review. Only an accepted fresh review chain may precede implementation acceptance and merge.

## 18. Review boundaries and STOP confirmation

```text
NO implementation file edited.
NO Completion Review edited or overwritten.
NO model called, loaded, or contacted.
NO Ollama/provider/network execution path invoked.
NO Docker or Parker runtime invoked.
NO evidence producer invoked.
NO evidence-production approval variable set or used.
NO governed evidence produced.
NO numeric bound selected.
NO deployment performed.
NO file staged, committed, or pushed.
ONLY this Independent Constitutional Review document was created.
```

