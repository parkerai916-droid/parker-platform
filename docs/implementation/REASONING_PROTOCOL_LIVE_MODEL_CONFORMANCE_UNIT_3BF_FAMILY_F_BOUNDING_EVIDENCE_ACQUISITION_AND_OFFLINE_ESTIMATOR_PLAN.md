**Status:** Unit 3-BF Family F Bounding Evidence Acquisition and Offline Estimator Plan — **PROPOSED — PENDING INDEPENDENT CONSTITUTIONAL REVIEW.** Documentation-only plan drafted against merged baseline `54b4ef33d2f1c6da07a38997d4f01537bd8ba630`. It defines the exact offline estimator, passive evidence-acquisition work packages, artifact schema, provenance, failure behavior, review gates, and authority sequence needed to investigate the still-unresolved Family F request, response, header, evidence-budget (`E`), and dedicated-runtime (`R`) bounds. It authorizes no implementation or evidence production, selects no numeric bound, contacts no provider or model, launches no daemon, provisions no host, grants no Explicit Execution Approval, and does not authorize Knowledge Discoverability Attempt 3. `READINESS=NOT READY` remains unchanged.

**Model-identity premise correction.** Corrected in place against the accepted Model-Identity Premise Defect Confirmation Review and its Independent Constitutional Review (commit `4d8d5012243df955683fe929a6cf7a0dc6766ffc`): this document's own `CONTROL_MODEL=llama3.2:3b` designation is not itself rewritten, but rests on an upstream premise — that `llama3.2:3b` was Parker's current, live, or production model — now corrected; `qwen2.5-coder:7b`, not `llama3.2:3b`, was Parker's committed deployed Docker baseline throughout this programme. CONTROL_MODEL/SUBJECT_MODEL roles and the Family F research question remain unresolved, pending separate governance. The remainder of this document's body is unmodified and remains the historical record of this review.

# Reasoning Protocol Live-Model Conformance — Unit 3-BF Family F Bounding Evidence Acquisition and Offline Estimator Plan

## 1. Baseline and controlling authority

```text
BASELINE=54b4ef33d2f1c6da07a38997d4f01537bd8ba630
BASELINE_DESCRIPTION=Merge pull request #30 from governance/reasoning-protocol-family-f-bounding-scope-lock
DOCUMENT_TYPE=IMPLEMENTATION/EVIDENCE PLAN
```

This Plan is controlled by, and must be read with:

1. `docs/architecture/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_CAPTURE_PROXY_RESPONSE_REQUEST_SIZE_AND_DEDICATED_RUNTIME_GROWTH_BOUNDING_SCOPE_LOCK.md` and its accepted Independent Constitutional Review — especially Sections 3, 5–16, 18–19, and 22;
2. `docs/reviews/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_RESPONSE_RUNTIME_AND_PARKER_HOST_ISOLATION_PLANNING_REVIEW.md` and its accepted Independent Constitutional Review;
3. `docs/architecture/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_ALTERNATIVE_DIAGNOSTIC_HOST_REQUIREMENTS_SCOPE_LOCK.md` and its accepted Independent Constitutional Review — especially Sections 9.2–9.5;
4. `docs/implementation/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_DIAGNOSTIC_IMPLEMENTATION_EXECUTION_PLAN.md` and its accepted Independent Constitutional Review;
5. the accepted Family F implementation, raw-transport correction, completion reviews, Readiness Review, Parker Candidate Host Assessment, and their accepted Independent Constitutional Reviews; and
6. `docs/architecture/parker-constitution.md`.

This Plan implements no authority by existing. Acceptance and merge would establish only the design and the next governance action named in Section 30.

## 2. Purpose

The accepted Bounding Scope Lock leaves five deliberately unresolved values:

```text
MAX_REQUEST_BOUND=computable in principle; not computed or selected
MAX_RESPONSE_BOUND=uncomputable on current governed evidence
MAX_HEADER_COUNT=unresolved
MAX_AGGREGATE_HEADER_BYTES=unresolved
R=uncomputable
```

This Plan defines how a future, separately authorized evidence-production task would:

- deterministically compute request-body evidence offline from the accepted implementation;
- collect and classify primary provider evidence relevant to a hard response maximum;
- collect and classify primary JDK/provider evidence relevant to header limits;
- collect and classify provider documentation relevant to writable runtime growth;
- preserve complete raw evidence, negative search results, hashes, and provenance; and
- return `RESOLVED`, `UNRESOLVED`, or `NOT ADMISSIBLE` without forcing a numeric answer.

## 3. Authority created by acceptance and merge

Acceptance and merge of this Plan would authorize only the drafting of a separate **Family F Bounding Evidence Implementation Authorization Decision**.

It would not authorize:

- either implementation file in Section 5 to be created or changed;
- the offline estimator to be compiled or run;
- network browsing, downloads, HTTP requests, provider APIs, or model endpoints;
- `ollama` CLI use, provider-version queries, model loading, generation, or unloading;
- a dedicated provider launch or `R` observation campaign;
- creation of the evidence directory or artifacts in Section 9;
- selection or approval of any bound;
- recomputation or approval of `E` or `R`;
- host or VM provisioning;
- Explicit Execution Approval; or
- Knowledge Discoverability Attempt 3.

No step in this Plan authorizes its successor.

## 4. Frozen inputs and statuses

The evidence work must preserve:

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
RANKING=PROHIBITED
MODEL_SUBSTITUTION=PROHIBITED
QUANTIZATION_CHANGE=PROHIBITED
```

Current statuses remain:

```text
MAX_REQUEST_BOUND_STATUS=NOT COMPUTED OR SELECTED
MAX_RESPONSE_BOUND_STATUS=UNCOMPUTABLE
HEADER_BOUNDS_STATUS=UNRESOLVED
E_STATUS=UNCOMPUTABLE
R_STATUS=UNCOMPUTABLE
READINESS=NOT READY
```

## 5. Exact future implementation boundary

Any future implementation proposed under this Plan is limited to exactly:

```text
build.gradle.kts
tests/integration/ReasoningProtocolFamilyFBoundingEvidenceTest.kt (new)
```

The first file may add only one detached offline task. The second may contain only the offline estimator, evidence-schema writer, offline validators, source-inspection guards, and tests specified here.

No file under `src/` may change. The accepted Family F diagnostic files, shared live-model harness, production formatter, production serializer, provider client, parser, and existing Gradle tasks must remain byte-unchanged under this Plan.

Needing a third file is a stop condition requiring new governance.

## 6. Detached task and network/model isolation

The future Gradle task name is frozen as:

```text
reasoningProtocolFamilyFBoundingEvidence
```

The execution gate and output configuration names are frozen as:

```text
SYSTEM_PROPERTY=parker.reasoning.familyf.boundingEvidence.enabled
APPROVAL_ENV=PARKER_REASONING_FAMILY_F_BOUNDING_EVIDENCE_APPROVED
OUTPUT_ROOT_ENV=PARKER_REASONING_FAMILY_F_BOUNDING_EVIDENCE_ROOT
```

It must:

- use the existing `liveModelEvaluation` source set;
- filter exclusively to `parker.integration.ReasoningProtocolFamilyFBoundingEvidenceTest`;
- be absent from `test`, `check`, `build`, and `assemble` task graphs;
- set only `parker.reasoning.familyf.boundingEvidence.enabled=true`;
- require an output directory supplied by a separately approved configuration value;
- reject an existing non-empty output directory;
- run with no Family F live-execution property or environment approval;
- make no socket, HTTP, Docker, Ollama, Proxmox, subprocess, or model call; and
- fail if a source-inspection guard finds forbidden network/process APIs in the estimator class.

The single evidence-producing entry test must check, before resolving the output root or constructing any writer:

1. `parker.reasoning.familyf.boundingEvidence.enabled == "true"`; and
2. `PARKER_REASONING_FAMILY_F_BOUNDING_EVIDENCE_APPROVED == "true"`.

If either is absent, the entry test self-skips before configuration or filesystem access. Only the dedicated Gradle task may set the system property; only a future Evidence Production Authorization Decision may authorize the environment value. A source-inspection meta-test must prove that the entry test contains exactly these two gates and invokes the real evidence entry point exactly once.

The broad `reasoningProtocolLiveModelEvaluation` task must therefore encounter only a skipped evidence-producing entry test, while ordinary offline unit tests for the estimator remain runnable without producing evidence.

This is offline evidence tooling, not a live-model task. The existing `PARKER_REASONING_FAMILY_F_EXECUTION_APPROVED` value must be absent; if present, the evidence entry point must refuse to run rather than treating it as additional authority.

## 7. Work-package model

The evidence-production task is divided into five packages:

```text
WP-A=offline request-bound estimator
WP-B=response provider/version/tokenizer evidence inventory
WP-C=header-limit evidence inventory
WP-D=dedicated-runtime provider-documentation inventory
WP-E=manifest, integrity verification, and completion report
```

WP-A is executable Kotlin operating solely on repository inputs. WP-B through WP-D are governed evidence-ingestion and validation packages: the future task may validate evidence files supplied through a separately authorized collection process, but it may not itself browse, query, download, or contact anything.

Every work package may return `UNRESOLVED`. Evidence production succeeds when it honestly preserves the result, not only when it finds a usable bound.

## 8. Evidence root and atomic completion

The future authorization must name one absolute `BOUNDING_EVIDENCE_ROOT` outside the repository and outside every production data/model volume.

The implementation must:

1. reject an unresolved, relative, symlink-ambiguous, existing non-empty, production-overlapping, or repository-inside root;
2. resolve and record its filesystem/device identity;
3. write into one newly created campaign directory;
4. force every artifact to durable storage before the next dependent step;
5. never delete or clean unrelated data to manufacture capacity;
6. never redirect a check to a volume the evidence package will not actually use;
7. write `SHA256SUMS.txt` only after every mandatory artifact exists;
8. verify every artifact from a freshly copied directory before writing `evidence.complete`; and
9. make the completed directory read-only where the host supports doing so without changing another owner’s data.

No partial directory may be represented as complete. `evidence.failed` and `evidence.complete` are mutually exclusive terminal markers.

## 9. Mandatory artifact set

The future evidence directory must contain exactly the governed artifacts below, plus no undisclosed evidence-bearing file:

```text
evidence-identity.json
repository-inputs.json
progress-ledger.jsonl
request-estimator-records.jsonl
request-estimator-summary.json
response-primary-evidence-index.json
response-primary-evidence/                  (zero or more immutable source captures)
header-primary-evidence-index.json
header-primary-evidence/                    (zero or more immutable source captures)
runtime-primary-evidence-index.json
runtime-primary-evidence/                   (zero or more immutable source captures)
evidence-gap-register.json
bounding-evidence-report.md
SHA256SUMS.txt
evidence.complete OR evidence.failed
```

An empty primary-evidence directory is lawful only when its index records `SOURCE_COUNT=0`, the complete search protocol, and `STATUS=UNRESOLVED`. Absence without a negative-evidence record is not a result.

## 10. Evidence identity

`evidence-identity.json` must include:

```text
schemaVersion
evidenceCampaignId
planCommit
scopeLockCommit
repositoryCommit
repositoryTree
estimatorImplementationCommit
gradleVersion
kotlinVersion
jvmVendor
jvmVersion
operatingSystem
startedAt
completedAt
outputRoot
outputFilesystemIdentity
```

The evidence campaign ID must be supplied by the future authorization and match a conservative machine-safe pattern. The evidence identity may not imply a Family F live campaign and must not reuse a diagnostic campaign ID.

## 11. Repository-input preservation

`repository-inputs.json` must record SHA-256 and Git blob identity for:

- `build.gradle.kts`;
- the new estimator file;
- `tests/integration/ReasoningProtocolFamilyFDiagnosticTest.kt`;
- `tests/integration/ReasoningProtocolFamilyFDiagnosticOrchestrationTest.kt`;
- `tests/integration/ReasoningProtocolLiveModelEvaluationHarness.kt`;
- `src/runtime/ReasoningPromptBuilder.kt`;
- `src/runtime/ModelInferenceClient.kt`;
- the Bounding Scope Lock and accepted ICR; and
- this Plan and accepted ICR.

The task must stop if the working tree is dirty, if `HEAD` differs from the separately authorized implementation commit, or if any protected input differs from the reviewed hash set.

## 12. WP-A — exact schedule-derived request estimator

The estimator must iterate the accepted `FamilyFCampaignDefinition.allTrials` schedule (`tests/integration/ReasoningProtocolFamilyFDiagnosticOrchestrationTest.kt:83-137`); it may not maintain a second handwritten fixture or schedule copy.

For each of the 392 scheduled calls it must:

1. identify block, order, warm-up/scored status, role, model name, fixture, profile, and repetition;
2. reproduce the accepted offline-recovery construction exactly: resolve a null warm-up profile to `ContextProfileId.MINIMAL_PRODUCTION_CONTEXT`, call `SyntheticContextProfiles.construct(trial.fixture, profileId)`, and obtain the `Turn` with `(input.request.subject as ReasoningSubject.OfTurn).turn`, matching `tests/integration/ReasoningProtocolFamilyFDiagnosticOrchestrationTest.kt:1284`;
3. call the unmodified `DefaultReasoningPromptBuilder().buildPrompt(turn, input.request.reasoningContext)`;
4. call the unmodified `defaultOllamaRequestBody(prompt, modelName)`;
5. encode the returned body as UTF-8;
6. record its exact byte count and SHA-256;
7. preserve the complete body as Base64 in the evidence record; and
8. independently decode, recount, and re-hash before finalization.

The record count must be exactly 392. The task must also derive a unique-body index so repeated schedule entries remain visible rather than silently discarded.

The estimator must prove coverage of:

```text
23 fixtures × 2 profiles × 4 repetitions × 2 roles = 368 scored records
8 blocks × 3 warm-ups = 24 warm-up records
TOTAL=392
```

Any mismatch halts evidence production.

## 13. WP-A output schema

Each canonical JSONL record must contain, in fixed key order:

```text
schemaVersion
sequence
trialId
blockId
blockOrder
warmup
role
modelName
fixtureId
profileId
repetition
promptUtf8ByteCount
promptSha256
requestBodyUtf8ByteCount
requestBodySha256
requestBodyBase64
```

JSONL encoding is UTF-8 with LF endings. Numbers use base-10 integer form. Base64 is standard padded Base64. No locale-dependent formatting is permitted.

`request-estimator-summary.json` must contain:

- expected and observed record counts;
- scored and warm-up counts;
- per-role/profile maxima;
- the global maximum record identity and byte count;
- every tied maximum identity;
- unique-body count;
- sorted distribution of exact body lengths;
- source-input hashes; and
- a final `REQUEST_BOUND_EVIDENCE_STATUS`.

The summary may state `PROPOSED_MAX_REQUEST_BOUND=<exact global maximum>` only as an evidence result. It is not an accepted bound until a later completion review, constitutional review, and explicit bound-selection decision.

## 14. WP-A independent checks

Offline tests must prove:

- deterministic byte-identical output across two clean runs;
- exact 392-record coverage and trial-ID equality with the frozen schedule;
- no missing or duplicate trial ID;
- both model names represented;
- both profiles represented;
- all fixtures represented for both roles and profiles;
- all 24 warm-up records represented;
- UTF-8 byte count differs correctly from character count for adversarial Unicode;
- JSON and Base64 round-trip integrity;
- maximum computation handles ties;
- checked arithmetic rejects overflow;
- any protected-input drift stops before output finalization; and
- no network/process API is reachable from the estimator.

No test may contact a model endpoint or depend on a resident model.

## 15. WP-B — response primary-evidence inventory

WP-B may ingest only immutable or content-addressed primary sources collected under the future authorization. Admissible source categories are:

1. official provider documentation for the exact provider version and endpoint;
2. official provider source or release artifact tied to the exact executable/image identity;
3. official tokenizer/model documentation tied to each frozen artifact; and
4. an already-accepted programme-enforced generation-limit specification.

Each source record must include:

```text
sourceId
sourceCategory
publisher
title
canonicalLocator
retrievedAt
applicableProviderVersionOrDigest
applicableModelDigest
contentSha256
localCapturePath
relevantClaim
exactLocationWithinSource
applicabilityAssessment
admissibilityStatus
rejectionReason
```

Third-party summaries, forum posts, search-result snippets, observed response sizes, averages, and uncited recollections are inadmissible.

## 16. WP-B response-bound derivation worksheet

The evidence report must keep these components separate:

```text
HARD_GENERATION_TOKEN_LIMIT
HARD_LIMIT_ENFORCEMENT_EVIDENCE
WORST_CASE_UTF8_BYTES_PER_TOKEN
FIXED_RESPONSE_ENVELOPE_BYTES
VARIABLE_NON_GENERATED_RESPONSE_BYTES
ESCAPING_EXPANSION
TERMINATION_AND_DELIMITER_BYTES
CHECKED_TOTAL_RESPONSE_BOUND
```

Every component must be supported by applicable primary evidence. A token limit without complete serializer/envelope accounting is `INCOMPLETE`. A transport ceiling whose number was chosen before this derivation is circular and `NOT ADMISSIBLE`.

WP-B must return exactly one status:

```text
RESOLVED_WITH_PROPOSED_VALUE
UNRESOLVED_NO_APPLICABLE_PRIMARY_SOURCE
UNRESOLVED_INCOMPLETE_SERIALIZATION_BOUND
NOT_ADMISSIBLE
```

This Plan predicts no status and selects no value.

## 17. WP-C — header primary-evidence inventory

WP-C must identify the exact JDK vendor/version and the actual HTTP server/client implementations used by the separately reviewed estimator/diagnostic commit. It must collect primary evidence for:

- inbound request-header parsing limits;
- upstream response-header parsing limits;
- whether limits apply before material allocation;
- header-name/value encoding and normalization;
- multi-valued header representation;
- count semantics;
- aggregate-byte semantics;
- configurable properties and their exact runtime values; and
- Base64/JSON durable-expansion behavior.

The evidence report must not conflate:

- parser wire limits;
- application-level iteration limits;
- durable encoded-size limits; or
- provider-produced header behavior.

WP-C returns separate statuses for `MAX_HEADER_COUNT` and `MAX_AGGREGATE_HEADER_BYTES`. If either is unresolved, header-bound status remains unresolved and `E` remains uncomputable.

## 18. WP-D — runtime provider-documentation inventory

WP-D is documentation-only. It may ingest primary provider evidence describing writable runtime behavior for the exact provider version, including:

- writable paths;
- logs and rotation;
- temporary files;
- caches;
- generated metadata;
- runtime state and locks;
- model-store mutation;
- unload residue;
- crash/restart residue;
- core dumps; and
- operation under a read-only root with explicit writable mounts.

WP-D performs no launch, benchmark, model request, unload, crash, restart, filesystem mutation, or observation campaign.

It returns:

```text
RESOLVED_BY_COMPLETE_PROVIDER_DOCUMENTATION
UNRESOLVED_PROVIDER_DOCUMENTATION_INCOMPLETE
NOT_ADMISSIBLE
```

If complete provider documentation does not establish a genuine hard ceiling, `R_STATUS=UNCOMPUTABLE` remains. The report may recommend a separately governed candidate-host-specific observation plan, but it may not draft or authorize the observation itself.

## 19. Negative-evidence protocol

“Not found” is not self-proving. For each unresolved work package, `evidence-gap-register.json` must record:

- exact question;
- admissible source classes;
- sources and versions searched;
- canonical locators;
- search terms or source sections examined;
- timestamps;
- source captures or immutable digests;
- why each candidate source was inapplicable or incomplete;
- remaining missing fact; and
- what future governance would be needed to seek it.

The task must never convert “not found in this search” into “does not exist.”

## 20. Source-collection boundary

The future Evidence Production Authorization Decision must enumerate every allowed collection mechanism. Unless expressly authorized there, the default is:

```text
REPOSITORY_READS=ALLOWED
PRE-SUPPLIED_LOCAL_PRIMARY_SOURCE_FILES=ALLOWED
OFFICIAL_WEB_DOCUMENT_RETRIEVAL=NOT AUTHORIZED
PROVIDER_API_OR_MODEL_ENDPOINT=NOT AUTHORIZED
OLLAMA_CLI=NOT AUTHORIZED
DOCKER_EXEC_CP_EXPORT_SAVE=NOT AUTHORIZED
DAEMON_LAUNCH_OR_RESTART=NOT AUTHORIZED
MODEL_ACQUISITION_OR_LOAD=NOT AUTHORIZED
INFRASTRUCTURE_CHANGE=NOT AUTHORIZED
```

An authorization may permit read-only official-document retrieval without permitting provider/model contact, but must name domains, source classes, capture requirements, and network boundaries explicitly.

## 21. WP-E — integrity and report generation

WP-E runs only after WP-A through WP-D reach terminal evidence statuses. It must:

1. verify every JSON/JSONL artifact against its schema;
2. decode and re-hash every preserved request body;
3. verify source-capture digests;
4. cross-check every evidence index against files on disk;
5. reject undisclosed files;
6. write `bounding-evidence-report.md` from structured evidence, not handwritten values;
7. write `SHA256SUMS.txt` covering every mandatory artifact except itself and the terminal marker;
8. copy the directory to a fresh verification location;
9. verify every hash from the copy; and
10. write exactly one terminal marker.

The report must state every status prominently and must not imply that a proposed value is accepted.

## 22. Exact-once and recovery behavior

Evidence production is offline but still governed. The future implementation must write the mandatory `progress-ledger.jsonl` artifact inside the evidence campaign directory. This append-only progress ledger must use deterministic step IDs for:

```text
PREFLIGHT
WP_A_ESTIMATOR
WP_B_RESPONSE_EVIDENCE
WP_C_HEADER_EVIDENCE
WP_D_RUNTIME_EVIDENCE
WP_E_VALIDATION
```

`WP_E_VALIDATION` is the final progress-ledger step. After its durable completion record is appended, the implementation must finalize `progress-ledger.jsonl` and prohibit every later append before writing `SHA256SUMS.txt`. The manifest therefore covers the complete, immutable progress ledger exactly like every other mandatory artifact.

Copy verification is a read-only, safely repeatable integrity operation over that finalized evidence set; it must not append to the progress ledger or modify any manifest-covered artifact. If interrupted before `evidence.complete`, it must be rerun in full from a fresh copy. Terminal state is represented only by the mutually exclusive `evidence.complete` or `evidence.failed` marker, not by a later ledger record. On resume, an existing terminal marker must be verified against the finalized manifest and ledger before it is accepted.

Completed ledger-governed steps may be verified and skipped on resume. An interrupted ledger-governed step may restart only if it wrote exclusively to a named temporary artifact excluded from the mandatory evidence set and the restart first verifies that no final artifact exists. Conflicting final/temporary state, duplicate step completion, an append attempted after ledger finalization, hash mismatch, or an unknown file halts permanently.

No evidence source may be silently re-fetched or replaced on resume.

## 23. Failure semantics

Any of the following writes `evidence.failed`, prevents `evidence.complete`, and requires review before rerun:

- dirty or unexpected repository state;
- input hash drift;
- schedule mismatch;
- missing or duplicate estimator record;
- non-deterministic estimator output;
- arithmetic overflow;
- malformed or inapplicable primary evidence represented as admissible;
- missing negative-evidence provenance;
- evidence-index/file mismatch;
- failed copied-directory verification;
- network, provider, model, process, Docker, or infrastructure access by the estimator task; or
- any attempt to select a bound inside evidence production.

An unresolved evidence question is not a task failure when correctly recorded; it is a valid negative result preserving `NOT READY`.

## 24. Offline verification requirements

Before evidence production could be authorized, the future two-file implementation must pass offline tests proving:

- task detachment from ordinary lifecycle tasks;
- exact task filter;
- refusal when the live execution-approval environment value is present;
- no network/process/provider/model code path;
- exact schedule and request serialization fidelity;
- deterministic output and schemas;
- provenance completeness;
- negative-evidence handling;
- exact-once recovery;
- terminal-marker exclusivity;
- copied-directory verification;
- tamper detection; and
- no change under `src/` or to accepted Family F implementation files.

The full ordinary Gradle suite must also pass. No live task is part of verification.

## 25. Evidence completion review

After a separately authorized evidence run, a Completion Review must independently verify:

- exact reviewed implementation and input commits;
- exact 392-record estimator coverage;
- request-body round trips and hashes;
- every primary source's identity and applicability;
- every negative search claim;
- response/header/runtime status derivations;
- complete mandatory artifact set;
- manifest and copied-directory verification;
- absence of prohibited contact or execution; and
- no accepted-bound language in the evidence report.

The Completion Review and its Independent Constitutional Review do not select a bound. They decide only whether the evidence package is admissible.

## 26. Bound-selection separation

If, and only if, an accepted evidence package proposes one or more values, a later **Family F Bound Selection Decision** must decide each independently:

```text
MAX_REQUEST_BOUND
MAX_RESPONSE_BOUND
MAX_HEADER_COUNT
MAX_AGGREGATE_HEADER_BYTES
R
```

The decision may accept some values and leave others unresolved. It must not manufacture completeness by treating a partial set as sufficient for `E` or readiness.

No implementation of bounded readers, headers, runtime containment, or `E` calculation may begin before the applicable values are explicitly selected.

## 27. `NO_MANUFACTURED_PASS`

Alternative Diagnostic Host Requirements Scope Lock Section 9.5 and the Bounding Scope Lock Section 11.1 remain binding. Evidence production may not manufacture a pass by:

- cleanup or deletion to inflate space;
- using an alternate filesystem/root the real campaign will not use;
- truncating evidence, dropping artifacts, or reducing the 392-call schedule; or
- substituting a smaller/different/requantized model or workload.

The evidence root's real path and device identity must be recorded. Raw usable disk space never substitutes for computed `E+R` and governed reserves.

## 28. Stop conditions

Work stops if it:

- exceeds the two-file future implementation boundary;
- duplicates production formatting or schedule logic instead of invoking accepted code;
- runs the estimator without a separate Evidence Production Authorization Decision;
- browses, downloads, queries, launches, or contacts anything not expressly authorized;
- converts a negative search into a universal non-existence claim;
- selects a numeric bound in an estimator or evidence report;
- treats request evidence as response evidence;
- treats a token limit as a complete response-byte limit without envelope evidence;
- treats finite observation, an average, or convenient padding as a runtime ceiling;
- weakens evidence, artifacts, models, or schedule;
- touches a production process, endpoint, volume, or model store;
- provisions a host or VM;
- treats partial evidence as `E` or readiness completion;
- issues Explicit Execution Approval; or
- begins Knowledge Discoverability Attempt 3.

## 29. Decision register

| Question | Determination | Status |
|---|---|---|
| Does this Plan select a numeric bound? | No. | RESOLVED |
| Does acceptance authorize implementation or evidence production? | No. | RESOLVED |
| Is the future implementation boundary exact? | Yes: `build.gradle.kts` plus one new offline test file. | RESOLVED |
| Is the request estimator schedule-derived? | Yes; all 392 calls are represented. | RESOLVED |
| Can evidence production return unresolved? | Yes; unresolved is a valid governed result. | RESOLVED |
| Does WP-B contact a provider or model? | No; it validates separately authorized primary-source captures. | RESOLVED |
| Does WP-D launch or observe a provider? | No. | RESOLVED |
| Does this Plan select or provision a host? | No. | RESOLVED |
| Is `NO_MANUFACTURED_PASS` preserved? | Yes. | RESOLVED |
| Does evidence completion select bounds? | No; selection is a later decision. | RESOLVED |
| Does this Plan change readiness? | No; `READINESS=NOT READY`. | RESOLVED |

## 30. Governance sequence and next lawful action

The sequence is:

1. this Plan;
2. Independent Constitutional Review;
3. acceptance and merge;
4. Family F Bounding Evidence Implementation Authorization Decision;
5. authorized two-file implementation;
6. implementation Completion Review and Independent Constitutional Review;
7. Family F Bounding Evidence Production Authorization Decision;
8. separately authorized evidence production;
9. Evidence Completion Review and Independent Constitutional Review;
10. Family F Bound Selection Decision;
11. bounded-transport/runtime implementation plan and implementation;
12. bounded-transport/runtime implementation Completion Review and Independent Constitutional Review;
13. renewed candidate-host assessment and Readiness Review;
14. Explicit Execution Approval; and only then
15. live Family F diagnostic execution.

No step authorizes its successor.

Before this Plan is accepted and merged:

```text
NEXT_LAWFUL_ACTION=Independent Constitutional Review of this Plan
```

After acceptance and merge:

```text
NEXT_LAWFUL_ACTION=Family F Bounding Evidence Implementation Authorization Decision
```

## 31. Explicit non-claims

This Plan does not claim that:

- an estimator has been implemented or run;
- 392 request records have been produced;
- a request bound is accepted;
- applicable response/provider/tokenizer evidence exists;
- applicable JDK header evidence is sufficient;
- provider documentation can resolve `R`;
- an `R` observation campaign is authorized;
- any evidence root exists;
- any host is selected or ready;
- any bound will fit a candidate host;
- `E` or `R` is computable now;
- implementation or evidence production is authorized;
- provider/model contact is authorized;
- readiness has improved; or
- a live campaign or Knowledge Discoverability Attempt 3 is lawful.

## 32. Constitutional conformance

This Plan preserves the constitutional separation of proposal, authorization, and execution. It defines how cognition may structure evidence, requires trust to authorize implementation, collection, selection, and execution separately, and leaves runtime unable to act under this document alone. Negative evidence preserves owner control by allowing the programme to remain blocked without manufacturing a result.

## 33. Final authority statement

```text
PROGRAMME_STATUS=ACTIVE
FAMILY_F_STATUS=INCLUDED FOR PRE-QUALIFICATION DIAGNOSTIC SCOPING ONLY (unchanged)
IMPLEMENTATION_STATUS=ACCEPTED FOR THE EXISTING DIAGNOSTIC ONLY (unchanged)
BOUNDING_EVIDENCE_IMPLEMENTATION_STATUS=NOT STARTED
BOUNDING_EVIDENCE_PRODUCTION_STATUS=NOT AUTHORIZED
READINESS=NOT READY
MAX_REQUEST_BOUND_STATUS=NOT COMPUTED OR SELECTED
MAX_RESPONSE_BOUND_STATUS=UNCOMPUTABLE
HEADER_BOUNDS_STATUS=UNRESOLVED
E_STATUS=UNCOMPUTABLE
R_STATUS=UNCOMPUTABLE
NUMERIC_BOUND_SELECTED=NONE
PROVIDER_OR_MODEL_CONTACT_AUTHORIZED=NO
DEDICATED_PROVIDER_LAUNCH_AUTHORIZED=NO
R_OBSERVATION_AUTHORIZED=NO
HOST_OR_VM_PROVISIONING_AUTHORIZED=NO
MODEL_ACQUISITION_AUTHORIZED=NO
CAMPAIGN_AUTHORIZED=NO
EXPLICIT_EXECUTION_APPROVAL_AUTHORIZED=NO
KNOWLEDGE_DISCOVERABILITY_ATTEMPT_3_AUTHORIZED=NO
NEXT_LAWFUL_ACTION=Independent Constitutional Review; after acceptance and merge, a Family F
  Bounding Evidence Implementation Authorization Decision
```
