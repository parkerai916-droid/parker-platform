# Reasoning Protocol Live-Model Conformance — Unit 3-BF Family F Diagnostic Implementation/Execution Plan

**Status:** Proposed implementation/execution plan. This document creates no execution authority until it and its Independent Constitutional Review are accepted and merged, the implementation is completed and independently accepted, a Readiness Review and its Independent Constitutional Review are accepted, and a separate Explicit Execution Approval is issued.

**Model-identity premise correction.** Corrected in place against the accepted Model-Identity Premise Defect Confirmation Review and its Independent Constitutional Review (commit `4d8d5012243df955683fe929a6cf7a0dc6766ffc`): this document's own designation of `llama3.2:3b` as comparison control is not itself rewritten, but rests on an upstream premise — that `llama3.2:3b` was Parker's current, live, or production model — now corrected; `qwen2.5-coder:7b`, not `llama3.2:3b`, was Parker's committed deployed Docker baseline throughout this programme. CONTROL_MODEL/SUBJECT_MODEL roles and the Family F research question remain unresolved, pending separate governance. The remainder of this document's body is unmodified and remains the historical record of this review.

---

## 1. Baseline and controlling authority

This plan is authored from repository commit
`0f9eb8b4d90b9f51a8afeda975ee5238464a6582`.

Its controlling authority is:

- `docs/architecture/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_EXPERIMENTAL_RECLASSIFICATION_AND_QUALIFICATION_BOUNDARY_SCOPE_LOCK.md`;
- its accepted Independent Constitutional Review;
- the accepted Family F Alternative-Model Diagnostic Planning Review and its Independent Constitutional Review;
- the accepted Reopening Decision and its Independent Constitutional Review;
- the still-binding Unit 3-B qualification boundary, including item 14 except for the single Family F-only exception fixed by the Unit 3-BF Scope Lock; and
- the original Unit 2 corpus and context definitions incorporated by the Scope Lock.

The Scope Lock remains authoritative if any wording here can reasonably be read more broadly.

## 2. Purpose

This plan defines the exact test-tier implementation, evidence mechanics, review sequence, readiness checks, and execution protocol for the one bounded Family F diagnostic frozen by the Scope Lock.

The diagnostic asks only whether the proposed subject satisfies its own absolute investment-screening gate across the complete frozen corpus and both frozen context profiles. It does not rank the subject against the control, qualify either model, select a model, select a remedy, authorize deployment, or unblock Knowledge Discoverability.

## 3. Authority created by acceptance and merge

Acceptance and merge of this plan authorize only implementation of the three files in Section 5 and the reviews named in Section 22.

They do not authorize:

- acquiring, pulling, copying, converting, or modifying a model artifact;
- starting or loading a model;
- contacting a model endpoint;
- running a warm-up, scored call, smoke call, or partial campaign;
- changing production Kotlin code;
- changing a production configuration;
- using a production Parker process or production model endpoint;
- Knowledge Discoverability Attempt 3;
- qualification, selection, deployment, or remedy implementation; or
- treating any implementation test as live-model evidence.

## 4. Frozen implementation character

The implementation is test-tier orchestration around the unmodified production prompt, inference-client, response-parser, and classification path. It may add capture and durability infrastructure only in the two new integration-test files.

No file under `src/` may change. The public types in
`tests/integration/ReasoningProtocolLiveModelEvaluationHarness.kt` are reused without modification. Existing Unit 2-D and Unit 3-C evidence, tasks, tests, and artifacts remain untouched.

## 5. Exact file boundary

Exactly three repository files are authorized:

1. `build.gradle.kts` — modified only to add the detached Family F diagnostic task fixed in Section 7.
2. `tests/integration/ReasoningProtocolFamilyFDiagnosticTest.kt` **(new file)** — frozen definitions, configuration validation, transparent capture proxy, live entry point, and offline tests.
3. `tests/integration/ReasoningProtocolFamilyFDiagnosticOrchestrationTest.kt` **(new file)** — exact schedule construction, resource/residency gates, durable ledger, recovery rules, sealing, and offline orchestration tests.

No fourth file is authorized. If compilation or testing appears to require another file, implementation stops and governance is revised before work continues.

## 6. Single atomic implementation boundary

The three files in Section 5 form one atomic implementation boundary and one implementation commit. Partial implementation must not be presented for acceptance.

The implementation must compile and its offline tests must pass without any model endpoint, local model daemon, model artifact, campaign directory, or execution approval being present. The live entry point must remain inert unless every gate in Sections 7 and 19 is satisfied.

## 7. Detached Gradle task and double execution gate

`build.gradle.kts` adds exactly one task named:

`reasoningProtocolFamilyFDiagnostic`

It uses the existing `liveModelEvaluation` source set, includes exactly the two test classes named in Section 5, and is not a dependency of `test`, `check`, `build`, `assemble`, or any other ordinary lifecycle task.

The task supplies the JVM system property:

`parker.reasoning.familyf.enabled=true`

The live test additionally requires the environment value:

`PARKER_REASONING_FAMILY_F_EXECUTION_APPROVED=true`

Both must be present. Either one alone is insufficient. When either is absent, the live test is skipped before configuration resolves an endpoint and before any process, socket, model, or campaign artifact is touched. Offline definition and orchestration tests remain runnable.

No alternative task name, test filter, hidden entry point, or direct main method may bypass this double gate.

## 8. Frozen corpus and profile transcription

The implementation transcribes, without semantic change, the complete 23-fixture Unit 2 primary corpus incorporated by the Scope Lock:

- 3 REMEMBER fixtures;
- 13 REPLY fixtures;
- 5 GOAL fixtures; and
- 2 NOACTION fixtures.

The fixture order is fixed as:

`R01, R02, R03, P01` through `P13`, `G01` through `G05`, `N01, N02`.

Each fixture is evaluated under both profiles, in this order:

1. `minimal-production-context`;
2. `mixed-full-production-like`.

The definitions must be equality-tested against the canonical Unit 2 values. No fixture may be replaced, paraphrased, omitted, added, reclassified, or reordered. No context field may be varied except as fixed by the two canonical profiles.

## 9. Models and immutable identity

The logical roles are:

- **subject:** `qwen2.5-coder:7b`, a proposed diagnostic subject only;
- **control:** `llama3.2:3b`, a comparison control only.

Neither role implies qualification or selection. Execution may use only exact pre-existing artifacts whose immutable identities, artifact sizes, and provider-visible identities are fixed in the future Explicit Execution Approval.

Before any load, the runner must verify for each role:

- configured role and exact model name;
- immutable digest or provider-equivalent identity;
- verified artifact size in bytes;
- provider and provider version;
- model metadata returned by the dedicated endpoint; and
- equality with the Explicit Execution Approval.

An alias, a changed digest, an unknown size, an unexpected quantization, a substituted provider, or a missing pre-existing artifact is a hard stop. This plan authorizes no acquisition to repair such a stop.

## 10. Frozen 392-call schedule

There are exactly four scored repetitions. Each model receives every one of the 46 fixture/profile cells once per repetition:

`23 fixtures × 2 profiles × 4 repetitions × 2 models = 368 scored calls`.

There are eight residency blocks: one block for each repetition/model combination. Every block begins with exactly three unscored warm-up calls:

`8 blocks × 3 warm-ups = 24 warm-up calls`.

The complete envelope is therefore:

`368 scored + 24 warm-up = 392 total model calls`.

The model order is AB/BA alternation, not a full Latin square:

- repetitions 1 and 3: subject block, then control block;
- repetitions 2 and 4: control block, then subject block.

Within every model block, the three warm-ups occur first. The 46 scored calls then follow the fixture and profile order fixed in Section 8.

Every warm-up uses `minimal-production-context` and this exact existing Unit 2 input:

`Synthetic warm-up request: reply with a brief acknowledgement.`

The warm-up prompt, profile, order, and count are identical in every block and for both models.

No retry, replacement call, discretionary probe, readiness inference, third model, extra warm-up, or partial rerun may increase or alter the 392-call envelope.

## 11. Deterministic trial identity

Every call receives one immutable trial identifier before execution.

Warm-up identifiers use:

`ff-r<01-04>-<subject|control>-warmup-<01-03>`

Scored identifiers use:

`ff-r<01-04>-<subject|control>-<fixture-id-lowercase>-<minimal|mixed>`

The complete ordered schedule and its SHA-256 hash are materialized before the first call. The runner rejects duplicate identifiers, missing identifiers, an order mismatch, any count other than 392, any scored count other than 368, or any warm-up count other than 24.

## 12. Production-path fidelity and transparent capture

Every model call must use the unmodified production chain represented by:

- `DefaultReasoningPromptBuilder`;
- `ModelReasoningProvider`;
- `LocalHttpModelInferenceClient`;
- the default Ollama request formatter;
- the default Ollama response-body extraction;
- `TaggedReasoningResponseParser`; and
- the existing classification and representation-validity semantics from the live-model evaluation harness.

Because `LocalHttpModelInferenceClient` does not expose the complete HTTP status/header exchange, its endpoint is a test-tier, loopback-only transparent capture proxy implemented in the new test file. The proxy forwards the request body unchanged to the dedicated upstream model endpoint and forwards the upstream response status, relevant headers, and body unchanged to the production client.

For every call the proxy durably records:

- trial identifier;
- timestamp and monotonic ordering value;
- upstream endpoint identity without credentials;
- complete raw request body and SHA-256;
- HTTP status;
- interpretation-relevant response headers;
- complete raw response bytes and SHA-256; and
- forwarding outcome.

The corresponding terminal observation also records campaign, repetition, model-block, fixture, profile and attempt identities; model/provider/digest/runtime and host identities; repository commit; inference settings; prompt SHA-256; extracted response string; parse result or parser failure; expected and actual action; content-fidelity result; material mutation/invention result; representation validity; start/end timestamps; latency; timeout and transport outcomes; token/evaluation counts where supplied; resource and residency evidence references; and terminal-ledger state.

The response record is forced to durable storage before the response is released to `LocalHttpModelInferenceClient`. A capture or durability failure after dispatch is an ambiguous-call hard stop, never authority to retry.

Offline tests use a fake upstream server to prove byte-for-byte request and response body transparency, status preservation, required-header preservation, exactly-one forwarding, and failure behavior. Those tests are implementation evidence only and are not live-model evidence.

Model identity is the only experimental variable. Both arms use the same repository commit, host, production chain, endpoint protocol, transparent proxy, request timeout, classification procedure, fixtures, rubrics, profiles, order, and resource policy. The production default formatter emits only `model`, `prompt`, and `stream:false`; the model name is the sole arm-specific request value. If provider metadata or preflight shows any other inference setting or default cannot be made identical, the campaign stops before model contact pending fresh governance. No prompt rewrite, schema change, semantic repair, retry policy, deterministic classifier, inference-control change, or hybrid mechanism may be introduced.

## 13. No downstream Knowledge Discoverability path

The runner invokes only the synthetic Reasoning Protocol conformance path. It must not:

- start Parker's interactive runtime;
- submit an owner message;
- create or query Memory Core data;
- invoke `ReasoningKnowledgeSource`;
- construct a Knowledge Discoverability prompt;
- inspect Knowledge Discoverability persistence;
- reuse either prior Knowledge Discoverability evidence directory; or

- perform Knowledge Discoverability Attempt 3.

Repository-level structural tests scan the two new files for prohibited production entry points and fail if this boundary is broadened.

## 14. Dedicated endpoint and production isolation

Execution requires a dedicated loopback model-daemon process and endpoint created for this diagnostic. It must be distinct from every production Parker and production model endpoint.

The future Explicit Execution Approval must fix:

- the dedicated endpoint URL;
- the dedicated daemon launch procedure and executable identity;
- the provider binary's resolved absolute path and SHA-256;
- dedicated writable runtime and evidence roots;
- production Parker and production model-daemon PIDs/endpoints to protect;
- both immutable model identities and sizes; and
- the exact repository commit and plan/review commits.

The approved launch procedure may be performed only inside the Explicit Execution Approval's start window. After launch and before model contact, the runner records the observed dedicated PID and verifies its executable path and digest against the approval. A pre-existing or unexpected PID is not accepted merely because it listens on the configured port.

The runner confirms before and after the campaign that protected production PIDs and endpoints are unchanged and healthy using read-only checks. It never signals, stops, restarts, reconfigures, or routes traffic through them.

A shared production endpoint, an endpoint identity that cannot be distinguished, a non-loopback endpoint, or an unexpected process identity is a hard stop.

## 15. Residency-block protocol

Only the model assigned to the current block may be resident in the dedicated daemon.

Before each of the eight blocks, the runner:

1. confirms through the dedicated provider's process/model-residency API that neither governed model is resident;
2. performs the artifact-size-aware pre-load memory gate in Section 16;
3. records and forces the gate evidence to disk; and
4. permits the first of the three warm-ups to load the assigned model.

After each block, the runner uses the resolved provider binary against only the dedicated endpoint to request unload of the assigned model, then polls the residency API until absence is verified or the fixed timeout expires.

Provider control and residency-inspection operations are recorded in a separate control-event ledger. They are not `/api/generate` calls and do not count toward 392. They must not themselves generate an inference. A failed unload, both models resident, the wrong model resident, or unverifiable residency is a hard stop.

## 16. Resource gates

Immediately before the first call that can load a model in each residency block:

`MemAvailable >= verified artifact size of assigned model + 2 GiB`.

Immediately before and immediately after every one of the 392 calls:

`MemAvailable >= 2 GiB`.

The runner reads `MemAvailable` from the operating system source fixed by the Explicit Execution Approval and records the raw reading, parsed byte value, threshold, trial identifier, and timestamp.

The campaign evidence root and dedicated runtime root must each have at least 2 GiB usable space before campaign creation and before every block. The configured artifact parent must already exist; the runner does not create a substitute parent on another volume.

Any failed, missing, stale, unparseable, or below-threshold measurement stops before the next model call. Resource failure never authorizes model substitution, reduced fixture coverage, reduced repetitions, fewer profiles, deletion of evidence, or a retry.

## 17. Durable campaign layout

The campaign is created beneath the governed artifact parent using the campaign identifier fixed by the Explicit Execution Approval. Creation fails if the directory already exists.

The directory contains at least:

- `campaign-definition.json`;
- `campaign-identity.json`;
- `schedule.jsonl`;
- `intent.jsonl`;
- `dispatch.jsonl`;
- `transport.jsonl`;
- `terminal.jsonl`;
- `control-events.jsonl`;
- `resource-readings.jsonl`;
- `advancement-worksheet.json`;
- `SHA256SUMS.txt`; and
- exactly one terminal marker: `campaign.sealed` or `campaign.halted`.

Every append-only record is canonical UTF-8 JSON with one object per line. Each record includes schema version, campaign ID, trial ID where applicable, sequence number, prior-record hash, record hash, and timestamp. The implementation forces file contents and parent-directory metadata at each governance-critical transition.

No mutable summary is evidence unless it can be regenerated from the sealed append-only ledgers.

## 18. Exact-once dispatch and recovery

Before any model contact, all 392 schedule and intent records are written, verified, and forced to disk.

For each call, the runner uses this order:

1. verify the scheduled next trial and all pre-call gates;
2. append and force its dispatch record immediately before HTTP dispatch;
3. have the proxy durably persist the raw request before forwarding;
4. dispatch exactly once;
5. have the proxy durably persist the raw response before returning it;
6. parse, classify, and validate representation;
7. append and force exactly one terminal record; and
8. re-verify counts and chain integrity before proceeding.

Recovery rules are fixed:

- intent without dispatch may resume only at that same next trial after all gates are rerun;
- dispatch without a complete raw response is ambiguous and permanently halts the campaign;
- a complete durably captured response without a terminal record may be classified offline without another model call;
- terminal without complete transport evidence is invalid and permanently halts the campaign;
- no completed or ambiguous trial may be reissued; and
- a halted campaign cannot be resumed or replaced without new governance.

Process death, timeout, HTTP failure, malformed JSON, parser failure, invalid representation, or evidence-write failure consumes the dispatched call and follows these rules. None authorizes a retry.

## 19. Runtime configuration and approval equality

The live runner requires exact values for:

- campaign ID;
- artifact parent;
- dedicated upstream endpoint;
- protected production endpoint identities;
- subject and control names, digests, and sizes;
- provider binary path and digest;
- dedicated provider launch procedure and the observed PID recorded after approved launch;
- protected production PIDs;
- repository commit;
- request timeout;
- unload timeout; and
- the Explicit Execution Approval identifier and hash.

The concrete environment-variable names are constants in the new definition file and are exhaustively equality-tested. The runner writes the resolved non-secret values into `campaign-identity.json` before contact.

The Explicit Execution Approval must reproduce every value. Missing values, additional models, approval mismatch, an unclean repository, a different HEAD, or an unaccepted review commit is a pre-contact hard stop.

Secrets, if any, must not be written to evidence. This plan expects a loopback endpoint requiring no secret; the appearance of credential-bearing configuration is a stop condition and requires governance revision.

## 20. Scoring and subject-only advancement gate

Warm-ups are never scored. Each scored response is evaluated against the frozen expected action, content-fidelity rule, mutation/invention rule, and representation-validity rule.

The subject satisfies the absolute investment-screening advancement gate only if all of these are true:

- at least 3 of 4 scored repetitions are correct in every one of the 46 fixture/profile cells;
- zero false-positive REMEMBER classifications occur;
- zero false-positive GOAL classifications occur;
- zero material mutation or invention occurs;
- all 184 subject scored outputs are representation-valid;
- all 392 calls and all required evidence records are complete and sealed; and
- every resource, residency, identity, schedule, and durability gate remains valid.

This is always described as an **investment-screening advancement gate**. It is not a qualification pass, production threshold, model-selection rule, ranking criterion, or permission to execute another campaign.

The control is reported under the identical corpus, profiles, repetitions, and measurement definitions, but no relative comparison, win/loss statement, aggregate ranking, or substitution decision is authorized—even if both models satisfy their respective reported absolute measures.

## 21. Interpretation and provenance separation

The sealed report presents subject and control results in separate sections and by fixture/profile cell. It must disclose all failures, invalid representations, call failures, resource events, and halted states.

This campaign remains distinct from:

- Unit 2-D DQ4 evidence;
- the 277-plus calls from prior remedy work;
- Knowledge Discoverability Attempt 1;
- Knowledge Discoverability Attempt 2; and
- any future qualification evidence.

No call or outcome is pooled into those statistics. No warm-up or scored exposure counts toward qualification. The durable Knowledge Discoverability evidence remains read-only and outside the campaign directory.

## 22. Mandatory governance and review sequence

The sequence is linear:

1. this Implementation/Execution Plan receives an accepted Independent Constitutional Review and is merged;
2. the exact three-file implementation is completed without model contact;
3. an Implementation Completion Review and Independent Constitutional Review are accepted and merged;
4. a Readiness Review verifies the precise runtime environment, pre-existing artifacts, identities, resource capacity, evidence root, production isolation, and dry-run-only offline evidence;
5. the Readiness Review receives an accepted Independent Constitutional Review and is merged;
6. a separate Explicit Execution Approval fixes all Section 19 values and authorizes exactly one 392-call campaign; and only then
7. the campaign may execute once.

No later step may begin before both required reviews of the preceding step are accepted. A draft PR, green CI, installed model, available endpoint, or successful offline test is not execution approval.

## 23. Implementation verification requirements

Before implementation can be accepted, tests must prove at least:

- exactly 23 canonical fixtures and the exact category distribution;
- exactly two canonical profiles and the fixed order;
- exactly 368 scored, 24 warm-up, and 392 total schedule records;
- AB/BA alternation and exact per-block ordering;
- deterministic unique trial IDs and schedule hash;
- exactly three warm-ups per block and no warm-up scoring;
- subject/control role separation and immutable identity matching;
- double-gate inertness before any endpoint resolution;
- task detachment from normal Gradle lifecycle tasks;
- transparent proxy byte preservation, status/header capture, and exactly-once forwarding;
- production formatter/parser use rather than duplicated protocol logic;
- durable pre-dispatch intent and dispatch ordering;
- every recovery branch in Section 18, including ambiguous-call no-retry behavior;
- artifact-size-aware pre-load and per-call resource gates using fake readings;
- residency verification and unload failure behavior using a fake provider controller;
- no production process control and endpoint/PID inequality checks;
- exact advancement-gate calculations across all 46 cells;
- zero-false-positive and zero-mutation gate behavior;
- representation-validity completeness;
- separate subject/control reporting with no ranking output;
- provenance separation;
- seal and halt marker exclusivity;
- full manifest/hash verification from a copied campaign directory; and
- structural absence of Knowledge Discoverability and production-runtime entry points.

Tests must use fake endpoints, fake resource readers, fake clocks, fake provider controllers, and temporary directories. They must make zero real model calls.

## 24. Readiness Review evidence

The future Readiness Review must independently verify, without inference calls:

- accepted plan and implementation review commits;
- clean exact repository HEAD;
- full Gradle suite success;
- task detachment and double-gate behavior;
- executable/provider identity;
- exact pre-existing subject and control artifact identity and size;
- dedicated endpoint/process isolation from production;
- production PID/endpoint protection baseline;
- sufficient memory for the larger artifact plus 2 GiB;
- sufficient disk for evidence and dedicated runtime roots;
- a fresh nonexistent campaign directory beneath the governed parent;
- offline transparent-proxy and recovery tests;
- schedule count/hash and 392-call envelope;
- exact approval-value template; and
- absence of model acquisition, model loading, model contact, and Knowledge Discoverability activity during readiness work.

If any item needs a live model call to verify, readiness fails; that call cannot be disguised as preparation.

## 25. Explicit Execution Approval content

The separate approval must name:

- its own immutable identifier and document hash;
- the exact accepted repository commit;
- campaign ID and evidence parent;
- dedicated endpoint, launch procedure, and expected daemon executable identity;
- subject/control names, digests, sizes, and metadata;
- provider binary path and digest;
- protected production PIDs/endpoints;
- timeouts and resource sources;
- schedule hash and expected counts;
- authorized start window and human operator;
- the one permitted Gradle command; and
- confirmation that only one fresh campaign is authorized.

It must restate that a halt, ambiguity, or failed campaign receives no automatic retry and that Knowledge Discoverability Attempt 3 remains unauthorized.

## 26. Stop conditions

Implementation stops if:

- any file outside Section 5 appears necessary;
- a production source edit appears necessary;
- the complete raw transport cannot be captured while retaining the unmodified production client/parser path;
- ordinary Gradle tasks can reach the live entry point;
- an offline test contacts a real endpoint; or
- exact-once durability cannot be established without broadening authority.

Readiness or execution stops before the next call if:

- any review or approval is missing or mismatched;
- the repository is dirty or at the wrong commit;
- a required model artifact is absent, changed, or unidentified;
- acquisition, download, conversion, or substitution would be required;
- the dedicated endpoint/process cannot be distinguished from production;
- a production process would be touched;
- model residency is wrong or unverifiable;
- a resource or disk gate fails;
- evidence cannot be forced durably;
- schedule, count, order, or hash differs;
- an ambiguous or duplicate dispatch exists;
- a 393rd model call would occur;
- a synthetic fixture or profile would change;
- a Knowledge Discoverability message, persistence path, or retrieval path would be invoked; or
- credentials would enter the evidence.

After any stop, the campaign is marked halted where safe, no further model call occurs, evidence is preserved read-only, and a factual report is required. No automatic retry or replacement campaign is authorized.

## 27. Decision register

1. **RESOLVED:** implementation is test-tier only and confined to exactly three files.
2. **RESOLVED:** the live task is detached and double-gated.
3. **RESOLVED:** the complete 23-fixture, two-profile corpus is used without alteration.
4. **RESOLVED:** the schedule is exactly 368 scored plus 24 warm-up calls.
5. **RESOLVED:** ordering is AB/BA alternation, not a full Latin square.
6. **RESOLVED:** subject and control are fixed logical roles, not selected or qualified models.
7. **RESOLVED:** capture uses a test-tier transparent proxy while preserving the unmodified production client/parser path.
8. **RESOLVED:** exactly-once behavior uses precommitted intent, pre-dispatch records, raw transport capture, terminal records, and no retry after ambiguity.
9. **RESOLVED:** execution uses only a dedicated endpoint/process and never production.
10. **RESOLVED:** model availability is a readiness fact, never acquisition authority.
11. **RESOLVED:** memory gating is artifact-size-aware before load and at least 2 GiB before/after every call.
12. **RESOLVED:** model residency is verified between all eight blocks; provider control operations are not inference calls.
13. **RESOLVED:** the subject gate is an absolute investment-screening gate across all 46 cells.
14. **RESOLVED:** the control cannot create relative ranking or substitution authority.
15. **RESOLVED:** no exposure counts toward qualification.
16. **RESOLVED:** no Knowledge Discoverability dependency probe or Attempt 3 is included.
17. **RESOLVED:** execution requires Completion, Constitutional, Readiness, Constitutional, and Explicit Approval gates after implementation.
18. **RESOLVED:** a halt or ambiguous call receives no automatic retry.

No item remains TBD, optional, implementation-defined, or deferred to operator discretion.

## 28. Explicit non-claims

This plan does not claim that:

- Family F is admitted beyond the single diagnostic;
- Qwen or Llama is qualified, selected, safe, superior, or suitable for production;
- the control is a benchmark winner or loser;
- the 3-of-4 investment-screening gate is qualification;
- the diagnostic is a full qualification campaign;
- the implementation exists or passes before it is reviewed;
- model artifacts are available;
- resource gates will pass;
- a live campaign will complete;
- any outcome authorizes remedy implementation or deployment;
- Knowledge Discoverability live promotion or recall is verified;
- Knowledge Discoverability closure is unblocked; or
- a failed or halted campaign may be repeated.

## 29. Exit criteria

This plan is complete only when it specifies, without unresolved alternatives:

- exact files and one atomic implementation boundary;
- detached and double-gated reachability;
- exact corpus, profiles, models, ordering, schedule, and identifiers;
- production-path fidelity and complete raw capture;
- dedicated endpoint and production isolation;
- resource, residency, identity, disk, and durability gates;
- exact-once recovery and no-retry semantics;
- evidence layout, sealing, scoring, reporting, and provenance;
- mandatory implementation tests;
- the complete review/readiness/approval chain; and
- stop conditions and non-claims.

Meeting these drafting criteria authorizes no execution.

## 30. Final authority statement

Upon acceptance and merge, the next lawful action is implementation of exactly the three files in Section 5, with zero model contact. The Family F diagnostic itself remains unauthorized until all later reviews and the separate Explicit Execution Approval in Section 22 are complete.
