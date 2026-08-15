**Status:** Unit 3-BF Family F Capture-Proxy Response/Request-Size and Dedicated-Runtime-Growth Bounding Scope Lock — **PROPOSED — PENDING INDEPENDENT CONSTITUTIONAL REVIEW.** Documentation-only governance drafted against merged baseline `666c54a6e25b3431428a92e94cfb08210840af7b`. It freezes the evidence, selection, enforcement, failure, provenance, and review rules required to make the Family F evidence budget (`E`) and dedicated-runtime budget (`R`) computable. It selects no numeric bound, changes no implementation, provisions no host, contacts no provider or model, grants no Explicit Execution Approval, and does not authorize Knowledge Discoverability Attempt 3. `READINESS=NOT READY` remains unchanged.

# Reasoning Protocol Live-Model Conformance — Unit 3-BF Family F Capture-Proxy Response/Request-Size and Dedicated-Runtime-Growth Bounding Scope Lock

## 1. Baseline and controlling authority

```text
BASELINE=666c54a6e25b3431428a92e94cfb08210840af7b
BASELINE_DESCRIPTION=Merge pull request #29 from governance/reasoning-protocol-family-f-bounding-planning
DOCUMENT_TYPE=SCOPE LOCK
```

This Scope Lock is controlled by, and must be read with:

1. `docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_RESPONSE_RUNTIME_AND_PARKER_HOST_ISOLATION_PLANNING_REVIEW.md` and its accepted Independent Constitutional Review — especially Planning Review Sections 5–10, 11–15, 20, and 23;
2. `docs/architecture/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_ALTERNATIVE_DIAGNOSTIC_HOST_REQUIREMENTS_SCOPE_LOCK.md` and its accepted Independent Constitutional Review — especially Sections 4, 6–10, and 13–15;
3. `docs/implementation/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_DIAGNOSTIC_IMPLEMENTATION_EXECUTION_PLAN.md` and its accepted Independent Constitutional Review — especially Sections 4–7, 12, 14, 16–18, 22–26, and 30;
4. `docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_DIAGNOSTIC_RAW_TRANSPORT_CAPTURE_CORRECTION_COMPLETION_REVIEW.md` and its accepted Independent Constitutional Review — the accepted correction establishing complete raw transport durability;
5. `docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_PARKER_CANDIDATE_HOST_ASSESSMENT_REVIEW.md` and its accepted Independent Constitutional Review — the current `READINESS=NOT READY` determination;
6. `docs/architecture/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_EXPERIMENTAL_RECLASSIFICATION_AND_QUALIFICATION_BOUNDARY_SCOPE_LOCK.md` and its accepted Independent Constitutional Review — the frozen Family F campaign invariants; and
7. `docs/architecture/parker-constitution.md` — the controlling separation of proposal, authorization, and execution.

This document does not reopen or weaken any accepted finding. If this Scope Lock conflicts with a stricter pre-existing prohibition, the stricter prohibition controls unless a later, expressly authorized governance act identifies and lawfully amends it.

## 2. Purpose

The accepted Planning Review concluded that a bounded-transport and bounded-runtime Scope Lock is proportionate, but that the evidence required to select a response-body ceiling and an `R` ceiling does not yet exist. This document converts that conclusion into binding rules.

It freezes:

- how `MAX_REQUEST_BOUND` may be derived;
- what evidence may establish `MAX_RESPONSE_BOUND`;
- what evidence may establish header-count and aggregate-header-byte bounds;
- the capture proxy's required bounded-streaming behavior;
- oversize failure, durability, classification, retry, and sealing behavior;
- how accepted bounds feed the evidence-budget calculation `E`;
- what is and is not included in dedicated-runtime growth `R`;
- what evidence and containment must exist before `R` can be accepted;
- the provenance package required for every proposed value; and
- the governance sequence required before any implementation or execution.

## 3. Effect of acceptance and merge

Acceptance and merge of this Scope Lock would authorize only the drafting of the next documentation artifact named in Section 22. It would not itself:

- compute, select, or approve a numeric request, response, header, evidence, or runtime-growth bound;
- authorize an estimator, benchmark, provider query, provider launch, or model contact;
- authorize a Kotlin, Gradle, production, test, container, VM, filesystem, or infrastructure change;
- change the accepted Family F implementation;
- change `READINESS=NOT READY`;
- issue or imply Explicit Execution Approval; or
- authorize Knowledge Discoverability Attempt 3.

No section of this document is executable authority.

## 4. Frozen campaign invariants

Nothing in this Scope Lock changes:

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
ADVANCEMENT_GATE=absolute subject-only 3-of-4 per cell plus the existing zero-false-positive,
  zero-material-mutation, and representation-validity conditions
RANKING=PROHIBITED
SUBSTITUTION=PROHIBITED
QUANTIZATION_CHANGE=PROHIBITED
REDUCED_CORPUS_PROFILE_REPETITION_OR_CALL_SCHEDULE=PROHIBITED
```

Host choice, storage pressure, missing provider evidence, or difficulty establishing a bound may not relax any invariant. The lawful result of an unresolved bound is `NOT READY`, not a smaller campaign.

## 5. Definitions and current status

```text
MAX_REQUEST_BOUND=maximum permitted raw HTTP request-body bytes at the capture proxy
MAX_RESPONSE_BOUND=maximum permitted raw upstream HTTP response-body bytes at the capture proxy
MAX_HEADER_COUNT=maximum permitted header entries under the future governed counting rule
MAX_AGGREGATE_HEADER_BYTES=maximum permitted aggregate encoded header bytes under the future governed
  normalization and counting rule
E=maximum allocated bytes for all governed evidence artifacts, calculated under Alternative Host
  Requirements Scope Lock Section 9.2 and the candidate filesystem's allocation unit
R=maximum additional writable growth of the dedicated provider runtime beyond immutable provider-image
  and already-consumed immutable model-artifact storage, calculated under that Scope Lock Section 9.3
```

Current status at this baseline:

```text
MAX_REQUEST_BOUND_STATUS=COMPUTABLE IN PRINCIPLE; NOT COMPUTED OR SELECTED
MAX_RESPONSE_BOUND_STATUS=UNCOMPUTABLE ON CURRENT GOVERNED EVIDENCE
HEADER_BOUNDS_STATUS=EVIDENCE REQUIREMENTS IDENTIFIED; NO VALUES SELECTED
E_STATUS=UNCOMPUTABLE
R_STATUS=UNCOMPUTABLE
READINESS=NOT READY
```

## 6. `MAX_REQUEST_BOUND` selection protocol

`MAX_REQUEST_BOUND` may be proposed only from a deterministic offline estimator that invokes the accepted implementation's actual production formatting path. The estimator must:

1. enumerate all 23 frozen fixtures under both frozen context profiles;
2. include the frozen warm-up input;
3. format each input for both frozen model-name strings;
4. invoke the unmodified `DefaultReasoningPromptBuilder` and `defaultOllamaRequestBody` path used by the accepted Family F harness;
5. measure the exact UTF-8 byte length of every resulting raw HTTP request body;
6. emit every input identity, role/model identity, profile, warm-up/scored designation, measured length, and resulting maximum;
7. fail if any expected input is absent, duplicated, reformatted outside the production path, or not represented in the output; and
8. use checked arithmetic and deterministic ordering.

The proposed value must equal the maximum measured raw request-body length. It may not be:

- sampled from a subset;
- inferred from character count;
- copied from a provider limit;
- padded by an unexplained percentage or round number;
- reduced to make a resource gate pass; or
- computed using a mock formatter, alternate serializer, or changed model name.

`MAX_REQUEST_BOUND` becomes accepted only through a separately reviewed evidence package and governance decision. This Scope Lock does not supply the estimator or its result.

## 7. `MAX_RESPONSE_BOUND` admissible evidence

Because response content is model-generated, the request estimator cannot establish `MAX_RESPONSE_BOUND`. A proposed value is admissible only through one of these routes:

### 7.1 Provider-published hard maximum

The evidence package must identify an immutable provider publication applicable to:

- the exact provider implementation and version;
- the exact request mode and endpoint;
- the exact frozen subject and control artifacts; and
- the complete serialized HTTP response body, not merely generated text.

If the publication limits tokens rather than response bytes, the proposal must also supply immutable tokenizer/serialization evidence establishing a worst-case conversion from the enforced token limit to the complete serialized UTF-8 response-body size, including fixed and variable envelope overhead. Average bytes per token, typical output size, observed maxima, or undocumented assumptions are inadmissible.

### 7.2 Programme-enforced generation maximum

A future proposal may create a hard generation ceiling only if it proves, for the exact provider version, that:

1. the named request parameter is enforced as a hard maximum rather than a hint or soft target;
2. the actual Family F request contains the parameter for every governed call;
3. the provider cannot emit additional generated content beyond that maximum under the governed endpoint behavior;
4. immutable tokenizer evidence supplies a worst-case byte conversion; and
5. the response-envelope serializer's complete worst-case overhead is independently bounded.

The proposed `MAX_RESPONSE_BOUND` must be the checked-arithmetic result of those components. A transport cap cannot be selected first and then described as provider evidence; its value must itself be justified by this section.

### 7.3 Fail-closed status

If neither route produces a complete, version-specific, independently reproducible bound:

```text
MAX_RESPONSE_BOUND_STATUS=UNCOMPUTABLE
E_STATUS=UNCOMPUTABLE
READINESS=NOT READY
```

Historical responses, averages, percentiles, convenient powers of two, memory availability, free disk space, and a bound used by another model or provider are prohibited substitutes.

## 8. Header-bound selection protocol

The accepted implementation currently iterates and persists multi-valued headers without a governed count or aggregate-byte ceiling (`ReasoningProtocolFamilyFDiagnosticTest.kt:505-514,543-547`; `ReasoningProtocolFamilyFDiagnosticOrchestrationTest.kt:569-579`). A future proposal must establish both `MAX_HEADER_COUNT` and `MAX_AGGREGATE_HEADER_BYTES`.

The evidence package must:

1. identify the exact JDK and HTTP-server/client implementations used by the dedicated diagnostic;
2. document every relevant built-in parsing limit and prove whether it applies before allocation;
3. define whether a header entry means a name, a name/value pair, or each value in a multi-valued field;
4. define the exact byte encoding and normalization used for counting;
5. account for inbound request and upstream response headers separately where their parsers differ;
6. account for Base64 and JSON-escaping expansion in durable `responseHeadersJson`; and
7. reject any bound derived solely from typical observed headers.

Implicit library defaults do not become governed bounds merely because they exist. They must be version-identified, reproducible, and shown to satisfy the allocation and evidence requirements. If those facts are absent, the header-bound status remains unresolved and `E` remains uncomputable.

## 9. Bounded capture-proxy enforcement contract

Any future implementation plan must preserve the accepted production-path fidelity while replacing the two live unbounded body reads identified by the accepted Planning Review:

- inbound request: `ReasoningProtocolFamilyFDiagnosticTest.kt:493`;
- upstream response: `ReasoningProtocolFamilyFDiagnosticTest.kt:516`.

The implementation must satisfy all of the following:

```text
STREAMING_COUNT=bytes are counted incrementally before unbounded aggregation
REJECT_BEFORE_EXCESS_ALLOCATION=the implementation stops as soon as the next bytes would exceed the
  accepted bound; it does not first allocate the complete oversize body
NO_TRUNCATE_AND_FORWARD=an oversize body is never shortened and released as if complete
NO_OVERSIZE_RELEASE=the production inference client never receives an oversize upstream response
NO_PARTIAL_DURABLE_BODY=oversize content is not durably stored in full or in misleading truncated form
EXACT_BOUND_ACCEPTED=a body exactly equal to its accepted bound is handled normally
BOUND_PLUS_ONE_REJECTED=the first byte beyond the accepted bound deterministically rejects
```

The response mechanism may use a custom bounded `BodySubscriber` or `BodyHandlers.ofInputStream()` with a bounded read loop, but the future plan must select one mechanism and prove cancellation, closure, and exception propagation. `BodyHandlers.ofByteArray()` is not acceptable on the live upstream-response path after a bound is adopted.

The unmodified production client in `src/runtime/ModelInferenceClient.kt` may remain unchanged only if the proxy proves it never forwards more than `MAX_RESPONSE_BOUND`. Any design that forwards oversize content would reopen the production-client risk and fall outside this Scope Lock.

## 10. Oversize failure and durable evidence

An oversize request, response, or header set is a measurement-invalidating capture failure. It must:

1. fail closed;
2. never retry or reissue the trial;
3. never receive a scored `PrimaryClassification`;
4. never contribute to advancement or subject/control comparison;
5. route through the accepted `FamilyFArtifactIntegrityException` and campaign halt behavior (`ReasoningProtocolFamilyFDiagnosticOrchestrationTest.kt:1634-1640`);
6. prevent campaign sealing; and
7. require new governance before any subsequent live continuation.

The durable rejection record must contain only bounded metadata necessary to prove the failure, including:

- campaign and trial identity;
- request/response/header category;
- accepted bound identity and evidence-package digest;
- observed count at rejection;
- rejection timestamp;
- forwarding/cancellation outcome; and
- chained-ledger envelope and integrity fields.

It must not contain the complete oversize content or present a truncated prefix as the captured request or response. The future plan must define whether the observed count represents `bound + 1`, bytes received before cancellation, or another deterministic bounded quantity, and tests must enforce that definition.

## 11. Evidence-budget (`E`) recomputation

`E` may be recomputed only after request, response, and header bounds are all accepted. The recomputation must use the corrected raw-transport field composition and the Alternative Host Requirements Scope Lock Section 9.2 rules.

At minimum it must include:

- all 392 maximum-size raw request and response records;
- Base64 expansion for raw bodies;
- JSON-escaping expansion where applicable;
- bounded multi-valued header representation;
- all seven chained ledgers and their universal envelopes;
- control and resource records;
- campaign definition and identity;
- subject and control sealed reporting;
- advancement worksheet;
- manifest and terminal marker;
- filesystem allocation rounding for every artifact;
- any proven transient coexistence allocation under the accepted writer implementation; and
- the independently governed 2 GiB evidence-root reserve.

No component may be double-counted. No component may be omitted because it is expected to be small. The estimator must identify the implementation commit, estimator version, raw output, output SHA-256, filesystem allocation unit, checked-arithmetic behavior, and every component subtotal.

If any maximum-size component remains unbounded or any serializer cannot be reproduced from accepted source, `E_STATUS=UNCOMPUTABLE` remains mandatory.

### 11.1 `NO_MANUFACTURED_PASS` reaffirmed

Alternative Diagnostic Host Requirements Scope Lock Section 9.5's `NO_MANUFACTURED_PASS` rule remains binding here, explicitly and without weakening. Neither an evidence package nor a future candidate-host decision may manufacture a resource pass by:

1. deleting or cleaning unrelated data to inflate a momentary usable-space reading;
2. measuring or pointing at an alternate volume, filesystem, evidence root, or runtime root that the governed campaign will not actually use;
3. reducing the evidence captured, including truncating raw bodies, dropping ledgers or mandatory artifacts, or reducing the frozen 392-call schedule; or
4. substituting a different or smaller model artifact, changing quantization, or otherwise reducing the frozen workload.

Every usable-space reading, filesystem allocation unit, `E` calculation, and `R` calculation must resolve to the exact paths and devices named for the future governed campaign. Path/device identity must be recorded and independently reproduced before a pass may be declared. Raw usable disk space, however large, never substitutes for computed `E+R` and the applicable governed reserves.

## 12. Dedicated-runtime budget (`R`) scope

`R` covers only additional writable growth caused by the dedicated diagnostic provider runtime. It excludes:

- immutable provider-image layers already present at rest; and
- immutable frozen model artifacts already present at rest.

It includes every writable location reachable under the exact dedicated launch procedure, including as applicable:

- container writable layer;
- logs;
- temporary files;
- caches;
- generated metadata;
- runtime state and locks;
- unload residue;
- crash or restart residue;
- core dumps; and
- any provider-controlled path not proven read-only or unreachable.

Unknown writable locations are not zero. They make `R` uncomputable unless containment makes them unwritable and the resulting provider behavior is independently verified.

## 13. `R` admissible evidence

A proposed `R` value is admissible only through:

1. immutable, version-specific provider documentation that bounds every writable location under the exact launch procedure; or
2. a separately authorized, candidate-host-specific observation campaign using the exact dedicated launch procedure, with all writable roots identified and measured under a governed representative workload.

For observational evidence, the future governance must define before launch:

- provider image and executable identity;
- exact launch command and configuration;
- read-only and writable mounts;
- model-store disposition;
- log driver and limits;
- tmpfs/quota/cgroup settings;
- measurement points and precision;
- representative load and why it upper-bounds the real 392-call campaign;
- crash, cancellation, unload, and restart scenarios;
- cleanup prohibition during measurement; and
- the rule converting observed growth into a hard ceiling without unexplained padding.

No dedicated observation is authorized by this Scope Lock. Until one admissible route is completed and independently accepted:

```text
R_STATUS=UNCOMPUTABLE
READINESS=NOT READY
```

## 14. Runtime containment contract

Any future dedicated launch procedure used to establish or enforce `R` must default to:

```text
READ_ONLY_ROOT_FILESYSTEM=REQUIRED where the provider can operate under it
EXPLICIT_WRITABLE_PATHS=REQUIRED; every path named and justified
WRITABLE_CAPACITY_ENFORCEMENT=REQUIRED through a host-supported, independently verified mechanism
LOG_GROWTH_BOUND=REQUIRED
CORE_DUMP_BEHAVIOR=EXPLICITLY BOUNDED OR DISABLED
PRODUCTION_VOLUME_MOUNTS=PROHIBITED
PRODUCTION_ENDPOINT_OR_PID_REUSE=PROHIBITED
MODEL_STORE_MUTATION=PROHIBITED during the governed diagnostic
DYNAMIC_DISK_GATES=REMAIN INDEPENDENTLY BINDING
```

Candidate mechanisms include a read-only container root, explicitly sized writable volume, bounded tmpfs, filesystem/project quota, or a genuinely supported container-storage limit. A mechanism is admissible only when the candidate host proves it is active and enforceable. Naming a Docker flag or filesystem feature without host-specific verification is insufficient.

If tmpfs contributes to runtime containment, its full configured capacity must also enter the memory-admission calculation. It may not disappear from accounting merely because it is not disk-backed.

Static `R` containment and the existing per-block dynamic disk checks are cumulative safeguards. Neither replaces the other.

## 15. Evidence provenance package

Every proposed bound must arrive as a sealed, independently reviewable package containing:

```text
BOUND_NAME
PROPOSED_VALUE_IN_BYTES_OR_COUNT
DERIVATION_ROUTE
SOURCE_OR_IMPLEMENTATION_COMMIT
PROVIDER_AND_MODEL_IDENTITIES_WHERE_APPLICABLE
ESTIMATOR_OR_MEASUREMENT_PROCEDURE_VERSION
COMPLETE_RAW_OUTPUT
RAW_OUTPUT_SHA256
CHECKED_ARITHMETIC_WORKSHEET
ASSUMPTIONS (must be empty or individually governed)
REPRODUCTION_INSTRUCTIONS
INDEPENDENT_REPRODUCTION_RESULT
```

The package must keep supplied evidence distinct from independently reproduced evidence. A digest proves identity, not truth; the derivation must also be reviewed.

No value may inherit acceptance from another provider version, model, host, filesystem, serializer, or campaign shape.

## 16. Measurement and decision separation

The party or task producing an estimator or observation result does not thereby approve the resulting bound. The sequence is:

1. accepted Scope Lock;
2. accepted evidence-acquisition and offline-estimator plan;
3. separately authorized evidence production, with no live provider/model contact unless expressly authorized;
4. completion review of the produced evidence;
5. Independent Constitutional Review;
6. explicit bound-selection decision;
7. implementation plan;
8. implementation and offline verification;
9. completion and constitutional reviews;
10. renewed candidate-host assessment and Readiness Review;
11. Explicit Execution Approval; and only then
12. live diagnostic execution.

No step authorizes its successor. Missing evidence at any step preserves `NOT READY`.

## 17. Host-isolation handoff

This Scope Lock does not resolve Category 6 operational isolation. It preserves the accepted Planning Review's explicit handoff:

- same-VM coexistence on Parker VM 102 is not recommended on current evidence;
- a dedicated diagnostic VM on the same physical Proxmox node is constitutionally viable in principle but unprovisioned and unassessed;
- remaining blocked is valid; and
- no VM creation, resize, allocation, provider installation, or model acquisition is authorized here.

Transport and runtime bounds are necessary but not sufficient for host readiness. A future host-track document remains separately required.

## 18. Acceptance gates for proposed bounds

A bound-selection decision must reject a proposed package unless all applicable gates pass:

```text
PRIMARY_EVIDENCE_GATE=PASS
VERSION_AND_ARTIFACT_IDENTITY_GATE=PASS
COMPLETE_SERIALIZATION_OR_WRITABLE_PATH_COVERAGE_GATE=PASS
DETERMINISTIC_REPRODUCTION_GATE=PASS
OVERFLOW_SAFE_ARITHMETIC_GATE=PASS
NO_ARBITRARY_PADDING_OR_GUESS_GATE=PASS
NO_MANUFACTURED_PASS_GATE=PASS
INDEPENDENT_REVIEW_GATE=PASS
FROZEN_INVARIANT_PRESERVATION_GATE=PASS
```

For response and runtime bounds, a smaller value is not safer if it lacks evidence; it merely creates an unjustified failure mode. A larger value is not conservative if it breaks resource admission. Evidence, not intuition, must determine the value.

## 19. Stop conditions

Work under this governance chain stops immediately if it:

- chooses a round, convenient, sampled, average, percentile, or observed-historical bound without an admissible derivation;
- uses typical response size as maximum response size;
- treats a token limit as a complete HTTP response-byte limit without worst-case tokenizer and envelope evidence;
- computes `E` before all request, response, and header bounds are accepted;
- treats immutable model or image bytes as `R`, or omits a writable runtime path from `R`;
- assumes an unsupported quota, tmpfs, storage-driver, or read-only-root behavior;
- truncates oversize content and continues;
- retries an oversize trial;
- scores, classifies, seals, or advances a campaign after an oversize rejection;
- weakens raw evidence, the 392-call schedule, frozen artifacts, or advancement gates to reduce resource requirements;
- contacts a provider/model endpoint, launches a daemon, or provisions infrastructure without separate authority;
- treats this document as resolving host isolation, readiness, or execution authority; or
- begins Knowledge Discoverability Attempt 3.

## 20. Decision register

| Question | Determination | Status |
|---|---|---|
| Is a bounding Scope Lock proportionate? | Yes; the accepted Planning Review established it as the single next governance action. | RESOLVED |
| Does this document select any numeric bound? | No. | RESOLVED |
| Is the request bound computable from frozen inputs? | Yes, through the governed offline estimator in Section 6; not computed here. | RESOLVED |
| Is the response bound computable now? | No; Section 7's evidence routes remain unsatisfied. | RESOLVED |
| Are header bounds established? | No; Section 8 freezes the evidence protocol only. | RESOLVED |
| Is `E` computable now? | No; accepted response and header bounds are absent. | RESOLVED |
| Is `R` computable now? | No; neither admissible evidence route is complete. | RESOLVED |
| May raw usable disk substitute for `E+R`? | No. | RESOLVED |
| Must oversize content fail closed without retry or scoring? | Yes. | RESOLVED |
| Does this Scope Lock resolve Parker host isolation? | No; Section 17 preserves the separate handoff. | RESOLVED |
| Does this Scope Lock authorize evidence production? | No; it authorizes only the next plan after acceptance and merge. | RESOLVED |
| Does this Scope Lock authorize implementation or execution? | No. | RESOLVED |

## 21. Explicit non-claims

This Scope Lock does not claim that:

- any bound is accepted or even numerically proposed;
- the current provider exposes a suitable hard generation limit;
- a tokenizer worst-case byte ratio is available;
- JDK implicit header limits satisfy this programme;
- a dedicated runtime can operate with a read-only root;
- any quota or storage-limit mechanism is available on Parker VM 102 or another host;
- a dedicated observation campaign is safe or authorized;
- `E` or `R` will fit any current or future filesystem;
- a dedicated VM is provisionable;
- same-VM coexistence is accepted;
- readiness has improved; or
- any model run or Knowledge Discoverability Attempt 3 is lawful.

## 22. Exit criteria and next lawful action

This Scope Lock is complete only when:

1. every requirement above is explicit and internally consistent;
2. every cited source exists and supports the attributed claim;
3. an Independent Constitutional Review returns `VERDICT=ACCEPTED` with zero P0–P3 findings;
4. the Scope Lock and accepted review are merged; and
5. `READINESS=NOT READY` and all execution prohibitions remain explicit.

Before acceptance and merge:

```text
NEXT_LAWFUL_ACTION=Independent Constitutional Review of this Scope Lock
```

After acceptance and merge:

```text
NEXT_LAWFUL_ACTION=Family F Bounding Evidence Acquisition and Offline Estimator Plan
```

That future plan may propose how to produce the Section 6 request estimator and how to seek admissible response/header/runtime evidence. It may not assume authority to contact a provider or model, launch a dedicated daemon, or provision a host; any such step must be separately identified and authorized.

## 23. Constitutional conformance

This Scope Lock follows the constitution's separation: cognition may propose a measurement and enforcement design; trust must separately authorize evidence acquisition, implementation, and execution; runtime may execute only after those approvals. No bound authorizes itself, no missing evidence becomes permission, and the owner retains the ability to see, limit, stop, and decline every later step.

## 24. Final authority statement

```text
PROGRAMME_STATUS=ACTIVE
FAMILY_F_STATUS=INCLUDED FOR PRE-QUALIFICATION DIAGNOSTIC SCOPING ONLY (unchanged)
IMPLEMENTATION_STATUS=ACCEPTED (unchanged)
READINESS=NOT READY (unchanged)
MAX_REQUEST_BOUND_STATUS=COMPUTABLE IN PRINCIPLE; NOT COMPUTED OR SELECTED
MAX_RESPONSE_BOUND_STATUS=UNCOMPUTABLE
HEADER_BOUNDS_STATUS=UNRESOLVED
E_STATUS=UNCOMPUTABLE
R_STATUS=UNCOMPUTABLE
NUMERIC_BOUND_SELECTED=NONE
IMPLEMENTATION_AUTHORIZED=NO
EVIDENCE_ACQUISITION_AUTHORIZED=NO
PROVIDER_OR_MODEL_CONTACT_AUTHORIZED=NO
DEDICATED_PROVIDER_LAUNCH_AUTHORIZED=NO
HOST_OR_VM_PROVISIONING_AUTHORIZED=NO
MODEL_ACQUISITION_AUTHORIZED=NO
CAMPAIGN_AUTHORIZED=NO
EXPLICIT_EXECUTION_APPROVAL_AUTHORIZED=NO
KNOWLEDGE_DISCOVERABILITY_ATTEMPT_3_AUTHORIZED=NO
CATEGORY_6_HOST_ISOLATION=HANDED OFF, NOT RESOLVED
NEXT_LAWFUL_ACTION=Independent Constitutional Review; after acceptance and merge, a separately
  governed Family F Bounding Evidence Acquisition and Offline Estimator Plan
```
